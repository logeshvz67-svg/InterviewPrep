import type { RadarPoint } from '../api/types'

/**
 * Hand-made SVG radar (no chart library = smaller, faster app).
 * Two shapes: what you rated yourself (dashed) and what the app has measured (filled).
 */
export function RadarChart({ points, size = 240 }: { points: RadarPoint[]; size?: number }) {
  const c = size / 2
  const r = size / 2 - 34
  const n = points.length
  const angle = (i: number) => (Math.PI * 2 * i) / n - Math.PI / 2
  const at = (i: number, v: number) => [c + Math.cos(angle(i)) * r * (v / 5), c + Math.sin(angle(i)) * r * (v / 5)]
  const poly = (vals: (number | null)[]) => vals.map((v, i) => at(i, v ?? 0).join(',')).join(' ')

  return (
    <figure>
      <svg viewBox={`-40 0 ${size + 80} ${size}`} className="w-full max-w-[320px] mx-auto" role="img"
        aria-label={points.map((p) => `${p.area}: self ${p.selfRated ?? '-'} of 5, measured ${p.measured ?? 'not tracked yet'}`).join('; ')}>
        {[1, 2, 3, 4, 5].map((lvl) => (
          <polygon key={lvl} points={poly(points.map(() => lvl))} fill="none" className="stroke-line" strokeWidth={1} />
        ))}
        {points.map((_, i) => { const [x, y] = at(i, 5); return <line key={i} x1={c} y1={c} x2={x} y2={y} className="stroke-line" /> })}
        <polygon points={poly(points.map((p) => p.selfRated))} className="fill-transparent stroke-muted" strokeDasharray="4 3" strokeWidth={1.5} />
        <polygon points={poly(points.map((p) => p.measured))} className="fill-primary/30 stroke-primary" strokeWidth={2} />
        {points.map((p, i) => {
          const [x, y] = at(i, 6.1)
          return <text key={p.area} x={x} y={y} textAnchor="middle" dominantBaseline="middle" className="fill-muted text-[10px]">{p.area}</text>
        })}
      </svg>
      <figcaption className="flex justify-center gap-4 text-xs text-muted mt-1">
        <span><span className="inline-block w-3 border-t-2 border-dashed border-muted align-middle mr-1" />Self-rated</span>
        <span><span className="inline-block w-3 h-2 bg-primary/40 border border-primary align-middle mr-1" />Measured</span>
      </figcaption>
    </figure>
  )
}
