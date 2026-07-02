(ns retirement.optimize-test
  (:require [clojure.test :refer [deftest is testing]]
            [retirement.optimize :as optimize]
            [retirement.strategy :as strategy]
            [retirement.test-util :refer [basic-inputs]]))

(deftest ranking-is-sorted-and-best-is-first
  (let [{:keys [best ranking]} (optimize/optimize basic-inputs)]
    (is (= (:strategy best) (:strategy (first ranking))))
    (is (apply >= (map :score ranking)))
    (testing "every candidate was scored"
      (is (= (count (strategy/candidates)) (count ranking))))
    (testing "the winner beats or ties the conventional rule of thumb"
      (let [conventional-score (:score (first (filter #(= strategy/conventional
                                                          (:strategy %))
                                                      ranking)))]
        (is (>= (:score best) conventional-score))))
    (testing "the best entry carries the full year-by-year plan"
      (is (seq (get-in best [:plan :years]))))))

(deftest feasibility-flagged-per-strategy
  (let [{:keys [ranking]} (optimize/optimize
                           (assoc basic-inputs
                                  :goal {:type :spend-down :annual-spending 200000}))]
    (testing "nobody can fund 200k/yr from this portfolio"
      (is (every? (complement :meets-goal?) ranking)))))

(deftest custom-candidates-override
  (let [candidates [{:type :proportional}
                    {:type :bracket-fill :ceiling :oas-clawback}]
        {:keys [ranking]} (optimize/optimize basic-inputs
                                             {:candidates candidates})]
    (is (= 2 (count ranking)))
    (is (= (set candidates) (set (map :strategy ranking))))))

(deftest success-rate-metric-uses-monte-carlo
  (let [{:keys [best ranking]} (optimize/optimize
                                basic-inputs
                                {:metric :success-rate :trials 60 :seed 9
                                 :candidates [strategy/conventional
                                              {:type :proportional}]})]
    (is (= 2 (count ranking)))
    (is (every? #(<= 0.0 (:success-rate %) 1.0) ranking))
    (is (contains? best :simulation))))

(deftest bracket-fill-wins-for-wealthy-rrsp-heavy-retiree
  ;; The classic RRSP-meltdown case: large RRSP, income well past GIS
  ;; territory, deferred CPP. Draining the RRSP evenly through low-income
  ;; years must beat hoarding it for forced RRIF minimums + a taxed estate.
  (let [inputs {:person {:age 62 :province :on
                         :cpp {:start-age 70 :at-65 16000}
                         :oas {:start-age 70}
                         :pensions [{:annual 30000 :start-age 62}]}
                :accounts [{:id :rrsp :type :rrsp :balance 1500000.0
                            :holdings {:equity 0.6 :bonds 0.4}}
                           {:id :tfsa :type :tfsa :balance 150000.0
                            :holdings {:equity 0.8 :bonds 0.2}}]
                :goal {:type :spend-down :annual-spending 70000}
                :start-year 2026}
        {:keys [ranking]} (optimize/optimize inputs)
        score-of (fn [pred] (:score (first (filter pred ranking))))
        meltdown (score-of #(= :bracket-fill (get-in % [:strategy :type])))
        tfsa-first (score-of #(= [:tfsa :registered :non-registered]
                                 (get-in % [:strategy :order])))]
    (is (> meltdown tfsa-first)
        "spreading RRSP income across brackets should beat spending TFSA first")))

(deftest invalid-metric-throws
  (is (thrown? clojure.lang.ExceptionInfo
               (optimize/optimize basic-inputs {:metric :sharpe}))))
