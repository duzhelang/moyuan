# 业务模块说明

## 概述

本文档描述"古今诗话——墨渊"项目的 23 大业务模块功能说明。

## 模块总览

| 模块 | 说明 | 状态 |
|------|------|------|
| 首页模块 | 网站入口，展示精选内容 | 前端已实现（已重写，连接后端API） |
| 诗词模块 | 诗词浏览、搜索、赏析、评分、用户发布 | 前端+后端已实现 |
| 诗人模块 | 诗人介绍、作品集、智能推荐 | 前端+后端已实现 |
| 诗人认证模块 | 诗人认证申请、审核、资料管理 | 前端+后端已实现 |
| 论坛模块 | 用户交流、发帖评论 | 前端+后端已实现 |
| 交流广场 | 轻量级交流空间，快速发帖和互动 | 前端+后端已实现 |
| AI模块 | AI 辅助创作、看图写诗、智能分析、AI对联、古籍OCR识文、AI模型配置管理 | 前端+后端已实现 |
| 搜索模块 | 全文搜索 | 前端+后端已实现（跨模块搜索） |
| 用户模块 | 注册登录、个人中心、用户主页 | 前端+后端已实现 |
| 管理模块 | 后台管理、内容管理、数据统计、内容审核 | 前端+后端已实现 |
| 文件模块 | 文件上传、图片管理 | 前端+后端已实现 |
| 浏览历史模块 | 浏览记录、历史查看 | 前端+后端已实现 |
| 诗话视野模块 | 诗词文化文章浏览、详情展示 | 前端+后端已实现 |
| 首页导航模块 | 首页导航菜单管理（作品/流派/朝代） | 前端+后端已实现 |
| 精选诗人模块 | 精选诗人展示和管理 | 前端+后端已实现 |
| 诗词评分模块 | 诗词用户评分和AI评分 | 前端+后端已实现 |
| 韵律查询模块 | 平水韵韵脚查询、同韵字查询 | 前端+后端已实现 |
| 报修模块 | 用户报修工单提交、进度跟踪、满意度评价 | 前端+后端已实现 |
| 静态页面模块 | 网站静态页面管理（使用条款、隐私政策、联系我们） | 前端+后端已实现 |
| 诗人草稿模块 | 诗人资料编辑草稿、审核流程 | 前端+后端已实现 |
| 诗词内容缓存模块 | 诗词外部获取内容的缓存管理 | 前端+后端已实现 |
| 联系我们模块 | 用户反馈表单页面 | 前端已实现 |
| **诗云模块** | **Three.js 3D 星空可视化，诗人星系和诗星光点云** | **前端已实现** |

---

## 1. 首页模块

### 功能说明

- 轮播图展示精选诗词（Swiper轮播，主轮播+诗文轮播）
- 导航菜单（朝代、流派、诗人、作品、论坛）
- 推荐诗人卡片
- 推荐作品卡片
- 分类精赏
- 古诗推选（历史的印痕/为您推荐/搜索/详情弹窗）
- 当代精选（分页/点赞/收藏/评论）
- 论坛预览（标题/搜索/滚动字幕/精选板块/热帖）
- AI功能区域（看图写诗/智能分析/对对联/古籍OCR识文）
- 响应式布局（4断点：1200/992/768/576px）

### 页面组件

| 组件 | 说明 |
|------|------|
| HomeNavBar | 首页导航栏（导航菜单+搜索栏，含固定定位） |
| LoginDropdown | 登录下拉窗（快速登录+用户菜单） |
| HomeNavigation | 首页分类导航（作品/流派/朝代，自动轮播） |
| DailyPoetry | 每日诗词组件 |
| PoetCard | 精选诗人卡片 |
| HomeCarousel | Swiper轮播组件（主轮播+诗文轮播） |
| ForumPreview | 论坛预览组件 |
| AncientPoemSelection | 古诗推选组件 |
| ContemporaryPoems | 当代精选拄件 |
| AppFooter | 底部信息 |

### 静态数据

| 文件 | 内容 |
|------|------|
| data/home-ancient-poems.json | 10首经典古诗 |
| data/home-contemporary-poems.json | 11首当代诗词 |
| data/home-poetry-library.json | AI区域数据（诗海拾贝/分析示例/对联示例） |

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
- 诗词译文、名人点评、创作背景
- 点赞、收藏功能
- **诗词评分**（新增）：用户评分（1.0-5.0分）和AI评分
- **原创标识**（新增）：支持标记诗词为原创作品
- **内容审核**（新增）：诗词发布后需经过审核

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/poems | 诗词列表 | backend PoemController |
| GET /api/poems/modern/page | 分页获取现代诗词（支持筛选和排序） | sc-moyuan-backend PoemController |
| GET /api/poems/{id} | 诗词详情 | backend PoemController |
| GET /api/poems/search | 搜索诗词 | backend PoemController |
| GET /api/poems/{id}/ratings | 获取诗词评分详情 | sc-moyuan-backend PoemRatingController |
| POST /api/poems/{id}/ratings | 用户评分（1.0-5.0） | sc-moyuan-backend PoemRatingController |
| POST /api/poems/{id}/ratings/ai | 请求AI评分 | sc-moyuan-backend PoemRatingController |
| GET /api/poems/{id}/ratings/ai | 获取AI评分 | sc-moyuan-backend PoemRatingController |

### 评分维度

诗词评分支持以下维度：
- **格律**：诗词的格律规范性
- **意境**：诗词的意境表达
- **用词**：诗词的用词精准度
- **情感**：诗词的情感表达
- **创新**：诗词的创新程度

### 前端路由

| 路径 | 说明 |
|------|------|
| `/poem` | 诗词列表页 |
| `/poem/create` | 发布新诗（支持原创标识） |
| `/poem/:id` | 诗词详情页（含评分功能） |

---

## 3. 诗人模块

### 功能说明

