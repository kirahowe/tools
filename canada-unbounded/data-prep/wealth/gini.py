# Grouped-data Gini (trapezoid Lorenz; lower bound since within-group inequality ignored)
import csv,sys
def gini(groups):  # list of (pop_share, wealth_share) ordered poorest->richest
    L=0;P=0;area=0
    for p,s in groups:
        area+=p*(2*L+s); L+=s; P+=p
    return 1-area
out=sys.argv[2]
w=csv.writer(open(out,'w',newline=''))
w.writerow(['source','year','grouping','gini_grouped_lower_bound','note'])
# SFS deciles
d={}
for r in csv.DictReader(open(sys.argv[1])):
    if r['geo']=='Canada': d.setdefault(r['sfs_year'],[]).append(r)
order=['Lowest','Second','Third','Fourth','Fifth','Sixth','Seventh','Eighth','Ninth','Highest']
for y,rs in sorted(d.items()):
    tot=[float(r[[k for k in r if k.startswith('total_net_worth')][0]]) for r in rs if r['net_worth_deciles'].startswith('Total')][0]
    g=[]
    for o in order:
        r=[r for r in rs if r['net_worth_deciles'].startswith(o)][0]
        g.append((0.1,float(r[[k for k in r if k.startswith('total_net_worth')][0]])/tot))
    w.writerow(['StatCan SFS table 11-10-0075-01',y,'net worth deciles (economic families)',round(gini(g),3),'computed by Claude from decile totals'])
