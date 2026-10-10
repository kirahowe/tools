import csv, os, sys
root=sys.argv[1]
J='https://milescorak.com/wp-content/uploads/2020/10/connolly-corak-haeck-2019-intergenerational-mobility-canada-us-jole.pdf'
JS='Connolly, Corak & Haeck (2019) JOLE 37(S2):S595-S641'
def W(name,hdr,rows):
    with open(os.path.join(root,name),'w',newline='') as f:
        w=csv.writer(f); w.writerow(hdr); w.writerows(rows)
# 1. Transition matrices (Table 3). rows=child quintile, cols=parent quintile
Q=['Bottom','Second','Third','Fourth','Top']
T={'A. United States (US distribution)':[[33.7,24.2,17.8,13.4,10.9],[28.0,24.2,19.8,16.0,11.9],[18.4,21.7,22.1,20.9,17.0],[12.3,17.6,22.0,24.4,23.6],[7.5,12.3,18.3,25.4,36.5]],
   'B. Canada (Canadian distribution)':[[32.0,20.3,17.0,15.3,15.5],[24.0,23.2,20.7,18.0,14.8],[18.2,21.3,21.6,20.6,18.8],[14.6,19.3,21.8,22.8,21.4],[11.2,15.9,18.9,23.3,29.5]],
   'C. Canada (placed in US distribution)':[[29.1,18.1,15.6,15.4,17.0],[22.5,20.1,16.7,14.1,12.1],[21.7,24.7,23.5,22.0,19.6],[19.0,26.1,29.5,29.3,26.8],[7.7,10.9,14.8,19.3,24.6]]}
rows=[]
for panel,m in T.items():
    for i,cq in enumerate(Q):
        for j,pq in enumerate(Q):
            rows.append([panel,pq,cq,m[i][j],JS+' Table 3',J])
