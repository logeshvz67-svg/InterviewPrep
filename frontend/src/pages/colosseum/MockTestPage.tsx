import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, ApiError } from '../../api/client'
import { ErrorBox, Spinner } from '../../components/ui'
import { errorMessage, useApi } from '../../lib/hooks'
import { CodeBlock, Explanation, Options, niceCode } from './shared'
import type {
  AptitudeMockItem, CodingAnswer, CodingMockItem, CodingStatus, MockAnswer, MockItem, MockSection, MockTestView, TechMockItem,
} from './types'

const storeKey = (id: string) => `cq.mock.${id}`
function loadSaved(id: string): Record<string, MockAnswer> {
  try {
    const raw = localStorage.getItem(storeKey(id))
    const v: unknown = raw ? JSON.parse(raw) : null
    return v && typeof v === 'object' ? (v as Record<string, MockAnswer>) : {}
  } catch { return {} }
}
function saveLocal(id: string, a: Record<string, MockAnswer>) {
  try { localStorage.setItem(storeKey(id), JSON.stringify(a)) } catch { /* storage full or blocked: answers stay in memory */ }
}
function clearLocal(id: string) {
  try { localStorage.removeItem(storeKey(id)) } catch { /* ignore */ }
}

const isCoding = (s: MockSection) => s.kind === 'CODING'
const isAnswered = (a: MockAnswer | undefined) => a !== undefined && (typeof a === 'number' || a.status !== 'NOT_ATTEMPTED')

/** Route: /interview/mock-test/:id */
export default function MockTestPage() {
  const { id = '' } = useParams()
  const mock = useApi<MockTestView>(id ? `/api/mock-tests/${id}` : null)

  if (mock.loading && !mock.data) return <Spinner label="Loading your test…" />
  if (mock.error) return <div className="space-y-3"><ErrorBox message={mock.error} onRetry={mock.reload} /><Link to="/interview?tab=mocks" className="btn-ghost">← Back to mock tests</Link></div>
  if (!mock.data) return null
  const t = mock.data

  if (t.status === 'RUNNING') {
    return <RunningTest key={t.id} test={t} onSubmitted={(v) => { clearLocal(id); mock.setData(v) }} onConflict={() => void mock.reload()} />
  }
  return <Results test={t} />
}

// ------------------------------------------------------------------ running

