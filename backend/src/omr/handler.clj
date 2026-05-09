(ns omr.handler
  "Functional core: request -> response. Engine is just a function passed in."
  (:require [clojure.string :as str]
            [reitit.ring :as ring]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.parameters :as parameters]))

(def ^:private cors-headers
  ;; Static frontend lives on a different origin (Pages/CDN). Permissive CORS
  ;; is fine for an OMR endpoint that only accepts image uploads.
  {"Access-Control-Allow-Origin"  "*"
   "Access-Control-Allow-Methods" "POST, GET, OPTIONS"
   "Access-Control-Allow-Headers" "Content-Type"
   "Access-Control-Max-Age"       "86400"})

(defn- with-cors [response]
  (update response :headers merge cors-headers))

(defn- bad-request [msg]
  (with-cors {:status 400
              :headers {"Content-Type" "application/json"}
              :body (str "{\"error\":\"" msg "\"}")}))

(defn- server-error [msg]
  (with-cors {:status 500
              :headers {"Content-Type" "application/json"}
              :body (str "{\"error\":\"" msg "\"}")}))

(defn- extract-image
  "Pull the uploaded image out of a multipart request. Returns
   {:bytes ..., :filename ...} or nil."
  [request]
  (let [parts (or (:multipart-params request) {})
        f     (or (get parts "file") (get parts "image"))]
    (when (and f (:tempfile f) (:filename f))
      {:bytes    (with-open [in (java.io.FileInputStream. ^java.io.File (:tempfile f))]
                   (.readAllBytes in))
       :filename (:filename f)})))

(defn- ok-musicxml [^String xml]
  (with-cors {:status 200
              :headers {"Content-Type" "application/vnd.recordare.musicxml+xml"}
              :body xml}))

(defn make-omr-handler
  "Build a handler around an engine function.

   `recognize-fn` :: bytes, opts -> musicxml-string"
  [recognize-fn]
  (fn [request]
    (if-let [{:keys [bytes filename]} (extract-image request)]
      (try
        (ok-musicxml (recognize-fn bytes {:filename filename}))
        (catch clojure.lang.ExceptionInfo e
          (server-error (str "OMR failed: " (.getMessage e))))
        (catch Throwable e
          (server-error (str "Unexpected error: " (.getMessage e)))))
      (bad-request "Missing 'file' multipart field"))))

(defn- health [_]
  (with-cors {:status 200
              :headers {"Content-Type" "application/json"}
              :body "{\"ok\":true}"}))

(defn- options [_]
  (with-cors {:status 204 :headers {} :body ""}))

(defn make-app
  "Build the full Ring handler. `recognize-fn` is the engine."
  [recognize-fn]
  (let [omr (make-omr-handler recognize-fn)]
    (ring/ring-handler
     (ring/router
      [["/health" {:get health}]
       ["/omr"    {:post    omr
                   :options options
                   :middleware [parameters/parameters-middleware
                                multipart/multipart-middleware]}]]
      {:data {:middleware []}})
     (ring/create-default-handler
      {:not-found (fn [_] (with-cors {:status 404 :body "Not found"}))}))))
