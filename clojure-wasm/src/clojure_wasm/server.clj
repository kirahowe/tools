(ns clojure-wasm.server
  "Zero-dependency dev server for the browser-JVM REPL.

  Serves the static app from resources/public using the JDK's built-in
  com.sun.net.httpserver (no deps beyond Clojure itself), and exposes
  POST /api/eval, which evaluates Clojure on *this* JVM using the same
  bootstrap.clj + JSON contract the browser uses with CheerpJ.

  /api/eval exists for UI development and e2e tests (open the app with
  ?mode=server). It is arbitrary code execution by design — this is a
  local dev tool, not something to host publicly. It binds to 127.0.0.1
  by default; set NO_SERVER_EVAL=1 to disable the endpoint entirely."
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   (com.sun.net.httpserver HttpExchange HttpHandler HttpServer)
   (java.io ByteArrayOutputStream)
   (java.net InetSocketAddress)
   (java.nio.charset StandardCharsets)
   (java.util.concurrent Executors))
  (:gen-class))

(def ^:private content-types
  {"html" "text/html; charset=utf-8"
   "css"  "text/css; charset=utf-8"
   "js"   "text/javascript; charset=utf-8"
   "mjs"  "text/javascript; charset=utf-8"
   "json" "application/json"
   "clj"  "text/plain; charset=utf-8"
   "jar"  "application/java-archive"
   "wasm" "application/wasm"
   "svg"  "image/svg+xml"
   "ico"  "image/x-icon"
   "map"  "application/json"})

