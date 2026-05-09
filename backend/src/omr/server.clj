(ns omr.server
  "Imperative shell: read config, pick engine fn, start http-kit."
  (:require [omr.engine.oemer :as oemer]
            [omr.handler :as handler]
            [org.httpkit.server :as http])
  (:gen-class))

;; Engine registry. Adding SMT++ on Modal = one new entry whose value is a
;; function with the same `(bytes, opts) -> musicxml-string` shape.
(def engines
  {:oemer-local oemer/recognize})

(defn- env [k default]
  (or (System/getenv k) default))

(defn- pick-engine [engine-key]
  (or (get engines engine-key)
      (throw (ex-info (str "Unknown OMR_ENGINE: " engine-key)
                      {:engine engine-key
                       :available (keys engines)}))))

(defn -main [& _]
  (let [engine-key  (keyword (env "OMR_ENGINE" "oemer-local"))
        port        (Integer/parseInt (env "PORT" "8080"))
        recognize   (pick-engine engine-key)
        app         (handler/make-app recognize)]
    (println (str "Starting omr server on :" port " using engine " engine-key))
    (http/run-server app {:port port :legacy-return-value? false})
    @(promise)))
