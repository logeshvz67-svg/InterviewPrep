import { useCallback, useEffect, useState } from 'react'
import { api, ApiError } from '../api/client'

/** Load data from the API with loading + error state, and a reload() function. */
export function useApi<T>(path: string | null) {
  const [data, setData] = useState<T | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(!!path)

  const reload = useCallback(async () => {
    if (!path) return
    setLoading(true)
    try {
      setData(await api<T>(path))
      setError(null)
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Something went wrong')
    } finally {
      setLoading(false)
    }
  }, [path])

  useEffect(() => { void reload() }, [reload])
  return { data, error, loading, reload, setData }
}

export function errorMessage(e: unknown): string {
  if (e instanceof ApiError) {
    if (e.fieldErrors) return Object.entries(e.fieldErrors).map(([k, v]) => `${k}: ${v}`).join('\n')
    return e.message
  }
  return 'Something went wrong'
}

/** Stored per browser: theme choice. */
export function useLocalSetting<T extends string>(key: string, initial: T): [T, (v: T) => void] {
  const [v, setV] = useState<T>(() => {
    try { return (localStorage.getItem(key) as T) ?? initial } catch { return initial }
  })
  const set = (nv: T) => { setV(nv); try { localStorage.setItem(key, nv) } catch { /* ignore */ } }
  return [v, set]
}
