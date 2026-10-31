import { useState } from 'react'
import { ErrorBox, Spinner } from '../../components/ui'
import { useApi } from '../../lib/hooks'
import { CodeBlock, Explanation, Options } from './shared'
import type { AnnotationCard, DebugChallenge, McqQuiz } from './types'

/** One MCQ checked in the browser (the answer is part of this public reference content). */
function LocalMcq({ id, quiz }: { id: string; quiz: McqQuiz }) {
  const [pick, setPick] = useState<number | null>(null)
  const [checked, setChecked] = useState(false)
  return (
    <div className="space-y-2">
      <p className="text-sm font-medium">{quiz.prompt}</p>
      <Options name={id} options={quiz.options} value={pick} onChange={(v) => { setPick(v); setChecked(false) }} answer={checked ? quiz.answer : undefined} />
      {checked
        ? <Explanation correct={pick === quiz.answer}>{quiz.explanation}</Explanation>
        : <button className="btn-ghost text-sm" disabled={pick === null} onClick={() => setChecked(true)}>Check</button>}
    </div>
  )
}

export function AnnotationSpells() {
  const list = useApi<AnnotationCard[]>('/api/spring/annotations')
  const [cat, setCat] = useState('')
  const [q, setQ] = useState('')
  if (list.loading && !list.data) return <Spinner />
  if (list.error) return <ErrorBox message={list.error} onRetry={list.reload} />
  const all = list.data ?? []
  const cats = [...new Set(all.map((a) => a.category))]
  const query = q.trim().toLowerCase()
  const shown = all.filter((a) => (!cat || a.category === cat) && (!query || (a.name + ' ' + a.meaning).toLowerCase().includes(query)))

  return (
    <div className="space-y-4">
      <div className="space-y-2">
        <label className="sr-only" htmlFor="spell-q">Search annotations</label>
        <input id="spell-q" className="input" type="search" placeholder="Search, e.g. @Transactional" value={q} onChange={(e) => setQ(e.target.value)} />
        <div className="flex flex-wrap gap-1.5">
          {['', ...cats].map((c) => (
            <button key={c || 'all'} aria-pressed={cat === c} onClick={() => setCat(c)} className={`chip px-3 py-1 ${cat === c ? 'chip-on' : ''}`}>{c || 'All'}</button>
          ))}
        </div>
      </div>
      <ul className="grid gap-3 lg:grid-cols-2">
        {shown.map((a, i) => (
          <li key={a.name + i} className="card space-y-3 min-w-0">
            <div className="flex items-center justify-between gap-2">
              <h3 className="font-mono font-bold text-primary text-lg break-all">{a.name}</h3>
              <span className="chip">{a.category}</span>
            </div>
            <p className="text-sm">{a.meaning}</p>
            <p className="text-sm text-muted">💡 <i>{a.analogy}</i></p>
            {a.snippet && <CodeBlock code={a.snippet} label={`Example of ${a.name}`} />}
            {a.quiz && <LocalMcq id={`spell-${i}`} quiz={a.quiz} />}
          </li>
        ))}
      </ul>
      {shown.length === 0 && <p className="card text-sm text-muted">No spells match.</p>}
    </div>
  )
}

export function DebugChallenges() {
  const list = useApi<DebugChallenge[]>('/api/spring/debug-challenges')
  if (list.loading && !list.data) return <Spinner />
  if (list.error) return <ErrorBox message={list.error} onRetry={list.reload} />
  return (
    <div className="space-y-4">
      <p className="text-sm text-muted">Read the error first. Then the code. Then guess the cause. This is exactly how you debug real apps.</p>
      <ul className="space-y-4">
        {(list.data ?? []).map((c) => <DebugCard key={c.id} c={c} />)}
      </ul>
    </div>
  )
}

function DebugCard({ c }: { c: DebugChallenge }) {
  const [pick, setPick] = useState<number | null>(null)
  const [revealed, setRevealed] = useState(false)
  return (
    <li className="card space-y-3 min-w-0">
      <h3 className="font-semibold">🐞 {c.title}</h3>
      <div>
        <p className="label">The error</p>
        <pre className="font-mono text-xs bg-bad/10 border border-bad/40 text-ink rounded-xl p-3 overflow-x-auto whitespace-pre" tabIndex={0} aria-label="Error output">{c.error.replace(/\n$/, '')}</pre>
      </div>
      <div>
        <p className="label">The code</p>
        <CodeBlock code={c.code} label="Buggy code" />
      </div>
      <div className="space-y-2">
        <p className="text-sm font-medium">{c.question || 'What is the cause?'}</p>
        <Options name={`dbg-${c.id}`} options={c.options} value={pick} onChange={(v) => { setPick(v); setRevealed(false) }} answer={revealed ? c.answer : undefined} />
      </div>
      {!revealed
        ? <button className="btn-primary text-sm" onClick={() => setRevealed(true)}>{pick === null ? 'Show the answer' : 'Check + show fix'}</button>
        : (
          <>
            <Explanation correct={pick === null ? undefined : pick === c.answer}>{c.explanation}</Explanation>
            {c.fix && <div><p className="label">🔧 The fix</p><CodeBlock code={c.fix} label="Fix" /></div>}
          </>
        )}
    </li>
  )
}
