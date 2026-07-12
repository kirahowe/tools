(ns compositor.store
  "Session-record persistence. Authoritative only for what the repo can't know:
   intent text, the short human id, tmux window names, state-machine timestamps
   (review §5). `:applied` and conflict info are derived caches — reconcile.clj
   recomputes them from the repo.

   M0–M1: a single EDN file guarded by an agent-serialized atom, so the store
   has no external dependency and no pod download on the critical path. The
   read-state/write-state boundary is the seam it swaps across to the
   pod-babashka-go-sqlite3 pod at M2, with no caller changes."
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
            [compositor.config :as config]))

;; A store value: {:next-id 1 :sessions {1 {..session..}}}
(def ^:private empty-store {:next-id 1 :sessions {}})

(defn open
  "Open (or create) the store for a project state dir. Returns a handle."
  [state]
  (let [f     (config/state-file state)
        init  (if (fs/exists? f) (edn/read-string (slurp f)) empty-store)]
    (fs/create-dirs state)
    {:file f :cache (atom init)}))

(defn- persist! [{:keys [file cache]}]
  (spit file (pr-str @cache))
  @cache)

(defn sessions [store] (vals (:sessions @(:cache store))))
(defn session  [store id] (get-in @(:cache store) [:sessions id]))

(defn add-session!
  "Create a session record from a partial map, assigning the next short id.
   Returns the stored session."
  [store session]
  (let [{:keys [cache]} store
        s (swap! cache
                 (fn [st]
                   (let [id (:next-id st)]
                     (-> st
                         (assoc-in [:sessions id] (assoc session :id id))
                         (assoc :next-id (inc id))))))]
    (persist! store)
    (get-in s [:sessions (dec (:next-id s))])))

(defn update-session!
  "Apply `f` to session `id` (f session -> session), persist, return it."
  [store id f]
  (let [st (swap! (:cache store) update-in [:sessions id] f)]
    (persist! store)
    (get-in st [:sessions id])))

(defn remove-session!
  [store id]
  (swap! (:cache store) update :sessions dissoc id)
  (persist! store))

(defn applied-sessions
  "Live sessions currently applied to the megamerge (a derived view)."
  [store]
  (filter :applied (sessions store)))
