---
name: "moyuan-architecture"
description: "古今诗话——墨渊（SC_MoYuan2）项目的系统架构文档路径索引（Spring Boot 3.x + Vue 3 + TypeScript + MySQL 8 + Redis）。涵盖项目结构、API、数据库、前端、业务模块、技术栈限制、开发规范、指南等全部文档位置。当询问架构、功能、API、规范、技术细节时调用以定位对应文档。"
---

# moyuan-architecture — 文档路径索引

## 检测到的技术栈

- **后端**: Spring Boot 3.x（Java 17+），位于 `sc-moyuan-backend/`
- **前端**: Vue 3.4+ + TypeScript 5 + Vite 5.x，位于 `frontend/`
- **数据库**: MySQL 8.x（库名 `moyuan`，utf8mb4）
- **ORM**: MyBatis-Plus 3.5.x
- **缓存**: Redis 7.x
- **安全**: Spring Security 6.x + JWT

## 模块目录总览

| 目录 | 说明 |
|------|------|
| `sc-moyuan-backend/` | 后端唯一工程（Controller → Service → Mapper 分层） |
| `frontend/` | Vue 3 前端工程（api/components/stores/views） |
| `html/` `css/` `js/` | 旧版静态页面资源 |
| `poetry-cloud-design/` | 诗云星空 3D 页面设计稿 |
| `docs/` | 项目文档体系（本次技能索引目标） |

## docs/ 目录总览

| 分类 | 目录 | 文档数 |
|------|------|--------|
| 架构概览 | [architecture/](../../docs/architecture/) | 1 个文件 |
| API文档 | [api/](../../docs/api/) | 2 个文件 |
| 数据库 | [database/](../../docs/database/) | 1 文档 + 1 迁移脚本 |
| 业务功能 | [business/](../../docs/business/) | 1 个文件 |
| 技术栈约束 | [constraints/](../../docs/constraints/) | 1 个文件 |
| 开发规范 | [standards/](../../docs/standards/) | 3 个文件 |
| 指南 | [guides/](../../docs/guides/) | 5 个文件 |
| 迁移/实现/报告 | [migration/](../../docs/migration/) [implementation/](../../docs/implementation/) [reports/](../../docs/reports/) 等 | 若干 |

## 文档路径索引

### 架构文档

| 文档 | 路径 | 内容 |
|------|------|------|
| 系统架构 | [architecture/system-architecture.md](../../docs/architecture/system-architecture.md) | 项目概览、技术栈、模块划分（19+ 模块）、测试架构、部署架构 |

### API 文档

| 文档 | 路径 | 内容 |
|------|------|------|
| 端点清单 | [api/endpoints.md](../../docs/api/endpoints.md) | Spring Boot REST API 端点列表（按模块分组） |
| 认证 | [api/auth.md](../../docs/api/auth.md) | 认证授权机制（JWT、注册/登录流程） |

### 数据库文档

| 文档 | 路径 | 内容 |
|------|------|------|
| 表结构 | [database/schema.md](../../docs/database/schema.md) | 29 张核心表清单、字段说明、索引、建表脚本状态 |
| 迁移脚本 | [database/migration_002_user_history.sql](../../docs/database/migration_002_user_history.sql) | 浏览历史表迁移脚本 |

### 业务功能文档

| 文档 | 路径 | 内容 |
|------|------|------|
| 业务模块 | [business/modules.md](../../docs/business/modules.md) | 23 大业务模块功能说明、页面组件清单 |

### 技术栈约束

| 文档 | 路径 | 内容 |
|------|------|------|
| 技术栈限制 | [constraints/tech-stack-constraints.md](../../docs/constraints/tech-stack-constraints.md) | 前端/后端技术栈强制与禁用项、编码规范、性能/安全/部署限制 |

### 开发规范

| 文档 | 路径 | 内容 |
|------|------|------|
| 后端规范 | [standards/backend-standards.md](../../docs/standards/backend-standards.md) | 后端编码规范 |
| 前端规范 | [standards/frontend-standards.md](../../docs/standards/frontend-standards.md) | 前端编码规范 |
| 数据库规范 | [standards/database-standards.md](../../docs/standards/database-standards.md) | 数据库设计规范 |

### 指南文档

| 文档 | 路径 | 内容 |
|------|------|------|
| 开发指南 | [guides/development-guide.md](../../docs/guides/development-guide.md) | 环境准备、前端/后端启动步骤 |
| 沙箱规范 | [guides/sandbox-workflow.md](../../docs/guides/sandbox-workflow.md) | Windows PowerShell 5 命令执行规范（RunCommand 前必读） |
| Git 屏蔽规范 | [guides/git-exclude-rules.md](../../docs/guides/git-exclude-rules.md) | .gitignore / .git/info/exclude 排除规则、文档同步检查 |
| Python 扩展计划 | [guides/python-extension-plan.md](../../docs/guides/python-extension-plan.md) | 后端 Python 扩展规划 |
| Python 扩展简版 | [guides/python-extension-simple-plans.md](../../docs/guides/python-extension-simple-plans.md) | 后端 Python 扩展简版规划 |

### 迁移与报告文档

| 文档 | 路径 | 内容 |
|------|------|------|
| 前端迁移 | [migration/migration-guide.md](../../docs/migration/migration-guide.md) | 静态 HTML → Vue 3 迁移说明、目录结构 |
| 本地存储优化 | [implementation/local-storage-optimization-report.md](../../docs/implementation/local-storage-optimization-report.md) | 浏览器本地存储优化实现报告 |
| 智能体协作 | [reports/agent-skill-coordination-report.md](../../docs/reports/agent-skill-coordination-report.md) | 智能体与技能协作报告 |
| 代码审查 | [review/code-review-report-2026-06-06.md](../../docs/review/code-review-report-2026-06-06.md) | 代码审查报告 |
| 后端数据库计划 | [backend-database-plan.md](../../docs/backend-database-plan.md) | 后端数据库建设计划 |
| ER 图 | [diagrams/er-diagram.dot](../../docs/diagrams/er-diagram.dot) [er-diagram.svg](../../docs/diagrams/er-diagram.svg) | 数据库 ER 关系图（Graphviz/SVG） |

> 数据资产相关文档索引见 [moyuan-data-assets](../moyuan-data-assets/SKILL.md) 技能
> 文档同步更新规范见 [doc-manager](../doc-manager/SKILL.md) 技能
