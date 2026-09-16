/**
 * Genre helpers — keep comparison keys aligned with backend NormalizationUtils.
 */

export function genreKey(value) {
  if (value == null) return ''
  return String(value)
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .replace(/\p{P}/gu, '')
    .trim()
    .replace(/\s+/g, ' ')
    .toLowerCase()
}

/** e.g. "ROMANCE....." → "Romance", "historical fiction" → "Historical Fiction" */
export function toGenreLabel(value) {
  const key = genreKey(value)
  if (!key) return ''
  return key
    .split(' ')
    .filter(Boolean)
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

export function findGenreByKey(genres, value) {
  const key = genreKey(value)
  if (!key) return null
  return (genres || []).find((genre) => genreKey(genre) === key) || null
}
