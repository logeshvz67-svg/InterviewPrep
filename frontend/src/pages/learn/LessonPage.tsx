import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { api } from '../../api/client'
import type { ExplainBackResult, LessonView, QuizResult } from '../../api/learning'
import type { XpAward } from '../../api/types'
import { CodeBlock, Markdown } from '../../components/Markdown'
import { QuizRunner } from '../../components/QuizRunner'
import { ErrorBox, Spinner, XpToast } from '../../components/ui'
import { Visualizer } from '../../components/viz/Visualizer'
import { errorMessage, useApi } from '../../lib/hooks'
import { similarity } from '../../lib/text'

/**
 * The lesson player. One step on screen at a time (less to look at = easier to focus):
 * Why -> Analogy -> Visual -> Explanation -> Tiny code -> Try it -> Predict -> Traps & interview -> Quiz -> Explain it back -> Summary.
 */
const STEPS = [
  { id: 'why', label: 'Why' }, { id: 'analogy', label: 'Analogy' }, { id: 'visual', label: 'Visual' },
  { id: 'explanation', label: 'Lesson' }, { id: 'tinyCode', label: 'Tiny code' }, { id: 'tryIt', label: 'Try it' },
  { id: 'predict', label: 'Predict' }, { id: 'traps', label: 'Traps' }, { id: 'quiz', label: 'Quiz' },
  { id: 'explainBack', label: 'Explain back' }, { id: 'summary', label: 'Summary' },
] as const
type StepId = (typeof STEPS)[number]['id']
const WORLD_NAMES: Record<string, string> = {
  JAVA_CORE: 'Java Core Island', DSA: 'DSA Kingdom', SQL: 'SQL Caves', CS_CORE: 'CS Core Temple', SPRING: 'Spring Boot Forge',
  INTERVIEW: 'Interview Colosseum', APTITUDE: 'Aptitude Arena', SYSTEM_DESIGN: 'System Design Tower',
}
/** Worlds where "code" means a query, a worked answer or an answer template rather than a Java program. */
const NON_CODE_WORLDS = new Set(['INTERVIEW', 'APTITUDE', 'CS_CORE'])
const SERVER_STEPS = new Set(['why', 'analogy', 'visual', 'explanation', 'tinyCode', 'tryIt', 'predict'])

