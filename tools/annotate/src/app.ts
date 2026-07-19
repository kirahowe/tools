// Application wiring: load a document, annotate spans, export the notes.

import { extractReadable, extractFromPasted } from "./extract.js";
import { TextMap } from "./textmap.js";
import { Highlighter } from "./highlight.js";
import { relocate } from "./anchor.js";
import { generate, type ExportAnnotation, type ExportFormat } from "./exporters.js";
import {
  docKeyForUrl,
  hashString,
  loadDoc,
  saveDoc,
  deleteDoc,
  loadIndex,
} from "./storage.js";
import type { Annotation, LoadedDocument } from "./types.js";

// ---- tiny DOM helpers -------------------------------------------------------

const $ = <T extends HTMLElement = HTMLElement>(id: string): T => {
  const el = document.getElementById(id);
  if (!el) throw new Error(`missing #${id}`);
  return el as T;
};

function newId(): string {
  return "a" + Math.random().toString(36).slice(2, 10) + Date.now().toString(36).slice(-4);
}

function normWs(s: string): string {
  return s.replace(/\s+/g, " ").trim();
}

// ---- application state ------------------------------------------------------

interface Captured {
  start: number;
  end: number;
  quote: string;
  prefix: string;
  suffix: string;
}

let doc: LoadedDocument | null = null;
let map: TextMap | null = null;
let highlighter: Highlighter | null = null;
let annotations: Annotation[] = [];
let activeId: string | null = null;
let editingId: string | null = null;
let pending: Captured | null = null;

// ---- loading ----------------------------------------------------------------

async function loadFromUrl(rawUrl: string): Promise<void> {
  const url = rawUrl.trim();
  if (!url) return;
  const withScheme = /^https?:\/\//i.test(url) ? url : "https://" + url;

  setBusy(true, "Fetching page…");
  try {
    const res = await fetch(`${__BASE__}/api/fetch?url=${encodeURIComponent(withScheme)}`);
    const data = (await res.json()) as { html?: string; finalUrl?: string; error?: string };
    if (!res.ok || !data.html) {
      throw new Error(data.error || `Could not load (HTTP ${res.status}).`);
    }
    const finalUrl = data.finalUrl || withScheme;
    const extracted = extractReadable(data.html, finalUrl);
    openDocument({
      key: docKeyForUrl(finalUrl),
      url: finalUrl,
      title: extracted.title,
      byline: extracted.byline,
      siteName: extracted.siteName,
      contentHtml: extracted.contentHtml,
      retrievedAt: new Date().toISOString(),
    });
    toast(`Loaded “${extracted.title}”`);
  } catch (err) {
    toast(err instanceof Error ? err.message : "Failed to load page.", true);
  } finally {
    setBusy(false);
  }
}

function loadFromPaste(text: string): void {
  if (!text.trim()) return;
  const extracted = extractFromPasted(text);
  openDocument({
    key: "paste:" + hashString(text),
    url: null,
    title: extracted.title,
    byline: extracted.byline,
    siteName: extracted.siteName,
    contentHtml: extracted.contentHtml,
    retrievedAt: new Date().toISOString(),
  });
  toast("Loaded pasted text");
}

function openStored(key: string): void {
  const stored = loadDoc(key);
  if (!stored) return;
  if (stored.contentHtml) {
    openDocument({
      key: stored.key,
      url: stored.url,
      title: stored.title,
      byline: stored.byline,
      siteName: stored.siteName,
      contentHtml: stored.contentHtml,
      retrievedAt: stored.retrievedAt,
    });
    toast(`Reopened “${stored.title}”`);
  } else if (stored.url) {
    void loadFromUrl(stored.url);
  } else {
    toast("This document has no saved content; paste it again.", true);
  }
}

