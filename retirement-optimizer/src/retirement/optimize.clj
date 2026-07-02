(ns retirement.optimize
  "Strategy optimization: grid search over withdrawal strategies.

  Why grid search and not a constraint solver (core.logic etc.): the
  decision space is continuous and the objective (after-tax estate, or
  Monte Carlo success rate) is a smooth-ish monotone function of a small
  number of strategy parameters, evaluated by running the plan engine.
  This is numeric optimization, not relational/combinatorial search — the
  practical tools are the per-year bisection solve in `retirement.plan`
  plus enumeration of the meaningful strategy family here (which is what
  purpose-built retirement optimizers do, via LP/DP or exhaustive search).

  Scoring: after-tax real estate minus a large penalty per real dollar of
  spending shortfall, so any strategy that misses spending ranks below any
  that doesn't, and ties break toward the largest legacy (equivalently,
  least lifetime tax)."
  (:require [retirement.inputs :as inputs]
            [retirement.plan :as plan]
            [retirement.simulate :as simulate]
            [retirement.strategy :as strategy]))

(def ^:private shortfall-penalty 1000.0)

(defn- deterministic-score [inputs strat]
  (let [result (plan/run-plan (assoc inputs :strategy strat))
        estate (get-in result [:estate :after-tax-real])
        shortfall (get-in result [:summary :total-shortfall-real])
        legacy (double (or (get-in result [:goal :legacy]) 0.0))]
    {:strategy strat
     :description (strategy/describe strat)
     :score (- estate (* shortfall-penalty shortfall))
     :estate-real estate
     :total-tax-real (get-in result [:summary :total-tax-real])
     :total-shortfall-real shortfall
     :meets-goal? (and (get-in result [:summary :success?])
                       (>= estate legacy))
     :plan result}))

(defn- simulated-score [inputs strat {:keys [trials seed]}]
  (let [result (simulate/simulate (assoc inputs :strategy strat)
                                  {:trials trials :seed seed})
        deterministic (deterministic-score inputs strat)]
    (assoc deterministic
           :score (:success-rate result)
           :success-rate (:success-rate result)
           :simulation (dissoc result :yearly))))

(defn optimize
  "Search the strategy grid and return the best plan.

  opts:
    :metric      :estate (default — deterministic after-tax estate net of
                 shortfall penalties) or :success-rate (Monte Carlo)
    :candidates  override the strategy list (default `strategy/candidates`
                 plus any :strategy present in the inputs)
    :trials/:seed  for :success-rate scoring (defaults 400 / 1)

  Returns {:best {...} :ranking [...]} where each entry carries the
  strategy, its score and key outcome figures; :best includes the full
  year-by-year :plan."
  ([inputs] (optimize inputs {}))
  ([inputs {:keys [metric candidates trials seed]
            :or {metric :estate trials 400 seed 1}}]
   ;; Validate once up front so a bad input fails fast, not mid-search.
   (inputs/normalize inputs)
   (let [candidates (or candidates
                        (distinct (concat (when-let [s (:strategy inputs)] [s])
                                          (strategy/candidates))))
         score (case metric
                 :estate #(deterministic-score inputs %)
                 :success-rate #(simulated-score inputs % {:trials trials :seed seed})
                 (throw (ex-info ":metric must be :estate or :success-rate"
                                 {:metric metric})))
         scored (sort-by (comp - :score) (map score candidates))
         strip-plan #(dissoc % :plan)]
     {:metric metric
      :best (first scored)
      :ranking (mapv strip-plan scored)})))
