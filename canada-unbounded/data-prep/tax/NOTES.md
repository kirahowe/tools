# Who pays tax in Canada: research notes

Retrieved 2026-10-10. All raw downloads are in `raw/` (text conversions in `raw/txt/`). Scripts are in `../tax_scripts/`, outside `raw/`. Every figure below comes from a file saved in `raw/` unless it is marked **[web-only]**, meaning it was read through a search or fetch tool and no copy could be saved, or **[UNVERIFIED]**.

---

## 0. Key finding about the "tax by wealth group" chart (read first)

**Statistics Canada's DHEA tables do NOT report taxes by wealth quintile.** I checked every DHEA table, downloaded in full: 36-10-0587, -0588, -0660, -0662, -0663, -0664, -0665, -0667 and -0101.
- The income, consumption and saving tables (36-10-0587-01 annual, 36-10-0662-01 quarterly) break households down by **income** quintile, age, household type, main income source, tenure and generation. They have **no wealth-quintile dimension**.
- These tables also have **no separate "current taxes on income, wealth" line**. Taxes appear only inside **"Less: current transfers paid"**, which bundles:
  - personal income tax;
  - social insurance contributions;
  - other transfers to government;
  - transfers to corporations, to non-profit institutions serving households (NPISH) and to non-residents.
- **Wealth quintiles exist only in the wealth tables** (36-10-0660-01 and 36-10-0664-01). Note 16 in those tables: "Wealth quintiles are assigned based on the household net worth."
- DHEA has **no top 1% or top-wealth breakdown**.

**Consequence.** A Canadian version of the IRS "tax burden by wealth group" chart cannot be built from published DHEA tables. The closest analogue that can be built is **"current transfers paid" (mostly income tax plus payroll contributions) as a % of net worth, by INCOME quintile**. That is in `tax_by_income_quintile_dhea.csv` (§2). Wealth shares by **wealth** quintile are in `wealth_by_wealth_quintile_dhea.csv`. A true wealth-group tax burden would need microdata: the SFS linked to T1 tax data, or the PBO High-net-worth Family Database. Neither is published with taxes.

---

## 1. Federal revenue by source

### 1a. FY2024-25 (final, Public Accounts basis)
- **Source:** Annual Financial Report of the Government of Canada, Fiscal Year 2024-2025, Table 4 "Revenues" and Chart 2.
- **URL:** https://www.canada.ca/en/department-finance/services/publications/annual-financial-report/2025.html
- **Raw file:** `raw/finance/afr_2024-25.html`

| Source | 2024-25 ($M) | Share | 2023-24 ($M) |
|---|---:|---:|---:|
| Personal income tax | 234,319 | 45.9% | 217,696 |
| Corporate income tax | 96,954 | 19.0% | 82,468 |
| Non-resident income tax | 13,528 | 2.6% | 12,541 |
| GST | 52,503 | 10.3% | 51,416 |
| Energy taxes | 5,650 | 1.1% | 5,599 |
| Customs import duties | 6,264 | 1.2% | 5,571 |
| Other excise taxes and duties | 7,487 | 1.5% | 6,829 |
| EI premiums | 31,530 | 6.2% | 29,560 |
| Pollution pricing proceeds | 13,552 | 2.7% | 10,503 |
| Other revenues | 49,164 | 9.6% | 37,366 |
| **Total** | **510,951** | 100% | 459,549 |

Context from the AFR text:
- Total expenses were $547.3B and the deficit was $36.3B.
- Federal revenues were 16.6% of GDP.
- The AFR attributes the 17.6% jump in corporate income tax partly to taxpayers reacting to the Budget 2024 proposal to raise the capital gains inclusion rate, by realizing gains early ("pull-forward effect").
- The AFR states that the **CPP is excluded from the reporting entity** because changes to the CPP require the agreement of two-thirds of participating provinces. So CPP contributions are **not** federal revenue.

### 1b. FY2025-26 (preliminary, not final)
- **Source:** The Fiscal Monitor, March 2026, Table 2, April-to-March column.
- **URL:** https://www.canada.ca/en/department-finance/services/publications/fiscal-monitor/2026/03.html
- **Raw file:** `raw/finance/fm_2026-03.html`

