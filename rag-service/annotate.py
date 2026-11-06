# -*- coding: utf-8 -*-
"""qrels 标注辅助：把每题 D 组（混合+重排）的 top-8 候选导出成人可读的标注表

用途：213 块不必全标，只标注每题的 top-8 候选，二值 0/1 足够算 NDCG@8。

用法：
    python annotate.py --export          # 生成 eval/annotation_sheet.md 供人工核对
    python annotate.py --check           # 校验 qrels 中的块 id 是否都存在
"""
import argparse
import json

import config
from rag_engine import RagEngine

TOP_K = 8


def load_json(name):
    return json.loads((config.EVAL_DIR / name).read_text(encoding="utf-8"))


def chunk_label(chunk, limit=90):
    text = chunk["text"].replace("\n", " ")
    return f"{chunk['section']}｜{text[:limit]}"


def export():
    questions = load_json("questions_v2.json")
    qrels = load_json("qrels.json")
    engine = RagEngine(use_rerank=True, retrieval_mode="hybrid")

    lines = [
        "# 检索评测标注表",
        "",
        "对每题 top-8 候选逐块标注：能直接支撑回答填 1，否则填 0。",
        "标完把结果填回 `eval/qrels.json` 的 `relevant` 数组，并把 `source` 改成 `human`。",
        "",
    ]
    for item in questions:
        result = engine.retrieve(item["question"], rerank_top_k=TOP_K)
        lines.append(f"## {item['id']}（{item['tier']}）{item['question']}")
        lines.append("")
        if item["tier"] == "refuse":
            lines.append("- 拒答题：确认知识库确实没有该内容即可，无需标相关块。")
            lines.append("")
            continue
        lines.append(f"- 期望来源：{'、'.join(item['sources'])}")
        lines.append(f"- 现有标注：{qrels.get(item['id'], {}).get('relevant', [])}")
        lines.append("")
        lines.append("| 排名 | 块 id | 文件 | 章节 / 正文 | 标注 |")
        lines.append("|---|---|---|---|---|")
        if result["refused"]:
            lines.append("| - | - | - | 检索判定拒答，未返回候选 | - |")
        for rank, idx in enumerate(result["ranked"], 1):
            chunk = engine.chunks[idx]
            lines.append(
                f"| {rank} | {idx} | {chunk['file']} | {chunk_label(chunk)} | |"
            )
        lines.append("")

    path = config.EVAL_DIR / "annotation_sheet.md"
    path.write_text("\n".join(lines), encoding="utf-8")
    print(f"标注表已生成：{path}")
    print(f"共 {len(questions)} 题，其中拒答 {sum(1 for q in questions if q['tier'] == 'refuse')} 题")


def check():
    qrels = load_json("qrels.json")
    questions = load_json("questions_v2.json")
    chunks = json.loads(
        (config.index_dir(config.DEFAULT_EMBED) / "chunks.json").read_text(encoding="utf-8")
    )
    total = len(chunks)
    bad = []
    for qid, entry in qrels.items():
        if qid == "_说明":
            continue
        for idx in entry["relevant"]:
            if not 0 <= idx < total:
                bad.append((qid, idx))
    missing = [q["id"] for q in questions if q["id"] not in qrels]
    print(f"块总数 {total}，标注题数 {len(qrels) - 1}")
    if bad:
        print(f"越界块 id：{bad}")
    if missing:
        print(f"缺少标注的题：{', '.join(missing)}")
    if not bad and not missing:
        print("qrels 校验通过")


def main():
    parser = argparse.ArgumentParser(description="qrels 标注辅助")
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--export", action="store_true", help="导出标注表")
    group.add_argument("--check", action="store_true", help="校验 qrels")
    args = parser.parse_args()
    export() if args.export else check()


if __name__ == "__main__":
    main()
