# oas — Old Age Security payout modelling for Canadians

A small, dependency-free, purely functional Clojure library that models
Canada's **Old Age Security (OAS)** pension: one function that takes plain
data in and returns expected payments as plain data out.

```clojure
(require '[oas.core :as oas])

(oas/estimate {:birth-date "1961-06-15"
               :years-in-canada 40
               :annual-income 30000})
;; => {:eligible? true
;;     :monthly {:oas-gross 751.97M :recovery-tax 0.00M
;;               :oas-net 751.97M :gis 0.00M :total 751.97M}
;;     :schedule [...] :notes [...] ...}
```

Everything is stateless and deterministic: no clocks, no I/O, no global
state. Benefit rates live in [`oas.rates`](src/oas/rates.clj) as data and
can be overridden per call, because the real-world figures change every
quarter.

> **Disclaimer** — this is a planning/estimation tool, not legal or
> financial advice, and not a substitute for Service Canada. Amounts were
> researched as of July 2026 and are close to—but not guaranteed to
> match—official figures, especially for GIS (see *Accuracy* below).

## How OAS actually works (research notes)

OAS is frequently misunderstood as contribution-based. It is not — there
are **no OAS contributions and no contribution rate**. Unlike the Canada
Pension Plan (a separate, contributory program this library does not
model), OAS is funded from general tax revenue, and entitlement is earned
purely by **residence in Canada after age 18**. The rules this library
implements:

### Eligibility
- Age 65 or older; payments can begin the month after the 65th birthday.
- Minimum **10 years** of Canadian residence after 18 if you live in
  Canada, **20 years** if you apply from abroad. Years in countries with an
  international social security agreement can be totalized to *meet the
  minimum* (they never increase the amount).
- With fewer than 20 years of residence, payments stop after six months
  outside Canada.

### Amount
- **Full pension** requires **40 years** of residence after 18; otherwise a
  **partial pension** of (whole years)/40. The fraction is locked in when
  payments start.
- **Voluntary deferral**: +**0.6 % per month** past eligibility, up to 60
  months (+36 % at age 70). No credit accrues past 70. A month of waiting
  can count *either* toward the deferral credit *or* toward more residence
  years — never both — so for people short of 40 years the library
  maximizes over every possible split (which sometimes beats both the
  "keep accruing" and the "pure deferral" strategies).
- **Age 75 uplift**: a permanent automatic **+10 %** in the month after the
  75th birthday (in place since July 2022). This is why official tables
  show two maximums (July–September 2026: **$751.97** for 65–74,
  **$827.17** for 75+ — always exactly 1.10× apart).
- **Quarterly indexation**: amounts are re-indexed to CPI every January,
  April, July and October, and by law never decrease.

### Recovery tax (the "clawback")
OAS is reduced by **15 %** of net world income — *including* the OAS
itself — above an annually indexed threshold (**$90,997** for 2024,
**$93,454** for 2025, **$95,323** for 2026), capped at the OAS received.
Administratively it is withheld from July to June based on the previous
year's return; the library models it economically against the income you
supply. Because 75+ pensioners receive more OAS, their pension survives to
a higher income before being fully clawed back.

### Guaranteed Income Supplement (estimated)
GIS is a non-taxable, income-tested supplement for low-income OAS
pensioners residing in Canada. The library models the statutory structure:

- income for GIS **excludes OAS/GIS** and gets an **earnings exemption**
  (first $5,000 of employment/self-employment earnings, plus 50 % of the
  next $10,000, per person);
- the base supplement reduces by **$1/month per $2/month** of income for
  singles, and **$1 per $4** of combined income for each member of a couple
  where both receive OAS; the 2011/2016 **top-up** phases out at 25 ¢ (single)
  / 12.5 ¢ each (couple) per dollar above $2,000 / $4,000;
- **partial pensioners get a larger GIS maximum** equal to their OAS
  shortfall, so the OAS + GIS floor is the same as a full pensioner's;
- **no GIS is payable while deferring OAS** (a real cost of deferral for
  low-income seniors), nor while living outside Canada;
- couples **involuntarily separated** (e.g. one partner in long-term care)
  are each assessed as single on their own income.

## Usage

```clojure
(require '[oas.core :as oas])

;; Defer to 70 with a high income (clawback):
(oas/estimate {:birth-date "1958-06-01"
               :years-in-canada 40
               :start-date "2028-07"          ; optional; default = first eligible month
               :retirement-income 100000      ; optional; excludes OAS/GIS
               :income-year 2026})

;; Low-income couple, partial pension, GIS:
(oas/estimate {:birth-date "1959-09-21"
               :years-in-canada 34
               :marital-status :married
               :partner-receives-oas? true
               :annual-income 8000
               :employment-income 6000
               :partner-annual-income 4000})

;; Exact residence history instead of a year count:
(oas/estimate {:birth-date "1961-06-15"
               :residence-periods [["1990-01" "2005-01"] [[2015 1] nil]]})

;; Newer rates than the library ships with:
(oas/estimate {:birth-date "1961-06-15"
               :years-in-canada 40
               :as-of [2026 10]
               :rates {:quarters [{:from [2026 10]
                                   :oas-65-74 760.00M :oas-75-plus 836.00M}]
                       :clawback-thresholds {2027 97000M}}})
```

