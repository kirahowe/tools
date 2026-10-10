import sys,csv,collections
sys.path.insert(0,sys.argv[0].rsplit('/',1)[0])
from sc import find
R='raw/'
# --- Debt
d1={r['REF_DATE']:(r['VALUE'],r['VECTOR']) for r in find(R+'38100238/38100238.csv',{'Categories':'Credit market debt to disposable income'})}
d2={r['REF_DATE']:(r['VALUE'],r['VECTOR']) for r in find(R+'11100065/11100065.csv',{'Seasonal adjustment':'Seasonally adjusted at annual rates','Estimates':'Debt service ratio'})}
d3={r['REF_DATE']:(r['VALUE'],r['VECTOR']) for r in find(R+'11100065/11100065.csv',{'Seasonal adjustment':'Seasonally adjusted at annual rates','Estimates':'Debt service ratio, interest only'})}
d4={r['REF_DATE']:(r['VALUE'],r['VECTOR']) for r in find(R+'11100065/11100065.csv',{'Seasonal adjustment':'Seasonally adjusted at annual rates','Estimates':'Mortgage debt service ratio'})}
print('vectors',list(d1.values())[0][1],list(d2.values())[0][1],list(d3.values())[0][1],list(d4.values())[0][1],file=sys.stderr)
w=csv.writer(open('household_debt_canada_quarterly.csv','w',newline=''))
w.writerow(['quarter','credit_market_debt_to_disposable_income_pct_t38100238','debt_service_ratio_pct_t11100065','debt_service_ratio_interest_only_pct_t11100065','mortgage_debt_service_ratio_pct_t11100065'])
for q in sorted(d1):
    w.writerow([q,d1[q][0],d2.get(q,('',))[0],d3.get(q,('',))[0],d4.get(q,('',))[0]])
# OECD debt % NDI
rows=[r for r in csv.DictReader(open(R+'oecd/naagV.csv')) if r['MEASURE']=='LES1M_FD4']
w=csv.writer(open('household_debt_oecd_can_usa_annual.csv','w',newline=''))
w.writerow(['year','country','household_npish_debt_pct_net_disposable_income','source'])
for r in sorted(rows,key=lambda r:(r['REF_AREA'],r['TIME_PERIOD'])):
    w.writerow([r['TIME_PERIOD'],r['REF_AREA'],round(float(r['OBS_VALUE']),1),'OECD NAAG Ch.5 LES1M_FD4'])
# OECD house prices
rows=[r for r in csv.DictReader(open(R+'oecd/hp.csv')) if r['MEASURE'] in('HPI_YDH','RHP','HPI','HPI_RPI','HPI_YDH_AVG')]
for fq,fn in (('Q','oecd_house_price_to_income_can_usa_quarterly.csv'),('A','oecd_house_price_to_income_can_usa_annual.csv')):
    dd=collections.defaultdict(dict)
    for r in rows:
        if r['FREQ']==fq: dd[(r['TIME_PERIOD'],r['REF_AREA'])][r['MEASURE']]=round(float(r['OBS_VALUE']),2)
    w=csv.writer(open(fn,'w',newline=''))
    w.writerow(['period','country','price_to_income_idx2015','price_to_income_pct_of_longterm_avg','real_house_price_idx2015','nominal_house_price_idx2015','price_to_rent_idx2015','price_to_income_rebased_1970eq100'])
    base={}
    for (p,c),v in sorted(dd.items(),key=lambda x:(x[0][1],x[0][0])):
        if c not in base and 'HPI_YDH' in v: base[c]=None
    # rebase to 1970 (annual) or 1970-Q1 average of 1970
    for c in ('CAN','USA'):
        vals=[v['HPI_YDH'] for (p,cc),v in dd.items() if cc==c and p.startswith('1970') and 'HPI_YDH' in v]
        base[c]=sum(vals)/len(vals) if vals else None
    for (p,c),v in sorted(dd.items(),key=lambda x:(x[0][1],x[0][0])):
        y=v.get('HPI_YDH')
        w.writerow([p,c,y,v.get('HPI_YDH_AVG',''),v.get('RHP',''),v.get('HPI',''),v.get('HPI_RPI',''),round(100*y/base[c],1) if y and base[c] else ''])
