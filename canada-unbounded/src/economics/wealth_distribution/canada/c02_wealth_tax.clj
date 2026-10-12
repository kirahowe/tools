^:kindly/hide-code
(ns economics.wealth-distribution.canada.c02-wealth-tax
  {:kindly/options {:static true
                    :kinds-that-hide-code #{:kind/var :kind/hiccup2 :kind/fn}}
   :clay {:title  "Unbounded North: Wealth Tax"
          :quarto {:sidebar     "unbounded-north"
                   :author      "Kira Howe"
                   :description "How Canadian wealth tax proposals work, who would pay, and how much they would raise."
                   :abstract    "A 1–3% tax on family wealth above $10M would touch about 0.6% of Canadian families and raise roughly $23B a year, by the PBO's conservative estimate."
                   :date        "2026-10-12"
                   :type        :post
                   :category    :economics
                   :tags        [:tax :economics :growth :canada]}}}
  (:require [scicloj.plotje.api :as pj]
            [tablecloth.api :as tc]
            [economics.wealth-distribution.canada.common :as c]))

;; ## What Is a Wealth Tax?

;; A wealth tax is an annual tax on net worth (everything you own minus everything you owe) above a high threshold.
;; With a \$10M threshold, a family worth \$12M pays tax only on the \$2M above it.
;; It reaches what income tax misses: the growth of large fortunes that is not sold, and so goes untaxed for decades, until death,
;; as we saw in [Who Pays Tax](c01_who_pays_tax.html).

;; Canada has seriously costed several wealth taxes.
;; The Parliamentary Budget Officer (PBO), Parliament's independent fiscal watchdog,
;; has estimated the revenue from each of them.

;; | Proposal | Design | PBO revenue estimate | Families affected |
;; | :--- | :--- | :--- | :--- |
;; | Requested by NDP MP Peter Julian (2020)[^1] | 1% above \$20M | \$5.6B in 2020–21 | 13,800 |
;; | Motion M-68, Liberal MP Nathaniel Erskine-Smith (2021)[^2] | One-time 3% above \$10M, 5% above \$20M, paid over 5 years | \$44.0B–\$60.7B over 5 years | — |
;; | NDP platform (2021)[^3] | 1% above \$10M | about \$60B over 5 years | about 78,000 |
;; | NDP and Green platforms (2025)[^4] | 1% on \$10–50M, 2% on \$50–100M, 3% above \$100M | **\$22.8B in 2025–26; \$121.5B over 5 years** | 108,000 |
;;
;; : Canadian wealth tax proposals costed by the Parliamentary Budget Officer {#tbl-wealth-tax-designs}

;; The current NDP leader, Avi Lewis, elected in March 2026, campaigned on the same 1/2/3% schedule,
;; plus an inheritance tax on estates over \$5M.[^5]

;; Every PBO costing is deliberately cautious.
;; The PBO assumes **35%** of each wealthy family's net worth is hidden or avoided and drops it from the tax base,
;; then subtracts 2% for administration.[^4]
;; Cutting each family's *total* net worth by 35% roughly halves the revenue on the PBO's own public file.
;; A review of the international evidence suggests a well-designed 1% wealth tax would shrink its base by 7–17%,[^18]
;; so the PBO's estimate is probably conservative, even allowing for the higher 2% and 3% rates (see [The Investment Question](c09_investment_question.html)).

;; ## Who Would Pay?

;; Using the PBO's own public high-net-worth database, we can see who sits above each threshold.[^6]

(def total-families 16852000)

(def hfd
  (c/data "pbo_hfd_2023_public.csv"))

(defn families-above [x]
  (->> (map vector (:FNETWTH hfd) (:FWEIGHT hfd))
       (filter (fn [[nw _]] (> nw x)))
       (map second)
       (reduce +)))

(defn wealth-above [x]
  (->> (map vector (:FNETWTH hfd) (:FWEIGHT hfd))
       (filter (fn [[nw _]] (> nw x)))
       (map (fn [[nw wt]] (* nw wt)))
       (reduce +)))

^:kind/table
(tc/dataset
 (for [[x label] [[10e6 "$10M"] [50e6 "$50M"] [100e6 "$100M"] [1e9 "$1B"]]]
   (let [n (families-above x)]
     {"Net worth above"     label
      "Families"            (format "%,d" (Math/round (double n)))
      "One family in"       (format "%,d" (Math/round (double (/ total-families n))))
      "Share of all wealth" (c/pct-text (* 100.0 (/ (wealth-above x) 16341e9)))})))

;; About 108,000 families (0.6% of all Canadian families) have more than \$10M.
;; Together they hold about a fifth of all the wealth in the country.
;; **If your family is worth less than \$10M, you would not pay this tax.**
;; The median Canadian family is worth about \$520,000.[^19]
;; For the median family, a \$10M threshold is about twenty times what they own today.

;; The schedule is gentle at the threshold and rises with the size of the fortune.

(defn ndp-2025-tax
  "Annual tax under the 2025 NDP/Green schedule, before any avoidance."
  [nw]
  (+ (* 0.01 (max 0 (- (min nw 50e6) 10e6)))
     (* 0.02 (max 0 (- (min nw 100e6) 50e6)))
     (* 0.03 (max 0 (- nw 100e6)))))

(def schedule
  (let [xs (map #(Math/pow 10 %) (range 6.8 10.51 0.02))]
    (tc/dataset {:net-worth-m (map #(/ % 1e6) xs)
                 :effective   (map #(* 100.0 (/ (ndp-2025-tax %) %)) xs)})))

^{:kindly/options {:id :fig-effective-rate
                   :caption "Wealth tax owed as a share of total net worth under the 2025 NDP/Green schedule (1% on $10–50M, 2% on $50–100M, 3% above $100M). Log scale."}}
(-> schedule
    (pj/lay-line :net-worth-m :effective {:color c/o :size 2})
    (pj/scale :x :log)
    (pj/lay-text {:data (tc/dataset {:net-worth-m [20 100 1000 10000]
                                     :effective   (map #(* 100.0 (/ (ndp-2025-tax (* % 1e6)) (* % 1e6))) [20 100 1000 10000])
                                     :t           (map #(format "%s: %s a year" (c/money (* % 1e6)) (c/money (ndp-2025-tax (* % 1e6)))) [20 100 1000 10000])})
                  :x :net-worth-m :y :effective :text :t :align-x :right :offset-x -6 :offset-y -10 :overlay true})
    (pj/options {:title   "How Much of a Fortune Would Go to Tax Each Year?"
                 :x-label "Family net worth (C$ millions, log scale)"
                 :y-label "Tax as % of net worth"}))

;; A \$20M family would pay \$100,000 a year, 0.5% of its wealth.
;; A \$1B family would pay about \$28M, 2.8%.
;; Large fortunes have recently grown faster than that.
;; The combined wealth of Forbes billionaires living in Canada went from US\$110B in April 2020 (near the pandemic market low) to US\$202B in 2026,
;; partly because new billionaires joined the list (see [Wealth Concentration](c05_wealth_concentration.html)).
;; A 3% tax slows the snowball; it doesn't melt it.

;; ### Replicating the PBO Estimate

;; We can roughly reproduce the PBO's revenue estimate from its public file:
;; shrink every family's wealth by 35% for avoidance, apply the schedule, and subtract 2% for administration.

(def mechanical-revenue
  (->> (map vector (:FNETWTH hfd) (:FWEIGHT hfd))
       (map (fn [[nw wt]] (* wt (ndp-2025-tax (* 0.65 nw)))))
       (reduce +)
       (* 0.98)))

^:kind/table
(tc/dataset
 [{"Estimate" "Mechanical, from PBO public 2023 file (this page)"
   "Annual revenue" (c/money mechanical-revenue)}
  {"Estimate" "PBO costing for 2025–26 (wealth projected forward from 2023)"
   "Annual revenue" "$22.8B"}])

;; The mechanical figure for 2023 wealth, \$21.1B, is within about 8% of the PBO's 2025–26 estimate,
;; and the family count above \$10M (108,035) matches the PBO's published 108,000.
;; That is expected: the PBO projects wealth forward to 2025 and uses its full model rather than the
;; smaller public file.
;; Either way, a tax touching 0.6% of families would raise about \$20B a year.
;; That is about **4% of all federal revenue**, and more than a third of what Ottawa pays in interest on its debt.

;; ## Public Support

(def polls
  (-> (c/data "polling_wealth_tax_canada.csv")
      (tc/select-rows #(and (= "Abacus Data" (:pollster %))
                            (re-matches #"\d+" (str (:support_conservative_pct %)))))
      (tc/update-columns {:support_conservative_pct (partial map #(parse-long (str %)))
                          :support_all_pct          (partial map #(parse-long (str %)))})))

^{:kindly/options {:id :fig-polling
                   :caption "Support for a wealth tax in Abacus Data polls, all respondents and Conservative voters. 2019: 2% above $50M (commissioned by North 99). 2020: 1% above $20M (Abacus national survey). The dashed line marks majority support.[^7]"}}
(-> (c/grouped-bars
     (for [r (sort-by :published (tc/rows polls :as-maps))
           :let [poll ({"2019" "2019: 2% above $50M" "2020" "2020: 1% above $20M"} (subs (str (:published r)) 0 4))]
           [g v] [["Conservative voters" (:support_conservative_pct r)] ["All Canadians" (:support_all_pct r)]]]
       {:x poll :group g :value v})
     {:colors [c/wd c/o] :title "Support for a Wealth Tax" :y-label "% who support" :y-max 100})
    (pj/lay-rule-h {:y-intercept 50 :stroke-dash :dashed}))

;; In 2020, Abacus Data found **79%** of Canadians supported a 1% tax on wealth over \$20M,
;; including **64% of Conservative voters** and 73% of people in the highest income bracket.[^7]
;; A 2021 Abacus poll for the Broadbent Institute found 88% wanted a wealth tax in the pandemic recovery plan (54% called it a must-have).
;; I could not find a published 2025–26 poll from a major firm.
;; The question has dropped out of the national conversation; whether opinion has moved with it is unknown.

;; Support is broad, but specific measures framed as hitting "investors" fare worse.
;; When the 2024 capital gains increase was in the news, the Angus Reid Institute found only about a third of Canadians supported it.[^8]
;; A broad tax on the very rich polls far better than a specific change that many voters expect could reach them.

;; ## Why Does It Matter?

;; In [the original series](https://clojurecivitas.org/economics/wealth_distribution/unbounded/u2_wealth_tax.html),
;; the urgent case was American public debt: 20% of US federal revenue goes to interest.
;; Canada's position is different, and better, but it is heading the wrong way.

(def fiscal
  (c/data "fed_fiscal_history_frt2025.csv"))

(def seu-2026
  "Spring Economic Update 2026, Table A1.7: public debt charges and revenues ($B)."
  (tc/dataset {:fiscal_year ["2025-26" "2026-27" "2027-28" "2028-29" "2029-30" "2030-31"]
               :charges     [54.0 58.7 65.7 71.6 75.7 80.9]
               :revenues    [511.5 529.6 546.8 565.9 589.8 613.7]}))

(def debt-charges
  (tc/concat
   (-> fiscal
       (tc/add-column :year #(map (fn [fy] (parse-long (subs fy 0 4))) (:fiscal_year %)))
       (tc/rename-columns {:debt_charges_pct_revenue_calc :pct})
       (tc/add-column :series "Actual")
       (tc/select-columns [:year :pct :series]))
   (-> seu-2026
       (tc/add-column :year #(map (fn [fy] (parse-long (subs fy 0 4))) (:fiscal_year %)))
       (tc/add-column :pct #(map (fn [a b] (* 100.0 (/ a b))) (:charges %) (:revenues %)))
       (tc/add-column :series "Projected (Spring Economic Update 2026)")
       (tc/select-columns [:year :pct :series]))))

^{:kindly/options {:id :fig-debt-charges
                   :caption "Federal public debt charges as a share of federal revenue, 1966–67 to 2024–25, and the government's projection to 2030–31. Fiscal years are labelled by their starting year.[^9]"}}
(-> debt-charges
    (pj/lay-line :year :pct {:color :series})
    (pj/scale :color {:values [c/wd c/red]})
    (pj/scale :y {:include 0})
    (pj/options {:title "Share of Federal Revenue Spent on Interest"
                 :x-label "Fiscal year" :y-label "Debt charges, % of revenue"
                 :color-label "" :legend-position :bottom}))

;; In 1990–91, interest ate **37.6%** of federal revenue.
;; The austerity of the 1990s brought it down, and by 2021–22 it was just 5.9%.
;; Since then rates have risen and deficits returned.
;; Debt charges were \$53.4B in 2024–25, 10.5% of revenue, and are projected to reach \$80.9B (13.2%) by 2030–31.[^9]
;; **Of every federal tax dollar you pay, more than a dime goes to bondholders**, and the share is rising.

;; A wealth tax raising \$20–25B a year would cover about a third or more of today's interest payments.
;; It wouldn't replace responsible budgeting.
;; But it would mean the adjustment doesn't fall entirely on people who work for a living.

;; ### Canada's Hidden Fiscal Strength

;; One genuinely Canadian difference is the public balance sheet.
;; Canada's *gross* government debt, counting all levels of government together (113.5% of GDP in 2025), is a little below the G7 average of 123%.
;; Its *net* debt is the lowest in the G7 by far: 10.2% of GDP.[^10]

(def g7
  (-> (c/data "imf_g7_debt_fm2026apr.csv")
      (tc/select-rows #(= 2025 (:year %)))
      (tc/update-columns {:country (partial map #(get {"G7" "G7 average" "United Kingdom" "UK" "United States" "US"} % %))})
      (tc/pivot->longer [:gg_gross_debt_pct_gdp :gg_net_debt_pct_gdp]
                        {:target-columns :measure :value-column-name :pct})
      (tc/update-columns {:measure (partial map {:gg_gross_debt_pct_gdp "Gross debt" :gg_net_debt_pct_gdp "Net debt"})})))

^{:kindly/options {:id :fig-g7-debt
                   :caption "Gross and net debt of all levels of government combined, % of GDP, 2025 (IMF Fiscal Monitor, April 2026). Canada's net debt counts the assets of the Canada and Quebec Pension Plans, which are earmarked for future pensions.[^10]"}}
(c/dumbbell
 (sort-by :from (c/pair-rows g7 :country :measure :pct "Net debt" "Gross debt"))
 {:from-name "Net debt (after subtracting financial assets)" :to-name "Gross debt"
  :from-color c/red :to-color c/neutral :include-zero true
  :title "G7 Government Debt, 2025" :x-label "% of GDP" :fmt #(format "%.0f" (double %))})

;; The Canada and Quebec Pension Plans are a big part of why net debt is so low.
;; Their funds hold about \$850B in net financial assets (about 27% of GDP), which offset about 59% of federal and provincial net liabilities.[^11]
;; The rest of the gap is other government financial assets, such as equity holdings, loans and receivables.
;; Even without the pension plans, Canada's net debt would still be the lowest in the G7.
;; That is a real strength: Canadians collectively own a large, well-managed public investment fund.
;; But those assets are earmarked for future pensions, so "lowest net debt in the G7" flatters the federal books.

;; ## Objections, Canadian Edition

;; ### "Just Fix the Loopholes"

;; Closing loopholes is worthwhile; Canada's biggest "loophole" is the statute itself.
;; Half-taxing capital gains costs Ottawa an estimated \$30–40B a year, and that is the law working as written
;; (see [Who Pays Tax](c01_who_pays_tax.html)).
;; Raising the inclusion rate, as Budget 2024 proposed, would help.
;; But a capital gains tax only bites when assets are *sold*.
;; Only a tax on the stock of wealth can slow fortunes that are never sold.

;; ### "They'll All Leave"

;; Canada already has an exit tax.
;; Under section 128.1 of the *Income Tax Act*, a person who stops being a Canadian resident is
;; "deemed to have disposed" of their property at fair market value, and owes tax on the accrued gains.[^12]
;; A wealth tax can be paired with a parallel exit charge.
;; Evidence from Norway, the most-cited case of millionaires leaving, finds that at most about 100 people left
;; because of its 2021–24 wealth tax increases, and revenue still rose (see [The Investment Question](c09_investment_question.html)).

;; Canada's emigration is also small: about 112,000 people in 2025, roughly 0.3% of the population,
;; with about half that number returning each year.[^13]
;; And the assets a wealth tax targets (Canadian companies, Canadian land, Canadian buildings) cannot leave.

;; ### "It Will Kill Investment"

;; This is the most common objection in Canada, and it gets its own part:
;; [The Investment Question](c09_investment_question.html).
;; The short answer: Canada cut capital taxes deeply between 2000 and 2012, and investment and productivity fell further behind anyway.

;; ### "It's Unconstitutional / a Provincial Matter"

;; It isn't.
;; Section 91(3) of the *Constitution Act, 1867* gives Ottawa "the raising of Money by any Mode or System of Taxation."
;; The provinces can also levy direct taxes, which a wealth tax is.[^14]
;; A federal tax is better: the Spanish and Swiss experience shows sub-national wealth taxes leak to the lowest-tax region.

;; ### "Farmers and Small Business Owners Will Be Hit"

;; Not at a \$10M threshold.
;; The lifetime capital gains exemption already shelters up to \$1.275M (2026) of gains on qualifying small businesses, farms and fishing property.[^15]
;; A family farm or small business worth less than \$10M in net worth would owe nothing.
;; For illiquid fortunes above the threshold, the OECD recommends instalment or deferral options, and they should be part of any design.

;; ::: {.callout-note}
;; ## A Closer Look: Canada Has Done This Before

;; Canada is not new to taxing concentrated wealth.
;; It levied federal death taxes from 1941 to 1971, and provincial succession duties from the 1890s until 1985.[^16]
;; More recently, Parliament passed:

;; * a **luxury tax** on cars, boats and aircraft (2022), since scaled back to cars only;
;; * the **Canada Recovery Dividend**, a one-time 15% tax on the largest banks' and insurers' profits above \$1B,
;;   plus a permanent 1.5-point surtax on their taxable income above \$100M (2022);
;; * a **global minimum tax** of 15% on large multinationals (2024).[^17]

;; :::

;; ## Conclusion

;; A Canadian wealth tax on fortunes above \$10M would touch fewer than 1 family in 150.
;; By the PBO's cautious estimate it would raise over \$20B a year, enough to cover a third of federal interest costs.
;; Canada already taxes gains at death and on emigration, and has taxed concentrated wealth before.
;; Most objections are design questions with known answers.
;; The rest of this series looks at why it matters beyond the revenue.

;; In the next part, [Labour Hours](c03_labour_hours.html), we look at what Canadians' work buys them today.

;; [^1]: Office of the Parliamentary Budget Officer, [Net Wealth Tax on Canadian Resident Economic Families](https://www.pbo-dpb.ca/en/publications/RP-2021-017-M--net-wealth-tax-canadian-resident-economic-families--impot-patrimoine-net-familles-economiques-residant-au-canada), July 8, 2020.
;; [^2]: Office of the Parliamentary Budget Officer, [Revenue Estimates for M-68: One-Time Tax on Extreme Wealth](https://www.pbo-dpb.ca/en/publications/RP-2122-012-M--revenue-estimates-m-68-one-time-tax-extreme-wealth--estimation-recettes-mesures-prevues-dans-motion-m-68-impot-unique-richesse-extreme), July 15, 2021.
;; [^3]: The PBO's 2021 election costing of the NDP's 1% tax above \$10M (about \$60.1B over 2021–22 to 2025–26) is reported in [Advisor.ca, September 2021](https://www.advisor.ca/tax/ndps-proposed-wealth-tax-would-generate-60-billion-over-five-years-pbo/); the family count above \$10M is from the PBO's [December 2021 top-tail update](https://www.pbo-dpb.ca/en/publications/RP-2122-023-M--estimating-top-tail-family-wealth-distribution-in-canada-updates-trends--estimation-extremite-superieure-distribution-patrimoine-familial-canada-mises-jour-tendances), Table 2-3.
;; [^4]: Office of the Parliamentary Budget Officer, [Implementing an Annual Wealth Tax](https://distribution-a617274656661637473.pbo-dpb.ca/cc1b029d8d36c1340f715cb7666bfd2132ef6683608cecae42a5e71b82bb6ad6), election proposal costing, April 9, 2025: \$22.790B (2025–26) rising to \$26.848B (2029–30).
;; [^5]: Avi Lewis leadership campaign, [Tax policy: full plan](https://lewisisleader.ca/ideas/tax-policy-full-plan/). Its revenue claims are the campaign's own and have not been independently costed.
;; [^6]: Office of the Parliamentary Budget Officer, [Estimating the Top Tail of the Family Wealth Distribution in Canada: 2025 Update](https://www.pbo-dpb.ca/en/publications/RP-2526-009-S--estimating-top-tail-family-wealth-distribution-in-canada-2025-update--estimation-extremite-superieure-distribution-patrimoine-familial-canada-mises-jour-2025), public High-net-worth Family Database file (2023). Counts and shares computed on this page from the public file, using the PBO's totals of 16.852 million families and \$16,341B.
;; [^7]: Abacus Data, ["Wealth tax: Canadians support it"](https://abacusdata.ca/wealth-tax-canada-poll/), November 2020 (n=1,660); Abacus Data for the Broadbent Institute and PIPSC, ["Canadians want a wealth tax"](https://abacusdata.ca/canadians-want-a-wealth-tax/), August 2021; Abacus Data for North 99, May 2019, as reported by [Ricochet](https://ricochet.media/justice/poll-over-two-thirds-of-canadians-back-a-wealth-tax/).
;; [^8]: Angus Reid Institute, [Capital gains inclusion rate](https://angusreid.org/federal-politics-capital-gains-inclusion/), June 2024.
;; [^9]: Department of Finance Canada, [Fiscal Reference Tables 2025](https://www.canada.ca/en/department-finance/services/publications/fiscal-reference-tables/2025/part-1.html), Tables 1–2, and [Spring Economic Update 2026](https://budget.canada.ca/update-miseajour/2026/report-rapport/anx1-en.html), Table A1.7. The PBO's [assessment of the update](https://www.pbo-dpb.ca/en/publications/NT-2627-002-S--pbo-assessment-spring-economic-update-fiscal-anchors-fiscal-sustainability--evaluation-dpb-mise-jour-economique-printemps-cibles-budgetaires-viabilite-financiere) gives the same path. Data before 1983–84 are not on a full accrual basis.
;; [^10]: IMF, [Fiscal Monitor, April 2026, Statistical Appendix](https://www.imf.org/-/media/files/publications/fiscal-monitor/2026/april/english/msa.pdf), Tables A7–A8.
;; [^11]: Department of Finance Canada, [Fiscal Reference Tables 2025, Part 8](https://www.canada.ca/en/department-finance/services/publications/fiscal-reference-tables/2025/part-8.html), Tables 47–50 (calendar 2024).
;; [^12]: *Income Tax Act*, [section 128.1](https://laws-lois.justice.gc.ca/eng/acts/I-3.3/section-128.1.html). Some property, such as Canadian real estate and pensions, is excluded from the deemed disposition.
;; [^13]: Statistics Canada, [Table 17-10-0040-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1710004001), estimates of emigrants and returning emigrants.
;; [^14]: [Constitution Act, 1867](https://laws-lois.justice.gc.ca/eng/const/page-3.html), ss. 91(3) and 92(2).
;; [^15]: [Prime Minister's statement of March 21, 2025](https://www.pm.gc.ca/en/news/news-releases/2025/03/21/prime-minister-mark-carney-cancels-proposed-capital-gains-tax-increase), confirming the \$1.25M lifetime capital gains exemption; indexed to \$1,275,000 for 2026 ([CRA indexation table](https://www.canada.ca/en/revenue-agency/services/tax/individuals/frequently-asked-questions-individuals/adjustment-personal-income-tax-benefit-amounts.html)).
;; [^16]: Vaillancourt and Kerkhoff, ["Capital gains taxation in Canada, 1972–2017"](https://www.unsw.edu.au/content/dam/pdfs/business/acct-audit-tax/research-reports/past-issues/bus-2019/2019-volume-16-number-2/BUS-2019-V16-paper1-v16-n2.pdf), *eJournal of Tax Research* 16(2), 2019.
;; [^17]: LEGISinfo: [Bill C-19 (2022)](https://www.parl.ca/legisinfo/en/bill/44-1/C-19), *Select Luxury Items Tax Act*; [Bill C-32 (2022)](https://www.parl.ca/legisinfo/en/bill/44-1/C-32), Canada Recovery Dividend and additional tax on banks and life insurers; [Bill C-69 (2024)](https://www.parl.ca/legisinfo/en/bill/44-1/C-69), *Global Minimum Tax Act*; [Bill C-15 (2026)](https://www.parl.ca/legisinfo/en/bill/45-1/C-15), which removed aircraft and vessels from the luxury tax.
;; [^18]: Arun Advani and Hannah Tarrant, ["Behavioural responses to a wealth tax"](https://doi.org/10.1111/1475-5890.12283), *Fiscal Studies* 42(3–4), 2021. The 7–17% range is for a 1% tax.
;; [^19]: Statistics Canada, Survey of Financial Security 2023, [Table 11-10-0016-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1110001601), median net worth of families (\$519,700).