| Source | $M |
|---|---:|
| Personal income tax | 226,769 |
| Corporate income tax | 101,543 |
| Non-resident income tax | 14,684 |
| GST | 50,841 |
| Energy taxes | 5,539 |
| Customs import duties | 10,240 (U.S. tariff countermeasures) |
| Other excise taxes and duties | 7,310 |
| Pollution pricing proceeds | -196 (fuel charge ended 1 April 2025) |
| EI premiums | 32,606 |
| Other revenues | 50,681 |
| **Total** | **500,017** |

Caveats:
- The Fiscal Monitor says these are "not the final results" and that final figures will be in the 2026 Public Accounts.
- The 2025-26 Annual Financial Report was **not yet online** when I checked (`annual-financial-report/2026.html` did not return a page).
- The Fiscal Monitor's own 2024-25 comparison column ($494,811M total) is the earlier, unaudited April-March figure. It is not the final AFR figure.

Tidy file: `federal_revenue_by_source.csv`.

### 1c. CPP (outside federal revenue)
- In FY2023-24, CPP contributions were **$81.6B** and benefits paid were **$60.8B**.
- **Source:** Annual report of the Canada Pension Plan for the fiscal year ending March 31, 2024. https://www.canada.ca/en/employment-social-development/programs/pensions/reports/annual-2024.html (`raw/esdc/cpp_annual_2024.html`)
- The 2025 edition did not download (`annual-2025.html` was unavailable).

### 1d. Capital gains within personal income tax: CRA T1 statistics, tax year 2023
- **Source:** CRA, Individual Income Tax Return Statistics (2023 tax year), Table 2 (all Canada).
- **Page URL:** https://www.canada.ca/en/revenue-agency/programs/about-canada-revenue-agency-cra/income-statistics-gst-hst-statistics/t1-final-statistics/2023-tax-year.html
- **CSV URL:** https://www.canada.ca/content/dam/cra-arc/prog-policy/stats/t1-final-stats/2023-tax-year/tbl2_ac.csv (`raw/cra/tbl2_ac.csv`)

Totals for 2023:
- Taxable capital gains: **$54.99B**, reported by 2.71M filers. Because the inclusion rate was 50%, this implies about $110B of net realized gains.
- Total income assessed: $1,896.4B.
- Taxable Canadian dividends: $111.6B.
- Capital gains deduction (Lifetime Capital Gains Exemption claims): $9.30B, claimed by 60,500 filers.
- Net federal tax: $207.9B.

Filers with income of **$250,000 or more** were 546,650 returns, **1.76%** of all returns. Their shares:

| Item | Share held by $250K+ filers |
|---|---:|
| Taxable capital gains | **70.6%** ($38.84B) |
| Taxable dividends | 48.1% |
| Capital gains deduction claimed | 80.9% |
| Total income assessed | 15.6% |
| Employment income | 11.3% |
| Net federal tax | 29.6% |

Tidy file: `cra_t1_2023_by_income_class.csv`.

**Tax collected on capital gains is not published by CRA.** I have not estimated it.

### 1e. All levels of government, by source

**OECD Revenue Statistics 2025, Canada country note**
- URL: https://www.oecd.org/content/dam/oecd/en/publications/reports/2025/12/revenue-statistics-2025-country-notes_3708be73/canada_3aa043fb/795aaca9-en.pdf (`raw/oecd/revstats2025_canada.pdf`)
- Tax-to-GDP was 34.9% in 2024 (provisional) and 34.8% in 2023. The OECD average was 34.1%. Canada ranked 21st of 38.

2023 tax structure, all levels of government, including CPP/QPP as social security contributions. Total taxes C$1,020,530M.

| Tax | C$M | % of total |
|---|---:|---:|
| Personal income, profits and gains | 366,543 | 35.9 |
| Corporate income and gains | 141,199 | 13.8 |
| Social security contributions | 151,974 | 14.9 |
| Payroll taxes | 22,086 | 2.2 |
| Property taxes | 100,834 | 9.9 |
| Goods and services | 223,644 | 21.9 |
| of which VAT/GST/HST | 141,291 | 13.8 |
| of which excises | 33,955 | 3.3 |
| Other | 1,396 | 0.1 |

Relative to the OECD average, the country note says Canada collects "substantially higher" revenue from personal income tax and "substantially lower" revenue from social security contributions.

