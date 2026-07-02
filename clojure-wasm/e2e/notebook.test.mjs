// End-to-end test of the notebook prototype (server kernel — same cell
// contract as the browser JVM; see repl.test.mjs for env vars).

import assert from "node:assert/strict";
import { chromium } from "playwright";

const BASE_URL = process.env.BASE_URL ?? "http://127.0.0.1:8080";
const log = (msg) => console.log(`  ${msg}`);

const ready = (page, timeout = 60_000) =>
  page.waitForFunction(
    () => document.querySelector(".title-row .pill")?.textContent === "ready",
    undefined,
    { timeout, polling: 250 }
  );

async function main() {
  console.log(`notebook e2e: ${BASE_URL}`);
  const health = await fetch(`${BASE_URL}/healthz`).catch(() => null);
  if (!health?.ok) {
    console.error(`No server at ${BASE_URL} — start one with \`bb dev\`.`);
    process.exit(1);
  }

  const executablePath = process.env.CHROMIUM_PATH;
  const browser = await chromium.launch(executablePath ? { executablePath } : {});
  try {
    const page = await browser.newPage();
    page.on("pageerror", (e) => console.error("  [pageerror]", e.message));

    // --- welcome example: run all, check kind rendering ---------------------
    await page.goto(`${BASE_URL}/notebook.html?example=welcome&kernel=server`);
    await ready(page);
    log("✓ notebook ready on the server kernel");

    assert.ok((await page.locator("#cells .md-render h1").first().textContent()).includes("Notebooks"));
    log("✓ markdown cells render");

    await page.click("#run-all");
    await page.waitForFunction(
      () => document.querySelectorAll('#cells .cell-out [data-kind], #cells .cell-out .out-block').length >= 6,
      undefined, { timeout: 60_000 }
    );
    await page.waitForFunction(
      () => !document.querySelector("#cells .out-block.running"),
      undefined, { timeout: 60_000 }
    );

    assert.ok(await page.locator("#cells table.nb-table").count() >= 1, "expected a rendered table");
    log("✓ seq-of-maps rendered as a table");

    assert.ok((await page.locator("#cells .out-block.hiccup").first().innerHTML()).includes("<h3>"));
    log("✓ hiccup rendered as HTML");

    const vegaCell = page.locator("#cells .out-block.vega").first();
    assert.ok((await vegaCell.count()) === 1);
    log("✓ vega cell present (embed or spec fallback)");

    assert.ok(await page.locator("#cells .out-block.md").count() >= 1);
    log("✓ (kind/md …) rendered as markdown");

    // --- editing: add a cell, run it, defs shared with earlier cells --------
    await page.click("#add-code");
    const newSrc = page.locator("#cells .cell.code .cell-src").last();
    await newSrc.fill("(count population)");
    await newSrc.press("Control+Enter");
    await page.waitForFunction(
      () => {
        const outs = document.querySelectorAll("#cells .cell-out");
        const last = outs[outs.length - 1];
        return last && last.textContent.trim() === "8";
      },
      undefined, { timeout: 30_000 }
    );
    log("✓ new cell sees defs from earlier cells (shared kernel world)");

    // --- save, reload by id, share view mode --------------------------------
    await page.fill("#nb-title", "e2e notebook");
    await page.click("#save");
    await page.waitForFunction(
      () => new URL(location.href).searchParams.get("nb") === "e2e-notebook",
      undefined, { timeout: 15_000 }
    );
    log("✓ saved to the server as e2e-notebook");

    await page.goto(`${BASE_URL}/notebook.html?nb=e2e-notebook&kernel=server&view=1`);
    await ready(page);
    // view mode auto-runs; the appended (count population) cell should render 8
    await page.waitForFunction(
      () => [...document.querySelectorAll("#cells .cell-out .out-block.val")]
        .some((el) => el.textContent.trim() === "8"),
      undefined, { timeout: 60_000 }
    );
    assert.equal(await page.locator("#cells textarea").count(), 0, "view mode should not be editable");
    log("✓ share link (view mode) loads, auto-runs, and is read-only");

    console.log("\nnotebook e2e PASSED");
  } finally {
    await browser.close();
  }
}

main().catch((e) => {
  console.error("\nnotebook e2e FAILED:", e);
  process.exit(1);
});
