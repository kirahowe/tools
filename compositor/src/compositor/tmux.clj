(ns compositor.tmux
  "tmux driver on a dedicated socket (`tmux -L compositor` by default). tmux is
   the pty host and the supervisor: window 0 = daemon, window 1 = dev server,
   sessions get windows 2+. We never render terminal cells — the user attaches
   with their own terminal (spec §3). Always socket-scoped so we never collide
   with the user's own tmux or nest inside it (review §12)."
  (:require [babashka.process :as p]
            [clojure.string :as str]))

;; The compositor's own tmux session name (inside the -L compositor socket).
(def session-name "compositor")

(defn- base
  "Split the configured mux string (e.g. \"tmux -L compositor\") into argv."
  [cfg] (str/split (:mux cfg "tmux -L compositor") #"\s+"))

(defn- tmux
  [cfg args & {:keys [check?] :or {check? true}}]
  (let [res (apply p/shell {:out :string :err :string :continue true}
                   (concat (base cfg) args))]
    (when (and check? (not (zero? (:exit res))))
      (throw (ex-info (str "tmux failed: " (str/join " " args))
                      {:exit (:exit res) :err (:err res)})))
    res))

(defn running?
  "Is the compositor tmux session up?"
  [cfg]
  (zero? (:exit (tmux cfg ["has-session" "-t" session-name] :check? false))))

(defn ensure-session
  "Create the compositor session (detached) with a first window if absent."
  [cfg {:keys [window dir cmd]}]
  (when-not (running? cfg)
    (tmux cfg (cond-> ["new-session" "-d" "-s" session-name "-n" (or window "daemon")]
                dir (into ["-c" dir])
                cmd (into [cmd])))))

(defn new-window
  "Open a window named `wname` in `dir` running `cmd`. `cmd` is a single argv
   string passed to the shell tmux spawns; the agent's initial prompt travels
   as part of `cmd` (argv), never via send-keys (review F9)."
  [cfg {:keys [wname dir cmd]}]
  (tmux cfg (cond-> ["new-window" "-t" session-name "-n" wname]
              dir (into ["-c" dir])
              cmd (into [cmd]))))

(defn kill-window [cfg wname]
  (tmux cfg ["kill-window" "-t" (str session-name ":" wname)] :check? false))

(defn send-text
  "Type literal text into a window then Enter. Reserved for send-back, where a
   human is watching — not for the opening prompt (review F9). `-l` keeps text
   literal so quotes/newlines aren't reinterpreted."
  [cfg wname text]
  (tmux cfg ["send-keys" "-t" (str session-name ":" wname) "-l" text])
  (tmux cfg ["send-keys" "-t" (str session-name ":" wname) "Enter"]))

(defn list-windows
  "Window names currently in the compositor session."
  [cfg]
  (let [{:keys [out exit]} (tmux cfg ["list-windows" "-t" session-name
                                      "-F" "#{window_name}"] :check? false)]
    (if (zero? exit) (remove str/blank? (str/split-lines out)) [])))

(defn window-activity
  "Seconds since last activity in `wname`, or nil. Feeds the quiescence
   fallback for agents without a done-hook (review F8)."
  [cfg wname]
  (let [{:keys [out exit]} (tmux cfg ["display-message" "-p" "-t"
                                      (str session-name ":" wname)
                                      "#{window_activity}"] :check? false)]
    (when (zero? exit)
      (some-> out str/trim not-empty parse-long))))
