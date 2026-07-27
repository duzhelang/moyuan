# API 端点清单

## 概述

本文档记录"古今诗话——墨渊"项目后端 API 端点清单。

## 项目说明

本项目使用 **sc-moyuan-backend** 作为唯一后端项目，负责所有 API 接口。

## 技术栈

- 后端框架: Spring Boot 3.x
- 认证方式: JWT
- 响应格式: 统一 JSON 响应 `R<T>` (code/message/data)
- API 文档: Knife4j (http://localhost:8081/doc.html)

## 统一响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

## API 端点清单

### 认证模块 (/api/auth)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/auth/register | 用户注册（支持兴趣选项） | 否 |
| POST | /api/auth/login | 用户登录 | 否 |

### 用户模块 (/api/users)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/users/me | 获取当前用户信息 | 是 |
| PUT | /api/users/me | 更新当前用户信息 | 是 |
| PUT | /api/users/me/password | 修改密码 | 是 |
| GET | /api/users/me/posts | 获取当前用户帖子列表 | 是 |
| GET | /api/users/me/stats | 获取当前用户统计信息 | 是 |
| GET | /api/users/{id} | 获取用户信息 | 否 |
| GET | /api/users/{id}/profile | 获取用户主页信息（含诗人资料） | 否 |
| GET | /api/users/{id}/works | 获取用户作品列表 | 否 |

### 诗词模块 (/api/poems)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/poems | 获取诗词列表 | 否 |
| GET | /api/poems/modern | 获取现代诗词 | 否 |
| GET | /api/poems/modern/page | 分页获取现代诗词 | 否 |
| GET | /api/poems/category/{category} | 按分类获取诗词 | 否 |
| GET | /api/poems/featured | 获取精选诗词 | 否 |
| GET | /api/poems/random | 获取随机诗词 | 否 |
| GET | /api/poems/daily | 获取每日推荐诗词 | 否 |
| GET | /api/poems/{id} | 获取诗词详情 | 否 |
| GET | /api/poems/search | 搜索诗词 | 否 |
| POST | /api/poems | 创建诗词（用户发布） | 是 |
| PUT | /api/poems/{id} | 更新诗词 | 是 |
| DELETE | /api/poems/{id} | 删除诗词 | 是 |
| POST | /api/poems/{id}/like | 点赞/取消点赞 | 是 |
| GET | /api/poems/{id}/like | 检查是否点赞 | 是 |
| POST | /api/poems/{id}/favorite | 收藏/取消收藏 | 是 |
| GET | /api/poems/{id}/favorite | 检查是否收藏 | 是 |
| GET | /api/poems/favorites | 获取我的收藏列表 | 是 |
| POST | /api/poems/import-external | 导入外部诗词 | 是 |
| POST | /api/poems/fix-external-poems | 修复缺失诗人信息的外部诗词 | 是 |

**分页获取现代诗词参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | int | 否 | 页码，默认1 |
| pageSize | int | 否 | 每页数量，默认10 |
| isOriginal | boolean | 否 | 是否原创筛选 |
| hasCertifiedPoet | boolean | 否 | 是否筛选有认证诗人 |
| sortBy | string | 否 | 排序方式：latest（默认）、popular（浏览量）、likes（点赞数） |

**分页获取现代诗词响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "list": [...],
    "total": 100,
    "pageNum": 1,
    "pageSize": 10
  }
}
```

### 诗人模块 (/api/poets)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/poets | 获取诗人列表 | 否 |
| GET | /api/poets/{id} | 获取诗人详情 | 否 |
| GET | /api/poets/recommend | 推荐著名诗人（协同过滤） | 否 |
| GET | /api/poets/popular | 热门著名诗人排行 | 否 |

**推荐接口参数**：
- `limit` (可选): 返回数量，默认 6（recommend）/ 10（popular），上限 20/50

**推荐算法说明**：
- 基于用户的协同过滤（User-Based CF），综合收藏、点赞、浏览历史三种行为数据
- 收藏权重 5、点赞权重 3、浏览权重 1
- 使用余弦相似度计算用户相似性，推荐相似用户喜欢但当前用户未交互的诗人
- 新用户/无行为数据时降级为热门诗人推荐（基于全局行为热度排行）
- 推荐结果缓存 2 小时（Redis @Cacheable）

### 诗人认证模块 (/api/poet-profile)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/poet-profile/me | 获取当前用户诗人资料 | 是 |
| POST | /api/poet-profile/apply | 申请诗人认证 | 是 |
| GET | /api/poet-profile/{userId} | 获取指定用户诗人资料 | 否 |

**申请认证请求体**（PoetProfile）：
```json
{
  "penName": "笔名",
  "realName": "真实姓名",
  "specialty": "古体诗,近体诗,词",
  "introduction": "诗人简介",
  "literaryConcept": "创作理念",
  "achievements": "主要成就",
  "contactInfo": "邮箱/微信"
}
```

**认证状态说明**：
- `0` - 未认证
- `1` - 已认证
- `2` - 认证中（待审核）
- `3` - 认证失败

### 诗词评分模块 (/api/poems/{poemId}/ratings)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/poems/{poemId}/ratings | 获取诗词评分详情 | 否 |
| POST | /api/poems/{poemId}/ratings | 用户评分 | 是 |
| POST | /api/poems/{poemId}/ratings/ai | 请求AI评分 | 是 |
| GET | /api/poems/{poemId}/ratings/ai | 获取AI评分 | 否 |
| POST | /api/poems/{poemId}/ratings/ai/regenerate | 重新生成AI评价 | 是 |
| GET | /api/poems/{poemId}/ratings/me | 获取当前用户评分 | 是 |

**用户评分参数**：
- `score` (必填): 评分值，1.0-5.0
- `comment` (可选): 评分说明

**评分类型说明**：
- `1` - 用户评分
- `2` - AI评分

**评分维度**：格律、意境、用词、情感、创新

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
| GET | /api/forum/comments | 获取评论列表（需targetId和targetType参数） | 否 |
| POST | /api/forum/comments | 创建评论 | 是 |
| DELETE | /api/forum/comments/{id} | 删除评论 | 是 |
| POST | /api/forum/comments/{id}/like | 点赞/取消点赞评论 | 是 |

### 诗人纠错建议模块 (/api/poet-suggestions)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/poet-suggestions | 提交诗人纠错建议 | 是 |
| GET | /api/admin/poet-suggestions | 获取建议列表（管理员） | 是 |
| PUT | /api/admin/poet-suggestions/{id}/review | 审核建议（管理员） | 是 |

**提交建议请求体**（PoetSuggestion）：
```json
{
  "poetId": 1,
  "section": "biography",
  "content": "纠错内容"
}
```

**建议字段说明**：
- `poetId` (必填): 诗人ID
- `section` (必填): 纠错板块（biography/life_story/influence/anecdotes/other）
- `content` (必填): 纠错内容

**审核建议参数**：
- `status` (必填): 审核状态（approved/rejected）
- `reviewComment` (可选): 审核备注

### 精选诗人模块 (/api/poet-featured)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/poet-featured/random | 随机获取精选诗人 | 否 |
| GET | /api/poet-featured/list | 获取精选诗人列表 | 否 |
| GET | /api/poet-featured/{id} | 获取精选诗人详情 | 否 |

### 首页导航模块 (/api/home-navigation)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/home-navigation | 获取首页导航列表（按类型筛选） | 否 |

**请求参数**：
- `type` (可选): 导航类型，可选值 `works`（作品）、`genres`（流派）、`dynasties`（朝代）

### 诗话视野模块 (/api/vision)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/vision/articles | 获取文章列表 | 否 |
| GET | /api/vision/articles/featured | 获取推荐文章列表 | 否 |
| GET | /api/vision/articles/{id} | 获取文章详情 | 否 |
| POST | /api/vision/articles/{id}/like | 点赞文章 | 否 |

**请求参数**：
- `pageNum` (可选): 页码，默认1
- `pageSize` (可选): 每页数量，默认10
- `category` (可选): 文章分类筛选

### 搜索模块 (/api/search)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/search | 全局搜索（跨模块：诗词+诗人+帖子） | 否 |
| GET | /api/search/poems | 诗词搜索（支持协同过滤推荐） | 否 |
| GET | /api/search/poems/recommended | 获取推荐诗词（基于协同过滤） | 否 |
| GET | /api/search/poems/popular | 获取热门诗词 | 否 |
| GET | /api/search/poems/external | 从外部API获取古诗词 | 否 |
| GET | /api/search/poems/external/detail | 外部API获取诗词详情 | 否 |
| GET | /api/search/smart | 智能搜索（复合筛选+评分排序） | 否 |
| GET | /api/search/suggestions | 获取搜索建议 | 否 |
| GET | /api/search/hot | 获取热门搜索 | 否 |
| GET | /api/search/history | 获取搜索历史 | 是 |
| DELETE | /api/search/history | 清除搜索历史 | 是 |

**搜索建议接口说明**：
- 接口路径：`GET /api/search/suggestions`
- 功能：根据关键词获取搜索建议，支持诗词标题和诗人名称匹配
- 性能优化：使用 `selectList` 代替 `selectPage`，避免不必要的 `SELECT COUNT(*)` 查询
- 缓存：结果缓存5分钟（Redis）

**外部诗词接口说明**：
- 接口路径：`GET /api/search/poems/external`
- 功能：从外部API获取古诗词
- 关键词限制：关键词长度不能超过20字符（外部API限制），超过时直接返回空结果
- 频控机制：最小调用间隔1秒，每分钟最大30次调用

**智能搜索（复合筛选）接口参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| dynastyId | Long | 否 | 朝代ID筛选 |
| categoryId | Long | 否 | 分类ID筛选 |
| poetId | Long | 否 | 诗人ID精确筛选（下拉选择） |
| poetName | String | 否 | 诗人名称模糊筛选（自行输入） |
| keyword | String | 否 | 关键词搜索（匹配标题/内容/诗人名） |
| sortBy | String | 否 | 排序方式：latest（默认）、popular、likes |
| pageNum | int | 否 | 页码，默认1 |
| pageSize | int | 否 | 每页数量，默认10 |

**诗人筛选逻辑**：
- `poetId` 和 `poetName` 可同时存在，取 OR 关系
- `poetName` 通过 LIKE 模糊匹配诗人表，返回匹配的诗人 ID 列表
- 与其他维度（朝代、分类、关键词）取 AND 关系

**智能搜索响应示例**：
```json
{
  "code": 200,
  "data": {
    "list": [...],
    "total": 100,
    "searchLevel": "exact",
    "message": "找到 100 条结果",
    "suggestExternal": false
  }
}
```

**诗词搜索推荐接口参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | string | 是 | 搜索关键词 |
| page | int | 否 | 页码，默认1 |
| size | int | 否 | 每页数量，默认10 |
| limit | int | 否 | 返回数量，默认10（推荐/热门）/5（外部） |

**推荐算法说明**：
- 统一推荐服务（RecommendationService），同时支持诗人推荐和诗词推荐
- 基于用户的协同过滤（User-Based CF），综合收藏、点赞、浏览历史三种行为数据
- 收藏权重 5、点赞权重 3、浏览权重 1
- 使用余弦相似度计算用户相似性，推荐相似用户喜欢但当前用户未交互的内容
- 结合用户兴趣推荐（注册时选择的兴趣标签：古典/现代/自由体/外国）
- 新用户/无行为数据时降级为热门推荐
- 支持外部古诗词API（今日诗词）数据源
- 推荐结果缓存 2 小时（Redis @Cacheable）

### 外部 API — 接口盒子 (apihz.cn)

诗人详情页的诗词详情增强功能，调用接口盒子 API 获取诗词赏析内容。

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | https://cn.apihz.cn/api/zici/poetry.php | 古诗文大全查询 | 否（需ID和KEY） |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | string | 是 | 用户ID |
| key | string | 是 | 通讯秘钥 |
| words | string | 是 | 查询关键词（诗人名/诗句/标签）<br>⚠️ **长度限制**：不超过20字符（外部API限制） |
| page | int | 否 | 页码，每页5首，最大50页 |

**注意事项**：
- 关键词长度限制：外部API对`words`参数有长度限制（实测最大约20字符），超长会导致 `word参数长度过长` 错误
- 系统已在 `RecommendationServiceImpl.getExternalPoems` 方法中添加长度检查，超过限制时直接返回空结果
- 调用频控：最小间隔1秒，每分钟最大30次（防止触发外部API限制）

**返回字段**：

| 字段 | 说明 |
|------|------|
| name | 诗文标题 |
| content | 诗文内容 |
| author | 作者 |
| dynasty | 朝代 |
| ywjzsy | 译文及注释一 |
| ywjzse | 译文及注释二 |
| czbj | 创作背景 |
| jsy/jse | 鉴赏一/二 |
| sxy/sxe | 赏析一/二 |
| jj | 句解 |
| yj | 意境 |
| xzsf | 写作手法 |
| pj | 评价 |
| jx | 简析 |

### 外部 API — 接口盒子诗人查询 (apihz.cn)

诗人详情页的懒加载功能，当本地数据不完整时自动调用此 API 获取诗人信息。

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | https://cn.apihz.cn/api/zici/poet.php | 诗人查询 | 否（需ID和KEY） |

**请求参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | string | 是 | 用户ID |
| key | string | 是 | 通讯秘钥 |
| name | string | 是 | 诗人姓名 |
| page | int | 否 | 页码，默认1 |

**返回字段**：

| 字段 | 说明 | 数据库映射 |
|------|------|-----------|
| name | 诗人姓名 | `name` |
| image | 诗人头像 | `avatar` |
| tag | 诗人别号 | `pseudonym` |
| content | 诗人简介 | `biography` |
| rwsp | 人物生平 | `life_story` |
| zycj | 主要成就 | `influence` |
| ysdg | 轶事典故 | `anecdotes` |

**懒加载逻辑**：
- 仅在本地数据不完整时调用（biography、life_story、influence、anecdotes、avatar 任一为空）
- 获取数据后自动保存到数据库
- 从简介文本中自动提取生卒年和出生地
- 下次访问直接读取本地数据

### 文件模块 (/api/files) — 新增

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/files/upload | 上传文件 | 是 |
| DELETE | /api/files/{fileKey} | 删除文件 | 是 |
| GET | /api/files/{fileKey} | 获取文件信息 | 是 |

### 报修模块 (/api/repair) — 新增

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/repair/orders | 提交报修工单 | 是 |
| GET | /api/repair/orders | 获取我的报修列表 | 是 |
| GET | /api/repair/orders/{id} | 获取报修详情 | 是 |
| POST | /api/repair/orders/{id}/comments | 添加反馈评论 | 是 |
| PUT | /api/repair/orders/{id}/close | 关闭工单 | 是 |
| PUT | /api/repair/orders/{id}/satisfaction | 提交满意度评价 | 是 |
| GET | /api/repair/orders/{id}/comments | 获取评论列表 | 是 |

### 浏览历史模块 (/api/history) — 新增

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/history | 添加浏览历史 | 是 |
| GET | /api/history | 获取浏览历史 | 是 |
| DELETE | /api/history | 清空浏览历史 | 是 |

### 诗人草稿模块 (/api/poet-drafts) — 新增

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/poet-drafts | 保存诗人资料草稿 | 是 |
| GET | /api/admin/poet-drafts | 获取草稿列表（管理员） | 是 |
| GET | /api/admin/poet-drafts/{id} | 获取草稿详情（管理员） | 是 |
| PUT | /api/admin/poet-drafts/{id}/review | 审核草稿（管理员） | 是 |

### AI模块 (/api/ai) — 新增

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /api/ai/chat | AI问答 | 否 |
| POST | /api/ai/write-poem | 看图写诗 | 否 |
| POST | /api/ai/analyze | 智能分析 | 否 |
| POST | /api/ai/ocr | 图片OCR识别（古籍/书法/碑帖文字识别） | 否 |
| POST | /api/ai/couplet | AI对对联 | 否 |
| GET | /api/ai/config/{moduleCode} | 获取模块AI配置 | 否 |
| POST | /api/ai/fill-content | 触发AI填充（生成待审核内容） | 是 |
| GET | /api/ai/fill-status/{targetType}/{targetId} | 查询目标的AI填充状态 | 否 |
| POST | /api/ai/refresh-cache | 刷新AI模型缓存 | 是 |
| POST | /api/ai/preview | AI生成预览（不写入数据库） | 是 |
| POST | /api/ai/submit-review | 提交AI内容到审核表 | 是 |

**获取模块AI配置响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "moduleCode": "poet_chat",
    "moduleName": "诗人对话",
    "modelId": null,
    "requireVision": 0,
    "description": "诗人介绍助手",
    "promptTemplate": "提示词模板，支持{poetName}等变量",
    "maxLength": 150,
    "responseStyle": "concise",
    "firstResponseLength": 80,
    "enableMarkdown": 0
  }
}
```

