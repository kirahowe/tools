^:kindly/hide-code
(ns economics.wealth-distribution.canada.c11-canadian-differences
  {:kindly/options {:static true
                    :kinds-that-hide-code #{:kind/var :kind/hiccup2 :kind/fn}}
   :clay {:title  "Unbounded North: What Makes Canada Different"
          :quarto {:sidebar     "unbounded-north"
                   :author      "Kira Howe"
                   :description "The features of Canada's economy and society that shape its version of the wealth concentration problem."
                   :abstract    "Canada is more equal and more mobile than the United States, thanks largely to public programs. But it has its own fault lines: housing, provinces, Indigenous communities, concentrated industries, and untaxed inheritance."
                   :date        "2026-10-21"
                   :type        :post
                   :category    :economics
                   :tags        [:tax :economics :growth :canada]}}}
  (:require [clojure.string :as str]
            [scicloj.plotje.api :as pj]
            [tablecloth.api :as tc]
            [economics.wealth-distribution.canada.common :as c]))

;; ## Not the United States

;; Most of the original series' argument carries over to Canada.
;; But the Canadian context differs in ways that matter for both the problem and the solution.
;; This part collects them in one place.
;; Some are reasons for optimism; some are distinctly Canadian problems.

;; ## More Equal, Because of Public Programs

(def gini-g7
  (-> (c/data "oecd_idd_gini_g7.csv")
      (tc/select-rows #(= 2023 (:year %)))
      (tc/update-columns {:country (partial map {"CAN" "Canada" "DEU" "Germany" "FRA" "France" "GBR" "United Kingdom"
                                                  "ITA" "Italy" "USA" "United States" "JPN" "Japan"})})
      (tc/add-column :c #(map (fn [x] (if (= x "Canada") c/red c/w)) (:country %)))
      (tc/order-by [:gini_disposable_income_total_pop])))

^{:kindly/options {:id :fig-gini-g7
                   :caption "Gini coefficient of disposable (after-tax and transfer) household income, 2023 (OECD Income Distribution Database; no 2023 figure for Japan). Higher means more unequal.[^1]"}}
(-> gini-g7
    (pj/lay-bar :gini_disposable_income_total_pop :country {:color {:column :c :scale false}})
    (pj/lay-label {:text :gini_disposable_income_total_pop})
    (pj/options {:title "Income Inequality After Taxes and Transfers, 2023"
                 :x-label "Gini coefficient" :y-label ""}))

;; After taxes and transfers, Canada's income distribution is much more equal than America's
;; (a Gini of 0.306 versus 0.394) and in line with Germany and France.[^1]
;; Before taxes and transfers it is far less equal: Statistics Canada's market-income Gini was 0.436 in 2024.[^2]
;; The difference is the work done by public programs.

;; Canada also has much higher intergenerational mobility.
;; Using linked tax records for both countries, Connolly, Corak and Haeck find:[^3]

;; | | Canada | United States |
;; | :--- | ---: | ---: |
;; | Chance a child from the bottom fifth reaches the top fifth | 11.2% | 7.5% |
;; | Chance a child from the top fifth stays there | 29.5% | 36.5% |
;; | Rank–rank slope (lower = more mobile) | 0.21–0.23 | 0.34 |
;;
;; : Intergenerational income mobility {#tbl-mobility}

;; In their words, "the US 'middle class' is within easier reach for low income Canadian children,
;; than it is for low income Americans."
;; They also find large differences *within* Canada: some regions "have more in common with the low mobility
;; southern parts of the United States than with the rest of Canada."[^3]

;; ### What Public Programs Do

;; Several Canadian programs reduce the cost of being alive in ways that matter for mobility:

;; * **Single-payer health insurance.** Canada spent 11.5% of GDP on health in 2024, versus 17.2% in the United States,
;;   and US\$7,372 per person versus US\$14,823.[^4]
;;   Health coverage doesn't depend on keeping a particular job.
;; * **The Canada Child Benefit** (since 2016), an income-tested, tax-free monthly payment to families.
;;   Peer-reviewed research finds it reduced child poverty with no detectable reduction in mothers' work.[^5]
;; * **Public pensions** (CPP/QPP, Old Age Security, and the Guaranteed Income Supplement), which keep senior poverty low.
;; * Newer national programs: **\$10-a-day child care** agreements with the provinces, **dental care**, and the start of **pharmacare**.

(def child-poverty
  (-> (c/data "statcan_poverty_rates_mbm_11-10-0135-01.csv")
      (tc/select-rows #(and (= "Persons under 18 years" (:group %))
                            (#{"Market basket measure, 2018 base" "Market basket measure, 2023 base"} (:mbm_base %))))
      (tc/update-columns {:mbm_base (partial map #(str/replace % "Market basket measure, " ""))})))

^{:kindly/options {:id :fig-child-poverty
                   :caption "Child poverty rate under Canada's official poverty line, the Market Basket Measure, on its 2018 and 2023 bases. The Canada Child Benefit began in July 2016; 2020–21 were lowered by temporary pandemic benefits.[^6]"}}
(-> child-poverty
    (pj/lay-line :year :poverty_rate_pct {:color :mbm_base :size 2})
    (pj/lay-point {:color :mbm_base})
    (pj/lay-rule-v {:x-intercept 2016.5 :stroke-dash :dashed})
    (pj/scale :color {:values [c/wd c/o]})
    (pj/scale :y {:include 0})
    (pj/options {:title "Child Poverty in Canada"
                 :x-label "Year" :y-label "% of children below the poverty line"
                 :color-label "" :legend-position :bottom}))

;; Child poverty fell from 16.3% in 2015 to 9.4% in 2019 on the 2018-base measure, and to 4.8% in 2020 with pandemic benefits.
;; Since then it has climbed back, to 11.5% in 2024 on the newer 2023 base, as the cost of living outran benefits.[^6]
;; Not all of the 2015–2019 decline was due to the CCB, but it shows that public programs can move the numbers quickly.
;; This is Canada's counterweight to the concentration loops in [Interactive Factors](c04_interactive_factors.html).
;; It is funded mainly by taxes on work.

;; ## A Federation of Very Different Economies

;; Canada's provinces differ more from each other than the national averages suggest.

(def provinces
  ["British Columbia" "Alberta" "Saskatchewan" "Manitoba" "Ontario" "Quebec"
   "New Brunswick" "Nova Scotia" "Prince Edward Island" "Newfoundland and Labrador"])

(def provincial
  (tc/left-join
   (-> (c/data "statcan_sfs_median_networth_by_province_11-10-0016-01.csv")
       (tc/select-rows #(and (= 2023 (:year %)) ((set provinces) (:geo %))))
       (tc/select-columns [:geo :median_net_worth_2023_constant_dollars]))
   (-> (c/data "statcan_gini_11-10-0134-01.csv")
       (tc/select-rows #(and (= 2024 (:year %)) (= "Adjusted after-tax income" (:income_concept %))
                             ((set provinces) (:geo %))))
       (tc/select-columns [:geo :gini]))
   :geo))

(def provincial-labelled
  (tc/add-column provincial :label #(map (fn [g] (str/replace g "Newfoundland and Labrador" "N.L.")) (:geo %))))

^{:kindly/options {:id :fig-provinces
                   :caption "Median family net worth (Survey of Financial Security 2023) and the Gini coefficient of adjusted after-tax income (2024), by province.[^7]"}}
(-> provincial-labelled
    (pj/lay-point :median_net_worth_2023_constant_dollars :gini {:size 6 :color c/wd})
    (pj/lay-text {:data (tc/select-rows provincial-labelled #(not= "Quebec" (:geo %)))
                  :text :label :offset-x 8 :offset-y -6})
    (pj/lay-text {:data (tc/select-rows provincial-labelled #(= "Quebec" (:geo %)))
                  :text :label :offset-x 8 :offset-y 10})
    (pj/options {:title "Wealth and Inequality by Province"
                 :x-label "Median family net worth, 2023 ($)"
                 :y-label "After-tax income Gini, 2024"}))

;; British Columbia's median family is worth \$773,500, more than two and a half times New Brunswick's \$286,200;
;; in the Vancouver area the median is over \$1M.[^7]
;; Most of that difference is home equity.
;; Ontario has the most unequal after-tax incomes of any province (Gini 0.318);
;; **Quebec**, with the most extensive social programs, including low-cost child care since 1997,
;; has one of the least unequal (0.276) despite a lower median net worth.[^2]

;; The provinces matter for a wealth tax.
;; They can levy direct taxes, but evidence from Spain and Switzerland shows that regional wealth taxes leak to the lowest-tax region.
;; A national tax avoids a race to the bottom (see [The Investment Question](c09_investment_question.html)).

;; ## Indigenous Peoples

;; The largest economic gaps in Canada are between Indigenous and non-Indigenous people.
;; In the 2021 Census, median income for people aged 25–64 was:[^8]

;; | Group | Median total income (2020) | Gap |
;; | :--- | ---: | ---: |
;; | Non-Indigenous | \$50,400 | — |
;; | Métis | \$48,800 | \$1,600 |
;; | Inuit | \$42,800 | \$7,600 |
;; | First Nations (Registered, off reserve) | \$42,000 | \$8,400 |
;; | First Nations (Registered, on reserve) | \$32,000 | \$18,400 |
;;
;; : Median income, ages 25–64, 2021 Census {#tbl-indigenous-income}

;; Almost a third (31.3%) of Registered First Nations people living on reserve were in low income in 2020,
;; against 10.7% of non-Indigenous people.[^8]
;; We do not even know the wealth gap: the Survey of Financial Security excludes people living on reserves,
;; and no official estimate of Indigenous wealth exists.[^9]
;; A country that cannot measure a gap cannot close it.

;; ## Newcomers

;; Immigration drives most of Canada's population growth, and newcomers start far behind on wealth.
;; Statistics Canada finds that Canadian-born families held double the median wealth of recent immigrant families in 2016 and 2023,
;; while established immigrant families had overtaken Canadian-born families by 2023.[^10]
;; Rising house prices make that climb harder for each new cohort: the asset that built the Canadian middle class
;; is the hardest one for newcomers to buy.

;; ## Concentrated Industries and Old Money

;; Canadians live with more concentrated industries than most.
;; The Big Six banks dominate banking, three national carriers dominate wireless,
;; and five chains control about 80% of grocery sales.[^11]
;; The Competition Bureau found concentration rising in Canada's most concentrated industries and entry falling across the economy.[^12]
;; Concentrated markets earn profits that flow to concentrated shareholdings.

;; Canada also has an unusual amount of dynastic control.
;; Families like the Thomsons, Westons, Irvings, McCains, Rogers, Saputos and Desmarais have controlled
;; major companies for generations, often through dual-class shares that let a family control a company
;; while owning a minority of its equity.
;; Research on "the Canadian disease" found that heir-controlled Canadian firms underperform (see [Economic Growth](c08_economic_growth.html)).[^13]

;; And there is no inheritance tax.
;; Canada abolished its federal estate tax in 1972 and the last provincial succession duties, Quebec's, in 1985.
;; Canada taxes accrued capital gains at death instead, which captures some growth but not the transfer itself.

;; ## Housing as the Middle-Class Asset

;; In the United States, middle-class wealth is split between homes and retirement accounts.
;; In Canada it is overwhelmingly homes, and the principal residence is completely exempt from capital gains tax.
;; That makes the housing market the main channel through which Canadian wealth inequality is produced.
;; Families aged 55–64 who own their home and have an employer pension had median net worth of \$1.4M in 2023;
;; renters without a pension had \$11,900.[^9]
;; Owners versus renters is Canada's real class line.

;; ## Brain Drain and the Exodus Argument

;; Canadians often worry about losing talent to the United States.
;; Emigration is real but modest.

(def emigration
  (-> (c/data "statcan_emigration_17-10-0040-01.csv")
      (tc/select-rows #(= 4 (:quarters_summed %)))
      (tc/pivot->longer [:emigrants :returning_emigrants] {:target-columns :measure :value-column-name :people})
      (tc/update-columns {:measure (partial map {:emigrants "Emigrants" :returning_emigrants "Returning emigrants"})})))

^{:kindly/options {:id :fig-emigration
                   :caption "Estimated emigrants from Canada and returning emigrants, by calendar year (Statistics Canada; recent quarters preliminary).[^14]"}}
(-> emigration
    (pj/lay-line :calendar_year :people {:color :measure :size 2})
    (pj/scale :color {:values [c/red c/wd]})
    (pj/scale :y {:include 0})
    (pj/options {:title "Emigration from Canada"
                 :x-label "Year" :y-label "People"
                 :color-label "" :legend-position :bottom}))

;; About 112,000 people emigrated in 2025, roughly a quarter of one percent of the population,
;; and about 50,000 returned.[^14]
;; Statistics Canada does not publish destinations or incomes, so claims about the *rich* leaving should be treated with care.
;; Canada already taxes accrued gains when residents leave, through the departure tax (see [Wealth Tax](c02_wealth_tax.html)).

;; ## Conclusion

;; Canada is more equal and more mobile than the United States, because of public programs,
;; not because markets distribute income more evenly.
;; Those programs are paid for mostly by taxes on work.
;; Canada's distinct fault lines (housing, provincial divides, Indigenous gaps, concentrated industries,
;; old family fortunes and untaxed inheritance) all run in the same direction:
;; toward wealth that compounds for those who already have it.

;; In the next part, [Scorecard](c12_scorecard.html), we look at what the current government has done so far, and what the data show.

;; [^1]: OECD, [Income Distribution Database](https://www.oecd.org/en/data/datasets/income-and-wealth-distribution-database.html), Gini coefficient of disposable income, total population, 2023.
;; [^2]: Statistics Canada, [Table 11-10-0134-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1110013401), Gini coefficients of adjusted market, total and after-tax income.
;; [^3]: Marie Connolly, Miles Corak and Catherine Haeck, ["Intergenerational Mobility between and within Canada and the United States"](https://doi.org/10.1086/703465), *Journal of Labor Economics* 37(S2), 2019. Figures from the [NBER working paper version](https://www.nber.org/papers/w25735), Tables 2–3.
;; [^4]: OECD, [System of Health Accounts](https://data-explorer.oecd.org/s/3zl), 2024, current health expenditure as % of GDP and per capita in US dollars at PPP.
;; [^5]: Michael Baker, Derek Messacar and Mark Stabile, ["Effects of Child Tax Benefits on Poverty and Labor Supply: Evidence from the Canada Child Benefit and Universal Child Care Benefit"](https://doi.org/10.1086/721379), *Journal of Labor Economics* 41(4), 2023.
;; [^6]: Statistics Canada, [Table 11-10-0135-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1110013501), low income statistics by age, Market Basket Measure.
;; [^7]: Statistics Canada, Survey of Financial Security 2023, [Table 11-10-0016-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1110001601), median net worth in 2023 dollars; the territories are not covered.
;; [^8]: Indigenous Services Canada, ["An update on the socio-economic gaps between Indigenous Peoples and the non-Indigenous population in Canada: Highlights from the 2021 Census"](https://www.sac-isc.gc.ca/eng/1690909773300/1690909797208).
;; [^9]: Statistics Canada, [The Daily, October 29, 2024](https://www150.statcan.gc.ca/n1/daily-quotidien/241029/dq241029a-eng.htm), Survey of Financial Security 2023. The survey excludes people living on reserves and in other Indigenous settlements, and the territories.
;; [^10]: Statistics Canada, ["Trends in the wealth gap between immigrant and Canadian-born families"](https://www150.statcan.gc.ca/n1/pub/36-28-0001/2026003/article/00002-eng.htm), *Economic and Social Reports*, March 2026.
;; [^11]: Grocery share from the Competition Bureau's 2023 retail grocery market study, as summarized by [Global News](https://globalnews.ca/news/9796699/competition-bureau-canada-grocery-study-takeaways/). I could not retrieve official market-share figures for banking and wireless, so none are given here.
;; [^12]: Competition Bureau Canada, ["Competition in Canada from 2000 to 2020: An Economy at a Crossroads"](https://ised-isde.canada.ca/site/competition-bureau-canada/en/how-we-foster-competition/education-and-outreach/competition-canada-2000-2020-economy-crossroads), 2023.
;; [^13]: Randall Morck, David Stangeland and Bernard Yeung, ["Inherited Wealth, Corporate Control and Economic Growth: The Canadian Disease"](https://www.nber.org/papers/w6814), NBER Working Paper 6814, 1998.
;; [^14]: Statistics Canada, [Table 17-10-0040-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1710004001), estimates of the components of international migration, quarterly, summed to calendar years.
