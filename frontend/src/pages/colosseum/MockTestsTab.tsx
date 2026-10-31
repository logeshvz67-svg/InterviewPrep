import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../../api/client'
import type { CompanyView, ProfileView } from '../../api/types'
import { ErrorBox, Section, Spinner } from '../../components/ui'
import { errorMessage, useApi } from '../../lib/hooks'
import { niceCode, today } from './shared'
import type { ExamPatternRequest, ExamPatternView, MockSummary, MockTestView, PatternSection, SectionKind } from './types'

const KINDS: { id: SectionKind; label: string }[] = [
  { id: 'APTITUDE', label: 'Aptitude' }, { id: 'TECH_MCQ', label: 'Technical MCQ' }, { id: 'CODING', label: 'Coding' },
]

export default function MockTestsTab() {
  const nav = useNavigate()
  const mocks = useApi<MockSummary[]>('/api/mock-tests')
  const patterns = useApi<ExamPatternView[]>('/api/exam-patterns')
  const companies = useApi<CompanyView[]>('/api/companies')
  const profile = useApi<ProfileView>('/api/profile')
  const [editing, setEditing] = useState<ExamPatternView | 'new' | null>(null)
  const [busy, setBusy] = useState<number | null>(null)
  const [err, setErr] = useState<string | null>(null)
  const name = (code: string) => companies.data?.find((c) => c.code === code)?.name ?? niceCode(code)

  const start = async (p: ExamPatternView) => {
    setBusy(p.id); setErr(null)
    try {
      const t = await api<MockTestView>(`/api/mock-tests?patternId=${p.id}`, { method: 'POST' })
      nav(`/interview/mock-test/${t.id}`)
    } catch (e) { setErr(errorMessage(e)); setBusy(null) }
  }

  const remove = async (p: ExamPatternView) => {
    if (!window.confirm(`Delete the pattern "${p.name}"? Past mock tests stay.`)) return
    try {
      await api<void>(`/api/exam-patterns/${p.id}`, { method: 'DELETE' })
      void patterns.reload()
    } catch (e) { setErr(errorMessage(e)) }
  }

  return (
    <div className="space-y-6">
      <div className="card border-gold/60 bg-gold/10" role="note">
        <p className="font-semibold">📌 Check the official Infosys careers page and enter the current pattern. Code Quest never guesses it.</p>
        <p className="text-sm text-muted mt-1">Test patterns change often. You type in the sections, question counts, minutes and cutoffs you found, and mock tests are built from that.</p>
      </div>

      <Section title="🧾 My exam patterns" action={!editing && <button className="btn-primary text-sm" onClick={() => setEditing('new')}>+ Add pattern</button>}>
        {editing && (
          <PatternForm initial={editing === 'new' ? null : editing} companies={companies.data ?? []}
            defaultCompany={profile.data?.primaryCompany ?? companies.data?.[0]?.code ?? ''}
            onDone={() => { setEditing(null); void patterns.reload() }} onCancel={() => setEditing(null)} />
        )}
        {err && <ErrorBox message={err} />}
        {patterns.loading && !patterns.data && <Spinner />}
        {patterns.error && <ErrorBox message={patterns.error} onRetry={patterns.reload} />}
        {patterns.data?.length === 0 && !editing && (
          <p className="card text-sm text-muted">No pattern yet. Add one to start a timed mock test.</p>
        )}
        <ul className="space-y-3">
          {patterns.data?.map((p) => (
            <li key={p.id} className="card space-y-2">
              <div className="flex flex-wrap items-start justify-between gap-2">
                <div>
                  <h3 className="font-semibold">{p.name}</h3>
                  <p className="text-xs text-muted">{name(p.companyCode)} · {p.totalMinutes} min total</p>
                </div>
                <div className="flex gap-2">
                  <button className="btn-ghost text-sm" onClick={() => setEditing(p)}>Edit</button>
                  <button className="btn-ghost text-sm text-bad" onClick={() => remove(p)} aria-label={`Delete ${p.name}`}>Delete</button>
                </div>
              </div>
              <ul className="text-sm space-y-1">
                {p.sections.map((s, i) => (
                  <li key={i} className="flex flex-wrap gap-x-2">
                    <b>{s.name}</b>
                    <span className="text-muted">{KINDS.find((k) => k.id === s.kind)?.label} · {s.questions} q · {s.minutes} min{s.cutoffPercent != null ? ` · cutoff ${s.cutoffPercent}%` : ''}</span>
                  </li>
                ))}
              </ul>
              <p className="text-xs text-muted">
                {p.sourceNote ? <>Source: {p.sourceNote}. </> : <>No source note. </>}
                {p.verifiedOn ? <>Checked on {p.verifiedOn}.</> : <span className="text-warn">Not verified yet.</span>}
              </p>
              <button className="btn-gold w-full sm:w-auto" disabled={busy !== null} onClick={() => start(p)}>
                {busy === p.id ? 'Preparing paper…' : `▶ Start ${p.totalMinutes}-min mock`}
              </button>
            </li>
          ))}
        </ul>
      </Section>

      <Section title="📜 Past mock tests">
        {mocks.loading && !mocks.data && <Spinner />}
        {mocks.error && <ErrorBox message={mocks.error} onRetry={mocks.reload} />}
        {mocks.data?.length === 0 && <p className="card text-sm text-muted">No mock tests yet.</p>}
        <ul className="space-y-2">
          {mocks.data?.map((m) => (
            <li key={m.id}>
              <button className="card w-full text-left flex items-center justify-between gap-3 hover:border-primary/60" onClick={() => nav(`/interview/mock-test/${m.id}`)}>
                <span className="min-w-0">
                  <span className="font-medium block truncate">{m.title}</span>
                  <span className="text-xs text-muted">{new Date(m.startedAt).toLocaleString()} · {name(m.companyCode)}</span>
                </span>
                <span className="shrink-0 text-right">
                  {m.score != null
                    ? <span className="font-bold text-lg">{Math.round(Number(m.score))}%</span>
                    : <span className={`chip ${m.status === 'RUNNING' ? 'text-warn' : ''}`}>{m.status === 'RUNNING' ? 'In progress' : niceCode(m.status)}</span>}
                </span>
              </button>
            </li>
          ))}
        </ul>
      </Section>
    </div>
  )
}

