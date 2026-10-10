# Intergenerational mobility: Canada vs US, a stress test

Research date: 2026-10-10. Every figure below was read from the primary PDF, HTML table or StatLink spreadsheet saved in `raw/` unless it is marked **[secondary]** or **[unverified]**. Status labels: **[peer-reviewed]**, **[StatCan]**, **[OECD]**, **[working paper]**.

The claim under test is the series line: "Canada has better (upward) intergenerational mobility than the US: bottom-to-top-quintile probability 11.2% vs 7.5%; rank-rank slope ~0.21–0.23 vs 0.34 (Connolly, Corak & Haeck 2019)."

**Short answer.** The relative-mobility claim holds up. The 11.2% vs 7.5% "rags to riches" comparison is the weakest way to make it, because those two numbers use different yardsticks. The Canadian advantage is real but concentrated in the bottom half of the distribution. It has been shrinking across cohorts, and it does not hold for Indigenous children, for Manitoba and the North, or at the very top.

---

## 1. Headline Canada–US comparison (Connolly, Corak & Haeck 2019)

**Source.** Connolly, M., Corak, M. & Haeck, C. (2019), "Intergenerational Mobility between and within Canada and the United States," *Journal of Labor Economics* 37(S2): S595–S641, doi:10.1086/703465 [peer-reviewed]. The published version is posted on Corak's site: https://milescorak.com/wp-content/uploads/2020/10/connolly-corak-haeck-2019-intergenerational-mobility-canada-us-jole.pdf (NBER WP 25735: https://www.nber.org/papers/w25735). The JOLE Tables 2 and 3 are identical to the NBER WP.

**Data and design.** Canada uses the IID (tax data). The cohort is children born in 1980 and 1982, matching Chetty et al. (2014)'s 1980–82 US cohort; StatCan's file has no 1981 cohort. Parent family income is averaged over 1996–2000. Child family income is averaged over 2011–2012, at about ages 30–32. Non-filers are set to zero, and common-law partners are treated as married. Canadian incomes are converted to US$ at the 2012 PPP rate of 1.28. The US figures come straight from Chetty et al. (2014).

### Table 2 (JOLE p. S611): exact figures
| Spec | US | Canada |
|---|---|---|
| IGE, log family income | 0.344 | 0.310 |
| IGE, incomes <$500 excluded | – | 0.226 |
| IGE, zero recoded to $1 | 0.618 | 0.474 |
| IGE, <$1,000 recoded to $1,000 | 0.413 | 0.294 |
| Rank-rank, percentile in US distribution | 0.341 | 0.229 |
| Rank-rank, own-country percentiles | 0.341 | **0.212** |
| Rank-rank, own country, excl. missing | – | 0.228 |
| Rank-rank, parents 1999–2003 | 0.339 | 0.216 |
| Rank-rank, top-earning parent | 0.312 | 0.219 |

The authors say all differences are statistically significant, with a maximum SE of 0.006. The series' "0.21–0.23 vs 0.34" is correct. The paper's own wording is "21 to 23 percentiles" vs "31 to 34".

### Table 3 (JOLE p. S613): quintile transition matrices (child quintile | parent quintile, %)
- **US, US distribution:** bottom→top **7.5**, bottom→bottom 33.7, top→top 36.5, top→bottom 10.9
- **Canada, Canadian distribution:** bottom→top **11.2**, bottom→bottom 32.0, top→top 29.5, top→bottom 15.5
- **Canada, placed in the US distribution:** bottom→top **7.7**, bottom→bottom 29.1, top→top 24.6

The full matrices are in `cch2019_quintile_transition_matrices.csv`.

