import csv,sys,collections
src,out=sys.argv[1],sys.argv[2]
groups={'All households':'All households','Lowest wealth quintile':'Q1 (bottom 20%)','Second wealth quintile':'Q2','Third wealth quintile':'Q3','Fourth wealth quintile':'Q4','Highest wealth quintile':'Q5 (top 20%)'}
d=collections.defaultdict(dict)
for r in csv.DictReader(open(src,encoding='utf-8-sig')):
    if r['Wealth']!='Net worth (wealth)' or r['Characteristics'] not in groups: continue
    d[r['REF_DATE']].setdefault(groups[r['Characteristics']],{})[r['Statistics']]=r['VALUE']
qmap={'01':'Q1','04':'Q2','07':'Q3','10':'Q4'}
w=csv.writer(open(out,'w',newline=''))
w.writerow(['quarter','ref_date','wealth_group','derived','net_worth_total_millions_cad','share_published_pct','share_computed_pct','net_worth_per_household_cad'])
for ref in sorted(d):
    g=d[ref]
    if not g['All households'].get('Value'): continue
    y,m=ref.split('-'); q=f'{y}{qmap[m]}'
    tot=float(g['All households']['Value'])
    for name in groups.values():
        x=g[name]; v=float(x['Value'])
        w.writerow([q,ref,name,0,x['Value'],x['Distribution of value'],round(100*v/tot,3),x['Value per household']])
    for name,parts in [('Bottom 40% (Q1+Q2)',['Q1 (bottom 20%)','Q2']),('Middle 40% (Q3+Q4)',['Q3','Q4']),('Top 40% (Q4+Q5)',['Q4','Q5 (top 20%)'])]:
        v=sum(float(g[p]['Value']) for p in parts)
        w.writerow([q,ref,name,1,int(v),'',round(100*v/tot,3),''])
