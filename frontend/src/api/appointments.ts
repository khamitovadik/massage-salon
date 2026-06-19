import api from './client'
import type { Appointment, AppointmentStatus, CreateAppointmentRequest } from '../types'

export const getMyAppointments = () =>
  api.get<Appointment[]>('/appointments/my').then((r) => r.data)

export const getAllAppointments = () =>
  api.get<Appointment[]>('/appointments').then((r) => r.data)

export const getAppointmentsByEmployee = (employeeId: number) =>
  api.get<Appointment[]>(`/appointments/employee/${employeeId}`).then((r) => r.data)

export const createAppointment = (data: CreateAppointmentRequest) =>
  api.post<Appointment>('/appointments', data).then((r) => r.data)

export const cancelAppointment = (id: number) =>
  api.patch<Appointment>(`/appointments/${id}/cancel`).then((r) => r.data)

export const updateAppointmentStatus = (id: number, status: AppointmentStatus) =>
  api.patch<Appointment>(`/appointments/${id}/status`, null, { params: { status } }).then((r) => r.data)
