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
    contentSafe: contentSafe === null || contentSafe === undefined ? undefined : contentSafe,
    page,
    size,
  }

  const response = await api.get('/books/filter', { params })
  return response.data
}
