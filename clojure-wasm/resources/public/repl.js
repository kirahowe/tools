// Boots the runtime and evaluates REPL submissions.
//
// Two modes, same eval contract (see bootstrap.clj):
//   wasm   (default) — a real JVM in this tab: CheerpJ (OpenJDK on WASM)
//                      loads the Clojure jars and evaluates locally.
//   server           — POSTs code to the dev server's JVM (/api/eval).
//                      Useful for UI work and e2e tests where the CheerpJ
//                      CDN isn't reachable.
//
// Presentation state lives in datastar signals; we push updates by
// dispatching CustomEvents that the data-on-* handlers on <body> pick up.

const $ = (sel) => document.querySelector(sel);

// Datastar attaches the data-on-* listeners asynchronously; in server mode
// the REPL can be ready before that happens. Record every signal value and
// replay them all once datastar announces itself, so no update is lost.
const signalState = {};

const signal = (name, value) => {
  signalState[name] = value;
  document.body.dispatchEvent(new CustomEvent(`repl${name}`, { detail: value }));
};

document.addEventListener(
  "datastar-ready",
  () => {
    for (const [name, value] of Object.entries(signalState)) {
      document.body.dispatchEvent(new CustomEvent(`repl${name}`, { detail: value }));
    }
  },
  { once: true }
);

const setStatus = (v) => signal("status", v);
const setNs = (v) => signal("ns", v);
const setBusy = (v) => signal("busy", v);

const params = new URLSearchParams(location.search);
const MODE = params.get("mode") === "server" ? "server" : "wasm";
const JAVA_VERSION = Number(params.get("java") || "8");

// --- boot phase display, with an elapsed-seconds ticker ------------------

let phaseBase = "";
let phaseT0 = null;
let phaseTimer = null;

function setPhase(text, { timer = false } = {}) {
  phaseBase = text;
  clearInterval(phaseTimer);
  if (timer) {
    phaseT0 = phaseT0 ?? performance.now();
    phaseTimer = setInterval(() => {
      const s = Math.round((performance.now() - phaseT0) / 1000);
      signal("phase", `${phaseBase} (${s}s elapsed)`);
    }, 1000);
  }
  signal("phase", text);
}

// --- transcript -----------------------------------------------------------

function transcript(kind, text) {
  const el = document.createElement("pre");
  el.className = `entry ${kind}`;
  el.dataset.kind = kind;
  el.textContent = text;
  $("#transcript").appendChild(el);
  el.scrollIntoView({ block: "end" });
  return el;
}

// --- runtimes -------------------------------------------------------------

// CheerpJ library mode returns java.lang.String as a JS string; keep a
// defensive coercion in case a proxy sneaks through.
const asString = (x) => (typeof x === "string" ? x : String(x));

async function bootWasm() {
  if (typeof cheerpjInit !== "function") {
    throw new Error(
      "The CheerpJ loader didn't load — is https://cjrtnc.leaningtech.com " +
      "reachable from this network? (Try ?mode=server for the dev fallback.)"
    );
  }
  setPhase(`Initializing CheerpJ (Java ${JAVA_VERSION} runtime)…`, { timer: true });
  await cheerpjInit({ version: JAVA_VERSION });

  const manifest = await (await fetch("/jars/manifest.json")).json();
  for (const jar of manifest.jars) {
    const head = await fetch(`/jars/${jar.file}`, { method: "HEAD" });
    if (!head.ok) {
      throw new Error(`Missing /jars/${jar.file} — run \`bb jars\` and reload.`);
    }
  }

  setPhase("Starting the JVM and loading the Clojure jars…", { timer: true });
  // CheerpJ mounts the web server's root at /app/ in the JVM's virtual FS.
  const classpath = manifest.jars.map((j) => `/app/jars/${j.file}`).join(":");
  const lib = await cheerpjRunLibrary(classpath);

  setPhase(
    "Booting clojure.core inside the browser JVM — the first run JIT-compiles " +
    "a lot of bytecode, so this is the slow part…",
    { timer: true }
  );
  const Clojure = await lib.clojure.java.api.Clojure;
  const loadString = await Clojure.var("clojure.core", "load-string");
  const bootstrap = await (await fetch("/bootstrap.clj")).text();
  await loadString.invoke(bootstrap);
  const evalStr = await Clojure.var("browser.repl", "eval-str");
  return async (code) => asString(await evalStr.invoke(code));
}

