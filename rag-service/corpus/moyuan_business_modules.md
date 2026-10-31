# 业务模块说明

## 概述

本文档描述"古今诗话——墨渊"项目的 26 大业务模块功能说明。

## 模块总览

| 序号 | 模块 | 说明 | 状态 |
|------|------|------|------|
| 1 | 首页模块 | 网站入口，展示精选内容 | 前端已实现（连接后端 API） |
| 2 | 诗词模块 | 诗词浏览、搜索、赏析、用户发布 | 前端+后端已实现 |
| 3 | 诗人模块 | 诗人介绍、作品集、智能推荐 | 前端+后端已实现 |
| 4 | 诗人认证模块 | 诗人认证申请、审核、资料管理 | 前端+后端已实现 |
| 5 | 论坛模块 | 用户交流、发帖评论 | 前端+后端已实现 |
| 6 | 交流广场模块 | 轻量级交流空间，快速发帖和互动 | 前端+后端已实现 |
| 7 | AI模块 | AI 辅助创作、看图写诗、智能分析、AI对联、藏头诗、平仄格律校验、古籍OCR识文、AI内容填充与审核、AI模型配置管理 | 前端+后端已实现 |
| 8 | 搜索模块 | 全文搜索、复合筛选、拼音匹配、搜索建议/历史 | 前端+后端已实现 |
| 9 | 用户模块 | 注册登录、个人中心、用户主页 | 前端+后端已实现 |
| 10 | 管理模块 | 后台管理、内容管理、数据统计、内容审核 | 前端+后端已实现 |
| 11 | 文件模块 | 文件上传、图片管理、AI水印 | 前端+后端已实现 |
| 12 | 浏览历史模块 | 浏览记录、历史查看 | 前端+后端已实现 |
| 13 | 首页导航模块 | 首页导航菜单管理（作品/流派/朝代） | 前端+后端已实现 |
| 14 | 精选诗人模块 | 精选诗人展示和管理 | 前端+后端已实现 |
| 15 | 诗话视野模块 | 诗词文化文章浏览、详情展示 | 前端+后端已实现 |
| 16 | 诗词评分模块 | 诗词用户评分和AI评分 | 前端+后端已实现 |
| 17 | 韵律查询模块 | 平水韵韵脚查询、同韵字查询 | 前端+后端已实现 |
| 18 | 报修模块 | 用户报修工单提交、进度跟踪、满意度评价 | 前端+后端已实现 |
| 19 | 静态页面模块 | 网站静态页面管理（使用条款、隐私政策等） | 前端+后端已实现 |
| 20 | 诗人草稿模块 | 诗人资料编辑草稿、审核流程 | 前端+后端已实现 |
| 21 | 诗词内容缓存模块 | 诗词外部获取内容的缓存管理 | 前端+后端已实现 |
| 22 | 联系我们模块 | 用户反馈表单页面 | 前端已实现 |
| 23 | 诗云模块 | Three.js 3D 星空可视化，诗人—诗作两级层级星图 + 配色实验室 | 前端已实现 |
| 24 | 游戏模块 | 飞花令 / 诗词接龙对局（模式/难度、回合计时、三级真实性校验、游客模式） | 前端+后端已实现 |
| 25 | AI 对话页「诗宝阁」 | 角色化统一对话入口：6 角色 × 12 能力，覆盖命题作诗/集句/藏头/润色/平仄校验/韵脚查询/对对联/意象典故/诗话点评/飞花令/接龙/群贤论诗；其中飞花令为设令字后逐句判定（含字/判重/真实性）的对局流程 | 阶段一前端已完成（非流式）、阶段二后端 SSE 已完成（前端流式待接入） |
| 26 | 全局 AI 小窗与系统知识库 | 全站悬浮 AI 问答入口：诗词助理 / 网页用法助理双助理，接入 system_knowledge 知识库 | 前端+后端已实现 |

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
- AI功能区域「诗境 AI」（看图写诗/智能分析/诗海拾贝；韵脚查询/对对联/古籍识文/藏头诗/平仄校验/飞花令等工具卡，工具卡自动横向滑动、悬浮暂停；展开面板提供「前往完整页面」入口，统一跳「诗宝阁」并预选角色与能力，飞花令/接龙跳对应游戏页，古籍识文暂无完整页）
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
| 飞花令/诗词接龙卡片 | 首页 AI 小工具区两张入口卡片（游客展开内嵌小窗口 GameMiniPanel，登录用户跳转游戏页） |
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
- **外文诗词页签**（新增）：`/poem?tab=foreign` 展示已入库的 20 首英文名篇（`poem_type='foreign'`）；先读库、库空才实时兜底 PoetryDB（兜底结果不落库），加载超过 5 秒在过场动画上提示「检索耗时较久，请耐心等待…」

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/poems | 诗词列表 | backend PoemController |
| GET /api/poems/modern/page | 分页获取现代诗词（支持筛选和排序） | sc-moyuan-backend PoemController |
| GET /api/external-poetry/foreign | 外文诗词列表（先读库 `poem_type='foreign'`，库空实时兜底且不落库） | sc-moyuan-backend ExternalPoetryController |
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

当用户访问诗人详情页时，后端会检查本地数据库中的诗人信息是否完整。如果 `biography`、`life_story`、`influence`、`anecdotes`、`avatar` 任一字段为空，则自动调用接口盒子（apihz）API 获取并补齐数据；头像按优先级：接口 `image` 字段 → `PoetDefaultAvatar` 本地预设头像兜底。

> 该运行时流程（含字段映射、头像分配完整规则与头像库）已纳入 **[系统工作流文档 system-workflow.md](system-workflow.md)**，此处不再重复维护，请以工作流文档为准。

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

## 4. 诗人认证模块

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

## 5. 论坛模块

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

## 6. 交流广场模块

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

## 7. AI 模块

### 功能说明

