import csv,sys,collections
raw=sys.argv[1]; outdir=sys.argv[2]
NW='Net worth (total assets less total debt)'
stats={'Average value for those holding asset or debt':'mean','Median value for those holding asset or debt':'median','Total values':'total','Percentage of total assets or total debts':'share_pct','Number holding asset or debt':'n_families','Percentage holding asset or debt':'pct_holding'}
ci={'Estimate':'','Lower bound of a 95% confidence interval':'_lo95','Upper bound of a 95% confidence interval':'_hi95'}
def run(pid,groupcol,out,geo_filter=None,extra=None):
    d=collections.OrderedDict(); scal={}
    for r in csv.DictReader(open(f'{raw}/{pid}/{pid}.csv',encoding='utf-8-sig')):
        if r['Assets and debts']!=NW: continue
        if geo_filter and r['GEO'] not in geo_filter: continue
        key=(r['REF_DATE'],r['GEO'])+tuple(r[c] for c in groupcol)
        d.setdefault(key,{})[stats[r['Statistics']]+ci[r['Confidence intervals']]]=r['VALUE']
        if r['Statistics']=='Total values': scal['total']=r['SCALAR_FACTOR']
        if r['Statistics']=='Number holding asset or debt': scal['n']=r['SCALAR_FACTOR']
    cols=['mean','median','total','share','n_families','mean_lo95','mean_hi95','median_lo95','median_hi95']
    tot={}
    for k,v in d.items():
        if k[2].startswith('Total') or (len(k)>3 and k[2]=='All ages' and k[3].startswith('Economic families and')): tot[(k[0],k[1])]=v.get('total')
    for k,v in d.items():
        t=tot.get((k[0],k[1]))
        v['share']=round(100*float(v['total'])/float(t),2) if (t and v.get('total') not in (None,'')) else ''
    w=csv.writer(open(out,'w',newline=''))
    w.writerow(['sfs_year','geo']+[c.lower().replace(' ','_') for c in groupcol]+['mean_net_worth_2023cad','median_net_worth_2023cad',f'total_net_worth_2023cad_{scal.get("total")}','share_of_total_net_worth_pct_computed',f'number_of_family_units_{scal.get("n")}','mean_lo95','mean_hi95','median_lo95','median_hi95'])
    for k,v in d.items(): w.writerow(list(k)+[v.get(c,'') for c in cols])
    print(out,len(d),scal)
run('11100075',['Net worth deciles'],f'{outdir}/sfs_networth_by_decile.csv')
run('11100049',['Net worth quintiles'],f'{outdir}/sfs_networth_by_quintile_canada_provinces.csv')
run('11100016',['Age group','Economic family type'],f'{outdir}/sfs_networth_by_age_familytype_geo.csv')
