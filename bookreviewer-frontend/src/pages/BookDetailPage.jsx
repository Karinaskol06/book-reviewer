import { useEffect, useMemo, useRef, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router-dom'
import AppChrome from '../components/layout/AppChrome.jsx'
import { useAuth } from '../hooks/useAuth.js'
import { resolveMediaUrl } from '../utils/media.js'
import {
  clearBookStatus,
  deleteReview,
  getBookDetail,
  getBookReviews,
  getBookStatus,
  setBookStatus,
  toggleReviewHelpful,
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

const toPacingLabel = (pacing) => {
  if (!pacing) return null
  const map = { SLOW: 'Slow', MEDIUM: 'Medium', FAST: 'Fast' }
  return map[pacing] || pacing
}

const shortenText = (value, max = 140) => {
  const text = String(value || '').trim()
  if (!text) return ''
  if (text.length <= max) return text
  return `${text.slice(0, max).trimEnd()}…`
}

const reviewMoods = (review) => {
  const moods = Array.isArray(review?.mood) ? review.mood : []
  if (moods.length) return moods
  return Array.isArray(review?.tags) ? review.tags : []
}

const BookDetailPage = () => {
  const { id } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const { user } = useAuth()
  const reviewPageSearchInProgress = useRef(false)
  const [helpfulBusyId, setHelpfulBusyId] = useState(null)

  const [book, setBook] = useState(null)
  const [reviewsPage, setReviewsPage] = useState({ content: [], totalPages: 0, number: 0 })
  const [reviewPage, setReviewPage] = useState(0)
  const [includeSpoilers, setIncludeSpoilers] = useState(false)
  const [currentStatus, setCurrentStatus] = useState('')
  const [loading, setLoading] = useState(true)
  const [activeReview, setActiveReview] = useState(null)
  const [deletingReviewId, setDeletingReviewId] = useState(null)

  const isOwnReview = (review) => (
    user?.userId != null && Number(review?.user?.id) === Number(user.userId)
  )

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

    const match = (reviewsPage.content || []).find((review) => Number(review.id) === targetReviewId)
    if (match) {
      setActiveReview(match)
      document.getElementById(`review-${targetReviewId}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
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
          const found = (pageData?.content || []).find((review) => Number(review.id) === targetReviewId)
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

  useEffect(() => {
    if (!activeReview) return undefined
    const onKeyDown = (event) => {
      if (event.key === 'Escape') setActiveReview(null)
    }
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.body.style.overflow = previousOverflow
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [activeReview])

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

  const handleToggleHelpful = async (review) => {
    if (!review?.id || helpfulBusyId != null) return
    if (isOwnReview(review)) return

    setHelpfulBusyId(review.id)
    try {
      await toggleReviewHelpful(review.id)
      const marked = Boolean(review.hasHelpful)
      const nextHelpful = {
        hasHelpful: !marked,
        helpfulCount: Math.max(0, (review.helpfulCount || 0) + (marked ? -1 : 1)),
      }
      setReviewsPage((prev) => ({
        ...prev,
        content: (prev.content || []).map((entry) => (
          Number(entry.id) === Number(review.id) ? { ...entry, ...nextHelpful } : entry
        )),
      }))
      setActiveReview((prev) => (
        prev && Number(prev.id) === Number(review.id) ? { ...prev, ...nextHelpful } : prev
      ))
    } finally {
      setHelpfulBusyId(null)
    }
  }

  const handleDeleteReview = async (review) => {
    if (!review?.id || !isOwnReview(review) || deletingReviewId != null) return
    const confirmed = window.confirm('Delete this review? This cannot be undone.')
    if (!confirmed) return

    setDeletingReviewId(review.id)
    try {
      await deleteReview(review.id)
      setActiveReview((prev) => (prev && Number(prev.id) === Number(review.id) ? null : prev))
      setReviewsPage((prev) => ({
        ...prev,
        content: (prev.content || []).filter((entry) => Number(entry.id) !== Number(review.id)),
        totalElements: Math.max(0, (prev.totalElements || 1) - 1),
      }))
      const refreshed = await getBookDetail(id)
      setBook(refreshed)
    } finally {
      setDeletingReviewId(null)
    }
  }

  const renderHelpfulButton = (review) => (
    <button
      type="button"
      className={`review-helpful__btn${review.hasHelpful ? ' is-active' : ''}`}
      disabled={
        helpfulBusyId === review.id
        || isOwnReview(review)
      }
      onClick={(event) => {
        event.stopPropagation()
        handleToggleHelpful(review)
      }}
    >
      {review.hasHelpful ? 'Helpful' : 'Mark helpful'}
      <span>{review.helpfulCount || 0}</span>
    </button>
  )

  const renderOwnerActions = (review) => {
    if (!isOwnReview(review)) return null
    return (
      <div className="review-owner-actions">
        <button
          type="button"
          className="review-owner-btn"
          onClick={(event) => {
            event.stopPropagation()
            navigate(`/books/${id}/review/${review.id}/edit`)
          }}
        >
          Edit
        </button>
        <button
          type="button"
          className="review-owner-btn review-owner-btn--danger"
          disabled={deletingReviewId === review.id}
          onClick={(event) => {
            event.stopPropagation()
            handleDeleteReview(review)
          }}
        >
          {deletingReviewId === review.id ? 'Deleting…' : 'Delete'}
        </button>
      </div>
    )
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
                className={currentStatus === 'ABANDONED' ? 'is-active' : ''}
                type="button"
                onClick={() => handleSetStatus('ABANDONED')}
              >
                Abandoned
              </button>
              {book.userHasReviewed ? (
                <button
                  type="button"
                  className="add-review-btn add-review-btn--done"
                  onClick={() => {
                    const own = (reviewsPage.content || []).find((review) => isOwnReview(review))
                    if (own?.id) {
                      navigate(`/books/${id}/review/${own.id}/edit`)
                      return
                    }
                    document.getElementById('critical-discourse')?.scrollIntoView({ behavior: 'smooth' })
                  }}
                >
                  Edit your review
                </button>
              ) : (
                <button
                  type="button"
                  className="add-review-btn"
                  onClick={() => navigate(`/books/${id}/review/new`)}
                >
                  Add review
                </button>
              )}
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
            <button
              type="button"
              className="ghost edit-book-btn"
              onClick={() => navigate(`/books/${id}/edit`)}
            >
              Edit book
            </button>

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

        <section className="critical" id="critical-discourse">
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
            {sortedReviews.map((review) => {
              const moods = reviewMoods(review).slice(0, 4)
              const warnings = Array.isArray(review.contentWarnings) ? review.contentWarnings : []
              const pacingLabel = toPacingLabel(review.pacing)
              const verdict = String(review.verdict || '').trim()
              const forReaders = shortenText(review.whoIsItFor, 120)

              return (
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
                    <span className="review-stars">
                      {'★'.repeat(review.rating || 0)}{'☆'.repeat(5 - (review.rating || 0))}
                    </span>
                  </header>

                  <div className="review-meta">
                    {pacingLabel && <span className="review-chip review-chip--pacing">Pacing · {pacingLabel}</span>}
                    {warnings.length > 0 && (
                      <span className="review-chip review-chip--warn">
                        {warnings.length} warning{warnings.length > 1 ? 's' : ''}
                      </span>
                    )}
                    {review.hasSpoiler && <span className="review-chip review-chip--spoiler">Spoilers</span>}
                  </div>

                  <div className="review-summary">
                    <p className="review-label">Verdict</p>
                    <p className="review-summary__verdict">{verdict || 'No verdict provided.'}</p>
                    <p className="review-label">Who this book is for</p>
                    <p>{forReaders || 'No details provided.'}</p>
                  </div>

                  {moods.length > 0 && (
                    <div className="review-tags">
                      {moods.map((tag) => (
                        <span key={tag}>{tag}</span>
                      ))}
                    </div>
                  )}

                  <div className="review-card__actions">
                    {renderHelpfulButton(review)}
                    <button type="button" className="review-open-btn" onClick={() => setActiveReview(review)}>
                      Read full review
                    </button>
                    {renderOwnerActions(review)}
                  </div>
                </article>
              )
            })}
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

      {activeReview && (
        <div
          className="review-modal-backdrop"
          role="presentation"
          onClick={() => setActiveReview(null)}
        >
          <div
            className="review-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby={`review-modal-title-${activeReview.id}`}
            onClick={(event) => event.stopPropagation()}
          >
            <header className="review-modal__header">
              <div className="reviewer">
                <img
                  src={resolveMediaUrl(activeReview.user?.avatarUrl, '/user-stub.png')}
                  alt={activeReview.user?.username || 'Reviewer'}
                />
                <div>
                  <h3 id={`review-modal-title-${activeReview.id}`}>
                    {activeReview.user?.username || 'Anonymous'}
                  </h3>
                  <p>
                    {activeReview.user?.badge || 'Reviewer'}
                    {' · '}
                    {activeReview.user?.booksReviewed || 0} books
                  </p>
                </div>
              </div>
              <div className="review-modal__header-right">
                <span className="review-stars">
                  {'★'.repeat(activeReview.rating || 0)}{'☆'.repeat(5 - (activeReview.rating || 0))}
                </span>
                <button
                  type="button"
                  className="review-modal__close"
                  aria-label="Close review"
                  onClick={() => setActiveReview(null)}
                >
                  ×
                </button>
              </div>
            </header>

            <div className="review-modal__meta">
              {toPacingLabel(activeReview.pacing) && (
                <span className="review-chip review-chip--pacing">
                  Pacing · {toPacingLabel(activeReview.pacing)}
                </span>
              )}
              {(activeReview.contentWarnings || []).length > 0 && (
                <span className="review-chip review-chip--warn">
                  {(activeReview.contentWarnings || []).length} warning
                  {(activeReview.contentWarnings || []).length > 1 ? 's' : ''}
                </span>
              )}
              {activeReview.hasSpoiler && (
                <span className="review-chip review-chip--spoiler">Contains spoilers</span>
              )}
            </div>

            <div className="review-modal__body">
              <section>
                <p className="review-label">The verdict</p>
                <p className="review-modal__verdict">
                  {activeReview.verdict || 'No verdict provided.'}
                </p>
              </section>

              <div className="review-modal__split">
                <section>
                  <p className="review-label">Who this book is for</p>
                  <p>{activeReview.whoIsItFor || 'No details provided.'}</p>
                </section>
                <section>
                  <p className="review-label">Who this book is not for</p>
                  <p>{activeReview.whoIsItNotFor || 'No details provided.'}</p>
                </section>
              </div>

              {activeReview.detailedReview && (
                <section>
                  <p className="review-label">Full review</p>
                  <p className="review-modal__long">{activeReview.detailedReview}</p>
                </section>
              )}

              {reviewMoods(activeReview).length > 0 && (
                <section>
                  <p className="review-label">Mood</p>
                  <div className="review-tags">
                    {reviewMoods(activeReview).map((tag) => (
                      <span key={tag}>{tag}</span>
                    ))}
                  </div>
                </section>
              )}

              {(activeReview.contentWarnings || []).length > 0 && (
                <section>
                  <p className="review-label">Content warnings</p>
                  <div className="review-tags review-tags--warn">
                    {activeReview.contentWarnings.map((warning) => (
                      <span key={warning}>{warning}</span>
                    ))}
                  </div>
                </section>
              )}

              {includeSpoilers && activeReview.hasSpoiler && activeReview.spoilerContent && (
                <section className="review-modal__spoiler">
                  <p className="review-label">Spoilers</p>
                  <p>{activeReview.spoilerContent}</p>
                </section>
              )}
            </div>

            <footer className="review-modal__footer">
              {renderHelpfulButton(activeReview)}
              {renderOwnerActions(activeReview)}
              <button type="button" className="review-open-btn" onClick={() => setActiveReview(null)}>
                Close
              </button>
            </footer>
          </div>
        </div>
      )}
    </AppChrome>
  )
}

export default BookDetailPage
