(ns oas.core
  "Old Age Security (OAS) payout estimation for Canadians.

  OAS is Canada's residence-based public pension: unlike CPP it is
  non-contributory (funded from general revenue), so entitlement is driven
  by years of residence in Canada after age 18, not by contributions.

  The single entry point is `estimate`: plain data in, plain data out.

    (estimate {:birth-date \"1961-06-15\"
               :years-in-canada 40
               :annual-income 30000})

  Modeled rules:
    - eligibility at 65 with >= 10 years of Canadian residence after 18
      (>= 20 years when residing outside Canada; an international social
      security agreement can satisfy the minimum)
    - partial pension of (whole years of residence)/40, full at 40 years
    - optional start date with voluntary deferral: +0.6% per month past
      eligibility, max 60 months, no credit past age 70 — including the
      \"better of\" comparison between taking the deferral credit and
      continuing to accrue residence years while deferring
    - automatic 10% uplift in the month after the 75th birthday
    - recovery tax (\"clawback\"): 15% of net world income (including the
      OAS itself) above the year's threshold, capped at the OAS received
    - estimated Guaranteed Income Supplement for low-income pensioners,
      including the larger GIS available to partial pensioners, the
      earnings exemption, and the fact that no GIS is payable while
      deferring OAS or while outside Canada
    - quarterly-indexed rate tables (see oas.rates), overridable via :rates

  All results are expressed in the dollars of the rate quarter selected by
  :as-of (default: the latest quarter in the rate table). Future amounts
  are therefore in constant, current-quarter dollars: the library does not
  forecast CPI."
  (:require [oas.gis :as gis]
            [oas.rates :as rates]
            [oas.util :as u]))

;; --- inputs ----------------------------------------------------------------

(defn- invalid! [msg data]
  (throw (ex-info msg (assoc data :error :invalid-input))))

(defn- normalize
  "Validate and normalize the input map: parse dates, apply aliases and
  defaults. Throws ex-info on invalid input."
  [inputs]
  (when-not (map? inputs)
    (invalid! "estimate expects a map of inputs" {:inputs inputs}))
  (let [birth (or (u/parse-ym (:birth-date inputs))
                  (invalid! ":birth-date is required (\"YYYY-MM-DD\", \"YYYY-MM\", [y m] or {:year :month})"
                            {:inputs (keys inputs)}))
        years (or (:years-in-canada-at-65 inputs) (:years-in-canada inputs))
        periods (some->> (:residence-periods inputs)
                         (mapv (fn [[from to]]
                                 [(u/parse-ym from) (some-> to u/parse-ym)])))
        income (or (:annual-income inputs) (:retirement-income inputs))]
    (when (and (nil? years) (nil? periods))
      (invalid! "Provide :years-in-canada (residence after age 18, as of age 65) or :residence-periods"
                {:inputs (keys inputs)}))
    (when (and years (or (not (number? years)) (neg? years) (> years 47)))
      (invalid! ":years-in-canada must be a number between 0 and 47" {:years-in-canada years}))
    (doseq [k [:annual-income :retirement-income :employment-income
               :partner-annual-income :partner-employment-income]
            :let [v (k inputs)]]
      (when (and v (or (not (number? v)) (neg? v)))
        (invalid! (str k " must be a non-negative number") {k v})))
    (when-let [ms (:marital-status inputs)]
      (when-not (contains? #{:single :widowed :divorced :married :common-law} ms)
        (invalid! ":marital-status must be one of :single :widowed :divorced :married :common-law"
                  {:marital-status ms})))
    (-> inputs
        (assoc :birth-ym birth
               :years-in-canada-at-65 (some-> years u/exact)
               :residence-periods periods
               :annual-income income
               :resides-in-canada? (:resides-in-canada? inputs true)
               :include-gis? (:include-gis? inputs true)
               :start-ym (u/parse-ym (:start-date inputs))
               :as-of-ym (u/parse-ym (:as-of inputs))))))

;; --- residence -------------------------------------------------------------

(defn- residence-months-at
  "Months of Canadian residence (after age 18) accumulated before month ym.
  Uses :residence-periods when given (periods are [from to] months, to nil
  for ongoing); otherwise :years-in-canada-at-65 plus continued accrual
  after 65 while residing in Canada."
  [{:keys [residence-periods years-in-canada-at-65 resides-in-canada? birth-ym]} ym]
  (if residence-periods
    (reduce (fn [acc [from to]]
              (let [end (if to (u/ym-min to ym) ym)]
                (+ acc (max 0 (u/months-between from end)))))
            0
            residence-periods)
    (let [m65 (u/ym+ birth-ym (* 12 65))]
      (+ (* 12 years-in-canada-at-65)
         (if resides-in-canada?
           (max 0 (u/months-between m65 ym))
           0)))))

(defn- residence-years-at
  "Whole years of residence at ym (the OAS Act counts only complete years
  for the 40ths fraction), capped at the full-pension requirement."
  [in constants ym]
  (min (:full-pension-years constants)
       (quot (long (residence-months-at in ym)) 12)))