- AI诗词问答（支持多模型切换；调用优先走 `ai_module_config` 模块绑定，未绑定某模块时回退默认模型。可用提供商：智谱AI、DeepSeek、Kimi、千问、小米MiMo、OpenRouter；NVIDIA NIM 已全部禁用/下线）
- 看图写诗（上传图片，AI生成古诗词，支持视觉模型如 GLM-4V-Flash、DeepSeek-Flash）
- 智能分析（输入诗句进行深度解析）
- AI对对联（输入上联，AI生成下联，遵循平仄规则）
- **藏头诗**（输入 2-8 个藏头字并选择五言/七言，AI 生成藏头诗，每句首字依次对应藏头字；复用 `POST /api/ai/chat`，moduleCode=chat，不新增后端接口）
- **平仄格律校验**（粘贴一首绝句/律诗，AI 逐句标注平仄（平声绿底、仄声红底）、判定是否合律、识别所属平水韵韵部并给出总体评价；AI 返回严格 JSON，前端解析失败时回退展示原始文本；复用 `POST /api/ai/chat`，moduleCode=chat，不新增后端接口）
- **AI 对话页「诗宝阁」**（`/ai-chat`）：角色化统一对话入口，6 角色（诗仙·创作 / 诗话·鉴赏 / 格律·校正 / 诗友·行令 / 群贤·论诗 / 顾问·问答）× 12 能力（命题作诗 / 集句成诗 / 藏头诗 / 润色入律 / 平仄校验 / 韵脚查询 / 对对联 / 意象典故 / 诗话点评 / 飞花令 / 诗词接龙 / 群贤论诗）；阶段一复用 `POST /api/ai/chat`（moduleCode=chat，非流式），阶段二后端已新增 `POST /api/ai/chat/stream`（SSE 流式，见 6.1 接口契约设计），会话按角色分存 localStorage，详见第 25 章
- **飞花令 / 诗词接龙**：原为纯前端小工具（复用 `POST /api/ai/chat`），现已升级为**独立游戏模块**（见第 24 章「游戏模块」），拥有专属后端接口 `/api/game/feihua`、`/api/game/chain` 与 `ai_module_config` 游戏模块绑定；首页 AI 区块保留「飞花令」「诗词接龙」两张卡片入口。
- **古籍OCR识文**（上传古诗词图片/书法作品/碑帖，AI精准识别文字内容，保留原文结构，标注印章题款）
- AI模型配置管理（后台管理，支持CRUD、启用/禁用、默认模型设置、连接测试）
- **AI内容填充与审核**：对诗词/诗人缺失字段由 AI 生成候选内容，存入审核表，管理员审核通过后写回原表
- **诗词AI助手（poetry_chat）**：寻章摘句页面的AI功能模块，包括译文注释、名人点评、创作背景、历史文化背景、AI逐句解析、AI诗词助手对话
  - **译文注释**：逐句翻译，关键词语注释
  - **名人点评**：历代名家点评（引用《诗品》《沧浪诗话》《人间词话》等）、文学史地位
  - **创作背景**：诗人生平、创作缘由、历史背景
  - **AI逐句解析**：点击诗句展开解释，支持多句同时展开，内存缓存不写库，可关闭
  - **弹窗交互**：三个功能按钮以向左展开的弹窗形式呈现，覆盖在诗词内容上方，不挤压布局
  - **搜索式加载**：AI生成过程中以搜索动画替代旋转加载图标

### 技术方案

- 调用大语言模型 API（智谱AI、DeepSeek、Kimi、千问、小米MiMo、OpenRouter；NVIDIA NIM 已下线）
- 模块绑定：`ai_module_config`（chat / poet_chat / poetry_chat / write_poem / analyze / couplet，以及游戏模块的 feihua_gen_ancient / feihua_gen_normal / feihua_gen_super / feihua_verify / feihua_topic / chain_generate）→ `ai_model`，由 `AiModelRegistry` 按模块解析模型；统一适配层 `OpenAiCompatibleAdapter` / `NvidiaAdapter`
- 视觉能力：视觉模型用于看图写诗与 OCR 识别；DeepSeek 视觉请求需关闭思考模式（body 顶层 `thinking.type=disabled`）
- OCR 识别复用视觉模型基础设施，通过专用提示词引导模型精准识别古籍文字
- AI模型配置存入数据库（ai_model 表），支持动态管理；api_key 部分为占位符，真实密钥从 `secrets/application-secrets.yml` 运行时注入
- 前端首页集成AI问答组件（右下角悬浮按钮）
- 前端首页"展风拓潮"模块集成看图写诗、智能分析、对联、藏头诗、平仄校验和OCR功能；「飞花令」「诗词接龙」为两张卡片入口（见第 24 章「游戏模块」）

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| POST /api/ai/chat | AI问答（非流式） | sc-moyuan-backend AiController |
| POST /api/ai/chat/stream | AI流式问答（SSE，返回 `text/event-stream`） | sc-moyuan-backend AiController |
| POST /api/ai/write-poem | 看图写诗 | sc-moyuan-backend AiController |
| POST /api/ai/analyze | 智能分析 | sc-moyuan-backend AiController |
| POST /api/ai/ocr | 图片OCR识别（古籍/书法/碑帖） | sc-moyuan-backend AiController |
| POST /api/ai/couplet | AI对对联 | sc-moyuan-backend AiController |
| GET /api/ai/config/{moduleCode} | 获取模块AI配置（提示词模板、回答风格等） | sc-moyuan-backend AiController |
| POST /api/ai/preview | AI生成预览（不写入数据库） | sc-moyuan-backend AiContentController |
| POST /api/ai/submit-review | 提交AI内容到审核表 | sc-moyuan-backend AiContentController |
| POST /api/ai/fill-content | 触发AI填充（直接生成并存库） | sc-moyuan-backend AiContentController |
| GET /api/ai/fill-status/{targetType}/{targetId} | 查询某目标的AI填充状态 | sc-moyuan-backend AiContentController |

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

### AI 内容填充与审核

- 对诗词和诗人表中暂未收录的字段，提供 AI 填充能力
- AI 生成的内容不直接写入原表，存入审核表等待管理员审核，审核通过后正式写入原表
- 防重复生成机制：同一目标的同一字段如有待审核记录则不重复调用 AI
- 支持字段：
  - 诗词：`translation`（译文）、`appreciation`（赏析）、`background`（创作背景）
  - 诗人：`biography`（生平简介）、`life_story`（人物生平）、`influence`（主要影响）、`evaluation`（历史评价）、`anecdotes`（轶事典故）
