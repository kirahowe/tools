"""targets_vs_actuals.csv: official targets/projections vs latest actuals. Actuals pulled from tidy CSVs where possible.
Usage: python3 -I build_targets.py <data_dir>
"""
import csv, os, sys
D = sys.argv[1]
B25 = 'https://budget.canada.ca/2025/report-rapport/'
SEU = 'https://budget.canada.ca/update-miseajour/2026/report-rapport/'
PBO = 'https://www.pbo-dpb.ca/en/publications/'
SC = 'https://www150.statcan.gc.ca/t1/tbl1/en/tv.action?pid='


def get(fn, ind, date=None):
    rows = [r for r in csv.DictReader(open(os.path.join(D, fn))) if r['indicator'] == ind]
    if date:
        rows = [r for r in rows if r['date'] == date]
    r = rows[-1]
    return float(r['value']), r['date'][:7]


hs25, _ = get('ind_housing_annual.csv', 'housing_starts_annual_total', '2025-01-01')
hs24, _ = get('ind_housing_annual.csv', 'housing_starts_annual_total', '2024-01-01')
saar = [float(r['value']) for r in csv.DictReader(open(os.path.join(D, 'ind_housing_monthly.csv'))) if r['indicator'] == 'housing_starts_saar' and r['date'] >= '2026-01-01']
last_hs = max(r['date'] for r in csv.DictReader(open(os.path.join(D, 'ind_housing_monthly.csv'))))[:7]
hs26 = sum(saar) / len(saar)
npr, npr_d = get('ind_npr_quarterly.csv', 'npr_share_of_population')
popg, popg_d = get('ind_population_quarterly.csv', 'population_growth_yoy')
cpi, cpi_d = get('ind_prices_monthly.csv', 'cpi_all_items_yoy')
cpix, _ = get('ind_prices_monthly.csv', 'cpi_ex_food_energy_yoy')
nus25, _ = get('ind_trade_annual.csv', 'goods_exports_non_us_annual', '2025-01-01')
nus24, _ = get('ind_trade_annual.csv', 'goods_exports_non_us_annual', '2024-01-01')
nus_ytd, _ = get('ind_trade_annual.csv', 'goods_exports_non_us_ytd_growth_vs_prior_year')
gdp_pc_q1, _ = get('ind_gdp_investment_quarterly.csv', 'real_gdp_per_capita', '2025-01-01')
gdp_pc, gdp_pc_d = get('ind_gdp_investment_quarterly.csv', 'real_gdp_per_capita')
lp_q1, _ = get('ind_productivity_quarterly.csv', 'business_labour_productivity_index', '2025-01-01')
lp, lp_d = get('ind_productivity_quarterly.csv', 'business_labour_productivity_index')

COLS = ['target_id', 'area', 'metric', 'target_value', 'target_unit', 'target_period', 'target_source', 'target_source_url',
        'actual_value', 'actual_unit', 'actual_period', 'actual_source', 'actual_source_url', 'assessment', 'caveat']
