---
name: "doc-reader"
description: "查阅项目文档库中的技术规范、架构设计、数据库设计等文档。当需要了解项目架构、技术栈、编码规范、数据库设计或遇到技术问题时调用此技能。"
---

# 文档查阅技能

本技能用于快速查阅"古今诗话——墨渊"项目文档库中的各类技术文档，帮助开发人员了解项目规范和技术细节。

## 文档库结构

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

## 使用场景

### 1. 了解系统架构
**触发条件**：询问项目整体架构、模块划分、技术选型时
**查阅文档**：`docs/architecture/system-architecture.md`

### 2. 前端开发规范
**触发条件**：进行前端开发、编写Vue组件、处理状态管理时
**查阅文档**：`docs/standards/frontend-standards.md`

### 3. 后端开发规范
**触发条件**：进行后端开发、编写Spring Boot代码、设计API时
**查阅文档**：`docs/standards/backend-standards.md`

### 4. 数据库设计规范
**触发条件**：设计数据库表、编写SQL、优化查询时
**查阅文档**：`docs/standards/database-standards.md`

### 5. 技术栈限制
**触发条件**：选择技术方案、评估技术可行性时
**查阅文档**：`docs/constraints/tech-stack-constraints.md`

### 6. API端点查阅
**触发条件**：了解接口定义、前后端联调、接口测试时
**查阅文档**：`docs/api/endpoints.md`

### 7. 认证授权机制
**触发条件**：实现登录注册、权限控制、Token管理时
**查阅文档**：`docs/api/auth.md`

### 8. 业务模块说明
**触发条件**：了解业务功能、模块划分、功能需求时
**查阅文档**：`docs/business/modules.md`

### 9. 数据库表结构
**触发条件**：了解表结构、字段定义、表关系时
**查阅文档**：`docs/database/schema.md`

## 文档查阅流程

### 步骤1：识别需求类型
根据用户问题，判断需要查阅哪类文档：
- 架构相关 → system-architecture.md
- 前端相关 → frontend-standards.md
- 后端相关 → backend-standards.md
- 数据库规范相关 → database-standards.md
- 数据库表结构相关 → schema.md
- 技术选型相关 → tech-stack-constraints.md
- API接口相关 → endpoints.md
- 认证授权相关 → auth.md
- 业务功能相关 → modules.md

### 步骤2：定位具体章节
在文档中定位与问题相关的具体章节：
- 使用文档目录结构快速定位
- 搜索关键词匹配相关内容
- 查阅示例代码和最佳实践

### 步骤3：提取关键信息
从文档中提取与问题直接相关的信息：
- 技术规范和约束
- 代码示例和模板
- 注意事项和限制
- 最佳实践建议

### 步骤4：提供解决方案
基于文档内容，提供具体的解决方案：
- 引用文档中的规范和标准
- 提供符合规范的代码示例
- 说明注意事项和限制
- 推荐最佳实践

## 快速查阅指南

### 前端开发速查

| 问题类型 | 查阅位置 | 关键内容 |
|----------|----------|----------|
| 组件规范 | frontend-standards.md → Vue组件规范 | 组件结构、命名规范 |
| 状态管理 | frontend-standards.md → 状态管理规范 | Pinia使用规范 |
| 路由配置 | frontend-standards.md → 路由规范 | 路由守卫、懒加载 |
| API调用 | frontend-standards.md → API规范 | Axios封装、错误处理 |
| 样式规范 | frontend-standards.md → 样式规范 | SCSS变量、BEM命名 |

### 后端开发速查

| 问题类型 | 查阅位置 | 关键内容 |
|----------|----------|----------|
| 项目结构 | backend-standards.md → 项目结构 | 包结构、分层规范 |
| 实体类 | backend-standards.md → 实体类规范 | MyBatis-Plus注解 |
| Controller | backend-standards.md → Controller规范 | RESTful API设计 |
| Service | backend-standards.md → Service规范 | 业务逻辑封装 |
| 异常处理 | backend-standards.md → 异常处理规范 | 全局异常处理器 |

### 数据库设计速查

| 问题类型 | 查阅位置 | 关键内容 |
|----------|----------|----------|
| 表设计 | database-standards.md → 表结构设计 | 字段类型、约束 |
| 索引设计 | database-standards.md → 索引设计规范 | 索引类型、命名 |
| SQL规范 | database-standards.md → SQL编写规范 | 查询优化、分页 |
| 数据安全 | database-standards.md → 数据安全规范 | 加密、脱敏 |
| 表结构详情 | database/schema.md | 10张表的字段定义、索引、关系 |

### API接口速查

| 问题类型 | 查阅位置 | 关键内容 |
|----------|----------|----------|
| 接口清单 | api/endpoints.md | 所有模块的API端点列表 |
| 认证流程 | api/auth.md | JWT认证、Token管理 |
| 接口权限 | api/endpoints.md → 权限标识 | 公开/需登录/需管理员 |
| 请求响应格式 | api/endpoints.md → 接口详情 | 请求参数、响应结构 |

### 业务模块速查

| 问题类型 | 查阅位置 | 关键内容 |
|----------|----------|----------|
| 模块概览 | business/modules.md | 7大业务模块功能说明 |
| 首页模块 | business/modules.md → 首页模块 | 轮播、推荐、热门 |
| 诗词模块 | business/modules.md → 诗词模块 | 浏览、搜索、详情、收藏 |
| 论坛模块 | business/modules.md → 论坛模块 | 发帖、评论、点赞 |
| 用户模块 | business/modules.md → 用户模块 | 注册、登录、个人中心 |
| AI模块 | business/modules.md → AI模块 | 智能问答、推荐、赏析 |
| 搜索模块 | business/modules.md → 搜索模块 | 全文搜索、筛选 |

## 示例用法

### 示例1：询问如何创建Vue组件

**用户问题**：如何创建一个符合规范的Vue组件？

**查阅流程**：
1. 识别需求：前端开发 → Vue组件规范
2. 定位文档：`docs/standards/frontend-standards.md` → Vue组件规范
3. 提取信息：组件结构顺序、Props定义、命名规范
4. 提供方案：给出符合规范的组件模板和示例

### 示例2：询问数据库表设计

**用户问题**：如何设计诗词表？

**查阅流程**：
1. 识别需求：数据库设计 → 表结构设计
2. 定位文档：`docs/standards/database-standards.md` → 表结构设计 → 诗词表
3. 提取信息：字段定义、索引设计、外键约束
4. 提供方案：给出完整的建表SQL和设计说明

### 示例3：询问技术选型

**用户问题**：能否使用MongoDB存储诗词数据？

**查阅流程**：
1. 识别需求：技术选型 → 技术栈限制
2. 定位文档：`docs/constraints/tech-stack-constraints.md` → 数据库限制
3. 提取信息：MySQL为主数据库，MongoDB仅在必要时使用
4. 提供方案：说明限制原因，推荐使用MySQL

## 注意事项

1. **文档版本**：确保查阅的是最新版本文档
2. **规范遵循**：严格按照文档规范执行，不得随意修改
3. **例外处理**：如需例外处理，需经过技术评审
4. **文档更新**：发现文档问题或需要补充时，及时反馈

## 相关技能

- **oda-project**：项目整体知识库
- **data-assets**：数据资产管理
- **code-reviewer**：代码审查（依据文档规范）
- **developer**：代码开发（遵循文档规范）

---

**技能版本**：v1.1  
**最后更新**：2026-05-25  
**维护人员**：墨渊开发团队