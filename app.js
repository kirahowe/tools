// Sheet Music Scanner — main thread.
//
// - Pick/take a picture -> hand bytes to the OMR worker (Pyodide + ONNX
//   Runtime Web, all client-side) -> MusicXML
// - Render with OpenSheetMusicDisplay
// - Walk OSMD's iterator to extract a flat list of {time, freq, dur} events
// - Schedule on Tone.js; advance cursor in sync
//
// No framework, no build step. OSMD and Tone are loaded as classic scripts
// (UMD globals) from either ./vendor or a CDN — see config.js.

const CFG = self.SMP_CONFIG;

// ---------- asset mode ------------------------------------------------------

async function detectAssets() {
  try {
    const res = await fetch(CFG.vendored.probe, { method: "HEAD" });
    if (res.ok) return { mode: "vendored", ...CFG.vendored };
  } catch (_) { /* fall through to CDN */ }
  return { mode: "cdn", ...CFG.cdn };
}

function loadScript(src) {
  return new Promise((resolve, reject) => {
    const s = document.createElement("script");
    s.src = src;
    s.onload = resolve;
    s.onerror = () => reject(new Error(`failed to load ${src}`));
    document.head.appendChild(s);
  });
}

// ---------- pure helpers ----------------------------------------------------

const DEFAULT_BPM = 100;

/** Convert OSMD wholeNote-fraction time into seconds at a given BPM. */
const wholeToSec = (wholeNotes, bpm) => wholeNotes * 4 * (60 / bpm);

/**
 * Walk an OSMD instance and extract a flat schedule of playback events.
 * Returns { events: [{time, freq, dur}], totalDur }, in seconds at `bpm`.
 */
function extractEvents(osmd, bpm) {
  const cursor = osmd.cursor;
  cursor.reset();
  const it = cursor.Iterator || cursor.iterator;
  const events = [];
  let totalDur = 0;

  while (it && !it.EndReached) {
    const tsWhole = it.currentTimeStamp?.RealValue ?? 0;
    const time = wholeToSec(tsWhole, bpm);

    for (const ve of it.CurrentVoiceEntries || []) {
      for (const note of ve.Notes || []) {
        const lenWhole = note.Length?.RealValue ?? 0;
        const dur = wholeToSec(lenWhole, bpm);
        const isRest =
          (typeof note.isRest === "function" && note.isRest()) ||
          note.PrintObject === false ||
          !note.Pitch;
        if (!isRest && note.Pitch && note.Pitch.frequency) {
          events.push({ time, freq: note.Pitch.frequency, dur });
        }
        totalDur = Math.max(totalDur, time + dur);
      }
    }
    it.moveToNext();
  }

  cursor.reset();
  return { events, totalDur };
}

/** Score-declared tempo if present, else fallback. */
function scoreBpm(osmd, fallback = DEFAULT_BPM) {
  const t = osmd?.Sheet?.DefaultStartTempoInBpm;
  return Number.isFinite(t) && t > 0 ? t : fallback;
}

// ---------- imperative shell ------------------------------------------------

const $ = (id) => document.getElementById(id);

const els = {
  camera: $("camera"),
  file: $("file"),
  play: $("play"),
  pause: $("pause"),
  stop: $("stop"),
  tempo: $("tempo"),
  tempoOut: $("tempo-out"),
  deskew: $("deskew"),
  status: $("status"),
  progress: $("progress"),
  progressBar: $("progress-bar"),
  preview: $("preview"),
  score: $("score"),
};

const setStatus = (html, kind = "idle") => {
  els.status.innerHTML = html;
  els.status.className = `status ${kind}`;
};

const setProgress = (pct) => {
  if (pct == null) {
    els.progress.hidden = true;
    els.progressBar.style.width = "0";
  } else {
    els.progress.hidden = false;
    els.progressBar.style.width = `${Math.round(pct * 100)}%`;
  }
};

const setControlsEnabled = (canPlay, canPause, canStop) => {
  els.play.disabled = !canPlay;
  els.pause.disabled = !canPause;
  els.stop.disabled = !canStop;
};

