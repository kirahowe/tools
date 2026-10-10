# Canada's talent flows: who leaves, who arrives, who returns, and the net

Research notes for the data-essay series. Compiled 2026-10-10.

**Labels**
- **[primary]**: official statistics or peer-reviewed or academic working papers.
- **[calc]**: my own arithmetic on primary data.
- **[context]**: think tank, industry or advocacy material.
- **[unverified]**: secondary reports I could not check against the original.

This file builds on research/context/notes.md §6j (emigration counts from 17-10-0040) and does not repeat it.

---

## 0. Bottom line (for the author)

1. **Headcount: not close.** Canada takes in about 4 to 10 permanent immigrants for every net emigrant (4.0 in 2000, 9.5 in 2019, 6.4 in 2025), before counting temporary residents. In 2025 it took in 393,781 immigrants against 61,508 net emigrants [primary, 17-10-0040; ratio is calc].
2. **Degree holders: still positive.** Immigrants are much more educated than the Canadian-born: 59.4% of 2016-21 arrivals aged 25-64 hold a bachelor's degree or higher, against 26.8% of non-immigrants. They also make up more than half of all doctorate holders.
   - In the 1990s, degree-holding arrivals outnumbered degree-holding departures to the US by about 4 to 1 (StatCan).
   - A rough modern equivalent [calc] is about 1.6 to 1 against *gross* degree-holding emigration to all destinations, and about 4 to 1 if returnees are as educated as leavers.
3. **The top tail is where Canada loses.** Leaving rates rise with ability, both for the Canadian-born and for immigrants who already hold PR or citizenship.
   - Leaving is most common among graduates of top universities (U of T, UBC, Waterloo, McGill, U de M, U of A), PhDs, math and CS graduates, and high earners.
   - Canadian CS graduates of top-ranked universities: 77% are still filing taxes in Canada 3 years out, against 90% from other institutions.
   - At Waterloo, Canadian-born graduates at the 95th skill percentile leave at twice the rate of those at the 5th, and the best leavers are the least likely to return.
   - Physicians are the exception: the 1990s physician brain drain has largely ended, with net flows of -35 in 2024 and +14 in 2023.
4. **The "they're immigrants themselves" point is right, in both directions.**
   - Immigrants are 61.2% of emigrants, against 23.6% of the population.
   - More than 15% of immigrants leave within 20 years of landing.
   - International students are the least likely group to stay. Under 60% of 2015-2020 international STEM grads are still filing 3 years out, although the most able Waterloo international students are the most likely to stay.
   - So a large part of measured "brain drain" is brain *circulation* of people Canada recruited, not Canadian-born talent.
5. **Verdict in one line:** Canada is a large net importer of degrees, but a modest net exporter of its very best home-grown STEM, AI and PhD talent, mainly to the US. The evidence measures the size of that top-tail loss in people, not in dollars or output.

---

## 1. Total flows, 2000-2025 (`canada_talent_flows_2000_2025.csv`)

**Source:** StatCan 17-10-0040-01, quarterly estimates summed to calendar years [primary] https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1710004001. Population is 17-10-0009-01 (July 1). The stock of non-permanent residents (NPR) is 17-10-0121-01. Zips: https://www150.statcan.gc.ca/n1/tbl/csv/<pid>-eng.zip (downloaded 2026-10-10; file dated Sept 23, 2026).

**Methodology break (from table footnotes 7, 12 and 13).**
- From July 2016, short-term emigrants and short-term returnees are counted inside "emigrants" and "returning emigrants".
- "Net temporary emigration" is no longer calculated after that date.
- Before July 2016: net emigration = emigrants + net temporary emigration − returning emigrants.
- So **gross emigrant counts jump in 2016 partly by construction.** Compare *net* emigration across the break. The CSV `note` column flags this.

| Year | Immigrants | Emigrants | Returning | Net emigration | Net NPR | Immigration rate /1,000 | Emigration rate /1,000 |
|---|---|---|---|---|---|---|---|
| 2000 | 227,429 | 48,841 | 17,550 | 57,109 | 28,433 | 7.41 | 1.59 |
| 2010 | 280,739 | 59,688 | 35,990 | 42,187 | 11,927 | 8.26 | 1.76 |
| 2015 | 271,867 | 68,945 | 39,436 | 56,566 | −9,330 | 7.61 | 1.93 |
| 2019 | 341,174 | 90,460 | 54,669 | 35,791 | 189,781 | 9.07 | 2.40 |
| 2023 | 471,817 | 112,561 | 51,380 | 61,181 | 756,689 | 11.79 | 2.81 |
| 2024 | 483,654 | 109,535 | 49,186 | 60,349 | 302,690 | 11.75 | 2.66 |
| 2025 | 393,781 | 111,572 | 50,064 | 61,508 | −89,445 | 9.46 | 2.68 |

