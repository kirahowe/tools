(ns nice-errors.core
  "Proof-of-concept: an opt-in, Elm/Rust-flavoured error printer for Clojure.

   It is built ONLY on public hooks that already exist in Clojure 1.10+:
     - clojure.main/ex-triage + Throwable->map  (the data behind the default message)
     - clojure.main/repl-caught                  (what the clojure.main REPL calls on error)
     - nrepl.middleware.caught/*caught-fn*       (what nREPL clients call, defaults to repl-caught)
     - clojure.spec.alpha/*explain-out*          (spec's pluggable explain printer)
     - Thread/setDefaultUncaughtExceptionHandler (errors on non-REPL threads)

   Everything it prints is derived from information that is already present in the
   exception; the point is to show how far a library can get *without* changing the
   language, and where it hits the wall (see README)."
  (:require [clojure.main :as main]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.repl :as repl]
            [clojure.spec.alpha :as s])
  (:import [java.io File PushbackReader]
           [java.util.regex Pattern]))

(def ^:dynamic *context-lines* "Lines of source shown above the failing line." 2)

;;; ---------------------------------------------------------------- stack frames

(def internal-prefixes
  "Class-name prefixes treated as 'not your code' and hidden by default."
  ["clojure.lang." "clojure.core" "clojure.main" "clojure.spec" "clojure.repl" "clojure.java."
   "clojure.string" "java." "jdk." "sun." "nrepl." "cider." "nice_errors."])

(defn- internal? [^String cls] (boolean (some #(str/starts-with? cls %) internal-prefixes)))

(defn- category [^String cls]
  (cond (str/starts-with? cls "clojure.core") "clojure.core"
        (str/starts-with? cls "clojure.lang.") "clojure.lang"
        (str/starts-with? cls "clojure.main") "clojure.main"
        (str/starts-with? cls "clojure.spec") "clojure.spec"
        (re-find #"^(java|jdk|sun)\." cls) "jdk"
        (re-find #"^(nrepl|cider)\." cls) "nrepl"
        :else "other"))

(defn- clean-name
  "user$eval145 -> user/eval145 ; bugs.deep$parse_row$fn__147 -> bugs.deep/parse-row/<anon>"
  [^String cls]
  (-> (repl/demunge cls)
      (str/replace #"/fn--\d+" "/<anon>")
      (str/replace #"--\d+" "")))

(defn- frame->map [^StackTraceElement el]
  (let [cls (.getClassName el)]
    {:class cls :method (.getMethodName el) :file (.getFileName el) :line (.getLineNumber el)
     :name (clean-name cls) :internal? (internal? cls) :category (category cls)}))

(defn- root-cause ^Throwable [^Throwable t]
  (if-let [c (.getCause t)] (recur c) t))

(defn- dedupe-invoke
  "Every Clojure fn call shows up twice (invokeStatic + invoke). Keep one."
  [frames]
  (reduce (fn [acc f]
            (let [p (peek acc)]
              (if (and p (= (:class p) (:class f))
                       (= "invokeStatic" (:method p))
                       (#{"invoke" "doInvoke" "applyTo"} (:method f)))
                acc
                (conj acc f))))
          [] frames))

(defn- all-frames [^Throwable t]
  (dedupe-invoke (mapv frame->map (.getStackTrace (root-cause t)))))

;;; ---------------------------------------------------------------- source files

(defn- classpath-entries []
  (map io/file (str/split (System/getProperty "java.class.path") (Pattern/compile (Pattern/quote File/pathSeparator)))))

(defn- find-by-name
  "Runtime errors only know the *file name* (from the JVM stack frame). Look for it."
  [^String name]
  (first (for [^File d (concat (filter #(.isDirectory ^File %) (classpath-entries)) [(io/file ".")])
               ^File f (file-seq d)
               :when (and (.isFile f) (= (.getName f) name)
                          (not (re-find #"/(\.git|node_modules|target|out)/" (.getPath f))))]
           f)))

(defn- try-resolve [sym] (try (resolve sym) (catch Throwable _ nil)))

(defn- source-file
  "Best-effort location of the source file a triage map refers to."
  [{:clojure.error/keys [source path symbol]}]
  (or (when (and path (.exists (io/file path))) (io/file path))
      (when (and source (.exists (io/file source))) (io/file source))
      (when-let [f (some-> symbol try-resolve meta :file)]
        (when (and source (= (.getName (io/file f)) (.getName (io/file source))))
          (or (when (.exists (io/file f)) (io/file f)) (io/resource f))))
      (when source (or (io/resource source) (find-by-name (.getName (io/file source)))))))

(defn- source-lines [f] (when f (try (vec (str/split-lines (slurp f))) (catch Exception _ nil))))

(defn- file-ns
  "The namespace declared by the first form of a source file, if it is loaded."
  [f]
  (when f
    (try (with-open [r (PushbackReader. (io/reader f))]
           (let [form (read {:read-cond :allow :eof nil} r)]
             (when (and (seq? form) (= 'ns (first form)) (symbol? (second form)))
               (find-ns (second form)))))
         (catch Exception _ nil))))

(def ^:private classpath-namespaces
  "Namespaces available on the classpath (dirs + jars), for 'did you mean' on failed requires."
  (delay
    (->> (classpath-entries)
         (mapcat (fn [^File e]
                   (cond (.isDirectory e)
                         (for [^File f (file-seq e) :when (.isFile f)]
                           (subs (.getPath f) (inc (count (.getPath e)))))
                         (str/ends-with? (.getName e) ".jar")
                         (try (with-open [z (java.util.zip.ZipFile. e)]
                                (mapv #(.getName ^java.util.zip.ZipEntry %) (enumeration-seq (.entries z))))
                              (catch Exception _ nil)))))
         (filter #(re-find #"\.clj[cs]?$" %))
         (remove #(str/starts-with? % "META-INF"))
         (map #(-> % (str/replace #"\.clj[cs]?$" "") (str/replace "/" ".") (str/replace "_" "-")))
         distinct sort vec)))

;;; ---------------------------------------------------------------- column guessing

(defn- guess-column
  "The JVM class file stores only line numbers, so a runtime error has no column.
   Guess one by finding, on the failing line, the core function that was executing
   (taken from the innermost clojure.core stack frame), or a Java interop call."
  [line-text frames]
  (when line-text
    (let [core-names (->> frames
                          (filter #(str/starts-with? (:class %) "clojure.core$"))
                          (map #(last (str/split (:name %) #"/")))
                          (remove #{"<anon>"})
                          distinct)
          find-col (fn [re] (let [m (re-matcher re line-text)] (when (.find m) (inc (.start m)))))
          quote Pattern/quote]
      (or (some #(find-col (Pattern/compile (str "\\(" (quote %) "(?=[\\s)\\]])"))) core-names)      ; call position
          (some #(find-col (Pattern/compile (str "(?<![^\\s(\\[])" (quote %) "(?=[\\s)\\]])"))) core-names) ; bare token
          (when (some #(re-find #"clojure\.lang\.Reflector" (:class %)) frames) (find-col #"\(\.[\w-]+"))
          (when (some #(= "clojure.lang.Numbers" (:class %)) frames) (find-col #"\([-+*/]|[-+*/] "))))))

