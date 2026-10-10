"""Hand-curated tables (every figure transcribed from a cited source; see notes.md).
Usage: python3 -I build_tables.py <out_dir>
"""
import csv, os, sys

OUT = sys.argv[1]
B25 = 'https://budget.canada.ca/2025/report-rapport/'
SEU = 'https://budget.canada.ca/update-miseajour/2026/report-rapport/'
LG = 'https://www.parl.ca/legisinfo/en/bill/45-1/'
PBO = 'https://www.pbo-dpb.ca/en/publications/'
NATO26 = 'https://www.nato.int/content/dam/nato/webready/documents/finance/def-exp-2026-en.pdf'
NATO25 = 'https://www.nato.int/content/dam/nato/webready/documents/finance/def-exp-2025-en.pdf'
FM03 = 'https://www.canada.ca/en/department-finance/services/publications/fiscal-monitor/2026/03.html'
FM07 = 'https://www.canada.ca/en/department-finance/services/publications/fiscal-monitor/2026/07.html'
TAR25 = 'https://www.canada.ca/en/department-finance/programs/international-trade-finance-policy/canadas-response-us-tariffs.html'
TAR26 = 'https://www.canada.ca/en/department-finance/news/2026/08/canada-announces-targeted-countermeasures-and-substantive-support-for-workers-and-businesses-in-response-to-us-tariffs.html'
BCA = 'https://laws-lois.justice.gc.ca/eng/acts/B-9.89/FullText.html'

# status vocabulary: announced | legislated_in_force | implemented_administratively | partially_implemented |
#                    in_parliament | reversed | ongoing
INV_COLS = ['policy_id', 'area', 'policy', 'date_announced', 'legislative_vehicle', 'royal_assent_date',
            'effective_date', 'status_2026_10_10', 'stated_target_or_metric', 'outcome_indicators', 'notes', 'sources']
