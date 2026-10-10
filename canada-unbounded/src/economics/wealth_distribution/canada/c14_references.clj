^:kindly/hide-code
(ns economics.wealth-distribution.canada.c14-references
  {:kindly/options {:static true
                    :kinds-that-hide-code #{:kind/var :kind/hiccup2 :kind/fn}}
   :clay {:title  "Unbounded North: References"
          :quarto {:sidebar     "unbounded-north"
                   :author      "Kira Howe"
                   :description "The data, research and institutions behind the Unbounded North series."
                   :abstract    "Every chart in this series is built from public data published by Canadian and international institutions. Here is where to find it."
                   :date        "2026-10-24"
                   :type        :post
                   :category    :economics
                   :tags        [:tax :economics :growth :canada]}}})

;; ## Why This Chapter Exists

;; This series adapts Timothy Pratley's [*Unbounded*](https://clojurecivitas.org/economics/wealth_distribution/unbounded/u0_introduction.html)
;; to Canada.
;; Like the original, every chart is computed in a Clojure notebook from data files you can inspect, using
;; [Clay](https://scicloj.github.io/clay/), [Plotje](https://github.com/scicloj/plotje) and
;; [Tablecloth](https://scicloj.github.io/tablecloth/).
;; The data files are in the `data/` folder next to the notebooks, and `data/SOURCES.md` records where each came from.

;; I have tried to use only institutional sources: Statistics Canada, the Parliamentary Budget Officer,
;; Finance Canada, the Canada Revenue Agency, the Bank of Canada, the OECD and the IMF, plus peer-reviewed research.
;; Where a number comes from a news report of an official release, a charity, or an advocacy group, the footnote says so.

;; > "Some people use statistics as a drunken man uses a lamppost — for support rather than illumination." -- Andrew Lang

;; ## Canadian Data

;; **Statistics Canada**
;;
;; * Distributions of Household Economic Accounts: [36-10-0660-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610066001) (wealth by quintile, quarterly), [36-10-0587-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610058701) (income, consumption and saving).
;; * Survey of Financial Security 2023: [11-10-0075-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1110007501) (net worth deciles), [11-10-0016-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1110001601) (by province).
;; * High income tax filers: [11-10-0055-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1110005501).
;; * National Balance Sheet Accounts: [36-10-0580-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610058001).
;; * GDP by expenditure and income: [36-10-0104-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610010401), [36-10-0103-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610010301).
;; * Productivity and labour compensation: [36-10-0208-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610020801), [36-10-0206-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610020601), archived 36-10-0305-01.
;; * Income, poverty and inequality: [11-10-0239-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1110023901), [11-10-0134-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1110013401), [11-10-0135-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1110013501).
;; * Housing: Census homeownership rates, the Canadian Housing Survey ([46-10-0083-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=4610008301), [46-10-0085-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=4610008501)), and the Canadian Housing Statistics Program ([46-10-0099-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=4610009901)).
;; * Prices: [18-10-0005-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1810000501). Migration: [17-10-0040-01](https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1710004001).
;;
;; Full tables can be downloaded as CSV from `https://www150.statcan.gc.ca/n1/tbl/csv/<table-id>-eng.zip`.

;; **Office of the Parliamentary Budget Officer**
;;
;; * [Estimating the Top Tail of the Family Wealth Distribution in Canada: 2025 Update](https://www.pbo-dpb.ca/en/publications/RP-2526-009-S--estimating-top-tail-family-wealth-distribution-in-canada-2025-update--estimation-extremite-superieure-distribution-patrimoine-familial-canada-mises-jour-2025), and its public High-net-worth Family Database file. This is the single most useful source on the top of Canada's wealth distribution.
;; * Wealth tax costings: [2020](https://www.pbo-dpb.ca/en/publications/RP-2021-017-M--net-wealth-tax-canadian-resident-economic-families--impot-patrimoine-net-familles-economiques-residant-au-canada), [2021 (M-68)](https://www.pbo-dpb.ca/en/publications/RP-2122-012-M--revenue-estimates-m-68-one-time-tax-extreme-wealth--estimation-recettes-mesures-prevues-dans-motion-m-68-impot-unique-richesse-extreme), [2025](https://distribution-a617274656661637473.pbo-dpb.ca/cc1b029d8d36c1340f715cb7666bfd2132ef6683608cecae42a5e71b82bb6ad6).
;; * [Increasing the Capital Gains Inclusion Rate](https://www.pbo-dpb.ca/en/publications/LEG-2425-010-S--increasing-capital-gains-inclusion-rate--augmentation-taux-inclusion-gains-capital), 2024.

;; **Department of Finance Canada and the Canada Revenue Agency**
;;
;; * [Annual Financial Report 2024–25](https://www.canada.ca/en/department-finance/services/publications/annual-financial-report/2025.html) and [Fiscal Reference Tables 2025](https://www.canada.ca/en/department-finance/services/publications/fiscal-reference-tables/2025/part-1.html).
;; * [Report on Federal Tax Expenditures 2026](https://www.canada.ca/content/dam/fin/publications/taxexp-depfisc/2026/taxexp-depfisc-26-eng.pdf).
;; * [Spring Economic Update 2026](https://budget.canada.ca/update-miseajour/2026/report-rapport/anx1-en.html) and [Budget 2024, Chapter 8](https://budget.canada.ca/2024/report-rapport/chap8-en.html).
;; * CRA [Individual Income Tax Return Statistics, 2023 tax year](https://www.canada.ca/en/revenue-agency/programs/about-canada-revenue-agency-cra/income-statistics-gst-hst-statistics/t1-final-statistics/2023-tax-year.html).

;; **Other Canadian institutions**
;;
;; * Bank of Canada: Carolyn Rogers, ["Time to break the glass: Fixing Canada's productivity problem"](https://www.bankofcanada.ca/2024/03/time-to-break-the-glass-fixing-canadas-productivity-problem/), 2024.
;; * Competition Bureau Canada: ["Competition in Canada from 2000 to 2020: An Economy at a Crossroads"](https://ised-isde.canada.ca/site/competition-bureau-canada/en/how-we-foster-competition/education-and-outreach/competition-canada-2000-2020-economy-crossroads), 2023.
;; * Indigenous Services Canada: [socio-economic gaps, 2021 Census](https://www.sac-isc.gc.ca/eng/1690909773300/1690909797208).
;; * Elections Canada and the House of Commons (election results, votes, procedure).

;; ## International Data

;; * OECD: [Analytical House Price Database](https://data-explorer.oecd.org/s/eh), [Corporate Tax Statistics](https://www.oecd.org/en/data/datasets/corporate-income-tax-rates-database.html), Productivity Database, Economic Outlook, Income Distribution Database, System of Health Accounts, [Revenue Statistics 2025](https://www.oecd.org/content/dam/oecd/en/publications/reports/2025/12/revenue-statistics-2025-country-notes_3708be73/canada_3aa043fb/795aaca9-en.pdf), [Economic Surveys: Canada 2025](https://www.oecd.org/en/publications/2025/05/oecd-economic-surveys-canada-2025_ee18a269.html), and [The Role and Design of Net Wealth Taxes in the OECD](https://doi.org/10.1787/9789264290303-en) (2018).
;; * IMF: [Fiscal Monitor, April 2026](https://www.imf.org/-/media/files/publications/fiscal-monitor/2026/april/english/msa.pdf); [2025 Article IV concluding statement for Canada](https://www.imf.org/en/news/articles/2025/12/05/cs-canada-staff-concluding-statement-of-the-2025-article-iv-mission).
;; * [Maddison Project Database 2023](https://www.rug.nl/ggdc/historicaldevelopment/maddison/releases/maddison-project-database-2023) and the [World Inequality Database](https://wid.world/country/canada/).
;; * [Forbes World's Billionaires List 2026](https://www.forbes.com/billionaires/).

;; ## Research

;; **Canadian wealth, income and taxation**
;;
;; * Davies and Di Matteo, "Long Run Canadian Wealth Inequality in International Context," *Review of Income and Wealth* 67(1), 2021.
;; * Saez and Veall, ["The Evolution of High Incomes in Northern America: Lessons from Canadian Evidence"](https://doi.org/10.1257/0002828054201404), *American Economic Review* 95(3), 2005.
;; * Wolfson, Veall, Brooks and Murphy, ["Piercing the Veil: Private Corporations and the Income of the Affluent"](https://ideas.repec.org/a/ctf/journl/v64y2016i1p1-30.html), *Canadian Tax Journal* 64(1), 2016.
;; * Vaillancourt and Kerkhoff, ["Capital gains taxation in Canada, 1972–2017"](https://www.unsw.edu.au/content/dam/pdfs/business/acct-audit-tax/research-reports/past-issues/bus-2019/2019-volume-16-number-2/BUS-2019-V16-paper1-v16-n2.pdf), *eJournal of Tax Research* 16(2), 2019.
;; * Connolly, Corak and Haeck, ["Intergenerational Mobility between and within Canada and the United States"](https://doi.org/10.1086/703465), *Journal of Labor Economics* 37(S2), 2019.
;; * Morck, Stangeland and Yeung, ["Inherited Wealth, Corporate Control and Economic Growth: The Canadian Disease"](https://www.nber.org/papers/w6814), 1998.
;; * Gu, ["Investment Slowdown in Canada After the Mid-2000s"](https://www150.statcan.gc.ca/n1/pub/11f0019m/11f0019m2024001-eng.htm), Statistics Canada, 2024.
;; * Uguccioni, ["Explaining the Gap between Productivity and Median Wage Growth in Canada, 1976–2014"](https://csls.ca/ipm-archive/ipm-issue-31/explaining-the-gap-between-productivity-and-median-wage-growth-in-canada-1976-2014.md), *International Productivity Monitor* 31, 2016.
;; * Baker, Messacar and Stabile, ["Effects of Child Tax Benefits on Poverty and Labor Supply"](https://doi.org/10.1086/721379), *Journal of Labor Economics* 41(4), 2023.

;; **Wealth taxes, capital taxes and investment**
;;
;; * Jakobsen, Jakobsen, Kleven and Zucman, ["Wealth Taxation and Wealth Accumulation: Theory and Evidence from Denmark"](https://doi.org/10.1093/qje/qjz032), *QJE*, 2020.
;; * Brülhart, Gruber, Krapf and Schmidheiny, ["Behavioral Responses to Wealth Taxes: Evidence from Switzerland"](https://doi.org/10.1257/pol.20200258), *AEJ: Economic Policy*, 2022.
;; * Ring, ["Wealth Taxation and Household Saving"](https://doi.org/10.1093/restud/rdae100), *Review of Economic Studies*, 2025, and ["Taxing Entrepreneurial Wealth: Evidence from Norway, 2021–2025"](https://www.nber.org/papers/w35854), NBER, 2026.
;; * Bjørneby, Markussen and Røed, ["An imperfect wealth tax and employment in closely held firms"](https://doi.org/10.1111/ecca.12456), *Economica*, 2023.
;; * Agrawal, Foremny and Martínez-Toledano, ["Wealth Tax Mobility and Tax Coordination"](https://doi.org/10.1257/app.20220615), *AEJ: Applied*, 2025.
;; * Londoño-Vélez and Ávila-Mahecha, ["Enforcing Wealth Taxes in the Developing World"](https://doi.org/10.1257/aeri.20200319), *AER: Insights*, 2021.
;; * Advani and Tarrant, ["Behavioural responses to a wealth tax"](https://doi.org/10.1111/1475-5890.12283), *Fiscal Studies*, 2021.
;; * Yagan, ["Capital Tax Reform and the Real Economy"](https://doi.org/10.1257/aer.20130098), *AER*, 2015; Zwick and Mahon, ["Tax Policy and Heterogeneous Investment Behavior"](https://doi.org/10.1257/aer.20140855), *AER*, 2017.
;; * Hope and Limberg, ["The economic consequences of major tax cuts for the rich"](https://doi.org/10.1093/ser/mwab061), *Socio-Economic Review*, 2022.
;; * France Stratégie, [evaluation of the 2018 capital tax reforms](https://www.strategie-plan.gouv.fr/files/files/Publications/Rapport/fs-2023-rapport-isf-quatrieme_rapport_complet_17octobre_avis_0.pdf), 2023.

;; **Productivity, competition and corporate churn**
;;
;; * Gu and Willox, ["The Post-2001 Productivity Growth Divergence between Canada and the United States"](https://csls.ca/ipm-archive/ipm-issue-45/the-post-2001-productivity-growth-divergence-between-canada-and-the-united-states/), *International Productivity Monitor* 45, 2023.
;; * McCormack and Macdonald, ["Measuring Canada's economic performance relative to the United States"](https://www150.statcan.gc.ca/n1/pub/36-28-0001/2026003/article/00004-eng.htm), Statistics Canada, 2026.
;; * IMF, [Canada: 2025 Article IV Staff Report](https://www.imf.org/en/publications/cr/issues/2026/01/21/canada-2025-article-iv-consultation-press-release-and-staff-report-573340), 2026.
;; * Bank of Canada speeches: Macklem (June 2024), [Rogers (October 2025)](https://www.bankofcanada.ca/2025/10/productivitys-competitive-edge/), [Vincent (November 2025)](https://www.bankofcanada.ca/2025/11/toward-a-virtuous-circle-for-productivity/).
;; * Royal Commission on Corporate Concentration, [*Report*](https://publications.gc.ca/collections/collection_2014/bcp-pco/Z1-1975-1-1-eng.pdf), 1978.
;; * Forbes Global 2000, 2011–2026.

;; **Dynastic wealth**
;;
;; * Lord Durham, *Report on the Affairs of British North America*, 1839 ([full text](https://www.fadedpage.com/showbook.php?pid=20210933)).
;; * Morck, Stangeland and Yeung, ["Inherited Wealth, Corporate Control, and Economic Growth: The Canadian Disease?"](https://www.nber.org/system/files/chapters/c9014/c9014.pdf), 2000; Morck, Wolfenzon and Yeung, ["Corporate Governance, Economic Entrenchment, and Growth"](https://www.nber.org/papers/w10692), *JEL*, 2005.
;; * Bennedsen, Nielsen, Pérez-González and Wolfenzon, ["Inside the Family Firm"](https://www.nber.org/papers/w12356), *QJE*, 2007.
;; * Gagnon, ["Elementary Kinship Structures in Parliament since 1867"](https://www.revparlcan.ca/?p=3907), *Canadian Parliamentary Review*, 2011; Dal Bó, Dal Bó and Snyder, ["Political Dynasties"](https://www.nber.org/papers/w13122), *REStud*, 2009.

;; **Land**
;;
;; * Haig, [*The Exemption of Improvements from Taxation in Canada and the United States*](https://archive.org/details/exemptionofimpro00haiguoft), 1915.
;; * Dye and England, [*Assessing the Theory and Practice of Land Value Taxation*](https://www.lincolninst.edu/app/uploads/legacy-files/pubfiles/assessing-theory-practice-land-value-taxation-full_0.pdf), Lincoln Institute, 2010.
;; * OECD, [*Housing Taxation in OECD Countries*](https://www.oecd.org/tax/tax-policy/brochure-housing-taxation-in-oecd-countries.pdf), 2022; Mirrlees et al., *Tax by Design*, 2011.

;; **Scorecard**
;;
;; * LEGISinfo bill records; Finance Canada budgets and economic updates; Office of the Parliamentary Budget Officer reports (2025–2026); NATO defence expenditure reports.

;; ## Books

;; * **The Beginning of Infinity**, *David Deutsch*: progress as error correction.
;; * **Capital in the Twenty-First Century**, *Thomas Piketty*: the long-run dynamics of wealth concentration.
;; * **The Trading Game**, *Gary Stevenson*: a trader's view of how markets diverge from household outcomes.
;; * The original [*Unbounded*](https://clojurecivitas.org/economics/wealth_distribution/unbounded/u8_references.html) reference list, which this series builds on.

;; ## Data Caveats

;; * **Units differ.** The Survey of Financial Security and the PBO count *families*; the Distributions of Household Economic Accounts count *households*; the World Inequality Database counts *adults*. The stitched distribution in [Wealth Concentration](c05_wealth_concentration.html) joins these, so read it as a picture of shape, not precise levels.
;; * **Surveys miss the top.** Statistics Canada says its survey understates the top 1%'s share. The PBO's database corrects for this using rich lists; its estimates are the best published, but they are modelled.
;; * **No Canadian tax-by-wealth study exists.** Unlike the American IRS study used in the original series, there is no public estimate of taxes paid by wealth group in Canada.
;; * **Some figures are from secondary reports.** Living wages for Toronto, Vancouver and Calgary, and the Competition Bureau's grocery share, come from news reports of official releases; the primary sites could not be fetched automatically.
;; * **Long-run wealth shares.** The Davies–Di Matteo figures are from their 2018 working paper; the 2021 journal version revised some estimates.
;; * **Top companies.** The Forbes Global 2000 API is complete only from 2011, so the animated comparison covers 2011–2026. Forbes ranks by market value, Fortune by revenue, and the 1978 Royal Commission by sales; long-run comparisons mix measures.
;; * **The scorecard is early.** Most measures became law in 2026, US tariffs and an oil shock dominate the data, and all recent figures will be revised.
;; * **Land values are modelled.** Statistics Canada splits residential property into land and structures using a land-to-structure ratio it describes as dated.
