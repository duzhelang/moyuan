import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useApiCacheStore } from './apiCache'

vi.mock('@/utils/storage', () => ({
  getCache: vi.fn(),
  setCache: vi.fn(),
  removeCache: vi.fn()
}))

describe('useApiCacheStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('默认空缓存', () => {
    const store = useApiCacheStore()
    expect(store.cache).toEqual({})
  })

  describe('getCachedData', () => {
    it('获取缓存数据', () => {
      const store = useApiCacheStore()
      const { getCache } = require('@/utils/storage')
      
      getCache.mockReturnValue({ data: 'test' })
      
      const result = store.getCachedData('testKey')
      
      expect(getCache).toHaveBeenCalledWith('testKey')
      expect(result).toEqual({ data: 'test' })
    })

    it('缓存不存在返回null', () => {
      const store = useApiCacheStore()
      const { getCache } = require('@/utils/storage')
      
      getCache.mockReturnValue(null)
      
      const result = store.getCachedData('nonexistent')
      
      expect(result).toBeNull()
    })
  })

  describe('setCachedData', () => {
    it('设置缓存数据', () => {
      const store = useApiCacheStore()
      const { setCache } = require('@/utils/storage')
      
      const data = { id: 1, name: 'test' }
      store.setCachedData('testKey', data)
      
      expect(setCache).toHaveBeenCalledWith('testKey', data, undefined)
      expect(store.cache['testKey']).toEqual(data)
    })

    it('设置带过期时间的缓存', () => {
      const store = useApiCacheStore()
      const { setCache } = require('@/utils/storage')
      
      const data = { id: 1 }
      store.setCachedData('testKey', data, 3600)
      
      expect(setCache).toHaveBeenCalledWith('testKey', data, 3600)
    })
  })

  describe('removeCachedData', () => {
    it('删除缓存数据', () => {
      const store = useApiCacheStore()
      const { removeCache } = require('@/utils/storage')
      
      store.cache['testKey'] = 'testData'
      store.removeCachedData('testKey')
      
      expect(removeCache).toHaveBeenCalledWith('testKey')
      expect(store.cache['testKey']).toBeUndefined()
    })
  })

  describe('clearCache', () => {
    it('清空所有缓存', () => {
      const store = useApiCacheStore()
      const { removeCache } = require('@/utils/storage')
      
      store.cache = {
        key1: 'data1',
        key2: 'data2',
        key3: 'data3'
      }
      
      store.clearCache()
      
      expect(removeCache).toHaveBeenCalledTimes(3)
      expect(store.cache).toEqual({})
    })

    it('空缓存时清空不报错', () => {
      const store = useApiCacheStore()
      const { removeCache } = require('@/utils/storage')
      
      store.clearCache()
      
      expect(removeCache).not.toHaveBeenCalled()
      expect(store.cache).toEqual({})
    })
  })
})
