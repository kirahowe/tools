// Service worker: makes the app installable and usable offline.
//
// It caches the app shell (HTML/CSS/JS/icons) so the annotator loads without a
// network, and — because documents and notes are stored locally — you can keep
// annotating cached documents offline too. It deliberately never touches the
// /api/ proxy or cross-origin requests.

const CACHE = "annotate-for-llm-v1";
const SHELL = [
  "/",
  "/index.html",
  "/styles.css",
  "/app.js",
  "/manifest.webmanifest",
  "/icons/icon.svg",
  "/icons/icon-192.png",
  "/icons/icon-512.png",
];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches
      .open(CACHE)
      // add() each item individually so one 404 doesn't fail the whole install.
      .then((cache) => Promise.allSettled(SHELL.map((url) => cache.add(url))))
      .then(() => self.skipWaiting()),
  );
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim()),
  );
});

self.addEventListener("fetch", (event) => {
  const { request } = event;
  if (request.method !== "GET") return;
  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return; // leave cross-origin alone
  if (url.pathname.startsWith("/api/")) return; // never cache the fetch proxy

  // Navigations: network-first, falling back to the cached shell when offline.
  if (request.mode === "navigate") {
    event.respondWith(
      fetch(request)
        .then((res) => {
          cachePut(request, res.clone());
          return res;
        })
        .catch(() => caches.match(request).then((c) => c || caches.match("/index.html"))),
    );
    return;
  }

  // Static assets: serve cached immediately, refresh in the background.
  event.respondWith(
    caches.match(request).then((cached) => {
      const network = fetch(request)
        .then((res) => {
          cachePut(request, res.clone());
          return res;
        })
        .catch(() => cached);
      return cached || network;
    }),
  );
});

function cachePut(request, response) {
  if (response && response.ok && response.type === "basic") {
    caches.open(CACHE).then((cache) => cache.put(request, response));
  }
}
