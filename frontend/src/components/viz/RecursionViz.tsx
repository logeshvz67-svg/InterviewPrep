import { useMemo, useState } from 'react'
import { Tabs } from '../ui'
import { MOVE } from './draw'
import { InputPanel, Stepper } from './fields'
import { clamp, mcq, nearNumbers } from './lib'
import { StepPlayer } from './StepPlayer'
import type { BaseStep } from './types'

/**
 * RECURSION
 * A function that calls itself on a smaller input.
 *  - Base case: a small input we answer directly (no more calls). Without it, calls never stop.
 *  - Each call waits on the CALL STACK until the smaller calls RETURN their answers.
 *  - factorial(n) = n × factorial(n-1), base: factorial(0 or 1) = 1.  n calls, O(n).
 *  - fib(n) = fib(n-1) + fib(n-2), base: fib(0) = 0, fib(1) = 1.     Calls double each level, O(2^n).
 * The tree shows every call. Calls go DOWN the tree, answers come back UP.
 */

type Fn = 'fact' | 'fib'
const MODES: { id: Fn; label: string }[] = [{ id: 'fact', label: '❗ factorial(n)' }, { id: 'fib', label: '🐇 fibonacci(n)' }]

const CODE: Record<Fn, string[]> = {
  fact: [
    'int fact(int n) {',
    '    if (n <= 1) return 1;          // base case',
    '    return n * fact(n - 1);',
    '}',
  ],
  fib: [
    'int fib(int n) {',
    '    if (n <= 1) return n;          // base case',
    '    return fib(n - 1) + fib(n - 2);',
    '}',
  ],
}

interface CallNode {
  id: number
  arg: number
  label: string
  children: number[]
  parent: number | null
  depth: number
  x: number // horizontal slot in the tree drawing
  value: number
}

/** Build the whole call tree first (so the drawing does not jump), then play it as steps. */
function buildTree(fn: Fn, n: number): CallNode[] {
  const nodes: CallNode[] = []
  let leafSlot = 0
  const make = (arg: number, parent: number | null, depth: number): number => {
    const id = nodes.length
    const node: CallNode = { id, arg, label: `${fn}(${arg})`, children: [], parent, depth, x: 0, value: 0 }
    nodes.push(node)
    const kids = arg <= 1 ? [] : fn === 'fact' ? [arg - 1] : [arg - 1, arg - 2]
    node.children = kids.map((k) => make(k, id, depth + 1))
    if (node.children.length === 0) {
      node.x = leafSlot++
      node.value = fn === 'fact' ? 1 : arg
    } else {
      // A parent sits in the middle above its children.
      node.x = node.children.reduce((s, c) => s + nodes[c].x, 0) / node.children.length
      node.value = fn === 'fact' ? arg * nodes[node.children[0]].value : nodes[node.children[0]].value + nodes[node.children[1]].value
    }
    return id
  }
  make(n, null, 0)
  return nodes
}

interface RecStep extends BaseStep {
  called: number[] // ids that have been called so far (drawn)
  returned: Record<number, number> // id -> value it returned
  stack: number[] // call stack, bottom -> top
}

