// Turn annotations into text an LLM can act on.
//
// The whole point of the tool: the export must make it unambiguous *which span*
// of the source each note refers to. Every format therefore carries the exact
// quoted text; two of the three also carry the full document so an assistant can
// rewrite it in place.

import type { LoadedDocument } from "./types.js";
import type { LocationInfo } from "./textmap.js";

export interface ExportAnnotation {
  index: number; // 1-based, document order
  note: string;
  start: number;
  end: number;
  quote: string;
  prefix: string;
  suffix: string;
  location: LocationInfo;
  orphaned: boolean;
}

export interface ExportInput {
  doc: LoadedDocument;
  text: string; // canonical document text
  annotations: ExportAnnotation[];
}

export type ExportFormat = "inline" | "markdown" | "json";

export function generate(format: ExportFormat, input: ExportInput): string {
  switch (format) {
    case "inline":
      return inlineAnnotatedDocument(input);
    case "markdown":
      return markdownNotes(input);
    case "json":
      return jsonExport(input);
  }
}

// --- Format 1: the full document with notes embedded where they apply --------

function inlineAnnotatedDocument(input: ExportInput): string {
  const { doc, text } = input;
  const anchored = input.annotations.filter((a) => !a.orphaned);
  const orphaned = input.annotations.filter((a) => a.orphaned);

  // Build the annotated body by splicing markers at each span boundary.
  interface Evt { pos: number; kind: "open" | "close"; i: number }
  const events: Evt[] = [];
  for (const a of anchored) {
    events.push({ pos: a.start, kind: "open", i: a.index });
    events.push({ pos: a.end, kind: "close", i: a.index });
  }
  // At the same position, close before open so adjacent spans don't merge.
  events.sort((x, y) => x.pos - y.pos || (x.kind === "close" ? -1 : 1));

  let body = "";
  let cursor = 0;
  for (const e of events) {
    body += text.slice(cursor, e.pos);
    body += e.kind === "open" ? "⟦" : `⟧[^${e.i}]`;
    cursor = e.pos;
  }
  body += text.slice(cursor);

  const header = [
    `# Annotated document: ${doc.title}`,
    doc.url ? `Source: ${doc.url}` : null,
    `Exported: ${nowLabel()} · ${anchored.length} note${anchored.length === 1 ? "" : "s"}`,
    "",
    "The document below is reproduced in full. Each place I annotated is wrapped",
    "in `⟦ ⟧` and marked with a footnote reference like `[^1]`. The matching note",
    "is listed under NOTES at the end. Apply each note to the span it marks and",
    "return the full, revised document.",
    "",
    "---",
    "",
  ]
    .filter((l) => l !== null)
    .join("\n");

  const notes = ["", "---", "", "## NOTES", ""];
  for (const a of anchored) {
    notes.push(`[^${a.index}]: ${oneLine(a.note)}`);
  }
  if (orphaned.length) {
    notes.push("");
    notes.push("## NOTES THAT COULD NOT BE PLACED");
    notes.push(
      "(The quoted text was not found in the current document, so these are not marked inline.)",
    );
    notes.push("");
    for (const a of orphaned) {
      notes.push(`- On "${oneLine(a.quote)}": ${oneLine(a.note)}`);
    }
  }

  return header + body + "\n" + notes.join("\n") + "\n";
}

// --- Format 2: a readable list of notes, each with its quote & location ------

function markdownNotes(input: ExportInput): string {
  const { doc, annotations } = input;
  const out: string[] = [];
  out.push(`# Annotations: ${doc.title}`);
  if (doc.url) out.push(`Source: ${doc.url}`);
  out.push(
    `Exported: ${nowLabel()} · ${annotations.length} note${annotations.length === 1 ? "" : "s"}`,
  );
  out.push("");
  out.push(
    "Each note below quotes the exact text it refers to. Use the quote (and the",
    "surrounding context) to locate the span in the source and apply the note.",
  );
  out.push("");
  out.push("---");

  for (const a of annotations) {
    out.push("");
    out.push(`## Note ${a.index} — ${locationLabel(a)}`);
    if (a.orphaned) {
      out.push("");
      out.push("> ⚠️ The quoted text was not found in the current document.");
    }
    out.push("");
    out.push("**Quoted text:**");
    out.push("");
    out.push(blockquote(a.quote));
    out.push("");
    if (a.prefix || a.suffix) {
      out.push(`**In context:** …${oneLine(a.prefix)}⟦${oneLine(a.quote)}⟧${oneLine(a.suffix)}…`);
      out.push("");
    }
    out.push(`**Note:** ${a.note.trim()}`);
    out.push("");
    out.push("---");
  }

  return out.join("\n") + "\n";
}

// --- Format 3: structured JSON (W3C-inspired), full text + selectors ---------

function jsonExport(input: ExportInput): string {
  const { doc, text, annotations } = input;
  const payload = {
    "@context": "https://www.w3.org/ns/anno.jsonld",
    source: {
      url: doc.url,
      title: doc.title,
      byline: doc.byline,
      siteName: doc.siteName,
      retrievedAt: doc.retrievedAt,
      exportedAt: new Date().toISOString(),
    },
    documentText: text,
    annotations: annotations.map((a) => ({
      id: a.index,
      note: a.note,
      orphaned: a.orphaned,
      location: {
        block: a.location.block,
        heading: a.location.heading,
        blockTag: a.location.tag,
      },
      target: {
        // W3C TextQuoteSelector
        selector: [
          {
            type: "TextQuoteSelector",
            exact: a.quote,
            prefix: a.prefix,
            suffix: a.suffix,
          },
          // W3C TextPositionSelector (offsets into documentText)
          {
            type: "TextPositionSelector",
            start: a.start,
            end: a.end,
          },
        ],
      },
    })),
  };
  return JSON.stringify(payload, null, 2);
}

// --- helpers -----------------------------------------------------------------

function locationLabel(a: ExportAnnotation): string {
  const parts: string[] = [];
  if (a.location.heading) parts.push(`under “${a.location.heading}”`);
  if (a.location.block != null) parts.push(`block ${a.location.block}`);
  const where = parts.length ? parts.join(", ") : "location unknown";
  return `${where} (chars ${a.start}–${a.end})`;
}

function blockquote(s: string): string {
  return s
    .trim()
    .split("\n")
    .map((line) => `> ${line}`)
    .join("\n");
}

function oneLine(s: string): string {
  return s.replace(/\s+/g, " ").trim();
}

function nowLabel(): string {
  const d = new Date();
  return d.toISOString().slice(0, 16).replace("T", " ") + " UTC";
}
