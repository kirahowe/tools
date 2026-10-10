# Canada: housing, cost of living, wages and productivity — data notes

Retrieved 2026-10-10. Raw downloads are in `raw/` (one directory per source). Scripts are in `scripts/` (run from the `labour/` directory with `python3 -I scripts/<name>.py`). Every CSV in this folder can be rebuilt from `raw/` using those scripts.

StatCan full-table CSVs come from `https://www150.statcan.gc.ca/n1/tbl/csv/<pid>-eng.zip`. The table viewer for each is `https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=<pid>01`. Vector IDs (vNNN) are recorded in the CSVs wherever possible.

Source-quality labels:
- [StatCan], [OECD], [CMHC], [Finance/Tax DB]: official sources.
- [Academic]: peer-reviewed papers, CSLS, Maddison Project.
- [Charity]: Food Banks Canada.
- [Living-wage estimate]: a methodology-based estimate from a living wage network or think tank. It is not an official statistic.
- [Secondary]: the number comes from media coverage of a primary release because the primary site blocked automated fetch. Check it by hand before publishing.

---

## 1. House price to income

### 1a. OECD Analytical House Price Database: price-to-income ratio, Canada vs US, 1970-Q1 to 2026-Q2 [OECD]
- API: `https://sdmx.oecd.org/public/rest/data/OECD.ECO.MPD,DSD_AN_HOUSE_PRICES@DF_HOUSE_PRICES,/CAN+USA...?format=csvfilewithlabels`. Raw file: `raw/oecd/hp.csv`.
- Files: `oecd_house_price_to_income_can_usa_quarterly.csv` and `..._annual.csv`.
- Method: nominal house price index ÷ nominal net disposable income per capita, as an index (2015=100). OECD also publishes a "standardised" version, expressed as a % of each country's long-term average. Only the standardised version is comparable across countries. I added a column rebased to the 1970 average = 100.
- Canada's house-price input comes from national sources: the StatCan NHPI / Teranet-type series as compiled by the OECD. It is not CREA. See the OECD metadata.
- Key numbers for Canada:
  - % of long-term average: lowest 60.1 (1985-Q2), peak 174.1 (2022-Q2), latest 139.2 (2026-Q2).
  - Rebased so 1970 = 100: 2022-Q2 = 215.8 and 2026-Q2 = 172.6. In other words, house prices relative to income per head are about 1.7 times their 1970 level, and peaked at about 2.2 times.
  - Annual averages: 1970 = 100, 1985 = 76, 2000 = 93, 2015 = 143, 2022 = 208, 2025 = 187.
- United States, same rebased measure: 2022 = 103, 2025 = 99. % of long-term average peaked at 119.4 (2022-Q2) and is 109.8 latest. Canada's run-up is far larger than the US one.
- Caveat: the denominator is per-capita disposable income for the whole economy. It is neither household nor median income, so it is not a "years of median income" ratio.

### 1b. Census snapshots: dwelling value vs median household income [StatCan]
File: `census_dwelling_value_to_income_snapshots.csv`

| Census | Value | Income | Ratio |
|---|---|---|---|
| 1961 | $11,021, median value of owner-occupied single-detached non-farm dwellings | n/a | n/a |
| 1971 | $19,020, same concept (+72.6% vs 1961) | n/a | n/a |
| 2011 (NHS) | $345,182, average owner-estimated value of all owner-occupied dwellings | n/a | n/a |
| 2016 | $443,058, average owner-estimated value | $70,336, median household total income for 2015 | **6.3** |
| 2021 | ≈$618,500, derived as 2016 × 1.396 (StatCan states +39.6% nominal growth 2016→2021) | $84,000, median household total income for 2020 (table 98-10-0061-01) | **≈7.4** |

Sources for the table:
- 1961 and 1971: Canada Year Book 1976-77, p. 709 — https://www66.statcan.gc.ca/eng/1976-77/197607170709_p.%20709.pdf
- 2011 and 2016 values: https://www150.statcan.gc.ca/n1/daily-quotidien/171025/dq171025c-eng.htm
- 2015 income: https://www150.statcan.gc.ca/n1/daily-quotidien/170913/dq170913a-eng.htm
- 2021 value growth (+39.6%): https://www150.statcan.gc.ca/n1/daily-quotidien/220921/dq220921b-eng.pdf (p. 15)

