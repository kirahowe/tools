import csv,sys
src,out=sys.argv[1],sys.argv[2]
keep={'SH_TOP1','SH_TOP5','SH_TOP10','SH_BOT40','M_NW_RATIO','NW'}
rows=[]
for r in csv.DictReader(open(src,encoding='utf-8-sig')):
    if r['MEASURE'] in keep and r['OBS_VALUE']!='':
        if r['MEASURE']=='NW' and r['STATISTICAL_OPERATION']!='MEDIAN': pass
        rows.append([r['REF_AREA'],r['Reference area'],r['TIME_PERIOD'],r['MEASURE'],r['Measure'],r['STATISTICAL_OPERATION'],r['Unit of measure'],r['CURRENCY'],round(float(r['OBS_VALUE']),3),r['OBS_STATUS'],r['Observation status']])
rows.sort()
w=csv.writer(open(out,'w',newline=''))
w.writerow(['iso3','country','year','measure_code','measure','statistic','unit','currency','value','obs_status','obs_status_label'])
w.writerows(rows)
