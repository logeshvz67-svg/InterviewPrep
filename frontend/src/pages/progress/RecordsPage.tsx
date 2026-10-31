import { api, downloadFile } from '../../api/client'
import { ErrorBox, Section, Spinner } from '../../components/ui'
import { useApi } from '../../lib/hooks'

type Row = Record<string, unknown>
interface Records {
  today: string; heatmap: Row[]; xpByDay: Row[]; totals: Row; hoursByTopic: Row[]; problemsByPattern: Row[]; problemsByDifficulty: Row[]
  firstAttemptRate: Row; hintTrend: Row[]; recallByTopic: Row[]; quizBySection: Row[]; speakingTrend: Row[]; mockScores: Row[]
  mockInterviewScores: Row[]; readinessHistory: Row[]; mistakesByCategory: Row[]
}
const n = (v: unknown) => Number(v ?? 0)

/** Hall of Records: your history. Compared only with your past self. */
export default function RecordsPage() {
  const { data, error, loading, reload } = useApi<Records>('/api/records')
  if (loading && !data) return <Spinner />
  if (error || !data) return <ErrorBox message={error ?? 'No data'} onRetry={reload} />
  const t = data.totals
  const fa = data.firstAttemptRate
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">🏛️ Hall of Records</h1>
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        {[['XP', t.xp], ['Days studied', t.days_studied], ['Focus hours', Math.round(n(t.focus_minutes) / 6) / 10], ['Lessons', t.lessons],
          ['Problems', t.problems], ['Card reviews', t.reviews], ['Mistakes logged', t.mistakes],
          ['First-try, no hint', n(fa.total) ? `${Math.round((n(fa.first_try_no_hint) / n(fa.total)) * 100)}%` : '—']].map(([l, v]) => (
          <div key={String(l)} className="card"><p className="text-xs text-muted">{String(l)}</p><p className="text-2xl font-bold font-display">{String(v)}</p></div>
        ))}
      </div>

      <Section title="Study heatmap (last 12 months)"><Heatmap rows={data.heatmap} today={data.today} /></Section>
      <Section title="XP per day (90 days)"><Bars rows={data.xpByDay} x="day" y="xp" /></Section>
      <div className="grid md:grid-cols-2 gap-4">
        <Section title="Focus time by topic"><HBars rows={data.hoursByTopic} label="topic" value="minutes" unit="min" /></Section>
        <Section title="Problems by pattern"><HBars rows={data.problemsByPattern} label="pattern" value="count" /></Section>
        <Section title="Problems by difficulty"><HBars rows={data.problemsByDifficulty} label="difficulty" value="count" /></Section>
        <Section title="Hint dependence by week (lower is better)">
          <HBars rows={data.hintTrend.map((r) => ({ week: r.week, pct: n(r.total) ? Math.round((n(r.with_hint) / n(r.total)) * 100) : 0 }))} label="week" value="pct" unit="%" />
        </Section>
        <Section title="Recall accuracy by topic">
          <HBars rows={data.recallByTopic.map((r) => ({ topic: r.topic, pct: n(r.total) ? Math.round((n(r.remembered) / n(r.total)) * 100) : 0 }))} label="topic" value="pct" unit="%" />
        </Section>
        <Section title="Quiz average by section"><HBars rows={data.quizBySection.map((r) => ({ s: `${r.world === 'DSA' ? 'DSA' : 'Java ' + String(r.section)}`, v: n(r.avg_percent) }))} label="s" value="v" unit="%" /></Section>
        <Section title="Speaking trend (weekly score)"><Bars rows={data.speakingTrend} x="week" y="score" /></Section>
        <Section title="Mock test scores"><Bars rows={data.mockScores} x="at" y="score" /></Section>
        <Section title="Readiness over time"><Bars rows={data.readinessHistory} x="day" y="score" /></Section>
        <Section title="Mistakes by type"><HBars rows={data.mistakesByCategory} label="category" value="count" /></Section>
      </div>
      <Section title="Reports and export"><ExportBox /></Section>
    </div>
  )
}