// State held in the shell — confined here, not threaded through the core.
const state = {
  assets: null,
  worker: null,
  workerReady: null, // promise
  osmd: null,
  events: [],
  totalDur: 0,
  scoreBpm: DEFAULT_BPM,
  part: null,
  cursorEvents: [],
  busy: false,
};

// ---------- OMR worker ------------------------------------------------------

const STAGE_LABELS = {
  "loading-pyodide": "Loading Python runtime (Pyodide)…",
  "loading-packages": "Loading scientific packages (numpy, OpenCV…)…",
  "installing-oemer": "Installing the oemer OMR engine…",
  "loading-ort": "Loading ONNX Runtime…",
  "downloading-model": "Downloading recognition models…",
  "creating-session": "Preparing neural network…",
  "reading-image": "Reading image…",
  "segmenting-staff": "Recognising stafflines &amp; symbols (pass 1/2)…",
  "segmenting-symbols": "Recognising noteheads &amp; clefs (pass 2/2)…",
  "postprocess": "Reconstructing the score…",
};

function ensureWorker() {
  if (state.worker) return state.workerReady;

  const worker = new Worker("./omr-worker.js");
  state.worker = worker;

  state.workerReady = new Promise((resolve, reject) => {
    const onMsg = (e) => {
      if (e.data.type === "ready") {
        worker.removeEventListener("message", onMsg);
        resolve(worker);
      } else if (e.data.type === "error") {
        worker.removeEventListener("message", onMsg);
        reject(new Error(e.data.message));
      }
    };
    worker.addEventListener("message", onMsg);
  });

  worker.addEventListener("message", (e) => {
    const msg = e.data;
    if (msg.type === "status") {
      const label = STAGE_LABELS[msg.stage] || msg.stage;
      const extra = msg.detail ? ` <small>${msg.detail}</small>` : "";
      setStatus(`${label}${extra}`, "working");
      setProgress(msg.pct ?? null);
    } else if (msg.type === "log") {
      console.log("[omr]", msg.line);
    }
  });

  worker.postMessage({
    type: "init",
    assets: JSON.parse(JSON.stringify(state.assets)),
    origin: new URL(".", location.href).href,
  });

  return state.workerReady;
}

function recognize(imageBytes, name, deskew) {
  return new Promise((resolve, reject) => {
    const worker = state.worker;
    const onMsg = (e) => {
      const msg = e.data;
      if (msg.type === "result") {
        worker.removeEventListener("message", onMsg);
        resolve(msg.musicxml);
      } else if (msg.type === "error") {
        worker.removeEventListener("message", onMsg);
        reject(new Error(msg.message));
      }
    };
    worker.addEventListener("message", onMsg);
    worker.postMessage({ type: "omr", imageBytes, name, deskew }, [imageBytes]);
  });
}

// ---------- score rendering -------------------------------------------------

async function renderScore(musicxml) {
  if (!state.osmd) {
    state.osmd = new opensheetmusicdisplay.OpenSheetMusicDisplay(els.score, {
      autoResize: true,
      backend: "svg",
      drawTitle: true,
    });
  }
  await state.osmd.load(musicxml);
  state.osmd.render();
  state.osmd.cursor.show();
  state.scoreBpm = scoreBpm(state.osmd);
}

// ---------- playback --------------------------------------------------------

function buildSchedule() {
  const userPct = Number(els.tempo.value) / 100;
  const bpm = state.scoreBpm * userPct;
  const { events, totalDur } = extractEvents(state.osmd, bpm);
  state.events = events;
  state.totalDur = totalDur;
  return bpm;
}

function clearSchedule() {
  if (state.part) {
    state.part.stop();
    state.part.dispose();
    state.part = null;
  }
  Tone.Transport.cancel(0);
  state.cursorEvents = [];
}

let synth = null;
function getSynth() {
  if (!synth) {
    synth = new Tone.PolySynth(Tone.Synth, {
      oscillator: { type: "triangle" },
      envelope: { attack: 0.005, decay: 0.1, sustain: 0.4, release: 0.4 },
    }).toDestination();
    synth.volume.value = -8;
  }
  return synth;
}