Notes on the table:
- CMHC's HMIP table "Average Value of Owner-occupied Dwellings" shows a Canada total of $617,500. It appears to be for 2021, which is consistent with my derived figure. https://www03.cmhc-schl.gc.ca/hmip-pimh/en/TableMapChart/TableCategory?geographyType=Country&geographyId=1&categoryLevel1=Population,+Households+and+Housing+Stock&categoryLevel2=Value+of+Owner-occupied+Dwellings+($)
- **Gap:** I could not build a consistent census ratio back to 1951/1961. The 1961/1971 values are medians for single-detached dwellings only, and I could not retrieve a matching census median family income for 1960/1970. Those would come from the 1971 Census bulletin "Summary of family income statistics", cat. 93-747. Also, www12.statcan.gc.ca (the census site) returned HTTP 403 to automated fetch.
- Do not put the 1961/1971 values and the 2016/2021 values in one ratio chart without a footnote. The concepts differ: median vs average, and single-detached vs all owner-occupied dwellings.
- Recommendation: use OECD 1a for the long-run line. Use 6.3 (2016) and 7.4 (2021) as the "years of median household income" anchor.

### 1c. Price-to-income of actual buyers, Canadian Housing Statistics Program, table 46-10-0099-01 [StatCan]
File: `first_time_buyers_chsp_46100099.csv`. Coverage is 2021–2023, for NS, NB, MB, BC and YT only (Ontario and Quebec are not in this table).

2023 median price-to-income ratio of residential property buyers:

| | First-time buyers | Repeat buyers |
|---|---|---|
| BC | 4.3 | 5.6 |
| MB | 2.9 | 2.5 |
| NS | 2.8 | 2.4 |
| NB | 2.4 | 2.0 |

### 1d. Not done
CREA MLS HPI is industry data and was not used. The NHPI (18-10-0205) is downloaded in `raw/18100205` but not charted, because it covers new houses only.

## 2. Age of first-time buyers, homeownership by age, rents

### Median age of first-time buyers, 2021–2023 (table 46-10-0099-01, CHSP) [StatCan]
2023 medians:

| Province | First-time buyers | Repeat buyers |
|---|---|---|
| BC | 34 | 48 |
| MB | 32 | 45 |
| NB | 33 | 47 |
| NS | 33 | 48 |
| YT | 32 | 44 |

- I found no comparable official "then" figure, for example for the 1970s or 1980s. Do not claim a historical first-time-buyer age without a source.

### Canadian Housing Survey, table 46-10-0083-01 [StatCan]
File: `housing_need_and_tenure_chs.csv`.
- Definition: a first-time-homebuyer household owns its dwelling and either moved in the last 5 years from renting or used first-time homebuyer tax incentives. The age used is that of the reference person.
- Share of first-time-buyer households whose reference person is 25–34: 46.4% (2018) → 40.4% (2024).
- Share aged 35–44: 27.0% → 31.7%. First purchases are shifting older.
- Homeownership rate of households with a reference person aged 25–34 (CHS, my computation): 53.0% (2018), 50.4% (2021), 45.9% (2022), 47.1% (2024).
- All households: 68.6% (2018) → 64.4% (2024).

### Census homeownership rate [StatCan]
File: `homeownership_rate_canada_census.csv`.
- All households:

| Year | Rate |
|---|---|
| 1941 | 56.7% |
| 1961 | 66.0% |
| 1971 | 60.3% |
| 1981 | 62.1% |
| 1991 | 62.6% |
| 2001 | 65.8% |
| 2006 | 68.4% |
| 2011 | 69.0% (peak) |
| 2016 | 67.8% |
| 2021 | 66.5% |

- By age of primary maintainer:

