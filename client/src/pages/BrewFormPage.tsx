import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { ApiError } from '../api/client'
import type { Bean, Brew, BrewInput, Equipment, GrindSize } from '../api/types'
import { useBeans } from '../hooks/useBeans'
import { useBrew, useSaveBrew } from '../hooks/useBrews'
import { useEquipment } from '../hooks/useEquipment'
import { Alert } from '../components/ui/Alert'
import { Button } from '../components/ui/Button'
import { buttonClasses } from '../components/ui/buttonClasses'
import { Card } from '../components/ui/Card'
import { SelectField, TextAreaField, TextField } from '../components/ui/Field'
import { PageHeader } from '../components/ui/PageHeader'
import { EmptyState, Loading, QueryError } from '../components/ui/States'
import { GRIND_RANGE, GRIND_SIZES, grindSuits, label, ratio, toLocalInput } from '../lib/brewing'

/** Form state is all strings: that's what inputs give us. Conversion happens once, on submit. */
interface FormValues {
  beanId: string
  equipmentId: string
  grindSize: GrindSize | ''
  coffeeGrams: string
  waterGrams: string
  waterTempC: string
  brewTimeSeconds: string
  rating: string
  notes: string
  brewedAt: string
}

const EMPTY: FormValues = {
  beanId: '',
  equipmentId: '',
  grindSize: '',
  coffeeGrams: '',
  waterGrams: '',
  waterTempC: '',
  brewTimeSeconds: '',
  rating: '',
  notes: '',
  brewedAt: '',
}

function fromBrew(brew: Brew): FormValues {
  return {
    beanId: String(brew.bean.id),
    equipmentId: String(brew.equipment.id),
    grindSize: brew.grindSize,
    coffeeGrams: String(brew.coffeeGrams),
    waterGrams: String(brew.waterGrams),
    waterTempC: brew.waterTempC?.toString() ?? '',
    brewTimeSeconds: String(brew.brewTimeSeconds),
    rating: brew.rating?.toString() ?? '',
    notes: brew.notes ?? '',
    brewedAt: toLocalInput(brew.brewedAt),
  }
}

const optionalNumber = (value: string) => (value.trim() === '' ? undefined : Number(value))

/**
 * Empty required fields are sent as undefined (omitted from the JSON), and the server's
 * @NotNull answers with a field error that the form displays. The server stays the single
 * source of truth for validation. That's why this builds a Partial and casts once,
 * deliberately, instead of pretending every field is filled in.
 */
function toRequest(v: FormValues): BrewInput {
  const body: Partial<BrewInput> = {
    beanId: optionalNumber(v.beanId),
    equipmentId: optionalNumber(v.equipmentId),
    grindSize: v.grindSize || undefined,
    coffeeGrams: optionalNumber(v.coffeeGrams),
    waterGrams: optionalNumber(v.waterGrams),
    waterTempC: optionalNumber(v.waterTempC),
    brewTimeSeconds: optionalNumber(v.brewTimeSeconds),
    rating: optionalNumber(v.rating),
    notes: v.notes.trim() || undefined,
    // datetime-local has no time zone; new Date() reads it as local time, toISOString() sends UTC.
    brewedAt: v.brewedAt ? new Date(v.brewedAt).toISOString() : undefined,
  }
  return body as BrewInput
}

export function BrewFormPage() {
  const { id } = useParams()
  const brewId = id === undefined ? undefined : Number(id)
  const brew = useBrew(brewId)
  const beans = useBeans()
  const equipment = useEquipment()

  if (beans.isPending || equipment.isPending || (brewId !== undefined && brew.isPending)) return <Loading />
  if (beans.isError) return <QueryError error={beans.error} />
  if (equipment.isError) return <QueryError error={equipment.error} />
  if (brew.isError) return <QueryError error={brew.error} />

  if (beans.data.length === 0 || equipment.data.length === 0) {
    return (
      <EmptyState title="You need a bean and some equipment first">
        <Link to="/beans" className="font-medium text-brand-700 underline">Add beans</Link> ·{' '}
        <Link to="/equipment" className="font-medium text-brand-700 underline">Add equipment</Link>
      </EmptyState>
    )
  }

  // Mount the form only once its data exists, with that data as initial state. No effect
  // copying props into state, and `key` gives a fresh form if the id ever changes.
  return (
    <BrewForm
      key={brewId ?? 'new'}
      brewId={brewId}
      initial={brew.data ? fromBrew(brew.data) : EMPTY}
      beans={beans.data}
      equipment={equipment.data}
    />
  )
}

interface BrewFormProps {
  brewId?: number
  initial: FormValues
  beans: Bean[]
  equipment: Equipment[]
}

