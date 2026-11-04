// @vitest-environment jsdom
import { afterEach, describe, expect, it } from 'vitest'
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import TopGenresChart from './TopGenresChart.jsx'

afterEach(cleanup)

const top = [
  { name: 'Literary Fiction', bookCount: 4 },
  { name: 'Dark Academia', bookCount: 3 },
  { name: 'Classics', bookCount: 2 },
  { name: 'Fantasy', bookCount: 2 },
  { name: 'Science Fiction', bookCount: 2 },
]
const profile = {
  sampleSize: 6,
  topGenres: top,
  allGenres: [...top, { name: 'Mystery', bookCount: 2 }, { name: 'Gothic', bookCount: 1 }],
  otherGenreCount: 2,
}

const openDialog = () => {
  fireEvent.click(screen.getByRole('button', { name: '+ 2 more genres' }))
  return screen.getByRole('dialog', { name: 'All genres' })
}

describe('TopGenresChart', () => {
  it('shows the top genres with how many books carry each', () => {
    render(<TopGenresChart tasteProfile={profile} isOwn />)

    const list = screen.getByRole('list', { name: 'Top genres by number of books' })
    const rows = within(list).getAllByRole('listitem')
    expect(rows).toHaveLength(5)
    expect(within(rows[0]).getByText('Literary Fiction')).toBeTruthy()
    expect(within(rows[0]).getByText('4 books')).toBeTruthy()
  })

  it('opens a dialog with a bar for every genre from the "+ 2 more genres" button', () => {
    render(<TopGenresChart tasteProfile={profile} isOwn />)
    expect(screen.queryByRole('dialog')).toBeNull()

    const dialog = openDialog()

    const rows = within(dialog).getAllByRole('listitem')
    expect(rows).toHaveLength(7)
    expect(within(rows[6]).getByText('Gothic')).toBeTruthy()
    expect(within(rows[6]).getByText('1 book')).toBeTruthy()
  })

  it('moves focus into the dialog when it opens', () => {
    render(<TopGenresChart tasteProfile={profile} isOwn />)

    const dialog = openDialog()

    expect(document.activeElement).toBe(within(dialog).getByRole('button', { name: 'Close' }))
  })

  it('closes on Escape and gives focus back to the button', () => {
    render(<TopGenresChart tasteProfile={profile} isOwn />)
    openDialog()

    fireEvent.keyDown(document.activeElement, { key: 'Escape' })

    expect(screen.queryByRole('dialog')).toBeNull()
    expect(document.activeElement).toBe(screen.getByRole('button', { name: '+ 2 more genres' }))
  })

  it('closes when the backdrop is clicked, but not when the panel is', () => {
    render(<TopGenresChart tasteProfile={profile} isOwn />)
    const dialog = openDialog()

    fireEvent.click(within(dialog).getByText('Gothic'))
    expect(screen.queryByRole('dialog')).not.toBeNull()

    fireEvent.click(dialog.parentElement)
    expect(screen.queryByRole('dialog')).toBeNull()
  })

  it('keeps Tab inside the dialog', () => {
    render(<TopGenresChart tasteProfile={profile} isOwn />)
    const dialog = openDialog()
    const closeButton = within(dialog).getByRole('button', { name: 'Close' })
    const scrollList = within(dialog).getByRole('list')

    scrollList.focus()
    fireEvent.keyDown(scrollList, { key: 'Tab' })
    expect(document.activeElement).toBe(closeButton)

    fireEvent.keyDown(closeButton, { key: 'Tab', shiftKey: true })
    expect(document.activeElement).toBe(scrollList)
  })

  it('stops the page behind from scrolling while open', () => {
    render(<TopGenresChart tasteProfile={profile} isOwn />)

    openDialog()
    expect(document.body.style.overflow).toBe('hidden')

    fireEvent.keyDown(document.activeElement, { key: 'Escape' })
    expect(document.body.style.overflow).toBe('')
  })

  it('shows no button when every genre already fits in the chart', () => {
    render(<TopGenresChart tasteProfile={{ ...profile, allGenres: top, otherGenreCount: 0 }} isOwn />)

    expect(screen.queryByRole('button', { name: /more genre/ })).toBeNull()
  })
})
