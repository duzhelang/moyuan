---
name: "moyuan-architecture"
description: "古今诗话——墨渊项目的系统架构文档路径索引（Vue 3 + Spring Boot 3 + MySQL）。涵盖项目结构、API、数据库、前端、业务逻辑、开发规范、指南等全部文档位置。当询问架构、功能、API、规范、技术细节时调用以定位对应文档。"
---

# moyuan-architecture — 文档路径索引

## 检测到的技术栈

- **后端**: Spring Boot 3.x（规划中）
- **前端**: Vue 3 + Vite
- **数据库**: MySQL 8.0+
- **缓存**: Redis 7.x
- **ORM**: MyBatis-Plus 3.5.x
- **安全**: Spring Security + JWT
- **UI**: Element Plus

## docs/ 目录总览

| 分类 | 目录 | 文档数 |
|------|------|--------|
| 架构概览 | [architecture/](../../docs/architecture/) | 1 个文件 |
| 技术规范 | [standards/](../../docs/standards/) | 3 个文件 |
| 约束条件 | [constraints/](../../docs/constraints/) | 1 个文件 |
| 开发指南 | [guides/](../../docs/guides/) | 1 个文件 |
| 数据库 | [database/](../../docs/database/) | 1 个文件 |
| API文档 | [api/](../../docs/api/) | 2 个文件 |
| 业务功能 | [business/](../../docs/business/) | 1 个文件 |

## 文档路径索引

### 架构文档

| 文档 | 路径 | 内容 |
|------|------|------|
| 系统架构 | [architecture/system-architecture.md](../../docs/architecture/system-architecture.md) | 整体架构图、模块划分、技术选型、部署架构 |

### 技术规范文档

| 文档 | 路径 | 内容 |
|------|------|------|
| 前端规范 | [standards/frontend-standards.md](../../docs/standards/frontend-standards.md) | Vue 3 组件规范、TypeScript、Pinia、路由、样式 |
| 后端规范 | [standards/backend-standards.md](../../docs/standards/backend-standards.md) | Spring Boot 分层架构、Controller/Service/Mapper 规范 |
| 数据库规范 | [standards/database-standards.md](../../docs/standards/database-standards.md) | 表结构设计、索引规范、SQL 编写规范 |

### 约束条件文档

| 文档 | 路径 | 内容 |
|------|------|------|
| 技术栈限制 | [constraints/tech-stack-constraints.md](../../docs/constraints/tech-stack-constraints.md) | 前后端技术栈限制、性能要求、安全规范 |

### 数据库文档

| 文档 | 路径 | 内容 |
|------|------|------|
| 数据库表结构 | [database/schema.md](../../docs/database/schema.md) | 10 张核心表结构说明（user、poem、poet、dynasty 等） |

### API 文档

| 文档 | 路径 | 内容 |
|------|------|------|
| API 端点清单 | [api/endpoints.md](../../docs/api/endpoints.md) | Spring Boot REST API 端点列表 |
| 认证授权 | [api/auth.md](../../docs/api/auth.md) | JWT 认证机制、Spring Security 配置 |

### 业务功能文档

| 文档 | 路径 | 内容 |
|------|------|------|
| 业务模块 | [business/modules.md](../../docs/business/modules.md) | 7 大业务模块功能说明（首页、诗词、诗人、论坛、AI、搜索、用户） |

### 开发指南

| 文档 | 路径 | 内容 |
|------|------|------|
| 开发指南 | [guides/development-guide.md](../../docs/guides/development-guide.md) | 环境准备、项目结构、开发流程、部署指南 |

---

## 文档查阅场景

| 用户问题 | 推荐查阅文档 |
|----------|--------------|
| 项目整体架构是怎样的？ | [architecture/system-architecture.md](../../docs/architecture/system-architecture.md) |
| 前端组件怎么写？ | [standards/frontend-standards.md](../../docs/standards/frontend-standards.md) |
| 后端 API 怎么设计？ | [standards/backend-standards.md](../../docs/standards/backend-standards.md) |
| 数据库表怎么设计？ | [standards/database-standards.md](../../docs/standards/database-standards.md) 或 [database/schema.md](../../docs/database/schema.md) |
| 有哪些技术限制？ | [constraints/tech-stack-constraints.md](../../docs/constraints/tech-stack-constraints.md) |
| 如何启动项目？ | [guides/development-guide.md](../../docs/guides/development-guide.md) |
| API 接口有哪些？ | [api/endpoints.md](../../docs/api/endpoints.md) |
| 认证机制是什么？ | [api/auth.md](../../docs/api/auth.md) |
| 有哪些业务模块？ | [business/modules.md](../../docs/business/modules.md) |

> 数据资产相关文档索引见 [moyuan-data-assets](../moyuan-data-assets/SKILL.md) 技能