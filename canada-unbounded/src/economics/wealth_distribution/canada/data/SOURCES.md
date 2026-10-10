# Data sources

All files retrieved or derived on 2026-10-10. Full provenance (URLs, table and vector IDs,
page numbers, caveats) is in `data-prep/<topic>/NOTES.md`; the scripts that built each file
are alongside. "Supporting" files back figures quoted in the text rather than a chart.

## Taxes (Who Pays Tax)
| File | Source |
| --- | --- |
| `federal_revenue_by_source.csv` | Finance Canada, Annual Financial Report 2024–25, Table 4; Fiscal Monitor March 2026 |
| `cra_t1_2023_by_income_class.csv` | CRA, Individual Income Tax Return Statistics, 2023 tax year, Table 2 |
| `high_income_filers_11100055_extract.csv` | Statistics Canada Table 11-10-0055-01 (extract, 2019–2023) |
| `tax_expenditures_2026_report.csv` | Finance Canada, Report on Federal Tax Expenditures 2026, Part 2 |
| `top_marginal_rates_2026.csv` | Computed from CRA 2026 federal/provincial brackets and T4127 (January 2026) |
| `tax_by_income_quintile_dhea.csv` | Supporting. Statistics Canada Tables 36-10-0587-01 and 36-10-0660-01 (computed) |
| `wealth_by_wealth_quintile_dhea.csv` | Supporting. Statistics Canada Table 36-10-0660-01 (computed annual averages) |

## Wealth (Wealth Concentration, Wealth Tax)
| File | Source |
| --- | --- |
| `pbo_hfd_2023_public.csv` | PBO public High-net-worth Family Database 2023 (sheet "HFD 2023 EN", converted from XLSX) |
| `dhea_networth_by_wealth_quintile_quarterly.csv` | Statistics Canada Table 36-10-0660-01 |
| `sfs_networth_by_decile.csv` | Statistics Canada Table 11-10-0075-01 (SFS), 2023 dollars |
| `sfs_networth_by_quintile_canada_provinces.csv`, `sfs_networth_by_age_familytype_geo.csv` | Supporting. Statistics Canada SFS tables 11-10-0049/0057-01 |
| `statcan_sfs_median_networth_by_province_11-10-0016-01.csv` | Statistics Canada Table 11-10-0016-01 |
| `pbo_top_tail_published.csv`, `pbo_top1_share_series.csv` | Supporting. PBO top-tail reports (2020, 2021, 2025), published tables |
| `davies_dimatteo_wp2018_wealth_shares.csv` | Davies & Di Matteo, EPRI Working Paper 2018-1, Table 1 |
| `forbes_2026_canadian_billionaires.csv`, `forbes_canadian_billionaires_by_year.csv` | Forbes billionaires lists (forbes.com API JSON), Canadian citizens |
| `nbsa_per_adult_selected.csv` | Supporting. Statistics Canada Tables 36-10-0580-01 and 17-10-0005-01 (computed per adult) |
| `wid_canada_us_wealth_income_shares.csv` | Supporting. World Inequality Database (low-quality flag for Canadian wealth) |
| `oecd_wdd_wealth_shares.csv` | Supporting. OECD Wealth Distribution Database |

## Public finances and politics (Wealth Tax, How to Get a Wealth Tax)
| File | Source |
| --- | --- |
| `fed_fiscal_history_frt2025.csv` | Finance Canada, Fiscal Reference Tables 2025, Tables 1–2 |
| `imf_g7_debt_fm2026apr.csv` | IMF Fiscal Monitor April 2026, Statistical Appendix Tables A7–A8 |
| `polling_wealth_tax_canada.csv` | Pollster releases (Abacus Data, Angus Reid); the Léger row is unverified and not used |
| `wealth_tax_proposals_canada.csv` | Supporting. PBO costings and party documents |

