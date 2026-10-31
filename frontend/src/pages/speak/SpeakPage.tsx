import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { api } from '../../api/client'
import type { Page, XpAward } from '../../api/types'
import { ErrorBox, Section, Spinner, Tabs, XpToast } from '../../components/ui'
import { errorMessage, useApi } from '../../lib/hooks'
import { AnswerInput, useAnswerInput, type AnswerState } from './AnswerInput'
import { FRAMEWORKS, frameworkByCode, type FrameworkDef } from './frameworks'
import { FRAMEWORK_NAMES, fmtTime } from './metrics'
import { MiniChart, SessionResult } from './results'
import type { Library, PromptView, SessionSaved, SessionView, SpeakMode, Trends, VocabWord } from './types'
import { useSpeech, useVoiceSettings, type SpeechState } from './useSpeech'

type Tab = 'practice' | 'explain' | 'frameworks' | 'library' | 'progress'
const TABS: { id: Tab; label: string }[] = [
  { id: 'practice', label: '🎤 Practice' }, { id: 'explain', label: '💡 Explain aloud' }, { id: 'frameworks', label: '🧭 Frameworks' },
  { id: 'library', label: '📚 Library' }, { id: 'progress', label: '📈 Progress' },
]

export default function SpeakPage() {
  const [params, setParams] = useSearchParams()
  const tab = (TABS.some((t) => t.id === params.get('tab')) ? params.get('tab') : 'practice') as Tab
  const voice = useVoiceSettings()
  const tts = useSpeech(voice.lang, voice.rate)
  const [award, setAward] = useState<XpAward | null>(null)
  const clear = useCallback(() => setAward(null), [])

  const change = (t: Tab) => {
    tts.cancel()
    setParams(t === 'practice' ? {} : { tab: t }, { replace: true })
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h1 className="text-2xl font-bold">🗣️ Communication Gym</h1>
        <Link to="/interview/mock" className="text-sm text-primary hover:underline">🐉 Ready? Try a mock interview →</Link>
      </div>
      <p className="text-sm text-muted -mt-3">Speak a little every day. Slow and clear beats fast and nervous.</p>
      <Tabs tabs={TABS} value={tab} onChange={change} />
      {tab === 'practice' && <Practice lang={voice.lang} tts={tts} onAward={setAward} />}
      {tab === 'explain' && <ExplainAloud lang={voice.lang} tts={tts} onAward={setAward} />}
      {tab === 'frameworks' && <Frameworks tts={tts} />}
      {tab === 'library' && <LibraryView tts={tts} />}
      {tab === 'progress' && <Progress />}
      <XpToast award={award} onDone={clear} />
    </div>
  )
}

// ------------------------------------------------------------------ shared helpers

function HearButton({ tts, text, label = '🔊 Hear it', className = 'btn-ghost text-sm px-3 py-1.5' }: { tts: SpeechState; text: string; label?: string; className?: string }) {
  if (!tts.supported) return null
  return (
    <button type="button" className={className} onClick={() => (tts.speaking ? tts.cancel() : tts.speak(text))}
      aria-label={`${tts.speaking ? 'Stop' : 'Hear'}: ${text.slice(0, 60)}`}>
      {tts.speaking ? '⏹ Stop' : label}
    </button>
  )
}

function AiToggle({ value, onChange }: { value: boolean; onChange: (v: boolean) => void }) {
  return (
    <label className="flex items-center gap-2 text-sm cursor-pointer select-none">
      <input type="checkbox" className="h-4 w-4 accent-[rgb(var(--c-primary))]" checked={value} onChange={(e) => onChange(e.target.checked)} />
      🤖 Get AI feedback <span className="text-muted">(grammar, clarity, a better version)</span>
    </label>
  )
}

