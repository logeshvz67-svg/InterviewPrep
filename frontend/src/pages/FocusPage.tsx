import { useCallback, useEffect, useMemo, useState } from 'react'
import { api } from '../api/client'
import type { CommitmentView, DistractionInsights, FocusCompleted, FocusStats, FocusView, ProfileView, XpAward } from '../api/types'
import { ErrorBox, Section, Spinner, XpToast } from '../components/ui'
import { errorMessage, useApi } from '../lib/hooks'
import { ding } from '../lib/sound'

const DISTRACTIONS = [
  ['PHONE', '📱 Phone'], ['SOCIAL_MEDIA', '💬 Social'], ['VIDEO', '📺 Video'], ['MESSAGES', '✉️ Messages'], ['NOISE', '🔊 Noise'],
  ['TIRED', '😴 Tired'], ['HUNGRY', '🍜 Hungry'], ['OVERTHINKING', '🌀 Overthinking'], ['WORK', '💼 Work'], ['OTHER', '❓ Other'],
] as const

export default function FocusPage() {
  const active = useApi<FocusView | undefined>('/api/focus/active')
  const profile = useApi<ProfileView>('/api/profile')
  const [award, setAward] = useState<XpAward | null>(null)
  const clearAward = useCallback(() => setAward(null), [])

  return (
    <div className="space-y-8">
      <h1 className="text-2xl font-bold">⏱️ Focus</h1>
      {active.loading && active.data === null ? <Spinner /> : active.error ? <ErrorBox message={active.error} onRetry={active.reload} /> : (
        <Timer session={active.data ?? null} sound={profile.data?.soundEnabled ?? true}
          onChanged={(a) => { if (a) setAward(a); void active.reload() }} />
      )}
      <DistractionLog />
      <Commitments onAward={setAward} />
      <StatsBlock />
      <XpToast award={award} onDone={clearAward} />
    </div>
  )
}

