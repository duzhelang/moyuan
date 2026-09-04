---
name: "doc-manager"
description: "古今诗话——墨渊（SC_MoYuan2）项目的文档更新管理技能。当代码变更后需要检查/更新文档、用户要求更新文档、git 提交前文档同步检查时触发。依据文档-变更映射表判断影响范围，确保 docs/ 文档与代码实现一致。"
---

# doc-manager — 文档更新管理

## 触发条件

- 代码变更后需要检查相关文档是否同步更新
- git 提交前的文档同步检查
- 用户要求更新/核对项目文档
- 新增模块、API、数据库表、组件等代码变更

## 文档-变更映射表

根据变更类型定位需检查的文档：

| 变更类型 | 需检查的文档 |
|----------|-------------|
| 新增/修改 Vue 组件 | [standards/frontend-standards.md](../../docs/standards/frontend-standards.md)、[architecture/system-architecture.md](../../docs/architecture/system-architecture.md) |
| 新增/修改后端 API | [api/endpoints.md](../../docs/api/endpoints.md)、[api/auth.md](../../docs/api/auth.md) |
| 新增/修改数据库表 | [database/schema.md](../../docs/database/schema.md)、[database/init.sql](../../sc-moyuan-backend/src/main/resources/db/init.sql) |
| 新增/修改业务模块 | [business/modules.md](../../docs/business/modules.md)、[architecture/system-architecture.md](../../docs/architecture/system-architecture.md) |
| 技术栈/依赖变更 | [constraints/tech-stack-constraints.md](../../docs/constraints/tech-stack-constraints.md) |
| 任何代码变更 | `README.md`（版本号和更新日期，注意其为 .git/info/exclude 本地文件） |

## 标准流程

### 第一步：识别影响范围

- 查看代码变更 diff，判断变更涉及的模块（前端/后端/数据库/AI等）
- 依据上述映射表，列出所有可能受影响的文档

### 第二步：核对文档状态

- 逐一打开受影响文档，核对内容与实际代码是否一致
- 重点核对：
  - API 端点路径、请求/响应字段
  - 数据库表结构、字段、索引
  - 模块功能描述、页面组件清单
  - 技术栈版本、端口号（前端 5173 / 后端 8085）
  - AI 模型清单、提示词模板

### 第三步：更新文档

- 发现不一致 → 修改文档使其与实际代码一致
- 修改后更新文档末尾的版本号与"最后更新"日期
- 记录变更记录表

### 第四步：确认提交

- 所有受影响文档已同步 → 告知 git-commit 技能可以提交
- 未同步文档必须先更新再提交

## 文档索引

| 文档分类 | 路径 |
|----------|------|
| 架构/API/数据库/规范索引 | [moyuan-architecture](../moyuan-architecture/SKILL.md) 技能 |
| 数据资产/建表/迁移索引 | [moyuan-data-assets](../moyuan-data-assets/SKILL.md) 技能 |
| 文档屏蔽规则 | [docs/guides/git-exclude-rules.md](../../docs/guides/git-exclude-rules.md) |

## 注意事项

- `docs/` 目录由 `.git/info/exclude` 排除，仅本地维护，不提交远程
- 文档更新完成后无需 git 提交，但需确保本地内容与实际一致
- 数据库结构修改后必须同步更新 `init.sql` 与 `schema.md`
- 新增模块时需同步更新架构图、模块清单、API 端点文档
