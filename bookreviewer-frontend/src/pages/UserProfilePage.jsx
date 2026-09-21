import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { motion, useReducedMotion } from 'framer-motion'
import AppChrome from '../components/layout/AppChrome.jsx'
import { useAuth } from '../hooks/useAuth.js'
import { getBookDetail, deleteReview } from '../services/bookService.js'
import { resolveMediaUrl } from '../utils/media.js'
import {
  exportReadingListPdf,
  getMyProfile,
  getMyReviews,
  getMyTasteProfile,
  getRecommendations,
  getTasteProfileByUserId,
  getUserProfileById,
  getUserLibrary,
  getUserLibraryByUserId,
  getUserReviewsByUserId,
  updateProfile,
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
  reviews: 'Reviews',
  followers: 'Followers',
  following: 'Following',
}

const formatJoinedDate = (value) => {
  if (!value) return null
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return null
  return date.toLocaleDateString(undefined, { month: 'long', year: 'numeric' })
}

const socialLinkLabel = (url) => {
  try {
    const host = new URL(url.includes('://') ? url : `https://${url}`).hostname.replace(/^www\./, '')
    return host || url
  } catch {
    return url
  }
}

const parseSocialLinksInput = (value) =>
  String(value || '')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .slice(0, 5)

const UserProfilePage = () => {
  const MotionArticle = motion.article
  const MotionDiv = motion.div
  const reduceMotion = useReducedMotion()
  const navigate = useNavigate()
  const location = useLocation()
  const { id } = useParams()
  const { user, updateUser } = useAuth()
  const isOwnProfile = !id

  const [profile, setProfile] = useState(null)
  const [displayName, setDisplayName] = useState('')
  const [aboutMe, setAboutMe] = useState('')
  const [socialLinksText, setSocialLinksText] = useState('')
  const [profileSaveState, setProfileSaveState] = useState('idle')
  const [avatarPreviewUrl, setAvatarPreviewUrl] = useState('')
  const [avatarSaveState, setAvatarSaveState] = useState('idle')
  const [currentlyReadingBooks, setCurrentlyReadingBooks] = useState([])
  const [wantToReadBooks, setWantToReadBooks] = useState([])
  const [readBooks, setReadBooks] = useState([])
  const [abandonedBooks, setAbandonedBooks] = useState([])
  const [myReviews, setMyReviews] = useState([])
  const [tasteProfile, setTasteProfile] = useState(null)
  const [recommendations, setRecommendations] = useState([])
  const [recommendationsLoading, setRecommendationsLoading] = useState(true)
  const [followStats, setFollowStats] = useState({ followers: 0, following: 0 })
  const [isFollowing, setIsFollowing] = useState(false)
  const [followBusy, setFollowBusy] = useState(false)
  const [followError, setFollowError] = useState('')
  const [statsModal, setStatsModal] = useState(null)
  const [modalPeople, setModalPeople] = useState([])
  const [modalPeopleLoading, setModalPeopleLoading] = useState(false)
  const [deletingReviewId, setDeletingReviewId] = useState(null)
  const [hoveredGenre, setHoveredGenre] = useState(null)
  const [aboutPanelHeight, setAboutPanelHeight] = useState(null)
  const aboutPanelRef = useRef(null)

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
      setDisplayName(currentProfile.displayName || '')
      setSocialLinksText(Array.isArray(currentProfile.socialLinks) ? currentProfile.socialLinks.join('\n') : '')
      setProfileSaveState('idle')

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

      const [readingStatuses, wantStatuses, readStatuses, abandonedStatuses] = isOwnProfile
        ? await Promise.all([
          getUserLibrary('READING'),
          getUserLibrary('WANT_TO_READ'),
          getUserLibrary('READ'),
          getUserLibrary('ABANDONED'),
        ])
        : await Promise.all([
          getUserLibraryByUserId(currentProfile.id, 'READING'),
          getUserLibraryByUserId(currentProfile.id, 'WANT_TO_READ'),
          getUserLibraryByUserId(currentProfile.id, 'READ'),
          getUserLibraryByUserId(currentProfile.id, 'ABANDONED'),
        ])

      const readingBooks = await Promise.all(readingStatuses.map((item) => getBookDetail(item.bookId)))
      const wantBooks = await Promise.all(wantStatuses.map((item) => getBookDetail(item.bookId)))
      const doneBooks = await Promise.all(readStatuses.map((item) => getBookDetail(item.bookId)))
      const droppedBooks = await Promise.all(abandonedStatuses.map((item) => getBookDetail(item.bookId)))
      setCurrentlyReadingBooks(readingBooks)
      setWantToReadBooks(wantBooks)
      setReadBooks(doneBooks)
      setAbandonedBooks(droppedBooks)

      try {
        const taste = isOwnProfile
          ? await getMyTasteProfile()
          : await getTasteProfileByUserId(currentProfile.id)
        setTasteProfile(taste || null)
      } catch {
        setTasteProfile(null)
      }

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

      const isPersonalProfile = isOwnProfile || String(currentProfile.id) === String(user?.userId)

      if (!isPersonalProfile) {
        setRecommendations([])
        setRecommendationsLoading(false)
      } else {
        setRecommendationsLoading(true)
        try {
          const recommended = await getRecommendations(6)
          setRecommendations(Array.isArray(recommended) ? recommended : [])
        } catch {
          setRecommendations([])
        } finally {
          setRecommendationsLoading(false)
        }
      }
    }
    load()
  }, [id, isOwnProfile, user?.userId])

  const viewingOwnAccount = isOwnProfile || String(profile?.id) === String(user?.userId)

  useEffect(() => {
    if (!viewingOwnAccount || location.hash !== '#recommendations' || recommendationsLoading) return
    document.getElementById('recommendations')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }, [location.hash, recommendationsLoading, viewingOwnAccount])

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

  const handleDeleteReview = async (entry) => {
    const reviewId = entry?.review?.id
    if (!isOwnProfile || !reviewId || deletingReviewId != null) return
    const confirmed = window.confirm('Delete this review? This cannot be undone.')
    if (!confirmed) return

    setDeletingReviewId(reviewId)
    try {
      await deleteReview(reviewId)
      setMyReviews((prev) => prev.filter((item) => Number(item.review?.id) !== Number(reviewId)))
    } finally {
      setDeletingReviewId(null)
    }
  }

  const topGenres = Array.isArray(tasteProfile?.topGenres) ? tasteProfile.topGenres : []
  const topMoods = Array.isArray(tasteProfile?.topMoods) ? tasteProfile.topMoods : []
  const genreChartLabel = topGenres.length > 0
    ? `Top genres: ${topGenres.map((g) => `${g.name} ${g.sharePercent}%`).join(', ')}`
    : 'No genre taste data yet'
  const genreShareTotal = topGenres.reduce((sum, g) => sum + (Number(g.sharePercent) || 0), 0) || 1

  useEffect(() => {
    const panel = aboutPanelRef.current
    if (!panel || typeof ResizeObserver === 'undefined') return undefined

    const syncHeight = () => {
      const nextHeight = Math.round(panel.getBoundingClientRect().height)
      setAboutPanelHeight((prev) => (prev === nextHeight ? prev : nextHeight))
    }

    syncHeight()
    const observer = new ResizeObserver(syncHeight)
    observer.observe(panel)
    return () => observer.disconnect()
  }, [profile, isOwnProfile, displayName, aboutMe, socialLinksText, profileSaveState])

  const saveProfile = async () => {
    setProfileSaveState('saving')
    try {
      const socialLinks = parseSocialLinksInput(socialLinksText)
      await updateProfile({
        displayName,
        aboutMe,
        socialLinks,
      })
      setProfile((prev) => ({
        ...prev,
        displayName: displayName.trim() || null,
        aboutMe: aboutMe.trim() || null,
        socialLinks,
      }))
      setSocialLinksText(socialLinks.join('\n'))
      setProfileSaveState('saved')
      setTimeout(() => setProfileSaveState('idle'), 1800)
    } catch {
      setProfileSaveState('failed')
    }
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

  const joinedLabel = formatJoinedDate(profile.joinedAt)
  const visibleName = (isOwnProfile ? displayName : profile.displayName)?.trim() || profile.username
  const visibleSocialLinks = isOwnProfile
    ? parseSocialLinksInput(socialLinksText)
    : (Array.isArray(profile.socialLinks) ? profile.socialLinks : [])
  const visibleAbout = isOwnProfile ? aboutMe : (profile.aboutMe || '')

  return (
    <AppChrome>
      <div className="home-content profile-page">
        <section className="profile-top">
          <div className="profile-main">
            <label className={`avatar-upload${isOwnProfile ? '' : ' avatar-upload--readonly'}`}>
              <img
                src={avatarPreviewUrl || resolveMediaUrl(profile.avatarUrl, '/user-stub.png')}
                alt={visibleName}
                onError={(event) => {
                  event.currentTarget.src = '/user-stub.png'
                }}
              />
              {isOwnProfile && <input type="file" accept="image/*" hidden onChange={onAvatarChange} />}
              {isOwnProfile && <span>✎</span>}
            </label>

            <div className="profile-identity">
              <header className="profile-identity__header">
                <div className="profile-identity__titles">
                  <h2>{visibleName}</h2>
                  <p className="profile-handle">@{profile.username}</p>
                  {joinedLabel && (
                    <p className="profile-joined">Joined {joinedLabel}</p>
                  )}
                </div>
              </header>

              <div className="profile-body">
              <div className="profile-side">
              <div className="profile-panel" ref={aboutPanelRef}>
                <section className="profile-panel__block">
                  <div className="profile-panel__heading">
                    <h3>About</h3>
                  </div>
                  {isOwnProfile ? (
                    <>
                      <label className="profile-field">
                        <span className="profile-field__label">Display name</span>
                        <input
                          className="profile-input"
                          type="text"
                          maxLength={120}
                          value={displayName}
                          onChange={(e) => setDisplayName(e.target.value)}
                          placeholder="Your name"
                          autoComplete="name"
                        />
                      </label>
                      <label className="profile-field">
                        <span className="profile-field__label">Bio</span>
                        <textarea
                          className="about"
                          value={aboutMe}
                          onChange={(e) => setAboutMe(e.target.value)}
                          placeholder="Tell readers about yourself…"
                          maxLength={1500}
                        />
                      </label>
                    </>
                  ) : visibleAbout ? (
                    <p className="profile-about-text">{visibleAbout}</p>
                  ) : (
                    <p className="profile-empty">No bio yet.</p>
                  )}
                </section>

                <section className="profile-panel__block">
                  <div className="profile-panel__heading">
                    <h3>Elsewhere</h3>
                    {isOwnProfile && <span className="profile-panel__hint">Up to 5 links</span>}
                  </div>
                  {isOwnProfile ? (
                    <>
                      <label className="profile-field">
                        <span className="profile-field__label">Social links</span>
                        <textarea
                          className="about about--links"
                          value={socialLinksText}
                          onChange={(e) => setSocialLinksText(e.target.value)}
                          placeholder={'instagram.com/you\ngoodreads.com/you'}
                          rows={4}
                        />
                      </label>
                      {visibleSocialLinks.length > 0 && (
                        <ul className="profile-socials" aria-label="Link preview">
                          {visibleSocialLinks.map((link) => (
                            <li key={link}>
                              <a href={link.includes('://') ? link : `https://${link}`} target="_blank" rel="noreferrer noopener">
                                {socialLinkLabel(link)}
                              </a>
                            </li>
                          ))}
                        </ul>
                      )}
                    </>
                  ) : visibleSocialLinks.length > 0 ? (
                    <ul className="profile-socials">
                      {visibleSocialLinks.map((link) => (
                        <li key={link}>
                          <a href={link.includes('://') ? link : `https://${link}`} target="_blank" rel="noreferrer noopener">
                            {socialLinkLabel(link)}
                          </a>
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <p className="profile-empty">No links yet.</p>
                  )}
                </section>

                {isOwnProfile && (
                  <div className="profile-panel__actions">
                    <button type="button" className="save-about" onClick={saveProfile}>
                      Save profile
                    </button>
                    {profileSaveState === 'saving' && <p className="save-hint">Saving…</p>}
                    {profileSaveState === 'saved' && <p className="save-hint save-hint--ok">Saved successfully.</p>}
                    {profileSaveState === 'failed' && (
                      <p className="save-hint save-hint--error">Could not save. Check links use http/https.</p>
                    )}
                  </div>
                )}
              </div>

              {isOwnProfile && avatarSaveState === 'saving' && <p className="save-hint">Uploading avatar...</p>}
              {isOwnProfile && avatarSaveState === 'saved' && <p className="save-hint save-hint--ok">Avatar updated.</p>}
              {isOwnProfile && avatarSaveState === 'failed' && (
                <p className="save-hint save-hint--error">Avatar upload failed. Local preview is shown.</p>
              )}
              {isOwnProfile && avatarSaveState === 'tooLarge' && (
                <p className="save-hint save-hint--error">File is too large. Max avatar size is 10MB.</p>
              )}

              <div className="stats-row">
                <div className="stats">
                  <button type="button" className="stats__item" onClick={() => openStatsModal('reviews')}>
                    <strong>{profile.booksReviewed || 0}</strong>
                    <span>Reviews</span>
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
              </div>
              </div>

          <aside
            className={`taste-card${aboutPanelHeight ? ' taste-card--matched' : ''}`}
            style={aboutPanelHeight ? { minHeight: `${aboutPanelHeight}px` } : undefined}
          >
            <h3>Top genres</h3>
            {topGenres.length > 0 ? (
              <>
                <div
                  className="genre-spectrum"
                  role="img"
                  aria-label={genreChartLabel}
                  onMouseLeave={() => setHoveredGenre(null)}
                >
                  <div className="genre-spectrum__ribbon" aria-hidden="true">
                    {topGenres.map((genre, idx) => {
                      const share = Number(genre.sharePercent) || 0
                      const flexGrow = Math.max(share, 6)
                      const isActive = hoveredGenre === genre.name
                      const isDimmed = hoveredGenre && !isActive
                      return (
                        <div
                          key={genre.name}
                          className={`genre-spectrum__slot${isActive ? ' is-active' : ''}${isDimmed ? ' is-dimmed' : ''}`}
                          style={{ flexGrow }}
                          onMouseEnter={() => setHoveredGenre(genre.name)}
                        >
                          <MotionDiv
                            className={`genre-spectrum__segment genre-tone-${idx}`}
                            initial={reduceMotion ? false : { scaleX: 0 }}
                            animate={{ scaleX: 1 }}
                            transition={reduceMotion
                              ? { duration: 0 }
                              : { duration: 0.45, delay: idx * 0.08, ease: [0.22, 1, 0.36, 1] }}
                          />
                          <span className="genre-spectrum__hint">
                            <strong>{genre.name}</strong>
                            <em>{share}% of taste</em>
                          </span>
                        </div>
                      )
                    })}
                  </div>

                  <ul className="genre-rank">
                    {topGenres.map((genre, idx) => {
                      const share = Number(genre.sharePercent) || 0
                      const isActive = hoveredGenre === genre.name
                      return (
                        <li key={genre.name}>
                          <button
                            type="button"
                            className={`genre-rank__row${isActive ? ' is-active' : ''}`}
                            onMouseEnter={() => setHoveredGenre(genre.name)}
                            onFocus={() => setHoveredGenre(genre.name)}
                            onBlur={() => setHoveredGenre(null)}
                            aria-label={`${genre.name}, ${share} percent`}
                          >
                            <span className={`genre-rank__swatch genre-tone-${idx}`} aria-hidden="true" />
                            <span className="genre-rank__meta">
                              <span className="genre-rank__name">{genre.name}</span>
                              <span className="genre-rank__track" aria-hidden="true">
                                <span
                                  className={`genre-rank__fill genre-tone-${idx}`}
                                  style={{ width: `${Math.max(8, (share / genreShareTotal) * 100)}%` }}
                                />
                              </span>
                            </span>
                            <span className="genre-rank__pct">{share}%</span>
                          </button>
                        </li>
                      )
                    })}
                  </ul>
                </div>
                <p className="taste-caption">
                  Based on {tasteProfile?.sampleSize || 0} books on {viewingOwnAccount ? 'your' : 'their'} shelves.
                </p>
              </>
            ) : (
              <p className="save-hint">Add books to your shelves to build your chart.</p>
            )}
            <div className="taste-card__fingerprint">
              <p className="fingerprint-title">Reading fingerprint</p>
              <div className="fingerprint">
                <div className="fingerprint__row">
                  {topMoods.length > 0
                    ? topMoods.map((mood) => (
                      <span key={mood.name}>{mood.name} · {mood.count}</span>
                    ))
                    : <span>No mood data yet</span>}
                </div>
                {(tasteProfile?.dominantPacing || tasteProfile?.averageRating != null) && (
                  <div className="fingerprint__row fingerprint__row--meta">
                    {tasteProfile?.dominantPacing && (
                      <span>Pacing · {tasteProfile.dominantPacing}</span>
                    )}
                    {tasteProfile?.averageRating != null && (
                      <span>Avg rating · {tasteProfile.averageRating}</span>
                    )}
                  </div>
                )}
              </div>
            </div>
          </aside>
              </div>
            </div>
          </div>
        </section>

        {viewingOwnAccount && (
          <section id="recommendations" className="recommendations-section">
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

        <section className="shelf-section shelf-section--tinted">
          <div className="shelf-header">
            <h3>Abandoned</h3>
          </div>
          <div className="shelf-grid">
            {abandonedBooks.map((book) => (
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

                    {isOwnProfile && (
                      <div className="review-entry__actions">
                        <button
                          type="button"
                          className="review-entry__action"
                          onClick={(event) => {
                            event.stopPropagation()
                            navigate(`/books/${entry.bookId}/review/${entry.review.id}/edit`)
                          }}
                        >
                          Edit
                        </button>
                        <button
                          type="button"
                          className="review-entry__action review-entry__action--danger"
                          disabled={deletingReviewId === entry.review.id}
                          onClick={(event) => {
                            event.stopPropagation()
                            handleDeleteReview(entry)
                          }}
                        >
                          {deletingReviewId === entry.review.id ? 'Deleting…' : 'Delete'}
                        </button>
                      </div>
                    )}
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
