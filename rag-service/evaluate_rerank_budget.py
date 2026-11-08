# -*- coding: utf-8 -*-
"""重排预算折中实验：同一 RAG 检索管线上，比较重排候选池大小对质量与时延的影响

目的：把“重排固定约 9 秒/题”拆成可调参数。固定 hybrid 召回、固定输出 top-8，
只改变进入 CrossEncoder 的候选池大小（8/12/16/20/24/30），并保留“关重排”基线。

指标（二值相关，统一 @8，与 evaluate_retrieval.py 口径一致）：
Recall@8 / Hit@8 / MRR@8 / NDCG@8，重排耗时、总耗时、P50/P95。
拒答门控发生在重排之前，候选池大小不影响拒答判定，故本实验只统计可答题。

用法：
    python evaluate_rerank_budget.py
    python evaluate_rerank_budget.py --pools 8,12,16,24,30 --repeats 1
"""
import argparse
import json
import statistics
import time

import config
from evaluate_retrieval import load_json, mean, mrr_at_k, ndcg_at_k, recall_at_k
from rag_engine import RagEngine

POOLS = [8, 12, 16, 20, 24, 30]


def percentile(values, ratio):
    items = sorted(values)
    if not items:
        return 0.0
    pos = min(len(items) - 1, int(round((len(items) - 1) * ratio)))
    return items[pos]


def run(pool, questions, qrels, repeats):
    engine = RagEngine(use_rerank=True, retrieval_mode="hybrid")
    rows = []
    for item in questions:
        for _ in range(repeats):
            result = engine.retrieve(
                item["question"], rerank_top_k=8, min_score=-1.0, rerank_pool=pool
            )
            ranked = result["ranked"]
            relevant = qrels[item["id"]]["relevant"]
            latency = result.get("latency_ms", {})
            rows.append({
                "id": item["id"],
                "ranked": ranked,
                "recall": recall_at_k(ranked, relevant, 8),
                "hit": hit_at_k(ranked, relevant, 8),
                "mrr": mrr_at_k(ranked, relevant, 8),
                "ndcg": ndcg_at_k(ranked, relevant, 8),
                "rerank_ms": latency.get("rerank", 0.0),
                "total_ms": latency.get("total", 0.0),
            })
    totals = [r["total_ms"] for r in rows]
    reranks = [r["rerank_ms"] for r in rows]
    summary = {
        "pool": pool,
        "label": f"重排候选池 {pool}",
        "recall": mean([r["recall"] for r in rows]),
        "hit": mean([r["hit"] for r in rows]),
        "mrr": mean([r["mrr"] for r in rows]),
        "ndcg": mean([r["ndcg"] for r in rows]),
        "rerank_ms": mean(reranks),
        "total_ms": mean(totals),
        "total_p50": statistics.median(totals),
        "total_p95": percentile(totals, 0.95),
    }
    return summary, rows


def hit_at_k(ranked, relevant, k):
    return 1.0 if set(ranked[:k]) & set(relevant) else 0.0


def run_baseline(questions, qrels):
    """关重排基线：同一 hybrid 召回，直接按 RRF 取 top-8。"""
    engine = RagEngine(use_rerank=False, retrieval_mode="hybrid")
    rows = []
    for item in questions:
        result = engine.retrieve(item["question"], rerank_top_k=8, min_score=-1.0)
        ranked = result["ranked"]
        relevant = qrels[item["id"]]["relevant"]
        latency = result.get("latency_ms", {})
        rows.append({
            "id": item["id"],
            "ranked": ranked,
            "recall": recall_at_k(ranked, relevant, 8),
            "hit": hit_at_k(ranked, relevant, 8),
            "mrr": mrr_at_k(ranked, relevant, 8),
            "ndcg": ndcg_at_k(ranked, relevant, 8),
            "rerank_ms": 0.0,
            "total_ms": latency.get("total", 0.0),
        })
    totals = [r["total_ms"] for r in rows]
    return {
        "pool": 0,
        "label": "混合·关重排（基线）",
        "recall": mean([r["recall"] for r in rows]),
        "hit": mean([r["hit"] for r in rows]),
        "mrr": mean([r["mrr"] for r in rows]),
        "ndcg": mean([r["ndcg"] for r in rows]),
        "rerank_ms": 0.0,
        "total_ms": mean(totals),
        "total_p50": statistics.median(totals),
        "total_p95": percentile(totals, 0.95),
    }, rows


