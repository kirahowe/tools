# Canada Scorecard: research notes (Carney government, from March 14, 2025)

Compiled 2026-10-10. Every figure here has a source URL and a date. Rules used throughout:
- **(A) Announced** means a government statement or budget proposal.
- **(L) Legislated/implemented** means royal assent (from the LEGISinfo JSON, `raw/legis/bills_45-1.json`) or an administrative act.
- **(O) Outcome** means measured data.
- Media is used only to date events that have no primary source in hand, and it is labelled "(media)" wherever it appears.

The government took office on **2025-03-14**. In the tidy CSVs, `regime` is set as follows:
- `pre`: monthly data before 2025-03, quarterly data before 2025Q1.
- `transition`: the month 2025-03 and the quarter 2025Q1.
- `post`: everything after that.

Chart guide: draw a vertical rule at 2025-03-14. Treat 2025Q1 as mostly pre-policy.

---

## 0. Headline picture (as of 2026-10-10)

| Indicator | Before (period) | Latest (period) | Source |
|---|---|---|---|
| Real GDP growth, y/y | 3.1% (2025Q1) | 1.1% (2026Q2); q/q annualized: -1.0% 2025Q2, +1.9% Q3, -1.0% Q4, +0.3% 2026Q1, +3.3% 2026Q2 | 36-10-0104 |
| Real GDP per capita (chained 2017 $) | 60,340 (2025Q1) | 60,446 (2026Q2), +0.2% over 5 quarters | 36-10-0104 / 17-10-0009, derived |
| Business labour productivity (2017=100) | 104.73 (2025Q1) | 105.28 (2026Q2), +0.5%; y/y +1.2% | 36-10-0206 |
| Real business non-residential investment, y/y | +2.2% (2025Q1) | -0.9% (2026Q1), +1.5% (2026Q2). M&E: -6.4% 2025Q2 to +6.3% 2026Q2 | 36-10-0104 |
| Unemployment rate, SA | 6.8% (Mar 2025) | peaked at 7.1% (Aug/Sep 2025); 6.5% (Sep 2026) | 14-10-0287 |
| CPI, y/y | 2.3% (Mar 2025) | 1.7% (Apr 2025, carbon charge removed); 3.2% (May 2026); 3.0% (Aug 2026). Core ex food and energy 2.1% | 18-10-0004 |
| Real wages (LFS hourly, deflated by CPI) | +1.2% (Mar 2025) | -1.05% (Aug 2026) | 14-10-0063 / 18-10-0004 |
| Housing starts | 245,367 (2024) | 259,028 (2025); Jan-Aug 2026 average SAAR 243,813; Aug 2026 SAAR 229,046 | 34-10-0135, 34-10-0158 |
| Population growth, y/y | 1.88% (Jan 1 2025) | 0.46% (Jul 1 2026); population fell 0.02% between Oct 1 2025 and Jan 1 2026 | 17-10-0009 |
| NPR share of population | 7.05% (Apr 2025) | 6.65% (Jul 1 2026); target <5% by end-2027 | 17-10-0121, derived |
| US share of goods exports (12-month rolling) | 76.2% (Mar 2025) | 68.3% (Aug 2026) | 12-10-0011, derived |
| BoC policy rate | 2.75% (cut 2025-03-12) | 2.50% (2025-09-17); 2.25% (2025-10-29); held since then (Sep 2 2026 decision) | BoC Valet V39079 |
| Federal deficit 2025-26 | Budget 2025 projection: -$78.3B | SEU 2026: -$66.9B; Fiscal Monitor preliminary (unaudited): -$55.3B | Finance Canada |
| Core defence, % GDP (NATO) | 1.41% (2024) | 2.03% (2025e); 2.13% (2026e) | NATO, 2026-07 |

Note on the policy-rate dates: Valet's effective dates are one day after the announcement (for example, 2025-03-13 is the date effective for the March 12 decision). The BoC policy rate is not a government lever. It appears here as context.

---

## 1. Tax and affordability

### Consumer carbon price ended
- **(A)** The first act of the new government, on 2025-03-14. The fuel charge was set to zero from 2025-04-01 by order in council. Source for the date: https://www.cbc.ca/lite/story/1.7484290 (media). Budget 2025 claims this cut gas prices by "approximately 18¢ per litre in most provinces and territories": https://budget.canada.ca/2025/report-rapport/intro-en.html
- **(L)** Bill C-4 (Making Life More Affordable for Canadians Act), Part 3, "repeals Part 1 of the Greenhouse Gas Pollution Pricing Act and the Fuel Charge Regulations". Royal assent **2026-03-12** (S.C. 2026, c. 2).
  - Bill page: https://www.parl.ca/legisinfo/en/bill/45-1/c-4
  - Royal-assent text: https://www.parl.ca/DocumentViewer/en/45-1/bill/C-4/royal-assent