**AI问答请求参数**：
```json
{
  "message": "用户问题",
  "model": "zhipu"  // 可选: zhipu, deepseek, kimi, nvidia
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
  "model": "zhipu"  // 可选: zhipu, deepseek, kimi, nvidia
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

**AI对对联请求参数**：
```json
{
  "upperCouplet": "上联内容",
  "model": "zhipu"  // 可选: zhipu, deepseek, kimi, nvidia
}
```

**AI对对联响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "upperCouplet": "上联内容",
    "lowerCouplet": "AI生成的下联",
    "model": "zhipu"
  }
}
```

**图片OCR识别请求参数**：
- Content-Type: multipart/form-data
- 参数：
  - `image`: 图片文件（必填）
  - `model`: AI模型（可选，默认zhipu）
  - `visionModel`: 视觉模型（可选，默认glm-4.6v-flash）

**图片OCR识别响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "text": "识别出的文字内容",
    "model": "zhipu",
    "visionModel": "glm-4.6v-flash"
  }
}
```

**AI填充请求参数**：
```json
{
  "targetType": "poem",
  "targetId": 1,
  "fieldName": "translation"
}
```
- `targetType` (必填): 目标类型，poem-诗词，poet-诗人
- `targetId` (必填): 目标ID
- `fieldName` (必填): 字段名
  - 诗词可选：translation（译文）、appreciation（赏析）、background（创作背景）
  - 诗人可选：biography（简介）、life_story（生平）、influence（影响）、evaluation（评价）、anecdotes（轶事）

**AI填充响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "targetType": "poem",
    "targetId": 1,
    "targetName": "静夜思",
    "fieldName": "translation",
    "content": "AI生成的译文内容...",
    "aiModel": "default",
    "status": 0,
    "createTime": "2026-06-19 12:00:00"
  }
}
```

