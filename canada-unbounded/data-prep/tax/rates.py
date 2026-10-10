import re,sys,csv
L=open(sys.argv[1]).read().split('\n')
blocks={};cur=None
for l in L:
    m=re.match(r'\s*(.+?) rate: 2026',l)
    if m: cur=m.group(1); blocks[cur]=[]; continue
    if cur and ('Refer to Revenu' in l or 'Adjustments to income' in l): cur=None
    if cur: blocks[cur].append(l)
out=[]
fed=None
for name,b in blocks.items():
    rates=[float(x.strip(' |%')) for x in b if re.match(r'\s*\|\s*[\d.]+%\s*$',x)]
    froms=[x.strip(' |$').replace(',','') for x in b if re.match(r'\s*\|\s*\$[\d,.]+\s*$',x)]
    thr=[x for x in froms if x.endswith('.01')]
    top=rates[-1]; t=thr[-1] if thr else ''
    if name=='Federal': fed=(top,t); continue
    out.append([name,top,t])
rows=[]
for name,top,t in out:
    eff=top; note=''
    if name=='Ontario':
        eff=round(top*1.56,4); note='Ontario surtax 20%+36% of basic Ontario tax above $5,818/$7,446 (CRA T4127 Jan 2026, factor V1); effective top Ontario rate = 13.16% x 1.56'
    rows.append(dict(jurisdiction=name,federal_top_rate=fed[0],federal_top_threshold=fed[1],prov_top_statutory_rate=top,prov_top_threshold=t,prov_top_effective_rate=eff,combined_top_rate=round(fed[0]+eff,3),note=note))
rows.append(dict(jurisdiction='Quebec',federal_top_rate=fed[0],federal_top_threshold=fed[1],prov_top_statutory_rate=25.75,prov_top_threshold='132245.01',prov_top_effective_rate=25.75,combined_top_rate=round(fed[0]*(1-0.165)+25.75,3),note='Federal tax reduced by 16.5% Quebec abatement; Quebec rate from Revenu Quebec page (not downloadable; cited via search)'))
rows.sort(key=lambda r:-r['combined_top_rate'])
w=csv.DictWriter(open(sys.argv[2],'w',newline=''),fieldnames=list(rows[0])); w.writeheader(); w.writerows(rows)
for r in rows: print(r['jurisdiction'],r['prov_top_statutory_rate'],r['prov_top_threshold'],r['combined_top_rate'])
