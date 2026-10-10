# 系统已知缺口待办清单

> 本文件集中登记项目运行过程中发现的"已知缺口"（未实现、不完整、或存在但调用会失败的功能），作为待办与排期参考。
> 信息依据：实际源码核验（`sc-moyuan-backend/src/main/java/com/moyuan/`、`frontend/src/`），每项均标注涉及文件、影响与建议方案。
> 维护说明：docs 本地维护文件，受 `.git/info/exclude` 排除不提交远程。

## 清单速览

| # | 缺口 | 优先级 | 状态 | 域 |
|---|------|--------|------|----|
| 1 | qwen（千问）缺少 AI 适配器 | 高 | ✅ 已落地 | AI |
| 2 | OpenRouter 缺少 AI 适配器（8 个免费档不可用） | 高 | ✅ 已落地 | AI |
| 3 | NvidiaAdapter 不支持视觉模型 | 中 | ✅ 流程完善 | AI |
| 4 | JWT 无刷新 / 登出失效机制 | 中 | ✅ 已落地 | 安全 |
| 5 | 方法级权限控制未实际使用 | 低 | 待办 | 安全 |
| 6 | 文件上传缺防盗链与存储限额校验 | 低 | 待办 | 文件/安全 |
| 7 | 搜索基于数据库 LIKE，不具备分析能力 | 低 | ✅ 方案A已落地（默认开启） | 搜索 |
| 8 | PoetDefaultAvatar 流派维度（style）未实现 | 低 | ✅ 已落地 | 诗人 |

---

## AI 适配器缺口（1–3）

### 1. qwen（千问）缺少 AI 适配器 ⚠ 高
- **现象**：`ai_model` 表中已有 `qwen36plus` 记录（api_url `https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions`，默认禁用），管理端与前端下拉均列出 `qwen → 千问`；但代码中**没有任何 Adapter 支持 `qwen` 供应商**。
- **影响**：一旦启用任意 qwen 模型，调用即抛 `BusinessException("不支持的AI提供商: qwen")`，AI 功能不可用。
- **根因**：`AiModelRegistry.getAdapter` 通过 `adapter.supports(provider)` 过滤，而现存的 `OpenAiCompatibleAdapter`（zhipu/deepseek/kimi/mimo）与 `NvidiaAdapter`（nvidia）均不含 qwen。
- **建议方案**：qwen 仅需在 `OpenAiCompatibleAdapter.supports()` 中加入 `"qwen"` 即可（其 api_url 已是 OpenAPI 兼容格式）；同时确认 `secrets/application-secrets.yml` 中已配置 `ai.providers.qwen.api-key`。
- **状态 ✅ 已落地（2026-09-05）**：`OpenAiCompatibleAdapter.supports()` 已加入 `"qwen"`。
- **涉及文件**：`ai/OpenAiCompatibleAdapter.java`、`ai/AiModelRegistry.java`、`config/AiProviderProperties.java`、`resources/db/init.sql`（qwen36plus 记录）。

### 2. OpenRouter 缺少 AI 适配器 ⚠ 高
- **现象**：`init.sql` 录入 **8 个 OpenRouter 免费档模型**（`free-deepseek-v4`、`free-kimi-k26`、`free-gemma4`、`free-qwen3-next`、`free-qwen3-coder`、`free-minimax-m25`、`free-llama4-scout`、`free-minimax-m27`），其中 `free-minimax-m27` 已启用（`isEnabled=1`）、文档标注"MiniMax-M2.7 实测可用"；但代码同样**没有任何 Adapter 支持 `openrouter`**。
- **影响**：`free-minimax-m27` 虽已启用，但调用仍抛"不支持的AI提供商: openrouter"，该"实测可用"模型实际 100% 失败。
- **根因**：同 1，`supports()` 未包含 `openrouter`。
- **建议方案**：在 `OpenAiCompatibleAdapter.supports()` 增加 `"openrouter"`（其 api_url 为 `https://openrouter.ai/api/v1/chat/completions`，OpenAI 兼容）；核对 `ai.providers.openrouter.api-key` 已配置。
- **状态 ✅ 已落地（2026-09-05）**：`OpenAiCompatibleAdapter.supports()` 已加入 `"openrouter"`。
- **涉及文件**：`ai/OpenAiCompatibleAdapter.java`、`ai/AiModelRegistry.java`、`config/AiProviderProperties.java`、`resources/db/init.sql`（free-* 记录）、`docs/guides/development-guide.md`（OpenRouter 免费模型清单）。

### 3. NvidiaAdapter 不支持视觉模型（vision） 中
- **现象**：`NvidiaAdapter.chat` 可用（nvidia），但 `vision` 直接抛 `BusinessException("NVIDIA NIM暂不支持视觉模型")`。
- **影响**：选 Nvidia 模型做"看图写诗/图片识别/OCR"（`visionModel`）会失败；仅文本场景可用。
- **建议方案**：接入 NVIDIA NIM 的视觉端点（multi-modal / `nvidia/vlm` 类模型），或在下拉中屏蔽 Nvidia 的视觉选项，避免误选。
- **状态 ✅ 流程完善（2026-09-05）**：当前无合适的 Nvidia 视觉模型，`NvidiaAdapter.vision` 已改为抛出可操作错误提示（建议回退到智谱/千问视觉模型），不再含糊。
- **涉及文件**：`ai/NvidiaAdapter.java`。

---

## 安全与权限缺口（4–6）

