// @vitest-environment jsdom
import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { AuthContext } from '../../context/authContext.js'
import AppChrome from './AppChrome.jsx'

afterEach(() => {
  cleanup()
  window.scrollY = 0
})

const EMOJI = /\p{Extended_Pictographic}/u

const renderChrome = (path = '/feed', logout = vi.fn()) => {
  render(
    <AuthContext.Provider value={{ user: { userId: 2, username: 'karina' }, logout }}>
      <MemoryRouter initialEntries={[path]}>
        <AppChrome>
          <p>Page content</p>
        </AppChrome>
      </MemoryRouter>
    </AuthContext.Provider>,
  )
  return { logout }
}

const tabBar = () => screen.getByRole('navigation', { name: 'Mobile navigation' })
const primaryNav = () => screen.getByRole('navigation', { name: 'Primary' })
const menuButton = () => screen.getByRole('button', { name: /^(Open|Close) menu$/ })
const accountButton = () => screen.getByRole('button', { name: 'Account menu' })
const controlled = (button) => document.getElementById(button.getAttribute('aria-controls'))

const scrollTo = (y) => {
  window.scrollY = y
  fireEvent.scroll(window)
}

describe('AppChrome mobile tab bar', () => {
  it('shows Home, Feed, Search, Clubs and Profile as icon links without emoji', () => {
    renderChrome()

    const links = within(tabBar()).getAllByRole('link')
    expect(links.map((link) => link.textContent.trim())).toEqual(['Home', 'Feed', 'Search', 'Clubs', 'Profile'])
    links.forEach((link) => {
      expect(link.querySelector('svg')).not.toBeNull()
      expect(link.textContent).not.toMatch(EMOJI)
    })
  })

  it('marks the tab for the current page', () => {
    renderChrome('/clubs')

    expect(within(tabBar()).getByRole('link', { name: 'Clubs' }).getAttribute('aria-current')).toBe('page')
    expect(within(tabBar()).getByRole('link', { name: 'Home' }).getAttribute('aria-current')).toBeNull()
  })
})

describe('AppChrome masthead', () => {
  it('links the logo home and lists the five sections, marking the current one', () => {
    renderChrome('/feed')

    expect(screen.getByRole('link', { name: 'BookReviewer' }).getAttribute('href')).toBe('/dashboard')
    const links = within(primaryNav()).getAllByRole('link')
    expect(links.map((link) => link.textContent)).toEqual(['Home', 'Library', 'Feed', 'Clubs', 'Collections'])
    expect(within(primaryNav()).getByRole('link', { name: 'Feed' }).getAttribute('aria-current')).toBe('page')
    expect(within(primaryNav()).getByRole('link', { name: 'Home' }).getAttribute('aria-current')).toBeNull()
  })

  it('links Collections to the collections page and marks it on any collection', () => {
    renderChrome('/collections/Dark%20Academia')

    const collections = within(primaryNav()).getByRole('link', { name: 'Collections' })
    expect(collections.getAttribute('href')).toBe('/collections')
    expect(collections.getAttribute('aria-current')).toBe('page')
    expect(within(primaryNav()).getByRole('link', { name: 'Home' }).getAttribute('aria-current')).toBeNull()
  })

  it('offers Add book as the header action', () => {
    renderChrome()

    const addLinks = screen.getAllByRole('link', { name: 'Add book' })
    expect(addLinks.length).toBeGreaterThan(0)
    addLinks.forEach((link) => expect(link.getAttribute('href')).toBe('/books/new'))
  })
})

describe('AppChrome account menu', () => {
  it('opens with the profile link and log out', () => {
    const { logout } = renderChrome()
    expect(accountButton().getAttribute('aria-expanded')).toBe('false')

    fireEvent.click(accountButton())

    const panel = controlled(accountButton())
    expect(accountButton().getAttribute('aria-expanded')).toBe('true')
    expect(within(panel).getByRole('link', { name: 'My profile' }).getAttribute('href')).toBe('/profile')
    fireEvent.click(within(panel).getByRole('button', { name: 'Log out' }))
    expect(logout).toHaveBeenCalledTimes(1)
  })

  it('closes on Escape and gives focus back to the avatar button', () => {
    renderChrome()
    fireEvent.click(accountButton())

    fireEvent.keyDown(document, { key: 'Escape' })

    expect(accountButton().getAttribute('aria-expanded')).toBe('false')
    expect(document.activeElement).toBe(accountButton())
  })
})

