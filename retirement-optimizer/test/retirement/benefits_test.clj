(ns retirement.benefits-test
  (:require [clojure.test :refer [deftest is testing]]
            [retirement.benefits :as benefits]
            [retirement.test-util :refer [approx=]]))

(deftest cpp-adjustments
  (is (approx= 1.0 (benefits/cpp-adjustment 65)))
  (is (approx= 0.64 (benefits/cpp-adjustment 60)))
  (is (approx= 1.42 (benefits/cpp-adjustment 70)))
  (testing "clamped outside 60-70"
    (is (approx= 0.64 (benefits/cpp-adjustment 55)))
    (is (approx= 1.42 (benefits/cpp-adjustment 80)))))

(deftest cpp-annual-payments
  (let [cfg {:start-age 70 :at-65 12000}]
    (is (zero? (benefits/cpp-annual cfg 69 1.0)))
    (is (approx= (* 12000 1.42) (benefits/cpp-annual cfg 70 1.0)))
    (testing "indexed by inflation"
      (is (approx= (* 12000 1.42 1.1) (benefits/cpp-annual cfg 75 1.1)))))
  (testing "no CPP config means no CPP"
    (is (zero? (benefits/cpp-annual nil 70 1.0)))))

(deftest oas-adjustments-and-payments
  (is (approx= 1.0 (benefits/oas-adjustment 65)))
  (is (approx= 1.36 (benefits/oas-adjustment 70)))
  (let [at-65 8732.0]
    (is (zero? (benefits/oas-annual {:start-age 65} 64 1.0)))
    (is (approx= at-65 (benefits/oas-annual {:start-age 65} 65 1.0)))
    (testing "10% boost from age 75"
      (is (approx= (* at-65 1.10) (benefits/oas-annual {:start-age 65} 75 1.0))))
    (testing "deferral to 70"
      (is (approx= (* at-65 1.36) (benefits/oas-annual {:start-age 70} 70 1.0))))
    (testing "partial residency"
      (is (approx= (* at-65 0.5)
                   (benefits/oas-annual {:start-age 65 :fraction 0.5} 65 1.0))))))

(deftest gis-behaviour
  (testing "maximum at zero income"
    (is (approx= 13042.6 (benefits/gis-annual true 0.0 1.0))))
  (testing "reduced 50 cents per dollar of non-OAS income"
    (is (approx= (- 13042.6 5000.0) (benefits/gis-annual true 10000.0 1.0))))
  (testing "zero at higher incomes"
    (is (zero? (benefits/gis-annual true 30000.0 1.0))))
  (testing "requires OAS receipt"
    (is (zero? (benefits/gis-annual false 0.0 1.0)))))
