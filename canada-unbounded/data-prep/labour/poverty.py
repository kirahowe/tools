import sys,csv
sys.path.insert(0,sys.argv[0].rsplit('/',1)[0])
from sc import find
R='raw/'
out=[]
for line in ['Market basket measure, 2018 base','Market basket measure, 2023 base','Low income measure after tax']:
  for p in ['All persons','Persons under 18 years','Persons 18 to 64 years','Persons 65 years and over']:
    for r in find(R+'11100135/11100135.csv',{'GEO':'Canada','Persons in low income':p,'Low income lines':line}):
      if r['Statistics'] in ('Percentage of persons in low income','Number of persons in low income') and r['VALUE']:
        out.append([r['REF_DATE'],line,p,r['Statistics'],r['VALUE'],r['VECTOR']])
w=csv.writer(open('poverty_rate_canada_cis.csv','w',newline=''));w.writerow(['year','low_income_line','population','statistic','value','vector']);w.writerows(sorted(out,key=lambda x:(x[1],x[2],x[3],x[0])))
for o in out:
  if o[2]=='All persons' and int(o[0])>=2015 and o[3].startswith('Perc'): print(o)
cities=['Toronto, Ontario','Vancouver, British Columbia','Montréal, Québec','Calgary, Alberta','Halifax, Nova Scotia','Ottawa-Gatineau, Ontario part, Ontario/Quebec','Winnipeg, Manitoba','Edmonton, Alberta']
out=[]
for c in cities:
  for r in find(R+'11100066/11100066.csv',{'GEO':c,'Dollar concept':'Current dollars'}):
    if r['VALUE'] and int(r['REF_DATE'])>=2015 and r['Base year'] in ('2018 base','2023 base'):
      out.append([r['REF_DATE'],c,r['Base year'],r['Component'],r['VALUE'],r['VECTOR']])
w=csv.writer(open('mbm_thresholds_cities_current_dollars.csv','w',newline=''));w.writerow(['year','mbm_region','base','component','value_current_dollars_reference_family_of_4','vector']);w.writerows(sorted(out))
for o in sorted(out):
  if o[0] in('2024','2025') and o[3] in('Total threshold','Shelter'): print(o)
