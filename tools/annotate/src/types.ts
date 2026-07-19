// Types shared across the frontend modules.

/**
 * How an annotation is anchored to the document text. Modeled on the W3C Web
 * Annotation selectors so the anchor survives re-loads and is unambiguous to a
 * human or an LLM reading the export.
 */
export interface TextAnchor {
  /** The exact text the note refers to (verbatim slice of the document text). */
  quote: string;
  /** Up to ~40 chars of text immediately before the quote (for disambiguation). */
  prefix: string;
  /** Up to ~40 chars of text immediately after the quote. */
  suffix: string;
  /** Character offset of the quote's start within the canonical document text. */
  start: number;
  /** Character offset of the quote's end (exclusive). */
  end: number;
}

export interface Annotation {
  id: string;
  /** The user's note / comment. */
  note: string;
  anchor: TextAnchor;
  createdAt: string;
  updatedAt: string;
  /**
   * Set at render time (not persisted): the anchor could not be re-located in
   * the current document text. Such annotations are shown as "orphaned".
   */
  orphaned?: boolean;
}

/** The extracted, readable document plus provenance. */
export interface LoadedDocument {
  /** Stable key used for persistence (normalized URL, or a hash for pasted text). */
  key: string;
  url: string | null;
  title: string;
  byline: string | null;
  /** Sanitized readable HTML rendered into the reader pane. */
  contentHtml: string;
  siteName: string | null;
  retrievedAt: string;
}

/** What we persist per document in localStorage. */
export interface StoredDoc {
  key: string;
  url: string | null;
  title: string;
  byline: string | null;
  siteName: string | null;
  /** Cached readable HTML so the doc can be reopened instantly / offline. */
  contentHtml: string | null;
  retrievedAt: string;
  annotations: Annotation[];
  updatedAt: string;
}
