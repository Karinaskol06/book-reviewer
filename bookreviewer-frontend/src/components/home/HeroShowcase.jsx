import { useEffect, useMemo, useState } from 'react'
import { AnimatePresence, motion, useReducedMotion } from 'framer-motion'
import { Link } from 'react-router-dom'
import { resolveMediaUrl } from '../../utils/media.js'
import './HeroShowcase.css'

const MotionDiv = motion.div
const MotionButton = motion.button

const ROTATE_MS = 4200
const SPINE_TONES = ['#022859', '#c97f86', '#2f4e50', '#d98a1f', '#2c5273']

/* Where each book sits relative to the active one: 0 = front, ±1 = flanking, anything else waits behind. */
const SLOTS = {
  0: { x: '0%', z: 40, rotateY: 22, scale: 1, opacity: 1, zIndex: 3 },
  1: { x: '78%', z: -140, rotateY: -30, scale: 0.8, opacity: 0.92, zIndex: 2 },
  [-1]: { x: '-78%', z: -140, rotateY: 34, scale: 0.8, opacity: 0.92, zIndex: 2 },
  hidden: { x: '0%', z: -280, rotateY: 0, scale: 0.6, opacity: 0, zIndex: 1 },
}

const PLACEHOLDERS = [
  { id: 'placeholder-a', title: 'Your next favourite', author: 'Loading the shelf…', placeholder: true },
  { id: 'placeholder-b', title: 'A story worth sharing', author: '', placeholder: true },
  { id: 'placeholder-c', title: 'The one you can’t put down', author: '', placeholder: true },
]

const slotFor = (index, active, count) => {
  if (count === 1) return 0
  let offset = (index - active + count) % count
  if (offset > count / 2) offset -= count
  if (count === 2 && offset !== 0) return 1
  return offset === 0 || offset === 1 || offset === -1 ? offset : 'hidden'
}

const Stars = ({ rating = 0 }) => {
  const rounded = Math.round(rating)
  return (
    <span className="showcase-card__stars" aria-label={`Rated ${rating.toFixed(1)} out of 5`}>
      {Array.from({ length: 5 }).map((_, index) => (
        <svg key={index} viewBox="0 0 20 20" aria-hidden="true" className={index < rounded ? 'is-on' : ''}>
          <path d="M10 1.6l2.6 5.3 5.8.8-4.2 4.1 1 5.8L10 14.9l-5.2 2.7 1-5.8L1.6 7.7l5.8-.8z" />
        </svg>
      ))}
    </span>
  )
}

const Sparkle = ({ className }) => (
  <svg className={`showcase__sparkle ${className}`} viewBox="0 0 24 24" aria-hidden="true">
    <path d="M12 0c.9 6.6 4.9 10.8 12 12-7.1 1.2-11.1 5.4-12 12-.9-6.6-4.9-10.8-12-12C7.1 10.8 11.1 6.6 12 0z" />
  </svg>
)

const Book3D = ({ book, index, slot, onSelect, isActive, reduceMotion }) => {
  const target = SLOTS[slot]
  const tone = SPINE_TONES[index % SPINE_TONES.length]
  return (
    <MotionButton
      type="button"
      className={`book3d ${isActive ? 'is-active' : ''}`}
      style={{ '--spine': tone }}
      initial={false}
      animate={target}
      transition={reduceMotion ? { duration: 0 } : { type: 'spring', stiffness: 90, damping: 18, mass: 0.9 }}
      onClick={() => onSelect(index)}
      tabIndex={slot === 'hidden' ? -1 : 0}
      aria-hidden={slot === 'hidden'}
      aria-label={isActive ? `Open ${book.title}` : `Show ${book.title}`}
    >
      <span className="book3d__float">
        <span className="book3d__face book3d__front">
          {book.placeholder ? (
            <span className="book3d__placeholder">
              <span>{book.title}</span>
            </span>
          ) : (
            <img src={resolveMediaUrl(book.coverUrl, '/home-book.jpg')} alt="" draggable="false" />
          )}
          <span className="book3d__gloss" />
        </span>
        <span className="book3d__face book3d__spine">
          <span>{book.title}</span>
        </span>
        <span className="book3d__face book3d__pages" />
        <span className="book3d__face book3d__back" />
      </span>
      <span className="book3d__shadow" />
    </MotionButton>
  )
}

