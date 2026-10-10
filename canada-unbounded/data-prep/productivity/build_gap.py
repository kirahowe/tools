import csv,math
d={}
for r in csv.DictReader(open('raw/oecd/pdb_from_investment.csv')):
    if r['ACTIVITY']=='_T' and r['TRANSFORMATION']=='N' and r['REF_AREA'] in ('CAN','USA','OECD') and r['OBS_VALUE']:
        d[(r['REF_AREA'],r['MEASURE'],r['UNIT_MEASURE'],r['PRICE_BASE'],int(r['TIME_PERIOD']))]=float(r['OBS_VALUE'])
def g(a,m,u,p,y): return d.get((a,m,u,p,y))
rows=[]
for y in range(1990,2026):
    cpc=g('CAN','GDPPOP','USD_PPP_PS','V',y); upc=g('USA','GDPPOP','USD_PPP_PS','V',y)
    cpcr=g('CAN','GDPPOP','USD_PPP_PS','LR',y); upcr=g('USA','GDPPOP','USD_PPP_PS','LR',y)
    ccad=g('CAN','GDPPOP','XDC_PS','V',y)
    ch=g('CAN','GDPHRS','USD_PPP_H','V',y); uh=g('USA','GDPHRS','USD_PPP_H','V',y)
    if None in (cpc,upc,ch,uh): continue
    # hours per capita = (GDP/pop)/(GDP/hour)
    chp=cpc/ch; uhp=upc/uh
    gap=math.log(cpc/upc); prod=math.log(ch/uh); util=math.log(chp/uhp)
    rows.append([y,round(cpc),round(upc),round(100*cpc/upc,1),round(upc-cpc),round(ccad) if ccad else '',
      round(ccad*(upc/cpc-1)) if ccad else '',
      round(cpcr) if cpcr else '',round(upcr) if upcr else '',
      round(chp,1),round(uhp,1),round(100*prod/gap,1) if gap else '',round(100*util/gap,1) if gap else '',
      'OECD Productivity Database DF_PDB(2.0), total economy; GDPPOP & GDPHRS current PPP (V), constant 2020 PPP (LR); CAD per capita XDC_PS'])
w=csv.writer(open('G1_gdp_per_capita_gap_canada_us.csv','w',newline=''))
w.writerow(['year','can_gdp_per_capita_usd_ppp_current','usa_gdp_per_capita_usd_ppp_current','can_pct_of_usa','gap_usd_ppp_per_person','can_gdp_per_capita_cad','gap_in_cad_if_at_us_level_per_person','can_gdp_per_capita_usd_ppp2020','usa_gdp_per_capita_usd_ppp2020','can_hours_per_capita','usa_hours_per_capita','share_of_log_gap_from_productivity_pct','share_of_log_gap_from_hours_per_capita_pct','source'])
w.writerows(rows)
for r in rows:
    if r[0] in (1990,2000,2007,2014,2019,2022,2023,2024,2025): print(r[:13])
# counterfactual: Canada GDP/hour grows like US / OECD since 2000
for a in ('USA','OECD'):
    c0,c1=g('CAN','GDPHRS','USD_PPP_H','LR',2000),g('CAN','GDPHRS','USD_PPP_H','LR',2025)
    o0,o1=g(a,'GDPHRS','USD_PPP_H','LR',2000),g(a,'GDPHRS','USD_PPP_H','LR',2025)
    print(a,'CAN growth',round(100*(c1/c0-1),1),a,round(100*(o1/o0-1),1),'counterfactual level uplift %',round(100*((o1/o0)/(c1/c0)-1),1))
