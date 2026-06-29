import { describe, it, expect, vi, beforeEach } from 'vitest'
import request from '@/utils/request'
import {
  login,
  register,
  getUserInfo,
  updateUser,
  updatePassword,
  logout,
  getUserList,
  getMyPosts,
  getUserStats,
  getUserProfile,
  getUserWorks
} from './user'

vi.mock('@/utils/request', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn()
  }
}))

describe('user API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('login', () => {
    it('调用POST /auth/login', async () => {
      const data = { username: 'test', password: '123456' }
      await login(data)
      expect(request.post).toHaveBeenCalledWith('/auth/login', data)
    })
  })

  describe('register', () => {
    it('调用POST /auth/register', async () => {
      const data = { username: 'newuser', password: '123456', email: 'test@example.com' }
      await register(data)
      expect(request.post).toHaveBeenCalledWith('/auth/register', data)
    })
  })

  describe('getUserInfo', () => {
    it('调用GET /users/me', async () => {
      await getUserInfo()
      expect(request.get).toHaveBeenCalledWith('/users/me')
    })
  })

  describe('updateUser', () => {
    it('调用PUT /users/me', async () => {
      const data = { nickname: '新昵称' }
      await updateUser(data)
      expect(request.put).toHaveBeenCalledWith('/users/me', data)
    })
  })

  describe('updatePassword', () => {
    it('调用PUT /users/me/password', async () => {
      const data = { oldPassword: 'old', newPassword: 'new' }
      await updatePassword(data)
      expect(request.put).toHaveBeenCalledWith('/users/me/password', data)
    })
  })

  describe('logout', () => {
    it('返回Promise.resolve', async () => {
      const result = await logout()
      expect(result).toBeUndefined()
    })
  })

  describe('getUserList', () => {
    it('调用GET /admin/users', async () => {
      const params = { page: 1, size: 10, keyword: 'test' }
      await getUserList(params)
      expect(request.get).toHaveBeenCalledWith('/admin/users', { params })
    })
  })

  describe('getMyPosts', () => {
    it('调用GET /users/me/posts', async () => {
      await getMyPosts({ pageNum: 1, pageSize: 10 })
      expect(request.get).toHaveBeenCalledWith('/users/me/posts', {
        params: { pageNum: 1, pageSize: 10 }
      })
    })

    it('无参数时调用', async () => {
      await getMyPosts()
      expect(request.get).toHaveBeenCalledWith('/users/me/posts', { params: undefined })
    })
  })

  describe('getUserStats', () => {
    it('调用GET /users/me/stats', async () => {
      await getUserStats()
      expect(request.get).toHaveBeenCalledWith('/users/me/stats')
    })
  })

  describe('getUserProfile', () => {
    it('调用GET /users/:id/profile', async () => {
      await getUserProfile(123)
      expect(request.get).toHaveBeenCalledWith('/users/123/profile')
    })
  })

  describe('getUserWorks', () => {
    it('调用GET /users/:id/works', async () => {
      await getUserWorks(123, 1, 10)
      expect(request.get).toHaveBeenCalledWith('/users/123/works', {
        params: { pageNum: 1, pageSize: 10 }
      })
    })

    it('使用默认参数', async () => {
      await getUserWorks(123)
      expect(request.get).toHaveBeenCalledWith('/users/123/works', {
        params: { pageNum: 1, pageSize: 10 }
      })
    })
  })
})