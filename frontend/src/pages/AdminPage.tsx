import { useState } from 'react'
import { api } from '../api/client'
import type { Diagnostics, TestResult } from '../api/learning'
import { ErrorBox, Section, Spinner, Tabs } from '../components/ui'
import { errorMessage, useApi } from '../lib/hooks'

interface LessonRow { slug: string; world: string; section: string; order: number; title: string; version: number; adminEdited: boolean }

/** Admin only: AI diagnostics + lesson editor. */
export default function AdminPage() {
  const [tab, setTab] = useState<'ai' | 'lessons'>('ai')
  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">🛠️ Admin</h1>
      <Tabs tabs={[{ id: 'ai', label: '🧙 AI diagnostics' }, { id: 'lessons', label: '📝 Lesson editor' }]} value={tab} onChange={setTab} />
      {tab === 'ai' ? <AiDiagnostics /> : <LessonEditor />}
    </div>
  )
}

function AiDiagnostics() {
  const d = useApi<Diagnostics>('/api/admin/ai')
  const [test, setTest] = useState<TestResult | null>(null)
  const [busy, setBusy] = useState(false)
  if (d.loading && !d.data) return <Spinner />
  if (d.error || !d.data) return <ErrorBox message={d.error ?? 'No data'} onRetry={d.reload} />
  const x = d.data
  return (
    <div className="space-y-4">
      <div className="grid sm:grid-cols-2 gap-3">
        {[['AI enabled (AI_ENABLED)', x.enabled ? 'yes' : 'no'], ['Usable (key + model set)', x.usable ? 'yes' : 'no'],
          ['Active model', x.primaryModel ?? '— (set OPENROUTER_MODEL)'], ['Fallback models', x.fallbackModels.join(', ') || '—'],
          ['My usage today', `${x.myUsageToday} / ${x.dailyLimitPerUser}`], ['All users today', x.allUsageToday],
          ['Cached answers', x.cacheEntries], ['Last error', x.lastError ? `${x.lastError} (${new Date(x.lastErrorAt!).toLocaleString()})` : '—']].map(([k, v]) => (
          <div key={String(k)} className="card"><p className="text-xs text-muted">{String(k)}</p><p className="font-semibold break-words">{String(v)}</p></div>
        ))}
      </div>
      <div className="card space-y-2">
        <button className="btn-primary" disabled={busy} onClick={async () => { setBusy(true); try { setTest(await api<TestResult>('/api/admin/ai/test', { method: 'POST' })); void d.reload() } finally { setBusy(false) } }}>
          {busy ? 'Testing… (can take a minute)' : '🔌 Test AI connection'}
        </button>
        {test && <p className={test.ok ? 'text-good' : 'text-bad'}>{test.ok ? `OK: ${test.model} answered in ${test.latencyMs} ms: "${test.reply}"` : `Failed: ${test.error}`}</p>}
      </div>
      <Section title="Recent AI calls (no prompt content is stored)">
        <div className="card overflow-x-auto">
          <table className="text-xs w-full">
            <thead><tr className="text-left text-muted"><th>When</th><th>Feature</th><th>Model</th><th>OK</th><th>HTTP</th><th>ms</th><th>Error</th></tr></thead>
            <tbody>{x.recentCalls.map((c, i) => (
              <tr key={i} className="border-t border-line/40"><td>{new Date(c.created_at).toLocaleTimeString()}</td><td>{c.feature}</td><td className="max-w-[180px] truncate">{c.model}</td>
                <td>{c.success ? '✓' : '✗'}</td><td>{c.http_status}</td><td>{c.latency_ms}</td><td className="max-w-[200px] truncate">{c.error}</td></tr>
            ))}</tbody>
          </table>
        </div>
      </Section>
    </div>
  )
}

function LessonEditor() {
  const list = useApi<LessonRow[]>('/api/admin/lessons')
  const [slug, setSlug] = useState('')
  const [yaml, setYaml] = useState('')
  const [msg, setMsg] = useState<string | null>(null)
  async function open(s: string) {
    setSlug(s); setMsg(null)
    try { setYaml((await api<{ yaml: string }>(`/api/admin/lessons/${s}`)).yaml) } catch (e) { setMsg(errorMessage(e)) }
  }
  async function save() {
    try { setYaml((await api<{ yaml: string }>(`/api/admin/lessons/${slug}`, { method: 'PUT', body: { yaml } })).yaml); setMsg('Saved ✓ (this lesson is now marked as admin-edited, so file updates won\'t overwrite it)'); void list.reload() }
    catch (e) { setMsg(errorMessage(e)) }
  }
  return (
    <div className="space-y-3">
      <select className="input" value={slug} onChange={(e) => open(e.target.value)} aria-label="Lesson">
        <option value="">Choose a lesson…</option>
        {list.data?.map((l) => <option key={l.slug} value={l.slug}>{l.world === 'DSA' ? 'DSA' : l.section}{l.order}. {l.title} (v{l.version}{l.adminEdited ? ', edited' : ''})</option>)}
      </select>
      {slug && (
        <>
          <textarea className="input font-mono text-xs min-h-[60vh]" value={yaml} onChange={(e) => setYaml(e.target.value)} spellCheck={false} aria-label="Lesson YAML" />
          <p className="text-xs text-muted">Format: docs/CONTENT_FORMAT.md. The quiz answers are included here and validated on save.</p>
          {msg && <p className="text-sm whitespace-pre-line">{msg}</p>}
          <button className="btn-primary" onClick={save}>Validate and save</button>
        </>
      )}
    </div>
  )
}
