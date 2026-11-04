// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { mockIsIntersecting, setupIntersectionMocking } from 'react-intersection-observer/test-utils'
import { AuthContext } from '../context/authContext.js'
import { getFeedPage } from '../services/feedService.js'
import ActivityFeedPage from './ActivityFeedPage.jsx'

vi.mock('../services/feedService.js', () => ({ getFeedPage: vi.fn() }))
vi.mock('../services/userService.js', () => ({
  followUser: vi.fn(),
  unfollowUser: vi.fn(),
  searchUsersByUsername: vi.fn().mockResolvedValue([]),
}))

beforeEach(() => {
  setupIntersectionMocking(vi.fn)
})

afterEach(() => {
  cleanup()
  vi.clearAllMocks()
})

const now = new Date().toISOString()

const review = (id, title) => ({
  id,
  type: 'REVIEWED',
  createdAt: now,
  user: { id: 10, username: 'natalia' },
  book: { id: id + 100, title, author: 'Donna Tartt', coverUrl: '' },
  reviewSnippet: { id: id + 500, verdict: 'Haunting and gorgeous.', detailedReview: 'Long review text.', mood: ['dark'] },
})

const finished = (id, title) => ({
  id,
  type: 'FINISHED_READING',
  createdAt: now,
  user: { id: 11, username: 'artem' },
  book: { id: id + 100, title, author: 'Ray Bradbury', coverUrl: '' },
  status: 'Finished',
})

const renderFeed = () =>
  render(
    <AuthContext.Provider value={{ user: { userId: 2, username: 'karina' }, logout: vi.fn() }}>
      <MemoryRouter initialEntries={['/feed']}>
        <ActivityFeedPage />
      </MemoryRouter>
    </AuthContext.Provider>,
  )

describe('ActivityFeedPage', () => {
  it('appends the next page without remounting cards that are already on screen', async () => {
    getFeedPage
      .mockResolvedValueOnce({ content: [review(1, 'The Secret History'), finished(2, 'Dandelion Wine')], last: false })
      .mockResolvedValueOnce({ content: [review(3, 'If We Were Villains'), finished(4, 'Babel')], last: true })
    renderFeed()

    const firstReview = (await screen.findByRole('heading', { name: 'The Secret History' })).closest('article')
    const firstStatus = screen.getByRole('heading', { name: 'Dandelion Wine' }).closest('article')

    mockIsIntersecting(document.querySelector('.feed-sentinel'), true)

    expect(await screen.findByRole('heading', { name: 'If We Were Villains' })).toBeTruthy()
    expect(getFeedPage).toHaveBeenLastCalledWith({ page: 1, size: 6 })
    // Same DOM nodes: earlier cards stay put instead of being torn down and faded in again.
    expect(screen.getByRole('heading', { name: 'The Secret History' }).closest('article')).toBe(firstReview)
    expect(screen.getByRole('heading', { name: 'Dandelion Wine' }).closest('article')).toBe(firstStatus)
    expect(firstReview.isConnected).toBe(true)
    expect(await screen.findByText('You’re all caught up.')).toBeTruthy()
  })

  it('filters the loaded activity by kind', async () => {
    getFeedPage.mockResolvedValueOnce({
      content: [review(1, 'The Secret History'), finished(2, 'Dandelion Wine')],
      last: true,
    })
    renderFeed()
    await screen.findByRole('heading', { name: 'Dandelion Wine' })

    fireEvent.click(screen.getByRole('button', { name: 'Reviews' }))

    await waitFor(() => expect(screen.queryByRole('heading', { name: 'Dandelion Wine' })).toBeNull())
    expect(screen.getByRole('heading', { name: 'The Secret History' })).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Reviews' }).getAttribute('aria-pressed')).toBe('true')
  })
})
