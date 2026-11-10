import api from './client'
import type { HourSlot } from '../types'

export const getHourlySlots = (employeeId: number, date: string, branchId?: number | null) =>
  api.get<HourSlot[]>('/schedule/hourly', { params: { employeeId, date, branchId: branchId ?? undefined } }).then((r) => r.data)
