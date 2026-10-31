import { useMemo, useState } from 'react'
import { Tabs } from '../ui'
import { ArrayRow } from './draw'
import { DraftField, InputPanel, Stats, Stepper } from './fields'
import { clamp, mcq, nearNumbers, p, parseNumberList, type Parsed } from './lib'
import { StepPlayer } from './StepPlayer'
import type { BaseStep, Pointer, Tone } from './types'

/**
 * SLIDING WINDOW
 * A "window" is a group of boxes next to each other. Instead of starting again for every group,
 * we slide the window one box to the right and only fix what changed.
 *  - Fixed size k, max sum: new sum = old sum + (box coming in) - (box going out).       O(n)
 *  - Variable size, longest substring with no repeated letter:
 *      grow the window on the right; if the new letter is already inside,
 *      shrink from the left until it is gone. A set remembers the letters inside.      O(n)
 */

type Mode = 'fixed' | 'variable'
const MODES: { id: Mode; label: string }[] = [{ id: 'fixed', label: '📏 Max sum, size k' }, { id: 'variable', label: '🔤 Longest no-repeat text' }]

interface SWStep extends BaseStep {
  values: (number | string)[]
  tones: Tone[]
  pointers: Pointer[]
  window?: { from: number; to: number }
  windowTone?: 'gold' | 'primary'
  stats: { label: string; value: string | number; tone?: 'gold' | 'good' | 'bad' | 'primary' }[]
  set?: string[]
}

const FIXED_CODE = [
  'int sum = 0;',
  'for (int i = 0; i < k; i++) sum += a[i];',
  'int best = sum;',
  'for (int i = k; i < n; i++) {',
  '    sum += a[i] - a[i - k];',
  '    best = Math.max(best, sum);',
  '}',
  'return best;',
]

const VARIABLE_CODE = [
  'Set<Character> set = new HashSet<>();',
  'int left = 0, best = 0;',
  'for (int right = 0; right < s.length(); right++) {',
  '    while (set.contains(s.charAt(right))) {',
  '        set.remove(s.charAt(left)); left++;',
  '    }',
  '    set.add(s.charAt(right));',
  '    best = Math.max(best, right - left + 1);',
  '}',
]

function fixedSteps(a: number[], k: number): SWStep[] {
  const n = a.length
  const steps: SWStep[] = [{
    values: a, tones: a.map(() => 'normal'), pointers: [], stats: [{ label: 'k', value: k }], line: 0,
    caption: `We want the biggest sum of ${k} boxes in a row. A window of ${k} boxes will slide from left to right.`,
  }]
  let sum = a.slice(0, k).reduce((s, v) => s + v, 0)
  let best = sum
  let bestStart = 0
  steps.push({
    values: a, tones: a.map((_, j) => (j < k ? 'active' : 'normal')), pointers: [], window: { from: 0, to: k - 1 },
    stats: [{ label: 'Window sum', value: sum, tone: 'primary' }, { label: 'Best', value: best, tone: 'gold' }], line: 2,
    caption: `First window: index 0 to ${k - 1}. Sum = ${a.slice(0, k).map(p).join(' + ')} = ${sum}. Best so far = ${sum}.`,
    predict: mcq(`What is the sum of the first ${k} boxes?`, String(sum),
      nearNumbers(sum, [sum - a[k - 1], k < n ? sum + a[k] : sum + 3]), 'Add the first k values together.'),
  })
  for (let i = k; i < n; i++) {
    const out = a[i - k]
    const inn = a[i]
    const next = sum + inn - out
    const better = next > best
    steps.push({
      values: a,
      tones: a.map((_, j) => (j === i ? 'good' : j === i - k ? 'bad' : j > i - k && j < i ? 'active' : 'normal')),
      pointers: [{ index: i, label: 'in', tone: 'good' }, { index: i - k, label: 'out', tone: 'bad' }],
      window: { from: i - k + 1, to: i },
      stats: [{ label: 'Window sum', value: next, tone: 'primary' }, { label: 'Best', value: Math.max(best, next), tone: 'gold' }],
      line: 4,
      caption: `Slide right. Add ${inn} (coming in), remove ${out} (going out). Sum = ${sum} + ${p(inn)} − ${p(out)} = ${next}.`
        + (better ? ` New best: ${next}!` : ` Best stays ${best}.`),
      predict: mcq(`The window slides right: ${inn} comes in, ${out} goes out. What is the new sum?`, String(next),
        [String(sum + inn), String(sum - out), String(sum), ...nearNumbers(next)], 'New sum = old sum + new box − old box. No need to add everything again.'),
    })
    sum = next
    if (better) { best = next; bestStart = i - k + 1 }
  }
  steps.push({
    values: a, tones: a.map((_, j) => (j >= bestStart && j < bestStart + k ? 'good' : 'muted')), pointers: [],
    window: { from: bestStart, to: bestStart + k - 1 }, windowTone: 'gold',
    stats: [{ label: 'Best sum', value: best, tone: 'gold' }], line: 7,
    caption: `Done! The biggest sum is ${best} (index ${bestStart} to ${bestStart + k - 1}). Each box went in and out once: O(n) time.`,
  })
  return steps
}