function openDocument(d: LoadedDocument): void {
  doc = d;
  activeId = null;
  editingId = null;
  pending = null;

  const content = $("content");
  content.innerHTML = d.contentHtml;
  map = new TextMap(content);
  highlighter = new Highlighter(map);

  // Load & re-anchor any saved annotations for this document.
  const stored = loadDoc(d.key);
  annotations = stored?.annotations ?? [];
  reanchorAll();

  // Reveal the reader UI.
  $("emptyState").hidden = true;
  $("reader").hidden = false;
  $("docTitle").textContent = d.title;
  const metaBits = [d.byline, d.siteName, d.url].filter(Boolean) as string[];
  const meta = $("docMeta");
  meta.textContent = "";
  if (d.url) {
    const a = document.createElement("a");
    a.href = d.url;
    a.target = "_blank";
    a.rel = "noopener noreferrer";
    a.textContent = d.siteName || new URL(d.url).hostname;
    meta.appendChild(a);
    if (d.byline) meta.append(` · ${d.byline}`);
  } else {
    meta.textContent = metaBits.join(" · ") || "Pasted text";
  }

  persist();
  renderHighlights();
  renderList();
  if (!highlighter.supported) {
    toast("Inline highlighting isn't supported here, but annotations still work.");
  }
}

/** Recompute current offsets for stored annotations against the current text. */
function reanchorAll(): void {
  if (!map) return;
  for (const a of annotations) {
    const found = relocate(a.anchor, map);
    if (found) {
      a.anchor.start = found.start;
      a.anchor.end = found.end;
      a.orphaned = false;
    } else {
      a.orphaned = true;
    }
  }
}

// ---- selection -> add note --------------------------------------------------

let selectionTimer: number | undefined;

function onSelectionChange(): void {
  window.clearTimeout(selectionTimer);
  selectionTimer = window.setTimeout(evaluateSelection, 150);
}

function evaluateSelection(): void {
  if (!map) return;
  const sel = window.getSelection();
  const content = $("content");
  if (!sel || sel.isCollapsed || sel.rangeCount === 0) {
    hideAddButton();
    return;
  }
  const range = sel.getRangeAt(0);
  if (!content.contains(range.commonAncestorContainer)) {
    hideAddButton();
    return;
  }
  const offsets = map.rangeToOffsets(range);
  if (!offsets) {
    hideAddButton();
    return;
  }
  pending = {
    ...offsets,
    quote: map.quote(offsets.start, offsets.end),
    prefix: map.prefix(offsets.start),
    suffix: map.suffix(offsets.end),
  };
  showAddButton(range);
}

function showAddButton(range: Range): void {
  const btn = $("addNoteBtn");
  const rect = range.getBoundingClientRect();
  btn.hidden = false;
  const btnRect = btn.getBoundingClientRect();
  let left = rect.left + rect.width / 2 - btnRect.width / 2;
  left = Math.max(8, Math.min(left, window.innerWidth - btnRect.width - 8));
  let top = rect.top - btnRect.height - 10;
  if (top < 8) top = rect.bottom + 10; // flip below if no room above
  btn.style.left = `${left}px`;
  btn.style.top = `${top}px`;
}

function hideAddButton(): void {
  $("addNoteBtn").hidden = true;
}

// ---- note editor ------------------------------------------------------------

function openEditor(quote: string, existing: Annotation | null): void {
  editingId = existing?.id ?? null;
  $("noteQuote").textContent = normWs(quote) || "(selected text)";
  const ta = $<HTMLTextAreaElement>("noteText");
  ta.value = existing?.note ?? "";
  $("noteDelete").hidden = !existing;
  $("noteEditor").hidden = false;
  $("scrim").hidden = false;
  hideAddButton();
  ta.focus();
}

function closeEditor(): void {
  $("noteEditor").hidden = true;
  $("scrim").hidden = true;
  editingId = null;
}

function saveNote(): void {
  const text = $<HTMLTextAreaElement>("noteText").value.trim();
  if (!text) {
    toast("Write a note first, or cancel.", true);
    return;
  }
  const now = new Date().toISOString();
  if (editingId) {
    const a = annotations.find((x) => x.id === editingId);
    if (a) {
      a.note = text;
      a.updatedAt = now;
    }
  } else if (pending) {
    annotations.push({
      id: newId(),
      note: text,
      anchor: { ...pending },
      createdAt: now,
      updatedAt: now,
    });
  }
  pending = null;
  clearSelection();
  closeEditor();
  persist();
  renderHighlights();
  renderList();
  toast("Note saved");
}

function deleteActive(): void {
  if (!editingId) return;
  annotations = annotations.filter((a) => a.id !== editingId);
  if (activeId === editingId) activeId = null;
  closeEditor();
  persist();
  renderHighlights();
  renderList();
  toast("Note deleted");
}

