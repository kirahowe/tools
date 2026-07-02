# Can dtype-next (and therefore noj) run in a browser JVM?

[noj](https://scicloj.github.io/noj/) is the ecosystem entrypoint, but its
data spine is a tower with one foundation:

```
noj → tablecloth → tech.ml.dataset (TMD) → dtype-next
      tableplot ──→ (vega specs — pure data, no problem)
      fastmath ───→ (pure JVM math, mostly fine)
      clay/kindly → (pure JVM rendering, fine)
```

Everything above dtype-next is ordinary JVM code that a real JVM-in-WASM
(CheerpJ) should load the way it loads any jar. dtype-next is where the
low-level tricks live, so it decides how much of noj can follow. This doc
goes through what it actually uses, feature by feature, against what a
browser JVM can offer.

## What dtype-next is doing under the hood

Its [deps](https://github.com/cnuernber/dtype-next/blob/master/deps.edn)
and design sort into four buckets:

**1. Pure-JVM machinery (fine in any real JVM).**
RoaringBitmap (missing-value indexes), commons-math3, JTransforms (FFT),
ham-fisted (Chris's high-performance collections), primitive-math,
camel-snake-kebab. These are ordinary bytecode. CheerpJ runs ordinary
bytecode.

**2. Runtime bytecode generation — `insn` + ASM 9.2.**
dtype-next emits specialized classes at runtime (typed buffer
implementations, fused readers). This is the same capability Clojure's own
compiler needs — dynamic classloading — and it's exactly the thing that
made CheerpJ the only viable runtime in the first place. CheerpJ
advertises dynamic class loading and reflection as supported; our REPL
*is* that feature working. Expected: **works** (perf is JIT-dependent).

**3. Off-heap memory: `sun.misc.Unsafe`, DirectByteBuffers, and mmap.**
This is the heart of the question.

- `tech.v3.datatype.native-buffer` allocates and addresses raw memory via
  Unsafe (`allocateMemory`/`putLong`/`getLong`...).
- mmap comes from `org.xerial.larray/larray-mmap` — which is **JNI-backed**
  (ships per-platform native `.so`/`.dylib`). It's how TMD reads
  Arrow/Parquet/nippy without copying.
- The FFI layer (`tech.v3.datatype.ffi`) has JNA, JDK-Panama, and
  GraalVM backends — for zerocopy interop with NumPy/OpenCV/Julia.

Against a browser JVM:

| feature | verdict |
|---|---|
| on-heap primitive arrays (dtype-next's default containers) | works — plain JVM semantics |
| `ByteBuffer/allocateDirect` | plausibly works — OpenJDK's NIO is compiled into CheerpJ's runtime; "direct" memory just lands in WASM linear memory |
| `sun.misc.Unsafe` memory access | **unknown — the decisive probe.** CheerpJ is a real JVM implementation so it *can* implement Unsafe against linear memory, but it isn't documented either way |
| mmap via larray-mmap | **no, as-is** — it loads a platform-native JNI library that doesn't exist for WASM. CheerpJ 4's JNI supports libraries *recompiled to WASM with their toolchain*, so a port is possible but is real work |
| JNA / Panama FFI | no (JNA needs its native dispatch lib; Panama needs JDK 21+, CheerpJ tops out at 17 until CheerpJ 6) |
| memory | hard ceiling ~2 GB (wasm32 address space), practically less; no swap |

**4. JDK-version fast paths.** dtype-next's `:jdk-17`/`:jdk-19` aliases
turn on the foreign-memory and vector incubator modules
(`--add-modules jdk.incubator.foreign --enable-native-access=ALL-UNNAMED`).
These are *fast paths*, not requirements — the library runs on JDK 8/11
without them (JDK 16 specifically is unsupported). CheerpJ offers 8, 11,
and 17, so the version gate itself is not the problem; the flags just
won't buy anything there.

## The empirical answer: run the probe

Theory only goes so far, so the notebook app ships a probe:

```
http://localhost:8080/notebook.html?example=jvm-probe        (wasm kernel)
```

It exercises exactly the list above — heap arrays, DirectByteBuffer,
Unsafe alloc/read/write, `FileChannel/map`, threads/`pmap`, runtime
`defrecord` via `eval`, `proxy`, reflection, max heap — and renders a
pass/fail table *from inside whatever JVM is running it*. On the server
kernel (a stock JVM) everything passes; the interesting run is the wasm
kernel on your machine. That result decides which of the strategies below
is live.

## Strategies, best case to worst

1. **If Unsafe + DirectByteBuffer pass:** most of dtype-next's *in-memory*
   world is available. On-heap and native buffers both work; what's lost
   is mmap (Arrow/Parquet zerocopy) and FFI. TMD can still read
   CSV/JSON/transit from streams — in a browser you'd fetch data over
   HTTP anyway. Realistic outcome: **tablecloth on moderate data works**,
   file-format ingestion is the compromise.
2. **If Unsafe fails but heap arrays pass (they will):** dtype-next's
   default containers are heap-backed, so a meaningful subset may still
   load and run — `->dataset` on seqs of maps, column ops, group-by,
   joins. Native-buffer code paths would need to be avoided (and a small
   patch upstream to make native-buffer loading lazy/optional would go a
   long way — Chris has been receptive to runtime-portability work
   before; dtype-next already special-cases GraalVM native).
3. **The tmdjs detour:** [tmdjs](https://github.com/cnuernber/tmdjs)
   reimplements the tech.v3.dataset API over JS typed arrays for
   ClojureScript — same author, same idioms, transit-friendly. Paired
   with the scittle kernel it gives "tablecloth-feeling" notebooks that
   boot in under a second, no JVM at all. It's not noj — but it's a very
   credible fast path for the 80% notebook.
4. **The AOT hybrid (watch, don't build yet):** GraalVM Web Image
   AOT-compiles a *fixed* classpath (noj's library universe — dtype-next
   already supports GraalVM native) to WASM, with SCI interpreting user
   code on top. No runtime bytecode gen needed for user cells because SCI
   interprets. Experimental today; the most interesting alternative to
   CheerpJ in 1–2 years.

## Bottom line

The noj stack was engineered for exactly one controversial thing —
treating memory as a raw, addressable resource — and that's the one thing
browsers meter carefully. Everything else (bytecode gen, reflection,
threads, the pure-JVM libraries) is already demonstrated or documented to
work under CheerpJ. So the question isn't "can noj run in a browser" in
the abstract; it's "does CheerpJ implement Unsafe-over-linear-memory, and
can we live without mmap." The first half is one probe-run away; the
second half is a data-loading strategy (fetch + parse instead of mmap),
not a dealbreaker.
