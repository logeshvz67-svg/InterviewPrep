/* Code Quest service worker: makes the app installable and the shell load offline.
   API calls are NEVER cached (your data always comes fresh from the server). */
const CACHE = 'cq-shell-v1'
const SHELL = ['/', '/index.html', '/manifest.webmanifest', '/icon.svg', '/icon-192.png', '/icon-512.png']

self.addEventListener('install', (e) => {
  e.waitUntil(caches.open(CACHE).then((c) => c.addAll(SHELL)).then(() => self.skipWaiting()))
})

self.addEventListener('activate', (e) => {
  e.waitUntil(caches.keys().then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k)))).then(() => self.clients.claim()))
})

self.addEventListener('fetch', (e) => {
  const req = e.request
  const url = new URL(req.url)
  if (req.method !== 'GET' || url.origin !== self.location.origin) return // API lives on another origin: untouched
  if (req.mode === 'navigate') {
    // Network first for pages, fall back to the cached shell when offline.
    e.respondWith(fetch(req).catch(() => caches.match('/index.html')))
    return
  }
  if (url.pathname.startsWith('/assets/')) {
    // Hashed build files never change: cache first.
    e.respondWith(caches.match(req).then((hit) => hit || fetch(req).then((res) => {
      const copy = res.clone()
      caches.open(CACHE).then((c) => c.put(req, copy))
      return res
    })))
  }
})

self.addEventListener('notificationclick', (e) => {
  e.notification.close()
  e.waitUntil(self.clients.matchAll({ type: 'window' }).then((list) => list.length ? list[0].focus() : self.clients.openWindow('/')))
})
