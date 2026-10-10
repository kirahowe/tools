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
| 0 | `c0_introduction.clj` | Introduction and central claims |
| 1 | `c1_who_pays_tax.clj` | Federal revenue, capital gains, tax preferences, no inheritance tax |
| 2 | `c2_wealth_tax.clj` | PBO-costed proposals, who would pay, debt charges, objections |
| 3 | `c3_labour_hours.clj` | House prices vs income, homeownership, debt, living wage, productivity and pay |
| 4 | `c4_interactive_factors.clj` | Causal loop diagram of the Canadian system |
| 5 | `c5_wealth_concentration.clj` | The full distribution (SFS + PBO + Forbes), quintile trends, long run |
| 6 | `c6_economic_growth.clj` | Circulation, participation, "the Canadian disease" |
| 7 | `c7_investment_question.clj` | Would a wealth tax kill investment? Canada's tax-cut experiment and the evidence |
| 8 | `c8_canadian_differences.clj` | Mobility, public programs, provinces, Indigenous gaps, concentration |
| 9 | `c9_how_to_get_a_wealth_tax.clj` | Canada's parliamentary path to a tax |
| 10 | `c10_references.clj` | Sources and caveats |

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
