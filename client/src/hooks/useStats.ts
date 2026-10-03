import { useQuery } from '@tanstack/react-query'
import { api, unwrap } from '../api/client'

export function useStats() {
  return useQuery({
    queryKey: ['stats'],
    queryFn: () => unwrap(api.GET('/api/v1/stats')),
  })
}
