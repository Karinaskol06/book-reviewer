import { useState } from 'react'
import { Link } from 'react-router-dom'
import { resolveMediaUrl } from '../../utils/media.js'
import './ProfileReviews.css'

const SORTS = [
  { key: 'newest', label: 'Newest' },
  { key: 'rating', label: 'Highest rated' },
]

const MAX_MOODS = 3
const dateFormat = new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'short', year: 'numeric' })

const timeOf = (entry) => {
  const time = new Date(entry.review?.createdAt).getTime()
  return Number.isNaN(time) ? 0 : time
}

const ratingOf = (entry) => Math.max(0, Math.min(5, Math.round(Number(entry.review?.rating) || 0)))

const sortEntries = (entries, sort) =>
  [...entries].sort((a, b) =>
    sort === 'rating' ? ratingOf(b) - ratingOf(a) || timeOf(b) - timeOf(a) : timeOf(b) - timeOf(a),
  )

const formatDate = (value) => {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : dateFormat.format(date)
}

const STAR_PATH = 'm12 3.2 2.7 5.5 6 .9-4.35 4.25 1.03 6-5.38-2.83-5.38 2.83 1.03-6L3.3 9.6l6-.9z'

const Stars = ({ value }) => (
  <span className="profile-review__stars" role="img" aria-label={`Rated ${value} out of 5`}>
    {Array.from({ length: 5 }, (_, index) => (
      <svg key={index} viewBox="0 0 24 24" width="16" height="16" aria-hidden="true" className={index < value ? 'is-on' : ''}>
        <path d={STAR_PATH} />
      </svg>
    ))}
  </span>
)

const ActionIcon = ({ name }) => (
  <svg
    viewBox="0 0 24 24"
    width="16"
    height="16"
    fill="none"
    stroke="currentColor"
    strokeWidth="1.8"
    strokeLinecap="round"
    strokeLinejoin="round"
    aria-hidden="true"
  >
    {name === 'edit' ? (
      <path d="M4 20h4L19 9a2.1 2.1 0 0 0-3-3L5 17v3zM14.5 7.5l2 2" />
    ) : (
      <path d="M4.5 7h15M9.5 7V4.5h5V7M6.5 7l.9 12.5h9.2L17.5 7M10 11v5M14 11v5" />
    )}
  </svg>
)

function ReviewCard({ entry, isOwnProfile, deleting, onEdit, onDelete }) {
  const { review } = entry
  const verdict = review.verdict || review.detailedReview || ''
  const moods = Array.isArray(review.mood) ? review.mood : []
  const helpful = Number(review.helpfulCount) || 0
  const reviewedOn = formatDate(review.createdAt)

  return (
    <article className="profile-review">
      <div className="profile-review__cover">
        <img src={resolveMediaUrl(entry.bookCover, '/home-book.jpg')} alt="" loading="lazy" />
      </div>

      <div className="profile-review__body">
        <header className="profile-review__head">
          <div className="profile-review__titles">
            <h4 className="profile-review__title">
              {/* Stretched link: the whole card opens the review on the book page. */}
              <Link to={`/books/${entry.bookId}#review-${review.id}`}>{entry.bookTitle}</Link>
            </h4>
            {entry.bookAuthor && <p className="profile-review__author">{entry.bookAuthor}</p>}
          </div>
          <Stars value={ratingOf(entry)} />
        </header>

        {verdict && <blockquote className="profile-review__verdict">“{verdict}”</blockquote>}

        {review.whoIsItFor && (
          <p className="profile-review__for">
            <span>For</span> {review.whoIsItFor}
          </p>
        )}

        {moods.length > 0 && (
          <ul className="profile-review__moods" aria-label="Mood">
            {moods.slice(0, MAX_MOODS).map((mood) => (
              <li key={mood}>{mood}</li>
            ))}
            {moods.length > MAX_MOODS && <li>+{moods.length - MAX_MOODS}</li>}
          </ul>
        )}
      </div>

      <footer className="profile-review__foot">
        <p className="profile-review__meta">
          {reviewedOn ? `Reviewed ${reviewedOn}` : 'Reviewed recently'}
          {helpful > 0 && (
            <>
              <span aria-hidden="true"> · </span>
              {helpful} found this helpful
            </>
          )}
        </p>
        {isOwnProfile && (
          <div className="profile-review__actions">
            <button
              type="button"
              className="profile-review__action"
              aria-label={`Edit review of ${entry.bookTitle}`}
              onClick={() => onEdit(entry)}
            >
              <ActionIcon name="edit" />
              Edit
            </button>
            <button
              type="button"
              className="profile-review__action profile-review__action--danger"
              aria-label={`Delete review of ${entry.bookTitle}`}
              disabled={deleting}
              onClick={() => onDelete(entry)}
            >
              <ActionIcon name="delete" />
              {deleting ? 'Deleting…' : 'Delete'}
            </button>
          </div>
        )}
      </footer>
    </article>
  )
}

/**
 * Profile "My Reviews" section: a sortable list of review cards. Each card opens the review on its book page;
 * the owner also gets Edit and Delete.
 */
export default function ProfileReviews({ entries, isOwnProfile, ownerName, deletingReviewId, onEdit, onDelete }) {
  const [sort, setSort] = useState('newest')
  const list = Array.isArray(entries) ? entries : []
  const sorted = sortEntries(list, sort)

  return (
    <section className="profile-reviews" aria-labelledby="profile-reviews-title">
      <div className="profile-reviews__header">
        <h3 id="profile-reviews-title" className="profile-reviews__title">
          {isOwnProfile ? 'My Reviews' : `${ownerName}'s Reviews`}
        </h3>
        {list.length > 0 && <span className="profile-reviews__count">{list.length}</span>}
        {list.length > 1 && (
          <div className="profile-reviews__sort" role="group" aria-label="Sort reviews">
            {SORTS.map(({ key, label }) => (
              <button key={key} type="button" aria-pressed={sort === key} onClick={() => setSort(key)}>
                {label}
              </button>
            ))}
          </div>
        )}
      </div>

      {list.length === 0 ? (
        <div className="profile-reviews__empty">
          {isOwnProfile ? (
            <>
              <p>You haven’t reviewed any books yet.</p>
              <Link to="/search?page=0">Find a book to review</Link>
            </>
          ) : (
            <p>No reviews yet.</p>
          )}
        </div>
      ) : (
        <div className="profile-reviews__list">
          {sorted.map((entry) => (
            <ReviewCard
              key={entry.review.id}
              entry={entry}
              isOwnProfile={isOwnProfile}
              deleting={deletingReviewId != null && Number(deletingReviewId) === Number(entry.review.id)}
              onEdit={onEdit}
              onDelete={onDelete}
            />
          ))}
        </div>
      )}
    </section>
  )
}
