# -*- coding: utf-8 -*-
"""构建检索索引：Markdown 结构化切分 → 嵌入向量化 → 向量矩阵 + BM25 词表落盘

每个嵌入模型使用独立目录：data/index/<embed_key>/
本期只构建 bge-small；其他模型在注册表中标记为 reserved，不建索引、不实测。

用法：
    python build_index.py                     # 默认 bge-small
    python build_index.py --embed bge-small
"""
import argparse
import json
import re
import time
from datetime import datetime

import jieba
import numpy as np
from llama_index.core.node_parser import SentenceSplitter

import config
import model_registry

# 语料文件（顺序即引用编号顺序）
FILE_LABELS = [
    "moyuan_business_modules.md",
    "moyuan_system_workflow.md",
    "moyuan_system_knowledge.md",
]
HEADING_RE = re.compile(r"^(#{1,3})\s+(.*)$")
HTML_RE = re.compile(r"<[^>]+>")


def clean_text(text):
    """去 HTML 标签、压缩连续空行，保留 Markdown 结构与表格。"""
    text = HTML_RE.sub("", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


def split_sections(text):
    """按 Markdown 标题切分为 [(章节路径, 文本)]。"""
    sections, buffer, path = [], [], []
    for line in text.splitlines():
        match = HEADING_RE.match(line)
        if match:
            if buffer:
                sections.append((" / ".join(t for _, t in path), "\n".join(buffer).strip()))
                buffer = []
            level = len(match.group(1))
            path = [(lv, t) for lv, t in path if lv < level]
            path.append((level, match.group(2).strip()))
            buffer.append(line)
        else:
            buffer.append(line)
    if buffer:
        sections.append((" / ".join(t for _, t in path), "\n".join(buffer).strip()))
    return sections


def chunk_markdown(text, file_name, splitter):
    """章节级切分，返回带元数据的文本块列表。"""
    chunks = []
    for section, content in split_sections(clean_text(text)):
        if len(content) < 25:
            continue
        heading = f"【章节：{section}】\n" if section else ""
        if len(content) <= 1200:
            parts = [content]
        else:
            parts = [p for p in splitter.split_text(content) if p.strip()]
        for part in parts:
            chunks.append({
                "file": file_name,
                "section": section or "（无章节标题）",
                "text": (heading + part).strip(),
            })
    return chunks


def tokenize(text):
    """jieba 搜索模式分词，过滤空白词。"""
    return [t for t in jieba.lcut_for_search(text) if t.strip()]


def main():
    parser = argparse.ArgumentParser(description="构建本地知识库索引")
    parser.add_argument(
        "--embed", default=config.load_runtime_config()["embed"], help="嵌入模型 key"
    )
    args = parser.parse_args()

    embed_key = args.embed
    if embed_key not in config.EMBEDDINGS:
        raise SystemExit(f"未注册的嵌入模型：{embed_key}")
    spec = config.EMBEDDINGS[embed_key]
    if spec["status"] != "ready":
        raise SystemExit(
            f"{embed_key}（{spec['label']}）仅保留接口，本期不建索引、不实测"
        )

    output_dir = config.index_dir(embed_key)
    output_dir.mkdir(parents=True, exist_ok=True)
    splitter = SentenceSplitter(
        chunk_size=config.CHUNK_SIZE, chunk_overlap=config.CHUNK_OVERLAP
    )

    # 1. 读取并切分语料
    chunks, stats = [], {}
    for name in FILE_LABELS:
        path = config.CORPUS_DIR / name
        if not path.exists():
            raise SystemExit(f"语料缺失：{path}\n请先运行 python corpus_export.py")
        before = len(chunks)
        chunks.extend(chunk_markdown(path.read_text(encoding="utf-8"), name, splitter))
        stats[name] = len(chunks) - before
        print(f"  {name:<32} 文本块 {stats[name]}")

    # 2. 为每块记录文件内序号，供查询时做邻居扩展
    for i, chunk in enumerate(chunks):
        chunk["id"] = i
        chunk["chunk_index"] = sum(1 for c in chunks[:i] if c["file"] == chunk["file"])

    # 3. 向量化
    print(f"\n嵌入模型：{embed_key}（{spec['label']}，{spec['provider']}）")
    embedder = model_registry.get_embedder(embed_key)
    started = time.time()
    embeddings = np.array(
        embedder.get_text_embedding_batch([c["text"] for c in chunks], show_progress=True),
        dtype="float32",
    )
    embeddings /= np.linalg.norm(embeddings, axis=1, keepdims=True) + 1e-8
    actual_dim = int(embeddings.shape[1])
    if actual_dim != spec["dim"]:
        raise SystemExit(
            f"维度不匹配：模型输出 {actual_dim} 维，注册表记录 {spec['dim']} 维"
        )
    print(f"向量化完成：{embeddings.shape}，耗时 {time.time() - started:.1f} 秒")

    # 4. 落盘
    (output_dir / "chunks.json").write_text(
        json.dumps(chunks, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    np.save(output_dir / "embeddings.npy", embeddings)
    tokens = [tokenize(c["text"]) for c in chunks]
    (output_dir / "tokens.json").write_text(
        json.dumps(tokens, ensure_ascii=False), encoding="utf-8"
    )
    meta = {
        "构建时间": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
        "嵌入模型": embed_key,
        "模型名称": spec["label"],
        "模型提供方": spec["provider"],
        "文本块数": len(chunks),
        "向量维度": actual_dim,
        "各文件文本块数": stats,
        "语料总字数": sum(len(c["text"]) for c in chunks),
        "切分参数": {
            "chunk_size": config.CHUNK_SIZE,
            "chunk_overlap": config.CHUNK_OVERLAP,
        },
    }
    (output_dir / "meta.json").write_text(
        json.dumps(meta, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    print(f"索引已保存：{output_dir}")


if __name__ == "__main__":
    main()
