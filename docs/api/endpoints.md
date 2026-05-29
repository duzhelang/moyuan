# API 端点清单

## 概述

本文档记录"古今诗话——墨渊"项目后端 API 端点清单。

## 项目说明

本项目包含**两个独立的后端项目**，各自承担不同的职责：

| 后端项目 | 路径前缀 | 职责说明 | 统一响应类 |
|----------|----------|----------|------------|
| **backend** | `/api` | 诗词核心模块（诗词 CRUD、搜索等） | `Result<T>` (code/message/data) |
| **sc-moyuan-backend** | `/api/auth`、`/api/forum` | 认证授权、论坛社区模块 | `R<T>` (code/message/data) |

> 两个后端均使用 Spring Boot 3.x，但各自定义了独立的统一响应包装类，字段结构一致（code/message/data），类名不同。

## 技术栈

- 后端框架: Spring Boot 3.x
- 认证方式: JWT
- 响应格式: 统一 JSON 响应（backend 使用 `Result<T>`，sc-moyuan-backend 使用 `R<T>`）
- API 文档: Knife4j (http://localhost:8080/doc.html)

## 统一响应格式

**backend** 响应格式 (`Result<T>`)：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

**sc-moyuan-backend** 响应格式 (`R<T>`)：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

## API 端点清单

### 认证模块 (/api/auth) — sc-moyuan-backend

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/auth/register | 用户注册 | 否 |
| POST | /api/auth/login | 用户登录 | 否 |

### 用户模块 (/api/users) — 新增

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/users/me | 获取当前用户信息 | 是 |
| PUT | /api/users/me | 更新当前用户信息 | 是 |
| PUT | /api/users/me/password | 修改密码 | 是 |
| GET | /api/users/me/posts | 获取当前用户帖子列表 | 是 |
| GET | /api/users/{id} | 获取用户信息 | 否 |

### 诗词模块 (/api/poems) — backend

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/poems/modern | 获取现代诗词 | 否 |
| GET | /api/poems/modern/page | 分页获取现代诗词 | 否 |
| GET | /api/poems/category/{category} | 按分类获取诗词 | 否 |
| GET | /api/poems/featured | 获取精选诗词 | 否 |
| GET | /api/poems/random | 获取随机诗词 | 否 |
| GET | /api/poems/daily | 获取每日诗词 | 否 |
| GET | /api/poems/{id} | 获取诗词详情 | 否 |
| GET | /api/poems/search | 搜索诗词 | 否 |
| POST | /api/poems | 创建诗词 | 是 |
| PUT | /api/poems/{id} | 更新诗词 | 是 |
| DELETE | /api/poems/{id} | 删除诗词 | 是 |

### 诗人模块 (/api/poets)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/poets | 获取诗人列表 | 否 |
| GET | /api/poets/{id} | 获取诗人详情 | 否 |

### 朝代模块 (/api/dynasties)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/dynasties | 获取所有朝代列表 | 否 |
| GET | /api/dynasties/{id} | 获取朝代详情 | 否 |

### 分类模块 (/api/categories)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/categories | 获取所有分类列表 | 否 |
| GET | /api/categories/{id} | 获取分类详情 | 否 |

### 论坛模块 (/api/forum)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/forum/posts | 获取帖子列表 | 否 |
| GET | /api/forum/posts/{id} | 获取帖子详情 | 否 |
| POST | /api/forum/posts | 创建帖子 | 是 |
| PUT | /api/forum/posts/{id} | 更新帖子 | 是 |
| DELETE | /api/forum/posts/{id} | 删除帖子 | 是 |
| POST | /api/forum/posts/{id}/like | 点赞/取消点赞帖子 | 是 |
| GET | /api/forum/posts/{id}/like | 检查是否点赞帖子 | 是 |
| GET | /api/forum/posts/{id}/comments | 获取帖子评论列表 | 否 |
| POST | /api/forum/posts/{id}/comments | 创建评论 | 是 |
| DELETE | /api/forum/comments/{id} | 删除评论 | 是 |
| POST | /api/forum/comments/{id}/like | 点赞/取消点赞评论 | 是 |

### 搜索模块 (/api/search)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/search | 全局搜索（跨模块：诗词+诗人+帖子） | 否 |

### 文件模块 (/api/files) — 新增

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/files/upload | 上传文件 | 是 |

### 浏览历史模块 (/api/history) — 新增

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/history | 添加浏览历史 | 是 |
| GET | /api/history | 获取浏览历史 | 是 |
| DELETE | /api/history | 清空浏览历史 | 是 |

### AI模块 (/api/ai) — 新增

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/ai/chat | AI问答 | 否 |
| POST | /api/ai/write-poem | 看图写诗 | 否 |
| POST | /api/ai/analyze | 智能分析 | 否 |

**AI问答请求参数**：
```json
{
  "message": "用户问题",
  "model": "zhipu"  // 可选: zhipu, deepseek, kimi, mimo
}
```

**AI问答响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "message": "用户问题",
    "reply": "AI回复内容",
    "model": "zhipu"
  }
}
```

**看图写诗请求参数**：
- Content-Type: multipart/form-data
- 参数：
  - `image`: 图片文件（必填）
  - `model`: AI模型（可选，默认zhipu）

**看图写诗响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "poem": "AI生成的诗词内容",
    "model": "zhipu"
  }
}
```