function BrewForm({ brewId, initial, beans, equipment }: BrewFormProps) {
  const [values, setValues] = useState(initial)
  const save = useSaveBrew()
  const navigate = useNavigate()

  // One generic setter: `field` must be a key of FormValues, checked by the compiler.
  const set = (field: keyof FormValues) => (e: { target: { value: string } }) =>
    setValues((prev) => ({ ...prev, [field]: e.target.value }))

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    save.mutate({ id: brewId, body: toRequest(values) }, { onSuccess: () => navigate('/brews') })
  }

  const selectedEquipment = equipment.find((e) => String(e.id) === values.equipmentId)
  const method = selectedEquipment?.method
  const grindWarning =
    method && values.grindSize && !grindSuits(method, values.grindSize)
      ? `${label(values.grindSize)} is outside the usual range for ${label(method)}.`
      : undefined
  const preview = ratio(Number(values.coffeeGrams), Number(values.waterGrams))

  const error = save.error instanceof ApiError ? save.error : null
  const fieldError = (name: keyof FormValues) => error?.fieldErrors[name]
  const hasFieldErrors = error && Object.keys(error.fieldErrors).length > 0

  return (
    <>
      <PageHeader title={brewId === undefined ? 'Log a brew' : 'Edit brew'} />
      <Card>
        <form onSubmit={handleSubmit} noValidate className="space-y-6">
          {error && !hasFieldErrors && <Alert>{error.message}</Alert>}
          {hasFieldErrors && <Alert>Please fix the highlighted fields.</Alert>}

          <div className="grid gap-4 sm:grid-cols-2">
            <SelectField label="Bean" value={values.beanId} onChange={set('beanId')} error={fieldError('beanId')}>
              <option value="">Choose a bean…</option>
              {beans.map((b) => (
                <option key={b.id} value={b.id}>{b.name}</option>
              ))}
            </SelectField>
            <SelectField
              label="Equipment"
              value={values.equipmentId}
              onChange={set('equipmentId')}
              error={fieldError('equipmentId')}
            >
              <option value="">Choose equipment…</option>
              {equipment.map((e) => (
                <option key={e.id} value={e.id}>{e.name} ({label(e.method)})</option>
              ))}
            </SelectField>
          </div>

          <SelectField
            label="Grind size"
            value={values.grindSize}
            onChange={set('grindSize')}
            error={fieldError('grindSize')}
            hint={
              method
                ? `${label(method)}: ${label(GRIND_RANGE[method][0])} to ${label(GRIND_RANGE[method][1])}`
                : 'Pick equipment to see the recommended range.'
            }
          >
            <option value="">Choose a grind…</option>
            {GRIND_SIZES.map((g) => (
              <option key={g} value={g}>{label(g)}</option>
            ))}
          </SelectField>
          {grindWarning && <Alert tone="warning">{grindWarning} The server will reject it.</Alert>}

          <div className="grid gap-4 sm:grid-cols-3">
            <TextField label="Coffee (g)" type="number" inputMode="decimal" step="0.1" min="0"
              value={values.coffeeGrams} onChange={set('coffeeGrams')} error={fieldError('coffeeGrams')} />
            <TextField label="Water (g)" type="number" inputMode="decimal" step="0.1" min="0"
              value={values.waterGrams} onChange={set('waterGrams')} error={fieldError('waterGrams')}
              hint={preview ? `Ratio ${preview}` : undefined} />
            <TextField label="Water temp (°C)" type="number" inputMode="numeric" min="0" max="100"
              value={values.waterTempC} onChange={set('waterTempC')} error={fieldError('waterTempC')}
              hint="Optional: leave empty for cold brew." />
          </div>

          <div className="grid gap-4 sm:grid-cols-3">
            <TextField label="Brew time (seconds)" type="number" inputMode="numeric" min="1"
              value={values.brewTimeSeconds} onChange={set('brewTimeSeconds')} error={fieldError('brewTimeSeconds')} />
            <SelectField label="Rating" value={values.rating} onChange={set('rating')} error={fieldError('rating')}>
              <option value="">Not rated</option>
              {[5, 4, 3, 2, 1].map((r) => (
                <option key={r} value={r}>{'★'.repeat(r)} ({r})</option>
              ))}
            </SelectField>
            <TextField label="Brewed at" type="datetime-local" value={values.brewedAt} onChange={set('brewedAt')}
              error={fieldError('brewedAt')} hint="Optional: defaults to now." />
          </div>

          <TextAreaField label="Notes" rows={3} maxLength={2000} value={values.notes} onChange={set('notes')}
            error={fieldError('notes')} placeholder="Tasting notes, what you'd change next time…" />

          <div className="flex gap-3">
            <Button type="submit" disabled={save.isPending}>
              {save.isPending ? 'Saving…' : 'Save brew'}
            </Button>
            <Link to="/brews" className={buttonClasses('secondary')}>Cancel</Link>
          </div>
        </form>
      </Card>
    </>
  )
}
