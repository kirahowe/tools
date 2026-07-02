(ns oas.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [oas.core :as oas]))

;; All expectations below use the July-September 2026 rate quarter:
;; full OAS 65-74 = $751.97/month, 75+ = $827.17, clawback threshold
;; (2026 income year) = $95,323.

(def as-of [2026 7])

(deftest full-pension-at-65
  (let [r (oas/estimate {:birth-date "1961-06-15"
                         :years-in-canada 40
                         :as-of as-of})]
    (is (:eligible? r))
    (is (= [2026 7] (:first-eligible-month r)))
    (is (= [2026 7] (:start-month r)))
    (is (= 1 (get-in r [:calculation :pension-fraction])))
    (is (= 0 (get-in r [:calculation :deferral-months])))
    (is (= 751.97M (get-in r [:monthly :oas-gross])))
    (testing "no income supplied -> no clawback, no GIS"
      (is (= 0.00M (get-in r [:monthly :recovery-tax])))
      (is (nil? (:recovery-tax r)))
      (is (nil? (:gis r)))
      (is (= 751.97M (get-in r [:monthly :total]))))))

(deftest partial-pension
  (let [r (oas/estimate {:birth-date "1961-01-20"
                         :years-in-canada 20
                         :resides-in-canada? true
                         :include-gis? false
                         :as-of as-of})]
    (is (= 1/2 (get-in r [:calculation :pension-fraction])))
    (is (= 375.99M (get-in r [:monthly :oas-gross]))))
  (testing "whole years only: 20.9 years still pays 20/40"
    (let [r (oas/estimate {:birth-date "1961-01-20" :years-in-canada 20.9
                           :resides-in-canada? false
                           :agreement-satisfies-minimum? true
                           :as-of as-of})]
      (is (= 1/2 (get-in r [:calculation :pension-fraction]))))))

(deftest age-75-uplift
  (let [r (oas/estimate {:birth-date "1950-01-10"
                         :years-in-canada 40
                         :as-of as-of})]
    (testing "already 75+: uplifted maximum equals the published 75+ rate"
      (is (= 827.17M (get-in r [:monthly :oas-gross]))))
    (testing "schedule shows the historical structure: start at 65, uplift after 75"
      (is (= [{:band :65-74 :from [2015 2] :oas 751.97M}
              {:band :75-plus :from [2025 2] :oas 827.17M}]
             (mapv (fn [e] {:band (:age-band e) :from (:from e)
                            :oas (get-in e [:monthly :oas-gross])})
                   (:schedule r))))))
  (testing "uplift starts the month after the 75th birthday"
    (let [r (oas/estimate {:birth-date "1958-03-05" :years-in-canada 40 :as-of as-of})]
      (is (= [2033 4] (get-in r [:calculation :age-75-uplift-from])))
      (is (= 827.17M (get-in r [:monthly-at-75 :oas-gross]))))))

