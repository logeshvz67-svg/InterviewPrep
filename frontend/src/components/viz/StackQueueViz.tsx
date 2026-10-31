import { useMemo, useState } from 'react'
import { Tabs } from '../ui'
import { ArrayRow, StackColumn } from './draw'
import { DraftField, InputPanel } from './fields'
import { mcq, type Parsed } from './lib'
import { StepPlayer } from './StepPlayer'
import type { BaseStep, Pointer, Tone } from './types'

/**
 * STACK and QUEUE
 *  - Stack = a pile of plates. Add (push) and remove (pop) only at the TOP. Last In, First Out (LIFO).
 *    peek = look at the top without removing it. All O(1).
 *  - Queue = a line at a shop. Join at the REAR (enqueue), leave from the FRONT (dequeue).
 *    First In, First Out (FIFO). All O(1).
 *  - Balanced brackets: push every opening bracket. For a closing bracket, the top of the stack
 *    must be its matching opening bracket (then pop). At the end the stack must be empty.
 */

type Mode = 'stack' | 'queue' | 'brackets'
const MODES: { id: Mode; label: string }[] = [
  { id: 'stack', label: '🥞 Stack' }, { id: 'queue', label: '🚶 Queue' }, { id: 'brackets', label: '🧩 Balanced brackets' },
]

const MAX_SIZE = 6

interface Op { kind: 'add' | 'remove' | 'peek'; value?: number }

interface SQStep extends BaseStep {
  items: (number | string)[]
  tones: Tone[]
  opIndex: number // which operation (or character) we are on; -1 = none
  result?: string
}

/** Read "push 3, pop, peek" (stack) or "enqueue 3, dequeue, peek" (queue). */
function parseOps(text: string, mode: 'stack' | 'queue'): Parsed<Op[]> {
  const parts = text.split(/[,;\n]+/).map((s) => s.trim().toLowerCase()).filter(Boolean)
  if (parts.length === 0) return { error: 'Type at least one operation.' }
  if (parts.length > 10) return { error: 'Use at most 10 operations.' }
  const addWords = mode === 'stack' ? ['push'] : ['enqueue', 'enq', 'add', 'offer']
  const removeWords = mode === 'stack' ? ['pop'] : ['dequeue', 'deq', 'poll', 'remove']
  const ops: Op[] = []
  let size = 0
  for (const part of parts) {
    const m = part.match(/^([a-z]+)\s*\(?\s*(-?\d+)?\s*\)?$/)
    if (!m) return { error: `I do not understand "${part}".` }
    const [, word, num] = m
    if (addWords.includes(word)) {
      if (num === undefined) return { error: `"${part}" needs a number, like ${addWords[0]} 5.` }
      const v = Number(num)
      if (v < -99 || v > 99) return { error: 'Numbers must be between -99 and 99.' }
      if (++size > MAX_SIZE) return { error: `Keep at most ${MAX_SIZE} items inside, so the picture fits.` }
      ops.push({ kind: 'add', value: v })
    } else if (removeWords.includes(word)) { ops.push({ kind: 'remove' }); size = Math.max(0, size - 1) }
    else if (word === 'peek' || word === 'front' || word === 'top') ops.push({ kind: 'peek' })
    else return { error: `Unknown operation "${word}". Use ${addWords[0]}, ${removeWords[0]} or peek.` }
  }
  return { value: ops }
}

const opText = (op: Op, mode: 'stack' | 'queue') =>
  op.kind === 'add' ? `${mode === 'stack' ? 'push' : 'enqueue'}(${op.value})` : op.kind === 'remove' ? (mode === 'stack' ? 'pop()' : 'dequeue()') : 'peek()'

/** Back to the text the learner types: "push 3, pop". */
const opsToText = (ops: Op[], mode: 'stack' | 'queue') =>
  ops.map((o) => opText(o, mode).replace(/\((-?\d*)\)/, (_m, num: string) => (num ? ` ${num}` : ''))).join(', ')

