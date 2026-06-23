const CACHE_NAME = 'beanbook-v1';
const OFFLINE_ASSETS = ['/offline.html', '/icons/icon-192.svg'];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(OFFLINE_ASSETS))
  );
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(self.clients.claim());
});

self.addEventListener('fetch', (event) => {
  // Only intercept top-level navigation requests.
  // Vaadin WebSocket, XHR, and push traffic must always reach the live server.
  if (event.request.mode !== 'navigate') return;

  event.respondWith(
    fetch(event.request).catch(() => caches.match('/offline.html'))
  );
});
