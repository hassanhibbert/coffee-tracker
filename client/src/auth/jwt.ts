export interface TokenClaims {
  userId: number
  username: string
  expiresAt: Date
  /** The shared demo account. The server refuses its writes; the UI uses this to explain why. */
  isDemo: boolean
}

/**
 * Reads the claims in the middle segment of a JWT (header.payload.signature).
 *
 * This does NOT verify the signature. The browser has no key to check it with, and it
 * doesn't need one: it only uses the claims for display and to know when the token
 * expires. The server verifies the signature on every request.
 */
export function decodeToken(token: string): TokenClaims | null {
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    const payload = JSON.parse(atob(base64)) as { sub: string; username: string; exp: number; scope?: string }
    return {
      userId: Number(payload.sub),
      username: payload.username,
      expiresAt: new Date(payload.exp * 1000), // exp is in seconds; Date wants milliseconds
      isDemo: payload.scope?.split(' ').includes('demo') ?? false, // OAuth2 scopes are space-separated
    }
  } catch {
    return null
  }
}
