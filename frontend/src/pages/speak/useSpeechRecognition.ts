import { useCallback, useEffect, useRef, useState } from 'react'
import type { SRConstructor, SRInstance } from './types'

/** Gap between two recognition results that counts as a "long pause". */
export const LONG_PAUSE_MS = 3000

export const SR_HELP = 'Speech recognition works best in Chrome or Edge and may need internet.'

function getRecognitionCtor(): SRConstructor | null {
  if (typeof window === 'undefined') return null
  const w = window as unknown as { SpeechRecognition?: SRConstructor; webkitSpeechRecognition?: SRConstructor }
  return w.SpeechRecognition ?? w.webkitSpeechRecognition ?? null
}

export function speechRecognitionSupported(): boolean {
  return getRecognitionCtor() !== null
}

function errorText(code: string): string {
  switch (code) {
    case 'not-allowed':
    case 'service-not-allowed':
      return `Microphone permission was blocked. Allow the mic for this site, or type your answer below. ${SR_HELP}`
    case 'network':
      return `Speech recognition could not reach its service (network error). ${SR_HELP} You can type your answer below.`
    case 'audio-capture':
      return 'No microphone was found. Plug one in, or type your answer below.'
    case 'language-not-supported':
      return `This voice language is not supported here. ${SR_HELP} You can type your answer below.`
    default:
      return `Speech recognition stopped (${code}). ${SR_HELP} You can type your answer below.`
  }
}

/** Errors that are part of normal use: we just keep listening. */
const SOFT_ERRORS = new Set(['no-speech', 'aborted'])

/**
 * Live speech-to-text with the browser's Web Speech API. Audio is never stored or uploaded by us:
 * only the text transcript is kept in memory (the browser's own speech service may process audio).
 */
export function useSpeechRecognition(lang = 'en-IN') {
  const [supported] = useState(speechRecognitionSupported)
  const [listening, setListening] = useState(false)
  const [finalText, setFinalText] = useState('')
  const [interim, setInterim] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [elapsed, setElapsed] = useState(0)
  const [longPauses, setLongPauses] = useState(0)

  const recRef = useRef<SRInstance | null>(null)
  const wantRef = useRef(false)          // the user wants to be listening (restart after auto-end)
  const committedRef = useRef('')        // finals from earlier recognition runs (Chrome ends runs on silence)
  const runFinalRef = useRef('')         // finals of the current run
  const lastResultRef = useRef(0)
  const startedAtRef = useRef<number | null>(null)
  const accumulatedRef = useRef(0)

  const join = (a: string, b: string) => (a && b ? `${a.trim()} ${b.trim()}` : (a || b).trim())

  // Elapsed-seconds ticker while listening.
  useEffect(() => {
    if (!listening) return
    const t = setInterval(() => {
      if (startedAtRef.current != null) setElapsed(Math.floor((accumulatedRef.current + Date.now() - startedAtRef.current) / 1000))
    }, 250)
    return () => clearInterval(t)
  }, [listening])

  const freezeClock = () => {
    if (startedAtRef.current != null) {
      accumulatedRef.current += Date.now() - startedAtRef.current
      startedAtRef.current = null
      setElapsed(Math.floor(accumulatedRef.current / 1000))
    }
  }

  const commitRun = () => {
    committedRef.current = join(committedRef.current, runFinalRef.current)
    runFinalRef.current = ''
    setFinalText(committedRef.current)
    setInterim('')
  }

  const createAndStart = useCallback(() => {
    const Ctor = getRecognitionCtor()
    if (!Ctor) return
    const rec = new Ctor()
    rec.lang = lang
    rec.continuous = true
    rec.interimResults = true
    rec.maxAlternatives = 1
    rec.onresult = (e) => {
      const now = Date.now()
      if (lastResultRef.current && now - lastResultRef.current > LONG_PAUSE_MS) setLongPauses((n) => n + 1)
      lastResultRef.current = now
      // Rebuild this run's text from the full result list (avoids duplicated words on some phones).
      let fin = ''
      let mid = ''
      for (let i = 0; i < e.results.length; i++) {
        const r = e.results[i]
        const txt = r[0]?.transcript ?? ''
        if (r.isFinal) fin = join(fin, txt)
        else mid = join(mid, txt)
      }
      runFinalRef.current = fin
      setFinalText(join(committedRef.current, fin))
      setInterim(mid)
    }
    rec.onerror = (e) => {
      if (SOFT_ERRORS.has(e.error)) return
      wantRef.current = false
      setError(errorText(e.error))
    }
    rec.onend = () => {
      commitRun()
      if (wantRef.current) {
        try { rec.start(); return } catch { /* fall through and stop */ }
      }
      recRef.current = null
      wantRef.current = false
      freezeClock()
      setListening(false)
    }
    recRef.current = rec
    try {
      rec.start()
    } catch {
      wantRef.current = false
      recRef.current = null
      setError(`Could not start the microphone. ${SR_HELP} You can type your answer below.`)
      freezeClock()
      setListening(false)
    }
  }, [lang])

  const start = useCallback(() => {
    if (!supported) { setError(`${SR_HELP} Your browser does not support it, so please type your answer.`); return }
    if (wantRef.current) return
    setError(null)
    wantRef.current = true
    startedAtRef.current = Date.now()
    lastResultRef.current = 0
    setListening(true)
    createAndStart()
  }, [supported, createAndStart])

  const stop = useCallback(() => {
    wantRef.current = false
    const rec = recRef.current
    if (rec) {
      try { rec.stop() } catch { /* already stopped */ }
    } else {
      freezeClock()
      setListening(false)
    }
  }, [])

  const reset = useCallback(() => {
    wantRef.current = false
    try { recRef.current?.abort() } catch { /* ignore */ }
    recRef.current = null
    committedRef.current = ''
    runFinalRef.current = ''
    lastResultRef.current = 0
    startedAtRef.current = null
    accumulatedRef.current = 0
    setFinalText('')
    setInterim('')
    setElapsed(0)
    setLongPauses(0)
    setError(null)
    setListening(false)
  }, [])

  // Stop the mic when the component goes away.
  useEffect(() => () => {
    wantRef.current = false
    try { recRef.current?.abort() } catch { /* ignore */ }
  }, [])

  const transcript = join(finalText, interim)
  return { supported, listening, transcript, finalText, interim, error, elapsed, longPauses, start, stop, reset }
}

export type SpeechRecognitionState = ReturnType<typeof useSpeechRecognition>
