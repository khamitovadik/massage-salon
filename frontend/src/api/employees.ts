import api from './client'
import type { Employee } from '../types'

export const getEmployees = () =>
  api.get<Employee[]>('/employees').then((r) => r.data)

export const getEmployee = (id: number) =>
  api.get<Employee>(`/employees/${id}`).then((r) => r.data)

export const createEmployee = (data: {
  name: string; email: string; phone: string; password: string
  specialization: string; description?: string
}) => api.post<Employee>('/employees', data).then((r) => r.data)

export const updateEmployee = (id: number, data: Partial<{
  name: string; phone: string; specialization: string; description: string; active: boolean
}>) => api.put<Employee>(`/employees/${id}`, data).then((r) => r.data)

export const deactivateEmployee = (id: number) =>
  api.delete(`/employees/${id}`)
