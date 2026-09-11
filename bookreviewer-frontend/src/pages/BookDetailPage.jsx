import { useEffect, useMemo, useRef, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import AppChrome from '../components/layout/AppChrome.jsx'
import { resolveMediaUrl } from '../utils/media.js'
import {
  clearBookStatus,
  getBookDetail,
  getBookReviews,
  getBookStatus,
  setBookStatus,
} from '../services/bookService.js'
import './BookDetailPage.css'

const toStatusLabel = (status) => {
  if (!status) return 'Not set'

  const map = {
    WANT_TO_READ: 'Want to read',
    READING: 'Reading',
    READ: 'Read',
    ABANDONED: 'Abandoned',
    PENDING: 'Reading',
  }
  return map[status] || status
}

const BookDetailPage = () => {
  const { id } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const reviewPageSearchInProgress = useRef(false)

  const [book, setBook] = useState(null)
  const [reviewsPage, setReviewsPage] = useState({ content: [], totalPages: 0, number: 0 })
  const [reviewPage, setReviewPage] = useState(0)
  const [includeSpoilers, setIncludeSpoilers] = useState(false)
  const [currentStatus, setCurrentStatus] = useState('')
  const [loading, setLoading] = useState(true)

  const targetReviewId = useMemo(() => {
    const hash = location.hash || ''
    if (!hash.startsWith('#review-')) return null
    const parsed = Number(hash.replace('#review-', ''))
    return Number.isFinite(parsed) ? parsed : null
  }, [location.hash])

  useEffect(() => {
    const loadBook = async () => {
      setLoading(true)
      try {
        const data = await getBookDetail(id)
        setBook(data)
        setCurrentStatus(data.userReadingStatus || '')
      } finally {
        setLoading(false)
      }
    }
    loadBook()
  }, [id])

  useEffect(() => {
    const loadStatus = async () => {
      try {
        const status = await getBookStatus(id)
        if (status?.status) {
          setCurrentStatus(status.status)
        }
      } catch {
        // 204/unauthorized can happen; ignore and use book response status.
      }
    }
    loadStatus()
  }, [id])

  useEffect(() => {
    const loadReviews = async () => {
      const data = await getBookReviews(id, { page: reviewPage, size: 4, includeSpoilers })
      setReviewsPage(data)
    }
    loadReviews()
  }, [id, includeSpoilers, reviewPage])

  useEffect(() => {
    if (!targetReviewId || reviewsPage.content.length === 0) return

    const directTarget = document.getElementById(`review-${targetReviewId}`)
    if (directTarget) {
      directTarget.scrollIntoView({ behavior: 'smooth', block: 'start' })
      return
    }

    const hasAnyPages = Number.isFinite(reviewsPage.totalPages) && reviewsPage.totalPages > 1
    if (!hasAnyPages || reviewPageSearchInProgress.current) return

    reviewPageSearchInProgress.current = true
    ;(async () => {
      try {
        for (let pageIndex = 0; pageIndex < reviewsPage.totalPages; pageIndex += 1) {
          if (pageIndex === reviewPage) continue
          const pageData = await getBookReviews(id, { page: pageIndex, size: 4, includeSpoilers })
          const found = (pageData?.content || []).some((review) => Number(review.id) === targetReviewId)
          if (found) {
            setReviewPage(pageIndex)
            return
          }
        }
      } finally {
        reviewPageSearchInProgress.current = false
      }
    })()
  }, [id, includeSpoilers, reviewPage, reviewsPage.content, reviewsPage.totalPages, targetReviewId])

  const stats = book?.ratingStats || { average: 0, total: 0, distribution: {} }
  const distribution = useMemo(() => {
    const result = []
    for (let star = 5; star >= 1; star -= 1) {
      const value = stats.distribution?.[star] ?? stats.distribution?.[String(star)] ?? 0
      const percent = stats.total ? Math.round((value / stats.total) * 100) : 0
      result.push({ star, value, percent })
    }
    return result
  }, [stats.distribution, stats.total])

  const sortedReviews = useMemo(() => {
    return [...(reviewsPage.content || [])].sort((a, b) => {
      const timeA = new Date(a?.createdAt || 0).getTime()
      const timeB = new Date(b?.createdAt || 0).getTime()
      if (timeA !== timeB) return timeB - timeA
      return Number(b?.id || 0) - Number(a?.id || 0)
    })
  }, [reviewsPage.content])

  const handleSetStatus = async (status) => {
    if (currentStatus === status) {
      await clearBookStatus(id)
      setCurrentStatus('')
      return
    }

    await setBookStatus(id, status)
    setCurrentStatus(status)
  }

  if (loading) {
    return (
      <AppChrome>
        <p className="detail-loading">Loading book details...</p>
      </AppChrome>
    )
  }

  if (!book) {
    return (
      <AppChrome>
        <p className="detail-loading">Book not found.</p>
      </AppChrome>
    )
  }

  return (
    <AppChrome>
      <div className="home-content">
        <section className="book-hero">
          <aside className="book-cover-col">
            <img src={resolveMediaUrl(book.coverUrl, '/home-book.jpg')} alt={book.title} />
            <div className="status-actions">
              <button
                className={currentStatus === 'WANT_TO_READ' ? 'is-active' : ''}
                type="button"
                onClick={() => handleSetStatus('WANT_TO_READ')}
              >
                Want to Read
              </button>
              <button
                className={currentStatus === 'READING' ? 'is-active' : ''}
                type="button"
                onClick={() => handleSetStatus('READING')}
              >
                Reading
              </button>
              <button
                className={currentStatus === 'READ' ? 'is-active' : ''}
                type="button"
                onClick={() => handleSetStatus('READ')}
              >
                Read
              </button>
              <button
                type="button"
                className="add-review-btn"
                onClick={() => navigate(`/books/${id}/review/new`)}
              >
                Add review
              </button>
            </div>
          </aside>

          <section className="book-main">
            <p className="book-kicker">
              {[
                book.publicationYear ? String(book.publicationYear) : null,
                (book.genres || []).slice(0, 2).join(' · ') || null,
                `★ ${(stats.average?.toFixed?.(1) || '0.0')} (${stats.total || 0})`,
              ]
                .filter(Boolean)
                .join(' · ')}
            </p>
            <h2>{book.title}</h2>
            <p className="book-author">by {book.author}</p>

            <div className="book-stats-row">
              <span>★ {stats.average?.toFixed?.(1) || '0.0'} ({stats.total || 0} reviews)</span>
              <span>{book.publicationYear ? `Published ${book.publicationYear}` : 'Year unknown'}</span>
              <span>Status: {toStatusLabel(currentStatus)}</span>
            </div>

            <div className="book-genres">
              {(book.genres || []).map((genre) => (
                <span key={genre}>{genre}</span>
              ))}
            </div>

            <article className="synopsis">
              <h3>The Synopsis</h3>
              <p>{book.description || 'No synopsis is available for this title yet.'}</p>
            </article>
          </section>
        </section>

        <section className="critical">
          <h3>Critical Discourse</h3>
          <div className="rating-summary">
            <div className="rating-box">
              <strong>{stats.average?.toFixed?.(1) || '0.0'}</strong>
              <span>Global Reader Index</span>
            </div>
            <div className="distribution">
              {distribution.map((row) => (
                <div key={row.star} className="dist-row">
                  <span>{row.star}</span>
                  <div className="bar">
                    <i style={{ width: `${row.percent}%` }} />
                  </div>
                  <span>{row.percent}%</span>
                </div>
              ))}
            </div>
          </div>

          <div className="reviews-grid">
            {sortedReviews.map((review) => (
              <article key={review.id} id={`review-${review.id}`} className="review-card">
                <header>
                  <div className="reviewer">
                    <img
                      src={resolveMediaUrl(review.user?.avatarUrl, '/user-stub.png')}
                      alt={review.user?.username || 'Reviewer'}
                    />
                    <div>
                      <h4>{review.user?.username || 'Anonymous'}</h4>
                      <p>{review.user?.badge || 'Reviewer'} · {review.user?.booksReviewed || 0} books</p>
                    </div>
                  </div>
                  <span className="review-stars">{'★'.repeat(review.rating || 0)}{'☆'.repeat(5 - (review.rating || 0))}</span>
                </header>
                <div className="review-body">
                  <div className="review-body__left">
                    <p className="review-label">Who this book is for</p>
                    <p>{review.whoIsItFor || 'No details provided.'}</p>
                    <p className="review-label">Who this book is not for</p>
                    <p>{review.whoIsItNotFor || 'No details provided.'}</p>
                  </div>
                  <aside className="review-verdict">
                    <p className="review-label verdict-label">The verdict</p>
                    <p>{review.verdict || review.detailedReview || 'No verdict provided.'}</p>
                  </aside>
                </div>
                <div className="review-tags">
                  {(review.tags || []).map((tag) => (
                    <span key={tag}>{tag}</span>
                  ))}
                </div>
                {includeSpoilers && review.hasSpoiler && review.spoilerContent && (
                  <p className="spoiler-text">{review.spoilerContent}</p>
                )}
              </article>
            ))}
          </div>

          <div className="pagination">
            <button
              type="button"
              disabled={reviewPage <= 0}
              onClick={() => setReviewPage((p) => Math.max(p - 1, 0))}
            >
              Previous reviews
            </button>
            <span>Page {reviewsPage.number + 1} of {Math.max(reviewsPage.totalPages, 1)}</span>
            <button
              type="button"
              disabled={reviewPage >= reviewsPage.totalPages - 1}
              onClick={() => setReviewPage((p) => p + 1)}
            >
              Next reviews
            </button>
          </div>
        </section>

        <section className="spoiler-gate">
          <p>ARCHIVAL SPOILERS AHEAD</p>
          <h3>Are you ready to see the other lives?</h3>
          <button type="button" onClick={() => setIncludeSpoilers((v) => !v)}>
            {includeSpoilers ? 'Hide archived secrets' : 'Reveal archived secrets'}
          </button>
        </section>
      </div>
    </AppChrome>
  )
}

export default BookDetailPage
