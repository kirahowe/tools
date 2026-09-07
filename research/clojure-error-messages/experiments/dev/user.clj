;; Loaded automatically by the Clojure runtime (RT.doInit) when it is on the classpath,
;; i.e. BEFORE clojure.main/repl starts -- so the alter-var-root in install! is seen by the REPL.
(require 'nice-errors.core)
(nice-errors.core/install!)
