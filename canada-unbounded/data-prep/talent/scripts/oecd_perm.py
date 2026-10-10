import csv,json,sys,collections
perm,wbp,out=sys.argv[1:4]
iso3={}
d=json.load(open(wbp))[1]
pop={(x['countryiso3code'],x['date']):x['value'] for x in d if x['value']}
tot=collections.defaultdict(float); work=collections.defaultdict(float); stat={}
names={}
for r in csv.DictReader(open(perm,encoding='utf-8-sig')):
    if not r['OBS_VALUE']: continue
    k=(r['REF_AREA'],r['TIME_PERIOD']); v=float(r['OBS_VALUE']); names[r['REF_AREA']]=r['Reference area']
    tot[k]+=v
    if r['MIGRATION_TYPE'] in ('WO','AC'): work[k]+=v
    if r['OBS_STATUS']!='A': stat[k]=r['OBS_STATUS']
w=csv.writer(open(out,'w',newline=''))
w.writerow(['country_iso3','country','year','permanent_inflows_standardised','work_plus_accompanying','population_worldbank','permanent_inflows_per_1000','work_share_pct','obs_status_flag','source'])
for (c,y),v in sorted(tot.items()):
    p=pop.get((c,y))
    w.writerow([c,names[c],y,int(v),int(work[(c,y)]),int(p) if p else '',round(v/p*1000,2) if p else '',round(work[(c,y)]/v*100,1) if v else '',stat.get((c,y),''),'OECD DF_MIG_INT_PER (standardised permanent-type inflows) via sdmx.oecd.org; population World Bank SP.POP.TOTL; per-1000 is calc'])
