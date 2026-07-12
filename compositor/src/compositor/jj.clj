(ns compositor.jj
  "The single contract with jujutsu. Everything that touches the repo goes
   through here.

   Two non-negotiable rules from the design review (docs/REVIEW.md):
     - Never scrape human-readable output. Use `-T` templates that emit JSON,
       parsed here into Clojure data. (F-notes and §2.4 of the spec.)
     - Never rewrite trunk. Every invocation passes
       immutable_heads() = the trunk bookmark, so `absorb`/`describe`/`squash`
       physically cannot rewrite landed history (review F3).

   Concurrency: the daemon funnels *all* jj calls through one channel (`queue`
   / `submit`) so two writes never race. Because there is exactly one consumer
   thread, work is serialized with no lock and in FIFO order — that single
   channel is the daemon's whole synchronization story (see
   docs/concurrency-notes.md). One-shot CLI commands run on a single thread
   already and may call `run`/`commits`/`change-id` directly."
  (:require [babashka.process :as p]
            [cheshire.core :as json]
            [clojure.core.async :as async]
            [clojure.string :as str]))

;; ── context ──────────────────────────────────────────────────────────────
;; A ctx is {:repo "/abs/path/to/repo" :trunk "main"}. `:trunk` drives the
;; immutability guard; `:repo` is the default working directory for calls.

(defn- immutable-config [{:keys [trunk]}]
  ;; Passed as two argv elements — no shell in between, so no quoting games.
  (when trunk
    ["--config" (str "revset-aliases.\"immutable_heads()\"=present(" trunk ")")]))

(defn- base-args [ctx]
  (into ["--no-pager" "--color" "never"] (immutable-config ctx)))

;; ── raw invocation ───────────────────────────────────────────────────────

(defn run
  "Run jj in `ctx`'s repo (or `:dir` override). Returns {:out :err :exit}.
   Throws ex-info on nonzero exit unless `:check?` false is passed — some jj
   commands report expected non-success (e.g. probing a stale workspace)."
  [ctx args & {:keys [dir check?] :or {check? true}}]
  (let [res (apply p/shell
                   {:out :string :err :string :continue true
                    :dir (or dir (:repo ctx))}
                   (into ["jj"] (concat (base-args ctx) args)))]
    (when (and check? (not (zero? (:exit res))))
      (throw (ex-info (str "jj failed: jj " (str/join " " args))
                      {:exit (:exit res) :err (:err res) :args (vec args)})))
    res))

(defn out
  "Trimmed stdout of a jj call, or nil if blank."
  [ctx args & opts]
  (-> (apply run ctx args opts) :out str/trim not-empty))

;; ── templates → data ─────────────────────────────────────────────────────

(def ^:private commit-template
  ;; Emits one JSON object per commit. Verified against jj 0.43 that
  ;; description.escape_json() yields a fully-quoted, escaped JSON string.
  (str "\"{\""
       " ++ \"\\\"change_id\\\":\\\"\" ++ change_id.short() ++ \"\\\",\""
       " ++ \"\\\"commit_id\\\":\\\"\" ++ commit_id.short() ++ \"\\\",\""
       " ++ \"\\\"description\\\":\" ++ description.escape_json() ++ \",\""
       " ++ \"\\\"conflict\\\":\" ++ if(conflict,\"true\",\"false\") ++ \",\""
       " ++ \"\\\"empty\\\":\" ++ if(empty,\"true\",\"false\") ++ \",\""
       " ++ \"\\\"parents\\\":[\" ++ parents.map(|c| \"\\\"\" ++ c.change_id().short() ++ \"\\\"\").join(\",\") ++ \"]\""
       " ++ \"}\\n\""))

