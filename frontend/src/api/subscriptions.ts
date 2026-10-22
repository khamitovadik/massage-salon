import api from './client'
import type { Subscription, CreateSubscriptionRequest } from '../types'

export const getMySubscriptions = () =>
  api.get<Subscription[]>('/subscriptions/my').then((r) => r.data)

export const getMyActiveSubscriptions = () =>
  api.get<Subscription[]>('/subscriptions/my/active').then((r) => r.data)

export const getAllSubscriptions = () =>
  api.get<Subscription[]>('/subscriptions').then((r) => r.data)

export const createSubscription = (data: CreateSubscriptionRequest) =>
  api.post<Subscription>('/subscriptions', data).then((r) => r.data)

export const useSubscriptionSession = (id: number) =>
  api.patch<Subscription>(`/subscriptions/${id}/use`).then((r) => r.data)

export const cancelSubscription = (id: number) =>
  api.patch<Subscription>(`/subscriptions/${id}/cancel`).then((r) => r.data)

export const getClientSubscriptions = (clientId: number) =>
  api.get<Subscription[]>(`/subscriptions/client/${clientId}`).then((r) => r.data)

export const approveSubscription = (id: number) =>
  api.patch<Subscription>(`/subscriptions/${id}/approve`).then((r) => r.data)

export const rejectSubscription = (id: number, reason?: string) =>
  api.patch<Subscription>(`/subscriptions/${id}/reject`, null, { params: { reason } }).then((r) => r.data)

export const acceptSubscription = (id: number) =>
  api.patch<Subscription>(`/subscriptions/${id}/accept`).then((r) => r.data)

export const declineSubscription = (id: number) =>
  api.patch<Subscription>(`/subscriptions/${id}/decline`).then((r) => r.data)

export const transferSubscription = (id: number, toClientId: number) =>
  api.patch<Subscription>(`/subscriptions/${id}/transfer`, null, { params: { toClientId } }).then((r) => r.data)
