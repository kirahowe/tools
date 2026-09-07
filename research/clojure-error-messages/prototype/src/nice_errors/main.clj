(ns nice-errors.main
  "Alternative entry point: `clj -M -m nice-errors.main` starts a REPL whose :caught is ours."
  (:require [clojure.main :as main]
            [nice-errors.core :as ne]))

(defn -main [& _]
  (ne/install!)
  (main/repl :init #(apply require main/repl-requires)
             :caught ne/print-error))
