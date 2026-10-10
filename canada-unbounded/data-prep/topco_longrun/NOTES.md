# Research notes: long-run top-firm churn (Task A) and the "Canada did best in 2008-09 and COVID" claim (Task B)

Compiled 2026-10-10. Every number in the CSVs carries a `source` and `source_url`. Raw downloads are in `raw/`, and the scripts that rebuild the CSVs are in `scripts/`:

```
python3 -I scripts/build_topfirms.py raw .
python3 -I scripts/build_crisis.py raw .
```

## Files

| File | Contents |
|---|---|
| `canada_top_firms_longrun.csv` | Canada's top 100 non-financial firms, 1975/76: sales, assets and net income. Canada's top 25 financial institutions, 1975/76, by assets. Canada's largest company by market cap (#1 only), 1820-2017. |
| `canada_churn_stats.csv` | Official churn and aggregate statistics for Canada's top 100: 1923-33 (Royal Commission on Price Spreads) and 1967-71 / 1971-75 (Royal Commission on Corporate Concentration, RCCC). |
| `us_top_firms_longrun.csv` | Fortune 500 top 50 by revenue for every list year 1955-2026, with a `lineage_key` column for matching firms across renamings. Also US firms in the world's top-100 industrials by market cap, 1912 (Hannah). |
| `us_fortune_topN_churn.csv` | For every list year: how many of the top 10 and top 25 were in the top 10/25 ten years earlier, with entrant names. Also how many of the 1955 top 10/25/100/500 were still on the Fortune 500 in later years. |
| `us_churn_stats.csv` | Published churn statistics: Kauffman (think tank) and Hannah (LSE CEP, academic). |
| `crisis_comparison_long.csv` | Tidy table for the 38 OECD countries, flagged G7, covering 2008-09 and COVID. Columns: country, period, indicator, value, unit, source, url, note. |
| `crisis_canada_ranks.csv` | Canada's rank among the G7 and among OECD countries for each indicator where a ranking makes sense. |

---

## TASK A: corporate churn

### Key correction about Morck, Percy, Tian & Yeung (NBER WP 10635 / NBER chapter c10268)

- I read both the working paper and the book-chapter PDFs in full text: `raw/w10635.pdf` and `raw/c10268.pdf`, plus the 2003 conference draft `raw/morck_conf.pdf`.
- **None of them contains firm-level appendix tables.** The top-100 data appear only in aggregate, as stacked shares by control type in Figures 5-7. Those cover the years 1902 (66 firms), 1910, 1920 ... 1960, 1965, 1969, 1975, 1980, 1985, 1990, 1994 and 1998.
- The data section of WP 10635 (p.3) describes how the lists were built:
  - 1965-1998: the Financial Post top 100, ranked by assets until 1967 and by revenue after that.
  - 1910-1940: the authors' own rankings from the *Canadian Annual Financial Review*.
  - 1950-1960: *Financial Post Corporate Securities*.
  - Financial firms are excluded.
- Morck & Tian (NBER w21707, 2015) uses the same panel and also publishes no firm list.
- **Name/rank/assets lists for 1902-1998 therefore cannot be extracted from these papers.** Getting the underlying lists would mean contacting Randall Morck or Gloria Tian (now at U. Lethbridge), or rebuilding them from the source directories.

### What I could get for Canada

1. **RCCC Report (1978), Table 2.1: the full top 100 for 1975/76.** Ranked by sales, with sales, assets and net income.
   - The table includes foreign-owned subsidiaries (GM Canada, Ford Canada, Imperial Oil and others) and Crown corporations (Ontario Hydro, Hydro-Québec, CN, Air Canada).
   - Data come from the *Financial Post 300* (Summer 1976) and *Canadian Business* (July 1976). The source is official.
   - Text extracted from the PDF with two OCR fixes (rows 9 and 100), both checked against the printed table.
   - Top 10:
     1. George Weston
     2. Ford Canada
     3. GM Canada
     4. Imperial Oil
     5. Canadian Pacific
     6. Bell Canada
     7. Massey-Ferguson
     8. Chrysler Canada
     9. Alcan
     10. Dominion Stores
