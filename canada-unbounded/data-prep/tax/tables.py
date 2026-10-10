import csv,sys,collections
D=sys.argv[1]
AFR='https://www.canada.ca/en/department-finance/services/publications/annual-financial-report/2025.html'
FM='https://www.canada.ca/en/department-finance/services/publications/fiscal-monitor/2026/03.html'
fed=[('Personal income tax',234319,217696,226769),('Corporate income tax',96954,82468,101543),('Non-resident income tax',13528,12541,14684),
('Goods and Services Tax',52503,51416,50841),('Energy taxes',5650,5599,5539),('Customs import duties',6264,5571,10240),('Other excise taxes and duties',7487,6829,7310),
('Employment Insurance premiums',31530,29560,32606),('Pollution pricing proceeds',13552,10503,-196),('Other revenues',49164,37366,50681)]
tot=(510951,459549,500017)
assert abs(sum(r[1] for r in fed)-tot[0])<=3 and abs(sum(r[2] for r in fed)-tot[1])<=3 and abs(sum(r[3] for r in fed)-tot[2])<=3, [sum(r[i] for r in fed) for i in (1,2,3)]
with open(D+'/federal_revenue_by_source.csv','w',newline='') as f:
    w=csv.writer(f); w.writerow(['source','fy2023_24_final_millions','fy2024_25_final_millions','fy2024_25_share_pct','fy2025_26_prelim_millions','fy2025_26_share_pct','citation'])
    for s,a,b,c in fed: w.writerow([s,b,a,round(100*a/tot[0],2),c,round(100*c/tot[2],2),'AFR 2024-25 Table 4 ('+AFR+'); FY2025-26 = Fiscal Monitor March 2026 Table 2, April-March, preliminary ('+FM+')'])
    w.writerow(['Total revenues',tot[1],tot[0],100.0,tot[2],100.0,''])
# income-tax ETR by top income group from 11-10-0055
d=collections.defaultdict(dict)
for r in csv.DictReader(open(D+'/high_income_filers_11100055_extract.csv')):
    d[(r['REF_DATE'],r['Income concepts'],r['Income groups'])][r['Statistics']]=r['VALUE']
G=['All tax filers','Bottom 50 percent income group','Top 10 percent income group','Top 5 percent income group','Top 1 percent income group','Top 0.1 percent income group','Top 0.01 percent income group']
with open(D+'/top_income_groups_tax_share_2023.csv','w',newline='') as f:
    w=csv.writer(f); w.writerow(['year','income_concept','income_group','threshold_dollars','n_filers','avg_income','share_of_income_pct','avg_fed_prov_income_tax','share_of_income_tax_pct','avg_tax_div_avg_income_pct'])
    for y in ('2021','2022','2023'):
        for c in ('Total income','Total income with capital gains'):
            for g in G:
                x=d[(y,c,g)]; ai=float(x['Average income']); at=float(x['Average federal and provincial or territorial income taxes paid'])
                w.writerow([y,c,g,x.get('Threshold value',''),x['Number of tax filers'],x['Average income'],x['Share of income'],x['Average federal and provincial or territorial income taxes paid'],x['Share of federal and provincial or territorial income taxes paid'],round(100*at/ai,1)])
TE='https://www.canada.ca/content/dam/fin/publications/taxexp-depfisc/2026/taxexp-depfisc-26-eng.pdf'
te=[('Partial inclusion of capital gains','PIT individuals',[9970,19535,14045,13315,15780,16175,16780,17605]),
('Partial inclusion of capital gains','PIT trusts',[1545,2600,1535,1205,1400,1440,1495,1565]),
('Partial inclusion of capital gains','CIT',[11500,18450,18965,14965,24560,16550,18650,20810]),
('Partial inclusion of capital gains','Total',[23015,40580,34545,29485,41740,34165,36920,39980]),
('Non-taxation of capital gains on principal residences','PIT',[6930,12175,10595,7565,7460,8170,8545,9420]),
('Lifetime Capital Gains Exemption','PIT',[1725,2550,2625,2585,2700,2770,2880,3025]),
('Dividend gross-up and tax credit','PIT',[4660,5225,5820,6380,6710,6975,7205,7515]),
('Dividend gross-up and tax credit','TRU',[280,360,475,315,325,340,350,365]),
('Deduction of interest and carrying charges incurred to earn investment income','PIT',[1900,2280,2465,2835,3100,3170,3255,3370]),
('Tax-Free Savings Account','PIT',[1700,3220,2005,2665,3675,4050,4695,5505])]
with open(D+'/tax_expenditures_2026_report.csv','w',newline='') as f:
    w=csv.writer(f); w.writerow(['measure','tax','2020','2021','2022','2023','2024_P','2025_P','2026_P','2027_P','units','citation'])
    for m,t,v in te: w.writerow([m,t]+v+['$ millions, federal only',TE+' (Part 2 tables p.30-40; Part 3 descriptions, partial inclusion p.244, PRE p.223)'])
print('ok')