// ---- rendering --------------------------------------------------------------

function renderHighlights(): void {
  if (!highlighter) return;
  const spans = annotations
    .filter((a) => !a.orphaned)
    .map((a) => ({ id: a.id, start: a.anchor.start, end: a.anchor.end }));
  highlighter.render(spans, activeId);
}

function renderList(): void {
  const list = $("annotationsList");
  list.textContent = "";
  const ordered = orderedAnnotations();
  $("annoCount").textContent = String(annotations.length);
  $("panelCount").textContent = String(annotations.length);
  $("emptyNotes").hidden = annotations.length > 0;

  ordered.forEach((a, i) => {
    const item = document.createElement("div");
    item.className = "note-item" + (a.id === activeId ? " active" : "") + (a.orphaned ? " orphan" : "");
    item.dataset.id = a.id;

    const num = document.createElement("div");
    num.className = "note-num";
    num.textContent = String(i + 1);
    item.appendChild(num);

    const bodyEl = document.createElement("div");
    bodyEl.className = "note-body";

    const q = document.createElement("div");
    q.className = "note-quote";
    q.textContent = normWs(a.anchor.quote).slice(0, 200) || "(text)";
    if (a.orphaned) q.title = "The quoted text was not found in the current document.";
    bodyEl.appendChild(q);

    const n = document.createElement("div");
    n.className = "note-text";
    n.textContent = a.note;
    bodyEl.appendChild(n);

    const actions = document.createElement("div");
    actions.className = "note-actions";
    const edit = document.createElement("button");
    edit.textContent = "Edit";
    edit.className = "link-btn";
    edit.addEventListener("click", (e) => {
      e.stopPropagation();
      openEditor(a.anchor.quote, a);
    });
    const del = document.createElement("button");
    del.textContent = "Delete";
    del.className = "link-btn danger";
    del.addEventListener("click", (e) => {
      e.stopPropagation();
      editingId = a.id;
      deleteActive();
    });
    actions.append(edit, del);
    bodyEl.appendChild(actions);

    item.appendChild(bodyEl);
    item.addEventListener("click", () => setActive(a.id, true));
    list.appendChild(item);
  });
}

/** Anchored annotations by document position, then orphaned ones. */
function orderedAnnotations(): Annotation[] {
  const anchored = annotations.filter((a) => !a.orphaned).sort((x, y) => x.anchor.start - y.anchor.start);
  const orphaned = annotations.filter((a) => a.orphaned);
  return [...anchored, ...orphaned];
}

function setActive(id: string | null, scroll = false): void {
  activeId = id;
  renderHighlights();
  // Update list selection without a full rebuild.
  document.querySelectorAll(".note-item").forEach((el) => {
    el.classList.toggle("active", (el as HTMLElement).dataset.id === id);
  });
  if (scroll && id && highlighter) highlighter.scrollTo(id);
}

// ---- click a highlight to select it -----------------------------------------

function onContentClick(e: MouseEvent): void {
  const target = e.target as HTMLElement;
  if (target.closest("a")) {
    e.preventDefault(); // don't navigate away while annotating
  }
  const sel = window.getSelection();
  if (sel && !sel.isCollapsed) return; // a selection is in progress
  if (!map) return;
  const offset = offsetFromPoint(e.clientX, e.clientY);
  if (offset == null) return;
  const hit = annotations.find(
    (a) => !a.orphaned && offset >= a.anchor.start && offset < a.anchor.end,
  );
  if (hit) {
    setActive(hit.id);
    openPanelIfMobile();
    const el = document.querySelector(`.note-item[data-id="${hit.id}"]`);
    el?.scrollIntoView({ block: "nearest", behavior: "smooth" });
  }
}

function offsetFromPoint(x: number, y: number): number | null {
  if (!map) return null;
  const anyDoc = document as unknown as {
    caretRangeFromPoint?: (x: number, y: number) => Range | null;
    caretPositionFromPoint?: (x: number, y: number) => { offsetNode: Node; offset: number } | null;
  };
  if (anyDoc.caretRangeFromPoint) {
    const r = anyDoc.caretRangeFromPoint(x, y);
    if (r) return map.caretToOffset(r.startContainer, r.startOffset);
  } else if (anyDoc.caretPositionFromPoint) {
    const pos = anyDoc.caretPositionFromPoint(x, y);
    if (pos) return map.caretToOffset(pos.offsetNode, pos.offset);
  }
  return null;
}

