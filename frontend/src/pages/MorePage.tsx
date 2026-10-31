import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { NAV_GROUPS } from '../components/Layout'

/** Mobile "More" menu: every section of the app. */
export default function MorePage() {
  const { user } = useAuth()
  return (
    <div className="space-y-5">
      {NAV_GROUPS.map((g) => (
        <section key={g.title}>
          <h2 className="text-sm uppercase tracking-wider text-muted mb-2">{g.title}</h2>
          <div className="grid grid-cols-2 gap-2">
            {g.items.filter((n) => !n.admin || user?.role === 'ADMIN').map((n) => (
              <Link key={n.to} to={n.to} className="card flex items-center gap-2 py-3 hover:bg-raised">
                <span className="text-xl" aria-hidden>{n.icon}</span><span className="text-sm font-medium">{n.label}</span>
              </Link>
            ))}
          </div>
        </section>
      ))}
    </div>
  )
}
