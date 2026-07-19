// Canonical plain-text of the rendered content, plus an exact, invertible map
// between DOM selection ranges and character offsets in that text.
//
// Why this exists: annotations are anchored to character offsets in a single
// canonical string (so the export can say "chars 1204-1250" unambiguously), but
// the user selects text in a live DOM. We must convert between the two exactly,
// in both directions, or highlights and quotes would drift.
//
// The canonical text collapses runs of insignificant whitespace to a single
// space and puts a blank line between block elements, so it reads naturally
// (paragraphs are separated) while every character still traces back to a real
// text node — which keeps quotes, offsets, and highlights perfectly aligned.

const CONTEXT_LEN = 48;

// Elements that force a paragraph break in the canonical text.
const BLOCK_TAGS = new Set([
  "address", "article", "aside", "blockquote", "caption", "dd", "div", "dl",
  "dt", "fieldset", "figcaption", "figure", "footer", "form", "h1", "h2", "h3",
  "h4", "h5", "h6", "header", "hr", "li", "main", "nav", "ol", "p", "pre",
  "section", "table", "tbody", "td", "tfoot", "th", "thead", "tr", "ul",
]);

// Blocks we count for human-readable location labels ("block 12").
const CONTENT_BLOCK_TAGS = new Set([
  "p", "li", "blockquote", "pre", "h1", "h2", "h3", "h4", "h5", "h6",
  "dt", "dd", "td", "th", "figcaption", "caption",
]);

const HEADING_TAGS = new Set(["h1", "h2", "h3", "h4", "h5", "h6"]);

const ASCII_WS = " \t\n\r\f\v";

interface Piece {
  /** The source text node, or null for a synthetic separator. */
  node: Text | null;
  /** Global start offset of this piece in the canonical text. */
  base: number;
  /** The (normalized) text this piece contributes. */
  text: string;
  /**
   * For text pieces: map[i] = original offset within `node` for normalized index
   * i, for i in [0, text.length]. Empty for synthetic separators.
   */
  map: number[];
}

export interface LocationInfo {
  /** 1-based index among content blocks, or null if unknown. */
  block: number | null;
  /** Nearest preceding heading text, if any. */
  heading: string | null;
  /** Tag name of the containing block (e.g. "p", "h2"). */
  tag: string | null;
}

export class TextMap {
  readonly text: string;
  private pieces: Piece[] = [];
  private textPieces: Piece[] = [];
  private nodePiece = new Map<Text, Piece>();
  private contentBlocks: Element[] = [];
  private blockText = new Map<Element, string>();

  constructor(private root: HTMLElement) {
    this.build();
    this.text = this.pieces.map((p) => p.text).join("");
    this.textPieces = this.pieces.filter((p) => p.node);
  }

  // ---- building -----------------------------------------------------------

  private plain = "";

  private build(): void {
    this.walk(this.root, false);
    // Drop trailing synthetic separators so the text doesn't end in blank lines.
    while (this.pieces.length && this.pieces[this.pieces.length - 1].node === null) {
      const p = this.pieces.pop()!;
      this.plain = this.plain.slice(0, p.base);
    }
    // Precompute each content block's own text for heading labels & context.
    for (const blk of this.contentBlocks) {
      this.blockText.set(blk, (blk.textContent ?? "").replace(/\s+/g, " ").trim());
    }
  }

  private lastChar(): string {
    return this.plain.length ? this.plain[this.plain.length - 1] : "";
  }

  private pushPiece(node: Text | null, text: string, map: number[]): void {
    const piece: Piece = { node, base: this.plain.length, text, map };
    this.pieces.push(piece);
    if (node) this.nodePiece.set(node, piece);
    this.plain += text;
  }

  /** Remove a single trailing collapsed space before inserting a block break. */
  private trimTrailingSoftSpace(): void {
    if (!this.plain.endsWith(" ")) return;
    const p = this.pieces[this.pieces.length - 1];
    if (!p || !p.text.endsWith(" ")) return;
    p.text = p.text.slice(0, -1);
    if (p.map.length >= 2) p.map.splice(p.map.length - 2, 1); // drop that char's entry
    this.plain = this.plain.slice(0, -1);
    if (p.text.length === 0) {
      this.pieces.pop();
      if (p.node) this.nodePiece.delete(p.node);
    }
  }

  private ensureBreak(): void {
    if (this.plain.length === 0) return;
    this.trimTrailingSoftSpace();
    if (this.plain.length === 0 || this.plain.endsWith("\n\n")) return;
    const sep = this.plain.endsWith("\n") ? "\n" : "\n\n";
    this.pushPiece(null, sep, []);
  }

