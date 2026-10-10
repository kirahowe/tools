import re,sys,json,csv,os
out=csv.writer(sys.stdout); out.writerow(['year','rank','company','revenue_musd','assets_musd','url'])
for f in sys.argv[1:]:
    y=os.path.basename(f).split('.')[0]
    t=open(f,errors='replace').read()
    m=re.search(r'<script id="__NEXT_DATA__"[^>]*>(.*?)</script>',t,re.S)
    if not m: continue
    d=json.loads(m.group(1))
    items=d['props']['pageProps'].get('franchiseSearch',{}).get('items',[])
    for it in items:
        dd={x['key'] if isinstance(x,dict) and 'key' in x else None:x for x in []}
        data=it.get('data',{})
        def g(k):
            v=data.get(k)
            if isinstance(v,str): v=v.replace('$','').replace(',','')
            return v
        out.writerow([y,it.get('rank'),it.get('name'),g('Revenues ($M)'),g('Assets ($M)'),f'https://fortune.com/ranking/fortune500/{y}/'])
