import { useMemo, useState } from 'react'
import { Tabs } from '../ui'
import { ArrayRow } from './draw'
import { DraftField, InputPanel, Stats } from './fields'
import { mcq, p, parseNumber, parseNumberList } from './lib'
import { StepPlayer } from './StepPlayer'
import type { BaseStep, Pointer, Tone } from './types'

/**
 * TWO POINTERS
 * Two arrows (L and R) start at the two ends of the array and walk toward each other.
 *  - Pair with target sum (array must be SORTED):
 *      sum = a[L] + a[R]. Too small -> move L right (bigger numbers). Too big -> move R left (smaller numbers).
 *      Equal -> found. Every step throws away one box, so it is O(n) instead of O(n²).
 *  - Reverse in place: swap a[L] and a[R], then move both inward, until they meet.  O(n) time, O(1) space.
 */

type Mode = 'pair' | 'reverse'
const MODES: { id: Mode; label: string }[] = [{ id: 'pair', label: '🎯 Pair with target sum' }, { id: 'reverse', label: '🔄 Reverse in place' }]

interface TPStep extends BaseStep {
  values: number[]
  tones: Tone[]
  L: number
  R: number
  sum?: number
}

const PAIR_CODE = [
  'int left = 0, right = n - 1;',
  'while (left < right) {',
  '    int sum = a[left] + a[right];',
  '    if (sum == target) return new int[]{left, right};',
  '    else if (sum < target) left++;',
  '    else right--;',
  '}',
  'return null; // no pair',
]

const REVERSE_CODE = [
  'int left = 0, right = n - 1;',
  'while (left < right) {',
  '    int temp = a[left];',
  '    a[left] = a[right];',
  '    a[right] = temp;',
  '    left++; right--;',
  '}',
]

const SMALL = 'Sum is too small: move L right'
const BIG = 'Sum is too big: move R left'
const EQUAL = 'Sum equals the target: found!'

function pairSteps(a: number[], t: number): TPStep[] {
  const n = a.length
  // Boxes outside [L, R] are already ruled out, so we fade them.
  const tones = (L: number, R: number, hit?: Tone): Tone[] =>
    a.map((_, j) => (j === L || j === R ? hit ?? 'active' : j < L || j > R ? 'muted' : 'normal'))
  let L = 0
  let R = n - 1
  const steps: TPStep[] = [{
    values: a, tones: tones(L, R), L, R, line: 0,
    caption: `The array is sorted. L starts at the left end, R at the right end. We want two numbers that add up to ${t}.`,
  }]
  while (L < R) {
    const sum = a[L] + a[R]
    const verdict = sum === t ? EQUAL : sum < t ? SMALL : BIG
    const ask = mcq(`L points to ${a[L]}, R points to ${a[R]}. The target is ${t}. What do we do?`, verdict, [SMALL, BIG, EQUAL],
      'Sorted array: moving L right makes the sum bigger, moving R left makes it smaller.')
    if (sum === t) {
      steps.push({
        values: a, tones: tones(L, R, 'good'), L, R, sum, line: 3, predict: ask,
        caption: `${p(a[L])} + ${p(a[R])} = ${sum}. That is the target! The pair is at index ${L} and ${R}. Each step moved one pointer, so this is O(n).`,
      })
      return steps
    }
    steps.push({
      values: a, tones: tones(L, R), L, R, sum, line: sum < t ? 4 : 5, predict: ask,
      caption: sum < t
        ? `${p(a[L])} + ${p(a[R])} = ${sum}. That is smaller than ${t}. We need a bigger sum, so L moves right.`
        : `${p(a[L])} + ${p(a[R])} = ${sum}. That is bigger than ${t}. We need a smaller sum, so R moves left.`,
    })
    if (sum < t) L++
    else R--
    steps.push({
      values: a, tones: tones(L, R), L, R, line: 1,
      caption: sum < t ? `L moves to index ${L} (value ${a[L]}).` : `R moves to index ${R} (value ${a[R]}).`,
    })
  }
  steps.push({
    values: a, tones: a.map(() => 'muted'), L, R, line: 7,
    caption: `L and R met. No two numbers add up to ${t}. Return "no pair".`,
    predict: mcq('L and R are now on the same box. What happens?', 'Stop: there is no pair',
      ['Add the box to itself', 'Move L right again'], 'We need two DIFFERENT boxes, so the loop stops when left < right is false.'),
  })
  return steps
}

