import openpyxl,sys,csv
f,label,out,total_fam,total_nw=sys.argv[1],sys.argv[2],sys.argv[3],float(sys.argv[4]),float(sys.argv[5])
wb=openpyxl.load_workbook(f,read_only=True,data_only=True)
ws=wb.worksheets[0]
rows=[(float(a),float(b)) for a,b in list(ws.iter_rows(values_only=True))[1:] if a is not None]
rows.sort(reverse=True)
W=sum(w for _,w in rows); NW=sum(v*w for v,w in rows)
print(label,'records',len(rows),'weight(families)',round(W),'wealth $bn',round(NW/1e9,1),'min',min(v for v,_ in rows),'max',max(v for v,_ in rows))
# top shares
tops=[0.2,0.1,0.05,0.01,0.005,0.001,0.0001,0.00001,0.000001]
res=[]
for t in tops:
    target=t*total_fam; cw=0; cs=0; thr=None
    for v,w in rows:
        if cw+w>=target:
            part=target-cw; cs+=part*v; cw=target; thr=v; break
        cw+=w; cs+=w*v
    if thr is None: res.append((t,None,None,None)); continue
    res.append((t,thr,cs,100*cs/total_nw))
for t,thr,cs,sh in res:
    print(f'top {t*100:g}%: families {t*total_fam:,.0f} threshold ${thr/1e6 if thr else float("nan"):,.2f}M  wealth ${cs/1e9 if cs else float("nan"):,.0f}bn share {sh if sh else float("nan"):.2f}%')
w=csv.writer(open(out,'w',newline=''))
w.writerow(['source','top_group_pct','n_families','threshold_net_worth_cad','cumulative_net_worth_cad','share_of_total_pct','denominators'])
for t,thr,cs,sh in res:
    if thr: w.writerow([label,f'{t*100:g}',round(t*total_fam),round(thr),round(cs),round(sh,3),f'total families {total_fam:.0f}; total NW {total_nw:.0f}'])
