# Does taxing capital/wealth "kill productivity and investment"? Evidence notes (Canada)

Compiled 2026-10-10. Data pulled 2026-10-10 from Statistics Canada full-table CSVs, OECD SDMX API, and primary documents. All derived numbers come from the CSVs in this folder, which can be regenerated with `scripts/`. Raw downloads are in `raw/` (untrusted, not edited).

**Labelling conventions.** [PRIMARY] marks official statistics, central bank, government or international-organisation material. [PEER-REVIEWED] marks journal articles. [WP] marks working papers (NBER, IZA, ECGI, StatCan research papers). [SECONDARY / THINK TANK] marks context only. [MEDIA] marks a news report of a quote.

---

## Bottom line (balanced read of the evidence)

1. **The Canadian natural experiment does not support the claim that capital-tax levels are the binding constraint on investment.**
   - Canada's combined statutory corporate income tax (CIT) rate fell from 42.4% (2000) to 26.1% (2012) [OECD]. In 2000 it was about 3 points above the US rate (39.3%). From 2012 to 2017 it was about 13 points below the US. After the 2018 US cut, the two have been roughly equal (Canada 26.0 vs US 25.6 in 2025).
   - Business non-residential investment did not trend up as a share of GDP. It averaged:
     - 11.9% of GDP in 1981–1999
     - 12.0% in 2000–2012
     - 11.6% in 2013–2025
     - 11.2% in 2025
   - The 2006–2014 bump was oil-and-gas structures during the commodity boom.
   - Machinery and equipment (M&E) fell from about 6.3% of GDP (2000) to 3.2% (2025).
   - Corporate gross operating surplus rose from about 20.5% of GDP (1981–99 average) to about 24.2% (2013–25). Investment relative to that surplus fell from 0.58 to 0.48.
   - Non-financial private corporations' currency and deposits rose from 11.0% of GDP (2000) to 19.5% (2012) and 29.4% (2025).

   Caveat: this is a before/after comparison, not a controlled experiment. Investment might have been even weaker without the cuts (see the counter-evidence in D.6). The fair claim is "rate cuts did not deliver the promised investment boom", not "rate cuts had zero effect".

2. **Institutions that study Canada's weak productivity and investment point mainly to non-tax factors.**
   - The Bank of Canada (Rogers, 2024) names competition, policy and regulatory uncertainty, and scale.
   - The OECD Economic Survey 2025 names business dynamism, concentration, interprovincial barriers and R&D.
   - The IMF Article IV (2025) names weak business dynamism and high concentration in key service industries.
   - Statistics Canada (Gu, 2024) finds declining firm entry and competition explain about 30% of the 2006–2019 fall in investment per worker.

   None of these identifies the *level* of capital taxation as the main cause. The IMF and Ottawa (Budget 2025) do favour *targeted* investment incentives such as accelerated expensing. That fits the micro evidence that incentives tied to new investment work better than general cuts to rates on owners' income or wealth.

3. **Direct quasi-experimental evidence on wealth taxes finds small or no real effects on firms and investment, but large reporting, avoidance, evasion and (at the very top) mobility responses.**
   - Norway: Ring (RES 2024) finds the wealth tax *raised* household saving. Bjørneby, Markussen & Røed (Economica 2023) find no job losses, and in fact a positive link with employment in closely held firms. Ring (NBER 2026) finds no fall in firm investment after the 2021–24 tax rises, and at most 100 people emigrated because of them.
   - France: the 2023 France Stratégie evaluation detected no investment effect from replacing the wealth tax (ISF) or from the flat tax on capital income (PFU).
   - The main *counter*-evidence on firms is Berzins, Bøhren & Stacescu (Norway, ECGI working paper). They find owners facing a liquidity squeeze pull dividends out of their firms, with lower investment and growth, concentrated in small and medium firms with less-wealthy owners.
   - The behavioural cost is real but sits mostly on the *tax base*:
     - Switzerland: reported wealth rises at least 43% six years after a 1-point rate cut.
     - Denmark: sizeable long-run elasticities at the top.
     - Colombia and Spain: evasion and offshoring, and mobility to Madrid.
     - Sweden: elasticities of 0.09–0.27, mostly underreporting.
   - These responses affect *revenue* and *design* (third-party reporting, valuation, exit taxes, few exemptions). They say much less about productivity.

4. **Macro and cross-country work on cutting taxes on the rich finds no growth payoff.**
   - Hope & Limberg (2022; 18 OECD countries including Canada): no significant effect on GDP per capita or unemployment, and a top 1% share about 0.8 points higher after 5 years.
   - Piketty, Saez & Stantcheva (2014): top-rate cuts are associated with higher top shares, not faster growth.
   - IMF (Ostry et al., 2014): redistribution is generally benign for growth except in extreme cases.
   - Caveats in the other direction:
     - OECD (Cingano, 2014) finds the growth damage comes from inequality at the *bottom*. It finds no evidence that top earners pulling away hurts growth, so this is not an argument specifically for taxing the top.
     - Some Canadian provincial panel studies (Dahlby & Ferede) find CIT cuts raise growth.
     - US evidence finds corporate-rate and expensing changes *do* move investment (Ohrn 2018; Chodorow-Reich, Zidar & Zwick 2024), though by less than advertised. Dividend-tax and owner-level cuts do not (Yagan 2015).

5. **The honest framing.** The best evidence separates two kinds of tax:
   - **Taxes on the *firm's* marginal investment.** These include the CIT rate, depreciation and expensing. They have measurable but modest effects on investment.
   - **Taxes on the *owner's* income or wealth.** These include dividend tax, capital gains tax and wealth tax. They have small or undetectable effects on real investment and employment, but sizeable effects on reported wealth, realisations and location.

   A wealth tax falls mainly in the second group. The "kills investment" rebuttal mostly borrows evidence from the first group.

---

## A. The Canadian natural experiment: corporate tax cuts, 2000–2012

### A1. Statutory corporate tax rates. File: `A1_corporate_tax_rates_oecd.csv`

**Source.** [PRIMARY] OECD Corporate Tax Statistics, dataflow `OECD.CTP.TPS:DSD_TAX_CIT@DF_CIT(2.0)`, measure `CIT_C` (combined central plus sub-central statutory rate).
- Query: https://sdmx.oecd.org/public/rest/data/OECD.CTP.TPS,DSD_TAX_CIT@DF_CIT,2.0/.A.CIT_C.ST......
- Canada also has the central rate (CIT) and sub-central rate (CIT_SCG).
- The OECD-38 average is an unweighted mean that I computed. 2026 covers only 35 countries, so treat it as provisional.

**Canada:**

