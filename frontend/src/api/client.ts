import axios from 'axios'
import { getAuth, isAdmin } from '../store/auth'
import { getCurrentBranchId } from '../store/branch'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ? `${import.meta.env.VITE_API_URL}/api` : '/api',
  headers: { 'Content-Type': 'application/json' },
})

// Attach JWT token to every request
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  // Руководитель и владелец смотрят данные выбранного в шапке филиала.
  // Явно переданный branchId (например, при записи) не перезаписываем.
  const branchId = getCurrentBranchId()
  if (branchId && isAdmin(getAuth().user?.role) && !(config.params && 'branchId' in config.params)) {
    config.params = { ...(config.params ?? {}), branchId }
  }
  return config
})

// Redirect to login on 401
api.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api
