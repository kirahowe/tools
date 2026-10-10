import csv,sys,io,zipfile,collections
zp,year=sys.argv[1],int(sys.argv[2])
z=zipfile.ZipFile(zp)
agg=collections.defaultdict(float)
for name in z.namelist():
    if not name.endswith('.csv'): continue
    with z.open(name) as fb:
        r=csv.reader(io.TextIOWrapper(fb,encoding='latin-1'))
        h=next(r); ix={k:i for i,k in enumerate(h)}
        for row in r:
            nat=row[ix['NATIVITY']]; pob=row[ix['POBP']]
            w=float(row[ix['PWGTP']]); age=int(row[ix['AGEP']])
            schl=row[ix['SCHL']].strip(); s=int(schl) if schl else 0
            grp=['us_native'] if nat=='1' else ['foreign_born']
            if pob=='301': grp.append('canada_born')
            for g in grp:
                agg[(g,'pop_all')]+=w
                if age>=25:
                    agg[(g,'pop25')]+=w
                    if s>=21: agg[(g,'ba_plus25')]+=w
                    if s>=22: agg[(g,'grad_plus25')]+=w
                    if s==24: agg[(g,'phd25')]+=w
                    if s==23: agg[(g,'prof25')]+=w
            if pob=='301':
                yoep=row[ix['YOEP']].strip()
                if yoep and int(yoep)>=year-9:
                    agg[('canada_born_arr10y','pop_all')]+=w
                    if age>=25:
                        agg[('canada_born_arr10y','pop25')]+=w
                        if s>=21: agg[('canada_born_arr10y','ba_plus25')]+=w
                        if s>=22: agg[('canada_born_arr10y','grad_plus25')]+=w
                        if s==24: agg[('canada_born_arr10y','phd25')]+=w
                agg[('canada_born_n','records')]+=1
                if age>=25 and s>=21: 
                    cit=row[ix['CIT']]; agg[('canada_born_ba25_cit'+cit,'pop')]+=w
w=csv.writer(sys.stdout)
for k,v in sorted(agg.items()): w.writerow([year,k[0],k[1],round(v)])
