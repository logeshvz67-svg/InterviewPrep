import { useState } from 'react'
import { api } from '../api/client'
import type { ProfileView } from '../api/types'
import { Stars } from '../components/ui'
import { errorMessage } from '../lib/hooks'
import { avatars, game } from '../theme/gameConfig'

const LATER = [
  { code: 'TCS', name: 'TCS' }, { code: 'WIPRO', name: 'Wipro' }, { code: 'COGNIZANT', name: 'Cognizant' },
  { code: 'ACCENTURE', name: 'Accenture' }, { code: 'CAPGEMINI', name: 'Capgemini' }, { code: 'PRODUCT', name: 'Product companies' },
]
const AREAS = [
  { key: 'JAVA', label: 'Java' }, { key: 'DSA', label: 'DSA (data structures & algorithms)' },
  { key: 'SQL', label: 'SQL' }, { key: 'SPRING', label: 'Spring Boot' }, { key: 'COMMUNICATION', label: 'Spoken communication' },
]
const STEPS = ['Character', 'Target', 'Schedule', 'Self-check', 'Your why'] as const

export default function OnboardingPage({ onDone }: { onDone: () => void }) {
  const browserTz = Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Kolkata'
  const [step, setStep] = useState(0)
  const [characterName, setCharacterName] = useState('')
  const [avatar, setAvatar] = useState('knight')
  const [primaryCompany, setPrimary] = useState<'INFOSYS_SP' | 'INFOSYS_DSE'>('INFOSYS_SP')
  const [later, setLater] = useState<string[]>(['TCS', 'WIPRO', 'COGNIZANT', 'ACCENTURE', 'CAPGEMINI'])
  const [testDate, setTestDate] = useState('')
  const [timezone, setTimezone] = useState(browserTz)
  const [dailyMinutes, setDailyMinutes] = useState(60)
  const [self, setSelf] = useState<Record<string, number>>({ JAVA: 2, DSA: 2, SQL: 2, SPRING: 1, COMMUNICATION: 2 })
  const [why, setWhy] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const canNext = step !== 0 || characterName.trim().length > 0

  async function finish() {
    setBusy(true)
    setError(null)
    try {
      await api<ProfileView>('/api/onboarding', {
        method: 'PUT',
        body: { characterName, avatar, primaryCompany, laterCompanies: later, testDate: testDate || null, timezone, dailyMinutes, whyStatement: why || null, selfAssessment: self },
      })
      onDone()
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="min-h-screen max-w-lg mx-auto p-4 space-y-5">
      <header>
        <p className="text-sm text-muted">Step {step + 1} of {STEPS.length}: {STEPS[step]}</p>
        <div className="flex gap-1 mt-2">{STEPS.map((s, i) => <div key={s} className={`h-1.5 flex-1 rounded ${i <= step ? 'bg-primary' : 'bg-raised'}`} />)}</div>
      </header>

      {step === 0 && (
        <section className="card space-y-4">
          <h1 className="text-2xl font-bold">Create your character</h1>
          <p className="text-muted">You're a developer-in-training travelling to the {game.finalDestination}. Bugs are monsters, concepts are spells, interviews are boss fights.</p>
          <div>
            <label className="label" htmlFor="cn">Character name</label>
            <input id="cn" className="input" value={characterName} onChange={(e) => setCharacterName(e.target.value)} maxLength={60} />
          </div>
          <div className="grid grid-cols-3 gap-2" role="radiogroup" aria-label="Avatar">
            {avatars.map((a) => (
              <button key={a.id} role="radio" aria-checked={avatar === a.id} onClick={() => setAvatar(a.id)}
                className={`rounded-xl p-3 text-center ${avatar === a.id ? 'bg-primary/20 ring-2 ring-primary' : 'bg-raised'}`}>
                <div className="text-3xl">{a.icon}</div><div className="text-xs mt-1">{a.label}</div>
              </button>
            ))}
          </div>
        </section>
      )}

      {step === 1 && (
        <section className="card space-y-4">
          <h1 className="text-2xl font-bold">Your primary target</h1>
          <p className="text-muted">Everything is tuned for Infosys first. Other companies go in a "Later" lane.</p>
          <div className="grid gap-2">
            {([['INFOSYS_SP', 'Infosys Specialist Programmer (SP)', 'Heavier DSA, timed multi-problem coding, optimization and depth.'],
              ['INFOSYS_DSE', 'Infosys Digital Specialist Engineer (DSE)', 'Java/OOP/SQL/DSA fundamentals, MCQs plus 1-2 coding problems, project and HR rounds.']] as const).map(([code, title, desc]) => (
              <button key={code} onClick={() => setPrimary(code)} className={`text-left rounded-xl p-3 ${primaryCompany === code ? 'bg-primary/20 ring-2 ring-primary' : 'bg-raised'}`}>
                <div className="font-semibold">{title}</div><div className="text-sm text-muted">{desc}</div>
              </button>
            ))}
          </div>
          <p className="text-xs text-muted">Not sure? Pick SP. It covers everything DSE needs, plus more DSA. You can switch anytime in Settings.</p>
          <div>
            <p className="label">Later lane (optional)</p>
            <div className="flex flex-wrap gap-2">
              {LATER.map((c) => (
                <button key={c.code} onClick={() => setLater((l) => l.includes(c.code) ? l.filter((x) => x !== c.code) : [...l, c.code])}
                  className={`chip px-3 py-1.5 ${later.includes(c.code) ? 'chip-on' : ''}`}>{c.name}</button>
              ))}
            </div>
          </div>
        </section>
      )}

      {step === 2 && (
        <section className="card space-y-4">
          <h1 className="text-2xl font-bold">Your schedule</h1>
          <div>
            <label className="label" htmlFor="td">Test or interview date (optional)</label>
            <input id="td" type="date" className="input" value={testDate} onChange={(e) => setTestDate(e.target.value)} />
            <p className="text-xs text-muted mt-1">If you add one, the roadmap squeezes or stretches to fit it.</p>
          </div>
          <div>
            <label className="label" htmlFor="dm">Time you can give each day: {dailyMinutes} min</label>
            <input id="dm" type="range" min={20} max={240} step={10} value={dailyMinutes} onChange={(e) => setDailyMinutes(+e.target.value)} className="w-full accent-[rgb(var(--c-primary))]" />
            <p className="text-xs text-muted">Busy day? The 20-minute Minimum Viable Day still keeps your streak.</p>
          </div>
          <div>
            <label className="label" htmlFor="tz">Timezone (your streak day ends at YOUR midnight)</label>
            <input id="tz" className="input" value={timezone} onChange={(e) => setTimezone(e.target.value)} />
          </div>
        </section>
      )}

      {step === 3 && (
        <section className="card space-y-4">
          <h1 className="text-2xl font-bold">Quick self-check</h1>
          <p className="text-muted text-sm">1 = "never heard of it", 5 = "could teach it". This is <b>just for you to compare later</b>. It never skips any lessons: you'll learn everything properly.</p>
          {AREAS.map((a) => (
            <div key={a.key}>
              <p className="label">{a.label}</p>
              <Stars label={a.label} value={self[a.key]} onChange={(v) => setSelf({ ...self, [a.key]: v })} />
            </div>
          ))}
        </section>
      )}

      {step === 4 && (
        <section className="card space-y-4">
          <h1 className="text-2xl font-bold">Why are you doing this?</h1>
          <p className="text-muted text-sm">We'll show this on hard days. Write it the way you'd say it to a friend.</p>
          <textarea className="input min-h-32" maxLength={1000} value={why} onChange={(e) => setWhy(e.target.value)}
            placeholder="e.g. I want a real developer job so I can stop copy-pasting all day and build things." />
          {error && <p className="text-bad text-sm whitespace-pre-line" role="alert">{error}</p>}
        </section>
      )}

      <div className="flex justify-between">
        <button className="btn-ghost" disabled={step === 0} onClick={() => setStep(step - 1)}>Back</button>
        {step < STEPS.length - 1
          ? <button className="btn-primary" disabled={!canNext} onClick={() => setStep(step + 1)}>Next</button>
          : <button className="btn-gold" disabled={busy} onClick={finish}>{busy ? 'Building your roadmap…' : 'Start my quest 🚀'}</button>}
      </div>
    </main>
  )
}
