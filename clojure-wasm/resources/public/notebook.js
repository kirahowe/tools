// Notebook prototype: Clay-flavored cells evaluated by a pluggable kernel.
//
// Kernels (pick with ?kernel=…, default wasm):
//   wasm     a real JVM in this tab (CheerpJ) — "run notebooks on user hardware"
//   server   the dev server's JVM via /api/eval-cell — stands in for the
//            per-notebook sprites hosted-clay uses today
//   scittle  SCI (Clojure interpreter compiled to JS) — instant boot,
//            experimental: no JVM, values-only fidelity
//
// All kernels speak the same contract: code string -> notebook-engine JSON
// {tag, out, ns, kind, val|html|vega|md}. See notebook-engine.cljc.
//
// Documents are {id, title, cells: [{type: "md"|"code", source}]} — saved to
// the dev server (PUT /api/notebooks/:id), drafted to localStorage, shared
// with ?nb=<id>&view=1 links (view mode renders read-only and auto-runs).

const $ = (sel) => document.querySelector(sel);

// --- datastar signal bridge (same pattern as repl.js) -----------------------

const signalState = {};
const signal = (name, value) => {
  signalState[name] = value;
  document.body.dispatchEvent(new CustomEvent(`repl${name}`, { detail: value }));
};
document.addEventListener("datastar-ready", () => {
  for (const [name, value] of Object.entries(signalState)) {
    document.body.dispatchEvent(new CustomEvent(`repl${name}`, { detail: value }));
  }
}, { once: true });

const setStatus = (v) => signal("status", v);
const setBusy = (v) => signal("busy", v);

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

// --- params -----------------------------------------------------------------

const params = new URLSearchParams(location.search);
const KERNEL = ["server", "scittle"].includes(params.get("kernel")) ? params.get("kernel") : "wasm";
const JAVA_VERSION = Number(params.get("java") || "8");
const VIEW = params.get("view") === "1";
const NB_ID = params.get("nb");
const EXAMPLE = params.get("example");

// --- tiny markdown renderer (escape first, then transform) ------------------

function escapeHtml(s) {
  return s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

export function renderMarkdown(md) {
  const blocks = escapeHtml(md).split(/```/);
  const out = [];
  for (let i = 0; i < blocks.length; i++) {
    if (i % 2 === 1) {
      out.push(`<pre class="md-code">${blocks[i].replace(/^\w*\n/, "")}</pre>`);
      continue;
    }
    const html = blocks[i]
      .replace(/^###### (.*)$/gm, "<h6>$1</h6>")
      .replace(/^##### (.*)$/gm, "<h5>$1</h5>")
      .replace(/^#### (.*)$/gm, "<h4>$1</h4>")
      .replace(/^### (.*)$/gm, "<h3>$1</h3>")
      .replace(/^## (.*)$/gm, "<h2>$1</h2>")
      .replace(/^# (.*)$/gm, "<h1>$1</h1>")
      .replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>")
      .replace(/(^|\W)\*([^*\n]+)\*/g, "$1<em>$2</em>")
      .replace(/`([^`]+)`/g, "<code>$1</code>")
      .replace(/\[([^\]]+)\]\(([^)\s]+)\)/g, '<a href="$2" target="_blank" rel="noreferrer">$1</a>')
      .replace(/^[-*] (.*)$/gm, "<li>$1</li>")
      .replace(/(<li>[\s\S]*?<\/li>)(?!\s*<li>)/g, "<ul>$1</ul>")
      .split(/\n{2,}/)
      .map((p) => (/^\s*<(h\d|ul|pre)/.test(p) ? p : p.trim() ? `<p>${p.trim()}</p>` : ""))
      .join("\n");
    out.push(html);
  }
  return out.join("\n");
}

// --- kernels ----------------------------------------------------------------

const asString = (x) => (typeof x === "string" ? x : String(x));

async function fetchText(url) {
  const r = await fetch(url);
  if (!r.ok) throw new Error(`${url} responded ${r.status}`);
  return r.text();
}

