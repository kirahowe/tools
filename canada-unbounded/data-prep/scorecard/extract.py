"""Build tidy indicator CSVs from StatCan full-table zips + BoC Valet CSV.
Usage: python3 -I extract.py <raw_dir> <out_dir>
Tidy columns: date,indicator,value,unit,source_table,regime
regime: pre (< Mar 2025), transition (Mar 2025 month / 2025Q1), post (>= Apr 2025 / >= 2025Q2)
"""
import zipfile, csv, io, sys, os
from collections import defaultdict

RAW, OUT = sys.argv[1], sys.argv[2]
START = '2021-01'  # keep 2021 so 2022 y/y can be computed; output trimmed to >= 2022-01


def rows(pid):
    z = zipfile.ZipFile(os.path.join(RAW, f'{pid}-eng.zip'))
    with z.open(f'{pid}.csv') as fh:
        for r in csv.DictReader(io.TextIOWrapper(fh, encoding='utf-8-sig')):
            yield r


def series(pid, pred, scale=1.0, start=START):
    d = {}
    for r in rows(pid):
        if r['REF_DATE'][:7] < start or r['VALUE'] in ('', None):
            continue
        if pred(r):
            k = r['REF_DATE'][:7]
            if k in d:
                raise SystemExit(f'duplicate {pid} {k}')
            d[k] = float(r['VALUE']) * scale
    return d


def shift(k, n):
    y, m = int(k[:4]), int(k[5:7])
    m += n
    while m <= 0:
        m += 12; y -= 1
    while m > 12:
        m -= 12; y += 1
    return f'{y:04d}-{m:02d}'


def pct(a, b):
    return (a / b - 1) * 100


def regime(k, freq):
    if freq == 'Q':
        return 'pre' if k < '2025-01' else ('transition' if k == '2025-01' else 'post')
    return 'pre' if k < '2025-03' else ('transition' if k == '2025-03' else 'post')


class W:
    def __init__(self, name, freq):
        self.name, self.freq, self.out = name, freq, []

    def add(self, d, ind, unit, src, nd=4):
        for k in sorted(d):
            if k >= '2022-01':
                self.out.append((k + '-01', ind, round(d[k], nd), unit, src, regime(k, self.freq)))

    def save(self):
        with open(os.path.join(OUT, self.name), 'w', newline='') as f:
            w = csv.writer(f)
            w.writerow(['date', 'indicator', 'value', 'unit', 'source_table', 'regime'])
            w.writerows(self.out)
        print(self.name, len(self.out))


def yoy(d, lag):
    return {k: pct(v, d[shift(k, -lag)]) for k, v in d.items() if shift(k, -lag) in d}


def qoq_ann(d):
    return {k: ((v / d[shift(k, -3)]) ** 4 - 1) * 100 for k, v in d.items() if shift(k, -3) in d}


# ---------------- GDP & investment (quarterly) ----------------
T = '36-10-0104-01'
sa = lambda r: r['Seasonal adjustment'].startswith('Seasonally') and r['Prices'] == 'Chained (2017) dollars'
gdp = series('36100104', lambda r: sa(r) and r['Estimates'] == 'Gross domestic product at market prices')
nrsme = series('36100104', lambda r: sa(r) and r['Estimates'] == 'Non-residential structures, machinery and equipment')
ipp = series('36100104', lambda r: sa(r) and r['Estimates'] == 'Intellectual property products')
me = series('36100104', lambda r: sa(r) and r['Estimates'] == 'Machinery and equipment')
resi = series('36100104', lambda r: sa(r) and r['Estimates'] == 'Residential structures')
xg = series('36100104', lambda r: sa(r) and r['Estimates'] == 'Exports of goods')
ggfcf = series('36100104', lambda r: sa(r) and r['Estimates'] == 'General governments gross fixed capital formation')
bnr = {k: nrsme[k] + ipp[k] for k in nrsme if k in ipp}

pop = series('17100009', lambda r: r['GEO'] == 'Canada', start='2020-10')
# population is a point estimate at quarter start (Jan 1, Apr 1, ...). Quarterly average approximated by
# mean of this quarter's start and next quarter's start; falls back to quarter-start value for the latest quarter.
popavg = {}
for k in gdp:
    nk = shift(k, 3)
    if k in pop:
        popavg[k] = (pop[k] + pop[nk]) / 2 if nk in pop else pop[k]
gdppc = {k: gdp[k] * 1e6 / popavg[k] for k in gdp if k in popavg}

