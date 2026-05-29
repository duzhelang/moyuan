---
alwaysApply: false
description: 
---
# 墨渊项目全局配置索引

> 本文件为项目所有配置参数、路径信息与调整策略的系统化汇总。配置变更时须同步更新本文件。

---

## 一、路径体系索引

### 1.1 规则文件路径

| 文件 | 功能 | 加载方式 |
|------|------|----------|
| `.trae/rules/project_rules_always.md` | 轻量路径索引（始终加载） | alwaysApply |
| `.trae/rules/project_rules_task_specific.md` | 特定任务规则 | 按需加载 |
| `.trae/rules/agents-config.md` | 智能体配置 | 按需加载 |
| `.trae/rules/skills-trigger-rules.md` | 技能触发规则 | 按需加载 |
| `.trae/rules/doc-change-mapping.md` | 文档变更映射 | 按需加载 |
| `.trae/rules/git-commit-message.md` | Git提交规范 | 按需加载 |
| `.trae/rules/user-rules-compact.md` | 用户规则紧凑版 | 按需加载 |
| `.trae/rules/config-index.md` | 配置索引（本文件） | 按需加载 |

### 1.2 项目文档路径

| 分类 | 路径 |
|------|------|
| 架构文档 | `docs/architecture/system-architecture.md` |
| API文档 | `docs/api/endpoints.md`、`docs/api/auth.md` |
| 数据库文档 | `docs/database/schema.md` |
| 业务模块文档 | `docs/business/modules.md` |
| 前端规范 | `docs/standards/frontend-standards.md` |
| 后端规范 | `docs/standards/backend-standards.md` |
| 数据库规范 | `docs/standards/database-standards.md` |
| 技术栈约束 | `docs/constraints/tech-stack-constraints.md` |
| 开发指南 | `docs/guides/development-guide.md` |
| 迁移指南 | `docs/migration/migration-guide.md` |

### 1.3 任务与记忆路径

| 内容 | 路径 |
|------|------|
| 进行中任务 | `.trae/tasks/{日期}-{序号}-{简短名称}.md`（每个任务独立文件） |
| 已完成归档 | 任务完成后从 `tasks/` 移动到记忆目录 |
| 记忆存储 | `.trae/memories/{年-月}/{日}/{时间}-{uuid}.md` |

**任务文件命名规则**：`{YYYYMMDD}-{序号}-{简短名称}.md`，序号从 01 开始递增
**任务生命周期**：创建方案 → 写入 `tasks/` → 执行 → 完成后归档到 `memories/` 并从 `tasks/` 删除
**并发安全**：每个任务独立文件，多智能体操作不同任务不会冲突

### 1.4 智能体路径

| 智能体 | 代号 | 配置路径 |
|--------|------|----------|
| 分析规划师 | super-brain | `.trae/agents/analysis-planner/AGENT.md` |
| 代码开发 | developer | `.trae/agents/developer/AGENT.md` |
| 代码审查 | code-reviewer | `.trae/agents/code-reviewer/AGENT.md` |
| 文档管理 | doc-manager | `.trae/agents/doc-manager/AGENT.md` |
| 后端架构 | backend-architect | `.trae/agents/backend-architect/AGENT.md` |
| API测试 | api-test-pro | `.trae/agents/api-test-pro/AGENT.md` |
| 前端测试 | frontend-test | `.trae/agents/frontend-test/AGENT.md` |
| UI设计师 | ui-designer | `.trae/agents/ui-designer/AGENT.md` |
| 本地测试 | local-fullstack-tester | `.trae/agents/local-fullstack-tester/AGENT.md` |

### 1.5 测试与环境配置

| 内容 | 路径/值 |
|------|---------|
| 测试环境配置 | `.trae/test-env.yml` |
| 前端端口 | `8080` |
| 后端端口 | `9090` |
| 登录接口 | `/user/login`（POST） |
| Token传递方式 | 请求头 `token` 字段 |
| 代理配置 | `/api` → `http://localhost:9090` |

---

## 二、配置参数汇总

### 2.1 任务调度参数

| 参数 | 值 | 说明 |
|------|-----|------|
| 直接执行阈值 | 单文件、单栈、低风险 | 符合条件时调度器直接执行 |
| 调度执行阈值 | ≥2文件、涉及逻辑变更、需审查、需文档同步 | 需调度智能体参与 |
| 技能触发策略 | 匹配→**必须立即自动调用**；多匹配→按流程顺序调用；不匹配→直接执行 | 见 `skills-trigger-rules.md` |
| 技能禁止事项 | 不强行调用、不重复调用（后续阶段可再调） | 任务完成优先 |
| 强制触发场景 | 多步骤需求→analysis-planner/writing-plans；代码完成后→code-reviewer；变更后→doc-manager；完成前→verification-before-completion；异常→systematic-debugging | 不允许跳过 |

### 2.2 文档变更映射参数

| 变更类型 | 需检查文档 |
|----------|-----------|
| Vue组件 | `docs/standards/frontend-standards.md`、`docs/architecture/system-architecture.md` |
| Composable | `docs/standards/frontend-standards.md`、`docs/architecture/system-architecture.md` |
| 页面视图 | `docs/architecture/system-architecture.md`、`docs/business/modules.md` |
| 后端API | `docs/api/endpoints.md`、`docs/api/auth.md`、`docs/standards/backend-standards.md` |
| 数据库表 | `docs/database/schema.md`、`docs/standards/database-standards.md` |
| 业务模块 | `docs/business/modules.md`、`docs/architecture/system-architecture.md` |
| 认证功能 | `docs/api/auth.md`、`docs/api/endpoints.md` |
| 项目结构 | `docs/architecture/system-architecture.md`、`README.md` |
| 技术栈 | `docs/constraints/tech-stack-constraints.md` |
| 任何变更 | `README.md`（版本号+更新日期） |