async function bootWasmKernel() {
  if (typeof cheerpjInit !== "function") {
    throw new Error(
      "CheerpJ loader unavailable — is https://cjrtnc.leaningtech.com reachable? " +
      "(?kernel=server or ?kernel=scittle work without it.)"
    );
  }
  setPhase(`Initializing CheerpJ (Java ${JAVA_VERSION})…`, { timer: true });
  await cheerpjInit({ version: JAVA_VERSION });
  const manifest = await (await fetch("/jars/manifest.json")).json();
  setPhase("Starting the JVM and loading the Clojure jars…", { timer: true });
  const lib = await cheerpjRunLibrary(manifest.jars.map((j) => `/app/jars/${j.file}`).join(":"));
  setPhase("Booting clojure.core in the browser JVM (first run is the slow one)…", { timer: true });
  const Clojure = await lib.clojure.java.api.Clojure;
  const loadString = await Clojure.var("clojure.core", "load-string");
  await loadString.invoke(await fetchText("/bootstrap.clj"));
  const evalStr = await Clojure.var("browser.repl", "eval-str");
  setPhase("Loading the notebook engine…", { timer: true });
  const engineRes = asString(await evalStr.invoke(await fetchText("/notebook-engine.cljc")));
  if (!engineRes.includes("notebook.engine ready")) {
    throw new Error(`notebook engine failed to load: ${engineRes}`);
  }
  const evalCell = await Clojure.var("notebook.engine", "eval-cell");
  return { describe: "browser JVM (CheerpJ)", evalCell: async (code) => asString(await evalCell.invoke(code)) };
}

async function bootServerKernel() {
  setPhase("Connecting to the dev server JVM…");
  const evalCell = async (code) => {
    const r = await fetch("/api/eval-cell", {
      method: "POST",
      headers: { "Content-Type": "text/plain" },
      body: code,
    });
    if (!r.ok) throw new Error(`/api/eval-cell responded ${r.status}: ${await r.text()}`);
    return r.text();
  };
  await evalCell(":warm-up");
  return { describe: "server JVM (sprite stand-in)", evalCell };
}

async function bootScittleKernel() {
  setPhase("Loading scittle (SCI compiled to JS)…", { timer: true });
  await new Promise((resolve, reject) => {
    const s = document.createElement("script");
    s.src = "https://cdn.jsdelivr.net/npm/scittle@0.6.22/dist/scittle.min.js";
    s.onload = resolve;
    s.onerror = () => reject(new Error("could not load scittle from jsdelivr — is the CDN reachable?"));
    document.head.appendChild(s);
  });
  const sci = window.scittle?.core;
  if (!sci) throw new Error("scittle loaded but window.scittle.core is missing");
  setPhase("Loading the notebook engine into SCI…");
  sci.eval_string(await fetchText("/notebook-engine.cljc"));
  const evalCell = (code) =>
    asString(sci.eval_string(`(notebook.engine/eval-cell ${JSON.stringify(code)})`));
  return { describe: "scittle/SCI (experimental — interpreter, no JVM)", evalCell: async (c) => evalCell(c) };
}

const KERNELS = { wasm: bootWasmKernel, server: bootServerKernel, scittle: bootScittleKernel };

// --- notebook document state -------------------------------------------------

let nb = { id: null, title: "untitled", cells: [] };
let evalFn = null;
let cellSeq = 0;

const newCell = (type, source = "") => ({ id: `c${++cellSeq}`, type, source });

const STARTER = {
  title: "untitled",
  cells: [
    { type: "md", source: "# New notebook\n\nCode cells run on the selected kernel. The last value of each cell is rendered by kind: tables, hiccup, vega-lite, markdown, or plain values." },
    { type: "code", source: '(for [i (range 5)]\n  {:i i :square (* i i)})' },
  ],
};

function loadFromDoc(doc, id) {
  nb = {
    id: id ?? doc.id ?? null,
    title: doc.title ?? "untitled",
    cells: (doc.cells ?? []).map((c) => newCell(c.type === "md" ? "md" : "code", String(c.source ?? ""))),
  };
}

function serialize() {
  return JSON.stringify({
    id: nb.id,
    title: nb.title,
    cells: nb.cells.map(({ type, source }) => ({ type, source })),
  }, null, 2);
}

const draftKey = () => `clojure-wasm-nb:${nb.id ?? "draft"}`;

function saveDraft() {
  try { localStorage.setItem(draftKey(), serialize()); } catch { /* private mode etc. */ }
}

async function loadInitial() {
  if (NB_ID) {
    try {
      loadFromDoc(JSON.parse(await fetchText(`/api/notebooks/${NB_ID}`)), NB_ID);
      return;
    } catch (e) {
      setPhase(`could not load notebook "${NB_ID}": ${e.message}`);
    }
  }
  if (EXAMPLE) {
    try {
      loadFromDoc(JSON.parse(await fetchText(`/examples/${EXAMPLE}.json`)));
      return;
    } catch (e) {
      setPhase(`could not load example "${EXAMPLE}": ${e.message}`);
    }
  }
  const draft = localStorage.getItem("clojure-wasm-nb:draft");
  loadFromDoc(draft ? JSON.parse(draft) : STARTER);
}

// --- rendering ----------------------------------------------------------------

