import { useState } from 'react'
import { SCORE_LABELS } from './metrics'
import type { SessionView } from './types'

export function Notice({ text }: { text: string | null | undefined }) {
  if (!text) return null
  return <div className="card border-warn/40 text-sm flex gap-2" role="status"><span aria-hidden>😴</span><span>{text}</span></div>
}

export function ScoreBars({ scores }: { scores: Record<string, number> | null | undefined }) {
  const entries = Object.entries(scores ?? {})
  if (!entries.length) return null
  return (
    <ul className="space-y-2">
      {entries.map(([k, v]) => {
        const tone = v >= 8 ? 'bg-good' : v >= 5 ? 'bg-accent' : 'bg-warn'
        return (
          <li key={k}>
            <div className="flex justify-between text-sm"><span>{SCORE_LABELS[k] ?? k}</span><span className="text-muted">{v}/10</span></div>
            <div className="h-2 rounded-full bg-raised overflow-hidden" role="meter" aria-label={SCORE_LABELS[k] ?? k} aria-valuemin={0} aria-valuemax={10} aria-valuenow={v}>
              <div className={`h-full ${tone} transition-all duration-700`} style={{ width: `${v * 10}%` }} />
            </div>
          </li>
        )
      })}
    </ul>
  )
}

export function ScoreRing({ value }: { value: number }) {
  const r = 34
  const c = 2 * Math.PI * r
  const pct = Math.max(0, Math.min(100, value)) / 100
  return (
    <svg viewBox="0 0 80 80" className="h-24 w-24" role="img" aria-label={`Overall score ${value} out of 100`}>
      <circle cx="40" cy="40" r={r} className="stroke-raised" strokeWidth="8" fill="none" />
      <circle cx="40" cy="40" r={r} className="stroke-primary transition-all duration-700" strokeWidth="8" fill="none" strokeLinecap="round"
        strokeDasharray={`${c * pct} ${c}`} transform="rotate(-90 40 40)" />
      <text x="40" y="45" textAnchor="middle" className="fill-ink font-display" fontSize="18" fontWeight="700">{Math.round(value)}</text>
    </svg>
  )
}

/** Full results for one speaking session. */
export function SessionResult({ s, sampleAnswer, notice }: { s: SessionView; sampleAnswer?: string | null; notice?: string | null }) {
  const [showTranscript, setShowTranscript] = useState(false)
  const fillers = Object.entries(s.fillers ?? {})
  const feedback = (s.feedback ?? '').split('\n').map((l) => l.replace(/^[-•*]\s*/, '').trim()).filter(Boolean)
  return (
    <div className="space-y-4" aria-live="polite">
      <Notice text={notice} />
      <div className="card flex flex-wrap items-center gap-4">
        <ScoreRing value={Number(s.overallScore ?? 0)} />
        <div className="grid grid-cols-3 gap-3 flex-1 min-w-[14rem] text-center">
          <div><div className="text-2xl font-bold font-display">{s.wpm}</div><div className="text-xs text-muted">words / min<br />(aim 110-160)</div></div>
          <div><div className="text-2xl font-bold font-display">{s.fillerCount}</div><div className="text-xs text-muted">fillers</div></div>
          <div><div className="text-2xl font-bold font-display">{s.longPauses}</div><div className="text-xs text-muted">long pauses</div></div>
        </div>
        <p className="w-full text-xs text-muted">{s.wordCount} words in {s.durationSeconds}s · {s.inputMethod === 'VOICE' ? '🎙️ spoken' : '⌨️ typed'}
          {s.aiUsed ? ' · AI feedback included (AI can be wrong)' : ' · measured without AI'}</p>
        {fillers.length > 0 && (
          <div className="w-full flex flex-wrap gap-1.5" aria-label="Fillers you used">
            {fillers.map(([k, n]) => <span key={k} className="chip bg-warn/15 text-warn">"{k}" × {n}</span>)}
          </div>
        )}
      </div>

      <div className="card space-y-2">
        <h3 className="font-semibold">📊 Scores (0-10)</h3>
        <ScoreBars scores={s.scores} />
      </div>

      {feedback.length > 0 && (
        <div className="card space-y-2">
          <h3 className="font-semibold">💬 Feedback</h3>
          <ul className="list-disc pl-5 space-y-1 text-sm">{feedback.map((f, i) => <li key={i}>{f}</li>)}</ul>
        </div>
      )}

      {s.improvedAnswer && (
        <div className="card border-accent/40 space-y-2">
          <h3 className="font-semibold">✨ A better version (in your voice)</h3>
          <p className="text-sm whitespace-pre-line">{s.improvedAnswer}</p>
          <p className="text-xs text-muted">Read it aloud 2 times, then try again in your own words.</p>
        </div>
      )}

      {sampleAnswer && (
        <details className="card">
          <summary className="font-semibold cursor-pointer">📘 Compare with the sample answer</summary>
          <p className="mt-2 text-sm whitespace-pre-line">{sampleAnswer}</p>
          <p className="mt-1 text-xs text-muted">Samples are models to adapt. Replace [brackets] with your own true details.</p>
        </details>
      )}

      <div>
        <button type="button" className="btn-ghost text-sm" aria-expanded={showTranscript} onClick={() => setShowTranscript(!showTranscript)}>
          {showTranscript ? 'Hide' : 'Show'} what you said
        </button>
        {showTranscript && <p className="mt-2 text-sm text-muted whitespace-pre-line rounded-lg bg-raised p-3">{s.transcript}</p>}
      </div>
    </div>
  )
}

