import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import AppChrome from '../components/layout/AppChrome.jsx'
import { useDebounce } from '../hooks/useDebounce.js'
import { getGenres } from '../services/homeService.js'
import { checkBookDuplicate, createBook, getBookDetail, uploadBookCover } from '../services/bookService.js'
import { resolveMediaUrl } from '../utils/media.js'
import { findGenreByKey, genreKey, toGenreLabel } from '../utils/genre.js'
import './AddBookPage.css'

const AddBookPage = () => {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const fileInputRef = useRef(null)

  const [availableGenres, setAvailableGenres] = useState([])
  const [genreQuery, setGenreQuery] = useState('')
  const [genreFeedback, setGenreFeedback] = useState({ tone: '', text: '' })
  const [duplicateInfo, setDuplicateInfo] = useState(null)
  const [duplicateBook, setDuplicateBook] = useState(null)
  const [isDragging, setIsDragging] = useState(false)
  const [coverUploading, setCoverUploading] = useState(false)
  const [coverError, setCoverError] = useState('')

  const initialQuery = searchParams.get('query') || ''
  const [form, setForm] = useState({
    title: initialQuery,
    author: '',
    publicationYear: '',
    description: '',
    genres: [],
    coverUrl: '',
  })
  const [coverPreview, setCoverPreview] = useState('')

  const debouncedTitle = useDebounce(form.title.trim(), 400)
  const debouncedAuthor = useDebounce(form.author.trim(), 400)

  useEffect(() => {
    const loadGenres = async () => {
      const list = await getGenres()
      setAvailableGenres(Array.isArray(list) ? list : [])
    }
    loadGenres()
  }, [])

  useEffect(() => {
    const runDuplicateCheck = async () => {
      if (!debouncedTitle || !debouncedAuthor) {
        setDuplicateInfo(null)
        return
      }
      try {
        const result = await checkBookDuplicate(debouncedTitle, debouncedAuthor)
        setDuplicateInfo(result.exists ? result : null)
      } catch {
        setDuplicateInfo(null)
      }
    }
    runDuplicateCheck()
  }, [debouncedAuthor, debouncedTitle])

  useEffect(() => {
    const loadDuplicateBook = async () => {
      if (!duplicateInfo?.bookId) {
        setDuplicateBook(null)
        return
      }
      try {
        const detail = await getBookDetail(duplicateInfo.bookId)
        setDuplicateBook(detail)
      } catch {
        setDuplicateBook(null)
      }
    }
    loadDuplicateBook()
  }, [duplicateInfo])

  const canSubmit = useMemo(() => {
    const required = form.title.trim() && form.author.trim()
    if (!required) return false
    if (duplicateInfo) return false
    if (coverUploading) return false
    return true
  }, [coverUploading, duplicateInfo, form.author, form.title])

  const filteredGenres = useMemo(() => {
    const query = genreKey(genreQuery)
    const list = [...availableGenres].sort((a, b) => a.localeCompare(b))
    if (!query) return list
    return list.filter((genre) => genreKey(genre).includes(query))
  }, [availableGenres, genreQuery])

  const exactGenreMatch = useMemo(() => findGenreByKey(availableGenres, genreQuery), [availableGenres, genreQuery])

  const updateForm = (key, value) => setForm((prev) => ({ ...prev, [key]: value }))

  const setGenreMessage = (tone, text) => setGenreFeedback({ tone, text })

  const addGenre = (genre) => {
    const label = toGenreLabel(genre)
    if (!label) return

    const alreadySelected = form.genres.some((item) => genreKey(item) === genreKey(label))
    if (alreadySelected) {
      const existingLabel = form.genres.find((item) => genreKey(item) === genreKey(label)) || label
      setGenreMessage('warn', `"${existingLabel}" is already on this book.`)
      return
    }

    setForm((prev) => ({ ...prev, genres: [...prev.genres, label] }))
    setGenreMessage('ok', `Added “${label}”.`)
  }

  const removeGenre = (genre) => {
    setForm((prev) => ({
      ...prev,
      genres: prev.genres.filter((item) => item !== genre),
    }))
    setGenreMessage('', '')
  }

  const toggleGenre = (genre) => {
    const selected = form.genres.some((item) => genreKey(item) === genreKey(genre))
    if (selected) {
      const matched = form.genres.find((item) => genreKey(item) === genreKey(genre))
      removeGenre(matched || genre)
      return
    }
    addGenre(genre)
  }

  const handleAddFromQuery = () => {
    const value = genreQuery.trim()
    if (!value) {
      setGenreMessage('warn', 'Type a genre name to search or add.')
      return
    }

    if (exactGenreMatch) {
      const alreadySelected = form.genres.some((item) => genreKey(item) === genreKey(exactGenreMatch))
      if (alreadySelected) {
        setGenreMessage('warn', `"${exactGenreMatch}" already exists and is already selected.`)
        return
      }
      addGenre(exactGenreMatch)
      setGenreQuery('')
      return
    }

    const label = toGenreLabel(value)
    if (!label) {
      setGenreMessage('warn', 'Enter a valid genre name.')
      return
    }

    setAvailableGenres((prev) => (findGenreByKey(prev, label) ? prev : [...prev, label]))
    addGenre(label)
    setGenreQuery('')
  }

  const applyCoverValue = (value) => {
    const normalized = String(value || '').trim()
    updateForm('coverUrl', normalized)
    setCoverPreview(normalized ? resolveMediaUrl(normalized, '') : '')
    setCoverError('')
  }

  const handleFile = async (file) => {
    if (!file) return
    const maxBytes = 10 * 1024 * 1024
    if (file.size > maxBytes) {
      setCoverError('Cover must be 10MB or smaller.')
      return
    }
    setCoverUploading(true)
    setCoverError('')
    try {
      const uploaded = await uploadBookCover(file)
      applyCoverValue(uploaded.coverUrl)
    } catch (err) {
      setCoverError(err?.response?.data?.message || 'Could not upload cover.')
    } finally {
      setCoverUploading(false)
      if (fileInputRef.current) fileInputRef.current.value = ''
    }
  }

  const onDrop = (event) => {
    event.preventDefault()
    setIsDragging(false)
    const file = event.dataTransfer.files?.[0]
    handleFile(file)
  }

  const onSubmit = async (event) => {
    event.preventDefault()
    if (!canSubmit) return

    const payload = {
      title: form.title.trim(),
      author: form.author.trim(),
      description: form.description.trim(),
      coverUrl: form.coverUrl || undefined,
      publicationYear: form.publicationYear ? Number(form.publicationYear) : undefined,
      genres: form.genres.length ? form.genres : undefined,
    }

    const created = await createBook(payload)
    navigate(`/books/${created.id}`)
  }

  const addButtonLabel = exactGenreMatch
    ? (form.genres.some((g) => genreKey(g) === genreKey(exactGenreMatch))
      ? 'Already added'
      : 'Add existing')
    : (genreQuery.trim() ? `Add “${toGenreLabel(genreQuery) || genreQuery.trim()}”` : 'Add genre')

  return (
    <AppChrome className="add-book-shell">
      <div className="home-content add-book-page">
        <section>
          <h2>Archival Submission</h2>
          <p className="intro">
            Add a new volume to the collective library. Please ensure the metadata matches the physical
            edition.
          </p>

          <form className="book-form" onSubmit={onSubmit}>
            <label>
              Book Title
              <input value={form.title} onChange={(e) => updateForm('title', e.target.value)} required />
            </label>

            <div className="two-col">
              <label>
                Primary Author
                <input value={form.author} onChange={(e) => updateForm('author', e.target.value)} required />
              </label>
              <label>
                Year of Publication
                <input
                  value={form.publicationYear}
                  onChange={(e) => updateForm('publicationYear', e.target.value)}
                  type="number"
                  min="1000"
                  max="2100"
                />
              </label>
            </div>

            <label>
              Archival Summary
              <textarea
                value={form.description}
                onChange={(e) => updateForm('description', e.target.value)}
                placeholder="Enter a brief summary of the work's historical or literary context..."
              />
            </label>

            <label>
              Cover Image URL (optional)
              <input
                type="url"
                value={form.coverUrl.startsWith('http') ? form.coverUrl : ''}
                onChange={(e) => applyCoverValue(e.target.value)}
                placeholder="https://example.com/cover.jpg"
              />
            </label>

            <div className="genres">
              <p>Taxonomy (Genres)</p>

              {form.genres.length > 0 && (
                <div className="genre-selected" aria-label="Selected genres">
                  {form.genres.map((genre) => (
                    <button
                      key={`selected-${genre}`}
                      type="button"
                      className="genre-selected__chip"
                      onClick={() => removeGenre(genre)}
                      title={`Remove ${genre}`}
                    >
                      {genre}
                      <span aria-hidden="true">×</span>
                    </button>
                  ))}
                </div>
              )}

              <div className="genre-add-row">
                <label className="genre-search-label">
                
                  <input
                    value={genreQuery}
                    placeholder="Start typing to filter genres…"
                    onChange={(e) => {
                      setGenreQuery(e.target.value)
                      setGenreFeedback({ tone: '', text: '' })
                    }}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        e.preventDefault()
                        handleAddFromQuery()
                      }
                    }}
                    aria-describedby="genre-feedback"
                  />
                </label>
                <button
                  type="button"
                  className="genre-add-btn"
                  onClick={handleAddFromQuery}
                  disabled={
                    !genreQuery.trim()
                    || (exactGenreMatch
                      && form.genres.some((g) => genreKey(g) === genreKey(exactGenreMatch)))
                  }
                >
                  {addButtonLabel}
                </button>
              </div>

              {genreFeedback.text && (
                <p
                  id="genre-feedback"
                  className={`genre-feedback genre-feedback--${genreFeedback.tone || 'ok'}`}
                  role="status"
                >
                  {genreFeedback.text}
                </p>
              )}

              {!genreFeedback.text && exactGenreMatch && (
                <p id="genre-feedback" className="genre-feedback genre-feedback--hint" role="status">
                  “{exactGenreMatch}” already exists in the archive
                  {form.genres.some((g) => genreKey(g) === genreKey(exactGenreMatch))
                    ? ' and is selected.'
                    : ' — click Add existing to attach it.'}
                </p>
              )}

              {!genreFeedback.text && genreQuery.trim() && !exactGenreMatch && toGenreLabel(genreQuery) && (
                <p id="genre-feedback" className="genre-feedback genre-feedback--hint" role="status">
                  No exact match. You can add “{toGenreLabel(genreQuery)}” as a new genre.
                </p>
              )}

              <div className="genre-chips" role="group" aria-label="Available genres">
                {filteredGenres.length === 0 ? (
                  <p className="genre-empty">No genres match that search.</p>
                ) : (
                  filteredGenres.map((genre) => (
                    <button
                      key={genre}
                      type="button"
                      className={form.genres.some((g) => genreKey(g) === genreKey(genre)) ? 'active' : ''}
                      onClick={() => toggleGenre(genre)}
                      aria-pressed={form.genres.some((g) => genreKey(g) === genreKey(genre))}
                    >
                      {genre}
                    </button>
                  ))
                )}
              </div>
            </div>

            <div
              className={`dropzone ${isDragging ? 'is-dragging' : ''}`}
              onDragOver={(e) => {
                e.preventDefault()
                setIsDragging(true)
              }}
              onDragLeave={() => setIsDragging(false)}
              onDrop={onDrop}
            >
              <p>
                Drag and drop a cover image, or{' '}
                <button type="button" onClick={() => fileInputRef.current?.click()}>browse</button>
              </p>
              {coverUploading && <p className="cover-hint">Uploading cover…</p>}
              {coverError && <p className="cover-hint cover-hint--error">{coverError}</p>}
              {coverPreview && !coverUploading && (
                <img src={coverPreview} alt="Cover preview" />
              )}
              <input
                ref={fileInputRef}
                type="file"
                accept="image/jpeg,image/png,image/webp,image/gif"
                hidden
                onChange={(e) => handleFile(e.target.files?.[0])}
              />
            </div>

            <div className="actions">
              <button type="submit" disabled={!canSubmit}>Commit to Archive</button>
              <button type="button" className="ghost" onClick={() => navigate(-1)}>Discard Draft</button>
            </div>
          </form>
        </section>

        <aside>
          {duplicateInfo ? (
            <section className="duplicate-box">
              <h3>Duplicate Entry Detected</h3>
              <p>Our archivists identified a potential match for this volume already residing in our collection.</p>
              <div className="duplicate-entry">
                <img
                  src={resolveMediaUrl(duplicateBook?.coverUrl, '/home-book.jpg')}
                  alt={duplicateInfo.title}
                />
                <div>
                  <p className="small">Existing record</p>
                  <strong>{duplicateInfo.title}</strong>
                  <p>
                    {duplicateInfo.author}
                    {duplicateBook?.publicationYear ? ` (${duplicateBook.publicationYear})` : ''}
                  </p>
                  <p className="duplicate-stats">
                    ★ {duplicateBook?.ratingStats?.average?.toFixed?.(1) || '0.0'}{' '}
                    <span>{duplicateBook?.ratingStats?.total || 0} readers</span>
                  </p>
                </div>
              </div>
              <button type="button" onClick={() => navigate(`/books/${duplicateInfo.bookId}`)}>Add Review Instead</button>
              <button type="button" className="ghost" onClick={() => navigate(`/books/${duplicateInfo.bookId}`)}>
                View Existing Book
              </button>
            </section>
          ) : (
            <section className="note-box">
              <h3>Archivist&apos;s Note</h3>
              <p className="quote">
                Accurate titles and authors keep reviews, shelves, and recommendations tied to the right
                book - so readers can find what you meant to share.
              </p>
            </section>
          )}
        </aside>
      </div>
    </AppChrome>
  )
}

export default AddBookPage
