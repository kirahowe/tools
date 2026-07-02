(ns retirement.benefits
  "Canadian government retirement benefits: CPP, OAS, GIS.

  All amounts are computed from base-year (2025) dollar values in
  `retirement.taxdata` scaled by a cumulative inflation factor, except CPP
  where the user supplies their own expected entitlement at 65 in
  start-of-plan dollars.

  Simplifications: CPP is price-indexed both before and after take-up
  (in reality it is wage-indexed before take-up, which usually grows
  slightly faster); GIS uses a flat 50% reduction rate (the real top-up
  portion phases out at a higher combined rate, so this model is slightly
  generous near the cutoff); GIS is based on current-year income rather
  than the prior year's."
  (:require [retirement.taxdata :as data]))

(defn- clamp [x lo hi] (-> x (max lo) (min hi)))

(defn cpp-adjustment
  "Multiplier on the age-65 CPP entitlement for a given start age.
  -0.6%/month before 65 (floor age 60), +0.7%/month after (cap age 70)."
  [start-age]
  (let [{:keys [early-reduction-per-month late-increase-per-month
                earliest-age latest-age]} (:cpp data/benefits)
        age (clamp start-age earliest-age latest-age)
        months (* 12.0 (- age 65))]
    (if (neg? months)
      (+ 1.0 (* early-reduction-per-month months))
      (+ 1.0 (* late-increase-per-month months)))))

(defn cpp-annual
  "Annual CPP for the year a person is `age`, given the plan's CPP config
  {:start-age .. :at-65 ..} (at-65 in start-year dollars) and the inflation
  index relative to the start year. Zero before the start age."
  [{:keys [start-age at-65] :or {start-age 65}} age year-index]
  (if (and at-65 (>= age (clamp start-age 60 70)))
    (* at-65 (cpp-adjustment start-age) year-index)
    0.0))

(defn oas-adjustment
  "Deferral multiplier on OAS: +0.6%/month past 65, max +36% at 70."
  [start-age]
  (let [{:keys [deferral-per-month earliest-age latest-age]} (:oas data/benefits)
        age (clamp start-age earliest-age latest-age)]
    (+ 1.0 (* deferral-per-month 12.0 (- age earliest-age)))))

(defn oas-annual
  "Annual OAS (before any clawback) at `age`, given OAS config
  {:start-age .. :fraction ..} and the cumulative inflation factor relative
  to the tax data base year. Includes the +10% boost from age 75.
  :fraction covers partial residency (years-in-Canada / 40), default 1.0."
  [{:keys [start-age fraction] :or {start-age 65 fraction 1.0}} age base-factor]
  (let [{:keys [annual-at-65 boost-at-75 earliest-age]} (:oas data/benefits)
        start (clamp start-age earliest-age 70)]
    (if (>= age start)
      (* annual-at-65 base-factor fraction
         (oas-adjustment start)
         (if (>= age 75) (+ 1.0 boost-at-75) 1.0))
      0.0)))

(defn gis-annual
  "Annual GIS for a single person receiving OAS. `income-excl-oas` is
  net income excluding OAS and GIS (TFSA withdrawals do not count).
  Non-taxable. Zero if not receiving OAS."
  [receiving-oas? income-excl-oas base-factor]
  (if-not receiving-oas?
    0.0
    (let [{:keys [annual-max-single reduction-rate]} (:gis data/benefits)]
      (max 0.0 (- (* annual-max-single base-factor)
                  (* reduction-rate (max 0.0 income-excl-oas)))))))