2. **RCCC Table 2.2: the 25 largest financial institutions, 1975/76, by assets.** The top 5 are RBC, CIBC, BMO, BNS and TD, which are still the Big 5 today.
3. **Official churn statistics for Canada's top 100 (RCCC Table 2.3):**
   - 1967-71: 21 exits (13 displaced, 1 bankruptcy, 7 mergers).
   - 1971-75: 22 exits (16 displaced, 0 bankruptcies, 6 mergers).
   - So roughly 78-79 of the top 100 survived each 4-year window.
   - Among the top 25 financial institutions, only 2 new entrants appeared in each window.
4. **Royal Commission on Price Spreads (1935).** The top 100 non-financial firms by net assets held $5,096M in 1923 and $7,324M in 1933. Five transportation companies held $3,193M and $3,831M of those totals. 63 large companies existed continuously through 1923-33. The report gives aggregates only, with no names.
5. **Finaeon / Global Financial Data blog (commercial vendor, labelled as such): Canada's #1 company by market cap in sample years.**
   - Canadian Pacific Railway: 1890-1930
   - Inco: 1940-1970
   - Gulf Canada: 1980
   - BCE: 1990
   - Nortel: 2000
   - TD: 2010
   - RBC: 2017
   - Only the top firm is given, and the currency/units are not stated.
6. **Financial Post 500 spreadsheets for 1999 and 2003-2013 exist in the University of Alberta ERA repository** (collection "FP 500: Canada's largest corporations"). Downloads return HTTP 401 because they are restricted to UAlberta users. Someone with UAlberta access could fill 1999-2013 by revenue.
7. **No Canadian-headquartered firm was among the world's 100 largest industrials of 1912** (Hannah 1997). International Nickel was counted as US-headquartered.

### United States

- **Fortune 500, 1955-2019:** from the `cmusam/fortune500` GitHub repo (commit 29ef4c3, Dec 2019), a scrape of money.cnn.com's archive (1955-2005) and fortune.com / FortuneChina (2006-2019).
- **Fortune 500, 2020-2026:** parsed directly from the fortune.com ranking pages (`__NEXT_DATA__` JSON), which give revenue and assets for the full Fortune 1000.
  - The 2026 list is top-ranked by **Amazon ($716.9B), ahead of Walmart ($713.2B)**. This is the first year Walmart is not #1 since 2013.
- **Caveat on names:** the CNN archive used *later/successor* names for 1955-2005 entries. Examples:
  - "Exxon Mobil" for Standard Oil (NJ)
  - "ChevronTexaco" for Standard Oil of California
  - "Esmark" for Swift
  - "AT&T Technologies" for Western Electric
  - "Altria Group" for Philip Morris
  - "Navistar" for International Harvester

  fortune.com also renames retroactively, e.g. "GE Aerospace" in 2020. Match firms on `lineage_key` (an alias map in the script), not on `company`.
- **Caveat on list scope:** until the 1994 list, the Fortune 500 covered industrials only (manufacturing, mining, oil). From 1995 it included services, which is why Walmart, AT&T, Citigroup and others appear. A list dated year Y ranks fiscal year Y-1 revenue.
- **1912:** Hannah (1997, LSE CEP DP 318) Appendix A ranks the world's top 100 industrials by equity market cap. I kept the US-headquartered firms and re-ranked them 1-25 among US firms.
- **Gap:** I found no open-access US top-100-by-assets lists for 1917-1950. The candidates are all paywalled or only in print:
  - Navin (1970) "The 500 Largest American Industrials in 1917" (*Business History Review*)
  - Kaplan (1964) *Big Enterprise in a Competitive System*
  - Collins & Preston (1961, AER)
  - Chandler's *Scale and Scope*, Appendix A
  - Forbes' 1917 assets list, reprinted in Forbes' 2017 centenary issue

  web.archive.org was unreachable from this environment (connection reset).

### Churn results (computed: `us_fortune_topN_churn.csv`)

**US top 10 still in the top 10 ten years earlier (Fortune 500 by revenue):**

