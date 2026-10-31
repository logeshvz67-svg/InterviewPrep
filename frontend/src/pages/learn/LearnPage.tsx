import { Link } from 'react-router-dom'
import type { WorldSummary } from '../../api/learning'
import { ErrorBox, Spinner } from '../../components/ui'
import { useApi } from '../../lib/hooks'
import { worlds as worldTheme } from '../../theme/gameConfig'

/** World map: every world with progress. Worlds with only an outline are marked "coming next". */
export default function LearnPage() {
  const { data, error, loading, reload } = useApi<WorldSummary[]>('/api/worlds/progress')
  if (loading && !data) return <Spinner />
  if (error || !data) return <ErrorBox message={error ?? 'No data'} onRetry={reload} />
  return (
    <div className="space-y-5">
      <header>
        <h1 className="text-2xl font-bold">🗺️ The Worlds</h1>
        <p className="text-sm text-muted">Travel from Java Core Island to the Offer Letter Castle. Pass a lesson's quiz ({'80%'}+) to unlock the next one.</p>
      </header>
      <div className="grid sm:grid-cols-2 gap-3">
        {data.map((w) => {
          const pct = w.lessonsWritten ? Math.round((w.lessonsCompleted / w.lessonsWritten) * 100) : 0
          const extra = EXTRA_LINKS[w.code]
          return (
            <div key={w.code} className="card space-y-2">
              <div className="flex items-start gap-3">
                <span className="text-4xl" aria-hidden>{worldTheme[w.code]?.icon ?? '🌍'}</span>
                <div className="flex-1">
                  <h2 className="font-semibold text-lg">{w.name}</h2>
                  <p className="text-sm text-muted">{w.description}</p>
                </div>
              </div>
              {w.lessonsWritten > 0 ? (
                <>
                  <div className="h-2 rounded bg-raised"><div className="h-full rounded bg-accent" style={{ width: `${pct}%` }} /></div>
                  <p className="text-xs text-muted">{w.lessonsCompleted}/{w.lessonsWritten} lessons{w.outlineCount ? ` · ${w.outlineCount} more planned` : ''}</p>
                </>
              ) : <p className="text-xs text-muted">{w.outlineCount} lessons planned (outline). Practice tools are already open.</p>}
              <div className="flex flex-wrap gap-2">
                <Link to={`/learn/${w.code}`} className={w.lessonsWritten ? 'btn-primary text-sm' : 'btn-ghost text-sm'}>{w.lessonsWritten ? 'Enter world' : 'See outline'}</Link>
                {extra?.map(([to, label]) => <Link key={to} to={to} className="btn-ghost text-sm">{label}</Link>)}
              </div>
            </div>
          )
        })}
      </div>
    </div>
  )
}

const EXTRA_LINKS: Record<string, [string, string][]> = {
  DSA: [['/gym/suggested', '🧩 Suggested problems'], ['/visualizers', '🎞️ Visualizers']],
  SQL: [['/interview?tab=bank&category=SQL', '🕳️ SQL practice questions']],
  CS_CORE: [['/interview?tab=bank&category=OS', '🛕 OS/DBMS/CN practice']],
  SPRING: [['/spring', '🔥 Spring Boot track']],
  INTERVIEW: [['/interview', '🏟️ Colosseum'], ['/interview/mock', '🐉 Boss fight']],
  APTITUDE: [['/interview?tab=aptitude', '🎯 Aptitude Arena']],
  SYSTEM_DESIGN: [['/mentor', '🧙 Practise a design with the mentor']],
}