**查询AI填充状态响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "targetType": "poem",
      "targetId": 1,
      "fieldName": "translation",
      "status": 0,
      "createTime": "2026-06-19 12:00:00"
    }
  ]
}
```
- `status`: 0-待审核，1-已通过，2-已拒绝

### 静态页面模块 (/api/static-pages) — 新增

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/static-pages/{pageKey} | 根据pageKey获取静态页面内容 | 否 |
| GET | /api/admin/static-pages | 获取静态页面列表（管理端） | 是 |
| GET | /api/admin/static-pages/{id} | 获取静态页面详情（管理端） | 是 |
| PUT | /api/admin/static-pages/{id} | 更新静态页面内容（管理端） | 是 |

### 诗词内容缓存模块 (/api/poem-content) — 新增

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/poem-content/cache | 获取缓存的诗词内容 | 否 |
| POST | /api/poem-content/cache | 保存诗词内容缓存 | 是 |

### 韵脚查询模块 (/api/rhyme)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/rhyme/query | 按汉字查询韵脚 | 否 |
| GET | /api/rhyme/group | 按韵部查询同韵字 | 否 |

**按汉字查询韵脚参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| character | string | 是 | 查询的汉字 |

**按汉字查询韵脚响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "character": "东",
    "rhymeGroup": "上平一东",
    "toneType": "平声",
    "rhymeCategory": "平水韵"
  }
}
```

