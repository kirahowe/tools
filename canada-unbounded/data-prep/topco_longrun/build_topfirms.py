"""Build long-run top-firm CSVs for Canada and the US, plus churn tables.
Usage: python3 -I build_topfirms.py <raw_dir> <out_dir>
"""
import csv, re, sys, os, glob, collections

RAW, OUT = sys.argv[1], sys.argv[2]

RCCC_URL = "https://publications.gc.ca/collections/collection_2014/bcp-pco/Z1-1975-1-1-eng.pdf"
RCCC = "Royal Commission on Corporate Concentration, Report (1978), Table 2.1 (data: Financial Post 300, Summer 1976; Canadian Business July 1976; RCCC research)"
RCCC_FIN = "Royal Commission on Corporate Concentration, Report (1978), Table 2.2 (data: Canadian Business, July 1976)"
FIN_URL = "https://finaeon.com/three-centuries-of-canadian-leadership/"
FIN = "Finaeon (Global Financial Data), B. Taylor, 'Three Centuries of Canadian Leadership' (Aug 2018) - commercial data vendor blog"
CMU_URL = "https://github.com/cmusam/fortune500"
CMU = "Fortune 500 (Fortune magazine) via cmusam/fortune500 GitHub scrape of money.cnn.com fortune500_archive (1955-2005) and fortune.com/fortunechina (2006-2019)"
FC = "Fortune 500 (Fortune magazine), fortune.com ranking page"
HAN_URL = "https://cep.lse.ac.uk/pubs/download/dp0318.pdf"
HAN = "Hannah (1997) 'Marshall's Trees and the Global Forest', CEP Discussion Paper 318, Appendix A (world top-100 industrials by equity market cap, 1912)"

can, us = [], []

# ---------------- Canada: RCCC Table 2.1 (1975/76) ----------------
lines = open(os.path.join(RAW, "rcc", "v1.txt"), errors="replace").read().split("\n")
# table body spans the lines between 'Table 2 . 1' and 'Sources : - Unless otherwise'
start = next(i for i, l in enumerate(lines) if "100 Largest Non-Financial C an adi an Comp anies" in l)
end = next(i for i in range(start, len(lines)) if lines[i].strip().startswith("Sources : - Unless"))
body = lines[start:end]
num = r"\(?-?[\d][\d ,]*\d\)?"
recs, pending = [], None
for l in body:
    s = l.replace("•", ".").strip().lstrip('"')
    s = re.sub(r"^B \.", "15 .", s)  # OCR: rank 15 printed as 'B .'
    s = re.sub(r"^14\. ", "14 . ", s)
    m = re.match(r"^(\d{1,3})\s*\.\s*(.*)$", s)
    if m and not re.match(r"^\d+\s*\.\s*\d", s):
        if pending:
            recs.append(pending)
        pending = [int(m.group(1)), m.group(2)]
    elif pending and s and not s.startswith(("Rank", "Sales", "or", "16 Royal", "Revenues", "Corporate")):
        pending[1] += " " + s
if pending:
    recs.append(pending)
out_rows = []
for rank, txt in recs:
    txt = re.sub(r"\s+", " ", txt)
    m = re.match(r"^(.*?)\s+(\d[\d,]*)\s+(\d[\d ,]*\d)\s+(\(?[\d ,]+\)?)\s*$", txt)
    if not m:
        print("UNPARSED", rank, txt, file=sys.stderr); continue
    name = m.group(1).rstrip(" ,")
    flag = ""
    if name.endswith("**") or "**" in name: flag = "Canadian Business July 1976 figures"
    elif name.endswith("*"): flag = "RCCC estimate"
    name = re.sub(r"\*+'?|\s*\d$|t$", "", name).strip()
    name = name.replace(" .", ".").replace("Compan y", "Company").replace("o f", "of").replace("Te a", "Tea").replace("Wort s", "Worts").replace("Industrie s", "Industries").replace("Powe r", "Power").replace(" ,", ",")
    name = re.sub(r"\s+", " ", name)
    if name == "Irving Oil Limited2": name = "Irving Oil Limited"
    if name.endswith("Limited3"): name = name[:-1]
    sales = int(m.group(2).replace(",", ""))
    assets = int(m.group(3).replace(",", "").replace(" ", ""))
    ni = m.group(4).replace(",", "").replace(" ", "")
    ni = -int(ni.strip("()")) if ni.startswith("(") else int(ni)
    # OCR fixes verified against the printed table: stray spaces inside the last number
    if rank == 9: assets, ni = 3011781, 22570      # 'Alcan ... 3,011,781 22,57 0'
    if rank == 100: assets, ni = 167671, 9806      # 'Standard Brands ... 167,671 9,80 6'
    out_rows.append((rank, name, sales, assets, ni, flag))