| Age | 2011 | 2016 | 2021 |
|---|---|---|---|
| 25–29 | 44.1% | 39.6% | 36.5% |
| 30–34 | 59.2% | 55.0% | 52.3% |

- Sources:
  - 1971–2021: The Daily 2022-09-21, charts 1–2 — https://www150.statcan.gc.ca/n1/daily-quotidien/220921/cg-b001-eng.htm and https://www150.statcan.gc.ca/n1/daily-quotidien/220921/cg-b002-eng.htm
  - 1941 and 1961: Canada Year Book 1974, p. 558 — https://www66.statcan.gc.ca/eng/1974/197405700558_p.%20558.pdf
- 2011 is from the NHS, a voluntary survey, so it is not strictly comparable.
- **Gap:** I did not obtain census rates by age for 1981–2006. StatCan paper 11F0027M2013083 covers 1981–2006 by age and income: https://www150.statcan.gc.ca/n1/daily-quotidien/130129/dq130129a-eng.htm

### Rents [CMHC, via CP / StatCan]
- CMHC Rental Market Report, October 2025 survey (released 2025-12-11):
  - Average rent for a purpose-built 2-bedroom: $1,550, +5.1% y/y (+5.4% in 2024).
  - National vacancy rate: 3.1% (2.2% in 2024; record low of 1.5% in 2023).
  - These come from press coverage [Secondary]: https://www.ctvnews.ca/business/article/rent-growth-slowed-in-2025-as-national-vacancy-rate-continued-to-rise-cmhc/. Confirm against the CMHC report: https://www.cmhc-schl.gc.ca/professionals/housing-markets-data-and-research/market-reports/rental-market-reports-major-centres
- CPI components, table 18-10-0005-01 (file `cpi_components_canada_annual.csv`):

| Component | 2019→2025 | 2000→2025 |
|---|---|---|
| All-items | +20.7% | +72.1% |
| Rent | +30.6% | +68.6% |
| Shelter | +30.0% | +96.4% |
| Mortgage interest cost | +56.6% | n/a |
| Food | +30.0% | n/a |

  - The CPI rent index is widely considered to understate market-rent growth for movers.

### Core housing need [StatCan]
File: `housing_need_and_tenure_chs.csv`. Source: CHS, table 46-10-0085-01.
- Renter households in core housing need: 22.0% in 2024, which is 1,225,400 households.
- 22.0% in 2018, 19.7% in 2021 (pandemic benefits), 22.0% in 2022.
- All households: 11.6% in 2024.
- The 2021 Census measured it differently: 10.1% of all households, down from 12.7% in 2016 (The Daily 2022-09-21).

## 3. Household debt

### StatCan series [StatCan]
File: `household_debt_canada_quarterly.csv`. Quarters are labelled YYYY-Qn; StatCan's own REF_DATE uses the quarter's first month.
- Credit market debt to disposable income, table 38-10-0238-01, vector v1038036698:

| Quarter | Ratio |
|---|---|
| 1990-Q1 | 89.6% |
| 2000-Q1 | 114.0% |
| 2008-Q1 | 152.7% |
| 2022-Q3 (peak) | 188.2% |
| 2026-Q2 (latest) | 176.4% |

- Debt service ratio, principal + interest, table 11-10-0065-01, v1001696813, seasonally adjusted:

| Quarter | Ratio |
|---|---|
| 1990-Q1 | 12.2% |
| 2019-Q1 | 15.1% |
| 2023-Q1 (peak) | 15.2% |
| 2026-Q2 | 14.5% |

- Interest-only debt service ratio: 10.6% (1990-Q1), 5.9% (2022-Q1), 8.5% (2026-Q2).

### Canada vs US, OECD National Accounts at a Glance, ch. 5, measure LES1M_FD4 [OECD]
File: `household_debt_oecd_can_usa_annual.csv`. API: `https://sdmx.oecd.org/public/rest/data/OECD.SDD.NAD,DSD_NAAG@DF_NAAG_V,/A.CAN+USA...?format=csvfilewithlabels`.
- Measure: debt of households and NPISH as % of net disposable income.