| List year | Still in top 10 | Entrants |
|---|---|---|
| 1965 | 7/10 | Ford (unlisted in 1955), IBM, Texaco |
| 1975 | 8/10 | |
| 1985 | 7/10 | |
| 1995 | 7/10 | Walmart, Sears, Philip Morris |
| 2005 | 6/10 | |
| 2015 | 6/10 | Apple, Berkshire, CVS, Phillips 66 |
| 2025 | 5/10 | Alphabet, Amazon, Cencora, McKesson, UnitedHealth |
| 2026 | 7/10 | |

- **Lowest values:** 3/10 in each list year 2019-2022, against 2009-2012.
- **Top 25, ten-year overlap:** 21 in 1965, 18 in 1975, 17 in 1985, 14 in 1995, 12 in 2005, 16 in 2015, 15 in 2025.
- **Survival of the 1955 cohort on the full Fortune 500**, using lineage-name matching:

  | Cohort | 1985 | 1995 | 2005 | 2015 | 2025 |
  |---|---|---|---|---|---|
  | 1955 top 10 | 6 | 6 | 5 | 5 | 5 |
  | 1955 top 100 | 70 | 46 | 34 | 30 | 24 |
  | 1955 full 500 | 211 | 115 | 85 | 62 | 51 |

  These counts are lower bounds, because renamed firms are under-matched after 2005.
- **Published statistics** (`us_churn_stats.csv`):
  - Kauffman (think tank), average annual Fortune 500 turnover:
    - 6.5%/yr in 1955-59
    - under 5% in the 1960s-70s
    - 7.5% in the 1980s-90s
    - 6.5% in the 2000s
  - Kauffman, median annual turnover: 29 firms/yr in 1955-93 and 39 in 1995-2011. The rise is partly an artefact of the 1994 change in list scope.
  - Hannah (academic): only 21% of the world's top 100 industrials of 1912 were still in the top 100 in 1995. The figure was 19% for US firms, 47% for UK firms and 29% for German firms.
- **Innosight (consultancy) S&P 500 average-tenure figures could not be verified at source.** innosight.com now redirects to Huron. Secondary reports give 33 years in 1964/65 falling to roughly 20-24 years. Do not cite these without the original PDF.

### Comparability (assets vs revenue vs market value)

| Source | Measure | Notes |
|---|---|---|
| Forbes Global 2000 (2011-26) | Market value | Volatile; favours tech and banks. |
| Fortune 500 | Revenue | Favours retailers, distributors and health intermediaries (McKesson, Cencora, CVS). Excluded services before 1995. |
| RCCC 1975 | Sales (assets also given) | Includes foreign subsidiaries and Crown corporations. Forbes/FP lists of public companies would exclude GM Canada and Ontario Hydro. |
| Morck et al. | Assets to 1967, revenue after | Excludes financial firms. |
| Finaeon | Market cap | #1 only. |

Banks dominate any Canadian list that includes financial firms: the Big 5 in 1975 are the Big 5 in 2026. A like-for-like Canada/US churn comparison needs the same measure and scope. The cleanest option with data in hand is to compare Forbes Global 2000 Canada vs US top 10 by market value for 2011-26. That gives a short window. For a longer window, compare the 1975 RCCC Canadian top 10 by sales with the 1976 Fortune top 10 by revenue, and repeat with the FP500 if access is obtained.

**Suggested framing for the essay:** the 1970s official data show Canadian top-100 turnover of about 21-22% per 4 years. That is not obviously low. US Fortune 500 turnover in the 1970s was under 5% per year, roughly 18% over 4 years, which is similar. The persistence that stands out in Canada is at the very top, among the banks, and in family-controlled groups (Morck et al.), not in non-financial top-100 membership.

---

## TASK B: fact-check, "Canada did best of any country through 2008-09 and COVID"

### Data sources

- **Real GDP growth:** World Bank WDI (API) and IMF WEO April 2026 (DataMapper API).
- **Quarterly real GDP:** OECD Quarterly National Accounts (SDMX, chain-linked volume, seasonally adjusted).
- **Unemployment:** OECD harmonised monthly rate, 15+. IMF WEO gives annual rates.
- **Excess mortality:**
  - WHO modelled estimates, 2020-21 (May 2023 release).
  - OECD weekly excess deaths against the 2015-19 average. This measure is not age- or trend-adjusted.
  - OWID, Karlinsky-Kobak projected baseline (labelled aggregator).
  - The Economist model (labelled media model).
