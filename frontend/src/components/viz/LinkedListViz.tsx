import { useId, useMemo, useState } from 'react'
import { Tabs } from '../ui'
import { MOVE, POINTER_TEXT, TONE_BOX } from './draw'
import { DraftField, InputPanel, Stepper } from './fields'
import { clamp, mcq, parseNumber, parseNumberList } from './lib'
import { StepPlayer } from './StepPlayer'
import type { BaseStep, PointerTone, Tone } from './types'

/**
 * LINKED LIST
 * Each node has a value and a "next" link to the following node. `head` points to the first node.
 * The last node's next is null.
 *  - Insert at head: new.next = head; head = new.                         O(1)
 *  - Insert at position p: walk to node p-1 (call it curr), then
 *      new.next = curr.next FIRST, then curr.next = new.                   O(n)
 *  - Delete at position p: walk to node p-1, then curr.next = curr.next.next.   O(n)
 *  - Reverse: walk with three pointers (prev, curr, next) and turn every arrow around.  O(n)
 */

type Mode = 'insertHead' | 'insertAt' | 'delete' | 'reverse'
const MODES: { id: Mode; label: string }[] = [
  { id: 'insertHead', label: '⬅️ Insert at head' }, { id: 'insertAt', label: '➕ Insert at position' },
  { id: 'delete', label: '➖ Delete' }, { id: 'reverse', label: '🔄 Reverse' },
]

const NEW = 100 // id of the new node (old nodes use ids 0..n-1)

interface LLStep extends BaseStep {
  order: number[] // node ids, in the order they are drawn from left to right
  vals: Record<number, number>
  next: Record<number, number | null>
  head: number | null
  ptrs: { label: string; at: number | null; tone: PointerTone }[]
  tones: Record<number, Tone>
}

/** A tiny "world" we change step by step, taking a snapshot (copy) for each step. */
function makeWorld(values: number[]) {
  const order = values.map((_, i) => i)
  const vals: Record<number, number> = {}
  const next: Record<number, number | null> = {}
  values.forEach((v, i) => { vals[i] = v; next[i] = i + 1 < values.length ? i + 1 : null })
  const w = { order, vals, next, head: values.length ? 0 : null as number | null }
  const steps: LLStep[] = []
  const snap = (caption: string, extra: Partial<LLStep> = {}) => {
    steps.push({
      order: [...w.order], vals: { ...w.vals }, next: { ...w.next }, head: w.head, ptrs: [], tones: {}, caption, ...extra,
    })
  }
  return { w, steps, snap }
}

const valOf = (vals: Record<number, number>, id: number | null) => (id === null ? 'null' : String(vals[id]))

function insertHeadSteps(values: number[], x: number): LLStep[] {
  const { w, steps, snap } = makeWorld(values)
  snap('This is a linked list. Each node has a value and a link (next) to the next node. head points to the first node.')
  w.order.unshift(NEW); w.vals[NEW] = x; w.next[NEW] = null
  snap(`Make a new node with value ${x}. Its next is null for now.`, {
    tones: { [NEW]: 'gold' }, ptrs: [{ label: 'new', at: NEW, tone: 'gold' }],
    predict: mcq('To insert at the head, what do we do first?', `Make a new node with ${x}`, ['Move head to the last node', 'Delete the first node']),
  })
  w.next[NEW] = w.head
  snap(`Set new.next = head. The new node now points to ${valOf(w.vals, w.head)}.`, {
    tones: { [NEW]: 'gold' }, ptrs: [{ label: 'new', at: NEW, tone: 'gold' }],
    predict: mcq('The new node is ready. Which link do we set now?', 'new.next = head', ['head = null', 'head.next = new'],
      'Connect the new node to the old first node, so we do not lose the list.'),
  })
  w.head = NEW
  snap('Move head to the new node: head = new. Done! Insert at head is O(1): no walking needed.', {
    tones: { [NEW]: 'good' },
    predict: mcq('new.next points to the old first node. What is the last step?', 'head = new', ['new.next = null', 'head = head.next']),
  })
  return steps
}

