# 墨渊知识问答服务（rag-service）

独立的本地知识问答服务，供全站 AI 小窗「网页用法助理」使用。
与《智能生成系统开发》期末作业无关。

## 目录

| 文件 | 作用 |
|---|---|
| `config.py` | 模型注册表、索引目录、运行时配置读写 |
| `model_registry.py` | 嵌入/生成模型解析、索引状态校验 |
| `corpus_export.py` | 导出业务文档 + `system_knowledge` 24 条到 `corpus/` |
| `build_index.py` | 章节切分 → 向量化 → BM25 词表落盘 |
| `rag_engine.py` | 混合检索、RRF、重排、邻居扩展、来源覆盖 |
| `app.py` | FastAPI 接口（健康、模型、配置、问答、SSE） |
| `ask.py` | 命令行问答 |
| `evaluate.py` + `eval/questions.json` | 旧版 8 题要点/来源覆盖回归 |
| `evaluate_retrieval.py` | 四组检索对比（A 仅向量 / B 仅 BM25 / C 混合无重排 / D 混合+重排） |
| `evaluate_generation.py` | 生成层对比（A / D 两组，含拒答与事实一致率） |
| `annotate.py` + `eval/questions_v2.json` + `eval/qrels.json` | 35 题评测集与 qrels 标注辅助 |
| `plot_results.py` | 论文双轴图：左 NDCG@8、右平均耗时 |
| `PLAN.md` | 接入方案 |

## 启动

```powershell
cd D:\A-xiangmu\moyuan-master2\rag-service

# 1) 导出语料（首次或文档更新后）
python corpus_export.py

# 2) 构建索引（默认 bge-small，仅此模型实测）
python build_index.py

# 3) 启动接口服务（127.0.0.1:8010）
python app.py
```

前端开发时 Vite 已把 `/rag` 代理到 `http://127.0.0.1:8010`，管理员页面入口：
`/admin/ai-models` 的「知识问答（本地模型）」标签页（旧地址 `/admin/rag-config` 会自动跳转）。

## 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/rag/health` | 服务、索引、Ollama 状态 |
| GET | `/rag/models` | 模型清单与索引状态 |
| GET | `/rag/config` | 当前生效配置 |
| PUT | `/rag/config` | 管理员切换嵌入/生成模型、重排、top_k |
| POST | `/rag/ask` | 同步问答，返回答案 + 来源 |
| POST | `/rag/ask/stream` | SSE：`meta` → `delta` → `done` / `error` |

## 模型策略

- 默认且唯一建索引：`bge-small-zh-v1.5`（512 维）
- 生成：Ollama 客户端，默认 `qwen3.5:4b`（评分题约 45 秒），备用 `maternion/minicpm5:2b-q8_0`（约 22 秒，接口列得少一些）
- 预留接口、不建索引、不实测：`jina-clip-v2`（1024 维）、`qwen3-embedding:0.6b`（1024 维）
- 每个嵌入模型使用独立索引：`data/index/<embed_key>/`；维度不匹配或未建索引会直接报错

## 已验证

- 8 个跨三文件问题：要点覆盖 100%，来源覆盖 100%
- 35 题评测集（10 Easy / 10 Medium / 10 Hard / 5 拒答）四组对比：
  仅向量 NDCG@8 0.721 → 仅 BM25 0.735 → 混合无重排 0.743 → 混合+重排 0.864，
  重排代价约 +9.2 秒/题；结果见 `eval/results/retrieval_groups.md` 与 `fig_retrieval.png`
- 评分问题端到端回答：两种评分方式、1.0-5.0、四个接口、入口 `/poem/:id`
- FastAPI 健康/模型/配置接口通过；切换预留模型会返回明确拒绝
- 前端 `aiChat` 单测 14/14 通过；小窗已接入 RAG，服务不可用时自动回退原链路

## 已知限制

- 4GB 显存下 `qwen3.5:4b` 单次回答约 40 秒，已限制 `num_ctx=4096`、`num_predict=400`、关闭思考链
- 小窗当前走同步 `/rag/ask`；SSE 接口已就绪，可下一步切换
- `app.py` 只监听 `127.0.0.1`，未做鉴权；对外部署需经 Spring/Nginx 并校验 JWT
- `system_knowledge` 目前从 `init.sql` 导出，后台修改后需重新导出并重建索引
