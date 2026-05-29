# 业务模块说明

## 概述

本文档描述"古今诗话——墨渊"项目的 8 大业务模块功能说明。

## 模块总览

| 模块 | 说明 | 状态 |
|------|------|------|
| 首页模块 | 网站入口，展示精选内容 | 前端已实现（已重写，连接后端API） |
| 诗词模块 | 诗词浏览、搜索、赏析 | 前端+后端已实现 |
| 诗人模块 | 诗人介绍、作品集 | 前端+后端已实现 |
| 论坛模块 | 用户交流、发帖评论 | 前端+后端已实现 |
| AI模块 | AI 辅助创作 | 规划中 |
| 搜索模块 | 全文搜索 | 前端+后端已实现（跨模块搜索） |
| 用户模块 | 注册登录、个人中心 | 前端+后端已实现 |
| 管理模块 | 后台管理、内容管理、数据统计 | 前端+后端已实现 |
| 文件模块 | 文件上传、图片管理 | 前端+后端已实现 |
| 浏览历史模块 | 浏览记录、历史查看 | 前端+后端已实现 |

---

## 1. 首页模块

### 功能说明

- 轮播图展示精选诗词
- 导航菜单（朝代、流派、诗人、作品、论坛）
- 推荐诗人卡片
- 推荐作品卡片
- 分类精赏
- 随机古诗词展示

### 页面组件

| 组件 | 说明 |
|------|------|
| AppHeader | 顶部导航栏 |
| HeroSection | 轮播图区域 |
| FeaturedPoets | 推荐诗人 |
| FeaturedPoems | 推荐作品 |
| CategorySection | 分类精赏 |
| AppFooter | 底部信息 |

### 前端路由

| 路径 | 说明 |
|------|------|
| `/` | 首页 |

---

## 2. 诗词模块

### 功能说明

- 诗词列表浏览（分页、筛选）
- 按朝代筛选（先秦、汉朝、唐朝、宋朝等）
- 按分类筛选（古体诗、近体诗、词、曲等）
- 按诗人筛选
- 诗词详情展示
- 诗词译文、赏析、创作背景
- 点赞、收藏功能

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/poems | 诗词列表 | backend PoemController |
| GET /api/poems/{id} | 诗词详情 | backend PoemController |
| GET /api/poems/search | 搜索诗词 | backend PoemController |

### 前端路由

| 路径 | 说明 |
|------|------|
| `/poem` | 诗词列表页 |
| `/poem/:id` | 诗词详情页 |

---

## 3. 诗人模块

### 功能说明

- 诗人列表浏览
- 按朝代筛选
- 诗人详情展示
- 诗人生平简介
- 诗人作品集
- 相关诗人推荐

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/poets | 诗人列表 | sc-moyuan-backend PoetController |
| GET /api/poets/{id} | 诗人详情 | sc-moyuan-backend PoetController |

### 前端路由

| 路径 | 说明 |
|------|------|
| `/poet` | 诗人列表页 |
| `/poet/:id` | 诗人详情页 |

---

## 4. 论坛模块

### 功能说明

- 帖子列表浏览
- 帖子发布（富文本编辑）
- 帖子详情查看
- 评论功能（支持多级回复）
- 点赞功能
- 精华帖推荐
- 置顶帖功能

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/forum/posts | 帖子列表 | sc-moyuan-backend ForumController |
| GET /api/forum/posts/{id} | 帖子详情 | sc-moyuan-backend ForumController |
| POST /api/forum/posts | 发布帖子 | sc-moyuan-backend ForumController |
| PUT /api/forum/posts/{id} | 编辑帖子 | sc-moyuan-backend ForumController |
| DELETE /api/forum/posts/{id} | 删除帖子 | sc-moyuan-backend ForumController |
| POST /api/forum/posts/{id}/like | 点赞帖子 | sc-moyuan-backend ForumController |
| GET /api/forum/posts/{id}/comments | 评论列表 | sc-moyuan-backend ForumController |
| POST /api/forum/posts/{id}/comments | 发表评论 | sc-moyuan-backend ForumController |

### 前端路由

| 路径 | 说明 |
|------|------|
| `/forum` | 论坛列表页 |
| `/forum/create` | 发帖页 |
| `/forum/:id` | 论坛详情页 |

---

## 5. AI 模块

### 功能说明

- AI诗词问答（支持多模型切换）
- 看图写诗（上传图片生成古诗词）
- 智能分析（输入诗句进行深度解析）
- 智能诗词推荐
- 诗词赏析生成
- 创作辅助

### 技术方案

- 调用大语言模型 API（智谱AI、DeepSeek、Kimi、MiMo）
- 智谱AI视觉模型（GLM-4V）用于看图写诗
- 前端首页集成AI问答组件
- 前端首页"展风拓潮"模块集成看图写诗和智能分析

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| POST /api/ai/chat | AI问答 | sc-moyuan-backend AiController |
| POST /api/ai/write-poem | 看图写诗 | sc-moyuan-backend AiController |
| POST /api/ai/analyze | 智能分析 | sc-moyuan-backend AiController |

### 前端组件

| 组件 | 说明 |
|------|------|
| AI问答对话框 | 首页右下角悬浮按钮，点击打开AI问答对话框 |
| 展风拓潮模块 | 首页"展风拓潮"区域，包含看图写诗和智能分析功能 |

---

## 6. 搜索模块

### 功能说明

