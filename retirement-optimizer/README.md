# retirement-optimizer

A stateless, functional Clojure library for **Canadian retirement drawdown
planning**. Give it your accounts, a goal, and assumptions; it gives back:

1. **A yearly drawdown plan** (Part 1) — for each year of retirement, how much
   to withdraw from which account, the CPP/OAS/GIS you'll receive, the tax
   you'll pay, and end-of-year balances — plus a grid-search **optimizer**
   that finds the best withdrawal strategy for your situation.
2. **Monte Carlo simulation** (Part 2) — runs the plan across thousands of
   randomized market futures and answers *"this plan is successful 95% of the
   time"*, including how much you *can* spend at a chosen confidence level.

Zero dependencies beyond Clojure itself. Every function is pure: the same
inputs (including the random seed) always produce identical outputs, so the
library can be embedded in web apps, notebooks, spreadsheets-of-the-future,
or other tools without ceremony.

> **Not financial advice.** This is a modeling library with documented
> simplifications. Verify anything important with a professional.

## Quick start

```clojure
(require '[retirement.core :as r])

(def inputs
  {:person {:age 65
            :province :on                        ; :on | :bc | :ab
            :cpp {:start-age 70 :at-65 12000}    ; your expected CPP at 65
            :oas {:start-age 65}}
   :accounts [{:id :rrsp    :type :rrsp           :balance 500000
               :holdings {:equity 0.6 :bonds 0.4}}
              {:id :tfsa    :type :tfsa           :balance 120000
               :holdings {:equity 0.8 :bonds 0.2}}
              {:id :taxable :type :non-registered :balance 250000
               :acb 180000 :holdings {:equity 0.7 :bonds 0.3}}]
   :goal {:type :spend-down :annual-spending 55000}  ; real, after tax
   :start-year 2026})

(r/plan inputs)                        ; Part 1: year-by-year plan
(r/optimize inputs)                    ; best strategy + ranking
(r/simulate inputs {:trials 1000 :seed 42})          ; Part 2
(r/sustainable-spending inputs {:target 0.95})       ; "$X/yr at 95%"
(r/max-spending inputs)                ; deterministic ceiling
```

Sample of what comes back (see `examples/demo.clj` for the full tour):

```
year  age  spend   withdrawals                      tax     balance
2026  65   55000   {:taxable 32897}                 36      877010
...
2033  72   63613   {:rrsp 38501, :taxable 3288}     9086    934468

This plan succeeds 70% of the time.
When it fails, money runs out around age 90.
At 95% confidence this household can spend $47,500/yr (real, after tax).
```

All amounts in and out are **start-year ("today's") dollars** for inputs and
real-dollar summaries; year rows are nominal and carry an `:index` (CPI vs the
start year) so you can deflate anything.

Run the tests with `bin/test` (uses the Clojure CLI, or plain `java` with
`CLOJURE_JARS` pointing at a directory of Clojure jars).

## The model

### Accounts

| Type | Withdrawals taxed as | Special mechanics |
|---|---|---|
| `:rrsp` | ordinary income | auto-converts to RRIF at 71; forced minimums from 72 |
| `:rrif` | ordinary income | forced minimums at any age (CRA factor table; 5.28% @71 → 20% @95) |
| `:tfsa` | tax-free | withdrawals restore contribution room the next year; surplus cash is re-sheltered here first, room permitting |
| `:non-registered` | realized capital gains (50% inclusion), pro-rated by the account's ACB | throws off eligible dividends (equity) and interest (bonds/cash) annually, which are taxed even without withdrawals |

Holdings are deliberately vague asset mixes — `{:equity 0.6 :bonds 0.4}` —
not tickers. That's the right granularity for a 30-year projection.

### Government benefits

- **CPP**: you supply your expected age-65 entitlement; the library applies
  the actuarial adjustments (−0.6%/month before 65, +0.7%/month after, so
  64% at 60 and 142% at 70) and inflation indexing.
- **OAS**: 2025 base $727.67/month, +0.6%/month deferral to 70, +10% at 75,
  residency proration, and the **15% recovery tax (clawback)** above the
  indexed ~$93,454 threshold.
- **GIS**: income-tested at ~50¢ per dollar of non-OAS income. Crucially,
  TFSA withdrawals don't count as income — the engine will happily discover
  GIS-preserving strategies (defer CPP, live off TFSA/taxable) on its own.

### Tax engine

Data-driven (all constants live in `retirement.taxdata` as plain maps):
federal + provincial progressive brackets, basic personal amount (with the
federal high-income phase-out), age amount (65+, income-tested), pension
income credit, eligible/non-eligible dividend gross-up and credits, 50%
capital-gains inclusion, Ontario surtax and health premium. Bracket
thresholds and credit amounts are indexed to the plan's (possibly stochastic)
inflation path, except amounts that are unindexed in law (Ontario's $150k/
$220k thresholds, the health premium, the federal $2,000 pension amount).
The July 2025 federal rate cut is modeled (14.5% effective for 2025, 14%
from 2026).

