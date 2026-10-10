# Wealth concentration in Canada: research notes

Retrieved: 2026-10-10 (all downloads in `raw/`; scripts in `scripts/`; tidy outputs in `data/`).
Every number below comes from the cited table or document. Rows marked **computed** are my own arithmetic on those sources, and the method is stated for each.

---

## 0. Which source to use for what (read first)

| Question | Best source | Unit | Notes |
|---|---|---|---|
| Quarterly quintile shares, 2010 onward | StatCan DHEA 36-10-0660-01 | households | Benchmarked to the national balance sheet (NBSA). **Quintiles only**: no top 1%/10%. |
| Detailed survey distribution (deciles, age, province) | StatCan SFS 2023/2019 | economic families + unattached individuals | The survey misses the very top. StatCan: "measures like the share of wealth held by the top 1% will be understated". |
| Top tail (top 1%, 0.1%, 0.01%, thresholds, counts) | PBO High-net-worth Family Database (HFD) | economic families | SFS + Pareto interpolation + Forbes/Maclean's, scaled to NBSA. The closest Canadian analogue to the stitched curve in the original series. |
| Long run (1892 onward) | Davies & Di Matteo; WID.world | families / adults | WID's Canada wealth series is low quality (see section 4). |
| International comparison | OECD WDD; WID | households / adults | Survey-based (OECD) vs. imputed (WID). |

**DHEA vs SFS vs PBO.** DHEA allocates the macro household-sector balance sheet (NBSA) across households using the SFS and other inputs, so its totals match the national accounts. SFS is the raw survey, valued in 2023 constant dollars, and is designed to be reliable only up to about the top 5%. PBO's HFD adds a synthetic Pareto top tail anchored on rich lists, then scales to NBSA totals. For the top 20% share, DHEA gives 65.7% (Q4 2025), SFS 63.5% (2023) and PBO 69.2% (2023). An experimental StatCan capitalization-of-income estimate puts the top 20% at 81.2% (2019; see 3d).

---

## 1. StatCan DHEA wealth, table 36-10-0660-01 (quarterly)

- Table: "Distributions of household economic accounts, wealth, by characteristic, Canada, quarterly". https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610066001 ; CSV https://www150.statcan.gc.ca/n1/tbl/csv/36100660-eng.zip (file dated 2026-04-13).
- **Wealth groups available:** wealth quintiles only (Lowest, Second, Third, Fourth, Highest). There is **no top 1%, top 10% or decile** breakdown. Other characteristics: income quintiles, age of the major income earner, generation, tenure, and main income source. Regional versions are in 36-10-0661-01; ratios are in 36-10-0664-01 and 36-10-0665-01.
- **Coverage:** Q4 only for 2010 to 2019, then every quarter from 2020Q1 to **2025Q4, the latest**. The Q4 2025 Daily (2026-04-13) says: "The Distributions of Household Economic Accounts will be returning to an annual release, with data for the 2026 reference year to be released on April 13, 2027." No newer DHEA quarter exists as of 2026-10-10.
- Wealth series revised back to 2010 in the April 2026 release.
- Output: `data/dhea_networth_by_wealth_quintile_quarterly.csv`. It includes the total ($ millions), published share (1 decimal), share computed from totals, and the per-household average. Derived rows (flag `derived=1`): Bottom 40%, Middle 40% and Top 40%.

**Headline, Q4 2025 (36-10-0660-01):**

| Group | Net worth ($M) | Share (published) | Avg per household |
|---|---|---|---|
| All households | 18,594,899 | 100.0 | $1,078,642 |
| Q1 (bottom 20%) | -15,887 | -0.1 | -$4,608 |
| Q2 | 578,916 | 3.1 | $167,907 |
| Q3 | 1,814,200 | 9.8 | $526,185 |
| Q4 | 3,999,991 | 21.5 | $1,160,145 |
| Q5 (top 20%) | 12,217,678 | **65.7** | **$3,543,579** |
| Bottom 40% (computed) | 563,029 | **3.03** | $81,650 (Daily) |

Trend in the top 20% share (Q4 of each year, published): 2010 68.9, 2015 67.7, 2019 68.2, **2020 65.0**, 2021 64.2, 2022 65.0, 2023 64.8, 2024 65.2, 2025 65.7. The bottom 40% (computed) rose from 1.49% (2010) to 3.33% (2021), then 3.03% (2025).

