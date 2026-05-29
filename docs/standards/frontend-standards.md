# 前端技术规范文档

## 概述

本文档定义了"古今诗话——墨渊"项目前端开发的技术规范，包括代码风格、组件设计、状态管理、路由配置等标准。

## 技术栈

### 核心框架

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue.js | 3.4+ | 渐进式JavaScript框架 |
| TypeScript | 5.x | 类型安全的JavaScript超集 |
| Vite | 5.x | 下一代前端构建工具 |

### 状态管理

| 技术 | 版本 | 说明 |
|------|------|------|
| Pinia | 2.x | Vue官方状态管理库 |

### UI组件库

| 技术 | 版本 | 说明 |
|------|------|------|
| Element Plus | 2.x | Vue 3组件库 |
| @element-plus/icons-vue | 2.x | Element Plus图标库 |

### 工具库

| 技术 | 版本 | 说明 |
|------|------|------|
| Axios | 1.x | HTTP客户端 |
| Vue Router | 4.x | 官方路由管理器 |
| dayjs | 1.x | 日期处理库 |
| lodash-es | 4.x | 工具函数库 |

## 项目结构

```
src/
├── api/                    # API接口定义
│   ├── modules/           # 按模块划分的API
│   │   ├── user.ts        # 用户相关API
│   │   ├── poem.ts        # 诗词相关API
│   │   ├── poet.ts        # 诗人相关API
│   │   ├── forum.ts       # 论坛相关API
│   │   ├── category.ts    # 分类相关API
│   │   ├── dynasty.ts     # 朝代相关API
│   │   ├── file.ts        # 文件上传API
│   │   └── history.ts     # 浏览历史API
│   └── index.ts           # API统一导出
├── assets/                # 静态资源
│   └── styles/           # 全局样式
│       ├── variables.scss # SCSS变量
│       ├── mixins.scss   # SCSS混入
│       └── global.scss   # 全局样式
├── components/            # 公共组件
│   ├── common/           # 通用组件
│   │   ├── AppHeader.vue
│   │   ├── AppFooter.vue
│   │   ├── ErrorMessage.vue
│   │   ├── LoadingSpinner.vue
│   │   └── ImageUpload.vue  # 图片上传组件
│   └── business/         # 业务组件
│       ├── PoemCard.vue
│       └── ForumPost.vue
├── composables/           # 组合式函数
│   ├── useAuth.ts        # 认证相关
│   ├── usePoem.ts        # 诗词相关
│   └── useForum.ts       # 论坛相关
├── layouts/               # 布局组件
│   └── DefaultLayout.vue
├── router/                # 路由配置
│   ├── index.ts          # 路由入口（统一定义所有路由）
│   └── guards.ts         # 路由守卫
├── stores/                # Pinia状态管理
│   ├── index.ts          # Store入口
│   ├── user.ts           # 用户状态
│   ├── poem.ts           # 诗词状态
│   └── app.ts            # 应用状态
├── types/                 # TypeScript类型定义
│   ├── api.d.ts          # API请求/响应类型
│   ├── model.d.ts        # 数据模型类型
│   └── global.d.ts       # 全局类型
├── utils/                 # 工具函数
│   ├── request.ts        # Axios封装
│   ├── storage.ts        # 本地存储
│   ├── format.ts         # 格式化工具
│   └── validate.ts       # 验证工具
├── views/                 # 页面组件
│   ├── home/            # 首页
│   │   └── index.vue    # 首页（已重写，连接后端API）
│   ├── poem/            # 诗词模块
│   │   ├── list.vue     # 诗词列表
│   │   └── detail.vue   # 诗词详情
│   ├── poet/            # 诗人模块
│   │   ├── list.vue     # 诗人列表
│   │   └── detail.vue   # 诗人详情
│   ├── forum/           # 论坛模块
│   │   ├── list.vue     # 帖子列表
│   │   ├── create.vue   # 发帖页
│   │   └── detail.vue   # 帖子详情
│   ├── search/          # 搜索模块
│   │   └── index.vue    # 全局搜索结果页
│   ├── user/            # 用户模块
│   │   ├── login.vue    # 登录
│   │   ├── register.vue # 注册
│   │   └── profile.vue  # 个人中心
│   ├── admin/           # 管理模块
│   │   ├── dashboard.vue # 控制台（含ECharts图表）
│   │   ├── users.vue    # 用户管理
│   │   ├── poems.vue    # 诗词管理
│   │   ├── categories.vue # 分类管理
│   │   ├── dynasties.vue # 朝代管理
│   │   ├── poets.vue    # 诗人管理
│   │   ├── forum-posts.vue # 帖子管理
│   │   └── logs.vue     # 操作日志
│   └── error/           # 错误页面
│       └── 404.vue
├── App.vue               # 根组件
├── auto-imports.d.ts     # unplugin-auto-import 自动生成
├── components.d.ts       # unplugin-vue-components 自动生成
└── main.ts               # 入口文件
```

