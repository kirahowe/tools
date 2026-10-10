import csv,math
src='raw/36100208/36100208.csv'
K='Multifactor productivity and related variables'; N='North American Industry Classification System (NAICS)'
want={'Labour productivity':'lp','Multifactor productivity':'mfp','Contribution of capital intensity to labour productivity growth':'cap_int','Contribution of labour composition to labour productivity growth':'lab_comp','Real gross domestic product (GDP)':'real_gdp','Hours worked':'hours'}
d={};vec={}
for x in csv.DictReader(open(src,encoding='utf-8-sig')):
    m=x[K]
    if m in want and x['VALUE']!='':
        d[(x[N],want[m],int(x['REF_DATE']))]=float(x['VALUE']); vec[(x[N],want[m])]=x['VECTOR']
inds=sorted(set(k[0] for k in d))
# annual long file for business sector
with open('A1_mfp_decomposition_business_sector_annual.csv','w',newline='') as f:
    w=csv.writer(f); w.writerow(['year','sector','measure','index_2017_100','vector','source'])
    for (i,m,y),v in sorted(d.items()):
        if i in ('Business sector','Business sector, goods, special aggregation','Business sector, services, special aggregation'):
            w.writerow([y,i,m,v,vec[(i,m)],'StatCan 36-10-0208-01'])
periods=[(1961,1973),(1973,1981),(1981,2000),(2000,2008),(2008,2019),(2019,2023),(2000,2023)]
with open('A2_lp_growth_decomposition_by_period.csv','w',newline='') as f:
    w=csv.writer(f); w.writerow(['sector','start','end','labour_productivity_pct_yr','contrib_capital_intensity_pp','contrib_labour_composition_pp','mfp_pct_yr','residual_pp','source'])
    for i in inds:
        for a,b in periods:
            try: g={m:100*(math.log(d[(i,m,b)])-math.log(d[(i,m,a)]))/(b-a) for m in ('lp','cap_int','lab_comp','mfp')}
            except KeyError: continue
            w.writerow([i,a,b]+[round(g[m],2) for m in ('lp','cap_int','lab_comp','mfp')]+[round(g['lp']-g['cap_int']-g['lab_comp']-g['mfp'],2),'StatCan 36-10-0208-01; average annual log growth x100'])