function renderOutput(el, res) {
  el.innerHTML = "";
  const add = (kind, node) => {
    node.classList.add("out-block", kind);
    el.appendChild(node);
  };
  if (res.out) {
    const pre = document.createElement("pre");
    pre.textContent = res.out.replace(/\n$/, "");
    add("out", pre);
  }
  const kind = res.tag === "err" ? "err" : res.kind;
  el.dataset.kind = kind;
  if (kind === "err") {
    const pre = document.createElement("pre");
    pre.textContent = res.val;
    add("err", pre);
  } else if (kind === "table" || kind === "hiccup") {
    const div = document.createElement("div");
    div.innerHTML = res.html;
    add(kind, div);
  } else if (kind === "md") {
    const div = document.createElement("div");
    div.className = "md-render";
    div.innerHTML = renderMarkdown(res.md);
    add("md", div);
  } else if (kind === "vega") {
    const div = document.createElement("div");
    add("vega", div);
    if (typeof window.vegaEmbed === "function") {
      window.vegaEmbed(div, res.vega, { actions: false }).catch((e) => {
        div.textContent = `vega render failed: ${e.message}`;
      });
    } else {
      const pre = document.createElement("pre");
      pre.textContent = "(vega-embed not loaded — spec below)\n" + JSON.stringify(res.vega, null, 2);
      div.appendChild(pre);
    }
  } else if (kind === "nil") {
    el.dataset.kind = "nil"; // no visible output for nil, like Clay
  } else {
    const pre = document.createElement("pre");
    pre.textContent = res.val;
    add("val", pre);
  }
}

async function runCell(cell) {
  if (!evalFn || cell.type !== "code") return;
  const outEl = $(`[data-out="${cell.id}"]`);
  outEl.innerHTML = '<pre class="out-block running">…</pre>';
  try {
    const raw = await evalFn(cell.source);
    let res;
    try { res = JSON.parse(raw); }
    catch { res = { tag: "err", kind: "value", val: `unparseable result: ${raw}`, out: "" }; }
    renderOutput(outEl, res);
  } catch (e) {
    renderOutput(outEl, { tag: "err", val: `evaluation failed: ${e.message || e}`, out: "" });
  }
}

async function runAll() {
  setBusy(true);
  try {
    for (const cell of nb.cells) {
      if (cell.type === "code") await runCell(cell); // sequential: cells share the kernel's world
    }
  } finally {
    setBusy(false);
  }
}

function autosize(ta) {
  ta.style.height = "auto";
  ta.style.height = `${ta.scrollHeight + 2}px`;
}

function cellDom(cell) {
  const wrap = document.createElement("div");
  wrap.className = `cell ${cell.type}`;
  wrap.dataset.cellId = cell.id;

  if (!VIEW) {
    const side = document.createElement("div");
    side.className = "cell-side";
    const btn = (label, title, fn) => {
      const b = document.createElement("button");
      b.textContent = label;
      b.title = title;
      b.addEventListener("click", fn);
      side.appendChild(b);
      return b;
    };
    if (cell.type === "code") btn("▶", "run cell", () => runCell(cell));
    btn("↑", "move up", () => moveCell(cell, -1));
    btn("↓", "move down", () => moveCell(cell, +1));
    btn("✕", "delete cell", () => { nb.cells = nb.cells.filter((c) => c !== cell); saveDraft(); renderCells(); });
    wrap.appendChild(side);
  }

  const main = document.createElement("div");
  main.className = "cell-main";

  if (cell.type === "md") {
    const view = document.createElement("div");
    view.className = "md-render";
    view.innerHTML = renderMarkdown(cell.source);
    main.appendChild(view);
    if (!VIEW) {
      view.title = "click to edit";
      const ta = document.createElement("textarea");
      ta.className = "cell-src";
      ta.value = cell.source;
      ta.hidden = true;
      const commit = () => {
        cell.source = ta.value;
        view.innerHTML = renderMarkdown(cell.source);
        ta.hidden = true;
        view.hidden = false;
        saveDraft();
      };
      view.addEventListener("click", () => { view.hidden = true; ta.hidden = false; autosize(ta); ta.focus(); });
      ta.addEventListener("blur", commit);
      ta.addEventListener("keydown", (e) => { if (e.key === "Enter" && (e.ctrlKey || e.metaKey)) commit(); });
      ta.addEventListener("input", () => autosize(ta));
      main.appendChild(ta);
    }
  } else {
    if (VIEW) {
      const pre = document.createElement("pre");
      pre.className = "cell-src-view";
      pre.textContent = cell.source;
      main.appendChild(pre);
    } else {
      const ta = document.createElement("textarea");
      ta.className = "cell-src";
      ta.value = cell.source;
      ta.spellcheck = false;
      ta.addEventListener("input", () => { cell.source = ta.value; autosize(ta); saveDraft(); });
      ta.addEventListener("keydown", (e) => {
        if (e.key === "Enter" && (e.ctrlKey || e.metaKey)) { e.preventDefault(); runCell(cell); }
      });
      main.appendChild(ta);
      queueMicrotask(() => autosize(ta));
    }
    const out = document.createElement("div");
    out.className = "cell-out";
    out.dataset.out = cell.id;
    main.appendChild(out);
  }

  wrap.appendChild(main);
  return wrap;
}

