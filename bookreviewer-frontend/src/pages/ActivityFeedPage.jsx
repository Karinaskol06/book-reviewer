import { memo, useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useInView } from 'react-intersection-observer'
import { motion, useReducedMotion } from 'framer-motion'
import AppChrome from '../components/layout/AppChrome.jsx'
import { useAuth } from '../hooks/useAuth.js'
import { getFeedPage } from '../services/feedService.js'
import { followUser, searchUsersByUsername, unfollowUser } from '../services/userService.js'
import { resolveMediaUrl } from '../utils/media.js'
import './ActivityFeedPage.css'

const FEED_PAGE_SIZE = 6

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
    return { content: payload, hasNext: false }
  }

  const content = Array.isArray(payload?.content)
    ? payload.content
    : (Array.isArray(payload?.items) ? payload.items : [])

  let hasNext = false
  if (typeof payload?.last === 'boolean') {
    hasNext = !payload.last
  } else if (typeof payload?.totalPages === 'number' && typeof payload?.number === 'number') {
    hasNext = payload.number + 1 < payload.totalPages
  } else if (typeof payload?.hasNext === 'boolean') {
    hasNext = payload.hasNext
  }

  return { content, hasNext }
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
    createdAt: activity?.createdAt || activity?.timestamp || null,
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


const FILTERS = [
  { id: 'all', label: 'All', match: () => true },
  { id: 'reviews', label: 'Reviews', match: (activity) => activity.type === 'review' },
  {
    id: 'reading',
    label: 'Reading',
    match: (activity) => ['reading_intent', 'status_change', 'book_added'].includes(activity.type),
  },
  { id: 'people', label: 'People', match: (activity) => activity.type === 'followed_user' },
]

const SEARCH_ICON = 'M9 15a6 6 0 1 0 0-12 6 6 0 0 0 0 12zM17 17l-3.6-3.6'

const TYPE_BADGES = {
  review: { label: 'Review', icon: 'M4 4h12v9H8l-4 3z' },
  reading_intent: { label: 'Reading list', icon: 'M6 3h8v14l-4-3-4 3z' },
  status_change: { label: 'Finished', icon: 'M4 10.5l3.5 3.5L16 5.5' },
  book_added: { label: 'New book', icon: 'M10 4v12M4 10h12' },
  followed_user: { label: 'Follow', icon: 'M10 9a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM4 17c.8-3 3.1-4.5 6-4.5s5.2 1.5 6 4.5' },
}

const startOfDay = (date) => new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()

/* In a newest-first feed the day buckets only move forward, so an appended page extends the last group
   instead of reshuffling cards that are already on screen. */
const getDayGroup = (value) => {
  if (!value) return 'Earlier'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Earlier'
  const days = Math.round((startOfDay(new Date()) - startOfDay(date)) / 86400000)
  if (days <= 0) return 'Today'
  if (days === 1) return 'Yesterday'
  if (days < 7) return 'This week'
  return 'Earlier'
}

const groupByDay = (list) => {
  const groups = []
  list.forEach((activity) => {
    const label = getDayGroup(activity.createdAt)
    const last = groups[groups.length - 1]
    if (last && last.label === label) {
      last.items.push(activity)
    } else {
      groups.push({ label, key: `${label}-${groups.length}`, items: [activity] })
    }
  })
  return groups
}

const Icon = ({ path }) => (
  <svg className="feed-icon" viewBox="0 0 20 20" aria-hidden="true">
    <path d={path} />
  </svg>
)

const BookCover = ({ src, title, bookId, onNavigate, size = 'md' }) => (
  <button
    type="button"
    className={`feed-cover feed-cover--${size}`}
    onClick={() => bookId && onNavigate(`/books/${bookId}`)}
    aria-label={`Open ${title}`}
  >
    <img src={src} alt="" loading="lazy" decoding="async" width="96" height="144" />
  </button>
)

const ActorButton = ({ activity, onNavigate }) => (
  <button
    type="button"
    className="feed-card__name"
    onClick={() => activity.actorId && onNavigate(`/users/${activity.actorId}`)}
  >
    {activity.actorName}
  </button>
)

