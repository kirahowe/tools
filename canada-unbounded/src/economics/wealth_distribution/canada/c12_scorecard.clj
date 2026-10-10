^:kindly/hide-code
(ns economics.wealth-distribution.canada.c12-scorecard
  {:kindly/options {:static true
                    :kinds-that-hide-code #{:kind/var :kind/hiccup2 :kind/hiccup :kind/fn}}
   :clay {:title  "Unbounded North: Scorecard"
          :quarto {:sidebar     "unbounded-north"
                   :author      "Kira Howe"
                   :description "What the Carney government has announced, what it has enacted, and what the data show so far."
                   :abstract    "Eighteen months in, most of the government's structural agenda has only just become law. The early numbers are dominated by US tariffs. Here is what can and can't be measured yet."
                   :date        "2026-10-22"
                   :type        :post
                   :category    :economics
                   :tags        [:economics :growth :canada :policy]}}}
  (:require [clojure.string :as str]
            [scicloj.plotje.api :as pj]
            [tablecloth.api :as tc]
            [economics.wealth-distribution.canada.common :as c]))

;; ## How to Read a Scorecard Early

;; Mark Carney became Prime Minister on March 14, 2025, and won a mandate on April 28, 2025.
;; His government has framed its agenda around exactly the problem this series keeps returning to:
;; Canada's weak productivity and investment, and its dependence on the United States.
;; This part tracks what it has done so far, and what the data show.

;; Three cautions before the numbers:

;; 1. **It's early.** Most structural measures only became law in 2026: the main budget bill (C-15) in March,
;;    the Build Canada Homes Act in June. Effects on investment, productivity and housing take years.
;; 2. **Tariffs dominate.** The data since early 2025 are driven by US tariffs, a 2026 oil-price shock, and gold prices,
;;    none of which the government controls.
;; 3. **Not everything is the government's.** The Bank of Canada sets interest rates independently,
;;    and the slowdown in population growth began with immigration changes made in 2024.

;; So this is a scorecard of *commitments and early indicators*, not a verdict.[^1]

(def carney-start (java.time.LocalDate/of 2025 3 14))

;; ## What Was Done

;; | Area | Measure | Status (October 2026) |
;; | :--- | :--- | :--- |
;; | Tax | Consumer carbon price set to zero (April 1, 2025) | In force; repealed in law March 2026 (C-4) |
;; | Tax | Cancelled the 2024 capital gains inclusion increase | Done (March 2025) |
;; | Tax | Lowest income tax rate cut from 15% to 14% | In force; PBO: \$28B over 5 years, about \$110–200 per filer a year |
;; | Tax | Digital Services Tax and Underused Housing Tax repealed; luxury tax on boats and planes ended | In force (C-15) |
;; | Investment | "Productivity Super-Deduction": faster write-offs for new investment; bigger R&D credits | Mostly in force (C-15); remainder in Parliament |
;; | Internal trade | One Canadian Economy Act (C-5): federal internal trade barriers and labour-mobility rules; fast-track for "national interest" projects | In force June 2025; all 53 federal exceptions to the internal trade agreement removed; **no project yet formally designated** |
;; | Housing | Build Canada Homes agency; GST rebate for first-time buyers of new homes; development charge deals | In force 2026; no unit counts published yet |
;; | Defence | NATO 2% of GDP in 2025–26; pledge of 5% (3.5% core) by 2035 | 2% met on NATO's estimates |
;; | Fiscal | Separate capital and operating budgets; spending review; about 40,000 fewer public service positions | Ongoing |
;; | Immigration | Lower levels: temporary residents under 5% of population by end-2027 | Ongoing |
;; | Competition | Open banking law; Competition Act amendments; non-compete ban for federally regulated employers | Open banking in force; non-compete ban in Parliament |
;; | Trade | Most counter-tariffs removed (September 2025); new surtaxes after talks broke down (September 2026); goal to double non-US exports in a decade | Ongoing |
;;
;; : Major federal measures since March 2025 {#tbl-policy}[^2]

;; ## Targets vs Actuals

;; Where the government set a measurable goal, here is where things stand.

;; | Goal | Target | Latest | Assessment |
;; | :--- | :--- | :--- | :--- |
;; | Defence spending | 2.0% of GDP in 2025 | 2.03% (2025), 2.13% (2026), NATO estimates | ✅ Met |
;; | 2025–26 deficit | –\$78.3B (Budget 2025) | –\$55.3B preliminary (PBO projected –\$72.0B) | ✅ Better than planned, preliminary; helped by oil revenues |
;; | Public service size | About 40,000 fewer positions by 2028–29 | 342,000 FTEs (June 2026), down from 367,000 peak (June 2024) | ↗ Moving; decline began before Budget 2025 |
;; | Temporary residents | Under 5% of population by end-2027 | 6.65% (July 2026), from a 7.6% peak | ↗ Moving |
;; | Non-US exports | Double in a decade | Non-US goods exports +17.5% in 2025 | ↗ Ahead of pace, but driven by gold and oil prices |
;; | Housing | "Double the pace" of homebuilding (about 500,000 starts a year) | 259,000 starts in 2025; about 244,000 a year in 2026 so far | ⚠️ Off pace |
;; | Build Canada Homes | — | PBO: about 26,000 extra homes over five years | ⏳ Too early |
;; | National-interest projects | Fast-track approvals | None formally designated | ⏳ Too early |
;; | Productivity | No numeric target | Business labour productivity roughly flat | ⏳ Too early |
;;
;; : Government goals and outcomes {#tbl-targets}[^3]

;; ## The Indicators

;; Each chart marks March 14, 2025 with a dashed line.
;; Where there is a target, it is shown in orange.

(def quarterly (c/data "sc_ind_gdp_investment_quarterly.csv"))
(def productivity (c/data "sc_ind_productivity_quarterly.csv"))
(def labour (c/data "sc_ind_labour_monthly.csv"))
(def prices (c/data "sc_ind_prices_monthly.csv"))
(def housing (c/data "sc_ind_housing_monthly.csv"))
(def trade (c/data "sc_ind_trade_monthly.csv"))
(def npr (c/data "sc_ind_npr_quarterly.csv"))

(defn series [ds indicator]
  (-> ds
      (tc/select-rows #(= indicator (:indicator %)))
      (tc/map-columns :date [:date] #(if (string? %) (java.time.LocalDate/parse %) %))))

(defn small-chart
  [ds {:keys [title y-label target target-label y-domain scale color]}]
  (cond-> (-> ds
              (pj/lay-line :date :value {:color (or color c/wd) :size 2})
              (pj/lay-rule-v {:x-intercept carney-start :stroke-dash :dashed}))
    target   (pj/lay-rule-h {:y-intercept target :color c/od :stroke-dash :dotted})
    y-domain (pj/scale :y {:domain y-domain})
    scale    (pj/scale :y {:include 0})
    true     (pj/options {:title title :x-label "" :y-label y-label :width 340 :height 250})
    true     (pj/plot)))

^:kind/hiccup
[:div {:style "display:grid;grid-template-columns:repeat(auto-fill,minmax(330px,1fr));gap:10px"}
 (small-chart (series quarterly "real_gdp_per_capita")
              {:title "Real GDP per person" :y-label "chained 2017 $"})
 (small-chart (series productivity "business_labour_productivity_index")
              {:title "Business labour productivity" :y-label "index, 2017 = 100"})
 (small-chart (series quarterly "real_business_machinery_equipment")
              {:title "Machinery & equipment investment" :y-label "C$ millions, real, SAAR"})
 (small-chart (series labour "unemployment_rate")
              {:title "Unemployment rate" :y-label "%" :scale true})
 (small-chart (series prices "cpi_all_items_yoy")
              {:title "Inflation (CPI, year over year)" :y-label "%" :target 2.0})
 (small-chart (series housing "housing_starts_saar_6mo_avg")
              {:title "Housing starts (6-month average)" :y-label "units a year" :target 500000 :y-domain [0 520000]})
 (small-chart (series npr "npr_share_of_population")
              {:title "Temporary residents, % of population" :y-label "%" :target 5.0 :scale true})
 (small-chart (series trade "us_share_of_goods_exports_12mo")
              {:title "Share of goods exports going to the US" :y-label "%, 12-month" :y-domain [60 80]})]

;; *Indicators since 2022. Dashed vertical line: March 14, 2025. Orange dotted line: target
;; (Bank of Canada's 2% inflation target; "double the pace" housing goal of about 500,000 starts; 5% temporary resident goal).*[^4]

;; What stands out:

;; * **Growth and productivity are flat.** Real GDP was only 1.1% higher in mid-2026 than a year earlier, with two contracting quarters in 2025.
;;   GDP per person and business productivity have barely moved.
;;   That is no worse than the previous decade's trend, and no better.
;; * **Investment took a hit, then recovered.** Machinery and equipment investment fell 4.5–6.4% year over year through 2025 as tariff uncertainty hit,
;;   and was up 6.3% in the second quarter of 2026.
;;   That is too early to attribute to the super-deduction, which became law in March 2026.
;; * **Unemployment rose and partly recovered**: from 6.7% at the start of 2025 to a peak of 7.1% (August–September 2025), then 6.5% in September 2026.
;; * **Inflation picked up in 2026** because of the Middle East oil shock; inflation excluding food and energy has stayed near 2%.
;; * **Housing starts are flat**, at roughly half the pace the government promised.
;; * **Population growth has nearly stopped** as temporary residents leave, which mechanically lowers GDP growth and raises GDP per person.
;; * **Trade has started to diversify.** The US share of goods exports fell from about 76% in 2024 to 68% over the 12 months to August 2026,
;;   but much of the non-US growth is gold sold to the UK and higher oil prices.

;; ## Deficit: Projection vs Outcome

(def fiscal
  (-> (c/data "sc_fiscal_projections_vs_results.csv")
      (tc/select-rows #(and (= "2025-26" (:fiscal_year %)) (= "budgetary_balance" (:indicator %))))
      (tc/map-columns :label [:vintage]
                      #(-> %
                           (str/replace "FES 2024 (as reported in Budget 2025 Table 1)" "Fall 2024 statement (previous govt)")
                           (str/replace " (Apr 2025-Mar 2026)" " (preliminary)")
                           (str/replace "PBO Economic and Fiscal Outlook June 2026" "PBO (June 2026)")
                           (str/replace "Fiscal Monitor March 2026" "Fiscal Monitor")))
      (tc/map-columns :text [:value] #(format "–$%.1fB" (abs %)))))

^{:kindly/options {:id :fig-deficit
                   :caption "Federal budgetary balance for 2025–26, by forecast vintage and the preliminary unaudited result (Finance Canada; Office of the Parliamentary Budget Officer).[^5]"}}
(-> fiscal
    (pj/lay-bar :value :label {:color c/red})
    (pj/lay-label {:text :text})
    (pj/options {:title "2025–26 Federal Deficit: Forecasts and Result"
                 :x-label "C$ billions" :y-label ""}))

;; The 2025–26 deficit is likely to come in well below Budget 2025's –\$78.3B forecast:
;; the preliminary, unaudited figure is –\$55.3B.
;; The improvement is mostly revenue: higher oil prices raised corporate taxes and royalties.
;; The PBO considers the government's definition of "capital" spending (which it excludes from its operating-balance goal)
;; "overly expansive," about \$94B larger than the PBO's own over six years, and recommends an independent body classify it.[^5]

;; ## Defence

(def defence
  (-> (c/data "sc_defence_spending.csv")
      (tc/select-rows #(= "core_defence_pct_gdp" (:indicator %)))
      (tc/map-columns :yr [:year] str)))

^{:kindly/options {:id :fig-defence
                   :caption "Canada's core defence spending as a share of GDP, NATO definition (2025 and 2026 are estimates). The NATO 2% guideline is shown in orange.[^6]"}}
(-> defence
    (pj/lay-bar :yr :value {:color c/wd})
    (pj/lay-rule-h {:y-intercept 2.0 :color c/od :stroke-dash :dotted})
    (pj/options {:title "Defence Spending, % of GDP"
                 :x-label "Year" :y-label "% of GDP"}))

;; Defence spending jumped from 1.41% of GDP in 2024 to about 2.03% in 2025, meeting NATO's 2% guideline for the first time in decades.
;; The pledge of 3.5% core spending by 2035 is far larger: the PBO estimates it needs about \$33.5B a year more and would add \$63B to the 2035–36 deficit.[^6]

;; ## Through the Lens of This Series

;; How does the agenda line up with the problems the earlier parts identified?

;; | Problem identified in this series | What the government has done | Assessment |
;; | :--- | :--- | :--- |
;; | Weak competition, protected incumbents ([Stable and Stuck](c06_productivity.html)) | Open banking; Competition Act amendments; non-compete ban (pending); internal trade barrier removal | Real steps, but none yet targets the concentrated sectors (banking, telecom, groceries) directly |
;; | Low business investment ([The Investment Question](c09_investment_question.html)) | Productivity Super-Deduction: faster write-offs for *new* investment | Consistent with the evidence: targeted at new investment, not owners. PBO calls it largely a "revenue deferral" (\$19.4B over five years) |
;; | Capital gains taxed lightly ([Who Pays Tax](c01_who_pays_tax.html)) | Cancelled the inclusion rate increase; raised the lifetime capital gains exemption | Moves the other way |
;; | Housing and land ([Land](c10_land.html)) | Build Canada Homes; GST rebate for new homes; ended the Underused Housing Tax | Supply-side focus; nothing on land value or the principal residence exemption. PBO expects Build Canada Homes to add about 26,000 homes over five years |
;; | Wealth concentration ([Wealth Tax](c02_wealth_tax.html)) | No measure | Not on the agenda |
;; | Tax burden on work | Lowest bracket cut 15% → 14% | Broad but small: about \$110–200 per filer a year |
;;
;; : The government's agenda against this series' diagnosis {#tbl-lens}

;; The government's growth agenda points at several of the right targets: internal trade, investment incentives aimed at new capital, and competition.
;; What's missing is the other half of the diagnosis.
;; It has made capital gains *more* lightly taxed than its predecessor planned, left land and large fortunes untouched, and not yet taken on the
;; concentrated industries that the Bank of Canada itself calls an oligopoly.

;; ::: {.callout-note}
;; ## A Closer Look: How to Update This Scorecard

;; Every chart on this page is built from public data that is updated monthly or quarterly.
;; Rerun the data-preparation scripts in `data-prep/scorecard/` (Statistics Canada tables 36-10-0104, 36-10-0206, 14-10-0287, 18-10-0004,
;; 34-10-0158, 17-10-0121 and 12-10-0011), then rebuild the site.
;; Items to watch: the 2025–26 Public Accounts, the first national-interest project designations, Build Canada Homes unit counts,
;; and post-2025 interprovincial trade data.
;; :::

;; ## Conclusion

;; Eighteen months in, the Carney government has passed most of its economic agenda into law, met its 2% defence target,
;; and beaten its own deficit forecast, mostly thanks to oil.
;; The indicators that matter most for this series (productivity, investment, GDP per person, housing) are flat or off pace,
;; but the measures meant to move them only became law in 2026, and US tariffs dominate the data.
;; The fair verdict is "too early to judge," with one exception: on taxing wealth, land and capital gains, the direction is clear,
;; and it runs the other way.

;; In the next part, [How to Get a Wealth Tax](c13_how_to_get_a_wealth_tax.html), we look at what it would take to change that.

;; [^1]: Attribution caveats are detailed in `data-prep/scorecard/NOTES.md`, section 10. Data downloaded October 10, 2026: GDP through 2026Q2, Labour Force Survey through September 2026, CPI through August 2026, trade and housing starts through August 2026.
;; [^2]: LEGISinfo: [Bill C-4](https://www.parl.ca/legisinfo/en/bill/45-1/C-4), [Bill C-5](https://www.parl.ca/legisinfo/en/bill/45-1/C-5), [Bill C-15](https://www.parl.ca/legisinfo/en/bill/45-1/C-15); Department of Finance Canada, [Budget 2025](https://budget.canada.ca/2025/report-rapport/overview-apercu-en.html) and [Spring Economic Update 2026](https://budget.canada.ca/update-miseajour/2026/report-rapport/anx1-en.html); Office of the Parliamentary Budget Officer, [Reducing the lowest federal personal income tax rate to 14 per cent](https://www.pbo-dpb.ca/en/publications/LEG-2526-002-S--reducing-lowest-federal-personal-income-tax-rate-14-per-cent--reduire-14-taux-plus-bas-impot-federal-revenu-particuliers). Full inventory with sources in `data/sc_policy_inventory.csv`.
;; [^3]: Sources for each row are in `data/sc_targets_vs_actuals.csv`. Housing: Statistics Canada [Table 34-10-0135-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3410013501); PBO, [Build Canada Homes](https://www.pbo-dpb.ca/en/publications/RP-2526-020-S--build-canada-homes-outlook-housing-programs-under-budget-2025--maisons-canada-perspectives-entourant-programmes-logement-dans-cadre-budget-2025); public service: PBO Federal Employment Tracking Tool; temporary residents: Statistics Canada Table 17-10-0121-01 and PBO.
;; [^4]: Statistics Canada Tables [36-10-0104-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610010401) and [17-10-0009-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1710000901) (GDP per person), [36-10-0206-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610020601) (productivity), [14-10-0287-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1410028701) (unemployment), [18-10-0004-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1810000401) (CPI), [34-10-0158-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3410015801) (housing starts), [17-10-0121-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1710012101) (non-permanent residents), [12-10-0011-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1210001101) (exports by destination).
;; [^5]: Finance Canada, [Budget 2025](https://budget.canada.ca/2025/report-rapport/overview-apercu-en.html), Spring Economic Update 2026, and the Fiscal Monitor (March 2026); PBO, [Economic and Fiscal Outlook, June 2026](https://www.pbo-dpb.ca/en/publications/RP-2627-002-S--economic-fiscal-outlook-june-2026--perspectives-economiques-financieres-juin-2026) and [Budget 2025: Issues for Parliamentarians](https://www.pbo-dpb.ca/en/publications/RP-2526-017-S--budget-2025-issues-parliamentarians--budget-2025-enjeux-parlementaires).
;; [^6]: NATO, *Defence Expenditure of NATO Countries (2014–2026)*, July 2026; PBO, [Fiscal implications of meeting the NATO 5% commitment](https://www.pbo-dpb.ca/en/publications/RP-2526-022-S--fiscal-implications-meeting-nato-5-commitment--repercussions-financieres-atteinte-cible-5-otan), February 2026.
