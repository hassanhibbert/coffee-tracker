import type { components } from './schema'

// Friendly names for the generated schema types. If a Java DTO changes and you rerun
// `npm run gen:api`, every component using these types is re-checked by the compiler.
type Schemas = components['schemas']

export type Bean = Schemas['BeanResponse']
export type BeanInput = Schemas['BeanRequest']
export type Equipment = Schemas['EquipmentResponse']
export type EquipmentInput = Schemas['EquipmentRequest']
export type Brew = Schemas['BrewResponse']
export type BrewInput = Schemas['BrewRequest']
export type BrewPage = Schemas['PageResponseBrewResponse']
export type Stats = Schemas['StatsResponse']
export type TokenResponse = Schemas['TokenResponse']

// Union types pulled out of the schema: 'ESPRESSO' | 'MOKA_POT' | ...
export type BrewMethod = Equipment['method']
export type GrindSize = Brew['grindSize']
export type RoastLevel = Bean['roastLevel']
