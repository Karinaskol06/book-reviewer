import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import AppChrome from '../components/layout/AppChrome.jsx'
import { createClub, getMyClubs, getPublicClubs } from '../services/clubService.js'
import { resolveMediaUrl } from '../utils/media.js'
import './ClubsPage.css'

const ClubsPage = () => {
  const navigate = useNavigate()
  const [tab, setTab] = useState('discover')
  const [clubs, setClubs] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showCreate, setShowCreate] = useState(false)
  const [saving, setSaving] = useState(false)
  const [form, setForm] = useState({
    name: '',
    description: '',
    focus: '',
    isPrivate: false,
  })

  const load = async () => {
    setLoading(true)
    setError('')
    try {
      const data = tab === 'mine' ? await getMyClubs() : await getPublicClubs()
      setClubs(data?.content || data || [])
    } catch (err) {
      setError(err?.response?.data?.message || 'Could not load clubs.')
      setClubs([])
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [tab])

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
          <p className="clubs-toolbar__hint">
            {tab === 'discover' ? 'Public clubs open to every reader' : 'Clubs you own or joined'}
          </p>
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
            <p>
              {tab === 'mine'
                ? 'Your membership shelf is empty. Discover a club or create your own.'
                : 'No public clubs yet — be the first host.'}
            </p>
            <button type="button" className="clubs-primary" onClick={() => setShowCreate(true)}>
              Create club
            </button>
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
              Private (join requests need approval)
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