- Rates are [calc] against the July 1 population.
- Net emigration rate in 2025 was about 1.48 per 1,000, or 0.15% of the population [calc].
- The NPR stock (July 1) was 2,196,877 in 2023, 2,900,083 in 2024 and 2,934,388 in 2025.
- Cumulative totals for 2000-2025: 7.62M immigrants, 1.93M emigrants and 1.04M returning emigrants [calc]. Gross emigrants are not comparable across the 2016 break.
- Q1-Q2 2026 are also in the table (emigrants 26,114 and 24,926). They are not in the CSV because the year is incomplete.

**Long-run context [primary].**
- Permanent emigration was more than 1% of the population a year early in the 20th century and about 0.15% in the 1990s (Zhao, Drew & Murray 2000; URL in §5).
- Card (2003): "even since 1960 the emigration rate has remained at about one third of the immigration rate"; Canadians in the US "have been better-educated than those who remained in Canada" for at least 60 years. https://eml.berkeley.edu/~card/papers/canadian-emigrants.pdf [academic]

**OECD comparison (`oecd_permanent_inflows_per1000.csv`).**
- Source: OECD standardised inflows of permanent-type migrants (DF_MIG_INT_PER), from https://sdmx.oecd.org/public/rest/data/OECD.ELS.IMD,DSD_MIG_INT@DF_MIG_INT_PER,1.0/all [primary]. Per-1,000 is [calc] using World Bank population.
- 2024 rates per 1,000:

| Country | Per 1,000 (2024) | Note |
|---|---|---|
| Luxembourg | 38.9 | free movement inflates EU/EFTA figures |
| Switzerland | 15.1 | free movement inflates EU/EFTA figures |
| Ireland | 13.3 | |
| Portugal | 12.9 | |
| Slovenia | 12.4 | |
| **Canada** | **11.7** | |
| Austria | 11.2 | |
| Australia | 8.8 | |
| Germany | 7.0 | |
| UK | 6.3 | |
| France | 4.4 | |
| US | 4.2 | |
| Italy | 2.9 | |
| Japan | 1.4 | |

- **Canada has the highest permanent intake per capita in the G7.** 58% of its standardised permanent inflow is work-related (work plus accompanying family) [calc], against 13% in the US.
- **Gap:** I found no harmonised OECD *emigration*-rate comparison for 2020s (the DIOC tertiary-emigration rates are not machine-accessible here). Canada's emigration rate of about 2.7 per 1,000 is gross and partly reflects re-emigration of immigrants.
- OECD IMO 2025 country note (blocked 403; summary via search) [unverified detail]: the 2025-27 Levels Plan set PR targets of 395,000 / 380,000 / 365,000, and the aim to cut temporary residents to below 5% of population by 2027. Student inflows were down 39% in 2024.

---

## 2. Highly skilled emigration

### 2a. STEM graduates (PSIS–T1FF linkage), `statcan_stem_grad_retention_choi_hou_2025.csv`

**Source:** Choi & Hou, "Retention of STEM graduates in Canada", StatCan *Economic and Social Reports* 5(7), July 23, 2025 [primary] https://www150.statcan.gc.ca/n1/pub/36-28-0001/2025007/article/00004-eng.htm (PDF in raw/pdf).
- The measure is presence in tax data. Non-filers include stayers who do not file, so the measure overstates leaving.
- Universe: bachelor's-or-higher STEM grads of provincially funded institutions, 2010-2020.

**Canadian students (citizens and PRs).**
- STEM grads filing in year 1 / 3 / 5: 90.3 / 87.7 / 85.7% for the 2010-14 cohort; 91.2 / 88.9% (years 1 / 3) for 2015-20. The 2010 cohort was at 83% by year 10.
- The share still filing at year 3 (2015-20 cohort) by group:

| Group | Filing at year 3 |
|---|---|
| Computer & information science | 86.1% |
| Mathematics | 85.9% |
| Engineering | 88.7% |
| Non-STEM | 91.8% |
| Doctoral | 83.4% |
| Bachelor's | 89.1% |
| Master's | 90.1% |
| High-ranking universities | 83.5% |
| Other universities and colleges | 91.1% |
| CS grads, high-ranking universities | 77% (text) |
| CS grads, other institutions | 90% (text) |

