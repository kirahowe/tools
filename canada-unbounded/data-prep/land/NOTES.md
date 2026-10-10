# Land value taxation and taxes on unproductive land: Canadian evidence base

Research notes for the wealth, productivity and growth data-essay series. Compiled 2026-10-10.

Conventions:
- **[official]**: government, statistical agency or PBO source, read directly.
- **[peer-reviewed]**: academic journal article.
- **[working paper]** or **[think tank]**: not peer-reviewed.
- **[press]**: news report, usually of an official figure that I could not read at source.
- **[advocacy]**: labelled context only.
- **[computed]**: my own arithmetic from the cited inputs. The script is named.

All CSVs are in this folder, and the scripts that built them are in `scripts/`.

---

## 1. How much of Canada's wealth is land

**Source:** Statistics Canada, National Balance Sheet Accounts, Table 36-10-0580-01, market value, quarterly from 1990Q1 to 2026Q2. Downloaded 2026-10-10 from https://www150.statcan.gc.ca/n1/tbl/csv/36100580-eng.zip; the table was last updated 2025-09-11 per the file date.

The table has the land split built in. The categories are "Land", "Land underlying dwellings", "Land underlying buildings and structures", and "Agricultural land", alongside "Dwellings" (the structures) and "Other buildings and structures". These are given for every sector: national (national wealth), households, HH+NPISH, corporations, and governments.

**GDP:** Table 36-10-0104-01, nominal, seasonally adjusted at annual rates. I average the four quarterly SAAR values to get annual or trailing-four-quarter GDP **[computed]**.

**Files:**
- `nbs_land_long.csv`: tidy long format with columns quarter, sector, category and value (CAD millions). Every quarter from 1990 to 2026Q2. Sectors: national, households, hh_npish, nonfin_corps, govt.
- `land_share_summary.csv`: Q4 (year-end) rows plus the latest quarter (2026Q2), by sector. It holds land, land under dwellings, structures, non-financial assets (NFA) and net worth, plus these ratios:
  - land share of NFA (%)
  - land under dwellings as a share of (land under dwellings + dwelling structures)
  - land share of net worth
  - land / GDP
  - residential land / GDP

Script: `scripts/nbs.py`.

### Key figures [official; ratios computed]

**National balance sheet (national wealth):**

| Period | Land, all ($B) | Land under dwellings ($B) | Dwelling structures ($B) | Land % of NFA | Land under dwellings as % of residential real estate | Land % of GDP |
|---|---|---|---|---|---|---|
| 1990Q4 | 856.8 | 642.0 | 574.3 | 31.8 | 52.8 | 123 |
| 2000Q4 | 1,122.1 | 810.4 | 874.8 | 25.5 | 48.1 | 101 (series low) |
| 2010Q4 | 2,414.7 | 1,776.0 | 1,781.8 | 31.5 | 49.9 | 145 |
| 2021Q4 (peak) | 7,123.2 | 5,776.6 | 3,583.6 | 42.4 | 61.7 | 281 |
| 2025Q4 | 6,369.5 | 4,522.6 | 4,575.3 | 36.5 | 49.7 | 196 |
| 2026Q2 (latest) | 6,557.6 | 4,622.6 | 4,647.2 | 35.7 | 49.9 | 197 |

- National land value peaked in 2022Q1 at $7.376T. The land share of NFA peaked in 2021Q4 at 42.4%; its low was 25.4% in 2001Q1.
- At 2025Q4, land was 33.3% of national net worth ($19.11T).
- **Households sector, 2025Q4:**
  - Land: $4.584T, which is 46.6% of household NFA ($9.84T) and 25.0% of household net worth.
  - Land under dwellings: $4.235T, against dwelling structures of $4.129T.
  - The household land share of NFA peaked at 57.7% in 2021Q4.
- **The story for the post:** almost all of the 2010–2021 run-up in residential real estate wealth was land, not buildings.
  - From 2010Q4 to 2021Q4, land under dwellings rose about $4.0T (+225%).
  - Dwelling structures rose about $1.8T (+101%) over the same period.
  - Since 2022 the structures value has kept rising (replacement cost) while land has fallen back.

