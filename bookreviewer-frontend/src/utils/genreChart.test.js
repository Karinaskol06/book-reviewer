import { describe, expect, it } from 'vitest'
import { booksLabel, buildGenreRows, genreToneClass, leadingGenreNames, moreGenresLabel } from './genreChart.js'

describe('genreToneClass', () => {
  it('gives each of the first ten genres its own palette tone, in rank order', () => {
    expect(genreToneClass(0)).toBe('genre-tone-0')
    expect(genreToneClass(4)).toBe('genre-tone-4')
    expect(genreToneClass(9)).toBe('genre-tone-9')
  })

  it('gives every genre after the tenth one shared neutral tone instead of repeating colours', () => {
    expect(genreToneClass(10)).toBe('genre-tone-rest')
    expect(genreToneClass(23)).toBe('genre-tone-rest')
  })
})

describe('buildGenreRows', () => {
  it('fills each bar with the share of the shelf that carries the genre', () => {
    const rows = buildGenreRows(
      [
        { name: 'Literary Fiction', bookCount: 4 },
        { name: 'Dark Academia', bookCount: 3 },
      ],
      6,
    )

    expect(rows).toEqual([
      { name: 'Literary Fiction', bookCount: 4, fillPercent: 67, countLabel: '4 books' },
      { name: 'Dark Academia', bookCount: 3, fillPercent: 50, countLabel: '3 books' },
    ])
  })

  it('never fills a bar past the whole shelf', () => {
    const [row] = buildGenreRows([{ name: 'Gothic', bookCount: 7 }], 6)

    expect(row.fillPercent).toBe(100)
  })

  it('returns empty bars instead of NaN when the shelf size is unknown', () => {
    const [row] = buildGenreRows([{ name: 'Gothic', bookCount: 2 }], 0)

    expect(row.fillPercent).toBe(0)
  })

  it('returns no rows for missing data', () => {
    expect(buildGenreRows(undefined, 6)).toEqual([])
  })
})

describe('booksLabel', () => {
  it('uses the singular for one book', () => {
    expect(booksLabel(1)).toBe('1 book')
    expect(booksLabel(4)).toBe('4 books')
  })
})

describe('leadingGenreNames', () => {
  it('names the single genre on the most books', () => {
    const rows = buildGenreRows(
      [
        { name: 'Literary Fiction', bookCount: 4 },
        { name: 'Dark Academia', bookCount: 3 },
      ],
      6,
    )

    expect(leadingGenreNames(rows)).toEqual(['Literary Fiction'])
  })

  it('names both genres when the top two are tied, but never more than two', () => {
    const rows = buildGenreRows(
      [
        { name: 'Gothic', bookCount: 2 },
        { name: 'Horror', bookCount: 2 },
        { name: 'Mystery', bookCount: 2 },
      ],
      3,
    )

    expect(leadingGenreNames(rows)).toEqual(['Gothic', 'Horror'])
  })

  it('names nothing when there are no rows', () => {
    expect(leadingGenreNames([])).toEqual([])
  })
})

describe('moreGenresLabel', () => {
  it('counts the genres left out of the chart', () => {
    expect(moreGenresLabel(11)).toBe('+ 11 more genres')
    expect(moreGenresLabel(1)).toBe('+ 1 more genre')
  })

  it('is empty when every genre is shown', () => {
    expect(moreGenresLabel(0)).toBe('')
    expect(moreGenresLabel(undefined)).toBe('')
  })
})
