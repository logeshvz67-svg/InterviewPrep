import { useEffect, useState, type ReactNode } from 'react'
import { wakeServer } from '../api/client'
import { game } from '../theme/gameConfig'

const TIPS = [
  'Tip: in Java, == compares references for objects. Use equals() for content.',
  'Tip: a HashMap lookup is O(1) on average. That is why hashing solves so many problems.',
  'Tip: dry-run your loop with a 3-element array before you trust it.',
  'Tip: explaining a concept out loud is the fastest way to find gaps.',
  'Tip: String is immutable. Every "change" creates a new String.',
]

/**
 * The free Render server sleeps after ~15 min idle and needs up to a minute to wake.
 * Instead of a scary error, show a friendly screen while we ping /health.
 */
export function WakeGate({ children }: { children: ReactNode }) {
  const [ready, setReady] = useState(false)
  const [waited, setWaited] = useState(0)
  const [failed, setFailed] = useState(false)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let alive = true
    setFailed(false)
    wakeServer((s) => alive && setWaited(s)).then((ok) => {
      if (!alive) return
      if (ok) setReady(true)
      else setFailed(true)
    })
    return () => { alive = false }
  }, [attempt])

  if (ready) return <>{children}</>
  const showScreen = waited >= 2 || failed
  if (!showScreen) return null

  return (
    <main className="min-h-screen grid place-items-center p-6">
      <div className="card max-w-md text-center space-y-4">
        <div className="text-5xl animate-bounce" aria-hidden>🐉</div>
        <h1 className="text-2xl font-bold">{failed ? 'The server is still asleep' : 'Waking the server…'}</h1>
        <p className="text-muted">
          {failed
            ? 'It took longer than 2 minutes. It might be a free-tier hiccup or no internet.'
            : `${game.title} runs on a free server that naps when nobody uses it. Waking it takes up to a minute.`}
        </p>
        {!failed && (
          <>
            <div className="h-2 rounded-full bg-raised overflow-hidden">
              <div className="h-full bg-primary transition-all" style={{ width: `${Math.min(100, (waited / 60) * 100)}%` }} />
            </div>
            <p className="text-sm text-muted">{waited}s · {TIPS[Math.floor(waited / 10) % TIPS.length]}</p>
          </>
        )}
        {failed && <button className="btn-primary" onClick={() => { setWaited(0); setAttempt((a) => a + 1) }}>Try again</button>}
      </div>
    </main>
  )
}
