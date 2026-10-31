import { Link, useParams } from 'react-router-dom'
import { findVisualizer, VISUALIZERS } from '../../components/viz/registry'
import { Visualizer } from '../../components/viz/Visualizer'

/**
 * /visualizers      -> grid of all visualizers
 * /visualizers/:id  -> one visualizer, full width
 */
export default function VisualizersPage() {
  const { id } = useParams<{ id?: string }>()
  if (id) return <Detail id={id} />

  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-2xl font-bold">🎬 Visualizers</h1>
        <p className="text-muted mt-1">Watch an algorithm step by step. Pause any time. Turn on 🎯 Predict mode to guess each next step.</p>
      </div>
      <ul className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {VISUALIZERS.map((v) => (
          <li key={v.id} className="card flex flex-col gap-3">
            <div className="flex items-start gap-3">
              <span className="text-3xl" aria-hidden>{v.icon}</span>
              <div>
                <h2 className="text-lg font-semibold">{v.title}</h2>
                <p className="text-sm text-muted">{v.description}</p>
              </div>
            </div>
            <div className="mt-auto flex flex-wrap items-center gap-2">
              <Link to={`/visualizers/${v.id}`} className="btn-primary">▶ Open</Link>
              {v.lessonSlug && <Link to={`/lesson/${v.lessonSlug}`} className="btn-ghost text-sm">📖 Related lesson</Link>}
            </div>
          </li>
        ))}
      </ul>
    </div>
  )
}

function Detail({ id }: { id: string }) {
  const info = findVisualizer(id)
  return (
    <div className="space-y-4">
      <Link to="/visualizers" className="text-sm text-muted hover:text-ink">← All visualizers</Link>
      {info ? (
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h1 className="text-2xl font-bold"><span aria-hidden>{info.icon}</span> {info.title}</h1>
            <p className="text-muted mt-1">{info.description}</p>
          </div>
          {info.lessonSlug && <Link to={`/lesson/${info.lessonSlug}`} className="btn-ghost text-sm">📖 Related lesson</Link>}
        </div>
      ) : (
        <h1 className="text-2xl font-bold">Visualizer not found</h1>
      )}
      <Visualizer id={id} />
    </div>
  )
}
