(ns demo
  "Runnable tour of the library. From retirement-optimizer/:

    clojure -M -e '(load-file \"examples/demo.clj\")'

  or with plain java, add examples/ is not needed — just load this file
  from a REPL with src/ on the classpath."
  (:require [retirement.core :as r]))

(def inputs
  {:person {:age 65
            :province :on
            :cpp {:start-age 70 :at-65 12000}   ; deferring CPP to 70
            :oas {:start-age 65}
            :tfsa-room 10000}
   :accounts [{:id :rrsp :type :rrsp :balance 500000
               :holdings {:equity 0.6 :bonds 0.4}}
              {:id :tfsa :type :tfsa :balance 120000
               :holdings {:equity 0.8 :bonds 0.2}}
              {:id :taxable :type :non-registered :balance 250000
               :acb 180000 :holdings {:equity 0.7 :bonds 0.3}}]
   :goal {:type :spend-down :annual-spending 55000}
   :start-year 2026})

;; ---------------------------------------------------------------------------
;; Part 1 — a year-by-year drawdown plan

(let [{:keys [years estate summary]} (r/plan inputs)]
  (println "== Drawdown plan (conventional strategy, expected returns) ==")
  (println (format "%-5s %-4s %-12s %-34s %-10s %-10s"
                   "year" "age" "spend" "withdrawals" "tax" "balance"))
  (doseq [row (take 10 years)]
    (println (format "%-5d %-4d %-12.0f %-34s %-10.0f %-10.0f"
                     (:year row) (:age row) (:spending-target row)
                     (pr-str (update-vals (:withdrawals row) #(Math/round (double %))))
                     (get-in row [:tax :total])
                     (:total-balance-end row))))
  (println "...")
  (println "estate (real, after tax):" (Math/round (:after-tax-real estate)))
  (println "lifetime tax (real):     " (Math/round (:total-tax-real summary)))
  (println))

;; ---------------------------------------------------------------------------
;; Which withdrawal strategy is best for this household?

(let [{:keys [best ranking]} (r/optimize inputs)]
  (println "== Strategy ranking (score = real after-tax estate, shortfalls penalized) ==")
  (doseq [entry ranking]
    (println (format "  %-45s estate %10.0f  lifetime tax %10.0f"
                     (:description entry)
                     (:estate-real entry)
                     (:total-tax-real entry))))
  (println "best:" (:description best))
  (println))

;; ---------------------------------------------------------------------------
;; Part 2 — Monte Carlo: how confident can we be?

(let [result (r/simulate inputs {:trials 1000 :seed 42})]
  (println "== Monte Carlo (1000 trials) ==")
  (println (format "This plan succeeds %.0f%% of the time."
                   (* 100 (:success-rate result))))
  (println "estate percentiles (real, after tax):"
           (update-vals (:estate-real result) #(Math/round (double %))))
  (when (get-in result [:ruin :ages])
    (println "when it fails, money runs out around age"
             (get-in result [:ruin :ages :p50])))
  (println))

;; And the headline number: what CAN they spend at 95% confidence?
(let [result (r/sustainable-spending inputs {:target 0.95 :trials 500 :seed 42})]
  (println (format "At 95%% confidence this household can spend $%.0f/yr (real, after tax)."
                   (:annual-spending result))))
