import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '@/types/model'
import { login as loginApi, register as registerApi, getUserInfo, updateUser as updateUserApi, updatePassword as updatePasswordApi } from '@/api/modules/user'
import type { LoginRequest, RegisterRequest, UserUpdateRequest, PasswordUpdateRequest } from '@/types/api'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<User | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')
  const avatar = computed(() => userInfo.value?.avatar || '')
  const lastAdminUsername = computed(() => localStorage.getItem('lastAdminUsername') || '')

  if (token.value && !userInfo.value) {
    fetchUserInfo()
  }

  async function login(params: LoginRequest) {
    const response = await loginApi(params)
    token.value = response.data.token
    localStorage.setItem('token', response.data.token)
    await fetchUserInfo()
  }

  async function register(params: RegisterRequest) {
    const response = await registerApi(params)
    token.value = response.data.token
    localStorage.setItem('token', response.data.token)
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
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
  }

  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
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