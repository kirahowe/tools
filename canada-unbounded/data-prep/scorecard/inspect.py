import zipfile,csv,io,sys
from collections import defaultdict
f=sys.argv[1]; maxrows=int(sys.argv[2]) if len(sys.argv)>2 else 10**9
z=zipfile.ZipFile(f); name=[n for n in z.namelist() if 'MetaData' not in n][0]
r=csv.DictReader(io.TextIOWrapper(z.open(name),encoding='utf-8-sig'))
skip={'REF_DATE','VALUE','VECTOR','COORDINATE','STATUS','SYMBOL','TERMINATED','DECIMALS','DGUID','SCALAR_ID','UOM_ID'}
u=defaultdict(set); dates=set()
for i,row in enumerate(r):
    if i>=maxrows: break
    dates.add(row['REF_DATE'])
    for k,v in row.items():
        if k not in skip and len(u[k])<80: u[k].add(v)
print(name, 'dates', min(dates), max(dates))
for k,v in u.items(): print(' ',k,len(v),sorted(v)[:80])
