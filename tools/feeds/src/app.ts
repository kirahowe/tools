// Application wiring: parse with feed.ts, then render the model into the
// page, sanitizing entry bodies at that boundary.

import DOMPurify from "dompurify";
import { parseFeed, type Entry, type EntryContent, type FeedFormat } from "./feed.js";

const $ = <T extends HTMLElement = HTMLElement>(id: string): T => {
  const element = document.getElementById(id);
  if (!element) throw new Error(`missing #${id}`);
  return element as T;
};

const form = $<HTMLFormElement>("feedForm");
const source = $<HTMLTextAreaElement>("feedSource");
const previewButton = $<HTMLButtonElement>("previewButton");
const atomSampleButton = $<HTMLButtonElement>("atomSampleButton");
const rssSampleButton = $<HTMLButtonElement>("rssSampleButton");
const error = $<HTMLParagraphElement>("error");
// A live region kept outside #preview: the preview itself must never carry
// aria-live, or a screen reader reads the whole feed aloud on every render.
const previewStatus = $<HTMLParagraphElement>("previewStatus");
const preview = $<HTMLElement>("preview");
const previewKind = $<HTMLParagraphElement>("previewKind");
const feedTitle = $<HTMLHeadingElement>("feedTitle");
const feedSubtitle = $<HTMLParagraphElement>("feedSubtitle");
const feedMeta = $("feedMeta");
const entries = $("entries");
// What to call each format on the page. The parser reports which document
// shape it read; naming it is a rendering decision, so the label lives here.
const FORMAT_LABEL: Record<FeedFormat, string> = { atom: "Atom", rss: "RSS 2.0" };

// The label is authored in index.html; capture it so "Loading…" can be undone
// without a second copy of the string here.
const previewLabel = previewButton.textContent ?? "";

// The same two posts in each format, so switching samples shows the parsing
// difference rather than a different feed. Between them they cover what the
// parser has to get right: a summary standing in for an absent body, escaped
// html, CDATA, a permalink <guid> with no <link> beside it, an author given
// as an address with a name after it, and both date formats.
const SAMPLE_ATOM = `<?xml version="1.0" encoding="utf-8"?>
<feed xmlns="http://www.w3.org/2005/Atom">
  <title>Lorem ipsum</title>
  <subtitle>Dolor sit amet, consectetur adipiscing elit.</subtitle>
  <link href="https://example.com/"/>
  <updated>2026-08-12T14:00:00Z</updated>
  <author><name>Lorem Ipsum</name></author>
  <entry>
    <title>Consectetur adipiscing elit</title>
    <link href="https://example.com/lorem-ipsum"/>
    <id>https://example.com/lorem-ipsum</id>
    <updated>2026-08-12T14:00:00Z</updated>
    <summary>Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.</summary>
  </entry>
  <entry>
    <title>Ut enim ad minim veniam</title>
    <link href="https://example.com/consectetur"/>
    <id>https://example.com/consectetur</id>
    <updated>2026-08-08T09:30:00Z</updated>
    <content type="html">&lt;p&gt;Quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.&lt;/p&gt;</content>
  </entry>
</feed>`;

const SAMPLE_RSS = `<?xml version="1.0" encoding="utf-8"?>
<rss version="2.0"
     xmlns:content="http://purl.org/rss/1.0/modules/content/"
     xmlns:dc="http://purl.org/dc/elements/1.1/">
  <channel>
    <title>Lorem ipsum</title>
    <link>https://example.com/</link>
    <description>Dolor sit amet, consectetur adipiscing elit.</description>
    <lastBuildDate>Wed, 12 Aug 2026 14:00:00 GMT</lastBuildDate>
    <managingEditor>lorem@example.com (Lorem Ipsum)</managingEditor>
    <item>
      <title>Consectetur adipiscing elit</title>
      <link>https://example.com/lorem-ipsum</link>
      <guid>https://example.com/lorem-ipsum</guid>
      <pubDate>Wed, 12 Aug 2026 14:00:00 GMT</pubDate>
      <description>Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.</description>
    </item>
    <item>
      <title>Ut enim ad minim veniam</title>
      <guid isPermaLink="true">https://example.com/consectetur</guid>
      <pubDate>Sat, 08 Aug 2026 09:30:00 GMT</pubDate>
      <dc:creator>Dolor Sit</dc:creator>
      <content:encoded><![CDATA[<p>Quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.</p>]]></content:encoded>
    </item>
  </channel>
</rss>`;

