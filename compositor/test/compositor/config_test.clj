(ns compositor.config-test
  (:require [clojure.test :refer [deftest is testing]]
            [compositor.config :as config]))

(deftest validate-rejects-bad-config
  (testing "agent-cmd must carry {prompt}"
    (is (thrown? clojure.lang.ExceptionInfo
                 (config/validate (assoc config/defaults :agent-cmd "claude")))))
  (testing "trunk required"
    (is (thrown? clojure.lang.ExceptionInfo
                 (config/validate (assoc config/defaults :trunk "")))))
  (testing "done-hook is an enum"
    (is (thrown? clojure.lang.ExceptionInfo
                 (config/validate (assoc config/defaults :agent-done-hook "banana")))))
  (testing "defaults are valid"
    (is (= config/defaults (config/validate config/defaults)))))

(deftest state-root-is-stable-and-scoped
  (let [a (config/state-root "/home/x/repo-a")
        b (config/state-root "/home/x/repo-b")]
    (is (= a (config/state-root "/home/x/repo-a")) "deterministic")
    (is (not= a b) "per-repo")
    (is (re-find #"/compositor/" a))))