- **(L)** Bill C-31 would stop Canada Carbon Rebate payments for returns filed after 2026-10-30. Status: in committee. https://www.parl.ca/legisinfo/en/bill/45-1/c-31
- Industrial carbon pricing was retained.
- **(O)** CPI gasoline y/y was -18.1% in Apr 2025 (Mar: -1.6%). All-items CPI fell from 2.3% (Mar) to 1.7% (Apr 2025). This is a mechanical price-level effect. It also creates a base effect: when the removal dropped out of the y/y comparison in Apr 2026, gasoline y/y jumped to +28.6%, together with the Middle East oil shock. Data: `ind_prices_monthly.csv`.

### Capital gains inclusion-rate increase cancelled
- **(A)** 2025-03-21. Source (media): https://www.ctvnews.ca/politics/article/carney-confirms-liberals-wont-proceed-with-planned-capital-gains-tax-change
- **(L)** No law was needed, because the 2024 increase was never enacted. Budget 2025's revenue outlook cites the cancellation: https://budget.canada.ca/2025/report-rapport/anx1-en.html
- **(L)** The higher Lifetime Capital Gains Exemption ($1.25M) was legislated in C-15. Royal assent 2026-03-26.

### Middle-class tax cut (lowest bracket 15% to 14%)
- **(A)** 2025-05-14, effective 2025-07-01. The rate is 14.5% for 2025 and 14% from 2026. Government claims: 22 million people, and up to $840/yr for a two-income family (Budget 2025 intro).
- **(L)** Bill C-4 Part 1. Royal assent **2026-03-12**. It was applied through payroll from July 2025, ahead of royal assent.
- **PBO**, 2025-06-18: https://www.pbo-dpb.ca/en/publications/LEG-2526-002-S--reducing-lowest-federal-personal-income-tax-rate-14-per-cent--reduire-14-taux-plus-bas-impot-federal-revenu-particuliers
  - Net cost $4.23B in 2025-26, rising to $6.40B in 2029-30, for a **$28.24B 5-year total**. This includes a $35.7B offset from the lower value of non-refundable credits.
  - Average saving per filer: $110 (2025-26), $190 (2026-27), $200 (2029-30).
- **PBO**, 2025-10-30: https://www.pbo-dpb.ca/en/publications/RP-2526-015-S--individuals-adversely-affected-middle-class-tax--particuliers-desavantages-reduction-impot-classe-moyenne
  - 19,690 singles and 19,420 couples (<1% of each) pay more in 2026, by $141 and $155 on average. These are filers subject to AMT or with high non-refundable credits, often medical or disability claims.
  - C-15 added a "temporary non-refundable tax credit applicable where an individual's non-refundable tax credit amounts exceed the first income tax bracket threshold" (item z.3), which addresses this group.
- **(O)** No ex-post outcome data. This is a distributional measure.

### Digital Services Tax rescinded
- **(A)** 2025-06-29. The June 30 collection was halted after the US suspended trade talks. https://www.canada.ca/en/department-finance/news/2025/06/canada-rescinds-digital-services-tax-to-advance-broader-trade-negotiations-with-the-united-states.html
- **(L)** C-15 Part 2 repeals the DST Act. Royal assent **2026-03-26**.

### Underused Housing Tax and luxury tax
- **(A)** Budget 2025 (2025-11-04).
- **(L)** C-15 Part 3, royal assent 2026-03-26:
  - Div 2 ends the UHT for 2025 and later years, then repeals the Act.
  - Div 3 ends the luxury tax on aircraft and vessels.
- Royal-assent text: https://www.parl.ca/DocumentViewer/en/45-1/bill/C-15/royal-assent

### Fuel excise tax suspension (2026)
- **(A)** 2026-04-14, a response to the Middle East oil-price shock. Gasoline 10¢/L and diesel 4¢/L set to $0 from 2026-04-20 to 2026-09-07.
- **(L)** C-30 (Spring Economic Update 2026 Implementation Act), royal assent **2026-06-18**.
- **(A)** Extension announced 2026-09-02: to 2027-01-31, then 50% of the normal rate until 2027-03-31. Vehicle: Bill C-38, at **report stage**, not yet law. C-38 deems the extension in force from 2026-09-08.
- **PBO**, 2026-10-08: **$4.9B** in 2026-27 for both phases, about $276 per family. Expected to be classed as operating, not capital, spending. https://www.pbo-dpb.ca/en/publications/ES-2627-004-S--pbo-update-temporarily-suspending-federal-fuel-excise-tax--mise-jour-bdpb-suspension-temporaire-taxe-accise-federale-carburant
- **(O)** Gasoline CPI was still +20% to +33% y/y from Apr to Aug 2026. Crude prices and refining margins swamp the excise relief.

### Other 2026 affordability measures
- **Canada Groceries and Essentials Benefit** (C-19): announced 2026-01-26, royal assent **2026-02-12**. Government claim: more than 12M recipients, up to $1,890 for a family of four.
- **CPP contribution-rate cut from 2027** (C-30 Part 3 Div 5): royal assent 2026-06-18. Not yet in effect.

---