**智能分析请求参数**：
```json
{
  "poem": "要分析的诗词内容",
  "model": "zhipu"  // 可选: zhipu, deepseek, kimi, mimo
}
```

**智能分析响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "poem": "原始诗词",
    "analysis": "AI分析结果",
    "model": "zhipu"
  }
}
```

### 管理员模块 (/api/admin) — 新增

> 以下接口为管理员专属接口，需要管理员权限才能访问。

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/admin/stats | 获取控制台统计数据 | 是 |
| GET | /api/admin/stats/trend | 获取统计数据趋势 | 是 |
| GET | /api/admin/users | 获取用户列表 | 是 |
| GET | /api/admin/users/{id} | 获取用户详情 | 是 |
| PUT | /api/admin/users/{id}/status | 更新用户状态 | 是 |
| DELETE | /api/admin/users/{id} | 删除用户 | 是 |
| GET | /api/admin/poems | 获取诗词列表 | 是 |
| GET | /api/admin/poems/{id} | 获取诗词详情 | 是 |
| PUT | /api/admin/poems/{id}/status | 更新诗词状态 | 是 |
| DELETE | /api/admin/poems/{id} | 删除诗词 | 是 |
| GET | /api/admin/categories | 获取分类列表 | 是 |
| POST | /api/admin/categories | 创建分类 | 是 |
| PUT | /api/admin/categories/{id} | 更新分类 | 是 |
| DELETE | /api/admin/categories/{id} | 删除分类 | 是 |
| GET | /api/admin/dynasties | 获取朝代列表 | 是 |
| POST | /api/admin/dynasties | 创建朝代 | 是 |
| PUT | /api/admin/dynasties/{id} | 更新朝代 | 是 |
| DELETE | /api/admin/dynasties/{id} | 删除朝代 | 是 |
| GET | /api/admin/poets | 获取诗人列表 | 是 |
| GET | /api/admin/poets/{id} | 获取诗人详情 | 是 |
| PUT | /api/admin/poets/{id}/status | 更新诗人状态 | 是 |
| DELETE | /api/admin/poets/{id} | 删除诗人 | 是 |
| GET | /api/admin/forum/posts | 获取帖子列表 | 是 |
| GET | /api/admin/forum/posts/{id} | 获取帖子详情 | 是 |
| PUT | /api/admin/forum/posts/{id}/status | 更新帖子状态 | 是 |
| DELETE | /api/admin/forum/posts/{id} | 删除帖子 | 是 |
| GET | /api/admin/logs | 获取操作日志列表 | 是 |

---

## 接口规范

### 请求参数

- 请求体格式: JSON
- 分页参数: `pageNum` (页码, 从1开始), `pageSize` (每页数量, 默认10)
- 排序参数: `orderBy` (排序字段), `orderDir` (排序方向: asc/desc)

### 错误码

| 错误码 | 说明 |
|--------|------|
| 200 | 操作成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
| 1001 | 用户不存在 |
| 1002 | 用户已存在 |
| 1003 | 密码错误 |
| 2001 | 诗词不存在 |
| 3001 | 帖子不存在 |

---

**文档版本**：v1.3  
**最后更新**：2026-05-29  
**维护人员**：墨渊开发团队