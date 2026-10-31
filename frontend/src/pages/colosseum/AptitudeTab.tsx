import { useEffect, useRef, useState } from 'react'
import { api } from '../../api/client'
import { ErrorBox, Section, Spinner } from '../../components/ui'
import { errorMessage, useApi } from '../../lib/hooks'
import { Explanation, Options, niceCode } from './shared'
import type { AptitudeGraded, AptitudeItem, TopicAccuracy, XpAward } from './types'

const SECTIONS: Record<string, string[]> = {
  QUANT: ['Averages', 'Compound Interest', 'Number System', 'Percentages', 'Permutations and Combinations', 'Probability',
    'Profit and Loss', 'Ratio and Proportion', 'Simple Interest', 'Time Speed Distance', 'Time and Work'],
  LOGICAL: ['Blood Relations', 'Calendars', 'Clocks', 'Coding-Decoding', 'Data Sufficiency', 'Direction Sense', 'Letter Series',
    'Number Series', 'Odd One Out', 'Puzzles', 'Ranking and Order', 'Seating Arrangement', 'Syllogism'],
  VERBAL: ['Antonyms', 'Error Spotting', 'Fill in the Blank', 'Para Jumbles', 'Reading Comprehension', 'Sentence Correction', 'Synonyms'],
}

type Phase = { kind: 'setup' } | { kind: 'play'; items: AptitudeItem[] } | { kind: 'done'; items: AptitudeItem[]; chosen: (number | null)[]; graded: AptitudeGraded }