- 诗人列表浏览
- 按朝代筛选
- 诗人详情展示（左侧导航、英雄区域、简介、生平、影响、评价、轶事）
- 诗人生平简介
- 诗人作品集（分页）
- **诗词详情增强**：点击诗词卡片弹出详情面板，调用接口盒子 API 获取译文、注释、创作背景、名人点评等
- **诗人数据懒加载**：本地数据不完整时自动从接口盒子 API 获取并保存到数据库
- 相关诗人推荐（基于协同过滤算法）
- **右侧浮动面板**：相关诗人列表、诗词特点、AI对话窗口、快捷工具栏
- **AI诗人对话**：基于后台配置的提示词模板，支持多轮对话，用户点击"开始对话"按钮后发起首次提问，支持本地缓存（按诗人ID区分，有效期1小时），刷新页面自动加载缓存记录，支持语音朗读
- **收藏诗人**：使用 localStorage 存储收藏状态
- **分享功能**：支持 Web Share API 或复制链接
- **朗读功能**：支持单段朗读和全文朗读
- **字号调节**：4档字号可选（14px/16px/18px/20px）
- **粒子动画背景**：使用 useParticles composable 实现流行粒子动画效果，粒子颜色为暖色调（#d4af87, #f0e4d7, #c9a06c, #8b7355），适配页面背景
- **半透明毛玻璃效果**：内容区块（.content-section）背景透明度 0.62，模糊效果 blur(3px)，其他面板透明度 0.85，模糊效果 blur(10px)

### 前端组件

| 组件 | 说明 |
|------|------|
| PoetryDetailDialog | 诗词详情弹窗，展示接口盒子 API 返回的赏析内容 |
| ai-prompt.ts | AI提示词构建工具函数，支持变量替换 |

### 诗人数据懒加载机制

当用户访问诗人详情页时，后端会检查本地数据库中的诗人信息是否完整。如果以下任一字段为空，则自动调用接口盒子 API 获取数据：

| 检查字段 | 说明 | API 字段映射 |
|----------|------|-------------|
| `biography` | 简介 | `content` |
| `life_story` | 生平 | `rwsp` |
| `influence` | 影响 | `zycj` |
| `anecdotes` | 轶事 | `ysdg` |
| `avatar` | 头像 | `image` |

**处理流程**：
1. 查询本地数据库
2. 检查字段完整性
3. 调用接口盒子诗人查询 API
4. 提取数据并保存到数据库
5. 从简介文本中自动提取生卒年和出生地
6. 如果 API 无头像且本地无头像，使用 `PoetDefaultAvatar.getAvatar(poet)` 智能分配预设头像
7. 下次访问直接读取本地数据

### 诗人预设头像分配

当诗人无头像时（API 未返回或同步失败），系统通过 `PoetDefaultAvatar` 工具类按诗人类型智能分配预设头像。

**头像库**（9 张预设图片，位于 `/img/poet_avatars/`）：

| 头像文件 | 分配规则 |
|----------|----------|
| `avatar_cn_ancient_male_01.jpg` | 中国古典男性（按 id 轮换） |
| `avatar_cn_ancient_male_02.jpg` | 中国古典男性（按 id 轮换） |
| `avatar_cn_ancient_male_03.jpg` | 中国古典男性（按 id 轮换） |
| `avatar_cn_ancient_female_01.jpg` | 中国古典女性（李清照、唐婉等） |
| `avatar_cn_modern_male_01.jpg` | 中国现代男性 |
| `avatar_cn_modern_female_01.jpg` | 中国现代女性（林徽因、冰心等） |
| `avatar_west_classical_male_01.jpg` | 西方古典诗人（莎士比亚、歌德等） |
| `avatar_west_romantic_male_01.jpg` | 西方浪漫主义诗人（拜伦、雪莱等） |
| `avatar_west_modern_female_01.jpg` | 西方现代女性诗人（狄金森等） |

**分配逻辑**（`PoetDefaultAvatar.getAvatar(poet)`）：
1. 判断性别：匹配已知女性诗人名单
2. 判断地区：西方诗人名单匹配 或 无朝代信息
3. 判断时期：`poetType` 字段区分古代/现代
4. 按 `地区_时期_性别` 组合匹配头像池
5. 兜底：同地区随机 → 全局随机

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/poets | 诗人列表 | sc-moyuan-backend PoetController |
| GET /api/poets/{id} | 诗人详情 | sc-moyuan-backend PoetController |
| GET /api/poets/recommend | 推荐著名诗人（协同过滤） | sc-moyuan-backend PoetController |
| GET /api/poets/popular | 热门著名诗人排行 | sc-moyuan-backend PoetController |
| GET https://cn.apihz.cn/api/zici/poetry.php | 诗词详情增强（外部） | 接口盒子 API |

**推荐接口参数**：
- `limit` (可选): 返回数量，默认 6（recommend）/ 10（popular），上限 20/50

**诗词详情增强参数**：
- `id`: 用户ID
- `key`: 通讯秘钥
- `words`: 诗词标题关键词
- `page`: 页码

### 前端路由

| 路径 | 说明 |
|------|------|
| `/poet` | 诗人列表页 |
| `/poet/:id` | 诗人详情页 |

---

## 3.5 诗人认证模块（新增）

### 功能说明

- 诗人认证申请：用户提交诗人认证申请，填写诗人资料
- 认证审核：管理员审核诗人认证申请，通过或拒绝
- 诗人资料管理：认证诗人可管理自己的诗人资料
- 认证状态展示：用户主页展示诗人认证状态和资料

### 认证流程

```
用户提交认证申请 → 管理员审核 → 审核通过/拒绝
    ↓                              ↓
填写诗人资料                    更新认证状态
（笔名、简介、擅长体裁等）        用户获得诗人标识
```

### 认证状态

| 状态值 | 说明 |
|--------|------|
| 0 | 未认证 |
| 1 | 已认证 |
| 2 | 认证中（待审核） |
| 3 | 认证失败 |

### 诗人资料字段

| 字段 | 说明 |
|------|------|
| penName | 笔名 |
| realName | 真实姓名 |
| specialty | 擅长体裁（古体诗,近体诗,词,曲,现代诗） |
| introduction | 诗人简介 |
| literaryConcept | 创作理念 |
| achievements | 主要成就 |
| contactInfo | 联系方式（邮箱/微信） |

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/poet-profile/me | 获取当前用户诗人资料 | sc-moyuan-backend PoetProfileController |
| POST /api/poet-profile/apply | 申请诗人认证 | sc-moyuan-backend PoetProfileController |
| GET /api/poet-profile/{userId} | 获取指定用户诗人资料 | sc-moyuan-backend PoetProfileController |
| GET /api/admin/poet-profiles | 获取诗人认证申请列表（管理员） | sc-moyuan-backend AdminController |
| PUT /api/admin/poet-profiles/{id}/verify | 审核诗人认证（管理员） | sc-moyuan-backend AdminController |

