(ns retirement.test-util)

(defn approx=
  "Absolute-tolerance float comparison."
  ([a b] (approx= a b 0.01))
  ([a b tol] (< (abs (- (double a) (double b))) (double tol))))

(def basic-inputs
  "A representative single-retiree scenario used across tests."
  {:person {:age 65 :province :on
            :cpp {:start-age 70 :at-65 12000}
            :oas {:start-age 65}}
   :accounts [{:id :rrsp :type :rrsp :balance 500000
               :holdings {:equity 0.6 :bonds 0.4}}
              {:id :tfsa :type :tfsa :balance 120000
               :holdings {:equity 0.8 :bonds 0.2}}
              {:id :taxable :type :non-registered :balance 250000
               :acb 180000 :holdings {:equity 0.7 :bonds 0.3}}]
   :goal {:type :spend-down :annual-spending 55000}
   :start-year 2026})
