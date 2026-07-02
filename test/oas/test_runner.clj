(ns oas.test-runner
  "Zero-dependency test entry point: clojure -M:test"
  (:require [clojure.test :as t]
            [oas.core-test]
            [oas.gis-test]
            [oas.rates-test]))

(defn -main [& _]
  (let [{:keys [fail error]} (t/run-tests 'oas.core-test 'oas.gis-test 'oas.rates-test)]
    (System/exit (if (zero? (+ fail error)) 0 1))))
