(ns oas.gis
  "Guaranteed Income Supplement (GIS) estimation.

  GIS is a non-taxable, income-tested supplement paid on top of OAS to
  low-income pensioners residing in Canada. This namespace models the
  statutory structure rather than reproducing Service Canada's lookup
  tables, so results are close estimates (the official tables move in
  $24/$48 annual-income brackets and round independently):

    - income for GIS excludes OAS/GIS itself and gets an earnings
      exemption (first $5,000 of employment/self-employment earnings,
      plus 50% of the next $10,000 — per person)
    - the base supplement is reduced by $1/month for every $2/month of
      income for singles (i.e. income/24 annually), and by $1/month per
      $4/month of combined income for each member of a couple where both
      receive OAS (income/48)
    - the GIS top-up (introduced 2011, enhanced for singles 2016) is
      phased out at 25 cents per dollar of income above $2,000 for
      singles, and 12.5 cents each above $4,000 combined for couples
    - a pensioner receiving a partial OAS pension has their GIS maximum
      increased by the shortfall between the full and partial pension
      (income-tested at the base rate)"
  (:require [oas.util :as u]))

(defn earnings-exemption
  "Exempt portion of one person's annual employment/self-employment
  earnings for GIS purposes."
  [constants earnings]
  (let [e (u/exact (or earnings 0))
        full (:gis-full-earnings-exemption constants)
        partial-band (:gis-partial-earnings-exemption constants)]
    (+ (min e full)
       (* 1/2 (min (max (- e full) 0) partial-band)))))

(defn assessable-income
  "Annual income counted against GIS: non-OAS income minus the earnings
  exemption, for the pensioner alone or combined with their partner."
  [constants {:keys [annual-income employment-income
                     partner-annual-income partner-employment-income]}
   combined?]
  (let [person (fn [income earnings]
                 (max 0 (- (u/exact (or income 0))
                           (earnings-exemption constants earnings))))]
    (+ (person annual-income employment-income)
       (if combined?
         (person partner-annual-income partner-employment-income)
         0))))

(defn category
  "GIS rate category for the household.

  Couples forced to live apart for reasons beyond their control (e.g. one
  partner in long-term care) are each assessed as single, on individual
  income (:involuntarily-separated? true)."
  [{:keys [marital-status partner-receives-oas? partner-receives-allowance?
           involuntarily-separated?]}]
  (cond
    involuntarily-separated? :single
    (contains? #{nil :single :widowed :divorced} marital-status) :single
    partner-receives-oas? :partner-oas
    partner-receives-allowance? :partner-allowance
    :else :partner-no-oas))

(defn estimate
  "Estimated monthly GIS (exact rational, unrounded) for one pensioner.

  quarter    - a rate quarter map from oas.rates
  constants  - (:constants rates)
  inputs     - normalized user inputs (income fields, marital status flags)
  shortfall  - monthly gap between the full and the pensioner's own OAS
               pension (0 for a full 40/40 pension); increases the GIS
               maximum for partial pensioners

  Returns {:monthly x :category k :assessable-income i :cutoff c}."
  [quarter constants inputs shortfall]
  (let [cat (category inputs)
        combined? (and (not= cat :single)
                       (not (:involuntarily-separated? inputs)))
        {gis-max :max cutoff :cutoff} (get-in quarter [:gis cat])
        max* (u/exact gis-max)
        cutoff* (u/exact cutoff)
        income (assessable-income constants inputs combined?)
        monthly
        (case cat
          ;; single-rate maximum, couple income test; the published cutoff
          ;; embeds this category's special deductions, so model a straight
          ;; line from the maximum down to zero at the cutoff.
          :partner-no-oas
          (max 0 (- (+ max* shortfall)
                    (* income (/ max* cutoff*))))

          ;; base + top-up, each with its own statutory phase-out
          (let [single? (= cat :single)
                base (/ cutoff* (if single? 24 48))
                topup (- max* base)
                base-reduction (/ income (if single? 24 48))
                topup-floor (if single?
                              (:gis-topup-exemption-single constants)
                              (:gis-topup-exemption-couple constants))
                topup-reduction (/ (max 0 (- income topup-floor))
                                   (if single? 48 96))]
            (+ (max 0 (- (+ base shortfall) base-reduction))
               (max 0 (- topup topup-reduction)))))]
    {:monthly monthly
     :category cat
     :assessable-income income
     :cutoff cutoff*}))
