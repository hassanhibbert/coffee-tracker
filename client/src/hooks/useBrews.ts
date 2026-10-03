import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, unwrap } from '../api/client'
import type { paths } from '../api/schema'
import type { BrewInput } from '../api/types'

/** Exactly the query parameters GET /api/v1/brews accepts, taken from the generated spec. */
export type BrewQuery = NonNullable<paths['/api/v1/brews']['get']['parameters']['query']>

export function useBrews(query: BrewQuery) {
  return useQuery({
    // The params are part of the key: each filter/page combination is cached separately.
    queryKey: ['brews', 'list', query],
    queryFn: () => unwrap(api.GET('/api/v1/brews', { params: { query } })),
    // While page 2 loads, keep showing page 1 instead of flashing a spinner.
    placeholderData: keepPreviousData,
  })
}

export function useBrew(id: number | undefined) {
  return useQuery({
    queryKey: ['brews', 'detail', id],
    queryFn: () => unwrap(api.GET('/api/v1/brews/{id}', { params: { path: { id: id! } } })),
    enabled: id !== undefined,
  })
}

export function useSaveBrew() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id?: number; body: BrewInput }) =>
      id === undefined
        ? unwrap(api.POST('/api/v1/brews', { body }))
        : unwrap(api.PUT('/api/v1/brews/{id}', { params: { path: { id } }, body })),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['brews'] })
      queryClient.invalidateQueries({ queryKey: ['stats'] })
    },
  })
}

export function useDeleteBrew() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => unwrap(api.DELETE('/api/v1/brews/{id}', { params: { path: { id } } })),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['brews'] })
      queryClient.invalidateQueries({ queryKey: ['stats'] })
    },
  })
}
