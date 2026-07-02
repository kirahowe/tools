(ns retirement.plan
  "Part 1: the yearly drawdown plan.

  `run-plan` walks the retirement horizon one year at a time. Each year:

    1. Compute the nominal spending target and government benefits.
    2. Take forced RRIF minimums and any strategy pre-withdrawals
       (e.g. bracket-fill top-ups).
    3. Solve — by bisection, since tax, OAS clawback and GIS all depend on
       the withdrawal amounts — for the smallest discretionary withdrawal
       that meets the after-tax spending target.
    4. Reinvest any surplus cash (forced minimums beyond needs) into the
       TFSA while contribution room lasts, then into a non-registered
       account.
    5. Apply market growth.

  Everything is pure: the same inputs and market path always produce the
  same plan. Withdrawals happen at the start of the year; growth applies to
  what remains. Money is nominal inside the engine; rows carry :index
  (CPI vs the start year) so callers can deflate, and the summary reports
  key figures in real (start-year) dollars."
  (:require [retirement.accounts :as acct]
            [retirement.benefits :as benefits]
            [retirement.inputs :as inputs]
            [retirement.strategy :as strategy]
            [retirement.tax :as tax]
            [retirement.taxdata :as data]))

(def ^:private cash-tolerance 0.25)
(def ^:private shortfall-tolerance 1.0)

(defn deterministic-path
  "Market path where every year returns the assumed means."
  [assumptions n-years]
  (let [year {:equity (get-in assumptions [:returns :equity :mean])
              :bonds (get-in assumptions [:returns :bonds :mean])
              :cash (get-in assumptions [:returns :cash :mean])
              :inflation (get-in assumptions [:inflation :mean])}]
    (vec (repeat n-years year))))

(defn- sum-vals [m] (reduce + 0.0 (vals m)))

(defn- withdrawal-character
  "Tax character of a hypothetical withdrawal map, from start-of-year
  account facts (no account mutation)."
  [facts wd-map age]
  (reduce-kv
   (fn [acc id amt]
     (let [{:keys [class gain-frac pension?]} (get facts id)
           amt (double amt)]
       (-> acc
           (update :total + amt)
           (cond->
            (= class :registered) (update :ordinary + amt)
            (= class :non-registered) (update :capital-gains + (* amt gain-frac))
            (= class :tfsa) (update :tfsa + amt)
            (and pension? (>= age 65)) (update :pension + amt)))))
   {:total 0.0 :ordinary 0.0 :capital-gains 0.0 :tfsa 0.0 :pension 0.0}
   wd-map))

(defn- account-facts [accounts age]
  (into {}
        (map (fn [account]
               [(:id account)
                {:class (acct/account-class account)
                 :gain-frac (acct/gain-fraction account)
                 :pension? (acct/rrif? account age)}]))
        accounts))

(defn- evaluate-year
  "Compute taxes, GIS and net cash for a given withdrawal map."
  [{:keys [year age province base-factor cpp oas pension-cash
           pension-income-fixed ordinary-fixed dists facts]}
   wd-map]
  (let [character (withdrawal-character facts wd-map age)
        income {:ordinary (+ ordinary-fixed (:ordinary character))
                :eligible-dividends (:eligible-dividends dists)
                :capital-gains (:capital-gains character)
                :age age
                :pension-income (+ pension-income-fixed (:pension character))}
        tax-detail (tax/income-tax {:year year :factor base-factor
                                    :province province :income income
                                    :oas-received oas})
        gis (benefits/gis-annual (pos? oas)
                                 (- (:net-income tax-detail) oas)
                                 base-factor)
        net-cash (+ cpp oas gis pension-cash
                    (:total character)
                    (:eligible-dividends dists) (:interest dists)
                    (- (:total tax-detail)))]
    {:tax tax-detail
     :gis gis
     :character character
     :net-cash net-cash}))

