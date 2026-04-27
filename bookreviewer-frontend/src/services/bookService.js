import api from './api'

export const getBookDetail = async (bookId) => {
  const response = await api.get(`/books/${bookId}`)
  return response.data
}

export const getBookReviews = async (bookId, { page = 0, size = 6, includeSpoilers = false } = {}) => {
  const response = await api.get(`/books/${bookId}/reviews`, {
    params: { page, size, includeSpoilers },
  })
  return response.data
}

export const setBookStatus = async (bookId, status) => {
  const response = await api.post(`/user/books/${bookId}/status`, { status })
  return response.data
}

export const getBookStatus = async (bookId) => {
  const response = await api.get(`/user/books/${bookId}/status`)
  return response.data
}

export const clearBookStatus = async (bookId) => {
  await api.delete(`/user/books/${bookId}/status`)
}

export const checkBookDuplicate = async (title, author) => {
  const response = await api.get('/books/check', { params: { title, author } })
  return response.data
}

export const createBook = async (payload) => {
  const response = await api.post('/books', payload)
  return response.data
}

export const createReview = async (bookId, payload) => {
  const response = await api.post(`/books/${bookId}/reviews`, payload)
  return response.data
}
