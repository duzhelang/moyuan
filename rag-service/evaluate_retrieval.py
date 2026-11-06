# -*- coding: utf-8 -*-
""""四组检索对比实验：A 仅向量 / B 仅 BM25 / C 混合无重排 / D 混合+重排

指标（二值相关，@8）：
- Recall@8：top-8 覆盖了多少比例的相关块
- Hit@8：top-8 是否至少命中一个相关块
- MRR@8：第一个相关块排名的倒数
- NDCG@8：log2 折损的归一化折损累计增益
- 平均检索耗时（embed / bm25 / rerank 分段 + 总耗时）
- 拒答正确率：5 道拒答题被正确拒答的比例

排序指标统一取重排/融合后的 top-8（`ranked`），不受来源覆盖与邻居扩展影响；
拒答阈值按「30 道可答题上该组 top-1 门控分的最小值」标定，四组口径一致、零误拒。

用法：
    python evaluate_retrieval.py
    python evaluate_retrieval.py --groups A,D
    python evaluate_retrieval.py --top-k 8
"""
import argparse
import json
import math

import config
from rag_engine import RagEngine

GROUPS = {
    "A": {"mode": "vector", "rerank": False, "label": "仅向量（bge-small）"},
    "B": {"mode": "bm25", "rerank": False, "label": "仅 BM25"},
    "C": {"mode": "hybrid", "rerank": False, "label": "向量+BM25 → RRF（关重排）"},
    "D": {"mode": "hybrid", "rerank": True, "label": "RRF + bge-reranker（现方案）"},
}


def load_json(name):
    return json.loads((config.EVAL_DIR / name).read_text(encoding="utf-8"))


def recall_at_k(ranked, relevant, k):
    if not relevant:
        return None
    hit = len(set(ranked[:k]) & set(relevant))
    return hit / len(relevant)


def hit_at_k(ranked, relevant, k):
    if not relevant:
        return None
    return 1.0 if set(ranked[:k]) & set(relevant) else 0.0


def mrr_at_k(ranked, relevant, k):
    if not relevant:
        return None
    for pos, idx in enumerate(ranked[:k], 1):
        if idx in relevant:
            return 1.0 / pos
    return 0.0


def ndcg_at_k(ranked, relevant, k):
    if not relevant:
        return None
    gains = [1.0 if idx in relevant else 0.0 for idx in ranked[:k]]
    dcg = sum(g / math.log2(i + 2) for i, g in enumerate(gains))
    ideal = [1.0] * min(len(relevant), k)
    idcg = sum(1 / math.log2(i + 2) for i in range(len(ideal)))
    return dcg / idcg if idcg else 0.0


def mean(values):
    items = [v for v in values if v is not None]
    return sum(items) / len(items) if items else 0.0


def run_group(key, questions, qrels, top_k):
    spec = GROUPS[key]
    engine = RagEngine(use_rerank=spec["rerank"], retrieval_mode=spec["mode"])
    answerable = [q for q in questions if q["tier"] != "refuse"]
    refusal = [q for q in questions if q["tier"] == "refuse"]

    # 1) 排序指标：统一关闭拒答门控，保证每组都能出 top-k，便于公平比较
    rows = []
    for item in answerable:
        result = engine.retrieve(item["question"], rerank_top_k=top_k, min_score=-1.0)
        rows.append({
            "id": item["id"],
            "tier": item["tier"],
            "ranked": result["ranked"],
            "latency": result.get("latency_ms", {}).get("total"),
            "latency_parts": result.get("latency_ms", {}),
            "gate": result["gate_score"],
        })

    # 2) 拒答阈值：取可答题 top-1 门控分的最小值，保证零误拒
    threshold = min(r["gate"] for r in rows) if rows else 0.0
    refused = []
    for item in refusal:
        result = engine.retrieve(item["question"], rerank_top_k=top_k, min_score=threshold)
        refused.append({"id": item["id"], "gate": result["gate_score"], "refused": result["refused"]})
    refusal_rate = sum(1 for r in refused if r["refused"]) / len(refused) if refused else 0.0

    # 3) 汇总
    per_question = []
    for row, item in zip(rows, answerable):
        relevant = qrels[item["id"]]["relevant"]
        per_question.append({
            "id": row["id"],
            "tier": row["tier"],
            "ranked": row["ranked"],
            "recall": recall_at_k(row["ranked"], relevant, top_k),
            "hit": hit_at_k(row["ranked"], relevant, top_k),
            "mrr": mrr_at_k(row["ranked"], relevant, top_k),
            "ndcg": ndcg_at_k(row["ranked"], relevant, top_k),
            "latency_ms": row["latency"],
        })

    summary = {
        "group": key,
        "label": spec["label"],
        "mode": spec["mode"],
        "rerank": spec["rerank"],
        "recall": mean([q["recall"] for q in per_question]),
        "hit": mean([q["hit"] for q in per_question]),
        "mrr": mean([q["mrr"] for q in per_question]),
        "ndcg": mean([q["ndcg"] for q in per_question]),
        "latency_ms": mean([q["latency_ms"] for q in per_question]),
        "refusal_threshold": round(threshold, 4),
        "refusal_rate": refusal_rate,
    }
    for tier in ("easy", "medium", "hard"):
        tier_rows = [q for q in per_question if q["tier"] == tier]
        summary[f"ndcg_{tier}"] = mean([q["ndcg"] for q in tier_rows])
        summary[f"recall_{tier}"] = mean([q["recall"] for q in tier_rows])
    return summary, per_question, refused


