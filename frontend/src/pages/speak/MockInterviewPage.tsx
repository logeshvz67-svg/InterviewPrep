import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../api/client'
import { ErrorBox, Section, Spinner } from '../../components/ui'
import { errorMessage, useApi } from '../../lib/hooks'
import { AnswerInput, useAnswerInput } from './AnswerInput'
import { Notice, ScoreRing } from './results'
import type { InterviewTurn, InterviewView, RoundType } from './types'
import { useSpeech, useVoiceSettings, type SpeechState } from './useSpeech'

const ROUNDS: { id: RoundType; icon: string; title: string; desc: string }[] = [
  { id: 'HR', icon: '🤝', title: 'HR round', desc: 'About you, your story, teamwork and "why us". Great first fight.' },
  { id: 'TECHNICAL', icon: '☕', title: 'Technical round', desc: 'Java, OOP, collections, SQL, OS and DSA basics.' },
  { id: 'SPRING', icon: '🌱', title: 'Spring round', desc: 'Spring Boot, dependency injection, REST and JPA.' },
]
const roundName = (r: string) => ROUNDS.find((x) => x.id === r)?.title ?? r

interface LogLine { role: 'interviewer' | 'candidate'; text: string }

export default function MockInterviewPage() {
  const voice = useVoiceSettings()
  const tts = useSpeech(voice.lang, voice.rate)
  const history = useApi<InterviewView[]>('/api/interviews')
  const [fight, setFight] = useState<{ turn: InterviewTurn; round: RoundType } | null>(null)
  const [card, setCard] = useState<InterviewView | null>(null)
  const [busy, setBusy] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const start = async (round: RoundType) => {
    setBusy(round); setError(null)
    try {
      const turn = await api<InterviewTurn>('/api/interviews', { method: 'POST', body: { roundType: round } })
      setCard(null)
      setFight({ turn, round })
    } catch (e) { setError(errorMessage(e)) } finally { setBusy(null) }
  }

  const finishOld = async (id: number) => {
    setBusy(`f${id}`); setError(null)
    try {
      setCard(await api<InterviewView>(`/api/interviews/${id}/finish`, { method: 'POST' }))
      void history.reload()
    } catch (e) { setError(errorMessage(e)) } finally { setBusy(null) }
  }

  if (fight) {
    return (
      <BossFight key={fight.turn.interviewId} first={fight.turn} round={fight.round} lang={voice.lang} tts={tts}
        onDone={(v) => { tts.cancel(); setFight(null); setCard(v); void history.reload() }}
        onQuit={() => { tts.cancel(); setFight(null); void history.reload() }} />
    )
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">🐉 Mock Interview</h1>
        <p className="text-sm text-muted">A calm boss fight: 5 practice questions, spoken aloud. Answer by voice or by typing.
          Each answer takes HP off the Interview Dragon.</p>
        <Link to="/speak" className="text-sm text-primary hover:underline">← Warm up in the Communication Gym first</Link>
      </div>

      {card && <Scorecard v={card} onClose={() => setCard(null)} />}
      {error && <ErrorBox message={error} />}

      {!card && (
        <Section title="Choose your round">
          <div className="grid sm:grid-cols-3 gap-3">
            {ROUNDS.map((r) => (
              <button key={r.id} type="button" className="card text-left hover:border-primary/60 transition disabled:opacity-50" disabled={!!busy} onClick={() => start(r.id)}>
                <div className="text-3xl" aria-hidden>{r.icon}</div>
                <div className="font-semibold mt-1">{r.title}</div>
                <div className="text-sm text-muted">{r.desc}</div>
                {busy === r.id && <div className="text-xs text-primary mt-2">Summoning the dragon…</div>}
              </button>
            ))}
          </div>
          <p className="text-xs text-muted">These are practice questions from our question bank, not questions a company is known to ask.
            {tts.supported ? ' Turn your volume up: the interviewer speaks.' : ' Your browser cannot read text aloud, so the interviewer will only appear as text.'}</p>
        </Section>
      )}

      <Section title="Past fights">
        {history.loading && <Spinner />}
        {history.error && <ErrorBox message={history.error} onRetry={history.reload} />}
        {history.data && history.data.length === 0 && <p className="text-sm text-muted">No mock interviews yet. Your first one will feel hard, and that's normal.</p>}
        <ul className="space-y-2">
          {history.data?.map((h) => {
            const answered = (h.transcript ?? []).some((t) => t.role === 'candidate')
            return (
              <li key={h.id} className="card flex flex-wrap items-center justify-between gap-2">
                <div>
                  <div className="font-medium">{roundName(h.roundType)}</div>
                  <div className="text-xs text-muted">{new Date(h.startedAt).toLocaleString()}{h.aiUsed ? ' · AI scorecard' : ''}</div>
                </div>
                <div className="flex items-center gap-2">
                  {h.status === 'COMPLETED' ? (
                    <>
                      <span className="font-bold font-display">{Math.round(Number(h.overallScore ?? 0))}<span className="text-xs text-muted">/100</span></span>
                      <button type="button" className="btn-ghost text-sm px-3 py-1.5" onClick={() => { setCard(h); window.scrollTo({ top: 0, behavior: 'smooth' }) }}>Scorecard</button>
                    </>
                  ) : answered ? (
                    <button type="button" className="btn-ghost text-sm px-3 py-1.5" disabled={!!busy} onClick={() => finishOld(h.id)}>
                      {busy === `f${h.id}` ? 'Scoring…' : 'Unfinished · score it'}
                    </button>
                  ) : <span className="chip">Unfinished</span>}
                </div>
              </li>
            )
          })}
        </ul>
      </Section>
    </div>
  )
}

// ------------------------------------------------------------------ the fight

function BossFight({ first, round, lang, tts, onDone, onQuit }: {
  first: InterviewTurn; round: RoundType; lang: string; tts: SpeechState; onDone: (v: InterviewView) => void; onQuit: () => void
}) {
  const ans = useAnswerInput(lang)
  const [turn, setTurn] = useState(first)
  const [log, setLog] = useState<LogLine[]>([{ role: 'interviewer', text: first.interviewerText }])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const answeredAny = log.some((l) => l.role === 'candidate')
  const total = Math.max(1, turn.totalQuestions)
  const defeated = turn.finished ? total : Math.max(0, turn.questionNumber - 1)
  const hp = Math.round(100 * (1 - defeated / total))
  const logEnd = useRef<HTMLDivElement>(null)

  // Speak each new interviewer line once.
  const { speak } = tts
  const spokenRef = useRef(0)
  useEffect(() => {
    if (log.length > spokenRef.current && log[log.length - 1].role === 'interviewer') {
      spokenRef.current = log.length
      speak(log[log.length - 1].text)
    }
  }, [log, speak])

  useEffect(() => { logEnd.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' }) }, [log.length])

  const send = async () => {
    const text = ans.text
    if (!text) return
    ans.sr.stop()
    ans.stopTypedTimer()
    setBusy(true); setError(null)
    try {
      const next = await api<InterviewTurn>(`/api/interviews/${turn.interviewId}/answer`, {
        method: 'POST', body: { answer: text.slice(0, 6000), durationSeconds: Math.min(900, Math.max(0, ans.seconds)) },
      })
      setLog((l) => [...l, { role: 'candidate', text }, { role: 'interviewer', text: next.interviewerText }])
      setTurn(next)
      ans.reset()
    } catch (e) { setError(errorMessage(e)) } finally { setBusy(false) }
  }

  const finish = async () => {
    tts.cancel()
    ans.sr.stop()
    setBusy(true); setError(null)
    try {
      onDone(await api<InterviewView>(`/api/interviews/${turn.interviewId}/finish`, { method: 'POST' }))
    } catch (e) { setError(errorMessage(e)); setBusy(false) }
  }

  const current = log[log.length - 1]
  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between gap-2">
        <h1 className="text-xl font-bold">🐉 {roundName(round)}</h1>
        <button type="button" className="btn-ghost text-sm" onClick={onQuit} disabled={busy}>Leave</button>
      </div>

      {/* Boss HP */}
      <div className="card border-bad/30">
        <div className="flex items-center gap-3">
          <div className={`text-4xl transition-transform duration-500 ${hp === 0 ? 'grayscale rotate-12 opacity-60' : ''}`} aria-hidden>🐉</div>
          <div className="flex-1">
            <div className="flex justify-between text-sm">
              <span className="font-semibold">Interview Dragon</span>
              <span className="text-muted">{hp === 0 ? 'Defeated! 🏆' : `HP ${hp}%`}</span>
            </div>
            <div className="mt-1 h-3 rounded-full bg-raised overflow-hidden" role="meter" aria-label="Dragon health" aria-valuemin={0} aria-valuemax={100} aria-valuenow={hp}>
              <div className="h-full bg-gradient-to-r from-bad to-warn transition-all duration-700" style={{ width: `${hp}%` }} />
            </div>
            <div className="text-xs text-muted mt-1">Question {Math.min(turn.questionNumber, total)} of {total}{turn.aiUsed && !turn.finished ? ' · follow-up' : ''}</div>
          </div>
        </div>
      </div>

      <Notice text={turn.notice} />

      {/* Conversation so far */}
      {log.length > 1 && (
        <details className="card">
          <summary className="cursor-pointer text-sm font-medium">📜 Conversation so far ({log.length} lines)</summary>
          <ol className="mt-3 space-y-2 text-sm max-h-72 overflow-y-auto">
            {log.slice(0, -1).map((l, i) => (
              <li key={i} className={`rounded-lg p-2 ${l.role === 'interviewer' ? 'bg-raised' : 'bg-primary/10 ml-6'}`}>
                <span className="text-xs text-muted block">{l.role === 'interviewer' ? '🧑‍💼 Interviewer' : '🙋 You'}</span>{l.text}
              </li>
            ))}
          </ol>
        </details>
      )}

      {/* Interviewer now */}
      <div className="card border-primary/40 space-y-2" aria-live="polite">
        <div className="flex items-center justify-between gap-2">
          <span className="text-sm text-muted">🧑‍💼 Interviewer {tts.speaking && <span className="text-primary">· speaking…</span>}</span>
          {tts.supported && (
            <button type="button" className="btn-ghost text-sm px-3 py-1.5" onClick={() => (tts.speaking ? tts.cancel() : tts.speak(current.text))}>
              {tts.speaking ? '⏹ Stop voice' : '🔊 Repeat'}
            </button>
          )}
        </div>
        <p className="text-lg leading-relaxed">{current.text}</p>
      </div>
      <div ref={logEnd} />

      {!turn.finished ? (
        <div className="card space-y-4">
          <p className="text-xs text-muted">Take a breath. It's fine to say "Let me think for a moment." Use a framework: STAR for stories, Concept → Example → Use case for technical questions.</p>
          <AnswerInput ans={ans} disabled={busy} onBeforeRecord={tts.cancel} placeholder="Type your answer as you would say it…" />
          {error && <ErrorBox message={error} />}
          <div className="flex flex-wrap gap-2 items-center">
            <button type="button" className="btn-primary" disabled={busy || !ans.text || ans.sr.listening} onClick={send}>
              {busy ? 'Sending…' : '⚔️ Send answer'}
            </button>
            {ans.sr.listening && <span className="text-sm text-muted">Press Stop first.</span>}
            {answeredAny && <button type="button" className="btn-ghost" disabled={busy} onClick={finish}>End now and see scorecard</button>}
          </div>
        </div>
      ) : (
        <div className="card text-center space-y-3">
          <p className="text-lg font-semibold">🏆 You finished the interview!</p>
          {error && <ErrorBox message={error} />}
          <button type="button" className="btn-gold" disabled={busy} onClick={finish}>{busy ? 'Scoring…' : 'See my scorecard'}</button>
        </div>
      )}
    </div>
  )
}

// ------------------------------------------------------------------ scorecard

function Scorecard({ v, onClose }: { v: InterviewView; onClose: () => void }) {
  const sc = v.scorecard
  const ai = sc?.ai
  return (
    <section className="space-y-4" aria-label="Scorecard">
      <div className="card flex flex-wrap items-center gap-4">
        <ScoreRing value={Number(v.overallScore ?? 0)} />
        <div className="flex-1 min-w-[12rem]">
          <h2 className="text-xl font-semibold">📋 {roundName(v.roundType)} scorecard</h2>
          {sc?.method && <p className="text-xs text-muted">{sc.method}</p>}
          <p className="text-sm mt-1">Every fight makes the next one easier. Read the missing points, then try again tomorrow.</p>
        </div>
        <button type="button" className="btn-ghost text-sm" onClick={onClose}>Close</button>
      </div>

      {ai && (
        <div className="card border-accent/40 space-y-3">
          <h3 className="font-semibold">🤖 AI coach (AI can be wrong)</h3>
          {ai.summary && <p className="text-sm">{ai.summary}</p>}
          <div className="grid grid-cols-3 gap-2 text-center">
            {([['Communication', ai.communication], ['Technical depth', ai.technicalDepth], ['Confidence', ai.confidence]] as const).map(([k, n]) => (
              <div key={k} className="rounded-lg bg-raised p-2">
                <div className="text-xl font-bold font-display">{n ?? '–'}<span className="text-xs text-muted">/10</span></div>
                <div className="text-xs text-muted">{k}</div>
              </div>
            ))}
          </div>
          <div className="grid sm:grid-cols-2 gap-3 text-sm">
            {!!ai.strengths?.length && (
              <div><h4 className="font-medium text-good">💪 Strengths</h4><ul className="list-disc pl-5 space-y-1">{ai.strengths.map((s, i) => <li key={i}>{s}</li>)}</ul></div>
            )}
            {!!ai.improve?.length && (
              <div><h4 className="font-medium text-warn">🎯 Improve next time</h4><ul className="list-disc pl-5 space-y-1">{ai.improve.map((s, i) => <li key={i}>{s}</li>)}</ul></div>
            )}
          </div>
        </div>
      )}

      <ol className="space-y-3">
        {(sc?.questions ?? []).map((q, i) => (
          <li key={i} className="card space-y-2">
            <div className="flex items-start justify-between gap-2">
              <p className="font-medium">Q{i + 1}. {q.question}</p>
              <span className="whitespace-nowrap font-bold font-display">{q.answered ? `${q.score}/10` : <span className="chip">Skipped</span>}</span>
            </div>
            <div className="h-2 rounded-full bg-raised overflow-hidden" role="meter" aria-label={`Question ${i + 1} score`} aria-valuemin={0} aria-valuemax={10} aria-valuenow={q.score}>
              <div className={`h-full ${q.score >= 7 ? 'bg-good' : q.score >= 4 ? 'bg-accent' : 'bg-warn'}`} style={{ width: `${q.score * 10}%` }} />
            </div>
            {q.missingPoints?.length > 0 && (
              <div className="text-sm">
                <p className="text-muted">Key points to add next time:</p>
                <ul className="list-disc pl-5">{q.missingPoints.map((m, j) => <li key={j}>{m}</li>)}</ul>
              </div>
            )}
            {q.modelAnswer && (
              <details>
                <summary className="cursor-pointer text-sm text-primary">📘 Model answer</summary>
                <p className="mt-2 text-sm whitespace-pre-line rounded-lg bg-raised p-3">{q.modelAnswer}</p>
              </details>
            )}
          </li>
        ))}
      </ol>
    </section>
  )
}
