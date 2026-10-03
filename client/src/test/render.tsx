import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { tokenStore } from '../auth/tokenStore'
import { routes } from '../routes'

/** A JWT-shaped string the client can decode. The signature is junk: only the server checks it. */
export function fakeToken({ username = 'alice', expiresInSeconds = 3600 } = {}) {
  const encode = (value: object) => btoa(JSON.stringify(value)).replace(/=+$/, '')
  const payload = { sub: '1', username, exp: Math.floor(Date.now() / 1000) + expiresInSeconds }
  return `${encode({ alg: 'HS256' })}.${encode(payload)}.signature`
}

/**
 * Renders the real app routes at `path`, with a fresh QueryClient per test (no cache shared
 * between tests) and retries off (a failing request should fail the test immediately).
 */
export function renderApp(path: string, { loggedIn = false } = {}) {
  if (loggedIn) tokenStore.set(fakeToken())
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  render(
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
  return { router }
}
