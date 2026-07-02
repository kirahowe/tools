(ns cpp.calc
  "Internal CPP benefit mechanics. All functions are pure; every input
  arrives as an argument and results are plain data.

  The model follows the Service Canada month-level methodology (CPP
  Act ss. 46-53.6; Doug Runchey's published worked examples):

  Base component:
    1. Establish the contributory period in months (month after the
       18th birthday or Jan 1966, through the month before the pension
       starts), excluding months on a CPP disability pension.
    2. Convert each year's pensionable earnings to a ratio of that
       year's (prorated) YMPE; every month of the year carries that
       ratio. A month's dollar value is ratio * MPEA / 12, where MPEA
       is the five-year average YMPE ending in the start year - this
       ratio revaluation is the only wage indexing CPP applies.
    3. Dropouts, in order: child-rearing months (child under 7,
       below-average earnings, excluded only where that helps), the
       general 17% dropout (ceil(0.17*N) lowest months, leaving at
       least 120), then the over-65 substitution (each contributory
       month after 65 permits dropping one more below-average month).
    4. Base pension = 25% of the average surviving monthly value.

  Enhanced components (2019+), per CPP Act ss. 53.1-53.6:
    * First additional: 8.33% replacement on the same earnings band as
      the base, fixed 480-month divisor (best months once a record
      exceeds 40 years), no dropouts; 2019-2022 earnings earn partial
      credit (0.15/0.30/0.50/0.75) matching the contribution phase-in.
    * Second additional (CPP2): 33.33% replacement on earnings between
      YMPE and YAMPE, same 480-month averaging, from 2024.
    * Child-rearing and disability are drop-ins (credited earnings,
      used only when greater than actual) rather than dropouts.

  Actuarial adjustment: -0.6%/month before 65, +0.7%/month after 65
  (max +42% at 70), applied to all three components."
  (:require [cpp.data :as data]
            [cpp.dates :as d]))

;; ---------------------------------------------------------------------------
;; Constants
;; ---------------------------------------------------------------------------

(def base-replacement-rate 0.25)
(def first-additional-replacement-rate (/ 1.0 12.0)) ; 8.33%
(def second-additional-replacement-rate (/ 1.0 3.0)) ; 33.33%

(def early-adjustment-per-month 0.006) ; age 60-65
(def late-adjustment-per-month 0.007)  ; age 65-70
(def max-deferral-months 60)

(def general-dropout-rate 0.17)
(def min-base-months 120)           ; base averaging never below 10 years
(def enhanced-divisor-months 480)   ; enhanced always averages over 40 years

(def child-rearing-months-per-child 84) ; month after birth until age 7

(def cpp-inception (d/ym 1966 1))
(def enhancement-inception (d/ym 2019 1))
(def cpp2-inception (d/ym 2024 1))

;; ---------------------------------------------------------------------------
;; Start date & actuarial adjustment
;; ---------------------------------------------------------------------------

(defn start-month
  "Month the pension begins, clamped to the legal window (month after
  the 60th birthday through month after the 70th). Defaults to the
  month after the 65th birthday. `start` may be a year-month (see
  cpp.dates/parse-ym) or an age map {:years 63 :months 4}."
  [birth-ym start]
  (let [requested (cond
                    (nil? start) (inc (d/add-years birth-ym 65))
                    (and (map? start) (:years start))
                    (inc (d/add-months (d/add-years birth-ym (:years start))
                                       (:months start 0)))
                    :else (d/parse-ym start))]
    (-> requested
        (max (inc (d/add-years birth-ym 60)))
        (min (inc (d/add-years birth-ym 70))))))

(defn adjustment-factor
  "Actuarial adjustment for starting before/after the month following
  the 65th birthday: -0.6%/month early, +0.7%/month late (capped at 60
  months). Fixed permanently at the start month; applies to the base
  and both additional components."
  [birth-ym start-ym]
  (let [at-65 (inc (d/add-years birth-ym 65))
        months (d/months-between at-65 start-ym)]
    (if (neg? months)
      (- 1.0 (* early-adjustment-per-month (min 60 (- months))))
      (+ 1.0 (* late-adjustment-per-month (min max-deferral-months months))))))

;; ---------------------------------------------------------------------------
;; Contributory period & month sets
;; ---------------------------------------------------------------------------

