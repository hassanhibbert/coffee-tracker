import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { tokenStore } from '../auth/tokenStore'
import { server } from './server'

// MSW intercepts fetch at the network layer, so components run their real code paths
// (api client, middleware, unwrap) against fake HTTP responses.
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }))
afterEach(() => {
  server.resetHandlers()
  cleanup()
  tokenStore.clear()
  sessionStorage.clear()
})
afterAll(() => server.close())
