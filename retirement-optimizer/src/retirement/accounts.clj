(ns retirement.accounts
  "Account mechanics: classes, RRIF minimums, withdrawals with ACB tracking,
  growth, and taxable distributions from non-registered accounts.

  Account map:
    :id        keyword or string, unique
    :type      :rrsp | :rrif | :tfsa | :non-registered
    :balance   current market value
    :acb       adjusted cost base (non-registered only; defaults to balance)
    :holdings  asset mix, e.g. {:equity 0.6 :bonds 0.35 :cash 0.05}

  Conventions: withdrawals happen at the start of the year, growth applies
  to what remains. RRSPs convert to RRIFs in the year the holder turns 71;
  mandatory minimums then start the following year (age 72). Accounts that
  are already :rrif have minimums at any age."
  (:require [retirement.taxdata :as data]))

(def account-classes
  "Strategy-level account classes, in the conventional withdrawal order."
  [:non-registered :registered :tfsa])

(defn account-class
  [{:keys [type]}]
  (case type
    (:rrsp :rrif) :registered
    :tfsa :tfsa
    :non-registered :non-registered))

(defn registered?
  [account]
  (= :registered (account-class account)))

(defn rrif?
  "Is this account a RRIF (or an RRSP past the mandatory conversion age)?"
  [{:keys [type]} age]
  (or (= type :rrif)
      (and (= type :rrsp) (>= age 71))))

(defn rrif-minimum
  "Mandatory minimum withdrawal for this account at `age` (start-of-year
  balance). RRSPs converted at 71 owe their first minimum at 72; accounts
  already held as RRIFs owe one at any age."
  [{:keys [type balance] :as account} age]
  (cond
    (not (rrif? account age)) 0.0
    (and (= type :rrsp) (< age 72)) 0.0
    :else (min (double balance)
               (* balance (data/rrif-minimum-factor age)))))

(defn gain-fraction
  "Fraction of each non-registered withdrawal that is realized capital gain."
  [{:keys [type balance acb]}]
  (if (and (= type :non-registered) (pos? (double balance)))
    (max 0.0 (/ (- balance (double (or acb balance))) balance))
    0.0))

(defn withdraw
  "Withdraw `amount` (clamped to balance). Returns
  {:account updated :amount actual :realized-gain g :taxable-ordinary o}
  where the tax character depends on the account type."
  [{:keys [type balance acb] :as account} amount age]
  (let [amt (max 0.0 (min (double amount) (double balance)))
        gain-frac (gain-fraction account)
        realized (* amt gain-frac)
        acb' (when (= type :non-registered)
               (let [b (double balance)
                     a (double (or acb balance))]
                 (if (pos? b) (* a (- 1.0 (/ amt b))) a)))
        account' (cond-> (assoc account :balance (- balance amt))
                   (= type :non-registered) (assoc :acb acb')
                   (and (= type :rrsp) (>= age 71)) (assoc :type :rrif))]
    {:account account'
     :amount amt
     :realized-gain (if (= type :non-registered) realized 0.0)
     :taxable-ordinary (if (registered? account) amt 0.0)}))

(defn portfolio-return
  "Weighted return of a holdings mix given per-asset returns."
  [holdings returns]
  (reduce-kv (fn [acc asset weight]
               (+ acc (* weight (double (get returns asset 0.0)))))
             0.0
             holdings))

(defn distribution-yields
  "Annual taxable cash distributions thrown off by a non-registered account:
  eligible dividends on the equity portion, interest on bonds + cash.
  Returns {:eligible-dividends d :interest i} on the start-of-year balance."
  [{:keys [type balance holdings]} {:keys [dividend-yield interest-yield]
                                    :or {dividend-yield 0.02 interest-yield 0.03}}]
  (if (not= type :non-registered)
    {:eligible-dividends 0.0 :interest 0.0}
    (let [eq (get holdings :equity 0.0)
          fixed (+ (get holdings :bonds 0.0) (get holdings :cash 0.0))]
      {:eligible-dividends (* balance eq dividend-yield)
       :interest (* balance fixed interest-yield)})))

(defn grow
  "Apply one year of market growth. For non-registered accounts the cash
  distributions (already paid out) are subtracted from the total return so
  only the price change compounds; ACB is unchanged (gains stay unrealized)."
  [{:keys [type balance holdings] :as account} returns dist-opts]
  (let [r (portfolio-return holdings returns)
        dist (when (= type :non-registered)
               (let [{:keys [eligible-dividends interest]}
                     (distribution-yields account dist-opts)]
                 (if (pos? (double balance))
                   (/ (+ eligible-dividends interest) balance)
                   0.0)))
        growth (if dist (- r dist) r)]
    (assoc account :balance (max 0.0 (* balance (+ 1.0 growth))))))

(defn contribute
  "Add `amount` to an account (surplus reinvestment). Non-registered
  contributions raise the ACB."
  [{:keys [type] :as account} amount]
  (cond-> (update account :balance + amount)
    (= type :non-registered) (update :acb (fnil + 0.0) amount)))

(defn total-balance
  [accounts]
  (reduce + 0.0 (map :balance accounts)))

(defn unrealized-gains
  [accounts]
  (reduce + 0.0
          (map (fn [{:keys [type balance acb]}]
                 (if (= type :non-registered)
                   (max 0.0 (- balance (double (or acb balance))))
                   0.0))
               accounts)))
