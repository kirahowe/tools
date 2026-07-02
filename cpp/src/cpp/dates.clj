(ns cpp.dates
  "Minimal year/month arithmetic. CPP is calculated at month
  granularity; we represent a month as a single integer
  (year * 12 + month-index) so ranges and differences are plain
  arithmetic."
  (:require [clojure.string :as str]))

(defn ym
  "Build a month index from a year and a 1-based month."
  [year month]
  (+ (* 12 year) (dec month)))

(defn ym-year [m] (quot m 12))
(defn ym-month [m] (inc (rem m 12)))

(defn parse-ym
  "Accepts \"YYYY-MM\", \"YYYY-MM-DD\", {:year Y :month M}, or an
  integer year (interpreted as January). Returns a month index."
  [x]
  (cond
    (integer? x) (ym x 1)
    (map? x) (ym (:year x) (:month x 1))
    (string? x) (let [[y m] (map #(Long/parseLong %) (take 2 (str/split x #"-")))]
                  (ym y (or m 1)))
    :else (throw (ex-info "Unparseable year-month" {:value x}))))

(defn add-months [m n] (+ m n))

(defn add-years [m n] (+ m (* 12 n)))

(defn months-between
  "Whole months from a to b (b - a)."
  [a b]
  (- b a))

(defn format-ym [m]
  (format "%d-%02d" (ym-year m) (ym-month m)))
