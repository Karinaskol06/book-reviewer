import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import AuthForm from '../components/auth/AuthForm.jsx'
import { useAuth } from '../hooks/useAuth.js'
import { useApiRequest } from '../hooks/useApiRequest.js'

const RegisterPage = () => {
  const [formState, setFormState] = useState({
    email: '',
    username: '',
    password: '',
  })
  const { register } = useAuth()
  const { execute, loading, error } = useApiRequest()
  const navigate = useNavigate()

  const onChange = (event) => {
    const { name, value } = event.target
    setFormState((prev) => ({ ...prev, [name]: value }))
  }

  const onSubmit = async (event) => {
    event.preventDefault()
    try {
      await execute(() => register(formState))
      navigate('/dashboard', { replace: true })
    } catch {
      // Error state is handled in hook.
    }
  }

  return (
    <AuthForm
      mode="register"
      formState={formState}
      onChange={onChange}
      onSubmit={onSubmit}
      loading={loading}
      error={error}
    />
  )
}

export default RegisterPage
