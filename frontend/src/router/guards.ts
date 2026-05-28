import type { Router } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'

export function setupRouterGuards(router: Router) {
  router.beforeEach(async (to, _from, next) => {
    const userStore = useUserStore()
    
    if (to.meta.title) {
      document.title = `${to.meta.title} - 古今诗话`
    }
    
    if (to.meta.requiresAuth && !userStore.isLoggedIn) {
      next({ name: 'Login', query: { redirect: to.fullPath } })
      return
    }
    
    if ((to.name === 'Login' || to.name === 'Register') && userStore.isLoggedIn) {
      next({ name: 'Home' })
      return
    }
    
    next()
  })
  
  router.afterEach((to) => {
    const appStore = useAppStore()
    if (to.meta.keepAlive && to.name) {
      appStore.addCachedView(to.name as string)
    }
  })
}