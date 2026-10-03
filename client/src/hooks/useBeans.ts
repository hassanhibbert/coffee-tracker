import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, unwrap } from '../api/client'
import type { BeanInput } from '../api/types'

export function useBeans() {
  return useQuery({
    queryKey: ['beans'],
    queryFn: () => unwrap(api.GET('/api/v1/beans')),
  })
}

export function useSaveBean() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id?: number; body: BeanInput }) =>
      id === undefined
        ? unwrap(api.POST('/api/v1/beans', { body }))
        : unwrap(api.PUT('/api/v1/beans/{id}', { params: { path: { id } }, body })),
    // Brews and stats display bean names, so a rename must refresh them too.
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['beans'] })
      queryClient.invalidateQueries({ queryKey: ['brews'] })
      queryClient.invalidateQueries({ queryKey: ['stats'] })
    },
  })
}

export function useDeleteBean() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => unwrap(api.DELETE('/api/v1/beans/{id}', { params: { path: { id } } })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['beans'] }),
  })
}
