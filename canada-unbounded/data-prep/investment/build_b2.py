import csv,collections,sys
R=sys.argv[1]; O=sys.argv[2]
d=collections.defaultdict(dict)
for f in ['eo.csv','eo_ppp.csv']:
    for r in csv.DictReader(open(R+'/oecd/'+f)):
        if r['OBS_VALUE']: d[(r['REF_AREA'],r['MEASURE'])][int(r['TIME_PERIOD'])]=float(r['OBS_VALUE'])
ppp17=d[('CAN','PPP')][2017]
yrs=sorted(set(d[('CAN','IBV')])&set(d[('USA','IBV')])&set(d[('CAN','ET')])&set(d[('USA','ET')]))
pc=lambda c,y: d[(c,'IBV')][y]/d[(c,'ET')][y]
with open(O+'/B2_business_investment_per_worker_canada_us.csv','w',newline='') as f:
    w=csv.writer(f); w.writerow(['year','can_priv_nonres_gfcf_per_worker_2017usd_ppp','usa_priv_nonres_gfcf_per_worker_2017usd','can_pct_of_usa','can_index_2014_100','usa_index_2014_100','status','source'])
    for y in yrs:
        c=pc('CAN',y)/ppp17; u=pc('USA',y)
        w.writerow([y,round(c),round(u),round(100*c/u,1),round(100*pc('CAN',y)/pc('CAN',2014),1),round(100*pc('USA',y)/pc('USA',2014),1),'OECD projection' if y>=2026 else 'outturn/estimate','OECD Economic Outlook 119 (OECD.ECO.MAD:DSD_EO@DF_EO(1.5)): IBV private non-res GFCF volume (chain-linked 2017 national currency) / ET total employment; CAD converted at 2017 GDP PPP %.4f CAD/USD (approximation: GDP PPP not investment-goods PPP)'%ppp17])
