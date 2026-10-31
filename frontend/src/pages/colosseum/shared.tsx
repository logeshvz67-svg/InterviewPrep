import type { ReactNode } from 'react'

/** Code in a mono block with horizontal scroll (never wraps, never breaks the layout). */
export function CodeBlock({ code, label }: { code: string; label?: string }) {
  return (
    <pre className="font-mono text-xs sm:text-sm bg-raised border border-line/60 rounded-xl p-3 overflow-x-auto whitespace-pre max-w-full"
      aria-label={label} tabIndex={0}>
      <code>{code.replace(/\n$/, '')}</code>
    </pre>
  )
}

/**
 * Multiple choice options as a radio group.
 * After `answer` is known, the right option turns green and a wrong pick turns red.
 */
export function Options({ name, options, value, onChange, answer, disabled }: {
  name: string; options: string[]; value: number | null; onChange: (i: number) => void; answer?: number | null; disabled?: boolean
}) {
  const revealed = answer !== undefined && answer !== null
  return (
    <div role="radiogroup" className="space-y-2">
      {options.map((o, i) => {
        const picked = value === i
        let tone = picked ? 'border-primary bg-primary/10' : 'border-line bg-raised'
        if (revealed && i === answer) tone = 'border-good bg-good/10'
        else if (revealed && picked) tone = 'border-bad bg-bad/10'
        return (
          <label key={i} className={`flex items-start gap-3 rounded-xl border px-3 py-2.5 cursor-pointer text-sm ${tone} ${disabled ? 'cursor-default' : ''}`}>
            <input type="radio" name={name} className="mt-1 accent-primary" checked={picked}
              disabled={disabled} onChange={() => onChange(i)} />
            <span className="whitespace-pre-wrap break-words min-w-0">
              <b className="text-muted mr-1">{String.fromCharCode(65 + i)}.</b>{o}
              {revealed && i === answer && <span className="sr-only"> (correct answer)</span>}
            </span>
          </label>
        )
      })}
    </div>
  )
}

export function Explanation({ correct, children }: { correct?: boolean; children: ReactNode }) {
  return (
    <div className={`rounded-xl p-3 text-sm border ${correct === undefined ? 'border-line bg-raised' : correct ? 'border-good/50 bg-good/10' : 'border-bad/50 bg-bad/10'}`} role="status">
      {correct !== undefined && <b className={correct ? 'text-good' : 'text-bad'}>{correct ? '✓ Correct. ' : '✗ Not quite. '}</b>}
      {children}
    </div>
  )
}

export function pct(n: number | null | undefined, digits = 0): string {
  if (n === null || n === undefined) return '—'
  return `${Number(n).toFixed(digits)}%`
}

export function niceCode(code: string): string {
  return code.split('_').map((w) => w.charAt(0) + w.slice(1).toLowerCase()).join(' ')
}

export function today(): string {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
