# cpp — Canada Pension Plan payout modeling

A small, functional, stateless Clojure library that models CPP
retirement pension payouts, following the actual Service Canada
month-level methodology rather than a back-of-envelope approximation.

Everything is pure data in, pure data out: give it a birth date and a
map of `{year earnings}` (or `{year contributions}`), optionally a
start date/age, and it returns the expected monthly pension broken
into its legislated components.

## Quick start

```clojure
(require '[cpp.core :as cpp])

(cpp/estimate
 {:birth-date "1961-03"
  :pensionable-earnings {1985 25000, 1986 27000, #_... 2025 71300}})
;; => {:monthly {:base 812.34
;;               :first-additional 41.20
;;               :second-additional 6.75
;;               :total 860.29}
;;     :annual 10323.48
;;     :adjustment {:months 0 :factor 1.0}
;;     :start {:year 2026 :month 4 :age {:years 65 :months 0}}
;;     :details {...}}
```

## What it models

- **Base CPP** — 25% of average monthly pensionable earnings, with
  earnings wage-indexed to the start year through the ratio-to-YMPE /
  MPEA method used by Service Canada.
- **Contributory period** at month granularity (month after the 18th
  birthday, or January 1966, through the month before the pension
  starts).
- **General dropout** — the lowest-earning 17% of contributory months
  are dropped (never averaging over fewer than 120 months).
- **Child-rearing provisions** — months as primary caregiver of a
  child under 7 are excluded from the base calculation when
  below-average (CRDO), and *dropped in* at prior-average earnings for
  the enhanced components (CRDI).
- **Over-65 dropout** — below-average months after 65 are excluded.
- **Disability exclusion / drop-in** — months on a CPP disability
  pension are excluded from the base contributory period and credited
  for the enhanced components.
- **Early/late start** — take it as early as 60 (−0.6%/month) or as
  late as 70 (+0.7%/month, +42% max), with deferral also picking up
  wage indexing through the MPEA.
- **CPP enhancement (2019+)** — the first additional component
  (8.33% replacement, 40-year averaging, 2019–2023 phase-in credits)
  and the second additional component / CPP2 (33.33% replacement on
  earnings between the YMPE and YAMPE).
- **Contributions → earnings inversion** — if you have contribution
  amounts (e.g. from a CPP statement of contributions) instead of
  earnings, the library converts using each year's rate and basic
  exemption.
- **Future years** — earnings past the published YMPE table are
  handled by projecting the YMPE at a configurable wage-growth
  assumption.

## Input reference

| key | required | shape | notes |
|-----|----------|-------|-------|
| `:birth-date` | yes | `"YYYY-MM"` or `{:year Y :month M}` | day-of-month is irrelevant to CPP |
| `:pensionable-earnings` | one of | `{year amount}` | annual employment earnings (capped at YMPE/YAMPE internally) |
| `:contributions` | one of | `{year amount}` | annual *employee* contributions; inverted to earnings |
| `:start` | no | `"YYYY-MM"` or `{:years A :months M}` (an age) | default: month after 65th birthday; clamped to 60–70 |
| `:children` | no | `[{:born "YYYY-MM"} ...]` | assumes this person claims the child-rearing provision |
| `:disability` | no | `[{:from "YYYY-MM" :to "YYYY-MM"} ...]` | periods receiving a CPP disability pension |
| `:assumptions` | no | `{:wage-growth 0.03}` | YMPE projection beyond the published table |

## Not modeled

Post-retirement benefits (working while collecting), pension sharing,
credit splitting on divorce, survivor/death/disability benefits
themselves, the incremental over-70 rule (none exists — deferral gains
stop at 70), and Quebec's QPP (close cousin, different parameters).

## Development

```sh
clojure -X:test
```
