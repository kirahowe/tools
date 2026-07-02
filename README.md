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
node test/e2e.js test/tabi.jpg            # clean-ish photo, no dewarp
node test/e2e.js test/tabi.jpg --deskew   # with dewarping (slower)
```

Artifacts (the produced MusicXML and a screenshot) land in
`test/artifacts/`. The sample image `test/tabi.jpg` is the phone-taken demo
photo from the oemer repository (MIT).

## Expectations & caveats

- **Speed.** Recognition is minutes-per-page, not seconds: the two U-Nets
  slide over ~200 patches each, then oemer's post-processing does a lot of
  pixel-level work in Python. Multi-threaded wasm (COOP/COEP headers) and
  WebGPU (tried automatically, falls back to wasm) help. A phone photo at
  ~3–4 MP is the design point; bigger images are resized to that anyway.
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