INV = [
 ('TAX-01', 'tax/affordability', 'End consumer carbon price (federal fuel charge set to zero)', '2025-03-14',
  'Order in council (fuel charge to $0 from 2025-04-01); statutory repeal of GGPPA Part 1 in Bill C-4 Part 3', '2026-03-12', '2025-04-01',
  'legislated_in_force', 'Govt claim: cut gas prices ~18 cents/L in most provinces; final Canada Carbon Rebate April 2025; C-31 ends rebate payments for returns filed after 2026-10-30',
  'ind_prices_monthly.csv: cpi_gasoline_yoy (Apr 2025 -18.1%), cpi_all_items_yoy',
  'Industrial (output-based) carbon pricing retained. Order-in-council date from media (CBC 2025-03-14); statutory repeal confirmed in C-4 royal-assent text. C-31 (Budget 2025 Implementation Act No.2) still in committee.',
  f'{B25}intro-en.html ; {LG}c-4 ; https://www.parl.ca/DocumentViewer/en/45-1/bill/C-4/royal-assent ; https://www.cbc.ca/lite/story/1.7484290 (media) ; {LG}c-31'),
 ('TAX-02', 'tax', 'Cancel proposed capital gains inclusion-rate increase (1/2 to 2/3)', '2025-03-21',
  'None needed (2024 proposal was never enacted); LCGE increase to $1.25M legislated in C-15', '2026-03-26 (C-15, LCGE)', '2025-03-21',
  'implemented_administratively', 'Inclusion rate stays at 1/2', 'n/a (revenue effect noted in Budget 2025 revenue outlook)',
  'Budget 2025 cites the cancellation as a reason corporate and personal income tax revenue growth is soft in 2025-26.',
  f'https://www.ctvnews.ca/politics/article/carney-confirms-liberals-wont-proceed-with-planned-capital-gains-tax-change (media) ; {B25}anx1-en.html ; https://www.parl.ca/DocumentViewer/en/45-1/bill/C-15/royal-assent'),
 ('TAX-03', 'tax', 'Middle-class tax cut: lowest federal PIT rate 15% -> 14% (14.5% for 2025, 14% from 2026)', '2025-05-14',
  'Bill C-4 Part 1 (Making Life More Affordable for Canadians Act)', '2026-03-12', '2025-07-01',
  'legislated_in_force', 'Govt: 22 million people; up to $840/yr for a two-income family. PBO: net cost $28.2B over 2025-26 to 2029-30; avg saving $110 (2025-26) to $200 (2029-30) per filer',
  'n/a (distributional; see PBO)', 'Applied via payroll from July 2025 ahead of royal assent (normal ways-and-means practice). PBO: ~39,000 filers (AMT / high non-refundable credits) pay slightly more in 2026. C-15 added a temporary credit addressing that issue (item z.3).',
  f'{LG}c-4 ; https://www.canada.ca/en/department-finance/news/2025/05/delivering-a-middle-class-tax-cut.html ; {PBO}LEG-2526-002-S--reducing-lowest-federal-personal-income-tax-rate-14-per-cent--reduire-14-taux-plus-bas-impot-federal-revenu-particuliers ; {PBO}RP-2526-015-S--individuals-adversely-affected-middle-class-tax--particuliers-desavantages-reduction-impot-classe-moyenne'),
 ('TAX-04', 'tax/trade', 'Rescind Digital Services Tax', '2025-06-29',
  'Bill C-15 Part 2 (repeals Digital Services Tax Act)', '2026-03-26', '2025-06-30 (collection halted)',
  'legislated_in_force', 'Restart Canada-US trade talks', 'n/a',
  'Announced after the US suspended trade talks; repeal retroactive to the Act (refunds with interest per Finance letter of 2025-09-13).',
  f'https://www.canada.ca/en/department-finance/news/2025/06/canada-rescinds-digital-services-tax-to-advance-broader-trade-negotiations-with-the-united-states.html ; {LG}c-15'),
 ('TAX-05', 'tax/housing', 'Eliminate Underused Housing Tax (2025 and later years)', '2025-11-04',
  'Bill C-15 Part 3 Div 2', '2026-03-26', 'calendar 2025 onward', 'legislated_in_force', 'n/a', 'n/a', '', f'{B25}chap5-en.html ; {LG}c-15'),
 ('TAX-06', 'tax', 'End luxury tax on aircraft and vessels (cars over $100k still taxed)', '2025-11-04',
  'Bill C-15 Part 3 Div 3', '2026-03-26', 'per C-15', 'legislated_in_force', 'n/a', 'n/a', '', f'{LG}c-15 ; https://www.parl.ca/DocumentViewer/en/45-1/bill/C-15/royal-assent'),
 ('TAX-07', 'investment/productivity', 'Productivity Super-Deduction (reinstated Accelerated Investment Incentive, immediate expensing for M&P buildings, productivity-enhancing assets, SR&ED capex, clean energy, ZEVs)', '2025-11-04',
  'Bill C-15 (most items); C-31 (immediate expensing of M&P buildings, still in committee)', '2026-03-26 (C-15)', 'various (assets acquired 2025-01-01 / 2025-11-04 onward)',
  'partially_implemented', 'Govt: lowers Canada\'s marginal effective tax rate (METR) by more than 2 points; lowest METR in G7. PBO: cost $19.4B over 5 yrs (mostly timing/deferral)',
  'ind_gdp_investment_quarterly.csv: real_business_nonres_investment, real_business_machinery_equipment', 'Too early for investment response; business M&E investment fell through 2025 and rebounded in 2026Q2 (tariff, oil-price and uncertainty effects dominate).',
  f'{B25}chap1-en.html ; {LG}c-15 ; {LG}c-31 ; {PBO}LEG-2526-012-S--accelerated-capital-cost-allowance-immediate-expensing-measures--mesures-deduction-amortissement-accelere-passation-charges-immediate'),
 ('TAX-08', 'investment/innovation', 'SR&ED enhancements (higher expenditure limit for 35% credit, eligibility for public corporations)', '2025-11-04',
  'Bill C-15 Part 1 (k)', '2026-03-26', 'per C-15', 'legislated_in_force', 'n/a', 'n/a', '', f'{LG}c-15 ; {PBO}LEG-2526-011-S--enhancing-scientific-research-experimental-development-tax-incentives--bonifier-encouragements-fiscaux-recherche-scientifique-developpement-experimental'),
 ('TAX-09', 'affordability', 'Temporary suspension of federal fuel excise tax (gasoline 10c/L, diesel 4c/L)', '2026-04-14',
  'Bill C-30 Part 2 (Apr 20 - Sep 7, 2026); extension in Bill C-38 to 2027-01-31 then 50% to 2027-03-31', '2026-06-18 (C-30); C-38 not yet passed', '2026-04-20',
  'partially_implemented', 'PBO: $4.9B in 2026-27 for both phases; ~$276 per family', 'ind_prices_monthly.csv: cpi_gasoline_yoy',
  'Response to 2026 Middle East oil shock. Extension (announced 2026-09-02) is at report stage in the House as of 2026-10-10; C-38 deems it in force from 2026-09-08.',
  f'{SEU}chap2-en.html ; {LG}c-30 ; {LG}c-38 ; {PBO}ES-2627-004-S--pbo-update-temporarily-suspending-federal-fuel-excise-tax--mise-jour-bdpb-suspension-temporaire-taxe-accise-federale-carburant'),
 ('AFF-01', 'affordability', 'Canada Groceries and Essentials Benefit (top-up to GST credit)', '2026-01-26',
  'Bill C-19', '2026-02-12', '2026 payments (from June per SEU)', 'legislated_in_force', 'Govt: >12 million people; up to $1,890 this year for a family of four', 'n/a', '',
  f'{LG}c-19 ; {SEU}intro-en.html ; {PBO}LEG-2526-010-S--canada-groceries-essentials-benefit--allocation-canadienne-epicerie-besoins-essentiels'),
 ('AFF-02', 'affordability', 'Reduce CPP contribution rate from 2027', '2026-04-28', 'Bill C-30 Part 3 Div 5', '2026-06-18', '2027-01-01', 'legislated_in_force (not yet effective)', 'n/a', 'n/a', 'Too early.', f'{LG}c-30 ; https://www.parl.ca/DocumentViewer/en/45-1/bill/C-30/royal-assent'),
 ('HOU-01', 'housing', 'Build Canada Homes (federal agency/Crown corp to build non-market and affordable housing; $13B initial capitalization)', '2025-09-14 (launch; campaign pledge Mar 2025)',
  'Funding: Bill C-15 Part 5 Div 3 (up to $11.5B + $1.515B to Canada Lands); Crown corporation: Bill C-20 (Build Canada Homes Act)', '2026-03-26 (C-15); 2026-06-18 (C-20)', '2025-09-14 (operating as interim agency)',
  'legislated_in_force', 'Govt: part of national effort to "double the pace of housing construction over the next decade" (campaign: nearly 500,000 homes/yr). PBO: BCH adds ~26,000 units over 5 yrs (+2.1% vs baseline completions; 3.7% of 690,000-unit 2035 gap)',
  'ind_housing_monthly.csv, ind_housing_annual.csv', 'SEU 2026 dropped explicit pace targets (PBO NT-2627-003). PBO: total federal housing program spending falls 56% from $9.8B (2025-26) to $4.3B (2028-29).',
  f'https://www.canada.ca/en/housing-infrastructure-communities/news/2025/09/the-honourable-joel-lightbound-highlights-the-launch-of-build-canada-homes-to-supercharge-homebuilding-across-the-country.html ; {LG}c-20 ; {LG}c-15 ; {PBO}RP-2526-020-S--build-canada-homes-outlook-housing-programs-under-budget-2025--maisons-canada-perspectives-entourant-programmes-logement-dans-cadre-budget-2025'),
 ('HOU-02', 'housing', 'GST rebate for first-time buyers on new homes (100% up to $1M; phased out $1M-$1.5M)', '2025-05-27',
  'Bill C-4 Part 2', '2026-03-12', 'homes sold after 2025-05-27 and before 2031', 'legislated_in_force', 'Govt: saves up to $50,000. PBO cost: $1.9B over 6 years', 'ind_housing_monthly.csv (no direct take-up data)', 'No public take-up data found.',
  f'{LG}c-4 ; {PBO}RP-2526-001-S--introducing-gst-rebates-first-time-home-buyers--remboursement-tps-acheteurs-une-premiere-habitation'),
 ('HOU-03', 'housing', 'Development-charge reductions via federal-provincial deals ($1.7B to provinces/territories; e.g. Ontario cuts DCs up to 50% for 3 years)', '2026-03 (SEU/Bill C-26)',
  'Bill C-26 (payments out of CRF to improve housing supply)', '2026-06-18', 'per bilateral agreements', 'partially_implemented',
  'Jurisdictions with highest DCs required to cut them for 3 years', 'ind_housing_monthly.csv', 'Too early for supply effects.', f'{SEU}chap2-en.html ; {LG}c-26'),
 ('ECO-01', 'internal trade / major projects', 'One Canadian Economy Act (Bill C-5): Free Trade and Labour Mobility in Canada Act + Building Canada Act', '2025-06-06',
  'Bill C-5 (S.C. 2025, c. 2)', '2025-06-26', '2025-06-26', 'legislated_in_force',
  'Remove federal barriers to internal trade; fast-track "national interest projects" (Schedule 1 of Building Canada Act)',
  'No post-2025 interprovincial trade data yet (StatCan annual interprovincial trade lags ~2 yrs)',
  'As of the Justice Laws consolidation current to 2026-09-21, Schedule 1 (national interest projects) is EMPTY. Gazette notices of intent (30-day) published for West Coast Oil Pipeline (2026-08-01), Mackenzie Valley Highway and Roberts Bank Terminal 2 (2026-08-29, per secondary source), and Deep Geological Repository (2026-09-19). Bill C-39 (Sept 2026) would amend the Building Canada Act and Impact Assessment Act.',
  f'{LG}c-5 ; {BCA} ; https://gazette.gc.ca/rp-pr/p1/2026/2026-08-01/html/sup1-eng.html ; https://gazette.gc.ca/rp-pr/p1/2026/2026-09-19/html/notice-avis-eng.html ; {LG}c-39'),
 ('ECO-02', 'major projects', 'Major Projects Office (MPO) and referred "nation-building" projects', '2025 (MPO launch); first 5 projects referred 2025-09-11 (media)',
  'Administrative (Privy Council Office); designation power under Building Canada Act', 'n/a', '2025', 'ongoing',
  'Govt (SEU 2026): 15 projects referred, >$125B capital, >60,000 construction jobs', 'n/a (project-level)',
  'Referral to MPO is not the same as designation under the Building Canada Act (none designated as of 2026-09-21).',
  f'https://www.canada.ca/en/privy-council/major-projects-office/projects/national.html ; {SEU}chap1-en.html ; {PBO}NT-2627-003-S--pbo-assessment-spring-economic-update-government-major-capital-priorities--evaluation-dpb-mise-jour-economique-printemps-principales-priorites-investissement-gouvernement-dans'),
 ('ECO-03', 'internal trade', 'Remove federal exceptions to the Canadian Free Trade Agreement; mutual recognition of goods; labour mobility service standard', '2025-06',
  'Administrative + FTLMCA (C-5)', '2025-06-26 (C-5)', '2025-06', 'partially_implemented',
  'Govt: all 53 federal CFTA exceptions removed by June 2025; FPT agreement to sign Canadian Mutual Recognition Agreement on Sale of Goods; IMF: removing internal frictions could lift real GDP by up to ~7% over time',
  'none available yet', 'Provincial barriers (the bulk) depend on provinces.', f'{SEU}chap1-en.html ; https://www.imf.org/en/news/articles/2025/12/05/cs-canada-staff-concluding-statement-of-the-2025-article-iv-mission'),
 ('DEF-01', 'defence', 'Reach NATO 2% of GDP in 2025-26 (~$9B additional)', '2025-06-09',
  'Supplementary Estimates (A) 2025-26 / Appropriation Acts; Budget 2025 ($81.8B over 5 yrs)', 'Appropriation Acts 2025-26 (C-6/C-7 2025-06-26; C-17 2025-12-11; C-23 2026-03-26)', '2025-26',
  'implemented', 'Core defence >= 2% GDP in 2025', 'defence_spending.csv (NATO: 2.03% 2025e; 2.13% 2026e)',
  'NATO measures on a cash, calendar-year basis; federal accounts are accrual, fiscal-year.',
  f'{NATO26} ; {B25}chap4-en.html ; {PBO}NT-2627-013-S--canada-defence-spending-commitments-primer--engagements-canada-matiere-depenses-defense-guide-introduction'),
 ('DEF-02', 'defence', 'NATO Hague commitment: 5% of GDP by 2035 (3.5% core + up to 1.5% ancillary); Canada interim 4% total (2.5% core) by 2030', '2025-06-25',
  'Political commitment; Defence Investment Agency (announced 2025-10-02); Bill C-40 (defence procurement, introduced 2026-10-06)', 'n/a', 'n/a', 'announced',
  '3.5% core by 2035; PBO: requires ~$33.5B/yr extra cash on average; core spending ~$159-164B in 2035-36; +$63B deficit and +6.3 pts debt/GDP in 2035-36',
  'defence_spending.csv', 'No published federal spending path to 5% (PBO).',
  f'{PBO}RP-2526-022-S--fiscal-implications-meeting-nato-5-commitment--repercussions-financieres-atteinte-cible-5-otan ; {PBO}NT-2627-013-S--canada-defence-spending-commitments-primer--engagements-canada-matiere-depenses-defense-guide-introduction ; {LG}c-40'),
 ('FIS-01', 'fiscal', 'Budget 2025: capital vs operating budget framework; anchors = balance operating budget by 2028-29 and declining deficit-to-GDP', '2025-11-04',
  'Budget 2025 (tabled); implementation Acts C-15 (RA 2026-03-26), C-31 (in committee)', '2026-03-26 (C-15)', '2025-11-04', 'ongoing',
  'Operating balance by 2028-29; deficit/GDP 2.5% (2025-26) -> 1.5% (2029-30)', 'fiscal_projections_vs_results.csv',
  'PBO: govt definition of capital is "overly expansive" (~$94B or 30% larger than PBO definition over 2024-25 to 2029-30); 7.5% chance deficit/GDP falls every year; IMF recommends debt anchor.',
  f'{B25}overview-apercu-en.html ; {PBO}RP-2526-017-S--budget-2025-issues-parliamentarians--budget-2025-enjeux-parlementaires ; {PBO}RP-2627-010-S--government-operating-budget-fiscal-anchor--cible-budget-fonctionnement-gouvernement'),
 ('FIS-02', 'fiscal/public service', 'Comprehensive Expenditure Review: $60B savings+revenues over 5 yrs; ~$13B/yr by 2028-29; public service down ~40,000 positions (~10%) by 2028-29 vs 2023-24', '2025 (review); 2025-11-04 (Budget 2025)',
  'Budget 2025; C-15 Divs 6-7 (early retirement options for workforce reduction)', '2026-03-26', '2026-27 onward', 'ongoing',
  '~40,000 fewer positions by 2028-29', 'PBO Federal Employment Tracking Tool: FTEs 367k peak (Jun 2024) -> 342k (Jun 2026); FY avg 358k (2024-25) -> 344k (2025-26)',
  'Positions vs FTEs are not identical concepts; decline began before Budget 2025 (from mid-2024).',
  f'{B25}intro-en.html ; {PBO}NT-2627-011-S--pbo-releases-federal-employment-tracking-tool--bdpb-lance-outil-suivi-emploi-federal'),
 ('IMM-01', 'immigration', '2026-2028 Immigration Levels Plan: PR 380,000/yr; new temporary resident arrivals 385,000 (2026), 370,000 (2027-28) vs 673,650 in 2025; NPRs < 5% of population by end-2027', '2025-11-04 (Budget 2025)',
  'Levels Plan (administrative); C-12 (Strengthening Canada\'s Immigration System and Borders Act, RA 2026-03-26)', '2026-03-26 (C-12)', '2026-01-01', 'ongoing',
  'NPR share < 5% by end of 2027; PR < 1% of population beyond 2027', 'ind_population_quarterly.csv; ind_npr_quarterly.csv',
  'TR reductions began under the previous government (2024 caps, 2025-2027 plan). PBO projects NPR share just under 5% by end-2027 and ~flat population in 2026.',
  f'{B25}chap1-en.html ; {PBO}RP-2526-025-S--demographic-implications-2026-2028-immigration-levels-plan--implications-demographiques-plan-niveaux-immigration-2026-2028 ; {LG}c-12'),
 ('TRD-01', 'trade', 'Counter-tariffs on US goods (25% on ~$30B from 2025-03-04; on ~$29.8B steel/aluminum/other from 2025-03-13; autos from 2025-04-09)', '2025-03-04 (pre-Carney; continued)',
  'Orders under Customs Tariff (United States Surtax Order 2025 etc.); remission order 2025-04-16', 'n/a', '2025-03-04', 'reversed (mostly, 2025-09-01); steel/aluminum/auto measures kept',
  'Pressure US to remove tariffs', 'ind_prices_monthly.csv; ind_trade_monthly.csv',
  'Budget 2025: $6.7B gross revenue assessed to 2025-10-17, $3.0B remitted, net $3.7B. First measures predate Carney (sworn in 2025-03-14).',
  f'{TAR25} ; {B25}anx1-en.html'),
 ('TRD-02', 'trade', 'Removal of most counter-tariffs on US goods (kept steel, aluminum, autos)', '2025-08-22',
  'Customs Tariff orders', 'n/a', '2025-09-01', 'implemented', 'n/a', 'ind_trade_monthly.csv', '', TAR25),
 ('TRD-03', 'trade', 'New counter-tariffs after Canada-US talks broke down: 15/25/50% surtaxes on $27.6B of US goods + $7.5B support package', '2026-08-25',
  'United States Surtax Order (2026) (per secondary sources); CBSA notice', 'n/a', '2026-09-08', 'implemented',
  'Mirror US 50% tariff on $27.6B of Canadian goods effective 2026-08-22', 'too early (Sept 2026 trade data not yet released)', 'Support package on top of ~$25B earlier tariff supports.', TAR26),
 ('TRD-04', 'trade', 'Buy Canadian procurement policy', '2025-09-05', 'Treasury Board / PSPC policy', 'n/a', '2025-12-16', 'implemented',
  'Federal departments and Crown corps (e.g. BCH) to prefer Canadian suppliers', 'n/a', 'Effective date from media (CTV).',
  'https://www.pm.gc.ca/en/node/51831 ; https://www.ctvnews.ca/politics/article/buy-canadian-policy-comes-into-effect-changing-rules-for-federal-procurement/ (media)'),
 ('TRD-05', 'trade diversification', 'Double non-US exports (goods and services) over the next decade (+$300B); $6B Trade Infrastructure Strategy incl. $5B Trade Diversification Corridors Fund', '2025-11-04',
  'Budget 2025; SEU 2026', 'n/a', 'n/a', 'ongoing', 'Double non-US exports by ~2035', 'ind_trade_monthly.csv; ind_trade_annual.csv',
  'Govt: non-US goods+services exports +$33B in 2025 vs 2024. Goods-only StatCan data show non-US share rising; much of the rise is gold (UK) and crude oil (price effects, TMX opened May 2024).',
  f'{SEU}overview-apercu-en.html ; {SEU}chap1-en.html'),
 ('TRD-06', 'trade agreements', 'Canada-Indonesia CEPA', 'n/a (bill introduced 2025-12-11)', 'Bill C-18', '2026-05-06', 'per order in council', 'legislated_in_force', 'n/a', 'n/a', '', f'{LG}c-18'),
 ('TRD-07', 'trade agreements', 'UK accession to CPTPP (implementation)', 'n/a', 'Bill C-13', '2026-05-06', 'per order', 'legislated_in_force', 'n/a', 'n/a', '', f'{LG}c-13'),
 ('TRD-08', 'trade agreements', 'Canada-China strategic partnership (China to reduce/suspend tariffs on key agri-food and seafood); other FTA talks (India by end-2026, UAE, ASEAN, Philippines, Mercosur)', 'before 2026-04-28 (per SEU; exact date unverified)',
  'Executive agreements / negotiations', 'n/a', 'n/a', 'ongoing', 'n/a', 'ind_trade_monthly.csv: goods_exports_china', 'Details of China arrangement not verified from primary text.', f'{SEU}chap1-en.html'),
 ('COM-01', 'competition/productivity', 'Consumer-Driven Banking Act (open banking) re-enacted', '2025-11-04', 'Bill C-15 Part 5 Div 9', '2026-03-26', 'phased', 'legislated_in_force', 'n/a', 'n/a', '', f'{LG}c-15'),
 ('COM-02', 'competition/labour', 'Ban most non-compete clauses (federally regulated employers)', '2025-11-04', 'Bill C-31 Part 4 Div 9', '', '', 'in_parliament', 'n/a', 'n/a', 'C-31 at committee in House as of 2026-10-10.', f'{LG}c-31'),
 ('COM-03', 'competition', 'Competition Act: remove internationally-recognized-methodology requirement for environmental claims (greenwashing provisions)', '2025-11-04', 'Bill C-15 Part 5 Div 43', '2026-03-26', '', 'legislated_in_force', 'n/a', 'n/a', '', f'{LG}c-15'),
 ('COM-04', 'regulation', 'Red Tape Reduction Act amendments (temporary regulatory exemptions/sandboxes)', '2025-11-04', 'Bill C-15 Part 5 Div 5', '2026-03-26', '', 'legislated_in_force', 'n/a', 'n/a', '', f'{LG}c-15'),
 ('COM-05', 'regulation/major projects', 'Building Canada Strong Act (Impact Assessment Act, Building Canada Act, CER Act, Fisheries Act, Canada Marine Act amendments; Enabling Digital Trade Act)', '2026-09-21', 'Bill C-39', '', '', 'in_parliament', '"One project, one review"', 'n/a', 'At committee in House as of 2026-10-10.', f'{LG}c-39'),
 ('COM-06', 'infrastructure', 'High-Speed Rail Network Act (Alto, Quebec-Ontario)', '2025-11-04', 'Bill C-15 Part 5 Div 1', '2026-03-26', '', 'legislated_in_force', 'n/a', 'n/a', 'PBO cost analysis RP-2627-009-C (2026-10-01).', f'{LG}c-15'),
 ('ENE-01', 'energy/climate', 'Canada-Alberta MOU: support for a West Coast bitumen pipeline; proposed oil & gas emissions cap dropped; stronger industrial carbon price (TIER) and Pathways CCS', '2025-11-27',
  'MOU (non-legislative); Building Canada Act Gazette notice for West Coast Oil Pipeline (2026-08-01)', 'n/a', 'n/a', 'announced',
  'Industrial carbon price path (sources differ: ~$130/t by 2035)', 'n/a', 'MOU details from media/secondary sources; verify against official text.',
  'https://www.ctvnews.ca/politics/article/carney-signs-major-energy-agreement-with-alberta-laying-out-conditions-for-new-oil-pipeline/ (media) ; https://gazette.gc.ca/rp-pr/p1/2026/2026-08-01/html/sup1-eng.html'),
]