function moveCell(cell, delta) {
  const i = nb.cells.indexOf(cell);
  const j = i + delta;
  if (j < 0 || j >= nb.cells.length) return;
  [nb.cells[i], nb.cells[j]] = [nb.cells[j], nb.cells[i]];
  saveDraft();
  renderCells();
}

function renderCells() {
  const host = $("#cells");
  host.innerHTML = "";
  for (const cell of nb.cells) host.appendChild(cellDom(cell));
}

// --- toolbar ------------------------------------------------------------------

const slugify = (s) =>
  s.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-+|-+$/g, "").slice(0, 64) || "untitled";

async function saveToServer() {
  nb.id = nb.id ?? slugify(nb.title);
  const r = await fetch(`/api/notebooks/${nb.id}`, { method: "PUT", body: serialize() });
  if (!r.ok) {
    setPhase(`save failed: ${r.status} ${await r.text()}`);
    return;
  }
  const url = new URL(location.href);
  url.searchParams.set("nb", nb.id);
  url.searchParams.delete("example");
  history.replaceState(null, "", url);
  setPhase(`saved as "${nb.id}" — share: ${shareLink()}`);
  refreshOpenList();
}

function shareLink() {
  const url = new URL(location.href);
  if (nb.id) url.searchParams.set("nb", nb.id);
  url.searchParams.set("view", "1");
  return url.href;
}

async function refreshOpenList() {
  const sel = $("#open-select");
  try {
    const ids = JSON.parse(await fetchText("/api/notebooks"));
    sel.innerHTML = '<option value="">open…</option>' +
      ids.map((id) => `<option value="nb:${id}">${id}</option>`).join("") +
      '<option value="ex:welcome">example: welcome</option>' +
      '<option value="ex:jvm-probe">example: jvm-probe</option>';
  } catch {
    sel.innerHTML = '<option value="">open…</option><option value="ex:welcome">example: welcome</option><option value="ex:jvm-probe">example: jvm-probe</option>';
  }
}

function wireToolbar() {
  const title = $("#nb-title");
  title.value = nb.title;
  title.addEventListener("input", () => { nb.title = title.value; saveDraft(); });

  $("#kernel-select").value = KERNEL;
  $("#kernel-select").addEventListener("change", (e) => {
    const url = new URL(location.href);
    url.searchParams.set("kernel", e.target.value);
    location.href = url.href; // kernels boot once per page load
  });

  $("#add-code").addEventListener("click", () => { nb.cells.push(newCell("code")); saveDraft(); renderCells(); });
  $("#add-md").addEventListener("click", () => { nb.cells.push(newCell("md", "*(click to edit)*")); saveDraft(); renderCells(); });
  $("#run-all").addEventListener("click", runAll);
  $("#save").addEventListener("click", saveToServer);
  $("#share").addEventListener("click", async () => {
    try { await navigator.clipboard.writeText(shareLink()); setPhase("share link copied to clipboard"); }
    catch { setPhase(`share link: ${shareLink()}`); }
  });
  $("#open-select").addEventListener("change", (e) => {
    const v = e.target.value;
    if (!v) return;
    const url = new URL(location.href);
    url.searchParams.delete("nb");
    url.searchParams.delete("example");
    if (v.startsWith("nb:")) url.searchParams.set("nb", v.slice(3));
    else url.searchParams.set("example", v.slice(3));
    location.href = url.href;
  });
  refreshOpenList();

  if (VIEW) {
    $("#edit-link").href = (() => {
      const url = new URL(location.href);
      url.searchParams.delete("view");
      return url.href;
    })();
    $("#view-run-all").addEventListener("click", runAll);
  }
}

// --- boot ---------------------------------------------------------------------

async function main() {
  signal("kernel", KERNEL);
  signal("view", VIEW);
  setStatus("booting");
  await loadInitial();
  document.title = `${nb.title} — notebook`;
  renderCells();
  wireToolbar();
  try {
    const kernel = await KERNELS[KERNEL]();
    evalFn = kernel.evalCell;
    setPhase(kernel.describe);
    setStatus("ready");
    if (VIEW) await runAll();
  } catch (e) {
    setStatus("error");
    setPhase(e.message || String(e));
  }
  window.__nb = { runAll, notebook: () => nb, eval: (c) => evalFn?.(c) };
}

main();