| Year | Canada | US |
|---|---|---|
| 1995 | 108% | 95% |
| 2007 | 156% | 145% (US peak) |
| 2024 | 181% | 99% |

- US households deleveraged after 2008; Canadian households did not.

## 4. Incomes, poverty, living wage, food banks

### Canadian Income Survey (2024 reference year, released 2026), in 2024 constant dollars [StatCan]
File: `income_canada_cis_annual.csv`.
- Median total income of economic families (2+ persons), table 11-10-0190-01, v96411932:

| Year | Value |
|---|---|
| 2024 | **$125,600** |
| 2019 | $117,000 |
| 2000 | $94,100 |
| 1976 | $89,300 |

- Median after-tax income of economic families: $108,900 (2024).
- All units (families and unattached persons): median total $86,000; median after-tax $75,500.
- Couples with children, median total income: $157,600 (2024) vs $98,900 (1976).
- Individuals 15+, median total income (table 11-10-0239-01, v107660858): **$46,300** (2024), $37,500 (1976).
- Individuals 15+, median employment income (v107660868):

| Year | Value |
|---|---|
| 2024 | **$46,100** |
| 1976 | $43,200 |
| 1990 | $39,000 |
| 2007 | $40,100 |

  - That is only about +7% in real terms over 48 years.
  - Family income rose more, largely because of more dual-earner households and more hours worked by women.

### Share of tax filers with total income ≥ $100,000 (T1FF, table 11-10-0008-01, v20790263 / v20790253; nominal dollars) [StatCan]
File: `share_income_over_100k_t11100008.csv`.
- 2023: 14.8%, or 4.54 million people out of 30.65 million with income.
- 2019: 10.1%. 2000: 2.4%.
- ≥ $200,000: 2.6% (2023).
- Median total income of individuals in 2023: $45,000.
- Caveat: the thresholds are not indexed to inflation, so the share rises partly through inflation. It does not mean a rising share of people are "rich".

### Official poverty line, Market Basket Measure [StatCan]
- Poverty rate (table 11-10-0135-01, MBM 2023 base, v1792630890): **11.0% in 2024** and 11.1% in 2023. File `poverty_rate_canada_cis.csv`.
- On the 2018 base: 10.5% (2019), 6.5% (2020, pandemic transfers), 10.3% (2022), 10.4% (2023).
- MBM thresholds for a reference family of 2 adults + 2 children, after-tax, current dollars, 2025, 2023 base (table 11-10-0066-01; file `mbm_thresholds_cities_current_dollars.csv`):

| City | Threshold | Of which shelter |
|---|---|---|
| Toronto | $62,880 | $26,791 |
| Vancouver | $64,351 | $28,147 |
| Montréal | $50,287 | $14,193 |
| Calgary | $59,153 | $21,643 |
| Halifax | $54,855 | $18,243 |

### Living wage vs minimum wage [Living-wage estimate]
File: `living_wage_vs_minimum_wage_cities.csv`.

| City | Living wage | Minimum wage | Source |
|---|---|---|---|
| GTA (Toronto) | $27.20/h (2025) | $17.60 (Oct 2025); $17.95 from Oct 1, 2026 | Ontario Living Wage Network, Nov 10 2025, via CP24 [Secondary]; discrepancy below |
| Metro Vancouver | $27.85/h (2025; $27.05 in 2024) | $18.25 (Jun 1 2026) | Living Wage BC, Nov 13 2025 [Secondary, Daily Hive]; minimum wage from gov.bc.ca |
| Calgary | $26.50/h (2025) | $15.00 (frozen since Oct 2018) | Vibrant Communities Calgary [Secondary]; Alberta averages across household types; Medicine Hat lists $26.20 |
| Halifax | $29.40/h (2025; $28.30 in 2024) | $17.00 (Oct 1 2026) | CCPA-NS report PDF (primary) |
| Montréal | about $30/h for a single person at 35 h/week | $16.60 (May 1 2026) | IRIS "revenu viable" 2026 |

