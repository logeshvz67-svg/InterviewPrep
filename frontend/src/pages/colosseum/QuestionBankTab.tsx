import { useEffect, useState } from 'react'
import { api } from '../../api/client'
import type { CompanyView, Page } from '../../api/types'
import { ErrorBox, Spinner } from '../../components/ui'
import { errorMessage, useApi } from '../../lib/hooks'
import { niceCode } from './shared'
import type { BankQuestion, PracticeResult, XpAward } from './types'

const CATEGORIES = ['JAVA', 'OOP', 'COLLECTIONS', 'EXCEPTIONS', 'JAVA8', 'JVM', 'MULTITHREADING', 'SPRING', 'SQL', 'DBMS', 'DSA', 'OS', 'CN', 'HR', 'BEHAVIORAL', 'PROJECT']
const DIFFICULTIES = ['EASY', 'MEDIUM', 'HARD']
const SIZE = 10

export default function QuestionBankTab({ onAward }: { onAward: (a: XpAward) => void }) {
  const companies = useApi<CompanyView[]>('/api/companies')
  const [category, setCategory] = useState('')
  const [difficulty, setDifficulty] = useState('')
  const [company, setCompany] = useState('')
  const [search, setSearch] = useState('')
  const [q, setQ] = useState('')
  const [page, setPage] = useState(0)

  // Wait until typing stops before searching.
  useEffect(() => {
    const t = setTimeout(() => { setQ(search.trim()); setPage(0) }, 350)
    return () => clearTimeout(t)
  }, [search])

  const params = new URLSearchParams({ page: String(page), size: String(SIZE) })
  if (category) params.set('category', category)
  if (difficulty) params.set('difficulty', difficulty)
  if (company) params.set('company', company)
  if (q) params.set('q', q)
  const list = useApi<Page<BankQuestion>>(`/api/question-bank?${params.toString()}`)
  const companyName = companies.data?.find((c) => c.code === company)?.name

  const reset = (fn: () => void) => { fn(); setPage(0) }

  return (
    <div className="space-y-4">
      <div className="card border-warn/50 text-sm" role="note">
        ℹ️ {companyName
          ? <>These are <b>practice questions in the style of {companyName}</b>, not actual questions from their interviews.</>
          : <>These are <b>practice questions</b> in the style of common interviews, not actual questions from any company.</>}
      </div>

      <div className="card grid gap-3 sm:grid-cols-2">
        <div>
          <label className="label" htmlFor="qb-cat">Category</label>
          <select id="qb-cat" className="input" value={category} onChange={(e) => reset(() => setCategory(e.target.value))}>
            <option value="">All categories</option>
            {CATEGORIES.map((c) => <option key={c} value={c}>{niceCode(c)}</option>)}
          </select>
        </div>
        <div>
          <label className="label" htmlFor="qb-diff">Difficulty</label>
          <select id="qb-diff" className="input" value={difficulty} onChange={(e) => reset(() => setDifficulty(e.target.value))}>
            <option value="">Any</option>
            {DIFFICULTIES.map((d) => <option key={d} value={d}>{niceCode(d)}</option>)}
          </select>
        </div>
        <div>
          <label className="label" htmlFor="qb-co">Company style</label>
          <select id="qb-co" className="input" value={company} onChange={(e) => reset(() => setCompany(e.target.value))}>
            <option value="">Any company</option>
            {companies.data?.map((c) => <option key={c.code} value={c.code}>{c.name}</option>)}
          </select>
        </div>
        <div>
          <label className="label" htmlFor="qb-q">Search</label>
          <input id="qb-q" className="input" type="search" placeholder="e.g. HashMap" value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
      </div>

      {list.loading && !list.data && <Spinner />}
      {list.error && <ErrorBox message={list.error} onRetry={list.reload} />}
      {list.data && (
        <>
          <p className="text-sm text-muted" aria-live="polite">{list.data.totalItems} practice question{list.data.totalItems === 1 ? '' : 's'}</p>
          <ul className="space-y-3">
            {list.data.items.map((item) => <QuestionCard key={item.id} q={item} onAward={onAward} />)}
          </ul>
          {list.data.items.length === 0 && <p className="card text-muted text-sm">No questions match. Try fewer filters.</p>}
          {list.data.totalPages > 1 && (
            <nav className="flex items-center justify-between" aria-label="Pages">
              <button className="btn-ghost text-sm" disabled={page === 0} onClick={() => setPage(page - 1)}>← Previous</button>
              <span className="text-sm text-muted">Page {page + 1} of {list.data.totalPages}</span>
              <button className="btn-ghost text-sm" disabled={page + 1 >= list.data.totalPages} onClick={() => setPage(page + 1)}>Next →</button>
            </nav>
          )}
        </>
      )}
    </div>
  )
}

