^:kindly/hide-code
(ns economics.wealth-distribution.canada.c01-who-pays-tax
  {:kindly/options {:static true
                    :kinds-that-hide-code #{:kind/var :kind/hiccup2 :kind/fn}}
   :clay {:title  "Unbounded North: Who Pays Tax?"
          :quarto {:sidebar     "unbounded-north"
                   :author      "Kira Howe"
                   :description "Who pays tax in Canada, and why a dollar of wages is taxed more than a dollar of asset growth."
                   :abstract    "Canada's tax system leans on work. Asset growth is half-taxed when realized, untaxed until then, and untaxed entirely on a principal residence."
                   :date        "2026-10-11"
                   :type        :post
                   :category    :economics
                   :tags        [:tax :economics :growth :canada]}}}
  (:require [clojure.string :as str]
            [scicloj.plotje.api :as pj]
            [tablecloth.api :as tc]
            [economics.wealth-distribution.canada.common :as c]))

;; ## Tax Revenues

;; In fiscal year 2024–25 the Government of Canada collected \$511B in revenue.[^1]
;; Personal income tax alone brought in \$234B, nearly half of it.
;; Employment Insurance premiums, paid on wages, added another \$31.5B.
;; Corporate income tax was \$97B, about one dollar in five.
;; The GST and other taxes on what we buy brought in most of the rest.

(def revenue
  (-> (c/data "federal_revenue_by_source.csv")
      (tc/select-rows #(not= "Total revenues" (:source %)))
      (tc/add-columns
       {:base (fn [ds]
                (map {"Personal income tax"           "Mostly work"
                      "Employment Insurance premiums" "Mostly work"
                      "Corporate income tax"          "Mostly ownership"
                      "Non-resident income tax"       "Mostly ownership"
                      "Goods and Services Tax"        "Spending"
                      "Energy taxes"                  "Spending"
                      "Customs import duties"         "Spending"
                      "Other excise taxes and duties" "Spending"
                      "Pollution pricing proceeds"    "Spending"
                      "Other revenues"                "Other"}
                     (:source ds)))
        :revenue-b (fn [ds] (map #(/ % 1000.0) (:fy2024_25_final_millions ds)))
        :revenue-text (fn [ds] (map #(c/money (* 1e6 %)) (:fy2024_25_final_millions ds)))})
      (tc/add-column :c #(map {"Mostly work" c/w "Mostly ownership" c/o
                               "Spending" c/neutral "Other" "#E4E0D8"} (:base %)))
      (tc/order-by [:fy2024_25_final_millions])))

^{:kindly/options {:id :fig-federal-revenue
                   :caption "Federal revenue, 2024–25. Personal income tax and EI premiums (blue) are mostly paid on wages and salaries. Corporate and non-resident taxes (orange) are levied on corporate profits and on investment income paid to non-residents. CPP contributions ($82B in 2023–24) are not federal revenue and are excluded.[^1]"}}
(-> revenue
    (pj/lay-bar :revenue-b :source {:color {:column :c :scale false}})
    (pj/lay-label {:text :revenue-text})
    (pj/options {:title   "Federal Revenue, 2024–25"
                 :x-label "Revenue (C$ billions)"
                 :y-label ""
                 :legend-position :none}))

;; Canada leans harder on personal income tax than most rich countries.
;; Across all levels of government, the OECD finds that taxes on personal income were 35.9% of all
;; Canadian tax revenue in 2023, "substantially higher" than the OECD average.[^2]
;; Personal income tax, social security contributions (including CPP/QPP), and payroll taxes together
;; make up about 53% of all taxes collected in Canada.
;; Corporate income tax is 13.8%.

;; Personal income tax isn't only a tax on wages.
;; Capital gains, dividends, and interest are personal income too.
;; So who reports the capital gains?

;; ## Who Reports Capital Gains?

;; For the 2023 tax year, Canadians reported \$55B of *taxable* capital gains.[^3]
;; Only half of a capital gain is taxable, so that is about \$110B of actual gains.
;; Filers with incomes over \$250,000 filed 1.8% of all tax returns.
;; They reported **71%** of all taxable capital gains.
;; Because the income classes include the gains themselves, some of these filers are there only because of a one-time sale, such as a business or a cottage.

(def income-band
  (fn [cls]
    (cond
      (= cls "Total") nil
      (str/starts-with? cls "250000") "$250K+"
      (str/starts-with? cls "150000") "$150K–250K"
      (str/starts-with? cls "100000") "$100K–150K"
      :else "Under $100K")))

(def capital-gains-by-band
  (let [ds    (c/data "cra_t1_2023_by_income_class.csv")
        rows  (tc/rows ds :as-maps)
        sum   (fn [item k]
                (->> rows
                     (filter #(= item (:item %)))
                     (keep (fn [r] (when-let [b (income-band (:income_class r))]
                                     [b (k r)])))
                     (reduce (fn [m [b v]] (update m b (fnil + 0) v)) {})))
        n     (sum "Total number of returns" :number)
        g     (sum "Taxable capital gains" :amount_thousands)
        tot-n (reduce + (vals n))
        tot-g (reduce + (vals g))
        bands ["Under $100K" "$100K–150K" "$150K–250K" "$250K+"]]
    (tc/dataset
     (for [b bands
           [measure v] [["Share of tax returns" (* 100.0 (/ (n b) tot-n))]
                        ["Share of taxable capital gains" (* 100.0 (/ (g b) tot-g))]]]
       {:band b :measure measure :share v :label (c/pct-text v)}))))

^{:kindly/options {:id :fig-capital-gains-by-income
                   :caption "Tax year 2023. Filers with total income over $250,000 filed fewer than 2 in 100 returns but reported 71% of taxable capital gains.[^3]"}}
(c/grouped-bars
 (for [r (tc/rows capital-gains-by-band :as-maps)]
   {:x (:band r) :group (:measure r) :value (:share r)})
 {:colors [c/w c/o]
  :title "Who Reports Capital Gains, by Income of Filer"
  :y-label "% of Canada-wide total"})

;; Capital gains are not a middle-class income source.
;; Most Canadians' savings sit in RRSPs, TFSAs, pension plans, and their homes,
;; and none of those produce taxable capital gains.
;; Taxable capital gains come overwhelmingly from people who own businesses, investment portfolios, and real estate beyond their own home.

;; ## Half-Taxed Income

;; Since October 2000 only **50%** of a capital gain has been included in taxable income.[^4]
;; An Ontario employee in the top bracket who earns an extra dollar of salary pays 53.5¢ of it in tax.
;; An investor realizing an extra dollar of capital gain pays 26.8¢.
;; If the investor doesn't sell, they pay nothing at all that year.
;; If the gain is on their principal residence, they never pay tax on it.

(def ontario-top-rate
  (-> (c/data "top_marginal_rates_2026.csv")
      (tc/select-rows #(= "Ontario" (:jurisdiction %)))
      :combined_top_rate
      first))

(def dollar-of-income
  (tc/dataset
   {:kind ["Gain on principal residence"
           "Unrealized capital gain"
           "Realized capital gain"
           "Salary or wages"]
    :rate [0.0
           0.0
           (/ ontario-top-rate 2.0)
           ontario-top-rate]
    :c    [c/o c/o c/o c/w]}))

^{:kindly/options {:id :fig-dollar-of-income
                   :caption "Top marginal tax rate on an extra dollar of income for an Ontario resident in 2026, by how the dollar was earned. Computed from the 2026 federal and Ontario rates (including the Ontario surtax) and the 50% capital gains inclusion rate.[^4]"}}
(-> dollar-of-income
    (tc/add-column :label #(map (fn [r] (format "%.1f¢" r)) (:rate %)))
    (pj/lay-bar :rate :kind {:color {:column :c :scale false}})
    (pj/lay-label {:text :label})
    (pj/scale :x {:domain [0 60]})
    (pj/options {:title   "Tax on One More Dollar, Ontario, 2026"
                 :x-label "Cents of tax per dollar"
                 :y-label ""
                 :legend-position :none}))

;; The 2024 federal budget tried to narrow the gap.
;; It proposed raising the inclusion rate to two-thirds on capital gains above \$250,000 a year
;; for individuals, and on all capital gains realized by corporations and trusts.
;; The Parliamentary Budget Officer estimated it would raise \$17.4B over five years.[^5]
;; Finance Canada estimated it would affect 0.13% of Canadians in any given year.[^6]
;; It was cancelled on March 21, 2025, before it took effect.[^4]

;; ## Counting Capital Gains Changes the Picture

;; Statistics Canada publishes the incomes and taxes of the highest-income tax filers in two ways:
;; with capital gains left out of "income", and with them included.[^7]
;; When gains are left out, the tax system looks steeply progressive at the very top.
;; Count the gains as income, which they are, and the top rates sag.

(def top-filers-2023
  (-> (c/data "high_income_filers_11100055_extract.csv" {:key-fn identity})
      (tc/select-rows (fn [r]
                        (and (= 2023 (r "REF_DATE"))
                             (#{"Total income" "Total income with capital gains"} (r "Income concepts"))
                             (#{"Average income" "Average federal and provincial or territorial income taxes paid"}
                              (r "Statistics")))))))

(def effective-rates
  (let [rows   (tc/rows top-filers-2023 :as-maps)
        lookup (into {} (map (fn [r] [[(r "Income concepts") (r "Income groups") (r "Statistics")] (r "VALUE")]) rows))
        groups [["Top 10 percent income group" "Top 10%"]
                ["Top 1 percent income group" "Top 1%"]
                ["Top 0.1 percent income group" "Top 0.1%"]
                ["Top 0.01 percent income group" "Top 0.01%"]]]
    (tc/dataset
     (for [[g label] groups
           [concept measure] [["Total income" "Capital gains left out"]
                              ["Total income with capital gains" "Capital gains counted"]]
           :let [inc (lookup [concept g "Average income"])
                 tax (lookup [concept g "Average federal and provincial or territorial income taxes paid"])]
           :when (and inc tax)]
       {:group label :measure measure :rate (* 100.0 (/ tax inc))}))))

^{:kindly/options {:id :fig-effective-rates
                   :caption "Average federal and provincial income tax paid as a share of average income, 2023. Each group is ranked on the income concept shown. The top 0.01% pay 41% of income in tax when capital gains are ignored, but 32% once their gains are counted as income.[^7]"}}
(c/dumbbell
 (c/pair-rows effective-rates :group :measure :rate "Capital gains left out" "Capital gains counted")
 {:from-name "Capital gains left out" :to-name "Capital gains counted"
  :from-color c/w :to-color c/o :include-zero true
  :title "Average Income Tax Rate of Top Earners, 2023"
  :x-label "Income tax as % of income" :fmt #(format "%.0f%%" (double %))})

;; The 2023 rows tell the story:

;; * Ranked without gains, the top 0.01% (3,110 people) had average income of \$7.7M and paid 41.4% of it in income tax.
;; * Ranked with gains, the top 0.01% (a partly different 3,110 people) averaged \$10.8M and paid 31.6%.
;; * Under the same measure, the top 1% go from 38.9% to 31.3%.

;; These figures leave a lot out.
;; They count only personal income tax, not the corporate tax paid before profits reach shareholders, which would push the rate up.
;; In the other direction, they capture only *realized* gains.
;; Wealth that grows without being sold does not show up at all.
;; Neither does income retained inside private holding corporations,
;; where it is taxed at the corporate rate and can be left to compound for years.
;; Researchers using linked Canadian tax data found that "top income shares are significantly higher"
;; once income kept inside private corporations is attributed to the people who own them.[^8]
;; Finance Canada found that taxable passive investment income held inside private corporations
;; more than tripled, from \$8.6B in 2002 to \$26.8B in 2015.[^17]

;; ## What We Can and Can't Measure

;; The original American series showed taxes paid as a share of *wealth*, by wealth group,
;; using an IRS study that linked tax returns to estimated wealth.[^9]
;; **No equivalent public study exists for Canada.**
;; Statistics Canada's Distributions of Household Economic Accounts report wealth by wealth quintile,
;; but taxes only by *income* quintile, and only bundled together with other transfers.[^10]
;; The Parliamentary Budget Officer maintains a database of high-net-worth families for costing wealth taxes,
;; but does not publish taxes paid by wealth group.[^11]

;; This gap is itself worth noticing.
;; Canada collects excellent data on what workers earn.
;; We collect much less on what owners own.
;; That makes it much harder to see whether the very wealthy are paying their share.

;; ## The Price of Preferences

;; Finance Canada does publish the cost of the tax breaks that favour asset growth.
;; Each year's *Report on Federal Tax Expenditures* estimates how much revenue Ottawa gives up
;; through each special rule.[^12]

(def expenditures
  (let [ds (c/data "tax_expenditures_2026_report.csv")
        pick {["Partial inclusion of capital gains" "Total"]                          "Half-taxed capital gains"
              ["Non-taxation of capital gains on principal residences" "PIT"]         "Principal residence exemption"
              ["Dividend gross-up and tax credit" "PIT"]                              "Dividend tax credit"
              ["Tax-Free Savings Account" "PIT"]                                      "Tax-Free Savings Account"
              ["Deduction of interest and carrying charges incurred to earn investment income" "PIT"] "Investment interest deduction"
              ["Lifetime Capital Gains Exemption" "PIT"]                              "Lifetime capital gains exemption"}]
    (-> ds
        (tc/map-columns :label [:measure :tax] (fn [m t] (pick [m t])))
        (tc/drop-missing :label)
        (tc/add-columns {:cost-b (fn [ds] (map #(/ % 1000.0) (ds :2023)))
                         :cost-text (fn [ds] (map #(c/money (* 1e6 %)) (ds :2023)))})
        (tc/order-by [:cost-b]))))

^{:kindly/options {:id :fig-tax-expenditures
                   :caption "Estimated federal revenue forgone in 2023 through selected tax measures on capital and savings. Finance Canada classes the dividend tax credit and the investment interest deduction as structural rather than preferences: the dividend credit offsets corporate tax already paid. Provincial revenue forgone is additional.[^12]"}}
(-> expenditures
    (pj/lay-bar :cost-b :label {:color c/o})
    (pj/lay-label {:text :cost-text})
    (pj/options {:title   "Federal Revenue Forgone on Capital, 2023"
                 :x-label "C$ billions"
                 :y-label ""}))

;; The partial inclusion of capital gains alone cost Ottawa an estimated \$29.5B in 2023 (before any change in behaviour),
;; and is projected at \$34–40B a year from 2025 to 2027, after a one-off \$41.7B in 2024 as investors sold ahead of the proposed increase.
;; That is more than the \$26.3B Ottawa paid out in children's benefits in 2023–24.[^13]
;; The principal residence exemption costs another \$7.6B,
;; and that is a break Canadians feel very directly through the price of housing.

;; ## Uniquely Canadian: No Inheritance Tax, but Tax at Death

;; Canada has had no estate or inheritance tax since 1972,
;; when Ottawa abolished its estate and gift taxes as part of the reform that first taxed capital gains.
;; The provinces followed: Quebec, the last to give up its succession duties, did so in 1985.[^14]
;; (Provinces still charge probate fees, such as Ontario's Estate Administration Tax, but these are small and are not inheritance taxes.)

;; Instead, Canada taxes accrued gains at death.
;; When someone dies, they are "deemed" to have sold everything at fair market value,
;; and the capital gain is taxed on their final return, unless the property passes to a spouse.[^15]
;; This is an important difference from the United States.
;; There, heirs receive a "stepped-up basis" and the gain escapes income tax for good, although very large estates face a federal estate tax that Canada doesn't have.
;; In Canada, "buy, borrow, die" *defers* tax, sometimes for a lifetime, but does not erase it.

;; The deferral is still worth a great deal.
;; A fortune that compounds untaxed for 40 years and is taxed once at half-rate at the end
;; is taxed far more lightly than a salary taxed every two weeks.
;; And the deemed disposition can itself be deferred for another generation,
;; through a spousal rollover, through family trusts (for up to 21 years), or through an "estate freeze" that moves future growth to children.

;; ::: {.callout-note}
;; ## A Closer Look: "A Buck Is a Buck"

;; Canada once came close to taxing all income alike.
;; In 1962 the Diefenbaker government appointed a Royal Commission on Taxation, chaired by Toronto
;; accountant Kenneth Carter.
;; Its 1966 report argued that every increase in a person's economic power should be taxed the same way,
;; whether it came from wages, dividends, gifts, inheritances, or gains on assets.
;; The idea is usually summed up in a phrase attributed to Carter himself: *"a buck is a buck."*[^16]

;; The Commission recommended taxing capital gains, gifts and inheritances like any other income,
;; because they "increase the economic power" of the people who receive them.
;; The 1971 tax reform that followed taxed only half of capital gains, from 1972.
;; It also abolished the federal estate and gift taxes, on the grounds that taxing gains at death made them redundant.
;; Fifty-five years later, the gap Carter tried to close is still there.
;; :::

;; ## Conclusion

;; Canada's federal revenue comes mainly from taxes on work and spending.
;; Capital gains are concentrated among the highest-income 2% of filers.
;; They are taxed at half rate when realized, not at all when unrealized, and never on a principal residence.
;; Counting capital gains as income lowers the top 1%'s average income tax rate from 39% to 31%.
;; The wealthiest Canadians' unrealized growth doesn't show up in any tax statistic.

;; In the next part, [Wealth Tax](c02_wealth_tax.html), we look at what a Canadian wealth tax would do, and who it would touch.

;; [^1]: Department of Finance Canada, [Annual Financial Report of the Government of Canada, Fiscal Year 2024–2025](https://www.canada.ca/en/department-finance/services/publications/annual-financial-report/2025.html), Table 4. The Canada Pension Plan is excluded from the federal reporting entity; CPP contributions were \$81.6B in 2023–24 ([CPP Annual Report 2024](https://www.canada.ca/en/employment-social-development/programs/pensions/reports/annual-2024.html)).
;; [^2]: OECD, [Revenue Statistics 2025: Canada country note](https://www.oecd.org/content/dam/oecd/en/publications/reports/2025/12/revenue-statistics-2025-country-notes_3708be73/canada_3aa043fb/795aaca9-en.pdf). All levels of government, 2023.
;; [^3]: Canada Revenue Agency, [Individual Income Tax Return Statistics, 2023 tax year](https://www.canada.ca/en/revenue-agency/programs/about-canada-revenue-agency-cra/income-statistics-gst-hst-statistics/t1-final-statistics/2023-tax-year.html), Table 2 ([CSV](https://www.canada.ca/content/dam/cra-arc/prog-policy/stats/t1-final-stats/2023-tax-year/tbl2_ac.csv)). Income classes are by total income assessed.
;; [^4]: The inclusion rate history (0% before 1972; 50% from 1972; 66⅔% from 1988; 75% from 1990; 66⅔% from February 2000; 50% from October 18, 2000) and the cancellation of the 2024 proposal are documented in Department of Finance Canada, [Report on Federal Tax Expenditures 2026](https://www.canada.ca/content/dam/fin/publications/taxexp-depfisc/2026/taxexp-depfisc-26-eng.pdf), and the [Prime Minister's statement of March 21, 2025](https://www.pm.gc.ca/en/news/news-releases/2025/03/21/prime-minister-mark-carney-cancels-proposed-capital-gains-tax-increase). Ontario's 2026 top combined rate of 53.53% is computed from the federal 33% bracket and the Ontario rate and surtax in CRA's [T4127 Payroll Deductions Formulas, January 2026](https://www.canada.ca/en/revenue-agency/services/forms-publications/payroll/t4127-payroll-deductions-formulas/t4127-jan/t4127-jan-payroll-deductions-formulas-computer-programs.html) and [current tax rates](https://www.canada.ca/en/revenue-agency/services/tax/individuals/tax-rates-brackets/current-year.html).
;; [^5]: Office of the Parliamentary Budget Officer, [Increasing the Capital Gains Inclusion Rate](https://www.pbo-dpb.ca/en/publications/LEG-2425-010-S--increasing-capital-gains-inclusion-rate--augmentation-taux-inclusion-gains-capital), 2024.
;; [^6]: Department of Finance Canada, [Budget 2024, Chapter 8: Tax Fairness for Every Generation](https://budget.canada.ca/2024/report-rapport/chap8-en.html).
;; [^7]: Statistics Canada, [Table 11-10-0055-01, High income tax filers in Canada](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1110005501). Average federal and provincial/territorial income tax paid divided by average income, for the "Total income" and "Total income with capital gains" concepts.
;; [^8]: Michael Wolfson, Mike Veall, Neil Brooks and Brian Murphy, ["Piercing the Veil: Private Corporations and the Income of the Affluent"](https://ideas.repec.org/a/ctf/journl/v64y2016i1p1-30.html), *Canadian Tax Journal* 64(1): 1–30, 2016.
;; [^9]: Timothy Pratley, [Unbounded: Who Pays Tax?](https://clojurecivitas.org/economics/wealth_distribution/unbounded/u1_who_pays_tax.html), citing the IRS study [Estimating Tax Burdens by Wealth Group](https://www.irs.gov/pub/irs-soi/24rpestimatingtaxburdens.pdf).
;; [^10]: Statistics Canada, Distributions of Household Economic Accounts: [Table 36-10-0587-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610058701) (income, consumption and saving, by income quintile) and [Table 36-10-0660-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610066001) (wealth, by wealth quintile). Taxes appear only within "current transfers paid", which also includes non-tax transfers.
;; [^11]: Office of the Parliamentary Budget Officer, [Estimating the Top Tail of the Family Wealth Distribution in Canada: 2025 Update](https://www.pbo-dpb.ca/en/publications/RP-2526-009-S--estimating-top-tail-family-wealth-distribution-in-canada-2025-update--estimation-extremite-superieure-distribution-patrimoine-familial-canada-mises-jour-2025), and its wealth tax costings.
;; [^12]: Department of Finance Canada, [Report on Federal Tax Expenditures: Concepts, Estimates and Evaluations 2026](https://www.canada.ca/content/dam/fin/publications/taxexp-depfisc/2026/taxexp-depfisc-26-eng.pdf), Part 2 tables. Federal revenue only; figures for 2024 onward are projections.
;; [^13]: Children's benefits (mainly the Canada Child Benefit) were \$26.3B in 2023–24 and \$28.6B in 2024–25: [Annual Financial Report 2024–25](https://www.canada.ca/en/department-finance/services/publications/annual-financial-report/2025.html), Table 5.
;; [^14]: François Vaillancourt and Sarah Kerkhoff, ["Capital gains taxation in Canada, 1972–2017: evolution in a federal setting"](https://www.unsw.edu.au/content/dam/pdfs/business/acct-audit-tax/research-reports/past-issues/bus-2019/2019-volume-16-number-2/BUS-2019-V16-paper1-v16-n2.pdf), *eJournal of Tax Research* 16(2), 2019. Federal death taxes were first imposed in 1941; "All provinces abolished succession duties in the 1970s except for Quebec, which eliminated the tax in 1985."
;; [^15]: *Income Tax Act*, [section 70(5)](https://laws-lois.justice.gc.ca/eng/acts/I-3.3/section-70.html) (deemed disposition at death) and 70(6) (spousal rollover).
;; [^16]: *Report of the Royal Commission on Taxation* (Carter Commission), 1966, vol. 3. On the recommendations, the 1971 reform, and the attribution of "a buck is a buck" to Carter, see Vaillancourt and Kerkhoff (2019), above.
;; [^17]: Department of Finance Canada, [Tax Planning Using Private Corporations](https://www.canada.ca/content/dam/fin/migration/activty/consult/tppc-pfsp-eng.pdf), consultation paper, July 2017.
