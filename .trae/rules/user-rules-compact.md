---
alwaysApply: false
description: 
---
# 全局规则紧凑版

> 替换系统提示 `user_rules` 中的冗长规则。详细配置参数与路径体系见 `.trae/rules/config-index.md`。

---

## 一、核心原则

| 原则 | 说明 |
|------|------|
| 语言 | 使用简体中文（回复、注释、日志） |
| 修复原则 | 先分析→最小侵入→兼容优先→验证闭环 |
| 文档同步 | 代码变更后按 `doc-change-mapping.md` 同步相关文档，并更新相关skill配置 |

## 二、调度策略

| 任务特征 | 执行方式 | 路径参考 |
|----------|----------|----------|
| 单文件、单栈、低风险 | 直接执行 | config-index.md §3.1 |
| ≥2文件、跨栈、逻辑变更、需审查、多步骤 | 调度执行 | config-index.md §3.1 |

## 二.一、强制触发技能

| 触发场景 | 必须触发的技能 |
|----------|---------------|
| 多步骤/复杂需求 | `analysis-planner` 或 `writing-plans` |
| 代码编写完成后 | `code-reviewer` |
| 代码变更完成后 | `doc-manager` |
| 存在书面计划 | `executing-plans` 或 `subagent-driven-development` |
| 声称任务完成前 | `verification-before-completion` |
| 遇到异常行为 | `systematic-debugging` |
| 2个以上独立任务 | `dispatching-parallel-agents` |

## 三、异常响应

| 异常 | 响应 |
|------|------|
| 执行失败 | 重试→替换→回退→暂停 |
| 审查不通过 | 修复后重新审查 |
| 范围膨胀/需求变更 | 暂停→重新评估→更新计划 |

## 四、代码底线

- 不添加注释（除非用户要求）
- 不暴露/记录/提交密钥、密码、Token
- 不假设库可用（先确认项目已有该依赖）
- 新代码匹配现有风格
- 删除未使用的导入、变量和函数

## 五、任务管理

- 每个任务独立文件：`.trae/tasks/{YYYYMMDD}-{序号}-{简短名称}.md`
- 状态流转：进行中 → 已完成后归档到 `.trae/memories/`
- 指令：`/git` 提交，`/finish` 归档并从 `tasks/` 移除

## 六、配置索引

| 内容 | 路径 |
|------|------|
| 完整配置索引（参数/策略/路径） | `.trae/rules/config-index.md` |
| 轻量路径索引（始终加载） | `.trae/rules/project_rules_always.md` |
| 智能体配置 | `.trae/rules/agents-config.md` |
| 技能触发规则 | `.trae/rules/skills-trigger-rules.md` |
| 文档变更映射 | `.trae/rules/doc-change-mapping.md` |
| 特定任务规则 | `.trae/rules/project_rules_task_specific.md` |
| 用户规则紧凑版 | `.trae/rules/user-rules-compact.md` |
| Git提交规范 | `.trae/rules/git-commit-message.md` |