- **Vaccination:** OWID.
- **COVID fiscal support:** IMF Fiscal Monitor Database of Country Fiscal Measures, as of 27 September 2021, Table 1.
- **Banking crises:** Laeven & Valencia (2018), IMF WP/18/206. FDIC failures API.
- **Canada facts:** PBO costing note on the IMPP, and Zorn, Wilkins & Engert (2009), *Bank of Canada Review*.

### 2008-09: largely true *within the G7*, false for "any country"

Canada was best or near-best in the G7 on these measures:

- **Smallest 2007→2009 cumulative fall in real GDP:**

  | Country | Change 2007→2009 |
  |---|---|
  | Canada | -1.9% |
  | France | -2.5% |
  | US | -2.5% |
  | UK | -4.6% |
  | Germany | -4.7% |
  | Italy | -6.3% |
  | Japan | -7.0% |

  Source: WB WDI.
- **Highest 2008 growth in the G7:** +1.0%.
- **Fastest return to the pre-crisis quarterly peak:** 9 quarters (2008Q3→2010Q4), against 10 for the US (2008Q2→2010Q4 in the current OECD vintage), 12 for Germany and France, 21 for the UK, 22 for Japan, and 57 for Italy.
- **No systemic banking crisis** in Laeven-Valencia.
  - By contrast: US (2007), UK (2007), Germany (2008), Italy (2008) and France (2008, borderline).
  - Gross fiscal cost of bank restructuring: UK 8.8% of GDP, US 4.5%, Germany 2.7%.
  - The US had 25 bank failures in 2008, 140 in 2009 and 157 in 2010 (FDIC; excludes assistance transactions).
- **"No bank bailout" needs a footnote.** The support was liquidity, not recapitalisation:
  - CMHC's Insured Mortgage Purchase Program bought **$69 billion** of insured mortgage pools in 2008-10, against a $125B limit (PBO Legislative Costing Note, 2 April 2020).
  - The Bank of Canada's term liquidity outstanding peaked at **over $40 billion in December 2008** (Zorn, Wilkins & Engert 2009, *BoC Review*).
- **WEF ranked Canada's banking system soundest in the world for seven consecutive years, 2008-2014.** Source: Department of Finance releases (2013: sixth consecutive year; 2014: seventh). The WEF measure is the "soundness of banks" item in the Executive Opinion Survey, which is a perception survey, not a stress metric.

Where the claim fails for 2008-09:

- **2009 growth:** Canada -2.9%, against the US -2.6% and France -2.7/-2.8%. Canada ranks 3rd in the G7.
- **Peak-to-trough quarterly decline:** Canada -4.4%, against the US -4.0% and France -4.1%. Canada ranks 3rd in the G7.
- **Unemployment rise:** Canada +2.6 pp to a peak of 8.8% (June 2009). Germany's rate *fell*, and Japan (+1.7 pp) and France (+2.3 pp) rose less. Canada ranks 4th in the G7.
- **OECD-wide:** Canada ranks 14th of 38 on 2007-09 GDP change. Poland, Australia, Israel, Colombia and Korea grew through the crisis, and Australia had no recession.

### COVID: false as stated; strong on health outcomes and vaccination, middling on the economy

Where Canada did well:

- **Excess mortality was second-lowest in the G7, after Japan,** on every age- or trend-adjusted measure:
  - WHO 2020-21: about 70 per 100k, P-score 4.5%. The US was about 292 per 100k, Italy about 310, the UK about 220, Germany about 122, and Japan about 0.
  - The Economist model, cumulative to the end of 2022: 164 per 100k, against 391 for the US.
  - OWID projected baseline, to the end of 2022: 1,426 per million, against 3,759 for the US.
  - Among OECD countries, Canada ranks about 10th of 38 (WHO). New Zealand, Japan, Australia, Korea, Denmark, Iceland, Norway and others did better.
  - The OECD's crude measure (excess against the 2015-19 average, with no adjustment for population growth or ageing) ranks Canada 5th of 6 G7 countries with data for 2020-23. Canada's rapid population growth inflates its crude excess deaths, so use the adjusted measures.
