import csv,sys
w=csv.writer(sys.stdout)
w.writerow(['year','group','metric','value','unit','source','note'])
data={}
for f in sys.argv[1:]:
    for y,g,m,v in csv.reader(open(f)):
        data[(y,g,m)]=float(v)
groups=['canada_born','canada_born_excl_uscit_at_birth','canada_born_arr10y','us_native','foreign_born']
src='US Census Bureau ACS 1-year PUMS person file (www2.census.gov/programs-surveys/acs/data/pums/<year>/1-Year/csv_pus.zip); weighted by PWGTP; author tabulation'
notes={'canada_born':'POBP=301 (born in Canada); includes US citizens born abroad to US parents (CIT=3)','canada_born_excl_uscit_at_birth':'POBP=301 excluding CIT=3','canada_born_arr10y':'Canada-born who entered US in last 10 survey years (YOEP>=year-9); survivors still in US','us_native':'NATIVITY=1 (incl. CIT=3 Canada-born)','foreign_born':'NATIVITY=2'}
for y in sorted({k[0] for k in data}):
    for g in groups:
        if (y,g,'pop_all') not in data: continue
        p=data[(y,g,'pop_all')]; p25=data[(y,g,'pop25')]
        w.writerow([y,g,'population_all_ages',round(p),'persons',src,notes[g]])
        w.writerow([y,g,'population_25plus',round(p25),'persons',src,notes[g]])
        for m,lab in [('ba_plus25','bachelor_or_higher'),('grad_plus25','masters_prof_or_doctorate'),('phd25','doctorate')]:
            if (y,g,m) in data:
                w.writerow([y,g,lab+'_25plus',round(data[(y,g,m)]),'persons',src,notes[g]])
                w.writerow([y,g,lab+'_share_25plus',round(data[(y,g,m)]/p25*100,1),'pct',src,notes[g]])
