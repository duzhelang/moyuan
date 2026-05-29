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

| 接口 | 说明 |
|------|------|
| /api/auth/** | 认证相关接口（注册、登录） |
| /api/poems | 诗词列表 |
| /api/poems/{id} | 诗词详情 |
| /api/poems/random | 随机诗词 |
| /api/poems/daily | 每日诗词 |
| /api/poets | 诗人列表 |
| /api/poets/{id} | 诗人详情 |
| /api/dynasties | 朝代列表 |
| /api/dynasties/{id} | 朝代详情 |
| /api/categories | 分类列表 |
| /api/categories/{id} | 分类详情 |
| /api/forum/posts | 帖子列表 |
| /api/forum/posts/{id} | 帖子详情 |
| /api/forum/posts/{id}/comments | 评论列表 |
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

```typescript
// utils/request.ts
service.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const userStore = useUserStore()
      userStore.clearToken()
      router.push('/login')
    }
    return Promise.reject(error)
  }
)
```

## 安全配置

### Spring Security 配置

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/poems/**").permitAll()
                .requestMatchers("/poets/**").permitAll()
                .requestMatchers("/dynasties/**").permitAll()
                .requestMatchers("/categories/**").permitAll()
                .requestMatchers("/forum/posts/**").permitAll()
                .requestMatchers("/comments/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

### 密码加密

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

---

**文档版本**：v1.2  
**最后更新**：2026-05-29  
**维护人员**：墨渊开发团队