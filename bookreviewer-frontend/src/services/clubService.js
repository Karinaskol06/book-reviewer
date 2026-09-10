import api from './api'

export const getPublicClubs = async ({ page = 0, size = 12 } = {}) => {
  const response = await api.get('/clubs', { params: { page, size } })
  return response.data
}

export const getMyClubs = async ({ page = 0, size = 12 } = {}) => {
  const response = await api.get('/clubs/my', { params: { page, size } })
  return response.data
}

export const getClub = async (clubId) => {
  const response = await api.get(`/clubs/${clubId}`)
  return response.data
}

export const createClub = async (payload) => {
  const response = await api.post('/clubs', payload)
  return response.data
}

export const updateClub = async (clubId, payload) => {
  const response = await api.put(`/clubs/${clubId}`, payload)
  return response.data
}

export const deleteClub = async (clubId) => {
  await api.delete(`/clubs/${clubId}`)
}

export const joinClub = async (clubId) => {
  await api.post(`/clubs/${clubId}/join`)
}

export const leaveClub = async (clubId) => {
  await api.delete(`/clubs/${clubId}/leave`)
}

export const getClubMembers = async (clubId) => {
  const response = await api.get(`/clubs/${clubId}/members`)
  return response.data
}

export const getPendingMembers = async (clubId) => {
  const response = await api.get(`/clubs/${clubId}/members/pending`)
  return response.data
}

export const approveMember = async (clubId, userId) => {
  await api.put(`/clubs/${clubId}/members/${userId}/approve`)
}

export const rejectMember = async (clubId, userId) => {
  await api.put(`/clubs/${clubId}/members/${userId}/reject`)
}

export const removeMember = async (clubId, userId) => {
  await api.delete(`/clubs/${clubId}/members/${userId}`)
}

export const transferOwnership = async (clubId, userId) => {
  await api.put(`/clubs/${clubId}/members/${userId}/transfer`)
}

export const promoteMember = async (clubId, userId) => {
  await api.put(`/clubs/${clubId}/members/${userId}/promote`)
}

export const demoteMember = async (clubId, userId) => {
  await api.put(`/clubs/${clubId}/members/${userId}/demote`)
}

export const setCurrentBook = async (clubId, bookId) => {
  await api.put(`/clubs/${clubId}/current-book`, null, { params: { bookId } })
}

export const setNextMeeting = async (clubId, { meetingTime, meetingLink }) => {
  await api.put(`/clubs/${clubId}/next-meeting`, { meetingTime, meetingLink })
}

export const getClubPosts = async (clubId, { page = 0, size = 20 } = {}) => {
  const response = await api.get(`/clubs/${clubId}/posts`, { params: { page, size } })
  return response.data
}

export const createPost = async (clubId, content, parentPostId) => {
  const response = await api.post(`/clubs/${clubId}/posts`, { content, parentPostId })
  return response.data
}

export const createReply = async (clubId, postId, content) => {
  const response = await api.post(`/clubs/${clubId}/posts/${postId}/replies`, { content })
  return response.data
}

export const getReplies = async (clubId, postId, { page = 0, size = 50 } = {}) => {
  const response = await api.get(`/clubs/${clubId}/posts/${postId}/replies`, { params: { page, size } })
  return response.data
}

export const deletePost = async (clubId, postId) => {
  await api.delete(`/clubs/${clubId}/posts/${postId}`)
}

export const toggleInsightful = async (clubId, postId) => {
  await api.post(`/clubs/${clubId}/posts/${postId}/insightful`)
}
