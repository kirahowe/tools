^:kindly/hide-code
(ns economics.wealth-distribution.canada.c07-old-money
  {:kindly/options {:static true
                    :kinds-that-hide-code #{:kind/var :kind/hiccup2 :kind/fn}}
   :clay {:title  "Unbounded North: Old Money"
          :quarto {:sidebar     "unbounded-north"
                   :author      "Kira Howe"
                   :description "From Crown land grants to family holding companies: Canada's dynastic wealth, the evidence on inherited control, and political families."
                   :abstract    "Canada began as a colony where land and office went to the well-connected. Its largest firms are still unusually likely to be family-controlled, and the evidence says heir control is bad for growth."
                   :date        "2026-10-17"
                   :type        :post
                   :category    :economics
                   :tags        [:tax :economics :growth :canada :inheritance]}}}
  (:require [scicloj.plotje.api :as pj]
            [tablecloth.api :as tc]
            [economics.wealth-distribution.canada.common :as c]))

;; ## A Colony Built on Grants

;; Canada began as a set of colonies where the Crown owned the land, and handed it out.
;; How it handed it out shaped who got rich.

;; **New France** was organised under the seigneurial system: large grants to seigneurs, who collected rents from the
;; farmers (*censitaires*) who worked them.
;; Quebec abolished the system in 1854, but the remaining rents were only finally bought out in the 20th century,
;; and the buy-out was repaid through a local property tax until 1981.[^1]

;; **Rupert's Land**, a third of today's Canada, was granted by Charles II to the Hudson's Bay Company in 1670.
;; When Canada took it over in 1870, it paid the company £300,000, and the company kept land around its posts plus the right to claim
;; "grants of land not exceeding one twentieth part" of the fertile belt of the Prairies.[^2]

;; In **Upper Canada** (now Ontario), the 1791 Constitutional Act set aside a seventh of all granted land for the Protestant clergy,
;; and a similar share was reserved for the Crown.
;; In 1839, Lord Durham's report to the British government described how the colony's governing clique, the "Family Compact,"
;; had used its control of the land-granting councils:
;; "by grant or purchase, they have acquired nearly the whole of the waste lands of the Province;
;; they are all-powerful in the chartered banks, and, till lately, shared among themselves almost exclusively
;; all offices of trust and profit."[^3]

(def durham
  (-> (c/data "dyn_durham_upper_canada_land_grants_1839.csv")
      (tc/select-rows #(and (= "Upper Canada" (:province %)) (number? (:acres %))))
      (tc/map-columns :group [:recipient_category]
                      {"United Empire Loyalists (refugees settled before 1787) and their children" "Loyalists and their children"
                       "Militiamen" "Militiamen"
                       "Discharged soldiers and sailors" "Discharged soldiers and sailors"
                       "Magistrates and barristers" "Magistrates and barristers"
                       "Executive councillors and their families" "Executive councillors and families"
                       "Five legislative councillors and their families" "Five legislative councillors and families"
                       "Clergymen as private property" "Clergymen (private)"
                       "Persons contracting to make surveys" "Survey contractors"
                       "Officers of the army and navy" "Army and navy officers"
                       "Endowment of schools" "School endowment"
                       "Colonel (Thomas) Talbot" "Colonel Thomas Talbot"
                       "Heirs of General Brock" "Heirs of General Brock"
                       "Doctor Mountain, former Bishop of Quebec" "Dr. Mountain, former Bishop of Quebec"})
      (tc/map-columns :elite [:group]
                      #(if (#{"Executive councillors and families" "Five legislative councillors and families"
                              "Magistrates and barristers" "Survey contractors" "Colonel Thomas Talbot"
                              "Heirs of General Brock" "Dr. Mountain, former Bishop of Quebec"} %) c/o c/w))
      (tc/map-columns :label [:acres] #(format "%,d" (long %)))
      (tc/order-by [:acres])))