Notes on the table:
- GTA discrepancy: CCPA (2026-09-28) gives a 2025 Ontario range of "$21.05 to $27.60", which conflicts with $27.20. This is unresolved, and ontariolivingwage.ca returned 403.
- Montréal: IRIS uses a different concept. The revenu viable is $41,585 after tax for a single person, and full-time work at minimum wage reaches only 66% of it. Source: https://iris-recherche.qc.ca/wp-content/uploads/2026/04/IRIS-%E2%80%93-RV-2026-WEB.pdf. IRIS is a Quebec think tank. Label this as an estimate, not a Living Wage Canada figure.
- The Living Wage Canada standard is based mainly on a two-parent, two-child family with both parents working full time. Alberta now averages across household types, so its rate is not strictly comparable.

### Food banks: Food Banks Canada, HungerCount 2025 (March 2025) [Charity]
Source: https://foodbankscanada.ca/press-releases/canadas-2025-hungercount-shows-the-cost-of-inaction/. File: `food_bank_hungercount_2025.csv`.
- **2,166,000 visits** in March 2025.
- +5% vs 2024 and **+99.4% vs March 2019**.
- **19.4% of clients are employed**, vs 12.2% in 2019.
- 33% of clients are children, about 712,000 visits.
- HungerCount 2026 had not been released as of 2026-10-10. It usually comes out in late October.

## 5. Productivity vs pay

### Files
- `productivity_pay_canada_annual.csv`: wide format, 1961–2025.
- `productivity_pay_canada_1946_2001_archived.csv`
- `productivity_pay_index_long.csv`: long format with panels A–E, ready to chart.

### Series and sources
All CPI deflation uses CPI all-items, table 18-10-0005-01 (v41693271).
- **A (1946–2001), archived table 36-10-0305-01, SIC basis.**
  - Business-sector labour productivity (v720290) and total compensation per hour (v720734), deflated by CPI.
  - 1946–1975: productivity **+3.75%/yr**, real compensation per hour **+3.89%/yr**. They moved in lockstep.
  - 1975–2000: productivity **+1.53%/yr**, real compensation per hour **+0.52%/yr**.
  - Caveat: the 1946 compensation index is published with only one decimal (3.5), so treat the early levels as approximate.
- **B (1961–2023), table 36-10-0208-01, business sector.**
  - Labour productivity index (v41712898), and labour compensation (v41713170) ÷ hours worked (v41712966), deflated by CPI.
  - 1961 → 2023: productivity **+192%** (index 292); real compensation per hour **+149%** (index 249).
- **C (1981–2025), table 36-10-0206-01, quarterly, averaged to years.**
  - Labour productivity (v1409153) and total compensation per hour (v1409158), deflated by CPI.
  - 1981 → 2025: productivity **+64.4%**; real compensation per hour **+45.9%**.
- **D (1997–2025), with Labour Force Survey wages, table 14-10-0064-01.**
  - The median hourly wage of all employees was $14.00 in 1997 and $30.77 in 2025, nominal.
  - In 2025 dollars, the 1997 median was $25.43, so the real median rose **+21.0%**.
  - The real average hourly wage rose +28.5%.
  - Business-sector productivity rose **+34.7%**, and real compensation per hour +35.2%.
- **E: labour share.** Computed as labour compensation ÷ nominal business-sector GDP from table 36-10-0208-01:

| Year | Share |
|---|---|
| 1961 | 61.8% |
| 1976 | 61.1% |
| 1991 | 63.1% |
| 2006 | 55.7% |
| 2019 | 57.8% |
| 2021 | 56.3% |

  - Table 36-10-0206-01 also has a labour-share index (v20805661, 2017=100).

### Interpretation (accuracy matters here)
- Before about 1975, Canadian pay tracked productivity.
- From the mid-1970s to the late 1990s, average real compensation fell well behind productivity.
- Since 1997, **average** compensation per hour, deflated by CPI, has roughly kept pace with business-sector productivity. Part of the reason is that Canada's terms-of-trade gains raised what wages could buy.
- The **median** wage has lagged both: +21% vs +35%. So the Canadian gap is now mostly a median-vs-average (inequality) gap and a labour-share story, not a story of average pay falling behind.
- Do not reproduce the EPI US chart's message one-for-one.

