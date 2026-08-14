// Pure feed parsing: XML text in, a data model out. Atom (RFC 4287) and RSS
// 2.0 are two quite different documents that describe the same thing, so both
// are normalized to the one `Feed` shape below and the renderer never learns
// which it got. No page DOM is read or mutated here — the only DOM contact is
// a read-only DOMParser walk of the parsed XML document, which is what lets
// this module be exercised without the app's HTML.

/** Which document shape a feed was read from. Kept on the model so the page
 *  can tell you what it detected; nothing about rendering depends on it. */
export type FeedFormat = "atom" | "rss";

export interface Feed {
  format: FeedFormat;
  title: string;
  subtitle: string;
  link: string;
  author: string;
  updated: string;
  entries: Entry[];
}

export interface Entry {
  title: string;
  link: string;
  author: string;
  /** Raw timestamp, unformatted: ISO 8601 from Atom, RFC 822 from RSS. */
  date: string;
  content: EntryContent | null;
}

/** An entry body, still unsanitized: `html` is untrusted markup the caller must sanitize. */
export type EntryContent =
  | { kind: "html"; html: string }
  | { kind: "text"; text: string };

export type ParseResult =
  | { ok: true; feed: Feed }
  | { ok: false; error: string };

/** Namespaces looked up by URI rather than by prefix: a feed may bind these to
 *  any prefix it likes, and `content:encoded` in particular is a name generic
 *  enough that some other namespace could reasonably use it too. */
const NS = {
  content: "http://purl.org/rss/1.0/modules/content/",
  dc: "http://purl.org/dc/elements/1.1/",
} as const;

function children(parent: Element, name: string): Element[] {
  return Array.from(parent.children).filter((child) => child.localName === name);
}

function child(parent: Element, name: string): Element | null {
  return children(parent, name)[0] ?? null;
}

function childIn(parent: Element, namespace: string, name: string): Element | null {
  return (
    Array.from(parent.children).find(
      (element) => element.localName === name && element.namespaceURI === namespace,
    ) ?? null
  );
}

/** Text that carries markup, as opposed to text that merely contains a "<".
 *  A tag needs a name, a closing slash, or a `<!` right after the bracket, so
 *  prose like "5 < 6" and "use <- to assign" don't read as markup. */
const MARKUP = /<[a-z!/]/i;

/**
 * Resolves markup down to the text it wraps. Parsing with a second
 * `DOMParser` and reading back only `.textContent` — never the parsed nodes —
 * keeps this a flatten rather than a sanitize: `parseFromString` doesn't
 * execute scripts or load subresources, and nothing from the feed reaches the
 * page as markup along this path. Script and style element contents are
 * removed so their code is not flattened into the text.
 */
function flattenMarkup(markup: string): string {
  const parsed = new DOMParser().parseFromString(markup, "text/html");
  for (const code of parsed.querySelectorAll("script, style")) code.remove();
  return (parsed.body.textContent ?? "").trim();
}

/**
 * Flattens an Atom Text construct (RFC 4287 §3.1) — the form used by
 * `<title>`, `<subtitle>`, `<name>`, and the like — to plain text. These are
 * rendered as plain text, not markup, so an `html`-typed construct needs its
 * markup resolved away rather than kept: otherwise a feed with e.g.
 * `<title type="html">…&lt;em&gt;…</title>` would show its tags literally
 * instead of the text they wrap.
 */
function textConstruct(element: Element): string {
  const type = normalizedType(element);
  if (type === "html" || type === "text/html") {
    return flattenMarkup(element.textContent ?? "");
  }
  // xhtml wraps real child elements, so textContent is already just the
  // text. Anything else (no type, or "text") has no markup to strip either.
  return element.textContent?.trim() ?? "";
}

function text(parent: Element, name: string): string {
  const element = child(parent, name);
  return element ? textConstruct(element) : "";
}

/**
 * The one link a reader would follow. Atom carries it in a `href` attribute
 * and RSS as the element's own text, so both are tried — which also covers
 * the common hybrid where an RSS channel adds an `<atom:link>` beside its
 * plain `<link>`. Links that declare a `rel` other than `alternate` (`self`,
 * `hub`, `enclosure`, …) point at something other than the human page, so
 * they're only considered when nothing better exists.
 */
