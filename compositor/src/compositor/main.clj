(ns compositor.main
  "CLI entry + dispatch. `comp <cmd>`:
     init                     set up the project, launch daemon + dev server
     new \"<intent>\"           spawn a session (agent in its own window)
     ls                       list sessions
     toggle <id>              apply/unapply — the app changes under your cursor
     keep <id>                land it on trunk
     drop <id>                abandon it
     app                      open the stage in the browser
     _daemon                  (internal) run the daemon loop

   Mutating commands go over the daemon socket so they serialize with the watch
   loop. `ls`/`app` are the exceptions: `ls` reads the socket too (single source
   of truth is the running daemon); `app` and `init` are local."
  (:require [compositor.config :as config]
            [compositor.store :as store]
            [compositor.jj :as jj]
            [compositor.tmux :as tmux]
            [compositor.graph :as graph]
            [compositor.daemon :as daemon]
            [babashka.fs :as fs]
            [babashka.process :as p]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.java.io :as io])
  (:import [java.net Socket]))

(defn- load-proj
  "Build the {:ctx :cfg :store :state} map for the repo containing cwd."
  []
  (let [repo  (config/repo-root)
        state (config/state-root repo)
        cfg   (or (config/load-config state)
                  (throw (ex-info "Not initialized here — run `comp init`." {:repo repo})))]
    {:ctx (config/context repo cfg) :cfg cfg
     :store (store/open state) :state state :repo repo}))

;; ── client transport ─────────────────────────────────────────────────────────

(defn- send-daemon [state req]
  (let [pf (config/port-file state)]
    (when-not (fs/exists? pf)
      (throw (ex-info "Daemon not running (no daemon.port). Run `comp init`." {})))
    (let [port (parse-long (str/trim (slurp pf)))]
      (with-open [sock (Socket. "127.0.0.1" (int port))]
        (let [out (io/writer sock) in (io/reader sock)]
          (.write out (pr-str req)) (.write out "\n") (.flush out)
          (edn/read-string (.readLine in)))))))

(defn- reply! [{:keys [ok error data]}]
  (if error
    (binding [*out* *err*] (println "✗" error) (System/exit 1))
    (do (when (some? ok) (prn ok)) ok)))

;; ── init ─────────────────────────────────────────────────────────────────────

(defn- check-gitignore
  "Warn (don't fail) if build output / dev DB likely isn't ignored — jj would
   snapshot it into the composite and warn on every large file (review F11)."
  [repo]
  (let [gi (fs/path repo ".gitignore")
        txt (if (fs/exists? gi) (slurp (str gi)) "")
        missing (remove #(str/includes? txt %) ["node_modules"])]
    (when (seq missing)
      (binding [*out* *err*]
        (println "⚠ .gitignore may not cover:" (str/join ", " missing)
                 "— build output can pollute the composite. Add it before heavy use.")))))

(defn cmd-init [args]
  (let [repo  (config/repo-root)
        state (config/state-root repo)
        cfg   (config/save-config state (merge config/defaults (config/load-config state)))
        ctx   (config/context repo cfg)
        proj  {:ctx ctx :cfg cfg :store (store/open state) :state state :repo repo}]
    (when-not (fs/exists? (fs/path repo ".jj"))
      (jj/git-init ctx))
    ;; persist immutability into repo config so the user's own jj is guarded too (F3)
    (jj/run ctx ["config" "set" "--repo"
                 "revset-aliases.\"immutable_heads()\"" (str "present(" (:trunk cfg) ")")]
            :check? false)
    (check-gitignore repo)
    (graph/init! proj)
    (tmux/ensure-session cfg {:window "daemon" :dir (:repo proj) :cmd "comp _daemon"})
    (tmux/new-window cfg {:wname "dev" :dir (config/dev-ws-dir state) :cmd (:dev-server-cmd cfg)})
    (println "✓ Initialized. Daemon + dev server are running in tmux -L compositor.")
    (println "  Attach:  tmux -L compositor attach")
    (println "  Then:    comp new \"add a dark mode toggle\"")))

;; ── ls rendering ─────────────────────────────────────────────────────────────

(defn- print-sessions [sessions]
  (if (empty? sessions)
    (println "(no sessions — `comp new \"...\"` to start one)")
    (do
      (printf "%-3s %-9s %-4s %-13s %s%n" "id" "state" "on?" "change" "intent")
      (doseq [s sessions]
        (let [c (:change-id s)]
          (printf "%-3s %-9s %-4s %-13s %s%n"
                  (:id s) (:state s) (if (:applied s) "▣" "☐")
                  (if c (subs c 0 (min 12 (count c))) "-")
                  (:intent s)))))))

;; ── dispatch ─────────────────────────────────────────────────────────────────

(defn -main [& args]
  (let [[cmd & more] args]
    (case cmd
      "init"    (cmd-init more)
      "_daemon" (daemon/start (load-proj))
      "new"     (reply! (send-daemon (:state (load-proj)) {:cmd "new" :args [(str/join " " more)]}))
      "ls"      (print-sessions (:ok (send-daemon (:state (load-proj)) {:cmd "ls"})))
      "toggle"  (reply! (send-daemon (:state (load-proj)) {:cmd "toggle" :args (vec more)}))
      "keep"    (reply! (send-daemon (:state (load-proj)) {:cmd "keep"   :args (vec more)}))
      "drop"    (reply! (send-daemon (:state (load-proj)) {:cmd "drop"   :args (vec more)}))
      "app"     (let [{:keys [cfg]} (load-proj)]
                  (p/shell {:continue true} "bash" "-c" (:open-app-cmd cfg)))
      (do (binding [*out* *err*]
            (println "usage: comp <init|new|ls|toggle|keep|drop|app>"))
          (System/exit 2)))))
