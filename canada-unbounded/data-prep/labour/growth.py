import sys,csv,collections
sys.path.insert(0,sys.argv[0].rsplit('/',1)[0])
from sc import find
R='raw/'
g=collections.defaultdict(list)
for r in find(R+'36100104/36100104.csv',{'Prices':'Chained (2017) dollars','Seasonal adjustment':'Seasonally adjusted at annual rates','Estimates':'Gross domestic product at market prices'}):
    if r['VALUE']: g[int(r['REF_DATE'][:4])].append(float(r['VALUE'])); vec=r['VECTOR']
p=collections.defaultdict(list)
for r in find(R+'17100009/17100009.csv',{'GEO':'Canada'}):
    if r['VALUE']: p[int(r['REF_DATE'][:4])].append(float(r['VALUE']))
gdi={int(r['REF_DATE']):float(r['VALUE']) for r in find(R+'36100229/36100229.csv',{'GEO':'Canada','Long-run variables':'Real gross domestic income (GDI) per capita'}) if r['VALUE']}
rhdi={int(r['REF_DATE']):float(r['VALUE']) for r in find(R+'36100229/36100229.csv',{'GEO':'Canada','Long-run variables':'Real household disposable income per capita'}) if r['VALUE']}
u=[r for r in find(R+'36100229/36100229.csv',{'GEO':'Canada','Long-run variables':'Real gross domestic income (GDI) per capita'})][:1]
print('36100229 UOM',u[0]['UOM'],u[0]['VECTOR'],file=sys.stderr); print('GDP vec',vec,file=sys.stderr)
mad={}
for r in csv.DictReader(open(R+'maddison/can_usa.csv')):
    if r['gdppc']: mad[(r['countrycode'],int(r['year']))]=float(r['gdppc'])
w=csv.writer(open('real_gdp_per_capita_canada_long_run.csv','w',newline=''))
w.writerow(['year','statcan_real_gdp_per_capita_2017dollars','statcan_real_gdi_per_capita_t36100229','statcan_real_household_disposable_income_per_capita_t36100229','maddison_gdppc_CAN_2011ppp','maddison_gdppc_USA_2011ppp'])
pc={}
for y in range(1926,2026):
    v=''
    if len(g.get(y,[]))==4 and len(p.get(y,[]))==4:
        v=round(sum(g[y])/4*1e6/(sum(p[y])/4),0); pc[y]=v
    w.writerow([y,v,gdi.get(y,''),rhdi.get(y,''),mad.get(('CAN',y),''),mad.get(('USA',y),'')])
def cagr(d,a,b): return round(100*((d[b]/d[a])**(1/(b-a))-1),2)
M={y:v for (c,y),v in mad.items() if c=='CAN'}; MU={y:v for (c,y),v in mad.items() if c=='USA'}
print('Maddison CAN 1946-1975',cagr(M,1946,1975),'1950-1975',cagr(M,1950,1975),'1980-2022',cagr(M,1980,2022),'2000-2022',cagr(M,2000,2022))
print('Maddison USA 1946-1975',cagr(MU,1946,1975),'1950-1975',cagr(MU,1950,1975),'1980-2022',cagr(MU,1980,2022))
print('StatCan GDI pc 1950-1975',cagr(gdi,1950,1975),'1980-2016',cagr(gdi,1980,2016))
print('StatCan RHDI pc 1946-1975',cagr(rhdi,1946,1975),'1980-2016',cagr(rhdi,1980,2016))
print('StatCan GDP pc 1961-1975',cagr(pc,1961,1975),'1980-2025',cagr(pc,1980,2025),'2000-2025',cagr(pc,2000,2025),'2015-2025',cagr(pc,2015,2025), pc[1961],pc[1975],pc[1980],pc[2025])
