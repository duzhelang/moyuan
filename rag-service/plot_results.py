# -*- coding: utf-8 -*-
"""把检索四组实验画成双轴图：左轴 NDCG@8，右轴平均检索耗时（供论文图 9.3 使用）

用法：
    python plot_results.py
"""
import json

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt

import config

plt.rcParams["font.sans-serif"] = ["Microsoft YaHei", "SimHei"]
plt.rcParams["axes.unicode_minus"] = False


SHORT = {
    "A": "A 仅向量",
    "B": "B 仅BM25",
    "C": "C 混合·关重排",
    "D": "D 混合·开重排",
}


def main():
    path = config.EVAL_DIR / "results" / "retrieval_groups.json"
    data = json.loads(path.read_text(encoding="utf-8"))
    keys = list(data.keys())
    labels = [SHORT.get(k, k) for k in keys]
    ndcg = [data[k]["summary"]["ndcg"] for k in keys]
    recall = [data[k]["summary"]["recall"] for k in keys]
    latency = [data[k]["summary"]["latency_ms"] for k in keys]

    fig, ax1 = plt.subplots(figsize=(8, 4.6))
    x = range(len(keys))
    width = 0.36
    bars1 = ax1.bar(
        [i - width / 2 for i in x], ndcg, width, label="NDCG@8", color="#4C72B0"
    )
    bars2 = ax1.bar(
        [i + width / 2 for i in x], recall, width, label="Recall@8", color="#9DB8DC"
    )
    ax1.set_ylabel("检索质量（二值相关）")
    ax1.set_ylim(0, 1.05)
    ax1.set_xlim(-0.55, len(keys) - 0.42)
    ax1.set_xticks(list(x))
    ax1.set_xticklabels(labels, fontsize=9)
    for bar in list(bars1) + list(bars2):
        ax1.annotate(
            f"{bar.get_height():.3f}",
            (bar.get_x() + bar.get_width() / 2, bar.get_height()),
            ha="center", va="bottom", fontsize=8,
        )
    ax1.legend(loc="upper left", fontsize=9)

    ax2 = ax1.twinx()
    ax2.plot(
        list(x), latency, marker="o", color="#C44E52", linewidth=1.8,
        label="平均检索耗时",
    )
    ax2.set_ylabel("平均检索耗时（ms）")
    ax2.set_yscale("log")
    ax2.set_ylim(min(latency) * 0.55, max(latency) * 2.6)
    for i, value in zip(x, latency):
        ax2.annotate(
            f"{value:.0f}ms", (i, value), textcoords="offset points",
            xytext=(17, 0), ha="left", va="center", fontsize=8, color="#C44E52",
            bbox=dict(boxstyle="round,pad=0.18", fc="white", ec="none", alpha=0.85),
        )
    ax2.legend(loc="lower left", fontsize=9)

    ax1.set_title("四组检索配置的效果与成本（30 道可答题，top-8）", fontsize=11)
    fig.tight_layout()
    out = config.EVAL_DIR / "results" / "fig_retrieval.png"
    fig.savefig(out, dpi=200)
    print(f"图已保存：{out}")


if __name__ == "__main__":
    main()
