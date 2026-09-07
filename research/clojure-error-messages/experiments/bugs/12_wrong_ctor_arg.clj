(ns bugs.wrong-ctor-arg
  (:import [java.io File]))

(defn file-of [x]
  (File. x))

(file-of 42)
