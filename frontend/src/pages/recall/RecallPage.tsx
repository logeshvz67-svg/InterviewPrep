import { useCallback, useEffect, useState } from 'react'
import { api } from '../../api/client'
import type { FlashcardView, QuestionResult, RecallComplete, ReviewResult, WarmupView } from '../../api/learning'
import type { Page, XpAward } from '../../api/types'
import { QuizRunner } from '../../components/QuizRunner'
import { ErrorBox, Section, Spinner, Tabs, XpToast } from '../../components/ui'
import { errorMessage, useApi } from '../../lib/hooks'

type Tab = 'warmup' | 'cards' | 'new'

/** Recall Engine: daily 10-minute warm-up (due flashcards + 3 quick questions) before learning anything new. */
export default function RecallPage() {
  const [tab, setTab] = useState<Tab>('warmup')
  const [award, setAward] = useState<XpAward | null>(null)
  const clear = useCallback(() => setAward(null), [])
  return (
    <div className="space-y-5">
      <h1 className="text-2xl font-bold">🧠 Recall</h1>
      <p className="text-sm text-muted">Pulling an answer out of memory beats re-reading. Cards come back after 1, 3, 7, 14, then 30 days.</p>
      <Tabs tabs={[{ id: 'warmup', label: '🔥 Warm-up' }, { id: 'cards', label: '🃏 All cards' }, { id: 'new', label: '➕ New card' }]} value={tab} onChange={setTab} />
      {tab === 'warmup' && <Warmup onAward={setAward} />}
      {tab === 'cards' && <CardList />}
      {tab === 'new' && <NewCard onDone={() => setTab('cards')} />}
      <XpToast award={award} onDone={clear} />
    </div>
  )
}

function Warmup({ onAward }: { onAward: (a: XpAward) => void }) {
  const { data, error, loading, reload } = useApi<WarmupView>('/api/recall/warmup')
  const [queue, setQueue] = useState<FlashcardView[]>([])
  const [flipped, setFlipped] = useState(false)
  const [stats, setStats] = useState({ reviewed: 0, dueLeft: 0 })
  const [done, setDone] = useState<RecallComplete | null>(null)

  useEffect(() => {
    if (data) { setQueue(data.cards); setStats({ reviewed: data.reviewedToday, dueLeft: data.dueTotal }) }
  }, [data])

  if (loading && !data) return <Spinner />
  if (error || !data) return <ErrorBox message={error ?? 'No data'} onRetry={reload} />

  const card = queue[0]

  async function grade(g: 'AGAIN' | 'HARD' | 'GOOD' | 'EASY') {
    if (!card) return
    try {
      const r = await api<ReviewResult>(`/api/flashcards/${card.id}/review`, { method: 'POST', body: { grade: g } })
      setStats({ reviewed: r.reviewedToday, dueLeft: r.dueLeft })
      setQueue(g === 'AGAIN' ? [...queue.slice(1), card] : queue.slice(1)) // "Again" comes back in this session
      setFlipped(false)
    } catch (e) { alert(errorMessage(e)) }
  }

  async function finish() {
    try {
      const r = await api<RecallComplete>('/api/recall/complete', { method: 'POST' })
      setDone(r)
      if (r.xp.awarded) onAward(r.xp)
    } catch (e) { alert(errorMessage(e)) }
  }

  return (
    <div className="space-y-6">
      <Section title={`Flashcards due (${queue.length})`}>
        {card ? (
          <div className="space-y-3">
            <button className="card w-full min-h-48 text-left grid place-items-center" onClick={() => setFlipped(!flipped)} aria-live="polite">
              <div className="space-y-2 w-full">
                <p className="text-xs text-muted">{card.topic ?? card.sourceType} · {flipped ? 'Answer' : 'Question'} (tap to flip)</p>
                <p className="text-lg whitespace-pre-line">{flipped ? card.back : card.front}</p>
              </div>
            </button>
            {!flipped ? (
              <button className="btn-primary w-full" onClick={() => setFlipped(true)}>Show answer (try to remember first!)</button>
            ) : (
              <div className="grid grid-cols-4 gap-2" role="group" aria-label="How well did you remember?">
                <button className="btn-ghost text-bad" onClick={() => grade('AGAIN')}>Again</button>
                <button className="btn-ghost text-warn" onClick={() => grade('HARD')}>Hard</button>
                <button className="btn-ghost text-good" onClick={() => grade('GOOD')}>Good</button>
                <button className="btn-ghost text-accent" onClick={() => grade('EASY')}>Easy</button>
              </div>
            )}
          </div>
        ) : (
          <div className="card text-muted">{stats.reviewed > 0 ? '🎉 All due cards reviewed!' : 'No cards due today. Complete lessons to earn flashcards.'}</div>
        )}
        <p className="text-xs text-muted">Reviewed today: {stats.reviewed} · still due: {stats.dueLeft}</p>
      </Section>

      {data.questions.length > 0 && (
        <Section title="3 quick questions from lessons you finished">
          <QuizRunner questions={data.questions} submitLabel="Check" onSubmit={async (answers) => {
            try { return await api<QuestionResult[]>('/api/recall/warmup/answers', { method: 'POST', body: { answers } }) }
            catch (e) { alert(errorMessage(e)); return null }
          }} />
        </Section>
      )}

      <div className="card space-y-2">
        {done ? <p className="text-good">{done.message}{done.xp.awarded ? ` +${done.xp.xp} XP` : ''}</p> : (
          <>
            <p className="text-sm">{data.rewardedToday ? 'Recall session already counted today. Extra reviews still help!' : 'Finish the session to earn +15 XP (5+ cards, or all due cards).'}</p>
            <button className="btn-gold" onClick={finish}>Finish recall session</button>
          </>
        )}
      </div>
    </div>
  )
}

