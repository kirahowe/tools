(ns retirement.tax-test
  (:require [clojure.test :refer [deftest is testing]]
            [retirement.tax :as tax]
            [retirement.taxdata :as data]
            [retirement.test-util :refer [approx=]]))

(def t2025 (data/resolve-table (data/tables) 2025))
(def t2026 (data/resolve-table (data/tables) 2026))
(def fed-2025 (:federal t2025))

(deftest federal-rate-schedule-by-year
  (is (= 0.145 (get-in t2025 [:federal :brackets 0 :rate])))
  (is (= 0.145 (get-in t2025 [:federal :credit-rate])))
  (is (= 0.14 (get-in t2026 [:federal :brackets 0 :rate])))
  (is (= 0.14 (get-in t2026 [:federal :credit-rate]))))

(deftest bracket-tax-basics
  (let [brackets (tax/index-brackets (:brackets fed-2025) 1.0)]
    (is (zero? (tax/bracket-tax brackets 0.0)))
    (is (approx= (* 1000 0.145) (tax/bracket-tax brackets 1000.0)))
    (testing "boundary of the first bracket"
      (is (approx= (* 57375 0.145) (tax/bracket-tax brackets 57375.0))))
    (testing "top bracket is open-ended"
      (is (pos? (tax/bracket-tax brackets 1.0e7))))))

(deftest ordinary-income-100k-ontario-2025
  ;; Hand-computed against the 2025 tables:
  ;; federal: 57,375*0.145 + 42,625*0.205 = 17,057.50 gross,
  ;;          minus BPA 16,129*0.145 = 2,338.71 -> 14,718.80
  ;; Ontario: 52,886*0.0505 + 47,114*0.0915 = 6,981.67 gross,
  ;;          minus BPA 12,747*0.0505 = 643.72 -> 6,337.95 basic
  ;;          surtax 0.20*(6,337.95-5,710) = 125.59; health premium 750
  (let [result (tax/income-tax {:table t2025 :factor 1.0 :province :on
                                :income {:ordinary 100000.0 :age 50}})]
    (is (approx= 100000.0 (:taxable-income result)))
    (is (approx= 14718.80 (:federal result) 0.5))
    (is (approx= 6337.95 (:provincial result) 0.5))
    (is (approx= 125.59 (:surtax result) 0.5))
    (is (approx= 750.0 (:health-premium result) 0.01))
    (is (approx= (+ 14718.80 6337.95 125.59 750.0) (:total result) 1.5))))

(deftest ordinary-income-50k-nova-scotia-2025
  ;; Hand-computed against the post-Budget-2025 NS tables:
  ;; NS: 30,507*0.0879 + 19,493*0.1495 = 5,595.77 gross,
  ;;     minus flat BPA 11,744*0.0879 = 1,032.30 -> 4,563.47
  ;; federal: 50,000*0.145 - 16,129*0.145 = 4,911.30
  ;; No surtax, no health premium in NS.
  (let [result (tax/income-tax {:table t2025 :factor 1.0 :province :ns
                                :income {:ordinary 50000.0 :age 50}})]
    (is (approx= 4563.47 (:provincial result) 0.5))
    (is (approx= 4911.30 (:federal result) 0.5))
    (is (zero? (:surtax result)))
    (is (zero? (:health-premium result)))
    (is (approx= (+ 4563.47 4911.30) (:total result) 1.0))))

(deftest published-federal-2026-hand-computed
  ;; 100k ordinary: 58,523*0.14 + 41,477*0.205 = 16,696.01 gross,
  ;; minus BPA 16,452*0.14 = 2,303.28 -> 14,392.73
  (is (approx= 14392.73
               (tax/federal-tax (:federal t2026) 1.0 {:ordinary 100000.0 :age 50})
               0.5)))

(deftest zero-income-zero-tax
  (let [result (tax/income-tax {:table t2025 :factor 1.0 :province :on
                                :income {:ordinary 0.0 :age 70}})]
    (is (zero? (:total result)))))

(deftest low-income-fully-sheltered-by-bpa
  (let [result (tax/income-tax {:table t2025 :factor 1.0 :province :on
                                :income {:ordinary 12000.0 :age 40}})]
    (is (zero? (:federal result)))
    (is (zero? (:provincial result)))))

