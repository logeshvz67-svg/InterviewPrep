import { useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { api } from '../../api/client'
import type { BriefingResponse, ChatResponse, ConversationView, GeneratedQuestion, HintResponse, MessageView, QuizGenResponse } from '../../api/learning'
import type { Page } from '../../api/types'
import { CodeBlock, Markdown } from '../../components/Markdown'
import { Spinner, Tabs } from '../../components/ui'
import { errorMessage, useApi } from '../../lib/hooks'

type Tab = 'chat' | 'history' | 'hint' | 'quiz' | 'briefing'
const MODES = [
  ['DOUBT', '❓ Doubt', 'Ask anything. Simple words, an analogy, a tiny example.'],
  ['SOCRATIC', '🧭 Socratic', 'Guides you with questions instead of giving the answer.'],
  ['CODE_REVIEW', '🔍 Code review', 'Paste your code: bugs, edge cases, Big-O, readability.'],
  ['MISTAKE', '🐞 Mistake explainer', 'Tell it what you thought and what went wrong.'],
] as const

/** AI Mentor. A HELPER that can be wrong: lessons and quizzes are the source of truth. */
export default function MentorPage() {
  const [params] = useSearchParams()
  const [tab, setTab] = useState<Tab>((params.get('tab') as Tab) ?? 'chat')
  const [openId, setOpenId] = useState<number | null>(null)
  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold">🧙 Mentor</h1>
      <PrivacyNotice />
      <Tabs tabs={[{ id: 'chat', label: '💬 Chat' }, { id: 'history', label: '📚 Doubt history' }, { id: 'hint', label: '🪜 Hint ladder' },
        { id: 'quiz', label: '📝 Quiz generator' }, { id: 'briefing', label: '☀️ Briefing' }]} value={tab} onChange={(t) => { setTab(t); setOpenId(null) }} />
      {tab === 'chat' && <Chat conversationId={openId} />}
      {tab === 'history' && <History onOpen={(id) => { setOpenId(id); setTab('chat') }} />}
      {tab === 'hint' && <HintLadder />}
      {tab === 'quiz' && <QuizGen />}
      {tab === 'briefing' && <Briefing />}
    </div>
  )
}

function PrivacyNotice() {
  const [hidden, setHidden] = useState(() => { try { return localStorage.getItem('cq.aiNotice') === '1' } catch { return false } })
  if (hidden) return null
  return (
    <div className="card border-warn/50 text-sm space-y-1" role="note">
      <p><b>Privacy:</b> free AI models may log prompts. Never paste passwords, API keys, or private data. (Obvious secrets are removed automatically, but be careful.)</p>
      <p><b>Accuracy:</b> the mentor can be wrong. Verify with the lesson.</p>
      <button className="text-primary text-xs underline" onClick={() => { setHidden(true); try { localStorage.setItem('cq.aiNotice', '1') } catch { /* ignore */ } }}>Got it</button>
    </div>
  )
}

function Chat({ conversationId }: { conversationId: number | null }) {
  const [mode, setMode] = useState<string>('DOUBT')
  const [convId, setConvId] = useState<number | null>(conversationId)
  const [msgs, setMsgs] = useState<MessageView[]>([])
  const [text, setText] = useState('')
  const [busy, setBusy] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)
  const [saved, setSaved] = useState<number[]>([])
  const end = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!conversationId) return
    api<ConversationView>(`/api/mentor/conversations/${conversationId}`).then((c) => { setMsgs(c.messages); setMode(c.mode); setConvId(c.id) }).catch(() => undefined)
  }, [conversationId])
  useEffect(() => { end.current?.scrollIntoView({ behavior: 'smooth' }) }, [msgs])

  async function send() {
    const m = text.trim()
    if (!m) return
    setBusy(true); setNotice(null)
    const temp: MessageView = { id: -Date.now(), role: 'user', content: m, fallback: false, createdAt: new Date().toISOString() }
    setMsgs((x) => [...x, temp]); setText('')
    try {
      const r = await api<ChatResponse>('/api/mentor/chat', { method: 'POST', body: { conversationId: convId, mode, message: m } })
      setConvId(r.conversationId)
      setMsgs((x) => [...x, r.reply])
      setNotice(r.notice)
    } catch (e) { setNotice(errorMessage(e)) } finally { setBusy(false) }
  }

  async function toCard(id: number) {
    try { await api(`/api/mentor/messages/${id}/flashcard`, { method: 'POST' }); setSaved((s) => [...s, id]) } catch (e) { alert(errorMessage(e)) }
  }

  return (
    <div className="space-y-3">
      {!convId && (
        <div className="grid sm:grid-cols-2 gap-2">
          {MODES.map(([m, l, d]) => (
            <button key={m} onClick={() => setMode(m)} className={`text-left rounded-xl p-3 ${mode === m ? 'bg-primary/20 ring-2 ring-primary' : 'bg-raised'}`}>
              <p className="font-semibold">{l}</p><p className="text-xs text-muted">{d}</p>
            </button>
          ))}
        </div>
      )}
      {convId && <div className="flex justify-between items-center text-sm"><span className="chip">{MODES.find((x) => x[0] === mode)?.[1]}</span>
        <button className="text-primary" onClick={() => { setConvId(null); setMsgs([]) }}>+ New conversation</button></div>}
      <div className="space-y-3">
        {msgs.map((m) => (
          <div key={m.id} className={`rounded-2xl p-3 max-w-[92%] ${m.role === 'user' ? 'bg-primary/15 ml-auto' : 'bg-surface border border-line/60'}`}>
            {m.role === 'assistant' ? <Markdown text={m.content} className="text-sm" /> : <p className="text-sm whitespace-pre-wrap">{m.content}</p>}
            {m.role === 'assistant' && m.id > 0 && (
              <div className="flex gap-2 mt-2 text-xs">
                {m.fallback && <span className="chip">non-AI answer</span>}
                <button className="chip" disabled={saved.includes(m.id)} onClick={() => toCard(m.id)}>{saved.includes(m.id) ? '✓ Flashcard saved' : '🃏 Convert to flashcard'}</button>
              </div>
            )}
          </div>
        ))}
        {busy && <Spinner label="Mentor is thinking… (free models can take up to a minute)" />}
        <div ref={end} />
      </div>
      {notice && <p className="text-sm text-warn" role="status">{notice}</p>}
      <div className="space-y-2">
        <textarea className={`input min-h-24 ${mode === 'CODE_REVIEW' ? 'font-mono text-sm' : ''}`} value={text} onChange={(e) => setText(e.target.value)} maxLength={6000}
          placeholder={mode === 'CODE_REVIEW' ? 'Paste your Java code (and the problem in one line)…' : 'Type your question…'}
          onKeyDown={(e) => { if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) void send() }} aria-label="Message" />
        <button className="btn-primary w-full" disabled={busy || !text.trim()} onClick={send}>Send (Ctrl+Enter)</button>
      </div>
    </div>
  )
}

