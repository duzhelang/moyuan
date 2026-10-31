# 墨渊知识问答接入方案（复用 AI 小窗 · FastAPI + Vue）

> 定位：这是墨渊平台自身的知识问答能力建设，与《智能生成系统开发》期末作业无关。
> 服务代码在 `rag-service/`，前端改动在 `frontend/`，**不新建页面**，复用现有全站 AI 小窗。

## 0. 结论先行

复用现有 AI 小窗的「网页用法助理」，把它的回答链路从
「前端关键词检索 + Spring `/api/ai/chat`」
升级为
「FastAPI 知识问答服务（混合检索 + 本地模型）」
服务不可用时自动回退原链路。页面、路由、窗口交互均不改，只改 `aiChat.ts` 的一条分支、加一个 API 模块和 Vite 代理。

本期模型策略：
- **嵌入默认且唯一实测**：`bge-small-zh-v1.5`
- **生成**：`ollama` Python 客户端，默认 `qwen3.5:4b`，备用 `maternion/minicpm5:2b-q8_0`
- **其他嵌入模型**（`jina-clip-v2`、`qwen3-embedding:0.6b`）只保留注册接口与独立索引机制，**不建索引、不实测**

## 1. 现状盘点

| 项 | 现状 |
|---|---|
| 小窗组件 | `frontend/src/components/common/AiChatWindow.vue`（全局单例，跨路由不销毁） |
| 会话逻辑 | `frontend/src/stores/aiChat.ts`，两个助理：诗词助理 / 网页用法助理 |
| 网页用法助理链路 | 前端 `systemKnowledgeSearch.ts` 关键词打分 → 拼接资料 → `POST /api/ai/chat`（moduleCode=`system_chat`） |
| 知识来源 | MySQL `system_knowledge` 24 条，经 `GET /api/system-knowledge` 下发到前端 |
| 已有 RAG 服务 | `rag-service/`：语料导出、索引构建、混合检索、评测、CLI、标准库 HTTP |
| 已知问题 | LlamaIndex 0.14 的 `Ollama.stream_chat()` 必须传 `ChatMessage` 对象，传 dict 会报 `'dict' object has no attribute 'role'`；本期生成侧改用 `ollama` 客户端可彻底绕开 |

## 2. 本地模型清单（按 `D:\BIG` 实际目录核对）

### 2.1 嵌入模型

| key | 实际路径 | 维度 | 体积/状态 | 本期策略 |
|---|---|---|---|---|
| `bge-small` | `D:\BIG\huggingface\hub\models--BAAI--bge-small-zh-v1.5` | 512 | ~95MB，完整可用 | **默认，唯一实测并建索引** |
| `jina-clip` | `D:\BIG\jina-clip-v2` | 1024 | 1.73GB，完整；`custom_st.py` 需 `trust_remote_code=True` | 已装好，**只留接口，不建索引、不实测** |
| `qwen3-embed` | Ollama `qwen3-embedding:0.6b` | 1024 | 639MB，可用 | **只留接口，不建索引、不实测** |

> 说明：你本地安装的是 `jina-clip-v2`（`D:\BIG\jina-clip-v2`），与 HF 缓存里的 `jina-embeddings-v3` 不是同一个模型。
> HF 缓存中的 `jina-embeddings-v3` 只有约 16MB（权重缺失），本方案不纳入。

### 2.2 生成模型

| key | 模型 | 体积 | 本期策略 |
|---|---|---|---|
| `qwen3.5` | `qwen3.5:4b` | 3.4GB | **默认，实测** |
| `minicpm5` | `maternion/minicpm5:2b-q8_0` | 2.7GB | 备用，已实测可切换 |

生成侧统一使用 **`ollama` Python 客户端**（`from ollama import Client; client.chat(...)`），不再走 `llama_index.llms.ollama`，避免 `ChatMessage` 类型兼容问题，也少一层抽象。

### 2.3 重排模型

`bge-reranker-base`（`D:\BIG\huggingface\hub\models--BAAI--bge-reranker-base`），继续使用。

### 2.4 推荐组合

- **本期落地**：`bge-small` + `qwen3.5:4b` + `bge-reranker-base`
- **备用生成**：`maternion/minicpm5:2b-q8_0`（实测 22.3 秒，速度约为主模型一半）
- **接口预留**：`jina-clip`、`qwen3-embed`（需要时补建对应索引即可启用）

