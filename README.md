# Annotate for LLM

Load any public web page, highlight the exact passages you want to comment on,
and export your notes in a format an LLM understands — with each note tied to the
**precise span of text** it refers to.

The motivating use case: paste a URL to a book chapter, mark up the parts you want
changed, then hand the export to an assistant so it knows exactly which sentences
each note applies to and can revise the text in place.

- **No framework** — plain HTML/CSS and vanilla TypeScript.
- **Deployable & mobile-first** — a static, installable PWA.
- **No server to run** — the only server-side piece is a tiny on-demand fetch
  proxy that runs as a Cloudflare Pages Function (scales to zero, free tier).

![desktop](docs/screenshot-desktop.png)

## Why there's a (serverless) proxy

Everything that matters — extracting readable text, anchoring notes to exact
character offsets, highlighting, exporting, and saving your notes — runs entirely
in your browser. The *one* thing a browser can't do is fetch an arbitrary other
website: cross-origin requests are blocked by CORS, and that's enforced by the
browser itself (WebAssembly, service workers, etc. can't get around it).

So a single serverless function (`functions/api/fetch.ts`) fetches the page for
you on Cloudflare's edge and hands back the HTML. It runs on demand and scales to
zero — there's no process to keep alive. If you'd rather not run it at all, the
**Paste** button lets you drop in text or HTML directly, fully offline.

## Quick start (local)

```bash
npm install
npm run build       # bundles the frontend to public/app.js
npm run preview     # serves public/ + the function via wrangler at localhost:8788
```

For active development, run the watcher in one terminal and the preview in another:

```bash
npm run watch       # rebuilds public/app.js on change
npm run preview     # wrangler pages dev
```

## Deploy to Cloudflare Pages

**Option A — connect the Git repo (recommended).** In the Cloudflare dashboard,
create a Pages project from this repo with:

- **Build command:** `npm run build`
- **Build output directory:** `public`

Functions in `functions/` are picked up automatically. Every push deploys.

**Option B — deploy from your machine:**

```bash
npm run deploy      # npm run build && wrangler pages deploy public
```

The app is a PWA, so once loaded on a phone you can "Add to Home Screen" and it
works offline for any documents you've already opened.

> The same static frontend also works on hosts without serverless functions
> (e.g. GitHub Pages) — you just lose URL fetching and rely on **Paste** mode.

## Using it

1. Paste a public page URL and press **Load** (or use **Paste** for raw text/HTML).
2. Select any text — an **Add note** button appears — and write your comment.
3. Notes are listed on the right (a drawer on mobile) and saved on your device.
4. Press **Export** and copy the result into your LLM.

Tip: `?url=https://…` loads a page immediately, which makes a handy bookmarklet.

## Export formats

- **Inline document** — the full text reproduced verbatim, with each annotated
  span wrapped in `⟦ ⟧` and marked with a footnote reference (`[^1]`) whose note
  is listed at the end. Best for asking an LLM to revise the document in place.
- **Notes list** — a readable list; each note carries its exact quote, the
  surrounding context, and a location (nearest heading + block + char range).
- **JSON** — structured output (W3C Web Annotation–style `TextQuoteSelector` +
  `TextPositionSelector`) including the full document text and character offsets,
  for programmatic use.

## How anchoring stays precise

Each annotation records the exact quoted text plus a prefix/suffix of surrounding
context and character offsets into a single canonical plain-text version of the
document. On reload the note is re-located by searching for that quote (scored by
context and position), so annotations survive re-fetches and minor page edits; if
the quote can't be found the note is shown as *orphaned* rather than silently
pointing at the wrong place.

Highlights are painted with the [CSS Custom Highlight API](https://developer.mozilla.org/docs/Web/API/CSS_Custom_Highlight_API),
which draws over the text **without inserting elements** — keeping the DOM
(and therefore the offsets) exactly as measured. Where the API is unavailable the
app still works; only the inline tint is skipped.

## Project layout

```
functions/api/fetch.ts   Cloudflare Pages Function: the CORS fetch proxy
src/
  app.ts                 UI wiring: load, select→note, sidebar, export, persistence
  textmap.ts             canonical text + exact DOM-range ↔ offset mapping
  anchor.ts              re-locate stored annotations in the current text
  highlight.ts           CSS Custom Highlight API rendering
  extract.ts             Readability extraction + DOMPurify sanitizing
  exporters.ts           the three export formats
  storage.ts             localStorage persistence
  types.ts               shared types
public/                  index.html, styles.css, manifest, service worker, icons
build.mjs                esbuild bundling for the frontend
```

## Security notes

- Fetched HTML is sanitized with **DOMPurify** before it's ever rendered.
- The proxy only allows `http`/`https`, caps responses at 8 MB, times out, and
  blocks private/loopback/link-local addresses (basic SSRF protection).
- A strict `Content-Security-Policy` (see `public/_headers`) is applied.

## License

MIT © Kira Howe
