(ns cpp.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [cpp.calc :as calc]
            [cpp.core :as cpp]
            [cpp.data :as data]
            [cpp.dates :as d]))

(defn- max-earnings
  "An earnings record comfortably above the ceiling for every year in
  [from to], i.e. a lifetime maximum contributor."
  [from to]
  (into {} (map (fn [y] [y 1000000])) (range from (inc to))))

(def close-to-a-dollar 1.0)

(defn- approx= [expected actual tolerance]
  (< (abs (- expected actual)) tolerance))

;; ---------------------------------------------------------------------------
;; Validation against officially published amounts
;; ---------------------------------------------------------------------------

(deftest maximum-new-pension-2025
  ;; canada.ca: maximum new retirement pension at 65, Jan 2025 = $1,433.00
  (let [r (cpp/estimate {:birth-date "1959-12"
                         :pensionable-earnings (max-earnings 1978 2024)})]
    (testing "starts the month after the 65th birthday by default"
      (is (= "2025-01" (get-in r [:start :date])))
      (is (= {:years 65 :months 0} (get-in r [:start :age])))
      (is (= 1.0 (get-in r [:adjustment :factor]))))
    (testing "reproduces the published 2025 maximum"
      (is (approx= 1433.00 (get-in r [:monthly :total]) close-to-a-dollar)))
    (testing "component split"
      (is (approx= 1387.08 (get-in r [:monthly :base]) 0.02))
      (is (approx= 42.77 (get-in r [:monthly :first-additional]) 0.02))
      (is (approx= 3.17 (get-in r [:monthly :second-additional]) 0.02)))))

(deftest maximum-new-pension-2026
  ;; canada.ca / OPSEU fact sheet: maximum at 65, Jan 2026 = $1,507.65
  (let [r (cpp/estimate {:birth-date "1960-12"
                         :pensionable-earnings (max-earnings 1979 2025)})]
    (is (= "2026-01" (get-in r [:start :date])))
    (is (approx= 1507.65 (get-in r [:monthly :total]) close-to-a-dollar))))

(deftest mpea-values
  (let [ympe (data/ympe-fn data/default-wage-growth)]
    (is (== 66580.0 (data/mpea 2025 ympe)))
    (is (== 69180.0 (data/mpea 2026 ympe)))
    (testing "pre-1999 averaging windows (3yr, then 4yr for 1998)"
      (is (== (/ (+ 34900 35400 35800 36900) 4.0) (data/mpea 1998 ympe)))
      (is (== (/ (+ 34900 35400 35800) 3.0) (data/mpea 1997 ympe))))))

;; ---------------------------------------------------------------------------
;; Actuarial adjustment / start dates
;; ---------------------------------------------------------------------------

(deftest early-and-late-starts
  (let [person {:birth-date "1961-06"
                :pensionable-earnings (max-earnings 1980 2025)}
        at-60 (cpp/estimate (assoc person :start {:years 60}))
        at-65 (cpp/estimate person)
        at-70 (cpp/estimate (assoc person :start {:years 70}))]
    (is (= 0.64 (get-in at-60 [:adjustment :factor])))
    (is (= -60 (get-in at-60 [:adjustment :months])))
    (is (= 1.0 (get-in at-65 [:adjustment :factor])))
    (is (= 1.42 (get-in at-70 [:adjustment :factor])))
    (testing "deferral strictly increases the pension"
      (is (< (get-in at-60 [:monthly :total])
             (get-in at-65 [:monthly :total])
             (get-in at-70 [:monthly :total]))))))

(deftest start-clamped-to-legal-window
  (let [person {:birth-date "1961-06"
                :pensionable-earnings (max-earnings 1980 2025)}]
    (is (= (cpp/estimate (assoc person :start {:years 60}))
           (cpp/estimate (assoc person :start {:years 58}))))
    (is (= (cpp/estimate (assoc person :start {:years 70}))
           (cpp/estimate (assoc person :start {:years 75}))))))

