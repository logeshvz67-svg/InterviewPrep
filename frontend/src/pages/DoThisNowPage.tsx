import { Link } from 'react-router-dom'
import type { NextAction } from '../api/types'
import { ErrorBox, Spinner } from '../components/ui'
import { useApi } from '../lib/hooks'

/** Distraction-free: ONE thing to do. Nothing else on screen. */
export default function DoThisNowPage() {
  const { data, error, loading, reload } = useApi<NextAction>('/api/next-action')
  if (loading && !data) return <Spinner />
  if (error || !data) return <ErrorBox message={error ?? 'No data'} onRetry={reload} />
  return (
    <div className="min-h-[70vh] grid place-items-center text-center">
      <div className="space-y-6 max-w-md">
        <p className="text-sm uppercase tracking-widest text-primary">Your next step</p>
        <h1 className="text-3xl md:text-4xl font-bold">{data.title}</h1>
        <p className="text-muted">{data.detail}</p>
        <Link to={data.route} className="btn-primary text-lg px-8 py-3">Start</Link>
        <p className="text-xs text-muted">Just this. Nothing else needs your attention right now.</p>
      </div>
    </div>
  )
}