**StatCan 10-10-0147-01 (Canadian Government Finance Statistics)**
- Consolidated Canadian general government, 2024, $M (`raw/10100147/`).
- **This series excludes CPP/QPP** (table note 5).

| Item | $M |
|---|---:|
| Revenue | 1,167,761 |
| Taxes | 910,642 |
| Taxes on income, profits and capital gains | 548,943 |
| of which individuals | 382,888 |
| of which corporations | 151,174 |
| of which non-residents | 14,881 |
| Payroll taxes | 20,543 |
| Property taxes | 91,899 |
| of which real property taxes | 78,690 |
| Goods and services | 243,094 |
| of which general sales taxes | 151,216 |
| Social contributions | 46,863 |
| Other revenue | 210,248 |

Vectors: v106982462 to v106982500.

**Labour vs capital split: NOT AVAILABLE from official Canadian sources I could verify.**
- OECD Revenue Statistics 2025 has a special feature splitting personal income tax revenue by income source (employed labour, capital, self-employment, pensions) for 29 countries. I could not confirm whether Canada is included: the chapter page returned 403. **[UNVERIFIED]**
- A proxy, without a formal split: under the OECD 2023 figures, personal income tax plus social security contributions plus payroll taxes make up about 53% of all taxes. Corporate income tax plus property taxes make up about 24%.

---

## 2. DHEA: taxes vs wealth (the central computation)

**Sources**
- 36-10-0587-01 (income, annual, 1999-2025), file dated 2026-04-13. https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610058701; CSV https://www150.statcan.gc.ca/n1/tbl/csv/36100587-eng.zip
- 36-10-0660-01 (wealth, quarterly, to Q4 2025), file dated 2026-04-13. https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610066001; CSV https://www150.statcan.gc.ca/n1/tbl/csv/36100660-eng.zip

**Method** (`../tax_scripts/dhea.py`)
- Numerator: "Less: current transfers paid" (CTP) for the year.
- Denominator: the average of the four quarterly net worth values for the same year.
- Both are by **income** quintile, since the income table has no wealth quintile.

**Composition of household "current transfers paid"**, 2024, $M. Source: 36-10-0112-01 (household current accounts), unadjusted quarters summed. https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610011201

| Component | $M | Share of CTP |
|---|---:|---:|
| Personal income tax | 389,437 | 49.9% |
| Contributions to social insurance plans | 160,417 | 20.6% |
| Other transfers to government | 14,202 | 1.8% |
| To corporations (not taxes) | 181,527 | 23.3% |
| To NPISH | 24,932 | 3.2% |
| To non-residents | 9,950 | 1.3% |
| **Total** | **780,465** | 100% |

- The total matches the DHEA total in 36-10-0587 exactly.
- So about 72% of CTP is income tax plus payroll and social contributions. The remaining ~28% is not tax.
- **DHEA does not publish this split by quintile.**
- For 2025 the two tables come from different vintages: 36-10-0587 gives CTP of 813,513 and 36-10-0112 gives 823,993.

**Results, 2025** (latest year; 2023 and 2024 are also in the CSV)

| Income quintile | Share of net worth | Share of CTP ("taxes") | CTP per household | CTP as % of net worth | CTP as % of (disposable income + CTP) |
|---|---:|---:|---:|---:|---:|
| Lowest | 9.8% | 4.0% | $9,460 | 1.84% | 22.4% |
| Second | 10.1% | 8.4% | $19,797 | 3.71% | 24.1% |
| Third | 14.9% | 15.8% | $37,250 | 4.75% | 29.8% |
| Fourth | 22.1% | 25.0% | $58,995 | 5.05% | 32.9% |
| Highest | 43.1% | 46.9% | $110,636 | 4.87% | 33.6% |
| All households | 100% | 100% | $47,227 | 4.48% | 31.1% |

How to read it:
- By income quintile, CTP relative to wealth rises from about 1.8% to about 5% and then **flattens or dips slightly in the top quintile**. In 2024: fourth quintile 5.07%, top quintile 4.98%.
- Ranking by income mixes wealthy retirees, who have low income and high wealth, into the low quintiles.

Wealth by **wealth** quintile, 2025, annual average (36-10-0660-01):

