#!/usr/bin/env python3
"""Dev/self-host server for the sheet-music scanner.

A plain static server works too, but this one adds:

  - COOP/COEP headers, so the page is cross-origin isolated and ONNX Runtime
    Web can use multi-threaded WebAssembly (noticeably faster recognition).
    COEP uses `credentialless` so CDN-mode still works in Chromium/Firefox;
    Safari falls back to single-threaded inference.
  - Correct MIME types for .wasm/.mjs/.whl.
  - Cache-friendly headers for the large vendored assets.

Usage: python3 serve.py [port]   (default 8000)
"""

import sys
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer


class Handler(SimpleHTTPRequestHandler):
    extensions_map = {
        **SimpleHTTPRequestHandler.extensions_map,
        ".wasm": "application/wasm",
        ".mjs": "text/javascript",
        ".whl": "application/octet-stream",
        ".onnx": "application/octet-stream",
        ".musicxml": "application/vnd.recordare.musicxml+xml",
    }

    def end_headers(self):
        self.send_header("Cross-Origin-Opener-Policy", "same-origin")
        self.send_header("Cross-Origin-Embedder-Policy", "credentialless")
        self.send_header("Cross-Origin-Resource-Policy", "cross-origin")
        if self.path.startswith("/vendor/"):
            self.send_header("Cache-Control", "public, max-age=86400")
        super().end_headers()


def main():
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8000
    server = ThreadingHTTPServer(("0.0.0.0", port), Handler)
    print(f"Serving on http://localhost:{port}")
    server.serve_forever()


if __name__ == "__main__":
    main()
