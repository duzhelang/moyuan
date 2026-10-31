# -*- coding: utf-8 -*-
"""检索质量评测：8 个跨三个语料文件的复合问题

只评测“检索 + 上下文组装”，不调用大模型，可快速回归。
指标：
- 要点覆盖率：参考答案关键词在送入模型的上下文中出现的比例；
- 来源覆盖率：期望的三个语料文件实际进入上下文的比例。

用法：
    python evaluate.py
    python evaluate.py --no-rerank          # 对比实验：关闭重排
    python evaluate.py --show-context       # 打印每个问题的上下文
"""
import argparse
import json

import config
from rag_engine import RagEngine


def load_questions():
    path = config.EVAL_DIR / "questions.json"
    return json.loads(path.read_text(encoding="utf-8"))


def main():
    parser = argparse.ArgumentParser(description="墨渊知识库检索评测")
    parser.add_argument("--no-rerank", action="store_true", help="关闭重排")
    parser.add_argument("--show-context", action="store_true", help="打印上下文")
    parser.add_argument("--strict", action="store_true", help="指标不达标时返回非 0")
    parser.add_argument(
        "--embed", default=config.load_runtime_config()["embed"], help="嵌入模型 key"
    )
    args = parser.parse_args()

    engine = RagEngine(embed_key=args.embed, use_rerank=not args.no_rerank)
    rows, keyword_total, source_total = [], [], []

    for item in load_questions():
        result = engine.retrieve(item["question"])
        context = engine.build_context(result["chunks"])
        found = [kw for kw in item["keywords"] if kw in context]
        keyword_rate = len(found) / len(item["keywords"]) if item["keywords"] else 1.0
        expected_sources = set(item["sources"])
        hit_sources = expected_sources & set(result["files"])
        source_rate = len(hit_sources) / len(expected_sources) if expected_sources else 1.0
        keyword_total.append(keyword_rate)
        source_total.append(source_rate)
        rows.append((item, result, found, keyword_rate, source_rate))

        if args.show_context:
            print(f"\n===== {item['id']} {item['question']} =====")
            print(f"检索文件：{result['files']}")
            print(context)

    print("\n" + "=" * 96)
    print(f"{'题号':<5}{'上下文块':<8}{'来源覆盖':<10}{'要点覆盖':<10}{'缺失要点'}")
    print("-" * 96)
    for item, result, found, keyword_rate, source_rate in rows:
        missing = [kw for kw in item["keywords"] if kw not in found]
        print(
            f"{item['id']:<5}{len(result['chunks']):<8}"
            f"{source_rate * 100:>6.1f}%   {keyword_rate * 100:>6.1f}%   "
            f"{'、'.join(missing) if missing else '无'}"
        )
    print("-" * 96)
    print(
        f"平均要点覆盖率 {sum(keyword_total) / len(keyword_total) * 100:.1f}%  |  "
        f"平均来源覆盖率 {sum(source_total) / len(source_total) * 100:.1f}%"
    )
    print("=" * 96)

    if args.strict:
        bad = [r[0]["id"] for r in rows if r[3] < 0.8 or r[4] < 1.0]
        if bad:
            print(f"未达标题目：{', '.join(bad)}")
            raise SystemExit(1)


if __name__ == "__main__":
    main()