Dates accept `"YYYY-MM-DD"`, `"YYYY-MM"`, `[year month]` or
`{:year y :month m}`. See the `oas.core/estimate` docstring for the full
input/output reference. Money is returned as 2-decimal `BigDecimal`s; all
internal arithmetic is exact (rationals), rounded only at the edges.

Results are expressed in the dollars of the `:as-of` rate quarter
(default: latest known). The library deliberately does **not** forecast
CPI — a projection in constant current dollars is the honest baseline for
planning.

## Keeping rates current

Quarterly maximums and annual thresholds go stale by design. Update
`oas.rates/quarters` / `clawback-thresholds` from
[Old Age Security payment amounts](https://www.canada.ca/en/services/benefits/publicpensions/old-age-security/payments.html)
and the [quarterly benefit-amount reports](https://www.canada.ca/en/employment-social-development/programs/pensions/pension/statistics/2026-quarterly-april-june.html),
or pass newer figures at the call site via `:rates` (no code changes
needed). Handy invariants for checking a new quarter's data: the 75+
maximum is exactly 1.10× the 65–74 maximum; GIS single/couple cutoffs are
multiples of $24/$48; amounts never decrease.

## Accuracy notes and non-goals

- **OAS amounts** (full/partial/deferral/75+/clawback) follow the
  statutory formulas and should match Service Canada to the cent, given
  correct inputs and current rate data.
- **GIS amounts** are estimates from the statutory formula; Service
  Canada's published tables move in income brackets and round
  independently, so expect small differences. The
  spouse-without-OAS category uses a simplified linear model. GIS output
  is flagged `:estimate? true`.
- The deferral/residence-accrual interaction is modeled as "the best
  no-double-counting split", which matches the advisory consensus
  ("whichever is more advantageous") and generalizes it; Service Canada's
  internal calculation may differ marginally.
- Not modeled: CPP/QPP; the Allowance and Allowance for the Survivor
  (ages 60–64); application mechanics (automatic enrolment, 11-month
  retroactivity); the pre-1977 grandfathered full-pension rules;
  non-resident withholding tax; sponsored-immigrant GIS restrictions;
  incarceration suspensions; month-of-birthday day precision.

## Development

Zero runtime dependencies. With the [Clojure CLI](https://clojure.org/guides/install_clojure):

```sh
clojure -M:test
```

or with nothing but a JDK and the Clojure jars on the classpath:

```sh
java -cp clojure.jar:spec.alpha.jar:core.specs.alpha.jar:src:test \
     clojure.main -m oas.test-runner
```

## Sources

Figures were cross-checked (July 2026) against Service Canada / canada.ca
pages and multiple secondary republications:

- [Old Age Security payment amounts — Canada.ca](https://www.canada.ca/en/services/benefits/publicpensions/old-age-security/payments.html)
- [Old Age Security: How much you could receive — Canada.ca](https://www.canada.ca/en/services/benefits/publicpensions/old-age-security/benefit-amount.html)
- [When to start your OAS pension — Canada.ca](https://www.canada.ca/en/services/benefits/publicpensions/old-age-security/when-start.html)
- [OAS pension recovery tax — Canada.ca](https://www.canada.ca/en/services/benefits/publicpensions/old-age-security/recovery-tax.html)
- [GIS: How much you could receive — Canada.ca](https://www.canada.ca/en/services/benefits/publicpensions/old-age-security/guaranteed-income-supplement/benefit-amount.html)
- [Quarterly maximum benefit amounts (Jan–Mar 2026)](https://www.canada.ca/en/employment-social-development/programs/pensions/pension/statistics/2026-quarterly-january-march.html) and [(Apr–Jun 2026)](https://www.canada.ca/en/employment-social-development/programs/pensions/pension/statistics/2026-quarterly-april-june.html)
- [KPMG — Old Age Security benefit rate sheet (2026)](https://assets.kpmg.com/content/dam/kpmg/ca/pdf/2026/01/ca-old-age-security-benefits.pdf)
- [Wealthsimple — OAS clawback explained](https://www.wealthsimple.com/en-ca/learn/oas-clawback-explained)
- [Savvy New Canadians — maximum income to qualify for GIS](https://www.savvynewcanadians.com/what-is-the-maximum-income-to-qualify-for-gis/)
- [Cardinal Point Wealth — deferring CPP and OAS](https://cardinalpointwealth.com/2025/10/10/deferring-social-security-cpp-and-oas-a-cross-border-perspective-for-u-s-citizens-living-in-canada/) (deferral vs. residence-accrual interaction)
- [Boomer & Echo — deferring OAS to 70](https://boomerandecho.com/defer-oas-to-70/), [Retire Happy — voluntary deferral of OAS](https://retirehappy.ca/voluntary-deferral-of-oas/)
- [LifeMoney — OAS increase July 2026](https://lifemoney.ca/blog/oas-increase-july-2026), [OAS payment amounts 2026](https://lifemoney.ca/blog/oas-payment-amounts-2026)

## License

MIT — see [LICENSE](LICENSE).
