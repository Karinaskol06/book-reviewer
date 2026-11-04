import { useEffect, useId, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { searchBooks } from '../../services/homeService.js'
import { searchUsersByUsername } from '../../services/userService.js'
import { resolveMediaUrl } from '../../utils/media.js'
import {
  clearRecentSearches,
  collectionPath,
  highlightParts,
  matchGenres,
  readRecentSearches,
  rememberSearch,
} from '../../utils/search.js'
import './SearchSuggest.css'

const DEBOUNCE_MS = 250
const BOOK_LIMIT = 5
const READER_LIMIT = 3
const READER_MIN_CHARS = 2

const Icon = ({ name }) => (
  <svg
    className="search-suggest__icon"
    viewBox="0 0 24 24"
    width="18"
    height="18"
    fill="none"
    stroke="currentColor"
    strokeWidth="1.8"
    strokeLinecap="round"
    strokeLinejoin="round"
    aria-hidden="true"
    focusable="false"
  >
    {name === 'search' && (
      <>
        <circle cx="11" cy="11" r="6.5" />
        <path d="m20 20-4.4-4.4" />
      </>
    )}
    {name === 'clock' && (
      <>
        <circle cx="12" cy="12" r="8.5" />
        <path d="M12 7.5V12l3 2" />
      </>
    )}
    {name === 'shelf' && <path d="M5 4.5v15M9.5 4.5v15M14 5.5l4.5 13.5M3.5 19.5h17" />}
    {name === 'plus' && <path d="M12 5v14M5 12h14" />}
    {name === 'arrow' && <path d="M5 12h14M13 6l6 6-6 6" />}
  </svg>
)

const Highlight = ({ text, query }) =>
  highlightParts(text, query).map((part, index) =>
    part.match ? <mark key={index}>{part.text}</mark> : <span key={index}>{part.text}</span>,
  )

/**
 * Archive search with live suggestions (WAI-ARIA combobox): books, collections and readers as you type,
 * recent searches and popular collections before you type. Enter with nothing picked opens the Library.
 */
export default function SearchSuggest({ genres = [], popularGenres = [], placeholder = 'Search by title, author or genre…' }) {
  const navigate = useNavigate()
  const listId = useId()
  const rootRef = useRef(null)
  const inputRef = useRef(null)
  const requestRef = useRef(0)
  const [query, setQuery] = useState('')
  const [open, setOpen] = useState(false)
  const [activeIndex, setActiveIndex] = useState(-1)
  const [results, setResults] = useState({ query: '', books: [], readers: [] })
  const [recent, setRecent] = useState(readRecentSearches)

  const trimmed = query.trim()
  const fresh = trimmed !== '' && results.query === trimmed
  const loading = trimmed !== '' && !fresh

  // Fetch suggestions shortly after typing stops; answers to older queries are dropped.
  useEffect(() => {
    if (!trimmed) return undefined
    const timer = setTimeout(async () => {
      const requestId = ++requestRef.current
      const [books, readers] = await Promise.allSettled([
        searchBooks(trimmed, BOOK_LIMIT),
        trimmed.length >= READER_MIN_CHARS ? searchUsersByUsername(trimmed, READER_LIMIT) : Promise.resolve([]),
      ])
      if (requestId !== requestRef.current) return
      setResults({
        query: trimmed,
        books: books.status === 'fulfilled' && Array.isArray(books.value) ? books.value : [],
        readers: readers.status === 'fulfilled' && Array.isArray(readers.value) ? readers.value.slice(0, READER_LIMIT) : [],
      })
    }, DEBOUNCE_MS)
    return () => clearTimeout(timer)
  }, [trimmed])

  useEffect(() => {
    if (!open) return undefined
    const onPointerDown = (event) => {
      if (!rootRef.current?.contains(event.target)) setOpen(false)
    }
    document.addEventListener('pointerdown', onPointerDown)
    return () => document.removeEventListener('pointerdown', onPointerDown)
  }, [open])

  const go = (path) => {
    rememberSearch(trimmed)
    setRecent(readRecentSearches())
    setOpen(false)
    setActiveIndex(-1)
    navigate(path)
  }

  const searchAll = () => go(`/search?query=${encodeURIComponent(trimmed)}&page=0`)

  const changeQuery = (value) => {
    setQuery(value)
    setActiveIndex(-1)
    setOpen(true)
  }

  // ---- Build the option groups shown in the panel ----
  const groups = []
  if (trimmed) {
    // While a new answer is loading, keep showing the previous one so the panel doesn't jump.
    const books = results.books
    const collections = matchGenres(genres, trimmed, 3)
    if (books.length) {
      groups.push({
        label: 'Books',
        layout: 'list',
        items: books.map((book) => ({
          key: `book-${book.id}`,
          label: [book.title, book.author, book.publicationYear].filter(Boolean).join(', '),
          select: () => go(`/books/${book.id}`),
          content: (
            <>
              <img className="search-suggest__cover" src={resolveMediaUrl(book.coverUrl, '/home-book.jpg')} alt="" />
              <span className="search-suggest__text">
                <span className="search-suggest__title">
                  <Highlight text={book.title} query={trimmed} />
                </span>
                <span className="search-suggest__meta">
                  <Highlight text={[book.author, book.publicationYear].filter(Boolean).join(' · ')} query={trimmed} />
                </span>
              </span>
            </>
          ),
        })),
      })
    }
    if (collections.length) {
      groups.push({
        label: 'Collections',
        layout: 'list',
        items: collections.map((genre) => ({
          key: `genre-${genre}`,
          label: `${genre} collection`,
          select: () => go(collectionPath(genre)),
          content: (
            <>
              <span className="search-suggest__badge"><Icon name="shelf" /></span>
              <span className="search-suggest__text">
                <span className="search-suggest__title">
                  <Highlight text={genre} query={trimmed} />
                </span>
                <span className="search-suggest__meta">Collection</span>
              </span>
            </>
          ),
        })),
      })
    }
    if (results.readers.length) {
      groups.push({
        label: 'Readers',
        layout: 'list',
        items: results.readers.map((reader) => ({
          key: `reader-${reader.id}`,
          label: `@${reader.username}, reader`,
          select: () => go(`/users/${reader.id}`),
          content: (
            <>
              <img className="search-suggest__avatar" src={resolveMediaUrl(reader.avatarUrl, '/user-stub.png')} alt="" />
              <span className="search-suggest__text">
                <span className="search-suggest__title">
                  @<Highlight text={reader.username} query={trimmed} />
                </span>
                <span className="search-suggest__meta">Reader</span>
              </span>
            </>
          ),
        })),
      })
    }
    const actions = [
      {
        key: 'all',
        select: searchAll,
        content: (
          <>
            <span className="search-suggest__badge"><Icon name="search" /></span>
            <span className="search-suggest__title">See all results for “{trimmed}”</span>
            <Icon name="arrow" />
          </>
        ),
      },
    ]
    if (fresh && books.length === 0) {
      actions.push({
        key: 'add',
        select: () => go(`/books/new?query=${encodeURIComponent(trimmed)}`),
        content: (
          <>
            <span className="search-suggest__badge"><Icon name="plus" /></span>
            <span className="search-suggest__title">Add “{trimmed}” to the archive</span>
          </>
        ),
      })
    }
    groups.push({ label: null, layout: 'list', items: actions, className: 'search-suggest__group--actions' })
  } else {
    if (recent.length) {
      groups.push({
        label: 'Recent searches',
        layout: 'list',
        items: [
          ...recent.map((term) => ({
            key: `recent-${term}`,
            select: () => {
              changeQuery(term)
              inputRef.current?.focus()
            },
            content: (
              <>
                <span className="search-suggest__badge search-suggest__badge--plain"><Icon name="clock" /></span>
                <span className="search-suggest__title">{term}</span>
              </>
            ),
          })),
          {
            key: 'recent-clear',
            className: 'search-suggest__option--quiet',
            select: () => {
              clearRecentSearches()
              setRecent([])
              setActiveIndex(-1)
            },
            content: <span className="search-suggest__meta">Clear recent searches</span>,
          },
        ],
      })
    }
    if (popularGenres.length) {
      groups.push({
        label: 'Browse a collection',
        layout: 'chips',
        items: [
          ...popularGenres.map((genre) => ({
            key: `popular-${genre}`,
            select: () => go(collectionPath(genre)),
            content: genre,
          })),
          { key: 'popular-all', select: () => go('/collections'), content: 'All collections →' },
        ],
      })
    }
  }

  const options = groups.flatMap((group) => group.items)
  const active = activeIndex >= 0 && activeIndex < options.length ? activeIndex : -1
  const optionId = (index) => `${listId}-option-${index}`
  const expanded = open && options.length > 0

  const onKeyDown = (event) => {
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault()
      if (!open) {
        setOpen(true)
        return
      }
      if (!options.length) return
      const step = event.key === 'ArrowDown' ? 1 : -1
      setActiveIndex(active < 0 ? (step > 0 ? 0 : options.length - 1) : (active + step + options.length) % options.length)
    } else if (event.key === 'Enter') {
      event.preventDefault()
      if (expanded && active >= 0) options[active].select()
      else if (trimmed) searchAll()
    } else if (event.key === 'Escape') {
      if (expanded) {
        event.preventDefault()
        setOpen(false)
        setActiveIndex(-1)
      }
    } else if (event.key === 'Tab') {
      setOpen(false)
    }
  }

  let status = ''
  if (trimmed && loading && !results.books.length) status = 'Searching…'
  else if (fresh) {
    const counts = groups.filter((group) => group.label).map((group) => `${group.items.length} ${group.label.toLowerCase()}`)
    status = counts.length ? `Suggestions: ${counts.join(', ')}.` : `No suggestions for ${trimmed}.`
  }

  let index = -1
  return (
    <div className="search-suggest" ref={rootRef}>
      <div className="search-suggest__field" data-loading={loading}>
        <Icon name="search" />
        <input
          ref={inputRef}
          className="search-suggest__input"
          type="search"
          role="combobox"
          aria-label="Search the archive"
          aria-autocomplete="list"
          aria-expanded={expanded}
          aria-controls={listId}
          aria-activedescendant={expanded && active >= 0 ? optionId(active) : undefined}
          autoComplete="off"
          enterKeyHint="search"
          placeholder={placeholder}
          value={query}
          onChange={(event) => changeQuery(event.target.value)}
          onFocus={() => setOpen(true)}
          onKeyDown={onKeyDown}
        />
        <span className="search-suggest__spinner" aria-hidden="true" />
      </div>
      <p className="search-suggest__status" role="status" aria-live="polite">
        {status}
      </p>

      {expanded && (
        <div className="search-suggest__panel">
          {trimmed && fresh && results.books.length === 0 && (
            <p className="search-suggest__empty">
              No books match “{trimmed}”. Try another spelling, or add it below.
            </p>
          )}
          <ul id={listId} className="search-suggest__list" role="listbox" aria-label="Search suggestions">
            {groups.map((group, groupIndex) => {
              const labelId = `${listId}-group-${groupIndex}`
              return (
                <li key={group.label || `group-${groupIndex}`} role="presentation" className={`search-suggest__group ${group.className || ''}`}>
                  {group.label && (
                    <p id={labelId} className="search-suggest__group-label">
                      {group.label}
                    </p>
                  )}
                  <ul
                    role="group"
                    aria-labelledby={group.label ? labelId : undefined}
                    aria-label={group.label ? undefined : 'More'}
                    className={`search-suggest__options search-suggest__options--${group.layout}`}
                  >
                    {group.items.map((item) => {
                      index += 1
                      const itemIndex = index
                      return (
                        <li
                          key={item.key}
                          id={optionId(itemIndex)}
                          role="option"
                          aria-selected={itemIndex === active}
                          aria-label={item.label}
                          className={`search-suggest__option ${item.className || ''}`}
                          onMouseDown={(event) => event.preventDefault()}
                          onMouseMove={() => itemIndex !== active && setActiveIndex(itemIndex)}
                          onClick={item.select}
                        >
                          {item.content}
                        </li>
                      )
                    })}
                  </ul>
                </li>
              )
            })}
          </ul>
          <p className="search-suggest__hint" aria-hidden="true">
            <kbd>↑</kbd> <kbd>↓</kbd> to move · <kbd>Enter</kbd> to open · <kbd>Esc</kbd> to close
          </p>
        </div>
      )}
    </div>
  )
}
