import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth.js'
import { useDebounce } from '../hooks/useDebounce.js'
import { getGenres } from '../services/homeService.js'
import { checkBookDuplicate, createBook, getBookDetail } from '../services/bookService.js'
import './AddBookPage.css'

const AddBookPage = () => {
  const navigate = useNavigate()
  const { user, logout } = useAuth()
  const [searchParams] = useSearchParams()
  const fileInputRef = useRef(null)

  const [headerSearch, setHeaderSearch] = useState('')
  const [availableGenres, setAvailableGenres] = useState([])
  const [customGenre, setCustomGenre] = useState('')
  const [duplicateInfo, setDuplicateInfo] = useState(null)
  const [duplicateBook, setDuplicateBook] = useState(null)
  const [isDragging, setIsDragging] = useState(false)

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
    const query = headerSearch.trim()
    if (!query) return
    const timer = setTimeout(() => {
      navigate(`/search?query=${encodeURIComponent(query)}&page=0`)
    }, 350)
    return () => clearTimeout(timer)
  }, [headerSearch, navigate])

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
    return true
  }, [duplicateInfo, form.author, form.title])

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
    setCoverPreview(normalized)
  }

  const handleFile = (file) => {
    if (!file) return
    const reader = new FileReader()
    reader.onload = () => {
      const dataUrl = String(reader.result || '')
      applyCoverValue(dataUrl)
    }
    reader.readAsDataURL(file)
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
    <main className="dashboard">
      <header className="home-nav">
        <h1>BookReviewer</h1>
        <nav>
          <Link to="/dashboard">Home</Link>
          <Link to="/dashboard#trending">Library</Link>
          <Link to="/books/new">Add Book</Link>
          <Link to="/dashboard#collections">Collections</Link>
          <Link to="/dashboard#community">Journal</Link>
        </nav>
        <input
          className="home-nav__search"
          type="search"
          placeholder="Search the archive..."
          value={headerSearch}
          onChange={(e) => setHeaderSearch(e.target.value)}
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
                value={form.coverUrl}
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
              <p>Drag and drop high-resolution scans, or <button type="button" onClick={() => fileInputRef.current?.click()}>browse archive</button></p>
              {coverPreview && <img src={coverPreview} alt="Cover preview" />}
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
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
                  src={duplicateBook?.coverUrl || '/home-book.jpg'}
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

      <footer className="home-footer">
        <h2>BookReviewer</h2>
        <nav>
          <Link to="/dashboard#trending">Library</Link>
          <Link to="/dashboard#collections">Collections</Link>
          <Link to="/feed">Feed</Link>
        </nav>
        <p>© 2026 BookReviewer. The Digital Archivist.</p>
      </footer>
    </main>
  )
}

export default AddBookPage