### CSLS / academic sources [Academic]
- Uguccioni, J. (2016). "Explaining the Gap between Productivity and Median Wage Growth in Canada, 1976–2014." *International Productivity Monitor* 31: 37–56. https://csls.ca/ipm-archive/ipm-issue-31/explaining-the-gap-between-productivity-and-median-wage-growth-in-canada-1976-2014.md
  - Abstract: median real hourly earnings grew 0.09%/yr while labour productivity grew 1.12%/yr, a gap of 1.03 percentage points.
  - Rising earnings inequality accounts for about half of the gap. The other half is the falling labour share plus the deterioration in labour's terms of trade (consumer prices rising faster than output prices).
- Sharpe, A., J.-F. Arsenault & P. Harrison (2008). "Why Have Real Wages Lagged Labour Productivity Growth in Canada?" *International Productivity Monitor* 17: 16–27. https://csls.ca/ipm-archive/ipm-issue-17/why-have-real-wages-lagged-labour-productivity-growth-in-canada.md
  - Between 1980 and 2005, median real earnings stagnated while labour productivity rose 37%.
  - It names four factors of roughly equal importance: rising earnings inequality, falling terms of trade for labour, a falling labour share, and measurement issues.
  - The full report is CSLS Research Report 2008-08.
- Sharpe & Uguccioni (2017), "Decomposing the Productivity-Wage Nexus in Selected OECD Countries, 1986-2013", *International Productivity Monitor* 32. https://csls.ca/ipm-archive/ipm-issue-32/decomposing-the-productivity-wage-nexus-in-selected-oecd-countries-1986-2013.md. I did not extract its Canada-specific decomposition numbers.

## 6. Top tax rates and top income shares

### Top marginal income tax rate, 1920–2025
- Files: `top_marginal_tax_rate_canada_vs_us_1920_2025.csv` and `top_marginal_tax_rates_canada_saez_veall_1920_2000.csv`, which also has marginal rates at P90/P99/P99.9/P99.99.
- 1920–1999 [Academic]: Saez & Veall data file `TabCanVolume-web.xls`, Table F1, column "Top". https://eml.berkeley.edu/~saez/TabCanVolume-web.xls
  - The file accompanies Saez & Veall (2005), AER 95(2), and their 2007 chapter in Atkinson & Piketty (eds.), *Top Incomes over the Twentieth Century*.
  - Per the F1 notes: rates are federal only before 1972 ("these included provincial income tax rates in most cases"), and federal + Ontario from 1972, including surtaxes.
- Values:

| Year | Top rate |
|---|---|
| 1943–44 | 95% |
| 1945 | 91.2% |
| 1950 | 80% |
| 1952 | 91% |
| 1955 | 76% |
| 1960 | 77% |
| 1965 | 75% |
| 1971 | 76.1% |
| 1972 | 59.1% (Carter/Benson reform) |
| 1975 | 61.3% |
| 1981 | 62.8% |
| 1982 | 50.3% |
| 1986 | 54.9% |
| 1988 | 46.1% (Wilson tax reform: brackets cut to three) |
| 1995 | 52.3% |
| 2000 | 47.9% |

- 2000–2025 [OECD Tax Database]: DF_PIT_TOP_EARN_THRESH, measure TS_PIT, which is central + sub-central; for Canada that means federal + Ontario. https://sdmx.oecd.org/public/rest/data/OECD.CTP.TPS,DSD_TAX_PIT@DF_PIT_TOP_EARN_THRESH,/CAN+USA...........?format=csvfilewithlabels
  - Canada: 47.86% (2000), 46.41% (2005–2010), 49.53% (2015), **53.53% (2016–2025)**. The jump in 2016 came from the new 33% federal bracket.
  - US (federal + state, OECD method): 46.65% (2000), 43.65% (2025).
  - The overlap with Saez–Veall agrees: 47.87% vs 47.86% in 2000.
