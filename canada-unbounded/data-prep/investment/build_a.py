import csv,collections,sys,os
R=sys.argv[1]; O=sys.argv[2]
def load(path,filt):
    out=collections.defaultdict(dict); first={}
    for row in csv.DictReader(open(path,encoding='utf-8-sig')):
        k=filt(row)
        if k and row['VALUE']!='':
            if k in first and first[k]!=row['VECTOR']: continue  # same label on liability side: keep asset-side (first) vector
            first.setdefault(k,row['VECTOR'])
            out[k][row['REF_DATE']]=(float(row['VALUE']),row['VECTOR'])
    return out
# expenditure GDP current prices SAAR
g=load(R+'/36100104/36100104.csv',lambda r: r['Estimates'] if r['Prices']=='Current prices' and r['Seasonal adjustment']=='Seasonally adjusted at annual rates' else None)
items=['Gross domestic product at market prices','Business gross fixed capital formation','Residential structures','Non-residential structures','Machinery and equipment','Intellectual property products']
vec={i:list(g[i].values())[0][1] for i in items}
print(vec)
ann=collections.defaultdict(dict)
for i in items:
    byy=collections.defaultdict(list)
    for d,(v,_) in g[i].items(): byy[int(d[:4])].append(v)
    for y,l in byy.items():
        if len(l)==4: ann[i][y]=sum(l)/4
years=sorted(ann['Gross domestic product at market prices'])
with open(O+'/A2_business_nonres_investment_share_gdp_annual.csv','w',newline='') as f:
    w=csv.writer(f); w.writerow(['year','gdp_cad_mn','nonres_structures_cad_mn','machinery_equipment_cad_mn','ipp_cad_mn','business_nonres_total_cad_mn','nonres_structures_pct_gdp','machinery_equipment_pct_gdp','ipp_pct_gdp','business_nonres_total_pct_gdp','source'])
    for y in years:
        G=ann['Gross domestic product at market prices'][y]; s=ann['Non-residential structures'][y]; m=ann['Machinery and equipment'][y]; p=ann['Intellectual property products'][y]; t=s+m+p
        w.writerow([y,round(G),round(s),round(m),round(p),round(t),round(100*s/G,2),round(100*m/G,2),round(100*p/G,2),round(100*t/G,2),'StatCan 36-10-0104-01 current prices SAAR, annual avg of 4 quarters; vectors '+';'.join(vec[i] for i in items if i!='Residential structures' and i!='Business gross fixed capital formation')])
# quarterly too
with open(O+'/A2_business_nonres_investment_share_gdp_quarterly.csv','w',newline='') as f:
    w=csv.writer(f); w.writerow(['quarter','business_nonres_total_pct_gdp','nonres_structures_pct_gdp','machinery_equipment_pct_gdp','ipp_pct_gdp'])
    for d in sorted(g['Gross domestic product at market prices']):
        G=g['Gross domestic product at market prices'][d][0]
        s,m,p=[g[i][d][0] for i in ['Non-residential structures','Machinery and equipment','Intellectual property products']]
        w.writerow([d,round(100*(s+m+p)/G,2),round(100*s/G,2),round(100*m/G,2),round(100*p/G,2)])
# balance sheet
b=load(R+'/36100580/36100580.csv',lambda r: (r['Sectors'],r['Valuation'],r['Categories']) if r['Sectors'] in('Non-financial private corporations','Non-financial corporations') and r['Categories'] in ('Total currency and deposits','Canadian currency and deposits','Foreign currency and deposits','Total financial assets','Total assets') else None)
for k in b: print(k, list(b[k].values())[0][1], min(b[k]), max(b[k]))
with open(O+'/A3_nfc_currency_deposits_pct_gdp.csv','w',newline='') as f:
    w=csv.writer(f); w.writerow(['year','sector','currency_deposits_q4_cad_mn','gdp_annual_cad_mn','currency_deposits_pct_gdp','total_financial_assets_q4_cad_mn','source'])
    for sec in ['Non-financial private corporations','Non-financial corporations']:
        k=(sec,'Book value','Total currency and deposits')
        if k not in b: k=(sec,'Market value','Total currency and deposits')
        kt=(k[0],k[1],'Total financial assets')
        for d in sorted(b[k]):
            if d.endswith('-10'):
                y=int(d[:4])
                if y in ann['Gross domestic product at market prices']:
                    G=ann['Gross domestic product at market prices'][y]; v=b[k][d][0]
                    w.writerow([y,sec,round(v),round(G),round(100*v/G,2),round(b[kt][d][0]) if d in b[kt] else '','StatCan 36-10-0580-01 ('+k[1]+', Q4 stock, vector '+b[k][d][1]+') / annual GDP 36-10-0104-01'])
# profits vs investment: 36-10-0103
inc=load(R+'/36100103/36100103.csv',lambda r: r['Estimates'] if r['Seasonal adjustment']=='Seasonally adjusted at annual rates' else None)
iv={i:list(inc[i].values())[0][1] for i in inc}; print(iv)
def annual(dd):
    byy=collections.defaultdict(list)
    for d,(v,_) in dd.items(): byy[int(d[:4])].append(v)
    return {y:sum(l)/4 for y,l in byy.items() if len(l)==4}
nos=annual(inc['Net operating surplus: corporations']); cfc=annual(inc['Consumption of fixed capital: corporations'])
with open(O+'/A4_corporate_surplus_vs_business_investment.csv','w',newline='') as f:
    w=csv.writer(f); w.writerow(['year','corp_net_operating_surplus_pct_gdp','corp_gross_operating_surplus_pct_gdp','business_nonres_investment_pct_gdp','nonres_investment_to_corp_gross_operating_surplus_ratio','source'])
    for y in years:
        if y in nos:
            G=ann['Gross domestic product at market prices'][y]; t=sum(ann[i][y] for i in ['Non-residential structures','Machinery and equipment','Intellectual property products'])
            w.writerow([y,round(100*nos[y]/G,2),round(100*(nos[y]+cfc[y])/G,2),round(100*t/G,2),round(t/(nos[y]+cfc[y]),3),'StatCan 36-10-0103-01 (NOS corporations '+iv['Net operating surplus: corporations']+', CFC corporations '+iv['Consumption of fixed capital: corporations']+'); 36-10-0104-01'])
