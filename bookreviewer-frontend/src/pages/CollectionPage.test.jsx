// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { AuthContext } from '../context/authContext.js'
import { filterBooks } from '../services/homeService.js'
import CollectionPage from './CollectionPage.jsx'

vi.mock('../services/homeService.js', () => ({ filterBooks: vi.fn() }))

afterEach(() => {
  cleanup()
  vi.clearAllMocks()
})

const book = (id, title, genres, extra = {}) => ({
  id, title, author: `Author ${id}`, genres, averageRating: 4.5, publicationYear: 2000 + id, coverUrl: '', ...extra,
})

const page = (content, { number = 0, totalPages = 1, totalElements = content.length } = {}) => ({
  content, number, totalPages, totalElements, last: number >= totalPages - 1,
})

const renderCollection = (genre = 'Dark Academia') =>
  render(
    <AuthContext.Provider value={{ user: { userId: 2, username: 'karina' }, logout: vi.fn() }}>
      <MemoryRouter initialEntries={[`/collections/${encodeURIComponent(genre)}`]}>
        <Routes>
          <Route path="/collections/:genre" element={<CollectionPage />} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  )

const bookTitles = () => within(screen.getByRole('list', { name: 'Books in this collection' }))
  .getAllByRole('link').map((link) => link.getAttribute('aria-label') || link.textContent)

describe('CollectionPage', () => {
  it('shows the collection with its size and books, top rated first', async () => {
    filterBooks.mockResolvedValue(page(
      [book(3, 'The Secret History', ['Dark Academia', 'Mystery']), book(19, 'If We Were Villains', ['Dark Academia'])],
      { totalElements: 2 },
    ))
    renderCollection()

    expect(await screen.findByRole('heading', { level: 2, name: 'Dark Academia' })).toBeTruthy()
    expect(await screen.findByText('2 books')).toBeTruthy()
    expect(screen.getByRole('link', { name: /The Secret History/ }).getAttribute('href')).toBe('/books/3')
    expect(filterBooks).toHaveBeenCalledWith(expect.objectContaining({
      genres: ['Dark Academia'], page: 0, sort: 'averageRating,desc',
    }))
  })

  it('re-sorts from the first page when another order is picked', async () => {
    filterBooks.mockResolvedValue(page([book(3, 'The Secret History', ['Dark Academia'])]))
    renderCollection()
    await screen.findByText('1 book')

    fireEvent.click(screen.getByRole('button', { name: 'Newest' }))

    expect(screen.getByRole('button', { name: 'Newest' }).getAttribute('aria-pressed')).toBe('true')
    await vi.waitFor(() =>
      expect(filterBooks).toHaveBeenLastCalledWith(expect.objectContaining({ page: 0, sort: 'publicationYear,desc' })),
    )
  })

  it('loads more books onto the end of the list until the last page', async () => {
    filterBooks
      .mockResolvedValueOnce(page([book(1, 'First', ['Dark Academia'])], { totalPages: 2, totalElements: 2 }))
      .mockResolvedValueOnce(page([book(2, 'Second', ['Dark Academia'])], { number: 1, totalPages: 2, totalElements: 2 }))
    renderCollection()

    fireEvent.click(await screen.findByRole('button', { name: 'Show more books' }))

    await screen.findByRole('link', { name: /Second/ })
    expect(bookTitles().map((name) => name.split(',')[0])).toEqual(['First', 'Second'])
    expect(filterBooks).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1 }))
    expect(screen.queryByRole('button', { name: 'Show more books' })).toBeNull()
  })

  it('suggests the genres most often shelved alongside this one', async () => {
    filterBooks.mockResolvedValue(page([
      book(1, 'A', ['Dark Academia', 'Mystery', 'Thriller']),
      book(2, 'B', ['dark academia', 'Mystery']),
      book(3, 'C', ['Dark Academia', 'Gothic']),
    ]))
    renderCollection()

    const related = await screen.findByRole('navigation', { name: 'Often shelved with' })
    const links = within(related).getAllByRole('link')
    expect(links.map((link) => link.textContent)).toEqual(['Mystery', 'Gothic', 'Thriller'])
    expect(links[0].getAttribute('href')).toBe('/collections/Mystery')
  })

  it('links to the Library with this genre pre-filtered for finer filtering', async () => {
    filterBooks.mockResolvedValue(page([book(3, 'The Secret History', ['Dark Academia'])]))
    renderCollection()

    const link = await screen.findByRole('link', { name: /Refine in the Library/ })
    expect(link.getAttribute('href')).toBe('/search?genres=Dark%20Academia&page=0')
  })

  it('invites adding a book when the collection is empty', async () => {
    filterBooks.mockResolvedValue(page([]))
    renderCollection('Solarpunk')

    expect(await screen.findByText(/No books in Solarpunk yet/)).toBeTruthy()
    expect(screen.getByRole('link', { name: 'Add the first book' }).getAttribute('href')).toBe('/books/new')
  })
})