### 前端路由

| 路径 | 说明 |
|------|------|
| `/user/:id/homepage` | 用户主页（展示诗人认证状态和资料） |
| `/user/profile` | 个人中心（提交认证申请入口） |

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

## 5. 交流广场模块

### 功能说明

- 轻量级交流空间，快速发帖和互动
- 帖子列表浏览（分页加载）
- 快速发帖（支持图片上传）
- 帖子详情展开
- 评论功能（支持多级回复）
- 点赞功能
- 时间格式化显示（刚刚、几分钟前、几小时前等）

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/forum/posts | 帖子列表 | sc-moyuan-backend ForumController |
| POST /api/forum/posts | 发布帖子 | sc-moyuan-backend ForumController |
| POST /api/forum/posts/{id}/like | 点赞帖子 | sc-moyuan-backend ForumController |
| GET /api/forum/comments | 评论列表 | sc-moyuan-backend ForumController |
| POST /api/forum/comments | 发表评论 | sc-moyuan-backend ForumController |
| POST /api/forum/comments/{id}/like | 点赞评论 | sc-moyuan-backend ForumController |

### 前端路由

| 路径 | 说明 |
|------|------|
| `/communicate` | 交流广场页面 |

### 前端组件

| 组件 | 说明 |
|------|------|
| communicate/index.vue | 交流广场主页面（帖子列表、发帖、评论、点赞） |

---

## 6. AI 模块

### 功能说明

- AI诗词问答（支持多模型切换；实测可用：智谱 glm-4-flash / glm-4.7-flash，DeepSeek deepseek-v4-flash，OpenRouter MiniMax-M2.7 免费档；默认模型为智谱 glm-4.7-flash）
- 看图写诗（上传图片，AI生成古诗词，支持视觉模型GLM-4V-Flash）
- 智能分析（输入诗句进行深度解析）
- AI对对联（输入上联，AI生成下联，遵循平仄规则）
- **古籍OCR识文**（上传古诗词图片/书法作品/碑帖，AI精准识别文字内容，保留原文结构，标注印章题款）
- AI模型配置管理（后台管理，支持CRUD、启用/禁用、默认模型设置、连接测试）
- **诗词AI助手（poetry_chat）**：寻章摘句页面的AI功能模块，包括译文注释、名人点评、创作背景、历史文化背景、AI逐句解析、AI诗词助手对话
  - **译文注释**：逐句翻译，关键词语注释
  - **名人点评**：历代名家点评（引用《诗品》《沧浪诗话》《人间词话》等）、文学史地位
  - **创作背景**：诗人生平、创作缘由、历史背景
  - **AI逐句解析**：点击诗句展开解释，支持多句同时展开，内存缓存不写库，可关闭
  - **弹窗交互**：三个功能按钮以向左展开的弹窗形式呈现，覆盖在诗词内容上方，不挤压布局
  - **搜索式加载**：AI生成过程中以搜索动画替代旋转加载图标

### 技术方案

- 调用大语言模型 API（智谱AI、DeepSeek、Kimi、NVIDIA NIM）
- 智谱AI视觉模型（GLM-4V-Flash / GLM-4.6V-Flash）用于看图写诗和OCR识别
- OCR识别复用视觉模型基础设施，通过专用提示词引导模型精准识别古籍文字
- AI模型配置存入数据库（ai_model 表），支持动态管理；api_key 为占位符，真实密钥从 secrets/application-secrets.yml 的 ai.providers.* 运行时注入
- 前端首页集成AI问答组件（右下角悬浮按钮）
- 前端首页"展风拓潮"模块集成看图写诗、智能分析、对联和OCR功能

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| POST /api/ai/chat | AI问答 | sc-moyuan-backend AiController |
| POST /api/ai/write-poem | 看图写诗 | sc-moyuan-backend AiController |
| POST /api/ai/analyze | 智能分析 | sc-moyuan-backend AiController |
| POST /api/ai/ocr | 图片OCR识别（古籍/书法/碑帖） | sc-moyuan-backend AiController |
| POST /api/ai/couplet | AI对对联 | sc-moyuan-backend AiController |
| GET /api/ai/config/{moduleCode} | 获取模块AI配置（提示词模板、回答风格等） | sc-moyuan-backend AiController |

**获取模块AI配置**：根据 moduleCode 返回对应模块的配置信息，包含提示词模板（promptTemplate）、最大回答长度（maxLength）、回答风格（responseStyle）、首次回答长度（firstResponseLength）等。前端用于构建 AI 对话的系统约束。

**提示词模板变量**：promptTemplate 支持以下变量占位符，前端 `buildAiPrompt` 会自动替换：

| 变量 | 说明 | 适用模块 |
|------|------|----------|
| `{poetName}` | 诗人名称 | poetry_chat、poet_chat |
| `{poemTitle}` | 诗词标题 | poetry_chat |
| `{poemContent}` | 诗词全文内容 | poetry_chat |
| `{maxLength}` | 最大回答字数 | 所有模块 |
| `{styleHint}` | 风格提示（首次/后续） | 所有模块 |

> 当 promptTemplate 中未使用 `{poemTitle}` 时，前端会自动追加诗词上下文（标题+内容），确保 AI 始终知道当前讨论的具体诗作。

### AI模型管理接口（管理员）

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/admin/ai-models | 获取所有AI模型 | sc-moyuan-backend AiModelConfigController |
| GET /api/admin/ai-models/enabled | 获取已启用的AI模型 | sc-moyuan-backend AiModelConfigController |
| POST /api/admin/ai-models | 创建AI模型 | sc-moyuan-backend AiModelConfigController |
| PUT /api/admin/ai-models/{id} | 更新AI模型 | sc-moyuan-backend AiModelConfigController |
| DELETE /api/admin/ai-models/{id} | 删除AI模型 | sc-moyuan-backend AiModelConfigController |
| POST /api/admin/ai-models/{id}/toggle | 启用/禁用模型 | sc-moyuan-backend AiModelConfigController |
| POST /api/admin/ai-models/{id}/set-default | 设置默认模型 | sc-moyuan-backend AiModelConfigController |
| POST /api/admin/ai-models/{id}/test | 测试连接 | sc-moyuan-backend AiModelConfigController |

### 前端组件