- 数据库表：`ai_generated_content`
- 审核流程：

```
用户点击"AI填充" → AI生成内容 → 存入审核表(status=0)
                                        ↓
管理员进入后台 → AI内容审核页面 → 查看待审核列表
                                        ↓
                          审核通过 → 写入原表(poem/poet)
                          审核拒绝 → 标记已拒绝(status=2)
```

### 前端组件

| 组件 | 说明 |
|------|------|
| AI问答对话框 | 首页右下角悬浮按钮，点击打开AI问答对话框 |
| 展风拓潮模块 | 首页"展风拓潮"区域，包含看图写诗、智能分析、藏头诗、平仄校验和古籍OCR识文功能；另有「飞花令」「诗词接龙」卡片入口 |
| AI模型管理页面 | 管理后台 `admin/ai-models`，支持模型CRUD和配置管理 |
| poem/detail.vue、poet/detail.vue | 诗词/诗人详情页 AI 填充按钮 |
| admin/ai-contents.vue | 后台 AI 内容审核页面 |

---

## 8. 搜索模块

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
- **搜索引擎**：MySQL 8 ngram 中文全文检索（`/api/search/smart` 关键词默认走 `MATCH(title,content) AGAINST(...)`，`search.fulltext-enabled` 默认开启，可经 `SEARCH_FULLTEXT_ENABLED` 关闭回退 LIKE）；Elasticsearch 属后期可选优化
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
| GET /api/search/poems/external/detail | 获取外部古诗词详情 | sc-moyuan-backend SearchController |
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
  "code": "200",
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

1. **协同过滤算法（ItemCF）**
   - 构建用户-内容评分矩阵（收藏5分、点赞3分、浏览1分；诗词侧同一用户对同一作品的多行为取最大值合并，不累加）
   - 使用余弦相似度计算物品（诗人/诗词）相似性
   - 每个已交互物品取最相似的 Top-50 物品，按用户行为权重加权汇总，推荐当前用户未交互的内容

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

5. **四组模型策略（离线制品查表）**
   - era（古今）、genre（体裁）、fusion（融合）、global（全局）四组离线 ItemCF 结果导入 `recommendation_cache`，按用户 × 策略 × Top20 查表
   - 服务端过滤已交互物品与非古典/下架作品，不足 topK 时按 rank 回填，仍不足用热门古典补齐；缓存未命中或版本下线时回落规则推荐
   - 模型版本记录在 `recommendation_model_version`，后台可查看离线指标与上下线状态

6. **后台实时看板**
   - 控制台推荐指标屏展示缓存覆盖（行数/用户/作品/策略）、古典池规模、四个上线模型的版本与 NDCG@10
   - 展示近 7 天策略切换趋势、累计切换去向、来源页面分布与最近 10 条切换事件，30 秒轮询 `/api/recommend/stats`
   - 无切换事件时显示空态提示，不填充假数据

### 前端组件

| 组件 | 说明 |
|------|------|
| poem/list.vue - 筛选区域 | 朝代/分类/诗人/排序筛选器，支持复合搜索 |
| home/index.vue - 历史的印痕模块 | 首页搜索框、搜索结果展示、为您推荐标签页 |
| 搜索结果卡片 | 显示诗词信息、来源标签、推荐理由 |
| components/admin/RecommendationScreen.vue | 后台控制台推荐指标屏：离线四组指标 + 实时缓存/切换看板 |

### 前端路由

| 路径 | 说明 |
|------|------|
| `/poem` | 诗词列表页（含复合筛选搜索） |
| `/` (首页) | 历史的印痕模块内嵌搜索功能 |

> 说明：项目未设置独立的 `/search` 结果页路由，全局搜索能力内嵌在首页与诗词列表页中通过 `/api/search/smart` 实现。

---

## 9. 用户模块

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
| PUT /api/users/me | 更新当前用户信息 | sc-moyuan-backend UserController |
| PUT /api/users/me/password | 修改密码 | sc-moyuan-backend UserController |
| GET /api/users/me/posts | 获取当前用户帖子列表 | sc-moyuan-backend UserController |
| GET /api/users/me/stats | 获取当前用户统计信息 | sc-moyuan-backend UserController |
| GET /api/users/{id} | 获取用户信息 | sc-moyuan-backend UserController |
| GET /api/users/{id}/profile | 获取用户主页信息（含诗人资料） | sc-moyuan-backend UserController |
| GET /api/users/{id}/works | 获取用户作品列表 | sc-moyuan-backend UserController |

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

## 10. 管理模块

### 功能说明

- 管理后台布局（左侧导航 + 右侧内容区域）
- 控制台（Dashboard）：系统统计数据展示、审核统计数据
- 用户管理：用户列表、状态管理、创建/编辑/删除用户
- 诗词管理：诗词列表、状态管理、创建/编辑/删除诗词（支持标题/内容关键字搜索与按创建时间正序/倒序切换）
- **诗词审核**（新增）：审核用户发布的诗词（列表按 auditStatus 0-待审/1-通过/2-拒绝 筛选，通过或拒绝）
- **评论管理**（新增）：评论列表、审核评论、删除评论（status 1-正常/0-隐藏）
- 分类管理：分类列表、创建/编辑/删除分类
- 朝代管理：朝代列表、创建/编辑/删除朝代
- 诗人管理：诗人列表、状态管理、创建/编辑/删除诗人（支持姓名关键字搜索与按创建时间正序/倒序切换）
- 帖子管理：帖子列表、状态管理、创建/编辑/删除帖子
- **帖子审核**（新增）：审核帖子，通过或拒绝
- **诗人认证审核**（新增）：审核诗人认证申请，通过或拒绝
- **AI内容审核**（新增）：审核AI生成的内容，通过或拒绝后写入原表
- **诗话视野管理**（新增）：诗话视野文章的增删改查、上下架、推荐设置（含草稿）
- **文件管理**（新增）：文件列表与存储统计、单文件删除、孤儿文件清理
- **缓存管理**（新增）：一键清除诗人缓存 / 全部业务缓存

### 后台菜单结构