**按韵部查询同韵字参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| rhymeGroup | string | 是 | 韵部名称（如：上平一东） |

**按韵部查询同韵字响应示例**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {"character": "东", "toneType": "平声"},
    {"character": "同", "toneType": "平声"},
    {"character": "风", "toneType": "平声"}
  ]
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
| POST | /api/admin/users | 创建用户 | 是 |
| PUT | /api/admin/users/{id} | 更新用户信息 | 是 |
| DELETE | /api/admin/users/{id} | 删除用户 | 是 |
| GET | /api/admin/poems | 获取诗词列表 | 是 |
| GET | /api/admin/poems/{id} | 获取诗词详情 | 是 |
| POST | /api/admin/poems | 创建诗词 | 是 |
| PUT | /api/admin/poems/{id} | 更新诗词信息 | 是 |
| PUT | /api/admin/poems/{id}/audit | 审核诗词 | 是 |
| DELETE | /api/admin/poems/{id} | 删除诗词 | 是 |
| GET | /api/admin/categories | 获取分类列表 | 是 |
| GET | /api/admin/categories/{id} | 获取分类详情 | 是 |
| POST | /api/admin/categories | 创建分类 | 是 |
| PUT | /api/admin/categories/{id} | 更新分类 | 是 |
| DELETE | /api/admin/categories/{id} | 删除分类 | 是 |
| GET | /api/admin/dynasties | 获取朝代列表 | 是 |
| GET | /api/admin/dynasties/{id} | 获取朝代详情 | 是 |
| POST | /api/admin/dynasties | 创建朝代 | 是 |
| PUT | /api/admin/dynasties/{id} | 更新朝代 | 是 |
| DELETE | /api/admin/dynasties/{id} | 删除朝代 | 是 |
| GET | /api/admin/poets | 获取诗人列表 | 是 |
| GET | /api/admin/poets/{id} | 获取诗人详情 | 是 |
| POST | /api/admin/poets | 创建诗人 | 是 |
| PUT | /api/admin/poets/{id} | 更新诗人信息 | 是 |
| DELETE | /api/admin/poets/{id} | 删除诗人 | 是 |
| POST | /api/admin/poets/sync/{name} | 同步单个诗人数据 | 是 |
| POST | /api/admin/poets/sync-all | 同步所有诗人数据 | 是 |
| GET | /api/admin/forum-posts | 获取帖子列表 | 是 |
| GET | /api/admin/forum-posts/{id} | 获取帖子详情 | 是 |
| PUT | /api/admin/forum-posts/{id}/status | 更新帖子状态 | 是 |
| DELETE | /api/admin/forum-posts/{id} | 删除帖子 | 是 |
| PUT | /api/admin/forum-posts/{id}/audit | 审核帖子 | 是 |
| GET | /api/admin/comments | 获取评论列表 | 是 |
| PUT | /api/admin/comments/{id}/audit | 审核评论 | 是 |
| DELETE | /api/admin/comments/{id} | 删除评论 | 是 |
| GET | /api/admin/audit/stats | 获取审核统计数据 | 是 |
| GET | /api/admin/logs | 获取操作日志列表 | 是 |
| GET | /api/admin/poet-profiles | 获取诗人认证申请列表 | 是 |
| PUT | /api/admin/poet-profiles/{id}/verify | 审核诗人认证 | 是 |
| GET | /api/admin/ai-contents | 获取AI生成内容列表 | 是 |
| PUT | /api/admin/ai-contents/{id}/approve | 审核通过AI生成内容 | 是 |
| PUT | /api/admin/ai-contents/{id}/reject | 审核拒绝AI生成内容 | 是 |
| GET | /api/admin/poet-featured | 获取精选诗人列表（管理端） | 是 |
| GET | /api/admin/poet-featured/{id} | 获取精选诗人详情（管理端） | 是 |
| POST | /api/admin/poet-featured | 创建精选诗人 | 是 |
| PUT | /api/admin/poet-featured/{id} | 更新精选诗人 | 是 |
| DELETE | /api/admin/poet-featured/{id} | 删除精选诗人 | 是 |
| GET | /api/admin/home-navigation | 获取首页导航列表（管理端） | 是 |
| GET | /api/admin/home-navigation/manage | 管理端获取首页导航列表 | 是 |
| POST | /api/admin/home-navigation | 创建首页导航 | 是 |
| PUT | /api/admin/home-navigation/{id} | 更新首页导航 | 是 |
| DELETE | /api/admin/home-navigation/{id} | 删除首页导航 | 是 |
| GET | /api/admin/stats/visits | 获取访问统计数据 | 是 |
| GET | /api/admin/stats/visits/trend | 获取访问趋势数据 | 是 |
| POST | /api/admin/cache/clear-poets | 清除诗人缓存 | 是 |
| POST | /api/admin/cache/clear-all | 清除所有缓存 | 是 |
| GET | /api/admin/repairs | 获取报修列表 | 是 |
| GET | /api/admin/repairs/{id} | 获取报修详情 | 是 |
| PUT | /api/admin/repairs/{id}/status | 更新报修状态 | 是 |
| PUT | /api/admin/repairs/{id}/assign | 分配处理人 | 是 |
| POST | /api/admin/repairs/{id}/comments | 添加内部备注 | 是 |
| GET | /api/admin/repairs/{id}/comments | 获取报修评论列表 | 是 |
| GET | /api/admin/repairs/stats | 获取报修统计数据 | 是 |