| 组件 | 说明 |
|------|------|
| AI问答对话框 | 首页右下角悬浮按钮，点击打开AI问答对话框 |
| 展风拓潮模块 | 首页"展风拓潮"区域，包含看图写诗、智能分析和古籍OCR识文功能 |
| AI模型管理页面 | 管理后台 `admin/ai-models`，支持模型CRUD和配置管理 |

---

## 7. 搜索模块

### 功能说明

- **复合筛选搜索**（核心）：支持朝代、分类、诗人、关键词等多维度组合筛选
- **诗人模糊输入**：诗人下拉框支持自行输入任意诗人名称进行模糊匹配
- **智能搜索**（SmartSearch）：基于评分的渐进式匹配搜索
- **拼音匹配**：支持拼音输入匹配诗词标题和诗人名
- 全局搜索（跨模块：诗词+诗人+帖子）
- 诗词搜索推荐（协同过滤算法排序）
- 为您推荐（基于用户兴趣和浏览历史）
- 搜索建议（Redis 缓存，5分钟过期）
- 热门搜索（Redis ZSet 统计）
- 搜索历史（Redis List，30天过期，最多20条）

### 技术方案

- **搜索核心**：SmartSearchService，多条件复合查询 + 评分排序
- **搜索引擎**：MySQL LIKE 模糊匹配（初期），Elasticsearch（后期优化）
- **拼音支持**：PinyinUtil 工具类，可配置搜索范围（默认200条，最大1000条）
- **缓存策略**：Redis 缓存搜索建议（5分钟）、热门搜索（实时统计）、搜索历史（30天）
- **推荐服务**：RecommendationService，协同过滤 + 兴趣推荐 + 外部API
- **外部API集成**：今日诗词API，补充古诗词数据源

### 复合搜索策略

智能搜索（`/api/search/smart`）支持以下筛选维度的任意组合：

| 维度 | 参数 | 匹配方式 | 说明 |
|------|------|----------|------|
| 朝代 | `dynastyId` | 精确匹配 | 选择特定朝代的诗词 |
| 分类 | `categoryId` | 精确匹配 | 选择特定分类（古体诗、近体诗等） |
| 诗人（精确） | `poetId` | 精确匹配 | 从下拉列表选择数据库已有的诗人 |
| 诗人（模糊） | `poetName` | 模糊匹配（LIKE） | 自行输入诗人名称，模糊匹配数据库中的诗人 |
| 关键词 | `keyword` | 标题/内容/诗人名匹配 | 同时匹配诗词标题、内容、诗人名 |
| 排序 | `sortBy` | 按字段排序 | latest（最新）、popular（热门）、likes（点赞） |

**诗人筛选逻辑**：
- `poetId` 和 `poetName` 可同时存在，取 OR 关系
- `poetName` 通过 `LIKE` 模糊匹配诗人表，返回匹配的诗人 ID 列表
- 最终查询条件为：`poetId = ? OR poetId IN (模糊匹配的ID列表)`
- 与其他维度（朝代、分类、关键词）取 AND 关系

### 搜索评分算法

搜索结果按评分排序，评分规则：

| 匹配类型 | 分数 | 说明 |
|----------|------|------|
| 标题完全匹配 | 1.0 | 去除标点后完全一致 |
| 标题包含关键词 | 0.8 | 标题中包含完整关键词 |
| 诗人名精确匹配 | 0.9 | 诗人名与关键词完全一致 |
| 诗人名包含关键词 | 0.7 | 诗人名中包含关键词 |
| 内容包含关键词 | 0.5 | 诗词内容中包含完整关键词 |
| 标题包含拆分词 | 0.3 | 标题中包含拆分后的单个关键词 |
| 内容包含拆分词 | 0.2 | 内容中包含拆分后的单个关键词 |
| 拼音匹配（基础） | 0.3 | 拼音匹配基础分 |
| 拼音标题加成 | +0.2 | 拼音匹配标题额外加分 |
| 拼音诗人加成 | +0.1 | 拼音匹配诗人名额外加分 |

### 搜索流程

```
用户输入筛选条件（朝代/分类/诗人/关键词）
    ↓
前端调用 GET /api/search/smart
    ↓
后端 SmartSearchService 处理：
    ├─ poetName → resolvePoetNameIds() 模糊匹配诗人ID
    ├─ 无关键词 → searchWithoutKeyword() 条件筛选
    └─ 有关键词 → 三路并行搜索：
        ├─ searchPoems() 标题/内容匹配
        ├─ searchByPoetName() 诗人名匹配
        └─ searchByPinyin() 拼音匹配
    ↓
去重合并 → 评分排序 → 分页返回
    ↓
保存搜索历史（已登录用户）
```

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/search?keyword=xxx | 全局搜索（跨模块：诗词+诗人+帖子） | sc-moyuan-backend SearchController |
| GET /api/search/poems?keyword=xxx | 诗词搜索（支持协同过滤推荐） | sc-moyuan-backend SearchController |
| GET /api/search/poems/recommended | 获取推荐诗词（基于协同过滤） | sc-moyuan-backend SearchController |
| GET /api/search/poems/popular | 获取热门诗词 | sc-moyuan-backend SearchController |
| GET /api/search/poems/external?keyword=xxx | 从外部API获取古诗词 | sc-moyuan-backend SearchController |
| GET /api/search/smart | 智能搜索（复合筛选+评分排序） | sc-moyuan-backend SearchController |
| GET /api/search/suggestions | 获取搜索建议 | sc-moyuan-backend SearchController |
| GET /api/search/hot | 获取热门搜索 | sc-moyuan-backend SearchController |
| GET /api/search/history | 获取搜索历史 | sc-moyuan-backend SearchController |
| DELETE /api/search/history | 清除搜索历史 | sc-moyuan-backend SearchController |

### 智能搜索参数

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

### 智能搜索响应

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

### 推荐算法说明

统一推荐服务（RecommendationService）实现细节：

1. **协同过滤算法**
   - 构建用户-内容评分矩阵（收藏5分、点赞3分、浏览1分）
   - 使用余弦相似度计算用户相似性
   - 推荐相似用户喜欢但当前用户未交互的内容
   - 最多参考20个相似用户

2. **兴趣推荐**
   - 基于用户注册时选择的兴趣标签（古典/现代/自由体/外国）
   - 推荐对应朝代的诗词和诗人

