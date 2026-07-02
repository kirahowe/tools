# Running JVM Clojure in the browser: state of the art (July 2026)

This document surveys the options for running *real* JVM Clojure — not a
reimplementation — inside a browser, explains the choice made in this app,
and sketches the path toward the ultimate goal: running
[Clay](https://scicloj.github.io/clay/) notebooks on users' hardware instead
of per-notebook server sandboxes (as [hosted-clay](https://github.com/kirahowe/hosted-clay)
does today with Fly.io Sprites).

## The constraint that shapes everything

Clojure is not just "a language that compiles to JVM bytecode." Its
compilation unit is the *form*: every `eval`, every REPL submission, every
`defn` **generates JVM bytecode at runtime** and loads it through
`clojure.lang.DynamicClassLoader`. There is no interpreter fallback in
Clojure proper.

That single fact sorts the whole landscape into two piles:

1. **Ahead-of-time compilers** (bytecode → JS/WASM at build time). These can
   run a *fixed, precompiled* Java/Clojure program, but they cannot host a
   REPL or a notebook kernel, because no new classes can come into existence
   at runtime.
2. **Actual JVMs that happen to run in a browser.** Only these can run
   `eval`, and therefore only these can run a Clojure REPL, nREPL, or a Clay
   kernel.

## The options

### CheerpJ (Leaning Tech) — the chosen one ✅

- **What it is:** a full JVM implementation (written in C++, compiled to
  WebAssembly) plus a complete **unmodified OpenJDK runtime**, running
  fully client-side. Java bytecode starts in an interpreter and hot code is
  JIT-compiled — notably to *JavaScript*, so the browser's own JIT then
  compiles it to machine code.
- **Capabilities that matter for Clojure:** dynamic class loading ✔,
  reflection ✔, threads ✔, JNI ✔ (since 4.0). This is why Clojure's
  compiler just works on it — it's a real JVM as far as bytecode is
  concerned.
- **Versions:** CheerpJ [4.0 (Feb 2025)](https://labs.leaningtech.com/blog/cheerpj-4.0)
  added Java 11 + JNI; 4.2 added Java 17;
  [4.3 (Apr 2026)](https://bytecode.news/posts/2026/04/cheerpj-4-3-webassembly-based-jvm-for-the-browser)
  is current. Per the [roadmap](https://cheerpj.com/our-roadmap-for-modern-java-in-the-browser/),
  Java 21 lands with CheerpJ 6.0 in early 2026 and LTS parity (Java 26) is
  targeted by end of 2026.
- **Delivery:** a ~few-line `<script>` include from their CDN
  (`https://cjrtnc.leaningtech.com/4.3/loader.js`). The runtime streams in
  on demand. Web server root is mounted read-only at `/app/` inside the
  JVM's virtual filesystem; `/files/` is a persistent IndexedDB-backed FS.
- **Library mode** (`cheerpjRunLibrary`) exposes Java classes to JS as async
  proxies — which is exactly how this app drives
  `clojure.java.api.Clojure` / `load-string` without any Java glue code.
- **Costs / caveats:**
  - First boot downloads the runtime and JIT-compiles `clojure.core`
    (expect tens of seconds; subsequent loads are much faster thanks to
    browser caching).
  - Everything runs in the tab: heavy computation blocks UI unless moved to
    a worker; memory is capped by the browser (~2–4 GB per tab).
  - No raw sockets (browser sandbox); network is fetch/WebSocket-shaped.
  - **Licensing:** free for personal use and technical evaluation;
    commercial/hosted use requires a license from Leaning Tech. This
    matters for a hosted notebook product and should be scoped early.

### GraalVM Web Image (native-image → WASM) — promising, but AOT ❌ (for a kernel)

Oracle [announced a WebAssembly backend for Native Image](https://thenewstack.io/graalvm-finally-gets-java-for-webassembly/)
in April 2025 (`native-image --tool:svm-wasm`). It AOT-compiles a Java
application to a WASM module (leaning on WasmGC) with a JS wrapper. As of
GraalVM 25.x it is [explicitly experimental](https://www.graalvm.org/latest/reference-manual/web-image/)
and — decisively for us — it inherits Native Image's **closed-world
assumption**: no runtime class definition, hence no Clojure `eval`, hence no
REPL/notebook kernel. Clojure's own runtime does AOT-compile under
native-image, so a *fixed* Clojure program could ship this way.

Worth watching for a different reason: a mature Web Image could AOT-compile
the *library universe* (tablecloth, tech.ml.dataset, Clay's rendering
machinery) to WASM, with **SCI interpreting user code on top** (SCI
interprets — it never emits bytecode, so it dodges the closed-world wall).
That hybrid would trade full Clojure semantics for a much smaller, faster
runtime. It's the most credible future alternative to CheerpJ for the
notebook use case.

### TeaVM, JWebAssembly, Bytecoder — AOT transpilers ❌

TeaVM (the most mature of these) compiles bytecode to JS/WASM ahead of time
and is excellent for Java/Kotlin apps, but there's no dynamic class loading:
no `eval`, no kernel. Same story for JWebAssembly and Bytecoder.

### DoppioJVM — proof of concept, abandoned ❌

A JVM interpreter in TypeScript (c. 2014–2017) that *could* do dynamic
classloading and famously booted Clojure's REPL in a tab — slowly, on a
Java-8-era libc. Unmaintained for years; historically important as the
existence proof, not a current option.

### Not-really-JVM alternatives (for contrast)

Self-hosted ClojureScript, [SCI](https://github.com/babashka/sci)/scittle,
and nbb give you a Clojure *experience* in the browser today with instant
startup — but none of them can load JVM libraries, and Clay's value comes
precisely from the JVM data-science stack (tech.ml.dataset, tablecloth,
kindly renderers…). They're complements (fast path for simple cells), not
substitutes.

## What this app demonstrates (and what it doesn't)

- `bootstrap.clj` — a ~90-line REPL engine (read → eval → print with ns
  persistence, output capture, and a JSON wire contract) — is **verified by
  unit tests on a real JVM** and is exactly what CheerpJ loads in the tab.
- The UI, datastar signal wiring, and the eval contract are **verified
  end-to-end with Playwright** in `?mode=server` (same contract, dev-server
  JVM standing in for the browser JVM).
- The `wasm` path (CheerpJ itself) could not be exercised in the sandbox
  this was built in — its egress policy blocks `cjrtnc.leaningtech.com` —
  so the first real-browser boot happens on your machine. The moving parts
  unique to that path are small: `cheerpjInit` → `cheerpjRunLibrary` with
  the three Clojure jars → `Clojure.var("clojure.core", "load-string")`.

## Implications for hosted-clay ("run notebooks on users' hardware")

A realistic migration path, in increasing ambition:

1. **Browser REPL beside the notebook (this app).** Real JVM eval with the
   core language. Ship it as an "instant scratchpad" while sprites keep
   doing the heavy lifting.
2. **Clay-lite kernel in the tab.** Load Clay + kindly + hiccup (pure-JVM
   jars served like the Clojure jars here; CheerpJ mounts the site at
   `/app/`). Render `kind/*` values to HTML in-browser. Open questions to
   prototype: total classpath weight (likely 30–100 MB — cacheable, but
   real), boot latency per tab, and any AWT/filesystem corners Clay touches
   (CheerpJ implements a lot of both, but Clay's file-watching workflow
   would be replaced by direct function calls).
3. **Full "local mode" for hosted-clay.** The server becomes a static
   notebook store + jar CDN; user code never leaves the tab (a genuinely
   nice privacy/critical-cost story — sprites only spin up for
   full-fidelity runs, native deps, or big data). A Web Worker hosts the
   JVM to keep the UI responsive; notebooks persist through CheerpJ's
   IndexedDB filesystem.

Blockers to resolve before committing to (3): CheerpJ commercial licensing
for a hosted product; libraries with native components (e.g. parts of the
dtype-next/Arrow world) which need JNI ports or exclusion; and memory
ceilings for real datasets.

## Sources

- [CheerpJ](https://cheerpj.com/) · [4.0 release](https://labs.leaningtech.com/blog/cheerpj-4.0) · [4.3 release note](https://bytecode.news/posts/2026/04/cheerpj-4-3-webassembly-based-jvm-for-the-browser) · [roadmap](https://cheerpj.com/our-roadmap-for-modern-java-in-the-browser/) · [cheerpj-meta](https://github.com/leaningtech/cheerpj-meta) · [HN discussion of 4.0](https://news.ycombinator.com/item?id=43772588)
- [GraalVM Web Image docs](https://www.graalvm.org/latest/reference-manual/web-image/) · [The New Stack on the WASM backend](https://thenewstack.io/graalvm-finally-gets-java-for-webassembly/) · [GraalVM 25](https://www.infoworld.com/article/4061937/graalvm-25-arrives-backed-by-jdk-25.html)
- [Clojure REPL reference](https://clojure.org/reference/repl_and_main) (the runtime-compilation model)
- [datastar](https://data-star.dev/) · [releases](https://github.com/starfederation/datastar/releases)
- [Clay](https://scicloj.github.io/clay/) · [hosted-clay](https://github.com/kirahowe/hosted-clay)
