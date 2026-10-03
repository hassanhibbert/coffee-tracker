import { describe, expect, it } from 'vitest'
import { formatDuration, grindSuits, label, ratio } from './brewing'

describe('grindSuits', () => {
  it.each([
    ['ESPRESSO', 'FINE', true],
    ['ESPRESSO', 'COARSE', false],
    ['PHIN', 'MEDIUM', true],
    ['FRENCH_PRESS', 'MEDIUM', false],
  ] as const)('%s with %s -> %s', (method, grind, expected) => {
    expect(grindSuits(method, grind)).toBe(expected)
  })
})

describe('ratio', () => {
  it('matches the server: water / coffee, one decimal', () => {
    expect(ratio(15, 250)).toBe('1:16.7')
    expect(ratio(18, 36)).toBe('1:2.0')
  })

  it('is null until both amounts are positive', () => {
    expect(ratio(0, 250)).toBeNull()
    expect(ratio(Number(''), 250)).toBeNull() // empty input -> NaN
  })
})

describe('formatting', () => {
  it('labels enum values', () => {
    expect(label('MEDIUM_FINE')).toBe('Medium fine')
    expect(label('AEROPRESS')).toBe('AeroPress')
  })

  it('formats durations', () => {
    expect(formatDuration(185)).toBe('3:05')
    expect(formatDuration(43200)).toBe('12h 0m')
  })
})
