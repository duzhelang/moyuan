# 古今诗话——墨渊 前端迁移开发文档

## 1. 项目概述

### 1.1 迁移目标

将现有的静态HTML页面迁移到Vue 3单文件组件架构，实现组件化、模块化开发，提升代码可维护性和开发效率。

### 1.2 迁移范围

| 原HTML文件 | 目标Vue组件 | 说明 |
|------------|-------------|------|
| index.html | views/home/index.vue | 首页主页面 |
| html/denglu.html | views/user/login.vue | 登录页面 |
| html/zhuce.html | views/user/register.vue | 注册页面 |
| html/fenye.html | views/poem/list.vue | 诗词分页列表 |
| html/fenyejiagou.html | views/poem/detail.vue | 诗词详情框架 |
| html/diushi.html | views/poem/detail.vue | 诗词详情页 |

## 2. 技术架构

### 2.1 技术栈选型

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue.js | 3.4+ | 前端框架 |
| TypeScript | 5.x | 类型系统 |
| Vite | 5.x | 构建工具 |
| Vue Router | 4.x | 路由管理 |
| Pinia | 2.x | 状态管理 |
| Element Plus | 2.x | UI组件库 |
| Axios | 1.x | HTTP客户端 |
| SCSS | - | CSS预处理器 |

### 2.2 项目结构

```
frontend/
├── src/
│   ├── api/                    # API接口定义
│   │   ├── modules/           # 按模块划分
│   │   │   ├── user.ts        # 用户API
│   │   │   ├── poem.ts        # 诗词API
│   │   │   ├── forum.ts       # 论坛API
│   │   │   ├── category.ts    # 分类API
│   │   │   └── dynasty.ts     # 朝代API
│   │   └── index.ts           # 统一导出
│   ├── assets/                # 静态资源
│   │   └── styles/           # 全局样式
│   │       ├── variables.scss
│   │       ├── mixins.scss
│   │       └── global.scss
│   ├── components/            # 公共组件
│   │   ├── common/           # 通用组件
│   │   │   ├── AppHeader.vue
│   │   │   ├── AppFooter.vue
│   │   │   ├── ErrorMessage.vue
│   │   │   └── LoadingSpinner.vue
│   │   └── business/         # 业务组件
│   │       ├── PoemCard.vue
│   │       └── ForumPost.vue
│   ├── composables/           # 组合式函数
│   │   ├── useAuth.ts
│   │   ├── usePoem.ts
│   │   └── useForum.ts
│   ├── layouts/               # 布局组件
│   │   └── DefaultLayout.vue
│   ├── router/                # 路由配置
│   │   ├── index.ts          # 统一定义所有路由
│   │   └── guards.ts
│   ├── stores/                # 状态管理
│   │   ├── index.ts
│   │   ├── user.ts
│   │   ├── poem.ts
│   │   └── app.ts
│   ├── types/                 # 类型定义
│   │   ├── api.d.ts
│   │   ├── model.d.ts
│   │   └── global.d.ts
│   ├── utils/                 # 工具函数
│   │   ├── request.ts
│   │   ├── storage.ts
│   │   ├── format.ts
│   │   └── validate.ts
│   ├── views/                 # 页面组件
│   │   ├── home/
│   │   │   └── index.vue
│   │   ├── poem/
│   │   │   ├── list.vue
│   │   │   └── detail.vue
│   │   ├── forum/
│   │   │   ├── list.vue
│   │   │   └── detail.vue
│   │   ├── user/
│   │   │   ├── login.vue
│   │   │   ├── register.vue
│   │   │   └── profile.vue
│   │   └── error/
│   │       └── 404.vue
│   ├── App.vue
│   ├── auto-imports.d.ts     # unplugin-auto-import 自动生成
│   ├── components.d.ts       # unplugin-vue-components 自动生成
│   └── main.ts
├── public/
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── env.d.ts
```

## 3. 组件设计规范

### 3.1 组件拆分原则

#### 首页组件拆分

首页目前采用单文件组件实现，所有逻辑集中在 `views/home/index.vue` 中，后续可按需拆分：

```
views/home/index.vue              # 首页（单文件组件，包含轮播图、导航菜单、搜索栏、各区域板块等）
```

#### 诗词模块组件拆分

```
views/poem/
├── list.vue                    # 诗词列表页
└── detail.vue                  # 诗词详情页
```

#### 论坛模块组件拆分

