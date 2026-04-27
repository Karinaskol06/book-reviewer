import { useState } from 'react'
import { AUTH_TOKEN_KEY, AUTH_USER_KEY } from '../constants/storage'
import { loginUser, registerUser } from '../services/authService'
import { AuthContext } from './authContext'

const readStoredUser = () => {
  const rawUser = localStorage.getItem(AUTH_USER_KEY)
  if (!rawUser) return null
  try {
    return JSON.parse(rawUser)
  } catch {
    return null
  }
}

export const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(localStorage.getItem(AUTH_TOKEN_KEY))
  const [user, setUser] = useState(readStoredUser())

  const persistSession = (authResponse) => {
    const nextToken = authResponse.token
    const nextUser = {
      userId: authResponse.userId,
      username: authResponse.username,
      email: authResponse.email,
      avatarUrl: authResponse.avatarUrl,
      type: authResponse.type,
    }

    localStorage.setItem(AUTH_TOKEN_KEY, nextToken)
    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(nextUser))
    setToken(nextToken)
    setUser(nextUser)
  }

  const login = async (credentials) => {
    const authResponse = await loginUser(credentials)
    persistSession(authResponse)
    return authResponse
  }

  const register = async (payload) => {
    const authResponse = await registerUser(payload)
    persistSession(authResponse)
    return authResponse
  }

  const logout = () => {
    localStorage.removeItem(AUTH_TOKEN_KEY)
    localStorage.removeItem(AUTH_USER_KEY)
    setToken(null)
    setUser(null)
  }

  const value = {
    token,
    user,
    isAuthenticated: Boolean(token),
    login,
    register,
    logout,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
