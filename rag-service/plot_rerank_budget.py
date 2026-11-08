# -*- coding: utf-8 -*-
"""重排候选池折中曲线：左轴 NDCG@8 / Recall@8，右轴平均检索耗时

用法：
    python plot_rerank_budget.py
"""
import json

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt

import config

plt.rcParams["font.sans-serif"] = ["Microsoft YaHei", "SimHei"]
plt.rcParams["axes.unicode_minus"] = False


def main():
    path = config.EVAL_DIR / "results" / "rerank_budget.json"
    data = json.loads(path.read_text(encoding="utf-8"))
    baseline = data["baseline"]["summary"]
    pools = [int(k) for k in data["pools"].keys()]
    pools.sort()
    rows = [data["pools"][str(p)]["summary"] for p in pools]

    fig, ax1 = plt.subplots(figsize=(8, 4.6))
    x = [0] + pools
    ndcg = [baseline["ndcg"]] + [r["ndcg"] for r in rows]
    recall = [baseline["recall"]] + [r["recall"] for r in rows]
    ax1.plot(x, ndcg, marker="o", color="#4C72B0", linewidth=1.8, label="NDCG@8")
    ax1.plot(x, recall, marker="s", color="#9DB8DC", linewidth=1.5, label="Recall@8")
    for xi, yi in zip(x, ndcg):
        ax1.annotate(f"{yi:.3f}", (xi, yi), textcoords="offset points",
                     xytext=(0, 7), ha="center", fontsize=8, color="#4C72B0")
    ax1.set_xlabel("重排候选池大小（0 = 关重排）")
    ax1.set_ylabel("检索质量（二值相关，top-8）")
    ax1.set_ylim(0.75, 0.98)
    ax1.set_xticks(x)
    ax1.legend(loc="lower right", fontsize=9)

    ax2 = ax1.twinx()
    latency = [baseline["total_ms"]] + [r["total_ms"] for r in rows]
    ax2.plot(x, latency, marker="^", color="#C44E52", linewidth=1.6, label="平均检索耗时")
    ax2.set_ylabel("平均检索耗时（ms）")
    ax2.set_ylim(0, max(latency) * 1.18)
    for xi, yi in zip(x, latency):
        ax2.annotate(f"{yi:.0f}", (xi, yi), textcoords="offset points",
                     xytext=(0, -12), ha="center", fontsize=8, color="#C44E52")
    ax2.legend(loc="upper left", fontsize=9)

    ax1.set_title("重排候选池的效果与成本折中（30 道可答题，top-8）", fontsize=11)
    fig.tight_layout()
    out = config.EVAL_DIR / "results" / "fig_rerank_budget.png"
    fig.savefig(out, dpi=200)
    print(f"图已保存：{out}")


if __name__ == "__main__":
    main()