function formatDate(value: string): string {
  if (!value) return "";
  const date = new Date(value);
  return Number.isNaN(date.valueOf())
    ? value
    : new Intl.DateTimeFormat(undefined, { dateStyle: "medium" }).format(date);
}

// A pasted feed has no base URL of its own; resolving its relative links
// against this page would mint links to the tool's own origin. A null base
// means only already-absolute values are accepted.
function safeUrl(value: string, baseUrl: string | null): string {
  // An absent href resolves to the base itself, which would turn "no link" —
  // or a link DOMPurify already threw out — into a live link to the feed's
  // own document that nobody wrote.
  if (!value.trim()) return "";
  try {
    const url = baseUrl === null ? new URL(value) : new URL(value, baseUrl);
    return ["http:", "https:", "mailto:"].includes(url.protocol) ? url.href : "";
  } catch {
    return "";
  }
}

// A click on a preview link shouldn't cost someone the XML they pasted into
// the textarea (this page holds no other copy of it), so every link opens in
// a new tab; noopener noreferrer is what makes target="_blank" safe to hand
// to untrusted feed content.
function openInNewTab(anchor: HTMLAnchorElement): void {
  anchor.target = "_blank";
  anchor.rel = "noopener noreferrer";
}

function linkedText(label: string, href: string, baseUrl: string | null): Node {
  const safeHref = safeUrl(href, baseUrl);
  if (!safeHref) return document.createTextNode(label);
  const anchor = document.createElement("a");
  anchor.href = safeHref;
  anchor.textContent = label;
  openInNewTab(anchor);
  return anchor;
}

// Tags we keep in an entry body. Anything else is unwrapped in place by
// DOMPurify (KEEP_CONTENT defaults on, so the text survives) except for its
// bad-list — script, iframe and friends — which is discarded wholesale. Feed
// content is untrusted third-party HTML, so it goes through the same audited
// sanitizer Annotate uses rather than a bespoke walk.
const ALLOWED_TAGS = [
  "a", "p", "br", "strong", "em", "b", "i", "code", "pre", "blockquote",
  "ul", "ol", "li", "h1", "h2", "h3", "h4", "img", "figure", "figcaption",
];

function sanitizeHtml(raw: string, baseUrl: string | null): DocumentFragment {
  const fragment = DOMPurify.sanitize(raw, {
    ALLOWED_TAGS,
    ALLOWED_ATTR: ["href", "src", "alt"],
    ALLOW_DATA_ATTR: false,
    RETURN_DOM_FRAGMENT: true,
  });

  for (const anchor of Array.from(fragment.querySelectorAll("a"))) {
    const href = safeUrl(anchor.getAttribute("href") ?? "", baseUrl);
    if (href) {
      anchor.setAttribute("href", href);
      openInNewTab(anchor);
    } else {
      anchor.removeAttribute("href");
    }
  }
  for (const img of Array.from(fragment.querySelectorAll("img"))) {
    const src = safeUrl(img.getAttribute("src") ?? "", baseUrl);
    if (src) img.setAttribute("src", src);
    else img.removeAttribute("src");
    img.loading = "lazy";
  }
  return fragment;
}

function addMeta(container: Element, values: string[]): void {
  container.replaceChildren();
  for (const value of values.filter(Boolean)) {
    const span = document.createElement("span");
    span.textContent = value;
    container.append(span);
  }
}

function fail(message: string): void {
  error.textContent = message;
  error.hidden = false;
  preview.hidden = true;
  previewStatus.textContent = "";
}

function contentFragment(content: EntryContent | null, baseUrl: string | null): DocumentFragment {
  if (!content) return document.createDocumentFragment();
  if (content.kind === "html") return sanitizeHtml(content.html, baseUrl);
  const fragment = document.createDocumentFragment();
  const paragraph = document.createElement("p");
  paragraph.textContent = content.text;
  fragment.append(paragraph);
  return fragment;
}

