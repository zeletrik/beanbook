const CACHE_NAME = 'beanbook-v2';
const OFFLINE_URL = '/offline.html';
const OPTIONAL_ASSETS = ['/icons/icon-192.svg'];

self.addEventListener('install', (event) => {
  event.waitUntil((async () => {
    const cache = await caches.open(CACHE_NAME);
    // The offline fallback is required: fail the install if it can't be cached.
    await cache.add(OFFLINE_URL);
    // Optional assets must not abort the install if one is missing.
    await Promise.allSettled(OPTIONAL_ASSETS.map((url) => cache.add(url)));
  })());
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil((async () => {
    // Drop caches left over from previous versions.
    const keys = await caches.keys();
    await Promise.all(keys.filter((key) => key !== CACHE_NAME).map((key) => caches.delete(key)));
    await self.clients.claim();
  })());
});

self.addEventListener('fetch', (event) => {
  const { request } = event;

  // Top-level navigations: try the network, fall back to the cached offline page.
  // Vaadin WebSocket, XHR, and push traffic are NOT navigations, so they pass straight through.
  if (request.mode === 'navigate') {
    event.respondWith((async () => {
      try {
        return await fetch(request);
      } catch (e) {
        const cached = await caches.match(OFFLINE_URL);
        return cached || new Response('Offline', {
          status: 503,
          headers: { 'Content-Type': 'text/plain' },
        });
      }
    })());
    return;
  }

  // Precached static assets (the offline page's logo) are served cache-first so they also render
  // when offline. Scoped to the exact precached same-origin URLs — every other request, including
  // all of Vaadin's dynamic traffic, is left untouched and goes to the network as normal.
  const url = new URL(request.url);
  if (
    request.method === 'GET' &&
    url.origin === self.location.origin &&
    OPTIONAL_ASSETS.includes(url.pathname)
  ) {
    event.respondWith((async () => (await caches.match(request)) || fetch(request))());
  }
});