- "High-ranking" means the THE 2025 top 100 in engineering, CS or physical sciences: U of T, UBC, Waterloo, McGill, U de M, U of A.
- PR graduates born in the US (77.9% at year 3) and China (81.0%) file at lower rates than citizens (89.4%).

**International students.**
- STEM, 2010-14 cohort: 60.5 / 56.6 / 55.3% (years 1 / 3 / 5).
- STEM, 2015-20 cohort: 67.4 / 63.6% (years 1 / 3).
- Year-3 rates (2015-20 cohort) by group: India 86.4%, US 29.0%, master's 72.2%, PhD 54.2%.
- Retention improved across cohorts, consistent with the growth of post-graduation work permits (PGWPs).
- International students were 11% of STEM bachelor's-plus grads in 2010 (5,550) and 28% in 2021 (24,900).

**Implied attrition [calc] (2015-20 Canadian STEM grads).** About 11% are absent from tax files 3 years out. If the 63,250 Canadian STEM grads of 2021 behaved the same way, that would be about 7,000 non-filers a year. This is an upper bound on leavers.

### 2b. Waterloo: top-tail selection and returns

**Source:** Blit, Skuterud & Zhang (2024), "The Potential of Canada's International Education Strategy: Evidence from the 'MIT of the North'", CLEF WP #74 (forthcoming in *Canadian Public Policy*) [academic] https://clef.uwaterloo.ca/wp-content/uploads/2024/09/CLEF-074-2024.pdf. It links UWaterloo records, including grades, to IRCC and T1 data for 2005-2021 grads. Outmigration means 3 consecutive years of non-filing.

**Outmigration in the first 3 years, by skill percentile** (read from Fig. 6 as stated in the text):

| Group | 5th percentile | 95th percentile |
|---|---|---|
| Canadian-born | 5% | 10% |
| Foreign-born PR | 10% | 16% |
| International students | 40% | 28% |

- The best international students are the most likely to stay. The best Canadian-born and PR students are the most likely to leave, which the authors link to TN visa eligibility.
- Cumulative outmigration peaks at about 20% for the Canadian-born and about 30% for foreign-born PRs.
- There were about 5 times as many Canadian-born grads as international grads, so **the Canadian-born contribute more of the absolute brain drain** at Waterloo.
- **Returns:** within 4-5 years, 16.5% of Canadian-born leavers return, against 13.0% of foreign-born PRs and 4.4% of international students. Self-reports within 10 years: 26.4% (Canadian-born) and 19.9% (foreign-born PR). Return rates are **lower at the top of the skill distribution.**
- About 70% of Waterloo international students transition to PR, twice the national rate.
- There is no evidence that the BlackBerry downsizing (2010-14) raised outmigration.

### 2c. Who emigrates (all ages), `skilled_emigration_indicators.csv`

**Source:** StatCan, "Portrait of Canadian emigration" (infographic 11-627-M, March 18, 2026), from a 2021 Census–T1FF linkage [primary] https://www150.statcan.gc.ca/n1/pub/11-627-m/11-627-m2026013-eng.htm

| | Emigrants | Canadian population |
|---|---|---|
| Aged 20-44 | 67.0% | 31.8% |
| Immigrants | **61.2%** | 23.6% |
| Bachelor's degree | 38.7% | 20.1% |
| Master's or higher | **31.1%** | 8.9% |
| Natural and applied sciences occupations | 22.9% | 7.9% |
| Business/finance occupations | 21.2% | 17.4% |

- Canadian-born living abroad in 2020: 1,292,329, of whom 793,897 (61.4%) were in the US (UN Migrant Stock).
- Caveat: the age base of the population comparison is not stated.

**Immigrant re-emigration (IMDB, The Daily, Feb 2, 2024)** [primary] https://www150.statcan.gc.ca/n1/daily-quotidien/240202/dq240202a-eng.htm
- More than 15% of 1982-2017 immigrants left within 20 years.
- More than 40% of investor-class and 30% of entrepreneur-class immigrants left.
- More than 25% of those born in Taiwan, the US, France, Hong Kong and Lebanon left.

**High earners: Finnie (2006), StatCan 11F0019M no. 288 (LAD 1982-2003)** [primary] https://www150.statcan.gc.ca/n1/pub/11f0019m/11f0019m2006288-eng.htm
- Annual departure rates were 0.045-0.133%.
- "The higher the individual's income, the greater the probability of leaving, especially at the highest income levels ($60,000 to $100,000 and $100,000 and more)", but "the vast majority of leavers are in the lower-income categories".
- About 15% return within 5 years, and return rates are higher at high incomes.
- **Gap:** I found no current (2020s) official study of high-income emigrants, consistent with context §6j.