> 不同嵌入模型维度不同（512 / 1024），必须各自建索引；本期只落地 512 维的 `bge-small`，其他维度仅保留目录与校验逻辑。

## 3. 依赖建议（不受期末约束）

| 用途 | 建议 | 说明 |
|---|---|---|
| 接口 | `fastapi` + `uvicorn` | 前后端分离接口层 |
| SSE | FastAPI `StreamingResponse` | 不额外引入 `sse-starlette` |
| 生成 | **`ollama` Python 客户端** | 直接调用本地 Ollama，统一模型切换 |
| 嵌入 | `sentence-transformers` + `torch` + `transformers` | 已安装；`bge-small` 直接加载，jina-clip 预留 `trust_remote_code` 入口 |
| 向量 | `numpy`（当前 200+ 块够用） | 后续语料变大再换 FAISS |
| 关键词 | `jieba` + `rank_bm25` | 已安装 |
| 校验 | `pydantic v2` | FastAPI 自带 |

LlamaIndex 不再是硬性依赖：检索层保留现有代码即可；生成层完全改为 `ollama` 客户端。后续可逐步去掉 LlamaIndex。

## 4. 总体架构

```text
Vue 全站 AI 小窗（AiChatWindow.vue，不改结构和路由）
        │  网页用法助理
        ▼
frontend/src/stores/aiChat.ts  answerBySystem()
        │  ① 优先：POST /rag/ask/stream（SSE）
        │  ② 失败：回退原「关键词检索 + /api/ai/chat」
        ▼
FastAPI（rag-service/app.py，127.0.0.1:8010）
        │
        ├─ RagEngine：子问题拆分 → 向量+BM25 → RRF → 重排 → 邻居扩展 → 三文件覆盖
        ├─ 索引：data/index/<embed_key>/（当前只有 bge-small）
        └─ 生成：ollama 客户端 → qwen3.5:4b / minicpm5:2b
```

第一版 **Spring Boot 不参与**，前端由 Vite 代理直达 FastAPI；生产环境再由 Nginx 或 Spring 网关转发，并加鉴权。

## 5. 接口契约

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/rag/health` | 返回服务状态、当前索引块数、默认模型 |
| GET | `/rag/models` | 返回嵌入/生成模型清单；`bge-small` 为 `ready`，其余为 `reserved` |
| POST | `/rag/ask` | 同步回答：`{question, embed, model, top_k, rerank}` |
| POST | `/rag/ask/stream` | SSE 流式：先 `meta`（来源），再 `delta`，最后 `done` |

`embed` 默认 `bge-small`；请求其他嵌入模型时，若索引不存在直接返回“模型已注册但未建索引”，不回退、不自动构建。

SSE 事件格式：

```text
event: meta
data: {"sources":[{"file":"moyuan_business_modules.md","section":"16. 诗词评分模块 / API 接口"}]}

event: delta
data: {"text":"平台有两种评分方式……"}

event: done
data: {"elapsed":3.2}
```

## 6. 前端接入改造（不新建页面）

1. `frontend/vite.config.ts`：新增 `/rag` 代理 → `http://127.0.0.1:8010`（独立前缀，避开现有 `/api` → 8085）。
2. 新增 `frontend/src/api/modules/rag.ts`：用 `fetch` + `ReadableStream` 解析 SSE（Axios 不适合流式），并封装健康探测与配置读写。
3. 改 `aiChat.ts` 的 `answerBySystem()`：
   - 先做一次 `/rag/health` 探测（结果缓存 30 秒）；
   - 可用则走流式，边收边写入当前 assistant 消息；
   - 不可用/超时/报错则回退原链路；
   - RAG 回答不写会话缓存（知识库更新后避免旧答案），回退链路保持原缓存逻辑。
4. 来源展示：回答末尾追加 `（来源：文件名·章节）`；如知识条目带 `route`，沿用现有 `[[/route]]` 链接语法。
5. 窗口 UI 不改；模型选择放在服务端配置，暂不做前端下拉。

## 7. 语料与索引

- 语料：`docs/business/modules.md`、`docs/business/system-workflow.md`、`system_knowledge` 24 条。
- `system_knowledge` 第一版从 `init.sql` 导出；第二版改从 `GET /api/system-knowledge` 或 MySQL 直读，保证后台改动能生效。
- 索引目录：`rag-service/data/index/<embed_key>/`，`meta.json` 记录维度、块数、模型路径。
- 本期只构建：`data/index/bge-small/`。
- 预留构建命令：`python build_index.py --embed jina-clip`、`--embed qwen3-embed`，但本期不执行、不验证。
- 查询时若索引不存在或维度不匹配，返回明确提示，不自动重建。