export default function LessonPage() {
  const { slug = '' } = useParams()
  const nav = useNavigate()
  const { data, error, loading, reload, setData } = useApi<LessonView>(`/api/lessons/${slug}`)
  const [step, setStep] = useState(0)
  const [award, setAward] = useState<XpAward | null>(null)
  const clear = useCallback(() => setAward(null), [])

  useEffect(() => { setStep(0); window.scrollTo(0, 0) }, [slug])

  const markStep = useCallback(async (id: string) => {
    if (!SERVER_STEPS.has(id) || data?.stepsDone.includes(id)) return
    try { setData(await api<LessonView>(`/api/lessons/${slug}/steps`, { method: 'POST', body: { step: id } })) } catch { /* not critical */ }
  }, [data, slug, setData])

  if (loading && !data) return <Spinner label="Opening the lesson…" />
  if (error || !data) return <ErrorBox message={error ?? 'Lesson not found'} onRetry={reload} />
  if (data.locked || !data.content) {
    return (
      <div className="card text-center space-y-3 max-w-lg mx-auto mt-10">
        <div className="text-5xl">🔒</div>
        <h1 className="text-xl font-bold">{data.title}</h1>
        <p className="text-muted">{data.lockReason}</p>
        <div className="flex flex-wrap gap-2 justify-center">
          {data.prevSlug && <Link className="btn-primary" to={`/lesson/${data.prevSlug}`}>Go to previous lesson</Link>}
          <Link className="btn-ghost" to={`/lesson/${slug}/prove-it`}>🎖️ Prove it (90%+)</Link>
        </div>
      </div>
    )
  }

  const c = data.content
  const current = STEPS[step].id as StepId
  const go = async (n: number) => {
    if (n > step) await markStep(current)
    setStep(Math.max(0, Math.min(STEPS.length - 1, n)))
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  return (
    <article className="space-y-4 max-w-3xl">
      <header className="space-y-2">
        <div className="flex justify-between items-center text-sm">
          <Link to={`/learn/${data.world}`} className="text-primary">← {WORLD_NAMES[data.world] ?? 'World'}</Link>
          <Link to={`/lesson/${slug}/prove-it`} className="chip px-3 py-1">🎖️ Prove it</Link>
        </div>
        <h1 className="text-2xl font-bold">{data.title} {c.rebuild && <span className="chip align-middle">Rebuild</span>}</h1>
        <p className="text-xs text-muted">🎯 Infosys: {c.infosysRelevance} · Other companies: {c.otherCompanies}</p>
        {data.world === 'SYSTEM_DESIGN' && <p className="text-xs text-muted">System design answers have no single "right" design: interviewers judge your reasoning and trade-offs.</p>}
        <nav aria-label="Lesson steps" className="flex gap-1 overflow-x-auto pb-1">
          {STEPS.map((s, i) => {
            const done = data.stepsDone.includes(s.id) || (s.id === 'quiz' && data.quizPassed) || (s.id === 'explainBack' && !!data.explainBack)
            return (
              <button key={s.id} onClick={() => go(i)} aria-current={i === step ? 'step' : undefined}
                className={`chip whitespace-nowrap px-2.5 py-1 ${i === step ? 'chip-on' : ''} ${done ? 'text-good' : ''}`}>
                {done ? '✓ ' : ''}{s.label}
              </button>
            )
          })}
        </nav>
        <div className="h-1.5 rounded bg-raised"><div className="h-full rounded bg-primary transition-all" style={{ width: `${((step + 1) / STEPS.length) * 100}%` }} /></div>
      </header>

      <section className="card space-y-3 min-h-[40vh]">
        {current === 'why' && <><h2 className="text-xl font-semibold">Why this matters</h2><Markdown text={c.why} /></>}
        {current === 'analogy' && <><h2 className="text-xl font-semibold">💡 Think of it like this</h2><Markdown text={c.analogy} /></>}
        {current === 'visual' && (
          <>
            <h2 className="text-xl font-semibold">👀 See it</h2>
            <pre className="bg-raised rounded-xl p-3 overflow-x-auto text-xs sm:text-sm font-mono leading-snug">{c.visual.diagram}</pre>
            <p className="text-muted">{c.visual.caption}</p>
            {c.visual.visualizer && <div className="border-t border-line/60 pt-3"><p className="text-sm font-semibold mb-2">Interactive: step through it yourself</p><Visualizer id={c.visual.visualizer} /></div>}
          </>
        )}
        {current === 'explanation' && <><h2 className="text-xl font-semibold">📖 The lesson</h2><Markdown text={c.explanation} /></>}
        {current === 'tinyCode' && (
          <><h2 className="text-xl font-semibold">🧪 Tiny code</h2><CodeBlock code={c.tinyCode.code} /><Markdown text={c.tinyCode.walkthrough} /></>
        )}
        {current === 'tryIt' && <TryIt instructions={c.tryIt.instructions} code={c.tryIt.code} done={data.stepsDone.includes('tryIt')} onDone={() => markStep('tryIt')} sql={data.world === 'SQL'} />}
        {current === 'predict' && <Predict code={c.predict.code} expected={c.predict.expectedOutput} explanation={c.predict.explanation} onDone={() => markStep('predict')}
          kind={data.world === 'SQL' ? 'result' : NON_CODE_WORLDS.has(data.world) ? 'answer' : 'output'} />}
        {current === 'traps' && (
          <>
            <h2 className="text-xl font-semibold">⚠️ Common mistakes</h2>
            <ul className="list-disc pl-5 space-y-1">{c.commonMistakes.map((m, i) => <li key={i}><Markdown text={m} /></li>)}</ul>
            <div className="rounded-xl bg-accent/10 p-3 space-y-2">
              <h3 className="font-semibold">🎤 Why interviewers ask this</h3>
              <p className="text-sm">{c.interviewBox.why}</p>
              {c.interviewBox.questions.map((q, i) => (
                <details key={i} className="bg-surface rounded-lg p-2">
                  <summary className="cursor-pointer font-medium text-sm">Practice question: {q.q}</summary>
                  <div className="text-sm mt-2"><Markdown text={q.a} /></div>
                </details>
              ))}
            </div>
          </>
        )}
        {current === 'quiz' && <LessonQuiz lesson={data} onAward={setAward} onPassed={reload} onNext={(s) => nav(`/lesson/${s}`)} />}
        {current === 'explainBack' && <ExplainBack lesson={data} onAward={setAward} onSaved={reload} />}
        {current === 'summary' && (
          <>
            <h2 className="text-xl font-semibold">🗝️ Key takeaways</h2>
            <ul className="list-disc pl-5 space-y-1">{c.keyTakeaways.map((k, i) => <li key={i}><Markdown text={k} /></li>)}</ul>
            <h3 className="font-semibold mt-4">🃏 Your flashcards (added when you complete the lesson)</h3>
            <div className="grid sm:grid-cols-2 gap-2">{c.flashcards.map((f, i) => <FlipCard key={i} front={f.front} back={f.back} />)}</div>
            <p className="text-sm text-muted">Status: {data.status === 'COMPLETED' ? '✅ Completed' : 'In progress: finish Try it, Predict and pass the quiz.'}</p>
            {data.nextSlug && data.status === 'COMPLETED' && <Link className="btn-primary" to={`/lesson/${data.nextSlug}`}>Next lesson →</Link>}
          </>
        )}
      </section>

      <div className="flex justify-between gap-2">
        <button className="btn-ghost" disabled={step === 0} onClick={() => go(step - 1)}>← Back</button>
        {step < STEPS.length - 1 && <button className="btn-primary" onClick={() => go(step + 1)}>Next: {STEPS[step + 1].label} →</button>}
      </div>
      <XpToast award={award} onDone={clear} />
    </article>
  )
}

/** Type the code yourself. Paste is blocked on purpose: typing builds memory (and breaks the copy-paste habit). */
function TryIt({ instructions, code, done, onDone, sql }: { instructions: string; code: string; done: boolean; onDone: () => void; sql?: boolean }) {
  const [typed, setTyped] = useState('')
  const [warn, setWarn] = useState<string | null>(null)
  const score = useMemo(() => similarity(typed, code), [typed, code])
  const ok = score >= 0.9
  useEffect(() => { if (ok && !done) onDone() }, [ok, done, onDone])
  return (
    <>
      <h2 className="text-xl font-semibold">⌨️ Try it yourself</h2>
      <p>{instructions}</p>
      <CodeBlock code={code} label="Type this (look, then type: don't copy)" />
      <textarea className="input font-mono text-sm min-h-56" value={typed} spellCheck={false} autoCapitalize="off" autoCorrect="off"
        aria-label="Type the code here" placeholder="Start typing the code here…"
        onPaste={(e) => { e.preventDefault(); setWarn('Pasting is turned off here on purpose. Typing it is how it sticks!') }}
        onChange={(e) => setTyped(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === 'Tab') {
            e.preventDefault()
            const t = e.currentTarget, s = t.selectionStart
            setTyped(typed.slice(0, s) + '    ' + typed.slice(t.selectionEnd))
            requestAnimationFrame(() => { t.selectionStart = t.selectionEnd = s + 4 })
          }
        }} />
      {warn && <p className="text-warn text-sm">{warn}</p>}
      <div className="flex items-center gap-3">
        <div className="flex-1 h-2 rounded bg-raised"><div className={`h-full rounded ${ok ? 'bg-good' : 'bg-primary'}`} style={{ width: `${Math.round(score * 100)}%` }} /></div>
        <span className="text-sm text-muted">{Math.round(score * 100)}% match</span>
      </div>
      {(ok || done) && <p className="text-good text-sm">✅ Nice typing! {sql ? 'Now run it in a free online SQL playground (or your local MySQL/PostgreSQL) and compare the result.' : 'Now run it on your computer (or an online Java compiler) and check the output.'}</p>}
    </>
  )
}