3. **分数合并**
   - 协同过滤分数 + 兴趣推荐分数
   - 按总分降序排列

4. **兜底策略**
   - 未登录用户：返回热门推荐
   - 无行为数据：返回热门推荐
   - 推荐结果为空：返回热门推荐

### 前端组件

| 组件 | 说明 |
|------|------|
| poem/list.vue - 筛选区域 | 朝代/分类/诗人/排序筛选器，支持复合搜索 |
| home/index.vue - 历史的印痕模块 | 首页搜索框、搜索结果展示、为您推荐标签页 |
| 搜索结果卡片 | 显示诗词信息、来源标签、推荐理由 |

### 前端路由

| 路径 | 说明 |
|------|------|
| `/search` | 全局搜索结果页 |
| `/poem` | 诗词列表页（含复合筛选搜索） |
| `/` (首页) | 历史的印痕模块内嵌搜索功能 |

---

## 8. 用户模块

### 功能说明

- 用户注册（支持兴趣选项，用于协同过滤推荐）
- 用户登录
- 个人中心
- 修改密码
- 修改个人资料（性别、生日等）
- 头像上传
- 我的收藏
- 我的点赞
- 我的帖子
- **用户主页**（新增）：展示用户信息、诗人认证状态、作品列表、统计数据
- **用户统计**（新增）：作品数、获赞数、被收藏数等
- **诗人认证入口**（新增）：从个人中心申请诗人认证

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| POST /api/auth/register | 注册（支持兴趣选项） | sc-moyuan-backend AuthController |
| POST /api/auth/login | 登录 | sc-moyuan-backend AuthController |
| GET /api/users/me | 获取当前用户信息 | sc-moyuan-backend UserController |
| PUT | /api/users/me | 更新当前用户信息 | sc-moyuan-backend UserController |
| PUT | /api/users/me/password | 修改密码 | sc-moyuan-backend UserController |
| GET | /api/users/me/posts | 获取当前用户帖子列表 | sc-moyuan-backend UserController |
| GET | /api/users/me/stats | 获取当前用户统计信息 | sc-moyuan-backend UserController |
| GET | /api/users/{id} | 获取用户信息 | sc-moyuan-backend UserController |
| GET | /api/users/{id}/profile | 获取用户主页信息（含诗人资料） | sc-moyuan-backend UserController |
| GET | /api/users/{id}/works | 获取用户作品列表 | sc-moyuan-backend UserController |

### 用户主页功能

用户主页展示以下信息：
- **用户基本信息**：头像、昵称、简介、性别、生日
- **诗人认证状态**：是否已认证、认证诗人资料
- **统计数据**：作品数、获赞数、被收藏数、粉丝数
- **作品列表**：用户发布的诗词和帖子

### 前端路由

| 路径 | 说明 |
|------|------|
| `/user/login` | 登录页 |
| `/user/register` | 注册页 |
| `/user/profile` | 个人中心（含诗人认证申请入口） |
| `/user/:id/homepage` | 用户主页（新增） |

---

## 9. 管理模块（新增）

### 功能说明

- 管理后台布局（左侧导航 + 右侧内容区域）
- 控制台（Dashboard）：系统统计数据展示、审核统计数据
- 用户管理：用户列表、状态管理、创建/编辑/删除用户
- 诗词管理：诗词列表、状态管理、创建/编辑/删除诗词
- **诗词审核**（新增）：审核用户发布的诗词，通过或拒绝
- **评论管理**（新增）：评论列表、审核评论、删除评论
- 分类管理：分类列表、创建/编辑/删除分类
- 朝代管理：朝代列表、创建/编辑/删除朝代
- 诗人管理：诗人列表、状态管理、创建/编辑/删除诗人
- 帖子管理：帖子列表、状态管理、创建/编辑/删除帖子
- **帖子审核**（新增）：审核帖子，通过或拒绝
- **诗人认证审核**（新增）：审核诗人认证申请，通过或拒绝
- **AI内容审核**（新增）：审核AI生成的内容，通过或拒绝后写入原表

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
| DELETE | /api/admin/forum-posts/{id} | 删除帖子 | backend AdminController |
| PUT | /api/admin/poems/{id}/audit | 审核诗词 | sc-moyuan-backend AdminController |
| GET | /api/admin/poet-profiles | 获取诗人认证申请列表 | sc-moyuan-backend AdminController |
| PUT | /api/admin/poet-profiles/{id}/verify | 审核诗人认证 | sc-moyuan-backend AdminController |
| PUT | /api/admin/forum-posts/{id}/audit | 审核帖子 | sc-moyuan-backend AdminController |
| GET | /api/admin/audit/stats | 获取审核统计数据 | sc-moyuan-backend AdminController |
| GET | /api/admin/comments | 获取评论列表 | sc-moyuan-backend AdminController |
| PUT | /api/admin/comments/{id}/audit | 审核评论 | sc-moyuan-backend AdminController |
| GET | /api/admin/ai-contents | 获取AI内容列表 | sc-moyuan-backend AdminController |
| PUT | /api/admin/ai-contents/{id}/approve | 审核通过AI内容 | sc-moyuan-backend AdminController |
| PUT | /api/admin/ai-contents/{id}/reject | 审核拒绝AI内容 | sc-moyuan-backend AdminController |
| GET | /api/admin/visitor-stats | 获取访客统计数据 | sc-moyuan-backend AdminController |
| GET | /api/admin/home-navigation | 获取首页导航列表（管理端） | sc-moyuan-backend AdminController |
| POST | /api/admin/home-navigation | 创建首页导航 | sc-moyuan-backend AdminController |
| PUT | /api/admin/home-navigation/{id} | 更新首页导航 | sc-moyuan-backend AdminController |
| DELETE | /api/admin/home-navigation/{id} | 删除首页导航 | sc-moyuan-backend AdminController |
| GET | /api/admin/poet-featured | 获取精选诗人列表（管理端） | sc-moyuan-backend AdminController |
| POST | /api/admin/poet-featured | 创建精选诗人 | sc-moyuan-backend AdminController |
| PUT | /api/admin/poet-featured/{id} | 更新精选诗人 | sc-moyuan-backend AdminController |
| DELETE | /api/admin/poet-featured/{id} | 删除精选诗人 | sc-moyuan-backend AdminController |
| GET | /api/admin/repairs | 获取报修列表（管理员） | sc-moyuan-backend AdminController |
| GET | /api/admin/repairs/{id} | 获取报修详情（管理员） | sc-moyuan-backend AdminController |
| PUT | /api/admin/repairs/{id}/status | 更新报修状态（管理员） | sc-moyuan-backend AdminController |
| PUT | /api/admin/repairs/{id}/assign | 分配处理人（管理员） | sc-moyuan-backend AdminController |
| POST | /api/admin/repairs/{id}/comments | 添加内部备注（管理员） | sc-moyuan-backend AdminController |
| GET | /api/admin/repairs/{id}/comments | 获取报修评论列表（管理员） | sc-moyuan-backend AdminController |
| GET | /api/admin/repairs/stats | 获取报修统计数据（管理员） | sc-moyuan-backend AdminController |
| GET | /api/admin/static-pages | 获取静态页面列表（管理端） | sc-moyuan-backend AdminController |
| GET | /api/admin/static-pages/{id} | 获取静态页面详情（管理端） | sc-moyuan-backend AdminController |
| PUT | /api/admin/static-pages/{id} | 更新静态页面内容（管理端） | sc-moyuan-backend AdminController |
| GET | /api/admin/poet-drafts | 获取草稿列表（管理员） | sc-moyuan-backend AdminController |
| GET | /api/admin/poet-drafts/{id} | 获取草稿详情（管理员） | sc-moyuan-backend AdminController |
| PUT | /api/admin/poet-drafts/{id}/review | 审核草稿（管理员） | sc-moyuan-backend AdminController |
| POST | /api/admin/poets/{id}/sync | 同步诗人数据 | sc-moyuan-backend AdminController |
| GET | /api/admin/cache/stats | 获取缓存统计信息 | sc-moyuan-backend AdminController |
| DELETE | /api/admin/cache/clear | 清除缓存 | sc-moyuan-backend AdminController |

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
| `/admin/comments` | 评论管理 |
| `/admin/poem-audit` | 诗词审核 |
| `/admin/poet-profiles` | 诗人认证 |
| `/admin/ai-contents` | AI内容审核 |
| `/admin/logs` | 操作日志 |
| `/admin/ai-models` | AI模型管理 |

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