q = W('ind_gdp_investment_quarterly.csv', 'Q')
q.add(gdp, 'real_gdp', 'millions of chained 2017 dollars, SAAR', T, 0)
q.add(qoq_ann(gdp), 'real_gdp_growth_qoq_annualized', 'percent', T, 2)
q.add(yoy(gdp, 12), 'real_gdp_growth_yoy', 'percent', T, 2)
q.add(gdppc, 'real_gdp_per_capita', 'chained 2017 dollars per person, annualized', T + ' / 17-10-0009-01 (derived)', 0)
q.add(qoq_ann(gdppc), 'real_gdp_per_capita_growth_qoq_annualized', 'percent', T + ' / 17-10-0009-01 (derived)', 2)
q.add(yoy(gdppc, 12), 'real_gdp_per_capita_growth_yoy', 'percent', T + ' / 17-10-0009-01 (derived)', 2)
q.add(bnr, 'real_business_nonres_investment', 'millions of chained 2017 dollars, SAAR (business NR structures+M&E + IPP; chained components not strictly additive)', T + ' (derived)', 0)
q.add(yoy(bnr, 12), 'real_business_nonres_investment_yoy', 'percent', T + ' (derived)', 2)
q.add(qoq_ann(bnr), 'real_business_nonres_investment_qoq_annualized', 'percent', T + ' (derived)', 2)
q.add(me, 'real_business_machinery_equipment', 'millions of chained 2017 dollars, SAAR', T, 0)
q.add(yoy(me, 12), 'real_business_machinery_equipment_yoy', 'percent', T, 2)
q.add(resi, 'real_residential_investment', 'millions of chained 2017 dollars, SAAR', T, 0)
q.add(yoy(resi, 12), 'real_residential_investment_yoy', 'percent', T, 2)
q.add(ggfcf, 'real_government_gfcf', 'millions of chained 2017 dollars, SAAR', T, 0)
q.add(yoy(ggfcf, 12), 'real_government_gfcf_yoy', 'percent', T, 2)
q.add(xg, 'real_exports_goods', 'millions of chained 2017 dollars, SAAR', T, 0)
q.add(yoy(xg, 12), 'real_exports_goods_yoy', 'percent', T, 2)
q.save()

# ---------------- Productivity (quarterly) ----------------
T = '36-10-0206-01'
lp = series('36100206', lambda r: r['Labour productivity measures and related measures'] == 'Labour productivity')
hw = series('36100206', lambda r: r['Labour productivity measures and related measures'] == 'Hours worked')
bgdp = series('36100206', lambda r: r['Labour productivity measures and related measures'] == 'Real gross domestic product (GDP)')
ulc = series('36100206', lambda r: r['Labour productivity measures and related measures'] == 'Unit labour cost')
p = W('ind_productivity_quarterly.csv', 'Q')
p.add(lp, 'business_labour_productivity_index', 'index 2017=100', T, 2)
p.add({k: pct(v, lp[shift(k, -3)]) for k, v in lp.items() if shift(k, -3) in lp}, 'business_labour_productivity_qoq', 'percent (not annualized)', T, 2)
p.add(yoy(lp, 12), 'business_labour_productivity_yoy', 'percent', T, 2)
p.add(hw, 'business_hours_worked_index', 'index 2017=100', T, 2)
p.add(bgdp, 'business_real_gdp_index', 'index 2017=100', T, 2)
p.add(yoy(ulc, 12), 'business_unit_labour_cost_yoy', 'percent', T, 2)
p.save()

# ---------------- Monthly GDP ----------------
T = '36-10-0434-01'
mg = series('36100434', lambda r: r['GEO'] == 'Canada' and r['Seasonal adjustment'].startswith('Seasonally') and r['Prices'] == 'Chained (2017) dollars' and r['North American Industry Classification System (NAICS)'] == 'All industries [T001]')
m = W('ind_gdp_monthly.csv', 'M')
m.add(mg, 'real_gdp_basic_prices_all_industries', 'millions of chained 2017 dollars, SAAR', T, 0)
m.add({k: pct(v, mg[shift(k, -1)]) for k, v in mg.items() if shift(k, -1) in mg}, 'real_gdp_basic_prices_mom', 'percent', T, 2)
m.add(yoy(mg, 12), 'real_gdp_basic_prices_yoy', 'percent', T, 2)
m.save()

# ---------------- Labour market ----------------
T = '14-10-0287-01'
def lfs(ch):
    return series('14100287', lambda r: r['GEO'] == 'Canada' and r['Gender'] == 'Total - Gender' and r['Age group'] == '15 years and over' and r['Statistics'] == 'Estimate' and r['Data type'] == 'Seasonally adjusted' and r['Labour force characteristics'] == ch)
