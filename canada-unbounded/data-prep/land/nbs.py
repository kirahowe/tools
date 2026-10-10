import csv,sys,collections
nbs,gdpf,out1,out2=sys.argv[1:5]
CATS=['Non-financial assets','Produced non-financial assets','Dwellings','Other buildings and structures','Land','Land underlying dwellings','Land underlying buildings and structures','Agricultural land','Net worth','Natural resources excluding land','Non-produced non-financial assets','Total assets']
SECT={'National balance sheets':'national','Households':'households','Households and non-profit institutions serving households':'hh_npish','Non-financial corporations':'nonfin_corps','General governments':'govt'}
d=collections.defaultdict(dict)
for r in csv.DictReader(open(nbs,encoding='utf-8-sig')):
    if r['GEO']!='Canada' or r['Valuation']!='Market value': continue
    if r['Sectors'] not in SECT or r['Categories'] not in CATS: continue
    if not r['VALUE']: continue
    d[(r['REF_DATE'],SECT[r['Sectors']])][r['Categories']]=float(r['VALUE'])
gdp={}
for r in csv.DictReader(open(gdpf,encoding='utf-8-sig')):
    if r['GEO']=='Canada' and r['Prices']=='Current prices' and r['Seasonal adjustment']=='Seasonally adjusted at annual rates' and r['Estimates']=='Gross domestic product at market prices' and r['VALUE']:
        gdp[r['REF_DATE']]=float(r['VALUE'])
# annual GDP = mean of 4 quarterly SAAR
ann=collections.defaultdict(list)
for k,v in gdp.items(): ann[k[:4]].append(v)
annual_gdp={y:sum(v)/4 for y,v in ann.items() if len(v)==4}
# trailing 4-quarter GDP for each quarter
qs=sorted(gdp)
def trail(q):
    i=qs.index(q); 
    if i<3: return None
    return sum(gdp[x] for x in qs[i-3:i+1])/4
w=csv.writer(open(out1,'w',newline=''))
w.writerow(['ref_date','quarter','sector','category','value_cad_millions','source'])
for (date,s),cats in sorted(d.items()):
    q=f"{date[:4]}Q{(int(date[5:7])-1)//3+1}"
    for c,v in cats.items():
        w.writerow([date,q,s,c,int(v),'StatCan 36-10-0580-01 market value'])
# summary: Q4 year-end (and latest quarter)
w=csv.writer(open(out2,'w',newline=''))
w.writerow(['period','sector','land_total','land_under_dwellings','land_under_other_bldgs','agricultural_land','dwellings_structures','other_buildings_structures','non_financial_assets','net_worth','land_share_nfa_pct','land_under_dwellings_share_of_dwelling_plus_land_pct','land_share_net_worth_pct','gdp_trailing4q_saar','land_to_gdp_pct','res_land_to_gdp_pct','units'])
dates=sorted({k[0] for k in d})
sel=[x for x in dates if x.endswith('-10')]+[dates[-1]]
for date in sel:
  for s in ['national','households','hh_npish','nonfin_corps','govt']:
    c=d.get((date,s));
    if not c: continue
    q=f"{date[:4]}Q{(int(date[5:7])-1)//3+1}"
    L=c.get('Land'); LD=c.get('Land underlying dwellings'); D=c.get('Dwellings'); N=c.get('Non-financial assets'); NW=c.get('Net worth')
    g=trail(date)
    f=lambda a,b: round(100*a/b,2) if a is not None and b else ''
    w.writerow([q,s,L,LD,c.get('Land underlying buildings and structures'),c.get('Agricultural land'),D,c.get('Other buildings and structures'),N,NW,f(L,N),f(LD,(LD or 0)+(D or 0)) if LD else '',f(L,NW),round(g) if g else '',f(L,g),f(LD,g),'CAD millions (stocks end of quarter); GDP = avg of 4 quarterly SAAR nominal, 36-10-0104-01'])
print(sel[0],sel[-1],len(sel))
