import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, unwrap } from '../api/client'
import type { EquipmentInput } from '../api/types'

export function useEquipment() {
  return useQuery({
    queryKey: ['equipment'],
    queryFn: () => unwrap(api.GET('/api/v1/equipment')),
  })
}

export function useSaveEquipment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, body }: { id?: number; body: EquipmentInput }) =>
      id === undefined
        ? unwrap(api.POST('/api/v1/equipment', { body }))
        : unwrap(api.PUT('/api/v1/equipment/{id}', { params: { path: { id } }, body })),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['equipment'] })
      queryClient.invalidateQueries({ queryKey: ['brews'] })
      queryClient.invalidateQueries({ queryKey: ['stats'] })
    },
  })
}

export function useDeleteEquipment() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => unwrap(api.DELETE('/api/v1/equipment/{id}', { params: { path: { id } } })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['equipment'] }),
  })
}
