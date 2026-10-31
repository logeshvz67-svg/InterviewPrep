import type { QuestBoard, Badge } from '../../api/learning'
import type { QuestView } from '../../api/types'
import { ErrorBox, Section, Spinner } from '../../components/ui'
import { useApi } from '../../lib/hooks'

/** Quest Board + badges. You only ever compete with your past self. */
export default function QuestsPage() {
  const board = useApi<QuestBoard>('/api/quests/board')
  const badges = useApi<Badge[]>('/api/achievements')
  if (board.loading && !board.data) return <Spinner />
  if (board.error || !board.data) return <ErrorBox message={board.error ?? 'No data'} onRetry={board.reload} />
  const b = board.data
  const unlocked = badges.data?.filter((x) => x.unlockedAt).length ?? 0
  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">📜 Quest Board</h1>
      <Section title="Today (max 3)"><QuestList quests={b.daily} /></Section>
      <Section title={`This week (from ${b.weekStart})`}><QuestList quests={b.weekly} /></Section>
      <Section title="Story quests"><QuestList quests={b.story} /></Section>
      <Section title={`Badges (${unlocked}/${badges.data?.length ?? 0})`}>
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
          {badges.data?.map((x) => (
            <div key={x.code} className={`card text-center ${x.unlockedAt ? 'border-gold/60' : 'opacity-50'}`}>
              <div className={`text-3xl ${x.unlockedAt ? '' : 'grayscale'}`} aria-hidden>{x.icon}</div>
              <p className="font-semibold text-sm mt-1">{x.name}</p>
              <p className="text-xs text-muted">{x.description}</p>
              {x.unlockedAt && <p className="text-xs text-gold mt-1">Unlocked {new Date(x.unlockedAt).toLocaleDateString()}</p>}
            </div>
          ))}
        </div>
      </Section>
    </div>
  )
}

function QuestList({ quests }: { quests: QuestView[] }) {
  return (
    <ul className="grid gap-2">
      {quests.map((q) => (
        <li key={q.code} className={`card flex items-center gap-3 ${q.completed ? 'opacity-70' : ''}`}>
          <span className="text-2xl" aria-hidden>{q.completed ? '✅' : '📜'}</span>
          <div className="flex-1">
            <p className="font-medium">{q.title}</p>
            <p className="text-xs text-muted">{q.description}</p>
            {q.target > 1 && (
              <div className="mt-1 h-1.5 rounded bg-raised"><div className="h-full rounded bg-primary" style={{ width: `${(q.progress / q.target) * 100}%` }} /></div>
            )}
          </div>
          <div className="text-right text-sm">
            <p className="text-gold">+{q.goldReward}</p>
            {q.target > 1 && <p className="text-xs text-muted">{q.progress}/{q.target}</p>}
          </div>
        </li>
      ))}
    </ul>
  )
}