function Heatmap({ rows, today }: { rows: Row[]; today: string }) {
  const map = new Map(rows.map((r) => [String(r.day), r]))
  const end = new Date(today + 'T00:00:00')
  const start = new Date(end); start.setDate(start.getDate() - 364 - start.getDay())
  const cells: { d: string; v: number; frozen: boolean }[] = []
  for (const d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
    const key = d.toISOString().slice(0, 10)
    const r = map.get(key)
    cells.push({ d: key, v: r ? n(r.xp) + n(r.minutes) : 0, frozen: !!r?.frozen })
  }
  const weeks = Math.ceil(cells.length / 7)
  const tone = (c: { v: number; frozen: boolean }) => c.frozen ? 'fill-accent/40' : c.v === 0 ? 'fill-raised' : c.v < 30 ? 'fill-primary/30' : c.v < 80 ? 'fill-primary/60' : 'fill-primary'
  return (
    <div className="card overflow-x-auto">
      <svg width={weeks * 13} height={7 * 13} role="img" aria-label={`${rows.length} active days in the last year`}>
        {cells.map((c, i) => (
          <rect key={c.d} x={Math.floor(i / 7) * 13} y={(i % 7) * 13} width={11} height={11} rx={2} className={tone(c)}>
            <title>{c.d}: {c.frozen ? 'covered by a streak freeze' : c.v ? `${c.v} activity points` : 'rest day'}</title>
          </rect>
        ))}
      </svg>
      <p className="text-xs text-muted mt-2">Darker = more XP and focus. Teal = a day covered by a streak freeze.</p>
    </div>
  )
}

function Bars({ rows, x, y }: { rows: Row[]; x: string; y: string }) {
  if (!rows.length) return <p className="card text-sm text-muted">No data yet.</p>
  const max = Math.max(1, ...rows.map((r) => n(r[y])))
  const w = Math.max(4, Math.min(24, Math.floor(600 / rows.length)))
  return (
    <div className="card overflow-x-auto">
      <svg width={rows.length * (w + 2)} height={110} role="img" aria-label={`${rows.length} values, max ${max}`}>
        {rows.map((r, i) => {
          const h = Math.max(2, (n(r[y]) / max) * 100)
          return <rect key={i} x={i * (w + 2)} y={105 - h} width={w} height={h} rx={2} className="fill-primary"><title>{String(r[x]).slice(0, 10)}: {n(r[y])}</title></rect>
        })}
      </svg>
    </div>
  )
}

function HBars({ rows, label, value, unit = '' }: { rows: Row[]; label: string; value: string; unit?: string }) {
  if (!rows.length) return <p className="card text-sm text-muted">No data yet.</p>
  const max = Math.max(1, ...rows.map((r) => n(r[value])))
  return (
    <div className="card space-y-2">
      {rows.map((r, i) => (
        <div key={i} className="text-sm">
          <div className="flex justify-between gap-2"><span className="truncate">{String(r[label]).slice(0, 40)}</span><span className="text-muted">{n(r[value])}{unit}</span></div>
          <div className="h-2 rounded bg-raised"><div className="h-full rounded bg-accent" style={{ width: `${(n(r[value]) / max) * 100}%` }} /></div>
        </div>
      ))}
    </div>
  )
}

function ExportBox() {
  const reports = useApi<Row[]>('/api/reports')
  async function generate(kind: 'WEEKLY' | 'MONTHLY', useAi: boolean) {
    await api(`/api/reports?kind=${kind}&useAi=${useAi}`, { method: 'POST' }).catch(() => undefined)
    void reports.reload()
  }
  const download = (path: string, name: string) => downloadFile(path, name).catch((e) => alert(e.message))
  return (
    <div className="card space-y-3">
      <div className="flex flex-wrap gap-2">
        <button className="btn-ghost text-sm" onClick={() => generate('WEEKLY', false)}>📄 Weekly report</button>
        <button className="btn-ghost text-sm" onClick={() => generate('WEEKLY', true)}>🧙 Weekly report + AI summary</button>
        <button className="btn-ghost text-sm" onClick={() => generate('MONTHLY', false)}>📄 Monthly report</button>
        <button className="btn-ghost text-sm" onClick={() => download('/api/export/json', 'code-quest-export.json')}>⬇️ Export all (JSON)</button>
        <button className="btn-ghost text-sm" onClick={() => download('/api/export/problems.csv', 'problems.csv')}>⬇️ Problems (CSV)</button>
      </div>
      <ul className="space-y-2">
        {reports.data?.map((r, i) => (
          <li key={i} className="rounded-xl bg-raised p-3 text-sm">
            <p className="font-semibold">{String(r.kind)} · {String(r.period_start)} → {String(r.period_end)}</p>
            <p className="whitespace-pre-line mt-1">{String(r.ai_summary ?? '')}</p>
          </li>
        ))}
      </ul>
    </div>
  )
}
