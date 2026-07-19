// Visual highlighting of annotated spans.
//
// Uses the CSS Custom Highlight API, which paints ranges WITHOUT inserting any
// elements into the DOM. That is important here: if we wrapped spans in <mark>
// we'd change the text nodes the offsets are computed against, and every anchor
// would drift. With Custom Highlights the DOM the TextMap measured stays exactly
// as-is.
//
// Where the API isn't available the app still works fully (sidebar, exports,
// scroll-to); only the inline tint is skipped.

import type { TextMap } from "./textmap.js";

export interface Span {
  id: string;
  start: number;
  end: number;
}

const ALL = "annotation";
const ACTIVE = "annotation-active";

export class Highlighter {
  readonly supported: boolean;
  private ranges = new Map<string, Range>();

  constructor(private map: TextMap) {
    this.supported =
      typeof CSS !== "undefined" &&
      "highlights" in CSS &&
      typeof Highlight !== "undefined";
  }

  /** Rebuild the highlight ranges for the given spans. Returns ids that failed. */
  render(spans: Span[], activeId: string | null): string[] {
    this.ranges.clear();
    const failed: string[] = [];
    for (const s of spans) {
      const range = this.map.offsetsToRange(s.start, s.end);
      if (range) this.ranges.set(s.id, range);
      else failed.push(s.id);
    }
    if (!this.supported) return failed;

    const all = new Highlight();
    const active = new Highlight();
    for (const [id, range] of this.ranges) {
      if (id === activeId) active.add(range);
      else all.add(range);
    }
    CSS.highlights.set(ALL, all);
    CSS.highlights.set(ACTIVE, active);
    return failed;
  }

  /** Scroll the annotation into view and briefly flash it. */
  scrollTo(id: string): void {
    const range = this.ranges.get(id);
    if (!range) return;
    const rect = range.getBoundingClientRect();
    if (rect.height === 0 && rect.width === 0) return;
    const target = rect.top + window.scrollY - window.innerHeight / 3;
    window.scrollTo({ top: Math.max(0, target), behavior: "smooth" });
  }

  clear(): void {
    this.ranges.clear();
    if (!this.supported) return;
    CSS.highlights.delete(ALL);
    CSS.highlights.delete(ACTIVE);
  }
}
