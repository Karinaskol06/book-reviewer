import { useCallback, useState } from 'react'

const getMessage = (error) =>
  error?.response?.data?.message || error?.response?.data?.error || 'Request failed. Please try again.'

export const useApiRequest = () => {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const execute = useCallback(async (requestFn) => {
    setLoading(true)
    setError('')
    try {
      const data = await requestFn()
      return data
    } catch (requestError) {
      setError(getMessage(requestError))
      throw requestError
    } finally {
      setLoading(false)
    }
  }, [])

  return { execute, loading, error, setError }
}
