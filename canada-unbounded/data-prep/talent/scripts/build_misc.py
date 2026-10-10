import csv,sys
out=sys.argv[1]
P='https://www150.statcan.gc.ca/n1/pub/11-627-m/11-627-m2026013-eng.htm'
CH='https://www150.statcan.gc.ca/n1/pub/36-28-0001/2025007/article/00004-eng.htm'
CL='https://clef.uwaterloo.ca/wp-content/uploads/2024/09/CLEF-074-2024.pdf'
ZD='https://www150.statcan.gc.ca/n1/pub/75-001-x/2000002/article/5072-eng.pdf'
FI='https://www150.statcan.gc.ca/n1/pub/11f0019m/11f0019m2006288-eng.htm'
IM='https://www150.statcan.gc.ca/n1/daily-quotidien/240202/dq240202a-eng.htm'
rows=[
# emigrant profile (Portrait 2026)
('emigrant_profile','age_20_44','2021','emigrants',67.0,'pct','primary',P,'Census 2021-T1FF linkage'),
('emigrant_profile','age_20_44','2021','canadian_population',31.8,'pct','primary',P,''),
('emigrant_profile','immigrants_share','2021','emigrants',61.2,'pct','primary',P,'immigrants = landed immigrants (incl. citizens by naturalization)'),
('emigrant_profile','immigrants_share','2021','canadian_population',23.6,'pct','primary',P,''),
('emigrant_profile','edu_high_school_or_less','2021','emigrants',17.2,'pct','primary',P,'age base of population comparison not stated'),
('emigrant_profile','edu_high_school_or_less','2021','canadian_population',43.2,'pct','primary',P,''),
('emigrant_profile','edu_college_trades','2021','emigrants',12.9,'pct','primary',P,''),
('emigrant_profile','edu_college_trades','2021','canadian_population',27.8,'pct','primary',P,''),
('emigrant_profile','edu_bachelor','2021','emigrants',38.7,'pct','primary',P,''),
('emigrant_profile','edu_bachelor','2021','canadian_population',20.1,'pct','primary',P,''),
('emigrant_profile','edu_masters_or_higher','2021','emigrants',31.1,'pct','primary',P,''),
('emigrant_profile','edu_masters_or_higher','2021','canadian_population',8.9,'pct','primary',P,''),
('emigrant_profile','occ_natural_applied_sciences','2021','emigrants',22.9,'pct','primary',P,''),
('emigrant_profile','occ_natural_applied_sciences','2021','canadian_population',7.9,'pct','primary',P,''),
('emigrant_profile','occ_business_finance_admin','2021','emigrants',21.2,'pct','primary',P,''),
('emigrant_profile','occ_business_finance_admin','2021','canadian_population',17.4,'pct','primary',P,''),
('emigrant_profile','canadian_born_abroad_in_us','2020','canadian_born_abroad',793897,'persons','primary (UN Migrant Stock via StatCan)',P,'61.4% of 1,292,329 Canadian-born living abroad'),
('emigrant_profile','canadian_born_abroad_total','2020','canadian_born_abroad',1292329,'persons','primary (UN Migrant Stock via StatCan)',P,''),
# immigrant re-emigration
('immigrant_reemigration','left_within_20y_of_landing','1982-2017 landings','all_immigrants','>15','pct','primary',IM,'IMDB'),
('immigrant_reemigration','left_within_20y_of_landing','1982-2017 landings','investor_class','>40','pct','primary',IM,''),
('immigrant_reemigration','left_within_20y_of_landing','1982-2017 landings','entrepreneur_class','30','pct','primary',IM,''),
# Choi-Hou text-only
('stem_grad_retention','cs_grads_filing_yr3','2015-2020 grads','canadian_students_high_ranking_univ',77,'pct','primary',CH,'text, not in table; U of T, UBC, Waterloo, McGill, U de M, U of A'),
('stem_grad_retention','cs_grads_filing_yr3','2015-2020 grads','canadian_students_other_institutions',90,'pct','primary',CH,'text, not in table'),
('stem_grad_retention','stem_grads_filing_yr10','2010 grads','canadian_students',83,'pct','primary',CH,'Chart 1 text'),
('stem_grad_retention','intl_share_of_stem_bachelor_plus_grads','2010','all',11,'pct','primary',CH,'5,550 of ~50,930'),
('stem_grad_retention','intl_share_of_stem_bachelor_plus_grads','2021','all',28,'pct','primary',CH,'24,900 international; 63,250 Canadian'),
# Waterloo
('waterloo_outmigration','outmig_3y_nonfiling_5th_pctile_skill','2005-2021 grads','canadian_born',5,'pct','academic (CLEF WP 74)',CL,'read from Fig 6, approximate'),
('waterloo_outmigration','outmig_3y_nonfiling_95th_pctile_skill','2005-2021 grads','canadian_born',10,'pct','academic (CLEF WP 74)',CL,''),
('waterloo_outmigration','outmig_3y_nonfiling_5th_pctile_skill','2005-2021 grads','foreign_born_PR',10,'pct','academic (CLEF WP 74)',CL,''),
('waterloo_outmigration','outmig_3y_nonfiling_95th_pctile_skill','2005-2021 grads','foreign_born_PR',16,'pct','academic (CLEF WP 74)',CL,''),
('waterloo_outmigration','outmig_3y_nonfiling_5th_pctile_skill','2005-2021 grads','international_students',40,'pct','academic (CLEF WP 74)',CL,''),
('waterloo_outmigration','outmig_3y_nonfiling_95th_pctile_skill','2005-2021 grads','international_students',28,'pct','academic (CLEF WP 74)',CL,''),
('waterloo_outmigration','all_grads_outmig_3_consecutive_nonfiling','2005-2021 grads','all',11.4,'pct','academic (CLEF WP 74)',CL,'fn21; 15.8/12.7/11.4/10.6/9.9 for 1-5 yrs nonfiling'),
('waterloo_outmigration','cumulative_outmig_peak','2005-2021 grads','canadian_born',20,'pct','academic (CLEF WP 74)',CL,'peak cumulative'),
('waterloo_outmigration','cumulative_outmig_peak','2005-2021 grads','foreign_born_PR',30,'pct','academic (CLEF WP 74)',CL,''),
('waterloo_outmigration','return_within_yr4_5_of_leavers','2005-2021 grads','canadian_born',16.5,'pct','academic (CLEF WP 74)',CL,'returns lower at top of skill distribution'),
('waterloo_outmigration','return_within_yr4_5_of_leavers','2005-2021 grads','foreign_born_PR',13.0,'pct','academic (CLEF WP 74)',CL,''),
('waterloo_outmigration','return_within_yr4_5_of_leavers','2005-2021 grads','international_students',4.4,'pct','academic (CLEF WP 74)',CL,''),
('waterloo_outmigration','return_within_10y_selfreport','2005-2021 grads','canadian_born',26.4,'pct','academic (CLEF WP 74)',CL,'fn25 T1 self-reports'),
('waterloo_outmigration','intl_students_transition_to_PR','2005-2021 grads','international_students',70,'pct','academic (CLEF WP 74)',CL,'roughly; ~2x national rate'),
# 1990s baseline
('zdm_1990s','degree_holders_entering_canada_per_year','1990-1996','all_inflows',39000,'persons/yr','primary (StatCan Perspectives 2000)',ZD,'perm+temp; incl 11,000 master/PhD'),
('zdm_1990s','masters_phd_entering_canada_per_year','1990-1996','all_inflows',11000,'persons/yr','primary',ZD,''),
('zdm_1990s','degree_holders_leaving_for_us_per_year','1990-1999','to_us',10000,'persons/yr','primary',ZD,'estimate from US CPS 1994-99; perm+temp'),
('zdm_1990s','ratio_degree_inflow_to_us_outflow','1990s','ratio',3.9,'ratio','primary',ZD,'~4 to 1 as stated'),
('zdm_1990s','physicians_emigrating_to_us_per_year','1996-1997','physicians',450,'persons/yr','primary',ZD,'~150/yr late 1980s'),
('zdm_1990s','nurses_emigrating_to_us_per_year','1996-1997','nurses',825,'persons/yr','primary',ZD,''),
('zdm_1990s','net_loss_ratio_to_us_physicians','1990s','physicians',19,'ratio out:in','primary',ZD,'nurses 15:1; engineers & managers 7:1'),
('zdm_1990s','share_1995_grads_in_us_1997','1995 grads','all_postsecondary',1.5,'pct','primary',ZD,'12% of doctoral grads; 18% of movers back by 1999'),
('zdm_1990s','share_1995_grads_in_us_1997','1995 grads','doctoral',12,'pct','primary',ZD,''),
('finnie_lad','annual_departure_rate_taxfilers','1982-2003','all_taxfilers','0.045-0.133','pct/yr','primary (StatCan 11F0019M no.288)',FI,'higher at $100k+ incomes; ~15% return within 5 years'),
('finnie_lad','returned_within_5y','1982-2003','leavers',15,'pct','primary',FI,''),
]
w=csv.writer(open(out,'w',newline=''))
w.writerow(['topic','indicator','period','group','value','unit','label','source_url','note'])
for r in rows: w.writerow(r)
