(ns retirement.strategy
  "Withdrawal strategies: given a year's context and a cash amount, decide
  which accounts to draw from.

  A strategy is plain data with a :type key:

    {:type :sequential :order [:non-registered :registered :tfsa]}
      Drain account classes in order (the conventional wisdom default).

    {:type :proportional}
      Draw from all accounts pro-rata to their available balances.

    {:type :bracket-fill :ceiling :first-bracket-top
     :order [:non-registered :tfsa :registered]}
      Each year, withdraw from RRSP/RRIF up to a taxable-income ceiling
      even beyond spending needs (surplus is re-sheltered in the TFSA),
      then meet any remaining cash need from :order. This is the
      'RRSP meltdown' / tax-smoothing family. :ceiling is
      :first-bracket-top, :second-bracket-top, :oas-clawback, or a number
      in start-year dollars.

  Two extension points, both pure:
    (pre-withdrawals strategy ctx)   income-driven withdrawals taken
                                     regardless of cash need
    (allocate strategy ctx amount)   split a discretionary cash amount
                                     across accounts

  ctx (built by retirement.plan):
    :accounts           account vector (start-of-year, input order)
    :available          {account-id available-balance} net of forced/pre
    :age :year          ints
    :fed                federal tax table for the year
    :base-factor        CPI factor vs the tax-data base year
    :year-index         CPI factor vs the plan start year
    :ordinary-baseline  taxable ordinary income already locked in
                        (CPP + OAS + pensions + interest + forced minimums)"
  (:require [retirement.accounts :as acct]))

(defn- take-in-order
  "Withdraw up to `amount` walking account classes in `class-order`,
  accounts within a class in input order. Returns {account-id amount}."
  [accounts available class-order amount]
  (loop [result {}
         remaining (double amount)
         ids (for [class class-order
                   account accounts
                   :when (= class (acct/account-class account))]
               (:id account))]
    (if (or (empty? ids) (< remaining 0.005))
      result
      (let [id (first ids)
            take-amt (min remaining (double (get available id 0.0)))]
        (recur (if (> take-amt 0.005) (assoc result id take-amt) result)
               (- remaining take-amt)
               (rest ids))))))

;; ---------------------------------------------------------------------------

(defmulti pre-withdrawals
  "Income-driven withdrawals taken regardless of the year's cash need."
  (fn [strategy _ctx] (:type strategy)))

(defmethod pre-withdrawals :default [_ _] {})

(defn resolve-ceiling
  "Resolve a :bracket-fill ceiling to nominal dollars for the year."
  [ceiling {:keys [fed base-factor year-index]}]
  (cond
    (number? ceiling) (* (double ceiling) year-index)
    (= ceiling :first-bracket-top) (* (get-in fed [:brackets 0 :up-to]) base-factor)
    (= ceiling :second-bracket-top) (* (get-in fed [:brackets 1 :up-to]) base-factor)
    (= ceiling :oas-clawback) (* (get-in fed [:oas-clawback :threshold]) base-factor)
    :else (throw (ex-info (str "Unknown bracket-fill ceiling: " (pr-str ceiling))
                          {:ceiling ceiling}))))

(defmethod pre-withdrawals :bracket-fill
  [{:keys [ceiling] :or {ceiling :first-bracket-top}}
   {:keys [accounts available ordinary-baseline] :as ctx}]
  (let [target (resolve-ceiling ceiling ctx)
        room (max 0.0 (- target (double ordinary-baseline)))]
    (take-in-order accounts available [:registered] room)))

;; ---------------------------------------------------------------------------

(defmulti allocate
  "Split a discretionary cash withdrawal across accounts. Must return
  {account-id amount} with the total equal to (min amount sum-available)."
  (fn [strategy _ctx _amount] (:type strategy)))

(defmethod allocate :sequential
  [{:keys [order] :or {order [:non-registered :registered :tfsa]}}
   {:keys [accounts available]} amount]
  (take-in-order accounts available order amount))

(defmethod allocate :bracket-fill
  ;; The bracket already pulled registered money via pre-withdrawals; cash
  ;; needs beyond it come from taxable then TFSA, registered as last resort.
  [{:keys [order] :or {order [:non-registered :tfsa :registered]}}
   {:keys [accounts available]} amount]
  (take-in-order accounts available order amount))

(defmethod allocate :proportional
  [_ {:keys [available]} amount]
  (let [total (reduce + 0.0 (vals available))]
    (if (< total 0.005)
      {}
      (let [amt (min (double amount) total)]
        (into {}
              (keep (fn [[id avail]]
                      (when (pos? (double avail))
                        [id (* amt (/ (double avail) total))])))
              available)))))

;; ---------------------------------------------------------------------------

(def conventional
  "Taxable first, registered second, TFSA last — the standard rule of thumb."
  {:type :sequential :order [:non-registered :registered :tfsa]})

(def sequential-orders
  (vec
   (for [a [:non-registered :registered :tfsa]
         b [:non-registered :registered :tfsa]
         :when (not= a b)
         :let [c (first (remove #{a b} [:non-registered :registered :tfsa]))]]
     [a b c])))

(defn candidates
  "Strategy candidates for the optimizer's grid search: every sequential
  ordering, proportional, and bracket-fill at each interesting ceiling."
  []
  (concat
   (map (fn [order] {:type :sequential :order order}) sequential-orders)
   [{:type :proportional}]
   (map (fn [ceiling] {:type :bracket-fill :ceiling ceiling})
        [:first-bracket-top :second-bracket-top :oas-clawback])))

(defn describe
  [{:keys [type order ceiling]}]
  (case type
    :sequential (str "sequential " (mapv name order))
    :proportional "proportional to balances"
    :bracket-fill (str "bracket-fill to "
                       (if (number? ceiling) (str "$" (long ceiling)) (name ceiling)))
    (str type)))
