(require 'org.corfield.rephrase)
(alter-var-root #'clojure.main/repl-caught (constantly org.corfield.rephrase/repl-caught))
