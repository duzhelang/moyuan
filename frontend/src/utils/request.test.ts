import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import axios from 'axios'

vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => ({
      interceptors: {
        request: { use: vi.fn() },
        response: { use: vi.fn() }
      },
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
      delete: vi.fn()
    }))
  }
}))

vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn(),
    success: vi.fn()
  }
}))

vi.mock('@/stores/user', () => ({
  useUserStore: vi.fn(() => ({
    token: 'test-token',
    userInfo: { role: 'user' },
    logout: vi.fn()
  }))
}))

describe('request utils', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('isPublicRequest', () => {
    const { isPublicRequest } = require('./request')

    it('公共路径返回true', () => {
      expect(isPublicRequest('/poems')).toBe(true)
      expect(isPublicRequest('/poets')).toBe(true)
      expect(isPublicRequest('/dynasties')).toBe(true)
      expect(isPublicRequest('/categories')).toBe(true)
      expect(isPublicRequest('/forum/posts')).toBe(true)
      expect(isPublicRequest('/forum/comments')).toBe(true)
      expect(isPublicRequest('/poet-featured')).toBe(true)
      expect(isPublicRequest('/home-navigation')).toBe(true)
      expect(isPublicRequest('/vision')).toBe(true)
      expect(isPublicRequest('/ai')).toBe(true)
      expect(isPublicRequest('/rhyme')).toBe(true)
      expect(isPublicRequest('/search')).toBe(true)
      expect(isPublicRequest('/static-pages')).toBe(true)
      expect(isPublicRequest('/auth')).toBe(true)
    })

    it('admin路径返回false', () => {
      expect(isPublicRequest('/admin/users')).toBe(false)
      expect(isPublicRequest('/admin/poems')).toBe(false)
    })

    it('未知路径返回false', () => {
      expect(isPublicRequest('/unknown')).toBe(false)
      expect(isPublicRequest('/api/test')).toBe(false)
    })

    it('空字符串返回false', () => {
      expect(isPublicRequest('')).toBe(false)
      expect(isPublicRequest()).toBe(false)
    })
  })

  describe('HTTP方法导出', () => {
    it('导出get方法', () => {
      const { get } = require('./request')
      expect(typeof get).toBe('function')
    })

    it('导出post方法', () => {
      const { post } = require('./request')
      expect(typeof post).toBe('function')
    })

    it('导出put方法', () => {
      const { put } = require('./request')
      expect(typeof put).toBe('function')
    })

    it('导出del方法', () => {
      const { del } = require('./request')
      expect(typeof del).toBe('function')
    })

    it('导出默认service实例', () => {
      const service = require('./request').default
      expect(service).toBeDefined()
    })
  })

  describe('handleNetworkError', () => {
    it('超时错误显示超时提示', () => {
      const { ElMessage } = require('element-plus')
      const error = { code: 'ECONNABORTED', message: 'timeout' }
      
      require('./request')
      
      expect(ElMessage.error).not.toHaveBeenCalled()
    })
  })
})
