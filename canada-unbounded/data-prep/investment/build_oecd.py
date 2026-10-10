import csv,collections,sys
R=sys.argv[1]; O=sys.argv[2]
OECD38='AUS AUT BEL CAN CHL COL CRI CZE DNK EST FIN FRA DEU GRC HUN ISL IRL ISR ITA JPN KOR LVA LTU LUX MEX NLD NZL NOR POL PRT SVK SVN ESP SWE CHE TUR GBR USA'.split()
d=collections.defaultdict(dict)
for r in csv.DictReader(open(R+'/oecd/cit_all.csv')):
    if r['MEASURE']=='CIT_C' and r['TARGETING']=='ST' and r['OBS_VALUE']!='':
        d[int(r['TIME_PERIOD'])][r['REF_AREA']]=float(r['OBS_VALUE'])
fed={}
for r in csv.DictReader(open(R+'/oecd/cit.csv')):
    if r['REF_AREA']=='CAN' and r['TARGETING']=='ST' and r['OBS_VALUE']!='':
        fed[(int(r['TIME_PERIOD']),r['MEASURE'])]=float(r['OBS_VALUE'])
with open(O+'/A1_corporate_tax_rates_oecd.csv','w',newline='') as f:
    w=csv.writer(f); w.writerow(['year','canada_combined_statutory_cit_pct','canada_central_cit_pct','canada_central_cit_less_subnational_deduction_pct','canada_subcentral_cit_pct','usa_combined_statutory_cit_pct','oecd38_unweighted_avg_combined_pct','n_oecd_countries_in_avg','source'])
    for y in sorted(d):
        v=[d[y][c] for c in OECD38 if c in d[y]]
        w.writerow([y,d[y].get('CAN'),fed.get((y,'CIT')),fed.get((y,'CIT_LSN')),fed.get((y,'CIT_SCG')),d[y].get('USA'),round(sum(v)/len(v),2),len(v),'OECD Corporate Tax Statistics, dataflow OECD.CTP.TPS:DSD_TAX_CIT@DF_CIT(2.0), measure CIT_C statutory; https://sdmx.oecd.org/public/rest/data/OECD.CTP.TPS,DSD_TAX_CIT@DF_CIT,2.0/.A.CIT_C.ST......'])
p=collections.defaultdict(dict)
for r in csv.DictReader(open(R+'/oecd/pdb.csv')):
    if r['ACTIVITY']=='_T' and r['MEASURE'] in('GDPHRS','GDPEMP') and r['UNIT_MEASURE'] in('USD_PPP_H','USD_PPP_PS') and r['TRANSFORMATION']=='N' and r['PRICE_BASE'] in('LR','V') and r['OBS_VALUE']!='':
        p[int(r['TIME_PERIOD'])][(r['MEASURE'],r['PRICE_BASE'],r['REF_AREA'])]=float(r['OBS_VALUE'])
with open(O+'/B1_labour_productivity_canada_us_oecd.csv','w',newline='') as f:
    w=csv.writer(f); w.writerow(['year','can_gdp_per_hour_usd_ppp_2020','usa_gdp_per_hour_usd_ppp_2020','oecd_gdp_per_hour_usd_ppp_2020','can_pct_of_usa_per_hour_constant','can_pct_of_usa_per_hour_current_ppp','can_gdp_per_worker_usd_ppp_2020','usa_gdp_per_worker_usd_ppp_2020','can_pct_of_usa_per_worker_constant','source'])
    for y in sorted(p):
        q=p[y]; g=lambda k:q.get(k)
        c,u,o=g(('GDPHRS','LR','CAN')),g(('GDPHRS','LR','USA')),g(('GDPHRS','LR','OECD'))
        cv,uv=g(('GDPHRS','V','CAN')),g(('GDPHRS','V','USA'))
        ce,ue=g(('GDPEMP','LR','CAN')),g(('GDPEMP','LR','USA'))
        if c is None and ce is None: continue
        rd=lambda x: round(x,2) if x is not None else ''
        w.writerow([y,rd(c),rd(u),rd(o),rd(100*c/u) if c and u else '',rd(100*cv/uv) if cv and uv else '',rd(ce),rd(ue),rd(100*ce/ue) if ce and ue else '','OECD Productivity Database OECD.SDD.TPS:DSD_PDB@DF_PDB(2.0), total economy (_T); LR = constant prices, constant 2020 PPPs; V = current prices current PPPs'])
