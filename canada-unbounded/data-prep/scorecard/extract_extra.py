"""Supplementary tidy series: NPR share of population, annual non-US goods exports vs doubling path,
fiscal (projections vs results), defence (% GDP, NATO basis).
Usage: python3 -I extract_extra.py <raw_dir> <out_dir>
"""
import zipfile, csv, io, sys, os
from collections import defaultdict

RAW, OUT = sys.argv[1], sys.argv[2]


def rows(pid):
    z = zipfile.ZipFile(os.path.join(RAW, f'{pid}-eng.zip'))
    with z.open(f'{pid}.csv') as fh:
        yield from csv.DictReader(io.TextIOWrapper(fh, encoding='utf-8-sig'))


def series(pid, pred, start='2015-01'):
    d = {}
    for r in rows(pid):
        if r['REF_DATE'][:7] >= start and r['VALUE'] and pred(r):
            assert r['REF_DATE'][:7] not in d, (pid, r['REF_DATE'])
            d[r['REF_DATE'][:7]] = float(r['VALUE'])
    return d


def reg(k):
    return 'pre' if k < '2025-01' else ('transition' if k == '2025-01' else 'post')


HDR = ['date', 'indicator', 'value', 'unit', 'source_table', 'regime']

# ---- NPR share (quarterly) ----
npr = series('17100121', lambda r: r['GEO'] == 'Canada' and r['Non-permanent resident types'] == 'Total, non-permanent residents', start='2021-07')
pop = series('17100009', lambda r: r['GEO'] == 'Canada', start='2021-07')
out = []
for k in sorted(npr):
    out.append((k + '-01', 'non_permanent_residents', npr[k], 'persons (quarter-start estimate)', '17-10-0121-01', reg(k)))
    if k in pop:
        out.append((k + '-01', 'npr_share_of_population', round(npr[k] / pop[k] * 100, 2), 'percent', '17-10-0121-01 / 17-10-0009-01 (derived)', reg(k)))
with open(os.path.join(OUT, 'ind_npr_quarterly.csv'), 'w', newline='') as f:
    w = csv.writer(f); w.writerow(HDR); w.writerows(out)
print('npr', [(o[0], o[2]) for o in out if o[1] == 'npr_share_of_population'][-6:])

# ---- Non-US goods exports, annual and YTD, vs doubling path ----
def tr(p):
    return series('12100011', lambda r: r['Trade'] == 'Export' and r['Basis'] == 'Balance of payments' and r['Seasonal adjustment'] == 'Seasonally adjusted' and r['Principal trading partners'] == p, start='2022-01')
xa, xu = tr('All countries'), tr('United States')
ann = defaultdict(lambda: [0.0, 0.0, 0])
for k in xa:
    a = ann[k[:4]]; a[0] += xa[k]; a[1] += xu[k]; a[2] += 1
last = max(xa)
with open(os.path.join(OUT, 'ind_trade_annual.csv'), 'w', newline='') as f:
    w = csv.writer(f); w.writerow(HDR + ['months'])
    for y in sorted(ann):
        a, u, n = ann[y]
        tag = '' if n == 12 else f' (Jan-{last[5:]} only)'
        w.writerow([f'{y}-01-01', 'goods_exports_non_us_annual' + ('' if n == 12 else '_ytd'), round(a - u), 'millions of dollars, SA, BOP basis' + tag, '12-10-0011-01 (derived)', 'pre' if y < '2025' else 'post', n])
        w.writerow([f'{y}-01-01', 'us_share_goods_exports_annual' + ('' if n == 12 else '_ytd'), round(u / a * 100, 2), 'percent' + tag, '12-10-0011-01 (derived)', 'pre' if y < '2025' else 'post', n])
    # YTD comparison same months in prior year
    y = last[:4]; m = int(last[5:])
    def ytd(yy):
        return sum(xa[f'{yy}-{i:02d}'] - xu[f'{yy}-{i:02d}'] for i in range(1, m + 1))
    w.writerow([f'{y}-01-01', 'goods_exports_non_us_ytd_growth_vs_prior_year', round((ytd(y) / ytd(str(int(y) - 1)) - 1) * 100, 2), f'percent (Jan-{m:02d} vs same months prior year)', '12-10-0011-01 (derived)', 'post', m])
base = ann['2024'][0] - ann['2024'][1]
g = 2 ** (1 / 10)  # doubling over ten years (2024 base -> 2034); Budget 2025 target covers goods AND services
print('non-US goods 2024', round(base), '2025', round(ann['2025'][0] - ann['2025'][1]), 'path 2025', round(base * g))
TRADE_PATH = {'base_2024': base, 'path_2025': base * g, 'actual_2025': ann['2025'][0] - ann['2025'][1],
              'path_2026': base * g * g, 'ytd_2026_growth': (ytd(y) / ytd(str(int(y) - 1)) - 1) * 100, 'last': last}
with open(os.path.join(OUT, '_trade_path.txt'), 'w') as f:
    f.write(repr(TRADE_PATH))
