---
name: "moyuan-data-assets"
description: "古今诗话——墨渊（SC_MoYuan2）项目的数据资产文档路径索引（MySQL 8 + Redis）。涵盖数据库表、建表脚本、迁移脚本、ER 图、前端静态数据、本地存储、数据流转等全部文档位置。当询问数据、数据库、建表、迁移、数据文件内容时调用以定位对应文档。"
---

# moyuan-data-assets — 数据资产文档路径索引

## 检测到的数据相关技术栈

- **数据库**: MySQL 8.x（库名 `moyuan`，utf8mb4，29 张核心表）
- **ORM框架**: MyBatis-Plus 3.5.x
- **缓存**: Redis 7.x（搜索建议/热门/历史缓存）
- **建表脚本**: `sc-moyuan-backend/src/main/resources/db/init.sql`（全量合并版 v3.0）
- **前端静态数据**: `frontend/src/data/*.json`

## 数据相关目录总览

| 分类 | 目录 | 文档数 |
|------|------|--------|
| 数据库 | [docs/database/](../../docs/database/) | 1 文档 + 1 迁移脚本 |
| 数据库建表 | `sc-moyuan-backend/src/main/resources/db/` | 1 个 init.sql |
| 前端静态数据 | [frontend/src/data/](../../frontend/src/data/) | 3 个 json |
| 迁移文档 | [docs/migration/](../../docs/migration/) | 1 个文件 |
| ER 图 | [docs/diagrams/](../../docs/diagrams/) | 2 个文件 |

## 文档路径索引

### 数据库文档

| 文档 | 路径 | 内容 |
|------|------|------|
| 表结构 | [docs/database/schema.md](../../docs/database/schema.md) | 29 张核心表清单、字段说明、索引、建表脚本状态 |
| 迁移脚本 | [docs/database/migration_002_user_history.sql](../../docs/database/migration_002_user_history.sql) | 浏览历史表增量迁移脚本 |

### 建表与种子数据

| 文件 | 路径 | 内容 |
|------|------|------|
| 全量建表脚本 | [sc-moyuan-backend/src/main/resources/db/init.sql](../../sc-moyuan-backend/src/main/resources/db/init.sql) | 29 张表建表 DDL（全量合并版 v3.0） |
| 初始化脚本 | [根目录 init-db.bat](../../init-db.bat) | 数据库一键初始化批处理 |

### 前端静态数据

| 文件 | 路径 | 内容 |
|------|------|------|
| 首页古诗数据 | [frontend/src/data/home-ancient-poems.json](../../frontend/src/data/home-ancient-poems.json) | 古诗推选栏目静态数据 |
| 首页当代诗词 | [frontend/src/data/home-contemporary-poems.json](../../frontend/src/data/home-contemporary-poems.json) | 当代精选栏目静态数据 |
| 首页诗词库 | [frontend/src/data/home-poetry-library.json](../../frontend/src/data/home-poetry-library.json) | 首页诗词库静态数据 |

### 数据迁移与优化

| 文档 | 路径 | 内容 |
|------|------|------|
| 前端迁移文档 | [docs/migration/migration-guide.md](../../docs/migration/migration-guide.md) | 静态 HTML → Vue 3 迁移、前端数据迁移说明 |
| 本地存储优化 | [docs/implementation/local-storage-optimization-report.md](../../docs/implementation/local-storage-optimization-report.md) | 浏览器本地存储（用户偏好/搜索历史/API缓存）优化实现 |

### ER 图

| 文件 | 路径 | 内容 |
|------|------|------|
| ER 图源码 | [docs/diagrams/er-diagram.dot](../../docs/diagrams/er-diagram.dot) | Graphviz 关系图源码 |
| ER 图成品 | [docs/diagrams/er-diagram.svg](../../docs/diagrams/er-diagram.svg) | 可视化关系图 |

> 系统整体架构文档索引见 [moyuan-architecture](../moyuan-architecture/SKILL.md) 技能
> 文档同步更新规范见 [doc-manager](../doc-manager/SKILL.md) 技能
