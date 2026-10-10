import sys,csv
sys.path.insert(0,sys.argv[0].rsplit('/',1)[0])
from sc import find
R='raw/'
out=[]
for fam in ['Economic families','Couples with children','Persons not in an economic family','Economic families and persons not in an economic family']:
  for con in ['Median total income','Median after-tax income','Median market income']:
    for r in find(R+'11100190/11100190.csv',{'GEO':'Canada','Income concept':con,'Economic family type':fam}):
        out.append([r['REF_DATE'],'11-10-0190-01',fam,con,r['UOM'],r['VALUE'],r['VECTOR']])
for st in ['Median income (excluding zeros)','Average income (excluding zeros)']:
  for src in ['Total income','Employment income']:
    for r in find(R+'11100239/11100239.csv',{'GEO':'Canada','Age group':'15 years and over','Gender':'Total - Gender','Income source':src,'Statistics':st}):
        out.append([r['REF_DATE'],'11-10-0239-01','Individuals 15+',src+' - '+st,r['UOM'],r['VALUE'],r['VECTOR']])
w=csv.writer(open('income_canada_cis_annual.csv','w',newline=''))
w.writerow(['year','statcan_table','population','measure','unit','value','vector']); w.writerows(sorted(out,key=lambda x:(x[1],x[2],x[3],x[0])))
for o in out:
    if o[0] in ('1976','1981','1990','2000','2019','2023','2024') and 'Median' in o[3] and ('market' not in o[3]): print(o)