### Methodology caveats (important for the post)
- In the NBS, land is not observed directly. StatCan's archived article "Measuring the stock of residential real estate" (Canadian Economic Observer, 13-605-X, 2013) says the NBSA values residential land with **a land-to-structure ratio applied to structure value**. Structures come from the perpetual inventory method (PIM). The same article says "the methodology for estimating land is dated and needs revision". https://www150.statcan.gc.ca/n1/pub/13-605-x/2013002/article/11782-eng.htm
- The 2021 comprehensive revision (Latest Developments in the Canadian Economic Accounts, 13-605-X 2021) added three new series: land underlying dwellings, land underlying buildings and structures, and agricultural land. It also revised land upward using "municipal property assessment files" and the Survey of Financial Security. https://www150.statcan.gc.ca/n1/pub/13-605-x/2021001/article/00006-eng.htm
- Land is therefore a modelled residual-type estimate. It is sensitive to the structure valuation, so treat quarter-to-quarter moves with caution.
- **Experimental land tables:** I did not find a StatCan table "36-10-0675" or any other table that splits land from structure outside the NBS. The zip download for 36-10-0675 failed, and searches found no such title. The closest experimental product is the Canadian Housing Statistics Program (CHSP) assessment-value tables, which cover property counts and assessment values. Some provinces' assessment rolls separate land from improvements (BC Assessment does), but I did not build a series from them. **Gap.**

---

## 2. Canadian history of land value taxation (the "single tax" era)

**Key primary source:** Robert Murray Haig (1915), *The Exemption of Improvements from Taxation in Canada and the United States*, report to the Committee on Taxation of the City of New York. Full text: https://archive.org/details/exemptionofimpro00haiguoft (local copy at raw/haig/haig.txt). Haig did fieldwork in summer 1914 in Winnipeg, Regina, Moose Jaw, Saskatoon, Edmonton, Leduc, Lloydminster, Ponoka, Calgary, Vancouver and Victoria.

The tidy timeline is in `canada_land_tax_history.csv`.

### Verified points (Haig 1915 unless noted)

**Vancouver:**
- The 1891 charter already allowed separate land/improvement assessment and exemption of improvements.
- 1895: buildings assessed at 50%.
- 1906: buildings at 25%, explicitly to discourage vacant lots ("the city contained too many vacant lots").
- 1910: improvements fully exempt, part of the platform of Mayor L.D. Taylor. The exemption was renewed every year through 1914.
- Haig notes that "there has never been in Vancouver a direct expression of popular sentiment on the question".
- The tax base grew in 1906 and 1910 despite the building exemptions, because of the land boom.

**Edmonton:** its 1904 city charter exempted improvements entirely. After the 1913 bust, the city assessor wrote on Mar 16 1915 that "Little more than half of the 1914 taxes have been paid, and the amount of the arrears is now somewhat alarming". The city faced its first tax sales (Haig pp. ~95–96).

**Calgary:**
- 1909: buildings assessed at 80%.
- 1911: a charter amendment set buildings at 50%, with a compulsory cut of at least 10 points a year to zero.
- 1912: buildings cut to 25%.
- 1913: council had a "change of heart". It froze buildings at 25% and had the legislature remove the compulsory phase-down.

**Winnipeg:** improvements were assessed at two-thirds of value under a charter amendment. The Manitoba legislature resisted further moves.

**Saskatchewan:** local option allowed a gradual reduction of the building tax. Moose Jaw went from 60% to 45% in 1913.

**Vacant-land penalties existed then too:** Lethbridge and smaller BC municipalities levied super-taxes on vacant land. SK, AB and BC levied provincial "wild land" taxes on speculative holdings. Calgary doubled frontage charges on vacant property (Haig, p.261).

**Haig's conclusions (pp.277–280):**
- Exemption "to some degree stimulates building and discourages speculation".
- But results "depend largely upon local conditions": "in some places the measures have been conspicuously unsuccessful".
- Transitions worked only while land values were rising strongly and evenly. Where the tax base contracted, the system was modified.
- In the Alberta towns, where the plan was imposed suddenly without rapid growth, results were "in some cases disastrous".
- The key political appeal was as "a weapon against the non-resident speculator".
- Quote for the post: "neither a plague nor a panacea".

