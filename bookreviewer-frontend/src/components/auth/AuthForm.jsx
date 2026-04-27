import { Link } from 'react-router-dom'
import './AuthForm.css'

const AuthForm = ({
  mode,
  formState,
  onChange,
  onSubmit,
  loading,
  error,
}) => {
  const isRegister = mode === 'register'
  const title = isRegister ? 'Create account' : 'Hello Again!'

  return (
    <form className="auth-form" onSubmit={onSubmit}>
      <header className="auth-form__header">
        <h1>{title}</h1>
      </header>

      {isRegister && (
        <label className="auth-form__field">
          <input
            type="text"
            name="username"
            value={formState.username}
            onChange={onChange}
            placeholder="Name"
            autoComplete="username"
            minLength={3}
            required
          />
        </label>
      )}

      {isRegister && (
        <label className="auth-form__field">
          <input
            type="email"
            name="email"
            value={formState.email}
            onChange={onChange}
            placeholder="Email"
            autoComplete="email"
            required
          />
        </label>
      )}

      {!isRegister && (
        <label className="auth-form__field">
          <input
            type="text"
            name="username"
            value={formState.username}
            onChange={onChange}
            placeholder="Email or username"
            autoComplete="username"
            minLength={3}
            required
          />
        </label>
      )}

      <label className="auth-form__field">
        <input
          type="password"
          name="password"
          value={formState.password}
          onChange={onChange}
          placeholder="Password"
          autoComplete={isRegister ? 'new-password' : 'current-password'}
          minLength={6}
          required
        />
      </label>

      {error && <p className="auth-form__error">{error}</p>}

      <button className="auth-form__button" type="submit" disabled={loading}>
        {loading ? 'Please wait...' : isRegister ? 'Create account' : 'Sign in'}
      </button>

      <p className="auth-form__switch">
        {isRegister ? 'Already have an account?' : "Don't have an account?"}{' '}
        <Link to={isRegister ? '/auth/login' : '/auth/register'}>
          {isRegister ? 'Login' : 'Register'}
        </Link>
      </p>
    </form>
  )
}

export default AuthForm
