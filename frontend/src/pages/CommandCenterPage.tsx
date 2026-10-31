import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import type { CommandCenter } from '../api/types'
import { RadarChart } from '../components/RadarChart'
import { ErrorBox, Section, Spinner, StreakBadge, XpBar } from '../components/ui'
import { useApi } from '../lib/hooks'
import { avatars, labels } from '../theme/gameConfig'

const COMPONENT_LABELS: Record<string, string> = {
  JAVA: 'Java', DSA: 'DSA', SQL: 'SQL', APTITUDE: 'Aptitude', MOCKS: 'Mock tests', COMMUNICATION: 'Communication',
}

export default function CommandCenterPage() {
  const { data, error, loading, reload } = useApi<CommandCenter>('/api/command-center')
  const nav = useNavigate()

  if (loading && !data) return <Spinner />
  if (error || !data) return <ErrorBox message={error ?? 'No data'} onRetry={reload} />

  const avatar = avatars.find((a) => a.id === data.avatar)?.icon ?? '🛡️'
  const doneQuests = data.todayQuests.filter((q) => q.completed).length

  async function startMvd() {
    try {
      await api('/api/focus/start', { method: 'POST', body: { mode: 'MVD', topic: data!.currentWeekTitle ?? 'Minimum Viable Day', plannedMinutes: 20 } })
    } catch { /* already running: just go there */ }
    nav('/focus')
  }

  async function ackComeback() {
    await api('/api/streak/comeback/ack', { method: 'POST' }).catch(() => undefined)
    void reload()
  }

  return (
    <div className="space-y-6">
      <header className="flex items-center gap-3">
        <div className="text-4xl" aria-hidden>{avatar}</div>
        <div>
          <h1 className="text-2xl font-bold">Hi, {data.characterName}</h1>
          <p className="text-muted text-sm">{data.welcomeMessage}</p>
        </div>
      </header>

      {data.streak.comebackPending && (
        <div className="card border-accent/60 bg-accent/10 space-y-2">
          <h2 className="font-semibold">🌅 Welcome back, adventurer</h2>
          <p className="text-sm">You were away {data.streak.daysAway} days. That's okay: progress isn't lost, and there's nothing to "catch up". Today's quests are lighter on purpose.</p>
          {data.whyStatement && <p className="text-sm italic text-muted">Your why: “{data.whyStatement}”</p>}
          <button className="btn-ghost text-sm" onClick={ackComeback}>Thanks, let's go</button>
        </div>
      )}

      {/* The ONE next step, big and obvious */}
      <Link to={data.nextAction.route} className="card block bg-gradient-to-br from-primary/25 to-accent/10 border-primary/40 hover:brightness-110">
        <p className="text-xs uppercase tracking-wide text-primary font-semibold">{labels.doThisNow}</p>
        <p className="text-lg font-semibold mt-1">{data.nextAction.title}</p>
        <p className="text-sm text-muted">{data.nextAction.detail}</p>
      </Link>

      <div className="grid md:grid-cols-2 gap-4">
        <div className="card"><XpBar level={data.level} /><p className="text-xs text-muted mt-2">Today: +{data.todayXp} XP</p></div>
        <div className="card flex items-center justify-between">
          <StreakBadge streak={data.streak} />
          <button className="btn-ghost text-sm" onClick={startMvd} title="20 focused minutes keeps your streak alive">⚡ {labels.mvd}</button>
        </div>
      </div>

      <Section title={`${labels.questBoard} (${doneQuests}/${data.todayQuests.length})`}>
        <ul className="grid gap-2">
          {data.todayQuests.map((q) => (
            <li key={q.id} className={`card flex items-center gap-3 ${q.completed ? 'opacity-70' : ''}`}>
              <span className="text-2xl" aria-hidden>{q.completed ? '✅' : '📜'}</span>
              <div className="flex-1">
                <p className={`font-medium ${q.completed ? 'line-through' : ''}`}>{q.title}</p>
                <p className="text-xs text-muted">{q.description}</p>
              </div>
              <span className="text-gold text-sm whitespace-nowrap">+{q.goldReward} gold</span>
            </li>
          ))}
        </ul>
      </Section>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        <Stat label={labels.recallQueue} value={data.recallQueueCount} hint="Problems due for a re-solve" to="/gym/revisits" />
        <Stat label="Days to test" value={data.daysToTest ?? '—'} hint={data.daysToTest == null ? 'Add a date in Settings' : 'Countdown'} to="/settings" />
        <Stat label="Roadmap week" value={data.currentWeek || '—'} hint={data.currentWeekTitle ?? ''} to="/roadmap" />
        <Stat label={labels.readiness} value={`${data.readiness.score}%`} hint={data.readiness.companyCode.replace('_', ' ')} to="#readiness" />
      </div>

      <div className="grid md:grid-cols-2 gap-4">
        <Section title="Skill radar">
          <div className="card"><RadarChart points={data.radar} /></div>
        </Section>
        <Section title={`${labels.readiness}: ${data.readiness.companyCode.replace('_', ' ')}`}>
          <div className="card space-y-3" id="readiness">
            <div className="text-4xl font-bold font-display">{data.readiness.score}<span className="text-lg text-muted">/100</span></div>
            <ul className="space-y-1.5 text-sm">
              {data.readiness.components.map((c) => (
                <li key={c.component}>
                  <div className="flex justify-between">
                    <span>{COMPONENT_LABELS[c.component]} <span className="text-muted">({Math.round(c.weight * 100)}%)</span></span>
                    <span className={c.tracked ? '' : 'text-muted'}>{c.tracked ? `${Math.round(c.value * 100)}%` : 'not tracked yet'}</span>
                  </div>
                  <div className="h-1.5 rounded bg-raised"><div className="h-full rounded bg-accent" style={{ width: `${c.value * 100}%` }} /></div>
                </li>
              ))}
            </ul>
            <p className="text-sm"><b>Improve next:</b> {data.readiness.improveNextTip}</p>
            <p className="text-xs text-muted">Only {data.readiness.trackedWeightPercent}% of this score can be measured today; the rest unlocks as you use more of the app. The weights are our prep estimate, not an official formula.</p>
          </div>
        </Section>
      </div>

      <Section title="Quick actions">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
          <Link className="btn-primary" to="/curriculum">📖 Start Learning</Link>
          <Link className="btn-ghost" to="/gym/log">🧩 Solve a Problem</Link>
          <button className="btn-ghost" disabled title="Arrives in Phase 3">🎙️ Practice Speaking</button>
          <button className="btn-ghost" disabled title="Arrives in Phase 3">🧙 Ask Mentor</button>
        </div>
      </Section>
    </div>
  )
}

function Stat({ label, value, hint, to }: { label: string; value: string | number; hint: string; to: string }) {
  const body = (
    <>
      <p className="text-xs text-muted">{label}</p>
      <p className="text-2xl font-bold font-display">{value}</p>
      <p className="text-xs text-muted truncate">{hint}</p>
    </>
  )
  return to.startsWith('#') ? <a href={to} className="card block">{body}</a> : <Link to={to} className="card block hover:bg-raised">{body}</Link>
}