function reverseSteps(input: number[]): TPStep[] {
  const a = [...input]
  const n = a.length
  const tones = (L: number, R: number, mid: Tone = 'active'): Tone[] =>
    a.map((_, j) => (j === L || j === R ? mid : j < L || j > R ? 'done' : 'normal'))
  let L = 0
  let R = n - 1
  let swaps = 0
  const steps: TPStep[] = [{
    values: [...a], tones: tones(L, R), L, R, line: 0,
    caption: 'L starts at the left end, R at the right end. We swap the two ends and walk inward.',
  }]
  while (L < R) {
    const x = a[L]
    const y = a[R]
    a[L] = y
    a[R] = x
    swaps++
    steps.push({
      values: [...a], tones: tones(L, R, 'good'), L, R, line: 3,
      caption: `Swap ${x} and ${y}. Now index ${L} has ${y} and index ${R} has ${x}.`,
      predict: mcq(`L is on ${x}, R is on ${y}. After the swap, what is at index ${L}?`, String(y),
        [String(x), ...input.map(String)], 'A swap exchanges the two values.'),
    })
    L++
    R--
    steps.push({
      values: [...a], tones: tones(L, R), L, R, line: 5,
      caption: L < R ? `Move L one step right (index ${L}) and R one step left (index ${R}).` : 'Move L right and R left. They met (or crossed) in the middle.',
    })
  }
  steps.push({
    values: [...a], tones: a.map(() => 'done'), L, R, line: 1,
    caption: `L is not less than R, so we stop. The array is reversed! ${swaps} swap${swaps === 1 ? '' : 's'}: O(n) time, O(1) extra space.`,
    predict: mcq(`L is at index ${L} and R is at index ${R}. Is L < R?`, 'No: stop, the array is reversed', ['Yes: swap again'],
      'When the pointers meet or cross, every pair has been swapped.'),
  })
  return steps
}

export default function TwoPointersViz() {
  const [mode, setMode] = useState<Mode>('pair')
  const [arr, setArr] = useState<number[]>([1, 3, 4, 6, 8, 11])
  const [target, setTarget] = useState(10)

  // Pair-sum needs a sorted array, so we sort a copy (and tell the learner).
  const sorted = useMemo(() => [...arr].sort((x, y) => x - y), [arr])
  const steps = useMemo(() => (mode === 'pair' ? pairSteps(sorted, target) : reverseSteps(arr)), [mode, sorted, arr, target])
  const wasSorted = sorted.every((v, i) => v === arr[i])

  return (
    <div className="space-y-4">
      <Tabs tabs={MODES} value={mode} onChange={setMode} />
      <InputPanel>
        <DraftField label="Array values" initial={arr.join(', ')} hint="2 to 8 numbers, from -99 to 99"
          parse={(s) => parseNumberList(s, { minLen: 2, maxLen: 8 })} onApply={setArr} />
        {mode === 'pair' && (
          <DraftField label="Target sum" initial={String(target)} inputMode="numeric" hint="From -198 to 198"
            parse={(s) => parseNumber(s, -198, 198)} onApply={setTarget} />
        )}
        {mode === 'pair' && !wasSorted && (
          <p className="text-sm text-warn self-center">We sorted your numbers first. Two pointers for a pair sum only works on a sorted array.</p>
        )}
      </InputPanel>
      <StepPlayer steps={steps} label={mode === 'pair' ? 'Pair with target sum' : 'Reverse array'}
        code={mode === 'pair' ? PAIR_CODE : REVERSE_CODE}
        render={(s) => {
          const ptrs: Pointer[] = []
          if (s.L < s.values.length) ptrs.push({ index: s.L, label: 'L', tone: 'primary' })
          if (s.R >= 0) ptrs.push({ index: s.R, label: 'R', tone: 'accent' })
          return (
            <>
              <ArrayRow values={s.values} tones={s.tones} pointers={ptrs}
                ariaLabel={`Array ${s.values.join(', ')}. L at index ${s.L}, R at index ${s.R}.`} />
              {mode === 'pair' && (
                <Stats items={[
                  { label: 'Target', value: target, tone: 'gold' },
                  { label: 'Sum now', value: s.sum ?? '–', tone: s.sum === undefined ? undefined : s.sum === target ? 'good' : 'primary' },
                ]} />
              )}
            </>
          )
        }} />
    </div>
  )
}
