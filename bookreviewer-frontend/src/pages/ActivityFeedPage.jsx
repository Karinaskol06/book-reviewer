import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useInView } from 'react-intersection-observer'
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion'
import AppChrome from '../components/layout/AppChrome.jsx'
import { useAuth } from '../hooks/useAuth.js'
import { getFeedPage } from '../services/feedService.js'
import { followUser, searchUsersByUsername, unfollowUser } from '../services/userService.js'
import { resolveMediaUrl } from '../utils/media.js'
import './ActivityFeedPage.css'

const getRelativeTime = (value) => {
  if (!value) return 'Recently'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Recently'

  const diffMs = Date.now() - date.getTime()
  const diffMinutes = Math.max(1, Math.round(diffMs / (1000 * 60)))
  if (diffMinutes < 60) return `${diffMinutes} min ago`
  const diffHours = Math.round(diffMinutes / 60)
  if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`
  const diffDays = Math.round(diffHours / 24)
  if (diffDays < 8) return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`
  return date.toLocaleDateString()
}

const normalizeFeedResponse = (payload) => {
  if (Array.isArray(payload)) {
    return { content: payload, hasNext: payload.length > 0 }
  }

  const content = payload?.content || payload?.items || []
  const hasNext =
    payload?.hasNext ??
    payload?.nextPage ??
    (typeof payload?.last === 'boolean' ? !payload.last : undefined) ??
    (typeof payload?.totalPages === 'number' && typeof payload?.number === 'number'
      ? payload.number + 1 < payload.totalPages
      : content.length > 0)

  return { content, hasNext: Boolean(hasNext) }
}

const getActivityType = (activity) => {
  const rawType = (activity?.type || activity?.activityType || activity?.eventType || activity?.kind || '').toString()
  const upperType = rawType.toUpperCase()
  if (upperType === 'REVIEWED') return 'review'
  if (upperType === 'WANT_TO_READ' || upperType === 'STARTED_READING') return 'reading_intent'
  if (upperType === 'FINISHED_READING' || upperType === 'ABANDONED') return 'status_change'
  if (upperType === 'FOLLOWED_USER') return 'followed_user'
  if (upperType === 'BOOK_ADDED_TO_CATALOG') return 'book_added'
  return 'status_change'
}

const mapActivity = (activity, index) => {
  const type = getActivityType(activity)
  const actor = activity?.user || activity?.actor || {}
  const book = activity?.book || activity?.review?.book || {}
  const reviewSnippet = activity?.reviewSnippet || activity?.review || {}
  const targetUser = activity?.targetUser || {}
  const fallbackDescription = activity?.bookDescription || ''
  const shortDescription = (book?.description || fallbackDescription || '').slice(0, 170)

  return {
    id: activity?.id || activity?.activityId || `${type}-${activity?.createdAt || index}`,
    type,
    rawType: (activity?.type || '').toString(),
    actorName: actor?.username || actor?.name || 'A reader',
    actorId: actor?.id,
    actorAvatar: resolveMediaUrl(actor?.avatarUrl, '/user-stub.png'),
    createdAtText: getRelativeTime(activity?.createdAt || activity?.timestamp),
    bookId: book?.id || activity?.bookId,
    bookTitle: book?.title || activity?.bookTitle || 'Untitled Book',
    bookAuthor: book?.author || activity?.bookAuthor || '',
    bookCover: resolveMediaUrl(book?.coverUrl || activity?.bookCoverUrl, '/home-book.jpg'),
    shortDescription: shortDescription ? `${shortDescription}${shortDescription.length >= 170 ? '...' : ''}` : '',
    status: activity?.statusLabel || activity?.status || activity?.bookStatus || activity?.payload?.status || '',
    reviewId: reviewSnippet?.id || activity?.reviewId,
    verdict: reviewSnippet?.verdict || '',
    detailedReview:
      reviewSnippet?.detailedReview || activity?.reviewText || activity?.excerpt || '',
    whoIsItFor: reviewSnippet?.whoIsItFor || '',
    moods: Array.isArray(reviewSnippet?.mood) ? reviewSnippet.mood : [],
    targetUserId: targetUser?.id || activity?.targetUserId,
    targetUserName: targetUser?.username || 'Reader',
  }
}

const activityDedupeKey = (activity) => {
  if (activity.reviewId) return `review:${activity.reviewId}`
  if (activity.type === 'followed_user') {
    return `follow:${activity.actorId}:${activity.targetUserId}:${activity.id}`
  }
  if (activity.id != null && String(activity.id).length > 0) return `id:${activity.id}`
  return `${activity.type}:${activity.actorId}:${activity.bookId}:${activity.createdAtText}`
}