## 10. 文件模块

### 功能说明

- 统一文件上传功能（支持图片上传，按类型分目录存储）
- 文件元数据管理（file_metadata表）
- AI生成图片记录管理（ai_image_record表）
- 图片预览和管理
- 文件存储和访问
- AI图片自动水印功能
- 缩略图生成

### 目录结构

```
uploads/
├── avatars/               # 用户头像
├── poems/                 # 诗词配图
├── user_poems/            # 用户自己发布的诗词配图
├── forum/                 # 论坛帖子图片
├── vision/                # 诗话视野文章配图
├── ai_generated/          # AI 生成的图片
├── config/                # 动态配置图片（轮播图、导航图标）
├── export/                # 导出文件
├── temp/                  # 临时文件
├── backup/                # 自动备份
├── audit/                 # 审核留证
├── watermark/             # 水印素材
└── cache/                 # 动态生成缓存（缩略图、WebP）
```

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| POST /api/files/upload | 上传文件（支持fileType、relatedId、relatedType参数） | sc-moyuan-backend FileController |
| DELETE /api/files/{fileKey} | 删除文件 | sc-moyuan-backend FileController |
| GET /api/files/{fileKey} | 获取文件信息 | sc-moyuan-backend FileController |

### 服务层

| 服务 | 说明 |
|------|------|
| FileStorageService | 统一文件服务接口，包含upload、delete、getUrl、generateThumb、getFileInfo方法 |
| FileStorageServiceImpl | 文件服务实现类，支持按类型分目录存储、MD5计算、图片宽高读取 |
| WatermarkUtil | 水印工具类，支持文字水印、图片水印、AI生成水印 |

### 前端组件

| 组件 | 说明 |
|------|------|
| ImageUpload.vue | 图片上传组件（支持预览、裁剪、fileType参数） |

### 配置说明

```yaml
file:
  upload-dir: ./uploads
  base-url: http://localhost:8085/uploads
  allowed-extensions: .jpg,.jpeg,.png,.gif,.webp,.bmp
  max-size: 10485760
  watermark:
    text: "AI生成"
    logo-path: ./uploads/watermark/logo.png
    opacity: 0.5
    position: 3  # 0=左上 1=右上 2=左下 3=右下 4=居中
```

---

## 11. 浏览历史模块

### 功能说明

- 记录用户浏览历史
- 查看浏览历史列表
- 清空浏览历史

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| POST /api/history | 添加浏览历史 | sc-moyuan-backend HistoryController |
| GET /api/history | 获取浏览历史 | sc-moyuan-backend HistoryController |
| DELETE /api/history | 清空浏览历史 | sc-moyuan-backend HistoryController |

---

## 12. 首页导航模块

### 功能说明

- 首页导航菜单管理（作品/流派/朝代三种类型）
- 导航数据动态配置
- 支持图片和链接配置

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/home-navigation | 获取首页导航列表 | sc-moyuan-backend HomeNavigationController |

### 前端组件

| 组件 | 说明 |
|------|------|
| HomeNavigation.vue | 首页导航组件（展示作品/流派/朝代导航） |
| admin/home-navigation.vue | 首页导航管理页面（后台管理） |

---

## 13. 精选诗人模块

### 功能说明

- 精选诗人展示（首页卡片展示）
- 精选诗人详情查看
- 精选诗人管理（后台管理）

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/poet-featured/random | 随机获取精选诗人 | sc-moyuan-backend PoetFeaturedController |
| GET /api/poet-featured/list | 获取精选诗人列表 | sc-moyuan-backend PoetFeaturedController |
| GET /api/poet-featured/{id} | 获取精选诗人详情 | sc-moyuan-backend PoetFeaturedController |

### 前端组件

| 组件 | 说明 |
|------|------|
| PoetCard.vue | 精选诗人卡片组件 |
| admin/poet-featured.vue | 精选诗人管理页面（后台管理） |

---

## 14. 诗话视野模块（新增）

### 功能说明

