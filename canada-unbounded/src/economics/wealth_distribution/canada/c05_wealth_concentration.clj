^:kindly/hide-code
(ns economics.wealth-distribution.canada.c05-wealth-concentration
  {:kindly/options {:static true
                    :kinds-that-hide-code #{:kind/var :kind/hiccup2 :kind/fn}}
   :clay {:title  "Unbounded North: Wealth Concentration"
          :quarto {:sidebar     "unbounded-north"
                   :author      "Kira Howe"
                   :description "What Canada's wealth distribution looks like, and how it is changing."
                   :abstract    "Canada's wealth is less concentrated than America's, but its shape is the same right angle. The broad middle held its ground on housing; the very top pulled away."
                   :date        "2026-10-15"
                   :type        :post
                   :category    :economics
                   :tags        [:tax :economics :growth :canada]}}}
  (:require [clojure.string :as str]
            [scicloj.plotje.api :as pj]
            [tablecloth.api :as tc]
            [economics.wealth-distribution.canada.common :as c]))

;; ## The Current Situation

;; At the end of 2025, Canadian households owned \$18.6 trillion in net worth.[^1]
;; The wealthiest 20% of households held 65.7% of it, an average of \$3.5M each.
;; The bottom 40% held 3.0%, an average of \$81,650.
;; The bottom fifth, on average, owed more than it owned.

;; Those quintile figures come from Statistics Canada's quarterly
;; Distributions of Household Economic Accounts, which tie a household survey to the
;; national balance sheet.
;; They are reliable, but they stop at the top 20%.
;; To see the shape of the distribution, we need to stitch sources together, as the original series did.[^2]

;; ## Every Family in Canada

;; There are about 16.9 million "family units" in Canada: economic families plus people living on their own.
;; The chart below lines them up from poorest to richest.
;; It combines three sources:

;; * For the bottom three-quarters, the median net worth within each decile from Statistics Canada's 2023 Survey of Financial Security.[^3]
;; * For the top quarter, net worth thresholds computed from the Parliamentary Budget Officer's
;;   public High-net-worth Family Database (2023).
;;   The PBO built it by extending the survey with a modelled top tail anchored on rich lists,
;;   scaled to match the national balance sheet.[^4]
;; * At the very tip, individual billionaires living in Canada from the 2026 Forbes list.[^5]

(def total-families
  "PBO's family count for 2023, the denominator for its published shares."
  16852000)

(def hfd
  (-> (c/data "pbo_hfd_2023_public.csv")
      (tc/order-by [:FNETWTH] :desc)))

(defn hfd-threshold
  "Net worth at percentile p, from the PBO public microfile."
  [p]
  (let [target (* total-families (/ (- 100.0 p) 100.0))]
    (->> (map vector (:FNETWTH hfd) (reductions + (:FWEIGHT hfd)))
         (drop-while (fn [[_ cum]] (< cum target)))
         ffirst)))

(defn hfd-share
  "Share of total net worth held above percentile p, using PBO's published total ($16,341B)."
  [p]
  (let [target (* total-families (/ (- 100.0 p) 100.0))]
    (->> (map vector (:FNETWTH hfd) (:FWEIGHT hfd) (reductions + (:FWEIGHT hfd)))
         (take-while (fn [[_ _ cum]] (<= cum target)))
         (map (fn [[nw wt _]] (* nw wt)))
         (reduce +)
         (* (/ 100.0 16341e9)))))

(def sfs-points
  (-> (c/data "sfs_networth_by_decile.csv")
      (tc/select-rows #(and (= 2023 (:sfs_year %)) (= "Canada" (:geo %))
                            (not (str/starts-with? (:net_worth_deciles %) "Total"))))
      (tc/add-column :percentile (fn [_] [5 15 25 35 45 55 65 75 85 95]))
      (tc/rename-columns {:median_net_worth_2023cad :net-worth})
      (tc/select-rows #(<= (:percentile %) 75))
      (tc/add-column :source "Survey of Financial Security")
      (tc/select-columns [:percentile :net-worth :source])))

(def hfd-points
  (let [ps [80 85 90 95 97 98 99 99.5 99.9 99.95 99.99 99.995 99.999]]
    (tc/dataset {:percentile ps
                 :net-worth  (map hfd-threshold ps)
                 :source     "PBO high-net-worth database"})))

(def usd->cad
  "Bank of Canada USD/CAD rate on 2026-03-10, the Forbes list date."
  1.3567)

(def forbes
  (-> (c/data "forbes_2026_canadian_billionaires.csv")
      ;; Living in Canada, or no residence given (several Thomson heirs).
      (tc/select-rows #(contains? #{"Canada" nil ""} (:country_of_residence %)))
      (tc/order-by [:net_worth_usd_billions] :asc)))

(def forbes-points
  (let [n (tc/row-count forbes)]
    (-> forbes
        (tc/add-columns {:net-worth  (fn [ds] (map #(* % 1e9 usd->cad) (:net_worth_usd_billions ds)))
                         :percentile (fn [_] (map #(- 100.0 (/ (* 100.0 (- n %)) total-families)) (range n)))
                         :source     "Forbes billionaires"})
        (tc/select-columns [:percentile :net-worth :source :name]))))

(def curve
  (-> (tc/concat sfs-points hfd-points forbes-points)
      (tc/order-by [:percentile])))

(defn curve-chart
  [ds {:keys [title unit divisor labels log?]}]
  (cond-> (-> ds
              (tc/add-column :y #(map (fn [v] (/ v divisor)) (:net-worth %)))
              (pj/lay-line :percentile :y {:size 0.5 :color "#8b7246"})
              (pj/lay-point {:color :source}))
    log? (pj/scale :y :log)
    labels (pj/lay-text {:data (tc/dataset labels) :x :percentile :y :y :text :text
                         :align-x :right :offset-x -8 :overlay true})
    true (pj/options {:title   title
                      :x-label "Families ordered by net worth (percentile)"
                      :y-label (str "Net worth (C$ " unit ")")
                      :color-label "" :legend-position :bottom})))

^{:kindly/options {:id :fig-ca-wealth-distribution
                   :caption "Every Canadian family unit, lined up by net worth. At this scale everyone below the billionaires is a flat line at zero.[^3][^4][^5]"}}
(let [top (last (tc/rows curve :as-maps))]
  (curve-chart curve {:title   "Canadian Family Wealth Distribution"
                      :unit    "billions"
                      :divisor 1e9
                      :labels  {:percentile [(:percentile top)] :y [(/ (:net-worth top) 1e9)]
                                :text [(str (:name top) " (" (c/money (:net-worth top)) ")")]}}))

;; As in the American series, the "curve" is a right angle.
;; The richest billionaire living in Canada has as much wealth as more than 60,000 median Canadian families combined.
;; Let's zoom in by removing the billionaires.

^{:kindly/options {:id :fig-ca-wealth-excluding-billionaires
                   :caption "Excluding billionaires. The top 0.001% of families (about 170) start at roughly $840M; the top 0.01% start at about $170M."}}
(curve-chart (tc/select-rows curve #(< (:net-worth %) 1e9))
             {:title "Excluding Billionaires" :unit "millions" :divisor 1e6
              :labels {:percentile [99.999 99.99]
                       :y (map #(/ (hfd-threshold %) 1e6) [99.999 99.99])
                       :text (map #(str %1 " starts at " (c/money (hfd-threshold %2)))
                                  ["Top 0.001%" "Top 0.01%"] [99.999 99.99])}})

;; Still a right angle.
;; Remove the top 1%, whose threshold is about \$7.4M.

^{:kindly/options {:id :fig-ca-wealth-excluding-top-1
                   :caption "Excluding the top 1% of families. The top 10% starts at about $2.0M; the median is about $520K."}}
(curve-chart (tc/select-rows curve #(<= (:percentile %) 99))
             {:title "Excluding the Top 1%" :unit "millions" :divisor 1e6
              :labels {:percentile [99 90] :y (map #(/ (hfd-threshold %) 1e6) [99 90])
                       :text (map #(str %1 " starts at " (c/money (hfd-threshold %2)))
                                  ["Top 1%" "Top 10%"] [99 90])}})

;; Now the curve is visible, but it is still steep.
;; It is only once we drop the top 10% that we can see how ordinary Canadians are doing.

^{:kindly/options {:id :fig-ca-wealth-excluding-top-10
                   :caption "Excluding the top 10%. The poorest tenth of families has a median net worth of about $1,000; the 45th–55th percentile is about $420–630K, most of it home equity."}}
(curve-chart (tc/select-rows curve #(<= (:percentile %) 90))
             {:title "Excluding the Top 10%" :unit "thousands" :divisor 1e3})

;; Canada's middle looks very different from America's.
;; The median Canadian family's net worth was \$519,700 in 2023, up 36% from 2019 in real terms.[^3]
;; Most of that is a house.
;; Housing is the main asset of the middle 60% of Canadian families,
;; which is why Canada's middle class looks wealthier on paper than America's,
;; and why that wealth is so tied up in one illiquid, overpriced asset
;; (see [Labour Hours](c03_labour_hours.html)).

;; On a log scale we can see the whole distribution at once.

^{:kindly/options {:id :fig-ca-wealth-log
                   :caption "Log scale. Families with zero or negative net worth (roughly the bottom 5%) cannot be shown."}}
(curve-chart (tc/select-rows curve #(pos? (:net-worth %)))
             {:title "Canadian Family Wealth, Log Scale" :unit "millions" :divisor 1e6 :log? true})

;; ## How Much Does the Top Hold?

(def top-shares
  (tc/dataset
   (for [[p label] [[99.999 "Top 0.001%"] [99.99 "Top 0.01%"] [99.9 "Top 0.1%"]
                    [99 "Top 1%"] [90 "Top 10%"]]]
     {:group label
      :families (Math/round (* total-families (/ (- 100.0 p) 100.0)))
      :share (hfd-share p)
      :threshold (hfd-threshold p)})))

^:kind/table
(-> top-shares
    (tc/add-columns {:families  (fn [ds] (map #(format "%,d" %) (:families ds)))
                     :share     (fn [ds] (map c/pct-text (:share ds)))
                     :threshold (fn [ds] (map c/money (:threshold ds)))})
    (tc/rename-columns {:group "Group" :families "Families" :share "Share of all wealth"
                        :threshold "Starts at"}))

;; The top 1% of families, about 169,000 households with \$7.4M or more, hold about 24% of all wealth.
;; This is the PBO's own published estimate, and our calculation from its public file reproduces it.[^4]
;; The top 0.01%, about 1,700 families, hold over 5%.
;; That is almost two-thirds as much as the entire bottom half of Canadian families combined,
;; whose share is 8.1% in the survey.[^3]

;; Statistics Canada's own survey puts the top 1% at only 11%.
;; The gap is because surveys miss the very rich.
;; Statistics Canada says so plainly:
;; "measures like the share of wealth held by the top 1% will be understated in this data source."[^3]
;; An experimental Statistics Canada estimate, built by capitalizing investment income reported on tax returns,
;; puts the top 1% even higher, at 26.6% in 2019.[^6]

;; ## Is It Getting Worse?

;; Here the Canadian story is genuinely different from the American one, and it is worth being careful.

(def dhea
  (-> (c/data "dhea_networth_by_wealth_quintile_quarterly.csv")
      (tc/select-rows #(and (zero? (:derived %)) (not= "All households" (:wealth_group %))))
      (tc/add-column :date (fn [ds]
                             (map #(let [[y m] (str/split % #"-")]
                                     (+ (parse-long y) (/ (dec (parse-long m)) 12.0)))
                                  (:ref_date ds))))
      (tc/add-column :group (fn [ds]
                              (map {"Q1 (bottom 20%)" "Bottom 20%" "Q2" "Second 20%" "Q3" "Middle 20%"
                                    "Q4" "Fourth 20%" "Q5 (top 20%)" "Top 20%"}
                                   (:wealth_group ds))))))

^{:kindly/options {:id :fig-dhea-shares
                   :caption "Share of household net worth by wealth quintile. Fourth quarter of each year 2010–2019, then quarterly to Q4 2025.[^1]"}}
(-> dhea
    (pj/lay-line :date :share_computed_pct {:color :group})
    (pj/lay-point {:color :group :size 2})
    (pj/options {:title   "Share of Canadian Household Wealth by Quintile"
                 :x-label "Year"
                 :y-label "Share of total net worth (%)"
                 :color-label "" :legend-position :bottom}))

;; By quintile, Canada's wealth distribution has been remarkably stable since 2010.
;; The top 20%'s share actually *fell* slightly, from 68.9% to 65.7%,
;; and the bottom 40%'s share doubled, from 1.5% to 3.0%.[^1]
;; Two forces drove that.
;; House prices rose sharply, and housing is middle-class wealth.
;; Households also saved heavily during the pandemic, helped by emergency benefits.
;; Since 2021, the trend has reversed: the gap between the top 20% and the bottom 40% has widened
;; every year, and in 2025 the top grew its wealth fastest (+6.0%) and the bottom slowest (+2.1%),
;; "mainly from equity markets".[^1]

;; Quintiles can't see the very top, though.
;; The top 20% is 3.4 million households; the people a wealth tax is aimed at are a few thousand.
;; For them, the clearest signal is the billionaire count.

(def billionaire-years
  (-> (c/data "forbes_canadian_billionaires_by_year.csv")
      (tc/select-rows #(>= (:list_year %) 2010))))

^{:kindly/options {:id :fig-billionaire-count
                   :caption "Canadian citizens on the Forbes billionaires list, and how many of them live in Canada. Nominal US dollars.[^5]"}}
(-> billionaire-years
    (tc/pivot->longer [:canadian_citizens_count :canada_residents_count]
                      {:target-columns :measure :value-column-name :count})
    (tc/update-columns {:measure (partial map {:canadian_citizens_count "Canadian citizens"
                                                :canada_residents_count  "Living in Canada"})})
    (pj/lay-line :list_year :count {:color :measure})
    (pj/lay-point {:color :measure})
    (pj/scale :y {:include 0})
    (pj/options {:title   "Canadian Billionaires, 2010–2026"
                 :x-label "Forbes list year"
                 :y-label "Number of billionaires"
                 :color-label "" :legend-position :bottom}))

;; In 2010, Forbes counted 24 Canadian billionaires, worth a combined US\$75B.
;; In 2026 it counted 82, worth US\$457B.[^5]
;; Fifty of them live in Canada, holding US\$202B.
;; Over the same period, total Canadian household net worth grew about 2.6-fold in Canadian dollars,
;; while the count of billionaires more than tripled.

;; The Forbes list also undercounts old Canadian money.
;; Several of the country's best-known dynasties (the Westons, Irvings, Rogers, Desmarais, McCains and Sobeys)
;; do not appear on Forbes's 2026 list, even though Canadian rich lists put several of them among the
;; country's wealthiest families.
;; The PBO supplements Forbes with the Canadian Business/Maclean's rich list for exactly this reason.[^4]

;; ## The Long View

;; Over the long run, Canadian wealth concentration traces a U shape, like most rich countries.
;; Using estate tax records, Davies and Di Matteo estimate the top 1% of Ontario families held about 41% of wealth in 1892.
;; By the late 1960s, the top 1% of Canadian adults held about 20%.
;; By the 1980s, concentration was rising again.[^7]

(def long-run
  (tc/concat
   (-> (c/data "davies_dimatteo_wp2018_wealth_shares.csv")
       (tc/select-rows #(and (= "adjusted" (:adjusted %)) (:top1_pct %)))
       (tc/add-column :year #(map (fn [a b] (/ (+ a b) 2.0)) (:period_start %) (:period_end %)))
       (tc/add-column :source "Davies & Di Matteo")
       (tc/select-columns [:year :top1_pct :source]))
   (tc/dataset {:year     [2016 2019 2023]
                :top1_pct [25.6 23.4 23.8]
                :source   "PBO"})))

^{:kindly/options {:id :fig-long-run-top-1
                   :caption "Top 1% share of net worth. 1892 and 1902 are Ontario families; 1946–1970 are Canadian adults (estate multiplier method); 1970 onward are Canadian families from surveys with a rich-list adjustment. Different sources and units: compare the shape, not individual points.[^7][^4]"}}
(-> long-run
    (pj/lay-line :year :top1_pct {:color :source})
    (pj/lay-point {:color :source})
    (pj/scale :y {:include 0})
    (pj/options {:title   "Top 1% Share of Canadian Wealth"
                 :x-label "Year"
                 :y-label "Share of net worth (%)"
                 :color-label "" :legend-position :bottom}))

;; The mid-century trough coincides with Canada's highest top tax rates and its estate tax.
;; The federal estate tax was abolished in 1972.
;; The top 1% share bottomed out around then and has been in the low-to-mid twenties ever since.

;; ## Compared With the United States

;; By every measure, Canadian wealth is less concentrated than American wealth.
;; The World Inequality Database puts the top 1% share at about 29% in Canada and 35% in the United States.[^8]
;; The PBO's 23.8% for Canada compares with roughly 31% in the U.S. Federal Reserve's Distributional Financial Accounts.[^2]
;; Canada's median family has more net worth relative to the mean, mainly because of homeownership.

;; Public wealth differs even more.
;; The original series noted that the U.S. federal balance sheet is more than \$40T in the red,
;; around \$156,000 per adult.[^2]
;; Canada's federal government also has a negative net worth: \$910B, or about \$26,600 per adult.
;; But Canada's *combined* general government (federal, provincial, local, and the CPP/QPP funds)
;; has a **positive** net worth of \$1.53T, about \$44,800 per adult,
;; because public assets, including over \$1T in public pension funds, exceed public debts.[^9]
;; Canadians collectively still own their public sector.
;; That is a rare position among rich countries, and it is worth protecting.

;; ::: {.callout-note}
;; ## A Closer Look: Who Are Canada's Billionaires?

;; The 2026 Forbes list includes 82 Canadian citizens.[^5]
;; Only 50 live in Canada.
;; The richest, cryptocurrency exchange founder Changpeng Zhao, lives in the United Arab Emirates;
;; his fortune alone is almost a quarter of the citizens' total.
;; Others live in the United States, Hong Kong, the United Kingdom and Switzerland.

;; Among those who do live here, the list mixes new fortunes (Shopify, Lululemon, Couche-Tard, homebuilding,
;; pharmacy) with inherited ones: six members of the Thomson family appear, together worth US\$43.5B.
;; A Canadian wealth tax would apply to residents.
;; Canada already taxes residents who leave through its "departure tax",
;; which treats emigrants as having sold their assets on the way out (see [Wealth Tax](c02_wealth_tax.html)).
;; :::

;; ## Conclusion

;; Canada's wealth distribution has the same basic shape as America's: a right angle.
;; The top 1% of families hold about a quarter of all wealth.
;; The top 0.01% hold about as much as the bottom half.
;; Canada's broad middle has held on, mostly through home equity.
;; But since 2021 the gap has been widening again, and the number of Canadian billionaires
;; has more than tripled since 2010.
;; Canada also starts from a stronger public balance sheet than the United States,
;; which gives it room to choose.

;; In the next part, [Stable and Stuck](c06_productivity.html), we look at Canada's central economic problem: productivity.

;; [^1]: Statistics Canada, [Table 36-10-0660-01, Distributions of household economic accounts, wealth, by characteristic, Canada, quarterly](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610066001), and [The Daily, April 13, 2026](https://www150.statcan.gc.ca/n1/daily-quotidien/260413/dq260413a-eng.htm).
;; [^2]: Timothy Pratley, [Unbounded: Wealth Concentration](https://clojurecivitas.org/economics/wealth_distribution/unbounded/u5_wealth_concentration.html), using the Federal Reserve's [Distributional Financial Accounts](https://www.federalreserve.gov/releases/z1/dataviz/dfa/).
;; [^3]: Statistics Canada, Survey of Financial Security 2023, [Table 11-10-0075-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1110007501) (net worth by decile) and [The Daily, October 29, 2024](https://www150.statcan.gc.ca/n1/daily-quotidien/241029/dq241029a-eng.htm). Points plotted are the median within each decile, placed at the decile midpoint. Values in 2023 dollars.
;; [^4]: Office of the Parliamentary Budget Officer, [Estimating the Top Tail of the Family Wealth Distribution in Canada: 2025 Update](https://www.pbo-dpb.ca/en/publications/RP-2526-009-S--estimating-top-tail-family-wealth-distribution-in-canada-2025-update--estimation-extremite-superieure-distribution-patrimoine-familial-canada-mises-jour-2025) (September 2025), Table 2, and its public High-net-worth Family Database file for 2023. Thresholds and shares above are computed from the public file using the PBO's denominators (16.852 million families, \$16,341B).
;; [^5]: [Forbes World's Billionaires List 2026](https://www.forbes.com/billionaires/) (March 10, 2026), Canadian citizens. Converted to Canadian dollars at the Bank of Canada rate of 1.3567 for March 10, 2026 ([Valet API, FXUSDCAD](https://www.bankofcanada.ca/valet/observations/FXUSDCAD)). The curve includes billionaires living in Canada or with no residence listed.
;; [^6]: Statistics Canada experimental estimates from capitalized investment income, as compared in the PBO's [2025 update](https://www.pbo-dpb.ca/en/publications/RP-2526-009-S--estimating-top-tail-family-wealth-distribution-in-canada-2025-update--estimation-extremite-superieure-distribution-patrimoine-familial-canada-mises-jour-2025), Table 4. Experimental; excludes real estate.
;; [^7]: James B. Davies and Livio Di Matteo, "Long Run Canadian Wealth Inequality in International Context", [Western University EPRI Working Paper 2018-1](https://ir.lib.uwo.ca/epri-workingpapers/), Table 1 (adjusted series); published in revised form in the *Review of Income and Wealth* 67(1), 2021.
;; [^8]: [World Inequality Database](https://wid.world/country/canada/), net personal wealth, top 1% share (2024). WID rates its Canadian wealth series as low quality (mostly imputed).
;; [^9]: Statistics Canada, [Table 36-10-0580-01, National Balance Sheet Accounts](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610058001), second quarter 2026; adults from [Table 17-10-0005-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1710000501).
