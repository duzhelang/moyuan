# -*- coding: utf-8 -*-
"""门控阈值的分层对半划分检验（论文 9.5.2 中 1 000 次划分数据的复现脚本）

做法：按档位分层随机对半划分 30 道可答题，用标定半的门控分最小值作阈值，
在评估半上统计误拒数，并用该阈值判定 5 道拒答题，统计正确拒答数。

用法：
    python eval/gate_split_check.py
"""
import json
import random
import statistics
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

import config
from rag_engine import RagEngine

TIMES = 1000
SEED = 20261106


def main():
    questions = json.loads(
        (config.EVAL_DIR / "questions_v2.json").read_text(encoding="utf-8")
    )
    engine = RagEngine(use_rerank=False, retrieval_mode="hybrid")

    gates = {}
    tiers = {}
    for item in questions:
        result = engine.retrieve(item["question"], min_score=0.0)
        gates[item["id"]] = result["gate_score"]
        tiers[item["id"]] = item["tier"]

    answerable = [qid for qid in gates if tiers[qid] != "refuse"]
    refuse = [qid for qid in gates if tiers[qid] == "refuse"]
    threshold_full = min(gates[q] for q in answerable)
    print(f"全量标定阈值 {threshold_full:.4f}，"
          f"同集正确拒答 {sum(1 for q in refuse if gates[q] < threshold_full)}/{len(refuse)}，"
          f"误拒 {sum(1 for q in answerable if gates[q] < threshold_full)}/{len(answerable)}")

    by_tier = {}
    for qid in answerable:
        by_tier.setdefault(tiers[qid], []).append(qid)

    rng = random.Random(SEED)
    correct, false = [], []
    for _ in range(TIMES):
        calib, evalu = [], []
        for ids in by_tier.values():
            ids = ids[:]
            rng.shuffle(ids)
            half = len(ids) // 2
            calib += ids[:half]
            evalu += ids[half:]
        threshold = min(gates[q] for q in calib)
        correct.append(sum(1 for q in refuse if gates[q] < threshold))
        false.append(sum(1 for q in evalu if gates[q] < threshold))

    print(f"{TIMES} 次划分：评估半正确拒答均值 {statistics.mean(correct):.2f}/5 "
          f"（范围 {min(correct)}～{max(correct)}），"
          f"误拒均值 {statistics.mean(false):.2f}/15")

    out = config.EVAL_DIR / "results" / "gate_split_check.json"
    out.write_text(
        json.dumps(
            {
                "threshold_full": threshold_full,
                "gates": gates,
                "times": TIMES,
                "seed": SEED,
                "mean_correct": statistics.mean(correct),
                "mean_false": statistics.mean(false),
            },
            ensure_ascii=False,
            indent=2,
        ),
        encoding="utf-8",
    )
    print(f"已写入 {out}")


if __name__ == "__main__":
    main()
