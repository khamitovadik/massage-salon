import api from './client'
import type { SalonService } from '../types'

export const getServices = () =>
  api.get<SalonService[]>('/services').then((r) => r.data)

export const getService = (id: number) =>
  api.get<SalonService>(`/services/${id}`).then((r) => r.data)

export const createService = (data: {
  name: string; description?: string; price: number; durationMinutes: number
}) => api.post<SalonService>('/services', data).then((r) => r.data)

export const updateService = (id: number, data: Partial<{
  name: string; description: string; price: number; durationMinutes: number; active: boolean
}>) => api.put<SalonService>(`/services/${id}`, data).then((r) => r.data)

export const deleteService = (id: number) =>
  api.delete(`/services/${id}`)
