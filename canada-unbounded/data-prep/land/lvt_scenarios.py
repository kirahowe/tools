import csv,sys
long,out=sys.argv[1:3]
d={}
for r in csv.DictReader(open(long)):
    if r['quarter']=='2025Q4': d[(r['sector'],r['category'])]=float(r['value_cad_millions'])
LD_nat=d[('national','Land underlying dwellings')]; LD_hh=d[('households','Land underlying dwellings')]; L_nat=d[('national','Land')]
GDP=3245257  # avg of 4 quarterly SAAR 2025, 36-10-0104-01 (computed in land_share_summary.csv)
OCC=14978941; TOT=16284235  # 2021 Census private dwellings occupied by usual residents / total private dwellings
rows=[]
def add(sc,base_desc,base,rate,exempt_desc,revenue,note):
    rows.append([sc,base_desc,round(base),rate,exempt_desc,round(revenue),round(100*revenue/GDP,2),note])
add('A','Land underlying dwellings, all sectors, 2025Q4',LD_nat,1.0,'none',0.01*LD_nat,'Static; no capitalization or behavioural response')
add('B','Land underlying dwellings, households sector, 2025Q4',LD_hh,1.0,'none',0.01*LD_hh,'Static')
for E in (50000,100000,200000):
    red=OCC*E/1e6
    add('C_'+str(E//1000)+'k','Land underlying dwellings, all sectors, 2025Q4',LD_nat,1.0,f'${E:,} land-value exemption per occupied dwelling (14,978,941 dwellings)',0.01*max(LD_nat-red,0),'LOWER BOUND: assumes every dwelling has land value >= exemption; true revenue lies between this and scenario A')
for r in (0.03,0.04,0.05):
    f=r/(r+0.01)
    add('D_r'+str(int(r*100)),'Land underlying dwellings, all sectors, after capitalization',LD_nat*f,1.0,'none',0.01*LD_nat*f,f'Perpetuity capitalization V\'=V*r/(r+t) with discount rate r={r:.0%}; land values fall {100*(1-f):.0f}%')
add('E','All land (dwellings + other buildings/structures + agricultural), all sectors, 2025Q4',L_nat,1.0,'none',0.01*L_nat,'Static; includes commercial/industrial and farm land')
w=csv.writer(open(out,'w',newline=''))
w.writerow(['scenario','base_description','base_cad_millions','tax_rate_pct','exemption','revenue_cad_millions_per_year','revenue_pct_of_2025_gdp','note'])
w.writerows(rows)
for r in rows: print(r)
