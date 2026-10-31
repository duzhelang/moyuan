# -*- coding: utf-8 -*-
"""墨渊知识问答服务 · FastAPI 接口层

启动：
    python app.py                      # 127.0.0.1:8010
    uvicorn app:app --reload --port 8010

接口：
    GET  /rag/health
    GET  /rag/models
    GET  /rag/config
    PUT  /rag/config                  管理员模型配置（前端后台页使用）
    POST /rag/ask                     同步回答
    POST /rag/ask/stream              SSE 流式回答
"""
import json
import time

import uvicorn
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field

import config
import model_registry
from rag_engine import RagEngine

app = FastAPI(title="墨渊知识问答服务", version="1.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://localhost:4173",
    ],
    allow_methods=["*"],
    allow_headers=["*"],
)

# 引擎按 (嵌入模型, 是否重排) 缓存，避免每次请求重新加载模型
_engines = {}


class AskRequest(BaseModel):
    question: str = Field(min_length=1, max_length=500)
    embed: str | None = None
    generation: str | None = None
    top_k: int | None = Field(default=None, ge=4, le=20)
    rerank: bool | None = None


class ConfigUpdate(BaseModel):
    embed: str | None = None
    generation: str | None = None
    rerank: bool | None = None
    top_k: int | None = Field(default=None, ge=4, le=20)


def get_engine(embed_key: str, rerank: bool) -> RagEngine:
    cache_key = (embed_key, rerank)
    if cache_key not in _engines:
        _engines[cache_key] = RagEngine(embed_key=embed_key, use_rerank=rerank)
    return _engines[cache_key]


def source_payload(result):
    return [
        {
            "file": c["file"],
            "section": c["section"],
            "vector": round(c["vector"], 3),
            "bm25": round(c["bm25"], 2),
            "rerank": round(c["rerank"], 3),
            "neighbor": c["neighbor"],
        }
        for c in result["chunks"]
    ]


def content_of(part) -> str:
    """兼容 ollama 客户端不同版本的返回结构。"""
    message = getattr(part, "message", None)
    if message is not None:
        return message.content or ""
    return part.get("message", {}).get("content", "")


def sse(event: str, payload: dict) -> str:
    return f"event: {event}\ndata: {json.dumps(payload, ensure_ascii=False)}\n\n"


def resolve_request(req: AskRequest):
    """解析请求参数，返回运行时配置、引擎与检索结果。"""
    runtime = config.load_runtime_config()
    embed_key = req.embed or runtime["embed"]
    generation_key = req.generation or runtime["generation"]
    rerank = runtime["rerank"] if req.rerank is None else req.rerank

    try:
        engine = get_engine(embed_key, rerank)
    except Exception as exc:
        raise HTTPException(status_code=400, detail=f"嵌入模型不可用：{exc}")
    try:
        client, model_name = model_registry.get_generator(generation_key)
    except Exception as exc:
        raise HTTPException(status_code=400, detail=f"生成模型不可用：{exc}")

    result = engine.retrieve(req.question, req.top_k)
    return runtime, engine, client, model_name, result


@app.get("/rag/health")
def health():
    runtime = config.load_runtime_config()
    status = model_registry.index_status(runtime["embed"])
    return {
        "status": "ok",
        "embed": runtime["embed"],
        "generation": runtime["generation"],
        "index_ready": status["ready"],
        "chunks": status["chunks"],
        "ollama": model_registry.ollama_available(),
    }


@app.get("/rag/models")
def models():
    return {
        "config": config.load_runtime_config(),
        **model_registry.models_payload(),
    }


@app.get("/rag/config")
def get_config():
    return {
        "config": config.load_runtime_config(),
        **model_registry.models_payload(),
    }


@app.put("/rag/config")
def update_config(payload: ConfigUpdate):
    try:
        saved = config.save_runtime_config(payload.model_dump(exclude_none=True))
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc))
    _engines.clear()  # 配置变更后重新加载引擎
    return {"config": saved, "message": "配置已保存"}


@app.post("/rag/ask")
def ask(req: AskRequest):
    started = time.time()
    runtime, engine, client, model_name, result = resolve_request(req)
    if result["refused"]:
        return {
            "answer": "知识库未收录该问题，请换一种问法。",
            "refused": True,
            "sources": [],
            "embed": engine.embed_key,
            "generation": model_name,
            "elapsed": round(time.time() - started, 2),
        }

    messages = engine.build_messages(req.question, result["chunks"])
    response = client.chat(
        model=model_name,
        messages=messages,
        think=False,
        keep_alive=config.LLM_KEEP_ALIVE,
        options={
            "temperature": config.LLM_TEMPERATURE,
            "num_ctx": config.LLM_NUM_CTX,
            "num_predict": config.LLM_NUM_PREDICT,
        },
    )
    return {
        "answer": content_of(response).strip(),
        "refused": False,
        "sources": source_payload(result),
        "embed": engine.embed_key,
        "generation": model_name,
        "elapsed": round(time.time() - started, 2),
    }


@app.post("/rag/ask/stream")
def ask_stream(req: AskRequest):
    started = time.time()
    runtime, engine, client, model_name, result = resolve_request(req)

    if result["refused"]:
        return StreamingResponse(
            iter([
                sse("meta", {"sources": [], "embed": engine.embed_key, "generation": model_name}),
                sse("delta", {"text": "知识库未收录该问题，请换一种问法。"}),
                sse("done", {"elapsed": round(time.time() - started, 2)}),
            ]),
            media_type="text/event-stream",
            headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
        )

    messages = engine.build_messages(req.question, result["chunks"])
    sources = source_payload(result)

    def generate():
        yield sse("meta", {
            "sources": sources,
            "embed": engine.embed_key,
            "generation": model_name,
        })
        try:
            for part in client.chat(
                model=model_name,
                messages=messages,
                stream=True,
                think=False,
                keep_alive=config.LLM_KEEP_ALIVE,
                options={
                    "temperature": config.LLM_TEMPERATURE,
                    "num_ctx": config.LLM_NUM_CTX,
                    "num_predict": config.LLM_NUM_PREDICT,
                },
            ):
                text = content_of(part)
                if text:
                    yield sse("delta", {"text": text})
            yield sse("done", {"elapsed": round(time.time() - started, 2)})
        except Exception as exc:
            yield sse("error", {"message": str(exc)})

    return StreamingResponse(
        generate(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )


if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=8010)
