// Bundles the browser frontend (src/app.ts -> public/app.js) with esbuild.
//
//   node build.mjs            one-off build
//   node build.mjs --watch    rebuild on change
//
// The Cloudflare Pages Function in functions/ is bundled by Cloudflare itself
// (and by `wrangler pages dev`), so it is not part of this build.

import * as esbuild from "esbuild";

const watch = process.argv.includes("--watch");

/** @type {import("esbuild").BuildOptions} */
const frontend = {
  entryPoints: ["src/app.ts"],
  outfile: "public/app.js",
  bundle: true,
  format: "iife",
  platform: "browser",
  target: ["es2021"],
  sourcemap: true,
  minify: !watch,
  logLevel: "info",
};

if (watch) {
  const ctx = await esbuild.context(frontend);
  await ctx.watch();
  console.log("[build] watching src/ for changes…");
} else {
  await esbuild.build(frontend);
  console.log("[build] done -> public/app.js");
}
