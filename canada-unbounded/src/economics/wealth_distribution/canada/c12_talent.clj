^:kindly/hide-code
(ns economics.wealth-distribution.canada.c12-talent
  {:kindly/options {:static true
                    :kinds-that-hide-code #{:kind/var :kind/hiccup2 :kind/fn}}
   :clay {:title  "Unbounded North: Talent and Opportunity"
          :quarto {:sidebar     "unbounded-north"
                   :author      "Kira Howe"
                   :description "Who leaves Canada, who arrives, and whether Canadian children really have better odds than American ones."
                   :abstract    "Canada is better at raising children out of poverty than the United States, but the advantage is smaller than usually claimed, sits in the bottom half, and is shrinking."
                   :date        "2026-10-22"
                   :type        :post
                   :category    :economics
                   :tags        [:tax :economics :growth :canada]}}}
  (:require [clojure.string :as str]
            [scicloj.plotje.api :as pj]
            [tablecloth.api :as tc]
            [economics.wealth-distribution.canada.common :as c]))

;; ## Opportunity, Inherited

;; The original series argues that concentrated wealth blocks the circulation of opportunity.
;; The usual Canadian reply is that Canada keeps opportunity circulating better than the United States:
;; a poor child here has a better shot.
;; This part tests that claim.

;; ## Is Canada Really More Mobile?

;; Economists measure intergenerational mobility in several ways.
;; The most robust is the **rank–rank slope**: if parents move up ten percentiles in their country's income distribution,
;; how many percentiles higher do their children end up, on average?
;; A slope of 0 means parents' income tells you nothing about children's; a slope of 1 means children inherit their parents' rank exactly.

;; The best comparison uses tax records in both countries for children born around 1980.[^1]
;; Canada's rank–rank slope is 0.21–0.23 across six ways of measuring it; America's is 0.31–0.34.
;; A Canadian child's rank depends about a third less on their parents' rank.
;; That result is solid.

;; The figure most often quoted, though, is that 11.2% of Canadian children born in the bottom fifth reach the top fifth,
;; against 7.5% in the United States.
;; That comparison ranks each country's children against their own country's income distribution,
;; and Canada's top fifth starts at a lower income than America's.
;; Placed on the US income scale, the Canadian figure is **7.7%**, essentially the same as the American one.[^1]

(def bottom-outcomes
  (tc/dataset
   [{:outcome "Stays in bottom fifth" :group "Canada (US cut-offs)" :pct 29.1}
    {:outcome "Stays in bottom fifth" :group "United States" :pct 33.7}
    {:outcome "Reaches top fifth" :group "Canada (own cut-offs)" :pct 11.2}
    {:outcome "Reaches top fifth" :group "Canada (US cut-offs)" :pct 7.7}
    {:outcome "Reaches top fifth" :group "United States" :pct 7.5}]))

^{:kindly/options {:id :fig-bottom-outcomes
                   :caption "Outcomes for children whose parents were in the bottom fifth of income, children born 1980–82, incomes at about age 30–32. \"US cut-offs\" places Canadian incomes (converted at purchasing power parity) in the American distribution.[^1]"}}
(-> bottom-outcomes
    (pj/lay-bar :outcome :pct {:color :group :position :dodge})
    (pj/scale :color {:values [c/red c/o c/w]})
    (pj/options {:title "Where Children of the Poorest Fifth End Up"
                 :x-label "" :y-label "% of children"
                 :color-label "" :legend-position :bottom}))

;; On a common yardstick, Canada's advantage is in the bottom half:
;; poor Canadian children are less likely to stay poor (29.1% versus 33.7%) and more likely to reach the middle.
;; In the authors' words, they are "much more likely to rise to the top half of the income distribution...
;; although not necessarily more likely to rise to the top fifth."[^1]
;; At the top, Canada is less sticky than the US at the level of fifths: 29.5% of children of top-fifth parents stay there, versus 36.5%.

;; Two cautions on the comparison.
;; The Canadian children's incomes were measured in 2011–12, at the height of the commodity boom,
;; and the most mobile Canadian regions are in Alberta, Saskatchewan and Newfoundland and Labrador, which the authors flag themselves.[^1]
;; And part of Canada's advantage comes from its large second generation of immigrants:
;; children of immigrants are very mobile in both countries,
;; and one working paper puts the slope for Canadian-born children alone at about 0.29, still lower than the American 0.34 but much closer.[^2]

;; ## A Shrinking Advantage

(def cohort-trend
  (-> (c/data "mob_canada_relative_mobility_by_cohort.csv")
      (tc/select-rows #(and (= "Ages 25 to 29" (:child_age %))
                            (#{"P(child bottom quintile | parent quintile) ; parent Quintile 1 (bottom)"
                               "P(child middle three quintiles | parent quintile) ; parent Quintile 1 (bottom)"
                               "P(child top quintile | parent quintile) ; parent Quintile 1 (bottom)"} (:measure %))))
      (tc/map-columns :outcome [:measure]
                      #(cond (str/includes? % "bottom quintile |") "Stayed in bottom fifth"
                             (str/includes? % "middle three") "Reached the middle three fifths"
                             :else "Reached top fifth"))
      (tc/map-columns :pct [:value] #(* 100 %))))

(def slope-trend
  (-> (c/data "mob_canada_relative_mobility_by_cohort.csv")
      (tc/select-rows #(and (= "Rank-rank" (:measure %)) (= "Ages 25 to 29" (:child_age %))))))

^{:kindly/options {:id :fig-slope-trend
                   :caption "Rank–rank slope of child income (ages 25–29) on parents' family income, by birth cohort (Statistics Canada, Intergenerational Income Database). Higher means less mobility. Definitions differ from the Canada–US comparison above, so the levels are not comparable to it.[^3]"}}
(-> slope-trend
    (pj/lay-line :cohort_label :value {:size 2 :color c/red})
    (pj/lay-point {:color c/red})
    (pj/scale :y {:include 0})
    (pj/options {:title "Canadian Mobility Is Falling"
                 :x-label "Children born from (four-year cohorts)" :y-label "Rank–rank slope"}))

;; Canadian mobility has declined steadily.
;; The rank–rank slope rose by about a quarter, from 0.189 for children born 1963–66 to 0.234 for children born 1982–85.[^3]
;; Over the same cohorts, the American slope stayed close to 0.30.[^4]
;; The authors describe Canada as "going up the Great Gatsby Curve": as parents' incomes became more unequal
;; (the Gini of parental pre-tax income rose from 0.36 to 0.44), children's outcomes became more tied to them.[^3]

^{:kindly/options {:id :fig-bottom-trend
                   :caption "Outcomes at ages 25–29 for children of bottom-fifth parents, by birth cohort.[^3]"}}
(-> cohort-trend
    (pj/lay-line :cohort_label :pct {:color :outcome :size 2})
    (pj/lay-point {:color :outcome})
    (pj/scale :color {:values [c/red c/wd c/o]})
    (pj/scale :y {:include 0})
    (pj/options {:title "Children of the Poorest Fifth, by Birth Cohort"
                 :x-label "Children born from (four-year cohorts)" :y-label "% of children"
                 :color-label "" :legend-position :bottom}))

;; The decline is a growing poverty trap, not a closing road to the top.
;; The chance of reaching the top fifth held at 12–13%;
;; the chance of staying in the bottom fifth rose from 27% to 33%, mostly at the expense of reaching the middle.[^3]

;; ## Not Everywhere

(def province-slopes
  (let [ds (-> (c/data "mob_canada_rank_slope_by_province_cohort.csv")
               (tc/select-rows #(#{1963 1982} (:cohort_label %)))
               (tc/map-columns :cohort [:birth_years] #(str "Born " %)))
        order (->> (tc/rows (tc/select-rows ds #(= 1982 (:cohort_label %))) :as-maps)
                   (sort-by :value)
                   (map :province_abbr))
        rank (zipmap order (range))]
    (-> ds (tc/order-by [#(rank (:province_abbr %)) :cohort_label]))))

^{:kindly/options {:id :fig-province-slopes
                   :caption "Rank–rank slope by province or territory, children born 1963–66 and 1982–85, ranked within each province. Higher means less mobility. Nunavut and the Northwest Territories are combined.[^3]"}}
(-> province-slopes
    (pj/lay-bar :province_abbr :value {:color :cohort :position :dodge})
    (pj/coord :flip)
    (pj/scale :color {:values [c/w c/red]})
    (pj/options {:title "Mobility Fell in Almost Every Province"
                 :x-label "" :y-label "Rank–rank slope (higher = less mobile)"
                 :color-label "" :legend-position :bottom :height 420}))

;; Every province became less mobile except Yukon.
;; Saskatchewan saw the largest decline (0.175 to 0.331), and Manitoba (0.315) and the Northwest Territories and Nunavut (0.350)
;; now look more like the least mobile parts of the United States than the rest of Canada.[^3]
;; A flat slope does not guarantee opportunity, either:
;; in New Brunswick and Nova Scotia, children are not tied to their parents' rank,
;; but only 6–7% of children from the bottom fifth reached the top fifth, below the US average.[^5]

;; The sharpest gap is for **Indigenous children**.
;; Indigenous share of the population is the strongest regional predictor of low mobility in Canada,
;; much as the share of Black residents is in the United States.[^1]
;; New research using linked census and tax data finds mobility about two times lower for children of Indigenous background,
;; and that they account for almost all of the steeper slope at the bottom of Canada's distribution.[^6]

;; ## At the Very Top

;; Canada's mobility is a story about the bottom and middle.
;; At the very top, Canadian advantage passes down strongly.
;; Among sons of the top 1% of fathers, the income elasticity is "almost 0.8",
;; compared with close to zero at the bottom.[^7]
;; Almost seven in ten sons of top-1% fathers have worked for an employer that also employed their father,
;; compared with about four in ten overall.[^8]
;; This is the labour-market face of the family firms in [Old Money](c07_old_money.html).

;; Wealth is harder to trace, because Canada's tax-linked mobility data have no wealth in them.
;; Home ownership is the clearest signal:
;; Canadians born in the 1990s are twice as likely to own a home if their parents do (17.4% versus 8.1% in 2021),
;; and 23.8% if their parents own more than one.[^9]

;; ## Mobility: The Verdict

;; | Measure | Canada better? | Evidence |
;; | :--- | :--- | :--- |
;; | Rank–rank slope | **Yes, robustly** | 0.21–0.23 vs 0.31–0.34 |
;; | Poor children staying poor | **Yes** | 29.1% vs 33.7% on a common yardstick |
;; | Poor children reaching the top fifth | **No difference** | 7.7% vs 7.5% on a common yardstick |
;; | Share out-earning their parents (born 1985) | **Slightly** | 57% vs 50%; Norway 74%[^10] |
;; | Father–son earnings elasticity | **Yes, but less than once thought** | about 0.32 vs 0.4–0.5[^11] |
;; | Trend | **No: Canada worsening, US flat** | slope 0.19 → 0.23 |
;; | Indigenous children, Manitoba, the North | **No** | mobility about half the national rate |
;; | Very top | **Strong persistence** | income elasticity near 0.8 for the top 1% |
;;
;; : How well Canada's mobility advantage holds up {#tbl-mobility-verdict}

;; Canada is better than the United States at keeping poor children from staying poor.
;; It is not better at giving them a shot at the top, and its advantage is eroding as inequality among parents grows.
;; The OECD estimates it would take about four generations for a family in Canada's bottom tenth to reach average income,
;; versus five in the United States and two or three in the Nordic countries.[^12]

;; In the next part, [Scorecard](c13_scorecard.html), we look at what the current government has done so far, and what the data show.

;; [^1]: Marie Connolly, Miles Corak and Catherine Haeck, ["Intergenerational Mobility between and within Canada and the United States"](https://doi.org/10.1086/703465), *Journal of Labor Economics* 37(S2), 2019, Tables 2–5 and 7, pp. S612–S625 ([author's copy](https://milescorak.com/wp-content/uploads/2020/10/connolly-corak-haeck-2019-intergenerational-mobility-canada-us-jole.pdf)). Canadian incomes converted at the 2012 PPP rate.
;; [^2]: Aiyesha Adnan, Xuelin Zhang and Yuqian Zheng, [IZA Discussion Paper 16471](https://docs.iza.org/dp16471.pdf), 2023 (working paper). The Canadian-born slope (0.286) is reported there as supplied by M. Connolly; it uses different income definitions from Connolly, Corak and Haeck, so treat it as indicative. See also Abdurrahman Aydemir, Wen-Hao Chen and Miles Corak, ["Intergenerational Earnings Mobility among the Children of Canadian Immigrants"](https://doi.org/10.1162/rest.91.2.377), *Review of Economics and Statistics* 91(2), 2009.
;; [^3]: Marie Connolly, Catherine Haeck and David Lapierre, ["Trends in Intergenerational Income Mobility and Income Inequality in Canada"](https://www150.statcan.gc.ca/n1/pub/11f0019m/11f0019m2021001-eng.htm), Statistics Canada Analytical Studies Branch Research Paper 11F0019M No. 458, 2021, Tables 2–3 and Charts 3–6 data tables; published as Connolly and Haeck, *Canadian Journal of Economics* 57(1), 2024, [doi:10.1111/caje.12699](https://doi.org/10.1111/caje.12699).
;; [^4]: Raj Chetty, Nathaniel Hendren, Patrick Kline, Emmanuel Saez and Nicholas Turner, ["Is the United States Still a Land of Opportunity? Recent Trends in Intergenerational Mobility"](https://doi.org/10.1257/aer.104.5.141), *American Economic Review* 104(5), 2014.
;; [^5]: Miles Corak, ["The Canadian Geography of Intergenerational Income Mobility"](https://doi.org/10.1093/ej/uez019), *Economic Journal* 130(631), 2020, Tables 4–6 (children born 1963–70; not directly comparable with the US figure).
;; [^6]: Marie Connolly and Catherine Haeck, "The Great Mobility Puzzle Resolved", *Canadian Public Policy* 52(1), 2026, [doi:10.3138/cpp.2026-004](https://doi.org/10.3138/cpp.2026-004) (abstract). The authors note that the on-reserve income tax exemption may lower measured incomes.
;; [^7]: Miles Corak and Andrew Heisz, ["The Intergenerational Earnings and Income Mobility of Canadian Men: Evidence from Longitudinal Income Tax Data"](https://econwpa.ub.uni-muenchen.de/econ-wp/lab/papers/9808/9808001.pdf), *Journal of Human Resources* 34(3), 1999.
;; [^8]: Miles Corak, ["Income Inequality, Equality of Opportunity, and Intergenerational Mobility"](https://doi.org/10.1257/jep.27.3.79), *Journal of Economic Perspectives* 27(3), 2013, citing Corak and Piraino, *Journal of Labor Economics* 29(1), 2011.
;; [^9]: Statistics Canada, Canadian Housing Statistics Program, ["Homeownership among young Canadians: the role of parents"](https://www150.statcan.gc.ca/n1/pub/46-28-0001/2023001/article/00004-eng.htm), 2023 (2021 data; excludes Quebec and Saskatchewan).
;; [^10]: Statistics Canada, [Economic and Social Reports 36-28-0001, December 2023](https://www150.statcan.gc.ca/n1/pub/36-28-0001/2023012/article/00001-eng.htm), charting Manduca et al., *AEJ: Applied Economics* 16(2), 2024. See also Yuri Ostrovsky, [StatCan 11-626-X No. 073](https://www150.statcan.gc.ca/n1/pub/11-626-x/11-626-x2017073-eng.htm), 2017, which finds the two countries "similar" for the 1984 cohort.
;; [^11]: Wen-Hao Chen, Yuri Ostrovsky and Patrizio Piraino, ["Lifecycle Variation, Errors-in-Variables Bias and Nonlinearities in Intergenerational Income Transmission"](https://www150.statcan.gc.ca/n1/pub/11-626-x/11-626-x2016059-eng.htm), Statistics Canada 2016; *Labour Economics* 44, 2017. Older estimates of about 0.19 understated Canadian persistence.
;; [^12]: OECD, [*A Broken Social Elevator? How to Promote Social Mobility*](https://doi.org/10.1787/9789264301085-en), 2018, Figure 1.5. The OECD calls these simulations illustrative.