async function bootServer() {
  setPhase("Connecting to the dev server JVM…");
  const doEval = async (code) => {
    const r = await fetch("/api/eval", {
      method: "POST",
      headers: { "Content-Type": "text/plain" },
      body: code,
    });
    if (!r.ok) throw new Error(`/api/eval responded ${r.status}: ${await r.text()}`);
    return r.text();
  };
  await doEval(":warm-up"); // fail fast if the endpoint is off
  return doEval;
}

// --- REPL loop ------------------------------------------------------------

let evalFn = null;
let currentNs = "user";
let running = false;
const history = [];
let historyIdx = -1;

async function run() {
  const code = $("#code").value.trim();
  if (!code || running || !evalFn) return;
  running = true;
  setBusy(true);
  transcript("input", `${currentNs}=> ${code}`);
  try {
    const raw = await evalFn(code);
    let res;
    try {
      res = JSON.parse(raw);
    } catch {
      res = { tag: "err", val: `unparseable result: ${raw}`, out: "", ns: currentNs };
    }
    if (res.out) transcript("out", res.out.replace(/\n$/, ""));
    transcript(res.tag === "err" ? "err" : "val", res.val);
    if (res.ns && res.ns !== currentNs) {
      currentNs = res.ns;
      setNs(currentNs);
    }
    history.push(code);
    historyIdx = history.length;
    $("#code").value = "";
  } catch (e) {
    transcript("err", `evaluation failed: ${e.message || e}`);
  } finally {
    running = false;
    setBusy(false);
    $("#code").focus();
  }
}

function wireUi() {
  $("#run").addEventListener("click", run);
  $("#code").addEventListener("keydown", (e) => {
    if (e.key === "Enter" && (e.ctrlKey || e.metaKey)) {
      e.preventDefault();
      run();
    } else if (e.key === "ArrowUp" && $("#code").value === "" && history.length) {
      e.preventDefault();
      historyIdx = Math.max(0, historyIdx - 1);
      $("#code").value = history[historyIdx] ?? "";
    } else if (e.key === "ArrowDown" && historyIdx < history.length) {
      historyIdx = Math.min(history.length, historyIdx + 1);
      $("#code").value = history[historyIdx] ?? "";
    }
  });
  $("#examples").addEventListener("click", (e) => {
    const example = e.target?.dataset?.example;
    if (example) {
      $("#code").value = example;
      $("#code").focus();
    }
  });
}

async function main() {
  wireUi();
  signal("mode", MODE);
  setStatus("booting");
  try {
    evalFn = MODE === "server" ? await bootServer() : await bootWasm();
    // Prove it's a real JVM: ask the runtime to describe itself.
    const raw = await evalFn(
      '(print (str "Clojure " (clojure-version) " on " ' +
      '(System/getProperty "java.vm.name") " " (System/getProperty "java.version")))'
    );
    let banner = MODE === "server" ? "dev server JVM" : "browser JVM";
    try {
      const info = JSON.parse(raw);
      if (info.tag === "ret" && info.out) banner = info.out;
    } catch { /* banner stays generic */ }
    setPhase(`${banner}${MODE === "wasm" ? " — running entirely in this tab" : ""}`);
    setStatus("ready");
    transcript("banner", `;; ${banner} — ready`);
    $("#code").focus();
  } catch (e) {
    setStatus("error");
    setPhase(e.message || String(e));
    transcript("err", `boot failed: ${e.message || e}`);
  }
  // Handy for devtools poking and e2e assertions.
  window.__repl = { eval: (code) => evalFn?.(code), mode: MODE };
}

main();