### 2d. US data on Canadians, `us_acs_canadian_born_education.csv` and `us_admin_flows_canadians.csv`

**ACS 1-year PUMS (my tabulation, weighted)** [calc from primary]. Files: https://www2.census.gov/programs-surveys/acs/data/pums/2024/1-Year/csv_pus.zip and the /2014/ equivalent. The Census API now requires a key, so I tabulated from the microdata.

| Group, aged 25+ | BA+ 2014 | BA+ 2024 | Graduate/professional 2024 | Doctorate 2024 |
|---|---|---|---|---|
| Canada-born | 44.8% | **54.8%** | 25.7% | 4.4% |
| US-native | 30.4% | 37.2% | 14.4% | 1.6% |
| All foreign-born | 28.6% | 35.9% | 16.3% | 2.7% |
| Canada-born who arrived in the last 10 years | 53.0% | 59.1% | n/a | n/a |

- Canada-born population in the US: 933,792 in 2014 and 986,550 in 2024, of whom 861,058 were aged 25+. Excluding US citizens at birth: 826,563 people with 55.1% BA+.
- Canada-born BA+ holders aged 25+ who arrived in 2015-2024: 87,953, or about 8,800 a year still resident [calc].
- Sample sizes: 10,191 (2014) and 11,357 (2024) person records, so expect ±1-2 pt sampling error.

**DHS LPR (green cards) to the Canada-born, FY2013-2022** [primary] https://ohss.dhs.gov/topics/immigration/yearbook/2022/table3

| FY | 2013 | 2014 | 2015 | 2016 | 2017 | 2018 | 2019 | 2020 | 2021 | 2022 |
|---|---|---|---|---|---|---|---|---|---|---|
| Green cards | 13,181 | 11,586 | 12,673 | 12,793 | 11,484 | 9,898 | 11,388 | 9,526 | 9,978 | 11,317 |

**H-1B, FY2023** [primary]. USCIS report (Fig. 5 and App. Tables 4a-c): 3,852 approved petitions for the Canada-born, 1.0% of 386,318. Of these, 1,881 were initial and 1,971 continuing. https://www.uscis.gov/sites/default/files/document/reports/OLA_Signed_H-1B_Characteristics_Congressional_Report_FY2023.pdf

**TN visas: GAP.**
- DHS I-94 tables count admission *events* and exclude most short-term Canadian admissions.
- TN is not broken out by country in Table 32: FY2018 Canada shows H-1B 65,076 events and TN inside "Other" 667,850. https://ohss.dhs.gov/topics/immigration/yearbook/2018/table32
- No usable people-count of Canadian TN holders was found. Do not chart TN admissions as people.

### 2e. Physicians (CIHI), `cihi_physician_international_migration.csv`

**Source:** CIHI, Supply, Distribution and Migration of Physicians in Canada, data tables for 2024 and 2023 [primary]. https://www.cihi.ca/sites/default/files/document/supply-distribution-migration-physicians-in-canada-2024-data-tables-en.xlsx (and the -2023- file)

| Year | Moved abroad | Of which Canadian MD grads | Returned | Of which Canadian MD grads | Net |
|---|---|---|---|---|---|
| 2024 | 109 | 62 | 74 | 44 | **−35** |
| 2023 | 84 | 39 | 98 | 41 | **+14** |

- The stock was 99,555 in 2024; 27.4% were internationally trained.
- Since 2017, abroad status depends on licensing-authority data and self-report, so the counts are likely undercounted.

**History** [primary/academic].
- Early-to-mid 1990s: net losses averaged about 400 a year. Mid-2000s: net gains of 30-60 a year. Source: Watanabe, Comeau & Buske 2008, *Healthcare Policy* 3(4). https://www.longwoods.com/content/19899
- 1996-97: about 450 physicians a year went to the US. Out:in ratios to the US were 19:1 for physicians, 15:1 for nurses and 7:1 for engineers and managers (Zhao et al. 2000).
- **The physician brain drain of the 1990s is essentially over in the official data.** The physician issue today is foreign-credential use (see §3).

---

## 3. Highly skilled immigration

**Education of the stock: Census 2021** (`census2021_degree_by_immigrant_status.csv`, from 98-10-0446-01 via the WDS API; private households) [primary]

