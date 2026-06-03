import { describe, it, expect, beforeEach, vi } from 'vitest'
import {
  getItem,
  setItem,
  removeItem,
  clear,
  getToken,
  setToken,
  removeToken
} from './storage'

const mockLocalStorage: Record<string, any> & {
  store: Record<string, string>
} = {
  store: {} as Record<string, string>,
  getItem: vi.fn((key: string) => mockLocalStorage.store[key] || null),
  setItem: vi.fn((key: string, value: string) => {
    mockLocalStorage.store[key] = value
  }),
  removeItem: vi.fn((key: string) => {
    delete mockLocalStorage.store[key]
  }),
  clear: vi.fn(() => {
    mockLocalStorage.store = {}
  }),
  get length() {
    return Object.keys(mockLocalStorage.store).length
  },
  key: vi.fn((index: number) => Object.keys(mockLocalStorage.store)[index] || null)
}

Object.defineProperty(window, 'localStorage', {
  value: mockLocalStorage
})

describe('storage', () => {
  beforeEach(() => {
    mockLocalStorage.store = {}
    vi.clearAllMocks()
  })

  describe('getItem', () => {
    it('获取存在的值', () => {
      mockLocalStorage.store['moyuan_test'] = '{"name":"test"}'
      const result = getItem<{ name: string }>('test')
      expect(result).toEqual({ name: 'test' })
    })

    it('获取不存在的值返回null', () => {
      const result = getItem('nonexistent')
      expect(result).toBeNull()
    })

    it('处理无效的JSON', () => {
      mockLocalStorage.store['moyuan_invalid'] = 'invalid json'
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
      const result = getItem('invalid')
      expect(result).toBeNull()
      expect(consoleSpy).toHaveBeenCalled()
      consoleSpy.mockRestore()
    })
  })

  describe('setItem', () => {
    it('设置值', () => {
      setItem('test', { name: 'test' })
      expect(mockLocalStorage.setItem).toHaveBeenCalledWith(
        'moyuan_test',
        '{"name":"test"}'
      )
    })

    it('设置基本类型值', () => {
      setItem('string', 'hello')
      expect(mockLocalStorage.setItem).toHaveBeenCalledWith(
        'moyuan_string',
        '"hello"'
      )

      setItem('number', 123)
      expect(mockLocalStorage.setItem).toHaveBeenCalledWith(
        'moyuan_number',
        '123'
      )

      setItem('boolean', true)
      expect(mockLocalStorage.setItem).toHaveBeenCalledWith(
        'moyuan_boolean',
        'true'
      )
    })
  })

  describe('removeItem', () => {
    it('删除存在的值', () => {
      removeItem('test')
      expect(mockLocalStorage.removeItem).toHaveBeenCalledWith('moyuan_test')
    })
  })

  describe('clear', () => {
    it('清空所有moyuan前缀的值', () => {
      mockLocalStorage.store = {
        'moyuan_token': 'token123',
        'moyuan_user': '{"id":1}',
        'other_key': 'other_value'
      }

      clear()

      expect(mockLocalStorage.removeItem).toHaveBeenCalledTimes(2)
      expect(mockLocalStorage.removeItem).toHaveBeenCalledWith('moyuan_token')
      expect(mockLocalStorage.removeItem).toHaveBeenCalledWith('moyuan_user')
      expect(mockLocalStorage.removeItem).not.toHaveBeenCalledWith('other_key')
    })
  })

  describe('token操作', () => {
    it('getToken获取token', () => {
      mockLocalStorage.store['moyuan_token'] = '"test-token"'
      const token = getToken()
      expect(token).toBe('test-token')
    })

    it('setToken设置token', () => {
      setToken('new-token')
      expect(mockLocalStorage.setItem).toHaveBeenCalledWith(
        'moyuan_token',
        '"new-token"'
      )
    })

    it('removeToken删除token', () => {
      removeToken()
      expect(mockLocalStorage.removeItem).toHaveBeenCalledWith('moyuan_token')
    })
  })
})