/** Walk curr from head to the node at position `to`. Adds one step per move. */
function walk(w: ReturnType<typeof makeWorld>['w'], snap: ReturnType<typeof makeWorld>['snap'], to: number, why: string): number {
  let curr = w.head as number
  snap(`${why} Start with curr = head (${w.vals[curr]}).`, { ptrs: [{ label: 'curr', at: curr, tone: 'primary' }], tones: { [curr]: 'active' } })
  for (let i = 0; i < to; i++) {
    const before = curr
    curr = w.next[curr] as number
    snap(`Move curr one step: curr = curr.next. curr is now at ${w.vals[curr]}.`, {
      ptrs: [{ label: 'curr', at: curr, tone: 'primary' }], tones: { [curr]: 'active' },
      predict: mcq(`curr is at ${w.vals[before]}. Where is curr after curr = curr.next?`, `At ${w.vals[curr]}`,
        [`At ${w.vals[before]}`, `At ${w.vals[w.head as number]}`, 'At null'], 'curr.next is the node right after curr.'),
    })
  }
  return curr
}

function insertAtSteps(values: number[], pos: number, x: number): LLStep[] {
  if (pos === 0) return insertHeadSteps(values, x)
  const world = makeWorld(values)
  const { w, steps, snap } = world
  const curr = walk(w, snap, pos - 1, `We want ${x} at position ${pos} (counting from 0). We stop at the node just before it: position ${pos - 1}.`)
  const after = w.next[curr]
  w.order.splice(pos, 0, NEW); w.vals[NEW] = x; w.next[NEW] = null
  const ptrs = [{ label: 'curr', at: curr, tone: 'primary' as const }, { label: 'new', at: NEW, tone: 'gold' as const }]
  snap(`Make a new node with ${x}. It is not connected yet.`, { ptrs, tones: { [curr]: 'active', [NEW]: 'gold' } })
  w.next[NEW] = after
  snap(`Set new.next = curr.next. The new node now points to ${valOf(w.vals, after)}.`, {
    ptrs, tones: { [curr]: 'active', [NEW]: 'gold' },
    predict: mcq('Which link do we set FIRST?', 'new.next = curr.next', ['curr.next = new'],
      'If we change curr.next first, we lose the link to the rest of the list!'),
  })
  w.next[curr] = NEW
  snap(`Set curr.next = new. Done! ${x} is in the list. Walking to the spot takes O(n).`, {
    ptrs, tones: { [NEW]: 'good' },
    predict: mcq('new.next is set. What is the last step?', 'curr.next = new', ['head = new', 'new.next = null']),
  })
  return steps
}

function deleteSteps(values: number[], pos: number): LLStep[] {
  const { w, steps, snap } = makeWorld(values)
  if (pos === 0) {
    const old = w.head as number
    snap(`We want to delete the first node (${w.vals[old]}).`, { tones: { [old]: 'bad' } })
    w.head = w.next[old]
    snap('Move head to the next node: head = head.next.', {
      tones: { [old]: 'muted' },
      predict: mcq('How do we delete the first node?', 'head = head.next', ['head = null', 'head.next = null'],
        'head simply skips the first node.'),
    })
    w.order = w.order.filter((id) => id !== old)
    snap(`No link points to ${w.vals[old]} now, so Java's garbage collector cleans it up. Delete at head is O(1).`)
    return steps
  }
  const curr = walk(w, snap, pos - 1, `We want to delete the node at position ${pos}. We stop at the node just before it: position ${pos - 1}.`)
  const target = w.next[curr] as number
  const ptrs = [{ label: 'curr', at: curr, tone: 'primary' as const }]
  snap(`The node to delete is curr.next (${w.vals[target]}).`, { ptrs, tones: { [curr]: 'active', [target]: 'bad' } })
  w.next[curr] = w.next[target]
  snap(`Set curr.next = curr.next.next. The link now jumps over ${w.vals[target]}.`, {
    ptrs, tones: { [curr]: 'active', [target]: 'muted' },
    predict: mcq('How do we skip the node?', 'curr.next = curr.next.next', ['curr = curr.next', 'head = curr'],
      'Point curr past the node we delete.'),
  })
  w.order = w.order.filter((id) => id !== target)
  snap(`Nothing points to ${w.vals[target]} any more, so it is gone. Done! Walking to it took O(n).`, { ptrs })
  return steps
}

const REVERSE_CODE = [
  'Node prev = null, curr = head;',
  'while (curr != null) {',
  '    Node next = curr.next;',
  '    curr.next = prev;',
  '    prev = curr;',
  '    curr = next;',
  '}',
  'head = prev;',
]
const LINES = ['next = curr.next', 'curr.next = prev', 'prev = curr', 'curr = next']

