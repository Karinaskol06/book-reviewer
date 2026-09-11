import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { motion } from 'framer-motion'
import AppChrome from '../components/layout/AppChrome.jsx'
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
import {
  followUser,
  getFollowers,
  getFollowing,
  getFollowStats,
  isFollowingUser,
  unfollowUser,
} from '../services/userService.js'
import './UserProfilePage.css'

const STAT_MODAL_TITLES = {
  read: 'Books read',
  reviews: 'Reviews',
  want: 'Want to read',
  followers: 'Followers',
  following: 'Following',
}

const UserProfilePage = () => {
  const MotionArticle = motion.article
  const navigate = useNavigate()
  const { id } = useParams()
  const { user, updateUser } = useAuth()
  const isOwnProfile = !id

  const [profile, setProfile] = useState(null)
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
  const [followStats, setFollowStats] = useState({ followers: 0, following: 0 })
  const [isFollowing, setIsFollowing] = useState(false)
  const [followBusy, setFollowBusy] = useState(false)
  const [followError, setFollowError] = useState('')
  const [statsModal, setStatsModal] = useState(null)
  const [modalPeople, setModalPeople] = useState([])
  const [modalPeopleLoading, setModalPeopleLoading] = useState(false)

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
      if (isOwnProfile && currentProfile?.avatarUrl) {
        updateUser?.({ avatarUrl: currentProfile.avatarUrl })
      }
      setAboutMe(currentProfile.aboutMe || '')

      try {
        const stats = await getFollowStats(currentProfile.id)
        setFollowStats({
          followers: Number(stats?.followers) || 0,
          following: Number(stats?.following) || 0,
        })
      } catch {
        setFollowStats({ followers: 0, following: 0 })
      }

      const viewingOwnAccount = isOwnProfile || String(currentProfile.id) === String(user?.userId)
      if (!viewingOwnAccount) {
        try {
          setIsFollowing(await isFollowingUser(currentProfile.id))
        } catch {
          setIsFollowing(false)
        }
      } else {
        setIsFollowing(false)
      }
      setFollowError('')

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
          bookCover: relatedBook?.coverUrl || '',
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
      const isPersonalProfile = isOwnProfile || String(currentProfile.id) === String(user?.userId)

      if (!isPersonalProfile) {
        setRecommendations([])
        setRecommendationsLoading(false)
      } else {
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
    }
    load()
  }, [id, isOwnProfile, user?.userId])

  const viewingOwnAccount = isOwnProfile || String(profile?.id) === String(user?.userId)

  const handleToggleFollow = async () => {
    if (!profile?.id || followBusy || viewingOwnAccount) return
    setFollowBusy(true)
    setFollowError('')
    try {
      if (isFollowing) {
        await unfollowUser(profile.id)
        setIsFollowing(false)
        setFollowStats((prev) => ({
          ...prev,
          followers: Math.max(0, (prev.followers || 0) - 1),
        }))
      } else {
        await followUser(profile.id)
        setIsFollowing(true)
        setFollowStats((prev) => ({
          ...prev,
          followers: (prev.followers || 0) + 1,
        }))
      }
    } catch {
      setFollowError(isFollowing ? 'Could not unfollow. Try again.' : 'Could not follow. Try again.')
    } finally {
      setFollowBusy(false)
    }
  }

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
        updateUser?.({ avatarUrl: uploaded.avatarUrl || cacheBusted })
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

  const closeStatsModal = () => {
    setStatsModal(null)
    setModalPeople([])
    setModalPeopleLoading(false)
  }

  const openStatsModal = async (type) => {
    setStatsModal(type)
    if (type !== 'followers' && type !== 'following') {
      setModalPeople([])
      return
    }
    if (!profile?.id) return
    setModalPeopleLoading(true)
    try {
      const people = type === 'followers'
        ? await getFollowers(profile.id)
        : await getFollowing(profile.id)
      setModalPeople(Array.isArray(people) ? people : [])
    } catch {
      setModalPeople([])
    } finally {
      setModalPeopleLoading(false)
    }
  }

  useEffect(() => {
    if (!statsModal) return undefined
    const onKeyDown = (event) => {
      if (event.key === 'Escape') closeStatsModal()
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [statsModal])

  if (!profile) {
    return (
      <AppChrome>
        <p className="detail-loading">Loading profile...</p>
      </AppChrome>
    )
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
    <AppChrome>
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
              {!viewingOwnAccount && (
                <div className="profile-follow-row">
                  <button
                    type="button"
                    className={`profile-follow-btn${isFollowing ? ' profile-follow-btn--following' : ''}`}
                    onClick={handleToggleFollow}
                    disabled={followBusy}
                    aria-pressed={isFollowing}
                  >
                    {followBusy ? 'Please wait…' : isFollowing ? 'Unfollow' : 'Follow'}
                  </button>
                  {followError && <p className="save-hint save-hint--error">{followError}</p>}
                </div>
              )}
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
                <button type="button" className="stats__item" onClick={() => openStatsModal('read')}>
                  <strong>{profile.booksRead || 0}</strong>
                  <span>Books read</span>
                </button>
                <button type="button" className="stats__item" onClick={() => openStatsModal('reviews')}>
                  <strong>{profile.booksReviewed || 0}</strong>
                  <span>Reviews</span>
                </button>
                <button type="button" className="stats__item" onClick={() => openStatsModal('want')}>
                  <strong>{profile.booksWantToRead || 0}</strong>
                  <span>Want to read</span>
                </button>
                <button type="button" className="stats__item" onClick={() => openStatsModal('followers')}>
                  <strong>{followStats.followers}</strong>
                  <span>Followers</span>
                </button>
                <button type="button" className="stats__item" onClick={() => openStatsModal('following')}>
                  <strong>{followStats.following}</strong>
                  <span>Following</span>
                </button>
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

        {viewingOwnAccount && (
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
                      <img src={resolveMediaUrl(book.coverUrl, '/home-book.jpg')} alt={book.title} />
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
        )}

        <section className="shelf-section">
          <div className="shelf-header">
            <h3>Currently Reading</h3>
            {isOwnProfile && <button type="button" onClick={downloadPdf}>Export PDF</button>}
          </div>
          <div className="shelf-grid">
            {currentlyReadingBooks.map((book) => (
              <article key={book.id} className="shelf-book" onClick={() => navigate(`/books/${book.id}`)}>
                <img src={resolveMediaUrl(book.coverUrl, '/home-book.jpg')} alt={book.title} />
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
                <img src={resolveMediaUrl(book.coverUrl, '/home-book.jpg')} alt={book.title} />
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
                <img src={resolveMediaUrl(book.coverUrl, '/home-book.jpg')} alt={book.title} />
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
            {myReviews.map((entry) => {
              const verdictText = entry.review.verdict || entry.review.detailedReview || ''
              return (
                <article
                  key={entry.review.id}
                  className="review-entry"
                  onClick={() => navigate(`/books/${entry.bookId}#review-${entry.review.id}`)}
                >
                  <div className="review-entry__cover">
                    <img
                      src={resolveMediaUrl(entry.bookCover, '/home-book.jpg')}
                      alt=""
                    />
                  </div>
                  <div className="review-entry__body">
                    <div className="review-entry__top">
                      <div className="review-entry__titles">
                        <h4>{entry.bookTitle}</h4>
                        {entry.bookAuthor && <p className="review-entry__author">{entry.bookAuthor}</p>}
                      </div>
                      <span className="review-entry__rating" aria-label={`${entry.review.rating || 0} stars`}>
                        {renderReviewStars(entry.review.rating)}
                      </span>
                    </div>

                    {verdictText && (
                      <blockquote className="review-entry__verdict">
                        “{verdictText}”
                      </blockquote>
                    )}

                    {entry.review.whoIsItFor && (
                      <div className="review-entry__for">
                        <p className="review-entry__for-label">Who this is for</p>
                        <p className="review-entry__for-text">{entry.review.whoIsItFor}</p>
                      </div>
                    )}

                    {Array.isArray(entry.review.mood) && entry.review.mood.length > 0 && (
                      <div className="review-entry__moods">
                        {entry.review.mood.slice(0, 4).map((mood) => (
                          <span key={mood}>{mood}</span>
                        ))}
                      </div>
                    )}

                    <p className="review-entry__meta">
                      {formatReviewDate(entry.review.createdAt)}
                      <span aria-hidden="true"> · </span>
                      Helpful {entry.review.helpfulCount || 0}
                    </p>
                  </div>
                </article>
              )
            })}
          </div>
        </section>
      </div>

      {statsModal && (
        <div
          className="profile-stats-modal"
          role="dialog"
          aria-modal="true"
          aria-labelledby="profile-stats-modal-title"
          onClick={closeStatsModal}
        >
          <div
            className="profile-stats-modal__panel"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="profile-stats-modal__header">
              <h3 id="profile-stats-modal-title">{STAT_MODAL_TITLES[statsModal]}</h3>
              <button type="button" className="profile-stats-modal__close" onClick={closeStatsModal} aria-label="Close">
                ×
              </button>
            </div>

            {statsModal === 'read' && (
              <div className="profile-stats-modal__list">
                {readBooks.length === 0 && <p className="profile-stats-modal__empty">No books marked as read yet.</p>}
                {readBooks.map((book) => (
                  <button
                    key={book.id}
                    type="button"
                    className="profile-stats-modal__book"
                    onClick={() => {
                      closeStatsModal()
                      navigate(`/books/${book.id}`)
                    }}
                  >
                    <img src={resolveMediaUrl(book.coverUrl, '/home-book.jpg')} alt="" />
                    <span>
                      <strong>{book.title}</strong>
                      <em>{book.author || 'Unknown author'}</em>
                    </span>
                  </button>
                ))}
              </div>
            )}

            {statsModal === 'want' && (
              <div className="profile-stats-modal__list">
                {wantToReadBooks.length === 0 && <p className="profile-stats-modal__empty">Want-to-read shelf is empty.</p>}
                {wantToReadBooks.map((book) => (
                  <button
                    key={book.id}
                    type="button"
                    className="profile-stats-modal__book"
                    onClick={() => {
                      closeStatsModal()
                      navigate(`/books/${book.id}`)
                    }}
                  >
                    <img src={resolveMediaUrl(book.coverUrl, '/home-book.jpg')} alt="" />
                    <span>
                      <strong>{book.title}</strong>
                      <em>{book.author || 'Unknown author'}</em>
                    </span>
                  </button>
                ))}
              </div>
            )}

            {statsModal === 'reviews' && (
              <div className="profile-stats-modal__list">
                {myReviews.length === 0 && <p className="profile-stats-modal__empty">No reviews yet.</p>}
                {myReviews.map((entry) => (
                  <button
                    key={entry.review.id}
                    type="button"
                    className="profile-stats-modal__book"
                    onClick={() => {
                      closeStatsModal()
                      navigate(`/books/${entry.bookId}#review-${entry.review.id}`)
                    }}
                  >
                    <span>
                      <strong>{entry.bookTitle}</strong>
                      <em>{entry.bookAuthor || entry.review.verdict || 'Open review'}</em>
                    </span>
                  </button>
                ))}
              </div>
            )}

            {(statsModal === 'followers' || statsModal === 'following') && (
              <div className="profile-stats-modal__list">
                {modalPeopleLoading && <p className="profile-stats-modal__empty">Loading…</p>}
                {!modalPeopleLoading && modalPeople.length === 0 && (
                  <p className="profile-stats-modal__empty">
                    {statsModal === 'followers' ? 'No followers yet.' : 'Not following anyone yet.'}
                  </p>
                )}
                {!modalPeopleLoading && modalPeople.map((person) => (
                  <button
                    key={person.id}
                    type="button"
                    className="profile-stats-modal__person"
                    onClick={() => {
                      closeStatsModal()
                      navigate(String(person.id) === String(user?.userId) ? '/profile' : `/users/${person.id}`)
                    }}
                  >
                    <img src={resolveMediaUrl(person.avatarUrl, '/user-stub.png')} alt="" />
                    <strong>{person.username}</strong>
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </AppChrome>
  )
}

export default UserProfilePage