/** Tiny inline SVG trend line (no chart library). */
export function MiniChart({ label, values, labels, unit, goodHigh = true, kind = 'line' }: {
  label: string; values: (number | null)[]; labels: string[]; unit?: string; goodHigh?: boolean; kind?: 'line' | 'bar'
}) {
  const pts = values.map((v, i) => ({ v, i })).filter((p): p is { v: number; i: number } => p.v != null && Number.isFinite(p.v))
  const W = 160
  const H = 48
  const last = pts.length ? pts[pts.length - 1].v : null
  const prev = pts.length > 1 ? pts[pts.length - 2].v : null
  const trend = last != null && prev != null ? (last > prev ? '▲' : last < prev ? '▼' : '•') : ''
  const better = last != null && prev != null && last !== prev ? (last > prev) === goodHigh : null
  const max = Math.max(1, ...pts.map((p) => p.v))
  const min = kind === 'bar' ? 0 : Math.min(...pts.map((p) => p.v), max)
  const span = Math.max(1, max - min)
  const n = Math.max(1, values.length - 1)
  const x = (i: number) => (values.length === 1 ? W / 2 : 4 + (i / n) * (W - 8))
  const y = (v: number) => H - 4 - ((v - min) / span) * (H - 12)
  const desc = pts.map((p) => `${labels[p.i]}: ${p.v}${unit ?? ''}`).join(', ')
  return (
    <div className="card p-3">
      <div className="flex items-baseline justify-between">
        <span className="text-sm text-muted">{label}</span>
        <span className="font-bold font-display">
          {last ?? '–'}{last != null && unit}
          {trend && <span className={`ml-1 text-xs ${better == null ? 'text-muted' : better ? 'text-good' : 'text-warn'}`}>{trend}</span>}
        </span>
      </div>
      {pts.length === 0 ? <p className="text-xs text-muted mt-2">No data yet</p> : (
        <svg viewBox={`0 0 ${W} ${H}`} className="w-full h-12 mt-1" role="img" aria-label={`${label} by week. ${desc}`}>
          {kind === 'bar'
            ? pts.map((p) => {
              const bw = Math.max(4, (W - 8) / values.length - 4)
              return <rect key={p.i} x={x(p.i) - bw / 2} y={y(p.v)} width={bw} height={H - 4 - y(p.v)} rx="2" className="fill-primary/70" />
            })
            : <>
              <polyline fill="none" className="stroke-accent" strokeWidth="2" strokeLinejoin="round" strokeLinecap="round"
                points={pts.map((p) => `${x(p.i)},${y(p.v)}`).join(' ')} />
              {pts.map((p) => <circle key={p.i} cx={x(p.i)} cy={y(p.v)} r="2.5" className="fill-accent" />)}
            </>}
        </svg>
      )}
    </div>
  )
}
