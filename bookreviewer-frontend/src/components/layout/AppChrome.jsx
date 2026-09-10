import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth.js'
import '../../pages/DashboardPage.css'

const PROFILE_ICON = (
  <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
    <path d="M12 12c2.76 0 5-2.24 5-5S14.76 2 12 2 7 4.24 7 7s2.24 5 5 5Zm0 2c-3.86 0-7 3.14-7 7 0 .55.45 1 1 1h12c.55 0 1-.45 1-1 0-3.86-3.14-7-7-7Z" />
  </svg>
)

/**
 * Shared header, footer, and mobile tab bar used across authenticated pages.
 */
const AppChrome = ({ children, className = '' }) => {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [headerSearch, setHeaderSearch] = useState('')

  const submitSearch = (event) => {
    if (event.key !== 'Enter') return
    const query = headerSearch.trim()
    if (!query) return
    navigate(`/search?query=${encodeURIComponent(query)}&page=0`)
  }

  return (
    <main className={`dashboard ${className}`.trim()}>
      <header className="home-nav">
        <h1>BookReviewer</h1>
        <nav>
          <Link to="/dashboard">Home</Link>
          <Link to="/search?page=0">Library</Link>
          <Link to="/books/new">Add Book</Link>
          <Link to="/dashboard#collections">Collections</Link>
          <Link to="/feed">Feed</Link>
          <Link to="/clubs">Clubs</Link>
        </nav>
        <input
          className="home-nav__search"
          type="search"
          placeholder="Search the archive..."
          value={headerSearch}
          onChange={(event) => setHeaderSearch(event.target.value)}
          onKeyDown={submitSearch}
        />
        <div className="home-nav__actions">
          <Link
            className="home-nav__profile"
            to="/profile"
            aria-label="My profile"
            title={user?.username || 'My profile'}
          >
            {PROFILE_ICON}
          </Link>
          <button type="button" onClick={logout}>
            Logout
          </button>
        </div>
      </header>

      {children}

      <footer className="home-footer">
        <h2>BookReviewer</h2>
        <nav>
          <Link to="/search?page=0">Library</Link>
          <Link to="/dashboard#collections">Collections</Link>
          <Link to="/feed">Feed</Link>
          <Link to="/clubs">Clubs</Link>
        </nav>
        <p>© 2026 BookReviewer. The Digital Archivist.</p>
      </footer>

      <nav className="mobile-tabbar" aria-label="Mobile navigation">
        <button type="button" onClick={() => navigate('/dashboard')} className="mobile-tabbar__item">
          <span>🏠</span>
          Home
        </button>
        <button type="button" onClick={() => navigate('/feed')} className="mobile-tabbar__item">
          <span>📝</span>
          Feed
        </button>
        <button type="button" onClick={() => navigate('/search?page=0')} className="mobile-tabbar__item mobile-tabbar__item--search">
          <span>🔎</span>
          Search
        </button>
        <button type="button" onClick={() => navigate('/books/new')} className="mobile-tabbar__item">
          <span>➕</span>
          Add
        </button>
        <button type="button" onClick={() => navigate('/profile')} className="mobile-tabbar__item">
          <span>👤</span>
          Profile
        </button>
      </nav>
    </main>
  )
}

export default AppChrome