**Daily text, Q4 2025.** "Distributions of household economic accounts for income, consumption, saving and wealth of Canadian households, fourth quarter 2025", released 2026-04-13. https://www150.statcan.gc.ca/n1/daily-quotidien/260413/dq260413a-eng.htm
> "The wealthiest households (top 20% of the wealth distribution) accounted for almost two-thirds (65.7%) of Canada's total net worth at the end of 2025 (i.e., fourth quarter of 2025), averaging $3.5 million per household. Meanwhile, the least wealthy households (bottom 40% of the wealth distribution) accounted for 3.0% of total net worth, averaging $81,650 per household. Overall household net worth increased at the end of 2025 relative to a year earlier (+5.3%), entirely due to a strong gain in financial assets (+9.9%), mainly from equity markets."
>
> "The gap in wealth between households in the top 20% and the bottom 40% reached 62.7 percentage points at the end of 2025, up 0.6 percentage points from a year earlier."
>
> "The least wealthy households grew their net worth at the slowest pace of any wealth group at the end of 2025 (+2.1% vs. +5.3% for all households) ... In contrast, the wealthiest households increased their net worth at the fastest pace at the end of 2025 (+6.0%)..."
>
> Note to readers: "...the average household in the lowest wealth quintile owed more in liabilities than it owned in assets..."

Previous release (Q3 2025, 2026-01-29), https://www150.statcan.gc.ca/n1/daily-quotidien/260129/dq260129b-eng.htm: top 20% held 65.5%, bottom 40% held 3.1% (average $82,100).

The NBSA Q2 2026 Daily (2026-09-11), https://www150.statcan.gc.ca/n1/daily-quotidien/260911/dq260911a-eng.htm, says: "the latest release of the Distributions of household economic accounts highlights that 69.0% of all financial assets and 49.7% of non-financial assets are held by the highest wealth quintile. As a result, market gains are not equally shared among Canadian households."

---

## 2. StatCan Survey of Financial Security (SFS) 2023 and 2019

Release: The Daily, 2024-10-29, https://www150.statcan.gc.ca/n1/daily-quotidien/241029/dq241029a-eng.htm. Associated tables: 11-10-0016-01, 11-10-0049-01, 11-10-0057-01, 11-10-0075-01, 11-10-0078-01, 11-10-0082-01, 11-10-0083-01.
All dollar values are in **2023 constant dollars**. The unit is economic families plus persons not in an economic family ("family units").

> "The SFS is a sample survey and the sample size is insufficient to have a representative sample of economic families with very high wealth. Therefore, measures like 'the share of wealth held by the top 1%' will be understated in this data source."

- **Median net worth, all family units:** $519,700 in 2023 (95% CI 502,100 to 539,800) vs $381,100 in 2019. Mean: $995,900 (2023) vs $852,800 (2019). Source: 11-10-0016-01 / 11-10-0049-01. Mean/median ratio (OECD WDD): 2.04 (2023).
- **Number of family units:** 16,851k (2023).
- **Deciles, table 11-10-0075-01** "Selected assets and debts by net worth deciles" (https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1110007501). Output: `data/sfs_networth_by_decile.csv` (1999, 2005, 2012, 2016, 2019, 2023; regions; mean/median within decile with 95% CIs). Shares are **computed** from decile totals.

| Decile | 2023 median within decile | 2023 share | 2019 share |
|---|---|---|---|
| D1 | $1,000 | -0.12% | -0.18% |
| D2 | $31,800 | 0.34% | 0.17% |
| D3 | $109,500 | 1.14% | 0.70% |
| D4 | $249,400 | 2.50% | 1.88% |
| D5 | $420,000 | 4.23% | 3.52% |
| D6 | $627,000 | 6.33% | 5.69% |
| D7 | $910,000 | 9.16% | 8.60% |
| D8 | $1,270,700 | 12.93% | 12.43% |
| D9 | $1,917,000 | 19.53% | 19.09% |
| D10 | $3,435,000 (mean $4,376,700) | **43.95%** | 48.11% |

  The bottom 50% share (computed) is 8.1% (2023) vs 6.1% (2019).
