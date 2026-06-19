import dayjs from 'dayjs'
import 'dayjs/locale/ru'
import type { AppointmentStatus, SubscriptionStatus } from '../types'

dayjs.locale('ru')

export function formatDateTime(iso: string): string {
  return dayjs(iso).format('D MMM YYYY, HH:mm')
}

export function formatDate(iso: string): string {
  return dayjs(iso).format('D MMM YYYY')
}

export function statusBadge(status: AppointmentStatus): string {
  switch (status) {
    case 'PENDING': return 'badge-yellow'
    case 'CONFIRMED': return 'badge-blue'
    case 'COMPLETED': return 'badge-green'
    case 'CANCELLED': return 'badge-gray'
    default: return 'badge-gray'
  }
}

export function statusLabel(status: AppointmentStatus): string {
  switch (status) {
    case 'PENDING': return 'Ожидает'
    case 'CONFIRMED': return 'Подтверждено'
    case 'COMPLETED': return 'Завершено'
    case 'CANCELLED': return 'Отменено'
    default: return status
  }
}

export function subStatusBadge(status: SubscriptionStatus): string {
  switch (status) {
    case 'ACTIVE': return 'badge-green'
    case 'EXPIRED': return 'badge-yellow'
    case 'EXHAUSTED': return 'badge-gray'
    case 'CANCELLED': return 'badge-red'
    default: return 'badge-gray'
  }
}

export function subStatusLabel(status: SubscriptionStatus): string {
  switch (status) {
    case 'ACTIVE': return 'Активен'
    case 'EXPIRED': return 'Истёк'
    case 'EXHAUSTED': return 'Исчерпан'
    case 'CANCELLED': return 'Отменён'
    default: return status
  }
}

export function formatPrice(price: number): string {
  return new Intl.NumberFormat('ru-RU', { style: 'currency', currency: 'KZT', maximumFractionDigits: 0 }).format(price)
}