W('cch2019_quintile_transition_matrices.csv',['panel','parent_quintile','child_quintile','percent','source','url'],rows)
# 2. Headline Canada vs US measures
H=[
 ['rank-rank slope (own-country percentiles)','Canada',0.212,'1980 & 1982 births; family income 2011-12 (~age 30-32); parents 1996-2000','Table 2 row 8',JS,J],
 ['rank-rank slope (own-country percentiles)','United States',0.341,'1980-82 births; Chetty et al. (2014)','Table 2 row 8',JS,J],
 ['rank-rank slope (US percentiles)','Canada',0.229,'Canadians ranked in US distribution','Table 2 row 6',JS,J],
 ['rank-rank slope (own country, excl. missing)','Canada',0.228,'','Table 2 row 9',JS,J],
 ['rank-rank slope (own country, parents 1999-2003)','Canada',0.216,'','Table 2 row 10',JS,J],
 ['rank-rank slope (own country, parents 1999-2003)','United States',0.339,'','Table 2 row 10',JS,J],
 ['rank-rank slope (top-earning parent)','Canada',0.219,'','Table 2 row 11',JS,J],
 ['rank-rank slope (top-earning parent)','United States',0.312,'','Table 2 row 11',JS,J],
 ['IGE log family income','Canada',0.310,'','Table 2 row 1',JS,J],
 ['IGE log family income','United States',0.344,'','Table 2 row 1',JS,J],
 ['IGE, zero incomes recoded to $1','Canada',0.474,'','Table 2 row 4',JS,J],
 ['IGE, zero incomes recoded to $1','United States',0.618,'','Table 2 row 4',JS,J],
 ['IGE, incomes <$1,000 recoded to $1,000','Canada',0.294,'','Table 2 row 5',JS,J],
 ['IGE, incomes <$1,000 recoded to $1,000','United States',0.413,'','Table 2 row 5',JS,J],
 ['P(bottom->top quintile), own distribution','Canada',11.2,'percent','Table 3 panel B',JS,J],
 ['P(bottom->top quintile), own distribution','United States',7.5,'percent','Table 3 panel A',JS,J],
 ['P(bottom->top quintile), Canadians in US distribution','Canada',7.7,'percent; same yardstick as US 7.5','Table 3 panel C',JS,J],
 ['P(bottom->bottom), own distribution','Canada',32.0,'percent','Table 3 panel B',JS,J],
 ['P(bottom->bottom), own distribution','United States',33.7,'percent','Table 3 panel A',JS,J],
 ['P(bottom->bottom), Canadians in US distribution','Canada',29.1,'percent','Table 3 panel C',JS,J],
 ['P(top->top), own distribution','Canada',29.5,'percent','Table 3 panel B',JS,J],
 ['P(top->top), own distribution','United States',36.5,'percent','Table 3 panel A',JS,J],
 ['absolute rank mobility, population-weighted avg across regions (US percentiles)','Canada',40.6,'expected rank of child of p25 parents','text p.S625',JS,J],
 ['absolute rank mobility, population-weighted avg across regions (US percentiles)','United States',35.8,'','text p.S625',JS,J],
 ['P(bottom->top), population-weighted avg across regions (US quintile cutoffs)','Canada',8.54,'percent','text p.S625',JS,J],
 ['P(bottom->top), population-weighted avg across regions (US quintile cutoffs)','United States',10.0,'percent','text p.S625',JS,J],
 ['P(bottom->bottom), population-weighted avg across regions','Canada',26.6,'percent','fn 21 p.S625',JS,J],
 ['P(bottom->bottom), population-weighted avg across regions','United States',30.7,'percent','fn 21 p.S625',JS,J],
 ['father-son earnings IGE, comparability-adjusted (preferred)','Canada',0.19,'cohorts born ~early-mid 1960s; range 0.16-0.21','Corak (2004b) Table A-1 as reproduced in Aydemir, Chen & Corak (2005) Table 1','Aydemir, Chen & Corak (2005) StatCan 11F0019MIE No. 267','https://www150.statcan.gc.ca/n1/en/pub/11f0019m/11f0019m2005267-eng.pdf'],
 ['father-son earnings IGE, comparability-adjusted (preferred)','United States',0.47,'range 0.40-0.52','same',
  'Aydemir, Chen & Corak (2005) StatCan 11F0019MIE No. 267','https://www150.statcan.gc.ca/n1/en/pub/11f0019m/11f0019m2005267-eng.pdf'],
 ['father-son earnings IGE, lifetime earnings (fathers 35-55, sons 38-42)','Canada',0.318,'mid-career; corrects life-cycle bias in older 0.2 estimates; market income 0.343, total income 0.359','Chart 1','Chen, Ostrovsky & Piraino (2016) StatCan 11-626-X No. 059; journal version Labour Economics 44 (2017)','https://www150.statcan.gc.ca/n1/pub/11-626-x/11-626-x2016059-eng.htm'],
 ['father-son earnings IGE (Corak & Heisz 1999 replication)','Canada',0.227,'earnings; income 0.222','Chart 1','Chen, Ostrovsky & Piraino (2016) StatCan 11-626-X No. 059','https://www150.statcan.gc.ca/n1/pub/11-626-x/11-626-x2016059-eng.htm'],
 ['father-son earnings elasticity x100 (OECD compilation, late 2000s)','Canada',32,'source: Chen et al. (2017)','Fig 4.8','OECD (2018) A Broken Social Elevator?','http://dx.doi.org/10.1787/888933754253'],
 ['father-son earnings elasticity x100 (OECD compilation, late 2000s)','United States',41.0,'OECD PSID estimate','Fig 4.8','OECD (2018) A Broken Social Elevator?','http://dx.doi.org/10.1787/888933754253'],
 ['generations for bottom-decile family to reach mean income','Canada',4,'illustrative simulation','Fig 1.5','OECD (2018) A Broken Social Elevator?','http://dx.doi.org/10.1787/888933761910'],
 ['generations for bottom-decile family to reach mean income','United States',5,'illustrative simulation','Fig 1.5','OECD (2018) A Broken Social Elevator?','http://dx.doi.org/10.1787/888933761910'],
 ['generations for bottom-decile family to reach mean income','OECD24 average',4.5,'illustrative simulation','Fig 1.5','OECD (2018) A Broken Social Elevator?','http://dx.doi.org/10.1787/888933761910'],
 ['absolute mobility: share out-earning parents at age 30 (1985 cohort)','Canada',57,'percent','Chart 1','Ostrovsky (2023) StatCan ESR 36-28-0001 v3n12 citing Manduca et al. (2024)','https://www150.statcan.gc.ca/n1/pub/36-28-0001/2023012/article/00001-eng.htm'],
 ['absolute mobility: share out-earning parents at age 30 (1985 cohort)','United States',50,'percent','Chart 1','Ostrovsky (2023) StatCan ESR 36-28-0001 v3n12 citing Manduca et al. (2024)','https://www150.statcan.gc.ca/n1/pub/36-28-0001/2023012/article/00001-eng.htm'],
]
W('canada_us_headline_measures.csv',['measure','country','value','note','table_or_location','source','url'],H)
# 3. CCH clusters Table 4/5
C=[['two',1,402,2137,65750,40.1,0.239,10.7,27.0,83508,171,489,231,1648],
   ['two',2,556,8411,245948,32.3,0.345,7.4,34.1,87246,78,284,478,8128],
   ['four',1,153,350,9021,46.3,0.237,16.0,22.2,72841,40,103,113,246],
   ['four',2,342,1421,44988,38.3,0.253,8.1,28.0,64745,175,552,167,869],
   ['four',3,149,5822,178081,34.1,0.321,8.6,32.8,100457,5,88,144,5733],
   ['four',4,314,2956,79608,29.6,0.376,5.9,36.0,68953,29,28,285,2928]]