const CardHeader = ({ activity, onNavigate }) => {
  const badge = TYPE_BADGES[activity.type] || TYPE_BADGES.status_change
  return (
    <header className="feed-card__header">
      <button
        type="button"
        className="feed-card__avatar"
        onClick={() => activity.actorId && onNavigate(`/users/${activity.actorId}`)}
        aria-label={`Open ${activity.actorName}'s profile`}
      >
        <img src={activity.actorAvatar} alt="" loading="lazy" decoding="async" width="40" height="40" />
      </button>
      <div className="feed-card__who">
        <p>
          <ActorButton activity={activity} onNavigate={onNavigate} />{' '}
          <span className="feed-card__verb">{activityVerb(activity)}</span>
        </p>
        <time className="feed-card__time" dateTime={activity.createdAt || undefined}>
          {activity.createdAtText}
        </time>
      </div>
      <span className={`feed-badge feed-badge--${activity.type}`}>
        <Icon path={badge.icon} />
        {badge.label}
      </span>
    </header>
  )
}

const FeedCard = memo(({ activity, onNavigate, reduceMotion }) => {
  const MotionArticle = motion.article
  // One-shot reveal on a stable element: once shown, a card never fades out again.
  const motionProps = reduceMotion
    ? {}
    : {
        initial: { opacity: 0, y: 12 },
        whileInView: { opacity: 1, y: 0 },
        viewport: { once: true, margin: '0px 0px -40px 0px' },
        transition: { duration: 0.35, ease: [0.22, 1, 0.36, 1] },
      }

  if (activity.type === 'followed_user') {
    return (
      <MotionArticle className="feed-card feed-card--follow" {...motionProps}>
        <button
          type="button"
          className="feed-card__avatar feed-card__avatar--sm"
          onClick={() => activity.actorId && onNavigate(`/users/${activity.actorId}`)}
          aria-label={`Open ${activity.actorName}'s profile`}
        >
          <img src={activity.actorAvatar} alt="" loading="lazy" decoding="async" width="32" height="32" />
        </button>
        <p className="feed-card__follow-line">
          <ActorButton activity={activity} onNavigate={onNavigate} /> started following{' '}
          <button
            type="button"
            className="feed-card__name"
            onClick={() => activity.targetUserId && onNavigate(`/users/${activity.targetUserId}`)}
          >
            {activity.targetUserName}
          </button>
        </p>
        <time className="feed-card__time" dateTime={activity.createdAt || undefined}>
          {activity.createdAtText}
        </time>
      </MotionArticle>
    )
  }

  if (activity.type === 'review') {
    const excerpt = activity.detailedReview?.trim()
    const hasLink = Boolean(activity.reviewId && activity.bookId)
    return (
      <MotionArticle className="feed-card feed-card--review" {...motionProps}>
        <CardHeader activity={activity} onNavigate={onNavigate} />
        <div className="feed-card__body">
          <BookCover src={activity.bookCover} title={activity.bookTitle} bookId={activity.bookId} onNavigate={onNavigate} />
          <div className="feed-card__copy">
            <h3 className="feed-card__title">{activity.bookTitle}</h3>
            {activity.bookAuthor && <p className="feed-card__author">{activity.bookAuthor}</p>}
            {activity.verdict && <blockquote className="feed-verdict">{activity.verdict}</blockquote>}
            {excerpt && <p className="feed-excerpt">{excerpt}</p>}
            {activity.whoIsItFor && (
              <p className="feed-for">
                <span>For</span> {activity.whoIsItFor}
              </p>
            )}
          </div>
        </div>
        {(activity.moods.length > 0 || hasLink) && (
          <footer className="feed-card__footer">
            {activity.moods.length > 0 && (
              <ul className="feed-moods" aria-label="Moods">
                {activity.moods.slice(0, 4).map((mood) => (
                  <li key={mood}>{mood}</li>
                ))}
              </ul>
            )}
            {hasLink && (
              <button
                type="button"
                className="feed-link"
                onClick={() => onNavigate(`/books/${activity.bookId}#review-${activity.reviewId}`)}
              >
                Read full review <span aria-hidden="true">→</span>
              </button>
            )}
          </footer>
        )}
      </MotionArticle>
    )
  }

  const actionLabel =
    activity.type === 'reading_intent' ? 'Preview book' : activity.type === 'book_added' ? 'See book' : 'Open book'

  return (
    <MotionArticle className={`feed-card feed-card--compact feed-card--${activity.type}`} {...motionProps}>
      <CardHeader activity={activity} onNavigate={onNavigate} />
      <div className="feed-card__body feed-card__body--compact">
        <BookCover
          src={activity.bookCover}
          title={activity.bookTitle}
          bookId={activity.bookId}
          onNavigate={onNavigate}
          size="sm"
        />
        <div className="feed-card__copy">
          <h3 className="feed-card__title feed-card__title--sm">{activity.bookTitle}</h3>
          {activity.bookAuthor && <p className="feed-card__author">{activity.bookAuthor}</p>}
          {activity.type === 'status_change' && activity.status && <p className="feed-status">{activity.status}</p>}
          {activity.shortDescription && <p className="feed-desc">{activity.shortDescription}</p>}
          {activity.bookId && (
            <button type="button" className="feed-link" onClick={() => onNavigate(`/books/${activity.bookId}`)}>
              {actionLabel} <span aria-hidden="true">→</span>
            </button>
          )}
        </div>
      </div>
    </MotionArticle>
  )
})

