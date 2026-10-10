import csv,collections
def q(path,vecs):
    out=collections.defaultdict(dict)
    for r in csv.DictReader(open(path,encoding='utf-8-sig')):
        if r['VECTOR'] in vecs and r['VALUE']: out[r['VECTOR']][r['REF_DATE']]=float(r['VALUE'])
    return out
gdp=q('raw/36100104/36100104.csv',{'v62305783'})['v62305783']
V={'v62144022':'residential_total','v62144023':'residential_new_construction','v62144024':'residential_renovations','v62144025':'residential_ownership_transfer_costs',
   'v62144026':'business_nonres_structures','v62144029':'business_machinery_equipment','v62144039':'business_ipp','v62144041':'business_rd'}
d=q('raw/36100108/36100108.csv',set(V))
def ann(s):
    b=collections.defaultdict(list)
    for k,v in s.items(): b[int(k[:4])].append(v)
    return {y:sum(l)/4 for y,l in b.items() if len(l)==4}
G=ann(gdp); A={V[k]:ann(s) for k,s in d.items()}
with open('D1_residential_vs_business_investment_pct_gdp_annual.csv','w',newline='') as f:
    w=csv.writer(f)
    cols=['residential_total','residential_new_construction','residential_renovations','residential_ownership_transfer_costs','business_nonres_structures','business_machinery_equipment','business_ipp','business_rd']
    w.writerow(['year','gdp_cad_mn']+[c+'_pct_gdp' for c in cols]+['business_nonres_total_pct_gdp','residential_to_business_nonres_ratio','source'])
    for y in sorted(G):
        if all(y in A[c] for c in cols):
            nr=A['business_nonres_structures'][y]+A['business_machinery_equipment'][y]+A['business_ipp'][y]
            w.writerow([y,round(G[y])]+[round(100*A[c][y]/G[y],2) for c in cols]+[round(100*nr/G[y],2),round(A['residential_total'][y]/nr,3),
              'StatCan 36-10-0108-01 (business GFCF by asset, current prices SAAR; '+';'.join(V)+') / GDP 36-10-0104-01 v62305783; annual avg of quarters'])
# quarterly simple
with open('D1_residential_vs_business_investment_pct_gdp_quarterly.csv','w',newline='') as f:
    w=csv.writer(f); w.writerow(['quarter','residential_pct_gdp','business_nonres_pct_gdp','ownership_transfer_costs_pct_gdp'])
    for k in sorted(gdp):
        try:
            nr=d['v62144026'][k]+d['v62144029'][k]+d['v62144039'][k]
            w.writerow([k,round(100*d['v62144022'][k]/gdp[k],2),round(100*nr/gdp[k],2),round(100*d['v62144025'][k]/gdp[k],2)])
        except KeyError: pass