(defn- solve-discretionary
  "Find the smallest discretionary withdrawal whose net cash meets the
  target. Monotone in the withdrawal amount, so bisection converges."
  [year-ctx strategy-ctx strat fixed-wd target]
  (let [f (fn [d]
            (let [wd (if (pos? d)
                       (merge-with + fixed-wd (strategy/allocate strat strategy-ctx d))
                       fixed-wd)]
              (assoc (evaluate-year year-ctx wd) :withdrawals wd)))
        at-zero (f 0.0)
        d-max (sum-vals (:available strategy-ctx))]
    (cond
      (>= (:net-cash at-zero) target) at-zero
      (< (:net-cash (f d-max)) target) (f d-max)
      :else
      (loop [lo 0.0
             hi d-max
             best (f d-max)
             n 0]
        (if (or (> n 64) (< (- hi lo) cash-tolerance))
          best
          (let [mid (* 0.5 (+ lo hi))
                r (f mid)]
            (if (>= (:net-cash r) target)
              (recur lo mid r (inc n))
              (recur mid hi best (inc n)))))))))

(defn- apply-withdrawals [accounts wd-map age]
  (mapv (fn [account]
          (let [amt (get wd-map (:id account) 0.0)]
            (if (pos? (double amt))
              (:account (acct/withdraw account amt age))
              ;; Still convert RRSP->RRIF at 71 even with no withdrawal.
              (if (and (= :rrsp (:type account)) (>= age 71))
                (assoc account :type :rrif)
                account))))
        accounts))

