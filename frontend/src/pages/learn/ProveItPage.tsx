import { useCallback, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../../api/client'
import type { QuestionView, QuizResult } from '../../api/learning'
import type { XpAward } from '../../api/types'
import { QuizRunner } from '../../components/QuizRunner'
import { ErrorBox, Spinner, XpToast } from '../../components/ui'
import { errorMessage, useApi } from '../../lib/hooks'

/** Optional "Prove it" test: 90%+ marks the topic Mastered (spaced review only). Never auto-skips content. */
export default function ProveItPage() {
  const { slug = '' } = useParams()
  const qs = useApi<QuestionView[]>(`/api/lessons/${slug}/prove-it`)
  const [result, setResult] = useState<QuizResult | null>(null)
  const [award, setAward] = useState<XpAward | null>(null)
  const clear = useCallback(() => setAward(null), [])
  if (qs.loading && !qs.data) return <Spinner />
  if (qs.error || !qs.data) return <ErrorBox message={qs.error ?? 'Not found'} onRetry={qs.reload} />
  return (
    <div className="space-y-4 max-w-3xl">
      <Link to={`/lesson/${slug}`} className="text-sm text-primary">← Back to the lesson</Link>
      <h1 className="text-2xl font-bold">🎖️ Prove it</h1>
      <p className="text-muted">Score 90% or more and this topic becomes <b>Mastered</b>: it moves to spaced review only. Below 90%? No problem: the lesson will fill the gaps.</p>
      <QuizRunner questions={qs.data} submitLabel="Submit Prove-it" onSubmit={async (answers) => {
        try {
          const r = await api<QuizResult>(`/api/lessons/${slug}/prove-it`, { method: 'POST', body: { answers } })
          setResult(r)
          const xp = r.xp.reduce((s, x) => s + x.xp, 0)
          if (xp) setAward({ awarded: true, xp, gold: r.xp.reduce((s, x) => s + x.gold, 0), reason: null })
          return r.results
        } catch (e) { alert(errorMessage(e)); return null }
      }} />
      {result && (
        <div className={`card ${result.passed ? 'border-gold/60' : ''}`} role="status">
          <p className="text-2xl font-bold font-display">{Math.round(result.percent)}%</p>
          <p>{result.message}</p>
          <Link className="btn-primary mt-2" to={`/lesson/${slug}`}>{result.passed ? 'Back to the lesson' : 'Study the lesson'}</Link>
        </div>
      )}
      <XpToast award={award} onDone={clear} />
    </div>
  )
}
