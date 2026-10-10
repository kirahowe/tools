import csv,sys,collections
R=sys.argv[1]; O=sys.argv[2]
s=collections.defaultdict(dict); vec={}
for r in csv.DictReader(open(R+'/36100208/36100208.csv',encoding='utf-8-sig')):
    if r['North American Industry Classification System (NAICS)']=='Business sector' and r['Multifactor productivity and related variables'] in ('Labour productivity','Real gross domestic product (GDP)','Multifactor productivity','Capital input','Hours worked') and r['VALUE']:
        k=r['Multifactor productivity and related variables']; s[k][int(r['REF_DATE'])]=float(r['VALUE']); vec[k]=(r['VECTOR'],r['UOM'])
print(vec, min(s['Labour productivity']), max(s['Labour productivity']))
per=[(1961,1973),(1973,1981),(1981,2000),(2000,2008),(2008,2019),(1961,2000),(2000,max(s['Labour productivity']))]
with open(O+'/F1_business_sector_productivity_growth_by_period.csv','w',newline='') as f:
    w=csv.writer(f); w.writerow(['start','end','labour_productivity_cagr_pct','mfp_cagr_pct','real_gdp_cagr_pct','source'])
    for a,b in per:
        g=lambda k: round(100*((s[k][b]/s[k][a])**(1/(b-a))-1),2)
        w.writerow([a,b,g('Labour productivity'),g('Multifactor productivity'),g('Real gross domestic product (GDP)'),'StatCan 36-10-0208-01, business sector, index levels (vectors '+vec['Labour productivity'][0]+','+vec['Multifactor productivity'][0]+','+vec['Real gross domestic product (GDP)'][0]+')'])
with open(O+'/F1_business_sector_labour_productivity_index_annual.csv','w',newline='') as f:
    w=csv.writer(f); w.writerow(['year','labour_productivity_index','mfp_index','real_gdp_index','uom'])
    for y in sorted(s['Labour productivity']):
        w.writerow([y,s['Labour productivity'][y],s['Multifactor productivity'].get(y,''),s['Real gross domestic product (GDP)'].get(y,''),vec['Labour productivity'][1]])
