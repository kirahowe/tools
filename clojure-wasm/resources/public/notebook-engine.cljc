;; Kind-aware cell evaluation engine for the notebook prototype.
;;
;; One file, three runtimes:
;;   - the in-tab CheerpJ JVM  (loaded through browser.repl/eval-str, which
;;     reads with {:read-cond :allow})
;;   - the dev server JVM      (same loading path, backs POST /api/eval-cell)
;;   - scittle/SCI (CLJS)      (experimental instant kernel)
;;
;; notebook.engine/eval-cell : code string -> JSON string
;;   tag  "ret" | "err"
;;   out  captured *out*/*err*
;;   ns   current namespace after evaluation
;;   kind "value" | "table" | "hiccup" | "vega" | "md" | "nil"
;;   val  pr-str of the value (value/nil kinds, or the error message)
;;   html rendered HTML        (table/hiccup kinds)
;;   vega the vega-lite spec as JSON  (vega kind)
;;   md   raw markdown text    (md kind)
;;
;; Values choose their rendering the way kindly does: explicitly via
;; metadata — (kind/table v), (kind/hiccup v), ^{:kind :vega} {...} — or
;; implicitly: hiccup-shaped vectors, seqs of maps (tables), maps with
;; :$schema (vega-lite).

(ns notebook.engine
  (:require [clojure.string :as str]))

(defonce state (atom {:ns 'user}))

;; --- portable JSON encoding ------------------------------------------------

(defn- char-code [c]
  #?(:clj (int c) :cljs (.charCodeAt c 0)))

(defn- hex4 [i]
  #?(:clj (format "\\u%04x" i)
     :cljs (str "\\u" (.padStart (.toString i 16) 4 "0"))))

(defn- json-escape [s]
  (str/join
   (map (fn [c]
          (let [i (char-code c)]
            (cond
              (= c \") "\\\""
              (= c \\) "\\\\"
              (= c \newline) "\\n"
              (= c \return) "\\r"
              (= c \tab) "\\t"
              (< i 32) (hex4 i)
              :else c)))
        s)))

(defn- finite? [n]
  #?(:clj (and (not (and (double? n) (Double/isNaN n)))
               (not (and (double? n) (Double/isInfinite n))))
     :cljs (js/isFinite n)))

(defn to-json
  "Small recursive JSON encoder (maps, sequentials, numbers, booleans, nil,
  strings; keywords/symbols become their names). Enough for cell results
  and vega-lite specs — not a general-purpose encoder."
  [v]
  (cond
    (nil? v) "null"
    (true? v) "true"
    (false? v) "false"
    (and (number? v) (finite? v)) (str v)
    (map? v) (str "{"
                  (str/join "," (map (fn [[k val]]
                                       (str "\"" (json-escape (if (or (keyword? k) (symbol? k)) (name k) (str k))) "\":"
                                            (to-json val)))
                                     v))
                  "}")
    (sequential? v) (str "[" (str/join "," (map to-json v)) "]")
    :else (str "\"" (json-escape (if (or (keyword? v) (symbol? v)) (name v) (str v))) "\"")))

;; --- HTML rendering ---------------------------------------------------------

(defn- html-escape [s]
  (str/escape (str s) {\< "&lt;" \> "&gt;" \& "&amp;" \" "&quot;"}))

(defn- attrs->str [attrs]
  (str/join (map (fn [[k v]]
                   (str " " (name k) "=\"" (html-escape (if (keyword? v) (name v) (str v))) "\""))
                 attrs)))

(defn hiccup->html
  "Minimal hiccup renderer: keyword tags with .class/#id shorthand, attr
  maps, nested vectors/seqs, everything else escaped text."
  [h]
  (cond
    (vector? h)
    (let [[tag & body] h
          [attrs body] (if (map? (first body)) [(first body) (rest body)] [{} body])
          tag-str (name tag)
          [tag-name & mods] (str/split tag-str #"(?=[.#])")
          classes (keep #(when (str/starts-with? % ".") (subs % 1)) mods)
          id (some #(when (str/starts-with? % "#") (subs % 1)) mods)
          attrs (cond-> attrs
                  id (assoc :id id)
                  (seq classes) (update :class #(str/trim (str (str/join " " classes) " " (or % "")))))]
      (str "<" tag-name (attrs->str attrs) ">"
           (str/join (map hiccup->html body))
           "</" tag-name ">"))

    (seq? h) (str/join (map hiccup->html h))
    (nil? h) ""
    :else (html-escape h)))

(def ^:private max-table-rows 500)

(defn table-html
  "Seq of maps -> HTML table (capped at 500 rows)."
  [rows]
  (let [rows (vec (take (inc max-table-rows) rows))
        truncated? (> (count rows) max-table-rows)
        rows (if truncated? (subvec rows 0 max-table-rows) rows)
        cols (distinct (mapcat keys rows))
        cell (fn [v] (str "<td>" (html-escape (if (string? v) v (pr-str v))) "</td>"))]
    (str "<table class=\"nb-table\"><thead><tr>"
         (str/join (map #(str "<th>" (html-escape (if (keyword? %) (name %) (str %))) "</th>") cols))
         "</tr></thead><tbody>"
         (str/join (map (fn [row] (str "<tr>" (str/join (map #(cell (get row %)) cols)) "</tr>")) rows))
         "</tbody></table>"
         (when truncated? (str "<p class=\"nb-note\">… truncated at " max-table-rows " rows</p>")))))

;; --- kind detection ---------------------------------------------------------

(defn kind-of [v]
  ;; kindly writes namespaced kinds (:kind/hiccup); normalize to bare keywords
  (or (some-> (meta v) (as-> m (or (:kindly/kind m) (:kind m))) name keyword)
      (cond
        (nil? v) :nil
        (and (map? v) (or (contains? v :$schema) (contains? v "$schema"))) :vega
        (and (vector? v) (keyword? (first v))) :hiccup
        (and (sequential? v) (seq v) (every? map? (take 20 v))) :table
        :else :value)))

;; --- evaluation -------------------------------------------------------------

(defn- realize
  "Make lazy seqs safe to classify and print (bounds infinite seqs).
  take returns a fresh seq, so re-attach metadata — kind/* helpers rely
  on it surviving."
  [v]
  (if (seq? v)
    (with-meta (doall (take 1001 v)) (meta v))
    v))

(defn- error-message [t]
  #?(:clj (let [root (loop [t t] (if-let [c (.getCause ^Throwable t)] (recur c) t))]
            (str (.getName (class root))
                 (when-let [m (ex-message root)] (str ": " m))))
     :cljs (str t)))

#?(:clj
   (defn- eval-forms [code]
     (let [out (java.io.StringWriter.)]
       (binding [*ns* (or (find-ns (:ns @state)) (the-ns 'user))
                 *out* out
                 *err* out]
         (let [res (try
                     (let [rdr (clojure.lang.LineNumberingPushbackReader.
                                (java.io.StringReader. code))
                           eof (Object.)]
                       (loop [ret nil]
                         (let [form (read {:eof eof :read-cond :allow} rdr)]
                           (if (identical? form eof)
                             {:tag "ret" :value (realize ret)}
                             (recur (eval form))))))
                     (catch Throwable t
                       {:tag "err" :error (error-message t)}))]
           (swap! state assoc :ns (ns-name *ns*))
           (assoc res :out (str out) :nsname (str (ns-name *ns*)))))))
   :cljs
   (defn- eval-forms [code]
     (let [value (atom nil)
           res (try
                 (let [out (with-out-str (reset! value (load-string code)))]
                   {:tag "ret" :value (realize @value) :out out})
                 (catch :default e
                   {:tag "err" :error (error-message e) :out ""}))]
       (assoc res :nsname "user"))))

(defn- pr-str-limited [v]
  (binding [*print-length* 1000]
    (try (pr-str v)
         (catch #?(:clj Throwable :cljs :default) t
           (str "<unprintable: " (error-message t) ">")))))

(defn eval-cell
  "Evaluate one notebook cell; returns the JSON contract described above."
  [code]
  (let [{:keys [tag value error out nsname]} (eval-forms code)]
    (if (= tag "err")
      (to-json {:tag "err" :kind "value" :val error :out out :ns nsname})
      (let [base {:tag "ret" :out out :ns nsname}
            k (try (kind-of value) (catch #?(:clj Throwable :cljs :default) _ :value))]
        (to-json
         (merge base
                (case k
                  :nil {:kind "nil" :val "nil"}
                  :hiccup (try {:kind "hiccup" :html (hiccup->html value)}
                               (catch #?(:clj Throwable :cljs :default) t
                                 {:kind "value" :val (str "<hiccup render failed: " (error-message t) "> "
                                                          (pr-str-limited value))}))
                  :table (try {:kind "table" :html (table-html value)}
                              (catch #?(:clj Throwable :cljs :default) t
                                {:kind "value" :val (str "<table render failed: " (error-message t) "> "
                                                         (pr-str-limited value))}))
                  :vega {:kind "vega" :vega value}
                  :md {:kind "md" :md (if (sequential? value) (str/join "\n" (map str value)) (str value))}
                  {:kind "value" :val (pr-str-limited value)})))))))

;; --- kindly-style helpers, available to cells as kind/... -------------------

(ns kind)

(defn hiccup [v] (vary-meta v assoc :kind :hiccup))
(defn table [v] (vary-meta v assoc :kind :table))
(defn vega [v] (vary-meta v assoc :kind :vega))
(defn value [v] (vary-meta v assoc :kind :value))
(defn md
  "Markdown. Strings can't carry metadata, so wrap: (kind/md \"# title\")."
  [s]
  (vary-meta (if (sequential? s) (vec s) [s]) assoc :kind :md))

;; Set up the user namespace like a REPL would, and leave the loader's
;; current namespace there (not in `kind`) so loading this file through
;; browser.repl/eval-str doesn't strand the REPL in the kind namespace.
#?(:clj (binding [*ns* (create-ns 'user)]
          (refer-clojure)))
#?(:clj (clojure.core/in-ns 'user))

"notebook.engine ready"
