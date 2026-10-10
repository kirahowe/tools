import csv,sys
src,out=sys.argv[1],sys.argv[2]
w=csv.writer(open(out,'w',newline=''))
w.writerow(['country','indicator','indicator_name','year','value_pct_gdp','is_projection','latest_actual_year','weo_publication_date','dataflow'])
for r in csv.DictReader(open(src,encoding='utf-8-sig')):
    la=r.get('LATEST_ACTUAL_ANNUAL_DATA','')
    proj = (la!='' and int(r['TIME_PERIOD'])>int(la))
    w.writerow([r['COUNTRY'],r['INDICATOR'],r['SERIES_NAME'],r['TIME_PERIOD'],round(float(r['OBS_VALUE']),2),int(proj),la,r['PUBLICATION_DATE'][:10],r['DATAFLOW']])