const emptySection = (): PatternSection => ({ name: '', kind: 'APTITUDE', questions: 10, minutes: 20, cutoffPercent: null })

function PatternForm({ initial, companies, defaultCompany, onDone, onCancel }: {
  initial: ExamPatternView | null; companies: CompanyView[]; defaultCompany: string; onDone: () => void; onCancel: () => void
}) {
  const [f, setF] = useState<ExamPatternRequest>(() => initial
    ? { companyCode: initial.companyCode, name: initial.name, sections: initial.sections.map((s) => ({ ...s, cutoffPercent: s.cutoffPercent ?? null })), sourceNote: initial.sourceNote, verifiedOn: initial.verifiedOn }
    : { companyCode: defaultCompany, name: '', sections: [emptySection()], sourceNote: '', verifiedOn: null })
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState<string | null>(null)

  const setSec = (i: number, patch: Partial<PatternSection>) =>
    setF({ ...f, sections: f.sections.map((s, j) => (j === i ? { ...s, ...patch } : s)) })
  const total = f.sections.reduce((a, s) => a + (Number(s.minutes) || 0), 0)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true); setErr(null)
    const body: ExamPatternRequest = {
      ...f, companyCode: f.companyCode || defaultCompany, name: f.name.trim(),
      sourceNote: f.sourceNote?.trim() || null, verifiedOn: f.verifiedOn || null,
      sections: f.sections.map((s) => ({ ...s, name: s.name.trim(), cutoffPercent: s.cutoffPercent === null || Number.isNaN(s.cutoffPercent) ? null : s.cutoffPercent })),
    }
    try {
      await api<ExamPatternView>(initial ? `/api/exam-patterns/${initial.id}` : '/api/exam-patterns', { method: initial ? 'PUT' : 'POST', body })
      onDone()
    } catch (e2) { setErr(errorMessage(e2)) } finally { setBusy(false) }
  }

  return (
    <form onSubmit={submit} className="card space-y-4 border-primary/50" aria-label={initial ? 'Edit exam pattern' : 'New exam pattern'}>
      <div className="grid gap-3 sm:grid-cols-2">
        <div>
          <label className="label" htmlFor="pat-co">Company</label>
          <select id="pat-co" className="input" value={f.companyCode || defaultCompany} onChange={(e) => setF({ ...f, companyCode: e.target.value })} required>
            {companies.map((c) => <option key={c.code} value={c.code}>{c.name}</option>)}
          </select>
        </div>
        <div>
          <label className="label" htmlFor="pat-name">Pattern name</label>
          <input id="pat-name" className="input" required maxLength={120} placeholder="e.g. Infosys SP online test 2026"
            value={f.name} onChange={(e) => setF({ ...f, name: e.target.value })} />
        </div>
      </div>

      <fieldset className="space-y-3">
        <legend className="font-semibold text-sm">Sections (in test order)</legend>
        {f.sections.map((s, i) => (
          <div key={i} className="rounded-xl bg-raised p-3 grid gap-2 grid-cols-2 sm:grid-cols-6 items-end">
            <div className="col-span-2">
              <label className="label" htmlFor={`sec-name-${i}`}>Section name</label>
              <input id={`sec-name-${i}`} className="input bg-surface" required maxLength={80} placeholder="e.g. Reasoning"
                value={s.name} onChange={(e) => setSec(i, { name: e.target.value })} />
            </div>
            <div className="col-span-2 sm:col-span-1">
              <label className="label" htmlFor={`sec-kind-${i}`}>Kind</label>
              <select id={`sec-kind-${i}`} className="input bg-surface" value={s.kind} onChange={(e) => setSec(i, { kind: e.target.value as SectionKind })}>
                {KINDS.map((k) => <option key={k.id} value={k.id}>{k.label}</option>)}
              </select>
            </div>
            <div>
              <label className="label" htmlFor={`sec-q-${i}`}>Questions</label>
              <input id={`sec-q-${i}`} className="input bg-surface" type="number" min={1} max={100} required
                value={s.questions} onChange={(e) => setSec(i, { questions: e.target.valueAsNumber })} />
            </div>
            <div>
              <label className="label" htmlFor={`sec-m-${i}`}>Minutes</label>
              <input id={`sec-m-${i}`} className="input bg-surface" type="number" min={1} max={300} required
                value={s.minutes} onChange={(e) => setSec(i, { minutes: e.target.valueAsNumber })} />
            </div>
            <div>
              <label className="label" htmlFor={`sec-c-${i}`}>Cutoff %</label>
              <input id={`sec-c-${i}`} className="input bg-surface" type="number" min={0} max={100} placeholder="optional"
                value={s.cutoffPercent ?? ''} onChange={(e) => setSec(i, { cutoffPercent: e.target.value === '' ? null : e.target.valueAsNumber })} />
            </div>
            {f.sections.length > 1 && (
              <button type="button" className="col-span-2 sm:col-span-6 text-xs text-bad underline justify-self-start"
                onClick={() => setF({ ...f, sections: f.sections.filter((_, j) => j !== i) })}>Remove this section</button>
            )}
          </div>
        ))}
        {f.sections.length < 10 && (
          <button type="button" className="btn-ghost text-sm" onClick={() => setF({ ...f, sections: [...f.sections, emptySection()] })}>+ Add section</button>
        )}
        <p className="text-sm text-muted">Total time: <b>{total} min</b></p>
      </fieldset>

      <div className="grid gap-3 sm:grid-cols-2">
        <div>
          <label className="label" htmlFor="pat-src">Source note (where you found it)</label>
          <input id="pat-src" className="input" maxLength={300} placeholder="e.g. Infosys careers page, SP hiring section"
            value={f.sourceNote ?? ''} onChange={(e) => setF({ ...f, sourceNote: e.target.value })} />
        </div>
        <div>
          <label className="label" htmlFor="pat-ver">Verified on</label>
          <input id="pat-ver" className="input" type="date" max={today()}
            value={f.verifiedOn ?? ''} onChange={(e) => setF({ ...f, verifiedOn: e.target.value || null })} />
        </div>
      </div>

      {err && <ErrorBox message={err} />}
      <div className="flex gap-2">
        <button className="btn-primary" disabled={busy}>{busy ? 'Saving…' : 'Save pattern'}</button>
        <button type="button" className="btn-ghost" onClick={onCancel}>Cancel</button>
      </div>
    </form>
  )
}
