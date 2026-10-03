import type { ReactNode } from 'react'

export function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <section className={`rounded-xl bg-white p-5 shadow-sm ring-1 ring-stone-200 ${className}`}>{children}</section>
}