T = [
 ('HOU-T1', 'housing', 'Housing starts vs "double the pace" goal', 500000, 'units/yr (~double; campaign figure "nearly 500,000")', '~2035 (over next decade)',
  'Liberal platform 2025 (party, campaign); govt wording: "double the pace of housing construction over the next decade" (BCH launch, 2025-09-14)',
  'https://www.canada.ca/en/housing-infrastructure-communities/news/2025/09/the-honourable-joel-lightbound-highlights-the-launch-of-build-canada-homes-to-supercharge-homebuilding-across-the-country.html',
  round(hs25), 'units (actual, all areas)', '2025', 'StatCan/CMHC 34-10-0135-01 (sum of quarters)', SC + '3410013501',
  'off_pace_early', f'2024 = {round(hs24)}; Jan-{last_hs[5:]} 2026 average SAAR = {round(hs26)} (34-10-0158). No interim path published; SEU 2026 gives no pace target (PBO). Starts reflect pre-2025 approvals, rates, condo-market slump, slower population growth.'),
 ('HOU-T2', 'housing', 'CMHC supply-gap benchmark', '430000-480000', 'units/yr', 'next decade', 'Cited in Budget 2025 ch.3 (CMHC estimate to restore 2019 affordability)', B25 + 'chap3-en.html',
  round(hs25), 'units', '2025', '34-10-0135-01', SC + '3410013501', 'benchmark_not_target', 'PBO alternative benchmark: 290,000 units/yr to close housing gap (also cited in Budget 2025 ch.3).'),
 ('HOU-T3', 'housing', 'Build Canada Homes contribution (PBO estimate)', 26000, 'units over 5 years', '2025-26 to 2029-30', 'PBO RP-2526-020-S', PBO + 'RP-2526-020-S--build-canada-homes-outlook-housing-programs-under-budget-2025--maisons-canada-perspectives-entourant-programmes-logement-dans-cadre-budget-2025',
  '', '', '', 'No BCH unit-completion data published (govt: "thousands of units" committed, SEU 2026)', SEU + 'intro-en.html', 'too_early', '+2.1% vs PBO baseline completions; 3.7% of the 690,000-unit 2035 gap.'),
 ('DEF-T1', 'defence', 'Core defence spending, % GDP', 2.0, '% of GDP', '2025-26 (NATO: calendar 2025)', 'Govt announcement 2025-06-09; Budget 2025 ch.4', B25 + 'chap4-en.html',
  2.03, '% of GDP (NATO estimate)', '2025e (2026e: 2.13)', 'NATO Defence Investment of NATO Countries 2014-2026 (cut-off 2026-07-03)', 'https://www.nato.int/content/dam/nato/webready/documents/finance/def-exp-2026-en.pdf',
  'met_on_estimates', 'NATO estimates, cash & calendar basis; final figures may be revised. 2024 = 1.41%.'),
 ('DEF-T2', 'defence', 'Core defence % GDP (interim, total 4%)', 2.5, '% of GDP core (4% total)', '2030', 'Canada commitment (PBO primer Table 1)', PBO + 'NT-2627-013-S--canada-defence-spending-commitments-primer--engagements-canada-matiere-depenses-defense-guide-introduction',
  2.13, '% of GDP', '2026e', 'NATO 2026 report', 'https://www.nato.int/content/dam/nato/webready/documents/finance/def-exp-2026-en.pdf', 'too_early', 'No published federal year-by-year path (PBO).'),
 ('DEF-T3', 'defence', 'NATO Hague: core 3.5% + up to 1.5% ancillary', 5.0, '% of GDP total', '2035', 'NATO Hague Summit 2025-06-25', PBO + 'RP-2526-022-S--fiscal-implications-meeting-nato-5-commitment--repercussions-financieres-atteinte-cible-5-otan',
  2.13, '% of GDP core', '2026e', 'NATO 2026 report', 'https://www.nato.int/content/dam/nato/webready/documents/finance/def-exp-2026-en.pdf', 'too_early', 'PBO: +$63B deficit (1.4% GDP) and +6.3 pts debt/GDP in 2035-36 vs a 2% baseline.'),
 ('FIS-T1', 'fiscal', 'Federal budgetary balance 2025-26', -78.3, '$ billions', '2025-26', 'Budget 2025 (2025-11-04)', B25 + 'overview-apercu-en.html',
  -55.3, '$ billions (preliminary, unaudited)', 'Apr 2025-Mar 2026', 'Fiscal Monitor March 2026 (2026-05-29)', 'https://www.canada.ca/en/department-finance/services/publications/fiscal-monitor/2026/03.html',
  'better_than_projected_preliminary', 'SEU 2026 revised projection: -66.9; PBO June 2026: -72.0. Year-end adjustments historically moved results by -6.8 to +10.9 $B; final figure in Public Accounts 2026. FES 2024 had projected -42.2.'),
 ('FIS-T2', 'fiscal', 'Deficit-to-GDP declining every year (anchor)', '2.5 -> 1.5', '% of GDP', '2025-26 to 2029-30', 'Budget 2025', B25 + 'overview-apercu-en.html',
  '2.1 -> 1.4', '% of GDP (projection, not actual)', '2025-26 to 2030-31 (SEU)', 'SEU 2026', SEU + 'overview-apercu-en.html', 'too_early',
  'PBO (Nov 2025): 7.5% probability the ratio declines every year to 2029-30; PBO (May 2026): SEU on track to respect both anchors.'),
 ('FIS-T3', 'fiscal', 'Balance operating budget', 0, '$ (operating balance)', '2028-29', 'Budget 2025', B25 + 'intro-en.html',
  '', '', '', 'No actual yet; PBO notes classification of capital vs operating is contestable', PBO + 'RP-2627-010-S--government-operating-budget-fiscal-anchor--cible-budget-fonctionnement-gouvernement', 'too_early', 'PBO: govt capital definition ~30% ($94B) broader than PBO definition over 2024-25 to 2029-30.'),
 ('FIS-T4', 'fiscal', 'Federal debt-to-GDP 2025-26', 42.4, '% of GDP', '2025-26', 'Budget 2025', B25 + 'overview-apercu-en.html',
  41.1, '% of GDP (SEU estimate)', '2025-26', 'SEU 2026', SEU + 'overview-apercu-en.html', 'not_final', 'Final ratio awaits Public Accounts; PBO June 2026 estimates 41.3%.'),
 ('PS-T1', 'public service', 'Public service size', '-40,000 positions (~-10%)', 'positions vs 2023-24', '2028-29', 'Budget 2025 (as quoted by PBO)', PBO + 'NT-2627-011-S--pbo-releases-federal-employment-tracking-tool--bdpb-lance-outil-suivi-emploi-federal',
  344000, 'FTEs (fiscal-year average)', '2025-26 (2024-25: 358,000; June 2026 monthly: 342,000)', 'PBO Federal Employment Tracking Tool note (2026-09-15)', PBO + 'NT-2627-011-S--pbo-releases-federal-employment-tracking-tool--bdpb-lance-outil-suivi-emploi-federal',
  'moving_in_direction', 'Positions vs FTEs differ; decline started mid-2024 (peak 367k FTE June 2024), i.e. before Budget 2025.'),
 ('IMM-T1', 'immigration', 'Non-permanent residents share of population', 5.0, '% of population (below)', 'end of 2027', 'Budget 2025 ch.1 / 2026-2028 Levels Plan', B25 + 'chap1-en.html',
  npr, '% of population', npr_d, 'StatCan 17-10-0121-01 / 17-10-0009-01 (derived)', SC + '1710012101', 'moving_in_direction', 'PBO projects just under 5% by end-2027. Peak ~7.5% (2024, Budget 2025) / 7.6% (PBO).'),
 ('IMM-T2', 'immigration', 'Population growth (implied by levels plan)', 'about 0 in 2026; 0.3% in 2027 (PBO projection)', '% per year', '2026-2027', 'PBO RP-2526-025-S (projection, not govt target)', PBO + 'RP-2526-025-S--demographic-implications-2026-2028-immigration-levels-plan--implications-demographiques-plan-niveaux-immigration-2026-2028',
  popg, '% y/y', popg_d, 'StatCan 17-10-0009-01', SC + '1710000901', 'consistent_with_projection', 'Pop growth was ~3% y/y in 2023-24.'),
 ('TRD-T1', 'trade', 'Double non-US exports (goods+services) over next decade', 'x2 (~+$300B)', 'C$', '~2035', 'Budget 2025 / SEU 2026', SEU + 'overview-apercu-en.html',
  round(nus25), 'C$ millions, goods only, BOP basis', '2025 (2024: %d)' % round(nus24), 'StatCan 12-10-0011-01 (derived)', SC + '1210001101',
  'ahead_of_linear_path_goods_only', f'Goods-only non-US exports +{(nus25/nus24-1)*100:.1f}% in 2025; Jan-Aug 2026 +{nus_ytd:.1f}% y/y. A doubling over 10 yrs needs ~7.2%/yr. Gains are concentrated in gold (UK) and crude oil (price; TMX opened 2024), i.e. not clearly policy-driven. Govt: goods+services non-US exports +$33B in 2025.'),
 ('MAC-T1', 'inflation', 'CPI inflation (Bank of Canada target, not govt)', 2.0, '% y/y', 'ongoing', 'Bank of Canada 2% target', 'https://www.bankofcanada.ca/2026/09/fad-press-release-2026-09-02/',
  cpi, '% y/y', cpi_d, 'StatCan 18-10-0004-01', SC + '1810000401', 'above_target', f'CPI ex food & energy {cpix}% ({cpi_d}). 2026 overshoot driven by gasoline (Middle East conflict) despite fuel-excise suspension.'),
 ('MAC-T2', 'growth', 'Real GDP per capita (no explicit target)', '', '', '', 'Govt aim: "strongest economy in the G7" (Budget 2025/SEU)', SEU + 'overview-apercu-en.html',
  round(gdp_pc), 'chained 2017 $ per person', gdp_pc_d, '36-10-0104 / 17-10-0009 (derived)', SC + '3610010401', 'flat', f'2025Q1 = {round(gdp_pc_q1)}; change {(gdp_pc/gdp_pc_q1-1)*100:.2f}% since.'),
 ('MAC-T3', 'productivity', 'Business-sector labour productivity (no explicit target)', '', '', '', 'Budget 2025 productivity measures', B25 + 'chap1-en.html',
  lp, 'index 2017=100', lp_d, 'StatCan 36-10-0206-01', SC + '3610020601', 'flat', f'2025Q1 = {lp_q1}; change {(lp/lp_q1-1)*100:.2f}% since.'),
 ('ECO-T1', 'major projects', 'National-interest designations under Building Canada Act', '', 'projects in Schedule 1', '', 'Bill C-5 (RA 2025-06-26)', 'https://www.parl.ca/legisinfo/en/bill/45-1/c-5',
  0, 'projects listed in Schedule 1', 'consolidation current to 2026-09-21', 'Justice Laws (Building Canada Act full text)', 'https://laws-lois.justice.gc.ca/eng/acts/B-9.89/FullText.html', 'none_designated_yet',
  'Govt (SEU 2026): 15 projects referred to MPO, >$125B. Gazette notices of intent (2026-08 to 2026-09) for West Coast Oil Pipeline, DGR and others.'),
 ('TAX-T1', 'tax', 'Middle-class tax cut: cost', 28.24, '$ billions net, 2025-26 to 2029-30 (PBO)', '5 years', 'PBO LEG-2526-002-S', PBO + 'LEG-2526-002-S--reducing-lowest-federal-personal-income-tax-rate-14-per-cent--reduire-14-taux-plus-bas-impot-federal-revenu-particuliers',
  '', '', '', 'Finance Canada: >$27B over five years (news release 2025-05)', 'https://www.canada.ca/en/department-finance/news/2025/05/delivering-a-middle-class-tax-cut.html', 'not_an_outcome', 'Cost estimates only; no ex-post data.'),
]
with open(os.path.join(D, 'targets_vs_actuals.csv'), 'w', newline='') as f:
    w = csv.writer(f); w.writerow(COLS)
    for r in T:
        assert len(r) == len(COLS), r[0]
        w.writerow(r)
print(len(T), npr, npr_d, popg, popg_d, cpi, cpi_d, hs26, gdp_pc, lp)
