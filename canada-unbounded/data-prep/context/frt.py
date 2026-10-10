import csv,sys
m=[ [c.strip().replace(',','') for c in l.split('|')] for l in open(sys.argv[1]) if l.strip()]
p=[ [c.strip() for c in l.split('|')] for l in open(sys.argv[2]) if l.strip()]
w=csv.writer(open(sys.argv[3],'w',newline=''))
w.writerow(['fiscal_year','revenues_mn','program_expenses_mn','public_debt_charges_mn','budgetary_balance_mn','accumulated_deficit_mn','revenues_pct_gdp','program_expenses_pct_gdp','public_debt_charges_pct_gdp','budgetary_balance_pct_gdp','accumulated_deficit_pct_gdp','debt_charges_pct_revenue_calc','implied_nominal_gdp_mn_calc'])
for a,b in zip(m,p):
    assert a[0]==b[0]
    rev=float(a[1]);pdc=float(a[3]);ad=float(a[5])
    gdp=round(ad/float(b[5])*100) 
    w.writerow(a+b[1:]+[round(pdc/rev*100,1),''])