assert len(out_rows) == 100 and [r[0] for r in out_rows] == list(range(1, 101)), [r[0] for r in out_rows]
for rank, name, sales, assets, ni, flag in out_rows:
    base = dict(year=1975, list="RCCC 100 largest non-financial companies (incl. foreign subsidiaries & crown corps), ranked by sales",
                rank=rank, company=name, source=RCCC, source_url=RCCC_URL, note=("fiscal 1975/76; " + flag).strip("; "))
    can.append({**base, "measure": "sales_or_revenue", "value": sales, "unit": "CAD thousands"})
    can.append({**base, "measure": "total_assets", "value": assets, "unit": "CAD thousands"})
    can.append({**base, "measure": "net_income_after_tax", "value": ni, "unit": "CAD thousands"})

# RCCC Table 2.2 financial institutions
start = next(i for i, l in enumerate(lines) if re.match(r"^\s*1 \. The Royal Bank of Canada", l))
end = next(i for i in range(start, len(lines)) if lines[i].strip().startswith("Source :"))
fin, pending = [], None
for l in lines[start:end]:
    s = l.strip()
    m = re.match(r"^(\d{1,2})\s*\.\s*(.*)$", s)
    if m:
        if pending: fin.append(pending)
        pending = [int(m.group(1)), m.group(2)]
    elif pending and s and not s.startswith(("Rank", "Net", "Total", "(Thousands")):
        pending[1] += " " + s
        if re.search(r"\d[\d,]*\s+(N/A|[\d ,]+)$", s) is None:
            pass
fin.append(pending)
seen = set()
for rank, txt in fin:
    if rank in seen: continue
    seen.add(rank)
    txt = re.sub(r"\s+", " ", txt)
    m = re.match(r"^(.*?)\s+(\d[\d,]*)\s+(N/A|[\d ,]*\d)\s*$", txt)
    if not m:
        print("UNPARSED FIN", rank, txt, file=sys.stderr); continue
    name = m.group(1).replace(" .", ".").strip()
    can.append(dict(year=1975, list="RCCC 25 largest Canadian financial institutions, ranked by assets", rank=rank, company=name,
                    measure="total_assets", value=int(m.group(2).replace(",", "")), unit="CAD thousands", source=RCCC_FIN, source_url=RCCC_URL, note="1975/76"))
assert len(seen) == 25, sorted(seen)

# Finaeon: largest Canadian company by market cap, selected years (CAD or GBP millions as listed)
finaeon = [(1820, "Hudson's Bay Company", 0.8), (1830, "Hudson's Bay Company", 1.37), (1840, "Hudson's Bay Company", 1.68),
           (1850, "Hudson's Bay Company", 1.27), (1855, "Great Western Railway of Canada", 2.63), (1860, "Bank of British North America", 4.94),
           (1870, "Bank of Montreal", 13.92), (1880, "Great Western Railway of Canada", 21.91), (1890, "Canadian Pacific Railway Ltd.", 49.16),
           (1900, "Canadian Pacific Railway Ltd.", 61.10), (1910, "Canadian Pacific Railway Ltd.", 362.36), (1920, "Canadian Pacific Railway Ltd.", 348.56),
           (1930, "Canadian Pacific Railway Ltd.", 520.93), (1940, "International Nickel (Inco Ltd.)", 403.17), (1950, "International Nickel (Inco Ltd.)", 751.94),
           (1960, "International Nickel (Inco Ltd.)", 1712), (1970, "International Nickel (Inco Ltd.)", 3417), (1980, "Gulf Canada Ltd. (British American Oil)", 5262),
           (1990, "BCE Inc. (Bell Telephone of Canada)", 12073), (2000, "Nortel Networks Corp.", 147300), (2010, "Toronto-Dominion Bank", 129677),
           (2017, "Royal Bank of Canada", 149717)]
