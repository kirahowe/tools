(ns retirement.taxdata-test
  (:require [clojure.test :refer [deftest is testing]]
            [retirement.taxdata :as data]
            [retirement.test-util :refer [approx=]])
  (:import (java.io StringReader)))

(def t2025 (data/resolve-table (data/tables) 2025))

(deftest builtin-tables-load-from-edn
  (is (= [2025 2026] (vec (keys (data/tables)))))
  (doseq [[year table] (data/tables)]
    (is (= year (:year table)))
    (is (contains? table :federal))
    (is (contains? (:provinces table) :ns))
    (is (contains? table :benefits))
    (is (contains? table :rrif-factors))))

(deftest table-resolution
  (let [tables (data/tables)]
    (testing "exact year"
      (is (= 2025 (:year (data/resolve-table tables 2025)))))
    (testing "later years use the latest table"
      (is (= 2026 (:year (data/resolve-table tables 2030)))))
    (testing "earlier years fall back to the earliest table"
      (is (= 2025 (:year (data/resolve-table tables 2020)))))))

(deftest rrif-factors
  (is (approx= 0.0528 (data/rrif-minimum-factor t2025 71)))
  (is (approx= 0.0540 (data/rrif-minimum-factor t2025 72)))
  (is (approx= 0.20 (data/rrif-minimum-factor t2025 95)))
  (is (approx= 0.20 (data/rrif-minimum-factor t2025 101)))
  (testing "below 71 the factor is 1/(90-age)"
    (is (approx= 0.04 (data/rrif-minimum-factor t2025 65)))
    (is (approx= 0.05 (data/rrif-minimum-factor t2025 70)))))

(deftest user-tables-patch-builtins
  (let [patched (data/tables {2025 {:provinces {:on {:bpa {:max 99999.0}}}}})
        on (get-in patched [2025 :provinces :on])]
    (testing "the patched value takes effect"
      (is (approx= 99999.0 (get-in on [:bpa :max]))))
    (testing "everything else survives the deep merge"
      (is (approx= 0.0505 (get-in on [:brackets 0 :rate])))
      (is (seq (:surtax on)))
      (is (= 2025 (get-in patched [2025 :year]))))
    (testing "other years untouched"
      (is (= (data/resolve-table (data/tables) 2026)
             (data/resolve-table patched 2026))))))

(deftest new-year-tables-must-be-complete
  (testing "a full new-year table is accepted and resolves"
    (let [full (assoc (data/resolve-table (data/tables) 2026) :year 2027)
          tables (data/tables {2027 full})]
      (is (= 2027 (:year (data/resolve-table tables 2028))))))
  (testing "a partial new-year table throws with the missing keys"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"missing"
                          (data/tables {2027 {:federal {}}}))))
  (testing "non-map override throws"
    (is (thrown? clojure.lang.ExceptionInfo (data/tables {2025 :nope})))))

(deftest read-table-reads-edn-sources
  (let [table (data/read-table (StringReader. "{:year 2030 :federal {:x 1}}"))]
    (is (= 2030 (:year table)))
    (is (= 1 (get-in table [:federal :x])))))

(deftest nova-scotia-2025-post-budget-figures
  (let [ns-table (get-in t2025 [:provinces :ns])]
    (testing "flat BPA — Budget 2025 removed the phase-out"
      (is (approx= 11744.0 (get-in ns-table [:bpa :max])))
      (is (nil? (get-in ns-table [:bpa :min]))))
    (testing "age amount with unindexed threshold"
      (is (approx= 5734.0 (get-in ns-table [:age-amount :max])))
      (is (false? (get-in ns-table [:age-amount :threshold-indexed?]))))
    (testing "non-eligible dividend credit cut to 1.5% in 2025"
      (is (approx= 0.015 (get-in ns-table [:dividend-credits :non-eligible]))))
    (testing "no surtax, no health premium"
      (is (nil? (:surtax ns-table)))
      (is (nil? (:health-premium ns-table))))))

(deftest published-2026-figures
  (let [t2026 (data/resolve-table (data/tables) 2026)]
    (testing "federal 2026: 14% bottom rate, published thresholds"
      (is (= 0.14 (get-in t2026 [:federal :brackets 0 :rate])))
      (is (approx= 58523.0 (get-in t2026 [:federal :brackets 0 :up-to])))
      (is (approx= 16452.0 (get-in t2026 [:federal :bpa :max])))
      (is (approx= 95323.0 (get-in t2026 [:federal :oas-clawback :threshold]))))
    (testing "Nova Scotia 2026: published 1.6% indexation"
      (is (approx= 30995.0 (get-in t2026 [:provinces :ns :brackets 0 :up-to])))
      (is (approx= 11932.0 (get-in t2026 [:provinces :ns :bpa :max]))))))
