^:kindly/hide-code
(ns economics.wealth-distribution.canada.c06-productivity
  {:kindly/options {:static true
                    :kinds-that-hide-code #{:kind/var :kind/hiccup2 :kind/hiccup :kind/fn}}
   :clay {:title  "Unbounded North: Stable and Stuck"
          :quarto {:sidebar     "unbounded-north"
                   :author      "Kira Howe"
                   :description "Canada's central economic problem is productivity. Its stability is real, but it has come to protect incumbents rather than enable newcomers."
                   :abstract    "Canada produces about 30% less per hour than the United States, and the gap is mostly about efficiency, not equipment. The same firms sit at the top year after year, capital flows into housing, and too few new firms start or grow."
                   :date        "2026-10-16"
                   :type        :post
                   :category    :economics
                   :tags        [:tax :economics :growth :canada :productivity]}}}
  (:require [clojure.string :as str]
            [scicloj.plotje.api :as pj]
            [tablecloth.api :as tc]
            [economics.wealth-distribution.canada.common :as c]))

;; ## Canada's Achilles Heel

;; > "Our Achilles heel is productivity. We have been very good at growing our economy by adding workers.
;; > We have been much less successful at increasing output per worker." -- Tiff Macklem, Governor of the Bank of Canada, 2024[^1]

;; If the American story is about wealth concentrating at the top of a dynamic economy,
;; the Canadian story is about an economy that has stopped getting more productive.
;; The two are connected, and this part is about how.

(def gdp-gap
  (c/data "prod_G1_gdp_per_capita_gap_canada_us.csv"))

^{:kindly/options {:id :fig-gdp-gap
                   :caption "Canada's GDP per person as a share of the US level, current purchasing power parities (OECD Productivity Database).[^2]"}}
(-> gdp-gap
    (pj/lay-line :year :can_pct_of_usa {:color c/red :size 2})
    (pj/scale :y {:domain [60 100]})
    (pj/options {:title "Canadian Living Standards, % of US"
                 :x-label "Year" :y-label "GDP per person, Canada as % of US"}))

;; In 2025, Canada's GDP per person was 74% of America's: about US\$23,000 (roughly C\$27,000) less per person.[^2]
;; Canadians don't work less: they actually work slightly more hours per person than Americans.
;; The entire gap is output per hour.
;; Statistics Canada puts it plainly: Canada's productivity relative to the US "has fallen 26% since the late 1990s,"
;; and "the relative decline in productivity growth is the principal reason why Canada is falling behind,
;; regardless of how economic performance is measured."[^3]

;; The Bank of Canada estimates that if Canadian productivity had grown like the rest of the G7's since 2000,
;; GDP would be "about 9% higher, which translates to almost \$7,000 per person."[^4]
;; That is more than any plausible tax change could redistribute.
;; Productivity is how an economy gets richer; distribution is who gets the gains.
;; Canada has a problem with both.

;; ## Not a Shortage of Machines

;; The obvious explanation is that Canadian firms don't invest enough in equipment.
;; That is part of it, especially recently (see [The Investment Question](c09_investment_question.html)).
;; But Statistics Canada's own comparison with the US finds something more striking.

(def gu-willox
  (-> (c/data "prod_A5_gu_willox_2023_table1_canada_us_decomposition.csv" {:key-fn identity})
      (tc/rename-columns {"country" :country "component" :component "2001_2019_pp" :pp})))

^{:kindly/options {:id :fig-gu-willox
                   :caption "How much faster labour productivity grew per year in the US private business sector than in the Canadian business sector, 2001–2019, and how much of that gap each source accounts for. The three sources add up to the total gap, allowing for rounding. Multifactor productivity is output growth not explained by more capital or more skilled labour (Gu and Willox, International Productivity Monitor, 2023, Table 1).[^5]"}}
(let [gap (-> gu-willox
                (tc/select-rows #(and (= "Canada minus United States" (:country %))
                                      (#{"Labour productivity" "Capital intensity" "Labour composition" "Total factor productivity"} (:component %))))
                (tc/map-columns :us-ahead [:pp] #(- %))
                (tc/map-columns :label [:component] {"Labour productivity" "Total gap"
                                                     "Capital intensity" "More capital per worker"
                                                     "Labour composition" "More skilled workforce"
                                                     "Total factor productivity" "Efficiency (MFP)"})
                (tc/map-columns :c [:component] #(case % "Labour productivity" c/wd "Total factor productivity" c/red c/neutral))
                (tc/map-columns :t [:us-ahead] #(format "%.2f" (abs (double %))))
                (tc/order-by [:us-ahead]))]
  (-> gap
      (pj/lay-bar :us-ahead :label {:color {:column :c :scale false}})
      (pj/lay-text {:text :t :align-x :left :offset-x 6})
      (pj/scale :x {:domain [0 1.05]})
      (pj/options {:title "Why US Productivity Grew Faster, 2001–2019"
                   :x-label "US growth minus Canadian growth, percentage points per year" :y-label "" :height 260})))

;; Between 2001 and 2019, Canada's labour productivity grew 0.92 points a year slower than America's.
;; **0.87 points of that gap was multifactor productivity**: how efficiently firms combine people, equipment and ideas.
;; Capital per worker was a small part of the story.[^5]
;; The authors trace much of the gap to particular industries, above all information and cultural services,
;; and point to "limits on foreign investment, most notably but not exclusively related to telecommunications."
;; Their markup estimates suggest "limited competition has significantly reduced the productivity performance
;; of that industry as well as the performances of others that are dependent on its services."[^5]

;; In other words: the problem isn't mainly that Canadian firms lack machines.
;; It's that the economy isn't pushing them to use what they have better.
;; That is what competition does.

;; ## Who's on Top, Year After Year

;; In a dynamic economy, today's giants are regularly overtaken by firms that didn't exist a generation ago.
;; Below are the ten largest public companies in each country by market value, every year from 2011 to 2026,
;; from the Forbes Global 2000.[^6]
;; Each bar is coloured by when the company (or the oldest company it grew out of) was founded.

(def forbes-g2000
  (let [ds    (-> (c/data "forbes_g2000_ca_us_2011_2026.csv")
                  (tc/map-columns :uri [:uri] #(if (= % "google") "alphabet" %)))
        rows  (tc/rows ds :as-maps)
        ;; Forbes mislabels a few US companies as Canadian in some years (e.g. 2022):
        ;; assign each company the country it is listed under most often.
        modal (->> rows
                   (group-by :uri)
                   (map (fn [[u rs]] [u (key (apply max-key val (frequencies (map :country rs))))]))
                   (into {}))
        names (->> rows (sort-by :year) (map (juxt :uri :company)) (into {}))]
    (-> ds
        (tc/map-columns :home [:uri] modal)
        (tc/select-rows #(= (:home %) (:country %)))
        (tc/map-columns :name [:uri] names))))

(def founding
  (-> (c/data "top_company_founding_years.csv")
      (tc/select-columns [:uri :founded])))

(defn era [y]
  (cond (nil? y) "Unknown"
        (< y 1900) "Before 1900"
        (< y 1975) "1900–1974"
        :else "1975 or later"))

(def era-colors {"Before 1900" "#6b4f2a" "1900–1974" "#c9a46a" "1975 or later" c/wd "Unknown" c/neutral})

(def top10
  (-> forbes-g2000
      (tc/group-by [:year :country])
      (tc/order-by [:market_value_usd_m] :desc)
      (tc/head 10)
      (tc/ungroup)
      (tc/left-join founding :uri)
      (tc/map-columns :era [:founded] era)
      (tc/map-columns :c [:era] era-colors)
      (tc/map-columns :value-b [:market_value_usd_m] #(/ % 1000.0))))

(defn frame-chart [year country]
  (let [ds   (-> top10
                 (tc/select-rows #(and (= year (:year %)) (= country (:country %))))
                 (tc/order-by [:value-b]))
        xmax (->> (tc/rows (tc/select-rows top10 #(= country (:country %))) :as-maps)
                  (map :value-b) (apply max) (* 1.05))]
    (-> ds
        (pj/lay-bar :value-b :company {:color {:column :c :scale false}})
        (pj/scale :x {:domain [0 xmax]})
        (pj/options {:title (str (if (= country "Canada") "Canada" "United States") ", " year)
                     :x-label "Market value (US$ billions)" :y-label ""
                     :width 460 :height 340 :legend-position :none})
        (pj/plot))))

(def years (range 2011 2027))

^:kind/hiccup
[:div {:class "churn-anim"}
 [:div {:style "display:flex;gap:12px;align-items:center;flex-wrap:wrap;margin-bottom:8px"}
  [:button {:class "play btn btn-sm btn-outline-secondary"} "▶ Play"]
  [:input {:type "range" :min 0 :max (dec (count years)) :value 0 :class "slider" :style "flex:1;min-width:160px"}]
  [:strong {:class "year-label"} (first years)]]
 (into [:div {:class "frames"}]
       (for [[i y] (map-indexed vector years)]
         [:div {:class "frame" :data-year y :style (str "display:" (if (zero? i) "flex" "none") ";flex-wrap:wrap;gap:8px")}
          (frame-chart y "United States")
          (frame-chart y "Canada")]))
 (into [:div {:style "display:flex;gap:16px;flex-wrap:wrap;font-size:0.85em;margin-top:6px"}]
       (for [[label col] (dissoc era-colors "Unknown")]
         [:span [:span {:style (str "display:inline-block;width:12px;height:12px;margin-right:4px;background:" col)}] (str "Founded " (str/lower-case label))]))
 [:script "document.querySelectorAll('.churn-anim').forEach(function(root){
  var fr=root.querySelectorAll('.frame'), s=root.querySelector('.slider'), lab=root.querySelector('.year-label'), btn=root.querySelector('.play'), t=null;
  function show(i){fr.forEach(function(f,j){f.style.display=(j==i)?'flex':'none';}); s.value=i; lab.textContent=fr[i].dataset.year;}
  s.addEventListener('input',function(){show(+s.value);});
  btn.addEventListener('click',function(){ if(t){clearInterval(t);t=null;btn.textContent='▶ Play';return;}
    btn.textContent='❚❚ Pause'; var i=+s.value; if(i==fr.length-1){i=-1;}
    t=setInterval(function(){i++; show(i); if(i>=fr.length-1){clearInterval(t);t=null;btn.textContent='▶ Play';}},900);});
});"]]

;; Press play.
;; America's top ten turns over from oil, industrial and old-line firms (Exxon, GE, Chevron, IBM) to companies
;; founded in the last fifty years: Nvidia, Alphabet, Amazon, Meta, Tesla, Broadcom.
;; Canada's top ten is mostly the same banks, pipelines and resource companies throughout, with
;; Shopify the one young company to break in.

(def founded-share
  (-> top10
      (tc/group-by [:year :country])
      (tc/aggregate {:young-share #(* 10.0 (count (filter (fn [y] (and y (>= y 1975))) (:founded %))))
                     :median-founded #(let [ys (sort (remove nil? (:founded %)))]
                                        (when (seq ys) (nth ys (quot (count ys) 2))))})))

^{:kindly/options {:id :fig-young-share
                   :caption "Share of each country's ten largest public companies by market value that were founded in 1975 or later (Forbes Global 2000; founding years of the original company).[^6]"}}
(-> founded-share
    (pj/lay-line :year :young-share {:color :country :size 2})
    (pj/lay-point {:color :country})
    (pj/scale :color {:values [c/red c/wd]})
    (pj/scale :y {:domain [0 100]})
    (pj/options {:title "How Many Top-10 Companies Are Young?"
                 :x-label "Year" :y-label "% of top 10 founded 1975 or later"
                 :color-label "" :legend-position :bottom}))

;; Simple turnover doesn't capture the difference.
;; Canada's top ten actually swaps members fairly often, because gold, potash and oil companies rise and fall
;; with commodity prices.
;; What it lacks is *new* companies: firms founded in living memory that grew to dominate their industries.
;; The IMF's 2025 review of Canada found the same thing in the data on all firms: "Limited turnover at the top suggests entrenched incumbents."[^7]

;; ::: {.callout-note}
;; ## A Closer Look: The Long View

;; Is Canada's top tier *always* this static? Not entirely.
;; A 1978 Royal Commission on Corporate Concentration tracked Canada's 100 largest non-financial companies and found that
;; 21 of them dropped out between 1967 and 1971, and 22 between 1971 and 1975: turnover similar to the US Fortune 500 at the time.[^19]
;; The biggest Canadian non-financial firms in 1975 were George Weston, Ford Canada, GM Canada, Imperial Oil and Canadian Pacific,
;; several of them subsidiaries of American companies.

;; Where Canada really is different is finance.
;; The five largest financial institutions in 1975 were RBC, CIBC, BMO, Scotiabank and TD.
;; Half a century later, they are still the five largest banks, and still among Canada's ten most valuable companies.
;; Among the 25 largest financial institutions, only two newcomers broke in during each four-year period the Commission studied.[^19]
;; Canada's persistence is concentrated in banking and in family-controlled groups (see [Old Money](c07_old_money.html)),
;; and in what has *not* happened: very few new Canadian firms have grown to join them.
;; :::

;; The same pattern shows up in personal fortunes.
;; Among billionaires who live in each country, the share Forbes counts as "self-made" is about the same,
;; roughly three in four.
;; What differs is *where* the money comes from.

(def billionaire-industries
  (c/data "forbes_2026_ca_us_selfmade_industry.csv"))

^{:kindly/options {:id :fig-billionaire-industry
                   :caption "Share of billionaire wealth by industry, billionaires who are citizens of each country, Forbes 2026. Canadian finance includes cryptocurrency fortunes held by Canadians abroad.[^8]"}}
(c/dumbbell
 (->> (c/pair-rows billionaire-industries :industry :country :share_pct "United States" "Canada")
      (filter #(#{"Technology" "Finance & Investments" "Media & Entertainment" "Fashion & Retail"
                  "Real Estate" "Food & Beverage" "Energy" "Diversified"} (:label %)))
      (sort-by #(- (:from %))))
 {:from-name "United States" :to-name "Canada" :from-color c/wd :to-color c/red :include-zero true
  :title "Where Billionaire Wealth Comes From, 2026"
  :x-label "% of the country's billionaire wealth" :fmt #(format "%.0f%%" (double %))})

;; Technology accounts for 41% of American billionaire wealth and 17% of Canadian-citizen billionaire wealth;
;; among billionaires who actually *live* in Canada, it is just 8%.[^8]
;; Several of the Canadian citizens who made technology fortunes (in search, e-commerce and AI) built them
;; in the United States or Asia.
;; Canada produces founders; it is less good at growing their companies at home.

;; ## Fewer New Firms, and They Stay Small

;; New firms are how new ideas enter an economy.
;; The IMF, using Statistics Canada's firm-level data, finds that firm entry fell from 17% to 8.5% between
;; 2001 and 2023, and young firms' share of output fell from about 30% to under 20%.[^7]
;; Statistics Canada's public business dynamics table, which counts firms differently, shows a milder decline.

(def dynamics
  (-> (c/data "prod_B1_business_dynamics_entry_exit_33100164.csv")
      (tc/select-rows #(and (= "Private sector" (:industry %)) (= "Private sector" (:firm_size %))
                            (#{"Entry rate" "Exit rate"} (:measure %))))))

^{:kindly/options {:id :fig-entry-exit
                   :caption "Entry and exit rates of employer businesses, private sector (Statistics Canada Table 33-10-0164-01). The IMF's firm-level measure shows a steeper decline in entry.[^9]"}}
(-> dynamics
    (pj/lay-line :year :value {:color :measure :size 2})
    (pj/scale :color {:values [c/green c/red]})
    (pj/scale :y {:include 0})
    (pj/options {:title "Business Entry and Exit in Canada"
                 :x-label "Year" :y-label "% of active firms"
                 :color-label "" :legend-position :bottom}))

;; Either way, the bigger problem is that Canadian firms don't *grow*.
;; Statistics Canada finds that in 2019, small firms (under 500 employees) produced 56% of Canada's business output,
;; against 45% in the United States, and Canadian small firms were only 70% as productive as their American counterparts.
;; Too many small firms that stay small explains about 60% of the productivity gap with the US.[^10]
;; A healthy economy needs both new firms and a path for the best ones to become big.
;; Canada's path is crowded by incumbents.

;; ## Comfortable Oligopolies

;; Many of the industries Canadians deal with every day are dominated by a few firms:

;; | Industry | Concentration | Source |
;; | :--- | :--- | :--- |
;; | Banking | Six largest banks hold about 93–94% of banking assets | IMF (2025); Bank of Canada (2025) |
;; | Wireless | Bell, Rogers and TELUS earn 89% of retail mobile revenue; wireless EBITDA margin 48% | CRTC (2024 data) |
;; | Groceries | Five largest grocers hold about 80% of sales | Agriculture and Agri-Food Canada (2020 data) |
;; | Airlines | Air Canada and WestJet carry half to three-quarters of domestic passengers at major airports | Competition Bureau (2025) |
;;
;; : Concentration in key Canadian industries {#tbl-concentration}[^11]

;; The Bank of Canada's Senior Deputy Governor said in 2025:
;; "It would also be hard to argue, on any objective measure, that Canada's banking system is anything other than an oligopoly."
;; She also noted that the big banks are "consistently more profitable than their peers in most advanced economies."[^12]
;; The IMF finds that "a 1 percent increase in markups is linked to about a 0.4 percent decline" in subsequent productivity growth.[^7]

;; ## Capital Goes to Housing

;; Where does Canadian capital go instead?
;; Increasingly, into homes.

(def res-vs-bus
  (-> (c/data "prod_D4_residential_vs_business_investment_canada_vs_us.csv")
      (tc/select-rows #(>= (:year %) 1990))
      (tc/select-columns [:year :country :residential_pct_gdp :business_nonres_pct_gdp])))

^{:kindly/options {:id :fig-residential-vs-business
                   :caption "Residential investment and business non-residential investment as a share of GDP, Canada (Statistics Canada) and the United States (US Bureau of Economic Analysis).[^13]"}}
(-> res-vs-bus
    (pj/lay-line :year :business_nonres_pct_gdp {:color :country :size 2})
    (pj/lay-line :year :residential_pct_gdp {:color :country :size 2 :stroke-dash :dashed :overlay true})
    (pj/scale :color {:values [c/red c/wd]})
    (pj/scale :y {:include 0})
    (pj/options {:title "Building Businesses (solid) vs Building Homes (dashed)"
                 :x-label "Year" :y-label "% of GDP"
                 :color-label "" :legend-position :bottom}))

;; In 2025, Canada spent 7.4% of GDP on residential investment and 11.2% on business investment.
;; The United States spent 3.8% on housing and 14.2% on business.[^13]
;; Canadian lending tilts the same way: credit secured on household real estate has grown from about half of
;; household-mortgage-plus-business lending in 1990 to about two-thirds today.[^14]
;; Land alone is now 36.5% of the value of Canada's non-financial assets, up from 25.5% in 2000
;; (see [Land](c10_land.html)).

;; This is not proof that housing "crowds out" business investment; the Bank of Canada hasn't said that,
;; and the two can rise together.
;; But it shows where Canadian savings and credit have gone: into bidding up the price of existing land,
;; which earns a return without making the economy any more productive.

;; ## Ideas: Not Enough R&D

(def rd
  (-> (c/data "prod_E1_rd_intensity_oecd_msti.csv")
      (tc/select-rows #(and (= "BERD_pct_GDP" (:measure %))
                            (#{"Canada" "United States" "OECD"} (:area %))
                            (>= (:year %) 1990)))))

^{:kindly/options {:id :fig-rd
                   :caption "Business expenditure on research and development as a share of GDP (OECD Main Science and Technology Indicators).[^15]"}}
(-> rd
    (pj/lay-line :year :value {:color :area :size 2})
    (pj/scale :color {:values [c/red c/neutral c/wd]})
    (pj/scale :y {:include 0})
    (pj/options {:title "Business R&D Spending"
                 :x-label "Year" :y-label "% of GDP"
                 :color-label "" :legend-position :bottom}))

;; Canadian business R&D peaked at 1.25% of GDP in 2001 and was about 1.06% in 2024,
;; against 2.67% in the US and about 2% across the OECD.[^15]
;; Firms that face little competition have less reason to invent.

;; ## Stability, the Good Kind and the Other Kind

;; None of this is an argument against stability.
;; Tim Pratley's original series makes the case that stable institutions are what let ordinary people take risks:
;; a predictable legal system, a safety net, a sound financial system.
;; Canada has those, and they are real achievements.
;; How well did they serve Canada in the last two crises?
;; Compared with the other G7 countries:[^20]

;; | | Where Canada did well | Where it didn't |
;; | :--- | :--- | :--- |
;; | **2008–09 financial crisis** | Smallest two-year output loss in the G7 (−1.9%, 2007–2009); back to its pre-crisis peak fastest (9 quarters); no systemic banking crisis and no bank bailouts; banks rated soundest in the world by the World Economic Forum's executive survey, 2008–2014 | Its single-year 2009 contraction (−2.9%) was slightly deeper than America's (−2.6%); unemployment rose more than in Germany, which kept workers on through job-sharing |
;; | **COVID-19** | Second-lowest excess deaths in the G7 after Japan (about 70 per 100,000 in 2020–21, against about 290 in the US); highest first-dose vaccination rate in the G7 by the end of 2021 | GDP fell 5.0% in 2020 against 2.1% in the US, and recovered more slowly; unemployment peaked at 14.2%, among the sharpest rises in the G7 |
;;
;; : Canada's record in two crises, relative to the G7 {#tbl-crises}

;; So Canada's stability is genuine where it matters most for ordinary people: its banks didn't fail, and fewer Canadians died.
;; But stability is not dynamism.
;; The same economy that rides out crises well grows slowly between them.
;; Government support in 2008–09 came as liquidity, not bailouts: CMHC bought \$69B of insured mortgages from the banks,
;; and the Bank of Canada lent more than \$40B at the peak.[^20]

;; But there are two kinds of stability.
;; **System stability** means the rules are predictable and failure isn't catastrophic, so newcomers can try things.
;; **Incumbent stability** means the same firms and families keep their positions whether or not they improve.
;; The first encourages entrepreneurship.
;; The second smothers it.
;; Canada has a great deal of both, and it has been slow to tell them apart.

;; ::: {.callout-note}
;; ## A Closer Look: What Productivity Isn't

;; Two cautions, so the argument isn't overstated.

;; * **Population growth.** Part of Canada's recent productivity slump is arithmetic: the population grew very fast in
;;   2022–2024, mostly through temporary residents, and capital didn't keep up.
;;   The Bank of Canada estimates this explains about 70% of the post-pandemic shortfall from trend.[^16]
;;   The long-run gap with the US predates it by two decades.
;; * **Interprovincial trade.** Removing internal trade barriers could raise GDP by up to 7% over time, according to the IMF.[^17]
;;   But in Statistics Canada's 2026 business survey, 78% of firms that trade across provinces reported no obstacle.[^18]
;;   Internal trade matters; it isn't the whole story.
;; :::

;; ## Conclusion

;; Canada's living standards have fallen from about 85% of America's to about 74% in a generation,
;; and almost all of the gap is productivity.
;; The gap is mostly about efficiency, not equipment, and the evidence points to weak competition, protected incumbents,
;; firms that don't scale, too little R&D, and capital that flows into land.
;; Those are the same features that concentrate Canadian wealth in old, stable fortunes.
;; Fixing distribution and fixing productivity are, in Canada, largely the same project.

;; In the next part, [Old Money](c07_old_money.html), we look at where Canada's oldest fortunes came from, and what the evidence says about inherited control.

;; [^1]: Tiff Macklem, ["Workers, jobs, growth and inflation—Today and tomorrow"](https://www.bankofcanada.ca/2024/06/workers-jobs-growth-and-inflation-today-and-tomorrow/), Bank of Canada, Winnipeg, June 24, 2024.
;; [^2]: OECD [Productivity Database](https://data-explorer.oecd.org/s/4rg), GDP per capita and per hour worked, current PPPs, total economy.
;; [^3]: Statistics Canada, McCormack and Macdonald, ["Measuring Canada's economic performance relative to the United States"](https://www150.statcan.gc.ca/n1/pub/36-28-0001/2026003/article/00004-eng.htm), *Economic and Social Reports*, March 2026.
;; [^4]: Nicolas Vincent, ["Toward a virtuous circle for productivity"](https://www.bankofcanada.ca/2025/11/toward-a-virtuous-circle-for-productivity/), Bank of Canada, November 19, 2025.
;; [^5]: Wulong Gu and Michael Willox, ["The Post-2001 Productivity Growth Divergence between Canada and the United States"](https://csls.ca/ipm-archive/ipm-issue-45/the-post-2001-productivity-growth-divergence-between-canada-and-the-united-states/), *International Productivity Monitor* 45, 2023, Table 1 and abstract.
;; [^6]: Forbes Global 2000, 2011–2026 editions (market value in US dollars). Company identities matched across years and renamings (e.g. Google/Alphabet); where Forbes listed a company under a different country in a single year, it is assigned the country it is listed under most often. Founding years (of the company or the predecessor it names as its origin) were compiled by hand; where ambiguous, the later date is used for Canadian firms and the earlier for US firms, biasing the comparison against this essay's argument (`data/top_company_founding_years.csv`).
;; [^7]: IMF, [Canada: 2025 Article IV Consultation—Staff Report](https://www.imf.org/en/publications/cr/issues/2026/01/21/canada-2025-article-iv-consultation-press-release-and-staff-report-573340), January 2026, paragraphs 32–33 and Box 4.
;; [^8]: [Forbes World's Billionaires List 2026](https://www.forbes.com/billionaires/), industry and self-made flags as assigned by Forbes; computed on this page.
;; [^9]: Statistics Canada, [Table 33-10-0164-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3310016401), business dynamics measures.
;; [^10]: Statistics Canada, Gu and Lesica, "The role of firm size in the Canada–U.S. labour productivity gap since 2000," *Economic and Social Reports*, December 2025; [The Daily](https://www150.statcan.gc.ca/n1/daily-quotidien/251222/dq251222c-eng.htm).
;; [^11]: Banking: IMF, *Canada: Financial System Stability Assessment*, Country Report 25/220, August 2025, and the Bank of Canada speech below. Wireless: CRTC, Canadian Telecommunications Market Report 2025, [open data](https://open.canada.ca/data/dataset/bd41f23a-6dbc), Figure 14. Groceries: Agriculture and Agri-Food Canada, as cited by the Competition Bureau. Airlines: Competition Bureau Canada, market study, June 2025.
;; [^12]: Carolyn Rogers, ["Productivity's competitive edge"](https://www.bankofcanada.ca/2025/10/productivitys-competitive-edge/), Bank of Canada, Toronto, October 9, 2025. She also notes that bank concentration supports financial stability.
;; [^13]: Statistics Canada, [Table 36-10-0108-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610010801) and [36-10-0104-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610010401); US Bureau of Economic Analysis National Income and Product Accounts (via FRED: PRFI, PNFI, GDP).
;; [^14]: Statistics Canada, [Tables 36-10-0639-01 and 36-10-0640-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610063901), credit measures.
;; [^15]: OECD, Main Science and Technology Indicators, business enterprise expenditure on R&D (BERD) as % of GDP; 2024 values provisional.
;; [^16]: Bank of Canada, [Staff Analytical Paper 2026-19](https://www.bankofcanada.ca/2026/05/staff-analytical-paper-2026-19/), "Potential output in Canada: 2026 assessment," Box 3.
;; [^17]: IMF, [Canada: 2025 Article IV Staff Report](https://www.imf.org/en/publications/cr/issues/2026/01/21/canada-2025-article-iv-consultation-press-release-and-staff-report-573340); see also Alvarez, Krznar and Tombe, ["Internal Trade in Canada: Case for Liberalization"](https://www.imf.org/en/Publications/WP/Issues/2019/07/22/Internal-Trade-in-Canada-Case-for-Liberalization-47100), IMF Working Paper 19/158.
;; [^18]: Statistics Canada, [Table 33-10-1118-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3310111801), Canadian Survey on Business Conditions, first quarter 2026.
;; [^19]: Royal Commission on Corporate Concentration, [*Report*](https://publications.gc.ca/collections/collection_2014/bcp-pco/Z1-1975-1-1-eng.pdf), 1978, Tables 2.3–2.4 and the lists of the largest non-financial and financial corporations, 1975.
;; [^20]: GDP: IMF World Economic Outlook and OECD quarterly national accounts; banking crises: Laeven and Valencia, IMF systemic banking crises database; excess deaths: WHO estimates for 2020–21; vaccination: Our World in Data; unemployment: OECD; CMHC Insured Mortgage Purchase Program: Office of the Parliamentary Budget Officer (2020); Bank of Canada term liquidity: *Bank of Canada Review*, 2009; World Economic Forum, *Global Competitiveness Report* (executive opinion survey), 2008–2014. Full sources in `data-prep/topco_longrun/NOTES.md`.
