(ns retirement.inputs
  "Input schema, defaults, validation and normalization.

  Full input map:

    {:person {:birth-year 1961          ; or :age (at start-year)
              :end-age 95               ; plan horizon (default 95)
              :province :on             ; :on | :ns | :bc | :ab (or plugged)
              :cpp {:start-age 70 :at-65 12000}   ; expected annual CPP at 65,
                                                  ; in start-year dollars
              :oas {:start-age 65 :fraction 1.0}  ; fraction = residency years/40
              :pensions [{:annual 20000 :start-age 65 :indexed? true}]
              :tfsa-room 0}
     :accounts [{:id :rrsp :type :rrsp :balance 400000
                 :holdings {:equity 0.6 :bonds 0.4}}
                {:id :tfsa :type :tfsa :balance 100000
                 :holdings {:equity 0.8 :bonds 0.2}}
                {:id :taxable :type :non-registered :balance 250000
                 :acb 175000 :holdings {:equity 0.7 :bonds 0.3}}]
     :goal {:type :spend-down            ; or :legacy
            :annual-spending 60000       ; real, after-tax, start-year dollars
            :legacy 0}                   ; real after-tax estate target
     :strategy {:type :sequential :order [:non-registered :registered :tfsa]}
     :start-year 2026
     :assumptions {...}                  ; deep-merged over default-assumptions
     :tax-tables {2025 {...} 2027 {...}}} ; optional per-year tax table
                                          ; patches/additions, deep-merged
                                          ; over the built-in EDN snapshots
                                          ; (see retirement.taxdata)

  All money amounts are in start-year (today's) dollars unless stated
  otherwise; the engine converts to nominal internally."
  (:require [retirement.taxdata :as data]))

(def default-assumptions
  "Long-term capital market assumptions, FP Canada 2025 Projection Assumption
  Guidelines flavour: equity blends Canadian (6.4%) and foreign developed
  (6.5%) equities; bonds 3.4%; cash/short-term 2.3%; inflation 2.1%.
  Volatilities and correlations are conventional planning values — the
  stock/bond correlation defaults to 0 (long-run average) and should be
  stressed positive for inflationary regimes."
  {:inflation {:mean 0.021 :vol 0.015}
   :returns {:equity {:mean 0.064 :vol 0.16}
             :bonds {:mean 0.034 :vol 0.07}
             :cash {:mean 0.023 :vol 0.01}}
   :correlations {:equity {:bonds 0.0 :cash 0.0 :inflation 0.0}
                  :bonds {:cash 0.3 :inflation -0.2}
                  :cash {:inflation 0.4}}
   :annual-fee 0.0                       ; MER/advice fee, subtracted from returns
   :distributions {:dividend-yield 0.02  ; eligible dividends on non-reg equity
                   :interest-yield 0.03} ; interest on non-reg bonds + cash
   :surplus-holdings {:equity 0.6 :bonds 0.4}})

(def default-holdings {:equity 0.6 :bonds 0.4})

(def default-strategy
  {:type :sequential :order [:non-registered :registered :tfsa]})

(def account-types #{:rrsp :rrif :tfsa :non-registered})

(def deep-merge data/deep-merge)

(defn- holdings-ok? [holdings]
  (and (map? holdings)
       (every? #{:equity :bonds :cash} (keys holdings))
       (every? #(and (number? %) (<= 0.0 (double %) 1.0)) (vals holdings))
       (< (abs (- 1.0 (reduce + 0.0 (map double (vals holdings))))) 0.001)))

(defn- validate-account [i {:keys [id type balance holdings acb]}]
  (cond-> []
    (nil? id)
    (conj (str "account " i ": missing :id"))
    (not (contains? account-types type))
    (conj (str "account " i ": :type must be one of " (sort account-types)
               ", got " (pr-str type)))
    (not (and (number? balance) (>= (double balance) 0.0)))
    (conj (str "account " i ": :balance must be a non-negative number"))
    (and (some? holdings) (not (holdings-ok? holdings)))
    (conj (str "account " i ": :holdings must map :equity/:bonds/:cash "
               "to weights in [0,1] summing to 1"))
    (and (some? acb) (not= type :non-registered))
    (conj (str "account " i ": :acb only applies to :non-registered accounts"))
    (and (some? acb) (not (and (number? acb) (>= (double acb) 0.0))))
    (conj (str "account " i ": :acb must be a non-negative number"))))

(defn validate
  "Returns a vector of human-readable problems; empty when valid."
  [{:keys [person accounts goal start-year tax-tables] :as inputs}]
  (let [{:keys [birth-year age end-age province cpp]} person
        {:keys [type annual-spending legacy]} goal
        start-year (or start-year 2026)
        ids (map :id accounts)
        tables-result (try {:tables (data/tables tax-tables)}
                           (catch Exception e {:error (ex-message e)}))
        known-provinces (when-let [ts (:tables tables-result)]
                          (set (keys (:provinces (data/resolve-table
                                                  ts
                                                  (if (integer? start-year)
                                                    start-year
                                                    2026))))))]
    (-> []
        (cond->
         (nil? person) (conj "missing :person")
         (nil? goal) (conj "missing :goal")
         (and person (nil? birth-year) (nil? age))
         (conj ":person needs :birth-year or :age")
         (:error tables-result)
         (conj (str ":tax-tables invalid: " (:error tables-result)))
         (and person province known-provinces
              (not (contains? known-provinces province)))
         (conj (str ":person :province must be one of "
                    (sort known-provinces) ", got " (pr-str province)))
         (and goal (not (contains? #{:spend-down :legacy nil} type)))
         (conj ":goal :type must be :spend-down or :legacy")
         (and goal (not (and (number? annual-spending)
                             (pos? (double (or annual-spending 0))))))
         (conj ":goal :annual-spending must be a positive number")
         (and (= type :legacy) (not (and (number? legacy) (pos? (double (or legacy 0))))))
         (conj ":goal of :type :legacy needs a positive :legacy amount")
         (and legacy (number? legacy) (neg? (double legacy)))
         (conj ":goal :legacy must be non-negative")
         (not (integer? start-year))
         (conj ":start-year must be an integer year")
         (and (integer? start-year) (< start-year data/anchor-year))
         (conj (str ":start-year must be >= " data/anchor-year
                    " (the earliest tax data year)"))
         (and person birth-year end-age start-year
              (<= (+ birth-year (or end-age 95)) start-year))
         (conj ":person :end-age is reached before :start-year")
         (not= (count ids) (count (distinct ids)))
         (conj "account :ids must be unique")
         (and (map? cpp) (:at-65 cpp) (not (number? (:at-65 cpp))))
         (conj ":person :cpp :at-65 must be a number"))
        (into (mapcat #(apply validate-account %) (map-indexed vector accounts))))))

(defn- normalize-account [account]
  (let [account (update account :holdings #(or % default-holdings))
        account (update account :balance double)]
    (if (= :non-registered (:type account))
      (update account :acb #(double (or % (:balance account))))
      (dissoc account :acb))))

(defn normalize
  "Validate and normalize inputs into an engine config. Throws ex-info with
  :errors on invalid input."
  [inputs]
  (let [problems (validate inputs)]
    (when (seq problems)
      (throw (ex-info (str "Invalid inputs: " (first problems)
                           (when (next problems)
                             (str " (+" (dec (count problems)) " more in :errors)")))
                      {:errors problems}))))
  (let [start-year (or (:start-year inputs) 2026)
        person (:person inputs)
        birth-year (or (:birth-year person)
                       (- start-year (:age person)))
        end-age (or (:end-age person) 95)
        assumptions (deep-merge default-assumptions (:assumptions inputs))
        start-age (- start-year birth-year)]
    {:person (-> person
                 (assoc :birth-year birth-year :end-age end-age)
                 (update :province #(or % :on))
                 (update :tfsa-room #(double (or % 0.0)))
                 (update :pensions #(or % [])))
     :accounts (mapv normalize-account (:accounts inputs))
     :goal (merge {:type :spend-down :legacy 0.0} (:goal inputs))
     :strategy (or (:strategy inputs) default-strategy)
     :assumptions assumptions
     :tables (data/tables (:tax-tables inputs))
     :start-year start-year
     :start-age start-age
     :n-years (inc (- end-age start-age))
     ;; CPI factor from the anchor year to the plan start year, using
     ;; assumed mean inflation for the intervening years.
     :initial-base-factor (Math/pow (+ 1.0 (get-in assumptions [:inflation :mean]))
                                    (- start-year data/anchor-year))}))