| Wealth quintile | Share of net worth | Average net worth per household |
|---|---:|---:|
| Lowest | -0.08% | -$4,012 |
| Second | 3.17% | $166,903 |
| Third | 9.86% | $520,045 |
| Fourth | 21.61% | $1,139,516 |
| Highest | 65.44% | $3,450,236 |

- At Q4 2025 the published distribution for the highest wealth quintile is 65.7%.
- The DHEA total household net worth averaged over 2025 is $18.17 trillion; at Q4 2025 it is $18.59 trillion.

**Files**
- `tax_by_income_quintile_dhea.csv`. Columns:
  - `year`, `group`
  - `n_quarters_wealth`
  - `net_worth_avg_millions`: average of 4 quarters
  - `net_worth_q4_millions`
  - `wealth_share_pct`
  - `net_worth_per_hh_avg`
  - `current_transfers_paid_millions`, `ctp_share_pct`, `ctp_per_hh`
  - `disposable_income_millions`
  - `ctp_pct_of_net_worth`
  - `ctp_pct_of_income_before_transfers_paid`: CTP / (disposable income + CTP)
  - `ctp_pct_of_primary_income`: CTP / (disposable income + CTP − current transfers received)
- `wealth_by_wealth_quintile_dhea.csv`. Columns:
  - `year`, `group`
  - `net_worth_avg_millions`, `wealth_share_pct`, `net_worth_per_hh_avg`
  - `net_worth_q4_millions`
  - `q4_share_published_pct`: StatCan's published "Distribution of value"

**Caveat: what DHEA misses.** DHEA covers only taxes households pay directly. It excludes:
- corporate income tax on retained earnings, which matters for the wealthy;
- GST and excise taxes, which sit in consumption prices;
- the full incidence of property taxes.

So it **understates the tax burden on consumption-heavy low-wealth households** and **misses corporate-level tax on top wealth**.

---

## 3. Top incomes, effective tax rates and top wealth

### 3a. StatCan 11-10-0055-01: high-income tax filers (LAD, a 20% sample of filers)
- Release dated 2025-10-31; latest year 2023.
- https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1110005501; CSV https://www150.statcan.gc.ca/n1/tbl/csv/11100055-eng.zip
- Extract: `high_income_filers_11100055_extract.csv`. Summary: `top_income_groups_tax_share_2023.csv`.
- In the table below, "tax" means federal plus provincial personal income tax. The tax/income column is average tax divided by average income.

Canada, 2023:

| Group | Concept | Threshold | Share of income | Share of income tax | Avg tax / avg income |
|---|---|---:|---:|---:|---:|
| Top 1% | Total income (excluding capital gains) | $293,800 | 10.1% | 22.1% | 38.9% |
| Top 1% | Total income including capital gains | $328,500 | 12.4% | 22.8% | **31.3%** |
| Top 0.1% | Including capital gains | $1,256,400 | 4.7% | 9.0% | 32.1% |
| Top 0.01% | Including capital gains | $5,097,400 | 1.7% | 3.2% | 31.6% |
| Bottom 50% | Including capital gains | | 17.3% | 4.3% | 4.3% |
| All filers | Including capital gains | | | | 17.0% |

- When capital gains are counted as income, the average rate is almost flat across the top: 31.3% for the top 1%, 32.1% for the top 0.1% and 31.6% for the top 0.01%. When capital gains are excluded, the same groups pay 38.9%, 42.6% and 41.4%.
- These are **personal** taxes only. They exclude corporate tax and income retained inside corporations.

### 3b. Budget 2024, Chapter 8 ("Fairness for every generation")
- https://budget.canada.ca/2024/report-rapport/chap8-en.html (`raw/finance/budget2024_chap8-en.html`)
- "In 2021, the top 1 per cent earned 10.4 per cent of all income in Canada; when capital gains are factored in, this jumps to 13.4 per cent."
- About 5% of Canadians under 30 had any capital gains in 2021.

Chart 8.2: capital gains as a share of gross income (Finance Canada T1 model, 2025 projection).

| Group | Capital gains share of gross income |
|---|---:|
| Bottom 50% | 0.7% |
| p50 to p90 | 1.2% |
| p99 to p99.5 | 10.7% |
| p99.9 to p99.99 | 29.3% |
| **Top 0.01%** | **39.9%** (plus 17.9% from dividends) |

