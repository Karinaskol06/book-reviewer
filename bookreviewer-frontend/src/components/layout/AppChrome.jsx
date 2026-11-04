import { useEffect, useRef, useState } from 'react'
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth.js'
import { resolveMediaUrl } from '../../utils/media.js'
import '../../pages/DashboardPage.css'
import './AppChrome.css'

/** Masthead sections; `key` decides which one is marked as the current page. */
const SECTIONS = [
  { key: 'home', to: '/dashboard', label: 'Home' },
  { key: 'library', to: '/search?page=0', label: 'Library' },
  { key: 'feed', to: '/feed', label: 'Feed' },
  { key: 'clubs', to: '/clubs', label: 'Clubs' },
  { key: 'collections', to: '/collections', label: 'Collections' },
]

const TABS = [
  { to: '/dashboard', label: 'Home', icon: 'home' },
  { to: '/feed', label: 'Feed', icon: 'feed' },
  { to: '/search?page=0', label: 'Search', icon: 'search', featured: true },
  { to: '/clubs', label: 'Clubs', icon: 'clubs' },
  { to: '/profile', label: 'Profile', icon: 'user' },
]

/** Footer link columns. */
const FOOTER_GROUPS = [
  {
    heading: 'Explore',
    links: [
      { to: '/dashboard', label: 'Home' },
      { to: '/search?page=0', label: 'Library' },
      { to: '/collections', label: 'Collections' },
    ],
  },
  {
    heading: 'Community',
    links: [
      { to: '/feed', label: 'Feed' },
      { to: '/clubs', label: 'Clubs' },
    ],
  },
  {
    heading: 'Your shelf',
    links: [
      { to: '/profile', label: 'My profile' },
      { to: '/books/new', label: 'Add a book' },
    ],
  },
]

/** Pixels of scroll movement ignored, so small jitters don't flip the header. */
const SCROLL_SLACK = 6

/** 24px line icons (stroke follows the text colour). */
const ICON_PATHS = {
  home: (
    <>
      <path d="M3.5 10.5 12 3.5l8.5 7" />
      <path d="M5.5 9v11h4.75v-6h3.5v6h4.75V9" />
    </>
  ),
  feed: (
    <>
      <rect x="4" y="3.5" width="16" height="17" rx="2.5" />
      <path d="M8 8.5h8M8 12h8M8 15.5h5" />
    </>
  ),
  search: (
    <>
      <circle cx="11" cy="11" r="6.5" />
      <path d="m20 20-4.4-4.4" />
    </>
  ),
  clubs: (
    <>
      <circle cx="9" cy="8.5" r="3.5" />
      <path d="M2.5 20a6.5 6.5 0 0 1 13 0" />
      <path d="M15.5 5.2a3.5 3.5 0 0 1 0 6.6" />
      <path d="M17.6 14.3a6.5 6.5 0 0 1 3.9 5.7" />
    </>
  ),
  user: (
    <>
      <circle cx="12" cy="8" r="4" />
      <path d="M4.5 20.5a7.5 7.5 0 0 1 15 0" />
    </>
  ),
  plus: <path d="M12 5v14M5 12h14" />,
  arrowUp: <path d="M12 19V5M6 11l6-6 6 6" />,
  chevron: <path d="m6 9 6 6 6-6" />,
  menu: <path d="M4 7h16M4 12h16M4 17h16" />,
  close: <path d="M6 6l12 12M18 6 6 18" />,
}

const NavIcon = ({ name }) => (
  <svg
    className="nav-icon"
    viewBox="0 0 24 24"
    width="24"
    height="24"
    fill="none"
    stroke="currentColor"
    strokeWidth="1.8"
    strokeLinecap="round"
    strokeLinejoin="round"
    aria-hidden="true"
    focusable="false"
  >
    {ICON_PATHS[name]}
  </svg>
)

