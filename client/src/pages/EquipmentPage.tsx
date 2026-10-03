import { useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import type { BrewMethod, Equipment, EquipmentInput } from '../api/types'
import { useDeleteEquipment, useEquipment, useSaveEquipment } from '../hooks/useEquipment'
import { Alert } from '../components/ui/Alert'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { SelectField, TextField } from '../components/ui/Field'
import { PageHeader } from '../components/ui/PageHeader'
import { EmptyState, Loading, QueryError } from '../components/ui/States'
import { BREW_METHODS, label } from '../lib/brewing'

const EMPTY = { name: '', method: '' as BrewMethod | '', brand: '' }

export function EquipmentPage() {
  const equipment = useEquipment()
  const save = useSaveEquipment()
  const remove = useDeleteEquipment()
  const [editing, setEditing] = useState<Equipment | null>(null)
  const [values, setValues] = useState(EMPTY)

  function startEdit(item: Equipment) {
    setEditing(item)
    setValues({ name: item.name, method: item.method, brand: item.brand ?? '' })
    save.reset()
  }

  function reset() {
    setEditing(null)
    setValues(EMPTY)
    save.reset()
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const body = { name: values.name, method: values.method || undefined, brand: values.brand.trim() || undefined } as EquipmentInput
    save.mutate({ id: editing?.id, body }, { onSuccess: reset })
  }

  function handleDelete(item: Equipment) {
    if (window.confirm(`Delete "${item.name}"?`)) {
      remove.mutate(item.id)
    }
  }

  const error = save.error instanceof ApiError ? save.error : null

  return (
    <>
      <PageHeader title="Equipment" description="Brewers, each with its method." />

      <Card className="mb-6">
        <h2 className="mb-4 font-medium">{editing ? `Edit ${editing.name}` : 'Add equipment'}</h2>
        <form onSubmit={handleSubmit} noValidate className="grid gap-4 sm:grid-cols-4 sm:items-start">
          <TextField label="Name" value={values.name} placeholder="Hario V60"
            onChange={(e) => setValues({ ...values, name: e.target.value })} error={error?.fieldErrors.name} />
          <SelectField label="Method" value={values.method}
            onChange={(e) => setValues({ ...values, method: e.target.value as BrewMethod })}
            error={error?.fieldErrors.method}>
            <option value="">Choose…</option>
            {BREW_METHODS.map((m) => <option key={m} value={m}>{label(m)}</option>)}
          </SelectField>
          <TextField label="Brand" value={values.brand} placeholder="Optional"
            onChange={(e) => setValues({ ...values, brand: e.target.value })} error={error?.fieldErrors.brand} />
          <div className="flex gap-2 sm:pt-6">
            <Button type="submit" disabled={save.isPending}>{editing ? 'Save' : 'Add'}</Button>
            {editing && <Button variant="secondary" onClick={reset}>Cancel</Button>}
          </div>
        </form>
      </Card>

      {remove.isError && <div className="mb-4"><Alert>{remove.error.message}</Alert></div>}

      {equipment.isPending ? <Loading /> : equipment.isError ? <QueryError error={equipment.error} /> : equipment.data.length === 0 ? (
        <EmptyState title="No equipment yet">Add your first brewer above.</EmptyState>
      ) : (
        <ul className="divide-y divide-stone-100 rounded-xl bg-white shadow-sm ring-1 ring-stone-200">
          {equipment.data.map((item) => (
            <li key={item.id} className="flex flex-wrap items-center justify-between gap-3 px-4 py-3">
              <div>
                <p className="font-medium">{item.name}</p>
                <p className="text-sm text-stone-500">{label(item.method)}{item.brand ? ` · ${item.brand}` : ''}</p>
              </div>
              <div className="flex gap-2">
                <Button variant="secondary" className="px-3 py-1" onClick={() => startEdit(item)}>Edit</Button>
                <Button variant="danger" className="px-3 py-1" onClick={() => handleDelete(item)}>Delete</Button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
