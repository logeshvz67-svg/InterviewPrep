import { useEffect, useId, useRef, useState, type KeyboardEvent, type ReactNode } from 'react'
import { CodePanel } from './draw'
import { usePrefersReducedMotion } from './lib'
import type { BaseStep } from './types'

/**
 * The reusable "video player" for every visualizer.
 *
 * A visualizer gives us a list of steps (pictures of the data + a caption).
 * The player only moves an index through that list:
 *   play / pause, step back, step forward, reset, speed, progress.
 *
 * Predict mode: before showing a step that has a `predict` question, we stop and ask it.
 * The learner picks an answer, sees right/wrong + a short reason, then reveals the step.
 * A streak counts correct answers in a row (local only, nothing is saved).
 *
 * Keyboard (when focus is anywhere inside the player):
 *   Space = play / pause, ← / → = step back / forward, R = reset, 1-4 = answer a question.
 */

const SPEEDS = [
  { label: 'Slow', ms: 2400 },
  { label: 'Normal', ms: 1500 },
  { label: 'Fast', ms: 800 },
]

export function StepPlayer<S extends BaseStep>({ steps, render, code, label }: {
  steps: S[]
  render: (step: S, index: number) => ReactNode
  /** Optional Java code. Steps with `line` highlight that line. */
  code?: string[]
  /** What this player shows, for screen readers (e.g. "Linear search"). */
  label: string
}) {
  const reduced = usePrefersReducedMotion()
  const [index, setIndex] = useState(0)
  const [playing, setPlaying] = useState(false)
  const [speed, setSpeed] = useState(reduced ? 0 : 1)
  const [predictMode, setPredictMode] = useState(false)
  const [asking, setAsking] = useState(false)
  const [chosen, setChosen] = useState<number | null>(null)
  const [streak, setStreak] = useState(0)
  const [best, setBest] = useState(0)
  const [score, setScore] = useState({ right: 0, total: 0 })
  // Steps whose question was already scored (going back and answering again does not count twice).
  const [scored, setScored] = useState<ReadonlySet<number>>(new Set())

  // New input = new steps: start again from the first step (the streak is kept).
  const [prevSteps, setPrevSteps] = useState(steps)
  if (prevSteps !== steps) {
    setPrevSteps(steps)
    setIndex(0); setPlaying(false); setAsking(false); setChosen(null); setScored(new Set())
  }

  const last = steps.length - 1
  const at = Math.min(index, last)
  const step = steps[at]
  const upcoming = at < last ? steps[at + 1] : undefined
  const question = asking ? upcoming?.predict : undefined

  const box = useRef<HTMLDivElement>(null)
  // After the question closes, keep keyboard focus inside the player so the keys still work.
  const reveal = () => { setIndex(at + 1); setAsking(false); setChosen(null); box.current?.focus({ preventScroll: true }) }

  const forward = () => {
    if (at >= last) { setPlaying(false); return }
    if (asking) { if (chosen !== null) reveal(); return }
    if (predictMode && upcoming?.predict) { setAsking(true); setChosen(null); setPlaying(false); return }
    setIndex(at + 1)
  }
  const back = () => { setPlaying(false); setAsking(false); setChosen(null); setIndex(Math.max(0, at - 1)) }
  const reset = () => { setPlaying(false); setAsking(false); setChosen(null); setIndex(0) }
  const togglePlay = () => {
    if (asking) return
    if (playing) { setPlaying(false); return }
    if (at >= last) setIndex(0)
    setPlaying(true)
  }

  const choose = (i: number) => {
    if (!question || chosen !== null) return
    setChosen(i)
    const target = at + 1
    if (scored.has(target)) return
    setScored(new Set(scored).add(target))
    const right = i === question.answer
    setScore((s) => ({ right: s.right + (right ? 1 : 0), total: s.total + 1 }))
    const next = right ? streak + 1 : 0
    setStreak(next)
    setBest((b) => Math.max(b, next))
  }

  // Auto-play: one step every `ms`. Uses a ref so the timer always calls the latest `forward`.
  const forwardRef = useRef(forward)
  useEffect(() => { forwardRef.current = forward })
  useEffect(() => {
    if (!playing) return
    const t = setTimeout(() => forwardRef.current(), SPEEDS[speed].ms)
    return () => clearTimeout(t)
  }, [playing, at, speed])

  // Move keyboard focus to the question, then to the "Show the step" button after answering.
  const firstOption = useRef<HTMLButtonElement>(null)
  const revealBtn = useRef<HTMLButtonElement>(null)
  useEffect(() => { if (asking && chosen === null) firstOption.current?.focus() }, [asking, chosen])
  useEffect(() => { if (chosen !== null) revealBtn.current?.focus() }, [chosen])

  const onKeyDown = (e: KeyboardEvent<HTMLDivElement>) => {
    const target = e.target as HTMLElement
    if (target.closest('input, textarea, select')) return
    if (e.altKey || e.ctrlKey || e.metaKey) return
    if (e.key === ' ' && target.tagName !== 'BUTTON') { e.preventDefault(); togglePlay() }
    else if (e.key === 'ArrowRight') { e.preventDefault(); forward() }
    else if (e.key === 'ArrowLeft') { e.preventDefault(); back() }
    else if (e.key === 'r' || e.key === 'R') { e.preventDefault(); reset() }
    else if (question && /^[1-4]$/.test(e.key)) {
      const i = Number(e.key) - 1
      if (i < question.options.length) { e.preventDefault(); choose(i) }
    }
  }

  const headingId = useId()
  const percent = steps.length <= 1 ? 100 : Math.round((at / last) * 100)

  return (
    <div ref={box} className="card space-y-4 focus-visible:ring-2" tabIndex={0} onKeyDown={onKeyDown}
      role="group" aria-labelledby={headingId}>
      <p id={headingId} className="sr-only">{label} visualizer. Keys: Space to play or pause, left and right arrows to step, R to reset.</p>

      {/* The picture for this step */}
      <div className="rounded-xl bg-bg/60 border border-line/40 p-3 overflow-x-auto">{render(step, at)}</div>

      {code && <CodePanel code={code} line={step.line} />}

      {/* Big, simple caption. Read out by screen readers when it changes. */}
      <p className="text-xl md:text-2xl font-semibold leading-snug min-h-[3.5rem]" aria-live="polite">{step.caption}</p>

      {question && (
        <div className="rounded-xl border-2 border-gold/60 bg-gold/10 p-3 space-y-3" role="region" aria-label="Predict the next step">
          <p className="font-semibold text-lg">🤔 {question.question}</p>
          <div className="grid gap-2 sm:grid-cols-2">
            {question.options.map((opt, i) => {
              const isRight = i === question.answer
              const picked = chosen === i
              const look = chosen === null ? 'bg-raised hover:bg-line/60'
                : isRight ? 'bg-good/20 ring-2 ring-good'
                  : picked ? 'bg-bad/20 ring-2 ring-bad' : 'bg-raised opacity-60'
              return (
                <button key={opt} ref={i === 0 ? firstOption : undefined} type="button" disabled={chosen !== null && !picked && !isRight}
                  className={`btn justify-start text-left min-h-[48px] ${look}`} onClick={() => choose(i)}
                  aria-pressed={picked}>
                  <span className="chip shrink-0">{i + 1}</span> {opt}
                  {chosen !== null && isRight && <span className="ml-auto" aria-label="right answer">✓</span>}
                  {picked && !isRight && <span className="ml-auto" aria-label="your answer, wrong">✗</span>}
                </button>
              )
            })}
          </div>
          {chosen !== null && (
            <div className="space-y-2" aria-live="assertive">
              <p className={`font-semibold ${chosen === question.answer ? 'text-good' : 'text-bad'}`}>
                {chosen === question.answer ? `Yes! Correct. ${streak > 1 ? `🔥 ${streak} in a row!` : ''}` : 'Not quite. Look at the right answer in green.'}
              </p>
              {question.explain && <p className="text-muted">{question.explain}</p>}
              <button ref={revealBtn} type="button" className="btn-primary" onClick={reveal}>Show the step →</button>
            </div>
          )}
        </div>
      )}

      {/* Progress */}
      <div>
        <div className="flex justify-between text-sm text-muted mb-1">
          <span>Step {at + 1} of {steps.length}</span>
          {at === last && <span className="text-good font-semibold">Finished ✓</span>}
        </div>
        <div className="h-2.5 rounded-full bg-raised overflow-hidden" role="progressbar" aria-label="Progress"
          aria-valuemin={1} aria-valuemax={steps.length} aria-valuenow={at + 1}>
          <div className="h-full bg-gradient-to-r from-primary to-accent motion-safe:transition-all" style={{ width: `${percent}%` }} />
        </div>
      </div>

      {/* Controls */}
      <div className="flex flex-wrap items-center gap-2">
        <button type="button" className="btn-ghost h-12 w-12 p-0 text-lg" onClick={reset} aria-label="Reset to the first step" title="Reset (R)">⟲</button>
        <button type="button" className="btn-ghost h-12 w-12 p-0 text-lg aria-disabled:opacity-40" onClick={back} aria-disabled={at === 0} aria-label="Step back" title="Step back (←)">◀</button>
        <button type="button" className="btn-primary h-12 min-w-[7rem] aria-disabled:opacity-40" onClick={togglePlay} aria-disabled={asking}
          aria-label={playing ? 'Pause' : 'Play'} title="Play / pause (Space)">{playing ? '⏸ Pause' : '▶ Play'}</button>
        <button type="button" className="btn-ghost h-12 w-12 p-0 text-lg aria-disabled:opacity-40" onClick={forward}
          aria-disabled={at >= last || (asking && chosen === null)} aria-label="Step forward" title="Step forward (→)">▶</button>
        <div className="flex gap-1 ml-auto" role="radiogroup" aria-label="Speed">
          {SPEEDS.map((s, i) => (
            <button key={s.label} type="button" role="radio" aria-checked={speed === i}
              className={`chip px-3 py-2 text-sm ${speed === i ? 'chip-on' : ''}`} onClick={() => setSpeed(i)}>{s.label}</button>
          ))}
        </div>
      </div>

      {/* Predict mode toggle + streak */}
      <div className="flex flex-wrap items-center gap-3 border-t border-line/40 pt-3">
        <label className="flex items-center gap-2 cursor-pointer font-medium">
          <input type="checkbox" className="h-5 w-5 accent-[rgb(var(--c-primary))]" checked={predictMode}
            onChange={(e) => { setPredictMode(e.target.checked); setAsking(false); setChosen(null) }} />
          🎯 Predict the next step
        </label>
        {predictMode && (
          <span className="text-sm text-muted" aria-live="polite">
            Streak <b className="text-gold">🔥 {streak}</b> · Best {best} · {score.right}/{score.total} right
          </span>
        )}
        <span className="text-xs text-muted ml-auto hidden md:inline">Keys: Space play/pause · ← → step · R reset</span>
      </div>
    </div>
  )
}
