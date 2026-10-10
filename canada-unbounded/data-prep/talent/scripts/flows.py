import csv,sys,collections
raw=sys.argv[1]; out=sys.argv[2]
def rd(p):
    with open(p,encoding='utf-8-sig') as f: yield from csv.DictReader(f)
comp=collections.defaultdict(lambda: collections.defaultdict(float)); nq=collections.defaultdict(lambda: collections.defaultdict(int))
for r in rd(raw+'/t17100040/17100040.csv'):
    if r['GEO']!='Canada' or not r['VALUE']: continue
    y=int(r['REF_DATE'][:4]); c=r['Components of population growth']
    comp[y][c]+=float(r['VALUE']); nq[y][c]+=1
pop={}
for r in rd(raw+'/t17100009/17100009.csv'):
    if r['GEO']=='Canada' and r['REF_DATE'].endswith('-07-01') and r['VALUE']: pop[int(r['REF_DATE'][:4])]=float(r['VALUE'])
npr={}
for r in rd(raw+'/t17100121/17100121.csv'):
    if r['GEO']=='Canada' and r['Non-permanent resident types']=='Total, non-permanent residents' and r['REF_DATE'].endswith('-07-01') and r['VALUE']:
        npr[int(r['REF_DATE'][:4])]=float(r['VALUE'])
w=csv.writer(open(out,'w',newline=''))
w.writerow(['year','series','value','unit','source','note'])
names={'Immigrants':'immigrants','Emigrants':'emigrants','Returning emigrants':'returning_emigrants','Net temporary emigration':'net_temporary_emigration','Net emigration':'net_emigration','Net non-permanent residents':'net_nonpermanent_residents'}
for y in range(2000,2026):
    note='method break: from Jul 2016 short-term emigrants/returnees are inside emigrants & returning emigrants; net temporary emigration no longer calculated' if y>=2016 else ''
    if y==2016: note='mixed-method year (H1 old method incl. net temp emig; H2 new); '+note
    for c,n in names.items():
        if n=='net_temporary_emigration' and y>=2017: continue
        w.writerow([y,n,round(comp[y][c]),'persons','StatCan 17-10-0040-01 (quarterly summed to calendar year)',note])
    if y in pop:
        P=pop[y]
        w.writerow([y,'population_july1',int(P),'persons','StatCan 17-10-0009-01',''])
        for n,c in [('immigration_rate_per1000','Immigrants'),('emigration_rate_per1000','Emigrants'),('returning_emigrant_rate_per1000','Returning emigrants'),('net_emigration_rate_per1000','Net emigration')]:
            w.writerow([y,n,round(comp[y][c]/P*1000,3),'per 1,000 population','calc from 17-10-0040-01 / 17-10-0009-01 (July 1 pop)',note])
        w.writerow([y,'net_permanent_flow_immigrants_minus_net_emigration',round(comp[y]['Immigrants']-comp[y]['Net emigration']),'persons','calc','immigrants minus net emigration (emig - returning + net temp emig)'])
    if y in npr: w.writerow([y,'npr_stock_july1',int(npr[y]),'persons','StatCan 17-10-0121-01','stock of non-permanent residents July 1'])