### 4. JWT 无刷新 / 登出失效机制 中
- **现象**：登录/注册签发无过期时间的刷新 token；登出仅前端清除 `localStorage.token`，**后端未校验 token 失败清单（黑名单）/登出失效**，`JwtUtil.validateToken` 只做验签+过期判断。
- **影响**：token 一旦泄露，在 `jwt.expiration` 内始终有效；无法主动吊销（如改密/封禁后旧 token 仍可用）。
- **建议方案**：引入 refresh token 轮换；登出/改密/封禁时将旧 token 加入 Redis 黑名单（`jwt:blacklist:{jti}`）并在过滤器校验。
- **状态 ✅ 已落地（2026-09-05）**：
  - 双 token（access/refresh），refresh token 7 天、轮换式签发，消费即作废旧 refresh token；
  - 登出用 `TokenBlacklistService`（Redis + 内存兜底）将 access token 的 jti 加入黑名单，`JwtAuthenticationFilter` 校验黑名单；
  - JWT 带 jti 与 type 声明，access/refresh 互防误用；
  - 前端 `request.ts` 401 时基于 refresh token 静默续期并重放原请求。
- **涉及文件**：`util/JwtUtil.java`、`service/TokenBlacklistService.java`、`security/JwtAuthenticationFilter.java`、`controller/AuthController.java`、`service/impl/UserServiceImpl.java`、`resources/application.yml`（jwt.refresh-expiration）、`frontend/src/utils/request.ts`、`frontend/src/stores/user.ts`。

### 5. 方法级权限控制未实际使用 低
- **现象**：`SecurityConfig` 已加 `@EnableMethodSecurity`，但各 Controller **未使用 `@PreAuthorize` 等方法级注解**，权限完全依赖 URL 规则（`/api/admin/**` + `hasRole`）。
- **影响**：接口级权责划分不够细，归档到非 `/api/admin/**` 但需管理员限定的操作只能额外走 URL 规则，易遗漏。
- **建议方案**：对涉及管理员/本人校验的接口补 `@PreAuthorize`，与 URL 规则互补。
- **涉及文件**：`config/SecurityConfig.java`、各 Controller。

### 6. 文件上传缺防盗链与存储限额校验 低
- **现象**：`FileStorageServiceImpl.upload` 未做 referer 防盗链、单用户存储总量/配额校验。
- **影响**：`/uploads/**` 公开访问，存在被外部站引用带宽盗刷、单用户无限占用的风险。
- **建议方案**：为 `/uploads/**` 增加校验/限流；后台配置用户级配额。
- **涉及文件**：`service/impl/FileStorageServiceImpl.java`、`config/SecurityConfig.java`（/uploads 放行处）。

---

## 搜索能力缺口（7）

### 7. 搜索基于数据库 LIKE，不具备分析能力 低（需求评估）
- **现象**：`SmartSearchServiceImpl` 用 SQL `LIKE` + 打分实现搜索（`frontend/ELASTICSEARCH_INTEGRATION.md` 亦注明：不支持分词、相关性排序、高亮、模糊匹配）。循环、拼音匹配为补充手段。
- **影响**：长文本/复杂查询体验一般，无拼写容错与精确相关性排序。
- **建议方案**：评估接入 Elasticsearch（仓库已有 `frontend/ELASTICSEARCH_INTEGRATION.md` 规划文档），或保持现状仅作增量优化（例如索引/停用词/同义词）。
- **状态 ✅ 方案A已落地并默认开启（2026-09-05）**：采用 MySQL 8 ngram 中文全文索引（方案A，详见 `docs/guides/search-improvement-design.md`）。后端 `search.fulltext-enabled` 已在 `application.yml` 默认置 `true`（全文检索开启），`smartSearch` 对 keyword 走 `MATCH(...) AGAINST(...)` 全文检索，中文分词 + 相关度更高；可用环境变量 `SEARCH_FULLTEXT_ENABLED` 关闭回退 LIKE。前端 `<mark>` 高亮为后续可选增强。
- **涉及文件**：`service/impl/SmartSearchServiceImpl.java`、`resources/application.yml`（search.fulltext-enabled）、`resources/db/init.sql`（poem 全文索引 ngram）、`docs/database/migration_004_poem_search_fulltext.sql`、`frontend/ELASTICSEARCH_INTEGRATION.md`。

---

## 诗人头像域缺口（8）

### 8. PoetDefaultAvatar 流派维度（style）未实现 低
- **现象**：`PoetDefaultAvatar.getAvatar` 第 0 级"按流派精确匹配"代码被注释跳过，注释为"暂不支持，因为 Poet 没有 style 字段"；当前仅按「地区_时期_性别」匹配。
- **影响**：同一分组内头像区分度有限（如豪放/婉约不分）；但功能可用，非阻塞。
- **建议方案**：若需按流派更精准分配，需先给 `poet` 增加 `style` 字段并用流数据填充后启用第 0 级逻辑。
- **状态 ✅ 已落地（2026-09-05）**：`poet` 表 + `Poet` 实体新增 `style` 字段（VARCHAR(32)）；`PoetDefaultAvatar.getAvatar()` 恢复第 0 级流派匹配（`STYLE_MAP` 命中则优先，否则回退「地区_时期_性别」）。详见 `docs/guides/poet-avatar-style-design.md`。`style` 当下优先依赖 `seed.sql` 标注或外部 API 增量填充，外部诗人 API 暂无流派字段。
- **涉及文件**：`util/PoetDefaultAvatar.java`、`entity/Poet.java`、`resources/db/init.sql`、`docs/database/migration_003_poet_style.sql`。

---

## 说明与建议

- **优先级参考**：#1、#2（AI 适配器）影响实际模型使用且 init.sql 已预置数据，建议优先处理；#3 影响视觉场景；#4 属安全加固建议。
- **修复建议核对**：修改适配器后，可用管理端 `POST /api/admin/ai-models/test` 做连通性自检。
- **代码变更后**：请依据 doc-manager 技能同步核对 `docs/business/system-workflow.md`（AI 能力域）、`docs/guides/development-guide.md`（模型清单）等文档。