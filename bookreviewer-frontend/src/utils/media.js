export const resolveMediaUrl = (url, fallback = '') => {
  if (!url) return fallback

  const value = String(url).trim()
  if (!value) return fallback

  if (
    value.startsWith('http://')
    || value.startsWith('https://')
    || value.startsWith('data:')
    || value.startsWith('blob:')
  ) {
    return value
  }

  // Bare storage keys (e.g. avatars/uuid.jpg) must go through the public uploads mount.
  const storageKey = !value.startsWith('/') && /^[\w-]+\//.test(value)
    ? `/uploads-book-reviewer/${value}`
    : null

  const normalizedPath = storageKey || (value.startsWith('/') ? value : `/${value}`)
  const apiBase = import.meta.env.VITE_API_BASE_URL || '/api'

  if (apiBase.startsWith('http://') || apiBase.startsWith('https://')) {
    try {
      const apiOrigin = new URL(apiBase).origin
      return `${apiOrigin}${normalizedPath}`
    } catch {
      return normalizedPath
    }
  }

  return normalizedPath
}
