import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import AppChrome from '../components/layout/AppChrome.jsx'
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
  const [book, setBook] = useState(null)
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
    return (
      <AppChrome>
        <p className="detail-loading">Loading review form...</p>
      </AppChrome>
    )
  }

  return (
    <AppChrome>
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
    </AppChrome>
  )
}

export default WriteReviewPage
