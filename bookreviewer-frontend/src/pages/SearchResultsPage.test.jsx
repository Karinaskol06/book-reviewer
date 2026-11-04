// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { AuthContext } from '../context/authContext.js'
import SearchResultsPage from './SearchResultsPage.jsx'

vi.mock('../services/homeService.js', () => ({
  getGenres: vi.fn().mockResolvedValue([]),
  filterBooks: vi.fn().mockResolvedValue({ content: [], totalPages: 0, totalElements: 0, number: 0 }),
}))

afterEach(cleanup)

const CurrentSearch = () => <output data-testid="location">{useLocation().search}</output>

const renderPage = (path) =>
  render(
    <AuthContext.Provider value={{ user: { userId: 2, username: 'karina' }, logout: vi.fn() }}>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route
            path="/search"
            element={
              <>
                <SearchResultsPage />
                <CurrentSearch />
              </>
            }
          />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  )

describe('SearchResultsPage search box', () => {
  it('shows a search box filled with the current query', () => {
    renderPage('/search?query=gatsby&page=0')

    expect(screen.getByRole('searchbox', { name: 'Search books' }).value).toBe('gatsby')
  })

  it('searches for the typed text from the first page, keeping the filters', () => {
    renderPage('/search?query=gatsby&genres=Classics&page=2')

    fireEvent.change(screen.getByRole('searchbox', { name: 'Search books' }), { target: { value: '  babel ' } })
    fireEvent.submit(screen.getByRole('search', { name: 'Search books' }))

    const params = new URLSearchParams(screen.getByTestId('location').textContent)
    expect(params.get('query')).toBe('babel')
    expect(params.get('page')).toBe('0')
    expect(params.getAll('genres')).toEqual(['Classics'])
  })
})
