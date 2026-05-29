import type { Router } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { ElMessage } from 'element-plus'

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
    
    if (to.meta.requiresAdmin && userStore.userInfo?.role !== 'admin') {
      ElMessage.error('无权限访问，需要管理员权限')
      next({ name: 'Home' })
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