### 2.3 Git提交参数

| 参数 | 值 |
|------|-----|
| 格式 | `<类型>: <描述>` |
| 描述语言 | 简体中文 |
| 描述长度 | ≤50字 |
| 类型前缀 | `[文档]`、`[数据库]`、`[算法]`、`[前端]`、`[后端]`、`[修复]`、`[优化]`、`[配置]` |
| 提交前检查 | 确认相关文档已同步更新 |

---

## 三、调整策略

### 3.1 任务调度决策策略

| 特征维度 | 直接执行 | 调度执行 |
|----------|----------|----------|
| 涉及文件 | 仅单文件 | ≥2文件或跨模块 |
| 技术栈 | 单一栈且简单 | 跨栈或涉及数据层 |
| 风险等级 | 低风险（样式/文案/简单配置） | 中高风险（逻辑变更/数据/安全/需审查） |
| 审查需求 | 不需要 | 需要 |
| 文档影响 | 不涉及 | 需同步更新 |
| 复杂度 | 单步完成 | 多步骤/需规划 |

### 3.2 异常处理策略

| 异常类型 | 响应 |
|----------|------|
| 智能体执行失败 | 记录原因→重新分配或降级直接执行 |
| 审查不通过 | 反馈给开发→修复后重新审查 |
| 任务范围膨胀 | 暂停→重新评估→更新计划 |
| 依赖缺失 | 暂停→解决依赖→恢复 |
| 需求变更 | 暂停→重新分析→更新计划→通知影响范围 |

### 3.3 优先级分类策略

| 级别 | 标准 | 处理方式 |
|------|------|----------|
| P0 | 线上故障/安全漏洞/数据丢失 | 立即中断，优先处理 |
| P1 | 阻塞开发/临近截止 | 优先调度，可并行 |
| P2 | 常规功能/非阻塞优化 | 正常队列 |
| P3 | 代码清理/文档完善 | 空闲或批量处理 |

### 3.4 阶段门控策略

| 阶段转换 | 门控条件 |
|----------|----------|
| 分析→开发 | 计划明确、子任务可独立执行 |
| 开发→审查 | 代码完成、无明显语法错误 |
| 审查→文档 | 审查通过或问题已修复 |
| 文档→交付 | 文档与代码一致 |

### 3.5 降级策略（按顺序）

1. **重试**：同一智能体重新执行
2. **替换**：调度其他可用智能体
3. **回退**：调度器直接执行
4. **暂停**：标记阻塞，等待用户介入

---

## 四、技能触发策略

| 技能 | 触发条件 | 触发强度 |
|------|----------|----------|
| `moyuan-architecture` | 询问项目结构、功能模块、技术细节 | 常规 |
| `moyuan-data-assets` | 询问数据文件位置、数据库表内容、数据流转 | 常规 |
| `ui-design-system` | 新增页面、调整样式、统一视觉风格 | 常规 |
| `ui-implementer` | 根据设计规范实现UI组件 | 常规 |
| `analysis-planner` | 新需求、功能开发、问题排查、长任务、多步骤任务、复杂问题 | **强制** |
| `writing-plans` | 制定执行计划、拆解复杂任务、规划多步骤工作流 | **强制** |
| `executing-plans` | 存在书面执行计划需要执行 | **强制** |
| `subagent-driven-development` | 执行具有独立子任务的实现计划 | **强制** |
| `developer` | 涉及代码编写（需先读开发规范） | 常规 |
| `code-reviewer` | 代码编写完成后 | **强制** |
| `doc-manager` | 代码变更后 | **强制** |
| `local-fullstack-tester` | 测试本地前后端联通行 | 常规 |
| `dispatching-parallel-agents` | 2个以上可独立处理的互不依赖任务 | **强制** |
| `verification-before-completion` | 声称工作完成/修复/通过之前 | **强制** |
| `systematic-debugging` | 遇到bug、测试失败或意外行为 | **强制** |

**触发优先级：** 用户指定 > 强制触发条件匹配 > 常规条件匹配 > 无匹配时直接执行

**强制触发场景：**
- 用户提出**任何多步骤/复杂需求** → 必须先触发 `analysis-planner` 或 `writing-plans`
- 任何代码编写完成后 → 必须触发 `code-reviewer`
- 任何代码变更完成后 → 必须触发 `doc-manager`
- 存在书面计划时 → 必须触发 `executing-plans` 或 `subagent-driven-development`
- 声称任务完成前 → 必须触发 `verification-before-completion`
- 遇到异常行为时 → 必须触发 `systematic-debugging`
- 2个以上独立任务 → 必须触发 `dispatching-parallel-agents`

---

## 五、工作流标准流程

```
需求 → 调度器分析
  ├── 简单任务 → 直接执行 → 交付
  └── 复杂任务 → 分析规划 → 分派子任务
                   ├── 并行：无文件级/数据依赖的子任务
                   └── 串行：存在依赖的子任务
                   → 代码审查 → 文档同步 → 交付
```

---

## 六、约束与规范

### 6.1 代码约束

- 不添加注释（除非用户要求）
- 不添加未使用的导入/变量/函数
- 使用任何库/框架前确认项目已有该依赖
- 新代码匹配现有风格

### 6.2 安全红线

- 不暴露/记录/提交密钥、密码、Token
- 不在客户端硬编码服务端密钥或内部端点
- 不引入已知安全漏洞的依赖

### 6.3 智能体约束

- 禁止智能体之间绕过调度器直接通信
- 禁止跳过审查交付高风险变更
- 禁止未告知用户扩大任务范围
- 禁止重复调度已失败的相同方案


