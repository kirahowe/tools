^:kindly/hide-code
(ns economics.wealth-distribution.canada.c00-introduction
  {:kindly/options {:kinds-that-hide-code #{:kind/var :kind/hiccup2 :kind/fn}}
   :clay {:title  "Unbounded North: Wealth, Taxes and Growth in Canada"
          :quarto {:sidebar     "unbounded-north"
                   :author      "Kira Howe"
                   :description "A Canadian adaptation of the Unbounded series: how wealth concentration shapes opportunity, productivity and progress in Canada."
                   :abstract    "Canada is more equal and more stable than the United States, but it has stopped getting more productive. The features that concentrate Canadian wealth are the same ones that keep its economy stuck."
                   :date        "2026-10-10"
                   :type        :post
                   :category    :economics
                   :tags        [:tax :economics :growth :canada]}}})

;; ## Unbounded, North of the Border

;; Timothy Pratley's series [*Unbounded*](https://clojurecivitas.org/economics/wealth_distribution/unbounded/u0_introduction.html)
;; argues that progress depends on the circulation of ideas, resources and opportunity,
;; and that extreme wealth concentration blocks it.
;; Its evidence is American.
;; This series asks the same questions of Canada, with Canadian data, in the same way:
;; every chart is built in a Clojure notebook from public data you can inspect and rerun.

;; It is tempting for Canadians to read an American story about inequality and feel relieved.
;; In some ways we should.
;; Canada's after-tax incomes are much more equal than America's,
;; a child born poor in Canada is more likely to reach the middle class,
;; Canadian banks came through 2008 without a bailout,
;; and Canadians collectively still own a public sector with positive net worth.
;; Canada's institutions are stable, and that stability is a real achievement.

;; But Canada has a different problem, and in some ways a bigger one: **it has stopped getting more productive.**
;; Canadians produce about 30% less per hour than Americans, and the gap has been widening for a generation.
;; GDP per person has fallen from about 85% of the American level to about 74%.
;; The United States concentrates wealth at the top of a dynamic economy that keeps producing new firms;
;; Canada concentrates wealth in a stable economy that doesn't.
;; Canada's most valuable companies are, with one or two exceptions, the same banks, pipelines and resource firms they were decades ago.
;; Its largest fortunes sit in old family holding companies, protected industries, and land.

;; This series argues that the two problems, how Canada's wealth is distributed and why its economy doesn't grow,
;; are largely the same problem.
;; The same features that concentrate wealth (half-taxed capital gains, no inheritance tax, protected incumbents,
;; capital flowing into land rather than new businesses) also keep productivity low.
;; Canada has protected *incumbents* as well as *institutions*, and confused the two kinds of stability.

;; > "Courage, my friends; 'tis not too late to build a better world." -- Tommy Douglas

;; ## Central Claims

;; 1. **[Who Pays Tax](c01_who_pays_tax.html)**: Canada's tax system leans on work. Capital gains are concentrated at the top, half-taxed when realized and untaxed until then; counting them lowers the top 1%'s average income tax rate from 39% to 31%.
;; 2. **[Wealth Tax](c02_wealth_tax.html)**: A 1–3% tax on family wealth above \$10M would touch fewer than 1 family in 150 and raise over \$20B a year, by the Parliamentary Budget Officer's cautious estimate.
;; 3. **[Labour Hours](c03_labour_hours.html)**: Canadian house prices relative to income have roughly doubled since 1970 while median employment income has barely moved. Families have borrowed to keep up.
;; 4. **[Interactive Factors](c04_interactive_factors.html)**: Canada's version of the wealth-concentration loop runs through housing, concentrated industries and untaxed inheritance. Public programs are the counterweight.
;; 5. **[Wealth Concentration](c05_wealth_concentration.html)**: Canada's wealth distribution is a right angle too. The broad middle has held on through home equity, but the very top is pulling away.
;; 6. **[Stable and Stuck](c06_productivity.html)**: Canada's central economic problem is productivity. The gap with the US is mostly about efficiency, not equipment; the same firms stay on top year after year; too few new firms start or grow.
;; 7. **[Old Money](c07_old_money.html)**: From colonial land grants to family holding companies, Canada's wealth has often come from access rather than competition, and the evidence says heir control is bad for growth.
;; 8. **[Economic Growth](c08_economic_growth.html)**: Growth needs circulation and participation. Canada's slow growth since the 1970s is a participation problem.
;; 9. **[The Investment Question](c09_investment_question.html)**: The claim that a wealth tax would kill investment doesn't survive the data. Canada already cut capital taxes deeply, and investment and productivity fell further behind.
;; 10. **[Land](c10_land.html)**: Land is over a third of the value of everything Canada owns. Western Canada once taxed land instead of buildings; economists still rank it the least harmful tax there is.
;; 11. **[What Makes Canada Different](c11_canadian_differences.html)**: More equal and mobile than the US because of public programs, with distinctly Canadian fault lines.
;; 12. **[Talent and Opportunity](c12_talent.html)**: Canada gains far more educated people than it loses, but leaks its best home-grown graduates to the US. It keeps more poor children out of poverty than the US, but doesn't get more of them to the top, and that advantage is shrinking.
;; 13. **[Scorecard](c13_scorecard.html)**: What the Carney government has enacted, and what the early data show.
;; 14. **[How to Get a Wealth Tax](c14_how_to_get_a_wealth_tax.html)**: In Canada, only Cabinet can start a tax. Public support exists; what's missing is pressure on the governing party.
;; 15. **[References](c15_references.html)**: the data and research behind every claim.

;; ## Canada at a Glance

;; | | Canada | Source |
;; | :--- | :--- | :--- |
;; | Share of wealth held by the top 1% of families | 23.8% (2023) | Parliamentary Budget Officer |
;; | Share held by the bottom 40% of households | 3.0% (2025) | Statistics Canada |
;; | Families with net worth above \$10M | about 108,000 | PBO database (computed) |
;; | Canadian billionaires (Forbes) | 24 in 2010 → 82 in 2026 | Forbes |
;; | Federal revenue forgone by half-taxing capital gains | \$29.5B (2023) | Finance Canada |
;; | House prices relative to income since 1970 | about 1.9× | OECD |
;; | Household debt | 176% of disposable income (2026) | Statistics Canada |
;; | Real median employment income growth, 1976–2024 | about 7% | Statistics Canada |
;; | Labour productivity, % of US level | 87% (2000) → 70% (2025) | OECD |
;; | Top-10 public companies founded since 1975 (2026) | Canada 1, US 6 | Forbes Global 2000 |
;; | Permanent immigrants per net emigrant | about 6 (2025) | Statistics Canada |
;; | Land as a share of Canada's non-financial assets | 25% (2000) → 37% (2025) | Statistics Canada |
;; | Business investment as % of GDP, before vs after 2000–2012 corporate tax cuts | 11.9% → 11.6% | Statistics Canada |
;;
;; : Canada at a glance (details and sources in each part) {#tbl-glance}

;; ::: {.callout-note}
;; ## About This Series

;; This is an adaptation, not an original argument.
;; The structure, the core claims and the visual approach follow Tim Pratley's *Unbounded* closely,
;; and its American posts are worth reading alongside these.
;; The Canadian data, analysis and any errors are mine.
;; Where Canadian evidence points a different way from the American story, as it does on recent wealth shares
;; and on productivity and pay since the late 1990s, I've tried to say so plainly rather than force the parallel.
;; :::

;; ## Start Here

;; In the first part, [Who Pays Tax](c01_who_pays_tax.html), we look at where Canada's tax revenue comes from,
;; and why a dollar of wages is taxed more than a dollar of asset growth.
