import api from './client'
import type { ClientShort, SalonSettings } from '../types'

export const getClients = () =>
  api.get<ClientShort[]>('/users/clients').then((r) => r.data)

export const getSalonSettings = () =>
  api.get<SalonSettings>('/settings').then((r) => r.data)

export interface Me {
  id: number
  name: string
  email: string
  phone: string
  role: string
  telegramLinked: boolean
  phoneVerified: boolean
}

export const getMe = () =>
  api.get<Me>('/users/me').then((r) => r.data)

/** Ссылка на бота с одноразовым кодом */
export const createTelegramLink = () =>
  api.post<{ url: string; bot: string }>('/users/me/telegram-link').then((r) => r.data)

export const unlinkTelegram = () =>
  api.delete('/users/me/telegram')