function recursionSteps(fn: Fn, n: number, nodes: CallNode[]): RecStep[] {
  const called: number[] = []
  const returned: Record<number, number> = {}
  const stack: number[] = []
  const steps: RecStep[] = [{
    called: [], returned: {}, stack: [],
    caption: `We will compute ${fn}(${n}). Watch the calls go DOWN the tree, and the answers come back UP.`,
  }]
  const snap = (caption: string, line: number, predict?: RecStep['predict']) =>
    steps.push({ called: [...called], returned: { ...returned }, stack: [...stack], caption, line, predict })

  const visit = (id: number) => {
    const node = nodes[id]
    const parent = node.parent === null ? null : nodes[node.parent]
    called.push(id)
    stack.push(id)
    if (!parent) {
      snap(`Call ${node.label}. It goes on the call stack.`, 0)
    } else {
      const siblingIndex = parent.children.indexOf(id)
      const wrong = [`${fn}(${parent.arg})`, `${fn}(${parent.arg + 1})`, 'Nothing: it returns now']
      if (fn === 'fib') wrong.unshift(`${fn}(${siblingIndex === 0 ? parent.arg - 2 : parent.arg - 1})`)
      snap(`${parent.label} needs ${node.label}. Call it, and push it on the stack. ${parent.label} waits.`, 2,
        mcq(siblingIndex === 1 ? `${parent.label} got ${nodes[parent.children[0]].label}'s answer. What does it call next?` : `${parent.label} is running. What does it call?`,
          node.label, wrong, fn === 'fact' ? `fact(n) calls fact(n - 1).` : siblingIndex === 0 ? 'fib(n) first calls fib(n - 1).' : 'After fib(n - 1), it calls fib(n - 2).'))
    }
    node.children.forEach(visit)
    returned[id] = node.value
    stack.pop()
    const to = parent ? ` The answer goes back up to ${parent.label}.` : ` That is the final answer!`
    if (node.children.length === 0) {
      snap(`${node.label} is a base case. It returns ${node.value} right away. Pop it off the stack.${to}`, 1,
        mcq(`What does ${node.label} return?`, String(node.value), nearNumbers(node.value, [node.arg === node.value ? node.value + 1 : node.arg]),
          fn === 'fact' ? 'Base case: fact(0) = fact(1) = 1.' : 'Base case: fib(n) = n when n is 0 or 1.'))
    } else {
      const kids = node.children.map((c) => nodes[c])
      const formula = fn === 'fact'
        ? `${node.arg} × ${kids[0].value} = ${node.value}`
        : `${kids[0].value} + ${kids[1].value} = ${node.value}`
      const wrong = fn === 'fact'
        ? [String(kids[0].value), String(node.arg + kids[0].value), ...nearNumbers(node.value)]
        : [String(kids[0].value * kids[1].value), String(kids[0].value), ...nearNumbers(node.value)]
      snap(`${node.label} = ${formula}. Return ${node.value} and pop it off the stack.${to}`, 2,
        mcq(`${kids.map((k) => `${k.label} returned ${k.value}`).join(', ')}. What does ${node.label} return?`, String(node.value), wrong,
          fn === 'fact' ? `fact(${node.arg}) = ${node.arg} × fact(${node.arg - 1}).` : `fib(${node.arg}) = fib(${node.arg - 1}) + fib(${node.arg - 2}).`))
    }
  }
  visit(0)
  const calls = `${nodes.length} call${nodes.length === 1 ? '' : 's'}`
  steps.push({
    called: [...called], returned: { ...returned }, stack: [],
    caption: fn === 'fact'
      ? `${fn}(${n}) = ${nodes[0].value}. It took ${calls}: one per number, so O(n). The stack was ${calls} deep.`
      : n >= 3
        ? `fib(${n}) = ${nodes[0].value}. It took ${calls}! Some calls repeat (look for the same label twice). That is why plain recursive fib is O(2^n).`
        : `fib(${n}) = ${nodes[0].value}. It took ${calls}. Try a bigger n to see how fast the tree grows: plain recursive fib is O(2^n).`,
  })
  return steps
}

// ------------------------------------------------------------------ drawing

const SLOT = 74
const LEVEL = 78
const R = 28

