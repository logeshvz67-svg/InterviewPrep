import { useMemo, useState } from 'react'
import { Tabs } from '../ui'
import { ArrayRow } from './draw'
import { DraftField, InputPanel, Stepper } from './fields'
import { clamp, mcq, parseNumber, parseNumberList } from './lib'
import { StepPlayer } from './StepPlayer'
import type { BaseStep, Pointer, Tone } from './types'

/**
 * ARRAY OPERATIONS
 * An array is a row of boxes next to each other. Each box has an index (0, 1, 2...).
 *  - Traverse: visit every box from left to right.                     O(n)
 *  - Insert at index k: move every value from k onward one box RIGHT,
 *    starting from the end, then put the new value in box k.            O(n)
 *  - Delete at index k: move every value after k one box LEFT,
 *    then remove the last (now extra) box.                              O(n)
 *  - Linear search: check boxes one by one until we find the target.   O(n)
 */

type Mode = 'traverse' | 'insert' | 'delete' | 'search'
const MODES: { id: Mode; label: string }[] = [
  { id: 'traverse', label: '👣 Traverse' }, { id: 'insert', label: '➕ Insert' },
  { id: 'delete', label: '➖ Delete' }, { id: 'search', label: '🔍 Search' },
]

interface ArrStep extends BaseStep {
  values: (number | null)[]
  tones: Tone[]
  pointers: Pointer[]
}

const otherValues = (a: number[], skip: number) => a.filter((v) => v !== skip).map(String)

function traverseSteps(a: number[]): ArrStep[] {
  const n = a.length
  const steps: ArrStep[] = [{
    values: a, tones: a.map(() => 'normal'), pointers: [],
    caption: `This is our array. It has ${n} box${n === 1 ? '' : 'es'}. We will visit every box, from left to right.`,
  }]
  a.forEach((v, i) => {
    steps.push({
      values: a,
      tones: a.map((_, j) => (j < i ? 'done' : j === i ? 'active' : 'normal')),
      pointers: [{ index: i, label: 'i' }],
      caption: `Visit index ${i}. The value here is ${v}.`,
      predict: mcq(i === 0 ? 'Where do we start?' : `We just visited index ${i - 1}. Which index comes next?`,
        `Index ${i}`, [i - 1, i + 1, i + 2, n].filter((x) => x >= 0 && x !== i).map((x) => `Index ${x}`),
        'We always move one box to the right: i = i + 1.'),
    })
  })
  steps.push({
    values: a, tones: a.map(() => 'done'), pointers: [],
    caption: `The end. We visited all ${n} boxes, one time each. This takes O(n) time.`,
    predict: mcq(`We visited the last box (index ${n - 1}). What happens now?`, 'We stop: no boxes are left',
      ['We go back to index 0', `We visit index ${n}`], `Index ${n} does not exist. The last index is always n - 1.`),
  })
  return steps
}

function insertSteps(a: number[], k: number, x: number): ArrStep[] {
  const n = a.length
  let cur: (number | null)[] = [...a, null]
  const steps: ArrStep[] = [{
    values: cur,
    tones: cur.map((_, j) => (j === k ? 'gold' : 'normal')),
    pointers: [{ index: k, label: 'insert here', tone: 'gold' }],
    caption: `We want to put ${x} at index ${k}. The array needs one more box, so we add an empty box at the end.`,
  }]
  // Shift right, starting from the END, so we never overwrite a value we still need.
  for (let j = n - 1; j >= k; j--) {
    cur = [...cur]
    cur[j + 1] = cur[j]
    steps.push({
      values: cur,
      tones: cur.map((_, t) => (t === j + 1 ? 'good' : t === j ? 'active' : t > j + 1 ? 'done' : 'normal')),
      pointers: [{ index: j, label: 'from' }, { index: j + 1, label: 'to', tone: 'good' }],
      caption: `Move ${a[j]} from index ${j} to index ${j + 1} (one box to the right).`,
      predict: mcq('Which value moves one box to the right next?', String(a[j]), [...otherValues(a, a[j]), String(x)],
        'We start at the right end and walk left. That way we never overwrite a value we still need.'),
    })
  }
  cur = [...cur]
  cur[k] = x
  steps.push({
    values: cur,
    tones: cur.map((_, t) => (t === k ? 'good' : 'normal')),
    pointers: [{ index: k, label: 'new', tone: 'good' }],
    caption: `Box ${k} is free now. Put ${x} in it.`,
    predict: mcq(k === n ? `The new box is at index ${k}. What do we do?` : `All values from index ${k} moved right. What do we do now?`,
      `Put ${x} at index ${k}`, ['Move another value right', `Delete index ${k}`]),
  })
  steps.push({
    values: cur, tones: cur.map(() => 'done'), pointers: [],
    caption: `Done! The array now has ${n + 1} values. We moved ${n - k} value${n - k === 1 ? '' : 's'}. In the worst case (index 0) we move all of them: O(n).`,
  })
  return steps
}