function Predict({ code, expected, explanation, onDone, kind = 'output' }: { code: string; expected: string; explanation: string; onDone: () => void; kind?: 'output' | 'result' | 'answer' }) {
  const [guess, setGuess] = useState('')
  const [shown, setShown] = useState(false)
  const norm = (s: string) => s.replace(/\r/g, '').split('\n').map((l) => l.trimEnd()).join('\n').trim()
  const right = norm(guess) === norm(expected)
  return (
    <>
      <h2 className="text-xl font-semibold">🔮 Predict the {kind}</h2>
      <p className="text-muted text-sm">{kind === 'output' ? 'Read the code line by line, like the computer. Write exactly what it prints.'
        : kind === 'result' ? 'Work out the result table in your head (or on paper) before revealing it.'
        : 'Think it through and write your answer before revealing the real one.'}</p>
      <CodeBlock code={code} />
      <textarea className="input font-mono text-sm min-h-24" value={guess} onChange={(e) => setGuess(e.target.value)} disabled={shown}
        placeholder="Your predicted output" aria-label="Your predicted output" spellCheck={false} />
      {!shown ? (
        <button className="btn-primary" disabled={!guess.trim()} onClick={() => { setShown(true); onDone() }}>Reveal the real {kind}</button>
      ) : (
        <div className={`rounded-xl p-3 ${right ? 'bg-good/10' : 'bg-warn/10'}`}>
          <p className="font-semibold">{right ? '🎯 Exactly right!' : `Compare with the real ${kind}:`}</p>
          <CodeBlock code={expected} />
          <Markdown text={explanation} />
        </div>
      )}
    </>
  )
}