const dedupeActivities = (list) => {
  const seen = new Set()
  return list.filter((item) => {
    const key = activityDedupeKey(item)
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}

const activityVerb = (activity) => {
  if (activity.type === 'review') return 'wrote a review'
  if (activity.type === 'status_change') return 'finished reading'
  if (activity.type === 'reading_intent') return `added to ${activity.status || 'reading list'}`
  if (activity.type === 'followed_user') return `followed ${activity.targetUserName}`
  if (activity.type === 'book_added') return 'added a book to the catalog'
  return 'shared an update'
}

const FeedCard = ({ activity, onNavigate, reduceMotion }) => {
  const { ref: cardRef, inView: cardVisible } = useInView({ triggerOnce: true, threshold: 0.12 })
  const motionProps = reduceMotion
    ? {}
    : {
        initial: { opacity: 0, y: 14 },
        animate: cardVisible ? { opacity: 1, y: 0 } : { opacity: 0, y: 14 },
        transition: { duration: 0.35, ease: [0.22, 1, 0.36, 1] },
      }

  if (activity.type === 'followed_user') {
    return (
      <motion.article
        ref={cardRef}
        className="feed-item feed-item--follow"
        {...motionProps}
      >
        <button
          type="button"
          className="feed-item__avatar-btn"
          onClick={() => activity.actorId && onNavigate(`/users/${activity.actorId}`)}
          aria-label={`Open ${activity.actorName} profile`}
        >
          <img src={activity.actorAvatar} alt="" />
        </button>
        <p className="feed-item__follow-line">
          <button
            type="button"
            className="feed-item__name-link"
            onClick={() => activity.actorId && onNavigate(`/users/${activity.actorId}`)}
          >
            {activity.actorName}
          </button>
          {' '}started following{' '}
          <button
            type="button"
            className="feed-item__name-link"
            onClick={() => activity.targetUserId && onNavigate(`/users/${activity.targetUserId}`)}
          >
            {activity.targetUserName}
          </button>
          <span className="feed-item__time"> · {activity.createdAtText}</span>
        </p>
      </motion.article>
    )
  }

  if (activity.type === 'review') {
    return (
      <motion.article
        ref={cardRef}
        className="feed-item feed-item--review"
        {...motionProps}
      >
        <header className="feed-item__header">
          <button
            type="button"
            className="feed-item__avatar-btn"
            onClick={() => activity.actorId && onNavigate(`/users/${activity.actorId}`)}
            aria-label={`Open ${activity.actorName} profile`}
          >
            <img src={activity.actorAvatar} alt="" />
          </button>
          <div>
            <p className="feed-item__meta">
              <strong>{activity.actorName}</strong> {activityVerb(activity)}
            </p>
            <p className="feed-item__time">{activity.createdAtText}</p>
          </div>
        </header>

        <div className="feed-review-block">
          <button
            type="button"
            className="feed-review-block__cover"
            onClick={() => activity.bookId && onNavigate(`/books/${activity.bookId}`)}
            aria-label={`Open ${activity.bookTitle}`}
          >
            <img src={activity.bookCover} alt="" />
          </button>
          <div className="feed-review-block__copy">
            <h3>{activity.bookTitle}</h3>
            {activity.bookAuthor && <p className="feed-item__author">{activity.bookAuthor}</p>}
            {activity.detailedReview?.trim() ? (
              <p className="feed-detailed-excerpt">{activity.detailedReview.trim()}</p>
            ) : (
              activity.verdict && (
                <blockquote className="feed-pullquote">
                  <span aria-hidden="true">“</span>
                  {activity.verdict}
                  <span aria-hidden="true">”</span>
                </blockquote>
              )
            )}
            {activity.whoIsItFor && (
              <div className="feed-review-meta">
                <p className="feed-review-meta__label">Who this is for</p>
                <p>{activity.whoIsItFor}</p>
              </div>
            )}
            {activity.moods.length > 0 && (
              <div className="feed-moods" aria-label="Moods">
                {activity.moods.slice(0, 4).map((mood) => (
                  <span key={mood}>{mood}</span>
                ))}
              </div>
            )}
            {activity.reviewId && activity.bookId && (
              <button
                type="button"
                className="feed-text-link"
                onClick={() => onNavigate(`/books/${activity.bookId}#review-${activity.reviewId}`)}
              >
                Read full review
              </button>
            )}
          </div>
        </div>
      </motion.article>
    )
  }

  const primaryAction =
    activity.type === 'reading_intent'
      ? { label: 'Preview', path: activity.bookId ? `/books/${activity.bookId}` : null }
      : activity.type === 'book_added'
        ? { label: 'See book', path: activity.bookId ? `/books/${activity.bookId}` : null }
        : { label: 'Open book', path: activity.bookId ? `/books/${activity.bookId}` : null }

  return (
    <motion.article
      ref={cardRef}
      className={`feed-item feed-item--compact feed-item--${activity.type}`}
      {...motionProps}
    >
      <header className="feed-item__header">
        <button
          type="button"
          className="feed-item__avatar-btn"
          onClick={() => activity.actorId && onNavigate(`/users/${activity.actorId}`)}
          aria-label={`Open ${activity.actorName} profile`}
        >
          <img src={activity.actorAvatar} alt="" />
        </button>
        <div>
          <p className="feed-item__meta">
            <strong>{activity.actorName}</strong> {activityVerb(activity)}
          </p>
          <p className="feed-item__time">{activity.createdAtText}</p>
        </div>
      </header>

      <div className="feed-compact-body">
        <button
          type="button"
          className="feed-compact-body__cover"
          onClick={() => activity.bookId && onNavigate(`/books/${activity.bookId}`)}
          aria-label={`Open ${activity.bookTitle}`}
        >
          <img src={activity.bookCover} alt="" />
        </button>
        <div>
          <h3>{activity.bookTitle}</h3>
          {activity.type === 'status_change' && (
            <p className="feed-status-chip">{activity.status || 'Status updated'}</p>
          )}
          {activity.shortDescription && (
            <p className="feed-item__desc">{activity.shortDescription}</p>
          )}
          {primaryAction.path && (
            <button
              type="button"
              className="feed-text-link"
              onClick={() => onNavigate(primaryAction.path)}
            >
              {primaryAction.label}
            </button>
          )}
        </div>
      </div>
    </motion.article>
  )
}

const FeedSkeleton = () => (
  <div className="feed-columns feed-skeleton-wrap" aria-hidden="true">
    <div className="feed-columns__col">
      {Array.from({ length: 2 }).map((_, idx) => (
        <div key={`L-${idx}`} className="feed-skeleton-card">
          <div className="feed-skeleton-line feed-skeleton-line--title" />
          <div className="feed-skeleton-line" />
          <div className="feed-skeleton-line feed-skeleton-line--short" />
          <div className="feed-skeleton-block" />
        </div>
      ))}
    </div>
    <div className="feed-columns__col">
      {Array.from({ length: 2 }).map((_, idx) => (
        <div key={`R-${idx}`} className="feed-skeleton-card">
          <div className="feed-skeleton-line feed-skeleton-line--title" />
          <div className="feed-skeleton-line" />
          <div className="feed-skeleton-line feed-skeleton-line--short" />
          <div className="feed-skeleton-block" />
        </div>
      ))}
    </div>
  </div>
)

const ActivityFeedPage = () => {
  const navigate = useNavigate()
  const { user } = useAuth()
  const reduceMotion = useReducedMotion()
  const { ref, inView } = useInView({ rootMargin: '320px' })

  const [activities, setActivities] = useState([])
  const [findOpen, setFindOpen] = useState(false)
  const [userSearch, setUserSearch] = useState('')
  const [userResults, setUserResults] = useState([])
  const [searchingUsers, setSearchingUsers] = useState(false)
  const [page, setPage] = useState(0)
  const [hasMore, setHasMore] = useState(true)
  const [loading, setLoading] = useState(false)
  const [loadingInitial, setLoadingInitial] = useState(true)
  const [error, setError] = useState('')

  const loadFeed = useCallback(async (targetPage) => {
    setLoading(true)
    setError('')

    try {
      const raw = await getFeedPage({ page: targetPage, size: 6 })
      const { content, hasNext } = normalizeFeedResponse(raw)
      const mapped = dedupeActivities(
        content
          .map((entry, index) => mapActivity(entry, index))
          .filter((entry) => {
            if (!user?.userId) return true
            return Number(entry.actorId) !== Number(user.userId)
          }),
      )
      setActivities((prev) => dedupeActivities(targetPage === 0 ? mapped : [...prev, ...mapped]))
      setHasMore(hasNext && mapped.length > 0)
      setPage(targetPage)
    } catch {
      setError('We could not load your reading circle right now.')
    } finally {
      setLoading(false)
      setLoadingInitial(false)
    }
  }, [user?.userId])

  useEffect(() => {
    loadFeed(0)
  }, [loadFeed])

  useEffect(() => {
    if (!inView || loading || !hasMore || loadingInitial) return
    loadFeed(page + 1)
  }, [hasMore, inView, loadFeed, loading, loadingInitial, page])

  useEffect(() => {
    const query = userSearch.trim()
    if (!query) {
      setUserResults([])
      return
    }
    const timer = setTimeout(async () => {
      try {
        setSearchingUsers(true)
        const results = await searchUsersByUsername(query, 8)
        setUserResults(results)
      } finally {
        setSearchingUsers(false)
      }
    }, 300)
    return () => clearTimeout(timer)
  }, [userSearch])

  const handleToggleFollow = async (targetUserId, currentlyFollowing) => {
    try {
      if (currentlyFollowing) {
        await unfollowUser(targetUserId)
      } else {
        await followUser(targetUserId)
      }
      setUserResults((prev) => prev.map((entry) => (
        Number(entry.id) === Number(targetUserId)
          ? { ...entry, following: !currentlyFollowing }
          : entry
      )))
    } catch (followError) {
      console.error('Follow toggle failed', followError)
    }
  }

  const emptyState = useMemo(
    () => !loadingInitial && activities.length === 0 && !error,
    [activities.length, error, loadingInitial],
  )

  const { leftColumn, rightColumn } = useMemo(() => {
    const splitAt = Math.ceil(activities.length / 2)
    return {
      leftColumn: activities.slice(0, splitAt),
      rightColumn: activities.slice(splitAt),
    }
  }, [activities])

  const mastheadMotion = reduceMotion
    ? {}
    : {
        initial: { opacity: 0, y: 8 },
        animate: { opacity: 1, y: 0 },
        transition: { duration: 0.4, ease: 'easeOut' },
      }

  return (
    <AppChrome className="feed-page">
      <div className="feed-shell">
        <section className="feed-masthead">
          <div className="feed-masthead__row">
            <motion.div className="feed-masthead__title-row" {...mastheadMotion}>
              <h2>Reading Circle</h2>
              <button
                type="button"
                className={`feed-find__toggle${findOpen ? ' is-open' : ''}`}
                aria-expanded={findOpen}
                aria-controls="feed-find-panel"
                onClick={() => setFindOpen((prev) => !prev)}
              >
                Find readers
              </button>
            </motion.div>
            <p className="feed-lede">Updates from people you follow.</p>

            <AnimatePresence initial={false}>
              {findOpen && (
                <motion.div
                  id="feed-find-panel"
                  className="feed-find__panel"
                  initial={reduceMotion ? false : { opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: 'auto' }}
                  exit={reduceMotion ? undefined : { opacity: 0, height: 0 }}
                  transition={{ duration: 0.22 }}
                >
                  <input
                    type="search"
                    placeholder="Search by username…"
                    value={userSearch}
                    onChange={(event) => setUserSearch(event.target.value)}
                    autoFocus
                  />
                  {searchingUsers && <p className="feed-find__muted">Searching…</p>}
                  {!searchingUsers && userSearch.trim() && userResults.length === 0 && (
                    <p className="feed-find__muted">No users found.</p>
                  )}
                  {userResults.length > 0 && (
                    <div className="feed-find__list">
                      {userResults.map((searchUser) => (
                        <article key={searchUser.id} className="feed-find__item">
                          <button
                            type="button"
                            className="feed-find__profile"
                            onClick={() => navigate(`/users/${searchUser.id}`)}
                          >
                            <img
                              src={resolveMediaUrl(searchUser.avatarUrl, '/user-stub.png')}
                              alt=""
                            />
                            <span>{searchUser.username}</span>
                          </button>
                          <button
                            type="button"
                            className="feed-find__follow"
                            onClick={() => handleToggleFollow(searchUser.id, Boolean(searchUser.following))}
                          >
                            {searchUser.following ? 'Unfollow' : 'Follow'}
                          </button>
                        </article>
                      ))}
                    </div>
                  )}
                </motion.div>
              )}
            </AnimatePresence>
          </div>
        </section>

        <section className="feed-stream">
          {loadingInitial && <FeedSkeleton />}
          {error && (
            <div className="feed-banner feed-error">
              <p>{error}</p>
              <button type="button" onClick={() => loadFeed(0)}>Retry</button>
            </div>
          )}
          {emptyState && (
            <div className="feed-banner feed-empty">
              <p className="feed-empty__title">Your shelf is quiet</p>
              <p>Follow readers to fill this timeline with reviews and reading updates.</p>
              <button type="button" className="feed-find__toggle" onClick={() => setFindOpen(true)}>
                Find readers
              </button>
            </div>
          )}

          {activities.length > 0 && (
            <div className="feed-columns">
              <div className="feed-columns__col">
                {leftColumn.map((activity) => (
                  <FeedCard
                    key={activity.id}
                    activity={activity}
                    onNavigate={navigate}
                    reduceMotion={reduceMotion}
                  />
                ))}
              </div>
              <div className="feed-columns__col">
                {rightColumn.map((activity) => (
                  <FeedCard
                    key={activity.id}
                    activity={activity}
                    onNavigate={navigate}
                    reduceMotion={reduceMotion}
                  />
                ))}
              </div>
            </div>
          )}

          <div ref={ref} className="feed-observer" />
          {loading && !loadingInitial && <p className="feed-muted">Loading more updates…</p>}
        </section>
      </div>
    </AppChrome>
  )
}

export default ActivityFeedPage
