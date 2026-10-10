# 系统工作流文档

## 1. 概述

本文件集中描述系统在运行时执行的核心业务工作流（数据流、兜底逻辑、触发器与处理步骤），与 `business/modules.md` 的按模块功能说明互补。凡涉及"某个业务动作对应什么处理流程"均在本文登记。

- **与 modules.md 的分工**：`modules.md` 描述"系统有哪些功能/模块"；本文描述"功能如何被触发并一步步执行"。同一业务的流程细节**只在本文维护**，模块文档保留索引链接，避免双份维护漂移。
- **覆盖范围**：后端 25 个 Controller / 27 个 Service 的运行时链路 + 前端数据流转层（Pinia / 统一请求 / localStorage / 页面生命周期）。
- **信息依据**：本文内容均取自实际源码（后端 `sc-moyuan-backend/src/main/java/com/moyuan/`，前端 `frontend/src/`），不虚构类名、方法、端点或字段。

## 2. 全局约定与分层（引擎级机制）

### 2.1 分层调用约定
- 严格 `Controller → Service（接口+impl）→ Mapper`，禁止跨层调用。
- 实体字段驼峰命名，由 MyBatis-Plus `mapUnderscoreToCamelCase` 映射数据库下划线字段。

### 2.2 统一响应与全局异常
- 所有接口返回 `R{code, message, data}`（`common/R.java`）；成功 `code="200"`。
- `BusinessException`（业务码枚举见 `ResultCode`，如 `USER_NOT_FOUND=1001`、`USERNAME_EXISTS=1002`、`EMAIL_EXISTS=1003`、`PASSWORD_ERROR=1004`、`ACCOUNT_DISABLED=1005` 等）由 `GlobalExceptionHandler.handleBusinessException` 统一转 `R.error`。
- 参数校验失败（`MethodArgumentNotValidException`/`BindException`）收集字段错误拼 `字段: 提示` 返回 400。
- 未捕获异常兜底返回 `500 服务器内部错误`，堆栈只进日志不外泄。
- 安全相关 401/403 由 `SecurityConfig` 的 `authenticationEntryPoint`/`accessDeniedHandler` 直接写 `R.error("401",…)` / `R.error("403",…)`。

### 2.3 JWT 认证与过滤器链
- 无状态会话（`SessionCreationPolicy.STATELESS`），关闭 CSRF。
- `JwtAuthenticationFilter`（挂在 `UsernamePasswordAuthenticationFilter` 之前）从 `Authorization: Bearer` 取 token → 验签/过期 → 校验 **type=access** 且 `jti` 未被 `TokenBlacklistService` 拉黑 → 查库 → 组装 `LoginUser.getAuthorities()`（`ROLE_`+role）写入 SecurityContext。当前 JWT 采用**双 token**（access 短时效 + refresh 续期），登出/刷新将旧 token 的 `jti` 加入黑名单使其失效（详见 4.2、4.3）。
- 公开接口 `permitAll`（首页静态资源、Swagger、`/api/auth/**`、浏览类接口、`/uploads/**` 等）；`/api/admin/**` 需 `ROLE_ADMIN`；其余需登录。详细放行清单见 4.3。

### 2.4 字段填充与逻辑删除
- `create_time` / `update_time` 由 MyBatis-Plus 自动填充。
- 逻辑删除使用 `deleted` 字段（`logic-delete-value=1`）。

