# -*- coding: utf-8 -*-
"""检索引擎：问题拆解 + 向量/BM25 混合召回 + RRF 融合 + 重排 + 邻居扩展 + 来源覆盖

设计目标（针对期末版本暴露的问题）：
- 复合问题（“哪两种方式 / 范围 / 接口 / 在哪里”）逐个子问题召回，避免只答一半；
- 关键词与语义双路召回，避免“评分”被“点赞”等相近语义挤掉；
- 命中块扩展同章节相邻块，评分范围、接口表、页面入口不再被切碎；
- 按来源文件补齐覆盖，三个语料文件都能被引用到；
- 每个文本块带“文件名 + 章节标题”，回答可精确标注出处。
"""
import re
import time

import jieba
import numpy as np
from rank_bm25 import BM25Okapi
from sentence_transformers import CrossEncoder

import config
import model_registry

SYSTEM_PROMPT = """你是「墨渊」古典诗词文化交流平台的系统知识助手。
回答规则：
1. 只依据【参考资料】作答，不得编造资料中不存在的页面、接口、参数或数值。
2. 问题可能包含多个子问题，必须逐项回答；问“哪几种/范围/接口/在哪里”时，类型、数值范围、完整接口路径、具体页面入口都要给出。
3. 接口一律写出完整路径（含请求方法）；入口写到具体页面或按钮；数值照抄资料原文。
4. 资料来自多个文件时，在相应结论后用“（来源：文件名·章节标题）”标注；直接写资料开头【】里的文件名与章节，不要使用“资料 1/资料 6”这类编号；至少覆盖资料中涉及的全部文件。
5. 只有在资料确实没有对应内容时，才说明“知识库未收录该项”，并指出缺的是哪一项，不要用相近内容替代。
6. 先给结论，再分条展开；不使用 Markdown 标题符号，总字数不超过 400 字。"""


def split_questions(question):
    """把复合问题拆成子问题；原问题始终保留，保证整体语义不丢。"""
    parts = re.split(r"[？?；;。\n]", question)
    subs = []
    for part in parts:
        part = re.sub(r"^(那么|另外|还有|以及|并且|然后|请|帮我|我想知道)", "", part)
        part = part.strip(" ，,、：:")
        if len(part) >= 4:
            subs.append(part)
    queries = [question] + subs
    return list(dict.fromkeys(queries))[:4]


def tokenize(text):
    return [t for t in jieba.lcut_for_search(text) if t.strip()]


RETRIEVAL_MODES = ("vector", "bm25", "hybrid")


