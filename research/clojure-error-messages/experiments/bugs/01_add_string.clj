(ns bugs.add-string)

(defn total [prices]
  (reduce + prices))

(total [10 20 "30"])
