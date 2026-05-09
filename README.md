# Sheet Music Player

Upload a photo or scan of sheet music. The backend recognises the notes
(via [oemer](https://github.com/BreezeWhite/oemer)) and the browser plays
them back over the rendered score.

## Architecture

```
┌──────────────────┐  multipart  ┌─────────────────────────┐
│ static frontend  │ ──────────▶ │ Clojure API (Cloud Run) │
│ (Pages / GH /    │             │  Ring + http-kit        │
│  any static host)│ ◀────────── │  shells out to oemer    │
└──────────────────┘   MusicXML  └─────────────────────────┘
        │
        ▼
  OSMD renders score, Tone.js plays it
```

The OMR engine is a single function `(bytes, opts) -> musicxml-string`.
Swapping oemer for SMT++ on Modal later means adding one new namespace and
one new entry in `omr.server/engines`. Nothing else changes.

```
backend/
  src/omr/
    server.clj          ; imperative shell — env, engine selection, http-kit
    handler.clj         ; pure-ish: request -> response, engine fn injected
    engine/oemer.clj    ; recognize fn (shells out via babashka.process)
  build.clj             ; tools.build uberjar
  Dockerfile            ; JRE + Python + oemer

web/
  index.html app.js styles.css config.js

deploy/
  cloud-run.sh          ; gcloud build + run deploy

docker-compose.yml      ; local: api on :8080, static web on :5173
```

## Run locally

```bash
docker compose up --build
# Frontend: http://localhost:5173
# API:      http://localhost:8080/health
```

The first build takes a few minutes (downloading oemer + dependencies).
The first OMR call is also slow as oemer fetches its model weights.

## Deploy

**API → Cloud Run**

```bash
PROJECT_ID=your-gcp-project ./deploy/cloud-run.sh
```

The script builds with Cloud Build (no local Docker daemon required),
pushes to Artifact Registry, and deploys a scale-to-zero Cloud Run service.
Free tier covers low-traffic hobby use.

**Frontend → any static host**

The `web/` directory is plain static files. After deploying the API, edit
`web/config.js` and set `API_URL` to the Cloud Run URL printed by the
deploy script. Then upload `web/` to GitHub Pages, Cloudflare Pages,
Netlify, S3+CloudFront, or any static host.

## Caveats

- **Cold start.** Cloud Run scales to zero by default; the first request
  after idle takes ~10–30s to wake. Subsequent requests on the warm
  instance are immediate (modulo OMR processing time, ~30–90s/page on CPU).
- **OMR accuracy.** Oemer is decent on phone photos and good on clean
  scans, but no OMR is perfect. Expect mistakes on complex scores.
- **Playback fidelity.** The frontend extracts notes from OSMD and plays
  them on a Tone.js synth. Repeats, voltas, multi-voice writing, grace
  notes, dynamics, and articulations are not modelled.
