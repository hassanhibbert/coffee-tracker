import type { ReactNode } from 'react'
import { Alert } from './Alert'

export function Loading({ label = 'Loading…' }: { label?: string }) {
  return (
    <p role="status" className="py-10 text-center text-sm text-stone-500">
      {label}
    </p>
  )
}

export function QueryError({ error }: { error: Error }) {
  return <Alert>Couldn't load data: {error.message}</Alert>
}

export function EmptyState({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="rounded-xl border-2 border-dashed border-stone-300 px-6 py-10 text-center">
      <p className="font-medium text-stone-800">{title}</p>
      {children && <div className="mt-2 text-sm text-stone-600">{children}</div>}
    </div>
  )
}
