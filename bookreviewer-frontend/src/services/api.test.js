import { describe, expect, it } from 'vitest'
import api from './api.js'

describe('api query parameters', () => {
  it('sends lists as repeated parameters, the way Spring binds them', () => {
    const url = api.getUri({ url: '/books/filter', params: { genres: ['Dark Academia', 'Classics'], page: 0 } })

    expect(url).toBe('/api/books/filter?genres=Dark+Academia&genres=Classics&page=0')
  })
})