W('cch2019_regional_clusters.csv',['clustering','cluster','n_regions','children_thousands','total_pop_thousands','absolute_rank_mobility','rank_rank_slope','P15_pct','P11_pct','avg_parent_income_usd','canada_census_divisions','canada_children_thousands','us_commuting_zones','us_children_thousands','source','url'],
  [r+[JS+' Tables 4-5',J] for r in C])
# 4. Corak 2020 province table (1963-70 births)
E='https://milescorak.com/wp-content/uploads/2020/10/corak-2020-economic-journal.pdf'; ES='Corak (2020) Economic Journal 130(631):2134-2174'
P=[['Newfoundland and Labrador',8.69,0.180,29395,0.724,35.3,0.273,42.1,33.2,0.277,40.1,0.087,0.321,0.295,0.210],
['Prince Edward Island',8.91,0.159,30739,0.708,35.1,0.245,41.2,35.3,0.239,41.3,0.077,0.278,0.279,0.232],
['Nova Scotia',8.49,0.192,35158,0.627,32.6,0.251,38.9,32.0,0.249,38.2,0.071,0.350,0.256,0.212],
['New Brunswick',8.54,0.189,32871,0.658,31.6,0.280,38.6,31.1,0.286,38.2,0.061,0.352,0.264,0.214],
['Quebec',8.67,0.186,39700,0.636,36.7,0.249,42.9,36.9,0.240,42.9,0.091,0.290,0.298,0.233],
['Ontario',8.67,0.191,44249,0.624,41.0,0.225,46.6,43.4,0.215,48.8,0.141,0.284,0.352,0.210],
['Manitoba',6.98,0.341,36518,0.630,31.2,0.325,39.3,29.9,0.320,37.9,0.076,0.414,0.296,0.238],
['Saskatchewan',8.19,0.238,39768,0.657,41.5,0.226,47.1,37.7,0.236,43.6,0.141,0.277,0.333,0.222],
['Alberta',8.71,0.194,48544,0.618,44.4,0.206,49.5,41.1,0.203,46.2,0.185,0.259,0.375,0.200],
['British Columbia',8.73,0.176,47185,0.549,39.6,0.184,44.2,39.9,0.185,44.5,0.120,0.298,0.256,0.226],
['Yukon',8.62,0.187,42444,0.570,36.3,0.248,42.5,38.5,0.176,42.9,0.117,0.371,0.295,0.196],
['Northwest Territories and Nunavut',8.67,0.175,29036,0.689,34.1,0.281,41.1,31.4,0.283,38.5,0.100,0.397,0.391,0.178],
['Canada',8.52,0.201,42032,0.625,38.3,0.242,44.3,38.4,0.240,44.4,0.114,0.301,0.323,0.219]]
W('canada_mobility_by_province_corak2020.csv',['province','abs_income_mobility_alpha_log','ige_beta','avg_parent_income_cad2014','share_above_parents','abs_rank_mobility_age38_45','rank_rank_slope_age38_45','expected_rank_bottom_half_age38_45','abs_rank_mobility_age31_32','rank_rank_slope_age31_32','expected_rank_bottom_half_age31_32','P15_rags_to_riches','P11_cycle_of_poverty','P55_cycle_of_privilege','P33_middle_to_middle','cohort','source','url'],
  [r+['born 1963-1970; family income; ranks national',ES+' Tables 4-6',E] for r in P])
