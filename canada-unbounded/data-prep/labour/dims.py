import csv,sys,collections
f=sys.argv[1]; maxv=int(sys.argv[2]) if len(sys.argv)>2 else 40
skip={'REF_DATE','VALUE','DGUID','UOM_ID','SCALAR_ID','VECTOR','COORDINATE','STATUS','SYMBOL','TERMINATED','DECIMALS'}
c=collections.defaultdict(collections.Counter); dates=set()
with open(f,encoding='utf-8-sig') as fh:
    for r in csv.DictReader(fh):
        dates.add(r['REF_DATE'])
        for k,v in r.items():
            if k not in skip: c[k][v]+=1
print('dates',min(dates),max(dates),len(dates))
for k,v in c.items():
    print('##',k,len(v)); [print('   ',x) for x in list(v)[:maxv]]