| Group, aged 25-64 | Share with BA+ |
|---|---|
| Non-immigrants | 26.8% |
| All immigrants | 45.2% |
| Immigrants landed 2016-21 | **59.4%** |
| Non-permanent residents | 60.8% |
| Total | 32.9% |

**The Daily, Nov 30, 2022** [primary] https://www150.statcan.gc.ca/n1/daily-quotidien/221130/dq221130a-eng.htm
- Immigrants and NPRs were **55.8% of working-age earned-doctorate holders**, 52.2% of master's holders, 50.8% of medicine/dentistry/vet/optometry degree holders, and 39.1% of bachelor's holders.
- 2016-21 immigrants account for 2.1 of the 4.3-pt rise in the BA+ share.

**Over-qualification (same release)** [primary]
- 25.8% of immigrants aged 25-64 with a foreign degree work in jobs needing at most high school. The rate is 10.6% for Canadian-born degree holders and 11.8% for immigrants with a Canadian degree.
- Only 41.1% of immigrants with foreign medical degrees work as doctors, against 90.1% for Canadian medical degrees. The figure for foreign RN degrees is 36.5%.
- Counterpoint for the top end: the Waterloo study finds no evidence of skill under-use once grades and English proficiency are controlled for (§2b).

**International students to PR** [primary]. Choi, Crossman & Hou (2021), *ESR* 1(6). https://www150.statcan.gc.ca/n1/pub/36-28-0001/2021006/article/00002-eng.htm
- About 3 in 10 first-time study-permit holders who arrived in the 2000s became PRs within 10 years.
- The rate was 5 in 10 for master's students and 6 in 10 for PhD students.
- It was 62% for PGWP holders and about 9 in 10 for those earning more than $50k.

**Express Entry (IRCC year-end fact sheets)** [primary]
- 2023: 110,266 invitations to apply (ITAs). 2024: 98,903 ITAs, of which 43,475 (43%) were category-based.
- 2024: 91% of invitees claimed a post-secondary credential of 3+ years. Top occupations were software engineers/designers, food service supervisors, software developers, administrative assistants and secondary teachers.
- 2024 URL: https://www.canada.ca/en/immigration-refugees-citizenship/corporate/publications-manuals/express-entry-year-end-report-2024/fact-sheet-2024.html
- The 2023 count comes via a search summary of the IRCC fact sheet, because canada.ca timed out.
- **Gap:** EE *admissions* (as opposed to ITAs) for 2023-25 were not retrieved.

**Global Talent Stream: GAP.** There is no national count of positions approved. ESDC open-data LMIA files are needed (the open.canada.ca API failed).

**OECD Indicators of Talent Attractiveness 2023** (Migration Policy Debates No. 29 and No. 30, March 2023) [primary]
- PDFs: https://www.oecd.org/content/dam/oecd/en/publications/reports/2023/03/what-is-the-best-country-for-global-talents-in-the-oecd_3496c15f/5186ab2d-en.pdf and the start-up brief (Bertelsmann copy) https://www.bertelsmann-stiftung.de/fileadmin/files/Projekte/Migration_fair_gestalten/Policy-Brief-OECD_OECD-Indicators-of-Talent-Attractiveness-2023_What-are-the-top-OECD-destinations-for-start-up-talents-Migration-Policy-Debates-March-2023_20230315.pdf

| Profile | Canada's position |
|---|---|
| Highly qualified workers | Would be 7th without the visa penalty; dragged down by "relatively long visa processing times (Canada)". The headline rank of 10th is from secondary coverage [unverified rank]. |
| Entrepreneurs | Top 5 (Sweden, Switzerland, Canada, Norway, NZ). Canada lost 1st place after abolishing its federal entrepreneur programme. |
| International students | 6th |
| Start-up founders | **1st** ("most attractive country for start-up founders in the OECD") |

**Policy since then [primary via secondary; verify on canada.ca].**
- Start-Up Visa: new PR applications closed Dec 31, 2025, and the program has been paused since Jan 1, 2026. A "high impact Start-Up Visa pilot" is planned in the IRCC 2026-27 Departmental Plan. Sources: https://immigration.ca/canada-plans-high-impact-start-up-visa-pilot-after-pausing-existing-program/ and https://outboundinvestment.com/canada-to-stop-accepting-new-start-up-visa-suv-permanent-residence-applications-after-december-31-2025/ [context].
- Budget 2025 (Nov 2025): up to $1.7B to recruit 1,000+ international researchers, including a $1B, 13-year accelerated research chairs initiative, plus an announced "accelerated pathway for H-1B holders" [unverified program details]. Source: https://www.cicnews.com/2025/11/canada-to-launch-accelerated-pathway-for-h-1b-holders-as-part-of-suite-of-measures-to-attract-skilled-workers-1161634.html [context].
- Implication: Canada's top OECD ranking for start-up founders is now out of date.