function History({ onOpen }: { onOpen: (id: number) => void }) {
  const [q, setQ] = useState('')
  const [search, setSearch] = useState('')
  const list = useApi<Page<ConversationView>>(`/api/mentor/conversations?size=50${search ? `&q=${encodeURIComponent(search)}` : ''}`)
  async function toggle(c: ConversationView) {
    await api(`/api/mentor/conversations/${c.id}/resolved?value=${!c.resolved}`, { method: 'PATCH' }).catch(() => undefined)
    void list.reload()
  }
  async function del(c: ConversationView) {
    if (!confirm('Delete this conversation?')) return
    await api(`/api/mentor/conversations/${c.id}`, { method: 'DELETE' }).catch(() => undefined)
    void list.reload()
  }
  return (
    <div className="space-y-3">
      <form className="flex gap-2" onSubmit={(e) => { e.preventDefault(); setSearch(q.trim()) }}>
        <input className="input" placeholder="Search your doubts (e.g. hashmap)" value={q} onChange={(e) => setQ(e.target.value)} />
        <button className="btn-ghost">Search</button>
      </form>
      {list.loading && !list.data ? <Spinner /> : (
        <ul className="space-y-2">
          {list.data?.items.map((c) => (
            <li key={c.id} className="card flex items-center gap-3">
              <button className="flex-1 text-left" onClick={() => onOpen(c.id)}>
                <p className="font-medium">{c.title}</p>
                <p className="text-xs text-muted">{c.mode.toLowerCase().replace('_', ' ')} · {new Date(c.updatedAt).toLocaleString()}</p>
              </button>
              <button className={`chip px-3 ${c.resolved ? 'text-good' : ''}`} onClick={() => toggle(c)}>{c.resolved ? '✓ Resolved' : 'Mark resolved'}</button>
              <button className="chip text-bad" onClick={() => del(c)} aria-label="Delete">✕</button>
            </li>
          ))}
          {list.data?.items.length === 0 && <li className="text-muted">No conversations{search ? ' match' : ' yet'}.</li>}
        </ul>
      )}
    </div>
  )
}

const LEVELS = ['Nudge', 'Pattern name', 'Pseudo-code', 'Full approach']

