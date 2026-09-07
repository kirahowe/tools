(ns bugs.nil-interop)

(defn shout [s]
  (.toUpperCase s))

(shout nil)