(deftest deferral-past-65-with-no-earnings-still-gains
  ;; the over-65 substitution drops the empty post-65 months, so
  ;; deferring costs nothing even after retiring from work at 65
  (let [stop-at-65 {:birth-date "1955-01"
                    :pensionable-earnings (max-earnings 1973 2019)}
        at-65 (cpp/estimate stop-at-65)
        at-70 (cpp/estimate (assoc stop-at-65 :start {:years 70}))]
    (is (approx= (* 1.42
                    (/ (get-in at-70 [:details :mpea])
                       (get-in at-65 [:details :mpea]))
                    (get-in at-65 [:monthly :base]))
                 (get-in at-70 [:monthly :base])
                 close-to-a-dollar))))

;; ---------------------------------------------------------------------------
;; Dropouts
;; ---------------------------------------------------------------------------

(deftest general-dropout-absorbs-eight-zero-years
  ;; ceil(17% of 564 months) = 96 months = 8 years
  (let [full (cpp/estimate {:birth-date "1959-12"
                            :pensionable-earnings (max-earnings 1978 2024)})
        gappy (cpp/estimate {:birth-date "1959-12"
                             :pensionable-earnings
                             (apply dissoc (max-earnings 1978 2024)
                                    (range 2010 2018))})]
    (is (= (get-in full [:monthly :base])
           (get-in gappy [:monthly :base])))
    (testing "a ninth zero year does reduce the base"
      (let [gappier (cpp/estimate {:birth-date "1959-12"
                                   :pensionable-earnings
                                   (apply dissoc (max-earnings 1978 2024)
                                          (range 2009 2018))})]
        (is (< (get-in gappier [:monthly :base])
               (get-in full [:monthly :base])))))))

(deftest child-rearing-dropout-protects-base
  ;; a 12-year child-rearing gap (two children) is more than the 17%
  ;; general dropout can absorb on its own
  (let [career (merge (max-earnings 1989 1999) (max-earnings 2012 2034))
        person {:birth-date "1970-06"
                :pensionable-earnings career}
        without (cpp/estimate person)
        with (cpp/estimate (assoc person :children [{:born "2000-01"}
                                                    {:born "2005-01"}]))]
    (testing "claiming the provision never hurts and here helps"
      (is (> (get-in with [:monthly :base])
             (get-in without [:monthly :base]))))
    (testing "the zero-earnings child-rearing months vanish from the average"
      (is (pos? (get-in with [:details :base :months :child-rearing-dropped])))
      (is (approx= 1.0 (get-in with [:details :base :average-ratio]) 1e-9)))))

(deftest child-rearing-dropout-skipped-when-unhelpful
  ;; earnings during the child-rearing years were at the maximum, so
  ;; excluding those months could only hurt - the fixpoint leaves them in
  (let [person {:birth-date "1970-06"
                :pensionable-earnings (max-earnings 1989 2025)}
        with (cpp/estimate (assoc person :children [{:born "2000-01"}]))]
    (is (zero? (get-in with [:details :base :months :child-rearing-dropped])))
    (is (= (get-in (cpp/estimate person) [:monthly :total])
           (get-in with [:monthly :total])))))

(deftest disability-months-excluded-from-base-period
  (let [career (merge (max-earnings 1989 2009) (max-earnings 2015 2025))
        person {:birth-date "1970-06"
                :pensionable-earnings career}
        without (cpp/estimate person)
        with (cpp/estimate (assoc person :disability
                                  [{:from "2010-01" :to "2014-12"}]))]
    (is (> (get-in with [:monthly :base])
           (get-in without [:monthly :base])))))

;; ---------------------------------------------------------------------------
;; Enhanced components
;; ---------------------------------------------------------------------------

(deftest enhancement-phase-in-credits
  (is (= [0.0 0.15 0.30 0.50 0.75 1.0 1.0]
         (mapv data/first-additional-phase-in
               [2018 2019 2020 2021 2022 2023 2024]))))

