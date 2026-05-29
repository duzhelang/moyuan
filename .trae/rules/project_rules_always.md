---
alwaysApply: true
description: 必用规则 - 轻量级路径索引
---

# 墨渊项目路径索引

## 编码前置检查

1. **技能检查**：调用 `find-skills` skill 检索适用技能
2. **编码规范**：调用 `karpathy-guidelines` skill 避免常见编码错误
3. **文档查阅**：调用 `doc-reader` skill 查阅项目技术文档

## 强制触发速查

| 场景 | 必须触发 |
|------|----------|
| 多步骤/复杂需求/长任务 | `analysis-planner` 或 `writing-plans` |
| 存在书面执行计划 | `executing-plans` 或 `subagent-driven-development` |
| 3+ 独立任务 | `dispatching-parallel-agents` |
| 代码编写完成后 | `code-reviewer` |
| 代码变更完成后 | `doc-manager` |
| 声称完成前 | `verification-before-completion` |
| 遇到异常/bug | `systematic-debugging` |

> 详细规则见 [skills-trigger-rules.md](skills-trigger-rules.md)。

## 任务持久化

| 内容 | 路径 |
|------|------|
| 进行中任务 | `.trae/tasks/{日期}-{序号}-{简短名称}.md`（每个长任务独立文件） |
| 已完成归档 | 任务完成后从 `tasks/` 移动到记忆目录 |
| 记忆存储 | `.trae/memories/{年-月}/{日}/{时间}-{uuid}.md` |

**任务生命周期**：创建任务方案 → 写入 `.trae/tasks/` → 多智能体协作执行 → 完成后归档到 `.trae/memories/` 并从 `tasks/` 删除

## 文档自动更新

代码变更涉及 API 端点、数据库表结构、前端类型定义时，必须同步更新 `docs/` 下对应文档，并更新相关skill配置（`moyuan-architecture`、`moyuan-data-assets`、`doc-manager`）。

> 本文件为轻量级路径索引。详细配置参数、策略及完整路径体系见 [config-index.md](config-index.md)。
