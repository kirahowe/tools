;; REPL engine for the in-browser JVM.
;;
;; This file is fetched by repl.js and loaded (once) into the CheerpJ JVM via
;; clojure.core/load-string. After that, each REPL submission is a single call
;; to browser.repl/eval-str, which returns a JSON string so that only
;; primitives cross the JS <-> JVM boundary.
;;
;; The same file is loaded by the dev server (clojure-wasm.server) to back the
;; POST /api/eval endpoint, so `?mode=server` and e2e tests exercise exactly
;; this code.
;;
;; JSON contract, all values strings:
;;   tag  "ret" | "err"
;;   val  pr-str of the last evaluated form's value, or the error message
;;   out  everything printed to *out* / *err* during evaluation
;;   ns   the current namespace after evaluation (in-ns/ns changes persist)
(ns browser.repl
  (:require [clojure.string :as str]))

(defonce state (atom {:ns 'user}))

(defn- json-escape [^String s]
  (str/join
   (map (fn [c]
          (let [i (int c)]
            (cond
              (= c \") "\\\""
              (= c \\) "\\\\"
              (= c \newline) "\\n"
              (= c \return) "\\r"
              (= c \tab) "\\t"
              (< i 32) (format "\\u%04x" i)
              :else c)))
        s)))

(defn- json-obj [kvs]
  (str "{"
       (str/join "," (map (fn [[k v]]
                            (str "\"" (name k) "\":\"" (json-escape (str v)) "\""))
                          kvs))
       "}"))

(defn- error-message [^Throwable t]
  (let [root (loop [t t] (if-let [c (.getCause t)] (recur c) t))]
    (str (.getName (class root))
         (when-let [m (.getMessage root)] (str ": " m)))))

(defn eval-str
  "Read and evaluate every form in `code`, REPL-style, returning a JSON string
  (see contract above). Keeps the current namespace in `state` between calls
  so (in-ns ...) and (ns ...) behave like a normal REPL. *print-length* is
  bound to 1000 so printing infinite seqs doesn't wedge the browser tab;
  override per-form with e.g. (take 5 (range)) or (set! *print-length* nil)."
  [code]
  (let [out (java.io.StringWriter.)]
    (binding [*ns* (or (find-ns (:ns @state)) (the-ns 'user))
              *out* out
              *err* out
              *print-length* 1000]
      (let [result (try
                     (let [rdr (clojure.lang.LineNumberingPushbackReader.
                                (java.io.StringReader. code))
                           eof (Object.)]
                       (loop [ret nil]
                         (let [form (read {:eof eof :read-cond :allow} rdr)]
                           (if (identical? form eof)
                             {:tag "ret" :val (pr-str ret)}
                             (recur (eval form))))))
                     (catch Throwable t
                       {:tag "err" :val (error-message t)}))]
        (swap! state assoc :ns (ns-name *ns*))
        (json-obj [[:tag (:tag result)]
                   [:val (:val result)]
                   [:out (str out)]
                   [:ns (ns-name *ns*)]])))))

;; Set up the `user` namespace like a normal REPL would.
(binding [*ns* (create-ns 'user)]
  (refer-clojure))

"browser.repl ready"
