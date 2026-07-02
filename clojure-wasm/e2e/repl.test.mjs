// End-to-end test of the browser REPL.
//
//   BASE_URL      server to test against (default http://127.0.0.1:8080)
//   E2E_MODE      "server" (default) or "wasm"
//                 - server: evals go to the dev server JVM via /api/eval.
//                   Exercises the full UI, datastar signal wiring, transcript,
//                   and the bootstrap.clj JSON contract. Runs anywhere.
//                 - wasm: boots the real CheerpJ JVM in the browser. Needs
//                   https://cjrtnc.leaningtech.com to be reachable and the
//                   jars downloaded (bb jars). First boot can take minutes.
//   CHROMIUM_PATH optional explicit Chromium binary for playwright
//
// Run: node repl.test.mjs   (server must already be running; use `bb e2e`)

import assert from "node:assert/strict";
import { chromium } from "playwright";

const BASE_URL = process.env.BASE_URL ?? "http://127.0.0.1:8080";
const MODE = process.env.E2E_MODE === "wasm" ? "wasm" : "server";
const BOOT_TIMEOUT = MODE === "wasm" ? 900_000 : 60_000;

const log = (msg) => console.log(`  ${msg}`);

async function evalCode(page, code) {
  const before = await page.locator("#transcript .entry").count();
  await page.fill("#code", code);
  await page.click("#run");
  // each submission appends the input echo + (optional out) + val/err
  await page.waitForFunction(
    (n) => {
      const entries = document.querySelectorAll("#transcript .entry");
      const last = entries[entries.length - 1];
      return entries.length > n + 1 && ["val", "err"].includes(last?.dataset.kind);
    },
    before,
    { timeout: 120_000 }
  );
  return page.$$eval("#transcript .entry", (els) =>
    els.map((e) => ({ kind: e.dataset.kind, text: e.textContent }))
  );
}

const last = (entries) => entries[entries.length - 1];

async function main() {
  console.log(`e2e: ${BASE_URL} in ${MODE} mode`);

  const health = await fetch(`${BASE_URL}/healthz`).catch(() => null);
  if (!health?.ok) {
    console.error(`No server at ${BASE_URL} — start one with \`bb dev\` (or use \`bb e2e\`).`);
    process.exit(1);
  }

  const executablePath = process.env.CHROMIUM_PATH;
  const browser = await chromium.launch(executablePath ? { executablePath } : {});
  try {
    const page = await browser.newPage();
    page.on("pageerror", (e) => console.error("  [pageerror]", e.message));

    await page.goto(`${BASE_URL}/?mode=${MODE}`);

    log(`waiting for REPL to become ready (up to ${BOOT_TIMEOUT / 1000}s)…`);
    await page.waitForFunction(
      () => document.querySelector(".title-row .pill")?.textContent === "ready",
      undefined,
      { timeout: BOOT_TIMEOUT, polling: 500 }
    );
    log("✓ status pill reports ready (datastar signal wiring works)");

    let entries = await evalCode(page, "(+ 1 2)");
    assert.equal(last(entries).kind, "val");
    assert.equal(last(entries).text, "3");
    log("✓ (+ 1 2) => 3");

    await evalCode(page, "(defn square [x] (* x x))");
    entries = await evalCode(page, "(square 12)");
    assert.equal(last(entries).text, "144");
    log("✓ defn persists across submissions");

    entries = await evalCode(page, '(println "hello from the JVM")');
    const out = entries[entries.length - 2];
    assert.equal(out.kind, "out");
    assert.equal(out.text, "hello from the JVM");
    assert.equal(last(entries).text, "nil");
    log("✓ println output is captured");

    await evalCode(page, "(in-ns 'demo)");
    assert.equal(await page.locator(".prompt").textContent(), "demo=>");
    log("✓ (in-ns 'demo) updates the prompt via signals");

    entries = await evalCode(page, "(clojure.core// 1 0)");
    assert.equal(last(entries).kind, "err");
    assert.match(last(entries).text, /ArithmeticException/);
    log("✓ exceptions render as tagged errors");

    await evalCode(page, "(clojure.core/in-ns 'user)");
    assert.equal(await page.locator(".prompt").textContent(), "user=>");
    log("✓ back to user namespace");

    console.log(`\ne2e PASSED (${MODE} mode)`);
  } finally {
    await browser.close();
  }
}

main().catch((e) => {
  console.error("\ne2e FAILED:", e);
  process.exit(1);
});
