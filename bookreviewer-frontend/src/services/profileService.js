import api from './api'

export const getMyProfile = async () => {
  const response = await api.get('/users/me')
  return response.data
}

export const getUserProfileById = async (userId) => {
  const response = await api.get(`/users/${userId}`)
  return response.data
}

export const updateAboutMe = async (aboutMe) => {
  await api.put('/users/me/about-me', { aboutMe })
}

export const uploadAvatar = async (file) => {
  const formData = new FormData()
  formData.append('file', file)
  const response = await api.post('/users/me/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return response.data
}

export const getUserLibrary = async (status) => {
  const response = await api.get('/user/library', { params: { status: status || undefined } })
  return response.data
}

export const getUserLibraryByUserId = async (userId, status) => {
  const response = await api.get(`/users/${userId}/library`, { params: { status: status || undefined } })
  return response.data
}

export const getMyReviews = async ({ page = 0, size = 50, includeSpoilers = true } = {}) => {
  const response = await api.get('/users/me/reviews', {
    params: { page, size, includeSpoilers },
  })
  return response.data
}

export const getUserReviewsByUserId = async (userId, { page = 0, size = 50, includeSpoilers = true } = {}) => {
  const response = await api.get(`/users/${userId}/reviews`, {
    params: { page, size, includeSpoilers },
  })
  return response.data
}

export const exportReadingListPdf = async () => {
  const response = await api.get('/export/reading-list', { responseType: 'blob' })
  return response.data
}
