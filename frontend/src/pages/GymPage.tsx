import { useCallback, useState, type FormEvent } from 'react'
import { Link, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import type { BankProblem, PatternCoverage, Suggestion } from '../api/learning'
import { HintLadder } from './mentor/MentorPage'
import { api } from '../api/client'
import type { MistakeCategoryView, MistakeTrend, MistakeView, Page, PatternMap, PatternView, ProblemSaved, ProblemView, RevisitView, XpAward } from '../api/types'
import { ErrorBox, Section, Spinner, Stars, Tabs, XpToast } from '../components/ui'
import { errorMessage, useApi } from '../lib/hooks'

type Tab = 'problems' | 'suggested' | 'bank' | 'log' | 'patterns' | 'revisits' | 'mistakes'
const TABS: { id: Tab; label: string }[] = [
  { id: 'suggested', label: '✨ Suggested' }, { id: 'log', label: '➕ Log problem' }, { id: 'problems', label: '📚 My problems' },
  { id: 'bank', label: '🗃️ Problem bank' }, { id: 'patterns', label: '🗺️ Pattern map' },
  { id: 'revisits', label: '🔁 Revisits' }, { id: 'mistakes', label: '📓 Mistakes' },
]

export default function GymPage() {
  const loc = useLocation()
  const nav = useNavigate()
  const current = (loc.pathname.split('/')[2] || 'problems') as Tab
  const [award, setAward] = useState<XpAward | null>(null)
  const clear = useCallback(() => setAward(null), [])

  return (
    <div className="space-y-5">
      <h1 className="text-2xl font-bold">🏋️ Problem Gym</h1>
      <Tabs tabs={TABS} value={current} onChange={(t) => nav(t === 'problems' ? '/gym' : `/gym/${t}`)} />
      <Routes>
        <Route index element={<ProblemList />} />
        <Route path="suggested" element={<Suggested />} />
        <Route path="bank" element={<Bank />} />
        <Route path="log" element={<LogProblem onAward={setAward} />} />
        <Route path="patterns" element={<PatternMapView />} />
        <Route path="revisits" element={<Revisits onAward={setAward} />} />
        <Route path="mistakes" element={<Mistakes onAward={setAward} />} />
      </Routes>
      <XpToast award={award} onDone={clear} />
    </div>
  )
}

// ------------------------------------------------------------------ framework card

const FRAMEWORK = [
  ['Understand', 'Say the problem in your own words. What is the input? The output?'],
  ['Examples', 'Work 2 small examples by hand, including a tricky one.'],
  ['Brute force', 'What is the simplest way, even if slow? Say its Big-O.'],
  ['Optimize', 'Which pattern fits? (hashing, two pointers, sliding window...)'],
  ['Edge cases', 'Empty, one element, duplicates, negatives, very large.'],
  ['Code', 'Type it yourself. No copy-paste.'],
  ['Dry run', 'Trace your code line by line on one example.'],
  ['Complexity', 'Final time and space Big-O, in one sentence.'],
]

function FrameworkCard() {
  const [open, setOpen] = useState(true)
  return (
    <div className="card border-accent/40">
      <button className="w-full flex justify-between items-center font-semibold" onClick={() => setOpen(!open)} aria-expanded={open}>
        🧭 Problem-solving framework (use it before every problem) <span>{open ? '−' : '+'}</span>
      </button>
      {open && (
        <ol className="mt-3 grid sm:grid-cols-2 gap-2 text-sm">
          {FRAMEWORK.map(([t, d], i) => (
            <li key={t} className="rounded-lg bg-raised p-2"><b>{i + 1}. {t}</b><br /><span className="text-muted">{d}</span></li>
          ))}
        </ol>
      )}
    </div>
  )
}

// ------------------------------------------------------------------ log a problem

const PLATFORMS = ['LEETCODE', 'HACKERRANK', 'GEEKSFORGEEKS', 'CODECHEF', 'CODEFORCES', 'INTERVIEWBIT', 'OTHER']
const HINTS = [
  ['NONE', 'No hints: solved it myself'], ['SMALL', 'Small hint / nudge'], ['FULL_SOLUTION', 'Read the full solution'],
] as const

function LogProblem({ onAward }: { onAward: (a: XpAward) => void }) {
  const patterns = useApi<PatternView[]>('/api/patterns')
  const nav = useNavigate()
  const loc = useLocation()
  const fromBank = (loc.state as { bank?: BankProblem } | null)?.bank
  const [f, setF] = useState({
    title: fromBank?.title ?? '', link: fromBank?.url ?? '', platform: fromBank?.platform ?? 'LEETCODE', difficulty: fromBank?.difficulty ?? 'EASY',
    patternCodes: fromBank ? [fromBank.patternCode] : [] as string[], timeTakenMinutes: 30, attempts: 1,
    hintLevel: 'NONE', attemptedBeforeHint: true, confidence: 3, approach: '', code: '', bankSlug: fromBank?.slug ?? null as string | null,
  })
  const [showHints, setShowHints] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState<ProblemSaved | null>(null)
  const [busy, setBusy] = useState(false)
  const set = <K extends keyof typeof f>(k: K, v: (typeof f)[K]) => setF({ ...f, [k]: v })

  async function submit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    if (!f.patternCodes.length) { setError('Pick at least one pattern'); return }
    if (f.approach.trim().length < 20) { setError('Write your approach in your own words (at least 20 characters). This is where the learning happens!'); return }
    setBusy(true)
    try {
      const r = await api<ProblemSaved>('/api/problems', { method: 'POST', body: { ...f, link: f.link.trim() || null, code: f.code || null } })
      setSaved(r)
      const total: XpAward = { awarded: r.approachXp.awarded || r.attemptXp.awarded, xp: r.approachXp.xp + r.attemptXp.xp, gold: r.approachXp.gold + r.attemptXp.gold, reason: r.attemptXp.reason }
      onAward(total)
    } catch (err) { setError(errorMessage(err)) } finally { setBusy(false) }
  }

  if (saved) {
    return (
      <div className="card space-y-3 text-center">
        <div className="text-4xl">🗡️</div>
        <h2 className="text-xl font-bold">Monster defeated: {saved.problem.title}</h2>
        <p className="text-sm">+{saved.approachXp.xp} XP for explaining your approach{saved.attemptXp.awarded ? `, +${saved.attemptXp.xp} XP for trying before hints` : ''}.</p>
        {!saved.attemptXp.awarded && saved.attemptXp.reason && <p className="text-sm text-muted">{saved.attemptXp.reason}</p>}
        <p className="text-sm text-muted">It will come back for a rematch on {saved.revisitDates.join(', ')}. Solve it from scratch then.</p>
        <div className="flex gap-2 justify-center">
          <button className="btn-primary" onClick={() => { setSaved(null); setF({ ...f, title: '', link: '', approach: '', code: '', patternCodes: [], bankSlug: null }) }}>Log another</button>
          <button className="btn-ghost" onClick={() => nav('/gym/mistakes')}>Record a mistake</button>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <FrameworkCard />
      <div className="card">
        <button className="w-full flex justify-between font-semibold" onClick={() => setShowHints(!showHints)} aria-expanded={showHints}>🪜 Stuck? Use the hint ladder (smallest hint first) <span>{showHints ? '−' : '+'}</span></button>
        {showHints && <div className="mt-3"><HintLadder initialTitle={f.title} initialPattern={f.patternCodes[0] ?? ''} /></div>}
      </div>
      <form className="card space-y-4" onSubmit={submit} noValidate>
        <div className="grid sm:grid-cols-2 gap-3">
          <div className="sm:col-span-2">
            <label className="label" htmlFor="pt">Problem title</label>
            <input id="pt" className="input" required maxLength={200} value={f.title} onChange={(e) => set('title', e.target.value)} />
          </div>
          <div className="sm:col-span-2">
            <label className="label" htmlFor="pl">Link (optional)</label>
            <input id="pl" className="input" type="url" maxLength={500} value={f.link} onChange={(e) => set('link', e.target.value)} placeholder="https://" />
          </div>
          <div>
            <label className="label" htmlFor="pf">Platform</label>
            <select id="pf" className="input" value={f.platform} onChange={(e) => set('platform', e.target.value)}>
              {PLATFORMS.map((p) => <option key={p} value={p}>{p.charAt(0) + p.slice(1).toLowerCase()}</option>)}
            </select>
          </div>
          <div>
            <label className="label" htmlFor="pd">Difficulty</label>
            <select id="pd" className="input" value={f.difficulty} onChange={(e) => set('difficulty', e.target.value)}>
              <option value="EASY">Easy</option><option value="MEDIUM">Medium</option><option value="HARD">Hard</option>
            </select>
          </div>
          <div>
            <label className="label" htmlFor="tt">Time taken (minutes)</label>
            <input id="tt" className="input" type="number" min={1} max={600} value={f.timeTakenMinutes} onChange={(e) => set('timeTakenMinutes', +e.target.value)} />
          </div>
          <div>
            <label className="label" htmlFor="at">Attempts (submissions)</label>
            <input id="at" className="input" type="number" min={1} max={50} value={f.attempts} onChange={(e) => set('attempts', +e.target.value)} />
          </div>
        </div>

        <div>
          <p className="label">Patterns used (pick 1-5)</p>
          {patterns.loading ? <Spinner /> : (
            <div className="flex flex-wrap gap-2">
              {patterns.data?.map((p) => (
                <button type="button" key={p.code} title={p.description}
                  onClick={() => set('patternCodes', f.patternCodes.includes(p.code) ? f.patternCodes.filter((c) => c !== p.code) : f.patternCodes.length < 5 ? [...f.patternCodes, p.code] : f.patternCodes)}
                  className={`chip px-3 py-1.5 ${f.patternCodes.includes(p.code) ? 'chip-on' : ''}`}>{p.name}</button>
              ))}
            </div>
          )}
        </div>

        <div>
          <p className="label">How much help did you use?</p>
          <div className="grid sm:grid-cols-3 gap-2">
            {HINTS.map(([v, l]) => (
              <button type="button" key={v} onClick={() => set('hintLevel', v)} className={`rounded-xl p-2 text-sm ${f.hintLevel === v ? 'bg-primary/20 ring-2 ring-primary' : 'bg-raised'}`}>{l}</button>
            ))}
          </div>
          <label className="flex items-center gap-2 mt-2 text-sm">
            <input type="checkbox" checked={f.attemptedBeforeHint} onChange={(e) => set('attemptedBeforeHint', e.target.checked)} className="h-4 w-4" />
            I honestly tried it myself first (at least 15 minutes) before any hint <span className="text-gold">+10 XP</span>
          </label>
        </div>

        <div>
          <p className="label">Confidence: could you solve it again tomorrow without help?</p>
          <Stars label="Confidence" value={f.confidence} onChange={(v) => set('confidence', v)} />
        </div>

        <div>
          <label className="label" htmlFor="ap">My approach, in my own words <span className="text-gold">+15 XP</span></label>
          <textarea id="ap" className="input min-h-28" maxLength={5000} value={f.approach} onChange={(e) => set('approach', e.target.value)}
            placeholder="Explain it like to a friend: what idea solves it, why it works, and the Big-O." />
          <p className="text-xs text-muted mt-1">{f.approach.trim().length}/20 characters minimum</p>
        </div>

        <div>
          <label className="label" htmlFor="cd">My code (optional, type it yourself)</label>
          <textarea id="cd" className="input min-h-32 font-mono text-sm" maxLength={20000} value={f.code} onChange={(e) => set('code', e.target.value)} spellCheck={false} />
        </div>

        {error && <p className="text-bad text-sm whitespace-pre-line" role="alert">{error}</p>}
        <button className="btn-primary w-full" disabled={busy}>{busy ? 'Saving…' : 'Log problem'}</button>
      </form>
    </div>
  )
}

// ------------------------------------------------------------------ list

function ProblemList() {
  const [pattern, setPattern] = useState('')
  const [page, setPage] = useState(0)
  const patterns = useApi<PatternView[]>('/api/patterns')
  const list = useApi<Page<ProblemView>>(`/api/problems?page=${page}&size=15${pattern ? `&pattern=${pattern}` : ''}`)
  const [open, setOpen] = useState<number | null>(null)

  async function del(id: number) {
    if (!confirm('Delete this problem? Its revisits are removed too (XP already earned stays).')) return
    await api(`/api/problems/${id}`, { method: 'DELETE' }).catch(() => undefined)
    void list.reload()
  }

  return (
    <div className="space-y-3">
      <select className="input max-w-xs" value={pattern} onChange={(e) => { setPattern(e.target.value); setPage(0) }} aria-label="Filter by pattern">
        <option value="">All patterns</option>
        {patterns.data?.map((p) => <option key={p.code} value={p.code}>{p.name}</option>)}
      </select>
      {list.loading && !list.data ? <Spinner /> : list.error ? <ErrorBox message={list.error} onRetry={list.reload} /> : list.data!.items.length === 0 ? (
        <div className="card text-center space-y-2"><p>No problems logged yet.</p><Link className="btn-primary" to="/gym/log">Log your first one</Link></div>
      ) : (
        <>
          <ul className="space-y-2">
            {list.data!.items.map((p) => (
              <li key={p.id} className="card">
                <button className="w-full text-left" onClick={() => setOpen(open === p.id ? null : p.id)} aria-expanded={open === p.id}>
                  <div className="flex justify-between gap-2">
                    <span className="font-semibold">{p.title}</span>
                    <span className={`chip ${p.difficulty === 'EASY' ? 'text-good' : p.difficulty === 'MEDIUM' ? 'text-warn' : 'text-bad'}`}>{p.difficulty.toLowerCase()}</span>
                  </div>
                  <div className="flex flex-wrap gap-1 mt-1">{p.patterns.map((pt) => <span key={pt.code} className="chip">{pt.name}</span>)}</div>
                  <p className="text-xs text-muted mt-1">{p.solvedOn} · {p.timeTakenMinutes} min · hint: {p.hintLevel.toLowerCase().replace('_', ' ')} · confidence {p.confidence}/5</p>
                </button>
                {open === p.id && (
                  <div className="mt-3 space-y-2 text-sm border-t border-line/60 pt-3">
                    <p className="whitespace-pre-wrap"><b>Approach:</b> {p.approach}</p>
                    {p.code && <pre className="bg-raised rounded-lg p-3 overflow-x-auto text-xs font-mono">{p.code}</pre>}
                    <div className="flex gap-2">
                      {p.link && <a className="btn-ghost text-sm" href={p.link} target="_blank" rel="noreferrer">Open problem ↗</a>}
                      <button className="btn-ghost text-sm text-bad" onClick={() => del(p.id)}>Delete</button>
                    </div>
                  </div>
                )}
              </li>
            ))}
          </ul>
          <div className="flex justify-between items-center text-sm">
            <button className="btn-ghost" disabled={page === 0} onClick={() => setPage(page - 1)}>← Newer</button>
            <span className="text-muted">Page {page + 1} of {Math.max(1, list.data!.totalPages)}</span>
            <button className="btn-ghost" disabled={page + 1 >= list.data!.totalPages} onClick={() => setPage(page + 1)}>Older →</button>
          </div>
        </>
      )}
    </div>
  )
}

// ------------------------------------------------------------------ pattern map

const STRENGTH_STYLE: Record<string, string> = {
  STRONG: 'bg-good/20 text-good', OK: 'bg-accent/20 text-accent', WEAK: 'bg-warn/20 text-warn', NOT_STARTED: 'bg-raised text-muted',
}

function PatternMapView() {
  const { data, error, loading, reload } = useApi<PatternMap>('/api/gym/pattern-map')
  if (loading && !data) return <Spinner />
  if (error || !data) return <ErrorBox message={error ?? 'No data'} onRetry={reload} />
  return (
    <div className="space-y-4">
      {data.recommendedNext && (
        <div className="card border-primary/50 bg-primary/10">
          <p className="text-xs uppercase text-primary font-semibold">Recommended next pattern</p>
          <p className="text-lg font-semibold">{data.recommendedNext.name}</p>
          <p className="text-sm text-muted">{data.recommendedNext.description}</p>
          <p className="text-sm mt-1">{data.recommendationReason}</p>
        </div>
      )}
      <p className="text-xs text-muted">Strength = how many you solved (up to 5), your confidence, and how often you solved without hints. ⭐ = core for Infosys SP-style prep (our judgment).</p>
      <div className="grid sm:grid-cols-2 gap-2">
        {data.patterns.map((p) => (
          <div key={p.code} className="card flex items-center justify-between gap-2 py-3">
            <div>
              <p className="font-medium">{p.spCore && '⭐ '}{p.name}</p>
              <p className="text-xs text-muted">{p.solved} solved · conf {p.avgConfidence || '–'} · {Math.round(p.unaidedRate * 100)}% no-hint</p>
            </div>
            <span className={`chip ${STRENGTH_STYLE[p.strength]}`}>{p.strength.replace('_', ' ').toLowerCase()}</span>
          </div>
        ))}
      </div>
    </div>
  )
}

// ------------------------------------------------------------------ revisits

function Revisits({ onAward }: { onAward: (a: XpAward) => void }) {
  const due = useApi<RevisitView[]>('/api/revisits/due')
  const upcoming = useApi<RevisitView[]>('/api/revisits/upcoming')
  const [active, setActive] = useState<number | null>(null)
  const [notes, setNotes] = useState('')

  async function complete(id: number, solvedUnaided: boolean) {
    try {
      const r = await api<XpAward>(`/api/revisits/${id}/complete`, { method: 'POST', body: { solvedUnaided, notes: notes || null } })
      onAward(r)
      setActive(null); setNotes('')
      void due.reload(); void upcoming.reload()
    } catch (e) { alert(errorMessage(e)) }
  }

  return (
    <div className="space-y-6">
      <Section title={`Due now (${due.data?.length ?? 0})`}>
        <p className="text-sm text-muted">Re-solve from a blank editor. Don't look at your old code first. This is how it sticks.</p>
        {due.loading && !due.data ? <Spinner /> : due.data?.length === 0 ? <div className="card text-muted">Nothing due. 🎉</div> : (
          <ul className="space-y-2">
            {due.data?.map((r) => (
              <li key={r.id} className="card space-y-2">
                <div className="flex justify-between">
                  <span className="font-semibold">{r.problemTitle}</span>
                  <span className={`chip ${r.overdue ? 'text-warn' : ''}`}>Round {r.round} · {r.dueDate}</span>
                </div>
                {active === r.id ? (
                  <div className="space-y-2">
                    <textarea className="input" placeholder="What did you notice this time? (optional)" value={notes} onChange={(e) => setNotes(e.target.value)} maxLength={2000} />
                    <div className="grid sm:grid-cols-2 gap-2">
                      <button className="btn-primary" onClick={() => complete(r.id, true)}>✅ Solved it without help (+25 XP)</button>
                      <button className="btn-ghost" onClick={() => complete(r.id, false)}>🤝 I needed help (it comes back in 3 days)</button>
                    </div>
                  </div>
                ) : (
                  <div className="flex gap-2">
                    {r.link && <a className="btn-ghost text-sm" href={r.link} target="_blank" rel="noreferrer">Open ↗</a>}
                    <button className="btn-primary text-sm" onClick={() => setActive(r.id)}>I re-solved it</button>
                  </div>
                )}
              </li>
            ))}
          </ul>
        )}
      </Section>
      <Section title="Coming up">
        <ul className="text-sm space-y-1">
          {upcoming.data?.map((r) => <li key={r.id} className="flex justify-between card py-2"><span>{r.problemTitle}</span><span className="text-muted">{r.dueDate}</span></li>)}
          {upcoming.data?.length === 0 && <li className="text-muted">Nothing scheduled.</li>}
        </ul>
      </Section>
    </div>
  )
}

// ------------------------------------------------------------------ mistakes

function Mistakes({ onAward }: { onAward: (a: XpAward) => void }) {
  const cats = useApi<MistakeCategoryView[]>('/api/mistake-categories')
  const top = useApi<MistakeTrend[]>('/api/mistakes/weekly-top')
  const [filter, setFilter] = useState<'open' | 'resolved' | 'all'>('open')
  const list = useApi<Page<MistakeView>>(`/api/mistakes?size=50${filter === 'all' ? '' : `&resolved=${filter === 'resolved'}`}`)
  const [f, setF] = useState({ categoryCode: 'EDGE_CASE', topic: '', whatHappened: '', lessonLearned: '' })
  const [error, setError] = useState<string | null>(null)

  const tip = cats.data?.find((c) => c.code === f.categoryCode)?.tip

  async function submit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    try {
      const r = await api<{ mistake: MistakeView; xp: XpAward }>('/api/mistakes', { method: 'POST', body: { ...f, topic: f.topic || null } })
      onAward(r.xp)
      setF({ ...f, topic: '', whatHappened: '', lessonLearned: '' })
      void list.reload(); void top.reload()
    } catch (err) { setError(errorMessage(err)) }
  }

  async function toggle(m: MistakeView) {
    await api(`/api/mistakes/${m.id}/resolved?value=${!m.resolved}`, { method: 'PATCH' }).catch(() => undefined)
    void list.reload()
  }

  return (
    <div className="space-y-6">
      {top.data && top.data.length > 0 && (
        <Section title="This week's top recurring mistakes">
          <ul className="grid gap-2">
            {top.data.map((t) => (
              <li key={t.categoryCode} className="card py-3"><b>{t.categoryName}</b> ×{t.count}<p className="text-sm text-muted">💡 {t.tip}</p></li>
            ))}
          </ul>
        </Section>
      )}
      <Section title="Record a mistake (+10 XP for honesty)">
        <form className="card space-y-3" onSubmit={submit}>
          <div className="grid sm:grid-cols-2 gap-3">
            <div>
              <label className="label" htmlFor="mc">Type of mistake</label>
              <select id="mc" className="input" value={f.categoryCode} onChange={(e) => setF({ ...f, categoryCode: e.target.value })}>
                {cats.data?.map((c) => <option key={c.code} value={c.code}>{c.name}</option>)}
              </select>
            </div>
            <div>
              <label className="label" htmlFor="mt">Topic (optional)</label>
              <input id="mt" className="input" maxLength={120} value={f.topic} onChange={(e) => setF({ ...f, topic: e.target.value })} placeholder="e.g. Sliding window" />
            </div>
          </div>
          {tip && <p className="text-xs text-accent">💡 {tip}</p>}
          <div>
            <label className="label" htmlFor="mw">What happened?</label>
            <textarea id="mw" className="input" minLength={10} maxLength={2000} value={f.whatHappened} onChange={(e) => setF({ ...f, whatHappened: e.target.value })} />
          </div>
          <div>
            <label className="label" htmlFor="ml">What will you do differently?</label>
            <textarea id="ml" className="input" minLength={5} maxLength={2000} value={f.lessonLearned} onChange={(e) => setF({ ...f, lessonLearned: e.target.value })} />
          </div>
          {error && <p className="text-bad text-sm whitespace-pre-line" role="alert">{error}</p>}
          <button className="btn-primary w-full">Save to notebook</button>
        </form>
      </Section>
      <Section title="Notebook" action={<Tabs tabs={[{ id: 'open', label: 'Open' }, { id: 'resolved', label: 'Resolved' }, { id: 'all', label: 'All' }]} value={filter} onChange={setFilter} />}>
        {list.loading && !list.data ? <Spinner /> : (
          <ul className="space-y-2">
            {list.data?.items.map((m) => (
              <li key={m.id} className="card text-sm space-y-1">
                <div className="flex justify-between gap-2">
                  <span className="chip">{m.categoryName}</span>
                  <button className={`chip px-3 ${m.resolved ? 'text-good' : ''}`} onClick={() => toggle(m)}>{m.resolved ? '✓ Resolved' : 'Mark resolved'}</button>
                </div>
                {m.topic && <p className="text-muted">{m.topic}</p>}
                <p>{m.whatHappened}</p>
                <p className="text-accent">→ {m.lessonLearned}</p>
                <button className="chip mt-1" onClick={async () => { try { await api(`/api/mistakes/${m.id}/flashcard`, { method: 'POST' }); alert('Flashcard created: it will come back for review tomorrow.') } catch (e) { alert(errorMessage(e)) } }}>🃏 Make a flashcard</button>
              </li>
            ))}
            {list.data?.items.length === 0 && <li className="text-muted">Nothing here.</li>}
          </ul>
        )}
      </Section>
    </div>
  )
}

