import { useEffect, useState } from 'react'
import type { CompanyView, ProfileView } from '../../api/types'
import { ErrorBox, Spinner } from '../../components/ui'
import { useApi } from '../../lib/hooks'
import type { ReadinessComponent, ReadinessView } from './types'

const COMPONENT_LABEL: Record<ReadinessComponent, { label: string; icon: string }> = {
  JAVA: { label: 'Java', icon: '☕' },
  DSA: { label: 'DSA (coding)', icon: '🧩' },
  SQL: { label: 'SQL', icon: '🗄️' },
  APTITUDE: { label: 'Aptitude', icon: '🎯' },
  MOCKS: { label: 'Mock tests', icon: '⏱️' },
  COMMUNICATION: { label: 'Speaking', icon: '🗣️' },
}

export default function ReadinessTab() {
  const profile = useApi<ProfileView>('/api/profile')
  const companies = useApi<CompanyView[]>('/api/companies')
  const [company, setCompany] = useState<string>('')
  useEffect(() => {
    if (!company && profile.data) setCompany(profile.data.primaryCompany)
  }, [company, profile.data])
  const r = useApi<ReadinessView>(company ? `/api/readiness?company=${encodeURIComponent(company)}` : null)

  if (profile.error) return <ErrorBox message={profile.error} onRetry={profile.reload} />
  const name = (code: string) => companies.data?.find((c) => c.code === code)?.name ?? code

  return (
    <div className="space-y-4">
      <div className="card space-y-2">
        <label className="label" htmlFor="ready-company">Show readiness for</label>
        <select id="ready-company" className="input" value={company} onChange={(e) => setCompany(e.target.value)}>
          {!company && <option value="">Loading…</option>}
          {(companies.data ?? (company ? [{ code: company, name: company }] : [])).map((c) => (
            <option key={c.code} value={c.code}>{c.name}{profile.data?.primaryCompany === c.code ? ' (my target)' : ''}</option>
          ))}
        </select>
      </div>

      {r.loading && !r.data && <Spinner />}
      {r.error && <ErrorBox message={r.error} onRetry={r.reload} />}
      {r.data && (
        <>
          <div className="card flex items-center gap-5">
            <ScoreRing score={Number(r.data.score)} />
            <div className="min-w-0">
              <h2 className="text-lg font-semibold">{name(r.data.companyCode)} readiness</h2>
              <p className="text-sm text-muted">
                Based on {Number(r.data.trackedWeightPercent)}% of the areas we can measure so far.
                Areas not tracked yet count as 0.
              </p>
            </div>
          </div>

          <div className="card space-y-3">
            <h3 className="font-semibold">What makes up the score</h3>
            <ul className="space-y-3">
              {r.data.components.map((c) => {
                const meta = COMPONENT_LABEL[c.component] ?? { label: c.component, icon: '•' }
                const value = Math.round(Number(c.value) * 100)
                const weight = Math.round(Number(c.weight) * 100)
                return (
                  <li key={c.component}>
                    <div className="flex justify-between text-sm">
                      <span>{meta.icon} {meta.label} <span className="text-muted">· weight {weight}%</span></span>
                      {c.tracked ? <span className="font-semibold">{value}%</span> : <span className="chip">not tracked yet</span>}
                    </div>
                    <div className="mt-1 h-2.5 rounded-full bg-raised overflow-hidden" role="progressbar"
                      aria-label={`${meta.label}`} aria-valuemin={0} aria-valuemax={100} aria-valuenow={c.tracked ? value : 0}
                      aria-valuetext={c.tracked ? `${value}%` : 'not tracked yet'}>
                      <div className={`h-full transition-all duration-700 ${c.tracked ? (value >= 70 ? 'bg-good' : value >= 40 ? 'bg-warn' : 'bg-bad') : 'bg-line'}`}
                        style={{ width: `${c.tracked ? value : 0}%` }} />
                    </div>
                  </li>
                )
              })}
            </ul>
            <p className="text-xs text-muted">
              ⚖️ The weights are our prep estimate, not an official company formula.
            </p>
          </div>

          <div className="card border-accent/50">
            <h3 className="font-semibold">👉 Improve next: {COMPONENT_LABEL[r.data.improveNext]?.label ?? r.data.improveNext}</h3>
            <p className="text-sm mt-1">{r.data.improveNextTip}</p>
          </div>
        </>
      )}
    </div>
  )
}

function ScoreRing({ score }: { score: number }) {
  const s = Math.max(0, Math.min(100, score))
  const C = 2 * Math.PI * 34
  const tone = s >= 70 ? 'text-good' : s >= 40 ? 'text-warn' : 'text-bad'
  return (
    <div className="relative h-24 w-24 shrink-0" role="img" aria-label={`Readiness score ${s} out of 100`}>
      <svg viewBox="0 0 80 80" className="h-full w-full -rotate-90">
        <circle cx="40" cy="40" r="34" fill="none" strokeWidth="8" className="stroke-raised" />
        <circle cx="40" cy="40" r="34" fill="none" strokeWidth="8" strokeLinecap="round"
          className={`stroke-current ${tone} transition-all duration-700`} strokeDasharray={C} strokeDashoffset={C * (1 - s / 100)} />
      </svg>
      <div className="absolute inset-0 flex items-center justify-center text-xl font-bold font-display">{s.toFixed(0)}</div>
    </div>
  )
}
