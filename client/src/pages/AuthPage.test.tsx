import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { tokenStore } from '../auth/tokenStore'
import { fakeToken, renderApp } from '../test/render'
import { server } from '../test/server'

const emptyStats = { totalBrews: 0, averageRating: null, byMethod: [], topBeans: [] }
const emptyPage = { content: [], page: 0, size: 5, totalElements: 0, totalPages: 0 }

describe('authentication', () => {
  it('sends logged-out users to the login page', async () => {
    const { router } = renderApp('/brews')

    expect(await screen.findByRole('heading', { name: /sign in/i })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/login')
  })

  it('logs in, stores the token, and returns to the page originally requested', async () => {
    let sentBody: unknown
    server.use(
      http.post('*/api/v1/auth/login', async ({ request }) => {
        sentBody = await request.json()
        return HttpResponse.json({ accessToken: fakeToken(), tokenType: 'Bearer', expiresIn: 3600 })
      }),
      http.get('*/api/v1/stats', () => HttpResponse.json(emptyStats)),
      http.get('*/api/v1/brews', () => HttpResponse.json(emptyPage)),
    )
    const user = userEvent.setup()
    const { router } = renderApp('/')

    await user.type(await screen.findByLabelText('Username'), 'alice')
    await user.type(screen.getByLabelText('Password'), 'correct-horse')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('heading', { name: 'Dashboard' })).toBeInTheDocument()
    expect(sentBody).toEqual({ username: 'alice', password: 'correct-horse' })
    expect(tokenStore.get()).not.toBeNull()
    expect(router.state.location.pathname).toBe('/')
  })

  it("shows the server's message for wrong credentials", async () => {
    server.use(
      http.post('*/api/v1/auth/login', () =>
        HttpResponse.json(
          { title: 'Authentication failed', status: 401, detail: 'Invalid username or password' },
          { status: 401 },
        ),
      ),
    )
    const user = userEvent.setup()
    renderApp('/login')

    await user.type(await screen.findByLabelText('Username'), 'alice')
    await user.type(screen.getByLabelText('Password'), 'wrong-pass')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid username or password')
    expect(tokenStore.get()).toBeNull()
  })

  it('shows per-field validation errors next to the fields on register', async () => {
    server.use(
      http.post('*/api/v1/auth/register', () =>
        HttpResponse.json(
          { title: 'Validation failed', status: 400, errors: { password: 'size must be between 8 and 72' } },
          { status: 400 },
        ),
      ),
    )
    const user = userEvent.setup()
    renderApp('/register')

    await user.type(await screen.findByLabelText('Username'), 'alice')
    await user.type(screen.getByLabelText('Password'), 'short')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    // toHaveAccessibleDescription proves the message is linked via aria-describedby,
    // i.e. a screen reader announces it with the field, not just that it's on screen.
    expect(await screen.findByLabelText('Password')).toHaveAccessibleDescription('size must be between 8 and 72')
  })

  it('logs the user out when the API rejects their token', async () => {
    server.use(
      http.get('*/api/v1/stats', () => new HttpResponse(null, { status: 401 })),
      http.get('*/api/v1/brews', () => new HttpResponse(null, { status: 401 })),
    )
    const { router } = renderApp('/', { loggedIn: true })

    expect(await screen.findByRole('heading', { name: /sign in/i })).toBeInTheDocument()
    expect(tokenStore.get()).toBeNull()
    expect(router.state.location.pathname).toBe('/login')
  })
})
