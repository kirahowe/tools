(ns clojure-wasm.notebook-engine-test
  "Tests the notebook cell engine (resources/public/notebook-engine.cljc) by
  loading it the same way the browser and server do: through
  browser.repl/eval-str, which reads with {:read-cond :allow}."
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing use-fixtures]]))

(def eval-cell
  (delay
    (load-string (slurp (io/resource "public/bootstrap.clj")))
    (let [eval-str @(requiring-resolve 'browser.repl/eval-str)
          res (eval-str (slurp (io/resource "public/notebook-engine.cljc")))]
      (assert (str/includes? res "notebook.engine ready") res))
    @(requiring-resolve 'notebook.engine/eval-cell)))

(use-fixtures :each
  (fn [f]
    (@eval-cell "(in-ns 'user)")
    (f)))

(deftest plain-values
  (is (str/includes? (@eval-cell "(+ 1 2)") "\"kind\":\"value\""))
  (is (str/includes? (@eval-cell "(+ 1 2)") "\"val\":\"3\""))
  (is (str/includes? (@eval-cell "nil") "\"kind\":\"nil\"")))

(deftest seq-of-maps-renders-as-table
  (let [res (@eval-cell "[{:a 1 :b 2} {:a 3 :b 4}]")]
    (is (str/includes? res "\"kind\":\"table\""))
    (is (str/includes? res "<table"))
    (is (str/includes? res "<th>a</th>"))
    (is (str/includes? res "<td>3</td>"))))

(deftest hiccup-renders-as-html
  (let [res (@eval-cell "[:div.card {:title \"hi\"} [:p \"hello \" [:b \"world\"]]]")]
    (is (str/includes? res "\"kind\":\"hiccup\""))
    (is (str/includes? res "class=\\\"card\\\""))
    (is (str/includes? res "<b>world</b>"))))

(deftest hiccup-escapes-content
  (let [res (@eval-cell "[:p \"<script>alert(1)</script>\"]")]
    (is (not (str/includes? res "<script>")))
    (is (str/includes? res "&lt;script&gt;"))))

(deftest vega-detected-by-schema
  (let [res (@eval-cell "{:$schema \"https://vega.github.io/schema/vega-lite/v5.json\"
                          :mark \"bar\"
                          :data {:values [{:x 1 :y 2}]}}")]
    (is (str/includes? res "\"kind\":\"vega\""))
    (is (str/includes? res "\"mark\":\"bar\""))))

(deftest kind-helpers-override-detection
  (testing "a seq of maps forced to plain value"
    (is (str/includes? (@eval-cell "(kind/value [{:a 1}])") "\"kind\":\"value\"")))
  (testing "metadata survives lazy-seq realization"
    (is (str/includes? (@eval-cell "(kind/value (map identity [{:a 1} {:a 2}]))")
                       "\"kind\":\"value\"")))
  (testing "explicit table"
    (is (str/includes? (@eval-cell "(kind/table (map #(hash-map :n %) (range 3)))")
                       "\"kind\":\"table\"")))
  (testing "markdown helper"
    (let [res (@eval-cell "(kind/md \"# Title\")")]
      (is (str/includes? res "\"kind\":\"md\""))
      (is (str/includes? res "# Title"))))
  (testing "kindly-style namespaced metadata"
    (is (str/includes? (@eval-cell "^{:kindly/kind :kind/hiccup} [:p \"x\"]")
                       "\"kind\":\"hiccup\""))))

(deftest output-and-ns-flow-through
  (let [res (@eval-cell "(println \"side effect\") 42")]
    (is (str/includes? res "\"out\":\"side effect\\n\""))
    (is (str/includes? res "\"val\":\"42\"")))
  (is (str/includes? (@eval-cell "(in-ns 'nb-demo)") "\"ns\":\"nb-demo\""))
  (@eval-cell "(clojure.core/in-ns 'user)"))

(deftest errors-tagged
  (let [res (@eval-cell "(throw (ex-info \"boom\" {}))")]
    (is (str/includes? res "\"tag\":\"err\""))
    (is (str/includes? res "boom"))))

(deftest infinite-seqs-safe
  (is (str/includes? (@eval-cell "(range)") "...")))

(deftest state-isolated-from-repl-engine
  (@eval-cell "(in-ns 'only-notebook)")
  (let [repl-eval @(requiring-resolve 'browser.repl/eval-str)]
    (is (str/includes? (repl-eval ":check") "\"ns\":\"user\"")))
  (@eval-cell "(clojure.core/in-ns 'user)"))
