import { useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import AppChrome from '../components/layout/AppChrome.jsx'
import { useAuth } from '../hooks/useAuth.js'
import {
  createReview,
  getBookDetail,
  getBookStatus,
  getReview,
  setBookStatus,
  updateReview,
} from '../services/bookService.js'
import { resolveMediaUrl } from '../utils/media.js'
import './WriteReviewPage.css'

const moodOptions = [
  'SOMBER',
  'WHIMSICAL',
  'TENSE',
  'MELANCHOLIC',
  'HOPEFUL',
  'DARK',
  'HUMOROUS',
  'THOUGHT-PROVOKING',
  'ROMANTIC',
  'ADVENTUROUS',
  'INSPIRING',
  'HEARTBREAKING',
]
const warningOptions = [
  'Violence',
  'Strong Language',
  'Grief/Loss',
  'Substance Use',
  'Sexual Content',
  'Self-Harm',
  'Mental Illness',
  'Racism',
  'Eating Disorders',
]

const READING_STATUS_OPTIONS = [
  { value: 'WANT_TO_READ', label: 'Want to Read' },
  { value: 'READING', label: 'Reading' },
  { value: 'READ', label: 'Finished Reading' },
  { value: 'ABANDONED', label: 'Abandoned' },
]

const WriteReviewPage = () => {
  const { id, reviewId } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const isEditMode = Boolean(reviewId)
  const [book, setBook] = useState(null)
  const [saving, setSaving] = useState(false)
  const [loadError, setLoadError] = useState('')
  const [statusOpen, setStatusOpen] = useState(false)
  const statusMenuRef = useRef(null)
  const [form, setForm] = useState({
    rating: 4,
    readingStatus: 'READ',
    verdict: '',
    pacing: 'MEDIUM',
    mood: ['WHIMSICAL'],
    contentWarnings: [],
    detailedReview: '',
    whoIsItFor: '',
    whoIsItNotFor: '',
    spoilerContent: '',
    hasSpoiler: false,
  })

  useEffect(() => {
    const load = async () => {
      setLoadError('')
      try {
        const data = await getBookDetail(id)
        if (!isEditMode && data?.userHasReviewed) {
          navigate(`/books/${id}`, { replace: true })
          return
        }

        if (isEditMode) {
          const review = await getReview(reviewId, { includeSpoilers: true })
          if (Number(review.bookId) !== Number(id)) {
            navigate(`/books/${id}`, { replace: true })
            return
          }
          if (user?.userId != null && Number(review.user?.id) !== Number(user.userId)) {
            navigate(`/books/${id}`, { replace: true })
            return
          }

          let readingStatus = 'READ'
          try {
            const status = await getBookStatus(id)
            if (status?.status) readingStatus = status.status
          } catch {
            // keep default
          }

          setForm({
            rating: review.rating || 4,
            readingStatus,
            verdict: review.verdict || '',
            pacing: review.pacing || 'MEDIUM',
            mood: Array.isArray(review.mood) && review.mood.length
              ? review.mood
              : (Array.isArray(review.tags) ? review.tags : []),
            contentWarnings: Array.isArray(review.contentWarnings) ? review.contentWarnings : [],
            detailedReview: review.detailedReview || '',
            whoIsItFor: review.whoIsItFor || '',
            whoIsItNotFor: review.whoIsItNotFor || '',
            spoilerContent: review.spoilerContent || '',
            hasSpoiler: Boolean(review.hasSpoiler),
          })
        }

        setBook(data)
      } catch {
        setLoadError('Could not load this review.')
      }
    }
    load()
  }, [id, isEditMode, navigate, reviewId, user?.userId])

  useEffect(() => {
    if (!statusOpen) return undefined
    const onPointerDown = (event) => {
      if (!statusMenuRef.current?.contains(event.target)) {
        setStatusOpen(false)
      }
    }
    const onKeyDown = (event) => {
      if (event.key === 'Escape') setStatusOpen(false)
    }
    document.addEventListener('pointerdown', onPointerDown)
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('pointerdown', onPointerDown)
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [statusOpen])

  const readingStatusLabel =
    READING_STATUS_OPTIONS.find((option) => option.value === form.readingStatus)?.label
    || 'Finished Reading'

  const toggleInArray = (key, value) => {
    setForm((prev) => ({
      ...prev,
      [key]: prev[key].includes(value) ? prev[key].filter((v) => v !== value) : [...prev[key], value],
    }))
  }

  const submit = async (event) => {
    event.preventDefault()
    setSaving(true)
    try {
      await setBookStatus(id, form.readingStatus)
      const payload = {
        rating: form.rating,
        verdict: form.verdict,
        detailedReview: form.detailedReview || undefined,
        pacing: form.pacing,
        mood: form.mood,
        whoIsItFor: form.whoIsItFor,
        whoIsItNotFor: form.whoIsItNotFor,
        contentWarnings: form.contentWarnings,
        hasSpoiler: form.hasSpoiler,
        spoilerContent: form.hasSpoiler ? form.spoilerContent : undefined,
        tags: form.mood,
      }
      if (isEditMode) {
        await updateReview(reviewId, payload)
      } else {
        await createReview(id, payload)
      }
      navigate(`/books/${id}${isEditMode ? `#review-${reviewId}` : ''}`)
    } finally {
      setSaving(false)
    }
  }

  if (loadError) {
    return (
      <AppChrome>
        <p className="detail-loading">{loadError}</p>
      </AppChrome>
    )
  }

  if (!book) {
    return (
      <AppChrome>
        <p className="detail-loading">Loading review form...</p>
      </AppChrome>
    )
  }

  return (
    <AppChrome>
      <div className="home-content write-review-page">
        <form className="review-form-shell motion-surface" onSubmit={submit}>
          <section className="review-book-head">
            <div className="review-book-head__cover">
              <img src={resolveMediaUrl(book.coverUrl, '/home-book.jpg')} alt={book.title} />
            </div>
            <div className="review-book-head__copy">
              <p className="kicker">{isEditMode ? 'Edit your review' : 'Write a review'}</p>
              <h2>{book.title}</h2>
              <p className="author">by {book.author}</p>
              <div className="head-row">
                <div className="status-field" ref={statusMenuRef}>
                  <span className="status-field__label" id="reading-status-label">
                    Reading status
                  </span>
                  <button
                    type="button"
                    className={`status-select${statusOpen ? ' is-open' : ''}`}
                    aria-haspopup="listbox"
                    aria-expanded={statusOpen}
                    aria-labelledby="reading-status-label"
                    onClick={() => setStatusOpen((open) => !open)}
                  >
                    <span>{readingStatusLabel}</span>
                    <span className="status-select__chevron" aria-hidden="true" />
                  </button>
                  {statusOpen && (
                    <ul className="status-menu" role="listbox" aria-labelledby="reading-status-label">
                      {READING_STATUS_OPTIONS.map((option) => (
                        <li key={option.value} role="presentation">
                          <button
                            type="button"
                            role="option"
                            aria-selected={form.readingStatus === option.value}
                            className={`status-menu__option${form.readingStatus === option.value ? ' is-selected' : ''}`}
                            onClick={() => {
                              setForm((prev) => ({ ...prev, readingStatus: option.value }))
                              setStatusOpen(false)
                            }}
                          >
                            {option.label}
                          </button>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
                <div className="rating-field">
                  <span className="rating-field__label">Reader&apos;s rating</span>
                  <div className="stars-row" role="group" aria-label="Reader's rating">
                    {[1, 2, 3, 4, 5].map((value) => (
                      <button
                        key={value}
                        type="button"
                        className={`star-btn${form.rating >= value ? ' is-on' : ''}`}
                        aria-label={`${value} star${value > 1 ? 's' : ''}`}
                        aria-pressed={form.rating === value}
                        onClick={() => setForm((p) => ({ ...p, rating: value }))}
                      >
                        ★
                      </button>
                    ))}
                    <span className="stars-row__value">{form.rating} / 5</span>
                  </div>
                </div>
              </div>
            </div>
          </section>

          <section className="section-block">
            <p className="section-kicker">For other readers</p>
            <h3>Who should pick this up?</h3>
            <div className="two-cols">
              <label>
                Who is this book for?
                <textarea
                  value={form.whoIsItFor}
                  onChange={(e) => setForm((p) => ({ ...p, whoIsItFor: e.target.value }))}
                  placeholder="Readers who love slow burns, rich worlds, quiet endings..."
                  required
                />
              </label>
              <label>
                Who is this book not for?
                <textarea
                  value={form.whoIsItNotFor}
                  onChange={(e) => setForm((p) => ({ ...p, whoIsItNotFor: e.target.value }))}
                  placeholder="Anyone looking for a quick, light beach read..."
                  required
                />
              </label>
            </div>
            <label>
              Short verdict
              <input
                value={form.verdict}
                onChange={(e) => setForm((p) => ({ ...p, verdict: e.target.value }))}
                placeholder="One or two sentences that capture the book"
                required
              />
            </label>
          </section>

          <section className="section-block">
            <p className="section-kicker">Feel of the book</p>
            <h3>Pacing, mood &amp; warnings</h3>
            <label>
              Pacing
              <div className="segmented">
                {['SLOW', 'MEDIUM', 'FAST'].map((pacing) => (
                  <button
                    key={pacing}
                    type="button"
                    className={form.pacing === pacing ? 'active' : ''}
                    onClick={() => setForm((prev) => ({ ...prev, pacing }))}
                  >
                    {pacing === 'SLOW' ? 'Slow' : pacing === 'MEDIUM' ? 'Medium' : 'Fast'}
                  </button>
                ))}
              </div>
            </label>

            <label>
              Mood &amp; tone
              <div className="chips">
                {moodOptions.map((mood) => (
                  <button
                    key={mood}
                    type="button"
                    className={form.mood.includes(mood) ? 'active' : ''}
                    onClick={() => toggleInArray('mood', mood)}
                  >
                    {mood}
                  </button>
                ))}
              </div>
            </label>

            <div className="warnings">
              <p>Content warnings</p>
              <div>
                {warningOptions.map((warning) => (
                  <label key={warning} className="check warning-check">
                    <input
                      type="checkbox"
                      checked={form.contentWarnings.includes(warning)}
                      onChange={() => toggleInArray('contentWarnings', warning)}
                    />
                    <span className="warning-check__box" aria-hidden="true" />
                    <span className="warning-check__text">{warning}</span>
                  </label>
                ))}
              </div>
            </div>
          </section>

          <section className="section-block">
            <p className="section-kicker">Optional depth</p>
            <h3>The longer note</h3>
            <label>
              Detailed review
              <textarea
                className="long-form"
                value={form.detailedReview}
                onChange={(e) => setForm((p) => ({ ...p, detailedReview: e.target.value }))}
                placeholder="Share scenes, themes, or anything that stayed with you..."
              />
            </label>
          </section>

          <section className="section-block spoiler-block">
            <label className="check spoiler-toggle">
              <input
                type="checkbox"
                checked={form.hasSpoiler}
                onChange={(e) => setForm((p) => ({ ...p, hasSpoiler: e.target.checked }))}
              />
              Include a spoiler section
            </label>
            <textarea
              className="spoiler"
              placeholder="Spoilers stay hidden by default for other readers..."
              value={form.spoilerContent}
              disabled={!form.hasSpoiler}
              onChange={(e) => setForm((p) => ({ ...p, spoilerContent: e.target.value }))}
            />
          </section>

          <div className="review-actions">
            <p>
              {isEditMode
                ? 'Saving will update your review on this book’s page.'
                : 'Your review will appear on this book’s page for other readers.'}
            </p>
            <button type="button" className="ghost" onClick={() => navigate(-1)}>
              Cancel
            </button>
            <button type="submit" disabled={saving}>
              {saving ? (isEditMode ? 'Saving…' : 'Submitting…') : (isEditMode ? 'Save changes' : 'Submit review')}
            </button>
          </div>
        </form>
      </div>
    </AppChrome>
  )
}

export default WriteReviewPage
