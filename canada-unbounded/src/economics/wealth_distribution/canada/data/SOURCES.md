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
