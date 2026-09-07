(ns bugs.lazy)

(defn parse-all [xs]
  (map #(Integer/parseInt %) xs))

(def parsed (parse-all ["1" "2" "three"]))

(println "sum:" (reduce + parsed))
