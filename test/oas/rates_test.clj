(ns oas.rates-test
  (:require [clojure.test :refer [deftest is testing]]
            [oas.rates :as rates]))

(deftest table-internal-consistency
  (testing "75+ maximum is exactly 1.10 x the 65-74 maximum, rounded to the cent"
    (doseq [q (:quarters rates/default-rates)]
      (is (= (:oas-75-plus q)
             (.setScale (* (:oas-65-74 q) 1.1M) 2 java.math.RoundingMode/HALF_UP))
          (str "quarter " (:from q)))))
  (testing "amounts never decrease quarter over quarter (statutory)"
    (doseq [[a b] (partition 2 1 (:quarters rates/default-rates))]
      (is (<= (compare (:oas-65-74 a) (:oas-65-74 b)) 0)
          (str (:from a) " -> " (:from b)))))
  (testing "GIS cutoffs land on whole table brackets ($24 single, $48 couple)"
    (doseq [q (:quarters rates/default-rates)]
      (is (zero? (rem (long (get-in q [:gis :single :cutoff])) 24)))
      (is (zero? (rem (long (get-in q [:gis :partner-oas :cutoff])) 48))))))

(deftest quarter-lookup
  (is (= [2026 7] (:from (rates/quarter-for rates/default-rates [2026 8]))))
  (is (= [2026 4] (:from (rates/quarter-for rates/default-rates [2026 5]))))
  (is (= [2025 1] (:from (rates/quarter-for rates/default-rates [2025 3]))))
  (is (nil? (rates/quarter-for rates/default-rates [2024 12])))
  (is (= [2026 7] (:from (rates/latest-quarter rates/default-rates)))))

(deftest threshold-lookup
  (is (= {:threshold 93454M :year 2025 :exact? true}
         (rates/clawback-threshold rates/default-rates 2025)))
  (testing "unknown year falls back to the latest known"
    (is (= {:threshold 95323M :year 2026 :exact? false}
           (rates/clawback-threshold rates/default-rates 2031)))))

(deftest rates-override
  (let [merged (rates/merge-rates
                {:quarters [{:from [2026 10] :oas-65-74 760.00M :oas-75-plus 836.00M}]
                 :clawback-thresholds {2027 97000M}})]
    (is (= [2026 10] (:from (rates/latest-quarter merged))))
    (is (= 97000M (:threshold (rates/clawback-threshold merged 2027))))
    (testing "defaults are preserved"
      (is (= [2026 7] (:from (rates/quarter-for merged [2026 8])))))
    (testing "a quarter with the same :from replaces the default"
      (let [replaced (rates/merge-rates {:quarters [{:from [2026 7] :oas-65-74 999.99M}]})]
        (is (= 999.99M (:oas-65-74 (rates/quarter-for replaced [2026 7]))))
        (is (= (count (:quarters rates/default-rates))
               (count (:quarters replaced))))))))
