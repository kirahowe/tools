/// <reference types="@cloudflare/workers-types" />

// Worker entry point for the tools site.
//
// Static files built into dist/ are served directly by Cloudflare's asset layer
// (see the [assets] block in wrangler.toml). Any request that does not match a
// built file falls through to the fetch handler at the bottom of this file. The
// dynamic routes today are the annotate tool's page fetcher and the feeds
// tool's feed fetcher, both of which fetch a third-party URL on the browser's
// behalf.
//
// To add an endpoint for a new tool, add a branch to the router in `fetch`,
// mounted under /<tool>/api/… to mirror how the frontend calls ${__BASE__}/api/….
// If the endpoint needs a server-side fetch like the ones below, reach for
// `fetchUpstream` — it already handles validation, the SSRF guard, timeouts,
// and byte caps — rather than copy-pasting a handler.

interface Env {}

const MAX_BYTES = 8 * 1024 * 1024; // 8 MB cap
const FETCH_TIMEOUT_MS = 20_000;

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      // Same-origin in normal use; permissive so a self-hosted copy still works.
      "access-control-allow-origin": "*",
      "cache-control": "no-store",
    },
  });
}

/** Reject obvious private / internal literal addresses (basic SSRF guard). */
function isBlockedHost(hostname: string): boolean {
  const host = hostname.replace(/^\[|\]$/g, "").toLowerCase();
  if (host === "localhost" || host.endsWith(".localhost")) return true;
  if (host === "0.0.0.0" || host === "::1" || host === "::") return true;
  // IPv4 literal in a private / loopback / link-local range.
  const m = host.match(/^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$/);
  if (m) {
    const [a, b] = [Number(m[1]), Number(m[2])];
    if (a === 10 || a === 127 || a === 0) return true;
    if (a === 169 && b === 254) return true; // link-local / cloud metadata
    if (a === 172 && b >= 16 && b <= 31) return true;
    if (a === 192 && b === 168) return true;
    if (a === 100 && b >= 64 && b <= 127) return true; // CGNAT
  }
  // IPv6 unique-local / link-local literal.
  if (host.startsWith("fc") || host.startsWith("fd") || host.startsWith("fe80")) {
    return true;
  }
  return false;
}

interface UpstreamOptions {
  userAgent: string;
  accept: string;
  acceptLanguage?: string;
  /**
   * If set, the response content-type must match `pattern`, else 415; `expected`
   * names what the caller wanted ("an HTML page") for the error message. Omit to
   * accept whatever the server sends.
   */
  contentTypes?: { pattern: RegExp; expected: string };
  /** Noun used in user-facing error messages: "page", "feed". */
  noun: string;
}

type UpstreamResult =
  | { ok: false; response: Response }
  | { ok: true; text: string; finalUrl: string; contentType: string; status: number };

/**
 * Shared guard rail for every server-side fetch this worker makes: parse and
 * validate the target URL, apply the SSRF host guard, bound the request by
 * time and by bytes, and decode the body to text. Callers supply the request
 * headers (and, optionally, a content-type allowlist); everything else —
 * timeouts, size caps, status codes — is identical across callers, so it
 * lives here once instead of drifting copy to copy.
 */