```
views/forum/
├── list.vue                    # 帖子列表页
└── detail.vue                  # 帖子详情页
```

#### 用户模块组件拆分

```
views/user/
├── login.vue                   # 登录页
├── register.vue                # 注册页
└── profile.vue                 # 个人中心
```

### 3.2 组件通信规范

| 场景 | 方式 | 示例 |
|------|------|------|
| 父传子 | Props | `<PoemCard :poem="poemData" />` |
| 子传父 | Emit | `emit('update', newValue)` |
| 兄弟组件 | Pinia Store | `usePoemStore()` |
| 全局状态 | Pinia Store | `useAppStore()` |
| 跨层级 | Provide/Inject | `provide('theme', theme)` |

### 3.3 组件Props定义规范

```typescript
// 使用TypeScript接口定义Props
interface PoemCardProps {
  poem: Poem
  showAuthor?: boolean
  maxWidth?: number
}

const props = withDefaults(defineProps<PoemCardProps>(), {
  showAuthor: true,
  maxWidth: 800
})
```

## 4. 样式迁移策略

### 4.1 CSS文件映射

| 原CSS文件 | 用途 | 迁移方案 |
|-----------|------|----------|
| body.css | 基础样式 | global.scss |
| shouye.css | 首页样式 | HomeView scoped |
| sy_caidan.css | 菜单样式 | HomeNav scoped |
| zhuangshi.css | 装饰样式 | global.scss |
| gd.css | 滚动样式 | ScrollBar scoped |
| daohang.css | 导航样式 | AppHeader scoped |
| luntan.css | 论坛样式 | ForumSection scoped |
| modern-poems.css | 现代诗词 | PoemSection scoped |
| denglu.css | 登录样式 | LoginView scoped |
| fenye.css | 分页样式 | PoemPagination scoped |
| fenyejiagou.css | 框架样式 | DefaultLayout scoped |
| yinyue.css | 音乐样式 | MusicPlayer scoped |

### 4.2 SCSS变量定义

```scss
// assets/styles/variables.scss

// 主题色
$primary-color: #8B4513;
$secondary-color: #D2691E;
$accent-color: #CD853F;

// 文本色
$text-color: #333333;
$text-color-secondary: #666666;
$text-color-light: #999999;

// 背景色
$background-color: #F5F5F5;
$background-color-light: #FFFFFF;
$background-color-dark: #2C2C2C;

// 边框
$border-color: #E8E8E8;
$border-radius-sm: 4px;
$border-radius-md: 8px;
$border-radius-lg: 12px;

// 间距
$spacing-xs: 4px;
$spacing-sm: 8px;
$spacing-md: 16px;
$spacing-lg: 24px;
$spacing-xl: 32px;

// 字体
$font-family-base: 'Noto Serif SC', 'Songti SC', serif;
$font-family-title: 'KaiTi', 'STKaiti', serif;
$font-size-sm: 12px;
$font-size-base: 14px;
$font-size-lg: 16px;
$font-size-xl: 20px;
$font-size-xxl: 24px;

// 阴影
$box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
$box-shadow-lg: 0 4px 16px rgba(0, 0, 0, 0.15);

// 断点
$breakpoint-sm: 576px;
$breakpoint-md: 768px;
$breakpoint-lg: 992px;
$breakpoint-xl: 1200px;
```

### 4.3 样式迁移步骤

1. **提取公共变量**：将颜色、字体、间距等提取为SCSS变量
2. **转换选择器**：将class选择器转换为BEM命名规范
3. **添加scoped**：为组件样式添加scoped属性
4. **使用嵌套**：利用SCSS嵌套语法简化代码
5. **提取混入**：将重复的样式提取为mixin

## 5. 脚本迁移策略

### 5.1 JavaScript到TypeScript

```typescript
// 原JavaScript
var now = 1;
var max = 6;
function imgLoopShow(id) {
  now = id;
  document.getElementById("ad_scroll").src = "img/lb_ (" + now + ").jpg";
}

// 迁移为TypeScript
const currentSlide = ref<number>(1)
const maxSlides = 6

const imgLoopShow = (id: number): void => {
  currentSlide.value = id
  // 使用响应式数据驱动视图更新
}
```

### 5.2 DOM操作迁移

```typescript
// 原DOM操作
document.getElementById('sousuo0').onsubmit = function() {
  window.location.href = 'https://example.com';
  return false;
}

// Vue方式
const handleSearch = (): void => {
  router.push({ path: '/search', query: { keyword: searchKeyword.value } })
}
```