function link(parent: Element): string {
  const links = children(parent, "link");
  const alternates = links.filter((item) => {
    const rel = item.getAttribute("rel");
    return !rel || rel === "alternate";
  });
  for (const candidate of alternates.length ? alternates : links) {
    const href = candidate.getAttribute("href")?.trim();
    if (href) return href;
    const value = candidate.textContent?.trim();
    if (value) return value;
  }
  return "";
}

/** Normalizes a body element's `type` attribute per RFC 4287 §4.1.3.1: missing
 * means "text", and any MIME parameters (`; charset=…`) are irrelevant to how
 * the body is structured, so they're stripped before comparison. */
function normalizedType(element: Element): string {
  const raw = element.getAttribute("type") ?? "text";
  return raw.split(";")[0].trim().toLowerCase();
}

/**
 * Resolves an Atom `<content>` or `<summary>` element to renderable markup,
 * per RFC 4287 §4.1.3.1. The order below matters: `application/xhtml+xml`
 * also ends in `+xml`, and `text/xml` also starts with `text/`, so the
 * specific cases are matched before the general ones. `text/html` is accepted
 * next to the spec's bare `html` because feeds write the MIME form in
 * practice. Anything left over is base64-encoded binary per the RFC, which
 * isn't something to show as text. As with all `kind: "html"` results, the
 * markup is untrusted and left for the caller to sanitize.
 */
function inlineBody(element: Element): EntryContent | null {
  const type = normalizedType(element);
  if (type === "html" || type === "text/html") {
    return { kind: "html", html: element.textContent ?? "" };
  }
  if (type === "xhtml" || type === "application/xhtml+xml") {
    const div = Array.from(element.children).find((item) => item.localName === "div");
    return { kind: "html", html: div?.innerHTML ?? element.textContent ?? "" };
  }
  if (type.endsWith("/xml") || type.endsWith("+xml")) {
    return { kind: "html", html: element.innerHTML };
  }
  if (type === "text" || type === "" || type.startsWith("text/")) {
    return { kind: "text", text: element.textContent?.trim() ?? "" };
  }
  return null;
}

/** True when a resolved body has nothing visible in it: absent, base64
 * (`inlineBody` returns null for that), or markup/text that's blank once
 * trimmed. Checked on the extracted markup rather than `textContent`, since
 * an xhtml body can be genuinely non-empty (e.g. a bare `<img>`) while its
 * text is empty. */
function isBlank(content: EntryContent | null): boolean {
  if (!content) return true;
  return (content.kind === "html" ? content.html : content.text).trim() === "";
}

// ---- Atom (RFC 4287) --------------------------------------------------------

/**
 * A `<content src="…">` MUST be empty per RFC 4287 §4.1.3.2 — the body lives
 * at that URL, not in the feed — and that's one of the cases where §4.1.2
 * guarantees the entry also carries a `<summary>`. So `<content>` is used
 * only when it's inline and resolves to something non-blank; otherwise
 * `<summary>` is tried the same way.
 */
function atomBody(entry: Element): EntryContent | null {
  const content = child(entry, "content");
  if (content && !content.hasAttribute("src")) {
    const body = inlineBody(content);
    if (!isBlank(body)) return body;
  }
  const summary = child(entry, "summary");
  if (summary) {
    const body = inlineBody(summary);
    if (!isBlank(body)) return body;
  }
  return null;
}

function parseAtomEntry(item: Element, feedAuthorElement: Element | null): Entry {
  const authorElement = child(item, "author") ?? feedAuthorElement;
  return {
    title: text(item, "title") || "Untitled entry",
    link: link(item),
    author: authorElement ? text(authorElement, "name") : "",
    date: text(item, "published") || text(item, "updated"),
    content: atomBody(item),
  };
}

function parseAtom(root: Element): Feed {
  const authorElement = child(root, "author");
  return {
    format: "atom",
    title: text(root, "title") || "Untitled feed",
    subtitle: text(root, "subtitle"),
    link: link(root),
    author: authorElement ? text(authorElement, "name") : "",
    updated: text(root, "updated"),
    entries: children(root, "entry").map((item) => parseAtomEntry(item, authorElement)),
  };
}

// ---- RSS 2.0 ----------------------------------------------------------------

/**
 * RSS has no `type` attribute and no Text/Content constructs: a
 * `<description>` or `<content:encoded>` is HTML by convention, however it
 * arrived (escaped, or wrapped in CDATA — the XML parser resolves both to the
 * same text). Bodies that carry no markup at all are reported as text so they
 * render as a paragraph, the way an Atom text body does, rather than as a
 * bare run of text with no spacing around it.
 */
