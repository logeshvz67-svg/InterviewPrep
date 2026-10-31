import { NavLink, Outlet } from 'react-router-dom'
import { game, labels } from '../theme/gameConfig'

const NAV = [
  { to: '/', icon: '🏠', label: 'Home', end: true },
  { to: '/now', icon: '👉', label: labels.doThisNow },
  { to: '/curriculum', icon: '🏝️', label: 'Learn' },
  { to: '/gym', icon: '🏋️', label: 'Gym' },
  { to: '/focus', icon: '⏱️', label: labels.focus },
  { to: '/roadmap', icon: '🗺️', label: labels.roadmap },
  { to: '/settings', icon: '⚙️', label: labels.settings },
]
const MOBILE = ['/', '/curriculum', '/gym', '/focus', '/settings']

export function Layout() {
  return (
    <div className="md:flex min-h-screen">
      <nav className="hidden md:flex md:flex-col w-56 shrink-0 border-r border-line/60 p-4 gap-1 sticky top-0 h-screen" aria-label="Main">
        <div className="font-display font-bold text-xl mb-4">⚔️ {game.title}</div>
        {NAV.map((n) => (
          <NavLink key={n.to} to={n.to} end={n.end}
            className={({ isActive }) => `rounded-xl px-3 py-2 flex gap-2 items-center ${isActive ? 'bg-primary/15 text-primary font-semibold' : 'text-muted hover:bg-raised'}`}>
            <span aria-hidden>{n.icon}</span>{n.label}
          </NavLink>
        ))}
      </nav>
      <main className="flex-1 max-w-4xl mx-auto w-full p-4 pb-28 md:pb-8 md:p-8">
        <Outlet />
      </main>
      <nav className="md:hidden fixed bottom-0 inset-x-0 bg-surface/95 backdrop-blur border-t border-line/60 grid grid-cols-5 pb-[env(safe-area-inset-bottom)] z-40" aria-label="Main">
        {NAV.filter((n) => MOBILE.includes(n.to)).map((n) => (
          <NavLink key={n.to} to={n.to} end={n.end}
            className={({ isActive }) => `flex flex-col items-center py-2 text-[11px] ${isActive ? 'text-primary' : 'text-muted'}`}>
            <span className="text-xl" aria-hidden>{n.icon}</span>{n.label}
          </NavLink>
        ))}
      </nav>
    </div>
  )
}
