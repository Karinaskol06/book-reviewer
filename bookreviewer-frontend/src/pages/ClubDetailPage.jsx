import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { motion } from 'framer-motion'
import AppChrome from '../components/layout/AppChrome.jsx'
import { useAuth } from '../hooks/useAuth.js'
import { searchBooks } from '../services/homeService.js'
import {
  approveMember,
  createPost,
  createReply,
  deleteClub,
  deletePost,
  demoteMember,
  getClub,
  getClubMembers,
  getClubPosts,
  getPendingMembers,
  getReplies,
  joinClub,
  leaveClub,
  promoteMember,
  rejectMember,
  removeMember,
  setCurrentBook,
  setNextMeeting,
  toggleInsightful,
  transferOwnership,
  updateClub,
} from '../services/clubService.js'
import { resolveMediaUrl } from '../utils/media.js'
import './ClubsPage.css'
import './ClubDetailPage.css'

const ClubDetailPage = () => {
  const { clubId } = useParams()
  const { user } = useAuth()
  const navigate = useNavigate()

  const [club, setClub] = useState(null)
  const [members, setMembers] = useState([])
  const [pending, setPending] = useState([])
  const [posts, setPosts] = useState([])
  const [repliesByPost, setRepliesByPost] = useState({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [postText, setPostText] = useState('')
  const [replyDrafts, setReplyDrafts] = useState({})
  const [bookQuery, setBookQuery] = useState('')
  const [bookHits, setBookHits] = useState([])
  const [meetingTime, setMeetingTime] = useState('')
  const [meetingLink, setMeetingLink] = useState('')
  const [editOpen, setEditOpen] = useState(false)
  const [editForm, setEditForm] = useState({ name: '', description: '', focus: '', isPrivate: false })

  const membership = club?.userMembership
  const role = membership?.role
  const status = membership?.status
  const isActive = status === 'ACTIVE'
  const isOwner = role === 'OWNER' && isActive
  const isMod = (role === 'MODERATOR' || role === 'OWNER') && isActive
  const canDiscuss = isActive || !club?.isPrivate
  const canPost = isActive

  const refresh = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const details = await getClub(clubId)
      setClub(details)
      setMeetingLink(details.meetingLink || '')
      setMeetingTime(details.nextMeetingAt ? details.nextMeetingAt.slice(0, 16) : '')
      setEditForm({
        name: details.name || '',
        description: details.description || '',
        focus: details.focus || '',
        isPrivate: Boolean(details.isPrivate),
      })

      const memberList = await getClubMembers(clubId)
      setMembers(memberList || [])

      const mem = details.userMembership
      const mod = mem?.status === 'ACTIVE' && (mem?.role === 'OWNER' || mem?.role === 'MODERATOR')
      if (mod) {
        try {
          setPending(await getPendingMembers(clubId) || [])
        } catch {
          setPending([])
        }
      } else {
        setPending([])
      }

      const canLoadPosts = !details.isPrivate || mem?.status === 'ACTIVE'
      if (canLoadPosts) {
        const page = await getClubPosts(clubId)
        setPosts(page?.content || page || [])
      } else {
        setPosts([])
      }
    } catch (err) {
      setError(err?.response?.data?.message || 'Could not load club.')
    } finally {
      setLoading(false)
    }
  }, [clubId])

  useEffect(() => {
    refresh()
  }, [refresh])

  const membershipLabel = useMemo(() => {
    if (!membership) return 'Not a member'
    if (status === 'PENDING') return 'Join request pending'
    if (status === 'DECLINED') return 'Request declined — you can request again'
    return `${role}`
  }, [membership, role, status])

  const handleJoin = async () => {
    await joinClub(clubId)
    await refresh()
  }

  const handleLeave = async () => {
    if (isOwner) {
      setError('Transfer ownership to another active member before leaving.')
      return
    }
    await leaveClub(clubId)
    await refresh()
  }

  const handleDeleteClub = async () => {
    if (!window.confirm('Delete this club and all posts? This cannot be undone.')) return
    await deleteClub(clubId)
    navigate('/clubs')
  }

  const handleSaveEdit = async (event) => {
    event.preventDefault()
    await updateClub(clubId, editForm)
    setEditOpen(false)
    await refresh()
  }

  const handleCreatePost = async (event) => {
    event.preventDefault()
    if (!postText.trim()) return
    await createPost(clubId, postText.trim())
    setPostText('')
    await refresh()
  }

  const loadReplies = async (postId) => {
    const page = await getReplies(clubId, postId)
    setRepliesByPost((prev) => ({ ...prev, [postId]: page?.content || page || [] }))
  }

  const handleReply = async (postId) => {
    const text = (replyDrafts[postId] || '').trim()
    if (!text) return
    await createReply(clubId, postId, text)
    setReplyDrafts((prev) => ({ ...prev, [postId]: '' }))
    await loadReplies(postId)
    await refresh()
  }

  const searchBook = async () => {
    if (!bookQuery.trim()) return
    const hits = await searchBooks(bookQuery.trim(), 6)
    setBookHits(hits || [])
  }

  const saveMeeting = async () => {
    await setNextMeeting(clubId, {
      meetingTime: meetingTime ? `${meetingTime}:00` : null,
      meetingLink: meetingLink.trim() || null,
    })
    await refresh()
  }

  if (loading) {
    return (
      <AppChrome className="clubs-shell">
        <div className="clubs-content">
          <p className="clubs-muted">Loading club…</p>
        </div>
      </AppChrome>
    )
  }

  if (!club) {
    return (
      <AppChrome className="clubs-shell">
        <div className="clubs-content">
          <p className="clubs-error">{error || 'Club not found.'}</p>
          <Link to="/clubs" className="club-back">← Back to clubs</Link>
        </div>
      </AppChrome>
    )
  }

  const initial = (club.name || '?').trim().charAt(0).toUpperCase()

  return (
    <AppChrome className="clubs-shell">
      <div className="clubs-content club-detail">
        <Link to="/clubs" className="club-back">← All clubs</Link>

        <motion.section
          className="club-masthead"
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.35 }}
        >
          <div className="club-masthead__mark" aria-hidden="true">{initial}</div>
          <div className="club-masthead__copy">
            <div className="club-title-row">
              <h2>{club.name}</h2>
              {club.isPrivate && <span className="club-badge club-badge--ink">Private</span>}
            </div>
            {club.focus && <p className="club-focus">{club.focus}</p>}
            {club.description && <p className="club-lede">{club.description}</p>}
            <div className="club-status-row">
              <span className="club-pill">{membershipLabel}</span>
              <span className="club-pill club-pill--soft">{club.memberCount ?? 0} members</span>
            </div>
          </div>
          <div className="club-actions">
            {!membership && (
              <button type="button" className="clubs-primary" onClick={handleJoin}>
                {club.isPrivate ? 'Request to join' : 'Join club'}
              </button>
            )}
            {status === 'DECLINED' && (
              <button type="button" className="clubs-primary" onClick={handleJoin}>Request again</button>
            )}
            {isActive && !isOwner && (
              <button type="button" className="ghost" onClick={handleLeave}>Leave</button>
            )}
            {isMod && (
              <button type="button" className="ghost" onClick={() => setEditOpen(true)}>Edit</button>
            )}
            {isOwner && (
              <button type="button" className="danger" onClick={handleDeleteClub}>Delete club</button>
            )}
          </div>
        </motion.section>

        {error && <p className="clubs-error" role="alert">{error}</p>}

        <div className="club-layout">
          <aside className="club-rail">
            <section className="club-panel club-panel--shelf">
              <header className="club-people__head">
                <h3>Now reading</h3>
                {club.currentBook && <span className="club-people__count">1</span>}
              </header>

              {club.currentBook ? (
                <button
                  type="button"
                  className="club-book"
                  onClick={() => navigate(`/books/${club.currentBook.id}`)}
                >
                  <img
                    src={resolveMediaUrl(club.currentBook.coverUrl, '/home-book.jpg')}
                    alt=""
                  />
                  <div className="club-book__meta">
                    <p className="club-book__title">{club.currentBook.title}</p>
                    <p className="club-book__author">{club.currentBook.author}</p>
                    <span className="club-book__cta">View book</span>
                  </div>
                </button>
              ) : (
                <div className="club-shelf-empty">
                  <p className="club-shelf-empty__title">No book on the stand yet</p>
                  <p className="club-shelf-empty__hint">
                    {isMod
                      ? 'Search the catalog below to set what the club is reading.'
                      : 'A moderator will set the current title soon.'}
                  </p>
                </div>
              )}

              {isMod && (
                <div className="club-mod-block">
                  <p className="club-mod-block__label">Set current book</p>
                  <div className="club-search-row">
                    <input
                      value={bookQuery}
                      onChange={(e) => setBookQuery(e.target.value)}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter') {
                          e.preventDefault()
                          searchBook()
                        }
                      }}
                      placeholder="Search catalog…"
                      aria-label="Search books for current reading"
                    />
                    <button type="button" className="ghost" onClick={searchBook}>
                      Search
                    </button>
                  </div>
                  {bookHits.length > 0 && (
                    <ul className="club-book-hits">
                      {bookHits.map((book) => (
                        <li key={book.id}>
                          <button
                            type="button"
                            onClick={async () => {
                              await setCurrentBook(clubId, book.id)
                              setBookHits([])
                              setBookQuery('')
                              await refresh()
                            }}
                          >
                            <span className="club-book-hits__title">{book.title}</span>
                            {book.author && (
                              <span className="club-book-hits__author">{book.author}</span>
                            )}
                          </button>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              )}
            </section>

            <section className="club-panel club-panel--meeting">
              <header className="club-people__head">
                <h3>Next meeting</h3>
                {club.nextMeetingAt && (
                  <span className="club-people__count club-people__count--alert">Set</span>
                )}
              </header>

              <div className="club-meeting-card">
                {club.nextMeetingAt ? (
                  <>
                    <p className="club-meeting-card__label">When</p>
                    <p className="club-meeting-card__value">
                      {new Date(club.nextMeetingAt).toLocaleString(undefined, {
                        weekday: 'short',
                        year: 'numeric',
                        month: 'short',
                        day: 'numeric',
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                    </p>
                  </>
                ) : (
                  <>
                    <p className="club-meeting-card__label">When</p>
                    <p className="club-meeting-card__empty">No meeting scheduled</p>
                  </>
                )}

                <div className="club-meeting-card__divider" />

                {club.meetingLink ? (
                  <>
                    <p className="club-meeting-card__label">Join</p>
                    <a
                      className="club-meeting-link"
                      href={club.meetingLink}
                      target="_blank"
                      rel="noreferrer"
                    >
                      Open meeting link
                    </a>
                  </>
                ) : (
                  <>
                    <p className="club-meeting-card__label">Join</p>
                    <p className="club-meeting-card__empty">No meeting link yet</p>
                  </>
                )}
              </div>

              {isMod && (
                <div className="club-mod-block">
                  <p className="club-mod-block__label">Schedule</p>
                  <label className="club-field">
                    <span>Date & time</span>
                    <input
                      type="datetime-local"
                      value={meetingTime}
                      onChange={(e) => setMeetingTime(e.target.value)}
                    />
                  </label>
                  <label className="club-field">
                    <span>Zoom / Meet link</span>
                    <input
                      value={meetingLink}
                      onChange={(e) => setMeetingLink(e.target.value)}
                      placeholder="https://…"
                    />
                  </label>
                  <button type="button" className="clubs-primary club-mod-block__save" onClick={saveMeeting}>
                    Save meeting
                  </button>
                </div>
              )}
            </section>
          </aside>

          <section className="club-panel club-panel--wide">
            <div className="club-panel__head">
              <h3>Discussion</h3>
              <span className="clubs-muted">{posts.length} posts</span>
            </div>
            {!canDiscuss && club.isPrivate && (
              <p className="club-gate">Posts are visible after your join request is approved.</p>
            )}
            {canPost && (
              <form className="club-compose" onSubmit={handleCreatePost}>
                <textarea
                  value={postText}
                  onChange={(e) => setPostText(e.target.value)}
                  rows={3}
                  placeholder="Share a thought about the current book…"
                  aria-label="New discussion post"
                />
                <button type="submit" className="clubs-primary">Post</button>
              </form>
            )}

            <div className="club-posts">
              {posts.length === 0 && canDiscuss && (
                <p className="clubs-muted">No posts yet — open the first margin note.</p>
              )}
              {posts.map((post, index) => (
                <motion.article
                  key={post.id}
                  className="club-post"
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.3, delay: Math.min(index * 0.04, 0.24) }}
                >
                  <header>
                    <img src={resolveMediaUrl(post.author?.avatarUrl, '/user-stub.png')} alt="" />
                    <div>
                      <strong>{post.author?.username || 'Member'}</strong>
                      <span>{post.createdAt ? new Date(post.createdAt).toLocaleString() : ''}</span>
                    </div>
                  </header>
                  <p>{post.content}</p>
                  <div className="club-post__actions">
                    {canPost && (
                      <button
                        type="button"
                        className="ghost"
                        onClick={async () => {
                          await toggleInsightful(clubId, post.id)
                          await refresh()
                        }}
                      >
                        Insightful {post.insightfulCount ? `(${post.insightfulCount})` : ''}
                      </button>
                    )}
                    <button type="button" className="ghost" onClick={() => loadReplies(post.id)}>
                      Replies {post.replyCount != null ? `(${post.replyCount})` : ''}
                    </button>
                    {(canPost && (post.author?.id === user?.userId || isMod)) && (
                      <button
                        type="button"
                        className="ghost"
                        onClick={async () => {
                          await deletePost(clubId, post.id)
                          await refresh()
                        }}
                      >
                        Delete
                      </button>
                    )}
                  </div>
                  {(repliesByPost[post.id] || []).map((reply) => (
                    <div key={reply.id} className="club-reply">
                      <strong>{reply.author?.username}</strong>
                      <p>{reply.content}</p>
                    </div>
                  ))}
                  {canPost && (
                    <div className="club-reply-form">
                      <input
                        value={replyDrafts[post.id] || ''}
                        onChange={(e) => setReplyDrafts((prev) => ({ ...prev, [post.id]: e.target.value }))}
                        placeholder="Write a reply"
                        aria-label={`Reply to ${post.author?.username || 'post'}`}
                      />
                      <button type="button" className="ghost" onClick={() => handleReply(post.id)}>Reply</button>
                    </div>
                  )}
                </motion.article>
              ))}
            </div>
          </section>

          <aside className="club-rail">
            <section className="club-panel club-panel--people">
              <header className="club-people__head">
                <h3>Members</h3>
                <span className="club-people__count">{members.length}</span>
              </header>
              <ul className="club-member-list">
                {members.map((member) => {
                  const name = member.user?.username || 'Member'
                  const initial = name.trim().charAt(0).toUpperCase()
                  const roleLabel =
                    member.role === 'OWNER'
                      ? 'Owner'
                      : member.role === 'MODERATOR'
                        ? 'Moderator'
                        : 'Member'
                  const isSelf = member.user?.id === user?.userId
                  return (
                    <li key={member.userId || member.user?.id} className="club-member">
                      <div className="club-member__main">
                        <span className="club-member__avatar" aria-hidden="true">
                          {member.user?.avatarUrl ? (
                            <img src={resolveMediaUrl(member.user.avatarUrl, '/user-stub.png')} alt="" />
                          ) : (
                            initial
                          )}
                        </span>
                        <div className="club-member__meta">
                          <p className="club-member__name">
                            {name}
                            {isSelf && <span className="club-member__you">You</span>}
                          </p>
                          <p className={`club-member__role club-member__role--${(member.role || 'MEMBER').toLowerCase()}`}>
                            {roleLabel}
                          </p>
                        </div>
                      </div>
                      {isOwner && !isSelf && member.role !== 'OWNER' && (
                        <div className="club-member-actions">
                          {member.role === 'MEMBER' && (
                            <button type="button" className="ghost" onClick={async () => { await promoteMember(clubId, member.user.id); await refresh() }}>
                              Promote
                            </button>
                          )}
                          {member.role === 'MODERATOR' && (
                            <button type="button" className="ghost" onClick={async () => { await demoteMember(clubId, member.user.id); await refresh() }}>
                              Demote
                            </button>
                          )}
                          <button type="button" className="ghost" onClick={async () => { await transferOwnership(clubId, member.user.id); await refresh() }}>
                            Make owner
                          </button>
                          <button type="button" className="ghost" onClick={async () => { await removeMember(clubId, member.user.id); await refresh() }}>
                            Remove
                          </button>
                        </div>
                      )}
                      {isMod && !isOwner && member.role === 'MEMBER' && !isSelf && (
                        <div className="club-member-actions">
                          <button type="button" className="ghost" onClick={async () => { await removeMember(clubId, member.user.id); await refresh() }}>
                            Remove
                          </button>
                        </div>
                      )}
                    </li>
                  )
                })}
              </ul>

              {isMod && (
                <div className="club-pending">
                  <header className="club-people__head">
                    <h3>Pending requests</h3>
                    {pending.length > 0 && (
                      <span className="club-people__count club-people__count--alert">{pending.length}</span>
                    )}
                  </header>
                  {pending.length === 0 ? (
                    <p className="club-pending__empty">No one is waiting for approval.</p>
                  ) : (
                    <ul className="club-member-list">
                      {pending.map((member) => {
                        const name = member.user?.username || 'Reader'
                        const initial = name.trim().charAt(0).toUpperCase()
                        return (
                          <li key={member.user?.id} className="club-member club-member--pending">
                            <div className="club-member__main">
                              <span className="club-member__avatar" aria-hidden="true">{initial}</span>
                              <div className="club-member__meta">
                                <p className="club-member__name">{name}</p>
                                <p className="club-member__role">Requested access</p>
                              </div>
                            </div>
                            <div className="club-member-actions">
                              <button type="button" className="clubs-primary" onClick={async () => { await approveMember(clubId, member.user.id); await refresh() }}>
                                Approve
                              </button>
                              <button type="button" className="ghost" onClick={async () => { await rejectMember(clubId, member.user.id); await refresh() }}>
                                Reject
                              </button>
                            </div>
                          </li>
                        )
                      })}
                    </ul>
                  )}
                </div>
              )}
            </section>
          </aside>
        </div>
      </div>

      {editOpen && (
        <div
          className="clubs-modal"
          role="dialog"
          aria-modal="true"
          aria-labelledby="edit-club-title"
          onClick={(event) => {
            if (event.target === event.currentTarget) setEditOpen(false)
          }}
        >
          <form className="clubs-modal__panel" onSubmit={handleSaveEdit}>
            <h2 id="edit-club-title">Edit club</h2>
            <label>
              Name
              <input value={editForm.name} onChange={(e) => setEditForm((p) => ({ ...p, name: e.target.value }))} required />
            </label>
            <label>
              Focus
              <input value={editForm.focus} onChange={(e) => setEditForm((p) => ({ ...p, focus: e.target.value }))} />
            </label>
            <label>
              Description
              <textarea rows={4} value={editForm.description} onChange={(e) => setEditForm((p) => ({ ...p, description: e.target.value }))} />
            </label>
            <label className="clubs-check">
              <input
                type="checkbox"
                checked={editForm.isPrivate}
                onChange={(e) => setEditForm((p) => ({ ...p, isPrivate: e.target.checked }))}
              />
              Private
            </label>
            <div className="clubs-modal__actions">
              <button type="button" className="ghost" onClick={() => setEditOpen(false)}>Cancel</button>
              <button type="submit" className="clubs-primary">Save</button>
            </div>
          </form>
        </div>
      )}
    </AppChrome>
  )
}

export default ClubDetailPage