function renderEntry(entry: Entry, baseUrl: string | null): HTMLElement {
  const article = document.createElement("article");
  article.className = "entry";
  const heading = document.createElement("h3");
  heading.append(linkedText(entry.title, entry.link, baseUrl));
  const meta = document.createElement("div");
  meta.className = "meta";
  addMeta(meta, [entry.author, formatDate(entry.date)]);
  const body = document.createElement("div");
  body.className = "entry-body";
  body.append(contentFragment(entry.content, baseUrl));
  article.append(heading, meta, body);
  return article;
}

function render(raw: string, baseUrl: string | null = null): void {
  error.hidden = true;
  preview.hidden = true;

  const result = parseFeed(raw);
  if (!result.ok) return fail(result.error);
  const { feed } = result;

  previewKind.textContent = `${FORMAT_LABEL[feed.format]} feed`;
  feedTitle.replaceChildren(linkedText(feed.title, feed.link, baseUrl));
  feedSubtitle.textContent = feed.subtitle;
  feedSubtitle.hidden = !feed.subtitle;
  addMeta(feedMeta, [feed.author, formatDate(feed.updated)]);

  entries.replaceChildren();
  if (feed.entries.length === 0) {
    const empty = document.createElement("p");
    empty.className = "empty";
    empty.textContent = "This feed has no entries.";
    entries.append(empty);
  }
  for (const entry of feed.entries) {
    entries.append(renderEntry(entry, baseUrl));
  }
  preview.hidden = false;

  const count = feed.entries.length;
  const what = `the ${FORMAT_LABEL[feed.format]} feed ${feed.title}`;
  previewStatus.textContent = count === 0
    ? `Loaded ${what}. No entries.`
    : `Loaded ${count} ${count === 1 ? "entry" : "entries"} from ${what}.`;
}

// A bare host like "example.com/feed.xml" isn't a URL as far as new URL() is
// concerned, but it's what people type. Whitespace or an angle bracket means
// it's feed XML, not a hostname.
const URL_SHAPED = /^[a-z0-9][a-z0-9-]*(\.[a-z0-9-]+)*\.[a-z]{2,}(:\d+)?([/?#]\S*)?$/i;

function inputUrl(value: string): URL | null {
  const trimmed = value.trim();
  const candidate = URL_SHAPED.test(trimmed) ? `https://${trimmed}` : trimmed;
  try {
    const url = new URL(candidate);
    return url.protocol === "http:" || url.protocol === "https:" ? url : null;
  } catch {
    return null;
  }
}

interface FetchResult {
  xml?: string;
  finalUrl?: string;
  error?: string;
}

async function fetchAndRender(url: URL): Promise<void> {
  previewButton.disabled = true;
  previewButton.textContent = "Loading…";
  error.hidden = true;
  try {
    const response = await fetch(`${__BASE__}/api/fetch?url=${encodeURIComponent(url.href)}`);
    const result = await response.json() as FetchResult;
    if (!response.ok || !result.xml) throw new Error(result.error || "Could not load that feed.");
    render(result.xml, result.finalUrl || url.href);
  } catch (reason) {
    fail(reason instanceof Error ? reason.message : "Could not load that feed.");
  } finally {
    previewButton.disabled = false;
    previewButton.textContent = previewLabel;
  }
}

// The URL-or-XML decision, shared by the submit handler and the deep link
// below so there's only one place that makes it.
function previewSource(value: string): void {
  const url = inputUrl(value);
  if (url) {
    void fetchAndRender(url);
  } else {
    render(value);
  }
}

form.addEventListener("submit", (event) => {
  event.preventDefault();
  previewSource(source.value);
});

function useSample(xml: string): void {
  source.value = xml;
  previewSource(xml);
}

atomSampleButton.addEventListener("click", () => useSample(SAMPLE_ATOM));
rssSampleButton.addEventListener("click", () => useSample(SAMPLE_RSS));

// Enter in a textarea inserts a newline, so submitting needs its own
// shortcut. Go through requestSubmit rather than previewSource directly to
// keep a single submit path.
source.addEventListener("keydown", (event) => {
  if ((event.metaKey || event.ctrlKey) && event.key === "Enter") {
    event.preventDefault();
    form.requestSubmit();
  }
});

// Deep-link support: ?url=… loads immediately (nice for a bookmarklet).
const deepLinkUrl = new URLSearchParams(location.search).get("url");
if (deepLinkUrl) {
  source.value = deepLinkUrl;
  previewSource(deepLinkUrl);
}
