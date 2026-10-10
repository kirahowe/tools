import csv,sys
rows=list(csv.reader(open(sys.argv[1],encoding='latin-1')))
hdr=rows[0]
classes=[h.split('(')[0].strip() for h in hdr[4::2]]
print(classes)
want={'Total number of returns','Employment income','Taxable amount of dividends from taxable Canadian corporations','Taxable capital gains','Total income assessed','Capital gains deduction','Net federal tax','Federal dividend tax credit','Taxable income assessed','Total tax payable','Net provincial or territorial tax','Provincial or territorial tax'}
items=[r[1] for r in rows[1:]]
print([i for i in items if 'tax' in i.lower()][:40])
data={}
for r in rows[1:]:
    if len(r)<6: continue
    n=[float(x) if x.strip() not in('','-') else 0 for x in r[4:4+2*len(classes)]]
    data[r[1]]=(n[0::2],n[1::2])
out=[]
for it in ['Total number of returns','Total income assessed','Employment income','Taxable amount of dividends from taxable Canadian corporations','Taxable capital gains','Capital gains deduction','Net federal tax']:
    if it not in data: print('missing',it); continue
    num,amt=data[it]
    tot=amt[0] if it!='Total number of returns' else num[0]
    for c,nn,a in zip(classes,num,amt):
        v=a if it!='Total number of returns' else nn
        out.append(dict(tax_year=2023,item=it,income_class=c,number=int(nn),amount_thousands=int(a),share_of_total_pct=round(100*v/tot,2) if tot else ''))
with open(sys.argv[2],'w',newline='') as f:
    w=csv.DictWriter(f,fieldnames=list(out[0])); w.writeheader(); w.writerows(out)
for o in out:
    if o['income_class'] in('Total','250000 and over','150000 - 249999','100000 - 149999') or o['income_class'].startswith('250'): print(o)
