export type Variant = 'primary' | 'secondary' | 'danger' | 'ghost'

const variants: Record<Variant, string> = {
  primary: 'bg-brand-700 text-white hover:bg-brand-800 focus-visible:outline-brand-700',
  secondary: 'bg-white text-stone-800 ring-1 ring-stone-300 hover:bg-stone-100 focus-visible:outline-stone-500',
  danger: 'bg-white text-red-700 ring-1 ring-red-200 hover:bg-red-50 focus-visible:outline-red-600',
  ghost: 'text-stone-700 hover:bg-stone-100 focus-visible:outline-stone-500',
}

export function buttonClasses(variant: Variant = 'primary', extra = '') {
  return `inline-flex items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-medium transition-colors
    focus-visible:outline-2 focus-visible:outline-offset-2 disabled:cursor-not-allowed disabled:opacity-50
    ${variants[variant]} ${extra}`
}