// ------------------------------------------------------------------ suggestions + bank

function Suggested() {
  const list = useApi<Suggestion[]>('/api/problem-bank/suggestions')
  const nav = useNavigate()
  if (list.loading && !list.data) return <Spinner />
  if (list.error) return <ErrorBox message={list.error} onRetry={list.reload} />
  return (
    <div className="space-y-3">
      <p className="text-sm text-muted">Picked from your Pattern Map: shaky patterns first, then the next new core pattern, at the right difficulty. Solve on the site (free), then log it here with your own approach.</p>
      {list.data?.length === 0 && <div className="card">🎉 You've solved everything we'd suggest right now. Try the Problem bank.</div>}
      {list.data?.map(({ problem: p, reason }) => (
        <div key={p.id} className="card space-y-2">
          <div className="flex justify-between gap-2 items-start">
            <div><p className="font-semibold">{p.title}</p><p className="text-xs text-muted">{p.patternName} · {p.platform.toLowerCase()} · {p.difficulty.toLowerCase()}</p></div>
            <DiffChip d={p.difficulty} />
          </div>
          <p className="text-sm">💡 {reason}</p>
          <p className="text-sm text-muted">Trains: {p.why}</p>
          <div className="flex flex-wrap gap-2">
            <a className="btn-primary text-sm" href={p.url} target="_blank" rel="noreferrer">Solve it ↗</a>
            <button className="btn-ghost text-sm" onClick={() => nav('/gym/log', { state: { bank: p } })}>I solved it: log it</button>
          </div>
        </div>
      ))}
      <p className="text-xs text-muted">Practice problems from free public sites, chosen for the Infosys style of preparation. Not claimed to be actual test questions.</p>
    </div>
  )
}

