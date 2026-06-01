import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAppStore } from './app'

describe('useAppStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('初始状态', () => {
    const store = useAppStore()
    expect(store.cachedViews).toEqual([])
    expect(store.sidebarCollapsed).toBe(false)
    expect(store.loading).toBe(false)
  })

  describe('addCachedView', () => {
    it('添加缓存视图', () => {
      const store = useAppStore()
      store.addCachedView('Home')
      expect(store.cachedViews).toEqual(['Home'])
    })

    it('不重复添加缓存视图', () => {
      const store = useAppStore()
      store.addCachedView('Home')
      store.addCachedView('Home')
      expect(store.cachedViews).toEqual(['Home'])
    })

    it('添加多个缓存视图', () => {
      const store = useAppStore()
      store.addCachedView('Home')
      store.addCachedView('About')
      store.addCachedView('Contact')
      expect(store.cachedViews).toEqual(['Home', 'About', 'Contact'])
    })
  })

  describe('removeCachedView', () => {
    it('移除存在的缓存视图', () => {
      const store = useAppStore()
      store.addCachedView('Home')
      store.addCachedView('About')
      store.removeCachedView('Home')
      expect(store.cachedViews).toEqual(['About'])
    })

    it('移除不存在的缓存视图', () => {
      const store = useAppStore()
      store.addCachedView('Home')
      store.removeCachedView('About')
      expect(store.cachedViews).toEqual(['Home'])
    })

    it('从空列表移除', () => {
      const store = useAppStore()
      store.removeCachedView('Home')
      expect(store.cachedViews).toEqual([])
    })
  })

  describe('clearCachedViews', () => {
    it('清空缓存视图', () => {
      const store = useAppStore()
      store.addCachedView('Home')
      store.addCachedView('About')
      store.clearCachedViews()
      expect(store.cachedViews).toEqual([])
    })

    it('清空空列表', () => {
      const store = useAppStore()
      store.clearCachedViews()
      expect(store.cachedViews).toEqual([])
    })
  })

  describe('toggleSidebar', () => {
    it('切换侧边栏状态', () => {
      const store = useAppStore()
      expect(store.sidebarCollapsed).toBe(false)
      store.toggleSidebar()
      expect(store.sidebarCollapsed).toBe(true)
      store.toggleSidebar()
      expect(store.sidebarCollapsed).toBe(false)
    })
  })

  describe('setLoading', () => {
    it('设置loading状态', () => {
      const store = useAppStore()
      store.setLoading(true)
      expect(store.loading).toBe(true)
      store.setLoading(false)
      expect(store.loading).toBe(false)
    })
  })
})