- **Gap:** I found no OECD value for 2026. The federal top rate is unchanged at 33%, but confirm Ontario.

### Capital gains and estate taxes [Academic]
Vaillancourt & Kerkhoff (2019), "Capital gains taxation in Canada, 1972-2017: evolution in a federal setting", *eJournal of Tax Research* 16(2): 340–361. Raw file: `raw/tax/vk2019.pdf`. https://www.unsw.edu.au/business//sites/default/files/documents/Capital%20gains%20taxation%20in%20Canada%2C%201972-2017%20evolution%20in%20a.pdf
- "Prior to the publication of the report [Carter], capital gains in Canada were tax-free."
- Inclusion rates: 50% (1972–87), 66⅔% (1988–89), 75% (1990–Feb 2000), then 66⅔%, then 50% from October 2000.
- Federal death taxes were first imposed in 1941. "The abolition of death taxes and the introduction of capital gains taxation was carried out simultaneously at the federal level in 1972." Provinces abolished their succession duties in the 1970s, except Quebec in 1985.
- Federal succession-duty revenue was $111M in 1970, $6M in 1975 and $1M in 1980 (their Table 3, from StatCan 36-10-0178-01).
- Not covered by this paper: the proposed 2024 increase of the inclusion rate to ⅔ was cancelled in March 2025. Check this against Finance Canada if you use it.

### Top 1% income share
- 1920–2000 [Academic]: Saez–Veall Table B1, file `top_income_shares_canada_saez_veall_1920_2000.csv`. Total income excluding capital gains; tax-unit / adult-population denominators.

| Year | Top 1% share |
|---|---|
| 1920 | 14.4% |
| 1938 | 18.4% |
| 1945 | 10.1% |
| 1955 | 10.2% |
| 1965 | 9.2% |
| 1975 | 8.7% |
| 1980 | 8.1% (low) |
| 1990 | 9.3% |
| 2000 | 13.6% |

  - Top 0.1%: 5.4% (1920), 2.0% (1980), 5.2% (2000).
- 1982–2023 [StatCan]: table 11-10-0055-01 "High income tax filers in Canada", file `top_income_shares_canada_t11100055.csv`. Its denominator is all tax filers, so its levels are lower than Saez–Veall's.
  - Top 1% share of total income excluding capital gains (v62802587): 7.1% (1982), 8.1% (1990), 11.2% (2000), **12.1% (2006, peak)**, 11.3% (2015), 10.1% (2019), **10.1% (2023)**.
  - Including capital gains (v62837237): 7.4% (1982), 13.3% (2000), 13.6% (2006), 13.4% (2021), 12.4% (2023).
  - Top 1% threshold in 2023: $293,800 excluding capital gains; $328,500 including them.
- Do not splice the two series without a break marker.

## 7. "Golden age" growth
File: `real_gdp_per_capita_canada_long_run.csv`.

Average annual growth in real GDP (or GDI / disposable income) per capita:

| Source | Golden age | Since 1980 | Other periods |
|---|---|---|---|
| Maddison Project Database 2023 (Bolt & van Zanden), 2011$ PPP [Academic] | 1946–1975: **2.53%**; 1950–1975: 2.74% | 1980–2022: **1.36%** | 2000–2022: 0.95% |
| Maddison, US for comparison | 1946–1975: 1.95% | 1980–2022: 1.63% | |
| StatCan real GDP per capita: table 36-10-0104-01 (chained 2017 $, v62305752) ÷ population, table 17-10-0009-01 | 1961–1975: **3.20%** | 1980–2025: **1.11%** | 2000–2025: 0.73%; 2015–2025: 0.43% |
| StatCan long-run table 36-10-0229-01, real GDI per capita | 1950–1975: 3.21% | 1980–2016: 1.28% | |
| Same table, real household disposable income per capita | 1946–1975: 2.72% | 1980–2016: 1.12% | |

- Maddison source: https://dataverse.nl/api/access/datafile/421302. Citation required: Bolt, J. & van Zanden, J.L. (2024), "Maddison style estimates of the evolution of the world economy: A new 2023 update", *Journal of Economic Surveys*.
- StatCan real GDP per capita levels: $20,669 (1961) → $60,189 (2025).

