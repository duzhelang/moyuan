import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useSearchHistoryStore } from './searchHistory'

vi.mock('@/utils/storage', () => ({
  getSearchHistory: vi.fn(),
  addSearchHistory: vi.fn(),
  clearSearchHistory: vi.fn()
}))

describe('useSearchHistoryStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('初始状态', () => {
    it('默认值正确', () => {
      const { getSearchHistory } = require('@/utils/storage')
      vi.mocked(getSearchHistory).mockReturnValue([])

      const store = useSearchHistoryStore()
      expect(store.history).toEqual([])
    })
  })

  describe('addHistory', () => {
    it('添加搜索历史', () => {
      const { getSearchHistory, addSearchHistory } = require('@/utils/storage')
      vi.mocked(getSearchHistory).mockReturnValue([])

      const store = useSearchHistoryStore()

      vi.mocked(getSearchHistory).mockReturnValue([
        { keyword: '测试', type: 'all', timestamp: Date.now() }
      ])

      store.addHistory('测试', 'all')

      expect(addSearchHistory).toHaveBeenCalledWith('测试', 'all')
      expect(store.history).toHaveLength(1)
    })

    it('使用默认类型', () => {
      const { getSearchHistory, addSearchHistory } = require('@/utils/storage')
      vi.mocked(getSearchHistory).mockReturnValue([])

      const store = useSearchHistoryStore()
      store.addHistory('测试')

      expect(addSearchHistory).toHaveBeenCalledWith('测试', 'all')
    })
  })

  describe('clearHistory', () => {
    it('清空搜索历史', () => {
      const { getSearchHistory, clearSearchHistory } = require('@/utils/storage')
      vi.mocked(getSearchHistory).mockReturnValue([
        { keyword: '测试', type: 'all', timestamp: Date.now() }
      ])

      const store = useSearchHistoryStore()
      store.clearHistory()

      expect(clearSearchHistory).toHaveBeenCalled()
      expect(store.history).toEqual([])
    })
  })

  describe('getRecentSearches', () => {
    it('获取最近搜索', () => {
      const { getSearchHistory } = require('@/utils/storage')
      const mockHistory = [
        { keyword: '测试1', type: 'all', timestamp: 1 },
        { keyword: '测试2', type: 'poem', timestamp: 2 },
        { keyword: '测试3', type: 'poet', timestamp: 3 }
      ]
      vi.mocked(getSearchHistory).mockReturnValue(mockHistory)

      const store = useSearchHistoryStore()
      const recent = store.getRecentSearches(2)

      expect(recent).toEqual([mockHistory[0], mockHistory[1]])
    })

    it('使用默认限制', () => {
      const { getSearchHistory } = require('@/utils/storage')
      const mockHistory = Array.from({ length: 10 }, (_, i) => ({
        keyword: `测试${i}`,
        type: 'all',
        timestamp: i
      }))
      vi.mocked(getSearchHistory).mockReturnValue(mockHistory)

      const store = useSearchHistoryStore()
      const recent = store.getRecentSearches()

      expect(recent).toHaveLength(5)
    })
  })
})