- Table 8.1: about 40,000 people, 0.13% of filers, were projected to have capital gains above $250K in 2025, with average gross income of $1,411,000.
- In Ontario, a $70K nurse faced a 29.7% marginal rate, versus 26.8% on capital gains for someone with $1M income.

### 3c. PBO, "Estimating the top tail of the family wealth distribution in Canada: 2025 update" (RP-2526-009-S, 11 Sept 2025)
- https://www.pbo-dpb.ca/en/publications/RP-2526-009-S--estimating-top-tail-family-wealth-distribution-in-canada-2025-update--estimation-extremite-superieure-distribution-patrimoine-familial-canada-mises-jour-2025
- PDF: `raw/pbo/RP-2526-009-S_top_tail_wealth_2025.pdf`

Shares of net wealth, 2023:

| Group | PBO HFD estimate | 2023 SFS survey |
|---|---:|---:|
| Top 1% | **23.8%** | 11.0% |
| Top 10% | 53% | |
| Top 20% | 69.2% | 63.5% |
| Middle 40% | 27.6% | 32.6% |
| Bottom 40% | 3.3% | 3.9% |

- The top 1% threshold is $7.4M, covering about 169,400 families.
- The PBO says survey data considerably understate wealth at the top.
- **The PBO has not published effective tax rates by wealth group.** I found none.

### 3d. Other PBO costings
- **Wealth tax (Election Proposal Costing, 9 Apr 2025):** 1% on net worth over $10M, 2% over $50M, 3% over $100M. Estimated revenue **$121.5B over 2025-26 to 2029-30** ($22.8B in the first year). Assumes 35% of net worth is hidden in behavioural response.
  - PDF: https://distribution-a617274656661637473.pbo-dpb.ca/cc1b029d8d36c1340f715cb7666bfd2132ef6683608cecae42a5e71b82bb6ad6 (`raw/pbo/EPC_2025-04-09_wealth_tax.pdf`)
- **Budget 2024 capital gains inclusion rate increase (LEG-2425-010-S, 1 Aug 2024):** +$17.4B over 2024-25 to 2028-29 ($5.8B personal income tax, $11.7B corporate income tax).
  - https://www.pbo-dpb.ca/en/publications/LEG-2425-010-S--increasing-capital-gains-inclusion-rate--augmentation-taux-inclusion-gains-capital (`raw/pbo/LEG-2425-010-S_capgains.pdf`)
- **[web-only]** Earlier PBO wealth-tax costings exist (2021 M-68 one-time tax; RP-2021-017-M). I did not download them.

### 3e. Academic and older StatCan work
- **Wolfson, Veall, Brooks & Murphy (2016)**, "Piercing the Veil: Private Corporations and the Income of the Affluent," *Canadian Tax Journal* 64(1): 1-30. https://ideas.repec.org/a/ctf/journl/v64y2016i1p1-30.html (`raw/academic/ideas_wolfson2016.html`)
  - Abstract, verified: "top income shares are significantly higher when CCPC incomes are included."
  - **Specific numeric results are [UNVERIFIED].** The full text at ctf.ca was not retrieved. Do not quote specific numbers without checking the paper.
- **Finance Canada (July 2017), "Tax Planning Using Private Corporations"** consultation paper. https://www.canada.ca/content/dam/fin/migration/activty/consult/tppc-pfsp-eng.pdf (`raw/finance/tppc-pfsp-eng_2017.pdf`)
  - Taxable passive investment income of private corporations grew from $8.6B (2002) to $26.8B (2015).
  - Income sprinkling measures were estimated to raise about $250M a year.
  - Men initiate more than 70% of income-sprinkling strategies; women are 68% of recipients of sprinkled dividends.
- **[web-only]** Murphy, Roberts & Wolfson (2007), "A Profile of High Income Canadians, 1982 to 2004," StatCan 75F0002M2007006. https://www150.statcan.gc.ca/n1/pub/75f0002m/75f0002m2007006-eng.htm
  - Reported, via search: the effective tax rate of high-income filers fell from 29% to 27% between 1992 and 2004. Re-check before use.

---

## 4. Tax features