function stackSteps(ops: Op[]): SQStep[] {
  const st: number[] = []
  const steps: SQStep[] = [{ items: [], tones: [], opIndex: -1, caption: 'A stack is like a pile of plates. We add and remove only at the TOP. Last In, First Out.' }]
  ops.forEach((op, i) => {
    if (op.kind === 'add') {
      st.push(op.value as number)
      steps.push({
        items: [...st], tones: st.map((_, j) => (j === st.length - 1 ? 'good' : 'normal')), opIndex: i,
        caption: `push(${op.value}): put ${op.value} on the top. Size is now ${st.length}.`,
        predict: mcq(`push(${op.value}): where does ${op.value} go?`, 'On the top', ['At the bottom', 'In the middle']),
      })
    } else if (st.length === 0) {
      steps.push({
        items: [], tones: [], opIndex: i, result: 'error',
        caption: `${op.kind === 'remove' ? 'pop()' : 'peek()'} on an EMPTY stack! There is nothing there. In Java this throws an exception.`,
        predict: mcq(`${op.kind === 'remove' ? 'pop()' : 'peek()'}: what do we get?`, 'Nothing: the stack is empty (error)', ['0', 'The bottom item']),
      })
    } else if (op.kind === 'remove') {
      const top = st[st.length - 1]
      steps.push({
        items: [...st], tones: st.map((_, j) => (j === st.length - 1 ? 'bad' : 'normal')), opIndex: i, result: String(top),
        caption: `pop(): take ${top} off the top. It is removed and returned.`,
        predict: mcq('pop(): which value comes out?', String(top), [...st.slice(0, -1).reverse().map(String), 'Nothing: the stack is empty'],
          'Only the top can come out. The last one in is the first one out.'),
      })
      st.pop()
    } else {
      const top = st[st.length - 1]
      steps.push({
        items: [...st], tones: st.map((_, j) => (j === st.length - 1 ? 'gold' : 'normal')), opIndex: i, result: String(top),
        caption: `peek(): look at the top value, ${top}. We do NOT remove it.`,
        predict: mcq('peek(): what happens?', `We get ${top}, and it stays`, [`We get ${top}, and it is removed`, `We get ${st[0]}, and it stays`]),
      })
    }
  })
  steps.push({
    items: [...st], tones: st.map(() => 'done'), opIndex: ops.length,
    caption: st.length ? `All done. The stack has ${st.length} item${st.length === 1 ? '' : 's'}. The top is ${st[st.length - 1]}. Every operation was O(1).` : 'All done. The stack is empty. Every operation was O(1).',
  })
  return steps
}

function queueSteps(ops: Op[]): SQStep[] {
  const q: number[] = []
  const steps: SQStep[] = [{ items: [], tones: [], opIndex: -1, caption: 'A queue is like a line at a shop. People join at the REAR and leave from the FRONT. First In, First Out.' }]
  ops.forEach((op, i) => {
    if (op.kind === 'add') {
      q.push(op.value as number)
      steps.push({
        items: [...q], tones: q.map((_, j) => (j === q.length - 1 ? 'good' : 'normal')), opIndex: i,
        caption: `enqueue(${op.value}): ${op.value} joins at the rear (the back). Size is now ${q.length}.`,
        predict: mcq(`enqueue(${op.value}): where does ${op.value} go?`, 'At the rear (back)', ['At the front', 'In the middle']),
      })
    } else if (q.length === 0) {
      steps.push({
        items: [], tones: [], opIndex: i, result: 'error',
        caption: `${op.kind === 'remove' ? 'dequeue()' : 'peek()'} on an EMPTY queue! Nobody is in the line. (Java's poll() returns null here.)`,
        predict: mcq(`${op.kind === 'remove' ? 'dequeue()' : 'peek()'}: what do we get?`, 'Nothing: the queue is empty', ['0', 'The rear item']),
      })
    } else if (op.kind === 'remove') {
      const front = q[0]
      steps.push({
        items: [...q], tones: q.map((_, j) => (j === 0 ? 'bad' : 'normal')), opIndex: i, result: String(front),
        caption: `dequeue(): ${front} leaves from the front. It was waiting the longest.`,
        predict: mcq('dequeue(): which value leaves?', String(front), [String(q[q.length - 1]), ...q.slice(1).map(String), 'Nothing: the queue is empty'],
          'The front leaves first. First in, first out.'),
      })
      q.shift()
    } else {
      steps.push({
        items: [...q], tones: q.map((_, j) => (j === 0 ? 'gold' : 'normal')), opIndex: i, result: String(q[0]),
        caption: `peek(): look at the front value, ${q[0]}. It stays in the queue.`,
        predict: mcq('peek(): which value do we see?', String(q[0]), [String(q[q.length - 1]), ...q.map(String), 'Nothing: the queue is empty'], 'peek looks at the FRONT.'),
      })
    }
  })
  steps.push({
    items: [...q], tones: q.map(() => 'done'), opIndex: ops.length,
    caption: q.length ? `All done. ${q.length} item${q.length === 1 ? '' : 's'} in the line. Front: ${q[0]}, rear: ${q[q.length - 1]}. Every operation was O(1).` : 'All done. The queue is empty. Every operation was O(1).',
  })
  return steps
}

