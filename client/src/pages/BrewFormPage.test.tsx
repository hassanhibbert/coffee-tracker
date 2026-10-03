import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { renderApp } from '../test/render'
import { server } from '../test/server'

const beans = [{ id: 1, name: 'Ethiopia Guji', roastLevel: 'LIGHT', countryOrigin: 'Ethiopia' }]
const equipment = [
  { id: 2, name: 'Gaggia Classic', method: 'ESPRESSO', brand: 'Gaggia' },
  { id: 3, name: 'Phin', method: 'PHIN', brand: null },
]
const emptyPage = { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0 }

describe('BrewFormPage', () => {
  beforeEach(() => {
    server.use(
      http.get('*/api/v1/beans', () => HttpResponse.json(beans)),
      http.get('*/api/v1/equipment', () => HttpResponse.json(equipment)),
      http.get('*/api/v1/brews', () => HttpResponse.json(emptyPage)),
    )
  })

  it('warns immediately when the grind does not suit the method', async () => {
    const user = userEvent.setup()
    renderApp('/brews/new', { loggedIn: true })

    await user.selectOptions(await screen.findByLabelText('Equipment'), '2')
    expect(screen.getByLabelText('Grind size')).toHaveAccessibleDescription('Espresso: Extra fine to Fine')

    await user.selectOptions(screen.getByLabelText('Grind size'), 'COARSE')
    expect(screen.getByRole('alert')).toHaveTextContent('Coarse is outside the usual range for Espresso')
  })

  it('converts the form into the typed request and returns to the list', async () => {
    let sent: Record<string, unknown> | undefined
    server.use(
      http.post('*/api/v1/brews', async ({ request }) => {
        sent = (await request.json()) as Record<string, unknown>
        return HttpResponse.json({ id: 99 }, { status: 201 })
      }),
    )
    const user = userEvent.setup()
    const { router } = renderApp('/brews/new', { loggedIn: true })

    await user.selectOptions(await screen.findByLabelText('Bean'), '1')
    await user.selectOptions(screen.getByLabelText('Equipment'), '3')
    await user.selectOptions(screen.getByLabelText('Grind size'), 'FINE')
    await user.type(screen.getByLabelText('Coffee (g)'), '20')
    await user.type(screen.getByLabelText('Water (g)'), '100')
    expect(screen.getByLabelText('Water (g)')).toHaveAccessibleDescription('Ratio 1:5.0')
    await user.type(screen.getByLabelText('Brew time (seconds)'), '300')
    await user.click(screen.getByRole('button', { name: 'Save brew' }))

    await screen.findByRole('heading', { name: 'Brews' })
    expect(router.state.location.pathname).toBe('/brews')
    // Numbers, not strings; optional empties omitted entirely.
    expect(sent).toEqual({ beanId: 1, equipmentId: 3, grindSize: 'FINE', coffeeGrams: 20, waterGrams: 100, brewTimeSeconds: 300 })
  })

  it("shows the server's field errors and rule violations", async () => {
    server.use(
      http.post('*/api/v1/brews', () =>
        HttpResponse.json(
          { title: 'Validation failed', status: 400, errors: { coffeeGrams: 'is required' } },
          { status: 400 },
        ),
      ),
    )
    const user = userEvent.setup()
    renderApp('/brews/new', { loggedIn: true })

    await user.click(await screen.findByRole('button', { name: 'Save brew' }))

    expect(await screen.findByLabelText('Coffee (g)')).toHaveAccessibleDescription('is required')
    expect(screen.getByLabelText('Coffee (g)')).toBeInvalid()
  })
})
