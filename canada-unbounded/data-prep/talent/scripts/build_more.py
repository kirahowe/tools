import csv
def W(fn,header,rows):
    w=csv.writer(open(fn,'w',newline='')); w.writerow(header); [w.writerow(r) for r in rows]
# US admin flows
L='https://ohss.dhs.gov/topics/immigration/yearbook/2022/table3'
H='https://www.uscis.gov/sites/default/files/document/reports/OLA_Signed_H-1B_Characteristics_Congressional_Report_FY2023.pdf'
lpr=[13181,11586,12673,12793,11484,9898,11388,9526,9978,11317]
rows=[[2013+i,'us_lpr_green_cards_born_in_canada',v,'persons','DHS OHSS Yearbook 2022 Table 3 (by country of birth)',L,'fiscal year; country of birth not citizenship'] for i,v in enumerate(lpr)]
rows+=[[2023,'us_h1b_approved_petitions_born_in_canada_total',3852,'petitions','USCIS H-1B Characteristics Report FY2023, Fig 5/Appx Table 4a',H,'1.0% of 386,318; petitions not persons; country of birth'],
       [2023,'us_h1b_approved_petitions_born_in_canada_initial',1881,'petitions','USCIS FY2023 Table 4b',H,'initial employment'],
       [2023,'us_h1b_approved_petitions_born_in_canada_continuing',1971,'petitions','USCIS FY2023 Table 4c',H,'continuing employment'],
       [2018,'us_i94_temp_worker_admissions_canadian_citizens_H1B',65076,'admission events','DHS OHSS Yearbook 2018 Table 32','https://ohss.dhs.gov/topics/immigration/yearbook/2018/table32','events not persons; TN not broken out (in Other 667,850); most short-term Canadian admissions excluded']]
W('us_admin_flows_canadians.csv',['year','series','value','unit','source','url','note'],rows)
# CIHI
C24='https://www.cihi.ca/sites/default/files/document/supply-distribution-migration-physicians-in-canada-2024-data-tables-en.xlsx'
C23='https://www.cihi.ca/sites/default/files/document/supply-distribution-migration-physicians-in-canada-2023-data-tables-en.xlsx'
rows=[[2024,'moved_abroad','all',109,C24,'Table 13.0'],[2024,'moved_abroad','canadian_md_grads',62,C24,'Table 14.0'],[2024,'moved_abroad','foreign_md_grads',45,C24,'Table 14.1 (sum with 14.0 =107; 2 unknown place of MD presumably)'],
      [2024,'returned_from_abroad','all',74,C24,'Table 16.0'],[2024,'returned_from_abroad','canadian_md_grads',44,C24,'Table 17.0'],[2024,'returned_from_abroad','foreign_md_grads',26,C24,'Table 17.1'],
      [2024,'net_international','all',-35,C24,'Table A'],[2024,'physician_stock','all',99555,C24,'Table A'],[2024,'share_internationally_trained_pct','all',27.4,C24,'Table A'],
      [2023,'moved_abroad','all',84,C23,'Table 13.0'],[2023,'moved_abroad','canadian_md_grads',39,C23,'Table 14.0'],[2023,'moved_abroad','foreign_md_grads',44,C23,'Table 14.1'],
      [2023,'returned_from_abroad','all',98,C23,'Table 16.0'],[2023,'returned_from_abroad','canadian_md_grads',41,C23,'Table 17.0'],[2023,'returned_from_abroad','foreign_md_grads',56,C23,'Table 17.1'],
      [2023,'net_international','all',14,C23,'Table A'],
      ['1990-1995 avg','net_international','all',-400,'https://www.longwoods.com/content/19899','Watanabe, Comeau & Buske 2008 Healthcare Policy 3(4): "net losses averaged 400 per year" early-mid 1990s'],
      ['c.2004-2006','net_international','all','+30 to +60','https://www.longwoods.com/content/19899','same; "net gains of between 30 to 60 per year"']]
W('cihi_physician_international_migration.csv',['year','flow','md_origin','value','source_url','note'],
  [r[:4]+[r[4],r[5]+'; CIHI Scott\'s Medical Database; since 2017 abroad status relies on licensing authorities & self-report (likely undercount)'] for r in rows])
