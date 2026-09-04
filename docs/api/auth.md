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

## JWT Token 结构

### Token 生成

```java
String token = Jwts.builder()
    .subject(userId.toString())
    .claim("username", username)
    .issuedAt(new Date())
    .expiration(new Date(System.currentTimeMillis() + 86400000)) // 24小时
    .signWith(secretKey)
    .compact();
```

### Token 内容

```json
{
  "sub": "12345",           // 用户ID
  "username": "zhangsan",   // 用户名
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

### Token 存储

```typescript
// stores/user.ts
const token = ref<string>(localStorage.getItem('token') || '')

function setToken(newToken: string) {
  token.value = newToken
  localStorage.setItem('token', newToken)
}

function clearToken() {
  token.value = ''
  localStorage.removeItem('token')
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

### 401 处理

公开路径（首页、诗词、诗人、论坛浏览等）的 401 响应不会触发跳转，仅需认证的接口（个人中心、管理后台等）才会跳转登录页：

```typescript
// utils/request.ts
const PUBLIC_PATHS = [
  '/poems', '/poets', '/dynasties', '/categories',
  '/forum/posts', '/forum/comments',
  '/poet-featured', '/home-navigation', '/ai',
  '/search', '/auth'
]

function isPublicRequest(url: string = ''): boolean {
  return PUBLIC_PATHS.some(path => url.includes(path))
}

service.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      if (!isPublicRequest(error.config?.url)) {
        const userStore = useUserStore()
        userStore.logout()
        window.location.href = '/user/login'
      }
    }
    return Promise.reject(error)
  }
)
```

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

**文档版本**：v1.4  
**最后更新**：2026-06-01  
**维护人员**：墨渊开发团队