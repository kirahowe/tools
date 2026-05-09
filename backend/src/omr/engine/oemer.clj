(ns omr.engine.oemer
  "Oemer engine: image bytes -> MusicXML string.

   Side effects are confined to this namespace. The function `recognize` is the
   only abstraction the rest of the app consumes; swapping engines is just
   passing a different fn with the same signature."
  (:require [babashka.process :as p]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(defn- mk-tempdir ^java.io.File []
  (.toFile (Files/createTempDirectory "oemer-" (make-array FileAttribute 0))))

(defn- delete-recursively [^java.io.File f]
  (when (.isDirectory f)
    (run! delete-recursively (.listFiles f)))
  (.delete f))

(defn- find-musicxml ^java.io.File [^java.io.File dir]
  (->> (file-seq dir)
       (filter (fn [^java.io.File f]
                 (let [n (.getName f)]
                   (or (str/ends-with? n ".musicxml")
                       (str/ends-with? n ".xml")))))
       first))

(defn recognize
  "Run oemer on `image-bytes` and return MusicXML as a String.

   Throws ex-info with :type ::recognition-failed on timeout or non-zero exit."
  (^String [^bytes image-bytes]
   (recognize image-bytes {}))
  (^String [^bytes image-bytes {:keys [filename timeout-ms]
                                :or   {filename   "input.png"
                                       timeout-ms 300000}}]
   (let [dir   (mk-tempdir)
         input (io/file dir filename)]
     (try
       (with-open [out (io/output-stream input)]
         (.write out image-bytes))
       (let [proc   (p/process {:dir dir :out :string :err :string}
                               "oemer" "--without-deskew"
                               "-o" (.getAbsolutePath dir)
                               (.getAbsolutePath input))
             result (deref proc timeout-ms ::timeout)]
         (when (= result ::timeout)
           (p/destroy-tree proc)
           (throw (ex-info "oemer timed out"
                           {:type ::recognition-failed
                            :timeout-ms timeout-ms})))
         (when-not (zero? (:exit result))
           (throw (ex-info "oemer failed"
                           {:type   ::recognition-failed
                            :exit   (:exit result)
                            :stdout (:out result)
                            :stderr (:err result)})))
         (if-let [xml (find-musicxml dir)]
           (slurp xml)
           (throw (ex-info "oemer produced no MusicXML"
                           {:type   ::recognition-failed
                            :stdout (:out result)
                            :stderr (:err result)}))))
       (finally
         (delete-recursively dir))))))
