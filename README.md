# tools

A monorepo of small, self-contained web tools, deployed together as a single
Cloudflare Worker (with static assets) and served under one subdomain, each at
its own path:

- **`tools.kirahowe.com/annotate`** — [Annotate](tools/annotate) — load or paste any
  web page, annotate the exact passages you care about, and export your notes so
  each one stays tied to the span of text it refers to.
- **`tools.kirahowe.com/contrast`** — [Contrast](tools/contrast) — pick a background
  colour and choose text colours from only the ones with enough WCAG contrast to
  stay accessible.

## How it's organised

```
tools/<name>/        a web tool: TypeScript in src/, static assets in public/
  src/app.ts         entry point (bundled to /<name>/app.js)
  public/            index.html, styles.css, manifest, sw.js, icons (%BASE% templated)
  tool.json          title/description/emoji for the landing page
worker/index.ts      the Worker serving dynamic routes (e.g. /<name>/api/…)
web/                 the umbrella landing page, its icon + shared _headers
lib/                 shared UI: a linked CSS layer (tokens, base) plus build-time includes (e.g. the footer)
build.mjs            builds every tool into dist/<name>/, assembles the landing page
dist/                static assets Cloudflare serves (git-ignored)
```

Each tool is **mounted at `/<name>`**. The build injects that base path so a tool
works from a sub-path without a `<base>` tag:

- TypeScript is bundled with `__BASE__` defined to `"/<name>"` (used for the
  `/<name>/api/…` fetch and the service-worker scope).
- Static files have the `%BASE%` token replaced with `/<name>` (asset URLs, the
  web manifest's `start_url`/`scope`, the service worker's cache list).

Dynamic routes live in `worker/index.ts`, which matches on request path — the
annotate page-fetcher is served at `/annotate/api/fetch`. Static assets in
`dist/` are served directly; only paths with no matching file reach the Worker.

## Local development

```bash
npm install
npm run watch      # rebuild tools into dist/ on change (one terminal)
npm run preview    # wrangler dev  (another terminal: serves dist/ + the Worker)
```

Or a one-off build: `npm run build` (output in `dist/`).

Type checking: `npm run typecheck` (browser code) and `npm run typecheck:worker`.

## Deploying (Cloudflare Workers)

One Worker with static assets serves the whole repo. The repo is connected in
the Cloudflare dashboard (Workers &amp; Pages → Builds) with:

- **Build command:** `npm run build`
- **Deploy command:** `npx wrangler deploy`

`wrangler deploy` reads `wrangler.toml`: it bundles `worker/index.ts` and uploads
the `dist/` assets in one shot. Every push deploys; or deploy from your machine
with `npm run deploy`.

Point the custom domain `tools.kirahowe.com` at the Worker (the Worker's
Settings → Domains &amp; Routes). If the DNS zone is on Cloudflare it's one click.

## Adding a new web tool

1. `mkdir -p tools/<name>/src tools/<name>/public` and add an `app.ts`, an
   `index.html`, and a `tool.json`. Copy `tools/annotate` as a template — it's
   already base-path aware.
2. Reference assets with `%BASE%/…` in HTML/manifest, and use `${__BASE__}` for
   any same-origin API calls in TypeScript. In `<head>`, link `/lib/tokens.css`
   then `/lib/base.css` before your tool's own stylesheet — they supply the
   design tokens, reset, `.page` frame, and `.btn` pattern. Drop `%FOOTER%`
   wherever the shared footer (`lib/footer.html`) belongs in your page, and
   link `/lib/footer.css` in `<head>`.
3. Add any serverless endpoint as a branch in `worker/index.ts`, mounted under
   `/<name>/api/…`.
4. `npm run build` — it appears in `dist/<name>/` and on the landing page
   automatically.

## Philosophy

This repo is the **sandbox** where small tools start life cheaply. The rule for
what lives here vs. its own repo is the *release lifecycle*: things that deploy
together and share no independent versioning stay here; a tool that needs its own
versioned releases (e.g. a CLI shipping binaries via GitHub Actions), its own
issue tracker, or a heavier backend **graduates** to a dedicated repo (and, if it
has a UI, its own subdomain). Dead experiments just get deleted — git history
keeps them.

## License

MIT © Kira Howe
