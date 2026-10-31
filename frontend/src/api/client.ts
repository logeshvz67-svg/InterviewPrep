/**
 * Tiny fetch wrapper.
 *  - Access token lives in memory only; refresh token in localStorage (needed because the
 *    frontend and backend are on different onrender.com sub-domains, so cookies would be "third-party").
 *  - On 401 it refreshes once and retries.
 *  - On network errors / 502 / 503 / 504 (free server waking up) it retries with backoff.
 */
export const API_URL = (import.meta.env.VITE_API_URL as string | undefined)?.replace(/\/$/, '') ?? 'http://localhost:8080'

const REFRESH_KEY = 'cq.refresh'
let accessToken: string | null = null
let refreshing: Promise<boolean> | null = null
let onLoggedOut: (() => void) | null = null

export function setAuthListener(fn: () => void) { onLoggedOut = fn }
export function setTokens(access: string, refresh: string) {
  accessToken = access
  try { localStorage.setItem(REFRESH_KEY, refresh) } catch { /* private mode: stay logged in for this tab only */ }
}
export function clearTokens() {
  accessToken = null
  try { localStorage.removeItem(REFRESH_KEY) } catch { /* ignore */ }
}
export function getRefreshToken(): string | null {
  try { return localStorage.getItem(REFRESH_KEY) } catch { return null }
}
export function hasSession() { return !!accessToken || !!getRefreshToken() }

export class ApiError extends Error {
  status: number
  fieldErrors?: Record<string, string>
  constructor(status: number, message: string, fieldErrors?: Record<string, string>) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms))
const WAKE_STATUSES = new Set([502, 503, 504])

async function rawFetch(path: string, init: RequestInit, retries = 3): Promise<Response> {
  let delay = 1500
  for (let attempt = 0; ; attempt++) {
    try {
      const res = await fetch(API_URL + path, init)
      if (WAKE_STATUSES.has(res.status) && attempt < retries) { await sleep(delay); delay *= 2; continue }
      return res
    } catch (e) {
      if (attempt >= retries) throw new ApiError(0, "Can't reach the server. Check your internet, or it may still be waking up.")
      await sleep(delay); delay *= 2
    }
  }
}

async function tryRefresh(): Promise<boolean> {
  const rt = getRefreshToken()
  if (!rt) return false
  refreshing ??= (async () => {
    try {
      const res = await rawFetch('/api/auth/refresh', {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ refreshToken: rt }),
      })
      if (!res.ok) { clearTokens(); return false }
      const data = await res.json()
      setTokens(data.accessToken, data.refreshToken)
      return true
    } finally { setTimeout(() => { refreshing = null }, 0) }
  })()
  return refreshing
}

const PUBLIC_AUTH = ['/api/auth/login', '/api/auth/register', '/api/auth/refresh', '/api/auth/logout', '/api/auth/config']

export async function api<T>(path: string, opts: { method?: string; body?: unknown } = {}): Promise<T> {
  // After a page reload the access token (memory only) is gone: get a new one before calling.
  if (!accessToken && getRefreshToken() && !PUBLIC_AUTH.includes(path)) await tryRefresh()
  const doCall = () => rawFetch(path, {
    method: opts.method ?? 'GET',
    headers: {
      ...(opts.body !== undefined ? { 'Content-Type': 'application/json' } : {}),
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
    },
    body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined,
  })
  let res = await doCall()
  if (res.status === 401 && !path.startsWith('/api/auth/login') && (await tryRefresh())) res = await doCall()
  if (res.status === 401 && !path.startsWith('/api/auth/login')) { clearTokens(); onLoggedOut?.() }
  if (res.status === 204) return undefined as T
  const text = await res.text()
  const data = text ? JSON.parse(text) : undefined
  if (!res.ok) throw new ApiError(res.status, data?.detail ?? `Request failed (${res.status})`, data?.errors)
  return data as T
}

/** Ping /health until the free server is awake. Calls onTick with seconds waited. */
export async function wakeServer(onTick: (s: number) => void, maxSeconds = 180): Promise<boolean> {
  const start = Date.now()
  while ((Date.now() - start) / 1000 < maxSeconds) {
    try {
      const ctl = new AbortController()
      const t = setTimeout(() => ctl.abort(), 10000)
      const res = await fetch(API_URL + '/health', { signal: ctl.signal })
      clearTimeout(t)
      if (res.ok) return true
    } catch { /* still asleep */ }
    onTick(Math.round((Date.now() - start) / 1000))
    await sleep(3000)
  }
  return false
}

/** Download a file from the API (with auth, refresh and retries) and save it in the browser. */
export async function downloadFile(path: string, filename: string): Promise<void> {
  if (!accessToken && getRefreshToken()) await tryRefresh()
  let res = await rawFetch(path, { headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : {} })
  if (res.status === 401 && (await tryRefresh())) res = await rawFetch(path, { headers: { Authorization: `Bearer ${accessToken}` } })
  if (!res.ok) throw new ApiError(res.status, `Download failed (${res.status})`)
  const blob = await res.blob()
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = filename
  a.click()
  setTimeout(() => URL.revokeObjectURL(a.href), 1000)
}