## Housing, work and pay (Labour Hours)
| File | Source |
| --- | --- |
| `oecd_house_price_to_income_can_usa_annual.csv` | OECD Analytical House Price Database (rebased 1970 = 100) |
| `homeownership_rate_canada_census.csv` | Statistics Canada Census (The Daily, 2022-09-21; Canada Year Book 1974) |
| `household_debt_oecd_can_usa_annual.csv` | OECD National Accounts |
| `income_canada_cis_annual.csv` | Statistics Canada Tables 11-10-0190-01 and 11-10-0239-01, 2024 dollars |
| `productivity_pay_index_long.csv` | Statistics Canada Tables 36-10-0305-01 (archived), 36-10-0208-01, 36-10-0206-01, 14-10-0064-01, 18-10-0005-01 |
| `top_marginal_tax_rate_canada_vs_us_1920_2025.csv` | Saez & Veall data file (to 1999); OECD Tax Database (2000+) |
| `living_wage_vs_minimum_wage_cities.csv` | Supporting. Living wage networks (some via news reports); values are hard-coded in the notebook |
| `cpi_components_canada_annual.csv` | Supporting. Statistics Canada Table 18-10-0005-01 |
| `top_income_shares_canada_*.csv` | Supporting. Saez & Veall; Statistics Canada Table 11-10-0055-01 |
| `real_gdp_per_capita_canada_long_run.csv` | Maddison Project Database 2023; Statistics Canada |

## Investment and productivity (The Investment Question, Economic Growth)
| File | Source |
| --- | --- |
| `A1_corporate_tax_rates_oecd.csv` | OECD Corporate Tax Statistics (combined statutory CIT rate) |
| `A2_business_nonres_investment_share_gdp_annual.csv` | Statistics Canada Table 36-10-0104-01 |
| `A3_nfc_currency_deposits_pct_gdp.csv` | Statistics Canada Tables 36-10-0580-01 and 36-10-0104-01 |
| `A4_corporate_surplus_vs_business_investment.csv` | Statistics Canada Tables 36-10-0103-01 and 36-10-0104-01 |
| `B1_labour_productivity_canada_us_oecd.csv` | OECD Productivity Database |
| `B2_business_investment_per_worker_canada_us.csv` | OECD Economic Outlook 119 database (use the 2014 = 100 index) |
| `F1_business_sector_productivity_growth_by_period.csv` | Statistics Canada Table 36-10-0208-01 |
| `F2_marginal_tax_rates_canada_1920_2000_saez_veall.csv` | Supporting. Saez & Veall (2003) NBER WP 9607, Appendix Table E1 |

## What makes Canada different
| File | Source |
| --- | --- |
| `oecd_idd_gini_g7.csv` | OECD Income Distribution Database |
| `statcan_gini_11-10-0134-01.csv` | Statistics Canada Table 11-10-0134-01 |
| `statcan_poverty_rates_mbm_11-10-0135-01.csv` | Statistics Canada Table 11-10-0135-01 |
| `statcan_emigration_17-10-0040-01.csv` | Statistics Canada Table 17-10-0040-01 (quarters summed) |
| `oecd_health_spending_can_usa.csv` | Supporting. OECD System of Health Accounts |

## Productivity and competition (Stable and Stuck)
| File | Source |
| --- | --- |
| `prod_G1_gdp_per_capita_gap_canada_us.csv` | OECD Productivity Database |
| `prod_A4_canada_us_lp_mfp_index_2000eq100.csv` | Statistics Canada 36-10-0208-01; US BLS (supporting) |
| `prod_A5_gu_willox_2023_table1_canada_us_decomposition.csv` | Gu & Willox (2023), *International Productivity Monitor* 45, Table 1 |
| `prod_B1_business_dynamics_entry_exit_33100164.csv` | Statistics Canada Table 33-10-0164-01 |
| `prod_B3_telecom_market_shares_crtc.csv`, `prod_B4_concentration_headlines.csv` | Supporting. CRTC open data; IMF; Bank of Canada; Competition Bureau |
| `prod_D4_residential_vs_business_investment_canada_vs_us.csv` | Statistics Canada 36-10-0108-01 / 36-10-0104-01; US BEA via FRED |
| `prod_E1_rd_intensity_oecd_msti.csv` | OECD Main Science and Technology Indicators |
| `forbes_g2000_ca_us_2011_2026.csv` | Forbes Global 2000 (2011–2026), Canadian and US companies |
| `top_company_founding_years.csv` | Compiled by hand; see `data-prep/README.md` |
| `forbes_2026_ca_us_selfmade_industry.csv` | Forbes 2026 billionaires, industry shares (computed) |

## Old Money
| File | Source |
| --- | --- |
| `dyn_durham_upper_canada_land_grants_1839.csv` | Lord Durham's Report (1839), Lucas ed. vol. 2 pp. 222–223 |
| `dyn_parliament_kinship_counts_gagnon2011.csv` | Gagnon (2011), *Canadian Parliamentary Review* 34(3) |
| `dyn_political_dynasties_canada.csv`, `dyn_family_control_performance_studies.csv`, `dyn_forbes_2026_canada_inherited_vs_selfmade.csv` | Supporting. Legislature records; academic studies; Forbes 2026 |

