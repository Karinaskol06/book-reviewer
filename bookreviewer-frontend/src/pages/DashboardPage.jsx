import { useEffect, useMemo, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useInView } from 'react-intersection-observer'
import AppChrome from '../components/layout/AppChrome.jsx'
import { resolveMediaUrl } from '../utils/media.js'
import './DashboardPage.css'
import { getCollectionShelves, getGenres, getTrendingBooks, searchBooks } from '../services/homeService.js'

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
  const location = useLocation()
  const navigate = useNavigate()
  const shelfRef = useRef(null)
  const [trendingBooks, setTrendingBooks] = useState([])
  const [genres, setGenres] = useState([])
  const [searchText, setSearchText] = useState('')
  const [searchResults, setSearchResults] = useState([])
  const [genreCards, setGenreCards] = useState([])
  const [loadingHome, setLoadingHome] = useState(true)
  const [loadingSearch, setLoadingSearch] = useState(false)
  const [canScrollLeft, setCanScrollLeft] = useState(false)
  const [canScrollRight, setCanScrollRight] = useState(false)
  const [heroTilt, setHeroTilt] = useState({ x: 0, y: 0 })

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
        setGenres(genreList.slice(0, 6))

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
    const debounced = setTimeout(async () => {
      const query = searchText.trim()
      if (!query) {
        setSearchResults([])
        return
      }

      try {
        setLoadingSearch(true)
        const books = await searchBooks(query, 8)
        setSearchResults(books)
      } catch (error) {
        console.error('Search failed', error)
      } finally {
        setLoadingSearch(false)
      }
    }, 350)

    return () => clearTimeout(debounced)
  }, [searchText])

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

  const displayedBooks = useMemo(
    () => (searchText.trim() ? searchResults : trendingBooks.slice(0, 8)),
    [searchResults, searchText, trendingBooks],
  )

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

  const handleHeroMouseMove = (event) => {
    const { currentTarget, clientX, clientY } = event
    const rect = currentTarget.getBoundingClientRect()
    const x = ((clientX - rect.left) / rect.width - 0.5) * 12
    const y = ((clientY - rect.top) / rect.height - 0.5) * 10
    setHeroTilt({ x, y })
  }

  return (
    <AppChrome>
      <div className="home-content">
        <section className="hero motion-surface" onMouseMove={handleHeroMouseMove} onMouseLeave={() => setHeroTilt({ x: 0, y: 0 })}>
          <MotionDiv className="hero__copy" initial={{ opacity: 0, y: 24 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5 }}>
            <h2>
              Find your next
              <br />
              <span>Great Read</span>
            </h2>
            <p className="hero__subcopy">Exciting discoveries, thoughtful reviews, and communities of like-minded people.</p>
            <input
              type="search"
              placeholder="Search the archive by title, author, or genre..."
              value={searchText}
              onChange={(event) => setSearchText(event.target.value)}
            />
            <div className="hero__tags">
              {genres.map((genre) => (
                <span key={genre}>#{genre.replace(/\s+/g, '')}</span>
              ))}
            </div>
          </MotionDiv>
          <MotionDiv
            className="hero__image hero__image--book"
            aria-label="Illustration of an opened book"
            animate={{ x: heroTilt.x, y: heroTilt.y, rotate: heroTilt.x * 0.3 }}
            transition={{ type: 'spring', stiffness: 140, damping: 16 }}
          >
            <div className="book-illustration" aria-hidden="true">
              <div className="book-illustration__shadow" />
              <div className="book-illustration__spine" />
              <div className="book-illustration__left-page" />
              <div className="book-illustration__right-page" />
              <div className="book-illustration__left-stack">
                {Array.from({ length: 8 }).map((_, index) => (
                  <span key={`left-${index}`} style={{ '--page-index': index }} />
                ))}
              </div>
              <div className="book-illustration__flip-stack">
                {Array.from({ length: 10 }).map((_, index) => (
                  <span key={`right-${index}`} style={{ '--page-index': index }} />
                ))}
              </div>
            </div>
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
            <h3>{searchText.trim() ? 'Search Results' : 'Currently Trending'}</h3>
            <p>{searchText.trim() ? 'Results from your archive query' : 'The most checked-out volumes this week'}</p>
          </div>
          {(loadingSearch || loadingHome) && (
            <div className="shelf-skeleton">
              {Array.from({ length: 4 }).map((_, index) => (
                <div key={index} className="skeleton-card" />
              ))}
            </div>
          )}
          {!loadingSearch && searchText.trim() && displayedBooks.length === 0 && (
            <div className="empty-search">
              <p>No books found for "{searchText.trim()}".</p>
              <MotionButton
                type="button"
                whileTap={{ scale: 0.98 }}
                onClick={() => navigate(`/books/new?query=${encodeURIComponent(searchText.trim())}`)}
              >
                Add this book to archive
              </MotionButton>
            </div>
          )}
          {!loadingSearch && !loadingHome && displayedBooks.length > 0 && (
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
                      onClick={() => navigate(`/search?genres=${encodeURIComponent(leadCollection.genre)}&page=0`)}
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
                            else navigate(`/search?genres=${encodeURIComponent(leadCollection.genre)}&page=0`)
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
                              else navigate(`/search?genres=${encodeURIComponent(card.genre)}&page=0`)
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
                          onClick={() => navigate(`/search?genres=${encodeURIComponent(card.genre)}&page=0`)}
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
