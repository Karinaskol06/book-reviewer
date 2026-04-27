import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth.js'
import { createReview, getBookDetail, setBookStatus } from '../services/bookService.js'
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

const WriteReviewPage = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const { user, logout } = useAuth()
  const [book, setBook] = useState(null)
  const [headerSearch, setHeaderSearch] = useState('')
  const [saving, setSaving] = useState(false)
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
      const data = await getBookDetail(id)
      setBook(data)
    }
    load()
  }, [id])

  useEffect(() => {
    const q = headerSearch.trim()
    if (!q) return
    const timer = setTimeout(() => navigate(`/search?query=${encodeURIComponent(q)}&page=0`), 350)
    return () => clearTimeout(timer)
  }, [headerSearch, navigate])

  const stars = useMemo(() => '★'.repeat(form.rating) + '☆'.repeat(5 - form.rating), [form.rating])

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
      await createReview(id, {
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
      })
      navigate(`/books/${id}`)
    } finally {
      setSaving(false)
    }
  }

  if (!book) {
    return <main className="dashboard"><p className="detail-loading">Loading review form...</p></main>
  }

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
        <input className="home-nav__search" placeholder="Search the archive..." value={headerSearch} onChange={(e) => setHeaderSearch(e.target.value)} />
        <div className="home-nav__actions">
          <Link className="home-nav__profile" to="/profile" aria-label="My profile" title={user?.username || 'My profile'}>
            <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
              <path d="M12 12c2.76 0 5-2.24 5-5S14.76 2 12 2 7 4.24 7 7s2.24 5 5 5Zm0 2c-3.86 0-7 3.14-7 7 0 .55.45 1 1 1h12c.55 0 1-.45 1-1 0-3.86-3.14-7-7-7Z" />
            </svg>
          </Link>
          <button type="button" onClick={logout}>Logout</button>
        </div>
      </header>

      <div className="home-content write-review-page">
        <form className="review-form-shell" onSubmit={submit}>
          <section className="review-book-head">
            <img src={book.coverUrl || '/home-book.jpg'} alt={book.title} />
            <div>
              <p className="kicker">DIGITAL ARCHIVIST REVIEW</p>
              <h2>{book.title}</h2>
              <p className="author">by {book.author}</p>
              <div className="head-row">
                <label>
                  Reading Status
                  <select value={form.readingStatus} onChange={(e) => setForm((p) => ({ ...p, readingStatus: e.target.value }))}>
                    <option value="WANT_TO_READ">Want to Read</option>
                    <option value="READING">Reading</option>
                    <option value="READ">Finished Reading</option>
                    <option value="ABANDONED">Abandoned</option>
                  </select>
                </label>
                <label>
                  The Scholar&apos;s Rating
                  <div className="stars-row">
                    <input type="range" min="1" max="5" value={form.rating} onChange={(e) => setForm((p) => ({ ...p, rating: Number(e.target.value) }))} />
                    <span>{stars}</span>
                  </div>
                </label>
              </div>
            </div>
          </section>

          <section className="section-block">
            <h3>Decision-Focused Sections</h3>
            <div className="two-cols">
              <label>
                Who is this book for?
                <textarea value={form.whoIsItFor} onChange={(e) => setForm((p) => ({ ...p, whoIsItFor: e.target.value }))} required />
              </label>
              <label>
                Who is this book NOT for?
                <textarea value={form.whoIsItNotFor} onChange={(e) => setForm((p) => ({ ...p, whoIsItNotFor: e.target.value }))} required />
              </label>
            </div>
            <label>
              Short Verdict
              <input value={form.verdict} onChange={(e) => setForm((p) => ({ ...p, verdict: e.target.value }))} required />
            </label>
          </section>

          <section className="section-block">
            <h3>Detailed Breakdown</h3>
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
              Mood & Tone
              <div className="chips">
                {moodOptions.map((mood) => (
                  <button key={mood} type="button" className={form.mood.includes(mood) ? 'active' : ''} onClick={() => toggleInArray('mood', mood)}>
                    {mood}
                  </button>
                ))}
              </div>
            </label>

            <div className="warnings">
              <p>Content Warnings</p>
              <div>
                {warningOptions.map((warning) => (
                  <label key={warning} className="check">
                    <input type="checkbox" checked={form.contentWarnings.includes(warning)} onChange={() => toggleInArray('contentWarnings', warning)} />
                    {warning}
                  </label>
                ))}
              </div>
            </div>
          </section>

          <section className="section-block">
            <h3>The Long Form</h3>
            <label>
              Detailed Review (Optional)
              <textarea className="long-form" value={form.detailedReview} onChange={(e) => setForm((p) => ({ ...p, detailedReview: e.target.value }))} />
            </label>
          </section>

          <section className="section-block">
            <label className="check">
              <input type="checkbox" checked={form.hasSpoiler} onChange={(e) => setForm((p) => ({ ...p, hasSpoiler: e.target.checked }))} />
              Spoiler Section
            </label>
            <textarea
              className="spoiler"
              placeholder="Hide spoilers here. These will be hidden by default for other readers..."
              value={form.spoilerContent}
              disabled={!form.hasSpoiler}
              onChange={(e) => setForm((p) => ({ ...p, spoilerContent: e.target.value }))}
            />
          </section>

          <div className="review-actions">
            <p>By submitting, your review will be cataloged and shared with the scholarly community.</p>
            <button type="button" className="ghost" onClick={() => navigate(-1)}>Save Draft</button>
            <button type="submit" disabled={saving}>{saving ? 'Submitting...' : 'Submit Review'}</button>
          </div>
        </form>
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

export default WriteReviewPage