  private walk(node: Node, pre: boolean): void {
    for (const child of Array.from(node.childNodes)) {
      if (child.nodeType === Node.TEXT_NODE) {
        this.pushTextNode(child as Text, pre);
      } else if (child.nodeType === Node.ELEMENT_NODE) {
        const el = child as Element;
        const tag = el.tagName.toLowerCase();
        if (tag === "script" || tag === "style" || tag === "noscript") continue;
        if (tag === "br") {
          if (this.plain.length > 0 && !this.plain.endsWith("\n")) {
            this.pushPiece(null, "\n", []);
          }
          continue;
        }
        if (tag === "img") continue; // contributes no text
        const isBlock = BLOCK_TAGS.has(tag);
        if (isBlock) {
          this.ensureBreak();
          if (CONTENT_BLOCK_TAGS.has(tag)) this.contentBlocks.push(el);
        }
        this.walk(el, pre || tag === "pre");
        if (isBlock) this.ensureBreak();
      }
    }
  }

  private pushTextNode(tn: Text, pre: boolean): void {
    const value = tn.nodeValue ?? "";
    if (value.length === 0) return;
    if (pre) {
      const map: number[] = [];
      for (let i = 0; i <= value.length; i++) map.push(i);
      this.pushPiece(tn, value, map);
      return;
    }
    const suppressLeading =
      this.plain.length === 0 || this.lastChar() === " " || this.lastChar() === "\n";
    const { t, map } = normalizeWhitespace(value, suppressLeading);
    if (t.length > 0) this.pushPiece(tn, t, map);
  }

  // ---- DOM range  ->  offsets --------------------------------------------

  /** Convert a DOM selection Range to canonical {start,end}, or null if outside. */
  rangeToOffsets(range: Range): { start: number; end: number } | null {
    const start = this.pointToOffset(range.startContainer, range.startOffset, false);
    const end = this.pointToOffset(range.endContainer, range.endOffset, true);
    if (start == null || end == null) return null;
    if (end <= start) return null;
    return { start, end };
  }

  /** Map a single caret position (node, offset) to a canonical offset. */
  caretToOffset(node: Node, offset: number): number | null {
    return this.pointToOffset(node, offset, false);
  }

  private pointToOffset(node: Node, offset: number, isEnd: boolean): number | null {
    const resolved = resolveToText(node, offset, isEnd);
    if (!resolved) return null;
    const piece = this.nodePiece.get(resolved.node);
    if (!piece) {
      // Whitespace-only node that produced no piece: snap to a neighbouring piece.
      return this.snapToNeighbour(resolved.node, isEnd);
    }
    const local = originalToNormalized(piece.map, resolved.offset);
    return piece.base + local;
  }

  private snapToNeighbour(node: Text, isEnd: boolean): number | null {
    // Find the nearest text piece before/after this node in document order.
    let best: number | null = null;
    for (const p of this.textPieces) {
      if (!p.node) continue;
      const pos = node.compareDocumentPosition(p.node);
      if (isEnd) {
        if (pos & Node.DOCUMENT_POSITION_PRECEDING) best = p.base + p.text.length;
      } else if (pos & Node.DOCUMENT_POSITION_FOLLOWING) {
        return p.base;
      }
    }
    return best;
  }

  // ---- offsets  ->  DOM range --------------------------------------------

  /** Build a live DOM Range covering canonical [start,end], or null if impossible. */
  offsetsToRange(start: number, end: number): Range | null {
    const s = this.locate(start, false);
    const e = this.locate(end, true);
    if (!s || !e) return null;
    const range = document.createRange();
    try {
      range.setStart(s.node, s.offset);
      range.setEnd(e.node, e.offset);
    } catch {
      return null;
    }
    return range;
  }

  private locate(offset: number, isEnd: boolean): { node: Text; offset: number } | null {
    const pieces = this.textPieces;
    if (pieces.length === 0) return null;
    offset = Math.max(0, Math.min(offset, this.text.length));

    // Binary search for the text piece owning this offset.
    let lo = 0;
    let hi = pieces.length - 1;
    let idx = -1;
    while (lo <= hi) {
      const mid = (lo + hi) >> 1;
      const p = pieces[mid];
      if (offset < p.base) hi = mid - 1;
      else if (offset > p.base + p.text.length) lo = mid + 1;
      else {
        idx = mid;
        break;
      }
    }

    if (idx === -1) {
      // Offset lands in a gap (a synthetic block separator). Snap to the
      // adjacent text piece: previous piece's end for an end offset, next
      // piece's start otherwise.
      if (isEnd) {
        const prev = pieces[Math.max(0, lo - 1)];
        return { node: prev.node!, offset: prev.map[prev.text.length] };
      }
      const next = pieces[Math.min(lo, pieces.length - 1)];
      return { node: next.node!, offset: next.map[0] };
    }

    let p = pieces[idx];
    let local = offset - p.base;
    if (isEnd && local === 0 && idx > 0) {
      // End offset exactly at a piece start belongs to the previous piece's end.
      p = pieces[idx - 1];
      local = p.text.length;
    } else if (!isEnd && local === p.text.length && idx < pieces.length - 1) {
      // Start offset exactly at a piece end belongs to the next piece's start.
      p = pieces[idx + 1];
      local = 0;
    }
    local = Math.max(0, Math.min(local, p.text.length));
    return { node: p.node!, offset: p.map[local] };
  }