;; --- eligibility -----------------------------------------------------------

(defn- first-eligible-month
  "First month a pension could be payable: the month after the 65th
  birthday, or later if the residence minimum is only met later (residence
  keeps accruing while living in Canada). Returns nil when the minimum is
  never reached."
  [in constants]
  (let [start (u/ym+ (:birth-ym in) (inc (* 12 65)))
        min-months (* 12 (cond
                           (:agreement-satisfies-minimum? in) 0
                           (:resides-in-canada? in) (:min-years-in-canada constants)
                           :else (:min-years-abroad constants)))]
    (->> (iterate #(u/ym+ % 1) start)
         (take 601)                     ; give up after 50 years
         (filter #(>= (residence-months-at in %) min-months))
         first)))

;; --- pension amount --------------------------------------------------------

(defn- best-pension-base
  "The fraction of the full pension payable at start-ym, maximized over
  every way of splitting the wait since eligibility between residence
  accrual and the deferral credit.

  A month of waiting can count either toward more residence (a bigger
  /40ths fraction) or toward the 0.6%/month deferral increase — never both.
  Service Canada pays whichever is most advantageous, so we take the max
  over every switch month m: residence accrues until m, deferral credit
  from m to the start (credit capped at 60 months and never earned past
  age 70)."
  [in constants elig-ym start-ym]
  (let [m70 (u/ym+ (:birth-ym in) (inc (* 12 70)))
        credit-end (u/ym-min start-ym m70)
        rate (:deferral-increase-per-month constants)
        cap (:max-deferral-months constants)
        full-years (:full-pension-years constants)]
    (apply max-key :base
           (for [m (u/ym-range elig-ym start-ym)]
             (let [yrs (residence-years-at in constants m)
                   dm (min cap (max 0 (u/months-between m credit-end)))
                   factor (+ 1 (* rate dm))]
               {:residence-years yrs
                :fraction (/ yrs full-years)
                :deferral-months dm
                :deferral-factor factor
                :base (* (/ yrs full-years) factor)})))))

;; --- recovery tax ----------------------------------------------------------

(defn- recovery-tax
  "Annual and monthly clawback for a given gross monthly pension. Income
  for the recovery tax is net world income *including* the OAS pension
  itself, so the caller's :annual-income (which excludes OAS) has the
  annualized gross added to it here. Returns amounts as exact rationals."
  [rates-map in gross-monthly income-year]
  (let [{:keys [threshold year exact?]} (rates/clawback-threshold rates-map income-year)
        rate (get-in rates-map [:constants :recovery-tax-rate])
        annual-oas (* 12 gross-monthly)
        income (when (:annual-income in)
                 (+ (u/exact (:annual-income in)) annual-oas))
        annual (if income
                 (min annual-oas (* rate (max 0 (- income (u/exact threshold)))))
                 0)]
    {:annual annual
     :monthly (/ annual 12)
     :income-used income
     :threshold (u/exact threshold)
     :threshold-year year
     :threshold-exact? exact?
     ;; income (incl. OAS) at which this pension is fully clawed back
     :full-recovery-income (+ (u/exact threshold) (/ annual-oas rate))}))

;; --- assembly ---------------------------------------------------------------

(defn- band-payments
  "Monthly payment breakdown for one age band (:65-74 or :75-plus)."
  [rates-map quarter in calc uplift? income-year]
  (let [constants (:constants rates-map)
        full (cond-> (u/exact (:oas-65-74 quarter))
               uplift? (* (+ 1 (:age-75-uplift constants))))
        gross (* (:base calc) full)
        claw (recovery-tax rates-map in gross income-year)
        oas-net (- gross (:monthly claw))
        gis-payable? (and (:include-gis? in)
                          (:resides-in-canada? in)
                          (some? (:annual-income in)))
        gis-result (when gis-payable?
                     ;; partial pensioners get a bigger GIS maximum equal to
                     ;; their pension shortfall; a deferral-boosted pension
                     ;; (gross > full) never reduces GIS below the normal max
                     (gis/estimate quarter constants in (max 0 (- full gross))))
        gis-monthly (or (:monthly gis-result) 0)]
    {:oas-gross gross
     :recovery-tax (:monthly claw)
     :oas-net oas-net
     :gis gis-monthly
     :total (+ oas-net gis-monthly)
     :clawback claw
     :gis-details gis-result}))

(defn- round-monthly [band]
  {:oas-gross (u/round2 (:oas-gross band))
   :recovery-tax (u/round2 (:recovery-tax band))
   :oas-net (u/round2 (:oas-net band))
   :gis (u/round2 (:gis band))
   :total (u/round2 (:total band))})

(defn- annualize [band]
  (into {} (map (fn [[k v]] [k (u/round2 (* 12 (u/exact v)))])) (round-monthly band)))

(defn estimate
  "Estimate Old Age Security payments from a map of plain data.

  Required:
    :birth-date          \"YYYY-MM-DD\", \"YYYY-MM\", [year month] or
                         {:year y :month m}
    and one of:
    :years-in-canada     years of residence in Canada after age 18, as of
                         the 65th birthday (alias :years-in-canada-at-65);
                         residence is assumed to keep accruing after 65
                         while :resides-in-canada? is true
    :residence-periods   [[from to] ...] months of Canadian residence after
                         age 18 (to = nil for ongoing); overrides
                         :years-in-canada

  Optional:
    :start-date          first month of payment. Defaults to the first
                         eligible month (the month after the 65th birthday,
                         or when the residence minimum is met). Later start
                         dates earn the deferral credit.
    :annual-income       expected annual net income EXCLUDING OAS/GIS (CPP,
                         RRIF, private pensions, employment...). Used for
                         the recovery tax (OAS is added back internally)
                         and for GIS. Alias :retirement-income. When
                         omitted, no clawback or GIS is calculated.
    :employment-income   the employment/self-employment portion of
                         :annual-income (GIS earnings exemption)
    :marital-status      :single :widowed :divorced :married :common-law
                         (default :single)
    :partner-receives-oas?        \\
    :partner-receives-allowance?   > GIS rate category for couples
    :partner-annual-income        /  (+ :partner-employment-income)
    :involuntarily-separated?    couple living apart involuntarily: each
                                 assessed as single on own income
    :resides-in-canada?  default true. When false the 20-year residence
                         minimum applies and no GIS is payable.
    :agreement-satisfies-minimum?  an international social security
                         agreement meets the 10/20-year minimum (the
                         pension amount still uses actual Canadian years)
    :include-gis?        default true
    :as-of               [year month] etc. selecting the rate quarter
                         (default: latest known quarter)
    :income-year         tax year for the clawback threshold (default:
                         the :as-of year)
    :rates               partial override of oas.rates/default-rates

  Returns a map (money as 2-decimal BigDecimals, in :as-of dollars):
    :eligible?             false comes with :ineligible-reasons
    :first-eligible-month  [year month]
    :start-month           [year month] actually used
    :calculation           {:residence-years :fraction :deferral-months
                            :deferral-factor ...}
    :monthly / :annual     breakdown for the age band in effect at :as-of
                           (or at the start of payments, if later):
                           {:oas-gross :recovery-tax :oas-net :gis :total}
    :monthly-at-75         breakdown with the age-75 uplift (when relevant)
    :recovery-tax          threshold details and full-recovery income
    :gis                   GIS category/income details or nil
    :schedule              [{:from [y m] :age-band k :monthly {...}} ...]
    :notes / :warnings     human-readable explanations of applied rules"
  [inputs]
  (let [in (normalize inputs)
        rates-map (rates/merge-rates (:rates inputs))
        constants (:constants rates-map)
        as-of (or (:as-of-ym in) (:from (rates/latest-quarter rates-map)))
        quarter (or (rates/quarter-for rates-map as-of)
                    (invalid! ":as-of predates the rate table" {:as-of as-of}))
        income-year (or (:income-year in) (first as-of))
        elig (first-eligible-month in constants)
        m75 (u/ym+ (:birth-ym in) (inc (* 12 75)))]
    (if (nil? elig)
      {:eligible? false
       :ineligible-reasons
       [(if (:resides-in-canada? in)
          (str "Fewer than " (:min-years-in-canada constants)
               " years of Canadian residence after age 18, and the minimum is never reached.")
          (str "Residing outside Canada with fewer than " (:min-years-abroad constants)
               " years of Canadian residence after age 18. An international social security "
               "agreement may still satisfy the minimum (:agreement-satisfies-minimum? true)."))]}
      (let [requested-start (:start-ym in)
            start (if requested-start (u/ym-max requested-start elig) elig)
            m70+1 (u/ym+ (:birth-ym in) (inc (* 12 70)))
            calc (best-pension-base in constants elig start)
            at-start-uplift? (not (u/ym< start m75))
            start-band (band-payments rates-map quarter in calc at-start-uplift? income-year)
            band-75 (when-not at-start-uplift?
                      (band-payments rates-map quarter in calc true income-year))
            schedule (cond-> [{:from start
                               :age-band (if at-start-uplift? :75-plus :65-74)
                               :monthly (round-monthly start-band)}]
                       band-75 (conj {:from m75
                                      :age-band :75-plus
                                      :monthly (round-monthly band-75)}))
            ;; :monthly reflects the age band in effect at :as-of (or at the
            ;; start of payments, if that is later than :as-of)
            display-ym (u/ym-max start as-of)
            display-uplift? (not (u/ym< display-ym m75))
            display-band (if (and band-75 display-uplift?) band-75 start-band)
            claw (:clawback display-band)
            gis-details (:gis-details display-band)
            notes
            (cond-> ["All amounts are in the dollars of the selected rate quarter; OAS is indexed to CPI every January, April, July and October and never decreases."
                     "OAS payments are taxable income; GIS is not."]
              (< (:fraction calc) 1)
              (conj (str "Partial pension: " (:residence-years calc) "/40 years of Canadian residence after age 18."))
              (pos? (:deferral-months calc))
              (conj (str "Voluntary deferral: " (:deferral-months calc)
                         " months at 0.6%/month = +"
                         (u/round2 (* 100 (- (:deferral-factor calc) 1))) "%."
                         (when (:include-gis? in)
                           " No GIS is payable during the deferral period.")))
              (and requested-start (u/ym< elig requested-start))
              (conj "Deferral months were split between the 0.6%/month credit and continued residence accrual, whichever combination pays the most.")
              display-uplift?
              (conj "Includes the automatic 10% uplift for pensioners 75 and over.")
              band-75
              (conj (str "The pension increases by 10% automatically in the month after the 75th birthday ("
                         (first m75) "-" (format "%02d" (second m75)) "); see :monthly-at-75 and :schedule."))
              (nil? (:annual-income in))
              (conj "No :annual-income provided: recovery tax (clawback) and GIS were not assessed.")
              (and (:annual-income in) (pos? (:annual claw)))
              (conj (str "Recovery tax: 15% of net world income (including OAS) over the " (:threshold-year claw)
                         " threshold of $" (u/round2 (:threshold claw)) "."))
              gis-details
              (conj "GIS is an estimate from the statutory formula; official entitlements come from Service Canada's quarterly tables and the previous year's income.")
              (and (:include-gis? in) (not (:resides-in-canada? in)))
              (conj "GIS is not payable while residing outside Canada."))
            warnings
            (cond-> []
              (and requested-start (u/ym< requested-start elig))
              (conj (str "Requested start " requested-start " precedes first eligibility " elig "; using " elig "."))
              (u/ym< m70+1 start)
              (conj "Starting after age 70 earns no additional deferral credit (maximum 36% at 70). Applications can be paid up to 11 months retroactively.")
              (and (:annual-income in) (not (:threshold-exact? claw)))
              (conj (str "No clawback threshold on file for income year " income-year
                         "; used " (:threshold-year claw) ". Supply :rates {:clawback-thresholds {...}} to override."))
              (and (not (:resides-in-canada? in))
                   (< (residence-months-at in start) (* 12 20)))
              (conj "With fewer than 20 years of residence, OAS is only payable outside Canada for the month of departure plus six months."))]
        {:eligible? true
         :first-eligible-month elig
         :start-month start
         :as-of as-of
         :rates-quarter (:from quarter)
         :calculation {:residence-years (:residence-years calc)
                       :pension-fraction (:fraction calc)
                       :deferral-months (:deferral-months calc)
                       :deferral-factor (:deferral-factor calc)
                       :age-75-uplift-from m75}
         :monthly (round-monthly display-band)
         :annual (annualize display-band)
         :monthly-at-75 (round-monthly (or band-75 start-band))
         :recovery-tax (when (:annual-income in)
                         {:threshold (u/round2 (:threshold claw))
                          :threshold-year (:threshold-year claw)
                          :income-used (u/round2 (:income-used claw))
                          :annual (u/round2 (:annual claw))
                          :full-recovery-income (u/round2 (:full-recovery-income claw))})
         :gis (when gis-details
                {:category (:category gis-details)
                 :assessable-income (u/round2 (:assessable-income gis-details))
                 :income-cutoff (u/round2 (:cutoff gis-details))
                 :monthly (u/round2 (:monthly gis-details))
                 :estimate? true})
         :schedule schedule
         :notes notes
         :warnings warnings}))))
