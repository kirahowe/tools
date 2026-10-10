# Data preparation

The CSVs in `src/economics/wealth_distribution/canada/data/` were prepared from official
downloads (Statistics Canada full-table CSVs, OECD SDMX extracts, PBO and Finance Canada
PDFs, CRA tables, the IMF Fiscal Monitor appendix, Forbes list JSON) by the Python scripts
in this folder. Each topic folder has:

- `NOTES.md`: every figure with its source URL, table ID or vector ID, page reference and
  retrieval date (2026-10-10), plus caveats and items still to verify.
- `*.py`: the scripts that turned raw downloads into the tidy CSVs.

The raw downloads (about 9 GB, mostly full Statistics Canada tables) are not committed.
The scripts expect them in a `raw/` folder next to the script folder; the source URLs are
in each `NOTES.md`. Statistics Canada tables download as
`https://www150.statcan.gc.ca/n1/tbl/csv/<8-digit-table-id>-eng.zip`.

Run scripts with `python3 -I <script>.py` from the topic folder.

| Folder | Covers | Used in |
| --- | --- | --- |
| `tax/` | Federal revenue, CRA T1 statistics, high-income filers, tax expenditures, top rates, DHEA | Who Pays Tax |
| `wealth/` | DHEA quintiles, SFS deciles, PBO top tail, WID, Forbes, national balance sheet, IMF debt | Wealth Concentration, Wealth Tax |
| `labour/` | OECD house prices, homeownership, household debt, incomes, productivity and pay, top rates | Labour Hours |
| `investment/` | Corporate tax rates, business investment, corporate cash, productivity gap, wealth-tax evidence | The Investment Question, Economic Growth |
| `context/` | Fiscal history, IMF G7 debt, PBO costings, polling, politics, social data | Wealth Tax, What Makes Canada Different, How to Get a Wealth Tax |

Note: `imf_g7_debt_fm2026apr.csv` was re-extracted from the IMF Fiscal Monitor (April 2026)
Statistical Appendix Tables A7–A8 after the original script mis-parsed some rows.
