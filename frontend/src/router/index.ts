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
        meta: { title: '首页', keepAlive: true, hideHeader: true }
      },
      {
        path: 'poem',
        name: 'PoemList',
        component: () => import('@/views/poem/list.vue'),
        meta: { title: '诗词列表', keepAlive: true }
      },
      {
        path: 'poem/create',
        name: 'PoemCreate',
        component: () => import('@/views/poem/create.vue'),
        meta: { title: '发布新诗', requiresAuth: true }
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
        path: 'communicate',
        name: 'Communicate',
        component: () => import('@/views/communicate/index.vue'),
        meta: { title: '交流广场', keepAlive: true }
      },
      {
        path: 'poetry-cloud',
        name: 'PoetryCloud',
        component: () => import('@/views/poetry-cloud/index.vue'),
        meta: { title: '诗云星空', hideHeader: true }
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
      },
      {
        path: 'user/:id/homepage',
        name: 'UserHomepage',
        component: () => import('@/views/user/homepage.vue'),
        meta: { title: '用户主页' }
      },
      {
        path: 'repair',
        name: 'RepairList',
        component: () => import('@/views/repair/list.vue'),
        meta: { title: '报修中心', requiresAuth: true }
      },
      {
        path: 'repair/create',
        name: 'RepairCreate',
        component: () => import('@/views/repair/create.vue'),
        meta: { title: '提交报修', requiresAuth: true }
      },
      {
        path: 'repair/:id',
        name: 'RepairDetail',
        component: () => import('@/views/repair/detail.vue'),
        meta: { title: '报修详情', requiresAuth: true }
      },
      {
        path: 'contact',
        name: 'Contact',
        component: () => import('@/views/contact/index.vue'),
        meta: { title: '联系我们' }
      },
      {
        path: 'static/:type',
        name: 'StaticPage',
        component: () => import('@/views/static/index.vue'),
        meta: { title: '页面信息' }
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
        path: 'poet-suggestions',
        name: 'AdminPoetSuggestions',
        component: () => import('@/views/admin/poet-suggestions.vue'),
        meta: { title: '诗人内容审核', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'ai-contents',
        name: 'AdminAiContents',
        component: () => import('@/views/admin/ai-contents.vue'),
        meta: { title: 'AI内容审核', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'poem-audit',
        name: 'PoemAudit',
        component: () => import('@/views/admin/poem-audit.vue'),
        meta: { title: '诗词审核', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'poet-profiles',
        name: 'PoetProfiles',
        component: () => import('@/views/admin/poet-profiles.vue'),
        meta: { title: '诗人认证审核', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'comments',
        name: 'Comments',
        component: () => import('@/views/admin/comments.vue'),
        meta: { title: '评论管理', requiresAuth: true, requiresAdmin: true }
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
      },
      {
        path: 'repairs',
        name: 'AdminRepairs',
        component: () => import('@/views/admin/repairs.vue'),
        meta: { title: '报修管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'static-pages',
        name: 'AdminStaticPages',
        component: () => import('@/views/admin/static-pages.vue'),
        meta: { title: '静态页面管理', requiresAuth: true, requiresAdmin: true }
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
  scrollBehavior(_to, _from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0 }
  }
})

setupRouterGuards(router)

export default router