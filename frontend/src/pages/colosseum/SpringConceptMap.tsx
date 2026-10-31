import { useEffect, useRef, useState } from 'react'
import { api } from '../../api/client'
import { ErrorBox, Spinner } from '../../components/ui'
import { errorMessage, useApi } from '../../lib/hooks'
import { Explanation, Options } from './shared'
import type { ExplainOutcome, QuizOutcome, SpringStatus, TopicCard, TopicDetail, XpAward } from './types'

const GROUPS = ['Core', 'Web', 'Data', 'Security', 'Testing', 'Ops']
const GROUP_ICON: Record<string, string> = { Core: '⚙️', Web: '🌐', Data: '🗄️', Security: '🔐', Testing: '🧪', Ops: '🚀' }

const STATUS_STYLE: Record<SpringStatus, { label: string; node: string; dot: string }> = {
  NOT_STARTED: { label: 'Not started', node: 'border-line bg-raised text-muted', dot: 'bg-line' },
  LEARNING: { label: 'Learning', node: 'border-warn/70 bg-warn/10', dot: 'bg-warn' },
  UNDERSTOOD: { label: 'Understood', node: 'border-accent/70 bg-accent/10', dot: 'bg-accent' },
  CAN_EXPLAIN: { label: 'Can explain', node: 'border-primary bg-primary/15', dot: 'bg-primary' },
}