- **Vaccination:**
  - At least one dose: Canada was highest in the G7 at the end of 2021 (82.6%) and the end of 2022 (90.0%).
  - Fully vaccinated: second to Japan.
  - Canada was slow early on: 30.7% fully vaccinated on 30 June 2021, 6th of 7 in the G7.

Where Canada did not do best:

- **Real GDP:**
  - 2020: -5.0%, against the US -2.1% (best in the G7), Germany -4.1% and Japan -4.3%. Canada ranks 4th of 7.
  - Return to the 2019Q4 level: Canada took until 2021Q3. The US and Japan got back in 2021Q1.
  - By 2023Q4, Canada was +6.8% above 2019Q4, against +9.8% for the US. Canada ranks 2nd in the G7.
  - Across the OECD, Canada ranks 24th-26th of 38 on most COVID GDP measures.
- **Unemployment:** peaked at 14.2% (May 2020), up 8.4 pp from February 2020. Canada is 6th of 7 in the G7, behind only the US (14.8%, +11.3 pp). European job-retention schemes kept their measured unemployment low, so this partly reflects policy design.
- **Fiscal support (a size measure, not a performance measure):**
  - Above-the-line support was 15.9% of 2020 GDP. That is 4th in the G7, after the US (25.5%), the UK (19.3%) and Japan (16.7%).
  - Liquidity support was 4.0%, small next to Italy (35.3%), Japan (28.3%) and Germany (27.8%).
  - So "Canada spent the most" is also not supported by the IMF data, though it ranks high on household transfers. I did not break this out.

### Verdict

**Mostly false as worded. A defensible version is narrower:**

> "Among G7 countries, Canada came through the 2008-09 financial crisis with the smallest output loss and no banking crisis, and through COVID with the second-lowest excess mortality (after Japan) and the highest first-dose vaccination rate."

- "Best of any country" fails in both episodes. In 2008-09, Australia, Poland, Korea and Israel did better. During COVID, New Zealand, Japan, Australia and Korea did better on health.
- On the 2008-09 economic numbers, the US matched or beat Canada on 2009 growth and on the depth of the downturn.
- For COVID, "Canada did best" is wrong on the economy. The US recovered fastest in the G7, and Canada's unemployment spike was among the largest.

### Method notes and caveats

- **GFC quarterly peak:** the maximum real GDP level in 2007Q1-2009Q2. The trough is the minimum after the peak through 2010Q4. This understates a dip that is followed by a higher level within the window: Australia shows 0%.
- **Vintages:** OECD QNA and WB are current vintages, accessed 2026-10-10. Revisions move US peak-to-trough numbers. An older BEA vintage shows the US peak in 2007Q4 and a roughly 4.3% decline.
- **Unemployment:** OECD uses 15+ for all countries. The US official rate is 16+.
- **WHO Japan:** the WHO estimate of about 0 excess deaths for Japan in 2020-21 is the WHO's own figure. Other models give Japan small positive excess.
- **IMF fiscal table:** values were transcribed from the PDF summary table. The subtotal columns were checked against component sums: health + non-health = above-the-line, e.g. Canada 2.8 + 13.1 = 15.9. The IMF warns the database is "not meant for comparison across economies."

## Gaps and leads

- **Morck et al. firm-level top-100 lists (1902-1998):** not published. Ask the authors, or rebuild from the Financial Post 500 (print, 1965+), *Canadian Annual Financial Review* (1901-1940) and *FP Corporate Securities*.
- **FP500 1999-2013 spreadsheets:** on UAlberta ERA, but access is restricted.
- **web.archive.org** was blocked from this environment, so Fortune pages could not be checked against Wayback snapshots. The cmusam scrape stands in for that check.
- **US 1917-1950 asset rankings:** paywalled. See the candidate list above.
- **Innosight S&P tenure figures:** unverified.
