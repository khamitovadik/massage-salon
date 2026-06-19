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
