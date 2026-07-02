// OMR worker: runs oemer's full pipeline in the browser.
//
// Two wasm runtimes cooperate here:
//   - Pyodide runs oemer's Python pre/post-processing (numpy, OpenCV, scipy,
//     scikit-learn) unmodified, via a small bridge module (py/omr_bridge.py).
//   - ONNX Runtime Web runs the two U-Net segmentation models.
//
// oemer's own `inference()` is split around its `sess.run()` call: Python
// prepares the sliding-window patches, JS runs the neural nets (async), and
// Python merges the predictions and finishes the pipeline. That sidesteps the
// sync-Python-calling-async-JS problem without needing JSPI.

/* global loadPyodide, ort */

let assets = null;
let baseURL = null;
let pyodide = null;
let bridge = null;
const sessions = {}; // model name -> ort.InferenceSession

const post = (msg) => self.postMessage(msg);
const status = (stage, detail, pct) => post({ type: "status", stage, detail, pct });

const abs = (url) => new URL(url, baseURL).href;

// ---------- engine bring-up -------------------------------------------------

async function init() {
  status("loading-pyodide");
  importScripts(abs(assets.pyodideIndexURL + "pyodide.js"));
  pyodide = await loadPyodide({
    indexURL: abs(assets.pyodideIndexURL),
    stdout: (line) => post({ type: "log", line }),
    stderr: (line) => post({ type: "log", line }),
  });

  status("loading-packages");
  await pyodide.loadPackage(
    ["numpy", "opencv-python", "scipy", "scikit-learn", "matplotlib", "pillow", "typing-extensions", "micropip"],
    { messageCallback: (m) => post({ type: "log", line: m }) }
  );

  status("installing-oemer");
  const requirement = assets.oemerRequirement.startsWith("http") || !assets.oemerRequirement.includes("/")
    ? assets.oemerRequirement
    : abs(assets.oemerRequirement);
  // deps=False: oemer's requirements include onnxruntime and type stubs that
  // don't exist as wasm wheels; every runtime dep is already loaded above.
  await pyodide.runPythonAsync(`
import micropip
await micropip.install(${JSON.stringify(requirement)}, deps=False)
`);

  // Install the bridge + compat modules.
  for (const f of ["omr_bridge.py", "oemer_compat.py"]) {
    const res = await fetch(abs(`py/${f}`));
    if (!res.ok) throw new Error(`failed to fetch py/${f}`);
    pyodide.FS.writeFile(`/${f}`, await res.text());
  }
  pyodide.runPython(`
import os, sys
os.environ.setdefault("MPLBACKEND", "Agg")
sys.path.insert(0, "/")
import omr_bridge
`);
  bridge = pyodide.pyimport("omr_bridge");

  status("loading-ort");
  importScripts(abs(assets.ortScript));
  ort.env.wasm.wasmPaths = abs(assets.ortWasmPaths);
  ort.env.wasm.numThreads = self.crossOriginIsolated
    ? Math.min(4, navigator.hardwareConcurrency || 1)
    : 1;

  post({ type: "ready" });
}

// ---------- model loading ---------------------------------------------------

async function fetchModel(name) {
  const url = abs(assets.models[name]);
  const cache = await caches.open("smp-models-v1").catch(() => null);

  if (cache) {
    const hit = await cache.match(url);
    if (hit) return new Uint8Array(await hit.arrayBuffer());
  }

  status("downloading-model", name);
  const res = await fetch(url);
  if (!res.ok) throw new Error(`model download failed (${res.status}): ${url}`);

  const total = Number(res.headers.get("Content-Length")) || 0;
  const chunks = [];
  let got = 0;
  const reader = res.body.getReader();
  for (;;) {
    const { done, value } = await reader.read();
    if (done) break;
    chunks.push(value);
    got += value.length;
    if (total) status("downloading-model", `${name} ${(got / 1e6).toFixed(0)}/${(total / 1e6).toFixed(0)} MB`, got / total);
  }
  const bytes = new Uint8Array(got);
  let off = 0;
  for (const c of chunks) { bytes.set(c, off); off += c.length; }

  if (cache) await cache.put(url, new Response(bytes.slice().buffer)).catch(() => {});
  return bytes;
}

async function getSession(name) {
  if (sessions[name]) return sessions[name];
  const bytes = await fetchModel(name);
  status("creating-session", name);
  let session = null;
  try {
    session = await ort.InferenceSession.create(bytes.buffer, { executionProviders: ["webgpu"] });
  } catch (_) {
    session = await ort.InferenceSession.create(bytes.buffer, { executionProviders: ["wasm"] });
  }
  sessions[name] = session;
  return session;
}

// ---------- inference -------------------------------------------------------

const BATCH = 8;

async function runModel(name, stage) {
  const session = await getSession(name);
  status(stage, "preparing patches");

  const info = bridge.prepare(name).toJs(); // [n, win, outCh]
  const [n, win, outCh] = info;

  const patchesProxy = bridge.get_patches(name);
  const batchProxy = bridge.begin_pred(name, BATCH);

  const patchSize = win * win * 3;
  const outSize = win * win * outCh;
  const totalBatches = Math.ceil(n / BATCH);

  for (let i = 0; i < n; i += BATCH) {
    const k = Math.min(BATCH, n - i);

    // Copy this batch of uint8 patches out of the Pyodide heap. Views into
    // wasm memory are invalidated whenever the heap grows, so grab a fresh
    // buffer view each iteration and release it immediately.
    const pbuf = patchesProxy.getBuffer("u8");
    const batchData = pbuf.data.slice(i * patchSize, (i + k) * patchSize);
    pbuf.release();

    const input = new ort.Tensor("uint8", batchData, [k, win, win, 3]);
    const out = await session.run({ input });
    const outData = out[session.outputNames[0]].data; // Float32Array, k*outSize

    const obuf = batchProxy.getBuffer("f32");
    obuf.data.set(outData.subarray(0, k * outSize), 0);
    obuf.release();

    bridge.accumulate(name, k);

    const done = Math.floor(i / BATCH) + 1;
    status(stage, `${done}/${totalBatches}`, done / totalBatches);
  }

  patchesProxy.destroy();
  batchProxy.destroy();
  bridge.merge(name);
}

async function recognize(imageBytes, name, deskew) {
  status("reading-image");
  // oemer titles the score after the file's basename; keep the user's name
  // but strip anything path- or filesystem-hostile.
  const stem = (name || "score").replace(/\.[^.]*$/, "").replace(/[^\w\- ]+/g, "").trim() || "score";
  const path = `/work/${stem}.png`;
  pyodide.FS.mkdirTree("/work");
  pyodide.FS.writeFile(path, new Uint8Array(imageBytes));
  bridge.load_image(path);

  await runModel("unet_big", "segmenting-staff");
  await runModel("seg_net", "segmenting-symbols");

  status("postprocess");
  const musicxml = bridge.finalize(path, deskew);
  bridge.reset();
  return musicxml;
}

// ---------- message loop ----------------------------------------------------

self.onmessage = async (e) => {
  const msg = e.data;
  try {
    if (msg.type === "init") {
      assets = msg.assets;
      baseURL = msg.origin;
      await init();
    } else if (msg.type === "omr") {
      const musicxml = await recognize(msg.imageBytes, msg.name, !!msg.deskew);
      post({ type: "result", musicxml });
    }
  } catch (err) {
    console.error(err);
    post({ type: "error", message: String(err && err.message || err), stack: err && err.stack });
  }
};
