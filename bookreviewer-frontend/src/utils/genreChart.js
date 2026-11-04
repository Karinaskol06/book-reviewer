/**
 * "Top genres" card helpers. A book can carry several genres, so each bar is the share of the
 * shelf that carries that genre (4 of 6 books = 67% full), not a slice of a 100% whole.
 */

export function booksLabel(count) {
  return `${count} ${count === 1 ? 'book' : 'books'}`
}

/** @returns {{ name: string, bookCount: number, fillPercent: number, countLabel: string }[]} */
export function buildGenreRows(topGenres, sampleSize) {
  if (!Array.isArray(topGenres)) return []
  const shelfSize = Number(sampleSize) || 0
  return topGenres.map((genre) => {
    const bookCount = Number(genre?.bookCount) || 0
    const fillPercent = shelfSize > 0
      ? Math.min(100, Math.round((bookCount / shelfSize) * 100))
      : 0
    return { name: genre?.name ?? '', bookCount, fillPercent, countLabel: booksLabel(bookCount) }
  })
}

/** Genres sharing the highest book count (rows arrive sorted that way), at most two for the headline. */
export function leadingGenreNames(rows) {
  if (!Array.isArray(rows) || rows.length === 0) return []
  const topCount = rows[0].bookCount
  return rows
    .filter((row) => row.bookCount === topCount)
    .slice(0, 2)
    .map((row) => row.name)
}

export function moreGenresLabel(otherGenreCount) {
  const count = Number(otherGenreCount) || 0
  if (count <= 0) return ''
  return `+ ${count} more ${count === 1 ? 'genre' : 'genres'}`
}

/** Bars 1-10 get their own palette tone (see --tone-0..9 in UserProfilePage.css); the long tail shares a neutral one. */
const PALETTE_TONES = 10

export function genreToneClass(index) {
  return Number.isInteger(index) && index >= 0 && index < PALETTE_TONES
    ? `genre-tone-${index}`
    : 'genre-tone-rest'
}
