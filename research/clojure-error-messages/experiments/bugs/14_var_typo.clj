(ns bugs.var-typo
  (:require [clojure.string :as str]))

(defn csv [xs]
  (str/joinn "," xs))
