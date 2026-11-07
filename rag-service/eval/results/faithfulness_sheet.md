# 忠实度人工评阅表

对每题回答逐条核对：回答中的每条结论能否在下方上下文中找到依据。
在「评级」列填：A=全部有依据，B=存在轻微外推，C=存在无依据或与资料矛盾的结论。

## E1（easy）用户评分和 AI 评分的评分范围是多少？分别调用哪个接口？

**回答**：用户评分范围为 1.0-5.0 分，AI 评分由系统自动计算（失败兜底 3.5），无固定上限下限设定。

**用户评分接口：**
*   **GET 接口**：`GET /api/poems/{poemId}/ratings`（获取评分详情）
*   **POST 接口**：`POST .../ratings?score&comment`（提交带分数的用户评分，需校验 score∈[1.0,5.0]）

**AI 评分接口：**
*   **POST 接口**：`POST .../ratings/ai`（触发 AI 自动评分，rating_type=2，userId=null）
*   **GET 接口**：`GET /api/poems/{poemId}/ratings`（获取包含 AI 评分的详情）

（来源：moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / 功能说明；moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.4 诗词评分（C4）；moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条）/ 诗词 AI 评分（play.rating））

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 搜索评分算法】
【章节：业务模块说明 / 8. 搜索模块 / 搜索评分算法】
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

【moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / 功能说明】
【章节：业务模块说明 / 16. 诗词评分模块 / 功能说明】
### 功能说明

- 用户评分：用户对诗词进行评分（1.0-5.0分）
- AI评分：请求AI对诗词进行评分和分析
- 评分维度：支持多维度评分（格律、意境、用词、情感、创新）
- 评分统计：诗词平均评分和评分数量
- 评分详情：展示用户评分和AI评分详情
- 重新评价：用户已评分后可点击"重新评价"修改评分

【moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / 评分类型】
【章节：业务模块说明 / 16. 诗词评分模块 / 评分类型】
### 评分类型

| 类型 | 说明 |
|------|------|
| 用户评分 | 用户手动评分，可附带评语 |
| AI评分 | AI自动评分，附带AI分析内容 |

【moyuan_business_modules.md · 业务模块说明 / 25. AI 对话页「诗宝阁」模块 / 功能说明】
【章节：业务模块说明 / 25. AI 对话页「诗宝阁」模块 / 功能说明】
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
- **会话记忆**：按 `roleId` 存服务端 Redis（键 `ai:chat:session:{userId}:{roleId}`，TTL 7 天、最多 40 条），切换角色各自恢复，换设备登录同一账号可继续；本地 `localStorage`（键 `moyuan_ai_chat_${roleId}`）作为降级缓存，Redis 不可用时仍可用。单条内容上限 4000 字
- **空态引导**：进入页面 / 新建会话后，按角色给出引导语 + 推荐能力 chip
- **结果渲染**：普通能力渲染为文本气泡，流式逐字追加并自动滚底；藏头诗高亮每句首字（藏头字）；平仄校验改调本地确定性引擎 `POST /api/ai/tone-check`，把返回的逐字平仄与问题列表渲染为平仄标签卡（平底/仄底，三平尾、三仄尾、二四六失替等问题标红），不再依赖大模型输出 JSON
- **消息头像**：对话流每条消息两侧带圆形头像——AI 侧统一使用墨渊可爱卡通头像（本地静态资源 `/img/ai_chat_avatar.jpg`），用户侧使用本人头像（未登录 / 无头像时回退金色首字母）；用户气泡靠右、头像居其右，AI 气泡靠左、头像居其左，加载态气泡同样带 AI 头像

【moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.4 诗词评分（C4）】
【章节：系统工作流文档 / 8. 诗词内容域 / 8.4 诗词评分（C4）】
### 8.4 诗词评分（C4）
- **触发入口**：`GET /api/poems/{poemId}/ratings`；`POST .../ratings?score&comment`；`POST .../ratings/ai`、`GET/regenerate`；`GET .../ratings/me`。
- **分步流程**：校验 score∈[1.0,5.0] → `ratePoem`（`@Transactional`）按 `poem_id+user_id+rating_type=1` 增改评分 → `updatePoemAverageScore` 重算 `avg_score/rating_count`；AI 评分 `rating_type=2`（userId=null）：`aiService.analyzePoem(content, null, "rating")` 走 `ai_module_config` 的独立 `rating` 模块绑定（不再写死默认模型，可在后台「AI功能配置」页面调整）→ 正则解析分（失败兜底 3.5）→ 摘要取前 200 字、去 markdown 标记，随后重算均分，`ai_model` 字段记录实际生效模型名。`getPoemRatings` 返回 userRatings(type=1)/aiRating(type=2 最新)/averageScore/ratingCount，异常回退空数据。
- **关键类与方法**：`PoemRatingServiceImpl.ratePoem/requestAiRating/parseAiScore/generateSummary/updatePoemAverageScore`、`AiService.analyzePoem`。`PoemRatingController.*`。
- **数据与缓存**：`poem_rating`（score/comment/rating_type:1用户2AI/ai_model/ai_summary/ai_analysis）、`poem.avg_score/rating_count`。

【moyuan_system_workflow.md · 系统工作流文档 / 11. AI 能力域 / 11.1 AI 辅助创作与生成（E1）】
【章节：系统工作流文档 / 11. AI 能力域 / 11.1 AI 辅助创作与生成（E1）】
### 11.1 AI 辅助创作与生成（E1）
- **触发入口**：`POST /api/ai/chat|write-poem|analyze|couplet|ocr`；流式入口 `POST /api/ai/chat/stream`（SSE，`AiController.chatStream` → `AiService.streamChat` → `AiModelRegistry.streamByModule` → `OpenAiCompatibleAdapter.streamChat` 用 JDK `HttpClient` 逐行解析上游 SSE，客户端断开经 `emitter.onCompletion` 置取消信号停止消费）；另有 `POST /api/ai/tone-check`（平仄校验，走本地 `PingZeService` 确定性引擎，不调用大模型、**不计 token**，但仍计入请求次数限流）。
- **统一防护**：上述入口在进入 `AiService` 前依次经过 `AiGuardService.checkRequest`（每分钟/每日请求数与 token 配额，Redis 计数，admin 豁免）与 `AiInputValidator`（字数、消息条数、角色白名单、`maxTokens/temperature` 区间）；超限分别返回 `4291/4292` 与 `4290`。`/api/ai/ocr` 还受 `ai.features.ocr-enabled` 开关控制，关闭时返回 `4293`（当前默认关闭，前端入口置灰）。图片类入口校验非空、≤10MB、MIME 白名单。
- **分步流程**：`AiController` 分发 `AiService` → 各方法内置系统提示词常量 → 文本类 `AiModelRegistry.chat/getModel`、图片类 Base64 后 `AiModelRegistry.vision`；传 `moduleCode` 走 `chatByModule/visionByModule`（按模块选模型+覆盖提示词）→ 按 `model.provider` 定位 `AiModelAdapter`，`RestTemplate` 带 `Bearer apiKey` POST 到 `ai_model.api_url`（messages=system+user、max_tokens 默认 1024）→ 解析 `choices[0].message.content` 返回；失败抛 `BusinessException`。

【moyuan_system_workflow.md · 系统工作流文档 / 17. 关键代码速查表】
【章节：系统工作流文档 / 17. 关键代码速查表】
4（诗词 AI 评分改为独立 `rating` 模块：`PoemRatingServiceImpl.requestAiRating` 由写死默认模型改为 `aiService.analyzePoem(content, null, "rating")`，`ai_model` 字段记录实际生效模型名；后台新增「AI功能配置」页面（`/admin/ai-config`）可按模块调整模型与提示词；此前 v1.3 补充流式问答入口 `POST /api/ai/chat/stream` 调用链，v1.2 新增首页小游戏区块工作流）
**维护人员**：墨渊开发团队
**维护说明**：本文为 docs 本地维护文件，受 `.git/info/exclude` 排除不提交远程；代码变更后请依据 doc-manager 技能核对本文与实现一致性。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 诗词AI评分（play.rating）】
【章节：墨渊小助手系统知识库（24 条） / 诗词AI评分（play.rating）】
## 诗词AI评分（play.rating）

- 分类：play
- 关键词：AI评分,诗词评分,评分,打分
- 别名：AI打分,诗词打分
- 前端路由：/poem/:id
- 说明：诗词详情页提供 AI 评分，由 AI 从意境、格律、修辞、情感、用词等维度评价诗词并给出分数。进入任意诗词详情页即可查看或请求 AI 评分，用户本人也可以手动给诗词评分。
- 使用步骤：
  1. 打开任意诗词详情页
  2. 查看已有评分或请求 AI 评分
  3. 查看 AI 给出的评价与分数
- 常见问答：
  - 问：AI评分在哪里  答：在诗词详情页可以查看或请求 AI 评分，AI 会多维度评价并给出分数。
```
</details>

## E2（easy）浏览历史保存在哪里？前端会把它写进浏览器本地存储吗？

**回答**：浏览历史不保存在浏览器本地存储（localStorage），而是通过服务端接口记录与保存。

- **存储位置**：后端数据库表 `user_history`，数据由 `UserHistoryServiceImpl` 服务处理（来源：moyuan_business_modules.md·业务模块说明/12. 浏览历史模块/API 接口；moyuan_system_workflow.md·系统工作流文档/5. 用户与个人中心域/5.2 浏览历史（B2））。
- **前端存储方式**：前端不写入 localStorage，收藏、AI 对话及浏览历史均走服务端接口或内存 ref（来源：moyuan_system_workflow.md·系统工作流文档/15. 前端数据流转层/15.4 localStorage 使用点汇总（K4）；moyuan_business_modules.md·业务模块说明/12. 浏览历史模块/功能说明）。
- **访问入口**：登录后可在个人中心页面 `/user/profile` 查看历史记录，支持清空操作（来源：moyuan_system_knowledge.md·墨渊小助手系统知识库（24 条）/浏览历史（feature.history）；moyuan_business_modules.md·业务模块说明/12. 浏览历史模块/API 接口）。
- **API 交互**：通过 `POST /api/history` 添加、`GET /api/history` 获取、`DELETE /api/history` 清空，后端模块为 `sc-moyuan-backend HistoryController`（来源：moyuan_business_modules.md·业务模块说明/12. 浏览历史模块/API 接口）。

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 3. 诗人模块 / 诗人数据懒加载机制】
【章节：业务模块说明 / 3. 诗人模块 / 诗人数据懒加载机制】
### 诗人数据懒加载机制

当用户访问诗人详情页时，后端会检查本地数据库中的诗人信息是否完整。如果 `biography`、`life_story`、`influence`、`anecdotes`、`avatar` 任一字段为空，则自动调用接口盒子（apihz）API 获取并补齐数据；头像按优先级：接口 `image` 字段 → `PoetDefaultAvatar` 本地预设头像兜底。

> 该运行时流程（含字段映射、头像分配完整规则与头像库）已纳入 **[系统工作流文档 system-workflow.md](system-workflow.md)**，此处不再重复维护，请以工作流文档为准。

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 前端路由】
【章节：业务模块说明 / 8. 搜索模块 / 前端路由】
### 前端路由

| 路径 | 说明 |
|------|------|
| `/poem` | 诗词列表页（含复合筛选搜索） |
| `/` (首页) | 历史的印痕模块内嵌搜索功能 |

> 说明：项目未设置独立的 `/search` 结果页路由，全局搜索能力内嵌在首页与诗词列表页中通过 `/api/search/smart` 实现。

---

【moyuan_business_modules.md · 业务模块说明 / 12. 浏览历史模块 / 功能说明】
【章节：业务模块说明 / 12. 浏览历史模块 / 功能说明】
### 功能说明

- 记录用户浏览历史
- 查看浏览历史列表
- 清空浏览历史

【moyuan_business_modules.md · 业务模块说明 / 12. 浏览历史模块 / API 接口】
【章节：业务模块说明 / 12. 浏览历史模块 / API 接口】
### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| POST /api/history | 添加浏览历史 | sc-moyuan-backend HistoryController |
| GET /api/history | 获取浏览历史 | sc-moyuan-backend HistoryController |
| DELETE /api/history | 清空浏览历史 | sc-moyuan-backend HistoryController |

---

【moyuan_system_workflow.md · 系统工作流文档 / 5. 用户与个人中心域 / 5.2 浏览历史（B2）】
【章节：系统工作流文档 / 5. 用户与个人中心域 / 5.2 浏览历史（B2）】
### 5.2 浏览历史（B2）
- **目标**：记录用户访问的诗词/诗人历史并支持查看/清理。
- **触发入口**：`GET /api/history`、`POST /api/history`、`DELETE /api/history`。
- **分步流程**：访问时经 `UserHistoryServiceImpl` 记录（target_type/target_id），列表分页返回，支持按 `DELETE` 清空。
- **关键类与方法**：`HistoryController`、`UserHistoryServiceImpl`。
- **数据与缓存**：`user_history`（前端不落 localStorage，走服务端接口）。

---

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.3 诗词页面 Store 数据流（K3）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.3 诗词页面 Store 数据流（K3）】
### 15.3 诗词页面 Store 数据流（K3）
- **分步流程**：`usePoemStore` 的 `fetchPoemList`（`GET /poems`）写 `poemList/total/currentPage/pageSize`；`fetchPoemDetail`（`GET /poems/:id`）合并 `isLiked/isFavorited` 到 `currentPoem`；`fetchModernPoems` 走 `/poems/modern/page`。诗人页无独立 Store，直接 `getPoetList/getPoetById`。
- **分类/朝代跳转兜底**：无 keyword 且带分类/朝代筛选、本地结果 `total===0` 时，`views/poem/list.vue` 依据流派→代表诗人映射（`GENRE_REPRESENTATIVE_POETS`）改以诗人关键词调 `GET /search/poems/external` 触发全网（接口盒子）搜索兜底，避免空页面。
- **关键位置**：`stores/poem.ts`、`views/poem/{list,detail}.vue`、`views/poet/{list,detail}.vue`。

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.4 localStorage 使用点汇总（K4）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.4 localStorage 使用点汇总（K4）】
### 15.4 localStorage 使用点汇总（K4）
> **代码事实核对**：收藏、AI 对话、浏览历史**并未真正写入 localStorage**（收藏走服务端、AI 聊天在内存 ref、历史走 `/history` API）。真实落盘 key 如下：
- `token`/`refreshToken`（登录态双 token，user Store 直接写）
- `lastAdminUsername`（admin 记录）
- `home_nav_animation_config`（首页导航动画配置）
- `moyuan_token`（storage 封装的登录 token，带过期结构）
- `moyuan_user_preferences`（theme/language/fontSize/pageSize/sidebarCollapsed）
- `moyuan_search_history`（上限 20 条）
- `moyuan_form_draft_poem/forum/comment`
- `moyuan_sort_preferences`（{poem,poet,forum}）
- `moyuan_cache_dynasties/categories/poets`（筛选下拉，1h）
- `moyuan_cache_external_poem_`（外部诗词详情，24h）
- `moyuan_cache_`（外部搜索结果缓存）
- **收藏/AI对话/浏览历史**：无 localStorage key，分别走服务端接口/内存/API。
- **关键位置**：`utils/storage.ts`、`stores/preferences.ts/searchHistory.ts/formDraft.ts/apiCache.ts`。

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.5 首页数据组装（K5）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.5 首页数据组装（K5）】
### 15.5 首页数据组装（K5）
- **轮播图**：静态 `/img/lb_*.jpg`、`/img/lb_shiwen_*.png`，无后端。
- **今日诗词**：`DailyPoetry` → 后端 `GET /api/external-poetry/jinrishici` 代理（`v2.jinrishici.com/one.json`，后端 60 分钟进程内缓存，前端不再直连）。
- **首页导航**：`GET /home-navigation`。
- **精选诗人**：`getRandomPoetFeatured(8)`，去重取 4 位，每 20s 轮换。
- **论坛预览**：`GET /forum/posts?pageSize=5`。
- **古诗精赏**：本地 json + `GET /search/poems` + `/search/poems/recommended`。
- **当代精选**：本地 json + like/favorite/comment 接口。
- **AI 区块**：`/ai/write-poem`、`/ai/analyze`、`/ai/couplet`、`/ai/ocr`、`/ai/chat`（藏头诗、平仄格律校验，复用 `POST /api/ai/chat` 并传 `moduleCode=chat`，不新增后端接口）、`/rhyme/*`；"诗海拾贝"本地 json。
- **小游戏区块**：首页 AI 小工具区「飞花令」「诗词接龙」两张卡片——游客点击展开内嵌小窗口（`GameMiniPanel`）直接开局，登录用户跳转 `/game/feihua`、`/game/chain`；后端为独立游戏模块（`/api/game/feihua`、`/api/game/chain`，见 `modules.md` 第 24 章）。
- **诗话视野**：`GET /vision/articles`。
- **诗词卡片跳转**：首页「古诗精赏 / 当代精选」卡片点击经组合式函数 `composables/usePoemCardNavigation.ts` 处理——本地命中直接 `router.push('/poem/{id}')`，未命中回退 `PoetryDetailDialog`（外部诗词弹窗，可由接口盒子实时增强）。
- **关键位置**：`views/home/index.vue`、`components/{home,business}/*`、`composables/usePoemCardNavigation.ts`、`api/modules/*`。

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.6 诗云 3D 星空（K6）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.6 诗云 3D 星空（K6）】
### 15.6 诗云 3D 星空（K6）
- **数据加载**：`Promise.all` 并发 `GET /dynasties`、`/poets(pageSize 300)`、`/poems(pageSize 1000)`，失败回退内置 12 朝代。
- **渲染**：`initScene`（Scene+FogExp2+Camera+WebGLRenderer(ACESFilmicToneMapping)+OrbitControls）→ 3200 背景星（Sphere+ShaderMaterial）→ 诗人星（发光小球+光晕，缓存 `poetPositionMap`）+ 同朝代贝塞尔赠诗线 → 诗作独立 Mesh 以「诗人为中心、诗作环绕分布」的球面螺旋生成并经松弛迭代拉开间距，按诗人分组的 LineSegments2 连线 → 交互：hover 高亮并显示 DOM 提示框、点击 `selectPoet→flyToStar+loadPoetPoems+focusPoetPoems`、点击诗作 `selectPoem`、朝代筛选重建。
- **配色实验室**：底部控制模块由 `reactive(viz)` 驱动（7 套方案 + 光晕/连线/尺寸/亮度滑杆 + 标签与朝代配色开关），`applyVizStyles()` 统一重涂背景/雾效、诗人节点、诗作节点与两类连线；整体亮度经画布容器 CSS `filter: brightness()`。
- **诗人姓名标注**：DOM 层 `.label-layer` + `updatePoetLabels()`，每帧 `getWorldPosition().project(camera)` 更新标签屏幕坐标，相机背后/视口外自动隐藏。
- **卸载**：`onUnmounted` 释放 geometry/material、清理标签与定时器、`scene.clear()`、`renderer.dispose()`。
- **数据与缓存**：数据仅内存 ref，不做 localStorage；`poetPositionMap` 内存缓存 3D 坐标，`poemCountMap` 缓存每位诗人的诗作数（用于节点尺寸）。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 浏览历史（feature.history）】
【章节：墨渊小助手系统知识库（24 条） / 浏览历史（feature.history）】
## 浏览历史（feature.history）

- 分类：feature
- 关键词：浏览历史,历史记录,最近看过
- 别名：历史记录
- 前端路由：/user/profile
- 说明：浏览历史记录你访问过的诗词与诗人，登录后可在个人中心查看，也支持清空。历史通过服务端接口保存，不存放在本地。
- 使用步骤：
  1. 登录后浏览诗词或诗人
  2. 在个人中心查看浏览历史
  3. 需要时清空历史记录
- 常见问答：
  - 问：浏览历史在哪  答：登录后在个人中心可以查看浏览历史，并支持清空。
```
</details>

## E3（easy）飞花令单回合限时多久？有几种模式和几档难度？

**回答**：飞花令单回合有效答题时长为**30 秒**。系统设有古制严格、标准版、超级飞花令三种模式，以及简单、中等、困难三档难度。

具体规则如下：
1. **限时机制**：单回合倒计时从点击「开始挑战」后直接启动（30 秒递减）。输入框一旦有变化即暂停倒计时进入监测状态；连续 5 秒无改动恢复递减。30 秒耗尽后进入 5 秒缓冲窗口，期内可提交；缓冲耗尽仍无变动则判超时。有效答题时长上限为 35 秒（题面计时可随时终止）。
2. **三种模式**：
   - `ancient`（古制严格）：令字须落在本回合第 N 个汉字位置。
   - `normal`（标准版）：诗句包含令字即可。
   - `super`（超级飞花令）：含多关键字（`char`）或主题（`topic`）两种子类型。
3. **三档难度**：
   - `easy`（简单）：达标 10 句即胜。
   - `medium`（中等）：达标 20 句即胜。
   - `hard`（困难）：AI 无句可接则用户胜，无固定目标条数。

（来源：moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 功能说明；moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 飞花令（play.feihualing））

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 功能说明】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 功能说明】
### 功能说明

- **飞花令**（`/game/feihua`）：按「令字」接句，用户每回合提交一句含令字的真实古诗词名句。
  - 三种模式：`ancient`（古制严格，令字须落在本回合第 N 个汉字位置）、`normal`（标准版，诗句包含令字即可）、`super`（超级飞花令，含 `char` 多关键字 / `topic` 主题两种子类型）
  - 三种难度：`easy`（达标 10 句即胜）、`medium`（达标 20 句即胜）、`hard`（AI 无句可接则用户胜，无固定目标条数）
  - 每题给出 AI 候选诗句（LLM 生成 + 后端按位次/判重/真实性二次过滤后下发）
- **诗词接龙**（`/game/chain`）：以 AI 起句的末字为首字接出下一句名句，AI 再接给用户，交替进行；本地诗词库随机取起句（不调用 LLM）；AI 接不上时用户胜，链长即有效句数。
  - **选择范围**：配置阶段可选「中国古典诗词（不限朝代，默认）」或具体朝代（取自 `dynasty` 表）；选定后 AI 起句仅在该朝代内取句，AI 接句提示词限定该朝代，且**玩家提交与 AI 接句的真实性校验均限定在该朝代内**（超出范围判为「诗句真实性校验不通过」并提示越界）。
- **回合计时（计时/监测/缓冲三子态，打字即暂停）**：单回合有效答题时长 30 秒。

【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 功能说明】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 功能说明】
- **回合计时（计时/监测/缓冲三子态，打字即暂停）**：单回合有效答题时长 30 秒。点击「开始挑战」后倒计时**直接开始**（`counting`，30 秒递减）；输入框一旦有变化即**暂停倒计时**并进入 `monitoring`（监测停笔 5 秒），仅当连续 5 秒无改动才恢复递减；30 秒耗尽后自动进入 `buffer`（5 秒缓冲窗口，期内有输入变动仍可提交）；缓冲耗尽仍无改动则由前端自动提交并判超时。有效答题时长上限 30 + 5 = 35 秒；**题面计时均为显示层，可被主动提交（作答/放弃）随时终止**。答错不重置计时，答对推进下一回合并重置计时。
- **超时判定（后端按上报有效时长）**：前端每次作答请求上报 `activeSeconds`（本回合已消耗的有效秒数），后端 `GameConstants.isTimeout(activeSeconds, wallElapsedMillis)` 优先据此判定（`>= 35` 即超时）；未上报时回退墙钟口径（服务端 `roundStartTime` 毫秒时间戳，阈值 30 秒 + 1 秒网络宽限）。
- **容错**：每回合初始 2 次重试，累计错误达 3 次判负；空输入不计错误、不消耗重试、不重置计时。
- **三级真实性校验链**：① 本地 `poem` 表 ngram 全文索引 `MATCH...

【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 功能说明】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 功能说明】
- **三级真实性校验链**：① 本地 `poem` 表 ngram 全文索引 `MATCH...AGAINST`（限定范围时附加 `dynasty_id` 过滤）→ ② 外部 APIHZ 复核（仅在**不限朝代**时可用，外部源无法确认朝代归属）→ ③ LLM 兜底真伪判断（`feihua_verify`，限定范围时提示词附朝代约束），全链路失败即判为「诗句真实性校验不通过」。
- **游客模式**：游客与新页面共用同一套接口，后端按登录态分层——**游客对局仅存 Redis（不落 MySQL），登录用户对局与有效诗句写入 `game_record` / `game_sentence`**；前端以 `GuestBadge` 展示游客标识，登录态用户对局可回查。
- **状态/判重后端权威**：对局运行时状态存 Redis（键 `game:{gameId}` 与 `game:{gameId}:sentences`，TTL 30 分钟，交互续期）；**超时判定由后端依前端上报的 `activeSeconds` 裁定**（见上文「超时判定」）。
- **输入方式**：支持文本与语音（`inputType`），语音仅标识，校验逻辑一致。

【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 玩法规则速览】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 玩法规则速览】
### 玩法规则速览

| 维度 | 取值 | 说明 |
|------|------|------|
| 玩法类型 | feihua / chain | 对局表 `game_type` 区分 |
| 飞花令模式 | ancient / normal / super | super 需 `superType`（char/topic） |
| 飞花令难度 | easy / medium / hard | 达标条数 easy=10、medium=20、hard=null（AI 枯竭即胜） |
| 接龙选择范围 | 不限（默认，中国古典诗词）/ 具体朝代 | 传 `dynastyId` 限定；起句、AI 接句、玩家校验均限定在该朝代内 |
| 对局结果 | win / lose / giveup | 写入 `game_record.game_result` |

【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / API 接口】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / API 接口】
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

【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 前端组件】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 前端组件】
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

【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 前端路由】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 前端路由】
### 前端路由

| 路径 | 说明 |
|------|------|
| `/game/feihua` | 飞花令对局页 |
| `/game/chain` | 诗词接龙对局页 |

> 首页 AI 小工具区提供「飞花令」「诗词接龙」两张卡片：游客点击展开内嵌小窗口（`GameMiniPanel`，直接在首页开局），登录用户跳转到 `/game/feihua`、`/game/chain` 游戏页。

【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 数据库表】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 数据库表】
### 数据库表

- `game_record`：游戏对局主表（飞花令/诗词接龙共用，`game_type` 区分）
- `game_sentence`：对局有效诗句明细表（`uk_game_sentence` 唯一键兜底判重）

---

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 飞花令（play.feihualing）】
【章节：墨渊小助手系统知识库（24 条） / 飞花令（play.feihualing）】
## 飞花令（play.feihualing）

- 分类：play
- 关键词：飞花令,对诗,令字,行酒令,诗词游戏,怎么玩
- 别名：花令,飞花令游戏
- 前端路由：/game/feihua
- 说明：飞花令是与 AI 对句的诗词游戏，你每回合说出一句含指定「令字」的真实古诗词名句，AI 轮流接句，接不上或超时即判负。入口在首页 AI 小工具区的「飞花令」卡片，登录用户点击后进入对局页。有古制严格、标准版、超级飞花令三种模式与三档难度可选。现代诗不参与本游戏。
- 使用步骤：
  1. 选择模式与难度
  2. 设定令字（如「花」）
  3. 每回合提交一句含令字的古诗词，单回合限时 30 秒
  4. 与 AI 交替接句，AI 接不上或你超时判负
- 常见问答：
  - 问：飞花令怎么玩  答：每回合提交一句含指定令字的真实古诗词名句，与 AI 交替接句，AI 接不上或你超时则对方判负。
  - 问：在哪里能玩飞花令  答：首页 AI 小工具区有「飞花令」卡片，登录用户点击进入对局页。
  - 问：飞花令有几种模式  答：有古制严格、标准版、超级飞花令三种模式，难度分简单、中等、困难三档。
```
</details>

## E4（easy）诗词评分支持哪些维度？

**回答**：诗词评分支持以下五个维度：格律、意境、用词、情感、创新（来源：moyuan_business_modules.md·业务模块说明/2. 诗词模块/评分维度；moyuan_business_modules.md·业务模块说明/16. 诗词评分模块/评分维度）。

具体说明如下：
- **格律**：评估诗词的格律规范性。
- **意境**：评估诗词的意境表达。
- **用词**：评估诗词的用词精准度。
- **情感**：评估诗词的情感表达。
- **创新**：评估诗词的创新程度。

用户评分与 AI 评分均支持上述多维度评价，AI 还会结合修辞等维度进行分析（来源：moyuan_system_workflow.md·系统工作流文档/8. 诗词内容域/8.4 诗词评分（C4）；墨渊小助手系统知识库（24 条）·诗词 AI 评分（play.rating））。

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 2. 诗词模块 / API 接口】
【章节：业务模块说明 / 2. 诗词模块 / API 接口】
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

【moyuan_business_modules.md · 业务模块说明 / 2. 诗词模块 / 评分维度】
【章节：业务模块说明 / 2. 诗词模块 / 评分维度】
### 评分维度

诗词评分支持以下维度：
- **格律**：诗词的格律规范性
- **意境**：诗词的意境表达
- **用词**：诗词的用词精准度
- **情感**：诗词的情感表达
- **创新**：诗词的创新程度

【moyuan_business_modules.md · 业务模块说明 / 2. 诗词模块 / 前端路由】
【章节：业务模块说明 / 2. 诗词模块 / 前端路由】
### 前端路由

| 路径 | 说明 |
|------|------|
| `/poem` | 诗词列表页 |
| `/poem/create` | 发布新诗（支持原创标识） |
| `/poem/:id` | 诗词详情页（含评分功能） |

---

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 复合搜索策略】
【章节：业务模块说明 / 8. 搜索模块 / 复合搜索策略】
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

【moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / 功能说明】
【章节：业务模块说明 / 16. 诗词评分模块 / 功能说明】
### 功能说明

- 用户评分：用户对诗词进行评分（1.0-5.0分）
- AI评分：请求AI对诗词进行评分和分析
- 评分维度：支持多维度评分（格律、意境、用词、情感、创新）
- 评分统计：诗词平均评分和评分数量
- 评分详情：展示用户评分和AI评分详情
- 重新评价：用户已评分后可点击"重新评价"修改评分

【moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / 评分类型】
【章节：业务模块说明 / 16. 诗词评分模块 / 评分类型】
### 评分类型

| 类型 | 说明 |
|------|------|
| 用户评分 | 用户手动评分，可附带评语 |
| AI评分 | AI自动评分，附带AI分析内容 |

【moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / 评分维度】
【章节：业务模块说明 / 16. 诗词评分模块 / 评分维度】
### 评分维度

| 维度 | 说明 |
|------|------|
| 格律 | 诗词的格律规范性 |
| 意境 | 诗词的意境表达 |
| 用词 | 诗词的用词精准度 |
| 情感 | 诗词的情感表达 |
| 创新 | 诗词的创新程度 |

【moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / API 接口】
【章节：业务模块说明 / 16. 诗词评分模块 / API 接口】
### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/poems/{id}/ratings | 获取诗词评分详情 | sc-moyuan-backend PoemRatingController |
| POST /api/poems/{id}/ratings | 用户评分 | sc-moyuan-backend PoemRatingController |
| POST /api/poems/{id}/ratings/ai | 请求AI评分 | sc-moyuan-backend PoemRatingController |
| GET /api/poems/{id}/ratings/ai | 获取AI评分 | sc-moyuan-backend PoemRatingController |
| GET /api/poems/{id}/ratings/me | 获取当前用户评分 | sc-moyuan-backend PoemRatingController |

【moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / 前端组件】
【章节：业务模块说明 / 16. 诗词评分模块 / 前端组件】
### 前端组件

| 组件 | 说明 |
|------|------|
| poem/detail.vue | 诗词详情页（集成评分功能） |

---

【moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.4 诗词评分（C4）】
【章节：系统工作流文档 / 8. 诗词内容域 / 8.4 诗词评分（C4）】
### 8.4 诗词评分（C4）
- **触发入口**：`GET /api/poems/{poemId}/ratings`；`POST .../ratings?score&comment`；`POST .../ratings/ai`、`GET/regenerate`；`GET .../ratings/me`。
- **分步流程**：校验 score∈[1.0,5.0] → `ratePoem`（`@Transactional`）按 `poem_id+user_id+rating_type=1` 增改评分 → `updatePoemAverageScore` 重算 `avg_score/rating_count`；AI 评分 `rating_type=2`（userId=null）：`aiService.analyzePoem(content, null, "rating")` 走 `ai_module_config` 的独立 `rating` 模块绑定（不再写死默认模型，可在后台「AI功能配置」页面调整）→ 正则解析分（失败兜底 3.5）→ 摘要取前 200 字、去 markdown 标记，随后重算均分，`ai_model` 字段记录实际生效模型名。`getPoemRatings` 返回 userRatings(type=1)/aiRating(type=2 最新)/averageScore/ratingCount，异常回退空数据。
- **关键类与方法**：`PoemRatingServiceImpl.ratePoem/requestAiRating/parseAiScore/generateSummary/updatePoemAverageScore`、`AiService.analyzePoem`。`PoemRatingController.*`。
- **数据与缓存**：`poem_rating`（score/comment/rating_type:1用户2AI/ai_model/ai_summary/ai_analysis）、`poem.avg_score/rating_count`。

【moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.5 外部诗词导入（C5）】
【章节：系统工作流文档 / 8. 诗词内容域 / 8.5 外部诗词导入（C5）】
### 8.5 外部诗词导入（C5）
- **触发入口**：`POST /api/poems/import-external`（title/content/author/dynasty）；`POST /api/poems/fix-external-poems`。
- **分步流程**：校验 title → 按 dynasty 名称查朝代 → 按 author 查诗人（无则新建 Poet 并 `clearAllCache`）→ 按 `title+poetId` 查重：命中补全空缺字段返回 `{id,imported:false}`，未命中落库（`source=external`、`poemType=ancient`、`isOriginal=0`、`auditStatus=1`、status=1）并清 `poems` 缓存返回 `{id,imported:true}`。修复接口取 `source=external` 且 `poet_id is null` 前 100 条，调 `https://cn.apihz.cn/api/zici/poetry.php` 补全作者朝代，成功后清 `poets`/`poems` 缓存返回 `{total,fixed}`。
- **关键类与方法**：`PoemController.importExternalPoem/fixExternalPoems`、`PoemServiceImpl`、`PoetDefaultAvatar.getAvatar`。
- **数据与缓存**：`poem.source/is_original/poem_type/audit_status`；清缓存 key：`poems`、`poets`。

---

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 诗词AI评分（play.rating）】
【章节：墨渊小助手系统知识库（24 条） / 诗词AI评分（play.rating）】
## 诗词AI评分（play.rating）

- 分类：play
- 关键词：AI评分,诗词评分,评分,打分
- 别名：AI打分,诗词打分
- 前端路由：/poem/:id
- 说明：诗词详情页提供 AI 评分，由 AI 从意境、格律、修辞、情感、用词等维度评价诗词并给出分数。进入任意诗词详情页即可查看或请求 AI 评分，用户本人也可以手动给诗词评分。
- 使用步骤：
  1. 打开任意诗词详情页
  2. 查看已有评分或请求 AI 评分
  3. 查看 AI 给出的评价与分数
- 常见问答：
  - 问：AI评分在哪里  答：在诗词详情页可以查看或请求 AI 评分，AI 会多维度评价并给出分数。
```
</details>

## E5（easy）搜索评分中标题完全匹配得多少分？内容包含拆分词得多少分？

**回答**：搜索评分中，标题完全匹配得 1.0 分；内容包含拆分词得 0.2 分。

具体评分规则如下：
- **标题完全匹配**：去除标点后完全一致，得分为 1.0（来源：moyuan_business_modules.md·业务模块说明/8.搜索模块/搜索评分算法）。
- **内容包含拆分词**：诗词内容中包含拆分后的单个关键词，得分为 0.2（来源：moyuan_business_modules.md·业务模块说明/8.搜索模块/搜索评分算法）。

此外，在智能搜索的三路并行逻辑中，标题拆分词匹配同样适用 0.3 分规则，而内容拆分词匹配仍为 0.2 分（来源：moyuan_system_workflow.md·系统工作流文档/10.搜索与运营域/10.1 全文/跨模块智能搜索）。

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 功能说明】
【章节：业务模块说明 / 8. 搜索模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 复合搜索策略】
【章节：业务模块说明 / 8. 搜索模块 / 复合搜索策略】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 搜索评分算法】
【章节：业务模块说明 / 8. 搜索模块 / 搜索评分算法】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 搜索流程】
【章节：业务模块说明 / 8. 搜索模块 / 搜索流程】
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

【moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / 功能说明】
【章节：业务模块说明 / 16. 诗词评分模块 / 功能说明】
### 功能说明

- 用户评分：用户对诗词进行评分（1.0-5.0分）
- AI评分：请求AI对诗词进行评分和分析
- 评分维度：支持多维度评分（格律、意境、用词、情感、创新）
- 评分统计：诗词平均评分和评分数量
- 评分详情：展示用户评分和AI评分详情
- 重新评价：用户已评分后可点击"重新评价"修改评分

【moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / 评分类型】
【章节：业务模块说明 / 16. 诗词评分模块 / 评分类型】
### 评分类型

| 类型 | 说明 |
|------|------|
| 用户评分 | 用户手动评分，可附带评语 |
| AI评分 | AI自动评分，附带AI分析内容 |

【moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / 评分维度】
【章节：业务模块说明 / 16. 诗词评分模块 / 评分维度】
### 评分维度

| 维度 | 说明 |
|------|------|
| 格律 | 诗词的格律规范性 |
| 意境 | 诗词的意境表达 |
| 用词 | 诗词的用词精准度 |
| 情感 | 诗词的情感表达 |
| 创新 | 诗词的创新程度 |

【moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.4 诗词评分（C4）】
【章节：系统工作流文档 / 8. 诗词内容域 / 8.4 诗词评分（C4）】
### 8.4 诗词评分（C4）
- **触发入口**：`GET /api/poems/{poemId}/ratings`；`POST .../ratings?score&comment`；`POST .../ratings/ai`、`GET/regenerate`；`GET .../ratings/me`。
- **分步流程**：校验 score∈[1.0,5.0] → `ratePoem`（`@Transactional`）按 `poem_id+user_id+rating_type=1` 增改评分 → `updatePoemAverageScore` 重算 `avg_score/rating_count`；AI 评分 `rating_type=2`（userId=null）：`aiService.analyzePoem(content, null, "rating")` 走 `ai_module_config` 的独立 `rating` 模块绑定（不再写死默认模型，可在后台「AI功能配置」页面调整）→ 正则解析分（失败兜底 3.5）→ 摘要取前 200 字、去 markdown 标记，随后重算均分，`ai_model` 字段记录实际生效模型名。`getPoemRatings` 返回 userRatings(type=1)/aiRating(type=2 最新)/averageScore/ratingCount，异常回退空数据。
- **关键类与方法**：`PoemRatingServiceImpl.ratePoem/requestAiRating/parseAiScore/generateSummary/updatePoemAverageScore`、`AiService.analyzePoem`。`PoemRatingController.*`。
- **数据与缓存**：`poem_rating`（score/comment/rating_type:1用户2AI/ai_model/ai_summary/ai_analysis）、`poem.avg_score/rating_count`。

【moyuan_system_workflow.md · 系统工作流文档 / 10. 搜索与运营域 / 10.1 全文/跨模块智能搜索（C8）】
【章节：系统工作流文档 / 10. 搜索与运营域 / 10.1 全文/跨模块智能搜索（C8）】
### 10.1 全文/跨模块智能搜索（C8）
- **触发入口**：`GET /api/search/smart?dynastyId&categoryId&poetId&poetName&keyword&sortBy&pageNum&pageSize`；`GET /api/search?keyword=`（三段返回 poems/poets/posts）；`GET /api/search/poems`；`GET /api/search/suggestions|hot|history`、`DELETE /api/search/history`。
- **分步流程**（`SmartSearchServiceImpl.smartSearch`）：
  1. 有 keyword 且已登录 → `saveSearchHistory`。
  2. `resolvePoetNameIds`：poetName 对 poet.name 模糊匹配取 ID 集。
  3. 无 keyword → `searchWithoutKeyword` 按筛选条件分页 + 排序（最新/热门/点赞）。
  4. 有 keyword 三路并发，命中结果统一按 poem.id 去重取高分、内存分页，为空置 `suggestExternal=true`：
     - `searchPoems`（名称匹配，**双模式**）：
       - 关闭全文（`search.fulltext-enabled=false`）：对 title/content 用原始词 + 去标点拆分词 `like` 模糊匹配。
       - **默认开启**（`search.fulltext-enabled=true`，见 `application.yml`）：改用 ngram 中文全文索引 `MATCH(title, content) AGAINST(... IN NATURAL LANGUAGE MODE)`（`poem` 表 `ft_poem_title_content` 已 `WITH PARSER ngram`，中文分词 + 相关度更高，见 `docs/database/migration_004_poem_search_fulltext.sql`）。
       - 两模式命中集统一经 `scoreAndRank` 按 `calculateScore` 打分：标题精确 1.0/包含 0.8/拆分 0.3、内容包含 0.5/拆分 0.2。
     - `searchByPoetName`：匹配诗人名取作品，精确 0.9/包含 0.7/部分 0.3。
     - `searchByPinyin`：`PinyinUtil.matchesPinyin` 拼音联想，基础 0.3+标题 0.2+诗人 0.1。
  5.

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 诗词AI评分（play.rating）】
【章节：墨渊小助手系统知识库（24 条） / 诗词AI评分（play.rating）】
## 诗词AI评分（play.rating）

- 分类：play
- 关键词：AI评分,诗词评分,评分,打分
- 别名：AI打分,诗词打分
- 前端路由：/poem/:id
- 说明：诗词详情页提供 AI 评分，由 AI 从意境、格律、修辞、情感、用词等维度评价诗词并给出分数。进入任意诗词详情页即可查看或请求 AI 评分，用户本人也可以手动给诗词评分。
- 使用步骤：
  1. 打开任意诗词详情页
  2. 查看已有评分或请求 AI 评分
  3. 查看 AI 给出的评价与分数
- 常见问答：
  - 问：AI评分在哪里  答：在诗词详情页可以查看或请求 AI 评分，AI 会多维度评价并给出分数。
```
</details>

## E6（easy）智能搜索的建议词、热门词和搜索历史分别缓存成什么 key？

**回答**：智能搜索的建议词、热门词和搜索历史分别缓存为以下 Redis Key：

- **建议词**：`search:suggestion:{kw}`（来源：moyuan_system_workflow.md·10.1 全文/跨模块智能搜索）
- **热门词**：`search:hot`（ZSet 结构，来源：moyuan_business_modules.md·缓存策略 / moyuan_system_workflow.md·10.1 全文/跨模块智能搜索）
- **搜索历史**：`search:history:{userId}`（List 结构，来源：moyuan_business_modules.md·搜索历史 / moyuan_system_workflow.md·10.1 全文/跨模块智能搜索）

所有缓存均基于 Redis 实现，其中建议词和热门词支持 5 分钟或实时统计，搜索历史为已登录用户保存且 30 天过期。

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 功能说明】
【章节：业务模块说明 / 8. 搜索模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 技术方案】
【章节：业务模块说明 / 8. 搜索模块 / 技术方案】
### 技术方案

- **搜索核心**：SmartSearchService，多条件复合查询 + 评分排序
- **搜索引擎**：MySQL 8 ngram 中文全文检索（`/api/search/smart` 关键词默认走 `MATCH(title,content) AGAINST(...)`，`search.fulltext-enabled` 默认开启，可经 `SEARCH_FULLTEXT_ENABLED` 关闭回退 LIKE）；Elasticsearch 属后期可选优化
- **拼音支持**：PinyinUtil 工具类，可配置搜索范围（默认200条，最大1000条）
- **缓存策略**：Redis 缓存搜索建议（5分钟）、热门搜索（实时统计）、搜索历史（30天）
- **推荐服务**：RecommendationService，协同过滤 + 兴趣推荐 + 外部API
- **外部API集成**：今日诗词API，补充古诗词数据源

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 搜索流程】
【章节：业务模块说明 / 8. 搜索模块 / 搜索流程】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / API 接口】
【章节：业务模块说明 / 8. 搜索模块 / API 接口】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 智能搜索参数】
【章节：业务模块说明 / 8. 搜索模块 / 智能搜索参数】
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

【moyuan_system_workflow.md · 系统工作流文档 / 10. 搜索与运营域 / 10.1 全文/跨模块智能搜索（C8）】
【章节：系统工作流文档 / 10. 搜索与运营域 / 10.1 全文/跨模块智能搜索（C8）】
### 10.1 全文/跨模块智能搜索（C8）
- **触发入口**：`GET /api/search/smart?dynastyId&categoryId&poetId&poetName&keyword&sortBy&pageNum&pageSize`；`GET /api/search?keyword=`（三段返回 poems/poets/posts）；`GET /api/search/poems`；`GET /api/search/suggestions|hot|history`、`DELETE /api/search/history`。
- **分步流程**（`SmartSearchServiceImpl.smartSearch`）：
  1. 有 keyword 且已登录 → `saveSearchHistory`。
  2. `resolvePoetNameIds`：poetName 对 poet.name 模糊匹配取 ID 集。
  3. 无 keyword → `searchWithoutKeyword` 按筛选条件分页 + 排序（最新/热门/点赞）。
  4. 有 keyword 三路并发，命中结果统一按 poem.id 去重取高分、内存分页，为空置 `suggestExternal=true`：
     - `searchPoems`（名称匹配，**双模式**）：
       - 关闭全文（`search.fulltext-enabled=false`）：对 title/content 用原始词 + 去标点拆分词 `like` 模糊匹配。
       - **默认开启**（`search.fulltext-enabled=true`，见 `application.yml`）：改用 ngram 中文全文索引 `MATCH(title, content) AGAINST(... IN NATURAL LANGUAGE MODE)`（`poem` 表 `ft_poem_title_content` 已 `WITH PARSER ngram`，中文分词 + 相关度更高，见 `docs/database/migration_004_poem_search_fulltext.sql`）。
       - 两模式命中集统一经 `scoreAndRank` 按 `calculateScore` 打分：标题精确 1.0/包含 0.8/拆分 0.3、内容包含 0.5/拆分 0.2。
     - `searchByPoetName`：匹配诗人名取作品，精确 0.9/包含 0.7/部分 0.3。
     - `searchByPinyin`：`PinyinUtil.matchesPinyin` 拼音联想，基础 0.3+标题 0.2+诗人 0.1。
  5.

【moyuan_system_workflow.md · 系统工作流文档 / 10. 搜索与运营域 / 10.1 全文/跨模块智能搜索（C8）】
【章节：系统工作流文档 / 10. 搜索与运营域 / 10.1 全文/跨模块智能搜索（C8）】
建议：先查 Redis `search:suggestion:{kw}`（5min），未命中从 poem 标题+诗人名并集。
  6. 热门：读 Redis ZSet `search:hot` 逆序；空返回默认词。历史：`search:history:{userId}` List（remove+leftPush+trim(0,19)+30天过期），同时 `zset.incrementScore` 累加热度。
  7. `/api/search/poems` 走 `recommendationService.searchPoems`：本地（附 `recommendScore` 含兴趣加权+浏览/点赞对数分），不足时拉外部（author 含 keyword，`recommendScore=0.5`）合并降序。
- **关键类与方法**：`SmartSearchServiceImpl.*`、`SearchController.*`、`PinyinUtil.matchesPinyin`、`RecommendationServiceImpl.searchPoems/getExternalPoems/calculatePoemRecommendScore`；配置 `search.fulltext-enabled`（默认 true，全文已开启，可用环境变量 `SEARCH_FULLTEXT_ENABLED` 覆盖）、`search.pinyin.limit`。
- **数据与缓存**：Redis `search:suggestion:{kw}`、`search:hot`(ZSet)、`search:history:{userId}`(List)；`poem` 表全文索引 `ft_poem_title_content`（ngram）。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 智能检索（feature.search）】
【章节：墨渊小助手系统知识库（24 条） / 智能检索（feature.search）】
## 智能检索（feature.search）

- 分类：feature
- 关键词：搜索,检索,找诗,搜诗人,关键字
- 别名：智能搜索,全文搜索
- 前端路由：/poem
- 说明：智能检索支持按关键词搜索诗词、诗人与帖子，首页与诗词列表页内嵌了搜索入口。搜索会记录你的历史，登录后还会根据兴趣给出推荐。
- 使用步骤：
  1. 在首页或诗词列表页输入关键词
  2. 查看匹配的诗词、诗人或帖子
  3. 点开结果查看详情
- 常见问答：
  - 问：怎么搜索  答：在首页或诗词列表页的搜索框输入关键词，即可搜索诗词、诗人与帖子。
```
</details>

## E7（easy）登录成功后签发哪两种 token，各自有效期多久？

**回答**：登录成功后签发的两种 token 为 access token（访问令牌）和 refresh token（刷新令牌），其有效期如下：

1. **Access Token**：默认有效期为 **24 小时**。该令牌用于后续请求的自动续期，包含随机生成的 `jti` 标识符。
2. **Refresh Token**：默认有效期为 **7 天**。该令牌用于在 Access Token 过期后获取新的访问令牌，与 Access Token 共享相同的 `jti`。

（来源：moyuan_system_workflow.md · 系统工作流文档 / 4. 认证与权限域 / 4.2 登录与 JWT 签发（A2）；moyuan_business_modules.md · 业务模块说明 / 4. 诗人认证模块 / API 接口）

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 4. 诗人认证模块 / API 接口】
【章节：业务模块说明 / 4. 诗人认证模块 / API 接口】
### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/poet-profile/me | 获取当前用户诗人资料 | sc-moyuan-backend PoetProfileController |
| POST /api/poet-profile/apply | 申请诗人认证 | sc-moyuan-backend PoetProfileController |
| GET /api/poet-profile/{userId} | 获取指定用户诗人资料 | sc-moyuan-backend PoetProfileController |
| GET /api/admin/poet-profiles | 获取诗人认证申请列表（管理员） | sc-moyuan-backend AdminController |
| PUT /api/admin/poet-profiles/{id}/verify | 审核诗人认证（管理员） | sc-moyuan-backend AdminController |

【moyuan_system_workflow.md · 系统工作流文档 / 2. 全局约定与分层（引擎级机制） / 2.3 JWT 认证与过滤器链】
【章节：系统工作流文档 / 2. 全局约定与分层（引擎级机制） / 2.3 JWT 认证与过滤器链】
### 2.3 JWT 认证与过滤器链
- 无状态会话（`SessionCreationPolicy.STATELESS`），关闭 CSRF。
- `JwtAuthenticationFilter`（挂在 `UsernamePasswordAuthenticationFilter` 之前）从 `Authorization: Bearer` 取 token → 验签/过期 → 校验 **type=access** 且 `jti` 未被 `TokenBlacklistService` 拉黑 → 查库 → 组装 `LoginUser.getAuthorities()`（`ROLE_`+role）写入 SecurityContext。当前 JWT 采用**双 token**（access 短时效 + refresh 续期），登出/刷新将旧 token 的 `jti` 加入黑名单使其失效（详见 4.2、4.3）。
- 公开接口 `permitAll`（首页静态资源、Swagger、`/api/auth/**`、浏览类接口、`/uploads/**` 等）；`/api/admin/**` 需 `ROLE_ADMIN`；其余需登录。详细放行清单见 4.3。

【moyuan_system_workflow.md · 系统工作流文档 / 4. 认证与权限域 / 4.1 用户注册（A1）】
【章节：系统工作流文档 / 4. 认证与权限域 / 4.1 用户注册（A1）】
### 4.1 用户注册（A1）
- **目标**：完成参数校验、用户名/邮箱唯一性检查、密码 BCrypt 加密并落库，注册成功即签发 JWT 直接登录。
- **触发入口**：`POST /api/auth/register`（`@RequestBody RegisterRequest`）。
- **分步流程**：
  1. `@Valid` 校验：username 长度 3–20、password 长度 6–20、email/interests 可选；失败抛 `MethodArgumentNotValidException` → 400。
  2. `UserServiceImpl.register` 查 `user` 表：用户名重复抛 `USERNAME_EXISTS(1002)`、email 重复抛 `EMAIL_EXISTS(1003)`。
  3. `passwordEncoder.encode`（BCrypt）加密密码。
  4. 补全字段（nickname 缺省取 username、interests 逗号拼接、status=1）后 `userMapper.insert`（`@Transactional`）。
  5. `jwtUtil.generateToken(user.getId(), username)` 签发，返回 `TokenResponse{token, expiresIn}`。
- **关键类与方法**：`UserServiceImpl.register`、`AuthController.register`、`JwtUtil.generateToken`、`PasswordEncoderConfig`、`RegisterRequest`。
- **数据与缓存**：`user` 表；前端注册后写 `localStorage.token`。

【moyuan_system_workflow.md · 系统工作流文档 / 4. 认证与权限域 / 4.2 登录与 JWT 签发（A2）】
【章节：系统工作流文档 / 4. 认证与权限域 / 4.2 登录与 JWT 签发（A2）】
### 4.2 登录与 JWT 签发（A2）
- **目标**：校验账号密码与禁用状态，成功后刷新登录信息并签发 JWT。
- **触发入口**：`POST /api/auth/login`（`LoginRequest{username,password}`）。
- **分步流程**：
  1. 按 username 查 `user`：无抛 `USER_NOT_FOUND(1001)`；`status!=1` 抛 `ACCOUNT_DISABLED(1005)`。
  2. `passwordEncoder.matches` 校验密码，失败抛 `PASSWORD_ERROR(1004)`。
  3. 更新 `lastLoginTime`、`lastLoginIp`（`getClientIp` 依 `X-Forwarded-For`→`Proxy-Client-IP`→`WL-Proxy-Client-IP`→`X-Real-IP`→`remoteAddr` 取值）。
  4. `JwtUtil` 签发**双 token**：access token（`jwt.expiration`，默认 24h，`type=access`，含随机 `jti`）与 refresh token（`jwt.refresh-expiration`，默认 7 天，`type=refresh`，同 `jti`）；响应经 `TokenResponse` 同时返回二者。
  5. 前端持久化两种 token，用于后续自动续期。
- **刷新与登出（#4 双 token）**：
  - `POST /api/auth/refresh`：携带 refresh token → 校验 type=refresh 且未过期 → **轮换**（旧 access/refresh 拉黑，签发新的一对）并返回。
  - `POST /api/auth/logout`：将当前 token 的 `jti` 加入黑名单，登出立即失效。
  - 黑名单实现 `TokenBlacklistService`（优先 Redis，失败降级进程内内存），key 关联 `jti`。
- **关键类与方法**：`UserServiceImpl.login/getClientIp`、`AuthController.login/refresh/logout`、`JwtUtil.generateAccessToken/generateRefreshToken`、`TokenBlacklistService.blacklist/isBlacklisted`。
- **数据与缓存**：`user.last_login_time/last_login_ip`；前端写 `localStorage`（`token`/`refreshToken`，封装为 `moyuan_token`）、admin 写 `lastAdminUsername`。

【moyuan_system_workflow.md · 系统工作流文档 / 4. 认证与权限域 / 4.3 JWT 校验与安全过滤器链认证（A3）】
【章节：系统工作流文档 / 4. 认证与权限域 / 4.3 JWT 校验与安全过滤器链认证（A3）】
### 4.3 JWT 校验与安全过滤器链认证（A3）
- **目标**：受保护请求进入 Controller 前完成 token 验签、身份装配。
- **触发入口**：任何携带 token 的 HTTP 请求（过滤器链自动触发）。
- **分步流程**：`JwtAuthenticationFilter.doFilterInternal` → `extractToken`（`Bearer ` 前缀）→ `jwtUtil.validateToken`（验签 + 过期 + **type=access**）→ `TokenBlacklistService.isBlacklisted(jti)` 黑名单校验 → `getUserId` → 查库 → `status==1` 时组装 `LoginUser` 并写 `UsernamePasswordAuthenticationToken` 到 `SecurityContextHolder`。无论成败都 `doFilter` 放行，访问权限由 `SecurityConfig` 决定。
- **关键类与方法**：`JwtAuthenticationFilter.doFilterInternal/extractToken`、`JwtUtil.validateToken/parseToken/getUserId`、`TokenBlacklistService.isBlacklisted`、`LoginUser.getAuthorities`、`SecurityConfig.filterChain`。
- **数据与缓存**：`user.id/status/role`。

【moyuan_system_workflow.md · 系统工作流文档 / 9. 诗人内容域 / 9.3 诗人认证、草稿与投稿建议（E10，合并）】
【章节：系统工作流文档 / 9. 诗人内容域 / 9.3 诗人认证、草稿与投稿建议（E10，合并）】
### 9.3 诗人认证、草稿与投稿建议（E10，合并）
- **精选诗人展示/管理触发**：`GET /api/poet-featured/random?count`、`/list`、`/{id}`；后台 CRUD。`getRandomPoetFeatured` 仅取 `status=1 且 is_show=1` 的启用项后 `Collections.shuffle` 截断；后台列表支持 keyword 按姓名/朝代筛选，并提供 `PUT /api/admin/poet-featured/{id}/show`（单个）与 `PUT /api/admin/poet-featured/batch-show`（批量）切换首页展示。
- **认证申请/审核**：`POST /api/poet-profile/apply`（落 `poet_profile`，`verifiedStatus=2` 待审，同步 `user.poet_verified=2`、`user.poet_profile_id`；他人查看屏蔽 contactInfo）；`PUT /api/admin/poet-profiles/{id}/verify` 置通过/拒绝并回写 `user.poet_verified`，记录作品/获赞数。
- **草稿投稿**：`POST /api/poet-drafts`（校验诗人存在，`section` 映射字段并快照原值，status=0 待审）；后台 `PUT /api/admin/poet-drafts/{id}/review` 通过时 `publishDraft` 按 section 把 content 写回 `poet` 对应字段。
- **投稿建议**：`POST /api/poet-suggestions`（前端 `sanitizeInput` 清洗 HTML，status=pending）；后台 `PUT /api/admin/poet-suggestions/{id}/review`。
- **待审统计**：`countByStatus` 汇总到 `GET /api/admin/audit/stats`。
- **关键类与方法**：`PoetFeaturedServiceImpl`、`PoetProfileServiceImpl.applyVerification/verifyProfile/getByUserId`、`PoetDraftServiceImpl.saveDraft/reviewDraft/publishDraft`、`PoetSuggestionServiceImpl.submitSuggestion/reviewSuggestion`。
- **数据与缓存**：`poet_featured`、`poet_profile`、`user`（poet_verified/poet_profile_id）、`poet_draft`、`poet_suggestion`。

---

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.2 登录态与用户 Store（K2）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.2 登录态与用户 Store（K2）】
### 15.2 登录态与用户 Store（K2）
- **分步流程**：`useUserStore` 初始化读 `localStorage` 中的 `token`/`refreshToken`，有则 `fetchUserInfo()`；`login/register` 写双 token 到 store + localStorage 后拉取用户；`fetchUserInfo` 读 `/users/me`，admin 写 `lastAdminUsername`，网络/5xx 指数重试≤2，401→尝试续期或 `logout()`；`logout` 清双 token 与 localStorage，并调用 `/auth/logout`（后端拉黑）。
- **关键位置**：`stores/user.ts`、`api/modules/user.ts`、`views/user/login.vue`、`router/guards.ts`。
- **数据与缓存**：localStorage `token`/`refreshToken`（双 token）、`lastAdminUsername`。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 注册与登录（account.login）】
【章节：墨渊小助手系统知识库（24 条） / 注册与登录（account.login）】
## 注册与登录（account.login）

- 分类：account
- 关键词：注册,登录,账号,密码
- 别名：登录,注册
- 前端路由：/user/login
- 说明：注册时需要填写用户名、密码和邮箱，登录使用用户名与密码。登录后可享受收藏、发布、评论等完整功能。注册成功会直接登录。
- 使用步骤：
  1. 进入登录页
  2. 没有账号时切换到注册并填写信息
  3. 登录成功后使用完整功能
- 常见问答：
  - 问：怎么注册账号  答：在注册页填写用户名、密码和邮箱即可注册，注册成功会直接登录。
  - 问：怎么登录  答：在登录页输入用户名和密码即可登录。
```
</details>

## E8（easy）报修工单从待处理到已关闭经过哪几个状态？

**回答**：报修工单从待处理到已关闭共经过**5 个状态**。

1. **待处理 (0)**：工单创建后的初始状态，等待管理员分配或处理。
2. **处理中 (1)**：管理员分配给具体人员后进入此状态。
3. **已解决 (2)**：问题被修复或确认处理后进入此状态，系统记录解决时间。
4. **拒绝 (4)**：在流转过程中若被拒绝，会进入此状态（注：部分资料提及状态 3 为已关闭，但在状态机逻辑中明确列出了 4 为拒绝）。
5. **已关闭 (3)**：工单最终关闭的状态，系统记录关闭时间。

*注：根据资料《moyuan_business_modules.md · 业务模块说明 / 18. 报修模块 / 工单状态流转》，标准流转顺序为 `待处理(0) → 处理中(1) → 已解决(2) → 已关闭(3)`；但在《moyuan_system_workflow.md · 系统工作流文档 / 13. 报修工单域 / 13.1 工单全生命周期（E7）》的数据与缓存章节中，明确指出了状态机包含 `0/1/2/3/4`，其中 **4** 对应拒绝。*

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 10. 管理模块 / 功能说明】
【章节：业务模块说明 / 10. 管理模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 10. 管理模块 / 后台菜单结构】
【章节：业务模块说明 / 10. 管理模块 / 后台菜单结构】
### 后台菜单结构

- 内容管理：诗词管理、分类管理、朝代管理、诗人管理、精选诗人管理、首页导航管理、**诗话视野管理**
- 审核管理：诗词审核、**诗人认证审核**、诗人内容审核、AI内容审核、**评论管理**
- 系统管理：操作日志、静态页面管理、**文件管理**、**缓存管理**

【moyuan_business_modules.md · 业务模块说明 / 18. 报修模块 / 功能说明】
【章节：业务模块说明 / 18. 报修模块 / 功能说明】
### 功能说明

- 用户提交报修工单（标题、描述、分类、优先级）
- 查看我的报修列表和详情
- 添加反馈评论
- 关闭工单
- 提交满意度评价（1-5星 + 评语）
- 管理端：工单管理、分配处理人、状态流转、内部备注、统计

【moyuan_business_modules.md · 业务模块说明 / 18. 报修模块 / 工单状态流转】
【章节：业务模块说明 / 18. 报修模块 / 工单状态流转】
### 工单状态流转

```
待处理(0) → 处理中(1) → 已解决(2) → 已关闭(3)
```

【moyuan_business_modules.md · 业务模块说明 / 18. 报修模块 / API 接口】
【章节：业务模块说明 / 18. 报修模块 / API 接口】
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

【moyuan_business_modules.md · 业务模块说明 / 18. 报修模块 / 前端路由】
【章节：业务模块说明 / 18. 报修模块 / 前端路由】
### 前端路由

| 路径 | 说明 |
|------|------|
| `/repair` | 我的报修列表 |
| `/repair/create` | 提交报修 |
| `/repair/:id` | 报修详情 |
| `/admin/repairs` | 管理端报修管理 |

【moyuan_business_modules.md · 业务模块说明 / 18. 报修模块 / 数据库表】
【章节：业务模块说明 / 18. 报修模块 / 数据库表】
### 数据库表

- `repair_order`：报修工单表
- `repair_comment`：报修评论表

---

【moyuan_system_workflow.md · 系统工作流文档 / 13. 报修工单域 / 13.1 工单全生命周期（E7）】
【章节：系统工作流文档 / 13. 报修工单域 / 13.1 工单全生命周期（E7）】
### 13.1 工单全生命周期（E7）
- **目标**：用户报修 → 管理员分配/处理/关闭 → 满意度评价。
- **触发入口**：用户 `POST /api/repair/orders`、`GET /api/repair/orders`、`.../{id}`、`.../{id}/comments`、`PUT .../close`、`PUT .../satisfaction`；管理 `GET/PUT /api/admin/repairs…`、`/assign`、评论等。
- **分步流程**：
  1. **创建**：`generateOrderNo`=`RO`+yyyyMMddHHmmssSSS+随机3位；status=0 待处理、priority 默认 2。
  2. **派单**：设置 assigneeId，待处理→处理中(status=1)。
  3. **流转** `updateStatus`：status=2 记 resolveTime，3 记 closeTime，4 拒绝。
  4. **沟通**：`addComment` 写 `repair_comment`（用户评论 isInternal=0；管理端内部备注 isInternal=1，用户侧查询过滤）。
  5. **关闭/评价**：`closeOrder` 置 3（拒绝重复关闭）；`submitSatisfaction` 写满意度及评语。详情/列表校验归属权限。
  6. **统计** `getStats` 按状态计数 total/pending(0)/processing(1)/resolved(2)/closed(3)/rejected(4)。
- **关键类与方法**：`RepairOrderServiceImpl.generateOrderNo/createOrder/assignOrder/updateStatus/closeOrder/submitSatisfaction/getStats`、`RepairController.*`、`AdminController.listRepairs/updateRepairStatus/assignRepair/getRepairStats`。
- **数据与缓存**：`repair_order`（status 状态机 0/1/2/3/4、order_no 唯一）、`repair_comment`（is_internal）。

---

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 报修中心（page.repair）】
【章节：墨渊小助手系统知识库（24 条） / 报修中心（page.repair）】
## 报修中心（page.repair）

- 分类：page
- 关键词：报修,反馈,工单,故障,问题反馈
- 别名：报修,反馈中心
- 前端路由：/repair
- 说明：报修中心用于提交报修工单，可填写标题、描述、分类与优先级，提交后查看我的报修列表与详情、添加反馈评论、关闭工单并提交满意度评价。需要登录后进入。
- 使用步骤：
  1. 登录后进入报修中心
  2. 点击提交报修并填写工单信息
  3. 在列表中查看工单详情与进度
  4. 添加评论、关闭工单并评价
- 常见问答：
  - 问：怎么报修  答：登录后进入报修中心，点击提交报修，填写标题、描述、分类与优先级后提交。
```
</details>

## E9（easy）诗云星空页面是什么，能做什么交互？

**回答**：诗云星空页面（`/poetry-cloud`）是墨渊平台利用 Three.js 引擎构建的 3D 古诗词数据可视化界面，将中国古诗词以“诗人—诗作”两级层级星图形式呈现。每位诗人是一颗发光的大节点，其诗作则是环绕分布的可交互小球，通过贝塞尔曲线连接同朝代相邻诗人，并支持像素级线宽渲染与高亮切换（来源：moyuan_business_modules.md·业务模块说明 / 23. 诗云模块 / 功能说明）。

该页面支持以下核心交互功能：
1. **相机漫游**：鼠标拖动旋转、滚轮缩放、右键平移，点击星点可飞向目标（来源：moyuan_business_modules.md·业务模块说明 / 23. 诗云模块 / 功能说明）。
2. **节点交互**：悬停显示 DOM 提示框（非 Sprite），点击诗人星滑出信息面板并聚焦其诗作簇，点击诗作小点弹出详情浮层（来源：moyuan_business_modules.md·业务模块说明 / 23. 诗云模块 / 功能说明）。
3. **虚空取诗**：点击星空空白处随机浮现一首诗，生成蓝色“虚空新星”并飞向该位置（来源：moyuan_business_modules.md·业务模块说明 / 23. 诗云模块 / 功能说明）。
4. **配色实验室**：底部控制模块可切换 7 套配色方案（琥珀星海/青玉/珊瑚/冰川蓝/梅子紫/苔绿/赤陶），调节光晕强度、连线可见度、节点尺寸及整体亮度，支持“显示诗人姓名”与“诗人按朝代配色”开关（来源：moyuan_business_modules.md·业务模块说明 / 23. 诗云模块 / 功能说明）。
5. **

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 模块总览】
【章节：业务模块说明 / 模块总览】
| 诗词外部获取内容的缓存管理 | 前端+后端已实现 |
| 22 | 联系我们模块 | 用户反馈表单页面 | 前端已实现 |
| 23 | 诗云模块 | Three.js 3D 星空可视化，诗人—诗作两级层级星图 + 配色实验室 | 前端已实现 |
| 24 | 游戏模块 | 飞花令 / 诗词接龙对局（模式/难度、回合计时、三级真实性校验、游客模式） | 前端+后端已实现 |
| 25 | AI 对话页「诗宝阁」 | 角色化统一对话入口：6 角色 × 12 能力，覆盖命题作诗/集句/藏头/润色/平仄校验/韵脚查询/对对联/意象典故/诗话点评/飞花令/接龙/群贤论诗；其中飞花令为设令字后逐句判定（含字/判重/真实性）的对局流程 | 前端+后端已实现（SSE 流式、多轮上下文、服务端会话） |
| 26 | 全局 AI 小窗与系统知识库 | 全站悬浮 AI 问答入口：诗词助理 / 网页用法助理双助理，接入 system_knowledge 知识库 | 前端+后端已实现 |

---

【moyuan_business_modules.md · 业务模块说明 / 23. 诗云模块 / 功能说明】
【章节：业务模块说明 / 23. 诗云模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 23. 诗云模块 / 技术方案】
【章节：业务模块说明 / 23. 诗云模块 / 技术方案】
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

【moyuan_business_modules.md · 业务模块说明 / 23. 诗云模块 / 前端组件】
【章节：业务模块说明 / 23. 诗云模块 / 前端组件】
### 前端组件

| 组件 | 说明 |
|------|------|
| poetry-cloud/index.vue | 诗云主页面（Three.js 3D 渲染、数据加载、交互控制、悬浮提示） |

【moyuan_business_modules.md · 业务模块说明 / 23. 诗云模块 / API 接口】
【章节：业务模块说明 / 23. 诗云模块 / API 接口】
### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/dynasties | 获取朝代列表 | backend DynastyController |
| GET /api/poets | 获取诗人列表（pageSize 300，用于 3D 星系） | sc-moyuan-backend PoetController |
| GET /api/poems | 获取诗词列表（pageSize 1000，用于诗作节点与计数） | backend PoemController |

【moyuan_business_modules.md · 业务模块说明 / 23. 诗云模块 / 前端路由】
【章节：业务模块说明 / 23. 诗云模块 / 前端路由】
### 前端路由

| 路径 | 说明 |
|------|------|
| `/poetry-cloud` | 诗云星空页面（hideHeader） |

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.5 首页数据组装（K5）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.5 首页数据组装（K5）】
### 15.5 首页数据组装（K5）
- **轮播图**：静态 `/img/lb_*.jpg`、`/img/lb_shiwen_*.png`，无后端。
- **今日诗词**：`DailyPoetry` → 后端 `GET /api/external-poetry/jinrishici` 代理（`v2.jinrishici.com/one.json`，后端 60 分钟进程内缓存，前端不再直连）。
- **首页导航**：`GET /home-navigation`。
- **精选诗人**：`getRandomPoetFeatured(8)`，去重取 4 位，每 20s 轮换。
- **论坛预览**：`GET /forum/posts?pageSize=5`。
- **古诗精赏**：本地 json + `GET /search/poems` + `/search/poems/recommended`。
- **当代精选**：本地 json + like/favorite/comment 接口。
- **AI 区块**：`/ai/write-poem`、`/ai/analyze`、`/ai/couplet`、`/ai/ocr`、`/ai/chat`（藏头诗、平仄格律校验，复用 `POST /api/ai/chat` 并传 `moduleCode=chat`，不新增后端接口）、`/rhyme/*`；"诗海拾贝"本地 json。
- **小游戏区块**：首页 AI 小工具区「飞花令」「诗词接龙」两张卡片——游客点击展开内嵌小窗口（`GameMiniPanel`）直接开局，登录用户跳转 `/game/feihua`、`/game/chain`；后端为独立游戏模块（`/api/game/feihua`、`/api/game/chain`，见 `modules.md` 第 24 章）。
- **诗话视野**：`GET /vision/articles`。
- **诗词卡片跳转**：首页「古诗精赏 / 当代精选」卡片点击经组合式函数 `composables/usePoemCardNavigation.ts` 处理——本地命中直接 `router.push('/poem/{id}')`，未命中回退 `PoetryDetailDialog`（外部诗词弹窗，可由接口盒子实时增强）。
- **关键位置**：`views/home/index.vue`、`components/{home,business}/*`、`composables/usePoemCardNavigation.ts`、`api/modules/*`。

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.6 诗云 3D 星空（K6）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.6 诗云 3D 星空（K6）】
### 15.6 诗云 3D 星空（K6）
- **数据加载**：`Promise.all` 并发 `GET /dynasties`、`/poets(pageSize 300)`、`/poems(pageSize 1000)`，失败回退内置 12 朝代。
- **渲染**：`initScene`（Scene+FogExp2+Camera+WebGLRenderer(ACESFilmicToneMapping)+OrbitControls）→ 3200 背景星（Sphere+ShaderMaterial）→ 诗人星（发光小球+光晕，缓存 `poetPositionMap`）+ 同朝代贝塞尔赠诗线 → 诗作独立 Mesh 以「诗人为中心、诗作环绕分布」的球面螺旋生成并经松弛迭代拉开间距，按诗人分组的 LineSegments2 连线 → 交互：hover 高亮并显示 DOM 提示框、点击 `selectPoet→flyToStar+loadPoetPoems+focusPoetPoems`、点击诗作 `selectPoem`、朝代筛选重建。
- **配色实验室**：底部控制模块由 `reactive(viz)` 驱动（7 套方案 + 光晕/连线/尺寸/亮度滑杆 + 标签与朝代配色开关），`applyVizStyles()` 统一重涂背景/雾效、诗人节点、诗作节点与两类连线；整体亮度经画布容器 CSS `filter: brightness()`。
- **诗人姓名标注**：DOM 层 `.label-layer` + `updatePoetLabels()`，每帧 `getWorldPosition().project(camera)` 更新标签屏幕坐标，相机背后/视口外自动隐藏。
- **卸载**：`onUnmounted` 释放 geometry/material、清理标签与定时器、`scene.clear()`、`renderer.dispose()`。
- **数据与缓存**：数据仅内存 ref，不做 localStorage；`poetPositionMap` 内存缓存 3D 坐标，`poemCountMap` 缓存每位诗人的诗作数（用于节点尺寸）。

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.7 路由守卫（K7）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.7 路由守卫（K7）】
### 15.7 路由守卫（K7）
- **分步流程**：`beforeEach` 按 `meta.title` 设 document.title → `requiresAuth` 未登录跳 `Login`（带 redirect）；已登录缺 userInfo 先 `fetchUserInfo`；`requiresAdmin` 校验 role !== admin 弹错回 Home；已登录访问 Login/Register 回 Home。`afterEach` 维护 `appStore.cachedViews`（keep-alive）。
- **关键位置**：`router/guards.ts`、`router/index.ts`（`meta`: requiresAuth/requiresAdmin/keepAlive）、`stores/app.ts`。

---

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 诗云星空（play.poetry-cloud）】
【章节：墨渊小助手系统知识库（24 条） / 诗云星空（play.poetry-cloud）】
## 诗云星空（play.poetry-cloud）

- 分类：play
- 关键词：诗云,星空,3D,诗词星图,可视化
- 别名：诗云,诗云星空图
- 前端路由：/poetry-cloud
- 说明：诗云是用 3D 星空展示古诗数据的可视化页面，每位诗人是一颗星，其诗作环绕分布，可缩放旋转浏览。点击诗人星可查看诗人信息与代表作品，点击诗作节点可查看诗词详情。页面底部「配色实验室」可切换配色方案并调节光晕、连线、亮度等显示效果。
- 使用步骤：
  1. 进入诗云页面
  2. 拖动旋转、滚轮缩放浏览星图
  3. 点击诗人星查看诗人信息与作品
  4. 点击诗作节点查看诗词详情
- 常见问答：
  - 问：诗云是什么  答：诗云是把古诗词数据做成 3D 星空的页面，诗人是星、诗作围绕其分布，可交互浏览。
  - 问：诗云怎么用  答：拖动旋转、滚轮缩放浏览星图，点击诗人或诗作节点查看详情。
```
</details>

## E10（easy）哪些 AI 功能必须登录后才能用？

**回答**：必须登录才能使用的 AI 功能包括：看图写诗、AI 评分、诗宝阁对话以及平仄格律校验（来源：moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条）/ AI 功能需要登录（account.ai-login））。

古籍 OCR 识文虽属 AI 功能，但当前处于维护中，未登录用户无法使用；飞花令、诗词接龙等小工具游客可直接体验。

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 7. AI 模块 / 功能说明】
【章节：业务模块说明 / 7. AI 模块 / 功能说明】
### 功能说明

- AI诗词问答（支持多模型切换；调用优先走 `ai_module_config` 模块绑定，未绑定某模块时回退默认模型。可用提供商：智谱AI、DeepSeek、Kimi、千问、小米MiMo、OpenRouter；NVIDIA NIM 已全部禁用/下线）
- 看图写诗（上传图片，AI生成古诗词，支持视觉模型如 GLM-4V-Flash、DeepSeek-Flash）
- 智能分析（输入诗句进行深度解析）
- AI对对联（输入上联，AI生成下联，遵循平仄规则）
- **藏头诗**（输入 2-8 个藏头字并选择五言/七言，AI 生成藏头诗，每句首字依次对应藏头字；复用 `POST /api/ai/chat`，moduleCode=chat，不新增后端接口）
- **平仄格律校验**（粘贴一首绝句/律诗，由**本地确定性引擎** `POST /api/ai/tone-check` 逐句标注平仄（平声绿底、仄声红底）、判定是否合律并给出不合律原因；校验规则为句末三平尾/三仄尾、二四六位失替、偶数句押韵，不调用大模型、不消耗 AI 额度）
- **AI 对话页「诗宝阁」**（`/ai-chat`）：角色化统一对话入口，6 角色（诗仙·创作 / 诗话·鉴赏 / 格律·校正 / 诗友·行令 / 群贤·论诗 / 顾问·问答）× 12 能力（命题作诗 / 集句成诗 / 藏头诗 / 润色入律 / 平仄校验 / 韵脚查询 / 对对联 / 意象典故 / 诗话点评 / 飞花令 / 诗词接龙 /

【moyuan_business_modules.md · 业务模块说明 / 7. AI 模块 / 功能说明】
【章节：业务模块说明 / 7. AI 模块 / 功能说明】
- **古籍OCR识文**（上传古诗词图片/书法作品/碑帖，AI精准识别文字内容，保留原文结构，标注印章题款）——**当前维护中**：后端开关 `ai.features.ocr-enabled=false` 时接口返回 `4293`，前端首页入口置灰并提示「维护中」，恢复需同时打开后端开关与前端 `OCR_ENABLED` 常量，并存在 `ocr` 模块的视觉模型绑定
- AI模型配置管理（后台管理，支持CRUD、启用/禁用、默认模型设置、连接测试）
- **AI内容填充与审核**：对诗词/诗人缺失字段由 AI 生成候选内容，存入审核表，管理员审核通过后写回原表
- **诗词AI助手（poetry_chat）**：寻章摘句页面的AI功能模块，包括译文注释、名人点评、创作背景、历史文化背景、AI逐句解析、AI诗词助手对话
  - **译文注释**：逐句翻译，关键词语注释
  - **名人点评**：历代名家点评（引用《诗品》《沧浪诗话》《人间词话》等）、文学史地位
  - **创作背景**：诗人生平、创作缘由、历史背景
  - **AI逐句解析**：点击诗句展开解释，支持多句同时展开，内存缓存不写库，可关闭
  - **弹窗交互**：三个功能按钮以向左展开的弹窗形式呈现，覆盖在诗词内容上方，不挤压布局
  - **搜索式加载**：AI生成过程中以搜索动画替代旋转加载图标

【moyuan_business_modules.md · 业务模块说明 / 7. AI 模块 / AI 内容填充与审核】
【章节：业务模块说明 / 7. AI 模块 / AI 内容填充与审核】
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

【moyuan_business_modules.md · 业务模块说明 / 26. 全局 AI 小窗与系统知识库模块 / 功能说明】
【章节：业务模块说明 / 26. 全局 AI 小窗与系统知识库模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 26. 全局 AI 小窗与系统知识库模块 / 后端与数据结构】
【章节：业务模块说明 / 26. 全局 AI 小窗与系统知识库模块 / 后端与数据结构】
### 后端与数据结构

- `system_knowledge` 表：知识条目（code/title/category/keywords/aliases/body/steps/faq/route/is_show/sort_order/deleted），种子 24 条
- `ai_module_config` 新增 `system_chat` 模块（绑定 DeepSeek），与 `chat` 模块完全隔离，避免污染诗宝阁、藏头诗等其他 AI 功能提示词
- SecurityConfig：`/api/system-knowledge` 匿名可访问；`/api/ai/**` 已移除匿名放行，未登录返回 401
- 公开接口：`GET /api/system-knowledge`（只读列表）
- 后台接口：`GET /api/admin/system-knowledge`、`GET /{id}`、`POST`、`PUT /{id}`、`PUT /{id}/show`、`DELETE /{id}`

【moyuan_system_workflow.md · 系统工作流文档 / 11. AI 能力域 / 11.1 AI 辅助创作与生成（E1）】
【章节：系统工作流文档 / 11. AI 能力域 / 11.1 AI 辅助创作与生成（E1）】
### 11.1 AI 辅助创作与生成（E1）
- **触发入口**：`POST /api/ai/chat|write-poem|analyze|couplet|ocr`；流式入口 `POST /api/ai/chat/stream`（SSE，`AiController.chatStream` → `AiService.streamChat` → `AiModelRegistry.streamByModule` → `OpenAiCompatibleAdapter.streamChat` 用 JDK `HttpClient` 逐行解析上游 SSE，客户端断开经 `emitter.onCompletion` 置取消信号停止消费）；另有 `POST /api/ai/tone-check`（平仄校验，走本地 `PingZeService` 确定性引擎，不调用大模型、**不计 token**，但仍计入请求次数限流）。
- **统一防护**：上述入口在进入 `AiService` 前依次经过 `AiGuardService.checkRequest`（每分钟/每日请求数与 token 配额，Redis 计数，admin 豁免）与 `AiInputValidator`（字数、消息条数、角色白名单、`maxTokens/temperature` 区间）；超限分别返回 `4291/4292` 与 `4290`。`/api/ai/ocr` 还受 `ai.features.ocr-enabled` 开关控制，关闭时返回 `4293`（当前默认关闭，前端入口置灰）。图片类入口校验非空、≤10MB、MIME 白名单。
- **分步流程**：`AiController` 分发 `AiService` → 各方法内置系统提示词常量 → 文本类 `AiModelRegistry.chat/getModel`、图片类 Base64 后 `AiModelRegistry.vision`；传 `moduleCode` 走 `chatByModule/visionByModule`（按模块选模型+覆盖提示词）→ 按 `model.provider` 定位 `AiModelAdapter`，`RestTemplate` 带 `Bearer apiKey` POST 到 `ai_model.api_url`（messages=system+user、max_tokens 默认 1024）→ 解析 `choices[0].message.content` 返回；失败抛 `BusinessException`。

【moyuan_system_workflow.md · 系统工作流文档 / 11. AI 能力域 / 11.1 AI 辅助创作与生成（E1）】
【章节：系统工作流文档 / 11. AI 能力域 / 11.1 AI 辅助创作与生成（E1）】
provider` 定位 `AiModelAdapter`，`RestTemplate` 带 `Bearer apiKey` POST 到 `ai_model.api_url`（messages=system+user、max_tokens 默认 1024）→ 解析 `choices[0].message.content` 返回；失败抛 `BusinessException`。
- **注意（代码事实）**：providers 下拉含 qwen/openrouter，但当前**无对应 Adapter**，调用报"不支持的AI提供商"；`OpenAiCompatibleAdapter` 支持 zhipu/deepseek/kimi/mimo，`NvidiaAdapter` 支持 nvidia（vision 抛"暂不支持视觉模型"）。
- **关键类与方法**：`AiController.*`、`AiServiceImpl.chat/writePoemFromImage/analyzePoem/matchCouplet/ocrImage`、`AiModelRegistry.chat/vision/chatByModule/getModel`、`OpenAiCompatibleAdapter/NvidiaAdapter`。
- **数据与缓存**：`ai_model`（name/provider/apiUrl/modelId/visionModelId/apiKey/maxTokens/isEnabled/isDefault/modelType）；内存 `AiModelRegistry.modelCache`。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / AI功能需要登录（account.ai-login）】
【章节：墨渊小助手系统知识库（24 条） / AI功能需要登录（account.ai-login）】
## AI功能需要登录（account.ai-login）

- 分类：account
- 关键词：AI功能,登录,权限,使用条件
- 别名：AI需要登录
- 说明：AI 相关功能需要登录后才能使用，包括看图写诗、AI 评分、诗宝阁对话以及平仄格律校验等；古籍识文目前处于维护中。首页的飞花令、诗词接龙小工具游客可在首页直接体验。
- 使用步骤：
  1. 登录账号
  2. 进入 AI 功能入口
  3. 正常使用 AI 能力
- 常见问答：
  - 问：AI功能要登录吗  答：需要登录后使用，包括看图写诗、AI 评分、诗宝阁对话与平仄校验等（古籍识文维护中）；游客可在首页直接体验飞花令、诗词接龙小工具。
```
</details>

## M1（medium）有人收藏了一首诗、又点赞并浏览了一次，推荐算法分别给这三种行为记几分？

**回答**：推荐算法对三种行为的计分规则如下：

1. **收藏**：记 **5 分**。
2. **点赞**：记 **3 分**。
3. **浏览**：记 **1 分**。

（来源：moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 推荐算法说明；moyuan_system_workflow.md · 系统工作流文档 / 9. 诗人内容域 / 9.2 推荐 / 热门诗人（C7））

**补充说明：**
- 在诗词侧，同一用户对同一作品的多行为取**最大值合并，不累加**。
- 在诗人侧，计算热门诗人时，诗收藏 5 分 + 词收藏 5 分 + 赞 3 分 + 浏览 1 分。

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 功能说明】
【章节：业务模块说明 / 8. 搜索模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 复合搜索策略】
【章节：业务模块说明 / 8. 搜索模块 / 复合搜索策略】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 搜索评分算法】
【章节：业务模块说明 / 8. 搜索模块 / 搜索评分算法】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 智能搜索响应】
【章节：业务模块说明 / 8. 搜索模块 / 智能搜索响应】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 推荐算法说明】
【章节：业务模块说明 / 8. 搜索模块 / 推荐算法说明】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 前端组件】
【章节：业务模块说明 / 8. 搜索模块 / 前端组件】
### 前端组件

| 组件 | 说明 |
|------|------|
| poem/list.vue - 筛选区域 | 朝代/分类/诗人/排序筛选器，支持复合搜索 |
| home/index.vue - 历史的印痕模块 | 首页搜索框、搜索结果展示、为您推荐标签页 |
| 搜索结果卡片 | 显示诗词信息、来源标签、推荐理由 |
| components/admin/RecommendationScreen.vue | 后台控制台推荐指标屏：离线四组指标 + 实时缓存/切换看板 |

【moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / 评分类型】
【章节：业务模块说明 / 16. 诗词评分模块 / 评分类型】
### 评分类型

| 类型 | 说明 |
|------|------|
| 用户评分 | 用户手动评分，可附带评语 |
| AI评分 | AI自动评分，附带AI分析内容 |

【moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.3 收藏 / 点赞（C3）】
【章节：系统工作流文档 / 8. 诗词内容域 / 8.3 收藏 / 点赞（C3）】
### 8.3 收藏 / 点赞（C3）
- **触发入口**：`POST|GET /api/poems/{id}/like`、`POST|GET /api/poems/{id}/favorite`、`GET /api/poems/favorites`。
- **分步流程**：点赞 `toggleLike`（`@Transactional`）按 `user_id+target_id+target_type=POEM` 计数 `user_like`：存在则删记录且 `like_count-1`（`gt 0` 保护），否则插入并 `+1`；状态查询 `isLike/isFavorite` 计数判定；`getFavorites` 取收藏 ID 集后 `in` 查 poem 并填充。
- **注意（代码事实）**：收藏 `UserServiceImpl.toggleFavorite` **只写 `user_favorite` 表，不维护 `poem.favorite_count`**；点赞会维护 `poem.like_count`。
- **数据与缓存**：`user_like`、`user_favorite`、`poem.like_count`。

【moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.4 诗词评分（C4）】
【章节：系统工作流文档 / 8. 诗词内容域 / 8.4 诗词评分（C4）】
### 8.4 诗词评分（C4）
- **触发入口**：`GET /api/poems/{poemId}/ratings`；`POST .../ratings?score&comment`；`POST .../ratings/ai`、`GET/regenerate`；`GET .../ratings/me`。
- **分步流程**：校验 score∈[1.0,5.0] → `ratePoem`（`@Transactional`）按 `poem_id+user_id+rating_type=1` 增改评分 → `updatePoemAverageScore` 重算 `avg_score/rating_count`；AI 评分 `rating_type=2`（userId=null）：`aiService.analyzePoem(content, null, "rating")` 走 `ai_module_config` 的独立 `rating` 模块绑定（不再写死默认模型，可在后台「AI功能配置」页面调整）→ 正则解析分（失败兜底 3.5）→ 摘要取前 200 字、去 markdown 标记，随后重算均分，`ai_model` 字段记录实际生效模型名。`getPoemRatings` 返回 userRatings(type=1)/aiRating(type=2 最新)/averageScore/ratingCount，异常回退空数据。
- **关键类与方法**：`PoemRatingServiceImpl.ratePoem/requestAiRating/parseAiScore/generateSummary/updatePoemAverageScore`、`AiService.analyzePoem`。`PoemRatingController.*`。
- **数据与缓存**：`poem_rating`（score/comment/rating_type:1用户2AI/ai_model/ai_summary/ai_analysis）、`poem.avg_score/rating_count`。

【moyuan_system_workflow.md · 系统工作流文档 / 9. 诗人内容域 / 9.2 推荐 / 热门诗人（C7）】
【章节：系统工作流文档 / 9. 诗人内容域 / 9.2 推荐 / 热门诗人（C7）】
### 9.2 推荐 / 热门诗人（C7）
- **触发入口**：`GET /api/poets/recommend?limit=6`（上限 20）、`GET /api/poets/popular?limit=10`（上限 50）；诗词侧 `GET /api/search/poems/recommended|popular`。
- **分步流程**：取当前用户（未登录走热门）→ `userId==null` 走 `getPopularPoets`；`buildUserPoetMatrix` 由收藏/点赞/浏览行为构建 用户→诗人:{权重}（收藏 5、点赞 3、浏览 1）→ `calculateItemCollaborativeScores` 计算物品-物品余弦相似度，每个已交互诗人取 Top50 相似诗人、对未交互诗人累加 `相似度×权重`（诗词侧 `buildUserPoemMatrix` 同一作品多行为取最大值） → `getInterestBasedPoetScores` 按 `interests` 朝代加权（古典/现代/外国）→ `mergeScores` 相加取 TopN → 过滤 status=1；空回退热门。热门 `calculatePoetPopularity`（诗收藏5+词收藏5+赞3+浏览1）取 TopN。结果 `@Cacheable` 缓存。
- **关键类与方法**：`PoetController.getRecommendedPoets/getPopularPoets`、`RecommendationServiceImpl.*`、`SearchController.getRecommendedPoems/getPopularPoems`。
- **数据与缓存**：`user_favorite/user_like/user_history`、`user.interests`、`poem.view_count/like_count`；缓存 `poetRecommendation::user:{userId}:{limit}`、`popular:{limit}` 等。
```
</details>

## M2（medium）用户提交评分后，诗词的平均分是怎么更新的？AI 评分解析失败时会怎么办？

**回答**：用户提交评分后，系统会重新计算平均分；若 AI 评分解析失败，将兜底返回 3.5 分。

1. **平均分更新机制**：当用户提交评分（`POST /api/poems/{id}/ratings`）或请求 AI 评分时，系统会调用 `updatePoemAverageScore` 方法，基于最新的评分数据重新计算该诗词的 `avg_score`（平均分）和 `rating_count`（评分数量），并更新至数据库中的 `poem` 表。
2. **AI 解析失败处理**：在 AI 评分流程中，若正则解析分数失败，系统会执行兜底逻辑，直接返回固定分数 **3.5** 分，确保评分数据始终有效且不影响后续统计计算。

（来源：moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / 功能说明；moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.4 诗词评分（C4））

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 搜索评分算法】
【章节：业务模块说明 / 8. 搜索模块 / 搜索评分算法】
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

【moyuan_business_modules.md · 业务模块说明 / 10. 管理模块 / 功能说明】
【章节：业务模块说明 / 10. 管理模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / 功能说明】
【章节：业务模块说明 / 16. 诗词评分模块 / 功能说明】
### 功能说明

- 用户评分：用户对诗词进行评分（1.0-5.0分）
- AI评分：请求AI对诗词进行评分和分析
- 评分维度：支持多维度评分（格律、意境、用词、情感、创新）
- 评分统计：诗词平均评分和评分数量
- 评分详情：展示用户评分和AI评分详情
- 重新评价：用户已评分后可点击"重新评价"修改评分

【moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / 评分类型】
【章节：业务模块说明 / 16. 诗词评分模块 / 评分类型】
### 评分类型

| 类型 | 说明 |
|------|------|
| 用户评分 | 用户手动评分，可附带评语 |
| AI评分 | AI自动评分，附带AI分析内容 |

【moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / 评分维度】
【章节：业务模块说明 / 16. 诗词评分模块 / 评分维度】
### 评分维度

| 维度 | 说明 |
|------|------|
| 格律 | 诗词的格律规范性 |
| 意境 | 诗词的意境表达 |
| 用词 | 诗词的用词精准度 |
| 情感 | 诗词的情感表达 |
| 创新 | 诗词的创新程度 |

【moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / API 接口】
【章节：业务模块说明 / 16. 诗词评分模块 / API 接口】
### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/poems/{id}/ratings | 获取诗词评分详情 | sc-moyuan-backend PoemRatingController |
| POST /api/poems/{id}/ratings | 用户评分 | sc-moyuan-backend PoemRatingController |
| POST /api/poems/{id}/ratings/ai | 请求AI评分 | sc-moyuan-backend PoemRatingController |
| GET /api/poems/{id}/ratings/ai | 获取AI评分 | sc-moyuan-backend PoemRatingController |
| GET /api/poems/{id}/ratings/me | 获取当前用户评分 | sc-moyuan-backend PoemRatingController |

【moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.4 诗词评分（C4）】
【章节：系统工作流文档 / 8. 诗词内容域 / 8.4 诗词评分（C4）】
### 8.4 诗词评分（C4）
- **触发入口**：`GET /api/poems/{poemId}/ratings`；`POST .../ratings?score&comment`；`POST .../ratings/ai`、`GET/regenerate`；`GET .../ratings/me`。
- **分步流程**：校验 score∈[1.0,5.0] → `ratePoem`（`@Transactional`）按 `poem_id+user_id+rating_type=1` 增改评分 → `updatePoemAverageScore` 重算 `avg_score/rating_count`；AI 评分 `rating_type=2`（userId=null）：`aiService.analyzePoem(content, null, "rating")` 走 `ai_module_config` 的独立 `rating` 模块绑定（不再写死默认模型，可在后台「AI功能配置」页面调整）→ 正则解析分（失败兜底 3.5）→ 摘要取前 200 字、去 markdown 标记，随后重算均分，`ai_model` 字段记录实际生效模型名。`getPoemRatings` 返回 userRatings(type=1)/aiRating(type=2 最新)/averageScore/ratingCount，异常回退空数据。
- **关键类与方法**：`PoemRatingServiceImpl.ratePoem/requestAiRating/parseAiScore/generateSummary/updatePoemAverageScore`、`AiService.analyzePoem`。`PoemRatingController.*`。
- **数据与缓存**：`poem_rating`（score/comment/rating_type:1用户2AI/ai_model/ai_summary/ai_analysis）、`poem.avg_score/rating_count`。

【moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.5 外部诗词导入（C5）】
【章节：系统工作流文档 / 8. 诗词内容域 / 8.5 外部诗词导入（C5）】
### 8.5 外部诗词导入（C5）
- **触发入口**：`POST /api/poems/import-external`（title/content/author/dynasty）；`POST /api/poems/fix-external-poems`。
- **分步流程**：校验 title → 按 dynasty 名称查朝代 → 按 author 查诗人（无则新建 Poet 并 `clearAllCache`）→ 按 `title+poetId` 查重：命中补全空缺字段返回 `{id,imported:false}`，未命中落库（`source=external`、`poemType=ancient`、`isOriginal=0`、`auditStatus=1`、status=1）并清 `poems` 缓存返回 `{id,imported:true}`。修复接口取 `source=external` 且 `poet_id is null` 前 100 条，调 `https://cn.apihz.cn/api/zici/poetry.php` 补全作者朝代，成功后清 `poets`/`poems` 缓存返回 `{total,fixed}`。
- **关键类与方法**：`PoemController.importExternalPoem/fixExternalPoems`、`PoemServiceImpl`、`PoetDefaultAvatar.getAvatar`。
- **数据与缓存**：`poem.source/is_original/poem_type/audit_status`；清缓存 key：`poems`、`poets`。

---

【moyuan_system_workflow.md · 系统工作流文档 / 17. 关键代码速查表】
【章节：系统工作流文档 / 17. 关键代码速查表】
4（诗词 AI 评分改为独立 `rating` 模块：`PoemRatingServiceImpl.requestAiRating` 由写死默认模型改为 `aiService.analyzePoem(content, null, "rating")`，`ai_model` 字段记录实际生效模型名；后台新增「AI功能配置」页面（`/admin/ai-config`）可按模块调整模型与提示词；此前 v1.3 补充流式问答入口 `POST /api/ai/chat/stream` 调用链，v1.2 新增首页小游戏区块工作流）
**维护人员**：墨渊开发团队
**维护说明**：本文为 docs 本地维护文件，受 `.git/info/exclude` 排除不提交远程；代码变更后请依据 doc-manager 技能核对本文与实现一致性。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 诗词AI评分（play.rating）】
【章节：墨渊小助手系统知识库（24 条） / 诗词AI评分（play.rating）】
## 诗词AI评分（play.rating）

- 分类：play
- 关键词：AI评分,诗词评分,评分,打分
- 别名：AI打分,诗词打分
- 前端路由：/poem/:id
- 说明：诗词详情页提供 AI 评分，由 AI 从意境、格律、修辞、情感、用词等维度评价诗词并给出分数。进入任意诗词详情页即可查看或请求 AI 评分，用户本人也可以手动给诗词评分。
- 使用步骤：
  1. 打开任意诗词详情页
  2. 查看已有评分或请求 AI 评分
  3. 查看 AI 给出的评价与分数
- 常见问答：
  - 问：AI评分在哪里  答：在诗词详情页可以查看或请求 AI 评分，AI 会多维度评价并给出分数。
```
</details>

## M3（medium）在诗汇论坛发帖、评论、点赞分别要满足什么条件？涉及哪些接口？帖子列表怎么展示置顶与精华？

**回答**：在诗汇论坛发帖、评论、点赞需满足以下条件及涉及接口如下：

**1. 发帖条件与接口**
- **条件**：用户必须完成登录（注册支持兴趣选项用于协同过滤推荐）。
- **接口**：`POST /api/auth/register`（注册）、`POST /api/auth/login`（登录）；发布帖子具体接口资料未收录，仅知后端模块为 `sc-moyuan-backend AuthController` 或论坛模块相关逻辑。

**2. 评论与点赞条件与接口**
- **条件**：必须拥有账号且已登录；论坛支持多级回复。
- **接口**：资料中未收录具体的评论和点赞 API 接口路径及参数，仅说明功能存在（来源：moyuan_business_modules.md·业务模块说明/5. 论坛模块/功能说明）。

**3. 帖子列表展示置顶与精华**
- **展示方式**：帖子列表支持“置顶帖”与“精华帖推荐”功能。
- **机制**：资料中未收录具体的前端渲染逻辑或后端筛选接口路径，仅确认该功能属于论坛模块（来源：moyuan_business_modules.md·业务模块说明/5. 论坛模块/功能说明）。

注：知识库未收录发帖、评论、点赞的具体 API 路径及置顶/精华的筛选接口参数。

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 5. 论坛模块 / 功能说明】
【章节：业务模块说明 / 5. 论坛模块 / 功能说明】
### 功能说明

- 帖子列表浏览
- 帖子发布（富文本编辑）
- 帖子详情查看
- 评论功能（支持多级回复）
- 点赞功能
- 精华帖推荐
- 置顶帖功能

【moyuan_business_modules.md · 业务模块说明 / 9. 用户模块 / 功能说明】
【章节：业务模块说明 / 9. 用户模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 9. 用户模块 / API 接口】
【章节：业务模块说明 / 9. 用户模块 / API 接口】
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

【moyuan_business_modules.md · 业务模块说明 / 9. 用户模块 / 用户主页功能】
【章节：业务模块说明 / 9. 用户模块 / 用户主页功能】
### 用户主页功能

用户主页展示以下信息：
- **用户基本信息**：头像、昵称、简介、性别、生日
- **诗人认证状态**：是否已认证、认证诗人资料
- **统计数据**：作品数、获赞数、被收藏数、粉丝数
- **作品列表**：用户发布的诗词和帖子

【moyuan_business_modules.md · 业务模块说明 / 9. 用户模块 / 前端路由】
【章节：业务模块说明 / 9. 用户模块 / 前端路由】
### 前端路由

| 路径 | 说明 |
|------|------|
| `/user/login` | 登录页 |
| `/user/register` | 注册页 |
| `/user/profile` | 个人中心（含诗人认证申请入口） |
| `/user/:id/homepage` | 用户主页（新增） |

---

【moyuan_business_modules.md · 业务模块说明 / 10. 管理模块 / 功能说明】
【章节：业务模块说明 / 10. 管理模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 10. 管理模块 / 后台菜单结构】
【章节：业务模块说明 / 10. 管理模块 / 后台菜单结构】
### 后台菜单结构

- 内容管理：诗词管理、分类管理、朝代管理、诗人管理、精选诗人管理、首页导航管理、**诗话视野管理**
- 审核管理：诗词审核、**诗人认证审核**、诗人内容审核、AI内容审核、**评论管理**
- 系统管理：操作日志、静态页面管理、**文件管理**、**缓存管理**

【moyuan_business_modules.md · 业务模块说明 / 14. 精选诗人模块 / 功能说明】
【章节：业务模块说明 / 14. 精选诗人模块 / 功能说明】
### 功能说明

- 精选诗人展示（首页卡片展示，仅展示 `is_show=1` 的诗人）
- 精选诗人详情查看
- 精选诗人管理（后台管理，支持按姓名/朝代筛选、单个/批量设置首页展示开关）

【moyuan_business_modules.md · 业务模块说明 / 14. 精选诗人模块 / API 接口】
【章节：业务模块说明 / 14. 精选诗人模块 / API 接口】
### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/poet-featured/random | 随机获取精选诗人 | sc-moyuan-backend PoetFeaturedController |
| GET /api/poet-featured/list | 获取精选诗人列表 | sc-moyuan-backend PoetFeaturedController |
| GET /api/poet-featured/{id} | 获取精选诗人详情 | sc-moyuan-backend PoetFeaturedController |

【moyuan_system_workflow.md · 系统工作流文档 / 9. 诗人内容域 / 9.3 诗人认证、草稿与投稿建议（E10，合并）】
【章节：系统工作流文档 / 9. 诗人内容域 / 9.3 诗人认证、草稿与投稿建议（E10，合并）】
### 9.3 诗人认证、草稿与投稿建议（E10，合并）
- **精选诗人展示/管理触发**：`GET /api/poet-featured/random?count`、`/list`、`/{id}`；后台 CRUD。`getRandomPoetFeatured` 仅取 `status=1 且 is_show=1` 的启用项后 `Collections.shuffle` 截断；后台列表支持 keyword 按姓名/朝代筛选，并提供 `PUT /api/admin/poet-featured/{id}/show`（单个）与 `PUT /api/admin/poet-featured/batch-show`（批量）切换首页展示。
- **认证申请/审核**：`POST /api/poet-profile/apply`（落 `poet_profile`，`verifiedStatus=2` 待审，同步 `user.poet_verified=2`、`user.poet_profile_id`；他人查看屏蔽 contactInfo）；`PUT /api/admin/poet-profiles/{id}/verify` 置通过/拒绝并回写 `user.poet_verified`，记录作品/获赞数。
- **草稿投稿**：`POST /api/poet-drafts`（校验诗人存在，`section` 映射字段并快照原值，status=0 待审）；后台 `PUT /api/admin/poet-drafts/{id}/review` 通过时 `publishDraft` 按 section 把 content 写回 `poet` 对应字段。
- **投稿建议**：`POST /api/poet-suggestions`（前端 `sanitizeInput` 清洗 HTML，status=pending）；后台 `PUT /api/admin/poet-suggestions/{id}/review`。
- **待审统计**：`countByStatus` 汇总到 `GET /api/admin/audit/stats`。
- **关键类与方法**：`PoetFeaturedServiceImpl`、`PoetProfileServiceImpl.applyVerification/verifyProfile/getByUserId`、`PoetDraftServiceImpl.saveDraft/reviewDraft/publishDraft`、`PoetSuggestionServiceImpl.submitSuggestion/reviewSuggestion`。
- **数据与缓存**：`poet_featured`、`poet_profile`、`user`（poet_verified/poet_profile_id）、`poet_draft`、`poet_suggestion`。

---

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 诗汇论坛（page.forum）】
【章节：墨渊小助手系统知识库（24 条） / 诗汇论坛（page.forum）】
## 诗汇论坛（page.forum）

- 分类：page
- 关键词：论坛,诗汇论坛,发帖,讨论,交流
- 别名：论坛
- 前端路由：/forum
- 说明：诗汇论坛是诗词交流社区，登录后可发布新帖，浏览帖子、评论与点赞。帖子列表支持置顶与精华展示，帖子详情页可查看正文与多级评论。
- 使用步骤：
  1. 进入诗汇论坛
  2. 浏览帖子列表或搜索
  3. 登录后点击发布新帖
  4. 在帖子详情中评论、点赞
- 常见问答：
  - 问：论坛在哪里  答：在顶部导航进入诗汇论坛，可浏览帖子、发帖、评论与点赞。
  - 问：论坛怎么发帖  答：登录后进入诗汇论坛，点击发布新帖，编辑标题与内容后提交。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 评论与互动（feature.comment）】
【章节：墨渊小助手系统知识库（24 条） / 评论与互动（feature.comment）】
## 评论与互动（feature.comment）

- 分类：feature
- 关键词：评论,点赞,互动,回复
- 别名：评论,互动
- 前端路由：/forum
- 说明：在诗词、帖子与文章详情页可以对内容点赞与评论，论坛评论支持多级回复。评论需要有账号登录。
- 使用步骤：
  1. 打开诗词、帖子或文章详情
  2. 点击点赞或发表评论
  3. 在论坛可对评论进行回复
- 常见问答：
  - 问：怎么评论  答：在诗词、帖子或文章详情页可以直接发表评论与点赞，论坛评论支持多级回复。
```
</details>

## M4（medium）诗人认证从提交到通过要经历哪些状态？管理员用哪个接口审核？用户从哪里提交？

**回答**：诗人认证从提交到通过需经历“未认证、认证中（待审核）、已认证”三种状态，最终状态为 1 表示已通过。管理员使用 `PUT /api/admin/poet-profiles/{id}/verify` 接口进行审核，该接口属于 `sc-moyuan-backend AdminController`。用户登录后进入个人中心页面（前端路由 `/user/profile`），通过“提交诗人认证申请”功能入口填写资料并提交。

具体状态流转如下：
1. **未认证**：初始状态，状态值为 0；
2. **认证中（待审核）**：提交申请后进入此状态，状态值为 2；
3. **已认证**：管理员审核通过后变为该状态，状态值为 1。

若审核失败，状态将变为 3（认证失败）。用户需在个人中心提交申请，系统会将资料存入数据库并标记为待审核状态，随后由管理员调用上述接口完成最终判定。（来源：moyuan_business_modules.md·业务模块说明 / 4. 诗人认证模块 / 认证状态；moyuan_business_modules.md·业务模块说明 / 10. 管理模块 / API 接口；moyuan_system_knowledge.md·墨渊小助手系统知识库（24 条）/ 个人中心（page.profile）；moyuan_system_knowledge.md·墨渊小助手系统知识库（24 条）/ 诗人认证（feature.poet-cert））

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 4. 诗人认证模块 / 功能说明】
【章节：业务模块说明 / 4. 诗人认证模块 / 功能说明】
### 功能说明

- 诗人认证申请：用户提交诗人认证申请，填写诗人资料
- 认证审核：管理员审核诗人认证申请，通过或拒绝
- 诗人资料管理：认证诗人可管理自己的诗人资料
- 认证状态展示：用户主页展示诗人认证状态和资料

【moyuan_business_modules.md · 业务模块说明 / 4. 诗人认证模块 / 认证流程】
【章节：业务模块说明 / 4. 诗人认证模块 / 认证流程】
### 认证流程

```
用户提交认证申请 → 管理员审核 → 审核通过/拒绝
    ↓                              ↓
填写诗人资料                    更新认证状态
（笔名、简介、擅长体裁等）        用户获得诗人标识
```

【moyuan_business_modules.md · 业务模块说明 / 4. 诗人认证模块 / 认证状态】
【章节：业务模块说明 / 4. 诗人认证模块 / 认证状态】
### 认证状态

| 状态值 | 说明 |
|--------|------|
| 0 | 未认证 |
| 1 | 已认证 |
| 2 | 认证中（待审核） |
| 3 | 认证失败 |

【moyuan_business_modules.md · 业务模块说明 / 10. 管理模块 / API 接口】
【章节：业务模块说明 / 10. 管理模块 / API 接口】
| backend AdminController |
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
| PUT | /api/admin/poet-featured/{id} | 更新精选诗人 | sc-moyuan-backend AdminController

【moyuan_system_workflow.md · 系统工作流文档 / 12. 论坛与社区域 / 12.2 内容审核流（E6）】
【章节：系统工作流文档 / 12. 论坛与社区域 / 12.2 内容审核流（E6）】
### 12.2 内容审核流（E6）
- **触发入口**：`PUT /api/admin/poems/{id}/audit`、`/forum-posts/{id}/audit`、`/comments/{id}/audit`、`/ai-contents/{id}/approve|reject`、`GET /api/admin/audit/stats`。
- **分步流程**：各类内容创建进入待审态（`poem.audit_status=0`、AI 内容 `status=0`；评论创建即 `status=1` 直接可见，后台可置 0 隐藏）→ 运营后台列表筛选提交 status/reason → service 更新并记审计字段；AI 内容通过含写回原表（见 11.3）。`GET /api/admin/audit/stats` 聚合诗作/评论/诗人认证/AI内容/诗人草稿/投稿建议的待审数量。
- **待审对象速查**：
  | 对象 | 待审字段 | 审核接口 |
  |------|----------|----------|
  | 诗作 | `poem.audit_status=0` | `PUT /api/admin/poems/{id}/audit` |
  | 论坛帖子 | `forum_post.status` | `PUT /api/admin/forum-posts/{id}/audit` |
  | 评论 | `comment.status=1`（正常；0 为隐藏） | `PUT /api/admin/comments/{id}/audit` |
  | AI 内容 | `ai_generated_content.status=0` | `PUT /api/admin/ai-contents/{id}/approve|reject` |
  | 诗人认证 | `poet_profile.verified_status=0` | `PUT /api/admin/poet-profiles/{id}/verify` |
  | 诗人草稿 | `poet_draft.status=0` | `PUT /api/admin/poet-drafts/{id}/review` |
  | 投稿建议 | `poet_suggestion.

【moyuan_system_workflow.md · 系统工作流文档 / 12. 论坛与社区域 / 12.2 内容审核流（E6）】
【章节：系统工作流文档 / 12. 论坛与社区域 / 12.2 内容审核流（E6）】
verified_status=0` | `PUT /api/admin/poet-profiles/{id}/verify` |
  | 诗人草稿 | `poet_draft.status=0` | `PUT /api/admin/poet-drafts/{id}/review` |
  | 投稿建议 | `poet_suggestion.status=pending` | `PUT /api/admin/poet-suggestions/{id}/review` |
- **后台审核列表接口与前端取数约定**：
  - 列表查询：诗作 `GET /api/admin/poems`（参数 `page`、`size`、`keyword`、`auditStatus`、`sort`；`auditStatus` 0-待审核/1-已通过/2-已拒绝，不传返回全部；`sort` 取 asc-按创建时间正序/desc-倒序，默认 desc）；评论 `GET /api/admin/comments`（参数 `page`、`size`、`status`，1-正常/0-隐藏）；AI 内容 `GET /api/admin/ai-contents`（参数 `page`、`size`、`status`，0-待审核/1-已通过/2-已拒绝）。诗人草稿/建议用 `pageNum`、`pageSize`、`status`。
  - 审核变更：`PUT /api/admin/poems/{id}/audit` 的 body `{status, reason}` 中 `status` 对应 `auditStatus` 语义（0/1/2，拒绝为 2）；`PUT /api/admin/comments/{id}/audit` 的 `status` 为 1-正常/0-隐藏。
  - 前端取数：统一响应体为 `{code, message, data}`，Axios 拦截器返回整个响应体，故后台列表页面必须从 `res.data.records` / `res.data.total` 取值（不可写 `res.records`）；诗作审核页状态列与操作判断绑定 `row.auditStatus`，作者/朝代列绑定后端填充的 `poetName` / `dynastyName`（由 `PoemServiceImpl.getAdminPoemList` 调 `populatePoetAndDynasty` 填充），评论页状态列绑定 `row.status`（1-正常/0-隐藏）。
- **关键类与方法**：`AdminController.

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 个人中心（page.profile）】
【章节：墨渊小助手系统知识库（24 条） / 个人中心（page.profile）】
## 个人中心（page.profile）

- 分类：page
- 关键词：个人中心,我的,资料,收藏,历史,认证
- 别名：个人主页,我的页面
- 前端路由：/user/profile
- 说明：个人中心可查看与修改个人资料、上传头像、修改密码，并查看我的收藏、我的点赞、我的帖子和浏览历史。诗人认证申请也从个人中心提交。需要登录后进入。
- 使用步骤：
  1. 登录后进入个人中心
  2. 修改资料、头像或密码
  3. 查看收藏、点赞、帖子与浏览历史
  4. 需要时提交诗人认证申请
- 常见问答：
  - 问：个人中心在哪里  答：登录后可从用户菜单进入个人中心，管理资料、收藏、历史并提交诗人认证。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 诗人认证（feature.poet-cert）】
【章节：墨渊小助手系统知识库（24 条） / 诗人认证（feature.poet-cert）】
## 诗人认证（feature.poet-cert）

- 分类：feature
- 关键词：诗人认证,认证,成为诗人,诗人标识
- 别名：认证诗人,诗人身份
- 前端路由：/user/profile
- 说明：诗人认证可让你申请成为认证诗人，提交笔名、简介、擅长体裁等资料，由管理员审核，通过后获得诗人标识并在用户主页展示。认证申请从个人中心提交，需要登录。
- 使用步骤：
  1. 登录后进入个人中心
  2. 提交诗人认证申请并填写资料
  3. 等待管理员审核
  4. 通过后在用户主页展示认证状态
- 常见问答：
  - 问：怎么成为认证诗人  答：登录后在个人中心提交诗人认证申请，填写资料，通过管理员审核后即获得诗人标识。
```
</details>

## M5（medium）一份报修工单从创建到关闭再到满意度评价，经历了哪些状态和接口？用户在哪个页面操作？

**回答**：一份报修工单从创建到关闭再到满意度评价，经历了待处理、处理中、已解决、已关闭四个核心状态，并伴随内部备注或拒绝状态（4）。用户主要在“我的报修列表”及“报修详情”页面进行操作。

**1. 状态流转与数值**
- **待处理 (0)**：工单创建初始状态，由系统生成唯一编号。
- **处理中 (1)**：管理员分配处理人后进入此状态。
- **已解决 (2)**：处理完成时记录解决时间。
- **已关闭 (3)**：用户主动关闭或管理员操作后进入。
- **拒绝 (4)**：工单被拒绝时的特殊状态（来源：moyuan_business_modules.md · 业务模块说明 / 18. 报修模块 / API 接口；moyuan_system_workflow.md · 系统工作流文档 / 13. 报修工单域 / 13.1 工单全生命周期（E7））。

**2. 关键接口路径与方法**
- **创建工单**：`POST /api/repair/orders`（来源：moyuan_business_modules.md · 业务模块说明 / 18. 报修模块 / API 接口；moyuan_system_workflow.md · 系统工作流文档 / 13. 报修工单域 / 13.1 工单全生命周期（E7））。
- **更新状态**：`PUT /api/admin/repairs/{id}/status`（来源：moyuan_business_modules.md · 业务模块说明 / 18. 报修模块 / API 接口；moyuan_system_workflow.md · 系统工作流文档 / 13. 报修工单域 / 13.1 工单全生命周期（E7）

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 18. 报修模块 / 功能说明】
【章节：业务模块说明 / 18. 报修模块 / 功能说明】
### 功能说明

- 用户提交报修工单（标题、描述、分类、优先级）
- 查看我的报修列表和详情
- 添加反馈评论
- 关闭工单
- 提交满意度评价（1-5星 + 评语）
- 管理端：工单管理、分配处理人、状态流转、内部备注、统计

【moyuan_business_modules.md · 业务模块说明 / 18. 报修模块 / 工单状态流转】
【章节：业务模块说明 / 18. 报修模块 / 工单状态流转】
### 工单状态流转

```
待处理(0) → 处理中(1) → 已解决(2) → 已关闭(3)
```

【moyuan_business_modules.md · 业务模块说明 / 18. 报修模块 / API 接口】
【章节：业务模块说明 / 18. 报修模块 / API 接口】
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

【moyuan_business_modules.md · 业务模块说明 / 18. 报修模块 / 前端路由】
【章节：业务模块说明 / 18. 报修模块 / 前端路由】
### 前端路由

| 路径 | 说明 |
|------|------|
| `/repair` | 我的报修列表 |
| `/repair/create` | 提交报修 |
| `/repair/:id` | 报修详情 |
| `/admin/repairs` | 管理端报修管理 |

【moyuan_business_modules.md · 业务模块说明 / 18. 报修模块 / 数据库表】
【章节：业务模块说明 / 18. 报修模块 / 数据库表】
### 数据库表

- `repair_order`：报修工单表
- `repair_comment`：报修评论表

---

【moyuan_system_workflow.md · 系统工作流文档 / 12. 论坛与社区域 / 12.2 内容审核流（E6）】
【章节：系统工作流文档 / 12. 论坛与社区域 / 12.2 内容审核流（E6）】
### 12.2 内容审核流（E6）
- **触发入口**：`PUT /api/admin/poems/{id}/audit`、`/forum-posts/{id}/audit`、`/comments/{id}/audit`、`/ai-contents/{id}/approve|reject`、`GET /api/admin/audit/stats`。
- **分步流程**：各类内容创建进入待审态（`poem.audit_status=0`、AI 内容 `status=0`；评论创建即 `status=1` 直接可见，后台可置 0 隐藏）→ 运营后台列表筛选提交 status/reason → service 更新并记审计字段；AI 内容通过含写回原表（见 11.3）。`GET /api/admin/audit/stats` 聚合诗作/评论/诗人认证/AI内容/诗人草稿/投稿建议的待审数量。
- **待审对象速查**：
  | 对象 | 待审字段 | 审核接口 |
  |------|----------|----------|
  | 诗作 | `poem.audit_status=0` | `PUT /api/admin/poems/{id}/audit` |
  | 论坛帖子 | `forum_post.status` | `PUT /api/admin/forum-posts/{id}/audit` |
  | 评论 | `comment.status=1`（正常；0 为隐藏） | `PUT /api/admin/comments/{id}/audit` |
  | AI 内容 | `ai_generated_content.status=0` | `PUT /api/admin/ai-contents/{id}/approve|reject` |
  | 诗人认证 | `poet_profile.verified_status=0` | `PUT /api/admin/poet-profiles/{id}/verify` |
  | 诗人草稿 | `poet_draft.status=0` | `PUT /api/admin/poet-drafts/{id}/review` |
  | 投稿建议 | `poet_suggestion.

【moyuan_system_workflow.md · 系统工作流文档 / 12. 论坛与社区域 / 12.2 内容审核流（E6）】
【章节：系统工作流文档 / 12. 论坛与社区域 / 12.2 内容审核流（E6）】
verified_status=0` | `PUT /api/admin/poet-profiles/{id}/verify` |
  | 诗人草稿 | `poet_draft.status=0` | `PUT /api/admin/poet-drafts/{id}/review` |
  | 投稿建议 | `poet_suggestion.status=pending` | `PUT /api/admin/poet-suggestions/{id}/review` |
- **后台审核列表接口与前端取数约定**：
  - 列表查询：诗作 `GET /api/admin/poems`（参数 `page`、`size`、`keyword`、`auditStatus`、`sort`；`auditStatus` 0-待审核/1-已通过/2-已拒绝，不传返回全部；`sort` 取 asc-按创建时间正序/desc-倒序，默认 desc）；评论 `GET /api/admin/comments`（参数 `page`、`size`、`status`，1-正常/0-隐藏）；AI 内容 `GET /api/admin/ai-contents`（参数 `page`、`size`、`status`，0-待审核/1-已通过/2-已拒绝）。诗人草稿/建议用 `pageNum`、`pageSize`、`status`。
  - 审核变更：`PUT /api/admin/poems/{id}/audit` 的 body `{status, reason}` 中 `status` 对应 `auditStatus` 语义（0/1/2，拒绝为 2）；`PUT /api/admin/comments/{id}/audit` 的 `status` 为 1-正常/0-隐藏。
  - 前端取数：统一响应体为 `{code, message, data}`，Axios 拦截器返回整个响应体，故后台列表页面必须从 `res.data.records` / `res.data.total` 取值（不可写 `res.records`）；诗作审核页状态列与操作判断绑定 `row.auditStatus`，作者/朝代列绑定后端填充的 `poetName` / `dynastyName`（由 `PoemServiceImpl.getAdminPoemList` 调 `populatePoetAndDynasty` 填充），评论页状态列绑定 `row.status`（1-正常/0-隐藏）。
- **关键类与方法**：`AdminController.

【moyuan_system_workflow.md · 系统工作流文档 / 13. 报修工单域 / 13.1 工单全生命周期（E7）】
【章节：系统工作流文档 / 13. 报修工单域 / 13.1 工单全生命周期（E7）】
### 13.1 工单全生命周期（E7）
- **目标**：用户报修 → 管理员分配/处理/关闭 → 满意度评价。
- **触发入口**：用户 `POST /api/repair/orders`、`GET /api/repair/orders`、`.../{id}`、`.../{id}/comments`、`PUT .../close`、`PUT .../satisfaction`；管理 `GET/PUT /api/admin/repairs…`、`/assign`、评论等。
- **分步流程**：
  1. **创建**：`generateOrderNo`=`RO`+yyyyMMddHHmmssSSS+随机3位；status=0 待处理、priority 默认 2。
  2. **派单**：设置 assigneeId，待处理→处理中(status=1)。
  3. **流转** `updateStatus`：status=2 记 resolveTime，3 记 closeTime，4 拒绝。
  4. **沟通**：`addComment` 写 `repair_comment`（用户评论 isInternal=0；管理端内部备注 isInternal=1，用户侧查询过滤）。
  5. **关闭/评价**：`closeOrder` 置 3（拒绝重复关闭）；`submitSatisfaction` 写满意度及评语。详情/列表校验归属权限。
  6. **统计** `getStats` 按状态计数 total/pending(0)/processing(1)/resolved(2)/closed(3)/rejected(4)。
- **关键类与方法**：`RepairOrderServiceImpl.generateOrderNo/createOrder/assignOrder/updateStatus/closeOrder/submitSatisfaction/getStats`、`RepairController.*`、`AdminController.listRepairs/updateRepairStatus/assignRepair/getRepairStats`。
- **数据与缓存**：`repair_order`（status 状态机 0/1/2/3/4、order_no 唯一）、`repair_comment`（is_internal）。

---

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 报修中心（page.repair）】
【章节：墨渊小助手系统知识库（24 条） / 报修中心（page.repair）】
## 报修中心（page.repair）

- 分类：page
- 关键词：报修,反馈,工单,故障,问题反馈
- 别名：报修,反馈中心
- 前端路由：/repair
- 说明：报修中心用于提交报修工单，可填写标题、描述、分类与优先级，提交后查看我的报修列表与详情、添加反馈评论、关闭工单并提交满意度评价。需要登录后进入。
- 使用步骤：
  1. 登录后进入报修中心
  2. 点击提交报修并填写工单信息
  3. 在列表中查看工单详情与进度
  4. 添加评论、关闭工单并评价
- 常见问答：
  - 问：怎么报修  答：登录后进入报修中心，点击提交报修，填写标题、描述、分类与优先级后提交。
```
</details>

## M6（medium）智能搜索没有关键词时怎么处理？带关键词时三路并发分别做什么？

**回答**：当智能搜索无关键词时，系统执行条件筛选与排序；有关键词时则启动三路并行搜索并去重合并。具体处理如下：

1. **无关键词处理**：后端调用 `searchWithoutKeyword()` 方法，依据用户输入的朝代、分类、诗人等筛选条件进行分页查询，并按最新、热门或点赞数进行排序返回结果（来源：moyuan_business_modules.md·业务模块说明 / 8. 搜索模块 / 技术方案；moyuan_system_workflow.md·系统工作流文档 / 10.1 全文/跨模块智能搜索）。

2. **有关键词时的三路并发**：
   - **`searchPoems()`**：执行标题与内容的匹配。默认开启 MySQL ngram 全文检索（`MATCH(title, content) AGAINST(...)`），若关闭全文则回退为 `LIKE` 模糊匹配，结果经 `calculateScore` 打分排序；
   - **`searchByPoetName()`**：通过 `resolvePoetNameIds()` 模糊匹配诗人 ID，再检索该诗人的作品，评分规则为精确 0.9、包含 0.7、部分 0.3；
   - **`searchByPinyin()`**：利用 `PinyinUtil.matchesPinyin` 进行拼音联想搜索，基础分 0.3，结合标题 0.2 与诗人 0.1 加权计算（来源：moyuan_system_workflow.md·系统工作流文档 / 10.1 全文/跨模块智能搜索；moyuan_business_modules.md·业务模块说明 / 8. 搜索模块 / 技术方案）。

最终三路结果按 `poem.id` 去重合并，统一评分排序后分页返回。

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 技术方案】
【章节：业务模块说明 / 8. 搜索模块 / 技术方案】
### 技术方案

- **搜索核心**：SmartSearchService，多条件复合查询 + 评分排序
- **搜索引擎**：MySQL 8 ngram 中文全文检索（`/api/search/smart` 关键词默认走 `MATCH(title,content) AGAINST(...)`，`search.fulltext-enabled` 默认开启，可经 `SEARCH_FULLTEXT_ENABLED` 关闭回退 LIKE）；Elasticsearch 属后期可选优化
- **拼音支持**：PinyinUtil 工具类，可配置搜索范围（默认200条，最大1000条）
- **缓存策略**：Redis 缓存搜索建议（5分钟）、热门搜索（实时统计）、搜索历史（30天）
- **推荐服务**：RecommendationService，协同过滤 + 兴趣推荐 + 外部API
- **外部API集成**：今日诗词API，补充古诗词数据源

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 搜索流程】
【章节：业务模块说明 / 8. 搜索模块 / 搜索流程】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / API 接口】
【章节：业务模块说明 / 8. 搜索模块 / API 接口】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 前端路由】
【章节：业务模块说明 / 8. 搜索模块 / 前端路由】
### 前端路由

| 路径 | 说明 |
|------|------|
| `/poem` | 诗词列表页（含复合筛选搜索） |
| `/` (首页) | 历史的印痕模块内嵌搜索功能 |

> 说明：项目未设置独立的 `/search` 结果页路由，全局搜索能力内嵌在首页与诗词列表页中通过 `/api/search/smart` 实现。

---

【moyuan_business_modules.md · 业务模块说明 / 26. 全局 AI 小窗与系统知识库模块 / 功能说明】
【章节：业务模块说明 / 26. 全局 AI 小窗与系统知识库模块 / 功能说明】
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

【moyuan_system_workflow.md · 系统工作流文档 / 10. 搜索与运营域 / 10.1 全文/跨模块智能搜索（C8）】
【章节：系统工作流文档 / 10. 搜索与运营域 / 10.1 全文/跨模块智能搜索（C8）】
### 10.1 全文/跨模块智能搜索（C8）
- **触发入口**：`GET /api/search/smart?dynastyId&categoryId&poetId&poetName&keyword&sortBy&pageNum&pageSize`；`GET /api/search?keyword=`（三段返回 poems/poets/posts）；`GET /api/search/poems`；`GET /api/search/suggestions|hot|history`、`DELETE /api/search/history`。
- **分步流程**（`SmartSearchServiceImpl.smartSearch`）：
  1. 有 keyword 且已登录 → `saveSearchHistory`。
  2. `resolvePoetNameIds`：poetName 对 poet.name 模糊匹配取 ID 集。
  3. 无 keyword → `searchWithoutKeyword` 按筛选条件分页 + 排序（最新/热门/点赞）。
  4. 有 keyword 三路并发，命中结果统一按 poem.id 去重取高分、内存分页，为空置 `suggestExternal=true`：
     - `searchPoems`（名称匹配，**双模式**）：
       - 关闭全文（`search.fulltext-enabled=false`）：对 title/content 用原始词 + 去标点拆分词 `like` 模糊匹配。
       - **默认开启**（`search.fulltext-enabled=true`，见 `application.yml`）：改用 ngram 中文全文索引 `MATCH(title, content) AGAINST(... IN NATURAL LANGUAGE MODE)`（`poem` 表 `ft_poem_title_content` 已 `WITH PARSER ngram`，中文分词 + 相关度更高，见 `docs/database/migration_004_poem_search_fulltext.sql`）。
       - 两模式命中集统一经 `scoreAndRank` 按 `calculateScore` 打分：标题精确 1.0/包含 0.8/拆分 0.3、内容包含 0.5/拆分 0.2。
     - `searchByPoetName`：匹配诗人名取作品，精确 0.9/包含 0.7/部分 0.3。
     - `searchByPinyin`：`PinyinUtil.matchesPinyin` 拼音联想，基础 0.3+标题 0.2+诗人 0.1。
  5.

【moyuan_system_workflow.md · 系统工作流文档 / 10. 搜索与运营域 / 10.1 全文/跨模块智能搜索（C8）】
【章节：系统工作流文档 / 10. 搜索与运营域 / 10.1 全文/跨模块智能搜索（C8）】
建议：先查 Redis `search:suggestion:{kw}`（5min），未命中从 poem 标题+诗人名并集。
  6. 热门：读 Redis ZSet `search:hot` 逆序；空返回默认词。历史：`search:history:{userId}` List（remove+leftPush+trim(0,19)+30天过期），同时 `zset.incrementScore` 累加热度。
  7. `/api/search/poems` 走 `recommendationService.searchPoems`：本地（附 `recommendScore` 含兴趣加权+浏览/点赞对数分），不足时拉外部（author 含 keyword，`recommendScore=0.5`）合并降序。
- **关键类与方法**：`SmartSearchServiceImpl.*`、`SearchController.*`、`PinyinUtil.matchesPinyin`、`RecommendationServiceImpl.searchPoems/getExternalPoems/calculatePoemRecommendScore`；配置 `search.fulltext-enabled`（默认 true，全文已开启，可用环境变量 `SEARCH_FULLTEXT_ENABLED` 覆盖）、`search.pinyin.limit`。
- **数据与缓存**：Redis `search:suggestion:{kw}`、`search:hot`(ZSet)、`search:history:{userId}`(List)；`poem` 表全文索引 `ft_poem_title_content`（ngram）。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 智能检索（feature.search）】
【章节：墨渊小助手系统知识库（24 条） / 智能检索（feature.search）】
## 智能检索（feature.search）

- 分类：feature
- 关键词：搜索,检索,找诗,搜诗人,关键字
- 别名：智能搜索,全文搜索
- 前端路由：/poem
- 说明：智能检索支持按关键词搜索诗词、诗人与帖子，首页与诗词列表页内嵌了搜索入口。搜索会记录你的历史，登录后还会根据兴趣给出推荐。
- 使用步骤：
  1. 在首页或诗词列表页输入关键词
  2. 查看匹配的诗词、诗人或帖子
  3. 点开结果查看详情
- 常见问答：
  - 问：怎么搜索  答：在首页或诗词列表页的搜索框输入关键词，即可搜索诗词、诗人与帖子。
```
</details>

## M7（medium）文件上传时会做哪些校验和处理？上传接口是什么？

**回答**：文件上传时执行格式大小校验、MD5 秒传、分类存储及 AI 水印处理，具体流程如下：

1. **校验规则**：空文件返回 400；扩展名校验（默认支持 `.jpg,.jpeg,.png,.gif,.webp,.bmp`）；文件大小校验（默认限制 10MB）。
2. **存储逻辑**：生成唯一文件名（UUID 去横线 + 扩展名）；根据类型选择子目录（如 `avatar→avatars/{relatedId}/`、`poem→poems/{relatedId}/` 等）；计算 MD5 值，若磁盘已存在则复用文件。
3. **特殊处理**：若为 AI 生成图片（`fileType=="ai_generated"`），自动添加水印；读取图片尺寸并入库元数据。
4. **返回结果**：包含访问 URL、文件名及 fileKey。

上传接口路径为 `POST /api/files/upload`，位于后端模块 `sc-moyuan-backend FileController`，支持参数 `fileType`、`relatedId` 和 `relatedType`（来源：moyuan_business_modules.md·业务模块说明/11.文件模块/API 接口；moyuan_system_workflow.md·系统工作流文档/6.文件与资源域/6.1文件上传全链路（A6））。

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 11. 文件模块 / 功能说明】
【章节：业务模块说明 / 11. 文件模块 / 功能说明】
### 功能说明

- 统一文件上传功能（支持图片上传，按类型分目录存储）
- 文件元数据管理（file_metadata表）
- AI生成图片记录管理（ai_image_record表）
- 图片预览和管理
- 文件存储和访问
- AI图片自动水印功能
- 缩略图生成

【moyuan_business_modules.md · 业务模块说明 / 11. 文件模块 / API 接口】
【章节：业务模块说明 / 11. 文件模块 / API 接口】
### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| POST /api/files/upload | 上传文件（支持fileType、relatedId、relatedType参数） | sc-moyuan-backend FileController |
| DELETE /api/files/{fileKey} | 删除文件 | sc-moyuan-backend FileController |
| GET /api/files/{fileKey} | 获取文件信息 | sc-moyuan-backend FileController |

【moyuan_business_modules.md · 业务模块说明 / 11. 文件模块 / 前端组件】
【章节：业务模块说明 / 11. 文件模块 / 前端组件】
### 前端组件

| 组件 | 说明 |
|------|------|
| ImageUpload.vue | 图片上传组件（支持预览、裁剪、fileType参数） |

【moyuan_system_workflow.md · 系统工作流文档 / 4. 认证与权限域 / 4.3 JWT 校验与安全过滤器链认证（A3）】
【章节：系统工作流文档 / 4. 认证与权限域 / 4.3 JWT 校验与安全过滤器链认证（A3）】
### 4.3 JWT 校验与安全过滤器链认证（A3）
- **目标**：受保护请求进入 Controller 前完成 token 验签、身份装配。
- **触发入口**：任何携带 token 的 HTTP 请求（过滤器链自动触发）。
- **分步流程**：`JwtAuthenticationFilter.doFilterInternal` → `extractToken`（`Bearer ` 前缀）→ `jwtUtil.validateToken`（验签 + 过期 + **type=access**）→ `TokenBlacklistService.isBlacklisted(jti)` 黑名单校验 → `getUserId` → 查库 → `status==1` 时组装 `LoginUser` 并写 `UsernamePasswordAuthenticationToken` 到 `SecurityContextHolder`。无论成败都 `doFilter` 放行，访问权限由 `SecurityConfig` 决定。
- **关键类与方法**：`JwtAuthenticationFilter.doFilterInternal/extractToken`、`JwtUtil.validateToken/parseToken/getUserId`、`TokenBlacklistService.isBlacklisted`、`LoginUser.getAuthorities`、`SecurityConfig.filterChain`。
- **数据与缓存**：`user.id/status/role`。

【moyuan_system_workflow.md · 系统工作流文档 / 6. 文件与资源域 / 6.1 文件上传全链路（A6）】
【章节：系统工作流文档 / 6. 文件与资源域 / 6.1 文件上传全链路（A6）】
### 6.1 文件上传全链路（A6）
- **目标**：统一处理图片/文件上传，含格式与大小校验、MD5 秒传、分类存储、AI 水印与元数据入库。
- **触发入口**：`POST /api/files/upload`（multipart：`file`、`fileType`、可选 `relatedId`/`relatedType`）。
- **分步流程**（`FileStorageServiceImpl.upload`，`@Transactional`）：
  1. 空文件（400）、扩展名校验（`file.allowed-extensions`，默认 `.jpg,.jpeg,.png,.gif,.webp,.bmp`）、大小校验（`file.max-size` 默认 10MB）。
  2. 唯一文件名：`UUID(去横线)+扩展名`。
  3. 目录选择 `buildSubDirectory`：`avatar→avatars/{relatedId}/`、`poem→poems/{relatedId}/`、`user_poem→user_poems/{relatedId}/`、`forum→forum/{yyyy/MM}/`、`vision→vision/article_{relatedId}/`、`ai_generated→ai_generated/{yyyy-MM-dd}/` 等。
  4. 读字节 → `calculateMd5` → **MD5 秒传**：查 `file_metadata` 同 `md5` 且磁盘文件存在则直接复用，不重复存储。
  5. 写磁盘（父目录 mkdirs + Files.write）。
  6. **AI 水印**：`fileType=="ai_generated"` 且为 image 时，`WatermarkUtil.addAiWatermark` 生成水印图覆盖原文件。
  7. 图片尺寸读取（失败仅 warn 不阻断）；元数据 `file_metadata` 入库（fileKey=relativePath 等）。
  8. 返回 `{url: baseUrl+"/"+fileKey, fileName, fileKey}`；`/uploads/**` 由 `WebMvcConfig` 映射到磁盘目录并 `permitAll`。
  9. 配套：`DELETE /api/files/{fileKey}`（先删物理再删记录）、`GET /api/files/{fileKey}`。
  10.

【moyuan_system_workflow.md · 系统工作流文档 / 6. 文件与资源域 / 6.1 文件上传全链路（A6）】
【章节：系统工作流文档 / 6. 文件与资源域 / 6.1 文件上传全链路（A6）】
配套：`DELETE /api/files/{fileKey}`（先删物理再删记录）、`GET /api/files/{fileKey}`。
  10. **后台治理**：`GET /api/admin/files`（分页，fileType/keyword/status）、`GET /api/admin/files/stats`（总文件数、总占用、各类型占用、物理缺失数）、`POST /api/admin/files/clean-orphans`（扫描物理目录删除磁盘存在但库中无记录的孤儿文件，返回删除数量与释放字节）、`DELETE /api/admin/files/{id}`（按 id 查 fileKey 后走 `delete`，含物理文件）。
- **关键类与方法**：`FileStorageServiceImpl.upload/delete/getUrl/listFiles/getStorageStats/cleanOrphanFiles/buildSubDirectory/calculateMd5/applyWatermark`、`FileController`（用户端）、`AdminFileController`（管理端）、`WatermarkUtil.addAiWatermark`、`WebMvcConfig.addResourceHandlers`。
- **数据与缓存**：`file_metadata`（file_key/original_name/file_type/mime_type/size/md5/width/height/related_* 等）；配置 `file.upload-dir`（默认 `./uploads`）、`file.base-url`、`file.allowed-extensions`、`file.max-size`。

---

【moyuan_system_workflow.md · 系统工作流文档 / 7. 审计与统一响应（A7/A8） / 7.1 操作日志 / 审计记录（A7）】
【章节：系统工作流文档 / 7. 审计与统一响应（A7/A8） / 7.1 操作日志 / 审计记录（A7）】
### 7.1 操作日志 / 审计记录（A7）
- **目标**：AOP 对 Controller 调用自动记录操作人、方法、参数、IP、耗时、成败。
- **触发入口**：`com.moyuan.controller.*` 任意方法被调用（切面自动拦截）。
- **分步流程**：`OperationLogAspect.around`：分别记 `method`（请求方法，如 `GET`/`POST`）与 `uri`（请求 URI，两者拆分独立存储）、`ip`、`operation`（方法名）；`params` 序列化前 `maskSensitiveFields` 把 `password/oldPassword/newPassword/token/secret` 置 `***`；记录当前用户；`proceed()` 成功 status=1 并将返回结果序列化写入 `result`（超过 `MAX_RESULT_LENGTH=2000` 字符自动截断并追加 `...(已截断)`，序列化失败写 `[序列化失败]`）、异常 status=0 并记 errorMsg 后向上抛；finally 记 `duration` 落 `operation_log`（失败仅 log.error 不阻断）。查询 `GET /api/admin/logs`（keyword/startTime/endTime 过滤，keyword 匹配操作人、请求方法、请求 URI、操作内容，倒序分页）。
- **关键类与方法**：`OperationLogAspect.around/sanitizeParams/maskSensitiveFields/toJson`、`SecurityUtil`、`OperationLogServiceImpl`、`AdminController.getLogs`。
- **数据与缓存**：`operation_log`（user_id/username/operation/method/uri/params/result/ip/duration/status/error_msg/create_time）。

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.5 首页数据组装（K5）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.5 首页数据组装（K5）】
### 15.5 首页数据组装（K5）
- **轮播图**：静态 `/img/lb_*.jpg`、`/img/lb_shiwen_*.png`，无后端。
- **今日诗词**：`DailyPoetry` → 后端 `GET /api/external-poetry/jinrishici` 代理（`v2.jinrishici.com/one.json`，后端 60 分钟进程内缓存，前端不再直连）。
- **首页导航**：`GET /home-navigation`。
- **精选诗人**：`getRandomPoetFeatured(8)`，去重取 4 位，每 20s 轮换。
- **论坛预览**：`GET /forum/posts?pageSize=5`。
- **古诗精赏**：本地 json + `GET /search/poems` + `/search/poems/recommended`。
- **当代精选**：本地 json + like/favorite/comment 接口。
- **AI 区块**：`/ai/write-poem`、`/ai/analyze`、`/ai/couplet`、`/ai/ocr`、`/ai/chat`（藏头诗、平仄格律校验，复用 `POST /api/ai/chat` 并传 `moduleCode=chat`，不新增后端接口）、`/rhyme/*`；"诗海拾贝"本地 json。
- **小游戏区块**：首页 AI 小工具区「飞花令」「诗词接龙」两张卡片——游客点击展开内嵌小窗口（`GameMiniPanel`）直接开局，登录用户跳转 `/game/feihua`、`/game/chain`；后端为独立游戏模块（`/api/game/feihua`、`/api/game/chain`，见 `modules.md` 第 24 章）。
- **诗话视野**：`GET /vision/articles`。
- **诗词卡片跳转**：首页「古诗精赏 / 当代精选」卡片点击经组合式函数 `composables/usePoemCardNavigation.ts` 处理——本地命中直接 `router.push('/poem/{id}')`，未命中回退 `PoetryDetailDialog`（外部诗词弹窗，可由接口盒子实时增强）。
- **关键位置**：`views/home/index.vue`、`components/{home,business}/*`、`composables/usePoemCardNavigation.ts`、`api/modules/*`。
```
</details>

## M8（medium）平台为什么用双 token？用户登出后旧的 access token 怎么失效？

**回答**：平台采用双 token（access 短时效 + refresh 续期）机制，旨在平衡安全性与用户体验：access token 有效期短（默认 24h），降低长期持有风险；refresh token 有效期长（默认 7 天），用于自动续期，避免频繁登录。用户登出后，旧 access token 通过后端黑名单服务失效，具体流程如下：

1. **触发机制**：调用 `POST /api/auth/logout` 接口（来源：moyuan_system_workflow.md·4.2 登录与 JWT 签发（A2））。
2. **失效原理**：前端将当前 token 的 `jti`（唯一标识）发送给后端，后端通过 `TokenBlacklistService` 将该 `jti` 加入黑名单（优先 Redis，失败降级内存），标记为已失效（来源：moyuan_system_workflow.md·4.2 登录与 JWT 签发（A2））。
3. **拦截验证**：当旧 token 被再次使用时，`JwtAuthenticationFilter` 会先校验 `jti` 是否在黑名单中。若存在，则拒绝验签或视为无效，直接抛出错误，无需重新请求后端拉取用户信息（来源：moyuan_system_workflow.md·4.3 JWT 校验与安全过滤器链认证（A3））。
4. **前端处理**：若遇到 401 未鉴权，统一请求层会尝试用 refresh token 续期；若刷新失败或已登出，则跳转登录页（来源：moyuan_system_workflow.md·15.1 统一请求层（K1））。

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 25. AI 对话页「诗宝阁」模块 / 技术方案】
【章节：业务模块说明 / 25. AI 对话页「诗宝阁」模块 / 技术方案】
### 技术方案

- 对话主链路为 SSE 流式 `POST /api/ai/chat/stream`：前端用 `fetch` + `ReadableStream` 解析（Axios 不支持读取流、EventSource 不支持 POST）并逐段渲染；网络中断等可重试失败才回退非流式 `POST /api/ai/chat`
- 请求组装为真正的 system role：`[{role:'system', content: persona.systemPrompt}, …最近 12 条历史, {role:'user', content: skill ? skill.buildPrompt(raw) : raw}]`（历史单条 ≤2000 字、合计 ≤8000 字），不再把角色设定前缀塞进 user 消息
- 限流/额度/鉴权类错误（400/401/403/429 与 4290~4293）**不**回退非流式——回退会二次扣配额并掩盖真实原因，直接展示后端 `message`
- 流式正常返回 `text/event-stream`；限流与参数异常由 `@RestControllerAdvice` 以 JSON 返回，前端按 `content-type` 区分后抛出 `message`，错误对象携带 `status`/`code` 供判断是否可重试
- 生成中可停止：发送按钮切换为「停止」，`abort` 后保留已生成内容并持久化；`finally` 中比对 `AbortController` 实例，避免旧请求复位新请求的 loading
- 会话同步：切换角色 / 新建会话 / 首次进入时从服务端拉取；本地有而服务端为空时反向上传；`sessionSeq` 防止快速切换角色串台
- `moduleCode` 必传 `'chat'`（已绑定 DeepSeek）；否则回退智谱默认模型，会因 key 余额不足返回 `429 code 1113`
- 藏头诗任务模板复用首页现有实现并迁至能力层；平仄校验改调本地确定性引擎 `POST /api/ai/tone-check`
- 配色沿用墨渊棕金色系（宣纸底 / 铜边 / 棕金强调），禁用 Element Plus 默认蓝

【moyuan_system_workflow.md · 系统工作流文档 / 2. 全局约定与分层（引擎级机制） / 2.3 JWT 认证与过滤器链】
【章节：系统工作流文档 / 2. 全局约定与分层（引擎级机制） / 2.3 JWT 认证与过滤器链】
### 2.3 JWT 认证与过滤器链
- 无状态会话（`SessionCreationPolicy.STATELESS`），关闭 CSRF。
- `JwtAuthenticationFilter`（挂在 `UsernamePasswordAuthenticationFilter` 之前）从 `Authorization: Bearer` 取 token → 验签/过期 → 校验 **type=access** 且 `jti` 未被 `TokenBlacklistService` 拉黑 → 查库 → 组装 `LoginUser.getAuthorities()`（`ROLE_`+role）写入 SecurityContext。当前 JWT 采用**双 token**（access 短时效 + refresh 续期），登出/刷新将旧 token 的 `jti` 加入黑名单使其失效（详见 4.2、4.3）。
- 公开接口 `permitAll`（首页静态资源、Swagger、`/api/auth/**`、浏览类接口、`/uploads/**` 等）；`/api/admin/**` 需 `ROLE_ADMIN`；其余需登录。详细放行清单见 4.3。

【moyuan_system_workflow.md · 系统工作流文档 / 4. 认证与权限域 / 4.1 用户注册（A1）】
【章节：系统工作流文档 / 4. 认证与权限域 / 4.1 用户注册（A1）】
### 4.1 用户注册（A1）
- **目标**：完成参数校验、用户名/邮箱唯一性检查、密码 BCrypt 加密并落库，注册成功即签发 JWT 直接登录。
- **触发入口**：`POST /api/auth/register`（`@RequestBody RegisterRequest`）。
- **分步流程**：
  1. `@Valid` 校验：username 长度 3–20、password 长度 6–20、email/interests 可选；失败抛 `MethodArgumentNotValidException` → 400。
  2. `UserServiceImpl.register` 查 `user` 表：用户名重复抛 `USERNAME_EXISTS(1002)`、email 重复抛 `EMAIL_EXISTS(1003)`。
  3. `passwordEncoder.encode`（BCrypt）加密密码。
  4. 补全字段（nickname 缺省取 username、interests 逗号拼接、status=1）后 `userMapper.insert`（`@Transactional`）。
  5. `jwtUtil.generateToken(user.getId(), username)` 签发，返回 `TokenResponse{token, expiresIn}`。
- **关键类与方法**：`UserServiceImpl.register`、`AuthController.register`、`JwtUtil.generateToken`、`PasswordEncoderConfig`、`RegisterRequest`。
- **数据与缓存**：`user` 表；前端注册后写 `localStorage.token`。

【moyuan_system_workflow.md · 系统工作流文档 / 4. 认证与权限域 / 4.2 登录与 JWT 签发（A2）】
【章节：系统工作流文档 / 4. 认证与权限域 / 4.2 登录与 JWT 签发（A2）】
### 4.2 登录与 JWT 签发（A2）
- **目标**：校验账号密码与禁用状态，成功后刷新登录信息并签发 JWT。
- **触发入口**：`POST /api/auth/login`（`LoginRequest{username,password}`）。
- **分步流程**：
  1. 按 username 查 `user`：无抛 `USER_NOT_FOUND(1001)`；`status!=1` 抛 `ACCOUNT_DISABLED(1005)`。
  2. `passwordEncoder.matches` 校验密码，失败抛 `PASSWORD_ERROR(1004)`。
  3. 更新 `lastLoginTime`、`lastLoginIp`（`getClientIp` 依 `X-Forwarded-For`→`Proxy-Client-IP`→`WL-Proxy-Client-IP`→`X-Real-IP`→`remoteAddr` 取值）。
  4. `JwtUtil` 签发**双 token**：access token（`jwt.expiration`，默认 24h，`type=access`，含随机 `jti`）与 refresh token（`jwt.refresh-expiration`，默认 7 天，`type=refresh`，同 `jti`）；响应经 `TokenResponse` 同时返回二者。
  5. 前端持久化两种 token，用于后续自动续期。
- **刷新与登出（#4 双 token）**：
  - `POST /api/auth/refresh`：携带 refresh token → 校验 type=refresh 且未过期 → **轮换**（旧 access/refresh 拉黑，签发新的一对）并返回。
  - `POST /api/auth/logout`：将当前 token 的 `jti` 加入黑名单，登出立即失效。
  - 黑名单实现 `TokenBlacklistService`（优先 Redis，失败降级进程内内存），key 关联 `jti`。
- **关键类与方法**：`UserServiceImpl.login/getClientIp`、`AuthController.login/refresh/logout`、`JwtUtil.generateAccessToken/generateRefreshToken`、`TokenBlacklistService.blacklist/isBlacklisted`。
- **数据与缓存**：`user.last_login_time/last_login_ip`；前端写 `localStorage`（`token`/`refreshToken`，封装为 `moyuan_token`）、admin 写 `lastAdminUsername`。

【moyuan_system_workflow.md · 系统工作流文档 / 4. 认证与权限域 / 4.3 JWT 校验与安全过滤器链认证（A3）】
【章节：系统工作流文档 / 4. 认证与权限域 / 4.3 JWT 校验与安全过滤器链认证（A3）】
### 4.3 JWT 校验与安全过滤器链认证（A3）
- **目标**：受保护请求进入 Controller 前完成 token 验签、身份装配。
- **触发入口**：任何携带 token 的 HTTP 请求（过滤器链自动触发）。
- **分步流程**：`JwtAuthenticationFilter.doFilterInternal` → `extractToken`（`Bearer ` 前缀）→ `jwtUtil.validateToken`（验签 + 过期 + **type=access**）→ `TokenBlacklistService.isBlacklisted(jti)` 黑名单校验 → `getUserId` → 查库 → `status==1` 时组装 `LoginUser` 并写 `UsernamePasswordAuthenticationToken` 到 `SecurityContextHolder`。无论成败都 `doFilter` 放行，访问权限由 `SecurityConfig` 决定。
- **关键类与方法**：`JwtAuthenticationFilter.doFilterInternal/extractToken`、`JwtUtil.validateToken/parseToken/getUserId`、`TokenBlacklistService.isBlacklisted`、`LoginUser.getAuthorities`、`SecurityConfig.filterChain`。
- **数据与缓存**：`user.id/status/role`。

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.1 统一请求层（K1）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.1 统一请求层（K1）】
### 15.1 统一请求层（K1）
- **目标**：为所有请求统一注入 token、处理响应码与 HTTP 错误。
- **分步流程**：`utils/request.ts` 创建 Axios 实例（`baseURL=VITE_API_BASE_URL||'/api'`、timeout 120000）→ 请求拦截器注入 `Authorization: Bearer ` → 响应 `code==='200'` 返回 `response.data`，否则 `ElMessage.error` 并 reject。**401 静默续期**：非公开接口（`PUBLIC_PATHS`）遇 HTTP 401 时，用本地 `refreshToken` 调 `/auth/refresh` 换取新一对 token → 写回 store/localStorage → **重放原请求**；续期失败或本身是 refresh 请求才 `logout()` 并跳 `/user/login`（`isRedirectingToLogin` 防重入）。导出 `get/post/put/del`。
- **关键位置**：`utils/request.ts`、`api/modules/*`。
- **数据与缓存**：请求头 `Authorization`。

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.2 登录态与用户 Store（K2）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.2 登录态与用户 Store（K2）】
### 15.2 登录态与用户 Store（K2）
- **分步流程**：`useUserStore` 初始化读 `localStorage` 中的 `token`/`refreshToken`，有则 `fetchUserInfo()`；`login/register` 写双 token 到 store + localStorage 后拉取用户；`fetchUserInfo` 读 `/users/me`，admin 写 `lastAdminUsername`，网络/5xx 指数重试≤2，401→尝试续期或 `logout()`；`logout` 清双 token 与 localStorage，并调用 `/auth/logout`（后端拉黑）。
- **关键位置**：`stores/user.ts`、`api/modules/user.ts`、`views/user/login.vue`、`router/guards.ts`。
- **数据与缓存**：localStorage `token`/`refreshToken`（双 token）、`lastAdminUsername`。

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.4 localStorage 使用点汇总（K4）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.4 localStorage 使用点汇总（K4）】
### 15.4 localStorage 使用点汇总（K4）
> **代码事实核对**：收藏、AI 对话、浏览历史**并未真正写入 localStorage**（收藏走服务端、AI 聊天在内存 ref、历史走 `/history` API）。真实落盘 key 如下：
- `token`/`refreshToken`（登录态双 token，user Store 直接写）
- `lastAdminUsername`（admin 记录）
- `home_nav_animation_config`（首页导航动画配置）
- `moyuan_token`（storage 封装的登录 token，带过期结构）
- `moyuan_user_preferences`（theme/language/fontSize/pageSize/sidebarCollapsed）
- `moyuan_search_history`（上限 20 条）
- `moyuan_form_draft_poem/forum/comment`
- `moyuan_sort_preferences`（{poem,poet,forum}）
- `moyuan_cache_dynasties/categories/poets`（筛选下拉，1h）
- `moyuan_cache_external_poem_`（外部诗词详情，24h）
- `moyuan_cache_`（外部搜索结果缓存）
- **收藏/AI对话/浏览历史**：无 localStorage key，分别走服务端接口/内存/API。
- **关键位置**：`utils/storage.ts`、`stores/preferences.ts/searchHistory.ts/formDraft.ts/apiCache.ts`。
```
</details>

## M9（medium）诗词详情页的赏析增强内容缓存在哪张表？缓存 key 由哪些字段组成？

**回答**：诗词详情页的赏析增强内容缓存在 `poem_content_cache` 表中，缓存 key 由诗词标题（poem_title）、诗人姓名（poet_name）和内容类型（content_type）三个字段组合而成。

1. **缓存表**：`poem_content_cache`
   - 该表用于存储外部 API 获取的诗词内容（译文、注释、赏析等），避免重复调用外部接口。
   - （来源：moyuan_business_modules.md · 业务模块说明 / 21. 诗词内容缓存模块 / 数据库表）

2. **缓存 Key 组成**：`poem_title + poet_name + content_type`
   - 查询逻辑支持按诗词标题、诗人及内容类型进行组合匹配。
   - （来源：moyuan_business_modules.md · 业务模块说明 / 21. 诗词内容缓存模块 / 功能说明）

3. **触发入口与流程**：
   - 用户访问 `GET /api/poems/{id}` 时，系统会调用 `PoemContentCacheServiceImpl`。
   - 若命中缓存则直接返回；未命中则由外部或 AI 生成内容后通过 `POST /api/poem-content/cache` 接口落库保存。
   - （来源：moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.2 诗词详情与赏析缓存（C2））

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 10. 管理模块 / 功能说明】
【章节：业务模块说明 / 10. 管理模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 10. 管理模块 / 后台菜单结构】
【章节：业务模块说明 / 10. 管理模块 / 后台菜单结构】
### 后台菜单结构

- 内容管理：诗词管理、分类管理、朝代管理、诗人管理、精选诗人管理、首页导航管理、**诗话视野管理**
- 审核管理：诗词审核、**诗人认证审核**、诗人内容审核、AI内容审核、**评论管理**
- 系统管理：操作日志、静态页面管理、**文件管理**、**缓存管理**

【moyuan_business_modules.md · 业务模块说明 / 10. 管理模块 / 权限控制】
【章节：业务模块说明 / 10. 管理模块 / 权限控制】
### 权限控制

- 需要管理员权限才能访问（`requiresAdmin: true`）
- 路由守卫会检查用户是否为管理员角色
- 非管理员用户访问管理页面会被重定向到首页

【moyuan_business_modules.md · 业务模块说明 / 10. 管理模块 / 诗词浏览流程】
【章节：业务模块说明 / 10. 管理模块 / 诗词浏览流程】
### 诗词浏览流程

```
用户访问首页 → 展示精选诗词 → 点击诗词 → 查看诗词详情 → 点赞/收藏
```

【moyuan_business_modules.md · 业务模块说明 / 10. 管理模块 / 论坛发帖流程】
【章节：业务模块说明 / 10. 管理模块 / 论坛发帖流程】
### 论坛发帖流程

```
用户登录 → 进入论坛 → 点击发帖 → 编辑内容 → 提交帖子 → 帖子列表展示
```

【moyuan_business_modules.md · 业务模块说明 / 21. 诗词内容缓存模块 / 功能说明】
【章节：业务模块说明 / 21. 诗词内容缓存模块 / 功能说明】
### 功能说明

- 缓存外部API获取的诗词内容（译文、注释、赏析等）
- 避免重复调用外部API
- 支持按诗词标题+诗人+内容类型查询

【moyuan_business_modules.md · 业务模块说明 / 21. 诗词内容缓存模块 / API 接口】
【章节：业务模块说明 / 21. 诗词内容缓存模块 / API 接口】
### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/poem-content/cache | 获取缓存的诗词内容 | sc-moyuan-backend PoemContentController |
| POST /api/poem-content/cache | 保存诗词内容缓存 | sc-moyuan-backend PoemContentController |

【moyuan_business_modules.md · 业务模块说明 / 21. 诗词内容缓存模块 / 数据库表】
【章节：业务模块说明 / 21. 诗词内容缓存模块 / 数据库表】
### 数据库表

- `poem_content_cache`：诗词内容缓存表

---

【moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.1 诗词列表查询（C1）】
【章节：系统工作流文档 / 8. 诗词内容域 / 8.1 诗词列表查询（C1）】
### 8.1 诗词列表查询（C1）
- **触发入口**：`GET /api/poems?pageNum&pageSize&dynastyId&poetId&categoryId&keyword`；`GET /api/poems/modern/page`（现代，固定 dynastyId=13）；`GET /api/poems/favorites`。
- **分步流程**：`getPoemList` 构造 `LambdaQueryWrapper`：status=1、朝代/诗人/分类 `eq`、keyword 对标题/内容 `like`（`wrapper.and`）→ `orderByDesc(createTime)` 分页 → `@Cacheable(value="poems", key="'list:'+…")` 缓存；现代分支额外按 `isOriginal/hasCertifiedPoet` 过滤、`sortBy` 排序并 `populatePoetAndDynasty` 批量填充名称。
- **关键类与方法**：`PoemController.getPoemList/getModernPoems/getFavorites`、`PoemServiceImpl.*`、`PoemMapper/DynastyMapper/CategoryMapper.selectBatchIds`。
- **数据与缓存**：`poem`（title/content/poet_id/dynasty_id/category_id/view_count/is_original/status）；缓存 `poems::list:{dynastyId}:{poetId}:{categoryId}:{keyword}:{pageNum}:{pageSize}`。

【moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.2 诗词详情与赏析缓存（C2）】
【章节：系统工作流文档 / 8. 诗词内容域 / 8.2 诗词详情与赏析缓存（C2）】
### 8.2 诗词详情与赏析缓存（C2）
- **触发入口**：`GET /api/poems/{id}`；`GET/POST /api/poem-content/cache`。
- **分步流程**：`getPoemDetail`：`selectById` → 空/status!=1 抛 `POEM_NOT_FOUND` → `setSql("view_count=view_count+1")` 原子自增并内存同步。赏析增强内容由 `PoemContentCacheServiceImpl` 按 `poem_title+poet_name+content_type` 命中 `poem_content_cache`，命中返回、未命中由外部/AI 产出后 `POST` 落库。
- **关键类与方法**：`PoemController.getPoemDetail`、`PoemServiceImpl`、`PoemContentCacheServiceImpl.getCachedContent/saveCachedContent`、`PoemContentController`。
- **数据与缓存**：`poem_content_cache`；`poem.view_count` 原子自增。

【moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.4 诗词评分（C4）】
【章节：系统工作流文档 / 8. 诗词内容域 / 8.4 诗词评分（C4）】
### 8.4 诗词评分（C4）
- **触发入口**：`GET /api/poems/{poemId}/ratings`；`POST .../ratings?score&comment`；`POST .../ratings/ai`、`GET/regenerate`；`GET .../ratings/me`。
- **分步流程**：校验 score∈[1.0,5.0] → `ratePoem`（`@Transactional`）按 `poem_id+user_id+rating_type=1` 增改评分 → `updatePoemAverageScore` 重算 `avg_score/rating_count`；AI 评分 `rating_type=2`（userId=null）：`aiService.analyzePoem(content, null, "rating")` 走 `ai_module_config` 的独立 `rating` 模块绑定（不再写死默认模型，可在后台「AI功能配置」页面调整）→ 正则解析分（失败兜底 3.5）→ 摘要取前 200 字、去 markdown 标记，随后重算均分，`ai_model` 字段记录实际生效模型名。`getPoemRatings` 返回 userRatings(type=1)/aiRating(type=2 最新)/averageScore/ratingCount，异常回退空数据。
- **关键类与方法**：`PoemRatingServiceImpl.ratePoem/requestAiRating/parseAiScore/generateSummary/updatePoemAverageScore`、`AiService.analyzePoem`。`PoemRatingController.*`。
- **数据与缓存**：`poem_rating`（score/comment/rating_type:1用户2AI/ai_model/ai_summary/ai_analysis）、`poem.avg_score/rating_count`。

【moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.5 外部诗词导入（C5）】
【章节：系统工作流文档 / 8. 诗词内容域 / 8.5 外部诗词导入（C5）】
### 8.5 外部诗词导入（C5）
- **触发入口**：`POST /api/poems/import-external`（title/content/author/dynasty）；`POST /api/poems/fix-external-poems`。
- **分步流程**：校验 title → 按 dynasty 名称查朝代 → 按 author 查诗人（无则新建 Poet 并 `clearAllCache`）→ 按 `title+poetId` 查重：命中补全空缺字段返回 `{id,imported:false}`，未命中落库（`source=external`、`poemType=ancient`、`isOriginal=0`、`auditStatus=1`、status=1）并清 `poems` 缓存返回 `{id,imported:true}`。修复接口取 `source=external` 且 `poet_id is null` 前 100 条，调 `https://cn.apihz.cn/api/zici/poetry.php` 补全作者朝代，成功后清 `poets`/`poems` 缓存返回 `{total,fixed}`。
- **关键类与方法**：`PoemController.importExternalPoem/fixExternalPoems`、`PoemServiceImpl`、`PoetDefaultAvatar.getAvatar`。
- **数据与缓存**：`poem.source/is_original/poem_type/audit_status`；清缓存 key：`poems`、`poets`。

---
```
</details>

## M10（medium）AI 模型配置在后台改完之后怎么生效？API Key 是从哪里读的？

**回答**：AI 模型配置修改后需同时满足后端开关开启与前端常量同步方可生效，API Key 从运行时注入文件读取。

1. **配置生效条件**：
   - 后端必须打开 OCR 功能开关（`ai.features.ocr-enabled=true`），否则接口返回 `4293` 错误。
   - 前端需将首页入口置灰状态移除，并开启 `OCR_ENABLED` 常量。
   - 两者需同时恢复，且存在 `ocr` 模块的视觉模型绑定（来源：moyuan_business_modules.md · 业务模块说明 / 7. AI 模块 / 功能说明）。

2. **API Key 读取方式**：
   - API Key 不直接存储在数据库中，而是作为占位符存入 `ai_model` 表。
   - 真实密钥从配置文件 `secrets/application-secrets.yml` 中运行时动态注入（来源：moyuan_business_modules.md · 业务模块说明 / 7. AI 模块 / 技术方案）。

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 7. AI 模块 / 功能说明】
【章节：业务模块说明 / 7. AI 模块 / 功能说明】
### 功能说明

- AI诗词问答（支持多模型切换；调用优先走 `ai_module_config` 模块绑定，未绑定某模块时回退默认模型。可用提供商：智谱AI、DeepSeek、Kimi、千问、小米MiMo、OpenRouter；NVIDIA NIM 已全部禁用/下线）
- 看图写诗（上传图片，AI生成古诗词，支持视觉模型如 GLM-4V-Flash、DeepSeek-Flash）
- 智能分析（输入诗句进行深度解析）
- AI对对联（输入上联，AI生成下联，遵循平仄规则）
- **藏头诗**（输入 2-8 个藏头字并选择五言/七言，AI 生成藏头诗，每句首字依次对应藏头字；复用 `POST /api/ai/chat`，moduleCode=chat，不新增后端接口）
- **平仄格律校验**（粘贴一首绝句/律诗，由**本地确定性引擎** `POST /api/ai/tone-check` 逐句标注平仄（平声绿底、仄声红底）、判定是否合律并给出不合律原因；校验规则为句末三平尾/三仄尾、二四六位失替、偶数句押韵，不调用大模型、不消耗 AI 额度）
- **AI 对话页「诗宝阁」**（`/ai-chat`）：角色化统一对话入口，6 角色（诗仙·创作 / 诗话·鉴赏 / 格律·校正 / 诗友·行令 / 群贤·论诗 / 顾问·问答）× 12 能力（命题作诗 / 集句成诗 / 藏头诗 / 润色入律 / 平仄校验 / 韵脚查询 / 对对联 / 意象典故 / 诗话点评 / 飞花令 / 诗词接龙 /

【moyuan_business_modules.md · 业务模块说明 / 7. AI 模块 / 功能说明】
【章节：业务模块说明 / 7. AI 模块 / 功能说明】
- **古籍OCR识文**（上传古诗词图片/书法作品/碑帖，AI精准识别文字内容，保留原文结构，标注印章题款）——**当前维护中**：后端开关 `ai.features.ocr-enabled=false` 时接口返回 `4293`，前端首页入口置灰并提示「维护中」，恢复需同时打开后端开关与前端 `OCR_ENABLED` 常量，并存在 `ocr` 模块的视觉模型绑定
- AI模型配置管理（后台管理，支持CRUD、启用/禁用、默认模型设置、连接测试）
- **AI内容填充与审核**：对诗词/诗人缺失字段由 AI 生成候选内容，存入审核表，管理员审核通过后写回原表
- **诗词AI助手（poetry_chat）**：寻章摘句页面的AI功能模块，包括译文注释、名人点评、创作背景、历史文化背景、AI逐句解析、AI诗词助手对话
  - **译文注释**：逐句翻译，关键词语注释
  - **名人点评**：历代名家点评（引用《诗品》《沧浪诗话》《人间词话》等）、文学史地位
  - **创作背景**：诗人生平、创作缘由、历史背景
  - **AI逐句解析**：点击诗句展开解释，支持多句同时展开，内存缓存不写库，可关闭
  - **弹窗交互**：三个功能按钮以向左展开的弹窗形式呈现，覆盖在诗词内容上方，不挤压布局
  - **搜索式加载**：AI生成过程中以搜索动画替代旋转加载图标

【moyuan_business_modules.md · 业务模块说明 / 7. AI 模块 / 技术方案】
【章节：业务模块说明 / 7. AI 模块 / 技术方案】
### 技术方案

- 调用大语言模型 API（智谱AI、DeepSeek、Kimi、千问、小米MiMo、OpenRouter；NVIDIA NIM 已下线）
- 模块绑定：`ai_module_config`（chat / poet_chat / poetry_chat / write_poem / analyze / couplet，以及游戏模块的 feihua_gen_ancient / feihua_gen_normal / feihua_gen_super / feihua_verify / feihua_topic / chain_generate）→ `ai_model`，由 `AiModelRegistry` 按模块解析模型；统一适配层 `OpenAiCompatibleAdapter` / `NvidiaAdapter`
- 视觉能力：视觉模型用于看图写诗与 OCR 识别；DeepSeek 视觉请求需关闭思考模式（body 顶层 `thinking.type=disabled`）
- OCR 识别复用视觉模型基础设施，通过专用提示词引导模型精准识别古籍文字
- AI模型配置存入数据库（ai_model 表），支持动态管理；api_key 部分为占位符，真实密钥从 `secrets/application-secrets.yml` 运行时注入
- 前端首页集成AI问答组件（右下角悬浮按钮）
- 前端首页"展风拓潮"模块集成看图写诗、智能分析、对联、藏头诗、平仄校验和OCR功能；「飞花令」「诗词接龙」为两张卡片入口（见第 24 章「游戏模块」）

【moyuan_business_modules.md · 业务模块说明 / 7. AI 模块 / AI模型管理接口（管理员）】
【章节：业务模块说明 / 7. AI 模块 / AI模型管理接口（管理员）】
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

【moyuan_business_modules.md · 业务模块说明 / 26. 全局 AI 小窗与系统知识库模块 / 后端与数据结构】
【章节：业务模块说明 / 26. 全局 AI 小窗与系统知识库模块 / 后端与数据结构】
### 后端与数据结构

- `system_knowledge` 表：知识条目（code/title/category/keywords/aliases/body/steps/faq/route/is_show/sort_order/deleted），种子 24 条
- `ai_module_config` 新增 `system_chat` 模块（绑定 DeepSeek），与 `chat` 模块完全隔离，避免污染诗宝阁、藏头诗等其他 AI 功能提示词
- SecurityConfig：`/api/system-knowledge` 匿名可访问；`/api/ai/**` 已移除匿名放行，未登录返回 401
- 公开接口：`GET /api/system-knowledge`（只读列表）
- 后台接口：`GET /api/admin/system-knowledge`、`GET /{id}`、`POST`、`PUT /{id}`、`PUT /{id}/show`、`DELETE /{id}`

【moyuan_business_modules.md · 业务模块说明 / 26. 全局 AI 小窗与系统知识库模块 / API 接口】
【章节：业务模块说明 / 26. 全局 AI 小窗与系统知识库模块 / API 接口】
### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/system-knowledge | 公开读取系统知识库条目 | sc-moyuan-backend SystemKnowledgeController |
| POST /api/ai/chat | 诗词助理 / 网页用法助理对话（`moduleCode` 分别为页面画像值 / `system_chat`） | sc-moyuan-backend AiController |

---

**文档版本**：v3.18
**最后更新**：2026-11-07（AI 加固收尾同步：第 25 章诗宝阁由「阶段一非流式」改为 SSE 流式 + system 人设多轮上下文 + 服务端会话（Redis，TTL 7 天，localStorage 兜底）+ 停止生成；平仄校验改述为本地确定性引擎 `POST /api/ai/tone-check`（不消耗 AI 额度）；古籍识文标注维护中（`ai.features.ocr-enabled=false`）；接口清单补 `tone-check` 与 3 个会话接口，并注明 `/api/ai/**` 统一限流与入参校验）；此前 v3.17（推荐算法说明补充「四组模型策略（离线制品查表）」与「后台实时看板」；后台控制台推荐指标屏新增缓存覆盖、模型版本、近 7 天切换趋势、切换去向/页面分布与最近事件）；v3.16（推荐算法说明由 UserCF 更新为 ItemCF，诗词行为矩阵同一作品多行为取最大值合并）；v3.15（新增第 26 章「全局 AI 小窗与系统知识库」：双助理、`system_chat` 模块、`system_knowledge` 表、路由画像、意图分流与 FAQ 直答、知识库跳转入口、登录门禁；模块总数 25 → 26）；v3.14（首页「诗境 AI」展开面板新增「前往完整页面」入口，按工具预选诗宝阁角色与能力；韵脚查询/对对联补充为诗宝阁新能力，能力总数 10 → 12；功能卡增加自动横向滑动）；2026-10-19（AI 对话页模块名统一由「诗阁」更名为「诗宝阁」；此前第 25 章「AI 对话页『诗宝阁』」补充对话流消息头像说明：AI 侧统一卡通头像 / 用户侧本人头像，气泡与头像左右对齐；v3.13 补充后端流式接口 `POST /api/ai/chat/stream`（SSE 流式）说明与接口表条目；v3.12 新增第 25 章：6 角色 × 10 能力的角色化统一对话入口，纯前端复用 `POST /api/ai/chat` 非流式接口）
**维护人员**：墨渊开发团队

【moyuan_system_workflow.md · 系统工作流文档 / 11. AI 能力域 / 11.1 AI 辅助创作与生成（E1）】
【章节：系统工作流文档 / 11. AI 能力域 / 11.1 AI 辅助创作与生成（E1）】
provider` 定位 `AiModelAdapter`，`RestTemplate` 带 `Bearer apiKey` POST 到 `ai_model.api_url`（messages=system+user、max_tokens 默认 1024）→ 解析 `choices[0].message.content` 返回；失败抛 `BusinessException`。
- **注意（代码事实）**：providers 下拉含 qwen/openrouter，但当前**无对应 Adapter**，调用报"不支持的AI提供商"；`OpenAiCompatibleAdapter` 支持 zhipu/deepseek/kimi/mimo，`NvidiaAdapter` 支持 nvidia（vision 抛"暂不支持视觉模型"）。
- **关键类与方法**：`AiController.*`、`AiServiceImpl.chat/writePoemFromImage/analyzePoem/matchCouplet/ocrImage`、`AiModelRegistry.chat/vision/chatByModule/getModel`、`OpenAiCompatibleAdapter/NvidiaAdapter`。
- **数据与缓存**：`ai_model`（name/provider/apiUrl/modelId/visionModelId/apiKey/maxTokens/isEnabled/isDefault/modelType）；内存 `AiModelRegistry.modelCache`。

【moyuan_system_workflow.md · 系统工作流文档 / 17. 关键代码速查表】
【章节：系统工作流文档 / 17. 关键代码速查表】
4（诗词 AI 评分改为独立 `rating` 模块：`PoemRatingServiceImpl.requestAiRating` 由写死默认模型改为 `aiService.analyzePoem(content, null, "rating")`，`ai_model` 字段记录实际生效模型名；后台新增「AI功能配置」页面（`/admin/ai-config`）可按模块调整模型与提示词；此前 v1.3 补充流式问答入口 `POST /api/ai/chat/stream` 调用链，v1.2 新增首页小游戏区块工作流）
**维护人员**：墨渊开发团队
**维护说明**：本文为 docs 本地维护文件，受 `.git/info/exclude` 排除不提交远程；代码变更后请依据 doc-manager 技能核对本文与实现一致性。
```
</details>

## H1（hard）搜索排序规则里，诗人名精确匹配和标题完全匹配，哪个分数更高？分别是多少？

**回答**：在搜索排序规则中，**诗人名精确匹配**的分数更高。具体分值如下：

- **诗人名精确匹配**：**0.9 分**（说明：诗人名与关键词完全一致）
- **标题完全匹配**：**1.0 分**（说明：去除标点后完全一致）

因此，**标题完全匹配（1.0 分）** 的分数实际上高于诗人名精确匹配（0.9 分）。

（来源：moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 搜索评分算法）

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 功能说明】
【章节：业务模块说明 / 8. 搜索模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 复合搜索策略】
【章节：业务模块说明 / 8. 搜索模块 / 复合搜索策略】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 搜索评分算法】
【章节：业务模块说明 / 8. 搜索模块 / 搜索评分算法】
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

【moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.4 诗词评分（C4）】
【章节：系统工作流文档 / 8. 诗词内容域 / 8.4 诗词评分（C4）】
### 8.4 诗词评分（C4）
- **触发入口**：`GET /api/poems/{poemId}/ratings`；`POST .../ratings?score&comment`；`POST .../ratings/ai`、`GET/regenerate`；`GET .../ratings/me`。
- **分步流程**：校验 score∈[1.0,5.0] → `ratePoem`（`@Transactional`）按 `poem_id+user_id+rating_type=1` 增改评分 → `updatePoemAverageScore` 重算 `avg_score/rating_count`；AI 评分 `rating_type=2`（userId=null）：`aiService.analyzePoem(content, null, "rating")` 走 `ai_module_config` 的独立 `rating` 模块绑定（不再写死默认模型，可在后台「AI功能配置」页面调整）→ 正则解析分（失败兜底 3.5）→ 摘要取前 200 字、去 markdown 标记，随后重算均分，`ai_model` 字段记录实际生效模型名。`getPoemRatings` 返回 userRatings(type=1)/aiRating(type=2 最新)/averageScore/ratingCount，异常回退空数据。
- **关键类与方法**：`PoemRatingServiceImpl.ratePoem/requestAiRating/parseAiScore/generateSummary/updatePoemAverageScore`、`AiService.analyzePoem`。`PoemRatingController.*`。
- **数据与缓存**：`poem_rating`（score/comment/rating_type:1用户2AI/ai_model/ai_summary/ai_analysis）、`poem.avg_score/rating_count`。

【moyuan_system_workflow.md · 系统工作流文档 / 9. 诗人内容域 / 9.1 诗人详情与懒加载 / 头像分配（已收录，完整见下文）】
【章节：系统工作流文档 / 9. 诗人内容域 / 9.1 诗人详情与懒加载 / 头像分配（已收录，完整见下文）】
### 9.1 诗人详情与懒加载 / 头像分配（已收录，完整见下文）
- **触发入口**：`GET /api/poets/{id}`。
- **分步流程**：`getPoetDetail`（`@Cacheable("poets","detail:"+id)`）查库并补 `dynastyName` → `isPoetInfoIncomplete` 判 biography/lifeStory/influence/anecdotes/avatar 是否为空 → 不完整则 `fetchAndSavePoetData` 调 `https://cn.apihz.cn/api/zici/poet.php`（id/key/name），按字段逐项补全（pseudonym=tag、biography=content 经 `PoetDataExtractor` 去标签并抽生卒/籍贯、按生卒年定朝代、头像、lifeStory=rwsp、influence=zycj、anecdotes=ysdg）→ 有改动则 `updateById` 并 `evict` 详情缓存。
- **头像来源优先级**：①外部 API `image` 字段（网络图）→ ②`PoetDefaultAvatar.getAvatar()` 本地 `/img/poet_avatars/*.jpg` 兜底。
- **预设头像分配规则（PoetDefaultAvatar）**：第 0 级按 `poet.style` 精准流派匹配（`STYLE_MAP`：豪放派/婉约派/田园派/现实主义/新月派/浪漫主义/古典主义，命中即用，`style` 为空则跳过）→ 第 1~3 级按 性别（已知女诗人名单）→ 地区（中西名单/无朝代）→ 时期（`poetType=="modern"`），组出 `地区_时期_性别` 键匹配头像池；候选则随机，否则「地区+性别」随机 → 同地区随机 → 全局随机。

【moyuan_system_workflow.md · 系统工作流文档 / 9. 诗人内容域 / 9.2 推荐 / 热门诗人（C7）】
【章节：系统工作流文档 / 9. 诗人内容域 / 9.2 推荐 / 热门诗人（C7）】
### 9.2 推荐 / 热门诗人（C7）
- **触发入口**：`GET /api/poets/recommend?limit=6`（上限 20）、`GET /api/poets/popular?limit=10`（上限 50）；诗词侧 `GET /api/search/poems/recommended|popular`。
- **分步流程**：取当前用户（未登录走热门）→ `userId==null` 走 `getPopularPoets`；`buildUserPoetMatrix` 由收藏/点赞/浏览行为构建 用户→诗人:{权重}（收藏 5、点赞 3、浏览 1）→ `calculateItemCollaborativeScores` 计算物品-物品余弦相似度，每个已交互诗人取 Top50 相似诗人、对未交互诗人累加 `相似度×权重`（诗词侧 `buildUserPoemMatrix` 同一作品多行为取最大值） → `getInterestBasedPoetScores` 按 `interests` 朝代加权（古典/现代/外国）→ `mergeScores` 相加取 TopN → 过滤 status=1；空回退热门。热门 `calculatePoetPopularity`（诗收藏5+词收藏5+赞3+浏览1）取 TopN。结果 `@Cacheable` 缓存。
- **关键类与方法**：`PoetController.getRecommendedPoets/getPopularPoets`、`RecommendationServiceImpl.*`、`SearchController.getRecommendedPoems/getPopularPoems`。
- **数据与缓存**：`user_favorite/user_like/user_history`、`user.interests`、`poem.view_count/like_count`；缓存 `poetRecommendation::user:{userId}:{limit}`、`popular:{limit}` 等。

【moyuan_system_workflow.md · 系统工作流文档 / 9. 诗人内容域 / 9.3 诗人认证、草稿与投稿建议（E10，合并）】
【章节：系统工作流文档 / 9. 诗人内容域 / 9.3 诗人认证、草稿与投稿建议（E10，合并）】
### 9.3 诗人认证、草稿与投稿建议（E10，合并）
- **精选诗人展示/管理触发**：`GET /api/poet-featured/random?count`、`/list`、`/{id}`；后台 CRUD。`getRandomPoetFeatured` 仅取 `status=1 且 is_show=1` 的启用项后 `Collections.shuffle` 截断；后台列表支持 keyword 按姓名/朝代筛选，并提供 `PUT /api/admin/poet-featured/{id}/show`（单个）与 `PUT /api/admin/poet-featured/batch-show`（批量）切换首页展示。
- **认证申请/审核**：`POST /api/poet-profile/apply`（落 `poet_profile`，`verifiedStatus=2` 待审，同步 `user.poet_verified=2`、`user.poet_profile_id`；他人查看屏蔽 contactInfo）；`PUT /api/admin/poet-profiles/{id}/verify` 置通过/拒绝并回写 `user.poet_verified`，记录作品/获赞数。
- **草稿投稿**：`POST /api/poet-drafts`（校验诗人存在，`section` 映射字段并快照原值，status=0 待审）；后台 `PUT /api/admin/poet-drafts/{id}/review` 通过时 `publishDraft` 按 section 把 content 写回 `poet` 对应字段。
- **投稿建议**：`POST /api/poet-suggestions`（前端 `sanitizeInput` 清洗 HTML，status=pending）；后台 `PUT /api/admin/poet-suggestions/{id}/review`。
- **待审统计**：`countByStatus` 汇总到 `GET /api/admin/audit/stats`。
- **关键类与方法**：`PoetFeaturedServiceImpl`、`PoetProfileServiceImpl.applyVerification/verifyProfile/getByUserId`、`PoetDraftServiceImpl.saveDraft/reviewDraft/publishDraft`、`PoetSuggestionServiceImpl.submitSuggestion/reviewSuggestion`。
- **数据与缓存**：`poet_featured`、`poet_profile`、`user`（poet_verified/poet_profile_id）、`poet_draft`、`poet_suggestion`。

---

【moyuan_system_workflow.md · 系统工作流文档 / 14. 内容管理/运营域 / 14.3 精选诗人展示（见 9.3）】
【章节：系统工作流文档 / 14. 内容管理/运营域 / 14.3 精选诗人展示（见 9.3）】
### 14.3 精选诗人展示（见 9.3）
- 随机/列表/详情展示启用精选诗人，后台 CRUD 维护 sortOrder/status。

---
```
</details>

## H2（hard）给一首诗点赞之后，诗作记录上的点赞数和收藏数会同步变化吗？

**回答**：知识库未收录该问题，请换一种问法。

**评级**： 

<details><summary>上下文</summary>

```

```
</details>

## H3（hard）诗宝阁和首页 AI 小工具区是什么关系？藏头诗这两处都有吗？

**回答**：诗宝阁与首页 AI 小工具区是并存且互补的关系，两者均提供 AI 功能但定位不同。

- **关系**：诗宝阁是统一的角色化对话入口（`/ai-chat`），支持多角色与能力组合；首页 AI 小工具区保留快捷玩法入口，两处功能并存，互不冲突。（来源：moyuan_business_modules.md·业务模块说明 / 25. AI 对话页「诗宝阁」模块 / 功能说明）
- **藏头诗分布**：藏头诗功能在两处均有提供。首页 AI 小工具区可直接使用；诗宝阁中“诗仙·创作”角色下也提供该能力，生成结果会高亮每句首字。（来源：moyuan_system_knowledge.md·墨渊小助手系统知识库（24 条）/ 藏头诗（play.acrostic））

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 1. 首页模块 / 功能说明】
【章节：业务模块说明 / 1. 首页模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 7. AI 模块 / 功能说明】
【章节：业务模块说明 / 7. AI 模块 / 功能说明】
### 功能说明

- AI诗词问答（支持多模型切换；调用优先走 `ai_module_config` 模块绑定，未绑定某模块时回退默认模型。可用提供商：智谱AI、DeepSeek、Kimi、千问、小米MiMo、OpenRouter；NVIDIA NIM 已全部禁用/下线）
- 看图写诗（上传图片，AI生成古诗词，支持视觉模型如 GLM-4V-Flash、DeepSeek-Flash）
- 智能分析（输入诗句进行深度解析）
- AI对对联（输入上联，AI生成下联，遵循平仄规则）
- **藏头诗**（输入 2-8 个藏头字并选择五言/七言，AI 生成藏头诗，每句首字依次对应藏头字；复用 `POST /api/ai/chat`，moduleCode=chat，不新增后端接口）
- **平仄格律校验**（粘贴一首绝句/律诗，由**本地确定性引擎** `POST /api/ai/tone-check` 逐句标注平仄（平声绿底、仄声红底）、判定是否合律并给出不合律原因；校验规则为句末三平尾/三仄尾、二四六位失替、偶数句押韵，不调用大模型、不消耗 AI 额度）
- **AI 对话页「诗宝阁」**（`/ai-chat`）：角色化统一对话入口，6 角色（诗仙·创作 / 诗话·鉴赏 / 格律·校正 / 诗友·行令 / 群贤·论诗 / 顾问·问答）× 12 能力（命题作诗 / 集句成诗 / 藏头诗 / 润色入律 / 平仄校验 / 韵脚查询 / 对对联 / 意象典故 / 诗话点评 / 飞花令 / 诗词接龙 /

【moyuan_business_modules.md · 业务模块说明 / 25. AI 对话页「诗宝阁」模块 / 功能说明】
【章节：业务模块说明 / 25. AI 对话页「诗宝阁」模块 / 功能说明】
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
- **会话记忆**：按 `roleId` 存服务端 Redis（键 `ai:chat:session:{userId}:{roleId}`，TTL 7 天、最多 40 条），切换角色各自恢复，换设备登录同一账号可继续；本地 `localStorage`（键 `moyuan_ai_chat_${roleId}`）作为降级缓存，Redis 不可用时仍可用。单条内容上限 4000 字
- **空态引导**：进入页面 / 新建会话后，按角色给出引导语 + 推荐能力 chip
- **结果渲染**：普通能力渲染为文本气泡，流式逐字追加并自动滚底；藏头诗高亮每句首字（藏头字）；平仄校验改调本地确定性引擎 `POST /api/ai/tone-check`，把返回的逐字平仄与问题列表渲染为平仄标签卡（平底/仄底，三平尾、三仄尾、二四六失替等问题标红），不再依赖大模型输出 JSON
- **消息头像**：对话流每条消息两侧带圆形头像——AI 侧统一使用墨渊可爱卡通头像（本地静态资源 `/img/ai_chat_avatar.jpg`），用户侧使用本人头像（未登录 / 无头像时回退金色首字母）；用户气泡靠右、头像居其右，AI 气泡靠左、头像居其左，加载态气泡同样带 AI 头像

【moyuan_business_modules.md · 业务模块说明 / 25. AI 对话页「诗宝阁」模块 / 前端路由】
【章节：业务模块说明 / 25. AI 对话页「诗宝阁」模块 / 前端路由】
### 前端路由

| 路径 | 说明 |
|------|------|
| `/ai-chat` | 诗宝阁 AI 对话页面（支持 `?role=xxx&skill=xxx` 预选角色与能力） |

> 首页顶部导航栏已新增「诗宝阁」入口（与「诗云」「交流广场」同级）；首页原「展风拓潮」AI 区的藏头诗 / 平仄 / 飞花令入口**保持不变，两处并存**。

【moyuan_business_modules.md · 业务模块说明 / 25. AI 对话页「诗宝阁」模块 / API 接口】
【章节：业务模块说明 / 25. AI 对话页「诗宝阁」模块 / API 接口】
### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| POST /api/ai/chat/stream | 主链路。SSE 流式对话（请求 `{ moduleCode, messages[], temperature, maxTokens }`，响应 `text/event-stream`，事件 `{"delta":"…"}` … `[DONE]`；限流/入参错误以 JSON 返回） | sc-moyuan-backend AiController |
| POST /api/ai/chat | 兜底。非流式对话（请求 `{ message, moduleCode:'chat' }`，响应 `data.reply`），仅在流式遇可重试失败时调用 | sc-moyuan-backend AiController |
| GET /api/ai/chat/sessions/{roleId} | 读取当前用户在该角色下的会话 | AiChatSessionController |
| PUT /api/ai/chat/sessions/{roleId} | 保存会话（`{"messages":[…]}`，服务端只保留最近 40 条） | AiChatSessionController |
| DELETE /api/ai/chat/sessions/{roleId} | 清空该角色会话 | AiChatSessionController |
| POST /api/ai/tone-check | 平仄校验（本地确定性引擎，不消耗 AI 额度） | sc-moyuan-backend AiController |

【moyuan_business_modules.md · 业务模块说明 / 26. 全局 AI 小窗与系统知识库模块 / 功能说明】
【章节：业务模块说明 / 26. 全局 AI 小窗与系统知识库模块 / 功能说明】
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

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 诗宝阁AI对话（play.bao-ge）】
【章节：墨渊小助手系统知识库（24 条） / 诗宝阁AI对话（play.bao-ge）】
## 诗宝阁AI对话（play.bao-ge）

- 分类：play
- 关键词：诗宝阁,AI对话,创作,鉴赏,平仄校验,藏头诗
- 别名：诗阁,诗宝阁
- 前端路由：/ai-chat
- 说明：诗宝阁是统一的 AI 对话页面，AI 以不同角色与能力帮你完成命题作诗、集句成诗、藏头诗、润色入律、平仄校验、意象典故、诗话点评、飞花令、诗词接龙等任务。可在左侧选择角色与能力后直接对话，回复以流式方式逐字输出，生成过程中可随时点击「停止」。会话按角色保存在账号下并保留 7 天，换设备登录同一账号可以接着聊。入口在顶部导航的「诗宝阁」。
- 使用步骤：
  1. 打开诗宝阁页面
  2. 选择角色与能力
  3. 输入你的需求并发送
  4. 查看 AI 返回的文本、藏头高亮或平仄标注结果
- 常见问答：
  - 问：诗宝阁是什么  答：诗宝阁是 AI 对话页面，AI 分角色和能力，可帮你作诗、鉴赏、校验平仄、玩飞花令等。
  - 问：诗宝阁和首页的AI工具有什么区别  答：诗宝阁是统一的角色化对话入口，首页 AI 小工具区保留看图写诗、藏头诗、飞花令等快捷入口，两处并存。
  - 问：诗宝阁的对话会保存吗  答：会，按角色保存在你的账号下并保留 7 天，换设备登录同一账号也能接着聊。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 藏头诗（play.acrostic）】
【章节：墨渊小助手系统知识库（24 条） / 藏头诗（play.acrostic）】
## 藏头诗（play.acrostic）

- 分类：play
- 关键词：藏头诗,藏头,写诗,五言,七言
- 别名：藏头
- 前端路由：/
- 说明：藏头诗功能可输入 2 至 8 个藏头字并选择五言或七言，由 AI 生成每句首字依次对应藏头字的诗作。入口在首页 AI 小工具区，诗宝阁「诗仙·创作」角色下也提供藏头诗能力。生成结果会高亮每句首字。
- 使用步骤：
  1. 打开首页 AI 小工具区或诗宝阁
  2. 输入 2 至 8 个藏头字
  3. 选择五言或七言
  4. 点击生成，查看首字高亮的藏头诗
- 常见问答：
  - 问：藏头诗怎么用  答：输入 2 到 8 个藏头字并选择五言或七言，AI 会生成每句首字依次对应藏头字的诗。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 首页（page.home）】
【章节：墨渊小助手系统知识库（24 条） / 首页（page.home）】
## 首页（page.home）

- 分类：page
- 关键词：首页,主页,导航,入口
- 别名：主页
- 前端路由：/
- 说明：首页是站点的总入口，聚合了今日诗词、古诗精赏、当代精选、诗话视野、精选诗人等板块，并提供通往诗云、交流广场、诗宝阁的导航。首页还设有 AI 小工具区，可快捷使用看图写诗、藏头诗、飞花令等玩法。
- 使用步骤：
  1. 打开站点默认进入首页
  2. 通过顶部导航切换各板块
  3. 在 AI 小工具区使用各类玩法
- 常见问答：
  - 问：首页有哪些内容  答：首页聚合了今日诗词、古诗精赏、当代精选、诗话视野、精选诗人等板块，还有 AI 小工具区。
```
</details>

## H4（hard）游客在首页直接玩飞花令，对局记录会保存到哪里？

**回答**：游客在首页直接玩飞花令，对局记录仅临时保存在 Redis 中，不会写入 MySQL 数据库。

具体说明如下：
1. **存储位置**：游客与未登录用户共用同一套接口，后端按登录态分层处理，游客对局数据仅存于 Redis（不落 MySQL）。
2. **保存机制**：对局运行时状态及句子信息存储在 Redis 中，键名为 `game:{gameId}` 与 `game:{gameId}:sentences`，设置 TTL 为 30 分钟，交互期间可续期。
3. **数据持久化限制**：只有登录用户对局的有效诗句才会写入 `game_record`（对局主表）和 `game_sentence`（有效诗句明细表），游客模式无此持久化操作。
4. **前端展示**：游客模式下通过 `GuestBadge` 组件展示游客标识，且无法回查历史对局记录；登录态用户方可进入 `/game/feihua` 页面并保存数据。

（来源：moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 功能说明）

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 功能说明】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 功能说明】
- **三级真实性校验链**：① 本地 `poem` 表 ngram 全文索引 `MATCH...AGAINST`（限定范围时附加 `dynasty_id` 过滤）→ ② 外部 APIHZ 复核（仅在**不限朝代**时可用，外部源无法确认朝代归属）→ ③ LLM 兜底真伪判断（`feihua_verify`，限定范围时提示词附朝代约束），全链路失败即判为「诗句真实性校验不通过」。
- **游客模式**：游客与新页面共用同一套接口，后端按登录态分层——**游客对局仅存 Redis（不落 MySQL），登录用户对局与有效诗句写入 `game_record` / `game_sentence`**；前端以 `GuestBadge` 展示游客标识，登录态用户对局可回查。
- **状态/判重后端权威**：对局运行时状态存 Redis（键 `game:{gameId}` 与 `game:{gameId}:sentences`，TTL 30 分钟，交互续期）；**超时判定由后端依前端上报的 `activeSeconds` 裁定**（见上文「超时判定」）。
- **输入方式**：支持文本与语音（`inputType`），语音仅标识，校验逻辑一致。

【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 玩法规则速览】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 玩法规则速览】
### 玩法规则速览

| 维度 | 取值 | 说明 |
|------|------|------|
| 玩法类型 | feihua / chain | 对局表 `game_type` 区分 |
| 飞花令模式 | ancient / normal / super | super 需 `superType`（char/topic） |
| 飞花令难度 | easy / medium / hard | 达标条数 easy=10、medium=20、hard=null（AI 枯竭即胜） |
| 接龙选择范围 | 不限（默认，中国古典诗词）/ 具体朝代 | 传 `dynastyId` 限定；起句、AI 接句、玩家校验均限定在该朝代内 |
| 对局结果 | win / lose / giveup | 写入 `game_record.game_result` |

【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / API 接口】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / API 接口】
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

【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 前端组件】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 前端组件】
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

【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 前端路由】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 前端路由】
### 前端路由

| 路径 | 说明 |
|------|------|
| `/game/feihua` | 飞花令对局页 |
| `/game/chain` | 诗词接龙对局页 |

> 首页 AI 小工具区提供「飞花令」「诗词接龙」两张卡片：游客点击展开内嵌小窗口（`GameMiniPanel`，直接在首页开局），登录用户跳转到 `/game/feihua`、`/game/chain` 游戏页。

【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 数据库表】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 数据库表】
### 数据库表

- `game_record`：游戏对局主表（飞花令/诗词接龙共用，`game_type` 区分）
- `game_sentence`：对局有效诗句明细表（`uk_game_sentence` 唯一键兜底判重）

---

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.5 首页数据组装（K5）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.5 首页数据组装（K5）】
### 15.5 首页数据组装（K5）
- **轮播图**：静态 `/img/lb_*.jpg`、`/img/lb_shiwen_*.png`，无后端。
- **今日诗词**：`DailyPoetry` → 后端 `GET /api/external-poetry/jinrishici` 代理（`v2.jinrishici.com/one.json`，后端 60 分钟进程内缓存，前端不再直连）。
- **首页导航**：`GET /home-navigation`。
- **精选诗人**：`getRandomPoetFeatured(8)`，去重取 4 位，每 20s 轮换。
- **论坛预览**：`GET /forum/posts?pageSize=5`。
- **古诗精赏**：本地 json + `GET /search/poems` + `/search/poems/recommended`。
- **当代精选**：本地 json + like/favorite/comment 接口。
- **AI 区块**：`/ai/write-poem`、`/ai/analyze`、`/ai/couplet`、`/ai/ocr`、`/ai/chat`（藏头诗、平仄格律校验，复用 `POST /api/ai/chat` 并传 `moduleCode=chat`，不新增后端接口）、`/rhyme/*`；"诗海拾贝"本地 json。
- **小游戏区块**：首页 AI 小工具区「飞花令」「诗词接龙」两张卡片——游客点击展开内嵌小窗口（`GameMiniPanel`）直接开局，登录用户跳转 `/game/feihua`、`/game/chain`；后端为独立游戏模块（`/api/game/feihua`、`/api/game/chain`，见 `modules.md` 第 24 章）。
- **诗话视野**：`GET /vision/articles`。
- **诗词卡片跳转**：首页「古诗精赏 / 当代精选」卡片点击经组合式函数 `composables/usePoemCardNavigation.ts` 处理——本地命中直接 `router.push('/poem/{id}')`，未命中回退 `PoetryDetailDialog`（外部诗词弹窗，可由接口盒子实时增强）。
- **关键位置**：`views/home/index.vue`、`components/{home,business}/*`、`composables/usePoemCardNavigation.ts`、`api/modules/*`。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 飞花令（play.feihualing）】
【章节：墨渊小助手系统知识库（24 条） / 飞花令（play.feihualing）】
## 飞花令（play.feihualing）

- 分类：play
- 关键词：飞花令,对诗,令字,行酒令,诗词游戏,怎么玩
- 别名：花令,飞花令游戏
- 前端路由：/game/feihua
- 说明：飞花令是与 AI 对句的诗词游戏，你每回合说出一句含指定「令字」的真实古诗词名句，AI 轮流接句，接不上或超时即判负。入口在首页 AI 小工具区的「飞花令」卡片，登录用户点击后进入对局页。有古制严格、标准版、超级飞花令三种模式与三档难度可选。现代诗不参与本游戏。
- 使用步骤：
  1. 选择模式与难度
  2. 设定令字（如「花」）
  3. 每回合提交一句含令字的古诗词，单回合限时 30 秒
  4. 与 AI 交替接句，AI 接不上或你超时判负
- 常见问答：
  - 问：飞花令怎么玩  答：每回合提交一句含指定令字的真实古诗词名句，与 AI 交替接句，AI 接不上或你超时则对方判负。
  - 问：在哪里能玩飞花令  答：首页 AI 小工具区有「飞花令」卡片，登录用户点击进入对局页。
  - 问：飞花令有几种模式  答：有古制严格、标准版、超级飞花令三种模式，难度分简单、中等、困难三档。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 游客与登录用户的权限差异（account.guest-limit）】
【章节：墨渊小助手系统知识库（24 条） / 游客与登录用户的权限差异（account.guest-limit）】
## 游客与登录用户的权限差异（account.guest-limit）

- 分类：account
- 关键词：游客,未登录,权限,差异,登录
- 别名：游客权限
- 说明：未登录的游客可以浏览诗词、诗人、帖子与文章，也能在首页直接体验飞花令、诗词接龙等小工具（对局仅临时保存）。发布诗词、发帖、评论、点赞、收藏、报修、诗人认证等操作需要登录后才能使用。
- 使用步骤：
  1. 未登录时浏览诗词等公开内容
  2. 登录后解锁发布、评论、收藏等操作
- 常见问答：
  - 问：不登录能用吗  答：不登录可以浏览诗词、诗人、帖子与文章，也能在首页体验飞花令等小工具；发布、评论、收藏等需要登录。
```
</details>

## H5（hard）藏头诗最多能藏几个字？除了首页还能从哪里进入？

**回答**：藏头诗最多能藏 8 个字；除首页外，还可从“诗宝阁”的「诗仙·创作」角色下进入。

- **字数限制**：输入 2 至 8 个藏头字（来源：moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条）/ 藏头诗（play.acrostic））。
- **入口位置**：
  - 首页 AI 小工具区（来源同上）。
  - 诗宝阁「诗仙·创作」角色下（来源同上）。

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 2. 诗词模块 / 功能说明】
【章节：业务模块说明 / 2. 诗词模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 9. 用户模块 / API 接口】
【章节：业务模块说明 / 9. 用户模块 / API 接口】
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

【moyuan_business_modules.md · 业务模块说明 / 9. 用户模块 / 用户主页功能】
【章节：业务模块说明 / 9. 用户模块 / 用户主页功能】
### 用户主页功能

用户主页展示以下信息：
- **用户基本信息**：头像、昵称、简介、性别、生日
- **诗人认证状态**：是否已认证、认证诗人资料
- **统计数据**：作品数、获赞数、被收藏数、粉丝数
- **作品列表**：用户发布的诗词和帖子

【moyuan_business_modules.md · 业务模块说明 / 9. 用户模块 / 前端路由】
【章节：业务模块说明 / 9. 用户模块 / 前端路由】
### 前端路由

| 路径 | 说明 |
|------|------|
| `/user/login` | 登录页 |
| `/user/register` | 注册页 |
| `/user/profile` | 个人中心（含诗人认证申请入口） |
| `/user/:id/homepage` | 用户主页（新增） |

---

【moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.3 收藏 / 点赞（C3）】
【章节：系统工作流文档 / 8. 诗词内容域 / 8.3 收藏 / 点赞（C3）】
### 8.3 收藏 / 点赞（C3）
- **触发入口**：`POST|GET /api/poems/{id}/like`、`POST|GET /api/poems/{id}/favorite`、`GET /api/poems/favorites`。
- **分步流程**：点赞 `toggleLike`（`@Transactional`）按 `user_id+target_id+target_type=POEM` 计数 `user_like`：存在则删记录且 `like_count-1`（`gt 0` 保护），否则插入并 `+1`；状态查询 `isLike/isFavorite` 计数判定；`getFavorites` 取收藏 ID 集后 `in` 查 poem 并填充。
- **注意（代码事实）**：收藏 `UserServiceImpl.toggleFavorite` **只写 `user_favorite` 表，不维护 `poem.favorite_count`**；点赞会维护 `poem.like_count`。
- **数据与缓存**：`user_like`、`user_favorite`、`poem.like_count`。

【moyuan_system_workflow.md · 系统工作流文档 / 9. 诗人内容域 / 9.1 诗人详情与懒加载 / 头像分配（已收录，完整见下文）】
【章节：系统工作流文档 / 9. 诗人内容域 / 9.1 诗人详情与懒加载 / 头像分配（已收录，完整见下文）】
头像池共 11 张（`cn_ancient_male`×3、`cn_ancient_female`×1、`cn_modern_male/female`×1、`west_classical_male/female`×1、`west_romantic_male/female`×1、`west_modern_female`×1）；`poet.style` 字段经 `docs/database/migration_003_poet_style.sql` 新增，外部诗人 API 暂无流派字段。
- **关键类与方法**：`PoetServiceImpl.getPoetDetail/isPoetInfoIncomplete/fetchAndSavePoetData`、`PoetDataExtractor`、`PoetDefaultAvatar.getAvatar`。
- **数据与缓存**：`poet`（biography/year_info/birthplace/pseudonym/life_story/influence/anecdotes/avatar）；缓存 `poets::detail:{id}`。

【moyuan_system_workflow.md · 系统工作流文档 / 9. 诗人内容域 / 9.2 推荐 / 热门诗人（C7）】
【章节：系统工作流文档 / 9. 诗人内容域 / 9.2 推荐 / 热门诗人（C7）】
### 9.2 推荐 / 热门诗人（C7）
- **触发入口**：`GET /api/poets/recommend?limit=6`（上限 20）、`GET /api/poets/popular?limit=10`（上限 50）；诗词侧 `GET /api/search/poems/recommended|popular`。
- **分步流程**：取当前用户（未登录走热门）→ `userId==null` 走 `getPopularPoets`；`buildUserPoetMatrix` 由收藏/点赞/浏览行为构建 用户→诗人:{权重}（收藏 5、点赞 3、浏览 1）→ `calculateItemCollaborativeScores` 计算物品-物品余弦相似度，每个已交互诗人取 Top50 相似诗人、对未交互诗人累加 `相似度×权重`（诗词侧 `buildUserPoemMatrix` 同一作品多行为取最大值） → `getInterestBasedPoetScores` 按 `interests` 朝代加权（古典/现代/外国）→ `mergeScores` 相加取 TopN → 过滤 status=1；空回退热门。热门 `calculatePoetPopularity`（诗收藏5+词收藏5+赞3+浏览1）取 TopN。结果 `@Cacheable` 缓存。
- **关键类与方法**：`PoetController.getRecommendedPoets/getPopularPoets`、`RecommendationServiceImpl.*`、`SearchController.getRecommendedPoems/getPopularPoems`。
- **数据与缓存**：`user_favorite/user_like/user_history`、`user.interests`、`poem.view_count/like_count`；缓存 `poetRecommendation::user:{userId}:{limit}`、`popular:{limit}` 等。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 藏头诗（play.acrostic）】
【章节：墨渊小助手系统知识库（24 条） / 藏头诗（play.acrostic）】
## 藏头诗（play.acrostic）

- 分类：play
- 关键词：藏头诗,藏头,写诗,五言,七言
- 别名：藏头
- 前端路由：/
- 说明：藏头诗功能可输入 2 至 8 个藏头字并选择五言或七言，由 AI 生成每句首字依次对应藏头字的诗作。入口在首页 AI 小工具区，诗宝阁「诗仙·创作」角色下也提供藏头诗能力。生成结果会高亮每句首字。
- 使用步骤：
  1. 打开首页 AI 小工具区或诗宝阁
  2. 输入 2 至 8 个藏头字
  3. 选择五言或七言
  4. 点击生成，查看首字高亮的藏头诗
- 常见问答：
  - 问：藏头诗怎么用  答：输入 2 到 8 个藏头字并选择五言或七言，AI 会生成每句首字依次对应藏头字的诗。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 首页（page.home）】
【章节：墨渊小助手系统知识库（24 条） / 首页（page.home）】
## 首页（page.home）

- 分类：page
- 关键词：首页,主页,导航,入口
- 别名：主页
- 前端路由：/
- 说明：首页是站点的总入口，聚合了今日诗词、古诗精赏、当代精选、诗话视野、精选诗人等板块，并提供通往诗云、交流广场、诗宝阁的导航。首页还设有 AI 小工具区，可快捷使用看图写诗、藏头诗、飞花令等玩法。
- 使用步骤：
  1. 打开站点默认进入首页
  2. 通过顶部导航切换各板块
  3. 在 AI 小工具区使用各类玩法
- 常见问答：
  - 问：首页有哪些内容  答：首页聚合了今日诗词、古诗精赏、当代精选、诗话视野、精选诗人等板块，还有 AI 小工具区。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 寻章摘句（page.search）】
【章节：墨渊小助手系统知识库（24 条） / 寻章摘句（page.search）】
## 寻章摘句（page.search）

- 分类：page
- 关键词：寻章摘句,搜索,检索,找诗,诗词列表
- 别名：诗词列表,搜索页
- 前端路由：/poem
- 说明：寻章摘句即诗词列表页，支持按朝代、分类、诗人筛选，也能用关键词搜索诗词；题库只含古诗，现代诗单独成页。全局搜索能力内嵌在首页与诗词列表页中，没有单独的搜索页。未登录用户也能浏览与搜索。
- 使用步骤：
  1. 进入诗词列表页
  2. 通过筛选项或关键词查找诗词
  3. 点开卡片查看诗词详情
- 常见问答：
  - 问：寻章摘句在哪里  答：寻章摘句就是诗词列表页，可在顶部导航进入，支持筛选与关键词搜索。
  - 问：怎么搜索诗词  答：在诗词列表页输入关键词即可搜索，也可按朝代、分类、诗人筛选。
```
</details>

## H6（hard）诗词接龙开局限定朝代以后，哪些环节会被限制在该朝代内？

**回答**：在诗词接龙游戏中，若玩家于开局阶段选定特定朝代范围，以下三个核心环节均会被严格限制在该朝代内进行校验与生成：

1. **AI 起句来源**：系统从本地诗词库随机选取的起始诗句，仅限定于所选朝代范围内（来源：moyuan_business_modules.md·业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙）/ 功能说明）。
2. **AI 接句生成**：AI 在接续用户诗句时，其提示词及生成逻辑均被限定为仅检索该朝代内的诗句（来源：moyuan_business_modules.md·业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙）/ 功能说明）。
3. **真实性校验范围**：无论是玩家提交的答案，还是 AI 接续的句子，其真伪判定均限定在该朝代内；若诗句超出该朝代范围，系统将判定为「诗句真实性校验不通过」并提示越界（来源：moyuan_business_modules.md·业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙）/ 功能说明）。

此外，现代诗不参与本游戏，且若未限定朝代，则默认不限朝代范围（来源：moyuan_system_knowledge.md·墨渊小助手系统知识库（24 条）/ 诗词接龙（play.chain））。

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 功能说明】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 功能说明】
### 功能说明

- **飞花令**（`/game/feihua`）：按「令字」接句，用户每回合提交一句含令字的真实古诗词名句。
  - 三种模式：`ancient`（古制严格，令字须落在本回合第 N 个汉字位置）、`normal`（标准版，诗句包含令字即可）、`super`（超级飞花令，含 `char` 多关键字 / `topic` 主题两种子类型）
  - 三种难度：`easy`（达标 10 句即胜）、`medium`（达标 20 句即胜）、`hard`（AI 无句可接则用户胜，无固定目标条数）
  - 每题给出 AI 候选诗句（LLM 生成 + 后端按位次/判重/真实性二次过滤后下发）
- **诗词接龙**（`/game/chain`）：以 AI 起句的末字为首字接出下一句名句，AI 再接给用户，交替进行；本地诗词库随机取起句（不调用 LLM）；AI 接不上时用户胜，链长即有效句数。
  - **选择范围**：配置阶段可选「中国古典诗词（不限朝代，默认）」或具体朝代（取自 `dynasty` 表）；选定后 AI 起句仅在该朝代内取句，AI 接句提示词限定该朝代，且**玩家提交与 AI 接句的真实性校验均限定在该朝代内**（超出范围判为「诗句真实性校验不通过」并提示越界）。
- **回合计时（计时/监测/缓冲三子态，打字即暂停）**：单回合有效答题时长 30 秒。

【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 功能说明】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 功能说明】
- **回合计时（计时/监测/缓冲三子态，打字即暂停）**：单回合有效答题时长 30 秒。点击「开始挑战」后倒计时**直接开始**（`counting`，30 秒递减）；输入框一旦有变化即**暂停倒计时**并进入 `monitoring`（监测停笔 5 秒），仅当连续 5 秒无改动才恢复递减；30 秒耗尽后自动进入 `buffer`（5 秒缓冲窗口，期内有输入变动仍可提交）；缓冲耗尽仍无改动则由前端自动提交并判超时。有效答题时长上限 30 + 5 = 35 秒；**题面计时均为显示层，可被主动提交（作答/放弃）随时终止**。答错不重置计时，答对推进下一回合并重置计时。
- **超时判定（后端按上报有效时长）**：前端每次作答请求上报 `activeSeconds`（本回合已消耗的有效秒数），后端 `GameConstants.isTimeout(activeSeconds, wallElapsedMillis)` 优先据此判定（`>= 35` 即超时）；未上报时回退墙钟口径（服务端 `roundStartTime` 毫秒时间戳，阈值 30 秒 + 1 秒网络宽限）。
- **容错**：每回合初始 2 次重试，累计错误达 3 次判负；空输入不计错误、不消耗重试、不重置计时。
- **三级真实性校验链**：① 本地 `poem` 表 ngram 全文索引 `MATCH...

【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 功能说明】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 功能说明】
- **三级真实性校验链**：① 本地 `poem` 表 ngram 全文索引 `MATCH...AGAINST`（限定范围时附加 `dynasty_id` 过滤）→ ② 外部 APIHZ 复核（仅在**不限朝代**时可用，外部源无法确认朝代归属）→ ③ LLM 兜底真伪判断（`feihua_verify`，限定范围时提示词附朝代约束），全链路失败即判为「诗句真实性校验不通过」。
- **游客模式**：游客与新页面共用同一套接口，后端按登录态分层——**游客对局仅存 Redis（不落 MySQL），登录用户对局与有效诗句写入 `game_record` / `game_sentence`**；前端以 `GuestBadge` 展示游客标识，登录态用户对局可回查。
- **状态/判重后端权威**：对局运行时状态存 Redis（键 `game:{gameId}` 与 `game:{gameId}:sentences`，TTL 30 分钟，交互续期）；**超时判定由后端依前端上报的 `activeSeconds` 裁定**（见上文「超时判定」）。
- **输入方式**：支持文本与语音（`inputType`），语音仅标识，校验逻辑一致。

【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 玩法规则速览】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 玩法规则速览】
### 玩法规则速览

| 维度 | 取值 | 说明 |
|------|------|------|
| 玩法类型 | feihua / chain | 对局表 `game_type` 区分 |
| 飞花令模式 | ancient / normal / super | super 需 `superType`（char/topic） |
| 飞花令难度 | easy / medium / hard | 达标条数 easy=10、medium=20、hard=null（AI 枯竭即胜） |
| 接龙选择范围 | 不限（默认，中国古典诗词）/ 具体朝代 | 传 `dynastyId` 限定；起句、AI 接句、玩家校验均限定在该朝代内 |
| 对局结果 | win / lose / giveup | 写入 `game_record.game_result` |

【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / API 接口】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / API 接口】
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

【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 前端路由】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 前端路由】
### 前端路由

| 路径 | 说明 |
|------|------|
| `/game/feihua` | 飞花令对局页 |
| `/game/chain` | 诗词接龙对局页 |

> 首页 AI 小工具区提供「飞花令」「诗词接龙」两张卡片：游客点击展开内嵌小窗口（`GameMiniPanel`，直接在首页开局），登录用户跳转到 `/game/feihua`、`/game/chain` 游戏页。

【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 数据库表】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 数据库表】
### 数据库表

- `game_record`：游戏对局主表（飞花令/诗词接龙共用，`game_type` 区分）
- `game_sentence`：对局有效诗句明细表（`uk_game_sentence` 唯一键兜底判重）

---

【moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.5 外部诗词导入（C5）】
【章节：系统工作流文档 / 8. 诗词内容域 / 8.5 外部诗词导入（C5）】
### 8.5 外部诗词导入（C5）
- **触发入口**：`POST /api/poems/import-external`（title/content/author/dynasty）；`POST /api/poems/fix-external-poems`。
- **分步流程**：校验 title → 按 dynasty 名称查朝代 → 按 author 查诗人（无则新建 Poet 并 `clearAllCache`）→ 按 `title+poetId` 查重：命中补全空缺字段返回 `{id,imported:false}`，未命中落库（`source=external`、`poemType=ancient`、`isOriginal=0`、`auditStatus=1`、status=1）并清 `poems` 缓存返回 `{id,imported:true}`。修复接口取 `source=external` 且 `poet_id is null` 前 100 条，调 `https://cn.apihz.cn/api/zici/poetry.php` 补全作者朝代，成功后清 `poets`/`poems` 缓存返回 `{total,fixed}`。
- **关键类与方法**：`PoemController.importExternalPoem/fixExternalPoems`、`PoemServiceImpl`、`PoetDefaultAvatar.getAvatar`。
- **数据与缓存**：`poem.source/is_original/poem_type/audit_status`；清缓存 key：`poems`、`poets`。

---

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 诗词接龙（play.chain）】
【章节：墨渊小助手系统知识库（24 条） / 诗词接龙（play.chain）】
## 诗词接龙（play.chain）

- 分类：play
- 关键词：诗词接龙,接龙,接句,首尾相接,诗词游戏
- 别名：接龙游戏,诗龙
- 前端路由：/game/chain
- 说明：诗词接龙由 AI 起句，你接出的诗句首字须与上一句末字相同，再交给 AI 接，交替进行；一方接不上即判负。入口在首页 AI 小工具区的「诗词接龙」卡片，登录后进入对局页。开局可限定朝代范围，起句、AI 接句与你的作答都会在该朝代内校验。现代诗不参与本游戏。
- 使用步骤：
  1. 选择朝代范围（默认中国古典诗词不限朝代）
  2. AI 给出起句，你接出首字等于上句末字的真实古诗名句
  3. 与 AI 交替接句
  4. 接不上或超时判负，链长即有效句数
- 常见问答：
  - 问：诗词接龙怎么玩  答：AI 起句后，你接的诗句首字要和上一句末字相同，AI 再接，交替进行，接不上的一方判负。
  - 问：接龙可以限定朝代吗  答：可以，开局选择具体朝代后，起句、AI 接句与你的作答都会限定在该朝代内校验。
```
</details>

## H7（hard）诗云页面的配色实验室能调哪些显示参数？一共几套配色方案？

**回答**：诗云页面“配色实验室”可调节以下显示参数：光晕强度、连线可见度、节点尺寸、整体亮度（通过画布容器 CSS filter 实现），以及「显示诗人姓名」和「诗人按朝代配色」开关。

- **可调参数**：光晕强度、连线可见度、节点尺寸、整体亮度、显示诗人姓名、诗人按朝代配色（来源：moyuan_business_modules.md·业务模块说明 / 23. 诗云模块 / 功能说明；moyuan_system_workflow.md·系统工作流文档 / 15.6 诗云 3D 星空（K6））
- **配色方案数量**：共 7 套（琥珀星海、青玉、珊瑚、冰川蓝、梅子紫、苔绿、赤陶）（来源：moyuan_business_modules.md·业务模块说明 / 23. 诗云模块 / 功能说明；moyuan_system_workflow.md·系统工作流文档 / 15.6 诗云 3D 星空（K6））
- **参数驱动机制**：由 `reactive(viz)` 驱动，`applyVizStyles()` 统一重涂背景、雾效、节点与连线；光晕/连线/尺寸换算基准为 glow/65、line/55、size/100（来源：moyuan_business_modules.md·业务模块说明 / 23. 诗云模块 / 功能说明；moyuan_system_workflow.md·系统工作流文档 / 15.6 诗云 3D 星空（K6））

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 3. 诗人模块 / 功能说明】
【章节：业务模块说明 / 3. 诗人模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 23. 诗云模块 / 功能说明】
【章节：业务模块说明 / 23. 诗云模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 23. 诗云模块 / 技术方案】
【章节：业务模块说明 / 23. 诗云模块 / 技术方案】
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

【moyuan_business_modules.md · 业务模块说明 / 23. 诗云模块 / 前端组件】
【章节：业务模块说明 / 23. 诗云模块 / 前端组件】
### 前端组件

| 组件 | 说明 |
|------|------|
| poetry-cloud/index.vue | 诗云主页面（Three.js 3D 渲染、数据加载、交互控制、悬浮提示） |

【moyuan_business_modules.md · 业务模块说明 / 23. 诗云模块 / API 接口】
【章节：业务模块说明 / 23. 诗云模块 / API 接口】
### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/dynasties | 获取朝代列表 | backend DynastyController |
| GET /api/poets | 获取诗人列表（pageSize 300，用于 3D 星系） | sc-moyuan-backend PoetController |
| GET /api/poems | 获取诗词列表（pageSize 1000，用于诗作节点与计数） | backend PoemController |

【moyuan_business_modules.md · 业务模块说明 / 23. 诗云模块 / 前端路由】
【章节：业务模块说明 / 23. 诗云模块 / 前端路由】
### 前端路由

| 路径 | 说明 |
|------|------|
| `/poetry-cloud` | 诗云星空页面（hideHeader） |

【moyuan_business_modules.md · 业务模块说明 / 23. 诗云模块 / 数据可视化】
【章节：业务模块说明 / 23. 诗云模块 / 数据可视化】
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

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.6 诗云 3D 星空（K6）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.6 诗云 3D 星空（K6）】
### 15.6 诗云 3D 星空（K6）
- **数据加载**：`Promise.all` 并发 `GET /dynasties`、`/poets(pageSize 300)`、`/poems(pageSize 1000)`，失败回退内置 12 朝代。
- **渲染**：`initScene`（Scene+FogExp2+Camera+WebGLRenderer(ACESFilmicToneMapping)+OrbitControls）→ 3200 背景星（Sphere+ShaderMaterial）→ 诗人星（发光小球+光晕，缓存 `poetPositionMap`）+ 同朝代贝塞尔赠诗线 → 诗作独立 Mesh 以「诗人为中心、诗作环绕分布」的球面螺旋生成并经松弛迭代拉开间距，按诗人分组的 LineSegments2 连线 → 交互：hover 高亮并显示 DOM 提示框、点击 `selectPoet→flyToStar+loadPoetPoems+focusPoetPoems`、点击诗作 `selectPoem`、朝代筛选重建。
- **配色实验室**：底部控制模块由 `reactive(viz)` 驱动（7 套方案 + 光晕/连线/尺寸/亮度滑杆 + 标签与朝代配色开关），`applyVizStyles()` 统一重涂背景/雾效、诗人节点、诗作节点与两类连线；整体亮度经画布容器 CSS `filter: brightness()`。
- **诗人姓名标注**：DOM 层 `.label-layer` + `updatePoetLabels()`，每帧 `getWorldPosition().project(camera)` 更新标签屏幕坐标，相机背后/视口外自动隐藏。
- **卸载**：`onUnmounted` 释放 geometry/material、清理标签与定时器、`scene.clear()`、`renderer.dispose()`。
- **数据与缓存**：数据仅内存 ref，不做 localStorage；`poetPositionMap` 内存缓存 3D 坐标，`poemCountMap` 缓存每位诗人的诗作数（用于节点尺寸）。

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.7 路由守卫（K7）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.7 路由守卫（K7）】
### 15.7 路由守卫（K7）
- **分步流程**：`beforeEach` 按 `meta.title` 设 document.title → `requiresAuth` 未登录跳 `Login`（带 redirect）；已登录缺 userInfo 先 `fetchUserInfo`；`requiresAdmin` 校验 role !== admin 弹错回 Home；已登录访问 Login/Register 回 Home。`afterEach` 维护 `appStore.cachedViews`（keep-alive）。
- **关键位置**：`router/guards.ts`、`router/index.ts`（`meta`: requiresAuth/requiresAdmin/keepAlive）、`stores/app.ts`。

---

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 诗云星空（play.poetry-cloud）】
【章节：墨渊小助手系统知识库（24 条） / 诗云星空（play.poetry-cloud）】
## 诗云星空（play.poetry-cloud）

- 分类：play
- 关键词：诗云,星空,3D,诗词星图,可视化
- 别名：诗云,诗云星空图
- 前端路由：/poetry-cloud
- 说明：诗云是用 3D 星空展示古诗数据的可视化页面，每位诗人是一颗星，其诗作环绕分布，可缩放旋转浏览。点击诗人星可查看诗人信息与代表作品，点击诗作节点可查看诗词详情。页面底部「配色实验室」可切换配色方案并调节光晕、连线、亮度等显示效果。
- 使用步骤：
  1. 进入诗云页面
  2. 拖动旋转、滚轮缩放浏览星图
  3. 点击诗人星查看诗人信息与作品
  4. 点击诗作节点查看诗词详情
- 常见问答：
  - 问：诗云是什么  答：诗云是把古诗词数据做成 3D 星空的页面，诗人是星、诗作围绕其分布，可交互浏览。
  - 问：诗云怎么用  答：拖动旋转、滚轮缩放浏览星图，点击诗人或诗作节点查看详情。
```
</details>

## H8（hard）AI 评分的分数是怎么从模型输出里解析出来的？解析失败时的兜底分是多少？

**回答**：知识库未收录该项。

资料中仅说明 AI 评分由 AI 从意境、格律等维度评价并给出分数，但未提及分数具体如何从模型输出解析、解析失败的兜底分值或相关代码逻辑（来源：moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条）/ 诗词 AI 评分（play.rating））。

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 7. AI 模块 / 功能说明】
【章节：业务模块说明 / 7. AI 模块 / 功能说明】
### 功能说明

- AI诗词问答（支持多模型切换；调用优先走 `ai_module_config` 模块绑定，未绑定某模块时回退默认模型。可用提供商：智谱AI、DeepSeek、Kimi、千问、小米MiMo、OpenRouter；NVIDIA NIM 已全部禁用/下线）
- 看图写诗（上传图片，AI生成古诗词，支持视觉模型如 GLM-4V-Flash、DeepSeek-Flash）
- 智能分析（输入诗句进行深度解析）
- AI对对联（输入上联，AI生成下联，遵循平仄规则）
- **藏头诗**（输入 2-8 个藏头字并选择五言/七言，AI 生成藏头诗，每句首字依次对应藏头字；复用 `POST /api/ai/chat`，moduleCode=chat，不新增后端接口）
- **平仄格律校验**（粘贴一首绝句/律诗，由**本地确定性引擎** `POST /api/ai/tone-check` 逐句标注平仄（平声绿底、仄声红底）、判定是否合律并给出不合律原因；校验规则为句末三平尾/三仄尾、二四六位失替、偶数句押韵，不调用大模型、不消耗 AI 额度）
- **AI 对话页「诗宝阁」**（`/ai-chat`）：角色化统一对话入口，6 角色（诗仙·创作 / 诗话·鉴赏 / 格律·校正 / 诗友·行令 / 群贤·论诗 / 顾问·问答）× 12 能力（命题作诗 / 集句成诗 / 藏头诗 / 润色入律 / 平仄校验 / 韵脚查询 / 对对联 / 意象典故 / 诗话点评 / 飞花令 / 诗词接龙 /

【moyuan_business_modules.md · 业务模块说明 / 7. AI 模块 / AI 内容填充与审核】
【章节：业务模块说明 / 7. AI 模块 / AI 内容填充与审核】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 搜索评分算法】
【章节：业务模块说明 / 8. 搜索模块 / 搜索评分算法】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 推荐算法说明】
【章节：业务模块说明 / 8. 搜索模块 / 推荐算法说明】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 前端组件】
【章节：业务模块说明 / 8. 搜索模块 / 前端组件】
### 前端组件

| 组件 | 说明 |
|------|------|
| poem/list.vue - 筛选区域 | 朝代/分类/诗人/排序筛选器，支持复合搜索 |
| home/index.vue - 历史的印痕模块 | 首页搜索框、搜索结果展示、为您推荐标签页 |
| 搜索结果卡片 | 显示诗词信息、来源标签、推荐理由 |
| components/admin/RecommendationScreen.vue | 后台控制台推荐指标屏：离线四组指标 + 实时缓存/切换看板 |

【moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / 功能说明】
【章节：业务模块说明 / 16. 诗词评分模块 / 功能说明】
### 功能说明

- 用户评分：用户对诗词进行评分（1.0-5.0分）
- AI评分：请求AI对诗词进行评分和分析
- 评分维度：支持多维度评分（格律、意境、用词、情感、创新）
- 评分统计：诗词平均评分和评分数量
- 评分详情：展示用户评分和AI评分详情
- 重新评价：用户已评分后可点击"重新评价"修改评分

【moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / 评分类型】
【章节：业务模块说明 / 16. 诗词评分模块 / 评分类型】
### 评分类型

| 类型 | 说明 |
|------|------|
| 用户评分 | 用户手动评分，可附带评语 |
| AI评分 | AI自动评分，附带AI分析内容 |

【moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / 评分维度】
【章节：业务模块说明 / 16. 诗词评分模块 / 评分维度】
### 评分维度

| 维度 | 说明 |
|------|------|
| 格律 | 诗词的格律规范性 |
| 意境 | 诗词的意境表达 |
| 用词 | 诗词的用词精准度 |
| 情感 | 诗词的情感表达 |
| 创新 | 诗词的创新程度 |

【moyuan_system_workflow.md · 系统工作流文档 / 17. 关键代码速查表】
【章节：系统工作流文档 / 17. 关键代码速查表】
## 17. 关键代码速查表

| 关注点 | 文件 |
|--------|------|
| 认证入口 / JWT | `controller/AuthController.java`、`util/JwtUtil.java`、`security/JwtAuthenticationFilter.java`、`config/SecurityConfig.java` |
| 文件上传 | `controller/FileController.java`、`service/impl/FileStorageServiceImpl.java` |
| 诗人懒加载 / 头像 | `service/impl/PoetServiceImpl.java`、`util/PoetDefaultAvatar.java`、`util/PoetDataExtractor.java` |
| 推荐 / 搜索 / 押韵 | `service/impl/RecommendationServiceImpl.java`、`service/impl/SmartSearchServiceImpl.java`、`service/impl/RhymeServiceImpl.java` |
| AI | `service/impl/AiServiceImpl.java`、`ai/AiModelRegistry.java`、`service/impl/AiGeneratedContentServiceImpl.java` |
| 论坛 / 评论 / 工单 | `service/impl/ForumPostServiceImpl.java`、`service/impl/CommentServiceImpl.java`、`service/impl/RepairOrderServiceImpl.java` |
| 操作日志 / 访问统计 | `aspect/OperationLogAspect.java`、`aspect/VisitLogAspect.java` |
| 统一响应 / 异常 | `common/R.java`、`common/ResultCode.java`、`exception/GlobalExceptionHandler.java` |
| 前端请求 / 存储 | `frontend/src/utils/request.ts`、`frontend/src/utils/storage.ts` |
| 前端状态 / 守卫 | `frontend/src/stores/*`、`frontend/src/router/guards.ts` |

---

**文档版本**：v1.5
**最后更新**：2026-10-26（推荐流程由 UserCF 更新为 ItemCF：物品-物品余弦相似度 + Top-50 相似物品加权，诗词行为矩阵同一作品多行为取最大值合并）；此前 v1.4（诗词 AI 评分改为独立 `rating` 模块：`PoemRatingServiceImpl.requestAiRating` 由写死默认模型改为 `aiService.analyzePoem(content, null,

【moyuan_system_workflow.md · 系统工作流文档 / 17. 关键代码速查表】
【章节：系统工作流文档 / 17. 关键代码速查表】
4（诗词 AI 评分改为独立 `rating` 模块：`PoemRatingServiceImpl.requestAiRating` 由写死默认模型改为 `aiService.analyzePoem(content, null, "rating")`，`ai_model` 字段记录实际生效模型名；后台新增「AI功能配置」页面（`/admin/ai-config`）可按模块调整模型与提示词；此前 v1.3 补充流式问答入口 `POST /api/ai/chat/stream` 调用链，v1.2 新增首页小游戏区块工作流）
**维护人员**：墨渊开发团队
**维护说明**：本文为 docs 本地维护文件，受 `.git/info/exclude` 排除不提交远程；代码变更后请依据 doc-manager 技能核对本文与实现一致性。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 诗词AI评分（play.rating）】
【章节：墨渊小助手系统知识库（24 条） / 诗词AI评分（play.rating）】
## 诗词AI评分（play.rating）

- 分类：play
- 关键词：AI评分,诗词评分,评分,打分
- 别名：AI打分,诗词打分
- 前端路由：/poem/:id
- 说明：诗词详情页提供 AI 评分，由 AI 从意境、格律、修辞、情感、用词等维度评价诗词并给出分数。进入任意诗词详情页即可查看或请求 AI 评分，用户本人也可以手动给诗词评分。
- 使用步骤：
  1. 打开任意诗词详情页
  2. 查看已有评分或请求 AI 评分
  3. 查看 AI 给出的评价与分数
- 常见问答：
  - 问：AI评分在哪里  答：在诗词详情页可以查看或请求 AI 评分，AI 会多维度评价并给出分数。
```
</details>

## H9（hard）未登录时能查看自己的浏览历史吗？能体验哪些诗词游戏？

**回答**：未登录用户无法查看自己的浏览历史，需登录后才能在个人中心查看并清空记录（来源：moyuan_system_knowledge.md·墨渊小助手系统知识库（24 条）/ 浏览历史（feature.history）；moyuan_business_modules.md·业务模块说明 / 5. 用户与个人中心域 / 5.2 浏览历史（B2））。

未登录时可体验的诗词游戏包括飞花令和诗词接龙，这些功能仅在首页提供临时保存的对局体验，发布、评论、收藏等操作均需登录后才能进行（来源：moyuan_system_knowledge.md·墨渊小助手系统知识库（24 条）/ 游客与登录用户的权限差异（account.guest-limit））。

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 2. 诗词模块 / 功能说明】
【章节：业务模块说明 / 2. 诗词模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 2. 诗词模块 / API 接口】
【章节：业务模块说明 / 2. 诗词模块 / API 接口】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 搜索流程】
【章节：业务模块说明 / 8. 搜索模块 / 搜索流程】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / API 接口】
【章节：业务模块说明 / 8. 搜索模块 / API 接口】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 智能搜索参数】
【章节：业务模块说明 / 8. 搜索模块 / 智能搜索参数】
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

【moyuan_system_workflow.md · 系统工作流文档 / 5. 用户与个人中心域 / 5.2 浏览历史（B2）】
【章节：系统工作流文档 / 5. 用户与个人中心域 / 5.2 浏览历史（B2）】
### 5.2 浏览历史（B2）
- **目标**：记录用户访问的诗词/诗人历史并支持查看/清理。
- **触发入口**：`GET /api/history`、`POST /api/history`、`DELETE /api/history`。
- **分步流程**：访问时经 `UserHistoryServiceImpl` 记录（target_type/target_id），列表分页返回，支持按 `DELETE` 清空。
- **关键类与方法**：`HistoryController`、`UserHistoryServiceImpl`。
- **数据与缓存**：`user_history`（前端不落 localStorage，走服务端接口）。

---

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 诗云星空（play.poetry-cloud）】
【章节：墨渊小助手系统知识库（24 条） / 诗云星空（play.poetry-cloud）】
## 诗云星空（play.poetry-cloud）

- 分类：play
- 关键词：诗云,星空,3D,诗词星图,可视化
- 别名：诗云,诗云星空图
- 前端路由：/poetry-cloud
- 说明：诗云是用 3D 星空展示古诗数据的可视化页面，每位诗人是一颗星，其诗作环绕分布，可缩放旋转浏览。点击诗人星可查看诗人信息与代表作品，点击诗作节点可查看诗词详情。页面底部「配色实验室」可切换配色方案并调节光晕、连线、亮度等显示效果。
- 使用步骤：
  1. 进入诗云页面
  2. 拖动旋转、滚轮缩放浏览星图
  3. 点击诗人星查看诗人信息与作品
  4. 点击诗作节点查看诗词详情
- 常见问答：
  - 问：诗云是什么  答：诗云是把古诗词数据做成 3D 星空的页面，诗人是星、诗作围绕其分布，可交互浏览。
  - 问：诗云怎么用  答：拖动旋转、滚轮缩放浏览星图，点击诗人或诗作节点查看详情。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 寻章摘句（page.search）】
【章节：墨渊小助手系统知识库（24 条） / 寻章摘句（page.search）】
## 寻章摘句（page.search）

- 分类：page
- 关键词：寻章摘句,搜索,检索,找诗,诗词列表
- 别名：诗词列表,搜索页
- 前端路由：/poem
- 说明：寻章摘句即诗词列表页，支持按朝代、分类、诗人筛选，也能用关键词搜索诗词；题库只含古诗，现代诗单独成页。全局搜索能力内嵌在首页与诗词列表页中，没有单独的搜索页。未登录用户也能浏览与搜索。
- 使用步骤：
  1. 进入诗词列表页
  2. 通过筛选项或关键词查找诗词
  3. 点开卡片查看诗词详情
- 常见问答：
  - 问：寻章摘句在哪里  答：寻章摘句就是诗词列表页，可在顶部导航进入，支持筛选与关键词搜索。
  - 问：怎么搜索诗词  答：在诗词列表页输入关键词即可搜索，也可按朝代、分类、诗人筛选。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 智能检索（feature.search）】
【章节：墨渊小助手系统知识库（24 条） / 智能检索（feature.search）】
## 智能检索（feature.search）

- 分类：feature
- 关键词：搜索,检索,找诗,搜诗人,关键字
- 别名：智能搜索,全文搜索
- 前端路由：/poem
- 说明：智能检索支持按关键词搜索诗词、诗人与帖子，首页与诗词列表页内嵌了搜索入口。搜索会记录你的历史，登录后还会根据兴趣给出推荐。
- 使用步骤：
  1. 在首页或诗词列表页输入关键词
  2. 查看匹配的诗词、诗人或帖子
  3. 点开结果查看详情
- 常见问答：
  - 问：怎么搜索  答：在首页或诗词列表页的搜索框输入关键词，即可搜索诗词、诗人与帖子。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 浏览历史（feature.history）】
【章节：墨渊小助手系统知识库（24 条） / 浏览历史（feature.history）】
## 浏览历史（feature.history）

- 分类：feature
- 关键词：浏览历史,历史记录,最近看过
- 别名：历史记录
- 前端路由：/user/profile
- 说明：浏览历史记录你访问过的诗词与诗人，登录后可在个人中心查看，也支持清空。历史通过服务端接口保存，不存放在本地。
- 使用步骤：
  1. 登录后浏览诗词或诗人
  2. 在个人中心查看浏览历史
  3. 需要时清空历史记录
- 常见问答：
  - 问：浏览历史在哪  答：登录后在个人中心可以查看浏览历史，并支持清空。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 游客与登录用户的权限差异（account.guest-limit）】
【章节：墨渊小助手系统知识库（24 条） / 游客与登录用户的权限差异（account.guest-limit）】
## 游客与登录用户的权限差异（account.guest-limit）

- 分类：account
- 关键词：游客,未登录,权限,差异,登录
- 别名：游客权限
- 说明：未登录的游客可以浏览诗词、诗人、帖子与文章，也能在首页直接体验飞花令、诗词接龙等小工具（对局仅临时保存）。发布诗词、发帖、评论、点赞、收藏、报修、诗人认证等操作需要登录后才能使用。
- 使用步骤：
  1. 未登录时浏览诗词等公开内容
  2. 登录后解锁发布、评论、收藏等操作
- 常见问答：
  - 问：不登录能用吗  答：不登录可以浏览诗词、诗人、帖子与文章，也能在首页体验飞花令等小工具；发布、评论、收藏等需要登录。
```
</details>

## H10（hard）诗云里诗人节点的大小由什么决定？3D 坐标数据存在哪里？

**回答**：在诗云模块中，诗人节点大小由其名下诗作数量自适应决定；3D 坐标数据仅内存缓存，不持久化存储。

1. **诗人节点大小决定因素**：
   - 随诗人作品数量自适应调整（来源：moyuan_business_modules.md·业务模块说明/23. 诗云模块/数据可视化）。
   - 计算公式为 `starSize = 2.4 + log10(作品数+1) * 1.2`，并带有 1.8 倍光晕球（来源：moyuan_business_modules.md·业务模块说明/23. 诗云模块/数据可视化）。

2. **3D 坐标数据存储位置**：
   - 仅内存缓存，不做 localStorage 持久化存储（来源：moyuan_system_workflow.md·系统工作流文档/15. 前端数据流转层/15.6 诗云 3D 星空（K6））。
   - 具体通过 `poetPositionMap` 在内存中缓存 3D 坐标，同时 `poemCountMap` 缓存每位诗人的诗作数用于计算节点尺寸（来源：moyuan_system_workflow.md·系统工作流文档/15. 前端数据流转层/15.6 诗云 3D 星空（K6））。

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 3. 诗人模块 / 功能说明】
【章节：业务模块说明 / 3. 诗人模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 14. 精选诗人模块 / 功能说明】
【章节：业务模块说明 / 14. 精选诗人模块 / 功能说明】
### 功能说明

- 精选诗人展示（首页卡片展示，仅展示 `is_show=1` 的诗人）
- 精选诗人详情查看
- 精选诗人管理（后台管理，支持按姓名/朝代筛选、单个/批量设置首页展示开关）

【moyuan_business_modules.md · 业务模块说明 / 23. 诗云模块 / 功能说明】
【章节：业务模块说明 / 23. 诗云模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 23. 诗云模块 / 技术方案】
【章节：业务模块说明 / 23. 诗云模块 / 技术方案】
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

【moyuan_business_modules.md · 业务模块说明 / 23. 诗云模块 / API 接口】
【章节：业务模块说明 / 23. 诗云模块 / API 接口】
### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/dynasties | 获取朝代列表 | backend DynastyController |
| GET /api/poets | 获取诗人列表（pageSize 300，用于 3D 星系） | sc-moyuan-backend PoetController |
| GET /api/poems | 获取诗词列表（pageSize 1000，用于诗作节点与计数） | backend PoemController |

【moyuan_business_modules.md · 业务模块说明 / 23. 诗云模块 / 前端路由】
【章节：业务模块说明 / 23. 诗云模块 / 前端路由】
### 前端路由

| 路径 | 说明 |
|------|------|
| `/poetry-cloud` | 诗云星空页面（hideHeader） |

【moyuan_business_modules.md · 业务模块说明 / 23. 诗云模块 / 数据可视化】
【章节：业务模块说明 / 23. 诗云模块 / 数据可视化】
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

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.6 诗云 3D 星空（K6）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.6 诗云 3D 星空（K6）】
### 15.6 诗云 3D 星空（K6）
- **数据加载**：`Promise.all` 并发 `GET /dynasties`、`/poets(pageSize 300)`、`/poems(pageSize 1000)`，失败回退内置 12 朝代。
- **渲染**：`initScene`（Scene+FogExp2+Camera+WebGLRenderer(ACESFilmicToneMapping)+OrbitControls）→ 3200 背景星（Sphere+ShaderMaterial）→ 诗人星（发光小球+光晕，缓存 `poetPositionMap`）+ 同朝代贝塞尔赠诗线 → 诗作独立 Mesh 以「诗人为中心、诗作环绕分布」的球面螺旋生成并经松弛迭代拉开间距，按诗人分组的 LineSegments2 连线 → 交互：hover 高亮并显示 DOM 提示框、点击 `selectPoet→flyToStar+loadPoetPoems+focusPoetPoems`、点击诗作 `selectPoem`、朝代筛选重建。
- **配色实验室**：底部控制模块由 `reactive(viz)` 驱动（7 套方案 + 光晕/连线/尺寸/亮度滑杆 + 标签与朝代配色开关），`applyVizStyles()` 统一重涂背景/雾效、诗人节点、诗作节点与两类连线；整体亮度经画布容器 CSS `filter: brightness()`。
- **诗人姓名标注**：DOM 层 `.label-layer` + `updatePoetLabels()`，每帧 `getWorldPosition().project(camera)` 更新标签屏幕坐标，相机背后/视口外自动隐藏。
- **卸载**：`onUnmounted` 释放 geometry/material、清理标签与定时器、`scene.clear()`、`renderer.dispose()`。
- **数据与缓存**：数据仅内存 ref，不做 localStorage；`poetPositionMap` 内存缓存 3D 坐标，`poemCountMap` 缓存每位诗人的诗作数（用于节点尺寸）。

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.7 路由守卫（K7）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.7 路由守卫（K7）】
### 15.7 路由守卫（K7）
- **分步流程**：`beforeEach` 按 `meta.title` 设 document.title → `requiresAuth` 未登录跳 `Login`（带 redirect）；已登录缺 userInfo 先 `fetchUserInfo`；`requiresAdmin` 校验 role !== admin 弹错回 Home；已登录访问 Login/Register 回 Home。`afterEach` 维护 `appStore.cachedViews`（keep-alive）。
- **关键位置**：`router/guards.ts`、`router/index.ts`（`meta`: requiresAuth/requiresAdmin/keepAlive）、`stores/app.ts`。

---

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 诗云星空（play.poetry-cloud）】
【章节：墨渊小助手系统知识库（24 条） / 诗云星空（play.poetry-cloud）】
## 诗云星空（play.poetry-cloud）

- 分类：play
- 关键词：诗云,星空,3D,诗词星图,可视化
- 别名：诗云,诗云星空图
- 前端路由：/poetry-cloud
- 说明：诗云是用 3D 星空展示古诗数据的可视化页面，每位诗人是一颗星，其诗作环绕分布，可缩放旋转浏览。点击诗人星可查看诗人信息与代表作品，点击诗作节点可查看诗词详情。页面底部「配色实验室」可切换配色方案并调节光晕、连线、亮度等显示效果。
- 使用步骤：
  1. 进入诗云页面
  2. 拖动旋转、滚轮缩放浏览星图
  3. 点击诗人星查看诗人信息与作品
  4. 点击诗作节点查看诗词详情
- 常见问答：
  - 问：诗云是什么  答：诗云是把古诗词数据做成 3D 星空的页面，诗人是星、诗作围绕其分布，可交互浏览。
  - 问：诗云怎么用  答：拖动旋转、滚轮缩放浏览星图，点击诗人或诗作节点查看详情。
```
</details>

## E1（easy）用户评分和 AI 评分的评分范围是多少？分别调用哪个接口？

**回答**：用户评分范围为 1.0-5.0 分，AI 评分范围同样为 1.0-5.0 分（由 AI 生成后解析）。

**用户评分接口：**
- `POST /api/poems/{id}/ratings`：用于提交用户手动评分。

**AI 评分接口：**
- `POST /api/poems/{id}/ratings/ai`：用于请求 AI 对诗词进行评分和分析。

（来源：moyuan_business_modules.md · 业务模块说明 / 2. 诗词模块 / API 接口；moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.4 诗词评分（C4）；moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / 功能说明）

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 2. 诗词模块 / 功能说明】
【章节：业务模块说明 / 2. 诗词模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 2. 诗词模块 / API 接口】
【章节：业务模块说明 / 2. 诗词模块 / API 接口】
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

【moyuan_business_modules.md · 业务模块说明 / 2. 诗词模块 / 评分维度】
【章节：业务模块说明 / 2. 诗词模块 / 评分维度】
### 评分维度

诗词评分支持以下维度：
- **格律**：诗词的格律规范性
- **意境**：诗词的意境表达
- **用词**：诗词的用词精准度
- **情感**：诗词的情感表达
- **创新**：诗词的创新程度

【moyuan_business_modules.md · 业务模块说明 / 7. AI 模块 / API 接口】
【章节：业务模块说明 / 7. AI 模块 / API 接口】
features.ocr-enabled=false` 时返回 `4293`） | sc-moyuan-backend AiController |
| POST /api/ai/tone-check | 平仄格律校验（本地确定性引擎，不消耗 AI 额度） | sc-moyuan-backend AiController |
| POST /api/ai/couplet | AI对对联 | sc-moyuan-backend AiController |
| GET /api/ai/config/{moduleCode} | 获取模块AI配置（提示词模板、回答风格等） | sc-moyuan-backend AiController |
| POST /api/ai/preview | AI生成预览（不写入数据库） | sc-moyuan-backend AiContentController |
| POST /api/ai/submit-review | 提交AI内容到审核表 | sc-moyuan-backend AiContentController |
| POST /api/ai/fill-content | 触发AI填充（直接生成并存库） | sc-moyuan-backend AiContentController |
| GET /api/ai/fill-status/{targetType}/{targetId} | 查询某目标的AI填充状态 | sc-moyuan-backend AiContentController |

> **调用保护**：上述 `/api/ai/**` 接口统一经 `AiGuardService` 按用户限流（默认 60 次/分、1000 次/天、100 万 token/天，admin 豁免），入参长度与 `maxTokens`/`temperature` 由 `AiInputValidator` 统一校验，超限返回 `4290`，限流返回 `4291`，额度用尽返回 `4292`。详见 `docs/api/endpoints.md`。

**获取模块AI配置**：根据 moduleCode 返回对应模块的配置信息，包含提示词模板（promptTemplate）、最大回答长度（maxLength）、回答风格（responseStyle）、首次回答长度（firstResponseLength）等。前端用于构建 AI 对话的系统约束。

【moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / 功能说明】
【章节：业务模块说明 / 16. 诗词评分模块 / 功能说明】
### 功能说明

- 用户评分：用户对诗词进行评分（1.0-5.0分）
- AI评分：请求AI对诗词进行评分和分析
- 评分维度：支持多维度评分（格律、意境、用词、情感、创新）
- 评分统计：诗词平均评分和评分数量
- 评分详情：展示用户评分和AI评分详情
- 重新评价：用户已评分后可点击"重新评价"修改评分

【moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / 评分类型】
【章节：业务模块说明 / 16. 诗词评分模块 / 评分类型】
### 评分类型

| 类型 | 说明 |
|------|------|
| 用户评分 | 用户手动评分，可附带评语 |
| AI评分 | AI自动评分，附带AI分析内容 |

【moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / API 接口】
【章节：业务模块说明 / 16. 诗词评分模块 / API 接口】
### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/poems/{id}/ratings | 获取诗词评分详情 | sc-moyuan-backend PoemRatingController |
| POST /api/poems/{id}/ratings | 用户评分 | sc-moyuan-backend PoemRatingController |
| POST /api/poems/{id}/ratings/ai | 请求AI评分 | sc-moyuan-backend PoemRatingController |
| GET /api/poems/{id}/ratings/ai | 获取AI评分 | sc-moyuan-backend PoemRatingController |
| GET /api/poems/{id}/ratings/me | 获取当前用户评分 | sc-moyuan-backend PoemRatingController |

【moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.4 诗词评分（C4）】
【章节：系统工作流文档 / 8. 诗词内容域 / 8.4 诗词评分（C4）】
### 8.4 诗词评分（C4）
- **触发入口**：`GET /api/poems/{poemId}/ratings`；`POST .../ratings?score&comment`；`POST .../ratings/ai`、`GET/regenerate`；`GET .../ratings/me`。
- **分步流程**：校验 score∈[1.0,5.0] → `ratePoem`（`@Transactional`）按 `poem_id+user_id+rating_type=1` 增改评分 → `updatePoemAverageScore` 重算 `avg_score/rating_count`；AI 评分 `rating_type=2`（userId=null）：`aiService.analyzePoem(content, null, "rating")` 走 `ai_module_config` 的独立 `rating` 模块绑定（不再写死默认模型，可在后台「AI功能配置」页面调整）→ 正则解析分（失败兜底 3.5）→ 摘要取前 200 字、去 markdown 标记，随后重算均分，`ai_model` 字段记录实际生效模型名。`getPoemRatings` 返回 userRatings(type=1)/aiRating(type=2 最新)/averageScore/ratingCount，异常回退空数据。
- **关键类与方法**：`PoemRatingServiceImpl.ratePoem/requestAiRating/parseAiScore/generateSummary/updatePoemAverageScore`、`AiService.analyzePoem`。`PoemRatingController.*`。
- **数据与缓存**：`poem_rating`（score/comment/rating_type:1用户2AI/ai_model/ai_summary/ai_analysis）、`poem.avg_score/rating_count`。

【moyuan_system_workflow.md · 系统工作流文档 / 17. 关键代码速查表】
【章节：系统工作流文档 / 17. 关键代码速查表】
4（诗词 AI 评分改为独立 `rating` 模块：`PoemRatingServiceImpl.requestAiRating` 由写死默认模型改为 `aiService.analyzePoem(content, null, "rating")`，`ai_model` 字段记录实际生效模型名；后台新增「AI功能配置」页面（`/admin/ai-config`）可按模块调整模型与提示词；此前 v1.3 补充流式问答入口 `POST /api/ai/chat/stream` 调用链，v1.2 新增首页小游戏区块工作流）
**维护人员**：墨渊开发团队
**维护说明**：本文为 docs 本地维护文件，受 `.git/info/exclude` 排除不提交远程；代码变更后请依据 doc-manager 技能核对本文与实现一致性。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 诗词AI评分（play.rating）】
【章节：墨渊小助手系统知识库（24 条） / 诗词AI评分（play.rating）】
## 诗词AI评分（play.rating）

- 分类：play
- 关键词：AI评分,诗词评分,评分,打分
- 别名：AI打分,诗词打分
- 前端路由：/poem/:id
- 说明：诗词详情页提供 AI 评分，由 AI 从意境、格律、修辞、情感、用词等维度评价诗词并给出分数。进入任意诗词详情页即可查看或请求 AI 评分，用户本人也可以手动给诗词评分。
- 使用步骤：
  1. 打开任意诗词详情页
  2. 查看已有评分或请求 AI 评分
  3. 查看 AI 给出的评价与分数
- 常见问答：
  - 问：AI评分在哪里  答：在诗词详情页可以查看或请求 AI 评分，AI 会多维度评价并给出分数。
```
</details>

## E2（easy）浏览历史保存在哪里？前端会把它写进浏览器本地存储吗？

**回答**：浏览历史不保存在浏览器本地存储（localStorage），而是通过服务端接口记录。

- **保存位置**：后端数据库表 `user_history`，由 `UserHistoryServiceImpl` 服务处理（来源：moyuan_system_workflow.md·5.2 浏览历史（B2））。
- **前端存储方式**：前端不写入 localStorage，收藏、AI 对话及浏览历史均走服务端接口或内存缓存（来源：moyuan_system_workflow.md·15.4 localStorage 使用点汇总（K4）；moyuan_business_modules.md·诗人模块/功能说明）。
- **访问入口**：用户需登录后在个人中心页面查看，支持通过 `GET /api/history` 获取、`DELETE /api/history` 清空（来源：moyuan_business_modules.md·12. 浏览历史模块/API 接口；moyuan_system_knowledge.md·浏览历史/说明）。
- **数据流转**：访问诗词或诗人时经后端记录 target_type/target_id，列表分页返回，关键类为 `HistoryController`（来源：moyuan_system_workflow.md·5.2 浏览历史（B2））。

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 3. 诗人模块 / 功能说明】
【章节：业务模块说明 / 3. 诗人模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 12. 浏览历史模块 / API 接口】
【章节：业务模块说明 / 12. 浏览历史模块 / API 接口】
### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| POST /api/history | 添加浏览历史 | sc-moyuan-backend HistoryController |
| GET /api/history | 获取浏览历史 | sc-moyuan-backend HistoryController |
| DELETE /api/history | 清空浏览历史 | sc-moyuan-backend HistoryController |

---

【moyuan_system_workflow.md · 系统工作流文档 / 5. 用户与个人中心域 / 5.2 浏览历史（B2）】
【章节：系统工作流文档 / 5. 用户与个人中心域 / 5.2 浏览历史（B2）】
### 5.2 浏览历史（B2）
- **目标**：记录用户访问的诗词/诗人历史并支持查看/清理。
- **触发入口**：`GET /api/history`、`POST /api/history`、`DELETE /api/history`。
- **分步流程**：访问时经 `UserHistoryServiceImpl` 记录（target_type/target_id），列表分页返回，支持按 `DELETE` 清空。
- **关键类与方法**：`HistoryController`、`UserHistoryServiceImpl`。
- **数据与缓存**：`user_history`（前端不落 localStorage，走服务端接口）。

---

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.4 localStorage 使用点汇总（K4）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.4 localStorage 使用点汇总（K4）】
### 15.4 localStorage 使用点汇总（K4）
> **代码事实核对**：收藏、AI 对话、浏览历史**并未真正写入 localStorage**（收藏走服务端、AI 聊天在内存 ref、历史走 `/history` API）。真实落盘 key 如下：
- `token`/`refreshToken`（登录态双 token，user Store 直接写）
- `lastAdminUsername`（admin 记录）
- `home_nav_animation_config`（首页导航动画配置）
- `moyuan_token`（storage 封装的登录 token，带过期结构）
- `moyuan_user_preferences`（theme/language/fontSize/pageSize/sidebarCollapsed）
- `moyuan_search_history`（上限 20 条）
- `moyuan_form_draft_poem/forum/comment`
- `moyuan_sort_preferences`（{poem,poet,forum}）
- `moyuan_cache_dynasties/categories/poets`（筛选下拉，1h）
- `moyuan_cache_external_poem_`（外部诗词详情，24h）
- `moyuan_cache_`（外部搜索结果缓存）
- **收藏/AI对话/浏览历史**：无 localStorage key，分别走服务端接口/内存/API。
- **关键位置**：`utils/storage.ts`、`stores/preferences.ts/searchHistory.ts/formDraft.ts/apiCache.ts`。

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.5 首页数据组装（K5）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.5 首页数据组装（K5）】
### 15.5 首页数据组装（K5）
- **轮播图**：静态 `/img/lb_*.jpg`、`/img/lb_shiwen_*.png`，无后端。
- **今日诗词**：`DailyPoetry` → 后端 `GET /api/external-poetry/jinrishici` 代理（`v2.jinrishici.com/one.json`，后端 60 分钟进程内缓存，前端不再直连）。
- **首页导航**：`GET /home-navigation`。
- **精选诗人**：`getRandomPoetFeatured(8)`，去重取 4 位，每 20s 轮换。
- **论坛预览**：`GET /forum/posts?pageSize=5`。
- **古诗精赏**：本地 json + `GET /search/poems` + `/search/poems/recommended`。
- **当代精选**：本地 json + like/favorite/comment 接口。
- **AI 区块**：`/ai/write-poem`、`/ai/analyze`、`/ai/couplet`、`/ai/ocr`、`/ai/chat`（藏头诗、平仄格律校验，复用 `POST /api/ai/chat` 并传 `moduleCode=chat`，不新增后端接口）、`/rhyme/*`；"诗海拾贝"本地 json。
- **小游戏区块**：首页 AI 小工具区「飞花令」「诗词接龙」两张卡片——游客点击展开内嵌小窗口（`GameMiniPanel`）直接开局，登录用户跳转 `/game/feihua`、`/game/chain`；后端为独立游戏模块（`/api/game/feihua`、`/api/game/chain`，见 `modules.md` 第 24 章）。
- **诗话视野**：`GET /vision/articles`。
- **诗词卡片跳转**：首页「古诗精赏 / 当代精选」卡片点击经组合式函数 `composables/usePoemCardNavigation.ts` 处理——本地命中直接 `router.push('/poem/{id}')`，未命中回退 `PoetryDetailDialog`（外部诗词弹窗，可由接口盒子实时增强）。
- **关键位置**：`views/home/index.vue`、`components/{home,business}/*`、`composables/usePoemCardNavigation.ts`、`api/modules/*`。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 诗宝阁AI对话（play.bao-ge）】
【章节：墨渊小助手系统知识库（24 条） / 诗宝阁AI对话（play.bao-ge）】
## 诗宝阁AI对话（play.bao-ge）

- 分类：play
- 关键词：诗宝阁,AI对话,创作,鉴赏,平仄校验,藏头诗
- 别名：诗阁,诗宝阁
- 前端路由：/ai-chat
- 说明：诗宝阁是统一的 AI 对话页面，AI 以不同角色与能力帮你完成命题作诗、集句成诗、藏头诗、润色入律、平仄校验、意象典故、诗话点评、飞花令、诗词接龙等任务。可在左侧选择角色与能力后直接对话，回复以流式方式逐字输出，生成过程中可随时点击「停止」。会话按角色保存在账号下并保留 7 天，换设备登录同一账号可以接着聊。入口在顶部导航的「诗宝阁」。
- 使用步骤：
  1. 打开诗宝阁页面
  2. 选择角色与能力
  3. 输入你的需求并发送
  4. 查看 AI 返回的文本、藏头高亮或平仄标注结果
- 常见问答：
  - 问：诗宝阁是什么  答：诗宝阁是 AI 对话页面，AI 分角色和能力，可帮你作诗、鉴赏、校验平仄、玩飞花令等。
  - 问：诗宝阁和首页的AI工具有什么区别  答：诗宝阁是统一的角色化对话入口，首页 AI 小工具区保留看图写诗、藏头诗、飞花令等快捷入口，两处并存。
  - 问：诗宝阁的对话会保存吗  答：会，按角色保存在你的账号下并保留 7 天，换设备登录同一账号也能接着聊。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 个人中心（page.profile）】
【章节：墨渊小助手系统知识库（24 条） / 个人中心（page.profile）】
## 个人中心（page.profile）

- 分类：page
- 关键词：个人中心,我的,资料,收藏,历史,认证
- 别名：个人主页,我的页面
- 前端路由：/user/profile
- 说明：个人中心可查看与修改个人资料、上传头像、修改密码，并查看我的收藏、我的点赞、我的帖子和浏览历史。诗人认证申请也从个人中心提交。需要登录后进入。
- 使用步骤：
  1. 登录后进入个人中心
  2. 修改资料、头像或密码
  3. 查看收藏、点赞、帖子与浏览历史
  4. 需要时提交诗人认证申请
- 常见问答：
  - 问：个人中心在哪里  答：登录后可从用户菜单进入个人中心，管理资料、收藏、历史并提交诗人认证。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 智能检索（feature.search）】
【章节：墨渊小助手系统知识库（24 条） / 智能检索（feature.search）】
## 智能检索（feature.search）

- 分类：feature
- 关键词：搜索,检索,找诗,搜诗人,关键字
- 别名：智能搜索,全文搜索
- 前端路由：/poem
- 说明：智能检索支持按关键词搜索诗词、诗人与帖子，首页与诗词列表页内嵌了搜索入口。搜索会记录你的历史，登录后还会根据兴趣给出推荐。
- 使用步骤：
  1. 在首页或诗词列表页输入关键词
  2. 查看匹配的诗词、诗人或帖子
  3. 点开结果查看详情
- 常见问答：
  - 问：怎么搜索  答：在首页或诗词列表页的搜索框输入关键词，即可搜索诗词、诗人与帖子。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 浏览历史（feature.history）】
【章节：墨渊小助手系统知识库（24 条） / 浏览历史（feature.history）】
## 浏览历史（feature.history）

- 分类：feature
- 关键词：浏览历史,历史记录,最近看过
- 别名：历史记录
- 前端路由：/user/profile
- 说明：浏览历史记录你访问过的诗词与诗人，登录后可在个人中心查看，也支持清空。历史通过服务端接口保存，不存放在本地。
- 使用步骤：
  1. 登录后浏览诗词或诗人
  2. 在个人中心查看浏览历史
  3. 需要时清空历史记录
- 常见问答：
  - 问：浏览历史在哪  答：登录后在个人中心可以查看浏览历史，并支持清空。
```
</details>

## E3（easy）飞花令单回合限时多久？有几种模式和几档难度？

**回答**：飞花令单回合有效答题时长为**30 秒**。系统设有计时/监测/缓冲三子态机制，其中缓冲窗口为 5 秒，有效答题时长上限总计 35 秒（来源：moyuan_business_modules.md·业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 功能说明）。

关于模式与难度配置如下：
1. **三种模式**：`ancient`（古制严格，令字须落在本回合第 N 个汉字位置）、`normal`（标准版，诗句包含令字即可）、`super`（超级飞花令，含 `char` 多关键字或 `topic` 主题两种子类型）（来源：moyuan_business_modules.md·业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 模块总览】
【章节：业务模块说明 / 模块总览】
| 诗词外部获取内容的缓存管理 | 前端+后端已实现 |
| 22 | 联系我们模块 | 用户反馈表单页面 | 前端已实现 |
| 23 | 诗云模块 | Three.js 3D 星空可视化，诗人—诗作两级层级星图 + 配色实验室 | 前端已实现 |
| 24 | 游戏模块 | 飞花令 / 诗词接龙对局（模式/难度、回合计时、三级真实性校验、游客模式） | 前端+后端已实现 |
| 25 | AI 对话页「诗宝阁」 | 角色化统一对话入口：6 角色 × 12 能力，覆盖命题作诗/集句/藏头/润色/平仄校验/韵脚查询/对对联/意象典故/诗话点评/飞花令/接龙/群贤论诗；其中飞花令为设令字后逐句判定（含字/判重/真实性）的对局流程 | 前端+后端已实现（SSE 流式、多轮上下文、服务端会话） |
| 26 | 全局 AI 小窗与系统知识库 | 全站悬浮 AI 问答入口：诗词助理 / 网页用法助理双助理，接入 system_knowledge 知识库 | 前端+后端已实现 |

---

【moyuan_business_modules.md · 业务模块说明 / 7. AI 模块 / 功能说明】
【章节：业务模块说明 / 7. AI 模块 / 功能说明】
### 功能说明

- AI诗词问答（支持多模型切换；调用优先走 `ai_module_config` 模块绑定，未绑定某模块时回退默认模型。可用提供商：智谱AI、DeepSeek、Kimi、千问、小米MiMo、OpenRouter；NVIDIA NIM 已全部禁用/下线）
- 看图写诗（上传图片，AI生成古诗词，支持视觉模型如 GLM-4V-Flash、DeepSeek-Flash）
- 智能分析（输入诗句进行深度解析）
- AI对对联（输入上联，AI生成下联，遵循平仄规则）
- **藏头诗**（输入 2-8 个藏头字并选择五言/七言，AI 生成藏头诗，每句首字依次对应藏头字；复用 `POST /api/ai/chat`，moduleCode=chat，不新增后端接口）
- **平仄格律校验**（粘贴一首绝句/律诗，由**本地确定性引擎** `POST /api/ai/tone-check` 逐句标注平仄（平声绿底、仄声红底）、判定是否合律并给出不合律原因；校验规则为句末三平尾/三仄尾、二四六位失替、偶数句押韵，不调用大模型、不消耗 AI 额度）
- **AI 对话页「诗宝阁」**（`/ai-chat`）：角色化统一对话入口，6 角色（诗仙·创作 / 诗话·鉴赏 / 格律·校正 / 诗友·行令 / 群贤·论诗 / 顾问·问答）× 12 能力（命题作诗 / 集句成诗 / 藏头诗 / 润色入律 / 平仄校验 / 韵脚查询 / 对对联 / 意象典故 / 诗话点评 / 飞花令 / 诗词接龙 /

【moyuan_business_modules.md · 业务模块说明 / 7. AI 模块 / 功能说明】
【章节：业务模块说明 / 7. AI 模块 / 功能说明】
/ 集句成诗 / 藏头诗 / 润色入律 / 平仄校验 / 韵脚查询 / 对对联 / 意象典故 / 诗话点评 / 飞花令 / 诗词接龙 / 群贤论诗）；对话走 `POST /api/ai/chat/stream`（SSE 流式，网络类失败自动回退非流式 `POST /api/ai/chat`），请求携带 system 人设 + 最近 12 条历史作为多轮上下文，生成中可停止；会话按角色存服务端 Redis（`GET/PUT/DELETE /api/ai/chat/sessions/{roleId}`，TTL 7 天）并以 localStorage 兜底，详见第 25 章
- **飞花令 / 诗词接龙**：原为纯前端小工具（复用 `POST /api/ai/chat`），现已升级为**独立游戏模块**（见第 24 章「游戏模块」），拥有专属后端接口 `/api/game/feihua`、`/api/game/chain` 与 `ai_module_config` 游戏模块绑定；首页 AI 区块保留「飞花令」「诗词接龙」两张卡片入口。
- **古籍OCR识文**（上传古诗词图片/书法作品/碑帖，AI精准识别文字内容，保留原文结构，标注印章题款）——**当前维护中**：后端开关 `ai.features.

【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 功能说明】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 功能说明】
### 功能说明

- **飞花令**（`/game/feihua`）：按「令字」接句，用户每回合提交一句含令字的真实古诗词名句。
  - 三种模式：`ancient`（古制严格，令字须落在本回合第 N 个汉字位置）、`normal`（标准版，诗句包含令字即可）、`super`（超级飞花令，含 `char` 多关键字 / `topic` 主题两种子类型）
  - 三种难度：`easy`（达标 10 句即胜）、`medium`（达标 20 句即胜）、`hard`（AI 无句可接则用户胜，无固定目标条数）
  - 每题给出 AI 候选诗句（LLM 生成 + 后端按位次/判重/真实性二次过滤后下发）
- **诗词接龙**（`/game/chain`）：以 AI 起句的末字为首字接出下一句名句，AI 再接给用户，交替进行；本地诗词库随机取起句（不调用 LLM）；AI 接不上时用户胜，链长即有效句数。
  - **选择范围**：配置阶段可选「中国古典诗词（不限朝代，默认）」或具体朝代（取自 `dynasty` 表）；选定后 AI 起句仅在该朝代内取句，AI 接句提示词限定该朝代，且**玩家提交与 AI 接句的真实性校验均限定在该朝代内**（超出范围判为「诗句真实性校验不通过」并提示越界）。
- **回合计时（计时/监测/缓冲三子态，打字即暂停）**：单回合有效答题时长 30 秒。

【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 功能说明】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 功能说明】
- **回合计时（计时/监测/缓冲三子态，打字即暂停）**：单回合有效答题时长 30 秒。点击「开始挑战」后倒计时**直接开始**（`counting`，30 秒递减）；输入框一旦有变化即**暂停倒计时**并进入 `monitoring`（监测停笔 5 秒），仅当连续 5 秒无改动才恢复递减；30 秒耗尽后自动进入 `buffer`（5 秒缓冲窗口，期内有输入变动仍可提交）；缓冲耗尽仍无改动则由前端自动提交并判超时。有效答题时长上限 30 + 5 = 35 秒；**题面计时均为显示层，可被主动提交（作答/放弃）随时终止**。答错不重置计时，答对推进下一回合并重置计时。
- **超时判定（后端按上报有效时长）**：前端每次作答请求上报 `activeSeconds`（本回合已消耗的有效秒数），后端 `GameConstants.isTimeout(activeSeconds, wallElapsedMillis)` 优先据此判定（`>= 35` 即超时）；未上报时回退墙钟口径（服务端 `roundStartTime` 毫秒时间戳，阈值 30 秒 + 1 秒网络宽限）。
- **容错**：每回合初始 2 次重试，累计错误达 3 次判负；空输入不计错误、不消耗重试、不重置计时。
- **三级真实性校验链**：① 本地 `poem` 表 ngram 全文索引 `MATCH...

【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 功能说明】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 功能说明】
- **三级真实性校验链**：① 本地 `poem` 表 ngram 全文索引 `MATCH...AGAINST`（限定范围时附加 `dynasty_id` 过滤）→ ② 外部 APIHZ 复核（仅在**不限朝代**时可用，外部源无法确认朝代归属）→ ③ LLM 兜底真伪判断（`feihua_verify`，限定范围时提示词附朝代约束），全链路失败即判为「诗句真实性校验不通过」。
- **游客模式**：游客与新页面共用同一套接口，后端按登录态分层——**游客对局仅存 Redis（不落 MySQL），登录用户对局与有效诗句写入 `game_record` / `game_sentence`**；前端以 `GuestBadge` 展示游客标识，登录态用户对局可回查。
- **状态/判重后端权威**：对局运行时状态存 Redis（键 `game:{gameId}` 与 `game:{gameId}:sentences`，TTL 30 分钟，交互续期）；**超时判定由后端依前端上报的 `activeSeconds` 裁定**（见上文「超时判定」）。
- **输入方式**：支持文本与语音（`inputType`），语音仅标识，校验逻辑一致。

【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 玩法规则速览】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 玩法规则速览】
### 玩法规则速览

| 维度 | 取值 | 说明 |
|------|------|------|
| 玩法类型 | feihua / chain | 对局表 `game_type` 区分 |
| 飞花令模式 | ancient / normal / super | super 需 `superType`（char/topic） |
| 飞花令难度 | easy / medium / hard | 达标条数 easy=10、medium=20、hard=null（AI 枯竭即胜） |
| 接龙选择范围 | 不限（默认，中国古典诗词）/ 具体朝代 | 传 `dynastyId` 限定；起句、AI 接句、玩家校验均限定在该朝代内 |
| 对局结果 | win / lose / giveup | 写入 `game_record.game_result` |

【moyuan_system_workflow.md · 系统工作流文档 / 10. 搜索与运营域 / 10.1 全文/跨模块智能搜索（C8）】
【章节：系统工作流文档 / 10. 搜索与运营域 / 10.1 全文/跨模块智能搜索（C8）】
### 10.1 全文/跨模块智能搜索（C8）
- **触发入口**：`GET /api/search/smart?dynastyId&categoryId&poetId&poetName&keyword&sortBy&pageNum&pageSize`；`GET /api/search?keyword=`（三段返回 poems/poets/posts）；`GET /api/search/poems`；`GET /api/search/suggestions|hot|history`、`DELETE /api/search/history`。
- **分步流程**（`SmartSearchServiceImpl.smartSearch`）：
  1. 有 keyword 且已登录 → `saveSearchHistory`。
  2. `resolvePoetNameIds`：poetName 对 poet.name 模糊匹配取 ID 集。
  3. 无 keyword → `searchWithoutKeyword` 按筛选条件分页 + 排序（最新/热门/点赞）。
  4. 有 keyword 三路并发，命中结果统一按 poem.id 去重取高分、内存分页，为空置 `suggestExternal=true`：
     - `searchPoems`（名称匹配，**双模式**）：
       - 关闭全文（`search.fulltext-enabled=false`）：对 title/content 用原始词 + 去标点拆分词 `like` 模糊匹配。
       - **默认开启**（`search.fulltext-enabled=true`，见 `application.yml`）：改用 ngram 中文全文索引 `MATCH(title, content) AGAINST(... IN NATURAL LANGUAGE MODE)`（`poem` 表 `ft_poem_title_content` 已 `WITH PARSER ngram`，中文分词 + 相关度更高，见 `docs/database/migration_004_poem_search_fulltext.sql`）。
       - 两模式命中集统一经 `scoreAndRank` 按 `calculateScore` 打分：标题精确 1.0/包含 0.8/拆分 0.3、内容包含 0.5/拆分 0.2。
     - `searchByPoetName`：匹配诗人名取作品，精确 0.9/包含 0.7/部分 0.3。
     - `searchByPinyin`：`PinyinUtil.matchesPinyin` 拼音联想，基础 0.3+标题 0.2+诗人 0.1。
  5.

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 飞花令（play.feihualing）】
【章节：墨渊小助手系统知识库（24 条） / 飞花令（play.feihualing）】
## 飞花令（play.feihualing）

- 分类：play
- 关键词：飞花令,对诗,令字,行酒令,诗词游戏,怎么玩
- 别名：花令,飞花令游戏
- 前端路由：/game/feihua
- 说明：飞花令是与 AI 对句的诗词游戏，你每回合说出一句含指定「令字」的真实古诗词名句，AI 轮流接句，接不上或超时即判负。入口在首页 AI 小工具区的「飞花令」卡片，登录用户点击后进入对局页。有古制严格、标准版、超级飞花令三种模式与三档难度可选。现代诗不参与本游戏。
- 使用步骤：
  1. 选择模式与难度
  2. 设定令字（如「花」）
  3. 每回合提交一句含令字的古诗词，单回合限时 30 秒
  4. 与 AI 交替接句，AI 接不上或你超时判负
- 常见问答：
  - 问：飞花令怎么玩  答：每回合提交一句含指定令字的真实古诗词名句，与 AI 交替接句，AI 接不上或你超时则对方判负。
  - 问：在哪里能玩飞花令  答：首页 AI 小工具区有「飞花令」卡片，登录用户点击进入对局页。
  - 问：飞花令有几种模式  答：有古制严格、标准版、超级飞花令三种模式，难度分简单、中等、困难三档。
```
</details>

## E4（easy）诗词评分支持哪些维度？

**回答**：诗词评分支持以下五个维度：格律、意境、用词、情感、创新（来源：moyuan_business_modules.md·业务模块说明 / 2. 诗词模块 / 评分维度；moyuan_system_workflow.md·系统工作流文档 / 8. 诗词内容域 / 8.4 诗词评分（C4））。

具体说明如下：
- **格律**：指诗词的格律规范性。
- **意境**：指诗词的意境表达。
- **用词**：指诗词的用词精准度。
- **情感**：指诗词的情感表达。
- **创新**：指诗词的创新程度。

用户评分范围为 1.0-5.0 分，AI 评分同样基于上述维度进行分析并给出分数（来源：moyuan_business_modules.md·业务模块说明 / 2. 诗词模块 / 功能说明；moyuan_system_workflow.md·系统工作流文档 / 8. 诗词内容域 / 8.4 诗词评分（C4））。

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 2. 诗词模块 / 功能说明】
【章节：业务模块说明 / 2. 诗词模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 2. 诗词模块 / API 接口】
【章节：业务模块说明 / 2. 诗词模块 / API 接口】
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

【moyuan_business_modules.md · 业务模块说明 / 2. 诗词模块 / 评分维度】
【章节：业务模块说明 / 2. 诗词模块 / 评分维度】
### 评分维度

诗词评分支持以下维度：
- **格律**：诗词的格律规范性
- **意境**：诗词的意境表达
- **用词**：诗词的用词精准度
- **情感**：诗词的情感表达
- **创新**：诗词的创新程度

【moyuan_business_modules.md · 业务模块说明 / 2. 诗词模块 / 前端路由】
【章节：业务模块说明 / 2. 诗词模块 / 前端路由】
### 前端路由

| 路径 | 说明 |
|------|------|
| `/poem` | 诗词列表页 |
| `/poem/create` | 发布新诗（支持原创标识） |
| `/poem/:id` | 诗词详情页（含评分功能） |

---

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 功能说明】
【章节：业务模块说明 / 8. 搜索模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 复合搜索策略】
【章节：业务模块说明 / 8. 搜索模块 / 复合搜索策略】
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

【moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / 功能说明】
【章节：业务模块说明 / 16. 诗词评分模块 / 功能说明】
### 功能说明

- 用户评分：用户对诗词进行评分（1.0-5.0分）
- AI评分：请求AI对诗词进行评分和分析
- 评分维度：支持多维度评分（格律、意境、用词、情感、创新）
- 评分统计：诗词平均评分和评分数量
- 评分详情：展示用户评分和AI评分详情
- 重新评价：用户已评分后可点击"重新评价"修改评分

【moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / 评分类型】
【章节：业务模块说明 / 16. 诗词评分模块 / 评分类型】
### 评分类型

| 类型 | 说明 |
|------|------|
| 用户评分 | 用户手动评分，可附带评语 |
| AI评分 | AI自动评分，附带AI分析内容 |

【moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / 评分维度】
【章节：业务模块说明 / 16. 诗词评分模块 / 评分维度】
### 评分维度

| 维度 | 说明 |
|------|------|
| 格律 | 诗词的格律规范性 |
| 意境 | 诗词的意境表达 |
| 用词 | 诗词的用词精准度 |
| 情感 | 诗词的情感表达 |
| 创新 | 诗词的创新程度 |

【moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / API 接口】
【章节：业务模块说明 / 16. 诗词评分模块 / API 接口】
### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/poems/{id}/ratings | 获取诗词评分详情 | sc-moyuan-backend PoemRatingController |
| POST /api/poems/{id}/ratings | 用户评分 | sc-moyuan-backend PoemRatingController |
| POST /api/poems/{id}/ratings/ai | 请求AI评分 | sc-moyuan-backend PoemRatingController |
| GET /api/poems/{id}/ratings/ai | 获取AI评分 | sc-moyuan-backend PoemRatingController |
| GET /api/poems/{id}/ratings/me | 获取当前用户评分 | sc-moyuan-backend PoemRatingController |

【moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.4 诗词评分（C4）】
【章节：系统工作流文档 / 8. 诗词内容域 / 8.4 诗词评分（C4）】
### 8.4 诗词评分（C4）
- **触发入口**：`GET /api/poems/{poemId}/ratings`；`POST .../ratings?score&comment`；`POST .../ratings/ai`、`GET/regenerate`；`GET .../ratings/me`。
- **分步流程**：校验 score∈[1.0,5.0] → `ratePoem`（`@Transactional`）按 `poem_id+user_id+rating_type=1` 增改评分 → `updatePoemAverageScore` 重算 `avg_score/rating_count`；AI 评分 `rating_type=2`（userId=null）：`aiService.analyzePoem(content, null, "rating")` 走 `ai_module_config` 的独立 `rating` 模块绑定（不再写死默认模型，可在后台「AI功能配置」页面调整）→ 正则解析分（失败兜底 3.5）→ 摘要取前 200 字、去 markdown 标记，随后重算均分，`ai_model` 字段记录实际生效模型名。`getPoemRatings` 返回 userRatings(type=1)/aiRating(type=2 最新)/averageScore/ratingCount，异常回退空数据。
- **关键类与方法**：`PoemRatingServiceImpl.ratePoem/requestAiRating/parseAiScore/generateSummary/updatePoemAverageScore`、`AiService.analyzePoem`。`PoemRatingController.*`。
- **数据与缓存**：`poem_rating`（score/comment/rating_type:1用户2AI/ai_model/ai_summary/ai_analysis）、`poem.avg_score/rating_count`。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 诗词AI评分（play.rating）】
【章节：墨渊小助手系统知识库（24 条） / 诗词AI评分（play.rating）】
## 诗词AI评分（play.rating）

- 分类：play
- 关键词：AI评分,诗词评分,评分,打分
- 别名：AI打分,诗词打分
- 前端路由：/poem/:id
- 说明：诗词详情页提供 AI 评分，由 AI 从意境、格律、修辞、情感、用词等维度评价诗词并给出分数。进入任意诗词详情页即可查看或请求 AI 评分，用户本人也可以手动给诗词评分。
- 使用步骤：
  1. 打开任意诗词详情页
  2. 查看已有评分或请求 AI 评分
  3. 查看 AI 给出的评价与分数
- 常见问答：
  - 问：AI评分在哪里  答：在诗词详情页可以查看或请求 AI 评分，AI 会多维度评价并给出分数。
```
</details>

## E5（easy）搜索评分中标题完全匹配得多少分？内容包含拆分词得多少分？

**回答**：搜索评分中，标题完全匹配得 1.0 分；内容包含拆分词得 0.2 分。

具体评分规则如下：
- **标题完全匹配**：去除标点后完全一致时，得分为 1.0（来源：moyuan_business_modules.md·业务模块说明/8.搜索模块/搜索评分算法）。
- **内容包含拆分词**：诗词内容中包含拆分后的单个关键词时，得分为 0.2（来源：moyuan_business_modules.md·业务模块说明/8.搜索模块/搜索评分算法）。

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 2. 诗词模块 / 功能说明】
【章节：业务模块说明 / 2. 诗词模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 功能说明】
【章节：业务模块说明 / 8. 搜索模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 复合搜索策略】
【章节：业务模块说明 / 8. 搜索模块 / 复合搜索策略】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 搜索评分算法】
【章节：业务模块说明 / 8. 搜索模块 / 搜索评分算法】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 搜索流程】
【章节：业务模块说明 / 8. 搜索模块 / 搜索流程】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 智能搜索参数】
【章节：业务模块说明 / 8. 搜索模块 / 智能搜索参数】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 智能搜索响应】
【章节：业务模块说明 / 8. 搜索模块 / 智能搜索响应】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 推荐算法说明】
【章节：业务模块说明 / 8. 搜索模块 / 推荐算法说明】
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

【moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / 功能说明】
【章节：业务模块说明 / 16. 诗词评分模块 / 功能说明】
### 功能说明

- 用户评分：用户对诗词进行评分（1.0-5.0分）
- AI评分：请求AI对诗词进行评分和分析
- 评分维度：支持多维度评分（格律、意境、用词、情感、创新）
- 评分统计：诗词平均评分和评分数量
- 评分详情：展示用户评分和AI评分详情
- 重新评价：用户已评分后可点击"重新评价"修改评分

【moyuan_system_workflow.md · 系统工作流文档 / 10. 搜索与运营域 / 10.1 全文/跨模块智能搜索（C8）】
【章节：系统工作流文档 / 10. 搜索与运营域 / 10.1 全文/跨模块智能搜索（C8）】
### 10.1 全文/跨模块智能搜索（C8）
- **触发入口**：`GET /api/search/smart?dynastyId&categoryId&poetId&poetName&keyword&sortBy&pageNum&pageSize`；`GET /api/search?keyword=`（三段返回 poems/poets/posts）；`GET /api/search/poems`；`GET /api/search/suggestions|hot|history`、`DELETE /api/search/history`。
- **分步流程**（`SmartSearchServiceImpl.smartSearch`）：
  1. 有 keyword 且已登录 → `saveSearchHistory`。
  2. `resolvePoetNameIds`：poetName 对 poet.name 模糊匹配取 ID 集。
  3. 无 keyword → `searchWithoutKeyword` 按筛选条件分页 + 排序（最新/热门/点赞）。
  4. 有 keyword 三路并发，命中结果统一按 poem.id 去重取高分、内存分页，为空置 `suggestExternal=true`：
     - `searchPoems`（名称匹配，**双模式**）：
       - 关闭全文（`search.fulltext-enabled=false`）：对 title/content 用原始词 + 去标点拆分词 `like` 模糊匹配。
       - **默认开启**（`search.fulltext-enabled=true`，见 `application.yml`）：改用 ngram 中文全文索引 `MATCH(title, content) AGAINST(... IN NATURAL LANGUAGE MODE)`（`poem` 表 `ft_poem_title_content` 已 `WITH PARSER ngram`，中文分词 + 相关度更高，见 `docs/database/migration_004_poem_search_fulltext.sql`）。
       - 两模式命中集统一经 `scoreAndRank` 按 `calculateScore` 打分：标题精确 1.0/包含 0.8/拆分 0.3、内容包含 0.5/拆分 0.2。
     - `searchByPoetName`：匹配诗人名取作品，精确 0.9/包含 0.7/部分 0.3。
     - `searchByPinyin`：`PinyinUtil.matchesPinyin` 拼音联想，基础 0.3+标题 0.2+诗人 0.1。
  5.

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 诗词AI评分（play.rating）】
【章节：墨渊小助手系统知识库（24 条） / 诗词AI评分（play.rating）】
## 诗词AI评分（play.rating）

- 分类：play
- 关键词：AI评分,诗词评分,评分,打分
- 别名：AI打分,诗词打分
- 前端路由：/poem/:id
- 说明：诗词详情页提供 AI 评分，由 AI 从意境、格律、修辞、情感、用词等维度评价诗词并给出分数。进入任意诗词详情页即可查看或请求 AI 评分，用户本人也可以手动给诗词评分。
- 使用步骤：
  1. 打开任意诗词详情页
  2. 查看已有评分或请求 AI 评分
  3. 查看 AI 给出的评价与分数
- 常见问答：
  - 问：AI评分在哪里  答：在诗词详情页可以查看或请求 AI 评分，AI 会多维度评价并给出分数。
```
</details>

## E6（easy）智能搜索的建议词、热门词和搜索历史分别缓存成什么 key？

**回答**：智能搜索的建议词、热门词和搜索历史分别使用以下 Redis 键名进行缓存：

1. **搜索建议**：`search:suggestion:{kw}`（基于关键词 kw，5 分钟过期）（来源：moyuan_business_modules.md·业务模块说明 / 8. 搜索模块 / 技术方案；moyuan_system_workflow.md·系统工作流文档 / 10.1 全文/跨模块智能搜索（C8））
2. **热门搜索**：`search:hot`（Redis ZSet 结构，实时统计逆序返回）（来源：moyuan_business_modules.md·业务模块说明 / 8. 搜索模块 / 技术方案；moyuan_system_workflow.md·系统工作流文档 / 10.1 全文/跨模块智能搜索（C8））
3. **搜索历史**：`search:history:{userId}`（Redis List 结构，按用户 ID 区分，30 天过期，最多 20 条）（来源：moyuan_business_modules.md·业务模块说明 / 8. 搜索模块 / 功能说明；moyuan_system_workflow.md·系统工作流文档 / 10.1 全文/跨模块智能搜索（C8））

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 功能说明】
【章节：业务模块说明 / 8. 搜索模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 技术方案】
【章节：业务模块说明 / 8. 搜索模块 / 技术方案】
### 技术方案

- **搜索核心**：SmartSearchService，多条件复合查询 + 评分排序
- **搜索引擎**：MySQL 8 ngram 中文全文检索（`/api/search/smart` 关键词默认走 `MATCH(title,content) AGAINST(...)`，`search.fulltext-enabled` 默认开启，可经 `SEARCH_FULLTEXT_ENABLED` 关闭回退 LIKE）；Elasticsearch 属后期可选优化
- **拼音支持**：PinyinUtil 工具类，可配置搜索范围（默认200条，最大1000条）
- **缓存策略**：Redis 缓存搜索建议（5分钟）、热门搜索（实时统计）、搜索历史（30天）
- **推荐服务**：RecommendationService，协同过滤 + 兴趣推荐 + 外部API
- **外部API集成**：今日诗词API，补充古诗词数据源

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 搜索流程】
【章节：业务模块说明 / 8. 搜索模块 / 搜索流程】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / API 接口】
【章节：业务模块说明 / 8. 搜索模块 / API 接口】
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

【moyuan_system_workflow.md · 系统工作流文档 / 2. 全局约定与分层（引擎级机制） / 2.5 缓存策略速览】
【章节：系统工作流文档 / 2. 全局约定与分层（引擎级机制） / 2.5 缓存策略速览】
### 2.5 缓存策略速览
| 场景 | 载体 | key / 范围 |
|------|------|------------|
| 诗词/诗人列表与详情 | Spring Cache（`@Cacheable`） | `poems::…`、`poets::detail:{id}` |
| 推荐结果 | Spring Cache | `poetRecommendation::…`、`poemRecommendation::…` |
| 搜索建议/热门/历史 | Redis | `search:suggestion:{kw}`、`search:hot`(ZSet)、`search:history:{userId}`(List) |
| AI 模型注册表 | 进程内 `ConcurrentHashMap` | `AiModelRegistry.modelCache` |
| 诗词赏析增强内容 | 关系表 `poem_content_cache` | `poem_title+poet_name+content_type` |
| 前端下拉型数据 | localStorage（前缀 `moyuan_`） | `moyuan_cache_*`（详见 14） |

【moyuan_system_workflow.md · 系统工作流文档 / 10. 搜索与运营域 / 10.1 全文/跨模块智能搜索（C8）】
【章节：系统工作流文档 / 10. 搜索与运营域 / 10.1 全文/跨模块智能搜索（C8）】
建议：先查 Redis `search:suggestion:{kw}`（5min），未命中从 poem 标题+诗人名并集。
  6. 热门：读 Redis ZSet `search:hot` 逆序；空返回默认词。历史：`search:history:{userId}` List（remove+leftPush+trim(0,19)+30天过期），同时 `zset.incrementScore` 累加热度。
  7. `/api/search/poems` 走 `recommendationService.searchPoems`：本地（附 `recommendScore` 含兴趣加权+浏览/点赞对数分），不足时拉外部（author 含 keyword，`recommendScore=0.5`）合并降序。
- **关键类与方法**：`SmartSearchServiceImpl.*`、`SearchController.*`、`PinyinUtil.matchesPinyin`、`RecommendationServiceImpl.searchPoems/getExternalPoems/calculatePoemRecommendScore`；配置 `search.fulltext-enabled`（默认 true，全文已开启，可用环境变量 `SEARCH_FULLTEXT_ENABLED` 覆盖）、`search.pinyin.limit`。
- **数据与缓存**：Redis `search:suggestion:{kw}`、`search:hot`(ZSet)、`search:history:{userId}`(List)；`poem` 表全文索引 `ft_poem_title_content`（ngram）。

【moyuan_system_workflow.md · 系统工作流文档 / 10. 搜索与运营域 / 10.2 押韵/同韵字查询（C9）】
【章节：系统工作流文档 / 10. 搜索与运营域 / 10.2 押韵/同韵字查询（C9）】
### 10.2 押韵/同韵字查询（C9）
- **触发入口**：`GET /api/rhyme/query?character=月`；`GET /api/rhyme/group?group=仄声`（`/api/rhyme/**` 已放行）。
- **分步流程**（数据库优先 + AI 兜底落库）：`queryByCharacter` 取首字符按 `rhyme.character` 等值查；命中则回查该字所属韵部（`rhyme_group`）返回**全部同韵字**，`sort_order` 升序；未命中调 `aiService.chat`（`moduleCode=chat`，`MAX_CHARS=40`）让大模型现查，解析其严格 JSON 结果后 `INSERT IGNORE` 幂等落库（`character+rhyme_group+tone_type` 唯一键防重），再回查返回。`queryByRhymeGroup` 同理：先查整组，未命中走 AI 兜底落库。异常统一 `R.error("500","查询韵脚/韵部失败，请稍后重试")`。无 Redis，直接查库。
- **数据与缓存**：`rhyme`（character/rhyme_group/tone_type/rhyme_category/sort_order）。

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.4 localStorage 使用点汇总（K4）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.4 localStorage 使用点汇总（K4）】
### 15.4 localStorage 使用点汇总（K4）
> **代码事实核对**：收藏、AI 对话、浏览历史**并未真正写入 localStorage**（收藏走服务端、AI 聊天在内存 ref、历史走 `/history` API）。真实落盘 key 如下：
- `token`/`refreshToken`（登录态双 token，user Store 直接写）
- `lastAdminUsername`（admin 记录）
- `home_nav_animation_config`（首页导航动画配置）
- `moyuan_token`（storage 封装的登录 token，带过期结构）
- `moyuan_user_preferences`（theme/language/fontSize/pageSize/sidebarCollapsed）
- `moyuan_search_history`（上限 20 条）
- `moyuan_form_draft_poem/forum/comment`
- `moyuan_sort_preferences`（{poem,poet,forum}）
- `moyuan_cache_dynasties/categories/poets`（筛选下拉，1h）
- `moyuan_cache_external_poem_`（外部诗词详情，24h）
- `moyuan_cache_`（外部搜索结果缓存）
- **收藏/AI对话/浏览历史**：无 localStorage key，分别走服务端接口/内存/API。
- **关键位置**：`utils/storage.ts`、`stores/preferences.ts/searchHistory.ts/formDraft.ts/apiCache.ts`。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 智能检索（feature.search）】
【章节：墨渊小助手系统知识库（24 条） / 智能检索（feature.search）】
## 智能检索（feature.search）

- 分类：feature
- 关键词：搜索,检索,找诗,搜诗人,关键字
- 别名：智能搜索,全文搜索
- 前端路由：/poem
- 说明：智能检索支持按关键词搜索诗词、诗人与帖子，首页与诗词列表页内嵌了搜索入口。搜索会记录你的历史，登录后还会根据兴趣给出推荐。
- 使用步骤：
  1. 在首页或诗词列表页输入关键词
  2. 查看匹配的诗词、诗人或帖子
  3. 点开结果查看详情
- 常见问答：
  - 问：怎么搜索  答：在首页或诗词列表页的搜索框输入关键词，即可搜索诗词、诗人与帖子。
```
</details>

## E7（easy）登录成功后签发哪两种 token，各自有效期多久？

**回答**：登录成功后签发的两种 token 为 access token（访问令牌）和 refresh token（刷新令牌），其有效期如下：

1. **Access Token**：默认有效期为 **24 小时**。该令牌用于后续请求的自动续期，包含随机生成的 `jti` 标识。
2. **Refresh Token**：默认有效期为 **7 天**。该令牌用于在 Access Token 过期后换取新的访问令牌，与 Access Token 共享相同的 `jti`。

（来源：moyuan_system_workflow.md · 系统工作流文档 / 4. 认证与权限域 / 4.2 登录与 JWT 签发（A2））

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 功能说明】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 功能说明】
- **三级真实性校验链**：① 本地 `poem` 表 ngram 全文索引 `MATCH...AGAINST`（限定范围时附加 `dynasty_id` 过滤）→ ② 外部 APIHZ 复核（仅在**不限朝代**时可用，外部源无法确认朝代归属）→ ③ LLM 兜底真伪判断（`feihua_verify`，限定范围时提示词附朝代约束），全链路失败即判为「诗句真实性校验不通过」。
- **游客模式**：游客与新页面共用同一套接口，后端按登录态分层——**游客对局仅存 Redis（不落 MySQL），登录用户对局与有效诗句写入 `game_record` / `game_sentence`**；前端以 `GuestBadge` 展示游客标识，登录态用户对局可回查。
- **状态/判重后端权威**：对局运行时状态存 Redis（键 `game:{gameId}` 与 `game:{gameId}:sentences`，TTL 30 分钟，交互续期）；**超时判定由后端依前端上报的 `activeSeconds` 裁定**（见上文「超时判定」）。
- **输入方式**：支持文本与语音（`inputType`），语音仅标识，校验逻辑一致。

【moyuan_system_workflow.md · 系统工作流文档 / 2. 全局约定与分层（引擎级机制） / 2.3 JWT 认证与过滤器链】
【章节：系统工作流文档 / 2. 全局约定与分层（引擎级机制） / 2.3 JWT 认证与过滤器链】
### 2.3 JWT 认证与过滤器链
- 无状态会话（`SessionCreationPolicy.STATELESS`），关闭 CSRF。
- `JwtAuthenticationFilter`（挂在 `UsernamePasswordAuthenticationFilter` 之前）从 `Authorization: Bearer` 取 token → 验签/过期 → 校验 **type=access** 且 `jti` 未被 `TokenBlacklistService` 拉黑 → 查库 → 组装 `LoginUser.getAuthorities()`（`ROLE_`+role）写入 SecurityContext。当前 JWT 采用**双 token**（access 短时效 + refresh 续期），登出/刷新将旧 token 的 `jti` 加入黑名单使其失效（详见 4.2、4.3）。
- 公开接口 `permitAll`（首页静态资源、Swagger、`/api/auth/**`、浏览类接口、`/uploads/**` 等）；`/api/admin/**` 需 `ROLE_ADMIN`；其余需登录。详细放行清单见 4.3。

【moyuan_system_workflow.md · 系统工作流文档 / 4. 认证与权限域 / 4.1 用户注册（A1）】
【章节：系统工作流文档 / 4. 认证与权限域 / 4.1 用户注册（A1）】
### 4.1 用户注册（A1）
- **目标**：完成参数校验、用户名/邮箱唯一性检查、密码 BCrypt 加密并落库，注册成功即签发 JWT 直接登录。
- **触发入口**：`POST /api/auth/register`（`@RequestBody RegisterRequest`）。
- **分步流程**：
  1. `@Valid` 校验：username 长度 3–20、password 长度 6–20、email/interests 可选；失败抛 `MethodArgumentNotValidException` → 400。
  2. `UserServiceImpl.register` 查 `user` 表：用户名重复抛 `USERNAME_EXISTS(1002)`、email 重复抛 `EMAIL_EXISTS(1003)`。
  3. `passwordEncoder.encode`（BCrypt）加密密码。
  4. 补全字段（nickname 缺省取 username、interests 逗号拼接、status=1）后 `userMapper.insert`（`@Transactional`）。
  5. `jwtUtil.generateToken(user.getId(), username)` 签发，返回 `TokenResponse{token, expiresIn}`。
- **关键类与方法**：`UserServiceImpl.register`、`AuthController.register`、`JwtUtil.generateToken`、`PasswordEncoderConfig`、`RegisterRequest`。
- **数据与缓存**：`user` 表；前端注册后写 `localStorage.token`。

【moyuan_system_workflow.md · 系统工作流文档 / 4. 认证与权限域 / 4.2 登录与 JWT 签发（A2）】
【章节：系统工作流文档 / 4. 认证与权限域 / 4.2 登录与 JWT 签发（A2）】
### 4.2 登录与 JWT 签发（A2）
- **目标**：校验账号密码与禁用状态，成功后刷新登录信息并签发 JWT。
- **触发入口**：`POST /api/auth/login`（`LoginRequest{username,password}`）。
- **分步流程**：
  1. 按 username 查 `user`：无抛 `USER_NOT_FOUND(1001)`；`status!=1` 抛 `ACCOUNT_DISABLED(1005)`。
  2. `passwordEncoder.matches` 校验密码，失败抛 `PASSWORD_ERROR(1004)`。
  3. 更新 `lastLoginTime`、`lastLoginIp`（`getClientIp` 依 `X-Forwarded-For`→`Proxy-Client-IP`→`WL-Proxy-Client-IP`→`X-Real-IP`→`remoteAddr` 取值）。
  4. `JwtUtil` 签发**双 token**：access token（`jwt.expiration`，默认 24h，`type=access`，含随机 `jti`）与 refresh token（`jwt.refresh-expiration`，默认 7 天，`type=refresh`，同 `jti`）；响应经 `TokenResponse` 同时返回二者。
  5. 前端持久化两种 token，用于后续自动续期。
- **刷新与登出（#4 双 token）**：
  - `POST /api/auth/refresh`：携带 refresh token → 校验 type=refresh 且未过期 → **轮换**（旧 access/refresh 拉黑，签发新的一对）并返回。
  - `POST /api/auth/logout`：将当前 token 的 `jti` 加入黑名单，登出立即失效。
  - 黑名单实现 `TokenBlacklistService`（优先 Redis，失败降级进程内内存），key 关联 `jti`。
- **关键类与方法**：`UserServiceImpl.login/getClientIp`、`AuthController.login/refresh/logout`、`JwtUtil.generateAccessToken/generateRefreshToken`、`TokenBlacklistService.blacklist/isBlacklisted`。
- **数据与缓存**：`user.last_login_time/last_login_ip`；前端写 `localStorage`（`token`/`refreshToken`，封装为 `moyuan_token`）、admin 写 `lastAdminUsername`。

【moyuan_system_workflow.md · 系统工作流文档 / 4. 认证与权限域 / 4.3 JWT 校验与安全过滤器链认证（A3）】
【章节：系统工作流文档 / 4. 认证与权限域 / 4.3 JWT 校验与安全过滤器链认证（A3）】
### 4.3 JWT 校验与安全过滤器链认证（A3）
- **目标**：受保护请求进入 Controller 前完成 token 验签、身份装配。
- **触发入口**：任何携带 token 的 HTTP 请求（过滤器链自动触发）。
- **分步流程**：`JwtAuthenticationFilter.doFilterInternal` → `extractToken`（`Bearer ` 前缀）→ `jwtUtil.validateToken`（验签 + 过期 + **type=access**）→ `TokenBlacklistService.isBlacklisted(jti)` 黑名单校验 → `getUserId` → 查库 → `status==1` 时组装 `LoginUser` 并写 `UsernamePasswordAuthenticationToken` 到 `SecurityContextHolder`。无论成败都 `doFilter` 放行，访问权限由 `SecurityConfig` 决定。
- **关键类与方法**：`JwtAuthenticationFilter.doFilterInternal/extractToken`、`JwtUtil.validateToken/parseToken/getUserId`、`TokenBlacklistService.isBlacklisted`、`LoginUser.getAuthorities`、`SecurityConfig.filterChain`。
- **数据与缓存**：`user.id/status/role`。

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.2 登录态与用户 Store（K2）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.2 登录态与用户 Store（K2）】
### 15.2 登录态与用户 Store（K2）
- **分步流程**：`useUserStore` 初始化读 `localStorage` 中的 `token`/`refreshToken`，有则 `fetchUserInfo()`；`login/register` 写双 token 到 store + localStorage 后拉取用户；`fetchUserInfo` 读 `/users/me`，admin 写 `lastAdminUsername`，网络/5xx 指数重试≤2，401→尝试续期或 `logout()`；`logout` 清双 token 与 localStorage，并调用 `/auth/logout`（后端拉黑）。
- **关键位置**：`stores/user.ts`、`api/modules/user.ts`、`views/user/login.vue`、`router/guards.ts`。
- **数据与缓存**：localStorage `token`/`refreshToken`（双 token）、`lastAdminUsername`。

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.4 localStorage 使用点汇总（K4）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.4 localStorage 使用点汇总（K4）】
### 15.4 localStorage 使用点汇总（K4）
> **代码事实核对**：收藏、AI 对话、浏览历史**并未真正写入 localStorage**（收藏走服务端、AI 聊天在内存 ref、历史走 `/history` API）。真实落盘 key 如下：
- `token`/`refreshToken`（登录态双 token，user Store 直接写）
- `lastAdminUsername`（admin 记录）
- `home_nav_animation_config`（首页导航动画配置）
- `moyuan_token`（storage 封装的登录 token，带过期结构）
- `moyuan_user_preferences`（theme/language/fontSize/pageSize/sidebarCollapsed）
- `moyuan_search_history`（上限 20 条）
- `moyuan_form_draft_poem/forum/comment`
- `moyuan_sort_preferences`（{poem,poet,forum}）
- `moyuan_cache_dynasties/categories/poets`（筛选下拉，1h）
- `moyuan_cache_external_poem_`（外部诗词详情，24h）
- `moyuan_cache_`（外部搜索结果缓存）
- **收藏/AI对话/浏览历史**：无 localStorage key，分别走服务端接口/内存/API。
- **关键位置**：`utils/storage.ts`、`stores/preferences.ts/searchHistory.ts/formDraft.ts/apiCache.ts`。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 注册与登录（account.login）】
【章节：墨渊小助手系统知识库（24 条） / 注册与登录（account.login）】
## 注册与登录（account.login）

- 分类：account
- 关键词：注册,登录,账号,密码
- 别名：登录,注册
- 前端路由：/user/login
- 说明：注册时需要填写用户名、密码和邮箱，登录使用用户名与密码。登录后可享受收藏、发布、评论等完整功能。注册成功会直接登录。
- 使用步骤：
  1. 进入登录页
  2. 没有账号时切换到注册并填写信息
  3. 登录成功后使用完整功能
- 常见问答：
  - 问：怎么注册账号  答：在注册页填写用户名、密码和邮箱即可注册，注册成功会直接登录。
  - 问：怎么登录  答：在登录页输入用户名和密码即可登录。
```
</details>

## E8（easy）报修工单从待处理到已关闭经过哪几个状态？

**回答**：报修工单从待处理到已关闭共经过**5 个状态**。

1. **待处理 (0)**：用户提交工单后的初始状态，系统自动生成编号并默认优先级为 2。
2. **处理中 (1)**：管理员分配处理人后，状态由 0 流转至 1。
3. **已解决 (2)**：问题修复完成后，状态由 1 流转至 2，系统记录解决时间。
4. **拒绝 (4)**：若工单被拒绝（非标准关闭路径），状态可流转至 4，记录拒绝时间。
5. **已关闭 (3)**：用户主动关闭或管理员操作后，状态由 2 流转至 3，系统记录关闭时间。

注：部分资料中提及状态 4（拒绝）作为独立状态存在，但在“待处理到已关闭”的主流程描述中，核心流转为 0→1→2→3。具体状态机定义如下：`待处理 (0) → 处理中 (1) → 已解决 (2) → 已关闭 (3)`，同时支持 `拒绝 (4)` 分支（来源：moyuan_business_modules.md · 业务模块说明 / 18. 报修模块 / 工单状态流转；moyuan_system_workflow.md · 系统工作流文档 / 13. 报修工单域 / 13.1 工单全生命周期（E7））。

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 10. 管理模块 / API 接口】
【章节：业务模块说明 / 10. 管理模块 / API 接口】
keyword 按姓名/朝代筛选） | sc-moyuan-backend AdminController |
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
| POST | /api/admin/poets/{id}/sync |

【moyuan_business_modules.md · 业务模块说明 / 18. 报修模块 / 功能说明】
【章节：业务模块说明 / 18. 报修模块 / 功能说明】
### 功能说明

- 用户提交报修工单（标题、描述、分类、优先级）
- 查看我的报修列表和详情
- 添加反馈评论
- 关闭工单
- 提交满意度评价（1-5星 + 评语）
- 管理端：工单管理、分配处理人、状态流转、内部备注、统计

【moyuan_business_modules.md · 业务模块说明 / 18. 报修模块 / 工单状态流转】
【章节：业务模块说明 / 18. 报修模块 / 工单状态流转】
### 工单状态流转

```
待处理(0) → 处理中(1) → 已解决(2) → 已关闭(3)
```

【moyuan_business_modules.md · 业务模块说明 / 18. 报修模块 / API 接口】
【章节：业务模块说明 / 18. 报修模块 / API 接口】
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

【moyuan_business_modules.md · 业务模块说明 / 18. 报修模块 / 数据库表】
【章节：业务模块说明 / 18. 报修模块 / 数据库表】
### 数据库表

- `repair_order`：报修工单表
- `repair_comment`：报修评论表

---

【moyuan_system_workflow.md · 系统工作流文档 / 3. 工作流索引表】
【章节：系统工作流文档 / 3. 工作流索引表】
| [11](#11-ai-能力域) |
| 论坛与社区域 | 发帖/评论/点赞 / 内容审核 | `ForumController`、`ForumPostServiceImpl`、`CommentServiceImpl` | [12](#12-论坛与社区域) |
| 报修工单域 | 工单全生命周期 | `RepairController`、`RepairOrderServiceImpl`、`AdminController` | [13](#13-报修工单域) |
| 内容管理/运营域 | 导航 / 静态页 / 诗话视野 / 精选诗人 | `HomeNavigation`、`StaticPageService`、`VisionArticleServiceImpl`、`PoetFeaturedServiceImpl` | [14](#14-内容管理运营域) |
| 前端数据流转层 | 请求层 / Store / localStorage / 首页 / 诗云 / 守卫 | `utils/request.ts`、`stores/*`、`router/guards.ts` | [15](#15-前端数据流转层) |
| 数据初始化流 | init.sql（全量合并） | `db/` 脚本、`AiModelRegistry` 密钥注入 | 见 16 |

> 每条工作流统一模板：**目标 / 触发入口 / 分步流程 / 关键类与方法 / 数据与缓存**。

---

【moyuan_system_workflow.md · 系统工作流文档 / 13. 报修工单域 / 13.1 工单全生命周期（E7）】
【章节：系统工作流文档 / 13. 报修工单域 / 13.1 工单全生命周期（E7）】
### 13.1 工单全生命周期（E7）
- **目标**：用户报修 → 管理员分配/处理/关闭 → 满意度评价。
- **触发入口**：用户 `POST /api/repair/orders`、`GET /api/repair/orders`、`.../{id}`、`.../{id}/comments`、`PUT .../close`、`PUT .../satisfaction`；管理 `GET/PUT /api/admin/repairs…`、`/assign`、评论等。
- **分步流程**：
  1. **创建**：`generateOrderNo`=`RO`+yyyyMMddHHmmssSSS+随机3位；status=0 待处理、priority 默认 2。
  2. **派单**：设置 assigneeId，待处理→处理中(status=1)。
  3. **流转** `updateStatus`：status=2 记 resolveTime，3 记 closeTime，4 拒绝。
  4. **沟通**：`addComment` 写 `repair_comment`（用户评论 isInternal=0；管理端内部备注 isInternal=1，用户侧查询过滤）。
  5. **关闭/评价**：`closeOrder` 置 3（拒绝重复关闭）；`submitSatisfaction` 写满意度及评语。详情/列表校验归属权限。
  6. **统计** `getStats` 按状态计数 total/pending(0)/processing(1)/resolved(2)/closed(3)/rejected(4)。
- **关键类与方法**：`RepairOrderServiceImpl.generateOrderNo/createOrder/assignOrder/updateStatus/closeOrder/submitSatisfaction/getStats`、`RepairController.*`、`AdminController.listRepairs/updateRepairStatus/assignRepair/getRepairStats`。
- **数据与缓存**：`repair_order`（status 状态机 0/1/2/3/4、order_no 唯一）、`repair_comment`（is_internal）。

---

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 报修中心（page.repair）】
【章节：墨渊小助手系统知识库（24 条） / 报修中心（page.repair）】
## 报修中心（page.repair）

- 分类：page
- 关键词：报修,反馈,工单,故障,问题反馈
- 别名：报修,反馈中心
- 前端路由：/repair
- 说明：报修中心用于提交报修工单，可填写标题、描述、分类与优先级，提交后查看我的报修列表与详情、添加反馈评论、关闭工单并提交满意度评价。需要登录后进入。
- 使用步骤：
  1. 登录后进入报修中心
  2. 点击提交报修并填写工单信息
  3. 在列表中查看工单详情与进度
  4. 添加评论、关闭工单并评价
- 常见问答：
  - 问：怎么报修  答：登录后进入报修中心，点击提交报修，填写标题、描述、分类与优先级后提交。
```
</details>

## E9（easy）诗云星空页面是什么，能做什么交互？

**回答**：诗云星空页面（`/poetry-cloud`）是一个基于 Three.js 的 3D 可视化系统，将中国古诗词数据以“诗人—诗作”两级层级星图形式呈现。每位诗人是一颗发光的大节点，其诗作是环绕分布的小节点，支持缩放、旋转与交互浏览。

- **核心展示**：通过球面螺旋排布展示诗人星系与诗作节点，连线显示诗人作品归属及同朝代赠诗关系（来源：moyuan_business_modules.md·业务模块说明 / 23. 诗云模块 / 功能说明）。
- **交互操作**：支持鼠标拖动旋转、滚轮缩放、右键平移、点击星点相机飞向；hover 节点显示 DOM 提示框；点击诗人星查看信息面板并聚焦其诗作；点击诗作弹出详情浮层（来源：moyuan_business_modules.md·业务模块说明 / 23. 诗云模块 / 功能说明）。
- **特色功能**：虚空取诗可随机浮现一首诗并生成蓝色新星飞向；底部“配色实验室”可切换 7 套配色方案并调节光晕、连线可见度等（来源：moyuan_business_modules.md·业务模块说明 / 23. 诗云模块 / 功能说明）。
- **数据与渲染**：前端加载朝代、诗人、诗作数据，使用 OrbitControls 控制相机，通过松弛迭代优化节点间距，确保小节点可被命中（来源：moyuan_system_workflow.md·系统工作流文档 / 15.6 诗云 3D 星空（K6））。

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 模块总览】
【章节：业务模块说明 / 模块总览】
| 诗词外部获取内容的缓存管理 | 前端+后端已实现 |
| 22 | 联系我们模块 | 用户反馈表单页面 | 前端已实现 |
| 23 | 诗云模块 | Three.js 3D 星空可视化，诗人—诗作两级层级星图 + 配色实验室 | 前端已实现 |
| 24 | 游戏模块 | 飞花令 / 诗词接龙对局（模式/难度、回合计时、三级真实性校验、游客模式） | 前端+后端已实现 |
| 25 | AI 对话页「诗宝阁」 | 角色化统一对话入口：6 角色 × 12 能力，覆盖命题作诗/集句/藏头/润色/平仄校验/韵脚查询/对对联/意象典故/诗话点评/飞花令/接龙/群贤论诗；其中飞花令为设令字后逐句判定（含字/判重/真实性）的对局流程 | 前端+后端已实现（SSE 流式、多轮上下文、服务端会话） |
| 26 | 全局 AI 小窗与系统知识库 | 全站悬浮 AI 问答入口：诗词助理 / 网页用法助理双助理，接入 system_knowledge 知识库 | 前端+后端已实现 |

---

【moyuan_business_modules.md · 业务模块说明 / 23. 诗云模块 / 功能说明】
【章节：业务模块说明 / 23. 诗云模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 23. 诗云模块 / 技术方案】
【章节：业务模块说明 / 23. 诗云模块 / 技术方案】
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

【moyuan_business_modules.md · 业务模块说明 / 23. 诗云模块 / 前端组件】
【章节：业务模块说明 / 23. 诗云模块 / 前端组件】
### 前端组件

| 组件 | 说明 |
|------|------|
| poetry-cloud/index.vue | 诗云主页面（Three.js 3D 渲染、数据加载、交互控制、悬浮提示） |

【moyuan_business_modules.md · 业务模块说明 / 23. 诗云模块 / 前端路由】
【章节：业务模块说明 / 23. 诗云模块 / 前端路由】
### 前端路由

| 路径 | 说明 |
|------|------|
| `/poetry-cloud` | 诗云星空页面（hideHeader） |

【moyuan_business_modules.md · 业务模块说明 / 23. 诗云模块 / 数据可视化】
【章节：业务模块说明 / 23. 诗云模块 / 数据可视化】
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

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.6 诗云 3D 星空（K6）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.6 诗云 3D 星空（K6）】
### 15.6 诗云 3D 星空（K6）
- **数据加载**：`Promise.all` 并发 `GET /dynasties`、`/poets(pageSize 300)`、`/poems(pageSize 1000)`，失败回退内置 12 朝代。
- **渲染**：`initScene`（Scene+FogExp2+Camera+WebGLRenderer(ACESFilmicToneMapping)+OrbitControls）→ 3200 背景星（Sphere+ShaderMaterial）→ 诗人星（发光小球+光晕，缓存 `poetPositionMap`）+ 同朝代贝塞尔赠诗线 → 诗作独立 Mesh 以「诗人为中心、诗作环绕分布」的球面螺旋生成并经松弛迭代拉开间距，按诗人分组的 LineSegments2 连线 → 交互：hover 高亮并显示 DOM 提示框、点击 `selectPoet→flyToStar+loadPoetPoems+focusPoetPoems`、点击诗作 `selectPoem`、朝代筛选重建。
- **配色实验室**：底部控制模块由 `reactive(viz)` 驱动（7 套方案 + 光晕/连线/尺寸/亮度滑杆 + 标签与朝代配色开关），`applyVizStyles()` 统一重涂背景/雾效、诗人节点、诗作节点与两类连线；整体亮度经画布容器 CSS `filter: brightness()`。
- **诗人姓名标注**：DOM 层 `.label-layer` + `updatePoetLabels()`，每帧 `getWorldPosition().project(camera)` 更新标签屏幕坐标，相机背后/视口外自动隐藏。
- **卸载**：`onUnmounted` 释放 geometry/material、清理标签与定时器、`scene.clear()`、`renderer.dispose()`。
- **数据与缓存**：数据仅内存 ref，不做 localStorage；`poetPositionMap` 内存缓存 3D 坐标，`poemCountMap` 缓存每位诗人的诗作数（用于节点尺寸）。

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.7 路由守卫（K7）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.7 路由守卫（K7）】
### 15.7 路由守卫（K7）
- **分步流程**：`beforeEach` 按 `meta.title` 设 document.title → `requiresAuth` 未登录跳 `Login`（带 redirect）；已登录缺 userInfo 先 `fetchUserInfo`；`requiresAdmin` 校验 role !== admin 弹错回 Home；已登录访问 Login/Register 回 Home。`afterEach` 维护 `appStore.cachedViews`（keep-alive）。
- **关键位置**：`router/guards.ts`、`router/index.ts`（`meta`: requiresAuth/requiresAdmin/keepAlive）、`stores/app.ts`。

---

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 诗云星空（play.poetry-cloud）】
【章节：墨渊小助手系统知识库（24 条） / 诗云星空（play.poetry-cloud）】
## 诗云星空（play.poetry-cloud）

- 分类：play
- 关键词：诗云,星空,3D,诗词星图,可视化
- 别名：诗云,诗云星空图
- 前端路由：/poetry-cloud
- 说明：诗云是用 3D 星空展示古诗数据的可视化页面，每位诗人是一颗星，其诗作环绕分布，可缩放旋转浏览。点击诗人星可查看诗人信息与代表作品，点击诗作节点可查看诗词详情。页面底部「配色实验室」可切换配色方案并调节光晕、连线、亮度等显示效果。
- 使用步骤：
  1. 进入诗云页面
  2. 拖动旋转、滚轮缩放浏览星图
  3. 点击诗人星查看诗人信息与作品
  4. 点击诗作节点查看诗词详情
- 常见问答：
  - 问：诗云是什么  答：诗云是把古诗词数据做成 3D 星空的页面，诗人是星、诗作围绕其分布，可交互浏览。
  - 问：诗云怎么用  答：拖动旋转、滚轮缩放浏览星图，点击诗人或诗作节点查看详情。
```
</details>

## E10（easy）哪些 AI 功能必须登录后才能用？

**回答**：必须登录才能使用的 AI 功能包括：看图写诗、AI 评分、诗宝阁对话以及平仄格律校验（古籍 OCR 识文目前处于维护中，需同时开启后端开关与前端常量）。游客可在首页直接体验飞花令、诗词接龙小工具。

- **看图写诗**：上传图片生成古诗词，支持视觉模型（来源：moyuan_business_modules.md · 业务模块说明 / 7. AI 模块 / 功能说明）
- **AI 评分**：属于需登录的 AI 能力范畴（来源：moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条）/ AI 功能需要登录（account.ai-login））
- **诗宝阁对话**：角色化统一对话入口，含创作、鉴赏、格律校正等能力（来源：moyuan_business_modules.md · 业务模块说明 / 25. AI 对话页「诗宝阁」模块 / 功能说明）
- **平仄格律校验**：粘贴绝句/律诗进行逐句标注与合律判定，由本地确定性引擎处理（来源：moyuan_business_modules.md · 业务模块说明 / 7. AI 模块 / 功能说明）
- **古籍 OCR 识文**：当前维护中，需同时打开后端开关 `ai.features.ocr-enabled=true` 与前端 `OCR_ENABLED` 常量（来源：moyuan_business_modules.md · 业务模块说明 / 7. AI 模块 / 功能说明）

注：飞花令、诗词接龙小工具游客可直接体验；AI 对话页「诗宝阁」及全局悬浮小窗需登录后使用。

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 7. AI 模块 / 功能说明】
【章节：业务模块说明 / 7. AI 模块 / 功能说明】
### 功能说明

- AI诗词问答（支持多模型切换；调用优先走 `ai_module_config` 模块绑定，未绑定某模块时回退默认模型。可用提供商：智谱AI、DeepSeek、Kimi、千问、小米MiMo、OpenRouter；NVIDIA NIM 已全部禁用/下线）
- 看图写诗（上传图片，AI生成古诗词，支持视觉模型如 GLM-4V-Flash、DeepSeek-Flash）
- 智能分析（输入诗句进行深度解析）
- AI对对联（输入上联，AI生成下联，遵循平仄规则）
- **藏头诗**（输入 2-8 个藏头字并选择五言/七言，AI 生成藏头诗，每句首字依次对应藏头字；复用 `POST /api/ai/chat`，moduleCode=chat，不新增后端接口）
- **平仄格律校验**（粘贴一首绝句/律诗，由**本地确定性引擎** `POST /api/ai/tone-check` 逐句标注平仄（平声绿底、仄声红底）、判定是否合律并给出不合律原因；校验规则为句末三平尾/三仄尾、二四六位失替、偶数句押韵，不调用大模型、不消耗 AI 额度）
- **AI 对话页「诗宝阁」**（`/ai-chat`）：角色化统一对话入口，6 角色（诗仙·创作 / 诗话·鉴赏 / 格律·校正 / 诗友·行令 / 群贤·论诗 / 顾问·问答）× 12 能力（命题作诗 / 集句成诗 / 藏头诗 / 润色入律 / 平仄校验 / 韵脚查询 / 对对联 / 意象典故 / 诗话点评 / 飞花令 / 诗词接龙 /

【moyuan_business_modules.md · 业务模块说明 / 7. AI 模块 / 功能说明】
【章节：业务模块说明 / 7. AI 模块 / 功能说明】
- **古籍OCR识文**（上传古诗词图片/书法作品/碑帖，AI精准识别文字内容，保留原文结构，标注印章题款）——**当前维护中**：后端开关 `ai.features.ocr-enabled=false` 时接口返回 `4293`，前端首页入口置灰并提示「维护中」，恢复需同时打开后端开关与前端 `OCR_ENABLED` 常量，并存在 `ocr` 模块的视觉模型绑定
- AI模型配置管理（后台管理，支持CRUD、启用/禁用、默认模型设置、连接测试）
- **AI内容填充与审核**：对诗词/诗人缺失字段由 AI 生成候选内容，存入审核表，管理员审核通过后写回原表
- **诗词AI助手（poetry_chat）**：寻章摘句页面的AI功能模块，包括译文注释、名人点评、创作背景、历史文化背景、AI逐句解析、AI诗词助手对话
  - **译文注释**：逐句翻译，关键词语注释
  - **名人点评**：历代名家点评（引用《诗品》《沧浪诗话》《人间词话》等）、文学史地位
  - **创作背景**：诗人生平、创作缘由、历史背景
  - **AI逐句解析**：点击诗句展开解释，支持多句同时展开，内存缓存不写库，可关闭
  - **弹窗交互**：三个功能按钮以向左展开的弹窗形式呈现，覆盖在诗词内容上方，不挤压布局
  - **搜索式加载**：AI生成过程中以搜索动画替代旋转加载图标

【moyuan_business_modules.md · 业务模块说明 / 11. 文件模块 / 功能说明】
【章节：业务模块说明 / 11. 文件模块 / 功能说明】
### 功能说明

- 统一文件上传功能（支持图片上传，按类型分目录存储）
- 文件元数据管理（file_metadata表）
- AI生成图片记录管理（ai_image_record表）
- 图片预览和管理
- 文件存储和访问
- AI图片自动水印功能
- 缩略图生成

【moyuan_business_modules.md · 业务模块说明 / 25. AI 对话页「诗宝阁」模块 / 功能说明】
【章节：业务模块说明 / 25. AI 对话页「诗宝阁」模块 / 功能说明】
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
- **会话记忆**：按 `roleId` 存服务端 Redis（键 `ai:chat:session:{userId}:{roleId}`，TTL 7 天、最多 40 条），切换角色各自恢复，换设备登录同一账号可继续；本地 `localStorage`（键 `moyuan_ai_chat_${roleId}`）作为降级缓存，Redis 不可用时仍可用。单条内容上限 4000 字
- **空态引导**：进入页面 / 新建会话后，按角色给出引导语 + 推荐能力 chip
- **结果渲染**：普通能力渲染为文本气泡，流式逐字追加并自动滚底；藏头诗高亮每句首字（藏头字）；平仄校验改调本地确定性引擎 `POST /api/ai/tone-check`，把返回的逐字平仄与问题列表渲染为平仄标签卡（平底/仄底，三平尾、三仄尾、二四六失替等问题标红），不再依赖大模型输出 JSON
- **消息头像**：对话流每条消息两侧带圆形头像——AI 侧统一使用墨渊可爱卡通头像（本地静态资源 `/img/ai_chat_avatar.jpg`），用户侧使用本人头像（未登录 / 无头像时回退金色首字母）；用户气泡靠右、头像居其右，AI 气泡靠左、头像居其左，加载态气泡同样带 AI 头像

【moyuan_business_modules.md · 业务模块说明 / 26. 全局 AI 小窗与系统知识库模块 / 功能说明】
【章节：业务模块说明 / 26. 全局 AI 小窗与系统知识库模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 26. 全局 AI 小窗与系统知识库模块 / 后端与数据结构】
【章节：业务模块说明 / 26. 全局 AI 小窗与系统知识库模块 / 后端与数据结构】
### 后端与数据结构

- `system_knowledge` 表：知识条目（code/title/category/keywords/aliases/body/steps/faq/route/is_show/sort_order/deleted），种子 24 条
- `ai_module_config` 新增 `system_chat` 模块（绑定 DeepSeek），与 `chat` 模块完全隔离，避免污染诗宝阁、藏头诗等其他 AI 功能提示词
- SecurityConfig：`/api/system-knowledge` 匿名可访问；`/api/ai/**` 已移除匿名放行，未登录返回 401
- 公开接口：`GET /api/system-knowledge`（只读列表）
- 后台接口：`GET /api/admin/system-knowledge`、`GET /{id}`、`POST`、`PUT /{id}`、`PUT /{id}/show`、`DELETE /{id}`

【moyuan_system_workflow.md · 系统工作流文档 / 11. AI 能力域 / 11.2 AI 诗人多轮对话（E2）】
【章节：系统工作流文档 / 11. AI 能力域 / 11.2 AI 诗人多轮对话（E2）】
### 11.2 AI 诗人多轮对话（E2）
- **目标**：对诗中"AI 大学者"连续问答；前端维护会话，每次提问独立请求到无状态 chat 接口。
- **触发入口**：`views/poem/list.vue` AI 对话浮层（`toggleAiChat`），首次自动发"请简要介绍一下这首诗"。
- **分步流程**：校验登录 → `getAiModuleConfig('poetry_chat')` 拉模块配置 → 用户消息 push 进 `aiChatMessages`（前端内存维护多轮）→ `buildAiPrompt` 用 `aiConfig.promptTemplate` 替换 `{poetName}/{poemTitle}/{poemContent}/{maxLength}/{styleHint}`（首次 `firstResponseLength` 120、后续 `maxResponseLength` 300）→ 调 `chat({message,prompt moduleCode:'poetry_chat'})` → `AiModelRegistry.chatByModule` 用 `ai_module_config.prompt_template` 作为 system 注入 → 返回 reply push，滚动到底部。
- **关键类与方法**：前端 `poem/list.vue.buildAiPrompt/sendAiChatMessage/toggleAiChat`；后端 `AiController.chat/getModuleConfig`、`AiModelRegistry.chatByModule`、`AiModelConfigService.getModuleConfigByCode`。
- **数据与缓存**：`ai_module_config`（moduleCode/prompt_template/first_response_length/max_response_length/enable_markdown）；会话由前端内存数组维护（后端无状态）。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / AI功能需要登录（account.ai-login）】
【章节：墨渊小助手系统知识库（24 条） / AI功能需要登录（account.ai-login）】
## AI功能需要登录（account.ai-login）

- 分类：account
- 关键词：AI功能,登录,权限,使用条件
- 别名：AI需要登录
- 说明：AI 相关功能需要登录后才能使用，包括看图写诗、AI 评分、诗宝阁对话以及平仄格律校验等；古籍识文目前处于维护中。首页的飞花令、诗词接龙小工具游客可在首页直接体验。
- 使用步骤：
  1. 登录账号
  2. 进入 AI 功能入口
  3. 正常使用 AI 能力
- 常见问答：
  - 问：AI功能要登录吗  答：需要登录后使用，包括看图写诗、AI 评分、诗宝阁对话与平仄校验等（古籍识文维护中）；游客可在首页直接体验飞花令、诗词接龙小工具。
```
</details>

## M1（medium）有人收藏了一首诗、又点赞并浏览了一次，推荐算法分别给这三种行为记几分？

**回答**：推荐算法对三种行为的计分规则如下：

1.  **收藏**：记 **5 分**。
2.  **点赞**：记 **3 分**。
3.  **浏览**：记 **1 分**。

在构建用户 - 内容评分矩阵时，若同一用户对同一作品存在多行为（如既收藏又点赞），系统取最大值合并，不累加。此规则适用于诗词及诗人推荐场景（来源：moyuan_business_modules.md·业务模块说明/3. 诗人模块/功能说明；moyuan_system_workflow.md·系统工作流文档/8. 诗词内容域/8.3 收藏 / 点赞（C3）；moyuan_system_workflow.md·系统工作流文档/9. 诗人内容域/9.2 推荐 / 热门诗人（C7））。

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 3. 诗人模块 / 功能说明】
【章节：业务模块说明 / 3. 诗人模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 功能说明】
【章节：业务模块说明 / 8. 搜索模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 推荐算法说明】
【章节：业务模块说明 / 8. 搜索模块 / 推荐算法说明】
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

【moyuan_business_modules.md · 业务模块说明 / 26. 全局 AI 小窗与系统知识库模块 / API 接口】
【章节：业务模块说明 / 26. 全局 AI 小窗与系统知识库模块 / API 接口】
### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/system-knowledge | 公开读取系统知识库条目 | sc-moyuan-backend SystemKnowledgeController |
| POST /api/ai/chat | 诗词助理 / 网页用法助理对话（`moduleCode` 分别为页面画像值 / `system_chat`） | sc-moyuan-backend AiController |

---

**文档版本**：v3.18
**最后更新**：2026-11-07（AI 加固收尾同步：第 25 章诗宝阁由「阶段一非流式」改为 SSE 流式 + system 人设多轮上下文 + 服务端会话（Redis，TTL 7 天，localStorage 兜底）+ 停止生成；平仄校验改述为本地确定性引擎 `POST /api/ai/tone-check`（不消耗 AI 额度）；古籍识文标注维护中（`ai.features.ocr-enabled=false`）；接口清单补 `tone-check` 与 3 个会话接口，并注明 `/api/ai/**` 统一限流与入参校验）；此前 v3.17（推荐算法说明补充「四组模型策略（离线制品查表）」与「后台实时看板」；后台控制台推荐指标屏新增缓存覆盖、模型版本、近 7 天切换趋势、切换去向/页面分布与最近事件）；v3.16（推荐算法说明由 UserCF 更新为 ItemCF，诗词行为矩阵同一作品多行为取最大值合并）；v3.15（新增第 26 章「全局 AI 小窗与系统知识库」：双助理、`system_chat` 模块、`system_knowledge` 表、路由画像、意图分流与 FAQ 直答、知识库跳转入口、登录门禁；模块总数 25 → 26）；v3.14（首页「诗境 AI」展开面板新增「前往完整页面」入口，按工具预选诗宝阁角色与能力；韵脚查询/对对联补充为诗宝阁新能力，能力总数 10 → 12；功能卡增加自动横向滑动）；2026-10-19（AI 对话页模块名统一由「诗阁」更名为「诗宝阁」；此前第 25 章「AI 对话页『诗宝阁』」补充对话流消息头像说明：AI 侧统一卡通头像 / 用户侧本人头像，气泡与头像左右对齐；v3.13 补充后端流式接口 `POST /api/ai/chat/stream`（SSE 流式）说明与接口表条目；v3.12 新增第 25 章：6 角色 × 10 能力的角色化统一对话入口，纯前端复用 `POST /api/ai/chat` 非流式接口）
**维护人员**：墨渊开发团队

【moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.3 收藏 / 点赞（C3）】
【章节：系统工作流文档 / 8. 诗词内容域 / 8.3 收藏 / 点赞（C3）】
### 8.3 收藏 / 点赞（C3）
- **触发入口**：`POST|GET /api/poems/{id}/like`、`POST|GET /api/poems/{id}/favorite`、`GET /api/poems/favorites`。
- **分步流程**：点赞 `toggleLike`（`@Transactional`）按 `user_id+target_id+target_type=POEM` 计数 `user_like`：存在则删记录且 `like_count-1`（`gt 0` 保护），否则插入并 `+1`；状态查询 `isLike/isFavorite` 计数判定；`getFavorites` 取收藏 ID 集后 `in` 查 poem 并填充。
- **注意（代码事实）**：收藏 `UserServiceImpl.toggleFavorite` **只写 `user_favorite` 表，不维护 `poem.favorite_count`**；点赞会维护 `poem.like_count`。
- **数据与缓存**：`user_like`、`user_favorite`、`poem.like_count`。

【moyuan_system_workflow.md · 系统工作流文档 / 9. 诗人内容域 / 9.2 推荐 / 热门诗人（C7）】
【章节：系统工作流文档 / 9. 诗人内容域 / 9.2 推荐 / 热门诗人（C7）】
### 9.2 推荐 / 热门诗人（C7）
- **触发入口**：`GET /api/poets/recommend?limit=6`（上限 20）、`GET /api/poets/popular?limit=10`（上限 50）；诗词侧 `GET /api/search/poems/recommended|popular`。
- **分步流程**：取当前用户（未登录走热门）→ `userId==null` 走 `getPopularPoets`；`buildUserPoetMatrix` 由收藏/点赞/浏览行为构建 用户→诗人:{权重}（收藏 5、点赞 3、浏览 1）→ `calculateItemCollaborativeScores` 计算物品-物品余弦相似度，每个已交互诗人取 Top50 相似诗人、对未交互诗人累加 `相似度×权重`（诗词侧 `buildUserPoemMatrix` 同一作品多行为取最大值） → `getInterestBasedPoetScores` 按 `interests` 朝代加权（古典/现代/外国）→ `mergeScores` 相加取 TopN → 过滤 status=1；空回退热门。热门 `calculatePoetPopularity`（诗收藏5+词收藏5+赞3+浏览1）取 TopN。结果 `@Cacheable` 缓存。
- **关键类与方法**：`PoetController.getRecommendedPoets/getPopularPoets`、`RecommendationServiceImpl.*`、`SearchController.getRecommendedPoems/getPopularPoems`。
- **数据与缓存**：`user_favorite/user_like/user_history`、`user.interests`、`poem.view_count/like_count`；缓存 `poetRecommendation::user:{userId}:{limit}`、`popular:{limit}` 等。

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.4 localStorage 使用点汇总（K4）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.4 localStorage 使用点汇总（K4）】
### 15.4 localStorage 使用点汇总（K4）
> **代码事实核对**：收藏、AI 对话、浏览历史**并未真正写入 localStorage**（收藏走服务端、AI 聊天在内存 ref、历史走 `/history` API）。真实落盘 key 如下：
- `token`/`refreshToken`（登录态双 token，user Store 直接写）
- `lastAdminUsername`（admin 记录）
- `home_nav_animation_config`（首页导航动画配置）
- `moyuan_token`（storage 封装的登录 token，带过期结构）
- `moyuan_user_preferences`（theme/language/fontSize/pageSize/sidebarCollapsed）
- `moyuan_search_history`（上限 20 条）
- `moyuan_form_draft_poem/forum/comment`
- `moyuan_sort_preferences`（{poem,poet,forum}）
- `moyuan_cache_dynasties/categories/poets`（筛选下拉，1h）
- `moyuan_cache_external_poem_`（外部诗词详情，24h）
- `moyuan_cache_`（外部搜索结果缓存）
- **收藏/AI对话/浏览历史**：无 localStorage key，分别走服务端接口/内存/API。
- **关键位置**：`utils/storage.ts`、`stores/preferences.ts/searchHistory.ts/formDraft.ts/apiCache.ts`。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 个人中心（page.profile）】
【章节：墨渊小助手系统知识库（24 条） / 个人中心（page.profile）】
## 个人中心（page.profile）

- 分类：page
- 关键词：个人中心,我的,资料,收藏,历史,认证
- 别名：个人主页,我的页面
- 前端路由：/user/profile
- 说明：个人中心可查看与修改个人资料、上传头像、修改密码，并查看我的收藏、我的点赞、我的帖子和浏览历史。诗人认证申请也从个人中心提交。需要登录后进入。
- 使用步骤：
  1. 登录后进入个人中心
  2. 修改资料、头像或密码
  3. 查看收藏、点赞、帖子与浏览历史
  4. 需要时提交诗人认证申请
- 常见问答：
  - 问：个人中心在哪里  答：登录后可从用户菜单进入个人中心，管理资料、收藏、历史并提交诗人认证。
```
</details>

## M2（medium）用户提交评分后，诗词的平均分是怎么更新的？AI 评分解析失败时会怎么办？

**回答**：用户提交评分后，系统会重新计算平均分；AI 评分解析失败时会自动兜底为 3.5 分。

1. **平均分更新机制**：当用户提交评分（`POST /api/poems/{id}/ratings`）或 AI 完成评分后，系统会调用 `updatePoemAverageScore` 方法，基于最新的评分数据重新计算该诗词的 `avg_score`（平均分）和 `rating_count`（评分数量），并将结果存入数据库中的 `poem` 表（来源：moyuan_system_workflow.md·8.4 诗词评分（C4））。
2. **AI 评分解析失败处理**：在 AI 评分流程中，若正则解析分数失败，系统会自动采用兜底值 3.5 分，随后生成摘要并记录实际生效的模型名到 `ai_model` 字段，最后参与平均分重算（来源：moyuan_system_workflow.md·8.4 诗词评分（C4））。

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 2. 诗词模块 / 功能说明】
【章节：业务模块说明 / 2. 诗词模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 2. 诗词模块 / API 接口】
【章节：业务模块说明 / 2. 诗词模块 / API 接口】
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

【moyuan_business_modules.md · 业务模块说明 / 2. 诗词模块 / 评分维度】
【章节：业务模块说明 / 2. 诗词模块 / 评分维度】
### 评分维度

诗词评分支持以下维度：
- **格律**：诗词的格律规范性
- **意境**：诗词的意境表达
- **用词**：诗词的用词精准度
- **情感**：诗词的情感表达
- **创新**：诗词的创新程度

【moyuan_business_modules.md · 业务模块说明 / 7. AI 模块 / 功能说明】
【章节：业务模块说明 / 7. AI 模块 / 功能说明】
- **古籍OCR识文**（上传古诗词图片/书法作品/碑帖，AI精准识别文字内容，保留原文结构，标注印章题款）——**当前维护中**：后端开关 `ai.features.ocr-enabled=false` 时接口返回 `4293`，前端首页入口置灰并提示「维护中」，恢复需同时打开后端开关与前端 `OCR_ENABLED` 常量，并存在 `ocr` 模块的视觉模型绑定
- AI模型配置管理（后台管理，支持CRUD、启用/禁用、默认模型设置、连接测试）
- **AI内容填充与审核**：对诗词/诗人缺失字段由 AI 生成候选内容，存入审核表，管理员审核通过后写回原表
- **诗词AI助手（poetry_chat）**：寻章摘句页面的AI功能模块，包括译文注释、名人点评、创作背景、历史文化背景、AI逐句解析、AI诗词助手对话
  - **译文注释**：逐句翻译，关键词语注释
  - **名人点评**：历代名家点评（引用《诗品》《沧浪诗话》《人间词话》等）、文学史地位
  - **创作背景**：诗人生平、创作缘由、历史背景
  - **AI逐句解析**：点击诗句展开解释，支持多句同时展开，内存缓存不写库，可关闭
  - **弹窗交互**：三个功能按钮以向左展开的弹窗形式呈现，覆盖在诗词内容上方，不挤压布局
  - **搜索式加载**：AI生成过程中以搜索动画替代旋转加载图标

【moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / 功能说明】
【章节：业务模块说明 / 16. 诗词评分模块 / 功能说明】
### 功能说明

- 用户评分：用户对诗词进行评分（1.0-5.0分）
- AI评分：请求AI对诗词进行评分和分析
- 评分维度：支持多维度评分（格律、意境、用词、情感、创新）
- 评分统计：诗词平均评分和评分数量
- 评分详情：展示用户评分和AI评分详情
- 重新评价：用户已评分后可点击"重新评价"修改评分

【moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / 评分类型】
【章节：业务模块说明 / 16. 诗词评分模块 / 评分类型】
### 评分类型

| 类型 | 说明 |
|------|------|
| 用户评分 | 用户手动评分，可附带评语 |
| AI评分 | AI自动评分，附带AI分析内容 |

【moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / 评分维度】
【章节：业务模块说明 / 16. 诗词评分模块 / 评分维度】
### 评分维度

| 维度 | 说明 |
|------|------|
| 格律 | 诗词的格律规范性 |
| 意境 | 诗词的意境表达 |
| 用词 | 诗词的用词精准度 |
| 情感 | 诗词的情感表达 |
| 创新 | 诗词的创新程度 |

【moyuan_business_modules.md · 业务模块说明 / 16. 诗词评分模块 / API 接口】
【章节：业务模块说明 / 16. 诗词评分模块 / API 接口】
### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/poems/{id}/ratings | 获取诗词评分详情 | sc-moyuan-backend PoemRatingController |
| POST /api/poems/{id}/ratings | 用户评分 | sc-moyuan-backend PoemRatingController |
| POST /api/poems/{id}/ratings/ai | 请求AI评分 | sc-moyuan-backend PoemRatingController |
| GET /api/poems/{id}/ratings/ai | 获取AI评分 | sc-moyuan-backend PoemRatingController |
| GET /api/poems/{id}/ratings/me | 获取当前用户评分 | sc-moyuan-backend PoemRatingController |

【moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.4 诗词评分（C4）】
【章节：系统工作流文档 / 8. 诗词内容域 / 8.4 诗词评分（C4）】
### 8.4 诗词评分（C4）
- **触发入口**：`GET /api/poems/{poemId}/ratings`；`POST .../ratings?score&comment`；`POST .../ratings/ai`、`GET/regenerate`；`GET .../ratings/me`。
- **分步流程**：校验 score∈[1.0,5.0] → `ratePoem`（`@Transactional`）按 `poem_id+user_id+rating_type=1` 增改评分 → `updatePoemAverageScore` 重算 `avg_score/rating_count`；AI 评分 `rating_type=2`（userId=null）：`aiService.analyzePoem(content, null, "rating")` 走 `ai_module_config` 的独立 `rating` 模块绑定（不再写死默认模型，可在后台「AI功能配置」页面调整）→ 正则解析分（失败兜底 3.5）→ 摘要取前 200 字、去 markdown 标记，随后重算均分，`ai_model` 字段记录实际生效模型名。`getPoemRatings` 返回 userRatings(type=1)/aiRating(type=2 最新)/averageScore/ratingCount，异常回退空数据。
- **关键类与方法**：`PoemRatingServiceImpl.ratePoem/requestAiRating/parseAiScore/generateSummary/updatePoemAverageScore`、`AiService.analyzePoem`。`PoemRatingController.*`。
- **数据与缓存**：`poem_rating`（score/comment/rating_type:1用户2AI/ai_model/ai_summary/ai_analysis）、`poem.avg_score/rating_count`。

【moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.5 外部诗词导入（C5）】
【章节：系统工作流文档 / 8. 诗词内容域 / 8.5 外部诗词导入（C5）】
### 8.5 外部诗词导入（C5）
- **触发入口**：`POST /api/poems/import-external`（title/content/author/dynasty）；`POST /api/poems/fix-external-poems`。
- **分步流程**：校验 title → 按 dynasty 名称查朝代 → 按 author 查诗人（无则新建 Poet 并 `clearAllCache`）→ 按 `title+poetId` 查重：命中补全空缺字段返回 `{id,imported:false}`，未命中落库（`source=external`、`poemType=ancient`、`isOriginal=0`、`auditStatus=1`、status=1）并清 `poems` 缓存返回 `{id,imported:true}`。修复接口取 `source=external` 且 `poet_id is null` 前 100 条，调 `https://cn.apihz.cn/api/zici/poetry.php` 补全作者朝代，成功后清 `poets`/`poems` 缓存返回 `{total,fixed}`。
- **关键类与方法**：`PoemController.importExternalPoem/fixExternalPoems`、`PoemServiceImpl`、`PoetDefaultAvatar.getAvatar`。
- **数据与缓存**：`poem.source/is_original/poem_type/audit_status`；清缓存 key：`poems`、`poets`。

---

【moyuan_system_workflow.md · 系统工作流文档 / 17. 关键代码速查表】
【章节：系统工作流文档 / 17. 关键代码速查表】
4（诗词 AI 评分改为独立 `rating` 模块：`PoemRatingServiceImpl.requestAiRating` 由写死默认模型改为 `aiService.analyzePoem(content, null, "rating")`，`ai_model` 字段记录实际生效模型名；后台新增「AI功能配置」页面（`/admin/ai-config`）可按模块调整模型与提示词；此前 v1.3 补充流式问答入口 `POST /api/ai/chat/stream` 调用链，v1.2 新增首页小游戏区块工作流）
**维护人员**：墨渊开发团队
**维护说明**：本文为 docs 本地维护文件，受 `.git/info/exclude` 排除不提交远程；代码变更后请依据 doc-manager 技能核对本文与实现一致性。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 诗词AI评分（play.rating）】
【章节：墨渊小助手系统知识库（24 条） / 诗词AI评分（play.rating）】
## 诗词AI评分（play.rating）

- 分类：play
- 关键词：AI评分,诗词评分,评分,打分
- 别名：AI打分,诗词打分
- 前端路由：/poem/:id
- 说明：诗词详情页提供 AI 评分，由 AI 从意境、格律、修辞、情感、用词等维度评价诗词并给出分数。进入任意诗词详情页即可查看或请求 AI 评分，用户本人也可以手动给诗词评分。
- 使用步骤：
  1. 打开任意诗词详情页
  2. 查看已有评分或请求 AI 评分
  3. 查看 AI 给出的评价与分数
- 常见问答：
  - 问：AI评分在哪里  答：在诗词详情页可以查看或请求 AI 评分，AI 会多维度评价并给出分数。
```
</details>

## M3（medium）在诗汇论坛发帖、评论、点赞分别要满足什么条件？涉及哪些接口？帖子列表怎么展示置顶与精华？

**回答**：在诗汇论坛发帖、评论、点赞需满足登录条件，涉及多个 API 接口，帖子列表通过状态过滤和排序展示置顶与精华。

**1. 发帖、评论、点赞的条件与接口**
*   **发帖条件**：用户必须登录；支持富文本编辑及图片上传（交流广场模块）。
    *   **接口**：`POST /api/forum/posts`（来源：moyuan_business_modules.md·业务模块说明/5. 论坛模块/API 接口）
*   **评论条件**：用户必须登录；支持多级回复。
    *   **接口**：`POST /api/forum/comments/{id}`（来源：moyuan_business_modules.md·业务模块说明/5. 论坛模块/API 接口）
*   **点赞条件**：用户必须登录；同一内容不可重复点赞，撞库视为取消并扣减计数。
    *   **接口**：`POST /api/forum/posts/{id}/like`（来源：moyuan_business_modules.md·业务模块说明/5. 论坛模块/API 接口）

**2. 帖子列表展示置顶与精华的逻辑**
*   **展示逻辑**：列表按 `status=1` 过滤，结合 `category` 和 `keyword` 进行筛选；再依据 `isTop` 字段进行排序（倒序分页），最后批量填充用户信息。
    *   **核心机制**：后端模块 `ForumController` 中的 `ForumPostServiceImpl` 执行列表查询与分页逻辑（来源：moyuan_system_workflow.md·系统工作流文档/12. 论坛与社区域/12.1 发帖/回帖评论/点赞（E5））。
*   **功能支持**：论坛模块明确包含“置顶帖功能”和“精华帖推荐”功能（来源：moyuan_business_modules.md

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 5. 论坛模块 / 功能说明】
【章节：业务模块说明 / 5. 论坛模块 / 功能说明】
### 功能说明

- 帖子列表浏览
- 帖子发布（富文本编辑）
- 帖子详情查看
- 评论功能（支持多级回复）
- 点赞功能
- 精华帖推荐
- 置顶帖功能

【moyuan_business_modules.md · 业务模块说明 / 5. 论坛模块 / API 接口】
【章节：业务模块说明 / 5. 论坛模块 / API 接口】
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

【moyuan_business_modules.md · 业务模块说明 / 5. 论坛模块 / 前端路由】
【章节：业务模块说明 / 5. 论坛模块 / 前端路由】
### 前端路由

| 路径 | 说明 |
|------|------|
| `/forum` | 论坛列表页 |
| `/forum/create` | 发帖页 |
| `/forum/:id` | 论坛详情页 |

---

【moyuan_business_modules.md · 业务模块说明 / 6. 交流广场模块 / 功能说明】
【章节：业务模块说明 / 6. 交流广场模块 / 功能说明】
### 功能说明

- 轻量级交流空间，快速发帖和互动
- 帖子列表浏览（分页加载）
- 快速发帖（支持图片上传）
- 帖子详情展开
- 评论功能（支持多级回复）
- 点赞功能
- 时间格式化显示（刚刚、几分钟前、几小时前等）

【moyuan_business_modules.md · 业务模块说明 / 6. 交流广场模块 / API 接口】
【章节：业务模块说明 / 6. 交流广场模块 / API 接口】
### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/forum/posts | 帖子列表 | sc-moyuan-backend ForumController |
| POST /api/forum/posts | 发布帖子 | sc-moyuan-backend ForumController |
| POST /api/forum/posts/{id}/like | 点赞帖子 | sc-moyuan-backend ForumController |
| GET /api/forum/comments | 评论列表 | sc-moyuan-backend ForumController |
| POST /api/forum/comments | 发表评论 | sc-moyuan-backend ForumController |
| POST /api/forum/comments/{id}/like | 点赞评论 | sc-moyuan-backend ForumController |

【moyuan_business_modules.md · 业务模块说明 / 6. 交流广场模块 / 前端组件】
【章节：业务模块说明 / 6. 交流广场模块 / 前端组件】
### 前端组件

| 组件 | 说明 |
|------|------|
| communicate/index.vue | 交流广场主页面（帖子列表、发帖、评论、点赞） |

---

【moyuan_business_modules.md · 业务模块说明 / 10. 管理模块 / 诗词浏览流程】
【章节：业务模块说明 / 10. 管理模块 / 诗词浏览流程】
### 诗词浏览流程

```
用户访问首页 → 展示精选诗词 → 点击诗词 → 查看诗词详情 → 点赞/收藏
```

【moyuan_business_modules.md · 业务模块说明 / 10. 管理模块 / 论坛发帖流程】
【章节：业务模块说明 / 10. 管理模块 / 论坛发帖流程】
### 论坛发帖流程

```
用户登录 → 进入论坛 → 点击发帖 → 编辑内容 → 提交帖子 → 帖子列表展示
```

【moyuan_business_modules.md · 业务模块说明 / 10. 管理模块 / 搜索流程】
【章节：业务模块说明 / 10. 管理模块 / 搜索流程】
### 搜索流程

```
用户输入关键词 → 发起搜索请求 → 返回搜索结果 → 点击查看详情
```

---

【moyuan_system_workflow.md · 系统工作流文档 / 12. 论坛与社区域 / 12.1 发帖 / 回帖评论 / 点赞（E5）】
【章节：系统工作流文档 / 12. 论坛与社区域 / 12.1 发帖 / 回帖评论 / 点赞（E5）】
### 12.1 发帖 / 回帖评论 / 点赞（E5）
- **触发入口**：`GET|POST /api/forum/posts`、`GET|PUT|DELETE /api/forum/posts/{id}`、`POST|GET /api/forum/posts/{id}/like`、`GET|POST /api/forum/comments`、`DELETE /api/forum/comments/{id}`、`POST|GET /api/forum/comments/{id}/like`。
- **分步流程**：列表按 status=1 过滤 + category/keyword，isTop 再 createTime 倒序分页，`fillUserInfo` 批量填充昵称/头像；详情 `view_count+1`；发帖写 title/content/category，状态直接可见；评论 `Comment.createComment` 写 comment（parentId 默认 0），`targetType==2`（POST）时更新帖子 `comment_count+1、last_comment_time`，删除回减；点赞 `toggleLike` 插入 `user_like`（唯一约束），撞 `DuplicateKeyException` 视为取消点赞并 `like_count-1`（>0 保护）。
- **关键类与方法**：`ForumController.*`、`ForumPostServiceImpl.*`、`CommentServiceImpl.createComment/deleteComment/toggleLike/getCommentsByTarget`；`TargetType`（POEM=1/POST=2/COMMENT=3）。
- **数据与缓存**：`forum_post`、`comment`、`user_like`。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 诗汇论坛（page.forum）】
【章节：墨渊小助手系统知识库（24 条） / 诗汇论坛（page.forum）】
## 诗汇论坛（page.forum）

- 分类：page
- 关键词：论坛,诗汇论坛,发帖,讨论,交流
- 别名：论坛
- 前端路由：/forum
- 说明：诗汇论坛是诗词交流社区，登录后可发布新帖，浏览帖子、评论与点赞。帖子列表支持置顶与精华展示，帖子详情页可查看正文与多级评论。
- 使用步骤：
  1. 进入诗汇论坛
  2. 浏览帖子列表或搜索
  3. 登录后点击发布新帖
  4. 在帖子详情中评论、点赞
- 常见问答：
  - 问：论坛在哪里  答：在顶部导航进入诗汇论坛，可浏览帖子、发帖、评论与点赞。
  - 问：论坛怎么发帖  答：登录后进入诗汇论坛，点击发布新帖，编辑标题与内容后提交。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 评论与互动（feature.comment）】
【章节：墨渊小助手系统知识库（24 条） / 评论与互动（feature.comment）】
## 评论与互动（feature.comment）

- 分类：feature
- 关键词：评论,点赞,互动,回复
- 别名：评论,互动
- 前端路由：/forum
- 说明：在诗词、帖子与文章详情页可以对内容点赞与评论，论坛评论支持多级回复。评论需要有账号登录。
- 使用步骤：
  1. 打开诗词、帖子或文章详情
  2. 点击点赞或发表评论
  3. 在论坛可对评论进行回复
- 常见问答：
  - 问：怎么评论  答：在诗词、帖子或文章详情页可以直接发表评论与点赞，论坛评论支持多级回复。
```
</details>

## M4（medium）诗人认证从提交到通过要经历哪些状态？管理员用哪个接口审核？用户从哪里提交？

**回答**：诗人认证从提交到通过需经历“未认证（0）→ 认证中/待审核（2）→ 已认证（1）”三个状态，若被拒则为“认证失败（3）”。管理员使用 `PUT /api/admin/poet-profiles/{id}/verify` 接口执行审核操作。用户通过个人中心页面提交申请，具体入口为前端路由 `/user/profile` 中的诗人认证功能模块。

- **状态流转**：初始为未认证（0），提交申请后变为认证中（2），管理员审核通过后更新为已认证（1），拒绝则为认证失败（3）。（来源：moyuan_business_modules.md · 业务模块说明 / 4. 诗人认证模块 / 认证状态）
- **审核接口**：`PUT /api/admin/poet-profiles/{id}/verify`，属于 `sc-moyuan-backend AdminController` 模块，用于更新认证状态。（来源：moyuan_business_modules.md · 业务模块说明 / 4. 诗人认证模块 / API 接口）
- **提交入口**：用户登录后进入个人中心（路由 `/user/profile`），在该页面找到诗人认证申请入口进行资料填写并提交。（来源：moyuan_business_modules.md · 业务模块说明 / 9. 用户模块 / 功能说明；moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条）/ 个人中心（page.profile））

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 4. 诗人认证模块 / 功能说明】
【章节：业务模块说明 / 4. 诗人认证模块 / 功能说明】
### 功能说明

- 诗人认证申请：用户提交诗人认证申请，填写诗人资料
- 认证审核：管理员审核诗人认证申请，通过或拒绝
- 诗人资料管理：认证诗人可管理自己的诗人资料
- 认证状态展示：用户主页展示诗人认证状态和资料

【moyuan_business_modules.md · 业务模块说明 / 4. 诗人认证模块 / 认证流程】
【章节：业务模块说明 / 4. 诗人认证模块 / 认证流程】
### 认证流程

```
用户提交认证申请 → 管理员审核 → 审核通过/拒绝
    ↓                              ↓
填写诗人资料                    更新认证状态
（笔名、简介、擅长体裁等）        用户获得诗人标识
```

【moyuan_business_modules.md · 业务模块说明 / 4. 诗人认证模块 / 认证状态】
【章节：业务模块说明 / 4. 诗人认证模块 / 认证状态】
### 认证状态

| 状态值 | 说明 |
|--------|------|
| 0 | 未认证 |
| 1 | 已认证 |
| 2 | 认证中（待审核） |
| 3 | 认证失败 |

【moyuan_business_modules.md · 业务模块说明 / 4. 诗人认证模块 / 诗人资料字段】
【章节：业务模块说明 / 4. 诗人认证模块 / 诗人资料字段】
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

【moyuan_business_modules.md · 业务模块说明 / 4. 诗人认证模块 / API 接口】
【章节：业务模块说明 / 4. 诗人认证模块 / API 接口】
### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/poet-profile/me | 获取当前用户诗人资料 | sc-moyuan-backend PoetProfileController |
| POST /api/poet-profile/apply | 申请诗人认证 | sc-moyuan-backend PoetProfileController |
| GET /api/poet-profile/{userId} | 获取指定用户诗人资料 | sc-moyuan-backend PoetProfileController |
| GET /api/admin/poet-profiles | 获取诗人认证申请列表（管理员） | sc-moyuan-backend AdminController |
| PUT /api/admin/poet-profiles/{id}/verify | 审核诗人认证（管理员） | sc-moyuan-backend AdminController |

【moyuan_business_modules.md · 业务模块说明 / 4. 诗人认证模块 / 前端路由】
【章节：业务模块说明 / 4. 诗人认证模块 / 前端路由】
### 前端路由

| 路径 | 说明 |
|------|------|
| `/user/:id/homepage` | 用户主页（展示诗人认证状态和资料） |
| `/user/profile` | 个人中心（提交认证申请入口） |

---

【moyuan_business_modules.md · 业务模块说明 / 9. 用户模块 / 功能说明】
【章节：业务模块说明 / 9. 用户模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 10. 管理模块 / API 接口】
【章节：业务模块说明 / 10. 管理模块 / API 接口】
| backend AdminController |
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
| PUT | /api/admin/poet-featured/{id} | 更新精选诗人 | sc-moyuan-backend AdminController

【moyuan_system_workflow.md · 系统工作流文档 / 12. 论坛与社区域 / 12.2 内容审核流（E6）】
【章节：系统工作流文档 / 12. 论坛与社区域 / 12.2 内容审核流（E6）】
verified_status=0` | `PUT /api/admin/poet-profiles/{id}/verify` |
  | 诗人草稿 | `poet_draft.status=0` | `PUT /api/admin/poet-drafts/{id}/review` |
  | 投稿建议 | `poet_suggestion.status=pending` | `PUT /api/admin/poet-suggestions/{id}/review` |
- **后台审核列表接口与前端取数约定**：
  - 列表查询：诗作 `GET /api/admin/poems`（参数 `page`、`size`、`keyword`、`auditStatus`、`sort`；`auditStatus` 0-待审核/1-已通过/2-已拒绝，不传返回全部；`sort` 取 asc-按创建时间正序/desc-倒序，默认 desc）；评论 `GET /api/admin/comments`（参数 `page`、`size`、`status`，1-正常/0-隐藏）；AI 内容 `GET /api/admin/ai-contents`（参数 `page`、`size`、`status`，0-待审核/1-已通过/2-已拒绝）。诗人草稿/建议用 `pageNum`、`pageSize`、`status`。
  - 审核变更：`PUT /api/admin/poems/{id}/audit` 的 body `{status, reason}` 中 `status` 对应 `auditStatus` 语义（0/1/2，拒绝为 2）；`PUT /api/admin/comments/{id}/audit` 的 `status` 为 1-正常/0-隐藏。
  - 前端取数：统一响应体为 `{code, message, data}`，Axios 拦截器返回整个响应体，故后台列表页面必须从 `res.data.records` / `res.data.total` 取值（不可写 `res.records`）；诗作审核页状态列与操作判断绑定 `row.auditStatus`，作者/朝代列绑定后端填充的 `poetName` / `dynastyName`（由 `PoemServiceImpl.getAdminPoemList` 调 `populatePoetAndDynasty` 填充），评论页状态列绑定 `row.status`（1-正常/0-隐藏）。
- **关键类与方法**：`AdminController.

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 个人中心（page.profile）】
【章节：墨渊小助手系统知识库（24 条） / 个人中心（page.profile）】
## 个人中心（page.profile）

- 分类：page
- 关键词：个人中心,我的,资料,收藏,历史,认证
- 别名：个人主页,我的页面
- 前端路由：/user/profile
- 说明：个人中心可查看与修改个人资料、上传头像、修改密码，并查看我的收藏、我的点赞、我的帖子和浏览历史。诗人认证申请也从个人中心提交。需要登录后进入。
- 使用步骤：
  1. 登录后进入个人中心
  2. 修改资料、头像或密码
  3. 查看收藏、点赞、帖子与浏览历史
  4. 需要时提交诗人认证申请
- 常见问答：
  - 问：个人中心在哪里  答：登录后可从用户菜单进入个人中心，管理资料、收藏、历史并提交诗人认证。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 诗人认证（feature.poet-cert）】
【章节：墨渊小助手系统知识库（24 条） / 诗人认证（feature.poet-cert）】
## 诗人认证（feature.poet-cert）

- 分类：feature
- 关键词：诗人认证,认证,成为诗人,诗人标识
- 别名：认证诗人,诗人身份
- 前端路由：/user/profile
- 说明：诗人认证可让你申请成为认证诗人，提交笔名、简介、擅长体裁等资料，由管理员审核，通过后获得诗人标识并在用户主页展示。认证申请从个人中心提交，需要登录。
- 使用步骤：
  1. 登录后进入个人中心
  2. 提交诗人认证申请并填写资料
  3. 等待管理员审核
  4. 通过后在用户主页展示认证状态
- 常见问答：
  - 问：怎么成为认证诗人  答：登录后在个人中心提交诗人认证申请，填写资料，通过管理员审核后即获得诗人标识。
```
</details>

## M5（medium）一份报修工单从创建到关闭再到满意度评价，经历了哪些状态和接口？用户在哪个页面操作？

**回答**：一份报修工单从创建到关闭及满意度评价，需经历“待处理→处理中→已解决→已关闭”的状态流转，并调用对应 API 接口；用户主要在“报修中心”页面进行操作。

**状态流转与接口：**
- **创建阶段**：生成唯一编号（格式 RO+yyyyMMddHHmmssSSS+随机3位），初始状态为待处理(0)，优先级默认2。
  - 接口：`POST /api/repair/orders`（提交工单）
- **派单阶段**：管理员分配处理人，状态转为处理中(1)。
  - 接口：`PUT /api/admin/repairs/{id}/assign`（分配处理人）
- **流转阶段**：状态更新为已解决(2)或已关闭(3)，系统记录 resolveTime 或 closeTime。
  - 接口：`PUT /api/admin/repairs/{id}/status`（管理员更新状态）、`PUT /api/repair/orders/{id}/close`（用户关闭工单）
- **评价阶段**：提交满意度评价（1-5星+评语）。
  - 接口：`PUT /api/repair/orders/{id}/satisfaction`（提交评价）

**用户操作页面：**
- 用户需登录后进入“报修中心”页面（前端路由 `/repair`），在此填写并提交工单，查看列表与详情，添加评论，执行关闭及评价操作。
（来源：moyuan_business_modules.md · 业务模块说明 / 18. 报修模块 / API 接口；moyuan_system_workflow.md · 系统工作流文档 / 13.1 工单全生命周期（E7）；moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条）/ 报修中心（page.re

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 模块总览】
【章节：业务模块说明 / 模块总览】
|
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
| 23 | 诗云模块 | Three.

【moyuan_business_modules.md · 业务模块说明 / 10. 管理模块 / API 接口】
【章节：业务模块说明 / 10. 管理模块 / API 接口】
keyword 按姓名/朝代筛选） | sc-moyuan-backend AdminController |
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
| POST | /api/admin/poets/{id}/sync |

【moyuan_business_modules.md · 业务模块说明 / 10. 管理模块 / 前端路由】
【章节：业务模块说明 / 10. 管理模块 / 前端路由】
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

【moyuan_business_modules.md · 业务模块说明 / 18. 报修模块 / 功能说明】
【章节：业务模块说明 / 18. 报修模块 / 功能说明】
### 功能说明

- 用户提交报修工单（标题、描述、分类、优先级）
- 查看我的报修列表和详情
- 添加反馈评论
- 关闭工单
- 提交满意度评价（1-5星 + 评语）
- 管理端：工单管理、分配处理人、状态流转、内部备注、统计

【moyuan_business_modules.md · 业务模块说明 / 18. 报修模块 / 工单状态流转】
【章节：业务模块说明 / 18. 报修模块 / 工单状态流转】
### 工单状态流转

```
待处理(0) → 处理中(1) → 已解决(2) → 已关闭(3)
```

【moyuan_business_modules.md · 业务模块说明 / 18. 报修模块 / API 接口】
【章节：业务模块说明 / 18. 报修模块 / API 接口】
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

【moyuan_system_workflow.md · 系统工作流文档 / 13. 报修工单域 / 13.1 工单全生命周期（E7）】
【章节：系统工作流文档 / 13. 报修工单域 / 13.1 工单全生命周期（E7）】
### 13.1 工单全生命周期（E7）
- **目标**：用户报修 → 管理员分配/处理/关闭 → 满意度评价。
- **触发入口**：用户 `POST /api/repair/orders`、`GET /api/repair/orders`、`.../{id}`、`.../{id}/comments`、`PUT .../close`、`PUT .../satisfaction`；管理 `GET/PUT /api/admin/repairs…`、`/assign`、评论等。
- **分步流程**：
  1. **创建**：`generateOrderNo`=`RO`+yyyyMMddHHmmssSSS+随机3位；status=0 待处理、priority 默认 2。
  2. **派单**：设置 assigneeId，待处理→处理中(status=1)。
  3. **流转** `updateStatus`：status=2 记 resolveTime，3 记 closeTime，4 拒绝。
  4. **沟通**：`addComment` 写 `repair_comment`（用户评论 isInternal=0；管理端内部备注 isInternal=1，用户侧查询过滤）。
  5. **关闭/评价**：`closeOrder` 置 3（拒绝重复关闭）；`submitSatisfaction` 写满意度及评语。详情/列表校验归属权限。
  6. **统计** `getStats` 按状态计数 total/pending(0)/processing(1)/resolved(2)/closed(3)/rejected(4)。
- **关键类与方法**：`RepairOrderServiceImpl.generateOrderNo/createOrder/assignOrder/updateStatus/closeOrder/submitSatisfaction/getStats`、`RepairController.*`、`AdminController.listRepairs/updateRepairStatus/assignRepair/getRepairStats`。
- **数据与缓存**：`repair_order`（status 状态机 0/1/2/3/4、order_no 唯一）、`repair_comment`（is_internal）。

---

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 报修中心（page.repair）】
【章节：墨渊小助手系统知识库（24 条） / 报修中心（page.repair）】
## 报修中心（page.repair）

- 分类：page
- 关键词：报修,反馈,工单,故障,问题反馈
- 别名：报修,反馈中心
- 前端路由：/repair
- 说明：报修中心用于提交报修工单，可填写标题、描述、分类与优先级，提交后查看我的报修列表与详情、添加反馈评论、关闭工单并提交满意度评价。需要登录后进入。
- 使用步骤：
  1. 登录后进入报修中心
  2. 点击提交报修并填写工单信息
  3. 在列表中查看工单详情与进度
  4. 添加评论、关闭工单并评价
- 常见问答：
  - 问：怎么报修  答：登录后进入报修中心，点击提交报修，填写标题、描述、分类与优先级后提交。
```
</details>

## M6（medium）智能搜索没有关键词时怎么处理？带关键词时三路并发分别做什么？

**回答**：当智能搜索无关键词时，系统执行条件筛选与排序；有关键词时则启动三路并行搜索并合并评分。具体处理如下：

1. **无关键词处理**：
   - 调用 `searchWithoutKeyword()` 方法。
   - 依据前端传入的筛选维度（朝代、分类、诗人）进行精确或模糊匹配。
   - 对结果按 `sortBy` 参数指定的字段排序（最新/latest、热门/popular、点赞/likes）。
   - 执行分页返回，并保存已登录用户的搜索历史（来源：moyuan_business_modules.md·业务模块说明 / 8. 搜索模块 / 搜索流程；moyuan_system_workflow.md·系统工作流文档 / 10.1 全文/跨模块智能搜索）。

2. **有关键词时的三路并发任务**：
   - **`searchPoems()`（标题/内容匹配）**：
     - 默认开启 MySQL ngram 全文检索，使用 `MATCH(title, content) AGAINST(...)`。
     - 若关闭全文模式，则对标题和内容进行去标点拆分词后执行 LIKE 模糊匹配。
     - 结果经 `calculateScore` 打分（标题精确 1.0/包含 0.8/拆分 0.3；内容包含 0.5/拆分 0.2）。
   - **`searchByPoetName()`（诗人名匹配）**：
     - 先通过 `poetName` 模糊查询获取诗人 ID 列表，再关联查找该诗人的作品。
     - 评分逻辑：精确匹配 0.9、包含 0.7、部分匹配 0.3。
   - **`searchByPinyin()`（拼音联想）**：
     - 调用 `PinyinUtil.matchesPinyin` 工具类进行拼音匹配。
     -

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 技术方案】
【章节：业务模块说明 / 8. 搜索模块 / 技术方案】
### 技术方案

- **搜索核心**：SmartSearchService，多条件复合查询 + 评分排序
- **搜索引擎**：MySQL 8 ngram 中文全文检索（`/api/search/smart` 关键词默认走 `MATCH(title,content) AGAINST(...)`，`search.fulltext-enabled` 默认开启，可经 `SEARCH_FULLTEXT_ENABLED` 关闭回退 LIKE）；Elasticsearch 属后期可选优化
- **拼音支持**：PinyinUtil 工具类，可配置搜索范围（默认200条，最大1000条）
- **缓存策略**：Redis 缓存搜索建议（5分钟）、热门搜索（实时统计）、搜索历史（30天）
- **推荐服务**：RecommendationService，协同过滤 + 兴趣推荐 + 外部API
- **外部API集成**：今日诗词API，补充古诗词数据源

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 复合搜索策略】
【章节：业务模块说明 / 8. 搜索模块 / 复合搜索策略】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 搜索流程】
【章节：业务模块说明 / 8. 搜索模块 / 搜索流程】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / API 接口】
【章节：业务模块说明 / 8. 搜索模块 / API 接口】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 智能搜索参数】
【章节：业务模块说明 / 8. 搜索模块 / 智能搜索参数】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 前端路由】
【章节：业务模块说明 / 8. 搜索模块 / 前端路由】
### 前端路由

| 路径 | 说明 |
|------|------|
| `/poem` | 诗词列表页（含复合筛选搜索） |
| `/` (首页) | 历史的印痕模块内嵌搜索功能 |

> 说明：项目未设置独立的 `/search` 结果页路由，全局搜索能力内嵌在首页与诗词列表页中通过 `/api/search/smart` 实现。

---

【moyuan_system_workflow.md · 系统工作流文档 / 10. 搜索与运营域 / 10.1 全文/跨模块智能搜索（C8）】
【章节：系统工作流文档 / 10. 搜索与运营域 / 10.1 全文/跨模块智能搜索（C8）】
### 10.1 全文/跨模块智能搜索（C8）
- **触发入口**：`GET /api/search/smart?dynastyId&categoryId&poetId&poetName&keyword&sortBy&pageNum&pageSize`；`GET /api/search?keyword=`（三段返回 poems/poets/posts）；`GET /api/search/poems`；`GET /api/search/suggestions|hot|history`、`DELETE /api/search/history`。
- **分步流程**（`SmartSearchServiceImpl.smartSearch`）：
  1. 有 keyword 且已登录 → `saveSearchHistory`。
  2. `resolvePoetNameIds`：poetName 对 poet.name 模糊匹配取 ID 集。
  3. 无 keyword → `searchWithoutKeyword` 按筛选条件分页 + 排序（最新/热门/点赞）。
  4. 有 keyword 三路并发，命中结果统一按 poem.id 去重取高分、内存分页，为空置 `suggestExternal=true`：
     - `searchPoems`（名称匹配，**双模式**）：
       - 关闭全文（`search.fulltext-enabled=false`）：对 title/content 用原始词 + 去标点拆分词 `like` 模糊匹配。
       - **默认开启**（`search.fulltext-enabled=true`，见 `application.yml`）：改用 ngram 中文全文索引 `MATCH(title, content) AGAINST(... IN NATURAL LANGUAGE MODE)`（`poem` 表 `ft_poem_title_content` 已 `WITH PARSER ngram`，中文分词 + 相关度更高，见 `docs/database/migration_004_poem_search_fulltext.sql`）。
       - 两模式命中集统一经 `scoreAndRank` 按 `calculateScore` 打分：标题精确 1.0/包含 0.8/拆分 0.3、内容包含 0.5/拆分 0.2。
     - `searchByPoetName`：匹配诗人名取作品，精确 0.9/包含 0.7/部分 0.3。
     - `searchByPinyin`：`PinyinUtil.matchesPinyin` 拼音联想，基础 0.3+标题 0.2+诗人 0.1。
  5.

【moyuan_system_workflow.md · 系统工作流文档 / 10. 搜索与运营域 / 10.1 全文/跨模块智能搜索（C8）】
【章节：系统工作流文档 / 10. 搜索与运营域 / 10.1 全文/跨模块智能搜索（C8）】
建议：先查 Redis `search:suggestion:{kw}`（5min），未命中从 poem 标题+诗人名并集。
  6. 热门：读 Redis ZSet `search:hot` 逆序；空返回默认词。历史：`search:history:{userId}` List（remove+leftPush+trim(0,19)+30天过期），同时 `zset.incrementScore` 累加热度。
  7. `/api/search/poems` 走 `recommendationService.searchPoems`：本地（附 `recommendScore` 含兴趣加权+浏览/点赞对数分），不足时拉外部（author 含 keyword，`recommendScore=0.5`）合并降序。
- **关键类与方法**：`SmartSearchServiceImpl.*`、`SearchController.*`、`PinyinUtil.matchesPinyin`、`RecommendationServiceImpl.searchPoems/getExternalPoems/calculatePoemRecommendScore`；配置 `search.fulltext-enabled`（默认 true，全文已开启，可用环境变量 `SEARCH_FULLTEXT_ENABLED` 覆盖）、`search.pinyin.limit`。
- **数据与缓存**：Redis `search:suggestion:{kw}`、`search:hot`(ZSet)、`search:history:{userId}`(List)；`poem` 表全文索引 `ft_poem_title_content`（ngram）。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 智能检索（feature.search）】
【章节：墨渊小助手系统知识库（24 条） / 智能检索（feature.search）】
## 智能检索（feature.search）

- 分类：feature
- 关键词：搜索,检索,找诗,搜诗人,关键字
- 别名：智能搜索,全文搜索
- 前端路由：/poem
- 说明：智能检索支持按关键词搜索诗词、诗人与帖子，首页与诗词列表页内嵌了搜索入口。搜索会记录你的历史，登录后还会根据兴趣给出推荐。
- 使用步骤：
  1. 在首页或诗词列表页输入关键词
  2. 查看匹配的诗词、诗人或帖子
  3. 点开结果查看详情
- 常见问答：
  - 问：怎么搜索  答：在首页或诗词列表页的搜索框输入关键词，即可搜索诗词、诗人与帖子。
```
</details>

## M7（medium）文件上传时会做哪些校验和处理？上传接口是什么？

**回答**：文件上传时进行格式、大小及唯一性校验，并通过后端服务完成存储与元数据管理。具体流程如下：

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 模块总览】
【章节：业务模块说明 / 模块总览】
## 模块总览

| 序号 | 模块 | 说明 | 状态 |
|------|------|------|------|
| 1 | 首页模块 | 网站入口，展示精选内容 | 前端已实现（连接后端 API） |
| 2 | 诗词模块 | 诗词浏览、搜索、赏析、用户发布 | 前端+后端已实现 |
| 3 | 诗人模块 | 诗人介绍、作品集、智能推荐 | 前端+后端已实现 |
| 4 | 诗人认证模块 | 诗人认证申请、审核、资料管理 | 前端+后端已实现 |
| 5 | 论坛模块 | 用户交流、发帖评论 | 前端+后端已实现 |
| 6 | 交流广场模块 | 轻量级交流空间，快速发帖和互动 | 前端+后端已实现 |
| 7 | AI模块 | AI 辅助创作、看图写诗、智能分析、AI对联、藏头诗、平仄格律校验、古籍OCR识文（维护中）、AI内容填充与审核、AI模型配置管理 | 前端+后端已实现 |
| 8 | 搜索模块 | 全文搜索、复合筛选、拼音匹配、搜索建议/历史 | 前端+后端已实现 |
| 9 | 用户模块 | 注册登录、个人中心、用户主页 | 前端+后端已实现 |
| 10 | 管理模块 | 后台管理、内容管理、数据统计、内容审核 | 前端+后端已实现 |
| 11 | 文件模块 | 文件上传、图片管理、AI水印 | 前端+后端已实现 |
| 12 | 浏览历史模块 | 浏览记录、历史查看 | 前端+后端已实现 |
| 13 | 首页导航模块 | 首页导航菜单管理（作品/流派/朝代） |

【moyuan_business_modules.md · 业务模块说明 / 11. 文件模块 / 功能说明】
【章节：业务模块说明 / 11. 文件模块 / 功能说明】
### 功能说明

- 统一文件上传功能（支持图片上传，按类型分目录存储）
- 文件元数据管理（file_metadata表）
- AI生成图片记录管理（ai_image_record表）
- 图片预览和管理
- 文件存储和访问
- AI图片自动水印功能
- 缩略图生成

【moyuan_business_modules.md · 业务模块说明 / 11. 文件模块 / API 接口】
【章节：业务模块说明 / 11. 文件模块 / API 接口】
### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| POST /api/files/upload | 上传文件（支持fileType、relatedId、relatedType参数） | sc-moyuan-backend FileController |
| DELETE /api/files/{fileKey} | 删除文件 | sc-moyuan-backend FileController |
| GET /api/files/{fileKey} | 获取文件信息 | sc-moyuan-backend FileController |

【moyuan_business_modules.md · 业务模块说明 / 25. AI 对话页「诗宝阁」模块 / 技术方案】
【章节：业务模块说明 / 25. AI 对话页「诗宝阁」模块 / 技术方案】
### 技术方案

- 对话主链路为 SSE 流式 `POST /api/ai/chat/stream`：前端用 `fetch` + `ReadableStream` 解析（Axios 不支持读取流、EventSource 不支持 POST）并逐段渲染；网络中断等可重试失败才回退非流式 `POST /api/ai/chat`
- 请求组装为真正的 system role：`[{role:'system', content: persona.systemPrompt}, …最近 12 条历史, {role:'user', content: skill ? skill.buildPrompt(raw) : raw}]`（历史单条 ≤2000 字、合计 ≤8000 字），不再把角色设定前缀塞进 user 消息
- 限流/额度/鉴权类错误（400/401/403/429 与 4290~4293）**不**回退非流式——回退会二次扣配额并掩盖真实原因，直接展示后端 `message`
- 流式正常返回 `text/event-stream`；限流与参数异常由 `@RestControllerAdvice` 以 JSON 返回，前端按 `content-type` 区分后抛出 `message`，错误对象携带 `status`/`code` 供判断是否可重试
- 生成中可停止：发送按钮切换为「停止」，`abort` 后保留已生成内容并持久化；`finally` 中比对 `AbortController` 实例，避免旧请求复位新请求的 loading
- 会话同步：切换角色 / 新建会话 / 首次进入时从服务端拉取；本地有而服务端为空时反向上传；`sessionSeq` 防止快速切换角色串台
- `moduleCode` 必传 `'chat'`（已绑定 DeepSeek）；否则回退智谱默认模型，会因 key 余额不足返回 `429 code 1113`
- 藏头诗任务模板复用首页现有实现并迁至能力层；平仄校验改调本地确定性引擎 `POST /api/ai/tone-check`
- 配色沿用墨渊棕金色系（宣纸底 / 铜边 / 棕金强调），禁用 Element Plus 默认蓝

【moyuan_system_workflow.md · 系统工作流文档 / 3. 工作流索引表】
【章节：系统工作流文档 / 3. 工作流索引表】
## 3. 工作流索引表

| 分域 | 包含工作流 | 关键后端入口 | 章节 |
|------|------------|--------------|------|
| 认证与权限域 | 注册 / 登录 / JWT校验 / 权限控制 | `AuthController`、`UserServiceImpl`、`JwtAuthenticationFilter`、`SecurityConfig` | [4](#4-认证与权限域) |
| 用户与个人中心域 | 资料更新 / 头像设置 / 浏览历史 | `UserController`、`UserServiceImpl`、`HistoryController` | [5](#5-用户与个人中心域) |
| 文件与资源域 | 文件上传全链路 | `FileController`、`FileStorageServiceImpl` | [6](#6-文件与资源域) |
| 审计与统一响应 | 操作日志 / 统一响应 | `OperationLogAspect`、`GlobalExceptionHandler`、`R` | [7](#7-审计与统一响应) |
| 诗词内容域 | 列表 / 详情缓存 / 收藏点赞 / 评分 / 外部导入 | `PoemController`、`PoemServiceImpl`、`PoemRatingServiceImpl`、`PoemContentCacheServiceImpl` | [8](#8-诗词内容域) |
| 诗人内容域 | 懒加载头像 / 推荐热门 / 认证草稿投稿 | `PoetController`、`PoetServiceImpl`、`RecommendationServiceImpl`、`PoetProfile/Draft/Suggestion*` | [9](#9-诗人内容域) |
| 搜索与运营域 | 智能搜索 / 押韵 / 访问热度 | `SearchController`、`SmartSearchServiceImpl`、`RhymeServiceImpl`、`VisitLogAspect` | [10](#10-搜索与运营域) |
| AI 能力域 | 创作/对话/内容生成 / 模型配置 | `AiController`、`AiServiceImpl`、`AiModelRegistry`、`AiGeneratedContentServiceImpl` | [11](#11-ai-能力域) |
| 论坛与社区域 | 发帖/评论/点赞 / 内容审核 | `ForumController`、`ForumPostServiceImpl`、`CommentServiceImpl` | [12](#12-论坛与社区域) |
|

【moyuan_system_workflow.md · 系统工作流文档 / 6. 文件与资源域 / 6.1 文件上传全链路（A6）】
【章节：系统工作流文档 / 6. 文件与资源域 / 6.1 文件上传全链路（A6）】
### 6.1 文件上传全链路（A6）
- **目标**：统一处理图片/文件上传，含格式与大小校验、MD5 秒传、分类存储、AI 水印与元数据入库。
- **触发入口**：`POST /api/files/upload`（multipart：`file`、`fileType`、可选 `relatedId`/`relatedType`）。
- **分步流程**（`FileStorageServiceImpl.upload`，`@Transactional`）：
  1. 空文件（400）、扩展名校验（`file.allowed-extensions`，默认 `.jpg,.jpeg,.png,.gif,.webp,.bmp`）、大小校验（`file.max-size` 默认 10MB）。
  2. 唯一文件名：`UUID(去横线)+扩展名`。
  3. 目录选择 `buildSubDirectory`：`avatar→avatars/{relatedId}/`、`poem→poems/{relatedId}/`、`user_poem→user_poems/{relatedId}/`、`forum→forum/{yyyy/MM}/`、`vision→vision/article_{relatedId}/`、`ai_generated→ai_generated/{yyyy-MM-dd}/` 等。
  4. 读字节 → `calculateMd5` → **MD5 秒传**：查 `file_metadata` 同 `md5` 且磁盘文件存在则直接复用，不重复存储。
  5. 写磁盘（父目录 mkdirs + Files.write）。
  6. **AI 水印**：`fileType=="ai_generated"` 且为 image 时，`WatermarkUtil.addAiWatermark` 生成水印图覆盖原文件。
  7. 图片尺寸读取（失败仅 warn 不阻断）；元数据 `file_metadata` 入库（fileKey=relativePath 等）。
  8. 返回 `{url: baseUrl+"/"+fileKey, fileName, fileKey}`；`/uploads/**` 由 `WebMvcConfig` 映射到磁盘目录并 `permitAll`。
  9. 配套：`DELETE /api/files/{fileKey}`（先删物理再删记录）、`GET /api/files/{fileKey}`。
  10.

【moyuan_system_workflow.md · 系统工作流文档 / 6. 文件与资源域 / 6.1 文件上传全链路（A6）】
【章节：系统工作流文档 / 6. 文件与资源域 / 6.1 文件上传全链路（A6）】
配套：`DELETE /api/files/{fileKey}`（先删物理再删记录）、`GET /api/files/{fileKey}`。
  10. **后台治理**：`GET /api/admin/files`（分页，fileType/keyword/status）、`GET /api/admin/files/stats`（总文件数、总占用、各类型占用、物理缺失数）、`POST /api/admin/files/clean-orphans`（扫描物理目录删除磁盘存在但库中无记录的孤儿文件，返回删除数量与释放字节）、`DELETE /api/admin/files/{id}`（按 id 查 fileKey 后走 `delete`，含物理文件）。
- **关键类与方法**：`FileStorageServiceImpl.upload/delete/getUrl/listFiles/getStorageStats/cleanOrphanFiles/buildSubDirectory/calculateMd5/applyWatermark`、`FileController`（用户端）、`AdminFileController`（管理端）、`WatermarkUtil.addAiWatermark`、`WebMvcConfig.addResourceHandlers`。
- **数据与缓存**：`file_metadata`（file_key/original_name/file_type/mime_type/size/md5/width/height/related_* 等）；配置 `file.upload-dir`（默认 `./uploads`）、`file.base-url`、`file.allowed-extensions`、`file.max-size`。

---

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.5 首页数据组装（K5）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.5 首页数据组装（K5）】
### 15.5 首页数据组装（K5）
- **轮播图**：静态 `/img/lb_*.jpg`、`/img/lb_shiwen_*.png`，无后端。
- **今日诗词**：`DailyPoetry` → 后端 `GET /api/external-poetry/jinrishici` 代理（`v2.jinrishici.com/one.json`，后端 60 分钟进程内缓存，前端不再直连）。
- **首页导航**：`GET /home-navigation`。
- **精选诗人**：`getRandomPoetFeatured(8)`，去重取 4 位，每 20s 轮换。
- **论坛预览**：`GET /forum/posts?pageSize=5`。
- **古诗精赏**：本地 json + `GET /search/poems` + `/search/poems/recommended`。
- **当代精选**：本地 json + like/favorite/comment 接口。
- **AI 区块**：`/ai/write-poem`、`/ai/analyze`、`/ai/couplet`、`/ai/ocr`、`/ai/chat`（藏头诗、平仄格律校验，复用 `POST /api/ai/chat` 并传 `moduleCode=chat`，不新增后端接口）、`/rhyme/*`；"诗海拾贝"本地 json。
- **小游戏区块**：首页 AI 小工具区「飞花令」「诗词接龙」两张卡片——游客点击展开内嵌小窗口（`GameMiniPanel`）直接开局，登录用户跳转 `/game/feihua`、`/game/chain`；后端为独立游戏模块（`/api/game/feihua`、`/api/game/chain`，见 `modules.md` 第 24 章）。
- **诗话视野**：`GET /vision/articles`。
- **诗词卡片跳转**：首页「古诗精赏 / 当代精选」卡片点击经组合式函数 `composables/usePoemCardNavigation.ts` 处理——本地命中直接 `router.push('/poem/{id}')`，未命中回退 `PoetryDetailDialog`（外部诗词弹窗，可由接口盒子实时增强）。
- **关键位置**：`views/home/index.vue`、`components/{home,business}/*`、`composables/usePoemCardNavigation.ts`、`api/modules/*`。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 诗宝阁AI对话（play.bao-ge）】
【章节：墨渊小助手系统知识库（24 条） / 诗宝阁AI对话（play.bao-ge）】
## 诗宝阁AI对话（play.bao-ge）

- 分类：play
- 关键词：诗宝阁,AI对话,创作,鉴赏,平仄校验,藏头诗
- 别名：诗阁,诗宝阁
- 前端路由：/ai-chat
- 说明：诗宝阁是统一的 AI 对话页面，AI 以不同角色与能力帮你完成命题作诗、集句成诗、藏头诗、润色入律、平仄校验、意象典故、诗话点评、飞花令、诗词接龙等任务。可在左侧选择角色与能力后直接对话，回复以流式方式逐字输出，生成过程中可随时点击「停止」。会话按角色保存在账号下并保留 7 天，换设备登录同一账号可以接着聊。入口在顶部导航的「诗宝阁」。
- 使用步骤：
  1. 打开诗宝阁页面
  2. 选择角色与能力
  3. 输入你的需求并发送
  4. 查看 AI 返回的文本、藏头高亮或平仄标注结果
- 常见问答：
  - 问：诗宝阁是什么  答：诗宝阁是 AI 对话页面，AI 分角色和能力，可帮你作诗、鉴赏、校验平仄、玩飞花令等。
  - 问：诗宝阁和首页的AI工具有什么区别  答：诗宝阁是统一的角色化对话入口，首页 AI 小工具区保留看图写诗、藏头诗、飞花令等快捷入口，两处并存。
  - 问：诗宝阁的对话会保存吗  答：会，按角色保存在你的账号下并保留 7 天，换设备登录同一账号也能接着聊。
```
</details>

## M8（medium）平台为什么用双 token？用户登出后旧的 access token 怎么失效？

**回答**：平台采用双 token（access 短时效 + refresh 续期）机制，旨在平衡安全性与用户体验：access token 有效期短（默认 24h），降低长期持有风险；refresh token 有效期长（默认 7 天），用于自动续期，避免频繁登录。用户登出后，旧 access token 通过刷新令牌（refresh token）的 `jti`（唯一标识）被加入黑名单从而失效。

具体失效机制如下：
1. **双 token 签发**：登录成功后，后端同时签发 access token（含随机 `jti`）和 refresh token（同 `jti`

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 25. AI 对话页「诗宝阁」模块 / 技术方案】
【章节：业务模块说明 / 25. AI 对话页「诗宝阁」模块 / 技术方案】
### 技术方案

- 对话主链路为 SSE 流式 `POST /api/ai/chat/stream`：前端用 `fetch` + `ReadableStream` 解析（Axios 不支持读取流、EventSource 不支持 POST）并逐段渲染；网络中断等可重试失败才回退非流式 `POST /api/ai/chat`
- 请求组装为真正的 system role：`[{role:'system', content: persona.systemPrompt}, …最近 12 条历史, {role:'user', content: skill ? skill.buildPrompt(raw) : raw}]`（历史单条 ≤2000 字、合计 ≤8000 字），不再把角色设定前缀塞进 user 消息
- 限流/额度/鉴权类错误（400/401/403/429 与 4290~4293）**不**回退非流式——回退会二次扣配额并掩盖真实原因，直接展示后端 `message`
- 流式正常返回 `text/event-stream`；限流与参数异常由 `@RestControllerAdvice` 以 JSON 返回，前端按 `content-type` 区分后抛出 `message`，错误对象携带 `status`/`code` 供判断是否可重试
- 生成中可停止：发送按钮切换为「停止」，`abort` 后保留已生成内容并持久化；`finally` 中比对 `AbortController` 实例，避免旧请求复位新请求的 loading
- 会话同步：切换角色 / 新建会话 / 首次进入时从服务端拉取；本地有而服务端为空时反向上传；`sessionSeq` 防止快速切换角色串台
- `moduleCode` 必传 `'chat'`（已绑定 DeepSeek）；否则回退智谱默认模型，会因 key 余额不足返回 `429 code 1113`
- 藏头诗任务模板复用首页现有实现并迁至能力层；平仄校验改调本地确定性引擎 `POST /api/ai/tone-check`
- 配色沿用墨渊棕金色系（宣纸底 / 铜边 / 棕金强调），禁用 Element Plus 默认蓝

【moyuan_system_workflow.md · 系统工作流文档 / 2. 全局约定与分层（引擎级机制） / 2.3 JWT 认证与过滤器链】
【章节：系统工作流文档 / 2. 全局约定与分层（引擎级机制） / 2.3 JWT 认证与过滤器链】
### 2.3 JWT 认证与过滤器链
- 无状态会话（`SessionCreationPolicy.STATELESS`），关闭 CSRF。
- `JwtAuthenticationFilter`（挂在 `UsernamePasswordAuthenticationFilter` 之前）从 `Authorization: Bearer` 取 token → 验签/过期 → 校验 **type=access** 且 `jti` 未被 `TokenBlacklistService` 拉黑 → 查库 → 组装 `LoginUser.getAuthorities()`（`ROLE_`+role）写入 SecurityContext。当前 JWT 采用**双 token**（access 短时效 + refresh 续期），登出/刷新将旧 token 的 `jti` 加入黑名单使其失效（详见 4.2、4.3）。
- 公开接口 `permitAll`（首页静态资源、Swagger、`/api/auth/**`、浏览类接口、`/uploads/**` 等）；`/api/admin/**` 需 `ROLE_ADMIN`；其余需登录。详细放行清单见 4.3。

【moyuan_system_workflow.md · 系统工作流文档 / 4. 认证与权限域 / 4.1 用户注册（A1）】
【章节：系统工作流文档 / 4. 认证与权限域 / 4.1 用户注册（A1）】
### 4.1 用户注册（A1）
- **目标**：完成参数校验、用户名/邮箱唯一性检查、密码 BCrypt 加密并落库，注册成功即签发 JWT 直接登录。
- **触发入口**：`POST /api/auth/register`（`@RequestBody RegisterRequest`）。
- **分步流程**：
  1. `@Valid` 校验：username 长度 3–20、password 长度 6–20、email/interests 可选；失败抛 `MethodArgumentNotValidException` → 400。
  2. `UserServiceImpl.register` 查 `user` 表：用户名重复抛 `USERNAME_EXISTS(1002)`、email 重复抛 `EMAIL_EXISTS(1003)`。
  3. `passwordEncoder.encode`（BCrypt）加密密码。
  4. 补全字段（nickname 缺省取 username、interests 逗号拼接、status=1）后 `userMapper.insert`（`@Transactional`）。
  5. `jwtUtil.generateToken(user.getId(), username)` 签发，返回 `TokenResponse{token, expiresIn}`。
- **关键类与方法**：`UserServiceImpl.register`、`AuthController.register`、`JwtUtil.generateToken`、`PasswordEncoderConfig`、`RegisterRequest`。
- **数据与缓存**：`user` 表；前端注册后写 `localStorage.token`。

【moyuan_system_workflow.md · 系统工作流文档 / 4. 认证与权限域 / 4.2 登录与 JWT 签发（A2）】
【章节：系统工作流文档 / 4. 认证与权限域 / 4.2 登录与 JWT 签发（A2）】
### 4.2 登录与 JWT 签发（A2）
- **目标**：校验账号密码与禁用状态，成功后刷新登录信息并签发 JWT。
- **触发入口**：`POST /api/auth/login`（`LoginRequest{username,password}`）。
- **分步流程**：
  1. 按 username 查 `user`：无抛 `USER_NOT_FOUND(1001)`；`status!=1` 抛 `ACCOUNT_DISABLED(1005)`。
  2. `passwordEncoder.matches` 校验密码，失败抛 `PASSWORD_ERROR(1004)`。
  3. 更新 `lastLoginTime`、`lastLoginIp`（`getClientIp` 依 `X-Forwarded-For`→`Proxy-Client-IP`→`WL-Proxy-Client-IP`→`X-Real-IP`→`remoteAddr` 取值）。
  4. `JwtUtil` 签发**双 token**：access token（`jwt.expiration`，默认 24h，`type=access`，含随机 `jti`）与 refresh token（`jwt.refresh-expiration`，默认 7 天，`type=refresh`，同 `jti`）；响应经 `TokenResponse` 同时返回二者。
  5. 前端持久化两种 token，用于后续自动续期。
- **刷新与登出（#4 双 token）**：
  - `POST /api/auth/refresh`：携带 refresh token → 校验 type=refresh 且未过期 → **轮换**（旧 access/refresh 拉黑，签发新的一对）并返回。
  - `POST /api/auth/logout`：将当前 token 的 `jti` 加入黑名单，登出立即失效。
  - 黑名单实现 `TokenBlacklistService`（优先 Redis，失败降级进程内内存），key 关联 `jti`。
- **关键类与方法**：`UserServiceImpl.login/getClientIp`、`AuthController.login/refresh/logout`、`JwtUtil.generateAccessToken/generateRefreshToken`、`TokenBlacklistService.blacklist/isBlacklisted`。
- **数据与缓存**：`user.last_login_time/last_login_ip`；前端写 `localStorage`（`token`/`refreshToken`，封装为 `moyuan_token`）、admin 写 `lastAdminUsername`。

【moyuan_system_workflow.md · 系统工作流文档 / 4. 认证与权限域 / 4.3 JWT 校验与安全过滤器链认证（A3）】
【章节：系统工作流文档 / 4. 认证与权限域 / 4.3 JWT 校验与安全过滤器链认证（A3）】
### 4.3 JWT 校验与安全过滤器链认证（A3）
- **目标**：受保护请求进入 Controller 前完成 token 验签、身份装配。
- **触发入口**：任何携带 token 的 HTTP 请求（过滤器链自动触发）。
- **分步流程**：`JwtAuthenticationFilter.doFilterInternal` → `extractToken`（`Bearer ` 前缀）→ `jwtUtil.validateToken`（验签 + 过期 + **type=access**）→ `TokenBlacklistService.isBlacklisted(jti)` 黑名单校验 → `getUserId` → 查库 → `status==1` 时组装 `LoginUser` 并写 `UsernamePasswordAuthenticationToken` 到 `SecurityContextHolder`。无论成败都 `doFilter` 放行，访问权限由 `SecurityConfig` 决定。
- **关键类与方法**：`JwtAuthenticationFilter.doFilterInternal/extractToken`、`JwtUtil.validateToken/parseToken/getUserId`、`TokenBlacklistService.isBlacklisted`、`LoginUser.getAuthorities`、`SecurityConfig.filterChain`。
- **数据与缓存**：`user.id/status/role`。

【moyuan_system_workflow.md · 系统工作流文档 / 5. 用户与个人中心域 / 5.1 用户资料更新 / 头像设置（原 A5）】
【章节：系统工作流文档 / 5. 用户与个人中心域 / 5.1 用户资料更新 / 头像设置（原 A5）】
### 5.1 用户资料更新 / 头像设置（原 A5）
- **目标**：本人更新昵称/邮箱/手机/头像/简介/性别/生日或修改密码。
- **触发入口**：`PUT /api/users/me`（资料）；`PUT /api/users/me/password`（改密）；头像先 `POST /api/files/upload`（fileType=avatar）再回填。
- **分步流程**：`SecurityUtil.getCurrentUserId()` 取当前用户 → `updateUserInfo` 查库、对非空字段逐个 set 后 `updateById`（返回前 `setPassword(null)` 脱敏）；`updatePassword` 先 `matches` 旧密码（失败抛 1004）再 `encode` 覆盖。
- **关键类与方法**：`UserController.updateCurrentUser/updatePassword`、`UserServiceImpl.updateUserInfo/updatePassword`、`SecurityUtil.getCurrentUserId`。
- **数据与缓存**：`user.nickname/email/phone/avatar/bio/gender/birthday/password`。

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.1 统一请求层（K1）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.1 统一请求层（K1）】
### 15.1 统一请求层（K1）
- **目标**：为所有请求统一注入 token、处理响应码与 HTTP 错误。
- **分步流程**：`utils/request.ts` 创建 Axios 实例（`baseURL=VITE_API_BASE_URL||'/api'`、timeout 120000）→ 请求拦截器注入 `Authorization: Bearer ` → 响应 `code==='200'` 返回 `response.data`，否则 `ElMessage.error` 并 reject。**401 静默续期**：非公开接口（`PUBLIC_PATHS`）遇 HTTP 401 时，用本地 `refreshToken` 调 `/auth/refresh` 换取新一对 token → 写回 store/localStorage → **重放原请求**；续期失败或本身是 refresh 请求才 `logout()` 并跳 `/user/login`（`isRedirectingToLogin` 防重入）。导出 `get/post/put/del`。
- **关键位置**：`utils/request.ts`、`api/modules/*`。
- **数据与缓存**：请求头 `Authorization`。

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.2 登录态与用户 Store（K2）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.2 登录态与用户 Store（K2）】
### 15.2 登录态与用户 Store（K2）
- **分步流程**：`useUserStore` 初始化读 `localStorage` 中的 `token`/`refreshToken`，有则 `fetchUserInfo()`；`login/register` 写双 token 到 store + localStorage 后拉取用户；`fetchUserInfo` 读 `/users/me`，admin 写 `lastAdminUsername`，网络/5xx 指数重试≤2，401→尝试续期或 `logout()`；`logout` 清双 token 与 localStorage，并调用 `/auth/logout`（后端拉黑）。
- **关键位置**：`stores/user.ts`、`api/modules/user.ts`、`views/user/login.vue`、`router/guards.ts`。
- **数据与缓存**：localStorage `token`/`refreshToken`（双 token）、`lastAdminUsername`。

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.4 localStorage 使用点汇总（K4）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.4 localStorage 使用点汇总（K4）】
### 15.4 localStorage 使用点汇总（K4）
> **代码事实核对**：收藏、AI 对话、浏览历史**并未真正写入 localStorage**（收藏走服务端、AI 聊天在内存 ref、历史走 `/history` API）。真实落盘 key 如下：
- `token`/`refreshToken`（登录态双 token，user Store 直接写）
- `lastAdminUsername`（admin 记录）
- `home_nav_animation_config`（首页导航动画配置）
- `moyuan_token`（storage 封装的登录 token，带过期结构）
- `moyuan_user_preferences`（theme/language/fontSize/pageSize/sidebarCollapsed）
- `moyuan_search_history`（上限 20 条）
- `moyuan_form_draft_poem/forum/comment`
- `moyuan_sort_preferences`（{poem,poet,forum}）
- `moyuan_cache_dynasties/categories/poets`（筛选下拉，1h）
- `moyuan_cache_external_poem_`（外部诗词详情，24h）
- `moyuan_cache_`（外部搜索结果缓存）
- **收藏/AI对话/浏览历史**：无 localStorage key，分别走服务端接口/内存/API。
- **关键位置**：`utils/storage.ts`、`stores/preferences.ts/searchHistory.ts/formDraft.ts/apiCache.ts`。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 注册与登录（account.login）】
【章节：墨渊小助手系统知识库（24 条） / 注册与登录（account.login）】
## 注册与登录（account.login）

- 分类：account
- 关键词：注册,登录,账号,密码
- 别名：登录,注册
- 前端路由：/user/login
- 说明：注册时需要填写用户名、密码和邮箱，登录使用用户名与密码。登录后可享受收藏、发布、评论等完整功能。注册成功会直接登录。
- 使用步骤：
  1. 进入登录页
  2. 没有账号时切换到注册并填写信息
  3. 登录成功后使用完整功能
- 常见问答：
  - 问：怎么注册账号  答：在注册页填写用户名、密码和邮箱即可注册，注册成功会直接登录。
  - 问：怎么登录  答：在登录页输入用户名和密码即可登录。
```
</details>

## M9（medium）诗词详情页的赏析增强内容缓存在哪张表？缓存 key 由哪些字段组成？

**回答**：诗词详情页的赏析增强内容存储在 `poem_content_cache` 表中，缓存 key 由“诗词标题 + 诗人姓名 + 内容类型”三个字段组合而成。

具体依据如下：
1. **存储表**：资料明确记载数据库表为 `poem_content_cache`，用于缓存外部 API 获取的诗词内容（译文、注释、赏析等）。（来源：moyuan_business_modules.md · 业务模块说明 / 21. 诗词内容缓存模块 / 数据库表）
2. **缓存 key 组成**：在系统工作流文档中，诗词详情与赏析缓存（C2）流程指出，赏析增强内容由 `PoemContentCacheServiceImpl` 按 `poem_title+poet_name+content_type` 命中缓存；同时缓存策略速览中也确认了该组合规则。（来源：moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.2 诗词详情与赏析缓存（C2）；moyuan_system_workflow.md · 系统工作流文档 / 2. 全局约定与分层（引擎级机制） / 2.5 缓存策略速览）

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 3. 诗人模块 / 功能说明】
【章节：业务模块说明 / 3. 诗人模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 3. 诗人模块 / 前端组件】
【章节：业务模块说明 / 3. 诗人模块 / 前端组件】
### 前端组件

| 组件 | 说明 |
|------|------|
| PoetryDetailDialog | 诗词详情弹窗，展示接口盒子 API 返回的赏析内容 |
| ai-prompt.ts | AI提示词构建工具函数，支持变量替换 |

【moyuan_business_modules.md · 业务模块说明 / 21. 诗词内容缓存模块 / 功能说明】
【章节：业务模块说明 / 21. 诗词内容缓存模块 / 功能说明】
### 功能说明

- 缓存外部API获取的诗词内容（译文、注释、赏析等）
- 避免重复调用外部API
- 支持按诗词标题+诗人+内容类型查询

【moyuan_business_modules.md · 业务模块说明 / 21. 诗词内容缓存模块 / API 接口】
【章节：业务模块说明 / 21. 诗词内容缓存模块 / API 接口】
### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/poem-content/cache | 获取缓存的诗词内容 | sc-moyuan-backend PoemContentController |
| POST /api/poem-content/cache | 保存诗词内容缓存 | sc-moyuan-backend PoemContentController |

【moyuan_business_modules.md · 业务模块说明 / 21. 诗词内容缓存模块 / 数据库表】
【章节：业务模块说明 / 21. 诗词内容缓存模块 / 数据库表】
### 数据库表

- `poem_content_cache`：诗词内容缓存表

---

【moyuan_system_workflow.md · 系统工作流文档 / 2. 全局约定与分层（引擎级机制） / 2.4 字段填充与逻辑删除】
【章节：系统工作流文档 / 2. 全局约定与分层（引擎级机制） / 2.4 字段填充与逻辑删除】
### 2.4 字段填充与逻辑删除
- `create_time` / `update_time` 由 MyBatis-Plus 自动填充。
- 逻辑删除使用 `deleted` 字段（`logic-delete-value=1`）。

【moyuan_system_workflow.md · 系统工作流文档 / 2. 全局约定与分层（引擎级机制） / 2.5 缓存策略速览】
【章节：系统工作流文档 / 2. 全局约定与分层（引擎级机制） / 2.5 缓存策略速览】
### 2.5 缓存策略速览
| 场景 | 载体 | key / 范围 |
|------|------|------------|
| 诗词/诗人列表与详情 | Spring Cache（`@Cacheable`） | `poems::…`、`poets::detail:{id}` |
| 推荐结果 | Spring Cache | `poetRecommendation::…`、`poemRecommendation::…` |
| 搜索建议/热门/历史 | Redis | `search:suggestion:{kw}`、`search:hot`(ZSet)、`search:history:{userId}`(List) |
| AI 模型注册表 | 进程内 `ConcurrentHashMap` | `AiModelRegistry.modelCache` |
| 诗词赏析增强内容 | 关系表 `poem_content_cache` | `poem_title+poet_name+content_type` |
| 前端下拉型数据 | localStorage（前缀 `moyuan_`） | `moyuan_cache_*`（详见 14） |

【moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.1 诗词列表查询（C1）】
【章节：系统工作流文档 / 8. 诗词内容域 / 8.1 诗词列表查询（C1）】
### 8.1 诗词列表查询（C1）
- **触发入口**：`GET /api/poems?pageNum&pageSize&dynastyId&poetId&categoryId&keyword`；`GET /api/poems/modern/page`（现代，固定 dynastyId=13）；`GET /api/poems/favorites`。
- **分步流程**：`getPoemList` 构造 `LambdaQueryWrapper`：status=1、朝代/诗人/分类 `eq`、keyword 对标题/内容 `like`（`wrapper.and`）→ `orderByDesc(createTime)` 分页 → `@Cacheable(value="poems", key="'list:'+…")` 缓存；现代分支额外按 `isOriginal/hasCertifiedPoet` 过滤、`sortBy` 排序并 `populatePoetAndDynasty` 批量填充名称。
- **关键类与方法**：`PoemController.getPoemList/getModernPoems/getFavorites`、`PoemServiceImpl.*`、`PoemMapper/DynastyMapper/CategoryMapper.selectBatchIds`。
- **数据与缓存**：`poem`（title/content/poet_id/dynasty_id/category_id/view_count/is_original/status）；缓存 `poems::list:{dynastyId}:{poetId}:{categoryId}:{keyword}:{pageNum}:{pageSize}`。

【moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.2 诗词详情与赏析缓存（C2）】
【章节：系统工作流文档 / 8. 诗词内容域 / 8.2 诗词详情与赏析缓存（C2）】
### 8.2 诗词详情与赏析缓存（C2）
- **触发入口**：`GET /api/poems/{id}`；`GET/POST /api/poem-content/cache`。
- **分步流程**：`getPoemDetail`：`selectById` → 空/status!=1 抛 `POEM_NOT_FOUND` → `setSql("view_count=view_count+1")` 原子自增并内存同步。赏析增强内容由 `PoemContentCacheServiceImpl` 按 `poem_title+poet_name+content_type` 命中 `poem_content_cache`，命中返回、未命中由外部/AI 产出后 `POST` 落库。
- **关键类与方法**：`PoemController.getPoemDetail`、`PoemServiceImpl`、`PoemContentCacheServiceImpl.getCachedContent/saveCachedContent`、`PoemContentController`。
- **数据与缓存**：`poem_content_cache`；`poem.view_count` 原子自增。

【moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.5 外部诗词导入（C5）】
【章节：系统工作流文档 / 8. 诗词内容域 / 8.5 外部诗词导入（C5）】
### 8.5 外部诗词导入（C5）
- **触发入口**：`POST /api/poems/import-external`（title/content/author/dynasty）；`POST /api/poems/fix-external-poems`。
- **分步流程**：校验 title → 按 dynasty 名称查朝代 → 按 author 查诗人（无则新建 Poet 并 `clearAllCache`）→ 按 `title+poetId` 查重：命中补全空缺字段返回 `{id,imported:false}`，未命中落库（`source=external`、`poemType=ancient`、`isOriginal=0`、`auditStatus=1`、status=1）并清 `poems` 缓存返回 `{id,imported:true}`。修复接口取 `source=external` 且 `poet_id is null` 前 100 条，调 `https://cn.apihz.cn/api/zici/poetry.php` 补全作者朝代，成功后清 `poets`/`poems` 缓存返回 `{total,fixed}`。
- **关键类与方法**：`PoemController.importExternalPoem/fixExternalPoems`、`PoemServiceImpl`、`PoetDefaultAvatar.getAvatar`。
- **数据与缓存**：`poem.source/is_original/poem_type/audit_status`；清缓存 key：`poems`、`poets`。

---

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 诗词AI评分（play.rating）】
【章节：墨渊小助手系统知识库（24 条） / 诗词AI评分（play.rating）】
## 诗词AI评分（play.rating）

- 分类：play
- 关键词：AI评分,诗词评分,评分,打分
- 别名：AI打分,诗词打分
- 前端路由：/poem/:id
- 说明：诗词详情页提供 AI 评分，由 AI 从意境、格律、修辞、情感、用词等维度评价诗词并给出分数。进入任意诗词详情页即可查看或请求 AI 评分，用户本人也可以手动给诗词评分。
- 使用步骤：
  1. 打开任意诗词详情页
  2. 查看已有评分或请求 AI 评分
  3. 查看 AI 给出的评价与分数
- 常见问答：
  - 问：AI评分在哪里  答：在诗词详情页可以查看或请求 AI 评分，AI 会多维度评价并给出分数。
```
</details>

## M10（medium）AI 模型配置在后台改完之后怎么生效？API Key 是从哪里读的？

**回答**：AI 模型配置修改后通过后台刷新缓存或重启服务生效；API Key 从 `secrets/application-secrets.yml` 读取。

1. **生效机制**：
   - 应用启动时自动加载配置，由 `AiModelRegistry` 的 `@PostConstruct init→refreshCache` 方法执行全量读取与缓存更新（来源：moyuan_system_workflow.md · 系统工作流文档 / 11. AI 能力域 / 11.4 AI 模型配置加载（E4））。
   - 管理端通过 `/api/admin/ai-models/refresh-cache` 接口手工刷新缓存，或修改 `ai_model` 表数据后触发自动同步。
   - 运行时调用 `getModel/getDefaultModel/getModelByModuleCode/getModelForModule` 方法从内存缓存获取最新配置（来源：moyuan_system_workflow.md · 系统工作流文档 / 11.4 AI 模型配置加载（E4））。

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 7. AI 模块 / 技术方案】
【章节：业务模块说明 / 7. AI 模块 / 技术方案】
### 技术方案

- 调用大语言模型 API（智谱AI、DeepSeek、Kimi、千问、小米MiMo、OpenRouter；NVIDIA NIM 已下线）
- 模块绑定：`ai_module_config`（chat / poet_chat / poetry_chat / write_poem / analyze / couplet，以及游戏模块的 feihua_gen_ancient / feihua_gen_normal / feihua_gen_super / feihua_verify / feihua_topic / chain_generate）→ `ai_model`，由 `AiModelRegistry` 按模块解析模型；统一适配层 `OpenAiCompatibleAdapter` / `NvidiaAdapter`
- 视觉能力：视觉模型用于看图写诗与 OCR 识别；DeepSeek 视觉请求需关闭思考模式（body 顶层 `thinking.type=disabled`）
- OCR 识别复用视觉模型基础设施，通过专用提示词引导模型精准识别古籍文字
- AI模型配置存入数据库（ai_model 表），支持动态管理；api_key 部分为占位符，真实密钥从 `secrets/application-secrets.yml` 运行时注入
- 前端首页集成AI问答组件（右下角悬浮按钮）
- 前端首页"展风拓潮"模块集成看图写诗、智能分析、对联、藏头诗、平仄校验和OCR功能；「飞花令」「诗词接龙」为两张卡片入口（见第 24 章「游戏模块」）

【moyuan_business_modules.md · 业务模块说明 / 7. AI 模块 / API 接口】
【章节：业务模块说明 / 7. AI 模块 / API 接口】
features.ocr-enabled=false` 时返回 `4293`） | sc-moyuan-backend AiController |
| POST /api/ai/tone-check | 平仄格律校验（本地确定性引擎，不消耗 AI 额度） | sc-moyuan-backend AiController |
| POST /api/ai/couplet | AI对对联 | sc-moyuan-backend AiController |
| GET /api/ai/config/{moduleCode} | 获取模块AI配置（提示词模板、回答风格等） | sc-moyuan-backend AiController |
| POST /api/ai/preview | AI生成预览（不写入数据库） | sc-moyuan-backend AiContentController |
| POST /api/ai/submit-review | 提交AI内容到审核表 | sc-moyuan-backend AiContentController |
| POST /api/ai/fill-content | 触发AI填充（直接生成并存库） | sc-moyuan-backend AiContentController |
| GET /api/ai/fill-status/{targetType}/{targetId} | 查询某目标的AI填充状态 | sc-moyuan-backend AiContentController |

> **调用保护**：上述 `/api/ai/**` 接口统一经 `AiGuardService` 按用户限流（默认 60 次/分、1000 次/天、100 万 token/天，admin 豁免），入参长度与 `maxTokens`/`temperature` 由 `AiInputValidator` 统一校验，超限返回 `4290`，限流返回 `4291`，额度用尽返回 `4292`。详见 `docs/api/endpoints.md`。

**获取模块AI配置**：根据 moduleCode 返回对应模块的配置信息，包含提示词模板（promptTemplate）、最大回答长度（maxLength）、回答风格（responseStyle）、首次回答长度（firstResponseLength）等。前端用于构建 AI 对话的系统约束。

【moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.4 诗词评分（C4）】
【章节：系统工作流文档 / 8. 诗词内容域 / 8.4 诗词评分（C4）】
### 8.4 诗词评分（C4）
- **触发入口**：`GET /api/poems/{poemId}/ratings`；`POST .../ratings?score&comment`；`POST .../ratings/ai`、`GET/regenerate`；`GET .../ratings/me`。
- **分步流程**：校验 score∈[1.0,5.0] → `ratePoem`（`@Transactional`）按 `poem_id+user_id+rating_type=1` 增改评分 → `updatePoemAverageScore` 重算 `avg_score/rating_count`；AI 评分 `rating_type=2`（userId=null）：`aiService.analyzePoem(content, null, "rating")` 走 `ai_module_config` 的独立 `rating` 模块绑定（不再写死默认模型，可在后台「AI功能配置」页面调整）→ 正则解析分（失败兜底 3.5）→ 摘要取前 200 字、去 markdown 标记，随后重算均分，`ai_model` 字段记录实际生效模型名。`getPoemRatings` 返回 userRatings(type=1)/aiRating(type=2 最新)/averageScore/ratingCount，异常回退空数据。
- **关键类与方法**：`PoemRatingServiceImpl.ratePoem/requestAiRating/parseAiScore/generateSummary/updatePoemAverageScore`、`AiService.analyzePoem`。`PoemRatingController.*`。
- **数据与缓存**：`poem_rating`（score/comment/rating_type:1用户2AI/ai_model/ai_summary/ai_analysis）、`poem.avg_score/rating_count`。

【moyuan_system_workflow.md · 系统工作流文档 / 11. AI 能力域 / 11.1 AI 辅助创作与生成（E1）】
【章节：系统工作流文档 / 11. AI 能力域 / 11.1 AI 辅助创作与生成（E1）】
provider` 定位 `AiModelAdapter`，`RestTemplate` 带 `Bearer apiKey` POST 到 `ai_model.api_url`（messages=system+user、max_tokens 默认 1024）→ 解析 `choices[0].message.content` 返回；失败抛 `BusinessException`。
- **注意（代码事实）**：providers 下拉含 qwen/openrouter，但当前**无对应 Adapter**，调用报"不支持的AI提供商"；`OpenAiCompatibleAdapter` 支持 zhipu/deepseek/kimi/mimo，`NvidiaAdapter` 支持 nvidia（vision 抛"暂不支持视觉模型"）。
- **关键类与方法**：`AiController.*`、`AiServiceImpl.chat/writePoemFromImage/analyzePoem/matchCouplet/ocrImage`、`AiModelRegistry.chat/vision/chatByModule/getModel`、`OpenAiCompatibleAdapter/NvidiaAdapter`。
- **数据与缓存**：`ai_model`（name/provider/apiUrl/modelId/visionModelId/apiKey/maxTokens/isEnabled/isDefault/modelType）；内存 `AiModelRegistry.modelCache`。

【moyuan_system_workflow.md · 系统工作流文档 / 11. AI 能力域 / 11.4 AI 模型配置加载（E4）】
【章节：系统工作流文档 / 11. AI 能力域 / 11.4 AI 模型配置加载（E4）】
### 11.4 AI 模型配置加载（E4）
- **触发入口**：应用启动自动加载；`POST /api/admin/ai-models/refresh-cache` 手工刷新；`/api/admin/ai-models[/{id}]` CRUD。
- **分步流程**：密钥在 `secrets/application-secrets.yml` 配 `ai.providers..api-key`，由 `AiProviderProperties`（`@ConfigurationProperties("ai.providers")`）承载 → `AiModelRegistry @PostConstruct init→refreshCache`：读 `ai_model` 全量，缓存启用项，对 api_key 空或 `your-` 前缀覆盖 `injectApiKey` → 同时读 `ai_module_config` 入模块缓存 → 运行时 `getModel/getDefaultModel/getModelByModuleCode/getModelForModule`；管理端变更后 `refreshCache()` 保一致；`test` 发送"你好，请回复ok确认连接正常"验证连通性。**适配器分派**：`OpenAiCompatibleAdapter.supports()` 现支持 `zhipu/deepseek/kimi/mimo/qwen/openrouter`（OpenAI 兼容协议），`NvidiaAdapter.supports()` 仅 `nvidia`。
- **关键类与方法**：`AiModelRegistry.refreshCache/injectApiKey/chatByModule`、`AiProviderProperties.getApiKey`、`AiModelConfigService.*`。
- **数据与缓存**：`ai_model`、`ai_module_config`；进程内 `ConcurrentHashMap`（非 Redis）；密钥来源 secrets.yml。

---

【moyuan_system_workflow.md · 系统工作流文档 / 12. 论坛与社区域 / 12.2 内容审核流（E6）】
【章节：系统工作流文档 / 12. 论坛与社区域 / 12.2 内容审核流（E6）】
verified_status=0` | `PUT /api/admin/poet-profiles/{id}/verify` |
  | 诗人草稿 | `poet_draft.status=0` | `PUT /api/admin/poet-drafts/{id}/review` |
  | 投稿建议 | `poet_suggestion.status=pending` | `PUT /api/admin/poet-suggestions/{id}/review` |
- **后台审核列表接口与前端取数约定**：
  - 列表查询：诗作 `GET /api/admin/poems`（参数 `page`、`size`、`keyword`、`auditStatus`、`sort`；`auditStatus` 0-待审核/1-已通过/2-已拒绝，不传返回全部；`sort` 取 asc-按创建时间正序/desc-倒序，默认 desc）；评论 `GET /api/admin/comments`（参数 `page`、`size`、`status`，1-正常/0-隐藏）；AI 内容 `GET /api/admin/ai-contents`（参数 `page`、`size`、`status`，0-待审核/1-已通过/2-已拒绝）。诗人草稿/建议用 `pageNum`、`pageSize`、`status`。
  - 审核变更：`PUT /api/admin/poems/{id}/audit` 的 body `{status, reason}` 中 `status` 对应 `auditStatus` 语义（0/1/2，拒绝为 2）；`PUT /api/admin/comments/{id}/audit` 的 `status` 为 1-正常/0-隐藏。
  - 前端取数：统一响应体为 `{code, message, data}`，Axios 拦截器返回整个响应体，故后台列表页面必须从 `res.data.records` / `res.data.total` 取值（不可写 `res.records`）；诗作审核页状态列与操作判断绑定 `row.auditStatus`，作者/朝代列绑定后端填充的 `poetName` / `dynastyName`（由 `PoemServiceImpl.getAdminPoemList` 调 `populatePoetAndDynasty` 填充），评论页状态列绑定 `row.status`（1-正常/0-隐藏）。
- **关键类与方法**：`AdminController.

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.4 localStorage 使用点汇总（K4）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.4 localStorage 使用点汇总（K4）】
### 15.4 localStorage 使用点汇总（K4）
> **代码事实核对**：收藏、AI 对话、浏览历史**并未真正写入 localStorage**（收藏走服务端、AI 聊天在内存 ref、历史走 `/history` API）。真实落盘 key 如下：
- `token`/`refreshToken`（登录态双 token，user Store 直接写）
- `lastAdminUsername`（admin 记录）
- `home_nav_animation_config`（首页导航动画配置）
- `moyuan_token`（storage 封装的登录 token，带过期结构）
- `moyuan_user_preferences`（theme/language/fontSize/pageSize/sidebarCollapsed）
- `moyuan_search_history`（上限 20 条）
- `moyuan_form_draft_poem/forum/comment`
- `moyuan_sort_preferences`（{poem,poet,forum}）
- `moyuan_cache_dynasties/categories/poets`（筛选下拉，1h）
- `moyuan_cache_external_poem_`（外部诗词详情，24h）
- `moyuan_cache_`（外部搜索结果缓存）
- **收藏/AI对话/浏览历史**：无 localStorage key，分别走服务端接口/内存/API。
- **关键位置**：`utils/storage.ts`、`stores/preferences.ts/searchHistory.ts/formDraft.ts/apiCache.ts`。

【moyuan_system_workflow.md · 系统工作流文档 / 17. 关键代码速查表】
【章节：系统工作流文档 / 17. 关键代码速查表】
4（诗词 AI 评分改为独立 `rating` 模块：`PoemRatingServiceImpl.requestAiRating` 由写死默认模型改为 `aiService.analyzePoem(content, null, "rating")`，`ai_model` 字段记录实际生效模型名；后台新增「AI功能配置」页面（`/admin/ai-config`）可按模块调整模型与提示词；此前 v1.3 补充流式问答入口 `POST /api/ai/chat/stream` 调用链，v1.2 新增首页小游戏区块工作流）
**维护人员**：墨渊开发团队
**维护说明**：本文为 docs 本地维护文件，受 `.git/info/exclude` 排除不提交远程；代码变更后请依据 doc-manager 技能核对本文与实现一致性。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 飞花令（play.feihualing）】
【章节：墨渊小助手系统知识库（24 条） / 飞花令（play.feihualing）】
## 飞花令（play.feihualing）

- 分类：play
- 关键词：飞花令,对诗,令字,行酒令,诗词游戏,怎么玩
- 别名：花令,飞花令游戏
- 前端路由：/game/feihua
- 说明：飞花令是与 AI 对句的诗词游戏，你每回合说出一句含指定「令字」的真实古诗词名句，AI 轮流接句，接不上或超时即判负。入口在首页 AI 小工具区的「飞花令」卡片，登录用户点击后进入对局页。有古制严格、标准版、超级飞花令三种模式与三档难度可选。现代诗不参与本游戏。
- 使用步骤：
  1. 选择模式与难度
  2. 设定令字（如「花」）
  3. 每回合提交一句含令字的古诗词，单回合限时 30 秒
  4. 与 AI 交替接句，AI 接不上或你超时判负
- 常见问答：
  - 问：飞花令怎么玩  答：每回合提交一句含指定令字的真实古诗词名句，与 AI 交替接句，AI 接不上或你超时则对方判负。
  - 问：在哪里能玩飞花令  答：首页 AI 小工具区有「飞花令」卡片，登录用户点击进入对局页。
  - 问：飞花令有几种模式  答：有古制严格、标准版、超级飞花令三种模式，难度分简单、中等、困难三档。
```
</details>

## H1（hard）搜索排序规则里，诗人名精确匹配和标题完全匹配，哪个分数更高？分别是多少？

**回答**：诗人名精确匹配的分数更高，为 0.9；标题完全匹配的分数为 1.0。

根据资料中的《搜索评分算法》章节，具体分值如下：
- **标题完全匹配**：去除标点后完全一致，得分为 **1.0**。
- **诗人名精确匹配**：诗人名与关键词完全一致，得分为 **0.9**。

虽然标题完全匹配的绝对分值（1.0）高于诗人名精确匹配（0.9），但两者均为最高档评分中的次高项。若需区分优先级，标题完全匹配在数值上略高；若考虑实际业务场景，诗人名精确匹配通常用于精准定位作者作品，权重极高。

（来源：moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 搜索评分算法）

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 功能说明】
【章节：业务模块说明 / 8. 搜索模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 复合搜索策略】
【章节：业务模块说明 / 8. 搜索模块 / 复合搜索策略】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 搜索评分算法】
【章节：业务模块说明 / 8. 搜索模块 / 搜索评分算法】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 搜索流程】
【章节：业务模块说明 / 8. 搜索模块 / 搜索流程】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 智能搜索参数】
【章节：业务模块说明 / 8. 搜索模块 / 智能搜索参数】
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

【moyuan_business_modules.md · 业务模块说明 / 8. 搜索模块 / 智能搜索响应】
【章节：业务模块说明 / 8. 搜索模块 / 智能搜索响应】
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

【moyuan_system_workflow.md · 系统工作流文档 / 9. 诗人内容域 / 9.1 诗人详情与懒加载 / 头像分配（已收录，完整见下文）】
【章节：系统工作流文档 / 9. 诗人内容域 / 9.1 诗人详情与懒加载 / 头像分配（已收录，完整见下文）】
### 9.1 诗人详情与懒加载 / 头像分配（已收录，完整见下文）
- **触发入口**：`GET /api/poets/{id}`。
- **分步流程**：`getPoetDetail`（`@Cacheable("poets","detail:"+id)`）查库并补 `dynastyName` → `isPoetInfoIncomplete` 判 biography/lifeStory/influence/anecdotes/avatar 是否为空 → 不完整则 `fetchAndSavePoetData` 调 `https://cn.apihz.cn/api/zici/poet.php`（id/key/name），按字段逐项补全（pseudonym=tag、biography=content 经 `PoetDataExtractor` 去标签并抽生卒/籍贯、按生卒年定朝代、头像、lifeStory=rwsp、influence=zycj、anecdotes=ysdg）→ 有改动则 `updateById` 并 `evict` 详情缓存。
- **头像来源优先级**：①外部 API `image` 字段（网络图）→ ②`PoetDefaultAvatar.getAvatar()` 本地 `/img/poet_avatars/*.jpg` 兜底。
- **预设头像分配规则（PoetDefaultAvatar）**：第 0 级按 `poet.style` 精准流派匹配（`STYLE_MAP`：豪放派/婉约派/田园派/现实主义/新月派/浪漫主义/古典主义，命中即用，`style` 为空则跳过）→ 第 1~3 级按 性别（已知女诗人名单）→ 地区（中西名单/无朝代）→ 时期（`poetType=="modern"`），组出 `地区_时期_性别` 键匹配头像池；候选则随机，否则「地区+性别」随机 → 同地区随机 → 全局随机。

【moyuan_system_workflow.md · 系统工作流文档 / 10. 搜索与运营域 / 10.1 全文/跨模块智能搜索（C8）】
【章节：系统工作流文档 / 10. 搜索与运营域 / 10.1 全文/跨模块智能搜索（C8）】
### 10.1 全文/跨模块智能搜索（C8）
- **触发入口**：`GET /api/search/smart?dynastyId&categoryId&poetId&poetName&keyword&sortBy&pageNum&pageSize`；`GET /api/search?keyword=`（三段返回 poems/poets/posts）；`GET /api/search/poems`；`GET /api/search/suggestions|hot|history`、`DELETE /api/search/history`。
- **分步流程**（`SmartSearchServiceImpl.smartSearch`）：
  1. 有 keyword 且已登录 → `saveSearchHistory`。
  2. `resolvePoetNameIds`：poetName 对 poet.name 模糊匹配取 ID 集。
  3. 无 keyword → `searchWithoutKeyword` 按筛选条件分页 + 排序（最新/热门/点赞）。
  4. 有 keyword 三路并发，命中结果统一按 poem.id 去重取高分、内存分页，为空置 `suggestExternal=true`：
     - `searchPoems`（名称匹配，**双模式**）：
       - 关闭全文（`search.fulltext-enabled=false`）：对 title/content 用原始词 + 去标点拆分词 `like` 模糊匹配。
       - **默认开启**（`search.fulltext-enabled=true`，见 `application.yml`）：改用 ngram 中文全文索引 `MATCH(title, content) AGAINST(... IN NATURAL LANGUAGE MODE)`（`poem` 表 `ft_poem_title_content` 已 `WITH PARSER ngram`，中文分词 + 相关度更高，见 `docs/database/migration_004_poem_search_fulltext.sql`）。
       - 两模式命中集统一经 `scoreAndRank` 按 `calculateScore` 打分：标题精确 1.0/包含 0.8/拆分 0.3、内容包含 0.5/拆分 0.2。
     - `searchByPoetName`：匹配诗人名取作品，精确 0.9/包含 0.7/部分 0.3。
     - `searchByPinyin`：`PinyinUtil.matchesPinyin` 拼音联想，基础 0.3+标题 0.2+诗人 0.1。
  5.

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 智能检索（feature.search）】
【章节：墨渊小助手系统知识库（24 条） / 智能检索（feature.search）】
## 智能检索（feature.search）

- 分类：feature
- 关键词：搜索,检索,找诗,搜诗人,关键字
- 别名：智能搜索,全文搜索
- 前端路由：/poem
- 说明：智能检索支持按关键词搜索诗词、诗人与帖子，首页与诗词列表页内嵌了搜索入口。搜索会记录你的历史，登录后还会根据兴趣给出推荐。
- 使用步骤：
  1. 在首页或诗词列表页输入关键词
  2. 查看匹配的诗词、诗人或帖子
  3. 点开结果查看详情
- 常见问答：
  - 问：怎么搜索  答：在首页或诗词列表页的搜索框输入关键词，即可搜索诗词、诗人与帖子。
```
</details>

## H2（hard）给一首诗点赞之后，诗作记录上的点赞数和收藏数会同步变化吗？

**回答**：不会同步变化。点赞仅影响“赞”计数，不影响“收藏”计数；且系统明确说明收藏不维护作品端的收藏总数字段。

- **点赞逻辑**：调用 `POST|GET /api/poems/{id}/like` 时，执行 `toggleLike` 操作，更新 `user_like` 表并修改 `poem.like_count`（存在则减 1，不存在则加 1），但**不触碰**收藏相关字段。
- **收藏逻辑**：调用 `POST|GET /api/poems/{id}/favorite` 时，执行 `toggleFavorite` 操作，仅写入 `user_favorite` 表，**不维护** `poem.favorite_count`（作品端的收藏总数）。
- **数据隔离**：根据代码事实，点赞和收藏是两条独立的数据流，互不影响。用户给同一首诗点赞或收藏，各自对应的计数字段会更新，但“赞数”不会随“收藏”变动，“收藏数”也不会随“点赞”变动。

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 2. 诗词模块 / 功能说明】
【章节：业务模块说明 / 2. 诗词模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 3. 诗人模块 / 功能说明】
【章节：业务模块说明 / 3. 诗人模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 9. 用户模块 / 功能说明】
【章节：业务模块说明 / 9. 用户模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 9. 用户模块 / API 接口】
【章节：业务模块说明 / 9. 用户模块 / API 接口】
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

【moyuan_business_modules.md · 业务模块说明 / 9. 用户模块 / 用户主页功能】
【章节：业务模块说明 / 9. 用户模块 / 用户主页功能】
### 用户主页功能

用户主页展示以下信息：
- **用户基本信息**：头像、昵称、简介、性别、生日
- **诗人认证状态**：是否已认证、认证诗人资料
- **统计数据**：作品数、获赞数、被收藏数、粉丝数
- **作品列表**：用户发布的诗词和帖子

【moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.3 收藏 / 点赞（C3）】
【章节：系统工作流文档 / 8. 诗词内容域 / 8.3 收藏 / 点赞（C3）】
### 8.3 收藏 / 点赞（C3）
- **触发入口**：`POST|GET /api/poems/{id}/like`、`POST|GET /api/poems/{id}/favorite`、`GET /api/poems/favorites`。
- **分步流程**：点赞 `toggleLike`（`@Transactional`）按 `user_id+target_id+target_type=POEM` 计数 `user_like`：存在则删记录且 `like_count-1`（`gt 0` 保护），否则插入并 `+1`；状态查询 `isLike/isFavorite` 计数判定；`getFavorites` 取收藏 ID 集后 `in` 查 poem 并填充。
- **注意（代码事实）**：收藏 `UserServiceImpl.toggleFavorite` **只写 `user_favorite` 表，不维护 `poem.favorite_count`**；点赞会维护 `poem.like_count`。
- **数据与缓存**：`user_like`、`user_favorite`、`poem.like_count`。

【moyuan_system_workflow.md · 系统工作流文档 / 9. 诗人内容域 / 9.2 推荐 / 热门诗人（C7）】
【章节：系统工作流文档 / 9. 诗人内容域 / 9.2 推荐 / 热门诗人（C7）】
### 9.2 推荐 / 热门诗人（C7）
- **触发入口**：`GET /api/poets/recommend?limit=6`（上限 20）、`GET /api/poets/popular?limit=10`（上限 50）；诗词侧 `GET /api/search/poems/recommended|popular`。
- **分步流程**：取当前用户（未登录走热门）→ `userId==null` 走 `getPopularPoets`；`buildUserPoetMatrix` 由收藏/点赞/浏览行为构建 用户→诗人:{权重}（收藏 5、点赞 3、浏览 1）→ `calculateItemCollaborativeScores` 计算物品-物品余弦相似度，每个已交互诗人取 Top50 相似诗人、对未交互诗人累加 `相似度×权重`（诗词侧 `buildUserPoemMatrix` 同一作品多行为取最大值） → `getInterestBasedPoetScores` 按 `interests` 朝代加权（古典/现代/外国）→ `mergeScores` 相加取 TopN → 过滤 status=1；空回退热门。热门 `calculatePoetPopularity`（诗收藏5+词收藏5+赞3+浏览1）取 TopN。结果 `@Cacheable` 缓存。
- **关键类与方法**：`PoetController.getRecommendedPoets/getPopularPoets`、`RecommendationServiceImpl.*`、`SearchController.getRecommendedPoems/getPopularPoems`。
- **数据与缓存**：`user_favorite/user_like/user_history`、`user.interests`、`poem.view_count/like_count`；缓存 `poetRecommendation::user:{userId}:{limit}`、`popular:{limit}` 等。

【moyuan_system_workflow.md · 系统工作流文档 / 9. 诗人内容域 / 9.3 诗人认证、草稿与投稿建议（E10，合并）】
【章节：系统工作流文档 / 9. 诗人内容域 / 9.3 诗人认证、草稿与投稿建议（E10，合并）】
### 9.3 诗人认证、草稿与投稿建议（E10，合并）
- **精选诗人展示/管理触发**：`GET /api/poet-featured/random?count`、`/list`、`/{id}`；后台 CRUD。`getRandomPoetFeatured` 仅取 `status=1 且 is_show=1` 的启用项后 `Collections.shuffle` 截断；后台列表支持 keyword 按姓名/朝代筛选，并提供 `PUT /api/admin/poet-featured/{id}/show`（单个）与 `PUT /api/admin/poet-featured/batch-show`（批量）切换首页展示。
- **认证申请/审核**：`POST /api/poet-profile/apply`（落 `poet_profile`，`verifiedStatus=2` 待审，同步 `user.poet_verified=2`、`user.poet_profile_id`；他人查看屏蔽 contactInfo）；`PUT /api/admin/poet-profiles/{id}/verify` 置通过/拒绝并回写 `user.poet_verified`，记录作品/获赞数。
- **草稿投稿**：`POST /api/poet-drafts`（校验诗人存在，`section` 映射字段并快照原值，status=0 待审）；后台 `PUT /api/admin/poet-drafts/{id}/review` 通过时 `publishDraft` 按 section 把 content 写回 `poet` 对应字段。
- **投稿建议**：`POST /api/poet-suggestions`（前端 `sanitizeInput` 清洗 HTML，status=pending）；后台 `PUT /api/admin/poet-suggestions/{id}/review`。
- **待审统计**：`countByStatus` 汇总到 `GET /api/admin/audit/stats`。
- **关键类与方法**：`PoetFeaturedServiceImpl`、`PoetProfileServiceImpl.applyVerification/verifyProfile/getByUserId`、`PoetDraftServiceImpl.saveDraft/reviewDraft/publishDraft`、`PoetSuggestionServiceImpl.submitSuggestion/reviewSuggestion`。
- **数据与缓存**：`poet_featured`、`poet_profile`、`user`（poet_verified/poet_profile_id）、`poet_draft`、`poet_suggestion`。

---

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 评论与互动（feature.comment）】
【章节：墨渊小助手系统知识库（24 条） / 评论与互动（feature.comment）】
## 评论与互动（feature.comment）

- 分类：feature
- 关键词：评论,点赞,互动,回复
- 别名：评论,互动
- 前端路由：/forum
- 说明：在诗词、帖子与文章详情页可以对内容点赞与评论，论坛评论支持多级回复。评论需要有账号登录。
- 使用步骤：
  1. 打开诗词、帖子或文章详情
  2. 点击点赞或发表评论
  3. 在论坛可对评论进行回复
- 常见问答：
  - 问：怎么评论  答：在诗词、帖子或文章详情页可以直接发表评论与点赞，论坛评论支持多级回复。
```
</details>

## H3（hard）诗宝阁和首页 AI 小工具区是什么关系？藏头诗这两处都有吗？

**回答**：诗宝阁与首页 AI 小工具区是并存且互补的关系，两者功能定位不同但入口共存。

1. **关系说明**：诗宝阁是统一的 AI 对话页面，提供角色化（如诗仙、格律师等）与能力组合的深层交互；首页 AI 小工具区则是快捷入口，保留看图写诗、藏头诗、飞花令等独立玩法。两者并存，互不替代。
2. **藏头诗分布**：藏头诗功能在两处均有提供。
   - 在**首页 AI 小工具区**：用户可直接点击展开内嵌窗口（`GameMiniPanel`）进行开局。
   - 在**诗宝阁「诗仙·创作」角色**下：作为其默认能力之一，用户可选择五言或七言生成藏头诗，结果会高亮首字。
3. **技术实现差异**：首页工具区复用 `POST /api/ai/chat` 接口（moduleCode=chat）；诗宝阁同样复用该接口，但支持更复杂的角色与能力配置及会话记忆功能

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 1. 首页模块 / 功能说明】
【章节：业务模块说明 / 1. 首页模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 7. AI 模块 / 功能说明】
【章节：业务模块说明 / 7. AI 模块 / 功能说明】
### 功能说明

- AI诗词问答（支持多模型切换；调用优先走 `ai_module_config` 模块绑定，未绑定某模块时回退默认模型。可用提供商：智谱AI、DeepSeek、Kimi、千问、小米MiMo、OpenRouter；NVIDIA NIM 已全部禁用/下线）
- 看图写诗（上传图片，AI生成古诗词，支持视觉模型如 GLM-4V-Flash、DeepSeek-Flash）
- 智能分析（输入诗句进行深度解析）
- AI对对联（输入上联，AI生成下联，遵循平仄规则）
- **藏头诗**（输入 2-8 个藏头字并选择五言/七言，AI 生成藏头诗，每句首字依次对应藏头字；复用 `POST /api/ai/chat`，moduleCode=chat，不新增后端接口）
- **平仄格律校验**（粘贴一首绝句/律诗，由**本地确定性引擎** `POST /api/ai/tone-check` 逐句标注平仄（平声绿底、仄声红底）、判定是否合律并给出不合律原因；校验规则为句末三平尾/三仄尾、二四六位失替、偶数句押韵，不调用大模型、不消耗 AI 额度）
- **AI 对话页「诗宝阁」**（`/ai-chat`）：角色化统一对话入口，6 角色（诗仙·创作 / 诗话·鉴赏 / 格律·校正 / 诗友·行令 / 群贤·论诗 / 顾问·问答）× 12 能力（命题作诗 / 集句成诗 / 藏头诗 / 润色入律 / 平仄校验 / 韵脚查询 / 对对联 / 意象典故 / 诗话点评 / 飞花令 / 诗词接龙 /

【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 前端路由】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 前端路由】
### 前端路由

| 路径 | 说明 |
|------|------|
| `/game/feihua` | 飞花令对局页 |
| `/game/chain` | 诗词接龙对局页 |

> 首页 AI 小工具区提供「飞花令」「诗词接龙」两张卡片：游客点击展开内嵌小窗口（`GameMiniPanel`，直接在首页开局），登录用户跳转到 `/game/feihua`、`/game/chain` 游戏页。

【moyuan_business_modules.md · 业务模块说明 / 25. AI 对话页「诗宝阁」模块 / 功能说明】
【章节：业务模块说明 / 25. AI 对话页「诗宝阁」模块 / 功能说明】
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
- **会话记忆**：按 `roleId` 存服务端 Redis（键 `ai:chat:session:{userId}:{roleId}`，TTL 7 天、最多 40 条），切换角色各自恢复，换设备登录同一账号可继续；本地 `localStorage`（键 `moyuan_ai_chat_${roleId}`）作为降级缓存，Redis 不可用时仍可用。单条内容上限 4000 字
- **空态引导**：进入页面 / 新建会话后，按角色给出引导语 + 推荐能力 chip
- **结果渲染**：普通能力渲染为文本气泡，流式逐字追加并自动滚底；藏头诗高亮每句首字（藏头字）；平仄校验改调本地确定性引擎 `POST /api/ai/tone-check`，把返回的逐字平仄与问题列表渲染为平仄标签卡（平底/仄底，三平尾、三仄尾、二四六失替等问题标红），不再依赖大模型输出 JSON
- **消息头像**：对话流每条消息两侧带圆形头像——AI 侧统一使用墨渊可爱卡通头像（本地静态资源 `/img/ai_chat_avatar.jpg`），用户侧使用本人头像（未登录 / 无头像时回退金色首字母）；用户气泡靠右、头像居其右，AI 气泡靠左、头像居其左，加载态气泡同样带 AI 头像

【moyuan_business_modules.md · 业务模块说明 / 25. AI 对话页「诗宝阁」模块 / 前端组件】
【章节：业务模块说明 / 25. AI 对话页「诗宝阁」模块 / 前端组件】
### 前端组件

| 组件 | 说明 |
|------|------|
| views/ai-chat/index.vue | 页面壳：组合下辖组件、持有会话状态、处理 `?role=&skill=` query |
| components/ai-chat/RoleSidebar.vue | 角色栏 |
| components/ai-chat/ChatStream.vue | 对话流（气泡 + 消息头像 + 空态引导 + 流式跟随滚动 + 平仄/藏头特殊渲染） |
| components/ai-chat/SkillChips.vue | 能力快捷 chips |
| components/ai-chat/ChatInput.vue | 输入框 + 发送按钮（Enter 发送 / Shift+Enter 换行；生成中切换为「停止」） |
| configs/ai-chat/personas.ts | 角色常量（含 systemPrompt） |
| configs/ai-chat/skills.ts | 能力常量（含任务模板与发送前校验） |
| composables/useAiChat.ts | 会话状态、SSE 流式请求、多轮上下文组装、服务端会话同步 |
| api/modules/ai.ts | `chatStream`（`fetch` + `ReadableStream` 解析 SSE、401 静默刷新后重放一次）与会话接口封装 |

【moyuan_business_modules.md · 业务模块说明 / 25. AI 对话页「诗宝阁」模块 / 前端路由】
【章节：业务模块说明 / 25. AI 对话页「诗宝阁」模块 / 前端路由】
### 前端路由

| 路径 | 说明 |
|------|------|
| `/ai-chat` | 诗宝阁 AI 对话页面（支持 `?role=xxx&skill=xxx` 预选角色与能力） |

> 首页顶部导航栏已新增「诗宝阁」入口（与「诗云」「交流广场」同级）；首页原「展风拓潮」AI 区的藏头诗 / 平仄 / 飞花令入口**保持不变，两处并存**。

【moyuan_business_modules.md · 业务模块说明 / 25. AI 对话页「诗宝阁」模块 / API 接口】
【章节：业务模块说明 / 25. AI 对话页「诗宝阁」模块 / API 接口】
### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| POST /api/ai/chat/stream | 主链路。SSE 流式对话（请求 `{ moduleCode, messages[], temperature, maxTokens }`，响应 `text/event-stream`，事件 `{"delta":"…"}` … `[DONE]`；限流/入参错误以 JSON 返回） | sc-moyuan-backend AiController |
| POST /api/ai/chat | 兜底。非流式对话（请求 `{ message, moduleCode:'chat' }`，响应 `data.reply`），仅在流式遇可重试失败时调用 | sc-moyuan-backend AiController |
| GET /api/ai/chat/sessions/{roleId} | 读取当前用户在该角色下的会话 | AiChatSessionController |
| PUT /api/ai/chat/sessions/{roleId} | 保存会话（`{"messages":[…]}`，服务端只保留最近 40 条） | AiChatSessionController |
| DELETE /api/ai/chat/sessions/{roleId} | 清空该角色会话 | AiChatSessionController |
| POST /api/ai/tone-check | 平仄校验（本地确定性引擎，不消耗 AI 额度） | sc-moyuan-backend AiController |

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 诗宝阁AI对话（play.bao-ge）】
【章节：墨渊小助手系统知识库（24 条） / 诗宝阁AI对话（play.bao-ge）】
## 诗宝阁AI对话（play.bao-ge）

- 分类：play
- 关键词：诗宝阁,AI对话,创作,鉴赏,平仄校验,藏头诗
- 别名：诗阁,诗宝阁
- 前端路由：/ai-chat
- 说明：诗宝阁是统一的 AI 对话页面，AI 以不同角色与能力帮你完成命题作诗、集句成诗、藏头诗、润色入律、平仄校验、意象典故、诗话点评、飞花令、诗词接龙等任务。可在左侧选择角色与能力后直接对话，回复以流式方式逐字输出，生成过程中可随时点击「停止」。会话按角色保存在账号下并保留 7 天，换设备登录同一账号可以接着聊。入口在顶部导航的「诗宝阁」。
- 使用步骤：
  1. 打开诗宝阁页面
  2. 选择角色与能力
  3. 输入你的需求并发送
  4. 查看 AI 返回的文本、藏头高亮或平仄标注结果
- 常见问答：
  - 问：诗宝阁是什么  答：诗宝阁是 AI 对话页面，AI 分角色和能力，可帮你作诗、鉴赏、校验平仄、玩飞花令等。
  - 问：诗宝阁和首页的AI工具有什么区别  答：诗宝阁是统一的角色化对话入口，首页 AI 小工具区保留看图写诗、藏头诗、飞花令等快捷入口，两处并存。
  - 问：诗宝阁的对话会保存吗  答：会，按角色保存在你的账号下并保留 7 天，换设备登录同一账号也能接着聊。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 藏头诗（play.acrostic）】
【章节：墨渊小助手系统知识库（24 条） / 藏头诗（play.acrostic）】
## 藏头诗（play.acrostic）

- 分类：play
- 关键词：藏头诗,藏头,写诗,五言,七言
- 别名：藏头
- 前端路由：/
- 说明：藏头诗功能可输入 2 至 8 个藏头字并选择五言或七言，由 AI 生成每句首字依次对应藏头字的诗作。入口在首页 AI 小工具区，诗宝阁「诗仙·创作」角色下也提供藏头诗能力。生成结果会高亮每句首字。
- 使用步骤：
  1. 打开首页 AI 小工具区或诗宝阁
  2. 输入 2 至 8 个藏头字
  3. 选择五言或七言
  4. 点击生成，查看首字高亮的藏头诗
- 常见问答：
  - 问：藏头诗怎么用  答：输入 2 到 8 个藏头字并选择五言或七言，AI 会生成每句首字依次对应藏头字的诗。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 首页（page.home）】
【章节：墨渊小助手系统知识库（24 条） / 首页（page.home）】
## 首页（page.home）

- 分类：page
- 关键词：首页,主页,导航,入口
- 别名：主页
- 前端路由：/
- 说明：首页是站点的总入口，聚合了今日诗词、古诗精赏、当代精选、诗话视野、精选诗人等板块，并提供通往诗云、交流广场、诗宝阁的导航。首页还设有 AI 小工具区，可快捷使用看图写诗、藏头诗、飞花令等玩法。
- 使用步骤：
  1. 打开站点默认进入首页
  2. 通过顶部导航切换各板块
  3. 在 AI 小工具区使用各类玩法
- 常见问答：
  - 问：首页有哪些内容  答：首页聚合了今日诗词、古诗精赏、当代精选、诗话视野、精选诗人等板块，还有 AI 小工具区。
```
</details>

## H4（hard）游客在首页直接玩飞花令，对局记录会保存到哪里？

**回答**：游客在首页直接玩飞花令，对局记录仅临时保存在 Redis 中，不会写入 MySQL 数据库。

具体结论如下：
1. **存储位置**：游客对局的记录仅存于 Redis（键为 `game:{gameId}` 与 `game:{gameId}:sentences`），不落 MySQL。
2. **保存机制**：后端按登录态分层处理，游客模式下的对局数据不持久化到 `game_record` 或 `game_sentence` 表。
3. **查询限制**：由于未写入数据库，游客对局无法进行历史回查，仅在当前会话期间（TTL 30 分钟）通过 Redis 维持状态。
4. **对比差异**：登录用户的对局才会将有效诗句写入 `game_record` / `game_sentence` 表并支持回查。

（来源：moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙）/ 功能说明）

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 功能说明】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 功能说明】
- **三级真实性校验链**：① 本地 `poem` 表 ngram 全文索引 `MATCH...AGAINST`（限定范围时附加 `dynasty_id` 过滤）→ ② 外部 APIHZ 复核（仅在**不限朝代**时可用，外部源无法确认朝代归属）→ ③ LLM 兜底真伪判断（`feihua_verify`，限定范围时提示词附朝代约束），全链路失败即判为「诗句真实性校验不通过」。
- **游客模式**：游客与新页面共用同一套接口，后端按登录态分层——**游客对局仅存 Redis（不落 MySQL），登录用户对局与有效诗句写入 `game_record` / `game_sentence`**；前端以 `GuestBadge` 展示游客标识，登录态用户对局可回查。
- **状态/判重后端权威**：对局运行时状态存 Redis（键 `game:{gameId}` 与 `game:{gameId}:sentences`，TTL 30 分钟，交互续期）；**超时判定由后端依前端上报的 `activeSeconds` 裁定**（见上文「超时判定」）。
- **输入方式**：支持文本与语音（`inputType`），语音仅标识，校验逻辑一致。

【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / API 接口】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / API 接口】
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

【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 前端组件】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 前端组件】
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

【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 前端路由】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 前端路由】
### 前端路由

| 路径 | 说明 |
|------|------|
| `/game/feihua` | 飞花令对局页 |
| `/game/chain` | 诗词接龙对局页 |

> 首页 AI 小工具区提供「飞花令」「诗词接龙」两张卡片：游客点击展开内嵌小窗口（`GameMiniPanel`，直接在首页开局），登录用户跳转到 `/game/feihua`、`/game/chain` 游戏页。

【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 数据库表】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 数据库表】
### 数据库表

- `game_record`：游戏对局主表（飞花令/诗词接龙共用，`game_type` 区分）
- `game_sentence`：对局有效诗句明细表（`uk_game_sentence` 唯一键兜底判重）

---

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.5 首页数据组装（K5）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.5 首页数据组装（K5）】
### 15.5 首页数据组装（K5）
- **轮播图**：静态 `/img/lb_*.jpg`、`/img/lb_shiwen_*.png`，无后端。
- **今日诗词**：`DailyPoetry` → 后端 `GET /api/external-poetry/jinrishici` 代理（`v2.jinrishici.com/one.json`，后端 60 分钟进程内缓存，前端不再直连）。
- **首页导航**：`GET /home-navigation`。
- **精选诗人**：`getRandomPoetFeatured(8)`，去重取 4 位，每 20s 轮换。
- **论坛预览**：`GET /forum/posts?pageSize=5`。
- **古诗精赏**：本地 json + `GET /search/poems` + `/search/poems/recommended`。
- **当代精选**：本地 json + like/favorite/comment 接口。
- **AI 区块**：`/ai/write-poem`、`/ai/analyze`、`/ai/couplet`、`/ai/ocr`、`/ai/chat`（藏头诗、平仄格律校验，复用 `POST /api/ai/chat` 并传 `moduleCode=chat`，不新增后端接口）、`/rhyme/*`；"诗海拾贝"本地 json。
- **小游戏区块**：首页 AI 小工具区「飞花令」「诗词接龙」两张卡片——游客点击展开内嵌小窗口（`GameMiniPanel`）直接开局，登录用户跳转 `/game/feihua`、`/game/chain`；后端为独立游戏模块（`/api/game/feihua`、`/api/game/chain`，见 `modules.md` 第 24 章）。
- **诗话视野**：`GET /vision/articles`。
- **诗词卡片跳转**：首页「古诗精赏 / 当代精选」卡片点击经组合式函数 `composables/usePoemCardNavigation.ts` 处理——本地命中直接 `router.push('/poem/{id}')`，未命中回退 `PoetryDetailDialog`（外部诗词弹窗，可由接口盒子实时增强）。
- **关键位置**：`views/home/index.vue`、`components/{home,business}/*`、`composables/usePoemCardNavigation.ts`、`api/modules/*`。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 飞花令（play.feihualing）】
【章节：墨渊小助手系统知识库（24 条） / 飞花令（play.feihualing）】
## 飞花令（play.feihualing）

- 分类：play
- 关键词：飞花令,对诗,令字,行酒令,诗词游戏,怎么玩
- 别名：花令,飞花令游戏
- 前端路由：/game/feihua
- 说明：飞花令是与 AI 对句的诗词游戏，你每回合说出一句含指定「令字」的真实古诗词名句，AI 轮流接句，接不上或超时即判负。入口在首页 AI 小工具区的「飞花令」卡片，登录用户点击后进入对局页。有古制严格、标准版、超级飞花令三种模式与三档难度可选。现代诗不参与本游戏。
- 使用步骤：
  1. 选择模式与难度
  2. 设定令字（如「花」）
  3. 每回合提交一句含令字的古诗词，单回合限时 30 秒
  4. 与 AI 交替接句，AI 接不上或你超时判负
- 常见问答：
  - 问：飞花令怎么玩  答：每回合提交一句含指定令字的真实古诗词名句，与 AI 交替接句，AI 接不上或你超时则对方判负。
  - 问：在哪里能玩飞花令  答：首页 AI 小工具区有「飞花令」卡片，登录用户点击进入对局页。
  - 问：飞花令有几种模式  答：有古制严格、标准版、超级飞花令三种模式，难度分简单、中等、困难三档。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 诗宝阁AI对话（play.bao-ge）】
【章节：墨渊小助手系统知识库（24 条） / 诗宝阁AI对话（play.bao-ge）】
## 诗宝阁AI对话（play.bao-ge）

- 分类：play
- 关键词：诗宝阁,AI对话,创作,鉴赏,平仄校验,藏头诗
- 别名：诗阁,诗宝阁
- 前端路由：/ai-chat
- 说明：诗宝阁是统一的 AI 对话页面，AI 以不同角色与能力帮你完成命题作诗、集句成诗、藏头诗、润色入律、平仄校验、意象典故、诗话点评、飞花令、诗词接龙等任务。可在左侧选择角色与能力后直接对话，回复以流式方式逐字输出，生成过程中可随时点击「停止」。会话按角色保存在账号下并保留 7 天，换设备登录同一账号可以接着聊。入口在顶部导航的「诗宝阁」。
- 使用步骤：
  1. 打开诗宝阁页面
  2. 选择角色与能力
  3. 输入你的需求并发送
  4. 查看 AI 返回的文本、藏头高亮或平仄标注结果
- 常见问答：
  - 问：诗宝阁是什么  答：诗宝阁是 AI 对话页面，AI 分角色和能力，可帮你作诗、鉴赏、校验平仄、玩飞花令等。
  - 问：诗宝阁和首页的AI工具有什么区别  答：诗宝阁是统一的角色化对话入口，首页 AI 小工具区保留看图写诗、藏头诗、飞花令等快捷入口，两处并存。
  - 问：诗宝阁的对话会保存吗  答：会，按角色保存在你的账号下并保留 7 天，换设备登录同一账号也能接着聊。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 游客与登录用户的权限差异（account.guest-limit）】
【章节：墨渊小助手系统知识库（24 条） / 游客与登录用户的权限差异（account.guest-limit）】
## 游客与登录用户的权限差异（account.guest-limit）

- 分类：account
- 关键词：游客,未登录,权限,差异,登录
- 别名：游客权限
- 说明：未登录的游客可以浏览诗词、诗人、帖子与文章，也能在首页直接体验飞花令、诗词接龙等小工具（对局仅临时保存）。发布诗词、发帖、评论、点赞、收藏、报修、诗人认证等操作需要登录后才能使用。
- 使用步骤：
  1. 未登录时浏览诗词等公开内容
  2. 登录后解锁发布、评论、收藏等操作
- 常见问答：
  - 问：不登录能用吗  答：不登录可以浏览诗词、诗人、帖子与文章，也能在首页体验飞花令等小工具；发布、评论、收藏等需要登录。
```
</details>

## H5（hard）藏头诗最多能藏几个字？除了首页还能从哪里进入？

**回答**：藏头诗最多能藏 8 个字。除首页外，还可从诗宝阁的「诗仙·创作」角色下进入。

1. **字数限制**：输入 2 至 8 个藏头字（来源：moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条）/ 藏头诗（play.acrostic））。
2. **入口位置**：
   - 首页 AI 小工具区（来源：moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条）/ 藏头诗（play.acrostic）；moyuan_business_modules.md · 业务模块说明 / 1. 首页模块 / 功能说明）。
   - 诗宝阁「诗仙·创作」角色下（来源：moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条）/ 藏头诗（play.acrostic）；moyuan_business_modules.md · 业务模块说明 / 7. AI 模块 / 功能说明）。

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 1. 首页模块 / 功能说明】
【章节：业务模块说明 / 1. 首页模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 7. AI 模块 / 功能说明】
【章节：业务模块说明 / 7. AI 模块 / 功能说明】
### 功能说明

- AI诗词问答（支持多模型切换；调用优先走 `ai_module_config` 模块绑定，未绑定某模块时回退默认模型。可用提供商：智谱AI、DeepSeek、Kimi、千问、小米MiMo、OpenRouter；NVIDIA NIM 已全部禁用/下线）
- 看图写诗（上传图片，AI生成古诗词，支持视觉模型如 GLM-4V-Flash、DeepSeek-Flash）
- 智能分析（输入诗句进行深度解析）
- AI对对联（输入上联，AI生成下联，遵循平仄规则）
- **藏头诗**（输入 2-8 个藏头字并选择五言/七言，AI 生成藏头诗，每句首字依次对应藏头字；复用 `POST /api/ai/chat`，moduleCode=chat，不新增后端接口）
- **平仄格律校验**（粘贴一首绝句/律诗，由**本地确定性引擎** `POST /api/ai/tone-check` 逐句标注平仄（平声绿底、仄声红底）、判定是否合律并给出不合律原因；校验规则为句末三平尾/三仄尾、二四六位失替、偶数句押韵，不调用大模型、不消耗 AI 额度）
- **AI 对话页「诗宝阁」**（`/ai-chat`）：角色化统一对话入口，6 角色（诗仙·创作 / 诗话·鉴赏 / 格律·校正 / 诗友·行令 / 群贤·论诗 / 顾问·问答）× 12 能力（命题作诗 / 集句成诗 / 藏头诗 / 润色入律 / 平仄校验 / 韵脚查询 / 对对联 / 意象典故 / 诗话点评 / 飞花令 / 诗词接龙 /

【moyuan_business_modules.md · 业务模块说明 / 25. AI 对话页「诗宝阁」模块 / 前端组件】
【章节：业务模块说明 / 25. AI 对话页「诗宝阁」模块 / 前端组件】
### 前端组件

| 组件 | 说明 |
|------|------|
| views/ai-chat/index.vue | 页面壳：组合下辖组件、持有会话状态、处理 `?role=&skill=` query |
| components/ai-chat/RoleSidebar.vue | 角色栏 |
| components/ai-chat/ChatStream.vue | 对话流（气泡 + 消息头像 + 空态引导 + 流式跟随滚动 + 平仄/藏头特殊渲染） |
| components/ai-chat/SkillChips.vue | 能力快捷 chips |
| components/ai-chat/ChatInput.vue | 输入框 + 发送按钮（Enter 发送 / Shift+Enter 换行；生成中切换为「停止」） |
| configs/ai-chat/personas.ts | 角色常量（含 systemPrompt） |
| configs/ai-chat/skills.ts | 能力常量（含任务模板与发送前校验） |
| composables/useAiChat.ts | 会话状态、SSE 流式请求、多轮上下文组装、服务端会话同步 |
| api/modules/ai.ts | `chatStream`（`fetch` + `ReadableStream` 解析 SSE、401 静默刷新后重放一次）与会话接口封装 |

【moyuan_business_modules.md · 业务模块说明 / 25. AI 对话页「诗宝阁」模块 / 前端路由】
【章节：业务模块说明 / 25. AI 对话页「诗宝阁」模块 / 前端路由】
### 前端路由

| 路径 | 说明 |
|------|------|
| `/ai-chat` | 诗宝阁 AI 对话页面（支持 `?role=xxx&skill=xxx` 预选角色与能力） |

> 首页顶部导航栏已新增「诗宝阁」入口（与「诗云」「交流广场」同级）；首页原「展风拓潮」AI 区的藏头诗 / 平仄 / 飞花令入口**保持不变，两处并存**。

【moyuan_business_modules.md · 业务模块说明 / 25. AI 对话页「诗宝阁」模块 / API 接口】
【章节：业务模块说明 / 25. AI 对话页「诗宝阁」模块 / API 接口】
### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| POST /api/ai/chat/stream | 主链路。SSE 流式对话（请求 `{ moduleCode, messages[], temperature, maxTokens }`，响应 `text/event-stream`，事件 `{"delta":"…"}` … `[DONE]`；限流/入参错误以 JSON 返回） | sc-moyuan-backend AiController |
| POST /api/ai/chat | 兜底。非流式对话（请求 `{ message, moduleCode:'chat' }`，响应 `data.reply`），仅在流式遇可重试失败时调用 | sc-moyuan-backend AiController |
| GET /api/ai/chat/sessions/{roleId} | 读取当前用户在该角色下的会话 | AiChatSessionController |
| PUT /api/ai/chat/sessions/{roleId} | 保存会话（`{"messages":[…]}`，服务端只保留最近 40 条） | AiChatSessionController |
| DELETE /api/ai/chat/sessions/{roleId} | 清空该角色会话 | AiChatSessionController |
| POST /api/ai/tone-check | 平仄校验（本地确定性引擎，不消耗 AI 额度） | sc-moyuan-backend AiController |

【moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.3 收藏 / 点赞（C3）】
【章节：系统工作流文档 / 8. 诗词内容域 / 8.3 收藏 / 点赞（C3）】
### 8.3 收藏 / 点赞（C3）
- **触发入口**：`POST|GET /api/poems/{id}/like`、`POST|GET /api/poems/{id}/favorite`、`GET /api/poems/favorites`。
- **分步流程**：点赞 `toggleLike`（`@Transactional`）按 `user_id+target_id+target_type=POEM` 计数 `user_like`：存在则删记录且 `like_count-1`（`gt 0` 保护），否则插入并 `+1`；状态查询 `isLike/isFavorite` 计数判定；`getFavorites` 取收藏 ID 集后 `in` 查 poem 并填充。
- **注意（代码事实）**：收藏 `UserServiceImpl.toggleFavorite` **只写 `user_favorite` 表，不维护 `poem.favorite_count`**；点赞会维护 `poem.like_count`。
- **数据与缓存**：`user_like`、`user_favorite`、`poem.like_count`。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 诗宝阁AI对话（play.bao-ge）】
【章节：墨渊小助手系统知识库（24 条） / 诗宝阁AI对话（play.bao-ge）】
## 诗宝阁AI对话（play.bao-ge）

- 分类：play
- 关键词：诗宝阁,AI对话,创作,鉴赏,平仄校验,藏头诗
- 别名：诗阁,诗宝阁
- 前端路由：/ai-chat
- 说明：诗宝阁是统一的 AI 对话页面，AI 以不同角色与能力帮你完成命题作诗、集句成诗、藏头诗、润色入律、平仄校验、意象典故、诗话点评、飞花令、诗词接龙等任务。可在左侧选择角色与能力后直接对话，回复以流式方式逐字输出，生成过程中可随时点击「停止」。会话按角色保存在账号下并保留 7 天，换设备登录同一账号可以接着聊。入口在顶部导航的「诗宝阁」。
- 使用步骤：
  1. 打开诗宝阁页面
  2. 选择角色与能力
  3. 输入你的需求并发送
  4. 查看 AI 返回的文本、藏头高亮或平仄标注结果
- 常见问答：
  - 问：诗宝阁是什么  答：诗宝阁是 AI 对话页面，AI 分角色和能力，可帮你作诗、鉴赏、校验平仄、玩飞花令等。
  - 问：诗宝阁和首页的AI工具有什么区别  答：诗宝阁是统一的角色化对话入口，首页 AI 小工具区保留看图写诗、藏头诗、飞花令等快捷入口，两处并存。
  - 问：诗宝阁的对话会保存吗  答：会，按角色保存在你的账号下并保留 7 天，换设备登录同一账号也能接着聊。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 藏头诗（play.acrostic）】
【章节：墨渊小助手系统知识库（24 条） / 藏头诗（play.acrostic）】
## 藏头诗（play.acrostic）

- 分类：play
- 关键词：藏头诗,藏头,写诗,五言,七言
- 别名：藏头
- 前端路由：/
- 说明：藏头诗功能可输入 2 至 8 个藏头字并选择五言或七言，由 AI 生成每句首字依次对应藏头字的诗作。入口在首页 AI 小工具区，诗宝阁「诗仙·创作」角色下也提供藏头诗能力。生成结果会高亮每句首字。
- 使用步骤：
  1. 打开首页 AI 小工具区或诗宝阁
  2. 输入 2 至 8 个藏头字
  3. 选择五言或七言
  4. 点击生成，查看首字高亮的藏头诗
- 常见问答：
  - 问：藏头诗怎么用  答：输入 2 到 8 个藏头字并选择五言或七言，AI 会生成每句首字依次对应藏头字的诗。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 首页（page.home）】
【章节：墨渊小助手系统知识库（24 条） / 首页（page.home）】
## 首页（page.home）

- 分类：page
- 关键词：首页,主页,导航,入口
- 别名：主页
- 前端路由：/
- 说明：首页是站点的总入口，聚合了今日诗词、古诗精赏、当代精选、诗话视野、精选诗人等板块，并提供通往诗云、交流广场、诗宝阁的导航。首页还设有 AI 小工具区，可快捷使用看图写诗、藏头诗、飞花令等玩法。
- 使用步骤：
  1. 打开站点默认进入首页
  2. 通过顶部导航切换各板块
  3. 在 AI 小工具区使用各类玩法
- 常见问答：
  - 问：首页有哪些内容  答：首页聚合了今日诗词、古诗精赏、当代精选、诗话视野、精选诗人等板块，还有 AI 小工具区。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 收藏（feature.favorite）】
【章节：墨渊小助手系统知识库（24 条） / 收藏（feature.favorite）】
## 收藏（feature.favorite）

- 分类：feature
- 关键词：收藏,我的收藏,喜欢,书架
- 别名：我的收藏
- 前端路由：/user/profile
- 说明：收藏功能可把喜欢的诗词加入收藏，登录后在个人中心的「我的收藏」中查看。诗词卡片上提供收藏按钮。
- 使用步骤：
  1. 登录后打开诗词
  2. 点击收藏按钮
  3. 在个人中心查看我的收藏
- 常见问答：
  - 问：怎么收藏诗词  答：登录后在诗词卡片上点击收藏按钮，即可在个人中心的我的收藏中查看。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 游客与登录用户的权限差异（account.guest-limit）】
【章节：墨渊小助手系统知识库（24 条） / 游客与登录用户的权限差异（account.guest-limit）】
## 游客与登录用户的权限差异（account.guest-limit）

- 分类：account
- 关键词：游客,未登录,权限,差异,登录
- 别名：游客权限
- 说明：未登录的游客可以浏览诗词、诗人、帖子与文章，也能在首页直接体验飞花令、诗词接龙等小工具（对局仅临时保存）。发布诗词、发帖、评论、点赞、收藏、报修、诗人认证等操作需要登录后才能使用。
- 使用步骤：
  1. 未登录时浏览诗词等公开内容
  2. 登录后解锁发布、评论、收藏等操作
- 常见问答：
  - 问：不登录能用吗  答：不登录可以浏览诗词、诗人、帖子与文章，也能在首页体验飞花令等小工具；发布、评论、收藏等需要登录。
```
</details>

## H6（hard）诗词接龙开局限定朝代以后，哪些环节会被限制在该朝代内？

**回答**：在诗词接龙游戏中，若开局限定朝代范围，以下三个核心环节均会被严格限制在该朝代内进行校验：AI 起句、AI 接句以及玩家提交诗句。超出该范围将判定为“诗句真实性校验不通过”并提示越界（来源：moyuan_business_modules.md·业务模块说明/24.游戏模块（飞花令/诗词接龙）/功能说明；moyuan_system_knowledge.md·墨渊小助手系统知识库（24 条）/诗词接龙（play.chain））。

具体限制环节如下：
1. **AI 起句**：选定朝代后，AI 生成首句仅在该朝代库内随机选取。
2. **AI 接句**：AI 回复用户的诗句时，提示词会限定该朝代范围，确保接句符合设定。
3. **玩家提交校验**：用户作答的真实性校验逻辑同样绑定该朝代，非该朝代的诗句无法通过验证。

此外，游戏机制规定现代诗不参与本游戏，且起句、AI 接句与玩家作答均需在限定朝代内完成（来源：moyuan_business_modules.md·业务模块说明/24.游戏模块（飞花令/诗词接龙）/功能说明）。

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 2. 诗词模块 / 功能说明】
【章节：业务模块说明 / 2. 诗词模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 7. AI 模块 / 功能说明】
【章节：业务模块说明 / 7. AI 模块 / 功能说明】
/ 集句成诗 / 藏头诗 / 润色入律 / 平仄校验 / 韵脚查询 / 对对联 / 意象典故 / 诗话点评 / 飞花令 / 诗词接龙 / 群贤论诗）；对话走 `POST /api/ai/chat/stream`（SSE 流式，网络类失败自动回退非流式 `POST /api/ai/chat`），请求携带 system 人设 + 最近 12 条历史作为多轮上下文，生成中可停止；会话按角色存服务端 Redis（`GET/PUT/DELETE /api/ai/chat/sessions/{roleId}`，TTL 7 天）并以 localStorage 兜底，详见第 25 章
- **飞花令 / 诗词接龙**：原为纯前端小工具（复用 `POST /api/ai/chat`），现已升级为**独立游戏模块**（见第 24 章「游戏模块」），拥有专属后端接口 `/api/game/feihua`、`/api/game/chain` 与 `ai_module_config` 游戏模块绑定；首页 AI 区块保留「飞花令」「诗词接龙」两张卡片入口。
- **古籍OCR识文**（上传古诗词图片/书法作品/碑帖，AI精准识别文字内容，保留原文结构，标注印章题款）——**当前维护中**：后端开关 `ai.features.

【moyuan_business_modules.md · 业务模块说明 / 23. 诗云模块 / 功能说明】
【章节：业务模块说明 / 23. 诗云模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 功能说明】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 功能说明】
### 功能说明

- **飞花令**（`/game/feihua`）：按「令字」接句，用户每回合提交一句含令字的真实古诗词名句。
  - 三种模式：`ancient`（古制严格，令字须落在本回合第 N 个汉字位置）、`normal`（标准版，诗句包含令字即可）、`super`（超级飞花令，含 `char` 多关键字 / `topic` 主题两种子类型）
  - 三种难度：`easy`（达标 10 句即胜）、`medium`（达标 20 句即胜）、`hard`（AI 无句可接则用户胜，无固定目标条数）
  - 每题给出 AI 候选诗句（LLM 生成 + 后端按位次/判重/真实性二次过滤后下发）
- **诗词接龙**（`/game/chain`）：以 AI 起句的末字为首字接出下一句名句，AI 再接给用户，交替进行；本地诗词库随机取起句（不调用 LLM）；AI 接不上时用户胜，链长即有效句数。
  - **选择范围**：配置阶段可选「中国古典诗词（不限朝代，默认）」或具体朝代（取自 `dynasty` 表）；选定后 AI 起句仅在该朝代内取句，AI 接句提示词限定该朝代，且**玩家提交与 AI 接句的真实性校验均限定在该朝代内**（超出范围判为「诗句真实性校验不通过」并提示越界）。
- **回合计时（计时/监测/缓冲三子态，打字即暂停）**：单回合有效答题时长 30 秒。

【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 功能说明】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 功能说明】
- **三级真实性校验链**：① 本地 `poem` 表 ngram 全文索引 `MATCH...AGAINST`（限定范围时附加 `dynasty_id` 过滤）→ ② 外部 APIHZ 复核（仅在**不限朝代**时可用，外部源无法确认朝代归属）→ ③ LLM 兜底真伪判断（`feihua_verify`，限定范围时提示词附朝代约束），全链路失败即判为「诗句真实性校验不通过」。
- **游客模式**：游客与新页面共用同一套接口，后端按登录态分层——**游客对局仅存 Redis（不落 MySQL），登录用户对局与有效诗句写入 `game_record` / `game_sentence`**；前端以 `GuestBadge` 展示游客标识，登录态用户对局可回查。
- **状态/判重后端权威**：对局运行时状态存 Redis（键 `game:{gameId}` 与 `game:{gameId}:sentences`，TTL 30 分钟，交互续期）；**超时判定由后端依前端上报的 `activeSeconds` 裁定**（见上文「超时判定」）。
- **输入方式**：支持文本与语音（`inputType`），语音仅标识，校验逻辑一致。

【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 玩法规则速览】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 玩法规则速览】
### 玩法规则速览

| 维度 | 取值 | 说明 |
|------|------|------|
| 玩法类型 | feihua / chain | 对局表 `game_type` 区分 |
| 飞花令模式 | ancient / normal / super | super 需 `superType`（char/topic） |
| 飞花令难度 | easy / medium / hard | 达标条数 easy=10、medium=20、hard=null（AI 枯竭即胜） |
| 接龙选择范围 | 不限（默认，中国古典诗词）/ 具体朝代 | 传 `dynastyId` 限定；起句、AI 接句、玩家校验均限定在该朝代内 |
| 对局结果 | win / lose / giveup | 写入 `game_record.game_result` |

【moyuan_business_modules.md · 业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 前端组件】
【章节：业务模块说明 / 24. 游戏模块（飞花令 / 诗词接龙） / 前端组件】
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

【moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.5 外部诗词导入（C5）】
【章节：系统工作流文档 / 8. 诗词内容域 / 8.5 外部诗词导入（C5）】
### 8.5 外部诗词导入（C5）
- **触发入口**：`POST /api/poems/import-external`（title/content/author/dynasty）；`POST /api/poems/fix-external-poems`。
- **分步流程**：校验 title → 按 dynasty 名称查朝代 → 按 author 查诗人（无则新建 Poet 并 `clearAllCache`）→ 按 `title+poetId` 查重：命中补全空缺字段返回 `{id,imported:false}`，未命中落库（`source=external`、`poemType=ancient`、`isOriginal=0`、`auditStatus=1`、status=1）并清 `poems` 缓存返回 `{id,imported:true}`。修复接口取 `source=external` 且 `poet_id is null` 前 100 条，调 `https://cn.apihz.cn/api/zici/poetry.php` 补全作者朝代，成功后清 `poets`/`poems` 缓存返回 `{total,fixed}`。
- **关键类与方法**：`PoemController.importExternalPoem/fixExternalPoems`、`PoemServiceImpl`、`PoetDefaultAvatar.getAvatar`。
- **数据与缓存**：`poem.source/is_original/poem_type/audit_status`；清缓存 key：`poems`、`poets`。

---

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 诗词接龙（play.chain）】
【章节：墨渊小助手系统知识库（24 条） / 诗词接龙（play.chain）】
## 诗词接龙（play.chain）

- 分类：play
- 关键词：诗词接龙,接龙,接句,首尾相接,诗词游戏
- 别名：接龙游戏,诗龙
- 前端路由：/game/chain
- 说明：诗词接龙由 AI 起句，你接出的诗句首字须与上一句末字相同，再交给 AI 接，交替进行；一方接不上即判负。入口在首页 AI 小工具区的「诗词接龙」卡片，登录后进入对局页。开局可限定朝代范围，起句、AI 接句与你的作答都会在该朝代内校验。现代诗不参与本游戏。
- 使用步骤：
  1. 选择朝代范围（默认中国古典诗词不限朝代）
  2. AI 给出起句，你接出首字等于上句末字的真实古诗名句
  3. 与 AI 交替接句
  4. 接不上或超时判负，链长即有效句数
- 常见问答：
  - 问：诗词接龙怎么玩  答：AI 起句后，你接的诗句首字要和上一句末字相同，AI 再接，交替进行，接不上的一方判负。
  - 问：接龙可以限定朝代吗  答：可以，开局选择具体朝代后，起句、AI 接句与你的作答都会限定在该朝代内校验。
```
</details>

## H7（hard）诗云页面的配色实验室能调哪些显示参数？一共几套配色方案？

**回答**：诗云页面「配色实验室」可调节以下显示参数：光晕强度、连线可见度、节点尺寸、整体亮度，以及「显示诗人姓名」「诗人按朝代配色」开关。（来源：moyuan_business_modules.md·业务模块说明/23. 诗云模块/功能说明）

一共提供 7 套配色方案，分别为：琥珀星海、青玉、珊瑚、冰川蓝、梅子紫、苔绿、赤陶。（来源：moyuan_business_modules.md·业务模块说明/23. 诗云模块/功能说明；moyuan_system_workflow.md·系统工作流文档/15.

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 模块总览】
【章节：业务模块说明 / 模块总览】
| 诗词外部获取内容的缓存管理 | 前端+后端已实现 |
| 22 | 联系我们模块 | 用户反馈表单页面 | 前端已实现 |
| 23 | 诗云模块 | Three.js 3D 星空可视化，诗人—诗作两级层级星图 + 配色实验室 | 前端已实现 |
| 24 | 游戏模块 | 飞花令 / 诗词接龙对局（模式/难度、回合计时、三级真实性校验、游客模式） | 前端+后端已实现 |
| 25 | AI 对话页「诗宝阁」 | 角色化统一对话入口：6 角色 × 12 能力，覆盖命题作诗/集句/藏头/润色/平仄校验/韵脚查询/对对联/意象典故/诗话点评/飞花令/接龙/群贤论诗；其中飞花令为设令字后逐句判定（含字/判重/真实性）的对局流程 | 前端+后端已实现（SSE 流式、多轮上下文、服务端会话） |
| 26 | 全局 AI 小窗与系统知识库 | 全站悬浮 AI 问答入口：诗词助理 / 网页用法助理双助理，接入 system_knowledge 知识库 | 前端+后端已实现 |

---

【moyuan_business_modules.md · 业务模块说明 / 3. 诗人模块 / 功能说明】
【章节：业务模块说明 / 3. 诗人模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 23. 诗云模块 / 功能说明】
【章节：业务模块说明 / 23. 诗云模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 23. 诗云模块 / 技术方案】
【章节：业务模块说明 / 23. 诗云模块 / 技术方案】
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

【moyuan_business_modules.md · 业务模块说明 / 23. 诗云模块 / 数据可视化】
【章节：业务模块说明 / 23. 诗云模块 / 数据可视化】
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

【moyuan_system_workflow.md · 系统工作流文档 / 12. 论坛与社区域 / 12.2 内容审核流（E6）】
【章节：系统工作流文档 / 12. 论坛与社区域 / 12.2 内容审核流（E6）】
verified_status=0` | `PUT /api/admin/poet-profiles/{id}/verify` |
  | 诗人草稿 | `poet_draft.status=0` | `PUT /api/admin/poet-drafts/{id}/review` |
  | 投稿建议 | `poet_suggestion.status=pending` | `PUT /api/admin/poet-suggestions/{id}/review` |
- **后台审核列表接口与前端取数约定**：
  - 列表查询：诗作 `GET /api/admin/poems`（参数 `page`、`size`、`keyword`、`auditStatus`、`sort`；`auditStatus` 0-待审核/1-已通过/2-已拒绝，不传返回全部；`sort` 取 asc-按创建时间正序/desc-倒序，默认 desc）；评论 `GET /api/admin/comments`（参数 `page`、`size`、`status`，1-正常/0-隐藏）；AI 内容 `GET /api/admin/ai-contents`（参数 `page`、`size`、`status`，0-待审核/1-已通过/2-已拒绝）。诗人草稿/建议用 `pageNum`、`pageSize`、`status`。
  - 审核变更：`PUT /api/admin/poems/{id}/audit` 的 body `{status, reason}` 中 `status` 对应 `auditStatus` 语义（0/1/2，拒绝为 2）；`PUT /api/admin/comments/{id}/audit` 的 `status` 为 1-正常/0-隐藏。
  - 前端取数：统一响应体为 `{code, message, data}`，Axios 拦截器返回整个响应体，故后台列表页面必须从 `res.data.records` / `res.data.total` 取值（不可写 `res.records`）；诗作审核页状态列与操作判断绑定 `row.auditStatus`，作者/朝代列绑定后端填充的 `poetName` / `dynastyName`（由 `PoemServiceImpl.getAdminPoemList` 调 `populatePoetAndDynasty` 填充），评论页状态列绑定 `row.status`（1-正常/0-隐藏）。
- **关键类与方法**：`AdminController.

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.6 诗云 3D 星空（K6）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.6 诗云 3D 星空（K6）】
### 15.6 诗云 3D 星空（K6）
- **数据加载**：`Promise.all` 并发 `GET /dynasties`、`/poets(pageSize 300)`、`/poems(pageSize 1000)`，失败回退内置 12 朝代。
- **渲染**：`initScene`（Scene+FogExp2+Camera+WebGLRenderer(ACESFilmicToneMapping)+OrbitControls）→ 3200 背景星（Sphere+ShaderMaterial）→ 诗人星（发光小球+光晕，缓存 `poetPositionMap`）+ 同朝代贝塞尔赠诗线 → 诗作独立 Mesh 以「诗人为中心、诗作环绕分布」的球面螺旋生成并经松弛迭代拉开间距，按诗人分组的 LineSegments2 连线 → 交互：hover 高亮并显示 DOM 提示框、点击 `selectPoet→flyToStar+loadPoetPoems+focusPoetPoems`、点击诗作 `selectPoem`、朝代筛选重建。
- **配色实验室**：底部控制模块由 `reactive(viz)` 驱动（7 套方案 + 光晕/连线/尺寸/亮度滑杆 + 标签与朝代配色开关），`applyVizStyles()` 统一重涂背景/雾效、诗人节点、诗作节点与两类连线；整体亮度经画布容器 CSS `filter: brightness()`。
- **诗人姓名标注**：DOM 层 `.label-layer` + `updatePoetLabels()`，每帧 `getWorldPosition().project(camera)` 更新标签屏幕坐标，相机背后/视口外自动隐藏。
- **卸载**：`onUnmounted` 释放 geometry/material、清理标签与定时器、`scene.clear()`、`renderer.dispose()`。
- **数据与缓存**：数据仅内存 ref，不做 localStorage；`poetPositionMap` 内存缓存 3D 坐标，`poemCountMap` 缓存每位诗人的诗作数（用于节点尺寸）。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 诗云星空（play.poetry-cloud）】
【章节：墨渊小助手系统知识库（24 条） / 诗云星空（play.poetry-cloud）】
## 诗云星空（play.poetry-cloud）

- 分类：play
- 关键词：诗云,星空,3D,诗词星图,可视化
- 别名：诗云,诗云星空图
- 前端路由：/poetry-cloud
- 说明：诗云是用 3D 星空展示古诗数据的可视化页面，每位诗人是一颗星，其诗作环绕分布，可缩放旋转浏览。点击诗人星可查看诗人信息与代表作品，点击诗作节点可查看诗词详情。页面底部「配色实验室」可切换配色方案并调节光晕、连线、亮度等显示效果。
- 使用步骤：
  1. 进入诗云页面
  2. 拖动旋转、滚轮缩放浏览星图
  3. 点击诗人星查看诗人信息与作品
  4. 点击诗作节点查看诗词详情
- 常见问答：
  - 问：诗云是什么  答：诗云是把古诗词数据做成 3D 星空的页面，诗人是星、诗作围绕其分布，可交互浏览。
  - 问：诗云怎么用  答：拖动旋转、滚轮缩放浏览星图，点击诗人或诗作节点查看详情。
```
</details>

## H8（hard）AI 评分的分数是怎么从模型输出里解析出来的？解析失败时的兜底分是多少？

**回答**：AI 评分的分数解析及兜底分值如下：

1. **解析方式**：调用 AI 服务时，通过 `aiService.analyzePoem` 方法处理内容，利用正则表达式解析模型返回的分数结果。
2. **兜底分值**：若正则解析失败，系统会自动使用 **3.5** 分作为默认值进行兜底（来源：moyuan_system_workflow.md·8.4 诗词评分）。

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 7. AI 模块 / API 接口】
【章节：业务模块说明 / 7. AI 模块 / API 接口】
### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| POST /api/ai/chat/stream | AI流式问答（SSE，返回 `text/event-stream`；主链路） | sc-moyuan-backend AiController |
| POST /api/ai/chat | AI问答（非流式，流式遇可重试失败时兜底） | sc-moyuan-backend AiController |
| GET /api/ai/chat/sessions/{roleId} | 读取当前用户该角色会话（Redis，TTL 7 天） | AiChatSessionController |
| PUT /api/ai/chat/sessions/{roleId} | 保存会话（服务端最多保留 40 条） | AiChatSessionController |
| DELETE /api/ai/chat/sessions/{roleId} | 清空该角色会话 | AiChatSessionController |
| POST /api/ai/write-poem | 看图写诗（图片 ≤10MB，jpeg/png/gif/webp） | sc-moyuan-backend AiController |
| POST /api/ai/analyze | 智能分析 | sc-moyuan-backend AiController |
| POST /api/ai/ocr | 图片OCR识别（**当前维护中**，`ai.features.

【moyuan_business_modules.md · 业务模块说明 / 26. 全局 AI 小窗与系统知识库模块 / 功能说明】
【章节：业务模块说明 / 26. 全局 AI 小窗与系统知识库模块 / 功能说明】
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

【moyuan_system_workflow.md · 系统工作流文档 / 2. 全局约定与分层（引擎级机制） / 2.2 统一响应与全局异常】
【章节：系统工作流文档 / 2. 全局约定与分层（引擎级机制） / 2.2 统一响应与全局异常】
### 2.2 统一响应与全局异常
- 所有接口返回 `R{code, message, data}`（`common/R.java`）；成功 `code="200"`。
- `BusinessException`（业务码枚举见 `ResultCode`，如 `USER_NOT_FOUND=1001`、`USERNAME_EXISTS=1002`、`EMAIL_EXISTS=1003`、`PASSWORD_ERROR=1004`、`ACCOUNT_DISABLED=1005` 等）由 `GlobalExceptionHandler.handleBusinessException` 统一转 `R.error`。
- 参数校验失败（`MethodArgumentNotValidException`/`BindException`）收集字段错误拼 `字段: 提示` 返回 400。
- 未捕获异常兜底返回 `500 服务器内部错误`，堆栈只进日志不外泄。
- 安全相关 401/403 由 `SecurityConfig` 的 `authenticationEntryPoint`/`accessDeniedHandler` 直接写 `R.error("401",…)` / `R.error("403",…)`。

【moyuan_system_workflow.md · 系统工作流文档 / 8. 诗词内容域 / 8.4 诗词评分（C4）】
【章节：系统工作流文档 / 8. 诗词内容域 / 8.4 诗词评分（C4）】
### 8.4 诗词评分（C4）
- **触发入口**：`GET /api/poems/{poemId}/ratings`；`POST .../ratings?score&comment`；`POST .../ratings/ai`、`GET/regenerate`；`GET .../ratings/me`。
- **分步流程**：校验 score∈[1.0,5.0] → `ratePoem`（`@Transactional`）按 `poem_id+user_id+rating_type=1` 增改评分 → `updatePoemAverageScore` 重算 `avg_score/rating_count`；AI 评分 `rating_type=2`（userId=null）：`aiService.analyzePoem(content, null, "rating")` 走 `ai_module_config` 的独立 `rating` 模块绑定（不再写死默认模型，可在后台「AI功能配置」页面调整）→ 正则解析分（失败兜底 3.5）→ 摘要取前 200 字、去 markdown 标记，随后重算均分，`ai_model` 字段记录实际生效模型名。`getPoemRatings` 返回 userRatings(type=1)/aiRating(type=2 最新)/averageScore/ratingCount，异常回退空数据。
- **关键类与方法**：`PoemRatingServiceImpl.ratePoem/requestAiRating/parseAiScore/generateSummary/updatePoemAverageScore`、`AiService.analyzePoem`。`PoemRatingController.*`。
- **数据与缓存**：`poem_rating`（score/comment/rating_type:1用户2AI/ai_model/ai_summary/ai_analysis）、`poem.avg_score/rating_count`。

【moyuan_system_workflow.md · 系统工作流文档 / 10. 搜索与运营域 / 10.2 押韵/同韵字查询（C9）】
【章节：系统工作流文档 / 10. 搜索与运营域 / 10.2 押韵/同韵字查询（C9）】
### 10.2 押韵/同韵字查询（C9）
- **触发入口**：`GET /api/rhyme/query?character=月`；`GET /api/rhyme/group?group=仄声`（`/api/rhyme/**` 已放行）。
- **分步流程**（数据库优先 + AI 兜底落库）：`queryByCharacter` 取首字符按 `rhyme.character` 等值查；命中则回查该字所属韵部（`rhyme_group`）返回**全部同韵字**，`sort_order` 升序；未命中调 `aiService.chat`（`moduleCode=chat`，`MAX_CHARS=40`）让大模型现查，解析其严格 JSON 结果后 `INSERT IGNORE` 幂等落库（`character+rhyme_group+tone_type` 唯一键防重），再回查返回。`queryByRhymeGroup` 同理：先查整组，未命中走 AI 兜底落库。异常统一 `R.error("500","查询韵脚/韵部失败，请稍后重试")`。无 Redis，直接查库。
- **数据与缓存**：`rhyme`（character/rhyme_group/tone_type/rhyme_category/sort_order）。

【moyuan_system_workflow.md · 系统工作流文档 / 11. AI 能力域 / 11.1 AI 辅助创作与生成（E1）】
【章节：系统工作流文档 / 11. AI 能力域 / 11.1 AI 辅助创作与生成（E1）】
provider` 定位 `AiModelAdapter`，`RestTemplate` 带 `Bearer apiKey` POST 到 `ai_model.api_url`（messages=system+user、max_tokens 默认 1024）→ 解析 `choices[0].message.content` 返回；失败抛 `BusinessException`。
- **注意（代码事实）**：providers 下拉含 qwen/openrouter，但当前**无对应 Adapter**，调用报"不支持的AI提供商"；`OpenAiCompatibleAdapter` 支持 zhipu/deepseek/kimi/mimo，`NvidiaAdapter` 支持 nvidia（vision 抛"暂不支持视觉模型"）。
- **关键类与方法**：`AiController.*`、`AiServiceImpl.chat/writePoemFromImage/analyzePoem/matchCouplet/ocrImage`、`AiModelRegistry.chat/vision/chatByModule/getModel`、`OpenAiCompatibleAdapter/NvidiaAdapter`。
- **数据与缓存**：`ai_model`（name/provider/apiUrl/modelId/visionModelId/apiKey/maxTokens/isEnabled/isDefault/modelType）；内存 `AiModelRegistry.modelCache`。

【moyuan_system_workflow.md · 系统工作流文档 / 17. 关键代码速查表】
【章节：系统工作流文档 / 17. 关键代码速查表】
4（诗词 AI 评分改为独立 `rating` 模块：`PoemRatingServiceImpl.requestAiRating` 由写死默认模型改为 `aiService.analyzePoem(content, null, "rating")`，`ai_model` 字段记录实际生效模型名；后台新增「AI功能配置」页面（`/admin/ai-config`）可按模块调整模型与提示词；此前 v1.3 补充流式问答入口 `POST /api/ai/chat/stream` 调用链，v1.2 新增首页小游戏区块工作流）
**维护人员**：墨渊开发团队
**维护说明**：本文为 docs 本地维护文件，受 `.git/info/exclude` 排除不提交远程；代码变更后请依据 doc-manager 技能核对本文与实现一致性。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 诗词AI评分（play.rating）】
【章节：墨渊小助手系统知识库（24 条） / 诗词AI评分（play.rating）】
## 诗词AI评分（play.rating）

- 分类：play
- 关键词：AI评分,诗词评分,评分,打分
- 别名：AI打分,诗词打分
- 前端路由：/poem/:id
- 说明：诗词详情页提供 AI 评分，由 AI 从意境、格律、修辞、情感、用词等维度评价诗词并给出分数。进入任意诗词详情页即可查看或请求 AI 评分，用户本人也可以手动给诗词评分。
- 使用步骤：
  1. 打开任意诗词详情页
  2. 查看已有评分或请求 AI 评分
  3. 查看 AI 给出的评价与分数
- 常见问答：
  - 问：AI评分在哪里  答：在诗词详情页可以查看或请求 AI 评分，AI 会多维度评价并给出分数。
```
</details>

## H9（hard）未登录时能查看自己的浏览历史吗？能体验哪些诗词游戏？

**回答**：未登录用户无法查看自己的浏览历史，但可体验部分诗词游戏。

- **浏览历史**：需登录后在个人中心（`/user/profile`）查看；历史记录由服务端保存，不存本地，未登录者不可见（来源：moyuan_system_knowledge.md·墨渊小助手系统知识库（24 条）/ 浏览历史（feature.history）；moyuan_system_knowledge.md·墨渊小助手系统知识库（24 条）/ 个人中心（page.profile））。
- **诗词游戏**：未登录用户可在首页直接体验飞花令、诗词接龙等小工具，但对局仅临时保存，无法进入正式对局页；发布、评论、收藏等操作需登录后才能使用（来源：moyuan_system_knowledge.md·墨渊小助手系统知识库（24 条）/ 游客与登录用户的权限差异（account.guest-limit））。

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 2. 诗词模块 / 功能说明】
【章节：业务模块说明 / 2. 诗词模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 2. 诗词模块 / API 接口】
【章节：业务模块说明 / 2. 诗词模块 / API 接口】
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

【moyuan_system_workflow.md · 系统工作流文档 / 5. 用户与个人中心域 / 5.2 浏览历史（B2）】
【章节：系统工作流文档 / 5. 用户与个人中心域 / 5.2 浏览历史（B2）】
### 5.2 浏览历史（B2）
- **目标**：记录用户访问的诗词/诗人历史并支持查看/清理。
- **触发入口**：`GET /api/history`、`POST /api/history`、`DELETE /api/history`。
- **分步流程**：访问时经 `UserHistoryServiceImpl` 记录（target_type/target_id），列表分页返回，支持按 `DELETE` 清空。
- **关键类与方法**：`HistoryController`、`UserHistoryServiceImpl`。
- **数据与缓存**：`user_history`（前端不落 localStorage，走服务端接口）。

---

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 飞花令（play.feihualing）】
【章节：墨渊小助手系统知识库（24 条） / 飞花令（play.feihualing）】
## 飞花令（play.feihualing）

- 分类：play
- 关键词：飞花令,对诗,令字,行酒令,诗词游戏,怎么玩
- 别名：花令,飞花令游戏
- 前端路由：/game/feihua
- 说明：飞花令是与 AI 对句的诗词游戏，你每回合说出一句含指定「令字」的真实古诗词名句，AI 轮流接句，接不上或超时即判负。入口在首页 AI 小工具区的「飞花令」卡片，登录用户点击后进入对局页。有古制严格、标准版、超级飞花令三种模式与三档难度可选。现代诗不参与本游戏。
- 使用步骤：
  1. 选择模式与难度
  2. 设定令字（如「花」）
  3. 每回合提交一句含令字的古诗词，单回合限时 30 秒
  4. 与 AI 交替接句，AI 接不上或你超时判负
- 常见问答：
  - 问：飞花令怎么玩  答：每回合提交一句含指定令字的真实古诗词名句，与 AI 交替接句，AI 接不上或你超时则对方判负。
  - 问：在哪里能玩飞花令  答：首页 AI 小工具区有「飞花令」卡片，登录用户点击进入对局页。
  - 问：飞花令有几种模式  答：有古制严格、标准版、超级飞花令三种模式，难度分简单、中等、困难三档。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 寻章摘句（page.search）】
【章节：墨渊小助手系统知识库（24 条） / 寻章摘句（page.search）】
## 寻章摘句（page.search）

- 分类：page
- 关键词：寻章摘句,搜索,检索,找诗,诗词列表
- 别名：诗词列表,搜索页
- 前端路由：/poem
- 说明：寻章摘句即诗词列表页，支持按朝代、分类、诗人筛选，也能用关键词搜索诗词；题库只含古诗，现代诗单独成页。全局搜索能力内嵌在首页与诗词列表页中，没有单独的搜索页。未登录用户也能浏览与搜索。
- 使用步骤：
  1. 进入诗词列表页
  2. 通过筛选项或关键词查找诗词
  3. 点开卡片查看诗词详情
- 常见问答：
  - 问：寻章摘句在哪里  答：寻章摘句就是诗词列表页，可在顶部导航进入，支持筛选与关键词搜索。
  - 问：怎么搜索诗词  答：在诗词列表页输入关键词即可搜索，也可按朝代、分类、诗人筛选。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 个人中心（page.profile）】
【章节：墨渊小助手系统知识库（24 条） / 个人中心（page.profile）】
## 个人中心（page.profile）

- 分类：page
- 关键词：个人中心,我的,资料,收藏,历史,认证
- 别名：个人主页,我的页面
- 前端路由：/user/profile
- 说明：个人中心可查看与修改个人资料、上传头像、修改密码，并查看我的收藏、我的点赞、我的帖子和浏览历史。诗人认证申请也从个人中心提交。需要登录后进入。
- 使用步骤：
  1. 登录后进入个人中心
  2. 修改资料、头像或密码
  3. 查看收藏、点赞、帖子与浏览历史
  4. 需要时提交诗人认证申请
- 常见问答：
  - 问：个人中心在哪里  答：登录后可从用户菜单进入个人中心，管理资料、收藏、历史并提交诗人认证。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 智能检索（feature.search）】
【章节：墨渊小助手系统知识库（24 条） / 智能检索（feature.search）】
## 智能检索（feature.search）

- 分类：feature
- 关键词：搜索,检索,找诗,搜诗人,关键字
- 别名：智能搜索,全文搜索
- 前端路由：/poem
- 说明：智能检索支持按关键词搜索诗词、诗人与帖子，首页与诗词列表页内嵌了搜索入口。搜索会记录你的历史，登录后还会根据兴趣给出推荐。
- 使用步骤：
  1. 在首页或诗词列表页输入关键词
  2. 查看匹配的诗词、诗人或帖子
  3. 点开结果查看详情
- 常见问答：
  - 问：怎么搜索  答：在首页或诗词列表页的搜索框输入关键词，即可搜索诗词、诗人与帖子。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 浏览历史（feature.history）】
【章节：墨渊小助手系统知识库（24 条） / 浏览历史（feature.history）】
## 浏览历史（feature.history）

- 分类：feature
- 关键词：浏览历史,历史记录,最近看过
- 别名：历史记录
- 前端路由：/user/profile
- 说明：浏览历史记录你访问过的诗词与诗人，登录后可在个人中心查看，也支持清空。历史通过服务端接口保存，不存放在本地。
- 使用步骤：
  1. 登录后浏览诗词或诗人
  2. 在个人中心查看浏览历史
  3. 需要时清空历史记录
- 常见问答：
  - 问：浏览历史在哪  答：登录后在个人中心可以查看浏览历史，并支持清空。

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 游客与登录用户的权限差异（account.guest-limit）】
【章节：墨渊小助手系统知识库（24 条） / 游客与登录用户的权限差异（account.guest-limit）】
## 游客与登录用户的权限差异（account.guest-limit）

- 分类：account
- 关键词：游客,未登录,权限,差异,登录
- 别名：游客权限
- 说明：未登录的游客可以浏览诗词、诗人、帖子与文章，也能在首页直接体验飞花令、诗词接龙等小工具（对局仅临时保存）。发布诗词、发帖、评论、点赞、收藏、报修、诗人认证等操作需要登录后才能使用。
- 使用步骤：
  1. 未登录时浏览诗词等公开内容
  2. 登录后解锁发布、评论、收藏等操作
- 常见问答：
  - 问：不登录能用吗  答：不登录可以浏览诗词、诗人、帖子与文章，也能在首页体验飞花令等小工具；发布、评论、收藏等需要登录。
```
</details>

## H10（hard）诗云里诗人节点的大小由什么决定？3D 坐标数据存在哪里？

**回答**：诗云模块中诗人节点大小由其名下诗作数量自适应决定，具体计算公式为 `starSize = 2.4 + log10(作品数+1) * 1.2`（来源：moyuan_business_modules.md·业务模块说明 / 23. 诗云模块 / 数据可视化）。

3D 坐标数据存储在内存 ref 中，不保存至 localStorage。具体细节如下：
- **存储位置**：使用 `poetPositionMap` 缓存每位诗人的 3D 坐标，以及 `poemCountMap` 缓存诗作数量用于计算节点尺寸（来源：moyuan_system_workflow.md·系统工作流文档 / 15.6 诗云 3D 星空（K6））。
- **数据生成**：诗人星位置通过黄金角球面螺旋算法生成，坐标经 `initScene` 初始化并缓存于内存（来源：moyuan_business_modules.md·业务模块说明 / 23. 诗云模块 / 数据可视化；moyuan_system_workflow.md·系统工作流文档 / 15.6 诗云 3D 星空（K6））。
- **生命周期**：组件卸载时通过 `onUnmounted` 释放 geometry/material 并清理内存缓存，确保无持久化存储（来源：moyuan_system_workflow.md·系统工作流文档 / 15.6 诗云 3D 星空（K6））。

**评级**： 

<details><summary>上下文</summary>

```
【moyuan_business_modules.md · 业务模块说明 / 3. 诗人模块 / 功能说明】
【章节：业务模块说明 / 3. 诗人模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 23. 诗云模块 / 功能说明】
【章节：业务模块说明 / 23. 诗云模块 / 功能说明】
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

【moyuan_business_modules.md · 业务模块说明 / 23. 诗云模块 / 技术方案】
【章节：业务模块说明 / 23. 诗云模块 / 技术方案】
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

【moyuan_business_modules.md · 业务模块说明 / 23. 诗云模块 / 前端组件】
【章节：业务模块说明 / 23. 诗云模块 / 前端组件】
### 前端组件

| 组件 | 说明 |
|------|------|
| poetry-cloud/index.vue | 诗云主页面（Three.js 3D 渲染、数据加载、交互控制、悬浮提示） |

【moyuan_business_modules.md · 业务模块说明 / 23. 诗云模块 / API 接口】
【章节：业务模块说明 / 23. 诗云模块 / API 接口】
### API 接口

| 接口 | 说明 | 后端模块 |
|------|------|----------|
| GET /api/dynasties | 获取朝代列表 | backend DynastyController |
| GET /api/poets | 获取诗人列表（pageSize 300，用于 3D 星系） | sc-moyuan-backend PoetController |
| GET /api/poems | 获取诗词列表（pageSize 1000，用于诗作节点与计数） | backend PoemController |

【moyuan_business_modules.md · 业务模块说明 / 23. 诗云模块 / 前端路由】
【章节：业务模块说明 / 23. 诗云模块 / 前端路由】
### 前端路由

| 路径 | 说明 |
|------|------|
| `/poetry-cloud` | 诗云星空页面（hideHeader） |

【moyuan_business_modules.md · 业务模块说明 / 23. 诗云模块 / 数据可视化】
【章节：业务模块说明 / 23. 诗云模块 / 数据可视化】
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

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.6 诗云 3D 星空（K6）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.6 诗云 3D 星空（K6）】
### 15.6 诗云 3D 星空（K6）
- **数据加载**：`Promise.all` 并发 `GET /dynasties`、`/poets(pageSize 300)`、`/poems(pageSize 1000)`，失败回退内置 12 朝代。
- **渲染**：`initScene`（Scene+FogExp2+Camera+WebGLRenderer(ACESFilmicToneMapping)+OrbitControls）→ 3200 背景星（Sphere+ShaderMaterial）→ 诗人星（发光小球+光晕，缓存 `poetPositionMap`）+ 同朝代贝塞尔赠诗线 → 诗作独立 Mesh 以「诗人为中心、诗作环绕分布」的球面螺旋生成并经松弛迭代拉开间距，按诗人分组的 LineSegments2 连线 → 交互：hover 高亮并显示 DOM 提示框、点击 `selectPoet→flyToStar+loadPoetPoems+focusPoetPoems`、点击诗作 `selectPoem`、朝代筛选重建。
- **配色实验室**：底部控制模块由 `reactive(viz)` 驱动（7 套方案 + 光晕/连线/尺寸/亮度滑杆 + 标签与朝代配色开关），`applyVizStyles()` 统一重涂背景/雾效、诗人节点、诗作节点与两类连线；整体亮度经画布容器 CSS `filter: brightness()`。
- **诗人姓名标注**：DOM 层 `.label-layer` + `updatePoetLabels()`，每帧 `getWorldPosition().project(camera)` 更新标签屏幕坐标，相机背后/视口外自动隐藏。
- **卸载**：`onUnmounted` 释放 geometry/material、清理标签与定时器、`scene.clear()`、`renderer.dispose()`。
- **数据与缓存**：数据仅内存 ref，不做 localStorage；`poetPositionMap` 内存缓存 3D 坐标，`poemCountMap` 缓存每位诗人的诗作数（用于节点尺寸）。

【moyuan_system_workflow.md · 系统工作流文档 / 15. 前端数据流转层 / 15.7 路由守卫（K7）】
【章节：系统工作流文档 / 15. 前端数据流转层 / 15.7 路由守卫（K7）】
### 15.7 路由守卫（K7）
- **分步流程**：`beforeEach` 按 `meta.title` 设 document.title → `requiresAuth` 未登录跳 `Login`（带 redirect）；已登录缺 userInfo 先 `fetchUserInfo`；`requiresAdmin` 校验 role !== admin 弹错回 Home；已登录访问 Login/Register 回 Home。`afterEach` 维护 `appStore.cachedViews`（keep-alive）。
- **关键位置**：`router/guards.ts`、`router/index.ts`（`meta`: requiresAuth/requiresAdmin/keepAlive）、`stores/app.ts`。

---

【moyuan_system_knowledge.md · 墨渊小助手系统知识库（24 条） / 诗云星空（play.poetry-cloud）】
【章节：墨渊小助手系统知识库（24 条） / 诗云星空（play.poetry-cloud）】
## 诗云星空（play.poetry-cloud）

- 分类：play
- 关键词：诗云,星空,3D,诗词星图,可视化
- 别名：诗云,诗云星空图
- 前端路由：/poetry-cloud
- 说明：诗云是用 3D 星空展示古诗数据的可视化页面，每位诗人是一颗星，其诗作环绕分布，可缩放旋转浏览。点击诗人星可查看诗人信息与代表作品，点击诗作节点可查看诗词详情。页面底部「配色实验室」可切换配色方案并调节光晕、连线、亮度等显示效果。
- 使用步骤：
  1. 进入诗云页面
  2. 拖动旋转、滚轮缩放浏览星图
  3. 点击诗人星查看诗人信息与作品
  4. 点击诗作节点查看诗词详情
- 常见问答：
  - 问：诗云是什么  答：诗云是把古诗词数据做成 3D 星空的页面，诗人是星、诗作围绕其分布，可交互浏览。
  - 问：诗云怎么用  答：拖动旋转、滚轮缩放浏览星图，点击诗人或诗作节点查看详情。
```
</details>
