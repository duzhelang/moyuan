# -*- coding: utf-8 -*-
"""墨渊知识问答服务 · 全局配置与模型注册表

本服务独立于《智能生成系统开发》期末作业。
模型策略：默认只用 bge-small-zh-v1.5；其他嵌入模型只注册、不建索引、不实测。
"""
import json
import os
import tempfile
from pathlib import Path

# 使用本地模型缓存，禁止联网下载
os.environ.setdefault("HF_HOME", r"D:\BIG\huggingface")
os.environ.setdefault("HF_HUB_OFFLINE", "1")
os.environ.setdefault("TOKENIZERS_PARALLELISM", "false")

BASE_DIR = Path(__file__).resolve().parent
PROJECT_DIR = BASE_DIR.parent

# ---------------- 语料范围：两份业务文档 + 小助手系统知识库 ----------------
DOC_SOURCES = [
    PROJECT_DIR / "docs" / "business" / "modules.md",
    PROJECT_DIR / "docs" / "business" / "system-workflow.md",
]
SYSTEM_KNOWLEDGE_SQL = (
    PROJECT_DIR / "sc-moyuan-backend" / "src" / "main" / "resources" / "db" / "init.sql"
)

CORPUS_DIR = BASE_DIR / "corpus"
INDEX_ROOT = BASE_DIR / "data" / "index"
EVAL_DIR = BASE_DIR / "eval"
RUNTIME_CONFIG_PATH = BASE_DIR / "data" / "runtime_config.json"

# ---------------- 嵌入模型注册表 ----------------
# status: ready = 本期实测并建索引；reserved = 只保留接口，不建索引、不实测
EMBEDDINGS = {
    "bge-small": {
        "label": "BGE-small-zh-v1.5",
        "provider": "huggingface",
        "path": Path(
            r"D:\BIG\huggingface\hub\models--BAAI--bge-small-zh-v1.5\snapshots"
        ),
        "dim": 512,
        "status": "ready",
        "max_length": 512,
        "query_instruction": "为这个句子生成表示以用于检索相关文章：",
        "note": "默认；CPU 快，4GB 显存友好",
    },
    "jina-clip": {
        "label": "jina-clip-v2",
        "provider": "huggingface",
        "path": Path(r"D:\BIG\jina-clip-v2"),
        "dim": 1024,
        "status": "reserved",
        "max_length": 512,
        "query_instruction": "Represent the query for retrieving evidence documents: ",
        "trust_remote_code": True,
        "note": "已安装完整（1.73GB）；本期不建索引、不实测",
    },
    "qwen3-embed": {
        "label": "qwen3-embedding:0.6b",
        "provider": "ollama",
        "model": "qwen3-embedding:0.6b",
        "dim": 1024,
        "status": "reserved",
        "note": "Ollama 嵌入；仅保留接口，不建索引、不实测",
    },
}

# ---------------- 生成模型注册表 ----------------
GENERATIONS = {
    "qwen3.5": {
        "label": "qwen3.5:4b",
        "model": "qwen3.5:4b",
        "status": "ready",
        "note": "默认生成模型",
    },
    "minicpm5": {
        "label": "maternion/minicpm5:2b-q8_0",
        "model": "maternion/minicpm5:2b-q8_0",
        "status": "ready",
        "note": "备用快速模型",
    },
}

DEFAULT_EMBED = "bge-small"
DEFAULT_GENERATION = "qwen3.5"

# ---------------- 切分参数 ----------------
CHUNK_SIZE = 600
CHUNK_OVERLAP = 80

# ---------------- 检索参数 ----------------
VECTOR_TOP_K = 12
BM25_TOP_K = 12
RRF_K = 60
RERANK_TOP_K = 8
NEIGHBOR_WINDOW = 1
MAX_CONTEXT_CHUNKS = 12
MAX_CONTEXT_CHARS = 5500
MIN_RRF_SCORE = 0.03

# 各检索模式的拒答阈值（口径不同，不能混用）：
# - hybrid 用 RRF 累积分，沿用 MIN_RRF_SCORE；
# - 单路模式用该路 top-1 原始分，评测前由 calibrate.py 依据 30 道可答题重新标定。
REFUSAL_MIN_SCORE = {"vector": 0.0, "bm25": 0.0, "hybrid": MIN_RRF_SCORE}

RERANK_MODEL_DIR = Path(
    r"D:\BIG\huggingface\hub\models--BAAI--bge-reranker-base\snapshots"
)

# 生成参数（4GB 显存下 num_ctx 过大会触发 CPU 卸载，速度骤降）
LLM_TEMPERATURE = 0.1
LLM_NUM_CTX = 4096
LLM_NUM_PREDICT = 400
LLM_KEEP_ALIVE = "30m"

DEFAULT_RUNTIME_CONFIG = {
    "embed": DEFAULT_EMBED,
    "generation": DEFAULT_GENERATION,
    "rerank": True,
    "top_k": RERANK_TOP_K,
}


def find_snapshot(model_dir: Path) -> str:
    """取 HuggingFace 缓存快照路径；若路径本身是完整模型目录则直接返回。"""
    if model_dir.is_dir() and (model_dir / "config.json").exists():
        return str(model_dir)
    if model_dir.exists():
        sub_dirs = sorted(p for p in model_dir.iterdir() if p.is_dir())
        if sub_dirs:
            return str(sub_dirs[-1])
    return str(model_dir)


def index_dir(embed_key: str) -> Path:
    """每个嵌入模型独立索引目录。"""
    return INDEX_ROOT / embed_key


def load_runtime_config() -> dict:
    """读取运行时配置；文件不存在时返回默认值。"""
    config = dict(DEFAULT_RUNTIME_CONFIG)
    if RUNTIME_CONFIG_PATH.exists():
        try:
            data = json.loads(RUNTIME_CONFIG_PATH.read_text(encoding="utf-8"))
            config.update({k: v for k, v in data.items() if k in config})
        except Exception:
            pass
    return config


def save_runtime_config(patch: dict) -> dict:
    """校验并原子写入运行时配置，返回保存后的完整配置。"""
    config = load_runtime_config()
    if "embed" in patch:
        key = patch["embed"]
        if key not in EMBEDDINGS:
            raise ValueError(f"未注册的嵌入模型：{key}")
        if EMBEDDINGS[key]["status"] != "ready":
            raise ValueError(f"嵌入模型 {key} 仅为预留接口，未建索引，不可启用")
        if not (index_dir(key) / "meta.json").exists():
            raise ValueError(f"嵌入模型 {key} 尚未构建索引，请先运行 build_index.py")
        config["embed"] = key
    if "generation" in patch:
        key = patch["generation"]
        if key not in GENERATIONS:
            raise ValueError(f"未注册的生成模型：{key}")
        if GENERATIONS[key]["status"] != "ready":
            raise ValueError(f"生成模型 {key} 不可用")
        config["generation"] = key
    if "rerank" in patch:
        config["rerank"] = bool(patch["rerank"])
    if "top_k" in patch:
        value = int(patch["top_k"])
        if not 4 <= value <= 20:
            raise ValueError("top_k 必须在 4~20 之间")
        config["top_k"] = value

    RUNTIME_CONFIG_PATH.parent.mkdir(parents=True, exist_ok=True)
    fd, tmp_path = tempfile.mkstemp(dir=str(RUNTIME_CONFIG_PATH.parent), suffix=".tmp")
    with os.fdopen(fd, "w", encoding="utf-8") as file:
        json.dump(config, file, ensure_ascii=False, indent=2)
    os.replace(tmp_path, RUNTIME_CONFIG_PATH)
    return config