- 内容管理：诗词管理、分类管理、朝代管理、诗人管理、精选诗人管理、首页导航管理、**诗话视野管理**
- 审核管理：诗词审核、**诗人认证审核**、诗人内容审核、AI内容审核、**评论管理**
- 系统管理：操作日志、静态页面管理、**文件管理**、**缓存管理**

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/admin/dashboard/stats | 获取控制台统计数据 | backend AdminController |
| GET /api/admin/users | 获取用户列表 | backend AdminController |
| GET /api/admin/users/{id} | 获取用户详情 | backend AdminController |
| PUT /api/admin/users/{id}/status | 更新用户状态 | backend AdminController |
| DELETE /api/admin/users/{id} | 删除用户 | backend AdminController |
| GET /api/admin/poems | 获取诗词列表（支持 keyword、auditStatus 0-待审/1-通过/2-拒绝、sort asc-正序/desc-倒序（按创建时间，默认 desc）；含 poetName/dynastyName） | backend AdminController |
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
| GET /api/admin/poets | 获取诗人列表（支持 keyword 按姓名模糊匹配、sort asc-正序/desc-倒序（按创建时间，默认 desc）） | backend AdminController |
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
| GET | /api/admin/poet-featured | 获取精选诗人列表（管理端，支持 keyword 按姓名/朝代筛选） | sc-moyuan-backend AdminController |
| POST | /api/admin/poet-featured | 创建精选诗人 | sc-moyuan-backend AdminController |
| PUT | /api/admin/poet-featured/{id} | 更新精选诗人 | sc-moyuan-backend AdminController |
| DELETE | /api/admin/poet-featured/{id} | 删除精选诗人 | sc-moyuan-backend AdminController |
| PUT | /api/admin/poet-featured/{id}/show | 设置单个精选诗人首页展示状态 | sc-moyuan-backend AdminController |
| PUT | /api/admin/poet-featured/batch-show | 批量设置精选诗人首页展示状态 | sc-moyuan-backend AdminController |
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
| POST | /api/admin/cache/clear-poets | 清除诗人缓存 | sc-moyuan-backend AdminController |
| POST | /api/admin/cache/clear-all | 清除所有缓存 | sc-moyuan-backend AdminController |
| GET | /api/admin/files | 分页查询文件列表（fileType/keyword/status） | sc-moyuan-backend AdminFileController |
| GET | /api/admin/files/stats | 获取存储统计（总数/总占用/各类型/缺失数） | sc-moyuan-backend AdminFileController |
| POST | /api/admin/files/clean-orphans | 清理孤儿文件 | sc-moyuan-backend AdminFileController |
| DELETE | /api/admin/files/{id} | 删除单个文件（含物理文件） | sc-moyuan-backend AdminFileController |
| GET | /api/admin/vision-articles | 分页查询诗话视野文章列表（含草稿） | sc-moyuan-backend AdminVisionArticleController |
| GET | /api/admin/vision-articles/{id} | 获取诗话视野文章详情（任意状态） | sc-moyuan-backend AdminVisionArticleController |
| POST | /api/admin/vision-articles | 新建诗话视野文章 | sc-moyuan-backend AdminVisionArticleController |
| PUT | /api/admin/vision-articles/{id} | 更新诗话视野文章 | sc-moyuan-backend AdminVisionArticleController |
| PUT | /api/admin/vision-articles/{id}/status | 上下架诗话视野文章 | sc-moyuan-backend AdminVisionArticleController |
| PUT | /api/admin/vision-articles/{id}/featured | 设置诗话视野文章推荐状态 | sc-moyuan-backend AdminVisionArticleController |
| DELETE | /api/admin/vision-articles/{id} | 删除诗话视野文章（逻辑删除） | sc-moyuan-backend AdminVisionArticleController |

### 前端路由

| 路径 | 说明 |
|------|------|
| `/admin` | 管理后台（重定向到 Dashboard） |
| `/admin/dashboard` | 控制台 |
| `/admin/users` | 用户管理 |
| `/admin/poems` | 诗词管理 |
| `/admin/categories` | 分类管理 |
| `/admin/dynasties` | 朝代管理 |
| `/admin/poets` | 诗人管理 |
| `/admin/poet-featured` | 精选诗人管理 |
| `/admin/forum-posts` | 帖子管理 |
| `/admin/logs` | 操作日志 |
| `/admin/poet-suggestions` | 诗人内容审核（草稿 / 建议 两 Tab） |
| `/admin/poet-profiles` | 诗人认证审核 |
| `/admin/ai-contents` | AI内容审核 |
| `/admin/poem-audit` | 诗词审核 |
| `/admin/comments` | 评论管理 |
| `/admin/ai-models` | AI模型管理 |
| `/admin/home-navigation` | 首页导航管理 |
| `/admin/repairs` | 报修管理 |
| `/admin/static-pages` | 静态页面管理 |
| `/admin/vision-articles` | 诗话视野管理 |
| `/admin/files` | 文件管理 |
| `/admin/cache` | 缓存管理 |

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

## 11. 文件模块

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

## 12. 浏览历史模块

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

## 13. 首页导航模块

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

## 14. 精选诗人模块

### 功能说明

- 精选诗人展示（首页卡片展示，仅展示 `is_show=1` 的诗人）
- 精选诗人详情查看
- 精选诗人管理（后台管理，支持按姓名/朝代筛选、单个/批量设置首页展示开关）

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

## 15. 诗话视野模块

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

## 16. 诗词评分模块

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

## 17. 韵律查询模块

### 功能说明

- 按汉字查询韵脚信息（所属韵部、声调类型），命中库时返回该韵部的全部同韵字
- 按韵部查询同韵字列表
- 支持平水韵标准
- 用于诗词创作时的韵律参考
- 数据库优先 + AI 兜底：先查本地 rhyme 表，未收录的汉字或韵部调用大模型现查并落库，后续直接命中库

### 数据来源

- 平水韵韵脚数据（rhyme 表）
- 包含上平15韵、下平15韵及部分仄声韵
- 种子数据由 init.sql 初始化
- 未收录数据由大模型查询后动态扩充至 rhyme 表（依赖唯一键 `uk_rhyme_char_group_tone` 幂等插入）

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

## 18. 报修模块

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

## 19. 静态页面模块

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

## 20. 诗人草稿模块

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

## 21. 诗词内容缓存模块

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

## 22. 联系我们模块

