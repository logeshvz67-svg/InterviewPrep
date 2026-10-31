import { useState, type FormEvent } from 'react'
import { api } from '../../api/client'
import { ErrorBox, Section, Spinner } from '../../components/ui'
import { errorMessage, useApi } from '../../lib/hooks'
import { today } from './shared'
import type { DiaryRequest, DiaryView } from './types'

const ROUNDS = ['Online test', 'Technical', 'HR', 'Managerial', 'Group discussion', 'Other']
const blank = (): DiaryRequest => ({ company: '', round: 'Technical', interviewDate: today(), question: '', myAnswer: '', betterAnswer: '', tags: '' })

export default function DiaryTab() {
  const list = useApi<DiaryView[]>('/api/diary')
  const [editing, setEditing] = useState<DiaryView | 'new' | null>(null)
  const [filter, setFilter] = useState('')
  const [err, setErr] = useState<string | null>(null)

  const remove = async (d: DiaryView) => {
    if (!window.confirm('Delete this diary entry?')) return
    try { await api<void>(`/api/diary/${d.id}`, { method: 'DELETE' }); void list.reload() } catch (e) { setErr(errorMessage(e)) }
  }

  const f = filter.trim().toLowerCase()
  const shown = (list.data ?? []).filter((d) => !f || [d.company, d.round, d.question, d.tags ?? ''].join(' ').toLowerCase().includes(f))

  return (
    <div className="space-y-4">
      <p className="text-sm text-muted">
        After a real interview, write down what they asked while you still remember. Then write a better answer. Next time you will be ready.
      </p>
      <Section title="📔 Interview diary" action={!editing && <button className="btn-primary text-sm" onClick={() => setEditing('new')}>+ New entry</button>}>
        {editing && (
          <DiaryForm initial={editing === 'new' ? null : editing}
            onDone={() => { setEditing(null); void list.reload() }} onCancel={() => setEditing(null)} />
        )}
        {err && <ErrorBox message={err} />}
        {list.loading && !list.data && <Spinner />}
        {list.error && <ErrorBox message={list.error} onRetry={list.reload} />}
        {list.data && list.data.length > 0 && (
          <div>
            <label className="sr-only" htmlFor="diary-filter">Filter entries</label>
            <input id="diary-filter" className="input" type="search" placeholder="Filter by company, round, tag…" value={filter} onChange={(e) => setFilter(e.target.value)} />
          </div>
        )}
        {list.data?.length === 0 && !editing && <p className="card text-sm text-muted">No entries yet.</p>}
        <ul className="space-y-3">
          {shown.map((d) => (
            <li key={d.id} className="card space-y-2">
              <div className="flex flex-wrap items-start justify-between gap-2">
                <div className="text-xs text-muted">
                  <b className="text-ink">{d.company}</b> · {d.round} · {d.interviewDate}
                </div>
                <div className="flex gap-2">
                  <button className="btn-ghost text-xs px-3 py-1.5" onClick={() => setEditing(d)}>Edit</button>
                  <button className="btn-ghost text-xs px-3 py-1.5 text-bad" onClick={() => remove(d)} aria-label="Delete entry">Delete</button>
                </div>
              </div>
              <p className="font-medium whitespace-pre-wrap">❓ {d.question}</p>
              {d.myAnswer && <details className="text-sm"><summary className="cursor-pointer text-muted">What I said</summary><p className="mt-1 whitespace-pre-wrap">{d.myAnswer}</p></details>}
              {d.betterAnswer && <div className="rounded-xl bg-good/10 border border-good/40 p-2 text-sm whitespace-pre-wrap"><b>Better answer: </b>{d.betterAnswer}</div>}
              {d.tags && (
                <div className="flex flex-wrap gap-1">
                  {d.tags.split(',').map((t) => t.trim()).filter(Boolean).map((t) => <span key={t} className="chip">#{t}</span>)}
                </div>
              )}
            </li>
          ))}
        </ul>
      </Section>
    </div>
  )
}

function DiaryForm({ initial, onDone, onCancel }: { initial: DiaryView | null; onDone: () => void; onCancel: () => void }) {
  const [f, setF] = useState<DiaryRequest>(() => initial
    ? { company: initial.company, round: initial.round, interviewDate: initial.interviewDate, question: initial.question,
        myAnswer: initial.myAnswer ?? '', betterAnswer: initial.betterAnswer ?? '', tags: initial.tags ?? '' }
    : blank())
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState<string | null>(null)
  const set = (k: keyof DiaryRequest) => (e: { target: { value: string } }) => setF({ ...f, [k]: e.target.value })

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true); setErr(null)
    try {
      await api<DiaryView>(initial ? `/api/diary/${initial.id}` : '/api/diary', { method: initial ? 'PUT' : 'POST', body: f })
      onDone()
    } catch (e2) { setErr(errorMessage(e2)) } finally { setBusy(false) }
  }

  return (
    <form onSubmit={submit} className="card space-y-3 border-primary/50" aria-label={initial ? 'Edit diary entry' : 'New diary entry'}>
      <div className="grid gap-3 sm:grid-cols-3">
        <div>
          <label className="label" htmlFor="d-co">Company</label>
          <input id="d-co" className="input" required maxLength={80} value={f.company} onChange={set('company')} placeholder="e.g. Infosys" />
        </div>
        <div>
          <label className="label" htmlFor="d-round">Round</label>
          <input id="d-round" className="input" required maxLength={40} list="d-rounds" value={f.round} onChange={set('round')} />
          <datalist id="d-rounds">{ROUNDS.map((r) => <option key={r} value={r} />)}</datalist>
        </div>
        <div>
          <label className="label" htmlFor="d-date">Date</label>
          <input id="d-date" className="input" type="date" required max={today()} value={f.interviewDate} onChange={set('interviewDate')} />
        </div>
      </div>
      <div>
        <label className="label" htmlFor="d-q">Question they asked</label>
        <textarea id="d-q" className="input min-h-[70px]" required maxLength={2000} value={f.question} onChange={set('question')} />
      </div>
      <div>
        <label className="label" htmlFor="d-my">My answer (what I said)</label>
        <textarea id="d-my" className="input min-h-[70px]" maxLength={6000} value={f.myAnswer} onChange={set('myAnswer')} />
      </div>
      <div>
        <label className="label" htmlFor="d-better">Better answer (for next time)</label>
        <textarea id="d-better" className="input min-h-[90px]" maxLength={6000} value={f.betterAnswer} onChange={set('betterAnswer')} />
      </div>
      <div>
        <label className="label" htmlFor="d-tags">Tags (comma separated)</label>
        <input id="d-tags" className="input" maxLength={200} value={f.tags} onChange={set('tags')} placeholder="oop, hashmap, project" />
      </div>
      {err && <ErrorBox message={err} />}
      <div className="flex gap-2">
        <button className="btn-primary" disabled={busy}>{busy ? 'Saving…' : 'Save entry'}</button>
        <button type="button" className="btn-ghost" onClick={onCancel}>Cancel</button>
      </div>
    </form>
  )
}
