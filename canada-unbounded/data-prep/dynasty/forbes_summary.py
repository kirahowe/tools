import csv,sys,collections
rows=list(csv.DictReader(open(sys.argv[1])))
out=[]
for scope,filt in [('citizens',lambda r:True),('citizens_resident_in_canada',lambda r:r['country_of_residence']=='Canada'),('citizens_excl_changpeng_zhao',lambda r:r['name']!='Changpeng Zhao')]:
    for flag in ['True','False']:
        sub=[r for r in rows if filt(r) and r['self_made_forbes_flag']==flag]
        allsub=[r for r in rows if filt(r)]
        n=len(sub); w=sum(float(r['net_worth_usd_billions']) for r in sub); W=sum(float(r['net_worth_usd_billions']) for r in allsub)
        out.append([2026,'2026-03-10',scope,'self_made' if flag=='True' else 'inherited (Forbes selfMade=false)',n,len(allsub),round(w,1),round(W,1),round(100*w/W,1)])
w=csv.writer(sys.stdout)
w.writerow(['list_year','list_date','scope','forbes_category','n_billionaires','n_total','net_worth_usd_bn','total_net_worth_usd_bn','share_of_net_worth_pct'])
w.writerows(out)
print()
for r in rows:
    if r['self_made_forbes_flag']=='False': print(r['name'],r['net_worth_usd_billions'],r['source_of_wealth'],r['country_of_residence'],sep=' | ')
