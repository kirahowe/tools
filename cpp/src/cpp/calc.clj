(ns cpp.calc
  "Internal CPP benefit mechanics. All functions are pure; every input
  arrives as an argument and results are plain data.

  The model follows the actual Service Canada month-level methodology:

  Base component (pre-2019 rules, still ~75% of a modern pension):
    1. Establish the contributory period in months.
    2. Convert each year's pensionable earnings to a ratio of that
       year's YMPE (prorated for partial first/last years), then value
       every month at ratio * MPEA / 12 (MPEA = five-year average YMPE
       ending in the start year), which wage-indexes old earnings.
    3. Exclude child-rearing months (child under 7, below-average
       earnings), then months after 65 that are below average, then the
       general 17% dropout of lowest months (leaving at least 120).
    4. Base pension = 25% of the average of the remaining months.

  Enhanced components (2019+):
    * First additional: 8.33% of average monthly first-additional
      pensionable earnings, always averaged over 480 months (40 years),
      best years first. 2019-2023 earnings receive partial credit
      matching the contribution phase-in.
    * Second additional (CPP2, 2024+): 33.33% of average monthly
      earnings between YMPE and YAMPE, also averaged over 480 months.
    * Child-rearing and disability are 'drop-ins' (credited earnings)
      rather than dropouts for these components.

  Actuarial adjustment: -0.6%/month before 65, +0.7%/month after 65
  (max +42% at 70), applied to all three components."
  (:require [cpp.data :as data]
            [cpp.dates :as d]))

;; ---------------------------------------------------------------------------
;; Constants (verified against the CPP Act & Service Canada methodology)
;; ---------------------------------------------------------------------------

(def base-replacement-rate 0.25)
(def first-additional-replacement-rate (/ 1.0 12.0)) ; 8.33%
(def second-additional-replacement-rate (/ 1.0 3.0)) ; 33.33%

(def early-adjustment-per-month 0.006) ; age 60-65
(def late-adjustment-per-month 0.007)  ; age 65-70
(def max-deferral-months 60)

(def general-dropout-rate 0.17)
(def min-base-months 120)          ; base averaging never below 10 years
(def enhanced-averaging-months 480) ; enhanced always averages over 40 years

(def cpp-inception (d/ym 1966 1))

;; ---------------------------------------------------------------------------
;; Contributory period & start date
;; ---------------------------------------------------------------------------

(defn start-month
  "Month the pension begins. Defaults to the month after the 65th
  birthday month. `start` may be a year-month (see cpp.dates/parse-ym)
  or an age map {:years 63 :months 4}."
  [birth-ym start]
  (cond
    (nil? start) (inc (d/add-years birth-ym 65))
    (and (map? start) (:years start))
    (inc (d/add-months (d/add-years birth-ym (:years start))
                       (:months start 0)))
    :else (d/parse-ym start)))

(defn adjustment-factor
  "Actuarial adjustment for starting before/after the month following
  the 65th birthday. Reduction of 0.6%/month early (floor: month after
  60th birthday), increase of 0.7%/month late (cap: 60 months)."
  [birth-ym start-ym]
  (let [at-65 (inc (d/add-years birth-ym 65))
        months (d/months-between at-65 start-ym)]
    (if (neg? months)
      (- 1.0 (* early-adjustment-per-month (min 60 (- months))))
      (+ 1.0 (* late-adjustment-per-month (min max-deferral-months months))))))

(defn contributory-period
  "[from to) month range of the base contributory period: begins the
  month after the 18th birthday (or Jan 1966), ends with the month
  before the pension starts."
  [birth-ym start-ym]
  (let [from (max cpp-inception (inc (d/add-years birth-ym 18)))]
    [from (max from start-ym)]))

;; ---------------------------------------------------------------------------
;; Earnings timeline
;; ---------------------------------------------------------------------------

(defn year-months-in
  "Months of calendar year `y` inside the [from to) range."
  [y [from to]]
  (let [ystart (d/ym y 1)
        yend (d/ym (inc y) 1)]
    (max 0 (- (min to yend) (max from ystart)))))

(defn earnings-ratio
  "Fraction of the year's (prorated) YMPE that `earnings` represents,
  capped at 1.0. For partial years in the contributory period the YMPE
  is prorated by months, so strong earnings in e.g. the year the
  pension starts can still fill those months completely."
  [earnings ympe months-in-period]
  (if (zero? months-in-period)
    0.0
    (min 1.0 (/ (double (or earnings 0.0))
                (* ympe (/ months-in-period 12.0))))))

(defn month-timeline
  "Sequence of maps, one per month of the contributory period:
  {:ym m :year y :ratio r} where :ratio is the year's earnings ratio
  (every month of a year carries the year's ratio — CPP attributes
  annual earnings evenly across the year's months in the period)."
  [[from to :as period] earnings-by-year ympe-fn]
  (let [ratios (into {}
                     (map (fn [y]
                            [y (earnings-ratio (get earnings-by-year y)
                                               (ympe-fn y)
                                               (year-months-in y period))]))
                     (range (d/ym-year from) (inc (d/ym-year (dec to)))))]
    (for [m (range from to)
          :let [y (d/ym-year m)]]
      {:ym m :year y :ratio (get ratios y 0.0)})))

;; ---------------------------------------------------------------------------
;; Base component: dropouts
;; ---------------------------------------------------------------------------

(defn- mean [xs] (if (seq xs) (/ (reduce + 0.0 xs) (count xs)) 0.0))

(defn- exclude-below-average
  "Repeatedly exclude candidate months whose ratio is below the current
  average of the remaining months, until stable. Used for both the
  child-rearing dropout and the over-65 dropout: those months may only
  be excluded when doing so raises the average (excluding a
  below-average month always does, and re-checking after each pass
  reaches the fixpoint)."
  [months candidate?]
  (loop [kept months]
    (let [avg (mean (map :ratio kept))
          {drop true keep false} (group-by #(boolean
                                             (and (candidate? %)
                                                  (< (:ratio %) avg)))
                                           kept)]
      (if (seq drop)
        (recur (vec keep))
        (vec kept)))))

(defn general-dropout
  "Drop the lowest-earning 17% of months, never averaging over fewer
  than 120 months. Returns the months kept."
  [months]
  (let [n (count months)
        keep-n (max (min n min-base-months)
                    (- n (long (Math/floor (* general-dropout-rate n)))))]
    (->> months (sort-by :ratio) (drop (- n keep-n)) vec)))

(defn base-pension
  "Monthly base retirement pension (before actuarial adjustment), plus
  a breakdown of the dropout arithmetic.

  `cr-month?` and `over-65?` are predicates on timeline month maps
  identifying child-rearing months and months after the 65th birthday."
  [timeline mpea cr-month? over-65?]
  (let [after-cr (exclude-below-average timeline cr-month?)
        after-65 (exclude-below-average after-cr over-65?)
        kept (general-dropout after-65)
        avg-ratio (mean (map :ratio kept))]
    {:monthly (* base-replacement-rate avg-ratio (/ mpea 12.0))
     :average-ratio avg-ratio
     :months {:contributory (count timeline)
              :child-rearing-dropped (- (count timeline) (count after-cr))
              :over-65-dropped (- (count after-cr) (count after-65))
              :general-dropped (- (count after-65) (count kept))
              :averaged (count kept)}}))
