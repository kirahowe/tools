# Ecosystem attempts to improve Clojure error messages and stack traces

Research date: 2026-09-06. Every factual claim below is tied to a URL that was actually fetched during this session (GitHub README/raw source, Clojars API, docs pages, Slack archive, papers). Where something could not be fetched or confirmed, it is flagged inline and listed again in "What I could not verify".

Method notes:
- Stars, creation date and last-push date come from the GitHub search API (via the GitHub MCP `search_repositories` tool, full output) on 2026-09-06. `pushed_at` is used as "last commit" because per-repo commit listing was blocked in this session.
- Download counts come from `https://clojars.org/api/artifacts/<group>/<name>` fetched with curl on 2026-09-06 (cumulative, all versions).
- Source-code quotes come from `raw.githubusercontent.com` files downloaded and grepped locally.

Correction to the brief: **expound is by Ben Brinckerhoff (`bhb/expound`), not Bruce Hauman.** `github.com/bhauman/expound` returns 404. Bruce Hauman's connection is that figwheel-main *depends on* expound (see section 1).

---

## 1. expound (bhb/expound)

| Field | Value |
|---|---|
| What it does | Reformats `clojure.spec` `explain-data` into a verbose, human-oriented report: the failing value with the bad part highlighted, "should satisfy" predicate, and a "Relevant specs" table. Also `defmsg` for per-spec custom messages and `explain-results` for `stest/check` output. |
| Mechanism / hook | A **spec printer**: `expound.alpha/printer` is a drop-in for `clojure.spec.alpha/*explain-out*`. Once set, every core path that calls `s/explain-out` (assert, instrument, explain, and, since Clojure 1.10, `clojure.main/ex-str`) prints through expound. |
| Author / dates | Ben Brinckerhoff. Repo created 2017-06-25; last push 2026-08-11; 934 stars; EPL-1.0; not archived. Latest Clojars release 0.9.0. |
| Maintained? | Yes (push in Aug 2026). |
| Opt-in | `expound/expound {:mvn/version "0.9.0"}` then `(set! s/*explain-out* expound/printer)` (or `alter-var-root` for non-REPL threads). |
| Adoption | Clojars `expound/expound`: **20,123,470** downloads. |
| Limitations | README: "Expound will not give helpful errors (and in some cases, will throw an exception) if you use conformers to transform values." ClojureScript macro errors need expound configured on the Clojure side (`-e`). Only covers spec errors, not ClassCastException etc. |

Sources: https://github.com/bhb/expound ; README raw https://raw.githubusercontent.com/bhb/expound/master/README.md — "Replace calls to `clojure.spec.alpha/explain` with `expound.alpha/expound`"; global setup `(set! s/*explain-out* expound/printer)` "applies to `assert`, `instrument`, and `explain`"; printer options `:show-valid-values?`, `:value-str-fn`, `:print-specs?`, `:theme` (`:figwheel-theme` or `:none`).

Why the `*explain-out*` hook matters: Clojure's own REPL honours it. From https://raw.githubusercontent.com/clojure/clojure/master/src/clj/clojure/main.clj (`ex-str`, lines 289-296 and 329-335):

```clojure
(with-out-str
  (spec/explain-out
    (if (= spec/*explain-out* spec/explain-printer)
      (update spec :clojure.spec.alpha/problems
              (fn [probs] (map #(dissoc % :in) probs)))
      spec)))
```

So a macro-syntax-check ("Call to clojure.core/let did not conform to spec") or execution-phase spec error printed by `clojure.main/repl-caught` goes through expound if the var is set.