export default function AptitudeTab({ onAward }: { onAward: (a: XpAward) => void }) {
  const [section, setSection] = useState('')
  const [topic, setTopic] = useState('')
  const [phase, setPhase] = useState<Phase>({ kind: 'setup' })
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState<string | null>(null)
  const stats = useApi<TopicAccuracy[]>('/api/aptitude/stats')

  const start = async () => {
    setBusy(true); setErr(null)
    try {
      const p = new URLSearchParams({ count: '10' })
      if (section) p.set('section', section)
      if (topic) p.set('topic', topic)
      const items = await api<AptitudeItem[]>(`/api/aptitude/set?${p.toString()}`)
      if (items.length === 0) setErr('No questions for this choice yet. Pick another topic.')
      else setPhase({ kind: 'play', items })
    } catch (e) { setErr(errorMessage(e)) } finally { setBusy(false) }
  }

  if (phase.kind === 'play') {
    return <Play items={phase.items} onCancel={() => setPhase({ kind: 'setup' })}
      onGraded={(chosen, graded) => { setPhase({ kind: 'done', items: phase.items, chosen, graded }); onAward(graded.xp); void stats.reload() }} />
  }

  return (
    <div className="space-y-5">
      {phase.kind === 'done' && <Results items={phase.items} graded={phase.graded} onAgain={() => setPhase({ kind: 'setup' })} />}

      {phase.kind === 'setup' && (
        <div className="card space-y-4">
          <h2 className="text-lg font-semibold">🎯 Start a 10-question set</h2>
          <fieldset>
            <legend className="label">Section</legend>
            <div className="flex flex-wrap gap-2">
              {['', ...Object.keys(SECTIONS)].map((s) => (
                <button key={s || 'ALL'} type="button" aria-pressed={section === s} onClick={() => { setSection(s); setTopic('') }}
                  className={`chip px-3 py-1.5 text-sm ${section === s ? 'chip-on' : ''}`}>{s ? niceCode(s) : 'Mixed'}</button>
              ))}
            </div>
          </fieldset>
          {section && (
            <div>
              <label className="label" htmlFor="apt-topic">Topic</label>
              <select id="apt-topic" className="input" value={topic} onChange={(e) => setTopic(e.target.value)}>
                <option value="">All {niceCode(section)} topics</option>
                {SECTIONS[section].map((t) => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
          )}
          <p className="text-sm text-muted">Each question has its own timer. No pressure: it just helps you see which topics are slow.</p>
          {err && <ErrorBox message={err} />}
          <button className="btn-primary" disabled={busy} onClick={start}>{busy ? 'Loading…' : '▶ Start'}</button>
        </div>
      )}

      <Section title="📈 My accuracy by topic">
        {stats.loading && !stats.data && <Spinner />}
        {stats.error && <ErrorBox message={stats.error} onRetry={stats.reload} />}
        {stats.data && stats.data.length === 0 && <p className="card text-sm text-muted">No attempts yet. Your first set will show up here.</p>}
        {stats.data && stats.data.length > 0 && (
          <div className="card p-0 overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="text-muted text-left">
                <tr><th scope="col" className="p-3">Section</th><th scope="col" className="p-3">Topic</th><th scope="col" className="p-3 text-right">Tried</th><th scope="col" className="p-3 text-right">Accuracy</th></tr>
              </thead>
              <tbody>
                {stats.data.map((s) => {
                  const acc = s.attempts ? Math.round((s.correct * 100) / s.attempts) : 0
                  return (
                    <tr key={s.section + s.topic} className="border-t border-line/60">
                      <td className="p-3">{niceCode(s.section)}</td>
                      <td className="p-3">{s.topic}</td>
                      <td className="p-3 text-right">{s.attempts}</td>
                      <td className={`p-3 text-right font-semibold ${acc >= 70 ? 'text-good' : acc >= 40 ? 'text-warn' : 'text-bad'}`}>{acc}%</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </Section>
    </div>
  )
}

function Play({ items, onCancel, onGraded }: {
  items: AptitudeItem[]; onCancel: () => void; onGraded: (chosen: (number | null)[], g: AptitudeGraded) => void
}) {
  const [idx, setIdx] = useState(0)
  const [chosen, setChosen] = useState<(number | null)[]>(() => items.map(() => null))
  const [seconds, setSeconds] = useState<number[]>(() => items.map(() => 0))
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState<string | null>(null)
  const idxRef = useRef(idx)
  idxRef.current = idx

  // Per-question timer: adds a second to the question on screen.
  useEffect(() => {
    const t = setInterval(() => {
      if (document.hidden) return
      setSeconds((s) => s.map((v, i) => (i === idxRef.current ? Math.min(3600, v + 1) : v)))
    }, 1000)
    return () => clearInterval(t)
  }, [])

  const q = items[idx]
  const answered = chosen.filter((c) => c !== null).length

  const submit = async () => {
    setBusy(true); setErr(null)
    try {
      const g = await api<AptitudeGraded>('/api/aptitude/submit', {
        method: 'POST', body: { answers: items.map((it, i) => ({ questionId: it.id, chosen: chosen[i], seconds: seconds[i] })) },
      })
      onGraded(chosen, g)
    } catch (e) { setErr(errorMessage(e)) } finally { setBusy(false) }
  }

  return (
    <div className="card space-y-4">
      <div className="flex items-center justify-between text-sm">
        <span className="font-semibold">Question {idx + 1} of {items.length}</span>
        <span className="chip" aria-label={`Time on this question ${seconds[idx]} seconds`}>⏱ {fmt(seconds[idx])}</span>
      </div>
      <div className="h-1.5 rounded-full bg-raised overflow-hidden" aria-hidden>
        <div className="h-full bg-primary transition-all" style={{ width: `${(answered * 100) / items.length}%` }} />
      </div>
      <div className="flex flex-wrap gap-1.5">
        <span className="chip">{niceCode(q.section)}</span><span className="chip">{q.topic}</span>
      </div>
      <p className="font-medium whitespace-pre-wrap">{q.question}</p>
      <Options name={`apt-${q.id}`} options={q.options} value={chosen[idx]}
        onChange={(i) => setChosen((c) => c.map((v, j) => (j === idx ? i : v)))} />
      {err && <ErrorBox message={err} />}
      <div className="flex flex-wrap gap-2 justify-between">
        <button className="btn-ghost text-sm" disabled={idx === 0} onClick={() => setIdx(idx - 1)}>← Back</button>
        {idx + 1 < items.length
          ? <button className="btn-primary text-sm" onClick={() => setIdx(idx + 1)}>{chosen[idx] === null ? 'Skip →' : 'Next →'}</button>
          : <button className="btn-gold text-sm" disabled={busy} onClick={submit}>{busy ? 'Checking…' : `Submit (${answered}/${items.length})`}</button>}
      </div>
      <div className="flex justify-between items-center text-xs">
        <button className="text-muted underline" onClick={onCancel}>Quit set</button>
        {idx + 1 < items.length && <button className="text-primary underline" disabled={busy} onClick={submit}>Submit now</button>}
      </div>
    </div>
  )
}

function Results({ items, graded, onAgain }: { items: AptitudeItem[]; graded: AptitudeGraded; onAgain: () => void }) {
  const byId = new Map(graded.results.map((r) => [r.questionId, r]))
  return (
    <div className="space-y-3">
      <div className="card flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold">You got {graded.correct} of {graded.total}</h2>
          <p className="text-sm text-muted">{Math.round(Number(graded.percent))}% · read the explanations below, they are where the learning is.</p>
        </div>
        <button className="btn-primary" onClick={onAgain}>New set</button>
      </div>
      <ol className="space-y-3">
        {items.map((it, i) => {
          const r = byId.get(it.id)
          return (
            <li key={it.id} className="card space-y-2">
              <p className="text-sm font-medium"><span className="text-muted">{i + 1}.</span> <span className="whitespace-pre-wrap">{it.question}</span></p>
              {r && <Options name={`apt-r-${it.id}`} options={it.options} value={r.chosen} answer={r.answer} onChange={() => undefined} disabled />}
              {r && <Explanation correct={r.correct}>{r.chosen === null && !r.correct ? 'Skipped. ' : ''}{r.explanation}</Explanation>}
            </li>
          )
        })}
      </ol>
    </div>
  )
}

function fmt(s: number) {
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`
}