- **Percentile cutoffs (P10, P25, P90, P99 etc.) are not published** in any public SFS table. The within-decile medians above approximate P5, P15, ..., P95. PBO's thresholds (section 3) are the best published top cutoffs.
- **Quintiles, table 11-10-0049-01** (https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1110004901). Output: `data/sfs_networth_by_quintile_canada_provinces.csv`. 2023 shares (computed): 0.23 / 3.64 / 10.56 / 22.09 / **63.48**; 2019: -0.01 / 2.58 / 9.22 / 21.03 / 67.19. Average net worth in the 2023 top quintile is $3,160,700 (median $2,488,700); in the bottom quintile, $11,200.
- **Zero or negative net worth:** no public SFS table gives the share of families with net worth at or below zero. Indirect evidence: the median of the lowest decile is +$1,000 in 2023 (CI 600 to 1,800) and -$1,200 in 2019 (CI -3,500 to 0). That implies roughly 5% of family units had net worth near or below zero; it is an inference, not a published figure. The lowest decile's average is -$11,700 (2023). The DHEA lowest quintile averages -$4,608 (Q4 2025).
- **Median by age of the major income earner, 2023 (2019 in brackets), 11-10-0016-01:** under 35: $159,100 ($56,400); 35 to 44: $409,300 ($270,800); 45 to 54: $675,800 ($602,000); 55 to 64: $873,400 ($797,000); 65+: $738,900 ($627,500).
- **Median by province, 2023 (11-10-0016-01):** BC $773,500; ON $665,600; AB $457,100; PEI $399,800; SK $394,600; MB $386,300; QC $371,000; NS $354,600; NL $333,500; NB $286,200. CMAs: Vancouver $1,008,200; Toronto $743,000. Output: `data/sfs_networth_by_age_familytype_geo.csv`.
- Daily quote: "those who owned their principal residence and had an employer-sponsored pension plan had a median net worth of $1.4 million in 2023. On the other hand, those who rented and who did not have an employer-sponsored pension plan had a median net worth of $11,900." This refers to families with a major income earner aged 55 to 64.

---

## 3. PBO top tail (High-net-worth Family Database, HFD)

### 3a. 2025 update: "Estimating the top tail of the family wealth distribution in Canada: 2025 update" (RP-2526-009-S), 2025-09-11
Page: https://www.pbo-dpb.ca/en/publications/RP-2526-009-S--estimating-top-tail-family-wealth-distribution-in-canada-2025-update--estimation-extremite-superieure-distribution-patrimoine-familial-canada-mises-jour-2025
PDF: https://distribution-a617274656661637473.pbo-dpb.ca/94e59eedd1df221279b80e2f1378103b3d8f81a9388b667f54db173d1d5321a8 (`raw/pbo/pbo2025.pdf`).
Below, "p." means the printed page number and "PDF p." the PDF page.

Method: full SFS 2023 microdata (accessed at a Research Data Centre) plus the Forbes real-time list (snapshots 2023-06-26 and 2024-12-26) plus Maclean's 2024 rich list. Pareto interpolation is used, then wealth is aligned to NBSA household net worth. Canadians living abroad are excluded from the rich lists (note 5).

- Highlights (p.1, PDF p.4): "the top one per cent of economic families in Canada hold 24 per cent of the country's total net wealth, the top 10 per cent hold 53 per cent ... top 20 per cent hold 69 per cent ... The middle 40 per cent own around 27.6 per cent ... bottom 40 per cent own around 3.3 per cent" and "approximately 169,400 families in the top one per cent in 2023, and they each had a net wealth of at least $7.4 million. PBO projects these numbers to have grown to 176,800 families with a minimum net wealth of $7.5 million in the last quarter of 2024."
- **Table 2 (p.8, PDF p.11), HFD 2023:**

| Group | Threshold ($M) | Families (k) | Total ($B) | Share |
|---|---|---|---|---|
| Top 0.01% | 171.5 | 1.7 | 873 | 5.3 |
| Top 0.1% | 35.8 | 16.9 | 1,856 | 11.4 |
| Top 0.5% | 11.9 | 84.8 | 3,119 | 19.1 |
| Top 1% | 7.4 | 169.4 | 3,893 | 23.8 |
| Top 5% | 2.8 | 842.9 | 6,584 | 40.3 |
| Top 10% | 2.0 | 1,685.3 | 8,601 | 52.7 |
| Top 20% | 1.3 | 3,372.2 | 11,297 | 69.2 |
| Middle 40% | 0.3 to 1.3 | 6,739.0 | 4,502 | 27.6 |
| Bottom 40% | under 0.3 | 6,740.8 | 542 | 3.3 |

  Average wealth (**computed** = total/families): top 1% about $23.0M; top 0.1% about $110M; top 0.01% about $514M.
