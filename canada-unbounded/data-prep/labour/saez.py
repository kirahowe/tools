import xlrd,sys,csv
b=xlrd.open_workbook(sys.argv[1])
s=b.sheet_by_name('Table F1')
for r in range(86,s.nrows): print('F1 note:',s.cell_value(r,0))
w=csv.writer(open('top_marginal_tax_rates_canada_saez_veall_1920_2000.csv','w',newline=''))
w.writerow(['year','mtr_at_P90','mtr_at_P99','mtr_at_P99.9','mtr_at_P99.99','top_statutory_mtr','source'])
for r in range(5,86):
    v=[s.cell_value(r,c) for c in range(11)]
    w.writerow([int(v[0])]+[round(x,2) for x in (v[1],v[4],v[6],v[8],v[10])]+['Saez & Veall (2007) Table F1, TabCanVolume-web.xls'])
s=b.sheet_by_name('Table B1')
for r in range(86,s.nrows): print('B1 note:',s.cell_value(r,0))
w=csv.writer(open('top_income_shares_canada_saez_veall_1920_2000.csv','w',newline=''))
w.writerow(['year','top10_share_pct','top5_share_pct','top1_share_pct','top0.1_share_pct','top0.01_share_pct','source'])
for r in range(5,86):
    v=[s.cell_value(r,c) for c in range(7)]
    w.writerow([int(v[0])]+[round(x,3) if x!="" else "" for x in (v[1],v[2],v[3],v[5],v[6])]+['Saez & Veall (2007) Table B1 (excl. capital gains), TabCanVolume-web.xls'])