(deftest no-enhanced-components-before-2019
  (let [r (cpp/estimate {:birth-date "1953-12"
                         :pensionable-earnings (max-earnings 1972 2018)})]
    (is (= "2019-01" (get-in r [:start :date])))
    (is (zero? (get-in r [:monthly :first-additional])))
    (is (zero? (get-in r [:monthly :second-additional])))))

(deftest cpp2-only-earns-above-ympe
  (let [at-ympe (cpp/estimate {:birth-date "1959-12"
                               :pensionable-earnings
                               (into {} (map (fn [y] [y (data/ympe-table y)]))
                                     (range 1978 2025))})]
    (is (zero? (get-in at-ympe [:monthly :second-additional])))
    (is (pos? (get-in at-ympe [:monthly :first-additional])))))

(deftest enhanced-averaging-uses-fixed-480-divisor
  ;; one maximum enhanced year out of a 40-year divisor: Runchey's
  ;; worked example gives $1.44/month for a single max 2019 year
  (let [fa (calc/enhanced-component
            {:period [(d/ym 2019 1) (d/ym 2020 1)]
             :ratio-fn (fn [y _] (if (= y 2019) 1.0 0.0))
             :phase-in data/first-additional-phase-in
             :replacement-rate calc/first-additional-replacement-rate
             :mpea (data/mpea 2020 (data/ympe-fn 0.03))})]
    (is (approx= 1.44 (:monthly fa) 0.05))))

;; ---------------------------------------------------------------------------
;; Contributions -> earnings inversion
;; ---------------------------------------------------------------------------

(deftest contribution-inversion
  (let [ympe (data/ympe-fn 0.03)
        yampe (data/yampe-fn ympe)]
    (testing "2025 maximum base contribution maps back to the YMPE"
      (is (== 71300.0 (calc/earnings-from-contribution 2025 4034.10 1 ympe yampe))))
    (testing "adding the CPP2 maximum reaches the YAMPE"
      (is (== 81200.0 (calc/earnings-from-contribution 2025 4430.10 1 ympe yampe))))
    (testing "self-employed amounts are halved before inversion"
      (is (== 71300.0 (calc/earnings-from-contribution 2025 8068.20 2 ympe yampe))))
    (testing "estimate accepts :contributions directly"
      (let [r (cpp/estimate {:birth-date "1959-12"
                             :contributions {2024 3867.50}})]
        (is (pos? (get-in r [:monthly :base])))))))

(deftest sub-ybe-earnings-are-not-pensionable
  (let [r (cpp/estimate {:birth-date "1959-12"
                         :pensionable-earnings {2020 3400}})]
    (is (zero? (get-in r [:monthly :total])))))

;; ---------------------------------------------------------------------------
;; Plumbing
;; ---------------------------------------------------------------------------

(deftest date-parsing
  (is (= (d/ym 1990 6) (d/parse-ym "1990-06")))
  (is (= (d/ym 1990 6) (d/parse-ym "1990-06-15")))
  (is (= (d/ym 1990 6) (d/parse-ym {:year 1990 :month 6})))
  (is (= (d/ym 1990 1) (d/parse-ym 1990))))

(deftest future-years-use-projected-ympe
  (let [r (cpp/estimate {:birth-date "1990-06"
                         :pensionable-earnings (max-earnings 2010 2054)})]
    (is (pos? (get-in r [:monthly :total])))
    (testing "wage-growth assumption changes the result"
      (is (not= (get-in r [:monthly :total])
                (-> (cpp/estimate {:birth-date "1990-06"
                                   :pensionable-earnings (max-earnings 2010 2054)
                                   :assumptions {:wage-growth 0.02}})
                    (get-in [:monthly :total])))))))

(deftest missing-birth-date-throws
  (is (thrown? clojure.lang.ExceptionInfo (cpp/estimate {}))))
