import { useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import type { Bean, BeanInput, RoastLevel } from '../api/types'
import { useBeans, useDeleteBean, useSaveBean } from '../hooks/useBeans'
import { Alert } from '../components/ui/Alert'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { SelectField, TextField } from '../components/ui/Field'
import { PageHeader } from '../components/ui/PageHeader'
import { EmptyState, Loading, QueryError } from '../components/ui/States'
import { label, ROAST_LEVELS } from '../lib/brewing'

const EMPTY = { name: '', roastLevel: '' as RoastLevel | '', countryOrigin: '' }

export function BeansPage() {
  const beans = useBeans()
  const save = useSaveBean()
  const remove = useDeleteBean()
  const [editing, setEditing] = useState<Bean | null>(null)
  const [values, setValues] = useState(EMPTY)

  function startEdit(bean: Bean) {
    setEditing(bean)
    setValues({ name: bean.name, roastLevel: bean.roastLevel, countryOrigin: bean.countryOrigin })
    save.reset()
  }

  function reset() {
    setEditing(null)
    setValues(EMPTY)
    save.reset()
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    const body = { ...values, roastLevel: values.roastLevel || undefined } as BeanInput
    save.mutate({ id: editing?.id, body }, { onSuccess: reset })
  }

  function handleDelete(bean: Bean) {
    if (window.confirm(`Delete "${bean.name}"?`)) {
      remove.mutate(bean.id)
    }
  }

  const error = save.error instanceof ApiError ? save.error : null

  return (
    <>
      <PageHeader title="Beans" description="The coffees you brew with." />

      <Card className="mb-6">
        <h2 className="mb-4 font-medium">{editing ? `Edit ${editing.name}` : 'Add a bean'}</h2>
        <form onSubmit={handleSubmit} noValidate className="grid gap-4 sm:grid-cols-4 sm:items-start">
          <TextField label="Name" value={values.name} placeholder="Ethiopia Guji"
            onChange={(e) => setValues({ ...values, name: e.target.value })} error={error?.fieldErrors.name} />
          <SelectField label="Roast" value={values.roastLevel}
            onChange={(e) => setValues({ ...values, roastLevel: e.target.value as RoastLevel })}
            error={error?.fieldErrors.roastLevel}>
            <option value="">Choose…</option>
            {ROAST_LEVELS.map((r) => <option key={r} value={r}>{label(r)}</option>)}
          </SelectField>
          <TextField label="Origin" value={values.countryOrigin} placeholder="Ethiopia"
            onChange={(e) => setValues({ ...values, countryOrigin: e.target.value })} error={error?.fieldErrors.countryOrigin} />
          <div className="flex gap-2 sm:pt-6">
            <Button type="submit" disabled={save.isPending}>{editing ? 'Save' : 'Add'}</Button>
            {editing && <Button variant="secondary" onClick={reset}>Cancel</Button>}
          </div>
        </form>
      </Card>

      {/* 409 from the server: the bean is still used by brews. */}
      {remove.isError && <div className="mb-4"><Alert>{remove.error.message}</Alert></div>}

      {beans.isPending ? <Loading /> : beans.isError ? <QueryError error={beans.error} /> : beans.data.length === 0 ? (
        <EmptyState title="No beans yet">Add your first bean above.</EmptyState>
      ) : (
        <ul className="divide-y divide-stone-100 rounded-xl bg-white shadow-sm ring-1 ring-stone-200">
          {beans.data.map((bean) => (
            <li key={bean.id} className="flex flex-wrap items-center justify-between gap-3 px-4 py-3">
              <div>
                <p className="font-medium">{bean.name}</p>
                <p className="text-sm text-stone-500">{label(bean.roastLevel)} roast · {bean.countryOrigin}</p>
              </div>
              <div className="flex gap-2">
                <Button variant="secondary" className="px-3 py-1" onClick={() => startEdit(bean)}>Edit</Button>
                <Button variant="danger" className="px-3 py-1" onClick={() => handleDelete(bean)}>Delete</Button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