(defn contributory-period
  "[from to) month range of the base contributory period: begins the
  month after the 18th birthday (or Jan 1966), ends with the month
  before the pension starts."
  [birth-ym start-ym]
  (let [from (max cpp-inception (inc (d/add-years birth-ym 18)))]
    [from (max from start-ym)]))

(defn child-rearing-months
  "Set of months covered by the child-rearing provision: for each
  child, the month after birth through the month the child turns 7.
  Overlapping children merge."
  [children]
  (into #{}
        (mapcat (fn [{:keys [born]}]
                  (let [b (d/parse-ym born)]
                    (range (inc b) (+ (inc b) child-rearing-months-per-child)))))
        children))

(defn period-month-set
  "Set of months in periods given as [{:from ym :to ym} ...] with :to
  inclusive at month granularity."
  [periods]
  (into #{}
        (mapcat (fn [{:keys [from to]}]
                  (range (d/parse-ym from) (inc (d/parse-ym to)))))
        periods))

;; ---------------------------------------------------------------------------
;; Earnings ratios
;; ---------------------------------------------------------------------------

(defn earnings-ratio
  "`earnings` as a fraction of the year's prorated YMPE, capped at 1.
  Proration counts only the year's months actually in the period, so
  strong earnings in a partial year (the year the pension starts, the
  year of the 18th birthday) still fill those months completely - and
  for dropout comparisons a partial year is thereby annualized,
  matching Service Canada's method."
  [earnings ympe months]
  (if (zero? months)
    0.0
    (min 1.0 (/ (double (or earnings 0.0))
                (* ympe (/ months 12.0))))))

(defn excess-earnings-ratio
  "Earnings between the prorated YMPE and prorated YAMPE (the CPP2
  band), expressed as a fraction of the prorated YMPE - the CPP Act
  adjusts second-additional earnings by the same YMPE ratio as
  everything else. Zero for years before 2024 (no second ceiling)."
  [earnings ympe yampe months]
  (if (or (zero? months) (nil? yampe))
    0.0
    (let [proration (/ months 12.0)
          floor (* ympe proration)
          ceiling (* yampe proration)
          excess (- (min (double (or earnings 0.0)) ceiling) floor)]
      (/ (max 0.0 excess) floor))))

;; ---------------------------------------------------------------------------
;; Base component
;; ---------------------------------------------------------------------------

(defn- mean [xs] (if (seq xs) (/ (reduce + 0.0 xs) (count xs)) 0.0))

(defn month-timeline
  "One map per month of [from to), minus `excluded` (disability)
  months: {:ym m :year y :ratio r}. Every month of a year carries the
  year's earnings ratio; proration counts only the year's non-excluded
  months in the period."
  [[from to] earnings-by-year ympe excluded]
  (let [months (remove excluded (range from to))
        ratios (update-vals
                (group-by d/ym-year months)
                (fn [ms]
                  (let [y (d/ym-year (first ms))]
                    (earnings-ratio (get earnings-by-year y)
                                    (ympe y)
                                    (count ms)))))]
    (mapv (fn [m]
            (let [y (d/ym-year m)]
              {:ym m :year y :ratio (get ratios y 0.0)}))
          months)))

(defn general-dropout
  "Drop the ceil(17%) lowest-ratio months, never leaving fewer than
  120. Returns the kept months sorted ascending by ratio."
  [months]
  (let [n (count months)
        d (min (long (Math/ceil (* general-dropout-rate n)))
               (max 0 (- n min-base-months)))]
    (vec (drop d (sort-by :ratio months)))))

(defn over-65-substitution
  "CPP Act s.48(3): each contributory month after the 65th birthday
  permits dropping one further month - itself or a lower earlier
  month - so post-65 months only ever help. Takes months sorted
  ascending by ratio; drops up to `post-65-count` more months while
  the lowest sits below the current average (dropping it then always
  raises the average), respecting the 120-month floor."
  [sorted-months post-65-count]
  (loop [ms sorted-months
         budget post-65-count]
    (if (and (pos? budget)
             (> (count ms) min-base-months)
             (< (:ratio (first ms)) (mean (map :ratio ms))))
      (recur (subvec ms 1) (dec budget))
      ms)))