- 全文搜索
- 诗人搜索
- 诗词搜索
- 帖子搜索
- 搜索建议

### 技术方案

- MySQL 全文索引（初期）
- Elasticsearch（后期优化）

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/search?keyword=xxx | 全局搜索（跨模块：诗词+诗人+帖子） | sc-moyuan-backend SearchController |

### 前端路由

| 路径 | 说明 |
|------|------|
| `/search` | 全局搜索结果页 |

---

## 7. 用户模块

### 功能说明

- 用户注册
- 用户登录
- 个人中心
- 修改密码
- 修改个人资料
- 头像上传
- 我的收藏
- 我的点赞
- 我的帖子

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| POST /api/auth/register | 注册 | sc-moyuan-backend AuthController |
| POST /api/auth/login | 登录 | sc-moyuan-backend AuthController |
| POST /api/auth/logout | 登出 | sc-moyuan-backend AuthController |
| GET /api/auth/me | 当前用户信息 | sc-moyuan-backend AuthController |
| PUT /api/auth/password | 修改密码 | sc-moyuan-backend AuthController |
| PUT /api/user/profile | 更新资料 | 规划中 |
| PUT /api/user/avatar | 更新头像 | 规划中 |
| GET /api/user/favorites | 收藏列表 | 规划中 |
| GET /api/user/likes | 点赞列表 | 规划中 |

### 前端路由

| 路径 | 说明 |
|------|------|
| `/user/login` | 登录页 |
| `/user/register` | 注册页 |
| `/user/profile` | 个人中心 |

---

## 8. 管理模块（新增）

### 功能说明

- 管理后台布局（左侧导航 + 右侧内容区域）
- 控制台（Dashboard）：系统统计数据展示
- 用户管理：用户列表、状态管理、创建/编辑/删除用户
- 诗词管理：诗词列表、状态管理、创建/编辑/删除诗词
- 分类管理：分类列表、创建/编辑/删除分类
- 朝代管理：朝代列表、创建/编辑/删除朝代
- 诗人管理：诗人列表、状态管理、创建/编辑/删除诗人
- 帖子管理：帖子列表、状态管理、创建/编辑/删除帖子

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/admin/dashboard/stats | 获取控制台统计数据 | backend AdminController |
| GET /api/admin/users | 获取用户列表 | backend AdminController |
| GET /api/admin/users/{id} | 获取用户详情 | backend AdminController |
| PUT /api/admin/users/{id}/status | 更新用户状态 | backend AdminController |
| DELETE /api/admin/users/{id} | 删除用户 | backend AdminController |
| GET /api/admin/poems | 获取诗词列表 | backend AdminController |
| GET /api/admin/poems/{id} | 获取诗词详情 | backend AdminController |
| PUT /api/admin/poems/{id}/status | 更新诗词状态 | backend AdminController |
| DELETE /api/admin/poems/{id} | 删除诗词 | backend AdminController |
| GET /api/admin/categories | 获取分类列表 | backend AdminController |
| POST /api/admin/categories | 创建分类 | backend AdminController |
| PUT /api/admin/categories/{id} | 更新分类 | backend AdminController |
| DELETE /api/admin/categories/{id} | 删除分类 | backend AdminController |
| GET /api/admin/dynasties | 获取朝代列表 | backend AdminController |
| POST /api/admin/dynasties | 创建朝代 | backend AdminController |
| PUT /api/admin/dynasties/{id} | 更新朝代 | backend AdminController |
| DELETE /api/admin/dynasties/{id} | 删除朝代 | backend AdminController |
| GET /api/admin/poets | 获取诗人列表 | backend AdminController |
| GET /api/admin/poets/{id} | 获取诗人详情 | backend AdminController |
| PUT /api/admin/poets/{id}/status | 更新诗人状态 | backend AdminController |
| DELETE /api/admin/poets/{id} | 删除诗人 | backend AdminController |
| GET /api/admin/forum/posts | 获取帖子列表 | backend AdminController |
| GET /api/admin/forum/posts/{id} | 获取帖子详情 | backend AdminController |
| PUT /api/admin/forum/posts/{id}/status | 更新帖子状态 | backend AdminController |
| DELETE /api/admin/forum/posts/{id} | 删除帖子 | backend AdminController |

### 前端路由

| 路径 | 说明 |
|------|------|
| `/admin` | 管理后台（重定向到Dashboard） |
| `/admin/dashboard` | 控制台 |
| `/admin/users` | 用户管理 |
| `/admin/poems` | 诗词管理 |
| `/admin/categories` | 分类管理 |
| `/admin/dynasties` | 朝代管理 |
| `/admin/poets` | 诗人管理 |
| `/admin/forum/posts` | 帖子管理 |
| `/admin/logs` | 操作日志 |

### 权限控制

- 需要管理员权限才能访问（`requiresAdmin: true`）
- 路由守卫会检查用户是否为管理员角色
- 非管理员用户访问管理页面会被重定向到首页

### 数据流转

### 诗词浏览流程

```
用户访问首页 → 展示精选诗词 → 点击诗词 → 查看诗词详情 → 点赞/收藏
```

### 论坛发帖流程

```
用户登录 → 进入论坛 → 点击发帖 → 编辑内容 → 提交帖子 → 帖子列表展示
```

### 搜索流程

```
用户输入关键词 → 发起搜索请求 → 返回搜索结果 → 点击查看详情
```

---

**文档版本**：v1.2  
**最后更新**：2026-05-29  
**维护人员**：墨渊开发团队