async function play() {
  if (!state.osmd) return;
  await Tone.start();

  clearSchedule();
  const bpm = buildSchedule();

  const s = getSynth();
  const part = new Tone.Part((time, ev) => {
    s.triggerAttackRelease(ev.freq, Math.max(0.05, ev.dur * 0.95), time);
  }, state.events.map((e) => [e.time, e]));

  // Advance the visible OSMD cursor in step with playback.
  const cursor = state.osmd.cursor;
  cursor.reset();
  const it = cursor.Iterator || cursor.iterator;
  const cursorTimes = [];
  while (it && !it.EndReached) {
    const t = wholeToSec(it.currentTimeStamp?.RealValue ?? 0, bpm);
    cursorTimes.push(t);
    it.moveToNext();
  }
  cursor.reset();
  cursorTimes.forEach((t, idx) => {
    const id = Tone.Transport.schedule((time) => {
      Tone.Draw.schedule(() => {
        if (idx === 0) cursor.reset();
        else cursor.next();
      }, time);
    }, t);
    state.cursorEvents.push(id);
  });

  // Auto-stop at the end.
  Tone.Transport.scheduleOnce(() => stop(), state.totalDur + 0.25);

  part.start(0);
  state.part = part;
  Tone.Transport.start();

  setStatus(`Playing — ${Math.round(bpm)} bpm, ${state.events.length} notes`, "ok");
  setControlsEnabled(false, true, true);
}

function pause() {
  Tone.Transport.pause();
  setStatus("Paused.", "idle");
  setControlsEnabled(true, false, true);
}

function stop() {
  Tone.Transport.stop();
  Tone.Transport.position = 0;
  clearSchedule();
  if (state.osmd) state.osmd.cursor.reset();
  setStatus("Stopped.", "idle");
  setControlsEnabled(state.events.length > 0, false, false);
}

// ---------- flow ------------------------------------------------------------

async function onFileChange(e) {
  const file = e.target.files?.[0];
  if (!file || state.busy) return;
  state.busy = true;
  e.target.value = ""; // allow re-selecting the same file later

  els.preview.src = URL.createObjectURL(file);
  els.preview.hidden = false;
  setControlsEnabled(false, false, false);
  stopIfPlaying();

  try {
    setStatus("Starting the recognition engine…", "working");
    await ensureWorker();

    const bytes = await file.arrayBuffer();
    const t0 = performance.now();
    const musicxml = await recognize(bytes, file.name, els.deskew.checked);
    state.lastMusicXML = musicxml;
    const secs = ((performance.now() - t0) / 1000).toFixed(0);

    setProgress(null);
    setStatus("Rendering score…", "working");
    await renderScore(musicxml);
    setStatus(`Done in ${secs}s — ready to play. <small>(recognition is imperfect; expect a few odd notes)</small>`, "ok");
    setControlsEnabled(true, false, false);
  } catch (err) {
    console.error(err);
    setProgress(null);
    setStatus(`Failed: ${err.message}`, "error");
    setControlsEnabled(false, false, false);
  } finally {
    state.busy = false;
  }
}

function stopIfPlaying() {
  if (typeof Tone !== "undefined" && Tone.Transport.state !== "stopped") stop();
}

// ---------- boot ------------------------------------------------------------

async function boot() {
  state.assets = await detectAssets();
  console.log(`[smp] asset mode: ${state.assets.mode}`);
  await Promise.all([loadScript(state.assets.osmdScript), loadScript(state.assets.toneScript)]);

  els.camera.addEventListener("change", onFileChange);
  els.file.addEventListener("change", onFileChange);
  els.play.addEventListener("click", play);
  els.pause.addEventListener("click", pause);
  els.stop.addEventListener("click", stop);
  els.tempo.addEventListener("input", () => {
    els.tempoOut.textContent = `${els.tempo.value}%`;
  });

  // Expose a couple of hooks for automated testing.
  window.__smp = { state, extractEvents, buildSchedule, renderScore };
}

boot();
