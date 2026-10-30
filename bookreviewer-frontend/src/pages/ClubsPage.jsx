import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import AppChrome from '../components/layout/AppChrome.jsx'
import {
  acceptInvite,
  createClub,
  declineInvite,
  getMyClubs,
  getMyInvitations,
  getPublicClubs,
} from '../services/clubService.js'
import { resolveMediaUrl } from '../utils/media.js'
import './ClubsPage.css'

const ClubsPage = () => {
  const navigate = useNavigate()
  const [tab, setTab] = useState('discover')
  const [clubs, setClubs] = useState([])
  const [invitations, setInvitations] = useState([])
  const [query, setQuery] = useState('')
  const [appliedQuery, setAppliedQuery] = useState('')
  const [loading, setLoading] = useState(true)
  const [inviteBusyId, setInviteBusyId] = useState(null)
  const [error, setError] = useState('')
  const [showCreate, setShowCreate] = useState(false)
  const [saving, setSaving] = useState(false)
  const [form, setForm] = useState({
    name: '',
    description: '',
    focus: '',
    isPrivate: false,
  })

  const loadInvitations = async () => {
    try {
      setInvitations(await getMyInvitations())
    } catch {
      setInvitations([])
    }
  }

  const load = async (searchQuery = appliedQuery) => {
    setLoading(true)
    setError('')
    try {
      const data =
        tab === 'mine'
          ? await getMyClubs({ q: searchQuery })
          : await getPublicClubs({ q: searchQuery })
      setClubs(data?.content || data || [])
      await loadInvitations()
    } catch (err) {
      setError(err?.response?.data?.message || 'Could not load clubs.')
      setClubs([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load(appliedQuery)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tab, appliedQuery])

  useEffect(() => {
    const trimmed = query.trim()
    const timer = setTimeout(() => {
      setAppliedQuery(trimmed)
    }, 300)
    return () => clearTimeout(timer)
  }, [query])

  const handleCreate = async (event) => {
    event.preventDefault()
    if (!form.name.trim()) return
    setSaving(true)
    try {
      const created = await createClub({
        name: form.name.trim(),
        description: form.description.trim() || undefined,
        focus: form.focus.trim() || undefined,
        isPrivate: form.isPrivate,
      })
      setShowCreate(false)
      setForm({ name: '', description: '', focus: '', isPrivate: false })
      navigate(`/clubs/${created.id}`)
    } catch (err) {
      setError(err?.response?.data?.message || 'Could not create club.')
    } finally {
      setSaving(false)
    }
  }

  const handleAcceptInvite = async (clubId) => {
    setInviteBusyId(clubId)
    try {
      await acceptInvite(clubId)
      await load()
      navigate(`/clubs/${clubId}`)
    } catch (err) {
      setError(err?.response?.data?.message || 'Could not accept invite.')
    } finally {
      setInviteBusyId(null)
    }
  }

  const handleDeclineInvite = async (clubId) => {
    setInviteBusyId(clubId)
    try {
      await declineInvite(clubId)
      await loadInvitations()
    } catch (err) {
      setError(err?.response?.data?.message || 'Could not decline invite.')
    } finally {
      setInviteBusyId(null)
    }
  }

  const emptyMessage = appliedQuery
    ? `No clubs match “${appliedQuery}”.`
    : tab === 'mine'
      ? 'Your membership shelf is empty. Discover a club or create your own.'
      : 'No public clubs yet — be the first host.'

  return (
    <AppChrome className="clubs-shell">
      <div className="clubs-content">
        <section className="clubs-hero motion-surface">
          <div className="clubs-hero__copy">
            <p className="clubs-kicker">Shared shelves</p>
            <h2>
              Reading
              <br />
              <span>Clubs</span>
            </h2>
            <p>
              Gather around one book at a time — request private rooms, follow the current title,
              and keep the conversation on the page.
            </p>
            <button type="button" className="clubs-primary" onClick={() => setShowCreate(true)}>
              Start a club
            </button>
          </div>
          <div className="clubs-hero__art" aria-hidden="true">
            <div className="clubs-stack">
              <span className="clubs-spine clubs-spine--a" />
              <span className="clubs-spine clubs-spine--b" />
              <span className="clubs-spine clubs-spine--c" />
            </div>
          </div>
        </section>

        {invitations.length > 0 && (
          <section className="clubs-invites" aria-labelledby="club-invites-title">
            <div className="clubs-invites__head">
              <h3 id="club-invites-title">Invitations</h3>
              <span className="clubs-invites__count">{invitations.length}</span>
            </div>
            <ul className="clubs-invites__list">
              {invitations.map((club) => (
                <li key={club.id} className="clubs-invite-card">
                  <div>
                    <p className="clubs-invite-card__name">{club.name}</p>
                    {club.focus && <p className="clubs-invite-card__focus">{club.focus}</p>}
                    <p className="clubs-invite-card__meta">
                      Private club · {club.memberCount ?? 0} members
                    </p>
                  </div>
                  <div className="clubs-invite-card__actions">
                    <button
                      type="button"
                      className="clubs-primary"
                      disabled={inviteBusyId === club.id}
                      onClick={() => handleAcceptInvite(club.id)}
                    >
                      Accept
                    </button>
                    <button
                      type="button"
                      className="ghost"
                      disabled={inviteBusyId === club.id}
                      onClick={() => handleDeclineInvite(club.id)}
                    >
                      Decline
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          </section>
        )}

        <div className="clubs-toolbar">
          <div className="clubs-tabs" role="tablist" aria-label="Club lists">
            <button
              type="button"
              role="tab"
              aria-selected={tab === 'discover'}
              className={tab === 'discover' ? 'is-active' : ''}
              onClick={() => setTab('discover')}
            >
              Discover
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={tab === 'mine'}
              className={tab === 'mine' ? 'is-active' : ''}
              onClick={() => setTab('mine')}
            >
              My clubs
            </button>
          </div>
          <label className="clubs-search">
            <span className="visually-hidden">Search clubs</span>
            <input
              type="search"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder={tab === 'mine' ? 'Search my clubs…' : 'Search public clubs…'}
              aria-label={tab === 'mine' ? 'Search my clubs' : 'Search public clubs'}
            />
          </label>
        </div>

        {error && <p className="clubs-error" role="alert">{error}</p>}

        {loading && (
          <div className="clubs-skeleton" aria-hidden="true">
            {Array.from({ length: 3 }).map((_, index) => (
              <div key={index} className="clubs-skeleton__card" />
            ))}
          </div>
        )}

        {!loading && clubs.length === 0 && (
          <div className="clubs-empty">
            <p>{emptyMessage}</p>
            {!appliedQuery && (
              <button type="button" className="clubs-primary" onClick={() => setShowCreate(true)}>
                Create club
              </button>
            )}
          </div>
        )}

        <div className="clubs-grid">
          {clubs.map((club, index) => {
            const initial = (club.name || '?').trim().charAt(0).toUpperCase()
            return (
              <motion.article
                key={club.id}
                className={`club-card club-card--tone-${index % 3}`}
                onClick={() => navigate(`/clubs/${club.id}`)}
                initial={{ opacity: 0, y: 16 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.35, delay: index * 0.05 }}
                whileHover={{ y: -4 }}
              >
                <div className="club-card__band">
                  <span className="club-card__mono" aria-hidden="true">{initial}</span>
                  {club.isPrivate && <span className="club-badge">Private</span>}
                </div>
                <div className="club-card__body">
                  <h3>{club.name}</h3>
                  {club.focus && <p className="club-card__focus">{club.focus}</p>}
                  <p className="club-card__desc">
                    {club.description || 'A quiet room for long books and longer notes.'}
                  </p>
                  <div className="club-card__meta">
                    <span>{club.memberCount ?? 0} members</span>
                    {club.currentBook?.title && (
                      <span className="club-card__reading">
                        <img
                          src={resolveMediaUrl(club.currentBook.coverUrl, '/home-book.jpg')}
                          alt=""
                        />
                        {club.currentBook.title}
                      </span>
                    )}
                  </div>
                </div>
              </motion.article>
            )
          })}
        </div>
      </div>

      {showCreate && (
        <div
          className="clubs-modal"
          role="dialog"
          aria-modal="true"
          aria-labelledby="create-club-title"
          onClick={(event) => {
            if (event.target === event.currentTarget) setShowCreate(false)
          }}
        >
          <form className="clubs-modal__panel" onSubmit={handleCreate}>
            <h2 id="create-club-title">Create a club</h2>
            <p className="clubs-modal__lead">Name the room, set a focus, and decide who may enter.</p>
            <label>
              Name
              <input
                value={form.name}
                onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
                required
                maxLength={120}
              />
            </label>
            <label>
              Focus
              <input
                value={form.focus}
                onChange={(e) => setForm((prev) => ({ ...prev, focus: e.target.value }))}
                placeholder="e.g. Classics, Sci-fi"
              />
            </label>
            <label>
              Description
              <textarea
                value={form.description}
                onChange={(e) => setForm((prev) => ({ ...prev, description: e.target.value }))}
                rows={4}
              />
            </label>
            <label className="clubs-check">
              <input
                type="checkbox"
                checked={form.isPrivate}
                onChange={(e) => setForm((prev) => ({ ...prev, isPrivate: e.target.checked }))}
              />
              Private (members join by invite or approved request)
            </label>
            <div className="clubs-modal__actions">
              <button type="button" className="ghost" onClick={() => setShowCreate(false)}>
                Cancel
              </button>
              <button type="submit" className="clubs-primary" disabled={saving}>
                {saving ? 'Creating…' : 'Create'}
              </button>
            </div>
          </form>
        </div>
      )}
    </AppChrome>
  )
}

export default ClubsPage
