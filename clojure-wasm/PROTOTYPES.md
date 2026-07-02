# Notebook hosting prototypes

`notebook.html` is a working prototype of the hosted-clay "run notebooks on
users' hardware" idea: Clay-flavored notebooks (markdown + code cells,
kindly-style rendering, save/share links) where **the execution kernel is
pluggable**. The three kernels are the three hosting options, and you can
switch between them on the same document with the toolbar (or `?kernel=`):

```
bb jars && bb dev
open http://localhost:8080/notebook.html?example=welcome
```

## The options

### 1. `?kernel=wasm` — a real JVM in the user's tab (CheerpJ)

The hosted-clay endgame. OpenJDK-on-WebAssembly boots in the tab, loads the
Clojure jars from your server, and evaluates cells locally. User code and
data never leave the machine; the server's only jobs are storing notebook
documents and serving jars.

- **Fidelity:** real Clojure, real JVM — `eval`, interop, threads, macros.
- **Boot:** ~30–120s first time (CDN download + JIT of clojure.core), much
  faster cached. Fine for a "reader that can also run" page; needs a
  warm-boot story (Web Worker + keep-alive) for an editor you live in.
- **Costs:** browser memory ceiling (~2 GB), main-thread evals block the UI
  (worker move is the known fix), CheerpJ commercial license for a hosted
  product, and the dtype-next question (see `docs/dtype-next.md`).

### 2. `?kernel=server` — a JVM you host (what sprites do today)

The same cell contract evaluated by `POST /api/eval-cell` on the dev
server. In hosted-clay terms this is the Fly.io Sprite: full JVM, native
deps, real filesystem, big memory — and per-notebook infrastructure cost,
cold-start latency, and user code running on *your* machines.

In this prototype it exists for two reasons: it's the stand-in that makes
the whole UI testable in CI, and it demonstrates that **the notebook
document doesn't care where the kernel lives** — which is the architectural
point: hosted-clay could offer "local" and "sprite" as a per-notebook
toggle on the same document format.

### 3. `?kernel=scittle` — SCI in the tab (experimental)

[Scittle](https://github.com/babashka/scittle) (the SCI interpreter
compiled to JS) loads in well under a second and evaluates the same
notebook-engine contract, interpreted, with no JVM. No JVM libraries, no
`deftype`-heavy code, different numerics — but *instant*. The interesting
pairing is [tmdjs](https://github.com/cnuernber/tmdjs) (tech.ml.dataset's
API reimplemented over JS typed arrays, by TMD's own author), which could
give this kernel real dataframe chops.

This kernel is marked experimental: it's wired and should work where the
CDN is reachable, but it couldn't be exercised in the sandbox this was
built in, and namespace bookkeeping is looser than the JVM kernels.

## What the prototype demonstrates

- **One document, three runtimes.** Cells are stored as plain
  `{type, source}`; the kernel is a URL parameter.
- **Kindly-style rendering without Clay's weight.**
  `notebook-engine.cljc` (~200 lines, runs on JVM *and* SCI) classifies
  each cell's last value — seq-of-maps → table, keyword-vector → hiccup,
  `:$schema` map → vega-lite, `(kind/md …)` → markdown — and renders
  server-of-truth HTML inside the kernel, so the JS side stays dumb.
- **The hosted-clay loop:** create → edit → run → save (`PUT
  /api/notebooks/:id`) → share a read-only link (`?nb=…&view=1`) that
  auto-runs on the reader's own kernel. That last bit is the demo: sharing
  a notebook that executes on the *reader's* hardware.
- **The dtype-next probe:** `?example=jvm-probe` runs the JVM capability
  checks that decide how much of the noj stack can follow
  (see `docs/dtype-next.md`).

## Where I'd take it for hosted-clay

Hybrid, per-notebook:

1. **Default: wasm kernel.** Boot it in a Web Worker at page load; readers
   get Clay-quality rendered documents that turn interactive when the JVM
   is up. Zero marginal compute cost per reader.
2. **Escalate to a sprite** when the notebook declares needs the browser
   can't meet (native deps, > ~1.5 GB working set, secrets/data that live
   server-side). Same document, same contract — just a different
   `evalCell` transport, exactly like `?kernel=server` here.
3. **Scittle as the editor's instant preview** (and possibly the mobile
   reader path), with tmdjs standing in for tablecloth on simple tables.

Concrete next steps, roughly in order of information gained per effort:

- Run `?example=jvm-probe` on the real CheerpJ kernel (any machine with
  normal internet) — it settles the Unsafe/DirectByteBuffer/mmap questions
  empirically.
- Add tablecloth + TMD jars to `jars/manifest.json` and try
  `(require 'tablecloth.api)` on the wasm kernel. The manifest/classpath
  plumbing is already there; this is a 10-minute experiment on your laptop.
- Move the wasm kernel into a Web Worker (CheerpJ supports it) so long
  evals stop blocking the UI, and persist the CheerpJ IndexedDB filesystem
  so notebooks can write files.
- Talk to Leaning Tech about licensing before betting the hosted product
  on CheerpJ.
