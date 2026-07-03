(ns retirement.plan-test
  (:require [clojure.test :refer [deftest is testing]]
            [retirement.plan :as plan]
            [retirement.test-util :refer [approx= basic-inputs]]))

(def no-benefits
  "Suppress CPP (no config) and OAS (zero residency fraction) so cash flows
  come purely from accounts."
  {:oas {:start-age 65 :fraction 0.0}})

(deftest tfsa-only-plan-pays-no-tax
  (let [inputs {:person (merge {:age 65 :province :on :end-age 94} no-benefits)
                :accounts [{:id :tfsa :type :tfsa :balance 800000.0
                            :holdings {:equity 0.6 :bonds 0.4}}]
                :goal {:type :spend-down :annual-spending 30000}
                :start-year 2026}
        result (plan/run-plan inputs)]
    (is (= 30 (count (:years result))))
    (testing "every year: TFSA-only withdrawals, zero tax, target met exactly"
      (doseq [row (:years result)]
        (is (= [:tfsa] (keys (:withdrawals row))))
        (is (approx= 0.0 (get-in row [:tax :total])))
        (is (approx= (:spending-target row) (:net-cash row) 0.5))
        (is (zero? (:shortfall row)))))
    (testing "nominal withdrawal grows with inflation"
      (let [first-wd (get-in (first (:years result)) [:withdrawals :tfsa])
            late-wd (get-in (nth (:years result) 20) [:withdrawals :tfsa])]
        (is (approx= 30000.0 first-wd 0.5))
        (is (> late-wd first-wd))))
    (is (get-in result [:summary :success?]))))

(deftest rrsp-converts-and-forced-minimums-apply
  (let [inputs {:person (merge {:age 70 :province :on :end-age 90} no-benefits)
                :accounts [{:id :rrsp :type :rrsp :balance 1000000.0
                            :holdings {:equity 0.5 :bonds 0.5}}]
                :goal {:type :spend-down :annual-spending 20000}
                :start-year 2026}
        result (plan/run-plan inputs)
        by-age (into {} (map (juxt :age identity)) (:years result))]
    (testing "no forced minimum at 70 or 71"
      (is (empty? (:rrif-minimums (by-age 70))))
      (is (empty? (:rrif-minimums (by-age 71)))))
    (testing "first forced minimum at 72 at 5.40% of start-of-year balance"
      (let [row (by-age 72)
            balance-71-end (get-in (by-age 71) [:balances-end :rrsp])]
        (is (approx= (* 0.054 balance-71-end)
                     (get-in row [:rrif-minimums :rrsp])
                     1.0))))
    (testing "forced minimum beyond spending is reinvested, not lost"
      (let [row (by-age 75)]
        (is (> (:net-cash row) (:spending-target row)))
        (is (pos? (+ (get-in row [:surplus-reinvested :tfsa] 0.0)
                     (get-in row [:surplus-reinvested :non-registered] 0.0))))))
    (testing "reinvested surplus shows up as a new non-registered account"
      (is (pos? (get-in (last (:years result)) [:balances-end :reinvested] 0.0))))))

