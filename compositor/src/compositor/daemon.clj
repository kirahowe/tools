(ns compositor.daemon
  "The daemon: the small heart of the app (spec §7). Runs in tmux window 0.

   Concurrency model (docs/concurrency-notes.md): one jj queue is the only
   synchronization primitive. Three kinds of thread feed it:
     - the socket accept loop (one thread per client connection),
     - the watch loop (polls session workspaces, debounces, submits snapshots),
   and both funnel every repo mutation through `(submit thunk)`, which runs on
   the single jj-consumer thread. Serialization falls out of single-consumer —
   no lock, FIFO order — so the composite is never written mid-rewrite.

   Discipline: outer threads call `submit`; the submitted thunk does the whole
   operation by calling session/graph directly. Thunks must never call submit
   (that would deadlock the consumer on itself)."
  (:require [compositor.jj :as jj]
            [compositor.graph :as graph]
            [compositor.session :as session]
            [compositor.store :as store]
            [compositor.config :as config]
            [clojure.core.async :as async]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.net ServerSocket Socket InetAddress]
           [java.io File]))

;; ── watch loop (M1 polling; upgrades to the fswatcher pod later) ────────────

(def ^:private ignored-dirs #{".jj" ".git" "node_modules" ".next" "dist"
                              "target" ".turbo" "build" ".parcel-cache"})

(defn- fingerprint
  "Max last-modified millis under dir, skipping VCS/build dirs. A cheap change
   detector; the fswatcher pod replaces this when github is reachable."
  [dir]
  (loop [stack (list (io/file dir)) mx 0]
    (if-let [^File f (first stack)]
      (let [stack (rest stack)]
        (cond
          (and (.isDirectory f) (ignored-dirs (.getName f))) (recur stack mx)
          (.isDirectory f) (recur (into stack (or (.listFiles f) [])) mx)
          :else (recur stack (max mx (.lastModified f)))))
      mx)))

(defn- snapshot-and-compose!
  "Runs on the jj-consumer thread (via submit): snapshot the session workspace,
   refresh its touched-files cache, then rebuild + materialize the composite."
  [{:keys [ctx store] :as proj} session]
  (jj/snapshot! ctx (:workspace session))
  (let [files (session/files-touched ctx (:change-id session))]
    (store/update-session! store (:id session) #(assoc % :files-touched files)))
  (graph/refresh-then-materialize! proj))

(defn- start-watch!
  "Poll running sessions; on a workspace that changed and then went quiet for
   `debounce` ms, submit a snapshot+compose. Returns a stop! fn."
  [{:keys [store] :as proj} submit {:keys [poll debounce] :or {poll 250 debounce 500}}]
  (let [running? (atom true)
        fps      (atom {})]                         ; sid -> {:fp .. :dirty-at ..}
    (async/thread
      (while @running?
        (doseq [s (filter #(= "running" (:state %)) (store/sessions store))]
          (let [sid  (:id s)
                fp   (fingerprint (:workspace s))
                prev (get @fps sid)
                now  (System/currentTimeMillis)]
            (cond
              (nil? prev)            (swap! fps assoc sid {:fp fp :dirty-at nil})
              (not= fp (:fp prev))   (swap! fps assoc sid {:fp fp :dirty-at now})
              (and (:dirty-at prev)
                   (>= (- now (:dirty-at prev)) debounce))
              (do (try (submit #(snapshot-and-compose! proj s))
                       (catch Throwable t
                         (store/update-session! store sid #(assoc % :last-error (ex-message t)))))
                  (swap! fps assoc sid {:fp fp :dirty-at nil})))))
        (Thread/sleep poll)))
    (fn [] (reset! running? false))))

;; ── request handling ─────────────────────────────────────────────────────────

(defn- handle
  "Dispatch one client request. Mutations go through `submit` so they serialize
   with the watch loop's snapshots."
  [proj submit {:keys [cmd args]}]
  (try
    (case cmd
      "ping"   {:ok "pong"}
      "ls"     {:ok (vec (sort-by :id (store/sessions (:store proj))))}
      "status" {:ok {:sessions (vec (sort-by :id (store/sessions (:store proj))))
                     :megamerge (graph/megamerge-id proj)
                     :collisions (graph/collisions proj)}}
      "new"    {:ok (submit #(session/create! proj (first args)))}
      "toggle" {:ok (submit #(session/toggle! proj (parse-long (str (first args)))))}
      "keep"   {:ok (submit #(session/keep!  proj (parse-long (str (first args)))))}
      "drop"   {:ok (submit #(session/drop!  proj (parse-long (str (first args)))))}
      {:error (str "unknown command: " cmd)})
    (catch Throwable t
      {:error (ex-message t) :data (ex-data t)})))

(defn- serve!
  "Bind a localhost TCP socket, write the chosen port to daemon.port, and
   accept connections forever (each on its own thread). Returns a stop! fn."
  [{:keys [state] :as proj} submit]
  (let [ss   (ServerSocket. 0 50 (InetAddress/getByName "127.0.0.1"))
        port (.getLocalPort ss)]
    (spit (config/port-file state) (str port))
    (async/thread
      (try
        (loop []
          (let [sock (.accept ss)]
            (async/thread
              (with-open [^Socket s sock]
                (let [in  (io/reader s)
                      out (io/writer s)
                      req (edn/read-string (.readLine in))
                      res (handle proj submit req)]
                  (.write out (pr-str res)) (.write out "\n") (.flush out)))))
          (recur))
        (catch java.net.SocketException _ :closed)))    ; ss closed on stop
    {:port port :stop! (fn [] (.close ss))}))

;; ── lifecycle ────────────────────────────────────────────────────────────────

(defn start
  "Start the daemon and block (it is `comp _daemon`, foreground in window 0).
   Reconcile-on-boot (review F10) lands here at M2; for M1 the store is
   trusted as-is."
  [proj]
  (let [{:keys [submit stop]} (jj/queue)
        {:keys [port stop!]}  (serve! proj submit)
        stop-watch            (start-watch! proj submit {})]
    (println (str "compositor daemon up on 127.0.0.1:" port
                  "  (attach: tmux -L compositor attach)"))
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. ^Runnable (fn [] (stop-watch) (stop!) (stop))))
    @(promise)))                                        ; block forever