function rssBody(item: Element): EntryContent | null {
  const encoded = childIn(item, NS.content, "encoded");
  const raw = (encoded ?? child(item, "description"))?.textContent?.trim() ?? "";
  if (!raw) return null;
  return MARKUP.test(raw) ? { kind: "html", html: raw } : { kind: "text", text: raw };
}

/**
 * RSS 2.0's `<author>` and `<managingEditor>` hold an email address, by the
 * spec's own grammar, optionally followed by a display name in parentheses:
 * `ed@example.com (Ed Example)`. The name is the part worth showing, so it's
 * lifted out when present; a bare address is still shown rather than dropped,
 * since for many feeds it's the only attribution there is. `<dc:creator>`
 * wins over both — it holds a plain name, and it's what publishing tools
 * actually write.
 */
function rssAuthor(container: Element): string {
  const creator = childIn(container, NS.dc, "creator")?.textContent?.trim();
  if (creator) return creator;
  const raw = text(container, "author") || text(container, "managingEditor");
  return raw.match(/\(([^)]+)\)/)?.[1].trim() ?? raw;
}

/**
 * An RSS item may omit `<link>` entirely and carry its URL as a permalink
 * `<guid>`, which is a plain identifier rather than a link when
 * `isPermaLink="false"` — and the attribute defaults to true when absent, so
 * a guid that isn't a URL has to be filtered out on its own merits.
 */
function rssLink(item: Element): string {
  const direct = link(item);
  if (direct) return direct;
  const guid = child(item, "guid");
  if (!guid || guid.getAttribute("isPermaLink")?.toLowerCase() === "false") return "";
  const value = guid.textContent?.trim() ?? "";
  return /^https?:\/\//i.test(value) ? value : "";
}

function parseRssItem(item: Element, channel: Element): Entry {
  return {
    title: text(item, "title") || "Untitled entry",
    link: rssLink(item),
    author: rssAuthor(item) || rssAuthor(channel),
    date: text(item, "pubDate"),
    content: rssBody(item),
  };
}

function parseRss(channel: Element): Feed {
  // A channel description is prose, but plenty of feeds put markup in it and
  // it is rendered as plain text here, so flatten it the way an html-typed
  // Atom subtitle is flattened. Titles are left alone: RSS titles are plain
  // text by convention, and flattening one would eat a literal "<div>" that
  // a post about markup quite reasonably put in its title.
  const description = text(channel, "description");
  return {
    format: "rss",
    title: text(channel, "title") || "Untitled feed",
    subtitle: MARKUP.test(description) ? flattenMarkup(description) : description,
    link: link(channel),
    author: rssAuthor(channel),
    updated: text(channel, "lastBuildDate") || text(channel, "pubDate"),
    entries: children(channel, "item").map((item) => parseRssItem(item, channel)),
  };
}

// ---- entry point ------------------------------------------------------------

/**
 * Parse raw feed XML into a `Feed`, or an error message. The root element is
 * what picks the parser: `<feed>` is Atom, `<rss>` is RSS (the `version`
 * attribute goes unread, so the near-identical 0.9x feeds parse too). The
 * "Untitled feed" / "Untitled entry" fallbacks are decided here so the
 * renderer never has to re-decide them.
 */
export function parseFeed(xml: string): ParseResult {
  if (!xml.trim()) return { ok: false, error: "Paste an Atom or RSS feed to preview it." };

  const documentXml = new DOMParser().parseFromString(xml, "application/xml");
  const parseError = documentXml.querySelector("parsererror");
  if (parseError) {
    return {
      ok: false,
      error: `That XML could not be parsed: ${parseError.textContent?.split("\n")[0] ?? "unknown error"}`,
    };
  }

  const root = documentXml.documentElement;
  if (root.localName === "feed") return { ok: true, feed: parseAtom(root) };
  if (root.localName === "rss") {
    const channel = child(root, "channel");
    if (!channel) {
      return { ok: false, error: "The RSS feed is missing its <channel> element." };
    }
    return { ok: true, feed: parseRss(channel) };
  }
  // RSS 1.0 is RDF with a flat <item> list rather than RSS 2.0 with a smaller
  // name, so it earns its own message instead of "not a feed".
  if (root.localName === "RDF") {
    return {
      ok: false,
      error: "RSS 1.0 (RDF) is not supported. Use an Atom or RSS 2.0 feed.",
    };
  }
  return {
    ok: false,
    error: "Expected <feed> for Atom or <rss> for RSS.",
  };
}
