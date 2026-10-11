^:kindly/hide-code
(ns economics.wealth-distribution.canada.c03-labour-hours
  {:kindly/options {:static true
                    :kinds-that-hide-code #{:kind/var :kind/hiccup2 :kind/fn}}
   :clay {:title  "Unbounded North: Labour Hours"
          :quarto {:sidebar     "unbounded-north"
                   :author      "Kira Howe"
                   :description "How Canadian housing costs, household debt and weak median wage growth turn work into survival maintenance."
                   :abstract    "Canadian house prices have run far ahead of incomes, more than in the United States. Families have borrowed to keep up, and the median worker has captured little of the growth since the 1970s."
                   :date        "2026-10-13"
                   :type        :post
                   :category    :economics
                   :tags        [:tax :economics :growth :canada :housing]}}}
  (:require [clojure.string :as str]
            [scicloj.plotje.api :as pj]
            [tablecloth.api :as tc]
            [economics.wealth-distribution.canada.common :as c]))

;; ## Time Is the Ultimate Currency

;; Tim Pratley's original post made the case that the most important resource for progress is time:
;; time to learn, start something, raise children, change careers, recover, and take part in community life.[^1]
;; The question underneath every measure of prosperity is
;; *how much of a person's working life goes to securing the foundations of a good life?*

;; In Canada, the clearest answer is housing.

;; ## The Price of a Home

;; Canada has one of the largest housing booms in the developed world.
;; The OECD tracks house prices relative to disposable income per person for every member country.[^2]
;; Rebased so that 1970 equals 100, here is Canada beside the United States.

(def house-price-income
  (-> (c/data "oecd_house_price_to_income_can_usa_annual.csv")
      (tc/drop-missing :price_to_income_rebased_1970eq100)
      (tc/map-columns :year [:period] #(parse-long (str %)))
      (tc/update-columns {:country (partial map {"CAN" "Canada" "USA" "United States"})})))

^{:kindly/options {:id :fig-house-price-to-income
                   :caption "Nominal house price index divided by nominal disposable income per person, rebased to 1970 = 100 (OECD Analytical House Price Database). In Canada, homes cost about 1.9 times as much relative to income as in 1970, and peaked at 2.1 times in 2022. In the US, the ratio is back to roughly its 1970 level.[^2]"}}
(-> house-price-income
    (pj/lay-line :year :price_to_income_rebased_1970eq100 {:color :country :size 2})
    (pj/lay-rule-h {:y-intercept 100 :stroke-dash :dashed})
    (pj/scale :color {:values [c/red c/wd]})
    (pj/options {:title "House Prices Relative to Income"
                 :x-label "Year" :y-label "Index, 1970 = 100"
                 :color-label "" :legend-position :bottom}))

;; Through the 1970s and 1980s, Canadian house prices roughly tracked incomes, falling behind them in the mid-1980s.
;; From the early 2000s, they pulled away.
;; By 2022, a home cost more than twice as much relative to income as it did in 1970.
;; The United States had its own bubble and crash, but the American ratio is now back near its 1970 level.
;; **This is one of the sharpest differences between the two countries.**

;; The census gives a more familiar measure.
;; In 2016, the average owner-occupied home was worth \$443,058 and the median household income (for 2015) was \$70,336:
;; 6.3 years of income.
;; By 2021 the average home was worth about \$618,000 against a median household income of \$84,000 (for 2020, raised by pandemic benefits): about 7.4 years.[^3]
;; In Vancouver and Toronto the ratios are far higher.

;; If a family can save 5% of its income, every year of income in the home price means 20 years of saving.
;; Canada's housing boom has pushed the down payment, and the mortgage after it, further out of reach for each new generation.

;; ## Owning Later, or Not at All

(def ownership
  (-> (c/data "homeownership_rate_canada_census.csv")
      (tc/select-rows #(and (#{2011 2016 2021} (:year %))
                            (#{"25 to 29 years" "30 to 34 years" "35 to 39 years"} (:age_group %))))))

^{:kindly/options {:id :fig-young-ownership
                   :caption "Share of households that own their home, by age of the primary household maintainer (Census of Population; 2011 is from the voluntary National Household Survey).[^4]"}}
(let [ds (tc/map-columns ownership :age [:age_group] #(str/replace % " years" ""))]
  (-> ds
      (pj/lay-line :year :homeownership_rate_pct {:color :age :size 2})
      (pj/lay-point {:color :age :size 5})
      (pj/lay-text {:data (-> ds (tc/select-rows #(= 2021 (:year %)))
                              (tc/map-columns :t [:age :homeownership_rate_pct] #(format "Age %s: %.0f%%" %1 (double %2))))
                    :x :year :y :homeownership_rate_pct :text :t :align-x :left :offset-x 10 :overlay true})
      (pj/scale :color {:values [c/w "#7A9CC0" c/wd]})
      (pj/scale :x {:domain [2010 2024.5] :breaks [2011 2016 2021] :tick-labels ["2011" "2016" "2021"]})
      (pj/scale :y {:domain [0 80]})
      (pj/options {:title "Young Canadians Are Less Likely to Own"
                   :x-label "Census year" :y-label "Homeownership rate (%)"
                   :color-label "Age of household maintainer" :legend-position :bottom})))

;; Canada's homeownership rate peaked at 69% in 2011 and has fallen in every census since.[^4]
;; The decline is concentrated among the young.
;; Among households led by someone aged 25–29, ownership fell from 44% to 37% in just ten years.
;; The Canadian Housing Survey shows the same thing more recently: ownership among households aged 25–34
;; fell from 53% in 2018 to 47% in 2024, and first-time buyers are getting older.[^5]
;; In the four provinces and one territory where Statistics Canada tracks it, the median first-time buyer in 2023 was 32–34 years old.[^5]

;; Renters are squeezed hardest.
;; In 2024, 22% of renter households (1.2 million of them) were in core housing need:
;; living somewhere unaffordable, overcrowded, or in need of major repair, with no affordable alternative nearby.[^5]
;; From 2019 to 2025, the rent component of the consumer price index rose 30.6%, compared with 20.7% for prices overall.[^6]

;; ## Borrowing to Keep Up

;; Canadian families have kept buying homes by borrowing more.

(def household-debt
  (-> (c/data "household_debt_oecd_can_usa_annual.csv")
      (tc/update-columns {:country (partial map {"CAN" "Canada" "USA" "United States"})})))

^{:kindly/options {:id :fig-household-debt
                   :caption "Household (and non-profit) debt as a share of net disposable income (OECD National Accounts).[^7]"}}
(-> household-debt
    (pj/lay-line :year :household_npish_debt_pct_net_disposable_income {:color :country :size 2})
    (pj/scale :color {:values [c/red c/wd]})
    (pj/scale :y {:include 0})
    (pj/options {:title "Household Debt"
                 :x-label "Year" :y-label "% of net disposable income"
                 :color-label "" :legend-position :bottom}))

;; By Statistics Canada's measure, Canadian household debt rose from about 90% of disposable income in 1990 to a peak of 188% in 2022,
;; and was 176% in the second quarter of 2026.[^7]
;; (The OECD measure in the chart, which includes non-profits, shows the same rise.)
;; American households deleveraged after 2008; Canadian households didn't, and the ratio has eased only slightly since 2022.
;; Most of that debt is mortgages.
;; A Canadian family's home is often its largest asset *and* the reason for its largest debt.

;; This is the flip side of the "broad middle" in [Wealth Concentration](c05_wealth_concentration.html).
;; Canadian middle-class wealth is real, but it is concentrated in one illiquid asset bought with borrowed money.
;; Those who bought early rode the boom.
;; Those who didn't now pay the boom's price.

;; ## Living Comfortably

;; What does it take to live decently in a Canadian city?
;; Living wage networks estimate the hourly wage two working parents need to cover a basic budget for a family of four,
;; after taxes and benefits.[^8]

(def living-wage
  "Living wages (2025) from the local networks; minimum wages in force on 2026-10-10."
  (tc/dataset {:city    ["Toronto (GTA)" "Metro Vancouver" "Calgary" "Halifax"]
               :living  [27.20 27.85 26.50 29.40]
               :minimum [17.95 18.25 15.00 17.00]}))

^{:kindly/options {:id :fig-living-wage
                   :caption "Living wage (2025 estimates by local living wage networks) and the provincial minimum wage in force in October 2026. Living wages are methodology-based estimates, not official statistics.[^8]"}}
(c/dumbbell
 (for [r (tc/rows living-wage :as-maps)] {:label (:city r) :from (:minimum r) :to (:living r)})
 {:from-name "Minimum wage" :to-name "Living wage" :from-color c/w :to-color c/o :include-zero true
  :title "Living Wage vs Minimum Wage" :x-label "C$ per hour" :fmt #(format "$%.2f" (double %))})

;; In each of these four cities, the minimum wage falls \$9 to \$12 an hour short of the living wage.
;; Calgary's minimum wage hasn't moved since 2018.

;; The median Canadian individual's employment income was \$46,100 in 2024.
;; In inflation-adjusted terms, that is only about 7% more than in 1976.[^9]
;; The figure includes part-time and part-year workers; it fell through the 1980s and 1990s and has recovered since the late 1990s.

(def median-employment
  (-> (c/data "income_canada_cis_annual.csv")
      (tc/select-rows #(and (= "Individuals 15+" (:population %))
                            (= "Employment income - Median income (excluding zeros)" (:measure %))))))

^{:kindly/options {:id :fig-median-employment-income
                   :caption "Median employment income of individuals with employment income, in 2024 dollars (Statistics Canada, Canadian Income Survey and predecessors).[^9]"}}
(-> median-employment
    (pj/lay-line :year :value {:color c/wd :size 2})
    (pj/scale :y {:include 0})
    (pj/options {:title "Median Employment Income, 1976–2024"
                 :x-label "Year" :y-label "2024 dollars"}))

;; Almost half a century of economic growth has barely moved what the typical worker earns.
;; Meanwhile food bank use has doubled.
;; Food Banks Canada recorded 2.17 million visits in March 2025, up 99% from March 2019,
;; and 19.4% of clients had a job, up from 12.2% in 2019.[^10]
;; Work no longer guarantees you can afford to eat.

;; ## The Golden Age

;; Canada had its own golden age.
;; From 1946 to 1975, real GDP per person grew about 2.5% a year.
;; From 1980 to 2022 it grew about 1.4%.[^11]
;; In the golden age, the top federal income tax rate was 80% in 1950 and 75% in 1965, there was a federal estate tax,
;; and capital gains were untaxed.

(def top-rates
  (-> (c/data "top_marginal_tax_rate_canada_vs_us_1920_2025.csv")
      (tc/update-columns {:country (partial map {"CAN" "Canada" "USA" "United States"})})))

^{:kindly/options {:id :fig-top-rates
                   :caption "Top statutory marginal income tax rate. Canada: federal before 1972 and federal plus Ontario after, including surtaxes; United States (federal plus state, OECD Tax Database) shown from 2000 only. Very few taxpayers paid the top rate in the mid-century.[^12]"}}
(-> top-rates
    (pj/lay-step :year :top_marginal_rate_pct {:color :country})
    (pj/scale :color {:values [c/red c/wd]})
    (pj/scale :y {:domain [0 100]})
    (pj/options {:title "Top Marginal Income Tax Rate"
                 :x-label "Year" :y-label "%"
                 :color-label "" :legend-position :bottom}))

;; Canada's top rate fell sharply from 1972, earlier than America's: from 76% in 1971 to 59% in 1972, and to 46% by 1988.[^12]
;; Unlike the United States, Canada's top combined rate has risen again, to 53.5% in Ontario, since 2016.
;; But that top rate applies to *income*.
;; As [Who Pays Tax](c01_who_pays_tax.html) showed, the largest fortunes grow mostly through capital gains,
;; which are half-taxed when sold and untaxed until then.
;; A high top income tax rate is not the same as a high tax on the very rich.

;; Saez and Veall caution that the mid-century top rates applied to very few people.
;; Even so, the very top faced much higher rates, and the estate tax caught what income tax missed.

;; ## What Changed

;; Like the United States, Canada saw pay fall behind productivity after the mid-1970s.

(def productivity-pay-golden
  (-> (c/data "productivity_pay_index_long.csv")
      (tc/select-rows #(= "A_1946-2001_archived_SIC" (:panel %)))
      (tc/update-columns {:series (partial map {"Business-sector labour productivity" "Productivity"
                                                 "Real total compensation per hour (CPI-deflated)" "Real compensation per hour"})})))

^{:kindly/options {:id :fig-productivity-pay
                   :caption "Business-sector labour productivity and real (CPI-deflated) total compensation per hour, 1946 = 100 (Statistics Canada, archived Table 36-10-0305-01). They rose together until the mid-1970s.[^13]"}}
(-> productivity-pay-golden
    (pj/lay-line :year :value {:color :series :size 2})
    (pj/lay-rule-v {:x-intercept 1975 :stroke-dash :dashed})
    (pj/scale :color {:values [c/green c/wd]})
    (pj/options {:title "Productivity and Pay in Canada, 1946–2001"
                 :x-label "Year" :y-label "Index, 1946 = 100"
                 :color-label "" :legend-position :bottom}))

;; From 1946 to 1975, productivity grew 3.75% a year and real compensation per hour grew 3.89%: they moved in lockstep.
;; From 1975 to 2000, productivity grew 1.5% a year and real compensation only 0.5%.[^13]

;; Since 1997 the Canadian story differs from the American one, and it is worth being precise.
;; *Average* compensation per hour has roughly kept up with productivity: both rose about 35% by 2025,
;; helped by Canada's resource export prices.
;; But the *median* hourly wage rose only 21%.[^13]

(def productivity-pay-recent
  (-> (c/data "productivity_pay_index_long.csv")
      (tc/select-rows #(= "D_1997-2025" (:panel %)))
      (tc/update-columns {:series (partial map {"Business-sector labour productivity" "Productivity"
                                                 "Real total compensation per hour (CPI-deflated)" "Average compensation per hour"
                                                 "Real average hourly wage, all employees (LFS, CPI-deflated)" "Average hourly wage"
                                                 "Real median hourly wage, all employees (LFS, CPI-deflated)" "Median hourly wage"})})))

^{:kindly/options {:id :fig-productivity-median
                   :caption "Business-sector labour productivity, real compensation per hour, and real average and median hourly wages from the Labour Force Survey, 1997 = 100. All CPI-deflated.[^13]"}}
(-> productivity-pay-recent
    (pj/lay-line :year :value {:color :series :size 2})
    (pj/scale :color {:values [c/green c/w "#b5b0a3" c/wd]})
    (pj/options {:title "Productivity and Pay since 1997"
                 :x-label "Year" :y-label "Index, 1997 = 100"
                 :color-label "" :legend-position :bottom}))

;; The gap since 1997 is a gap *within* labour income: between the typical worker and the average,
;; which is pulled up by the highest earners.
;; Research by the Centre for the Study of Living Standards finds that from 1976 to 2014 Canadian median wages
;; grew 0.09% a year while productivity grew 1.12%, and that rising inequality among earners explains about half of the gap.[^14]
;; The rest is a falling labour share and consumer prices rising faster than the prices of what Canada produces.
;; Labour compensation was 61.1% of business-sector GDP in 1976 and 56.3% in 2021.[^13]

;; Either way the result is the same as in the United States.
;; The typical worker's hour buys less housing than their parents' did, and the gains from growth go disproportionately
;; to the top earners and to asset owners.

;; ## The Social Cost

;; When more people are stuck in survival mode, society spends less energy on innovation and more on getting by.
;; Young Canadians are delaying homeownership, delaying starting families,
;; and carrying debt their parents never needed.
;; A country that makes its young people spend their working lives catching up to the price of a home
;; is wasting the time that builds a future.

;; In the next part, [Interactive Factors](c04_interactive_factors.html), we look at how these pressures connect.

;; [^1]: Timothy Pratley, [Unbounded: Labor Hours](https://clojurecivitas.org/economics/wealth_distribution/unbounded/u3_labor_hours.html), ClojureCivitas, 2026.
;; [^2]: OECD, [Analytical House Price Database](https://data-explorer.oecd.org/s/eh) (dataflow `OECD.ECO.MPD:DSD_AN_HOUSE_PRICES@DF_HOUSE_PRICES`), price-to-income ratio, annual averages, rebased by me to 1970 = 100. The denominator is disposable income per person for the whole economy, not median household income.
;; [^3]: Statistics Canada, [The Daily, October 25, 2017](https://www150.statcan.gc.ca/n1/daily-quotidien/171025/dq171025c-eng.htm) (2016 average dwelling value), [The Daily, September 13, 2017](https://www150.statcan.gc.ca/n1/daily-quotidien/170913/dq170913a-eng.htm) (2015 median household income), [Census 2021 housing release](https://www150.statcan.gc.ca/n1/daily-quotidien/220921/dq220921b-eng.pdf) (+39.6% average value growth 2016–2021), and Table 98-10-0061-01 (2020 median household income). The 2021 value is derived from the 2016 value and the published growth rate; CMHC reports \$617,500.
;; [^4]: Statistics Canada, [The Daily, September 21, 2022](https://www150.statcan.gc.ca/n1/daily-quotidien/220921/cg-b002-eng.htm), homeownership rates by age, 2011–2021.
;; [^5]: Statistics Canada, Canadian Housing Survey, [Table 46-10-0083-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=4610008301) and [Table 46-10-0085-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=4610008501) (core housing need); Canadian Housing Statistics Program, [Table 46-10-0099-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=4610009901) (age of first-time buyers in BC, Manitoba, New Brunswick, Nova Scotia and Yukon).
;; [^6]: Statistics Canada, [Table 18-10-0005-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1810000501), Consumer Price Index, annual average.
;; [^7]: OECD National Accounts, household debt as % of net disposable income; Statistics Canada, [Table 38-10-0238-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3810023801), household credit market debt to disposable income (176.4% in Q2 2026).
;; [^8]: Living wages: Ontario Living Wage Network (GTA, \$27.20, as reported by [CP24](https://www.cp24.com/local/toronto/2025/11/10/torontonians-should-be-making-about-10-more-than-minimum-wage-to-cover-living-expenses-in-gta-report/)); Living Wage BC (Metro Vancouver, \$27.85, as reported by [Daily Hive](https://dailyhive.com/vancouver/living-wage-bc-vancouver-2025)); Vibrant Communities Calgary/Alberta Living Wage Network (Calgary, \$26.50, as reported by [Global News](https://globalnews.ca/news/11525284/calgary-living-wage-2025)); CCPA–Nova Scotia, [Living Wages in NL, NS and PEI 2025](https://www.policyalternatives.ca/wp-content/uploads/2025/08/2025-Living-Wages-for-NL-NS-and-PEI-Too-many-workers-struggle-to-make-ends-meet-1.pdf) (Halifax, \$29.40). Minimum wages from provincial governments, in force on October 10, 2026. Alberta's network now averages across household types, so its figure is not strictly comparable.
;; [^9]: Statistics Canada, [Table 11-10-0239-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1110023901), income of individuals, median employment income (excluding zeros), 2024 constant dollars.
;; [^10]: Food Banks Canada, [HungerCount 2025](https://foodbankscanada.ca/press-releases/canadas-2025-hungercount-shows-the-cost-of-inaction/). Food Banks Canada is a national charity; HungerCount is a survey of its member food banks.
;; [^11]: [Maddison Project Database 2023](https://www.rug.nl/ggdc/historicaldevelopment/maddison/releases/maddison-project-database-2023) (Bolt and van Zanden), real GDP per capita, compound annual growth.
;; [^12]: Emmanuel Saez and Michael Veall, [Canadian top income and marginal tax rate series](https://eml.berkeley.edu/~saez/) (to 1999; published as ["The Evolution of High Incomes in Northern America"](https://doi.org/10.1257/0002828054201404), *American Economic Review* 95(3), 2005), and the [OECD Tax Database](https://data-explorer.oecd.org/vis?df%5Bag%5D=OECD.CTP.TPS&df%5Bds%5D=dsDisseminateFinalDMZ&df%5Bid%5D=DSD_TAX_PIT%40DF_PIT_TOP_EARN_THRESH), top statutory personal income tax rates (2000 onward).
;; [^13]: Statistics Canada, archived [Table 36-10-0305-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610030501) (1946–2001), [Table 36-10-0206-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610020601), [Table 36-10-0208-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610020801) (labour share), [Table 14-10-0064-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1410006401) (Labour Force Survey wages), all deflated by CPI ([Table 18-10-0005-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1810000501)). Deflating by the GDP deflator instead would show a smaller gap.
;; [^14]: James Uguccioni, ["Explaining the Gap between Productivity and Median Wage Growth in Canada, 1976–2014"](https://csls.ca/ipm-archive/ipm-issue-31/explaining-the-gap-between-productivity-and-median-wage-growth-in-canada-1976-2014.md), *International Productivity Monitor* 31, 2016. See also Sharpe, Arsenault and Harrison, ["Why Have Real Wages Lagged Labour Productivity Growth in Canada?"](https://csls.ca/ipm-archive/ipm-issue-17/why-have-real-wages-lagged-labour-productivity-growth-in-canada.md), *International Productivity Monitor* 17, 2008.
