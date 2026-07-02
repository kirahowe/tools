(ns retirement.taxdata
  "Canadian tax and benefit constants, encoded as plain data.

  Base year: 2025. Federal and Ontario figures are verified against CRA /
  ESDC published amounts (see README for sources). BC and Alberta tables are
  best-effort 2025 values — check before relying on them, and replace via
  the same data schema if needed.

  The engine indexes every dollar threshold by cumulative inflation from
  `base-year`, except items marked :indexed? false (e.g. the federal $2,000
  pension amount, Ontario's top two bracket thresholds, and the Ontario
  health premium, none of which are indexed in law).

  2025 quirk: Bill C-4 cut the lowest federal rate from 15% to 14% effective
  July 1 2025, making the effective 2025 rate 14.5% and the 2026+ rate 14%.
  `federal-for-year` applies this. Non-refundable credits use the same rate.")

(def base-year 2025)

;; ---------------------------------------------------------------------------
;; RRIF minimum withdrawal factors (CRA prescribed, post-2015 rules)

(def rrif-minimum-factors
  "Prescribed factor by age for ages 71+. Below 71 the factor is 1/(90 - age)."
  {71 0.0528, 72 0.0540, 73 0.0553, 74 0.0567, 75 0.0582, 76 0.0598
   77 0.0617, 78 0.0636, 79 0.0658, 80 0.0682, 81 0.0708, 82 0.0738
   83 0.0771, 84 0.0808, 85 0.0851, 86 0.0899, 87 0.0955, 88 0.1021
   89 0.1099, 90 0.1192, 91 0.1306, 92 0.1449, 93 0.1634, 94 0.1879
   95 0.2000})

(defn rrif-minimum-factor
  "Minimum withdrawal factor for a RRIF holder of the given age
  (age at the start of the year, i.e. attained in the prior year)."
  [age]
  (cond
    (>= age 95) 0.20
    (>= age 71) (rrif-minimum-factors age)
    (>= age 90) 0.20
    :else (/ 1.0 (- 90 age))))

;; ---------------------------------------------------------------------------
;; Federal (2025 base values; thresholds indexed by inflation from 2025)

(def federal
  {:brackets [{:up-to 57375.0 :rate 0.145}
              {:up-to 114750.0 :rate 0.205}
              {:up-to 177882.0 :rate 0.26}
              {:up-to 253414.0 :rate 0.29}
              {:up-to nil :rate 0.33}]
   :credit-rate 0.145
   ;; Enhanced BPA: $16,129 max, phased down to $14,538 between the 4th and
   ;; 5th bracket thresholds.
   :bpa {:max 16129.0 :min 14538.0
         :phase-out-start 177882.0 :phase-out-end 253414.0}
   :age-amount {:max 9028.0 :threshold 45522.0 :rate 0.15}
   :pension-amount {:amount 2000.0 :indexed? false}
   :dividends {:eligible {:gross-up 0.38 :credit 0.150198}
               :non-eligible {:gross-up 0.15 :credit 0.090301}}
   :capital-gains-inclusion 0.5
   :oas-clawback {:threshold 93454.0 :rate 0.15}})

(defn federal-for-year
  "Federal table with the correct lowest bracket / credit rate for `year`
  (14.5% in 2025, 14% from 2026 on). Values remain in 2025 dollars; the
  tax engine applies inflation indexing."
  [year]
  (if (>= year 2026)
    (-> federal
        (assoc-in [:brackets 0 :rate] 0.14)
        (assoc :credit-rate 0.14))
    federal))

;; ---------------------------------------------------------------------------
;; Provinces (2025 base values)

(def provinces
  {;; Ontario — verified 2025 figures.
   :on {:name "Ontario"
        :brackets [{:up-to 52886.0 :rate 0.0505}
                   {:up-to 105775.0 :rate 0.0915}
                   {:up-to 150000.0 :rate 0.1116 :indexed? false}
                   {:up-to 220000.0 :rate 0.1216 :indexed? false}
                   {:up-to nil :rate 0.1316}]
        :credit-rate 0.0505
        :bpa {:max 12747.0}
        :age-amount {:max 6223.0 :threshold 46330.0 :rate 0.15}
        :pension-amount {:amount 1762.0 :indexed? true}
        :dividend-credits {:eligible 0.10 :non-eligible 0.029863}
        ;; 20% of basic ON tax over $5,710 plus 36% over $7,307.
        :surtax [{:threshold 5710.0 :rate 0.20}
                 {:threshold 7307.0 :rate 0.36}]
        ;; Ontario Health Premium: piecewise on taxable income, NOT indexed.
        ;; Segments: [income-floor, premium-at-floor, marginal-rate, cap].
        :health-premium [[20000.0 0.0 0.06 300.0]
                         [36000.0 300.0 0.06 450.0]
                         [48000.0 450.0 0.25 600.0]
                         [72000.0 600.0 0.25 750.0]
                         [200000.0 750.0 0.25 900.0]]}

   ;; British Columbia — best-effort 2025 figures.
   :bc {:name "British Columbia"
        :approximate? true
        :brackets [{:up-to 49279.0 :rate 0.0506}
                   {:up-to 98560.0 :rate 0.0770}
                   {:up-to 113158.0 :rate 0.1050}
                   {:up-to 137407.0 :rate 0.1229}
                   {:up-to 186306.0 :rate 0.1470}
                   {:up-to 259829.0 :rate 0.1680}
                   {:up-to nil :rate 0.2050}]
        :credit-rate 0.0506
        :bpa {:max 12932.0}
        :age-amount {:max 5373.0 :threshold 40011.0 :rate 0.15}
        :pension-amount {:amount 1000.0 :indexed? false}
        :dividend-credits {:eligible 0.12 :non-eligible 0.0196}}

   ;; Alberta — best-effort 2025 figures (includes the new 8% bracket).
   :ab {:name "Alberta"
        :approximate? true
        :brackets [{:up-to 60000.0 :rate 0.08}
                   {:up-to 151234.0 :rate 0.10}
                   {:up-to 181481.0 :rate 0.12}
                   {:up-to 241974.0 :rate 0.13}
                   {:up-to 362961.0 :rate 0.14}
                   {:up-to nil :rate 0.15}]
        :credit-rate 0.08
        :bpa {:max 22323.0}
        :age-amount {:max 6131.0 :threshold 45650.0 :rate 0.15}
        :pension-amount {:amount 1685.0 :indexed? true}
        :dividend-credits {:eligible 0.0812 :non-eligible 0.0218}}})

;; ---------------------------------------------------------------------------
;; Government benefits (2025 base values, indexed by inflation from 2025)

(def benefits
  {:cpp {;; Max monthly at 65 in 2025 is $1,433.00; users supply their own
         ;; expected entitlement — this is only used for validation warnings.
         :max-annual-at-65 17196.0
         :early-reduction-per-month 0.006   ; -36% max at age 60
         :late-increase-per-month 0.007     ; +42% max at age 70
         :earliest-age 60
         :latest-age 70}
   :oas {:annual-at-65 8732.0               ; $727.67/mo, Jan–Mar 2025
         :boost-at-75 0.10
         :deferral-per-month 0.006          ; +36% max at age 70
         :earliest-age 65
         :latest-age 70
         :full-residency-years 40}
   :gis {:annual-max-single 13042.6         ; $1,086.88/mo, Jan–Mar 2025
         :reduction-rate 0.5}               ; per $1 of non-OAS income
   :tfsa {:annual-limit 7000.0}})