### 功能说明

- 用户反馈表单页面
- 支持填写姓名、邮箱、类型（功能建议/意见反馈/商务合作/其他）、标题、内容

### 前端路由

| 路径 | 说明 |
|------|------|
| `/contact` | 联系我们页面 |

---

## 23. 诗云模块

### 功能说明

- Three.js 3D 星空可视化，以「诗人—诗作」两级层级星图展示中国古诗词数据
- 诗人星系：每位诗人是一颗发光的星（一级大节点），节点大小随作品数量自适应
- 诗作节点：每首诗是一个可交互的小球（二级小节点），按「诗人为中心、诗作环绕分布」的球面螺旋排布，并经松弛迭代强制拉开最小间距
- 诗人↔诗连线：每位诗人与其名下诗作之间用同色线段连接（LineSegments2 像素级线宽渲染）
- 赠诗连线：同朝代相邻诗人之间的贝塞尔曲线网络连线
- 诗人姓名标注：DOM 投影层跟随诗人星显示姓名，选中时变金色加粗
- 配色实验室：页面底部控制模块，可切换 7 套配色方案（琥珀星海 / 青玉 / 珊瑚 / 冰川蓝 / 梅子紫 / 苔绿 / 赤陶），实时调节光晕强度、连线可见度、节点尺寸、整体亮度，以及「显示诗人姓名」「诗人按朝代配色」开关
- 朝代筛选：按朝代过滤诗人和诗词
- 诗人信息面板：点击诗人星右侧滑出诗人信息与代表作品，并聚焦该诗人的诗作星簇
- 诗作详情浮层：点击诗作小点居中弹出诗词详情（标题/作者/朝代/诗体/正文），并保留「查看详情」按钮跳转 `/poem/{id}`
- 悬浮提示：鼠标悬停任意节点，跟随鼠标的提示框显示节点含义（诗人：姓名·朝代·作品数；诗作：诗名·作者·朝代·诗体）
- 相机漫游：鼠标拖动旋转、滚轮缩放、右键平移、点击星点相机飞向
- 虚空取诗：点击星空空白处随机浮现一首诗，并生成一颗蓝色「虚空新星」并飞向
- 中央提示：页面中央说明文字加载后 6 秒自动淡出，或点击画布立即淡出
- 回归原点：底部状态栏按钮，一键关闭全部面板并缓动飞回初始视角

### 技术方案

- Three.js 渲染引擎（WebGL，ACESFilmicToneMapping）
- OrbitControls 相机控制（阻尼旋转/缩放/平移；自动环绕，autoRotateSpeed 0.15）
- FogExp2 雾效 + 3200 颗背景星（ShaderMaterial，AdditiveBlending）
- 诗人/诗作节点均使用 SphereGeometry + MeshBasicMaterial（可交互 Mesh）
- 诗人↔诗连线、赠诗连线均使用 LineSegments2 + LineSegmentsGeometry + LineMaterial（像素级线宽，关闭 ACES 色调映射保证高对比；LineSegments2 每两点为独立线段，避免 Line2 串成长实线）
- 「诗人↔诗作」连线按诗人分组为多个 LineSegments2，支持逐诗人高亮
- 屏幕像素级命中检测：把节点投影到屏幕坐标，按像素距离命中最接近的节点（hover 最小命中半径 18px、click 22px），保证小节点可被鼠标悬浮/点击
- 悬浮提示使用跟随鼠标的 DOM 提示框（非 Sprite，保证可读性）
- 配色实验室：参数由 `reactive(viz)` 驱动，`applyVizStyles()` 统一重涂背景/雾效、诗人节点、诗作节点与两类连线；整体亮度经画布容器 CSS `filter: brightness()`
- 响应式布局适配移动端（≤1024px 两列；≤768px 实验室改为顶部抽屉）

### 前端组件

| 组件 | 说明 |
|------|------|
| poetry-cloud/index.vue | 诗云主页面（Three.js 3D 渲染、数据加载、交互控制、悬浮提示） |

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/dynasties | 获取朝代列表 | backend DynastyController |
| GET /api/poets | 获取诗人列表（pageSize 300，用于 3D 星系） | sc-moyuan-backend PoetController |
| GET /api/poems | 获取诗词列表（pageSize 1000，用于诗作节点与计数） | backend PoemController |

### 前端路由

| 路径 | 说明 |
|------|------|
| `/poetry-cloud` | 诗云星空页面（hideHeader） |

### 数据可视化

1. **诗人星系**：诗人分布为「诗人为中心」的层级结构，同层按黄金角（`Math.PI*(3-√5)`）球面螺旋排布。
2. **诗人节点尺寸**：随其名下诗作数量自适应，`starSize = 2.4 + log10(作品数+1) * 1.2`，带 1.8 倍光晕球。
3. **诗作节点**：独立小球，按「诗人为中心、诗作环绕分布」的球面螺旋生成初始坐标（半径 `r = max(starSize*1.8 + 4, 7) + √(gi/n)*9`，`gi` 为**组内序号**），再经 `relaxPoemPositions` 松弛迭代（8 轮）强制拉开最小间距（诗作间 `POEM_MIN_GAP=7`、诗作与诗人间 `POEM_POET_MIN_GAP=9`）。
4. **连线**：默认连线色为白色；诗人↔诗连线、赠诗连线均为像素级线宽（赠诗线 1.5 / 诗作线 2）；聚焦某诗人时其连线高亮、其余压暗。
5. **配色实验室**：默认方案「琥珀星海」，可切换 7 套方案；方案字段含诗人主色/外圈、诗作主色/外圈、两类连线色、背景色；光晕/连线/尺寸倍率换算基准为 glow/65、line/55、size/100。
6. **颜色映射**：
   - 朝代颜色：先秦（暗金）→ 唐（金色）→ 宋（橙色）→ 清（铜色）
   - 关闭「诗人按朝代配色」时，诗人节点统一使用当前方案主色
7. **交互效果**：
   - hover 节点高亮、光晕提亮，DOM 提示框显示节点含义
   - 点击诗人星 → 相机飞向 + 右侧信息面板 + 单诗人聚焦（其诗作放大高亮、其余淡出）
   - 点击诗作点 → 居中弹出诗词详情浮层（含「查看详情」跳转按钮）
   - 点击虚空 → 随机浮现一首诗并生成蓝色虚空新星飞向
   - 底部状态栏「配色实验室」开关 → 展开/收起可视化控制面板；「回归原点」→ 关闭全部面板并飞回初始视角

