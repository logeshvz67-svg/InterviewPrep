import { useEffect, useState, type ReactNode } from 'react'
import { ranks, streakStatusText } from '../theme/gameConfig'
import type { LevelView, StreakView, XpAward } from '../api/types'

export function Spinner({ label = 'Loading…' }: { label?: string }) {
  return (
    <div className="flex items-center gap-3 text-muted p-6" role="status">
      <span className="h-5 w-5 rounded-full border-2 border-primary border-t-transparent animate-spin" />
      {label}
    </div>
  )
}

export function ErrorBox({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="card border-bad/50 text-bad whitespace-pre-line" role="alert">
      {message}
      {onRetry && <button className="btn-ghost ml-3 text-sm" onClick={onRetry}>Try again</button>}
    </div>
  )
}

export function Section({ title, action, children }: { title: string; action?: ReactNode; children: ReactNode }) {
  return (
    <section className="space-y-3">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">{title}</h2>
        {action}
      </div>
      {children}
    </section>
  )
}

export function XpBar({ level }: { level: LevelView }) {
  const r = ranks[level.rank] ?? { name: level.rank, icon: '⭐' }
  const next = level.nextRank ? ranks[level.nextRank]?.name ?? level.nextRank : null
  return (
    <div>
      <div className="flex items-baseline justify-between text-sm">
        <span className="font-semibold">{r.icon} {r.name} · Level {level.level}</span>
        <span className="text-muted">{level.totalXp} XP · <span className="text-gold">{level.gold} gold</span></span>
      </div>
      <div className="mt-2 h-3 rounded-full bg-raised overflow-hidden" aria-label={`Level progress ${level.levelProgressPercent}%`}>
        <div className="h-full bg-gradient-to-r from-primary to-accent transition-all duration-700" style={{ width: `${level.levelProgressPercent}%` }} />
      </div>
      <p className="mt-1 text-xs text-muted">
        {level.nextLevelXp - level.totalXp} XP to level {level.level + 1}
        {next && level.nextRankXp ? ` · ${level.nextRankXp - level.totalXp} XP to ${next}` : ''}
      </p>
    </div>
  )
}

export function StreakBadge({ streak }: { streak: StreakView }) {
  return (
    <div className="flex items-center gap-3">
      <div className="text-3xl" aria-hidden>{streak.status === 'PAUSED' ? '🕯️' : '🔥'}</div>
      <div>
        <div className="text-2xl font-bold font-display">{streak.current} <span className="text-sm font-normal text-muted">day{streak.current === 1 ? '' : 's'}</span></div>
        <div className="text-xs text-muted">{streakStatusText[streak.status]}</div>
        <div className="text-xs mt-0.5" title="Earn 1 freeze every 7-day streak (max 2). A freeze covers a missed day.">
          {Array.from({ length: streak.maxFreezes }).map((_, i) => (
            <span key={i} className={i < streak.freezes ? '' : 'opacity-25'}>🧊</span>
          ))}
          <span className="text-muted ml-1">freezes</span>
        </div>
      </div>
    </div>
  )
}

/** Small pop-up after earning XP. Says honestly when nothing was awarded. */
export function XpToast({ award, onDone }: { award: XpAward | null; onDone: () => void }) {
  const [show, setShow] = useState(false)
  useEffect(() => {
    if (!award) return
    setShow(true)
    const t = setTimeout(() => { setShow(false); onDone() }, 3200)
    return () => clearTimeout(t)
  }, [award, onDone])
  if (!award || !show) return null
  return (
    <div className="fixed bottom-24 md:bottom-6 left-1/2 -translate-x-1/2 z-50 card shadow-xl flex items-center gap-2 animate-[fadein_.2s_ease-out]" role="status">
      {award.awarded
        ? <><span className="text-gold font-bold">+{award.xp} XP</span><span className="text-muted">+{award.gold} gold</span></>
        : <span className="text-muted text-sm">{award.reason}</span>}
    </div>
  )
}

export function Stars({ value, onChange, label }: { value: number; onChange: (v: number) => void; label: string }) {
  return (
    <div role="radiogroup" aria-label={label} className="flex gap-1">
      {[1, 2, 3, 4, 5].map((n) => (
        <button type="button" key={n} role="radio" aria-checked={value === n} onClick={() => onChange(n)}
          className={`h-9 w-9 rounded-lg text-lg ${n <= value ? 'bg-gold/20 text-gold' : 'bg-raised text-muted'}`}>★</button>
      ))}
    </div>
  )
}

export function Tabs<T extends string>({ tabs, value, onChange }: { tabs: { id: T; label: string }[]; value: T; onChange: (v: T) => void }) {
  return (
    <div className="flex gap-1 overflow-x-auto pb-1 -mx-1 px-1" role="tablist">
      {tabs.map((t) => (
        <button key={t.id} role="tab" aria-selected={value === t.id} onClick={() => onChange(t.id)}
          className={`chip whitespace-nowrap px-3 py-1.5 text-sm ${value === t.id ? 'chip-on' : ''}`}>{t.label}</button>
      ))}
    </div>
  )
}
