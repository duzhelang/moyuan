import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '@/types/model'
import { login as loginApi, register as registerApi, getUserInfo, updateUser as updateUserApi, updatePassword as updatePasswordApi, logout as logoutApi, refreshToken as refreshTokenApi } from '@/api/modules/user'
import type { LoginRequest, RegisterRequest, UserUpdateRequest, PasswordUpdateRequest } from '@/types/api'

const TOKEN_KEY = 'token'
const REFRESH_TOKEN_KEY = 'refresh_token'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
  const refreshToken = ref<string>(localStorage.getItem(REFRESH_TOKEN_KEY) || '')
  const userInfo = ref<User | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')
  const avatar = computed(() => userInfo.value?.avatar || '')
  const lastAdminUsername = computed(() => localStorage.getItem('lastAdminUsername') || '')

  if (token.value && !userInfo.value) {
    fetchUserInfo()
  }

  function persistTokens(t: { token: string; refreshToken?: string }) {
    token.value = t.token
    localStorage.setItem(TOKEN_KEY, t.token)
    if (t.refreshToken) {
      refreshToken.value = t.refreshToken
      localStorage.setItem(REFRESH_TOKEN_KEY, t.refreshToken)
    }
  }

  async function login(params: LoginRequest) {
    const response = await loginApi(params)
    persistTokens(response.data)
    await fetchUserInfo()
  }

  async function register(params: RegisterRequest) {
    const response = await registerApi(params)
    persistTokens(response.data)
    await fetchUserInfo()
  }

  async function fetchUserInfo(retryCount = 0) {
    const maxRetries = 2
    
    try {
      const response = await getUserInfo()
      userInfo.value = response.data
      if (response.data.role === 'admin') {
        localStorage.setItem('lastAdminUsername', response.data.username)
      }
    } catch (error: any) {
      const status = error?.response?.status
      const isNetworkError = !error?.response && error?.code !== 'ECONNABORTED'
      const isServerError = status >= 500
      const isTimeout = error?.code === 'ECONNABORTED' || error?.message?.includes('timeout')
      
      if ((isNetworkError || isServerError || isTimeout) && retryCount < maxRetries) {
        console.warn(`获取用户信息失败，${1 + retryCount}/${maxRetries} 次重试...`, error?.message)
        await new Promise(resolve => setTimeout(resolve, 1000 * (retryCount + 1)))
        return fetchUserInfo(retryCount + 1)
      }
      
      if (status === 401) {
        console.warn('Token 已过期，自动退出登录')
        logout()
      } else {
        console.error('获取用户信息失败:', error?.message || error)
      }
    }
  }

  function logout() {
    // 通知后端使 access token 失效（失败不阻塞，前端照常清理）
    try {
      logoutApi()
    } catch (e) {
      console.warn('调用登出接口失败:', e)
    }
    token.value = ''
    refreshToken.value = ''
    userInfo.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
  }

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem(TOKEN_KEY, newToken)
  }

  async function updateUser(data: UserUpdateRequest) {
    const response = await updateUserApi(data)
    userInfo.value = response.data
  }

  async function updatePassword(data: PasswordUpdateRequest) {
    await updatePasswordApi(data)
  }

  return {
    token,
    refreshToken,
    userInfo,
    isLoggedIn,
    username,
    avatar,
    lastAdminUsername,
    login,
    register,
    fetchUserInfo,
    logout,
    setToken,
    updateUser,
    updatePassword
  }
})