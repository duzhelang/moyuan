# 认证授权机制

## 概述

本文档描述"古今诗话——墨渊"项目的认证授权机制。

## 技术方案

- **认证方式**: JWT (JSON Web Token)
- **安全框架**: Spring Security 6.x
- **密码加密**: BCrypt
- **Token 存储**: 客户端 localStorage

## 认证流程

### 登录流程

```
1. 用户提交用户名/密码
   ↓
2. 后端验证用户名/密码
   ↓
3. 验证通过，生成 JWT Token
   ↓
4. 返回 Token 给前端
   ↓
5. 前端存储 Token 到 localStorage
   ↓
6. 后续请求携带 Token
```

### 请求认证流程

```
1. 前端在请求头中携带 Token
   Authorization: Bearer <token>
   ↓
2. Spring Security 拦截器拦截请求
   ↓
3. 解析并验证 Token
   ↓
4. Token 有效 → 放行请求
   Token 无效 → 返回 401 未授权
```

## JWT Token 结构（双 token）

系统采用 **access + refresh 双 token** 机制，登录成功后由 `JwtUtil` 签发一对并同时返回（见 `TokenResponse`）：

- **access token**：短时效（`jwt.expiration`，默认 24 小时），作为 `Authorization: Bearer` 用于受保护接口鉴权；过期后由 refresh token 换取新一对。
- **refresh token**：长时效（`jwt.refresh-expiration`，默认 7 天，配置 `JWT_REFRESH_EXPIRATION` 可改），仅在 `POST /api/auth/refresh` 内使用并**轮换**（旧 access/refresh 写入黑名单、签发新对）；同时 `POST /api/auth/logout` 将当前 token 的 `jti` 拉黑实现登出失效。

两种 token 均含 `type`（`access`/`refresh`）与每次签发唯一的 `jti`；黑名单由 `TokenBlacklistService` 管理（优先 Redis，Redis 失败时降级进程内内存）。

### Token 内容（access 示例）

```json
{
  "sub": "12345",           // 用户ID
  "username": "zhangsan",   // 用户名
  "type": "access",         // 类型：access / refresh
  "jti": "uuid-random",     // 每次签发随机，用于黑名单失效
  "iat": 1716633600,        // 签发时间
  "exp": 1716720000         // 过期时间
}
```

## 接口权限配置

### 公开接口（无需认证）

首页及所有浏览类页面均可未登录访问，仅写操作（发帖、点赞、收藏等）需要登录。

