import { useCallback, useEffect, useRef, useState } from 'react'
import { api } from '../../api/client'

export function speechSynthesisSupported(): boolean {
  return typeof window !== 'undefined' && 'speechSynthesis' in window && typeof window.SpeechSynthesisUtterance === 'function'
}

function pickVoice(voices: SpeechSynthesisVoice[], lang: string): SpeechSynthesisVoice | null {
  const l = lang.toLowerCase()
  const prefix = l.split('-')[0]
  return voices.find((v) => v.lang.toLowerCase() === l)
    ?? voices.find((v) => v.lang.toLowerCase().replace('_', '-') === l)
    ?? voices.find((v) => v.lang.toLowerCase().startsWith(prefix) && v.default)
    ?? voices.find((v) => v.lang.toLowerCase().startsWith(prefix))
    ?? null
}

/** Split long text into sentences: some browsers cut off utterances longer than ~15 seconds. */
function chunks(text: string): string[] {
  const parts = text.replace(/\s+/g, ' ').match(/[^.!?]+[.!?]*/g) ?? [text]
  return parts.map((p) => p.trim()).filter(Boolean)
}

/**
 * Text-to-speech with the browser's speechSynthesis. If it's missing, speak() does nothing
 * (silently) and still calls onEnd, so flows never get stuck.
 */
export function useSpeech(lang = 'en-IN', rate = 1) {
  const [supported] = useState(speechSynthesisSupported)
  const [speaking, setSpeaking] = useState(false)
  const [voices, setVoices] = useState<SpeechSynthesisVoice[]>([])
  const tokenRef = useRef(0)

  useEffect(() => {
    if (!supported) return
    const load = () => setVoices(window.speechSynthesis.getVoices())
    load()
    window.speechSynthesis.addEventListener?.('voiceschanged', load)
    return () => {
      window.speechSynthesis.removeEventListener?.('voiceschanged', load)
      window.speechSynthesis.cancel()
    }
  }, [supported])

  const cancel = useCallback(() => {
    tokenRef.current++
    if (supported) window.speechSynthesis.cancel()
    setSpeaking(false)
  }, [supported])

  const speak = useCallback((text: string, onEnd?: () => void) => {
    const token = ++tokenRef.current
    if (!supported || !text.trim()) { onEnd?.(); return }
    const synth = window.speechSynthesis
    synth.cancel()
    const voice = pickVoice(voices.length ? voices : synth.getVoices(), lang)
    const list = chunks(text)
    setSpeaking(true)
    list.forEach((part, i) => {
      const u = new SpeechSynthesisUtterance(part)
      u.lang = voice?.lang ?? lang
      if (voice) u.voice = voice
      u.rate = rate
      if (i === list.length - 1) {
        const done = () => { if (tokenRef.current === token) { setSpeaking(false); onEnd?.() } }
        u.onend = done
        u.onerror = done
      }
      synth.speak(u)
    })
  }, [supported, voices, lang, rate])

  return { supported, speaking, speak, cancel, voiceName: pickVoice(voices, lang)?.name ?? null }
}

export type SpeechState = ReturnType<typeof useSpeech>

/** Voice settings from GET /api/profile (settings.voiceLang / settings.voiceRate). Defaults: en-IN, 1.0. */
export function useVoiceSettings() {
  const [lang, setLang] = useState('en-IN')
  const [rate, setRate] = useState(1)
  useEffect(() => {
    let alive = true
    api<{ settings?: Record<string, unknown> | null }>('/api/profile')
      .then((p) => {
        if (!alive) return
        const s = p?.settings ?? {}
        if (typeof s.voiceLang === 'string' && /^[a-z]{2,3}(-[A-Za-z]{2,4})?$/.test(s.voiceLang)) setLang(s.voiceLang)
        const r = typeof s.voiceRate === 'number' ? s.voiceRate : typeof s.voiceRate === 'string' ? Number(s.voiceRate) : NaN
        if (Number.isFinite(r)) setRate(Math.min(2, Math.max(0.5, r)))
      })
      .catch(() => { /* keep defaults */ })
    return () => { alive = false }
  }, [])
  return { lang, rate }
}
