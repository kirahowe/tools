# Sheet Music Scanner

Take a picture of sheet music and hear it played back — entirely in your
browser. No backend, no uploads: optical music recognition (OMR), score
rendering and audio synthesis all run client-side.

## How it works

```
photo ──▶ OMR worker (Web Worker) ─────────────▶ MusicXML
          │                                          │
          │  Pyodide (CPython on wasm):              ▼
          │    oemer's pre/post-processing      OSMD renders the score
          │    (numpy, OpenCV, scipy, sklearn)       │
          │  ONNX Runtime Web (wasm/WebGPU):         ▼
          │    oemer's two U-Net models         Tone.js plays it, cursor
          └─────────────────────────────────    follows along
```

The heavy lifting is [oemer](https://github.com/BreezeWhite/oemer), an
end-to-end OMR system. It's a Python package, but nearly all of its
dependencies (numpy, OpenCV, scipy, scikit-learn) ship with
[Pyodide](https://pyodide.org), so the whole pipeline runs on WebAssembly in
a Web Worker. The one exception is `onnxruntime`: oemer's two neural nets
are ONNX models, and those run natively in the browser on
[ONNX Runtime Web](https://onnxruntime.ai/docs/tutorials/web/) instead.

Bridging the two runtimes is the interesting bit: oemer's `inference()`
is synchronous Python, but ORT Web's `session.run()` is async JavaScript.
Instead of blocking Python on a JS promise (needs JSPI, which isn't
available everywhere), `py/omr_bridge.py` splits `inference()` at the
`sess.run` boundary:

1. **Python** slices the (resized) image into overlapping 256/288 px patches
   — exactly the way oemer does;
2. **JS** runs the ONNX models over the patches in batches, streaming each
   batch's predictions back into a Python-side accumulator (this also keeps
   peak memory down);
3. **Python** merges the patches into class maps, then oemer's own
   `ete.extract()` runs unmodified — with its `inference` function
   monkeypatched to return the precomputed maps — through staffline
   extraction, dewarping, symbol/rhythm extraction and MusicXML building.

`py/oemer_compat.py` carries a small runtime patch for oemer 0.1.8 on the
modern numpy/OpenCV that Pyodide ships (newer OpenCV returns `HoughLinesP`
results with a different shape).

The result MusicXML is rendered with
[OpenSheetMusicDisplay](https://opensheetmusicdisplay.org/) and scheduled
onto a [Tone.js](https://tonejs.github.io/) synth; the OSMD cursor advances
in step with playback. (The render/playback code is adapted from the
`claude/sheet-music-player-TI5ok` branch, which did the same but with OMR
on a server.)

## Running it

Everything is static files — there is no build step.

```bash
python3 serve.py            # http://localhost:8000
```

`index.html` is a landing page that explains what the tool does, its
limitations, and exactly what the ~175 MB opt-in download involves —
nothing heavy loads there. The scanner itself lives at `scan.html`; opening
it is the opt-in, and it immediately brings up the engine and fetches the
default models (with progress) so recognition can start as soon as an
image is chosen.

By default all runtime assets are fetched lazily from public CDNs:
Pyodide and its wheels from jsDelivr, the oemer wheel from PyPI, and the
two model checkpoints (~110 MB, cached in the browser after the first run)
from oemer's GitHub release. So any static host works — but `serve.py` is
recommended because it sets `Cross-Origin-Opener-Policy` /
`Cross-Origin-Embedder-Policy` headers, which let ONNX Runtime Web use
multi-threaded wasm (noticeably faster).

### Fully offline / self-hosted

```bash
python3 tools/fetch_vendor.py   # ~700 MB into ./vendor
python3 serve.py
```

The app probes for `vendor/pyodide/pyodide.js` at startup and, if present,
loads everything (Pyodide, ORT, OSMD, Tone, the oemer wheel, the models)
from `./vendor` instead of the network.

## Testing

A headless end-to-end test drives the real pipeline (image → OMR →
MusicXML → rendered score → playback schedule) in Chromium via Playwright:

```bash
npm install playwright        # or reuse an existing install via NODE_PATH
node test/e2e.js test/tabi.jpg            # default engine, no dewarp
node test/e2e.js test/tabi.jpg --deskew   # with dewarping
node test/e2e.js test/tabi.jpg --engine std:256   # Fast engine
```

Artifacts (the produced MusicXML and a screenshot) land in
`test/artifacts/`. The sample image `test/tabi.jpg` is the phone-taken demo
photo from the oemer repository (MIT).

## Performance & download size (measured)

Time on a 4-core machine, headless Chromium, multi-threaded wasm, on the
sample phone photo (whole pipeline, image → MusicXML):

| stage | time |
|---|---|
| engine bring-up (Pyodide + packages + oemer) | ~15 s |
| pass 1: unet_big (staff/symbols), 232 patches | ~180 s |
| pass 2: seg_net (noteheads/clefs), 232 patches | ~350 s |
| post-processing + dewarp + MusicXML | ~55 s |
| **total** | **~10 min** |

Inference is ~90% of the wall clock, so the **Engine** selector trades it
against fidelity:

- **Max fidelity** (default) — oemer's fp32 models, dense window overlap
  (step 128). The reference configuration.
- **Light** — same fp32 unet_big, int8-quantized seg_net (99.96% pixel
  agreement, but boundary pixels matter: ~92% of note pitches match Max
  end-to-end on the sample page; durations ~99%). Same speed, 28 MB
  smaller download.
- **Fast** — Light models with step 256 (no window overlap): measured
  3.2× faster in the browser (587 s → 183 s on the sample page), ~90% of
  pitches match Max. Fine for a quick listen, not for transcription.

Notes from the tuning experiments (so nobody re-treads dead ends):

- int8 quantization is a **download** win, not a speed win — QDQ overhead
  cancels the compute savings (measured: seg_net 3.7 s/batch fp32 vs 4.0 s
  int8 natively). unet_big *cannot* be int8-quantized naively: its
  staffline/symbol channels collapse to ~0.5% recall regardless of
  calibration method or first/last-layer exclusion.
- Threads: wasm inference uses `min(8, hardwareConcurrency)` threads when
  cross-origin isolated (COOP/COEP), 1 otherwise — that difference alone
  is worth ~3–4×, which is why `serve.py` sets the headers.
- WebGPU is attempted first and silently falls back to wasm. On machines
  with a real GPU this is the biggest untapped speedup; the models are
  opset 9, which limits current WebGPU op coverage — converting to opset
  13+ (see `tools/quantize_models.py` for the pattern) is the starting
  point if you pick this up.

Download, first visit (all cached for later visits — models in the Cache
API, the rest in the HTTP cache):

| component | size |
|---|---|
| models (Max: both fp32; Light saves 28 MB) | 109.2 MB |
| Pyodide packages (numpy, OpenCV, scipy, sklearn…) | 38 MB |
| Pyodide core (wasm + stdlib) | 12.3 MB |
| ONNX Runtime Web (wasm) | 13.9 MB |
| OSMD + Tone.js + oemer wheel | 2.9 MB |
| **total** | **~176 MB** (~160 MB over the wire with CDN compression; Light: ~148 MB) |

(matplotlib and friends are stubbed out in the worker, saving ~9 MB of
wheels that oemer imports but never uses.)

## Expectations & caveats

- **Speed.** Recognition is minutes-per-page, not seconds — see the table
  above. A phone photo at ~3–4 MP is the design point; bigger images are
  resized to that anyway.
- **Memory.** Expect the worker to peak around 1–1.5 GB. Desktop browsers
  are fine; older phones may not be.
- **Accuracy.** oemer is good on clean scans and decent on phone photos,
  but no OMR is perfect — expect occasional wrong pitches/rhythms on dense
  scores. "Photo mode (dewarp)" straightens curved stafflines on
  hand-held shots; turn it off for flat scans (it's faster).
- **Playback fidelity.** Notes are extracted from OSMD's iterator and
  played on a simple polyphonic synth. Repeats, voltas, grace notes,
  dynamics and articulations are not modelled.

## Why in-browser OMR (and which approach)

Prior art on the `claude/sheet-music-player-TI5ok` branch ran oemer on a
Cloud Run backend. For a client-only version the options were:

1. **Pyodide + ONNX Runtime Web** (chosen): oemer's Python runs as-is on
   wasm; the models run on ORT Web. No porting, full pipeline fidelity,
   ~170 MB one-time download (Python runtime + packages + models), all
   cacheable.
2. Port oemer's post-processing (~6k lines of numpy/OpenCV image wrangling)
   to JavaScript with only the U-Nets on ORT Web — much smaller download,
   but a large, risky rewrite that would drift from upstream.
3. Other OMR engines: [homr](https://github.com/liebharc/homr) (transformer
   encoder-decoder, PyTorch — no realistic wasm path), Audiveris (Java),
   commercial APIs (not client-side).

Option 1 is the only one that gets the complete, battle-tested pipeline
into the browser without a rewrite.
