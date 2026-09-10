import { useEffect } from 'react'
import { Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth.js'
import './AuthLayout.css'

const AuthLayout = () => {
  const { isAuthenticated } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    if (!isAuthenticated) return
    navigate('/dashboard', { replace: true })
  }, [isAuthenticated, navigate])

  if (isAuthenticated) return null

  return (
    <main className="auth-layout">
      <section className="auth-layout__panel">
        <section className="auth-layout__left">
          <Outlet />
        </section>
        <section className="auth-layout__right" aria-label="Book scene artwork" />
      </section>
    </main>
  )
}

export default AuthLayout
