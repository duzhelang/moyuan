import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '@/utils/request'
import {
  searchPoemsWithRecommend,
  getRecommendedPoems,
  getPopularPoems,
  getExternalPoems,
  searchApihzPoems,
  getApihzPoetryDetail
} from './external-poetry'

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn()
  }
}))

describe('external-poetry API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('searchPoemsWithRecommend', () => {
    it('调用GET /search/poems', async () => {
      vi.mocked(request.get).mockResolvedValue({ data: [] })
      const result = await searchPoemsWithRecommend('测试', 1, 10)
      expect(request.get).toHaveBeenCalledWith('/search/poems', {
        params: { keyword: '测试', page: 1, size: 10 }
      })
      expect(result).toEqual([])
    })

    it('使用默认参数', async () => {
      vi.mocked(request.get).mockResolvedValue({ data: [] })
      await searchPoemsWithRecommend('测试')
      expect(request.get).toHaveBeenCalledWith('/search/poems', {
        params: { keyword: '测试', page: 1, size: 10 }
      })
    })
  })

  describe('getRecommendedPoems', () => {
    it('调用GET /search/poems/recommended', async () => {
      vi.mocked(request.get).mockResolvedValue({ data: [] })
      const result = await getRecommendedPoems(10)
      expect(request.get).toHaveBeenCalledWith('/search/poems/recommended', {
        params: { limit: 10 }
      })
      expect(result).toEqual([])
    })

    it('使用默认参数', async () => {
      vi.mocked(request.get).mockResolvedValue({ data: [] })
      await getRecommendedPoems()
      expect(request.get).toHaveBeenCalledWith('/search/poems/recommended', {
        params: { limit: 10 }
      })
    })
  })

  describe('getPopularPoems', () => {
    it('调用GET /search/poems/popular', async () => {
      vi.mocked(request.get).mockResolvedValue({ data: [] })
      const result = await getPopularPoems(10)
      expect(request.get).toHaveBeenCalledWith('/search/poems/popular', {
        params: { limit: 10 }
      })
      expect(result).toEqual([])
    })

    it('使用默认参数', async () => {
      vi.mocked(request.get).mockResolvedValue({ data: [] })
      await getPopularPoems()
      expect(request.get).toHaveBeenCalledWith('/search/poems/popular', {
        params: { limit: 10 }
      })
    })
  })

  describe('getExternalPoems', () => {
    it('调用GET /search/poems/external', async () => {
      vi.mocked(request.get).mockResolvedValue({ data: [] })
      const result = await getExternalPoems('测试', 5)
      expect(request.get).toHaveBeenCalledWith('/search/poems/external', {
        params: { keyword: '测试', limit: 5 }
      })
      expect(result).toEqual([])
    })

    it('使用默认参数', async () => {
      vi.mocked(request.get).mockResolvedValue({ data: [] })
      await getExternalPoems('测试')
      expect(request.get).toHaveBeenCalledWith('/search/poems/external', {
        params: { keyword: '测试', limit: 5 }
      })
    })
  })

  describe('searchApihzPoems', () => {
    it('调用GET /search/poems/external', async () => {
      vi.mocked(request.get).mockResolvedValue({ data: [] })
      const result = await searchApihzPoems('测试', 10)
      expect(request.get).toHaveBeenCalledWith('/search/poems/external', {
        params: { keyword: '测试', limit: 10 }
      })
      expect(result).toEqual([])
    })
  })

  describe('getApihzPoetryDetail', () => {
    it('调用GET /search/poems/external/detail', async () => {
      vi.mocked(request.get).mockResolvedValue({ data: null })
      const result = await getApihzPoetryDetail('静夜思')
      expect(request.get).toHaveBeenCalledWith('/search/poems/external/detail', {
        params: { keyword: '静夜思' }
      })
      expect(result).toBeNull()
    })
  })
})