## 8. 分阶段任务（约 2~2.5 天）

| 阶段 | 内容 | 工时 |
|---|---|---|
| M1 | 模型注册表（bge-small 实测；jina-clip/qwen3-embed 标记 reserved）、多索引目录、维度校验；只重建 bge-small 索引 | 0.5 天 |
| M2 | FastAPI + SSE + 错误处理；`ollama` 客户端接入；curl 验证健康/模型/问答接口 | 0.5 天 |
| M3 | 小窗接入：`answerBySystem` 分支、SSE 渲染、来源展示、失败回退 | 0.5 天 |
| M4 | 联调：8 题回归、`qwen3.5` 与 `minicpm5` 对比、超时与缓存策略 | 0.5 天 |
| M5（可选） | Spring/Nginx 转发、鉴权、部署说明 | 0.5 天 |

**当前进度（2026-11-01）**：M1、M2、M3 已完成——小窗已改用 `POST /rag/ask/stream` 流式渲染（首个 `delta` 到达时插入占位消息、后续原地追加，最终答案覆盖并补来源），`meta` 增加 `refused` 字段用于拒答判定与回退；SSE 实测事件序列 `meta → delta×248 → done`，来源 12 条。

M4 已完成生成侧对比（同一评分题）：

| 生成模型 | 耗时 | 质量 | 结论 |
|---|---|---|---|
| `qwen3.5:4b` | 44.9 秒 | 5 个评分接口齐全，分条清晰 | 保持默认 |
| `maternion/minicpm5:2b-q8_0` | 22.3 秒 | 两种评分方式与范围准确，接口列 2 个 | 速度敏感时后台可切 |

- 修复备用模型名：登记为 `maternion/minicpm5:2b-q8_0`（原短名 `minicpm5:2b-q8_0` 在 Ollama 中不存在，调用会失败）。
- 资料块标题去掉“资料 N”编号，改为 `【文件名 · 章节】`，并在系统提示中要求引用写“文件名·章节标题”；复测回答已输出可读来源。
- 检索评测 8 题仍为要点覆盖 100%、来源覆盖 100%。

M5 未开始。

## 9. 验收标准

- 小窗外观、入口、路由不变，无新增页面。
- 8 个跨三文件问题：要点覆盖 100%、来源覆盖 100%（当前已达标）。
- 原失败问题能答出：两种评分方式（用户评分 + AI 评分）、范围 1.0-5.0、四个接口、入口 `/poem/:id`。
- 问答接口首 token 在本机 4B 模型下 < 5 秒；服务停机时前端自动回退且提示清晰。
- `bge-small` 索引默认可用；`jina-clip` / `qwen3-embed` 仅验证“已注册但未建索引”的提示，不做实际检索质量测试。

## 10. 风险与对策

| 风险 | 对策 |
|---|---|
| `jina-clip-v2` 1.7GB，CPU 慢、4GB 显存紧张 | 本期不启用，只保留接口；默认 `bge-small` |
| 其他嵌入模型维度不同（1024） | 独立索引目录 + 维度校验；未建索引直接报错 |
| SSE 经代理被缓冲 | Vite 代理保持直连；生产 Nginx 设 `proxy_buffering off` |
| 8010 端口无鉴权 | 只监听 `127.0.0.1`；对外统一走 Spring/Nginx 并校验 JWT |
| 知识库在后台变更 | 第二版从数据库拉取并支持重建；首版接受手动重建 |
| `ollama` 模型未启动/不存在 | 启动时校验 `/rag/health`，失败返回明确错误并让前端回退 |

## 11. 暂不做

- 不新建独立问答页面、不加导航入口。
- 不实测 `jina-clip-v2`、`qwen3-embedding`，只保留注册与索引目录机制。
- 不做图片检索（jina-clip 的图片能力后置）。
- 不做多轮会话记忆（小窗仍按单轮问答）；第二版规划见第 13 节，仅留存备份，本期不实现。
- 不做模型前端切换、自动增量索引、Spring Boot 内嵌部署。

## 12. 生成侧选型说明：ollama 客户端 vs LlamaIndex（备份留存）

