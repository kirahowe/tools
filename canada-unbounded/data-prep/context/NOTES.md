# Canadian context for "Unbounded" adaptation: politics, fiscal position, history, legislative path, social data

Compiled 2026-10-10. Every number below has a URL next to it. Labels used:
- **[primary]**: the document was read directly (PDF, table CSV or official page).
- **[secondary]**: news, advocacy or third-party coverage. Use with the attribution shown.
- **[calc]**: my own arithmetic on primary figures. The method is stated each time.

Raw downloads are in `raw/` and helper scripts in `scripts/`. These tidy CSVs sit alongside this file:

| File | Contents |
|---|---|
| `fed_fiscal_history_frt2025.csv` | Federal revenues, program expenses, public debt charges, balance and accumulated deficit, 1966-67 to 2024-25, in $M and % of GDP, plus debt charges as % of revenue [calc] (Finance Canada, Fiscal Reference Tables 2025, Tables 1-2) |
| `imf_g7_debt_fm2026apr.csv` | G7 general government gross and net debt, % of GDP, 2017-2031 (IMF Fiscal Monitor April 2026, Tables A7/A8) |
| `wealth_tax_proposals_canada.csv` | Comparison table of Canadian wealth-tax proposals |
| `polling_wealth_tax_canada.csv` | Polling on wealth taxes |
| `statcan_dhea_networth_shares_36-10-0660-01.csv` | Quarterly net-worth shares by wealth quintile and the wealth gap, 2010 Q1 to 2025 Q4 |
| `statcan_sfs_median_networth_by_province_11-10-0016-01.csv` | Median and average net worth by province and CMA, SFS 1999 to 2023 |
| `statcan_gini_11-10-0134-01.csv` | Gini coefficients (market, total, after-tax) by province, 1976 to 2024 |
| `oecd_idd_gini_g7.csv` | Disposable-income Gini for G7 countries, OECD IDD, 2015 to 2023 |
| `statcan_poverty_rates_mbm_11-10-0135-01.csv` | Official poverty rate (MBM): all persons, children, seniors |
| `statcan_emigration_17-10-0040-01.csv` | Annual emigrants and returning emigrants |
| `oecd_health_spending_can_usa.csv` | Health spending, Canada vs US (OECD SHA) |

---

## 0. Political situation at a glance (as of 2026-10-10)

