^:kindly/hide-code
(ns economics.wealth-distribution.canada.c14-how-to-get-a-wealth-tax
  {:kindly/options {:static true
                    :kinds-that-hide-code #{:kind/var :kind/hiccup2 :kind/fn :kind/graphviz}}
   :clay {:title  "Unbounded North: How to Get a Wealth Tax"
          :quarto {:sidebar     "unbounded-north"
                   :author      "Kira Howe"
                   :description "How a wealth tax could actually become law in Canada's parliamentary system."
                   :abstract    "In Canada only Cabinet can start a tax. A wealth tax needs pressure that changes what the governing party believes it must do."
                   :date        "2026-10-24"
                   :type        :post
                   :category    :economics
                   :tags        [:tax :economics :growth :canada]}}})

;; ## Popular, but Not on the Agenda

;; A wealth tax is popular in Canada.
;; In 2020, 79% of Canadians supported a 1% tax on wealth above \$20M, including 64% of Conservative voters
;; (see [Wealth Tax](c02_wealth_tax.html)).
;; Yet when the NDP put a wealth tax motion to the House of Commons in November 2020, it was defeated 27 to 292.
;; Liberal, Conservative and Bloc MPs voted against it.[^1]
;; In 2025, the government cancelled even the much smaller increase in the capital gains inclusion rate.[^2]

;; The original series described the American path to law: bills, cosponsors, committee, discharge petitions.[^3]
;; Canada's path is different, and in some ways narrower.

;; ## How a Tax Becomes Law in Canada

;; ::: {#fig-path-to-law-canada .graphviz fig-align="center"}

^:kind/graphviz
["digraph canada_path {
  graph [rankdir=TB, bgcolor=\"transparent\", nodesep=0.3, ranksep=0.45, fontname=\"Helvetica\"]
  node [shape=box, style=\"rounded,filled\", fillcolor=\"#f4f1ea\", color=\"#8b7246\", fontname=\"Helvetica\", fontsize=11]
  edge [color=\"#777777\", fontname=\"Helvetica\", fontsize=9]

  opinion  [label=\"Public opinion\"]
  party    [label=\"Party platforms\\n(PBO costings)\"]
  election [label=\"Election\\n(next fixed date: 2029)\"]
  caucus   [label=\"Governing party\\ncaucus & Cabinet\"]
  minority [label=\"Minority Parliament:\\nopposition leverage\", fillcolor=\"#d6e4f0\"]
  wm       [label=\"Ways and Means motion\\n(only a Minister can move it)\", fillcolor=\"#f6c9c3\"]
  budget   [label=\"Budget / budget\\nimplementation bill\"]
  senate   [label=\"Senate\"]
  law      [label=\"Royal Assent\", fillcolor=\"#bfdbfe\"]

  opinion -> party -> election
  election -> caucus [label=\"majority\"]
  election -> minority [label=\"minority\"]
  minority -> caucus [label=\"confidence & supply\"]
  opinion -> caucus [label=\"sustained pressure\", style=dashed]
  caucus -> wm -> budget -> senate -> law
}"]

;; The legislative path for a federal tax. The red box is the gate: only Cabinet can open it.
;; :::

;; In Canada, **only a Cabinet minister can start a tax.**
;; Before any bill that imposes a tax can be introduced, the House must adopt a *Ways and Means* motion,
;; and "a ways and means motion may therefore be moved only by a Minister of the Crown."[^4]
;; A private member's bill that would raise anyone's taxes cannot even be introduced without one.
;; The Constitution requires tax bills to start in the House of Commons, not the Senate.[^5]

;; So there is no Canadian equivalent of a backbench wealth tax bill gathering cosponsors.
;; Opposition MPs and backbenchers can:

;; * move non-binding motions (like the NDP's 2020 opposition-day motion, or Liberal MP Nathaniel Erskine-Smith's
;;   2021 motion M-68 for a one-time tax on extreme wealth);
;; * ask the **Parliamentary Budget Officer** to cost a proposal, which puts an independent revenue estimate on the record;
;; * sponsor petitions and raise the issue in committees.

;; But the decision belongs to the governing party.

;; ## Minority Parliaments Are Where Things Happen

;; Canada's best recent example of turning popular demands into law came from a minority Parliament.
;; In March 2022, the Liberals and the NDP signed a "supply and confidence" agreement:
;; the NDP would support the government on budgets and confidence votes, and in exchange the government would deliver
;; specific programs.[^6]
;; It produced the Canadian Dental Care Plan, the *Pharmacare Act*, and,
;; under the heading "a fairer tax system", the 2022 Canada Recovery Dividend: a one-time 15% tax on the largest banks' and
;; insurers' profits above \$1B, plus a permanent surtax.[^7]
;; It did not produce a wealth tax.

;; That leverage is not available today.
;; The 2025 election returned a Liberal minority with 169 seats, and the NDP fell to 7 seats, losing official party status.[^8]
;; Through floor crossings and by-elections, the Liberals won a majority in April 2026 (174 of 343 seats; 173 in October).[^9]
;; In March 2026 the NDP elected Avi Lewis as leader, on a platform that includes a 1/2/3% wealth tax above \$10M
;; and an inheritance tax on estates over \$5M.[^10]
;; The next fixed-date election is expected in October 2029.

;; **The arithmetic is plain: in this Parliament, a wealth tax happens only if the governing party decides to do it.**

;; ## What Moves a Governing Party

;; Majority governments respond to things that threaten their next majority.
;; In practice that means:

;; 1. **Voters in swing ridings who make it a voting issue.** Popularity isn't the same as priority.
;;    A wealth tax that 79% support but almost nobody votes on gives a government no reason to act.
;; 2. **Fiscal pressure.** Debt charges are rising toward 13% of federal revenue by 2030–31
;;    (see [Wealth Tax](c02_wealth_tax.html)). Every government will need new revenue or cuts.
;;    The question is who pays.
;; 3. **Credible, costed proposals.** The PBO's estimate of \$121.5B over five years, with a cautious avoidance
;;    assumption, makes the proposal hard to dismiss as fantasy.
;; 4. **Allies inside the caucus.** Liberal MPs have proposed wealth taxes before (motion M-68).
;; 5. **International cover.** Canada joined the OECD global minimum tax on multinationals in 2024.
;;    G20 work on minimum taxation of billionaires, led by Brazil in 2024 and drawing on economist Gabriel Zucman's
;;    blueprint, offers a similar coordinated route.[^11]

;; ## A Provincial Route?

;; Provinces can levy direct taxes, and a net wealth tax is a direct tax.[^5]
;; A province could, in principle, go first, as provinces once levied succession duties.
;; But the evidence from Spain and Switzerland is that the very wealthy move to the lowest-tax region,
;; and Canadians move between provinces easily.
;; A provincial wealth tax would be a useful signal and a difficult revenue source.
;; The federal route is the one that works.

;; ## One Question

;; The original series argued that single-issue movements win by asking one simple question that crosses party lines.[^3]
;; In Canada the question has to be aimed at the people who can actually open the gate:

;; > *Will your party commit to a costed tax on fortunes above \$10 million in its next platform, and will you vote for it?*

;; Ask it of every candidate in every party, especially in the governing party.
;; Use the tools that exist:

;; * **Research.** Read the PBO's costings and the evidence on avoidance and investment.
;; * **Share.** Use facts and comparisons; the data in this series is all public and reproducible.
;; * **Petition.** A House of Commons e-petition needs 500 signatures to be presented, and the government must respond in writing within 45 days.[^12]
;; * **Vote.** Make it a voting issue, and say so.

;; ::: {.callout-note}
;; ## A Closer Look: The Last Time Canada Rewrote Its Tax System

;; The Carter Commission reported in 1966 that income should be taxed the same however it is earned,
;; an idea summed up in the phrase attributed to its chair, "a buck is a buck."
;; The report met fierce opposition.
;; The 1971 reform kept the core idea (capital gains would be taxed for the first time) but taxed only half of them,
;; and abolished the federal estate tax in the same package.[^13]
;; The lesson cuts both ways.
;; A big idea can move Canadian tax policy a long way.
;; And the details are decided by whoever is still paying attention after the headlines move on.
;; :::

;; ## Conclusion

;; In Canada, a wealth tax needs Cabinet.
;; Minority Parliaments are where small parties can force change, but the current Parliament is effectively a majority.
;; The path runs through the governing party's next platform, which means voters who treat it as a priority,
;; costed proposals that survive scrutiny, and allies inside the party.
;; Public support is already there; the missing piece is pressure.

;; In the next part, [So What Should Canada Do?](c15_what_should_canada_do.html), we pull the series together.

;; [^1]: House of Commons, [Vote No. 22, 43rd Parliament, 2nd Session](https://www.ourcommons.ca/members/en/votes/43/2/22), November 2020 (27 yeas, 292 nays); [CBC News](https://www.cbc.ca/news/politics/ndp-wealth-tax-opposition-day-1.5789923).
;; [^2]: [Prime Minister's statement, March 21, 2025](https://www.pm.gc.ca/en/news/news-releases/2025/03/21/prime-minister-mark-carney-cancels-proposed-capital-gains-tax-increase).
;; [^3]: Timothy Pratley, [Unbounded: How to Get a Wealth Tax](https://clojurecivitas.org/economics/wealth_distribution/unbounded/u7_how_to_get_a_wealth_tax.html), ClojureCivitas, 2026.
;; [^4]: [*House of Commons Procedure and Practice*, 3rd ed. (2017), Chapter 18, "The Business of Ways and Means"](https://www.ourcommons.ca/procedure/procedure-and-practice-3/ch_18_4-e.html).
;; [^5]: [*Constitution Act, 1867*](https://laws-lois.justice.gc.ca/eng/const/page-3.html), ss. 53–54, 91(3) and 92(2).
;; [^6]: Prime Minister of Canada, ["Delivering for Canadians Now"](https://www.pm.gc.ca/en/news/news-releases/2022/03/22/delivering-canadians-now), March 22, 2022.
;; [^7]: LEGISinfo: [Bill C-31 (2022)](https://www.parl.ca/legisinfo/en/bill/44-1/C-31) (interim dental benefit), [Bill C-64 (2024)](https://www.parl.ca/legisinfo/en/bill/44-1/C-64) (*Pharmacare Act*), [Bill C-32 (2022)](https://www.parl.ca/legisinfo/en/bill/44-1/C-32) (Canada Recovery Dividend).
;; [^8]: Elections Canada, [Official Voting Results, 45th General Election](https://www.elections.ca/res/rep/off/ovrGE45/62/table8E.html).
;; [^9]: House of Commons, [current Members of Parliament](https://www.ourcommons.ca/Members/en/search), retrieved October 10, 2026: Liberal 173, Conservative 137, Bloc Québécois 21, NDP 5, Green 1. The Liberals had 174 seats after the April 13, 2026 by-elections, as reported by Canadian news outlets.
;; [^10]: New Democratic Party, ["Avi Lewis elected leader"](https://www.ndp.ca/news/avi-lewis-elected-leader-ndp), March 2026; [Lewis campaign tax plan](https://lewisisleader.ca/ideas/tax-policy-full-plan/).
;; [^11]: Gabriel Zucman, [A blueprint for a coordinated minimum effective taxation standard for ultra-high-net-worth individuals](https://www.gov.br/g20/pt-br/trilhas/trilha-de-financas/tributacao_internacional/2-blueprint-for-a-coordinated-minimum-effective-taxation-standard-for-ultra-high-net-worth-individuals-gabriel-zucman.pdf/@@download/file), report commissioned by the Brazilian G20 presidency, 2024; [Bill C-69 (2024)](https://www.parl.ca/legisinfo/en/bill/44-1/C-69), *Global Minimum Tax Act*.
;; [^12]: House of Commons, [e-petitions](https://www.ourcommons.ca/petitions/en/Home/Index).
;; [^13]: Vaillancourt and Kerkhoff, ["Capital gains taxation in Canada, 1972–2017"](https://www.unsw.edu.au/content/dam/pdfs/business/acct-audit-tax/research-reports/past-issues/bus-2019/2019-volume-16-number-2/BUS-2019-V16-paper1-v16-n2.pdf), *eJournal of Tax Research* 16(2), 2019. The phrase "a buck is a buck" is attributed to Carter.