function LessonQuiz({ lesson, onAward, onPassed, onNext }: { lesson: LessonView; onAward: (a: XpAward) => void; onPassed: () => void; onNext: (slug: string) => void }) {
  const [result, setResult] = useState<QuizResult | null>(null)
  const [round, setRound] = useState(0)
  return (
    <>
      <h2 className="text-xl font-semibold">📝 Quiz</h2>
      <p className="text-sm text-muted">Pass with 80%. No penalty for trying again. Best so far: {Math.round(lesson.bestQuizPercent)}%.</p>
      <QuizRunner key={round} questions={lesson.quiz} topic={lesson.title} shuffleKey={String(round)}
        onSubmit={async (answers) => {
          try {
            const r = await api<QuizResult>(`/api/lessons/${lesson.slug}/quiz`, { method: 'POST', body: { answers } })
            setResult(r)
            const total = r.xp.reduce((s, x) => s + x.xp, 0)
            if (total > 0) onAward({ awarded: true, xp: total, gold: r.xp.reduce((s, x) => s + x.gold, 0), reason: null })
            return r.results
          } catch (e) { alert(errorMessage(e)); return null }
        }} />
      {result && (
        <div className={`rounded-xl p-4 ${result.passed ? 'bg-good/10' : 'bg-warn/10'} space-y-2`} role="status">
          <p className="text-2xl font-bold font-display">{Math.round(result.percent)}% <span className="text-base font-normal">({result.correct}/{result.total})</span></p>
          <p>{result.message}</p>
          <div className="flex flex-wrap gap-2">
            {!result.passed && <button className="btn-primary" onClick={() => { setResult(null); setRound(round + 1) }}>Try again</button>}
            {result.passed && <button className="btn-ghost" onClick={onPassed}>Continue</button>}
            {result.unlockedNextSlug && <button className="btn-gold" onClick={() => onNext(result.unlockedNextSlug!)}>🔓 Next lesson unlocked →</button>}
          </div>
        </div>
      )}
    </>
  )
}