(deftest deferral
  (testing "maximum deferral: 60 months -> +36%"
    (let [r (oas/estimate {:birth-date "1958-06-01"
                           :years-in-canada 40
                           :start-date "2028-07"
                           :as-of as-of})]
      (is (= 60 (get-in r [:calculation :deferral-months])))
      (is (= 34/25 (get-in r [:calculation :deferral-factor])))
      (is (= 1022.68M (get-in r [:monthly :oas-gross])))))
  (testing "partial deferral: 59 months"
    (let [r (oas/estimate {:birth-date "1958-06-01"
                           :years-in-canada 40
                           :start-date "2028-06"
                           :as-of as-of})]
      (is (= 59 (get-in r [:calculation :deferral-months])))
      (is (= 1018.17M (get-in r [:monthly :oas-gross])))))
  (testing "no credit accrues past age 70"
    (let [r (oas/estimate {:birth-date "1955-06-01"
                           :years-in-canada 40
                           :start-date "2026-07"   ; age 71
                           :as-of as-of})]
      (is (= 60 (get-in r [:calculation :deferral-months])))
      (is (= 1022.68M (get-in r [:monthly :oas-gross])))
      (is (some #(re-find #"after age 70" %) (:warnings r)))))
  (testing "start date before eligibility is clamped with a warning"
    (let [r (oas/estimate {:birth-date "1961-06-15" :years-in-canada 40
                           :start-date "2025-01" :as-of as-of})]
      (is (= [2026 7] (:start-month r)))
      (is (some #(re-find #"precedes first eligibility" %) (:warnings r))))))

(deftest deferral-vs-residence-accrual
  (testing "few residence years: continuing to accrue beats the deferral credit"
    ;; 10 years at 65, resident, defer 5 years. Pure deferral pays
    ;; 10/40 x 1.36 = 0.34 and pure accrual 15/40 = 0.375, but the best
    ;; no-double-counting split is accrue to 14 years, then take 13 months
    ;; of deferral credit: 14/40 x 1.078 = 0.3773.
    (let [r (oas/estimate {:birth-date "1958-06-01"
                           :years-in-canada 10
                           :start-date "2028-07"
                           :as-of as-of})]
      (is (= 14 (get-in r [:calculation :residence-years])))
      (is (= 13 (get-in r [:calculation :deferral-months])))
      (is (= 283.72M (get-in r [:monthly :oas-gross])))))
  (testing "many residence years: the deferral credit beats accrual"
    ;; 35 years at 65, defer 5: 35/40 x 1.36 = 1.19 beats 40/40 x 1.0
    (let [r (oas/estimate {:birth-date "1958-06-01"
                           :years-in-canada 35
                           :start-date "2028-07"
                           :as-of as-of})]
      (is (= 35 (get-in r [:calculation :residence-years])))
      (is (= 60 (get-in r [:calculation :deferral-months])))
      (is (= 894.84M (get-in r [:monthly :oas-gross]))))))

(deftest eligibility-rules
  (testing "residence minimum reached after 65 delays the start"
    (let [r (oas/estimate {:birth-date "1961-06-15" :years-in-canada 9 :as-of as-of})]
      (is (:eligible? r))
      (is (= [2027 6] (:first-eligible-month r)))))
  (testing "non-resident needs 20 years"
    (let [r (oas/estimate {:birth-date "1961-06-15" :years-in-canada 15
                           :resides-in-canada? false :as-of as-of})]
      (is (not (:eligible? r)))
      (is (some #(re-find #"20 years" %) (:ineligible-reasons r)))))
  (testing "a social security agreement can satisfy the minimum"
    (let [r (oas/estimate {:birth-date "1961-06-15" :years-in-canada 15
                           :resides-in-canada? false
                           :agreement-satisfies-minimum? true :as-of as-of})]
      (is (:eligible? r))
      (is (= 3/8 (get-in r [:calculation :pension-fraction])))
      (is (some #(re-find #"month of departure plus six months" %) (:warnings r)))))
  (testing "never eligible when residence cannot accrue"
    (is (not (:eligible? (oas/estimate {:birth-date "1961-06-15" :years-in-canada 5
                                        :resides-in-canada? false :as-of as-of}))))))

(deftest residence-periods
  (let [r (oas/estimate {:birth-date "1961-06-15"
                         :residence-periods [["1990-01" "2005-01"]  ; 15y
                                             [[2015 1] nil]]        ; ongoing
                         :as-of as-of})]
    (is (:eligible? r))
    ;; 15y + (2015-01 .. 2026-07) = 15 + 11.5 -> 26 whole years
    (is (= 26 (get-in r [:calculation :residence-years])))))

(deftest recovery-tax
  (testing "partial clawback"
    ;; gross 751.97 x 12 = 9,023.64; income 100,000 + 9,023.64 = 109,023.64
    ;; excess over 95,323 = 13,700.64 -> 15% = 2,055.10/yr = 171.26/mo
    (let [r (oas/estimate {:birth-date "1961-06-15" :years-in-canada 40
                           :annual-income 100000 :income-year 2026
                           :include-gis? false :as-of as-of})]
      (is (= 171.26M (get-in r [:monthly :recovery-tax])))
      (is (= 580.71M (get-in r [:monthly :total])))
      (is (= 2055.10M (get-in r [:recovery-tax :annual])))
      (is (= 109023.64M (get-in r [:recovery-tax :income-used])))))
  (testing "full clawback caps at the OAS received"
    (let [r (oas/estimate {:birth-date "1961-06-15" :years-in-canada 40
                           :annual-income 200000 :income-year 2026
                           :include-gis? false :as-of as-of})]
      (is (= 751.97M (get-in r [:monthly :recovery-tax])))
      (is (= 0.00M (get-in r [:monthly :total])))))
  (testing "below the threshold: nothing"
    (let [r (oas/estimate {:birth-date "1961-06-15" :years-in-canada 40
                           :annual-income 50000 :income-year 2026
                           :include-gis? false :as-of as-of})]
      (is (= 0.00M (get-in r [:monthly :recovery-tax])))))
  (testing "the 75+ full-recovery income is higher (bigger pension to claw back)"
    (let [at-65 (oas/estimate {:birth-date "1961-06-15" :years-in-canada 40
                               :annual-income 100000 :income-year 2026
                               :include-gis? false :as-of as-of})
          at-76 (oas/estimate {:birth-date "1950-01-10" :years-in-canada 40
                               :annual-income 100000 :income-year 2026
                               :include-gis? false :as-of as-of})]
      (is (pos? (compare (get-in at-76 [:recovery-tax :full-recovery-income])
                         (get-in at-65 [:recovery-tax :full-recovery-income])))))))

(deftest gis-integration
  (testing "zero income single pensioner gets full OAS + full GIS"
    (let [r (oas/estimate {:birth-date "1961-06-15" :years-in-canada 40
                           :annual-income 0 :as-of as-of})]
      (is (= 1123.17M (get-in r [:monthly :gis])))
      (is (= 1875.14M (get-in r [:monthly :total])))
      (is (true? (get-in r [:gis :estimate?])))))
  (testing "partial pensioner's GIS tops up the shortfall"
    (let [r (oas/estimate {:birth-date "1961-01-20" :years-in-canada 20
                           :annual-income 0 :as-of as-of})]
      (is (= 375.99M (get-in r [:monthly :oas-gross])))
      (is (= 1499.16M (get-in r [:monthly :gis])))
      ;; 375.985 + 1499.155 = 1875.14: same floor as a full pensioner
      (is (= 1875.14M (get-in r [:monthly :total])))))
  (testing "no GIS outside Canada"
    (let [r (oas/estimate {:birth-date "1961-06-15" :years-in-canada 25
                           :resides-in-canada? false
                           :annual-income 0 :as-of as-of})]
      (is (nil? (:gis r)))
      (is (= 0.00M (get-in r [:monthly :gis]))))))

(deftest schedule-and-annual
  (let [r (oas/estimate {:birth-date "1961-06-15" :years-in-canada 40
                         :annual-income 0 :as-of as-of})]
    (is (= 2 (count (:schedule r))))
    (is (= [2036 7] (:from (second (:schedule r)))))
    (is (= 9023.64M (get-in r [:annual :oas-gross])))
    (is (= (* 12 (get-in r [:monthly :total]))
           (get-in r [:annual :total])))))

(deftest input-validation
  (is (thrown? clojure.lang.ExceptionInfo (oas/estimate nil)))
  (is (thrown? clojure.lang.ExceptionInfo (oas/estimate {:years-in-canada 40})))
  (is (thrown? clojure.lang.ExceptionInfo (oas/estimate {:birth-date "1961-06-15"})))
  (is (thrown? clojure.lang.ExceptionInfo
               (oas/estimate {:birth-date "1961-06" :years-in-canada -3})))
  (is (thrown? clojure.lang.ExceptionInfo
               (oas/estimate {:birth-date "junk" :years-in-canada 40})))
  (is (thrown? clojure.lang.ExceptionInfo
               (oas/estimate {:birth-date "1961-06" :years-in-canada 40
                              :marital-status :widower})))
  (is (thrown? clojure.lang.ExceptionInfo
               (oas/estimate {:birth-date "1961-06" :years-in-canada 40
                              :annual-income -1}))))

(deftest custom-rates
  (testing "a caller-supplied future quarter is used"
    (let [r (oas/estimate {:birth-date "1961-06-15" :years-in-canada 40
                           :as-of [2026 10]
                           :rates {:quarters [{:from [2026 10]
                                               :oas-65-74 760.00M
                                               :oas-75-plus 836.00M}]}})]
      (is (= 760.00M (get-in r [:monthly :oas-gross]))))))