### 4a. Top combined marginal rates, 2026
- **Sources:**
  - Federal and provincial brackets: CRA "Current year tax rates and income brackets (2026)", https://www.canada.ca/en/revenue-agency/services/tax/individuals/tax-rates-brackets/current-year.html (`raw/cra/rates_current_year.html`)
  - Ontario surtax (20% and 36% of basic Ontario tax over $5,818 / $7,446): CRA T4127, January 2026.
  - Quebec: 25.75% over $132,245, from Revenu Québec **[web-only; page returned 403]**. Federal tax is reduced by the 16.5% Quebec abatement.
- **Federal brackets 2026:** 14% / 20.5% / 26% / 29% / **33% over $258,482**.
- File: `top_marginal_rates_2026.csv`. My computation; the computed Ontario rate matches the 53.53% Finance used in 2017.

Combined top rates on ordinary income:

| Province or territory | Rate |
|---|---:|
| NL (over $1.14M) | 54.8% |
| NS | 54.0% |
| ON | 53.53% |
| BC | 53.5% |
| QC | 53.31% |
| PE | 53.0% |
| NB | 52.5% |
| MB | 50.4% |
| AB | 48.0% |
| YT | 48.0% |
| SK | 47.5% |
| NT | 47.05% |
| NU | 44.5% |

The top rate on capital gains is half of each figure because of the 50% inclusion rate. For example, Ontario is 26.77%.

### 4b. Capital gains inclusion rate history
Source: Report on Federal Tax Expenditures 2026, Part 3, "Partial inclusion of capital gains", p.244.
- None before 1972.
- 1/2 from 1972, introduced in Budget 1971.
- 2/3 for 1988-89.
- 3/4 from 1990.
- 2/3 from 28 Feb 2000.
- **1/2 from 18 Oct 2000.**

Budget 2024 proposal and its cancellation:
- Budget 2024 proposed raising the rate to 2/3 on individuals' gains above $250K a year and on all gains of corporations and trusts, effective 25 June 2024.
- It was deferred to 1 Jan 2026 (Finance, 31 Jan 2025, **[web-only]**).
- It was **cancelled by PM Carney on 21 March 2025**: https://www.pm.gc.ca/en/news/news-releases/2025/03/21/prime-minister-mark-carney-cancels-proposed-capital-gains-tax-increase (`raw/finance/pmo_2025-03-21_capgains.html`).
- Budget 2025 confirmed the cancellation (2026 Tax Expenditures report, p.44-45).

### 4c. Lifetime Capital Gains Exemption
- 2023: $971,190.
- 2024 before 25 June: $1,016,813 (Budget 2024 text says $1,016,836).
- From 25 June 2024 and for 2025: $1,250,000.
- **2026: $1,275,000**, as indexation resumed. Source: CRA indexation table, https://www.canada.ca/en/revenue-agency/services/tax/individuals/frequently-asked-questions-individuals/adjustment-personal-income-tax-benefit-amounts.html (`raw/cra/indexation.html`).
- Earlier history (from the 2026 Tax Expenditures report): $500K in 1985, $750K in 2007, $800K in 2014, then indexed.
- **Data error to avoid:** CRA's line 25400 page prints the 2023 limit as "$917,190" alongside a deduction of $485,595. That is a typo for $971,190.

### 4d. Dividend tax credit (2025)
Source: 2026 Tax Expenditures report, Part 3.
- Eligible dividends: gross-up 38%, federal credit 15.0198%.
- Non-eligible dividends: gross-up 15%, federal credit 9.0301%.

### 4e. Principal residence exemption
- Exempts gains on a designated principal residence. One property per family since 1982.
- Dispositions must be reported since 2016.
- About 314,000 individuals claimed it in 2023.

### 4f. No estate or inheritance tax: deemed disposition at death
- At death, capital property is deemed sold at fair market value immediately before death.
- Tax is deferred if the property goes to a spouse or spousal trust (rollover); the gain is taxed when the spouse later sells or dies.
- CRA: https://www.canada.ca/en/revenue-agency/services/tax/individuals/life-events/doing-taxes-someone-died/prepare-returns/report-income/capital-gains.html (`raw/cra/deceased_capital_gains.html`)
- Unlike the US, there is **no step-up in basis that erases the gain**. "Buy, borrow, die" in Canada therefore defers tax until death, or until the second spouse dies, rather than eliminating it.
- Borrowing against assets is not a disposition. Interest on money borrowed to earn investment income is deductible: a $3.10B personal income tax expenditure in 2024, per the 2026 Tax Expenditures report.
- **No official Canadian estimate of "buy-borrow-die" use was found.** Provincial probate fees exist but were not researched.

