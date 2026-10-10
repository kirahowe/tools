^:kindly/hide-code
(ns economics.wealth-distribution.canada.c0-introduction
  {:kindly/options {:kinds-that-hide-code #{:kind/var :kind/hiccup2 :kind/fn}}
   :clay {:title  "Unbounded North: Wealth, Taxes and Growth in Canada"
          :quarto {:sidebar     "unbounded-north"
                   :author      "Kira Howe"
                   :description "A Canadian adaptation of the Unbounded series: how wealth concentration shapes opportunity, productivity and progress in Canada."
                   :abstract    "Canada is more equal than the United States, but its wealth has the same right-angled shape, its tax system leans on work, and its growth has stalled. A wealth tax is a corrective, and the evidence says it would not kill investment."
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
;; a child born poor in Canada has a better chance of reaching the top,
;; and Canadians collectively still own a public sector with positive net worth.
;; But the relief doesn't survive a closer look at wealth.
;; The top 1% of Canadian families hold about a quarter of all wealth.
;; Our tax system half-taxes capital gains, doesn't tax inheritances at all,
;; and draws most of its revenue from work.
;; House prices have run far ahead of incomes, much further than in the United States.
;; And Canada has been falling behind on productivity for decades, despite cutting taxes on capital deeply.

;; > "Courage, my friends; 'tis not too late to build a better world." -- Tommy Douglas

;; ## Central Claims

;; 1. **[Who Pays Tax](c1_who_pays_tax.html)**: Canada's tax system leans on work. Capital gains are concentrated at the top, half-taxed when realized and untaxed until then; counting them lowers the top 1%'s average income tax rate from 39% to 31%.
;; 2. **[Wealth Tax](c2_wealth_tax.html)**: A 1–3% tax on family wealth above \$10M would touch about 1 family in 150 and raise over \$20B a year, by the Parliamentary Budget Officer's cautious estimate.
;; 3. **[Labour Hours](c3_labour_hours.html)**: Canadian house prices relative to income have roughly doubled since 1970 while median employment income has barely moved. Families have borrowed to keep up.
;; 4. **[Interactive Factors](c4_interactive_factors.html)**: Canada's version of the wealth-concentration loop runs through housing, concentrated industries and untaxed inheritance. Public programs are the counterweight.
;; 5. **[Wealth Concentration](c5_wealth_concentration.html)**: Canada's wealth distribution is a right angle too. The broad middle has held on through home equity, but the very top is pulling away.
;; 6. **[Economic Growth](c6_economic_growth.html)**: Canada's growth problem is a participation problem. Economists have a name for one version of it: *the Canadian disease*.
;; 7. **[The Investment Question](c7_investment_question.html)**: The claim that a wealth tax would kill investment doesn't survive the data. Canada already cut capital taxes deeply, and investment and productivity fell further behind.
;; 8. **[What Makes Canada Different](c8_canadian_differences.html)**: More equal and mobile than the US because of public programs, with distinctly Canadian fault lines.
;; 9. **[How to Get a Wealth Tax](c9_how_to_get_a_wealth_tax.html)**: In Canada, only Cabinet can start a tax. Public support exists; what's missing is pressure on the governing party.
;; 10. **[References](c10_references.html)**: the data and research behind every claim.

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

;; In the first part, [Who Pays Tax](c1_who_pays_tax.html), we look at where Canada's tax revenue comes from,
;; and why a dollar of wages is taxed more than a dollar of asset growth.
