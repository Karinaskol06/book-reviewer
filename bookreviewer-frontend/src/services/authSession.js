import { AUTH_TOKEN_KEY, AUTH_USER_KEY } from '../constants/storage'

let logoutHandler = null

/** Register AuthProvider logout so the API client can clear React state too. */
export const registerLogoutHandler = (handler) => {
  logoutHandler = handler
  return () => {
    if (logoutHandler === handler) logoutHandler = null
  }
}

/** Clear session in storage + AuthContext (if mounted). */
export const forceLogout = () => {
  localStorage.removeItem(AUTH_TOKEN_KEY)
  localStorage.removeItem(AUTH_USER_KEY)
  logoutHandler?.()
}
