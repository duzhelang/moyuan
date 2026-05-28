---
alwaysApply: true
description: 必用规则
---

# 古今诗话——墨渊 项目必用规则

> 本文件定义每次任务执行时必须遵循的规则。

## 编码前置检查

1. **技能检查**：调用 `find-skills` skill 检索适用技能
2. **编码规范**：调用 `karpathy-guidelines` skill 避免常见编码错误
3. **文档查阅**：调用 `doc-reader` skill 查阅项目技术文档

## 核心知识库技能

| 技能 | 描述 | 触发条件 |
|------|------|----------|
| **oda-project** | 项目结构、技术栈、核心功能、API文档、数据库设计 | 询问项目架构、功能模块、技术细节、业务逻辑、项目依赖、版本控制 |
| **data-assets** | 数据文件、数据库表、目录结构、训练数据、模型文件 | 询问数据文件位置、训练集内容、模型存储、数据归档 |
| **doc-reader** | 查阅项目文档库中的技术规范、架构设计、数据库设计等文档 | 需要了解项目架构、技术栈、编码规范、数据库设计或遇到技术问题时 |

## 项目文档库

### 文档目录结构

```
docs/
├── architecture/                    # 架构设计
│   └── system-architecture.md      # 系统架构文档
├── standards/                       # 技术规范
│   ├── frontend-standards.md       # 前端技术规范
│   ├── backend-standards.md        # 后端技术规范
│   └── database-standards.md       # 数据库设计规范
├── constraints/                     # 约束条件
│   └── tech-stack-constraints.md   # 技术栈限制
├── database/                        # 数据库文档
│   └── schema.md                   # 数据库表结构（10张表）
├── api/                             # API文档
│   ├── endpoints.md                # API端点清单
│   └── auth.md                     # 认证授权机制
├── business/                        # 业务文档
│   └── modules.md                  # 业务模块说明
└── guides/                          # 开发指南
    └── development-guide.md        # 开发指南
```

### 文档使用规范

| 场景 | 查阅文档 | 说明 |
|------|----------|------|
| 系统架构设计 | `docs/architecture/system-architecture.md` | 了解整体架构、模块划分、技术选型 |
| 前端开发 | `docs/standards/frontend-standards.md` | Vue组件规范、状态管理、路由配置 |
| 后端开发 | `docs/standards/backend-standards.md` | Spring Boot规范、API设计、异常处理 |
| 数据库规范 | `docs/standards/database-standards.md` | 表结构设计、索引规范、SQL编写 |
| 数据库表结构 | `docs/database/schema.md` | 10张表的字段定义、索引、关系 |
| 技术选型 | `docs/constraints/tech-stack-constraints.md` | 技术栈限制、性能要求、安全规范 |
| API接口 | `docs/api/endpoints.md` | 所有模块的API端点清单 |
| 认证授权 | `docs/api/auth.md` | JWT认证、Token管理、权限控制 |
| 业务功能 | `docs/business/modules.md` | 7大业务模块功能说明 |
| 开发指南 | `docs/guides/development-guide.md` | 开发流程、调试技巧、部署指南 |

### 文档路径索引技能

| 技能 | 用途 | 触发条件 |
|------|------|----------|
| **moyuan-architecture** | 系统架构文档路径索引 | 询问项目架构、模块划分、技术选型时 |
| **moyuan-data-assets** | 数据资产文档路径索引 | 询问数据库表、API接口、业务模块时 |

## 智能体协作流程

| 步骤 | 负责智能体 | 任务 |
|------|------------|------|
| 1 | 分析规划师 | 分析需求、制定任务计划 |
| 2 | 代码开发 | 依据 [前端规范](../docs/standards/frontend-standards.md) 或 [后端规范](../docs/standards/backend-standards.md) 编写代码 |
| 3 | 代码审查 | 检查代码规范性，依据 [技术栈限制](../docs/constraints/tech-stack-constraints.md) |
| 4 | 文档管理 | 更新 `docs` 目录下相关文档 |
| 5 | 分析规划师 | 汇总结果、更新任务计划 |

> 智能体配置详见 [agents-config.md](agents-config.md)  
> 技能触发规则详见 [skills-trigger-rules.md](skills-trigger-rules.md)

## 任务与持久化

| 类型 | 路径 |
|------|------|
| 任务文件 | `task.md`（项目根目录） |
| 记忆存储 | `.trae/memories/xx-xx(年-月)/xx(日)/xxxx(时间)-uuid.md` |

## 技术栈要求

### 前端技术栈
- **框架**：Vue 3.4+ (Composition API)
- **构建工具**：Vite 5.x
- **状态管理**：Pinia 2.x
- **UI组件库**：Element Plus 2.x
- **HTTP客户端**：Axios 1.x
- **类型系统**：TypeScript 5.x

### 后端技术栈
- **框架**：Spring Boot 3.x
- **ORM**：MyBatis-Plus 3.5.x
- **数据库**：MySQL 8.0+
- **缓存**：Redis 7.x
- **安全**：Spring Security 6.x
- **构建工具**：Maven 3.8+

### 开发规范
- 前端开发必须遵循 `docs/standards/frontend-standards.md`
- 后端开发必须遵循 `docs/standards/backend-standards.md`
- 数据库设计必须遵循 `docs/standards/database-standards.md`
- 技术选型必须符合 `docs/constraints/tech-stack-constraints.md`
