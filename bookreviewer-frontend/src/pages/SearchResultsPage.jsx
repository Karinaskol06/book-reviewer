import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import AppChrome from '../components/layout/AppChrome.jsx'
import { resolveMediaUrl } from '../utils/media.js'
import { filterBooks, getGenres } from '../services/homeService.js'
import './SearchResultsPage.css'

const renderStars = (rating = 0) => {
  const rounded = Math.round(rating)
  return '★★★★★'.slice(0, rounded) + '☆☆☆☆☆'.slice(0, 5 - rounded)
}

const SearchResultsPage = () => {
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
    <AppChrome>
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
                    <img src={resolveMediaUrl(book.coverUrl, '/home-book.jpg')} alt={book.title} />
                  </div>
                  <div className="result-card__body">
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
    </AppChrome>
  )
}

export default SearchResultsPage
