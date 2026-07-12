(ns compositor.jj-test
  "Tests for the parts of jj.clj that don't need a repo — chiefly the
   serialization queue, which is the daemon's whole concurrency story."
  (:require [clojure.test :refer [deftest is testing]]
            [compositor.jj :as jj]))

(deftest queue-returns-values-and-runs-every-thunk
  (let [{:keys [submit stop]} (jj/queue)
        n 200
        results (mapv (fn [i] (future (submit (fn [] i)))) (range n))]
    (try
      (is (= (set (range n)) (set (map deref results)))
          "every submitted thunk runs and returns its value")
      (finally (stop)))))

(deftest queue-rethrows-on-caller
  (let [{:keys [submit stop]} (jj/queue)]
    (try
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"boom"
            (submit (fn [] (throw (ex-info "boom" {}))))))
      (testing "queue survives a thrown thunk and keeps serving"
        (is (= 42 (submit (fn [] 42)))))
      (finally (stop)))))

(deftest queue-serializes-mutations
  ;; A single consumer means thunks never overlap. Guard a deliberately
  ;; non-atomic read-modify-write with nothing but the queue; if it serializes,
  ;; the final count is exact.
  (let [{:keys [submit stop]} (jj/queue)
        box (volatile! 0)
        n 500
        fs (mapv (fn [_] (future (submit (fn [] (vswap! box inc))))) (range n))]
    (try
      (run! deref fs)
      (is (= n @box) "no lost updates ⇒ thunks ran one at a time")
      (finally (stop)))))
