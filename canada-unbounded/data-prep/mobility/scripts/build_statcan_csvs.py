import sys, csv, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tablelib import find
root=sys.argv[1]; raw=os.path.join(root,'raw')
U21='https://www150.statcan.gc.ca/n1/pub/11f0019m/11f0019m2021001-eng.htm'
SRC21='Connolly, Haeck & Lapierre (2021), StatCan 11F0019M No. 458'
f21=os.path.join(raw,'statcan_11f0019m2021001.html')
# Province x cohort rank-rank slopes (Chart 3)
t=find(f21,'Data table for Chart 3')
hdr=t['rows'][0][1:]
names={'B.C.':'British Columbia','Alta.':'Alberta','Sask.':'Saskatchewan','Man.':'Manitoba','Ont.':'Ontario','Que.':'Quebec','N.B.':'New Brunswick','N.S.':'Nova Scotia','P.E.I.':'Prince Edward Island','N.L.':'Newfoundland and Labrador','Y.T.':'Yukon','Nvt. and N.W.T.':'Nunavut and Northwest Territories'}
cohort_years={'1963':'1963-1966','1967':'1967-1970','1972':'1972-1975','1977':'1977-1980','1982':'1982-1985'}
rows=[]
for r in t['rows'][1:]:
    if r[0] in cohort_years:
        for h,v in zip(hdr,r[1:]):
            rows.append([names[h],h,r[0],cohort_years[r[0]],v,'rank_rank_slope','within-province ranks; child individual pre-tax total income avg ages 25-29; parent family income avg child ages 15-19',SRC21+' Chart 3 data table',U21])
with open(os.path.join(root,'canada_rank_slope_by_province_cohort.csv'),'w',newline='') as f:
    w=csv.writer(f); w.writerow(['province','province_abbr','cohort_label','birth_years','value','measure','definition','source','url']); w.writerows(rows)
# National by cohort: Table 3 + Charts 4,5,6 + Table 2 Gini
out=[]
t=find(f21,'Table 3 Mobility estimates by birth cohort')
section=None
for r in t['rows']:
    if len(r)==1 or (len(r)>1 and all(c=='' for c in r[1:])):
        section=r[0]; continue
    if r[0].startswith('Ages') and section:
        for c,v in zip(['1963','1967','1972','1977','1982'],r[1:]):
            if v and v[0].isdigit():
                meas=section.replace('IGE of incom e','IGE of income').strip()
                out.append([c,cohort_years[c],meas,r[0],v,'coefficient',SRC21+' Table 3',U21])
for chart,meas in [('Data table for Chart 4','P(child bottom quintile | parent quintile)'),('Data table for Chart 5','P(child top quintile | parent quintile)'),('Data table for Chart 6','P(child middle three quintiles | parent quintile)')]:
    t=find(f21,chart)
    for r in t['rows']:
        if r[0].startswith('Quintile'):
            for c,v in zip(['1963','1967','1972','1977','1982'],r[1:]):
                out.append([c,cohort_years[c],meas+' ; parent '+r[0],'Ages 25 to 29',v,'probability',SRC21+' '+chart,U21])
t=find(f21,'Table 2 Descriptive statistics')
for r in t['rows']:
    if r[0].startswith('Gini coefficient, parental'):
        for c,v in zip(['1963','1967','1972','1977','1982'],r[1:]):
            out.append([c,cohort_years[c],r[0],'parents (child aged 15-19)',v,'Gini x100',SRC21+' Table 2',U21])
with open(os.path.join(root,'canada_relative_mobility_by_cohort.csv'),'w',newline='') as f:
    w=csv.writer(f); w.writerow(['cohort_label','birth_years','measure','child_age','value','unit','source','url']); w.writerows(out)
# Absolute mobility 2017
U17='https://www150.statcan.gc.ca/n1/pub/11-626-x/11-626-x2017073-eng.htm'
f17=os.path.join(raw,'statcan_11-626-x2017073.html')
t=find(f17,'Description for Chart 1')
out=[]
for r in t['rows']:
    if r[0].isdigit():
        out.append([r[0],'Canada','share with family income >= parents at age 30 (adjusted for number of adults)',r[1],'percent','Ostrovsky (2017) StatCan 11-626-X No. 073, Chart 1',U17])
        out.append([r[0],'Canada','share with family income >= parents at age 30 (unadjusted family income)',r[2],'percent','Ostrovsky (2017) StatCan 11-626-X No. 073, Chart 1',U17])
t=find(os.path.join(raw,'statcan_esr_2023012.html'),'Data table for Chart 1')
U23='https://www150.statcan.gc.ca/n1/pub/36-28-0001/2023012/article/00001-eng.htm'
for r in t['rows']:
    if len(r)==2 and r[1].isdigit():
        out.append(['1985',r[0],'share with family income > parents at age 30 (Manduca et al. harmonized, pre-tax post-transfer)',r[1],'percent','Ostrovsky (2023) StatCan 36-28-0001 Vol.3 no.12, Chart 1, citing Manduca et al. (2024 AEJ:Applied)',U23])
with open(os.path.join(root,'absolute_mobility_canada_us.csv'),'w',newline='') as f:
    w=csv.writer(f); w.writerow(['child_birth_cohort','country','measure','value','unit','source','url']); w.writerows(out)
print('ok')
