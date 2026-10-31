import type { Pointer, PointerTone, Tone } from './types'

/**
 * Shared SVG drawing pieces. All colors come from the Tailwind theme (fill-*, stroke-*),
 * so the drawings follow the dark / light theme automatically.
 * Class names are written out in full so Tailwind can find them.
 */

export const TONE_BOX: Record<Tone, string> = {
  normal: 'fill-raised stroke-line',
  active: 'fill-primary/30 stroke-primary',
  good: 'fill-good/25 stroke-good',
  bad: 'fill-bad/25 stroke-bad',
  done: 'fill-accent/15 stroke-accent',
  muted: 'fill-surface stroke-line opacity-40',
  gold: 'fill-gold/25 stroke-gold',
  empty: 'fill-transparent stroke-muted',
}

export const POINTER_TEXT: Record<PointerTone, string> = {
  primary: 'fill-primary',
  accent: 'fill-accent',
  gold: 'fill-gold',
  good: 'fill-good',
  bad: 'fill-bad',
}

/** Smooth movement when allowed (Tailwind's motion-safe respects prefers-reduced-motion). */
export const MOVE = 'motion-safe:transition-all motion-safe:duration-300'

const CELL = 60
const GAP = 10
const PAD = 16

/**
 * A row of boxes (an array). Shows values, index numbers under each box,
 * pointer labels (stacked when several point at the same box) and an optional highlighted range (a window).
 */
export function ArrayRow({ values, tones, pointers = [], range, ariaLabel, showIndex = true }: {
  values: (number | string | null)[]
  tones?: Tone[]
  pointers?: Pointer[]
  range?: { from: number; to: number; label?: string; tone?: 'gold' | 'primary' }
  ariaLabel: string
  showIndex?: boolean
}) {
  const n = values.length
  if (n === 0) {
    return <p className="text-center text-muted py-8 text-lg" role="img" aria-label={ariaLabel}>(empty: no boxes)</p>
  }
  const x = (i: number) => PAD + i * (CELL + GAP)
  const top = range ? 34 : 10
  // How many pointer labels are stacked under the busiest box?
  const stack: Record<number, number> = {}
  const placed = pointers.map((ptr) => {
    const row = stack[ptr.index] ?? 0
    stack[ptr.index] = row + 1
    return { ...ptr, row }
  })
  const rows = Math.max(0, ...Object.values(stack))
  const indexY = top + CELL + 20
  const ptrY = indexY + (showIndex ? 14 : 0)
  const width = Math.max(PAD * 2 + n * CELL + (n - 1) * GAP, 160)
  const height = ptrY + rows * 26 + (rows ? 14 : 4)
  const big = values.some((v) => String(v ?? '').length > 3)

  return (
    <svg viewBox={`0 0 ${width} ${height}`} className="block w-full h-auto mx-auto" style={{ maxWidth: width * 1.4 }}
      role="img" aria-label={ariaLabel}>
      {range && range.from <= range.to && (
        <g>
          <rect x={x(range.from) - 6} y={top - 6} width={x(range.to) - x(range.from) + CELL + 12} height={CELL + 12} rx={14}
            className={`${range.tone === 'primary' ? 'fill-primary/10 stroke-primary' : 'fill-gold/10 stroke-gold'} ${MOVE}`} strokeWidth={2} />
          {range.label && (
            <text x={(x(range.from) + x(range.to) + CELL) / 2} y={top - 13} textAnchor="middle"
              className={`${range.tone === 'primary' ? 'fill-primary' : 'fill-gold'} text-[14px] font-semibold`}>{range.label}</text>
          )}
        </g>
      )}
      {values.map((v, i) => {
        const tone = v === null ? 'empty' : tones?.[i] ?? 'normal'
        return (
          <g key={i}>
            <rect x={x(i)} y={top} width={CELL} height={CELL} rx={10} strokeWidth={2.5}
              strokeDasharray={tone === 'empty' ? '6 4' : undefined} className={`${TONE_BOX[tone]} ${MOVE}`} />
            <text x={x(i) + CELL / 2} y={top + CELL / 2} textAnchor="middle" dominantBaseline="central"
              className={`fill-ink font-bold font-mono ${big ? 'text-[17px]' : 'text-[22px]'}`}>{v ?? ''}</text>
            {showIndex && (
              <text x={x(i) + CELL / 2} y={indexY} textAnchor="middle" className="fill-muted text-[13px] font-mono">{i}</text>
            )}
          </g>
        )
      })}
      {placed.map((ptr) => {
        const cx = x(ptr.index) + CELL / 2
        const y = ptrY + ptr.row * 26
        const cls = POINTER_TEXT[ptr.tone ?? 'primary']
        return (
          <g key={`${ptr.label}-${ptr.index}`} className={MOVE}>
            {ptr.row === 0 && <path d={`M ${cx} ${y - 2} l -6 9 h 12 z`} className={cls} />}
            <text x={cx} y={y + 22} textAnchor="middle" className={`${cls} text-[15px] font-bold`}>{ptr.label}</text>
          </g>
        )
      })}
    </svg>
  )
}

