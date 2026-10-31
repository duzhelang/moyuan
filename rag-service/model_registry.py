# -*- coding: utf-8 -*-
"""模型注册表的运行时解析：嵌入模型、生成模型、索引状态

说明：
- 本期只实际启用 bge-small（HuggingFace）；
- jina-clip / qwen3-embed 只保留适配代码，不做索引、不做实测；
- 生成侧统一使用 ollama Python 客户端，不使用 llama_index.llms.ollama。
"""
import json
from typing import Any

import ollama
from llama_index.embeddings.huggingface import HuggingFaceEmbedding

import config


class OllamaEmbedder:
    """Ollama 嵌入模型适配器（预留给 qwen3-embed，本期不建索引）。"""

    def __init__(self, model: str, dim: int):
        self.client = ollama.Client()
        self.model = model
        self.dim = dim

    def get_text_embedding_batch(self, texts, show_progress=False):
        response = self.client.embed(model=self.model, input=list(texts))
        return response.embeddings

    def get_query_embedding(self, text):
        return self.get_text_embedding_batch([text])[0]


def get_embedder(embed_key: str):
    """按注册表创建嵌入模型实例。"""
    if embed_key not in config.EMBEDDINGS:
        raise ValueError(f"未注册的嵌入模型：{embed_key}")
    spec = config.EMBEDDINGS[embed_key]
    if spec["status"] != "ready":
        raise ValueError(
            f"嵌入模型 {embed_key}（{spec['label']}）仅为预留接口，本期未启用"
        )
    if spec["provider"] == "ollama":
        return OllamaEmbedder(spec["model"], spec["dim"])
    return HuggingFaceEmbedding(
        model_name=config.find_snapshot(spec["path"]),
        device="cpu",
        max_length=spec.get("max_length", 512),
        query_instruction=spec.get("query_instruction"),
        trust_remote_code=spec.get("trust_remote_code", False),
    )


def get_generator(generation_key: str) -> tuple[Any, str]:
    """返回 (ollama.Client, 实际模型名)；生成侧不依赖 LlamaIndex。"""
    if generation_key not in config.GENERATIONS:
        raise ValueError(f"未注册的生成模型：{generation_key}")
    spec = config.GENERATIONS[generation_key]
    if spec["status"] != "ready":
        raise ValueError(f"生成模型 {generation_key} 不可用")
    return ollama.Client(), spec["model"]


def read_index_meta(embed_key: str) -> dict:
    """读取索引元信息；不存在返回空 dict。"""
    meta_path = config.index_dir(embed_key) / "meta.json"
    if not meta_path.exists():
        return {}
    try:
        return json.loads(meta_path.read_text(encoding="utf-8"))
    except Exception:
        return {}


def index_status(embed_key: str) -> dict:
    """索引就绪状态：存在、维度是否匹配。"""
    meta = read_index_meta(embed_key)
    if not meta:
        return {"ready": False, "chunks": 0, "index_dim": 0, "message": "尚未构建索引"}
    spec = config.EMBEDDINGS.get(embed_key, {})
    expected_dim = spec.get("dim")
    actual_dim = int(meta.get("向量维度", 0))
    if expected_dim and actual_dim != expected_dim:
        return {
            "ready": False,
            "chunks": int(meta.get("文本块数", 0)),
            "index_dim": actual_dim,
            "message": f"维度不匹配：索引 {actual_dim}，模型 {expected_dim}",
        }
    return {
        "ready": True,
        "chunks": int(meta.get("文本块数", 0)),
        "index_dim": actual_dim,
        "message": "索引可用",
    }


def models_payload() -> dict:
    """管理员配置页使用的模型清单。"""
    embeddings = []
    for key, spec in config.EMBEDDINGS.items():
        embeddings.append({
            "key": key,
            "label": spec["label"],
            "provider": spec["provider"],
            "dim": spec["dim"],
            "status": spec["status"],
            "note": spec.get("note", ""),
            **index_status(key),
        })
    generations = [
        {
            "key": key,
            "label": spec["label"],
            "model": spec["model"],
            "status": spec["status"],
            "note": spec.get("note", ""),
        }
        for key, spec in config.GENERATIONS.items()
    ]
    return {"embeddings": embeddings, "generations": generations}


def ollama_available() -> bool:
    try:
        ollama.Client().list()
        return True
    except Exception:
        return False