## 2. Investment and productivity (Budget 2025)

### Budget 2025 (2025-11-04)
https://budget.canada.ca/2025/report-rapport/chap1-en.html
- **Productivity Super-Deduction:**
  - Reinstates the Accelerated Investment Incentive and immediate expensing (M&P equipment, clean energy, ZEVs).
  - Immediate expensing for productivity-enhancing assets (patents, data network infrastructure, software) and for SR&ED capex.
  - New immediate expensing for manufacturing and processing buildings.
  - The government says it cuts Canada's METR "by more than two percentage points" and gives "the lowest METR in the G7".
- **(L)** Most of it is in C-15 (royal assent **2026-03-26**). The M&P buildings piece is in C-31, still in committee.
- **SR&ED enhancements:** C-15. PBO note LEG-2526-011-S.
- **PBO**, 2026-02-18: the four accelerated CCA and expensing measures cost **$19.4B over 5 years**, "best characterized as a revenue deferral". https://www.pbo-dpb.ca/en/publications/LEG-2526-012-S--accelerated-capital-cost-allowance-immediate-expensing-measures--mesures-deduction-amortissement-accelere-passation-charges-immediate
- **PBO investment multipliers**, 2026-09-10. Budget 2025 had $41.3B in investment measures. Infrastructure and private R&D have the largest and most durable multipliers. Industrial programs have the lowest, because M&E has high import content. Tax measures start modest and grow over five years. https://www.pbo-dpb.ca/en/publications/NT-2627-012-C--investment-multipliers--multiplicateurs-investissement
- **(O)** It is too early to assess.
  - Real business non-residential investment: 2025Q2 -0.9%, Q3 +0.4%, Q4 -0.6%, 2026Q1 -0.9%, 2026Q2 +1.5% (all y/y).
  - M&E y/y: -6.4% (2025Q2) to -4.5% (2025Q4), then +6.3% (2026Q2).
  - Business labour productivity: +1.2% y/y in 2026Q2, but the index is only 0.5% above 2025Q1.
  - Bank of Canada, Oct 2026 speech: "Canada is investing more in residential housing than in the tools, technology and ideas that grow our economy." https://www.bankofcanada.ca/2026/10/canadas-housing-affordability-dilemma/

### Competition, open banking and regulation
- **Consumer-Driven Banking Act** re-enacted: C-15 Part 5 Div 9, royal assent 2026-03-26.
- **Competition Act greenwashing provisions loosened**: C-15 Div 43.
- **Red Tape Reduction Act** regulatory sandboxes: C-15 Div 5.
- **Non-compete ban** for federally regulated employers: C-31 Div 9, in committee.
- **Building Canada Strong Act** (C-39), introduced 2026-09-21. Amends the IAA, Building Canada Act, CER Act, Fisheries Act and Canada Marine Act, and enacts the Enabling Digital Trade Act. Status: in committee.
- **High-Speed Rail Network Act**: C-15 Div 1.

---

## 3. One Canadian Economy: internal trade and major projects

### Bill C-5 (One Canadian Economy Act)
Free Trade and Labour Mobility in Canada Act plus Building Canada Act.
- Introduced 2025-06-06. Royal assent **2025-06-26** (S.C. 2025, c. 2). https://www.parl.ca/legisinfo/en/bill/45-1/c-5

### Internal trade
- **(A)/(L)** SEU 2026 makes three claims:
  - "As of June 2025, all 53 federal exceptions to the Canadian Free Trade Agreement (CFTA) have been removed."
  - The FTLMCA "is now in place".
  - First Ministers agreed to sign a Canadian Mutual Recognition Agreement on the Sale of Goods.
  - Source: https://budget.canada.ca/update-miseajour/2026/report-rapport/chap1-en.html
- IMF staff (2025-12-05): removing sub-national frictions "could lift real GDP by up to 7 percent over time". https://www.imf.org/en/news/articles/2025/12/05/cs-canada-staff-concluding-statement-of-the-2025-article-iv-mission
- **(O)** No outcome data yet. StatCan's annual interprovincial trade flows lag about two years, so this is a **data gap**. Most barriers are provincial.

### Major projects
- **(A)** Major Projects Office referrals. The first five were referred 2025-09-11 (media): LNG Canada Phase 2, Darlington SMR, Contrecoeur, McIlvenna Bay, Red Chris.
- SEU 2026: "15 projects announced to date represent over $125 billion in capital investments" and over 60,000 construction jobs. https://www.canada.ca/en/privy-council/major-projects-office/projects/national.html
- **(L) Key finding:** the Building Canada Act **Schedule 1 (national interest projects) is empty** in the Justice Laws consolidation current to **2026-09-21**. https://laws-lois.justice.gc.ca/eng/acts/B-9.89/FullText.html
- Only 30-day notices of intent have been published:
  - West Coast Oil Pipeline: 1 Mbbl/d, Bruderheim AB to near Delta BC, notice 2026-08-01. https://gazette.gc.ca/rp-pr/p1/2026/2026-08-01/html/sup1-eng.html
  - Deep Geological Repository: notice 2026-09-19. https://gazette.gc.ca/rp-pr/p1/2026/2026-09-19/html/notice-avis-eng.html
  - Mackenzie Valley Highway and Roberts Bank Terminal 2: notices 2026-08-29. Secondary source only; verify.
