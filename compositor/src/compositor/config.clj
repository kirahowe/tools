(ns compositor.config
  "Project configuration and the paths that hang off it. Every value here is
   somebody else's program (spec §5) — dev server, agent, warmup — which is
   the product boundary: we own only the composite.

   State lives outside the repo, keyed by a hash of the repo's canonical path,
   so agents can't see each other and our watchers don't recurse into the
   repo (review F14):
     <XDG_STATE_HOME>/compositor/<hash>/
       config.edn        this map
       state.edn         session records (store.clj)
       daemon.port       the localhost port the daemon is listening on
       daemon.log
       ws/<sid>          session workspaces
       ws/dev            the dev (composite) workspace"
  (:require [babashka.fs :as fs]
            [clojure.string :as str])
  (:import [java.security MessageDigest]))

(def defaults
  {:trunk           "main"
   :dev-server-cmd  "npm run dev"
   :agent-cmd       "claude {prompt}"     ; {prompt} = the intent, passed as argv (review F9)
   :warmup-cmd      nil
   :open-app-cmd    "open http://localhost:3000"
   :mux             "tmux -L compositor"
   ;; A session whose diff touches any of these is exclusive (spec §9, F13).
   ;; Lockfiles are migrations in disguise: two `npm install`s collide forever.
   :exclusive-globs ["**/migrations/**"
                     "**/package-lock.json" "**/pnpm-lock.yaml" "**/yarn.lock"
                     "**/Cargo.lock" "**/poetry.lock" "**/deps.edn"]
   :db-snapshot-cmd nil
   :db-restore-cmd  nil
   :agent-done-hook "none"})               ; "claude" | "codex" | "none" (F8)

(defn- sha256-hex [^String s]
  (let [d (.digest (MessageDigest/getInstance "SHA-256") (.getBytes s "UTF-8"))]
    (apply str (map #(format "%02x" %) d))))

(defn repo-root
  "Canonical absolute path of the repo containing `dir` (defaults to cwd).
   Requires a .jj or .git marker; throws with a friendly message otherwise."
  ([] (repo-root (fs/cwd)))
  ([dir]
   (loop [d (fs/canonicalize dir)]
     (cond
       (or (fs/exists? (fs/path d ".jj")) (fs/exists? (fs/path d ".git")))
       (str d)
       (nil? (fs/parent d))
       (throw (ex-info "Not inside a repo (no .jj or .git found)."
                       {:from (str dir)}))
       :else (recur (fs/parent d))))))

(defn state-root
  "The per-project state directory for `repo` (created on demand)."
  [repo]
  (let [base (or (System/getenv "XDG_STATE_HOME")
                 (str (fs/path (fs/home) ".local" "state")))]
    (str (fs/path base "compositor" (subs (sha256-hex repo) 0 16)))))

;; Path helpers — one place so nobody hardcodes layout.
(defn config-file [state]  (str (fs/path state "config.edn")))
(defn state-file  [state]  (str (fs/path state "state.edn")))
(defn port-file   [state]  (str (fs/path state "daemon.port")))
(defn log-file    [state]  (str (fs/path state "daemon.log")))
(defn ws-root     [state]  (str (fs/path state "ws")))
(defn ws-dir      [state sid] (str (fs/path state "ws" (str sid))))
(defn dev-ws-dir  [state]  (str (fs/path state "ws" "dev")))

(defn validate
  "Minimal contract check. Returns cfg or throws ex-info listing problems."
  [cfg]
  (let [problems (cond-> []
                   (str/blank? (:trunk cfg))          (conj ":trunk is required")
                   (str/blank? (:dev-server-cmd cfg)) (conj ":dev-server-cmd is required")
                   (not (str/includes? (:agent-cmd cfg) "{prompt}"))
                   (conj ":agent-cmd must contain {prompt}")
                   (not (contains? #{"claude" "codex" "none"} (:agent-done-hook cfg)))
                   (conj ":agent-done-hook must be claude|codex|none"))]
    (if (seq problems)
      (throw (ex-info (str "Invalid config: " (str/join "; " problems)) {:problems problems}))
      cfg)))

(defn load-config [state]
  (let [f (config-file state)]
    (when (fs/exists? f)
      (validate (read-string (slurp f))))))

(defn save-config [state cfg]
  (fs/create-dirs state)
  (spit (config-file state) (pr-str (validate cfg)))
  cfg)

(defn context
  "The jj context (see jj.clj) for a project: {:repo :trunk}."
  [repo cfg]
  {:repo repo :trunk (:trunk cfg)})
