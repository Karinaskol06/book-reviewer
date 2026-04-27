import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import AuthForm from '../components/auth/AuthForm.jsx'
import { useAuth } from '../hooks/useAuth.js'
import { useApiRequest } from '../hooks/useApiRequest.js'

const LoginPage = () => {
  const [formState, setFormState] = useState({
    username: '',
    password: '',
  })
  const { login } = useAuth()
  const { execute, loading, error } = useApiRequest()
  const navigate = useNavigate()
  const location = useLocation()

  const onChange = (event) => {
    const { name, value } = event.target
    setFormState((prev) => ({ ...prev, [name]: value }))
  }

  const onSubmit = async (event) => {
    event.preventDefault()
    try {
      await execute(() => login(formState))
      const redirectPath = location.state?.from?.pathname || '/dashboard'
      navigate(redirectPath, { replace: true })
    } catch {
      // Error state is handled in hook.
    }
  }

  return (
    <AuthForm
      mode="login"
      formState={formState}
      onChange={onChange}
      onSubmit={onSubmit}
      loading={loading}
      error={error}
    />
  )
}

export default LoginPage
