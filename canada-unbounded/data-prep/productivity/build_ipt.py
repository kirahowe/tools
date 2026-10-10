import csv,collections
gdp=collections.defaultdict(list)
for r in csv.DictReader(open('raw/36100104/36100104.csv',encoding='utf-8-sig')):
    if r['VECTOR']=='v62305783' and r['VALUE']: gdp[int(r['REF_DATE'][:4])].append(float(r['VALUE']))
G={y:sum(l)/4 for y,l in gdp.items() if len(l)==4}
d={}
for r in csv.DictReader(open('raw/12100088/12100088.csv',encoding='utf-8-sig')):
    if r['GEO']=='Canada' and r['Product'] in ('Total products','Total goods','Total services') and r['Trade flow detail'] in ('Interprovincial exports','International exports') and r['VALUE']:
        d[(int(r['REF_DATE']),r['Product'],r['Trade flow detail'])]=float(r['VALUE'])
with open('C1_interprovincial_vs_international_exports.csv','w',newline='') as f:
    w=csv.writer(f); w.writerow(['year','product','interprovincial_exports_cad_mn','international_exports_cad_mn','interprovincial_pct_gdp','international_pct_gdp','interprovincial_share_of_total_exports_pct','source'])
    for (y,p,t),v in sorted(d.items()):
        if t!='Interprovincial exports': continue
        i=d[(y,p,'International exports')]
        w.writerow([y,p,v,i,round(100*v/G[y],1),round(100*i/G[y],1),round(100*v/(v+i),1),'StatCan 12-10-0088-01 (basic prices, Canada total); GDP 36-10-0104-01 v62305783'])
