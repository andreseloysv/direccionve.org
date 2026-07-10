const CACHE_NAME = 'direccionve-v2';
const CORE_ASSETS = [
  '/app.html',
  '/direccionve.js',
  '/manifest.json',
  '/icon.svg',
  '/icon-192.png',
  '/icon-512.png',
  '/data-houses.json',
  '/data-streets.json',
  '/data-centers.json'
];

// Install: cache core assets
self.addEventListener('install', function (e) {
  e.waitUntil(
    caches.open(CACHE_NAME).then(function (cache) {
      return cache.addAll(CORE_ASSETS);
    })
  );
  self.skipWaiting();
});

// Activate: clean old caches
self.addEventListener('activate', function (e) {
  e.waitUntil(
    caches.keys().then(function (keys) {
      return Promise.all(
        keys.filter(function (k) { return k !== CACHE_NAME; })
            .map(function (k) { return caches.delete(k); })
      );
    })
  );
  self.clients.claim();
});

// Fetch: network-first for JSON (fresh data), cache-first for static assets
self.addEventListener('fetch', function (e) {
  const url = new URL(e.request.url);

  // Skip non-GET and cross-origin (except tile CDN)
  if (e.request.method !== 'GET') return;
  if (url.origin !== self.location.origin && !url.hostname.includes('basemaps.cartocdn.com')) return;

  // Tile images: cache-first (they rarely change)
  if (url.hostname.includes('basemaps.cartocdn.com')) {
    e.respondWith(
      caches.match(e.request).then(function (cached) {
        if (cached) return cached;
        return fetch(e.request).then(function (response) {
          if (response.ok) {
            const clone = response.clone();
            caches.open(CACHE_NAME).then(function (cache) { cache.put(e.request, clone); });
          }
          return response;
        });
      })
    );
    return;
  }

  // JSON data: network-first, fall back to cache
  if (url.pathname.endsWith('.json')) {
    e.respondWith(
      fetch(e.request).then(function (response) {
        if (response.ok) {
          const clone = response.clone();
          caches.open(CACHE_NAME).then(function (cache) { cache.put(e.request, clone); });
        }
        return response;
      }).catch(function () {
        return caches.match(e.request);
      })
    );
    return;
  }

  // Static assets: cache-first
  e.respondWith(
    caches.match(e.request).then(function (cached) {
      return cached || fetch(e.request).then(function (response) {
        if (response.ok && url.origin === self.location.origin) {
          const clone = response.clone();
          caches.open(CACHE_NAME).then(function (cache) { cache.put(e.request, clone); });
        }
        return response;
      });
    })
  );
});
