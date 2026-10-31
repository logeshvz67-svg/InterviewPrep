import { useId, useState, type FormEvent, type ReactNode } from 'react'
import type { Parsed } from './lib'

/**
 * A text box with a "Use" button. The learner types freely; we only apply the value
 * when it passes `parse` (so the visualizer never gets bad input). Errors are shown under the box.
 */
export function DraftField<T>({ label, initial, parse, onApply, hint, inputMode }: {
  label: string
  initial: string
  parse: (text: string) => Parsed<T>
  onApply: (value: T) => void
  hint?: string
  inputMode?: 'text' | 'numeric'
}) {
  const id = useId()
  const [draft, setDraft] = useState(initial)
  const [error, setError] = useState<string | null>(null)

  const submit = (e: FormEvent) => {
    e.preventDefault()
    const r = parse(draft)
    if ('error' in r) { setError(r.error); return }
    setError(null)
    onApply(r.value)
  }

  return (
    <form onSubmit={submit} className="min-w-0">
      <label htmlFor={id} className="label">{label}</label>
      <div className="flex gap-2">
        <input id={id} className="input font-mono" value={draft} inputMode={inputMode}
          aria-invalid={!!error} aria-describedby={`${id}-help`}
          onChange={(e) => setDraft(e.target.value)} autoComplete="off" spellCheck={false} />
        <button type="submit" className="btn-ghost shrink-0">Use</button>
      </div>
      <p id={`${id}-help`} className={`mt-1 text-xs ${error ? 'text-bad' : 'text-muted'}`} role={error ? 'alert' : undefined}>
        {error ?? hint}
      </p>
    </form>
  )
}

/** A number picker with big − and + buttons. Always stays inside [min, max]. */
export function Stepper({ label, value, min, max, onChange, hint }: {
  label: string; value: number; min: number; max: number; onChange: (v: number) => void; hint?: string
}) {
  const id = useId()
  return (
    <div>
      <span id={id} className="label">{label}</span>
      <div className="flex items-center gap-2" role="group" aria-labelledby={id}>
        <button type="button" className="btn-ghost h-11 w-11 p-0 text-xl" aria-label={`${label}: less`}
          disabled={value <= min} onClick={() => onChange(Math.max(min, value - 1))}>−</button>
        <output className="min-w-[3rem] text-center text-xl font-bold font-mono" aria-live="polite">{value}</output>
        <button type="button" className="btn-ghost h-11 w-11 p-0 text-xl" aria-label={`${label}: more`}
          disabled={value >= max} onClick={() => onChange(Math.min(max, value + 1))}>+</button>
      </div>
      <p className="mt-1 text-xs text-muted">{hint ?? `From ${min} to ${max}.`}</p>
    </div>
  )
}

/** The box that holds the input fields above a visualizer. */
export function InputPanel({ children }: { children: ReactNode }) {
  return (
    <fieldset className="card grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      <legend className="sr-only">Change the input</legend>
      {children}
    </fieldset>
  )
}

/** Small stat boxes under a drawing: "Sum: 12", "Best: 15". */
export function Stats({ items }: { items: { label: string; value: ReactNode; tone?: 'gold' | 'good' | 'bad' | 'primary' }[] }) {
  const toneClass = { gold: 'text-gold', good: 'text-good', bad: 'text-bad', primary: 'text-primary' }
  return (
    <dl className="flex flex-wrap justify-center gap-2 mt-3">
      {items.map((it) => (
        <div key={it.label} className="rounded-xl bg-raised px-3 py-1.5 text-center">
          <dt className="text-xs text-muted">{it.label}</dt>
          <dd className={`text-lg font-bold font-mono ${it.tone ? toneClass[it.tone] : ''}`}>{it.value}</dd>
        </div>
      ))}
    </dl>
  )
}
