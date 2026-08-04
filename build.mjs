// Root build for the tools monorepo.
//
// Each tool lives in tools/<name>/ and is mounted at /<name> on the deployed
// site (e.g. tools.kirahowe.com/annotate). This script builds every tool into
// dist/<name>/, injecting that mount path so a tool works from a sub-path:
//   - its TypeScript is bundled with `__BASE__` defined to "/<name>"
//   - its static files have the "%BASE%" token replaced with "/<name>"
//   - a "%FOOTER%" token, if present, is replaced with lib/footer.html (itself
//     with "%TOOL%" replaced by the tool's own name) — shared cross-tool UI
//     lives in lib/ as plain build-time includes, not a runtime component
// It then assembles the umbrella landing page (web/index.html) into dist/,
// listing every tool from its tool.json. lib/*.css is also copied verbatim to
// dist/lib/ so tools can link it directly (e.g. <link href="/lib/footer.css">).
//
//   node build.mjs            one-off build of everything into dist/
//   node build.mjs --watch    rebuild on change (JS + static)
//
// Cloudflare serves dist/ as static assets; dynamic routes are handled by the
// Worker in worker/index.ts (see wrangler.toml). A request that matches a built
// file is served directly; anything else falls through to the Worker.

import * as esbuild from "esbuild";
import { readdir, readFile, writeFile, mkdir, rm, copyFile } from "node:fs/promises";
import { existsSync, watch as fsWatch } from "node:fs";
import { join, extname } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = fileURLToPath(new URL(".", import.meta.url));
const TOOLS_DIR = join(ROOT, "tools");
const WEB_DIR = join(ROOT, "web");
const LIB_DIR = join(ROOT, "lib");
const DIST = join(ROOT, "dist");
const watch = process.argv.includes("--watch");

// Files we run %BASE% token replacement on (everything else is copied verbatim).
const TEXT_EXT = new Set([".html", ".css", ".js", ".json", ".webmanifest", ".svg", ".txt", ".xml"]);

async function discoverTools() {
  const entries = await readdir(TOOLS_DIR, { withFileTypes: true });
  const tools = [];
  for (const e of entries) {
    if (!e.isDirectory()) continue;
    const dir = join(TOOLS_DIR, e.name);
    if (!existsSync(join(dir, "src/app.ts"))) continue; // skip non-web tools
    let meta = {};
    const metaPath = join(dir, "tool.json");
    if (existsSync(metaPath)) {
      try {
        meta = JSON.parse(await readFile(metaPath, "utf8"));
      } catch {
        /* ignore bad tool.json */
      }
    }
    tools.push({ name: e.name, dir, base: `/${e.name}`, outDir: join(DIST, e.name), meta });
  }
  return tools;
}

function jsOptions(tool) {
  return {
    entryPoints: [join(tool.dir, "src/app.ts")],
    outfile: join(tool.outDir, "app.js"),
    bundle: true,
    format: "iife",
    platform: "browser",
    target: ["es2021"],
    sourcemap: true,
    minify: !watch,
    define: { __BASE__: JSON.stringify(tool.base) },
    logLevel: "info",
  };
}

async function copyTree(src, dst, tokens) {
  await mkdir(dst, { recursive: true });
  for (const e of await readdir(src, { withFileTypes: true })) {
    const s = join(src, e.name);
    const d = join(dst, e.name);
    if (e.isDirectory()) {
      await copyTree(s, d, tokens);
    } else if (TEXT_EXT.has(extname(e.name))) {
      let text = await readFile(s, "utf8");
      for (const [k, v] of Object.entries(tokens)) text = text.split(k).join(v);
      await writeFile(d, text);
    } else {
      await copyFile(s, d);
    }
  }
}

async function readFooterTemplate() {
  const p = join(LIB_DIR, "footer.html");
  return existsSync(p) ? await readFile(p, "utf8") : "";
}

async function copyStatic(tool, footerTemplate = "") {
  const pub = join(tool.dir, "public");
  const footer = footerTemplate.split("%TOOL%").join(tool.name);
  if (existsSync(pub)) await copyTree(pub, tool.outDir, { "%BASE%": tool.base, "%FOOTER%": footer });
}

async function copyLib() {
  if (!existsSync(LIB_DIR)) return;
  const dst = join(DIST, "lib");
  await mkdir(dst, { recursive: true });
  for (const e of await readdir(LIB_DIR, { withFileTypes: true })) {
    if (e.isFile() && extname(e.name) === ".css") await copyFile(join(LIB_DIR, e.name), join(dst, e.name));
  }
}

function esc(s) {
  return String(s).replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]));
}

async function buildLanding(tools) {
  await mkdir(DIST, { recursive: true });
  if (!existsSync(WEB_DIR)) return;
  for (const e of await readdir(WEB_DIR, { withFileTypes: true })) {
    if (e.name === "index.html") continue;
    const s = join(WEB_DIR, e.name);
    if (e.isDirectory()) await copyTree(s, join(DIST, e.name), {});
    else await copyFile(s, join(DIST, e.name));
  }
  const landingSrc = join(WEB_DIR, "index.html");
  if (!existsSync(landingSrc)) return;
  const items = tools
    .map(
      (t) => `      <li>
        <a href="${t.base}/">${esc(t.meta.title || t.name)}</a>
        <span>${esc(t.meta.description || "")}</span>
      </li>`,
    )
    .join("\n");
  const html = (await readFile(landingSrc, "utf8")).split("%TOOLS%").join(items);
  await writeFile(join(DIST, "index.html"), html);
}

async function fullBuild() {
  await rm(DIST, { recursive: true, force: true });
  const tools = await discoverTools();
  const footerTemplate = await readFooterTemplate();
  for (const t of tools) {
    await esbuild.build(jsOptions(t));
    await copyStatic(t, footerTemplate);
  }
  await copyLib();
  await buildLanding(tools);
  console.log(`[build] done -> dist/ (${tools.map((t) => t.name).join(", ") || "no tools"})`);
}

async function watchBuild() {
  await rm(DIST, { recursive: true, force: true });
  const tools = await discoverTools();
  const footerTemplate = await readFooterTemplate();
  for (const t of tools) {
    await copyStatic(t, footerTemplate);
    const ctx = await esbuild.context({
      ...jsOptions(t),
      plugins: [
        {
          name: "copy-static",
          setup(b) {
            b.onEnd((r) => {
              if (r.errors.length === 0) copyStatic(t, footerTemplate);
            });
          },
        },
      ],
    });
    await ctx.watch();
  }
  await copyLib();
  await buildLanding(tools);

  let timer;
  const refresh = () => {
    clearTimeout(timer);
    timer = setTimeout(async () => {
      for (const t of tools) await copyStatic(t, footerTemplate);
      await copyLib();
      await buildLanding(tools);
      console.log("[build] static refreshed");
    }, 150);
  };
  for (const t of tools) {
    const p = join(t.dir, "public");
    if (existsSync(p)) fsWatch(p, { recursive: true }, refresh);
  }
  if (existsSync(WEB_DIR)) fsWatch(WEB_DIR, { recursive: true }, refresh);
  if (existsSync(LIB_DIR)) fsWatch(LIB_DIR, { recursive: true }, refresh);
  console.log("[build] watching tools/, web/, and lib/ …");
}

if (watch) await watchBuild();
else await fullBuild();
