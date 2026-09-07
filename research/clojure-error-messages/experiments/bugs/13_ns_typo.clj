(ns bugs.ns-typo
  (:require [clojure.strng :as str]))

(str/join "," [1 2 3])
