import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth.js'
import './AuthLayout.css'

const AuthLayout = () => {
  const { isAuthenticated } = useAuth()

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />
  }

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
