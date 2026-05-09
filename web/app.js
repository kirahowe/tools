// Sheet Music Player — functional-ish frontend.
// - Upload image -> POST to OMR API -> MusicXML
// - Render with OpenSheetMusicDisplay
// - Walk OSMD's iterator to extract a flat list of {time, freq, dur} events
// - Schedule on Tone.js; advance cursor in sync

import { OpenSheetMusicDisplay } from "https://esm.sh/opensheetmusicdisplay@1.8.7";
import * as Tone from "https://esm.sh/tone@15.0.4";
import { API_URL } from "./config.js";

// ---------- pure helpers ----------------------------------------------------

const DEFAULT_BPM = 100;

/** Convert OSMD wholeNote-fraction time into seconds at a given BPM. */
const wholeToSec = (wholeNotes, bpm) => wholeNotes * 4 * (60 / bpm);

/**
 * Walk an OSMD instance and extract a flat schedule of playback events.
 * Returns { events: [{time, freq, dur}], totalDur }, in seconds at `bpm`.
 * Pure w.r.t. the DOM — only reads from the OSMD model.
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
  file:    $("file"),
  play:    $("play"),
  pause:   $("pause"),
  stop:    $("stop"),
  tempo:   $("tempo"),
  tempoOut:$("tempo-out"),
  status:  $("status"),
  score:   $("score"),
  fileLabel: document.querySelector(".file-input span"),
};

const setStatus = (text, kind = "idle") => {
  els.status.textContent = text;
  els.status.className = `status ${kind}`;
};

const setControlsEnabled = (canPlay, canPause, canStop) => {
  els.play.disabled  = !canPlay;
  els.pause.disabled = !canPause;
  els.stop.disabled  = !canStop;
};

// State held in the shell — confined here, not threaded through the core.
const state = {
  osmd: null,
  events: [],
  totalDur: 0,
  scoreBpm: DEFAULT_BPM,
  part: null,
  cursorEvents: [], // scheduled cursor-advance events
};

async function postImage(file) {
  const fd = new FormData();
  fd.append("file", file, file.name);
  const res = await fetch(`${API_URL}/omr`, { method: "POST", body: fd });
  if (!res.ok) {
    const msg = await res.text().catch(() => res.statusText);
    throw new Error(`OMR API ${res.status}: ${msg}`);
  }
  return res.text();
}

async function renderScore(musicxml) {
  if (!state.osmd) {
    state.osmd = new OpenSheetMusicDisplay(els.score, {
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
  buildSchedule();

  const s = getSynth();
  const part = new Tone.Part((time, ev) => {
    s.triggerAttackRelease(ev.freq, Math.max(0.05, ev.dur * 0.95), time);
  }, state.events.map((e) => [e.time, e]));

  // Advance the visible OSMD cursor in step with playback.
  const cursor = state.osmd.cursor;
  cursor.reset();
  const it = cursor.Iterator || cursor.iterator;
  const userPct = Number(els.tempo.value) / 100;
  const bpm = state.scoreBpm * userPct;
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

  setStatus(`Playing — ${Math.round(bpm)} bpm`, "ok");
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

async function onFileChange(e) {
  const file = e.target.files?.[0];
  if (!file) return;
  els.fileLabel.textContent = file.name;
  setControlsEnabled(false, false, false);
  setStatus("Recognising notes — this can take 30–90s on a cold start…", "working");
  try {
    const musicxml = await postImage(file);
    setStatus("Rendering score…", "working");
    await renderScore(musicxml);
    setStatus("Ready to play.", "ok");
    setControlsEnabled(true, false, false);
  } catch (err) {
    console.error(err);
    setStatus(`Failed: ${err.message}`, "error");
    setControlsEnabled(false, false, false);
  }
}

// Wire up.
els.file.addEventListener("change", onFileChange);
els.play.addEventListener("click", play);
els.pause.addEventListener("click", pause);
els.stop.addEventListener("click", stop);
els.tempo.addEventListener("input", () => {
  els.tempoOut.textContent = `${els.tempo.value}%`;
});