**Ontario figures are verified** against CRA/TD1/ESDC 2025 published values;
BC and Alberta are best-effort and marked `:approximate?` in the data — the
schema makes any province a ~15-line data addition.

### The yearly solve (Part 1)

Within a year the engine:

1. Computes the inflation-adjusted spending target and benefits.
2. Takes forced RRIF minimums and any strategy "pre-withdrawals"
   (e.g. bracket-fill top-ups).
3. **Solves by bisection** for the smallest discretionary withdrawal whose
   *after-tax* cash meets the target — necessary because tax, OAS clawback
   and GIS all depend on the withdrawals themselves. Net cash is monotone in
   the gross withdrawal, so bisection converges fast and exactly.
4. Reinvests surplus cash (e.g. forced minimums beyond needs): TFSA first
   while room lasts (room is tracked, including restored withdrawals), then
   a non-registered account (raising its ACB).
5. Applies market growth (withdraw-at-start-of-year convention).

At the horizon the **estate** is valued after terminal taxes: registered
balances are fully taxable as income on the final return and unrealized
non-registered gains are deemed disposed; the TFSA passes tax-free.

### Strategies and the optimizer

Strategies are plain data with two pure extension points
(`pre-withdrawals` and `allocate` multimethods in `retirement.strategy`):

```clojure
{:type :sequential :order [:non-registered :registered :tfsa]} ; conventional
{:type :proportional}
{:type :bracket-fill :ceiling :first-bracket-top}   ; "RRSP meltdown" family
{:type :bracket-fill :ceiling :oas-clawback}
{:type :bracket-fill :ceiling 70000}                ; custom, real dollars
```

`optimize` grid-searches the strategy family and ranks by real after-tax
estate with a heavy penalty per dollar of missed spending (or by Monte Carlo
success rate with `{:metric :success-rate}`). Because GIS, the OAS clawback,
bracket structure and terminal RRIF taxation all interact, **the winner is
scenario-dependent** — e.g. for a modest portfolio the conventional
taxable-first order can win *because* low early income preserves GIS, while
for a large RRSP-heavy portfolio bracket-filling wins by defusing forced
RRIF minimums and the estate tax bomb. The test suite pins both behaviours.

