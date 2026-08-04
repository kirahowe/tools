// Application wiring: pick a background, then pick a text colour from a
// continuous HSL field — click/drag or use arrow keys; anything under the
// chosen WCAG threshold is dimmed rather than hidden.

import { hexToRgb, rgbToHex, hslToRgb, rgbToHsl, contrastRatio, linearize, type Rgb } from "./colour.js";

// ---- tiny DOM helper ---------------------------------------------------------

const $ = <T extends HTMLElement = HTMLElement>(id: string): T => {
  const el = document.getElementById(id);
  if (!el) throw new Error(`missing #${id}`);
  return el as T;
};

function clamp(n: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, n));
}

// ---- state --------------------------------------------------------------------

type Level = "aaLarge" | "aa" | "aaa";

interface State {
  bg: string; // #rrggbb, lowercase
  fg: string;
  level: Level;
  sat: number; // 0-100
}

/** A point in the field: either the grey gutter (saturation forced to 0) or the hue×lightness plane. */
interface Marker {
  grey: boolean;
  hue: number; // 0-360, meaningless when grey
  lightness: number; // 0-100
}

const DEFAULTS: State = { bg: "#ffffff", fg: "#1a1a1a", level: "aa", sat: 70 };

const THRESHOLD: Record<Level, number> = { aaLarge: 3, aa: 4.5, aaa: 7 };
const LEVEL_TO_HASH: Record<Level, string> = { aaLarge: "aa-large", aa: "aa", aaa: "aaa" };
const HASH_TO_LEVEL: Record<string, Level> = { "aa-large": "aaLarge", aa: "aa", aaa: "aaa" };
const LEVEL_RADIO_ID: Record<Level, string> = { aaLarge: "levelAaLarge", aa: "levelAa", aaa: "levelAaa" };

let state: State = parseHash();
let marker: Marker = deriveMarkerFromFg(state.fg);

// ---- hex helpers ----------------------------------------------------------------

function normalizeHex(input: string): string | null {
  const rgb = hexToRgb(input);
  return rgb ? rgbToHex(rgb.r, rgb.g, rgb.b) : null;
}

function bgRgbOrWhite(): Rgb {
  return hexToRgb(state.bg) ?? { r: 255, g: 255, b: 255 };
}

// ---- hash (de)serialization ------------------------------------------------------

function parseHash(): State {
  const raw = location.hash.startsWith("#") ? location.hash.slice(1) : location.hash;
  const params = new URLSearchParams(raw);

  const bg = normalizeHex(params.get("bg") ?? "") ?? DEFAULTS.bg;
  const fg = normalizeHex(params.get("fg") ?? "") ?? DEFAULTS.fg;
  const level = HASH_TO_LEVEL[params.get("level") ?? ""] ?? DEFAULTS.level;

  const sParam = params.get("s");
  const satRaw = sParam === null ? NaN : Number(sParam);
  const sat = Number.isFinite(satRaw) ? clamp(satRaw, 0, 100) : DEFAULTS.sat;

  return { bg, fg, level, sat };
}

