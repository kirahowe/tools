;; Frame composition of the :clojure.main/trace in each script-mode report file (bb report_stats.clj out/default)
(require '[clojure.edn :as edn] '[clojure.java.io :as io] '[clojure.string :as str])
(def out-dir (or (first *command-line-args*) "out/default"))
(defn cat-of [cls]
  (let [c (str cls)]
    (cond (str/starts-with? c "bugs.") :user
          (str/starts-with? c "clojure.core$") :clojure.core
          (str/starts-with? c "clojure.main") :clojure.main
          (str/starts-with? c "clojure.spec") :clojure.spec
          (str/starts-with? c "clojure.lang.") :clojure.lang
          (re-find #"^(java|jdk|sun)\." c) :jdk
          :else :other)))
(def rows
  (for [f (sort (filter #(str/ends-with? (.getName %) ".report.edn") (file-seq (io/file out-dir))))]
    (let [r (edn/read-string {:default (fn [_ v] v)} (slurp f))
          trace (get-in r [:clojure.main/trace :trace])
          cats (map (comp cat-of first) trace)
          first-user (first (keep-indexed (fn [i c] (when (= c :user) (inc i))) cats))]
      (merge {:bug (str/replace (.getName f) ".report.edn" "") :frames (count trace) :first-user first-user
              :via (count (get-in r [:clojure.main/trace :via]))}
             (frequencies cats)))))
(println "| bug | frames | exceptions in :via | first user frame | user | clojure.core | clojure.lang | clojure.main | spec | jdk |")
(println "|---|---|---|---|---|---|---|---|---|---|")
(doseq [{:keys [bug frames via first-user user clojure.core clojure.lang clojure.main clojure.spec jdk] :or {user 0 clojure.core 0 clojure.lang 0 clojure.main 0 clojure.spec 0 jdk 0}} rows]
  (println (format "| %s | %d | %d | %s | %d | %d | %d | %d | %d | %d |" bug frames via (or first-user "-") user clojure.core clojure.lang clojure.main clojure.spec jdk)))
(let [tot (reduce + (map :frames rows)) usr (reduce + (map #(:user % 0) rows))]
  (println (format "\nTOTAL frames=%d user=%d (%.1f%%)" tot usr (* 100.0 (/ usr tot)))))