const HeroShowcase = ({ books = [], loading = false, tiltX, tiltY, onOpenBook }) => {
  const reduceMotion = useReducedMotion()
  const items = useMemo(() => {
    const withCovers = books.filter((book) => book?.coverUrl)
    const pool = (withCovers.length >= 3 ? withCovers : books).slice(0, 5)
    return pool.length ? pool : PLACEHOLDERS
  }, [books])
  const [rawActive, setActive] = useState(0)
  const [paused, setPaused] = useState(false)
  const count = items.length
  const active = rawActive % count
  const current = items[active]

  useEffect(() => {
    if (reduceMotion || paused || count < 2) return undefined
    const timer = setInterval(() => setActive((value) => (value + 1) % count), ROTATE_MS)
    return () => clearInterval(timer)
  }, [reduceMotion, paused, count])

  const handleSelect = (index) => {
    if (index === active && !items[index].placeholder) {
      onOpenBook?.(items[index])
      return
    }
    setActive(index)
  }

  return (
    <div
      className="showcase"
      role="region"
      aria-roledescription="carousel"
      aria-label="Trending books"
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
      onFocus={() => setPaused(true)}
      onBlur={() => setPaused(false)}
    >
      <div className="showcase__halo" aria-hidden="true" />
      <div className="showcase__orbit showcase__orbit--outer" aria-hidden="true">
        <span />
      </div>
      <div className="showcase__orbit showcase__orbit--inner" aria-hidden="true">
        <span />
      </div>
      <Sparkle className="showcase__sparkle--a" />
      <Sparkle className="showcase__sparkle--b" />
      <Sparkle className="showcase__sparkle--c" />

      <MotionDiv className="showcase__stage" style={{ rotateX: tiltY, rotateY: tiltX }}>
        {items.map((book, index) => (
          <Book3D
            key={book.id}
            book={book}
            index={index}
            slot={slotFor(index, active, count)}
            isActive={index === active}
            onSelect={handleSelect}
            reduceMotion={reduceMotion}
          />
        ))}
      </MotionDiv>

      <div className="showcase-card" aria-live="polite">
        <AnimatePresence mode="wait" initial={false}>
          <MotionDiv
            key={current.id}
            className="showcase-card__body"
            initial={reduceMotion ? false : { opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={reduceMotion ? { opacity: 0 } : { opacity: 0, y: -10 }}
            transition={{ duration: 0.28, ease: 'easeOut' }}
          >
            <span className="showcase-card__label">
              <span className="showcase-card__pulse" aria-hidden="true" />
              {loading ? 'Gathering the shelf' : current.placeholder ? 'Fresh on the shelf' : 'Trending now'}
            </span>
            <p className="showcase-card__title">{current.title}</p>
            {current.author && <p className="showcase-card__author">{current.author}</p>}
            {!current.placeholder && (
              <div className="showcase-card__meta">
                <Stars rating={current.averageRating || 0} />
                <Link to={`/books/${current.id}`} className="showcase-card__link">
                  Read reviews <span aria-hidden="true">→</span>
                </Link>
              </div>
            )}
          </MotionDiv>
        </AnimatePresence>
        {count > 1 && (
          <div className="showcase-card__dots" role="group" aria-label="Choose a book">
            {items.map((book, index) => (
              <button
                key={book.id}
                type="button"
                className={`${index === active ? 'is-active' : ''} ${!reduceMotion && !paused ? 'is-running' : ''}`}
                aria-label={`Show ${book.title}`}
                aria-current={index === active}
                onClick={() => setActive(index)}
              >
                <span key={`${active}-${paused}`} style={{ animationDuration: `${ROTATE_MS}ms` }} />
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}

export default HeroShowcase
