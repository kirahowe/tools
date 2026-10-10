import csv,math,openpyxl
# US BLS
wb=openpyxl.load_workbook('raw/bls/total-factor-productivity-major-sectors.xlsx',read_only=True)
mapus={'Labor productivity':'lp','Total factor productivity':'mfp','Contribution of capital intensity to labor productivity':'cap_int','Contribution of labor composition to labor productivity':'lab_comp'}
us={}
for r in wb['MachineReadable'].iter_rows(values_only=True,min_row=2):
    if r[0]=='PG' and r[4]=='Index (2017=100)' and r[3] in mapus and isinstance(r[6],(int,float)): us[(mapus[r[3]],int(r[5]))]=r[6]
ca={}
K='Multifactor productivity and related variables'; N='North American Industry Classification System (NAICS)'
mapca={'Labour productivity':'lp','Multifactor productivity':'mfp','Contribution of capital intensity to labour productivity growth':'cap_int','Contribution of labour composition to labour productivity growth':'lab_comp'}
for x in csv.DictReader(open('raw/36100208/36100208.csv',encoding='utf-8-sig')):
    if x[N]=='Business sector' and x[K] in mapca and x['VALUE']: ca[(mapca[x[K]],int(x['REF_DATE']))]=float(x['VALUE'])
print('US years',min(y for m,y in us),max(y for m,y in us))
periods=[(1987,2000),(2000,2008),(2008,2019),(2019,2023),(2000,2023)]
with open('A3_canada_us_lp_decomposition_by_period.csv','w',newline='') as f:
    w=csv.writer(f); w.writerow(['country','sector','start','end','component','pct_per_year','source'])
    for c,d,sec,src in (('Canada',ca,'Business sector','StatCan 36-10-0208-01'),('United States',us,'Private business sector','BLS Total factor productivity for major sectors, released 2026-03-19 (sector PG)')):
        for a,b in periods:
            for m,lab in (('lp','Labour productivity growth'),('cap_int','Capital intensity (capital deepening) contribution'),('lab_comp','Labour composition contribution'),('mfp','Multifactor productivity growth')):
                g=100*(math.log(d[(m,b)])-math.log(d[(m,a)]))/(b-a)
                w.writerow([c,sec,a,b,lab,round(g,2),src+'; avg annual log growth x100'])
                print(c,a,b,lab,round(g,2))
# annual US+CA index rebased 2000=100 for LP and MFP
with open('A4_canada_us_lp_mfp_index_2000eq100.csv','w',newline='') as f:
    w=csv.writer(f); w.writerow(['year','country','measure','index_2000_100','source'])
    for c,d,src in (('Canada',ca,'StatCan 36-10-0208-01 business sector'),('United States',us,'BLS TFP major sectors, private business (PG)')):
        for m in ('lp','mfp'):
            for y in range(1987,2026):
                if (m,y) in d: w.writerow([y,c,{'lp':'labour_productivity','mfp':'multifactor_productivity'}[m],round(100*d[(m,y)]/d[(m,2000)],2),src])
