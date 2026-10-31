import { useState } from 'react'
import { api } from '../api/client'
import { errorMessage } from '../lib/hooks'

const REASONS = [
  ['CONCEPT_GAP', "I didn't understand the concept"], ['MISREAD', 'I misread the question'],
  ['NO_DRY_RUN', "I didn't trace the code"], ['SYNTAX_API', 'I forgot the Java rule/API'], ['PANIC_TIME', 'I rushed / guessed'],
] as const

/** "Why did I get this wrong?" -> saved to the Mistake Notebook (+10 XP for honesty). */
export function WhyWrong({ topic, question }: { topic?: string; question: string }) {
  const [open, setOpen] = useState(false)
  const [reason, setReason] = useState<string>('CONCEPT_GAP')
  const [lesson, setLesson] = useState('')
  const [msg, setMsg] = useState<string | null>(null)

  async function save() {
    try {
      await api('/api/mistakes', { method: 'POST', body: {
        categoryCode: reason, topic: topic ?? null,
        whatHappened: `Quiz: ${question}`.slice(0, 2000), lessonLearned: lesson.trim() || 'Re-read the explanation and redo this question tomorrow.',
      } })
      setMsg('Saved to your Mistake Notebook (+10 XP for honesty).')
    } catch (e) { setMsg(errorMessage(e)) }
  }

  if (msg) return <p className="text-xs text-accent mt-2">{msg}</p>
  if (!open) return <button className="text-xs text-primary underline mt-2" onClick={() => setOpen(true)}>Why did I get this wrong?</button>
  return (
    <div className="mt-2 space-y-2">
      <div className="flex flex-wrap gap-1">
        {REASONS.map(([c, l]) => <button key={c} className={`chip px-2 py-1 ${reason === c ? 'chip-on' : ''}`} onClick={() => setReason(c)}>{l}</button>)}
      </div>
      <input className="input text-sm" placeholder="What will I do differently? (optional)" value={lesson} maxLength={300} onChange={(e) => setLesson(e.target.value)} />
      <button className="btn-ghost text-xs" onClick={save}>Save to Mistake Notebook</button>
    </div>
  )
}
