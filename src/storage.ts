// Per-document persistence in localStorage, so notes survive reloads.

import type { Annotation, LoadedDocument, StoredDoc } from "./types.js";

const PREFIX = "annotate-for-llm:doc:";
const INDEX_KEY = "annotate-for-llm:index";

export interface DocIndexEntry {
  key: string;
  url: string | null;
  title: string;
  count: number;
  updatedAt: string;
}

/** A stable key for a document: normalized URL, or a content hash for pasted text. */
export function docKeyForUrl(url: string): string {
  try {
    const u = new URL(url);
    u.hash = "";
    // Drop a trailing slash so "/a" and "/a/" share notes.
    let s = u.toString();
    if (u.pathname !== "/" && s.endsWith("/")) s = s.slice(0, -1);
    return s;
  } catch {
    return url.trim();
  }
}

/** Small, stable non-cryptographic hash (for pasted-text document keys). */
export function hashString(input: string): string {
  let h = 2166136261;
  for (let i = 0; i < input.length; i++) {
    h ^= input.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  return (h >>> 0).toString(36);
}

export function loadDoc(key: string): StoredDoc | null {
  const raw = localStorage.getItem(PREFIX + key);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as StoredDoc;
  } catch {
    return null;
  }
}

export function saveDoc(doc: LoadedDocument, annotations: Annotation[]): void {
  const updatedAt = new Date().toISOString();
  const stored: StoredDoc = {
    key: doc.key,
    url: doc.url,
    title: doc.title,
    byline: doc.byline,
    siteName: doc.siteName,
    contentHtml: doc.contentHtml,
    retrievedAt: doc.retrievedAt,
    annotations,
    updatedAt,
  };
  try {
    localStorage.setItem(PREFIX + doc.key, JSON.stringify(stored));
  } catch {
    // Likely a quota error — retry without the cached HTML, which is the big part.
    try {
      localStorage.setItem(PREFIX + doc.key, JSON.stringify({ ...stored, contentHtml: null }));
    } catch {
      /* give up silently; annotations stay in memory for this session */
    }
  }
  updateIndex({
    key: doc.key,
    url: doc.url,
    title: doc.title,
    count: annotations.length,
    updatedAt,
  });
}

export function deleteDoc(key: string): void {
  localStorage.removeItem(PREFIX + key);
  const idx = loadIndex().filter((e) => e.key !== key);
  localStorage.setItem(INDEX_KEY, JSON.stringify(idx));
}

export function loadIndex(): DocIndexEntry[] {
  const raw = localStorage.getItem(INDEX_KEY);
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw) as DocIndexEntry[];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function updateIndex(entry: DocIndexEntry): void {
  const idx = loadIndex().filter((e) => e.key !== entry.key);
  idx.unshift(entry);
  localStorage.setItem(INDEX_KEY, JSON.stringify(idx.slice(0, 50)));
}
