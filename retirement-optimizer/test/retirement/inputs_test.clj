(ns retirement.inputs-test
  (:require [clojure.test :refer [deftest is testing]]
            [retirement.inputs :as inputs]
            [retirement.test-util :refer [approx= basic-inputs]]))

(deftest valid-inputs-pass
  (is (empty? (inputs/validate basic-inputs))))

(defn- problem-mentioning [inputs s]
  (some #(clojure.string/includes? % s) (inputs/validate inputs)))

(deftest validation-catches-problems
  (testing "missing person / goal"
    (is (problem-mentioning (dissoc basic-inputs :person) ":person"))
    (is (problem-mentioning (dissoc basic-inputs :goal) ":goal")))
  (testing "bad account type"
    (is (problem-mentioning
         (assoc-in basic-inputs [:accounts 0 :type] :ira) ":type")))
  (testing "negative balance"
    (is (problem-mentioning
         (assoc-in basic-inputs [:accounts 0 :balance] -5) ":balance")))
  (testing "holdings must sum to one"
    (is (problem-mentioning
         (assoc-in basic-inputs [:accounts 0 :holdings] {:equity 0.5 :bonds 0.2})
         ":holdings")))
  (testing "duplicate ids"
    (is (problem-mentioning
         (assoc-in basic-inputs [:accounts 1 :id] :rrsp) "unique")))
  (testing "unknown province"
    (is (problem-mentioning
         (assoc-in basic-inputs [:person :province] :tx) ":province")))
  (testing "spending required"
    (is (problem-mentioning
         (update basic-inputs :goal dissoc :annual-spending) ":annual-spending")))
  (testing "legacy goal needs an amount"
    (is (problem-mentioning
         (assoc basic-inputs :goal {:type :legacy :annual-spending 50000})
         ":legacy")))
  (testing "acb only on non-registered"
    (is (problem-mentioning
         (assoc-in basic-inputs [:accounts 0 :acb] 100) ":acb")))
  (testing "start-year before tax data base year"
    (is (problem-mentioning (assoc basic-inputs :start-year 2020) ":start-year")))
  (testing "malformed :tax-tables reported, not thrown"
    (is (problem-mentioning (assoc basic-inputs :tax-tables {2025 :nope})
                            ":tax-tables"))
    (is (problem-mentioning (assoc basic-inputs :tax-tables {2027 {:federal {}}})
                            ":tax-tables")))
  (testing "a province added via :tax-tables becomes valid"
    (let [pei {:name "PEI" :brackets [{:up-to nil :rate 0.1}]
               :credit-rate 0.096 :bpa {:max 14250.0}
               :age-amount {:max 3764.0 :threshold 33740.0 :rate 0.15}
               :pension-amount {:amount 1000.0 :indexed? false}
               :dividend-credits {:eligible 0.105 :non-eligible 0.013}}
          with-pei (-> basic-inputs
                       (assoc-in [:person :province] :pe)
                       (assoc :tax-tables {2025 {:provinces {:pe pei}}
                                           2026 {:provinces {:pe pei}}}))]
      (is (problem-mentioning (assoc-in basic-inputs [:person :province] :pe)
                              ":province"))
      (is (empty? (inputs/validate with-pei))))))

(deftest normalize-defaults
  (let [cfg (inputs/normalize basic-inputs)]
    (is (= 1961 (get-in cfg [:person :birth-year])))
    (is (= 95 (get-in cfg [:person :end-age])))
    (is (= 31 (:n-years cfg)))
    (is (= 65 (:start-age cfg)))
    (testing "ACB defaults to balance (no unrealized gain assumed)"
      (let [cfg2 (inputs/normalize (update-in basic-inputs [:accounts 2] dissoc :acb))]
        (is (approx= 250000.0 (get-in cfg2 [:accounts 2 :acb])))))
    (testing "holdings default to a 60/40 mix"
      (let [cfg2 (inputs/normalize (update-in basic-inputs [:accounts 0] dissoc :holdings))]
        (is (= inputs/default-holdings (get-in cfg2 [:accounts 0 :holdings])))))
    (testing "CPI factor from 2025 base year to 2026 start"
      (is (approx= 1.021 (:initial-base-factor cfg) 1e-9)))
    (testing "assumptions deep-merge over defaults"
      (let [cfg2 (inputs/normalize
                  (assoc basic-inputs :assumptions {:returns {:equity {:mean 0.05}}}))]
        (is (approx= 0.05 (get-in cfg2 [:assumptions :returns :equity :mean])))
        (is (approx= 0.16 (get-in cfg2 [:assumptions :returns :equity :vol])))))))

(deftest normalize-throws-with-all-errors
  (let [bad (-> basic-inputs
                (assoc-in [:accounts 0 :type] :ira)
                (assoc-in [:person :province] :tx))]
    (try
      (inputs/normalize bad)
      (is false "should have thrown")
      (catch clojure.lang.ExceptionInfo e
        (is (= 2 (count (:errors (ex-data e)))))))))
