import { useEffect } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth.js'

/**
 * Imperative redirect avoids <Navigate> remount loops under AnimatePresence.
 */
const ProtectedRoute = ({ children }) => {
  const { isAuthenticated } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()

  useEffect(() => {
    if (isAuthenticated) return
    navigate('/auth/login', {
      replace: true,
      state: { from: { pathname: location.pathname, search: location.search } },
    })
    // Intentionally omit location.* — only redirect when auth flips false.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isAuthenticated, navigate])

  if (!isAuthenticated) return null

  return children
}

export default ProtectedRoute
