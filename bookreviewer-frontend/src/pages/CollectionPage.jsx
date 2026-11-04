import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import AppChrome from '../components/layout/AppChrome.jsx'
import { filterBooks } from '../services/homeService.js'
import { booksLabel } from '../utils/genreChart.js'
import { resolveMediaUrl } from '../utils/media.js'
import { collectionPath, relatedGenres } from '../utils/search.js'
import './CollectionsPage.css'

const PAGE_SIZE = 12
const SORTS = [
  { key: 'averageRating,desc', label: 'Top rated' },
  { key: 'totalReviews,desc', label: 'Most reviewed' },
  { key: 'publicationYear,desc', label: 'Newest' },
  { key: 'title,asc', label: 'A–Z' },
]

const STAR_PATH = 'm12 3.2 2.7 5.5 6 .9-4.35 4.25 1.03 6-5.38-2.83-5.38 2.83 1.03-6L3.3 9.6l6-.9z'

const Rating = ({ value }) => {
  const rating = Number(value) || 0
  const filled = Math.round(rating)
  return (
    <span className="collection-book__rating" role="img" aria-label={rating ? `Rated ${rating.toFixed(1)} out of 5` : 'Not rated yet'}>
      {Array.from({ length: 5 }, (_, index) => (
        <svg key={index} viewBox="0 0 24 24" width="13" height="13" aria-hidden="true" className={index < filled ? 'is-on' : ''}>
          <path d={STAR_PATH} />
        </svg>
      ))}
      {rating > 0 && <span>{rating.toFixed(1)}</span>}
    </span>
  )
}

/** A single collection: every book in one genre, sortable, with neighbouring collections to explore next. */
export default function CollectionPage() {
  const { genre = '' } = useParams()
  const [sort, setSort] = useState(SORTS[0].key)
  const requestKey = `${genre}|${sort}`
  const [state, setState] = useState({ key: null, books: [], page: 0, totalPages: 0, total: 0, error: false })
  const [loadingMore, setLoadingMore] = useState(false)

  useEffect(() => {
    let cancelled = false
    filterBooks({ genres: [genre], page: 0, size: PAGE_SIZE, sort })
      .then((result) => {
        if (cancelled) return
        setState({
          key: requestKey,
          books: Array.isArray(result?.content) ? result.content : [],
          page: 0,
          totalPages: Number(result?.totalPages) || 0,
          total: Number(result?.totalElements) || 0,
          error: false,
        })
      })
      .catch(() => {
        if (!cancelled) setState({ key: requestKey, books: [], page: 0, totalPages: 0, total: 0, error: true })
      })
    return () => {
      cancelled = true
    }
  }, [genre, sort, requestKey])

  const loading = state.key !== requestKey
  const books = loading ? [] : state.books
  const hasMore = !loading && state.page + 1 < state.totalPages

  const loadMore = async () => {
    setLoadingMore(true)
    try {
      const result = await filterBooks({ genres: [genre], page: state.page + 1, size: PAGE_SIZE, sort })
      setState((prev) => {
        if (prev.key !== requestKey) return prev
        const known = new Set(prev.books.map((book) => book.id))
        const next = (result?.content || []).filter((book) => !known.has(book.id))
        return { ...prev, books: [...prev.books, ...next], page: Number(result?.number ?? prev.page + 1) }
      })
    } finally {
      setLoadingMore(false)
    }
  }

  const related = relatedGenres(books, genre)
  const covers = books.slice(0, 3)

  return (
    <AppChrome>
      <div className="home-content collection-page">
        <Link className="collection-back" to="/collections">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M19 12H5M11 6l-6 6 6 6" />
          </svg>
          All collections
        </Link>

        <header className="collection-hero">
          <div className="collection-hero__copy">
            <p className="collections-kicker">Collection</p>
            <h2>{genre}</h2>
            <p className="collection-hero__count" aria-live="polite">
              {loading ? 'Counting the shelf…' : booksLabel(state.total)}
            </p>
          </div>
          {covers.length > 0 && (
            <div className="collection-hero__fan" aria-hidden="true">
              {covers.map((book, index) => (
                <img key={book.id} className={`collection-hero__cover collection-hero__cover--${index}`} src={resolveMediaUrl(book.coverUrl, '/home-book.jpg')} alt="" />
              ))}
            </div>
          )}
        </header>

        <div className="collection-toolbar">
          <div className="collection-sort" role="group" aria-label="Sort books">
            {SORTS.map((option) => (
              <button key={option.key} type="button" aria-pressed={sort === option.key} onClick={() => setSort(option.key)}>
                {option.label}
              </button>
            ))}
          </div>
          <Link className="collection-refine" to={`/search?genres=${encodeURIComponent(genre)}&page=0`}>
            Refine in the Library
            <span aria-hidden="true">→</span>
          </Link>
        </div>

        {loading && (
          <div className="collection-grid collection-grid--loading" aria-hidden="true">
            {Array.from({ length: 6 }, (_, index) => (
              <div key={index} className="collection-book collection-book--skeleton" />
            ))}
          </div>
        )}

        {!loading && state.error && (
          <p className="collection-message">We couldn’t load this collection. Please try again in a moment.</p>
        )}

        {!loading && !state.error && books.length === 0 && (
          <div className="collection-message">
            <p>No books in {genre} yet.</p>
            <Link className="collection-message__action" to="/books/new">
              Add the first book
            </Link>
          </div>
        )}

        {books.length > 0 && (
          <ul className="collection-grid" aria-label="Books in this collection">
            {books.map((book) => (
              <li key={book.id}>
                <Link className="collection-book" to={`/books/${book.id}`} aria-label={[book.title, book.author].filter(Boolean).join(', ')}>
                  <img className="collection-book__cover" src={resolveMediaUrl(book.coverUrl, '/home-book.jpg')} alt="" loading="lazy" />
                  <span className="collection-book__title">{book.title}</span>
                  {book.author && <span className="collection-book__author">{book.author}</span>}
                  <Rating value={book.averageRating} />
                </Link>
              </li>
            ))}
          </ul>
        )}

        {hasMore && (
          <div className="collection-more">
            <button type="button" onClick={loadMore} disabled={loadingMore}>
              {loadingMore ? 'Loading…' : 'Show more books'}
            </button>
          </div>
        )}

        {related.length > 0 && (
          <nav className="collection-related" aria-label="Often shelved with">
            <p className="collections-kicker">Often shelved with</p>
            <div className="collection-related__chips">
              {related.map((name) => (
                <Link key={name} to={collectionPath(name)}>
                  {name}
                </Link>
              ))}
            </div>
          </nav>
        )}
      </div>
    </AppChrome>
  )
}