### 2.5 缓存策略速览
| 场景 | 载体 | key / 范围 |
|------|------|------------|
| 诗词/诗人列表与详情 | Spring Cache（`@Cacheable`） | `poems::…`、`poets::detail:{id}` |
| 推荐结果 | Spring Cache | `poetRecommendation::…`、`poemRecommendation::…` |
| 搜索建议/热门/历史 | Redis | `search:suggestion:{kw}`、`search:hot`(ZSet)、`search:history:{userId}`(List) |
| AI 模型注册表 | 进程内 `ConcurrentHashMap` | `AiModelRegistry.modelCache` |
| 诗词赏析增强内容 | 关系表 `poem_content_cache` | `poem_title+poet_name+content_type` |
| 前端下拉型数据 | localStorage（前缀 `moyuan_`） | `moyuan_cache_*`（详见 14） |

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
| 报修工单域 | 工单全生命周期 | `RepairController`、`RepairOrderServiceImpl`、`AdminController` | [13](#13-报修工单域) |
| 内容管理/运营域 | 导航 / 静态页 / 诗话视野 / 精选诗人 | `HomeNavigation`、`StaticPageService`、`VisionArticleServiceImpl`、`PoetFeaturedServiceImpl` | [14](#14-内容管理运营域) |
| 前端数据流转层 | 请求层 / Store / localStorage / 首页 / 诗云 / 守卫 | `utils/request.ts`、`stores/*`、`router/guards.ts` | [15](#15-前端数据流转层) |
| 数据初始化流 | init.sql / seed.sql | `db/` 脚本、`AiModelRegistry` 密钥注入 | 见 16 |

> 每条工作流统一模板：**目标 / 触发入口 / 分步流程 / 关键类与方法 / 数据与缓存**。

---

## 4. 认证与权限域

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

### 4.3 JWT 校验与安全过滤器链认证（A3）
- **目标**：受保护请求进入 Controller 前完成 token 验签、身份装配。
- **触发入口**：任何携带 token 的 HTTP 请求（过滤器链自动触发）。
- **分步流程**：`JwtAuthenticationFilter.doFilterInternal` → `extractToken`（`Bearer ` 前缀）→ `jwtUtil.validateToken`（验签 + 过期 + **type=access**）→ `TokenBlacklistService.isBlacklisted(jti)` 黑名单校验 → `getUserId` → 查库 → `status==1` 时组装 `LoginUser` 并写 `UsernamePasswordAuthenticationToken` 到 `SecurityContextHolder`。无论成败都 `doFilter` 放行，访问权限由 `SecurityConfig` 决定。
- **关键类与方法**：`JwtAuthenticationFilter.doFilterInternal/extractToken`、`JwtUtil.validateToken/parseToken/getUserId`、`TokenBlacklistService.isBlacklisted`、`LoginUser.getAuthorities`、`SecurityConfig.filterChain`。
- **数据与缓存**：`user.id/status/role`。

### 4.4 角色权限控制（A4）
- **目标**：区分"公开接口"与"仅 ADMIN 接口"，未登录/未授权返回统一 JSON。
- **分步流程**：
  1. 关闭 CSRF，无状态会话。
  2. `permitAll` 白名单：`/`、`/index.html`、`/assets/**`、`/static/**`、`/favicon.ico`、Swagger（`/doc.html`、`/webjars/**`、`/v3/api-docs/**`、`/swagger-resources/**`）、`/api/auth/**`、浏览接口（`/api/poets`、`/api/dynasties`、`/api/categories`、`/api/forum/posts**`、`/api/forum/comments/**`、`/api/vision/**`、`/api/search/**`、`/api/ai/**`、`/api/poems/fix-external-poems`）、`/uploads/**`。
  3. 单列放行部分 GET：`/api/poems*`、`/api/poem-featured/**`、`/api/home-navigation/**`、`/api/rhyme/**`。
  4. `.requestMatchers("/api/admin/**").hasRole("ADMIN")`；其余 `anyRequest().authenticated()`。
  5. 401/403 由 entryPoint/deniedHandler 输出 `R.error`。
- **关键类与方法**：`SecurityConfig`、`LoginUser.getAuthorities`、`AdminController`。
- **数据与缓存**：`user.role`；前端 `request.ts` 的 `PUBLIC_PATHS` 与公开接口白名单对应。

---

## 5. 用户与个人中心域

### 5.1 用户资料更新 / 头像设置（原 A5）
- **目标**：本人更新昵称/邮箱/手机/头像/简介/性别/生日或修改密码。
- **触发入口**：`PUT /api/users/me`（资料）；`PUT /api/users/me/password`（改密）；头像先 `POST /api/files/upload`（fileType=avatar）再回填。
- **分步流程**：`SecurityUtil.getCurrentUserId()` 取当前用户 → `updateUserInfo` 查库、对非空字段逐个 set 后 `updateById`（返回前 `setPassword(null)` 脱敏）；`updatePassword` 先 `matches` 旧密码（失败抛 1004）再 `encode` 覆盖。
- **关键类与方法**：`UserController.updateCurrentUser/updatePassword`、`UserServiceImpl.updateUserInfo/updatePassword`、`SecurityUtil.getCurrentUserId`。
- **数据与缓存**：`user.nickname/email/phone/avatar/bio/gender/birthday/password`。

### 5.2 浏览历史（B2）
- **目标**：记录用户访问的诗词/诗人历史并支持查看/清理。
- **触发入口**：`GET /api/history`、`POST /api/history`、`DELETE /api/history`。
- **分步流程**：访问时经 `UserHistoryServiceImpl` 记录（target_type/target_id），列表分页返回，支持按 `DELETE` 清空。
- **关键类与方法**：`HistoryController`、`UserHistoryServiceImpl`。
- **数据与缓存**：`user_history`（前端不落 localStorage，走服务端接口）。

---

## 6. 文件与资源域

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
- **关键类与方法**：`FileStorageServiceImpl.upload/delete/getUrl/buildSubDirectory/calculateMd5/applyWatermark`、`FileController`、`WatermarkUtil.addAiWatermark`、`WebMvcConfig.addResourceHandlers`。
- **数据与缓存**：`file_metadata`（file_key/original_name/file_type/mime_type/size/md5/width/height/related_* 等）；配置 `file.upload-dir`（默认 `./uploads`）、`file.base-url`、`file.allowed-extensions`、`file.max-size`。

---

## 7. 审计与统一响应（A7/A8）

### 7.1 操作日志 / 审计记录（A7）
- **目标**：AOP 对 Controller 调用自动记录操作人、方法、参数、IP、耗时、成败。
- **触发入口**：`com.moyuan.controller.*` 任意方法被调用（切面自动拦截）。
- **分步流程**：`OperationLogAspect.around`：记 `method`（`GET/POST …uri`）、`ip`、`operation`（方法名）；`params` 序列化前 `maskSensitiveFields` 把 `password/oldPassword/newPassword/token/secret` 置 `***`；记录当前用户；`proceed()` 成功 status=1、异常 status=0 并记 errorMsg 后向上抛；finally 记 `duration` 落 `operation_log`（失败仅 log.error 不阻断）。查询 `GET /api/admin/logs`（keyword/startTime/endTime 过滤，倒序分页）。
- **关键类与方法**：`OperationLogAspect.around/sanitizeParams/maskSensitiveFields`、`SecurityUtil`、`OperationLogServiceImpl`、`AdminController.getLogs`。
- **数据与缓存**：`operation_log`（user_id/username/operation/method/params/ip/duration/status/error_msg/create_time）。

### 7.2 统一响应与全局异常（A8）（详见 2.2）
- 响应封装 `R`；异常治理 `GlobalExceptionHandler` + `ResultCode` + `BusinessException`。

---

## 8. 诗词内容域

### 8.1 诗词列表查询（C1）
- **触发入口**：`GET /api/poems?pageNum&pageSize&dynastyId&poetId&categoryId&keyword`；`GET /api/poems/modern/page`（现代，固定 dynastyId=13）；`GET /api/poems/favorites`。
- **分步流程**：`getPoemList` 构造 `LambdaQueryWrapper`：status=1、朝代/诗人/分类 `eq`、keyword 对标题/内容 `like`（`wrapper.and`）→ `orderByDesc(createTime)` 分页 → `@Cacheable(value="poems", key="'list:'+…")` 缓存；现代分支额外按 `isOriginal/hasCertifiedPoet` 过滤、`sortBy` 排序并 `populatePoetAndDynasty` 批量填充名称。
- **关键类与方法**：`PoemController.getPoemList/getModernPoems/getFavorites`、`PoemServiceImpl.*`、`PoemMapper/DynastyMapper/CategoryMapper.selectBatchIds`。
- **数据与缓存**：`poem`（title/content/poet_id/dynasty_id/category_id/view_count/is_original/status）；缓存 `poems::list:{dynastyId}:{poetId}:{categoryId}:{keyword}:{pageNum}:{pageSize}`。

### 8.2 诗词详情与赏析缓存（C2）
- **触发入口**：`GET /api/poems/{id}`；`GET/POST /api/poem-content/cache`。
- **分步流程**：`getPoemDetail`：`selectById` → 空/status!=1 抛 `POEM_NOT_FOUND` → `setSql("view_count=view_count+1")` 原子自增并内存同步。赏析增强内容由 `PoemContentCacheServiceImpl` 按 `poem_title+poet_name+content_type` 命中 `poem_content_cache`，命中返回、未命中由外部/AI 产出后 `POST` 落库。
- **关键类与方法**：`PoemController.getPoemDetail`、`PoemServiceImpl`、`PoemContentCacheServiceImpl.getCachedContent/saveCachedContent`、`PoemContentController`。
- **数据与缓存**：`poem_content_cache`；`poem.view_count` 原子自增。

### 8.3 收藏 / 点赞（C3）
- **触发入口**：`POST|GET /api/poems/{id}/like`、`POST|GET /api/poems/{id}/favorite`、`GET /api/poems/favorites`。
- **分步流程**：点赞 `toggleLike`（`@Transactional`）按 `user_id+target_id+target_type=POEM` 计数 `user_like`：存在则删记录且 `like_count-1`（`gt 0` 保护），否则插入并 `+1`；状态查询 `isLike/isFavorite` 计数判定；`getFavorites` 取收藏 ID 集后 `in` 查 poem 并填充。
- **注意（代码事实）**：收藏 `UserServiceImpl.toggleFavorite` **只写 `user_favorite` 表，不维护 `poem.favorite_count`**；点赞会维护 `poem.like_count`。
- **数据与缓存**：`user_like`、`user_favorite`、`poem.like_count`。

### 8.4 诗词评分（C4）
- **触发入口**：`GET /api/poems/{poemId}/ratings`；`POST .../ratings?score&comment`；`POST .../ratings/ai`、`GET/regenerate`；`GET .../ratings/me`。
- **分步流程**：校验 score∈[1.0,5.0] → `ratePoem`（`@Transactional`）按 `poem_id+user_id+rating_type=1` 增改评分 → `updatePoemAverageScore` 重算 `avg_score/rating_count`；AI 评分 `rating_type=2`（userId=null）：`aiService.analyzePoem(content, defaultAiModel)`（默认 zhipu）→ 正则解析分（失败兜底 3.5）→ 摘要取前 200 字、去 markdown 标记，随后重算均分。`getPoemRatings` 返回 userRatings(type=1)/aiRating(type=2 最新)/averageScore/ratingCount，异常回退空数据。
- **关键类与方法**：`PoemRatingServiceImpl.ratePoem/requestAiRating/parseAiScore/generateSummary/updatePoemAverageScore`、`AiService.analyzePoem`。`PoemRatingController.*`。
- **数据与缓存**：`poem_rating`（score/comment/rating_type:1用户2AI/ai_model/ai_summary/ai_analysis）、`poem.avg_score/rating_count`。

### 8.5 外部诗词导入（C5）
- **触发入口**：`POST /api/poems/import-external`（title/content/author/dynasty）；`POST /api/poems/fix-external-poems`。
- **分步流程**：校验 title → 按 dynasty 名称查朝代 → 按 author 查诗人（无则新建 Poet 并 `clearAllCache`）→ 按 `title+poetId` 查重：命中补全空缺字段返回 `{id,imported:false}`，未命中落库（`source=external`、`poemType=ancient`、`isOriginal=0`、`auditStatus=1`、status=1）并清 `poems` 缓存返回 `{id,imported:true}`。修复接口取 `source=external` 且 `poet_id is null` 前 100 条，调 `https://cn.apihz.cn/api/zici/poetry.php` 补全作者朝代，成功后清 `poets`/`poems` 缓存返回 `{total,fixed}`。
- **关键类与方法**：`PoemController.importExternalPoem/fixExternalPoems`、`PoemServiceImpl`、`PoetDefaultAvatar.getAvatar`。
- **数据与缓存**：`poem.source/is_original/poem_type/audit_status`；清缓存 key：`poems`、`poets`。

---

## 9. 诗人内容域

### 9.1 诗人详情与懒加载 / 头像分配（已收录，完整见下文）
- **触发入口**：`GET /api/poets/{id}`。
- **分步流程**：`getPoetDetail`（`@Cacheable("poets","detail:"+id)`）查库并补 `dynastyName` → `isPoetInfoIncomplete` 判 biography/lifeStory/influence/anecdotes/avatar 是否为空 → 不完整则 `fetchAndSavePoetData` 调 `https://cn.apihz.cn/api/zici/poet.php`（id/key/name），按字段逐项补全（pseudonym=tag、biography=content 经 `PoetDataExtractor` 去标签并抽生卒/籍贯、按生卒年定朝代、头像、lifeStory=rwsp、influence=zycj、anecdotes=ysdg）→ 有改动则 `updateById` 并 `evict` 详情缓存。
- **头像来源优先级**：①外部 API `image` 字段（网络图）→ ②`PoetDefaultAvatar.getAvatar()` 本地 `/img/poet_avatars/*.jpg` 兜底。
- **预设头像分配规则（PoetDefaultAvatar）**：第 0 级按 `poet.style` 精准流派匹配（`STYLE_MAP`：豪放派/婉约派/田园派/现实主义/新月派/浪漫主义/古典主义，命中即用，`style` 为空则跳过）→ 第 1~3 级按 性别（已知女诗人名单）→ 地区（中西名单/无朝代）→ 时期（`poetType=="modern"`），组出 `地区_时期_性别` 键匹配头像池；候选则随机，否则「地区+性别」随机 → 同地区随机 → 全局随机。头像池共 11 张（`cn_ancient_male`×3、`cn_ancient_female`×1、`cn_modern_male/female`×1、`west_classical_male/female`×1、`west_romantic_male/female`×1、`west_modern_female`×1）；`poet.style` 字段经 `docs/database/migration_003_poet_style.sql` 新增，外部诗人 API 暂无流派字段。
- **关键类与方法**：`PoetServiceImpl.getPoetDetail/isPoetInfoIncomplete/fetchAndSavePoetData`、`PoetDataExtractor`、`PoetDefaultAvatar.getAvatar`。
- **数据与缓存**：`poet`（biography/year_info/birthplace/pseudonym/life_story/influence/anecdotes/avatar）；缓存 `poets::detail:{id}`。

### 9.2 推荐 / 热门诗人（C7）
- **触发入口**：`GET /api/poets/recommend?limit=6`（上限 20）、`GET /api/poets/popular?limit=10`（上限 50）；诗词侧 `GET /api/search/poems/recommended|popular`。
- **分步流程**：取当前用户（未登录走热门）→ `userId==null` 走 `getPopularPoets`；`buildUserPoetMatrix` 由收藏/点赞/浏览行为构建 用户→诗人:{权重}（收藏 5、点赞 3、浏览 1）→ `calculateCollaborativeScores` 余弦相似度选 Top20 近邻、对未交互诗人累加 `similarity×权重` → `getInterestBasedPoetScores` 按 `interests` 朝代加权（古典/现代/外国）→ `mergeScores` 相加取 TopN → 过滤 status=1；空回退热门。热门 `calculatePoetPopularity`（诗收藏5+词收藏5+赞3+浏览1）取 TopN。结果 `@Cacheable` 缓存。
- **关键类与方法**：`PoetController.getRecommendedPoets/getPopularPoets`、`RecommendationServiceImpl.*`、`SearchController.getRecommendedPoems/getPopularPoems`。
- **数据与缓存**：`user_favorite/user_like/user_history`、`user.interests`、`poem.view_count/like_count`；缓存 `poetRecommendation::user:{userId}:{limit}`、`popular:{limit}` 等。

### 9.3 诗人认证、草稿与投稿建议（E10，合并）
- **精选诗人展示/管理触发**：`GET /api/poet-featured/random?count`、`/list`、`/{id}`；后台 CRUD。`getRandomPoetFeatured` 取启用项 `Collections.shuffle` 截断。
- **认证申请/审核**：`POST /api/poet-profile/apply`（落 `poet_profile`，`verifiedStatus=2` 待审，同步 `user.poet_verified=2`、`user.poet_profile_id`；他人查看屏蔽 contactInfo）；`PUT /api/admin/poet-profiles/{id}/verify` 置通过/拒绝并回写 `user.poet_verified`，记录作品/获赞数。
- **草稿投稿**：`POST /api/poet-drafts`（校验诗人存在，`section` 映射字段并快照原值，status=0 待审）；后台 `PUT /api/admin/poet-drafts/{id}/review` 通过时 `publishDraft` 按 section 把 content 写回 `poet` 对应字段。
- **投稿建议**：`POST /api/poet-suggestions`（前端 `sanitizeInput` 清洗 HTML，status=pending）；后台 `PUT /api/admin/poet-suggestions/{id}/review`。
- **待审统计**：`countByStatus` 汇总到 `GET /api/admin/audit/stats`。
- **关键类与方法**：`PoetFeaturedServiceImpl`、`PoetProfileServiceImpl.applyVerification/verifyProfile/getByUserId`、`PoetDraftServiceImpl.saveDraft/reviewDraft/publishDraft`、`PoetSuggestionServiceImpl.submitSuggestion/reviewSuggestion`。
- **数据与缓存**：`poet_featured`、`poet_profile`、`user`（poet_verified/poet_profile_id）、`poet_draft`、`poet_suggestion`。

---

## 10. 搜索与运营域

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
  5. 建议：先查 Redis `search:suggestion:{kw}`（5min），未命中从 poem 标题+诗人名并集。
  6. 热门：读 Redis ZSet `search:hot` 逆序；空返回默认词。历史：`search:history:{userId}` List（remove+leftPush+trim(0,19)+30天过期），同时 `zset.incrementScore` 累加热度。
  7. `/api/search/poems` 走 `recommendationService.searchPoems`：本地（附 `recommendScore` 含兴趣加权+浏览/点赞对数分），不足时拉外部（author 含 keyword，`recommendScore=0.5`）合并降序。
- **关键类与方法**：`SmartSearchServiceImpl.*`、`SearchController.*`、`PinyinUtil.matchesPinyin`、`RecommendationServiceImpl.searchPoems/getExternalPoems/calculatePoemRecommendScore`；配置 `search.fulltext-enabled`（默认 true，全文已开启，可用环境变量 `SEARCH_FULLTEXT_ENABLED` 覆盖）、`search.pinyin.limit`。
- **数据与缓存**：Redis `search:suggestion:{kw}`、`search:hot`(ZSet)、`search:history:{userId}`(List)；`poem` 表全文索引 `ft_poem_title_content`（ngram）。

### 10.2 押韵/同韵字查询（C9）
- **触发入口**：`GET /api/rhyme/query?character=月`；`GET /api/rhyme/group?group=仄声`（`/api/rhyme/**` 已放行）。
- **分步流程**：`queryByCharacter` 取首字符按 `rhyme.character` 等值查、`sort_order` 升序；`queryByRhymeGroup` 按 `rhyme_group` 等值查整组。无 Redis，直接查库。
- **数据与缓存**：`rhyme`（character/rhyme_group/tone_type/rhyme_category/sort_order）。

### 10.3 浏览访问统计与热度累计（C10）
- **触发入口**：`GET /api/poems/{id}`（浏览量）；任意 `/api/**` 请求（访问日志切面）；运营看板 `AdminController`。
- **分步流程**：①详情 `getPoemDetail` 对 `view_count` SQL 原子自增；②`VisitLogAspect` 拦截 `/api/` 前缀（排除 `/admin/logs`、`/admin/stats`）记 `VisitLog`（getClientIp 取代理头、User-Agent、path、visitDate），落 `visit_log` 失败仅告警；③热度被推荐/热门/排序消费（`Math.log1p(viewCount)*0.1`）。
- **关键类与方法**：`PoemServiceImpl.getPoemDetail`、`VisitLogAspect.around/shouldLog/getClientIp`、`VisitLogServiceImpl`。
- **数据与缓存**：`poem.view_count/like_count`、`visit_log`。

---

## 11. AI 能力域

### 11.1 AI 辅助创作与生成（E1）
- **触发入口**：`POST /api/ai/chat|write-poem|analyze|couplet|ocr`。
- **分步流程**：`AiController` 分发 `AiService` → 各方法内置系统提示词常量 → 文本类 `AiModelRegistry.chat/getModel`、图片类 Base64 后 `AiModelRegistry.vision`；传 `moduleCode` 走 `chatByModule/visionByModule`（按模块选模型+覆盖提示词）→ 按 `model.provider` 定位 `AiModelAdapter`，`RestTemplate` 带 `Bearer apiKey` POST 到 `ai_model.api_url`（messages=system+user、max_tokens 默认 1024）→ 解析 `choices[0].message.content` 返回；失败抛 `BusinessException`。
- **注意（代码事实）**：providers 下拉含 qwen/openrouter，但当前**无对应 Adapter**，调用报"不支持的AI提供商"；`OpenAiCompatibleAdapter` 支持 zhipu/deepseek/kimi/mimo，`NvidiaAdapter` 支持 nvidia（vision 抛"暂不支持视觉模型"）。
- **关键类与方法**：`AiController.*`、`AiServiceImpl.chat/writePoemFromImage/analyzePoem/matchCouplet/ocrImage`、`AiModelRegistry.chat/vision/chatByModule/getModel`、`OpenAiCompatibleAdapter/NvidiaAdapter`。
- **数据与缓存**：`ai_model`（name/provider/apiUrl/modelId/visionModelId/apiKey/maxTokens/isEnabled/isDefault/modelType）；内存 `AiModelRegistry.modelCache`。

### 11.2 AI 诗人多轮对话（E2）
- **目标**：对诗中"AI 大学者"连续问答；前端维护会话，每次提问独立请求到无状态 chat 接口。
- **触发入口**：`views/poem/list.vue` AI 对话浮层（`toggleAiChat`），首次自动发"请简要介绍一下这首诗"。
- **分步流程**：校验登录 → `getAiModuleConfig('poetry_chat')` 拉模块配置 → 用户消息 push 进 `aiChatMessages`（前端内存维护多轮）→ `buildAiPrompt` 用 `aiConfig.promptTemplate` 替换 `{poetName}/{poemTitle}/{poemContent}/{maxLength}/{styleHint}`（首次 `firstResponseLength` 120、后续 `maxResponseLength` 300）→ 调 `chat({message,prompt moduleCode:'poetry_chat'})` → `AiModelRegistry.chatByModule` 用 `ai_module_config.prompt_template` 作为 system 注入 → 返回 reply push，滚动到底部。
- **关键类与方法**：前端 `poem/list.vue.buildAiPrompt/sendAiChatMessage/toggleAiChat`；后端 `AiController.chat/getModuleConfig`、`AiModelRegistry.chatByModule`、`AiModelConfigService.getModuleConfigByCode`。
- **数据与缓存**：`ai_module_config`（moduleCode/prompt_template/first_response_length/max_response_length/enable_markdown）；会话由前端内存数组维护（后端无状态）。

### 11.3 AI 内容生成与审核（E3）
- **触发入口**：`POST /api/ai/preview`（只生成不落库）、`POST /api/ai/fill-content`（生成存审核表）、`POST /api/ai/submit-review`、`GET /api/ai/fill-status/{targetType}/{targetId}`；审核 `GET /api/admin/ai-contents`、`PUT .../approve|reject`。
- **分步流程**：鉴权 → `validateField` 白名单（诗词 translation/appreciation/background；诗人 biography/life_story/influence/evaluation/anecdotes）→ 读取实体+朝代/诗人信息，`buildPoemPrompt/buildPoetPrompt` 拼提示词 → `generateContent` 先查同 id 待审记录（有则复用）→ `adapter.chat` 生成写入 `ai_generated_content`（status=0）→ 管理员 `approve` 置 status=1 并 `writeContentToPoem/Poet` 写回原字段；`reject` 置 status=2 留原因。
- **关键类与方法**：`AiContentController.*`、`AiGeneratedContentServiceImpl.generateContent/approve/reject/writeContentToPoem/writeContentToPoet`、`AiModelRegistry.getModelForModule`。
- **数据与缓存**：`ai_generated_content`（status 0待审/1通过/2拒绝）。

### 11.4 AI 模型配置加载（E4）
- **触发入口**：应用启动自动加载；`POST /api/admin/ai-models/refresh-cache` 手工刷新；`/api/admin/ai-models[/{id}]` CRUD。
- **分步流程**：密钥在 `secrets/application-secrets.yml` 配 `ai.providers.<provider>.api-key`，由 `AiProviderProperties`（`@ConfigurationProperties("ai.providers")`）承载 → `AiModelRegistry @PostConstruct init→refreshCache`：读 `ai_model` 全量，缓存启用项，对 api_key 空或 `your-` 前缀覆盖 `injectApiKey` → 同时读 `ai_module_config` 入模块缓存 → 运行时 `getModel/getDefaultModel/getModelByModuleCode/getModelForModule`；管理端变更后 `refreshCache()` 保一致；`test` 发送"你好，请回复ok确认连接正常"验证连通性。**适配器分派**：`OpenAiCompatibleAdapter.supports()` 现支持 `zhipu/deepseek/kimi/mimo/qwen/openrouter`（OpenAI 兼容协议），`NvidiaAdapter.supports()` 仅 `nvidia`。
- **关键类与方法**：`AiModelRegistry.refreshCache/injectApiKey/chatByModule`、`AiProviderProperties.getApiKey`、`AiModelConfigService.*`。
- **数据与缓存**：`ai_model`、`ai_module_config`；进程内 `ConcurrentHashMap`（非 Redis）；密钥来源 secrets.yml。

---

## 12. 论坛与社区域

### 12.1 发帖 / 回帖评论 / 点赞（E5）
- **触发入口**：`GET|POST /api/forum/posts`、`GET|PUT|DELETE /api/forum/posts/{id}`、`POST|GET /api/forum/posts/{id}/like`、`GET|POST /api/forum/comments`、`DELETE /api/forum/comments/{id}`、`POST|GET /api/forum/comments/{id}/like`。
- **分步流程**：列表按 status=1 过滤 + category/keyword，isTop 再 createTime 倒序分页，`fillUserInfo` 批量填充昵称/头像；详情 `view_count+1`；发帖写 title/content/category，状态直接可见；评论 `Comment.createComment` 写 comment（parentId 默认 0），`targetType==2`（POST）时更新帖子 `comment_count+1、last_comment_time`，删除回减；点赞 `toggleLike` 插入 `user_like`（唯一约束），撞 `DuplicateKeyException` 视为取消点赞并 `like_count-1`（>0 保护）。
- **关键类与方法**：`ForumController.*`、`ForumPostServiceImpl.*`、`CommentServiceImpl.createComment/deleteComment/toggleLike/getCommentsByTarget`；`TargetType`（POEM=1/POST=2/COMMENT=3）。
- **数据与缓存**：`forum_post`、`comment`、`user_like`。

### 12.2 内容审核流（E6）
- **触发入口**：`PUT /api/admin/poems/{id}/audit`、`/forum-posts/{id}/audit`、`/comments/{id}/audit`、`/ai-contents/{id}/approve|reject`、`GET /api/admin/audit/stats`。
- **分步流程**：各类内容创建进入待审态（`poem.audit_status=0`、评论 `status=0`、AI 内容 status=0）→ 运营后台列表筛选提交 status/reason → service 更新并记审计字段；AI 内容通过含写回原表（见 11.3）。`GET /api/admin/audit/stats` 聚合诗作/评论/诗人认证/AI内容/诗人草稿/投稿建议的待审数量。
- **待审对象速查**：
  | 对象 | 待审字段 | 审核接口 |
  |------|----------|----------|
  | 诗作 | `poem.audit_status=0` | `PUT /api/admin/poems/{id}/audit` |
  | 论坛帖子 | `forum_post.status` | `PUT /api/admin/forum-posts/{id}/audit` |
  | 评论 | `comment.status=0` | `PUT /api/admin/comments/{id}/audit` |
  | AI 内容 | `ai_generated_content.status=0` | `PUT /api/admin/ai-contents/{id}/approve|reject` |
  | 诗人认证 | `poet_profile.verified_status=0` | `PUT /api/admin/poet-profiles/{id}/verify` |
  | 诗人草稿 | `poet_draft.status=0` | `PUT /api/admin/poet-drafts/{id}/review` |
  | 投稿建议 | `poet_suggestion.status=pending` | `PUT /api/admin/poet-suggestions/{id}/review` |
- **关键类与方法**：`AdminController.auditPoem/auditForumPost/auditComment/getAuditStats`、`PoemServiceImpl.auditPoem/createPoemWithAudit`、`CommentServiceImpl.auditComment`。

---

## 13. 报修工单域

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

## 14. 内容管理/运营域

### 14.1 首页导航与静态页面（E8）
- **触发入口**：前台 `GET /api/home-navigation?type=`、`GET /api/static-pages/{pageKey}`；后台 CRUD。
- **分步流程**：前台按 `type`（可选）过滤 `status=1` 按 sortOrder 升序返回；静态页按 `pageKey+status=1` 取单条富文本；管理端 `/manage` 分页含停用项、`type` 分组，变更即时生效。
- **数据与缓存**：`home_navigation`、`static_page`。

### 14.2 诗话视野文章（E9）
- **触发入口**：`GET /api/vision/articles`（分页+category）、`/featured`、`/{id}`、`POST /{id}/like`。
- **分步流程**：列表 status=1 + category + sortOrder/时间倒序分页；推荐取 `isFeatured=1`；详情 `view_count+1`；点赞 `like_count+1`。
- **数据与缓存**：`vision_article`（status/is_featured/sort_order/view_count/like_count）。

### 14.3 精选诗人展示（见 9.3）
- 随机/列表/详情展示启用精选诗人，后台 CRUD 维护 sortOrder/status。

---

## 15. 前端数据流转层

### 15.1 统一请求层（K1）
- **目标**：为所有请求统一注入 token、处理响应码与 HTTP 错误。
- **分步流程**：`utils/request.ts` 创建 Axios 实例（`baseURL=VITE_API_BASE_URL||'/api'`、timeout 120000）→ 请求拦截器注入 `Authorization: Bearer <token>` → 响应 `code==='200'` 返回 `response.data`，否则 `ElMessage.error` 并 reject。**401 静默续期**：非公开接口（`PUBLIC_PATHS`）遇 HTTP 401 时，用本地 `refreshToken` 调 `/auth/refresh` 换取新一对 token → 写回 store/localStorage → **重放原请求**；续期失败或本身是 refresh 请求才 `logout()` 并跳 `/user/login`（`isRedirectingToLogin` 防重入）。导出 `get/post/put/del`。
- **关键位置**：`utils/request.ts`、`api/modules/*`。
- **数据与缓存**：请求头 `Authorization`。

### 15.2 登录态与用户 Store（K2）
- **分步流程**：`useUserStore` 初始化读 `localStorage` 中的 `token`/`refreshToken`，有则 `fetchUserInfo()`；`login/register` 写双 token 到 store + localStorage 后拉取用户；`fetchUserInfo` 读 `/users/me`，admin 写 `lastAdminUsername`，网络/5xx 指数重试≤2，401→尝试续期或 `logout()`；`logout` 清双 token 与 localStorage，并调用 `/auth/logout`（后端拉黑）。
- **关键位置**：`stores/user.ts`、`api/modules/user.ts`、`views/user/login.vue`、`router/guards.ts`。
- **数据与缓存**：localStorage `token`/`refreshToken`（双 token）、`lastAdminUsername`。

### 15.3 诗词页面 Store 数据流（K3）
- **分步流程**：`usePoemStore` 的 `fetchPoemList`（`GET /poems`）写 `poemList/total/currentPage/pageSize`；`fetchPoemDetail`（`GET /poems/:id`）合并 `isLiked/isFavorited` 到 `currentPoem`；`fetchModernPoems` 走 `/poems/modern/page`。诗人页无独立 Store，直接 `getPoetList/getPoetById`。
- **关键位置**：`stores/poem.ts`、`views/poem/{list,detail}.vue`、`views/poet/{list,detail}.vue`。

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
- `moyuan_cache_external_poem_<title>`（外部诗词详情，24h）
- `moyuan_cache_<自定义>`（外部搜索结果缓存）
- **收藏/AI对话/浏览历史**：无 localStorage key，分别走服务端接口/内存/API。
- **关键位置**：`utils/storage.ts`、`stores/preferences.ts/searchHistory.ts/formDraft.ts/apiCache.ts`。

### 15.5 首页数据组装（K5）
- **轮播图**：静态 `/img/lb_*.jpg`、`/img/lb_shiwen_*.png`，无后端。
- **今日诗词**：`DailyPoetry` → `v2.jinrishici.com/one.json`（独立 Axios 实例）。
- **首页导航**：`GET /home-navigation`。
- **精选诗人**：`getRandomPoetFeatured(8)`，去重取 4 位，每 20s 轮换。
- **论坛预览**：`GET /forum/posts?pageSize=5`。
- **古诗精赏**：本地 json + `GET /search/poems` + `/search/poems/recommended`。
- **当代精选**：本地 json + like/favorite/comment 接口。
- **AI 区块**：`/ai/write-poem`、`/ai/analyze`、`/ai/couplet`、`/ai/ocr`、`/rhyme/*`；"诗海拾贝"本地 json。
- **诗话视野**：`GET /vision/articles`。
- **关键位置**：`views/home/index.vue`、`components/{home,business}/*`、`api/modules/*`。

### 15.6 诗云 3D 星空（K6）
- **数据加载**：`Promise.all` 并发 `GET /dynasties`、`/poets(pageSize 300)`、`/poems(pageSize 1000)`，失败回退内置 12 朝代。
- **渲染**：`initScene`（Scene+FogExp2+Camera+WebGLRenderer(ACESFilmicToneMapping)+OrbitControls）→ 3200 背景星（Sphere+ShaderMaterial）→ 诗人星（发光小球+光晕，缓存 `poetPositionMap`）+ 同朝代贝塞尔赠诗线 → 诗作独立 Mesh 以「诗人为中心、诗作环绕分布」的球面螺旋生成并经松弛迭代拉开间距，按诗人分组的 LineSegments2 连线 → 交互：hover 高亮并显示 DOM 提示框、点击 `selectPoet→flyToStar+loadPoetPoems+focusPoetPoems`、点击诗作 `selectPoem`、朝代筛选重建。
- **配色实验室**：底部控制模块由 `reactive(viz)` 驱动（7 套方案 + 光晕/连线/尺寸/亮度滑杆 + 标签与朝代配色开关），`applyVizStyles()` 统一重涂背景/雾效、诗人节点、诗作节点与两类连线；整体亮度经画布容器 CSS `filter: brightness()`。
- **诗人姓名标注**：DOM 层 `.label-layer` + `updatePoetLabels()`，每帧 `getWorldPosition().project(camera)` 更新标签屏幕坐标，相机背后/视口外自动隐藏。
- **卸载**：`onUnmounted` 释放 geometry/material、清理标签与定时器、`scene.clear()`、`renderer.dispose()`。
- **数据与缓存**：数据仅内存 ref，不做 localStorage；`poetPositionMap` 内存缓存 3D 坐标，`poemCountMap` 缓存每位诗人的诗作数（用于节点尺寸）。

### 15.7 路由守卫（K7）
- **分步流程**：`beforeEach` 按 `meta.title` 设 document.title → `requiresAuth` 未登录跳 `Login`（带 redirect）；已登录缺 userInfo 先 `fetchUserInfo`；`requiresAdmin` 校验 role !== admin 弹错回 Home；已登录访问 Login/Register 回 Home。`afterEach` 维护 `appStore.cachedViews`（keep-alive）。
- **关键位置**：`router/guards.ts`、`router/index.ts`（`meta`: requiresAuth/requiresAdmin/keepAlive）、`stores/app.ts`。

---

## 16. 数据初始化与种子流

- **库初始化**：`init.sql`（建表 + 基础种子数据）→ `seed.sql`（补充数据，`INSERT IGNORE` 幂等）→ 可选 `update-ai-models.sql`。
- **脚本位置**：`sc-moyuan-backend/src/main/resources/db/`。
- **配置键**：外部诗人数据源 `apihz.id/key`（环境变量 `APIHZ_ID`/`APIHZ_KEY`）；AI 各供应商 `ai.providers.<provider>.api-key`（secrets.yml）。
- **表/字段约定**：库 `moyuan`、utf8mb4；逻辑删除 `deleted`；时间字段自动填充。

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

**维护人员**：墨渊开发团队
**维护说明**：本文为 docs 本地维护文件，受 `.git/info/exclude` 排除不提交远程；代码变更后请依据 doc-manager 技能核对本文与实现一致性。