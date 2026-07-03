// Asset configuration for the sheet-music player.
//
// The app runs in one of two modes, decided at startup by probing for
// `vendor/pyodide/pyodide.js`:
//
//   - CDN mode (default): everything is fetched from public CDNs / GitHub
//     releases / PyPI. Works with any static file host, nothing to install.
//   - Vendored mode: all assets are served from ./vendor (fully offline).
//     Populate it with `python3 tools/fetch_vendor.py`.
//
// Loaded both as a page <script> and via importScripts() in the worker, so
// it only defines a global.

self.SMP_CONFIG = {
  versions: {
    pyodide: "0.28.3",
    onnxruntimeWeb: "1.27.0",
    opensheetmusicdisplay: "2.0.0",
    tone: "15.1.22",
    oemer: "0.1.8",
  },

  cdn: {
    pyodideIndexURL: "https://cdn.jsdelivr.net/pyodide/v0.28.3/full/",
    ortScript: "https://cdn.jsdelivr.net/npm/onnxruntime-web@1.27.0/dist/ort.min.js",
    ortWasmPaths: "https://cdn.jsdelivr.net/npm/onnxruntime-web@1.27.0/dist/",
    osmdScript: "https://cdn.jsdelivr.net/npm/opensheetmusicdisplay@2.0.0/build/opensheetmusicdisplay.min.js",
    toneScript: "https://cdn.jsdelivr.net/npm/tone@15.1.22/build/Tone.js",
    // "std" uses an int8-quantized seg_net shipped with this repo (28 MB
    // smaller download, ~99.96% agreement with fp32 — see
    // tools/quantize_models.py). unet_big stays fp32 in both sets: it loses
    // stafflines when quantized. fp32 checkpoints are oemer's official
    // release assets (GitHub serves them with CORS).
    modelSets: {
      max: {
        unet_big: "https://github.com/BreezeWhite/oemer/releases/download/checkpoints/1st_model.onnx",
        seg_net: "https://github.com/BreezeWhite/oemer/releases/download/checkpoints/2nd_model.onnx",
      },
      std: {
        unet_big: "https://github.com/BreezeWhite/oemer/releases/download/checkpoints/1st_model.onnx",
        seg_net: "models/seg_net_int8.onnx",
      },
    },
    // Installed by micropip from PyPI.
    oemerRequirement: "oemer==0.1.8",
  },

  vendored: {
    probe: "vendor/pyodide/pyodide.js",
    pyodideIndexURL: "vendor/pyodide/",
    ortScript: "vendor/onnxruntime-web/dist/ort.min.js",
    ortWasmPaths: "vendor/onnxruntime-web/dist/",
    osmdScript: "vendor/opensheetmusicdisplay/build/opensheetmusicdisplay.min.js",
    toneScript: "vendor/tone/build/Tone.js",
    modelSets: {
      max: {
        unet_big: "vendor/models/1st_model.onnx",
        seg_net: "vendor/models/2nd_model.onnx",
      },
      std: {
        unet_big: "vendor/models/1st_model.onnx",
        seg_net: "models/seg_net_int8.onnx",
      },
    },
    oemerRequirement: "vendor/wheels/oemer-0.1.8-py3-none-any.whl",
  },
};
