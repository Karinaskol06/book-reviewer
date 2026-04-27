import api from './api.js'

export const searchUsersByUsername = async (query, limit = 8) => {
  const response = await api.get('/users/search', {
    params: { query, limit },
  })
  return response.data || []
}

export const followUser = async (userId) => {
  await api.post(`/users/${userId}/follow`)
}

export const unfollowUser = async (userId) => {
  await api.delete(`/users/${userId}/follow`)
}
