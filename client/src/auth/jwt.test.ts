import { describe, expect, it } from 'vitest'
import { fakeToken } from '../test/render'
import { decodeToken } from './jwt'

describe('decodeToken', () => {
  it('reads the user and expiry from the payload', () => {
    const claims = decodeToken(fakeToken({ username: 'hassan', expiresInSeconds: 60 }))

    expect(claims?.username).toBe('hassan')
    expect(claims?.userId).toBe(1)
    expect(claims!.expiresAt.getTime()).toBeGreaterThan(Date.now())
  })

  it('returns null for garbage instead of throwing', () => {
    expect(decodeToken('not-a-jwt')).toBeNull()
  })
})
