(ns retirement.simulate
  "Part 2: Monte Carlo simulation of drawdown plans.

  Samples correlated annual asset returns (lognormal growth factors, so a
  return can never fall below -100%) and normally-distributed inflation via
  a Cholesky factorization of the correlation matrix, runs the Part-1 plan
  engine over each sampled path, and reports the fraction of trials in
  which the plan succeeds (every year's spending met, and the legacy goal —
  if any — reached).

  Deterministic and stateless: results depend only on (inputs, opts).
  Each trial's random stream is derived from (:seed opts) and the trial
  index, so results are identical whether or not trials run in parallel.

  Practice notes from the literature (see README): ~1,000 trials is the
  industry norm (sampling error ~±2pp at 95% CI); success probability is
  magnitude-blind, so per-year balance percentiles and the median
  shortfall among failures are also reported."
  (:require [retirement.inputs :as inputs]
            [retirement.plan :as plan])
  (:import (java.util SplittableRandom)))

(def assets [:equity :bonds :cash :inflation])

;; ---------------------------------------------------------------------------
;; Linear algebra (4x4 is all we need — no dependency required)

(defn correlation-matrix
  "Build the full symmetric correlation matrix from the nested
  {:a {:b rho}} map in the assumptions."
  [correlations]
  (let [rho (fn [a b]
              (cond
                (= a b) 1.0
                :else (double (or (get-in correlations [a b])
                                  (get-in correlations [b a])
                                  0.0))))]
    (mapv (fn [a] (mapv (fn [b] (rho a b)) assets)) assets)))

(defn cholesky
  "Lower-triangular L with L·Lᵀ = m. Throws if m is not positive-definite."
  [m]
  (let [n (count m)]
    (reduce
     (fn [l i]
       (reduce
        (fn [l j]
          (let [s (reduce + 0.0 (map #(* (get-in l [i %]) (get-in l [j %]))
                                     (range j)))]
            (if (= i j)
              (let [d (- (get-in m [i i]) s)]
                (when (<= d 1.0e-12)
                  (throw (ex-info "correlation matrix is not positive-definite"
                                  {:matrix m})))
                (assoc-in l [i j] (Math/sqrt d)))
              (assoc-in l [i j] (/ (- (get-in m [i j]) s)
                                   (get-in l [j j]))))))
        l
        (range (inc i))))
     (vec (repeat n (vec (repeat n 0.0))))
     (range n))))

(defn- correlate [l z]
  (mapv (fn [row] (reduce + 0.0 (map * row z))) l))

;; ---------------------------------------------------------------------------
;; Return sampling

(defn- lognormal-params
  "Convert an arithmetic mean/vol of simple returns into the mu/sigma of
  the log growth factor."
  [{:keys [mean vol]}]
  (let [g (+ 1.0 mean)
        sigma2 (Math/log (+ 1.0 (Math/pow (/ vol g) 2)))]
    {:mu (- (Math/log g) (* 0.5 sigma2))
     :sigma (Math/sqrt sigma2)}))

(defn- sampler-params [assumptions]
  {:chol (cholesky (correlation-matrix (:correlations assumptions)))
   :lognormal (into {}
                    (map (fn [asset]
                           [asset (lognormal-params (get-in assumptions [:returns asset]))]))
                    [:equity :bonds :cash])
   :inflation (:inflation assumptions)})