/** Hint ladder: always try the smallest hint first. Each level is a deliberate choice. */
export function HintLadder({ initialTitle = '', initialPattern = '' }: { initialTitle?: string; initialPattern?: string }) {
  const [title, setTitle] = useState(initialTitle)
  const [problem, setProblem] = useState('')
  const [pattern, setPattern] = useState(initialPattern)
  const [attempt, setAttempt] = useState('')
  const [hints, setHints] = useState<HintResponse[]>([])
  const [busy, setBusy] = useState(false)
  const patterns = useApi<{ code: string; name: string }[]>('/api/patterns')
  const next = hints.length + 1

  async function get() {
    if (next > 1 && next === 4 && !confirm('Full approach = no more puzzle. Tried for 15+ minutes with the pseudo-code?')) return
    setBusy(true)
    try {
      const r = await api<HintResponse>('/api/mentor/hint', { method: 'POST', body: { problemTitle: title, problemText: problem || null, patternCode: pattern || null, level: next, myAttempt: attempt || null } })
      setHints([...hints, r])
    } catch (e) { alert(errorMessage(e)) } finally { setBusy(false) }
  }

  return (
    <div className="card space-y-3">
      <p className="text-sm text-muted">Attempt first! Then climb one rung at a time: {LEVELS.join(' → ')}. Logging "attempted before hint" earns +10 XP.</p>
      <input className="input" placeholder="Problem title" value={title} onChange={(e) => setTitle(e.target.value)} disabled={hints.length > 0} />
      <textarea className="input" placeholder="Problem statement (optional, helps the AI)" value={problem} onChange={(e) => setProblem(e.target.value)} disabled={hints.length > 0} maxLength={3000} />
      <select className="input" value={pattern} onChange={(e) => setPattern(e.target.value)} disabled={hints.length > 0} aria-label="Pattern (if you know it)">
        <option value="">Pattern (if you know it; used for non-AI hints)</option>
        {patterns.data?.map((p) => <option key={p.code} value={p.code}>{p.name}</option>)}
      </select>
      <textarea className="input" placeholder="What have you tried so far?" value={attempt} onChange={(e) => setAttempt(e.target.value)} maxLength={3000} />
      {hints.map((h) => (
        <div key={h.level} className="rounded-xl bg-raised p-3">
          <p className="text-xs uppercase text-primary font-semibold">Rung {h.level}: {h.levelName} {h.aiUsed ? '' : '(built-in hint)'}</p>
          <Markdown text={h.hint} className="text-sm mt-1" />
          {h.notice && <p className="text-xs text-warn mt-1">{h.notice}</p>}
        </div>
      ))}
      {next <= 4 && <button className="btn-primary" disabled={busy || !title.trim()} onClick={get}>{busy ? 'Thinking…' : `Show rung ${next}: ${LEVELS[next - 1]}`}</button>}
      {hints.length > 0 && <button className="btn-ghost text-sm" onClick={() => setHints([])}>Start over</button>}
    </div>
  )
}

function QuizGen() {
  const [topic, setTopic] = useState('')
  const [res, setRes] = useState<QuizGenResponse | null>(null)
  const [busy, setBusy] = useState(false)
  async function gen() {
    setBusy(true)
    try { setRes(await api<QuizGenResponse>('/api/mentor/quiz', { method: 'POST', body: { topic, count: 5 } })) } catch (e) { alert(errorMessage(e)) } finally { setBusy(false) }
  }
  return (
    <div className="space-y-3">
      <div className="card space-y-2">
        <input className="input" placeholder="Topic, e.g. HashMap, polymorphism, sliding window" value={topic} onChange={(e) => setTopic(e.target.value)} maxLength={120} />
        <button className="btn-primary" disabled={busy || !topic.trim()} onClick={gen}>{busy ? 'Generating…' : 'Generate 5 practice questions'}</button>
        <p className="text-xs text-muted">Practice only (no XP). If AI is unavailable you get questions from the lessons instead.</p>
      </div>
      {res && (
        <div className="space-y-3">
          <p className="text-sm text-muted">Source: {res.source}{res.notice ? ` · ${res.notice}` : ''}</p>
          {res.questions.map((q, i) => <GenQuestion key={i} q={q} n={i + 1} />)}
        </div>
      )}
    </div>
  )
}

function GenQuestion({ q, n }: { q: GeneratedQuestion; n: number }) {
  const [pick, setPick] = useState<number | null>(null)
  return (
    <div className="card space-y-2">
      <p className="font-medium">{n}. {q.prompt}</p>
      {q.code && <CodeBlock code={q.code} />}
      {q.options.map((o, i) => (
        <button key={i} disabled={pick !== null} onClick={() => setPick(i)}
          className={`block w-full text-left rounded-xl px-3 py-2 bg-raised ${pick !== null && i === q.answer ? 'ring-2 ring-good' : pick === i ? 'ring-2 ring-bad' : ''}`}>{o}</button>
      ))}
      {pick !== null && <p className="text-sm">{pick === q.answer ? '✅ ' : '❌ '}{q.explanation}</p>}
    </div>
  )
}

function Briefing() {
  const [kind, setKind] = useState<'MORNING' | 'DEBRIEF' | 'WEEKLY_PLAN'>('MORNING')
  const b = useApi<BriefingResponse>(`/api/mentor/briefing?kind=${kind}`)
  return (
    <div className="space-y-3">
      <Tabs tabs={[{ id: 'MORNING', label: '☀️ Morning briefing' }, { id: 'DEBRIEF', label: '🌙 End-of-day debrief' }, { id: 'WEEKLY_PLAN', label: '🗓️ Next week' }]} value={kind} onChange={setKind} />
      {b.loading ? <Spinner /> : b.data && (
        <div className="card"><Markdown text={b.data.text} /><p className="text-xs text-muted mt-2">{b.data.aiUsed ? 'Written by the AI mentor from your data.' : 'Built from your data (AI not used).'}</p></div>
      )}
    </div>
  )
}