- Table 1 (p.7, PDF p.10): SFS 2023 alone gives top 0.1% 2.3, top 0.5% 6.9, **top 1% 11.0**, top 5% 29.3, top 10% 44.0, top 20% 63.5, middle 40% 32.6, bottom 40% 3.9.
- Table 3 (p.9, PDF p.12): 4,439.8k families had more than $1M (76.5% of wealth); 108.0k had more than $10M (20.6%); about 100 had more than $1B ($360B, 2.2%).
- **Table 4 (p.10, PDF p.13), projected to Q4 2024:** top 1% 24.1% (threshold $7.5M, 176.8k families, $4,218B); top 0.1% 11.7% ($36.5M); top 0.01% 5.7% ($175.0M); top 10% 52.9%; top 20% 69.2%; bottom 40% 3.3%. This was the baseline for the 2025 election wealth-tax costings.
- Trend in the top 1% share (Figure 1, p.11): SFS only 14.1 / 14.8 / 11.0 (2016/2019/2023); SFS+Forbes 21.9 / 23.4 / 22.0; SFS+Forbes+CB/Maclean's 25.6 / n.a. / 23.8. The top 5% series is in Figure 2. PBO: "the concentration of wealth among high-net-worth families in 2023 is considered to be either comparable to or marginally lower than in 2019."
- Note 10 (PDF p.27): the top 1% share for 2019 was **revised from 24.8% to 23.4%** because of the switch to full SFS data and a simplified interpolation.
- **Public HFD microfiles**, which can be used for a curve:
  - HFD 2023: https://pbo-dpb.s3.ca-central-1.amazonaws.com/files--fichiers/855740f452ff33c7fb1572daeed1073da246f71e8fee08344b859c9c5c8af711 (`raw/pbo/hfd_2023_public.xlsx`)
  - HFD 2024: https://pbo-dpb.s3.ca-central-1.amazonaws.com/files--fichiers/3bda4da4a629e05355692748e8c0220afe636f219f019060c526fbb38d37181c (`raw/pbo/hfd_2024q4_public.xlsx`)

  Each has 1,364 or 1,373 weighted records with FNETWTH and FWEIGHT, covering only families above $1M (4.43M families). **Computed** top shares from the 2023 file, using PBO denominators (16,852k families and $16,341B), reproduce Table 2 within 0.1 pp: top 1% 23.80%, top 0.1% 11.30%, top 0.01% 5.23%. Further down: top 0.001% (169 families) 2.29%, threshold about $843M; top 0.0001% (17 families) 0.79%. Outputs: `data/pbo_hfd2023_public_top_shares_computed.csv`; for 2024Q4, `data/pbo_hfd2024q4_public_top_shares_computed.csv` (public file gives 23.9% vs 24.1% published).

### 3b. 2021 update (RP-2122-023-S), 2021-12-09, requested by Sen. Rosa Galvez
Page: https://www.pbo-dpb.ca/en/publications/RP-2122-023-M--estimating-top-tail-family-wealth-distribution-in-canada-updates-trends--estimation-extremite-superieure-distribution-patrimoine-familial-canada-mises-jour-tendances ; PDF https://distribution-a617274656661637473.pbo-dpb.ca/20de98fc3f4d93c5213f8d71fbe7cd89ae69cb1899e9cbf2d3ca4d57f18ab25a
- Table 2-1 / ES-1 (p.6, PDF p.7), HFD 2019: top 0.01% 5.0, 0.1% 11.2, 0.5% 19.5, **1% 24.8**, 5% 43.5, 10% 56.9, 20% 73.9, middle 40% 25.1, bottom 40% 1.1. SFS PUMF gives top 1% 13.7.
- Table 2-2 (p.7, PDF p.8): top 1% threshold **$6.3M**, 160.6k families, $2,903B. Top 0.1% threshold $28.8M. Top 0.01% threshold $129.5M.
- Table 2-3: about 3.08M families had more than $1M, holding 73.0%.
- Table 2-4 (p.8, PDF p.9), projected to Q2 2021: top 1% 24.3% ($7.3M threshold).
- Table 3-1 (p.10, PDF p.11), top 1% share with Canadian Business list: 22.6 (1999), 25.1 (2012), 25.6 (2016). With Forbes: 19.4 / 22.2 / 22.7 / 24.8 (1999/2012/2016/2019). "the top one percent's share of net wealth in Canada has increased by approximately 5 percentage points over the period of 1999-2019."
- Outputs: `data/pbo_top_tail_published.csv` and `data/pbo_top1_share_series.csv`.

### 3c. Original 2020 report (RP-2021-007-S), 2020-06-17
PDF: https://distribution-a617274656661637473.pbo-dpb.ca/dff0e58ceb749876172be790f2d77873234642cd55509cb0dcb75fc82b10e374
- Table ES-1 (p.1, PDF p.4), 2016: HFD top 1% **25.6%** vs SFS PUMF 13.7%; top 0.01% 5.6%; bottom 40% 1.2%.
- Table B-1 (p.20, PDF p.23): top 1% threshold $5.5M, 153.4k families, $2,644B. The HFD covers about 15,349,000 families and $10.3T.
- Table 4-2 (p.9, PDF p.12): 2016 HFD projected to 2019, top 1% 25.7% ($6.1M threshold).