- **Referral to the MPO is not designation.**
- PBO (2026-05-04): the SEU gave "directional, rather than detailed" updates on capital priorities. https://www.pbo-dpb.ca/en/publications/NT-2627-003-S--pbo-assessment-spring-economic-update-government-major-capital-priorities--evaluation-dpb-mise-jour-economique-printemps-principales-priorites-investissement-gouvernement-dans

### Energy
- **(A)** Canada-Alberta MOU, 2025-11-27 (media: https://www.ctvnews.ca/politics/article/carney-signs-major-energy-agreement-with-alberta-laying-out-conditions-for-new-oil-pipeline/). Terms:
  - Federal support for a West Coast bitumen pipeline.
  - The proposed oil and gas emissions cap is dropped.
  - A stronger Alberta industrial carbon price and Pathways CCS.
- The official MOU text was not retrieved. Treat the terms as unverified.

---

## 4. Housing

### Build Canada Homes
- **(A)**
  - Campaign pledge (Mar 2025, Liberal platform, a party document): "nearly 500,000 homes per year", i.e. double the rate over a decade.
  - BCH launched **2025-09-14** with $13B initial capitalization. Official wording: "double the pace of housing construction over the next decade". https://www.canada.ca/en/housing-infrastructure-communities/news/2025/09/the-honourable-joel-lightbound-highlights-the-launch-of-build-canada-homes-to-supercharge-homebuilding-across-the-country.html
- **(L)**
  - Funding: C-15 Part 5 Div 3, up to $11.5B for BCH plus $1.515B for Canada Lands. Royal assent 2026-03-26.
  - Crown-corporation statute: **C-20 Build Canada Homes Act**, royal assent **2026-06-18**.
- Budget 2025 ch. 3 cites two benchmarks: CMHC says 430k-480k units/yr are needed to restore 2019 affordability, and PBO says 290k/yr would close the gap. https://budget.canada.ca/2025/report-rapport/chap3-en.html
- **PBO**, 2025-12-02: https://www.pbo-dpb.ca/en/publications/RP-2526-020-S--build-canada-homes-outlook-housing-programs-under-budget-2025--maisons-canada-perspectives-entourant-programmes-logement-dans-cadre-budget-2025
  - BCH will create "approximately 26,000 new housing units" over five years. That is +2.1% vs the PBO baseline for completions and 3.7% of the 690,000-unit 2035 gap.
  - Federal housing program spending falls **56%**, from $9.8B (2025-26) to $4.3B (2028-29).
  - BCH "should be expected to make a modest contribution".
- PBO, 2026-05-04: the SEU provides "no specific targets for the pace of homebuilding". The note also says "housing starts and population growth losing momentum since Budget 2025".

### First-time buyer GST rebate and development charges
- **GST rebate:** 100% up to $1M, phased out at $1M-$1.5M. Applies to homes sold after 2025-05-27 and before 2031.
  - (L) C-4 Part 2, royal assent 2026-03-12.
  - PBO cost: **$1.9B over 6 years**. https://www.pbo-dpb.ca/en/publications/RP-2526-001-S--introducing-gst-rebates-first-time-home-buyers--remboursement-tps-acheteurs-une-premiere-habitation
- **Development charges:** C-26 provides $1.7B to provinces and territories, royal assent 2026-06-18. Ontario will cut DCs by up to 50% for three years (SEU ch. 2).

### Outcome
`ind_housing_*.csv`
- Annual starts: 2023 240,267; 2024 245,367; **2025 259,028**.
- H1 2026 (117,999) is 3.3% below H1 2025 (121,966).
- SAAR fell from a 293,877 peak (Jul 2025) to 229,046 (Aug 2026). The derived 6-month average fell from 277k (Sep 2025) to 244k (Aug 2026).
- Against "double", starts are roughly flat, at about half the CMHC benchmark.
- Starts in 2025-26 mostly reflect projects approved before 2025, interest rates, the Toronto/Vancouver pre-sale condo slump, and slower population growth. BCH units are not yet visible in the data.

---

## 5. Defence

- **(A)** 2025-06-09: reach NATO's 2% in 2025-26, with about $9B more. Source (media): https://www.ctvnews.ca/politics/article/canada-to-boost-defence-spending-to-2-of-gdp-this-fiscal-year/
- **(A)** NATO Hague Summit, 2025-06-25: 5% of GDP by 2035, made up of at least 3.5% core and up to 1.5% ancillary. Canada's interim commitment is 4% total (2.5% core) by 2030 (PBO primer).
- **(L)**
  - Funding through the 2025-26 Supplementary Estimates and Appropriation Acts.
  - Budget 2025: **$81.8B over five years** (ch. 4). Defence Investment Agency announced 2025-10-02.
  - Bill C-40 (Canadian Corporation for Defence Investment; Defence Production Act changes) introduced **2026-10-06**.
- **(O)** NATO "Defence Investment of NATO Countries (2014-2026)", cut-off 2026-07-03. https://www.nato.int/content/dam/nato/webready/documents/finance/def-exp-2026-en.pdf
  - Core defence as a share of real GDP: 2022 1.18%, 2023 1.29%, 2024 1.41%, **2025e 2.03%**, **2026e 2.13%**.
  - Nominal: C$43.9B (2024), C$65.9B (2025e), C$71.5B (2026e).
  - The tables were read from page images, because the PDF has no text layer. The 2025 vintage (https://www.nato.int/content/dam/nato/webready/documents/finance/def-exp-2025-en.pdf) had 2.01% for 2025e.
  - **The 2% target was met on NATO estimates.**
- **PBO**, 2026-02-05: https://www.pbo-dpb.ca/en/publications/RP-2526-022-S--fiscal-implications-meeting-nato-5-commitment--repercussions-financieres-atteinte-cible-5-otan
  - Reaching 3.5% core by 2035 needs about **$33.5B/yr** of extra cash on average.
  - It adds **$63.0B to the 2035-36 deficit** (1.4% of GDP) and **+6.3 points** to debt/GDP.
  - "The Government has not published supporting projection details."
- **PBO primer**, 2026-10-06: under a gradual path, core spending reaches $163.6B in 2035-36. https://www.pbo-dpb.ca/en/publications/NT-2627-013-S--canada-defence-spending-commitments-primer--engagements-canada-matiere-depenses-defense-guide-introduction
- **PBO**, 2025-10-29: from 2017-18 to 2023-24, DND capital spending fell **$18.5B** short of plan. This is an execution risk.

---

## 6. Fiscal framework and the public service

### Budget 2025 projections
Source: https://budget.canada.ca/2025/report-rapport/overview-apercu-en.html, Table 1

| | 2025-26 | 2029-30 |
|---|---|---|
| Deficit | -$78.3B | -$56.6B |
| Deficit / GDP | 2.5% | 1.5% |
| Debt / GDP | 42.4% | 43.1% |

- The FES 2024 projection for 2025-26 was -$42.2B.
- New capital/operating framework. Anchors: balance the operating budget by 2028-29, and keep deficit/GDP declining.
- Comprehensive Expenditure Review: $60B in savings and revenues over 5 years, about $13B/yr by 2028-29.

### SEU 2026 (2026-04-28)
https://budget.canada.ca/update-miseajour/2026/report-rapport/overview-apercu-en.html
- 2025-26 deficit **-$66.9B** (2.1% of GDP), which is $11.5B lower than the Budget. The reason is about $12.1B/yr of improvement before new measures, from higher revenues and nominal GDP.
- New measures net $37.5B over six years.
- 2030-31 deficit -$53.2B. Debt/GDP 41.1% in 2025-26.

### Actuals
- Fiscal Monitor March 2026 (released 2026-05-29): 2025-26 deficit **-$55.3B**, preliminary and unaudited. https://www.canada.ca/en/department-finance/services/publications/fiscal-monitor/2026/03.html
  - Revenues $500.0B, program expenses $487.9B, debt charges $53.7B.
  - Post-March adjustments have moved results by -$6.8B to +$10.9B in recent years.
  - The final figure will appear in the Public Accounts 2026. The Annual Financial Report was not yet found as of 2026-10-10.
- Fiscal Monitor July 2026: Apr-Jul 2026 deficit -$5.1B, vs -$7.8B a year earlier. https://www.canada.ca/en/department-finance/services/publications/fiscal-monitor/2026/07.html

### Independent assessments
- **PBO**, 2025-11-14: https://www.pbo-dpb.ca/en/publications/RP-2526-017-S--budget-2025-issues-parliamentarians--budget-2025-enjeux-parlementaires
  - The government's definition of capital is "overly expansive". It is about $94B (30%) larger than PBO's over 2024-25 to 2029-30.
  - Under PBO's definition, the operating balance stays in deficit.
  - There is a **7.5% chance** that deficit/GDP falls every year.
  - PBO recommends an independent body to classify capital.
- **PBO**, 2026-05-04: the SEU is "on track to respect its two fiscal anchors". The extra fiscal room is "mostly used for new policy priorities", leaving a "substantially unchanged deficit track". https://www.pbo-dpb.ca/en/publications/NT-2627-001-S--pbo-assessment-spring-economic-update-economic-fiscal-track--evaluation-dpb-mise-jour-economique-printemps-profil-evolution-economique-financiere
- **PBO EFO**, June 2026: 2025-26 deficit -$72.0B. Deficits average $4.6B/yr above the SEU. Debt/GDP rises from 41.3% to 42.5% by 2030-31. https://www.pbo-dpb.ca/en/publications/RP-2627-002-S--economic-fiscal-outlook-june-2026--perspectives-economiques-financieres-juin-2026
- **PBO**, 2026-09-24: operating-vs-capital attributions "appear contradictory" in places. https://www.pbo-dpb.ca/en/publications/RP-2627-010-S--government-operating-budget-fiscal-anchor--cible-budget-fonctionnement-gouvernement
- **IMF** Article IV, Board 2026-01-21:
  - Welcomed the capital budgeting framework and expenditure reviews.
  - Recommends "gradual fiscal consolidation over the medium term" and making the **debt ratio a formal anchor**.
  - Staff want "a clearer bridge between the new framework and standard statistical classifications".
  - Source: https://www.imf.org/en/news/articles/2026/01/21/pr-26012-canada-imf-executive-board-concludes-2025-article-iv-consultation

### Public service
- **(A)** Budget 2025: a decline of "about 40,000 positions or 10 per cent" by 2028-29, relative to 2023-24 (as quoted by PBO).
- **(O)** PBO Federal Employment Tracking Tool (2026-09-15): FTEs peaked at 367,000 in June 2024 and fell to **342,000 in June 2026**. Fiscal-year averages: 358k (2024-25) to 344k (2025-26). https://www.pbo-dpb.ca/en/publications/NT-2627-011-S--pbo-releases-federal-employment-tracking-tool--bdpb-lance-outil-suivi-emploi-federal
- The decline began before this government's budget.

---

## 7. Immigration and population

- **(A)** Budget 2025 announced the 2026-2028 Levels Plan. https://budget.canada.ca/2025/report-rapport/chap1-en.html
  - Permanent residents: 380,000/yr, down from 395,000 in 2025.
  - New temporary resident arrivals: 385,000 (2026) and 370,000 (2027-28), down from 673,650 in 2025.
  - NPRs below 5% of the population by end-2027.
  - One-time PR pathways: protected persons, and up to 33,000 work-permit holders.
- **(L)** The Levels Plan is administrative. C-12 (Strengthening Canada's Immigration System and Borders Act) received royal assent 2026-03-26.
- **(O)** Population growth was 1.88% y/y on 2025-01-01, 0.68% on 2026-01-01 and **0.46% on 2026-07-01**. Population **fell 0.017% between 2025-10-01 and 2026-01-01**. The only other quarterly decline since 1946 was -0.003% (Jul to Oct 2020, COVID). Growth from Jan 1 to Apr 1 2026 was +0.003%.
- **(O)** NPR share of the population: 7.05% (Apr-Oct 2025), 6.86% (Jan 2026), **6.65% (Jul 2026)**.
- **PBO**, 2026-02-26: the NPR share falls from its 7.6% peak (2024) to "just under 5 per cent by the end of 2027". Population growth is flat in 2026 and 0.3% in 2027. https://www.pbo-dpb.ca/en/publications/RP-2526-025-S--demographic-implications-2026-2028-immigration-levels-plan--implications-demographiques-plan-niveaux-immigration-2026-2028
- **Attribution:** the cuts began under the previous government, with student caps in 2024 and the 2025-2027 plan. This government kept and deepened them.
- Bank of Canada (Oct 2026 explainer): "In 2025, population growth slowed to just 0.5%—the slowest rate of growth in more than a century." https://www.bankofcanada.ca/2026/10/demographic-changes-are-reshaping-the-canadian-economy/
  - The BoC figure uses a different period than our Jan-to-Jan calculation.
- Slower population growth mechanically lifts per-capita figures and lowers headline GDP.

---

## 8. Trade: US tariffs, countermeasures and diversification

### Counter-tariffs
Source: https://www.canada.ca/en/department-finance/programs/international-trade-finance-policy/canadas-response-us-tariffs.html (archived)
- 25% on about $30B of US goods (2025-03-04). This predates Carney.
- $29.8B in steel, aluminum and other goods (2025-03-13).
- Autos (2025-04-09).
- Remission order (2025-04-16).
- **Most removed on 2025-09-01**, citing CUSMA treatment. Steel, aluminum and auto measures were kept.
- Budget 2025: $6.7B gross revenue assessed to 2025-10-17, $3.0B remitted, **net $3.7B**.

### 2026 escalation
- The US imposed a **50% tariff on $27.6B of Canadian goods** effective 2026-08-22, after talks broke down.
- Canada answered on 2026-08-25 with **15/25/50% surtaxes on $27.6B of US goods** from 2026-09-08, plus a **$7.5B support package**. This comes on top of "nearly $25 billion" in earlier supports. https://www.canada.ca/en/department-finance/news/2026/08/canada-announces-targeted-countermeasures-and-substantive-support-for-workers-and-businesses-in-response-to-us-tariffs.html
- BoC (2026-09-02): "new US tariffs and Canadian counter-measures have been announced following the breakdown of trade talks". https://www.bankofcanada.ca/2026/09/fad-press-release-2026-09-02/
- SEU 2026: CUSMA keeps about 85% of exports tariff-free. The effective US tariff on Canadian goods is 5.2%. This predates the August 2026 escalation.

### Diversification
- **(A)** Budget 2025: **double non-US exports over the decade** (about +$300B). Supported by the $6B Trade Infrastructure Strategy and the $5B Trade Diversification Corridors Fund.
- **(L)** Agreements implemented or signed:
  - Canada-Indonesia CEPA: C-18, royal assent 2026-05-06.
  - UK accession to CPTPP: C-13, royal assent 2026-05-06.
  - Canada-China strategic partnership (China to reduce or suspend tariffs on agri-food and seafood, per SEU 2026). The date (around early 2026) and primary text were not verified.
- Negotiations under way: India (aim end-2026), UAE, ASEAN, Philippines, Mercosur.
- Buy Canadian procurement policy: announced 2025-09-05, effective 2025-12-16 (media for the dates).
- **(O)** Goods only, BOP basis, SA (`ind_trade_*.csv`):
  - The US share of goods exports fell from 75.9% (2024) to 71.6% (2025) to 68.3% (Jan-Aug 2026).
  - Non-US goods exports: $187.6B (2024), **$220.4B (2025, +17.5%)**, and +35.4% y/y in Jan-Aug 2026. That is ahead of the 7.2%/yr doubling path.
  - Exports to the UK went from $18.5B to $29.0B to **$52.7B** (Jan-Aug of 2024/25/26). That is almost certainly mostly gold, a price effect. Crude oil to Asia (TMX opened May 2024) and high 2026 oil prices also contribute.
  - The diversification is real in the data but largely commodity- and price-driven. It is not clearly policy-caused.
  - The government counts goods plus services, +$33B in 2025 (SEU).

---

## 9. Macro assessments (context; not policy attributions)

- **Bank of Canada**
  - Jul 15 2026 MPR: growth "stalled as the economy adjusted to new tariffs, high uncertainty and slower population growth". GDP forecast 0.7% in 2026, 1.8% in 2027-28. https://www.bankofcanada.ca/2026/07/fad-press-release-2026-07-15/
  - Sep 2 2026: Q2 GDP +3.3%, "partly temporary". CPI about 3% "mainly because of persistently higher gasoline prices"; core inflation about 2%. Rate held at 2.25%.
  - The next decision and MPR are on **2026-10-28**, after this compilation.
  - Sep 2026 explainer on trade: CUSMA shielded many goods. Auto, steel, aluminum and lumber were hard hit. https://www.bankofcanada.ca/2026/09/global-trade-is-changing-how-the-canadian-economy-works/
- **IMF** 2025 Article IV (statement 2025-12-05; Board 2026-01-21)
  - Growth 1.9% (2025) and 1.7% (2026).
  - The shock was "less severe than initially feared" thanks to USMCA exemptions, monetary easing and targeted support.
  - "Weak productivity growth is a central medium-term challenge."
  - Supports Build Canada Homes as a "well-targeted supply push" alongside zoning reform.
- **OECD** Economic Survey of Canada 2025 (May 2025): growth slowing to about 1%; urges removing internal trade barriers and fixing productivity. https://www.oecd.org/en/about/news/press-releases/2025/05/oecd-economic-surveys-canada-2025.html

---

## 10. Attribution caveats

1. **Too early for most structural measures.**
   - The Productivity Super-Deduction became law in March 2026, and part of it is still in C-31.
   - BCH became a Crown corporation in June 2026.
   - Not one national-interest project has been formally designated.
   - Internal-trade data lag about two years.
   - Investment, productivity and housing effects take years. Changes in 2025-26 data mostly reflect other forces.
2. **US tariffs dominate 2025-26 macro data.**
   - The GDP contractions in 2025Q2 and Q4 and the weakness in exports, M&E and manufacturing employment follow US tariff actions and uncertainty: Mar-Apr 2025, then the 50% escalation in Aug 2026.
   - CUSMA exemptions, set by US policy, cushioned the blow.
   - Neither the shock nor the cushion is a Canadian policy result.
3. **2026 Middle East oil shock.**
   - It raised CPI (gasoline +20-33% y/y), the terms of trade, nominal GDP and federal revenues. Much of the SEU's $11.5B deficit improvement is revenue-driven.
   - It also inflated nominal non-US crude exports.
   - Gold prices are behind most of the surge in exports to the UK.
4. **Population slowdown is mechanical and bipartisan.**
   - The TR cuts started in 2024.
   - Slower population growth lowers headline GDP and lifts per-capita and productivity ratios. It also eases rents: shelter CPI fell from 4.5% (Jan 2025) to 1.3-1.8% (2026).
5. **Monetary policy is independent.** The BoC cut rates from 5% (mid-2024) to 2.25% (Oct 2025). Most of the easing came before or independently of this government.
6. **Carbon-price removal** is a one-time price-level drop, not disinflation. It drops out of y/y comparisons after 12 months, which inflated the Apr 2026 gasoline y/y.
7. **Fiscal outcomes**
   - Preliminary results are unaudited, and year-end adjustments are large.
   - The capital/operating split is the government's own classification. PBO and IMF ask for independent validation.
   - Defence "2%" uses NATO's cash, calendar-year GDP basis. Federal accounts are accrual and fiscal-year.
8. **Counterfactuals.** Several "targets" are campaign pledges or benchmarks, not legislated targets: the ~500k homes/yr and CMHC's 430-480k. A flat housing trend is not proof that a policy failed, given rates, the condo slump and slower population growth.
9. **Data vintages.**
   - StatCan tables were downloaded 2026-10-10. GDP covers through 2026Q2 (monthly GDP through Jul 2026). LFS through Sep 2026. CPI through Aug 2026 (Sep CPI not yet released). Trade through Aug 2026. Housing starts through Aug 2026. Population through Jul 1 2026.
   - All of these will be revised.

---

## 11. Data gaps and next steps

- **Interprovincial trade:** no post-2025 data (StatCan 12-10-0088 is annual and lagged).
- **Build Canada Homes:** no unit, start or completion counts published.
- **GST first-time buyer rebate:** no take-up data.
- **Major Projects Office:** no project-level spending or approval timelines in hand.
- **Public Accounts / Annual Financial Report 2025-26:** not yet found as of 2026-10-10. Re-check later in October.
- **2026 Immigration:** IRCC monthly admissions (actual PR and TR arrivals vs 380k/385k) not pulled. Source: IRCC open data.
- **NATO "ancillary" 1.5% spending:** no Canadian breakdown.
- **Not yet retrieved (verify before publishing):**
  - Primary texts of the Canada-Alberta MOU and the Canada-China arrangement.
  - PMO releases for the carbon-price and capital-gains announcements. These are dated from media.
  - The 2026-08-29 Gazette notices.
- **Next data releases:** Oct 28 2026 BoC MPR; Sep CPI (around Oct 21); Q3 GDP (end of Nov); Fiscal Monitor for August (Oct 30).

---

## 12. Files

- `policy_inventory.csv`: 37 measures. Columns: id, area, dates, legislative vehicle, royal assent, status as of 2026-10-10, stated target, outcome file, notes, sources.
- `targets_vs_actuals.csv`: 19 target-vs-actual rows with assessment and caveat.
- `fiscal_projections_vs_results.csv`: FES 2024, Budget 2025, SEU 2026 and PBO projections, plus Fiscal Monitor actuals.
- `defence_spending.csv`: NATO core defence % GDP 2014-2026e (2025 and 2026 vintages), nominal C$, targets.
- Tidy indicator files. Columns: `date, indicator, value, unit, source_table, regime`. Dates are period starts (YYYY-MM-01).
  - `ind_gdp_investment_quarterly.csv`: real GDP, growth, per capita, business non-res investment, M&E, residential, government GFCF, real goods exports.
  - `ind_gdp_monthly.csv`: monthly GDP at basic prices.
  - `ind_productivity_quarterly.csv`: business labour productivity, hours, unit labour cost.
  - `ind_labour_monthly.csv`: unemployment, employment, participation, youth unemployment, LFS hourly wage and real, SEPH weekly earnings and real.
  - `ind_prices_monthly.csv`: CPI index; y/y for all-items, gasoline, shelter, rent, food from stores, ex-gasoline, ex food and energy.
  - `ind_housing_monthly.csv`: SAAR starts and a derived 6-month average.
  - `ind_housing_annual.csv`: annual and quarterly actual starts, all areas.
  - `ind_population_quarterly.csv` and `ind_npr_quarterly.csv`.
  - `ind_trade_monthly.csv` and `ind_trade_annual.csv`: exports by destination, US share, non-US.
  - `ind_policy_rate.csv`: end-of-month rate and change events.
- **Derived-series caveats:**
  - Business non-res investment is the sum of chained components (NR structures and M&E, plus IPP). That sum is approximate, because chained dollars are non-additive.
  - GDP per capita uses the average of quarter-start populations.
  - Real wages are nominal y/y deflated by CPI y/y. The LFS wage is NSA.
- `scripts/`: `extract.py` (StatCan/BoC to tidy), `extract_extra.py` (NPR, annual trade), `build_tables.py` (inventory, fiscal, defence), `build_targets.py`, `h2t.py`, `inspect.py`. Run with `python3 -I`.
- `raw/`: StatCan zips; budget, SEU, LEGISinfo, PBO, NATO and BoC downloads.

## Update, 2026-10-11

On October 1, 2026 the Prime Minister announced the Pacific Link (West Coast oil) pipeline as the first project of national interest under the Building Canada Act (CP24, Castanet, Global News, The Logic). The rows in `sc_policy_inventory.csv` and `sc_targets_vs_actuals.csv` that say no project was designated reflect the Justice Laws consolidation current to 2026-09-21 and are superseded; the Scorecard post text is updated. Check Canada Gazette Part II for the order amending Schedule 1.