(defn base-pension
  "Monthly base retirement pension before actuarial adjustment, plus a
  breakdown of the dropout arithmetic.

  The child-rearing dropout excludes CR months only where that raises
  the final average, so it is solved as a fixpoint: exclude the CR
  months whose ratio falls below the average the rest of the pipeline
  (17% dropout + over-65 substitution) produces, recompute, and repeat
  until the exclusion set stabilizes. The CRDO itself may not shrink
  the contributory period below 120 months."
  [timeline mpea cr-month-set post-65-count]
  (let [run (fn [excluded]
              (-> (into [] (remove (comp excluded :ym)) timeline)
                  general-dropout
                  (over-65-substitution post-65-count)))
        excluded (loop [excluded #{}]
                   (let [avg (mean (map :ratio (run excluded)))
                         room (max 0 (- (count timeline) min-base-months))
                         next-excluded (->> timeline
                                            (filter #(and (cr-month-set (:ym %))
                                                          (< (:ratio %) avg)))
                                            (sort-by :ratio)
                                            (take room)
                                            (into #{} (map :ym)))]
                     (if (= next-excluded excluded)
                       excluded
                       (recur next-excluded))))
        kept (run excluded)
        avg-ratio (mean (map :ratio kept))]
    {:monthly (* base-replacement-rate avg-ratio (/ mpea 12.0))
     :average-ratio avg-ratio
     :months {:contributory (count timeline)
              :child-rearing-dropped (count excluded)
              :dropped-or-substituted (- (count timeline) (count excluded)
                                         (count kept))
              :averaged (count kept)}}))

;; ---------------------------------------------------------------------------
;; Enhanced components (first additional 2019+, second additional 2024+)
;; ---------------------------------------------------------------------------

(defn- year-ratios
  "Year -> {:months n :ratio r} over [from to), where `ratio-fn` maps
  [year months-in-period] to that year's earnings ratio."
  [[from to] ratio-fn]
  (update-vals (group-by d/ym-year (range from to))
               (fn [ms]
                 (let [y (d/ym-year (first ms))]
                   {:months (count ms)
                    :ratio (ratio-fn y (count ms))}))))

(defn- prior-average-ratio
  "Mean earnings ratio over the `n` calendar years before `year`,
  computed with `ratio-fn` as full years. Used by the child-rearing
  and disability drop-ins."
  [ratio-fn year n]
  (mean (map #(ratio-fn % 12) (range (- year n) year))))

(defn enhanced-component
  "Average monthly pensionable earnings for an enhanced component:
  each month is valued at ratio * phase-in * MPEA / 12 (with the
  child-rearing / disability drop-ins substituting a credited ratio
  where greater), the best 480 month-values are summed, and the total
  is divided by the fixed 480-month divisor - so records shorter than
  40 years earn proportionally less, exactly as legislated. Returns
  the monthly pension before actuarial adjustment.

  Drop-ins per CPP Act ss. 53.3-53.6: child-rearing months (child
  under 7) are credited with the average ratio of the 5 years before
  the child-rearing period began; disability months with 70% of the
  average ratio of the 6 years before onset. Both apply only when
  greater than actual earnings for the month."
  [{:keys [period ratio-fn phase-in replacement-rate mpea
           cr-month-set dis-month-set]
    :or {cr-month-set #{} dis-month-set #{} phase-in (constantly 1.0)}}]
  (let [[from to] period
        ratios (year-ratios period ratio-fn)
        credit-start (fn [month-set]
                       (when-let [ms (seq (filter #(< % to) month-set))]
                         (apply min ms)))
        cr-credit (some-> (credit-start cr-month-set)
                          d/ym-year
                          (as-> y (prior-average-ratio ratio-fn y 5)))
        dis-credit (some-> (credit-start dis-month-set)
                           d/ym-year
                           (as-> y (* 0.70 (prior-average-ratio ratio-fn y 6))))
        month-value (fn [m]
                      (let [y (d/ym-year m)
                            actual (get-in ratios [y :ratio] 0.0)
                            credited (max (if (and cr-credit (cr-month-set m))
                                            cr-credit 0.0)
                                          (if (and dis-credit (dis-month-set m))
                                            dis-credit 0.0))]
                        (* (max actual credited)
                           (phase-in y)
                           (/ mpea 12.0))))
        values (map month-value (range from to))
        best (take enhanced-divisor-months (sort > values))]
    {:monthly (* replacement-rate
                 (/ (reduce + 0.0 best) enhanced-divisor-months))
     :months (count values)}))

;; ---------------------------------------------------------------------------
;; Contributions -> pensionable earnings
;; ---------------------------------------------------------------------------

(defn earnings-from-contribution
  "Invert an annual CPP contribution into pensionable earnings for
  `year` (how a Statement of Contributions' UPE is derived): earnings
  = contribution / rate + YBE on the first band, then the CPP2 band at
  4% with no exemption. `multiplier` is 1 for employee amounts, 2 for
  self-employed."
  [year contribution multiplier ympe yampe]
  (let [rate (* multiplier (data/employee-rate year))
        band1-max (* rate (- (ympe year) (data/ybe year)))
        c (double (or contribution 0.0))]
    (if (<= c band1-max)
      (+ (/ c rate) (data/ybe year))
      (let [rate2 (* multiplier data/cpp2-employee-rate)
            ceiling (or (yampe year) (ympe year))]
        (min ceiling (+ (ympe year)
                        (/ (- c band1-max) rate2)))))))

;; ---------------------------------------------------------------------------
;; Top-level estimate
;; ---------------------------------------------------------------------------

(defn- round2 [x] (/ (Math/round (* 100.0 x)) 100.0))

(defn- normalize-earnings
  "Resolve the earnings record: accept :pensionable-earnings directly
  or invert :contributions. Years with earnings at or below the YBE
  are deemed zero (no contributions arise - CPP Act s.53)."
  [{:keys [pensionable-earnings contributions self-employed?]} ympe yampe]
  (let [earnings (or pensionable-earnings
                     (into {}
                           (map (fn [[y c]]
                                  [y (earnings-from-contribution
                                      y c (if self-employed? 2 1) ympe yampe)]))
                           (or contributions {})))]
    (into {}
          (keep (fn [[y e]]
                  (when (and e (> e (data/ybe y)))
                    [y (double e)])))
          earnings)))

(defn estimate*
  "See cpp.core/estimate for the input/output contract."
  [{:keys [birth-date start children disability assumptions] :as person}]
  (when-not birth-date
    (throw (ex-info ":birth-date is required" {:person person})))
  (let [birth (d/parse-ym birth-date)
        start-m (start-month birth start)
        start-year (d/ym-year start-m)
        ympe (data/ympe-fn (:wage-growth assumptions data/default-wage-growth))
        yampe (data/yampe-fn ympe)
        mpea (data/mpea start-year ympe)
        earnings (normalize-earnings person ympe yampe)
        [from to :as period] (contributory-period birth start-m)
        dis-months (period-month-set disability)
        cr-months (child-rearing-months children)
        timeline (month-timeline period earnings ympe dis-months)
        at-65 (inc (d/add-years birth 65))
        post-65-count (count (filter #(>= (:ym %) at-65) timeline))
        base (base-pension timeline mpea cr-months post-65-count)
        base-ratio-fn (fn [y months]
                        (earnings-ratio (earnings y) (ympe y) months))
        sa-ratio-fn (fn [y months]
                      (excess-earnings-ratio (earnings y) (ympe y) (yampe y)
                                             months))
        fa (enhanced-component
            {:period [(min to (max from enhancement-inception)) to]
             :ratio-fn base-ratio-fn
             :phase-in data/first-additional-phase-in
             :replacement-rate first-additional-replacement-rate
             :mpea mpea
             :cr-month-set cr-months
             :dis-month-set dis-months})
        sa (enhanced-component
            {:period [(min to (max from cpp2-inception)) to]
             :ratio-fn sa-ratio-fn
             :replacement-rate second-additional-replacement-rate
             :mpea mpea
             :cr-month-set cr-months
             :dis-month-set dis-months})
        factor (adjustment-factor birth start-m)
        adj-months (d/months-between (inc (d/add-years birth 65)) start-m)
        total (* factor (+ (:monthly base) (:monthly fa) (:monthly sa)))
        age-months (d/months-between birth (dec start-m))]
    {:start {:date (d/format-ym start-m)
             :year (d/ym-year start-m)
             :month (d/ym-month start-m)
             :age {:years (quot age-months 12) :months (rem age-months 12)}}
     :adjustment {:months adj-months
                  :factor (round2 factor)}
     :monthly {:base (round2 (* factor (:monthly base)))
               :first-additional (round2 (* factor (:monthly fa)))
               :second-additional (round2 (* factor (:monthly sa)))
               :total (round2 total)}
     :annual (round2 (* 12 total))
     :details {:mpea mpea
               :base (assoc base :monthly (round2 (:monthly base)))
               :first-additional fa
               :second-additional sa}}))
