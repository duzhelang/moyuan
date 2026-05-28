import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { setupRouterGuards } from './guards'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/home/index.vue'),
        meta: { title: '首页', keepAlive: true }
      },
      {
        path: 'poem',
        name: 'PoemList',
        component: () => import('@/views/poem/list.vue'),
        meta: { title: '诗词列表', keepAlive: true }
      },
      {
        path: 'poem/:id',
        name: 'PoemDetail',
        component: () => import('@/views/poem/detail.vue'),
        meta: { title: '诗词详情' }
      },
      {
        path: 'forum',
        name: 'Forum',
        component: () => import('@/views/forum/list.vue'),
        meta: { title: '诗汇论坛', keepAlive: true }
      },
      {
        path: 'forum/:id',
        name: 'ForumDetail',
        component: () => import('@/views/forum/detail.vue'),
        meta: { title: '帖子详情' }
      }
    ]
  },
  {
    path: '/user',
    children: [
      {
        path: 'login',
        name: 'Login',
        component: () => import('@/views/user/login.vue'),
        meta: { title: '登录' }
      },
      {
        path: 'register',
        name: 'Register',
        component: () => import('@/views/user/register.vue'),
        meta: { title: '注册' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/user/profile.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面未找到' }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0 }
  }
})

setupRouterGuards(router)

export default router