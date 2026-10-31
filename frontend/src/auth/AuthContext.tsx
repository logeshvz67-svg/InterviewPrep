import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { api, clearTokens, getRefreshToken, hasSession, setAuthListener, setTokens } from '../api/client'
import type { TokenResponse, UserView } from '../api/types'

interface AuthState {
  user: UserView | null
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string, displayName: string) => Promise<void>
  logout: () => Promise<void>
}

const Ctx = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserView | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    setAuthListener(() => setUser(null))
    if (!hasSession()) { setLoading(false); return }
    api<UserView>('/api/auth/me').then(setUser).catch(() => setUser(null)).finally(() => setLoading(false))
  }, [])

  const accept = (t: TokenResponse) => { setTokens(t.accessToken, t.refreshToken); setUser(t.user) }

  const login = useCallback(async (email: string, password: string) => {
    accept(await api<TokenResponse>('/api/auth/login', { method: 'POST', body: { email, password } }))
  }, [])

  const register = useCallback(async (email: string, password: string, displayName: string) => {
    accept(await api<TokenResponse>('/api/auth/register', { method: 'POST', body: { email, password, displayName } }))
  }, [])

  const logout = useCallback(async () => {
    const rt = getRefreshToken()
    if (rt) await api('/api/auth/logout', { method: 'POST', body: { refreshToken: rt } }).catch(() => undefined)
    clearTokens()
    setUser(null)
  }, [])

  return <Ctx.Provider value={{ user, loading, login, register, logout }}>{children}</Ctx.Provider>
}

export function useAuth() {
  const v = useContext(Ctx)
  if (!v) throw new Error('useAuth outside AuthProvider')
  return v
}