| Year | Combined CIT (%) | Federal (%) | Provincial average (%) |
|---|---|---|---|
| 2000 | 42.43 | 29.12 (28% + surtax) | 13.31 |
| 2004 | 34.38 | 22.12 | — |
| 2008 | 31.43 | 19.5 | — |
| 2010 | 29.38 | 18.0 | — |
| 2012 | 26.15 | 15.0 | — |
| 2025 | 26.0 | 15.0 | 11.0 |

**US combined rate:** 39.34 (2000), 38.91 (2017), 25.84 (2018, after the Tax Cuts and Jobs Act), 25.57 (2025).

**OECD-38 unweighted average:** 32.26 (2000), 25.27 (2012), 24.20 (2025).

So Canada went from about 10 points *above* the OECD average to about 2 points above it. From 2000 to 2017 it went from about 3 points above the US to about 12 points below.

**Note.** This OECD series begins in 2000. For 1981–1999, Finance Canada or the older OECD Tax Database tables would be needed (not compiled here). The federal general rate was 28% plus a 4% surtax on federal tax (= 1.12 points) through 2000, which matches the OECD's 29.12.

### A2. Business non-residential investment as % of GDP, 1961–2025. Files: `A2_business_nonres_investment_share_gdp_annual.csv` and `..._quarterly.csv`

**Source.** [PRIMARY] StatCan Table 36-10-0104-01, current prices, seasonally adjusted at annual rates.
- Table: https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610010401
- CSV: https://www150.statcan.gc.ca/n1/tbl/csv/36100104-eng.zip
- Business gross fixed capital formation, non-residential structures: v62305767
- Machinery and equipment: v62305768
- Intellectual property products: v62305769
- GDP: v62305783
- Annual figures are the mean of 4 quarters. The ratio uses nominal values for both numerator and denominator.

**Key figures:**

| Year | Total (% of GDP) | M&E | IPP | Structures | Note |
|---|---|---|---|---|---|
| 1981 | 15.48 | 7.28 | — | — | Peak |
| 1993 | 9.79 | — | — | — | Trough |
| 2000 | 12.26 | 6.28 | 2.04 | 3.94 | |
| 2014 | 13.80 | 3.99 | — | — | Oil-sands boom |
| 2025 | 11.16 | 3.18 | 2.43 | 5.55 | |

**Period averages (% of GDP):**

| Period | Total | M&E | IPP | Structures |
|---|---|---|---|---|
| 1981–99 | 11.88 | 5.79 | 1.35 | 4.74 |
| 2000–12 | 12.02 | 4.98 | 2.15 | 4.89 |
| 2013–25 | 11.60 | 3.53 | 2.11 | 5.96 |

Interpretation: after the CIT cuts, M&E (the asset most tied to productivity) fell steadily as a share of GDP. The temporary rise in the total was structures, driven by oil prices. The latest quarter (2026 Q2) is 11.02%.

Caveat: nominal shares are affected by falling relative prices of equipment. A real per-worker view is in B2.

### A3. Corporate cash ("dead money"). File: `A3_nfc_currency_deposits_pct_gdp.csv`

**Source.** [PRIMARY] StatCan Table 36-10-0580-01 (National balance sheet accounts).
- Sector: Non-financial private corporations (also given: Non-financial corporations).
- Category: Total currency and deposits (asset side), vector v62694354.
- Valuation: Market value. For deposits this is identical to book value.
- Measured as the Q4 stock divided by annual nominal GDP.
- Table: https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610058001
- CSV: https://www150.statcan.gc.ca/n1/tbl/csv/36100580-eng.zip

| Q4 of | Currency and deposits | % of GDP |
|---|---|---|
| 1990 | $44.4bn | 6.4 |
| 2000 | $121.1bn | 10.9 |
| 2008 | $293.9bn | 17.7 |
| 2012 | $356.5bn | 19.5 |
| 2016 | $492.4bn | 24.3 |
| 2020 | $744.2bn | 33.5 |
| 2025 | $954.8bn | 29.4 |

The latest quarter, 2026 Q2, is $968.6bn.

Caveat: this is a *gross* asset. Corporate debt also rose, and cash-holding has risen across advanced economies for precautionary and intangibles-related reasons. It is not purely "idle" money.

**The Carney "dead money" quote** [MEDIA, Canadian Press via CBC, 23 Aug 2012]:
- Source: https://www.cbc.ca/news/business/carney-urges-firms-to-spend-cash-or-return-to-shareholders-1.1130766
- Context: Bank of Canada Governor Mark Carney, responding to a question at the Canadian Auto Workers gathering in Toronto on 22 Aug 2012. This was not in his prepared text.
- The CP report says holding on to cash only amounts to "dead money". That two-word phrase is the part attributed to him in quotation marks.
- Direct quotes: "The level of caution could be viewed as excessive." And, about corporate managers: "Their job is to put money to work and if they can't think of what to do with it, they should give it back to their shareholders."
- The question cited a study suggesting firms held "some $500 billion" in cash. Times Colonist (CP) attributes this to a Canadian Labour Congress study: https://www.timescolonist.com/business/move-your-money-to-help-economy-bank-boss-urges-4565486
- Press accounts (Globe and Mail, as summarised by others) cite $526bn at 2012 Q1.
- The current-vintage StatCan figure for non-financial private corporations' currency and deposits at 2012 Q1 is $334.5bn. The ~$500bn figures used a broader definition or an older vintage, so **do not mix the two**. Use the CSV series for charts and quote the 2012 number only as the figure "Carney was asked about".

### A4. Corporate surplus vs investment. File: `A4_corporate_surplus_vs_business_investment.csv`

**Source.** [PRIMARY] StatCan 36-10-0103-01 (income-based GDP).
- Net operating surplus, corporations: v62295553
- Consumption of fixed capital, corporations: v62295554
- Table: https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610010301

Gross operating surplus of corporations is a national-accounts proxy for pre-tax profits plus depreciation. It covers all corporations, including financial ones.

| Period | Gross operating surplus (% of GDP) | Investment ÷ surplus |
|---|---|---|
| 1981–99 average | 20.5 | 0.58 |
| 2000–12 average | 24.5 | 0.49 |
| 2013–25 average | 24.2 | 0.48 |
| 2022 | 27.3 | 0.40 |

Canadian corporations generate more surplus relative to GDP than before the cuts, but reinvest a smaller share of it.

---

## B. Canada's productivity and investment gap

### B1. Labour productivity, Canada vs US vs OECD. File: `B1_labour_productivity_canada_us_oecd.csv`

**Source.** [PRIMARY] OECD Productivity Database, `OECD.SDD.TPS:DSD_PDB@DF_PDB(2.0)`, total economy.
- GDP per hour worked, constant 2020 PPP (`LR`) and current PPP (`V`).
- GDP per person employed is also included.

**Canada's GDP per hour as % of the US (constant 2020 PPP):**

| Year | % of US |
|---|---|
| 1990 | 87.7 |
| 2000 | 87.1 |
| 2010 | 75.7 |
| 2020 | 78.6 |
| 2025 | 70.1 |

