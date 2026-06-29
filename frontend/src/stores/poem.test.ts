import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { usePoemStore } from './poem'

vi.mock('@/api/modules/poem', () => ({
  getPoemList: vi.fn(),
  getPoemById: vi.fn(),
  getModernPoems: vi.fn()
}))

describe('usePoemStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('初始状态', () => {
    it('默认值正确', () => {
      const store = usePoemStore()
      expect(store.poemList).toEqual([])
      expect(store.currentPoem).toBeNull()
      expect(store.modernPoems).toEqual([])
      expect(store.loading).toBe(false)
      expect(store.total).toBe(0)
      expect(store.currentPage).toBe(1)
      expect(store.pageSize).toBe(10)
    })
  })

  describe('fetchPoemList', () => {
    it('获取诗词列表成功', async () => {
      const { getPoemList } = await import('@/api/modules/poem')
      const mockData = {
        data: {
          list: [{ id: 1, title: '静夜思' }],
          total: 1,
          pageNum: 1,
          pageSize: 10
        }
      }
      vi.mocked(getPoemList).mockResolvedValue(mockData)

      const store = usePoemStore()
      await store.fetchPoemList({ pageNum: 1, pageSize: 10 })

      expect(store.poemList).toEqual([{ id: 1, title: '静夜思' }])
      expect(store.total).toBe(1)
      expect(store.currentPage).toBe(1)
      expect(store.pageSize).toBe(10)
      expect(store.loading).toBe(false)
    })

    it('获取诗词列表失败', async () => {
      const { getPoemList } = await import('@/api/modules/poem')
      vi.mocked(getPoemList).mockRejectedValue(new Error('网络错误'))

      const store = usePoemStore()
      await expect(store.fetchPoemList({ pageNum: 1, pageSize: 10 })).rejects.toThrow('网络错误')
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchPoemDetail', () => {
    it('获取诗词详情成功', async () => {
      const { getPoemById } = await import('@/api/modules/poem')
      const mockPoem = { id: 1, title: '静夜思', content: '床前明月光' }
      vi.mocked(getPoemById).mockResolvedValue({ data: mockPoem })

      const store = usePoemStore()
      await store.fetchPoemDetail(1)

      expect(store.currentPoem).toEqual(mockPoem)
      expect(store.loading).toBe(false)
    })

    it('获取诗词详情失败', async () => {
      const { getPoemById } = await import('@/api/modules/poem')
      vi.mocked(getPoemById).mockRejectedValue(new Error('网络错误'))

      const store = usePoemStore()
      await expect(store.fetchPoemDetail(1)).rejects.toThrow('网络错误')
      expect(store.loading).toBe(false)
    })
  })

  describe('fetchModernPoems', () => {
    it('获取现代诗词成功', async () => {
      const { getModernPoems } = await import('@/api/modules/poem')
      const mockData = {
        data: {
          list: [{ id: 1, title: '现代诗' }],
          total: 1
        }
      }
      vi.mocked(getModernPoems).mockResolvedValue(mockData)

      const store = usePoemStore()
      await store.fetchModernPoems()

      expect(store.modernPoems).toEqual([{ id: 1, title: '现代诗' }])
      expect(store.loading).toBe(false)
    })
  })

  describe('setCurrentPoem', () => {
    it('设置当前诗词', () => {
      const store = usePoemStore()
      const poem = { id: 1, title: '测试' } as any
      store.setCurrentPoem(poem)
      expect(store.currentPoem).toBe(poem)
    })

    it('设置为null', () => {
      const store = usePoemStore()
      store.setCurrentPoem(null)
      expect(store.currentPoem).toBeNull()
    })
  })

  describe('clearCurrentPoem', () => {
    it('清空当前诗词', () => {
      const store = usePoemStore()
      store.setCurrentPoem({ id: 1, title: '测试' } as any)
      store.clearCurrentPoem()
      expect(store.currentPoem).toBeNull()
    })
  })
})