function deleteSteps(a: number[], k: number): ArrStep[] {
  const n = a.length
  let cur: (number | null)[] = [...a]
  const steps: ArrStep[] = [{
    values: cur,
    tones: cur.map((_, j) => (j === k ? 'bad' : 'normal')),
    pointers: [{ index: k, label: 'delete', tone: 'bad' }],
    caption: k === n - 1
      ? `We want to remove ${a[k]} at index ${k}. It is the last box, so nothing needs to move.`
      : `We want to remove ${a[k]} at index ${k}. The values after it must move one box LEFT to fill the gap.`,
  }]
  // Shift left, starting right after the deleted box.
  for (let j = k; j < n - 1; j++) {
    cur = [...cur]
    cur[j] = cur[j + 1]
    steps.push({
      values: cur,
      tones: cur.map((_, t) => (t === j ? 'good' : t === j + 1 ? 'active' : t < j && t >= k ? 'done' : 'normal')),
      pointers: [{ index: j + 1, label: 'from' }, { index: j, label: 'to', tone: 'good' }],
      caption: `Copy ${a[j + 1]} from index ${j + 1} to index ${j} (one box to the left).`,
      predict: mcq('Which value moves one box to the left next?', String(a[j + 1]), otherValues(a, a[j + 1]),
        'We go from left to right, filling the gap one box at a time.'),
    })
  }
  cur = [...cur]
  cur[n - 1] = null
  steps.push({
    values: cur,
    tones: cur.map((_, t) => (t >= k && t < n - 1 ? 'done' : 'normal')),
    pointers: [{ index: n - 1, label: 'extra', tone: 'bad' }],
    caption: `The last box (index ${n - 1}) is extra now. Remove it. The size becomes ${n - 1}.`,
    predict: mcq('The values after the gap all moved left. What about the last box?', 'It is extra: remove it',
      ['Put the deleted value back in it', 'Copy index 0 into it']),
  })
  const final = cur.slice(0, n - 1)
  steps.push({
    values: final, tones: final.map(() => 'done'), pointers: [],
    caption: `Done! ${a[k]} is gone. We moved ${n - 1 - k} value${n - 1 - k === 1 ? '' : 's'}. Worst case (index 0): O(n).`,
  })
  return steps
}

function searchSteps(a: number[], t: number): ArrStep[] {
  const n = a.length
  const steps: ArrStep[] = [{
    values: a, tones: a.map(() => 'normal'), pointers: [],
    caption: `We look for ${t}. We check the boxes one by one, from the left.`,
  }]
  for (let i = 0; i < n; i++) {
    const found = a[i] === t
    steps.push({
      values: a,
      tones: a.map((_, j) => (j < i ? 'muted' : j === i ? (found ? 'good' : 'bad') : 'normal')),
      pointers: [{ index: i, label: 'i', tone: found ? 'good' : 'primary' }],
      caption: found ? `Index ${i} has ${a[i]}. That equals ${t}. Found it!` : `Index ${i} has ${a[i]}. That is not ${t}. Move on.`,
      predict: mcq(`Index ${i} has ${a[i]}. Is it the value we want (${t})?`,
        found ? 'Yes: found it, stop' : 'No: check the next box', [found ? 'No: check the next box' : 'Yes: found it, stop']),
    })
    if (found) {
      steps.push({
        values: a, tones: a.map((_, j) => (j === i ? 'good' : 'muted')), pointers: [{ index: i, label: 'answer', tone: 'good' }],
        caption: `Answer: index ${i}. We stopped early, after ${i + 1} check${i === 0 ? '' : 's'}. The worst case checks all n boxes: O(n).`,
      })
      return steps
    }
  }
  steps.push({
    values: a, tones: a.map(() => 'muted'), pointers: [],
    caption: `We checked all ${n} boxes. ${t} is not in the array. Return -1.`,
    predict: mcq(`We checked the last box and did not find ${t}. What do we return?`, '-1 (not found)', ['0', String(n), String(n - 1)],
      '-1 is the usual answer for "not found", because no real index is negative.'),
  })
  return steps
}

export default function ArrayViz() {
  const [mode, setMode] = useState<Mode>('traverse')
  const [arr, setArr] = useState<number[]>([7, 3, 9, 4, 6])
  const [index, setIndex] = useState(2)
  const [value, setValue] = useState(5)
  const [target, setTarget] = useState(4)

  const n = arr.length
  // Insert may use index 0..n; delete may use 0..n-1.
  const maxIndex = mode === 'insert' ? n : n - 1
  const k = clamp(index, 0, maxIndex)

  const steps = useMemo(() => {
    switch (mode) {
      case 'traverse': return traverseSteps(arr)
      case 'insert': return insertSteps(arr, k, value)
      case 'delete': return deleteSteps(arr, k)
      case 'search': return searchSteps(arr, target)
    }
  }, [mode, arr, k, value, target])

  return (
    <div className="space-y-4">
      <Tabs tabs={MODES} value={mode} onChange={setMode} />
      <InputPanel>
        <DraftField label="Array values" initial={arr.join(', ')} hint="1 to 7 numbers, from -99 to 99, like: 7, 3, 9"
          parse={(s) => parseNumberList(s, { minLen: 1, maxLen: 7 })} onApply={setArr} />
        {(mode === 'insert' || mode === 'delete') && (
          <Stepper label={mode === 'insert' ? 'Insert at index' : 'Delete index'} value={k} min={0} max={maxIndex} onChange={setIndex} />
        )}
        {mode === 'insert' && (
          <DraftField label="Value to insert" initial={String(value)} inputMode="numeric" hint="From -99 to 99"
            parse={(s) => parseNumber(s, -99, 99)} onApply={setValue} />
        )}
        {mode === 'search' && (
          <DraftField label="Target to find" initial={String(target)} inputMode="numeric" hint="From -99 to 99"
            parse={(s) => parseNumber(s, -99, 99)} onApply={setTarget} />
        )}
      </InputPanel>
      <StepPlayer steps={steps} label={`Array ${mode}`}
        render={(s) => <ArrayRow values={s.values} tones={s.tones} pointers={s.pointers}
          ariaLabel={`Array: ${s.values.map((v) => v ?? 'empty').join(', ')}`} />} />
    </div>
  )
}
