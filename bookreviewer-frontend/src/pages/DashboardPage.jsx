import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { motion, useMotionValue, useReducedMotion, useSpring } from 'framer-motion'
import { useInView } from 'react-intersection-observer'
import AppChrome from '../components/layout/AppChrome.jsx'
import SearchSuggest from '../components/search/SearchSuggest.jsx'
import HeroShowcase from '../components/home/HeroShowcase.jsx'
import { resolveMediaUrl } from '../utils/media.js'
import './DashboardPage.css'
import { getCollectionShelves, getGenres, getTrendingBooks } from '../services/homeService.js'
import { collectionPath } from '../utils/search.js'

const renderStars = (rating = 0) => {
  const rounded = Math.round(rating)
  return '★★★★★'.slice(0, rounded) + '☆☆☆☆☆'.slice(0, 5 - rounded)
}

const collectionBlurb = (genre, books) => {
  const titles = books.map((book) => book.title).filter(Boolean).slice(0, 2)
  if (titles.length >= 2) {
    return `A ${genre.toLowerCase()} trail marked by “${titles[0]}” and “${titles[1]}.”`
  }
  if (titles.length === 1) {
    return `Start this ${genre.toLowerCase()} shelf with “${titles[0]}.”`
  }
  return `Browse the archive’s ${genre.toLowerCase()} shelf and find your next chapter.`
}

const ACTION_LINKS = [
  {
    id: 'for-you',
    title: 'For you',
    blurb: 'Picks based on what you’ve rated.',
    to: '/profile#recommendations',
  },
  {
    id: 'add-book',
    title: 'Add a book',
    blurb: 'Missing from the archive? Add it.',
    to: '/books/new',
  },
  {
    id: 'find-readers',
    title: 'Find readers',
    blurb: 'Follow people whose taste matches yours.',
    to: '/feed',
    state: { openFind: true },
  },
  {
    id: 'clubs',
    title: 'Book clubs',
    blurb: 'Join a discussion, not a brochure.',
    to: '/clubs',
  },
]