/**
 * A stack drawn as an open box with items piled from the bottom up.
 * `items` go bottom -> top. `capacity` keeps the drawing the same height while the steps change.
 */
export function StackColumn({ items, tones, capacity, ariaLabel, title = 'Stack' }: {
  items: (string | number)[]; tones?: Tone[]; capacity: number; ariaLabel: string; title?: string
}) {
  const H = 44
  const W = 110
  const slots = Math.max(capacity, items.length, 3)
  const left = 60
  const top = 30
  const height = top + slots * (H + 6) + 20
  const bottom = top + slots * (H + 6) + 4
  return (
    <svg viewBox={`0 0 ${W + left + 20} ${height}`} className="block w-full h-auto mx-auto" style={{ maxWidth: 230 }}
      role="img" aria-label={ariaLabel}>
      <text x={left + W / 2} y={18} textAnchor="middle" className="fill-muted text-[14px] font-semibold">{title}</text>
      <path d={`M ${left - 8} ${top} V ${bottom} H ${left + W + 8} V ${top}`} className="fill-none stroke-line" strokeWidth={3} />
      {items.map((v, i) => {
        const y = bottom - 4 - (i + 1) * (H + 6) + 6
        const tone = tones?.[i] ?? 'normal'
        return (
          <g key={i}>
            <rect x={left} y={y} width={W} height={H} rx={8} strokeWidth={2.5} className={`${TONE_BOX[tone]} ${MOVE}`} />
            <text x={left + W / 2} y={y + H / 2} textAnchor="middle" dominantBaseline="central"
              className="fill-ink font-bold font-mono text-[20px]">{v}</text>
            {i === items.length - 1 && (
              <text x={left - 14} y={y + H / 2} textAnchor="end" dominantBaseline="central" className="fill-primary font-bold text-[15px]">top →</text>
            )}
          </g>
        )
      })}
      {items.length === 0 && (
        <text x={left + W / 2} y={bottom - 24} textAnchor="middle" className="fill-muted text-[14px]">empty</text>
      )}
    </svg>
  )
}

/** Java code with the current line highlighted. */
export function CodePanel({ code, line }: { code: string[]; line?: number }) {
  return (
    <pre className="rounded-xl bg-bg border border-line/60 p-3 text-[13px] leading-6 font-mono overflow-x-auto" aria-label="Java code">
      {code.map((text, i) => (
        <div key={i} className={`px-2 -mx-2 rounded ${i === line ? 'bg-primary/25 text-ink font-semibold' : 'text-muted'}`}
          aria-current={i === line ? 'step' : undefined}>
          {text || ' '}
        </div>
      ))}
    </pre>
  )
}
