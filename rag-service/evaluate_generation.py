# -*- coding: utf-8 -*-
"""生成层对比实验：只跑 A（仅向量）与 D（混合+重排）两组

指标：
- 要点覆盖：参考答案关键词在回答中出现的比例
- 来源覆盖：期望语料文件进入上下文的比例
- 事实一致率（可自动核对部分）：回答里出现的接口路径与带单位数值，有多少能在上下文中找到
- 拒答率：5 道拒答题被系统正确拒答的比例
- 忠实度：需人工判读，本脚本导出评阅表（eval/results/faithfulness_sheet.md）

用法：
    python evaluate_generation.py                  # A + D，全部 35 题
    python evaluate_generation.py --groups D
    python evaluate_generation.py --limit 6        # 先小样本试跑
"""
import argparse
import json
import re
import time

import config
import model_registry
from rag_engine import RagEngine

GROUPS = {
    "A": {"mode": "vector", "rerank": False, "label": "仅向量（bge-small）"},
    "D": {"mode": "hybrid", "rerank": True, "label": "RRF + bge-reranker（现方案）"},
}

API_RE = re.compile(r"/api/[A-Za-z0-9_\-/{}.]+")
NUM_RE = re.compile(r"\d+(?:\.\d+)?\s*(?:分|秒|天|小时|MB|条|个|档|种|项|%|元|倍)")
NUM_ONLY_RE = re.compile(r"\d+(?:\.\d+)?")


def load_json(path):
    return json.loads(path.read_text(encoding="utf-8"))


def fact_check(answer, context):
    """自动核对：接口路径须原文出现；带单位数值只核对数字本身（单位常由回答补全）。"""
    apis = [f.strip() for f in API_RE.findall(answer)]
    nums = [NUM_ONLY_RE.search(f).group(0) for f in NUM_RE.findall(answer)]
    facts = list(dict.fromkeys(apis + nums))
    unsupported = [f for f in facts if f not in context]
    return len(facts) - len(unsupported), len(facts), unsupported


def ensure_ollama():
    """Ollama 掉线时自动拉起本地服务，避免长跑中断。"""
    try:
        model_registry.ollama_available()
        return
    except Exception:
        pass
    try:
        import subprocess
        subprocess.Popen(
            ["ollama", "serve"],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            creationflags=getattr(subprocess, "DETACHED_PROCESS", 0),
        )
        print("  [警告] Ollama 未响应，已尝试重新拉起，等待 8 秒")
        time.sleep(8)
    except Exception as exc:
        print(f"  [警告] 拉起 Ollama 失败：{exc}")


def ask(client, model, messages, retries=3):
    """调用生成模型；失败时重试并自动恢复 Ollama。"""
    last_error = None
    for attempt in range(1, retries + 1):
        try:
            response = client.chat(
                model=model,
                messages=messages,
                think=False,
                keep_alive=config.LLM_KEEP_ALIVE,
                options={
                    "temperature": config.LLM_TEMPERATURE,
                    "num_ctx": config.LLM_NUM_CTX,
                    "num_predict": config.LLM_NUM_PREDICT,
                },
            )
            message = getattr(response, "message", None)
            if message is not None:
                return message.content or ""
            return response.get("message", {}).get("content", "")
        except Exception as exc:
            last_error = exc
            print(f"  [警告] 第 {attempt} 次生成失败：{type(exc).__name__}，准备重试")
            ensure_ollama()
            time.sleep(3)
    raise RuntimeError(f"生成连续 {retries} 次失败：{last_error}")


def load_thresholds():
    """复用检索层实验标定出的各组拒答阈值。"""
    path = config.EVAL_DIR / "results" / "retrieval_groups.json"
    if not path.exists():
        return {}
    data = load_json(path)
    return {k: v["summary"]["refusal_threshold"] for k, v in data.items()}


def run_group(key, questions, cache, client, model_name, thresholds, limit=None, save=None):
    spec = GROUPS[key]
    engine = RagEngine(use_rerank=spec["rerank"], retrieval_mode=spec["mode"])
    threshold = thresholds.get(key, config.REFUSAL_MIN_SCORE[spec["mode"]])

    items = questions[:limit] if limit else questions
    for item in items:
        if item["id"] in cache:
            continue
        started = time.time()
        result = engine.retrieve(item["question"], min_score=threshold)
        record = {
            "id": item["id"],
            "tier": item["tier"],
            "question": item["question"],
            "refused": result["refused"],
            "files": result["files"],
            "latency_retrieval_ms": result.get("latency_ms", {}).get("total"),
        }
        if result["refused"]:
            record.update({
                "answer": "知识库未收录该问题，请换一种问法。",
                "keywords_found": [],
                "keyword_coverage": None,
                "source_coverage": None,
                "fact_supported": None,
                "fact_total": None,
                "unsupported_facts": [],
                "latency_total_s": round(time.time() - started, 2),
            })
        else:
            context = engine.build_context(result["chunks"])
            answer = ask(client, model_name, engine.build_messages(item["question"], result["chunks"]))
            answer = answer.strip()
            keywords = item["keywords"]
            found = [kw for kw in keywords if kw in answer]
            expected = set(item["sources"])
            record.update({
                "answer": answer,
                "context": context,
                "keywords_found": found,
                "keyword_coverage": (len(found) / len(keywords)) if keywords else None,
                "source_coverage": (len(expected & set(result["files"])) / len(expected)) if expected else None,
                "latency_total_s": round(time.time() - started, 2),
            })
            supported, total, unsupported = fact_check(answer, context)
            record.update({
                "fact_supported": supported,
                "fact_total": total,
                "unsupported_facts": unsupported,
            })
        cache[item["id"]] = record
        if save:
            save()
        print(f"  [{key}] {item['id']} 完成，{record['latency_total_s']}s，拒答={record['refused']}")
    return [cache[q["id"]] for q in items]


