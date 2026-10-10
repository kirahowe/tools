# Unbounded North

A Canadian adaptation of Timothy Pratley's
[*Unbounded*](https://clojurecivitas.org/economics/wealth_distribution/unbounded/u0_introduction.html)
series on wealth distribution, taxation and growth, using Canadian data from Statistics
Canada, the Parliamentary Budget Officer, Finance Canada, the CRA, the Bank of Canada, the
OECD and the IMF.

Built with the same stack as the original: [Clay](https://scicloj.github.io/clay/) notebooks,
[Plotje](https://github.com/scicloj/plotje) charts and [Tablecloth](https://scicloj.github.io/tablecloth/)
data processing, rendered with [Quarto](https://quarto.org/).

## Posts

| # | Notebook | Topic |
| --- | --- | --- |
| 0 | `c00_introduction.clj` | Introduction and central claims |
| 1 | `c01_who_pays_tax.clj` | Federal revenue, capital gains, tax preferences, no inheritance tax |
| 2 | `c02_wealth_tax.clj` | PBO-costed proposals, who would pay, debt charges, objections |
| 3 | `c03_labour_hours.clj` | House prices vs income, homeownership, debt, living wage, productivity and pay |
| 4 | `c04_interactive_factors.clj` | Causal loop diagram of the Canadian system |
| 5 | `c05_wealth_concentration.clj` | The full distribution (SFS + PBO + Forbes), quintile trends, long run |
| 6 | `c06_productivity.clj` | Productivity, competition, corporate churn (animated top-10 companies, Canada vs US) |
| 7 | `c07_old_money.clj` | Colonial land grants, family holding companies, heir control, dual-class shares, political dynasties |
| 8 | `c08_economic_growth.clj` | Circulation, participation, "the Canadian disease" |
| 9 | `c09_investment_question.clj` | Would a wealth tax kill investment? Canada's tax-cut experiment and the evidence |
| 10 | `c10_land.clj` | Land as wealth, Western Canada's land-tax history, vacancy taxes, land value tax |
| 11 | `c11_canadian_differences.clj` | Mobility, public programs, provinces, Indigenous gaps, concentration |
| 12 | `c12_talent.clj` | Brain drain vs brain gain, immigrant founders, and how well Canada's mobility advantage holds up |
| 13 | `c13_scorecard.clj` | The Carney government's measures, targets vs actuals, early indicators |
| 14 | `c14_how_to_get_a_wealth_tax.clj` | Canada's parliamentary path to a tax |
| 15 | `c15_references.clj` | Sources and caveats |

Notebooks live in `src/economics/wealth_distribution/canada/`, mirroring the layout of the
original series in the [ClojureCivitas repository](https://github.com/ClojureCivitas/clojurecivitas.github.io)
so they can be moved there with minimal changes. (Civitas uses author keys; here `:author`
is a plain string.)

## Data

- `src/economics/wealth_distribution/canada/data/`: the tidy CSVs the notebooks read.
  See `data/SOURCES.md`.
- `data-prep/`: research notes with every figure's source and the scripts that built the CSVs
  from official downloads.

## Build

Requires Java, the [Clojure CLI](https://clojure.org/guides/install_clojure) and
[Quarto](https://quarto.org/docs/get-started/).

```sh
./build.sh            # render all notebooks with Clay, then the website with Quarto -> site/_site/
clojure -M:clay       # or: interactive Clay session
```
