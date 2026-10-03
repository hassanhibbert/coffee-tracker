import { Link } from 'react-router'
import { useBrews } from '../hooks/useBrews'
import { useStats } from '../hooks/useStats'
import { buttonClasses } from '../components/ui/buttonClasses'
import { Card } from '../components/ui/Card'
import { PageHeader } from '../components/ui/PageHeader'
import { Rating } from '../components/ui/Rating'
import { EmptyState, Loading, QueryError } from '../components/ui/States'
import { formatDate, label } from '../lib/brewing'

export function DashboardPage() {
  const stats = useStats()
  const recent = useBrews({ size: 5 })

  if (stats.isPending) return <Loading />
  if (stats.isError) return <QueryError error={stats.error} />

  const { totalBrews, averageRating, byMethod, topBeans } = stats.data
  const maxCount = Math.max(1, ...byMethod.map((m) => m.brewCount))

  return (
    <>
      <PageHeader
        title="Dashboard"
        description="How your coffee is going."
        action={
          <Link to="/brews/new" className={buttonClasses()}>
            Log a brew
          </Link>
        }
      />

      {totalBrews === 0 ? (
        <EmptyState title="No brews yet">
          Add a <Link to="/beans" className="font-medium text-brand-700 underline">bean</Link> and some{' '}
          <Link to="/equipment" className="font-medium text-brand-700 underline">equipment</Link>, then log your first brew.
        </EmptyState>
      ) : (
        <div className="grid gap-4 md:grid-cols-3">
          <Card>
            <p className="text-sm text-stone-500">Total brews</p>
            <p className="mt-1 text-3xl font-semibold">{totalBrews}</p>
          </Card>
          <Card>
            <p className="text-sm text-stone-500">Average rating</p>
            <p className="mt-1 text-3xl font-semibold">{averageRating !== null ? averageRating.toFixed(1) : '–'}</p>
          </Card>
          <Card>
            <p className="text-sm text-stone-500">Favourite method</p>
            <p className="mt-1 text-3xl font-semibold">{byMethod[0] ? label(byMethod[0].method) : '–'}</p>
          </Card>

          <Card className="md:col-span-2">
            <h2 className="font-medium">Brews by method</h2>
            <ul className="mt-4 space-y-3">
              {byMethod.map((m) => (
                <li key={m.method}>
                  <div className="flex justify-between text-sm">
                    <span>{label(m.method)}</span>
                    <span className="text-stone-500">
                      {m.brewCount} · avg {m.averageRating !== null ? m.averageRating.toFixed(1) : '–'}
                    </span>
                  </div>
                  <div className="mt-1 h-2 rounded-full bg-stone-100">
                    <div className="h-2 rounded-full bg-brand-600" style={{ width: `${(m.brewCount / maxCount) * 100}%` }} />
                  </div>
                </li>
              ))}
            </ul>
          </Card>

          <Card>
            <h2 className="font-medium">Top beans</h2>
            <ol className="mt-4 space-y-3">
              {topBeans.map((b) => (
                <li key={b.beanId} className="text-sm">
                  <p className="font-medium">{b.name}</p>
                  <p className="text-stone-500">
                    {b.averageRating !== null ? `${b.averageRating.toFixed(1)} avg` : 'Unrated'} · {b.brewCount} brews
                  </p>
                </li>
              ))}
            </ol>
          </Card>

          <Card className="md:col-span-3">
            <div className="flex items-center justify-between">
              <h2 className="font-medium">Recent brews</h2>
              <Link to="/brews" className="text-sm font-medium text-brand-700 hover:underline">
                View all
              </Link>
            </div>
            <ul className="mt-3 divide-y divide-stone-100">
              {recent.data?.content.map((brew) => (
                <li key={brew.id} className="flex flex-wrap items-center justify-between gap-2 py-2 text-sm">
                  <span>
                    <span className="font-medium">{brew.bean.name}</span>
                    <span className="text-stone-500"> · {label(brew.equipment.method)} · {brew.ratio}</span>
                  </span>
                  <span className="flex items-center gap-3">
                    <Rating value={brew.rating} />
                    <span className="text-stone-500">{formatDate(brew.brewedAt)}</span>
                  </span>
                </li>
              ))}
            </ul>
          </Card>
        </div>
      )}
    </>
  )
}