^{:kindly/options {:id :fig-durham
                   :caption "Land granted in Upper Canada by type of recipient, as listed in Lord Durham's 1839 report (acres). Orange: officials, the legal elite and other individuals. The clergy reserves (about one-seventh of granted land) are not included.[^3]"}}
(-> durham
    (pj/lay-bar :acres :group {:color {:column :elite :scale false}})
    (pj/lay-label {:text :label})
    (pj/options {:title "Who Got the Land in Upper Canada, to 1838"
                 :x-label "Acres" :y-label ""}))

;; Five legislative councillors and their families received 50,000 acres; the executive councillors, 136,000;
;; one man, Colonel Talbot, 48,520.
;; Together with the clergy reserves, grants had absorbed "nearly half of all the surveyed land in the Province,"
;; and "perhaps less than a tenth" of it was occupied by settlers.[^3]
;; Durham's verdict was that Upper Canada's public lands had been "alienated by the Government" before settlers arrived.

;; After Confederation, the pattern repeated on a continental scale.
;; The Canadian Pacific Railway received \$25 million and **25 million acres** in 1881.[^4]
;; A homesteader under the Dominion Lands Act received **160 acres** for a \$10 fee.[^5]
;; Under Treaties 1, 2 and 5, a First Nations family of five was allotted reserve land on the same scale: 160 acres.[^6]

