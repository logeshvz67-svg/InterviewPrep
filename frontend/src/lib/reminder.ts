import { useEffect } from 'react'

/**
 * Optional, gentle daily reminder (no guilt). Free static hosting has no push server, so this only
 * works while the app is open in a tab or installed as a PWA and running. We say so in Settings.
 */
export function useReminder(settings?: Record<string, unknown>) {
  useEffect(() => {
    const time = settings?.reminderTime as string | undefined
    if (!settings?.notificationsOn || !time || !('Notification' in window)) return
    const check = () => {
      const now = new Date()
      const hhmm = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
      const key = 'cq.reminded.' + now.toDateString()
      let done = false
      try { done = localStorage.getItem(key) === '1' } catch { /* ignore */ }
      if (hhmm >= time && !done && Notification.permission === 'granted') {
        const body = 'Even 20 minutes counts today. Your quests are waiting whenever you\'re ready.'
        navigator.serviceWorker?.ready.then((reg) => reg.showNotification('Code Quest', { body, icon: '/icon-192.png', tag: 'daily' }))
          .catch(() => new Notification('Code Quest', { body }))
        try { localStorage.setItem(key, '1') } catch { /* ignore */ }
      }
    }
    check()
    const id = setInterval(check, 60_000)
    return () => clearInterval(id)
  }, [settings])
}
