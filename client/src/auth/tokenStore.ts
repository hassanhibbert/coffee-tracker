import { decodeToken } from './jwt'

const STORAGE_KEY = 'coffee-tracker.token'

/**
 * The token lives here, outside React, because non-React code (the fetch middleware) needs
 * it too. React components subscribe through useAuth() (useSyncExternalStore).
 *
 * Kept in memory and mirrored to sessionStorage: survives a page refresh, gone when the tab
 * closes. Any script on the page can read sessionStorage, so this relies on there being no
 * XSS (React escapes rendered text by default). An httpOnly cookie is the stronger option.
 */
let token: string | null = readStorage()
const listeners = new Set<() => void>()

/** A stored token that is unreadable or already expired is treated as no token. */
function readStorage(): string | null {
  try {
    const stored = sessionStorage.getItem(STORAGE_KEY)
    const claims = stored ? decodeToken(stored) : null
    if (!stored || !claims || claims.expiresAt.getTime() <= Date.now()) {
      sessionStorage.removeItem(STORAGE_KEY)
      return null
    }
    return stored
  } catch {
    return null // storage can be unavailable (privacy modes); memory still works
  }
}

function emit() {
  listeners.forEach((listener) => listener())
}

export const tokenStore = {
  get: (): string | null => token,

  set(value: string) {
    token = value
    try {
      sessionStorage.setItem(STORAGE_KEY, value)
    } catch {
      /* memory only */
    }
    emit()
  },

  clear() {
    if (token === null) return
    token = null
    try {
      sessionStorage.removeItem(STORAGE_KEY)
    } catch {
      /* nothing stored */
    }
    emit()
  },

  subscribe(listener: () => void): () => void {
    listeners.add(listener)
    return () => listeners.delete(listener)
  },
}
