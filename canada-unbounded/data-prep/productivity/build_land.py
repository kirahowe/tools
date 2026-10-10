import csv,collections
cats=['Non-financial assets','Land','Land underlying dwellings','Land underlying buildings and structures','Agricultural land','Dwellings','Machinery and equipment','Intellectual property products','Buildings other than dwellings','Other structures','Fixed assets','Residential mortgages','Non-residential mortgages','Non-mortgage loans','Total all sectors']
d=collections.defaultdict(dict); vec={}
for r in csv.DictReader(open('raw/36100580/36100580.csv',encoding='utf-8-sig')):
    if r['Sectors']=='National balance sheets' and r['Categories'] in cats and r['VALUE'] and r['REF_DATE'].endswith('-10'):
        k=(r['Valuation'],r['Categories'])
        if k in vec and vec[k]!=r['VECTOR']: continue
        vec[k]=r['VECTOR']; d[k][int(r['REF_DATE'][:4])]=float(r['VALUE'])
print(sorted(vec))
val='Market value' if ('Market value','Land') in vec else 'Book value'
NFA=d[(val,'Non-financial assets')]
with open('D2_land_share_of_national_nonfinancial_assets.csv','w',newline='') as f:
    w=csv.writer(f); w.writerow(['year','nonfinancial_assets_cad_mn','land_cad_mn','land_under_dwellings_cad_mn','dwellings_cad_mn','machinery_equipment_cad_mn','ipp_cad_mn','land_pct_nfa','land_under_dwellings_pct_nfa','dwellings_plus_land_under_pct_nfa','machinery_equipment_pct_nfa','ipp_pct_nfa','source'])
    for y in sorted(NFA):
        g=lambda c: d[(val,c)].get(y)
        L,LD,D,M,I=g('Land'),g('Land underlying dwellings'),g('Dwellings'),g('Machinery and equipment'),g('Intellectual property products')
        if None in (L,LD,D,M,I): continue
        n=NFA[y]
        w.writerow([y,n,L,LD,D,M,I,round(100*L/n,2),round(100*LD/n,2),round(100*(D+LD)/n,2),round(100*M/n,2),round(100*I/n,2),
          'StatCan 36-10-0580-01, National balance sheets, '+val+', Q4 stock; vectors '+';'.join(vec[(val,c)] for c in ['Non-financial assets','Land','Land underlying dwellings','Dwellings','Machinery and equipment','Intellectual property products'])])