- **2025 federal election (April 28, 2025)** [primary, Elections Canada official voting results, GE45 Tables 7 and 8: https://www.elections.ca/res/rep/off/ovrGE45/62/table7E.html and https://www.elections.ca/res/rep/off/ovrGE45/62/table8E.html]

  | Party | Seats (of 343) | Votes | Vote share [calc] |
  |---|---|---|---|
  | Liberal | 169 | 8,595,488 | 43.8% |
  | Conservative | 144 | 8,113,484 | 41.3% |
  | Bloc Québécois | 22 | 1,236,349 | 6.3% |
  | NDP | 7 | 1,234,673 | 6.3% |
  | Green | 1 | 238,892 | 1.2% |

  Total valid votes were 19,641,663. The result was a Liberal minority.
- **The NDP lost official party status.** It won 7 seats, below the 12-seat threshold for recognized-party status, for the first time since 1993 [secondary: https://en.wikipedia.org/wiki/2026_New_Democratic_Party_leadership_election]. I did not retrieve a primary source for the 12-seat rule (Parliament of Canada Act s.62.1). Verify it before publishing.
- **The House now has a Liberal majority.** The House of Commons member list (XML, retrieved 2026-10-10: https://www.ourcommons.ca/Members/en/search/XML) shows 337 sitting MPs: Liberal 173, Conservative 137, Bloc 21, NDP 5, Green 1. A majority of 343 seats is 172. The change came from floor crossings (four MPs from November 2025 to March 2026, then Marilyn Gladu in April 2026) and by-elections [secondary: https://www.cbc.ca/news/politics/conservative-mp-marilyn-gladu-crosses-floor-to-liberals-9.7156167]. Further by-elections were held on August 31, 2026 in Chicoutimi–Le Fjord, Beaches–East York and North Vancouver–Capilano (https://enr.elections.ca/National.aspx?lang=e). Coverage of the 2026 Spring Economic Update described it as the "first fiscal document in seven years that won't need opposition votes" (headline: https://www.villagereport.ca/village-picks/liberals-table-first-fiscal-document-in-seven-years-that-wont-need-opposition-votes-12207538). **This matters for the "How to Get a Wealth Tax" post: minority-parliament leverage, the 2022-2024 route that produced dental care and pharmacare, is not currently available.**
- **The NDP has a new leader.** Avi Lewis won the leadership on March 29, 2026 on the first ballot: Lewis 39,734 (56.02%), Heather McPherson 20,899 (29.46%), Tanille Johnston 5,159 (7.27%), Rob Ashton 4,193 (5.91%), Tony McQuail 945 (1.33%) [secondary, from Wikipedia's results table: https://en.wikipedia.org/wiki/2026_New_Democratic_Party_leadership_election; NDP release: https://www.ndp.ca/news/avi-lewis-elected-leader-ndp]. Jagmeet Singh lost his own seat in 2025, and Don Davies was interim leader from May 5, 2025.
- **The Prime Minister is Mark Carney (Liberal).** The Finance Minister is François-Philippe Champagne.

---

## 1. Canadian wealth-tax proposals and PBO costings

Full table: `wealth_tax_proposals_canada.csv`. Summary:

| Proposer (year) | Design | Revenue estimate | Families affected | Source and status |
|---|---|---|---|---|
| NDP platform (2019) | 1% on family net wealth > $20M | PBO: **$68.6B over 10 years**, from $5.6B (2021) to $9.5B (2029) | n/a | [secondary] https://www.pressreader.com/canada/calgary-herald/20190911/281827170460775. PBO 2019 costing PDF not retrieved. |
| Peter Julian MP (NDP) request, PBO note, **July 8, 2020** | 1% on net wealth > $20M; all assets and liabilities except lottery winnings | **$5.6B net revenue in 2020-21** | **13,800 economic families** | [primary] https://www.pbo-dpb.ca/en/publications/RP-2021-017-M--net-wealth-tax-canadian-resident-economic-families--impot-patrimoine-net-familles-economiques-residant-au-canada (PDF: https://distribution-a617274656661637473.pbo-dpb.ca/c40540501fd9ddd5ea3593afe7d09d9637f58c59ef53c17406f9e39a29eb50aa). Net wealth reduced 35% for behavioural response ("based on adjusted findings from the IRS"). |
| NDP opposition-day motion, Nov 16, 2020 | 1% on wealth > $20M plus an excess-profits tax (non-binding) | n/a | n/a | **Defeated 27 yeas to 292 nays** (Vote No. 22, 43rd Parl. 2nd sess.; recorded subject "Tax measures to support Canadians") [primary: https://www.ourcommons.ca/members/en/votes/43/2/22]. Liberals, Conservatives and Bloc voted against [secondary: https://www.cbc.ca/news/politics/ndp-wealth-tax-opposition-day-1.5789923]. |
| Motion M-68, Nathaniel Erskine-Smith (Liberal MP), PBO report **July 15, 2021** | **One-time** 3% on net wealth > $10M and 5% > $20M, on wealth accumulated to April 2021, payable over 5 years with no exemptions | **$44.0B over 5 years** (35% avoidance) or **$60.7B over 5 years** under PBO's new one-time behavioural model (15-20% reduction) | n/a | [primary] https://www.pbo-dpb.ca/en/publications/RP-2122-012-M--revenue-estimates-m-68-one-time-tax-extreme-wealth--estimation-recettes-mesures-prevues-dans-motion-m-68-impot-unique-richesse-extreme |
| NDP platform (2021), PBO election costing, Sept 2021 | 1% on net wealth > $10M | **$10.85B (2021-22)**, then $11.36B-$13.12B a year to 2025-26; about **$60.1B over 5 years** | PBO HFD: **76,300** families > $10M in 2019 (June 2020 report, Table 4-3); **78,400** (Dec 2021 update, Table 2-3) | [secondary for the revenue] https://www.advisor.ca/tax/ndps-proposed-wealth-tax-would-generate-60-billion-over-five-years-pbo/ ; [primary for the counts] the PBO reports below. 35% avoidance; administration cost 2% of gross revenue. |
| Canadians for Tax Fairness (advocacy), Aug 2021 | 1% > $10M; 2% > $100M; 3% > $1B | "close to $20 billion annually," attributed to PBO figures without a document citation | n/a | [secondary, advocacy] https://www.taxfairness.ca/fr/node/1378 |
| **NDP platform (2025)** and Green Party (2025), PBO election costing **"Implementing annual wealth tax," April 9, 2025** | **1% on household net worth $10M-$50M; 2% on $50M-$100M; 3% > $100M**; effective July 1, 2025 | **$22.790B (2025-26), $23.078B, $23.628B, $25.137B, $26.848B (2029-30); total $121.479B over 5 years** | PBO 2023 HFD: **108,000 families > $10M; 10,300 > $50M; 3,700 > $100M** | [primary] https://distribution-a617274656661637473.pbo-dpb.ca/cc1b029d8d36c1340f715cb7666bfd2132ef6683608cecae42a5e71b82bb6ad6. Assumes 35% of each household's net worth is hidden; administration 2% of gross. |
| NDP cost document (April 2025) | same tiers | $22.755B / $23.042B / $23.591B / $25.098B (2025-26 to 2028-29) | | [primary, party doc] https://www.ndp.ca/sites/default/files/cost-of-commitments.pdf. The figures differ slightly from the PBO note. The PBO note does not name its requester, so whether it was the NDP's or the Greens' request is not stated. |
| Green Party release, April 22, 2025 | wealth tax on "households with a net worth exceeding $10 million" | "projected to generate $121 billion by 2030 … as outlined in the PBO's latest analysis" | | [primary, party] https://www.greenparty.ca/en/news/green-party-innovates-to-protect-canadas-economic-sovereignty |
| **Avi Lewis** leadership platform (2026), now NDP leader | "1% on net wealth above $10 million, 2% above $50 million and 3% above $100 million"; also a 45% tax on estates/inheritances > $5M, a 37% top bracket on income > $1M, a one-time windfall tax, and full inclusion of capital gains with inflation adjustment | Campaign claim: "$40 billion a year, and $500 billion over ten years" (links to CCPA-BC, **not** PBO). Inheritance tax "over $2 billion annually" per C4TF. | n/a | [primary, campaign] https://lewisisleader.ca/ideas/tax-policy-full-plan/. **These revenue figures are campaign claims, not independent costings.** The PBO costing of the same tiers was about $23-27B a year. |

Notes:
- **Caveat on comparing estimates.** The CCPA-BC estimate of about "half a trillion dollars" over ten years (https://bcpolicy.ca/2025/06/04/wealth-tax/) is advocacy, and sits roughly double PBO's estimate for the same tiers. The CCPA has argued that the PBO undercounts, mainly because of the 35% avoidance haircut [secondary: https://www.policyalternatives.ca/news-research/wealth-tax-would-raise-much-more-revenue-than-previously-calculated-by-parliamentary-budget-officer/].
- **PBO's wealth database (HFD), 2025 update** (Sept 11, 2025; https://www.pbo-dpb.ca/en/publications/RP-2526-009-S--estimating-top-tail-family-wealth-distribution-in-canada-2025-update--estimation-extremite-superieure-distribution-patrimoine-familial-canada-mises-jour-2025) [primary]:
  - Top 1% of families held **23.8%** of net wealth in 2023, against **11.0%** in the unadjusted SFS.
  - Top 10% held 52.7%, top 20% 69.2%, middle 40% 27.6% and bottom 40% 3.3%.
  - Top 0.01% (about 1,700 families, threshold $171.5M) held **5.3%**.
  - The top-1% threshold was **$7.4M** (169,400 families) in 2023. PBO projects 176,800 families above $7.5M by Q4 2024.
  - Families above $10M: **108,000**, holding $3,371B (20.6%). Above $1B: about 100 families with $360B (2.2%).
  - Concentration is "comparable to or marginally lower than in 2019": the top-1% share was 25.6% in 2016 (SFS + Forbes + Canadian Business) and 23.8% in 2023 (SFS + Forbes + Maclean's).
- **Related PBO costing of the capital-gains measure.** Raising the inclusion rate to two-thirds would add **$17.4B over 2024-25 to 2028-29** (PBO, Aug 1, 2024: https://www.pbo-dpb.ca/en/publications/LEG-2425-010-S--increasing-capital-gains-inclusion-rate--augmentation-taux-inclusion-gains-capital) [primary]. Budget 2024's own estimate was $19.4B over five years.

---

## 2. Polling on wealth taxes (`polling_wealth_tax_canada.csv`)

- **Abacus Data, November 2020** (fieldwork Nov 6-12; n=1,660; no sponsor stated) [primary: https://abacusdata.ca/wealth-tax-canada-poll/]. Asked about "a 1% tax on wealth paid by people who have more than $20 million in assets":
  - **79% support** (35% strongly).
  - **Conservative voters 64%**, Liberal 86%, NDP 87%.
  - At least 75% in every region and age group, and **73% in the top income bracket**.
- **Abacus for the Broadbent Institute and PIPSC, August 2021** (fieldwork July 13-19; n=1,500) [primary, sponsored: https://abacusdata.ca/canadians-want-a-wealth-tax/]. **88%** wanted a 1% wealth tax on the wealthiest in the recovery plan (54% "must have", 34% "nice to have"). The release says "Conservative supporters are almost as likely as NDP or Liberal supporters to support implementing a wealth tax" but gives no figure.
- **Abacus for North 99 (advocacy), May 2019** (n=2,000) [secondary, Ricochet: https://ricochet.media/justice/poll-over-two-thirds-of-canadians-back-a-wealth-tax/]. Asked about a 2% annual tax on assets over $50M:
  - **67% support, 14% oppose**.
  - **Conservative voters 60% support, 24% oppose**. Liberal 78%, NDP 80%.
- **Léger, April 2024** (n=1,525) [**UNVERIFIED**, from a social-media summary: https://x.com/CanadianPolling/status/1780303866946334823]. 78% support "a new tax on personal wealth over $10 million"; 29% support a new inheritance tax. I could not locate Léger's own release. Do not publish without the original.
- **Angus Reid Institute, June 2024** (fieldwork May 24-28; n=1,603) [primary: https://angusreid.org/federal-politics-capital-gains-inclusion/]. On the **capital-gains inclusion-rate increase**, which is not a wealth tax: about one-third support and half oppose. More than 84% of Conservative supporters oppose. A useful contrast: a broadly framed "tax the very rich" draws support, while a specific measure framed as hitting investors does not.
- **Gap.** I found no 2025-2026 pollster release (Abacus, Angus Reid, Léger, Environics, Nanos) on a wealth tax. The latest verified pollster release is Abacus 2021. Claims of "80-90% support" in 2025 commentary (The Tyee, CCPA-BC) cite no specific poll.

---

## 3. Federal fiscal position

### 3a. Latest official numbers

**Budget 2025** (tabled Nov 4, 2025), Table A1.7 [primary: https://budget.canada.ca/2025/report-rapport/anx1-en.html], in $B:

| | 2024-25 | 2025-26 | 2026-27 | 2027-28 | 2028-29 | 2029-30 |
|---|---|---|---|---|---|---|
| Revenues | 511.0 | 507.5 | 523.2 | 541.3 | 560.2 | 583.3 |
| Program expenses | 489.9 | 525.2 | 528.4 | 537.9 | 549.7 | 568.3 |
| Public debt charges | 53.4 | 55.6 | 60.0 | 66.2 | 71.4 | 76.1 |
| Budgetary balance | -36.3 | **-78.3** | -65.4 | -63.5 | -57.9 | -56.6 |
| Federal debt | 1,266.5 | 1,347.0 | 1,412.4 | 1,476.0 | 1,533.9 | 1,590.5 |
| Debt, % of GDP | 41.2 | 42.4 | 43.1 | 43.3 | 43.3 | 43.1 |

**Spring Economic Update 2026** (April 28, 2026), Table A1.7 [primary: https://budget.canada.ca/update-miseajour/2026/report-rapport/anx1-en.html], in $B:

| | 2024-25 | 2025-26 | 2026-27 | 2027-28 | 2028-29 | 2029-30 | 2030-31 |
|---|---|---|---|---|---|---|---|
| Revenues | 511.0 | 511.5 | 529.6 | 546.8 | 565.9 | 589.8 | 613.7 |
| Program expenses | 489.9 | 512.8 | 536.1 | 543.9 | 555.9 | 575.4 | 591.6 |
| Public debt charges | 53.4 | 54.0 | 58.7 | 65.7 | 71.6 | 75.7 | 80.9 |
| Balance | -36.3 | **-66.9** | -65.3 | -63.1 | -57.7 | -56.2 | -53.2 |
| Federal debt | 1,266.5 | 1,333.9 | 1,399.3 | 1,462.4 | 1,520.1 | 1,576.3 | 1,629.4 |
| Debt, % of GDP | 40.7 | 41.1 | 41.5 | 41.8 | 41.9 | 41.8 | 41.6 |

The 2024-25 debt ratio was revised from 41.2% to 40.7% because of GDP revisions.

- **Debt charges as a share of revenue** [calc from the SEU 2026 table]: 2024-25 **10.5%** (53.4/511.0); 2025-26 **10.6%**; 2026-27 11.1%; 2030-31 **13.2%**. The PBO gives the same path: "10.6% (2025-26) rising to 13.2% (2030-31)" (PBO assessment of SEU 2026, May 4, 2026: https://www.pbo-dpb.ca/en/publications/NT-2627-002-S--pbo-assessment-spring-economic-update-fiscal-anchors-fiscal-sustainability--evaluation-dpb-mise-jour-economique-printemps-cibles-budgetaires-viabilite-financiere) [primary]. PBO also reports:
  - federal debt per capita of $33,592 (2024-25);
  - public debt charges per capita of $1,409 (2026-27), rising to $1,901 (2030-31);
  - "The SEU projections indicate that the Government is on track to respect its two fiscal anchors."
- **2025-26 preliminary actual** (Fiscal Monitor, March 2026, reported May 29, 2026) [secondary, CP: https://www.cp24.com/news/canada/2026/05/29/ottawa-posts-553b-deficit-for-2025-26-fiscal-year-based-on-monthly-fiscal-monitor/]: deficit about **$55.3B**, public debt charges about **$53.7B**. These figures are before year-end adjustments; final numbers will appear in the Public Accounts 2026, which I did not find published yet.
- **2024-25 final** (Public Accounts 2025, via Fiscal Reference Tables 2025, Table 1): revenues $510,951M, program expenses $489,869M, **public debt charges $53,410M**, deficit -$36,348M, **accumulated deficit (federal debt) $1,266,484M = 41.2% of GDP** [primary: https://www.canada.ca/en/department-finance/services/publications/fiscal-reference-tables/2025/part-1.html].
- **Context for comparison:** Budget 2025 debt charges ($55.6B) exceeded the Canada Health Transfer ($54.7B) [secondary, CTF advocacy: https://www.taxpayer.com/newsroom/budget-2025-continues-to-balloon-spending-and-debt]. Verify against the Budget 2025 transfer table before using.
- **Fall Budget 2026:** I found no confirmed date. Budgets moved to the fall in 2025, with a spring update in April.

### 3b. Long history, 1966-67 to 2024-25 (`fed_fiscal_history_frt2025.csv`)

Source: Fiscal Reference Tables 2025, Tables 1-2 [primary: https://www.canada.ca/en/department-finance/services/publications/fiscal-reference-tables/2025/part-1.html]. Data from 1983-84 onward are on a full accrual basis and are not directly comparable with earlier years (table footnote).

- **Federal debt peaked at 66.6% of GDP in 1995-96.** It was 54.3% in 1990-91, fell to 28.2% in 2008-09, was 31.2% in 2019-20, jumped to 47.2% in 2020-21, and was 41.2% in 2024-25.
- **Public debt charges peaked at 6.5% of GDP (1990-91)**, against 1.7% in 2024-25 (0.9% in 2020-21).
- **Debt charges as a share of revenue** [calc]: peak **37.6% in 1990-91** (45,034/119,685); 35.2% in 1995-96; low of 6.4% in 2020-21; **10.5% in 2024-25**.
- **Balanced budgets ran from 1997-98 to 2007-08.** The largest surplus was 1.8% of GDP in 2000-01.
- **The 2020-21 COVID deficit was -$327.7B (-14.8% of GDP).**

### 3c. Canada vs the G7: gross vs net debt (`imf_g7_debt_fm2026apr.csv`)

IMF Fiscal Monitor, April 2026, Statistical Appendix Tables A7/A8 [primary PDF: https://www.imf.org/-/media/files/publications/fiscal-monitor/2026/april/english/msa.pdf], general government, % of GDP, 2025:

| | Gross debt | Net debt |
|---|---|---|
| **Canada** | **113.5** | **10.2** |
| US | 123.9 | 96.7 |
| UK | 102.3 | 93.8 |
| France | 116.0 | 108.8 |
| Germany | 62.9 | 47.2 |
| Italy | 137.1 | 127.7 |
| Japan | 206.5 | 136.5 |
| G7 average | 123.0 | 93.5 |

IMF projections for 2031: Canada net 10.4 and gross 103.5; G7 net 104.4.

**Why Canada's net debt is so low: the CPP/QPP and other assets effect**
- IMF note, verbatim: "Canada's net debt corresponds to net financial liabilities as reported by Statistics Canada and includes equity and investment fund shares, which Canada has built up substantially. Statistics Canada has made a recent methodological change to value assets at market value instead of book value, which has decreased net debt."
- For the US, Canada, Australia and Hong Kong, gross and net debt are "adjusted to exclude unfunded pension liabilities of government employees' defined-benefit pension plans."
- **General government includes social security funds (CPP/QPP).** Their assets offset government liabilities in the net figure. Fiscal Reference Tables 2025 Tables 47-50 (National Balance Sheet Accounts basis; source StatCan Table 36-10-0580-01) [primary: https://www.canada.ca/en/department-finance/services/publications/fiscal-reference-tables/2025/part-8.html], calendar 2024:
  - federal net financial liabilities **$984.2B**;
  - provincial, local and Aboriginal governments **$452.5B**;
  - **social security funds (CPP/QPP) net financial worth $849.8B** (assets $1,061.2B);
  - total general government net debt **$586.9B** (= 984.2 + 452.5 - 849.8).
  - In other words, CPP/QPP assets offset about 59% of federal and provincial net liabilities [calc: 849.8/1,436.8].
- **Caution for writers.** CPP/QPP assets are earmarked to pay future pensions. Much of the pension obligation is not counted as government debt, so the "lowest net debt in the G7" claim flatters Canada. The gross figure (113.5%) is near the G7 average. Budget 2025 cited "lowest net debt-to-GDP ratio in the G7 at 13.3 per cent" (IMF October 2025 vintage) [secondary summary of Budget 2025: https://www.budget.canada.ca/2025/report-rapport/pdf/budget-2025-our-plan.pdf].
- **CPP Investments** net assets were **$793.3B at March 31, 2026**, up from $714.4B, with a 7.8% net return for fiscal 2026 [secondary, press release via Newswire: https://www.newswire.ca/news-releases/cpp-investments-net-assets-total-793-3-billion-at-2026-fiscal-year-end-814459989.html].
- The Fiscal Reference Tables 2025 Table 54 (IMF October 2025 vintage) gives Canada's net debt as 8.7% (2019) and 12.5% (2024), against a G7 average of 93.3% (2024) [primary: https://www.canada.ca/en/department-finance/services/publications/fiscal-reference-tables/2025/part-9.html].

### 3d. Provinces (Fiscal Reference Tables 2025, Part 4)

Source: [primary: https://www.canada.ca/en/department-finance/services/publications/fiscal-reference-tables/2025/part-4.html], $M.

| | Ontario 2024-25 | Ontario 2023-24 | Quebec 2024-25 | Quebec 2023-24 |
|---|---|---|---|---|
| Revenues | 226,161 | 208,975 | 156,088 | 145,541 |
| Debt charges | 15,122 | 14,461 | 9,969 | 9,982 |
| Deficit | -1,090 | -693 | -5,175 (reported balance under the Balanced Budget Act: -7,572) | -5,994 |
| Net debt | **427,050** | 409,761 | **236,163** | 220,016 |

- Debt charges as a share of revenue, 2024-25 [calc]: Ontario 6.7%, Quebec 6.4%.
- All provinces and territories combined are in FRT Table 31/32 (not extracted).
- **Point for writers:** in Canada, roughly a third of general-government net liabilities sit with the provinces (452.5 of 1,436.8 before CPP/QPP [calc]). Health care and education are provincial responsibilities.

---

## 4. History

- **Carter Commission** (Royal Commission on Taxation; report 1966; chair Kenneth Carter). It recommended that capital gains, gifts and bequests be taxed like other income because they "increase the economic power" of recipients (Carter Report vol. 3, part A, p. 25). "A buck is a buck" is **attributed** to Carter: Vaillancourt & Kerkhoff footnote 1 says "Allegedly said by the Royal Commission chair Kenneth Carter: see Macdonald (2006)." [primary, peer-reviewed: Vaillancourt & Kerkhoff, "Capital gains taxation in Canada, 1972-2017: evolution in a federal setting," *eJournal of Tax Research* 16(2), 2019, pp. 340-361: https://www.unsw.edu.au/content/dam/pdfs/business/acct-audit-tax/research-reports/past-issues/bus-2019/2019-volume-16-number-2/BUS-2019-V16-paper1-v16-n2.pdf]. **Present it as attributed, not as a documented quote.**
- **1971 reform, effective 1972** (same source):
  - Before 1972, capital gains in Canada were tax-free.
  - From 1972, **50% of realized gains** were included in income, with **deemed realization at death** and a tax-free rollover to a surviving spouse.
  - **The federal estate and gift taxes were abolished at the same time (1972)**, "to avoid double taxation." Federal death taxes had first been imposed in 1941.
- **Provincial succession duties** (provinces introduced them 1892-1905) were abolished during the 1970s, "first Prince Edward Island then the other three Atlantic provinces," and **"All provinces abolished succession duties in the 1970s except for Quebec, which eliminated the tax in 1985."** The authors attribute Quebec's timing to the Parti Québécois government (1976-85) ending the duties "in early 1985 after a change in Finance minister" (same source).
  - Revenue context, $M current: federal succession duties were 101 (1965), 80 (1972), 6 (1975) and 1 (1980); all provinces 111 / 150 / 150 / 71 (same source, Table 3, from StatCan Table 36-10-0178-01).
- **Mulroney era** (same source):
  - Lifetime capital gains exemption introduced in **1985** and phased in from $10,000 (1985), with a general exemption until it was abolished in 1994.
  - 1987 tax reform (Wilson) raised the inclusion rate to **66⅔% (1988)** and **75% (1990-2000)**. It was part of the package that led to the GST.
- **2000 cuts:** the February 2000 budget cut the inclusion rate from 75% to 66.67%, and the **October 2000** economic statement cut it to **50%**, where it has stayed since (same source). The authors note the 2000 changes were "driven in part by a policy choice of Ontario."
- **2024 proposal and its cancellation:**
  - **Budget 2024** (April 16, 2024) proposed raising the inclusion rate to two-thirds on gains above $250,000 for individuals, and on all gains for corporations and trusts, from June 25, 2024. Its own figures: "$19.4 billion over five years"; "Only 0.13 per cent of Canadians with an average income of $1.4 million" would pay more, about **40,000 people** (Table 8.1); 307,000 corporations had capital gains in 2022 [primary: https://budget.canada.ca/2024/report-rapport/chap8-en.html].
  - The measure was never enacted.
  - **March 21, 2025:** PM Carney announced the government would "cancel the proposed hike in the capital gains inclusion rate," calling it "a recognition of the vital role that builders and small businesses play," while keeping the higher Lifetime Capital Gains Exemption of **$1.25M** [primary: https://www.pm.gc.ca/en/news/news-releases/2025/03/21/prime-minister-mark-carney-cancels-proposed-capital-gains-tax-increase].
  - The NDP's 2025 platform proposed reinstating it (NDP cost document above).
- **Deemed disposition at death, ITA s.70(5):** the taxpayer "shall be deemed to have, immediately before the taxpayer's death, disposed of each capital property" at fair market value. **s.70(6)** gives a spousal rollover at cost [primary: https://laws-lois.justice.gc.ca/eng/acts/I-3.3/section-70.html]. Canada therefore taxes accrued gains at death through income tax, not through a separate estate tax.
- **Departure tax, ITA s.128.1(4)(b)** (relevant to the "exodus" objection): an emigrant is "deemed to have disposed … of each property owned by the taxpayer … for proceeds equal to its fair market value" immediately before ceasing to be resident [primary: https://laws-lois.justice.gc.ca/eng/acts/I-3.3/section-128.1.html].
  - For individuals, the main exclusions are Canadian real or immovable property, Canadian resource and timber property, property of a business carried on through a Canadian permanent establishment, "excluded rights or interests" (e.g. pensions and RRSPs), and certain property of short-term residents (resident 60 months or less in the prior 120).
  - Taxpayers can elect to post security to defer payment (ITA s.220(4.5)). I did not retrieve the text of that section.
  - **Point for writers:** Canada already has an exit tax on unrealized gains. A wealth tax could be paired with it, as could an extension that brings Canadian real estate into the deemed disposition.
- **Recent precedents for taxing concentrated wealth and profits:**
  - **Select Luxury Items Tax Act.** Enacted in Bill C-19 (Budget Implementation Act, 2022, No. 1; S.C. 2022, c. 10; royal assent June 23, 2022) [primary, LEGISinfo XML: https://www.parl.ca/legisinfo/en/bill/44-1/C-19]. Covers new cars and aircraft over $100,000 and boats over $250,000; the tax is the lesser of 10% of full value or 20% of value above the threshold; effective Sept 1, 2022. PBO estimated **$163M in 2023-24** [primary: https://www.pbo-dpb.ca/en/publications/LEG-2223-006-M--luxury-goods-sales-tax-update--taxe-ventes-produits-luxe-mise-jour].
    - **Budget 2025 ended the tax on aircraft and vessels.** It cited the tax as "inefficient, costly to administer"; lost revenue is $135M over 5 years [primary: https://budget.canada.ca/2025/report-rapport/tm-mf-en.html]. The tax on vehicles remains.
    - This was enacted in **Bill C-15, Budget 2025 Implementation Act, No. 1 (S.C. 2026, c. 3; royal assent March 26, 2026)**. The same bill **ended the Underused Housing Tax** (a 1% annual tax on vacant or underused residential property, mostly owned by non-residents) from 2025, and **repealed the Digital Services Tax Act** [primary: https://www.parl.ca/legisinfo/en/bill/45-1/C-15].
  - **Canada Recovery Dividend (CRD):** a one-time **15%** tax on bank and life-insurer groups, on average 2020-21 taxable income above **$1B**, paid in five annual instalments. It came with a **permanent 1.5-percentage-point** corporate surtax on their taxable income over $100M.
    - Enacted in Bill C-32, Fall Economic Statement Implementation Act, 2022 (S.C. 2022, c. 19; royal assent Dec 15, 2022) [primary: https://www.parl.ca/legisinfo/en/bill/44-1/C-32].
    - PBO estimates: CRD **$3.0B** [primary: https://www.pbo-dpb.ca/en/publications/LEG-2223-014-S--canada-recovery-dividend--dividende-relance-canada]; additional tax **$2.3B** [primary: https://www.pbo-dpb.ca/en/publications/LEG-2223-013-S--additional-tax-banks-life-insurers--taxe-supplementaire-banques-assureurs-vie]. Both dated Sept 22, 2022.
    - Avi Lewis's platform says the 2022 tax "resulted in an estimated $5.3 billion." That is a campaign figure, probably CRD plus surtax.
  - **Global Minimum Tax Act** (15% minimum on large multinationals; Pillar Two). Part of Bill C-69, Budget Implementation Act, 2024, No. 1 (S.C. 2024, c. 17; royal assent June 20, 2024) [primary: https://www.parl.ca/legisinfo/en/bill/44-1/C-69].

---

## 5. The legislative path in Canada

- **Constitution Act, 1867** [primary: https://laws-lois.justice.gc.ca/eng/const/page-2.html]:
  - **s.53**: "Bills for appropriating any Part of the Public Revenue, or for imposing any Tax or Impost, shall originate in the House of Commons." Tax bills cannot start in the Senate.
  - **s.54**: the Commons may not pass a vote, resolution or bill "for the Appropriation of any Part of the Public Revenue, or of any Tax or Impost" unless it has "first been recommended to that House by Message of the Governor General" (the **Royal Recommendation**, which ministers obtain). This governs **spending**.
- **Ways and Means** [primary, *House of Commons Procedure and Practice*, 3rd ed. (2017), ch. 18, "The Business of Ways and Means": https://www.ourcommons.ca/procedure/procedure-and-practice-3/ch_18_4-e.html]:
  - "A ways and means motion may therefore be moved only by a Minister of the Crown."
  - "The House must first adopt a ways and means motion before a bill which imposes a tax or other charge" is introduced.
  - A private member's bill that "had the effect of increasing the amount of tax payable … cannot be introduced in the House unless it is preceded by a ways and means motion."
  - By contrast, "Legislative proposals which are not intended to raise money but rather to reduce taxation … need not be preceded by a ways and means motion."
  - **So an opposition MP or backbencher cannot table a binding wealth-tax bill.** They can move non-binding motions (e.g. M-68, or the 2020 NDP opposition-day motion, defeated 27-292), ask the PBO to cost proposals, and use petitions and committees. Only Cabinet can start a tax.
- **There is no US-style discharge petition or cosponsor mechanism.** Party discipline is strong and confidence votes discipline the governing party. Leverage comes from **minority parliaments** and opposition-day motions (non-binding).
- **E-petitions** [secondary: Samara Centre, https://samaracentre.ca/articles/e-petitions; official portal https://www.ourcommons.ca/petitions/en/Home/Index]:
  - A petition needs an MP sponsor and **5 supporters** to publish.
  - It is open **30, 60, 90 or 120 days**.
  - It needs **500 signatures** to be certified and presented.
  - The government must table a response within **45 calendar days** of presentation.
  - I did not retrieve a list of past wealth-tax e-petitions; the portal's search is JavaScript-only.
- **Minority leverage, the case study (2022-2024):**
  - The Liberal-NDP **"supply and confidence agreement"** was announced **March 22, 2022** and was to run "until when Parliament rises in June of 2025" [primary: https://www.pm.gc.ca/en/news/news-releases/2022/03/22/delivering-canadians-now].
  - Commitments included a **dental care program** for families with income under $90,000, a **Canada Pharmacare Act**, and, under "A fairer tax system," taxes on financial institutions that profited in the pandemic. That last item became the CRD and the 1.5-point surtax in Bill C-32.
  - Delivered:
    - **Dental care:** first through Bill C-31 (Cost of Living Relief Act, No. 2, S.C. 2022, c. 14; royal assent Nov 17, 2022) [primary: https://www.parl.ca/legisinfo/en/bill/44-1/C-31], then the Canadian Dental Care Plan.
    - **Pharmacare Act**, Bill C-64 (S.C. 2024, c. 24; royal assent Oct 10, 2024) [primary: https://www.parl.ca/legisinfo/en/bill/44-1/C-64].
  - The NDP ended the agreement on **Sept 4, 2024** [secondary: https://www.cbc.ca/1.7312910].
  - **Lesson:** a smaller party extracted signature programs and a bank-profits tax, **but not a wealth tax**.
- **Federal vs provincial taxing powers** [primary: https://laws-lois.justice.gc.ca/eng/const/page-3.html]:
  - Federal **s.91(3)**: "The raising of Money by any Mode or System of Taxation."
  - Provincial **s.92(2)**: "Direct Taxation within the Province in order to the raising of a Revenue for Provincial Purposes."
  - A net-wealth tax is a direct tax, so **both orders of government could levy one**. Provinces historically levied succession duties (Quebec until 1985), and municipal property taxes are already a tax on one form of wealth.
- **Current arithmetic.** With a Liberal majority (173 of 343) since spring 2026, an NDP with 5 MPs and no party status, and a government that cancelled the capital-gains increase, there is **no parliamentary route to a wealth tax in this Parliament without the government's own decision**. The pressure channels left are public opinion, the PBO (costings at an MP's request), committees, provincial action and the next election (fixed date in October 2029 unless called earlier). The October 2029 date is not verified; check the Canada Elections Act s.56.1.

---

## 6. Uniquely Canadian social context

### 6a. Intergenerational mobility: Canada vs US

Source: Connolly, Corak & Haeck, "Intergenerational Mobility between and within Canada and the United States," *Journal of Labor Economics* 37(S2), 2019, S595-S641, DOI 10.1086/703465. I read the NBER WP 25735 version (April 2019): https://www.nber.org/papers/w25735 (PDF https://www.nber.org/system/files/working_papers/w25735/w25735.pdf) [primary, working-paper version]. **Check the final JOLE tables before quoting the exact digits.**

- **Rank-rank slope: US 0.341, Canada 0.212-0.229** (Table 2). Lower means more mobile.
  - "a child raised by top percentile parents in the United states will rank about 31 to 34 percentiles higher … than a bottom percentile child, but in Canada this difference would, at 21 to 23 percentiles, be a full decile lower."
- **Bottom-to-top-quintile ("rags to riches") probability: US 7.5%, Canada 11.2%** (Table 3).
  - Top-quintile persistence: US 36.5%, Canada 29.5%.
  - Bottom-quintile persistence: US 33.7%, Canada 32.0%.
- "The US 'middle class' is within easier reach for low income Canadian children, than it is for low income Americans."
- Abstract: "some Canadian regions have more in common with the low mobility southern parts of the United States than with the rest of Canada."

### 6b. Income inequality

- **StatCan Table 11-10-0134-01, Gini on adjusted after-tax income, Canada** (`statcan_gini_11-10-0134-01.csv`) [primary: https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1110013401]:
  - after-tax: **0.305 (2024)**, 0.301 (2023), 0.281 (2020, the pandemic transfers low), 0.317 (2000), 0.286 (1990), 0.300 (1976);
  - market income Gini: 0.436 (2024).
  - **By province, 2024 after-tax:** Ontario 0.318 (highest of the provinces), BC 0.310, Alberta 0.308, Quebec **0.276**, PEI 0.268, Nunavut 0.323.
- **OECD IDD, disposable-income Gini 2023** (`oecd_idd_gini_g7.csv`) [primary: OECD SDMX DF_IDD, https://sdmx.oecd.org/public/rest/data/OECD.WISE.INE,DSD_WISE_IDD@DF_IDD,/]: **Canada 0.306, US 0.394, UK 0.367, Germany 0.307, France 0.298**.
- **StatCan income gap:** the gap between the top 40% and bottom 40% shares of disposable income reached **46.7 percentage points in 2025**, up from 46.4 (The Daily, April 13, 2026) [primary: https://www150.statcan.gc.ca/n1/daily-quotidien/260413/dq260413a-eng.htm].

### 6c. Wealth concentration: latest official (StatCan DHEA) (`statcan_dhea_networth_shares_36-10-0660-01.csv`)

Source: The Daily, **April 13, 2026** (Q4 2025) [primary: https://www150.statcan.gc.ca/n1/daily-quotidien/260413/dq260413a-eng.htm; Table 36-10-0660-01].

- "The wealth gap grew throughout 2025 as continued strong financial market gains benefited the wealthiest."
- Top 20% held **65.7%** of net worth (average **$3.5M** per household). Bottom 40% held **3.0%** (average **$81,650**).
- **Wealth gap (top 20% minus bottom 40%) = 62.7 points, up 0.6 points in a year.**
- Household net worth rose 5.3% year over year, "entirely due to a strong gain in financial assets (+9.9%)." Real estate fell 0.7%.
- Net worth grew fastest for the wealthiest (+6.0%) and slowest for the least wealthy (+2.1%).
- Trend from the CSV [calc from the table]: the gap was 67.4 (Q4 2010), fell to a low of **60.7 (Q1 2022)**, and has risen since to 62.7 (Q4 2025).
- **Note:** the DHEA is benchmarked to the SFS and national accounts. Like the SFS, it understates the very top. PBO's HFD (section 1) adjusts for this: 23.8% for the top 1% vs 11.0% in the SFS.

### 6d. Provincial differences in wealth (SFS 2023) (`statcan_sfs_median_networth_by_province_11-10-0016-01.csv`)

Source: StatCan Table 11-10-0016-01 [primary: https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1110001601].

- National median net worth **$519,700** (2023).
- By province: BC $773,500; Ontario $665,600; Alberta $457,100; PEI $399,800; Saskatchewan $394,600; Manitoba $386,300; **Quebec $371,000**; Nova Scotia $354,600; NL $333,500; **New Brunswick $286,200**.
- By city: **Vancouver CMA $1,008,200**; Toronto $743,000.
- These are 2023 constant dollars. The table also gives 95% confidence intervals.
- SFS coverage "excluding Indigenous communities and collective dwellings"; the territories are not covered (The Daily, Oct 29, 2024: https://www150.statcan.gc.ca/n1/daily-quotidien/241029/dq241029a-eng.htm).
  - Families aged 55-64 that own a home and have an employer pension had median net worth of **$1.4M**. Renters with no employer pension had **$11,900**.

### 6e. Indigenous income gaps

Source: 2021 Census (2020 incomes), compiled by Indigenous Services Canada, "An update on the socio-economic gaps between Indigenous Peoples and the non-Indigenous population in Canada: Highlights from the 2021 Census" [primary, federal: https://www.sac-isc.gc.ca/eng/1690909773300/1690909797208].

- **Median total (before-tax) income, ages 25-64:**

  | Group | Median income | Gap vs non-Indigenous |
  |---|---|---|
  | Non-Indigenous | $50,400 | – |
  | Métis | $48,800 | $1,600 |
  | Non-Status Indian | $43,200 | $7,200 |
  | Inuit | $42,800 | $7,600 |
  | Registered Indian, off reserve | $42,000 | $8,400 |
  | Registered Indian, on reserve | **$32,000** | **$18,400** |

- **Low-income rate (LIM-AT), 2020:** non-Indigenous 10.7%; Métis 12.6%; Inuit 16.5%; Non-Status 16.8%; Registered off reserve 19.8%; **Registered on reserve 31.3%**. "More than 335,000 Indigenous people were living in a low-income situation in 2020."
- **Wealth data gap:** no SFS-based Indigenous wealth estimates were found. The SFS excludes Indigenous communities, which is itself a point worth making.

### 6f. Immigrants and wealth

Source: StatCan/IRCC, "Trends in the wealth gap between immigrant and Canadian-born families," *Economic and Social Reports* (36-28-0001), **March 25, 2026** [primary: https://www150.statcan.gc.ca/n1/pub/36-28-0001/2026003/article/00002-eng.htm].

- "Canadian-born families held double the median wealth of recent immigrant families in 2016 and 2023."
- Established immigrant families were "surpassing Canadian-born families in median wealth by 2023."
- Recent immigrant families had median net worth of $132,300 in 2023, against $77,500 in 2016. This figure is from a secondary summary; check it against the article's tables.

### 6g. Concentrated industry and dynastic control

- **Groceries.** Competition Bureau, *Canada Needs More Grocery Competition*, June 27, 2023 (retail grocery market study). The Bureau's page could not be fetched; secondary sources:
  - The five largest grocers (Loblaw, Sobeys/Empire, Metro, Costco, Walmart) control "roughly 80 per cent" of the market. Food gross margins rose by a "modest yet meaningful" 1-2 percentage points over five years, "roughly equivalent to $1 to $2 on each $100." [secondary: https://globalnews.ca/news/9796699/competition-bureau-canada-grocery-study-takeaways/; Fasken summary: https://www.fasken.com/en/knowledge/2023/06/canada-needs-more-competition-competition-bureau-releases-its-retail-grocery-market-study-report]
  - Loblaw, Empire and Metro made "more than $3.6-billion" in profit on more than $100B of sales the previous year (Globe/National Observer summaries).
  - **Find the Bureau's original before publishing.**
- **Banks.** The Big Six hold about **90%+** of Canadian banking assets. Sources vary by year and measure: DBRS Morningstar 2024 gives **92% of loans and 94% of deposits** [secondary: https://dbrs.morningstar.com/research/421868/dbrs-morningstars-takeaways-from-credit-outlook-canada-2024-solid-fundamentals-position-large-canadian-banks-for-growth-opportunities]. I found no official OSFI or Finance figure; use the DBRS figure with attribution, or omit it.
- **Telecoms.** CRTC reports (Bell, Rogers, Telus "top 3" revenue share) could not be fetched; the site is bot-protected. **Not verified.** Primary source to check: https://crtc.gc.ca/eng/publications/reports/policymonitoring/2025/ctmr.htm (the "Revenue share (%) – Top 3 operators" chart).
- **Productivity and competition.** Bank of Canada Senior Deputy Governor Carolyn Rogers, "Time to break the glass: Fixing Canada's productivity problem," Halifax, March 26, 2024 [primary, BIS reprint: https://www.bis.org/review/r240326e.htm]:
  - "Back in 1984, the Canadian economy was producing 88% of the value generated by the US economy per hour … by 2022, Canadian productivity had fallen to just 71%."
  - "businesses become more productive when they're exposed to competition."
  - The speech does not discuss inequality.
- **Dynastic control, peer-reviewed.** Morck, Stangeland & Yeung, "Inherited Wealth, Corporate Control and Economic Growth: The Canadian Disease," NBER WP 6814 (1998); published in *Concentrated Corporate Ownership* (U. Chicago Press/NBER, 2000) [primary: https://www.nber.org/papers/w6814].
  - "Heir-controlled Canadian firms show low industry-adjusted financial performance."
  - Countries with large heir-held billionaire wealth relative to GDP grow more slowly.
  - Heir control fell after the Canada-US Free Trade Agreement.
- **Family fortunes.** Dynasties include the Thomsons (Thomson Reuters/Woodbridge), Westons (Loblaw/George Weston), Irvings, McCains, Rogers, Saputos and Desmarais (Power Corp). Forbes put the Thomson family at about US$68.6B in January 2025 [secondary: https://media.blogto.com/city/2025/01/canadas-wealthiest-family-fortune-almost-100-billion]. The Maclean's rich list (https://macleans.ca/society/canadas-richest-people/) returned 403. **Get current figures from Forbes or Maclean's directly before publishing.** Dual-class share data were not gathered.

### 6h. Inheritance and the coming transfer

- **CIBC (Benjamin Tal), "The Looming Bequest Boom," June 6, 2016** [secondary, bank research]: boomers "stand to inherit $750 billion in the next 10 years" [https://cibc.mediaroom.com/2016-06-06-Canadian-baby-boomers-stand-to-inherit-750-billion-in-the-next-10-years-CIBC]. The average inheritance was about $180,000, and higher-income households received much larger ones.
- **CPA Canada (2023):** about **$1 trillion** to pass from boomers to their children in 2023-2026 [secondary, via CBC: https://www.cbc.ca/news/canada/saskatchewan/wealth-transfer-inequality-1trillion-1.7462837]. **The "$1 trillion" figure is CPA Canada's, not CIBC's.**
- **Peer-reviewed:** a 2024 study using the SFS (2005-2019) finds that inheritances are more likely and larger for higher-socioeconomic-status households, and that they reproduce inequality [https://releve.erudit.org/fr/revues/efg/2024-n46-efg09848/1116166ar/resume/]. No official estimate of the share of wealth that is inherited was found.

### 6i. Public services and cost of living

- **Health spending** (OECD SHA, retrieved via sdmx.oecd.org; `oecd_health_spending_can_usa.csv`):

  | 2024 | Canada | US |
  |---|---|---|
  | Health spending, % of GDP | **11.5%** | **17.2%** |
  | Per capita, US$ PPP, current prices | **$7,372** | **$14,823** |
  | Household out-of-pocket share | 15.0% | 11.0% |

  - Caveat: Canada's out-of-pocket share is higher than the US share because the US counts mandated private insurance as "compulsory." Out-of-pocket per capita is still higher in the US ($1,637 vs $1,108).
  - CIHI forecasts 2025 health spending of **$399B, 12.7% of GDP, 71% public** [secondary summary of CIHI NHEX 2025: https://www.oralhealthgroup.com/news/2025s-top-five-health-expenditure-trends-in-canada-1003992492/]. Its GDP share differs from the OECD measure.
- **Child benefits and poverty:**
  - The **Canada Child Benefit** (from July 2016) is income-tested and tax-free.
  - Official poverty rate (MBM, 2018 base): children **16.3% (2015) → 9.4% (2019) → 4.8% (2020)**.
  - On the 2023 base: children **11.5% (2024)**; all persons **11.0% (2024)**, 11.1% (2023) [primary: StatCan Table 11-10-0135-01; `statcan_poverty_rates_mbm_11-10-0135-01.csv`].
  - Peer-reviewed causal evidence: Baker, Messacar & Stabile, "Effects of Child Tax Benefits on Poverty and Labor Supply: Evidence from the Canada Child Benefit and Universal Child Care Benefit," *J. Labor Econ.* 41(4), 2023, 1129-1182, DOI 10.1086/721379. Both reforms reduced child poverty, the CCB more so, with no detectable labour-supply response among single mothers [summary via https://crdcn.ca/publication/the-effects-of-child-tax-benefits-on-poverty-and-labor-supply-evidence-from-the-canada-child-benefit-and-universal-child-care-benefit/]. **Do not attribute the whole 2015-2019 decline to the CCB.**
- **$10-a-day child care:** the Canada-wide Early Learning and Child Care system (Budget 2021). I did not gather the numbers; verify before citing.

### 6j. "Brain drain" and emigration (`statcan_emigration_17-10-0040-01.csv`)

Source: StatCan Table 17-10-0040-01, quarterly components summed to calendar years [primary: https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid=1710004001].

- Emigrants: 68,945 (2015), 90,460 (2019), 60,407 (2020), **112,561 (2023)**, 109,535 (2024), **111,572 (2025)**.
- Returning emigrants: about 50,000 a year. Net emigration in 2025 was **61,508**.
- These are estimates, the table gives **no destination breakdown**, and recent quarters are preliminary.
- Against a population of about 41 million, annual emigration is about 0.27% [calc, approximate].
- I found no current official study on high-income emigrants. The Fraser and Hub pieces are opinion or advocacy.

---

## 7. Recent official reports on inequality (2024-2026)

- **StatCan DHEA, April 13, 2026:** the wealth gap grew through 2025, and the income gap reached 46.7 points (above).
- **StatCan DHEA for Q3 2025** (January 29, 2026): top 20% held 65.5% of net worth and the bottom 40% held 3.1% [secondary summary; Daily PDF: https://www150.statcan.gc.ca/n1/daily-quotidien/260129/dq260129b-eng.pdf].
- **StatCan SFS 2023** (The Daily, Oct 29, 2024): median $519,700; StatCan warns that the SFS "sample size is insufficient to have a representative sample of economic families with very high wealth … 'the share of wealth held by the top 1%' will be understated."
- **PBO HFD 2025 update** (Sept 11, 2025): top 1% held 23.8% (section 1).
- **StatCan/IRCC immigrant-wealth study** (March 25, 2026): section 6f.
- **Bank of Canada:** Rogers' productivity "break the glass" speech, March 26, 2024 (section 6g). I found **no 2025-2026 Macklem or Rogers speech focused on inequality**. Bank of Canada research on distributional effects exists (e.g. its 2022 annual conference had an inequality session: https://www.bankofcanada.ca/2022/11/2022-annual-bank-of-canada-economic-conference/), but I did not verify specific quotes.

---

## Open items: verify before publication

1. Léger April 2024 wealth-tax poll: find Léger's own release.
2. PBO 2019 and 2021 NDP wealth-tax costing PDFs: figures are currently press-reported.
3. Competition Bureau grocery study original; CRTC top-3 telecom share; an official figure for the Big Six share of bank assets.
4. Final Public Accounts 2026 (2025-26 actuals) and the date of the Fall 2026 budget.
5. Final JOLE tables for Connolly, Corak & Haeck: the numbers here are from the NBER WP.
6. Official-party-status threshold (Parliament of Canada Act s.62.1) and the fixed election date (Canada Elections Act s.56.1).
7. Current rich-list figures (Forbes or Maclean's) and data on dual-class shares.
