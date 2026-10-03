import type { BrewMethod, GrindSize, RoastLevel } from '../api/types'

// Ordered finest -> coarsest, like the server's GrindSize enum.
// `satisfies` checks every entry is a valid GrindSize without widening the type.
export const GRIND_SIZES = [
  'EXTRA_FINE',
  'FINE',
  'MEDIUM_FINE',
  'MEDIUM',
  'MEDIUM_COARSE',
  'COARSE',
  'EXTRA_COARSE',
] as const satisfies readonly GrindSize[]

export const ROAST_LEVELS = [
  'LIGHT',
  'MEDIUM_LIGHT',
  'MEDIUM',
  'MEDIUM_DARK',
  'DARK',
] as const satisfies readonly RoastLevel[]

/**
 * Client copy of BrewMethod's grind ranges, used only for an instant hint while typing.
 * The server's check is the one that counts. Record<BrewMethod, ...> makes the compiler
 * fail here if the server ever adds a brew method and the types are regenerated.
 */
export const GRIND_RANGE: Record<BrewMethod, readonly [GrindSize, GrindSize]> = {
  ESPRESSO: ['EXTRA_FINE', 'FINE'],
  MOKA_POT: ['FINE', 'MEDIUM_FINE'],
  AEROPRESS: ['FINE', 'MEDIUM'],
  PHIN: ['FINE', 'MEDIUM'],
  POUR_OVER: ['MEDIUM_FINE', 'MEDIUM_COARSE'],
  FRENCH_PRESS: ['MEDIUM_COARSE', 'EXTRA_COARSE'],
  COLD_BREW: ['COARSE', 'EXTRA_COARSE'],
}

export const BREW_METHODS = Object.keys(GRIND_RANGE) as BrewMethod[]

export function grindSuits(method: BrewMethod, grind: GrindSize): boolean {
  const [finest, coarsest] = GRIND_RANGE[method]
  const index = GRIND_SIZES.indexOf(grind)
  return index >= GRIND_SIZES.indexOf(finest) && index <= GRIND_SIZES.indexOf(coarsest)
}

// Proper names that the generic rule below would get wrong.
const LABEL_OVERRIDES: Record<string, string> = { AEROPRESS: 'AeroPress' }

/** 'MEDIUM_FINE' -> 'Medium fine', 'POUR_OVER' -> 'Pour over' */
export function label(value: string): string {
  if (value in LABEL_OVERRIDES) return LABEL_OVERRIDES[value]
  const words = value.toLowerCase().replace(/_/g, ' ')
  return words.charAt(0).toUpperCase() + words.slice(1)
}

/** Same rounding as the server (HALF_UP to one decimal), for the live preview in the form. */
export function ratio(coffeeGrams: number, waterGrams: number): string | null {
  if (!(coffeeGrams > 0) || !(waterGrams > 0)) return null
  return `1:${(Math.round((waterGrams / coffeeGrams) * 10) / 10).toFixed(1)}`
}

/** 185 -> '3:05' */
export function formatDuration(totalSeconds: number): string {
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  if (minutes >= 60) {
    return `${Math.floor(minutes / 60)}h ${minutes % 60}m`
  }
  return `${minutes}:${String(seconds).padStart(2, '0')}`
}

const dateFormat = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' })

export function formatDate(iso: string): string {
  return dateFormat.format(new Date(iso))
}

/** ISO instant -> value for <input type="datetime-local"> in the user's time zone. */
export function toLocalInput(iso: string): string {
  const date = new Date(iso)
  const offsetMs = date.getTimezoneOffset() * 60_000
  return new Date(date.getTime() - offsetMs).toISOString().slice(0, 16)
}