  // ---- helpers for annotations -------------------------------------------

  quote(start: number, end: number): string {
    return this.text.slice(start, end);
  }

  prefix(start: number): string {
    return this.text.slice(Math.max(0, start - CONTEXT_LEN), start);
  }

  suffix(end: number): string {
    return this.text.slice(end, end + CONTEXT_LEN);
  }

  /** Human-readable location for an offset: block number + nearest heading. */
  locationAt(offset: number): LocationInfo {
    const loc = this.locate(offset, false);
    if (!loc) return { block: null, heading: null, tag: null };
    let el: Element | null = loc.node.parentElement;
    let block: Element | null = null;
    while (el && el !== this.root) {
      if (CONTENT_BLOCK_TAGS.has(el.tagName.toLowerCase())) {
        block = el;
        break;
      }
      el = el.parentElement;
    }
    if (!block) return { block: null, heading: null, tag: null };
    const idx = this.contentBlocks.indexOf(block);
    const heading = this.findHeadingBefore(idx);
    return {
      block: idx >= 0 ? idx + 1 : null,
      heading,
      tag: block.tagName.toLowerCase(),
    };
  }

  private findHeadingBefore(blockIdx: number): string | null {
    for (let i = blockIdx; i >= 0; i--) {
      const el = this.contentBlocks[i];
      if (el && HEADING_TAGS.has(el.tagName.toLowerCase())) {
        const t = this.blockText.get(el) ?? "";
        if (t) return t.length > 80 ? t.slice(0, 77) + "…" : t;
      }
    }
    return null;
  }
}

/** Collapse ASCII whitespace runs to single spaces; build normalized->original map. */
function normalizeWhitespace(
  value: string,
  suppressLeading: boolean,
): { t: string; map: number[] } {
  let t = "";
  const map: number[] = [];
  let i = 0;
  const n = value.length;
  while (i < n) {
    const c = value[i];
    if (ASCII_WS.includes(c)) {
      const runStart = i;
      while (i < n && ASCII_WS.includes(value[i])) i++;
      if (t.length === 0 && suppressLeading) continue; // drop leading soft space
      map.push(runStart);
      t += " ";
    } else {
      map.push(i);
      t += c === " " ? " " : c; // treat nbsp as a normal, 1:1 space
      i++;
    }
  }
  map.push(n); // end sentinel: normalized index t.length -> original length
  return { t, map };
}

/** Given an original node offset, find the corresponding normalized index. */
function originalToNormalized(map: number[], nodeOffset: number): number {
  // map is non-decreasing; find the largest index whose value <= nodeOffset.
  let lo = 0;
  let hi = map.length - 1;
  if (nodeOffset <= map[0]) return 0;
  if (nodeOffset >= map[hi]) return hi;
  while (lo < hi) {
    const mid = (lo + hi + 1) >> 1;
    if (map[mid] <= nodeOffset) lo = mid;
    else hi = mid - 1;
  }
  return lo;
}

/** Resolve a (node, offset) selection endpoint to a concrete (text node, offset). */
function resolveToText(
  node: Node,
  offset: number,
  isEnd: boolean,
): { node: Text; offset: number } | null {
  if (node.nodeType === Node.TEXT_NODE) {
    return { node: node as Text, offset };
  }
  if (node.nodeType !== Node.ELEMENT_NODE) return null;
  const children = node.childNodes;
  if (!isEnd) {
    // Start: first text node at or after the boundary.
    for (let i = offset; i < children.length; i++) {
      const t = firstTextNodeIn(children[i]);
      if (t) return { node: t, offset: 0 };
    }
    // Nothing after: fall back to last text before the boundary.
    for (let i = offset - 1; i >= 0; i--) {
      const t = lastTextNodeIn(children[i]);
      if (t) return { node: t, offset: (t.nodeValue ?? "").length };
    }
  } else {
    // End: last text node at or before the boundary.
    for (let i = offset - 1; i >= 0; i--) {
      const t = lastTextNodeIn(children[i]);
      if (t) return { node: t, offset: (t.nodeValue ?? "").length };
    }
    for (let i = offset; i < children.length; i++) {
      const t = firstTextNodeIn(children[i]);
      if (t) return { node: t, offset: 0 };
    }
  }
  return null;
}

function firstTextNodeIn(node: Node): Text | null {
  if (node.nodeType === Node.TEXT_NODE) return node as Text;
  const walker = document.createTreeWalker(node, NodeFilter.SHOW_TEXT);
  return walker.nextNode() as Text | null;
}

function lastTextNodeIn(node: Node): Text | null {
  if (node.nodeType === Node.TEXT_NODE) return node as Text;
  const walker = document.createTreeWalker(node, NodeFilter.SHOW_TEXT);
  let last: Text | null = null;
  let cur: Node | null;
  while ((cur = walker.nextNode())) last = cur as Text;
  return last;
}