### 4g. Tax expenditures
- **Source:** Report on Federal Tax Expenditures 2026. https://www.canada.ca/content/dam/fin/publications/taxexp-depfisc/2026/taxexp-depfisc-26-eng.pdf (`raw/finance/taxexp-depfisc-26-eng.pdf`)
- All figures are **federal only**, in $ millions. "(P)" marks projections.

| Measure | 2023 | 2024 (P) | 2025 (P) |
|---|---:|---:|---:|
| Partial inclusion of capital gains, total | 29,485 | 41,740 | 34,165 |
| of which individuals' personal income tax | 13,315 | 15,780 | 16,175 |
| of which trusts | 1,205 | 1,400 | 1,440 |
| of which corporate income tax | 14,965 | 24,560 | 16,550 |
| Principal residence exemption | 7,565 | 7,460 | 8,170 |
| Lifetime Capital Gains Exemption | 2,585 | 2,700 | 2,770 |
| Dividend gross-up and credit, personal income tax | 6,380 | 6,710 | 6,975 |
| TFSA | 2,665 | 3,675 | 4,050 |

- Partial inclusion of capital gains in 2021 alone was $40,580M.
- Principal residence exemption peaked at $12,175M in 2021.
- About 2.7M individuals and 292,000 corporations reported capital gains in 2023.
- The report warns that these costs ignore behavioural responses, so they are not the revenue that repeal would raise.
- File: `tax_expenditures_2026_report.csv`.

### 4h. Private corporations and income sprinkling (TOSI)
- Since 2018, the tax on split income (TOSI) applies the top rate to "split income" of family members who are not actively engaged in the business, with exclusions for owners of 10% or more who are 25 or older. **[web-only]** Finance backgrounder, Dec 2017: https://www.canada.ca/en/department-finance/news/2017/12/backgrounder_on_simplifiedmeasurestoaddressincomesprinkling.html
- Statistics from the 2017 Finance paper are in §3e.

---

## 5. Who benefits from capital gains (summary)
- **CRA 2023:** filers with income of $250K or more (1.76% of returns) reported **70.6% of taxable capital gains** and 80.9% of capital gains deduction claims. See §1d.
- **StatCan 11-10-0055, 2023:** the top 1%'s income share rises from 10.1% to 12.4% when capital gains are included. Their income tax as a share of income falls from 38.9% to 31.3%.
- **Finance (Budget 2024):** in 2021 the top 1% share rose from 10.4% to 13.4% with capital gains. For the top 0.01%, capital gains were 39.9% of gross income (2025 projection).

---

## Data files produced
- `tax_by_income_quintile_dhea.csv`: DHEA taxes (current transfers paid) vs net worth by income quintile, 2023-2025.
- `wealth_by_wealth_quintile_dhea.csv`: DHEA wealth by wealth quintile, 2024-2025.
- `federal_revenue_by_source.csv`: AFR 2024-25 final, 2023-24, and Fiscal Monitor 2025-26 preliminary.
- `cra_t1_2023_by_income_class.csv`: CRA T1 2023 by income class (capital gains, dividends, deduction, net federal tax).
- `high_income_filers_11100055_extract.csv`: raw extract, Canada 2019-2023.
- `top_income_groups_tax_share_2023.csv`: shares and average tax/income, 2021-2023.
- `tax_expenditures_2026_report.csv`
- `top_marginal_rates_2026.csv`
- Raw files in `raw/`:
  - StatCan zips and CSVs: 36100587, 36100588, 36100660, 36100662-65, 36100667, 36100101, 36100112, 10100147, 11100055 (the 11100055 CSV is 790 MB)
  - `finance/`, `cra/`, `oecd/`, `pbo/`, `esdc/`, `academic/`
  - Text conversions in `txt/`

## Not verified or not found
- Taxes by wealth quintile or top wealth group: not published by StatCan, PBO or Finance.
- Tax revenue from capital gains: not published.
- Whether Canada is in the OECD 2025 personal income tax by income-source feature.
- Numeric results from the Wolfson et al. (2016) paper.
- The 2025-26 Annual Financial Report (not yet online) and the CPP 2024-25 annual report.
