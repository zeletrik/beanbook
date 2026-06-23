const CACHE_NAME = 'beanbook-v1';
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
  // Only intercept top-level navigation requests.
  // Vaadin WebSocket, XHR, and push traffic must always reach the live server.
  if (event.request.mode !== 'navigate') return;

  event.respondWith((async () => {
    try {
      return await fetch(event.request);
    } catch (e) {
      // Network failed — serve the cached offline page, or a last-resort 503 if it is missing.
      const cached = await caches.match(OFFLINE_URL);
      return cached || new Response('Offline', {
        status: 503,
        headers: { 'Content-Type': 'text/plain' },
      });
    }
  })());
});
