import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { usePreferencesStore } from './preferences'

vi.mock('@/utils/storage', () => ({
  getUserPreferences: vi.fn(() => ({
    theme: 'light',
    language: 'zh',
    fontSize: 14,
    pageSize: 10,
    sidebarCollapsed: false
  })),
  setUserPreferences: vi.fn(),
  getSortPreferences: vi.fn(() => ({
    poem: { field: 'createdAt', order: 'desc' },
    poet: { field: 'name', order: 'asc' }
  })),
  setSortPreferences: vi.fn()
}))

describe('usePreferencesStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('默认用户偏好设置', () => {
    const store = usePreferencesStore()
    expect(store.userPreferences.theme).toBe('light')
    expect(store.userPreferences.language).toBe('zh')
    expect(store.userPreferences.fontSize).toBe(14)
    expect(store.userPreferences.pageSize).toBe(10)
    expect(store.userPreferences.sidebarCollapsed).toBe(false)
  })

  it('默认排序偏好设置', () => {
    const store = usePreferencesStore()
    expect(store.sortPreferences.poem).toEqual({ field: 'createdAt', order: 'desc' })
    expect(store.sortPreferences.poet).toEqual({ field: 'name', order: 'asc' })
  })

  describe('updateUserPreferences', () => {
    it('更新部分用户偏好', () => {
      const store = usePreferencesStore()
      const { setUserPreferences } = require('@/utils/storage')
      
      store.updateUserPreferences({ theme: 'dark' })
      
      expect(store.userPreferences.theme).toBe('dark')
      expect(store.userPreferences.language).toBe('zh')
      expect(setUserPreferences).toHaveBeenCalled()
    })
  })

  describe('updateSortPreferences', () => {
    it('更新部分排序偏好', () => {
      const store = usePreferencesStore()
      const { setSortPreferences } = require('@/utils/storage')
      
      store.updateSortPreferences({ poem: { field: 'title', order: 'asc' } })
      
      expect(store.sortPreferences.poem).toEqual({ field: 'title', order: 'asc' })
      expect(setSortPreferences).toHaveBeenCalled()
    })
  })

  describe('setTheme', () => {
    it('设置主题为暗黑模式', () => {
      const store = usePreferencesStore()
      store.setTheme('dark')
      expect(store.userPreferences.theme).toBe('dark')
    })

    it('设置主题为自动模式', () => {
      const store = usePreferencesStore()
      store.setTheme('auto')
      expect(store.userPreferences.theme).toBe('auto')
    })
  })

  describe('setLanguage', () => {
    it('设置语言为英文', () => {
      const store = usePreferencesStore()
      store.setLanguage('en')
      expect(store.userPreferences.language).toBe('en')
    })
  })

  describe('setFontSize', () => {
    it('设置字体大小', () => {
      const store = usePreferencesStore()
      store.setFontSize(16)
      expect(store.userPreferences.fontSize).toBe(16)
    })
  })

  describe('setPageSize', () => {
    it('设置分页大小', () => {
      const store = usePreferencesStore()
      store.setPageSize(20)
      expect(store.userPreferences.pageSize).toBe(20)
    })
  })

  describe('setSidebarCollapsed', () => {
    it('折叠侧边栏', () => {
      const store = usePreferencesStore()
      store.setSidebarCollapsed(true)
      expect(store.userPreferences.sidebarCollapsed).toBe(true)
    })

    it('展开侧边栏', () => {
      const store = usePreferencesStore()
      store.setSidebarCollapsed(false)
      expect(store.userPreferences.sidebarCollapsed).toBe(false)
    })
  })
})