- 首页「诗话视野」板块动态展示文章列表（替代原有硬编码内容）
- 文章详情页展示（标题、摘要、正文、作者、分类、浏览量、点赞）
- 支持文章点赞
- 独立的文章列表页（分页浏览）
- 文章分类筛选

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/vision/articles | 文章列表 | sc-moyuan-backend VisionArticleController |
| GET /api/vision/articles/featured | 推荐文章列表 | sc-moyuan-backend VisionArticleController |
| GET /api/vision/articles/{id} | 文章详情 | sc-moyuan-backend VisionArticleController |
| POST /api/vision/articles/{id}/like | 点赞文章 | sc-moyuan-backend VisionArticleController |

### 前端路由

| 路径 | 说明 |
|------|------|
| `/vision` | 诗话视野列表页 |
| `/vision/:id` | 文章详情页 |

### 前端组件

| 组件 | 说明 |
|------|------|
| vision/list.vue | 文章列表页（分页、卡片式展示） |
| vision/detail.vue | 文章详情页（标题、正文、封面、点赞） |
| home/index.vue 中的 z_right 区域 | 首页诗话视野板块（动态加载文章列表） |

---

## 15. 诗词评分模块（新增）

### 功能说明

- 用户评分：用户对诗词进行评分（1.0-5.0分）
- AI评分：请求AI对诗词进行评分和分析
- 评分维度：支持多维度评分（格律、意境、用词、情感、创新）
- 评分统计：诗词平均评分和评分数量
- 评分详情：展示用户评分和AI评分详情
- 重新评价：用户已评分后可点击"重新评价"修改评分

### 评分类型

| 类型 | 说明 |
|------|------|
| 用户评分 | 用户手动评分，可附带评语 |
| AI评分 | AI自动评分，附带AI分析内容 |

### 评分维度

| 维度 | 说明 |
|------|------|
| 格律 | 诗词的格律规范性 |
| 意境 | 诗词的意境表达 |
| 用词 | 诗词的用词精准度 |
| 情感 | 诗词的情感表达 |
| 创新 | 诗词的创新程度 |

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/poems/{id}/ratings | 获取诗词评分详情 | sc-moyuan-backend PoemRatingController |
| POST /api/poems/{id}/ratings | 用户评分 | sc-moyuan-backend PoemRatingController |
| POST /api/poems/{id}/ratings/ai | 请求AI评分 | sc-moyuan-backend PoemRatingController |
| GET /api/poems/{id}/ratings/ai | 获取AI评分 | sc-moyuan-backend PoemRatingController |
| GET /api/poems/{id}/ratings/me | 获取当前用户评分 | sc-moyuan-backend PoemRatingController |

### 前端组件

| 组件 | 说明 |
|------|------|
| poem/detail.vue | 诗词详情页（集成评分功能） |

---

## 17. 韵律查询模块（新增）

### 功能说明

- 按汉字查询韵脚信息（所属韵部、声调类型）
- 按韵部查询同韵字列表
- 支持平水韵标准
- 用于诗词创作时的韵律参考

### 数据来源

- 平水韵韵脚数据（rhyme 表）
- 包含上平15韵、下平15韵及部分仄声韵
- 种子数据由 init.sql 初始化

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/rhyme/query | 按汉字查询韵脚 | sc-moyuan-backend RhymeController |
| GET /api/rhyme/group | 按韵部查询同韵字 | sc-moyuan-backend RhymeController |

### 使用场景

- 诗词创作时检查韵脚是否合规
- 查找同韵字用于押韵
- 学习平水韵知识

---

## 18. AI内容填充模块（新增）

### 功能说明

- 对诗词和诗人表中暂未收录的字段，提供AI填充能力
- AI生成的内容不直接写入原表，存入审核表等待管理员审核
- 审核通过后内容正式写入原表
- 防重复生成机制：同一目标的同一字段如有待审核记录则不重复调用AI

### 支持填充的字段

**诗词字段**：
| 字段名 | 说明 |
|--------|------|
| translation | 译文 |
| appreciation | 赏析 |
| background | 创作背景 |

**诗人字段**：
| 字段名 | 说明 |
|--------|------|
| biography | 生平简介 |
| life_story | 人物生平 |
| influence | 主要影响 |
| evaluation | 历史评价 |
| anecdotes | 轶事典故 |

### 审核流程

```
用户点击"AI填充" → AI生成内容 → 存入审核表(status=0)
                                        ↓
管理员进入后台 → AI内容审核页面 → 查看待审核列表
                                        ↓
                          审核通过 → 写入原表(poem/poet)
                          审核拒绝 → 标记已拒绝(status=2)
```

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| POST /api/ai/fill-content | 触发AI填充 | sc-moyuan-backend AiContentController |
| GET /api/ai/fill-status/{targetType}/{targetId} | 查询AI填充状态 | sc-moyuan-backend AiContentController |
| GET /api/admin/ai-contents | 获取AI内容列表（管理员） | sc-moyuan-backend AdminController |
| PUT /api/admin/ai-contents/{id}/approve | 审核通过（管理员） | sc-moyuan-backend AdminController |
| PUT /api/admin/ai-contents/{id}/reject | 审核拒绝（管理员） | sc-moyuan-backend AdminController |

### 数据库表

- `ai_generated_content`：AI生成内容审核表

### 前端组件

| 组件 | 说明 |
|------|------|
| poem/detail.vue | 诗词详情页AI填充按钮 |
| poet/detail.vue | 诗人详情页AI填充按钮 |
| admin/ai-contents.vue | 后台AI内容审核页面 |

---

## 19. 报修模块（新增）

### 功能说明

- 用户提交报修工单（标题、描述、分类、优先级）
- 查看我的报修列表和详情
- 添加反馈评论
- 关闭工单
- 提交满意度评价（1-5星 + 评语）
- 管理端：工单管理、分配处理人、状态流转、内部备注、统计

### 工单状态流转

