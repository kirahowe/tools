import csv,sys
src,out=sys.argv[1],sys.argv[2]
w=csv.writer(open(out,'w',newline=''))
w.writerow(['year_july1','geo','age_group','persons','vector','status'])
for r in csv.DictReader(open(src,encoding='utf-8-sig')):
    if r['GEO']=='Canada' and r['Gender']=='Total - gender' and r['Age group'] in ('All ages','18 years and older','0 to 17 years') and r['UOM']=='Persons' and int(r['REF_DATE'])>=1990:
        w.writerow([r['REF_DATE'],r['GEO'],r['Age group'],r['VALUE'],r['VECTOR'],r['STATUS']])