export default function SpringConceptMap({ onAward }: { onAward: (a: XpAward) => void }) {
  const topics = useApi<TopicCard[]>('/api/spring/topics')
  const [selected, setSelected] = useState<string | null>(null)
  const detailRef = useRef<HTMLDivElement>(null)

  useEffect(() => { if (selected) detailRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' }) }, [selected])

  if (topics.loading && !topics.data) return <Spinner />
  if (topics.error) return <ErrorBox message={topics.error} onRetry={topics.reload} />
  const list = topics.data ?? []
  const groups = [...GROUPS, ...new Set(list.map((t) => t.group).filter((g) => !GROUPS.includes(g)))]
  const counts = list.reduce<Record<string, number>>((m, t) => ({ ...m, [t.status]: (m[t.status] ?? 0) + 1 }), {})

  const updateCard = (c: TopicCard) => topics.setData((prev) => (prev ? prev.map((t) => (t.code === c.code ? c : t)) : prev))

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap gap-3 text-xs" aria-label="Legend">
        {(Object.keys(STATUS_STYLE) as SpringStatus[]).map((s) => (
          <span key={s} className="flex items-center gap-1.5">
            <span className={`h-2.5 w-2.5 rounded-full ${STATUS_STYLE[s].dot}`} aria-hidden />{STATUS_STYLE[s].label} ({counts[s] ?? 0})
          </span>
        ))}
      </div>

      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {groups.map((g) => {
          const items = list.filter((t) => t.group === g).sort((a, b) => a.order - b.order)
          if (items.length === 0) return null
          return (
            <section key={g} className="card" aria-labelledby={`grp-${g}`}>
              <h3 id={`grp-${g}`} className="font-semibold mb-3">{GROUP_ICON[g] ?? '📦'} {g}</h3>
              <ol className="relative space-y-2 before:absolute before:left-[11px] before:top-2 before:bottom-2 before:w-0.5 before:bg-line/70">
                {items.map((t) => {
                  const st = STATUS_STYLE[t.status] ?? STATUS_STYLE.NOT_STARTED
                  return (
                    <li key={t.code} className="relative pl-8">
                      <span className={`absolute left-1 top-3 h-4 w-4 rounded-full ring-4 ring-surface ${st.dot}`} aria-hidden />
                      <button onClick={() => setSelected(t.code)} aria-pressed={selected === t.code}
                        className={`w-full text-left rounded-xl border px-3 py-2 text-sm transition hover:brightness-110 ${st.node} ${selected === t.code ? 'ring-2 ring-primary' : ''}`}>
                        <span className="font-medium text-ink block">{t.title}</span>
                        <span className="text-xs text-muted">{st.label} · {t.checklistDone}/{t.checklistTotal} checklist</span>
                      </button>
                    </li>
                  )
                })}
              </ol>
            </section>
          )
        })}
      </div>

      <div ref={detailRef} className="scroll-mt-4">
        {selected && <TopicPanel key={selected} code={selected} onCard={updateCard} onAward={onAward} onClose={() => setSelected(null)} />}
      </div>
    </div>
  )
}

function TopicPanel({ code, onCard, onAward, onClose }: {
  code: string; onCard: (c: TopicCard) => void; onAward: (a: XpAward) => void; onClose: () => void
}) {
  const d = useApi<TopicDetail>(`/api/spring/topics/${encodeURIComponent(code)}`)
  const [err, setErr] = useState<string | null>(null)

  if (d.loading && !d.data) return <Spinner />
  if (d.error) return <ErrorBox message={d.error} onRetry={d.reload} />
  if (!d.data) return null
  const t = d.data
  const st = STATUS_STYLE[t.card.status] ?? STATUS_STYLE.NOT_STARTED

  const apply = (card: TopicCard, extra?: Partial<TopicDetail>) => {
    onCard(card)
    d.setData((p) => (p ? { ...p, ...extra, card } : p))
  }

  const toggle = async (i: number) => {
    setErr(null)
    try {
      const card = await api<TopicCard>(`/api/spring/topics/${encodeURIComponent(code)}/checklist/${i}`, { method: 'POST' })
      const done = t.checklistDone.includes(i) ? t.checklistDone.filter((x) => x !== i) : [...t.checklistDone, i]
      apply(card, { checklistDone: done })
    } catch (e) { setErr(errorMessage(e)) }
  }

  const startLearning = async () => {
    setErr(null)
    try {
      apply(await api<TopicCard>(`/api/spring/topics/${encodeURIComponent(code)}/status?status=LEARNING`, { method: 'PUT' }))
    } catch (e) { setErr(errorMessage(e)) }
  }

  return (
    <article className="card space-y-5 border-primary/50" aria-labelledby="topic-title">
      <header className="flex items-start justify-between gap-3">
        <div>
          <h2 id="topic-title" className="text-xl font-bold">{t.card.title}</h2>
          <span className={`chip mt-1 ${st.node}`}>{st.label}</span>
          {Number(t.card.bestQuizPercent) > 0 && <span className="chip ml-1">Best quiz {Math.round(Number(t.card.bestQuizPercent))}%</span>}
        </div>
        <button className="btn-ghost text-sm" onClick={onClose} aria-label="Close topic">✕</button>
      </header>

      <p className="text-sm leading-relaxed whitespace-pre-wrap">{t.card.summary}</p>
      {t.card.status === 'NOT_STARTED' && <button className="btn-primary" onClick={startLearning}>📖 I'm learning this</button>}
      {err && <ErrorBox message={err} />}

      {t.checklist.length > 0 && (
        <fieldset className="space-y-2">
          <legend className="font-semibold mb-1">✅ I can…</legend>
          {t.checklist.map((c, i) => (
            <label key={i} className="flex items-start gap-2 text-sm cursor-pointer">
              <input type="checkbox" className="mt-1 accent-primary" checked={t.checklistDone.includes(i)} onChange={() => void toggle(i)} />
              <span>{c}</span>
            </label>
          ))}
        </fieldset>
      )}

      {t.quiz.length > 0 && <TopicQuiz code={code} detail={t} onDone={(o) => { onAward(o.xp); apply({ ...t.card, status: o.status, bestQuizPercent: Math.max(Number(t.card.bestQuizPercent), Number(o.percent)) }) }} />}

      <ExplainBack code={code} detail={t} onDone={(o) => { onAward(o.xp); apply({ ...t.card, status: o.status }) }} />
    </article>
  )
}

function TopicQuiz({ code, detail, onDone }: { code: string; detail: TopicDetail; onDone: (o: QuizOutcome) => void }) {
  const [answers, setAnswers] = useState<(number | null)[]>(() => detail.quiz.map(() => null))
  const [out, setOut] = useState<QuizOutcome | null>(null)
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState<string | null>(null)

  const submit = async () => {
    setBusy(true); setErr(null)
    try {
      const o = await api<QuizOutcome>(`/api/spring/topics/${encodeURIComponent(code)}/quiz`, {
        method: 'POST', body: { answers: answers.map((a) => (a === null ? -1 : a)) },
      })
      setOut(o); onDone(o)
    } catch (e) { setErr(errorMessage(e)) } finally { setBusy(false) }
  }

  return (
    <section className="space-y-3" aria-labelledby={`quiz-${code}`}>
      <h3 id={`quiz-${code}`} className="font-semibold">🧠 Quick quiz ({detail.quiz.length} questions) <span className="text-xs text-muted font-normal">2 of 3 right = Understood</span></h3>
      {detail.quiz.map((q, i) => {
        const r = out?.results[i]
        return (
          <div key={i} className="space-y-2">
            <p className="text-sm font-medium"><span className="text-muted">{i + 1}.</span> {q.prompt}</p>
            <Options name={`sq-${code}-${i}`} options={q.options} value={answers[i]} answer={r?.answer} disabled={!!out}
              onChange={(v) => setAnswers((a) => a.map((x, j) => (j === i ? v : x)))} />
            {r && <Explanation correct={r.correct}>{r.explanation}</Explanation>}
          </div>
        )
      })}
      {err && <ErrorBox message={err} />}
      {out
        ? (
          <div className="flex flex-wrap items-center gap-3">
            <span className={`font-semibold ${out.passed ? 'text-good' : 'text-warn'}`}>{out.correct}/{out.total} correct{out.passed ? ' 🎉' : ''}</span>
            <button className="btn-ghost text-sm" onClick={() => { setOut(null); setAnswers(detail.quiz.map(() => null)) }}>Try again</button>
          </div>
        )
        : <button className="btn-primary" disabled={busy || answers.some((a) => a === null)} onClick={submit}>{busy ? 'Checking…' : 'Check answers'}</button>}
    </section>
  )
}

function ExplainBack({ code, detail, onDone }: { code: string; detail: TopicDetail; onDone: (o: ExplainOutcome) => void }) {
  const [text, setText] = useState(detail.explainBack ?? '')
  const [ticked, setTicked] = useState<number[]>([])
  const [useAi, setUseAi] = useState(false)
  const [out, setOut] = useState<ExplainOutcome | null>(null)
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState<string | null>(null)
  const tooShort = text.trim().length < 20

  const submit = async () => {
    setBusy(true); setErr(null)
    try {
      const o = await api<ExplainOutcome>(`/api/spring/topics/${encodeURIComponent(code)}/explain`, {
        method: 'POST', body: { text: text.trim(), checkedRubric: ticked, useAi },
      })
      setOut(o); onDone(o)
    } catch (e) { setErr(errorMessage(e)) } finally { setBusy(false) }
  }

  return (
    <section className="space-y-3" aria-labelledby={`exp-${code}`}>
      <h3 id={`exp-${code}`} className="font-semibold">🗣️ Explain it back</h3>
      <p className="text-sm text-muted">{detail.explainPrompt || 'Explain this topic in your own simple words, like to a friend.'}</p>
      <div>
        <label className="label" htmlFor={`exp-text-${code}`}>Your explanation (at least 20 characters)</label>
        <textarea id={`exp-text-${code}`} className="input min-h-[120px]" maxLength={4000} value={text} onChange={(e) => setText(e.target.value)} />
      </div>
      {detail.rubric.length > 0 && (
        <fieldset className="space-y-2">
          <legend className="text-sm font-semibold mb-1">Did you mention… (tick honestly)</legend>
          {detail.rubric.map((r, i) => (
            <label key={i} className="flex items-start gap-2 text-sm cursor-pointer">
              <input type="checkbox" className="mt-1 accent-primary" checked={ticked.includes(i)}
                onChange={() => setTicked((t) => (t.includes(i) ? t.filter((x) => x !== i) : [...t, i]))} />
              <span>{r}</span>
            </label>
          ))}
        </fieldset>
      )}
      <label className="flex items-center gap-2 text-sm cursor-pointer">
        <input type="checkbox" role="switch" aria-checked={useAi} className="accent-primary" checked={useAi} onChange={(e) => setUseAi(e.target.checked)} />
        🤖 Use AI to score (if AI is not available, your ticks are used)
      </label>
      {err && <ErrorBox message={err} />}
      {out && (
        <div className={`rounded-xl border p-3 text-sm ${Number(out.score) >= 60 ? 'border-good/50 bg-good/10' : 'border-warn/50 bg-warn/10'}`} role="status">
          <b>Score: {Math.round(Number(out.score))}%</b>{out.aiUsed && <span className="chip ml-2">AI scored</span>}
          <p className="mt-1 whitespace-pre-wrap">{out.feedback}</p>
        </div>
      )}
      <button className="btn-gold" disabled={busy || tooShort} onClick={submit}>{busy ? 'Scoring…' : 'Score my explanation'}</button>
    </section>
  )
}
