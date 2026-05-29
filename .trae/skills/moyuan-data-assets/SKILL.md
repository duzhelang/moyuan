---
name: "moyuan-data-assets"
description: "古今诗话——墨渊项目的数据资产文档路径索引（MySQL 8.0 + Redis 7.x）。涵盖数据文件、数据库表、目录结构、训练数据、模型文件等全部文档位置。当询问数据文件位置、数据库表内容、数据流转、模型存储时调用以定位对应文档。"
---

# moyuan-data-assets — 文档路径索引

## 检测到的数据相关技术栈

- **数据库**: MySQL 8.0+
- **缓存**: Redis 7.x
- **ORM框架**: MyBatis-Plus 3.5.x
- **数据文件目录**: （待创建）

## docs/ 数据相关目录总览

| 分类 | 目录 | 文档数 |
|------|------|--------|
| 数据库 | [database/](../../docs/database/) | 1 个文件 |
| 数据库规范 | [standards/](../../docs/standards/) | 1 个文件 |
| API文档 | [api/](../../docs/api/) | 2 个文件 |
| 业务文档 | [business/](../../docs/business/) | 1 个文件 |

## 文档路径索引

### 数据库文档

| 文档 | 路径 | 内容 |
|------|------|------|
| 表结构 | [database/schema.md](../../docs/database/schema.md) | 11 张表的结构说明、字段定义、索引设计 |
| 数据库规范 | [standards/database-standards.md](../../docs/standards/database-standards.md) | 表结构设计规范、索引设计规范、SQL编写规范 |

### API文档

| 文档 | 路径 | 内容 |
|------|------|------|
| API端点 | [api/endpoints.md](../../docs/api/endpoints.md) | 所有模块的API端点清单、请求响应格式 |
| 认证授权 | [api/auth.md](../../docs/api/auth.md) | JWT认证流程、Token管理、权限控制 |

### 业务文档

| 文档 | 路径 | 内容 |
|------|------|------|
| 业务模块 | [business/modules.md](../../docs/business/modules.md) | 7大业务模块功能说明、数据流转 |

### 数据库表清单

| 序号 | 表名 | 说明 | 主要字段 | 建表脚本 |
|------|------|------|----------|----------|
| 1 | user | 用户表 | id, username, password, email, nickname, avatar, phone, bio, status | init.sql |
| 2 | poet | 诗人表 | id, name, courtesy_name, pseudonym, dynasty_id, birth_year, death_year, biography | init.sql |
| 3 | dynasty | 朝代表 | id, name, start_year, end_year, description, sort_order | init.sql |
| 4 | category | 诗词分类表 | id, name, parent_id, description, icon, sort_order | init.sql |
| 5 | poem | 诗词表 | id, title, content, poet_id, dynasty_id, category_id, translation, appreciation, background | init.sql |
| 6 | forum_post | 论坛帖子表 | id, title, content, user_id, category, view_count, like_count, comment_count | init.sql |
| 7 | comment | 评论表 | id, content, user_id, post_id, parent_id, reply_to_user_id, like_count | init.sql |
| 8 | user_favorite | 用户收藏表 | id, user_id, target_id, target_type | init.sql |
| 9 | user_like | 用户点赞表 | id, user_id, target_id, target_type | init.sql |
| 10 | user_history | 用户浏览历史表 | id, user_id, target_id, target_type | migration_002_user_history.sql |
| 11 | operation_log | 操作日志表 | id, user_id, username, operation, method, params, ip, duration, status, error_msg | 📋 仅文档定义 |

### ER 关系图

```
dynasty ──────────────────────────────┐
  │                                   │
  │ dynasty_id (FK)                   │
  ▼                                   │
poet ──────────┐                      │
  │            │ poet_id (FK)         │
  │            ▼                      │ dynasty_id (FK)
  │          poem ◄───────────────────┘
  │            │
  │            │ category_id (FK)
  │            ▼
  │         category（支持层级：parent_id → 自关联）
  │
  │
user ──┬── forum_post（user_id FK）
       │      │
       │      │ post_id (FK)
       │      ▼
       ├── comment（user_id FK, post_id FK, 支持多级回复：parent_id 自关联）
       │
       ├── user_favorite（多态：target_type → poem/post/poet）
       │
       ├── user_like（多态：target_type → poem/post/comment）
       │
       └── operation_log（user_id FK，可为NULL）
```

---

## 数据资产查阅场景

| 用户问题 | 推荐查阅文档 |
|----------|--------------|
| 数据库有哪些表？ | [database/schema.md](../../docs/database/schema.md) |
| 用户表的字段有哪些？ | [database/schema.md](../../docs/database/schema.md) → user 表 |
| 诗词表怎么设计的？ | [database/schema.md](../../docs/database/schema.md) → poem 表 |
| 数据是怎么流转的？ | [business/modules.md](../../docs/business/modules.md) → 数据流转 |
| 数据库索引怎么设计？ | [standards/database-standards.md](../../docs/standards/database-standards.md) → 索引设计规范 |
| SQL 怎么写？ | [standards/database-standards.md](../../docs/standards/database-standards.md) → SQL 编写规范 |

> 系统整体架构文档索引见 [moyuan-architecture](../moyuan-architecture/SKILL.md) 技能