def mean(values):
    items = [v for v in values if v is not None]
    return sum(items) / len(items) if items else 0.0


def summarize(group, records):
    answerable = [r for r in records if r["tier"] != "refuse"]
    refusal = [r for r in records if r["tier"] == "refuse"]
    # 事实一致率按当前口径实时重算，避免沿用旧缓存字段
    supported_total = checked_total = 0
    for r in answerable:
        if r.get("context") and r.get("answer"):
            s, t, _ = fact_check(r["answer"], r["context"])
            supported_total += s
            checked_total += t
    return {
        "group": group,
        "label": GROUPS[group]["label"],
        "keyword_coverage": mean([r["keyword_coverage"] for r in answerable]),
        "source_coverage": mean([r["source_coverage"] for r in answerable]),
        "fact_consistency": (
            supported_total / checked_total if checked_total else 0.0
        ),
        "refusal_rate": (
            sum(1 for r in refusal if r["refused"]) / len(refusal) if refusal else 0.0
        ),
        "false_refusal": sum(1 for r in answerable if r["refused"]),
        "latency_total_s": mean([r["latency_total_s"] for r in records]),
    }


def write_faithfulness_sheet(records, path):
    lines = [
        "# 忠实度人工评阅表",
        "",
        "对每题回答逐条核对：回答中的每条结论能否在下方上下文中找到依据。",
        "在「评级」列填：A=全部有依据，B=存在轻微外推，C=存在无依据或与资料矛盾的结论。",
        "",
    ]
    for r in records:
        if r["tier"] == "refuse":
            continue
        lines.append(f"## {r['id']}（{r['tier']}）{r['question']}")
        lines.append("")
        lines.append(f"**回答**：{r['answer']}")
        lines.append("")
        lines.append(f"**评级**： ")
        lines.append("")
        lines.append("<details><summary>上下文</summary>")
        lines.append("")
        lines.append("```")
        lines.append(r.get("context", ""))
        lines.append("```")
        lines.append("</details>")
        lines.append("")
    path.write_text("\n".join(lines), encoding="utf-8")


def main():
    parser = argparse.ArgumentParser(description="生成层 A/D 两组实验")
    parser.add_argument("--groups", default="A,D")
    parser.add_argument("--limit", type=int, default=None, help="每组只跑前 N 题")
    parser.add_argument("--embed", default=config.DEFAULT_EMBED)
    parser.add_argument("--model", default=config.DEFAULT_GENERATION)
    args = parser.parse_args()

    questions = load_json(config.EVAL_DIR / "questions_v2.json")
    thresholds = load_thresholds()
    client, model_name = model_registry.get_generator(args.model)

    out_dir = config.EVAL_DIR / "results"
    out_dir.mkdir(parents=True, exist_ok=True)
    cache_path = out_dir / "generation_cache.json"
    cache_all = load_json(cache_path) if cache_path.exists() else {}

    keys = [k.strip() for k in args.groups.split(",") if k.strip()]

    def save_cache():
        cache_path.write_text(
            json.dumps(cache_all, ensure_ascii=False, indent=2), encoding="utf-8"
        )

    summaries, all_records = [], []
    for key in keys:
        if key not in GROUPS:
            raise SystemExit(f"未知的组：{key}")
        cache = cache_all.setdefault(key, {})
        records = run_group(
            key, questions, cache, client, model_name, thresholds, args.limit, save_cache
        )
        summary = summarize(key, records)
        summaries.append(summary)
        all_records += [dict(r, group=key) for r in records]
        print(f"{key} {summary['label']}: "
              f"要点覆盖={summary['keyword_coverage']:.3f} "
              f"来源覆盖={summary['source_coverage']:.3f} "
              f"事实一致率={summary['fact_consistency']:.3f} "
              f"拒答率={summary['refusal_rate'] * 100:.1f}% "
              f"平均耗时={summary['latency_total_s']:.0f}s")

    (out_dir / "generation_groups.json").write_text(
        json.dumps({"summary": summaries, "records": all_records}, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    lines = [
        "# 生成层两组对比（A 仅向量 / D 混合+重排）",
        "",
        "| 组 | 配置 | 要点覆盖 | 来源覆盖 | 事实一致率 | 拒答正确率 | 误拒题数 | 平均耗时(s) |",
        "|---|---|---|---|---|---|---|---|",
    ]
    for s in summaries:
        lines.append(
            f"| {s['group']} | {s['label']} | {s['keyword_coverage']:.3f} | "
            f"{s['source_coverage']:.3f} | {s['fact_consistency']:.3f} | "
            f"{s['refusal_rate'] * 100:.1f}% | {s['false_refusal']} | "
            f"{s['latency_total_s']:.0f} |"
        )
    (out_dir / "generation_groups.md").write_text("\n".join(lines) + "\n", encoding="utf-8")

    write_faithfulness_sheet(all_records, out_dir / "faithfulness_sheet.md")
    print(f"\n结果已写入：{out_dir}")


if __name__ == "__main__":
    main()
