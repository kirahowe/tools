^:kindly/hide-code
(ns economics.wealth-distribution.canada.common
  "Shared helpers for the Unbounded North series: data location, palette, formatting."
  {:clay {:kindly/options {:hide-code true}}}
  (:require [babashka.fs :as fs]
            [clojure.math :as m]
            [scicloj.plotje.api :as pj]
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

(defn pair-rows
  "Turn long data (one row per category and series) into `dumbbell` rows,
  keeping categories in the order they first appear."
  [ds label-k series-k value-k from-series to-series]
  (let [rows (tc/rows ds :as-maps)
        v    (fn [label series] (some #(when (and (= label (label-k %)) (= series (series-k %))) (value-k %)) rows))]
    (for [label (distinct (map label-k rows))]
      {:label label :from (v label from-series) :to (v label to-series)})))

(defn dumbbell
  "Dot plot comparing two values per category, joined by a thin line.
  `rows` is a seq of maps with `:label`, `:from` and `:to`, listed top to bottom.
  The `from` dot is the reference (muted); the `to` dot is the one the chart is about."
  [rows {:keys [from-name to-name from-color to-color title x-label fmt include-zero height]
         :or   {from-color neutral to-color red fmt #(format "%.1f" (double %))}}]
  (let [rows   (reverse rows)
        conn   (tc/dataset (for [{:keys [label from to]} rows]
                             {:label label :lo (min from to) :hi (max from to) :c "#B8B8B8"}))
        pts    (tc/dataset (concat (for [{:keys [label from]} rows] {:label label :value from :series from-name})
                                   (for [{:keys [label to]} rows] {:label label :value to :series to-name})))
        ;; Each value is labelled on the outside of its pair, so labels never sit on the connector.
        side   (fn [left?] (tc/dataset (for [{:keys [label from to]} rows
                                             [v other which] [[from to :from] [to from :to]]
                                             :when (if left? (< v other) (or (> v other) (and (= v other) (= which :to))))]
                                         {:label label :value v :t (fmt v)})))
        left   (side true)
        right  (side false)]
    (cond-> (-> conn
                (pj/lay-interval-h :lo :label {:x-end :hi :interval-thickness 0.12
                                               :color {:column :c :scale false}})
                (pj/lay-point {:data pts :x :value :y :label :color :series :size 5 :overlay true})
                (pj/lay-text {:data right :x :value :y :label :text :t :align-x :left :offset-x 9 :overlay true}))
      (pos? (tc/row-count left))
      (pj/lay-text {:data left :x :value :y :label :text :t :align-x :right :offset-x -9 :overlay true})
      include-zero (pj/scale :x {:include 0})
      true (pj/scale :color {:values [from-color to-color]})
      true (pj/options (cond-> {:title title :x-label x-label :y-label ""
                                :color-label "" :legend-position :bottom}
                         height (assoc :height height))))))