### Stress-test findings
1. **11.2% vs 7.5% is not like-for-like.** Each country's children are ranked in their own national distribution. The two income distributions differ: Canadian *parents* are far less spread out at the top (parents' p80 is US$87,972 in Canada vs US$107,900 in the US; p99 is US$242,279 vs US$420,100), while Canadian *children's* middle incomes are higher (children's p50 is US$44,663 vs US$34,600) (JOLE Table 1, PPP US$). When Canadians are ranked on the **US yardstick**, bottom→top is **7.7% vs 7.5%**, which is effectively the same. The paper's own words are that low-income Canadian children are "much more likely to rise to the top half... although not necessarily more likely to rise to the top fifth" (p. S614). **Recommendation:** stop leading with 11.2 vs 7.5, or always pair it with 7.7 vs 7.5 on the common yardstick.
2. **The robust part is the bottom and middle of the distribution.** On the common US yardstick, bottom-quintile Canadian children are less likely to stay in the bottom (29.1% vs 33.7%). Their expected rank is "somewhat above the 40th percentile" in Canada and "significantly below" it in the US (p. S612). The population-weighted average of absolute rank mobility across regions is **40.6 in Canada vs 35.8 in the US** (p. S625).
3. **A puzzle the series should not ignore.** The same page reports a population-weighted regional average of rags-to-riches, using US cutoffs, of **10% in the US vs 8.54% in Canada** (p. S625). On that metric Canada is lower. Bottom→bottom is 30.7% US vs 26.6% Canada (fn 21). The paper does not reconcile these regional averages with the national 7.5/7.7. The likely cause is weighting by total population rather than by children, but I have not verified that. On the US yardstick, top-quintile mobility is a wash or slightly worse in Canada.
4. **Relative rank mobility is robust.** The rank-rank slope gap of about 0.11–0.13 survives every specification in Table 2. The log-IGE gap is much smaller in the baseline (0.310 vs 0.344) and larger only under bottom-coding choices.
5. **Method caveats.** Both countries use tax data, which the authors call "probably... more accurate... than any country-wide comparison... to date." Some asymmetries remain:
   - Ages 30–32 are early in the life cycle. The authors note rank measures are fairly stable by then, and Corak (2020) gets 0.240 at 31–32 vs 0.242 at 38–45 for the 1963–70 cohort.
   - The US data capture non-filers through W-2s. Canadian non-filers are set to zero.
   - The Canadian parent-child link requires the teen to have a SIN while living at home.
   - Child incomes were measured in 2011–12, during the oil, potash and commodity boom. The most mobile cluster runs through Alberta, Saskatchewan and Newfoundland & Labrador, and the authors flag the boom (p. S622). Corak (2020, fn 18) also warns that boom-era incomes may overstate permanent income in those census divisions.
   - The rank-rank relationship is less linear in Canada, with a steeper slope at the bottom (p. S613), so a single slope summarizes Canada less well.
6. **The authors' own decomposition.** The national gap is partly a composition effect. The low-mobility cluster 4 is overwhelmingly American: 285 US commuting zones with 2.93M sample children, vs 29 Canadian CDs with 28k children (Tables 4–5). The Quebec City–Windsor corridor does look more mobile than the US Northeast and Great Lakes across the border. See section 3.

### Older elasticity estimates (for context)
- The comparability-adjusted father-son earnings IGE is **Canada 0.19 (range 0.16–0.21) vs US 0.47 (0.40–0.52)**. Source: Corak (2004b) Table A-1, reproduced in Aydemir, Chen & Corak (2005), StatCan 11F0019MIE No. 267, Table 1, https://www150.statcan.gc.ca/n1/en/pub/11f0019m/11f0019m2005267-eng.pdf [StatCan]. Corak (2013, JEP; IZA DP 7520 text) says the US IGE "at about 0.5, is twice as high as the Canadian."
- **The 0.19 is biased downward.** Chen, Ostrovsky & Piraino (2016/2017) measure lifetime earnings (fathers 35–55, sons 38–42) and get a father-son IGE of **0.318** (market income 0.343, total income 0.359). Re-running the Corak–Heisz design on the same data gives 0.227. About two-thirds of the gap comes from life-cycle bias. StatCan says the "comparable figure for the United States... is around 40% to 50%." Source: https://www150.statcan.gc.ca/n1/pub/11-626-x/11-626-x2016059-eng.htm [StatCan]; *Labour Economics* 44 (2017) [peer-reviewed]. **Implication:** "Canada ≈ Nordic at 0.19" is outdated. A defensible current statement is "about 0.32 vs about 0.4–0.5."

---

## 2. Trends over time