with open(os.path.join(OUT, 'policy_inventory.csv'), 'w', newline='') as f:
    w = csv.writer(f); w.writerow(INV_COLS)
    for r in INV:
        assert len(r) == len(INV_COLS), r[0]
        w.writerow(r)

# ---------- fiscal projections vs results ----------
FIS_COLS = ['fiscal_year', 'indicator', 'value', 'unit', 'vintage', 'vintage_date', 'kind', 'source_url', 'note']
F = []
def fis(fy, ind, v, unit, vint, vd, kind, url, note=''):
    F.append((fy, ind, v, unit, vint, vd, kind, url, note))
yrs = ['2024-25', '2025-26', '2026-27', '2027-28', '2028-29', '2029-30']
for fy, v in zip(yrs, [-48.3, -42.2, -31.0, -30.4, -27.8, -23.0]):
    fis(fy, 'budgetary_balance', v, '$ billions', 'FES 2024 (as reported in Budget 2025 Table 1)', '2024-12-16', 'projection', f'{B25}overview-apercu-en.html')
for fy, v in zip(yrs, [-36.3, -78.3, -65.4, -63.5, -57.9, -56.6]):
    fis(fy, 'budgetary_balance', v, '$ billions', 'Budget 2025', '2025-11-04', 'projection' if fy != '2024-25' else 'actual (Public Accounts)', f'{B25}overview-apercu-en.html')
