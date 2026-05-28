import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '@/types/model'
import { login as loginApi, getUserInfo, logout as logoutApi, updateUser as updateUserApi, updatePassword as updatePasswordApi } from '@/api/modules/user'
import type { LoginRequest, UserUpdateRequest, PasswordUpdateRequest } from '@/types/api'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<User | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')
  const avatar = computed(() => userInfo.value?.avatar || '')

  async function login(params: LoginRequest) {
    const response = await loginApi(params)
    token.value = response.data.token
    localStorage.setItem('token', response.data.token)
    await fetchUserInfo()
  }

  async function fetchUserInfo() {
    try {
      const response = await getUserInfo()
      userInfo.value = response.data
    } catch (error) {
      console.error('获取用户信息失败:', error)
      logout()
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
    login,
    fetchUserInfo,
    logout,
    setToken,
    updateUser,
    updatePassword
  }
})