// Turn raw fetched HTML into clean, sanitized, readable content.

import { Readability } from "@mozilla/readability";
import DOMPurify from "dompurify";

export interface ExtractResult {
  title: string;
  byline: string | null;
  siteName: string | null;
  contentHtml: string;
}

// Tags we keep in the reader. Everything else is stripped by DOMPurify.
const ALLOWED_TAGS = [
  "p", "br", "hr", "blockquote", "pre", "code",
  "h1", "h2", "h3", "h4", "h5", "h6",
  "ul", "ol", "li", "dl", "dt", "dd",
  "strong", "b", "em", "i", "u", "s", "sub", "sup", "mark", "small", "abbr", "cite", "q",
  "a", "img", "figure", "figcaption",
  "table", "thead", "tbody", "tfoot", "tr", "th", "td", "caption",
  "span", "div", "section", "article",
];

/**
 * Parse HTML, run Readability to isolate the main article, then sanitize.
 * Relative URLs are resolved against `baseUrl` first so links/images survive.
 */
export function extractReadable(html: string, baseUrl: string | null): ExtractResult {
  const parser = new DOMParser();
  const doc = parser.parseFromString(html, "text/html");

  // Note: we intentionally do NOT inject a <base> element here. Relative URLs
  // are resolved explicitly in resolveUrls() instead — setting <base> would
  // trip the page's `base-uri` Content-Security-Policy.

  const title =
    doc.querySelector("title")?.textContent?.trim() ||
    doc.querySelector("h1")?.textContent?.trim() ||
    "Untitled document";

  let article: ReturnType<Readability["parse"]> = null;
  try {
    // Readability mutates the document, so clone first.
    article = new Readability(doc.cloneNode(true) as Document, {
      charThreshold: 200,
    }).parse();
  } catch {
    article = null;
  }

  const rawContent =
    article?.content ||
    doc.querySelector("main")?.innerHTML ||
    doc.querySelector("article")?.innerHTML ||
    doc.body?.innerHTML ||
    "";

  const clean = sanitize(rawContent, baseUrl);

  return {
    title: article?.title?.trim() || title,
    byline: article?.byline?.trim() || null,
    siteName: (article as { siteName?: string } | null)?.siteName?.trim() || null,
    contentHtml: clean,
  };
}

/** Wrap arbitrary user-pasted text or HTML into clean readable content. */
export function extractFromPasted(input: string): ExtractResult {
  const looksLikeHtml = /<\/?[a-z][\s\S]*>/i.test(input);
  let contentHtml: string;
  if (looksLikeHtml) {
    contentHtml = sanitize(input, null);
  } else {
    // Treat as plain text: split on blank lines into paragraphs.
    contentHtml = input
      .split(/\n{2,}/)
      .map((para) => para.trim())
      .filter(Boolean)
      .map((para) => `<p>${escapeHtml(para).replace(/\n/g, "<br>")}</p>`)
      .join("\n");
  }
  const firstLine = input.trim().split("\n")[0]?.slice(0, 80) || "Pasted text";
  return { title: firstLine, byline: null, siteName: null, contentHtml };
}

function sanitize(html: string, baseUrl: string | null): string {
  const dirty = resolveUrls(html, baseUrl);
  return DOMPurify.sanitize(dirty, {
    ALLOWED_TAGS,
    ALLOWED_ATTR: ["href", "src", "alt", "title", "colspan", "rowspan"],
    ALLOW_DATA_ATTR: false,
    // Links open in a new tab and are neutered against tab-nabbing.
    ADD_ATTR: ["target", "rel"],
  });
}

/** Resolve relative href/src against the base URL so the reader isn't broken. */
function resolveUrls(html: string, baseUrl: string | null): string {
  if (!baseUrl) return html;
  const parser = new DOMParser();
  const doc = parser.parseFromString(html, "text/html");
  doc.querySelectorAll("a[href]").forEach((a) => {
    const href = a.getAttribute("href");
    if (href) {
      try {
        a.setAttribute("href", new URL(href, baseUrl).toString());
      } catch {
        /* leave as-is */
      }
    }
    a.setAttribute("target", "_blank");
    a.setAttribute("rel", "noopener noreferrer");
  });
  doc.querySelectorAll("img[src]").forEach((img) => {
    const src = img.getAttribute("src");
    if (src) {
      try {
        img.setAttribute("src", new URL(src, baseUrl).toString());
      } catch {
        /* leave as-is */
      }
    }
  });
  return doc.body.innerHTML;
}

function escapeHtml(s: string): string {
  return s
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}
