import { useCallback, useEffect, useRef, useState, type ReactNode } from 'react'
import { countFillers, countWords, fmtTime, paceLabel, wpmOf } from './metrics'
import type { InputMethod } from './types'
import { SR_HELP, useSpeechRecognition } from './useSpeechRecognition'

/**
 * One answer, spoken (Web Speech API) or typed (fallback), with its own timer.
 * The parent reads text / seconds / longPauses / method when submitting, then calls reset().
 */
export function useAnswerInput(lang: string) {
  const sr = useSpeechRecognition(lang)
  const [method, setMethodState] = useState<InputMethod>(sr.supported ? 'VOICE' : 'TYPED')
  const [typed, setTypedState] = useState('')
  const [typedElapsed, setTypedElapsed] = useState(0)
  const [typedStartedAt, setTypedStartedAt] = useState<number | null>(null)
  const typedBaseRef = useRef(0)

  useEffect(() => {
    if (typedStartedAt == null) return
    const t = setInterval(() => setTypedElapsed(Math.floor((typedBaseRef.current + Date.now() - typedStartedAt) / 1000)), 250)
    return () => clearInterval(t)
  }, [typedStartedAt])

  const switchToTyped = useCallback((carryText: string, carrySeconds: number) => {
    setMethodState('TYPED')
    setTypedState((t) => t || carryText)
    if (carrySeconds > 0 && typedBaseRef.current === 0) {
      typedBaseRef.current = carrySeconds * 1000
      setTypedElapsed(carrySeconds)
    }
  }, [])

  // A fatal recognition error (unsupported, permission, network) -> fall back to typing, keeping what was heard.
  const handledErrorRef = useRef<string | null>(null)
  useEffect(() => {
    if (!sr.error) { handledErrorRef.current = null; return }
    if (handledErrorRef.current === sr.error) return
    handledErrorRef.current = sr.error
    if (method === 'VOICE') switchToTyped(sr.transcript, sr.elapsed)
  }, [sr.error, sr.transcript, sr.elapsed, method, switchToTyped])

  const setMethod = (m: InputMethod) => {
    if (m === method) return
    if (m === 'TYPED') { sr.stop(); switchToTyped(sr.transcript, sr.elapsed) } else setMethodState('VOICE')
  }

  const setTyped = (v: string) => {
    setTypedState(v)
    if (typedStartedAt == null && v.length > 0) setTypedStartedAt(Date.now())
  }

  const stopTypedTimer = () => {
    if (typedStartedAt != null) {
      typedBaseRef.current += Date.now() - typedStartedAt
      setTypedStartedAt(null)
      setTypedElapsed(Math.floor(typedBaseRef.current / 1000))
    }
  }

  const reset = () => {
    sr.reset()
    setTypedState('')
    setTypedElapsed(0)
    setTypedStartedAt(null)
    typedBaseRef.current = 0
    setMethodState(sr.supported ? 'VOICE' : 'TYPED')
  }

  const voice = method === 'VOICE'
  return {
    sr, method, setMethod, typed, setTyped, stopTypedTimer, reset,
    text: (voice ? sr.transcript : typed).trim(),
    seconds: voice ? sr.elapsed : typedElapsed,
    longPauses: voice ? sr.longPauses : 0,
    timerRunning: voice ? sr.listening : typedStartedAt != null,
  }
}

export type AnswerState = ReturnType<typeof useAnswerInput>

