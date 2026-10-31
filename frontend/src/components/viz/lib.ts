import { useEffect, useState } from 'react'
import type { Prediction } from './types'

/**
 * Build a multiple-choice question. Wrong answers equal to the right one are dropped,
 * and at most 4 options are kept. The order is mixed in a fixed way (same question = same order),
 * so options do not jump around when the screen re-renders.
 * Returns undefined when there are not at least 2 different options.
 */
export function mcq(question: string, correct: string, wrong: string[], explain?: string): Prediction | undefined {
  const others = Array.from(new Set(wrong.filter((w) => w !== correct))).slice(0, 3)
  if (others.length === 0) return undefined
  const options = [correct, ...others]
  let hash = 0
  for (const ch of question + correct) hash = (hash * 31 + ch.charCodeAt(0)) >>> 0
  const shift = hash % options.length
  const mixed = options.slice(shift).concat(options.slice(0, shift))
  return { question, options: mixed, answer: mixed.indexOf(correct), explain }
}

/** Wrong-but-close number answers: value ± 1 plus some extra candidates. */
export function nearNumbers(value: number, extra: number[] = []): string[] {
  return [...extra, value + 1, value - 1, value + 2].filter((v) => v !== value).map(String)
}

/** Write negative numbers in brackets inside a sum: 5 + (-3). */
export function p(v: number): string {
  return v < 0 ? `(${v})` : String(v)
}

/** Keep a whole number inside [min, max]. */
export function clamp(v: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, v))
}

export type Parsed<T> = { value: T } | { error: string }

/** Parse "3, 1, 4" into numbers, with safe limits on length and size. */
export function parseNumberList(text: string, opts: { minLen: number; maxLen: number; min?: number; max?: number }): Parsed<number[]> {
  const min = opts.min ?? -99
  const max = opts.max ?? 99
  const parts = text.split(/[\s,]+/).filter(Boolean)
  if (parts.length < opts.minLen) return { error: `Please type at least ${opts.minLen} number${opts.minLen === 1 ? '' : 's'}.` }
  if (parts.length > opts.maxLen) return { error: `Please type at most ${opts.maxLen} numbers.` }
  const nums: number[] = []
  for (const part of parts) {
    if (!/^-?\d+$/.test(part)) return { error: `"${part}" is not a whole number.` }
    const n = Number(part)
    if (n < min || n > max) return { error: `Numbers must be between ${min} and ${max}.` }
    nums.push(n)
  }
  return { value: nums }
}

/** Parse a single whole number with limits. */
export function parseNumber(text: string, min: number, max: number): Parsed<number> {
  const t = text.trim()
  if (!/^-?\d+$/.test(t)) return { error: 'Please type a whole number.' }
  const n = Number(t)
  if (n < min || n > max) return { error: `Use a number between ${min} and ${max}.` }
  return { value: n }
}

/** True when the user asked the system for less motion. */
export function usePrefersReducedMotion(): boolean {
  const query = '(prefers-reduced-motion: reduce)'
  const [reduced, setReduced] = useState(() => typeof window !== 'undefined' && !!window.matchMedia?.(query).matches)
  useEffect(() => {
    const mq = window.matchMedia?.(query)
    if (!mq) return
    const onChange = () => setReduced(mq.matches)
    mq.addEventListener('change', onChange)
    return () => mq.removeEventListener('change', onChange)
  }, [])
  return reduced
}