---

## 24. 游戏模块（飞花令 / 诗词接龙）

### 功能说明

- **飞花令**（`/game/feihua`）：按「令字」接句，用户每回合提交一句含令字的真实古诗词名句。
  - 三种模式：`ancient`（古制严格，令字须落在本回合第 N 个汉字位置）、`normal`（标准版，诗句包含令字即可）、`super`（超级飞花令，含 `char` 多关键字 / `topic` 主题两种子类型）
  - 三种难度：`easy`（达标 10 句即胜）、`medium`（达标 20 句即胜）、`hard`（AI 无句可接则用户胜，无固定目标条数）
  - 每题给出 AI 候选诗句（LLM 生成 + 后端按位次/判重/真实性二次过滤后下发）
- **诗词接龙**（`/game/chain`）：以 AI 起句的末字为首字接出下一句名句，AI 再接给用户，交替进行；本地诗词库随机取起句（不调用 LLM）；AI 接不上时用户胜，链长即有效句数。
  - **选择范围**：配置阶段可选「中国古典诗词（不限朝代，默认）」或具体朝代（取自 `dynasty` 表）；选定后 AI 起句仅在该朝代内取句，AI 接句提示词限定该朝代，且**玩家提交与 AI 接句的真实性校验均限定在该朝代内**（超出范围判为「诗句真实性校验不通过」并提示越界）。
- **回合计时（计时/监测/缓冲三子态，打字即暂停）**：单回合有效答题时长 30 秒。点击「开始挑战」后倒计时**直接开始**（`counting`，30 秒递减）；输入框一旦有变化即**暂停倒计时**并进入 `monitoring`（监测停笔 5 秒），仅当连续 5 秒无改动才恢复递减；30 秒耗尽后自动进入 `buffer`（5 秒缓冲窗口，期内有输入变动仍可提交）；缓冲耗尽仍无改动则由前端自动提交并判超时。有效答题时长上限 30 + 5 = 35 秒；**题面计时均为显示层，可被主动提交（作答/放弃）随时终止**。答错不重置计时，答对推进下一回合并重置计时。
- **超时判定（后端按上报有效时长）**：前端每次作答请求上报 `activeSeconds`（本回合已消耗的有效秒数），后端 `GameConstants.isTimeout(activeSeconds, wallElapsedMillis)` 优先据此判定（`>= 35` 即超时）；未上报时回退墙钟口径（服务端 `roundStartTime` 毫秒时间戳，阈值 30 秒 + 1 秒网络宽限）。
- **容错**：每回合初始 2 次重试，累计错误达 3 次判负；空输入不计错误、不消耗重试、不重置计时。
- **三级真实性校验链**：① 本地 `poem` 表 ngram 全文索引 `MATCH...AGAINST`（限定范围时附加 `dynasty_id` 过滤）→ ② 外部 APIHZ 复核（仅在**不限朝代**时可用，外部源无法确认朝代归属）→ ③ LLM 兜底真伪判断（`feihua_verify`，限定范围时提示词附朝代约束），全链路失败即判为「诗句真实性校验不通过」。
- **游客模式**：游客与新页面共用同一套接口，后端按登录态分层——**游客对局仅存 Redis（不落 MySQL），登录用户对局与有效诗句写入 `game_record` / `game_sentence`**；前端以 `GuestBadge` 展示游客标识，登录态用户对局可回查。
- **状态/判重后端权威**：对局运行时状态存 Redis（键 `game:{gameId}` 与 `game:{gameId}:sentences`，TTL 30 分钟，交互续期）；**超时判定由后端依前端上报的 `activeSeconds` 裁定**（见上文「超时判定」）。
- **输入方式**：支持文本与语音（`inputType`），语音仅标识，校验逻辑一致。

### 玩法规则速览

| 维度 | 取值 | 说明 |
|------|------|------|
| 玩法类型 | feihua / chain | 对局表 `game_type` 区分 |
| 飞花令模式 | ancient / normal / super | super 需 `superType`（char/topic） |
| 飞花令难度 | easy / medium / hard | 达标条数 easy=10、medium=20、hard=null（AI 枯竭即胜） |
| 接龙选择范围 | 不限（默认，中国古典诗词）/ 具体朝代 | 传 `dynastyId` 限定；起句、AI 接句、玩家校验均限定在该朝代内 |
| 对局结果 | win / lose / giveup | 写入 `game_record.game_result` |

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| POST /api/game/feihua/start | 创建飞花令对局 | sc-moyuan-backend GameFeihuaController |
| POST /api/game/feihua/round/begin | 开始/进入下一回合 | sc-moyuan-backend GameFeihuaController |
| POST /api/game/feihua/play | 提交本轮作答 | sc-moyuan-backend GameFeihuaController |
| POST /api/game/feihua/giveup | 主动放弃本局 | sc-moyuan-backend GameFeihuaController |
| GET /api/game/feihua/state | 恢复当前对局状态 | sc-moyuan-backend GameFeihuaController |
| POST /api/game/chain/start | 创建诗词接龙对局（返回 AI 起句） | sc-moyuan-backend GameChainController |
| POST /api/game/chain/round/begin | 开始/进入下一回合 | sc-moyuan-backend GameChainController |
| POST /api/game/chain/play | 提交本轮接句 | sc-moyuan-backend GameChainController |
| POST /api/game/chain/giveup | 主动放弃本局 | sc-moyuan-backend GameChainController |
| GET /api/game/chain/state | 恢复当前对局状态 | sc-moyuan-backend GameChainController |

### 前端组件

