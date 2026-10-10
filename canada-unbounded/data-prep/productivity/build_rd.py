import csv,sys
r=list(csv.DictReader(open('raw/oecd/msti_ptgdp.csv')))
out=[]
for x in r:
    m=x['MEASURE: Measure'].split(':')[0]
    if m not in ('B','G','G_FB'): continue
    if x['OBS_VALUE']=='' : continue
    out.append(dict(year=int(x['TIME_PERIOD: Time period']),area_code=x['REF_AREA: Reference area'].split(':')[0],area=x['REF_AREA: Reference area'].split(': ',1)[1],
      measure={'B':'BERD_pct_GDP','G':'GERD_pct_GDP','G_FB':'GERD_business_financed_pct_GDP'}[m],value=float(x['OBS_VALUE']),
      obs_status=x['OBS_STATUS: Observation status']))
out.sort(key=lambda d:(d['measure'],d['area_code'],d['year']))
w=csv.DictWriter(open('E1_rd_intensity_oecd_msti.csv','w',newline=''),fieldnames=list(out[0])+['source'])
w.writeheader()
for d in out: d['source']='OECD MSTI, OECD.STI.STP:DSD_MSTI@DF_MSTI(1.3), unit PT_B1GQ'; w.writerow(d)