L = W('ind_labour_monthly.csv', 'M')
L.add(lfs('Unemployment rate'), 'unemployment_rate', 'percent, SA', T, 1)
L.add(lfs('Employment rate'), 'employment_rate', 'percent, SA', T, 1)
L.add(lfs('Participation rate'), 'participation_rate', 'percent, SA', T, 1)
emp = lfs('Employment')
L.add(emp, 'employment', 'thousands of persons, SA', T, 1)
L.add(yoy(emp, 12), 'employment_yoy', 'percent', T, 2)
yu = series('14100287', lambda r: r['GEO'] == 'Canada' and r['Gender'] == 'Total - Gender' and r['Age group'] == '15 to 24 years' and r['Statistics'] == 'Estimate' and r['Data type'] == 'Seasonally adjusted' and r['Labour force characteristics'] == 'Unemployment rate')
L.add(yu, 'youth_unemployment_rate_15_24', 'percent, SA', T, 1)

# ---------------- CPI ----------------
T = '18-10-0004-01'
def cpi(prod):
    return series('18100004', lambda r: r['GEO'] == 'Canada' and r['Products and product groups'] == prod and r['UOM'] == '2002=100')
C = W('ind_prices_monthly.csv', 'M')
allc = cpi('All-items')
C.add(allc, 'cpi_all_items_index', 'index 2002=100, NSA', T, 1)
for prod, nm in [('All-items', 'cpi_all_items_yoy'), ('Gasoline', 'cpi_gasoline_yoy'), ('Shelter', 'cpi_shelter_yoy'),
                 ('Rent', 'cpi_rent_yoy'), ('Food purchased from stores', 'cpi_food_stores_yoy'),
                 ('All-items excluding gasoline', 'cpi_ex_gasoline_yoy'), ('All-items excluding food and energy', 'cpi_ex_food_energy_yoy')]:
    s = allc if prod == 'All-items' else cpi(prod)
    if not s:
        print('missing CPI', prod); continue
    C.add(yoy(s, 12), nm, 'percent, y/y', T, 1)
C.save()

# ---------------- Wages (nominal & real) ----------------
cpiy = yoy(allc, 12)
T = '14-10-0063-01'
ahw = series('14100063', lambda r: r['GEO'] == 'Canada' and r['Wages'] == 'Average hourly wage rate' and r['Type of work'] == 'Both full- and part-time employees' and r['North American Industry Classification System (NAICS)'] == 'Total employees, all industries' and r['Gender'] == 'Total - Gender' and r['Age group'] == '15 years and over')
L.add(ahw, 'avg_hourly_wage_lfs', 'current dollars, NSA', T, 2)
wy = yoy(ahw, 12)
L.add(wy, 'avg_hourly_wage_lfs_yoy', 'percent', T, 2)
L.add({k: ((1 + v / 100) / (1 + cpiy[k] / 100) - 1) * 100 for k, v in wy.items() if k in cpiy}, 'real_avg_hourly_wage_lfs_yoy', 'percent (nominal y/y deflated by CPI all-items y/y)', T + ' / 18-10-0004-01 (derived)', 2)
T = '14-10-0223-01'
awe = series('14100223', lambda r: r['GEO'] == 'Canada' and r['Estimate'] == 'Average weekly earnings including overtime for all employees' and r['North American Industry Classification System (NAICS)'] == 'Industrial aggregate excluding unclassified businesses [11-91N]')
L.add(awe, 'avg_weekly_earnings_seph', 'current dollars, SA', T, 2)
ay = yoy(awe, 12)
L.add(ay, 'avg_weekly_earnings_seph_yoy', 'percent', T, 2)
L.add({k: ((1 + v / 100) / (1 + cpiy[k] / 100) - 1) * 100 for k, v in ay.items() if k in cpiy}, 'real_avg_weekly_earnings_seph_yoy', 'percent (nominal y/y deflated by CPI all-items y/y)', T + ' / 18-10-0004-01 (derived)', 2)
L.save()

# ---------------- Housing ----------------
H = W('ind_housing_monthly.csv', 'M')
T = '34-10-0158-01'
hs = series('34100158', lambda r: r['GEO'] == 'Canada', scale=1000)
H.add(hs, 'housing_starts_saar', 'units, SAAR (all areas)', T, 0)
ma6 = {}
ks = sorted(hs)
for i, k in enumerate(ks):
    if i >= 5:
        ma6[k] = sum(hs[x] for x in ks[i - 5:i + 1]) / 6