// ---- export -----------------------------------------------------------------

let currentFormat: ExportFormat = "inline";

const FORMAT_HINTS: Record<ExportFormat, string> = {
  inline:
    "Full document with your notes embedded where they apply — good for handing to an assistant to revise.",
  markdown:
    "A readable list of notes, each with the exact quote, its context, and location.",
  json:
    "Structured JSON (W3C-style selectors) with the full text and offsets — for programmatic use.",
};

function buildExportAnnotations(): ExportAnnotation[] {
  if (!map) return [];
  return orderedAnnotations().map((a, i) => ({
    index: i + 1,
    note: a.note,
    start: a.anchor.start,
    end: a.anchor.end,
    quote: a.anchor.quote,
    prefix: a.anchor.prefix,
    suffix: a.anchor.suffix,
    location: a.orphaned ? { block: null, heading: null, tag: null } : map!.locationAt(a.anchor.start),
    orphaned: !!a.orphaned,
  }));
}

function refreshExportPreview(): void {
  if (!doc || !map) return;
  const text = generate(currentFormat, {
    doc,
    text: map.text,
    annotations: buildExportAnnotations(),
  });
  $<HTMLTextAreaElement>("exportPreview").value = text;
  $("tabHint").textContent = FORMAT_HINTS[currentFormat];
}

function openExport(): void {
  if (!doc || annotations.length === 0) {
    toast("Add at least one note first.", true);
    return;
  }
  $("exportModal").hidden = false;
  $("scrim").hidden = false;
  refreshExportPreview();
}

function closeExport(): void {
  $("exportModal").hidden = true;
  $("scrim").hidden = true;
}

async function copyExport(): Promise<void> {
  const text = $<HTMLTextAreaElement>("exportPreview").value;
  try {
    await navigator.clipboard.writeText(text);
    toast("Copied to clipboard");
  } catch {
    $<HTMLTextAreaElement>("exportPreview").select();
    toast("Select all + copy manually", true);
  }
}

function downloadExport(): void {
  const text = $<HTMLTextAreaElement>("exportPreview").value;
  const ext = currentFormat === "json" ? "json" : "md";
  const name = (doc?.title || "annotations").replace(/[^\w.-]+/g, "-").slice(0, 60);
  const blob = new Blob([text], { type: currentFormat === "json" ? "application/json" : "text/markdown" });
  const a = document.createElement("a");
  a.href = URL.createObjectURL(blob);
  a.download = `${name}.${ext}`;
  a.click();
  URL.revokeObjectURL(a.href);
}

// ---- recent documents -------------------------------------------------------

function openRecent(): void {
  const list = $("recentList");
  list.textContent = "";
  const entries = loadIndex();
  if (entries.length === 0) {
    list.innerHTML = "<p class='muted'>No saved documents yet.</p>";
  }
  for (const e of entries) {
    const row = document.createElement("div");
    row.className = "recent-item";
    const info = document.createElement("button");
    info.className = "recent-open";
    info.innerHTML = `<strong>${escapeHtml(e.title)}</strong><span class="muted">${
      e.url ? escapeHtml(e.url) : "pasted text"
    } · ${e.count} note${e.count === 1 ? "" : "s"}</span>`;
    info.addEventListener("click", () => {
      closeRecent();
      openStored(e.key);
    });
    const del = document.createElement("button");
    del.className = "link-btn danger";
    del.textContent = "Remove";
    del.addEventListener("click", () => {
      deleteDoc(e.key);
      openRecent();
    });
    row.append(info, del);
    list.appendChild(row);
  }
  $("recentModal").hidden = false;
  $("scrim").hidden = false;
}

function closeRecent(): void {
  $("recentModal").hidden = true;
  $("scrim").hidden = true;
}

// ---- misc UI ----------------------------------------------------------------

function persist(): void {
  if (doc) saveDoc(doc, annotations);
}