(defn commits
  "All commits in `revset` as maps: {:change_id :commit_id :description
   :conflict :empty :parents}. Descriptions are right-trimmed (jj stores a
   trailing newline). The virtual root commit is excluded."
  [ctx revset & {:keys [dir]}]
  (->> (run ctx ["log" "-r" revset "--no-graph" "-T" commit-template] :dir dir)
       :out
       str/split-lines
       (remove str/blank?)
       (map #(json/parse-string % true))
       (map #(update % :description (fnil str/trimr "")))
       (remove #(= (:change_id %) "zzzzzzzzzzzz"))))   ; virtual root

(defn commit
  "The single commit at `revset`, or nil."
  [ctx revset & opts]
  (first (apply commits ctx revset opts)))

(defn change-id
  "The change id at `revset` (must resolve to exactly one commit), or nil."
  [ctx revset & {:keys [dir]}]
  (out ctx ["log" "-r" revset "--no-graph" "-T" "change_id"] :dir dir))

(defn conflicted?
  "True if the commit at `revset` records a conflict. This is the daemon's
   gate before ever materializing the composite (review F6)."
  [ctx revset & {:keys [dir]}]
  (= "1" (out ctx ["log" "-r" revset "--no-graph" "-T" "if(conflict,\"1\",\"0\")"]
              :dir dir)))

(defn conflicted-files
  "File paths with recorded conflicts at `revset` (from `jj resolve --list`).
   Empty when clean. Used to attribute a collision to sessions (review F12)."
  [ctx revset & {:keys [dir]}]
  (let [{:keys [out exit]} (run ctx ["resolve" "--list" "-r" revset]
                                :dir dir :check? false)]
    ;; exit is nonzero when there is nothing to resolve; treat as empty.
    (if (zero? exit)
      (->> out str/split-lines (remove str/blank?)
           (keep #(first (str/split % #"\s+"))))
      [])))

;; ── operations (thin; sequences live in graph.clj / session.clj) ──────────

(defn git-init      [ctx]            (run ctx ["git" "init" "--colocate"]))
(defn describe      [ctx rev msg & {:keys [dir]}]
  (run ctx ["describe" "-r" rev "-m" msg] :dir dir))
(defn new-commit
  "Create a commit with the given parents; return its change id. `--no-edit`
   keeps every workspace's @ where it is (review: daemon must never move @s)."
  [ctx parents msg]
  (let [res (run ctx (into ["new" "--no-edit" "-m" msg] parents))
        ;; jj prints \"Created new commit <hash>\"; we resolve the change id
        ;; of the commit we just described rather than parse the hash.
        m   (re-find #"Created new commit (\S+)" (str (:out res) (:err res)))]
    (when (second m)
      (change-id ctx (second m)))))
(defn rebase        [ctx src dests]
  (run ctx (into ["rebase" "-s" src] (mapcat (fn [d] ["-d" d]) dests))))
(defn abandon       [ctx rev]        (run ctx ["abandon" rev]))
(defn bookmark-move [ctx name to]    (run ctx ["bookmark" "move" name "--to" to "--allow-backwards"]))
(defn bookmark-create [ctx name rev] (run ctx ["bookmark" "create" name "-r" rev]))
(defn absorb        [ctx & {:keys [dir]}] (run ctx ["absorb"] :dir dir))
(defn op-restore    [ctx op-id]      (run ctx ["op" "restore" op-id]))

;; ── workspaces ────────────────────────────────────────────────────────────

(defn workspace-add
  "Add a workspace named `wname` rooted at `path`, its @ a fresh child of
   `rev`. Returns the change id of that new working-copy commit — this is the
   session's stable primary key (review F1)."
  [ctx wname path rev]
  (run ctx ["workspace" "add" "--name" wname (str path) "--revision" rev])
  (change-id ctx (str wname "@")))

(defn workspace-forget [ctx wname]   (run ctx ["workspace" "forget" wname]))

(defn snapshot!
  "Force a snapshot of the workspace at `dir` by running a no-op `jj status`
   there. jj amends that workspace's @ and auto-rebases its descendants (the
   megamerge). Retries once through `update-stale` if the copy is stale."
  [ctx dir]
  (let [{:keys [exit err]} (run ctx ["status"] :dir dir :check? false)]
    (when (and (not (zero? exit)) (str/includes? (str err) "stale"))
      (run ctx ["workspace" "update-stale"] :dir dir :check? false)
      (run ctx ["status"] :dir dir :check? false))))

(defn update-stale
  "Materialize the current commit into the workspace at `dir`. Safe to call
   when not stale (jj no-ops). This is how the composite reaches the dev
   server's disk — no file-copy code anywhere (spec §7)."
  [ctx dir]
  (run ctx ["workspace" "update-stale"] :dir dir :check? false))

;; ── serialized queue: the daemon's one synchronization primitive ──────────

(defn queue
  "Start a single-consumer jj queue. Returns {:submit fn :stop fn}.

   `submit` takes a thunk (usually a call into this namespace), runs it on the
   one consumer thread, and returns its value — rethrowing any exception on
   the calling thread. One consumer ⇒ every jj mutation is serialized with no
   lock to acquire or forget, and FIFO by construction. The composite can
   never be written mid-rewrite because the write and the rewrite are the same
   queue's items, run one at a time."
  []
  (let [ch (async/chan 1024)]
    (async/thread
      (loop []
        (when-let [[thunk p] (async/<!! ch)]
          (deliver p (try {:val (thunk)} (catch Throwable t {:ex t})))
          (recur))))
    {:ch ch
     :submit (fn [thunk]
               (let [p (promise)]
                 (async/>!! ch [thunk p])
                 (let [{:keys [val ex]} @p]
                   (if ex (throw ex) val))))
     :stop (fn [] (async/close! ch))}))
