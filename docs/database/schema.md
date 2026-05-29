# 数据库表结构文档

## 概述

本文档记录"古今诗话——墨渊"项目数据库的完整表结构，共 10 张核心表。

> **建表脚本状态**：
> - ✅ 已有建表脚本：`backend/src/main/resources/db/init.sql` 中包含全部 9 张业务表的建表语句及初始数据
> - 📋 仅文档定义：`operation_log` 表尚无实际建表脚本

## 表清单

| 序号 | 表名 | 说明 | 记录数预估 | 建表脚本 |
|------|------|------|------------|----------|
| 1 | user | 用户表 | 10万+ | ✅ init.sql |
| 2 | poet | 诗人表 | 1000+ | ✅ init.sql |
| 3 | dynasty | 朝代表 | 15 | ✅ init.sql |
| 4 | category | 诗词分类表 | 50+ | ✅ init.sql |
| 5 | poem | 诗词表 | 10万+ | ✅ init.sql |
| 6 | forum_post | 论坛帖子表 | 10万+ | ✅ init.sql |
| 7 | comment | 评论表 | 100万+ | ✅ init.sql |
| 8 | user_favorite | 用户收藏表 | 100万+ | ✅ init.sql |
| 9 | user_like | 用户点赞表 | 100万+ | ✅ init.sql |
| 10 | user_history | 用户浏览历史表 | 100万+ | ✅ migration_002_user_history.sql |
| 11 | operation_log | 操作日志表 | 1000万+ | 📋 仅文档定义 |

## 表结构详情

### 1. user（用户表）

存储系统用户信息。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | 自增 | 主键 |
| username | VARCHAR(50) | 是 | - | 用户名（唯一） |
| password | VARCHAR(100) | 是 | - | 密码（BCrypt加密） |
| email | VARCHAR(100) | 否 | NULL | 邮箱（唯一） |
| phone | VARCHAR(20) | 否 | NULL | 手机号 |
| nickname | VARCHAR(50) | 否 | NULL | 昵称 |
| avatar | VARCHAR(255) | 否 | NULL | 头像URL |
| bio | VARCHAR(500) | 否 | NULL | 个人简介 |
| status | TINYINT | 是 | 1 | 状态：0-禁用，1-正常 |
| last_login_time | DATETIME | 否 | NULL | 最后登录时间 |
| last_login_ip | VARCHAR(50) | 否 | NULL | 最后登录IP |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT | 是 | 0 | 逻辑删除：0-未删除，1-已删除 |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_user_username (username)
- UNIQUE KEY uk_user_email (email)
- KEY idx_user_status (status)
- KEY idx_user_create_time (create_time)

---

### 2. poet（诗人表）

存储诗人信息。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | 自增 | 主键 |
| name | VARCHAR(50) | 是 | - | 诗人姓名 |
| courtesy_name | VARCHAR(50) | 否 | NULL | 字 |
| pseudonym | VARCHAR(50) | 否 | NULL | 号 |
| dynasty_id | BIGINT | 是 | - | 朝代ID（外键） |
| birth_year | INT | 否 | NULL | 出生年份 |
| death_year | INT | 否 | NULL | 去世年份 |
| birthplace | VARCHAR(100) | 否 | NULL | 出生地 |
| biography | TEXT | 否 | NULL | 生平简介 |
| avatar | VARCHAR(255) | 否 | NULL | 头像URL |
| status | TINYINT | 是 | 1 | 状态：0-禁用，1-正常 |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT | 是 | 0 | 逻辑删除 |

**索引**：
- PRIMARY KEY (id)
- KEY idx_poet_dynasty_id (dynasty_id)
- KEY idx_poet_name (name)

---

### 3. dynasty（朝代表）

存储朝代信息。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | 自增 | 主键 |
| name | VARCHAR(50) | 是 | - | 朝代名称（唯一） |
| start_year | INT | 否 | NULL | 开始年份 |
| end_year | INT | 否 | NULL | 结束年份 |
| description | TEXT | 否 | NULL | 朝代简介 |
| sort_order | INT | 是 | 0 | 排序顺序 |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_dynasty_name (name)