| 接口 | 说明 |
|------|------|
| /api/auth/** | 认证相关接口（注册、登录） |
| /api/poems/** | 诗词模块全部接口（列表、详情、随机、每日等） |
| /api/poets/** | 诗人模块全部接口（列表、详情） |
| /api/dynasties/** | 朝代模块全部接口（列表、详情） |
| /api/categories/** | 分类模块全部接口（列表、详情） |
| /api/forum/posts/** | 论坛帖子模块全部接口（列表、详情） |
| /api/forum/comments/** | 论坛评论模块全部接口 |
| /api/poet-featured/** | 精选诗人模块全部接口（随机推荐等） |
| /api/home-navigation/** | 首页导航模块全部接口（作品、流派、朝代导航） |
| /api/ai/** | AI模块全部接口（问答、看图写诗、智能分析） |
| /api/search | 全局搜索 |
| /uploads/** | 静态资源（上传文件） |
| /doc.html | API文档 |
| /webjars/** | 静态资源 |
| /v3/api-docs/** | OpenAPI文档 |
| /swagger-resources/** | Swagger资源 |

### 需要认证的接口

| 接口 | 说明 |
|------|------|
| /api/users/me | 获取/更新当前用户 |
| /api/users/me/password | 修改密码 |
| /api/users/me/posts | 获取当前用户帖子列表 |
| /api/files/upload | 上传文件 |
| /api/history | 浏览历史管理 |
| /api/poems/{id}/like | 点赞诗词 |
| /api/poems/{id}/favorite | 收藏诗词 |
| /api/poems/favorites | 获取收藏列表 |
| /api/forum/posts | 创建帖子 |
| /api/forum/posts/{id} | 更新/删除帖子 |
| /api/forum/posts/{id}/like | 点赞帖子 |
| /api/forum/posts/{id}/comments | 发表评论 |
| /api/forum/comments/{id} | 删除评论 |
| /api/forum/comments/{id}/like | 点赞评论 |

### 管理员接口

| 接口 | 说明 |
|------|------|
| /api/admin/** | 管理员专属接口 |

## 前端实现

### Token 存储（双 token）

前端 `useUserStore` 同时持久化 access 与 refresh 两个 token：

```typescript
// stores/user.ts
const token = ref<string>(localStorage.getItem('token') || '')
const refreshToken = ref<string>(localStorage.getItem('refreshToken') || '')

function setToken(t: { token: string; refreshToken?: string }) {
  token.value = t.token
  localStorage.setItem('token', t.token)
  if (t.refreshToken) {
    refreshToken.value = t.refreshToken
    localStorage.setItem('refreshToken', t.refreshToken)
  }
}

function clearToken() {
  token.value = ''
  refreshToken.value = ''
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
}
```

### 管理员账号记忆

系统会自动记录已登录过的管理员用户名，用于快捷登录：

```typescript
// stores/user.ts
const lastAdminUsername = computed(() => localStorage.getItem('lastAdminUsername') || '')

async function fetchUserInfo() {
  const response = await getUserInfo()
  userInfo.value = response.data
  // 如果是管理员，记录用户名到 localStorage
  if (response.data.role === 'admin') {
    localStorage.setItem('lastAdminUsername', response.data.username)
  }
}
```

### 登录后智能跳转

登录成功后根据用户角色自动决定跳转目标：

```typescript
// views/user/login.vue
const handleLogin = async () => {
  await userStore.login({ username, password })
  const redirect = route.query.redirect as string
  
  if (userStore.userInfo?.role === 'admin' && !redirect) {
    // 管理员默认跳转到管理后台
    router.push('/admin/dashboard')
  } else {
    // 普通用户或有 redirect 参数时跳转到目标页
    router.push(redirect || '/')
  }
}
```

### 管理员快捷登录

登录页面会智能显示管理员快捷标识（仅对已知管理员显示）：

```vue
<!-- views/user/login.vue -->
<template>
  <!-- 仅当 localStorage 中有记录的管理员用户名时显示 -->
  <div v-if="userStore.lastAdminUsername" class="admin-quick-entry">
    <el-tag type="warning" effect="plain" @click="fillAdminUsername">
      管理员快捷登录
    </el-tag>
  </div>
</template>

<script setup>
const fillAdminUsername = () => {
  form.username = userStore.lastAdminUsername
}
</script>
```

**安全设计**：
- 只有曾经以管理员身份登录过的设备才会显示快捷标识
- 新用户看不到管理员入口，不暴露管理后台存在
- 快捷标识只填充用户名，仍需手动输入密码

### 请求携带 Token

```typescript
// utils/request.ts
service.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})
```

### 401 处理（静默续期 + 重放）

公开路径（首页、诗词、诗人、论坛浏览等）的 401 不会触发登出。对于需认证的接口，收到 401 时先用 refresh token 调 `POST /api/auth/refresh` 换取新一对，**重放原请求**；仅当续期失败（refresh 缺失/过期）才清理登录态并跳转登录页：

```typescript
// utils/request.ts
const PUBLIC_PATHS = [
  '/poems', '/poets', '/dynasties', '/categories',
  '/forum/posts', '/forum/comments',
  '/poet-featured', '/home-navigation', '/vision',
  '/ai', '/rhyme', '/search', '/static-pages', '/auth'
]

function isPublicRequest(url: string = ''): boolean {
  return PUBLIC_PATHS.some(path => url.includes(path))
}

async function tryRefreshToken(): Promise<boolean> {
  const userStore = useUserStore()
  if (!userStore.refreshToken) return false
  try {
    const response = await refreshService.post('/auth/refresh', { refreshToken: userStore.refreshToken })
    if (response.data?.code === '200' && response.data?.data?.token) {
      userStore.setToken(response.data.data.token)
      if (response.data.data.refreshToken) {
        // refresh 轮换：更新本地 refresh token
        localStorage.setItem('refresh_token', response.data.data.refreshToken)
      }
      return true
    }
    return false
  } catch {
    return false
  }
}
```

- `/auth/refresh`、`/auth/logout` 自身会跳过续期逻辑，避免层层递归。
- 并发多个 401 时复用同一次刷新（`refreshPromise` 去重），刷新成功后用新 token 重放原请求；二次重试仍 401 则停止。
- 续期失败才执行 `userStore.logout()` 并跳 `/user/login`（`isRedirectingToLogin` 防跳转重入）。

## 安全配置

### Spring Security 配置

首页及所有浏览类页面的 API 均已加入 `permitAll()`，未登录用户可自由访问：

```java
// SecurityConfig.java
.requestMatchers(
    "/api/auth/**",
    "/api/poems/**",
    "/api/poets/**",
    "/api/dynasties/**",
    "/api/categories/**",
    "/api/forum/posts/**",
    "/api/forum/comments/**",
    "/api/poet-featured/**",
    "/api/home-navigation/**",
    "/api/ai/**",
    "/api/search",
    "/uploads/**"
).permitAll()
.requestMatchers("/api/admin/**").hasRole("ADMIN")
.anyRequest().authenticated()
```

### 密码加密

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

## 测试用户

系统初始化时会创建以下测试用户（密码使用 BCrypt 加密存储）：

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| admin | admin123 | admin | 管理员账号，可访问管理后台 |
| test | test123 | user | 普通用户账号 |

**使用说明**：
1. 执行 `init.sql` 初始化数据库时会自动创建测试用户
2. 管理员登录后会自动跳转到 `/admin/dashboard`
3. 首次以管理员身份登录后，后续访问登录页面会显示"管理员快捷登录"标识

---

**文档版本**：v1.5
**最后更新**：2026-09-05（JWT 说明更新为 access/refresh 双 token 与黑名单；前端 token 存储与 401 静默续期说明）  
**维护人员**：墨渊开发团队