const SkeletonCard = () => (
  <div className="feed-card feed-skeleton" aria-hidden="true">
    <div className="feed-skeleton__head">
      <span className="feed-skeleton__circle" />
      <span className="feed-skeleton__line feed-skeleton__line--title" />
    </div>
    <div className="feed-skeleton__body">
      <span className="feed-skeleton__cover" />
      <div>
        <span className="feed-skeleton__line feed-skeleton__line--title" />
        <span className="feed-skeleton__line" />
        <span className="feed-skeleton__line" />
        <span className="feed-skeleton__line feed-skeleton__line--short" />
      </div>
    </div>
  </div>
)

const ActivityFeedPage = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const { user } = useAuth()
  const userId = user?.userId
  const reduceMotion = useReducedMotion()

  const [activities, setActivities] = useState([])
  const [filter, setFilter] = useState('all')
  const [findOpen, setFindOpen] = useState(() => Boolean(location.state?.openFind))
  const [seenNavState, setSeenNavState] = useState(location.state)
  const [userSearch, setUserSearch] = useState('')
  const [userResults, setUserResults] = useState([])
  const [searchingUsers, setSearchingUsers] = useState(false)
  const [page, setPage] = useState(0)
  const [hasMore, setHasMore] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [loadingInitial, setLoadingInitial] = useState(true)
  const [error, setError] = useState('')
  const [followBusyId, setFollowBusyId] = useState(null)

  const inFlightRef = useRef(false)
  const requestIdRef = useRef(0)
  const searchInputRef = useRef(null)

  // "Find readers" links elsewhere navigate here with { openFind: true }.
  if (location.state !== seenNavState) {
    setSeenNavState(location.state)
    if (location.state?.openFind) setFindOpen(true)
  }

  useEffect(() => {
    if (location.state?.openFind) searchInputRef.current?.focus()
  }, [location.state])

  // All state writes happen in the promise callback, after the request settles; callers flip the loading
  // flags themselves.
  const applyPage = useCallback((targetPage, requestId, { raw, failed }) => {
    if (requestId !== requestIdRef.current) return
    inFlightRef.current = false
    setLoadingInitial(false)
    setLoadingMore(false)

    if (failed) {
      setError('We could not load your reading circle right now.')
      if (targetPage === 0) {
        setActivities([])
        setHasMore(false)
      }
      return
    }

    const { content, hasNext } = normalizeFeedResponse(raw)
    const mapped = dedupeActivities(
      content
        .map((entry, index) => mapActivity(entry, index))
        .filter((entry) => !userId || Number(entry.actorId) !== Number(userId)),
    )

    setError('')
    setActivities((prev) => (targetPage === 0 ? mapped : dedupeActivities([...prev, ...mapped])))
    setHasMore(Boolean(hasNext) && mapped.length > 0)
    setPage(targetPage)
  }, [userId])

  const fetchPage = useCallback((targetPage) => {
    const requestId = ++requestIdRef.current
    inFlightRef.current = true
    return getFeedPage({ page: targetPage, size: FEED_PAGE_SIZE })
      .then((raw) => ({ raw }), () => ({ failed: true }))
      .then((result) => applyPage(targetPage, requestId, result))
  }, [applyPage])

  useEffect(() => {
    fetchPage(0)
  }, [fetchPage])

  const reload = () => {
    setLoadingInitial(true)
    setHasMore(true)
    setError('')
    return fetchPage(0)
  }

  const loadNextPage = () => {
    if (inFlightRef.current || loadingInitial || !hasMore || error) return
    setLoadingMore(true)
    fetchPage(page + 1)
  }

  // The sentinel is re-keyed per page, so if it is still within range after a page lands, the fresh
  // observer reports it straight away and the next page is requested.
  const { ref: sentinelRef } = useInView({
    rootMargin: '0px 0px 900px 0px',
    onChange: (inView) => {
      if (inView) loadNextPage()
    },
  })

  useEffect(() => {
    const query = userSearch.trim()
    if (!query) return undefined
    let cancelled = false
    const timer = setTimeout(async () => {
      setSearchingUsers(true)
      try {
        const results = await searchUsersByUsername(query, 8)
        if (!cancelled) setUserResults(results)
      } finally {
        if (!cancelled) setSearchingUsers(false)
      }
    }, 300)
    return () => {
      cancelled = true
      clearTimeout(timer)
    }
  }, [userSearch])

  const handleSearchChange = (event) => {
    setUserSearch(event.target.value)
    if (!event.target.value.trim()) setUserResults([])
  }

  const handleToggleFollow = async (targetUserId, currentlyFollowing) => {
    if (followBusyId != null) return
    setFollowBusyId(targetUserId)
    try {
      if (currentlyFollowing) {
        await unfollowUser(targetUserId)
        setActivities((prev) => prev.filter((item) => Number(item.actorId) !== Number(targetUserId)))
      } else {
        await followUser(targetUserId)
      }
      setUserResults((prev) => prev.map((entry) => (
        Number(entry.id) === Number(targetUserId) ? { ...entry, following: !currentlyFollowing } : entry
      )))
      if (!currentlyFollowing) await reload()
    } catch (followError) {
      console.error('Follow toggle failed', followError)
    } finally {
      setFollowBusyId(null)
    }
  }

  const openFind = () => {
    setFindOpen(true)
    requestAnimationFrame(() => searchInputRef.current?.focus())
  }

  const visibleActivities = useMemo(() => {
    const active = FILTERS.find((entry) => entry.id === filter) || FILTERS[0]
    return activities.filter(active.match)
  }, [activities, filter])

  const groups = useMemo(() => groupByDay(visibleActivities), [visibleActivities])

  const emptyState = !loadingInitial && activities.length === 0 && !error
  const filterEmpty = !loadingInitial && activities.length > 0 && visibleActivities.length === 0 && !hasMore

  return (
    <AppChrome className="feed-page">
      <div className="feed-shell">
        <header className="feed-head">
          <div className="feed-head__copy">
            <p className="feed-eyebrow">
              <span aria-hidden="true" />
              Reading circle
            </p>
            <h2>What your circle is reading</h2>
            <p className="feed-lede">Reviews, finished books and new follows from the readers you keep up with.</p>
          </div>
          <button
            type="button"
            className={`feed-find-toggle${findOpen ? ' is-open' : ''}`}
            aria-expanded={findOpen}
            aria-controls="feed-find"
            onClick={() => (findOpen ? setFindOpen(false) : openFind())}
          >
            <Icon path={SEARCH_ICON} />
            Find readers
          </button>
        </header>

        <div className="feed-layout">
          <aside id="feed-find" className={`feed-aside${findOpen ? ' is-open' : ''}`} aria-label="Find readers">
            <div className="feed-panel">
              <h3>Find readers</h3>
              <p className="feed-panel__hint">Follow people whose taste you trust and their activity shows up here.</p>
              <label className="feed-search">
                <span className="feed-sr-only">Search readers by username</span>
                <Icon path={SEARCH_ICON} />
                <input
                  ref={searchInputRef}
                  type="search"
                  placeholder="Search by username…"
                  value={userSearch}
                  onChange={handleSearchChange}
                />
              </label>
              <div className="feed-panel__results" aria-live="polite">
                {searchingUsers && <p className="feed-panel__muted">Searching…</p>}
                {!searchingUsers && userSearch.trim() && userResults.length === 0 && (
                  <p className="feed-panel__muted">No readers found.</p>
                )}
                {userResults.length > 0 && (
                  <ul className="feed-people">
                    {userResults.map((searchUser) => (
                      <li key={searchUser.id}>
                        <button
                          type="button"
                          className="feed-people__profile"
                          onClick={() => navigate(`/users/${searchUser.id}`)}
                        >
                          <img src={resolveMediaUrl(searchUser.avatarUrl, '/user-stub.png')} alt="" width="36" height="36" />
                          <span>{searchUser.username}</span>
                        </button>
                        <button
                          type="button"
                          className={`feed-people__follow${searchUser.following ? ' is-following' : ''}`}
                          disabled={followBusyId === searchUser.id}
                          aria-label={`${searchUser.following ? 'Unfollow' : 'Follow'} ${searchUser.username}`}
                          onClick={() => handleToggleFollow(searchUser.id, Boolean(searchUser.following))}
                        >
                          {followBusyId === searchUser.id ? '…' : (searchUser.following ? 'Following' : 'Follow')}
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>
          </aside>

          <div className="feed-main">
            <div className="feed-filters" role="group" aria-label="Filter activity">
              {FILTERS.map((entry) => (
                <button
                  key={entry.id}
                  type="button"
                  aria-pressed={filter === entry.id}
                  className={filter === entry.id ? 'is-active' : ''}
                  onClick={() => setFilter(entry.id)}
                >
                  {entry.label}
                </button>
              ))}
            </div>

            {error && (
              <div className="feed-notice feed-notice--error" role="alert">
                <p>{error}</p>
                <button type="button" onClick={() => (activities.length ? loadNextPage() : reload())}>
                  Try again
                </button>
              </div>
            )}

            {emptyState && (
              <div className="feed-notice">
                <p className="feed-notice__title">Your circle is quiet</p>
                <p>Follow a few readers and their reviews and reading updates will land here.</p>
                <button type="button" onClick={openFind}>
                  Find readers
                </button>
              </div>
            )}

            <div className="feed-timeline" role="feed" aria-busy={loadingInitial || loadingMore}>
              {loadingInitial && Array.from({ length: 3 }).map((_, index) => <SkeletonCard key={index} />)}
              {groups.map((group) => (
                <section key={group.key} className="feed-day" aria-label={group.label}>
                  <h3 className="feed-day__label">{group.label}</h3>
                  {group.items.map((activity) => (
                    <FeedCard
                      key={activityDedupeKey(activity)}
                      activity={activity}
                      onNavigate={navigate}
                      reduceMotion={reduceMotion}
                    />
                  ))}
                </section>
              ))}
              {loadingMore && <SkeletonCard />}
            </div>

            {filterEmpty && <p className="feed-end">Nothing like this in your circle yet.</p>}
            {hasMore && !loadingInitial && <div key={page} ref={sentinelRef} className="feed-sentinel" aria-hidden="true" />}
            {!hasMore && !loadingInitial && activities.length > 0 && !filterEmpty && (
              <p className="feed-end">You’re all caught up.</p>
            )}
          </div>
        </div>
      </div>
    </AppChrome>
  )
}

export default ActivityFeedPage
