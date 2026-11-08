# -*- coding: utf-8 -*-
"""语料导出：业务文档 + system_knowledge 24 条 → corpus/*.md

用法：
    python corpus_export.py
"""
import re
import shutil

import config

CORPUS_FILES = [
    (config.DOC_SOURCES[0], "moyuan_business_modules.md"),
    (config.DOC_SOURCES[1], "moyuan_system_workflow.md"),
]

EXTRA_EXTENSIONS = {".md", ".txt"}


def export_extra_docs():
    """把 docs 指定目录与 corpus_local 的本地文件复制进 corpus（统一 local__ 前缀）。"""
    # 清理上一轮导出的本地副本，避免已删除的文件残留
    for stale in config.CORPUS_DIR.glob("local__*"):
        if stale.is_file():
            stale.unlink()

    exported, skipped = 0, set()
    for directory in config.EXTRA_DOC_DIRS:
        if not directory.exists():
            continue
        for path in sorted(directory.rglob("*")):
            if path.suffix.lower() not in EXTRA_EXTENSIONS or not path.is_file():
                continue
            resolved = path.resolve()
            if resolved in skipped or resolved in {s.resolve() for s, _ in CORPUS_FILES}:
                continue
            skipped.add(resolved)
            rel = path.relative_to(config.PROJECT_DIR).as_posix().replace("/", "__")
            target = config.CORPUS_DIR / f"local__{rel}"
            shutil.copy2(path, target)
            exported += 1

    if config.LOCAL_DOC_DIR.exists():
        for path in sorted(config.LOCAL_DOC_DIR.rglob("*")):
            if path.suffix.lower() not in EXTRA_EXTENSIONS or not path.is_file():
                continue
            rel = path.relative_to(config.LOCAL_DOC_DIR).as_posix().replace("/", "__")
            target = config.CORPUS_DIR / f"local__user__{rel}"
            shutil.copy2(path, target)
            exported += 1
    return exported


def extract_insert_block(sql: str, table: str) -> str:
    """截取指定表的 INSERT ... VALUES 语句（到分号结束）。"""
    pattern = re.compile(
        rf"INSERT\s+(?:IGNORE\s+)?INTO\s+`{table}`.*?VALUES(.*?);",
        re.S | re.I,
    )
    match = pattern.search(sql)
    return match.group(1) if match else ""


def extract_rows(values: str) -> list[str]:
    """按括号层级提取每一行（忽略字符串内部的括号与逗号）。"""
    rows, buf, in_str, depth = [], "", False, 0
    i = 0
    while i < len(values):
        ch = values[i]
        if in_str:
            if ch == "'":
                if i + 1 < len(values) and values[i + 1] == "'":
                    buf += "''"
                    i += 2
                    continue
                in_str = False
            buf += ch
        elif ch == "'":
            in_str = True
            buf += ch
        elif ch == "(":
            depth += 1
            if depth == 1:
                buf = ""
            else:
                buf += ch
        elif ch == ")":
            depth -= 1
            if depth == 0:
                rows.append(buf)
            else:
                buf += ch
        elif depth >= 1:
            buf += ch
        i += 1
    return rows


def split_fields(row: str) -> list[str]:
    """按顶层逗号切分字段，字符串内的逗号不切。"""
    fields, buf, in_str = [], "", False
    i = 0
    while i < len(row):
        ch = row[i]
        if in_str:
            if ch == "'":
                if i + 1 < len(row) and row[i + 1] == "'":
                    buf += "''"
                    i += 2
                    continue
                in_str = False
            buf += ch
        elif ch == "'":
            in_str = True
            buf += ch
        elif ch == ",":
            fields.append(buf.strip())
            buf = ""
        else:
            buf += ch
        i += 1
    fields.append(buf.strip())
    return fields


def unquote(value: str) -> str:
    """去掉 SQL 单引号并还原转义；NULL 返回空串。"""
    value = value.strip()
    if not value or value.upper() == "NULL":
        return ""
    if value.startswith("'") and value.endswith("'"):
        value = value[1:-1]
    return value.replace("''", "'")


def parse_system_knowledge(sql: str) -> list[dict]:
    """解析 system_knowledge 表的种子数据。"""
    rows = extract_rows(extract_insert_block(sql, "system_knowledge"))
    items = []
    for row in rows:
        fields = split_fields(row)
        if len(fields) < 8:
            continue
        code, title, category, keywords, aliases, body, steps, faq, route, _, sort_order = (
            fields + [""] * 11
        )[:11]
        items.append({
            "code": unquote(code),
            "title": unquote(title),
            "category": unquote(category),
            "keywords": unquote(keywords),
            "aliases": unquote(aliases),
            "body": unquote(body),
            # SQL 中以字面 \n 表示换行，这里还原为真实换行
            "steps": unquote(steps).replace("\\n", "\n"),
            "faq": unquote(faq),
            "route": unquote(route),
            "sort_order": unquote(sort_order),
        })
    return items


def render_knowledge_md(items: list[dict]) -> str:
    """渲染为按条目分节的知识库文档（每节一个文本块，便于精确检索）。"""
    lines = [
        "# 墨渊小助手系统知识库（24 条）",
        "",
        "> 数据来源：sc-moyuan-backend/src/main/resources/db/init.sql 中 system_knowledge 表的种子数据，"
        f"共 {len(items)} 条。该知识库是墨渊 AI 小助手当前实际使用的系统用法知识，覆盖玩法、页面、功能与账号四类。",
        "",
    ]
    for item in items:
        lines.append(f"## {item['title']}（{item['code']}）")
        lines.append("")
        lines.append(f"- 分类：{item['category']}")
        lines.append(f"- 关键词：{item['keywords']}")
        if item["aliases"]:
            lines.append(f"- 别名：{item['aliases']}")
        if item["route"]:
            lines.append(f"- 前端路由：{item['route']}")
        lines.append(f"- 说明：{item['body']}")
        steps = [s.strip() for s in item["steps"].splitlines() if s.strip()]
        if steps:
            lines.append("- 使用步骤：")
            lines.extend(f"  {i}. {s}" for i, s in enumerate(steps, 1))
        if item["faq"]:
            lines.append("- 常见问答：")
            for qa in re.findall(r'"q":"(.*?)","a":"(.*?)"', item["faq"]):
                lines.append(f"  - 问：{qa[0]}  答：{qa[1]}")
        lines.append("")
    return "\n".join(lines)


def main():
    config.CORPUS_DIR.mkdir(parents=True, exist_ok=True)

    # 1. 复制业务文档（保持原文结构，便于按标题切分）
    for src, name in CORPUS_FILES:
        if not src.exists():
            raise FileNotFoundError(f"语料缺失：{src}")
        shutil.copy2(src, config.CORPUS_DIR / name)
        print(f"已导出 {name}（{src.stat().st_size} 字节）")

    # 2. 解析 system_knowledge
    sql = config.SYSTEM_KNOWLEDGE_SQL.read_text(encoding="utf-8")
    items = parse_system_knowledge(sql)
    if not items:
        raise RuntimeError("未解析到 system_knowledge 数据，请检查 init.sql")
    text = render_knowledge_md(items)
    out = config.CORPUS_DIR / "moyuan_system_knowledge.md"
    out.write_text(text, encoding="utf-8")
    print(f"已导出 moyuan_system_knowledge.md（{len(items)} 条，{len(text)} 字）")

    # 3. 追加本地文档（docs 指定目录 + corpus_local 投放目录）
    count = export_extra_docs()
    print(f"已追加本地文档 {count} 个（前缀 local__）")


if __name__ == "__main__":
    main()
