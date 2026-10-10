import csv,sys
r=csv.DictReader(open(sys.argv[1],encoding='utf-8-sig'))
q=['Lowest wealth quintile','Second wealth quintile','Third wealth quintile','Fourth wealth quintile','Highest wealth quintile','All households']
out={}
for x in r:
    if x['Wealth']=='Net worth (wealth)' and x['Characteristics'] in q and x['Statistics'] in ('Distribution of value','Value per household') and x['VALUE']:
        out.setdefault(x['REF_DATE'],{})[(x['Characteristics'],x['Statistics'])]=float(x['VALUE'])
w=csv.writer(open(sys.argv[2],'w',newline=''))
w.writerow(['quarter_start','top20_share_pct','fourth_q_share_pct','third_q_share_pct','second_q_share_pct','bottom20_share_pct','bottom40_share_pct','wealth_gap_top20_minus_bottom40_pp','top20_avg_networth_dollars','bottom40_avg_networth_dollars_calc','all_avg_networth_dollars'])
for d in sorted(out):
    o=out[d]; s=lambda c:o.get((c,'Distribution of value'))
    v=lambda c:o.get((c,'Value per household'))
    if s('Highest wealth quintile') is None: continue
    b40=s('Lowest wealth quintile')+s('Second wealth quintile')
    b40avg=(v('Lowest wealth quintile')+v('Second wealth quintile'))/2 if v('Lowest wealth quintile') is not None else ''
    w.writerow([d,s('Highest wealth quintile'),s('Fourth wealth quintile'),s('Third wealth quintile'),s('Second wealth quintile'),s('Lowest wealth quintile'),round(b40,1),round(s('Highest wealth quintile')-b40,1),v('Highest wealth quintile'),round(b40avg) if b40avg!='' else '',v('All households')])
