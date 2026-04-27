import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useInView } from 'react-intersection-observer'
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
    likes: Number(reviewSnippet?.helpfulCount ?? activity?.likesCount ?? activity?.likeCount ?? activity?.likes ?? 0),
    comments: Number(book?.totalReviews ?? activity?.commentCount ?? activity?.commentsCount ?? activity?.comments ?? 0),
    targetUserId: targetUser?.id || activity?.targetUserId,
    targetUserName: targetUser?.username || 'Reader',
  }
}

const ActivityFeedPage = () => {
  const navigate = useNavigate()
  const { user, logout } = useAuth()
  const { ref, inView } = useInView({ rootMargin: '320px' })

  const [headerSearch, setHeaderSearch] = useState('')
  const [activities, setActivities] = useState([])
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
      const mapped = content
        .map((entry, index) => mapActivity(entry, index))
        .filter((entry) => {
          if (!user?.userId) return true
          return Number(entry.actorId) !== Number(user.userId)
        })
      setActivities((prev) => (targetPage === 0 ? mapped : [...prev, ...mapped]))
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
    const q = headerSearch.trim()
    if (!q) return
    const timer = setTimeout(() => navigate(`/search?query=${encodeURIComponent(q)}&page=0`), 350)
    return () => clearTimeout(timer)
  }, [headerSearch, navigate])

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
    } catch (error) {
      console.error('Follow toggle failed', error)
    }
  }

  const emptyState = useMemo(() => !loadingInitial && activities.length === 0 && !error, [activities.length, error, loadingInitial])

  return (
    <main className="dashboard feed-page">
      <header className="home-nav">
        <h1>BookReviewer</h1>
        <nav>
          <Link to="/dashboard">Home</Link>
          <Link to="/search?page=0">Library</Link>
          <Link to="/books/new">Add Book</Link>
          <Link to="/dashboard#collections">Collections</Link>
          <Link to="/feed">Feed</Link>
        </nav>
        <input
          className="home-nav__search"
          placeholder="Search the archive..."
          value={headerSearch}
          onChange={(event) => setHeaderSearch(event.target.value)}
        />
        <div className="home-nav__actions">
          <Link className="home-nav__profile" to="/profile" aria-label="My profile" title={user?.username || 'My profile'}>
            <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
              <path d="M12 12c2.76 0 5-2.24 5-5S14.76 2 12 2 7 4.24 7 7s2.24 5 5 5Zm0 2c-3.86 0-7 3.14-7 7 0 .55.45 1 1 1h12c.55 0 1-.45 1-1 0-3.86-3.14-7-7-7Z" />
            </svg>
          </Link>
          <button type="button" onClick={logout}>Logout</button>
        </div>
      </header>

      <section className="feed-hero">
        <h2>Reading Circle</h2>
        <p>Daily updates from your fellow archivists.</p>
        <div className="feed-user-search">
          <input
            type="search"
            placeholder="Find users by username..."
            value={userSearch}
            onChange={(event) => setUserSearch(event.target.value)}
          />
          {searchingUsers && <p className="feed-user-search__muted">Searching users...</p>}
          {!searchingUsers && userSearch.trim() && userResults.length === 0 && (
            <p className="feed-user-search__muted">No users found.</p>
          )}
          {userResults.length > 0 && (
            <div className="feed-user-search__list">
              {userResults.map((searchUser) => (
                <article key={searchUser.id} className="feed-user-search__item">
                  <button
                    type="button"
                    className="feed-user-search__profile"
                    onClick={() => navigate(`/users/${searchUser.id}`)}
                  >
                    <img src={resolveMediaUrl(searchUser.avatarUrl, '/user-stub.png')} alt={searchUser.username} />
                    <span>{searchUser.username}</span>
                  </button>
                  <button
                    type="button"
                    className="feed-user-search__follow"
                    onClick={() => handleToggleFollow(searchUser.id, Boolean(searchUser.following))}
                  >
                    {searchUser.following ? 'Unfollow' : 'Follow'}
                  </button>
                </article>
              ))}
            </div>
          )}
        </div>
      </section>

      <section className="feed-list">
        {loadingInitial && <p className="feed-muted">Loading your feed...</p>}
        {error && (
          <div className="feed-error">
            <p>{error}</p>
            <button type="button" onClick={() => loadFeed(0)}>Retry</button>
          </div>
        )}
        {emptyState && (
          <div className="feed-empty">
            <p>Your feed is empty. Follow readers to see their reviews and status updates.</p>
          </div>
        )}

        {activities.map((activity) => (
          <article key={activity.id} className={`feed-card feed-card--${activity.type}`}>
            <div className="feed-card__header">
              <button
                type="button"
                className="feed-card__avatar-link"
                onClick={() => activity.actorId && navigate(`/users/${activity.actorId}`)}
                aria-label={`Open ${activity.actorName} profile`}
              >
                <img src={activity.actorAvatar} alt={activity.actorName} />
              </button>
              <div>
                <p className="feed-card__meta">
                  <strong>{activity.actorName}</strong>{' '}
                  {activity.type === 'review' && 'wrote a new review'}
                  {activity.type === 'status_change' && 'finished reading'}
                  {activity.type === 'reading_intent' && `added to ${activity.status || 'reading list'}`}
                  {activity.type === 'followed_user' && `started following ${activity.targetUserName}`}
                  {activity.type === 'book_added' && 'added a book to catalog'}
                </p>
                <p className="feed-card__time">{activity.createdAtText}</p>
              </div>
            </div>

            {(activity.type === 'status_change' || activity.type === 'reading_intent' || activity.type === 'book_added') && (
              <div className="feed-card__body">
                <img
                  src={activity.bookCover}
                  alt={activity.bookTitle}
                  onClick={() => activity.bookId && navigate(`/books/${activity.bookId}`)}
                  role={activity.bookId ? 'button' : undefined}
                />
                <div>
                  <h3>{activity.bookTitle}</h3>
                  {activity.type === 'status_change' && (
                    <p className="feed-card__status">{activity.status || 'Status updated'}</p>
                  )}
                  {(activity.type === 'reading_intent' || activity.type === 'book_added' || activity.type === 'status_change') && activity.shortDescription && (
                    <p className="feed-card__desc">{activity.shortDescription}</p>
                  )}
                  <div className="feed-card__actions">
                    {activity.type === 'reading_intent' && activity.bookId && (
                      <button type="button" onClick={() => navigate(`/books/${activity.bookId}`)}>Preview</button>
                    )}
                    {activity.type === 'book_added' && activity.bookId && (
                      <button type="button" onClick={() => navigate(`/books/${activity.bookId}`)}>See book</button>
                    )}
                    {activity.type === 'status_change' && activity.bookId && (
                      <button type="button" onClick={() => navigate(`/books/${activity.bookId}`)}>Open book</button>
                    )}
                  </div>
                </div>
              </div>
            )}

            {activity.type === 'review' && (
              <div className="feed-review">
                <h3>{activity.bookTitle}</h3>
                {activity.bookAuthor && <p className="feed-card__author">{activity.bookAuthor}</p>}
                {activity.verdict && <blockquote>"{activity.verdict}"</blockquote>}
                {activity.whoIsItFor && (
                  <div className="feed-review__section">
                    <p>Who this is for</p>
                    <span>{activity.whoIsItFor}</span>
                  </div>
                )}
                {activity.moods.length > 0 && (
                  <div className="feed-review__moods">
                    {activity.moods.slice(0, 4).map((mood) => <span key={mood}>{mood}</span>)}
                  </div>
                )}
                <div className="feed-card__actions">
                  {activity.reviewId && activity.bookId && (
                    <button type="button" onClick={() => navigate(`/books/${activity.bookId}#review-${activity.reviewId}`)}>
                      See full review
                    </button>
                  )}
                </div>
              </div>
            )}

            {activity.type === 'followed_user' && (
              <div className="feed-follow">
                <p>{activity.actorName} started following {activity.targetUserName}.</p>
                {activity.targetUserId && (
                  <button type="button" onClick={() => navigate(`/users/${activity.targetUserId}`)}>
                    See profile
                  </button>
                )}
              </div>
            )}

            <footer className="feed-card__footer">
              <span>♡ {activity.likes}</span>
              <span>▢ {activity.comments}</span>
            </footer>
          </article>
        ))}

        <div ref={ref} className="feed-observer" />
        {loading && !loadingInitial && <p className="feed-muted">Loading more updates...</p>}
      </section>
    </main>
  )
}

export default ActivityFeedPage
