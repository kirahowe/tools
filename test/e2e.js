// Headless end-to-end test: image -> in-browser OMR -> MusicXML -> OSMD
// render -> Tone.js playback schedule.
//
// Usage: node test/e2e.js <image> [--deskew] [--port 8765]
// Requires: `npm i playwright` somewhere on NODE_PATH, the preinstalled
// chromium, vendor/ populated (tools/fetch_vendor.py) and serve.py running
// is NOT required — this script starts its own server.

const { spawn } = require("child_process");
const fs = require("fs");
const path = require("path");
const { chromium } = require("playwright");

const ROOT = path.resolve(__dirname, "..");
const ARTIFACTS = path.join(__dirname, "artifacts");

const argv = process.argv.slice(2);
const image = argv.find((a) => !a.startsWith("--"));
const deskew = argv.includes("--deskew");
const port = Number(argv[argv.indexOf("--port") + 1]) || 8765;

if (!image) {
  console.error("usage: node test/e2e.js <image> [--deskew]");
  process.exit(2);
}

const TOTAL_TIMEOUT_MS = 45 * 60 * 1000;

async function main() {
  fs.mkdirSync(ARTIFACTS, { recursive: true });

  const server = spawn("python3", ["serve.py", String(port)], {
    cwd: ROOT,
    stdio: "ignore",
  });
  await new Promise((r) => setTimeout(r, 1000));

  const browser = await chromium.launch({
    executablePath: "/opt/pw-browsers/chromium",
    headless: true,
    args: [
      "--autoplay-policy=no-user-gesture-required",
      "--enable-features=SharedArrayBuffer",
    ],
  });

  try {
    const page = await browser.newPage();
    page.on("console", (m) => {
      const t = m.text();
      if (!t.startsWith("[omr] Loading") || t.includes("loaded")) {
        console.log(`  [console] ${t.slice(0, 200)}`);
      }
    });
    page.on("pageerror", (e) => console.log(`  [pageerror] ${e.message}`));

    console.log(`opening http://localhost:${port}/ …`);
    await page.goto(`http://localhost:${port}/`, { waitUntil: "load" });

    await page.waitForFunction(() => window.__smp, null, { timeout: 30000 });
    console.log("page booted; crossOriginIsolated =", await page.evaluate(() => crossOriginIsolated));

    if (!deskew) await page.uncheck("#deskew");
    await page.setInputFiles("#file", path.resolve(image));
    console.log(`submitted ${image} (deskew=${deskew}); waiting for OMR…`);

    // Poll status until done/failed, logging transitions.
    const t0 = Date.now();
    let last = "";
    for (;;) {
      if (Date.now() - t0 > TOTAL_TIMEOUT_MS) throw new Error("timeout waiting for OMR");
      const { text, cls } = await page.evaluate(() => ({
        text: document.getElementById("status").textContent.trim(),
        cls: document.getElementById("status").className,
      }));
      if (text !== last) {
        console.log(`  [${((Date.now() - t0) / 1000).toFixed(0)}s] ${text.slice(0, 120)}`);
        last = text;
      }
      if (cls.includes("error")) throw new Error(`app reported failure: ${text}`);
      if (cls.includes("ok") && /Done in/.test(text)) break;
      await new Promise((r) => setTimeout(r, 5000));
    }

    // Save the MusicXML artifact.
    const musicxml = await page.evaluate(() => window.__smp.state.lastMusicXML);
    const outPath = path.join(ARTIFACTS, "browser-output.musicxml");
    fs.writeFileSync(outPath, musicxml);
    console.log(`MusicXML: ${musicxml.length} bytes -> ${outPath}`);

    // Score rendered as SVG?
    const svgs = await page.locator("#score svg").count();
    if (svgs < 1) throw new Error("no SVG rendered in #score");
    console.log(`score rendered (${svgs} svg element(s))`);

    // Press play; verify a schedule was built and the transport started.
    await page.click("#play");
    await page.waitForFunction(
      () => window.__smp.state.events.length > 0 && Tone.Transport.state === "started",
      null,
      { timeout: 30000 }
    );
    const nEvents = await page.evaluate(() => window.__smp.state.events.length);
    const totalDur = await page.evaluate(() => window.__smp.state.totalDur);
    console.log(`playback started: ${nEvents} note events, ${totalDur.toFixed(1)}s total`);

    await page.click("#stop");
    await page.screenshot({ path: path.join(ARTIFACTS, "final.png"), fullPage: true });
    console.log("PASS");
  } finally {
    await browser.close();
    server.kill();
  }
}

main().catch((e) => {
  console.error("FAIL:", e.message);
  process.exit(1);
});
