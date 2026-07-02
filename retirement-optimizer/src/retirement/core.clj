(ns retirement.core
  "Public API for the retirement funding optimizer.

  A stateless, functional library for Canadian retirement drawdown
  planning: give it accounts, a goal and assumptions; get back a
  year-by-year withdrawal plan (Part 1) and Monte Carlo success rates
  (Part 2). See `retirement.inputs` for the full input schema and the
  README for worked examples.

    (require '[retirement.core :as r])

    (def inputs
      {:person {:age 65 :province :on
                :cpp {:start-age 70 :at-65 12000}
                :oas {:start-age 65}}
       :accounts [{:id :rrsp :type :rrsp :balance 500000
                   :holdings {:equity 0.6 :bonds 0.4}}
                  {:id :tfsa :type :tfsa :balance 120000
                   :holdings {:equity 0.8 :bonds 0.2}}
                  {:id :taxable :type :non-registered :balance 250000
                   :acb 180000 :holdings {:equity 0.7 :bonds 0.3}}]
       :goal {:type :spend-down :annual-spending 65000}
       :start-year 2026})

    (r/plan inputs)                  ;; Part 1: year-by-year drawdown plan
    (r/optimize inputs)              ;; best withdrawal strategy + plan
    (r/simulate inputs {:trials 1000 :seed 7})   ;; Part 2: success rate
    (r/sustainable-spending inputs {:target 0.95})
    (r/max-spending inputs)          ;; deterministic ceiling"
  (:require [retirement.inputs :as inputs]
            [retirement.optimize :as optimize]
            [retirement.plan :as plan]
            [retirement.simulate :as simulate]))

(defn plan
  "Year-by-year drawdown plan under expected returns. Optionally pass an
  explicit market path (a seq of {:equity :bonds :cash :inflation}) as the
  second argument. See `retirement.plan/run-plan`."
  ([inputs] (plan/run-plan inputs))
  ([inputs market-path] (plan/run-plan inputs market-path)))

(defn optimize
  "Grid-search withdrawal strategies; returns {:best {... :plan ...}
  :ranking [...]}. See `retirement.optimize/optimize` for options."
  ([inputs] (optimize/optimize inputs))
  ([inputs opts] (optimize/optimize inputs opts)))

(defn simulate
  "Monte Carlo simulation; returns success rate, estate percentiles and
  per-year balance bands. See `retirement.simulate/simulate` for options."
  ([inputs] (simulate/simulate inputs))
  ([inputs opts] (simulate/simulate inputs opts)))

(defn sustainable-spending
  "Highest real annual spending sustained in >= :target of trials.
  See `retirement.simulate/sustainable-spending`."
  ([inputs] (simulate/sustainable-spending inputs))
  ([inputs opts] (simulate/sustainable-spending inputs opts)))

(defn max-spending
  "Deterministic maximum sustainable real annual spending under expected
  returns. See `retirement.plan/solve-max-spending`."
  [inputs]
  (plan/solve-max-spending inputs))

(defn validate
  "Vector of human-readable input problems; empty when valid."
  [inputs]
  (inputs/validate inputs))

(def default-assumptions inputs/default-assumptions)
