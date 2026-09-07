(ns bugs.inc-nil)

(def config {:port 8080})

(defn next-port [c]
  (inc (:prot c)))       ; typo: :prot

(next-port config)
