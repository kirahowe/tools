import csv
src='raw/crtc/tel_overview/'
out=[]
def rd(f): return list(csv.reader(open(src+f,encoding='latin1')))
r=rd('T-F14.csv'); hdr=r[3]
for row in r[4:8]:
    for i,y in enumerate(hdr[1:],1):
        if y and row[i]: out.append([y[:4],'retail_mobile_revenue_share_pct',row[0],float(row[i]),'CRTC Canadian Telecommunications Market Report 2025 open data, Figure 14 (2020+ mobile phone only, "MP")'])
r=rd('T-T1.csv'); hdr=r[3]
for row in r[4:9]:
    for i,y in enumerate(hdr[1:13],1):
        out.append([y,'total_telecom_revenue_share_pct',row[0],float(row[i]),'CRTC CTMR 2025 open data, Table 1'])
r=rd('T-F13.csv'); hdr=r[3]
for row in r[4:7]:
    for i,y in enumerate(hdr[1:],1):
        if y: out.append([y,'ebitda_margin_pct',row[0],float(row[i]),'CRTC CTMR 2025 open data, Figure 13'])
# big3 mobile combined
yrs=sorted(set(o[0] for o in out if o[1]=='retail_mobile_revenue_share_pct'))
for y in yrs:
    s=sum(o[3] for o in out if o[1]=='retail_mobile_revenue_share_pct' and o[0]==y and o[2]!='Other')
    out.append([y,'retail_mobile_revenue_share_pct','Big 3 combined (Bell+TELUS+Rogers)',round(s,1),'Derived: sum of Figure 14 rows'])
w=csv.writer(open('B3_telecom_market_shares_crtc.csv','w',newline=''))
w.writerow(['year','measure','provider_group','value','source']); w.writerows(out)