function TreeScene({ nodes, step }: { nodes: CallNode[]; step: RecStep }) {
  const maxX = Math.max(...nodes.map((nd) => nd.x))
  const maxDepth = Math.max(...nodes.map((nd) => nd.depth))
  const width = (maxX + 1) * SLOT + 30
  const height = (maxDepth + 1) * LEVEL + 20
  const cx = (nd: CallNode) => 15 + nd.x * SLOT + SLOT / 2
  const cy = (nd: CallNode) => 10 + nd.depth * LEVEL + LEVEL / 2
  const visible = new Set(step.called)
  const top = step.stack[step.stack.length - 1]
  const onStack = new Set(step.stack)

  return (
    <div className="grid gap-4 md:grid-cols-[1fr_auto] items-start">
      <svg viewBox={`0 0 ${width} ${height}`} className="block w-full h-auto mx-auto" style={{ maxWidth: Math.max(width * 1.2, 160), maxHeight: 460 }}
        role="img" aria-label={`Recursion tree. ${step.called.length} calls made so far.`}>
        {nodes.map((nd) => {
          if (nd.parent === null || !visible.has(nd.id)) return null
          const par = nodes[nd.parent]
          const back = step.returned[nd.id] !== undefined
          return (
            <g key={`e${nd.id}`}>
              <line x1={cx(par)} y1={cy(par) + R} x2={cx(nd)} y2={cy(nd) - R} strokeWidth={2.5}
                className={`${back ? 'stroke-good' : 'stroke-primary'} ${MOVE}`} strokeDasharray={back ? undefined : '6 4'} />
              {back && (
                <text x={(cx(par) + cx(nd)) / 2 + 8} y={(cy(par) + cy(nd)) / 2} className="fill-good text-[14px] font-bold">↑{nd.value}</text>
              )}
            </g>
          )
        })}
        {nodes.map((nd) => {
          if (!visible.has(nd.id)) return null
          const done = step.returned[nd.id] !== undefined
          const cls = nd.id === top ? 'fill-primary/40 stroke-primary' : onStack.has(nd.id) ? 'fill-primary/10 stroke-primary' : done ? 'fill-good/20 stroke-good' : 'fill-raised stroke-line'
          return (
            <g key={nd.id}>
              <circle cx={cx(nd)} cy={cy(nd)} r={R} strokeWidth={nd.id === top ? 4 : 2.5} className={`${cls} ${MOVE}`} />
              <text x={cx(nd)} y={cy(nd)} textAnchor="middle" dominantBaseline="central" className="fill-ink font-mono font-bold text-[13px]">
                {nd.label}
              </text>
              {done && (
                <g>
                  <circle cx={cx(nd) + R - 2} cy={cy(nd) - R + 2} r={13} className="fill-gold" />
                  <text x={cx(nd) + R - 2} y={cy(nd) - R + 2} textAnchor="middle" dominantBaseline="central" className="fill-bg font-bold text-[12px]">
                    {nd.value}
                  </text>
                </g>
              )}
            </g>
          )
        })}
      </svg>

      {/* Call stack: top is drawn at the top */}
      <div className="min-w-[10rem]" aria-label="Call stack">
        <p className="text-sm font-semibold text-muted mb-1">Call stack (top first)</p>
        {step.stack.length === 0 ? (
          <p className="text-muted text-sm rounded-lg border border-dashed border-line p-2 text-center">empty</p>
        ) : (
          <ol className="space-y-1">
            {[...step.stack].reverse().map((id, i) => (
              <li key={id} className={`rounded-lg px-3 py-1.5 font-mono text-sm border ${i === 0 ? 'bg-primary/25 border-primary font-bold' : 'bg-raised border-line text-muted'}`}>
                {nodes[id].label} {i === 0 ? '← running' : '· waiting'}
              </li>
            ))}
          </ol>
        )}
      </div>
    </div>
  )
}

const LIMITS: Record<Fn, { min: number; max: number }> = { fact: { min: 0, max: 7 }, fib: { min: 0, max: 5 } }

export default function RecursionViz() {
  const [fn, setFn] = useState<Fn>('fact')
  const [nRaw, setN] = useState(4)
  const n = clamp(nRaw, LIMITS[fn].min, LIMITS[fn].max)
  const nodes = useMemo(() => buildTree(fn, n), [fn, n])
  const steps = useMemo(() => recursionSteps(fn, n, nodes), [fn, n, nodes])

  return (
    <div className="space-y-4">
      <Tabs tabs={MODES} value={fn} onChange={setFn} />
      <InputPanel>
        <Stepper label="n" value={n} min={LIMITS[fn].min} max={LIMITS[fn].max} onChange={setN}
          hint={`From ${LIMITS[fn].min} to ${LIMITS[fn].max}. ${fn === 'fib' ? 'fib grows fast: fib(5) already makes 15 calls!' : ''}`} />
      </InputPanel>
      <StepPlayer steps={steps} label={`${fn}(${n}) recursion`} code={CODE[fn]}
        render={(s) => <TreeScene nodes={nodes} step={s} />} />
    </div>
  )
}
