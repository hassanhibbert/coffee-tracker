import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useSyncExternalStore } from 'react'
import { decodeToken } from './jwt'
import { tokenStore } from './tokenStore'

/**
 * Current user, derived from the token. useSyncExternalStore re-renders every component
 * using this hook whenever tokenStore changes (login, logout, or a 401 in the middleware).
 */
export function useAuth() {
  const token = useSyncExternalStore(tokenStore.subscribe, tokenStore.get)
  const claims = token ? decodeToken(token) : null
  const queryClient = useQueryClient()

  const logout = useCallback(() => {
    tokenStore.clear()
  }, [])

  // Expire on time even if the user makes no requests.
  const expiresAt = claims?.expiresAt.getTime()
  useEffect(() => {
    if (expiresAt === undefined) return
    const timer = setTimeout(() => tokenStore.clear(), Math.max(0, expiresAt - Date.now()))
    return () => clearTimeout(timer)
  }, [expiresAt])

  // Whenever we become logged out, drop all cached server data so the next person on this
  // browser can't see the previous user's brews flash on screen.
  useEffect(() => {
    if (!token) queryClient.clear()
  }, [token, queryClient])

  // Expired tokens never get here: tokenStore drops them on load, and the timer above
  // clears a token the moment it expires. Rendering stays pure (no Date.now() in render).
  return {
    isAuthenticated: claims !== null,
    username: claims?.username ?? null,
    isDemo: claims?.isDemo ?? false,
    login: tokenStore.set,
    logout,
  }
}
