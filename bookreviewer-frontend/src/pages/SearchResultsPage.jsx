import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth.js'
import { filterBooks, getGenres } from '../services/homeService.js'
import './SearchResultsPage.css'

const renderStars = (rating = 0) => {
  const rounded = Math.round(rating)
  return '★★★★★'.slice(0, rounded) + '☆☆☆☆☆'.slice(0, 5 - rounded)
}

const SearchResultsPage = () => {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const paramsString = searchParams.toString()

  const query = searchParams.get('query') || ''
  const page = Number(searchParams.get('page') || '0')
  const minRating = searchParams.get('minRating') || ''
  const pacing = searchParams.get('pacing') || ''
  const yearFrom = searchParams.get('yearFrom') || ''
  const yearTo = searchParams.get('yearTo') || ''
  const contentSafe = searchParams.get('contentSafe') === 'true'
  const selectedGenres = useMemo(() => new URLSearchParams(paramsString).getAll('genres'), [paramsString])

  const [allGenres, setAllGenres] = useState([])
  const [booksPage, setBooksPage] = useState({ content: [], totalPages: 0, totalElements: 0, number: 0 })
  const [loading, setLoading] = useState(false)
  const [headerSearch, setHeaderSearch] = useState(query)

  useEffect(() => {
    const value = headerSearch.trim()
    if (value === query) return

    const timer = setTimeout(() => {
      const params = new URLSearchParams(searchParams)
      params.delete('query')
      if (value) params.set('query', value)
      params.set('page', '0')
      setSearchParams(params)
    }, 350)

    return () => clearTimeout(timer)
  }, [headerSearch, query, searchParams, setSearchParams])

  useEffect(() => {
    const loadGenres = async () => {
      const genres = await getGenres()
      setAllGenres(genres)
    }
    loadGenres()
  }, [])

  useEffect(() => {
    const loadResults = async () => {
      setLoading(true)
      try {
        const result = await filterBooks({
          query,
          genres: selectedGenres,
          minRating: minRating ? Number(minRating) : undefined,
          pacing: pacing || undefined,
          yearFrom: yearFrom ? Number(yearFrom) : undefined,
          yearTo: yearTo ? Number(yearTo) : undefined,
          contentSafe,
          page,
          size: 6,
        })
        setBooksPage(result)
      } finally {
        setLoading(false)
      }
    }
    loadResults()
  }, [contentSafe, minRating, pacing, page, query, selectedGenres, yearFrom, yearTo])

  const updateFilters = (next) => {
    const params = new URLSearchParams(searchParams)
    Object.entries(next).forEach(([key, value]) => {
      params.delete(key)
      if (Array.isArray(value)) {
        value.forEach((v) => params.append(key, v))
      } else if (value !== '' && value !== null && value !== undefined) {
        params.set(key, String(value))
      }
    })
    params.set('page', '0')
    setSearchParams(params)
  }

  const resultsCountText = useMemo(
    () => `Showing ${booksPage.totalElements ?? 0} curated titles${query ? ` for "${query}"` : ''}`,
    [booksPage.totalElements, query],
  )

  return (
    <main className="dashboard">
      <header className="home-nav">
        <h1>BookReviewer</h1>
        <nav>
          <Link to="/dashboard">Home</Link>
          <Link to="/dashboard#trending">Library</Link>
          <Link to="/books/new">Add Book</Link>
          <Link to="/dashboard#collections">Collections</Link>
          <Link to="/feed">Feed</Link>
        </nav>
        <input
          className="home-nav__search"
          type="search"
          placeholder="Search the archive..."
          value={headerSearch}
          onChange={(event) => setHeaderSearch(event.target.value)}
        />
        <div className="home-nav__actions">
          <Link className="home-nav__profile" to="/profile" aria-label="My profile" title={user?.username || 'My profile'}>
            <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
              <path d="M12 12c2.76 0 5-2.24 5-5S14.76 2 12 2 7 4.24 7 7s2.24 5 5 5Zm0 2c-3.86 0-7 3.14-7 7 0 .55.45 1 1 1h12c.55 0 1-.45 1-1 0-3.86-3.14-7-7-7Z" />
            </svg>
          </Link>
          <button type="button" onClick={logout}>
            Logout
          </button>
        </div>
      </header>

      <div className="home-content">
        <section className="search-layout">
          <aside className="filters-panel">
            <h3>Genre</h3>
            <div className="filter-tags">
              {allGenres.map((genre) => {
                const checked = selectedGenres.includes(genre)
                return (
                  <button
                    type="button"
                    key={genre}
                    className={checked ? 'is-active' : ''}
                    onClick={() => {
                      const nextGenres = checked
                        ? selectedGenres.filter((item) => item !== genre)
                        : [...selectedGenres, genre]
                      updateFilters({ genres: nextGenres })
                    }}
                  >
                    {genre}
                  </button>
                )
              })}
            </div>

            <h3>Rating</h3>
            <select value={minRating} onChange={(e) => updateFilters({ minRating: e.target.value })}>
              <option value="">Any rating</option>
              <option value="5">5 stars</option>
              <option value="4">4 stars & up</option>
              <option value="3">3 stars & up</option>
            </select>

            <h3>Pacing</h3>
            <select value={pacing} onChange={(e) => updateFilters({ pacing: e.target.value })}>
              <option value="">Any pacing</option>
              <option value="SLOW">Slow</option>
              <option value="MEDIUM">Medium</option>
              <option value="FAST">Fast</option>
            </select>

            <h3>Publication Year</h3>
            <div className="year-row">
              <input
                type="number"
                placeholder="From"
                value={yearFrom}
                onChange={(e) => updateFilters({ yearFrom: e.target.value })}
              />
              <input
                type="number"
                placeholder="To"
                value={yearTo}
                onChange={(e) => updateFilters({ yearTo: e.target.value })}
              />
            </div>

            <label className="safe-toggle">
              <input
                type="checkbox"
                checked={contentSafe}
                onChange={(e) => updateFilters({ contentSafe: e.target.checked })}
              />
              Safe for all ages
            </label>
          </aside>

          <section className="results-panel">
            <p className="results-kicker">DISCOVERY</p>
            <h2>Search Results</h2>
            <p className="results-sub">{resultsCountText}</p>

            {loading && <p className="results-sub">Loading results...</p>}
            {!loading && booksPage.content.length === 0 && query && (
              <div className="empty-search">
                <p>No books found for "{query}".</p>
                <button type="button" onClick={() => navigate(`/books/new?query=${encodeURIComponent(query)}`)}>
                  Add this book to archive
                </button>
              </div>
            )}

            <div className="results-list">
              {booksPage.content.map((book) => (
                <article key={book.id} className="result-card" onClick={() => navigate(`/books/${book.id}`)}>
                  <div className="result-card__cover">
                    <img src={book.coverUrl || '/home-book.jpg'} alt={book.title} />
                  </div>
                  <div>
                    <h4>{book.title}</h4>
                    <p className="author">
                      {book.author}
                      {book.publicationYear ? `, ${book.publicationYear}` : ''}
                    </p>
                    <p className="card-meta">
                      <span>{renderStars(book.averageRating)} ({book.averageRating?.toFixed?.(1) || '0.0'})</span>
                      <span>{(book.genres || []).slice(0, 3).join(' • ') || 'Uncategorized'}</span>
                    </p>
                    <p className="desc">{book.description || 'No description yet.'}</p>
                    <button
                      type="button"
                      className="result-card__add-review"
                      onClick={(event) => {
                        event.stopPropagation()
                        navigate(`/books/${book.id}/review/new`)
                      }}
                    >
                      Add review
                    </button>
                  </div>
                </article>
              ))}
            </div>

            <div className="pagination">
              <button
                type="button"
                disabled={page <= 0}
                onClick={() => {
                  const params = new URLSearchParams(searchParams)
                  params.set('page', String(Math.max(page - 1, 0)))
                  setSearchParams(params)
                }}
              >
                Previous
              </button>
              <span>
                Page {booksPage.number + 1} of {Math.max(booksPage.totalPages, 1)}
              </span>
              <button
                type="button"
                disabled={page >= booksPage.totalPages - 1}
                onClick={() => {
                  const params = new URLSearchParams(searchParams)
                  params.set('page', String(page + 1))
                  setSearchParams(params)
                }}
              >
                Next
              </button>
            </div>
          </section>
        </section>
      </div>

      <footer className="home-footer">
        <h2>BookReviewer</h2>
        <nav>
          <Link to="/dashboard#trending">Library</Link>
          <Link to="/dashboard#collections">Collections</Link>
          <Link to="/feed">Feed</Link>
        </nav>
        <p>© 2026 BookReviewer. The Digital Archivist.</p>
      </footer>
    </main>
  )
}

export default SearchResultsPage