**审核诗词请求体**：
```json
{
  "status": 1,
  "reason": "审核备注"
}
```
- `status`: 1-已通过，2-已拒绝

**审核帖子请求体**：
```json
{
  "status": 1,
  "reason": "审核备注"
}
```
- `status`: 1-已发布，2-已关闭

**审核评论请求体**：
```json
{
  "status": 1,
  "reason": "审核备注"
}
```
- `status`: 0-隐藏，1-正常

**审核统计数据响应**：
```json
{
  "poems": 5,
  "comments": 10,
  "poetProfiles": 3,
  "aiContents": 8,
  "poetDrafts": 2,
  "poetSuggestions": 4
}
```

**审核诗人认证请求体**：
```json
{
  "status": 1,
  "reason": "审核备注"
}
```
- `status`: 1-已认证，3-认证失败

**诗人认证申请列表参数**：
- `status` (可选): 按认证状态筛选（0-未认证，1-已认证，2-认证中，3-认证失败）

**AI内容审核列表参数**：
- `page` (可选): 页码，默认1
- `size` (可选): 每页数量，默认10
- `status` (可选): 按状态筛选（0-待审核，1-已通过，2-已拒绝）

**审核AI内容请求体**：
```json
{
  "reviewComment": "审核备注"
}
```
- `reviewComment` (可选): 审核备注

