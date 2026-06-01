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
        path: 'poet',
        name: 'PoetList',
        component: () => import('@/views/poet/list.vue'),
        meta: { title: '诗人风采', keepAlive: true }
      },
      {
        path: 'poet/:id',
        name: 'PoetDetail',
        component: () => import('@/views/poet/detail.vue'),
        meta: { title: '诗人详情' }
      },
      {
        path: 'forum',
        name: 'Forum',
        component: () => import('@/views/forum/list.vue'),
        meta: { title: '诗汇论坛', keepAlive: true }
      },
      {
        path: 'forum/create',
        name: 'ForumCreate',
        component: () => import('@/views/forum/create.vue'),
        meta: { title: '发布新帖', requiresAuth: true }
      },
      {
        path: 'forum/:id',
        name: 'ForumDetail',
        component: () => import('@/views/forum/detail.vue'),
        meta: { title: '帖子详情' }
      },
      {
        path: 'search',
        name: 'Search',
        component: () => import('@/views/search/index.vue'),
        meta: { title: '搜索结果' }
      },
      {
        path: 'vision',
        name: 'VisionList',
        component: () => import('@/views/vision/list.vue'),
        meta: { title: '诗话视野', keepAlive: true }
      },
      {
        path: 'vision/:id',
        name: 'VisionDetail',
        component: () => import('@/views/vision/detail.vue'),
        meta: { title: '文章详情' }
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
    path: '/admin',
    component: () => import('@/views/admin/index.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: '',
        redirect: '/admin/dashboard'
      },
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/dashboard.vue'),
        meta: { title: '控制台', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/users.vue'),
        meta: { title: '用户管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'poems',
        name: 'AdminPoems',
        component: () => import('@/views/admin/poems.vue'),
        meta: { title: '诗词管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'categories',
        name: 'AdminCategories',
        component: () => import('@/views/admin/categories.vue'),
        meta: { title: '分类管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'dynasties',
        name: 'AdminDynasties',
        component: () => import('@/views/admin/dynasties.vue'),
        meta: { title: '朝代管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'poets',
        name: 'AdminPoets',
        component: () => import('@/views/admin/poets.vue'),
        meta: { title: '诗人管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'poet-featured',
        name: 'AdminPoetFeatured',
        component: () => import('@/views/admin/poet-featured.vue'),
        meta: { title: '精选诗人管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'forum-posts',
        name: 'AdminForumPosts',
        component: () => import('@/views/admin/forum-posts.vue'),
        meta: { title: '帖子管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'logs',
        name: 'AdminLogs',
        component: () => import('@/views/admin/logs.vue'),
        meta: { title: '操作日志', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'ai-models',
        name: 'AdminAiModels',
        component: () => import('@/views/admin/ai-models.vue'),
        meta: { title: 'AI模型管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'home-navigation',
        name: 'AdminHomeNavigation',
        component: () => import('@/views/admin/home-navigation.vue'),
        meta: { title: '首页导航管理', requiresAuth: true, requiresAdmin: true }
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