import json,sys,csv,datetime
f,year,out=sys.argv[1],sys.argv[2],sys.argv[3]
d=json.load(open(f))
L=d['personList']['personsLists']
print('entries',len(L))
dates=set(datetime.datetime.utcfromtimestamp(p['date']/1000).date() for p in L if p.get('date'))
print('list dates',sorted(dates)[:5])
ca=[p for p in L if p.get('countryOfCitizenship')=='Canada']
res=[p for p in L if p.get('country')=='Canada']
print('citizens',len(ca),'sum $bn',sum(p['finalWorth'] for p in ca)/1000)
print('residents',len(res),'sum $bn',sum(p['finalWorth'] for p in res)/1000)
print('citizens not resident:',[(p['personName'],p.get('country')) for p in ca if p.get('country')!='Canada'])
print('residents not citizens:',[(p['personName'],p.get('countryOfCitizenship')) for p in res if p.get('countryOfCitizenship')!='Canada'])
w=csv.writer(open(out,'w',newline=''))
w.writerow(['list_year','list_date','world_rank','canada_rank_citizens','name','net_worth_usd_billions','source_of_wealth','industries','self_made_forbes_flag','country_of_citizenship','country_of_residence','city','forbes_uri'])
for i,p in enumerate(sorted(ca,key=lambda p:-p['finalWorth'])):
    dt=datetime.datetime.utcfromtimestamp(p['date']/1000).date() if p.get('date') else ''
    w.writerow([year,dt,p.get('rank'),i+1,p['personName'],p['finalWorth']/1000,p.get('source'),';'.join(p.get('industries') or []),p.get('selfMade'),p.get('countryOfCitizenship'),p.get('country'),p.get('city'),p.get('uri')])