In 2025, Canada was USD 59.98 per hour against 85.60 for the US and 66.27 for the OECD aggregate, all at 2020 PPP. On current PPPs, Canada was 73.3% of the US in 2025.

**Cross-checks:**
- OECD Economic Survey of Canada 2025, §4.1 (PDF p.96): "As of 2023, Canada's workforce generated the equivalent of USD 74.7 in goods and services per hour worked (purchasing power parity corrected), far from the USD 97.0 generated in the United States."
  - PDF: https://www.oecd.org/content/dam/oecd/en/publications/reports/2025/05/oecd-economic-surveys-canada-2025_ee18a269/28f9e02c-en.pdf
- Bank of Canada, Rogers (2024): "Back in 1984, the Canadian economy was producing 88% of the value generated by the US economy per hour… by 2022, Canadian productivity had fallen to just 71% of that of the United States."
- IMF 2025 Article IV staff report (as summarised in search results; not verified verbatim): output per hour about 30% below the US.

**Long-run growth** (OECD Survey §4.1, PDF p.98): from 2000 to 2008, Canadian labour productivity growth averaged 1% a year against 2% in the US. Over 2019–2023 Canada averaged 0.5%.

### B2. Business investment per worker, Canada vs US. File: `B2_business_investment_per_worker_canada_us.csv`

**Source.** [PRIMARY] OECD Economic Outlook 119 database, `OECD.ECO.MAD:DSD_EO@DF_EO(1.5)`.
- IBV = private non-residential gross fixed capital formation, volume (chain-linked 2017 national currency).
- ET = total employment.
- Canada is converted to USD at the 2017 GDP PPP (1.2123 CAD/USD). This is an approximation, because it uses the GDP PPP rather than an investment-goods PPP.
- 2026–27 values are OECD projections and are flagged in the CSV.
- **The 2014=100 index columns are the robust comparison.** Level ratios before about 2000 are sensitive to chain-linking and relative investment-goods prices; do not chart them.

**Results:**
- Index (2014 = 100), Canada vs US: 2019: 82.2 vs 113.3. 2023: 83.6 vs 128.3. 2025: 80.2 vs 135.3.
- Level: Canada's real private non-residential investment per worker was about 80.6% of the US in 2014, 58.5% in 2019 and 47.7% in 2025 (approximate).

**Cross-checks:**
- OECD Economic Survey 2025 §4.1 (PDF p.98): "As of 2023 investment per worker in Canada was only 85% of that in 2014 … By comparison, investment per worker increased by 21% in the US, by 13% in the euro area and by 11% in the OECD over the same period." Its business-investment definition differs slightly from the IBV series used here.
- [SECONDARY / THINK TANK] C.D. Howe Institute, Robson & Bafale, Commentary 699 (4 Dec 2025): Canadian workers get about 55 cents of new capital per dollar received by US workers, and about 70 cents per dollar vs the OECD.
  - https://cdhowe.org/publication/canadas-investment-crisis-shrinking-capital-undermines-competitiveness-and-wages/
  - This is consistent with B2.

### B3. Bank of Canada: Senior Deputy Governor Carolyn Rogers, "Time to break the glass: Fixing Canada's productivity problem"

[PRIMARY] Speech to the Halifax Partnership, Halifax, **26 March 2024**.
- https://www.bankofcanada.ca/2024/03/time-to-break-the-glass-fixing-canadas-productivity-problem/

Key quotes (verbatim):
- "You've seen those signs that say, 'In emergency, break glass.' Well, it's time to break the glass."
- "the level of productivity in Canada's business sector is more or less unchanged from where it was seven years ago."
- "You can go back 50 years and find a persistent gap between the level of capital spending per worker by Canadian firms and the level spent by their US counterparts. However, the situation has become worse over roughly the past decade."
- On causes:
  - "if I had to pick the biggest concern in this area, I'd say it's competition."
  - "If firms have high profits, high margins and limited competition, they may not feel as much pressure to invest."
  - She also cites policy and regulatory uncertainty.
  - "investment levels were also weak in the years before the pandemic, when rates were much lower than today."
- The speech does **not** mention tax levels as a cause. Footnote 2 cites Gu (2024) below.

### B4. Statistics Canada, Gu (2024)

[PRIMARY / WP] W. Gu, "Investment Slowdown in Canada After the Mid-2000s: The Role of Competition and Intangibles", Analytical Studies Branch Research Paper No. 474, 22 Feb 2024.
- Paper: https://www150.statcan.gc.ca/n1/pub/11f0019m/11f0019m2024001-eng.htm
- The Daily: https://www150.statcan.gc.ca/n1/daily-quotidien/240222/dq240222d-eng.htm

Findings (from The Daily):
- Investment per worker "declined by 20% from 2006 to 2021".
- Large and medium firms "accounted for 90% of the overall decline".
- Foreign-controlled firms "contributed 30% of the decline" but made up 20% of investment.
- Intangibles rose "from 8% in 2006 to 17% in 2021" of fixed assets.
- The firm entry rate fell "from 12% in 2006 to 10% in 2019", and to 7% in 2021.
- Declining entry and competitive intensity "accounted for 30% of the decline in investment per worker" over 2006–2019.

### B5. OECD Economic Survey of Canada 2025 (May 2025)

[PRIMARY] Same PDF as above.

**Investment and causes:**
- §4.1 (PDF p.98): the extraction sector drove business investment growth from 2000 to 2014, while the rest of the business sector "flatlined between the early 2000s and 2014". IPP investment intensity is comparatively low.
- §4.2.1: industry composition explains only a minor part of the Canada–US gap.

**§4.2.4 (PDF p.103–104): "Business dynamism has been weakening while market concentration has risen."**
- It cites the Competition Bureau (2023): rising concentration and higher profits and markups among already successful firms.
- It notes that the increase in concentration in Canada has been "milder than in the United States".
- It cites Gu (2024): rising concentration and declining entry "have had a negative effect on firm investment since 2006".

**Tax** (PDF p.47):
- The OECD describes the Budget 2024 capital-gains inclusion increase as enhancing progressivity "slightly", while strengthening "incentives to plan capital gains over time".
- It notes Canada, like most OECD countries, "imposes higher taxes on labour than on capital".
- It does not claim capital taxes are the cause of weak investment.

### B6. IMF 2025 Article IV concluding statement (5 Dec 2025)

[PRIMARY] https://www.imf.org/en/news/articles/2025/12/05/cs-canada-staff-concluding-statement-of-the-2025-article-iv-mission

