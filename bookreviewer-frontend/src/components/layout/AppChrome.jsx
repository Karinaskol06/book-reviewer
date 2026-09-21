import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth.js'
import { resolveMediaUrl } from '../../utils/media.js'
import '../../pages/DashboardPage.css'

/**
 * Shared header, footer, and mobile tab bar used across authenticated pages.
 */
const AppChrome = ({ children, className = '' }) => {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [headerSearch, setHeaderSearch] = useState('')
  const avatarSrc = resolveMediaUrl(user?.avatarUrl, '/user-stub.png')

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
            <img src={avatarSrc} alt="" />
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
          <Link to="/dashboard">Home</Link>
          <Link to="/search?page=0">Library</Link>
          <Link to="/books/new">Add Book</Link>
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
