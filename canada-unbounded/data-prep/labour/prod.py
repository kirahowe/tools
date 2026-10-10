import sys,csv,collections
sys.path.insert(0,sys.argv[0].rsplit('/',1)[0])
from sc import find
R='raw/'
cpi={int(r['REF_DATE']):float(r['VALUE']) for r in find(R+'18100005/18100005.csv',{'GEO':'Canada','Products and product groups':'All-items','UOM':'2002=100'}) if r['VALUE']}
# 36-10-0208 annual business sector
def s208(var):
    return {int(r['REF_DATE']):float(r['VALUE']) for r in find(R+'36100208/36100208.csv',{'Multifactor productivity and related variables':var,'North American Industry Classification System (NAICS)':'Business sector'}) if r['VALUE']}
lp208=s208('Labour productivity'); comp=s208('Labour compensation'); hrs=s208('Hours worked'); gdp=s208('Gross domestic product (GDP)')
# units
for v in ['Labour compensation','Hours worked','Gross domestic product (GDP)','Labour productivity']:
    r=find(R+'36100208/36100208.csv',{'Multifactor productivity and related variables':v,'North American Industry Classification System (NAICS)':'Business sector','REF_DATE':'2017'})[0]
    print(v,r['UOM'],r['SCALAR_FACTOR'],r['VALUE'],r['VECTOR'],file=sys.stderr)
# 36-10-0206 quarterly -> annual average
q=collections.defaultdict(lambda:collections.defaultdict(list)); vec={}
for r in find(R+'36100206/36100206.csv',{}):
    if r['VALUE']:
        m=r['Labour productivity measures and related measures']; q[m][int(r['REF_DATE'][:4])].append(float(r['VALUE'])); vec[m]=r['VECTOR']
print(vec,file=sys.stderr)
a206={m:{y:sum(v)/4 for y,v in d.items() if len(v)==4} for m,d in q.items()}
# LFS median hourly wage
med={int(r['REF_DATE']):float(r['VALUE']) for r in find(R+'14100064/14100064.csv',{'GEO':'Canada','Wages':'Median hourly wage rate','Type of work':'Both full- and part-time employees','North American Industry Classification System (NAICS)':'Total employees, all industries','Gender':'Total - Gender','Age group':'15 years and over'}) if r['VALUE']}
avg={int(r['REF_DATE']):float(r['VALUE']) for r in find(R+'14100064/14100064.csv',{'GEO':'Canada','Wages':'Average hourly wage rate','Type of work':'Both full- and part-time employees','North American Industry Classification System (NAICS)':'Total employees, all industries','Gender':'Total - Gender','Age group':'15 years and over'}) if r['VALUE']}
w=csv.writer(open('productivity_pay_canada_annual.csv','w',newline=''))
w.writerow(['year','cpi_2002_100','bs_labour_productivity_idx2017_t36100208','bs_labour_comp_musd_t36100208','bs_hours_worked_millions_t36100208','bs_nominal_gdp_musd_t36100208','bs_comp_per_hour_nominal_t36100208','bs_real_comp_per_hour_cpi_2025dollars','bs_labour_share_pct_t36100208',
 'bs_labour_productivity_idx2017_t36100206','bs_comp_per_hour_idx2017_t36100206','bs_labour_share_idx2017_t36100206','bs_implicit_price_deflator_idx2017_t36100206',
 'lfs_median_hourly_wage_nominal_t14100064','lfs_median_hourly_wage_2025dollars','lfs_average_hourly_wage_nominal_t14100064',
 'idx1961_productivity','idx1961_real_comp_per_hour','idx1981_productivity_q','idx1981_real_comp_per_hour_q','idx1997_productivity_q','idx1997_real_comp_per_hour_q','idx1997_real_median_hourly_wage','idx1997_real_average_hourly_wage'])
c25=cpi[2025]
def f(x,n=3): return '' if x is None else round(x,n)
for y in range(1961,2026):
    lp=lp208.get(y); cph=comp[y]/hrs[y] if y in comp and y in hrs else None
    rc=cph*c25/cpi[y] if cph else None
    ls=100*comp[y]/gdp[y] if y in comp and y in gdp else None
    lpq=a206['Labour productivity'].get(y); cq=a206['Total compensation per hour worked'].get(y)
    rcq=cq/cpi[y] if cq else None
    mw=med.get(y); aw=avg.get(y)
    row=[y,cpi[y],lp,comp.get(y),hrs.get(y),gdp.get(y),f(cph),f(rc,2),f(ls,2),f(lpq),f(cq),f(a206['Labour share'].get(y)),f(a206['Implicit price deflator'].get(y)),mw,f(mw*c25/cpi[y],2) if mw else '',aw,
      f(100*lp/lp208[1961],1) if lp else '', f(100*rc/(comp[1961]/hrs[1961]*c25/cpi[1961]),1) if rc else '',
      f(100*lpq/a206['Labour productivity'][1981],1) if lpq else '', f(100*rcq/(a206['Total compensation per hour worked'][1981]/cpi[1981]),1) if rcq else '',
      f(100*lpq/a206['Labour productivity'][1997],1) if lpq else '', f(100*rcq/(a206['Total compensation per hour worked'][1997]/cpi[1997]),1) if rcq else '',
      f(100*(mw/cpi[y])/(med[1997]/cpi[1997]),1) if mw else '', f(100*(aw/cpi[y])/(avg[1997]/cpi[1997]),1) if aw else '']
    w.writerow(row)