| 组件 | 说明 |
|------|------|
| views/game/feihua.vue | 飞花令对局页面 |
| views/game/chain.vue | 诗词接龙对局页面 |
| views/game/game-styles.scss | 游戏页面样式 |
| api/modules/game.ts | 游戏接口封装（含 game.test.ts） |
| stores/game.ts | 游戏对局状态管理 |
| components/game/GameStage.vue | 对局主舞台 |
| components/game/MarkTimer.vue | 回合计时器 |
| components/game/SealKeyword.vue | 令字/关键字印记 |
| components/game/PoemSlip.vue | 诗句签（展示句/作者/篇名） |
| components/game/ChainTrack.vue | 接龙链条轨迹 |
| components/game/VerdictBanner.vue | 裁判判定横幅 |
| components/game/ResultScroll.vue | 对局结果卷轴 |
| components/game/VoiceInputButton.vue | 语音输入按钮 |
| components/game/GuestBadge.vue | 游客标识 |
| components/game/GameMiniPanel.vue | 首页内嵌小面板（游客在首页直接开局） |

### 前端路由

| 路径 | 说明 |
|------|------|
| `/game/feihua` | 飞花令对局页 |
| `/game/chain` | 诗词接龙对局页 |

> 首页 AI 小工具区提供「飞花令」「诗词接龙」两张卡片：游客点击展开内嵌小窗口（`GameMiniPanel`，直接在首页开局），登录用户跳转到 `/game/feihua`、`/game/chain` 游戏页。

### 数据库表

- `game_record`：游戏对局主表（飞花令/诗词接龙共用，`game_type` 区分）
- `game_sentence`：对局有效诗句明细表（`uk_game_sentence` 唯一键兜底判重）

---

## 25. AI 对话页「诗宝阁」模块

### 功能说明

- **角色化统一对话入口**：把原先分散的创作类 / 鉴赏类 AI 小应用合并为单一对话页面——AI 赋角色（Persona）决定「谁在说话」，能力（Skill）决定「这次做什么任务」，两者正交组合，复用同一套对话流
- **6 个角色**：
  - `creator` 诗仙·创作（洒脱豪迈，默认能力：命题作诗 / 集句成诗 / 藏头诗）
  - `critic` 诗话·鉴赏（仿宋人诗话，默认能力：意象典故 / 诗话点评）
  - `judge` 格律·校正（精研平水韵，默认能力：平仄校验 / 润色入律）
  - `friend` 诗友·行令（默认能力：飞花令 / 诗词接龙）
  - `salon` 群贤·论诗（默认能力：群贤论诗）
  - `advisor` 顾问·问答（通识问答，无专属能力）
- **12 个能力**（Skill）：`compose` 命题作诗、`gather` 集句成诗、`acrostic` 藏头诗、`polish` 润色入律、`tone` 平仄校验、`rhyme` 韵脚查询、`couplet` 对对联、`imagery` 意象典故、`comment` 诗话点评、`feihua` 飞花令、`chain` 诗词接龙、`salon` 群贤论诗
- **三段式布局**：左「角色栏」+ 中「对话流」+ 底「能力快捷 chips + 输入框」
- **会话记忆**：按 `roleId` 分存 `localStorage`（键 `moyuan_ai_chat_${roleId}`），切换角色各自恢复；单角色最多保留 40 条（约 20 轮），单条内容上限 4000 字
- **空态引导**：进入页面 / 新建会话后，按角色给出引导语 + 推荐能力 chip
- **结果渲染**：普通能力渲染为文本气泡；藏头诗高亮每句首字（藏头字）；平仄校验解析 AI 返回的严格 JSON，渲染为平仄标签卡（平底/仄底、押韵标记、逐句简评、总评），解析失败回退展示原始文本
- **消息头像**：对话流每条消息两侧带圆形头像——AI 侧统一使用墨渊可爱卡通头像（本地静态资源 `/img/ai_chat_avatar.jpg`），用户侧使用本人头像（未登录 / 无头像时回退金色首字母）；用户气泡靠右、头像居其右，AI 气泡靠左、头像居其左，加载态气泡同样带 AI 头像

### 技术方案

- 阶段一为**纯前端 MVP**，不新增后端接口、不改数据库表，复用现有非流式接口 `POST /api/ai/chat`
- `moduleCode` 必传 `'chat'`（已绑定 DeepSeek）；否则回退智谱默认模型，会因 key 余额不足返回 `429 code 1113`
- 阶段一后端恒前置自身 `CHAT_SYSTEM_PROMPT`，故角色提示词以「角色设定」前缀注入 user 消息（**非**真正的 system role）；真正的 system role + 多轮上下文属阶段二
- 加载反馈遵循 loading-patterns 规范：发送按钮内置小转圈 +「生成中」并禁用重复点击、宽度固定；对话流等待期显示居中转圈 +「墨渊正在提笔…」；非流式整段返回（无逐字与停止按钮）
- 藏头诗 / 平仄校验的任务模板与解析逻辑复用首页现有实现，迁移至能力层
- 配色沿用墨渊棕金色系（宣纸底 / 铜边 / 棕金强调），禁用 Element Plus 默认蓝

### 前端组件

| 组件 | 说明 |
|------|------|
| views/ai-chat/index.vue | 页面壳：组合下辖组件、持有会话状态、处理 `?role=&skill=` query |
| components/ai-chat/RoleSidebar.vue | 角色栏 |
| components/ai-chat/ChatStream.vue | 对话流（气泡 + 消息头像 + 空态引导 + 自动滚动 + 平仄/藏头特殊渲染） |
| components/ai-chat/SkillChips.vue | 能力快捷 chips |
| components/ai-chat/ChatInput.vue | 输入框 + 发送按钮（Enter 发送 / Shift+Enter 换行） |
| configs/ai-chat/personas.ts | 角色常量（含 systemPrompt） |
| configs/ai-chat/skills.ts | 能力常量（含任务模板与发送前校验） |
| composables/useAiChat.ts | 会话状态、请求封装、本地记忆 |

### 前端路由

| 路径 | 说明 |
|------|------|
| `/ai-chat` | 诗宝阁 AI 对话页面（支持 `?role=xxx&skill=xxx` 预选角色与能力） |

> 首页顶部导航栏已新增「诗宝阁」入口（与「诗云」「交流广场」同级）；首页原「展风拓潮」AI 区的藏头诗 / 平仄 / 飞花令入口**保持不变，两处并存**。

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| POST /api/ai/chat | 复用现有非流式对话接口（请求 `{ message, moduleCode:'chat' }`，响应 `data.reply`） | sc-moyuan-backend AiController |
| POST /api/ai/chat/stream | 流式对话接口（请求 `{ moduleCode, messages[], temperature, maxTokens }`，响应 `text/event-stream`，事件 `{"delta":"…"}` … `[DONE]`） | sc-moyuan-backend AiController |

