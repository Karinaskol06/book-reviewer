import api from './api'

export const getTrendingBooks = async (limit = 8) => {
  const response = await api.get('/home/trending', { params: { limit } })
  return response.data
}

export const searchBooks = async (query, size = 12) => {
  const response = await api.get('/books/search', {
    params: { query, size, page: 0 },
  })
  return response.data?.content || []
}

export const getGenres = async () => {
  const response = await api.get('/genres')
  return response.data
}

export const getBooksByGenre = async (genre, size = 1) => {
  const response = await api.get('/books', {
    params: { genre, page: 0, size },
  })
  return response.data || []
}

export const getCollectionShelves = async ({ genres, genreCount = 4, booksPerGenre = 3 } = {}) => {
  const source = Array.isArray(genres) ? genres : await getGenres()
  const all = Array.isArray(source) ? source : []

  const findGenre = (name) =>
    all.find((genre) => String(genre).toLowerCase() === name.toLowerCase())

  const classics = findGenre('Classics')
  const darkAcademia = findGenre('Dark Academia')
  const dystopian = findGenre('Dystopian')
  const fillers = all.filter(
    (genre) => genre !== classics && genre !== darkAcademia && genre !== dystopian,
  )

  const ordered = [
    classics,
    darkAcademia,
    ...fillers.slice(0, Math.max(0, genreCount - 3)),
    dystopian,
  ].filter(Boolean)

  const selected = ordered.slice(0, genreCount)

  return Promise.all(
    selected.map(async (genre) => {
      const books = await getBooksByGenre(genre, booksPerGenre)
      return { genre, books: Array.isArray(books) ? books : [] }
    }),
  )
}

export const filterBooks = async ({
  query = '',
  genres = [],
  minRating,
  pacing,
  yearFrom,
  yearTo,
  contentSafe,
  page = 0,
  size = 8,
}) => {
  const params = {
    query: query || undefined,
    genres: genres.length ? genres : undefined,
    minRating: minRating || undefined,
    pacing: pacing || undefined,
    yearFrom: yearFrom || undefined,
    yearTo: yearTo || undefined,
    contentSafe: contentSafe === true ? true : undefined,
    page,
    size,
  }

  const response = await api.get('/books/filter', { params })
  return response.data
}