function CardList() {
  const [q, setQ] = useState('')
  const [page, setPage] = useState(0)
  const list = useApi<Page<FlashcardView>>(`/api/flashcards?page=${page}&size=30&q=${encodeURIComponent(q)}`)
  async function toggle(c: FlashcardView) {
    await api(`/api/flashcards/${c.id}/suspended?value=${!c.suspended}`, { method: 'PATCH' }).catch(() => undefined)
    void list.reload()
  }
  async function del(c: FlashcardView) {
    if (!confirm('Delete this card?')) return
    await api(`/api/flashcards/${c.id}`, { method: 'DELETE' }).catch(() => undefined)
    void list.reload()
  }
  return (
    <div className="space-y-3">
      <input className="input" placeholder="Search cards" value={q} onChange={(e) => { setQ(e.target.value); setPage(0) }} />
      {list.loading && !list.data ? <Spinner /> : (
        <ul className="space-y-2">
          {list.data?.items.map((c) => (
            <li key={c.id} className={`card text-sm ${c.suspended ? 'opacity-60' : ''}`}>
              <p className="font-medium">{c.front}</p>
              <p className="text-muted mt-1 whitespace-pre-line">{c.back}</p>
              <div className="flex flex-wrap gap-2 items-center mt-2 text-xs text-muted">
                <span>{c.topic}</span><span>· next {c.nextReviewOn}</span><span>· every {c.intervalDays || 1} d</span>
                <button className="chip" onClick={() => toggle(c)}>{c.suspended ? 'Resume' : 'Pause'}</button>
                <button className="chip text-bad" onClick={() => del(c)}>Delete</button>
              </div>
            </li>
          ))}
          {list.data?.items.length === 0 && <li className="text-muted">No cards yet. They're created when you complete lessons, or from mistakes and mentor answers.</li>}
        </ul>
      )}
      {list.data && list.data.totalPages > 1 && (
        <div className="flex justify-between"><button className="btn-ghost" disabled={page === 0} onClick={() => setPage(page - 1)}>←</button>
          <span className="text-sm text-muted">{page + 1}/{list.data.totalPages}</span>
          <button className="btn-ghost" disabled={page + 1 >= list.data.totalPages} onClick={() => setPage(page + 1)}>→</button></div>
      )}
    </div>
  )
}

function NewCard({ onDone }: { onDone: () => void }) {
  const [f, setF] = useState({ front: '', back: '', topic: '' })
  const [err, setErr] = useState<string | null>(null)
  async function save() {
    try { await api('/api/flashcards', { method: 'POST', body: { ...f, topic: f.topic || null } }); onDone() } catch (e) { setErr(errorMessage(e)) }
  }
  return (
    <div className="card space-y-3">
      <div><label className="label" htmlFor="ff">Question (front)</label><textarea id="ff" className="input" maxLength={500} value={f.front} onChange={(e) => setF({ ...f, front: e.target.value })} /></div>
      <div><label className="label" htmlFor="fb">Answer (back), in your own words</label><textarea id="fb" className="input" maxLength={2000} value={f.back} onChange={(e) => setF({ ...f, back: e.target.value })} /></div>
      <div><label className="label" htmlFor="ft">Topic (optional)</label><input id="ft" className="input" maxLength={160} value={f.topic} onChange={(e) => setF({ ...f, topic: e.target.value })} /></div>
      {err && <p className="text-bad text-sm">{err}</p>}
      <button className="btn-primary" disabled={!f.front.trim() || !f.back.trim()} onClick={save}>Save card</button>
    </div>
  )
}
