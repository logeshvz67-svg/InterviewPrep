import { useState } from 'react'
import type { QuestionResult, QuestionView } from '../api/learning'
import { CodeBlock } from './Markdown'
import { WhyWrong } from './WhyWrong'

type Answer = number | string | string[] | undefined

/**
 * Renders any set of quiz questions (MCQ, true/false, predict-the-output, spot-the-bug,
 * fill-the-code, match) and, after submitting, the server's verdict with explanations.
 * Grading ALWAYS happens on the server: this component only collects choices.
 */
export function QuizRunner({ questions, onSubmit, submitLabel = 'Submit answers', topic, shuffleKey }: {
  questions: QuestionView[]
  onSubmit: (answers: Record<string, unknown>) => Promise<QuestionResult[] | null>
  submitLabel?: string
  topic?: string
  shuffleKey?: string
}) {
  const [answers, setAnswers] = useState<Record<string, Answer>>({})
  const [results, setResults] = useState<Record<string, QuestionResult> | null>(null)
  const [busy, setBusy] = useState(false)
  const set = (id: string, v: Answer) => !results && setAnswers({ ...answers, [id]: v })
  const answered = questions.filter((q) => isAnswered(q, answers[q.id])).length

  async function submit() {
    setBusy(true)
    try {
      const r = await onSubmit(answers as Record<string, unknown>)
      if (r) setResults(Object.fromEntries(r.map((x) => [x.id, x])))
    } finally { setBusy(false) }
  }

  return (
    <div className="space-y-4" key={shuffleKey}>
      {questions.map((q, i) => {
        const res = results?.[q.id]
        return (
          <fieldset key={q.id} className={`card space-y-3 ${res ? (res.correct ? 'border-good/60' : 'border-bad/60') : ''}`}>
            <legend className="sr-only">Question {i + 1}</legend>
            <div className="flex gap-2 items-start">
              <span className="chip shrink-0">{i + 1}</span>
              <div className="flex-1">
                <p className="text-xs uppercase tracking-wide text-muted">{TYPE_LABEL[q.type]}</p>
                <p className="font-medium whitespace-pre-line">{q.prompt}</p>
              </div>
            </div>
            {q.code && <CodeBlock code={q.code} />}
            <QuestionInput q={q} value={answers[q.id]} onChange={(v) => set(q.id, v)} disabled={!!results} result={res} />
            {res && (
              <div className={`rounded-xl p-3 text-sm ${res.correct ? 'bg-good/10' : 'bg-bad/10'}`} role="status">
                <p className="font-semibold">{res.correct ? '✅ Correct' : '❌ Not quite'}</p>
                {!res.correct && <p className="mt-1"><b>Answer:</b> <span className="whitespace-pre-line font-mono">{res.correctAnswer}</span></p>}
                <p className="mt-1 whitespace-pre-line">{res.explanation}</p>
                {!res.correct && <WhyWrong topic={topic} question={q.prompt} />}
              </div>
            )}
          </fieldset>
        )
      })}
      {!results && (
        <button className="btn-primary w-full" disabled={busy || answered === 0} onClick={submit}>
          {busy ? 'Checking…' : `${submitLabel} (${answered}/${questions.length} answered)`}
        </button>
      )}
    </div>
  )
}

const TYPE_LABEL: Record<string, string> = {
  MCQ: 'Multiple choice', TRUE_FALSE: 'True or false', PREDICT_OUTPUT: 'Predict the output', SPOT_THE_BUG: 'Spot the bug',
  FILL_THE_CODE: 'Fill the code', MATCH: 'Match the concept',
}

function isAnswered(q: QuestionView, v: Answer) {
  if (v === undefined) return false
  if (q.type === 'MATCH') return Array.isArray(v) && v.every(Boolean)
  if (q.type === 'FILL_THE_CODE') return typeof v === 'string' && v.trim() !== ''
  return true
}

function QuestionInput({ q, value, onChange, disabled, result }: {
  q: QuestionView; value: Answer; onChange: (v: Answer) => void; disabled: boolean; result?: QuestionResult
}) {
  if (q.type === 'FILL_THE_CODE') {
    return (
      <input className="input font-mono" placeholder="Type what goes in ____" value={(value as string) ?? ''} disabled={disabled}
        onChange={(e) => onChange(e.target.value)} aria-label="Your answer" autoCapitalize="off" autoCorrect="off" spellCheck={false} />
    )
  }
  if (q.type === 'MATCH') {
    const opts = q.options as { left: string[]; right: string[] }
    const chosen = (value as string[]) ?? opts.left.map(() => '')
    return (
      <div className="space-y-2">
        {opts.left.map((l, i) => (
          <label key={l} className="grid sm:grid-cols-[1fr_1.4fr] gap-2 items-center">
            <span className="font-medium">{l}</span>
            <select className="input py-2" value={chosen[i]} disabled={disabled}
              onChange={(e) => { const c = [...chosen]; c[i] = e.target.value; onChange(c) }}>
              <option value="">Choose…</option>
              {opts.right.map((r) => <option key={r} value={r}>{r}</option>)}
            </select>
          </label>
        ))}
      </div>
    )
  }
  const options = (q.options as string[]) ?? []
  return (
    <div className="grid gap-2" role="radiogroup">
      {options.map((o, i) => {
        const picked = value === i
        const tone = result && picked ? (result.correct ? 'ring-2 ring-good' : 'ring-2 ring-bad') : picked ? 'ring-2 ring-primary bg-primary/10' : ''
        return (
          <button type="button" key={i} role="radio" aria-checked={picked} disabled={disabled} onClick={() => onChange(i)}
            className={`text-left rounded-xl bg-raised px-3 py-2.5 whitespace-pre-line ${q.type === 'PREDICT_OUTPUT' ? 'font-mono text-sm' : ''} ${tone}`}>
            {o.replace(/\\n/g, '\n')}
          </button>
        )
      })}
    </div>
  )
}