### Later history and abandonment
- **England (2018) [peer-reviewed abstract]:** Vancouver "obtained international acclaim as a 'single-tax' city ... over several decades, the system was scaled back and ultimately ended in 1984". He explains the tax revolt with Olson's collective-action model: organised property owners beat tenants and prospective buyers. Citation: *American Journal of Economics and Sociology* 77(1):59–94, doi:10.1111/ajes.12218 (abstract via Crossref API).
- **Newcomer & Hutchinson (1932):** "Taxation of Land Values in Canada", *Journal of Political Economy* 40(3):366–378, doi:10.1086/254348. As quoted by progress.org [advocacy wiki, unverified], it says "Vancouver changed from total exemption to 25 per cent assessment in 1918 and to 50 per cent assessment in 1919". **I could not read the original; verify on JSTOR before publishing.** The same wiki cites England for 50% of the land rate 1919–69 and 75% 1969–84; also unverified.
- **IMF Staff Papers 1967, "The Taxation of Land Value"** (via search snippet; paywalled): by 1914, about two-thirds of BC municipalities (including Vancouver and Victoria), all of Alberta's, and about a quarter of Saskatchewan's had fully exempted improvements.
- **Mechanism of retreat (supported by Haig's contemporaneous evidence):** the 1913 collapse of the western land boom and WWI shrank land values. A land-only base then produced arrears and tax-sale land, and councils reintroduced taxes on improvements and businesses.
- **Gap:** exact abandonment dates for Edmonton, Calgary and Winnipeg (mostly 1915–1920s per secondary literature) are not verified from primary sources.
- Archibald Stalker's 1914 McGill thesis, *Taxation of Land Values in Western Canada* (escholarship.mcgill.ca), is another contemporary source that I did not read.

---

## 3. Current Canadian taxes on unused or underused property

All entries are in `vacancy_taxes.csv` (tidy long format: jurisdiction, tax, year, rate, base, metric, value, unit, source).

### 3a. Federal Underused Housing Tax (UHT), 2022–2024; repealed

**Design:**
- 1% a year on the value of residential property owned by (mainly) non-resident non-Canadians that is vacant or underused. It took effect on Jan 1, 2022.
- Many Canadian private corporations, partnerships and trusts also had to *file* even when exempt. This is the main source of compliance burden.

**Revenue: projections versus actuals**

| Source | Figure |
|---|---|
| Budget 2021 Ch.10 projection | "$700 million over four years, starting in 2022-23" ([official], https://www.budget.canada.ca/2021/report-rapport/p4-en.html) |
| PBO Legislative Costing Note, Apr 19 2024 | $131.7M in 2023-24; $693.9M over 2023-24 to 2027-28 ([official], https://pbo-dpb.ca/en/publications/LEG-2425-001-S--estimated-revenues-from-underused-housing-tax--estimation-recettes-tirees-taxe-logements-sous-utilises) |
| CRA via press, as of Mar 4 2024 | $49M assessed; 578,910 returns processed ([press], Investment Executive, Apr 19 2024) |
| Globe and Mail via Wealth Professional, Jan 7 2025 | $49M collected in fiscal 2023-24, covering the 2022 and 2023 tax years ([press]) |

- The PBO note excludes administration costs, which "could significantly reduce the net revenues".

**Costs:**
- CRA spent $59M administering the UHT by Feb 14 2024, including hiring 350 people ([press]).
- About $2.5B of penalties and interest were waived through more than 531,000 waivers on late 2022 returns ([press]; the CRA confirmed the figure).
- Penalty minimums were cut from $5,000/$10,000 to $1,000/$2,000.

**Repeal:**
- Budget 2025 (Ch.5) proposed eliminating the UHT as of the 2025 calendar year. Its stated reasons: "To simplify Canada's tax system and reduce compliance costs for taxpayers and administrative costs for government, and given other efforts such as the federal foreign buyer ban and municipal and provincial vacant home taxes". The chapter also refers to measures "inefficient, costly to administer".
- The fiscal cost is $30M a year, or $150M over 2025-26 to 2029-30 (Table 1, Annex "Tax Measures: Supplementary Information") ([official], https://budget.canada.ca/2025/report-rapport/tm-mf-en.html).
- **Bill C-15** (Budget 2025 Implementation Act, No. 1), Division 2 of Part 3, "end[s] the underused housing tax in respect of 2025 and future calendar years" and later repeals the Act. It received **Royal Assent on March 26, 2026** ([official], https://www.parl.ca/DocumentViewer/en/45-1/bill/C-15/royal-assent). Obligations for 2022–2024 remain.
- **Lesson for the post:** this is a tax whose compliance and administrative burden dwarfed its net yield. It was narrowly targeted (on non-residents), but it imposed filing obligations on hundreds of thousands of owners who owed nothing. Of finalised returns, about 1.7% had UHT owing per an earlier CRA figure (Investment Executive, "CRA has assessed $30M"; [press]).

### 3b. BC Speculation and Vacancy Tax (SVT), 2018–

**Design:**
- An annual tax on residential property in designated urban areas. Owners must declare each year.
- Exemptions include principal residence and tenanted at least 6 months.
- BC residents get a credit (raised from $2,000 to $4,000 in 2026).
- Coverage: 59 communities in 2024, after 13 were added that year.

**Rates** ([official], https://www2.gov.bc.ca/gov/content/taxes/speculation-vacancy-tax/how-tax-works/tax-rates):
- 2018: 0.5% for all.
- 2019–2025: 2% for foreign owners and untaxed worldwide earners (satellite families); 0.5% for Canadian citizens and PRs.
- **2026: 3% / 1%.** The rates page lists 4% / 1% for 2027; the Jan 2026 release only mentions 3% / 1%, so verify.

**Revenue by tax year:**

| Tax year | Revenue | Source |
|---|---|---|
| 2018 | $65M | 2022 statutory review ([official], https://news.gov.bc.ca/files/Speculation_Vacancy_Tax_Review.pdf) |
| 2019 | $86M | same |
| 2020 | $81M | same |
| 2021 | >$78M | release 2022FIN0074 |
| 2022 | >$81M | release 2023FIN0070 |
| 2023 | $75.2M | 2024 technical briefing |
| 2024 | $79.6M | 2024 technical briefing (Dec 8 2025; data as of Oct 1 2025), https://news.gov.bc.ca/files/SVT_Technical%20Briefing_2024_TaxYear.pdf |

- Cumulative revenue is about $550M (release 2025FIN0047, Dec 18 2025).
- Forecast for 2025/26: $105M.
- **2024 revenue by owner type:** untaxed worldwide earners $23.6M, foreign owners $16.2M, BC residents $15.3M, other Canadians $12.7M.

**Non-exempt (taxed) properties:** 8,920 (2018), 5,651 (2019), 6,556 (2020), 6,477 (2023), 8,209 (2024). The 2024 count is out of 1.32M declared properties.
- Of the 6,477 properties taxed in 2023, by 2024:
  - 4,103 remained taxed
  - 560 were sold
  - 520 became principal residences
  - 730 were rented

**Effects claimed by government:** the SVT "helped add approximately 20,000 condo units to the long-term rental market in Metro Vancouver" (2022 review).
- **Caveat from the review itself:** this rests on CMHC's count of strata condos in the rental pool. It rose by about 11,000 units in 2018–19 and 7,137 in 2019–20, but that count includes *new construction*. CMHC attributes the jump only "at least in part" to the 2018 policies.
- The review's rent effect ("rents being 5 percent lower") is a back-of-envelope calculation using an assumed elasticity, not an estimate.

**Independent evaluations:** I found no peer-reviewed causal study of the SVT specifically. The SVT and the Vancouver EHT were introduced together, so their effects are hard to separate.

### 3c. City of Vancouver Empty Homes Tax (EHT), 2017–

**Design:** applies to properties vacant more than 6 months that are not a principal residence or tenanted.

**Rates:**
- 1% for reference years 2017–2019.
- 1.25% for 2020.
- 3% from 2021 on.

Sources: Han, Stacey & Chen (2023) [working paper]; the City of Toronto staff report (2024) [official] confirms 1.25%→3%. In April 2023 council raised the rate to 5% for 2023, then reverted to 3% on May 10, 2023 (council.vancouver.ca/20230510/documents/cfsc2.pdf; via search). A 2025 council motion to go to 5% for 2026 had unknown status.

**Vacant properties:**
- ~2,500 in 2017 (approximate; consistent with the City's "54% decline 2017–2022").
- 1,755 in 2020.
- 1,398 in 2021.
- 1,156 in 2022.
- ~1,073 in 2023 (implied).
- **979 in 2024**: a declared vacancy rate of 0.49%, with 60,831 tenanted properties, the most since inception.

Sources: City annual reports via press (Storeys, ConstructConnect, Daily Hive Dec 13 2025). vancouver.ca was behind Cloudflare and could not be read directly.

**Revenue:**
- $26M (2021 collection) and $67M (2022 collection, after the rate tripled), per the City of Toronto staff report, Sept 17 2024 [official].
- Cumulative net to housing: more than $194M since 2017 [press].

**Evaluations:**
- **Caracciolo & Miglino (2024), C.D. Howe E-Brief [think tank]:**
  - Method: difference-in-differences across Boundary Road (Vancouver vs Burnaby), 2011/2016/2021 Census dissemination areas.
  - Vacancy rate fell about 1.5 percentage points, roughly 21% off a 7% base. That is about 5,355 fewer vacant units citywide over 2016–21.
  - No significant effect on rents or new construction.
  - Critics note that Burnaby shares the same rental market, which biases the rent comparison.
  - https://cdhowe.org/publication/ripple-effects-impact-empty-homes-tax-housing-market/
- **Han, Stacey & Chen (2023), "Frictional and Speculative Vacancies: The Effects of an Empty Homes Tax" [working paper]:**
  - Model and Vancouver evidence: the tax cuts speculative vacancies and improves rental availability.
  - But it can distort "frictional" vacancies in the for-sale market, raising house prices and lowering home-ownership.
  - https://abfer.org/media/abfer-events-2023/annual-conference/papers-realestate/AC23P6023-Frictional-and-Speculative-Vacancies-The-Effects-of-an-Empty-Homes-Tax.pdf

### 3d. City of Toronto Vacant Home Tax (VHT), 2022–

All figures from the City staff report "Redesigning the Vacant Home Tax Program", Sept 17 2024 [official], https://www.toronto.ca/legdocs/mmis/2024/ex/bgrd/backgroundfile-248825.pdf, unless marked.

**Rates:** 1% of CVA for the 2022 and 2023 taxation years; **3% from the 2024 taxation year**.

**Billed revenue:**
- 2022: $56.5M, from 10,180 accounts (1,936 declared and 8,244 deemed vacant; 1.25% of accounts).
- 2023: $50.6M, from 8,695 accounts (1.07%).
- 2024: about $103M projected [press, CP24, Jan 27 2026]. The budget target was $50M.

**Administration problems:**
- Every owner had to declare. Non-declarers were "deemed vacant", and most "vacant" accounts were deemed, not declared.
- For the 2023 taxation year (declared in early 2024), the City received **169,149 Notices of Complaint**, against 38,003 for 2022.
- Press reported that about 167,000 homes were initially deemed vacant in the 2024 cycle, about 20% of residential properties, and that about 108,000 charges were reversed [press, CBC; not read at source].
- The Sept 2024 redesign added a 311 call centre and a communications campaign. It cost about $5.8M a year, fully offset from VHT revenue.

**Later counts** [press, CP24 Jan 2026]: 6,944 (2022), 5,077 (2023), 5,989 (2024) homes vacant. These differ from the Aug 2024 figures because of complaint and audit adjustments.
- The City "cannot confirm whether residential properties that were identified as vacant have been rented".
- The City estimates the vacancy rate at about 1–1.2%.

### 3e. Ontario Non-Resident Speculation Tax; federal foreign-buyer ban (brief)

- **NRST** (a transfer tax on purchases by foreign nationals and entities) [official]:
  - 15% from Apr 21 2017, Greater Golden Horseshoe only.
  - 20% province-wide from Mar 30 2022.
  - **25% from Oct 25 2022.**
  - Sources: https://www.ontario.ca/document/non-resident-speculation-tax and the transitional-provisions page.
  - BC has a parallel Additional Property Transfer Tax, not researched here.
- **Prohibition on the Purchase of Residential Property by Non-Canadians Act:** in force Jan 1 2023, and extended in Feb 2024 to at least Jan 1 2027 [secondary, law-firm summaries]. I found no further extension; verify close to publication.

### 3f. Vacant land and vacant commercial property: Canada historically *subsidised* vacancy

- In Ontario, from 1998 the province required **rebates or discounts for vacancy**: 30% for vacant commercial and 35% for vacant industrial space or land (the "vacant unit rebate" and the "vacant/excess land subclass").
- From 2017 municipalities could reduce or eliminate them. They were eliminated province-wide effective for 2019 (O. Reg. 490/18 and 491/18) [municipal staff reports; e.g., Puslinch FIN-2018-021].
- This is a strong example of property tax design rewarding unproductive land.
- In current Ontario rate schedules, vacant land is taxed at the commercial or industrial rate, not at a penalty rate. For example, Peel 2025 commercial general is 2.22–2.30%.
- **Gap:** no current Canadian municipality levies a dedicated *surcharge* on vacant developable land that I could verify. One partial exception: BC's SVT had an "exemption for land without residence" meant to give owners of vacant land time "to either begin developing the land or to sell it", and it **expired after December 31, 2019** (2022 SVT review, p.~13). Vacant residential-class land in SVT areas is therefore taxable under the SVT, a de facto vacant-lot tax at 0.5–3%.

---

## 4. Theory and evidence on LVT

### Efficiency case
- **OECD, Johansson et al. (2008), "Tax and Economic Growth", Economics Dept WP 620** [official/working paper, read]: "Corporate taxes are found to be most harmful for growth, followed by personal income taxes, and then consumption taxes. Recurrent taxes on immovable property appear to have the least impact." It recommends shifting revenue toward them, but notes they are "particularly unpopular" and sub-national. This underlies OECD (2010) *Tax Policy Reform and Economic Growth*. Copy: https://olis.oregonlegislature.gov/liz/2015R1/Downloads/CommitteeMeetingDocument/52428
- **OECD (2022), *Housing Taxation in OECD Countries*, Tax Policy Studies No. 29** (brochure read: https://www.oecd.org/tax/tax-policy/brochure-housing-taxation-in-oecd-countries.pdf):
  - It recommends "Strengthen[ing] the role of recurrent taxes on immovable property", based on regularly updated values, while reducing transaction taxes.
  - It flags that recurrent taxes "may raise liquidity concerns for low-income households, including retirees", and suggests tax deferral.
  - It says that "analyses of existing vacant home taxes suggest that some taxes have been successful in increasing housing supply, while others have had a limited impact".
  - The full report discusses split-rate taxes and valuation problems (not read; OECD site returned 403).
- **IMF, Norregaard (2013) WP/13/129, "Taxing Immovable Property: Revenue Potential and Implementation Challenges":** "economists continue to emphasize the virtues of the property tax owing to its relatively low efficiency costs, benign impact on growth, and high score on fairness... generally considered to be underutilized".
- **Mirrlees Review, *Tax by Design* (IFS 2011), Ch.16** (read):
  - "The economic case for taxing land itself is very strong ... Taxing land ownership is equivalent to taxing an economic rent".
  - "The economic case for a land value tax is simple, and almost undeniable."
  - It quotes Vickrey on the property tax as "one of the worst taxes ... and one of the best taxes".
  - Practical obstacles: valuing land separately from structures when land-only transactions are thin, and windfall losses to current owners through capitalisation.
  - It recommends replacing business rates with LVT on commercial and agricultural land first.
  - https://ifs.org.uk/sites/default/files/output_url_files/taxbydesign.pdf

### Empirical evidence on split-rate and land taxes
- Summary from **Dye & England (2010)**, *Assessing the Theory and Practice of Land Value Taxation*, Lincoln Institute Policy Focus Report [academic], https://www.lincolninst.edu/app/uploads/legacy-files/pubfiles/assessing-theory-practice-land-value-taxation-full_0.pdf
- **Pennsylvania:**
  - Pittsburgh and Scranton have had two-rate taxes since 1913. Pittsburgh raised its land-to-building ratio to 3.9 times in the home-rule era.
  - In 2008, 16 PA communities still used two-rate taxes, with land:building ratios from 1.66:1 to 30:1.
  - Pittsburgh rescinded its two-rate tax in 2001 after a botched reassessment. Bourassa: LVT "was the scapegoat for infrequent and inaccurate assessments and clumsy rate-setting procedures".
- **Oates & Schwab (1997)**, "The Impact of Urban Land Taxation: The Pittsburgh Experience", *National Tax Journal* 50(1):1–21 [peer-reviewed]:
  - Pittsburgh raised its land/building rate ratio from 2:1 to 5:1 in 1979–80.
  - Building permits then rose about 70%, while 14 comparison cities mostly declined.
  - The authors caution that the result is "consistent with a tax effect but not conclusive", because of a simultaneous development push.
  - Their main argument for LVT is that it raises revenue with "no damaging side effects", not that it stimulates growth.
- **Plassmann & Tideman (2000)**, *Journal of Urban Economics* 47(2):216–247 [peer-reviewed]: split-rate taxation raises residential construction by about 3–4% (as summarised by Dye & England).
- **Banzhaf & Lavery (2010)**, "Can the land tax help curb urban sprawl? Evidence from growth patterns in Pennsylvania", *Journal of Urban Economics* 67(2):169–179 [peer-reviewed]: split-rate taxation increases the *number* of housing units (density) rather than unit size. Specific magnitudes not verified.
- **Bourassa (1990), Mathis & Zech (1983):** mixed or null results (per Dye & England).
- **Lusht (1992):** more development in Melbourne municipalities that used site-value rating, with possible selection bias.
- **Australia** (per Dye & England):
  - State land taxes since the late 1800s.
  - A federal land tax from 1910 to 1952 aimed at breaking up large estates; it was repealed partly because it failed to do so.
  - **ACT reform:** a 20-year program since 2012 to replace conveyance (stamp) duty and insurance duty with general rates based on (averaged) unimproved land value. A review found revenue forecast error fell from 7.9% to 2.6% (ACT Treasury, https://www.treasury.act.gov.au/__data/assets/pdf_file/0004/1370344/ACT-Tax-Reform-Program.pdf; via search).
- **Denmark:** a national system of land valuation and municipal land tax (grundskyld), cited in the Mirrlees Review and the Barker review as evidence that land valuation is feasible. Rates not compiled here.
- **South Africa** moved *away* from site-value rating in 2001–04. Reasons: the political desire to tax improvement wealth, national uniformity, and difficulty getting credible land sales data in built-up areas (Franzsen & McCluskey, via Dye & England).

### Critiques and design issues
1. **Valuation:** land must be valued separately from buildings, and land-only sales are rare in built-up areas (Mirrlees; South Africa).
   - Canadian assessors (BC Assessment, MPAC) already report land and improvement components, but with uneven accuracy.
   - Ontario CVA has used a **Jan 1 2016 valuation date** since reassessment was postponed in 2020. This is exactly the OECD's "outdated values" problem. That is my understanding but I did not verify it from source; confirm the current MPAC status.
2. **Liquidity:** cash-poor, asset-rich owners such as seniors face bills unrelated to income (OECD 2022).
   - The standard remedy is deferral. BC has a Property Tax Deferment Program for owners 55+, surviving spouses, people with disabilities, and families with children. Interest switched from simple to compound for taxes deferred from 2026 (https://www2.gov.bc.ca/gov/content/taxes/property-taxes/annual-property-tax/defer-taxes).
3. **Capitalisation and transition fairness:** a new LVT is capitalised into lower land prices, a one-off windfall loss to current owners (Mirrlees). Many current owners bought at prices that capitalised the *old* low tax.
   - Haig 1915: transitions were painless only when they absorbed *new*, unexpected land-value gains during booms.
   - Remedies: gradual phase-in (Calgary 1911, the ACT 20-year plan), revenue-neutral swaps (cut transfer taxes or building taxes), and deferral.
4. **Political economy:** owner-voters are organised (England 2018, Olson model). Calgary 1913 and Pittsburgh 2001 show reversals when assessments or rate-setting go wrong.
5. **Vacancy-tax-specific issues:**
   - Declaration and enforcement burden: Toronto's 169,149 complaints; the UHT's $2.5B in waived penalties.
   - Narrow bases (non-residents, vacancy) raise little: UHT about $49M; SVT about $80M a year; Vancouver EHT $26–67M a year.
   - Taxing vacancy targets *use*, not *land value*. These taxes do not hit under-developed land (surface parking, single detached homes on transit-adjacent lots, land-banked sites), which is the core Georgist target.
6. **Distribution:** in Canada, land wealth is concentrated among older and higher-wealth households. Whether an LVT is progressive depends on exemptions or credits; this needs SFS microdata (**gap**).

---

## 5. Canadian property taxes today

**File:** `property_tax_revenue.csv` (year, geography, sector, measure, value in CAD millions, % of GDP for Canada).
- Local governments: StatCan 10-10-0020-01 (CGFS, municipalities and other local public administrations). Canada = sum of provinces and territories [computed].
- All governments, and provincial+local: StatCan 10-10-0147-01 (consolidated CGFS).
- Script: `scripts/proptax.py`.

**Key figures [official; % of GDP computed]:**
- **Local government recurrent taxes on immovable property, 2024:** $66.2B, 2.13% of GDP. Total local revenue was $152.3B, so property tax is about 43% of local revenue. By comparison: $35.6B (2.15% of GDP) in 2008 and $56.0B in 2019.
- **All Canadian governments, recurrent taxes on immovable property, 2024:** $81.7B, 2.63% of GDP. This includes provincial school and education property taxes. Total taxes on property were $91.9B, or 2.96% of GDP, including land transfer taxes.
- By province, 2024 local property tax: ON $27.8B, QC $15.0B, AB $9.0B, BC $8.2B.

**Effective residential rates** (`residential_property_tax_rates_2025.csv`, built by `scripts/rates.py`):
- **Toronto 2025** residential total: **0.754087%** of CVA (city 0.592653% + education 0.153% + City Building Fund 0.008434%). 2026: 0.767311%. (toronto.ca)
- **Peel suburbs 2025** (total incl. region and education): Mississauga **1.033864%**, Brampton **1.200644%**, Caledon 0.935400% (Region of Peel Appendix 1).
  - So the core city taxes homes at roughly 25–35% lower rates than its suburbs.
  - The cause: Toronto's large high-value assessment base and its heavier reliance on non-residential classes. Toronto's 2025 commercial general rate is 2.275478%, about three times the residential rate.
- **BC 2025** (Schedule 704 representative house, variable-rate taxes divided by house value) [computed]:
  - Vancouver 0.31% on a $2.68M house
  - Burnaby 0.30%
  - Surrey 0.31%
  - West Vancouver 0.29%
  - Victoria 0.52%
  - Kelowna 0.45%
- Rates are lowest where land values are highest. Because land is a large share of value there, the tax per dollar of *land* is even lower.

**Property taxes fall on buildings and land alike (anti-improvement bias):**
- Every Canadian municipal property tax applies one rate to total assessed value, land plus improvements.
- Nationally in 2025Q4, about 50% of residential real estate value is structures (NBS: $4.58T structures vs $4.52T land). Roughly half the residential property tax base is therefore a tax on buildings, which discourages construction, renovation and densification at the margin.
- The share varies widely: structures are a small share on high-value Vancouver and Toronto lots and a large share in rural areas.
- Multi-residential and commercial classes face higher rates, for example Peel multi-residential 1.27–1.94%. That penalises rental and business structures further.
- **Principal residence exemption:** $7.6B federal cost in 2023 (figure supplied by the author; source to be cited from Finance Canada's *Report on Federal Tax Expenditures*; not re-verified here).

---

## 6. Back-of-envelope: what could a land tax raise? (ILLUSTRATIVE)

**File:** `lvt_illustrative_scenarios.csv`. Script: `scripts/lvt_scenarios.py`.

**Inputs:**
- NBS 2025Q4 land underlying dwellings: $4.523T (all sectors) and $4.235T (households).
- All land: $6.369T.
- 2025 GDP: $3.245T [computed].
- 2021 Census: 14,978,941 private dwellings occupied by usual residents (16,284,235 total private dwellings).

| Scenario | Revenue/yr | % GDP |
|---|---|---|
| A. 1% on all residential land, static | $45.2B | 1.39% |
| B. 1% on household-sector residential land | $42.4B | 1.31% |
| C. 1% with $50k / $100k / $200k land exemption per occupied dwelling (**lower bounds**) | $37.7B / $30.2B / $15.3B | 1.16 / 0.93 / 0.47% |
| D. 1% after capitalisation (land value falls by V·t/(r+t); r = 3/4/5%) | $33.9B / $36.2B / $37.7B | ~1.05–1.16% |
| E. 1% on all land incl. commercial/industrial/agricultural | $63.7B | 1.96% |

Notes on reading the table:
- **Scenario C is a lower bound.** It assumes every dwelling's land value is at least the exemption. Many condos and rural homes are below it, so actual revenue lies between C and A.
- **Scenario D:** land values would fall 17–25% on announcement. This is the capitalisation and windfall-loss issue in §4.
- **For scale:** a 1% residential land tax (about $30–45B) would be roughly half to two-thirds of all current local property tax revenue ($66.2B in 2024). It is about 400–900 times the UHT's realised yield, and roughly 500 times the BC SVT.
- **Caveats:**
  - The NBS land is a modelled estimate (§1).
  - The scenarios are not net of existing property taxes, which already partly tax land.
  - There are no behavioural responses beyond capitalisation. A revenue-neutral swap (cutting the building portion of property taxes) is a different and arguably more relevant scenario.
  - There are no distributional results.

**Published Canadian estimates:** no PBO, Finance Canada or peer-reviewed national LVT costing was found.

Advocacy context only:
- Generation Squeeze proposed a surtax on homes over $1M (0.2% rising to 1%). It estimated $4.54B a year, or $5.83B at a 0.5% starting rate; the work was CMHC Solutions Labs-funded (https://www.gensqueeze.ca/price_on_housing_inequity).
- Common Wealth Canada [advocacy] claims "$362.5 billion/year" could be raised from an LVT, about 17% of GDP. This implies capturing almost all land rent, not a 1% tax; treat with heavy caution.
- The CCPA estimated a BC progressive tax on high-value holdings at $1.17B a year.

---

## Gaps and items to verify before publication
- The StatCan experimental land-value table ("36-10-0675?") was not found. Check with StatCan whether a CHSP land/improvement split exists.
- The Vancouver post-1918 percentages (Newcomer & Hutchinson 1932, and the England 2018 full text) were read only through an advocacy wiki. Read the originals.
- Abandonment dates for Edmonton, Calgary, Winnipeg and the Saskatchewan towns are not verified.
- The vancouver.ca primary PDFs (EHT annual reports) were blocked by Cloudflare. EHT counts and revenue are from press, city figures quoted by Toronto staff, and search snippets.
- UHT realised revenue for 2024-25 (Public Accounts) was not found; only the press figure of $49M as of FY2023-24 is available.
- No peer-reviewed causal evaluation of the BC SVT was found. The EHT evidence is from a think tank (C.D. Howe e-brief) and a working paper.
- The OECD 2022 full report's split-rate/LVT discussion was not read (403).
- The federal foreign-buyer ban beyond 2027 is unverified.
- The PRE $7.6B figure was not re-verified (supplied by the author).

## File list
- `notes.md`: this file.
- `nbs_land_long.csv`: StatCan 36-10-0580 land, structures and NFA by sector and quarter, 1990Q1–2026Q2.
- `land_share_summary.csv`: year-end (Q4) and latest-quarter land shares and land/GDP by sector.
- `property_tax_revenue.csv`: local and consolidated government property tax revenue by province, 2008–2024 (10-10-0020, 10-10-0147).
- `residential_property_tax_rates_2025.csv`: Toronto and Peel statutory rates, plus BC effective rates on a representative house (all BC municipalities).
- `vacancy_taxes.csv`: UHT, BC SVT, Vancouver EHT, Toronto VHT, Ontario NRST, the foreign-buyer ban, and Ontario vacancy rebates (rates, revenue, units, sources).
- `canada_land_tax_history.csv`: dated timeline of Western Canadian improvement exemptions, 1891–1984.
- `lvt_illustrative_scenarios.csv`: illustrative 1% land tax scenarios.
- `scripts/`: nbs.py, proptax.py, rates.py, lvt_scenarios.py.
- `raw/`: StatCan zips (36100580, 36100104, 10100147, 10100020), the Haig 1915 text, the Lincoln PDF, BC SVT PDFs, the Toronto staff report, Budget 2021/2025 HTML, OECD PDFs, the Mirrlees PDF, the Han et al. PDF, and BC Schedule 704.
