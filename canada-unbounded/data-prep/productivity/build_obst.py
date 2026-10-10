import csv
O='Most challenging obstacle when purchasing or selling interprovincially experienced by the business or organization over the last 12 months'
w=csv.writer(open('C2_interprovincial_obstacles_survey_q1_2026.csv','w',newline=''))
w.writerow(['ref_period','geo','business_group','obstacle','pct_of_businesses','status','source'])
for r in csv.DictReader(open('raw/33101118/33101118.csv',encoding='utf-8-sig')):
    if r['GEO']=='Canada' and r['VALUE']:
        w.writerow(['Q1 2026 survey (12-month recall; REF_DATE '+r['REF_DATE']+')',r['GEO'],r['Business characteristics'],r[O].replace(O+', ','').strip(),r['VALUE'],r['STATUS'],'StatCan 33-10-1118-01 (Canadian Survey on Business Conditions)'])
