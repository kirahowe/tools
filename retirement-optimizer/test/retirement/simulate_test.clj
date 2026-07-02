(ns retirement.simulate-test
  (:require [clojure.test :refer [deftest is testing]]
            [retirement.inputs :as inputs]
            [retirement.simulate :as simulate]
            [retirement.test-util :refer [approx= basic-inputs]]))

(deftest cholesky-reconstructs-the-matrix
  (let [m (simulate/correlation-matrix
           (:correlations inputs/default-assumptions))
        l (simulate/cholesky m)
        n (count m)
        product (for [i (range n) j (range n)]
                  (reduce + 0.0 (map #(* (get-in l [i %]) (get-in l [j %]))
                                     (range n))))]
    (doseq [[expected actual] (map vector (flatten m) product)]
      (is (approx= expected actual 1e-9)))
    (testing "non-positive-definite matrix throws"
      (is (thrown? clojure.lang.ExceptionInfo
                   (simulate/cholesky [[1.0 2.0] [2.0 1.0]]))))))

(deftest trial-paths-are-reproducible-and-well-formed
  (let [assumptions inputs/default-assumptions
        path (simulate/trial-path assumptions 30 42 7)]
    (is (= 30 (count path)))
    (is (every? #(= #{:equity :bonds :cash :inflation} (set (keys %))) path))
    (testing "returns can never fall below -100%"
      (is (every? #(> (:equity %) -1.0) path)))
    (is (= path (simulate/trial-path assumptions 30 42 7)))
    (is (not= path (simulate/trial-path assumptions 30 43 7)))
    (is (not= path (simulate/trial-path assumptions 30 42 8)))))

(deftest simulation-is-deterministic
  (let [opts {:trials 100 :seed 11}]
    (is (= (simulate/simulate basic-inputs opts)
           (simulate/simulate basic-inputs opts)))
    (testing "parallel and sequential runs agree"
      (is (= (simulate/simulate basic-inputs opts)
             (simulate/simulate basic-inputs (assoc opts :parallel? false)))))))

(deftest extreme-portfolios-pin-the-success-rate
  (let [person {:age 65 :province :on :oas {:start-age 65 :fraction 0.0}}]
    (testing "vast portfolio, modest spending: always succeeds"
      (is (= 1.0 (:success-rate
                  (simulate/simulate
                   {:person person
                    :accounts [{:id :tfsa :type :tfsa :balance 1.0e7
                                :holdings {:equity 0.3 :bonds 0.7}}]
                    :goal {:type :spend-down :annual-spending 40000}
                    :start-year 2026}
                   {:trials 200 :seed 3})))))
    (testing "tiny portfolio, big spending: always fails"
      (is (= 0.0 (:success-rate
                  (simulate/simulate
                   {:person person
                    :accounts [{:id :tfsa :type :tfsa :balance 50000.0
                                :holdings {:equity 0.5 :bonds 0.5}}]
                    :goal {:type :spend-down :annual-spending 80000}
                    :start-year 2026}
                   {:trials 200 :seed 3})))))))

(deftest success-rate-monotone-in-spending
  (let [rate (fn [spending]
               (:success-rate
                (simulate/simulate
                 (assoc-in basic-inputs [:goal :annual-spending] spending)
                 {:trials 200 :seed 5})))]
    (is (>= (rate 35000) (rate 55000) (rate 90000)))))

(deftest output-shape-and-ordering
  (let [result (simulate/simulate basic-inputs {:trials 150 :seed 2})]
    (is (= 150 (:trials result)))
    (is (<= 0.0 (:success-rate result) 1.0))
    (testing "estate percentiles are ordered"
      (let [{:keys [p5 p25 p50 p75 p95]} (:estate-real result)]
        (is (<= p5 p25 p50 p75 p95))))
    (testing "yearly balance bands are ordered and span the horizon"
      (let [{:keys [ages balance-real]} (:yearly result)]
        (is (= 31 (count ages)))
        (is (= 65 (first ages)))
        (doseq [i (range (count ages))]
          (is (<= (get-in balance-real [:p5 i])
                  (get-in balance-real [:p50 i])
                  (get-in balance-real [:p95 i]))))))
    (testing "ruin probability complements the success rate"
      (is (approx= (:success-rate result)
                   (- 1.0 (get-in result [:ruin :probability]))
                   1e-9)))))

(deftest sustainable-spending-meets-its-target
  (let [result (simulate/sustainable-spending
                basic-inputs {:target 0.9 :trials 150 :seed 4 :resolution 1000.0})]
    (is (pos? (:annual-spending result)))
    (is (>= (:success-rate result) 0.9))
    (testing "spending a lot more would miss the target"
      (let [pushed (:success-rate
                    (simulate/simulate
                     (assoc-in basic-inputs [:goal :annual-spending]
                               (+ 20000.0 (:annual-spending result)))
                     {:trials 150 :seed 4}))]
        (is (< pushed 0.9))))))
