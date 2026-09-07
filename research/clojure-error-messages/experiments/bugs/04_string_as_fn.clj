(ns bugs.string-as-fn)

(defn shout [s]
  (s "!"))          ; meant (str s "!")

(shout "hello")