for fy, v in zip(yrs, [-1.2, -2.5, -2.0, -1.9, -1.6, -1.5]):
    fis(fy, 'budgetary_balance_pct_gdp', v, '% of GDP', 'Budget 2025', '2025-11-04', 'projection', f'{B25}overview-apercu-en.html')
for fy, v in zip(yrs, [41.2, 42.4, 43.1, 43.3, 43.3, 43.1]):
    fis(fy, 'federal_debt_pct_gdp', v, '% of GDP', 'Budget 2025', '2025-11-04', 'projection', f'{B25}overview-apercu-en.html')
yrs7 = yrs + ['2030-31']
for fy, v in zip(yrs7, [-36.3, -66.9, -65.3, -63.1, -57.7, -56.2, -53.2]):
    fis(fy, 'budgetary_balance', v, '$ billions', 'Spring Economic Update 2026', '2026-04-28', 'projection', f'{SEU}overview-apercu-en.html')
for fy, v in zip(yrs7, [-1.2, -2.1, -1.9, -1.8, -1.6, -1.5, -1.4]):
    fis(fy, 'budgetary_balance_pct_gdp', v, '% of GDP', 'Spring Economic Update 2026', '2026-04-28', 'projection', f'{SEU}overview-apercu-en.html')
for fy, v in zip(yrs7, [40.7, 41.1, 41.5, 41.8, 41.9, 41.8, 41.6]):
    fis(fy, 'federal_debt_pct_gdp', v, '% of GDP', 'Spring Economic Update 2026', '2026-04-28', 'projection', f'{SEU}overview-apercu-en.html')
