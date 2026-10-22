import api from './client'
import type { ClientShort, SalonSettings } from '../types'

export const getClients = () =>
  api.get<ClientShort[]>('/users/clients').then((r) => r.data)

export const getSalonSettings = () =>
  api.get<SalonSettings>('/settings').then((r) => r.data)