- It names "weak business dynamism, slow capital deepening, and lagging innovation" as the main long-term constraint, with high concentration in key service industries.
- It recommends strengthening the Competition Bureau's mandate, and removing interprovincial frictions (worth "up to 7 percent" of real GDP over time).
- It notes Budget 2025's Productivity Super-Deduction and Accelerated Investment Incentive "will cut the marginal effective tax rate on new capital by over two percentage points". This is a targeted investment incentive and an important nuance: the IMF is not indifferent to tax design.
- No wealth-tax or capital-tax-level recommendation was found in the summary.
- Board press release (21 Jan 2026): https://www.imf.org/en/news/articles/2026/01/21/pr-26012-canada-imf-executive-board-concludes-2025-article-iv-consultation

### B7. Capital gains inclusion rate, 2000 and 2024

**2000 cuts.** In 2000 the inclusion rate fell from 75% to 66⅔% (Feb 2000 budget), then to 50% (Oct 2000 Economic Statement).
- I found **no peer-reviewed or official study** estimating an investment or productivity response to the 2000 cut.
- The A2 data show business non-residential investment at 12.26% of GDP in 2000 and 11.98% in 2005, with M&E falling from 6.28% to 5.25%. This is descriptive only.
- Evidence on realisations exists: timing responses around the 1990 increase to 75% (Vaillancourt, Kerkhoff & Godbout 2018, cited in secondary sources; not verified here).

**2024 proposal.** Budget 2024 proposed raising inclusion to two-thirds for corporations and trusts, and for individuals' gains above $250k, effective 25 June 2024.
- PBO, LEG-2425-010-S (1 Aug 2024), estimated +$17.4bn over 2024-25 to 2028-29.
  - https://www.pbo-dpb.ca/en/publications/LEG-2425-010-S--increasing-capital-gains-inclusion-rate--augmentation-taux-inclusion-gains-capital
- Its assumptions: realisations brought forward by +15% for corporations and +10% for individuals in 2024. Net-of-tax elasticities of 0.85 (corporations) and 1.48 (individuals) thereafter, covering lock-in, asset reallocation and "taxpayers relocating to other jurisdictions".
- The PBO did not model investment or productivity effects; it is a revenue costing.
- Implementation was deferred to 1 Jan 2026, then **cancelled on 21 March 2025** by PM Carney (PMO release: https://www.pm.gc.ca/en/news/news-releases/2025/03/21/prime-minister-mark-carney-cancels-proposed-capital-gains-tax-increase).
- So no ex-post evidence on investment exists. Claims on either side about investment effects were predictions, not evidence.

---

## C. Market concentration, markups and investment

### Competition Bureau Canada, "Competition in Canada from 2000 to 2020: An Economy at a Crossroads" (19 Oct 2023)

[PRIMARY] https://ised-isde.canada.ca/site/competition-bureau-canada/en/how-we-foster-competition/education-and-outreach/competition-canada-2000-2020-economy-crossroads

Key figures, verbatim from the report:
- HHI (a concentration index) in the top 10% most concentrated industries "rose by 8.6%: It went from 2,818 in 2005 to 3,060 in 2018". In the top 25% it "rose by 10.7%" (1,942 to 2,149).
- Industries with HHI > 2,500 ("highly concentrated") rose "from 19 in 2005 to 27 in 2018".
- Industries where the top 10 firms take more than 80% of revenue (CR10) rose from 47 to 50.
- Average concentration fell in medium- and low-concentration industries.
- Rank stability among the top 10 firms rose 2003–2020. "Less than 1% of industries saw their rank stability fall."
- "Entry rates have gone down by 24% (from 11.8% to 9.0%); exit rates, by 8% (from 13.4% to 12.3%)", 2001–2020.
- "a small overall increase of 6.7% in the average markup across all industries between 2002 and 2018", and 12.5% in the highest-markup industries.
- Return on sales rose for most industries, faster in the highest-profit ones.
- The Bureau itself cautions that higher markups can reflect fixed costs such as R&D.

### Statistics Canada markups work

[WP, PRIMARY] "Estimating Markups Using Firm-Level Data: A Comparative Analysis", 11F0019M2025001 (21 Jan 2025).
- https://www150.statcan.gc.ca/n1/daily-quotidien/250121/dq250121d-eng.htm
- Markups rose 5.3% from 2001 to 2019 using the wage bill as variable cost, and 13.3% using cost of goods sold.
- Average profitability rose and entry declined, interpreted as declining competitive intensity.

[WP, PRIMARY] Faryaar, Rosell & Stegnjaic, "The Impact of Competition Intensity on Labour Productivity Growth in Canada", ASB Research Paper No. 490 (30 Mar 2026).
- https://doi.org/10.25318/11f0019m2026002-eng
- "stronger competition is generally associated with faster productivity growth across most measures"
- Productivity leaders grow more slowly in less competitive markets. These are conditional correlations, not causal estimates.

### US evidence linking concentration to weak investment

- [PEER-REVIEWED] Gutiérrez & Philippon, "Investmentless Growth: An Empirical Investigation", *Brookings Papers on Economic Activity* 2017(2): 89–190, doi:10.1353/eca.2017.0013 (NBER WP 22897).
  - US investment has been weak relative to Tobin's Q (the ratio of a firm's market value to the replacement cost of its capital) since the early 2000s.
  - There is "no support for theories based on risk premia, financial constraints, or safe asset scarcity". There is "fairly strong support for the competition and short-termism/governance hypotheses".
  - "Industries with more concentration and more common ownership invest less", and firms "spend a disproportionate amount of free cash flows buying back their shares."
- [WP] Gutiérrez & Philippon, "Declining Competition and Investment in the U.S.", NBER WP 23583 (July 2017): causal evidence that competition raises investment.
- **Canada vs US:** the OECD (2025) notes the rise in concentration in Canada has been "milder than in the United States". The Canadian mechanism is more about **declining entry and dynamism** than about large increases in concentration.

---

## D. Direct evidence on wealth/capital taxes and investment, entrepreneurship and employment

### D.1 Norway

- [PEER-REVIEWED] **Bjørneby, Markussen & Røed**, "An imperfect wealth tax and employment in closely held firms", *Economica* 90(358): 557–583 (2023; online Dec 2022), doi:10.1111/ecca.12456. The working-paper title was "Does the Wealth Tax Kill Jobs?" (IZA DP 13766, 2020).
  - Uses Norwegian registers and the 2007–2017 reforms.
  - "Our results indicate a positive causal relationship between the level of a household's wealth tax and subsequent employment growth in the taxpayers' closely held firms."
  - Mechanism: intangible and business assets are effectively favoured, so owners shift wealth into their firms. That is partly an avoidance channel.
- [PEER-REVIEWED] **Ring**, "Wealth Taxation and Household Saving: Evidence from Assessment Discontinuities in Norway", *Review of Economic Studies* 92(5): 3375–3402 (2025; online 15 Oct 2024), doi:10.1093/restud/rdae100.
  - "Each additional NOK of wealth tax increases annual net financial saving by 3.76". Wealth taxation "causes households to save more", financed mainly by higher labour earnings.