(deftest eligible-dividends-modest-amount-attracts-no-income-tax
  ;; $20k of eligible dividends as only income: gross-up to $27,600;
  ;; BPA + dividend tax credits wipe out both federal and Ontario tax.
  ;; Only the (unindexed) Ontario health premium applies: ti 27,600 ->
  ;; min(300, 6% * 7,600) = 300.
  (let [result (tax/income-tax {:table t2025 :factor 1.0 :province :on
                                :income {:eligible-dividends 20000.0 :age 40}})]
    (is (approx= 27600.0 (:taxable-income result)))
    (is (zero? (:federal result)))
    (is (zero? (:provincial result)))
    (is (approx= 300.0 (:health-premium result)))
    (is (approx= 300.0 (:total result)))))

(deftest capital-gains-half-included
  (let [result (tax/income-tax {:table t2025 :factor 1.0 :province :on
                                :income {:capital-gains 100000.0 :age 40}})]
    (is (approx= 50000.0 (:taxable-income result)))))

(deftest oas-clawback-cases
  (testing "15% of net income above the threshold"
    (is (approx= (* 0.15 (- 100000 93454))
                 (tax/oas-clawback fed-2025 1.0 100000.0 8732.0))))
  (testing "capped at OAS received"
    (is (approx= 8732.0 (tax/oas-clawback fed-2025 1.0 500000.0 8732.0))))
  (testing "zero below threshold"
    (is (zero? (tax/oas-clawback fed-2025 1.0 60000.0 8732.0))))
  (testing "threshold indexes with inflation"
    (is (zero? (tax/oas-clawback fed-2025 1.1 100000.0 8732.0)))))

(deftest age-amount-behaviour
  (testing "full at low income, 65+"
    (is (approx= 9028.0 (tax/age-amount (:age-amount fed-2025) 1.0 65 40000.0))))
  (testing "phased out at high income"
    (is (zero? (tax/age-amount (:age-amount fed-2025) 1.0 65 200000.0))))
  (testing "not available before 65"
    (is (zero? (tax/age-amount (:age-amount fed-2025) 1.0 64 40000.0))))
  (testing "Nova Scotia's threshold stays fixed while the amount indexes"
    (let [ns-age (get-in t2025 [:provinces :ns :age-amount])]
      ;; factor 2.0: max doubles to 11,468 but the 30,828 threshold holds,
      ;; so at 40k net income the reduction is 0.15 * 9,172 = 1,375.80.
      (is (approx= (- (* 2 5734.0) 1375.80)
                   (tax/age-amount ns-age 2.0 70 40000.0)
                   0.5)))))

(deftest bpa-phase-out
  (let [bpa (:bpa fed-2025)]
    (is (approx= 16129.0 (tax/basic-personal-amount bpa 1.0 100000.0)))
    (is (approx= 14538.0 (tax/basic-personal-amount bpa 1.0 300000.0)))
    (testing "midpoint interpolates"
      (let [mid (/ (+ 177882.0 253414.0) 2)]
        (is (approx= (/ (+ 16129.0 14538.0) 2)
                     (tax/basic-personal-amount bpa 1.0 mid) 1.0))))
    (testing "a flat BPA (no :min) never phases out"
      (is (approx= 11744.0
                   (tax/basic-personal-amount
                    (get-in t2025 [:provinces :ns :bpa]) 1.0 500000.0))))))

(deftest pension-credit-reduces-tax-for-rrif-income-at-65
  (let [with-credit (tax/income-tax {:table t2025 :factor 1.0 :province :on
                                     :income {:ordinary 60000.0 :age 66
                                              :pension-income 5000.0}})
        without (tax/income-tax {:table t2025 :factor 1.0 :province :on
                                 :income {:ordinary 60000.0 :age 66}})]
    ;; federal 2,000 * 14.5% + Ontario 1,762 * 5.05%
    (is (approx= (+ (* 2000 0.145) (* 1762 0.0505))
                 (- (:total without) (:total with-credit))
                 0.5))))

(deftest indexing-scales-thresholds-but-not-unindexed-ones
  (let [brackets-2x (tax/index-brackets (get-in t2025 [:provinces :on :brackets]) 2.0)]
    (is (approx= (* 2 52886.0) (:up-to (first brackets-2x))))
    (testing "Ontario's 150k/220k thresholds are not indexed by law"
      (is (approx= 150000.0 (:up-to (nth brackets-2x 2))))
      (is (approx= 220000.0 (:up-to (nth brackets-2x 3)))))))

(deftest unknown-province-throws
  (is (thrown? clojure.lang.ExceptionInfo
               (tax/income-tax {:table t2025 :factor 1.0 :province :xx
                                :income {:ordinary 1000.0}}))))
