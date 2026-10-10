^:kindly/hide-code
(ns economics.wealth-distribution.canada.c10-land
  {:kindly/options {:static true
                    :kinds-that-hide-code #{:kind/var :kind/hiccup2 :kind/fn}}
   :clay {:title  "Unbounded North: Land"
          :quarto {:sidebar     "unbounded-north"
                   :author      "Kira Howe"
                   :description "How much of Canada's wealth is land, what Canada learned from taxing land a century ago, and what a modern land value tax could do."
                   :abstract    "Land is now over a third of the value of everything Canada has built and owns, and nearly two times GDP. Taxing land rather than buildings is the least growth-damaging tax economists know of, and Western Canada tried it a century ago."
                   :date        "2026-10-20"
                   :type        :post
                   :category    :economics
                   :tags        [:tax :economics :growth :canada :housing :land]}}}
  (:require [scicloj.plotje.api :as pj]
            [tablecloth.api :as tc]
            [economics.wealth-distribution.canada.common :as c]))

;; ## The Most Valuable Thing Nobody Made

;; Nobody built the land under a Toronto house.
;; Its value comes from what surrounds it: transit, schools, jobs, parks, neighbours, and the city's decision about what may be built there.
;; When land prices rise, owners gain wealth they did nothing to create.
;; In the 1870s the American economist Henry George argued that this is the fairest and least harmful thing to tax.
;; Economists across the spectrum have agreed with him ever since, even if governments rarely have.

;; In Canada, this matters more than almost anywhere.

(def land
  (-> (c/data "land_land_share_summary.csv")
      (tc/select-rows #(and (= "national" (:sector %)) (re-find #"Q4$" (:period %))))
      (tc/map-columns :year [:period] #(parse-long (subs % 0 4)))))

^{:kindly/options {:id :fig-land-share
                   :caption "Value of all land in Canada as a share of the country's non-financial assets (buildings, machinery, inventories, intellectual property and land), fourth quarter of each year (Statistics Canada National Balance Sheet Accounts).[^1]"}}
(-> land
    (pj/lay-line :year :land_share_nfa_pct {:color c/o :size 2})
    (pj/scale :y {:include 0})
    (pj/options {:title "Land as a Share of Canada's Non-Financial Assets"
                 :x-label "Year" :y-label "%"}))

;; Land was a quarter of the value of Canada's non-financial assets in 2000.
;; It peaked at 42% in late 2021 and was 36.5% at the end of 2025: about \$6.4 trillion, nearly twice the size of the
;; whole economy's annual output.[^1]
;; Machinery and equipment, by comparison, are about 3%.

(def residential
  (-> land
      (tc/select-columns [:year :land_under_dwellings :dwellings_structures])
      (tc/pivot->longer [:land_under_dwellings :dwellings_structures] {:target-columns :part :value-column-name :value})
      (tc/update-columns {:part (partial map {:land_under_dwellings "Land under homes" :dwellings_structures "The homes themselves"})})
      (tc/map-columns :value-t [:value] #(/ % 1e6))))

^{:kindly/options {:id :fig-land-vs-structures
                   :caption "Market value of residential structures and of the land beneath them, all sectors, C$ trillions (Statistics Canada). Statistics Canada estimates residential land using a land-to-structure ratio it describes as dated.[^1]"}}
(-> residential
    (pj/lay-line :year :value-t {:color :part :size 2})
    (pj/scale :color {:values [c/o c/w]})
    (pj/options {:title "What Canadian Homes Are Worth: Land vs Buildings"
                 :x-label "Year" :y-label "C$ trillions"
                 :color-label "" :legend-position :bottom}))

;; The land under Canada's homes is now worth about as much as the homes themselves (\$4.5 trillion each),
;; and it was worth considerably more at the 2021–22 peak.
;; From 2010 to 2021, the value of land under homes rose about 225%; the value of the buildings, about 100%.[^1]
;; **Most of the Canadian housing boom was a land boom.**

;; This is the link between the earlier parts of this series.
;; Land is where Canadian middle-class wealth sits ([Wealth Concentration](c05_wealth_concentration.html)),
;; why young people can't buy homes ([Labour Hours](c03_labour_hours.html)),
;; and where Canadian capital goes instead of into productive business ([Stable and Stuck](c06_productivity.html)).
;; Rising land values make owners richer without making the economy more productive.

;; ## How Canada Taxes Land Today

;; Canadian municipalities already tax property: \$66B in 2024, about 2.1% of GDP and roughly 43% of local government revenue.[^2]
;; But property tax falls on land *and* buildings together.
;; About half of residential property value is buildings, so roughly half of residential property tax is a tax on having built something.
;; A vacant lot pays less than the apartment building that could replace it.
;; Meanwhile the federal government exempts gains on a principal residence from capital gains tax entirely,
;; at a cost of about \$7.6B a year (see [Who Pays Tax](c01_who_pays_tax.html)).

;; Effective rates also vary in odd ways.
;; In 2025, Toronto's residential tax rate was 0.75% of assessed value, while neighbouring Mississauga's was 1.03% and Brampton's 1.20%;
;; and Ontario assessments are still based on 2016 values.[^3]
;; Vancouver's effective rate on a typical house is about 0.3%.
;; The places with the most expensive land tax it most lightly.

;; ## Canada Tried This Before

;; A century ago, Western Canada was the world's leading experiment in land value taxation.
;; Cities exempted buildings from tax, partly or entirely, and taxed land instead, to encourage building on vacant lots.

(def history
  (-> (c/data "land_canada_land_tax_history.csv")
      (tc/select-rows #(and (#{"Vancouver" "Calgary" "Edmonton"} (:place %))
                            (<= (:year %) 1920)
                            (number? (:improvements_assessed_pct_of_value %))))))

^{:kindly/options {:id :fig-single-tax-history
                   :caption "Share of building value included in the municipal property tax base, Vancouver, Calgary and Edmonton. 0% means buildings were untaxed and only land was taxed. Vancouver later raised the share again and ended the exemption in 1984. Sources: Haig (1915) field report; England (2018).[^4]"}}
(-> history
    (pj/lay-step :year :improvements_assessed_pct_of_value {:color :place :size 2})
    (pj/lay-point {:color :place})
    (pj/scale :color {:values [c/o c/wd c/green]})
    (pj/scale :y {:domain [0 100]})
    (pj/options {:title "Taxing Land, Not Buildings: Western Canada, 1891–1920"
                 :x-label "Year" :y-label "% of building value taxed"
                 :color-label "" :legend-position :bottom}))

;; * **Vancouver** taxed buildings at half their value in 1895, a quarter in 1906, and **not at all from 1910**.
;; * **Edmonton** exempted buildings entirely from its incorporation as a city in 1904.
;; * **Calgary** cut the tax on buildings to 25% of value by 1912.
;; * **Winnipeg** taxed buildings at two-thirds of value; Saskatchewan let towns choose, and Moose Jaw cut building assessments to 45%.
;; * Several towns and provinces added "super-taxes" on vacant land and "wild land" taxes on speculative holdings.[^4]

;; An American economist, Robert Murray Haig, toured these cities in 1914 for New York City's tax commission.
;; His verdict: the exemption of improvements was "neither a plague nor a panacea."
;; It worked smoothly while land values were rising.
;; When the western land boom collapsed in 1913, a tax base made only of land shrank, arrears piled up (by early 1915,
;; Edmonton had collected little more than half of its 1914 taxes) and cities began taxing buildings again.[^4]
;; Vancouver scaled its exemption back over decades and ended it in 1984.
;; A historian of the episode attributes the retreat to politics: organised property owners outlasted tenants and future buyers.[^5]

;; The lessons are useful.
;; A land tax is easiest to introduce when land values are rising (it captures gains owners haven't yet banked),
;; and it needs a stable, broad base, which is an argument for a national or provincial tax rather than a city-by-city one.

;; ## Today's Vacancy Taxes

;; Canada's recent experiments target empty homes rather than land value.

;; | Tax | Design | Results |
;; | :--- | :--- | :--- |
;; | Vancouver Empty Homes Tax (2017–) | 1% of assessed value on empty homes, 3% from 2021 | Vacant homes fell from about 2,500 (2017) to 979 (2024); about \$194M raised for housing |
;; | BC Speculation and Vacancy Tax (2018–) | 0.5–2% on homes left vacant, rising to 1–3% in 2026 | About \$550M raised 2018–2024; taxed properties fell from 8,920 to about 6,500 by 2023 |
;; | Toronto Vacant Home Tax (2022–) | 1%, then 3% from 2024 | \$56.5M billed for 2022; a 2024 rollout with 169,000 complaints from owners wrongly deemed vacant |
;; | Federal Underused Housing Tax (2022–2024) | 1% on vacant homes owned by non-residents | Projected \$700M over four years; about \$49M assessed by 2024, \$59M spent administering it; repealed from 2025 |
;;
;; : Canadian vacancy taxes {#tbl-vacancy}[^6]

;; The pattern is consistent.
;; Well-targeted local taxes reduced vacancy.
;; A C.D. Howe Institute study of Vancouver found vacancy fell by about 1.5 percentage points but saw no measurable effect on rents or construction.[^6]
;; The federal tax was a cautionary tale: a narrow base, complex filing rules that caught ordinary Canadians,
;; and revenue far below projections.
;; Vacancy taxes chase a symptom.
;; Empty homes are a small part of the problem; the large part is the price of the land under occupied ones.

;; ## A Modern Land Value Tax

;; The international evidence on land and property taxes is unusually consistent:

;; * The OECD ranks recurrent taxes on immovable property as the **least harmful to growth** of all major taxes,
;;   and recommends countries rely on them more, based on regularly updated values.[^7]
;; * The IMF calls property taxes "generally considered to be underutilized," with "relatively low efficiency costs, benign impact on growth,
;;   and high score on fairness."[^8]
;; * The UK's Mirrlees Review called the economic case for a land value tax "simple, and almost undeniable."[^9]
;; * Where cities have taxed land more heavily than buildings (Pittsburgh and other Pennsylvania cities), studies find more construction:
;;   residential building rose about 3–4% in a broader study of Pennsylvania split-rate cities.[^10]
;; * The Australian Capital Territory is replacing its taxes on home sales with taxes on unimproved land value over 20 years.[^11]

;; What could it raise in Canada?
;; The figures below are illustrative calculations from Statistics Canada's land values, not costed proposals.

(def scenarios
  (-> (c/data "land_lvt_illustrative_scenarios.csv")
      (tc/select-rows #(#{"A" "C_100k" "D_r4" "E"} (:scenario %)))
      (tc/map-columns :label [:scenario]
                      {"A" "1% on land under homes"
                       "C_100k" "…with a $100K exemption per home"
                       "D_r4" "…after land prices adjust (−20%)"
                       "E" "1% on all land"})
      (tc/map-columns :rev-b [:revenue_cad_millions_per_year] #(/ % 1000.0))
      (tc/map-columns :text [:revenue_cad_millions_per_year] #(c/money (* 1e6 %)))
      (tc/order-by [:rev-b])))

^{:kindly/options {:id :fig-lvt-scenarios
                   :caption "Illustrative annual revenue from a 1% tax on land value, based on Statistics Canada land values at the end of 2025. The exemption scenario is a lower bound; the price-adjustment scenario assumes land prices fall to capitalise the tax (4% discount rate). No behavioural or administrative costs included.[^12]"}}
(-> scenarios
    (pj/lay-bar :rev-b :label {:color c/o})
    (pj/lay-label {:text :text})
    (pj/options {:title "What a 1% Land Value Tax Could Raise"
                 :x-label "C$ billions per year" :y-label ""}))

;; A 1% annual tax on the land under Canada's homes would raise about \$45B a year before any exemption,
;; and at least \$30B with a \$100,000 exemption per home.
;; Even after land prices fell to reflect the tax (which is the point), it would raise around \$36B.
;; Extending it to all land, including commercial and agricultural land, gives about \$64B.[^12]
;; For comparison, the PBO's estimate for a wealth tax on fortunes above \$10M is about \$23B a year (see [Wealth Tax](c02_wealth_tax.html)).

;; ### The Hard Parts

;; A land value tax is not free of problems, and the honest version of the case admits them.

;; 1. **Valuation.** Land must be valued separately from buildings, and in built-up cities land rarely sells on its own.
;;    Denmark and parts of Australia do it routinely, and BC Assessment already reports separate land and building values for every property in British Columbia.
;; 2. **Cash-poor owners.** Seniors who bought decades ago may be asset-rich and income-poor.
;;    The OECD recommends letting them defer the tax until the home is sold, with the deferred amount secured on the property.
;; 3. **Transition.** A new land tax lowers land prices, a one-time loss for current owners, many of whom stretched to buy.
;;    Phasing it in gradually, and pairing it with cuts to taxes on buildings, land transfer taxes and income taxes on work, spreads the burden.
;; 4. **Politics.** Vancouver's experience shows how organised owners can erode a land tax over time.
;;    A land tax has to be designed to survive the next bust and the next election.

;; ::: {.callout-note}
;; ## A Closer Look: Swapping Taxes, Not Adding Them

;; A land value tax works best as a *replacement*.
;; Land transfer taxes discourage people from moving to where the jobs are; taxes on buildings discourage building;
;; the principal residence exemption rewards owning the most valuable land.
;; Shifting municipal property tax from buildings to land, phasing down land transfer taxes, and capping the principal residence
;; exemption, could make the tax system both more efficient and more progressive,
;; and reward building homes rather than holding land.
;; :::

;; ## Conclusion

;; Land is the largest and fastest-growing store of Canadian wealth, and the least productive.
;; Its value is created by the community, captured by owners, and lightly taxed.
;; Western Canada once led the world in taxing land instead of buildings; the experiment faded after the 1913 crash and decades of pressure from owners.
;; Economists still regard taxing land as the least harmful tax there is.
;; A gradual shift from taxing buildings, transactions and work towards taxing land would tackle housing affordability,
;; wealth concentration and Canada's investment problem at the same time.

;; In the next part, [What Makes Canada Different](c11_canadian_differences.html), we collect the features that set Canada apart.

;; [^1]: Statistics Canada, [Table 36-10-0580-01, National Balance Sheet Accounts](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610058001), market value, all sectors; GDP from [Table 36-10-0104-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610010401). Statistics Canada values residential land using a land-to-structure ratio applied to structure values.
;; [^2]: Statistics Canada, [Tables 10-10-0020-01 and 10-10-0147-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1010014701), government finance statistics, recurrent taxes on immovable property.
;; [^3]: City of Toronto, [property tax rates](https://www.toronto.ca/services-payments/property-taxes-utilities/property-tax/property-tax-rates-and-fees/); Region of Peel, [2025 tax rates](https://peelregion.ca/sites/default/files/2025-07/Appendix-1-%202025-tax-rate_0.pdf). Ontario assessments use a January 1, 2016 valuation date following the postponement of reassessment.
;; [^4]: Robert Murray Haig, [*The Exemption of Improvements from Taxation in Canada and the United States*](https://archive.org/details/exemptionofimpro00haiguoft), report to the Committee on Taxation of the City of New York, 1915.
;; [^5]: Richard W. England, "Vancouver's Experiment with Land Value Taxation" (2018), *American Journal of Economics and Sociology*.
;; [^6]: Vancouver Empty Homes Tax annual reports (as reported by the City and by the City of Toronto's staff reports); BC Ministry of Finance, Speculation and Vacancy Tax reports; City of Toronto, Vacant Home Tax reports to council; Office of the Parliamentary Budget Officer, [Estimated Revenues from the Underused Housing Tax](https://pbo-dpb.ca/en/publications/LEG-2425-001-S--estimated-revenues-from-underused-housing-tax--estimation-recettes-tirees-taxe-logements-sous-utilises); [Budget 2021](https://www.budget.canada.ca/2021/report-rapport/p4-en.html); [Bill C-15](https://www.parl.ca/DocumentViewer/en/45-1/bill/C-15/royal-assent); Caracciolo and Miglino, ["Ripple Effects: The Impact of the Empty Homes Tax on the Housing Market"](https://cdhowe.org/publication/ripple-effects-impact-empty-homes-tax-housing-market/), C.D. Howe Institute, 2024 (think tank). The federal assessed amount and administration cost are from press reports of CRA figures. Full details in `data/land_vacancy_taxes.csv`.
;; [^7]: Åsa Johansson et al., "Tax and Economic Growth," OECD Economics Department Working Paper No. 620, 2008; OECD, [*Housing Taxation in OECD Countries*](https://www.oecd.org/tax/tax-policy/brochure-housing-taxation-in-oecd-countries.pdf), Tax Policy Studies No. 29, 2022.
;; [^8]: John Norregaard, "Taxing Immovable Property: Revenue Potential and Implementation Challenges," IMF Working Paper 13/129, 2013.
;; [^9]: James Mirrlees et al., *Tax by Design* (Mirrlees Review), Institute for Fiscal Studies, 2011, chapter 16.
;; [^10]: Wallace Oates and Robert Schwab, "The Impact of Urban Land Taxation: The Pittsburgh Experience," *National Tax Journal* 50(1), 1997; Florenz Plassmann and Nicolaus Tideman, "A Markov Chain Monte Carlo Analysis of the Effect of Two-Rate Property Taxes on Construction," *Journal of Urban Economics* 47(2), 2000; see Dye and England, [*Assessing the Theory and Practice of Land Value Taxation*](https://www.lincolninst.edu/app/uploads/legacy-files/pubfiles/assessing-theory-practice-land-value-taxation-full_0.pdf), Lincoln Institute of Land Policy, 2010.
;; [^11]: ACT Treasury, ACT Tax Reform program (since 2012).
;; [^12]: Calculated from Statistics Canada Table 36-10-0580-01 (land under dwellings and all land, end of 2025) and 14.98 million occupied dwellings. Details in `data/land_lvt_illustrative_scenarios.csv`.
