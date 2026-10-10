# 搜索能力提升设计方案

> 对应"已知缺口" #7：搜索基于 DB LIKE（无分词 / 相关排序 / 高亮）
> 优先级：低 | 状态：✅ 方案A已落地并默认开启（2026-09-05）
> 编制日期：2026-09-05 | 最后更新：2026-09-05

## 1. 现状分析

### 当前实现（`SmartSearchServiceImpl`）
- 检索入口：`/api/search/smart`（`SearchController` -> `SmartSearchServiceImpl.smartSearch()`）
- 检索方式：对 `poem.title` / `poem.content` / `poet.name` 使用 MyBatis-Plus `LIKE` 匹配（`searchPoems` / `searchByPoetName`）
- 拼音补充：`searchByPinyin` 使用 `PinyinUtil.matchesPinyin` 做拼音联想（受 `search.pinyin.limit` 限制）
- 排序：内存中 Java 评分（`calculateScore`）——标题完全/包含、拆分关键词、诗人名精确/包含，但：
  - **无真正的分词**：仅按标点 `[\p{P}\s]+` 切分，二次拆关键词（长度≥2），不是中文语义分词
  - **无相关排序（BM25 等）**：评分是简单启发式权重，不够稳定
  - **无结果高亮**：前端 `views/poem/list.vue` 仅展示 `searchLevel` 标签，无关键词 `<mark>` 高亮片段
- 建议（`getSearchSuggestions`）：基于 LIKE + Redis 5 分钟缓存
- 无关键词时为纯条件筛选（`searchWithoutKeyword`）+ 排序（`applySort`）

### 与大模型生成用法
`PoemServiceImpl` 或相关对话流程可能用搜索兜底，但搜索整体低频、数据量为诗词（预计 < 10 万条量级），属"够用但体验粗糙"状态。

## 2. 目标
1. 支持真正的**中文分词**，避免"床前明月光/床前"跨词漏检
2. 提供稳定**相关度排序**（而非仅简单权重累加）
3. 前端返回**高亮片段**（关键词 `<mark>` 包裹）
4. 不破坏现有拼音 / 条件筛选 / 热度排序能力

## 3. 方案选型（二选一）

### 方案 A：MySQL 全文索引（ngram）增强（✅ 推荐先做）

**思路**：借助 MySQL 8 自带 ngram 全文解析器实现中文分词，全文索引替代/补充 LIKE；相关度使用系统自带相关性评分（`MATCH ... AGAINST`）；高亮由后端计算返回片段。**不引入任何新组件**，改动集中在后端 SQL 与前端渲染。

**优势**
- 零新增依赖，MySQL 8 原生支持，运维负担最小
- 数据量 < 10 万级下检索性能与体验足够
- 与项目"技术上克制、避免过度设计"风格一致

**代价**
- ngram 属于"token 化分词"，对漫游长词联想不如专业分词（如 ik）精细，但古诗场景可接受
- 评分依赖默认相关性，需辅以现有 Java 评分做二次加权排序

**实施步骤**
1. **迁移脚本**（`docs/database/` + `init.sql` 同步）：
   - `NGram` 全文索引：`ALTER TABLE poem ADD FULLTEXT INDEX ftx_poem_title_content (title, content) WITH PARSER ngram`（ngram_token_size=2 默认）
2. **SmartSearchServiceImpl**：
   - 优先走 `MATCH(title,content) AGAINST('关键词' IN NATURAL LANGUAGE MODE)` 加条件 + 分页
   - 精选结果保留现有 `calculateScore` 评分做相关性二次加权（全文评分为主、Java 分做 tie-break）
   - 对命中记录计算**高亮片段**：从 title/content 提取含关键词的窗口片段，字段 `highlight` 返回
   - 返回结构新增 `highlight`（`{title?: string, content?: string}` 或前缀段落）
3. **search.suggestions**：保持 LIKE + Redis 缓存不变（够用）
4. **前端**：
   - `api/modules/search.ts`：`SearchResult` 增加 `highlight` 字段
   - `views/poem/list.vue`：标题/内容渲染 `<mark>` 高亮标签；无 keyword 或不命中时高亮为空

**升级/回退**：SQL 可局部开关（`search.use-fulltext: true/false`），false 时回落 LIKE 逻辑，低风险。

### 方案 B：引入 Elasticsearch（当数据量大时再选）

**思路**：引入轻量 ES 8（single-node）+ ik 中文分词，`poem` / `poet` 全量同步双写，用 BM25 + highlight 做检索。

**优势**：真正语义分词、BM25 稳定排序、官方 highlight、未来可扩展
**代价**：新增独立服务、同步/一致性处理、运维与部署复杂度显著上升（与 #5/#6 类低优先缺口相比性价比低）

**结论：当前阶段不选，仅作为数据量增长后的演进路径。**

## 4. 推荐方案确定：方案 A

| 项目 | 说明 |
|------|------|
| 分词 | MySQL ngram（token_size=2）|
| 排序 | 全文相关性评分为主 + 现有 Java 评分 tie-break |
| 高亮 | 后端提取命中片段 + 前端 `<mark>` 渲染 |
| 兼容 | 保留拼音匹配、条件筛选、热度/点赞排序 |
| 风险开关 | `search.fulltext-enabled` 配置项支持回退 |

## 5. 文件影响范围（实施时核对）
- `docs/database/migration_004_poem_search_fulltext.sql`（新增，ngram 全文索引重建）
- `sc-moyuan-backend/src/main/resources/application.yml`（search.fulltext-enabled，默认 true）
- `sc-moyuan-backend/src/main/java/com/moyuan/service/impl/SmartSearchServiceImpl.java`
- `sc-moyuan-backend/src/main/resources/db/init.sql`（poem 全文索引改 ngram）
- `docs/database/schema.md`（poem 全文索引说明）

## 6. 实施情况（2026-09-05）
- ✅ 后端全文检索路径已实现并默认开启：`search.fulltext-enabled` 默认 `true`（2026-09-05 已由 false 改为 true），`searchPoems` 走 `MATCH(title,content) AGAINST(... IN NATURAL LANGUAGE MODE)`，命中集参与现有词频评分排序、去重、分页；置 `false` 时可回退原 LIKE 行为（可用环境变量 `SEARCH_FULLTEXT_ENABLED` 覆盖）。
- ✅ `poem.title/content` 全文索引统一改为 ngram（token_size=2），迁移与 init.sql 已同步。
- ⏳ **前端 `<mark>` 高亮为可选增强**：本次未接入（结果渲染经由 poemStore/列表组件、需跨组件传 keyword，改动面较大、回归风险偏高；且不是本次确认的硬性验收点）。如需，按下节接入。

## 7. 待用户确认 / 后续可选
- [ ] 未选「前端高亮」：如需要，在列表组件基于 `filters.keyword` 对 title/content 包 `<mark>`（`search.ts` 类型无需改，前端本地高亮即可）。
- [x] 生产开启全文（2026-09-05）：`application.yml` 的 `search.fulltext-enabled` 已置 `true`（默认值，可用 `SEARCH_FULLTEXT_ENABLED` 覆盖），本地库已执行 `migration_004`（poem 表 ngram 全文索引）。