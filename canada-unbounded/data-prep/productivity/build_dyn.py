import csv
M='Business dynamics measure';N='North American Industry Classification System (NAICS)';S='Firm size'
rows=[]
keep={'Entry rate','Exit rate','Number of active employer businesses in the private sector','Number of entrants','Number of exits','Gross employment creation by entrants','Gross employment creation','Net employment growth','Employment share'}
for x in csv.DictReader(open('raw/33100164/33100164.csv',encoding='utf-8-sig')):
    if x[M] in keep and x['VALUE']!='':
        rows.append([x['REF_DATE'],x[M],x[N],x[S],x['UOM'],x['VALUE'],x['STATUS'],x['VECTOR'],'StatCan 33-10-0164-01'])
w=csv.writer(open('B1_business_dynamics_entry_exit_33100164.csv','w',newline=''))
w.writerow(['year','measure','industry','firm_size','unit','value','status','vector','source'])
w.writerows(rows)