### Relative mobility in Canada is declining across cohorts
Source: Connolly, Haeck & Lapierre (2021), "Trends in Intergenerational Income Mobility and Income Inequality in Canada," StatCan 11F0019M No. 458, https://www150.statcan.gc.ca/n1/pub/11f0019m/11f0019m2021001-eng.htm [StatCan]. The data are the IID, with five cohorts born 1963–1985 and about 1.5M children per cohort (weighted). Child individual pre-tax income is averaged over ages 25–29. Parent family income is averaged over child ages 15–19.

| Birth cohort | 1963–66 | 1967–70 | 1972–75 | 1977–80 | 1982–85 |
|---|---|---|---|---|---|
| Rank-rank slope (25–29) | 0.189 | 0.188 | 0.216 | 0.215 | **0.234** |
| Rank-rank (30–34) | 0.201 | 0.214 | 0.232 | 0.235 | … |
| IGE (25–29) | 0.153 | 0.154 | 0.199 | 0.210 | 0.224 |
| P(bottom→bottom) | 0.27 | 0.28 | 0.31 | 0.31 | **0.33** |
| P(bottom→top) | 0.13 | 0.13 | 0.12 | 0.13 | 0.12 |
| P(bottom→middle 3) | 0.60 | 0.59 | 0.57 | 0.56 | 0.56 |
| Parental pre-tax Gini | 36.36 | 37.77 | 39.27 | 41.18 | 44.37 |

(Table 3 and the chart data tables 4–6; Table 2 for the Gini.)

- The slope rose by **24%**. Bottom→bottom rose from 27.1% to 32.6% (text). Bottom→top is flat. The deterioration is a growing "poverty trap": fewer children from the bottom reach the middle class.
- **Every province** deteriorated except Yukon (section 3). The authors describe Canada as "going up the Great Gatsby Curve."
- Journal version: Connolly & Haeck (2024), *Canadian Journal of Economics* 57(1), doi:10.1111/caje.12699 [peer-reviewed]. The abstract confirms the decline nationally and provincially. A secondary summary (vlex) quotes rank-rank rising "from 0.202 to 0.245" (1963 to 1985 cohorts) **[secondary, unverified; CJE full text not accessed]**.

### The US is flat
Chetty, Hendren, Kline, Saez & Turner (2014), NBER WP 19844 / *AER P&P*: "percentile rank-based measures of intergenerational mobility have remained extremely stable for the 1971-1993 birth cohorts." Rank-rank slopes are "all approximately 0.30" for the 1971–74, 1975–78 and 1979–82 cohorts (child ages 29–30). Bottom→top was 8.4% for the 1971 cohort and 9% for 1986 (age 26). Source: https://eml.berkeley.edu/~saez/chettyetalAEAPPNBER14.pdf [peer-reviewed/WP].

**Implication: the gap is narrowing.** Canada's slope rose by about 0.045 over roughly 20 cohorts while the US stayed flat. The two series use different income definitions and ages, so the levels should not be directly compared.

### A warning sign: Canadian-born children
Adnan, Zhang & Zheng (2023, IZA DP 16471) report a rank-rank slope of **0.286 for Canadian-born children** (cohorts 1975–85, IID–Census link). They compare it with **0.223 for immigrant children** (fn 25/35). The Canadian-born figure was provided privately by M. Connolly and is not in a published table **[working paper; treat as indicative]**. The definitions differ from CCH, so it cannot be set directly against the US 0.34. It does suggest that **part of Canada's flatter national slope reflects its large, highly mobile immigrant second generation.**

