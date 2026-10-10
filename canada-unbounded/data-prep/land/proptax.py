import csv,sys,collections
f20,f147,gdpf,out=sys.argv[1:5]
WANT={'Revenue [1]':'total_revenue','Taxes [11]':'total_taxes','Recurrent taxes on immovable property [1131]':'recurrent_taxes_immovable_property','Taxes on property [113]':'taxes_on_property'}
rows=[]
def load(fn,comp_label):
    seen=set()
    for r in csv.DictReader(open(fn,encoding='utf-8-sig')):
        s=r['Statement of operations and balance sheet']
        if r['Display value']=='Stocks' or s not in WANT or not r['VALUE']: continue
        key=(r['REF_DATE'],r['GEO'],r['Public sector components'],s)
        if key in seen: continue
        seen.add(key)
        rows.append([r['REF_DATE'],r['GEO'],comp_label.get(r['Public sector components'],r['Public sector components']),WANT[s],int(float(r['VALUE']))])
load(f20,{'Municipalities and other local public administrations':'local_governments'})
load(f147,{'Consolidated Canadian general government':'all_governments_consolidated','Consolidated provincial-territorial and local governments':'prov_terr_and_local_consolidated'})
agg=collections.defaultdict(int); cnt=collections.Counter()
for y,g,c,m,v in rows:
    if c=='local_governments' and g!='Canada': agg[(y,m)]+=v; cnt[(y,m)]+=1
for (y,m),v in agg.items():
    rows.append([y,'Canada (sum of provinces/territories, computed)','local_governments',m,v])
gdp=collections.defaultdict(list)
for r in csv.DictReader(open(gdpf,encoding='utf-8-sig')):
    if r['GEO']=='Canada' and r['Prices']=='Current prices' and r['Seasonal adjustment']=='Seasonally adjusted at annual rates' and r['Estimates']=='Gross domestic product at market prices' and r['VALUE']:
        gdp[r['REF_DATE'][:4]].append(float(r['VALUE']))
ag={y:sum(v)/4 for y,v in gdp.items() if len(v)==4}
w=csv.writer(open(out,'w',newline=''))
w.writerow(['year','geo','government_sector','measure','value_cad_millions','pct_of_gdp_canada_calendar','source_table'])
for y,g,c,m,v in sorted(rows):
    p=round(100*v/ag[y],2) if g.startswith('Canada') and y in ag else ''
    w.writerow([y,g,c,m,v,p,'StatCan 10-10-0020-01' if c=='local_governments' else 'StatCan 10-10-0147-01'])
