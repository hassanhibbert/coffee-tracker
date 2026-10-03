import type { ReactNode } from 'react'

/** role="alert" makes screen readers announce the message as soon as it appears. */
export function Alert({ children, tone = 'error' }: { children: ReactNode; tone?: 'error' | 'warning' }) {
  const styles = tone === 'error' ? 'bg-red-50 text-red-800 ring-red-200' : 'bg-amber-50 text-amber-900 ring-amber-200'
  return (
    <div role="alert" className={`rounded-lg px-4 py-3 text-sm ring-1 ${styles}`}>
      {children}
    </div>
  )
}