(deftest conventional-order-respected
  ;; Spend enough that the plan has to work through all three accounts.
  (let [result (plan/run-plan (assoc-in basic-inputs
                                        [:goal :annual-spending] 68000))
        first-row (first (:years result))]
    (testing "first year draws only from the taxable account"
      (is (= [:taxable] (keys (:withdrawals first-row)))))
    (testing "TFSA is touched only after taxable and registered are gone"
      (let [first-tfsa-age (some #(when (pos? (get-in % [:withdrawals :tfsa] 0.0))
                                    (:age %))
                                 (:years result))
            row (first (filter #(= (:age %) first-tfsa-age) (:years result)))]
        (is (some? first-tfsa-age))
        (is (< (get-in row [:balances-end :taxable] 0.0) 1.0))
        (is (< (get-in row [:balances-end :rrsp] 0.0) 1.0))))))

(deftest cash-conservation-every-year
  (let [result (plan/run-plan basic-inputs)]
    (doseq [row (:years result)]
      (let [{:keys [net-cash spending-target shortfall]} row
            surplus (+ (get-in row [:surplus-reinvested :tfsa] 0.0)
                       (get-in row [:surplus-reinvested :non-registered] 0.0))]
        (testing "net cash = spending + surplus - shortfall"
          (is (approx= net-cash (+ spending-target surplus (- shortfall)) 1.0)))))))

(deftest bracket-fill-tops-up-taxable-income
  (let [inputs {:person (merge {:age 65 :province :on :end-age 94} no-benefits)
                :accounts [{:id :rrsp :type :rrsp :balance 1000000.0
                            :holdings {:equity 0.5 :bonds 0.5}}]
                :goal {:type :spend-down :annual-spending 30000}
                :strategy {:type :bracket-fill :ceiling :first-bracket-top}
                :start-year 2026}
        result (plan/run-plan inputs)
        row (first (:years result))]
    (testing "taxable income lands on the published 2026 first-bracket top"
      ;; The 2026 plan year resolves the 2026 tax table at factor 1.0, so
      ;; the ceiling is exactly the published $58,523 threshold.
      (is (approx= 58523.0
                   (get-in row [:tax :taxable-income])
                   1.0)))
    (testing "the excess over spending is re-sheltered"
      (is (pos? (+ (get-in row [:surplus-reinvested :tfsa] 0.0)
                   (get-in row [:surplus-reinvested :non-registered] 0.0)))))))

(deftest shortfall-recorded-when-money-runs-out
  (let [inputs {:person (merge {:age 65 :province :on :end-age 90} no-benefits)
                :accounts [{:id :tfsa :type :tfsa :balance 100000.0
                            :holdings {:equity 0.5 :bonds 0.5}}]
                :goal {:type :spend-down :annual-spending 50000}
                :start-year 2026}
        result (plan/run-plan inputs)]
    (is (not (get-in result [:summary :success?])))
    (is (pos? (get-in result [:summary :total-shortfall-real])))
    (is (<= 66 (get-in result [:summary :first-shortfall-age]) 70))
    (testing "accounts drained to zero in the shortfall years"
      (is (approx= 0.0 (:total-balance-end (last (:years result))) 0.01)))))

(deftest pensions-reduce-withdrawal-needs
  (let [base {:person (merge {:age 65 :province :on} no-benefits)
              :accounts [{:id :rrif :type :rrif :balance 500000.0
                          :holdings {:equity 0.5 :bonds 0.5}}]
              :goal {:type :spend-down :annual-spending 50000}
              :start-year 2026}
        without (plan/run-plan base)
        with (plan/run-plan (assoc-in base [:person :pensions]
                                      [{:annual 30000 :start-age 65}]))]
    (is (< (get-in (first (:years with)) [:withdrawals :rrif])
           (get-in (first (:years without)) [:withdrawals :rrif])))))

(deftest oas-clawback-applied-at-high-income
  (let [inputs {:person {:age 72 :province :on :oas {:start-age 65}
                         :pensions [{:annual 150000 :start-age 65}]}
                :accounts [{:id :rrif :type :rrif :balance 2000000.0
                            :holdings {:equity 0.5 :bonds 0.5}}]
                :goal {:type :spend-down :annual-spending 100000}
                :start-year 2026}
        row (first (:years (plan/run-plan inputs)))]
    (testing "OAS fully clawed back at this income"
      (is (approx= (get-in row [:benefits :oas])
                   (get-in row [:tax :oas-clawback])
                   1.0))
      (is (approx= 0.0 (get-in row [:benefits :oas-net]) 1.0)))))

(deftest gis-supports-low-income-years
  ;; Deferring CPP and living off TFSA/taxable keeps taxable income near
  ;; zero, so GIS should appear alongside OAS.
  (let [inputs {:person {:age 65 :province :on :oas {:start-age 65}
                         :cpp {:start-age 70 :at-65 10000}}
                :accounts [{:id :tfsa :type :tfsa :balance 400000.0
                            :holdings {:equity 0.5 :bonds 0.5}}]
                :goal {:type :spend-down :annual-spending 30000}
                :start-year 2026}
        rows (:years (plan/run-plan inputs))
        by-age (into {} (map (juxt :age identity)) rows)]
    (testing "GIS received while income is only OAS"
      (is (pos? (get-in (by-age 65) [:benefits :gis]))))
    (testing "GIS shrinks once CPP starts"
      (is (< (get-in (by-age 70) [:benefits :gis])
             (get-in (by-age 65) [:benefits :gis]))))))

(deftest legacy-goal-checked-against-estate
  (let [base {:person (merge {:age 65 :province :on :end-age 85} no-benefits)
              :accounts [{:id :tfsa :type :tfsa :balance 2000000.0
                          :holdings {:equity 0.6 :bonds 0.4}}]
              :goal {:type :legacy :annual-spending 40000 :legacy 500000}
              :start-year 2026}
        rich (plan/run-plan base)
        poor (plan/run-plan (assoc base :goal {:type :legacy
                                               :annual-spending 40000
                                               :legacy 5000000}))]
    (is (get-in rich [:summary :success?]))
    (is (not (get-in poor [:summary :success?])))))

(deftest estate-taxes-registered-balances
  (let [inputs {:person (merge {:age 80 :province :on :end-age 82} no-benefits)
                :accounts [{:id :rrif :type :rrif :balance 1000000.0
                            :holdings {:equity 0.5 :bonds 0.5}}]
                :goal {:type :spend-down :annual-spending 60000}
                :start-year 2026}
        estate (:estate (plan/run-plan inputs))]
    (is (pos? (:terminal-tax estate)))
    (is (< (:after-tax estate) (:gross estate)))
    (testing "terminal tax bites hard on a large RRIF"
      (is (> (:terminal-tax estate) (* 0.3 (:gross estate)))))))

(deftest nova-scotia-plan-runs-without-ontario-only-levies
  (let [inputs (assoc-in basic-inputs [:person :province] :ns)
        result (plan/run-plan inputs)]
    (doseq [row (:years result)]
      (is (zero? (get-in row [:tax :surtax])))
      (is (zero? (get-in row [:tax :health-premium]))))
    (testing "provincial tax is actually levied"
      (is (pos? (get-in (last (:years result)) [:tax :provincial]))))))

(deftest runtime-tax-table-override-changes-the-plan
  ;; Patch the 2026 table with a brutal 90% flat bottom bracket; the same
  ;; scenario must now pay dramatically more tax.
  (let [inputs {:person (merge {:age 65 :province :on :end-age 70} no-benefits)
                :accounts [{:id :rrif :type :rrif :balance 1000000.0
                            :holdings {:equity 0.5 :bonds 0.5}}]
                :goal {:type :spend-down :annual-spending 50000}
                :start-year 2026}
        normal (plan/run-plan inputs)
        taxed (plan/run-plan
               (assoc inputs :tax-tables
                      {2026 {:federal {:brackets [{:up-to nil :rate 0.9}]}}}))]
    (is (> (get-in (first (:years taxed)) [:tax :federal])
           (* 3 (get-in (first (:years normal)) [:tax :federal]))))))

(deftest deterministic-given-same-inputs
  (is (= (plan/run-plan basic-inputs) (plan/run-plan basic-inputs))))

(deftest explicit-market-path-supported
  (let [n 31
        flat (repeat n {:equity 0.0 :bonds 0.0 :cash 0.0 :inflation 0.0})
        result (plan/run-plan basic-inputs flat)]
    (testing "zero inflation keeps the spending target flat"
      (is (approx= (:spending-target (first (:years result)))
                   (:spending-target (last (:years result)))
                   0.01)))
    (testing "path shorter than the horizon throws"
      (is (thrown? clojure.lang.ExceptionInfo
                   (plan/run-plan basic-inputs (take 5 flat)))))))

(deftest solve-max-spending-is-a-real-boundary
  (let [inputs {:person (merge {:age 65 :province :on :end-age 90} no-benefits)
                :accounts [{:id :tfsa :type :tfsa :balance 1000000.0
                            :holdings {:equity 0.6 :bonds 0.4}}]
                :goal {:type :spend-down :annual-spending 1000}
                :start-year 2026}
        max-spend (plan/solve-max-spending inputs)
        at (fn [s] (get-in (plan/run-plan
                            (assoc inputs :goal {:type :spend-down
                                                 :annual-spending s}))
                           [:summary :success?]))]
    (is (pos? max-spend))
    (is (at max-spend))
    (is (not (at (* 1.05 max-spend))))))