### 阶段规划

- **阶段一（已完成，前端）**：角色 / 能力常量、非流式对话、本地会话记忆、藏头诗与平仄特殊渲染、示例 loading 反馈
- **阶段二后端（已完成）**：`POST /api/ai/chat/stream`（Spring MVC `SseEmitter`）+ `AiModelAdapter.streamChat()` 默认降级与 `OpenAiCompatibleAdapter` 流式实现（JDK `HttpClient` 逐行解析 `SseParser`）+ `AiModelRegistry.streamByModule`；`@SkipOperationLog` 注解在两个日志切面 pointcut 排除流式方法
- **阶段二前端（待实施）**：`fetch` 解析 SSE 逐字渲染 + 停止生成；`messages` 多轮上下文；角色配置入库（复用 `ai_module_config`）

---

## 26. 全局 AI 小窗与系统知识库模块

### 功能说明

- **全站悬浮 AI 问答入口**：由全局单组件统一实现，替换诗人详情页、寻章摘句页、诗词详情页三套独立重复实现；各页面的默认位置、尺寸、主题、模块码、欢迎语、上下文注入均由路由画像配置表统一管理
- **双助理（AiAssistant）**：
  - 诗词助理 `poetry`：内容类问答（诗人、诗句、赏析），`moduleCode` 为 `null`，沿用当前页面画像的模块码
  - 网页用法助理 `system`：站内玩法、页面入口、功能使用类问答，绑定 `system_chat` 模块
  - 两者各有独立可爱头像与悬浮提示；切换时主动向用户提示当前助理的功能定位
  - 首次展开小窗、切换助理时，未被选中的头像自动弹出一次与悬浮一致的提示，约 3.6 秒后消失
- **轻量化对话**：仅保留对话流与输入框，不复用诗宝阁的角色栏、能力芯片；不与诗宝阁共享会话状态
- **会话记忆**：点击最小化保留当前会话上下文；点击关闭（叉号）清空上下文
- **登录门禁**：未登录点击唤起时提示「AI对话功能需要登录后才能使用」并跳转登录页，携带当前页面作为回跳地址
- **意图分流**：发送前做轻量意图分流——系统类问题走知识库检索与 FAQ 直答，内容类问题直接跳过知识库，避免误命中与跨功能污染
- **路由跳转入口**：跳转按钮由前端根据命中的知识条目 `route` 字段直接生成，不依赖模型输出 `[[route]]` 标记，避免幻觉
- **知识库兜底**：知识库加载失败标记为未加载，用户再次打开小窗时自动补拉一次，避免跳转链接永久失效

### 后端与数据结构

- `system_knowledge` 表：知识条目（code/title/category/keywords/aliases/body/steps/faq/route/is_show/sort_order/deleted），种子 24 条
- `ai_module_config` 新增 `system_chat` 模块（绑定 DeepSeek），与 `chat` 模块完全隔离，避免污染诗宝阁、藏头诗等其他 AI 功能提示词
- SecurityConfig：`/api/system-knowledge` 匿名可访问；`/api/ai/**` 已移除匿名放行，未登录返回 401
- 公开接口：`GET /api/system-knowledge`（只读列表）
- 后台接口：`GET /api/admin/system-knowledge`、`GET /{id}`、`POST`、`PUT /{id}`、`PUT /{id}/show`、`DELETE /{id}`

### 前端组件

| 组件 | 说明 |
|------|------|
| components/common/AiChatWindow.vue | 全局 AI 小窗主体（对话流 + 输入框 + 助理切换 + 知识库跳转入口） |
| components/common/AiChatFab.vue | 全局悬浮唤起按钮 |
| components/common/AiAssistantSwitch.vue | 双助理切换按钮（含首次/切换提示气泡） |
| configs/ai-chat/pageProfiles.ts | 路由画像配置表（各页面位置/尺寸/主题/模块码/欢迎语/上下文注入） |
| configs/ai-chat/assistants.ts | 双助理定义（头像、提示、介绍、moduleCode） |
| utils/systemKnowledgeSearch.ts | 系统知识库检索与 FAQ 匹配 |
| stores/aiChat.ts | 小窗会话状态与知识库加载状态 |

### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/system-knowledge | 公开读取系统知识库条目 | sc-moyuan-backend SystemKnowledgeController |
| POST /api/ai/chat | 诗词助理 / 网页用法助理对话（`moduleCode` 分别为页面画像值 / `system_chat`） | sc-moyuan-backend AiController |

---

**文档版本**：v3.17
**最后更新**：2026-10-27（推荐算法说明补充「四组模型策略（离线制品查表）」与「后台实时看板」；后台控制台推荐指标屏新增缓存覆盖、模型版本、近 7 天切换趋势、切换去向/页面分布与最近事件）；此前 v3.16（推荐算法说明由 UserCF 更新为 ItemCF，诗词行为矩阵同一作品多行为取最大值合并）；此前 v3.15（新增第 26 章「全局 AI 小窗与系统知识库」：双助理、`system_chat` 模块、`system_knowledge` 表、路由画像、意图分流与 FAQ 直答、知识库跳转入口、登录门禁；模块总数 25 → 26）；v3.14（首页「诗境 AI」展开面板新增「前往完整页面」入口，按工具预选诗宝阁角色与能力；韵脚查询/对对联补充为诗宝阁新能力，能力总数 10 → 12；功能卡增加自动横向滑动）；2026-10-19（AI 对话页模块名统一由「诗阁」更名为「诗宝阁」；此前第 25 章「AI 对话页『诗宝阁』」补充对话流消息头像说明：AI 侧统一卡通头像 / 用户侧本人头像，气泡与头像左右对齐；v3.13 补充阶段二后端流式接口 `POST /api/ai/chat/stream`（SSE 流式）说明与接口表条目；v3.12 新增第 25 章：6 角色 × 10 能力的角色化统一对话入口，阶段一纯前端复用 `POST /api/ai/chat` 非流式接口）
**维护人员**：墨渊开发团队