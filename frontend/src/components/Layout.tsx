import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { game } from '../theme/gameConfig'

export interface NavItem { to: string; icon: string; label: string; end?: boolean; admin?: boolean }

/** Every place in the app, grouped. Mobile shows 5 tabs + "More". */
export const NAV_GROUPS: { title: string; items: NavItem[] }[] = [
  { title: 'Today', items: [
    { to: '/', icon: '🏠', label: 'Command Center', end: true },
    { to: '/now', icon: '👉', label: 'Do this now' },
    { to: '/focus', icon: '⏱️', label: 'Focus' },
    { to: '/quests', icon: '📜', label: 'Quests & badges' },
  ] },
  { title: 'Learn', items: [
    { to: '/learn', icon: '🗺️', label: 'Worlds & lessons' },
    { to: '/recall', icon: '🧠', label: 'Recall' },
    { to: '/visualizers', icon: '🎞️', label: 'Visualizers' },
    { to: '/curriculum', icon: '🏝️', label: 'Java topic tracker' },
    { to: '/spring', icon: '🔥', label: 'Spring Boot track' },
  ] },
  { title: 'Practice', items: [
    { to: '/gym', icon: '🏋️', label: 'Problem Gym' },
    { to: '/mentor', icon: '🧙', label: 'AI Mentor' },
    { to: '/speak', icon: '🎙️', label: 'Communication Gym' },
    { to: '/interview', icon: '🏟️', label: 'Interview Colosseum' },
  ] },
  { title: 'You', items: [
    { to: '/records', icon: '🏛️', label: 'Hall of Records' },
    { to: '/roadmap', icon: '🧭', label: 'Roadmap' },
    { to: '/settings', icon: '⚙️', label: 'Settings' },
    { to: '/admin', icon: '🛠️', label: 'Admin', admin: true },
  ] },
]

const MOBILE: NavItem[] = [
  { to: '/', icon: '🏠', label: 'Home', end: true },
  { to: '/learn', icon: '🗺️', label: 'Learn' },
  { to: '/recall', icon: '🧠', label: 'Recall' },
  { to: '/gym', icon: '🏋️', label: 'Gym' },
  { to: '/more', icon: '☰', label: 'More' },
]

export function Layout() {
  const { user } = useAuth()
  return (
    <div className="md:flex min-h-screen">
      <nav className="hidden md:flex md:flex-col w-60 shrink-0 border-r border-line/60 p-4 gap-0.5 sticky top-0 h-screen overflow-y-auto" aria-label="Main">
        <div className="font-display font-bold text-xl mb-3">⚔️ {game.title}</div>
        {NAV_GROUPS.map((g) => (
          <div key={g.title} className="mb-2">
            <p className="text-[11px] uppercase tracking-wider text-muted px-3 mt-2 mb-1">{g.title}</p>
            {g.items.filter((n) => !n.admin || user?.role === 'ADMIN').map((n) => (
              <NavLink key={n.to} to={n.to} end={n.end}
                className={({ isActive }) => `rounded-xl px-3 py-1.5 flex gap-2 items-center text-sm ${isActive ? 'bg-primary/15 text-primary font-semibold' : 'text-muted hover:bg-raised'}`}>
                <span aria-hidden>{n.icon}</span>{n.label}
              </NavLink>
            ))}
          </div>
        ))}
      </nav>
      <main className="flex-1 max-w-5xl mx-auto w-full p-4 pb-28 md:pb-8 md:p-8 min-w-0">
        <Outlet />
      </main>
      <nav className="md:hidden fixed bottom-0 inset-x-0 bg-surface/95 backdrop-blur border-t border-line/60 grid grid-cols-5 pb-[env(safe-area-inset-bottom)] z-40" aria-label="Main">
        {MOBILE.map((n) => (
          <NavLink key={n.to} to={n.to} end={n.end}
            className={({ isActive }) => `flex flex-col items-center py-2 text-[11px] ${isActive ? 'text-primary' : 'text-muted'}`}>
            <span className="text-xl" aria-hidden>{n.icon}</span>{n.label}
          </NavLink>
        ))}
      </nav>
    </div>
  )
}