fis('2025-26', 'budgetary_balance', -72.0, '$ billions', 'PBO Economic and Fiscal Outlook June 2026', '2026-06-04', 'independent projection', f'{PBO}RP-2627-002-S--economic-fiscal-outlook-june-2026--perspectives-economiques-financieres-juin-2026')
fis('2030-31', 'budgetary_balance', -58.2, '$ billions', 'PBO Economic and Fiscal Outlook June 2026', '2026-06-04', 'independent projection', f'{PBO}RP-2627-002-S--economic-fiscal-outlook-june-2026--perspectives-economiques-financieres-juin-2026')
fis('2025-26', 'federal_debt_pct_gdp', 41.3, '% of GDP', 'PBO Economic and Fiscal Outlook June 2026', '2026-06-04', 'independent projection', f'{PBO}RP-2627-002-S--economic-fiscal-outlook-june-2026--perspectives-economiques-financieres-juin-2026')
fis('2030-31', 'federal_debt_pct_gdp', 42.5, '% of GDP', 'PBO Economic and Fiscal Outlook June 2026', '2026-06-04', 'independent projection', f'{PBO}RP-2627-002-S--economic-fiscal-outlook-june-2026--perspectives-economiques-financieres-juin-2026')
fis('2025-26', 'budgetary_balance', -55.3, '$ billions', 'Fiscal Monitor March 2026 (Apr 2025-Mar 2026)', '2026-05-29', 'preliminary actual (unaudited; before year-end adjustments)', FM03,
    'Prior 3 years post-March adjustments moved the final deficit by -6.8 (2024-25), +10.9 (2023-24), -6.0 (2022-23) $B. Same report: 2024-25 preliminary -43.2 vs final -36.3.')
