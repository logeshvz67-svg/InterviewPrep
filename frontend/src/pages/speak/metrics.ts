/**
 * LIVE speaking numbers shown while the learner speaks or types.
 * Mirrors backend domain/SpeakingMetrics.java. The server re-computes the official numbers.
 */
export const FILLERS = ['you know', 'i mean', 'kind of', 'sort of', 'um', 'uh', 'umm', 'uhh', 'er', 'ah',
  'like', 'basically', 'actually', 'literally', 'so yeah']

export function countWords(text: string): number {
  const t = text.trim()
  return t ? t.split(/\s+/).length : 0
}

/** Same result as the server's regex scan, done on word tokens (no regex lookbehind: older Safari lacks it). */
export function countFillers(text: string): { total: number; fillers: Record<string, number> } {
  let tokens = text.toLowerCase().replace(/[^a-z' ]/g, ' ').split(/\s+/).filter(Boolean)
  const fillers: Record<string, number> = {}
  let total = 0
  for (const f of FILLERS) {
    const parts = f.split(' ')
    const kept: string[] = []
    let n = 0
    for (let i = 0; i < tokens.length;) {
      if (parts.every((w, j) => tokens[i + j] === w)) { n++; i += parts.length } else { kept.push(tokens[i]); i++ }
    }
    if (n > 0) { fillers[f] = n; total += n; tokens = kept }
  }
  return { total, fillers }
}

export function wpmOf(words: number, seconds: number): number {
  return Math.round((words * 60) / Math.max(1, seconds))
}

/** Plain-words label for a words-per-minute number (target 110-160). */
export function paceLabel(wpm: number, words: number): { text: string; tone: string } {
  if (words < 5) return { text: 'Start speaking…', tone: 'text-muted' }
  if (wpm < 100) return { text: 'A little slow', tone: 'text-warn' }
  if (wpm > 165) return { text: 'A little fast: pause more', tone: 'text-warn' }
  return { text: 'Comfortable pace', tone: 'text-good' }
}

export const SCORE_LABELS: Record<string, string> = {
  pace: 'Pace', fillers: 'Few fillers', length: 'Length', pauses: 'Smooth flow', structure: 'Structure',
  clarity: 'Clarity', grammar: 'Grammar', vocabulary: 'Vocabulary', confidence: 'Confidence',
}

export const FRAMEWORK_NAMES: Record<string, string> = {
  STAR: 'STAR: Situation → Task → Action → Result',
  PPF: 'Present → Past → Future',
  CEU: 'Concept → Example → Use case',
  NONE: 'First → Then → Finally',
}

export function fmtTime(sec: number): string {
  const s = Math.max(0, Math.round(sec))
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`
}