(defn- reinvest-surplus
  "Put surplus cash into the TFSA (room permitting) then non-registered.
  Creates a non-registered overflow account if none exists."
  [accounts surplus tfsa-room surplus-holdings]
  ;; Sub-dollar surpluses are bisection rounding, not real cash flows —
  ;; reinvesting them would create dust accounts that pollute later plans.
  (if (< surplus 1.0)
    {:accounts accounts :tfsa 0.0 :non-registered 0.0 :tfsa-room tfsa-room}
    (let [tfsa-id (some #(when (= :tfsa (:type %)) (:id %)) accounts)
          tfsa-amt (if tfsa-id (min surplus tfsa-room) 0.0)
          rest-amt (- surplus tfsa-amt)
          nonreg-id (some #(when (= :non-registered (:type %)) (:id %)) accounts)
          accounts (cond-> accounts
                     (and (pos? rest-amt) (nil? nonreg-id))
                     (conj {:id :reinvested :type :non-registered
                            :balance 0.0 :acb 0.0 :holdings surplus-holdings}))
          nonreg-id (or nonreg-id (when (pos? rest-amt) :reinvested))
          contribute-to (fn [accts id amt]
                          (mapv #(if (= (:id %) id) (acct/contribute % amt) %)
                                accts))]
      {:accounts (cond-> accounts
                   (pos? tfsa-amt) (contribute-to tfsa-id tfsa-amt)
                   (pos? rest-amt) (contribute-to nonreg-id rest-amt))
       :tfsa tfsa-amt
       :non-registered rest-amt
       :tfsa-room (- tfsa-room tfsa-amt)})))

(defn- pension-income-annual
  "DB/employer pension income for the year (nominal)."
  [pensions age year-index]
  (reduce + 0.0
          (map (fn [{:keys [annual start-age indexed?]
                     :or {start-age 65 indexed? true}}]
                 (if (>= age start-age)
                   (* (double annual) (if indexed? year-index 1.0))
                   0.0))
               pensions)))

(defn- simulate-year
  [{:keys [person goal assumptions strategy initial-base-factor start-year] :as _cfg}
   {:keys [accounts year-index tfsa-room tfsa-withdrawn-last] :as _state}
   year
   returns]
  (let [age (- year (:birth-year person))
        base-factor (* initial-base-factor year-index)
        spend-target (* (:annual-spending goal) year-index)
        cpp (benefits/cpp-annual (:cpp person) age year-index)
        oas (benefits/oas-annual (:oas person) age base-factor)
        pension (pension-income-annual (:pensions person) age year-index)
        ;; TFSA room accrues each year after the first, plus last year's
        ;; withdrawals are restored.
        tfsa-room (+ tfsa-room
                     (if (> year start-year)
                       (+ (* (get-in data/benefits [:tfsa :annual-limit]) base-factor)
                          tfsa-withdrawn-last)
                       0.0))
        facts (account-facts accounts age)
        forced (into {}
                     (keep (fn [account]
                             (let [m (acct/rrif-minimum account age)]
                               (when (pos? m) [(:id account) m]))))
                     accounts)
        dists (reduce (fn [acc account]
                        (merge-with + acc (acct/distribution-yields
                                           account (:distributions assumptions))))
                      {:eligible-dividends 0.0 :interest 0.0}
                      accounts)
        fed (data/federal-for-year year)
        available-after (fn [wd]
                          (into {}
                                (map (fn [{:keys [id balance]}]
                                       [id (max 0.0 (- balance (get wd id 0.0)))]))
                                accounts))
        base-ctx {:accounts accounts :age age :year year :fed fed
                  :base-factor base-factor :year-index year-index
                  :ordinary-baseline (+ cpp oas pension (:interest dists)
                                        (sum-vals forced))}
        pre (strategy/pre-withdrawals strategy
                                      (assoc base-ctx :available (available-after forced)))
        fixed-wd (merge-with + forced pre)
        year-ctx {:year year :age age :province (:province person)
                  :base-factor base-factor :cpp cpp :oas oas
                  :ordinary-fixed (+ cpp oas pension (:interest dists))
                  :pension-cash pension
                  :pension-income-fixed pension
                  :dists dists :facts facts}
        strategy-ctx (assoc base-ctx :available (available-after fixed-wd))
        solved (solve-discretionary year-ctx strategy-ctx strategy fixed-wd spend-target)
        wd-map (:withdrawals solved)
        net-cash (:net-cash solved)
        shortfall (max 0.0 (- spend-target net-cash))
        surplus (max 0.0 (- net-cash spend-target))
        oas-net (- oas (get-in solved [:tax :oas-clawback]))
        accounts (apply-withdrawals accounts wd-map age)
        reinvested (reinvest-surplus accounts surplus tfsa-room
                                     (:surplus-holdings assumptions))
        fee (:annual-fee assumptions)
        net-returns (-> returns
                        (update :equity - fee)
                        (update :bonds - fee)
                        (update :cash - fee))
        grown (mapv #(acct/grow % net-returns (:distributions assumptions))
                    (:accounts reinvested))
        row {:year year
             :age age
             :index year-index
             :spending-target spend-target
             :withdrawals wd-map
             :rrif-minimums forced
             :benefits {:cpp cpp :oas oas :oas-net oas-net :gis (:gis solved)}
             :distributions dists
             :tax (:tax solved)
             :net-cash net-cash
             :shortfall shortfall
             :surplus-reinvested (select-keys reinvested [:tfsa :non-registered])
             :balances-end (into {} (map (juxt :id :balance)) grown)
             :total-balance-end (acct/total-balance grown)}]
    {:row row
     :state {:accounts grown
             :year-index (* year-index (+ 1.0 (:inflation returns)))
             :tfsa-room (:tfsa-room reinvested)
             :tfsa-withdrawn-last (get-in solved [:character :tfsa])}}))

(defn- estate-value
  "After-tax estate at the horizon: registered balances are fully taxable
  as income on the final return, unrealized non-registered gains are
  deemed disposed, TFSA passes tax-free. Modeled as a standalone final
  return (not stacked on that year's other income)."
  [accounts age year base-factor year-index province]
  (let [registered (reduce + 0.0 (map :balance (filter acct/registered? accounts)))
        gains (acct/unrealized-gains accounts)
        gross (acct/total-balance accounts)
        tax-detail (tax/income-tax
                    {:year year :factor base-factor :province province
                     :income {:ordinary registered
                              :capital-gains gains
                              :age age
                              :pension-income (if (>= age 65) registered 0.0)}})
        after-tax (- gross (:total tax-detail))]
    {:gross gross
     :terminal-tax (:total tax-detail)
     :after-tax after-tax
     :after-tax-real (/ after-tax year-index)}))

(defn run-plan*
  "Run a normalized config against an explicit market path (one map of
  {:equity :bonds :cash :inflation} per plan year). Used directly by the
  Monte Carlo engine; most callers want `run-plan`."
  [{:keys [person goal start-year n-years initial-base-factor] :as cfg} market-path]
  (when (< (count market-path) n-years)
    (throw (ex-info "market path shorter than the plan horizon"
                    {:path-years (count market-path) :n-years n-years})))
  (let [years (range start-year (+ start-year n-years))
        init {:accounts (:accounts cfg)
              :year-index 1.0
              :tfsa-room (get-in cfg [:person :tfsa-room])
              :tfsa-withdrawn-last 0.0}
        {:keys [rows state]}
        (reduce (fn [{:keys [rows state]} [year returns]]
                  (let [{:keys [row state]} (simulate-year cfg state year returns)]
                    {:rows (conj rows row) :state state}))
                {:rows [] :state init}
                (map vector years market-path))
        end-year (+ start-year n-years -1)
        end-age (- end-year (:birth-year person))
        ;; state's year-index has advanced through the final year's
        ;; inflation, so it deflates the end-of-final-year estate.
        estate-index (:year-index state)
        estate (estate-value (:accounts state) end-age end-year
                             (* initial-base-factor estate-index)
                             estate-index
                             (:province person))
        total-shortfall-real (reduce + 0.0 (map #(/ (:shortfall %) (:index %)) rows))
        legacy (double (or (:legacy goal) 0.0))
        success? (and (< total-shortfall-real shortfall-tolerance)
                      (>= (:after-tax-real estate) legacy))]
    {:years rows
     :estate estate
     :summary {:success? success?
               :total-shortfall-real total-shortfall-real
               :total-tax-real (reduce + 0.0 (map #(/ (get-in % [:tax :total])
                                                      (:index %))
                                                  rows))
               :total-withdrawn-real (reduce + 0.0
                                             (map #(/ (sum-vals (:withdrawals %))
                                                      (:index %))
                                                  rows))
               :first-shortfall-age (some #(when (> (:shortfall %) shortfall-tolerance)
                                             (:age %))
                                          rows)}
     :strategy (:strategy cfg)
     :goal goal}))

(defn run-plan
  "Part 1 entry point: validate inputs and produce the year-by-year
  drawdown plan using expected (deterministic) returns.

  Returns {:years [...] :estate {...} :summary {...}}; each year row says
  how much to withdraw from which account, the benefits received, taxes
  paid, and end-of-year balances."
  ([inputs]
   (let [cfg (inputs/normalize inputs)]
     (run-plan* cfg (deterministic-path (:assumptions cfg) (:n-years cfg)))))
  ([inputs market-path]
   (run-plan* (inputs/normalize inputs) market-path)))

(defn solve-max-spending
  "Highest sustainable real annual spending under expected returns (the
  deterministic analogue of a safe withdrawal amount). Respects the goal's
  :legacy floor if present. Returns dollars per year in start-year dollars."
  [inputs]
  (let [cfg (inputs/normalize inputs)
        path (deterministic-path (:assumptions cfg) (:n-years cfg))
        ok? (fn [spending]
              (get-in (run-plan* (assoc-in cfg [:goal :annual-spending] spending) path)
                      [:summary :success?]))
        hi (loop [hi 50000.0]
             (if (or (> hi 5.0e7) (not (ok? hi))) hi (recur (* hi 2.0))))]
    (loop [lo 0.0 hi hi n 0]
      (if (or (> n 40) (< (- hi lo) 10.0))
        (Math/floor lo)
        (let [mid (* 0.5 (+ lo hi))]
          (if (ok? mid)
            (recur mid hi (inc n))
            (recur lo mid (inc n))))))))