function clearSelection(): void {
  window.getSelection()?.removeAllRanges();
  hideAddButton();
}

function setBusy(busy: boolean, msg = ""): void {
  $("loadBtn").toggleAttribute("disabled", busy);
  if (busy) toast(msg);
}

let toastTimer: number | undefined;
function toast(msg: string, isError = false): void {
  const el = $("status");
  el.textContent = msg;
  el.classList.toggle("error", isError);
  el.classList.add("show");
  window.clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => el.classList.remove("show"), 3200);
}

function escapeHtml(s: string): string {
  return s.replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]!));
}

function openPanelIfMobile(): void {
  if (window.matchMedia("(max-width: 860px)").matches) $("app").classList.add("panel-open");
}

function togglePanel(): void {
  $("app").classList.toggle("panel-open");
}

// ---- wire up ----------------------------------------------------------------

function init(): void {
  $<HTMLFormElement>("urlForm").addEventListener("submit", (e) => {
    e.preventDefault();
    void loadFromUrl($<HTMLInputElement>("urlInput").value);
  });

  $("pasteBtn").addEventListener("click", () => {
    $("pasteModal").hidden = false;
    $("scrim").hidden = false;
    $<HTMLTextAreaElement>("pasteText").focus();
  });
  $("pasteLoad").addEventListener("click", () => {
    loadFromPaste($<HTMLTextAreaElement>("pasteText").value);
    $("pasteModal").hidden = true;
    $("scrim").hidden = true;
  });
  $("pasteCancel").addEventListener("click", () => {
    $("pasteModal").hidden = true;
    $("scrim").hidden = true;
  });

  $("addNoteBtn").addEventListener("mousedown", (e) => e.preventDefault()); // keep selection
  $("addNoteBtn").addEventListener("click", () => {
    if (pending) openEditor(pending.quote, null);
  });

  $("noteSave").addEventListener("click", saveNote);
  $("noteCancel").addEventListener("click", () => {
    closeEditor();
    clearSelection();
  });
  $("noteDelete").addEventListener("click", deleteActive);

  $("exportBtn").addEventListener("click", openExport);
  $("exportClose").addEventListener("click", closeExport);
  $("exportCopy").addEventListener("click", () => void copyExport());
  $("exportDownload").addEventListener("click", downloadExport);
  document.querySelectorAll<HTMLButtonElement>(".tab").forEach((tab) => {
    tab.addEventListener("click", () => {
      currentFormat = tab.dataset.format as ExportFormat;
      document.querySelectorAll(".tab").forEach((t) => t.classList.toggle("active", t === tab));
      refreshExportPreview();
    });
  });

  $("recentBtn").addEventListener("click", openRecent);
  $("recentClose").addEventListener("click", closeRecent);

  $("panelToggle").addEventListener("click", togglePanel);
  $("panelClose").addEventListener("click", () => $("app").classList.remove("panel-open"));

  $("scrim").addEventListener("click", () => {
    closeEditor();
    closeExport();
    closeRecent();
    $("pasteModal").hidden = true;
    $("scrim").hidden = true;
  });

  document.addEventListener("selectionchange", onSelectionChange);
  $("content").addEventListener("click", onContentClick);
  window.addEventListener("scroll", hideAddButton, { passive: true });
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") {
      closeEditor();
      closeExport();
      closeRecent();
      $("pasteModal").hidden = true;
      $("scrim").hidden = true;
    }
    // Cmd/Ctrl+Enter saves the note editor.
    if ((e.metaKey || e.ctrlKey) && e.key === "Enter" && !$("noteEditor").hidden) {
      saveNote();
    }
  });

  registerServiceWorker();

  // Deep-link support: ?url=… loads immediately (nice for a bookmarklet).
  const qp = new URLSearchParams(location.search).get("url");
  if (qp) {
    $<HTMLInputElement>("urlInput").value = qp;
    void loadFromUrl(qp);
  }
}

function registerServiceWorker(): void {
  if ("serviceWorker" in navigator) {
    window.addEventListener("load", () => {
      navigator.serviceWorker
        .register(`${__BASE__}/sw.js`, { scope: `${__BASE__}/` })
        .catch(() => {
          /* offline support is optional */
        });
    });
  }
}

init();