function Bank() {
  const [pattern, setPattern] = useState('')
  const [difficulty, setDifficulty] = useState('')
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const nav = useNavigate()
  const cov = useApi<PatternCoverage[]>('/api/problem-bank/coverage')
  const list = useApi<Page<BankProblem>>(`/api/problem-bank?page=${page}&size=25&pattern=${pattern}&difficulty=${difficulty}&status=${status}`)
  return (
    <div className="space-y-3">
      <div className="grid sm:grid-cols-3 gap-2">
        <select className="input" value={pattern} onChange={(e) => { setPattern(e.target.value); setPage(0) }} aria-label="Pattern">
          <option value="">All patterns</option>
          {cov.data?.map((c) => <option key={c.code} value={c.code}>{c.name} ({c.solved}/{c.total})</option>)}
        </select>
        <select className="input" value={difficulty} onChange={(e) => { setDifficulty(e.target.value); setPage(0) }} aria-label="Difficulty">
          <option value="">All levels</option><option value="EASY">Easy</option><option value="MEDIUM">Medium</option><option value="HARD">Hard</option>
        </select>
        <select className="input" value={status} onChange={(e) => { setStatus(e.target.value); setPage(0) }} aria-label="Status">
          <option value="">All</option><option value="UNSOLVED">Not solved yet</option><option value="SOLVED">Solved</option>
        </select>
      </div>
      {list.loading && !list.data ? <Spinner /> : (
        <ul className="space-y-2">
          {list.data?.items.map((p) => (
            <li key={p.id} className={`card flex items-center gap-3 ${p.solved ? 'opacity-70' : ''}`}>
              <span aria-hidden>{p.solved ? '✅' : '⬜'}</span>
              <div className="flex-1 min-w-0">
                <a href={p.url} target="_blank" rel="noreferrer" className="font-medium hover:text-primary">{p.title} ↗</a>
                <p className="text-xs text-muted truncate">{p.patternName} · {p.why}</p>
              </div>
              <DiffChip d={p.difficulty} />
              {!p.solved && <button className="chip px-3" onClick={() => nav('/gym/log', { state: { bank: p } })}>Log</button>}
            </li>
          ))}
        </ul>
      )}
      {list.data && list.data.totalPages > 1 && (
        <div className="flex justify-between items-center text-sm">
          <button className="btn-ghost" disabled={page === 0} onClick={() => setPage(page - 1)}>←</button>
          <span className="text-muted">Page {page + 1} of {list.data.totalPages} · {list.data.totalItems} problems</span>
          <button className="btn-ghost" disabled={page + 1 >= list.data.totalPages} onClick={() => setPage(page + 1)}>→</button>
        </div>
      )}
    </div>
  )
}

function DiffChip({ d }: { d: string }) {
  return <span className={`chip ${d === 'EASY' ? 'text-good' : d === 'MEDIUM' ? 'text-warn' : 'text-bad'}`}>{d.toLowerCase()}</span>
}
