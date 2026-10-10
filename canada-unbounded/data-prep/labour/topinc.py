import sys,csv
R='raw/'
C={'Total income','Total income with capital gains','Market income','After tax income'}
G={'Top 0.1 percent income group','Top 1 percent income group','Top 10 percent income group','Bottom 50 percent income group'}
S={'Share of income','Threshold value','Average income'}
out=[]
with open(R+'11100055/11100055.csv',encoding='utf-8-sig') as fh:
    for r in csv.DictReader(fh):
        if r['GEO']=='Canada' and r['Income concepts'] in C and r['Income groups'] in G and r['Statistics'] in S and r['VALUE']:
            out.append([r['REF_DATE'],r['Income concepts'],r['Income groups'],r['Statistics'],r['UOM'],r['VALUE'],r['VECTOR']])
w=csv.writer(open('top_income_shares_canada_t11100055.csv','w',newline=''));w.writerow(['year','income_concept','group','statistic','unit','value','vector']);w.writerows(sorted(out,key=lambda x:(x[1],x[2],x[3],x[0])))
for o in sorted(out):
  if o[1] in('Total income','Total income with capital gains') and o[3]!='Average income' and o[0] in('1982','1990','2000','2006','2015','2019','2021','2022','2023') and o[2] in('Top 1 percent income group','Top 10 percent income group'): print(o)
d={}
with open(R+'11100008/11100008.csv',encoding='utf-8-sig') as fh:
    for r in csv.DictReader(fh):
        if r['GEO']=='Canada' and r['Sex']=='Both sexes' and r['Age group']=='All age groups': d[(r['REF_DATE'],r['Persons with income'])]=(r['VALUE'],r['VECTOR'])
w=csv.writer(open('share_income_over_100k_t11100008.csv','w',newline=''));w.writerow(['year','persons_with_income','persons_100k_plus','pct_100k_plus','persons_150k_plus','pct_150k_plus','persons_200k_plus','pct_200k_plus','median_total_income_current_dollars'])
for y in sorted({k[0] for k in d}):
    a=float(d[(y,'All persons with income')][0]); b=float(d[(y,'Persons with income of $100,000 and over')][0]); c=float(d[(y,'Persons with income of $150,000 and over')][0]); e=float(d[(y,'Persons with income of $200,000 and over')][0])
    w.writerow([y,int(a),int(b),round(100*b/a,2),int(c),round(100*c/a,2),int(e),round(100*e/a,2),d[(y,'Median total income')][0]])
    print(y,int(a),int(b),round(100*b/a,2),round(100*e/a,2),d[(y,'Median total income')][0])
print('vec100k',d[('2023','Persons with income of $100,000 and over')][1],'all',d[('2023','All persons with income')][1])
