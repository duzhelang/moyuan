import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '@/utils/request'
import {
  smartSearch,
  getSearchSuggestions,
  getHotSearches,
  getSearchHistory,
  clearSearchHistory
} from './search'

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    delete: vi.fn()
  }
}))

describe('search API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('smartSearch', () => {
    it('调用GET /search/smart', async () => {
      const params = { keyword: '测试', pageNum: 1, pageSize: 10 }
      vi.mocked(request.get).mockResolvedValue({ data: { list: [], total: 0 } })
      await smartSearch(params)
      expect(request.get).toHaveBeenCalledWith('/search/smart', { params })
    })
  })

  describe('getSearchSuggestions', () => {
    it('调用GET /search/suggestions', async () => {
      vi.mocked(request.get).mockResolvedValue({ data: ['建议1', '建议2'] })
      const result = await getSearchSuggestions('测试', 10)
      expect(request.get).toHaveBeenCalledWith('/search/suggestions', {
        params: { keyword: '测试', limit: 10 }
      })
      expect(result).toEqual(['建议1', '建议2'])
    })

    it('使用默认参数', async () => {
      vi.mocked(request.get).mockResolvedValue({ data: [] })
      await getSearchSuggestions('测试')
      expect(request.get).toHaveBeenCalledWith('/search/suggestions', {
        params: { keyword: '测试', limit: 10 }
      })
    })
  })

  describe('getHotSearches', () => {
    it('调用GET /search/hot', async () => {
      vi.mocked(request.get).mockResolvedValue({ data: ['热门1', '热门2'] })
      const result = await getHotSearches(8)
      expect(request.get).toHaveBeenCalledWith('/search/hot', {
        params: { limit: 8 }
      })
      expect(result).toEqual(['热门1', '热门2'])
    })

    it('使用默认参数', async () => {
      vi.mocked(request.get).mockResolvedValue({ data: [] })
      await getHotSearches()
      expect(request.get).toHaveBeenCalledWith('/search/hot', {
        params: { limit: 8 }
      })
    })
  })

  describe('getSearchHistory', () => {
    it('调用GET /search/history', async () => {
      vi.mocked(request.get).mockResolvedValue({ data: ['历史1', '历史2'] })
      const result = await getSearchHistory(10)
      expect(request.get).toHaveBeenCalledWith('/search/history', {
        params: { limit: 10 }
      })
      expect(result).toEqual(['历史1', '历史2'])
    })

    it('使用默认参数', async () => {
      vi.mocked(request.get).mockResolvedValue({ data: [] })
      await getSearchHistory()
      expect(request.get).toHaveBeenCalledWith('/search/history', {
        params: { limit: 10 }
      })
    })
  })

  describe('clearSearchHistory', () => {
    it('调用DELETE /search/history', async () => {
      vi.mocked(request.delete).mockResolvedValue(undefined)
      await clearSearchHistory()
      expect(request.delete).toHaveBeenCalledWith('/search/history')
    })
  })
})