fis('2025-26', 'revenues', 500.0, '$ billions', 'Fiscal Monitor March 2026', '2026-05-29', 'preliminary actual', FM03)
fis('2025-26', 'program_expenses_excl_net_actuarial_losses', 487.9, '$ billions', 'Fiscal Monitor March 2026', '2026-05-29', 'preliminary actual', FM03)
fis('2025-26', 'public_debt_charges', 53.7, '$ billions', 'Fiscal Monitor March 2026', '2026-05-29', 'preliminary actual', FM03)
fis('2026-27 (Apr-Jul)', 'budgetary_balance_ytd', -5.1, '$ billions', 'Fiscal Monitor July 2026', '2026-09-25', 'year-to-date actual', FM07, 'Apr-Jul 2025: -7.8. Monthly results are seasonal; not comparable to the annual projection.')
fis('2025-26 (Apr-Jul)', 'budgetary_balance_ytd', -7.8, '$ billions', 'Fiscal Monitor July 2026', '2026-09-25', 'year-to-date actual', FM07)
with open(os.path.join(OUT, 'fiscal_projections_vs_results.csv'), 'w', newline='') as f:
    w = csv.writer(f); w.writerow(FIS_COLS); w.writerows(F)

# ---------- defence ----------
D_COLS = ['year', 'indicator', 'value', 'unit', 'basis', 'source', 'source_url']
D = []
n25 = dict(zip(range(2014, 2026), [1.01, 1.20, 1.16, 1.44, 1.30, 1.29, 1.41, 1.27, 1.20, 1.33, 1.47, 2.01]))
n26 = {2014: 1.01, 2015: 1.20, 2016: 1.16, 2017: 1.44, 2018: 1.30, 2019: 1.29, 2020: 1.41, 2021: 1.26, 2022: 1.18, 2023: 1.29, 2024: 1.41, 2025: 2.03, 2026: 2.13}
for y, v in n26.items():
    D.append((y, 'core_defence_pct_gdp', v, '% of GDP (real, 2021 prices)', 'NATO definition, calendar year, cash; 2025-2026 estimates', 'NATO Defence Investment of NATO Countries (2014-2026), cut-off 2026-07-03, Table 3', NATO26))
