(ns compositor.graph
  "The composite: one megamerge commit whose parents are trunk + every applied
   session, with the dev workspace's @ (SCRATCH) as its child. Created once at
   init and mutated in place forever via `jj rebase -s` — never recreated, so
   its change id is stable (review F2). Toggling a session on/off is just a new
   parent set. The dev workspace's `update-stale` is the only thing that writes
   the composite to disk (spec §7).

   Every fn takes a `proj` map: {:ctx :cfg :store :state}. `:ctx` is the jj
   context, `:state` the project state dir."
  (:require [compositor.jj :as jj]
            [compositor.store :as store]
            [compositor.config :as config]
            [babashka.fs :as fs]))

(defn megamerge-id
  "Stable change id of the megamerge. Stored at init; falls back to the revset
   `dev@-` (parent of the scratch working copy), which also always resolves
   to it."
  [{:keys [ctx store]}]
  (or (:megamerge @(:cache store))
      (jj/change-id ctx "dev@-")))

(defn init!
  "Create the megamerge (parented on trunk only) and the dev workspace whose @
   is its scratch child. Returns the megamerge change id, recorded in the store."
  [{:keys [ctx cfg store state] :as proj}]
  (let [trunk (:trunk cfg)
        mm    (jj/new-commit ctx [trunk] "compositor: composite")
        devwc (config/dev-ws-dir state)]
    (fs/create-dirs (config/ws-root state))
    ;; dev workspace @ is the auto-created empty child of the megamerge = SCRATCH
    (jj/workspace-add ctx "dev" devwc mm)
    (swap! (:cache store) assoc :megamerge mm)
    (spit (config/state-file state) (pr-str @(:cache store)))
    mm))

(defn applied-parents
  "The megamerge's parent set: trunk + each applied session's change id."
  [{:keys [cfg store]}]
  (into [(:trunk cfg)] (map :change-id (store/applied-sessions store))))

(defn rebuild!
  "Re-point the megamerge at the current applied set (in place, id-stable).
   Moves scratch/dev@ along automatically. Returns {:megamerge id
   :conflicted? bool}. Does NOT touch the dev workspace on disk — that's
   `materialize!`, gated on conflict (review F6)."
  [{:keys [ctx] :as proj}]
  (let [mm (megamerge-id proj)]
    (jj/rebase ctx mm (applied-parents proj))
    {:megamerge mm :conflicted? (jj/conflicted? ctx mm)}))

(defn materialize!
  "Write the composite to the dev workspace so the dev server's hot reload
   fires — but ONLY if the megamerge is conflict-free. A conflicted composite
   must never be snapshotted or its markers become literal source (review F6);
   we leave the last good composite on disk and let the caller surface the
   collision. Returns {:materialized? bool :conflicted? bool}."
  [{:keys [ctx state] :as proj}]
  (let [mm (megamerge-id proj)
        conflicted? (jj/conflicted? ctx mm)]
    (if conflicted?
      {:materialized? false :conflicted? true}
      (do (jj/update-stale ctx (config/dev-ws-dir state))
          {:materialized? true :conflicted? false}))))

(defn refresh-then-materialize!
  "The daemon's composite step: rebuild the parent set, then materialize if
   clean. Returns the merged result map."
  [proj]
  (merge (rebuild! proj) (materialize! proj)))

(defn collisions
  "Map of conflicted file -> ids of sessions whose touched-file set includes
   it. Cheap attribution for a badge (review F12): intersect `jj resolve
   --list` with each session's cached :files-touched."
  [{:keys [ctx store] :as proj}]
  (let [files (jj/conflicted-files ctx (megamerge-id proj))]
    (into {}
          (for [f files]
            [f (->> (store/sessions store)
                    (filter #(some #{f} (:files-touched %)))
                    (map :id))]))))
