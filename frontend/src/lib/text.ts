/** 0..1 similarity between what was typed and the target code (whitespace-insensitive Levenshtein). */
export function similarity(a: string, b: string): number {
  const x = a.replace(/\s+/g, ' ').trim()
  const y = b.replace(/\s+/g, ' ').trim()
  if (!x.length && !y.length) return 1
  if (!x.length || !y.length) return 0
  const n = x.length, m = y.length
  let prev = new Array(m + 1).fill(0).map((_, j) => j)
  for (let i = 1; i <= n; i++) {
    const cur = [i]
    for (let j = 1; j <= m; j++) {
      cur[j] = Math.min(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + (x[i - 1] === y[j - 1] ? 0 : 1))
    }
    prev = cur
  }
  return Math.max(0, 1 - prev[m] / Math.max(n, m))
}
