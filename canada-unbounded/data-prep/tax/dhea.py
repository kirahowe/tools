import csv,sys,collections
R=sys.argv[1]; OUT=sys.argv[2]
def rows(p):
    with open(p,encoding='utf-8-sig') as f:
        yield from csv.DictReader(f)
# household sector current transfers paid breakdown, unadjusted quarters summed
h=collections.defaultdict(float)
for r in rows(R+'/36100112/36100112.csv'):
    if r['Seasonal adjustment']=='Unadjusted' and r['REF_DATE'][:4] in('2024','2025') and r['VALUE']:
        h[(r['REF_DATE'][:4],r['Estimates'])]+=float(r['VALUE'])
for y in ('2024','2025'):
    print(y,{k[1]:round(v) for k,v in h.items() if k[0]==y and k[1] in('Less: current transfers paid','To non-profit institutions serving households','To corporations','To general governments','Personal income tax','Other current transfers','Of which: contributions to social insurance plans','To non-residents','Household disposable income')})
# income table
inc={}
for r in rows(R+'/36100587/36100587.csv'):
    if r['Statistics'] in('Value','Value per household','Distribution of value') and r['VALUE']:
        inc[(r['REF_DATE'],r['Statistics'],r['Characteristics'],r['Income, consumption and savings'])]=float(r['VALUE'])
# wealth table: average of 4 quarters, and Q4
w=collections.defaultdict(list); wq4={}
for r in rows(R+'/36100660/36100660.csv'):
    if r['Statistics'] in('Value','Value per household','Distribution of value') and r['VALUE'] and r['Wealth'] in('Net worth (wealth)','Total assets','Real estate','Other financial assets','Life insurance and pensions'):
        k=(r['REF_DATE'][:4],r['Statistics'],r['Characteristics'],r['Wealth'])
        w[k].append(float(r['VALUE']))
        if r['REF_DATE'].endswith('-10'): wq4[k]=float(r['VALUE'])
groups=['Lowest','Second','Third','Fourth','Highest']
out=[]
for y in ('2023','2024','2025'):
    tot_nw=sum(w[(y,'Value','All households','Net worth (wealth)')])/len(w[(y,'Value','All households','Net worth (wealth)')])
    for g in ['All households']+[f'{x} income quintile' for x in groups]:
        nw=w[(y,'Value',g,'Net worth (wealth)')]; nwa=sum(nw)/len(nw)
        nwhh=sum(w[(y,'Value per household',g,'Net worth (wealth)')])/4
        ctp=inc[(y,'Value',g,'Less: current transfers paid')]; ctp_all=inc[(y,'Value','All households','Less: current transfers paid')]
        hdi=inc[(y,'Value',g,'Household disposable income')]
        prim=hdi+ctp-inc[(y,'Value',g,'Current transfers received')]
        out.append(dict(year=y,group=g,n_quarters_wealth=len(nw),
          net_worth_avg_millions=round(nwa),net_worth_q4_millions=wq4.get((y,'Value',g,'Net worth (wealth)')),
          wealth_share_pct=round(100*nwa/tot_nw,2),
          net_worth_per_hh_avg=round(nwhh),
          current_transfers_paid_millions=round(ctp),
          ctp_share_pct=round(100*ctp/ctp_all,2),
          ctp_per_hh=inc[(y,'Value per household',g,'Less: current transfers paid')],
          disposable_income_millions=round(hdi),
          ctp_pct_of_net_worth=round(100*ctp/nwa,3),
          ctp_pct_of_income_before_transfers_paid=round(100*ctp/(hdi+ctp),2),
          ctp_pct_of_primary_income=round(100*ctp/prim,2)))
with open(OUT+'/tax_by_income_quintile_dhea.csv','w',newline='') as f:
    wr=csv.DictWriter(f,fieldnames=list(out[0])); wr.writeheader(); wr.writerows(out)
for o in out: print(o)
# wealth quintiles (wealth only)
wout=[]
for y in ('2024','2025'):
    tot=sum(w[(y,'Value','All households','Net worth (wealth)')])/4
    for g in ['All households']+[f'{x} wealth quintile' for x in groups]:
        v=w[(y,'Value',g,'Net worth (wealth)')]; a=sum(v)/len(v)
        wout.append(dict(year=y,group=g,net_worth_avg_millions=round(a),wealth_share_pct=round(100*a/tot,2),
          net_worth_per_hh_avg=round(sum(w[(y,'Value per household',g,'Net worth (wealth)')])/4),
          net_worth_q4_millions=wq4.get((y,'Value',g,'Net worth (wealth)')),
          q4_share_published_pct=wq4.get((y,'Distribution of value',g,'Net worth (wealth)'))))
with open(OUT+'/wealth_by_wealth_quintile_dhea.csv','w',newline='') as f:
    wr=csv.DictWriter(f,fieldnames=list(wout[0])); wr.writeheader(); wr.writerows(wout)
for o in wout: print(o)