- [WP] **Ring**, "Taxing Entrepreneurial Wealth: Evidence from Norway, 2021–2025", NBER WP 35854 (Oct 2026), doi:10.3386/w35854. Covers the 2021–24 reforms: higher dividend tax, closed exit-tax loopholes, and a near-doubling of the effective marginal tax rate on business wealth.
  - Migration responses are "concentrated among the top 0.1% … mostly confined to 2022 and 2023". "at most 100 individuals left due to the reforms".
  - "no evidence of significant outmigration or reduced inflows of startup founders or inventors".
  - Without outmigration, wealth-tax revenue would have risen 75% from 2021 to 2024. With it, 71.5%.
  - "no evidence that Norwegian-owned firms pay out more dividends or invest less after the reforms, nor … more bankruptcies".
  - This is very new and not yet peer-reviewed.
- [WP — counter-evidence] **Berzins, Bøhren & Stacescu**, "Illiquid Owners and Firm Behavior: Financial and Real Effects of the Personal Wealth Tax on Private Firms" (earlier "Shareholder Illiquidity and Firm Behavior"), ECGI Finance WP 646/2019, last revised 2021.
  - https://www.ecgi.global/publications/working-papers/illiquid-owners-and-firm-behavior-financial-and-real-effects-of-the
  - Norway 2000–2010, about 33,000 family firms a year.
  - Higher wealth tax on the owner's home leads to higher dividends and lower cash holdings, investment, sales growth and profitability. The effect is strongest in small and medium firms with lower-wealth owners.
  - The authors suggest raising the payment threshold.
  - The 2020 version reports magnitudes of about a 0.5-point rise in the dividends-to-earnings ratio and a 0.5-point fall in sales growth per 1-point rise in the tax-to-liquid-assets ratio (from secondary summary; check the current PDF before quoting).
  - Related: [PEER-REVIEWED] Berzins, Bøhren & Stacescu, "Dividends and taxes: The moderating role of agency conflicts", *Journal of Corporate Finance* 58: 583–604 (2019), doi:10.1016/j.jcorpfin.2019.07.003.
- **"Exodus" narrative.** Media (e.g., the Financial Times 2023, cited by Ring) reported wealthy Norwegians moving to Switzerland after the 2022 increases. Ring's (2026) causal estimate is that at most 100 people left. Be honest: mobility at the very top is real, but it is small in number and in revenue terms.

### D.2 Switzerland

[PEER-REVIEWED] **Brülhart, Gruber, Krapf & Schmidheiny**, "Behavioral Responses to Wealth Taxes: Evidence from Switzerland", *AEJ: Economic Policy* 14(4): 111–150 (2022), doi:10.1257/pol.20200258.
- "a 1 percentage point drop in a canton's wealth tax rate raises reported taxable wealth by at least 43 percent after 6 years".
- "24 percent of the effect arises from taxpayer mobility and 21 percent from a concurrent rise in housing prices". "Savings responses appear unable to explain more than a small fraction of the remainder, suggesting sizable evasion responses in this setting with no third-party reporting of financial wealth."
- This is the strongest *counter*-evidence on base erosion. It is about reporting and mobility between cantons, not real investment.

### D.3 Denmark

[PEER-REVIEWED] **Jakobsen, Jakobsen, Kleven & Zucman**, "Wealth Taxation and Wealth Accumulation: Theory and Evidence from Denmark", *QJE* 135(1): 329–388 (2020), doi:10.1093/qje/qjz032.
- Uses the 1989 reform as a quasi-experiment.
- "clear reduced-form effects of wealth taxes in the short and medium run, with larger effects on the very wealthy".
- The long-run elasticity of taxable wealth with respect to the net-of-tax return is "sizable at the top of the distribution".
- The outcome is wealth accumulation and reported wealth, not firm investment.

### D.4 Spain

[PEER-REVIEWED] **Agrawal, Foremny & Martínez-Toledano**, "Wealth Tax Mobility and Tax Coordination", *AEJ: Applied Economics* 17(1): 402–430 (2025), doi:10.1257/app.20220615.
- After Spain reintroduced the tax, all regions except Madrid levied it.
- Mobility responses are "within the range of prior estimates with respect to income taxes". However, they "generate losses to personal income tax revenues that are six times larger than the direct losses to wealth taxes". Madrid could gain revenue under a harmonised positive rate.
- This is counter-evidence for *sub-national* wealth taxes and an argument for federal or national design, which is relevant to Canadian federal–provincial design.

Also [WP] Durán-Cabré, Esteller-Moré & Mas-Montserrat (Catalonia, SSRN 2019): avoidance, not real responses. Higher wealth tax "did not have a negative effect on taxpayers' savings". The elasticity of taxable wealth is 0.64, mainly through business-asset exemptions.

### D.5 Colombia

- [PEER-REVIEWED] **Londoño-Vélez & Ávila-Mahecha**, "Enforcing Wealth Taxes in the Developing World: Quasi-Experimental Evidence from Colombia", *AER: Insights* 3(2): 131–148 (2021), doi:10.1257/aeri.20200319.
  - "two-fifths of the wealthiest 0.01 percent evade taxes, with these evaders concealing one-third of their wealth offshore".
  - Stronger enforcement raises compliance, revenue and progressivity.
- [PEER-REVIEWED] **Londoño-Vélez & Ávila-Mahecha**, "Behavioural Responses to Wealth Taxation: Evidence from Colombia", *Review of Economic Studies* 92(4): 2624–2655 (2025; online July 2024), doi:10.1093/restud/rdae076 (NBER WP 32134, 2024).
  - Taxpayers "instantly reduce the wealth they declare", with persistent "hysteresis" effects.
  - The response comes through misreporting items authorities cannot cross-verify, such as overstated debt and understated business assets, plus offshoring.

### D.6 France (ISF repealed 2018; replaced by IFI on real estate; flat tax PFU)

[PRIMARY] France Stratégie, Comité d'évaluation des réformes de la fiscalité du capital, *Quatrième rapport / rapport final*, "Avis du comité" (dated 9 Oct 2023; published 16–17 Oct 2023).
- https://www.strategie-plan.gouv.fr/files/files/Publications/Rapport/fs-2023-rapport-isf-quatrieme_rapport_complet_17octobre_avis_0.pdf