function ExplainBack({ lesson, onAward, onSaved }: { lesson: LessonView; onAward: (a: XpAward) => void; onSaved: () => void }) {
  const eb = lesson.content!.explainBack
  const [text, setText] = useState(lesson.explainBack ?? '')
  const [checked, setChecked] = useState<number[]>([])
  const [useAi, setUseAi] = useState(true)
  const [busy, setBusy] = useState(false)
  const [res, setRes] = useState<ExplainBackResult | null>(null)
  const [err, setErr] = useState<string | null>(null)
  const dictation = useDictation((t) => setText((x) => (x ? x + ' ' : '') + t))

  async function submit() {
    setBusy(true); setErr(null)
    try {
      const r = await api<ExplainBackResult>(`/api/lessons/${lesson.slug}/explain-back`, { method: 'POST', body: { text, checkedRubric: checked, useAi } })
      setRes(r)
      if (r.xp.awarded) onAward(r.xp)
      onSaved()
    } catch (e) { setErr(errorMessage(e)) } finally { setBusy(false) }
  }

  return (
    <>
      <h2 className="text-xl font-semibold">🗣️ Explain it back</h2>
      <p>{eb.prompt}</p>
      <p className="text-xs text-muted">Say it out loud first, then write (or dictate) it. Explaining is the fastest way to find gaps.</p>
      <textarea className="input min-h-32" value={text} onChange={(e) => setText(e.target.value)} maxLength={4000} aria-label="Your explanation" />
      {dictation.supported && (
        <button className="btn-ghost text-sm" onClick={dictation.listening ? dictation.stop : dictation.start}>{dictation.listening ? '⏹ Stop dictation' : '🎙️ Dictate'}</button>
      )}
      <fieldset className="space-y-1">
        <legend className="label">Honest self-check: tick what you covered</legend>
        {eb.rubric.map((r, i) => (
          <label key={i} className="flex gap-2 items-start text-sm">
            <input type="checkbox" className="mt-1 h-4 w-4" checked={checked.includes(i)} onChange={() => setChecked(checked.includes(i) ? checked.filter((x) => x !== i) : [...checked, i])} />
            {r}
          </label>
        ))}
      </fieldset>
      <label className="flex gap-2 items-center text-sm"><input type="checkbox" checked={useAi} onChange={(e) => setUseAi(e.target.checked)} /> Ask the AI mentor to score it too (if it's available; your ticks are used otherwise)</label>
      {err && <p className="text-bad text-sm">{err}</p>}
      <button className="btn-primary" disabled={busy || text.trim().length < 20} onClick={submit}>{busy ? 'Scoring…' : 'Check my explanation'}</button>
      {res && (
        <div className="rounded-xl bg-raised p-3 space-y-2">
          <p className="font-semibold">Score: {Math.round(res.score)}% {res.aiUsed ? '(AI, verify with the lesson)' : '(self-check)'}</p>
          <p className="whitespace-pre-line text-sm">{res.feedback}</p>
          <details open><summary className="cursor-pointer text-sm font-semibold">Model answer</summary><Markdown text={res.modelAnswer} className="text-sm mt-2" /></details>
        </div>
      )}
    </>
  )
}

function FlipCard({ front, back }: { front: string; back: string }) {
  const [flipped, setFlipped] = useState(false)
  return (
    <button className="rounded-xl bg-raised p-3 text-left min-h-24" onClick={() => setFlipped(!flipped)} aria-label={flipped ? 'Show question' : 'Show answer'}>
      <p className="text-xs text-muted">{flipped ? 'Answer' : 'Question'} · tap to flip</p>
      <p className="mt-1 text-sm">{flipped ? back : front}</p>
    </button>
  )
}

/** Tiny speech-to-text helper (feature-detected; typing always works). */
function useDictation(onText: (t: string) => void) {
  type Rec = { start: () => void; stop: () => void; lang: string; continuous: boolean; interimResults: boolean; onresult: ((e: { resultIndex: number; results: ArrayLike<{ isFinal: boolean; 0: { transcript: string } }> }) => void) | null; onend: (() => void) | null }
  const Ctor = (window as unknown as { SpeechRecognition?: new () => Rec; webkitSpeechRecognition?: new () => Rec }).SpeechRecognition
    ?? (window as unknown as { webkitSpeechRecognition?: new () => Rec }).webkitSpeechRecognition
  const [rec, setRec] = useState<Rec | null>(null)
  const start = () => {
    if (!Ctor) return
    const r = new Ctor()
    r.lang = 'en-IN'; r.continuous = true; r.interimResults = false
    r.onresult = (e) => { for (let i = e.resultIndex; i < e.results.length; i++) if (e.results[i].isFinal) onText(e.results[i][0].transcript.trim()) }
    r.onend = () => setRec(null)
    try { r.start(); setRec(r) } catch { /* already running */ }
  }
  return { supported: !!Ctor, listening: !!rec, start, stop: () => rec?.stop() }
}
