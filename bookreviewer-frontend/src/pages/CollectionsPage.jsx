import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useInView } from 'react-intersection-observer'
import AppChrome from '../components/layout/AppChrome.jsx'
import { filterBooks, getGenres } from '../services/homeService.js'
import { genreKey } from '../utils/genre.js'
import { booksLabel } from '../utils/genreChart.js'
import { resolveMediaUrl } from '../utils/media.js'
import { collectionPath } from '../utils/search.js'
import './CollectionsPage.css'

const TONES = 5

/** One shelf card; its covers and size load once the card scrolls near the screen. */
function CollectionCard({ genre, tone }) {
  const { ref, inView } = useInView({ triggerOnce: true, rootMargin: '200px 0px', fallbackInView: true })
  const [preview, setPreview] = useState(null)

  useEffect(() => {
    if (!inView) return undefined
    let cancelled = false
    filterBooks({ genres: [genre], page: 0, size: 3, sort: 'averageRating,desc' })
      .then((result) => {
        if (!cancelled) {
          setPreview({ covers: Array.isArray(result?.content) ? result.content : [], total: Number(result?.totalElements) || 0 })
        }
      })
      .catch(() => {
        if (!cancelled) setPreview({ covers: [], total: null })
      })
    return () => {
      cancelled = true
    }
  }, [genre, inView])

  return (
    <li ref={ref}>
      <Link className={`shelf-card shelf-card--tone-${tone}`} to={collectionPath(genre)}>
        <span className="shelf-card__stage" aria-hidden="true">
          {(preview?.covers?.length ? preview.covers : [null, null, null]).slice(0, 3).map((book, index) => (
            book
              ? <img key={book.id} className={`shelf-card__cover shelf-card__cover--${index}`} src={resolveMediaUrl(book.coverUrl, '/home-book.jpg')} alt="" loading="lazy" />
              : <span key={index} className={`shelf-card__cover shelf-card__cover--${index} shelf-card__cover--blank`} />
          ))}
        </span>
        <span className="shelf-card__copy">
          <h3>{genre}</h3>
          <span className="shelf-card__count">
            {preview == null ? <span className="shelf-card__counting">Counting…</span> : preview.total == null ? 'Open shelf' : booksLabel(preview.total)}
          </span>
        </span>
      </Link>
    </li>
  )
}

/** Every genre in the archive as a browsable shelf. */
export default function CollectionsPage() {
  const [genres, setGenres] = useState(null)
  const [filter, setFilter] = useState('')

  useEffect(() => {
    let cancelled = false
    getGenres()
      .then((list) => {
        if (cancelled) return
        const unique = [...new Map((Array.isArray(list) ? list : []).map((name) => [genreKey(name), String(name).trim()])).values()]
        setGenres(unique.filter(Boolean).sort((a, b) => a.localeCompare(b)))
      })
      .catch(() => {
        if (!cancelled) setGenres([])
      })
    return () => {
      cancelled = true
    }
  }, [])

  const key = genreKey(filter)
  const shown = (genres || []).filter((genre) => !key || genreKey(genre).includes(key))
  const query = filter.trim()

  return (
    <AppChrome>
      <div className="home-content collections-page">
        <header className="collections-head">
          <div>
            <p className="collections-kicker">Stacks from the archive</p>
            <h2>Collections</h2>
            <p className="collections-head__lede">Every genre in the archive, one shelf each. Pick one and browse.</p>
          </div>
          <label className="collections-filter">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" aria-hidden="true">
              <circle cx="11" cy="11" r="6.5" />
              <path d="m20 20-4.4-4.4" />
            </svg>
            <input
              type="search"
              aria-label="Find a collection"
              placeholder="Find a collection…"
              value={filter}
              onChange={(event) => setFilter(event.target.value)}
            />
          </label>
        </header>

        {genres == null ? (
          <div className="shelf-grid shelf-grid--loading" aria-hidden="true">
            {Array.from({ length: 8 }, (_, index) => <div key={index} className="shelf-card shelf-card--skeleton" />)}
          </div>
        ) : (
          <>
            <p className="collections-count">
              {query ? `${shown.length} of ${genres.length} collections` : `${genres.length} collections`}
            </p>
            {shown.length > 0 ? (
              <ul className="shelf-grid" aria-label="Collections">
                {shown.map((genre, index) => (
                  <CollectionCard key={genre} genre={genre} tone={index % TONES} />
                ))}
              </ul>
            ) : (
              <div className="collection-message">
                <p>No collection matches “{query}”.</p>
                <Link className="collection-message__action" to={`/search?query=${encodeURIComponent(query)}&page=0`}>
                  Search the archive for “{query}”
                </Link>
              </div>
            )}
          </>
        )}
      </div>
    </AppChrome>
  )
}