Key findings, my translation of the French; page numbers are the report's:
- p.19: "Research has not detected any impact of the PFU on investment and wages in the firms most exposed to the PFU". "No effect was detected on investment and wages among firms owned more by individuals". The extra dividends were financed by lower net saving inside the firm.
- p.19: this absence of an effect is "in line with numerous academic studies… which fail to show that a change in capital taxation borne by households can have a notable effect on the real behaviour of firms". "Conversely, the literature identifies an effect of changes in corporate income tax on firm investment and labour demand." This is exactly the household/owner vs firm distinction.
- p.16: on whether ISF exposure led family firms to pay more dividends or invest less, it seems "difficult to establish such an effect" (IPP estimates).
- p.20 (positive evidence for the reforms): business creation grew faster in sectors most exposed to the 2018 reforms, mainly because of the PFU and the CIT cut rather than the ISF repeal. Effects on entrepreneur departures and returns were "favourable" but "one cannot rigorously conclude to a causal effect", and "the effects found are of small magnitude".
- Departures and returns: since the switch to the IFI, returns exceed departures (380 returns vs 260 departures a year, 2018–2021), against 370 returns vs 950 departures a year under the ISF (2011–2016).
- Fiscal cost: the ISF's replacement cost "likely more than €4bn" in 2022. A hypothetical ISF would have raised €6.3bn against €1.83bn of IFI, a loss of about €4.5bn.

### D.7 Sweden (wealth tax abolished 2007)

- [PEER-REVIEWED] **Seim**, "Behavioral Responses to Wealth Taxes: Evidence from Sweden", *AEJ: Economic Policy* 9(4): 395–421 (2017), doi:10.1257/pol.20150290.
  - Elasticities of taxable wealth are 0.09–0.27. About a third is underreporting of asset values. Responses reflect "evasion and avoidance rather than changes in saving".
- [PEER-REVIEWED] Henrekson & Du Rietz, "The Rise and Fall of Swedish Wealth Taxation", *Nordic Tax Journal* 2014(1): 9–35, doi:10.1515/ntaxj-2014-0002. A history: the effective rates on business owners and the rationale for repeal (capital flight, exemptions that made the tax fall on the upper-middle class).
- **Investment after the 2007 repeal:** I found no credible causal study showing an investment or productivity boost after the repeal. Do not claim either way without a source.

### D.8 OECD (2018), *The Role and Design of Net Wealth Taxes in the OECD*, Tax Policy Studies No. 26

[PRIMARY] doi:10.1787/9789264290303-en. Summary PDF: https://www.oecd.org/content/dam/oecd/en/topics/policy-issues/tax-policy/role-and-design-of-net-wealth-taxes-in-the-oecd-summary.pdf

- "While 12 countries had net wealth taxes in 1990, there were only four OECD countries that still levied recurrent taxes on individuals' net wealth in 2017."
- "there is a strong case for addressing wealth inequality through the tax system."
- "from both an efficiency and equity perspective, there are limited arguments for having a net wealth tax in addition to broad-based personal capital income taxes and well-designed inheritance and gift taxes … net wealth taxes tend to be more distortive and less equitable" (because the tax is levied regardless of returns).
- **But:** "there are stronger arguments for having a net wealth tax in the absence of broad-based personal capital income taxes and taxes on wealth transfers… in countries where capital gains are not taxed [or taxed lightly] … A similar argument can be made for countries that do not levy taxes on inheritances."
  - Canada fits this: only 50% of capital gains are included in income, with no inheritance or estate tax since 1972.
  - Use this carefully. It is an inference from the OECD's logic, not an OECD statement about Canada.
- Design recommendations: a high exemption threshold, low rates, limited exemptions, market-value base, third-party reporting, payment by instalments for liquidity-constrained taxpayers, and information exchange.

### D.9 Owner-level vs firm-level capital tax cuts (US)

- [PEER-REVIEWED] **Yagan**, "Capital Tax Reform and the Real Economy: The Effects of the 2003 Dividend Tax Cut", *AER* 105(12): 3531–3563 (2015), doi:10.1257/aer.20130098.
  - "the tax cut caused zero change in corporate investment and employee compensation".
- [PEER-REVIEWED] **Zwick & Mahon**, "Tax Policy and Heterogeneous Investment Behavior", *AER* 107(1): 217–248 (2017), doi:10.1257/aer.20140855.
  - Bonus depreciation "raised investment in eligible capital relative to ineligible capital by 10.4 percent between 2001 and 2004 and 16.9 percent between 2008 and 2010". Small firms respond 95% more.
  - The contrast with Yagan: incentives targeted at *new investment* work.
- [PEER-REVIEWED, counter/nuance] **Ohrn**, AEJ: Economic Policy 10(2): 272–301 (2018), doi:10.1257/pol.20150378.
  - A 1-point cut in the effective corporate rate "increases investment by 4.7 percent of installed capital".
- [PEER-REVIEWED, nuance] **Chodorow-Reich, Zidar & Zwick**, "Lessons from the Biggest Business Tax Cut in US History", *Journal of Economic Perspectives* 38(3): 61–88 (2024), doi:10.1257/jep.38.3.61.
  - The 2017 Tax Cuts and Jobs Act cut corporate tax revenue by 40%.
  - A "loose consensus" estimate is that tangible corporate investment rose about 11%. Long-run GDP is up by less than 1%, and labour income by less than $1,000 per employee. Accelerated depreciation gives more investment per dollar of revenue.
- [PEER-REVIEWED, Canadian counter-evidence]
  - Ferede & Dahlby, *National Tax Journal* 65(3): 563–594 (2012), doi:10.17310/ntj.2012.3.03.
  - Dahlby & Ferede, *FinanzArchiv* 77(1): 59–82 (2021), doi:10.1628/fa-2021-0002: "a one-percentage-point reduction in the provincial CIT rate increases the growth rate by 0.12 percentage point four years after the initial CIT rate cut" (provinces, 1981–2016).
  - The authors are also frequent C.D. Howe and Fraser contributors, but these are peer-reviewed. Include them for balance.

### D.10 Taxes on the rich, inequality and growth (macro)

- [PEER-REVIEWED] **Hope & Limberg**, "The economic consequences of major tax cuts for the rich", *Socio-Economic Review* 20(2): 539–559 (2022), doi:10.1093/ser/mwab061.
  - Sample: 18 OECD countries, 1965–2015, including Canada. 30 "major tax cut" episodes, identified as a fall of more than 2 standard deviations in a latent index of taxes on the rich.
  - Each major cut raised the top 1% pre-tax income share by about 0.8 points after 5 years.
  - Real GDP per capita and unemployment were "not significantly affected".
- [PEER-REVIEWED] **Piketty, Saez & Stantcheva**, "Optimal Taxation of Top Labor Incomes: A Tale of Three Elasticities", *AEJ: Economic Policy* 6(1): 230–271 (2014), doi:10.1257/pol.6.1.230.
  - "Top tax rate cuts are associated with top one percent pretax income shares increases but not higher economic growth."
- [PRIMARY / IMF] **Ostry, Berg & Tsangarides**, "Redistribution, Inequality, and Growth", IMF Staff Discussion Note SDN/14/02 (Feb 2014), doi:10.5089/9781484352076.006.
  - "there is not a lot of evidence that redistribution has in fact undercut economic growth (except in extreme cases)". Lower net inequality is associated with faster and more durable growth.