(defn content-type-for
  "Content type for a request path, by file extension."
  [path]
  (let [ext (some-> (re-find #"\.([A-Za-z0-9]+)$" path) second str/lower-case)]
    (get content-types ext "application/octet-stream")))

(defn safe-path?
  "Only allow simple absolute paths — no traversal, no backslashes."
  [uri]
  (and (string? uri)
       (str/starts-with? uri "/")
       (not (str/includes? uri ".."))
       (not (str/includes? uri "\\"))))

(defn- resource-bytes [path]
  (when-let [res (io/resource (str "public" path))]
    (with-open [in (io/input-stream res)
                out (ByteArrayOutputStream.)]
      (io/copy in out)
      (.toByteArray out))))

(defn- allow-eval? []
  (nil? (System/getenv "NO_SERVER_EVAL")))

(def ^:private server-eval
  "Loads the same bootstrap.clj the browser runs, once, and resolves its
  eval fn. Keeping this lazy means simply starting the server doesn't
  define the browser.repl namespace unless /api/eval is actually used."
  (delay
    (load-string (slurp (io/resource "public/bootstrap.clj")))
    @(requiring-resolve 'browser.repl/eval-str)))

(def ^:private server-eval-cell
  "Loads the notebook engine (a .cljc with reader conditionals, so it goes
  through browser.repl/eval-str — the same loading path the browser uses)
  and resolves the kind-aware cell evaluator."
  (delay
    (let [res (@server-eval (slurp (io/resource "public/notebook-engine.cljc")))]
      (when (str/starts-with? res "{\"tag\":\"err\"")
        (throw (ex-info (str "notebook engine failed to load: " res) {}))))
    @(requiring-resolve 'notebook.engine/eval-cell)))

;; --- notebook storage (dev-grade: JSON files on disk) -----------------------

(def ^:private notebooks-dir (io/file "data" "notebooks"))

(defn notebook-id? [s]
  (boolean (re-matches #"[a-z0-9][a-z0-9-]{0,63}" (str s))))

(defn- notebook-routes [{:keys [method uri body]}]
  (if (= uri "/api/notebooks")
    (let [ids (->> (.listFiles notebooks-dir)
                   (keep #(second (re-matches #"(.+)\.json" (.getName ^java.io.File %))))
                   sort)]
      {:status 200 :content-type "application/json"
       :body (str "[" (str/join "," (map #(str "\"" % "\"") ids)) "]")})
    (let [id (subs uri (count "/api/notebooks/"))
          file (io/file notebooks-dir (str id ".json"))]
      (cond
        (not (notebook-id? id))
        {:status 400 :content-type "text/plain" :body "bad notebook id (want [a-z0-9-])"}

        (contains? #{"PUT" "POST"} method)
        (do (.mkdirs notebooks-dir)
            (spit file body)
            {:status 200 :content-type "application/json" :body "{\"ok\":true}"})

        (and (= method "GET") (.isFile file))
        {:status 200 :content-type "application/json" :body (slurp file)}

        (= method "GET")
        {:status 404 :content-type "text/plain" :body "no such notebook"}

        :else
        {:status 405 :content-type "text/plain" :body "method not allowed"}))))

(defn handle-request
  "Data-in/data-out request handler: {:method :uri :body} ->
  {:status :content-type :body}. Separated from HttpServer wiring so it
  can be unit tested without sockets."
  [{:keys [method uri body]}]
  (cond
    (= uri "/healthz")
    {:status 200 :content-type "text/plain" :body "ok"}

    (and (= method "POST") (= uri "/api/eval"))
    (if (allow-eval?)
      {:status 200 :content-type "application/json"
       :body (@server-eval (or body ""))}
      {:status 403 :content-type "text/plain"
       :body "server-side eval is disabled (NO_SERVER_EVAL is set)"})

    (and (= method "POST") (= uri "/api/eval-cell"))
    (if (allow-eval?)
      {:status 200 :content-type "application/json"
       :body (@server-eval-cell (or body ""))}
      {:status 403 :content-type "text/plain"
       :body "server-side eval is disabled (NO_SERVER_EVAL is set)"})

    (or (= uri "/api/notebooks") (str/starts-with? uri "/api/notebooks/"))
    (notebook-routes {:method method :uri uri :body body})

    (not (contains? #{"GET" "HEAD"} method))
    {:status 405 :content-type "text/plain" :body "method not allowed"}

    (not (safe-path? uri))
    {:status 400 :content-type "text/plain" :body "bad path"}

    :else
    (let [path (if (= uri "/") "/index.html" uri)]
      (if-let [b (resource-bytes path)]
        {:status 200 :content-type (content-type-for path) :body b}
        {:status 404 :content-type "text/plain" :body "not found"}))))

(defn- write-response! [^HttpExchange ex {:keys [status content-type body]}]
  (let [^bytes b (if (bytes? body)
                   body
                   (.getBytes (str body) StandardCharsets/UTF_8))]
    (doto (.getResponseHeaders ex)
      (.set "Content-Type" content-type)
      (.set "Cache-Control" "no-store"))
    (if (= "HEAD" (.getRequestMethod ex))
      (.sendResponseHeaders ex status -1)
      (do (.sendResponseHeaders ex status (alength b))
          (with-open [os (.getResponseBody ex)]
            (.write os b))))
    (.close ex)))

(defn start!
  "Start the server; returns the HttpServer instance."
  [{:keys [host port]}]
  (let [server (HttpServer/create (InetSocketAddress. ^String host (int port)) 0)]
    (.createContext server "/"
                    (reify HttpHandler
                      (handle [_ ex]
                        (try
                          (write-response! ex (handle-request
                                               {:method (.getRequestMethod ex)
                                                :uri (.getPath (.getRequestURI ex))
                                                :body (slurp (.getRequestBody ex))}))
                          (catch Throwable t
                            (try
                              (write-response! ex {:status 500
                                                   :content-type "text/plain"
                                                   :body (str "server error: " (.getMessage t))})
                              (catch Throwable _)))))))
    (.setExecutor server (Executors/newFixedThreadPool 8))
    (.start server)
    server))

(defn -main [& _]
  (let [host (or (System/getenv "HOST") "127.0.0.1")
        port (or (some-> (System/getenv "PORT") parse-long) 8080)]
    (start! {:host host :port port})
    (println (str "clojure-wasm dev server: http://" host ":" port))
    (println (if (allow-eval?)
               "POST /api/eval enabled (dev only — evaluates code on this JVM)"
               "POST /api/eval disabled (NO_SERVER_EVAL)"))
    @(promise)))
