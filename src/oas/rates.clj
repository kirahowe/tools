(ns oas.rates
  "Benefit rates and program parameters for Old Age Security, as plain data.

  OAS (and GIS) maximums are indexed to CPI every quarter (January, April,
  July, October) and by law can never decrease. The recovery-tax (\"clawback\")
  threshold is indexed annually per income tax year. Because these figures
  change constantly, they live here as data: every public function accepts a
  `:rates` override, so callers can supply newer quarters or corrected values
  without touching code.

  Provenance: OAS maximums and clawback thresholds below were cross-checked
  against Service Canada / canada.ca quarterly figures as republished by
  multiple secondary sources (July 2026). Figures marked `derived` were
  computed from the adjacent attested quarters using the CPI indexation
  factor and validated against the internal identities that hold for the
  official tables:
    - 75+ maximum = 1.10 x (65-74 maximum), rounded to the cent
    - GIS single cutoff = 24 x the base (non-top-up) GIS maximum
    - GIS couple cutoff = 48 x the base GIS maximum (each)
  GIS table values are estimates good to within a few cents; official
  entitlements come from Service Canada's published quarterly tables."
  (:require [oas.util :as u]))

(def constants
  "Structural program parameters. These are set by the OAS Act and change
  rarely (unlike the quarterly dollar amounts)."
  {;; Voluntary deferral: +0.6% per month deferred past eligibility,
   ;; to a maximum of 60 months (36%). No credit accrues past age 70.
   :deferral-increase-per-month 6/1000
   :max-deferral-months 60
   ;; Full pension requires 40 years of Canadian residence after age 18;
   ;; a partial pension is (whole years)/40.
   :full-pension-years 40
   ;; Minimum residence to qualify at all: 10 years if residing in Canada,
   ;; 20 years if applying from outside Canada. (An international social
   ;; security agreement can satisfy the minimum via totalization.)
   :min-years-in-canada 10
   :min-years-abroad 20
   ;; Automatic increase to the pension in the month after the 75th birthday
   ;; (permanent measure since July 2022).
   :age-75-uplift 1/10
   ;; Recovery tax: 15% of net world income above the year's threshold,
   ;; capped at the OAS actually received.
   :recovery-tax-rate 15/100
   ;; GIS earnings exemption: first $5,000 of employment/self-employment
   ;; earnings fully exempt, plus 50% of the next $10,000.
   :gis-full-earnings-exemption 5000
   :gis-partial-earnings-exemption 10000
   ;; GIS top-up phase-out floors (set in 2011, not indexed):
   ;; single top-up reduced 25 cents/$ of income above $2,000;
   ;; couple top-ups reduced 12.5 cents/$ each above $4,000 combined.
   :gis-topup-exemption-single 2000
   :gis-topup-exemption-couple 4000})

(def quarters
  "Maximum monthly amounts by quarter, most recent last.
  :from is the first [year month] the quarter's rates apply to.

  GIS entries: :max is the published maximum monthly supplement (includes
  the GIS top-up); :cutoff is the annual income (excluding OAS/GIS, after
  the earnings exemption) at which the supplement reaches zero. The base
  (pre-top-up) maximum is recovered as cutoff/24 for singles and cutoff/48
  (each) for couples where both receive OAS.

  Categories:
    :single             single, widowed or divorced
    :partner-oas        spouse/common-law partner receives the full OAS pension
    :partner-allowance  partner receives the Allowance (ages 60-64)
    :partner-no-oas     partner receives neither OAS nor the Allowance
                        (single-rate maximum, couple income test)"
  [{:from [2025 1]                                        ; attested
    :oas-65-74 727.67M :oas-75-plus 800.44M
    :gis {:single            {:max 1086.88M :cutoff 22056M}
          :partner-oas       {:max 654.23M  :cutoff 29136M}
          :partner-allowance {:max 654.23M  :cutoff 29136M}
          :partner-no-oas    {:max 1086.88M :cutoff 52848M}}}
   {:from [2025 4]                                        ; unchanged (flat CPI)
    :oas-65-74 727.67M :oas-75-plus 800.44M
    :gis {:single            {:max 1086.88M :cutoff 22056M}
          :partner-oas       {:max 654.23M  :cutoff 29136M}
          :partner-allowance {:max 654.23M  :cutoff 29136M}
          :partner-no-oas    {:max 1086.88M :cutoff 52848M}}}
   {:from [2025 7]                                        ; attested (+1.0%)
    :oas-65-74 734.95M :oas-75-plus 808.45M
    :gis {:single            {:max 1097.75M :cutoff 22272M}
          :partner-oas       {:max 660.77M  :cutoff 29424M}
          :partner-allowance {:max 660.77M  :cutoff 29424M}
          :partner-no-oas    {:max 1097.75M :cutoff 53376M}}}
   {:from [2025 10]                                       ; unchanged (flat CPI)
    :oas-65-74 734.95M :oas-75-plus 808.45M
    :gis {:single            {:max 1097.75M :cutoff 22272M}
          :partner-oas       {:max 660.77M  :cutoff 29424M}
          :partner-allowance {:max 660.77M  :cutoff 29424M}
          :partner-no-oas    {:max 1097.75M :cutoff 53376M}}}
   {:from [2026 1]                                        ; OAS attested; GIS single derived
    :oas-65-74 742.31M :oas-75-plus 816.54M
    :gis {:single            {:max 1108.74M :cutoff 22512M}
          :partner-oas       {:max 667.41M  :cutoff 29712M}
          :partner-allowance {:max 667.41M  :cutoff 29712M}
          :partner-no-oas    {:max 1108.74M :cutoff 53904M}}}
   {:from [2026 4]                                        ; attested (+0.1%)
    :oas-65-74 743.05M :oas-75-plus 817.36M
    :gis {:single            {:max 1109.85M :cutoff 22536M}
          :partner-oas       {:max 668.08M  :cutoff 29760M}
          :partner-allowance {:max 668.08M  :cutoff 29760M}
          :partner-no-oas    {:max 1109.85M :cutoff 53952M}}}
   {:from [2026 7]                                        ; attested (+1.2%); GIS partner derived
    :oas-65-74 751.97M :oas-75-plus 827.17M
    :gis {:single            {:max 1123.17M :cutoff 22800M}
          :partner-oas       {:max 676.10M  :cutoff 30096M}
          :partner-allowance {:max 676.10M  :cutoff 30096M}
          :partner-no-oas    {:max 1123.17M :cutoff 54624M}}}])

(def clawback-thresholds
  "Recovery-tax income thresholds by income tax year. The clawback for
  income year Y is repaid via withholding from July of Y+1 through June
  of Y+2, but economically it is 15% of income above the year's threshold."
  {2021 79845M
   2022 81761M
   2023 86912M
   2024 90997M
   2025 93454M
   2026 95323M})

(def default-rates
  {:constants constants
   :quarters quarters
   :clawback-thresholds clawback-thresholds})

(defn merge-rates
  "Merge a partial user-supplied rates map over the defaults. Quarters are
  concatenated and re-sorted (user quarters with the same :from replace the
  defaults); other keys are merged shallowly per section."
  [overrides]
  (if (nil? overrides)
    default-rates
    {:constants (merge constants (:constants overrides))
     :quarters (->> (concat quarters (:quarters overrides))
                    (reduce (fn [m q] (assoc m (:from q) q)) (sorted-map-by
                                                              (fn [a b] (compare (u/ym->months a)
                                                                                 (u/ym->months b)))))
                    vals
                    vec)
     :clawback-thresholds (merge clawback-thresholds (:clawback-thresholds overrides))}))

(defn quarter-for
  "The rate quarter in effect for month `ym` ([year month]): the latest
  quarter whose :from is on or before ym. Returns nil if ym predates the
  table."
  [rates ym]
  (->> (:quarters rates)
       (filter #(u/ym<= (:from %) ym))
       last))

(defn latest-quarter [rates]
  (last (:quarters rates)))

(defn clawback-threshold
  "Recovery-tax threshold for an income year. Falls back to the most recent
  known year (returned as {:threshold x :year y :exact? bool})."
  [rates year]
  (let [table (:clawback-thresholds rates)]
    (if-let [t (get table year)]
      {:threshold t :year year :exact? true}
      (let [[y t] (apply max-key key table)]
        {:threshold t :year y :exact? false}))))
