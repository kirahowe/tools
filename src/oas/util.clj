(ns oas.util
  "Date and money helpers for the OAS library.

  Calendar months are represented as plain vectors of [year month]
  (e.g. [2026 7] for July 2026). All internal money arithmetic is done
  with exact rational numbers; amounts are rounded to cents only at the
  edges of the public API."
  (:import (java.math RoundingMode)))

;; --- months ----------------------------------------------------------------

(defn parse-ym
  "Coerce a calendar month from plain data. Accepts:
    - [year month] vectors, e.g. [2026 7]
    - {:year 2026 :month 7} maps
    - \"YYYY-MM\" or \"YYYY-MM-DD\" strings (day is ignored)
  Returns a [year month] vector, or nil for nil input.
  Throws ex-info for anything unparseable."
  [x]
  (letfn [(check [y m]
            (when-not (and (integer? y) (integer? m) (<= 1 m 12) (<= 1800 y 3000))
              (throw (ex-info "Invalid year/month" {:value x})))
            [(long y) (long m)])]
    (cond
      (nil? x) nil
      (vector? x) (check (first x) (second x))
      (map? x) (check (:year x) (:month x))
      (string? x) (if-let [[_ y m] (re-matches #"(\d{4})-(\d{2})(?:-\d{2})?" x)]
                    (check (parse-long y) (parse-long m))
                    (throw (ex-info "Expected \"YYYY-MM\" or \"YYYY-MM-DD\"" {:value x})))
      :else (throw (ex-info "Cannot parse a calendar month from value"
                            {:value x :type (type x)})))))

(defn ym->months
  "Absolute month index of a [year month] vector."
  [[y m]]
  (+ (* 12 y) (dec m)))

(defn months->ym
  "Inverse of ym->months."
  [n]
  [(quot n 12) (inc (rem n 12))])

(defn ym+
  "Add n months to a [year month] vector."
  [ym n]
  (months->ym (+ (ym->months ym) n)))

(defn months-between
  "Number of months from a to b (positive when b is after a)."
  [a b]
  (- (ym->months b) (ym->months a)))

(defn ym<= [a b] (<= (ym->months a) (ym->months b)))
(defn ym< [a b] (< (ym->months a) (ym->months b)))

(defn ym-max [a b] (if (ym<= a b) b a))
(defn ym-min [a b] (if (ym<= a b) a b))

(defn ym-range
  "All months from a to b inclusive."
  [a b]
  (map months->ym (range (ym->months a) (inc (ym->months b)))))

;; --- money -----------------------------------------------------------------

(defn exact
  "Convert any Clojure number (including doubles and BigDecimals) to an
  exact rational so downstream arithmetic never loses precision."
  [x]
  (rationalize x))

(defn round2
  "Round a number to a 2-decimal BigDecimal (half-up) — i.e. dollars and cents."
  ^BigDecimal [x]
  (let [r (rationalize x)]
    (if (ratio? r)
      (.divide (bigdec (numerator r)) (bigdec (denominator r)) 2 RoundingMode/HALF_UP)
      (.setScale (bigdec r) 2 RoundingMode/HALF_UP))))
