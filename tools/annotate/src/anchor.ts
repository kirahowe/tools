// Re-locate a stored annotation in the current document text.
//
// Within a single session the offsets are exact, but a page can change between
// visits (edited article, re-flowed text). So instead of trusting the stored
// offsets blindly we re-anchor using the quoted text plus its surrounding
// context — the same idea as the W3C TextQuoteSelector. If the quote can't be
// found, the annotation is reported as "orphaned" rather than silently
// pointing at the wrong span.

import type { TextAnchor } from "./types.js";
import type { TextMap } from "./textmap.js";

export interface Located {
  start: number;
  end: number;
}

/** Return the current offsets for an anchor, or null if it can't be located. */
export function relocate(anchor: TextAnchor, map: TextMap): Located | null {
  const text = map.text;
  const quote = anchor.quote;
  if (!quote) return null;

  // Fast path: the stored offsets still hold the exact quote.
  if (
    anchor.start >= 0 &&
    anchor.end <= text.length &&
    text.slice(anchor.start, anchor.end) === quote
  ) {
    return { start: anchor.start, end: anchor.end };
  }

  // Otherwise, find every occurrence of the quote and score by context match
  // and closeness to the original position.
  const occurrences: number[] = [];
  let from = 0;
  for (;;) {
    const idx = text.indexOf(quote, from);
    if (idx === -1) break;
    occurrences.push(idx);
    from = idx + 1;
    if (occurrences.length > 500) break; // guard against pathological inputs
  }
  if (occurrences.length === 0) return null;
  if (occurrences.length === 1) {
    return { start: occurrences[0], end: occurrences[0] + quote.length };
  }

  let best = occurrences[0];
  let bestScore = -Infinity;
  for (const idx of occurrences) {
    const before = text.slice(Math.max(0, idx - anchor.prefix.length), idx);
    const after = text.slice(idx + quote.length, idx + quote.length + anchor.suffix.length);
    const ctx =
      commonSuffixLen(before, anchor.prefix) + commonPrefixLen(after, anchor.suffix);
    // Context match dominates; distance from the original offset breaks ties.
    const distance = Math.abs(idx - anchor.start);
    const score = ctx * 1000 - distance;
    if (score > bestScore) {
      bestScore = score;
      best = idx;
    }
  }
  return { start: best, end: best + quote.length };
}

function commonPrefixLen(a: string, b: string): number {
  const n = Math.min(a.length, b.length);
  let i = 0;
  while (i < n && a[i] === b[i]) i++;
  return i;
}

function commonSuffixLen(a: string, b: string): number {
  const n = Math.min(a.length, b.length);
  let i = 0;
  while (i < n && a[a.length - 1 - i] === b[b.length - 1 - i]) i++;
  return i;
}