function RunningTest({ test, onSubmitted, onConflict }: { test: MockTestView; onSubmitted: (v: MockTestView) => void; onConflict: () => void }) {
  const id = String(test.id)
  const [answers, setAnswers] = useState<Record<string, MockAnswer>>(() => loadSaved(id))
  const [secIdx, setSecIdx] = useState(0)
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState<string | null>(null)
  // The device clock can be wrong: measure how far it is from the server once, and correct every tick with that.
  const [offset] = useState(() => new Date(test.serverNow).getTime() - Date.now())
  const endsAt = new Date(test.endsAt).getTime()
  const [remaining, setRemaining] = useState(() => endsAt - (Date.now() + offset))
  const submitted = useRef(false)
  const answersRef = useRef(answers)
  answersRef.current = answers

  useEffect(() => { saveLocal(id, answers) }, [id, answers])

  useEffect(() => {
    const tick = () => setRemaining(endsAt - (Date.now() + offset))
    const t = setInterval(tick, 1000)
    document.addEventListener('visibilitychange', tick)
    return () => { clearInterval(t); document.removeEventListener('visibilitychange', tick) }
  }, [endsAt, offset])

  useEffect(() => {
    const warn = (e: BeforeUnloadEvent) => { e.preventDefault() }
    window.addEventListener('beforeunload', warn)
    return () => window.removeEventListener('beforeunload', warn)
  }, [])

  const submit = useCallback(async (auto: boolean) => {
    if (submitted.current) return
    if (!auto && !window.confirm('Submit the test now? You cannot change answers after this.')) return
    submitted.current = true
    setBusy(true); setErr(null)
    try {
      const v = await api<MockTestView>(`/api/mock-tests/${id}/submit`, { method: 'POST', body: { answers: answersRef.current } })
      onSubmitted(v)
    } catch (e) {
      if (e instanceof ApiError && e.status === 409) { onConflict(); return }
      submitted.current = false
      setErr(errorMessage(e) + (auto ? '\nTime is up. Your answers are saved on this device; press Submit to try again.' : ''))
    } finally { setBusy(false) }
  }, [id, onSubmitted, onConflict])

  useEffect(() => { if (remaining <= 0) void submit(true) }, [remaining, submit])

  const set = (key: string, v: MockAnswer) => setAnswers((a) => ({ ...a, [key]: v }))
  const sec = test.sections[secIdx]
  const total = test.sections.reduce((n, s) => n + s.items.length, 0)
  const done = test.sections.reduce((n, s) => n + s.items.filter((it) => isAnswered(answers[it.key])).length, 0)
  const secs = Math.max(0, Math.floor(remaining / 1000))
  const low = secs <= 300

  return (
    <div className="fixed inset-0 z-40 bg-bg overflow-y-auto">
      <header className="sticky top-0 z-10 bg-surface/95 backdrop-blur border-b border-line">
        <div className="max-w-3xl mx-auto px-4 py-3 flex items-center justify-between gap-3">
          <div className="min-w-0">
            <h1 className="font-semibold truncate">{test.title}</h1>
            <p className="text-xs text-muted">{done} of {total} answered · saved on this device</p>
          </div>
          <div className={`font-mono text-xl font-bold tabular-nums ${low ? 'text-bad animate-pulse' : 'text-ink'}`} role="timer"
            aria-label={`Time left ${Math.floor(secs / 60)} minutes ${secs % 60} seconds`}>
            {fmtClock(secs)}
          </div>
        </div>
        <nav className="max-w-3xl mx-auto px-4 pb-2 flex gap-1 overflow-x-auto" aria-label="Test sections">
          {test.sections.map((s, i) => {
            const n = s.items.filter((it) => isAnswered(answers[it.key])).length
            return (
              <button key={i} onClick={() => { setSecIdx(i); window.scrollTo(0, 0) }} aria-current={i === secIdx ? 'step' : undefined}
                className={`chip whitespace-nowrap px-3 py-1.5 text-sm ${i === secIdx ? 'chip-on' : ''}`}>
                {s.name} <span className="ml-1 opacity-70">{n}/{s.items.length}</span>
              </button>
            )
          })}
        </nav>
      </header>

      <main className="max-w-3xl mx-auto px-4 py-4 space-y-4 pb-28">
        <p className="text-sm text-muted">
          {niceCode(sec.kind)} · {sec.items.length} questions · suggested {sec.minutes} min
          {sec.cutoffPercent != null && ` · cutoff ${sec.cutoffPercent}%`}
        </p>
        {isCoding(sec) && (
          <div className="card border-warn/50 text-sm" role="note">
            💻 Solve each problem on the platform (opens in a new tab). Then come back and honestly report how it went. Code is not run here.
          </div>
        )}
        {sec.items.length === 0 && <p className="card text-sm text-muted">No questions were available for this section.</p>}
        <ol className="space-y-4">
          {sec.items.map((it, i) => (
            <li key={it.key} className="card space-y-3">
              <p className="text-xs text-muted">Question {i + 1}</p>
              {sec.kind === 'CODING'
                ? <CodingItem item={it as CodingMockItem} value={answers[it.key] as CodingAnswer | undefined} onChange={(v) => set(it.key, v)} />
                : <McqItem item={it} kind={sec.kind} value={typeof answers[it.key] === 'number' ? (answers[it.key] as number) : null} onChange={(v) => set(it.key, v)} />}
            </li>
          ))}
        </ol>
        <div className="flex justify-between gap-2">
          <button className="btn-ghost" disabled={secIdx === 0} onClick={() => { setSecIdx(secIdx - 1); window.scrollTo(0, 0) }}>← Previous section</button>
          {secIdx + 1 < test.sections.length && <button className="btn-primary" onClick={() => { setSecIdx(secIdx + 1); window.scrollTo(0, 0) }}>Next section →</button>}
        </div>
      </main>

      <footer className="fixed bottom-0 inset-x-0 bg-surface/95 backdrop-blur border-t border-line">
        <div className="max-w-3xl mx-auto px-4 py-3 space-y-2">
          {err && <ErrorBox message={err} />}
          <div className="flex items-center justify-between gap-3">
            <Link to="/interview?tab=mocks" className="text-xs text-muted underline">Leave (the timer keeps running)</Link>
            <button className="btn-gold" disabled={busy} onClick={() => void submit(false)}>{busy ? 'Submitting…' : 'Submit test'}</button>
          </div>
        </div>
      </footer>
    </div>
  )
}

