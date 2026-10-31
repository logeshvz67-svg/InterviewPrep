import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import type { WorldDetail } from '../../api/learning'
import type { CompanyView, ProfileView } from '../../api/types'
import { ErrorBox, Spinner } from '../../components/ui'
import { useApi } from '../../lib/hooks'

const STATUS_ICON: Record<string, string> = { LOCKED: '🔒', AVAILABLE: '⭐', IN_PROGRESS: '📖', COMPLETED: '✅', OUTLINE: '🗒️' }

export default function WorldPage() {
  const { code = 'JAVA_CORE' } = useParams()
  const profile = useApi<ProfileView>('/api/profile')
  const companies = useApi<CompanyView[]>('/api/companies')
  const [company, setCompany] = useState<string | null>(null)
  useEffect(() => { if (profile.data && company === null) setCompany(profile.data.primaryCompany) }, [profile.data, company])
  const world = useApi<WorldDetail>(company === null ? null : `/api/worlds/${code}?company=${company}`)
  const [open, setOpen] = useState<string | null>(null)

  if (world.loading && !world.data) return <Spinner />
  if (world.error || !world.data) return <ErrorBox message={world.error ?? 'No data'} onRetry={world.reload} />
  const w = world.data
  const firstOpen = open ?? w.sections.find((s) => s.completed < s.total)?.section ?? w.sections[0]?.section

  return (
    <div className="space-y-4">
      <header className="space-y-1">
        <Link to="/learn" className="text-sm text-primary">← All worlds</Link>
        <h1 className="text-2xl font-bold">{w.name}</h1>
        <p className="text-sm text-muted">{w.description}</p>
        {code === 'JAVA_CORE' && <p className="text-sm">Every topic gets a full lesson, even ones you "know": shallow knowledge breaks in interviews. Already confident? Take the <b>Prove it</b> test (90%+) on any lesson.</p>}
        <label className="text-sm flex items-center gap-2">Highlight lessons for
          <select className="input max-w-[220px] py-1.5" value={company ?? ''} onChange={(e) => setCompany(e.target.value)}>
            {companies.data?.filter((c) => c.code !== 'ALL').map((c) => <option key={c.code} value={c.code}>{c.name}</option>)}
          </select>
        </label>
      </header>
      {w.sections.map((s) => (
        <section key={s.section} className="card p-0 overflow-hidden">
          <button className="w-full flex justify-between items-center p-4 text-left" onClick={() => setOpen(firstOpen === s.section ? '' : s.section)} aria-expanded={firstOpen === s.section}>
            <span className="font-semibold">{s.section === 'OUTLINE' ? '🗒️ ' : `${s.section}. `}{s.name}</span>
            <span className="text-sm text-muted">{s.section === 'OUTLINE' ? `${s.total} planned` : `${s.completed}/${s.total}`} {firstOpen === s.section ? '▴' : '▾'}</span>
          </button>
          {firstOpen === s.section && (
            <ol className="border-t border-line/50 divide-y divide-line/40">
              {s.lessons.map((l) => {
                const inner = (
                  <div className={`flex gap-3 p-4 items-start ${l.relevant ? '' : 'opacity-60'}`}>
                    <span className="text-xl" aria-hidden>{l.mastered ? '🏅' : STATUS_ICON[l.status]}</span>
                    <div className="flex-1 min-w-0">
                      <p className="font-medium">{l.title}</p>
                      <p className="text-xs text-muted line-clamp-2">{l.summary}</p>
                      <p className="text-xs text-muted mt-1">
                        {l.estimatedMinutes ? `${l.estimatedMinutes} min` : ''}{l.bestQuizPercent > 0 ? ` · best quiz ${Math.round(l.bestQuizPercent)}%` : ''}
                        {!l.relevant && ' · mainly for other companies'}{l.mastered && ' · Mastered'}
                      </p>
                    </div>
                  </div>
                )
                return (
                  <li key={l.slug ?? l.title}>
                    {l.slug ? <Link to={`/lesson/${l.slug}`} className="block hover:bg-raised">{inner}</Link> : inner}
                  </li>
                )
              })}
            </ol>
          )}
        </section>
      ))}
    </div>
  )
}
