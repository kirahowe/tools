import csv,sys
keep={'Threshold value','Number of tax filers','Average income','Share of income','Average federal and provincial or territorial income taxes paid','Share of federal and provincial or territorial income taxes paid','Median income'}
out=[]
with open(sys.argv[1],encoding='utf-8-sig') as f:
    for r in csv.DictReader(f):
        if r['GEO']=='Canada' and r['REF_DATE'] in('2019','2020','2021','2022','2023') and r['Statistics'] in keep and r['Income concepts'] in('Total income','Total income with capital gains','Market income with capital gains','Market income'):
            out.append({k:r[k] for k in('REF_DATE','Income concepts','Income groups','Statistics','UOM','VALUE','STATUS','VECTOR')})
with open(sys.argv[2],'w',newline='') as f:
    w=csv.DictWriter(f,fieldnames=list(out[0])); w.writeheader(); w.writerows(out)
print(len(out))
