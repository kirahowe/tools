import csv,collections
gdp=collections.defaultdict(list)
for r in csv.DictReader(open('raw/36100104/36100104.csv',encoding='utf-8-sig')):
    if r['VECTOR']=='v62305783' and r['VALUE']: gdp[int(r['REF_DATE'][:4])].append(float(r['VALUE']))
G={y:sum(l)/4 for y,l in gdp.items() if len(l)==4}
f={}
for r in csv.DictReader(open('raw/36100471/36100471.csv',encoding='utf-8-sig')):
    if r['VECTOR'] in ('v61913424','v61913525') and r['VALUE']: f[(r['VECTOR'],int(r['REF_DATE']))]=float(r['VALUE'])
# directional flows 2007+
dq=collections.defaultdict(float); cnt=collections.Counter()
for r in csv.DictReader(open('raw/36100025/36100025.csv',encoding='utf-8-sig')):
    if r['VECTOR'] in ('v61913911','v61913923','v61913912','v61913924') and r['VALUE']:
        y=int(r['REF_DATE'][:4]); dq[(r['VECTOR'],y)]+=float(r['VALUE']); cnt[(r['VECTOR'],y)]+=1
st={}
for r in csv.DictReader(open('raw/36100008/36100008.csv',encoding='utf-8-sig')):
    if r['Countries or regions']=='All countries' and r['Canadian and foreign direct investment'] in ('Canadian direct investment abroad - total book value','Foreign direct investment in Canada - total book value') and r['VALUE']:
        st[(r['Canadian and foreign direct investment'][:8],int(r['REF_DATE'][:4]))]=(float(r['VALUE']),r['VECTOR'])
with open('F1_fdi_flows_and_stocks_annual.csv','w',newline='') as o:
    w=csv.writer(o)
    w.writerow(['year','di_assets_outflow_cad_mn','di_liabilities_inflow_cad_mn','net_outward_cad_mn','di_assets_outflow_pct_gdp','di_liabilities_inflow_pct_gdp',
      'directional_CDIA_net_flow_cad_mn','directional_FDI_net_flow_cad_mn','directional_CDIA_MA_cad_mn','directional_FDI_MA_cad_mn',
      'CDIA_stock_book_value_cad_mn','FDI_stock_book_value_cad_mn','CDIA_stock_pct_gdp','FDI_stock_pct_gdp','source'])
    for y in range(1981,2026):
        a=f.get(('v61913424',y)); l=f.get(('v61913525',y))
        dd=lambda v: round(dq[(v,y)]) if cnt[(v,y)]==4 else ''
        ca=st.get(('Canadian',y)); fd=st.get(('Foreign ',y))
        w.writerow([y,a,l,(a-l) if a is not None and l is not None else '',round(100*a/G[y],2) if a is not None else '',round(100*l/G[y],2) if l is not None else '',
          dd('v61913911'),dd('v61913923'),dd('v61913912'),dd('v61913924'),
          ca[0] if ca else '',fd[0] if fd else '',round(100*ca[0]/G[y],1) if ca else '',round(100*fd[0]/G[y],1) if fd else '',
          'Flows: StatCan 36-10-0471-01 financial account (asset/liability basis; v61913424 assets, v61913525 liabilities); directional: 36-10-0025-01 (sum of quarters; v61913911,v61913923,v61913912,v61913924); stocks: 36-10-0008-01 total book value, All countries; GDP 36-10-0104-01'])
