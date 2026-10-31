import { useCallback, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { Tabs, XpToast } from '../../components/ui'
import type { XpAward } from './types'
import ReadinessTab from './ReadinessTab'
import QuestionBankTab from './QuestionBankTab'
import AptitudeTab from './AptitudeTab'
import MockTestsTab from './MockTestsTab'
import DiaryTab from './DiaryTab'

type Tab = 'readiness' | 'bank' | 'aptitude' | 'mocks' | 'diary'
const TABS: { id: Tab; label: string }[] = [
  { id: 'readiness', label: '📊 Readiness' },
  { id: 'bank', label: '💬 Question bank' },
  { id: 'aptitude', label: '🎯 Aptitude Arena' },
  { id: 'mocks', label: '⏱️ Mock tests' },
  { id: 'diary', label: '📔 Interview diary' },
]
const IDS = TABS.map((t) => t.id)

/** Route: /interview  (tab kept in ?tab= so a reload keeps your place) */
export default function ColosseumPage() {
  const [params, setParams] = useSearchParams()
  const raw = params.get('tab') as Tab | null
  const tab: Tab = raw && IDS.includes(raw) ? raw : 'readiness'
  const [award, setAward] = useState<XpAward | null>(null)
  const clear = useCallback(() => setAward(null), [])

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold">🏟️ Interview Colosseum</h1>
          <p className="text-sm text-muted">Practise answers, aptitude and timed tests. One small step at a time.</p>
        </div>
        <Link to="/interview/mock" className="btn-gold text-sm">🐉 Mock interview boss fight</Link>
      </div>
      <Tabs tabs={TABS} value={tab} onChange={(t) => setParams(t === 'readiness' ? {} : { tab: t }, { replace: true })} />
      <div role="tabpanel" aria-label={TABS.find((t) => t.id === tab)?.label}>
        {tab === 'readiness' && <ReadinessTab />}
        {tab === 'bank' && <QuestionBankTab onAward={setAward} />}
        {tab === 'aptitude' && <AptitudeTab onAward={setAward} />}
        {tab === 'mocks' && <MockTestsTab />}
        {tab === 'diary' && <DiaryTab />}
      </div>
      <XpToast award={award} onDone={clear} />
    </div>
  )
}
