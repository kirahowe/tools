(ns bugs.deep)

(defn parse-row [row]
  (update row :qty #(Integer/parseInt %)))

(defn load-rows [rows]
  (mapv parse-row rows))

(defn total-qty [rows]
  (transduce (map :qty) + (load-rows rows)))

(defn -main []
  (println (total-qty [{:qty "3"} {:qty "4"} {:qty nil}])))

(-main)