async function submitSession(ans: AnswerState, opts: { promptId: number | null; mode: SpeakMode; promptText: string; useAi: boolean }) {
  ans.sr.stop()
  ans.stopTypedTimer()
  return api<SessionSaved>('/api/speaking/sessions', {
    method: 'POST',
    body: {
      promptId: opts.promptId, mode: opts.mode, promptText: opts.promptText.slice(0, 1000),
      transcript: ans.text.slice(0, 8000), durationSeconds: Math.min(900, Math.max(3, ans.seconds)),
      inputMethod: ans.method, longPauses: Math.min(200, ans.longPauses), useAi: opts.useAi,
    },
  })
}

function SubmitBar({ ans, busy, onSubmit, label = 'Submit answer' }: { ans: AnswerState; busy: boolean; onSubmit: () => void; label?: string }) {
  const tooShort = ans.text.length < 5
  return (
    <div className="flex flex-wrap items-center gap-3">
      <button type="button" className="btn-primary" disabled={busy || tooShort || ans.sr.listening} onClick={onSubmit}>
        {busy ? 'Checking…' : `✅ ${label}`}
      </button>
      {ans.sr.listening && <span className="text-sm text-muted">Press Stop first, then submit.</span>}
      {!ans.sr.listening && tooShort && <span className="text-sm text-muted">Say or type a few words first.</span>}
    </div>
  )
}

function FrameworkMini({ fw }: { fw: FrameworkDef | null }) {
  if (!fw) return <p className="text-sm text-muted">Structure: <b>{FRAMEWORK_NAMES.NONE}</b>. Use signposts so it's easy to follow.</p>
  return (
    <div>
      <p className="text-sm font-semibold mb-2">{fw.icon} Use: {fw.title}</p>
      <ol className="flex flex-wrap gap-2 text-sm">
        {fw.steps.map((s, i) => (
          <li key={s.name} className="rounded-lg bg-raised px-3 py-2 flex-1 min-w-[8rem]">
            <b className="text-primary">{i + 1}. {s.name}</b><br /><span className="text-xs text-muted">{s.hint}</span>
          </li>
        ))}
      </ol>
    </div>
  )
}

// ------------------------------------------------------------------ Practice

const CATEGORIES: [string, string][] = [
  ['', 'All'], ['SELF_INTRO', 'Self intro'], ['HR', 'HR'], ['BEHAVIORAL', 'Behavioral'], ['PROJECT', 'Project'],
  ['WHY_COMPANY', 'Why this company'], ['TECHNICAL', 'Technical'],
]
const catName = (c: string) => CATEGORIES.find(([k]) => k === c)?.[1] ?? c

function Practice({ lang, tts, onAward }: { lang: string; tts: SpeechState; onAward: (a: XpAward) => void }) {
  const [cat, setCat] = useState('')
  const prompts = useApi<PromptView[]>(`/api/speaking/prompts${cat ? `?category=${encodeURIComponent(cat)}` : ''}`)
  const [selected, setSelected] = useState<PromptView | null>(null)

  if (selected) return <PracticeRound key={selected.id} prompt={selected} lang={lang} tts={tts} onAward={onAward} onBack={() => { tts.cancel(); setSelected(null) }} />

  return (
    <Section title="Choose a practice prompt">
      <div className="flex flex-wrap gap-1.5" role="radiogroup" aria-label="Category">
        {CATEGORIES.map(([k, l]) => (
          <button key={k} type="button" role="radio" aria-checked={cat === k} className={`chip px-3 py-1.5 text-sm ${cat === k ? 'chip-on' : ''}`} onClick={() => setCat(k)}>{l}</button>
        ))}
      </div>
      {prompts.loading && <Spinner />}
      {prompts.error && <ErrorBox message={prompts.error} onRetry={prompts.reload} />}
      {prompts.data && prompts.data.length === 0 && <p className="text-muted text-sm">No prompts in this category yet.</p>}
      <ul className="grid sm:grid-cols-2 gap-2">
        {prompts.data?.map((p) => (
          <li key={p.id}>
            <button type="button" className="card w-full text-left hover:border-primary/60 transition h-full" onClick={() => setSelected(p)}>
              <span className="font-medium">{p.prompt}</span>
              <span className="mt-2 flex flex-wrap gap-1.5">
                <span className="chip">{catName(p.category)}</span>
                <span className="chip">{frameworkByCode(p.framework)?.title ?? 'Free structure'}</span>
                <span className="chip">⏱️ {fmtTime(p.timeLimitSeconds)}</span>
              </span>
            </button>
          </li>
        ))}
      </ul>
    </Section>
  )
}

