import { Link, useSearchParams } from 'react-router'
import type { BrewMethod } from '../api/types'
import { useBeans } from '../hooks/useBeans'
import { useBrews, useDeleteBrew, type BrewQuery } from '../hooks/useBrews'
import { Alert } from '../components/ui/Alert'
import { Button } from '../components/ui/Button'
import { buttonClasses } from '../components/ui/buttonClasses'
import { SelectField } from '../components/ui/Field'
import { PageHeader } from '../components/ui/PageHeader'
import { Rating } from '../components/ui/Rating'
import { EmptyState, Loading, QueryError } from '../components/ui/States'
import { BREW_METHODS, formatDate, formatDuration, label } from '../lib/brewing'

const PAGE_SIZE = 10

const SORTS = [
  { value: 'brewedAt,desc', label: 'Newest first' },
  { value: 'brewedAt,asc', label: 'Oldest first' },
  { value: 'rating,desc', label: 'Highest rated' },
  { value: 'rating,asc', label: 'Lowest rated' },
]

/** Turns URL params (always strings) into the typed query the API expects. */
function toQuery(params: URLSearchParams): BrewQuery {
  const method = params.get('method')
  const beanId = params.get('beanId')
  const minRating = params.get('minRating')
  return {
    method: method && BREW_METHODS.includes(method as BrewMethod) ? (method as BrewMethod) : undefined,
    beanId: beanId ? Number(beanId) : undefined,
    minRating: minRating ? Number(minRating) : undefined,
    sort: [params.get('sort') ?? SORTS[0].value],
    page: Number(params.get('page') ?? 0),
    size: PAGE_SIZE,
  }
}

export function BrewsPage() {
  const [params, setParams] = useSearchParams()
  const query = toQuery(params)
  const brews = useBrews(query)
  const beans = useBeans()
  const deleteBrew = useDeleteBrew()

  function setFilter(key: string, value: string) {
    const next = new URLSearchParams(params)
    if (value) next.set(key, value)
    else next.delete(key)
    next.delete('page') // a new filter starts again from page 1
    setParams(next)
  }

  function goToPage(page: number) {
    const next = new URLSearchParams(params)
    next.set('page', String(page))
    setParams(next)
  }

  function handleDelete(id: number) {
    if (window.confirm('Delete this brew? This cannot be undone.')) {
      deleteBrew.mutate(id)
    }
  }

  return (
    <>
      <PageHeader
        title="Brews"
        description="Every cup, filterable."
        action={
          <Link to="/brews/new" className={buttonClasses()}>
            Log a brew
          </Link>
        }
      />

      <div className="mb-6 grid gap-3 sm:grid-cols-4">
        <SelectField label="Method" value={params.get('method') ?? ''} onChange={(e) => setFilter('method', e.target.value)}>
          <option value="">All methods</option>
          {BREW_METHODS.map((m) => (
            <option key={m} value={m}>
              {label(m)}
            </option>
          ))}
        </SelectField>
        <SelectField label="Bean" value={params.get('beanId') ?? ''} onChange={(e) => setFilter('beanId', e.target.value)}>
          <option value="">All beans</option>
          {beans.data?.map((b) => (
            <option key={b.id} value={b.id}>
              {b.name}
            </option>
          ))}
        </SelectField>
        <SelectField
          label="Minimum rating"
          value={params.get('minRating') ?? ''}
          onChange={(e) => setFilter('minRating', e.target.value)}
        >
          <option value="">Any</option>
          {[5, 4, 3, 2, 1].map((r) => (
            <option key={r} value={r}>
              {r}+ stars
            </option>
          ))}
        </SelectField>
        <SelectField label="Sort" value={params.get('sort') ?? SORTS[0].value} onChange={(e) => setFilter('sort', e.target.value)}>
          {SORTS.map((s) => (
            <option key={s.value} value={s.value}>
              {s.label}
            </option>
          ))}
        </SelectField>
      </div>

      {deleteBrew.isError && <div className="mb-4"><Alert>{deleteBrew.error.message}</Alert></div>}

      {brews.isPending ? (
        <Loading />
      ) : brews.isError ? (
        <QueryError error={brews.error} />
      ) : brews.data.content.length === 0 ? (
        <EmptyState title={params.size > 0 ? 'No brews match these filters' : 'No brews yet'}>
          {params.size > 0 ? 'Try removing a filter.' : <Link to="/brews/new" className="font-medium text-brand-700 underline">Log your first brew</Link>}
        </EmptyState>
      ) : (
        <>
          {/* Faded while a new page/filter loads (keepPreviousData), instead of a spinner. */}
          <ul className={`space-y-3 transition-opacity ${brews.isPlaceholderData ? 'opacity-60' : ''}`}>
            {brews.data.content.map((brew) => (
              <li key={brew.id} className="rounded-xl bg-white p-4 shadow-sm ring-1 ring-stone-200">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <p className="font-medium">
                      {brew.bean.name} <span className="font-normal text-stone-500">with {brew.equipment.name}</span>
                    </p>
                    <p className="text-sm text-stone-500">{formatDate(brew.brewedAt)}</p>
                  </div>
                  <Rating value={brew.rating} />
                </div>
                <dl className="mt-3 flex flex-wrap gap-x-6 gap-y-1 text-sm">
                  <div><dt className="inline text-stone-500">Method </dt><dd className="inline">{label(brew.equipment.method)}</dd></div>
                  <div><dt className="inline text-stone-500">Grind </dt><dd className="inline">{label(brew.grindSize)}</dd></div>
                  <div><dt className="inline text-stone-500">Ratio </dt><dd className="inline">{brew.ratio} ({brew.coffeeGrams}g / {brew.waterGrams}g)</dd></div>
                  {brew.waterTempC !== null && <div><dt className="inline text-stone-500">Temp </dt><dd className="inline">{brew.waterTempC}°C</dd></div>}
                  <div><dt className="inline text-stone-500">Time </dt><dd className="inline">{formatDuration(brew.brewTimeSeconds)}</dd></div>
                </dl>
                {brew.notes && <p className="mt-2 text-sm text-stone-700">{brew.notes}</p>}
                <div className="mt-3 flex gap-2">
                  <Link to={`/brews/${brew.id}/edit`} className={buttonClasses('secondary', 'px-3 py-1')}>
                    Edit
                  </Link>
                  <Button variant="danger" className="px-3 py-1" onClick={() => handleDelete(brew.id)}>
                    Delete
                  </Button>
                </div>
              </li>
            ))}
          </ul>

          <nav aria-label="Pagination" className="mt-6 flex items-center justify-between text-sm">
            <Button variant="secondary" disabled={brews.data.page === 0} onClick={() => goToPage(brews.data.page - 1)}>
              Previous
            </Button>
            <span className="text-stone-600">
              Page {brews.data.page + 1} of {brews.data.totalPages} · {brews.data.totalElements} brews
            </span>
            <Button
              variant="secondary"
              disabled={brews.data.page + 1 >= brews.data.totalPages}
              onClick={() => goToPage(brews.data.page + 1)}
            >
              Next
            </Button>
          </nav>
        </>
      )}
    </>
  )
}
