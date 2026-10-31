# -*- coding: utf-8 -*-
"""命令行问答入口（生成侧使用 ollama 客户端）

用法：
    python ask.py                              # 交互式问答
    python ask.py --once "平台诗词评分有哪两种方式？"
    python ask.py --once "..." --retrieve-only      # 只检索，不调用大模型
    python ask.py --once "..." --no-rerank          # 关闭重排（对比实验）
    python ask.py --once "..." --embed bge-small
    python ask.py --once "..." --model minicpm5     # 切换生成模型
"""
import argparse

import ollama

import config
import model_registry
from rag_engine import RagEngine


def print_retrieval(result, show_detail=True):
    print(
        f"\n[检索] 子问题 {len(result['queries'])} 个 | "
        f"上下文 {len(result['chunks'])} 块 | "
        f"覆盖文件 {len(result['files'])} 个 | 最佳融合分 {result['best_rrf']:.3f}"
    )
    if not show_detail:
        return
    print("[来源]")
    for i, chunk in enumerate(result["chunks"], 1):
        tag = "（相邻块）" if chunk["neighbor"] else ""
        print(
            f"  {i}. {chunk['file']} · {chunk['section']}{tag}\n"
            f"     向量 {chunk['vector']:.3f} / BM25 {chunk['bm25']:.2f} / "
            f"重排 {chunk['rerank']:.3f} / 命中 {chunk['hits']} 个子问题"
        )


def chunk_text(part):
    """兼容 ollama 客户端不同版本的返回结构。"""
    message = getattr(part, "message", None)
    if message is not None:
        return message.content or ""
    return part.get("message", {}).get("content", "")


def stream_answer(client, model, messages):
    response = client.chat(
        model=model,
        messages=messages,
        stream=True,
        think=False,
        keep_alive=config.LLM_KEEP_ALIVE,
        options={
            "temperature": config.LLM_TEMPERATURE,
            "num_ctx": config.LLM_NUM_CTX,
            "num_predict": config.LLM_NUM_PREDICT,
        },
    )
    answer = ""
    for part in response:
        delta = chunk_text(part)
        answer += delta
        print(delta, end="", flush=True)
    return answer


def main():
    runtime = config.load_runtime_config()
    parser = argparse.ArgumentParser(description="墨渊知识库问答")
    parser.add_argument("--once", help="单次提问的问题")
    parser.add_argument("--retrieve-only", action="store_true", help="只检索，不调用大模型")
    parser.add_argument("--no-rerank", action="store_true", help="关闭重排")
    parser.add_argument("--show-context", action="store_true", help="打印送入模型的上下文")
    parser.add_argument("--embed", default=runtime["embed"], help="嵌入模型 key")
    parser.add_argument("--model", default=runtime["generation"], help="生成模型 key")
    args = parser.parse_args()

    engine = RagEngine(embed_key=args.embed, use_rerank=not args.no_rerank)
    client, model_name = None, ""
    if not args.retrieve_only:
        if args.model in config.GENERATIONS:
            client, model_name = model_registry.get_generator(args.model)
        else:
            client, model_name = ollama.Client(), args.model

    print("=" * 68)
    print(
        f"墨渊知识库 · 嵌入 {args.embed} · 生成 {model_name or '未加载（仅检索）'}"
    )
    print("向量召回 + BM25 召回 → RRF 融合 → 重排 → 邻居扩展 → 来源覆盖")
    print("=" * 68)

    if args.once:
        result = engine.retrieve(args.once)
        if result["refused"]:
            print(f"\n知识库未收录该问题（最佳融合分 {result['best_rrf']:.3f}）。")
            return
        print_retrieval(result)
        messages = engine.build_messages(args.once, result["chunks"])
        if args.show_context:
            print("\n[上下文]\n" + engine.build_context(result["chunks"]))
        if client is not None:
            print("\n墨渊 > ", end="", flush=True)
            stream_answer(client, model_name, messages)
            print()
        return

    history = []
    show_detail = True
    while True:
        try:
            question = input("\n你问 > ").strip()
        except (EOFError, KeyboardInterrupt):
            print("\n再见。")
            return
        if not question:
            continue
        if question in ("/exit", "/quit"):
            print("再见。")
            return
        if question == "/new":
            history = []
            print("已清空对话。")
            continue
        if question == "/sources":
            show_detail = not show_detail
            print(f"来源明细已{'开启' if show_detail else '关闭'}。")
            continue
        if question == "/help":
            print("直接输入问题；/new 清空对话；/sources 切换来源明细；/exit 退出。")
            continue

        result = engine.retrieve(question)
        if result["refused"]:
            print(f"\n知识库未收录该问题（最佳融合分 {result['best_rrf']:.3f}）。")
            continue
        print_retrieval(result, show_detail)
        if client is None:
            continue
        messages = [engine.build_messages(question, result["chunks"])[0]]
        messages.extend(history)
        messages.append(engine.build_messages(question, result["chunks"])[1])
        print("\n墨渊 > ", end="", flush=True)
        try:
            answer = stream_answer(client, model_name, messages)
            print()
            history.extend([
                {"role": "user", "content": question},
                {"role": "assistant", "content": answer},
            ])
            history = history[-6:]
        except Exception as exc:
            print(f"\n[错误] {exc}")
            print("[排查] 确认 Ollama 已启动（ollama serve）且模型存在（ollama list）")


if __name__ == "__main__":
    main()
