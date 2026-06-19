import type { AuthResponse, Role } from '../types'

export interface AuthState {
  token: string | null
  user: {
    id: number
    name: string
    email: string
    role: Role
  } | null
}

export function getAuth(): AuthState {
  const token = localStorage.getItem('token')
  const userStr = localStorage.getItem('user')
  return {
    token,
    user: userStr ? JSON.parse(userStr) : null,
  }
}

export function saveAuth(data: AuthResponse): void {
  localStorage.setItem('token', data.token)
  localStorage.setItem('user', JSON.stringify({
    id: data.id,
    name: data.name,
    email: data.email,
    role: data.role,
  }))
}

export function clearAuth(): void {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
}

export function isAdmin(role?: Role | null): boolean {
  return role === 'ADMIN' || role === 'OWNER'
}