### AI模型配置模块 (/api/admin/ai-models) — 新增

> 以下接口为管理员专属接口，用于管理AI模型配置，支持CRUD、启用/禁用、默认模型设置、连接测试。

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /api/admin/ai-models | 获取所有AI模型 | 是 |
| GET | /api/admin/ai-models/enabled | 获取已启用的AI模型 | 是 |
| GET | /api/admin/ai-models/{id} | 获取AI模型详情 | 是 |
| POST | /api/admin/ai-models | 创建AI模型 | 是 |
| PUT | /api/admin/ai-models/{id} | 更新AI模型 | 是 |
| DELETE | /api/admin/ai-models/{id} | 删除AI模型 | 是 |
| POST | /api/admin/ai-models/{id}/toggle | 启用/禁用AI模型 | 是 |
| POST | /api/admin/ai-models/{id}/set-default | 设置默认AI模型 | 是 |
| GET | /api/admin/ai-models/providers | 获取提供商列表 | 是 |
| POST | /api/admin/ai-models/{id}/test | 测试模型连接 | 是 |
| GET | /api/admin/ai-models/modules | 获取所有模块配置 | 是 |
| GET | /api/admin/ai-models/modules/{moduleCode}/models | 获取模块可用模型列表 | 是 |
| PUT | /api/admin/ai-models/modules/{moduleCode} | 更新模块配置 | 是 |

**AI模型实体字段（AiModel）**：
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| name | VARCHAR(50) | 模型标识（如 zhipu、deepseek、kimi、nvidia） |
| displayName | VARCHAR(100) | 显示名称（如 智谱AI、DeepSeek） |
| provider | VARCHAR(50) | 提供商 |
| modelType | ENUM('text','vision','both') | 模型类型：text-纯文本，vision-视觉，both-两者 |
| apiUrl | VARCHAR(255) | API地址 |
| apiKey | VARCHAR(255) | API密钥 |
| modelId | VARCHAR(100) | 模型ID |
| visionModelId | VARCHAR(100) | 视觉模型ID（可空） |
| maxTokens | INT | 最大token数 |
| extraConfig | JSON | 额外配置 |
| isEnabled | TINYINT | 是否启用：0-禁用，1-启用 |
| isDefault | TINYINT | 是否默认：0-否，1-是 |
| sortOrder | INT | 排序顺序 |

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
| 2101 | 文章不存在 |
| 3001 | 帖子不存在 |

---

**文档版本**：v3.4
**最后更新**：2026-07-28（搜索模块优化：补充搜索建议接口性能优化说明，外部诗词接口关键词长度限制）
**维护人员**：墨渊开发团队