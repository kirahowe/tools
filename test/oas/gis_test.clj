(ns oas.gis-test
  (:require [clojure.test :refer [deftest is testing]]
            [oas.gis :as gis]
            [oas.rates :as rates]
            [oas.util :as u]))

(def q (rates/quarter-for rates/default-rates [2026 7]))
(def constants (:constants rates/default-rates))

(defn- monthly [inputs shortfall]
  (u/round2 (:monthly (gis/estimate q constants inputs shortfall))))

(deftest earnings-exemption
  (is (= 0 (gis/earnings-exemption constants 0)))
  (is (= 3000 (gis/earnings-exemption constants 3000)))
  (is (= 5000 (gis/earnings-exemption constants 5000)))
  (testing "50% of the band between $5k and $15k"
    (is (= 7500 (gis/earnings-exemption constants 10000))))
  (testing "capped at $10,000 total"
    (is (= 10000 (gis/earnings-exemption constants 15000)))
    (is (= 10000 (gis/earnings-exemption constants 50000)))))

(deftest categories
  (is (= :single (gis/category {})))
  (is (= :single (gis/category {:marital-status :widowed})))
  (is (= :partner-oas (gis/category {:marital-status :married :partner-receives-oas? true})))
  (is (= :partner-allowance (gis/category {:marital-status :common-law
                                           :partner-receives-allowance? true})))
  (is (= :partner-no-oas (gis/category {:marital-status :married})))
  (testing "involuntarily separated couples are each assessed as single"
    (is (= :single (gis/category {:marital-status :married :partner-receives-oas? true
                                  :involuntarily-separated? true})))))

(deftest single-amounts
  (testing "no income -> published maximum"
    (is (= 1123.17M (monthly {:annual-income 0} 0))))
  (testing "at or past the cutoff -> zero"
    (is (= 0.00M (monthly {:annual-income 22800} 0)))
    (is (= 0.00M (monthly {:annual-income 60000} 0))))
  (testing "mid income: base reduced 50%, top-up reduced 25% past $2,000"
    ;; base 950 - 10000/24 = 533.33...; top-up 173.17 - 8000/48 = 6.5033...
    (is (= 539.84M (monthly {:annual-income 10000} 0))))
  (testing "employment earnings are partially exempt"
    ;; $10,000 all employment -> assessable 2,500: base 950 - 2500/24,
    ;; top-up 173.17 - 500/48
    (is (= 1008.59M (monthly {:annual-income 10000 :employment-income 10000} 0)))))

(deftest couple-amounts
  (let [couple {:marital-status :married :partner-receives-oas? true}]
    (testing "no income -> each gets the couple maximum"
      (is (= 676.10M (monthly (assoc couple :annual-income 0) 0))))
    (testing "combined income is assessed"
      (is (= 0.00M (monthly (assoc couple
                                   :annual-income 20000
                                   :partner-annual-income 10096)
                            0))))
    (testing "involuntary separation switches to single rate on own income"
      (is (= 1123.17M (monthly (assoc couple
                                      :annual-income 0
                                      :partner-annual-income 50000
                                      :involuntarily-separated? true)
                               0))))))

(deftest partial-pension-shortfall
  (testing "a partial pensioner's GIS maximum grows by the OAS shortfall"
    ;; 20/40 pension at 2026Q3: shortfall = 751.97/2 = 375.985
    (is (= 1499.16M (monthly {:annual-income 0} (u/exact 375.985M))))))

(deftest partner-no-oas-linear-model
  (testing "single-rate maximum, zero at the published couple cutoff"
    (let [in {:marital-status :married}]
      (is (= 1123.17M (monthly (assoc in :annual-income 0) 0)))
      (is (= 0.00M (monthly (assoc in :annual-income 30000 :partner-annual-income 24624) 0))))))
