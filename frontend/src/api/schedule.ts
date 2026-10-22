import api from './client'
import type { HourSlot } from '../types'

export const getHourlySlots = (employeeId: number, date: string) =>
  api.get<HourSlot[]>('/schedule/hourly', { params: { employeeId, date } }).then((r) => r.data)