function McqItem({ item, kind, value, onChange, answer, disabled }: {
  item: MockItem; kind: string; value: number | null; onChange: (v: number) => void; answer?: number; disabled?: boolean
}) {
  if (kind === 'APTITUDE') {
    const a = item as AptitudeMockItem
    return (
      <>
        <p className="font-medium whitespace-pre-wrap">{a.question}</p>
        <Options name={item.key} options={a.options ?? []} value={value} onChange={onChange} answer={answer} disabled={disabled} />
      </>
    )
  }
  const t = item as TechMockItem
  return (
    <>
      {t.type && t.type !== 'MCQ' && <span className="chip">{niceCode(t.type)}</span>}
      <p className="font-medium whitespace-pre-wrap">{t.prompt}</p>
      {t.code && <CodeBlock code={t.code} label="Code for this question" />}
      <Options name={item.key} options={t.options ?? []} value={value} onChange={onChange} answer={answer} disabled={disabled} />
    </>
  )
}

const CODING_STATUSES: { id: CodingStatus; label: string }[] = [
  { id: 'SOLVED', label: '✅ Solved' }, { id: 'PARTIAL', label: '🟡 Partly' }, { id: 'NOT_ATTEMPTED', label: '⬜ Not attempted' },
]

function CodingItem({ item, value, onChange }: { item: CodingMockItem; value: CodingAnswer | undefined; onChange: (v: CodingAnswer) => void }) {
  const v: CodingAnswer = value ?? { status: 'NOT_ATTEMPTED', minutes: 0 }
  return (
    <>
      <div className="flex flex-wrap items-center gap-2">
        <h3 className="font-semibold">{item.title}</h3>
        {item.difficulty && <span className="chip">{niceCode(item.difficulty)}</span>}
      </div>
      {item.url
        ? <a href={item.url} target="_blank" rel="noopener noreferrer" className="btn-ghost text-sm">Open problem ↗<span className="sr-only"> (opens in a new tab)</span></a>
        : <p className="text-sm text-muted">No link saved for this problem. Search its title on your coding platform.</p>}
      <fieldset>
        <legend className="label">How did it go?</legend>
        <div className="flex flex-wrap gap-2" role="radiogroup">
          {CODING_STATUSES.map((s) => (
            <button key={s.id} type="button" role="radio" aria-checked={v.status === s.id} onClick={() => onChange({ ...v, status: s.id })}
              className={`chip px-3 py-1.5 text-sm ${v.status === s.id ? 'chip-on' : ''}`}>{s.label}</button>
          ))}
        </div>
      </fieldset>
      <div className="max-w-[10rem]">
        <label className="label" htmlFor={`min-${item.key}`}>Minutes spent</label>
        <input id={`min-${item.key}`} className="input" type="number" min={0} max={300} value={v.minutes}
          onChange={(e) => onChange({ ...v, minutes: Number.isNaN(e.target.valueAsNumber) ? 0 : e.target.valueAsNumber })} />
      </div>
    </>
  )
}

