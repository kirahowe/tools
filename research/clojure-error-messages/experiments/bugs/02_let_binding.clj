(ns bugs.let-binding)

(defn greet [name]
  (let [greeting "Hello" name]
    (str greeting ", " name)))