### 5.3 事件处理迁移

```vue
<!-- 原HTML -->
<li onmouseover="imgLoopShow(1)" id="li1">1</li>

<!-- Vue方式 -->
<li 
  v-for="i in 6" 
  :key="i"
  :class="{ active: currentSlide === i }"
  @mouseover="imgLoopShow(i)"
>
  {{ i }}
</li>
```

## 6. 路由设计

### 6.1 路由配置

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

### 6.2 页面映射

| 原路径 | Vue路由 | 说明 |
|--------|---------|------|
| index.html | / | 首页 |
| html/denglu.html | /user/login | 登录页 |
| html/zhuce.html | /user/register | 注册页 |
| html/fenye.html | /poem | 诗词列表 |
| html/fenyejiagou.html | /poem/:id | 诗词详情 |
| html/diushi.html | /poem/:id | 诗词详情 |

## 7. 状态管理设计

### 7.1 Store模块划分

```typescript
// stores/user.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '@/types/model'
import { login as loginApi, getUserInfo } from '@/api/modules/user'
import type { LoginRequest } from '@/types/api'

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

  return { token, userInfo, isLoggedIn, username, avatar, login, fetchUserInfo, logout }
})

// stores/poem.ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Poem, PoemListParams } from '@/types/model'
import { getPoemList, getPoemById, getModernPoems } from '@/api/modules/poem'

export const usePoemStore = defineStore('poem', () => {
  const poemList = ref<Poem[]>([])
  const currentPoem = ref<Poem | null>(null)
  const modernPoems = ref<Poem[]>([])
  const loading = ref(false)
  const total = ref(0)

  async function fetchPoemList(params: PoemListParams) {
    loading.value = true
    try {
      const response = await getPoemList(params)
      poemList.value = response.data.records
      total.value = response.data.total
    } catch (error) {
      console.error('获取诗词列表失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  async function fetchPoemDetail(id: number) {
    loading.value = true
    try {
      const response = await getPoemById(id)
      currentPoem.value = response.data
    } catch (error) {
      console.error('获取诗词详情失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  async function fetchModernPoems() {
    loading.value = true
    try {
      const response = await getModernPoems()
      modernPoems.value = response.data
    } catch (error) {
      console.error('获取现代诗词失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  return { poemList, currentPoem, modernPoems, loading, total, fetchPoemList, fetchPoemDetail, fetchModernPoems }
})
```

## 8. 开发流程

### 8.1 迁移步骤

1. **环境搭建**
   - 创建Vue项目
   - 配置Vite、TypeScript
   - 安装依赖

2. **基础架构**
   - 配置路由
   - 配置状态管理
   - 配置Axios
   - 创建布局组件

3. **组件迁移**
   - 拆分HTML为组件
   - 迁移样式到SCSS
   - 迁移脚本到TypeScript
   - 添加响应式数据

4. **功能测试**
   - 页面渲染测试
   - 交互功能测试
   - 响应式布局测试

5. **优化完善**
   - 性能优化
   - 代码规范
   - 文档补充

### 8.2 开发命令

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build

# 代码检查
npm run lint

# 类型检查
npm run type-check
```

## 9. 注意事项

### 9.1 迁移注意事项

1. **保持功能一致**：确保迁移后页面功能与原HTML页面完全一致
2. **样式兼容性**：注意CSS样式在Vue中的兼容性问题
3. **事件处理**：将内联事件处理转换为Vue事件绑定
4. **资源路径**：调整图片、音频等资源的引用路径
5. **第三方库**：评估原生JS库是否需要替换为Vue版本

### 9.2 性能优化建议

1. **路由懒加载**：使用动态import实现路由懒加载
2. **组件懒加载**：大型组件使用defineAsyncComponent
3. **图片懒加载**：使用v-lazy或Intersection Observer
4. **代码分割**：合理拆分代码块
5. **缓存策略**：使用keep-alive缓存组件

### 9.3 代码规范

1. **TypeScript**：严格使用TypeScript，禁用any
2. **ESLint**：遵循项目ESLint配置
3. **Prettier**：统一代码格式
4. **注释规范**：关键代码添加注释
5. **Git规范**：遵循项目Git提交规范

---

**文档版本**：v1.1  
**创建日期**：2026-05-25  
**最后更新**：2026-05-28  
**维护人员**：墨渊开发团队