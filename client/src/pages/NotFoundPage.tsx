import { Link } from 'react-router'
import { EmptyState } from '../components/ui/States'

export function NotFoundPage() {
  return (
    <EmptyState title="Page not found">
      <Link to="/" className="font-medium text-brand-700 underline">Back to the dashboard</Link>
    </EmptyState>
  )
}