function reverseSteps(values: number[]): LLStep[] {
  const { w, steps, snap } = makeWorld(values)
  let prev: number | null = null
  let curr: number | null = w.head
  let nxt: number | null = null
  const ptrs = () => [
    { label: 'prev', at: prev, tone: 'accent' as const }, { label: 'curr', at: curr, tone: 'primary' as const },
    { label: 'next', at: nxt, tone: 'gold' as const },
  ]
  const tones = (): Record<number, Tone> => (curr === null ? {} : { [curr]: 'active' })
  const ask = (k: number, why: string) => mcq('Which line runs next?', LINES[k], LINES.filter((_, i) => i !== k), why)
  snap('Start: prev = null, curr = head. We will turn every arrow around, one node at a time.', { ptrs: ptrs(), tones: tones(), line: 0 })
  while (curr !== null) {
    nxt = w.next[curr]
    snap(`Save next = curr.next (${valOf(w.vals, nxt)}). We must remember it before we change the arrow.`, {
      ptrs: ptrs(), tones: tones(), line: 2, predict: ask(0, 'curr is not null, so the loop runs. First we SAVE the next node.'),
    })
    w.next[curr] = prev
    snap(`Turn the arrow around: curr.next = prev (${valOf(w.vals, prev)}).`, {
      ptrs: ptrs(), tones: { [curr]: 'good' }, line: 3, predict: ask(1, 'next is saved, so now it is safe to change curr.next.'),
    })
    prev = curr
    snap(`Move prev forward: prev = curr (${valOf(w.vals, prev)}).`, {
      ptrs: ptrs(), tones: tones(), line: 4, predict: ask(2, 'prev moves first, because curr is about to move.'),
    })
    curr = nxt
    snap(`Move curr forward: curr = next (${valOf(w.vals, curr)}).`, {
      ptrs: ptrs(), tones: tones(), line: 5, predict: ask(3, 'curr jumps to the node we saved.'),
    })
  }
  w.head = prev
  snap('curr is null, so the loop stops. Set head = prev. The list is reversed! O(n) time, O(1) extra space.', {
    ptrs: ptrs(), line: 7,
    predict: mcq('curr is null now. What happens?', 'Stop the loop and set head = prev', ['Run the loop again', 'Set head = curr'],
      'prev is on the old last node, which is the new first node.'),
  })
  return steps
}

// ------------------------------------------------------------------ drawing

const W = 84 // node width (value part + next part)
const VW = 54 // value part width
const H = 52
const GAP = 52
const PAD = 16
const TOP = 74 // room above nodes for "jump" arrows