## 代码规范

### TypeScript规范

#### 类型定义

```typescript
// 接口定义使用 interface
interface User {
  id: number
  username: string
  email: string
  avatar?: string
  createTime: string
}

// 类型别名使用 type
type PoemList = Poem[]

// 枚举使用 enum
enum PoemCategory {
  TANG = 'tang',
  SONG = 'song',
  YUAN = 'yuan'
}
```

#### 组件Props定义

```typescript
// 使用 defineProps 配合 TypeScript
interface Props {
  poem: Poem
  showAuthor?: boolean
  maxWidth?: number
}

const props = withDefaults(defineProps<Props>(), {
  showAuthor: true,
  maxWidth: 800
})
```

### Vue组件规范

#### 组件结构顺序

```vue
<script setup lang="ts">
// 1. 导入
import { ref, computed } from 'vue'
import { usePoemStore } from '@/stores/poem'

// 2. 类型定义
interface Props {
  id: number
}

// 3. Props和Emits
const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update', id: number): void
}>()

// 4. 组合式函数
const poemStore = usePoemStore()

// 5. 响应式数据
const loading = ref(false)

// 6. 计算属性
const poem = computed(() => poemStore.getPoemById(props.id))

// 7. 方法
const handleUpdate = () => {
  emit('update', props.id)
}

// 8. 生命周期
onMounted(() => {
  // 初始化逻辑
})
</script>

<template>
  <!-- 模板内容 -->
</template>

<style scoped lang="scss">
/* 样式 */
</style>
```

### 命名规范

#### 文件命名
- 组件文件：PascalCase（如 `PoemCard.vue`）
- 工具函数文件：camelCase（如 `formatDate.ts`）
- 类型定义文件：camelCase（如 `api.d.ts`）

#### 变量命名
- 普通变量：camelCase（如 `poemList`）
- 常量：UPPER_SNAKE_CASE（如 `API_BASE_URL`）
- 类型/接口：PascalCase（如 `UserInfo`）
- 布尔值：以 `is`/`has`/`can` 开头（如 `isLoading`）

#### CSS命名
- 使用BEM命名规范：`.block__element--modifier`
- 组件样式使用scoped

## 路由规范

### 路由配置

```typescript
// router/index.ts
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
```

### 路由守卫

```typescript
// router/guards.ts
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
```

## 状态管理规范

### Store结构

```typescript
// stores/user.ts
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
```

## API规范

### Axios封装

```typescript
// utils/request.ts
import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import type { ApiResponse } from '@/types/api'

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const { code, message } = response.data
    if (code === 200) {
      return response.data
    }
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message || '请求失败'))
  },
  (error) => {
    if (error.response) {
      const { status } = error.response
      switch (status) {
        case 401:
          ElMessage.error('登录已过期，请重新登录')
          const userStore = useUserStore()
          userStore.logout()
          window.location.href = '/user/login'
          break
        case 403:
          ElMessage.error('没有权限访问')
          break
        case 404:
          ElMessage.error('请求的资源不存在')
          break
        case 500:
          ElMessage.error('服务器内部错误')
          break
        default:
          ElMessage.error(error.message || '网络错误')
      }
    } else {
      ElMessage.error('网络连接失败')
    }
    return Promise.reject(error)
  }
)

export function get<T>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service.get(url, config)
}

export function post<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service.post(url, data, config)
}

export function put<T>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service.put(url, data, config)
}

export function del<T>(url: string, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service.delete(url, config)
}

export default service
```

### API模块定义