def main():
    parser = argparse.ArgumentParser(description="墨渊检索四组对比实验")
    parser.add_argument("--groups", default="A,B,C,D", help="要跑的组，逗号分隔")
    parser.add_argument("--top-k", type=int, default=config.RERANK_TOP_K, help="排序截断深度")
    parser.add_argument("--embed", default=config.DEFAULT_EMBED, help="嵌入模型 key")
    args = parser.parse_args()

    questions = load_json("questions_v2.json")
    qrels = load_json("qrels.json")
    keys = [k.strip() for k in args.groups.split(",") if k.strip()]

    results, details = {}, {}
    for key in keys:
        if key not in GROUPS:
            raise SystemExit(f"未知的组：{key}")
        summary, per_question, refused = run_group(key, questions, qrels, args.top_k)
        results[key] = summary
        details[key] = {"summary": summary, "per_question": per_question, "refusal": refused}
        print(
            f"{key} {summary['label']}: "
            f"Recall@{args.top_k}={summary['recall']:.3f} "
            f"Hit@{args.top_k}={summary['hit']:.3f} "
            f"MRR@{args.top_k}={summary['mrr']:.3f} "
            f"NDCG@{args.top_k}={summary['ndcg']:.3f} "
            f"耗时={summary['latency_ms']:.0f}ms "
            f"拒答={summary['refusal_rate'] * 100:.1f}%"
        )

    out_dir = config.EVAL_DIR / "results"
    out_dir.mkdir(parents=True, exist_ok=True)
    (out_dir / "retrieval_groups.json").write_text(
        json.dumps(details, ensure_ascii=False, indent=2), encoding="utf-8"
    )

    k = args.top_k
    lines = [
        f"# 检索层四组对比（top-{k}，二值相关，{len([q for q in questions if q['tier'] != 'refuse'])} 道可答题 + 5 道拒答题）",
        "",
        f"| 组 | 配置 | Recall@{k} | Hit@{k} | MRR@{k} | NDCG@{k} | 平均耗时(ms) | 拒答正确率 |",
        "|---|---|---|---|---|---|---|---|",
    ]
    for key in keys:
        s = results[key]
        lines.append(
            f"| {key} | {s['label']} | {s['recall']:.3f} | {s['hit']:.3f} | "
            f"{s['mrr']:.3f} | {s['ndcg']:.3f} | {s['latency_ms']:.0f} | "
            f"{s['refusal_rate'] * 100:.1f}% |"
        )
    lines += [
        "",
        f"| 组 | NDCG@{k} Easy | NDCG@{k} Medium | NDCG@{k} Hard |",
        "|---|---|---|---|",
    ]
    for key in keys:
        s = results[key]
        lines.append(
            f"| {key} | {s['ndcg_easy']:.3f} | {s['ndcg_medium']:.3f} | {s['ndcg_hard']:.3f} |"
        )
    (out_dir / "retrieval_groups.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"\n结果已写入：{out_dir / 'retrieval_groups.md'}")


if __name__ == "__main__":
    main()
