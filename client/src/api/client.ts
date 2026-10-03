import createClient, { type Middleware } from 'openapi-fetch'
import { tokenStore } from '../auth/tokenStore'
import type { paths } from './schema'

/**
 * The server's error body (RFC 9457 Problem Details), as built by GlobalExceptionHandler.
 * `errors` is only present on 400 validation failures: { fieldName: message }.
 */
interface ProblemDetail {
  title?: string
  detail?: string
  status?: number
  errors?: Record<string, string>
}

export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: Record<string, string>

  constructor(status: number, problem: ProblemDetail | undefined) {
    super(problem?.detail ?? problem?.title ?? `Request failed (${status})`)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = problem?.errors ?? {}
  }
}

/** Attaches the token to every request and logs the user out when the server rejects it. */
const authMiddleware: Middleware = {
  onRequest({ request }) {
    const token = tokenStore.get()
    if (token) {
      request.headers.set('Authorization', `Bearer ${token}`)
    }
    return request
  },
  onResponse({ request, response }) {
    // A 401 from a protected endpoint means the token expired or was rejected.
    // (A 401 from /auth/login just means a wrong password; that's not a logout.)
    const isAuthEndpoint = new URL(request.url).pathname.startsWith('/api/v1/auth/')
    if (response.status === 401 && !isAuthEndpoint) {
      tokenStore.clear()
    }
    return response
  },
}

/**
 * A fetch wrapper typed by the generated `paths`: api.GET('/api/v1/brews/{id}', ...) knows
 * the URL exists, which params it takes, and that the response is a BrewResponse.
 * Empty VITE_API_BASE_URL = same origin (dev proxy, or a host that proxies /api).
 */
export const api = createClient<paths>({
  baseUrl: import.meta.env.VITE_API_BASE_URL || globalThis.location?.origin,
  // Look fetch up on every call instead of capturing it once at startup, so tools that
  // patch globalThis.fetch later (MSW in tests) are honoured.
  fetch: (request) => globalThis.fetch(request),
})
api.use(authMiddleware)

/**
 * openapi-fetch never throws on HTTP errors; it returns { data, error, response }.
 * TanStack Query expects a rejected promise for failures, so convert here, once.
 */
export async function unwrap<T>(call: Promise<{ data?: T; error?: unknown; response: Response }>): Promise<T> {
  const { data, error, response } = await call
  if (!response.ok) {
    throw new ApiError(response.status, error as ProblemDetail | undefined)
  }
  return data as T
}
