import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from './user'

vi.mock('@/api/modules/user', () => ({
  login: vi.fn(),
  register: vi.fn(),
  getUserInfo: vi.fn(),
  updateUser: vi.fn(),
  updatePassword: vi.fn()
}))

describe('useUserStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  describe('初始状态', () => {
    it('默认未登录', () => {
      const store = useUserStore()
      expect(store.token).toBe('')
      expect(store.userInfo).toBeNull()
      expect(store.isLoggedIn).toBe(false)
      expect(store.username).toBe('')
      expect(store.avatar).toBe('')
    })

    it('从localStorage恢复token', () => {
      localStorage.setItem('token', 'test-token')
      const store = useUserStore()
      expect(store.token).toBe('test-token')
      expect(store.isLoggedIn).toBe(true)
    })
  })

  describe('login', () => {
    it('登录成功后设置token和用户信息', async () => {
      const { login, getUserInfo } = await import('@/api/modules/user')
      vi.mocked(login).mockResolvedValue({ data: { token: 'new-token' } })
      vi.mocked(getUserInfo).mockResolvedValue({
        data: { id: 1, username: 'test', nickname: '测试用户', avatar: '', role: 'user' }
      })

      const store = useUserStore()
      await store.login({ username: 'test', password: '123456' })

      expect(store.token).toBe('new-token')
      expect(store.isLoggedIn).toBe(true)
      expect(localStorage.getItem('token')).toBe('new-token')
    })
  })

  describe('register', () => {
    it('注册成功后设置token和用户信息', async () => {
      const { register, getUserInfo } = await import('@/api/modules/user')
      vi.mocked(register).mockResolvedValue({ data: { token: 'reg-token' } })
      vi.mocked(getUserInfo).mockResolvedValue({
        data: { id: 2, username: 'newuser', nickname: '新用户', avatar: '', role: 'user' }
      })

      const store = useUserStore()
      await store.register({ username: 'newuser', password: '123456', email: 'test@test.com' })

      expect(store.token).toBe('reg-token')
      expect(store.isLoggedIn).toBe(true)
    })
  })

  describe('logout', () => {
    it('退出登录清除token和用户信息', () => {
      localStorage.setItem('token', 'test-token')
      const store = useUserStore()
      store.logout()

      expect(store.token).toBe('')
      expect(store.userInfo).toBeNull()
      expect(store.isLoggedIn).toBe(false)
      expect(localStorage.getItem('token')).toBeNull()
    })
  })

  describe('setToken', () => {
    it('设置token并保存到localStorage', () => {
      const store = useUserStore()
      store.setToken('custom-token')

      expect(store.token).toBe('custom-token')
      expect(localStorage.getItem('token')).toBe('custom-token')
    })
  })

  describe('updateUser', () => {
    it('更新用户信息', async () => {
      const { updateUser, getUserInfo } = await import('@/api/modules/user')
      vi.mocked(getUserInfo).mockResolvedValue({
        data: { id: 1, username: 'test', nickname: '旧昵称', avatar: '', role: 'user' }
      })
      vi.mocked(updateUser).mockResolvedValue({
        data: { id: 1, username: 'test', nickname: '新昵称', avatar: '', role: 'user' }
      })

      const store = useUserStore()
      await store.fetchUserInfo()
      await store.updateUser({ nickname: '新昵称' })

      expect(store.userInfo?.nickname).toBe('新昵称')
    })
  })

  describe('computed属性', () => {
    it('isLoggedIn根据token判断', () => {
      const store = useUserStore()
      expect(store.isLoggedIn).toBe(false)

      store.setToken('test-token')
      expect(store.isLoggedIn).toBe(true)
    })

    it('username从userInfo获取', async () => {
      const { getUserInfo } = await import('@/api/modules/user')
      vi.mocked(getUserInfo).mockResolvedValue({
        data: { id: 1, username: 'testuser', nickname: '测试用户', avatar: '', role: 'user' }
      })

      const store = useUserStore()
      await store.fetchUserInfo()

      expect(store.username).toBe('testuser')
    })

    it('avatar从userInfo获取', async () => {
      const { getUserInfo } = await import('@/api/modules/user')
      vi.mocked(getUserInfo).mockResolvedValue({
        data: { id: 1, username: 'test', nickname: '测试', avatar: 'http://test.com/avatar.jpg', role: 'user' }
      })

      const store = useUserStore()
      await store.fetchUserInfo()

      expect(store.avatar).toBe('http://test.com/avatar.jpg')
    })
  })
})