### Absolute mobility (share earning more than parents at the same age)
- **US:** "approximately 90%" for the 1940 cohort, falling to "50%" for children born in the 1980s. Source: Chetty, Grusky, Hell, Hendren, Manduca & Narang (2017), *Science* 356:398–406 [peer-reviewed].
- **Canada** (Ostrovsky 2017, StatCan 11-626-X No. 073, https://www150.statcan.gc.ca/n1/pub/11-626-x/11-626-x2017073-eng.htm) [StatCan], at age 30:
  - Adjusted family income: 59% (1970 cohort), 67% (1977), 64% (1984). The figure is stable apart from cyclical swings.
  - Unadjusted: 48%, 55%, 53%.
  - StatCan says the 1970 cohort was "somewhat lower in Canada than in the United States." The US 1970 figures are 62% unadjusted and 73% adjusted (fn 7). By the 1984 cohort, rates "were similar in Canada and the United States when calculated using either income definition."
- **Harmonized cross-country estimate:** Manduca et al. (2024), *AEJ: Applied* 16(2), as charted in StatCan ESR 36-28-0001 v3 no.12 (Dec 2023), https://www150.statcan.gc.ca/n1/pub/36-28-0001/2023012/article/00001-eng.htm [StatCan citing peer-reviewed]. For the 1985 cohort at age 30: **Canada 57%, US 50%**, Finland 57%, UK 65%, Norway 74%.
  - The 2020 IZA working-paper version (DP 13456) says "the US and Canada saw upward mobility rates near 50% for recent cohorts" and "Only Canada and Denmark have recent upward mobility rates comparable to those in the United States."
- **Verdict on absolute mobility:** Canada did not suffer a US-style collapse; its rate was flat for the 1970–84 cohorts. Its level is now only modestly above the US (+7 pp in the harmonized estimate, "similar" in StatCan 2017) and well below the Nordics and UK. Manduca et al. attribute cross-country differences mainly to the income distributions, meaning growth and inequality, rather than to relative mobility.

---

## 3. Geography

### By province, cohort trend (StatCan 2021, Chart 3 data table; within-province ranks)
The data are in `canada_rank_slope_by_province_cohort.csv`.

| Province | 1963–66 cohort | 1982–85 cohort |
|---|---|---|
| Saskatchewan | 0.175 | **0.331** (+89%, the largest deterioration) |
| Manitoba | 0.269 | **0.315** (lowest mobility of the 10 provinces) |
| Nunavut + NWT | 0.336 | 0.350 |
| New Brunswick | 0.196 | 0.244 |
| BC | 0.153 | 0.233 |
| Ontario | 0.168 | 0.226 |
| Alberta | 0.156 | 0.226 |
| Quebec | 0.184 | 0.227 |
| Nova Scotia | 0.180 | 0.217 |
| Newfoundland and Labrador | 0.196 | 0.208 |
| Yukon | 0.324 | 0.197 (the only decrease; small population) |
| PEI | 0.157 | 0.156 |

### By province, rich indicator set, 1963–70 cohort (Corak 2020)
Source: Corak (2020), "The Canadian Geography of Intergenerational Income Mobility," *Economic Journal* 130(631): 2134–2174, https://milescorak.com/wp-content/uploads/2020/10/corak-2020-economic-journal.pdf [peer-reviewed]. Tables 4–6 cover 9 indicators by province. The data are in `canada_mobility_by_province_corak2020.csv`.
- **Canada:** IGE 0.201, rank-rank 0.242 (ages 38–45), P15 0.114, P11 0.301, P55 0.323, share above parents 0.625.
- **Manitoba is an outlier:** IGE 0.341, rank-rank 0.325, P11 **0.414**.
- **Atlantic Canada has flat slopes but low upward mobility.** Rags-to-riches is NB **6.1%**, NS 7.1% and PEI 7.7%, below the US national 7.5%. P11 is NB 35.2% and NS 35.0%, worse than the US 33.7% (different cohort and data, so indicative only). A low slope is not the same as high opportunity.
- **High-P15 provinces:** Alberta 18.5%, Ontario 14.1%, Saskatchewan 14.1%. These partly reflect the boom (fn 18). York Region accounts for 42.3% of the children in the high upward-mobility CDs.
- Across the 266 CDs:
  - 23 CDs have P11 ≥ 0.40, covering 2% of children; they are small communities.
  - Only 7 CDs have P11 < 0.20.
  - Nine CDs have P15 ≥ 0.20, covering 3.1% of children.

### Canada–US regions (CCH 2019 Tables 4–5, k-means on about 1,000 regions)
The data are in `cch2019_regional_clusters.csv`.
- **Cluster 4 (lowest mobility):** abs. rank 29.6, slope 0.376, P15 5.9%, P11 36.0%.
  - US: 285 commuting zones, 2.93M children.
  - Canada: 29 CDs, 28k children.
  - It groups the US South with **northern Canada**.
- **Cluster 1 (highest):** abs. rank 46.3, slope 0.237, P15 16.0%. It spans the US Midwest, Saskatchewan, Alberta and parts of NL. Canada has 40 CDs and 103k children in it.
- The Canadian standard deviation of regional rank-rank slopes, **0.113**, is higher than the US figure of **0.0648** (CCH 2019 text, section on between- and within-country comparisons). Canada is more heterogeneous internally on this metric.
- **Counterfactual excluding cluster 4:**
  - US abs. rank mobility rises from 35.8 to 39.6; Canada's only from 40.6 to 41.4.
  - US P15 rises from 10% to 12.4%; Canada's from 8.54% to 8.59%.
  - Read literally, the US outside its low-mobility South is roughly on par with Canada on absolute measures.

**Data gap.** I found no official StatCan data table (CODR/CANSIM) of intergenerational mobility by province or CD. Tables 11-10-0059 and 11-10-0061 are *within-lifetime* mobility. Corak (2020) has an online appendix with CD-level data that I did not retrieve; per the paper, it is at the EJ site.

---

## 4. Groups

### Children of immigrants: very high mobility, and it flatters the national figure
- Adnan, Zhang & Zheng (2023), IZA DP 16471, https://docs.iza.org/dp16471.pdf [working paper]. They link landing files to tax data.
  - Immigrant children: rank-rank 0.223 (refugees 0.263, non-refugees 0.205).
  - Expected rank at parental p25: refugees 47, non-refugees 51, **Canadian-born 43.7**.
  - The US comparator is immigrant sons' slope of 0.21 (Abramitzky et al. 2021, as cited).
- Aydemir, Chen & Corak (2005; *REStat* 91(2) 2009). From the 2001 Census, the best estimate of the father-son weekly earnings IGE for second-generation sons is **0.27**, "no different than for the population at large," and "possibly about 50% lower" than the US [StatCan/peer-reviewed].
- **Caveat.** Children of immigrants are highly mobile in both countries. Canada's larger immigrant share mechanically flattens its national slope.
- I did not locate a specific Hou & Bonikowska paper on intergenerational *income* mobility of children of immigrants **[gap]**.

### Indigenous children: much lower mobility
- Connolly & Haeck (2026), "The Great Mobility Puzzle Resolved," *Canadian Public Policy* 52(1), doi:10.3138/cpp.2026-004 [peer-reviewed; abstract only via Crossref, full text paywalled].
  - They use the IID–Census link.
  - The steeper Canadian rank-rank slope below parental p20 "is almost entirely attributable to the very low adult incomes of Indigenous children raised in low-income families."
  - Mobility is "**about two times lower**" for Indigenous-background children.
  - The on-reserve income tax exemption may partly depress *measured* income.
  - No numeric slopes were available to me **[gap: obtain full text]**.
- CCH 2019 Table 7, Canadian CDs: the correlation of the Indigenous population share with the rank-rank slope is **0.520** (SE 0.051), and with absolute rank mobility **−0.567**. In the US the corresponding correlation is 0.022. Indigenous share is to Canada's geography of low mobility what the Black population share is to the US's (US correlation with Black share: 0.631).

### Top-end persistence (the "rich stay rich" result)
- Corak & Heisz (1998 WP; *JHR* 34(3) 1999), sons born 1963–66, about 400,000 father-son pairs. The earnings elasticity is about 0.0 at the bottom percentile and rises to about 0.4 at the top percentile. "The intergenerational elasticity between father and son incomes at the top percentile is **almost 0.8**." Source: https://econwpa.ub.uni-muenchen.de/econ-wp/lab/papers/9808/9808001.pdf [peer-reviewed; numbers from the WP version].
- Chen, Ostrovsky & Piraino (2016), with lifetime measures: the quantile IGE is 0.07 at father p1, 0.27 at p10, 0.37 at p50, 0.43 at p90 and **0.45 at p95–99**. Persistence at the top is "nearly half" [StatCan].
- Corak & Piraino (2011, *JOLE*), as cited in Corak (2013): "Almost seven out of ten Canadian sons born to top 1 percent fathers" had worked for an employer that had employed their father, vs about 4 in 10 overall. Corak (2013) describes Canada as combining "high mobility for most" with a "**dynasty** for the top 1 percent."
- **Fairness check.** At the *quintile* level, Canada's top is *less* sticky than the US: top→top is 29.5% vs 36.5% (CCH Table 3). No directly comparable US top-1% income elasticity from the same design was found. The top-end result shows that Canada's advantage is a bottom-half story, not that Canada is worse than the US at the top.

### Wealth and housing
- **No Canadian study of the intergenerational wealth (net-worth) correlation was found [gap].** The IID has no wealth data.
- Housing (StatCan, Canadian Housing Statistics Program, 2021, excluding QC and SK). Source: Mirdamadi & Khalid (2023), https://www150.statcan.gc.ca/n1/pub/46-28-0001/2023001/article/00004-eng.htm.
  - People born in the 1990s whose parents own a home are "twice as likely" to own one themselves: 17.4% vs 8.1%, or 23.8% if the parents own multiple properties.
  - For people born 1990–92: 15.7% (non-owner parents) vs 31.7% (owner parents).
  - 52.8% of 1990s-born multiple-property owners have parents who own multiple properties.
- Khalid, Gordon & Mirdamadi (2024), https://www150.statcan.gc.ca/n1/pub/46-28-0001/2024001/article/00002-eng.htm: 17.3% of properties owned by 1990s-born owners are co-owned with parents [StatCan]. The data are in `canada_homeownership_by_parent_properties.csv`.
- The user mentioned a "StatCan 2025 familial support in housing" release; I did not find one. The 2023–24 series above is the nearest match **[gap]**.

---

## 5. OECD (2018) *A Broken Social Elevator?*

Source: OECD (2018), doi:10.1787/9789264301085-en. Values are read from the StatLink spreadsheets [OECD]. The data are in `oecd_broken_elevator_2018.csv`.
- **Generations for a bottom-decile family to reach mean income** (Fig 1.5, StatLink http://dx.doi.org/10.1787/888933761910): **Canada 4**, **US 5**, OECD24 average **4.5**, Denmark 2, Norway/Finland/Sweden 3, France/Germany 6, Colombia 11. **Correction for the series:** Canada is 4, not 4.5; the 4.5 is the OECD average. The OECD calls these "simulation-based and intended to be illustrative."
- **Father-son earnings elasticity** (Fig 4.8, StatLink http://dx.doi.org/10.1787/888933754253): **Canada 0.32**, **US 0.41**, OECD26 0.38, Denmark 0.12. The methods differ: Canada uses Chen et al. (2017) admin data; the US uses an OECD estimate from the PSID; many other countries use two-sample 2SLS.
- OECD text: earnings mobility is "relatively well" in Canada and New Zealand. Within-lifetime income persistence "has increased... more at the bottom in Canada, Finland, Italy and Portugal" (p. 33).

---

## 6. Verdict

**Is "Canada has better intergenerational mobility than the US" supported?** Yes for **relative mobility**, and modestly for **absolute mobility** of low-income children. With care, the series can keep the claim.

| Dimension | Canada better? | Evidence |
|---|---|---|
| Rank-rank slope (relative mobility) | **Yes, robust** | 0.21–0.23 vs 0.34 under 6 specs, both from tax data |
| Expected rank of low-income kids on a common yardstick | **Yes** | abs. rank 40.6 vs 35.8; P11 29.1 vs 33.7 on the US yardstick |
| Bottom→top quintile ("rags to riches") | **Not on a common yardstick** | 11.2 vs 7.5 uses own-country quintiles; 7.7 vs 7.5 on US quintiles; regional pop-weighted 8.54 vs 10.0 |
| Share out-earning parents | **Slightly** | 57 vs 50 (1985 cohort, Manduca et al.); StatCan 2017 calls them "similar" for the 1984 cohort |
| Father-son IGE | **Yes, but smaller than the old 0.19 vs 0.47** | lifetime 0.32 (CA) vs about 0.40–0.50 (US) |
| Trend | **Canada worsening, US flat** | CA slope 0.189→0.234, P11 0.27→0.33; US slope about 0.30 throughout |
| Top-end persistence | **Mixed** | quintile top→top 29.5 vs 36.5 (CA better); but top-percentile income IGE about 0.8, and 7/10 top-1% sons work for their father's employer |
| Indigenous children | **No** | mobility "about two times lower"; Indigenous share is the strongest CD-level correlate of low mobility |
| Regions | **Not everywhere** | Manitoba, northern CDs and Saskatchewan (recent cohorts) are US-South-like; Atlantic provinces have flat slopes but P15 below the US |
| Composition | **Caution** | Canadian-born slope 0.286 vs immigrant children 0.223 (WP); the immigrant second generation flatters the national number |

**Suggested wording for the series:**
> "On the best like-for-like tax data, a Canadian child's income rank depends about a third less on their parents' rank than an American child's does (rank-rank slope ≈0.21–0.23 vs 0.34; Connolly, Corak & Haeck 2019). That advantage sits mainly in the bottom half: low-income Canadian kids are more likely to reach the middle class, though not more likely to reach the top fifth when both countries are measured on the same US dollar yardstick (7.7% vs 7.5%). The gap is narrowing. Canadian mobility has declined steadily for children born 1963–1985 while US mobility held flat. It is far weaker for Indigenous children, for Manitoba and the North, and at the very top, where Canadian income advantage is passed on almost entirely."

**Flags and gaps**
- The CJE 2024 numbers (0.202→0.245) and the Connolly & Haeck 2026 CPP numeric results were not read in full text.
- The Canadian-born 0.286 slope is a private communication cited in an IZA working paper.
- There is no StatCan CODR table for intergenerational mobility. Provincial series come from the 2021 research paper's chart data tables.
- The newest IID cohort data, with tax years after 2014, appear in no publication I could find. The search indicated a 2024 IID update adding a 2006 cohort **[unverified]**.
- No Canadian intergenerational wealth-correlation estimate was found.
- The Hou & Bonikowska immigrant-mobility paper was not located.
- The CCH regional rags-to-riches averages (8.54 vs 10.0) conflict with the national 7.7 vs 7.5, and the paper does not explain why.

---

## File list
| File | Contents |
|---|---|
| `canada_us_headline_measures.csv` | 39 Canada/US measures with table refs (CCH 2019 T2/T3/text, Corak IGE, Chen et al., OECD, absolute mobility) |
| `cch2019_quintile_transition_matrices.csv` | 3 panels × 25 cells (US; Canada own-distribution; Canada in US distribution) |
| `cch2019_regional_clusters.csv` | k-means clusters (2 and 4), mobility stats, Canada/US region and child counts |
| `canada_relative_mobility_by_cohort.csv` | StatCan 2021: rank-rank and IGE by cohort, age and sex; P11/P15/P(middle) by parent quintile; parental Gini |
| `canada_rank_slope_by_province_cohort.csv` | StatCan 2021 Chart 3: 12 jurisdictions × 5 cohorts |
| `canada_mobility_by_province_corak2020.csv` | Corak 2020 EJ Tables 4–6: 9 indicators by province, 1963–70 cohort |
| `absolute_mobility_canada_us.csv` | Canada 1970–84 (adjusted/unadjusted), Corak 2020 share above parents, Manduca 1985 cross-country, US Chetty reference points |
| `canada_mobility_by_group.csv` | immigrants/refugees/Canadian-born, top-end quantile IGEs, top-1% employer transmission, Indigenous |
| `oecd_broken_elevator_2018.csv` | generations-to-mean and father-son elasticity, all countries |
| `canada_homeownership_by_parent_properties.csv` | 1990s-born homeownership by number of parental properties (2021) |
| `scripts/` | `tablelib.py`, `parse_statcan_tables.py`, `build_statcan_csvs.py` (parses StatCan HTML), `build_oecd_csv.py` (reads StatLink xlsx), `build_pdf_csvs.py` (values transcribed from PDFs), `dump_xlsx.py` |
| `raw/` | source PDFs/HTML/XLSX and their text extractions (untrusted downloads) |