def main():
    parser = argparse.ArgumentParser(description="重排候选池折中实验")
    parser.add_argument("--pools", default=",".join(str(p) for p in POOLS))
    parser.add_argument("--repeats", type=int, default=1, help="每档重复次数，用于时延稳定性")
    args = parser.parse_args()

    questions = [q for q in load_json("questions_v2.json") if q["tier"] != "refuse"]
    qrels = load_json("qrels.json")
    pools = [int(p) for p in args.pools.split(",") if p.strip()]

    started = time.perf_counter()
    out_dir = config.EVAL_DIR / "results"
    out_dir.mkdir(parents=True, exist_ok=True)
    out_path = out_dir / "rerank_budget.json"

    # 增量合并：分档运行时保留此前已完成的档位，避免一次中断丢掉全部结果
    results = {"baseline": None, "pools": {}}
    if out_path.exists():
        try:
            previous = json.loads(out_path.read_text(encoding="utf-8"))
            results["baseline"] = previous.get("baseline")
            results["pools"] = previous.get("pools", {})
        except Exception:
            pass
    if results["baseline"] is None:
        baseline, baseline_rows = run_baseline(questions, qrels)
        results["baseline"] = {"summary": baseline, "per_question": baseline_rows}
        print(
            f"基线 关重排: NDCG@8={baseline['ndcg']:.3f} "
            f"Recall@8={baseline['recall']:.3f} 总耗时={baseline['total_ms']:.0f}ms"
        )
    else:
        baseline = results["baseline"]["summary"]
        print(f"复用基线 关重排: NDCG@8={baseline['ndcg']:.3f}")

    for pool in pools:
        summary, rows = run(pool, questions, qrels, args.repeats)
        results["pools"][str(pool)] = {"summary": summary, "per_question": rows}
        out_path.write_text(
            json.dumps(results, ensure_ascii=False, indent=2), encoding="utf-8"
        )
        print(
            f"池={pool:2d}: NDCG@8={summary['ndcg']:.3f} "
            f"Recall@8={summary['recall']:.3f} MRR@8={summary['mrr']:.3f} "
            f"重排={summary['rerank_ms']:.0f}ms 总耗时={summary['total_ms']:.0f}ms "
            f"P95={summary['total_p95']:.0f}ms"
        )

    write_report(results, questions)
    print(f"\n结果已写入：{out_dir / 'rerank_budget.md'}")
    print(f"总用时 {(time.perf_counter() - started) / 60:.0f} 分钟")


def write_report(results, questions):
    out_dir = config.EVAL_DIR / "results"
    full = results["pools"].get("30", {}).get("summary")
    ordered = sorted(results["pools"].keys(), key=lambda k: int(k))
    lines = [
        f"# 重排候选池折中实验（hybrid 召回固定，输出 top-8，{len(questions)} 道可答题）",
        "",
        "| 配置 | 候选池 | NDCG@8 | Recall@8 | MRR@8 | 重排耗时(ms) | 总耗时(ms) | P95(ms) | 相对池30 NDCG |",
        "|---|---|---|---|---|---|---|---|---|",
    ]
    entries = [results["baseline"]["summary"]] + [
        results["pools"][k]["summary"] for k in ordered
    ]
    for s in entries:
        delta = f"{s['ndcg'] - full['ndcg']:+.3f}" if full else "—"
        lines.append(
            f"| {s['label']} | {s['pool'] or '—'} | {s['ndcg']:.3f} | {s['recall']:.3f} | "
            f"{s['mrr']:.3f} | {s['rerank_ms']:.0f} | {s['total_ms']:.0f} | "
            f"{s['total_p95']:.0f} | {delta} |"
        )
    (out_dir / "rerank_budget.md").write_text("\n".join(lines) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