function PracticeRound({ prompt, lang, tts, onAward, onBack }: {
  prompt: PromptView; lang: string; tts: SpeechState; onAward: (a: XpAward) => void; onBack: () => void
}) {
  const ans = useAnswerInput(lang)
  const [useAi, setUseAi] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [result, setResult] = useState<SessionSaved | null>(null)
  const fw = frameworkByCode(prompt.framework)

  const submit = async () => {
    setBusy(true); setError(null)
    try {
      const r = await submitSession(ans, { promptId: prompt.id, mode: 'PRACTICE', promptText: prompt.prompt, useAi })
      setResult(r)
      if (r.xp) onAward(r.xp)
    } catch (e) { setError(errorMessage(e)) } finally { setBusy(false) }
  }

  return (
    <div className="space-y-4">
      <button type="button" className="btn-ghost text-sm" onClick={onBack}>← Choose another prompt</button>
      <div className="card space-y-3">
        <div className="flex flex-wrap items-start justify-between gap-2">
          <h2 className="text-xl font-semibold">{prompt.prompt}</h2>
          <HearButton tts={tts} text={prompt.prompt} label="🔊 Hear the question" />
        </div>
        <FrameworkMini fw={fw} />
        {prompt.tips && prompt.tips.length > 0 && (
          <ul className="text-sm space-y-1">{prompt.tips.map((t, i) => <li key={i} className="flex gap-2"><span aria-hidden>💡</span>{t}</li>)}</ul>
        )}
      </div>

      {!result ? (
        <div className="card space-y-4">
          <AnswerInput ans={ans} timeLimit={prompt.timeLimitSeconds} disabled={busy} onBeforeRecord={tts.cancel} />
          <AiToggle value={useAi} onChange={setUseAi} />
          {error && <ErrorBox message={error} />}
          <SubmitBar ans={ans} busy={busy} onSubmit={submit} />
        </div>
      ) : (
        <>
          <SessionResult s={result.session} sampleAnswer={prompt.sampleAnswer} notice={result.notice} />
          <div className="flex flex-wrap gap-2">
            <button type="button" className="btn-primary" onClick={() => { ans.reset(); setResult(null) }}>🔁 Try this prompt again</button>
            <button type="button" className="btn-ghost" onClick={onBack}>Next prompt</button>
          </div>
        </>
      )}
    </div>
  )
}

// ------------------------------------------------------------------ Explain aloud

const CONCEPTS = ['HashMap', 'Polymorphism', 'Inheritance', 'Encapsulation', 'Exception handling', 'Garbage collection',
  'ArrayList vs LinkedList', 'Interface vs abstract class', 'REST API', 'Dependency injection', 'SQL joins', 'Normalization']

interface Round { question: string; saved: SessionSaved }

