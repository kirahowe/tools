(ns retirement.test-runner
  "Runs the whole suite: clojure -M:test, or plain java -cp (see bin/test)."
  (:require [clojure.test :as t]
            [retirement.accounts-test]
            [retirement.benefits-test]
            [retirement.inputs-test]
            [retirement.optimize-test]
            [retirement.plan-test]
            [retirement.simulate-test]
            [retirement.tax-test]))

(def test-namespaces
  ['retirement.accounts-test
   'retirement.benefits-test
   'retirement.inputs-test
   'retirement.optimize-test
   'retirement.plan-test
   'retirement.simulate-test
   'retirement.tax-test])

(defn -main [& _args]
  (let [{:keys [fail error]} (apply t/run-tests test-namespaces)]
    (shutdown-agents)
    (System/exit (if (zero? (+ fail error)) 0 1))))
