import { useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'

/** One-shot redirect; safe with AnimatePresence (unlike lingering <Navigate>). */
const RedirectOnce = ({ to }) => {
  const navigate = useNavigate()
  const didRedirect = useRef(false)

  useEffect(() => {
    if (didRedirect.current) return
    didRedirect.current = true
    navigate(to, { replace: true })
  }, [navigate, to])

  return null
}

export default RedirectOnce