function ExplainAloud({ lang, tts, onAward }: { lang: string; tts: SpeechState; onAward: (a: XpAward) => void }) {
  const ans = useAnswerInput(lang)
  const [concept, setConcept] = useState('')
  const [question, setQuestion] = useState<string | null>(null)
  const [rounds, setRounds] = useState<Round[]>([])
  const [useAi, setUseAi] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [showingResult, setShowingResult] = useState(false)
  const last = rounds[rounds.length - 1]
  const { speak } = tts

  // Speak a new follow-up question aloud (once), like a real interviewer.
  const followUp = showingResult && last ? last.saved.session.followUpQuestion : null
  const spokenRef = useRef<number | null>(null)
  const lastId = last?.saved.session.id ?? null
  useEffect(() => {
    if (followUp && lastId != null && spokenRef.current !== lastId) { spokenRef.current = lastId; speak(followUp) }
  }, [followUp, lastId, speak])

  const begin = (c: string) => {
    const name = c.trim()
    if (!name) return
    setConcept(name)
    setRounds([])
    setShowingResult(false)
    ans.reset()
    setQuestion(`Explain ${name} in simple words, with a small example and where it is used.`)
  }

  const submit = async () => {
    if (!question) return
    setBusy(true); setError(null)
    try {
      const r = await submitSession(ans, { promptId: null, mode: 'EXPLAIN_ALOUD', promptText: question, useAi })
      setRounds((rs) => [...rs, { question, saved: r }])
      setShowingResult(true)
      if (r.xp) onAward(r.xp)
    } catch (e) { setError(errorMessage(e)) } finally { setBusy(false) }
  }

  const answerFollowUp = () => {
    if (!followUp) return
    tts.cancel()
    ans.reset()
    setShowingResult(false)
    setQuestion(followUp)
  }

  if (!question) {
    return (
      <Section title="Explain a concept out loud">
        <div className="card space-y-3">
          <p className="text-sm text-muted">Teaching is the best test. Pick a concept and explain it like you're helping a friend.
            With AI on, you'll get a follow-up question, just like an interviewer.</p>
          <form className="flex gap-2" onSubmit={(e) => { e.preventDefault(); begin(concept) }}>
            <label htmlFor="concept" className="sr-only">Concept</label>
            <input id="concept" className="input" placeholder="e.g. HashMap" value={concept} maxLength={120} onChange={(e) => setConcept(e.target.value)} />
            <button className="btn-primary" disabled={!concept.trim()}>Start</button>
          </form>
          <div className="flex flex-wrap gap-1.5" aria-label="Suggestions">
            {CONCEPTS.map((c) => <button key={c} type="button" className="chip px-3 py-1.5 text-sm hover:text-ink" onClick={() => begin(c)}>{c}</button>)}
          </div>
        </div>
      </Section>
    )
  }

  return (
    <div className="space-y-4">
      <button type="button" className="btn-ghost text-sm" onClick={() => { tts.cancel(); ans.reset(); setQuestion(null); setRounds([]); setShowingResult(false) }}>← Pick another concept</button>
      {rounds.length > 0 && (
        <ol className="space-y-1 text-sm" aria-label="Earlier rounds">
          {rounds.filter((r) => r !== last || !showingResult).map((r, i) => (
            <li key={i} className="rounded-lg bg-raised px-3 py-2 flex justify-between gap-2">
              <span>Round {i + 1}: {r.question}</span>
              <span className="text-muted whitespace-nowrap">{Math.round(Number(r.saved.session.overallScore ?? 0))}/100</span>
            </li>
          ))}
        </ol>
      )}
      <div className="card space-y-3">
        <div className="flex flex-wrap items-start justify-between gap-2">
          <div>
            <p className="text-xs text-muted">Round {rounds.length + (showingResult ? 0 : 1)} · {concept}</p>
            <h2 className="text-xl font-semibold">{question}</h2>
          </div>
          <HearButton tts={tts} text={question} label="🔊 Hear" />
        </div>
        <FrameworkMini fw={frameworkByCode('CEU')} />
      </div>

      {!showingResult || !last ? (
        <div className="card space-y-4">
          <AnswerInput ans={ans} timeLimit={120} disabled={busy} onBeforeRecord={tts.cancel} />
          <AiToggle value={useAi} onChange={setUseAi} />
          {error && <ErrorBox message={error} />}
          <SubmitBar ans={ans} busy={busy} onSubmit={submit} label="Submit explanation" />
        </div>
      ) : (
        <>
          <SessionResult s={last.saved.session} notice={last.saved.notice} />
          {followUp ? (
            <div className="card border-primary/50 space-y-3">
              <p className="text-sm text-muted">🎙️ Interviewer follow-up</p>
              <p className="text-lg font-semibold">{followUp}</p>
              <div className="flex flex-wrap gap-2">
                <button type="button" className="btn-primary" onClick={answerFollowUp}>Answer the follow-up →</button>
                <HearButton tts={tts} text={followUp} label="🔊 Hear again" />
              </div>
            </div>
          ) : (
            <p className="text-sm text-muted">{useAi ? 'No follow-up this time.' : 'Turn on AI feedback to get a follow-up question.'}</p>
          )}
          <button type="button" className="btn-ghost" onClick={() => begin(concept)}>🔁 Explain {concept} again</button>
        </>
      )}
    </div>
  )
}

