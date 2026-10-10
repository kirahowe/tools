import csv,sys
src,out=sys.argv[1],sys.argv[2]
sectors=['National balance sheets','Households','Households and non-profit institutions serving households','General governments','Federal general government','Other levels of general government','Provincial and territorial general governments','Local general governments','Social security funds']
cats=['Total assets','Non-financial assets','Total financial assets','Total financial liabilities','Net financial assets','Net worth']
w=csv.writer(open(out,'w',newline=''))
w.writerow(['ref_date','quarter','sector','valuation','category','value_millions_cad','vector','status'])
q={'01':'Q1','04':'Q2','07':'Q3','10':'Q4'}
for r in csv.DictReader(open(src,encoding='utf-8-sig')):
    if r['Sectors'] in sectors and r['Categories'] in cats and r['Valuation']=='Market value' and r['VALUE']!='':
        y,m=r['REF_DATE'].split('-')
        w.writerow([r['REF_DATE'],y+q[m],r['Sectors'],r['Valuation'],r['Categories'],r['VALUE'],r['VECTOR'],r['STATUS']])