;; This is history, not genealogy.
;; Few of today's fortunes can be traced in a straight line to a colonial land grant,
;; and the Hudson's Bay Company itself filed for creditor protection and closed its last stores in 2025.[^2]
;; But it set a pattern that Canada has never fully broken:
;; wealth that comes from *access* (to land, licences, charters, tariffs and protected markets) more than from competition.
;; John Redpath, who opened Canada's first sugar refinery in 1855, is a documented example:
;; his fortune came from public-works contracts on the Lachine and Rideau canals, land bought from the Sulpicians
;; (Montreal's seigneurs), and tariffs "conveniently established by the Canadian government in 1855 – the year Redpath's factory opened."[^7]

;; ## Canada's Family Firms

;; Some of Canada's largest companies are still controlled by the families that founded them,
;; often through holding companies and shares that carry extra votes.
;; In 1988, only 53 of 246 large public Canadian companies in the Financial Post 500 were widely held;
;; 44 were controlled by heirs.[^13]

;; | Family | Business (founded) | How control is held |
;; | :--- | :--- | :--- |
;; | Thomson | Thomson Reuters (first newspaper 1934) | The Woodbridge Company owns about 70% of the shares |
;; | Weston | George Weston Ltd., Loblaw (1882) | Wittington Investments owns about 59% |
;; | Desmarais | Power Corporation | 15.6% of the shares carry 51.9% of the votes |
;; | Rogers | Rogers Communications | A family trust holds 97.5% of the voting shares, about 29% of all shares |
;; | Bombardier–Beaudoin | Bombardier | About 11% of the shares carry 47.8% of the votes |
;; | Sobey | Empire Company (Sobeys) | A family holding company held 92.7% of voting shares (2018) |
;; | Irving | J.D. Irving (1882), Irving Oil (1924) | Private |
;; | McCain | McCain Foods (1957) | Private |
;; | Richardson | James Richardson & Sons (1857) | Private |
;;
;; : Selected Canadian family business dynasties {#tbl-dynasties}[^8]

(def votes-vs-shares
  "From company filings: share of votes vs share of total equity held by the controlling family."
  (tc/dataset {:company ["Rogers" "Power Corp." "Bombardier" "Thomson Reuters" "George Weston"]
               :votes   [97.5 51.94 47.83 70.0 59.0]
               :shares  [29.0 15.64 11.0 70.0 59.0]}))

^{:kindly/options {:id :fig-votes-vs-shares
                   :caption "Share of votes and share of all shares held by the controlling family, from company filings (Rogers 2021; Power Corporation 2024; Bombardier 2025; Thomson Reuters 2025; George Weston 2025). Where the bars are equal, the family controls through majority ownership; where they diverge, through multiple-voting shares.[^8]"}}
(-> votes-vs-shares
    (tc/pivot->longer [:votes :shares] {:target-columns :measure :value-column-name :pct})
    (tc/update-columns {:measure (partial map {:votes "Share of votes" :shares "Share of all shares"})})
    (pj/lay-bar :company :pct {:color :measure :position :dodge})
    (pj/scale :color {:values [c/o c/w]})
    (pj/scale :y {:domain [0 100]})
    (pj/options {:title "Control Without Ownership"
                 :x-label "" :y-label "%" :color-label "" :legend-position :bottom}))

;; Multiple-voting shares let a family keep control of a company long after it has sold most of the equity.
;; In 2021, Edward Rogers used the family trust's control to replace five directors of Rogers Communications in a single written resolution,
;; and the BC Supreme Court upheld it.[^9]
;; About 100 TSX companies had dual-class shares in 2005 and about 69 in 2019.[^10]
;; Founder-led structures can be different.
;; Shopify's 2022 founder share gives Tobi Lütke about 40% of the votes, but it ends if he leaves his role:
;; it cannot be inherited.[^11]

;; Forbes's billionaire list, which leaves out several of these families entirely, still finds that
;; 16 of the 82 Canadian billionaires inherited their fortunes; the Thomson family alone accounts for US\$43.5B
;; of the US\$75.5B held by inheritors.[^12]
;; In 1993, when economists hand-classified every Canadian on the Forbes list, they found no entrepreneur billionaires at all.[^13]

;; ## Does It Matter Who Runs the Firm?

;; The best research distinguishes **founders**, who built a firm, from **heirs**, who inherited control of it.
;; Founder-run firms often do well.
;; Heir-run firms, on average, don't.

;; | Study | Setting | Finding |
;; | :--- | :--- | :--- |
;; | Morck, Stangeland & Yeung (2000) | Canada, 246 large firms; 41 countries | Heir-controlled Canadian firms earn lower returns, use more capital per worker, and do less R&D. Countries where inherited billionaire wealth is large relative to GDP grow more slowly. |
;; | Bennedsen, Nielsen, Pérez-González & Wolfenzon (2007) | Denmark, all CEO successions | Passing a firm to a family member *causes* operating profitability to fall by at least 4 percentage points. |
;; | Pérez-González (2006) | US CEO successions | Firms that appoint a family CEO underperform, especially when the heir did not attend a selective college. |
;; | Villalonga & Amit (2006) | US Fortune 500 | Family ownership creates value only while the founder is involved. "When descendants serve as CEOs, firm value is destroyed." |
;; | Smith & Amoako-Adu (1999) | Canadian family firms | Share prices fall when heirs take over, especially young heirs (as summarised by Morck, Wolfenzon & Yeung, 2005). |
;; | Anderson & Reeb (2003) | US S&P 500 | *Counter-evidence:* family firms outperform, a result later work attributes mostly to founder-led firms. |
;; | Jog, Zhu & Dutta (2010) | Canadian dual-class firms | *Counter-evidence:* no sign that restricted-voting firms underperform in Canada. |
;;
;; : Evidence on inherited control and firm performance {#tbl-heir-studies}[^14]

;; The Canadian study is the one that coined a name for the problem.
;; Morck, Stangeland and Yeung described "the Canadian disease" as
;; "poor overall management quality, capital markets and institutions that channel money to large, established family firms,
;; a dearth of innovation that locks in the status quo, and political rent seeking that deters entry."[^13]
;; Their natural experiment is telling.
;; When the 1988 election made the Canada–US Free Trade Agreement certain, the share prices of heir-controlled firms fell the most
;; and those of entrepreneur-controlled firms rose the most.
;; The market expected competition to hurt protected heirs.

;; Two cautions.
;; First, the evidence on *heirs* is strong; the evidence on dual-class shares as such is mixed, and Canada's
;; minority-shareholder protections are relatively good.
;; Second, it is not true that inherited fortunes earn low returns.
;; Swedish data show that wealthy heirs tend to keep their inheritances intact and earn higher returns than average.[^15]
;; The problem is less that old money loses money than that it protects old capital, and old markets, at the economy's expense.

;; ## Inheritance, Untaxed

;; Canada abolished its federal estate tax in 1972, and Quebec, the last province with succession duties, abolished them in 1985.
;; Today 24 of the 37 OECD countries tax inheritances or estates; Canada is one of the few rich countries that repealed its tax.[^16]
;; Statistics Canada finds that inheritances are growing and unequal: the median inheritance received by homeowners rose
;; from \$67,000 in 2019 to \$85,100 in 2023, and 4 in 10 homeowners had some help from family in buying their home.[^17]
;; No one has estimated what share of Canadian wealth is inherited, which is itself a gap worth closing.

;; ## Political Dynasties

;; Canada also has political families.
;; Pierre and Justin Trudeau were both Prime Minister, and Paul Martin followed his father, a long-serving
;; Liberal cabinet minister, into Parliament before becoming Prime Minister himself.
;; Provinces have them too: the Johnsons in Quebec (a father and two sons as premier, for different parties),
;; the Bennetts in British Columbia, the Ghizes in Prince Edward Island, the Mannings in Alberta, and three generations of Fords
;; in Ontario.[^18]

(def kinship
  (-> (c/data "dyn_parliament_kinship_counts_gagnon2011.csv")
      (tc/update-columns {:relationship (partial map {"brothers" "Brothers" "fathers_and_sons" "Fathers and sons"})})
      (tc/map-columns :period [:period] #(subs % 0 4))))

^{:kindly/options {:id :fig-kinship
                   :caption "Members of Parliament and senators entering Parliament who had a father/son or brother who also sat, by political era (period start shown). From Library of Parliament data compiled by Gagnon (2011).[^19]"}}
(-> kinship
    (pj/lay-bar :period :n_parliamentarians_starting_career {:color :relationship :position :dodge})
    (pj/scale :color {:values [c/w c/o]})
    (pj/options {:title "Family Ties in Parliament, by Era"
                 :x-label "Era beginning" :y-label "Parliamentarians"
                 :color-label "" :legend-position :bottom}))

;; The trend is encouraging.
;; Library of Parliament data show 94 father–son entries in Parliament's first three decades and 37 between 1968 and 2011.
;; More than 100 sons inherited their father's riding.[^19]
;; Canada's political dynasties are fewer than they were, and comparative research suggests most democracies
;; have dynasties below 10% of legislators.[^20]
;; The deeper concern is the connection between economic and political power.
;; In the United States, holding a seat in Congress for more than one term raises the chance that a relative later enters Congress by
;; about 70%: "power begets power."[^21]
;; In Canada, a 2005 review of corporate governance noted that the controlling shareholder of one of the country's largest
;; corporate groups was a close in-law of one former Prime Minister and a long-time business associate of another,
;; while adding that "there is no hint of corruption in many of these family ties."[^22]
;; The problem isn't corruption.
;; It's that a small, interconnected elite shapes the rules of a small, concentrated economy.

;; ::: {.callout-note}
;; ## A Closer Look: The Irving Newspapers

;; For decades, the Irving family owned every English-language daily newspaper in New Brunswick, alongside its oil, forestry,
;; shipbuilding and transportation businesses.
;; Federal prosecutors charged the newspaper consolidation as an illegal combine; in 1976 the Supreme Court of Canada upheld the
;; Irvings' acquittal, a ruling the government's own competition director said ended hopes for the criminal prohibition of
;; mergers as an effective tool.[^23]
;; The papers were sold to Postmedia in 2022.
;; :::

;; ## Conclusion

;; Canada's founding pattern was wealth through access: land grants, charters, contracts and tariffs.
;; Many of Canada's largest companies are still family-controlled, often through shares that
;; separate votes from ownership, and the best evidence, much of it Canadian, says heir control is bad for growth.
;; With no inheritance tax, nothing in the tax system slows the transfer of that control.
;; Political dynasties are fading, but economic and political elites remain closely tied.
;; Old money isn't the whole story of Canada's productivity problem, but it is part of the "status quo" that keeps it in place.

;; In the next part, [Economic Growth](c08_economic_growth.html), we look at how circulation and participation drive growth.

;; [^1]: F. Roy, review of Grenier and Morissette (eds.), *Nouveaux regards en histoire seigneuriale au Québec*, [*Cahiers de géographie du Québec* 61(173), 2017](https://www.erudit.org/fr/revues/cgq/2017-v61-n173-cgq03845/1049393ar/).
;; [^2]: [Rupert's Land and North-Western Territory Order](https://www.justice.gc.ca/eng/rp-pr/csj-sjc/constitution/lawreg-loireg/p1t31.html), June 23, 1870, Schedule. On the 2025 closure, see [CBC News](https://www.cbc.ca/1.7551035).
;; [^3]: Lord Durham, *Report on the Affairs of British North America* (1839), C. P. Lucas edition (Oxford, 1912), vol. 2, pp. 148, 220–223 ([full text](https://www.fadedpage.com/showbook.php?pid=20210933)).
;; [^4]: [*Statistical Year-Book of Canada 1886*](https://www66.statcan.gc.ca/eng/1886/188603150301_p.%20301.pdf), p. 301.
;; [^5]: Saskatchewan Archives, [Dominion Lands Act](https://www.saskarchives.com/node/82).
;; [^6]: Crown-Indigenous Relations and Northern Affairs Canada, [Treaty texts and summaries](https://www.rcaanc-cirnac.gc.ca/eng/1360948213124/1544620003549). Treaties 3 and 6 provided 640 acres per family of five. Treaty interpretation is contested; the comparison is illustrative.
;; [^7]: G. Tulchinsky, ["Redpath, John"](https://www.biographi.ca/en/bio/redpath_john_9E.html), *Dictionary of Canadian Biography*, vol. 9.
;; [^8]: Thomson Reuters [Form 40-F, 2024](https://www.sec.gov/Archives/edgar/data/1075124/000119312525048443/d880733dex991.htm); George Weston Limited [news release, May 2025](https://www.newswire.ca/news-releases/george-weston-limited-announces-normal-course-issuer-bid-843077628.html) and [company history](https://www.weston.ca/company/our-history); Power Corporation [2024 Management Proxy Circular](https://www.powercorporation.com/media/uploads/reports/proxy/bpcc-notice-proxy-circular_2024-en.pdf); Rogers Communications [Form 6-K, November 15, 2021](https://www.sec.gov/Archives/edgar/data/733099/000119312521330438/d251225dex991.htm); [Bombardier share structure](https://bombardier.com/en/node/2010476) (equity share calculated from the class percentages); Empire voting shares as reported by [Canadian Grocer](https://canadiangrocer.com/sobey-families-consolidate-empire-voting-shares-single-holding-company); company histories of [J.D. Irving](https://www.jdirving.com/en/about-us/), [Irving Oil](https://www.irvingoil.com/en-CA/discover-irving/history), [McCain](https://www.mccain.com/about-us/our-history/) and [Richardson](https://www.jrsl.ca/our-story/).
;; [^9]: Rogers Communications, [Form 6-K](https://www.sec.gov/Archives/edgar/data/733099/000119312521330438/d251225dex991.htm), November 15, 2021.
;; [^10]: Yvan Allaire, Institute for Governance of Private and Public Organizations, ["Dual-class shares are hot in the U.S. again"](https://igopp.org/en/?p=10939), 2019. IGOPP supports dual-class shares with safeguards.
;; [^11]: Shopify, [governance structure announcement](https://news.shopify.com/shopify-announces-receipt-of-final-order-and-implementation-of-updated-governance-structure), 2022.
;; [^12]: [Forbes World's Billionaires List 2026](https://www.forbes.com/billionaires/), Canadian citizens, using Forbes's self-made flag. The Weston, Irving, Rogers, Desmarais, Richardson, McCain, Sobey and Molson families are not on the list, so this understates inherited wealth.
;; [^13]: Randall Morck, David Stangeland and Bernard Yeung, ["Inherited Wealth, Corporate Control, and Economic Growth: The Canadian Disease?"](https://www.nber.org/system/files/chapters/c9014/c9014.pdf), in R. Morck (ed.), *Concentrated Corporate Ownership*, University of Chicago Press/NBER, 2000, ch. 11, Table 11.1.
;; [^14]: Bennedsen et al., ["Inside the Family Firm"](https://www.nber.org/papers/w12356), *QJE* 122(2), 2007; Pérez-González, "Inherited Control and Firm Performance," *AER* 96(5), 2006; Villalonga and Amit, "How do family ownership, control and management affect firm value?" *Journal of Financial Economics* 80(2), 2006; Smith and Amoako-Adu, *Journal of Corporate Finance* 5(4), 1999, as summarized in Morck, Wolfenzon and Yeung, ["Corporate Governance, Economic Entrenchment, and Growth"](https://www.nber.org/papers/w10692), *Journal of Economic Literature* 43(3), 2005; Anderson and Reeb, "Founding-Family Ownership and Firm Performance," *Journal of Finance* 58(3), 2003; Jog, Zhu and Dutta, *Corporate Governance: An International Review* 18(5), 2010.
;; [^15]: Arash Nekoei and David Seim, "How Do Inheritances Shape Wealth Inequality? Theory and Evidence from Sweden," *Review of Economic Studies* 90(1), 2023.
;; [^16]: OECD, [*Inheritance Taxation in OECD Countries*](https://doi.org/10.1787/e2879a7d-en), 2021.
;; [^17]: Statistics Canada, ["Familial support in entering the Canadian housing market"](https://www150.statcan.gc.ca/n1/pub/36-28-0001/2025003/article/00001-eng.htm), *Economic and Social Reports*, March 2025.
;; [^18]: House of Commons [heritage collection](https://www.ourcommons.ca/heritage/en/collection/25708) (Paul Martin); Assemblée nationale du Québec, [list of premiers](https://www.assnat.qc.ca/fr/patrimoine/pm.html); Legislative Library of British Columbia, [premiers of BC](https://www.llbc.leg.bc.ca/public/reference/premiers.pdf); Legislative Assembly of PEI, [MLA biography](https://www.assembly.pe.ca/sites/www.assembly.pe.ca/files/Historical%20MLA%20Bios/G-2.pdf); Government of Alberta and Legislative Assembly of Ontario member records.
;; [^19]: Jacques P. Gagnon, ["Elementary Kinship Structures in Parliament since 1867"](https://www.revparlcan.ca/?p=3907), *Canadian Parliamentary Review* 34(3), 2011, from Library of Parliament PARLINFO data. Counts of parliamentarians beginning their careers in each era; no denominators are given.
;; [^20]: Daniel M. Smith, *Dynasties and Democracy: The Inherited Incumbency Advantage in Japan*, Stanford University Press, 2018.
;; [^21]: Ernesto Dal Bó, Pedro Dal Bó and Jason Snyder, ["Political Dynasties"](https://www.nber.org/papers/w13122), *Review of Economic Studies* 76(1), 2009.
;; [^22]: Morck, Wolfenzon and Yeung (2005), above.
;; [^23]: *R. v. K.C. Irving Ltd.*, [1978] 1 S.C.R. 408; see J. P. Cairns, [*Alberta Law Review*](https://albertalawreview.com/index.php/ALR/article/download/1854/1843/1966). Sale to Postmedia as reported by CTV News, 2022.
