^:kindly/hide-code
(ns economics.wealth-distribution.canada.c09-investment-question
  {:kindly/options {:static true
                    :kinds-that-hide-code #{:kind/var :kind/hiccup2 :kind/fn}}
   :clay {:title  "Unbounded North: The Investment Question"
          :quarto {:sidebar     "unbounded-north"
                   :author      "Kira Howe"
                   :description "Would a wealth tax kill investment and productivity in Canada? What the data say."
                   :abstract    "Canada already ran the experiment in reverse: it cut capital taxes deeply, and investment and productivity fell further behind. The evidence on wealth taxes points to avoidance, not lost investment."
                   :date        "2026-10-19"
                   :type        :post
                   :category    :economics
                   :tags        [:tax :economics :growth :canada :investment :productivity]}}}
  (:require [scicloj.plotje.api :as pj]
            [tablecloth.api :as tc]
            [economics.wealth-distribution.canada.common :as c]))

;; ## The Classic Rebuttal

;; Propose a wealth tax in Canada and the reply is usually immediate:
;; *it will kill investment, drive away capital, and make our productivity problem worse.*
;; Canada's productivity problem is real.
;; In 2024 the Bank of Canada's Senior Deputy Governor called it an emergency:
;; "You've seen those signs that say, 'In emergency, break glass.' Well, it's time to break the glass."[^1]

;; So the question deserves a real answer.
;; Is taxing the wealthiest Canadians a threat to investment and productivity?
;; This part looks at the evidence: Canada's own history, what the institutions studying the productivity problem say causes it,
;; and what happened in countries that actually taxed wealth.
;; Where the evidence cuts the other way, I say so.

;; ## Canada Already Ran the Experiment

;; Between 2000 and 2012, Canada cut taxes on capital more aggressively than most rich countries: 16 points off its combined corporate rate, against 7 for the OECD average.

;; * The combined federal–provincial corporate income tax rate fell from 42.4% to 26.1%.[^2]
;;   In 2000 Canada's rate was about 3 points *above* the United States; by 2012 it was about 13 points *below* it.
;; * The capital gains inclusion rate was cut from 75% to 50% in 2000.
;; * Ottawa introduced the Tax-Free Savings Account in 2009.

;; The promise was more business investment, and with it higher productivity.
;; Here is what happened.

(def cit
  (-> (c/data "A1_corporate_tax_rates_oecd.csv")
      (tc/select-rows #(<= (:year %) 2025))
      (tc/select-columns [:year :canada_combined_statutory_cit_pct])
      (tc/rename-columns {:canada_combined_statutory_cit_pct :value})
      (tc/add-column :series "Corporate tax rate")))

(def investment
  (c/data "A2_business_nonres_investment_share_gdp_annual.csv"))

(def investment-long
  (-> investment
      (tc/select-rows #(<= 1981 (:year %) 2025))
      (tc/select-columns [:year :business_nonres_total_pct_gdp :machinery_equipment_pct_gdp])
      (tc/pivot->longer [:business_nonres_total_pct_gdp :machinery_equipment_pct_gdp]
                        {:target-columns :series :value-column-name :value})
      (tc/update-columns {:series (partial map {:business_nonres_total_pct_gdp "Business investment, % of GDP"
                                                 :machinery_equipment_pct_gdp   "Machinery & equipment, % of GDP"})})))

^{:kindly/options {:id :fig-cit-vs-investment
                   :caption "Canada's combined statutory corporate income tax rate (OECD) and business non-residential investment as a share of GDP (Statistics Canada). The 2006–2014 rise in investment was oil-and-gas structures during the commodity boom; machinery and equipment, the investment most tied to productivity, kept falling.[^2][^3]"}}
(-> (tc/concat cit investment-long)
    (pj/lay-line :year :value {:color :series})
    (pj/lay-rule-v {:x-intercept 2000 :stroke-dash :dashed})
    (pj/lay-rule-v {:x-intercept 2012 :stroke-dash :dashed})
    (pj/lay-text {:data (tc/dataset {:year [2006] :value [45] :t ["Rate cuts, 2000–2012"]})
                  :x :year :y :value :text :t :align-x :center :overlay true})
    (pj/scale :color {:values [c/o c/w c/wd]})
    (pj/scale :y {:include 0})
    (pj/options {:title "Corporate Tax Cuts and Business Investment"
                 :x-label "Year" :y-label "Percent" :color-label "" :legend-position :bottom}))

(defn period-avg [from to k]
  (let [xs (->> (tc/rows investment :as-maps)
                (filter #(<= from (:year %) to))
                (map k))]
    (/ (reduce + xs) (count xs))))

^:kind/table
(tc/dataset
 (for [[label from to] [["1981–1999 (before the cuts)" 1981 1999]
                        ["2000–2012 (during the cuts)" 2000 2012]
                        ["2013–2025 (after the cuts)" 2013 2025]]]
   {"Period"                        label
    "Business investment, % of GDP" (format "%.1f" (period-avg from to :business_nonres_total_pct_gdp))
    "Machinery & equipment, % of GDP" (format "%.1f" (period-avg from to :machinery_equipment_pct_gdp))}))

;; Business investment as a share of the economy did not rise.
;; It averaged 11.9% of GDP before the cuts, 12.0% during them, and 11.6% after.
;; Machinery and equipment fell by half, from 6.3% of GDP in 2000 to 3.2% in 2025, measured in current dollars; falling equipment prices explain part of that.[^3]

;; ### Where Did the Money Go?

;; Corporate profits did rise.
;; Corporations' gross operating surplus grew from about 20.5% of GDP on average in 1981–1999
;; to about 24% after 2000.
;; But firms reinvested a *smaller* share of it.[^3]

^{:kindly/options {:id :fig-reinvestment
                   :caption "Business non-residential investment divided by corporations' gross operating surplus (a national accounts measure of pre-tax profit plus depreciation). Below 0.5 means less than half of corporate surplus is reinvested in the business sector.[^3]"}}
(-> (c/data "A4_corporate_surplus_vs_business_investment.csv")
    (pj/lay-line :year :nonres_investment_to_corp_gross_operating_surplus_ratio {:color c/o})
    (pj/lay-rule-v {:x-intercept 2000 :stroke-dash :dashed})
    (pj/scale :y {:include 0})
    (pj/options {:title   "Share of Corporate Surplus Reinvested"
                 :x-label "Year"
                 :y-label "Investment ÷ corporate surplus"}))

;; Some of the difference piled up as cash.
;; In 2012, Bank of Canada Governor Mark Carney, now Prime Minister, told an audience of auto workers that companies
;; sitting on their cash were holding "dead money":
;; "Their job is to put money to work and if they can't think of what to do with it, they should give it back to their shareholders."[^4]

(def cash
  (-> (c/data "A3_nfc_currency_deposits_pct_gdp.csv")
      (tc/select-rows #(= "Non-financial private corporations" (:sector %)))))

^{:kindly/options {:id :fig-dead-money
                   :caption "Currency and deposits held by non-financial private corporations at year end, as a share of GDP. A gross figure: corporate debt also rose, and firms in many countries hold more cash than they used to.[^5]"}}
(-> cash
    (pj/lay-area :year :currency_deposits_pct_gdp {:color c/o :alpha 0.6})
    (pj/lay-rule-v {:x-intercept 2000 :stroke-dash :dashed})
    (pj/lay-rule-v {:x-intercept 2012 :stroke-dash :dashed})
    (pj/options {:title   "Canadian Corporate Cash"
                 :x-label "Year"
                 :y-label "% of GDP"}))

;; Corporate cash and deposits rose from 11% of GDP in 2000 to 19.5% in 2012 and 29% in 2025: almost \$1 trillion.[^5]
;; After the cuts, profits and corporate cash rose, but investment did not.

;; This is not a controlled experiment.
;; Investment might have been weaker still without the cuts, and the evidence on corporate tax rates
;; elsewhere (below) suggests they do have *some* effect.
;; But the fair conclusion from Canada's own data is that deep cuts to capital taxes
;; did not deliver the investment boom they promised.

;; ## The Productivity Gap Grew Anyway

(def productivity
  (-> (c/data "B1_labour_productivity_canada_us_oecd.csv")
      (tc/drop-missing :can_pct_of_usa_per_hour_constant)))

^{:kindly/options {:id :fig-productivity-gap
                   :caption "Canada's GDP per hour worked as a share of the US level, constant 2020 PPPs (OECD Productivity Database).[^6]"}}
(-> productivity
    (pj/lay-line :year :can_pct_of_usa_per_hour_constant {:color c/red})
    (pj/lay-rule-v {:x-intercept 2000 :stroke-dash :dashed})
    (pj/lay-rule-v {:x-intercept 2012 :stroke-dash :dashed})
    (pj/scale :y {:domain [50 100]})
    (pj/options {:title   "Canadian Labour Productivity, % of US"
                 :x-label "Year"
                 :y-label "Canada as % of US (GDP per hour)"}))

;; Canada's output per hour was 87% of the American level in 2000.
;; After a decade of capital tax cuts it was 76%, and in 2025 it was 70%.[^6]
;; Real business investment per worker tells a more complicated story.
;; During the cuts it rose faster in Canada than in the US, driven by the oil-sands boom;
;; since the 2014 oil peak it has fallen about 20% in Canada while rising about 35% in the US.

(def inv-per-worker
  (-> (c/data "B2_business_investment_per_worker_canada_us.csv")
      (tc/select-rows #(and (>= (:year %) 2000) (= "outturn/estimate" (:status %))))
      (tc/pivot->longer [:can_index_2014_100 :usa_index_2014_100]
                        {:target-columns :country :value-column-name :index})
      (tc/update-columns {:country (partial map {:can_index_2014_100 "Canada" :usa_index_2014_100 "United States"})})))

^{:kindly/options {:id :fig-investment-per-worker
                   :caption "Real private non-residential investment per worker, 2014 = 100 (OECD Economic Outlook database). Since 2014, investment per worker has risen about 35% in the US and fallen about 20% in Canada.[^7]"}}
(-> inv-per-worker
    (pj/lay-line :year :index {:color :country})
    (pj/scale :color {:values [c/red c/wd]})
    (pj/options {:title   "Business Investment per Worker"
                 :x-label "Year" :y-label "Index, 2014 = 100"
                 :color-label "" :legend-position :bottom}))

;; Canada's corporate tax rate is now roughly equal to America's (26.0% vs 25.6% in 2025).[^2]
;; Yet since 2014, real business investment per worker has risen about 35% in the United States and *fallen* about 20% in Canada.[^7]
;; The oil-price crash did much of the damage, but investment per worker collapsed in 2015–17, when Canada's statutory rate was 12 points *lower* than America's.
;; That points away from corporate tax rates as the main cause (though US bonus depreciation after 2017 also lowered the tax on new American investment).

;; ## What the Experts Say Is Wrong

;; The institutions that study Canada's productivity problem point mostly at something else: **weak competition.**

;; * **Bank of Canada** (Carolyn Rogers, 2024), on multifactor productivity: "if I had to pick the biggest concern in this area, I'd say it's competition."
;;   On investment: "If firms have high profits, high margins and limited competition, they may not feel as much pressure to invest."
;;   The speech also cites regulatory uncertainty. It does not name tax levels.[^1]
;; * **Statistics Canada** (Gu, 2024): investment per worker fell 20% from 2006 to 2021.
;;   Declining firm entry and competitive intensity explain about 30% of the decline over 2006–2019.[^8]
;; * **Competition Bureau** (2023): the number of highly concentrated industries rose from 19 to 27 between 2005 and 2018.
;;   Firm entry rates fell 24%. The same top firms increasingly stay on top year after year.[^9]
;; * **OECD Economic Survey of Canada** (2025): "Business dynamism has been weakening while market concentration has risen."[^10]
;; * **IMF** (2025): weak business dynamism, slow capital deepening and high concentration in key service industries
;;   are the main long-term constraints; it recommends a stronger competition mandate and removing interprovincial trade barriers.[^11]

;; None of them blames the tax on very large personal fortunes.
;; Canada does not currently have one.

;; The IMF does welcome the *targeted* investment incentives in Budget 2025, the "Productivity Super-Deduction" and accelerated
;; write-offs, which lower the tax on *new* capital.[^11]
;; That distinction turns out to be the key to the whole question.

;; ## Two Different Kinds of Capital Tax

;; The best research separates taxes on a *firm's new investment* from taxes on the *owner's income or wealth*.

;; * Taxes on the firm's marginal investment (the corporate rate, depreciation rules, expensing) have measurable,
;;   if modest, effects on investment.
;;   US bonus depreciation raised investment in eligible equipment by 10–17%.[^12]
;;   Peer-reviewed Canadian provincial studies find corporate rate cuts raise growth somewhat.[^13]
;; * Taxes on *owners* (dividend taxes, capital gains taxes, wealth taxes) show little or no effect on real investment.
;;   The 2003 US dividend tax cut "caused zero change in corporate investment and employee compensation."[^14]
;;   France's official evaluation of its 2018 capital tax reforms, which replaced its wealth tax,
;;   found no effect on investment or wages in the firms most exposed to the new flat tax on capital income,
;;   and could not detect that the old wealth tax had made family firms pay more dividends or invest less.
;;   It noted this was "in line with numerous academic studies… which fail to show that a change in capital taxation
;;   borne by households can have a notable effect on the real behaviour of firms."[^15]

;; A wealth tax on families worth more than \$10M falls on owners, not on the firm's next machine.
;; The "it will kill investment" argument mostly borrows evidence from the first category to argue about the second.

;; ## What Happened Where Wealth Was Actually Taxed

;; | Country | Study | Real effects (investment, jobs, saving) | Base effects (avoidance, evasion, mobility) |
;; | :--- | :--- | :--- | :--- |
;; | Norway | Bjørneby, Markussen & Røed (*Economica*, 2023)[^16] | Higher wealth tax → *more* employment in owners' firms | Owners shift wealth into favourably valued business assets |
;; | Norway | Ring (*Review of Economic Studies*, 2025)[^17] | Households *save more* when wealth tax rises, mainly by working more | — |
;; | Norway | Ring (NBER working paper, 2026)[^18] | After 2021–24 increases: no fall in firm investment, no rise in dividends or bankruptcies | Departures concentrated in top 0.1%; at most ~100 people left; revenue still rose 71.5% vs 75% without them |
;; | Norway | Berzins, Bøhren & Stacescu (working paper)[^19] | Illiquid owners of small firms pay more dividends; their firms invest and grow less | — |
;; | Switzerland | Brülhart et al. (*AEJ: Policy*, 2022)[^20] | Saving responses explain little | Reported wealth rises ≥43% six years after a 1-point rate cut; mobility and evasion |
;; | Denmark | Jakobsen et al. (*QJE*, 2020)[^21] | — | Sizeable long-run response of taxable wealth at the top |
;; | Spain | Agrawal, Foremny & Martínez-Toledano (*AEJ: Applied*, 2025)[^22] | — | Wealthy moved to Madrid, the one region without the tax |
;; | Colombia | Londoño-Vélez & Ávila-Mahecha (*AER: Insights*, 2021)[^23] | — | Evasion through offshore accounts; enforcement raised compliance |
;; | France | France Stratégie (2023)[^15] | No detectable investment effect from replacing the wealth tax | Fewer departures of wealthy taxpayers after repeal |
;;
;; : Evidence on wealth taxes (peer-reviewed unless noted; Ring 2026 and Berzins et al. are working papers; France Stratégie is an official evaluation) {#tbl-wealth-tax-evidence}

;; The pattern is consistent.
;; Wealth taxes cause **avoidance, evasion, and some mobility** at the very top.
;; Those effects are real, and they shrink the revenue.
;; But the studies that measure real effects (four from Norway, plus France's evaluation) mostly find **little sign of lost investment or jobs**.
;; The main exception is the Berzins, Bøhren and Stacescu working paper, which finds that owners of smaller private firms
;; who lack cash pull money out of their firms to pay the tax.
;; Every proposal costed by the PBO starts at \$10M or more of net worth, and the OECD recommends
;; instalment payments for exactly this case.[^24]

;; Across countries, cutting taxes on the rich doesn't buy growth either.
;; A study of 30 major tax cuts for the rich in 18 OECD countries, Canada included, over 50 years
;; found they raised the top 1%'s income share but had no significant effect on GDP per capita or unemployment.[^25]
;; Piketty, Saez and Stantcheva find top tax rate cuts are "associated with top one percent pretax income shares increases but not higher economic growth."[^26]

;; ## The Avoidance Problem Is a Design Problem

;; The honest objection to a wealth tax is not that it kills investment.
;; It is that the wealthy are good at avoiding it.
;; That is a design problem, and the evidence tells us how to design around it:

;; * **Tax at the national level.** Spain's regional wealth tax leaked to Madrid; Switzerland's cantonal taxes leak between cantons.
;;   A Canadian wealth tax should be federal, not provincial.
;; * **Use third-party reporting.** Colombia's and Switzerland's large responses came where financial wealth wasn't reported by banks.
;;   Canada's CRA already receives third-party slips on the income from, and sales of, most financial assets;
;;   adding year-end balances to those slips would give a wealth tax a third-party base.
;; * **Keep exemptions few.** In Norway and Spain, much of the avoidance ran through favourably valued or exempt business assets; in Sweden, through under-reported asset values.[^31]
;; * **Close the exit.** Canada already has a departure tax, which treats emigrants as having sold their assets on the way out.
;; * **Be conservative about revenue.** The PBO assumes 35% of each wealthy family's net worth is hidden or avoided, which on its own public file roughly halves the revenue;
;;   that is cautious compared with the 7–17% a review of the evidence suggests for a well-designed 1% tax.[^27]
;;   Even so, it estimates a 1–3% tax above \$10M would raise \$121.5B over five years.[^24]

;; The OECD is generally lukewarm on net wealth taxes.
;; But it also says there are "stronger arguments for having a net wealth tax" in countries
;; that tax capital gains lightly or have no inheritance tax.[^28]
;; Canada does both.

;; ## The Golden Age, Again

;; Canada's fastest productivity growth on record came in the 1960s, when Ottawa still had an estate tax
;; and the very highest earners (roughly the top 0.01%) faced much higher marginal rates.

(def growth-by-period
  (-> (c/data "F1_business_sector_productivity_growth_by_period.csv")
      (tc/select-rows #(not (#{[1961 2000] [2000 2023]} [(:start %) (:end %)])))
      (tc/add-column :period #(map (fn [a b] (str a "–" b)) (:start %) (:end %)))
      (tc/add-column :label #(map c/pct-text (:labour_productivity_cagr_pct %)))))

^{:kindly/options {:id :fig-productivity-by-period
                   :caption "Average annual growth in business-sector labour productivity (Statistics Canada).[^29]"}}
(-> growth-by-period
    (pj/lay-bar :period :labour_productivity_cagr_pct {:color c/w})
    (pj/lay-label {:text :label})
    (pj/options {:title   "Canadian Productivity Growth by Period"
                 :x-label "Period"
                 :y-label "Labour productivity growth, % per year"}))

;; Labour productivity grew 3.6% a year from 1961 to 1973, and under 1% a year since 2000 (0.8% over 2000–2023).[^29]
;; This is context, not proof.
;; The postwar boom had many causes, including catching up to American technology, the Auto Pact, cheap energy, and heavy public investment.
;; And the famous "80% top rate" applied to very few people.
;; Saez and Veall show that someone at the 99.9th percentile faced about 45% in 1950,
;; and 48% in 2000; only the top ~1,000 earners saw their rates fall by 10 points or more.[^30]
;; Since 2016 the combined top rate in Ontario has been 53.5%, higher than the rate at the 99.9th percentile in 1960.
;; What history does show is that a high-tax, estate-taxed Canada was compatible with the fastest productivity growth in the official series, which begins in 1961.

;; ::: {.callout-note}
;; ## A Closer Look: What the Rebuttal Gets Right

;; Fairness to the other side makes the case stronger, so here is what the evidence does support:

;; 1. Wealth taxes lose part of their base to avoidance, evasion and emigration. Revenue estimates must be conservative.
;; 2. Very wealthy people do move in response to wealth taxes, especially between regions of the same country.
;; 3. Owners of illiquid private firms can face cash squeezes. Thresholds and instalment rules matter.
;; 4. France's evaluation found faster business creation in the sectors that gained most from its 2018 capital-tax cuts, though mostly from its flat tax and corporate rate cut rather than the wealth-tax repeal.
;; 5. Taxes on *new corporate investment* do matter at the margin, so a wealth tax should not be paired with higher taxes on new investment.

;; None of these says a well-designed wealth tax on fortunes above \$10M would damage Canadian productivity.
;; The evidence says Canada's productivity problem lies in weak competition, protected incumbents, and low investment
;; by firms that already make plenty of profit.
;; :::

;; ## Conclusion

;; The claim that a wealth tax will "kill investment" doesn't survive contact with the data.
;; Canada spent a decade cutting capital taxes; investment did not rise as a share of the economy, and outside the oil boom it stalled; productivity fell further behind,
;; and corporate cash piled up.
;; The institutions studying the problem blame weak competition, not taxes on large fortunes.
;; Where wealth taxes have been studied, the costs show up as avoidance and lost revenue, not lost investment or jobs.
;; Those are design problems, and Canada already has some of the tools to solve them.

;; In the next part, [Land](c10_land.html), we look at the largest and least productive store of Canadian wealth, and how to tax it.

;; [^1]: Carolyn Rogers, Senior Deputy Governor, Bank of Canada, ["Time to break the glass: Fixing Canada's productivity problem"](https://www.bankofcanada.ca/2024/03/time-to-break-the-glass-fixing-canadas-productivity-problem/), Halifax, March 26, 2024.
;; [^2]: OECD [Corporate Tax Statistics](https://www.oecd.org/en/data/datasets/corporate-income-tax-rates-database.html), combined (central plus sub-central) statutory corporate income tax rates.
;; [^3]: Statistics Canada, [Table 36-10-0104-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610010401) (expenditure-based GDP: business gross fixed capital formation in non-residential structures, machinery and equipment, and intellectual property products) and [Table 36-10-0103-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610010301) (income-based GDP: corporations' net operating surplus and consumption of fixed capital). Nominal shares; annual averages of quarterly data.
;; [^4]: Canadian Press, ["Carney urges firms to spend cash or return it to shareholders"](https://www.cbc.ca/news/business/carney-urges-firms-to-spend-cash-or-return-to-shareholders-1.1130766), CBC News, August 23, 2012.
;; [^5]: Statistics Canada, [Table 36-10-0580-01, National Balance Sheet Accounts](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610058001): non-financial private corporations, currency and deposits (fourth quarter), divided by annual nominal GDP.
;; [^6]: OECD [Productivity Database](https://data-explorer.oecd.org/s/4rg), GDP per hour worked, total economy, constant prices and 2020 PPPs. Compare OECD, [Economic Surveys: Canada 2025](https://www.oecd.org/en/publications/2025/05/oecd-economic-surveys-canada-2025_ee18a269.html), and the Bank of Canada speech above, which put Canada at 71% of the US level in 2022.
;; [^7]: OECD Economic Outlook database (No. 119): private non-residential gross fixed capital formation (volume) divided by total employment, indexed to 2014. The OECD's [2025 Economic Survey of Canada](https://www.oecd.org/en/publications/2025/05/oecd-economic-surveys-canada-2025_ee18a269.html) reports that by 2023, Canadian investment per worker was 85% of its 2014 level, while it had grown 21% in the US.
;; [^8]: Wulong Gu, ["Investment Slowdown in Canada After the Mid-2000s: The Role of Competition and Intangibles"](https://www150.statcan.gc.ca/n1/pub/11f0019m/11f0019m2024001-eng.htm), Statistics Canada Analytical Studies Branch Research Paper No. 474, 2024; [The Daily](https://www150.statcan.gc.ca/n1/daily-quotidien/240222/dq240222d-eng.htm).
;; [^9]: Competition Bureau Canada, ["Competition in Canada from 2000 to 2020: An Economy at a Crossroads"](https://ised-isde.canada.ca/site/competition-bureau-canada/en/how-we-foster-competition/education-and-outreach/competition-canada-2000-2020-economy-crossroads), 2023.
;; [^10]: OECD, [Economic Surveys: Canada 2025](https://www.oecd.org/en/publications/2025/05/oecd-economic-surveys-canada-2025_ee18a269.html), Chapter 4.
;; [^11]: IMF, ["Canada: Staff Concluding Statement of the 2025 Article IV Mission"](https://www.imf.org/en/news/articles/2025/12/05/cs-canada-staff-concluding-statement-of-the-2025-article-iv-mission), December 5, 2025.
;; [^12]: Eric Zwick and James Mahon, ["Tax Policy and Heterogeneous Investment Behavior"](https://doi.org/10.1257/aer.20140855), *American Economic Review* 107(1), 2017. See also Ohrn, [*AEJ: Economic Policy* 10(2), 2018](https://doi.org/10.1257/pol.20150378), and Chodorow-Reich, Zidar and Zwick, ["Lessons from the Biggest Business Tax Cut in US History"](https://doi.org/10.1257/jep.38.3.61), *Journal of Economic Perspectives* 38(3), 2024.
;; [^13]: Ergete Ferede and Bev Dahlby, ["The Impact of Tax Cuts on Economic Growth: Evidence from the Canadian Provinces"](https://doi.org/10.17310/ntj.2012.3.03), *National Tax Journal* 65(3), 2012, and [*FinanzArchiv* 77(1), 2021](https://doi.org/10.1628/fa-2021-0002).
;; [^14]: Danny Yagan, ["Capital Tax Reform and the Real Economy: The Effects of the 2003 Dividend Tax Cut"](https://doi.org/10.1257/aer.20130098), *American Economic Review* 105(12), 2015.
;; [^15]: France Stratégie, Comité d'évaluation des réformes de la fiscalité du capital, [fourth and final report](https://www.strategie-plan.gouv.fr/files/files/Publications/Rapport/fs-2023-rapport-isf-quatrieme_rapport_complet_17octobre_avis_0.pdf), October 2023, pp. 16–20 (translation mine).
;; [^16]: Marie Bjørneby, Simen Markussen and Knut Røed, ["An imperfect wealth tax and employment in closely held firms"](https://doi.org/10.1111/ecca.12456), *Economica* 90(358), 2023.
;; [^17]: Marius A. K. Ring, ["Wealth Taxation and Household Saving: Evidence from Assessment Discontinuities in Norway"](https://doi.org/10.1093/restud/rdae100), *Review of Economic Studies* 92(5), 2025.
;; [^18]: Marius A. K. Ring, ["Taxing Entrepreneurial Wealth: Evidence from Norway, 2021–2025"](https://www.nber.org/papers/w35854), NBER Working Paper 35854, October 2026. Not yet peer reviewed.
;; [^19]: Janis Berzins, Øyvind Bøhren and Bogdan Stacescu, ["Illiquid Owners and Firm Behavior: Financial and Real Effects of the Personal Wealth Tax on Private Firms"](https://www.ecgi.global/publications/working-papers/illiquid-owners-and-firm-behavior-financial-and-real-effects-of-the), ECGI Finance Working Paper 646/2019.
;; [^20]: Marius Brülhart, Jonathan Gruber, Matthias Krapf and Kurt Schmidheiny, ["Behavioral Responses to Wealth Taxes: Evidence from Switzerland"](https://doi.org/10.1257/pol.20200258), *AEJ: Economic Policy* 14(4), 2022.
;; [^21]: Katrine Jakobsen, Kristian Jakobsen, Henrik Kleven and Gabriel Zucman, ["Wealth Taxation and Wealth Accumulation: Theory and Evidence from Denmark"](https://doi.org/10.1093/qje/qjz032), *Quarterly Journal of Economics* 135(1), 2020.
;; [^22]: David R. Agrawal, Dirk Foremny and Clara Martínez-Toledano, ["Wealth Tax Mobility and Tax Coordination"](https://doi.org/10.1257/app.20220615), *AEJ: Applied Economics* 17(1), 2025.
;; [^23]: Juliana Londoño-Vélez and Javier Ávila-Mahecha, ["Enforcing Wealth Taxes in the Developing World: Quasi-Experimental Evidence from Colombia"](https://doi.org/10.1257/aeri.20200319), *AER: Insights* 3(2), 2021.
;; [^24]: Office of the Parliamentary Budget Officer, [election proposal costing of a 1%/2%/3% net wealth tax above \$10M/\$50M/\$100M](https://distribution-a617274656661637473.pbo-dpb.ca/cc1b029d8d36c1340f715cb7666bfd2132ef6683608cecae42a5e71b82bb6ad6), April 9, 2025. OECD design guidance: [The Role and Design of Net Wealth Taxes in the OECD](https://doi.org/10.1787/9789264290303-en), 2018.
;; [^25]: David Hope and Julian Limberg, ["The economic consequences of major tax cuts for the rich"](https://doi.org/10.1093/ser/mwab061), *Socio-Economic Review* 20(2), 2022.
;; [^26]: Thomas Piketty, Emmanuel Saez and Stefanie Stantcheva, ["Optimal Taxation of Top Labor Incomes: A Tale of Three Elasticities"](https://doi.org/10.1257/pol.6.1.230), *AEJ: Economic Policy* 6(1), 2014. For a broader view see Ostry, Berg and Tsangarides, ["Redistribution, Inequality, and Growth"](https://doi.org/10.5089/9781484352076.006), IMF Staff Discussion Note 14/02. Note that OECD work by [Cingano (2014)](https://doi.org/10.1787/5jxrjncwxv6j-en) finds the growth cost of inequality comes from the gap at the *bottom*, not from top incomes pulling away.
;; [^27]: Arun Advani and Hannah Tarrant, ["Behavioural responses to a wealth tax"](https://doi.org/10.1111/1475-5890.12283), *Fiscal Studies* 42(3–4), 2021.
;; [^28]: OECD, [The Role and Design of Net Wealth Taxes in the OECD](https://doi.org/10.1787/9789264290303-en), Tax Policy Studies No. 26, 2018. Applying this reasoning to Canada is my inference, not the OECD's.
;; [^29]: Statistics Canada, [Table 36-10-0208-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610020801), business sector labour productivity; compound annual growth between the years shown.
;; [^30]: Emmanuel Saez and Michael R. Veall, ["The Evolution of High Incomes in Northern America: Lessons from Canadian Evidence"](https://doi.org/10.1257/0002828054201404), *American Economic Review* 95(3), 2005; marginal rates from the [NBER working paper](https://www.nber.org/papers/w9607) appendix.
;; [^31]: David Seim, ["Behavioral Responses to Wealth Taxes: Evidence from Sweden"](https://doi.org/10.1257/pol.20150290), *American Economic Journal: Economic Policy* 9(4), 2017.
