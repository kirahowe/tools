(ns clojure-wasm.server-test
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [clojure-wasm.server :as server]))

(deftest content-types
  (is (= "text/html; charset=utf-8" (server/content-type-for "/index.html")))
  (is (= "text/javascript; charset=utf-8" (server/content-type-for "/repl.js")))
  (is (= "application/java-archive" (server/content-type-for "/jars/clojure-1.12.5.jar")))
  (is (= "application/octet-stream" (server/content-type-for "/no-extension"))))

(deftest path-safety
  (is (server/safe-path? "/index.html"))
  (is (server/safe-path? "/jars/clojure-1.12.5.jar"))
  (is (not (server/safe-path? "/../deps.edn")))
  (is (not (server/safe-path? "/jars/../../secret")))
  (is (not (server/safe-path? "relative/path"))))

(deftest serves-index-at-root
  (let [res (server/handle-request {:method "GET" :uri "/"})]
    (is (= 200 (:status res)))
    (is (= "text/html; charset=utf-8" (:content-type res)))
    (is (str/includes? (String. ^bytes (:body res)) "Clojure in the browser"))))

(deftest serves-static-files
  (let [res (server/handle-request {:method "GET" :uri "/bootstrap.clj"})]
    (is (= 200 (:status res)))
    (is (str/includes? (String. ^bytes (:body res)) "browser.repl"))))

(deftest missing-files-404
  (is (= 404 (:status (server/handle-request {:method "GET" :uri "/nope.js"})))))

(deftest traversal-rejected
  (is (= 400 (:status (server/handle-request {:method "GET" :uri "/../deps.edn"})))))

(deftest non-get-rejected
  (is (= 405 (:status (server/handle-request {:method "DELETE" :uri "/index.html"})))))

(deftest healthz
  (is (= 200 (:status (server/handle-request {:method "GET" :uri "/healthz"})))))

(deftest eval-endpoint-matches-browser-contract
  (testing "POST /api/eval runs bootstrap.clj's eval-str on the server JVM"
    (let [res (server/handle-request {:method "POST" :uri "/api/eval" :body "(+ 20 22)"})]
      (is (= 200 (:status res)))
      (is (= "application/json" (:content-type res)))
      (is (str/includes? (:body res) "\"val\":\"42\"")))))

(deftest eval-cell-endpoint-is-kind-aware
  (let [res (server/handle-request {:method "POST" :uri "/api/eval-cell" :body "[{:a 1} {:a 2}]"})]
    (is (= 200 (:status res)))
    (is (str/includes? (:body res) "\"kind\":\"table\""))
    (is (str/includes? (:body res) "<table"))))

(deftest notebook-storage-roundtrip
  (let [id "server-test-notebook"
        doc "{\"title\":\"t\",\"cells\":[{\"type\":\"code\",\"source\":\"(+ 1 2)\"}]}"]
    (try
      (is (= 200 (:status (server/handle-request {:method "PUT" :uri (str "/api/notebooks/" id) :body doc}))))
      (let [res (server/handle-request {:method "GET" :uri (str "/api/notebooks/" id)})]
        (is (= 200 (:status res)))
        (is (= doc (:body res))))
      (is (str/includes? (:body (server/handle-request {:method "GET" :uri "/api/notebooks"}))
                         (str "\"" id "\"")))
      (finally
        (io/delete-file (io/file "data" "notebooks" (str id ".json")) true)))))

(deftest notebook-bad-ids-rejected
  (is (= 400 (:status (server/handle-request {:method "PUT" :uri "/api/notebooks/../evil" :body "{}"}))))
  (is (= 400 (:status (server/handle-request {:method "GET" :uri "/api/notebooks/No.Caps.Or.Dots"}))))
  (is (= 404 (:status (server/handle-request {:method "GET" :uri "/api/notebooks/does-not-exist"})))))
