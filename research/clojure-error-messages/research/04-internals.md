# Clojure error-message internals (source read of clojure-1.12.3)

**Source used.** `git clone --depth 1 --branch clojure-1.12.3 https://github.com/clojure/clojure.git`
-> commit `e6393a4063c42ddc0e0812f04464467764f0fd1e` (tag `clojure-1.12.3`), checked out at
`/tmp/claude-0/-home-user-tools/cf1a5c87-7190-585e-aa36-f0aa813f98cc/scratchpad/clojure-src`.
All `path:line` references below are relative to that checkout. spec.alpha is not in this repo; its
`clojure/spec/alpha.clj` was fetched raw from GitHub `master` (HEAD `6b698eed1787c14907082c86de3410dd51e816b4`) and
saved as `scratchpad/spec_alpha.clj`; citations to it are labelled `spec_alpha.clj:line`.

---

## 1. Source-location tracking

### 1a. Which reader forms get `:line`/`:column` (LispReader.java)

`RT.LINE_KEY` / `RT.COLUMN_KEY` appear in exactly three places in `src/jvm/clojure/lang/LispReader.java`:
lines 970-971 (MetaReader), 1128 (syntax-quote, *removing* them) and 1254-1255 (ListReader).

* **Lists — yes.** `ListReader.invoke` (`LispReader.java:1236-1260`) captures the position of `(`
  before reading the body and attaches it afterwards:
  ```java
  line = ((LineNumberingPushbackReader) r).getLineNumber();        // :1243
  column = ((LineNumberingPushbackReader) r).getColumnNumber()-1;  // :1244
  ...
  if(line != -1) {
      meta = RT.assoc(meta, RT.LINE_KEY, RT.get(meta, RT.LINE_KEY, line));      // :1254
      meta = RT.assoc(meta, RT.COLUMN_KEY, RT.get(meta,RT.COLUMN_KEY, column)); // :1255
      return s.withMeta((IPersistentMap)meta);
  ```
  Exception: the empty list returns the shared constant with no meta: `if(list.isEmpty()) return PersistentList.EMPTY;` (`:1247-1248`).
  Position comes from `LineNumberingPushbackReader` (`LineNumberingPushbackReader.java:41-42` `getLineNumber()` = `LineNumberReader.getLineNumber()+1`, i.e. 1-based; column counter starts at 1, `:30`, `:61-62`). Only a `LineNumberingPushbackReader` yields positions; with a plain `PushbackReader` `line` stays `-1` and nothing is attached.
