import { useState, type FormEvent } from 'react'
import { api } from '../../api/client'
import { ErrorBox, Spinner } from '../../components/ui'
import { errorMessage, useApi } from '../../lib/hooks'
import type { BuildLog, LogType, ProjectStatus, ProjectView, XpAward } from './types'

const STATUS: { id: ProjectStatus; label: string }[] = [
  { id: 'NOT_STARTED', label: 'Not started' }, { id: 'IN_PROGRESS', label: 'In progress' }, { id: 'DONE', label: 'Done' },
]
const LOG_TYPES: { id: LogType; label: string; icon: string; tone: string }[] = [
  { id: 'LOG', label: 'Log', icon: '📝', tone: 'border-line' },
  { id: 'BLOCKER', label: 'Blocker', icon: '🧱', tone: 'border-bad/50' },
  { id: 'LEARNED', label: 'Learned', icon: '💡', tone: 'border-good/50' },
]

export default function SpringProjects({ onAward }: { onAward: (a: XpAward) => void }) {
  const list = useApi<ProjectView[]>('/api/spring/projects')
  if (list.loading && !list.data) return <Spinner label="Setting up your projects…" />
  if (list.error) return <ErrorBox message={list.error} onRetry={list.reload} />
  const replace = (p: ProjectView) => list.setData((prev) => (prev ? prev.map((x) => (x.id === p.id ? p : x)) : prev))
  return (
    <div className="space-y-4">
      <p className="text-sm text-muted">Four projects, from small to bigger. Tick a milestone when it really works. Each milestone pays +40 XP the first time.</p>
      <ul className="space-y-4">
        {(list.data ?? []).map((p, i) => <ProjectCard key={p.id} p={p} n={i + 1} onChange={replace} onAward={onAward} />)}
      </ul>
    </div>
  )
}

function ProjectCard({ p, n, onChange, onAward }: { p: ProjectView; n: number; onChange: (p: ProjectView) => void; onAward: (a: XpAward) => void }) {
  const [open, setOpen] = useState(n === 1 || p.status === 'IN_PROGRESS')
  const [err, setErr] = useState<string | null>(null)
  const [busyMs, setBusyMs] = useState<number | null>(null)
  const done = p.milestones.filter((m) => m.done).length
  const percent = p.milestones.length ? Math.round((done * 100) / p.milestones.length) : 0

  const toggle = async (id: number, wasDone: boolean) => {
    setBusyMs(id); setErr(null)
    try {
      const v = await api<ProjectView>(`/api/spring/milestones/${id}/toggle`, { method: 'POST' })
      onChange(v)
      // The API doesn't say if XP was paid this time (it pays once per milestone), so we don't claim an amount.
      if (!wasDone) onAward({ awarded: false, xp: 0, gold: 0, reason: '🎉 Milestone done! +40 XP the first time you finish it.' })
    } catch (e) { setErr(errorMessage(e)) } finally { setBusyMs(null) }
  }

  return (
    <li className="card space-y-3">
      <button className="w-full text-left flex items-start justify-between gap-3" onClick={() => setOpen(!open)} aria-expanded={open}>
        <div className="min-w-0">
          <h3 className="font-semibold">Project {n}: {p.title}</h3>
          <p className="text-xs text-muted">{STATUS.find((s) => s.id === p.status)?.label} · {done}/{p.milestones.length} milestones</p>
        </div>
        <span className="text-lg" aria-hidden>{open ? '−' : '+'}</span>
      </button>
      <div className="h-2 rounded-full bg-raised overflow-hidden" role="progressbar" aria-label={`${p.title} progress`} aria-valuenow={percent} aria-valuemin={0} aria-valuemax={100}>
        <div className="h-full bg-gradient-to-r from-primary to-accent transition-all duration-500" style={{ width: `${percent}%` }} />
      </div>

      {open && (
        <div className="space-y-5">
          <p className="text-sm">{p.description}</p>
          {err && <ErrorBox message={err} />}

          <fieldset className="space-y-2">
            <legend className="font-semibold text-sm mb-1">Milestones</legend>
            {[...p.milestones].sort((a, b) => a.order - b.order).map((m, i) => (
              <label key={m.id} className={`flex items-start gap-3 rounded-xl border px-3 py-2 text-sm cursor-pointer ${m.done ? 'border-good/50 bg-good/10' : 'border-line bg-raised'}`}>
                <input type="checkbox" className="mt-1 accent-primary" checked={m.done} disabled={busyMs === m.id} onChange={() => void toggle(m.id, m.done)} />
                <span className={m.done ? 'line-through decoration-muted/60' : ''}><b className="text-muted mr-1">{i + 1}.</b>{m.title}</span>
              </label>
            ))}
          </fieldset>

          <ProjectSettings p={p} onChange={onChange} />
          <BuildLogView p={p} onChange={onChange} />
        </div>
      )}
    </li>
  )
}

