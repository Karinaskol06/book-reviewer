import { useEffect, useId, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import {
  booksLabel,
  buildGenreRows,
  genreToneClass,
  leadingGenreNames,
  moreGenresLabel,
} from '../../utils/genreChart.js'

/** One bar per genre: name, track filled to the share of the shelf, "N books". */
function GenreRankList({ rows, label, scrollable = false }) {
  return (
    <ul
      className={`genre-rank${scrollable ? ' genre-rank--scroll' : ''}`}
      aria-label={label}
      tabIndex={scrollable ? 0 : undefined}
    >
      {rows.map((genre, idx) => (
        <li key={genre.name} className="genre-rank__row">
          <span className="genre-rank__meta">
            <span className="genre-rank__name">{genre.name}</span>
            <span className="genre-rank__track" aria-hidden="true">
              <span
                className={`genre-rank__fill ${genreToneClass(idx)}`}
                style={{ width: `${genre.fillPercent}%` }}
              />
            </span>
          </span>
          <span className="genre-rank__count">{genre.countLabel}</span>
        </li>
      ))}
    </ul>
  )
}

function GenreBreakdownDialog({ rows, sampleSize, ownerWord, onClose }) {
  const titleId = useId()
  const panelRef = useRef(null)
  const closeButtonRef = useRef(null)

  useEffect(() => {
    closeButtonRef.current?.focus()
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.body.style.overflow = previousOverflow
    }
  }, [])

  const handleKeyDown = (event) => {
    if (event.key === 'Escape') {
      event.stopPropagation()
      onClose()
      return
    }
    if (event.key !== 'Tab') return
    // Keep keyboard focus inside the dialog: wrap from the last focusable element to the first and back.
    const focusable = [...panelRef.current.querySelectorAll('button, [tabindex="0"]')]
    const first = focusable[0]
    const last = focusable[focusable.length - 1]
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault()
      last.focus()
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault()
      first.focus()
    }
  }

  return createPortal(
    <div className="genre-breakdown" onClick={onClose}>
      <div
        ref={panelRef}
        className="genre-breakdown__panel"
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        onClick={(event) => event.stopPropagation()}
        onKeyDown={handleKeyDown}
      >
        <div className="genre-breakdown__header">
          <h3 id={titleId}>All genres</h3>
          <button
            ref={closeButtonRef}
            type="button"
            className="genre-breakdown__close"
            onClick={onClose}
            aria-label="Close"
          >
            ×
          </button>
        </div>
        <p className="genre-breakdown__caption">
          {rows.length} genres, out of {booksLabel(sampleSize)} on {ownerWord} shelves.
        </p>
        <GenreRankList rows={rows} label="All genres by number of books" scrollable />
      </div>
    </div>,
    document.body,
  )
}

/**
 * "Top genres" chart for the profile taste card. A book can carry several genres, so each bar is the
 * share of the shelf that carries the genre; "+ N more genres" opens a dialog with every genre.
 */
export default function TopGenresChart({ tasteProfile, isOwn }) {
  const [showAll, setShowAll] = useState(false)
  const moreButtonRef = useRef(null)

  const sampleSize = Number(tasteProfile?.sampleSize) || 0
  const rows = buildGenreRows(tasteProfile?.topGenres, sampleSize)
  const allRows = buildGenreRows(tasteProfile?.allGenres, sampleSize)
  if (rows.length === 0) return null

  const leadingGenres = leadingGenreNames(rows)
  const moreLabel = moreGenresLabel(allRows.length - rows.length)
  const ownerWord = isOwn ? 'your' : 'their'

  const closeAll = () => {
    setShowAll(false)
    moreButtonRef.current?.focus()
  }

  return (
    <div className="genre-chart">
      <p className="taste-headline">
        {isOwn ? 'Your' : 'Their'} top {leadingGenres.length > 1 ? 'genres' : 'genre'}:{' '}
        <strong>{leadingGenres.join(' & ')}</strong>
      </p>
      <GenreRankList rows={rows} label="Top genres by number of books" />
      {moreLabel && (
        <button
          ref={moreButtonRef}
          type="button"
          className="genre-more"
          aria-haspopup="dialog"
          onClick={() => setShowAll(true)}
        >
          {moreLabel}
        </button>
      )}
      <p className="taste-caption">
        Out of {booksLabel(sampleSize)} on {ownerWord} shelves.
      </p>
      {showAll && (
        <GenreBreakdownDialog rows={allRows} sampleSize={sampleSize} ownerWord={ownerWord} onClose={closeAll} />
      )}
    </div>
  )
}
