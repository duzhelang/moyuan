import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '@/utils/request'
import { getRandomPoetFeatured, getPoetFeaturedList, getPoetFeaturedDetail } from './poetFeatured'

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn()
  }
}))

describe('poetFeatured API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('getRandomPoetFeatured', () => {
    it('调用GET /poet-featured/random', async () => {
      await getRandomPoetFeatured(4)
      expect(request.get).toHaveBeenCalledWith('/poet-featured/random', {
        params: { count: 4 }
      })
    })

    it('使用默认参数', async () => {
      await getRandomPoetFeatured()
      expect(request.get).toHaveBeenCalledWith('/poet-featured/random', {
        params: { count: 4 }
      })
    })
  })

  describe('getPoetFeaturedList', () => {
    it('调用GET /poet-featured/list', async () => {
      await getPoetFeaturedList()
      expect(request.get).toHaveBeenCalledWith('/poet-featured/list')
    })
  })

  describe('getPoetFeaturedDetail', () => {
    it('调用GET /poet-featured/:id', async () => {
      await getPoetFeaturedDetail(1)
      expect(request.get).toHaveBeenCalledWith('/poet-featured/1')
    })
  })
})