import csv, sys, os
raw, out = sys.argv[1], sys.argv[2]
PROVS = ['Canada','Newfoundland and Labrador','Prince Edward Island','Nova Scotia','New Brunswick','Quebec','Ontario','Manitoba','Saskatchewan','Alberta','British Columbia']
def rows(t):
    with open(os.path.join(raw,t,t+'.csv'),encoding='utf-8-sig') as f:
        yield from csv.DictReader(f)
def num(v):
    try: return float(v)
    except: return None
# 1. median after-tax income
d={}
for r in rows('11100190'):
    if r['Income concept']=='Median after-tax income' and r['Economic family type']=='Economic families and persons not in an economic family' and (r['GEO'] in PROVS or r['GEO']=='Atlantic provinces'):
        d[(int(r['REF_DATE']),r['GEO'])]=(num(r['VALUE']),r['STATUS'],r['UOM'])
with open(os.path.join(out,'median_after_tax_income_by_province.csv'),'w',newline='') as f:
    w=csv.writer(f); w.writerow(['year','geo','median_after_tax_income','uom','pct_of_canada','statcan_status','source'])
    for (y,g),(v,s,u) in sorted(d.items()):
        c=d.get((y,'Canada'),(None,))[0]
        w.writerow([y,g,v,u,round(100*v/c,1) if v and c else '',s,'StatCan Table 11-10-0190-01'])
# 2. SFS median net worth
d={}
for r in rows('11100016'):
    if r['Assets and debts'].startswith('Net worth') and r['Age group']=='All ages' and r['Economic family type']=='Economic families and persons not in an economic family' and r['Confidence intervals']=='Estimate' and r['Statistics'] in ('Median value for those holding asset or debt','Average value for those holding asset or debt') and (r['GEO'] in PROVS or r['GEO']=='Atlantic'):
        d[(int(r['REF_DATE']),r['GEO'],r['Statistics'])]=(num(r['VALUE']),r['STATUS'],r['UOM'],r['SCALAR_FACTOR'])
stats=set(k[2] for k in d)
print('SFS stats found',stats, 'years', sorted(set(k[0] for k in d)))
with open(os.path.join(out,'sfs_net_worth_by_province.csv'),'w',newline='') as f:
    w=csv.writer(f); w.writerow(['year','geo','statistic','net_worth','uom','scalar','pct_of_canada','statcan_status','source'])
    for (y,g,s),(v,st,u,sc) in sorted(d.items()):
        c=d.get((y,'Canada',s),(None,))[0]
        w.writerow([y,g,s,v,u,sc,round(100*v/c,1) if v and c else '',st,'StatCan Table 11-10-0016-01'])
# 3. GDP per capita
gdp={}; 
ests=set()
for r in rows('36100222'):
    ests.add((r['Prices'],r['Estimates']))
    if r['Estimates']=='Gross domestic product at market prices' and r['GEO'] in PROVS and r['Prices'] in ('Current prices','Chained (2017) dollars'):
        gdp[(int(r['REF_DATE']),r['GEO'],r['Prices'])]=num(r['VALUE'])
pop={}
for r in rows('17100005'):
    if r['Gender'] in ('Total - gender','Both sexes') and r['Age group']=='All ages' and r['GEO'] in PROVS:
        pop[(int(r['REF_DATE']),r['GEO'])]=num(r['VALUE'])
prices=sorted(set(k[2] for k in gdp)); print('prices',prices)
with open(os.path.join(out,'gdp_per_capita_by_province.csv'),'w',newline='') as f:
    w=csv.writer(f); w.writerow(['year','geo','prices','gdp_millions','population_july1','gdp_per_capita_dollars','pct_of_canada','source'])
    for (y,g,p),v in sorted(gdp.items()):
        pp=pop.get((y,g))
        if v is None or not pp: continue
        pc=v*1e6/pp
        cv=gdp.get((y,'Canada',p)); cp=pop.get((y,'Canada'))
        cpc=cv*1e6/cp if cv and cp else None
        w.writerow([y,g,p,v,int(pp),round(pc),round(100*pc/cpc,1) if cpc else '','StatCan Tables 36-10-0222-01 (GDP, expenditure-based) and 17-10-0005-01 (population, July 1)'])
