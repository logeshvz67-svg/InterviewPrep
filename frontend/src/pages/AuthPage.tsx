import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { errorMessage } from '../lib/hooks'
import { game } from '../theme/gameConfig'

export default function AuthPage() {
  const { login, register } = useAuth()
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [name, setName] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [canRegister, setCanRegister] = useState(true)

  useEffect(() => {
    api<{ registrationEnabled: boolean }>('/api/auth/config').then((c) => setCanRegister(c.registrationEnabled)).catch(() => undefined)
  }, [])

  async function submit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    if (mode === 'register' && password.length < 8) { setError('Password must be at least 8 characters'); return }
    setBusy(true)
    try {
      if (mode === 'login') await login(email, password)
      else await register(email, password, name)
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="min-h-screen grid place-items-center p-4">
      <div className="w-full max-w-sm space-y-6">
        <header className="text-center space-y-1">
          <div className="text-5xl" aria-hidden>🏰</div>
          <h1 className="text-3xl font-bold">{game.title}</h1>
          <p className="text-muted">{game.tagline}</p>
        </header>
        <form onSubmit={submit} className="card space-y-4" noValidate>
          {mode === 'register' && (
            <div>
              <label className="label" htmlFor="name">Your name</label>
              <input id="name" className="input" value={name} onChange={(e) => setName(e.target.value)} required maxLength={80} autoComplete="name" />
            </div>
          )}
          <div>
            <label className="label" htmlFor="email">Email</label>
            <input id="email" type="email" className="input" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email" />
          </div>
          <div>
            <label className="label" htmlFor="pw">Password</label>
            <input id="pw" type="password" className="input" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={8}
              autoComplete={mode === 'login' ? 'current-password' : 'new-password'} />
            {mode === 'register' && <p className="text-xs text-muted mt-1">At least 8 characters. Stored as a BCrypt hash, never as text.</p>}
          </div>
          {error && <p className="text-bad text-sm whitespace-pre-line" role="alert">{error}</p>}
          <button className="btn-primary w-full" disabled={busy}>{busy ? 'Please wait…' : mode === 'login' ? 'Log in' : 'Create account'}</button>
        </form>
        {canRegister && (
          <p className="text-center text-sm text-muted">
            {mode === 'login' ? 'New here?' : 'Already have an account?'}{' '}
            <button className="text-primary font-semibold" onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setError(null) }}>
              {mode === 'login' ? 'Create an account' : 'Log in'}
            </button>
          </p>
        )}
      </div>
    </main>
  )
}
