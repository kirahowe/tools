# cpp — Canada Pension Plan payout modeling

A small, functional, stateless Clojure library that models CPP
retirement pension payouts, following the actual Service Canada
month-level methodology rather than a back-of-envelope approximation.

Everything is pure data in, pure data out: give it a birth date and a
map of `{year earnings}` (or `{year contributions}`), optionally a
start date/age, and it returns the expected monthly pension broken
into its legislated components.

The model reproduces the officially published maximum new pension at
65 — $1,433.00/month for January 2025 and $1,507.65/month for January
2026 — to within pennies (see the test suite).

## Quick start

```clojure
(require '[cpp.core :as cpp])

;; born March 1961, earned ~80% of the YMPE from 1981 through 2025
(cpp/estimate
 {:birth-date "1961-03"
  :pensionable-earnings {1981 11760, 1982 13200, #_... 2025 57040}})
;; => {:start {:date "2026-04" :year 2026 :month 4
;;             :age {:years 65 :months 0}}
;;     :adjustment {:months 0 :factor 1.0}
;;     :monthly {:base 1153.00
;;               :first-additional 45.16
;;               :second-additional 0.0
;;               :total 1198.16}
;;     :annual 14377.91
;;     :details {...}}

;; same person, deferring to 70: +42% actuarial factor plus five more
;; years of wage indexing through the MPEA
(cpp/estimate
 {:birth-date "1961-03"
  :pensionable-earnings {...}
  :start {:years 70}})
;; => {:adjustment {:months 60 :factor 1.42}
;;     :monthly {:base 1929.78 :first-additional 75.58 :total 2005.36}
;;     ...}
```

## What it models

- **Base CPP** — 25% of average monthly pensionable earnings. Each
  year's earnings become a ratio of that year's YMPE, and every month
  is valued at ratio × MPEA ÷ 12, where the MPEA is the five-year
  average YMPE ending in the start year (3-year before 1998, 4-year
  for 1998) — the only wage indexing CPP applies.
- **Contributory period** at month granularity: the month after the
  18th birthday (or January 1966) through the month before the
  pension starts, with partial first/last calendar years prorated
  against a prorated YMPE, exactly as Service Canada does.
- **General dropout** — the lowest ⌈17%⌉ of contributory months are
  dropped, never averaging over fewer than 120 months (so ~8 zero
  years of a 47-year period are free).
- **Child-rearing provisions** — months as primary caregiver of a
  child under 7 are excluded from the base calculation where that
  helps (CRDO, solved as a fixpoint against the rest of the dropout
  pipeline), and *dropped in* at the prior five-year average earnings
  for the enhanced components (CPP Act ss. 53.3–53.6).
- **Over-65 substitution** — each contributory month after 65 lets a
  below-average month (possibly itself) be dropped, so deferring past
  65 after stopping work costs nothing (s. 48(3)).
- **Disability** — months on a CPP disability pension are excluded
  from the base contributory period entirely, and credited at 70% of
  the prior six-year average for the enhanced components.
- **Early/late start** — clamped to the legal 60–70 window;
  −0.6%/month before 65, +0.7%/month after (max +42% at 70), fixed
  permanently at the start month and applied to all components.
- **CPP enhancement (2019+)** — the first additional component
  (8.33% replacement on the same band as the base, fixed 480-month
  divisor, no dropouts, best-40-years selection, 2019–2022 credits
  scaled 0.15/0.30/0.50/0.75 with the contribution phase-in) and the
  second additional component / CPP2 (33.33% replacement on earnings
  between the YMPE and YAMPE, from 2024).
- **Contributions → earnings inversion** — feed `:contributions`
  (annual employee amounts from a CPP statement of contributions;
  `:self-employed? true` for double-rate amounts) and the library
  reconstructs pensionable earnings using each year's rate and basic
  exemption, including the CPP2 band. Years at or below the YBE are
  deemed non-pensionable, per s. 53.
- **Future years** — earnings past the published table use a YMPE
  projected at a configurable wage-growth assumption (default 3%,
  rounded down to $100 like the real thing).

## Input reference

| key | required | shape | notes |
|-----|----------|-------|-------|
| `:birth-date` | yes | `"YYYY-MM"` or `{:year Y :month M}` | day of month is irrelevant to CPP |
| `:pensionable-earnings` | one of | `{year amount}` | annual gross employment earnings (capped internally at YMPE/YAMPE) |
| `:contributions` | one of | `{year amount}` | annual *employee* contributions; add `:self-employed? true` if they're the doubled amounts |
| `:start` | no | `"YYYY-MM"`, or an age `{:years 63 :months 4}` | default: month after the 65th birthday; clamped to 60–70 |
| `:children` | no | `[{:born "YYYY-MM"} ...]` | assumes this person claims the child-rearing provision |
| `:disability` | no | `[{:from "YYYY-MM" :to "YYYY-MM"} ...]` | periods receiving a CPP disability pension |
| `:assumptions` | no | `{:wage-growth 0.03}` | YMPE projection beyond the published table |

All amounts in the result are in dollars of the start year. Once in
pay, CPP is CPI-indexed every January (and never decreases), which is
a straightforward multiplier this library deliberately leaves to the
caller.

## Approximations & judgment calls

The CPP Act leaves a few corners genuinely ambiguous for a model;
these are the choices made here, all flagged in docstrings too:

- **1973 YMPE is $5,600**, matching Service Canada's own
  benefit-calculation tables; some CRA/QPP-derived series show $5,900
  (Quebec raised its ceiling mid-1973, the federal plan in 1974).
- **Enhanced drop-in look-backs** use the component's earnings ratios
  for the prior 5 (or 6) years even where the component didn't exist
  yet (e.g. a child born in 2021 looks back to 2016–2020). The
  transition-era statutory value would be lower; the steady state is
  identical.
- **1997's contribution rate** is CRA's retroactive 3.0% (2.925% was
  what payroll actually withheld) — only relevant to the
  contributions→earnings inversion.
- The CRDO's "where beneficial" test is solved as a fixpoint against
  the full dropout pipeline, which is the legislated intent; Service
  Canada's manual procedure can differ by a month at the margin.

## Not modeled

Post-retirement benefits (working while collecting), pension sharing,
credit splitting on divorce, survivor/death/disability benefit
*amounts*, OAS/GIS interactions, and Quebec's QPP (a close cousin
with different parameters).

## Development

```sh
clojure -X:test
```

(or, with no network beyond Maven Central: put Clojure's jars on a
plain `java -cp` classpath with `src:test` and run
`clojure.test/run-tests` on `cpp.core-test`.)

## Sources

Rates and mechanics were compiled and cross-verified from the CRA's
published contribution rates/maximums/exemptions, the Canada Pension
Plan Act (R.S.C. 1985, c. C-8, ss. 46–53.6), OSFI's actuarial reports
on the CPP, canada.ca benefit pages, and DR Pensions Consulting's
rate tables and worked examples.