const currentSection = ({ pathname }) => {
  if (pathname === '/dashboard') return 'home'
  if (pathname.startsWith('/collections')) return 'collections'
  if (pathname.startsWith('/search')) return 'library'
  if (pathname.startsWith('/feed')) return 'feed'
  if (pathname.startsWith('/clubs')) return 'clubs'
  return null
}

/**
 * Shared header, footer, and mobile tab bar used across authenticated pages.
 * Desktop: one bar with search, the centred logo, Add book and the account menu; the sections sit below it.
 * Phones: the logo and a menu button; the menu holds every section, Add book, search and log out.
 * The header slides away while scrolling down and comes back on any scroll up.
 */
const AppChrome = ({ children, className = '' }) => {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [headerSearch, setHeaderSearch] = useState('')
  const [scrolledAway, setScrolledAway] = useState(false)
  const headerRef = useRef(null)
  const menuButtonRef = useRef(null)
  const accountButtonRef = useRef(null)
  const accountRef = useRef(null)
  const avatarSrc = resolveMediaUrl(user?.avatarUrl, '/user-stub.png')
  const activeSection = currentSection(location)

  // A menu remembers the page it was opened on, so any navigation closes it.
  const locationKey = `${location.pathname}${location.search}${location.hash}`
  const [openMenu, setOpenMenu] = useState(null)
  const isOpen = (name) => openMenu?.name === name && openMenu.at === locationKey
  const toggleMenu = (name) => setOpenMenu(isOpen(name) ? null : { name, at: locationKey })
  const menuOpen = isOpen('site')
  const accountOpen = isOpen('account')
  const headerHidden = scrolledAway && !menuOpen && !accountOpen

  useEffect(() => {
    let lastY = window.scrollY
    const onScroll = () => {
      const y = window.scrollY
      const delta = y - lastY
      if (Math.abs(delta) < SCROLL_SLACK) return
      const headerHeight = headerRef.current?.offsetHeight ?? 0
      setScrolledAway(delta > 0 && y > headerHeight)
      lastY = y
    }
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  useEffect(() => {
    if (!openMenu) return undefined
    const close = (focusTarget) => {
      setOpenMenu(null)
      focusTarget?.focus()
    }
    const onKeyDown = (event) => {
      if (event.key !== 'Escape') return
      close(openMenu.name === 'account' ? accountButtonRef.current : menuButtonRef.current)
    }
    const onPointerDown = (event) => {
      if (openMenu.name === 'account' && !accountRef.current?.contains(event.target)) close()
    }
    document.addEventListener('keydown', onKeyDown)
    document.addEventListener('pointerdown', onPointerDown)
    return () => {
      document.removeEventListener('keydown', onKeyDown)
      document.removeEventListener('pointerdown', onPointerDown)
    }
  }, [openMenu])

  const scrollToTop = () => {
    const reduceMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches
    window.scrollTo({ top: 0, behavior: reduceMotion ? 'auto' : 'smooth' })
  }

  const submitSearch = (event) => {
    if (event.key !== 'Enter') return
    const query = headerSearch.trim()
    if (!query) return
    navigate(`/search?query=${encodeURIComponent(query)}&page=0`)
  }

  const searchField = (id) => (
    <label className="site-search" htmlFor={id}>
      <NavIcon name="search" />
      <input
        id={id}
        type="search"
        placeholder="Search the archive..."
        aria-label="Search the archive"
        value={headerSearch}
        onChange={(event) => setHeaderSearch(event.target.value)}
        onKeyDown={submitSearch}
      />
    </label>
  )

  return (
    <main className={`dashboard ${className}`.trim()}>
      <header
        ref={headerRef}
        className="site-header"
        data-hidden={headerHidden}
        onFocus={() => setScrolledAway(false)}
      >
        <div className="site-header__bar">
          {searchField('site-search-desktop')}
          <Link className="site-header__logo" to="/dashboard">
            BookReviewer
          </Link>
          <div className="site-header__actions">
            <Link className="site-header__add" to="/books/new">
              <NavIcon name="plus" />
              Add book
            </Link>
            <div className="account-menu" ref={accountRef}>
              <button
                ref={accountButtonRef}
                type="button"
                className="account-menu__button"
                aria-label="Account menu"
                aria-expanded={accountOpen}
                aria-controls="account-menu"
                onClick={() => toggleMenu('account')}
              >
                <img src={avatarSrc} alt="" />
                <NavIcon name="chevron" />
              </button>
              {accountOpen && (
                <div id="account-menu" className="account-menu__panel">
                  <p className="account-menu__name">@{user?.username}</p>
                  <Link to="/profile">My profile</Link>
                  <button type="button" onClick={logout}>
                    Log out
                  </button>
                </div>
              )}
            </div>
          </div>
          <button
            ref={menuButtonRef}
            type="button"
            className="site-header__menu-toggle"
            aria-expanded={menuOpen}
            aria-controls="app-menu"
            aria-label={menuOpen ? 'Close menu' : 'Open menu'}
            onClick={() => toggleMenu('site')}
          >
            <NavIcon name={menuOpen ? 'close' : 'menu'} />
          </button>
        </div>

        <nav className="site-header__sections" aria-label="Primary">
          {SECTIONS.map(({ key, to, label }) => (
            <Link key={key} to={to} aria-current={activeSection === key ? 'page' : undefined}>
              {label}
            </Link>
          ))}
        </nav>

        <div id="app-menu" className="site-menu" data-open={menuOpen}>
          {searchField('site-search-phone')}
          <nav aria-label="Menu">
            {SECTIONS.map(({ key, to, label }) => (
              <Link key={key} to={to} aria-current={activeSection === key ? 'page' : undefined} onClick={() => setOpenMenu(null)}>
                {label}
              </Link>
            ))}
            <Link to="/books/new" onClick={() => setOpenMenu(null)}>
              Add book
            </Link>
          </nav>
          <div className="site-menu__account">
            <Link className="site-menu__profile" to="/profile" onClick={() => setOpenMenu(null)}>
              <img src={avatarSrc} alt="" />
              My profile
            </Link>
            <button type="button" onClick={logout}>
              Log out
            </button>
          </div>
        </div>
      </header>

      {children}

      <footer className="site-footer">
        <div className="site-footer__main">
          <div className="site-footer__brand">
            <p className="site-footer__logo">BookReviewer</p>
            <p className="site-footer__tagline">Thoughtful reviews, shared shelves and reading circles.</p>
          </div>
          <nav className="site-footer__nav" aria-label="Footer">
            {FOOTER_GROUPS.map(({ heading, links }) => {
              const headingId = `footer-${heading.toLowerCase().replace(/\s+/g, '-')}`
              return (
                <div key={heading} className="site-footer__group">
                  <p id={headingId} className="site-footer__heading">
                    {heading}
                  </p>
                  <ul aria-labelledby={headingId}>
                    {links.map(({ to, label }) => (
                      <li key={label}>
                        <Link to={to}>{label}</Link>
                      </li>
                    ))}
                  </ul>
                </div>
              )
            })}
          </nav>
        </div>
        <div className="site-footer__base">
          <p className="site-footer__copy">© {new Date().getFullYear()} BookReviewer. The Digital Archivist.</p>
          <button type="button" className="site-footer__top" onClick={scrollToTop}>
            Back to top
            <NavIcon name="arrowUp" />
          </button>
        </div>
      </footer>

      <nav className="mobile-tabbar" aria-label="Mobile navigation">
        {TABS.map(({ to, label, icon, featured }) => (
          <NavLink
            key={label}
            to={to}
            className={`mobile-tabbar__item${featured ? ' mobile-tabbar__item--search' : ''}`}
          >
            <NavIcon name={icon} />
            <span>{label}</span>
          </NavLink>
        ))}
      </nav>
    </main>
  )
}

export default AppChrome
