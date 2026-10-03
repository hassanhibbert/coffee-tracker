import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from './useAuth'

/**
 * Route guard. This is a UX feature, not security: anyone can edit client code to skip it.
 * The real protection is the server answering 401 without a valid token.
 */
export function RequireAuth() {
  const { isAuthenticated } = useAuth()
  const location = useLocation()

  if (!isAuthenticated) {
    // Remember where they were headed, so login can send them back there.
    return <Navigate to="/login" replace state={{ from: location }} />
  }
  return <Outlet />
}
