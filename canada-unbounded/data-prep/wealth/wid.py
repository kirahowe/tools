import csv,sys
out=sys.argv[1]; files=sys.argv[2:]
want={'shwealj992':('net personal wealth','share'),'sptincj992':('pre-tax national income','share'),'sdiincj992':('post-tax national income','share'),'ghwealj992':('net personal wealth','gini'),'ghwealj999':None}
pcts={'p0p50':'Bottom 50%','p50p90':'Middle 40% (P50-P90)','p90p100':'Top 10%','p99p100':'Top 1%','p99.9p100':'Top 0.1%','p99.99p100':'Top 0.01%','p99.999p100':'Top 0.001%','p0p100':'All'}
w=csv.writer(open(out,'w',newline=''))
w.writerow(['country','year','concept','measure','group','percentile_code','value','wid_variable','unit_population','data_quality_flag'])
rows=[]
for f in files:
    for r in csv.DictReader(open(f,encoding='utf-8'),delimiter=';'):
        v=r['variable']
        if v in want and want[v] and r['percentile'] in pcts:
            c,m=want[v]
            if m=='gini' and r['percentile']!='p0p100': continue
            if m=='share' and r['percentile']=='p0p100': continue
            rows.append([r['country'],int(r['year']),c,m,pcts[r['percentile']] if m=='share' else 'All adults',r['percentile'],round(float(r['value']),5),v,'equal-split adults (20+)',r['data_quality']])
rows.sort()
for x in rows: w.writerow(x)
print(len(rows))
