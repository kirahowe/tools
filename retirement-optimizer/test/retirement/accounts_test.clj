(ns retirement.accounts-test
  (:require [clojure.test :refer [deftest is testing]]
            [retirement.accounts :as acct]
            [retirement.taxdata :as data]
            [retirement.test-util :refer [approx=]]))

(def nonreg {:id :t :type :non-registered :balance 100000.0 :acb 60000.0
             :holdings {:equity 1.0}})

(def table (data/resolve-table (data/tables) 2025))

(deftest classes
  (is (= :registered (acct/account-class {:type :rrsp})))
  (is (= :registered (acct/account-class {:type :rrif})))
  (is (= :tfsa (acct/account-class {:type :tfsa})))
  (is (= :non-registered (acct/account-class {:type :non-registered}))))

(deftest rrif-minimums
  (testing "RRSP has no minimum before conversion"
    (is (zero? (acct/rrif-minimum {:type :rrsp :balance 100000.0} 70 table)))
    (is (zero? (acct/rrif-minimum {:type :rrsp :balance 100000.0} 71 table))))
  (testing "converted RRSP owes its first minimum at 72"
    (is (approx= (* 100000 0.054)
                 (acct/rrif-minimum {:type :rrsp :balance 100000.0} 72 table))))
  (testing "an account already held as a RRIF has minimums at any age"
    (is (approx= (* 100000 0.04)
                 (acct/rrif-minimum {:type :rrif :balance 100000.0} 65 table))))
  (testing "TFSA and non-registered never have minimums"
    (is (zero? (acct/rrif-minimum {:type :tfsa :balance 100000.0} 80 table)))
    (is (zero? (acct/rrif-minimum nonreg 80 table)))))

(deftest withdraw-non-registered-tracks-acb
  (let [{:keys [account amount realized-gain taxable-ordinary]}
        (acct/withdraw nonreg 25000.0 70)]
    (is (approx= 25000.0 amount))
    ;; gain fraction = 40k/100k -> 10k realized on a 25k sale
    (is (approx= 10000.0 realized-gain))
    (is (zero? taxable-ordinary))
    (is (approx= 75000.0 (:balance account)))
    (is (approx= 45000.0 (:acb account)))))

(deftest withdraw-clamps-to-balance
  (let [{:keys [amount account]} (acct/withdraw nonreg 500000.0 70)]
    (is (approx= 100000.0 amount))
    (is (approx= 0.0 (:balance account)))))

(deftest withdraw-registered-is-ordinary-income
  (let [{:keys [realized-gain taxable-ordinary]}
        (acct/withdraw {:type :rrif :balance 50000.0 :holdings {}} 20000.0 72)]
    (is (approx= 20000.0 taxable-ordinary))
    (is (zero? realized-gain))))

(deftest withdraw-tfsa-is-tax-free
  (let [{:keys [realized-gain taxable-ordinary]}
        (acct/withdraw {:type :tfsa :balance 50000.0 :holdings {}} 20000.0 72)]
    (is (zero? taxable-ordinary))
    (is (zero? realized-gain))))

(deftest acb-loss-position-realizes-no-gain
  (let [underwater (assoc nonreg :acb 150000.0)]
    (is (zero? (:realized-gain (acct/withdraw underwater 10000.0 70))))))

(deftest portfolio-return-weighted
  (is (approx= (+ (* 0.6 0.08) (* 0.4 0.02))
               (acct/portfolio-return {:equity 0.6 :bonds 0.4}
                                      {:equity 0.08 :bonds 0.02}))))

(deftest distributions-only-from-non-registered
  (let [opts {:dividend-yield 0.02 :interest-yield 0.03}
        d (acct/distribution-yields
           {:type :non-registered :balance 100000.0
            :holdings {:equity 0.5 :bonds 0.3 :cash 0.2}} opts)]
    (is (approx= (* 100000 0.5 0.02) (:eligible-dividends d)))
    (is (approx= (* 100000 0.5 0.03) (:interest d)))
    (is (zero? (:eligible-dividends
                (acct/distribution-yields
                 {:type :rrsp :balance 100000.0 :holdings {:equity 1.0}} opts))))))

(deftest growth-nets-out-cash-distributions
  (testing "non-registered: distributions are paid out, only price change compounds"
    (let [grown (acct/grow (assoc nonreg :holdings {:equity 1.0})
                           {:equity 0.08}
                           {:dividend-yield 0.02 :interest-yield 0.03})]
      (is (approx= 106000.0 (:balance grown)))
      (is (approx= 60000.0 (:acb grown)))))
  (testing "registered: full return compounds"
    (let [grown (acct/grow {:type :rrsp :balance 100000.0 :holdings {:equity 1.0}}
                           {:equity 0.08}
                           {:dividend-yield 0.02 :interest-yield 0.03})]
      (is (approx= 108000.0 (:balance grown))))))

(deftest contribute-raises-acb-for-non-registered
  (let [c (acct/contribute nonreg 10000.0)]
    (is (approx= 110000.0 (:balance c)))
    (is (approx= 70000.0 (:acb c))))
  (let [c (acct/contribute {:type :tfsa :balance 5000.0} 1000.0)]
    (is (approx= 6000.0 (:balance c)))
    (is (nil? (:acb c)))))

(deftest unrealized-gains-sum
  (is (approx= 40000.0 (acct/unrealized-gains [nonreg {:type :rrsp :balance 99.0}]))))
