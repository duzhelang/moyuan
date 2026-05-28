---
alwaysApply: true
description: 必用规则
---

# 古今诗话——墨渊 项目必用规则

> 每次任务执行时必须遵循。详细配置见各专用文件。

## 编码前置检查

1. **技能检查**：调用 `find-skills` skill 检索适用技能
2. **编码规范**：调用 `karpathy-guidelines` skill 避免常见编码错误
3. **文档查阅**：调用 `doc-reader` skill 查阅项目技术文档

## 配置与文档索引

| 内容 | 路径 |
|------|------|
| 项目架构文档定位 | `moyuan-architecture` skill |
| 数据资产文档定位 | `moyuan-data-assets` skill |
| 技术栈与开发规范 | `docs/constraints/tech-stack-constraints.md` |
| 智能体配置 | [agents-config.md](agents-config.md) |
| 技能触发规则 | [skills-trigger-rules.md](skills-trigger-rules.md) |

## 智能体协作流程

分析规划师 → 代码开发 → 代码审查 → 文档管理 → 分析规划师汇总

> 详细配置见 [agents-config.md](agents-config.md)

## 任务与持久化

| 类型 | 路径 |
|------|------|
| 任务文件 | `task.md`（项目根目录） |
| 记忆存储 | `.trae/memories/xx-xx(年-月)/xx(日)/xxxx(时间)-uuid.md` |