for y, n, v in finaeon:
    can.append(dict(year=y, list="Largest Canadian company by stock market capitalisation (rank 1 only)", rank=1, company=n, measure="market_cap",
                    value=v, unit="millions (currency not stated; likely CAD/GBP)", source=FIN, source_url=FIN_URL,
                    note="non-academic vendor estimate; only the #1 firm per year is published"))

with open(os.path.join(OUT, "canada_top_firms_longrun.csv"), "w", newline="") as f:
    w = csv.DictWriter(f, fieldnames=["year", "list", "rank", "company", "measure", "value", "unit", "source", "source_url", "note"])
    w.writeheader(); w.writerows(can)

# ---------------- US: Fortune 500 ----------------
ALIAS = {"Wal-Mart Stores": "Walmart", "Intl. Business Machines": "IBM", "International Business Machines": "IBM",
         "American Intl. Group": "AIG", "American International Group": "AIG", "Amazon.com": "Amazon",
         "Exxon Mobil Holdings": "Exxon Mobil", "ChevronTexaco": "Chevron", "CVS Caremark": "CVS Health", "Costco Wholesale": "Costco",
         "Hewlett-Packard": "HP", "J.P. Morgan Chase & Co.": "JPMorgan Chase", "J.P. Morgan Chase": "JPMorgan Chase",
         "JP Morgan Chase": "JPMorgan Chase", "Chase Manhattan Corp.": "JPMorgan Chase", "Bank of America Corp.": "Bank of America",
         "Verizon Communications": "Verizon", "AmerisourceBergen": "Cencora", "Citicorp": "Citigroup", "SBC Communications": "AT&T",
         "Kmart Holding": "Kmart", "Honeywell Intl.": "Honeywell", "Honeywell International": "Honeywell",
         "Honeywell Technologies": "Honeywell", "GE Aerospace": "General Electric", "United States Steel": "U.S. Steel",
         "Dow": "Dow Chemical", "RTX": "United Technologies", "Raytheon Technologies": "United Technologies"}
def key(n):
    n = n.strip()
    return ALIAS.get(n, n)

f500 = collections.defaultdict(dict)  # year -> rank -> (name, revenue)
for fn in sorted(glob.glob(os.path.join(RAW, "fortune500", "repo", "csv", "fortune500-*.csv"))):
    y = int(re.search(r"(\d{4})", os.path.basename(fn)).group(1))
    for r in csv.reader(open(fn)):
        if r and r[0].isdigit():
            rev = r[2].replace(",", "") if len(r) > 2 else ""
            try: rev = float(rev)
            except ValueError: rev = None
            f500[y][int(r[0])] = (r[1], rev, CMU, CMU_URL)
for r in csv.DictReader(open(os.path.join(RAW, "fortune_com", "parsed.csv"))):
    y = int(r["year"])
    try: rev = float(r["revenue_musd"])
    except (ValueError, TypeError): rev = None
    if int(r["rank"]) <= 500:
        f500[y][int(r["rank"])] = (r["company"], rev, FC, r["url"])
TOPN = 50
for y in sorted(f500):
    for rk in range(1, TOPN + 1):
        if rk in f500[y]:
            n, rev, s, u = f500[y][rk]
            us.append(dict(year=y, list="Fortune 500 (US, by revenue; industrials only until 1994 list)", rank=rk, company=n, lineage_key=key(n),
                           measure="revenue", value=rev, unit="USD millions (nominal; prior fiscal year)", source=s, source_url=u,
                           note="1955-2005 names are CNN archive's later/successor names, not names at the time" if y <= 2005 else ""))