**初始数据**（13个，与 init.sql 一致）：先秦、秦朝、汉朝、魏晋南北朝、隋朝、唐朝、五代十国、宋朝、元朝、明朝、清朝、民国、现代

---

### 4. category（诗词分类表）

存储诗词分类信息，支持层级结构。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | 自增 | 主键 |
| name | VARCHAR(50) | 是 | - | 分类名称 |
| parent_id | BIGINT | 否 | 0 | 父分类ID（0=顶级） |
| description | VARCHAR(255) | 否 | NULL | 分类描述 |
| icon | VARCHAR(255) | 否 | NULL | 分类图标 |
| sort_order | INT | 是 | 0 | 排序顺序 |
| status | TINYINT | 是 | 1 | 状态：0-禁用，1-正常 |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

**索引**：
- PRIMARY KEY (id)
- KEY idx_category_parent_id (parent_id)

---

### 5. poem（诗词表）

存储诗词内容，核心业务表。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | 自增 | 主键 |
| title | VARCHAR(100) | 是 | - | 诗词标题 |
| content | TEXT | 是 | - | 诗词内容 |
| poet_id | BIGINT | 否 | NULL | 诗人ID（外键） |
| dynasty_id | BIGINT | 否 | NULL | 朝代ID（外键） |
| category_id | BIGINT | 否 | NULL | 分类ID（外键） |
| translation | TEXT | 否 | NULL | 译文 |
| appreciation | TEXT | 否 | NULL | 赏析 |
| background | TEXT | 否 | NULL | 创作背景 |
| source | VARCHAR(255) | 否 | NULL | 来源 |
| view_count | INT | 是 | 0 | 浏览次数 |
| like_count | INT | 是 | 0 | 点赞次数 |
| favorite_count | INT | 是 | 0 | 收藏次数 |
| status | TINYINT | 是 | 1 | 状态：0-草稿，1-已发布，2-待审核 |
| is_featured | TINYINT | 是 | 0 | 是否精选：0-否，1-是 |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT | 是 | 0 | 逻辑删除 |

**索引**：
- PRIMARY KEY (id)
- KEY idx_poem_poet_id (poet_id)
- KEY idx_poem_dynasty_id (dynasty_id)
- KEY idx_poem_category_id (category_id)
- KEY idx_poem_status (status)
- KEY idx_poem_create_time (create_time)
- FULLTEXT KEY ft_poem_title_content (title, content)

**外键**：
- fk_poem_poet → poet(id)
- fk_poem_dynasty → dynasty(id)
- fk_poem_category → category(id)

---

### 6. forum_post（论坛帖子表）

存储论坛帖子信息。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | 自增 | 主键 |
| title | VARCHAR(200) | 是 | - | 帖子标题 |
| content | TEXT | 是 | - | 帖子内容 |
| user_id | BIGINT | 是 | - | 发帖用户ID（外键） |
| category | VARCHAR(50) | 否 | NULL | 帖子分类 |
| view_count | INT | 是 | 0 | 浏览次数 |
| like_count | INT | 是 | 0 | 点赞次数 |
| comment_count | INT | 是 | 0 | 评论数量 |
| is_top | TINYINT | 是 | 0 | 是否置顶 |
| is_featured | TINYINT | 是 | 0 | 是否精选 |
| status | TINYINT | 是 | 1 | 状态：0-草稿，1-已发布，2-已关闭，3-已删除 |
| last_comment_time | DATETIME | 否 | NULL | 最后评论时间 |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT | 是 | 0 | 逻辑删除 |

**索引**：
- PRIMARY KEY (id)
- KEY idx_forum_post_user_id (user_id)
- KEY idx_forum_post_category (category)
- KEY idx_forum_post_status (status)
- KEY idx_forum_post_create_time (create_time)
- FULLTEXT KEY ft_forum_post_title_content (title, content)

---

### 7. comment（评论表）