function Timer({ session, sound, onChanged }: { session: FocusView | null; sound: boolean; onChanged: (a?: XpAward) => void }) {
  const [topic, setTopic] = useState('')
  const [minutes, setMinutes] = useState(25)
  const [busy, setBusy] = useState(false)
  const [msg, setMsg] = useState<string | null>(null)
  const [now, setNow] = useState(Date.now())
  const [breakEnds, setBreakEnds] = useState<number | null>(null)

  // Correct for a wrong device clock using the server's time.
  const skew = useMemo(() => (session ? new Date(session.serverNow).getTime() - Date.now() : 0), [session])
  useEffect(() => { const t = setInterval(() => setNow(Date.now()), 1000); return () => clearInterval(t) }, [])

  const remainingMs = session ? new Date(session.endsAt).getTime() - (now + skew) : 0
  const finished = session != null && remainingMs <= 0

  useEffect(() => {
    if (!finished) return
    ding(sound)
    if ('Notification' in window && Notification.permission === 'granted') new Notification('Focus session done! Tap Complete to claim it.')
  }, [finished, sound])

  async function start(mode: 'POMODORO' | 'MVD') {
    setBusy(true); setMsg(null)
    try {
      if ('Notification' in window && Notification.permission === 'default') void Notification.requestPermission()
      await api('/api/focus/start', { method: 'POST', body: { mode, topic: topic.trim() || (mode === 'MVD' ? 'Minimum Viable Day' : 'General study'), plannedMinutes: mode === 'MVD' ? 20 : minutes } })
      onChanged()
    } catch (e) { setMsg(errorMessage(e)) } finally { setBusy(false) }
  }

  async function complete() {
    if (!session) return
    setBusy(true); setMsg(null)
    try {
      const r = await api<FocusCompleted>(`/api/focus/${session.id}/complete`, { method: 'POST' })
      setBreakEnds(Date.now() + 5 * 60_000)
      onChanged(r.xp)
    } catch (e) { setMsg(errorMessage(e)) } finally { setBusy(false) }
  }

  async function cancel() {
    if (!session || !confirm('Stop this session? It won\'t count, and that\'s okay.')) return
    await api(`/api/focus/${session.id}/cancel`, { method: 'POST' }).catch(() => undefined)
    onChanged()
  }

  const fmt = (ms: number) => { const s = Math.max(0, Math.round(ms / 1000)); return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}` }

  if (session) {
    const total = session.plannedMinutes * 60_000
    const pct = Math.min(100, Math.max(0, 100 - (remainingMs / total) * 100))
    return (
      <div className="card text-center space-y-4">
        <p className="text-sm text-muted">{session.mode === 'MVD' ? '⚡ Minimum Viable Day' : '🍅 Pomodoro'} · {session.topic}</p>
        <div className="relative mx-auto w-56 h-56">
          <svg viewBox="0 0 100 100" className="w-full h-full -rotate-90" aria-hidden>
            <circle cx="50" cy="50" r="45" className="stroke-raised fill-none" strokeWidth="8" />
            <circle cx="50" cy="50" r="45" className="stroke-primary fill-none transition-all" strokeWidth="8" strokeLinecap="round"
              strokeDasharray={2 * Math.PI * 45} strokeDashoffset={(1 - pct / 100) * 2 * Math.PI * 45} />
          </svg>
          <div className="absolute inset-0 grid place-items-center text-5xl font-mono font-semibold" aria-live="polite">{finished ? '🎉' : fmt(remainingMs)}</div>
        </div>
        <p className="text-xs text-muted">Safe to close this tab: the timer is saved on the server.</p>
        {msg && <p className="text-bad text-sm">{msg}</p>}
        <div className="flex justify-center gap-2">
          <button className="btn-primary" disabled={!finished || busy} onClick={complete}>Complete ✓</button>
          <button className="btn-ghost" onClick={cancel}>Stop</button>
        </div>
      </div>
    )
  }

  return (
    <div className="card space-y-4">
      {breakEnds && breakEnds > now && (
        <div className="rounded-xl bg-accent/15 text-accent p-3 text-center">☕ Break time: {fmt(breakEnds - now)}. Stand up, drink water, look far away.</div>
      )}
      <div>
        <label className="label" htmlFor="topic">What will you focus on?</label>
        <input id="topic" className="input" value={topic} onChange={(e) => setTopic(e.target.value)} maxLength={120} placeholder="e.g. Java Strings, Two Sum, OOP" />
      </div>
      <div className="flex flex-wrap gap-2 items-center">
        {[25, 45, 50].map((m) => (
          <button key={m} className={`chip px-3 py-1.5 ${minutes === m ? 'chip-on' : ''}`} onClick={() => setMinutes(m)}>{m} min</button>
        ))}
        <span className="text-xs text-muted">25+ min sessions earn +10 XP</span>
      </div>
      {msg && <p className="text-bad text-sm">{msg}</p>}
      <div className="grid sm:grid-cols-2 gap-2">
        <button className="btn-primary" disabled={busy} onClick={() => start('POMODORO')}>🍅 Start {minutes}-minute focus</button>
        <button className="btn-ghost" disabled={busy} onClick={() => start('MVD')}>⚡ Minimum Viable Day (20 min)</button>
      </div>
      <p className="text-xs text-muted">Low-energy day? The Minimum Viable Day keeps your streak alive. Showing up is the win.</p>
    </div>
  )
}

function DistractionLog() {
  const insights = useApi<DistractionInsights>('/api/distractions/insights?days=7')
  const [note, setNote] = useState('')
  const [saved, setSaved] = useState<string | null>(null)

  async function log(category: string) {
    await api('/api/distractions', { method: 'POST', body: { category, note: note.trim() || null } }).catch(() => undefined)
    setNote('')
    setSaved('Noted. Now back to it 💪')
    setTimeout(() => setSaved(null), 2000)
    void insights.reload()
  }

  const d = insights.data
  return (
    <Section title="Got distracted?">
      <div className="card space-y-3">
        <p className="text-sm text-muted">Tap what pulled you away. No judgement: patterns help you fix it.</p>
        <input className="input" value={note} onChange={(e) => setNote(e.target.value)} maxLength={300} placeholder="Optional note" />
        <div className="flex flex-wrap gap-2">
          {DISTRACTIONS.map(([code, label]) => <button key={code} className="chip px-3 py-1.5 hover:bg-line" onClick={() => log(code)}>{label}</button>)}
        </div>
        {saved && <p className="text-good text-sm">{saved}</p>}
        {d && d.total > 0 && (
          <div className="text-sm border-t border-line/60 pt-3 space-y-1">
            <p><b>This week:</b> {d.total} distractions. Top: {d.topCategory?.toLowerCase().replace('_', ' ')}{d.worstHour != null ? `, mostly around ${d.worstHour}:00` : ''}.</p>
            <p className="text-muted">💡 {d.tip}</p>
          </div>
        )}
      </div>
    </Section>
  )
}

function Commitments({ onAward }: { onAward: (a: XpAward) => void }) {
  const today = useApi<CommitmentView>('/api/commitments/today')
  const tomorrow = useApi<CommitmentView>('/api/commitments/tomorrow')
  const [tasks, setTasks] = useState(['', '', ''])
  const [msg, setMsg] = useState<string | null>(null)

  useEffect(() => {
    if (tomorrow.data?.tasks.length) setTasks([0, 1, 2].map((i) => tomorrow.data!.tasks[i]?.text ?? ''))
  }, [tomorrow.data])

  async function toggle(i: number) {
    try { today.setData(await api<CommitmentView>(`/api/commitments/today/tasks/${i}/toggle`, { method: 'PATCH' })) } catch { /* ignore */ }
  }

  async function save() {
    const list = tasks.map((t) => t.trim()).filter(Boolean)
    if (!list.length) { setMsg('Write at least one task'); return }
    try {
      const r = await api<{ commitment: CommitmentView; xp: XpAward }>('/api/commitments/tomorrow', { method: 'PUT', body: { tasks: list.map((text) => ({ text, done: false })) } })
      tomorrow.setData(r.commitment)
      onAward(r.xp)
      setMsg('Saved! Future you says thanks.')
    } catch (e) { setMsg(errorMessage(e)) }
  }

  return (
    <Section title="Daily commitment">
      <div className="grid md:grid-cols-2 gap-4">
        <div className="card space-y-2">
          <h3 className="font-semibold">Today's 3 tasks</h3>
          {today.data?.tasks.length ? today.data.tasks.map((t, i) => (
            <label key={i} className="flex items-center gap-2 cursor-pointer">
              <input type="checkbox" checked={t.done} onChange={() => toggle(i)} className="h-5 w-5 accent-[rgb(var(--c-primary))]" />
              <span className={t.done ? 'line-through text-muted' : ''}>{t.text}</span>
            </label>
          )) : <p className="text-sm text-muted">You didn't pick tasks yesterday. That's fine: plan tomorrow below.</p>}
        </div>
        <div className="card space-y-2">
          <h3 className="font-semibold">Pick tomorrow's 3 tasks <span className="text-xs text-gold">+10 XP</span></h3>
          {tasks.map((t, i) => (
            <input key={i} className="input" value={t} maxLength={160} placeholder={`Task ${i + 1}`}
              onChange={(e) => setTasks(tasks.map((x, j) => (j === i ? e.target.value : x)))} />
          ))}
          <button className="btn-primary w-full" onClick={save}>Save tomorrow's plan</button>
          {msg && <p className="text-sm text-muted">{msg}</p>}
        </div>
      </div>
    </Section>
  )
}

function StatsBlock() {
  const { data } = useApi<FocusStats>('/api/focus/stats?days=7')
  if (!data || data.totalMinutes === 0) return null
  const max = Math.max(...Object.values(data.minutesByTopic))
  return (
    <Section title={`Last 7 days: ${Math.round(data.totalMinutes / 6) / 10} hours focused`}>
      <div className="card space-y-2">
        {Object.entries(data.minutesByTopic).sort((a, b) => b[1] - a[1]).map(([topic, m]) => (
          <div key={topic} className="text-sm">
            <div className="flex justify-between"><span>{topic}</span><span className="text-muted">{m} min</span></div>
            <div className="h-2 rounded bg-raised"><div className="h-full rounded bg-primary" style={{ width: `${(m / max) * 100}%` }} /></div>
          </div>
        ))}
      </div>
    </Section>
  )
}
