export type Role = 'CLIENT' | 'EMPLOYEE' | 'ADMIN' | 'OWNER'

export type AppointmentStatus = 'PENDING' | 'CONFIRMED' | 'COMPLETED' | 'CANCELLED'

export type SubscriptionStatus = 'PENDING' | 'OFFERED' | 'ACTIVE' | 'EXPIRED' | 'EXHAUSTED' | 'CANCELLED'

export type SubscriptionType = 'ANNUAL' | 'COURSE'

export type HourSlotStatus = 'FREE' | 'BUSY' | 'BLOCKED' | 'PAST'

export interface AuthResponse {
  token: string
  type: string
  id: number
  name: string
  email: string
  role: Role
}

export interface User {
  id: number
  name: string
  email: string
  phone: string
  role: Role
  active: boolean
  createdAt: string
}

export interface Employee {
  id: number
  userId: number
  name: string
  email: string
  phone: string
  specialization: string
  description: string
  active: boolean
}

export interface SalonService {
  id: number
  name: string
  description: string
  price: number
  durationMinutes: number
  active: boolean
}

export interface Appointment {
  id: number
  clientId: number
  clientName: string
  clientPhone: string
  employeeId: number
  employeeName: string
  employeeSpecialization: string
  serviceId: number
  serviceName: string
  servicePrice: number
  durationMinutes: number
  startTime: string
  endTime: string
  status: AppointmentStatus
  comment: string
  createdAt: string
  subscriptionId?: number | null
}

export interface Subscription {
  id: number
  clientId: number
  clientName: string
  clientPhone: string
  serviceId: number
  serviceName: string
  durationMinutes: number
  totalSessions: number
  remainingSessions: number
  startDate: string
  expiryDate: string
  status: SubscriptionStatus
  type: SubscriptionType
  price: number | null
  notes: string
  createdAt: string
}

export interface HourSlot {
  time: string
  status: HourSlotStatus
  appointmentId?: number | null
  clientName?: string | null
  serviceName?: string | null
}

export interface ClientShort {
  id: number
  name: string
  phone: string
}

export interface SalonSettings {
  name: string
  address: string
  phone: string
  email: string
  description: string
  workingHours: string
  instagramUrl: string
  annualPrice: number | null
  annualSessions: number | null
  annualDescription: string | null
}

// Request types
export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  name: string
  email: string
  phone: string
  password: string
}

export interface CreateAppointmentRequest {
  employeeId: number
  serviceId: number
  startTime: string
  comment?: string
  clientId?: number
  subscriptionId?: number
}

export interface CreateSubscriptionRequest {
  serviceId: number
  type?: SubscriptionType
  totalSessions?: number
  price?: number
  startDate?: string
  expiryDate?: string
  notes?: string
  clientId?: number
}
