(ns retirement.tax
  "Pure Canadian personal income tax calculations.

  All functions are referentially transparent: they take a tax table (see
  `retirement.taxdata`), an inflation index factor relative to the table's
  base year, and an income description, and return tax amounts.

  Income description map:
    :ordinary        fully-taxed income (CPP, OAS, RRSP/RRIF withdrawals,
                     interest, employment, etc.)
    :eligible-dividends      cash amount of eligible Canadian dividends
    :non-eligible-dividends  cash amount of non-eligible dividends
    :capital-gains   realized capital gains (before inclusion rate)
    :pension-income  income qualifying for the pension credit (RRIF/LIF
                     withdrawals at 65+, DB pension at any age)
    :age             age at year end

  Simplifications (documented in README): net income == taxable income
  (no deductions modeled); OAS recovery tax is applied in the same year as
  the income that triggers it.

  All functions take the tax-year table (see `retirement.taxdata`)
  explicitly — nothing here reaches for global data.")

(defn index-value
  "Scale a dollar threshold by the inflation factor unless marked unindexed."
  ([v factor] (* v factor))
  ([v factor indexed?] (if indexed? (* v factor) v)))

(defn index-brackets
  [brackets factor]
  (mapv (fn [{:keys [up-to rate indexed?] :or {indexed? true}}]
          {:up-to (when up-to (index-value up-to factor indexed?))
           :rate rate})
        brackets))

(defn bracket-tax
  "Progressive tax on `taxable` given indexed brackets."
  [brackets taxable]
  (loop [total 0.0
         lower 0.0
         bs brackets]
    (if (or (empty? bs) (<= taxable lower))
      total
      (let [{:keys [up-to rate]} (first bs)
            upper (or up-to Double/MAX_VALUE)
            slice (- (min taxable upper) lower)]
        (recur (+ total (* (max 0.0 slice) rate))
               upper
               (rest bs))))))

(defn taxable-income
  "Taxable income: ordinary + grossed-up dividends + included capital gains."
  [table income]
  (let [{:keys [eligible non-eligible]} (get-in table [:dividends]
                                                {:eligible {:gross-up 0.38}
                                                 :non-eligible {:gross-up 0.15}})
        incl (get table :capital-gains-inclusion 0.5)]
    (+ (double (:ordinary income 0.0))
       (* (:eligible-dividends income 0.0) (+ 1.0 (:gross-up eligible)))
       (* (:non-eligible-dividends income 0.0) (+ 1.0 (:gross-up non-eligible)))
       (* (max 0.0 (:capital-gains income 0.0)) incl))))

(defn basic-personal-amount
  "Federal enhanced BPA with high-income phase-out; provincial BPA is flat."
  [{:keys [max min phase-out-start phase-out-end]} factor net-income]
  (let [mx (index-value max factor)]
    (if (nil? min)
      mx
      (let [mn (index-value min factor)
            s (index-value phase-out-start factor)
            e (index-value phase-out-end factor)]
        (cond
          (<= net-income s) mx
          (>= net-income e) mn
          :else (- mx (* (- mx mn) (/ (- net-income s) (- e s)))))))))

(defn age-amount
  "Age credit base amount (65+), reduced by `rate` of net income over the
  threshold. Some provinces (e.g. Nova Scotia) don't index the threshold —
  mark those with :threshold-indexed? false."
  [{:keys [max threshold rate threshold-indexed?] :or {threshold-indexed? true}}
   factor age net-income]
  (if (and age (>= age 65))
    (let [thr (index-value threshold factor threshold-indexed?)]
      (clojure.core/max
       0.0
       (- (index-value max factor)
          (* rate (clojure.core/max 0.0 (- net-income thr))))))
    0.0))

(defn pension-amount
  "Pension income credit base amount."
  [{:keys [amount indexed?]} factor pension-income]
  (min (index-value amount factor (boolean indexed?))
       (clojure.core/max 0.0 (double (or pension-income 0.0)))))

(defn- dividend-tax-credits
  [income elig-rate non-elig-rate elig-gross-up non-elig-gross-up]
  (+ (* (:eligible-dividends income 0.0) (+ 1.0 elig-gross-up) elig-rate)
     (* (:non-eligible-dividends income 0.0) (+ 1.0 non-elig-gross-up) non-elig-rate)))

(defn federal-tax
  "Net federal tax (after non-refundable credits, floored at zero)."
  [fed factor income]
  (let [ti (taxable-income fed income)
        net-income ti
        gross (bracket-tax (index-brackets (:brackets fed) factor) ti)
        credit-base (+ (basic-personal-amount (:bpa fed) factor net-income)
                       (age-amount (:age-amount fed) factor (:age income) net-income)
                       (pension-amount (:pension-amount fed) factor (:pension-income income)))
        credits (* credit-base (:credit-rate fed))
        dtc (dividend-tax-credits income
                                  (get-in fed [:dividends :eligible :credit])
                                  (get-in fed [:dividends :non-eligible :credit])
                                  (get-in fed [:dividends :eligible :gross-up])
                                  (get-in fed [:dividends :non-eligible :gross-up]))]
    (max 0.0 (- gross credits dtc))))

(defn- health-premium
  "Ontario Health Premium (piecewise on taxable income, not indexed)."
  [segments ti]
  (if (or (nil? segments) (<= ti (ffirst segments)))
    0.0
    (let [[floor base rate cap]
          (last (filter (fn [[floor]] (> ti floor)) segments))]
      (min cap (+ base (* rate (- ti floor)))))))

(defn provincial-tax
  "Net provincial tax including surtax and health premium where applicable.
  Uses the federal table's gross-ups and inclusion rate to compute taxable
  income (as in the real system)."
  [prov fed factor income]
  (let [ti (taxable-income fed income)
        net-income ti
        gross (bracket-tax (index-brackets (:brackets prov) factor) ti)
        credit-base (+ (basic-personal-amount (:bpa prov) factor net-income)
                       (age-amount (:age-amount prov) factor (:age income) net-income)
                       (pension-amount (:pension-amount prov) factor (:pension-income income)))
        credits (* credit-base (:credit-rate prov))
        dtc (dividend-tax-credits income
                                  (get-in prov [:dividend-credits :eligible])
                                  (get-in prov [:dividend-credits :non-eligible])
                                  (get-in fed [:dividends :eligible :gross-up])
                                  (get-in fed [:dividends :non-eligible :gross-up]))
        basic (max 0.0 (- gross credits dtc))
        surtax (reduce (fn [acc {:keys [threshold rate]}]
                         (+ acc (* rate (max 0.0 (- basic (index-value threshold factor))))))
                       0.0
                       (:surtax prov))
        premium (health-premium (:health-premium prov) ti)]
    {:basic basic :surtax surtax :health-premium premium
     :total (+ basic surtax premium)}))

(defn oas-clawback
  "OAS recovery tax: 15% of net income above the (indexed) threshold,
  capped at the OAS actually received."
  [fed factor net-income oas-received]
  (let [{:keys [threshold rate]} (:oas-clawback fed)]
    (min (double oas-received)
         (* rate (max 0.0 (- net-income (index-value threshold factor)))))))

(defn income-tax
  "Total income tax for one person-year.

  `table` is the tax-year table to apply (see `retirement.taxdata`),
  `factor` is the cumulative inflation index relative to that table's
  :year, `province` is a key in the table's :provinces, and `income` is
  the income description map (see namespace doc). `oas-received` (in
  :ordinary already) is needed to cap the clawback.

  Returns a detail map; :total includes federal + provincial + OAS clawback."
  [{:keys [table factor province income oas-received]
    :or {oas-received 0.0}}]
  (let [fed (:federal table)
        prov (get-in table [:provinces province])
        _ (when-not prov
            (throw (ex-info (str "Unknown province: " province
                                 ". Known: " (sort (keys (:provinces table))))
                            {:province province})))
        ti (taxable-income fed income)
        federal (federal-tax fed factor income)
        provincial (provincial-tax prov fed factor income)
        clawback (oas-clawback fed factor ti oas-received)]
    {:taxable-income ti
     :net-income ti
     :federal federal
     :provincial (:basic provincial)
     :surtax (:surtax provincial)
     :health-premium (:health-premium provincial)
     :oas-clawback clawback
     :total (+ federal (:total provincial) clawback)}))
