import csv,sys
nbsa,pop,out=sys.argv[1:4]
P={}
for r in csv.DictReader(open(pop)):
    if r['age_group']=='18 years and older': P[int(r['year_july1'])]=int(r['persons'])
    if r['age_group']=='All ages': P[('all',int(r['year_july1']))]=int(r['persons'])
w=csv.writer(open(out,'w',newline=''))
w.writerow(['quarter','sector','category','value_millions_cad','adults_18plus_july1','population_july1','pop_year_used','value_per_adult_cad','value_per_capita_cad'])
for r in csv.DictReader(open(nbsa)):
    if r['quarter'] in ('2026Q2','2025Q4','2025Q2','2019Q4') and r['category'] in ('Net worth','Net financial assets'):
        y=int(r['quarter'][:4])
        a=P[y]; t=P[('all',y)]
        v=float(r['value_millions_cad'])*1e6
        w.writerow([r['quarter'],r['sector'],r['category'],r['value_millions_cad'],a,t,y,round(v/a),round(v/t)])