---

## 4. Founders and entrepreneurship (`immigrant_entrepreneurship.csv`)

**StatCan / IRCC research program** [primary]
- **Picot & Rollin (2019)**, StatCan 11F0019M2019011 (CEEDD, 2003-2013). https://www150.statcan.gc.ca/n1/pub/11f0019m/11f0019m2019011-eng.htm
  - Immigrant-owned firms (owners landed since 1980) were **17% of private incorporated firms but created 25% of net new jobs.**
  - The advantage comes from immigrant firms being younger. Adjusted for age and size, there is "no difference".
  - Immigrant-owned firms are more likely to be high-growth in the raw data.
- **Picot & Ostrovsky (2021)**, *ESR* 1(9). https://www150.statcan.gc.ca/n1/pub/36-28-0001/2021009/article/00001-eng.htm
  - Business ownership (incorporated with employees), ages 25-69 in 2016: immigrants 5.2%, second generation 5.0%, third-plus generation 4.0%.
  - Primary self-employment: 6.9%, 5.3% and 4.6% respectively.
  - STEM graduates among incorporated owners: immigrants 22.1%, second generation 12.3%, third-plus 10.0%.
  - Among SMEs, immigrant-owned firms report product innovation at 29% against 27%, and process innovation at 21% against 17%. IP use is similar.
- **Green, Liu, Ostrovsky & Picot (2016)**, 11F0019M no. 375. https://www150.statcan.gc.ca/n1/pub/11f0019m/11f0019m2016375-eng.htm
  - Immigrant-owned incorporated firms are smaller, so **per-capita job creation via incorporated firms is lower** for immigrants.
  - Immigrants' ownership rates surpass the comparison group after 4-8 years in Canada.

**Gap.** I found no StatCan study of immigrant *founders* of VC-backed, high-growth tech firms, or of unicorns.
- [context]: MaRS cites 34.7% of early-stage entrepreneurship by first- and second-generation Canadians: https://www.marsdd.com/our-story/how-immigrant-tech-founders-boost-canadian-innovation/
- [unverified]: a CI Ventures claim that 42% of Canadian unicorns had immigrant co-founders is unsourced. Do not use it.

**Named examples (biographical; Wikipedia / company pages, labelled)**

| Person | Facts | Source | Fits which story |
|---|---|---|---|
| **Tobi Lütke** (Shopify) | Born 1980 in Koblenz, West Germany; moved to Ottawa in 2002; built Snowdevil, which became Shopify | https://en.wikipedia.org/wiki/Tobias_L%C3%BCtke ; Shopify leadership page | Immigrant founder ✔ |
| **Aidan Gomez** (Cohere co-founder and CEO) | Wikipedia says *born in Toronto*, grew up in Brighton, Ont., U of T BSc; co-founded Cohere in 2019 with Nick Frosst and Ivan Zhang | https://en.wikipedia.org/wiki/Aidan_Gomez | **Not an immigrant**; Canadian-born founder who stayed (after Google Brain, Oxford) |
| **Ilya Sutskever** (OpenAI co-founder) | Born 1986 in Nizhny Novgorod; raised in Jerusalem; moved to Canada at 16; U of T BSc 2005, MSc 2007, PhD 2013; went to Google in 2013 via the DNNResearch acquisition, then OpenAI in 2015 | https://en.wikipedia.org/wiki/Ilya_Sutskever ; U of T alumni page https://entrepreneurship.artsci.utoronto.ca/node/847 | Immigrant trained in Canada, then lost |
| **Garrett Camp** (Uber co-founder, StumbleUpon) | Born in Calgary in 1978; U of Calgary EE 2001 | https://en.wikipedia.org/wiki/Garrett_Camp | Canadian-born, Canadian-trained, built in the US |
| **Stewart Butterfield** (Flickr, Slack) | Born in Lund, BC, in 1973; co-founded Flickr and Slack (company locations not verified here) | https://en.wikipedia.org/wiki/Stewart_Butterfield | Canadian-born founder (whether this counts as US-built: verify) |
| **Michael Katchen** (Wealthsimple) | Western (Ivey) grad; worked in San Francisco (1000memories) before founding Wealthsimple in Toronto in 2014 | https://www.ivey.uwo.ca/alumni/intouch-archive/2017/3/wealth-management-simplified/ | Returner / circulation. Birthplace **not verified** |

