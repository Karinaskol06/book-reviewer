import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import AppChrome from '../components/layout/AppChrome.jsx'
import { useDebounce } from '../hooks/useDebounce.js'
import { getGenres } from '../services/homeService.js'
import { checkBookDuplicate, createBook, getBookDetail, uploadBookCover } from '../services/bookService.js'
import { resolveMediaUrl } from '../utils/media.js'
import './AddBookPage.css'

const AddBookPage = () => {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const fileInputRef = useRef(null)

  const [availableGenres, setAvailableGenres] = useState([])
  const [customGenre, setCustomGenre] = useState('')
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
      setAvailableGenres(list)
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

  const updateForm = (key, value) => setForm((prev) => ({ ...prev, [key]: value }))

  const toggleGenre = (genre) => {
    setForm((prev) => ({
      ...prev,
      genres: prev.genres.includes(genre)
        ? prev.genres.filter((item) => item !== genre)
        : [...prev.genres, genre],
    }))
  }

  const handleAddCustomGenre = () => {
    const value = customGenre.trim()
    if (!value) return
    if (!form.genres.includes(value)) {
      setForm((prev) => ({ ...prev, genres: [...prev.genres, value] }))
    }
    setCustomGenre('')
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

  return (
    <AppChrome>
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
              <div className="genre-chips">
                {availableGenres.map((genre) => (
                  <button
                    key={genre}
                    type="button"
                    className={form.genres.includes(genre) ? 'active' : ''}
                    onClick={() => toggleGenre(genre)}
                  >
                    {genre}
                  </button>
                ))}
                <input
                  value={customGenre}
                  placeholder="+ Add genre"
                  onChange={(e) => setCustomGenre(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') {
                      e.preventDefault()
                      handleAddCustomGenre()
                    }
                  }}
                />
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
                A library is not just a collection of books, but a sanctuary of human thought. Precision
                in your entries ensures that future scholars can trace the lineage of every story.
              </p>
            </section>
          )}
        </aside>
      </div>
    </AppChrome>
  )
}

export default AddBookPage
