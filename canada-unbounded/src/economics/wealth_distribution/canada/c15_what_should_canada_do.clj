^:kindly/hide-code
(ns economics.wealth-distribution.canada.c15-what-should-canada-do
  {:kindly/options {:kinds-that-hide-code #{:kind/var :kind/hiccup2 :kind/fn}}
   :clay {:title  "Unbounded North: So What Should Canada Do?"
          :quarto {:sidebar     "unbounded-north"
                   :author      "Kira Howe"
                   :description "What the evidence in this series adds up to, and why Canada's answer differs from America's."
                   :abstract    "Canada's next generation faces the same squeeze as America's, but Canada's problem is less a few vast fortunes than wealth parked where it doesn't have to compete. The fix is to tax what doesn't move and doesn't compete, and to stop protecting it."
                   :date        "2026-10-25"
                   :type        :post
                   :category    :economics
                   :tags        [:tax :economics :growth :canada]}}})

;; ## The Same Worry, a Different Diagnosis

;; Tim Pratley's *Unbounded* ends where it begins: with the quality of life of the next generation,
;; and a wealth tax as the way to keep opportunity circulating.
;; The worry carries straight across the border.
;; The diagnosis, and so the remedy, comes out differently.

;; ## What We Found

;; 1. **The next generation is being squeezed.** House prices relative to income have roughly doubled since 1970,
;;    real median employment income has grown about 7% since 1976, young adults are less likely to own a home at each census,
;;    and household debt is 176% of disposable income ([Labour Hours](c03_labour_hours.html)).
;;    Canada still keeps more poor children out of poverty than the United States, but Canadian mobility has declined for every birth cohort from the 1960s to the 1980s
;;    ([Talent and Opportunity](c12_talent.html)).
;; 2. **Canada's top wealth is concentrated, but less extremely, and not sharply rising.**
;;    The top 1% of families hold about a quarter of the wealth and the bottom 40% about 3%,
;;    but survey-based shares have been fairly stable since 2010 ([Wealth Concentration](c05_wealth_concentration.html)).
;;    Billionaire fortunes have grown fastest; the broad middle has held on through home equity.
;; 3. **The bigger problem is that Canada has stopped getting more productive.**
;;    Canadians produce about 30% less per hour than Americans, and almost all of the widening gap is efficiency, not equipment.
;;    The same banks and resource firms stay on top, and too few young firms grow large ([Stable and Stuck](c06_productivity.html)).
;; 4. **Canadian capital goes into land.** Land has grown from a quarter to over a third of everything Canada owns,
;;    and the tax system rewards it: tax-free gains on homes, half-taxed capital gains, low taxes on land itself ([Land](c10_land.html)).
;; 5. **Old money is protected by the tax code.** Many of Canada's largest firms are still family-controlled,
;;    often through multiple-voting shares, and Canada has had no estate tax since 1972 and no provincial succession duties since 1985 ([Old Money](c07_old_money.html)).
;; 6. **Taxing capital less didn't buy investment.** Canada cut its corporate tax rate deeply from 2000 to 2012;
;;    business investment didn't rise as a share of the economy, and the productivity gap kept growing ([The Investment Question](c09_investment_question.html)).

;; These problems share roots.
;; The same features that let wealth compound without competing (land, protected incumbents, untaxed inheritance, lightly taxed gains)
;; are the ones institutions studying Canada's productivity point to.
;; The series doesn't show that concentration *causes* stagnation; it shows they grow from the same soil.

;; ## Two Kinds of Stability

;; Canada's stable institutions are a genuine achievement.
;; Its banks needed no bailout in 2008, Canadians collectively own large, well-managed public pension funds,
;; and public health insurance, child benefits and pensions are a big part of why it is more equal than the United States.
;; Those are worth protecting.

;; What isn't worth protecting is the stability of *incumbents*: the same firms, families and landowners holding the same positions,
;; sheltered from competition and from tax.
;; Canada has often treated the two kinds of stability as one.
;; The policy question is how to keep the first while ending the second.

;; ## A Canadian Toolkit

;; Tim's prescription targets the *stock* of extreme wealth.
;; For Canada, the evidence points at *where* wealth sits and what shields it.
;; The principle: **tax what doesn't move and doesn't compete, and stop protecting it.**

;; | Problem | Tool | Why it fits Canada | The hard part |
;; | :--- | :--- | :--- | :--- |
;; | Capital flows into land | Shift property tax from buildings onto land, revenue-neutral at first | Land is over a third of national assets; the OECD ranks property taxes least harmful to growth; western cities did this a century ago | Cash-poor owners (defer the tax until sale); political resistance |
;; | Fortunes pass on untaxed | Tax large inheritances, above a high lifetime threshold | Aims at heir control and old money directly; most OECD countries do it | Valuing private firms and farms; Canada would be starting from nothing |
;; | Gains taxed less than wages | Tax capital gains more like wages, with relief for lock-in | Half-taxing gains cost an estimated \$29.5B in 2023, mostly benefiting the top | The 2024 attempt was cancelled in 2025 |
;; | Extreme fortunes | An annual wealth tax on families above \$10M | Touches fewer than 1 family in 150; the PBO estimates about \$23B a year | Avoidance and valuation; good design matters more than the rate |
;; | Protected incumbents | Stronger competition law, entry in banking, telecom, groceries and airlines; fewer internal trade barriers; limits on dual-class control | Institutions studying Canada's productivity blame weak competition first | Incumbents are politically powerful |
;; | Unused talent | Faster recognition of foreign credentials, especially in health care | Recent immigrants are twice as likely as the Canadian-born to hold a degree, and a quarter of foreign-degree holders work in jobs needing high school | Provincial licensing bodies control it |
;;
;; : Canada's problems and the tools that fit them {#tbl-toolkit}

;; Nothing here requires choosing between them.
;; A wealth tax and a land tax reach different people: one the very richest families, the other everyone who owns valuable land, including the middle class.
;; That is the political problem with the land tax and its economic strength, which is why it should come as a *swap*, not an addition.

;; ## Swap, Don't Just Add

;; The strongest version of this agenda raises little new revenue on balance.
;; It moves the tax burden from work, buildings and new investment onto land, large inheritances and extreme fortunes.
;; The revenue could cut income taxes on lower-paid work, or speed up write-offs for new business investment,
;; so the whole package pushes money away from land and incumbency and toward new firms and the people who work in them.

;; That framing matters for the investment objection.
;; The question isn't whether to tax capital more or less, but *which* capital:
;; Canada has already cut taxes on corporate profits, and it taxes old wealth and land lightly too.
;; It should tax old wealth and land more, so that taxes on new investment can stay low.

;; ## What This Wouldn't Fix

;; It's worth being plain about the limits.

;; * **Productivity has many causes.** Canada's small domestic market, its resource economy, its regulatory overlap and its proximity
;;   to a richer neighbour all matter, and no tax change fixes them.
;; * **Some of the evidence cuts the other way.** Canada's measured wealth shares have been flat since 2010;
;;   average pay has kept up with productivity since the late 1990s (it is the median that lags);
;;   the evidence on dual-class shares is mixed; and wealthy heirs do not earn low returns.
;; * **A land tax is hard to bring in.** Existing owners bear the cost as land prices adjust, so it has to be phased in slowly,
;;   with deferral for people who are house-rich and cash-poor.
;; * **Early results are early.** The current government's measures are mostly too new to judge ([Scorecard](c13_scorecard.html)),
;;   though on taxing land, inheritance and capital gains its direction so far is the opposite of this agenda.

;; ## For the Next Generation

;; The original series asked what kind of economy gives the next generation the best chance.
;; Canada's answer has to start from what it already does well: stable institutions and strong public programs, paid for mostly by taxes on work.
;; What it does badly is let wealth settle where it doesn't have to compete: in land, in protected industries, in family control passed down untaxed.
;; A country that taxed that wealth more and work less, and made its incumbents compete,
;; would keep the stability that matters and lose the kind that keeps it stuck.

;; As for how to get there, [How to Get a Wealth Tax](c14_how_to_get_a_wealth_tax.html) applies to all of it:
;; in Canada, only Cabinet can start a tax, and what moves Cabinet is pressure on the governing party.

;; The final part, [References](c16_references.html), lists the data and research behind every claim.