function syncHash(): void {
  const params = new URLSearchParams();
  params.set("bg", state.bg.replace(/^#/, ""));
  params.set("fg", state.fg.replace(/^#/, ""));
  params.set("level", LEVEL_TO_HASH[state.level]);
  params.set("s", String(state.sat));
  history.replaceState(null, "", "#" + params.toString());
}

// ---- marker <-> colour ------------------------------------------------------------

/** Positions the marker from an external hex (load/hashchange/swap). One-way: does NOT
 *  write state.fg back — fg stays the exact externally-provided value; the marker is a
 *  best-effort visual/interactive proxy (it always renders at the CURRENT global
 *  saturation, which may not exactly reproduce fg's own saturation). */
function deriveMarkerFromFg(fg: string): Marker {
  const rgb = hexToRgb(fg) ?? { r: 26, g: 26, b: 26 };
  const { h, s, l } = rgbToHsl(rgb.r, rgb.g, rgb.b);
  return s < 1 ? { grey: true, hue: 0, lightness: l } : { grey: false, hue: h, lightness: l };
}

function markerRgb(m: Marker, sat: number): Rgb {
  return m.grey ? hslToRgb(0, 0, m.lightness) : hslToRgb(m.hue, sat, m.lightness);
}

function markerPasses(m: Marker): { rgb: Rgb; hex: string; ratio: number; passes: boolean } {
  const rgb = markerRgb(m, state.sat);
  const hex = rgbToHex(rgb.r, rgb.g, rgb.b);
  const ratio = contrastRatio(rgb, bgRgbOrWhite());
  return { rgb, hex, ratio, passes: ratio >= THRESHOLD[state.level] };
}

/** After a saturation change the marker's colour shifts; adopt it as fg only if it still passes. */
function recheckFgFromMarker(): void {
  const { hex, passes } = markerPasses(marker);
  if (passes) state.fg = hex;
}

// ---- the field: raster + mask + reticle -------------------------------------------
//
// The raster is sized to the canvas's true device pixels (not a fixed constant) so the
// mask boundary reads crisply on retina displays — see resizeRaster. Layout geometry
// (gutter/gap) is authored in CSS px and scaled by `renderScale` for raster work;
// pointer hit-testing (candidateFromPoint) and reticle placement (markerCssPos) stay in
// CSS-px space throughout, independent of raster resolution, so they're exact at any dpr
// or after the MAX_RASTER_W cap kicks in.

const GUTTER_CSS = 28; // CSS px: the greyscale strip
const GAP_CSS = 8; // CSS px: transparent gap between gutter and plane
const MAX_RASTER_W = 2048; // bounds worst-case pixel-fill cost on very large/high-dpr screens
const FAIL_ALPHA = 0.15; // how much of a failing pixel's own colour still shows through

const canvas = $<HTMLCanvasElement>("colourField");
// Asserted non-null here (rather than left as `CanvasRenderingContext2D | null` and
// narrowed by the guard below) because that narrowing doesn't survive into the other
// top-level functions — compositeMask, drawReticle, etc. — that close over `ctx`;
// TS control-flow analysis doesn't propagate across function boundaries. The guard
// still throws at runtime if getContext genuinely returns null.
const ctx = canvas.getContext("2d")!;
if (!ctx) throw new Error("2d canvas context unavailable");

// Raster/geometry state — (re)computed by resizeRaster() whenever the canvas's CSS size
// or the device pixel ratio changes.
let cssW = 0;
let cssH = 0;
let renderScale = 1; // raster px per CSS px; equals devicePixelRatio unless MAX_RASTER_W caps it
let rasterW = 0;
let rasterH = 0;
let gutterPx = 0;
let planeX0 = 0;
let planeW = 0;

/** RGB for every raster pixel at the current saturation — independent of bg/level. */
let baseRgb = new Uint8ClampedArray(0);
/** Precomputed WCAG relative luminance per raster pixel, alongside baseRgb — lets
 *  compositeMask skip all colour math and do plain luminance compares. */
let luma = new Float32Array(0);
/** The composited, mask-applied image — cheap to re-blit for reticle-only redraws. */
let baseImageData: ImageData | null = null;

// sRGB linearization is the expensive part of relative luminance (a pow() call per
// channel); channel values are always integers 0-255, so a 256-entry lookup table
// replaces it with an array index. Mirrors colour.ts's relLuminance exactly.
// NB: linearize() takes a 0-255 channel (it normalizes internally) — index i IS the
// channel value here; dividing first would linearize i/65025 and zero the whole table.
const LINEAR_LUT = new Float32Array(256);
for (let i = 0; i < 256; i++) LINEAR_LUT[i] = linearize(i);

function lumaFromRgb(r: number, g: number, b: number): number {
  return 0.2126 * LINEAR_LUT[r] + 0.7152 * LINEAR_LUT[g] + 0.0722 * LINEAR_LUT[b];
}

/** Re-measures the canvas and, if its device-pixel size actually changed, reallocates
 *  the raster buffers. Returns whether a reallocation happened (callers decide whether
 *  that warrants a redraw — init() handles a failed first measurement gracefully). */
function resizeRaster(): boolean {
  const rect = canvas.getBoundingClientRect();
  if (rect.width <= 0 || rect.height <= 0) return false;
  cssW = rect.width;
  cssH = rect.height;
  const dpr = window.devicePixelRatio || 1;
  renderScale = Math.min(dpr, MAX_RASTER_W / cssW);
  const newW = Math.max(1, Math.round(cssW * renderScale));
  const newH = Math.max(1, Math.round(cssH * renderScale));
  if (newW === rasterW && newH === rasterH) return false;

  rasterW = newW;
  rasterH = newH;
  gutterPx = Math.round(GUTTER_CSS * renderScale);
  planeX0 = gutterPx + Math.round(GAP_CSS * renderScale);
  planeW = rasterW - planeX0;

  canvas.width = rasterW;
  canvas.height = rasterH;
  baseRgb = new Uint8ClampedArray(rasterW * rasterH * 3);
  luma = new Float32Array(rasterW * rasterH);
  return true;
}

function rebuildBaseRgb(): void {
  const satFrac = state.sat / 100;
  for (let y = 0; y < rasterH; y++) {
    const light = 1 - y / (rasterH - 1); // 0-1, top(1)->bottom(0)

    // HSL's c/m terms depend only on (lightness, saturation) — constant for the whole
    // row, so hoisted out of the x loop. (This inlines hslToRgb's math deliberately;
    // colour.ts's hslToRgb stays the source of truth for the marker/pointer/loupe
    // paths, which only ever need a single colour at a time, not 1M+.)
    const greyVal = Math.round(light * 255);
    const greyLuma = lumaFromRgb(greyVal, greyVal, greyVal);

    const cRow = (1 - Math.abs(2 * light - 1)) * satFrac;
    const mRow = light - cRow / 2;

    for (let x = 0; x < rasterW; x++) {
      const iL = y * rasterW + x;
      const i3 = iL * 3;
      if (x < gutterPx) {
        baseRgb[i3] = greyVal; baseRgb[i3 + 1] = greyVal; baseRgb[i3 + 2] = greyVal;
        luma[iL] = greyLuma;
      } else if (x < planeX0) {
        continue; // gap: unused, mask leaves it transparent
      } else {
        const hue = ((x - planeX0) / (planeW - 1)) * 360;
        const hp = hue / 60;
        const xComp = cRow * (1 - Math.abs((hp % 2) - 1));
        let r1 = 0, g1 = 0, b1 = 0;
        if (hp < 1) { r1 = cRow; g1 = xComp; }
        else if (hp < 2) { r1 = xComp; g1 = cRow; }
        else if (hp < 3) { g1 = cRow; b1 = xComp; }
        else if (hp < 4) { g1 = xComp; b1 = cRow; }
        else if (hp < 5) { r1 = xComp; b1 = cRow; }
        else { r1 = cRow; b1 = xComp; }

        const r = Math.round((r1 + mRow) * 255);
        const g = Math.round((g1 + mRow) * 255);
        const b = Math.round((b1 + mRow) * 255);
        baseRgb[i3] = r; baseRgb[i3 + 1] = g; baseRgb[i3 + 2] = b;
        luma[iL] = lumaFromRgb(r, g, b);
      }
    }
  }
}

function cssColor(name: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
}

/** Re-runs only the pass/fail compare + dim-toward-page-background blend, reusing the
 *  cached baseRgb/luma — no HSL or luminance math, just array reads and a compare. */
function compositeMask(): void {
  const bgRgb = bgRgbOrWhite();
  const bgLuma = lumaFromRgb(bgRgb.r, bgRgb.g, bgRgb.b);
  const threshold = THRESHOLD[state.level];
  const pageBg = hexToRgb(cssColor("--bg")) ?? { r: 255, g: 255, b: 255 };
  const img = ctx.createImageData(rasterW, rasterH);

  for (let y = 0; y < rasterH; y++) {
    for (let x = 0; x < rasterW; x++) {
      if (x >= gutterPx && x < planeX0) continue; // transparent gap
      const iL = y * rasterW + x;
      const i3 = iL * 3;
      const i4 = iL * 4;
      const pxLuma = luma[iL];
      const ratio = pxLuma >= bgLuma
        ? (pxLuma + 0.05) / (bgLuma + 0.05)
        : (bgLuma + 0.05) / (pxLuma + 0.05);
      const r = baseRgb[i3], g = baseRgb[i3 + 1], b = baseRgb[i3 + 2];
      if (ratio >= threshold) {
        img.data[i4] = r; img.data[i4 + 1] = g; img.data[i4 + 2] = b; img.data[i4 + 3] = 255;
      } else {
        img.data[i4] = r * FAIL_ALPHA + pageBg.r * (1 - FAIL_ALPHA);
        img.data[i4 + 1] = g * FAIL_ALPHA + pageBg.g * (1 - FAIL_ALPHA);
        img.data[i4 + 2] = b * FAIL_ALPHA + pageBg.b * (1 - FAIL_ALPHA);
        img.data[i4 + 3] = 255;
      }
    }
  }
  baseImageData = img;
}

/** Marker position in CSS px within the canvas's own box — independent of raster
 *  resolution. Used both to place the reticle (after drawReticle's CSS-unit transform)
 *  and to drive the loupe. */
function markerCssPos(m: Marker): { x: number; y: number } {
  const y = (1 - m.lightness / 100) * cssH;
  const planeStartCss = GUTTER_CSS + GAP_CSS;
  const x = m.grey ? GUTTER_CSS / 2 : planeStartCss + (m.hue / 360) * (cssW - planeStartCss);
  return { x, y };
}

function drawReticle(): void {
  if (!baseImageData) return;

  // putImageData writes raw device pixels and ignores the current transform, so blit
  // first at identity, then switch to a CSS-unit transform for the reticle itself —
  // that keeps the ring's visual size constant across dpr instead of shrinking to
  // hairline width on retina. Uses renderScale (not raw devicePixelRatio) so it stays
  // aligned with the raster even when MAX_RASTER_W has capped the effective scale.
  ctx.setTransform(1, 0, 0, 1, 0, 0);
  ctx.putImageData(baseImageData, 0, 0);

  const { x, y } = markerCssPos(marker);
  const { hex, passes } = markerPasses(marker);
  const ringColor = passes ? cssColor("--accent") : cssColor("--danger");

  ctx.setTransform(renderScale, 0, 0, renderScale, 0, 0);
  ctx.save();
  ctx.beginPath();
  ctx.arc(x, y, 5, 0, Math.PI * 2);
  ctx.fillStyle = hex;
  ctx.fill();
  ctx.lineWidth = 2;
  ctx.strokeStyle = cssColor("--surface");
  ctx.stroke();

  ctx.beginPath();
  ctx.arc(x, y, 9, 0, Math.PI * 2);
  ctx.lineWidth = 2;
  ctx.strokeStyle = ringColor;
  ctx.stroke();
  ctx.restore();
  ctx.setTransform(1, 0, 0, 1, 0, 0);
}

// ---- render scheduling: coalesce rapid input (slider drag, pointer drag) into rAF ----

type DirtyLevel = "reticle" | "mask" | "full";
const DIRTY_RANK: Record<DirtyLevel, number> = { reticle: 1, mask: 2, full: 3 };

let dirty: DirtyLevel = "reticle";
let fgRecheckPending = false;
let rafToken: number | null = null;

function markDirty(level: DirtyLevel, recheckFg = false): void {
  if (DIRTY_RANK[level] > DIRTY_RANK[dirty]) dirty = level;
  if (recheckFg) fgRecheckPending = true;
  if (rafToken !== null) return;
  rafToken = requestAnimationFrame(() => {
    rafToken = null;
    const level2 = dirty;
    const recheck = fgRecheckPending;
    dirty = "reticle";
    fgRecheckPending = false;
    if (level2 === "full") rebuildBaseRgb();
    if (recheck) recheckFgFromMarker();
    if (level2 !== "reticle") compositeMask();
    drawReticle();
    updatePreview();
    scheduleHashSync();
  });
}

// Safari rate-limits history.replaceState; syncing on every drag frame would exceed it.
let hashTimer: number | undefined;
function scheduleHashSync(): void {
  window.clearTimeout(hashTimer);
  hashTimer = window.setTimeout(syncHash, 200);
}

// ---- resize handling ---------------------------------------------------------------

function wireResize(): void {
  const onResize = () => {
    if (resizeRaster()) markDirty("full");
  };
  new ResizeObserver(onResize).observe(canvas);
  // ResizeObserver fires on CSS-size changes; browser zoom can change devicePixelRatio
  // without necessarily changing the canvas's CSS size, so also recheck on window resize.
  window.addEventListener("resize", onResize);
}

// ---- pointer + keyboard interaction ------------------------------------------------

function candidateFromPoint(clientX: number, clientY: number): Marker | null {
  const rect = canvas.getBoundingClientRect();
  const px = clientX - rect.left;
  const py = clientY - rect.top;
  if (px < 0 || px > rect.width || py < 0 || py > rect.height) return null;
  const lightness = clamp((1 - py / rect.height) * 100, 0, 100);
  if (px < GUTTER_CSS) return { grey: true, hue: 0, lightness };
  const planeStartCss = GUTTER_CSS + GAP_CSS;
  if (px < planeStartCss) return null; // the gap
  const hue = clamp(((px - planeStartCss) / (rect.width - planeStartCss)) * 360, 0, 360);
  return { grey: false, hue, lightness };
}

function tryApplyPointerMarker(candidate: Marker): void {
  const { hex, passes } = markerPasses(candidate);
  if (!passes) return; // ignore — failing space, marker/fg unchanged
  marker = candidate;
  state.fg = hex;
  markDirty("reticle");
}

let dragging = false;

function wireField(): void {
  canvas.addEventListener("pointerdown", (e) => {
    const candidate = candidateFromPoint(e.clientX, e.clientY);
    if (!candidate) return;
    canvas.setPointerCapture(e.pointerId);
    dragging = true;
    tryApplyPointerMarker(candidate);
  });

  canvas.addEventListener("pointermove", (e) => {
    updateLoupe(e.clientX, e.clientY);

    const candidate = candidateFromPoint(e.clientX, e.clientY);
    if (dragging) {
      if (candidate) tryApplyPointerMarker(candidate);
      return;
    }
    if (!candidate) {
      canvas.style.cursor = "default";
      return;
    }
    canvas.style.cursor = markerPasses(candidate).passes ? "crosshair" : "not-allowed";
  });

  canvas.addEventListener("pointerleave", () => {
    hideLoupe();
  });

  canvas.addEventListener("pointerup", (e) => {
    if (!dragging) return;
    dragging = false;
    canvas.releasePointerCapture(e.pointerId);
    announceMarker();
  });
  canvas.addEventListener("pointercancel", () => { dragging = false; });

  canvas.addEventListener("keydown", (e) => {
    const big = e.shiftKey;
    let dHue = 0;
    let dLightness = 0;
    switch (e.key) {
      case "ArrowLeft": dHue = big ? -15 : -2; break;
      case "ArrowRight": dHue = big ? 15 : 2; break;
      case "ArrowUp": dLightness = big ? 5 : 1; break;
      case "ArrowDown": dLightness = big ? -5 : -1; break;
      default: return;
    }
    e.preventDefault();
    moveMarker(dHue, dLightness);
  });
}

function moveMarker(dHue: number, dLightness: number): void {
  let { grey, hue, lightness } = marker;
  lightness = clamp(lightness + dLightness, 0, 100);
  if (dHue !== 0) {
    if (grey) {
      if (dHue > 0) { grey = false; hue = 0; }
      // dHue < 0 while already grey: stuck at the left edge
    } else {
      const nextHue = hue + dHue;
      if (nextHue < 0) { grey = true; hue = 0; }
      else hue = Math.min(nextHue, 360);
    }
  }
  marker = { grey, hue, lightness };

  const { hex, passes } = markerPasses(marker);
  if (passes) state.fg = hex;
  markDirty("reticle");
  announceMarker();
}

function announceMarker(): void {
  const { hex, ratio, passes } = markerPasses(marker);
  $("markerStatus").textContent = passes
    ? `${hex} — ${ratio.toFixed(2)}:1`
    : `${hex} — ${ratio.toFixed(2)}:1, fails`;
}

// ---- loupe: magnified true colour under the cursor ----------------------------------

const LOUPE_SIZE = 56; // CSS px, matches contrast's styles.css .loupe-circle
const LOUPE_OFFSET = 64; // CSS px above the cursor, before the top-edge flip check

const loupe = $<HTMLDivElement>("loupe");
const loupeCircle = $<HTMLDivElement>("loupeCircle");
const loupeChip = $<HTMLDivElement>("loupeChip");
const fieldWrap = $<HTMLDivElement>("fieldWrap");

function hideLoupe(): void {
  loupe.hidden = true;
}

/** Shows the TRUE colour under the cursor, including over failing space — the field
 *  itself dims failing colours, so the loupe is how you preview what one actually looks
 *  like; the chip's "fails" text carries the judgment, never the circle's own colour. */
function updateLoupe(clientX: number, clientY: number): void {
  const candidate = candidateFromPoint(clientX, clientY);
  if (!candidate) {
    hideLoupe();
    return;
  }

  const rgb = markerRgb(candidate, state.sat);
  const hex = rgbToHex(rgb.r, rgb.g, rgb.b);
  const ratio = contrastRatio(rgb, bgRgbOrWhite());
  const passes = ratio >= THRESHOLD[state.level];

  loupeCircle.style.backgroundColor = hex;
  loupeChip.textContent = passes
    ? `${hex} · ${ratio.toFixed(2)}:1`
    : `${hex} · ${ratio.toFixed(2)}:1 · fails`;
  loupeChip.classList.toggle("fail", !passes);

  const wrapRect = fieldWrap.getBoundingClientRect();
  const half = LOUPE_SIZE / 2;
  const localX = clientX - wrapRect.left;
  const localY = clientY - wrapRect.top;

  const above = localY - LOUPE_OFFSET;
  const y = above - half < 0 ? localY + LOUPE_OFFSET : above;
  const x = clamp(localX, half, wrapRect.width - half);

  loupe.style.left = `${x}px`;
  loupe.style.top = `${y}px`;
  loupe.hidden = false;
}

// ---- preview + readout -------------------------------------------------------------

function updatePreview(): void {
  const bgRgb = bgRgbOrWhite();
  const fgRgb = hexToRgb(state.fg) ?? { r: 0, g: 0, b: 0 };
  const ratio = contrastRatio(bgRgb, fgRgb);

  const panel = $("previewPanel");
  panel.style.backgroundColor = state.bg;
  $("previewHeading").style.color = state.fg;
  $("previewBody").style.color = state.fg;
  $("previewSmall").style.color = state.fg;

  $("ratioValue").textContent = `${ratio.toFixed(2)}:1`;
  setBadge("badgeAaLarge", "AA large 3:1", ratio >= THRESHOLD.aaLarge);
  setBadge("badgeAa", "AA 4.5:1", ratio >= THRESHOLD.aa);
  setBadge("badgeAaa", "AAA 7:1", ratio >= THRESHOLD.aaa);

  $("bgHexOut").textContent = state.bg;
  $("fgHexOut").textContent = state.fg;
}

function setBadge(id: string, label: string, pass: boolean): void {
  const el = $(id);
  el.textContent = `${pass ? "✓" : "✗"} ${label}`;
  el.classList.toggle("pass", pass);
  el.classList.toggle("fail", !pass);
}

// ---- control <-> state sync ------------------------------------------------------

function syncControls(): void {
  $<HTMLInputElement>("bgColor").value = state.bg;
  $<HTMLInputElement>("bgHex").value = state.bg;
  $<HTMLInputElement>("satRange").value = String(state.sat);
  $("satOut").textContent = `${state.sat}%`;
  $<HTMLInputElement>(LEVEL_RADIO_ID[state.level]).checked = true;
}

function swap(): void {
  const { bg, fg } = state;
  state.bg = fg;
  state.fg = bg;
  marker = deriveMarkerFromFg(state.fg);
  syncControls();
  markDirty("mask");
}

// ---- copy + toast ------------------------------------------------------------------

let toastTimer: number | undefined;
function toast(msg: string): void {
  const el = $("status");
  el.textContent = msg;
  el.classList.add("show");
  window.clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => el.classList.remove("show"), 1500);
}

async function copyHex(hex: string): Promise<void> {
  try {
    await navigator.clipboard.writeText(hex);
    toast(`Copied ${hex}`);
  } catch {
    toast("Could not copy — copy it manually.");
  }
}

// ---- wire up ------------------------------------------------------------------------

function init(): void {
  syncControls();
  wireField();
  wireResize();

  if (resizeRaster()) {
    // First paint runs synchronously so there's no blank/flash frame. If the canvas
    // isn't laid out yet (0×0 rect), skip straight to the ResizeObserver's guaranteed
    // first callback instead of crashing on a zero-size ImageData.
    rebuildBaseRgb();
    compositeMask();
    drawReticle();
  }
  updatePreview();
  syncHash();

  const bgHex = $<HTMLInputElement>("bgHex");
  bgHex.addEventListener("input", () => {
    const hex = normalizeHex(bgHex.value);
    if (!hex) return;
    state.bg = hex;
    $<HTMLInputElement>("bgColor").value = hex;
    markDirty("mask");
  });

  $<HTMLInputElement>("bgColor").addEventListener("input", (e) => {
    const hex = normalizeHex((e.target as HTMLInputElement).value) ?? DEFAULTS.bg;
    state.bg = hex;
    bgHex.value = hex;
    markDirty("mask");
  });

  document.querySelectorAll<HTMLInputElement>('input[name="level"]').forEach((radio) => {
    radio.addEventListener("change", () => {
      if (radio.checked) {
        state.level = radio.value as Level;
        markDirty("mask");
      }
    });
  });

  const satRange = $<HTMLInputElement>("satRange");
  satRange.addEventListener("input", () => {
    state.sat = Number(satRange.value);
    $("satOut").textContent = `${state.sat}%`;
    markDirty("full", true);
  });

  $("swapBtn").addEventListener("click", swap);

  $("copyBg").addEventListener("click", () => void copyHex(state.bg));
  $("copyFg").addEventListener("click", () => void copyHex(state.fg));

  // The failing-region dim blends toward the page background; re-blend on theme flips.
  matchMedia("(prefers-color-scheme: dark)").addEventListener("change", () => markDirty("mask"));

  window.addEventListener("hashchange", () => {
    state = parseHash();
    marker = deriveMarkerFromFg(state.fg);
    syncControls();
    markDirty("full");
  });
}

init();
