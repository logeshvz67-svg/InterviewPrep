import { useEffect, useState } from 'react'
import { api } from '../api/client'
import type { CompanyView, CurriculumView, ProfileView, TopicView } from '../api/types'
import { ErrorBox, Spinner } from '../components/ui'
import { errorMessage, useApi } from '../lib/hooks'
import { topicStatusLabels } from '../theme/gameConfig'

const MANUAL_STATUSES = ['NOT_STARTED', 'LEARNING', 'UNDERSTOOD', 'CAN_EXPLAIN'] as const

export default function CurriculumPage() {
  const profile = useApi<ProfileView>('/api/profile')
  const companies = useApi<CompanyView[]>('/api/companies')
  const [company, setCompany] = useState<string | null>(null)
  useEffect(() => { if (profile.data && company === null) setCompany(profile.data.primaryCompany) }, [profile.data, company])
  const cur = useApi<CurriculumView>(company === null ? null : `/api/curriculum/java${company ? `?company=${company}` : ''}`)
  const [openSection, setOpenSection] = useState<string | null>('A')
  const [msg, setMsg] = useState<string | null>(null)

  async function setStatus(t: TopicView, status: string) {
    setMsg(null)
    try {
      const updated = await api<TopicView>(`/api/curriculum/java/${t.id}/status`, { method: 'PUT', body: { status } })
      cur.setData((d) => d && {
        ...d,
        sections: d.sections.map((s) => ({ ...s, topics: s.topics.map((x) => (x.id === t.id ? updated : x)) })),
      })
    } catch (e) { setMsg(errorMessage(e)) }
  }

  if ((cur.loading && !cur.data) || company === null) return <Spinner />
  if (cur.error || !cur.data) return <ErrorBox message={cur.error ?? 'No data'} onRetry={cur.reload} />
  const pct = cur.data.totalTopics ? Math.round((cur.data.understoodOrBetter / cur.data.totalTopics) * 100) : 0

  return (
    <div className="space-y-5">
      <header className="space-y-2">
        <h1 className="text-2xl font-bold">🏝️ Java Core Island</h1>
        <p className="text-muted text-sm">The full Java course from zero. Even topics you "know" get a proper Rebuild lesson, because shallow knowledge breaks in interviews. Full lessons, quizzes and visualizers arrive in Phase 2. For now, track your status as you study.</p>
        <div className="flex items-center gap-3">
          <div className="flex-1 h-2 rounded bg-raised"><div className="h-full rounded bg-accent" style={{ width: `${pct}%` }} /></div>
          <span className="text-sm text-muted">{cur.data.understoodOrBetter}/{cur.data.totalTopics} understood</span>
        </div>
        <label className="text-sm flex items-center gap-2">Show topics for
          <select className="input max-w-[220px] py-1.5" value={company} onChange={(e) => setCompany(e.target.value)}>
            <option value="">All companies</option>
            {companies.data?.filter((c) => c.code !== 'ALL').map((c) => <option key={c.code} value={c.code}>{c.name}</option>)}
          </select>
        </label>
      </header>
      {msg && <div className="card border-warn/50 text-warn text-sm" role="alert">{msg}</div>}

      {cur.data.sections.map((s) => (
        <section key={s.section} className="card p-0 overflow-hidden">
          <button className="w-full flex justify-between items-center p-4" onClick={() => setOpenSection(openSection === s.section ? null : s.section)} aria-expanded={openSection === s.section}>
            <span className="font-semibold">{s.section}. {s.name}</span>
            <span className="text-sm text-muted">{s.understoodOrBetter}/{s.total} {openSection === s.section ? '▴' : '▾'}</span>
          </button>
          {openSection === s.section && (
            <ul className="divide-y divide-line/50 border-t border-line/50">
              {s.topics.map((t) => (
                <li key={t.id} className="p-4 space-y-2">
                  <div className="flex flex-wrap items-start justify-between gap-2">
                    <div>
                      <p className="font-medium">{t.title} {t.rebuild && <span className="chip ml-1">Rebuild</span>}</p>
                      <p className="text-sm text-muted">{t.summary}</p>
                    </div>
                    <span className={`text-sm font-semibold ${topicStatusLabels[t.status]?.tone}`}>{topicStatusLabels[t.status]?.label}</span>
                  </div>
                  <div className="text-xs text-muted flex flex-wrap gap-1 items-center">
                    <span>🎯 {t.infosysRelevance}</span>
                    {t.companies.map((c) => <span key={c} className="chip">{c.replace('_', ' ')}</span>)}
                  </div>
                  {t.status !== 'MASTERED' && (
                    <div className="flex flex-wrap gap-1" role="radiogroup" aria-label={`Status for ${t.title}`}>
                      {MANUAL_STATUSES.map((st) => (
                        <button key={st} role="radio" aria-checked={t.status === st} onClick={() => setStatus(t, st)}
                          className={`chip px-3 py-1 ${t.status === st ? 'chip-on' : ''}`}>{topicStatusLabels[st].label}</button>
                      ))}
                      <span className="chip px-3 py-1 opacity-60" title="Earned by scoring 90%+ on a Prove-it test (Phase 2)">🔒 Mastered</span>
                    </div>
                  )}
                </li>
              ))}
            </ul>
          )}
        </section>
      ))}
    </div>
  )
}
