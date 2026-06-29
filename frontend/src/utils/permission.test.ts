import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import {
  hasRole,
  hasAnyRole,
  hasPermission,
  hasAnyPermission,
  hasAllPermissions,
  checkPermission
} from './permission'

vi.mock('@/stores/user', () => ({
  useUserStore: vi.fn()
}))

describe('permission utils', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  describe('hasRole', () => {
    it('用户角色匹配返回true', () => {
      const { useUserStore } = require('@/stores/user')
      useUserStore.mockReturnValue({
        userInfo: { role: 'user' }
      })
      
      expect(hasRole('user')).toBe(true)
    })

    it('用户角色不匹配返回false', () => {
      const { useUserStore } = require('@/stores/user')
      useUserStore.mockReturnValue({
        userInfo: { role: 'user' }
      })
      
      expect(hasRole('admin')).toBe(false)
    })

    it('无用户信息返回false', () => {
      const { useUserStore } = require('@/stores/user')
      useUserStore.mockReturnValue({
        userInfo: null
      })
      
      expect(hasRole('user')).toBe(false)
    })
  })

  describe('hasAnyRole', () => {
    it('用户角色在列表中返回true', () => {
      const { useUserStore } = require('@/stores/user')
      useUserStore.mockReturnValue({
        userInfo: { role: 'user' }
      })
      
      expect(hasAnyRole(['admin', 'user'])).toBe(true)
    })

    it('用户角色不在列表中返回false', () => {
      const { useUserStore } = require('@/stores/user')
      useUserStore.mockReturnValue({
        userInfo: { role: 'guest' }
      })
      
      expect(hasAnyRole(['admin', 'user'])).toBe(false)
    })
  })

  describe('hasPermission', () => {
    it('admin拥有所有管理权限', () => {
      const { useUserStore } = require('@/stores/user')
      useUserStore.mockReturnValue({
        userInfo: { role: 'admin' }
      })
      
      expect(hasPermission('user:manage')).toBe(true)
      expect(hasPermission('poem:manage')).toBe(true)
      expect(hasPermission('system:manage')).toBe(true)
    })

    it('user拥有读取和创建权限', () => {
      const { useUserStore } = require('@/stores/user')
      useUserStore.mockReturnValue({
        userInfo: { role: 'user' }
      })
      
      expect(hasPermission('poem:read')).toBe(true)
      expect(hasPermission('forum:create')).toBe(true)
      expect(hasPermission('comment:create')).toBe(true)
    })

    it('user没有管理权限', () => {
      const { useUserStore } = require('@/stores/user')
      useUserStore.mockReturnValue({
        userInfo: { role: 'user' }
      })
      
      expect(hasPermission('user:manage')).toBe(false)
      expect(hasPermission('system:manage')).toBe(false)
    })

    it('guest只有读取权限', () => {
      const { useUserStore } = require('@/stores/user')
      useUserStore.mockReturnValue({
        userInfo: { role: 'guest' }
      })
      
      expect(hasPermission('poem:read')).toBe(true)
      expect(hasPermission('forum:read')).toBe(true)
      expect(hasPermission('forum:create')).toBe(false)
    })

    it('无用户信息返回false', () => {
      const { useUserStore } = require('@/stores/user')
      useUserStore.mockReturnValue({
        userInfo: null
      })
      
      expect(hasPermission('poem:read')).toBe(false)
    })
  })

  describe('hasAnyPermission', () => {
    it('拥有其中一个权限返回true', () => {
      const { useUserStore } = require('@/stores/user')
      useUserStore.mockReturnValue({
        userInfo: { role: 'user' }
      })
      
      expect(hasAnyPermission(['poem:read', 'user:manage'])).toBe(true)
    })

    it('没有任何权限返回false', () => {
      const { useUserStore } = require('@/stores/user')
      useUserStore.mockReturnValue({
        userInfo: { role: 'guest' }
      })
      
      expect(hasAnyPermission(['forum:create', 'user:manage'])).toBe(false)
    })
  })

  describe('hasAllPermissions', () => {
    it('拥有所有权限返回true', () => {
      const { useUserStore } = require('@/stores/user')
      useUserStore.mockReturnValue({
        userInfo: { role: 'user' }
      })
      
      expect(hasAllPermissions(['poem:read', 'forum:create'])).toBe(true)
    })

    it('缺少部分权限返回false', () => {
      const { useUserStore } = require('@/stores/user')
      useUserStore.mockReturnValue({
        userInfo: { role: 'user' }
      })
      
      expect(hasAllPermissions(['poem:read', 'user:manage'])).toBe(false)
    })
  })

  describe('checkPermission', () => {
    it('角色和权限都满足时返回true', () => {
      const { useUserStore } = require('@/stores/user')
      useUserStore.mockReturnValue({
        userInfo: { role: 'user' }
      })
      
      const config = {
        roles: ['user'],
        permissions: ['poem:read']
      }
      
      expect(checkPermission(config)).toBe(true)
    })

    it('角色不满足返回false', () => {
      const { useUserStore } = require('@/stores/user')
      useUserStore.mockReturnValue({
        userInfo: { role: 'guest' }
      })
      
      const config = {
        roles: ['admin'],
        permissions: ['poem:read']
      }
      
      expect(checkPermission(config)).toBe(false)
    })

    it('权限不满足返回false', () => {
      const { useUserStore } = require('@/stores/user')
      useUserStore.mockReturnValue({
        userInfo: { role: 'user' }
      })
      
      const config = {
        roles: ['user'],
        permissions: ['user:manage']
      }
      
      expect(checkPermission(config)).toBe(false)
    })

    it('requireAll为true时需要所有权限', () => {
      const { useUserStore } = require('@/stores/user')
      useUserStore.mockReturnValue({
        userInfo: { role: 'user' }
      })
      
      const config = {
        permissions: ['poem:read', 'forum:create'],
        requireAll: true
      }
      
      expect(checkPermission(config)).toBe(true)
    })

    it('无配置返回true', () => {
      const { useUserStore } = require('@/stores/user')
      useUserStore.mockReturnValue({
        userInfo: { role: 'user' }
      })
      
      expect(checkPermission({})).toBe(true)
    })
  })
})
