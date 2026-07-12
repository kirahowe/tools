(ns compositor.graph-test
  (:require [clojure.test :refer [deftest is]]
            [compositor.graph :as graph]))

(defn- fake-proj [sessions]
  {:cfg {:trunk "main"}
   :store {:cache (atom {:sessions (into {} (map (juxt :id identity) sessions))})}})

(deftest applied-parents-is-trunk-plus-applied
  (let [proj (fake-proj [{:id 1 :applied true  :change-id "aaa"}
                         {:id 2 :applied false :change-id "bbb"}
                         {:id 3 :applied true  :change-id "ccc"}])]
    (is (= ["main" "aaa" "ccc"] (graph/applied-parents proj))
        "unapplied session 2 is excluded; trunk always leads")))

(deftest applied-parents-trunk-only-when-nothing-applied
  (is (= ["main"] (graph/applied-parents (fake-proj [{:id 1 :applied false :change-id "x"}])))))