describe('AppChrome hide on scroll', () => {
  it('hides the header when scrolling down and brings it back when scrolling up', () => {
    renderChrome()
    const header = screen.getByRole('banner')
    expect(header.dataset.hidden).toBe('false')

    scrollTo(400)
    expect(header.dataset.hidden).toBe('true')

    scrollTo(320)
    expect(header.dataset.hidden).toBe('false')
  })

  it('still hides after the reader has clicked something in the header', () => {
    renderChrome()
    fireEvent.click(accountButton())
    fireEvent.click(accountButton())
    accountButton().focus()

    scrollTo(400)

    expect(screen.getByRole('banner').dataset.hidden).toBe('true')
  })

  it('comes back when keyboard focus moves into it', () => {
    renderChrome()
    scrollTo(400)

    fireEvent.focus(accountButton())

    expect(screen.getByRole('banner').dataset.hidden).toBe('false')
  })

  it('keeps the header visible while its menu is open', () => {
    renderChrome()
    fireEvent.click(menuButton())

    scrollTo(400)

    expect(screen.getByRole('banner').dataset.hidden).toBe('false')
  })
})

describe('AppChrome phone menu', () => {
  it('opens with every section, Add book, search and log out', () => {
    renderChrome()
    expect(menuButton().getAttribute('aria-expanded')).toBe('false')

    fireEvent.click(menuButton())

    expect(menuButton().getAttribute('aria-expanded')).toBe('true')
    const menu = controlled(menuButton())
    expect(menu.dataset.open).toBe('true')
    const labels = within(menu).getAllByRole('link').map((link) => link.textContent.trim())
    expect(labels).toEqual(expect.arrayContaining(['Home', 'Library', 'Feed', 'Clubs', 'Collections', 'Add book']))
    expect(within(menu).getByRole('searchbox')).toBeTruthy()
    expect(within(menu).getByRole('button', { name: 'Log out' })).toBeTruthy()
  })

  it('closes on Escape and gives focus back to the menu button', () => {
    renderChrome()
    fireEvent.click(menuButton())

    fireEvent.keyDown(document, { key: 'Escape' })

    expect(menuButton().getAttribute('aria-expanded')).toBe('false')
    expect(document.activeElement).toBe(menuButton())
  })

  it('closes after following a link', () => {
    renderChrome('/feed')
    fireEvent.click(menuButton())

    fireEvent.click(within(controlled(menuButton())).getByRole('link', { name: 'Clubs' }))

    expect(menuButton().getAttribute('aria-expanded')).toBe('false')
  })
})

describe('AppChrome footer', () => {
  const footerNav = () => screen.getByRole('navigation', { name: 'Footer' })
  const group = (heading) => within(footerNav()).getByRole('list', { name: heading })

  it('groups its links under Explore, Community and Your shelf', () => {
    renderChrome()

    const names = (heading) => within(group(heading)).getAllByRole('link').map((link) => link.textContent)
    expect(names('Explore')).toEqual(['Home', 'Library', 'Collections'])
    expect(names('Community')).toEqual(['Feed', 'Clubs'])
    expect(names('Your shelf')).toEqual(['My profile', 'Add a book'])
    expect(within(group('Your shelf')).getByRole('link', { name: 'Add a book' }).getAttribute('href')).toBe('/books/new')
  })

  it('scrolls back to the top of the page', () => {
    renderChrome()
    const scrollSpy = vi.spyOn(window, 'scrollTo').mockImplementation(() => {})

    fireEvent.click(screen.getByRole('button', { name: 'Back to top' }))

    expect(scrollSpy).toHaveBeenCalledWith(expect.objectContaining({ top: 0 }))
    scrollSpy.mockRestore()
  })
})
