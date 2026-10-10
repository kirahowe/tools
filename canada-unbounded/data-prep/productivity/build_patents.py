import csv
pop={}
g={};pc={}
for r in csv.DictReader(open('raw/oecd/pdb_from_investment.csv')):
    if r['TRANSFORMATION']!='N' or r['ACTIVITY']!='_T' or r['PRICE_BASE']!='V' or not r['OBS_VALUE']: continue
    k=(r['REF_AREA'],int(r['TIME_PERIOD']))
    if r['MEASURE']=='GDP' and r['UNIT_MEASURE']=='USD_PPP': g[k]=float(r['OBS_VALUE'])*(10**int(r['UNIT_MULT'] or 0))
    if r['MEASURE']=='GDPPOP' and r['UNIT_MEASURE']=='USD_PPP_PS': pc[k]=float(r['OBS_VALUE'])
for k in g:
    if k in pc: pop[k]=g[k]/pc[k]
out=[]
for r in csv.DictReader(open('raw/oecd/msti_all.csv')):
    m=r['MEASURE: Measure'].split(':')[0]; u=r['UNIT_MEASURE: Unit of measure'].split(':')[0]
    if m in ('P_TRIAD','P_PCT') and r['OBS_VALUE']:
        a=r['REF_AREA: Reference area'].split(':')[0]; y=int(r['TIME_PERIOD: Time period']); v=float(r['OBS_VALUE'])
        if u=='PT_PATN_FM_T': out.append([y,a,'triadic_patent_families_pct_world',round(v,2)])
        elif u=='PATN':
            out.append([y,a,m+'_count',round(v,1)])
            if (a,y) in pop: out.append([y,a,m+'_per_million_pop',round(v/pop[(a,y)]*1e6,1)])
out.sort()
w=csv.writer(open('E2_patents_oecd_msti.csv','w',newline=''))
w.writerow(['year','area','measure','value','source'])
for o in out: w.writerow(o+['OECD MSTI DF_MSTI(1.3) (patents); population derived as GDP/GDP per capita from OECD PDB (current PPP)'])