// ------------------------------------------------------------------ results

function Results({ test }: { test: MockTestView }) {
  const r = test.result
  const [open, setOpen] = useState<number | null>(0)
  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-bold">{test.title}</h1>
        <Link to="/interview?tab=mocks" className="btn-ghost text-sm">← Mock tests</Link>
      </div>

      {!r && (
        <div className="card text-sm">
          {test.status === 'EXPIRED'
            ? 'This test expired without being submitted. Start a new one when you have the full time free.'
            : 'No result for this test.'}
        </div>
      )}

      {r && (
        <>
          <div className="card flex flex-wrap items-center gap-4">
            <div className="text-4xl font-bold font-display">{Math.round(Number(r.scorePercent))}%</div>
            <div className="text-sm text-muted">
              Average of all sections.{r.late && <span className="text-warn"> Submitted after the time limit.</span>}
              <br />Coding scores are self-reported (we do not run your code).
            </div>
          </div>

          <ul className="space-y-2">
            {r.sections.map((s, i) => (
              <li key={i} className="card flex flex-wrap items-center justify-between gap-2">
                <div>
                  <b>{s.name}</b> <span className="text-xs text-muted">{niceCode(s.kind)}</span>
                  <div className="text-sm text-muted">{s.score} / {s.total} · {Math.round(Number(s.percent))}%{s.note ? ` · ${s.note}` : ''}</div>
                </div>
                {s.passedCutoff === undefined
                  ? <span className="chip">no cutoff set</span>
                  : s.passedCutoff
                    ? <span className="chip bg-good/15 text-good">✓ Cleared cutoff</span>
                    : <span className="chip bg-bad/15 text-bad">✗ Below cutoff</span>}
              </li>
            ))}
          </ul>

          <h2 className="text-lg font-semibold">Review answers</h2>
          {test.sections.map((sec, si) => (
            <section key={si} className="card space-y-3">
              <button className="w-full flex justify-between items-center font-semibold" aria-expanded={open === si} onClick={() => setOpen(open === si ? null : si)}>
                {sec.name} <span aria-hidden>{open === si ? '−' : '+'}</span>
              </button>
              {open === si && (
                <ol className="space-y-4">
                  {sec.items.map((it, i) => {
                    const given = r.answers?.[it.key]
                    if (sec.kind === 'CODING') {
                      const c = it as CodingMockItem
                      const g = given && typeof given === 'object' ? given : null
                      return (
                        <li key={it.key} className="border-t border-line/60 pt-3 text-sm">
                          <b>{i + 1}. {c.title}</b>{' '}
                          {c.url && <a className="text-primary underline" href={c.url} target="_blank" rel="noopener noreferrer">open ↗</a>}
                          <div className="text-muted">You reported: {niceCode(g?.status ?? 'NOT_ATTEMPTED')}{g?.minutes ? ` · ${g.minutes} min` : ''}</div>
                        </li>
                      )
                    }
                    const ans = (it as AptitudeMockItem | TechMockItem).answer
                    const expl = (it as AptitudeMockItem | TechMockItem).explanation
                    const chosen = typeof given === 'number' ? given : null
                    return (
                      <li key={it.key} className="border-t border-line/60 pt-3 space-y-2">
                        <p className="text-xs text-muted">Question {i + 1}{chosen === null ? ' · skipped' : ''}</p>
                        <McqItem item={it} kind={sec.kind} value={chosen} onChange={() => undefined} answer={ans} disabled />
                        {expl && <Explanation correct={ans !== undefined ? chosen === ans : undefined}>{expl}</Explanation>}
                      </li>
                    )
                  })}
                </ol>
              )}
            </section>
          ))}
        </>
      )}
    </div>
  )
}

function fmtClock(s: number) {
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  return `${h > 0 ? `${h}:` : ''}${String(m).padStart(h > 0 ? 2 : 1, '0')}:${String(sec).padStart(2, '0')}`
}