本期生成侧已改为直接使用 `ollama` Python 客户端（`from ollama import Client; client.chat(...)`），**不再经过 `llama_index.llms.ollama`**。
嵌入、切分、向量检索仍由 LlamaIndex 提供（`HuggingFaceEmbedding`、`SentenceSplitter`），只是把「最后一步生成」从框架里拆出来。

### 12.1 收益

| 项 | 说明 |
|---|---|
| 少一层兼容坑 | LlamaIndex 0.14 的 `Ollama.stream_chat()` 强制要求 `ChatMessage` 对象，传 dict 报 `'dict' object has no attribute 'role'`；直连客户端直接传 dict，不受版本升级影响 |
| 流式更可控 | `client.chat(stream=True)` 直接产出增量 chunk，拼 SSE 只需要 `meta → delta → done`，比走框架的 StreamChat 中间层简单 |
| 参数直给 | `think=False`、`num_ctx`、`num_predict`、`keep_alive`、`options` 原样透传；本次“8192 上下文导致 CPU 卸载、卡 7 分钟”就是靠这组参数解决的 |
| 依赖更轻 | 可省掉 `llama-index-llms-ollama`，减少版本升级连锁 |
| 性能无损 | 框架底层本来就是 Ollama HTTP 接口，少封装几乎不影响速度（实测约 40 秒瓶颈在模型本身） |
| 好替换 | 换模型/换后端只改 `app.py`、`ask.py` 中一小段调用 |

### 12.2 代价（需自己承担）

| 丢失能力 | 重要性 | 判断 |
|---|---|---|
| `ChatEngine` | 低 | 本质是“检索 → 拼上下文 → 调模型”，已由 `rag_engine.retrieve()` + `build_messages()` 等价实现；唯一值得单独搬的是内置的 **condense question（追问改写）** |
| `Memory` | 中 | 提供历史 + token 预算裁剪 + 超长摘要；自写“保留最近 N 轮 + 超预算截断”约 30~50 行。真正难点在指代消解，不在存储 |
| `ResponseSynthesizer` | 低 | 其 refine / compact / tree_summarize 面向文档总结；事实性问答用单次拼装 + 单次生成即可 |
| 引用节点自动回填 | 低 | 现有接口直接返回 `file + section`，前端拼「（来源：…）」，可读性优于 node id 引用 |

其余需要自管的点：多轮记忆、来源标注、流式聚合、超时与错误（模型未拉取/输出中断）、上下文预算。

## 13. 多轮记忆规划（第二版，本期不实现，仅留存备份）

### 13.1 判断结论

四个 LlamaIndex 能力中，只有 `Memory` 与多轮直接相关，其余三个都不重要，自写反而更可控。
继续用 LlamaIndex 做嵌入/切分/检索；生成侧保持 ollama 客户端；多轮记忆自己实现。

### 13.2 多轮记忆的四个真实难点（都不在框架那四个能力里）

1. **查询改写（指代消解）**：用户第二句问“那它的接口呢？”，必须还原为“诗词评分的接口是什么”，否则向量检索跑偏。这是多轮最容易答错的一环。
2. **上下文预算分配**：历史 + 检索资料 + 输出合计不能超 `num_ctx=4096`，否则重现 CPU 卸载、极慢、回答为空。
3. **会话生命周期**：小窗现有规则是“关闭即清空会话”，多轮需区分「页面会话中保留」与「用户主动清空」。
4. **前端传 history**：小窗已有消息列表，把最近几轮一并发给 `/rag/ask` 即可。

### 13.3 拟采用的最小方案

- 保留最近 3~5 轮原文对话；
- 用快模型 `maternion/minicpm5:2b-q8_0` 做一次查询改写（`num_predict` 限制 20~40 token，避免拖慢主回答）；
- 超预算按“先裁历史、再裁相邻块、最后裁重排块”的顺序截断；
- 接口扩展：`POST /rag/ask`、`/rag/ask/stream` 增加可选 `history` 字段（`[{role, content}]`），不传则行为与现在完全一致；
- 前端只在网页用法助理分区传 history，诗词助理维持单轮。

预计改动规模约 100 行，比接入 `ChatEngine` / `Memory` 更简单、更好调。

### 13.4 触发条件

- 当前单轮问答验收达标且线上稳定后，再作为第二版任务启动；
- 本期不做任何相关代码改动，仅在本方案中留存设计。