function QuestionCard({ q, onAward }: { q: BankQuestion; onAward: (a: XpAward) => void }) {
  const [open, setOpen] = useState(false)
  const [mine, setMine] = useState('')
  const [revealed, setRevealed] = useState(false)
  const [covered, setCovered] = useState<number[]>([])
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState<string | null>(null)
  const [result, setResult] = useState<PracticeResult | null>(null)
  const best = result ? Math.max(Number(result.score), Number(q.myBestScore ?? 0)) : q.myBestScore

  const toggle = (i: number) => setCovered((c) => (c.includes(i) ? c.filter((x) => x !== i) : [...c, i]))

  const save = async () => {
    setBusy(true); setErr(null)
    try {
      const res = await api<PracticeResult>(`/api/question-bank/${q.id}/practice`, {
        method: 'POST', body: { myAnswer: mine.trim() || null, keyPointsCovered: covered.filter((i) => i <= 5).slice(0, 6) },
      })
      setResult(res)
      onAward(res.xp)
    } catch (e) { setErr(errorMessage(e)) } finally { setBusy(false) }
  }

  return (
    <li className="card space-y-3">
      <button className="w-full text-left" onClick={() => setOpen(!open)} aria-expanded={open}>
        <div className="flex flex-wrap gap-1.5 mb-1">
          <span className="chip">{niceCode(q.category)}</span>
          <span className={`chip ${q.difficulty === 'HARD' ? 'text-bad' : q.difficulty === 'MEDIUM' ? 'text-warn' : 'text-good'}`}>{niceCode(q.difficulty)}</span>
          {q.topic && <span className="chip">{q.topic}</span>}
          {best !== null && best !== undefined && <span className="chip text-gold">Best {Math.round(Number(best))}%</span>}
        </div>
        <p className="font-medium">{q.question}</p>
      </button>

      {open && (
        <div className="space-y-3">
          <div>
            <label className="label" htmlFor={`qb-ans-${q.id}`}>Try answering first, in your own simple words</label>
            <textarea id={`qb-ans-${q.id}`} className="input min-h-[110px]" value={mine} onChange={(e) => setMine(e.target.value)}
              placeholder="Write 3-5 lines. It's fine if it's not perfect." maxLength={6000} />
          </div>
          {!revealed
            ? <button className="btn-primary" onClick={() => setRevealed(true)}>👀 Reveal answer</button>
            : (
              <div className="space-y-3">
                <div className="rounded-xl bg-raised p-3 text-sm whitespace-pre-wrap">
                  <b className="block mb-1">Model answer</b>{q.answer}
                </div>
                {q.keyPoints.length > 0 && (
                  <fieldset className="space-y-2">
                    <legend className="font-semibold text-sm mb-1">Tick what you covered</legend>
                    {q.keyPoints.map((k, i) => (
                      <label key={i} className="flex items-start gap-2 text-sm cursor-pointer">
                        <input type="checkbox" className="mt-1 accent-primary" checked={covered.includes(i)} onChange={() => toggle(i)} disabled={!!result} />
                        <span>{k}</span>
                      </label>
                    ))}
                  </fieldset>
                )}
                {err && <ErrorBox message={err} />}
                {result
                  ? (
                    <div className="rounded-xl border border-good/50 bg-good/10 p-3 text-sm" role="status">
                      <b>Score: {Math.round(Number(result.score))}%</b> · {result.practisedToday} practised today.
                      <button className="btn-ghost text-sm ml-3" onClick={() => { setResult(null); setCovered([]); setMine(''); setRevealed(false) }}>Try again</button>
                    </div>
                  )
                  : <button className="btn-gold" disabled={busy} onClick={save}>{busy ? 'Saving…' : '✓ Save my practice'}</button>}
              </div>
            )}
        </div>
      )}
    </li>
  )
}