# Hannah 1912: US-headquartered firms in world top 100 industrials (equity market cap)
han = [(1, "US Steel (USX Marathon)", 741), (2, "Jersey Standard (Exxon)", 390), (4, "Pullman", 200), (6, "Anaconda", 178),
       (7, "General Electric", 174), (8, "Singer", 173), (9, "American Tobacco (American Brands)", 169), (10, "International Harvester (Navistar)", 160),
       (15, "Eastman Kodak", 128), (16, "Armour", 126), (18, "Utah Copper & Nevada Consolidated", 116), (19, "American Sugar", 110),
       (21, "Westinghouse Air Brake", 102), (22, "Phelps Dodge", 95), (25, "American Smelting (Asarco)", 92), (26, "Indiana Standard (Amoco)", 88),
       (30, "US Rubber (Uniroyal)", 80), (31, "BF Goodrich", 75), (32, "Swift", 75), (33, "New York Standard (Mobil)", 73),
       (34, "National Biscuit", 72), (35, "California Standard (Chevron)", 71), (36, "John Deere", 70), (37, "Du Pont", 69),
       (39, "Westinghouse Electric", 67), (40, "Ohio Oil", 66), (44, "Liggett & Myers", 58), (45, "International Nickel (Inco)", 57),
       (46, "American Can", 57), (50, "American Car & Foundry", 52), (52, "Procter & Gamble", 51), (54, "Calumet & Hecla", 51),
       (55, "Prairie Oil & Gas", 50), (59, "Western Electric", 47)]
han.sort(key=lambda x: (x[0]))
for i, (wr, n, v) in enumerate(han, 1):
    if i > 25: break
    us.append(dict(year=1912, list="US-HQ firms within Hannah's world top-100 industrials by equity market cap (rank = order among US firms)", rank=i,
                   company=n, lineage_key=n, measure="market_cap", value=v, unit="USD millions (1912 prices)", source=HAN, source_url=HAN_URL,
                   note=f"world rank {wr}; industrials only (mining & manufacturing); names in brackets = 1995 successor; International Nickel was US-HQ in 1912"))

with open(os.path.join(OUT, "us_top_firms_longrun.csv"), "w", newline="") as f:
    w = csv.DictWriter(f, fieldnames=["year", "list", "rank", "company", "lineage_key", "measure", "value", "unit", "source", "source_url", "note"])
    w.writeheader(); w.writerows(us)

# ---------------- churn: US top-N overlap with N years earlier ----------------
def topset(y, n):
    return {key(f500[y][r][0]) for r in range(1, n + 1) if r in f500[y]}
with open(os.path.join(OUT, "us_fortune_topN_churn.csv"), "w", newline="") as f:
    w = csv.writer(f)
    w.writerow(["year", "top_n", "lag_years", "comparison_year", "n_still_in_topN", "n_new", "entrants", "source"])
    for n in (10, 25):
        for y in sorted(f500):
            for lag in (10,):
                if y - lag in f500:
                    a, b = topset(y, n), topset(y - lag, n)
                    w.writerow([y, n, lag, y - lag, len(a & b), len(a - b), "; ".join(sorted(a - b)), "Fortune 500 (see us_top_firms_longrun.csv); lineage-matched names"])
    # survival of 1955 cohort
    for n in (10, 25, 100, 500):
        base = topset(1955, n)
        for y in (1965, 1975, 1985, 1995, 2005, 2015, 2025, 2026):
            if y in f500:
                full = {key(v[0]) for v in f500[y].values()}
                w.writerow([y, f"1955 top {n} still in that year's Fortune 500", y - 1955, 1955, len(base & full), "", "", "Fortune 500; exact lineage-name match (undercounts renamed firms after 2005)"])
print("canada rows", len(can), "us rows", len(us))
