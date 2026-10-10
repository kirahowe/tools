import csv
d={}
for x in csv.DictReader(open('raw/33100270/33100270.csv',encoding='utf-8-sig')):
    if x['GEO']=='Canada' and x['Industry']=='Business sector industries [T004]' and x['VALUE']:
        d.setdefault(x['REF_DATE'],{})[x['Business dynamics measure']]=float(x['VALUE'])
w=csv.writer(open('B2_monthly_business_entrants_exits_33100270.csv','w',newline=''))
w.writerow(['month','active','entrants','exits','opening','closing','entry_rate_pct_of_active','exit_rate_pct_of_active','source'])
for m in sorted(d):
    v=d[m]
    w.writerow([m,v.get('Active businesses'),v.get('Entrants'),v.get('Exits'),v.get('Opening businesses'),v.get('Closing businesses'),
      round(100*v['Entrants']/v['Active businesses'],3) if 'Entrants' in v else '',round(100*v['Exits']/v['Active businesses'],3) if 'Exits' in v else '',
      'StatCan 33-10-0270-01 (experimental, seasonally adjusted), business sector industries [T004]'])
# annual avg
ann={}
for m,v in d.items():
    y=m[:4]
    if 'Entrants' in v and 'Exits' in v: ann.setdefault(y,[]).append((v['Entrants']/v['Active businesses'],v['Exits']/v['Active businesses']))
for y in sorted(ann): print(y,len(ann[y]),round(100*sum(a for a,b in ann[y])/len(ann[y]),2),round(100*sum(b for a,b in ann[y])/len(ann[y]),2))
