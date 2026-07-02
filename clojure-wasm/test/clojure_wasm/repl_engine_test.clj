(ns clojure-wasm.repl-engine-test
  "Tests the browser REPL engine (resources/public/bootstrap.clj) — the same
  code CheerpJ loads in the browser — by loading it into this JVM."
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing use-fixtures]]))

(def eval-str
  (delay
    (load-string (slurp (io/resource "public/bootstrap.clj")))
    @(requiring-resolve 'browser.repl/eval-str)))

(use-fixtures :each
  (fn [f]
    ;; Each test starts from the `user` namespace.
    (@eval-str "(in-ns 'user)")
    (f)))

(deftest simple-value
  (is (= "{\"tag\":\"ret\",\"val\":\"3\",\"out\":\"\",\"ns\":\"user\"}"
         (@eval-str "(+ 1 2)"))))

(deftest defn-persists-across-calls
  (@eval-str "(defn square [x] (* x x))")
  (is (str/includes? (@eval-str "(square 12)") "\"val\":\"144\"")))

(deftest multiple-forms-return-last
  (is (str/includes? (@eval-str "(def a 20) (def b 22) (+ a b)") "\"val\":\"42\"")))

(deftest captures-out
  (let [res (@eval-str "(println \"hello from the JVM\") :done")]
    (is (str/includes? res "\"out\":\"hello from the JVM\\n\""))
    (is (str/includes? res "\"val\":\":done\""))))

(deftest json-escaping
  (testing "quotes, backslashes, and newlines in output survive JSON encoding"
    (let [res (@eval-str "(println \"say \\\"hi\\\"\\nback\\\\slash\")")]
      (is (str/includes? res "say \\\"hi\\\"\\nback\\\\slash")))))

(deftest errors-are-tagged
  (let [res (@eval-str "(/ 1 0)")]
    (is (str/includes? res "\"tag\":\"err\""))
    (is (str/includes? res "ArithmeticException"))))

(deftest reader-errors-are-tagged
  (is (str/includes? (@eval-str "(unbalanced") "\"tag\":\"err\"")))

(deftest ns-changes-persist
  (let [in-demo (@eval-str "(in-ns 'demo)")]
    (is (str/includes? in-demo "\"ns\":\"demo\""))
    (is (str/includes? (@eval-str "(clojure.core/+ 40 2)") "\"ns\":\"demo\""))
    (is (str/includes? (@eval-str "(in-ns 'user)") "\"ns\":\"user\""))))

(deftest infinite-seqs-are-truncated
  (let [res (@eval-str "(range)")]
    (is (str/includes? res "999"))
    (is (str/includes? res "..."))))
