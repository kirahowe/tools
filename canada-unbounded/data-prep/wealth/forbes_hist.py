import json,sys,csv,datetime,glob,os
out=sys.argv[1]; files=sorted(sys.argv[2:])
w=csv.writer(open(out,'w',newline=''))
w.writerow(['list_year','list_date','total_entries_world','canadian_citizens_count','canadian_citizens_networth_usd_bn','canada_residents_count','canada_residents_networth_usd_bn','citizens_selfmade_false_count','citizens_selfmade_false_networth_usd_bn','citizens_country_field_missing_count','source_url'])
for f in files:
    y=os.path.basename(f).split('_')[-1].split('.')[0]
    d=json.load(open(f))
    L=d.get('personList',{}).get('personsLists',[])
    if not L: print(y,'empty'); continue
    dates=sorted(set(datetime.datetime.fromtimestamp(p['date']/1000,datetime.UTC).date().isoformat() for p in L if p.get('date')))
    hascit=any('countryOfCitizenship' in p for p in L)
    ca=[p for p in L if p.get('countryOfCitizenship')=='Canada']
    res=[p for p in L if p.get('country')=='Canada']
    inh=[p for p in ca if p.get('selfMade') is False]
    w.writerow([y,';'.join(dates[:3]),len(L),len(ca),round(sum(p['finalWorth'] for p in ca)/1000,1),len(res),round(sum(p['finalWorth'] for p in res)/1000,1),len(inh),round(sum(p['finalWorth'] for p in inh)/1000,1),sum(1 for p in ca if not p.get('country')),f'https://www.forbes.com/forbesapi/person/billionaires/{y}/position/true.json'])
    print(y,len(L),dates[:2],hascit,len(ca),len(res))