```typescript
// api/modules/poem.ts
import request from '@/utils/request'
import type { Poem, PoemListParams, PageResult } from '@/types/model'
import type { PoemCreateRequest, PoemUpdateRequest } from '@/types/api'

export function getPoemList(params: PoemListParams) {
  return request.get<PageResult<Poem>>('/poems', { params })
}

export function getPoemById(id: number) {
  return request.get<Poem>(`/poems/${id}`)
}

export function searchPoems(keyword: string) {
  return request.get<Poem[]>('/poems/search', { params: { keyword } })
}

export function getModernPoems() {
  return request.get<Poem[]>('/poems/modern')
}

export function createPoem(data: PoemCreateRequest) {
  return request.post<Poem>('/poems', data)
}

export function updatePoem(data: PoemUpdateRequest) {
  return request.put<Poem>(`/poems/${data.id}`, data)
}

export function deletePoem(id: number) {
  return request.delete<void>(`/poems/${id}`)
}

export function getPoemsByDynasty(dynastyId: number, params?: { page?: number; size?: number }) {
  return request.get<PageResult<Poem>>(`/poems/dynasty/${dynastyId}`, { params })
}

export function getPoemsByCategory(categoryId: number, params?: { page?: number; size?: number }) {
  return request.get<PageResult<Poem>>(`/poems/category/${categoryId}`, { params })
}

export function likePoem(id: number) {
  return request.post<void>(`/poems/${id}/like`)
}

export function favoritePoem(id: number) {
  return request.post<void>(`/poems/${id}/favorite`)
}
```

## 样式规范

### SCSS变量

```scss
// assets/styles/variables.scss

// 颜色
$primary-color: #8B4513;
$secondary-color: #D2691E;
$text-color: #333333;
$text-color-secondary: #666666;
$border-color: #E8E8E8;
$background-color: #F5F5F5;

// 字体
$font-family: 'Noto Serif SC', 'Songti SC', serif;
$font-size-base: 14px;
$font-size-lg: 16px;
$font-size-sm: 12px;

// 间距
$spacing-xs: 4px;
$spacing-sm: 8px;
$spacing-md: 16px;
$spacing-lg: 24px;
$spacing-xl: 32px;

// 圆角
$border-radius-sm: 4px;
$border-radius-md: 8px;
$border-radius-lg: 12px;

// 阴影
$box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
$box-shadow-lg: 0 4px 16px rgba(0, 0, 0, 0.15);

// 断点
$breakpoint-sm: 576px;
$breakpoint-md: 768px;
$breakpoint-lg: 992px;
$breakpoint-xl: 1200px;
```

### 组件样式

```vue
<style scoped lang="scss">
.poem-card {
  padding: $spacing-md;
  border-radius: $border-radius-md;
  box-shadow: $box-shadow;
  transition: box-shadow 0.3s ease;
  
  &:hover {
    box-shadow: $box-shadow-lg;
  }
  
  &__title {
    font-size: $font-size-lg;
    color: $text-color;
    margin-bottom: $spacing-sm;
  }
  
  &__content {
    font-size: $font-size-base;
    color: $text-color-secondary;
    line-height: 1.6;
  }
  
  &__author {
    font-size: $font-size-sm;
    color: $text-color-secondary;
    margin-top: $spacing-sm;
  }
}
</style>
```

## 性能优化

### 代码分割

```typescript
// 路由懒加载
const routes = [
  {
    path: '/poem',
    component: () => import('@/views/poem/index.vue')
  }
]
```

### 图片优化

```vue
<template>
  <img 
    v-lazy="imageUrl" 
    :alt="alt"
    loading="lazy"
    @load="handleLoad"
  />
</template>
```

### 缓存策略

```typescript
// 使用keep-alive缓存组件
<router-view v-slot="{ Component }">
  <keep-alive :include="cachedViews">
    <component :is="Component" />
  </keep-alive>
</router-view>
```

## 错误处理

### 全局错误处理

```typescript
// main.ts
app.config.errorHandler = (err, instance, info) => {
  console.error('全局错误:', err)
  // 上报错误
}
```

### 组件错误处理

```vue
<template>
  <error-boundary>
    <poem-card :poem="poem" />
  </error-boundary>
</template>
```

## 测试规范

### 单元测试

```typescript
// tests/unit/components/PoemCard.spec.ts
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import PoemCard from '@/components/business/PoemCard.vue'

describe('PoemCard', () => {
  it('renders poem title correctly', () => {
    const wrapper = mount(PoemCard, {
      props: {
        poem: { id: 1, title: '静夜思', content: '床前明月光...' }
      }
    })
    expect(wrapper.text()).toContain('静夜思')
  })
})
```

## 文档注释

### 组件注释

```typescript
/**
 * 诗词卡片组件
 * @description 用于展示诗词摘要信息，支持点击查看详情
 * @example <poem-card :poem="poem" @click="handleClick" />
 */
interface PoemCardProps {
  /** 诗词数据对象 */
  poem: Poem
  /** 是否显示作者信息 */
  showAuthor?: boolean
}
```

---

**文档版本**：v1.3  
**最后更新**：2026-05-29  
**维护人员**：墨渊开发团队