(defn- sample-year
  [^SplittableRandom rng {:keys [chol lognormal inflation]}]
  (let [z (vec (repeatedly (count assets) #(.nextGaussian rng)))
        cz (correlate chol z)
        ret (fn [asset i]
              (let [{:keys [mu sigma]} (get lognormal asset)]
                (- (Math/exp (+ mu (* sigma (double (cz i))))) 1.0)))]
    {:equity (ret :equity 0)
     :bonds (ret :bonds 1)
     :cash (ret :cash 2)
     :inflation (+ (:mean inflation) (* (:vol inflation) (double (cz 3))))}))

(def ^:private golden-gamma (unchecked-long 0x9E3779B97F4A7C15))

(defn trial-rng
  "Independent, reproducible RNG for one trial."
  ^SplittableRandom [seed trial]
  (SplittableRandom.
   (bit-xor (unchecked-long seed)
            (unchecked-multiply (unchecked-long (inc trial)) golden-gamma))))

(defn trial-path
  "The sampled market path for one (seed, trial) pair — exposed so a
  caller can inspect exactly what a given trial experienced."
  [assumptions n-years seed trial]
  (let [params (sampler-params assumptions)
        rng (trial-rng seed trial)]
    (mapv (fn [_] (sample-year rng params)) (range n-years))))

;; ---------------------------------------------------------------------------
;; Aggregation

(defn- percentile [sorted-v q]
  (when (seq sorted-v)
    (nth sorted-v (long (Math/round (* q (dec (count sorted-v))))))))

(defn- percentiles [v]
  (let [s (vec (sort v))]
    {:p5 (percentile s 0.05) :p25 (percentile s 0.25) :p50 (percentile s 0.50)
     :p75 (percentile s 0.75) :p95 (percentile s 0.95)}))

(defn- trial-summary [result]
  {:success? (get-in result [:summary :success?])
   :estate-real (get-in result [:estate :after-tax-real])
   :shortfall-real (get-in result [:summary :total-shortfall-real])
   :first-shortfall-age (get-in result [:summary :first-shortfall-age])
   :balances-real (mapv #(/ (:total-balance-end %) (:index %))
                        (:years result))})

(defn simulate
  "Run `trials` Monte Carlo trials of the plan.

  opts: :trials (default 1000), :seed (default 1), :parallel? (default true).

  Returns
    {:trials n :seed s
     :success-rate 0.95
     :estate-real {:p5 .. :p25 .. :p50 .. :p75 .. :p95 ..}   ; after-tax, real
     :ruin {:probability .. :ages {...}}    ; first year spending was missed
     :shortfall {:probability .. :median-when-failed ..}     ; real dollars
     :yearly {:ages [..] :balance-real {:p5 [..] ...}}}      ; per-year bands"
  ([inputs] (simulate inputs {}))
  ([inputs {:keys [trials seed parallel?]
            :or {trials 1000 seed 1 parallel? true}}]
   (let [cfg (inputs/normalize inputs)
         n-years (:n-years cfg)
         params (sampler-params (:assumptions cfg))
         run (fn [t]
               (let [rng (trial-rng seed t)
                     path (mapv (fn [_] (sample-year rng params)) (range n-years))]
                 (trial-summary (plan/run-plan* cfg path))))
         results ((if parallel? (partial pmap) mapv) run (range trials))
         successes (count (filter :success? results))
         failures (remove :success? results)
         ruin-ages (keep :first-shortfall-age results)
         ages (vec (range (:start-age cfg) (+ (:start-age cfg) n-years)))
         by-year (apply mapv vector (map :balances-real results))
         yearly-pcts (mapv percentiles by-year)]
     {:trials trials
      :seed seed
      :success-rate (double (/ successes trials))
      :estate-real (percentiles (map :estate-real results))
      :ruin {:probability (double (/ (count ruin-ages) trials))
             :ages (when (seq ruin-ages) (percentiles ruin-ages))}
      :shortfall {:probability (double (/ (count failures) trials))
                  :median-when-failed (when (seq failures)
                                        (:p50 (percentiles (map :shortfall-real failures))))}
      :yearly {:ages ages
               :balance-real (into {}
                                   (map (fn [k] [k (mapv k yearly-pcts)]))
                                   [:p5 :p25 :p50 :p75 :p95])}})))

(defn sustainable-spending
  "Highest real annual spending that still succeeds in at least `target`
  of trials — 'you can spend $X/yr with 95% confidence'.

  opts: :target (default 0.95), :trials (default 500 — enough for a
  spending search; validate the result with a bigger simulate), :seed,
  :resolution (default $250)."
  ([inputs] (sustainable-spending inputs {}))
  ([inputs {:keys [target trials seed resolution]
            :or {target 0.95 trials 500 seed 1 resolution 250.0}}]
   (let [rate (fn [spending]
                (:success-rate (simulate (assoc-in inputs [:goal :annual-spending] spending)
                                         {:trials trials :seed seed})))
        ok? (fn [spending] (>= (rate spending) target))
        hi (loop [hi 40000.0]
             (if (or (> hi 5.0e7) (not (ok? hi))) hi (recur (* hi 2.0))))
        best (loop [lo 0.0 hi hi n 0]
               (if (or (> n 30) (< (- hi lo) resolution))
                 lo
                 (let [mid (* 0.5 (+ lo hi))]
                   (if (ok? mid)
                     (recur mid hi (inc n))
                     (recur lo mid (inc n))))))
        best (* resolution (Math/floor (/ best resolution)))]
     {:annual-spending best
      :success-rate (rate best)
      :target target
      :trials trials
      :seed seed})))