### 3d. Related StatCan experimental estimate (capitalization of income)
Gauthier, Hui & Krochmalnek, "Enhancing wealth distributions within the DHEA using a capitalization of income method", *Latest Developments in the Canadian Economic Accounts*, 13-605-X, 2025-07-16. https://www150.statcan.gc.ca/n1/pub/13-605-x/2025001/article/00002-eng.htm
- Table 4, Q4 2019, COI vs PBO HFD: top 0.01% 4.2 vs 5.0; top 0.1% 10.0 vs 11.2; top 1% **26.6** vs 24.8; top 10% 65.0 vs 56.9; top 20% **81.2** vs 73.9; bottom 40% 1.9 vs 1.1.
- Text: "the COI method results in a much higher share of wealth being attributed to the top 20% of the wealth distribution, at 81.2% for the COI method compared with 68.1% for DHEA as of the fourth quarter of 2019." The conclusion is that DHEA inequality is "likely underestimated".
- **Caveat:** the article says real estate, consumer goods and their liabilities "are excluded from the COI estimates". The published COI column also sums to more than 100% (81.2 + 22.5 + 1.9). Treat it as experimental.

### 3e. PBO wealth-tax costings
- **2025 federal election costing**, "Implementing annual wealth tax", 2025-04-09 (proposal sponsored by the NDP per the PBO EPC page): https://www.pbo-dpb.ca/en/epc-estimates--estimations-cpe/45/EL-45-1023246-P ; PDF https://distribution-a617274656661637473.pbo-dpb.ca/cc1b029d8d36c1340f715cb7666bfd2132ef6683608cecae42a5e71b82bb6ad6
  - Rates: 1% on net worth over $10M and up to $50M; 2% over $50M and up to $100M; 3% over $100M; effective 2025-07-01.
  - Revenue: **$121.5B over 2025-26 to 2029-30**, from $22.8B in 2025-26 to $26.8B in 2029-30.
  - Assumes 35% of net worth is hidden or avoided, and administrative costs of 2%. Uses the updated HFD.
- **M-68 one-time tax on extreme wealth**, 2021-07-15: https://distribution-a617274656661637473.pbo-dpb.ca/a2d27dbe13ecedf7ebdac816acc5ddb4ab59d297dcbd77caddd75a346472afb7
  - Rates: 3% over $10M and 5% over $20M.
  - Revenue: $44.0B over 5 years with a 35% wealth reduction, or $60.7B under alternative behavioural assumptions (15 to 20% reduction).

---

## 4. Long-run series

### 4a. WID.world (bulk download)
- Source: https://wid.world/bulk_download/wid_all_data.zip (Last-Modified 2026-09-09). I extracted `WID_data_CA.csv`, `WID_metadata_CA.csv` and the US equivalents into `raw/wid/`.
- Output: `data/wid_canada_us_wealth_income_shares.csv` (CA and US). Variables:
  - `shwealj992`: net personal wealth share, equal-split adults
  - `ghwealj992`: wealth Gini
  - `sptincj992`: pre-tax national income share
  - `sdiincj992`: post-tax income share
  - Groups: bottom 50, P50 to 90, top 10, 1, 0.1, 0.01, 0.001.
- Canada net personal wealth, top 1% share: 1900 39.3%; 1920 34.1%; 1950 30.2%; 1980 23.6%; 2000 27.4%; 2010 28.6%; 2021 29.5%; 2022 to 2024 **28.6%**.
  - Top 10%: 59.8% (2022 to 2024).
  - Bottom 50%: 14.0% (2022 to 2024).
  - Gini: 0.63.
- US 2024: top 1% 34.8%, top 10% 69.5%, bottom 50% 0.96%, Gini 0.83.
- **Caveats:**
  - WID metadata gives Canada's wealth series an `avg_quality` of **0.1**; income series are rated 5.0. The Canada wealth series is largely imputed (Bajard et al. 2021/2025 tech notes).
  - Canada's 2022 to 2024 values are identical, i.e. carried forward.
  - WID's Canada bottom 50% wealth share (about 14%) is far above SFS (about 8%) and inconsistent with survey evidence.
  - **Use WID wealth for Canada only with a strong caveat.** Prefer PBO and Davies & Di Matteo.
- Canada pre-tax national income share of the top 1%: 1920 10.4%; 1950 7.9%; 1980 7.6%; 2000 12.7%; 2019 10.4%; 2023 12.1% (2024 = 2023, carried forward). Top 10%: 36.8% (2023). Bottom 50%: 16.9% (2023).