H.add(ma6, 'housing_starts_saar_6mo_avg', 'units, SAAR (derived 6-month moving average; CMHC publishes its own trend)', T + ' (derived)', 0)
H.save()
T = '34-10-0135-01'
hq = series('34100135', lambda r: r['GEO'] == 'Canada' and r.get('Housing estimates') == 'Housing starts' and r.get('Type of unit') == 'Total units' and r.get('Seasonal adjustment') == 'Unadjusted', start='2015-01')
ann = defaultdict(float); cnt = defaultdict(int)
for k, v in hq.items():
    ann[k[:4]] += v; cnt[k[:4]] += 1
with open(os.path.join(OUT, 'ind_housing_annual.csv'), 'w', newline='') as f:
    w = csv.writer(f); w.writerow(['date', 'indicator', 'value', 'unit', 'source_table', 'regime', 'quarters_in_sum'])
    for y in sorted(ann):
        w.writerow([f'{y}-01-01', 'housing_starts_annual_total', round(ann[y]), 'units (all areas, actual, NSA)', T + ' (sum of quarters)', 'post' if y >= '2025' else 'pre', cnt[y]])
    for k in sorted(hq):
        if k >= '2022-01':
            w.writerow([k + '-01', 'housing_starts_quarterly_total', round(hq[k]), 'units (all areas, actual, NSA)', T, regime(k, 'Q'), 1])
print('ind_housing_annual.csv', dict(ann), dict(cnt))

# ---------------- Population (quarterly) ----------------
T = '17-10-0009-01'
P = W('ind_population_quarterly.csv', 'Q')
P.add(pop, 'population', 'persons (quarter-start estimate)', T, 0)
P.add({k: pct(v, pop[shift(k, -3)]) for k, v in pop.items() if shift(k, -3) in pop}, 'population_growth_qoq', 'percent change from previous quarter-start estimate (not annualized)', T, 3)
P.add(yoy(pop, 12), 'population_growth_yoy', 'percent', T, 2)
P.save()

# ---------------- Trade by destination (monthly) ----------------
T = '12-10-0011-01'
def tr(partner, trade='Export'):
    return series('12100011', lambda r: r['Trade'] == trade and r['Basis'] == 'Balance of payments' and r['Seasonal adjustment'] == 'Seasonally adjusted' and r['Principal trading partners'] == partner)
xa, xu = tr('All countries'), tr('United States')
xn = {k: xa[k] - xu[k] for k in xa if k in xu}
X = W('ind_trade_monthly.csv', 'M')
X.add(xa, 'goods_exports_all', 'millions of dollars, SA, BOP basis', T, 1)
X.add(xu, 'goods_exports_us', 'millions of dollars, SA, BOP basis', T, 1)
X.add(xn, 'goods_exports_non_us', 'millions of dollars, SA, BOP basis (derived: all - US)', T + ' (derived)', 1)
X.add({k: xu[k] / xa[k] * 100 for k in xn}, 'us_share_of_goods_exports', 'percent', T + ' (derived)', 2)
r12 = {}
ks = sorted(xn)
for i, k in enumerate(ks):
    if i >= 11:
        r12[k] = sum(xu[x] for x in ks[i - 11:i + 1]) / sum(xa[x] for x in ks[i - 11:i + 1]) * 100
X.add(r12, 'us_share_of_goods_exports_12mo', 'percent (rolling 12-month)', T + ' (derived)', 2)
X.add(yoy(xn, 12), 'goods_exports_non_us_yoy', 'percent', T + ' (derived)', 2)
X.add(yoy(xu, 12), 'goods_exports_us_yoy', 'percent', T + ' (derived)', 2)
for prt in ['European Union', 'China', 'United Kingdom', 'Japan', 'India']:
    X.add(tr(prt), 'goods_exports_' + prt.lower().replace(' ', '_'), 'millions of dollars, SA, BOP basis', T, 1)
X.save()

# ---------------- Policy rate ----------------
pr = {}
with open(os.path.join(RAW, 'boc', 'V39079.csv')) as f:
    lines = f.read().splitlines()
i = lines.index('"OBSERVATIONS"')
for r in csv.DictReader(lines[i + 1:]):
    if r.get('V39079'):
        pr[r['date']] = float(r['V39079'])
R = W('ind_policy_rate.csv', 'M')
eom = {}
for d in sorted(pr):
    eom[d[:7]] = pr[d]
R.add(eom, 'boc_policy_rate_end_of_month', 'percent', 'Bank of Canada Valet V39079', 2)
prev = None
for d in sorted(pr):
    if prev is not None and pr[d] != pr[prev]:
        R.out.append((d, 'boc_policy_rate_change_effective', pr[d], 'percent (new level, effective date)', 'Bank of Canada Valet V39079', 'pre' if d < '2025-03-14' else 'post'))
    prev = d
R.save()
