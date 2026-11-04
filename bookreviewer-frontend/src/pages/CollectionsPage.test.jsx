// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { AuthContext } from '../context/authContext.js'
import { filterBooks, getGenres } from '../services/homeService.js'
import CollectionsPage from './CollectionsPage.jsx'

vi.mock('../services/homeService.js', () => ({ filterBooks: vi.fn(), getGenres: vi.fn() }))

afterEach(() => {
  cleanup()
  vi.clearAllMocks()
})

const COUNTS = { Classics: 5, 'Dark Academia': 3, Fantasy: 1 }

const renderIndex = () => {
  getGenres.mockResolvedValue(['Fantasy', 'Classics', 'Dark Academia'])
  filterBooks.mockImplementation(async ({ genres }) => ({
    content: [{ id: 1, title: `${genres[0]} book`, coverUrl: '' }],
    totalElements: COUNTS[genres[0]],
  }))
  render(
    <AuthContext.Provider value={{ user: { userId: 2, username: 'karina' }, logout: vi.fn() }}>
      <MemoryRouter initialEntries={['/collections']}>
        <CollectionsPage />
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

const shelfList = () => screen.getByRole('list', { name: 'Collections' })

describe('CollectionsPage', () => {
  it('lists every collection A to Z with its size, each linking to its page', async () => {
    renderIndex()

    expect(await screen.findByText('5 books')).toBeTruthy()
    const links = within(shelfList()).getAllByRole('link')
    expect(links.map((link) => link.querySelector('h3').textContent)).toEqual(['Classics', 'Dark Academia', 'Fantasy'])
    expect(links[1].getAttribute('href')).toBe('/collections/Dark%20Academia')
    expect(await within(links[2]).findByText('1 book')).toBeTruthy()
  })

  it('narrows the list as you type a name', async () => {
    renderIndex()
    await screen.findByText('5 books')

    fireEvent.change(screen.getByRole('searchbox', { name: 'Find a collection' }), { target: { value: 'acad' } })

    const names = within(shelfList()).getAllByRole('link').map((link) => link.querySelector('h3').textContent)
    expect(names).toEqual(['Dark Academia'])
  })

  it('offers an archive search when no collection matches', async () => {
    renderIndex()
    await screen.findByText('5 books')

    fireEvent.change(screen.getByRole('searchbox', { name: 'Find a collection' }), { target: { value: 'solarpunk' } })

    expect(screen.getByText(/No collection matches “solarpunk”/)).toBeTruthy()
    expect(screen.getByRole('link', { name: /Search the archive for “solarpunk”/ }).getAttribute('href'))
      .toBe('/search?query=solarpunk&page=0')
  })
})