export function PrivacyNote() {
  return (
    <p className="text-xs text-muted flex gap-1.5 items-start">
      <span aria-hidden>🔒</span>
      <span>We never record, store or upload your audio. Only the text transcript is sent when you press submit.
        (Your browser's own speech service turns your voice into text.)</span>
    </p>
  )
}

export function LiveStats({ text, seconds, longPauses, showPauses }: { text: string; seconds: number; longPauses: number; showPauses: boolean }) {
  const words = countWords(text)
  const wpm = wpmOf(words, seconds)
  const f = countFillers(text)
  const pace = paceLabel(wpm, words)
  return (
    <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-center" aria-live="polite" aria-label="Live speaking stats">
      <Stat label="Words" value={String(words)} />
      <Stat label="Words / min" value={words < 5 ? '–' : String(wpm)} sub={<span className={pace.tone}>{pace.text}</span>} />
      <Stat label="Fillers" value={String(f.total)}
        sub={f.total > 0 ? <span className="text-warn">{Object.entries(f.fillers).map(([k, n]) => `"${k}"×${n}`).join(', ')}</span> : <span className="text-good">None so far</span>} />
      {showPauses
        ? <Stat label="Long pauses (>3s)" value={String(longPauses)} />
        : <Stat label="Time" value={fmtTime(seconds)} />}
    </div>
  )
}

function Stat({ label, value, sub }: { label: string; value: string; sub?: ReactNode }) {
  return (
    <div className="rounded-lg bg-raised p-2">
      <div className="text-xl font-bold font-display">{value}</div>
      <div className="text-xs text-muted">{label}</div>
      {sub && <div className="text-xs mt-0.5 truncate">{sub}</div>}
    </div>
  )
}

export function Countdown({ seconds, limit }: { seconds: number; limit: number }) {
  const left = limit - seconds
  const pct = Math.min(100, (seconds / Math.max(1, limit)) * 100)
  return (
    <div>
      <div className="flex justify-between text-sm">
        <span className="text-muted">⏱️ {fmtTime(seconds)} spoken</span>
        <span className={left >= 0 ? (left <= 10 ? 'text-warn font-semibold' : 'text-muted') : 'text-warn font-semibold'} aria-live="polite">
          {left >= 0 ? `${fmtTime(left)} left` : 'Time is up: finish your sentence'}
        </span>
      </div>
      <div className="mt-1 h-2 rounded-full bg-raised overflow-hidden" role="progressbar" aria-valuemin={0} aria-valuemax={limit} aria-valuenow={Math.min(seconds, limit)} aria-label="Time used">
        <div className={`h-full transition-all ${left >= 0 ? 'bg-accent' : 'bg-warn'}`} style={{ width: `${pct}%` }} />
      </div>
    </div>
  )
}

export function AnswerInput({ ans, timeLimit, disabled, placeholder = 'Type your answer here…', onBeforeRecord }: {
  ans: AnswerState; timeLimit?: number; disabled?: boolean; placeholder?: string; onBeforeRecord?: () => void
}) {
  const { sr } = ans
  const voice = ans.method === 'VOICE'
  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center gap-2" role="radiogroup" aria-label="How do you want to answer?">
        <button type="button" role="radio" aria-checked={voice} disabled={!sr.supported || sr.listening}
          className={`chip px-3 py-1.5 text-sm ${voice ? 'chip-on' : ''}`} onClick={() => ans.setMethod('VOICE')}>🎙️ Speak</button>
        <button type="button" role="radio" aria-checked={!voice} disabled={sr.listening}
          className={`chip px-3 py-1.5 text-sm ${!voice ? 'chip-on' : ''}`} onClick={() => ans.setMethod('TYPED')}>⌨️ Type</button>
        {!sr.supported && <span className="text-xs text-muted">{SR_HELP} Typing works everywhere.</span>}
      </div>

      {sr.error && <div className="card border-warn/50 text-warn text-sm" role="alert">{sr.error}</div>}

      {timeLimit ? <Countdown seconds={ans.seconds} limit={timeLimit} /> : null}

      {voice ? (
        <div className="space-y-2">
          <div className="flex flex-wrap gap-2">
            {!sr.listening ? (
              <button type="button" className="btn-primary" disabled={disabled}
                onClick={() => { onBeforeRecord?.(); sr.start() }}>
                🎙️ {ans.text ? 'Continue speaking' : 'Start speaking'}
              </button>
            ) : (
              <button type="button" className="btn-gold" onClick={sr.stop}>
                <span className="h-2.5 w-2.5 rounded-full bg-bad animate-pulse" aria-hidden /> Stop
              </button>
            )}
            {!timeLimit && <span className="self-center text-sm text-muted">⏱️ {fmtTime(ans.seconds)}</span>}
            {ans.text && !sr.listening && <button type="button" className="btn-ghost" onClick={ans.reset}>↺ Start over</button>}
          </div>
          <div className="min-h-[5rem] rounded-xl bg-raised border border-line p-3 text-sm whitespace-pre-wrap" aria-live="polite" aria-label="Live transcript">
            {sr.finalText}
            {sr.interim && <span className="text-muted"> {sr.interim}</span>}
            {!sr.transcript && <span className="text-muted">{sr.listening ? 'Listening… speak clearly, not too fast.' : 'Your words will appear here.'}</span>}
          </div>
          <p className="text-xs text-muted">Speech-to-text can mishear words. That's OK: the scores focus on pace, fillers and structure.</p>
        </div>
      ) : (
        <div className="space-y-1">
          <label className="label" htmlFor="answer-typed">Your answer {!timeLimit && <span className="font-normal">· ⏱️ {fmtTime(ans.seconds)}</span>}</label>
          <textarea id="answer-typed" className="input min-h-[8rem]" value={ans.typed} disabled={disabled}
            onChange={(e) => ans.setTyped(e.target.value)} placeholder={placeholder} maxLength={6000} />
          <p className="text-xs text-muted">The timer starts when you type the first letter. Tip: say each sentence aloud as you type it.</p>
        </div>
      )}

      <LiveStats text={ans.text} seconds={ans.seconds} longPauses={ans.longPauses} showPauses={voice} />
      <PrivacyNote />
    </div>
  )
}
