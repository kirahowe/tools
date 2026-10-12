^:kindly/hide-code
(ns economics.wealth-distribution.canada.c12-talent
  {:kindly/options {:static true
                    :kinds-that-hide-code #{:kind/var :kind/hiccup2 :kind/fn}}
   :clay {:title  "Unbounded North: Talent and Opportunity"
          :quarto {:sidebar     "unbounded-north"
                   :author      "Kira Howe"
                   :description "Who leaves Canada, who arrives, and whether Canadian children really have better odds than American ones."
                   :abstract    "Canada gains far more educated people than it loses, but leaks its very best home-grown talent to the United States. It is better than the US at keeping poor children from staying poor, but not at getting them to the top, and that advantage is shrinking."
                   :date        "2026-10-22"
                   :type        :post
                   :category    :economics
                   :tags        [:tax :economics :growth :canada]}}}
  (:require [clojure.string :as str]
            [scicloj.plotje.api :as pj]
            [tablecloth.api :as tc]
            [economics.wealth-distribution.canada.common :as c]))

;; ## Circulation, Both Ways

;; The original series argues that progress depends on circulation: of ideas, resources, and people.
;; Canada's version of that question has two halves.
;; Does Canada lose the talent it trains, and does it give the children it raises a fair shot?
;; Canadians usually answer "yes, badly" to the first and "yes, better than the Americans" to the second.
;; The data say both answers need qualifying.

;; ## Who Leaves, Who Arrives

(def flows
  (-> (c/data "tal_canada_talent_flows_2000_2025.csv")
      (tc/select-rows #(#{"immigrants" "net_emigration"} (:series %)))
      (tc/update-columns {:series (partial map {"immigrants" "Immigrants (permanent residents)"
                                                "net_emigration" "Net emigration"})})))

^{:kindly/options {:id :fig-flows
                   :caption "Permanent immigrants admitted and net emigration (emigrants minus returning emigrants, plus net temporary emigration before 2016), by calendar year. Temporary residents are not included.[^t1]"}}
(-> flows
    (pj/lay-line :year :value {:color :series :size 2})
    (pj/scale :color {:values [c/wd c/red]})
    (pj/scale :y {:include 0})
    (pj/options {:title "People Arriving vs People Leaving"
                 :x-label "Year" :y-label "People per year"
                 :color-label "" :legend-position :bottom}))

;; By headcount, it isn't close.
;; In 2025, Canada admitted 393,781 permanent immigrants; net emigration was 61,508.
;; Since 2000 Canada has admitted about 4 to 10 permanent immigrants for every net emigrant.[^t1]
;; Net emigration is about 0.15% of the population a year, roughly the same rate as in the 1990s,
;; and far below the more than 1% a year that left early in the twentieth century.[^t2]

(def g7-inflows
  (-> (c/data "tal_oecd_permanent_inflows_per1000.csv")
      (tc/select-rows #(and (= 2024 (:year %)) (#{"CAN" "USA" "GBR" "DEU" "FRA" "ITA" "JPN" "AUS"} (:country_iso3 %))))
      (tc/map-columns :c [:country] #(if (= % "Canada") c/red c/w))
      (tc/order-by [:permanent_inflows_per_1000])))

^{:kindly/options {:id :fig-g7-inflows
                   :caption "Permanent-type immigrant inflows per 1,000 residents, 2024, G7 and Australia (OECD standardised statistics; population from the World Bank).[^t3]"}}
(-> g7-inflows
    (pj/lay-bar :permanent_inflows_per_1000 :country {:color {:column :c :scale false}})
    (pj/lay-label {:text :permanent_inflows_per_1000})
    (pj/options {:title "Permanent Immigration per Capita, 2024"
                 :x-label "Permanent immigrants per 1,000 residents" :y-label ""}))

;; Canada takes in more permanent immigrants per person than any other G7 country, almost three times the American rate,
;; and 58% of them come through work-related streams.[^t3]
;; Recent arrivals are highly educated: 59% of immigrants aged 25–64 who landed in 2016–21 hold a university degree,
;; against 27% of the Canadian-born.
;; Immigrants and temporary residents hold 56% of the working-age doctorates in Canada.[^t4]

;; But the people who leave are highly educated too.

(def emigrant-profile
  (-> (c/data "tal_skilled_emigration_indicators.csv")
      (tc/select-rows #(and (= "emigrant_profile" (:topic %))
                            (#{"canadian_population" "emigrants"} (:group %))
                            (#{"age_20_44" "immigrants_share" "edu_bachelor" "edu_masters_or_higher" "occ_natural_applied_sciences"} (:indicator %))))
      (tc/map-columns :label [:indicator] {"age_20_44" "Aged 20–44"
                                           "immigrants_share" "Immigrants"
                                           "edu_bachelor" "Bachelor's degree"
                                           "edu_masters_or_higher" "Master's or higher"
                                           "occ_natural_applied_sciences" "Science and engineering jobs"})
      (tc/map-columns :who [:group] {"emigrants" "Recent emigrants" "canadian_population" "Canadian population"})
      (tc/map-columns :pct [:value] #(Double/parseDouble (str %)))))

^{:kindly/options {:id :fig-emigrant-profile
                   :caption "Characteristics of people who emigrated from Canada, compared with the Canadian population (Statistics Canada, 2021 Census linked to tax records).[^t5]"}}
(c/grouped-bars
 (for [[k short] [["Immigrants" "Immigrants"] ["Aged 20–44" "Aged 20–44"] ["Bachelor's degree" "Bachelor's"]
                  ["Master's or higher" "Master's+"] ["Science and engineering jobs" "Sci. & eng. jobs"]]
       who ["Canadian population" "Recent emigrants"]]
   {:x short :group who
    :value (->> (tc/rows emigrant-profile :as-maps)
                (filter #(and (= k (:label %)) (= who (:who %))))
                first :pct)})
 {:colors [c/neutral c/red] :title "Who Emigrates from Canada" :y-label "% of group"})

;; Seven in ten recent emigrants hold a university degree, and almost a third have a master's or doctorate, against 9% of Canadians.[^t5]
;; But the most striking pair is the first: **61% of emigrants are immigrants**, against 24% of the population.[^t5]
;; More than 15% of immigrants leave within 20 years of landing, and over 40% of those admitted as investors do.[^t6]
;; Much of what Canadians call brain drain is people Canada recruited moving on, the other half of the circulation that brings them here.

;; ## The Top of the Distribution

;; Averages hide where the loss is.
;; Most Canadian university graduates stay: 89% of Canadian STEM graduates of 2015–20 were still filing taxes in Canada three years after graduating,
;; and retention has been improving.[^t7]
;; The leaks are at the top.

(def stem-retention
  (-> (c/data "tal_statcan_stem_grad_retention_choi_hou_2025.csv")
      (tc/select-rows #(and (= "Canadian students" (:student_status %))
                            (= "2015-2020" (:graduation_cohort %))
                            (= 3 (:years_since_graduation %))
                            (#{"Non-STEM" "STEM total" "Computer and information science" "Mathematics and related studies"
                               "Doctoral degree" "High-ranking universities" "Other universities and colleges"} (:group %))))
      (tc/map-columns :label [:group] {"Non-STEM" "Non-STEM graduates"
                                       "STEM total" "All STEM graduates"
                                       "Computer and information science" "Computer science"
                                       "Mathematics and related studies" "Mathematics"
                                       "Doctoral degree" "STEM doctorates"
                                       "High-ranking universities" "STEM, top-ranked universities"
                                       "Other universities and colleges" "STEM, other institutions"})
      (tc/map-columns :gone [:pct_filing_tax_in_canada] #(Math/round (- 100.0 %)))
      (tc/order-by [:gone])))

(def stem-retention-plot
  (-> stem-retention
      (tc/concat (tc/dataset [{:label "CS, top-ranked universities" :gone 23}]))
      (tc/map-columns :c [:label] #(if (#{"STEM, top-ranked universities" "CS, top-ranked universities" "STEM doctorates"} %) c/red c/w))))

^{:kindly/options {:id :fig-stem-retention
                   :caption "Share of Canadian citizen and permanent-resident graduates (2015–20) who were not filing Canadian taxes three years after graduation. Not filing is a proxy for having left, so these are upper bounds. Top-ranked universities are Toronto, UBC, Waterloo, McGill, Montréal and Alberta.[^t7]"}}
(-> stem-retention-plot
    (pj/lay-bar :gone :label {:color {:column :c :scale false}})
    (pj/lay-label {:text :gone})
    (pj/options {:title "Graduates Missing from Canadian Tax Files After 3 Years"
                 :x-label "% not filing in Canada" :y-label ""}))

;; About one in nine STEM graduates is missing after three years.
;; For doctorates, and for graduates of Canada's top research universities, it is one in six;
;; for computer science graduates of those universities, almost one in four.[^t7]
;; A study of University of Waterloo graduates, which links grades to tax records, finds the same pattern:
;; Canadian-born students at the 95th percentile of grades leave at twice the rate of those at the 5th (10% versus 5%),
;; and the best leavers are the least likely to come back.[^t8]
;; The pattern is old: of 1995 graduates, 1.5% were in the United States two years later, but 12% of new PhDs were.[^t2]

;; There is one striking reversal.
;; At Waterloo, the *best* international students are the most likely to stay (28% leave at the 95th percentile versus 40% at the 5th),
;; and about 70% become permanent residents.[^t8]
;; Canada loses some of its best home-grown graduates to the United States and replaces them with some of the best graduates from everywhere else.

;; Other flows that once alarmed Canadians have faded.
;; In the early 1990s Canada lost about 400 physicians a year on net; in 2023–24 the net flow was close to zero.[^t9]
;; The AI field is the one to watch: Stanford's 2025 AI Index found Canada still gaining AI talent in 2024 (about 1 net arrival per 10,000 LinkedIn members),
;; but listed it among countries where the net inflow is declining (the 2026 edition was not checked).[^t10]

;; ## Founders

;; It's true that many of Canada's best-known founders are immigrants, and the best-known losses run both ways.
;; Tobi Lütke came from Germany in 2002 and built Shopify in Ottawa.
;; Ilya Sutskever arrived from Israel as a teenager, did three degrees at the University of Toronto, then left for Google and co-founded OpenAI:
;; an immigrant Canada trained and then lost.
;; Garrett Camp, born and educated in Calgary, co-founded Uber in San Francisco.
;; Aidan Gomez, a co-founder of Cohere, was born in Toronto and built his company there.[^t11]

;; The systematic evidence is more modest than the anecdotes.
;; Immigrant-owned firms were 17% of private incorporated firms but created 25% of net new jobs in 2003–13,
;; though the gap disappears once firm age is accounted for: immigrants start more *new* firms, and new firms grow.[^t12]
;; Immigrants are more likely to own an incorporated business than people whose families have been here for three or more generations (5.2% versus 4.0%),
;; and immigrant owners are twice as likely to hold a STEM degree.[^t12]
;; In an economy short of new firms (see [Stable and Stuck](c06_productivity.html)), that matters.
;; Canada has no official data on the birthplace of founders of venture-backed firms;
;; the widely shared claim that 42% of Canadian unicorns have immigrant co-founders has no traceable source.

;; ## How Much It Nets Out

(def degree-flows
  (tc/dataset
   [{:flow "Immigrants arriving" :people 99900 :c c/wd}
    {:flow "Emigrants leaving (gross)" :people 62000 :c c/red}
    {:flow "Canadian-born moving to US" :people 8800 :c c/o}]))

^{:kindly/options {:id :fig-degree-flows
                   :caption "Rough annual flows of university degree holders. Immigrants arriving: 2016–21 arrivals aged 25–64 with a bachelor's or higher still in Canada at the 2021 Census, per year. Emigrants leaving: average 2016–21 emigrants times the 69.8% degree share of recent emigrants; returning emigrants (about 52,000 a year, education unknown) are not subtracted. Canadian-born moving to US: 2015–24 arrivals aged 25+ with a degree, per year, from the American Community Survey. Author's calculations; see notes.[^t13]"}}
(-> degree-flows
    (pj/lay-bar :people :flow {:color {:column :c :scale false}})
    (pj/lay-label {:text :people})
    (pj/options {:title "Degree Holders In and Out, per Year"
                 :x-label "People per year" :y-label ""}))

;; On counts of degrees, Canada is still a clear net gainer.
;; In the 1990s Statistics Canada found Canada gained "four times as many university graduates from the rest of the world as it loses to the United States".[^t2]
;; Today the ratio is roughly 1.6 to 1 against gross emigration of degree holders to all destinations,
;; and closer to 4 to 1 if returning Canadians are as educated as those who leave (nobody measures this).[^t13]
;; Canadian-born degree holders settling in the United States, the classic brain drain, number about 8,800 a year,
;; about one-eleventh of the degree holders arriving.[^t13]

;; Two things make the picture less comfortable than the counts.
;; First, degrees are not skills: 26% of immigrants with a foreign degree work in jobs that need no more than high school,
;; against 11% of the Canadian-born, and only 41% of foreign-trained medical graduates work as doctors.[^t4]
;; Canada recruits talent and then fails to use much of it, which is a productivity loss of its own.
;; Second, the loss is concentrated at the very top, among PhDs, computer scientists and the best students of the best universities,
;; exactly the people most likely to found or staff the young, fast-growing firms Canada lacks.
;; No study has put a value on that loss, so the honest answer to "how much does it net out?" is:
;; **in people and degrees, strongly positive; at the frontier, a modest but real loss, mostly to the United States.**

;; ## Is Canada Really More Mobile?

;; Economists measure intergenerational mobility in several ways.
;; The most robust is the **rank–rank slope**: if parents move up ten percentiles in their country's income distribution,
;; how many percentiles higher do their children end up, on average?
;; A slope of 0 means parents' income tells you nothing about children's; a slope of 1 means children inherit their parents' rank exactly.

;; The best comparison uses tax records in both countries for children born around 1980.[^1]
;; Canada's rank–rank slope is 0.21–0.23 across five ways of measuring it; America's is 0.31–0.34.
;; A Canadian child's rank depends about a third less on their parents' rank.
;; That result is solid.

;; The figure most often quoted, though, is that 11.2% of Canadian children born in the bottom fifth reach the top fifth,
;; against 7.5% in the United States.
;; That comparison ranks each country's children against their own country's income distribution,
;; and Canada's top fifth starts at a lower income than America's.
;; Placed on the US income scale, the Canadian figure is **7.7%**, essentially the same as the American one.[^1]

(def top-fifth
  (tc/dataset
   [{:who "United States" :pct 7.5 :c c/wd}
    {:who "Canada, on the US scale" :pct 7.7 :c c/red}
    {:who "Canada, ranked within Canada" :pct 11.2 :c c/neutral}]))

^{:kindly/options {:id :fig-top-fifth
                   :caption "Share of children with bottom-fifth parents who reach the top fifth, children born 1980–82, incomes at about age 30–32. The grey bar ranks Canadian children against other Canadians, whose top fifth starts at a lower income; the red bar places them in the American income distribution (incomes converted at purchasing power parity).[^1]"}}
(-> top-fifth
    (pj/lay-bar :pct :who {:color {:column :c :scale false}})
    (pj/lay-text {:text :t :data (tc/map-columns top-fifth :t [:pct] #(str % "%"))
                  :x :pct :y :who :align-x :left :offset-x 6 :overlay true})
    (pj/options {:title "From the Bottom Fifth to the Top Fifth"
                 :x-label "% of children from the poorest fifth" :y-label "" :height 260}))

;; Measured on the same yardstick, the gap at the top disappears.
;; Canada's advantage is in the bottom half:
;; poor Canadian children are less likely to stay poor (29.1% versus 33.7%) and more likely to reach the middle.
;; In the authors' words, they are "much more likely to rise to the top half of the income distribution...
;; although not necessarily more likely to rise to the top fifth."[^1]
;; At the top, Canada is less sticky than the US at the level of fifths: on the US income scale, 24.6% of children of top-fifth parents stay there, versus 36.5% (29.5% ranked within Canada).

;; Two cautions on the comparison.
;; The Canadian children's incomes were measured in 2011–12, at the height of the commodity boom,
;; and the most mobile Canadian regions are in Alberta, Saskatchewan and Newfoundland and Labrador, which the authors flag themselves.[^1]
;; And part of Canada's advantage comes from its large second generation of immigrants:
;; children of immigrants are very mobile in both countries,
;; and one working paper puts the slope for Canadian-born children alone at about 0.29, against 0.22 for children of immigrants (different income definitions, so not directly comparable with the American 0.34).[^2]

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
;; For American children born 1971–82, the slope stayed close to 0.30, and rank-based mobility was stable through the 1993 cohort.[^4]
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
(c/dumbbell
 (for [[abbr rs] (->> (tc/rows province-slopes :as-maps) (group-by :province_abbr)
                      (sort-by (fn [[_ rs]] (- (:value (first (filter #(= 1982 (:cohort_label %)) rs)))))))
       :let [v (fn [y] (:value (first (filter #(= y (:cohort_label %)) rs))))]]
   {:label abbr :from (v 1963) :to (v 1982)})
 {:from-name "Born 1963–66" :to-name "Born 1982–85"
  :title "Mobility Fell in Almost Every Province"
  :x-label "Rank–rank slope (higher = less mobile)" :include-zero true
  :fmt #(format "%.2f" (double %)) :height 440})

;; Mobility fell in every province and territory except Yukon, where it rose, and Prince Edward Island, where it was unchanged (0.16).
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
;; Among sons of the top 1% of fathers, one study found an income elasticity of "almost 0.8",
;; compared with close to zero at the bottom;[^7] newer estimates using lifetime earnings put persistence among the top 5% at about 0.45.[^11]
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
;; | Indigenous children, Manitoba, the North | **No** | Indigenous children: mobility "about two times lower"; Manitoba and the North: slopes 0.32–0.35 vs 0.23 nationally |
;; | Very top | **Strong persistence** | elasticity about 0.45 (top 5%, lifetime) to 0.8 (top 1%, older study) |
;;
;; : How well Canada's mobility advantage holds up {#tbl-mobility-verdict}

;; Canada is better than the United States at keeping poor children from staying poor.
;; It is not better at giving them a shot at the top, and its advantage is eroding as inequality among parents grows.
;; The OECD estimates it would take about four generations for a family in Canada's bottom tenth to reach average income,
;; versus five in the United States and two or three in the Nordic countries.[^12]

;; ## Putting It Together

;; Both halves of the story point the same way.
;; Canada is good at the broad middle: it imports educated people at a scale no other G7 country matches,
;; keeps most of the graduates it trains, and keeps more poor children out of poverty than the United States does.
;; It is weaker at the frontier: its best graduates are the likeliest to leave, its poor children are no likelier than American ones to reach the top,
;; and at the very top, advantage passes strongly from parent to child.
;; That is the same shape as the economy described in [Stable and Stuck](c06_productivity.html):
;; a good floor, a low ceiling, and the top positions held by the same families and firms.

;; The policy lessons follow from where the losses are.
;; Using the skills of immigrants already here (credential recognition, especially in health care) would add more talent than any plausible cut in emigration.
;; Keeping top graduates means having more of the young, high-growth firms that employ them,
;; which brings the argument back to competition and capital in [The Investment Question](c09_investment_question.html).
;; And protecting mobility means taking the trends seriously: Canada's advantage was built for children born in the 1960s, and it has been eroding since.

;; In the next part, [Scorecard](c13_scorecard.html), we look at what the current government has done so far, and what the data show.

;; [^t1]: Statistics Canada, [Table 17-10-0040-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1710004001), components of population change, quarterly estimates summed to calendar years; population from [Table 17-10-0009-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1710000901). From July 2016 short-term emigrants are included in the emigrant counts, so gross emigration is not comparable across that date; net emigration is.
;; [^t2]: Zhao, Drew and Murray, ["Knowledge workers on the move"](https://www150.statcan.gc.ca/n1/pub/75-001-x/2000002/article/5072-eng.pdf), Statistics Canada, *Perspectives on Labour and Income*, Summer 2000. See also David Card, ["Canadian Emigration to the United States"](https://eml.berkeley.edu/~card/papers/canadian-emigrants.pdf), 2003.
;; [^t3]: OECD, International Migration Database, [standardised inflows of permanent-type migrants](https://sdmx.oecd.org/public/rest/data/OECD.ELS.IMD,DSD_MIG_INT@DF_MIG_INT_PER,1.0/all); World Bank population (SP.POP.TOTL). Per-1,000 rates and work shares (work plus accompanying family) are author's calculations.
;; [^t4]: Statistics Canada, 2021 Census, [Table 98-10-0446-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=9810044601) (ages 25–64, private households), and [*The Daily*, November 30, 2022](https://www150.statcan.gc.ca/n1/daily-quotidien/221130/dq221130a-eng.htm).
;; [^t5]: Statistics Canada, ["Portrait of Canadian emigration"](https://www150.statcan.gc.ca/n1/pub/11-627-m/11-627-m2026013-eng.htm), 11-627-M, March 2026 (2021 Census linked to tax records). The degree share is the sum of bachelor's (38.7%) and master's or higher (31.1%).
;; [^t6]: Statistics Canada, [*The Daily*, February 2, 2024](https://www150.statcan.gc.ca/n1/daily-quotidien/240202/dq240202a-eng.htm), emigration of immigrants admitted 1982–2017 (Longitudinal Immigration Database).
;; [^t7]: Youjin Choi and Feng Hou, ["Retention of science, technology, engineering, mathematics and computer science graduates in Canada"](https://www150.statcan.gc.ca/n1/pub/36-28-0001/2025007/article/00004-eng.htm), Statistics Canada, *Economic and Social Reports* 5(7), July 2025, Table 1 and text (the computer science figures are in the text). "High-ranking" means in the 2025 Times Higher Education top 100 for engineering, computer science or physical sciences.
;; [^t8]: Joel Blit, Mikal Skuterud and Ruiwen Zhang, ["The Potential of Canada's International Education Strategy: Evidence from the 'MIT of the North'"](https://clef.uwaterloo.ca/wp-content/uploads/2024/09/CLEF-074-2024.pdf), CLEF Working Paper 74, 2024 (forthcoming in *Canadian Public Policy*). Leaving means three consecutive years without filing Canadian taxes.
;; [^t9]: Canadian Institute for Health Information, [*Supply, Distribution and Migration of Physicians in Canada, 2024* data tables](https://www.cihi.ca/sites/default/files/document/supply-distribution-migration-physicians-in-canada-2024-data-tables-en.xlsx) (net −35 in 2024 and +14 in 2023; likely undercounted since 2017); Watanabe, Comeau and Buske, ["Analysis of International Migration Patterns Affecting Physician Supply in Canada"](https://www.longwoods.com/content/19899), *Healthcare Policy* 3(4), 2008.
;; [^t10]: Stanford HAI, [*AI Index Report 2025*, Chapter 4](https://hai.stanford.edu/assets/files/hai_ai-index-report-2025_chapter4_final.pdf), Figures 4.2.22–4.2.23 (LinkedIn data).
;; [^t11]: Biographical details from public biographies: [Tobias Lütke](https://en.wikipedia.org/wiki/Tobias_L%C3%BCtke), [Ilya Sutskever](https://en.wikipedia.org/wiki/Ilya_Sutskever), [Garrett Camp](https://en.wikipedia.org/wiki/Garrett_Camp), [Aidan Gomez](https://en.wikipedia.org/wiki/Aidan_Gomez). These are illustrations, not data.
;; [^t12]: Garnett Picot and Anne-Marie Rollin, ["Immigrant Entrepreneurs as Job Creators: The Case of Canadian Private Incorporated Companies"](https://www150.statcan.gc.ca/n1/pub/11f0019m/11f0019m2019011-eng.htm), Statistics Canada 11F0019M, 2019; Garnett Picot and Yuri Ostrovsky, ["Immigrant entrepreneurs in Canada: Highlights from recent studies"](https://www150.statcan.gc.ca/n1/pub/36-28-0001/2021009/article/00001-eng.htm), *Economic and Social Reports* 1(9), 2021.
;; [^t13]: Author's calculations from Statistics Canada Census 2021 Table 98-10-0446-01, Table 17-10-0040-01 and the 2026 emigration portrait, and from US Census Bureau [American Community Survey 1-year microdata, 2024](https://www2.census.gov/programs-surveys/acs/data/pums/2024/1-Year/) (people born in Canada; weighted). Derivations are in `data/tal_net_skill_balance_estimates.csv` and `data-prep/talent/NOTES.md`.

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
