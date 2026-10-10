# Build a stitched percentile-by-percentile wealth curve for Canada (families), clearly labelled by source/year.
import csv,sys,json,openpyxl
sfs_dec,hfd_xlsx,forbes_json,out=sys.argv[1:5]
FX=1.3567  # BoC FXUSDCAD 2026-03-10
rows=[]
order=['Lowest','Second','Third','Fourth','Fifth','Sixth','Seventh','Eighth','Ninth','Highest']
for r in csv.DictReader(open(sfs_dec)):
    if r['sfs_year']=='2023' and r['geo']=='Canada' and not r['net_worth_deciles'].startswith('Total'):
        i=order.index(r['net_worth_deciles'].split()[0])
        rows.append(dict(segment='SFS 2023 (survey)',year=2023,percentile=i*10+5,unit='economic family',net_worth_cad=float(r['median_net_worth_2023cad']),
          note='median within net-worth decile (approximates percentile at decile midpoint); StatCan 11-10-0075-01',source='StatCan table 11-10-0075-01'))
# HFD public 2023
wb=openpyxl.load_workbook(hfd_xlsx,read_only=True,data_only=True)
recs=sorted([(float(a),float(b)) for a,b in list(wb.worksheets[0].iter_rows(values_only=True))[1:] if a is not None],reverse=True)
TOTAL_FAM=16852000.0  # PBO 2025 Table 2: 3,372.2k + 6,739.0k + 6,740.8k
def thr(top_frac):
    target=top_frac*TOTAL_FAM; cw=0
    for v,w in recs:
        cw+=w
        if cw>=target: return v
for p in [80,85,90,92.5,95,97,98,99,99.5,99.8,99.9,99.95,99.98,99.99,99.995,99.999]:
    v=thr((100-p)/100)
    rows.append(dict(segment='PBO HFD 2023 (public file; SFS + Pareto + Forbes/Maclean\'s, NBSA-aligned)',year=2023,percentile=p,unit='economic family',net_worth_cad=round(v),
      note='threshold computed by Claude from PBO public HFD 2023 microfile, denominator 16,852k families (PBO 2025 Table 2)',source='PBO HFD 2023 public xlsx'))
# Forbes 2026 Canadian citizens (individuals)
L=json.load(open(forbes_json))['personList']['personsLists']
ca=sorted([p for p in L if p.get('countryOfCitizenship')=='Canada'],key=lambda p:-p['finalWorth'])
for i,p in enumerate(ca):
    rows.append(dict(segment='Forbes 2026 individual billionaires (Canadian citizens)',year=2026,percentile='',unit='individual (Forbes entry; some are families)',net_worth_cad=round(p['finalWorth']*1e6*FX),
      note=f"{p['personName']}; Forbes rank {p.get('rank')}; US${p['finalWorth']/1000:.1f}B x BoC FXUSDCAD {FX} (2026-03-10); residence {p.get('country')}",source='forbes.com/forbesapi/person/billionaires/2026/position/true.json'))
w=csv.DictWriter(open(out,'w',newline=''),fieldnames=['segment','year','percentile','unit','net_worth_cad','note','source'])
w.writeheader(); w.writerows(rows)
print(len(rows))
