// @vitest-environment jsdom
import { afterEach, describe, expect, it } from 'vitest'
import { clearRecentSearches, highlightParts, matchGenres, readRecentSearches, rememberSearch } from './search.js'

afterEach(() => localStorage.clear())

describe('highlightParts', () => {
  it('marks every case-insensitive match of the query', () => {
    expect(highlightParts('Secret Seminar', 'se')).toEqual([
      { text: 'Se', match: true },
      { text: 'cret ', match: false },
      { text: 'Se', match: true },
      { text: 'minar', match: false },
    ])
  })

  it('returns the whole text unmarked when the query is empty or absent', () => {
    expect(highlightParts('Babel', '')).toEqual([{ text: 'Babel', match: false }])
    expect(highlightParts('Babel', 'xyz')).toEqual([{ text: 'Babel', match: false }])
  })

  it('treats regex characters in the query literally', () => {
    expect(highlightParts('C++ (2nd ed.)', '(2')).toEqual([
      { text: 'C++ ', match: false },
      { text: '(2', match: true },
      { text: 'nd ed.)', match: false },
    ])
  })
})

describe('matchGenres', () => {
  const genres = ['Classics', 'Dark Academia', 'Fantasy', 'Science Fiction', 'Historical Fiction', 'Fiction']

  it('puts genres that start with the query before ones that only contain it', () => {
    expect(matchGenres(genres, 'fic')).toEqual(['Fiction', 'Historical Fiction', 'Science Fiction'])
  })

  it('ignores case, accents, hyphens and extra spaces', () => {
    expect(matchGenres(['Sci-Fi', 'Dark Academia'], 'sci fi')).toEqual(['Sci-Fi'])
    expect(matchGenres(genres, '  DARK  acad')).toEqual(['Dark Academia'])
  })

  it('respects the limit and returns nothing for a blank query', () => {
    expect(matchGenres(genres, 'i', 2)).toHaveLength(2)
    expect(matchGenres(genres, '   ')).toEqual([])
  })
})

describe('recent searches', () => {
  it('keeps the newest first, without duplicates, at most five', () => {
    ;['babel', 'gatsby', 'Babel', 'one', 'two', 'three', 'four'].forEach(rememberSearch)

    expect(readRecentSearches()).toEqual(['four', 'three', 'two', 'one', 'Babel'])
  })

  it('ignores blank searches and can be cleared', () => {
    rememberSearch('   ')
    expect(readRecentSearches()).toEqual([])

    rememberSearch('tartt')
    clearRecentSearches()
    expect(readRecentSearches()).toEqual([])
  })

  it('survives unreadable stored data', () => {
    localStorage.setItem('bookreviewer.recentSearches', '{not json')

    expect(readRecentSearches()).toEqual([])
  })
})