**Why not core.logic?** We looked at it. Constraint/relational solvers fit
discrete, combinatorial problems; this one is continuous and numeric — tax
owed is a piecewise-linear monotone function of withdrawals, and the
objective is a smooth function of a handful of strategy parameters. The
practical machinery (also what purpose-built commercial optimizers use) is
root-finding per year plus search over a small strategy space, which is what
we do. If someone later adds truly discrete choices (e.g. "in which single
year do I sell the cottage?"), exhaustive enumeration over those few
choices composes naturally with this engine.

### Monte Carlo (Part 2)

- **Correlated returns**: equity/bonds/cash/inflation drawn jointly via a
  Cholesky factorization of the correlation matrix; assets use lognormal
  growth factors (a return can never fall below −100%), inflation is normal.
- **Full plan per trial**: every trial re-runs the Part-1 engine — taxes,
  clawbacks, GIS, RRIF minimums and strategy behaviour all respond to each
  sampled path, so sequence-of-returns risk is captured properly.
- **Deterministic**: each trial's RNG stream derives from `(:seed opts)` and
  the trial index (SplittableRandom + golden-ratio mixing), so results are
  reproducible and identical whether trials run in parallel or not.
- **Honest reporting**: success probability is magnitude-blind (a $1 miss in
  year 30 counts like a $500k miss in year 15), so the result also includes
  estate percentiles, per-year real balance bands (p5–p95), the ruin-age
  distribution, and the median shortfall among failures.

```clojure
(r/simulate inputs {:trials 1000 :seed 42})
;; => {:success-rate 0.70
;;     :estate-real {:p5 0 :p25 0 :p50 222692 :p75 628193 :p95 1826385}
;;     :ruin {:probability 0.30 :ages {:p5 84 ... :p50 90 ...}}
;;     :shortfall {...} :yearly {...}}
```

`sustainable-spending` inverts the question by bisecting the spending level
against a target success rate — the headline "you can spend $X/yr with 95%
confidence" number.

## Research notes

What we found surveying the state of the art (sources verified 2025/2026):

- **Withdrawal ordering matters ~2–3 years of portfolio longevity.** The
  conventional taxable → tax-deferred → tax-free order is dominated by
  strategies that smooth marginal tax rates across retirement — filling low
  brackets with RRSP/RRIF income early instead of stacking forced RRIF
  minimums on CPP/OAS later (Cook, Meyer & Reichenstein, *Financial Analysts
  Journal* 71(2) 2015; FPA *Journal of Financial Planning* 2021). The
  Canadian expression of this is the **RRSP meltdown**: drain the RRSP in
  the low-income window before 71, often while deferring CPP/OAS to 70
  (+42%/+36%), re-sheltering the excess in the TFSA. C.D. Howe Commentary
  641 (2023) documents how mandatory RRIF minimums force decumulation too
  fast — the policy backdrop that makes voluntary early withdrawals
  attractive. True optimizers (e.g. MoneyReady) find per-year *blends*, not
  strict sequences, which is why strategies here are parameterized families
  rather than fixed orderings.
- **Safe withdrawal rates**: Bengen's 4% (revised by him to 4.7% in 2025),
  Trinity's 95% success at 4%/30yr/50-50, Morningstar's forward-looking
  3.9% (2025/26, 90% success), Guyton-Klinger guardrails 5.2–5.6% with
  spending rules. Sequence-of-returns risk is why Monte Carlo, not average
  returns, must be the arbiter — the first decade's real return explains
  most of the variance in 30-year outcomes.
- **Monte Carlo practice**: ~1,000 trials is the commercial norm (standard
  error ≈ ±1.1pp at p=0.85; 5,000–10,000 for ~1pp stability — model risk
  dominates beyond that). IID lognormal with a correlation matrix is the
  standard baseline; block-bootstrap and regime models fatten tails.
  Success-probability alone is criticized as magnitude-blind (Kitces) —
  hence the extra distributions we report. Stock–bond correlation
  assumptions of 0 to −0.2 are typical, with a positive-correlation
  (inflationary) stress regime recommended — the default here is 0, and the
  correlation matrix is a tunable assumption.
- **Existing open source**: nothing meaningful exists in Clojure; the JVM
  has OpenGamma Strata (derivatives pricing, not retirement), and the
  retirement space is Python/JS calculators (FIRECalc, cfiresim, TPAW) that
  are US-centric and mostly UI-bound rather than embeddable libraries.
  A pure-data Canadian library appears to be genuinely new ground.

Default capital-market assumptions follow the FP Canada 2025 Projection
Assumption Guidelines flavour (equities ~6.4%, bonds 3.4%, cash 2.3%,
inflation 2.1%) — all overridable per run.

## Input reference

See the `retirement.inputs` docstring for the full schema. Highlights:

```clojure
{:person {:birth-year 1961        ; or :age (at start-year)
          :end-age 95             ; plan to this age (default 95)
          :province :on
          :cpp {:start-age 70 :at-65 12000}
          :oas {:start-age 65 :fraction 1.0}   ; fraction = residency/40
          :pensions [{:annual 20000 :start-age 65 :indexed? true}]
          :tfsa-room 10000}
 :accounts [...]
 :goal {:type :spend-down          ; or :legacy
        :annual-spending 60000     ; real, after-tax
        :legacy 250000}            ; real after-tax estate floor (for :legacy)
 :strategy {...}                   ; optional; default conventional
 :assumptions {...}                ; deep-merged over r/default-assumptions
 :start-year 2026}
```

Bad inputs fail fast: `(r/validate inputs)` returns human-readable problems,
and every entry point throws `ex-info` with an `:errors` vector otherwise.

## Simplifications (read before trusting numbers)

Single person only (no spousal RRSPs, pension income splitting, or survivor
benefits — the biggest missing lever for couples). Net income == taxable
income (no deductions). OAS clawback and GIS use current-year income rather
than the prior year's. GIS uses a flat 50% reduction (slightly generous near
the cutoff where the real top-up phases out at ~75%). CPP is price-indexed
pre-take-up (really wage-indexed). TFSA room ignores the $500 rounding of
the annual limit. No LIRA/LIF maximums, RRSP withholding timing, AMT, or
probate fees. Quebec (abatement + separate regime) is not included.
Withdrawals happen at the start of each year. All are deliberate scope
choices; the data-driven design keeps each one an isolated, testable change.

## Extending

- **A province**: add a data map to `retirement.taxdata/provinces`.
- **A strategy**: implement `retirement.strategy/pre-withdrawals` and/or
  `allocate` for a new `:type`, then pass it in `:strategy` or to
  `optimize`'s `:candidates`.
- **A return model**: `r/plan` and `plan/run-plan*` accept any explicit
  market path — feed historical bootstraps or regime-switching paths from
  your own sampler straight into the Part-1 engine.

## Development

```
src/retirement/
  taxdata.clj    tax & benefit constants (data)
  tax.clj        tax engine
  benefits.clj   CPP / OAS / GIS
  accounts.clj   account mechanics, RRIF minimums, ACB
  inputs.clj     schema, defaults, validation
  strategy.clj   withdrawal strategies (open multimethods)
  plan.clj       Part 1: yearly engine + bisection solve
  simulate.clj   Part 2: Monte Carlo
  optimize.clj   strategy grid search
  core.clj       public API
```

`bin/test` runs the suite (61 tests / 387 assertions), including
hand-computed CRA tax cases, benefit adjustment factors, ACB accounting,
cash conservation per plan-year, strategy-behaviour pins, Cholesky
round-trips, determinism (parallel == sequential), and Monte Carlo
monotonicity properties.
