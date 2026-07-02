(ns cpp.data
  "Historical CPP parameters: YMPE, YBE, contribution rates, YAMPE.

  Sources: CRA 'CPP contribution rates, maximums and exemptions';
  Canada Pension Plan Act (R.S.C. 1985, c. C-8); DR Pensions rate
  tables. Values are exact legislated/announced figures through 2026;
  later years are projected (see `ympe-fn`).")

(defn- mean [xs] (/ (reduce + 0.0 xs) (count xs)))

(defn- round-down-100 [x] (* 100.0 (Math/floor (/ x 100.0))))

;; ---------------------------------------------------------------------------
;; Year's Maximum Pensionable Earnings (YMPE)
;; ---------------------------------------------------------------------------

(def ympe-table
  "Year -> YMPE in dollars, 1966 through the latest announced year.

  1973 is shown as $5,600, matching Service Canada's own
  benefit-calculation tables (their published 1974 MPEA of $5,900 is
  the average of 5,500/5,600/6,600); some CRA/QPP-derived series show
  $5,900 for 1973 (Quebec raised its ceiling mid-1973; the federal
  increase took effect in 1974)."
  {1966 5000, 1967 5000, 1968 5100, 1969 5200, 1970 5300
   1971 5400, 1972 5500, 1973 5600, 1974 6600, 1975 7400
   1976 8300, 1977 9300, 1978 10400, 1979 11700, 1980 13100
   1981 14700, 1982 16500, 1983 18500, 1984 20800, 1985 23400
   1986 25800, 1987 25900, 1988 26500, 1989 27700, 1990 28900
   1991 30500, 1992 32200, 1993 33400, 1994 34400, 1995 34900
   1996 35400, 1997 35800, 1998 36900, 1999 37400, 2000 37600
   2001 38300, 2002 39100, 2003 39900, 2004 40500, 2005 41100
   2006 42100, 2007 43700, 2008 44900, 2009 46300, 2010 47200
   2011 48300, 2012 50100, 2013 51100, 2014 52500, 2015 53600
   2016 54900, 2017 55300, 2018 55900, 2019 57400, 2020 58700
   2021 61600, 2022 64900, 2023 66600, 2024 68500, 2025 71300
   2026 74600})

(def latest-known-year (apply max (keys ympe-table)))

(def default-wage-growth
  "Default assumed annual YMPE growth for years beyond the published
  table. The YMPE tracks the industrial aggregate average weekly wage;
  ~3% is a reasonable long-run default. Override via
  {:assumptions {:wage-growth g}}."
  0.03)

(defn ympe-fn
  "Year -> YMPE. Known years come from the table; later years are
  projected at `wage-growth`, rounded down to the nearest $100 as the
  CPP Act prescribes for the real thing."
  [wage-growth]
  (fn [year]
    (or (ympe-table year)
        (if (< year 1966)
          (ympe-table 1966)
          (round-down-100
           (* (ympe-table latest-known-year)
              (Math/pow (+ 1.0 wage-growth) (- year latest-known-year))))))))

;; ---------------------------------------------------------------------------
;; YAMPE (second earnings ceiling, CPP2, 2024+)
;; ---------------------------------------------------------------------------

(def yampe-table
  "Announced YAMPE values. 2024 was set at ~107% of YMPE; from 2025 on
  it is ~114% of YMPE, rounded down to the nearest $100."
  {2024 73200, 2025 81200, 2026 85000})

(def ^:private yampe-ratio 1.14)

(defn yampe-fn
  "Year -> YAMPE, or nil before 2024 (no second ceiling existed).
  Projected years use 114% of the projected YMPE, rounded down to the
  nearest $100."
  [ympe]
  (fn [year]
    (cond
      (< year 2024) nil
      (yampe-table year) (yampe-table year)
      :else (round-down-100 (* yampe-ratio (ympe year))))))

;; ---------------------------------------------------------------------------
;; Year's Basic Exemption (YBE)
;; ---------------------------------------------------------------------------

(def ^:private ybe-table
  "$600 fixed until 1974; from 1975, 10% of YMPE rounded down to the
  nearest $100; frozen at $3,500 from 1996 by the 1997 reform."
  {1975 700, 1976 800, 1977 900, 1978 1000, 1979 1100
   1980 1300, 1981 1400, 1982 1600, 1983 1800, 1984 2000
   1985 2300, 1986 2500, 1987 2500, 1988 2600, 1989 2700
   1990 2800, 1991 3000, 1992 3200, 1993 3300, 1994 3400
   1995 3400})

(defn ybe
  "Year's Basic Exemption for `year`."
  [year]
  (cond
    (< year 1975) 600
    (>= year 1996) 3500
    :else (ybe-table year)))

;; ---------------------------------------------------------------------------
;; Contribution rates (employee; employer matches, self-employed pays 2x)
;; ---------------------------------------------------------------------------

(def ^:private employee-rate-table
  "Combined employee rate (base + first additional) on earnings between
  the YBE and YMPE. 1997 is CRA's retroactive reform rate of 3.0%
  (2.925% was withheld at source that year)."
  {1987 0.019, 1988 0.020, 1989 0.021, 1990 0.022, 1991 0.023
   1992 0.024, 1993 0.025, 1994 0.026, 1995 0.027, 1996 0.028
   1997 0.030, 1998 0.032, 1999 0.035, 2000 0.039, 2001 0.043
   2002 0.047, 2019 0.0510, 2020 0.0525, 2021 0.0545, 2022 0.0570})

(defn employee-rate
  "Combined employee contribution rate (base + first additional) for
  `year`, on earnings between the YBE and YMPE."
  [year]
  (cond
    (<= year 1986) 0.018
    (<= 2003 year 2018) 0.0495
    (>= year 2023) 0.0595
    :else (employee-rate-table year)))

(def cpp2-employee-rate
  "Employee rate on earnings between YMPE and YAMPE, 2024+. No basic
  exemption applies to this band."
  0.04)

;; ---------------------------------------------------------------------------
;; Enhancement phase-in
;; ---------------------------------------------------------------------------

(def ^:private phase-in-table
  {2019 0.15, 2020 0.30, 2021 0.50, 2022 0.75})

(defn first-additional-phase-in
  "Credit factor applied to a year's first-additional pensionable
  earnings: the pension credit for 2019-2023 is scaled in exact
  proportion to that year's first-additional contribution rate
  (0.15%/0.30%/0.50%/0.75% of the eventual 1.0%)."
  [year]
  (cond
    (< year 2019) 0.0
    (>= year 2023) 1.0
    :else (phase-in-table year)))

;; ---------------------------------------------------------------------------
;; MPEA (Maximum Pensionable Earnings Average, a.k.a. AYMPE)
;; ---------------------------------------------------------------------------

(defn mpea
  "Average YMPE over the averaging window ending in the pension-start
  year: 5 years for starts in 1999+, 4 years for 1998, 3 years before
  that (transition rule from the 1997 reform)."
  [start-year ympe]
  (let [n (cond
            (>= start-year 1999) 5
            (= start-year 1998) 4
            :else 3)]
    (mean (map ympe (range (inc (- start-year n)) (inc start-year))))))