function variableSteps(s: string): SWStep[] {
  const chars = s.split('')
  const n = chars.length
  const set = new Set<string>()
  let left = 0
  let best = 0
  let bestFrom = 0
  const stats = () => [{ label: 'Length', value: set.size, tone: 'primary' as const }, { label: 'Best', value: best, tone: 'gold' as const }]
  const tonesFor = (from: number, to: number, current: number, currentTone: Tone): Tone[] =>
    chars.map((_, j) => (j === current ? currentTone : j >= from && j <= to ? 'active' : j < from ? 'muted' : 'normal'))
  const steps: SWStep[] = [{
    values: chars, tones: chars.map(() => 'normal'), pointers: [], stats: stats(), set: [], line: 0,
    caption: 'We want the longest part of the text with NO repeated letter. The window grows on the right and shrinks on the left. A set remembers the letters inside.',
  }]
  for (let right = 0; right < n; right++) {
    const c = chars[right]
    const askText = () => `Next letter: '${c}'. Window letters: {${[...set].join(', ')}}. What happens next?`
    const addLabel = `Add '${c}' to the window`
    // Repeat found: shrink from the left until the old copy of c is gone.
    while (set.has(c)) {
      const gone = chars[left]
      const removeLabel = `Remove '${gone}' from the left`
      const predict = mcq(askText(), removeLabel, [addLabel], `'${c}' is already inside, so the window must shrink first.`)
      set.delete(gone)
      left++
      steps.push({
        values: chars, tones: tonesFor(left, right - 1, right, 'bad'),
        pointers: [{ index: left, label: 'L', tone: 'primary' }, { index: right, label: 'R', tone: 'accent' }],
        window: left <= right - 1 ? { from: left, to: right - 1 } : undefined, windowTone: 'primary',
        stats: stats(), set: [...set], line: 4, predict,
        caption: `'${c}' is already in the window. Remove '${gone}' from the left side. L moves to index ${left}.`,
      })
    }
    const predict = mcq(askText(), addLabel, left < right ? [`Remove '${chars[left]}' from the left`] : ['Stop: the text is finished'],
      `'${c}' is not inside the window, so we can add it.`)
    set.add(c)
    const len = right - left + 1
    const better = len > best
    if (better) { best = len; bestFrom = left }
    steps.push({
      values: chars, tones: tonesFor(left, right, right, 'good'),
      pointers: [{ index: left, label: 'L', tone: 'primary' }, { index: right, label: 'R', tone: 'accent' }],
      window: { from: left, to: right }, windowTone: 'primary', stats: stats(), set: [...set], line: 7, predict,
      caption: `'${c}' is new in the window. Add it. Window "${chars.slice(left, right + 1).join('')}" has length ${len}.`
        + (better ? ` New best: ${len}!` : ` Best stays ${best}.`),
    })
  }
  const answer = chars.slice(bestFrom, bestFrom + best).join('')
  steps.push({
    values: chars, tones: chars.map((_, j) => (j >= bestFrom && j < bestFrom + best ? 'good' : 'muted')), pointers: [],
    window: { from: bestFrom, to: bestFrom + best - 1 }, windowTone: 'gold',
    stats: [{ label: 'Best length', value: best, tone: 'gold' }], set: [], line: 8,
    caption: `Done! The longest part with no repeats is "${answer}", length ${best}. Each letter enters and leaves the window at most once: O(n).`,
  })
  return steps
}

function parseText(text: string): Parsed<string> {
  const t = text.trim().toLowerCase()
  if (!/^[a-z]{1,12}$/.test(t)) return { error: 'Use 1 to 12 letters (a to z), no spaces.' }
  return { value: t }
}

export default function SlidingWindowViz() {
  const [mode, setMode] = useState<Mode>('fixed')
  const [arr, setArr] = useState<number[]>([2, 1, 5, 1, 3, 2, 6])
  const [kRaw, setK] = useState(3)
  const [text, setText] = useState('abcabcbb')
  const k = clamp(kRaw, 1, arr.length)

  const steps = useMemo(() => (mode === 'fixed' ? fixedSteps(arr, k) : variableSteps(text)), [mode, arr, k, text])

  return (
    <div className="space-y-4">
      <Tabs tabs={MODES} value={mode} onChange={setMode} />
      <InputPanel>
        {mode === 'fixed' ? (
          <>
            <DraftField label="Array values" initial={arr.join(', ')} hint="2 to 8 numbers, from -99 to 99"
              parse={(s) => parseNumberList(s, { minLen: 2, maxLen: 8 })} onApply={setArr} />
            <Stepper label="Window size k" value={k} min={1} max={arr.length} onChange={setK} />
          </>
        ) : (
          <DraftField label="Text" initial={text} hint="1 to 12 letters, like: abcabcbb" parse={parseText} onApply={setText} />
        )}
      </InputPanel>
      <StepPlayer steps={steps} label={mode === 'fixed' ? 'Max sum window' : 'Longest substring without repeats'}
        code={mode === 'fixed' ? FIXED_CODE : VARIABLE_CODE}
        render={(s) => (
          <>
            <ArrayRow values={s.values} tones={s.tones} pointers={s.pointers}
              range={s.window ? { ...s.window, label: 'window', tone: s.windowTone ?? 'primary' } : undefined}
              ariaLabel={`Values ${s.values.join(', ')}${s.window ? `. Window from index ${s.window.from} to ${s.window.to}` : ''}`} />
            <Stats items={s.stats} />
            {s.set && mode === 'variable' && (
              <p className="text-center mt-2 text-muted">
                Set: <span className="font-mono text-lg text-ink">{'{ '}{s.set.join(', ')}{' }'}</span>
              </p>
            )}
          </>
        )} />
    </div>
  )
}