class RagEngine:
    def __init__(self, embed_key=None, use_rerank=None, retrieval_mode="hybrid"):
        runtime = config.load_runtime_config()
        self.embed_key = embed_key or runtime["embed"]
        if use_rerank is None:
            use_rerank = runtime["rerank"]
        if retrieval_mode not in RETRIEVAL_MODES:
            raise ValueError(f"未知的检索模式：{retrieval_mode}")
        self.retrieval_mode = retrieval_mode
        self.rerank_top_k = runtime["top_k"]
        if self.embed_key not in config.EMBEDDINGS:
            raise ValueError(f"未注册的嵌入模型：{self.embed_key}")

        self.index_dir = config.index_dir(self.embed_key)
        status = model_registry.index_status(self.embed_key)
        if not status["ready"]:
            raise FileNotFoundError(
                f"索引不可用：{self.embed_key}（{status['message']}）\n"
                f"请先运行 python build_index.py --embed {self.embed_key}"
            )

        self.chunks = self._load_json("chunks.json")
        self.embeddings = np.load(self.index_dir / "embeddings.npy")
        tokens = self._load_json("tokens.json")
        self.bm25 = BM25Okapi(tokens)
        self.by_file_index = {
            (c["file"], c["chunk_index"]): c["id"] for c in self.chunks
        }
        self.file_order = list(dict.fromkeys(c["file"] for c in self.chunks))

        self.embedder = model_registry.get_embedder(self.embed_key)
        self.reranker = None
        if use_rerank:
            self.reranker = CrossEncoder(
                config.find_snapshot(config.RERANK_MODEL_DIR),
                device="cpu",
                max_length=512,
            )

    # ---------------- 基础工具 ----------------
    def _load_json(self, name):
        import json
        return json.loads((self.index_dir / name).read_text(encoding="utf-8"))

    def _vector_ranking(self, query, top_k):
        vec = np.asarray(self.embedder.get_query_embedding(query), dtype="float32")
        vec /= np.linalg.norm(vec) + 1e-8
        scores = self.embeddings @ vec
        order = np.argsort(-scores)[:top_k]
        return [(int(i), float(scores[i])) for i in order]

    def _bm25_ranking(self, query, top_k):
        scores = self.bm25.get_scores(tokenize(query))
        order = np.argsort(-scores)[:top_k]
        return [(int(i), float(scores[i])) for i in order if scores[i] > 0]

    @staticmethod
    def _same_section_group(a, b):
        """相邻块是否属于同一章节组：同一小节，或同一父级下的兄弟小节。"""
        if a["section"] == b["section"]:
            return True
        pa, pb = a["section"].split(" / "), b["section"].split(" / ")
        return len(pa) >= 3 and pa[:-1] == pb[:-1]

    # ---------------- 检索主流程 ----------------
    def retrieve(self, question, rerank_top_k=None, min_score=None):
        started = time.perf_counter()
        top_n = rerank_top_k or self.rerank_top_k
        queries = split_questions(question)
        merged = {}
        use_vector = self.retrieval_mode in ("vector", "hybrid")
        use_bm25 = self.retrieval_mode in ("bm25", "hybrid")
        latency = {"embed": 0.0, "bm25": 0.0, "rerank": 0.0}

        for query in queries:
            if use_vector:
                t0 = time.perf_counter()
                vector_hits = self._vector_ranking(query, config.VECTOR_TOP_K)
                latency["embed"] += time.perf_counter() - t0
                for rank, (idx, score) in enumerate(vector_hits, 1):
                    item = merged.setdefault(
                        idx, {"rrf": 0.0, "vector": 0.0, "bm25": 0.0, "hits": 0}
                    )
                    item["rrf"] += 1.0 / (config.RRF_K + rank)
                    item["vector"] = max(item["vector"], score)
                    item["hits"] += 1
            if use_bm25:
                t0 = time.perf_counter()
                bm25_hits = self._bm25_ranking(query, config.BM25_TOP_K)
                latency["bm25"] += time.perf_counter() - t0
                for rank, (idx, score) in enumerate(bm25_hits, 1):
                    item = merged.setdefault(
                        idx, {"rrf": 0.0, "vector": 0.0, "bm25": 0.0, "hits": 0}
                    )
                    item["rrf"] += 1.0 / (config.RRF_K + rank)
                    item["bm25"] = max(item["bm25"], score)
                    item["hits"] += 1

        candidates = sorted(
            merged.items(), key=lambda kv: (-kv[1]["rrf"], -kv[1]["vector"])
        )
        best_rrf = candidates[0][1]["rrf"] if candidates else 0.0
        best_vector = max((v["vector"] for v in merged.values()), default=0.0)
        best_bm25 = max((v["bm25"] for v in merged.values()), default=0.0)
        gate_score = {
            "vector": best_vector,
            "bm25": best_bm25,
            "hybrid": best_rrf,
        }[self.retrieval_mode]
        threshold = (
            min_score
            if min_score is not None
            else config.REFUSAL_MIN_SCORE[self.retrieval_mode]
        )
        result = {
            "question": question,
            "queries": queries,
            "mode": self.retrieval_mode,
            "chunks": [],
            "files": [],
            "ranked": [],
            "ranked_scores": [],
            "best_rrf": best_rrf,
            "best_vector": best_vector,
            "best_bm25": best_bm25,
            "gate_score": gate_score,
            "threshold": threshold,
            "refused": not candidates or gate_score < threshold,
        }
        if result["refused"]:
            return result

        # 1) 重排：候选过多时先截断，再按“问题—文本块”相关性排序
        pool = candidates[:30]
        if self.reranker:
            t0 = time.perf_counter()
            pairs = [[question, self.chunks[i]["text"]] for i, _ in pool]
            rerank_scores = [float(s) for s in self.reranker.predict(pairs)]
            latency["rerank"] += time.perf_counter() - t0
            rerank_all = {i: s for (i, _), s in zip(pool, rerank_scores)}
            order = sorted(pool, key=lambda pair: -rerank_all[pair[0]])
            selected = [(i, rerank_all[i]) for i, _ in order[:top_n]]
        else:
            rerank_all = {i: fused["rrf"] for i, fused in pool}
            selected = [(i, fused["rrf"]) for i, fused in pool[:top_n]]
        result["ranked"] = [i for i, _ in selected]
        result["ranked_scores"] = [score for _, score in selected]

        # 2) 来源覆盖：某语料文件有召回却未进上下文时，补入该文件最相关的一块
        final = list(selected)
        final_ids = {i for i, _ in selected}
        selected_ids = set(final_ids)
        represented = {self.chunks[i]["file"] for i, _ in final}
        coverage_ids = set()
        for file_name in self.file_order:
            if file_name in represented:
                continue
            file_candidates = [
                i for i, _ in pool if self.chunks[i]["file"] == file_name
            ]
            if not file_candidates:
                continue
            best = max(
                file_candidates,
                key=lambda i: (merged[i]["hits"], merged[i]["rrf"]),
            )
            if merged[best]["rrf"] < config.MIN_RRF_SCORE:
                continue
            final.append((best, rerank_all.get(best, 0.0)))
            final_ids.add(best)
            coverage_ids.add(best)
            represented.add(file_name)

        # 3) 邻居扩展：命中块的前后块一并进上下文，保证章节完整（受块数上限约束）
        for idx, score in selected:
            chunk = self.chunks[idx]
            for offset in range(-config.NEIGHBOR_WINDOW, config.NEIGHBOR_WINDOW + 1):
                if offset == 0 or len(final) >= config.MAX_CONTEXT_CHUNKS:
                    continue
                nid = self.by_file_index.get(
                    (chunk["file"], chunk["chunk_index"] + offset)
                )
                if nid is None or nid in final_ids:
                    continue
                if not self._same_section_group(chunk, self.chunks[nid]):
                    continue
                final_ids.add(nid)
                final.append((nid, score * 0.5))

        # 4) 字数预算：超限时优先移除相邻块，保留重排命中块与来源覆盖块
        score_map = dict(final)
        while len(final) > config.MAX_CONTEXT_CHUNKS or sum(
            len(self.chunks[i]["text"]) for i, _ in final
        ) > config.MAX_CONTEXT_CHARS:
            removable = [
                i for i, _ in final
                if i not in selected_ids and i not in coverage_ids
            ]
            if not removable:
                break
            worst = min(removable, key=lambda i: score_map.get(i, 0.0))
            final = [(i, s) for i, s in final if i != worst]

        # 5) 组装结果：按“文件顺序 + 块序号”排列，阅读顺序更接近原文
        ordered = sorted(
            final,
            key=lambda pair: (
                self.file_order.index(self.chunks[pair[0]]["file"]),
                self.chunks[pair[0]]["chunk_index"],
            ),
        )
        for idx, rerank_score in ordered:
            chunk = self.chunks[idx]
            fused = merged.get(
                idx, {"vector": 0.0, "bm25": 0.0, "rrf": 0.0, "hits": 0}
            )
            result["chunks"].append({
                "id": idx,
                "file": chunk["file"],
                "section": chunk["section"],
                "text": chunk["text"],
                "vector": fused["vector"],
                "bm25": fused["bm25"],
                "rrf": fused["rrf"],
                "rerank": rerank_score,
                "hits": fused["hits"],
                "neighbor": idx not in selected_ids,
            })
        result["files"] = sorted(
            {c["file"] for c in result["chunks"]},
            key=self.file_order.index,
        )
        latency["total"] = (time.perf_counter() - started) * 1000
        for key in ("embed", "bm25", "rerank"):
            latency[key] = latency[key] * 1000
        result["latency_ms"] = latency
        return result

    # ---------------- 提示词 ------------
    @staticmethod
    def build_context(chunks):
        blocks = []
        for chunk in chunks:
            blocks.append(
                f"【{chunk['file']} · {chunk['section']}】\n{chunk['text']}"
            )
        return "\n\n".join(blocks)

    def build_messages(self, question, chunks):
        """生成侧使用 ollama 客户端，消息为标准 dict。"""
        context = self.build_context(chunks)
        user = f"参考资料：\n{context}\n\n问题：{question}"
        return [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": user},
        ]
