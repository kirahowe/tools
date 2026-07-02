(ns cpp.data
  "Historical CPP parameters: YMPE, YBE, contribution rates, YAMPE.

  Sources: Canada Revenue Agency published CPP contribution rates,
  maximums and exemptions; Canada Pension Plan Act. Values are exact
  legislated/announced figures through the current year; later years
  are projected (see `cpp.data/ympe`).")

;; ---------------------------------------------------------------------------
;; Year's Maximum Pensionable Earnings (YMPE)
;; ---------------------------------------------------------------------------

(def ympe-table
  "Year -> YMPE in dollars, 1966 through the latest announced year."
  {1966 5000, 1967 5000, 1968 5100, 1969 5200, 1970 5300
   1971 5400, 1972 5500, 1973 5900, 1974 6600, 1975 7400
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
  table. YMPE tracks average weekly earnings; ~3% is a reasonable
  long-run default and can be overridden via :assumptions."
  0.03)