---

## Caveats and to-dos before publishing
1. The Ontario, BC and Calgary living-wage figures and the CMHC rent figures came from media reports. The primary sites (ontariolivingwage.ca, livingwagebc.ca, cmhc-schl.gc.ca) returned 403 to automated fetch. Confirm them by hand.
2. Pre-1971 census ratio: there is no consistent dwelling-value / median-income pair before 2016 in this dataset. Use the OECD index for the long run.
3. The productivity-pay results depend heavily on the deflator. Deflating compensation with the GDP deflator instead of the CPI would show a smaller gap (the terms-of-trade effect). The CSLS papers decompose this.
4. The "salary needed to live comfortably" figure in the US post (SmartAsset) has no Canadian institutional equivalent. Use living wage + MBM instead.
5. I found no official long-run series for the age of first-time buyers. CHSP (2021–23) and CHS (2018–24) are the only official sources.
6. The very large StatCan CSVs (46100062.csv, about 10 GB, and 46100099.csv, about 8 GB) were deleted after extraction to save disk space. Their zips are kept in raw/, and raw/46100099/sub_tot.csv holds the filtered rows.

## File list
| File | Contents |
|---|---|
| oecd_house_price_to_income_can_usa_quarterly.csv / _annual.csv | OECD price-to-income, price-to-rent, real HPI, CAN & USA, 1970–2026 |
| census_dwelling_value_to_income_snapshots.csv | Census dwelling values 1961/1971/2011/2016/2021 and ratios 2016/2021 |
| first_time_buyers_chsp_46100099.csv | CHSP buyer age, price-to-income, income, price by FTHB status (5 jurisdictions, 2021–23) |
| housing_need_and_tenure_chs.csv | CHS core housing need by tenure; ownership rate by age; FTHB age distribution 2018–2024 |
| homeownership_rate_canada_census.csv | Census homeownership 1941–2021; by age 2011/16/21 |
| cpi_components_canada_annual.csv | CPI all-items, shelter, rent, food, etc. (2002=100) |
| household_debt_canada_quarterly.csv | Debt-to-disposable income, debt service ratios 1990-Q1–2026-Q2 |
| household_debt_oecd_can_usa_annual.csv | OECD household debt % NDI, CAN vs USA |
| income_canada_cis_annual.csv | CIS medians 1976–2024 (families, unattached persons, individuals), 2024 $ |
| share_income_over_100k_t11100008.csv | Share with income ≥$100k/150k/200k, 2000–2023 |
| poverty_rate_canada_cis.csv | MBM (2018 and 2023 base) and LIM poverty rates by age |
| mbm_thresholds_cities_current_dollars.csv | MBM thresholds by component, major cities, 2015–2025 |
| living_wage_vs_minimum_wage_cities.csv | Living wage vs minimum wage, 5 cities |
| food_bank_hungercount_2025.csv | HungerCount 2025 headline numbers |
| productivity_pay_canada_annual.csv | Wide-format productivity / compensation / LFS wages / labour share |
| productivity_pay_canada_1946_2001_archived.csv | 1946–2001 productivity vs real compensation per hour |
| productivity_pay_index_long.csv | Long-format chart-ready panels A–E |
| top_marginal_tax_rate_canada_vs_us_1920_2025.csv | Spliced top rate (Saez–Veall to 1999, OECD 2000–2025) plus US 2000–2025 |
| top_marginal_tax_rates_canada_saez_veall_1920_2000.csv | Saez–Veall MTRs at P90/P99/P99.9/P99.99/top |
| top_income_shares_canada_saez_veall_1920_2000.csv | Saez–Veall top 10/5/1/0.1/0.01% shares |
| top_income_shares_canada_t11100055.csv | StatCan top income shares, thresholds, averages 1982–2023 |
| real_gdp_per_capita_canada_long_run.csv | StatCan real GDP pc 1961–2025, real GDI / RHDI pc 1926–2016, Maddison CAN/USA |