const PAIRS: Record<string, string> = { ')': '(', ']': '[', '}': '{' }
const PUSH = 'Push it on the stack'
const POP = 'Pop: it matches the top'
const FAIL = 'Stop: NOT balanced'

function bracketSteps(text: string): SQStep[] {
  const st: string[] = []
  const steps: SQStep[] = [{
    items: [], tones: [], opIndex: -1,
    caption: 'Open brackets go on a stack. A closing bracket must match the TOP of the stack. At the end, the stack must be empty.',
  }]
  for (let i = 0; i < text.length; i++) {
    const c = text[i]
    const top = st[st.length - 1]
    const ask = (right: string, why: string) =>
      mcq(`Next character: '${c}'. Stack top: ${top ? `'${top}'` : 'empty'}. What do we do?`, right, [PUSH, POP, FAIL], why)
    if (!PAIRS[c]) {
      st.push(c)
      steps.push({ items: [...st], tones: st.map((_, j) => (j === st.length - 1 ? 'good' : 'normal')), opIndex: i,
        caption: `'${c}' is an opening bracket. Push it on the stack.`, predict: ask(PUSH, 'Opening brackets always go on the stack.') })
    } else if (top === undefined) {
      steps.push({ items: [], tones: [], opIndex: i, result: 'bad',
        caption: `'${c}' is a closing bracket, but the stack is empty. Nothing to match. NOT balanced.`,
        predict: ask(FAIL, 'A closing bracket with nothing open before it can never match.') })
      return steps
    } else if (top === PAIRS[c]) {
      steps.push({ items: [...st], tones: st.map((_, j) => (j === st.length - 1 ? 'good' : 'normal')), opIndex: i,
        caption: `'${c}' closes '${top}'. They match! Pop '${top}'.`, predict: ask(POP, `'${top}' and '${c}' are a pair.`) })
      st.pop()
    } else {
      steps.push({ items: [...st], tones: st.map((_, j) => (j === st.length - 1 ? 'bad' : 'normal')), opIndex: i, result: 'bad',
        caption: `'${c}' does not match the top '${top}'. NOT balanced.`, predict: ask(FAIL, `'${c}' needs '${PAIRS[c]}' on top, but the top is '${top}'.`) })
      return steps
    }
  }
  const ok = st.length === 0
  steps.push({
    items: [...st], tones: st.map(() => 'bad'), opIndex: text.length, result: ok ? 'good' : 'bad',
    caption: ok ? 'The text is finished and the stack is empty. Balanced! O(n) time.' : `The text is finished, but ${st.length} bracket${st.length === 1 ? ' is' : 's are'} still open. NOT balanced.`,
    predict: mcq(`We reached the end. The stack has ${st.length} item${st.length === 1 ? '' : 's'}. Is it balanced?`,
      ok ? 'Yes: balanced' : 'No: not balanced', [ok ? 'No: not balanced' : 'Yes: balanced'], 'Balanced means every bracket was closed, so the stack must be empty.'),
  })
  return steps
}

function parseBrackets(text: string): Parsed<string> {
  const t = text.replace(/\s+/g, '')
  if (!/^[()[\]{}]{1,14}$/.test(t)) return { error: 'Use 1 to 14 brackets: ( ) [ ] { }' }
  return { value: t }
}