- Anecdotes cut both ways. Use them as illustration only.

---

## 5. Net assessment

**1990s baseline: Zhao, Drew & Murray** [primary]. "Knowledge workers on the move", *Perspectives on Labour and Income*, Summer 2000, adapted from *Education Quarterly Review* 6(3). https://www150.statcan.gc.ca/n1/pub/75-001-x/2000002/article/5072-eng.pdf
- "Is there a brain drain to the United States? Yes", but losses were about 0.1% of people with employment income, and under 1% of any knowledge occupation's stock.
- About 39,000 degree holders entered Canada each year in 1990-96, including 11,000 master's/PhDs. About 10,000 a year left for the US. "Canada gains four times as many university graduates from the rest of the world as it loses to the United States."
- "As many immigrants enter Canada with a master's or doctorate as university graduates at all levels leave for the United States."
- Of 1995 grads, 1.5% were in the US in 1997, but **12% of doctoral grads** were. Movers were high achievers, and about 18% had returned by 1999.
- Recent immigrants made up almost one-third of 1991-96 growth in computer professional employment.

**A modern analogue (`net_skill_balance_estimates.csv`)** [calc, rough; state assumptions in the essay]
- Gross degree-holding emigrants: about 62,000 a year in 2016-21 (88,791 emigrants × 69.8% degree share from the 2021 Portrait). About 61% of emigrants are immigrants.
- Recent immigrants aged 25-64 with BA+ still resident in 2021: 535,635, or about 99,900 a year over 5.36 years. These are survivors, net of early re-emigration, and exclude 335,825 degree-holding NPRs.
- Gross ratio: about **1.6 : 1**. If returning emigrants (52,067 a year) had the same education as leavers, which is **unknown**, net degree emigration would be about 25,600 a year and the ratio about **3.9 : 1**.
- Canada-born BA+ holders settling in the US: about 8,800 a year (ACS 2024, 2015-24 arrivals). That is roughly one-eleventh of the immigrant degree inflow.
- **Interpretation.** On *counts* of degrees, Canada remains a clear net gainer, as in the 1990s.
- **Caveat on quality.** Degree counts are not skill-weighted. Foreign degrees earn less and are under-used (25.8% over-qualification), while leavers are positively selected within the Canadian-born: top universities, PhDs, top Waterloo students, high incomes. A skill- or earnings-weighted balance would be much narrower than 4 to 1. **No study I found computes it.**

**Where the top differs from the average** [primary/academic]
- Average STEM grad attrition is small, at about 11% by year 3, and has improved over time.
- At the top it is 2-3 times larger:
  - CS grads of top-ranked universities: 23% absent by year 3.
  - Canadian PhDs: 16.6% absent by year 3 (2015-20 cohort) and 23.8% by year 5 (2010-14 cohort).
  - Top-5% Waterloo students: Canadian-born leave at twice the bottom-5% rate and are less likely to return.
- The 1990s pattern holds: PhDs were 12% in the US two years out, against 1.5% of all grads.

**Fiscal cost**
- **No credible current estimate found.**
- [unverified, secondary] DeVoretz & Laryea (C.D. Howe, 1998) put the human-capital value of professionals and managers lost to the US, net of US arrivals, at about $6.6-6.7B for 1982-96. The C.D. Howe PDF URL is now dead; this was seen only in a search summary. Label it as dated think-tank context, or omit it.
- [calc idea, not done] A defensible approach would be per-student public operating grants × attrition counts by level. Not computed: no reliable per-graduate subsidy series was collected.

---

## 6. AI talent (`ai_talent_indicators.csv`)

**Pan-Canadian AI Strategy (launched 2017 as "the first national AI strategy in the world")** [context: CIFAR self-report] https://cifar.ca/ai/ and https://cifar.ca/ai/impact/
- More than 130 researchers have been recruited or retained through the Canada CIFAR AI Chairs at Amii, Mila and Vector.
- CIFAR gives no figures on the share recruited from abroad or on graduate retention. **Gap.**

