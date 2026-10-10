import csv,collections
us=collections.defaultdict(lambda: collections.defaultdict(list))
for r in csv.DictReader(open('raw/fred/fred.csv')):
    y=int(r['observation_date'][:4])
    for k in ('GDP','PRFI','PNFI','Y001RC1Q027SBEA','Y033RC1Q027SBEA','B009RC1Q027SBEA'):
        if r[k] not in ('','.'): us[y][k].append(float(r[k]))
ca={int(r['year']):r for r in csv.DictReader(open('D1_residential_vs_business_investment_pct_gdp_annual.csv'))}
w=csv.writer(open('D4_residential_vs_business_investment_canada_vs_us.csv','w',newline=''))
w.writerow(['year','country','residential_pct_gdp','business_nonres_pct_gdp','machinery_equipment_pct_gdp','ipp_pct_gdp','nonres_structures_pct_gdp','source'])
for y in range(1961,2026):
    if y in ca:
        c=ca[y]; w.writerow([y,'Canada',c['residential_total_pct_gdp'],c['business_nonres_total_pct_gdp'],c['business_machinery_equipment_pct_gdp'],c['business_ipp_pct_gdp'],c['business_nonres_structures_pct_gdp'],'StatCan 36-10-0108-01 / 36-10-0104-01 (business sector)'])
    u=us.get(y)
    if u and all(len(u[k])==4 for k in ('GDP','PRFI','PNFI','Y001RC1Q027SBEA','Y033RC1Q027SBEA','B009RC1Q027SBEA')):
        G=sum(u['GDP'])
        f=lambda k: round(100*sum(u[k])/G,2)
        w.writerow([y,'United States',f('PRFI'),f('PNFI'),f('Y033RC1Q027SBEA'),f('Y001RC1Q027SBEA'),f('B009RC1Q027SBEA'),'US BEA NIPA via FRED: PRFI, PNFI, Y033RC1Q027SBEA (equipment), Y001RC1Q027SBEA (IPP), B009RC1Q027SBEA (structures), GDP; private sector; annual avg of SAAR quarters'])
