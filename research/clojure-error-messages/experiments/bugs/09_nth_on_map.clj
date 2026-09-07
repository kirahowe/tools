(ns bugs.nth-on-map)

(def user {:name "Ada" :age 36})

(defn first-field [m]
  (nth m 0))

(first-field user)
