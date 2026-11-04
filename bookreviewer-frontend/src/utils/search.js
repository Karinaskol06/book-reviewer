import { genreKey } from './genre.js'

const escapeRegExp = (value) => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

/**
 * Splits `text` into parts so the parts matching `query` (case-insensitive) can be emphasised.
 * @returns {{ text: string, match: boolean }[]}
 */
export function highlightParts(text, query) {
  const source = String(text ?? '')
  const needle = String(query ?? '').trim()
  if (!needle) return [{ text: source, match: false }]

  const parts = source.split(new RegExp(`(${escapeRegExp(needle)})`, 'gi')).filter(Boolean)
  const lowered = needle.toLowerCase()
  return parts.length
    ? parts.map((part) => ({ text: part, match: part.toLowerCase() === lowered }))
    : [{ text: source, match: false }]
}

/** Genres matching `query`: names starting with it first, then names containing it. */
export function matchGenres(genres, query, limit = 3) {
  const key = genreKey(query)
  if (!key) return []
  const list = Array.isArray(genres) ? genres : []
  const starts = []
  const contains = []
  list.forEach((genre) => {
    const genreName = genreKey(genre)
    if (genreName.startsWith(key)) starts.push(genre)
    else if (genreName.includes(key)) contains.push(genre)
  })
  const byName = (a, b) => String(a).localeCompare(String(b))
  return [...starts.sort(byName), ...contains.sort(byName)].slice(0, limit)
}

/* Recent searches: a per-browser convenience, so storage failures (private mode, blocked storage) are ignored. */

const RECENT_KEY = 'bookreviewer.recentSearches'
const RECENT_LIMIT = 5

export function readRecentSearches() {
  try {
    const stored = JSON.parse(localStorage.getItem(RECENT_KEY) || '[]')
    return Array.isArray(stored) ? stored.filter((item) => typeof item === 'string') : []
  } catch {
    return []
  }
}

export function rememberSearch(query) {
  const value = String(query ?? '').trim()
  if (!value) return
  const lowered = value.toLowerCase()
  const next = [value, ...readRecentSearches().filter((item) => item.toLowerCase() !== lowered)].slice(0, RECENT_LIMIT)
  try {
    localStorage.setItem(RECENT_KEY, JSON.stringify(next))
  } catch {
    // storage unavailable: recent searches simply aren't kept
  }
}

export function clearRecentSearches() {
  try {
    localStorage.removeItem(RECENT_KEY)
  } catch {
    // ignore
  }
}

/** URL of a collection page. */
export const collectionPath = (genre) => `/collections/${encodeURIComponent(genre)}`

/** Tags too broad to be a useful "next shelf" (kept in line with the backend's GenreTaxonomy). */
const UMBRELLA_KEYS = new Set(['fiction', 'nonfiction', 'non fiction', 'novels'])

/** Genres that most often share a book with `genre`, by how many loaded books carry them. */
export function relatedGenres(books, genre, limit = 6) {
  const current = genreKey(genre)
  const tally = new Map()
  books.forEach((book) => {
    const seen = new Set()
    ;(book.genres || []).forEach((name) => {
      const key = genreKey(name)
      if (!key || key === current || UMBRELLA_KEYS.has(key) || seen.has(key)) return
      seen.add(key)
      const entry = tally.get(key) || { name: String(name).trim(), count: 0 }
      entry.count += 1
      tally.set(key, entry)
    })
  })
  return [...tally.values()]
    .sort((a, b) => b.count - a.count || a.name.localeCompare(b.name))
    .slice(0, limit)
    .map((entry) => entry.name)
}