function ListScene({ step }: { step: LLStep }) {
  const markerId = useId().replace(/:/g, '')
  const pos: Record<number, number> = {}
  step.order.forEach((id, i) => { pos[id] = i })
  const x = (id: number) => PAD + pos[id] * (W + GAP)
  const n = step.order.length
  const width = Math.max(PAD * 2 + n * W + (n - 1) * GAP, 200)
  const allPtrs = [{ label: 'head', at: step.head, tone: 'good' as PointerTone }, ...step.ptrs]
  // Stack pointer labels under each node.
  const rowOf: Record<number, number> = {}
  const placed = allPtrs.filter((q) => q.at !== null && pos[q.at] !== undefined).map((q) => {
    const row = rowOf[q.at as number] ?? 0
    rowOf[q.at as number] = row + 1
    return { ...q, row }
  })
  const rows = Math.max(1, ...Object.values(rowOf))
  const ptrTop = TOP + H + 62
  const height = ptrTop + rows * 24 + 10
  const nullPtrs = allPtrs.filter((q) => q.at === null)

  return (
    <div>
      <svg viewBox={`0 0 ${width} ${height}`} className="block w-full h-auto mx-auto" style={{ maxWidth: width * 1.3 }}
        role="img" aria-label={`Linked list: ${describe(step)}`}>
        <defs>
          <marker id={markerId} viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
            <path d="M 0 0 L 10 5 L 0 10 z" className="fill-muted" />
          </marker>
        </defs>
        {/* links */}
        {step.order.map((id) => {
          const to = step.next[id]
          if (to === null || to === undefined || pos[to] === undefined) return null
          const sx = x(id) + VW + (W - VW) / 2 // middle of the "next" part
          const cy = TOP + H / 2
          const diff = pos[to] - pos[id]
          let d: string
          if (diff === 1) d = `M ${sx} ${cy} L ${x(to) - 2} ${cy}` // straight to the right neighbour
          else if (diff > 1) { const tx = x(to) + W / 2; d = `M ${sx} ${TOP} C ${sx} ${TOP - 55}, ${tx} ${TOP - 55}, ${tx} ${TOP - 2}` } // jump over (above)
          else { const tx = x(to) + W / 2; d = `M ${sx} ${TOP + H} C ${sx} ${TOP + H + 50}, ${tx} ${TOP + H + 50}, ${tx} ${TOP + H + 2}` } // backwards (below)
          return <path key={`l${id}`} d={d} className={`fill-none stroke-muted ${MOVE}`} strokeWidth={2.5} markerEnd={`url(#${markerId})`} />
        })}
        {/* nodes */}
        {step.order.map((id) => {
          const tone = step.tones[id] ?? 'normal'
          const nx = x(id)
          const isNull = step.next[id] === null
          return (
            <g key={id}>
              <rect x={nx} y={TOP} width={W} height={H} rx={10} strokeWidth={2.5} className={`${TONE_BOX[tone]} ${MOVE}`} />
              <line x1={nx + VW} y1={TOP} x2={nx + VW} y2={TOP + H} className="stroke-line" strokeWidth={2} />
              <text x={nx + VW / 2} y={TOP + H / 2} textAnchor="middle" dominantBaseline="central" className="fill-ink font-bold font-mono text-[20px]">
                {step.vals[id]}
              </text>
              {isNull
                ? <text x={nx + VW + (W - VW) / 2} y={TOP + H / 2} textAnchor="middle" dominantBaseline="central" className="fill-muted text-[16px]">∅</text>
                : <circle cx={nx + VW + (W - VW) / 2} cy={TOP + H / 2} r={4} className="fill-muted" />}
            </g>
          )
        })}
        {/* pointer labels */}
        {placed.map((q) => {
          const cx = x(q.at as number) + VW / 2
          const y = ptrTop + q.row * 24
          return (
            <g key={q.label}>
              {q.row === 0 && <path d={`M ${cx} ${y - 18} l -6 9 h 12 z`} className={POINTER_TEXT[q.tone]} />}
              <text x={cx} y={y + 4} textAnchor="middle" className={`${POINTER_TEXT[q.tone]} text-[15px] font-bold`}>{q.label}</text>
            </g>
          )
        })}
      </svg>
      {nullPtrs.length > 0 && (
        <p className="text-center text-muted text-sm">{nullPtrs.map((q) => `${q.label} = null`).join(' · ')}</p>
      )}
      <p className="text-center text-xs text-muted mt-1">∅ = next is null (end of the list)</p>
    </div>
  )
}

/** Read the list from head, for screen readers: "3 -> 5 -> 7 -> null". */
function describe(step: LLStep): string {
  const out: string[] = []
  let id = step.head
  const seen = new Set<number>()
  while (id !== null && !seen.has(id) && out.length < 12) { seen.add(id); out.push(String(step.vals[id])); id = step.next[id] ?? null }
  return `${out.join(' → ')} → null`
}

export default function LinkedListViz() {
  const [mode, setMode] = useState<Mode>('insertHead')
  const [list, setList] = useState<number[]>([4, 8, 15, 16])
  const [value, setValue] = useState(42)
  const [posRaw, setPos] = useState(2)

  const n = list.length
  const maxPos = mode === 'insertAt' ? n : n - 1
  const pos = clamp(posRaw, 0, maxPos)

  const steps = useMemo(() => {
    switch (mode) {
      case 'insertHead': return insertHeadSteps(list, value)
      case 'insertAt': return insertAtSteps(list, pos, value)
      case 'delete': return deleteSteps(list, pos)
      case 'reverse': return reverseSteps(list)
    }
  }, [mode, list, pos, value])

  return (
    <div className="space-y-4">
      <Tabs tabs={MODES} value={mode} onChange={setMode} />
      <InputPanel>
        <DraftField label="List values (head first)" initial={list.join(', ')} hint="1 to 6 numbers, from -99 to 99"
          parse={(s) => parseNumberList(s, { minLen: 1, maxLen: 6 })} onApply={setList} />
        {(mode === 'insertHead' || mode === 'insertAt') && (
          <DraftField label="New value" initial={String(value)} inputMode="numeric" hint="From -99 to 99"
            parse={(s) => parseNumber(s, -99, 99)} onApply={setValue} />
        )}
        {(mode === 'insertAt' || mode === 'delete') && (
          <Stepper label="Position (0 = head)" value={pos} min={0} max={maxPos} onChange={setPos} />
        )}
      </InputPanel>
      <StepPlayer steps={steps} label={`Linked list ${mode}`} code={mode === 'reverse' ? REVERSE_CODE : undefined}
        render={(s) => <ListScene step={s} />} />
    </div>
  )
}