async function fetchUpstream(request: Request, options: UpstreamOptions): Promise<UpstreamResult> {
  const reqUrl = new URL(request.url);
  const target = reqUrl.searchParams.get("url");
  if (!target) return { ok: false, response: json({ error: "Missing ?url= parameter." }, 400) };

  let parsed: URL;
  try {
    parsed = new URL(target);
  } catch {
    return { ok: false, response: json({ error: "That does not look like a valid URL." }, 400) };
  }
  if (parsed.protocol !== "http:" && parsed.protocol !== "https:") {
    return { ok: false, response: json({ error: "Only http and https URLs are supported." }, 400) };
  }
  if (isBlockedHost(parsed.hostname)) {
    return { ok: false, response: json({ error: "Refusing to fetch a private or internal address." }, 403) };
  }

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), FETCH_TIMEOUT_MS);
  let upstream: Response;
  try {
    upstream = await fetch(parsed.toString(), {
      redirect: "follow",
      signal: controller.signal,
      headers: {
        "user-agent": options.userAgent,
        accept: options.accept,
        ...(options.acceptLanguage ? { "accept-language": options.acceptLanguage } : {}),
      },
    });
  } catch (err) {
    clearTimeout(timer);
    const aborted = err instanceof Error && err.name === "AbortError";
    return {
      ok: false,
      response: json(
        { error: aborted ? `The ${options.noun} took too long to load.` : "Could not fetch that URL." },
        502,
      ),
    };
  }
  clearTimeout(timer);

  if (!upstream.ok) {
    return { ok: false, response: json({ error: `That URL returned HTTP ${upstream.status}.` }, 502) };
  }

  // content-length is only a hint — it can be absent or wrong — so it's just
  // an early bailout; the streaming cap below is what actually enforces it.
  const contentLength = Number(upstream.headers.get("content-length"));
  if (Number.isFinite(contentLength) && contentLength > MAX_BYTES) {
    return {
      ok: false,
      response: json({ error: `That ${options.noun} is too large to load (over 8 MB).` }, 413),
    };
  }

  const contentType = upstream.headers.get("content-type") ?? "";
  if (options.contentTypes && !options.contentTypes.pattern.test(contentType)) {
    return {
      ok: false,
      response: json(
        {
          error: `That URL returned "${contentType || "unknown content"}", not ${options.contentTypes.expected}.`,
        },
        415,
      ),
    };
  }

  // Read with a hard byte cap so a huge page can't blow the memory/time budget.
  const reader = upstream.body?.getReader();
  if (!reader) return { ok: false, response: json({ error: "Empty response from that URL." }, 502) };
  const chunks: Uint8Array[] = [];
  let total = 0;
  for (;;) {
    const { done, value } = await reader.read();
    if (done) break;
    if (value) {
      total += value.length;
      if (total > MAX_BYTES) {
        await reader.cancel();
        return {
          ok: false,
          response: json({ error: `That ${options.noun} is too large to load (over 8 MB).` }, 413),
        };
      }
      chunks.push(value);
    }
  }
  const merged = new Uint8Array(total);
  let offset = 0;
  for (const chunk of chunks) {
    merged.set(chunk, offset);
    offset += chunk.length;
  }

  const charset = contentType.match(/charset=([^;]+)/i)?.[1]?.trim();
  let text: string;
  try {
    text = new TextDecoder(charset || "utf-8").decode(merged);
  } catch {
    text = new TextDecoder("utf-8").decode(merged);
  }

  return {
    ok: true,
    text,
    finalUrl: upstream.url || parsed.toString(),
    contentType,
    status: upstream.status,
  };
}

/**
 * GET /annotate/api/fetch?url=<encoded>
 *
 * Fetches a public web page server-side (on Cloudflare's edge) and returns its
 * HTML to the browser, which is otherwise blocked from cross-origin fetches by
 * CORS. This is the only server-side piece of the app; it runs on-demand and
 * scales to zero — there is no server process to keep running.
 */
async function fetchPage(request: Request): Promise<Response> {
  const result = await fetchUpstream(request, {
    userAgent:
      "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 " +
      "(KHTML, like Gecko) Chrome/124.0 Safari/537.36",
    accept: "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    acceptLanguage: "en-US,en;q=0.9",
    contentTypes: {
      pattern: /text\/html|application\/xhtml|text\/plain|application\/xml/i,
      expected: "an HTML page",
    },
    noun: "page",
  });
  if (!result.ok) return result.response;
  return json({
    finalUrl: result.finalUrl,
    contentType: result.contentType,
    status: result.status,
    html: result.text,
  });
}

/** Fetch a feed document for the browser, where cross-origin CORS often prevents a direct request. */
async function fetchFeed(request: Request): Promise<Response> {
  const result = await fetchUpstream(request, {
    userAgent: "tools.kirahowe.com Feed Preview",
    accept:
      "application/atom+xml,application/rss+xml,application/xml;q=0.9,text/xml;q=0.9,text/plain;q=0.5,*/*;q=0.1",
    // No contentTypes here — deliberately. Feeds are served under a zoo of
    // content types (application/atom+xml, application/rss+xml, text/xml,
    // application/xml, sometimes text/plain or a plain text/html from a
    // misconfigured host), and the client's XML parser already produces a
    // better error than a content-type guess would.
    noun: "feed",
  });
  if (!result.ok) return result.response;
  return json({ xml: result.text, finalUrl: result.finalUrl });
}

export default {
  async fetch(request: Request, _env: Env, _ctx: ExecutionContext): Promise<Response> {
    const { pathname } = new URL(request.url);

    if (pathname === "/annotate/api/fetch") {
      if (request.method !== "GET") return json({ error: "Method not allowed." }, 405);
      return fetchPage(request);
    }
    if (pathname === "/feeds/api/fetch") {
      if (request.method !== "GET") return json({ error: "Method not allowed." }, 405);
      return fetchFeed(request);
    }

    // Reached only when no static asset matched the request path.
    return new Response("Not found", { status: 404 });
  },
} satisfies ExportedHandler<Env>;
