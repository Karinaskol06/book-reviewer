// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import ProfileReviews from './ProfileReviews.jsx'

afterEach(cleanup)

const entry = (id, bookId, title, rating, createdAt, extra = {}) => ({
  bookId,
  bookTitle: title,
  bookAuthor: 'Some Author',
  bookCover: '',
  review: {
    id,
    rating,
    createdAt,
    verdict: `Verdict for ${title}`,
    whoIsItFor: 'Readers who like it',
    mood: ['Dark', 'Tense'],
    helpfulCount: 0,
    ...extra,
  },
})

const entries = [
  entry(11, 6, 'Babel', 5, '2026-04-24T10:00:00', { helpfulCount: 3 }),
  entry(12, 8, 'Frankenstein', 4, '2026-04-23T10:00:00'),
  entry(13, 4, 'Dandelion Wine', 3, '2026-04-25T10:00:00'),
]

const renderReviews = (props = {}) => {
  const handlers = { onEdit: vi.fn(), onDelete: vi.fn() }
  render(
    <MemoryRouter>
      <ProfileReviews entries={entries} isOwnProfile ownerName="karina" deletingReviewId={null} {...handlers} {...props} />
    </MemoryRouter>,
  )
  return handlers
}

const titles = () => screen.getAllByRole('article').map((article) => within(article).getByRole('heading').textContent)

describe('ProfileReviews', () => {
  it('lists every review with a link to it on the book page, its rating and date', () => {
    renderReviews()

    expect(screen.getByRole('heading', { name: /My Reviews/ })).toBeTruthy()
    const babel = screen.getAllByRole('article').find((article) => article.textContent.includes('Babel'))
    expect(within(babel).getByRole('link', { name: 'Babel' }).getAttribute('href')).toBe('/books/6#review-11')
    expect(within(babel).getByRole('img', { name: 'Rated 5 out of 5' })).toBeTruthy()
    expect(within(babel).getByText(/Reviewed 24 Apr 2026/)).toBeTruthy()
    expect(within(babel).getByText(/Verdict for Babel/)).toBeTruthy()
  })

  it('shows helpful votes only when there are some', () => {
    renderReviews()

    const [babel, frankenstein] = ['Babel', 'Frankenstein'].map((title) =>
      screen.getAllByRole('article').find((article) => article.textContent.includes(title)),
    )
    expect(within(babel).getByText(/3 found this helpful/)).toBeTruthy()
    expect(within(frankenstein).queryByText(/helpful/i)).toBeNull()
  })

  it('shows the newest first and can sort by rating', () => {
    renderReviews()
    expect(titles()).toEqual(['Dandelion Wine', 'Babel', 'Frankenstein'])

    fireEvent.click(screen.getByRole('button', { name: 'Highest rated' }))

    expect(titles()).toEqual(['Babel', 'Frankenstein', 'Dandelion Wine'])
    expect(screen.getByRole('button', { name: 'Highest rated' }).getAttribute('aria-pressed')).toBe('true')
    expect(screen.getByRole('button', { name: 'Newest' }).getAttribute('aria-pressed')).toBe('false')
  })

  it('lets the owner edit and delete a review', () => {
    const { onEdit, onDelete } = renderReviews()
    const babel = screen.getAllByRole('article').find((article) => article.textContent.includes('Babel'))

    fireEvent.click(within(babel).getByRole('button', { name: 'Edit review of Babel' }))
    fireEvent.click(within(babel).getByRole('button', { name: 'Delete review of Babel' }))

    expect(onEdit).toHaveBeenCalledWith(entries[0])
    expect(onDelete).toHaveBeenCalledWith(entries[0])
  })

  it('shows progress while a review is being deleted', () => {
    renderReviews({ deletingReviewId: 11 })
    const babel = screen.getAllByRole('article').find((article) => article.textContent.includes('Babel'))

    const button = within(babel).getByRole('button', { name: 'Delete review of Babel' })
    expect(button.disabled).toBe(true)
    expect(button.textContent).toMatch(/Deleting/)
  })

  it("hides edit and delete on someone else's profile and names them in the heading", () => {
    renderReviews({ isOwnProfile: false, ownerName: 'natalia' })

    expect(screen.getByRole('heading', { name: /natalia's Reviews/ })).toBeTruthy()
    expect(screen.queryByRole('button', { name: /Edit review/ })).toBeNull()
    expect(screen.queryByRole('button', { name: /Delete review/ })).toBeNull()
  })

  it('invites the owner to write a first review when there are none', () => {
    renderReviews({ entries: [] })

    expect(screen.getByRole('link', { name: 'Find a book to review' }).getAttribute('href')).toBe('/search?page=0')
    expect(screen.queryByRole('button', { name: 'Highest rated' })).toBeNull()
  })

  it("says there are no reviews yet on someone else's empty profile", () => {
    renderReviews({ entries: [], isOwnProfile: false, ownerName: 'natalia' })

    expect(screen.getByText('No reviews yet.')).toBeTruthy()
    expect(screen.queryByRole('link', { name: 'Find a book to review' })).toBeNull()
  })
})