```
待处理(0) → 处理中(1) → 已解决(2) → 已关闭(3)
```

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| POST /api/repair/orders | 提交报修工单 | sc-moyuan-backend RepairController |
| GET /api/repair/orders | 获取我的报修列表 | sc-moyuan-backend RepairController |
| GET /api/repair/orders/{id} | 获取报修详情 | sc-moyuan-backend RepairController |
| POST /api/repair/orders/{id}/comments | 添加反馈评论 | sc-moyuan-backend RepairController |
| PUT /api/repair/orders/{id}/close | 关闭工单 | sc-moyuan-backend RepairController |
| PUT /api/repair/orders/{id}/satisfaction | 提交满意度评价 | sc-moyuan-backend RepairController |
| GET /api/repair/orders/{id}/comments | 获取评论列表 | sc-moyuan-backend RepairController |
| GET /api/admin/repairs | 获取报修列表（管理员） | sc-moyuan-backend AdminController |
| GET /api/admin/repairs/{id} | 获取报修详情（管理员） | sc-moyuan-backend AdminController |
| PUT /api/admin/repairs/{id}/status | 更新报修状态（管理员） | sc-moyuan-backend AdminController |
| PUT /api/admin/repairs/{id}/assign | 分配处理人（管理员） | sc-moyuan-backend AdminController |
| POST /api/admin/repairs/{id}/comments | 添加内部备注（管理员） | sc-moyuan-backend AdminController |
| GET /api/admin/repairs/{id}/comments | 获取报修评论列表（管理员） | sc-moyuan-backend AdminController |
| GET /api/admin/repairs/stats | 获取报修统计数据（管理员） | sc-moyuan-backend AdminController |

### 前端路由

| 路径 | 说明 |
|------|------|
| `/repair` | 我的报修列表 |
| `/repair/create` | 提交报修 |
| `/repair/:id` | 报修详情 |
| `/admin/repairs` | 管理端报修管理 |

### 数据库表

- `repair_order`：报修工单表
- `repair_comment`：报修评论表

---

## 20. 静态页面模块（新增）

### 功能说明

- 前端展示静态页面内容（使用条款、隐私政策、关于我们等）
- 管理端编辑和管理静态页面
- 按 pageKey 动态获取页面内容

### 已知页面

| pageKey | 说明 |
|---------|------|
| terms | 使用条款 |
| privacy | 隐私政策 |
| contact | 联系我们 |

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/static-pages/{pageKey} | 获取静态页面内容 | sc-moyuan-backend StaticPageController |
| GET /api/admin/static-pages | 获取静态页面列表（管理端） | sc-moyuan-backend StaticPageController |
| GET /api/admin/static-pages/{id} | 获取静态页面详情（管理端） | sc-moyuan-backend StaticPageController |
| PUT /api/admin/static-pages/{id} | 更新静态页面内容（管理端） | sc-moyuan-backend StaticPageController |

### 前端路由

| 路径 | 说明 |
|------|------|
| `/static/:type` | 静态页面展示 |
| `/admin/static-pages` | 管理端静态页面管理 |

### 数据库表

- `static_page`：静态页面表

---

## 21. 诗人草稿模块（新增）

### 功能说明

- 管理员编辑诗人资料后保存为草稿
- 草稿审核流程（待审核→已通过/已拒绝）
- 审核通过后内容发布到诗人表

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| POST /api/poet-drafts | 保存诗人资料草稿 | sc-moyuan-backend PoetDraftController |
| GET /api/admin/poet-drafts | 获取草稿列表（管理员） | sc-moyuan-backend PoetDraftController |
| GET /api/admin/poet-drafts/{id} | 获取草稿详情（管理员） | sc-moyuan-backend PoetDraftController |
| PUT /api/admin/poet-drafts/{id}/review | 审核草稿（管理员） | sc-moyuan-backend PoetDraftController |

### 数据库表

- `poet_draft`：诗人内容草稿表

---

## 22. 诗词内容缓存模块（新增）

### 功能说明

- 缓存外部API获取的诗词内容（译文、注释、赏析等）
- 避免重复调用外部API
- 支持按诗词标题+诗人+内容类型查询

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/poem-content/cache | 获取缓存的诗词内容 | sc-moyuan-backend PoemContentController |
| POST /api/poem-content/cache | 保存诗词内容缓存 | sc-moyuan-backend PoemContentController |

### 数据库表

- `poem_content_cache`：诗词内容缓存表

---

## 23. 联系我们模块（新增）

### 功能说明

- 用户反馈表单页面
- 支持填写姓名、邮箱、类型（功能建议/意见反馈/商务合作/其他）、标题、内容

### 前端路由

| 路径 | 说明 |
|------|------|
| `/contact` | 联系我们页面 |

---

## 24. 诗云模块（新增）

### 功能说明

- Three.js 3D 星空可视化，展示中国古诗词数据
- 诗人星系：每位诗人是一颗发光的星，按朝代分层排列
- 诗星光点云：每首诗是一个微小光点，围绕在所属诗人星周围
- 赠诗连线：同朝代诗人之间的关系网络连线
- 朝代筛选：按朝代过滤诗人和诗词
- 诗人信息面板：点击诗人星显示诗人信息和代表作品
- 相机漫游：支持鼠标拖动旋转、滚轮缩放、右键平移
- 诗体分类着色：五绝/七绝/五律/七律/词/现代诗不同颜色

### 技术方案

- Three.js 渲染引擎（WebGL）
- OrbitControls 相机控制
- 自定义 Shader 实现发光效果
- Points 点云渲染诗词
- Raycaster 射线检测点击和hover
- 响应式布局适配移动端

### 前端组件

| 组件 | 说明 |
|------|------|
| poetry-cloud/index.vue | 诗云主页面（Three.js 3D 渲染、数据加载、交互控制） |

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/dynasties | 获取朝代列表 | backend DynastyController |
| GET /api/poets | 获取诗人列表（用于3D星系） | sc-moyuan-backend PoetController |
| GET /api/poems | 获取诗词列表（用于光点云） | backend PoemController |

### 前端路由

| 路径 | 说明 |
|------|------|
| `/poetry-cloud` | 诗云星空页面 |

### 数据可视化

1. **诗人星系**：诗人按朝代分为12个同心壳层，壳层半径从25到223递增
2. **诗星光点**：每首诗是一个光点，围绕在所属诗人星周围（半径1.5-4）
3. **颜色映射**：
   - 朝代颜色：先秦（暗金）→ 唐（金色）→ 宋（橙色）→ 清（铜色）
   - 诗体颜色：五绝（蓝）/ 七绝（绿）/ 五律（紫）/ 七律（橙）/ 词（粉）
4. **交互效果**：
   - hover 显示诗人名字标签
   - 点击飞向诗人星并显示信息面板
   - 选中诗人时高亮其关联连线

---

**文档版本**：v3.5
**最后更新**：2026-07-28（搜索模块补充复合搜索策略描述）
**维护人员**：墨渊开发团队