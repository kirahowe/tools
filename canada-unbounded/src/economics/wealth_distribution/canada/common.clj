^:kindly/hide-code
(ns economics.wealth-distribution.canada.common
  "Shared helpers for the Unbounded North series: data location, palette, formatting."
  {:clay {:kindly/options {:hide-code true}}}
  (:require [babashka.fs :as fs]
            [clojure.math :as m]
            [tablecloth.api :as tc]))

(def data-dir
  (fs/path "src/economics/wealth_distribution/canada/data/"))

(defn data
  "Load a CSV from the series data directory."
  ([file] (data file {}))
  ([file opts]
   (tc/dataset (str (fs/path data-dir file)) (merge {:key-fn keyword} opts))))

;; Palette shared with the original series: blue for work, orange for ownership.
(def w "#9FC0DE")
(def wd "#547A9E")
(def o "#FFB347")
(def od "#B86B00")
(def neutral "#C9C3B6")
(def red "#C0392B")
(def green "#2E8B57")

(def ^:private human-pows [[12 "T"] [9 "B"] [6 "M"] [3 "K"] [0 ""]])

(defn money
  "Format a dollar amount compactly, e.g. 1.2e9 -> \"$1.2B\"."
  [num]
  (let [neg?      (neg? num)
        num       (abs (double num))
        base-pow  (if (zero? num) 0 (int (m/floor (m/log10 num))))
        [p suffix] (first (filter (fn [[base _]] (>= base-pow base)) human-pows))
        value     (/ num (m/pow 10 p))]
    (str (when neg? "-") (format "$%.1f" value) suffix)))

(defn pct-text [x] (format "%.1f%%" (double x)))