* **`^meta form` — only if the target is a seq.** `MetaReader.invoke` (`LispReader.java:944-985`) captures line/column (`:949-953`) but attaches them only `if(line != -1 && o instanceof ISeq)` (`:968-972`). A `^{...} [vector]` or `^{...} sym` gets the user's meta but no position.
* **Vectors — no.** `VectorReader.invoke` (`:1347-1352`) is one line: `return LazilyPersistentVector.create(readDelimitedList(']', ...));`.
* **Maps — no.** `MapReader.invoke` (`:1355-1364`) returns `RT.map(a)`. **Sets — no.** `SetReader` (`:1366-1371`).
* **Symbols — no.** Tokens go through `readToken` -> `interpretToken` (`:306-307`) -> `matchSymbol` (`:418-465`), which returns `Symbol.intern(...)` (`:460-463`) bare. `Symbol` *could* carry meta (`Symbol.java:19` `implements IObj`), the reader just never attaches it.
* **Keywords — impossible.** `matchSymbol` returns `Keyword.intern(sym)` (`:461-462`); `Keyword.java:24` `public class Keyword implements IFn, Comparable, Named, Serializable, IHashEq` — not `IObj`, so it cannot carry metadata at all.
* **Numbers — impossible.** `read` calls `readNumber` (`:278-281`, `:296-302`) -> `matchNumber` returns boxed `java.lang.Number`s.
* **Strings — impossible.** `StringReader.invoke` (`:563-...`) builds a `java.lang.String`.
* **Syntax-quote strips positions.** `syntaxQuote` (`:1126-1131`):
  ```java
  //filter line and column numbers
  IPersistentMap newMeta = ((IObj) form).meta().without(RT.LINE_KEY).without(RT.COLUMN_KEY);
  ```
  so forms produced from `` `(...) `` templates in macros carry **no** position.

**Consequence.** Only list forms (and `^meta`-prefixed seqs) know where they were read. A bare string, number, keyword or symbol has no location of its own; the finest location the compiler can ever attribute to an error inside `(f x "y" :z)` is the position of the opening `(`.

### 1b. How the compiler obtains and emits line/column (Compiler.java)

* Dynamic vars: `SOURCE` = `clojure.core/*source-path*`, default `"NO_SOURCE_FILE"` (`Compiler.java:242-243`); `SOURCE_PATH` = `clojure.core/*file*`, default `"NO_SOURCE_PATH"` (`:246-247`); `LINE` and `COLUMN` are unnamed dynamic vars `Var.create(0).setDynamic()` (`:307-308`) read by `lineDeref()`/`columnDeref()` (`:310-316`); `LINE_BEFORE/COLUMN_BEFORE/LINE_AFTER/COLUMN_AFTER` (`:319-322`) track the reader position around each top-level form.
* Binding: `analyzeSeq` (`:7661-7669`) copies the form's meta into the thread bindings:
  ```java
  Object line = lineDeref();
  Object column = columnDeref();
  if(RT.meta(form) != null && RT.meta(form).containsKey(RT.LINE_KEY))
      line = RT.meta(form).valAt(RT.LINE_KEY);
  ...
  Var.pushThreadBindings(RT.map(LINE, line, COLUMN, column));
  ```
  Same pattern in `eval` (`:7724-7727`, also honouring `:clojure.core/eval-file` meta to rebind `SOURCE_PATH`/`SOURCE`, `:7729-7737`) and `compile1` (`:8291-8300`). Every `Expr` parser snapshots `lineDeref()`/`columnDeref()` at parse time — e.g. `HostExpr.Parser` `:972-973`, `NewExpr.Parser` `:3169-3170`, `RecurExpr.Parser` `:7239-7240`, `FnExpr.parse` `:4596-4597`, `FnMethod.parse` `:5917-5918`, `DefExpr` `:588`, `InvokeExpr` `:4438`.
* File bindings: `Compiler.load(Reader, sourcePath, sourceName)` (`:8187-8207`) pushes `SOURCE_PATH, sourcePath, SOURCE, sourceName`; `load(Reader)` (`:8169-8171`) passes `null, "NO_SOURCE_FILE"` (`:8170`) (this is the REPL path); `loadFile` (`:8160-8162`) passes the absolute path and `File.getName()`; `RT.loadResourceScript` (`RT.java:396-401`) passes the classpath-relative name and its last path segment: `Compiler.load(new InputStreamReader(ins, UTF8), name, file);`. `compile` (`:8327-8360`) binds the same set.
* **Emission: line only.** Every emission site passes just `line`: `gen.visitLineNumber(line, gen.mark());` (e.g. `:1475`, `:2260`, `:2282`, `:3260`, `:4314` in `InvokeExpr.emit`), and in method prologues `Label loopLabel = gen.mark(); gen.visitLineNumber(line, loopLabel);` (`:6083-6084`, `:6126-6127`). The bundled ASM API has no column parameter: `public void visitLineNumber(final int line, final Label start)` (`src/jvm/clojure/asm/MethodVisitor.java:759`, doc `:748-758`: "a line number. This number refers to the source file"). `MethodWriter.visitLineNumber` (`asm/MethodWriter.java:1528-1535`) writes `(bytecodeOffset, line)` shorts into the `"LineNumberTable"` attribute (`asm/Constants.java:54`).
* **Source file name.** `ObjExpr.compile` (`Compiler.java:4887-4910`):
  ```java
  String source = (String) SOURCE.deref();
  ...
  String smap = "SMAP\n" + ... + "*F\n" + "+ 1 " + source + "\n" + (String) SOURCE_PATH.deref() + "\n" + "*L\n" + ...
  cv.visitSource(source, smap);   // :4910
  ```
  `ClassVisitor.visitSource(final String source, final String debug)` (`asm/ClassVisitor.java:117`; doc: "the name of the source file"). So the class file's `SourceFile` attribute is the *bare file name* (`foo.clj`) or `NO_SOURCE_FILE`; the full path only lives in the JSR-45 `SourceDebugExtension` SMAP, which `StackTraceElement` does not expose.

**Consequence.** A JVM `StackTraceElement` is `(class, method, fileName, lineNumber)`. Clojure can populate at most `foo.clj:12`; the column that the reader collected and the compiler carries in `COLUMN` is discarded at emission, and there is no way to say *which* sub-expression on line 12 threw. Everything on one line shares one `LineNumberTable` entry per emitted instruction.

### 1c. Macroexpansion and location

* `macroexpand1` (`Compiler.java:7566-7650`) calls the macro with the *original* form (with its reader meta) and env: `ISeq args = RT.cons(form, RT.cons(Compiler.LOCAL_ENV.get(), form.next())); return v.applyTo(args);` (`:7581-7582`). Nothing copies meta from `form` onto the result. `preserveTag` (`:7528-7535`) copies **only** `:tag` and is used only for inline expansions (`:7682`) and the `(.method x)` sugar (`:7630`).
* `analyzeSeq` analyses the expansion *inside* the `LINE/COLUMN` bindings pushed for the original form: `Object me = macroexpand1(form); if(me != form) return analyze(context, me, name);` (`:7673-7675`, bindings pushed at `:7668-7669`, popped in `finally` `:7702`). Because the nested `analyzeSeq` for the expansion consults `RT.meta(form)` first and falls back to `lineDeref()` (`:7664-7667`), an expansion **without** `:line` meta inherits the **call-site** line — and since syntax-quote strips positions (`LispReader.java:1126-1131`), every list produced by a `` ` `` template inherits the same call-site line, with no finer granularity. Argument forms spliced in by the macro keep their own reader meta (they were read at the call site), so errors inside them still point at their own line.
* When the location becomes the macro's *definition* site: any list in the expansion that still carries `:line` meta from wherever it was read overrides the call-site binding. A plain quoted literal `'(...)` inside a macro body keeps the reader's `:line/:column` from the macro's file; constants with meta are emitted with that meta intact — `ObjExpr.emitValue`: `if(value instanceof IObj && RT.count(((IObj) value).meta()) > 0) { ... emitValue(elideMeta(m), gen); ... withMeta ... }` (`Compiler.java:5484-5494`), where `elideMeta` only drops keys listed in the `:elide-meta` compiler option (`:292-303`). Meanwhile `SOURCE_PATH` is still the call-site file, so the reported `file:line` can be a call-site file with a definition-site line.
* Deliberate call-site propagation exists in a few core macros: bootstrap `fn` does `(.withMeta ^clojure.lang.IObj (cons 'fn* decl) (.meta ^clojure.lang.IMeta &form))` (`src/clj/clojure/core.clj:45-47`) and the full `fn` macro ends `(with-meta (if name (list* 'fn* name new-sigs) (cons 'fn* new-sigs)) (meta &form))` (`core.clj:4618-4622`). These are the only two `(meta &form)` users in core.clj. Clojure's own test suite documents the limitation: `; would have used `are` here, but :line meta on &form doesn't survive successive macroexpansions` (`test/clojure/test_clojure/errors.clj:63`).
* Lazy-seq expansions: `analyze` converts a `LazySeq` result and copies only the LazySeq's own meta: `form = ((IObj)form).withMeta(RT.meta(mform));` (`Compiler.java:7341-7347`).
* Arity errors in macros hide the two implicit params: `catch(ArityException e) { if(e.name.equals(munge(v.ns.name.name) + "$" + munge(v.sym.name))) throw new ArityException(e.actual - 2, e.name); ...` (`:7584-7591`).

---

## 2. How errors are wrapped

### 2a. `CompilerException` (`Compiler.java:7403-7483`)

```java
static public class CompilerException extends RuntimeException implements IExceptionInfo{
    final public String source;      // :7404
    final public int line;           // :7406
    final public Object data;        // :7408
    final static public String ERR_NS = "clojure.error";                       // :7411
    final static public Keyword ERR_SOURCE = Keyword.intern(ERR_NS, "source"); // :7412
    ... ERR_LINE :7413, ERR_COLUMN :7414, ERR_PHASE :7415, ERR_SYMBOL :7416
    final static public Keyword PHASE_READ = Keyword.intern(null, "read-source");                 // :7419
    PHASE_MACRO_SYNTAX_CHECK "macro-syntax-check" :7420, PHASE_MACROEXPANSION "macroexpansion" :7421,
    PHASE_COMPILE_SYNTAX_CHECK "compile-syntax-check" :7422, PHASE_COMPILATION "compilation" :7423,
    PHASE_EXECUTION "execution" :7424
    final static public Keyword SPEC_PROBLEMS = Keyword.intern("clojure.spec.alpha", "problems"); // :7426
```
Note there is **no** `column`, `symbol` or `phase` field: those live only in the `data` map built by the 6-arg constructor (`:7437-7445`):
```java
Associative m = RT.map(ERR_PHASE, phase, ERR_LINE, line, ERR_COLUMN, column);
if(source != null) m = RT.assoc(m, ERR_SOURCE, source);
if(sym != null) m = RT.assoc(m, ERR_SYMBOL, sym);
```
Constructors: 4-arg `(source,line,column,cause)` -> `sym=null` (`:7429-7431`); 5-arg with `Symbol` -> `PHASE_COMPILE_SYNTAX_CHECK` (`:7433-7435`).

Message construction (`:7451-7466`):
```java
private static String verb(Keyword phase) {
    if(PHASE_READ.equals(phase)) return "reading source";
    else if(PHASE_COMPILE_SYNTAX_CHECK.equals(phase) || PHASE_COMPILATION.equals(phase)) return "compiling";
    else return "macroexpanding"; }
public static String makeMsg(String source, int line, int column, Symbol sym, Keyword phase, Throwable cause){
    return (PHASE_MACROEXPANSION.equals(phase) ? "Unexpected error " : "Syntax error ") +
        verb(phase) + " " + (sym != null ? sym + " " : "") +
        "at (" + (source != null && !source.equals("NO_SOURCE_PATH") ? (source + ":") : "") +
        line + ":" + column + ").";
```
=> `Syntax error compiling foo at (src/x.clj:3:1).` `toString` (`:7468-7482`) appends the cause's message on a new line **unless** the cause is a macro-syntax-check with `:clojure.spec.alpha/problems`, in which case only the headline is returned (`:7473-7474`). The deprecated pre-1.10 format `"%s, compiling:(%s:%d:%d)"` is still present as `errorMsg` (`:7707-7709`).

### 2b. Where wrapping happens, and the no-double-wrap rule

| Site | Lines | Phase / symbol |
|---|---|---|
| `analyze` catch-all | `Compiler.java:7394-7400` | 4-arg ctor -> `compile-syntax-check`, no symbol |
| `analyzeSeq` catch-all | `:7692-7699` | 5-arg ctor with `s` = head symbol -> `compile-syntax-check` |
| `checkSpecs` | `:7556-7563` | `PHASE_MACRO_SYNTAX_CHECK`, `v.toSymbol()` |
| `macroexpand1` | `:7593-7611` | `CompilerException` rethrown as-is; `IllegalArgumentException \| IllegalStateException \| ExceptionInfo` -> `macro-syntax-check`; any other `Throwable` -> `macro-syntax-check` iff its class is exactly `Exception.class`, else `macroexpansion` |
| `DefExpr.eval` | `:474-480` | `PHASE_EXECUTION`, symbol `def` |
| `InstanceMethodExpr.eval` | `:2026-2031` | `PHASE_EXECUTION` |
| `StaticMethodExpr.eval` | `:2242-2247` | `PHASE_EXECUTION` |
| `KeywordInvokeExpr.eval` | `:3826-3831` | `PHASE_EXECUTION` |
| `InvokeExpr.eval` | `:4224-4230` | `PHASE_EXECUTION` |
| `load` | `:8228-8237` | `ReaderException` -> `PHASE_READ` with `e.line/e.column`; other -> `PHASE_EXECUTION` at `LINE_BEFORE/COLUMN_BEFORE` |
| `compile` (AOT) | `:8484-8486` | `ReaderException` -> **4-arg ctor**, i.e. reported as `compile-syntax-check`, not `read-source` |

The guard is identical everywhere: `if(!(e instanceof CompilerException)) throw new CompilerException(...); else throw (CompilerException) e;` (`:476-479`, `:2028-2031`, `:2244-2247`, `:3828-3831`, `:4226-4229`, `:7396-7399`, `:7694-7698`, `:8234-8237`), plus `catch(CompilerException e) { throw e; }` in `macroexpand1` (`:7593-7595`). The reader's own wrapper: `LispReader.read` (`LispReader.java:310-317`) converts any exception into `new ReaderException(rdr.getLineNumber(), rdr.getColumnNumber(), e)` only when `!isRecursive && r instanceof LineNumberingPushbackReader`; `ReaderException` (`:148-167`) is an `IExceptionInfo` whose data is `{:clojure.error/line l :clojure.error/column c}`.

### 2c. Macro spec checking

* Switch: `RT.java:325` `public static boolean instrumentMacros = ! Boolean.getBoolean("clojure.spec.skip-macros");`, `:326` `static volatile boolean CHECK_SPECS = false;`, set at the end of `RT`'s static init `CHECK_SPECS = RT.instrumentMacros;` (`:364`). It is a JVM system property, not a var.
* Lookup: `Compiler.ensureMacroCheck` (`Compiler.java:7541-7554`) does `RT.load("clojure/spec/alpha"); RT.load("clojure/core/specs/alpha"); MACRO_CHECK = Var.find(Symbol.intern("clojure.spec.alpha", "macroexpand-check"));` — the exact var is **`clojure.spec.alpha/macroexpand-check`** (there is no `clojure.core/*macro-check*`).
* Call: `checkSpecs` (`:7556-7563`) `ensureMacroCheck().applyTo(RT.cons(v, RT.list(form.next())));` invoked from `macroexpand1` at `:7577` before the macro runs; any exception -> `CompilerException(..., v.toSymbol(), PHASE_MACRO_SYNTAX_CHECK, e)`.
* Message: `spec_alpha.clj:702-712`
  ```clojure
  (defn- macroexpand-check [v args]
    (let [fn-spec (get-spec v)]
      (when-let [arg-spec (:args fn-spec)]
        (when (invalid? (conform arg-spec args))
          (let [ed (assoc (explain-data* arg-spec [] ...) ::args args)]
            (throw (ex-info (str "Call to " (->sym v) " did not conform to spec.") ed)))))))
  ```
  Note it is `defn-` (private) — the compiler reaches it via `Var.find`, not a public API. The `explain-data` payload (`::problems`) is what `ex-triage` later surfaces as `:clojure.error/spec`; the printed problem lines come from `explain-printer` (`spec_alpha.clj:234-257`: `(pr val) (print " - failed: ") ... " in: " ... " at: " ... " spec: "`), selected through the dynamic var `(def ^:dynamic *explain-out* explain-printer)` (`:259`) via `explain-out` (`:261-265`).

---

## 3. The REPL error printer (`src/clj/clojure/main.clj`)

### 3a. `ex-triage` (`main.clj:207-266`)

Input is a `Throwable->map`. Bindings (`:229-233`): `{:keys [via trace phase] :or {phase :execution}}`; `{:keys [type message data]} (last via)` (the **root** cause); spec keys `:clojure.spec.alpha/problems`, `/fn`, `:clojure.spec.test.alpha/caller` from root `data`; `top-data` = `(:data (first via))` (the outermost exception's ex-data — for a `CompilerException` that is the `:clojure.error/*` map from 2a). Then `(case phase ...)` and finally `(assoc ... :clojure.error/phase phase)` (`:266`):

| phase | source of location | other keys |
|---|---|---|
| `:read-source` (`:236-242`) | `(merge (-> via second :data) top-data)` — line/column from the `ReaderException` (2nd in chain), `source` -> `file-name`/`file-path`; drops source if `NO_SOURCE_FILE`/`NO_SOURCE_PATH` | `:cause message` |
| `:compile-syntax-check :compilation :macro-syntax-check :macroexpansion` (`:244-250`) | `top-data` (CompilerException data), `source` -> `file-name`/`file-path`, `NO_SOURCE_*` dropped | `:class type`, `:cause message`, `:spec data` when `problems` |
| `:read-eval-result :print-eval-result` (`:252-258`) | **first** stack frame `[source method file line]` of `trace` | `:symbol (java-loc->source source method)`, `:class`, `:cause` |
| `:execution` (`:257-265`) | first frame after `(drop-while #(core-class? (name (first %))) trace)`; file = spec caller file or frame file unless `NO_SOURCE_*`; line = spec caller line or frame line | `:class type` always; `:symbol` = spec `fn` or `java-loc->source`; `:spec data` when `problems` |

Helpers: `core-class?` (`:56-60`) = class name starts with `clojure.lang.` or its `ns$` prefix is in `core-namespaces` (`:50-54`, a hard-coded set of `clojure.core`, `clojure.string`, `clojure.spec.alpha`, ...). `java-loc->source` (`:197-205`): for `invoke`/`invokeStatic`, split the class on `$`, `demunge` each part, strip the `--\d+` suffix (`degen #(.replaceAll ^String % "--.*$" "")`), and rebuild `ns/fn$nested`; other methods become `Class/method`. `file-name`/`file-path` (`:176-195`) reduce the absolute path to a name / cwd-relative path.

### 3b. `ex-str` templates (`main.clj:268-340`)

Preamble (`:275-280`): `loc (str (or path source "REPL") ":" (or line 1) (if column (str ":" column) ""))`; `simple-class` = last dotted segment; `cause-type` is `""` when the class is `Exception` or `RuntimeException` (`;; omit, not useful`), else `" (SimpleClass)"`. Templates, verbatim:

* `:read-source` — `"Syntax error reading source at (%s).%n%s%n"` (loc, cause) `:283`
* `:macro-syntax-check` — `"Syntax error macroexpanding %sat (%s).%n%s"` (symbol+space or "", loc, then either `(with-out-str (spec/explain-out ...))` or `(format "%s%n" cause)`) `:286-297`. When `spec/*explain-out*` is still the default `explain-printer`, `:in` is dissoc'ed from each problem first (`:293-296`).
* `:macroexpansion` — `"Unexpected error%s macroexpanding %sat (%s).%n%s%n"` (cause-type, symbol, loc, cause) `:300-304`
* `:compile-syntax-check` — `"Syntax error%s compiling %sat (%s).%n%s%n"` `:307-311`
* `:compilation` — `"Unexpected error%s compiling %sat (%s).%n%s%n"` `:314-318`
* `:read-eval-result` — `"Error reading eval result%s at %s (%s).%n%s%n"` (cause-type, symbol, loc, cause) `:321`
* `:print-eval-result` — `"Error printing return value%s at %s (%s).%n%s%n"` `:324`
* `:execution` with spec — `"Execution error - invalid arguments to %s at (%s).%n%s"` (symbol, loc, explain-out) `:328-335`
* `:execution` otherwise — `"Execution error%s at %s(%s).%n%s%n"` (cause-type, symbol+space or "", loc, cause) `:336-340`

The test suite pins one: `"Execution error (Error) at (REPL:1).\nxyz\n"` (`test/clojure/test_clojure/main.clj:59`).

### 3c. `repl-caught`, `report-error`, `clojure.main.report`

* `err->msg` (`:342-345`): `(-> e Throwable->map ex-triage ex-str)`.
* `repl-caught` (`:347-351`): `(binding [*out* *err*] (print (err->msg e)) (flush))` — **no stack trace, ever**.
* `repl` (`:368-475`): option doc `:caught, function of one argument, a throwable, called when read, eval, or print throws an exception or error  default: repl-caught` (`:407-410`); default map `caught repl-caught` (`:424`). Read failures are re-tagged `(throw (ex-info nil {:clojure.error/phase :read-source} e))` (`:435`), print failures `{:clojure.error/phase :print-eval-result}` (`:444`); every catch does `(caught e) (set! *e e)` (`:446-447`, `:453-454`, `:461-462`).
* `report-error` (`:585-615`): options `:target - "file" (default), "stderr", "none"`; builds `(array-map :clojure.main/message message :clojure.main/triage triage :clojure.main/trace trace)` (`:598-601`), pprints it with `*print-namespace-maps* false` (`:603`), writes to `(Files/createTempFile "clojure-" ".edn" ...)` (`:607`) and prints
  ```clojure
  (println (str message (System/lineSeparator) "Full report at:" (System/lineSeparator) err-path))  ; :614
  ```
  or, if the file could not be written / target is stderr, `(println (str report-str (System/lineSeparator) message))` (`:615`). All to `*err*` (`:612`).
* `main` (`:617-676`): `--report target` flag doc (`:626-627`); for any main option the handler is wrapped `(catch Throwable t (report-error t :target (get flags "report" (System/getProperty "clojure.main.report" "file"))) (System/exit 1))` (`:666-668`); the bare-REPL path uses `(report-error t :target "file")` (`:672`). So the property `-Dclojure.main.report=stderr|file|none` is read once, at `:667`, default `"file"`, and the `--report` flag overrides it.

Net: interactive REPL errors print only the `ex-str` headline+cause; an uncaught error in script/`-m` mode prints the same headline followed by `Full report at:` and a temp `.edn` path whose contents include the full `:trace` vector.

### 3d. Rebindable hooks — what kind each is

| Hook | Kind | Evidence |
|---|---|---|
| `clojure.main/repl` `:caught` | per-call option (fn) | `main.clj:407-410, 424` |
| `clojure.main/repl-caught` | plain `defn` (not `^:dynamic`) — needs `alter-var-root` or pass `:caught` | `:347` |
| `clojure.main/report-error` | plain `defn`; called through the var from `main` | `:585`, `:667`, `:672` |
| `clojure.main/ex-triage`, `ex-str`, `err->msg` | public plain fns (`:added "1.10"`); composed through var derefs, so `alter-var-root` on any of them changes REPL output | `:207`, `:268`, `:342` |
| `clojure.spec.alpha/*explain-out*` | **dynamic var**; `with-bindings` in `clojure.main` binds it so `set!` works at the REPL; `ex-str` compares it to `explain-printer` | `spec_alpha.clj:259`; `main.clj:95, 293, 333` |
| `clojure.core/*err*` | **dynamic var** (`RT.java:192-193` `.setDynamic()`); `repl-caught`/`report-error`/`pst` all `binding [*out* *err*]` | `main.clj:349, 612`; `repl.clj:251` |
| `*e` | set by the REPL after every caught error | `main.clj:447, 454, 462` |
| `print-method Throwable` | multimethod — `defmethod` to override `#error {...}` | `core_print.clj:545-546` |
| `clojure.stacktrace/print-cause-trace` | plain fn, opt-in | `stacktrace.clj:72-80` |
| `clojure.repl/pst` | plain fn, opt-in; depth arg (default `12`); **no `*default-pst-depth*` var exists** (grep of `src/` finds none) | `repl.clj:240-270` |
| `Thread/setDefaultUncaughtExceptionHandler` | JVM API; irrelevant to the main thread because `clojure.main` catches `Throwable` itself (`main.clj:666-668`) — only other threads reach it | |
| `-Dclojure.main.report`, `--report` | system property / CLI flag | `main.clj:656-657, 667` |
| `-Dclojure.spec.skip-macros=true` | system property; disables macro spec checks | `RT.java:325, 364` |

---

## 4. `Throwable->map` (`src/clj/clojure/core_print.clj:473-507`)

```clojure
(defn Throwable->map ... [^Throwable o]
  (let [base (fn [^Throwable t]
               (merge {:type (symbol (.getName (class t)))}
                 (when-let [msg (.getLocalizedMessage t)] {:message msg})
                 (when-let [ed (ex-data t)] {:data ed})
                 (let [st (.getStackTrace t)] (when (pos? (alength st)) {:at (StackTraceElement->vec (aget st 0))}))))
        via (loop [via [], ^Throwable t o] (if t (recur (conj via t) (.getCause t)) via))
        ^Throwable root (peek via)]
    (merge {:via (vec (map base via))
            :trace (vec (map StackTraceElement->vec (.getStackTrace ^Throwable (or root o))))}
      (when-let [root-msg (.getLocalizedMessage root)] {:cause root-msg})
      (when-let [data (ex-data root)] {:data data})
      (when-let [phase (-> o ex-data :clojure.error/phase)] {:phase phase}))))
```
So `:cause`/`:data` come from the **root** cause, `:phase` from the **outermost** exception's `ex-data` (`:506`), `:via` is the whole chain outer->root with `:type :message :data :at`, and `:trace` is only the root's frames. `StackTraceElement->vec` (`:467-471`) = `[(symbol (.getClassName o)) (symbol (.getMethodName o)) (.getFileName o) (.getLineNumber o)]` — raw JVM `class method file line`, no demunging, no column.

---

## 5. Name munging and what a frame means

* **Class naming** (`FnExpr.parse`, `Compiler.java:4550-4568`):
  ```java
  String basename = (enclosingMethod != null ? enclosingMethod.objx.name : (munge(currentNS().name.name))) + "$";
  if(RT.second(form) instanceof Symbol) { nm = (Symbol) RT.second(form); name = nm.name + "__" + RT.nextID(); }
  else { if(name == null) name = "fn__" + RT.nextID();
         else if (enclosingMethod != null) name += "__" + RT.nextID(); }
  String simpleName = munge(name).replace(".", "_DOT_");
  fn.name = basename + simpleName;
  ```
  For `(defn foo ...)` the `name` argument is the var's symbol name (`DefExpr.parse` `:589` `analyze(..., RT.third(form), v.sym.name)`), and with no enclosing method it is used verbatim -> `my.ns$foo`. Top-level non-`def` forms evaluated by `Compiler.eval` are wrapped in a fn named `"eval" + RT.nextID()` (`:7754-7758`), giving `user$eval138`; an anonymous fn inside it has `enclosingMethod != null`, so basename `user$eval138$` + `fn__139`. `RT.nextID` (`RT.java:523-525`) is `id.getAndIncrement()` on a global counter — ids are session-unique and unstable between runs. The comment at `Compiler.java:4875-4877` says it: `//with name current_ns.defname[$letname]+ //anonymous fns get names fn__id`.
* **Method naming**: `FnMethod.getMethodName()` = `isVariadic()?"doInvoke":"invoke"` (`:6299-6301`); direct-linkable fns additionally get `new Method("invokeStatic", returnType, argtypes)` (`:6074`) with `invoke` delegating to it (`:6126-6128`); primitive-hinted arities get `invokePrim` (`:6147`, `:6180`).
* **Munging table** `CHAR_MAP` (`:3365-3390`): `'-' -> "_"`, `':' -> "_COLON_"`, `'+' -> "_PLUS_"`, `'>' -> "_GT_"`, `'<' -> "_LT_"`, `'=' -> "_EQ_"`, `'!' -> "_BANG_"`, `'?' -> "_QMARK_"`, `'*' -> "_STAR_"`, `'/' -> "_SLASH_"`, ... (`'.'` is commented out, `:3367`). `munge` (`:3435-3446`) substitutes char by char; `demunge` (`:3448-3466`) reverses via `DEMUNGE_MAP`, which additionally maps `"$" -> '/'` (`:3396-3398`) and is matched longest-first (`:3408-3432`). Because `-` and `_` collapse, demunging is lossy (`fn__139` -> `fn--139`). `clojure.repl/demunge` (`repl.clj:207-212`) is a thin wrapper over `Compiler/demunge`; `stack-element-str` (`:227-238`) demunges only when the frame's file ends in `.clj`/`.cljc` or is `NO_SOURCE_FILE`.
* **Why `user$eval138$fn__139.invoke(NO_SOURCE_FILE:1)`**: ns `user` + `$` + eval wrapper `eval138` (`:7755-7758`) + `$` + anonymous `fn__139` (`:4561`); `invoke` because it is a non-variadic instance arity (`:6300`); `NO_SOURCE_FILE` because the REPL loads via `load(Reader)` -> `SOURCE = "NO_SOURCE_FILE"` (`:8170`) which becomes the class's `SourceFile` (`:4887, 4910`); `:1` because `repl-read` re-reads each form with the line counter reset to 1 (`main.clj:139-150` `renumbering-read`, called as `(renumbering-read {:read-cond :allow} *in* 1)` at `:167`) and only that line reaches the `LineNumberTable`.
* **Why `my.ns$foo.invokeStatic(foo.clj:12)`**: `munge("my.ns")` leaves dots (`:3367` commented out) -> `my.ns$foo` (`:4550-4567`); `invokeStatic` because the fn is direct-linkable (`:6074`); `foo.clj` is the last path segment passed as `sourceName` (`RT.java:396-401` / `Compiler.java:8162`); `12` from `gen.visitLineNumber(line, ...)` at the call (`:4314`).
* `ArityException` reuses these class names: `AFn.throwArity` (`AFn.java:427-430`) `String name = getClass().getName(); throw new ArityException(n, name);` and the message demunges it (see 6).

---

## 6. Messages that come from Java/the JVM, not from Clojure

* **`ClassCastException` in arithmetic.** `clojure.core/+` inlines to `Numbers.add` (`core.clj:986-989` `{:inline (nary-inline 'add 'unchecked_add)`; `nary-inline` emits `` `(. clojure.lang.Numbers (~op ~x ~y)) `` `:957-970`). `Numbers.add(Object,Object)` (`Numbers.java:154-156`) is `return ops(x).combine(ops(y)).add((Number)x, (Number)y);`, and `ops` (`:1098-1119`) has **no** type check — unknown classes fall through to `else return LONG_OPS;` (`:1118`). The `(Number)x` is a JVM `checkcast`; the text `class java.lang.String cannot be cast to class java.lang.Number (java.lang.String and java.lang.Number are in module java.base of loader 'bootstrap')` is HotSpot's own — `Numbers.java` never constructs a `ClassCastException` (a grep for `ClassCastException` in the file finds nothing).
* **`ArityException`** (`src/jvm/clojure/lang/ArityException.java:16-32`): `super("Wrong number of args (" + (actual <= 20 ? actual : "> 20") + ") passed to: " + Compiler.demunge(name), cause);` (`:29`), `extends IllegalArgumentException` (`:16`). Demunging happens right there in the constructor; the raw munged class name is kept in the public `name` field (`:22`). Thrown from `AFn.throwArity` (`AFn.java:427-430`) and `RestFn` (`RestFn.java:393, 402, 415`).
* **`RT.seqFrom`** (`RT.java:561-578`): `throw new IllegalArgumentException("Don't know how to create ISeq from: " + c.getName());` (`:577`); reached from `RT.seq` (`:551-558`).
* **`RT.nthFrom`** (`:916-953`): `throw new UnsupportedOperationException("nth not supported on this type: " + coll.getClass().getSimpleName());` (`:951-952`); vectors used as fns: `APersistentVector.invoke` `throw new IllegalArgumentException("Key must be integer");` (`APersistentVector.java:354-357`).
* **Reflector** (`Reflector.java`): `noMethodReport` `"No matching method " + methodName + " found taking " + args.length + " args" + (contextClass != null ? " for " + contextClass : "")` (`:125-128`), thrown at `:167`/`:183`; `"Can't call public method of non-public class: "` (`:191`); `"No matching ctor found"` (`:317`, `:337`); `"No matching field found: " + fieldName + " for " + c` (`:384`, also `:407, 425, 444, 462`).
* **NPE.** Only two sites in `clojure.lang` construct an NPE (`Namespace.java:248` with a message, `Util.java:237` without); every other NPE — e.g. `(.foo nil)` — is raised by the JVM, so on JDK 14+ the "helpful NullPointerException" text (JEP 358, e.g. `Cannot invoke "Object.toString()" because "x" is null`) is the JDK's and mentions Java-level names, not Clojure locals.
* **Compiler resolution errors** are plain `RuntimeException`s (`Util.runtimeException` = `new RuntimeException(s)`, `Util.java:220-222`), which is why `ex-str` prints no `(Class)` for them: `"Unable to resolve symbol: " + sym + " in this context"` (`Compiler.java:7918`, `:8002`); `"No such namespace: " + sym.ns` (`:7971`); `"No such var: " + sym` (`:7976`); `"Can't take value of a macro: " + v` (`:7907`); `"Unable to resolve classname: " + tag` (`:1124`); `"No matching ctor found for " + c` (`:3104`).
* **Reader EOF/delimiter messages** (`LispReader.java`): top-level `"EOF while reading"` (`:268-270`); inside collections `readDelimitedList` records `firstline` (`:1392-1394`) and throws `"EOF while reading"` or `"EOF while reading, starting at line " + firstline` (`:1404-1408`); `UnmatchedDelimiterReader` `"Unmatched delimiter: " + rightdelim` (`:1374-1376`); `"EOF while reading string"` (`:571, 576`); `"Invalid token: " + s` (`:414`); `"Map literal must contain an even number of forms"` (`:1360`); `"Invalid number: " + s` (`:354`).
* **Laziness** (`LazySeq.java`): the thunk runs in `force()` — `sv = fn.invoke();` (`:48-53`) — which is called from `realize()` (`:82-101`, `s = RT.seq(ls)` at `:94`), which is called from `seq()`: `if(lock != null) realize(); return s;` (`:104-108`). `RT.seq` dispatches `((LazySeq) coll).seq()` (`RT.java:551-555`). Hence an exception thrown by `f` in `(map f coll)` surfaces wherever the seq is first walked, with frames `...$fn.invoke <- clojure.core$map$fn__N.invoke <- clojure.lang.LazySeq.force(LazySeq.java:50) <- clojure.lang.LazySeq.realize(:89) <- clojure.lang.LazySeq.seq(:106) <- clojure.lang.RT.seq(RT.java:555) <- <consumer>`. At the REPL the consumer is usually `prn`, so the REPL tags it `:print-eval-result` (`main.clj:444`) and prints `Error printing return value (ClassCastException) at ...` rather than pointing at the `map` call.

---

## 7. `clojure.stacktrace` and `clojure.repl/pst`

* `clojure.stacktrace/root-cause` (`stacktrace.clj:20-26`): walks `.getCause` to the last throwable.
* `print-trace-element` (`:28-38`): matches `#"^([A-Za-z0-9_.-]+)\$(\w+)__\d+$"` and, only when the method is `"invoke"`, prints `ns/fn` (`printf "%s/%s"`); everything else prints `class.method`; then ` (file:line)`. It does **not** call `Compiler/demunge`, so `my.ns$foo.invokeStatic` and `user$eval138$fn__139` are printed raw.
* `print-throwable` (`:40-48`): `"%s: %s"` class + message, then `pr` of `ex-data`.
* `print-stack-trace` (`:50-70`): **no frame filtering**; optional `n` caps the count; prints ` at ` for the first frame and `    ` for the rest.
* `print-cause-trace` (`:72-80`): recurses through causes printing `"Caused by: "`. `e` (`:82-87`) = 8 frames of the root cause of `*e`.
* `clojure.repl/root-cause` (`repl.clj:214-224`) differs: it **stops at a `CompilerException` whose `.source` is not `"NO_SOURCE_FILE"`**, so file-based compile errors keep their location wrapper.
* `clojure.repl/pst` (`:240-270`): defaults to `(pst 12)` on `(root-cause *e)` (`:244-249`); prints to `*err*` (`:251`); prefixes compile-phase errors with `"Note: The following stack trace applies to the reader or compiler, your code was not executed."` (`:252-254`); prints `SimpleClassName message ex-data` (`:255-257`); filters **only** `clojure.lang.RestFn` and `clojure.lang.AFn` frames (`:261-262`); frames rendered by `stack-element-str` (`:227-238`, demunged if file is `.clj`/`.cljc`/`NO_SOURCE_FILE`); recurses into `Caused by:` with depth `(min depth (+ 2 (- (count cause-frames) (count st))))` (`:264-269`).

---

## 8. Other deliberate extension points

* `ex-info` (`core.clj:4832-4839`) — constructs `ExceptionInfo` and strips its own frames: `(elide-top-frames (ExceptionInfo. msg map) "clojure.core$ex_info")`, with `elide-top-frames` (`:4822-4829`) doing `drop-while` on the class name and `.setStackTrace`. `ex-data` (`:4841-4846`) works on any `IExceptionInfo` (so `CompilerException` and `ReaderException` participate: `Compiler.java:7403`, `LispReader.java:148`). `ex-message`/`ex-cause` (`:4849-4863`, 1.10). `ExceptionInfo.toString` = `"clojure.lang.ExceptionInfo: " + getMessage() + " " + data.toString()` (`ExceptionInfo.java:38-40`).
* `print-method Throwable` -> `print-throwable` (`core_print.clj:509-546`): the `#error {\n :cause ... :data ... :via [{:type ... :message ... :data ... :at ...}] :trace [[cls meth file line] ...]}` tagged literal (1.7+); `print-method StackTraceElement` prints the 4-vector (`:464-465`). Both are multimethods, so a library can `defmethod` over them.
* `*print-namespace-maps*` (`core_print.clj:42-46`, dynamic, default `false`); `clojure.main/with-bindings` sets it `true` (`main.clj:88`) and `report-error` forces `false` for the EDN report (`:603`).
* `Throwable->map` (`core_print.clj:473`), `ex-triage`, `ex-str`, `err->msg`, `repl-caught`, `report-error`, `renumbering-read` are all public in `clojure.main`/`clojure.core` — the printer pipeline is data-in/data-out and re-composable.
* `:clojure.core/eval-file` and `:line` metadata on a top-level form are honoured by `Compiler.eval` (`Compiler.java:7729-7737`) — a tool can attribute REPL forms to a real file/line; `clojure.main/renumbering-read` (`main.clj:139-150`) implements exactly that for the REPL.
* `:elide-meta` compiler option (`Compiler.java:264`, `:292-303`) controls which metadata keys survive into emitted constants.
* `LispReader$ReaderException` / `Compiler$CompilerException` expose `line`/`column`/`source` as public fields and their `:clojure.error/*` data map; `clojure.main` imports both (`main.clj:19-20`).
* `IExceptionInfo` interface (`Compiler.java:7403`, `LispReader.java:148`, `ExceptionInfo.java:18`): any Throwable implementing it is data-bearing for `ex-data`/`Throwable->map`.

---

## Hook points summary

| hook | kind | what it controls | limitations |
|---|---|---|---|
| `clojure.main/repl :caught` (`main.clj:407-410, 424`) | option (fn) | replaces the whole error printer for one `repl` call | only for REPLs started through `clojure.main/repl`; nREPL etc. have their own loop |
| `clojure.main/repl-caught` (`:347`) | plain fn | default `:caught` | not dynamic; `alter-var-root` only |
| `clojure.main/ex-triage` / `ex-str` / `err->msg` (`:207, 268, 342`) | plain fns, public | the data->message pipeline used by both REPL and `report-error` | not dynamic; `alter-var-root`; input is limited to what `Throwable->map` carries (file:line, no column at execution time) |
| `clojure.main/report-error` (`:585`) | plain fn | uncaught-error report in script/`-m` mode | not dynamic; called only from `main` |
| `-Dclojure.main.report` / `--report` (`:656-657, 667`) | system property / CLI flag | `file` (default) / `stderr` / `none` | just target selection; format fixed |
| `clojure.spec.alpha/*explain-out*` (`spec_alpha.clj:259`; bound in `main.clj:95`) | dynamic var | how spec problems are rendered inside `ex-str` (`main.clj:291, 331`) | `ex-str` only strips `:in` when the printer is the default one |
| `clojure.core/*err*` (`RT.java:192-193`) | dynamic var | destination of `repl-caught`, `report-error`, `pst` | changes where, not what |
| `*e` (`main.clj:447, 454, 462`) | dynamic var | last REPL exception for `pst` / `e` | REPL-only |
| `print-method Throwable` / `StackTraceElement` (`core_print.clj:545, 464`) | multimethod | `pr`/`prn` rendering (`#error {...}`) | not used by `repl-caught` (it uses `ex-str`) |
| `Throwable->map` (`core_print.clj:473`) | plain fn | data shape consumed by `ex-triage` | root-only `:trace`, raw JVM frames |
| `clojure.repl/pst`, `clojure.stacktrace/print-*` (`repl.clj:240`, `stacktrace.clj:50-80`) | plain fns, opt-in | on-demand traces | minimal/no filtering; `stacktrace` doesn't demunge |
| `:clojure.core/eval-file` / `:line` form meta (`Compiler.java:7729-7737`) | metadata protocol | file/line attributed to forms passed to `eval` | only lists carry `:line` (1a); line only reaches bytecode |
| `:elide-meta` compiler option (`Compiler.java:292-303`) | compiler option | which meta keys survive into constants | affects emitted data, not messages |
| `-Dclojure.spec.skip-macros` (`RT.java:325, 364`) | system property | whether `macroexpand-check` runs at all | boolean, JVM-wide |
| `Thread/setDefaultUncaughtExceptionHandler` | JVM API | errors on non-main threads | `clojure.main` catches main-thread errors first (`main.clj:666-668`) |

## Consequences for tooling

1. **Column/sub-expression precision is only available before execution.** The reader records `:column` only on lists (`LispReader.java:1254-1255`), the compiler keeps it in `COLUMN` (`Compiler.java:308`) and `CompilerException` data (`:7441`), but bytecode receives only `line` (`:4314`, `asm/MethodVisitor.java:759`). So `read-source`/`compile-syntax-check`/`macro-syntax-check` errors can carry `file:line:col`; `execution` errors can never do better than `file:line` from a `StackTraceElement`.
2. **Errors inside strings/keywords/numbers/symbols are attributed to the enclosing list's `(`** because those forms have no metadata slot or are never given one (1a) — a library wanting per-token positions must re-read the source itself (e.g. with `LineNumberingPushbackReader`, `:41-62`) and map from the enclosing form's `:line/:column`.
3. **Macro-generated code reports the call-site line for every generated form** (syntax-quote strips positions, `LispReader.java:1126-1131`; `analyzeSeq` inherits the binding, `Compiler.java:7664-7675`), so a stack frame at "line 12" may be inside code the user never wrote; and `:line` meta on `&form` is not preserved across successive expansions (`errors.clj:63`), except where macros copy it explicitly (`core.clj:45-47, 4618-4622`).
4. **The `SourceFile` attribute is the bare file name** (`Compiler.java:4887-4910`); the full path exists only in the SMAP. To resolve `foo.clj:12` a tool must search the classpath or use the var's `:file` meta (`DefExpr.parse` stores `RT.FILE_KEY` = `*file*`, `:572-574`).
5. **Phase classification depends on exception *class*, not on where it was thrown**: `IllegalArgumentException`/`IllegalStateException`/`ExceptionInfo` from a macro body become `macro-syntax-check`, a bare `Exception` also does, anything else becomes `macroexpansion` (`Compiler.java:7596-7610`); reader errors during AOT `compile` are reported as `compile-syntax-check` (`:8484-8486`). Tools cannot rely on phase to mean "user-facing syntax problem".
6. **The default REPL never prints a stack trace and `report-error` writes it to a temp file** (`main.clj:347-351, 605-614`); the only complete trace data structure is `Throwable->map`'s `:trace`, which holds root-cause frames only (`core_print.clj:500-501`) — frames of intermediate causes are available only via `.getStackTrace` on each `:via` entry.
7. **`ex-triage`'s "user frame" heuristic is a hard-coded namespace list** (`main.clj:50-60`); any library namespace not in that set is treated as user code, so a wrapper library's own frames will become the reported `:clojure.error/symbol`/`:line` unless it elides them (pattern: `elide-top-frames`, `core.clj:4822-4829`).
8. **Most runtime message text is Java's** (`checkcast` CCE from `Numbers.java:154-156, 1118`; JDK helpful NPEs; `Reflector.java:125-128`; `RT.java:577`); Clojure only adds the `Execution error (Class) at sym (file:line).` headline (`main.clj:336-340`). Improving these means pattern-matching JVM strings or intercepting at the `:caught`/`ex-str` layer, not a Clojure-side switch.
9. **Frame names are reconstructible but lossy**: `demunge` maps `$`->`/` and `_`->`-` (`Compiler.java:3396-3398`), so `fn__139` prints as `fn--139` and a fn literally named `a_b` becomes `a-b`; ids from `RT.nextID` (`RT.java:523-525`) differ between runs, so frame strings must not be used as stable keys.