## Land
| File | Source |
| --- | --- |
| `land_land_share_summary.csv` | Statistics Canada 36-10-0580-01 and 36-10-0104-01 (computed shares) |
| `land_canada_land_tax_history.csv` | Haig (1915); England (2018) |
| `land_lvt_illustrative_scenarios.csv` | Computed from Statistics Canada land values |
| `land_vacancy_taxes.csv` | Supporting. BC Ministry of Finance, City of Vancouver, City of Toronto, PBO, CRA (some via press reports) |

## Scorecard
| File | Source |
| --- | --- |
| `sc_policy_inventory.csv`, `sc_targets_vs_actuals.csv` | LEGISinfo, Budget 2025, Spring Economic Update 2026, PBO, NATO (sources per row) |
| `sc_ind_*.csv` | Statistics Canada tables 36-10-0104, 36-10-0206, 14-10-0287, 18-10-0004, 34-10-0158, 34-10-0135, 17-10-0009, 17-10-0121, 12-10-0011; Bank of Canada |
| `sc_fiscal_projections_vs_results.csv`, `sc_defence_spending.csv` | Finance Canada; PBO; NATO |

## Talent flows
| File | Source |
| --- | --- |
| `tal_canada_talent_flows_2000_2025.csv` | Statistics Canada 17-10-0040-01, 17-10-0009-01, 17-10-0121-01 |
| `tal_oecd_permanent_inflows_per1000.csv` | OECD DF_MIG_INT_PER (standardised permanent inflows); World Bank population |
| `tal_skilled_emigration_indicators.csv` | Statistics Canada Portrait of Canadian emigration (2026), The Daily (2024-02-02); CLEF WP 74; Zhao, Drew & Murray (2000); Finnie (2006) |
| `tal_statcan_stem_grad_retention_choi_hou_2025.csv` | Choi & Hou (2025), Statistics Canada ESR 5(7), Table 1 |
| `tal_census2021_degree_by_immigrant_status.csv` | Statistics Canada Census 2021, 98-10-0446-01 |
| `tal_us_acs_canadian_born_education.csv` | US Census Bureau ACS 1-year PUMS 2014 and 2024 (author tabulation) |
| `tal_us_admin_flows_canadians.csv` | DHS OHSS Yearbook Table 3; USCIS H-1B FY2023 report |
| `tal_cihi_physician_international_migration.csv` | CIHI physician supply and migration data tables 2023–2024 |
| `tal_immigrant_entrepreneurship.csv` | Picot & Rollin (2019); Picot & Ostrovsky (2021); Green et al. (2016) |
| `tal_ai_talent_indicators.csv` | Stanford AI Index 2025, Chapter 4 |
| `tal_net_skill_balance_estimates.csv` | Zhao, Drew & Murray (2000); author's calculations (derivations per row) |

## Mobility
| File | Source |
| --- | --- |
| `mob_canada_us_headline_measures.csv`, `mob_cch2019_*.csv` | Connolly, Corak & Haeck (2019), JOLE 37(S2), Tables 2–5, 7 |
| `mob_canada_relative_mobility_by_cohort.csv`, `mob_canada_rank_slope_by_province_cohort.csv` | Connolly, Haeck & Lapierre (2021), Statistics Canada 11F0019M No. 458 |
| `mob_canada_mobility_by_province_corak2020.csv` | Corak (2020), Economic Journal 130(631), Tables 4–6 |
| `mob_absolute_mobility_canada_us.csv` | Ostrovsky (2017), Statistics Canada 11-626-X No. 073; Manduca et al. (2024) via Statistics Canada ESR; Chetty et al. (2017) |
| `mob_canada_mobility_by_group.csv` | Adnan, Zhang & Zheng (2023); Aydemir, Chen & Corak (2009); Chen, Ostrovsky & Piraino (2016); Corak & Heisz (1999); Corak (2013); Connolly & Haeck (2026) |
| `mob_oecd_broken_elevator_2018.csv` | OECD (2018), A Broken Social Elevator?, Figures 1.5 and 4.8 (StatLink files) |
| `mob_canada_homeownership_by_parent_properties.csv` | Statistics Canada Canadian Housing Statistics Program (2023) |
