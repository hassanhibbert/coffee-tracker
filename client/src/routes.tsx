import type { RouteObject } from 'react-router'
import { RequireAuth } from './auth/RequireAuth'
import { Layout } from './components/Layout'
import { AuthPage } from './pages/AuthPage'
import { BeansPage } from './pages/BeansPage'
import { BrewFormPage } from './pages/BrewFormPage'
import { BrewsPage } from './pages/BrewsPage'
import { DashboardPage } from './pages/DashboardPage'
import { EquipmentPage } from './pages/EquipmentPage'
import { NotFoundPage } from './pages/NotFoundPage'

// Exported as plain data so tests can mount the same routes in a memory router.
export const routes: RouteObject[] = [
  { path: '/login', element: <AuthPage mode="login" /> },
  { path: '/register', element: <AuthPage mode="register" /> },
  {
    element: <RequireAuth />,
    children: [
      {
        element: <Layout />,
        children: [
          { index: true, element: <DashboardPage /> },
          { path: 'brews', element: <BrewsPage /> },
          { path: 'brews/new', element: <BrewFormPage /> },
          { path: 'brews/:id/edit', element: <BrewFormPage /> },
          { path: 'beans', element: <BeansPage /> },
          { path: 'equipment', element: <EquipmentPage /> },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },
]
