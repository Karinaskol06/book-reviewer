import axios from 'axios'
import { AUTH_TOKEN_KEY } from '../constants/storage'
import { forceLogout } from './authSession'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(AUTH_TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status
    const url = `${error?.config?.baseURL || ''}${error?.config?.url || ''}`
    const hadToken = Boolean(localStorage.getItem(AUTH_TOKEN_KEY))
    const isAuthCall = url.includes('/auth/login') || url.includes('/auth/register')
    const appMessage = error?.response?.data?.message
    // Expired/invalid JWT: Spring often returns empty 403 for anonymous callers.
    // App-level Forbidden still includes a JSON message — keep the session.
    const authFailed =
      hadToken &&
      !isAuthCall &&
      (status === 401 || (status === 403 && !appMessage))

    if (authFailed) {
      forceLogout()
      if (typeof window !== 'undefined' && !window.location.pathname.startsWith('/auth/')) {
        window.location.assign('/auth/login')
      }
    }

    return Promise.reject(error)
  },
)

export default api
