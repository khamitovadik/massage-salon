import api from './client'
import type { Branch, User } from '../types'

export type BranchInput = Partial<Pick<Branch, 'name' | 'address' | 'phone' | 'mapUrl' | 'workingHours' | 'active'>>

/** Активные филиалы — для сайта и записи */
export const getBranches = () =>
  api.get<Branch[]>('/branches').then((r) => r.data)

/** Филиалы в панели: владельцу все, руководителю — свои */
export const getManagedBranches = () =>
  api.get<Branch[]>('/branches/manage').then((r) => r.data)

export const createBranch = (data: BranchInput) =>
  api.post<Branch>('/branches', data).then((r) => r.data)

export const updateBranch = (id: number, data: BranchInput) =>
  api.put<Branch>(`/branches/${id}`, data).then((r) => r.data)

export const closeBranch = (id: number) =>
  api.delete(`/branches/${id}`)

export const setBranchManagers = (id: number, userIds: number[]) =>
  api.put<Branch>(`/branches/${id}/managers`, userIds).then((r) => r.data)

export const setEmployeeBranches = (employeeId: number, branchIds: number[]) =>
  api.put<number[]>(`/employees/${employeeId}/branches`, branchIds).then((r) => r.data)

/** Все пользователи — только владельцу (для выбора руководителей) */
export const getAllUsers = () =>
  api.get<User[]>('/users').then((r) => r.data)
