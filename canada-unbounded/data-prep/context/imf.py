import sys,re,csv
txt=open(sys.argv[1]).read().splitlines()
def grab(start,end):
    out={}
    for l in txt[start:end]:
        m=re.match(r'\s*(Canada2|France|Germany|Italy|Japan|United Kingdom|United States2|G7)\s+(.*)',l)
        if m:
            vals=[v.replace('–','-') for v in m.group(2).split()]
            out[m.group(1).rstrip('2')]=vals
    return out
g=grab(1221,1282); n=grab(1282,1333)
yrs=list(range(2017,2032))
w=csv.writer(open(sys.argv[2],'w',newline=''))
w.writerow(['country','year','gg_gross_debt_pct_gdp','gg_net_debt_pct_gdp','projection'])
for c in g:
    for i,y in enumerate(yrs):
        w.writerow([c,y,g[c][i],n[c][i],'yes' if y>=2026 else 'no'])
