import sys, csv, openpyxl, os
root=sys.argv[1]
def read(p):
    wb=openpyxl.load_workbook(p,data_only=True,read_only=True); d={}
    for r in wb.worksheets[0].iter_rows(values_only=True):
        if r[1] and isinstance(r[2],(int,float)): d[r[1]]=(r[0],r[2])
    return d
g=read(os.path.join(root,'raw','oecd_fig1_5.xlsx')); e=read(os.path.join(root,'raw','oecd_fig4_8.xlsx'))
codes=sorted(set(g)|set(e), key=lambda c:(e.get(c,(0,999))[1]))
with open(os.path.join(root,'oecd_broken_elevator_2018.csv'),'w',newline='') as f:
    w=csv.writer(f); w.writerow(['country','iso3','generations_bottom10_to_mean','father_son_earnings_elasticity_pct','source_generations','source_elasticity','note'])
    for c in codes:
        name=(g.get(c) or e.get(c))[0]
        w.writerow([name,c,g.get(c,(None,''))[1],round(e[c][1],1) if c in e else '', 'OECD (2018) A Broken Social Elevator?, Fig 1.5, StatLink http://dx.doi.org/10.1787/888933761910','OECD (2018) Fig 4.8, StatLink http://dx.doi.org/10.1787/888933754253','Generations are illustrative simulations (Bowles-Gintis). Elasticities mix methods: Canada = Chen, Ostrovsky & Piraino (2017) admin data; US = OECD PSID estimate; many others two-sample 2SLS'])
print(len(codes))
