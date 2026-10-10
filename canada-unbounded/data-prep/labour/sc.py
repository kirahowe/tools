import csv
def rows(f, **cond):
    out=[]
    with open(f,encoding='utf-8-sig') as fh:
        rd=csv.DictReader(fh)
        for r in rd:
            ok=True
            for k,v in cond.items():
                k=k.replace('__',' ')
                if r.get(k)!=v: ok=False;break
            if ok: out.append(r)
    return out
def find(f, cols, **cond):
    """cols: dict of exact column -> value"""
    out=[]
    with open(f,encoding='utf-8-sig') as fh:
        for r in csv.DictReader(fh):
            if all(r.get(k)==v for k,v in cols.items()): out.append(r)
    return out