// ------------------------------------------------------------------ Frameworks

function Frameworks({ tts }: { tts: SpeechState }) {
  return (
    <Section title="Answer frameworks">
      <p className="text-sm text-muted -mt-2">A framework is a simple order for your answer. It stops you from getting lost, even when you're nervous.</p>
      <div className="space-y-4">{FRAMEWORKS.map((f) => <FrameworkCard key={f.code} fw={f} tts={tts} />)}</div>
    </Section>
  )
}

const STEP_TONES = ['border-primary/60 text-primary', 'border-accent/60 text-accent', 'border-gold/60 text-gold', 'border-good/60 text-good']

function FrameworkCard({ fw, tts }: { fw: FrameworkDef; tts: SpeechState }) {
  const [fill, setFill] = useState<string[]>(() => fw.steps.map(() => ''))
  const full = fill.map((s) => s.trim()).filter(Boolean).join(' ')
  return (
    <article className="card space-y-3">
      <header>
        <h3 className="text-lg font-semibold">{fw.icon} {fw.title}</h3>
        <p className="text-sm text-muted">Use it for: {fw.useFor}</p>
      </header>
      <ol className="flex flex-col sm:flex-row gap-2" aria-label={`${fw.title} steps`}>
        {fw.steps.map((s, i) => (
          <li key={s.name} className="flex-1 flex sm:flex-col items-stretch gap-2">
            <div className={`flex-1 rounded-xl border-2 bg-raised p-3 ${STEP_TONES[i % STEP_TONES.length]}`}>
              <div className="font-bold">{i + 1}. {s.name}</div>
              <div className="text-xs text-ink/80">{s.hint}</div>
            </div>
            {i < fw.steps.length - 1 && <span className="hidden sm:block text-center text-muted" aria-hidden>→</span>}
          </li>
        ))}
      </ol>
      <details>
        <summary className="cursor-pointer text-sm font-medium">✍️ Fill it in yourself</summary>
        <div className="mt-3 space-y-2">
          {fw.steps.map((s, i) => (
            <div key={s.name}>
              <label className="label" htmlFor={`${fw.code}-${i}`}>{i + 1}. {s.name}</label>
              <input id={`${fw.code}-${i}`} className="input" value={fill[i]} placeholder={s.example}
                onChange={(e) => setFill((f) => f.map((v, j) => (j === i ? e.target.value : v)))} />
            </div>
          ))}
          <div className="flex flex-wrap gap-2">
            <button type="button" className="btn-ghost text-sm" onClick={() => setFill(fw.steps.map((s) => s.example))}>Use the example</button>
            <button type="button" className="btn-ghost text-sm" onClick={() => setFill(fw.steps.map(() => ''))}>Clear</button>
            {full && <HearButton tts={tts} text={full} label="🔊 Hear my answer" />}
          </div>
          {full && <p className="text-sm rounded-lg bg-raised p-3" aria-live="polite">{full}</p>}
          <p className="text-xs text-muted">Now say it out loud 3 times without reading. Then try a Practice prompt.</p>
        </div>
      </details>
    </article>
  )
}

// ------------------------------------------------------------------ Library

type LibTab = 'phrases' | 'vocabulary' | 'pronunciation'

