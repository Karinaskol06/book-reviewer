import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useAuth } from '../hooks/useAuth.js'
import { getBookDetail } from '../services/bookService.js'
import { getBooksByGenre, getTrendingBooks } from '../services/homeService.js'
import { resolveMediaUrl } from '../utils/media.js'
import {
  exportReadingListPdf,
  getMyProfile,
  getMyReviews,
  getUserProfileById,
  getUserLibrary,
  getUserLibraryByUserId,
  getUserReviewsByUserId,
  updateAboutMe,
  uploadAvatar,
} from '../services/profileService.js'
import './UserProfilePage.css'

const UserProfilePage = () => {
  const MotionArticle = motion.article
  const navigate = useNavigate()
  const { id } = useParams()
  const { logout } = useAuth()
  const isOwnProfile = !id

  const [profile, setProfile] = useState(null)
  const [headerSearch, setHeaderSearch] = useState('')
  const [aboutMe, setAboutMe] = useState('')
  const [aboutSaveState, setAboutSaveState] = useState('idle')
  const [avatarPreviewUrl, setAvatarPreviewUrl] = useState('')
  const [avatarSaveState, setAvatarSaveState] = useState('idle')
  const [currentlyReadingBooks, setCurrentlyReadingBooks] = useState([])
  const [wantToReadBooks, setWantToReadBooks] = useState([])
  const [readBooks, setReadBooks] = useState([])
  const [myReviews, setMyReviews] = useState([])
  const [genreCounts, setGenreCounts] = useState({})
  const [recommendations, setRecommendations] = useState([])
  const [recommendationsLoading, setRecommendationsLoading] = useState(true)

  useEffect(() => {
    const q = headerSearch.trim()
    if (!q) return
    const timer = setTimeout(() => navigate(`/search?query=${encodeURIComponent(q)}&page=0`), 350)
    return () => clearTimeout(timer)
  }, [headerSearch, navigate])

  useEffect(() => {
    const load = async () => {
      const getReviewsPage = async (userId, page = 0, size = 50) => {
        if (isOwnProfile) {
          return getMyReviews({ page, size, includeSpoilers: true })
        }
        return getUserReviewsByUserId(userId, { page, size, includeSpoilers: true })
      }

      const getAllReviews = async (userId) => {
        const pageSize = 50
        const firstPage = await getReviewsPage(userId, 0, pageSize)
        const firstContent = Array.isArray(firstPage?.content) ? firstPage.content : []
        const totalPages = Number(firstPage?.totalPages) || 1
        if (totalPages <= 1) return firstContent

        const restPages = await Promise.all(
          Array.from({ length: totalPages - 1 }, (_, index) => getReviewsPage(userId, index + 1, pageSize)),
        )

        return firstContent.concat(
          restPages.flatMap((page) => (Array.isArray(page?.content) ? page.content : [])),
        )
      }

      const currentProfile = isOwnProfile ? await getMyProfile() : await getUserProfileById(id)
      setProfile(currentProfile)
      setAboutMe(currentProfile.aboutMe || '')

      const [readingStatuses, wantStatuses, readStatuses, allStatuses] = isOwnProfile
        ? await Promise.all([getUserLibrary('READING'), getUserLibrary('WANT_TO_READ'), getUserLibrary('READ'), getUserLibrary()])
        : await Promise.all([
          getUserLibraryByUserId(currentProfile.id, 'READING'),
          getUserLibraryByUserId(currentProfile.id, 'WANT_TO_READ'),
          getUserLibraryByUserId(currentProfile.id, 'READ'),
          getUserLibraryByUserId(currentProfile.id),
        ])

      const readingBooks = await Promise.all(readingStatuses.map((item) => getBookDetail(item.bookId)))
      const wantBooks = await Promise.all(wantStatuses.map((item) => getBookDetail(item.bookId)))
      const doneBooks = await Promise.all(readStatuses.map((item) => getBookDetail(item.bookId)))
      setCurrentlyReadingBooks(readingBooks)
      setWantToReadBooks(wantBooks)
      setReadBooks(doneBooks)

      const allBooks = await Promise.all(allStatuses.map((item) => getBookDetail(item.bookId)))
      const genres = {}
      allBooks.forEach((book) => {
        ;(book.genres || []).forEach((genre) => {
          genres[genre] = (genres[genre] || 0) + 1
        })
      })
      setGenreCounts(genres)

      const collectedReviews = []
      const reviews = await getAllReviews(currentProfile.id)
      const uniqueBookIds = [...new Set(reviews.map((review) => review.bookId).filter(Boolean))]
      const reviewBooks = await Promise.all(uniqueBookIds.map((bookId) => getBookDetail(bookId)))
      const booksById = new Map(reviewBooks.filter(Boolean).map((book) => [book.id, book]))

      reviews.forEach((review) => {
        const relatedBook = booksById.get(review.bookId)
        collectedReviews.push({
          review,
          bookId: review.bookId,
          bookTitle: relatedBook?.title || 'Untitled Book',
          bookAuthor: relatedBook?.author || '',
        })
      })
      setMyReviews(
        collectedReviews
          .sort((a, b) => new Date(b.review?.createdAt || 0).getTime() - new Date(a.review?.createdAt || 0).getTime()),
      )

      const topGenreNames = Object.entries(genres)
        .sort((a, b) => b[1] - a[1])
        .slice(0, 3)
        .map(([genre]) => genre)

      const ownedBookIds = new Set(allBooks.map((book) => book.id))
      setRecommendationsLoading(true)
      try {
        const perGenreResponses = await Promise.all(topGenreNames.map((genre) => getBooksByGenre(genre, 4)))
        const trendingBooks = await getTrendingBooks(12)
        const assembled = []
        const seenIds = new Set()

        perGenreResponses.forEach((response, index) => {
          const genre = topGenreNames[index]
          const candidates = Array.isArray(response?.content) ? response.content : response
          ;(candidates || []).forEach((book) => {
            if (!book?.id || ownedBookIds.has(book.id) || seenIds.has(book.id)) return
            seenIds.add(book.id)
            assembled.push({ ...book, reason: `Because you enjoy ${genre}` })
          })
        })

        ;(trendingBooks || []).forEach((book) => {
          if (!book?.id || ownedBookIds.has(book.id) || seenIds.has(book.id)) return
          seenIds.add(book.id)
          assembled.push({ ...book, reason: 'Trending in the archive' })
        })

        setRecommendations(assembled.slice(0, 6))
      } finally {
        setRecommendationsLoading(false)
      }
    }
    load()
  }, [id, isOwnProfile])

  const topGenres = useMemo(
    () =>
      Object.entries(genreCounts)
        .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))
        .slice(0, 4),
    [genreCounts],
  )

  const moodCounts = useMemo(() => {
    const counts = {}
    myReviews.forEach(({ review }) => {
      ;(review?.mood || []).forEach((mood) => {
        counts[mood] = (counts[mood] || 0) + 1
      })
    })
    return counts
  }, [myReviews])

  const topMoods = useMemo(
    () =>
      Object.entries(moodCounts)
        .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))
        .slice(0, 3)
        .map(([mood]) => mood),
    [moodCounts],
  )

  const saveAbout = async () => {
    setAboutSaveState('saving')
    await updateAboutMe(aboutMe)
    setProfile((prev) => ({ ...prev, aboutMe }))
    setAboutSaveState('saved')
    setTimeout(() => setAboutSaveState('idle'), 1800)
  }

  const onAvatarChange = async (event) => {
    const file = event.target.files?.[0]
    if (!file) return
    const maxBytes = 10 * 1024 * 1024
    if (file.size > maxBytes) {
      setAvatarSaveState('tooLarge')
      event.target.value = ''
      return
    }
    const reader = new FileReader()
    reader.onload = async () => {
      const localDataUrl = String(reader.result || '')
      setAvatarPreviewUrl(localDataUrl)
      setAvatarSaveState('saving')
      try {
        const uploaded = await uploadAvatar(file)
        const normalizedUrl = resolveMediaUrl(uploaded.avatarUrl)
        const cacheBusted = `${normalizedUrl}${normalizedUrl.includes('?') ? '&' : '?'}t=${Date.now()}`
        setProfile((prev) => ({ ...prev, avatarUrl: cacheBusted }))
        setAvatarSaveState('saved')
        setTimeout(() => {
          setAvatarSaveState('idle')
          setAvatarPreviewUrl('')
        }, 1800)
      } catch {
        // Keep local preview visible even when upload fails.
        setAvatarSaveState('failed')
      }
      event.target.value = ''
    }
    reader.readAsDataURL(file)
  }

  const downloadPdf = async () => {
    const blob = await exportReadingListPdf()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'reading-list.pdf'
    a.click()
    URL.revokeObjectURL(url)
  }

  if (!profile) {
    return <main className="dashboard"><p className="detail-loading">Loading profile...</p></main>
  }

  const formatReviewDate = (value) => {
    if (!value) return 'Recently'
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return 'Recently'
    return date.toLocaleDateString()
  }

  const renderReviewStars = (rating = 0) => {
    const normalized = Math.max(0, Math.min(5, Number(rating) || 0))
    return '★'.repeat(normalized) + '☆'.repeat(5 - normalized)
  }

  const maxGenre = Math.max(...topGenres.map(([, count]) => count), 1)

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
          placeholder="Search the archive..."
          value={headerSearch}
          onChange={(e) => setHeaderSearch(e.target.value)}
        />
        <div className="home-nav__actions">
          <Link className="home-nav__profile" to="/profile" aria-label="My profile" title={profile?.username || 'My profile'}>
            <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
              <path d="M12 12c2.76 0 5-2.24 5-5S14.76 2 12 2 7 4.24 7 7s2.24 5 5 5Zm0 2c-3.86 0-7 3.14-7 7 0 .55.45 1 1 1h12c.55 0 1-.45 1-1 0-3.86-3.14-7-7-7Z" />
            </svg>
          </Link>
          <button type="button" onClick={logout}>Logout</button>
        </div>
      </header>

      <div className="home-content profile-page">
        <section className="profile-top">
          <div className="profile-main">
            <label className="avatar-upload">
              <img
                src={avatarPreviewUrl || resolveMediaUrl(profile.avatarUrl, '/user-stub.png')}
                alt={profile.username}
                onError={(event) => {
                  event.currentTarget.src = '/user-stub.png'
                }}
              />
              {isOwnProfile && <input type="file" accept="image/*" hidden onChange={onAvatarChange} />}
              {isOwnProfile && <span>✎</span>}
            </label>
            <div>
              <h2>{profile.username}</h2>
              <textarea
                className="about"
                value={aboutMe}
                onChange={(e) => setAboutMe(e.target.value)}
                placeholder="Tell readers about yourself..."
                readOnly={!isOwnProfile}
              />
              {isOwnProfile && <button type="button" className="save-about" onClick={saveAbout}>Save About Me</button>}
              {isOwnProfile && aboutSaveState === 'saving' && <p className="save-hint">Saving...</p>}
              {isOwnProfile && aboutSaveState === 'saved' && <p className="save-hint save-hint--ok">Saved successfully.</p>}
              {isOwnProfile && avatarSaveState === 'saving' && <p className="save-hint">Uploading avatar...</p>}
              {isOwnProfile && avatarSaveState === 'saved' && <p className="save-hint save-hint--ok">Avatar updated.</p>}
              {isOwnProfile && avatarSaveState === 'failed' && (
                <p className="save-hint save-hint--error">Avatar upload failed. Local preview is shown.</p>
              )}
              {isOwnProfile && avatarSaveState === 'tooLarge' && (
                <p className="save-hint save-hint--error">File is too large. Max avatar size is 10MB.</p>
              )}
              <div className="stats">
                <div><strong>{profile.booksRead || 0}</strong><span>Books read</span></div>
                <div><strong>{profile.booksReviewed || 0}</strong><span>Reviews</span></div>
                <div><strong>{profile.booksWantToRead || 0}</strong><span>Want to read</span></div>
              </div>
            </div>
          </div>
          <aside className="taste-card">
            <h3>Taste Profile</h3>
            {topGenres.length > 0 ? (
              <div className="bars">
                {topGenres.map(([genre, count], idx) => (
                  <div key={genre} className="bar-item">
                    <div
                      className={`bar-fill bar-${idx}`}
                      style={{ height: `${Math.max(18, Math.round((count / maxGenre) * 110))}px` }}
                    />
                    <span>{genre}</span>
                  </div>
                ))}
              </div>
            ) : (
              <p className="save-hint">Add books to your shelves to build your chart.</p>
            )}
            <p className="fingerprint-title">Reading fingerprint</p>
            <div className="fingerprint">
              {topMoods.length > 0
                ? topMoods.map((mood) => <span key={mood}>{mood}</span>)
                : <span>No mood data yet</span>}
            </div>
          </aside>
        </section>

        <section className="recommendations-section">
          <div className="recommendations-header">
            <div>
              <p className="recommendations-kicker">Curated for your next chapter</p>
              <h3>Recommendations</h3>
            </div>
            <Link className="recommendations-link" to="/search">
              Explore all books
            </Link>
          </div>
          {recommendationsLoading && (
            <div className="recommendations-grid recommendations-grid--loading" aria-hidden="true">
              {Array.from({ length: 3 }).map((_, index) => (
                <div key={`rec-skeleton-${index}`} className="recommendation-card recommendation-card--skeleton">
                  <div className="recommendation-card__cover-skeleton" />
                  <div className="recommendation-card__line recommendation-card__line--title" />
                  <div className="recommendation-card__line recommendation-card__line--subtitle" />
                </div>
              ))}
            </div>
          )}
          {!recommendationsLoading && recommendations.length === 0 && (
            <div className="recommendations-empty">
              <p>This shelf awaits its first story.</p>
              <span>Read or review a few books to unlock tailored recommendations.</span>
            </div>
          )}
          {!recommendationsLoading && recommendations.length > 0 && (
            <div className="recommendations-grid">
              {recommendations.map((book, index) => (
                <MotionArticle
                  key={book.id}
                  className="recommendation-card"
                  initial={{ opacity: 0, y: 24 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true, amount: 0.25 }}
                  transition={{ duration: 0.45, delay: index * 0.06, ease: 'easeOut' }}
                  whileHover={{ y: -6, scale: 1.02 }}
                  whileTap={{ scale: 0.985 }}
                  onClick={() => navigate(`/books/${book.id}`)}
                >
                  <div className="recommendation-card__cover-wrap">
                    <img src={book.coverUrl || '/home-book.jpg'} alt={book.title} />
                  </div>
                  <div className="recommendation-card__body">
                    <p className="recommendation-card__reason">{book.reason}</p>
                    <h4>{book.title}</h4>
                    <p>{book.author || 'Unknown author'}</p>
                  </div>
                </MotionArticle>
              ))}
            </div>
          )}
        </section>

        <section className="shelf-section">
          <div className="shelf-header">
            <h3>Currently Reading</h3>
            {isOwnProfile && <button type="button" onClick={downloadPdf}>Export PDF</button>}
          </div>
          <div className="shelf-grid">
            {currentlyReadingBooks.map((book) => (
              <article key={book.id} className="shelf-book" onClick={() => navigate(`/books/${book.id}`)}>
                <img src={book.coverUrl || '/home-book.jpg'} alt={book.title} />
                <h4>{book.title}</h4>
                <p>{book.author}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="shelf-section shelf-section--tinted">
          <div className="shelf-header">
            <h3>Want to Read</h3>
          </div>
          <div className="shelf-grid">
            {wantToReadBooks.map((book) => (
              <article key={book.id} className="shelf-book" onClick={() => navigate(`/books/${book.id}`)}>
                <img src={book.coverUrl || '/home-book.jpg'} alt={book.title} />
                <h4>{book.title}</h4>
                <p>{book.author}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="shelf-section shelf-section--tinted">
          <div className="shelf-header">
            <h3>Read</h3>
          </div>
          <div className="shelf-grid">
            {readBooks.map((book) => (
              <article key={book.id} className="shelf-book" onClick={() => navigate(`/books/${book.id}`)}>
                <img src={book.coverUrl || '/home-book.jpg'} alt={book.title} />
                <h4>{book.title}</h4>
                <p>{book.author}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="shelf-section reviews-section">
          <div className="shelf-header">
            <h3 className="reviews-title">{isOwnProfile ? 'My Reviews' : `${profile.username}'s Reviews`}</h3>
          </div>
          {myReviews.length === 0 && (
            <p className="reviews-empty">
              {isOwnProfile ? 'You have not written any reviews yet.' : 'No public reviews yet.'}
            </p>
          )}
          <div className="reviews-grid">
            {myReviews.map((entry) => (
              <article
                key={entry.review.id}
                className="review-entry"
                onClick={() => navigate(`/books/${entry.bookId}#review-${entry.review.id}`)}
              >
                <div className="review-entry__top">
                  <h4>{entry.bookTitle}</h4>
                  <span className="review-entry__rating">{renderReviewStars(entry.review.rating)}</span>
                </div>
                <p className="review-entry__author">{entry.bookAuthor}</p>
                <p className="review-entry__meta">{formatReviewDate(entry.review.createdAt)} · Helpful: {entry.review.helpfulCount || 0}</p>
                {entry.review.whoIsItFor && (
                  <p className="review-entry__for"><strong>Who this book is for:</strong> {entry.review.whoIsItFor}</p>
                )}
                {Array.isArray(entry.review.mood) && entry.review.mood.length > 0 && (
                  <div className="review-entry__moods">
                    {entry.review.mood.slice(0, 4).map((mood) => <span key={mood}>{mood}</span>)}
                  </div>
                )}
                <p className="review-entry__verdict">{entry.review.verdict || entry.review.detailedReview || 'No verdict provided.'}</p>
              </article>
            ))}
          </div>
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

export default UserProfilePage
