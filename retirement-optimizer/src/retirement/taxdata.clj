(ns retirement.taxdata
  "Tax and benefit tables, loaded from EDN and pluggable per year.

  Each tax year is a self-contained EDN snapshot living at
  `resources/retirement/taxdata/<year>.edn` with the shape

    {:year 2025
     :federal {:brackets [...] :bpa {...} ...}
     :provinces {:on {...} :ns {...} :bc {...} :ab {...}}
     :benefits {:cpp {...} :oas {...} :gis {...} :tfsa {...}}
     :rrif-factors {71 0.0528 ...}}

  Selection: a plan year uses the latest table whose :year <= the plan
  year; the tax engine then indexes dollar thresholds by cumulative
  inflation from that table's :year (items marked :indexed? false stay
  fixed). So shipping 2025 + 2026 covers all later years by indexation,
  and dropping in a published 2027.edn (or passing one at runtime) makes
  2027+ exact.

  Plugging tables at runtime — no code changes needed:

    {:tax-tables {2025 {:provinces {:on {:bpa {:max 13000.0}}}}  ; patch
                  2027 (taxdata/read-table \"my-2027.edn\")}}    ; add

  in the plan inputs. User tables deep-merge over the built-ins per year,
  so a patch can be as small as one bracket, one province, or one credit."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def anchor-year
  "CPI anchor: cross-year inflation factors inside the engine are expressed
  relative to this year."
  2025)

(defn deep-merge
  [a b]
  (if (and (map? a) (map? b))
    (merge-with deep-merge a b)
    (if (nil? b) a b)))

(defn read-table
  "Read one tax-year table from an EDN source — anything `slurp` accepts:
  a path string, java.io.File, URL, or Reader."
  [source]
  (edn/read-string (slurp source)))

(def builtin-years [2025 2026])

(def builtin-tables
  (into (sorted-map)
        (map (fn [year]
               [year (read-table (io/resource (str "retirement/taxdata/" year ".edn")))]))
        builtin-years))

(def ^:private required-table-keys
  [:year :federal :provinces :benefits :rrif-factors])

(defn- check-complete [year table]
  (let [missing (remove #(contains? table %) required-table-keys)]
    (when (seq missing)
      (throw (ex-info (str "tax table for " year " is missing " (vec missing)
                           " — a table for a year without a built-in must be"
                           " complete (see resources/retirement/taxdata/)")
                      {:year year :missing (vec missing)}))))
  table)

(defn tables
  "The table set to run against: built-ins, optionally deep-merged (per
  year) with user overrides of the same shape. A patch to a built-in year
  can be arbitrarily small; a table for a new year must be complete.
  Returns a sorted map of year -> table."
  ([] builtin-tables)
  ([user-tables]
   (reduce-kv (fn [acc year override]
                (when-not (map? override)
                  (throw (ex-info ":tax-tables values must be maps"
                                  {:year year :value override})))
                (update acc year
                        (fn [builtin]
                          (check-complete year (deep-merge (or builtin {:year year})
                                                           override)))))
              builtin-tables
              (or user-tables {}))))

(defn resolve-table
  "Table to use for `year`: the latest table with :year <= year, falling
  back to the earliest available for years before all tables."
  [tables year]
  (let [ys (keys tables)
        at-or-before (take-while #(<= % year) ys)]
    (get tables (if (seq at-or-before) (last at-or-before) (first ys)))))

(defn rrif-minimum-factor
  "Prescribed RRIF minimum withdrawal factor at `age` under a table's
  :rrif-factors; below the youngest tabulated age it is 1/(90 - age)."
  [table age]
  (let [factors (:rrif-factors table)
        top-age (apply max (keys factors))
        low-age (apply min (keys factors))]
    (cond
      (>= age top-age) (get factors top-age)
      (>= age low-age) (get factors age)
      (>= age 90) (get factors top-age)
      :else (/ 1.0 (- 90 age)))))
