// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { searchBooks } from '../../services/homeService.js'
import { searchUsersByUsername } from '../../services/userService.js'
import { readRecentSearches, rememberSearch } from '../../utils/search.js'
import SearchSuggest from './SearchSuggest.jsx'

vi.mock('../../services/homeService.js', () => ({ searchBooks: vi.fn() }))
vi.mock('../../services/userService.js', () => ({ searchUsersByUsername: vi.fn() }))

const GENRES = ['Classics', 'Dark Academia', 'Fantasy', 'Mystery']
const BOOKS = [
  { id: 3, title: 'The Secret History', author: 'Donna Tartt', publicationYear: 1992, coverUrl: '' },
  { id: 19, title: 'If We Were Villains', author: 'M.L. Rio', publicationYear: 2017, coverUrl: '' },
]

beforeEach(() => {
  searchBooks.mockResolvedValue(BOOKS)
  searchUsersByUsername.mockResolvedValue([])
})

afterEach(() => {
  cleanup()
  localStorage.clear()
  vi.clearAllMocks()
})

const Where = () => {
  const { pathname, search } = useLocation()
  return <output data-testid="where">{pathname + search}</output>
}

const renderSearch = () =>
  render(
    <MemoryRouter initialEntries={['/dashboard']}>
      <Routes>
        <Route path="*" element={<><SearchSuggest genres={GENRES} popularGenres={GENRES.slice(0, 3)} /><Where /></>} />
      </Routes>
    </MemoryRouter>,
  )

const box = () => screen.getByRole('combobox', { name: 'Search the archive' })
const listbox = () => screen.getByRole('listbox')
const where = () => screen.getByTestId('where').textContent
const type = (value) => {
  fireEvent.focus(box())
  fireEvent.change(box(), { target: { value } })
}

describe('SearchSuggest', () => {
  it('suggests matching books with the typed text highlighted and opens the one picked', async () => {
    renderSearch()
    type('secret')

    const option = await screen.findByRole('option', { name: /The Secret History/ })
    expect(within(option).getByText('Donna Tartt', { exact: false })).toBeTruthy()
    expect(within(option).getByText('Secret').tagName).toBe('MARK')
    expect(searchBooks).toHaveBeenCalledWith('secret', 5)

    fireEvent.click(option)

    expect(where()).toBe('/books/3')
    expect(readRecentSearches()).toEqual(['secret'])
  })

  it('suggests collections whose name matches and opens the collection page', async () => {
    renderSearch()
    type('dark')

    fireEvent.click(await screen.findByRole('option', { name: /Dark Academia/ }))

    expect(where()).toBe('/collections/Dark%20Academia')
  })

  it('suggests readers and opens their profile', async () => {
    searchUsersByUsername.mockResolvedValue([{ id: 4, username: 'natalia', avatarUrl: null }])
    renderSearch()
    type('nat')

    fireEvent.click(await screen.findByRole('option', { name: /@natalia/ }))

    expect(where()).toBe('/users/4')
  })

  it('opens all results in the Library on Enter when nothing is selected', async () => {
    renderSearch()
    type('tartt')
    await screen.findByRole('option', { name: /The Secret History/ })

    fireEvent.keyDown(box(), { key: 'Enter' })

    expect(where()).toBe('/search?query=tartt&page=0')
  })

  it('moves through suggestions with the arrow keys and opens the active one on Enter', async () => {
    renderSearch()
    type('i')
    await screen.findByRole('option', { name: /If We Were Villains/ })

    fireEvent.keyDown(box(), { key: 'ArrowDown' })
    fireEvent.keyDown(box(), { key: 'ArrowDown' })

    const active = document.getElementById(box().getAttribute('aria-activedescendant'))
    expect(active.textContent).toMatch(/If We Were Villains/)
    expect(active.getAttribute('aria-selected')).toBe('true')

    fireEvent.keyDown(box(), { key: 'Enter' })
    expect(where()).toBe('/books/19')
  })

  it('offers to add the book when nothing matches', async () => {
    searchBooks.mockResolvedValue([])
    renderSearch()
    type('й')

    expect(await screen.findByText(/No books match “й”/)).toBeTruthy()
    fireEvent.click(screen.getByRole('option', { name: /Add “й” to the archive/ }))

    expect(where()).toBe(`/books/new?query=${encodeURIComponent('й')}`)
  })

  it('closes on Escape', async () => {
    renderSearch()
    type('secret')
    await screen.findByRole('option', { name: /The Secret History/ })

    fireEvent.keyDown(box(), { key: 'Escape' })

    expect(box().getAttribute('aria-expanded')).toBe('false')
    expect(screen.queryByRole('listbox')).toBeNull()
  })

  it('shows recent searches and popular collections before anything is typed', async () => {
    rememberSearch('gatsby')
    renderSearch()

    fireEvent.focus(box())

    expect(within(listbox()).getByRole('option', { name: /gatsby/ })).toBeTruthy()
    expect(within(listbox()).getByRole('option', { name: /Classics/ })).toBeTruthy()

    fireEvent.click(within(listbox()).getByRole('option', { name: /gatsby/ }))
    expect(box().value).toBe('gatsby')
    expect(await screen.findByRole('option', { name: /The Secret History/ })).toBeTruthy()
  })

  it('keeps the answer for the latest query when an older request finishes last', async () => {
    let finishOld
    searchBooks
      .mockImplementationOnce(() => new Promise((resolve) => { finishOld = () => resolve([{ id: 99, title: 'Old Result', author: 'x' }]) }))
      .mockResolvedValueOnce([BOOKS[1]])
    renderSearch()

    type('v')
    await vi.waitFor(() => expect(searchBooks).toHaveBeenCalledTimes(1))
    type('villains')
    await screen.findByRole('option', { name: /If We Were Villains/ })
    await act(async () => finishOld())

    expect(screen.queryByRole('option', { name: /Old Result/ })).toBeNull()
  })
})
