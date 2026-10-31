import type { RoadmapView } from '../api/types'
import { ErrorBox, Spinner } from '../components/ui'
import { useApi } from '../lib/hooks'

export default function RoadmapPage() {
  const { data, error, loading, reload } = useApi<RoadmapView>('/api/roadmap')
  if (loading && !data) return <Spinner />
  if (error || !data) return <ErrorBox message={error ?? 'No roadmap'} onRetry={reload} />
  return (
    <div className="space-y-5">
      <header>
        <h1 className="text-2xl font-bold">🗺️ Roadmap</h1>
        <p className="text-sm text-muted">{data.totalWeeks} weeks from {data.startDate}. You're in week {data.currentWeek}. Change your test date in Settings to regenerate it.</p>
      </header>
      <ol className="relative border-l-2 border-line ml-3 space-y-4">
        {data.weeks.map((w) => {
          const current = w.week === data.currentWeek
          const past = w.week < data.currentWeek
          return (
            <li key={w.week} className="ml-5">
              <span className={`absolute -left-[9px] mt-5 h-4 w-4 rounded-full border-2 ${current ? 'bg-primary border-primary' : past ? 'bg-accent border-accent' : 'bg-bg border-line'}`} aria-hidden />
              <div className={`card ${current ? 'ring-2 ring-primary' : ''} ${w.laterLane ? 'border-dashed' : ''}`}>
                <p className="text-xs text-muted">Week {w.week} · from {w.startDate} · {w.phase}{current ? ' · 📍 you are here' : ''}</p>
                <p className="font-semibold">{w.title}</p>
                <ul className="list-disc ml-5 text-sm text-muted mt-1">{w.focus.map((f, i) => <li key={i}>{f}</li>)}</ul>
              </div>
            </li>
          )
        })}
      </ol>
    </div>
  )
}