存储评论信息，支持多级回复。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | 自增 | 主键 |
| content | TEXT | 是 | - | 评论内容 |
| user_id | BIGINT | 是 | - | 评论用户ID（外键） |
| post_id | BIGINT | 是 | - | 帖子ID（外键） |
| parent_id | BIGINT | 否 | 0 | 父评论ID（0=顶级） |
| reply_to_user_id | BIGINT | 否 | NULL | 回复目标用户ID |
| like_count | INT | 是 | 0 | 点赞次数 |
| status | TINYINT | 是 | 1 | 状态：0-隐藏，1-正常 |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT | 是 | 0 | 逻辑删除 |

**索引**：
- PRIMARY KEY (id)
- KEY idx_comment_user_id (user_id)
- KEY idx_comment_post_id (post_id)
- KEY idx_comment_parent_id (parent_id)
- KEY idx_comment_create_time (create_time)

---

### 8. user_favorite（用户收藏表）

存储用户收藏记录。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | 自增 | 主键 |
| user_id | BIGINT | 是 | - | 用户ID（外键） |
| target_id | BIGINT | 是 | - | 收藏目标ID |
| target_type | TINYINT | 是 | - | 收藏类型：1-诗词，2-帖子，3-诗人 |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_user_favorite (user_id, target_id, target_type)
- KEY idx_user_favorite_target (target_id, target_type)

---

### 9. user_like（用户点赞表）

存储用户点赞记录。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | 自增 | 主键 |
| user_id | BIGINT | 是 | - | 用户ID（外键） |
| target_id | BIGINT | 是 | - | 点赞目标ID |
| target_type | TINYINT | 是 | - | 点赞类型：1-诗词，2-帖子，3-评论 |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_user_like (user_id, target_id, target_type)
- KEY idx_user_like_target (target_id, target_type)

---

### 10. user_history（用户浏览历史表）

存储用户浏览历史记录。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | 自增 | 主键 |
| user_id | BIGINT | 是 | - | 用户ID（外键） |
| target_id | BIGINT | 是 | - | 浏览目标ID |
| target_type | TINYINT | 是 | - | 浏览类型：1-诗词，2-诗人，3-帖子 |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

**索引**：
- PRIMARY KEY (id)
- KEY idx_user_history_user_id (user_id)
- KEY idx_user_history_target (target_id, target_type)
- KEY idx_user_history_create_time (create_time)

---

### 11. operation_log（操作日志表）

存储系统操作日志。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | 自增 | 主键 |
| user_id | BIGINT | 否 | NULL | 操作用户ID |
| username | VARCHAR(50) | 否 | NULL | 操作用户名 |
| operation | VARCHAR(50) | 是 | - | 操作类型 |
| method | VARCHAR(200) | 是 | - | 请求方法 |
| params | TEXT | 否 | NULL | 请求参数 |
| ip | VARCHAR(50) | 否 | NULL | 请求IP |
| duration | INT | 否 | NULL | 请求时长（毫秒） |
| status | TINYINT | 是 | 1 | 操作状态：0-失败，1-成功 |
| error_msg | TEXT | 否 | NULL | 错误信息 |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

**索引**：
- PRIMARY KEY (id)
- KEY idx_operation_log_user_id (user_id)
- KEY idx_operation_log_operation (operation)
- KEY idx_operation_log_create_time (create_time)

---

## ER 关系图

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

### 外键约束说明

| 外键约束名 | 源表.字段 | 引用表.字段 | 说明 |
|------------|-----------|-------------|------|
| fk_poem_poet | poem.poet_id | poet.id | 诗词→诗人 |
| fk_poem_dynasty | poem.dynasty_id | dynasty.id | 诗词→朝代 |
| fk_poem_category | poem.category_id | category.id | 诗词→分类 |
| fk_forum_post_user | forum_post.user_id | user.id | 帖子→用户 |
| fk_comment_user | comment.user_id | user.id | 评论→用户 |
| fk_comment_post | comment.post_id | forum_post.id | 评论→帖子 |
| fk_user_favorite_user | user_favorite.user_id | user.id | 收藏→用户 |
| fk_user_like_user | user_like.user_id | user.id | 点赞→用户 |

> **注意**：外键约束在 `database-standards.md` 的建表SQL中定义，但 `init.sql` 中暂未包含外键约束（基础版后端使用了简化建表语句）。

---

**文档版本**：v1.2
**最后更新**：2026-05-29
**维护人员**：墨渊开发团队