**Stanford AI Index 2025, ch. 4 (LinkedIn and Quid data)** [primary-ish, academic compilation] https://hai.stanford.edu/assets/files/hai_ai-index-report-2025_chapter4_final.pdf
- **Net AI talent migration, Canada: +0.95 per 10,000 LinkedIn members in 2024.** It was positive, but down from a peak of about 2.4 in 2022-23 (read from the Fig. 4.2.23 chart). The report names Canada among countries with "declining net AI talent migration".
- For comparison: US +1.07, Germany +2.13, Israel −2.10, India −1.55.
- Newly funded AI companies: 51 in 2024 (6th). Over 2013-24, Canada had 481 (5th), against 6,956 in the US.
- AI private investment: US$2.89B in 2024 and US$15.31B over 2013-24.
- [unverified] Secondary summaries say the AI Index 2026 shows Canada *negative* in 2025, and that BCG's LinkedIn-based tracker has Canada falling in AI-talent attractiveness. Not checked; the 2026 PDF was not retrievable.

**MacroPolo Global AI Talent Tracker 2.0 (2023)** [context, think tank]. The page is JS-rendered and its Canada figures were not extracted. The Paulson Institute summary says Canada and India saw relative declines in their share of top-tier AI researchers by undergraduate origin. **Gap:** Canada-specific numbers.

**PhD and AI-adjacent retention.** Choi & Hou give math and CS grads at top-ranked universities as the least-retained Canadian groups (§2a). There is no AI-specific StatCan series.

**Illustrative case.** Sutskever: immigrant, U of T-trained (BSc, MSc, PhD), left for Google and OpenAI in the US (§4). The deep-learning lineage at U of T, Montréal and Alberta trained people who now run labs in both countries.

---

## Gaps and cautions (read before charting)

1. **The 2016 methodology break** in emigrant and returning-emigrant counts. Chart net emigration, or split the series at 2016.
2. **Tax non-filing is a proxy for leaving.** It overstates emigration, especially for young people and international students.
3. **No people-count of Canadians on TN visas.** Only H-1B (by birth country) and LPR counts exist.
4. **Education of returning emigrants is unknown.** This is the biggest hole in any modern "net degree" calculation.
5. **No skill-weighted or earnings-weighted net balance study** was found, nor any current fiscal-cost estimate.
6. **Not retrieved:** the OECD IMO 2025 Canada note and DIOC tertiary emigration rates (OECD site returned 403); Express Entry admissions; Global Talent Stream volumes; MacroPolo Canada shares; AI Index 2026.
7. **Founder examples are biographical** (Wikipedia-level). There is no systematic dataset on the birthplace of Canadian high-growth founders.

---

## Files (all in this directory)

| File | Contents |
|---|---|
| `notes.md` | These notes |
| `canada_talent_flows_2000_2025.csv` | year × series (immigrants, emigrants, returning, net temporary emigration to 2016, net emigration, net NPR, population, rates per 1,000, NPR stock) |
| `oecd_permanent_inflows_per1000.csv` | OECD standardised permanent inflows by country, 2015-2024, per 1,000 and work share |
| `statcan_stem_grad_retention_choi_hou_2025.csv` | Full Table 1, tidy (status × cohort × breakdown × group × year) |
| `skilled_emigration_indicators.csv` | Emigrant profile (Portrait 2026), immigrant re-emigration, Waterloo study, Zhao et al. 1990s, Finnie |
| `us_acs_canadian_born_education.csv` | ACS PUMS 2014 and 2024: Canada-born in the US by education, against US-native and foreign-born |
| `us_admin_flows_canadians.csv` | DHS LPR by birth (Canada) FY2013-22; H-1B FY2023; I-94 caveat row |
| `cihi_physician_international_migration.csv` | Physicians moved abroad, returned and net, 2023-2024, plus 1990s and 2000s history |
| `census2021_degree_by_immigrant_status.csv` | BA+ shares and counts, ages 25-64, by immigrant status (2021) |
| `immigrant_entrepreneurship.csv` | StatCan ownership, jobs, STEM and innovation indicators, plus one context row |
| `ai_talent_indicators.csv` | AI Index 2025 Canada metrics and CIFAR chairs |
| `net_skill_balance_estimates.csv` | 1990s ratio and the modern rough analogue with derivations |
| `scripts/` | flows.py, acs_canborn*.py, acs_tidy.py, oecd_perm.py, choi_hou_table.py, build_misc.py, build_more.py, h2t.py |
| `raw/` | StatCan zips/CSVs, PDFs (Choi & Hou, CLEF WP74, Zhao et al., OECD ITA, USCIS H-1B, Card 2003), CIHI xlsx, ACS aggregates (the PUMS zips were deleted after tabulation), AI Index ch. 4 |
