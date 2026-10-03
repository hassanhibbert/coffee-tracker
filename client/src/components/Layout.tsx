import { NavLink, Outlet } from 'react-router'
import { useAuth } from '../auth/useAuth'
import { Button } from './ui/Button'

const links = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/brews', label: 'Brews', end: false },
  { to: '/beans', label: 'Beans', end: false },
  { to: '/equipment', label: 'Equipment', end: false },
]

export function Layout() {
  const { username, logout } = useAuth()

  return (
    <div className="min-h-screen">
      <header className="border-b border-stone-200 bg-white">
        <div className="mx-auto flex max-w-5xl flex-wrap items-center gap-x-6 gap-y-2 px-4 py-3">
          <NavLink to="/" className="flex items-center gap-2 font-semibold text-brand-800">
            <img src="/favicon.svg" alt="" className="h-7 w-7" />
            Coffee Tracker
          </NavLink>
          <nav aria-label="Main" className="order-last flex w-full gap-1 overflow-x-auto sm:order-none sm:w-auto">
            {links.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                end={link.end}
                className={({ isActive }) =>
                  `rounded-md px-3 py-1.5 text-sm font-medium whitespace-nowrap ${
                    isActive ? 'bg-brand-100 text-brand-900' : 'text-stone-600 hover:bg-stone-100'
                  }`
                }
              >
                {link.label}
              </NavLink>
            ))}
          </nav>
          <div className="ml-auto flex items-center gap-3">
            <span className="text-sm text-stone-600">{username}</span>
            <Button variant="ghost" onClick={logout}>
              Log out
            </Button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-8">
        <Outlet />
      </main>
    </div>
  )
}
