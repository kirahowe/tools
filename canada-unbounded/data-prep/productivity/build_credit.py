import csv,collections
V={'v1231415622':'hh_res_mortgages_chartered_banks','v1231415634':'hh_heloc_chartered_banks','v1231415621':'hh_res_mortgages_all_lenders','v1231415626':'hh_real_estate_secured_all_lenders',
   'v1231415689':'pnfc_nonmortgage_loans_chartered_banks','v1231415694':'pnfc_res_mortgages_chartered_banks','v1231415704':'pnfc_nonres_mortgages_chartered_banks',
   'v1231415688':'pnfc_nonmortgage_loans_all_lenders','v1231415692':'pnfc_mortgage_loans_all_lenders','v1231415697':'pnfc_debt_securities','v1231415618':'hh_unincorporated_business_loans_chartered_banks'}
d=collections.defaultdict(dict)
for p in ('36100639','36100640'):
    for r in csv.DictReader(open(f'raw/{p}/{p}.csv',encoding='utf-8-sig')):
        if r['VECTOR'] in V and r['VALUE']: d[r['REF_DATE']][V[r['VECTOR']]]=float(r['VALUE'])
gdp=collections.defaultdict(list)
for r in csv.DictReader(open('raw/36100104/36100104.csv',encoding='utf-8-sig')):
    if r['VECTOR']=='v62305783' and r['VALUE']: gdp[int(r['REF_DATE'][:4])].append(float(r['VALUE']))
G={y:sum(l)/4 for y,l in gdp.items() if len(l)==4}
with open('D3_credit_housing_vs_business_annual_dec.csv','w',newline='') as f:
    w=csv.writer(f)
    w.writerow(['year','month','bank_household_mortgage_plus_heloc_cad_mn','bank_business_loans_pnfc_cad_mn','bank_household_mortgage_plus_heloc_share_pct','all_lender_household_real_estate_secured_cad_mn','all_lender_pnfc_loans_cad_mn','all_lender_household_RES_share_pct','household_RES_pct_gdp','pnfc_loans_pct_gdp','source'])
    for m in sorted(d):
        if not m.endswith('-12') and m!=max(d): continue
        v=d[m]; y=int(m[:4])
        bh=v['hh_res_mortgages_chartered_banks']+v['hh_heloc_chartered_banks']
        bb=v['pnfc_nonmortgage_loans_chartered_banks']+v['pnfc_res_mortgages_chartered_banks']+v['pnfc_nonres_mortgages_chartered_banks']
        ah=v['hh_real_estate_secured_all_lenders']; ab=v['pnfc_nonmortgage_loans_all_lenders']+v['pnfc_mortgage_loans_all_lenders']
        w.writerow([y,m,round(bh),round(bb),round(100*bh/(bh+bb),1),round(ah),round(ab),round(100*ah/(ah+ab),1),
            round(100*ah/G[y],1) if y in G else '',round(100*ab/G[y],1) if y in G else '',
            'StatCan 36-10-0639-01 & 36-10-0640-01 (seasonally adjusted, month-end stock; vectors '+';'.join(V)+'); GDP 36-10-0104-01'])
