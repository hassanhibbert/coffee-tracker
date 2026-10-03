export function Rating({ value }: { value: number | null }) {
  if (value === null) {
    return <span className="text-sm text-stone-400">Unrated</span>
  }
  return (
    <span className="text-amber-500" aria-label={`${value} out of 5`} title={`${value} / 5`}>
      {'★'.repeat(value)}
      <span className="text-stone-300">{'★'.repeat(5 - value)}</span>
    </span>
  )
}
