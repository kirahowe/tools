^:kindly/hide-code
(ns economics.wealth-distribution.canada.c4-interactive-factors
  {:kindly/options {:static true
                    :kinds-that-hide-code #{:kind/var :kind/hiccup2 :kind/fn :kind/graphviz}}
   :clay {:title  "Unbounded North: Interactive Factors"
          :quarto {:sidebar     "unbounded-north"
                   :author      "Kira Howe"
                   :description "A systems view of how wealth concentration, housing, market power, and public services interact in Canada."
                   :abstract    "Canada's distinctive feedback loops run through housing, concentrated industries, and inheritance. Its public services are the counterweight."
                   :date        "2026-10-14"
                   :type        :post
                   :category    :economics
                   :tags        [:tax :economics :growth :canada]}}})

;; ## Modelling the Canadian System

;; Tim Pratley's original series introduced an *Interactive Factors* diagram:
;; name the factors that matter, draw the causal arrows between them,
;; then look for the loops that reinforce themselves.[^1]
;; The loops matter more than any single arrow,
;; because a loop is what turns a small advantage into a large one.

;; The overall shape carries over to Canada.
;; The parts that differ are where wealth is stored, who it competes with, and what pushes back against it.
;; Canada's version has three loops of its own:

;; 1. **The housing loop.** In Canada, middle-class wealth is mostly housing wealth.
;;    Rising home prices make owners richer, so owning stays the main way to build wealth.
;;    That pulls in more capital and raises prices again.
;;    Housing wealth is also the most lightly taxed kind, because of the principal residence exemption.
;; 2. **The market-power loop.** Many Canadian industries are run by a few big firms:
;;    banking, telecoms, groceries, airlines, rail.
;;    Concentration raises profits ("rents") without raising investment.
;;    Those profits flow to concentrated shareholdings, and shareholders who already hold
;;    concentrated wealth can afford to defend the arrangement.
;; 3. **The inheritance loop.** Canada has had no estate or inheritance tax since 1972.
;;    Most of the country's largest fortunes belong to second-, third- and fourth-generation families,
;;    and dual-class shares let those families keep control of the firms that hold them.

;; Against these, Canada has counterweights the United States mostly lacks:
;; single-payer health insurance, the Canada Child Benefit, public pensions (CPP/QPP and OAS/GIS),
;; and, more recently, a national childcare and dental plan.
;; These cut the cost of being alive for ordinary families, which is why Canada still has much
;; higher intergenerational mobility than the United States (see [What Makes Canada Different](c8_canadian_differences.html)).
;; They are funded mostly by taxes on work, which is the problem [Who Pays Tax](c1_who_pays_tax.html) describes.

;; ::: {#fig-iff-canada}

^:kind/graphviz
["digraph canada {
  graph [rankdir=LR, bgcolor=\"transparent\", nodesep=0.35, ranksep=0.6, fontname=\"Helvetica\"]
  node  [shape=box, style=\"rounded,filled\", fillcolor=\"#f4f1ea\", color=\"#8b7246\", fontname=\"Helvetica\", fontsize=11]
  edge  [color=\"#777777\", fontname=\"Helvetica\", fontsize=9]

  wc      [label=\"Wealth\\nConcentration\", fillcolor=\"#f6c9c3\"]
  assets  [label=\"Asset Stores\\n(housing, land, equity)\"]
  capinc  [label=\"Capital Income\\n(half-taxed gains,\\ndividends, rents)\"]
  inherit [label=\"Inheritance\\n(no estate tax)\"]
  market  [label=\"Market Power\\n(banks, telecom,\\ngrocery, rail)\"]
  policy  [label=\"Government\\nPolicy\"]
  taxpref [label=\"Tax Preferences\\n(capital gains, PRE,\\nprivate corps)\"]
  housing [label=\"Housing\\nAffordability\"]
  debt    [label=\"Household\\nDebt\"]
  living  [label=\"Standard of\\nLiving\", fillcolor=\"#cfe3c9\"]
  stab    [label=\"System\\nStability\", fillcolor=\"#cfe3c9\"]
  mob     [label=\"Class\\nMobility\"]
  labour  [label=\"Labour\\nIncome\"]
  spend   [label=\"Consumer\\nSpending\"]
  prod    [label=\"Productive\\nCapacity\"]
  invest  [label=\"Business\\nInvestment\"]
  innov   [label=\"Innovation\"]
  know    [label=\"Knowledge\"]
  public  [label=\"Public Services\\n(health, CCB, CPP,\\nchildcare)\", fillcolor=\"#d6e4f0\"]
  revenue [label=\"Tax\\nRevenue\"]

  # the concentration loops (red)
  wc -> assets      [color=\"#c0392b\", penwidth=2]
  assets -> capinc  [color=\"#c0392b\", penwidth=2]
  capinc -> wc      [color=\"#c0392b\", penwidth=2]
  wc -> inherit     [color=\"#c0392b\", penwidth=2]
  inherit -> wc     [color=\"#c0392b\", penwidth=2]
  wc -> policy      [color=\"#c0392b\", penwidth=2, label=\"influence\"]
  policy -> taxpref [color=\"#c0392b\", penwidth=2]
  taxpref -> capinc [color=\"#c0392b\", penwidth=2]
  market -> capinc  [color=\"#c0392b\", penwidth=2, label=\"rents\"]
  wc -> market      [color=\"#c0392b\", penwidth=2]

  # the growth loops (green)
  labour -> spend   [color=\"#2e8b57\", penwidth=2]
  spend -> prod     [color=\"#2e8b57\", penwidth=2]
  prod -> labour    [color=\"#2e8b57\", penwidth=2]
  innov -> know     [color=\"#2e8b57\", penwidth=2]
  know -> prod      [color=\"#2e8b57\", penwidth=2]
  prod -> innov     [color=\"#2e8b57\", penwidth=2]
  invest -> prod    [color=\"#2e8b57\", penwidth=2]

  # cross links
  assets -> housing [label=\"bids up prices (−)\"]
  housing -> debt   [label=\"(−)\"]
  debt -> living    [label=\"(−)\"]
  debt -> spend     [label=\"(−)\"]
  market -> invest  [label=\"(−)\"]
  market -> innov   [label=\"(−)\"]
  wc -> mob         [label=\"(−)\"]
  housing -> mob
  mob -> know
  labour -> living
  living -> stab
  mob -> stab
  labour -> revenue
  capinc -> revenue [style=dashed, label=\"lightly\"]
  revenue -> public
  public -> living
  public -> mob
  stab -> assets
}"]

;; Canadian Interactive Factors (nodes) and causes (arrows). Red loops concentrate; green loops grow; blue is the public counterweight.
;; :::

;; ## Reading the Diagram

;; **Red: concentration compounds.**
;; Concentrated wealth buys asset stores (housing, land, and shares of firms with market power),
;; which pay capital income, which is taxed more lightly than wages, which adds to concentrated wealth.
;; Two shortcuts strengthen the loop in Canada.
;; Inheritance passes the stock to the next generation untaxed, except for the capital gains
;; triggered by the deemed disposition at death.
;; Market power raises the return on what is already owned without anyone building anything new.

;; **Green: participation grows.**
;; Labour income pays for consumer spending, which justifies productive capacity, which pays labour income.
;; Innovation produces knowledge, which raises productive capacity, which pays for more innovation.
;; Business investment feeds this loop.
;; [The Investment Question](c7_investment_question.html) shows that Canada's investment problem
;; sits in the market-power box, not the tax box.

;; **Cross links: where the loops collide.**
;; Housing is where the red loop hits households hardest.
;; When asset owners bid up homes, everyone else takes on more household debt to keep up.
;; That debt crowds out spending and squeezes living standards
;; (see [Labour Hours](c3_labour_hours.html)).
;; Canadian households carry among the highest debt loads in the G7, nearly all of it mortgages.

;; **Blue: the counterweight.**
;; Public services are Canada's main defence against the red loops.
;; Health insurance not tied to a job, child benefits, and public pensions keep class mobility high.
;; But tax revenue comes mainly from labour income.
;; Capital income flows into revenue only "lightly" (the dashed arrow).
;; So the counterweight is paid for by the same people the red loop is squeezing.
;; That is not a stable arrangement.

;; > "Courage, my friends; 'tis not too late to build a better world." -- Tommy Douglas

;; ## Where to Intervene

;; The point of a loop diagram is to find the arrows worth cutting.
;; Several are within Ottawa's reach:

;; | Arrow | Canadian lever |
;; | :--- | :--- |
;; | Capital Income → Wealth Concentration | Tax wealth growth like wages: a net wealth tax on fortunes above \$10M–\$20M, or a higher capital gains inclusion rate |
;; | Inheritance → Wealth Concentration | An inheritance or estate tax on very large estates (Canada abolished its own in 1972) |
;; | Policy → Tax Preferences | Review the largest tax expenditures, such as the partial inclusion of capital gains |
;; | Market Power → Capital Income | Competition law: the 2022–2024 Competition Act amendments were a first step |
;; | Revenue → Public Services | Broaden the base so the counterweight isn't paid for only by labour income |
;;
;; : Arrows and the Canadian levers that act on them {#tbl-levers}

;; The rest of the series puts numbers on these arrows.

;; ## Conclusion

;; Canada's version of the system has the same core loop as the American one:
;; wealth → assets → capital income → wealth.
;; Canada adds a housing loop that ties middle-class wealth to rising prices,
;; a market-power loop that pays incumbents without new investment,
;; and an inheritance loop left untaxed since 1972.
;; Canada's public services are a genuine counterweight, but they are funded by the people least able to absorb more tax.

;; In the next part, [Wealth Concentration](c5_wealth_concentration.html), we measure how concentrated Canadian wealth has become.

;; [^1]: Timothy Pratley, [Unbounded: Interactive Factors](https://clojurecivitas.org/economics/wealth_distribution/unbounded/u4_interactive_factors.html), ClojureCivitas, 2026.