# 5. Groups
G=[
 ['children of immigrants (all, 1.5 generation)','rank-rank slope',0.223,'immigrant children linked via landing files; incomes ages 30-34','Adnan, Zhang & Zheng (2023) IZA DP 16471 (working paper)','https://docs.iza.org/dp16471.pdf'],
 ['children of non-refugee immigrants','rank-rank slope',0.205,'','Adnan, Zhang & Zheng (2023) IZA DP 16471','https://docs.iza.org/dp16471.pdf'],
 ['children of refugees','rank-rank slope',0.263,'','Adnan, Zhang & Zheng (2023) IZA DP 16471','https://docs.iza.org/dp16471.pdf'],
 ['Canadian-born (comparison)','rank-rank slope',0.286,'birth cohorts 1975-85, IID-census linkage; figure supplied by M. Connolly (pers. comm. reported in paper) - not a published table','Adnan, Zhang & Zheng (2023) IZA DP 16471, fn 25/35','https://docs.iza.org/dp16471.pdf'],
 ['children of non-refugee immigrants','expected rank of child with parents at p25',51,'percentile','Adnan, Zhang & Zheng (2023) IZA DP 16471','https://docs.iza.org/dp16471.pdf'],
 ['children of refugees','expected rank of child with parents at p25',47,'percentile','Adnan, Zhang & Zheng (2023) IZA DP 16471','https://docs.iza.org/dp16471.pdf'],
 ['Canadian-born (comparison)','expected rank of child with parents at p25',43.7,'percentile; implied by intercept 36.536 + 0.286*25','Adnan, Zhang & Zheng (2023) IZA DP 16471 fn 35','https://docs.iza.org/dp16471.pdf'],
 ['sons of immigrant fathers (2001 Census)','father-son weekly earnings IGE',0.27,'best estimate; "no different than for the population at large"; ~50% lower than US','Aydemir, Chen & Corak (2005) StatCan 11F0019MIE No. 267 (published REStat 2009)','https://www150.statcan.gc.ca/n1/en/pub/11f0019m/11f0019m2005267-eng.pdf'],
 ['sons, all (lifetime earnings) - by father percentile p1','IGE at percentile of fathers lifetime earnings',0.0743,'quantile IGE','Chen, Ostrovsky & Piraino (2016) StatCan 11-626-X No. 059 Chart 2','https://www150.statcan.gc.ca/n1/pub/11-626-x/11-626-x2016059-eng.htm'],
 ['sons, all - father p10','IGE at percentile',0.2661,'','Chen, Ostrovsky & Piraino (2016) Chart 2','https://www150.statcan.gc.ca/n1/pub/11-626-x/11-626-x2016059-eng.htm'],
 ['sons, all - father p50','IGE at percentile',0.3705,'','Chen, Ostrovsky & Piraino (2016) Chart 2','https://www150.statcan.gc.ca/n1/pub/11-626-x/11-626-x2016059-eng.htm'],
 ['sons, all - father p90','IGE at percentile',0.4252,'','Chen, Ostrovsky & Piraino (2016) Chart 2','https://www150.statcan.gc.ca/n1/pub/11-626-x/11-626-x2016059-eng.htm'],
 ['sons, all - father p99','IGE at percentile',0.4475,'','Chen, Ostrovsky & Piraino (2016) Chart 2','https://www150.statcan.gc.ca/n1/pub/11-626-x/11-626-x2016059-eng.htm'],
 ['sons of top-percentile fathers','father-son INCOME elasticity at top percentile',0.8,'"almost 0.8"; earnings elasticity ~0.4 at top percentile; ~0.0 at bottom percentile; sons born 1963-66, early 30s','Corak & Heisz (1998 WP; JHR 34(3) 1999)','https://econwpa.ub.uni-muenchen.de/econ-wp/lab/papers/9808/9808001.pdf'],
 ['sons of top-1% fathers','share who ever worked for an employer that employed their father',0.7,'"almost seven out of ten" vs ~4 in 10 overall','Corak (2013) JEP 27(3), citing Corak & Piraino (2011) JOLE','https://docs.iza.org/dp7520.pdf'],
 ['Indigenous-background children','mobility relative to non-Indigenous',None,'"about two times lower"; explains nearly all of Canada\'s steeper rank-rank slope below parental p20; on-reserve tax exemption may partly depress measured income','Connolly & Haeck (2026) Canadian Public Policy 52(1), "The Great Mobility Puzzle Resolved" (abstract via Crossref)','https://doi.org/10.3138/cpp.2026-004'],
 ['Census divisions by Indigenous population share','correlation with rank-rank slope (Canada)',0.520,'SE 0.051; higher share = lower relative mobility; US analogue 0.022','Connolly, Corak & Haeck (2019) JOLE Table 7',J],
 ['Census divisions by Indigenous population share','correlation with absolute rank mobility (Canada)',-0.567,'SE 0.049','Connolly, Corak & Haeck (2019) JOLE Table 7',J],
]
W('canada_mobility_by_group.csv',['group','measure','value','note','source','url'],G)
# 6. Housing transmission
HU='https://www150.statcan.gc.ca/n1/pub/46-28-0001/2023001/article/00004-eng.htm'; HS='Mirdamadi & Khalid (2023) StatCan Housing Statistics in Canada 46-28-0001'
Hh=[['born 1990-1999','None',8.1],['born 1990-1999','One',14.7],['born 1990-1999','Two',22.0],['born 1990-1999','Three or more',27.8],
    ['born 1990-1999 (oldest subgroup, Chart 4)','None',15.7],['born 1990-1999 (oldest subgroup, Chart 4)','One',28.5],['born 1990-1999 (oldest subgroup, Chart 4)','Two',36.5],['born 1990-1999 (oldest subgroup, Chart 4)','Three or more',42.3]]
W('canada_homeownership_by_parent_properties.csv',['children','parent_properties_owned','child_homeownership_rate_pct','reference_year','coverage','source','url'],
  [r+['2021','all provinces/territories except Quebec and Saskatchewan',HS+(' Chart 3' if 'Chart 4' not in r[0] else ' Chart 4'),HU] for r in Hh])
print('done')
