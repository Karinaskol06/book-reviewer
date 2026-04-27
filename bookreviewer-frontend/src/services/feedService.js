import api from './api.js'

export const getFeedPage = async ({ page = 0, size = 6 } = {}) => {
  const response = await api.get('/feed', { params: { page, size } })
  return response.data
}