Who integrates expound (verified):
- **figwheel-main** (Bruce Hauman): `deps.edn` has `expound/expound {:mvn/version "0.9.0"}` and `src/figwheel/main/schema/core.clj` requires `expound.alpha` and defines `expound-string` used to validate figwheel config (https://raw.githubusercontent.com/bhauman/figwheel-main/master/deps.edn , https://raw.githubusercontent.com/bhauman/figwheel-main/master/src/figwheel/main/schema/core.clj). expound's `:figwheel-theme` exists for this.
- **guardrails** and **ghostwheel** (section 15).
- **orchestra** is named in the expound README as compatible for `:ret`/`:fn` violations (not fetched separately).
- **CIDER**: no mention of expound anywhere in the CIDER docs pages fetched (grep of https://docs.cider.mx/cider/usage/dealing_with_errors.html and https://docs.cider.mx/cider/repl/configuration.html returned nothing). See section 7 for what orchard actually does with spec data.

Introductory material: I could not find a blog post introducing expound (searched for both Hauman and Brinckerhoff). The closest primary source is The REPL podcast ep. 7, 2018-10-18: "Ben Brinckerhoff talks about Expound, his tool for formatting Clojure Spec error messages, a common language for Clojure errors, and the wider world of error messages" (https://www.therepl.net/episodes/7/). Brinckerhoff on design tradeoffs, Clojurians #clojure-spec 2017-08-31 (https://clojurians-log.clojureverse.org/clojure-spec/2017-08-31): "the default error messages are a good default, since they need to be useful 'out of the box' in a variety of contexts ... Expound makes a different set of tradeoffs that are very much optimized for the 'display to dev' case - much more verbose, and omits some information, but hopefully helps devs find the root problem faster".

---

## 2. clj-commons/pretty (formerly io.aviso/pretty)

| Field | Value |
|---|---|
| What it does | ANSI-coloured, column-aligned, *chronological* (root cause first) exception reports; demunged Clojure function names; repeated frames collapsed; noise frames omitted/hidden. README: "you can scan down to see things in chronological order; the important parts are highlighted, the names are the same (or closer) to your source code, unnecessary details are omitted, and it's much easier to pick out other essential parts easily, such as file names and line numbers." |
| Mechanism / hook | **Var redefinition** of core's printing entry points plus a default uncaught-exception handler. From `src/clj_commons/pretty/repl.clj`: `install-pretty-exceptions` "Installs an override that outputs pretty exceptions when caught by the main REPL loop. Also, overrides `clojure.repl/pst`, `clojure.stacktrace/print-stack-trace`, `clojure.stacktrace/print-cause-trace`. Extends `clojure.core/print-method` for type Throwable ... Finally, installs an [[uncaught-exception-handler]] so that uncaught exceptions in non-REPL threads will be printed reasonably." Code: `(reset-var! #'main/repl-caught pretty-repl-caught)` etc., and `(RT/loadResourceScript "clojure/test.clj")` "due to direct linking (from clojure.test to clojure.stacktrace)". Also ships nREPL middleware `clj-commons.pretty.nrepl/wrap-pretty` and `clj -X clj-commons.pretty.repl/main`. |
| Frame filtering | `default-frame-rules` in `src/clj_commons/format/exceptions.clj`: each rule is `[extractor, string-or-regex, :hide|:omit|:terminate|:show]`; defaults "omit everything in `clojure.lang`, `java.lang.reflect`; hide `clojure.core/with-bindings*` and `clojure.core/apply`, `clojure.lang.RestFn`; hide everything in `sun.reflect`; omit a number of functions in `clojure.test`; terminate at `speclj.*`, `clojure.main/.*`, or `nrepl.middleware.interruptible-eval`". Overridable via dynamic `*default-frame-rules*` / `*default-frame-filter*`. |
| Author / dates | Howard Lewis Ship. Repo created 2013-10-18; last push 2026-09-04; 623 stars; Apache-2.0. Latest Clojars `org.clj-commons/pretty` 3.8.0 (README shows 3.5.0). |
| Maintained? | Yes, actively (push Sept 2026). |
| Opt-in | `org.clj-commons/pretty {:mvn/version "3.8.0"}` then `(clj-commons.pretty.repl/install-pretty-exceptions)`; or Leiningen `:injections`; or `:exec-fn clj-commons.pretty.repl/main`. |
| Adoption | Clojars `io.aviso/pretty` **25,209,270** + `org.clj-commons/pretty` **3,709,272** downloads. |
| Limitations | Global var mutation (affects every consumer of `pst`/`print-stack-trace`); "Pretty is compatible with Clojure 1.10 and above"; Babashka support limited to a few namespaces (`clj-commons.ansi`) because of interpreter incompatibilities with JVM exceptions. Does not rewrite *messages*, only presentation. |

Sources: https://github.com/clj-commons/pretty ; https://raw.githubusercontent.com/clj-commons/pretty/main/README.md ; https://raw.githubusercontent.com/clj-commons/pretty/main/src/clj_commons/pretty/repl.clj ; https://raw.githubusercontent.com/clj-commons/pretty/main/src/clj_commons/format/exceptions.clj

---

## 3. clj-stacktrace (mmcgrana/clj-stacktrace)

| Field | Value |
|---|---|
| What it does | Readable stack traces: "Java methods are described with the usual `name.space.ClassName.methodName` convention and Clojure functions with their own `name.space/function-name` convention"; "Anonymous clojure functions are denoted by adding an `[fn]` to their enclosing, named function"; "Elements are vertically aligned". API: `pst`, `pst-on`, `pst-str`, `pst+` (colour), `parse-exception` (data for tools). |
| Mechanism / hook | Explicit printing functions; installed via the REPL `:caught` hook or `:injections`. |
| Author / dates | Mark McGranaghan. Repo created 2009-07-12; last push 2015-01-23; 272 stars; MIT. Latest Clojars 0.2.8. |
| Maintained? | No (no push since 2015). Still enormously downloaded as a transitive dependency. |
| Opt-in | `[clj-stacktrace "0.2.8"]`; `(use 'clj-stacktrace.repl)`; Leiningen: `:repl-options {:caught clj-stacktrace.repl/pst+}`. |
| Adoption | Clojars `clj-stacktrace/clj-stacktrace`: **23,434,987** downloads. |
| Limitations | Leiningen's own sample file warns that the `:caught` hook "currently overwrites *1, *2, etc". No frame filtering, no source context, no message rewriting. |

How Leiningen uses it (verified from https://raw.githubusercontent.com/technomancy/leiningen/master/sample.project.clj):

```clojure
;; Print stack traces on exceptions (highly recommended, but
;; currently overwrites *1, *2, etc).
:caught clj-stacktrace.repl/pst+
```
and a `:dev` profile example `:dependencies [[clj-stacktrace "0.2.4"]]`. Leiningen *itself* does not depend on clj-stacktrace today: `leiningen-core/project.clj` lists no such dependency, and `leiningen.core.main` prints its own failures with `clojure.stacktrace/print-cause-trace` (https://raw.githubusercontent.com/technomancy/leiningen/master/leiningen-core/src/leiningen/core/main.clj line 456). The clj-stacktrace README also describes a `~/.lein/profiles.clj` `:injections` recipe to replace built-in printing.

Sources: https://github.com/mmcgrana/clj-stacktrace

---

## 4. Babel (Clojure-Intro-Course/babel, UMN Morris)

| Field | Value |
|---|---|
| What it does | "A proof-of-concept tool for transforming Clojure's error messages into beginner-friendly forms." E.g. a ClassCastException becomes "Expected a number, but a string was given instead." Adds specs for `clojure.core` functions so that wrong-argument errors become spec errors that name the failing argument. |
| Mechanism / hook | **nREPL middleware** (`babel.middleware/interceptor`) that replaces nREPL's caught-exception printer: `(set! nrepl.middleware.caught/*caught-fn* ...)` in `setup-exc`. `modify-message` runs `Throwable->map`, branches on `:clojure.error/phase` and exception class, and for spec failures matches `#"Call to (.*) did not conform to spec."` etc.; everything else goes through `errors.prettify-exception/process-errors`, a **regex dictionary** (`errors/error_dictionary.clj`, 559 lines, "A vector of error dictionaries from the most specific one to the most general one"), e.g. `{:class "ClassCastException" :match (beginandend "class (\\S*) cannot be cast to class (\\S*)") :fn (fn [matches] (str "Expected " (get-type (nth matches 2)) ", but " (get-type (nth matches 1)) " was given instead.\n"))}`. Core specs are injected via `:injections [(require 'corefns.corefns)]`. It logs the original via `(cm/ex-str (cm/ex-triage (Throwable->map e)))` but does not reuse `ex-str` for the rewritten text. |
| Author / dates | Elena Machkasova and UMN Morris students (Charlot Shaw, Ethan Uphoff, RJ Holman, Dexter An, ...). GitHub repo (current object) created 2024-09-03 with 736 commits and a 2018 copyright, last push 2026-08-09, 3 stars, MIT on GitHub (project.clj says EPL). Clojars `babel-middleware` 0.2.0-alpha. |
| Maintained? | Sporadic academic activity (push Aug 2026) but README: "very early in development", "will experience breaking changes"; tested only with "Atom/proto-repl, and lein repl". |
| Opt-in | `[babel-middleware "0.2.0-alpha"]` + `:repl-options {:nrepl-middleware [babel.middleware/interceptor]}` (Leiningen, Clojure 1.9+). |
| Adoption | Clojars: **309** downloads. |
| Limitations | Regex-on-message approach is brittle across JDK/Clojure versions (the dictionary already carries three ClassCastException phrasings); middleware var must be set in-REPL ("I don't seem to be able to bind this var in middleware. Running (setup-exc) in repl does the trick."); Leiningen/nREPL only. |

Papers/talks (verified):
- Clojure/West 2015, "Adapting Clojure to an introductory CS classroom" (Machkasova): abstract cites "non-beginner-friendly error messages and a lack of a beginner-friendly IDE" (http://2016.clojurewest.org/speakers-2015). I could **not** find a talk titled "Clojure Error Messages for Novices"; the 2018 conj page says "the initial concept presented at Clojure/west in 2015".
- Clojure/conj 2018, "Babel: middleware for beginner-friendly error messages" (Machkasova, Shaw, Uphoff, Holman): "Programmers who are just learning their first programming languages find Clojure hard to approach. One of the reasons is that Clojure error messages require understanding Java classes and other terminology that beginners don't have background in." (https://2018.clojure-conj.org/elena-machkasova/). Video: https://www.youtube.com/watch?v=H4O8tjYARrY (page itself could not be fetched).
- MICS 2019, "A system for improving beginner-level error messages in Clojure" (Uphoff, An, Machkasova), PDF https://micsymposium.org/mics2019/wp-content/uploads/2019/05/MICS_AbstractClojure.pdf: "Our system, called babel, is integrated into Clojure runtime system as middleware for the interpreter known as nREPL. It makes use of a new Clojure feature known as Spec ... However, not all errors can be processed via Spec ... Our system consists of several components: 1. Spec contracts that are loaded at the start of the middleware, 2. Middleware that intercepts an error message if it occurs, 3. Lookup of necessary additional information ... 4. Error message replacement 5. Optional logging".
- Later work exists (UMN URS 2025 "Breaking Down Terminology of Clojure Error Messages...") but the PDF returned HTTP 403.

Downstream: Sean Corfield's 2026 `rephrase` says "Much of the initial mapping of class names and rephrasing of exception messages comes from Babel" (section 18).

Sources: https://github.com/Clojure-Intro-Course/babel ; https://raw.githubusercontent.com/Clojure-Intro-Course/babel/master/src/babel/middleware.clj ; .../src/babel/processor.clj ; .../src/errors/error_dictionary.clj ; .../src/errors/prettify_exception.clj ; .../project.clj

---

## 5. clojure-error-message-catalog (yogthos)

| Field | Value |
|---|---|
| What it does | Markdown catalog: "a catalog of common Clojure errors and their meaning", split into Clojure core, ClojureScript core and library errors. Inspired by Elm: "they have a repo for this purpose, and then provide custom compiler errors for these cases." Entries have Exception / Description / Cause N / Solution N sections so the catalog "can be processed into a format that can be also readable by programs." |
| Mechanism | None at runtime; documentation only. A companion static site was started: eggsyntax/clojure-error-website, "a very simple static website" to browse/search/submit errors. |
| Author / dates | Dmitri Sotnikov (yogthos) with contributors. Created 2016-04-22; last push 2021-11-17; 441 stars. |
| Maintained? | No (last push 2021). |
| Did it lead anywhere? | No evidence found of any tool consuming the catalog programmatically; neither Babel nor rephrase cite it. Whether the website is live was not verified. |

Sources: https://github.com/yogthos/clojure-error-message-catalog ; https://raw.githubusercontent.com/yogthos/clojure-error-message-catalog/master/README.md

---

## 6. rebel-readline (bhauman)

| Field | Value |
|---|---|
| Does it change error display? | **No.** `rebel-readline/src/rebel_readline/clojure/main.clj` (146 lines) has zero occurrences of `caught`, `ex-triage`, `ex-str` or `repl-caught`. `repl*` wraps `clojure.main/repl` and only supplies `:prompt`, `:read` (`create-repl-read`) and `:print` (`syntax-highlight-prn`); exceptions fall through to `clojure.main/repl-caught`. |
| Dates | Created 2018-01-12; last push 2026-06-20; 746 stars; Clojars `com.bhauman/rebel-readline` 0.1.11, **1,101,044** downloads. |

Sources: https://github.com/bhauman/rebel-readline ; https://raw.githubusercontent.com/bhauman/rebel-readline/master/rebel-readline/src/rebel_readline/clojure/main.clj

---

## 7. CIDER: cider-nrepl stacktrace middleware, orchard.stacktrace, `*cider-error*`

| Field | Value |
|---|---|
| What it does | Pops a `*cider-error*` buffer (`cider-stacktrace-mode`) with the cause chain, ex-data, spec problems, and per-frame flags so the user can toggle Java / Clojure / REPL / tooling / duplicate frames or show only project frames; frames are navigable (`M-.`/RET). |
| Mechanism / hook | **nREPL op** rather than `:caught`: `cider.nrepl.middleware.stacktrace` (ops `analyze-last-stacktrace`, deprecated `stacktrace`, `inspect-last-exception`) reads `*e` from the session (`(@session #'*e)`), caches it as `::analyzed-exception`, and calls `orchard.stacktrace/analyze`. Emacs renders the returned cause maps. |
| orchard.stacktrace | Namespace doc: "Instruments for analyzing exceptions and stacktraces which process exception objects and attach extra data to them." `analyze` accepts a Throwable or a `Throwable->map`-shaped map. Frame flags: `:clj`/`:cljc`/`:java` (by file extension), `:tooling` (frames matching a regex of "Clojure, JVM, nREPL or CIDER internals": `^clojure\.lang\.LazySeq|^clojure\.lang\.Var|...|^nrepl\.|^cider\.|^refactor-nrepl\.|^shadow.cljs\.|^clojure\.core/eval|...|^clojure\.main/repl`), `:dup`, `:repl`, `:project`/`:dependency` (by filesystem location). Spec: `prepare-spec-data` "Take in a map `ed` as returned by `clojure.spec.alpha/explain-data` and return a map of printed problems. The content of the returned map is modeled after `clojure.spec.alpha/explain-printer`." — i.e. a **re-implementation**, not `*explain-out*`. Compiler errors: `maybe-triage-message` calls `(clojure.main/ex-triage exception-data)` and `(with-out-str (s/explain-out explanation-data))` — this path *does* route through `*explain-out*`. |
| Emacs side (docs) | "Most of the time CIDER will display the errors in a dedicated buffer alongside the buffer you're currently evaluating code in." `cider-show-error-buffer` (`t`/`nil`/`'except-in-repl`/`'only-in-repl`); since 1.8.0 "only runtime exceptions (and not compilation errors) will cause a stacktrace buffer to be shown"; `cider-stacktrace-default-filters` accepts `java clj repl tooling dup` plus positive `project`/`all`, e.g. `(setq cider-stacktrace-default-filters '(tooling dup))`; keys j/c/r/t/d/p/a toggle categories. |
| Dates / adoption | cider-nrepl created 2013-12-21, 749 stars; orchard created 2018-01-04, 339 stars; both pushed Aug 2026. Clojars `cider/cider-nrepl` 0.62.2, **9,755,690** downloads; `cider/orchard` 0.44.0, **3,126,973**. |
| Opt-in | Automatic with CIDER's `cider-jack-in` (injects cider-nrepl). |
| Limitations | Requires cider-nrepl; analyses only the last exception; spec-assert formatting is CIDER's own (expound not consulted). |

Sources: https://docs.cider.mx/cider/usage/dealing_with_errors.html ; https://raw.githubusercontent.com/clojure-emacs/cider-nrepl/master/src/cider/nrepl/middleware/stacktrace.clj ; https://raw.githubusercontent.com/clojure-emacs/orchard/master/src/orchard/stacktrace.clj ; https://docs.cider.mx/cider-nrepl/nrepl-api/ops.html ("analyze-last-stacktrace: Return messages describing each cause and stack frame of the most recent exception.")

---

## 8. Cursive (IntelliJ)

| Field | Value |
|---|---|
| What is documented | REPL guide: the output pane shows "information about any exceptions which have occurred"; a `Tools > REPL > Print Last Exception` action exists (`:cursive.repl.actions/print-exception` in the page's menu map). EAP changelog entry 0.1.55 (dated "June 10", year not shown on the page): "Stack traces are automatically tidied using code folding." nREPL output "is pretty-printed and syntax highlighted". |
| Mechanism | Closed source; only the folding statement is documented. No documentation found for hiding "Clojure internals" or frame categories comparable to CIDER's. |
| History | Issues #98 "showing stack trace in the REPL" (2013-11-08: "the REPL only shows the exception message without the trace"), #204 "Make stack traces in REPL clickable" (2013-12-30), both closed at milestone 0.1.17; #297 "Tools > REPL > Print stack trace" (2014-03-05). |

Sources: https://cursive-ide.com/userguide/repl.html ; https://cursive-ide.com/eap.html ; https://github.com/cursive-ide/cursive/issues/98 ; https://github.com/cursive-ide/cursive/issues/204 ; https://github.com/cursive-ide/cursive/issues/297

---

## 9. Calva (VS Code)

| Field | Value |
|---|---|
| What is documented | REPL Window: errors "will automatically be printed"; "Calva: Print Last Stacktrace to the REPL Window" command; "you can also print the stack trace for any error message printed to the REPL Window via the Codelens button below it"; "When source locations are available (Clojure files), you will be able to navigate to them by pressing `ctrl+click` ... on the file name"; hover for docs, ctrl+click to Peek Definition; errors always go to the REPL Window because "the augmented stack traces need this". Output overview: "Rich stack traces" are supported only in the REPL Window (Output View / Sidebar marked "Support can be added"). |
| Mechanism | Calva connects through cider-nrepl middleware (e.g. the rephrase README's Calva alias includes `cider.nrepl/cider-middleware`); whether the stack-trace view uses the `analyze-last-stacktrace` op was not verified. |
| Limitation noted elsewhere | rephrase README: Calva "normally suppresses the second line of the standard exception report". |

Sources: https://calva.io/repl-window/ ; https://calva.io/output/

---

## 10. Babashka / SCI

| Field | Value |
|---|---|
| What it does | Structured CLI error report with `----- Error -----` (Type / Message / Data / Location file:line:col / Phase), `----- Context -----` (source excerpt with a caret) and `----- Stack trace -----` (SCI-level frames, elided in the middle). |
| Mechanism | Owns the whole pipeline in `src/babashka/impl/error_handler.clj`: `(defn ruler [title] (println (apply str "----- " title " " (repeat (- 80 7 (count title)) \-))))`; location from SCI's ex-data `(let [{:keys [:file :line :column]} d] ...)`; `error-context` reads the file and prints ~4 lines before / 6 after with `(str (str/join "" (repeat (dec column) " ")) (str "^--- " (ex-message ex)))`; the stack is `(sci/stacktrace e)` plus native frames cut at `sci.impl.`, formatted by `sci/format-stacktrace`; `split-stacktrace` keeps first 5 + last 5 frames unless `--debug`, printing `"... (run with --debug to see elided elements)"`; `--debug` adds a `----- Exception -----` section with the raw JVM trace. `phase` is `(or (:phase (ex-data ex)) (when (some :macro stacktrace) "macroexpand"))`. `--verbose` was renamed to `--debug` (`main.clj` lines 636-639, 768-771). |
| Docs | Babashka book lists only `--debug  Print debug information and internal stacktrace in case of exception.`; no prose section on the error format. |
| Dates / adoption | Repo created 2019-08-09; 4,603 stars; pushed 2026-09-05. Clojars `babashka/babashka` 1.13.219 (287,423 downloads; most users install the binary). |
| Limitations | Context/Stack-trace sections only for `:sci/error` exceptions (interpreted code); JVM-thrown internals need `--debug`. |

Sources: https://raw.githubusercontent.com/babashka/babashka/master/src/babashka/impl/error_handler.clj ; https://raw.githubusercontent.com/babashka/babashka/master/src/babashka/main.clj ; https://book.babashka.org/

---

## 11. shadow-cljs (thheller)

| Field | Value |
|---|---|
| What it does | Compiler errors and warnings are printed with a boxed header, `File: path:line:column`, a source excerpt (dim context lines, bold offending line, `^` arrow under the column) and the message; browser HUD overlay in dev. |
| Mechanism | `shadow.cljs.devtools.errors`: `ex-data-format` dispatches on `:tag`, `:clojure.error/phase`, reader-exception type or `::s/problems`; the `:shadow.build.compiler/compile-cljs` formatter prints `"File:" (str (or file url resource-name) (when (pos-int? line) (str ":" line (when (pos-int? column) (str ":" column)))))` and calls `w/print-source-excerpt-header` / `-footer`; the `:default` formatter prints class + message and up to 80 frames, filtered, via `clojure.repl/stack-element-str`. It does not use `ex-triage`/`ex-str`/`Throwable->map`. `shadow.build.warnings/print-source-excerpt-header` prints `before` lines dim, the `line` bold and `(println (sep-line "^" arrow-idx))` when `column` is positive. `print-warning` = header (" WARNING #idx - :warning-type ", " File: name:line:col") + excerpt + yellow message. |
| Config | User guide: `:warnings-as-errors true` or a set like `#{:undeclared-var}`; `:warnings {:undeclared-var false}`; `{:ignore #{some.ns some.library.*} :warnings-types #{:undeclared-var}}`; HUD section 7.8.1. |
| Dates / adoption | Created 2015-10-09; 2,406 stars; pushed 2026-09-05; Clojars `thheller/shadow-cljs` 3.5.0, **6,939,266** downloads. |

Sources: https://raw.githubusercontent.com/thheller/shadow-cljs/master/src/main/shadow/cljs/devtools/errors.clj ; https://raw.githubusercontent.com/thheller/shadow-cljs/master/src/main/shadow/build/warnings.clj ; https://shadow-cljs.github.io/docs/UsersGuide.html

---

## 12. ClojureScript compiler warnings (cljs.analyzer)

Verified from https://raw.githubusercontent.com/clojure/clojurescript/master/src/main/clojure/cljs/analyzer.cljc:

```clojure
(defn message [env s]
  (str s
    (if (:line env)
      (str " at line " (:line env) " " *cljs-file*)
      (when *cljs-file*
        (str " in file " *cljs-file*)))))
```
`default-warning-handler` prints `(message env (str "WARNING: " s))` to `*err*` (or throws when the warning is configured `:error`). Messages are `error-message` defmethods, e.g. `:undeclared-var` -> "Use of undeclared Var ns/name", `:fn-arity` -> "Wrong number of args (N) passed to f", `:invalid-arithmetic` -> "op, all arguments must be numbers, got [types] instead". `*cljs-warnings*` keys include `:undeclared-var :undeclared-ns :fn-arity :fn-deprecated :invalid-arithmetic :invalid-array-access :redef :dynamic :infer-warning` etc. Line only (no column) in the default handler; shadow-cljs adds the excerpt and column.

---

## 13. phrase (alexanderkiel)

| Field | Value |
|---|---|
| What it does | "Clojure(Script) library for phrasing spec problems. Phrasing refers to converting to human readable messages." `defphraser` defines a message per predicate pattern (with symbol capture of concrete values), `phrase-first` / `phrase` apply them to `explain-data`. Aimed at **end-user** form validation: "Phrase doesn't try to generically generate messages for all problems like Expound does. The target audience for generated messages are end-users of an application not developers." |
| Mechanism | Dispatch on the `:pred` of each spec problem ("The main discriminator in the problem map is the predicate"). Library call, not a REPL hook. |
| Dates / adoption | Created 2017-09-14; last push 2020-04-29; 287 stars; EPL-1.0; Clojars `phrase/phrase` 0.3-alpha4, **369,003** downloads. |
| Maintained? | Dormant since 2020. |

Source: https://github.com/alexanderkiel/phrase

---

## 14. malli (metosin) — `malli.error/humanize`

Evidence of the "structured errors -> human text" pattern outside spec. README (https://raw.githubusercontent.com/metosin/malli/master/README.md): `(-> Address (m/explain {...}) (me/humanize))` `;; => {:tags #{["should be a keyword"]} :address {:city ["missing required key"] :lonlat [nil ["should be a double"]]}}`; custom messages via `[:enum {:error/message "should be: S|M|L"} "S" "M" "L"]` or `:error/fn`, localisable; `me/with-spell-checking` on closed schemas gives `{:address {:streetz ["should be spelled :street"]}}`. Repo created 2019-05-17; 1,766 stars; pushed 2026-09-05; Clojars `metosin/malli` 0.20.1, **12,618,040** downloads. The `docs/error-messages.md` path returned 404; the README section was used instead.

---

## 15. Guardrails (fulcrologic) and ghostwheel (gnl)

| Field | Guardrails | ghostwheel |
|---|---|---|
| What it does | `>defn` with inline gspec (`[int? int? | #(< start end) => int?]`); checks args/return at runtime in dev; failures are logged (or thrown with `:throw? true`) as **expound** output. | Same inline syntax; "human-readable expound-powered reporting"; generative testing, side-effect detection, tracing. |
| Mechanism | Macro wrapping `defn`; `core.cljc` requires `[expound.alpha :as exp]` and `humanize-spec` calls `((exp/custom-printer expound-options) explain-data)` — i.e. it calls expound **directly**, not via `*explain-out*`. Config `guardrails.edn` `{:throw? false :expound {:show-valid-values? true} :guardrails/compact? true}`; enabled with `-Dguardrails.enabled=true`. | Macro; deprecated July 2023: "Ghostwheel's core functionality has been mostly split into independent (but compatible) projects" — points to fulcrologic/guardrails and gnl/playback. |
| Dates / adoption | Created 2019-10-28; pushed 2026-09-05; 259 stars; Clojars `com.fulcrologic/guardrails` 1.3.4, **1,046,727** downloads. | Created 2018-06-26; pushed 2024-02-17; 608 stars; Clojars `gnl/ghostwheel` 0.3.9, **145,601** downloads. |

Sources: https://github.com/fulcrologic/guardrails ; https://raw.githubusercontent.com/fulcrologic/guardrails/main/src/main/com/fulcrologic/guardrails/core.cljc ; https://github.com/gnl/ghostwheel

---

## 16. clj-kondo and clojure-lsp (static analysis)

| Field | Value |
|---|---|
| What it does | Lints without evaluating; `:type-mismatch` warns e.g. `(inc :foo)` -> "Expected: number, received: keyword."; `:invalid-arity` e.g. `(inc)`. `doc/types.md`: "Clj-kondo provides rudimentary type checking using a few basic rules and type annotations ... knows the types of a selection of clojure functions"; examples `(first (map inc))` -> "Expected: seqable collection, received: transducer." and `(defn foo [^String x] x) (foo 1)` -> "Expected: string or nil, received: positive integer." Users can add annotations in config (`:linters {:type-mismatch {:namespaces {foo {foo {:arities {1 {:args [:string] :ret :string}}}}}}}`). |
| Limitations (own docs) | types.md: "STATUS: WORK IN PROGRESS. THE CONFIGURATION FORMAT IS STILL IN FLUX, BREAKING CHANGES WILL HAPPEN."; type hints "were never designed with type checking in mind"; `:invalid-arity` false positives for macros that rewrite args (`:skip-args`). |
| clojure-lsp | "most diagnostics come from here [clj-kondo], configurations of these linters should be done on clj-kondo config side"; adds its own `unused-public-var`, `different-aliases`, `cyclic-dependencies`. |
| Dates / adoption | clj-kondo created 2019-03-20; 1,851 stars; pushed 2026-09-05; Clojars `clj-kondo/clj-kondo` 2026.08.04, **8,182,885** downloads. |

Sources: https://raw.githubusercontent.com/clj-kondo/clj-kondo/master/doc/linters.md ; https://raw.githubusercontent.com/clj-kondo/clj-kondo/master/doc/types.md ; https://clojure-lsp.io/features/

---

## 17. Portal, Reveal, FlowStorm

- **Portal** (djblue): has a dedicated viewer `:portal.viewer/ex` — `(defn ex "Viewer for datafied exceptions." ...)` in `src/portal/viewer.cljc`, backed by `src/portal/ui/viewer/exception.cljc` (`{:predicate exception? :component #'inspect-exception :name :portal.viewer/ex :doc "Viewer for datafied exceptions."}`). Typical use is `(tap> (Throwable->map *e))`. Created 2020-06-03; 1,030 stars; Clojars `djblue/portal` 0.67.2, **3,018,736** downloads. Sources: https://raw.githubusercontent.com/djblue/portal/master/src/portal/viewer.cljc ; GitHub code search hit on `src/portal/ui/viewer/exception.cljc`.
- **Reveal** (vlaaad): `src/vlaaad/reveal/stream.clj` has a `thrown` formatter for Throwable chains that drops `clojure.lang.RestFn`/`AFn` frames and collapses `invokeStatic`/`invoke` pairs, and formats print-time errors with core's pipeline: `(-> ex Throwable->map (assoc :phase :print-eval-result) m/ex-triage m/ex-str)` with `{:fill :error}`. Created 2020-02-17; 631 stars; Clojars `vlaaad/reveal` 1.3.327, **54,149** downloads. Source: https://raw.githubusercontent.com/vlaaad/reveal/master/src/vlaaad/reveal/stream.clj
- **FlowStorm**: records "all functions that didn't return because an exception unwind the stack, even when that exception was captured further"; "a combobox will show up in the toolbar, containing the functions names together with the exceptions types"; clicking positions the time-travel stepper at the throw; optional "Auto jump to exception". Created 2020-09-08; 860 stars; Clojars `com.github.flow-storm/flow-storm-dbg` 4.8.1, **122,188** downloads. Source: https://flow-storm.github.io/flow-storm-debugger/user_guide.html

---

## 18. Other projects found

| Project | What / mechanism | Status |
|---|---|---|
| **pyro** (venantius/pyro) https://github.com/venantius/pyro | "a 'booster pack' for clj-stacktrace": shows syntax-highlighted **source lines** next to frames by reading files from disk (LU cache); options `:show-source`, `:drop-nrepl-elements`, `:hide-clojure-elements`, `:hide-lein-elements`, `:ns-whitelist`. Hook: `(pyro.printer/swap-stacktrace-engine!)` rebinds the default printer. `[venantius/pyro "0.1.2"]`. | "alpha software ... development environments ONLY". Created 2015-07-07, last push 2018-07-09, 318 stars. Unmaintained. |
| **rephrase** (seancorfield/rephrase) https://github.com/seancorfield/rephrase | "an experiment in rephrasing exceptions to make them more beginner-friendly"; one-line messages, cause first then location. Regex config (`:ex-types`, `:inline-types`, `:removals`, `:ex-messages`): "The exception message is rephrased by mapping the `:inline-types` first, then applying the `:removals`, and finally applying the `:ex-messages` replacements." Hooks: `org.corfield.rephrase/repl-caught` ("a replacement for `clojure.main/repl-caught`", via `:caught`), nREPL middleware `org.corfield.rephrase.nrepl/wrap-rephrase`, and `rephrase-err->msg` ("a replacement for `clojure.main/err->msg`"). Example: `(inc "foo")` -> "Expected a number, but was given a string". Credits Babel for the initial mappings. `org.corfield/rephrase {:mvn/version "1.0.4"}`. | Created 2026-05-07, pushed 2026-08-10, 30 stars, EPL-2.0. Active. |
| **Maria "friendly"** (mhuebert/maria) https://github.com/mhuebert/maria/blob/main/friendly/src/maria/friendly/messages.cljs | Beginner ClojureScript environment; `friendly/messages.cljs` tokenises error text and matches it against a trie of templates (`"invalid arity: %"` -> "%1 is the wrong number of arguments for this function."; `"% is not a function"` -> "The value '%1' isn't a function, but it's being called like one."), plus `override-analyzer-messages!` for cljs analyzer warnings. | 492 stars; last push 2024-02-09. |
| **kaocha** (lambdaisland) | Test runner; `kaocha.stacktrace` elides frames whose class starts with `*stacktrace-filters*` = `["java.lang." "clojure.test$" "clojure.lang." "clojure.core" "clojure.main" "kaocha.monkey_patch"]` and stops at `*stacktrace-stop-list*` = `["kaocha.ns" "lambdaisland.tools.namespace.reload"]`, grouping elided frames as `"    ..."`. Origin: issue #58 (tonsky, 2019-01-26): "anything starting with `kaocha.`, `clojure.main` and `orchestra.` is not informative and actually obscures interesting parts of test output." I did **not** find evidence that kaocha prints source-context excerpts. | 860 stars; Clojars `lambdaisland/kaocha` 1.91.1392, **8,167,749** downloads. https://raw.githubusercontent.com/lambdaisland/kaocha/main/src/kaocha/stacktrace.clj ; https://github.com/lambdaisland/kaocha/issues/58 |
| **matcher-combinators** (nubank) https://github.com/nubank/matcher-combinators | Test assertions with "Pretty-printed diffs when the actual result doesn't match the expected matcher". | 545 stars; **8,680,564** downloads. |
| **hashp** | `#p` debug printer (Clojars 768,030 downloads) — not error-related; listed only because it was in the brief. Not fetched. |
| **StackParam** (cretz/stackparam) | JVM agent adding method parameters/locals to Java stack traces; cited in Eric Normand's 2018 issue. From search results only; repo page not fetched. |
| **klipse-repl** (viebel) | "Beginners friendly Clojure REPL", 44 stars — surfaced by GitHub search; not fetched. |

GitHub searches for `clojure stacktrace stars:>100` returned only pyro and clj-stacktrace; `clojure "error messages" language:clojure stars:>50` returned only expound.

Community discussions (fetched):
- Eric Normand, always-be-clojuring issue #7 "Error messages and stacktraces" (2018-03-16): "Stacktraces are a problem in Clojure. They are too long (tall) and they are printed in the reverse order they should be printed in and they lack any kind of formatting." Lists Pretty, Expound ("though imperfect"), Pyro, StackParam; mentions contact with Alex Miller about a core.specs spreadsheet. https://github.com/always-be-clojuring/issues/issues/7
- borkdude, ClojureVerse "Improving error messages in Clojure (as a library)?" (2018-03-15): proposes a "blessed" drop-in library; Brinckerhoff lists obstacles (consistent presentation across error types, feedback from intermediate users, library owners adopting `fspec`); didibus argues "Some of the bad error messages are attributable in part to the inherent design of Clojure"; jackrusher: "A great deal is possible by catching exceptions and applying templates." https://clojureverse.org/t/improving-error-messages-in-clojure-as-a-library/1765

---

## Summary table

| Project | Approach | Hook point | Maintained? (last push) | Adoption (stars / Clojars downloads) |
|---|---|---|---|---|
| expound | spec explain-data -> verbose human report | `clojure.spec.alpha/*explain-out*` (honoured by `clojure.main/ex-str`) | yes (2026-08) | 934 / 20.1M |
| clj-commons/pretty | formatted, filtered, chronological traces | redefines `clojure.main/repl-caught`, `clojure.repl/pst`, `clojure.stacktrace/*`, `print-method Throwable`, default uncaught handler; nREPL `wrap-pretty` | yes (2026-09) | 623 / 25.2M (io.aviso) + 3.7M (clj-commons) |
| clj-stacktrace | demunged, aligned traces | explicit `pst`; Leiningen `:repl-options {:caught pst+}` | no (2015-01) | 272 / 23.4M |
| Babel | regex dictionary + core specs -> beginner text | nREPL middleware; `nrepl.middleware.caught/*caught-fn*` | academic, sporadic (2026-08) | 3 / 309 |
| error-message-catalog | markdown catalog | none (docs) | no (2021-11) | 441 / n.a. |
| rebel-readline | none for errors | overrides only `:read`/`:print`/`:prompt` | yes (2026-06) | 746 / 1.1M |
| cider-nrepl + orchard | analysed cause chain, frame flags, spec problems | nREPL op reading `*e`; Emacs `cider-stacktrace-mode` | yes (2026-08) | 749+339 / 9.8M + 3.1M |
| Cursive | REPL output + "Print Last Exception"; "tidied using code folding" | closed source | yes | commercial |
| Calva | REPL Window errors, "Print Last Stacktrace", clickable frames | cider-nrepl (assumed) | yes | n.a. |
| Babashka/SCI | Error/Context/Stack trace blocks with source excerpt | own error handler over SCI ex-data | yes (2026-09) | 4,603 / binary |
| shadow-cljs | boxed compile errors/warnings with excerpt + caret | own `ex-data-format` multimethod | yes (2026-09) | 2,406 / 6.9M |
| cljs.analyzer | "WARNING: ... at line N file" | `default-warning-handler` | core | n.a. |
| phrase | spec problems -> end-user strings | predicate dispatch (`defphraser`) | dormant (2020-04) | 287 / 369K |
| malli.error | schema explain -> humanized map | library fn | yes (2026-09) | 1,766 / 12.6M |
| guardrails / ghostwheel | runtime arg checks, expound output | `>defn` macro calling `expound/custom-printer` | yes (2026-09) / deprecated (2024-02) | 259 / 1.0M ; 608 / 146K |
| clj-kondo / clojure-lsp | static `:type-mismatch`, `:invalid-arity` | linter | yes (2026-09) | 1,851 / 8.2M |
| Portal / Reveal / FlowStorm | exception viewer; formatted Throwables; exception time-travel | data viewers / own printer / instrumentation | yes | 1,030 / 3.0M ; 631 / 54K ; 860 / 122K |
| pyro | source lines in traces | `swap-stacktrace-engine!` | no (2018-07) | 318 / n.q. |
| rephrase | regex rephrasing, one-line | `:caught` replacement + nREPL middleware | yes (2026-08) | 30 / n.q. |
| kaocha | frame elision in test failures | own printer | yes (2026-09) | 860 / 8.2M |

n.q. = not queried.

---

## Patterns observed

1. **Four hook points cover almost everything.** (a) The REPL `:caught` slot / `clojure.main/repl-caught` (pretty, clj-stacktrace via Leiningen, rephrase). (b) nREPL: either middleware around the caught handler (`nrepl.middleware.caught/*caught-fn*`: Babel, rephrase, pretty's `wrap-pretty`) or a separate op that reads `*e` after the fact (cider-nrepl). (c) `clojure.spec.alpha/*explain-out*` (expound), the one hook the core team explicitly designed for and honours inside `ex-str`. (d) Owning the pipeline outright when you own the runtime/compiler (babashka/SCI, shadow-cljs, cljs.analyzer, kaocha, reveal).
2. **Message rewriting has been re-invented as regex/template tables at least four times** (Babel's `error-dictionary`, Maria's trie of templates, rephrase's `config.edn`, and the 2016 markdown catalog), each maintained by one person or one classroom. rephrase (2026) explicitly re-uses Babel's mappings, the first case of one such table feeding another.
3. **Frame-filtering lists are re-implemented independently everywhere** — pretty's `default-frame-rules`, orchard's `tooling-frame-re`, kaocha's `*stacktrace-filters*`, reveal's `RestFn`/`AFn` removals, pyro's `:hide-clojure-elements`, babashka's `sci.impl.` cut. No shared library or core-provided notion of "tooling frame" exists.
4. **Source-context excerpts appear only where the tool already has the source** (babashka reads the script file; shadow-cljs has the compilation unit; pyro reads files from disk with a cache). Neither core nor CIDER shows excerpts.
5. **Adoption follows tooling, not quality.** Libraries that ride inside popular tools (cider-nrepl 9.8M, expound 20M — pulled in by figwheel-main, guardrails, etc.; clj-stacktrace 23M — historically pulled in by Leiningen-era tooling) dwarf standalone beginner-message projects (Babel 309 downloads, rephrase 30 stars, catalog dormant).
6. **The spec -> human surface is the only "standardised" one** because core exposed a var and calls `s/explain-out` in `ex-str`. Everything else (frames, ClassCastException messages, arity errors) has no extension point beyond replacing `repl-caught` wholesale, which is why pretty and rephrase both ship a `repl-caught` replacement *and* an nREPL middleware.

## Core-team signals

- Alex Miller, Clojurians #clojure-spec, 2017-08-31: "I have been too busy to look at expound, but hope to soon" (https://clojurians-log.clojureverse.org/clojure-spec/2017-08-31). No endorsement or pointer to expound was found on clojure.org (the spec guide, 98 KB, contains no "expound"), in the Inside Clojure 1.10 errors post, or in the Clojure Deref issues that search surfaced.
- The 1.10 design deliberately exposes the pipeline for tools rather than naming any tool: Inside Clojure, "Clojure 1.10 error messages" (Alex Miller, 2018-12-17): `ex-triage` and `ex-str` "which in the Clojure repl are applied in a pipeline" so that external REPLs and tools can reuse them (https://insideclojure.org/2018/12/17/errors/). clojure.org/reference/repl_and_main lists `Throwable->map`, `ex-triage`, `ex-str` as the exposed pieces, and states that since 1.10.1 "uncaught exceptions will now be caught and printed according to the same error triage and printing functionality as the Clojure REPL" (https://clojure.org/reference/repl_and_main).
- Eric Normand's 2018 issue reports Alex Miller "reached out regarding help with spec development" / a spreadsheet of core functions for core.specs (https://github.com/always-be-clojuring/issues/issues/7).

## Observed integration surface

| Honour `*explain-out*` | Honour `ex-triage`/`ex-str` | Re-implement printing from scratch |
|---|---|---|
| clojure.main `ex-str` (core); orchard `maybe-triage-message` (compiler-phase spec errors only); figwheel-main and guardrails call expound directly rather than via the var | clojure.main; orchard (`clojure.main/ex-triage` on compiler errors); Reveal (print-phase errors); rephrase (replaces `repl-caught`/`err->msg` but keeps the phase/location shape); Babel (only for logging the original message) | pretty (own formatter, replaces `repl-caught`); clj-stacktrace; pyro; kaocha; shadow-cljs (`clojure.repl/stack-element-str`, no `ex-triage`); babashka (own handler over SCI `:phase`/`:file`/`:line`/`:column`); orchard `prepare-spec-data` for runtime spec asserts ("modeled after `clojure.spec.alpha/explain-printer`"); Babel's rewritten text; Maria |

## What I could not verify

- Any blog post by Bruce Hauman introducing expound (the premise appears wrong; expound is bhb's). No introductory blog post by Ben Brinckerhoff was found either — only the podcast and Slack quotes above.
- A 2015 Clojure/conj talk titled "Clojure Error Messages for Novices". The verifiable 2015 talk is Clojure/West "Adapting Clojure to an introductory CS classroom".
- The YouTube page for the 2018 Babel talk (redirected to a Google captcha) and the 2025 UMN paper (HTTP 403); the 2011 MICS paper (URL seen in search only).
- Exact "last commit" dates: GitHub `pushed_at` was used instead because per-repo commit listing was blocked.
- Cursive's implementation and the year of EAP 0.1.55 (page shows only "June 10"); whether Cursive filters "Clojure internals" beyond folding.
- Whether Calva's stack-trace view uses cider-nrepl's `analyze-last-stacktrace` op.
- Whether the error-catalog companion website is still online, and whether anything consumed the catalog programmatically.
- Any Sean Corfield *writing* about Clojure 1.10 error messages (none found; his 2026 `rephrase` library is the relevant artefact).
- kaocha printing source-context excerpts (only frame elision was verified).
- StackParam, klipse-repl, hashp, tools.namespace refresh errors, Timbre: not fetched / not researched.
- `docs/error-messages.md` for malli returned 404; README used instead. clj-commons/pretty's cljdoc "formatted exceptions" page reported "API analysis failed"; source was used instead.
- Clojars download counts for rephrase and pyro were not queried.