function LibraryView({ tts }: { tts: SpeechState }) {
  const lib = useApi<Library>('/api/speaking/library')
  const [sub, setSub] = useState<LibTab>('phrases')
  if (lib.loading) return <Spinner />
  if (lib.error) return <ErrorBox message={lib.error} onRetry={lib.reload} />
  if (!lib.data) return null
  const { phrases = [], vocabulary = [], pronunciation = [] } = lib.data
  return (
    <div className="space-y-4">
      <Tabs<LibTab> value={sub} onChange={setSub} tabs={[
        { id: 'phrases', label: `💬 Phrases (${phrases.length})` }, { id: 'vocabulary', label: `🃏 Vocabulary (${vocabulary.length})` },
        { id: 'pronunciation', label: `🔊 Say it right (${pronunciation.length})` },
      ]} />
      {sub === 'phrases' && (
        <ul className="space-y-2">
          {phrases.map((p, i) => (
            <li key={i} className="card flex items-start justify-between gap-3">
              <div><p className="font-medium">"{p.phrase}"</p>{p.useWhen && <p className="text-sm text-muted">Use when: {p.useWhen}</p>}</div>
              <HearButton tts={tts} text={p.phrase} label="🔊" />
            </li>
          ))}
        </ul>
      )}
      {sub === 'vocabulary' && (
        <>
          <p className="text-sm text-muted">Tap a card to flip it. Try to say the meaning before you flip.</p>
          <ul className="grid grid-cols-2 sm:grid-cols-3 gap-3">{vocabulary.map((v, i) => <li key={i}><FlipCard v={v} /></li>)}</ul>
        </>
      )}
      {sub === 'pronunciation' && (
        <ul className="grid sm:grid-cols-2 gap-2">
          {pronunciation.map((p, i) => (
            <li key={i} className="card space-y-1">
              <div className="flex items-center justify-between gap-2">
                <span className="text-lg font-semibold">{p.term}</span>
                <HearButton tts={tts} text={p.term} label="🔊 Hear it" />
              </div>
              <p className="text-sm">Say it like: <b className="text-accent font-mono">{p.sayItLike}</b></p>
              {p.note && <p className="text-xs text-muted">{p.note}</p>}
            </li>
          ))}
          {!tts.supported && <li className="text-sm text-muted">Your browser can't read words aloud. Use the "say it like" spelling.</li>}
        </ul>
      )}
    </div>
  )
}

function FlipCard({ v }: { v: VocabWord }) {
  const [flipped, setFlipped] = useState(false)
  const face = 'absolute inset-0 rounded-card border border-line/60 p-3 flex flex-col justify-center [backface-visibility:hidden]'
  return (
    <button type="button" className="relative block w-full h-40 [perspective:800px] text-left" aria-pressed={flipped}
      aria-label={flipped ? `${v.word}: ${v.meaning}` : `${v.word}. Tap to see the meaning`} onClick={() => setFlipped(!flipped)}>
      <span className="absolute inset-0 transition-transform duration-500 [transform-style:preserve-3d]" style={{ transform: flipped ? 'rotateY(180deg)' : 'none' }}>
        <span className={`${face} bg-surface items-center text-center`}>
          <span className="text-lg font-semibold font-display">{v.word}</span>
          <span className="text-xs text-muted mt-1">tap to flip</span>
        </span>
        <span className={`${face} bg-raised [transform:rotateY(180deg)]`}>
          <span className="text-sm font-medium">{v.meaning}</span>
          {v.example && <span className="text-xs text-muted mt-1 italic">"{v.example}"</span>}
        </span>
      </span>
    </button>
  )
}

// ------------------------------------------------------------------ Progress

function weekLabel(w: string | number): string {
  const d = typeof w === 'number' ? new Date(w) : new Date(`${String(w).slice(0, 10)}T00:00:00`)
  return Number.isNaN(d.getTime()) ? String(w) : d.toLocaleDateString(undefined, { day: 'numeric', month: 'short' })
}
const num = (v: unknown): number | null => (v == null || v === '' || !Number.isFinite(Number(v)) ? null : Number(v))
const todayLocal = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function Progress() {
  const trends = useApi<Trends>('/api/speaking/trends')
  const history = useApi<Page<SessionView>>('/api/speaking/sessions?size=30')
  const today = todayLocal()
  const todaySeconds = (history.data?.items ?? []).filter((s) => s.localDate === today).reduce((a, s) => a + s.durationSeconds, 0)
  const goal = 5 * 60
  const weekly = (trends.data?.weekly ?? []).slice(-8)
  const labels = weekly.map((w) => weekLabel(w.week))

  return (
    <div className="space-y-6">
      {trends.loading && <Spinner />}
      {trends.error && <ErrorBox message={trends.error} onRetry={trends.reload} />}
      {trends.data && (
        <>
          <div className="card flex flex-wrap items-center gap-4">
            <div className="text-4xl" aria-hidden>{trends.data.speakingStreak > 0 ? '🔥' : '🎙️'}</div>
            <div className="flex-1 min-w-[12rem]">
              <div className="text-2xl font-bold font-display">{trends.data.speakingStreak} <span className="text-sm font-normal text-muted">day speaking streak</span></div>
              <p className="text-xs text-muted">Daily goal: speak for 5 minutes. {trends.data.sessionsTotal} sessions so far.</p>
              <div className="mt-2 h-2.5 rounded-full bg-raised overflow-hidden" role="progressbar" aria-label="Speaking today" aria-valuemin={0} aria-valuemax={goal} aria-valuenow={Math.min(goal, todaySeconds)}>
                <div className="h-full bg-gradient-to-r from-primary to-accent transition-all" style={{ width: `${Math.min(100, (todaySeconds / goal) * 100)}%` }} />
              </div>
              <p className="text-xs mt-1">{todaySeconds >= goal ? <span className="text-good">5 minutes done today ✓</span> : <span className="text-muted">Today: {fmtTime(todaySeconds)} of 5:00</span>}</p>
            </div>
          </div>
          <Section title="Weekly trends">
            {weekly.length === 0 ? <p className="text-sm text-muted">Finish a few sessions to see your trends.</p> : (
              <div className="grid grid-cols-2 gap-3">
                <MiniChart label="Words / min" values={weekly.map((w) => num(w.wpm))} labels={labels} />
                <MiniChart label="Fillers per answer" values={weekly.map((w) => num(w.fillers))} labels={labels} goodHigh={false} />
                <MiniChart label="Overall score" values={weekly.map((w) => num(w.score))} labels={labels} />
                <MiniChart label="Confidence (AI)" values={weekly.map((w) => num(w.confidence))} labels={labels} unit="/10" />
                <div className="col-span-2"><MiniChart label="Sessions per week" kind="bar" values={weekly.map((w) => num(w.sessions))} labels={labels} /></div>
              </div>
            )}
            <p className="text-xs text-muted">Pace is best between 110 and 160 words/min. Fewer fillers is better.</p>
          </Section>
        </>
      )}
      <Section title="History">
        {history.loading && <Spinner />}
        {history.error && <ErrorBox message={history.error} onRetry={history.reload} />}
        {history.data && history.data.items.length === 0 && <p className="text-sm text-muted">No sessions yet. Start with "Tell me about yourself".</p>}
        <ul className="space-y-2">
          {history.data?.items.map((s) => (
            <li key={s.id}>
              <details className="card">
                <summary className="cursor-pointer flex flex-wrap items-baseline justify-between gap-2">
                  <span className="font-medium">{s.mode === 'EXPLAIN_ALOUD' ? '💡' : '🎤'} {s.promptText}</span>
                  <span className="text-xs text-muted">{s.localDate} · {s.wpm} wpm · {s.fillerCount} fillers · <b className="text-ink">{Math.round(Number(s.overallScore ?? 0))}</b>/100</span>
                </summary>
                <div className="mt-3"><SessionResult s={s} /></div>
              </details>
            </li>
          ))}
        </ul>
      </Section>
    </div>
  )
}
