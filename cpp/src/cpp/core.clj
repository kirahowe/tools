(ns cpp.core
  "Public API for estimating Canada Pension Plan retirement benefits.

  The single entry point is `estimate`, which takes a plain EDN map
  describing a contributor and returns the expected pension as data.
  See the README for the full input/output specification."
  (:require [cpp.calc :as calc]
            [cpp.data :as data]
            [cpp.dates :as d]))

(defn estimate
  "Estimate the CPP retirement pension for one contributor.

  Required:
    :birth-date  \"YYYY-MM\" | {:year Y :month M}
    :pensionable-earnings {year earnings ...}   ; annual gross, capped
      -- or --
    :contributions {year employee-contribution ...} ; converted for you

  Optional:
    :start        \"YYYY-MM\" | {:years 63 :months 4} (age). Default:
                  the month after the 65th birthday.
    :children     [{:born \"YYYY-MM\"} ...] child-rearing provisions
    :disability   [{:from \"YYYY-MM\" :to \"YYYY-MM\"} ...] periods on
                  CPP disability
    :assumptions  {:wage-growth 0.03} projection past the known table

  Returns a map: {:monthly {:base :first-additional :second-additional
  :total} :annual ... :adjustment ... :details ...} — all amounts in
  dollars of the start year."
  [person]
  (calc/estimate* person))
