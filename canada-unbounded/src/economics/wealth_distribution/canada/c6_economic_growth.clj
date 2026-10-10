^:kindly/hide-code
(ns economics.wealth-distribution.canada.c6-economic-growth
  {:kindly/options {:static true
                    :kinds-that-hide-code #{:kind/var :kind/hiccup2 :kind/fn}}
   :clay {:title  "Unbounded North: Economic Growth"
          :quarto {:sidebar     "unbounded-north"
                   :author      "Kira Howe"
                   :description "How concentrated, inherited wealth and protected incumbents slow Canadian growth."
                   :abstract    "Canada's growth problem is a participation problem. Economists have a name for one version of it: the Canadian disease."
                   :date        "2026-10-16"
                   :type        :post
                   :category    :economics
                   :tags        [:tax :economics :growth :canada]}}}
  (:require [scicloj.plotje.api :as pj]
            [tablecloth.api :as tc]
            [economics.wealth-distribution.canada.common :as c]))

;; ## Growth

;; Economic growth is the accumulation of useful knowledge:
;; better ways of turning time and resources into things people value.
;; Tim Pratley's original series argued that growth depends on *circulation* (money moving to where it is useful),
;; *participation* (many people able to try new ideas), and *error correction*
;; (mechanisms that stop harmful feedback loops from running away).[^1]
;; Canada is a useful test of that argument, because it has a long-running growth problem.

(def maddison
  (-> (c/data "real_gdp_per_capita_canada_long_run.csv")
      (tc/drop-missing [:maddison_gdppc_CAN_2011ppp :maddison_gdppc_USA_2011ppp])
      (tc/select-rows #(>= (:year %) 1920))
      (tc/add-column :ratio #(map (fn [a b] (* 100.0 (/ a b)))
                                  (:maddison_gdppc_CAN_2011ppp %) (:maddison_gdppc_USA_2011ppp %)))))

^{:kindly/options {:id :fig-gdp-per-capita-vs-us
                   :caption "Canada's real GDP per person as a share of the US level (Maddison Project Database 2023, 2011 PPP dollars).[^2]"}}
(-> maddison
    (pj/lay-line :year :ratio {:color c/red :size 2})
    (pj/scale :y {:domain [50 100]})
    (pj/options {:title "Canadian Living Standards, % of US"
                 :x-label "Year" :y-label "Canada's GDP per person, % of US"}))

(defn cagr [ds k from to]
  (let [v (fn [y] (first (k (tc/select-rows ds #(= y (:year %))))))]
    (* 100.0 (- (Math/pow (/ (v to) (v from)) (/ 1.0 (- to from))) 1.0))))

^:kind/table
(tc/dataset
 (for [[from to] [[1946 1975] [1980 2022] [2000 2022]]]
   {"Period"        (str from "–" to)
    "Canada, % a year" (format "%.2f" (cagr maddison :maddison_gdppc_CAN_2011ppp from to))
    "US, % a year"     (format "%.2f" (cagr maddison :maddison_gdppc_USA_2011ppp from to))}))

;; Real GDP per person grew about 2.5% a year in Canada from 1946 to 1975, faster than in the United States.
;; Since 1980 it has grown about 1.4% a year, and since 2000 under 1%.
;; Canada closed much of the gap with America in the postwar decades, from about 70% of the US level in 1945 to 88% in the mid-1970s.
;; Apart from a recovery during the 2000s commodity boom, it has been losing ground since, and is now below 80%.[^2]
;; Lower taxes on capital did not reverse that, as [The Investment Question](c7_investment_question.html) shows.
;; So what else could be going on?

;; ## Circulation

;; Money coordinates the economy.
;; Each exchange connects people, skills and capital with something that needs doing.
;; When wealth piles up faster than it circulates, the economy slows, because spending is someone else's income.

;; In Canada, three things slow circulation:

;; * **Corporate cash.** Non-financial private corporations held almost \$1 trillion in cash and deposits at the end of 2025,
;;   29% of GDP, up from 11% in 2000 (see [The Investment Question](c7_investment_question.html)).
;; * **Household debt.** Canadian households owe about 176% of their disposable income, mostly mortgages.
;;   Every dollar of interest paid to a lender is a dollar not spent at a local business (see [Labour Hours](c3_labour_hours.html)).
;; * **Housing as a store of value.** When the safest way to build wealth is to buy land and wait,
;;   capital flows into bidding up existing homes rather than building new businesses.
;;   Housing is the main asset of most Canadian families, and the most lightly taxed.

;; `wages → spending → sales → hiring → wages` is the loop that grows an economy from the bottom up.
;; Rising prices for land and existing homes divert income from that loop to people who already own.

;; ## Participation

;; Growth depends on how many people get to try things.
;; Every firm that enters a market tests a new idea, and incumbents have to improve or lose customers.
;; In Canada, that process has been slowing.

;; The Competition Bureau found that firm entry rates fell by 24% between 2001 and 2020,
;; the number of highly concentrated industries rose from 19 to 27 between 2005 and 2018,
;; and the same firms increasingly hold the top positions in their industries year after year.[^3]
;; Statistics Canada estimates that declining entry and competition explain about 30% of the fall in
;; business investment per worker from 2006 to 2019.[^4]

;; Some of Canada's most familiar industries are run by a handful of firms:
;; the Big Six banks, three national wireless carriers, and five grocery chains that the Competition Bureau's
;; 2023 grocery study found together control around 80% of the market.[^5]
;; A protected incumbent has less reason to invest, and a potential challenger has less chance to try.

;; ## The Canadian Disease

;; There is a striking Canadian strand of research on exactly this.
;; In the late 1990s, economists Randall Morck, David Stangeland and Bernard Yeung studied what happens when
;; large Canadian firms are controlled by heirs rather than founders or professional managers.
;; They found that "heir-controlled Canadian firms show low industry-adjusted financial performance,"
;; and that across countries, those where inherited billionaire wealth is large relative to GDP grow more slowly.[^6]
;; They called it *the Canadian disease*.

;; The mechanism is the one Tim describes.
;; Old, concentrated wealth protects what it already owns.
;; It lobbies for barriers to entry, favours stability over experimentation, and keeps control
;; through dual-class shares, family trusts and holding companies.
;; Morck and his co-authors found that heir control declined after the Canada–US Free Trade Agreement
;; exposed protected firms to competition.[^6]

;; Canada has no inheritance tax, so nothing in the tax system slows the transfer of control from one generation to the next.
;; A wave of inheritances is under way: CPA Canada estimated in 2023 that about \$1 trillion would pass
;; from baby boomers to their children between 2023 and 2026.[^7]
;; Studies of Canadian survey data find that inheritances are larger and more likely for families who are already well off.[^8]

;; ## Error Correction

;; Progress depends on finding and fixing mistakes.
;; A feedback loop in which wealth produces more wealth, and wealth produces political influence that protects wealth,
;; is a mistake that does not fix itself.
;; Competition policy is one correction: it limits how much a protected incumbent can earn without improving.
;; A wealth tax is another: it limits how fast the largest fortunes can compound,
;; and how far they can pull away from everyone else.

;; > "The best way to predict the future is to invent it." -- Alan Kay

;; ::: {.callout-note}
;; ## A Closer Look: Two Kinds of Canadian Fortune

;; The 2026 Forbes list of Canadian billionaires shows both of Canada's economies.[^9]
;; One is new: fortunes built from Shopify, Lululemon, Couche-Tard, homebuilding and software in the last few decades.
;; The other is old: the Thomson family alone has six members on the list, worth a combined US\$43.5B,
;; descended from a media business founded in the 1930s.

;; A wealth tax treats both the same.
;; But they relate to growth differently.
;; New fortunes are usually the result of participation: someone tried something and it worked.
;; Old fortunes are more often the reward for having owned something a long time ago.
;; An economy that wants more of the first should be cautious about letting the second compound untaxed forever.
;; :::

;; ## Conclusion

;; Canada's growth has slowed for decades, and deep cuts to capital taxes did not fix it.
;; The evidence points instead to weak competition, protected incumbents, and wealth that sits rather than circulates.
;; Canadian research even has a name for the role of inherited control: the Canadian disease.
;; Growth needs many participants, not just more capital in the same hands.
;; Competition policy and taxation of very large fortunes are complementary corrections.

;; In the next part, [The Investment Question](c7_investment_question.html), we test the claim that a wealth tax would kill investment.

;; [^1]: Timothy Pratley, [Unbounded: Economic Growth](https://clojurecivitas.org/economics/wealth_distribution/unbounded/u6_economic_growth.html), ClojureCivitas, 2026.
;; [^2]: [Maddison Project Database 2023](https://www.rug.nl/ggdc/historicaldevelopment/maddison/releases/maddison-project-database-2023), Jutta Bolt and Jan Luiten van Zanden, GDP per capita in 2011 international dollars. Growth rates computed on this page.
;; [^3]: Competition Bureau Canada, ["Competition in Canada from 2000 to 2020: An Economy at a Crossroads"](https://ised-isde.canada.ca/site/competition-bureau-canada/en/how-we-foster-competition/education-and-outreach/competition-canada-2000-2020-economy-crossroads), 2023.
;; [^4]: Wulong Gu, ["Investment Slowdown in Canada After the Mid-2000s: The Role of Competition and Intangibles"](https://www150.statcan.gc.ca/n1/pub/11f0019m/11f0019m2024001-eng.htm), Statistics Canada, 2024.
;; [^5]: Competition Bureau Canada, *Canada Needs More Grocery Competition*, retail grocery market study, June 2023; the "roughly 80%" figure for the five largest grocers (Loblaw, Sobeys/Empire, Metro, Costco and Walmart) is as summarized by [Global News](https://globalnews.ca/news/9796699/competition-bureau-canada-grocery-study-takeaways/).
;; [^6]: Randall Morck, David Stangeland and Bernard Yeung, ["Inherited Wealth, Corporate Control and Economic Growth: The Canadian Disease"](https://www.nber.org/papers/w6814), NBER Working Paper 6814, 1998; published in Randall Morck (ed.), *Concentrated Corporate Ownership*, University of Chicago Press, 2000.
;; [^7]: CPA Canada estimate (2023), as reported by [CBC News](https://www.cbc.ca/news/canada/saskatchewan/wealth-transfer-inequality-1trillion-1.7462837). An earlier CIBC estimate (2016) was \$750B over ten years.
;; [^8]: See the 2024 peer-reviewed study using the Survey of Financial Security 2005–2019, [abstract on Érudit](https://releve.erudit.org/fr/revues/efg/2024-n46-efg09848/1116166ar/resume/).
;; [^9]: [Forbes World's Billionaires List 2026](https://www.forbes.com/billionaires/), Canadian citizens.
