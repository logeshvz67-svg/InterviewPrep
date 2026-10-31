import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, clearTokens, downloadFile } from '../api/client'
import type { CompanyView, Page, ProfileView, XpEventView } from '../api/types'
import { useAuth } from '../auth/AuthContext'
import { Section, Spinner } from '../components/ui'
import { errorMessage, useApi, useLocalSetting } from '../lib/hooks'
import { applyTheme, avatars, themes, type ThemeName } from '../theme/gameConfig'

export default function SettingsPage({ onProfileChanged }: { onProfileChanged: () => void }) {
  const { user, logout } = useAuth()
  const profile = useApi<ProfileView>('/api/profile')
  const companies = useApi<CompanyView[]>('/api/companies')
  const xp = useApi<Page<XpEventView>>('/api/xp/events?size=15')
  const [theme, setTheme] = useLocalSetting<ThemeName>('cq.theme', 'quest-dark')
  const [form, setForm] = useState<ProfileView | null>(null)
  const [testDate, setTestDate] = useState('')
  const [msg, setMsg] = useState<string | null>(null)

  useEffect(() => { if (profile.data) { setForm(profile.data); setTestDate(profile.data.testDate ?? '') } }, [profile.data])
  if (!form) return <Spinner />

  const flash = (m: string) => { setMsg(m); setTimeout(() => setMsg(null), 2500) }

  async function saveProfile() {
    try {
      await api('/api/profile', { method: 'PUT', body: { characterName: form!.characterName, avatar: form!.avatar, timezone: form!.timezone, dailyMinutes: form!.dailyMinutes, whyStatement: form!.whyStatement, soundEnabled: form!.soundEnabled } })
      flash('Saved ✓'); onProfileChanged()
    } catch (e) { flash(errorMessage(e)) }
  }

  async function switchCompany(code: string) {
    try {
      const p = await api<ProfileView>('/api/profile/company', { method: 'PUT', body: { companyCode: code } })
      setForm(p); flash(`Target switched to ${code.replace('_', ' ')}. Readiness and filters now follow it.`)
    } catch (e) { flash(errorMessage(e)) }
  }

  async function saveTestDate() {
    try {
      await api('/api/profile/test-date', { method: 'PUT', body: { testDate: testDate || null } })
      flash('Test date saved, roadmap regenerated ✓')
    } catch (e) { flash(errorMessage(e)) }
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">⚙️ Settings</h1>
      {msg && <div className="card border-accent/50 text-sm" role="status">{msg}</div>}

      <Section title="Profile">
        <div className="card space-y-3">
          <div>
            <label className="label" htmlFor="sn">Character name</label>
            <input id="sn" className="input" value={form.characterName} maxLength={60} onChange={(e) => setForm({ ...form, characterName: e.target.value })} />
          </div>
          <div className="flex flex-wrap gap-2">
            {avatars.map((a) => (
              <button key={a.id} onClick={() => setForm({ ...form, avatar: a.id })} aria-label={a.label}
                className={`text-2xl rounded-xl p-2 ${form.avatar === a.id ? 'bg-primary/20 ring-2 ring-primary' : 'bg-raised'}`}>{a.icon}</button>
            ))}
          </div>
          <div className="grid sm:grid-cols-2 gap-3">
            <div>
              <label className="label" htmlFor="stz">Timezone</label>
              <input id="stz" className="input" value={form.timezone} onChange={(e) => setForm({ ...form, timezone: e.target.value })} />
            </div>
            <div>
              <label className="label" htmlFor="sdm">Daily time goal (minutes)</label>
              <input id="sdm" type="number" min={10} max={600} className="input" value={form.dailyMinutes} onChange={(e) => setForm({ ...form, dailyMinutes: +e.target.value })} />
            </div>
          </div>
          <div>
            <label className="label" htmlFor="sw">Why I'm doing this</label>
            <textarea id="sw" className="input" maxLength={1000} value={form.whyStatement ?? ''} onChange={(e) => setForm({ ...form, whyStatement: e.target.value })} />
          </div>
          <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={form.soundEnabled} onChange={(e) => setForm({ ...form, soundEnabled: e.target.checked })} /> Sounds on</label>
          <button className="btn-primary" onClick={saveProfile}>Save profile</button>
        </div>
      </Section>

      <Section title="Target company">
        <div className="card space-y-3">
          <p className="text-sm">Current: <b>{form.primaryCompany.replace('_', ' ')}</b>. Later lane: {form.laterCompanies.join(', ') || 'none'}</p>
          <div className="flex flex-wrap gap-2">
            {companies.data?.filter((c) => c.code !== 'ALL').map((c) => (
              <button key={c.code} className={`chip px-3 py-1.5 ${form.primaryCompany === c.code ? 'chip-on' : ''}`} onClick={() => switchCompany(c.code)}>{c.name}</button>
            ))}
          </div>
          <div className="grid sm:grid-cols-[1fr_auto] gap-2 items-end">
            <div>
              <label className="label" htmlFor="std">Test / interview date</label>
              <input id="std" type="date" className="input" value={testDate} onChange={(e) => setTestDate(e.target.value)} />
            </div>
            <button className="btn-ghost" onClick={saveTestDate}>Save & regenerate roadmap</button>
          </div>
          <p className="text-xs text-muted">Exam pattern editor (enter the current official Infosys pattern) arrives in Phase 4. We never hardcode or guess it.</p>
        </div>
      </Section>

      <Section title="Voice, reminders and goals">
        <VoiceAndReminders profile={form} onSaved={(p) => { setForm(p); flash('Saved ✓') }} />
      </Section>

      <Section title="Exam pattern">
        <div className="card text-sm space-y-2">
          <p>Enter the current Infosys test pattern (sections, questions, time, cutoffs) after checking the <b>official Infosys careers page</b>. Timed mock tests use exactly what you enter. Code Quest never guesses or hardcodes it.</p>
          <Link className="btn-ghost text-sm" to="/interview?tab=mocks">Open the exam pattern editor →</Link>
        </div>
      </Section>

      <Section title="Theme">
        <div className="card flex flex-wrap gap-2">
          {(Object.keys(themes) as ThemeName[]).map((t) => (
            <button key={t} className={`chip px-3 py-1.5 ${theme === t ? 'chip-on' : ''}`} onClick={() => { setTheme(t); applyTheme(t) }}>{themes[t].label}</button>
          ))}
        </div>
      </Section>

      <Section title="Recent XP (server audit trail)">
        <ul className="card text-sm divide-y divide-line/50 p-0">
          {xp.data?.items.map((e, i) => (
            <li key={i} className="flex justify-between px-4 py-2"><span>{e.action.toLowerCase().replaceAll('_', ' ')}{e.note ? ` · ${e.note}` : ''}</span><span className="text-gold">+{e.amount}</span></li>
          ))}
          {xp.data?.items.length === 0 && <li className="px-4 py-2 text-muted">No XP yet.</li>}
        </ul>
      </Section>

      <Section title="Account">
        <div className="card space-y-2 text-sm">
          <p>Signed in as {user?.email} {user?.role === 'ADMIN' && <span className="chip ml-1">admin</span>}</p>
          <button className="btn-ghost" onClick={() => void logout()}>Log out</button>
          <div className="flex flex-wrap gap-2 pt-2">
            <button className="btn-ghost text-sm" onClick={() => downloadFile('/api/export/json', 'code-quest-export.json').catch((e) => flash(e.message))}>⬇️ Export all my data (JSON)</button>
            <button className="btn-ghost text-sm" onClick={() => downloadFile('/api/export/problems.csv', 'problems.csv').catch((e) => flash(e.message))}>⬇️ Problems (CSV)</button>
          </div>
          <DeleteAccount />
          <p className="text-xs text-muted">AI: answers come from a free OpenRouter model run by the server; free models may log prompts, so never paste passwords or private data. The AI can be wrong; lessons are the source of truth. Speech: recognition runs in your browser (Chrome/Edge work best and may send audio to the browser vendor's service); Code Quest stores only transcripts and scores, never audio.</p>
          <p className="text-xs text-muted">Code Quest is a personal study tool. Practice questions are written "in the style of" companies and are never claimed to be actual test questions.</p>
        </div>
      </Section>
    </div>
  )
}

function VoiceAndReminders({ profile, onSaved }: { profile: ProfileView; onSaved: (p: ProfileView) => void }) {
  const st = profile.settings ?? {}
  const [voiceLang, setVoiceLang] = useState(String(st.voiceLang ?? 'en-IN'))
  const [voiceRate, setVoiceRate] = useState(Number(st.voiceRate ?? 1))
  const [reminderTime, setReminderTime] = useState(String(st.reminderTime ?? ''))
  const [notificationsOn, setNotificationsOn] = useState(Boolean(st.notificationsOn))
  const [readyThreshold, setReadyThreshold] = useState(Number(st.readyThreshold ?? 75))
  const [err, setErr] = useState<string | null>(null)

  async function save() {
    setErr(null)
    if (notificationsOn && 'Notification' in window && Notification.permission === 'default') await Notification.requestPermission()
    try {
      onSaved(await api<ProfileView>('/api/profile/settings', { method: 'PUT', body: { voiceLang, voiceRate, reminderTime: reminderTime || null, notificationsOn, readyThreshold } }))
    } catch (e) { setErr(errorMessage(e)) }
  }

  function preview() {
    if (!('speechSynthesis' in window)) return
    const u = new SpeechSynthesisUtterance('Hello! Explain polymorphism to me in simple words.')
    u.lang = voiceLang; u.rate = voiceRate
    window.speechSynthesis.cancel(); window.speechSynthesis.speak(u)
  }

  return (
    <div className="card space-y-3">
      <div className="grid sm:grid-cols-2 gap-3">
        <div>
          <label className="label" htmlFor="vl">Voice language / accent</label>
          <select id="vl" className="input" value={voiceLang} onChange={(e) => setVoiceLang(e.target.value)}>
            <option value="en-IN">English (India)</option><option value="en-US">English (US)</option><option value="en-GB">English (UK)</option>
          </select>
        </div>
        <div>
          <label className="label" htmlFor="vr">Speaking speed: {voiceRate.toFixed(1)}x</label>
          <input id="vr" type="range" min={0.5} max={1.5} step={0.1} value={voiceRate} onChange={(e) => setVoiceRate(+e.target.value)} className="w-full" />
        </div>
      </div>
      <button className="btn-ghost text-sm" onClick={preview}>🔊 Preview voice</button>
      <div className="grid sm:grid-cols-2 gap-3">
        <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={notificationsOn} onChange={(e) => setNotificationsOn(e.target.checked)} /> Gentle daily reminder</label>
        <input type="time" className="input" value={reminderTime} onChange={(e) => setReminderTime(e.target.value)} aria-label="Reminder time" disabled={!notificationsOn} />
      </div>
      <p className="text-xs text-muted">Reminders show only while Code Quest is open (or installed and running): the free hosting has no push server. No guilt messages, ever.</p>
      <div>
        <label className="label" htmlFor="rt">"Infosys Ready" badge threshold: {readyThreshold}%</label>
        <input id="rt" type="range" min={40} max={100} step={5} value={readyThreshold} onChange={(e) => setReadyThreshold(+e.target.value)} className="w-full" />
      </div>
      {err && <p className="text-bad text-sm">{err}</p>}
      <button className="btn-primary" onClick={save}>Save</button>
    </div>
  )
}

function DeleteAccount() {
  const [open, setOpen] = useState(false)
  const [pw, setPw] = useState('')
  const [err, setErr] = useState<string | null>(null)
  if (!open) return <button className="text-bad text-xs underline" onClick={() => setOpen(true)}>Delete my account and all data…</button>
  return (
    <div className="rounded-xl border border-bad/50 p-3 space-y-2">
      <p className="text-sm">This permanently deletes everything (progress, problems, notes). Export first if you want a copy.</p>
      <input type="password" className="input" placeholder="Your password" value={pw} onChange={(e) => setPw(e.target.value)} autoComplete="current-password" />
      {err && <p className="text-bad text-sm">{err}</p>}
      <div className="flex gap-2">
        <button className="btn text-white bg-bad" disabled={!pw} onClick={async () => {
          try { await api('/api/account/delete', { method: 'POST', body: { password: pw } }); clearTokens(); window.location.href = '/' } catch (e) { setErr(errorMessage(e)) }
        }}>Delete forever</button>
        <button className="btn-ghost" onClick={() => setOpen(false)}>Cancel</button>
      </div>
    </div>
  )
}