function ProjectSettings({ p, onChange }: { p: ProjectView; onChange: (p: ProjectView) => void }) {
  const [url, setUrl] = useState(p.githubUrl ?? '')
  const [status, setStatus] = useState<ProjectStatus>(p.status)
  const [busy, setBusy] = useState(false)
  const [msg, setMsg] = useState<{ ok: boolean; text: string } | null>(null)

  const save = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true); setMsg(null)
    try {
      const v = await api<ProjectView>(`/api/spring/projects/${p.id}`, { method: 'PUT', body: { githubUrl: url.trim() || null, status } })
      onChange(v); setMsg({ ok: true, text: 'Saved.' })
    } catch (err) { setMsg({ ok: false, text: errorMessage(err) }) } finally { setBusy(false) }
  }

  return (
    <form onSubmit={save} className="grid gap-3 sm:grid-cols-[1fr_auto_auto] items-end">
      <div>
        <label className="label" htmlFor={`gh-${p.id}`}>GitHub link</label>
        <input id={`gh-${p.id}`} className="input" type="url" maxLength={300} placeholder="https://github.com/you/project" value={url} onChange={(e) => setUrl(e.target.value)} />
      </div>
      <div>
        <label className="label" htmlFor={`st-${p.id}`}>Status</label>
        <select id={`st-${p.id}`} className="input" value={status} onChange={(e) => setStatus(e.target.value as ProjectStatus)}>
          {STATUS.map((s) => <option key={s.id} value={s.id}>{s.label}</option>)}
        </select>
      </div>
      <button className="btn-ghost" disabled={busy}>{busy ? 'Saving…' : 'Save'}</button>
      {msg && <p className={`text-sm sm:col-span-3 ${msg.ok ? 'text-good' : 'text-bad'}`} role="status">{msg.text}</p>}
      {p.githubUrl && <a href={p.githubUrl} target="_blank" rel="noopener noreferrer" className="text-sm text-primary underline sm:col-span-3 break-all">{p.githubUrl} ↗</a>}
    </form>
  )
}

function BuildLogView({ p, onChange }: { p: ProjectView; onChange: (p: ProjectView) => void }) {
  const [type, setType] = useState<LogType>('LOG')
  const [text, setText] = useState('')
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState<string | null>(null)
  const [logs, setLogs] = useState<BuildLog[]>(p.logs)
  const [prevLogs, setPrevLogs] = useState(p.logs)
  if (prevLogs !== p.logs) { setPrevLogs(p.logs); setLogs(p.logs) }

  const add = async (e: FormEvent) => {
    e.preventDefault()
    if (!text.trim()) return
    setBusy(true); setErr(null)
    try {
      const v = await api<ProjectView>(`/api/spring/projects/${p.id}/logs`, { method: 'POST', body: { entryType: type, text: text.trim() } })
      onChange(v); setText('')
    } catch (e2) { setErr(errorMessage(e2)) } finally { setBusy(false) }
  }

  const resolve = async (l: BuildLog) => {
    setErr(null)
    try {
      await api<void>(`/api/spring/logs/${l.id}/resolved?value=${!l.resolved}`, { method: 'PATCH' })
      setLogs((ls) => ls.map((x) => (x.id === l.id ? { ...x, resolved: !l.resolved } : x)))
    } catch (e) { setErr(errorMessage(e)) }
  }

  return (
    <section className="space-y-3" aria-label="Build log">
      <h4 className="font-semibold text-sm">📒 Build log</h4>
      <form onSubmit={add} className="space-y-2">
        <div className="flex gap-1.5" role="radiogroup" aria-label="Entry type">
          {LOG_TYPES.map((t) => (
            <button key={t.id} type="button" role="radio" aria-checked={type === t.id} onClick={() => setType(t.id)}
              className={`chip px-3 py-1.5 text-sm ${type === t.id ? 'chip-on' : ''}`}>{t.icon} {t.label}</button>
          ))}
        </div>
        <label className="sr-only" htmlFor={`log-${p.id}`}>Entry text</label>
        <textarea id={`log-${p.id}`} className="input min-h-[70px]" maxLength={4000} value={text} onChange={(e) => setText(e.target.value)}
          placeholder={type === 'BLOCKER' ? 'What is stuck? What error do you see?' : type === 'LEARNED' ? 'What did you learn today?' : 'What did you build today?'} />
        {err && <ErrorBox message={err} />}
        <button className="btn-primary text-sm" disabled={busy || !text.trim()}>{busy ? 'Adding…' : 'Add entry'}</button>
      </form>
      {logs.length === 0
        ? <p className="text-sm text-muted">No entries yet. One line a day is enough.</p>
        : (
          <ul className="space-y-2">
            {logs.map((l) => {
              const t = LOG_TYPES.find((x) => x.id === l.entry_type) ?? LOG_TYPES[0]
              return (
                <li key={l.id} className={`rounded-xl border bg-raised p-2.5 text-sm ${t.tone} ${l.resolved ? 'opacity-60' : ''}`}>
                  <div className="flex items-center justify-between gap-2 text-xs text-muted">
                    <span>{t.icon} {t.label} · {fmtDate(l.local_date)}</span>
                    {l.entry_type === 'BLOCKER' && (
                      <label className="flex items-center gap-1 cursor-pointer">
                        <input type="checkbox" className="accent-primary" checked={l.resolved} onChange={() => void resolve(l)} />
                        Solved
                      </label>
                    )}
                  </div>
                  <p className={`mt-1 whitespace-pre-wrap ${l.resolved ? 'line-through' : ''}`}>{l.text}</p>
                </li>
              )
            })}
          </ul>
        )}
    </section>
  )
}

function fmtDate(v: string | number) {
  if (typeof v === 'number') return new Date(v).toLocaleDateString()
  return v
}