export default function StackQueueViz() {
  const [mode, setMode] = useState<Mode>('stack')
  const [stackOps, setStackOps] = useState<Op[]>(() => (parseOps('push 3, push 8, push 5, peek, pop, push 1, pop, pop', 'stack') as { value: Op[] }).value)
  const [queueOps, setQueueOps] = useState<Op[]>(() => (parseOps('enqueue 3, enqueue 8, enqueue 5, dequeue, peek, enqueue 1, dequeue', 'queue') as { value: Op[] }).value)
  const [brackets, setBrackets] = useState('{[()]}(')

  const steps = useMemo(() => (mode === 'stack' ? stackSteps(stackOps) : mode === 'queue' ? queueSteps(queueOps) : bracketSteps(brackets)),
    [mode, stackOps, queueOps, brackets])
  const ops = mode === 'stack' ? stackOps : queueOps
  const capacity = mode === 'brackets' ? Math.min(14, Math.max(3, Math.ceil(brackets.length / 2) + 1)) : MAX_SIZE

  return (
    <div className="space-y-4">
      <Tabs tabs={MODES} value={mode} onChange={setMode} />
      <InputPanel>
        {mode === 'stack' && (
          <DraftField key="s" label="Stack operations" initial={opsToText(stackOps, 'stack')}
            hint="Use push N, pop, peek. Up to 10, comma separated." parse={(s) => parseOps(s, 'stack')} onApply={setStackOps} />
        )}
        {mode === 'queue' && (
          <DraftField key="q" label="Queue operations" initial={opsToText(queueOps, 'queue')}
            hint="Use enqueue N, dequeue, peek. Up to 10, comma separated." parse={(s) => parseOps(s, 'queue')} onApply={setQueueOps} />
        )}
        {mode === 'brackets' && (
          <DraftField key="b" label="Brackets" initial={brackets} hint="1 to 14 of ( ) [ ] { }, like {[()]}" parse={parseBrackets} onApply={setBrackets} />
        )}
      </InputPanel>
      <StepPlayer steps={steps} label={mode === 'brackets' ? 'Balanced brackets' : mode}
        render={(s) => (
          <div className="space-y-3">
            {/* The list of operations (or characters), current one highlighted */}
            {mode === 'brackets' ? (
              <ArrayRow values={brackets.split('')}
                tones={brackets.split('').map((_, i) => (i < s.opIndex ? 'done' : i === s.opIndex ? (s.result === 'bad' ? 'bad' : 'active') : 'normal'))}
                pointers={s.opIndex >= 0 && s.opIndex < brackets.length ? [{ index: s.opIndex, label: 'i' }] : []}
                ariaLabel={`Text ${brackets}`} />
            ) : (
              <ol className="flex flex-wrap gap-1.5 justify-center" aria-label="Operations">
                {ops.map((op, i) => (
                  <li key={i} aria-current={i === s.opIndex ? 'step' : undefined}
                    className={`chip font-mono text-sm px-3 py-1 ${i === s.opIndex ? 'chip-on' : i < s.opIndex ? 'opacity-50' : ''}`}>
                    {opText(op, mode === 'queue' ? 'queue' : 'stack')}
                  </li>
                ))}
              </ol>
            )}
            {mode === 'queue' ? (
              <QueueRow items={s.items} tones={s.tones} />
            ) : (
              <StackColumn items={s.items} tones={s.tones} capacity={capacity} ariaLabel={`Stack, bottom to top: ${s.items.join(', ') || 'empty'}`} />
            )}
            {s.result && mode !== 'brackets' && (
              <p className="text-center text-lg">Returned: <b className={`font-mono ${s.result === 'error' ? 'text-bad' : 'text-gold'}`}>{s.result === 'error' ? 'error (empty)' : s.result}</b></p>
            )}
            {mode === 'brackets' && (s.result === 'good' || s.result === 'bad') && (
              <p className={`text-center text-xl font-bold ${s.result === 'good' ? 'text-good' : 'text-bad'}`}>{s.result === 'good' ? '✓ Balanced' : '✗ Not balanced'}</p>
            )}
          </div>
        )} />
    </div>
  )
}

function QueueRow({ items, tones }: { items: (number | string)[]; tones: Tone[] }) {
  if (items.length === 0) return <p className="text-center text-muted text-lg py-6">(the queue is empty)</p>
  const pointers: Pointer[] = [{ index: 0, label: 'front', tone: 'primary' }, { index: items.length - 1, label: 'rear', tone: 'accent' }]
  return (
    <div>
      <p className="text-center text-xs text-muted mb-1">← leave here · · · join here →</p>
      <ArrayRow values={items} tones={tones} pointers={pointers} showIndex={false}
        ariaLabel={`Queue, front to rear: ${items.join(', ')}`} />
    </div>
  )
}