# Census 2021 education by immigrant status (98-10-0446, 25-64)
S='StatCan Census 2021 Table 98-10-0446-01 via WDS (Canada; 25 to 64; total gender; private households)'
c={'total':(19568260,6431940),'non_immigrants':(13592395,3642705),'immigrants':(5423905,2453410),'immigrants_2016_2021':(901075,535635),'non_permanent_residents':(551960,335825)}
rows=[]
for g,(p,b) in c.items(): rows.append([2021,g,p,b,round(b/p*100,1),S])
W('census2021_degree_by_immigrant_status.csv',['census_year','group','pop_25_64','bachelor_or_higher_25_64','share_pct','source'],rows)
# immigrant entrepreneurship
PR='https://www150.statcan.gc.ca/n1/pub/11f0019m/11f0019m2019011-eng.htm'
PO='https://www150.statcan.gc.ca/n1/pub/36-28-0001/2021009/article/00001-eng.htm'
GR='https://www150.statcan.gc.ca/n1/pub/11f0019m/11f0019m2016375-eng.htm'
rows=[['share_of_private_incorporated_firms','2003-2013','immigrant_owned (immigrants landed since 1980)',17,'pct','Picot & Rollin 2019 (StatCan 11F0019M no.423? catalogue 11F0019M2019011)',PR,''],
['share_of_net_jobs_created','2003-2013','immigrant_owned',25,'pct','Picot & Rollin 2019',PR,'advantage disappears after controlling for firm age/size'],
['ownership_rate_incorporated_with_employees','2016','immigrants',5.2,'pct of pop 25-69','Picot & Ostrovsky 2021 ESR',PO,''],
['ownership_rate_incorporated_with_employees','2016','second_generation',5.0,'pct of pop 25-69','Picot & Ostrovsky 2021',PO,''],
['ownership_rate_incorporated_with_employees','2016','third_plus_generation',4.0,'pct of pop 25-69','Picot & Ostrovsky 2021',PO,''],
['primary_self_employment_rate','2016','immigrants',6.9,'pct of pop 25-69','Picot & Ostrovsky 2021',PO,''],
['primary_self_employment_rate','2016','second_generation',5.3,'pct of pop 25-69','Picot & Ostrovsky 2021',PO,''],
['primary_self_employment_rate','2016','third_plus_generation',4.6,'pct of pop 25-69','Picot & Ostrovsky 2021',PO,''],
['stem_grad_share_owners_incorporated_with_employees','2016','immigrants',22.1,'pct','Picot & Ostrovsky 2021 Chart 2',PO,'STEM incl. college'],
['stem_grad_share_owners_incorporated_with_employees','2016','second_generation',12.3,'pct','Picot & Ostrovsky 2021',PO,''],
['stem_grad_share_owners_incorporated_with_employees','2016','third_plus_generation',10.0,'pct','Picot & Ostrovsky 2021',PO,''],
['sme_product_innovation_3y','c.2014-2017','immigrant_owned',29,'pct','Picot & Ostrovsky 2021',PO,'adjusted'],
['sme_product_innovation_3y','c.2014-2017','canadian_born_owned',27,'pct','Picot & Ostrovsky 2021',PO,'adjusted'],
['sme_process_innovation_3y','c.2014-2017','immigrant_owned',21,'pct','Picot & Ostrovsky 2021',PO,''],
['sme_process_innovation_3y','c.2014-2017','canadian_born_owned',17,'pct','Picot & Ostrovsky 2021',PO,''],
['early_stage_entrepreneurship_share_first_second_gen','n.d.','first+second generation',34.7,'pct','MaRS (CONTEXT, industry; GEM-based, not verified)','https://www.marsdd.com/our-story/how-immigrant-tech-founders-boost-canadian-innovation/','context only'],
]
W('immigrant_entrepreneurship.csv',['indicator','period','group','value','unit','source','url','note'],rows)
# AI
A='https://hai.stanford.edu/assets/files/hai_ai-index-report-2025_chapter4_final.pdf'
rows=[[2024,'net_ai_talent_migration_per_10k_linkedin','Canada',0.95,'Stanford AI Index 2025 Fig 4.2.23 (LinkedIn)',A,'positive=net inflow; peaked ~2.4 in 2022-23 (read from chart); index says Canada among countries with declining net AI talent migration'],
[2024,'net_ai_talent_migration_per_10k_linkedin','United States',1.07,'AI Index 2025 Fig 4.2.22',A,''],
[2024,'net_ai_talent_migration_per_10k_linkedin','Germany',2.13,'AI Index 2025',A,''],
[2024,'net_ai_talent_migration_per_10k_linkedin','Israel',-2.10,'AI Index 2025',A,''],
[2024,'net_ai_talent_migration_per_10k_linkedin','India',-1.55,'AI Index 2025',A,''],
[2024,'newly_funded_ai_companies','Canada',51,'AI Index 2025 (Quid)',A,'6th'],
['2013-2024','newly_funded_ai_companies_sum','Canada',481,'AI Index 2025 (Quid)',A,'US 6,956; China 1,605; UK 885; Israel 492'],
[2024,'ai_private_investment_usd_bn','Canada',2.89,'AI Index 2025 (Quid)',A,'US 109.08'],
['2013-2024','ai_private_investment_sum_usd_bn','Canada',15.31,'AI Index 2025 (Quid)',A,''],
[2026,'canada_cifar_ai_chairs','Canada','>130','CIFAR (context, program self-report)','https://cifar.ca/ai/impact/','"more than 130" researchers recruited via chairs program'],
]
W('ai_talent_indicators.csv',['year','indicator','country','value','source','url','note'],rows)