- [PRIMARY / OECD WP] **Cingano**, "Trends in Income Inequality and its Impact on Economic Growth", OECD SEM WP No. 163 (2014), doi:10.1787/5jxrjncwxv6j-en.
  - Inequality has a negative, significant effect on subsequent growth, driven by the gap between *low*-income households and the rest.
  - **No evidence that top earners pulling away harms growth.** Cite this accurately: it supports redistribution towards the bottom, not "taxing the top raises growth".
- [PEER-REVIEWED, synthesis] **Advani & Tarrant**, "Behavioural responses to a wealth tax", *Fiscal Studies* 42(3–4): 509–537 (2021), doi:10.1111/1475-5890.12283.
  - Estimates of the elasticity of taxable wealth "vary by a factor of 800". "a well-designed wealth tax would reduce the tax base by 7–17 per cent if levied at a tax rate of 1 per cent."
  - Useful benchmark against the PBO's 35% assumption.

---

## E. PBO costings of Canadian wealth-tax proposals

[PRIMARY] Office of the Parliamentary Budget Officer.

| Date | Proposal | Rate / threshold | Revenue estimate | Behavioural assumption | Admin cost | Emigration |
|---|---|---|---|---|---|---|
| 8 Jul 2020 (RP-2021-017-M; request of MP Peter Julian) | Annual net wealth tax on economic families | 1% above $20M | **$5.6bn net in 2020-21** (gross $5.666bn, admin $113M); 13,800 families | **Each family's net wealth reduced by 35%** ("based on adjusted findings from the IRS") | 2% of gross | Not modelled separately |
| 15 Jul 2021 (M-68, MP Nathaniel Erskine-Smith) | One-time tax on accumulated wealth, paid over 5 years | 3% above $10M; 5% above $20M | **$44.0bn over 5 years** (35% assumption) or **$60.7bn** (new modelled response) | New model: 15% of wealth avoided/evaded at a 3% rate, 19.8% at 5%. Calibrated to Saez–Zucman's 33% avoidance under the US estate tax. With no response: $82.5bn; base $1,892bn vs $1,008bn (35%) vs $1,403bn (model) | 2% | Not modelled separately |
| Sep 2021 (election costing, NDP platform) | Annual net wealth tax | 1% above $10M | **$60.07bn over 2021-22 to 2025-26** ($10.85bn rising to $13.12bn a year) | 35% reduction of each family's net wealth | 2% | Not modelled separately |
| 9 Apr 2025 (election costing, EL-45-1029379-P) | Annual net wealth tax from 1 Jul 2025 | 1% on $10–50M; 2% on $50–100M; 3% above $100M | **$121.5bn over 2025-26 to 2029-30** ($22.8bn rising to $26.8bn a year) | "35% of each household's net worth would be hidden and reduced from the overall tax base" | 2% | Not modelled separately |

Source documents:
- 2020: https://www.pbo-dpb.ca/en/publications/RP-2021-017-M--net-wealth-tax-canadian-resident-economic-families--impot-patrimoine-net-familles-economiques-residant-au-canada (PDF: https://distribution-a617274656661637473.pbo-dpb.ca/c40540501fd9ddd5ea3593afe7d09d9637f58c59ef53c17406f9e39a29eb50aa)
- 2021 (M-68): https://distribution-a617274656661637473.pbo-dpb.ca/a2d27dbe13ecedf7ebdac816acc5ddb4ab59d297dcbd77caddd75a346472afb7
- Sep 2021: https://distribution-a617274656661637473.pbo-dpb.ca/e45d50992f5e3c2e474f60ce79b76664db4bdf21c2f2059901c3575c65f6c0fd
- 2025: https://distribution-a617274656661637473.pbo-dpb.ca/cc1b029d8d36c1340f715cb7666bfd2132ef6683608cecae42a5e71b82bb6ad6

Notes:
- **2023/2024:** I found no separate PBO *annual* wealth-tax costing dated 2023 or 2024. The 2025 costing uses the updated High-net-worth Family Database (HFD; 2023 Survey of Financial Security). If the author has a specific 2023/24 costing in mind, it needs to be located before citing.
- **Wealth concentration context:**
  - PBO, "Estimating the top tail of the family wealth distribution in Canada" (17 Jun 2020): top 1% share 25.6% (HFD) vs 13.7% in the StatCan Survey of Financial Security.
  - PBO 2025 update (11 Sep 2025): top 1% hold 24%, top 10% hold 53%, bottom 40% hold 3.3%. About 169,400 families in the top 1% (≥$7.4M) in 2023.
  - Update: https://distribution-a617274656661637473.pbo-dpb.ca/94e59eedd1df221279b80e2f1378103b3d8f81a9388b667f54db173d1d5321a8
- **Assessment of the 35%:**
  - It is a single combined avoidance/evasion haircut. It includes no explicit emigration module and no macro feedback on investment or GDP.
  - It is higher than the Advani–Tarrant "well-designed tax" range of 7–17% at a 1% rate, and higher than the 15% assumed by Saez and Zucman for their US proposal (per secondary summaries; not checked here).
  - It is lower than Swiss-style responses (≥43% per rate point after 6 years, without third-party reporting).
  - So it is a deliberately cautious, conservative-for-revenue assumption. It is not evidence of real economic damage.
  - Critics (e.g., [SECONDARY] The Breach / Canadians for Tax Fairness) call it too high. Fiscal conservatives could cite Switzerland to call it too low.

---

## F. Historical: postwar Canada with high top rates and an estate tax

### F1. Productivity growth by period. File: `F1_business_sector_productivity_growth_by_period.csv` (and the annual index file)

**Source.** [PRIMARY] StatCan 36-10-0208-01, business sector.
- Labour productivity: v41712898
- Multifactor productivity (MFP): v41712881
- Real GDP: v41712932
- https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610020801
- The series starts in 1961, so the 1950s are not covered by this table.

| Period | Labour productivity (% a year) | MFP (% a year) | Business real GDP (% a year) |
|---|---|---|---|
| 1961–73 | 3.55 | 1.53 | 5.62 |
| 1973–81 | 1.80 | 0.15 | 3.91 |
| 1981–2000 | 1.76 | 0.47 | 3.17 |
| 2000–08 | 0.87 | −0.54 | 2.09 |
| 2008–19 | 0.91 | 0.16 | 1.83 |
| 2000–23 | 0.78 | −0.16 | 1.84 |

### F2. Marginal tax rates. File: `F2_marginal_tax_rates_canada_1920_2000_saez_veall.csv`

**Source.** [PEER-REVIEWED] Saez & Veall, "The Evolution of High Incomes in Northern America: Lessons from Canadian Evidence", *AER* 95(3): 831–849 (2005), doi:10.1257/0002828054201404. The data table is from NBER WP 9607 (2003), Appendix Table E1. Rates are combined federal plus Ontario.