for y, v in n25.items():
    D.append((y, 'core_defence_pct_gdp_vintage2025', v, '% of GDP (real, 2021 prices)', 'NATO definition; 2024-2025 estimates', 'NATO Defence Expenditure of NATO Countries (2014-2025), cut-off 2025-06-03, Table 3', NATO25))
for y, v in {2022: 33707, 2023: 38385, 2024: 43917, 2025: 65901, 2026: 71517}.items():
    D.append((y, 'core_defence_spending_nominal', v, 'C$ millions, current prices', 'NATO definition; 2025-2026 estimates', 'NATO 2026 report, Table 1', NATO26))
for y, v, b in [(2025, 2.0, 'Government commitment for FY2025-26'), (2030, 2.5, 'Core target implied by Canada 4% total commitment (PBO)'), (2035, 3.5, 'NATO Hague core target')]:
    D.append((y, 'core_defence_target_pct_gdp', v, '% of GDP', b, 'PBO NT-2627-013-S Table 1', f'{PBO}NT-2627-013-S--canada-defence-spending-commitments-primer--engagements-canada-matiere-depenses-defense-guide-introduction'))
for y, v in [(2030, 4.0), (2035, 5.0)]:
    D.append((y, 'total_defence_target_pct_gdp', v, '% of GDP', 'core + ancillary (ancillary up to 1.5%)', 'PBO NT-2627-013-S Table 1', f'{PBO}NT-2627-013-S--canada-defence-spending-commitments-primer--engagements-canada-matiere-depenses-defense-guide-introduction'))
with open(os.path.join(OUT, 'defence_spending.csv'), 'w', newline='') as f:
    w = csv.writer(f); w.writerow(D_COLS); w.writerows(D)
print('ok', len(INV), len(F), len(D))
