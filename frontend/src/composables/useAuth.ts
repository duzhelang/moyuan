import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

export function useAuth() {
  const router = useRouter()
  const userStore = useUserStore()

  const isLoggedIn = computed(() => userStore.isLoggedIn)
  const isAdmin = computed(() => userStore.userInfo?.role === 'admin')
  const username = computed(() => userStore.username)
  const avatar = computed(() => userStore.avatar)
  const userInfo = computed(() => userStore.userInfo)

  async function login(username: string, password: string) {
    await userStore.login({ username, password })
  }

  function logout() {
    userStore.logout()
    router.push('/')
  }

  function requireAuth() {
    if (!isLoggedIn.value) {
      router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } })
      return false
    }
    return true
  }

  return {
    isLoggedIn,
    isAdmin,
    username,
    avatar,
    userInfo,
    login,
    logout,
    requireAuth
  }
}