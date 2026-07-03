(ns retirement.benefits
  "Canadian government retirement benefits: CPP, OAS, GIS.

  All functions take their configuration (the :benefits section of a tax
  year table — see `retirement.taxdata`) explicitly, so benefit amounts and
  adjustment rules are pluggable data like the tax tables. Dollar values in
  the config are in the table's :year dollars and are scaled by `factor`,
  the cumulative inflation index relative to that year. CPP is the
  exception: the user supplies their own expected entitlement at 65 in
  start-of-plan dollars, scaled by `year-index` (CPI vs the plan start).

  Simplifications: CPP is price-indexed both before and after take-up
  (in reality it is wage-indexed before take-up, which usually grows
  slightly faster); GIS uses a flat 50% reduction rate (the real top-up
  portion phases out at a higher combined rate, so this model is slightly
  generous near the cutoff); GIS is based on current-year income rather
  than the prior year's.")

(defn- clamp [x lo hi] (-> x (max lo) (min hi)))

(defn cpp-adjustment
  "Multiplier on the age-65 CPP entitlement for a given start age.
  Default rules: -0.6%/month before 65 (floor 60), +0.7%/month after (cap 70)."
  [{:keys [early-reduction-per-month late-increase-per-month
           earliest-age latest-age]}
   start-age]
  (let [age (clamp start-age earliest-age latest-age)
        months (* 12.0 (- age 65))]
    (if (neg? months)
      (+ 1.0 (* early-reduction-per-month months))
      (+ 1.0 (* late-increase-per-month months)))))

(defn cpp-annual
  "Annual CPP for the year a person is `age`, given the table's CPP config
  and the person's {:start-age .. :at-65 ..} (at-65 in start-year dollars).
  Zero before the start age."
  [cpp-cfg {:keys [start-age at-65] :or {start-age 65}} age year-index]
  (if (and at-65
           (>= age (clamp start-age (:earliest-age cpp-cfg) (:latest-age cpp-cfg))))
    (* at-65 (cpp-adjustment cpp-cfg start-age) year-index)
    0.0))

(defn oas-adjustment
  "Deferral multiplier on OAS: +0.6%/month past 65 by default, max +36% at 70."
  [{:keys [deferral-per-month earliest-age latest-age]} start-age]
  (let [age (clamp start-age earliest-age latest-age)]
    (+ 1.0 (* deferral-per-month 12.0 (- age earliest-age)))))

(defn oas-annual
  "Annual OAS (before any clawback) at `age`, given the table's OAS config,
  the person's {:start-age .. :fraction ..} and the inflation factor
  relative to the table year. Includes the boost from age 75.
  :fraction covers partial residency (years-in-Canada / 40), default 1.0."
  [{:keys [annual-at-65 boost-at-75 earliest-age latest-age] :as oas-cfg}
   {:keys [start-age fraction] :or {start-age 65 fraction 1.0}}
   age factor]
  (let [start (clamp start-age earliest-age latest-age)]
    (if (>= age start)
      (* annual-at-65 factor fraction
         (oas-adjustment oas-cfg start)
         (if (>= age 75) (+ 1.0 boost-at-75) 1.0))
      0.0)))

(defn gis-annual
  "Annual GIS for a single person receiving OAS, given the table's GIS
  config. `income-excl-oas` is net income excluding OAS and GIS (TFSA
  withdrawals do not count). Non-taxable. Zero if not receiving OAS."
  [{:keys [annual-max-single reduction-rate]} receiving-oas? income-excl-oas factor]
  (if-not receiving-oas?
    0.0
    (max 0.0 (- (* annual-max-single factor)
                (* reduction-rate (max 0.0 income-excl-oas))))))