| Year | Statutory top rate (%) | At P99.9 (%) | At P99.99 (%) |
|---|---|---|---|
| 1950 | 80 | 45 | 55 |
| 1960 | 77 | 48 | 57.5 |
| 1970 | 77.3 | 54.6 | 62.8 |
| 1972 | 59.1 | 54 | 59.1 |
| 1988 | 46.1 | 46.1 | 46.1 |
| 2000 | 47.9 | 47.9 | 47.9 |

**Important honesty caveat** (Saez & Veall): "the top marginal tax rate is a very imperfect measure of the burden of taxation, as extremely few taxpayers had incomes large enough to be in the top bracket". "percentile P99.9 faced a rate of about 45% in 1950 and about 48% in 2000… only the super top (around 1,000 individuals within the top 0.01%) had a decline in marginal tax rates of 10 percentage points or more."

So "80% top rates" in Canada applied to a handful of people. The better claim is that the *very* top faced much higher rates, and wealth was also taxed at death.

### Estate tax

- The federal estate tax (Estate Tax Act, 1958; preceded by the Dominion Succession Duty Act, 1941) and federal gift-tax provisions were repealed with the 1971 tax reform, effective 1 January 1972.
- They were replaced by capital-gains taxation and deemed disposition of capital property at death.
- Provincial succession duties persisted for some years after.
- I did not retrieve a primary legal source here. **Verify exact dates** (e.g., Canadian Tax Foundation / *Canadian Tax Journal* histories, or the Library of Parliament) before publishing.

### Assessment

- Canada's fastest measured productivity growth (1961–73: 3.55% a year labour productivity, 1.53% MFP) came with very high statutory top rates and a federal estate tax.
- Productivity growth has been slowest since 2000, after large cuts to corporate and capital-gains taxes.
- This is **consistent with** taxes not being the binding constraint. It is **not causal evidence**. The postwar era also had catch-up to US technology, the baby boom, rapid trade liberalisation (Auto Pact 1965), cheap energy, and high public infrastructure investment.
- Use it as historical context, not proof.

---

## Where the evidence is genuinely mixed or against the author's priors

1. **Base erosion is large and real.**
   - Switzerland: ≥43% higher reported wealth six years after a 1-point rate cut.
   - Colombia: hidden offshore wealth.
   - Spain: regional mobility costing six times the wealth-tax loss in income-tax revenue.
   - Denmark: sizable long-run elasticities at the top.
   - Revenue projections need conservative haircuts like the PBO's 35%. A wealth tax needs third-party reporting, valuation rules, an exit or departure tax, and national rather than provincial administration.
2. **Liquidity effects on small private firms.** Berzins, Bøhren & Stacescu find real negative effects (investment, growth) for illiquid owners of small and medium firms. Mitigations include high thresholds (all PBO-costed Canadian proposals start at $10–20M of net worth) and deferral or instalment options (as the OECD recommends).
3. **Corporate-level taxes do matter somewhat for investment.** See Ohrn, Zwick & Mahon, Chodorow-Reich et al., and Dahlby & Ferede (Canadian provinces).
   - The Canadian 2000–2012 cuts coincided with no investment boom. That does not prove the effect was zero.
   - The policy-relevant point: a *wealth tax on individuals* above $10M is not a corporate tax on marginal investment.
4. **Mobility at the very top exists** (Norway 2022–23; Spain to Madrid). It is small in headcount: at most 100 people in Norway, with revenue gains still about 71.5% versus 75% without emigration.
5. **The OECD (2018) is lukewarm on net wealth taxes in principle.** It sees them as more distortive than capital income taxes. It sees a stronger case where capital gains are lightly taxed and there is no inheritance tax, which describes Canada.
6. **The "dead money" stock is a gross figure.** It is not proof of idle capital. Present it alongside debt, or as one indicator among several.

---

## File list

All paths are relative to `data-prep/investment/ (originally a scratch folder)`.

**Notes**
- `notes.md`: this file.

**Chart-ready CSVs**
- `A1_corporate_tax_rates_oecd.csv`: Canada combined, federal and provincial CIT rates; US; OECD-38 average; 2000–2026.
- `A2_business_nonres_investment_share_gdp_annual.csv`: 1961–2025; structures, M&E, IPP and total as % of GDP.
- `A2_business_nonres_investment_share_gdp_quarterly.csv`: 1961Q1–2026Q2.
- `A3_nfc_currency_deposits_pct_gdp.csv`: non-financial (private) corporations' currency and deposits, Q4 stock, $ and % of GDP, 1990–2025.
- `A4_corporate_surplus_vs_business_investment.csv`: corporate net and gross operating surplus % of GDP vs investment % of GDP, and their ratio.
- `B1_labour_productivity_canada_us_oecd.csv`: GDP per hour and per worker, Canada / US / OECD, Canada as % of US.
- `B2_business_investment_per_worker_canada_us.csv`: real private non-residential gross fixed capital formation per worker, Canada vs US; levels (approximate) and 2014=100 index; projections flagged.
- `F1_business_sector_productivity_growth_by_period.csv`: period growth rates.
- `F1_business_sector_labour_productivity_index_annual.csv`: annual business-sector productivity indices, 1961–2023.
- `F2_marginal_tax_rates_canada_1920_2000_saez_veall.csv`

**Scripts** (run with `python3 -I`)
- `scripts/build_a.py`, `scripts/build_oecd.py`, `scripts/build_b2.py`, `scripts/build_f.py`, `scripts/h2t.py`

**Raw downloads** (`raw/`)
- StatCan zips and CSVs: 36100104, 36100103, 36100580, 36100208, 36100480. The last was downloaded but not used, because OECD data gave a cleaner Canada–US comparison.
- `raw/oecd/`: CIT, PDB and EO extracts.
- `raw/pbo/`: PBO PDFs and text.
- `raw/web/`: Rogers speech, OECD Survey 2025, France Stratégie avis, Saez–Veall WP, Hope–Limberg PDF, Competition Bureau report.
- `raw/crossref/`: DOI metadata.

## Items not verified, or to check before publication

- Exact repeal date and wording for the federal estate tax (1 Jan 1972) and the provincial succession duties.
- IMF 2025 staff report wording ("about 30% lower than the US"; young firms' output share falling from about 30% to 20%). This is from a search summary; open the staff report (IMF Country Report No. 26/12) to quote.
- Berzins–Bøhren–Stacescu magnitudes. Use the latest ECGI PDF.
- The Globe and Mail "$526-billion" figure for 2012 Q1 is a secondary report. The current StatCan series differs (see A3).
- No PBO 2023 or 2024 annual wealth-tax costing was found.
- No rigorous study was found on investment effects of the 2000 capital-gains inclusion cut or the 2007 Swedish repeal.
