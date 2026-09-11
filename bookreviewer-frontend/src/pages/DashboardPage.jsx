import { useEffect, useMemo, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useInView } from 'react-intersection-observer'
import AppChrome from '../components/layout/AppChrome.jsx'
import { resolveMediaUrl } from '../utils/media.js'
import './DashboardPage.css'
import { getBooksByGenre, getGenres, getTrendingBooks, searchBooks } from '../services/homeService.js'

const renderStars = (rating = 0) => {
  const rounded = Math.round(rating)
  return '★★★★★'.slice(0, rounded) + '☆☆☆☆☆'.slice(0, 5 - rounded)
}

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
  const [communityRef, communityInView] = useInView({ triggerOnce: true, threshold: 0.2 })

  useEffect(() => {
    const loadHomeData = async () => {
      try {
        const trending = await getTrendingBooks(8)
        setTrendingBooks(trending)

        const allGenres = await getGenres()
        setGenres(allGenres.slice(0, 6))
        const selectedGenres = allGenres.slice(0, 4)

        const cards = await Promise.all(
          selectedGenres.map(async (genre) => {
            const books = await getBooksByGenre(genre, 1)
            return {
              genre,
              sampleBook: books[0] || null,
            }
          }),
        )

        setGenreCards(cards)
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
                    onClick={() => {
                      const queryToUse = searchText.trim() || book.title
                      navigate(`/search?query=${encodeURIComponent(queryToUse)}&page=0`)
                    }}
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
          className="section collections"
          id="collections"
          initial={{ opacity: 0, y: 28 }}
          animate={collectionsInView ? { opacity: 1, y: 0 } : {}}
          transition={{ duration: 0.5 }}
        >
          <div className="section__header">
            <h3>Explore Collections</h3>
          </div>
          {loadingHome ? (
            <div className="collections-skeleton">
              <div className="skeleton-panel skeleton-panel--feature" />
              <div className="skeleton-panel" />
              <div className="skeleton-panel" />
            </div>
          ) : (
            <div className="collection-layout">
              {genreCards[0] && (
                <article className="collection-card collection-card--feature motion-surface">
                  <span className="collection-card__meta">FEATURED STACK</span>
                  <h4>{genreCards[0].genre}</h4>
                  <p>
                    {genreCards[0].sampleBook
                      ? `Begin with "${genreCards[0].sampleBook.title}" by ${genreCards[0].sampleBook.author}.`
                      : 'Discover curated titles from this genre.'}
                  </p>
                  <MotionButton
                    type="button"
                    className="collection-card__action"
                    whileTap={{ scale: 0.97 }}
                    onClick={() => navigate(`/search?genres=${encodeURIComponent(genreCards[0].genre)}&page=0`)}
                  >
                    View genre
                  </MotionButton>
                </article>
              )}
              <div className="collection-layout__stack">
                {genreCards.slice(1).map((card, index) => (
                  <MotionArticle
                    key={card.genre}
                    className={`collection-card collection-card--${index}`}
                    whileHover={{ scale: 1.02 }}
                    transition={{ duration: 0.25 }}
                  >
                    <h4>{card.genre}</h4>
                    <p>
                      {card.sampleBook ? `Try "${card.sampleBook.title}" by ${card.sampleBook.author}.` : 'Curated notes from the archive.'}
                    </p>
                    <MotionButton
                      type="button"
                      className="collection-card__action"
                      whileTap={{ scale: 0.97 }}
                      onClick={() => navigate(`/search?genres=${encodeURIComponent(card.genre)}&page=0`)}
                    >
                      Explore
                    </MotionButton>
                  </MotionArticle>
                ))}
              </div>
            </div>
          )}
        </MotionSection>

        <MotionSection
          ref={communityRef}
          className="community section motion-surface"
          id="community"
          initial={{ opacity: 0, y: 28 }}
          animate={communityInView ? { opacity: 1, y: 0 } : {}}
          transition={{ duration: 0.5 }}
        >
          <div className="community__image" aria-label="Community section image" />
          <div className="community__copy">
            <p>BOOK CLUBS</p>
            <h3>Reflections on the Written Word</h3>
            <p>
              Our contributors do not just review books; they archive experiences. Dive into long-form essays that explore the cultural
              impact and emotional resonance of literature.
            </p>
            <a href="#community">Explore our community</a>
          </div>
        </MotionSection>
      </div>
    </AppChrome>
  )
}

export default DashboardPage