;;; ---------------------------------------------------------------- did you mean

(defn- levenshtein [^String a ^String b]
  (let [la (count a) lb (count b)]
    (loop [i 1 prev (vec (range (inc lb)))]
      (if (> i la)
        (peek prev)
        (recur (inc i)
               (loop [j 1 cur [i]]
                 (if (> j lb)
                   cur
                   (recur (inc j)
                          (conj cur (min (inc (cur (dec j)))
                                         (inc (prev j))
                                         (+ (prev (dec j))
                                            (if (= (.charAt a (dec i)) (.charAt b (dec j))) 0 1))))))))))))

(defn- suggest [word candidates]
  (let [word (str word) maxd (if (< (count word) 5) 1 2)]
    (->> candidates (map str) distinct
         (keep (fn [c] (let [d (levenshtein word c)] (when (and (pos? d) (<= d maxd)) [d c]))))
         sort (map second) (take 3) seq)))

(defn- symbol-candidates [ns]
  (concat (keys (ns-publics 'clojure.core))
          (when ns (keys (ns-map ns)))
          (keys (ns-map *ns*))))

;;; ---------------------------------------------------------------- hints

(defn- class-name [c] (cond (class? c) (.getName ^Class c) (nil? c) nil :else (str c)))

(defn- arglists-of [fq-name]
  (some-> fq-name symbol try-resolve meta :arglists))

(defn- describe
  "Returns {:headline .. :hint .. :suggestions ..} for known error shapes, else nil.
   This is the part a community could grow indefinitely (cf. Babel, error catalogues)."
  [{:clojure.error/keys [phase cause spec] err-sym :clojure.error/symbol :as triage} cls frames file-ns]
  (let [cause (or cause "")
        frame-class? (fn [re] (some #(re-find re (:class %)) frames))
        cast-target (second (re-find #"cannot be cast to class ([\w.$]+)" cause))
        cast-source (second (re-find #"class ([\w.$]+) cannot be cast" cause))
        short (fn [c] (some-> c (str/replace #"^java\.lang\." "") (str/replace #"^clojure\.lang\." "")))]
    (cond
      ;; --- arithmetic on a non-number ------------------------------------------
      (and (= cls "java.lang.ClassCastException") (= cast-target "java.lang.Number") (frame-class? #"clojure\.lang\.Numbers"))
      {:headline (str "Arithmetic on a " (short cast-source) " instead of a number.")
       :hint (str "One of the values reaching the arithmetic on this line is a " (short cast-source)
                  ". If it came from user input or a file, convert it first (`parse-long`, `parse-double`).")}

      ;; --- calling a non-function ---------------------------------------------
      (and (= cls "java.lang.ClassCastException") (= cast-target "clojure.lang.IFn"))
      {:headline (str "A " (short cast-source) " is being called as a function.")
       :hint (str "The first thing inside ( ) is called as a function; here it was a " (short cast-source)
                  ". Common causes: an extra pair of parentheses, or a value where a function name was intended"
                  (when (= cast-source "java.lang.String") " (did you mean `str`?)") ".")}

      ;; --- arithmetic on nil ----------------------------------------------------
      (and (= cls "java.lang.NullPointerException") (frame-class? #"clojure\.lang\.Numbers"))
      {:headline "Arithmetic on nil."
       :hint "One of the arguments to the arithmetic on this line was nil. nil usually comes from a map lookup with a missing or misspelled key, or from `first`/`get` on an empty collection."}

      ;; --- Java method on nil ---------------------------------------------------
      (and (= cls "java.lang.NullPointerException") (or (frame-class? #"clojure\.lang\.Reflector") (re-find #"because \"(target|x)\" is null" cause)))
      {:headline "A Java method was called on nil."
       :hint "The object before the method name is nil. Check the value flowing into this call, or guard it with `some->`."}

      ;; --- arity ----------------------------------------------------------------
      (= cls "clojure.lang.ArityException")
      (let [[_ n fname] (re-find #"Wrong number of args \((\d+)\) passed to: (\S+)" cause)
            arglists (arglists-of fname)]
        {:headline (str fname " was called with " n " argument" (when (not= n "1") "s") "."
                        (when arglists (str " It takes " (str/join " or " (map pr-str arglists)) ".")))
         :hint (when-not arglists "Check the function's definition for the number of parameters it accepts.")})

      ;; --- nth on a map ---------------------------------------------------------
      (and (= cls "java.lang.UnsupportedOperationException") (str/starts-with? cause "nth not supported"))
      {:headline (str "`nth` was given a " (short (second (re-find #"type: (\S+)" cause))) ", which has no positional order.")
       :hint "`nth` works on vectors, lists, seqs and strings. For a map use `get`, `keys`, `vals`, or `(nth (seq m) i)`."}

      ;; --- reflection failures --------------------------------------------------
      (and (= cls "java.lang.IllegalArgumentException") (re-find #"No matching (ctor|method|field)" cause))
      {:headline "Java interop could not find a constructor/method taking these argument types."
       :hint "Reflection matches on the runtime types of the arguments. Check them with `(type x)`; a common cause is passing a number or nil where a String is expected."}

      ;; --- number parsing -------------------------------------------------------
      (= cls "java.lang.NumberFormatException")
      {:headline (str "Could not parse " (or (second (re-find #"input string: (\".*\")" cause)) "the input") " as a number.")
       :hint "Validate or trim the string first, or use `parse-long`/`parse-double`, which return nil instead of throwing."}

      ;; --- missing namespace ----------------------------------------------------
      (and (= cls "java.io.FileNotFoundException") (str/starts-with? cause "Could not locate"))
      (let [path (second (re-find #"Could not locate (\S+)__init\.class" cause))
            ns-name (some-> path (str/replace "/" ".") (str/replace "_" "-"))
            sugg (when ns-name (suggest ns-name (concat (map str (all-ns)) @classpath-namespaces)))]
        {:headline (str "The namespace " ns-name " is not on the classpath.")
         :hint "Either the name is misspelled, or the library that provides it is missing from deps.edn / project.clj."
         :suggestions sugg})

      ;; --- unresolved symbol ----------------------------------------------------
      (re-find #"Unable to resolve symbol: (\S+)" cause)
      (let [sym (second (re-find #"Unable to resolve symbol: (\S+)" cause))]
        {:headline (str "`" sym "` is not defined here.")
         :hint "It may be misspelled, defined later in the file, or live in a namespace that has not been required."
         :suggestions (suggest sym (symbol-candidates file-ns))})

      ;; --- no such var ----------------------------------------------------------
      (re-find #"No such var: (\S+)" cause)
      (let [[_ alias nm] (re-find #"No such var: ([^/]+)/(\S+)" cause)
            target (when alias (or (get (ns-aliases (or file-ns *ns*)) (symbol alias)) (find-ns (symbol alias))))]
        {:headline (str "`" alias "/" nm "` does not exist" (when target (str " in " (ns-name target))) ".")
         :suggestions (when target (suggest nm (keys (ns-publics target))))})

      ;; --- unbalanced delimiters ------------------------------------------------
      (= phase :read-source)
      {:headline (cond (str/starts-with? cause "EOF while reading") "A form was never closed."
                       (str/starts-with? cause "Unmatched delimiter") "There is a closing delimiter with no opening one."
                       :else "The reader could not parse this file.")
       :hint (when (str/starts-with? cause "EOF") "Every ( [ { and \" needs a matching closer. The form that was still open is marked above.")}

      ;; --- macro spec failures --------------------------------------------------
      spec
      {:headline (str "The arguments to " err-sym " have the wrong shape.")
       :hint (when-not (find-ns 'expound.alpha) "Add `expound` to your deps for a friendlier spec explanation.")}

      :else nil)))

;;; ---------------------------------------------------------------- rendering

(defn- header [phase loc]
  (let [title (case phase
                :read-source "SYNTAX ERROR" :macro-syntax-check "MACRO SYNTAX ERROR" :macroexpansion "MACROEXPANSION ERROR"
                (:compile-syntax-check :compilation) "COMPILE ERROR" :execution "RUNTIME ERROR"
                :read-eval-result "READ ERROR" :print-eval-result "PRINT ERROR" "ERROR")
        left (str "-- " title " ") right (str " " (or loc "?"))]
    (str left (apply str (repeat (max 1 (- 80 (count left) (count right))) "-")) right)))

(defn- excerpt [lines line col label]
  (when (and lines line (<= 1 line (count lines)))
    (let [lo (max 1 (- line *context-lines*)) hi (min (count lines) (inc line))
          w (count (str hi))
          fmt (fn [n] (format (str "%" w "d | %s") n (nth lines (dec n))))
          text (nth lines (dec line))
          col (or col (inc (count (re-find #"^\s*" text))))
          pad (apply str (repeat w " "))]
      (str/join "\n" (concat (map fmt (range lo (inc line)))
                             [(str pad " | " (apply str (repeat (dec col) " ")) "^" (when label (str " " label)))]
                             (map fmt (range (inc line) (inc hi))))))))

(defn explain
  "Turn a Throwable into a data description of the error (no printing)."
  [^Throwable t]
  (let [tm (Throwable->map t)
        triage (main/ex-triage tm)
        {:clojure.error/keys [phase source line column symbol cause spec]} triage
        cls (class-name (:clojure.error/class triage))
        frames (all-frames t)
        first-user (first (remove :internal? frames))
        triage (if (and (= phase :execution) first-user
                        (not (re-find #"\.clj[cs]?$" (or source "")))
                        (re-find #"\.clj[cs]?$" (or (:file first-user) "")))
                 (assoc triage :clojure.error/source (:file first-user) :clojure.error/line (:line first-user)
                        :clojure.error/symbol (symbol (:name first-user)))
                 triage)
        {:clojure.error/keys [source line column symbol]} triage
        file (source-file triage)
        lines (source-lines file)
        ns (file-ns file)
        ;; "EOF while reading, starting at line 3": point at the unclosed form
        line (or (some->> cause (re-find #"starting at line (\d+)") second Long/parseLong) line)
        column (if (re-find #"starting at line" (or cause "")) nil column)
        col (or column (guess-column (when (and lines line (<= line (count lines))) (nth lines (dec line))) frames))
        user-frames (remove :internal? frames)
        hidden (frequencies (map :category (filter :internal? frames)))
        d (describe triage cls frames ns)]
    {:phase phase :class cls :cause cause :symbol symbol
     :location (when source (str (if (instance? File file) (.getPath ^File file) source) (when line (str ":" line)) (when column (str ":" column))))
     :column-guessed? (and (nil? column) (some? col))
     :headline (or (:headline d) (str (str/replace (or cls "Error") #"^.*\." "") (when cause (str ": " cause))))
     :detail (when (:headline d) (str (if cls (str (str/replace cls #"^.*\." "") ": ") "") cause))
     :hint (:hint d)
     :suggestions (:suggestions d)
     :spec-explanation (when spec (let [msg (main/ex-str triage)] (subs msg (inc (.indexOf msg "\n")))))
     :excerpt (excerpt lines line col nil)
     :lazy? (boolean (some #(= "clojure.lang.LazySeq" (:class %)) frames))
     :user-frames (if (= phase :execution) (vec user-frames) [])
     :hidden (if (= phase :execution) hidden {})}))

(defn render [{:keys [phase location headline detail hint suggestions spec-explanation excerpt lazy? user-frames hidden column-guessed?]}]
  (str/join "\n"
    (remove nil?
      [(header phase location) ""
       headline ""
       excerpt
       (when (and excerpt column-guessed?) "    (the JVM records only a line number; the column is a guess)")
       (when excerpt "")
       spec-explanation
       detail
       (when hint (str "\nHint: " hint))
       (when suggestions (str "\nDid you mean one of these?\n" (str/join "\n" (map #(str "    " %) suggestions))))
       (when lazy? "\nNote: this happened while a lazy sequence was being realized, which can be far from the `map`/`filter`/... that built it.")
       (when (seq user-frames)
         (str "\nIn your code (innermost first):\n"
              (str/join "\n" (map (fn [{:keys [name file line]}] (format "    %-42s %s:%s" name file line)) user-frames))))
       (when (seq hidden)
         (str "    ... " (reduce + (vals hidden)) " internal frames hidden ("
              (str/join ", " (map (fn [[k v]] (str k " " v)) (sort-by key hidden))) "). (nice-errors.core/pst) shows them."))
       ""])))

;;; ---------------------------------------------------------------- hooks

(defonce ^:private last-error (atom nil))

(defn print-error
  "Print an Elm-style description of `t` to *err*. Drop-in replacement for clojure.main/repl-caught."
  [^Throwable t]
  (reset! last-error t)
  (binding [*out* *err*]
    (println (try (render (explain t))
                  (catch Throwable inner
                    (str "nice-errors failed (" (.getMessage inner) "); falling back:\n" (main/err->msg t)))))
    (flush)))

(defn pst
  "Full, unfiltered stack trace of the last error printed."
  ([] (pst 100))
  ([depth] (when-let [t @last-error] (repl/pst t depth))))

(defn install!
  "Opt in. Call before starting a REPL (e.g. `clj -M -e \"(nice-errors.core/install!)\" -r`),
   or from an nREPL session (takes effect for that session's evaluations)."
  []
  (alter-var-root #'main/repl-caught (constantly print-error))
  ;; nREPL (CIDER, Calva, ...) has its own hook with the same contract:
  (when-let [v (try (requiring-resolve 'nrepl.middleware.caught/*caught-fn*) (catch Throwable _ nil))]
    (alter-var-root v (constantly print-error)))
  ;; spec explanations: prefer expound when it is on the classpath
  (when-let [printer (try (requiring-resolve 'expound.alpha/printer) (catch Throwable _ nil))]
    (alter-var-root #'s/*explain-out* (constantly printer)))
  (Thread/setDefaultUncaughtExceptionHandler
    (reify Thread$UncaughtExceptionHandler
      (uncaughtException [_ _ t] (print-error t))))
  :nice-errors/installed)
