(ns compositor.gen-script
  "Emit the standalone `./compositor` uberscript — the artifact the Homebrew
   tap installs (the clef pattern). `bb uberscript` inlines every required
   namespace into one file; we prepend a shebang and mark it executable. The
   result runs anywhere `bb` is on PATH, with no source tree."
  (:require [babashka.process :as p]
            [babashka.fs :as fs]
            [clojure.string :as str]))

(def ^:private target "./compositor")

(defn -main [& _]
  (let [{:keys [exit err]}
        (p/shell {:out :string :err :string :continue true}
                 "bb" "uberscript" target "-m" "compositor.main")]
    (when-not (zero? exit)
      (binding [*out* *err*] (println "uberscript failed:" err))
      (System/exit 1)))
  (let [body (slurp target)
        shebang "#!/usr/bin/env bb\n"]
    (when-not (str/starts-with? body shebang)
      (spit target (str shebang body))))
  (fs/set-posix-file-permissions target "rwxr-xr-x")
  (println "✓ wrote" target))
