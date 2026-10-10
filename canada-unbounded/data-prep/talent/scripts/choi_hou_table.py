import re,csv,sys
txt=open(sys.argv[1]).read().splitlines()
start=[i for i,l in enumerate(txt) if l.strip().startswith('All fields')][0]
end=[i for i,l in enumerate(txt) if '… not applicable' in l][0]
cols=[('Canadian students','2010-2014',1),('Canadian students','2010-2014',3),('Canadian students','2010-2014',5),('Canadian students','2015-2020',1),('Canadian students','2015-2020',3),
      ('International students','2010-2014',1),('International students','2010-2014',3),('International students','2010-2014',5),('International students','2015-2020',1),('International students','2015-2020',3)]
group='Total'; pending=''
rows=[]
headers={'Field of study':'field_of_study','Degree level':'degree_level','Country of citizenship':'country_of_citizenship','World university ranking':'university_ranking','Pre-graduation earnings':'pre_graduation_earnings'}
for l in txt[start:end]:
    s=l.strip()
    if not s: continue
    if s in headers: group=headers[s]; continue
    if s.startswith('Maximum annual T4') : continue
    nums=re.findall(r'(\d+\.\d|…)\s*',s)
    label=re.sub(r'\s+(\d+\.\d|…)(\s+(\d+\.\d|…))*\s*$','',s).strip()
    if len(nums)<10:
        pending=(pending+' '+s).strip(); continue
    label=(pending+' '+label).strip(); pending=''
    if group=='Total': g='all_or_stem' 
    else: g=group
    for (st,coh,yr),v in zip(cols,nums[-10:]):
        rows.append([st,coh,g,label,yr,'' if v=='…' else v])
w=csv.writer(sys.stdout)
w.writerow(['student_status','graduation_cohort','breakdown','group','years_since_graduation','pct_filing_tax_in_canada','source','note'])
src='StatCan, Choi & Hou (2025), Economic and Social Reports 5(7), Table 1, https://www150.statcan.gc.ca/n1/pub/36-28-0001/2025007/article/00004-eng.htm'
note='Tax filing (T1FF presence) as proxy for presence in Canada; non-filers include stayers who do not file. STEM bachelor+ grads of provincially funded institutions. 2015-2020 Year-3 uses 2015-2018 grads. Canadian students = citizens + PRs at graduation.'
for r in rows: w.writerow(r+[src,note])
