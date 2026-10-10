import sys,csv
sys.path.insert(0,sys.argv[0].rsplit('/',1)[0])
from sc import find
R='raw/'
cpi={int(r['REF_DATE']):float(r['VALUE']) for r in find(R+'18100005/18100005.csv',{'GEO':'Canada','Products and product groups':'All-items','UOM':'2002=100'}) if r['VALUE']}
def s(m):
    rs=find(R+'36100305/36100305.csv',{'Labour productivity measures and related measures':m,'Industries, by aggregation':'Business sector, special aggregation'})
    return {int(r['REF_DATE']):float(r['VALUE']) for r in rs if r['VALUE']}, (rs[0]['VECTOR'] if rs else None)
lp,v1=s('Labour productivity'); cph,v2=s('Total compensation per hour worked')
print(v1,v2,min(lp),max(lp),file=sys.stderr)
w=csv.writer(open('productivity_pay_canada_1946_2001_archived.csv','w',newline=''))
w.writerow(['year','bs_labour_productivity_idx1992_t36100305','bs_comp_per_hour_nominal_idx1992_t36100305','cpi_2002_100_t18100005','idx1946_labour_productivity','idx1946_real_comp_per_hour_cpi'])
b=1946
for y in sorted(lp):
    rc=cph[y]/cpi[y]; rb=cph[b]/cpi[b]
    w.writerow([y,lp[y],cph[y],cpi[y],round(100*lp[y]/lp[b],1),round(100*rc/rb,1)])
def cg(d,a,c): return round(100*((d[c]/d[a])**(1/(c-a))-1),2)
rc={y:cph[y]/cpi[y] for y in cph}
print('1946-1975 LP',cg(lp,1946,1975),'real comp/h',cg(rc,1946,1975))
print('1975-2000 LP',cg(lp,1975,2000),'real comp/h',cg(rc,1975,2000))