### 4b. Davies & Di Matteo
- **Published:** Davies, J.B. & Di Matteo, L. (2021), "Long Run Canadian Wealth Inequality in International Context", *Review of Income and Wealth* 67(1): 134-164, doi:10.1111/roiw.12453. Paywalled: Wiley returned 403 and Unpaywall reports no OA copy.
  - Abstract (via RePEc https://ideas.repec.org/a/bla/revinw/v67y2021i1p134-164.html): "...Top wealth shares decline from 1892 to 1902 and from 1945 to the late 1960s ... Since 1984 there has been a clear upward trend in wealth inequality in Canada ... wealth inequality is higher in Canada than in the U.K. and much of continental Europe, somewhat lower than Austria or Germany, and distinctly lower than the U.S."
- **Numbers used here come from the open working paper**, Research Report 2018-1, "Filling the Gap: Long Run Canadian Wealth Inequality in International Context" (Jan 2018), Western University. PDF via https://uwo.scholaris.ca/server/api/core/bitstreams/776c2e67-0e98-4464-b759-88ef4cfe9573/content (`raw/dd/dd_wp2018.pdf`).
  - Table 1 (unadjusted, PDF p.25) and Table 2 (adjusted to rich lists, PDF p.26) are transcribed in `data/davies_dimatteo_wp2018_wealth_shares.csv`.
  - Adjusted top 1% share: Ontario families 41.0 (1892), 36.4 (1902); Canada adults 29.6 (1946 to 1950 average), 27.7 (1951 to 1953), 22.9 (1960 to 1965), 19.6 (1966 to 1970); Canada families 22.9 (1970), 24.5 (1984), 22.9 (1999), 23.1 (2012).
  - **Caveat:** the published 2021 version revised the analysis (it extends to 2016 and finds a rising trend since 1984), whereas the WP abstract said "no trend since 1970". **Before publication, check the RIW 2021 tables, which require journal access**, and replace these WP figures if they differ.

---

## 5. Billionaires (Forbes)

- Source: the Forbes list API (the same data as forbes.com/billionaires). The 2026 list is dated 2026-03-10 with 3,428 entries.
  - https://www.forbes.com/forbesapi/person/billionaires/2026/position/true.json (`raw/forbes/forbes_billionaires_2026.json`)
  - Also 2005 to 2025, except 2000, which is unusable.
- **Forbes 2026, Canadian citizens:** **82 billionaires, combined US$457.2B**. Of these, 50 live in Canada (US$201.6B). 32 citizens live abroad or have no residence recorded (5 have none), including:
  - Changpeng Zhao (UAE), US$110B
  - Joseph Tsai (Hong Kong)
  - Sherry Brydson
  - Peter and Taylor Thomson
  - Bruce Flatt (UK)
  - Lawrence Stroll (Switzerland)
  - Greg Abel (US)

  Note that CZ alone accounts for 24% of the citizens' total.
- Top 20 (US$B, Forbes world rank): Changpeng Zhao 110.0 (#17); Stuart Hoegner 24.7 (#104); Joseph Tsai 13.3; Jim Pattison 12.0; Sherry Brydson 11.5; Tobi Lutke 10.7; David Cheriton 9.8; Alain Bouchard 7.9; Ling Tang 7.9; Peter Gilgan 7.7; Daniel Nadler 7.6; Daryl Katz 7.2; Christopher Olah 7.0; Stephen Smith 7.0; David Thomson 7.0; Peter Thomson 7.0; Taylor Thomson 7.0; Bruce Flatt 6.7; Chip Wilson 6.5; Lino Saputo & family 6.4. The top 20 sum to US$284.9B. The full list is in `data/forbes_2026_canadian_billionaires.csv`; 2025 is in `data/forbes_2025_canadian_billionaires.csv`.
- **Inheritors** (Forbes `selfMade=false`): **16 of 82, US$75.5B**.
  - Thomson family, 6 people, US$43.5B: Brydson 11.5; David, Peter and Taylor Thomson 7.0 each; Linda Campbell 5.5; Gaye Farncombe 5.5.
  - Saputo 6.4
  - Fidani 4.5
  - Zekelman family 7.7: Barry 3.6, Clayton 2.2, Alan 1.9
  - Péladeau 3.4
  - Azrieli sisters 5.8
  - Charles Bronfman 2.5
  - Brandt Louie 1.7
- **Important:** several famous Canadian dynasties are **not on the Forbes list at all**: Weston, Irving, Rogers, Desmarais, Richardson, McCain, Sobey. I checked by name in the 2026 and 2025 JSON. This is why PBO supplements Forbes with Maclean's / Canadian Business (PBO 2020 report: CB's "list contains billionaires missing in the Forbes list"). Do not state their wealth from Forbes.
- **Counts over time** (`data/forbes_canadian_billionaires_by_year.csv`, Canadian citizens / residents / citizens' net worth): 2005 14/12/$43.6B (API incomplete); 2010 24/21/$74.7B; 2015 40/32/$135.8B; 2020 44/36/$142.8B; 2021 64/45/$231.1B; 2022 65/45; 2023 63/42; 2024 67/45/$314.7B; 2025 76/47/$359.1B; **2026 82/50/$457.2B**.
  - The 2000 API record is unusable (only 8 entries worldwide).
  - The API's `selfMade` flag looks unreliable for 2010.
  - Historical `countryOfCitizenship` values may be back-filled by Forbes.
- Real-time list: the endpoint returned 503 on 2026-10-10 and was not captured.
- CAD conversion in the curve file uses Bank of Canada FXUSDCAD 1.3567 for 2026-03-10 (Valet API, `raw/boc/`).

---

## 6. Government and household balance sheets (NBSA, table 36-10-0580-01)

- Table: https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=3610058001 (CSV dated 2026-09-11; latest quarter **2026Q2**). Market value. Values are unconsolidated: general government = federal + other levels + social security funds (CPP/QPP); this adds exactly for net financial assets.
- Output: `data/nbsa_sector_balance_sheets_quarterly.csv` (1990 onward). Per-adult figures are in `data/nbsa_per_adult_selected.csv` (**computed** with July 1 population from 17-10-0005-01: 2026 adults 18+ = 34,158,557; total population 41,798,407).

| 2026Q2 ($M) | Net financial assets | Net worth | NW per adult (computed) |
|---|---|---|---|
| Households | 9,135,950 | **19,148,610** | **$560,580** |
| Households + NPISH | 9,354,791 | 19,423,212 | $568,619 |
| General governments (total) | **-448,577** | **1,531,555** | $44,837 (NFA: -$13,132) |
| Federal general government | -1,051,171 | **-910,276** | **-$26,649** (NFA: -$30,773) |
| Provincial/territorial | -397,394 | 735,156 | $21,522 |
| Local | +18,394 | 665,265 | $19,476 |
| Social security funds (CPP/QPP) | **+1,033,988** | 1,033,988 | $30,270 |
| National net worth | 1,945,455 (net foreign assets) | 20,302,362 | $594,357 |

- From the Daily (2026-09-11):
  - "net worth per capita expanded by $13,785, putting the national average at $462,336". StatCan uses households + NPISH and quarterly population; my per-capita figure based on July 1 population is $464,688.
  - "The federal government's net financial liabilities as a share of gross domestic product ... decreased to 33.3% in the second quarter of 2026 ... the ratio of net debt to GDP for other levels of government fell to 14.7%."
  - "Total government gross debt per capita ... reaching $105,455 in the second quarter of 2026."
- **IMF WEO April 2026** (published 2026-04-14), via the IMF SDMX API https://api.imf.org/external/sdmx/2.1/data/IMF.RES,WEO/... (`raw/imf/weo21.csv`). Output: `data/imf_weo_apr2026_gg_debt_pct_gdp.csv`. General government **net debt**, % of GDP, 2025:

  | Country | 2025 |
  |---|---|
  | **Canada** | **10.2** (projection; last actual 2024 = 10.8) |
  | US | 96.7 |
  | UK | 93.8 |
  | France | 108.8 (proj.) |
  | Germany | 47.2 |
  | Italy | 127.7 |
  | Japan | 136.5 (proj.) |

  Gross debt in 2025: Canada 113.5, US 123.9.
  - IMF's own Canada note: "Canada's net debt corresponds to net financial liabilities as reported by Statistics Canada and includes equity and investment fund shares, which Canada has built up substantially. Statistics Canada made a methodological change in late 2022 to value assets at market value instead of book value, which has decreased net debt."
  - CPP/QPP assets (social security funds, NFA +$1.03T) are inside general government, which is a key reason Canada's net debt is low.
  - The G7 aggregate was not returned by the API, so no G7 average is given.

---

## 7. Wealth Gini and Canada vs US comparison

- **StatCan publishes no wealth Gini** in SFS or DHEA tables. Its Gini table 11-10-0134-01 covers income only.
- **Computed** grouped-decile Gini from SFS 11-10-0075-01 (`data/wealth_gini_computed.csv`). This is a lower bound because it ignores inequality within deciles, and the SFS also misses the top: 1999 0.658; 2005 0.667; 2012 0.654; 2016 0.651; 2019 0.648; **2023 0.612**.
- **WID** wealth Gini (equal-split adults): Canada 0.63 (2022 to 2024, low quality) vs US 0.83.
- **OECD Wealth Distribution Database** (SDMX dataflow OECD.WISE.INE:DSD_WEALTH@DF_WEALTH, https://sdmx.oecd.org/public/rest/data/OECD.WISE.INE,DSD_WEALTH@DF_WEALTH,1.0/... ; output `data/oecd_wdd_wealth_shares.csv`). Household survey basis; **no Gini in WDD**.

  | | Canada 2023 (SFS) | US 2022 (SCF) |
  |---|---|---|
  | Top 1% share | 12.3% | 37.4% |
  | Top 5% share | 32.2% | 63.9% |
  | Top 10% share | **47.1%** | **75.1%** |
  | Bottom 40% share | 3.9% | 0.6% |
  | Mean/median ratio | 2.04 | 5.89 |

  Canada 2019: top 10% 51.4%, top 1% 17.5%. The US Survey of Consumer Finances oversamples the wealthy; the SFS does not. That partly explains the gap at the top 1%. On a top-tail-corrected basis Canada's top 1% is about 24% (PBO) vs about 35% for the US (WID).

---

## 8. Stitched percentile curve (analogue of the original's chart)

`data/wealth_curve_stitched_canada.csv` stitches three segments:

1. **SFS 2023 within-decile medians** at P5 to P95 (economic families, 2023 $).
2. **Thresholds computed from the PBO public HFD 2023** at P80 to P99.999 (families, 2023 $, NBSA-aligned).
3. **108 rows in total**, the last 82 of which are individual Forbes 2026 Canadian-citizen billionaires (individuals; US$ converted to CAD at 1.3567).

Caveats:
- The segments are not on one consistent basis. Units differ (families vs individuals), years differ (2023 vs 2026), and treatment differs (SFS is survey-raw; HFD is scaled to NBSA, which lowers the P85 to P90 range relative to SFS: SFS P85 is about $1.92M vs HFD P85 $1.58M).
- Forbes includes non-residents.
- Show the segments as distinct series with a note. Do not present them as one seamless distribution.

---

## Files

`data/` (tidy CSVs):
- `dhea_networth_by_wealth_quintile_quarterly.csv`: DHEA 36-10-0660-01, net worth by wealth quintile, 2010 to 2025Q4
- `sfs_networth_by_decile.csv`: SFS 11-10-0075-01 (1999 to 2023; Canada + 5 regions)
- `sfs_networth_by_quintile_canada_provinces.csv`: SFS 11-10-0049-01
- `sfs_networth_by_age_familytype_geo.csv`: SFS 11-10-0016-01 (median/mean by age, family type, province, CMA)
- `pbo_top_tail_published.csv`: PBO 2020/2021/2025 tables (thresholds, families, totals, shares)
- `pbo_top1_share_series.csv`: PBO top 1%/5% series 1999 to 2023
- `pbo_hfd2023_public_top_shares_computed.csv` and `pbo_hfd2024q4_public_top_shares_computed.csv`: computed from PBO public microfiles
- `wid_canada_us_wealth_income_shares.csv`: WID wealth and income shares and Gini, CA and US
- `davies_dimatteo_wp2018_wealth_shares.csv`: Davies & Di Matteo WP 2018-1, Tables 1 and 2
- `forbes_2026_canadian_billionaires.csv`, `forbes_2025_canadian_billionaires.csv`, `forbes_canadian_billionaires_by_year.csv`
- `nbsa_sector_balance_sheets_quarterly.csv`: NBSA 36-10-0580-01 (households, governments, national; 1990 to 2026Q2)
- `nbsa_per_adult_selected.csv`: per-adult and per-capita figures (computed)
- `population_canada_adults_annual.csv`: 17-10-0005-01
- `imf_weo_apr2026_gg_debt_pct_gdp.csv`: IMF WEO Apr 2026, net and gross debt for G7 countries
- `oecd_wdd_wealth_shares.csv`: OECD WDD, 8 countries
- `wealth_gini_computed.csv`: grouped SFS-decile Gini (lower bound)
- `wealth_curve_stitched_canada.csv`: stitched curve (see section 8)

`raw/` holds untrusted downloads: StatCan zips and CSVs, Daily HTML, PBO PDFs and xlsx, WID country CSVs, Forbes JSON, OECD/IMF CSV, BoC JSON, and the D&DM working paper.
`scripts/` holds the Python used. All of it was run with `python3 -I`.