const DashboardPage = () => {
  const MotionSection = motion.section
  const MotionDiv = motion.div
  const MotionArticle = motion.article
  const MotionButton = motion.button
  const MotionSpan = motion.span
  const MotionPath = motion.path
  const location = useLocation()
  const navigate = useNavigate()
  const shelfRef = useRef(null)
  const [trendingBooks, setTrendingBooks] = useState([])
  const [allGenres, setAllGenres] = useState([])
  const [genreCards, setGenreCards] = useState([])
  const [loadingHome, setLoadingHome] = useState(true)
  const [canScrollLeft, setCanScrollLeft] = useState(false)
  const [canScrollRight, setCanScrollRight] = useState(false)
  const reduceMotion = useReducedMotion()
  const pointerX = useMotionValue(0)
  const pointerY = useMotionValue(0)
  const tiltX = useSpring(pointerX, { stiffness: 120, damping: 18 })
  const tiltY = useSpring(pointerY, { stiffness: 120, damping: 18 })

  const [trendingRef, trendingInView] = useInView({ triggerOnce: true, threshold: 0.15 })
  const [collectionsRef, collectionsInView] = useInView({ triggerOnce: true, threshold: 0.15 })
  const [actionsRef, actionsInView] = useInView({ triggerOnce: true, threshold: 0.2 })

  useEffect(() => {
    const loadHomeData = async () => {
      try {
        const trending = await getTrendingBooks(8)
        setTrendingBooks(trending)

        const allGenres = await getGenres()
        const genreList = Array.isArray(allGenres) ? allGenres : []
        setAllGenres(genreList)

        const shelves = await getCollectionShelves({
          genres: genreList,
          genreCount: 4,
          booksPerGenre: 3,
        })
        setGenreCards(shelves)
      } catch (error) {
        console.error('Failed to load home data', error)
      } finally {
        setLoadingHome(false)
      }
    }

    loadHomeData()
  }, [])

  useEffect(() => {
    if (!location.hash) return
    const targetId = location.hash.replace('#', '')
    if (!targetId) return

    const timer = setTimeout(() => {
      const target = document.getElementById(targetId)
      target?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }, 0)

    return () => clearTimeout(timer)
  }, [location.hash])

  const displayedBooks = useMemo(() => trendingBooks.slice(0, 8), [trendingBooks])
  const heroGenres = allGenres.slice(0, 6)

  const leadCollection = genreCards[0] || null
  const railCollections = genreCards.slice(1)

  useEffect(() => {
    const shelf = shelfRef.current
    if (!shelf) return

    const updateControls = () => {
      setCanScrollLeft(shelf.scrollLeft > 8)
      setCanScrollRight(shelf.scrollLeft + shelf.clientWidth < shelf.scrollWidth - 8)
    }

    updateControls()
    shelf.addEventListener('scroll', updateControls, { passive: true })
    window.addEventListener('resize', updateControls)

    return () => {
      shelf.removeEventListener('scroll', updateControls)
      window.removeEventListener('resize', updateControls)
    }
  }, [displayedBooks])

  const scrollShelfBy = (amount) => {
    shelfRef.current?.scrollBy({ left: amount, behavior: 'smooth' })
  }

  const handleHeroPointerMove = (event) => {
    if (reduceMotion || event.pointerType !== 'mouse') return
    const rect = event.currentTarget.getBoundingClientRect()
    pointerX.set(((event.clientX - rect.left) / rect.width - 0.5) * 14)
    pointerY.set(-((event.clientY - rect.top) / rect.height - 0.5) * 10)
  }

  const resetHeroTilt = () => {
    pointerX.set(0)
    pointerY.set(0)
  }

  return (
    <AppChrome>
      <div className="home-content">
        <section className="hero motion-surface" onPointerMove={handleHeroPointerMove} onPointerLeave={resetHeroTilt}>
          <div className="hero__backdrop" aria-hidden="true">
            <span className="hero__blob hero__blob--rose" />
            <span className="hero__blob hero__blob--amber" />
            <span className="hero__blob hero__blob--sage" />
            <span className="hero__grid" />
          </div>
          <div className="hero__copy">
            <MotionDiv
              className="hero__eyebrow"
              initial={reduceMotion ? false : { opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.45 }}
            >
              <span className="hero__eyebrow-dot" aria-hidden="true" />
              Reviews by readers, for readers
            </MotionDiv>
            <h2 aria-label="Find your next Great Read">
              <span className="hero__line" aria-hidden="true">
                {['Find', 'your', 'next'].map((word, index) => (
                  <span className="hero__word-mask" key={word}>
                    <MotionSpan
                      className="hero__word"
                      initial={reduceMotion ? false : { y: '110%' }}
                      animate={{ y: '0%' }}
                      transition={{ duration: 0.7, delay: 0.08 + index * 0.09, ease: [0.22, 1, 0.36, 1] }}
                    >
                      {word}
                    </MotionSpan>
                  </span>
                ))}
              </span>
              <span className="hero__line hero__line--accent" aria-hidden="true">
                <span className="hero__word-mask">
                  <MotionSpan
                    className="hero__word hero__accent"
                    initial={reduceMotion ? false : { y: '110%' }}
                    animate={{ y: '0%' }}
                    transition={{ duration: 0.8, delay: 0.38, ease: [0.22, 1, 0.36, 1] }}
                  >
                    Great Read
                  </MotionSpan>
                </span>
                <svg className="hero__underline" viewBox="0 0 320 24" preserveAspectRatio="none">
                  <MotionPath
                    d="M4 16 C 70 6, 150 4, 220 10 S 300 18, 316 8"
                    initial={reduceMotion ? false : { pathLength: 0 }}
                    animate={{ pathLength: 1 }}
                    transition={{ duration: 0.9, delay: 0.95, ease: 'easeInOut' }}
                  />
                </svg>
              </span>
            </h2>
            <MotionDiv
              className="hero__lower"
              initial={reduceMotion ? false : { opacity: 0, y: 18 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.55, delay: 0.55 }}
            >
              <p className="hero__subcopy">Exciting discoveries, thoughtful reviews, and communities of like-minded people.</p>
              <SearchSuggest genres={allGenres} popularGenres={heroGenres} placeholder="Search by title, author or genre…" />
              {heroGenres.length > 0 && (
                <nav className="hero__tags" aria-label="Popular collections">
                  {heroGenres.map((genre) => (
                    <Link key={genre} to={collectionPath(genre)}>
                      <span aria-hidden="true">#</span>
                      {genre.replace(/\s+/g, '')}
                    </Link>
                  ))}
                  <Link className="hero__tags-all" to="/collections">
                    All collections
                    <span aria-hidden="true">→</span>
                  </Link>
                </nav>
              )}
            </MotionDiv>
          </div>
          <MotionDiv
            className="hero__image"
            initial={reduceMotion ? false : { opacity: 0, scale: 0.94 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.8, delay: 0.2, ease: [0.22, 1, 0.36, 1] }}
          >
            <HeroShowcase
              books={trendingBooks}
              loading={loadingHome}
              tiltX={tiltX}
              tiltY={tiltY}
              onOpenBook={(book) => navigate(`/books/${book.id}`)}
            />
          </MotionDiv>
        </section>

        <MotionSection
          ref={trendingRef}
          className="section"
          id="trending"
          initial={{ opacity: 0, y: 28 }}
          animate={trendingInView ? { opacity: 1, y: 0 } : {}}
          transition={{ duration: 0.5 }}
        >
          <div className="section__header">
            <h3>Currently Trending</h3>
            <p>The most checked-out volumes this week</p>
          </div>
          {loadingHome && (
            <div className="shelf-skeleton">
              {Array.from({ length: 4 }).map((_, index) => (
                <div key={index} className="skeleton-card" />
              ))}
            </div>
          )}
          {!loadingHome && displayedBooks.length > 0 && (
            <div className="shelf-wrap">
              <button
                type="button"
                className={`shelf-control shelf-control--left ${canScrollLeft ? 'is-visible' : ''}`}
                onClick={() => scrollShelfBy(-340)}
                aria-label="Scroll left"
              >
                ←
              </button>
              <div className="book-shelf" ref={shelfRef}>
                {displayedBooks.map((book, index) => (
                  <MotionArticle
                    key={book.id}
                    className="book-card"
                    onClick={() => navigate(`/books/${book.id}`)}
                    initial={{ opacity: 0, y: 22 }}
                    animate={trendingInView ? { opacity: 1, y: 0 } : {}}
                    transition={{ duration: 0.36, delay: index * 0.06 }}
                    whileHover={{ y: -6, scale: 1.02 }}
                  >
                    <img src={resolveMediaUrl(book.coverUrl, '/home-book.jpg')} alt={book.title} />
                    <h4>{book.title}</h4>
                    <p>{book.author}</p>
                    <p className="book-card__rating">{renderStars(book.averageRating)} ({book.averageRating?.toFixed?.(1) || '0.0'})</p>
                    <p className="book-card__genres">{(book.genres || []).slice(0, 2).join(' • ') || 'Uncategorized'}</p>
                  </MotionArticle>
                ))}
              </div>
              <button
                type="button"
                className={`shelf-control shelf-control--right ${canScrollRight ? 'is-visible' : ''}`}
                onClick={() => scrollShelfBy(340)}
                aria-label="Scroll right"
              >
                →
              </button>
            </div>
          )}
        </MotionSection>

        <MotionSection
          ref={collectionsRef}
          className="section folio-section"
          id="collections"
          initial={{ opacity: 0, y: 28 }}
          animate={collectionsInView ? { opacity: 1, y: 0 } : {}}
          transition={{ duration: 0.5 }}
        >
          <div className="folio-section__header">
            <p className="folio-section__kicker">Stacks from the archive</p>
            <h3>Explore Collections</h3>
            <p className="folio-section__lede">
              Each shelf opens with a few covers — follow the one that catches your eye.
            </p>
            <Link className="folio-section__all" to="/collections">
              See all collections
              <span aria-hidden="true">→</span>
            </Link>
          </div>

          {loadingHome ? (
            <div className="folio-skeleton">
              <div className="folio-skeleton__lead" />
              <div className="folio-skeleton__rail">
                <div />
                <div />
                <div />
              </div>
            </div>
          ) : (
            <>
              {leadCollection && (
                <MotionArticle
                  className="folio-lead"
                  initial={{ opacity: 0, y: 18 }}
                  animate={collectionsInView ? { opacity: 1, y: 0 } : {}}
                  transition={{ duration: 0.45 }}
                >
                  <div className="folio-lead__copy">
                    <span className="folio-lead__meta">Opening shelf</span>
                    <h4>{leadCollection.genre}</h4>
                    <p>{collectionBlurb(leadCollection.genre, leadCollection.books)}</p>
                    <button
                      type="button"
                      className="folio-link"
                      onClick={() => navigate(collectionPath(leadCollection.genre))}
                    >
                      Open collection
                      <span aria-hidden="true">→</span>
                    </button>
                  </div>
                  <div className="folio-lead__stage" aria-hidden={leadCollection.books.length === 0}>
                    {(leadCollection.books.length ? leadCollection.books : [null, null, null])
                      .slice(0, 3)
                      .map((book, index) => (
                        <button
                          key={book?.id || `lead-cover-${index}`}
                          type="button"
                          className={`folio-cover folio-cover--${index}`}
                          onClick={() => {
                            if (book?.id) navigate(`/books/${book.id}`)
                            else navigate(collectionPath(leadCollection.genre))
                          }}
                        >
                          <img
                            src={resolveMediaUrl(book?.coverUrl, '/home-book.jpg')}
                            alt={book?.title || `${leadCollection.genre} cover`}
                          />
                        </button>
                      ))}
                  </div>
                </MotionArticle>
              )}

              {railCollections.length > 0 && (
                <div className="folio-rail">
                  {railCollections.map((card, index) => (
                    <MotionArticle
                      key={card.genre}
                      className={`folio-card folio-card--${index % 3}`}
                      initial={{ opacity: 0, y: 22 }}
                      animate={collectionsInView ? { opacity: 1, y: 0 } : {}}
                      transition={{ duration: 0.4, delay: 0.1 + index * 0.08 }}
                      whileHover={{ y: -5 }}
                    >
                      <div className="folio-card__stack">
                        {(card.books.length ? card.books : [null, null, null]).slice(0, 3).map((book, coverIndex) => (
                          <button
                            key={book?.id || `${card.genre}-cover-${coverIndex}`}
                            type="button"
                            className={`folio-card__cover folio-card__cover--${coverIndex}`}
                            onClick={() => {
                              if (book?.id) navigate(`/books/${book.id}`)
                              else navigate(collectionPath(card.genre))
                            }}
                          >
                            <img
                              src={resolveMediaUrl(book?.coverUrl, '/home-book.jpg')}
                              alt={book?.title || `${card.genre} cover`}
                            />
                          </button>
                        ))}
                      </div>
                      <div className="folio-card__copy">
                        <h4>{card.genre}</h4>
                        <p>{collectionBlurb(card.genre, card.books)}</p>
                        <button
                          type="button"
                          className="folio-link folio-link--compact"
                          onClick={() => navigate(collectionPath(card.genre))}
                        >
                          Browse shelf
                          <span aria-hidden="true">→</span>
                        </button>
                      </div>
                    </MotionArticle>
                  ))}
                </div>
              )}
            </>
          )}
        </MotionSection>

        <MotionSection
          ref={actionsRef}
          className="section passage-board motion-surface"
          id="ways-in"
          initial={{ opacity: 0, y: 28 }}
          animate={actionsInView ? { opacity: 1, y: 0 } : {}}
          transition={{ duration: 0.5 }}
        >
          <div className="passage-board__intro">
            <p className="passage-board__kicker">Beyond the shelf</p>
            <h3>Keep exploring</h3>
            <p className="passage-board__lede">
              Recommendations, contributions, people, and clubs — pick a path and keep reading with others.
            </p>
          </div>

          <div className="passage-board__list">
            {ACTION_LINKS.map((link, index) => (
              <MotionButton
                key={link.id}
                type="button"
                className="passage-row"
                initial={{ opacity: 0, x: -10 }}
                animate={actionsInView ? { opacity: 1, x: 0 } : {}}
                transition={{ duration: 0.32, delay: 0.06 + index * 0.05 }}
                whileHover={{ x: 4 }}
                whileTap={{ scale: 0.995 }}
                onClick={() => navigate(link.to, link.state ? { state: link.state } : undefined)}
              >
                <span className="passage-row__mark" aria-hidden="true" />
                <span className="passage-row__text">
                  <span className="passage-row__title">{link.title}</span>
                  <span className="passage-row__blurb">{link.blurb}</span>
                </span>
                <span className="passage-row__go" aria-hidden="true">→</span>
              </MotionButton>
            ))}
          </div>
        </MotionSection>
      </div>
    </AppChrome>
  )
}

export default DashboardPage
