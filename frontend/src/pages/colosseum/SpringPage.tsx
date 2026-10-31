import { useCallback, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Tabs, XpToast } from '../../components/ui'
import type { XpAward } from './types'
import SpringConceptMap from './SpringConceptMap'
import { AnnotationSpells, DebugChallenges } from './SpringReference'
import SpringProjects from './SpringProjects'

type Tab = 'map' | 'spells' | 'debug' | 'projects'
const TABS: { id: Tab; label: string }[] = [
  { id: 'map', label: '🗺️ Concept map' },
  { id: 'spells', label: '✨ Annotation spells' },
  { id: 'debug', label: '🐞 Debug the app' },
  { id: 'projects', label: '🏗️ Projects' },
]
const IDS = TABS.map((t) => t.id)

/** Route: /spring */
export default function SpringPage() {
  const [params, setParams] = useSearchParams()
  const raw = params.get('tab') as Tab | null
  const tab: Tab = raw && IDS.includes(raw) ? raw : 'map'
  const [award, setAward] = useState<XpAward | null>(null)
  const clear = useCallback(() => setAward(null), [])

  return (
    <div className="space-y-5">
      <div>
        <h1 className="text-2xl font-bold">🔥 Spring Boot Track</h1>
        <p className="text-sm text-muted">Learn one topic, prove it with a quiz, explain it back, then build real projects.</p>
      </div>
      <Tabs tabs={TABS} value={tab} onChange={(t) => setParams(t === 'map' ? {} : { tab: t }, { replace: true })} />
      <div role="tabpanel" aria-label={TABS.find((t) => t.id === tab)?.label}>
        {tab === 'map' && <SpringConceptMap onAward={setAward} />}
        {tab === 'spells' && <AnnotationSpells />}
        {tab === 'debug' && <DebugChallenges />}
        {tab === 'projects' && <SpringProjects onAward={setAward} />}
      </div>
      <XpToast award={award} onDone={clear} />
    </div>
  )
}
