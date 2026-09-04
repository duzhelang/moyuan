# 数据库表结构文档

## 概述

本文档记录"古今诗话——墨渊"项目数据库的完整表结构，共 29 张核心表。

> **建表脚本状态**：
> - ✅ 全部 29 张表已纳入建表脚本：`sc-moyuan-backend/src/main/resources/db/init.sql`（全量合并版 v3.0）
> - ❌ 过渡迁移脚本 `migration_003_*.sql`、`migration_004_*.sql` 已删除

## 表清单

| 序号 | 表名 | 说明 | 记录数预估 | 建表脚本 |
|------|------|------|------------|----------|
| 1 | user | 用户表 | 10万+ | ✅ init.sql |
| 2 | poet | 诗人表 | 1000+ | ✅ init.sql |
| 3 | dynasty | 朝代表 | 13 | ✅ init.sql |
| 4 | category | 诗词分类表 | 70+ | ✅ init.sql |
| 5 | poem | 诗词表 | 10万+ | ✅ init.sql |
| 6 | forum_post | 论坛帖子表 | 10万+ | ✅ init.sql |
| 7 | comment | 评论表（通用target_id/target_type） | 100万+ | ✅ init.sql |
| 8 | user_favorite | 用户收藏表 | 100万+ | ✅ init.sql |
| 9 | user_like | 用户点赞表 | 100万+ | ✅ init.sql |
| 10 | user_history | 用户浏览历史表 | 100万+ | ✅ init.sql |
| 11 | operation_log | 操作日志表 | 1000万+ | ✅ init.sql |
| 12 | ai_model | AI模型配置表 | <100 | ✅ init.sql |
| 13 | poet_featured | 精选诗人卡片表 | <100 | ✅ init.sql |
| 14 | home_navigation | 首页导航数据表 | <100 | ✅ init.sql |
| 15 | vision_article | 诗话视野文章表 | 100+ | ✅ init.sql |
| 16 | visit_log | 访问日志表 | 1000万+ | ✅ init.sql |
| 17 | file_metadata | 文件元数据表 | 100万+ | ✅ init.sql |
| 18 | ai_image_record | AI生成图片记录表 | 10万+ | ✅ init.sql |
| 19 | poet_profile | 认证诗人资料表 | 1万+ | ✅ init.sql |
| 20 | poem_rating | 诗词评分表 | 100万+ | ✅ init.sql |
| 21 | rhyme | 韵脚表（平水韵） | 1000+ | ✅ init.sql |
| 22 | ai_module_config | AI模块模型配置表 | <100 | ✅ init.sql |
| 23 | poem_content_cache | 诗词内容缓存表 | 10万+ | ✅ init.sql |
| 24 | poet_suggestion | 诗人内容建议表 | 1万+ | ✅ init.sql |
| 25 | poet_draft | 诗人内容草稿表 | 1万+ | ✅ init.sql |
| 26 | static_page | 静态页面内容表 | <100 | ✅ init.sql |
| 27 | ai_generated_content | AI生成内容审核表 | 10万+ | ✅ init.sql |
| 28 | repair_order | 报修工单表 | 1万+ | ✅ init.sql |
| 29 | repair_comment | 报修反馈表 | 10万+ | ✅ init.sql |

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
| gender | TINYINT | 否 | 0 | 性别：0-未知，1-男，2-女 |
| birthday | DATE | 否 | NULL | 生日 |
| bio | VARCHAR(500) | 否 | NULL | 个人简介 |
| interests | VARCHAR(500) | 否 | NULL | 兴趣选项（逗号分隔，如：古典,现代,自由体,外国） |
| role | VARCHAR(20) | 是 | user | 角色：user-普通用户，admin-管理员 |
| poet_verified | TINYINT | 是 | 0 | 诗人认证状态：0-未认证，1-已认证，2-认证中 |
| poet_profile_id | BIGINT | 否 | NULL | 诗人资料ID（关联 poet_profile 表） |
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
| life_story | TEXT | 否 | NULL | 人物生平 |
| influence | TEXT | 否 | NULL | 主要影响 |
| evaluation | TEXT | 否 | NULL | 历史评价 |
| anecdotes | TEXT | 否 | NULL | 轶事典故 |
| avatar | VARCHAR(255) | 否 | NULL | 头像URL |
| status | TINYINT | 是 | 1 | 状态：0-禁用，1-正常 |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT | 是 | 0 | 逻辑删除 |

**索引**：
- PRIMARY KEY (id)
- KEY idx_poet_dynasty_id (dynasty_id)
- KEY idx_poet_name (name)
- UNIQUE KEY uk_poet_dynasty_name (dynasty_id, name)

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
- UNIQUE KEY uk_category_name (name)
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
| is_original | TINYINT | 是 | 0 | 是否原创：0-否，1-是 |
| audit_status | TINYINT | 是 | 1 | 审核状态：0-待审核，1-已通过，2-已拒绝 |
| audit_time | DATETIME | 否 | NULL | 审核时间 |
| audit_reason | VARCHAR(500) | 否 | NULL | 审核备注 |
| avg_score | DECIMAL(3,1) | 否 | NULL | 平均评分 |
| rating_count | INT | 是 | 0 | 评分数量 |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT | 是 | 0 | 逻辑删除 |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_poem_title_poet (title, poet_id)
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
| status | TINYINT | 是 | 1 | 状态：0-草稿，1-已发布，2-已关闭 |
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

存储评论信息，支持多级回复和通用目标关联。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | 自增 | 主键 |
| content | TEXT | 是 | - | 评论内容 |
| user_id | BIGINT | 是 | - | 评论用户ID（外键） |
| target_id | BIGINT | 是 | - | 目标ID（帖子ID 或 诗词ID 等） |
| target_type | TINYINT | 是 | - | 目标类型：1-诗词，2-帖子，3-评论 |
| parent_id | BIGINT | 否 | 0 | 父评论ID（0=顶级） |
| reply_to_user_id | BIGINT | 否 | NULL | 回复目标用户ID |
| like_count | INT | 是 | 0 | 点赞次数 |
| status | TINYINT | 是 | 1 | 状态：0-隐藏，1-正常 |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT | 是 | 0 | 逻辑删除 |

**索引**：
- PRIMARY KEY (id)
- KEY idx_comment_target (target_id, target_type)
- KEY idx_comment_user_id (user_id)
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
| target_type | TINYINT | 是 | - | 浏览类型：1-诗词，2-帖子 |
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
| uri | VARCHAR(500) | 否 | NULL | 请求URI |
| params | TEXT | 否 | NULL | 请求参数 |
| result | TEXT | 否 | NULL | 返回结果 |
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
### 12. ai_model（AI模型配置表）

存储AI模型配置信息，支持多提供商、文本/视觉模型切换。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | 自增 | 主键 |
| name | VARCHAR(50) | 是 | - | 模型标识（唯一，如 zhipu、deepseek） |
| display_name | VARCHAR(100) | 是 | - | 显示名称（如 智谱AI） |
| provider | VARCHAR(50) | 是 | - | 提供商（zhipu/deepseek/kimi/nvidia） |
| model_type | ENUM('text','vision','both') | 是 | text | 模型类型：text-纯文本，vision-视觉，both-两者 |
| api_url | VARCHAR(255) | 是 | - | API地址 |
| api_key | VARCHAR(255) | 是 | - | API密钥 |
| model_id | VARCHAR(100) | 是 | - | 模型ID（如 glm-4） |
| vision_model_id | VARCHAR(100) | 否 | NULL | 视觉模型ID（如 glm-4.6v-flash） |
| max_tokens | INT | 否 | 1024 | 最大token数 |
| extra_config | JSON | 否 | NULL | 额外配置（JSON格式） |
| is_enabled | TINYINT | 是 | 1 | 是否启用：0-禁用，1-启用 |
| is_default | TINYINT | 是 | 0 | 是否默认：0-否，1-是 |
| sort_order | INT | 是 | 0 | 排序顺序 |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_ai_model_name (name)
- KEY idx_ai_model_provider (provider)
- KEY idx_ai_model_enabled (is_enabled)

**初始数据**（30 个模型，覆盖多提供商）：
- 智谱AI (zhipu / zhipu-flash / zhipu-glm4-flash)，zhipu 为 both（视觉模型 glm-4v-flash），zhipu-flash(glm-4.7-flash) 为默认；DeepSeek (deepseek，deepseek-v4-flash)，Kimi (kimi)，千问 (qwen)，小米MiMo (mimo)
- NVIDIA NIM (nvidia*) 12 个、OpenRouter 免费档 (free-*) 8 个（含实测可用 minimax/minimax-m2.7:free）
- 密钥统一存放于 secrets/application-secrets.yml 的 ai.providers.*，数据库中 api_key 为占位符，运行时由 AiModelRegistry 按 provider 注入

---

### 13. poet_featured（精选诗人卡片表）

存储首页精选诗人展示数据。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | 自增 | 主键 |
| poet_id | BIGINT | 否 | NULL | 关联诗人ID |
| name | VARCHAR(50) | 是 | - | 诗人姓名 |
| dynasty | VARCHAR(50) | 否 | NULL | 朝代 |
| description | VARCHAR(500) | 否 | NULL | 简介 |
| biography | TEXT | 否 | NULL | 详细生平 |
| image_url | VARCHAR(255) | 否 | NULL | 意境图URL |
| sort_order | INT | 是 | 0 | 排序顺序 |
| status | TINYINT | 是 | 1 | 状态：0-禁用，1-正常 |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT | 是 | 0 | 逻辑删除 |

**索引**：
- PRIMARY KEY (id)
- KEY idx_poet_featured_status (status)
- KEY idx_poet_featured_sort (sort_order)

**初始数据**（6位诗人）：李清照、杜牧、苏轼、上官婉儿、辛弃疾、李白

---

### 14. home_navigation（首页导航数据表）

存储首页导航模块数据，支持作品、流派、朝代三种类型。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | 自增 | 主键 |
| type | VARCHAR(20) | 是 | - | 类型：works-作品，genres-流派，dynasties-朝代 |
| title | VARCHAR(100) | 是 | - | 标题 |
| subtitle | VARCHAR(200) | 否 | NULL | 副标题/描述 |
| image_url | VARCHAR(500) | 否 | NULL | 图片URL |
| link_id | BIGINT | 否 | NULL | 关联ID（诗词ID、分类ID、朝代ID等） |
| link_type | VARCHAR(20) | 否 | NULL | 链接类型：poem-诗词，category-分类，dynasty-朝代 |
| sort_order | INT | 是 | 0 | 排序 |
| status | TINYINT | 是 | 1 | 状态：1-启用，0-禁用 |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT | 是 | 0 | 逻辑删除 |

**索引**：
- PRIMARY KEY (id)
- KEY idx_home_navigation_type (type)
- KEY idx_home_navigation_sort_order (sort_order)
- KEY idx_home_navigation_status (status)

**初始数据**：
- works（作品）：8条，关联诗词表
- genres（流派）：8条，关联分类表
- dynasties（朝代）：8条，关联朝代表

---

### 15. vision_article（诗话视野文章表）

存储诗话视野模块的文章内容。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | 自增 | 主键 |
| title | VARCHAR(200) | 是 | - | 文章标题 |
| content | TEXT | 是 | - | 文章内容 |
| summary | VARCHAR(500) | 否 | NULL | 文章摘要 |
| cover_image | VARCHAR(500) | 否 | NULL | 封面图片URL |
| category | VARCHAR(50) | 否 | NULL | 文章分类 |
| author | VARCHAR(50) | 否 | NULL | 作者 |
| view_count | INT | 是 | 0 | 浏览次数 |
| like_count | INT | 是 | 0 | 点赞次数 |
| is_featured | TINYINT | 是 | 0 | 是否推荐：0-否，1-是 |
| sort_order | INT | 是 | 0 | 排序顺序 |
| status | TINYINT | 是 | 1 | 状态：0-草稿，1-已发布 |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT | 是 | 0 | 逻辑删除 |

**索引**：
- PRIMARY KEY (id)
- KEY idx_vision_article_category (category)
- KEY idx_vision_article_status (status)
- KEY idx_vision_article_create_time (create_time)

---

### 16. visit_log（访问日志表）

存储系统访问日志，用于统计分析。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | 自增 | 主键 |
| user_id | BIGINT | 否 | NULL | 用户ID（未登录为NULL） |
| ip | VARCHAR(50) | 是 | - | 访问IP |
| user_agent | VARCHAR(500) | 否 | NULL | 浏览器UA |
| path | VARCHAR(200) | 否 | NULL | 访问路径 |
| visit_date | DATE | 否 | NULL | 访问日期 |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

**索引**：
- PRIMARY KEY (id)
- KEY idx_visit_log_create_time (create_time)
- KEY idx_visit_log_visit_date (visit_date)
- KEY idx_visit_log_ip (ip)

---

### 17. file_metadata（文件元数据表）

存储文件元数据信息，支持统一文件管理。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | 自增 | 主键 |
| file_key | VARCHAR(255) | 是 | - | 存储路径（相对uploads） |
| original_name | VARCHAR(255) | 否 | NULL | 原始文件名 |
| file_type | VARCHAR(50) | 是 | - | 文件类型：avatar/poem/forum/ai_generated/vision/config/export/temp/backup/audit/watermark/cache |
| mime_type | VARCHAR(100) | 否 | NULL | MIME类型 |
| size | BIGINT | 是 | 0 | 文件大小（字节） |
| md5 | VARCHAR(32) | 否 | NULL | 文件MD5 |
| width | INT | 否 | NULL | 图片宽度 |
| height | INT | 否 | NULL | 图片高度 |
| related_id | BIGINT | 否 | NULL | 关联的用户ID/诗词ID/帖子ID |
| related_type | VARCHAR(50) | 否 | NULL | 关联类型：user/poem/forum/vision/article |
| storage_type | VARCHAR(20) | 是 | local | 存储类型：local/oss |
| status | TINYINT | 是 | 1 | 状态：0-禁用，1-正常 |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_file_key (file_key)
- KEY idx_file_type (file_type)
- KEY idx_related (related_type, related_id)
- KEY idx_md5 (md5)
- KEY idx_create_time (create_time)

---

### 18. ai_image_record（AI生成图片记录表）

存储AI生成图片的记录信息。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | 自增 | 主键 |
| session_id | VARCHAR(100) | 否 | NULL | 会话ID |
| user_id | BIGINT | 否 | NULL | 用户ID |
| model_name | VARCHAR(50) | 是 | - | 模型名称 |
| prompt | TEXT | 是 | - | 生成提示词 |
| negative_prompt | TEXT | 否 | NULL | 负面提示词 |
| image_url | VARCHAR(500) | 是 | - | 图片URL |
| image_key | VARCHAR(255) | 否 | NULL | 图片存储路径 |
| width | INT | 否 | NULL | 图片宽度 |
| height | INT | 否 | NULL | 图片高度 |
| seed | BIGINT | 否 | NULL | 随机种子 |
| steps | INT | 否 | NULL | 生成步数 |
| cfg_scale | DECIMAL(5,2) | 否 | NULL | CFG缩放 |
| generation_time | INT | 否 | NULL | 生成耗时（毫秒） |
| status | TINYINT | 是 | 1 | 状态：0-删除，1-正常 |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

**索引**：
- PRIMARY KEY (id)
- KEY idx_session_id (session_id)
- KEY idx_user_id (user_id)
- KEY idx_model_name (model_name)
- KEY idx_create_time (create_time)

---

### 19. poet_profile（认证诗人资料表）

存储认证诗人的详细资料信息，关联 user 表。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | 自增 | 主键 |
| user_id | BIGINT | 是 | - | 关联用户ID（唯一） |
| pen_name | VARCHAR(50) | 否 | NULL | 笔名 |
| real_name | VARCHAR(50) | 否 | NULL | 真实姓名 |
| specialty | VARCHAR(200) | 否 | NULL | 擅长体裁（逗号分隔：古体诗,近体诗,词,曲,现代诗） |
| introduction | TEXT | 否 | NULL | 诗人简介 |
| literary_concept | TEXT | 否 | NULL | 创作理念 |
| achievements | TEXT | 否 | NULL | 主要成就 |
| contact_info | VARCHAR(200) | 否 | NULL | 联系方式（邮箱/微信） |
| verified_status | TINYINT | 是 | 0 | 认证状态：0-未认证，1-已认证，2-认证中，3-认证失败 |
| verified_time | DATETIME | 否 | NULL | 认证时间 |
| verified_reason | VARCHAR(500) | 否 | NULL | 认证审核备注 |
| work_count | INT | 是 | 0 | 作品数量 |
| like_count | INT | 是 | 0 | 获赞总数 |
| favorite_count | INT | 是 | 0 | 被收藏总数 |
| follower_count | INT | 是 | 0 | 粉丝数 |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT | 是 | 0 | 逻辑删除 |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_poet_profile_user_id (user_id)
- KEY idx_poet_profile_verified_status (verified_status)
- KEY idx_poet_profile_work_count (work_count)
- KEY idx_poet_profile_like_count (like_count)

---

### 20. poem_rating（诗词评分表）

存储诗词的用户评分和 AI 评分记录。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | 自增 | 主键 |
| poem_id | BIGINT | 是 | - | 诗词ID |
| user_id | BIGINT | 否 | NULL | 评分用户ID（AI评分时为NULL） |
| score | DECIMAL(3,1) | 是 | - | 评分（1.0-5.0） |
| rating_type | TINYINT | 是 | 1 | 评分类型：1-用户评分，2-AI评分 |
| dimension | VARCHAR(50) | 否 | NULL | 评分维度（格律,意境,用词,情感,创新） |
| comment | TEXT | 否 | NULL | 评分说明 |
| ai_model | VARCHAR(50) | 否 | NULL | AI模型名称（AI评分时使用） |
| ai_analysis | TEXT | 否 | NULL | AI分析内容 |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

**索引**：
- PRIMARY KEY (id)
- KEY idx_poem_rating_poem_id (poem_id)
- KEY idx_poem_rating_user_id (user_id)
- KEY idx_poem_rating_type (rating_type)
- UNIQUE KEY uk_poem_rating_user_poem (poem_id, user_id, rating_type)

---

### 21. rhyme（韵脚表）

存储平水韵韵脚数据，用于诗词韵律查询。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | 自增 | 主键 |
| character | VARCHAR(10) | 是 | - | 汉字 |
| rhyme_group | VARCHAR(20) | 是 | - | 韵部（如：上平一东、下平一先） |
| tone_type | VARCHAR(10) | 是 | - | 声调类型：平声、上声、去声、入声 |
| rhyme_category | VARCHAR(10) | 是 | - | 韵类：平水韵、词林正韵 |
| sort_order | INT | 是 | 0 | 排序顺序 |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

**索引**：
- PRIMARY KEY (id)
- KEY idx_rhyme_character (character)
- KEY idx_rhyme_group (rhyme_group)
- KEY idx_rhyme_tone_type (tone_type)

**初始数据**：
- 上平15韵（一东、二冬、三江、四支...十五删）
- 下平15韵（一先、二萧、三肴...十五咸）
- 部分仄声韵（上声、去声、入声）

---

### 22. ai_module_config（AI模块模型配置表）

配置 AI 各业务模块（问答、看图写诗、赏析、对联等）使用的模型与提示词。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | 自增 | 主键 |
| module_code | VARCHAR(50) | 是 | - | 模块编码（唯一） |
| module_name | VARCHAR(100) | 是 | - | 模块名称 |
| model_id | BIGINT | 否 | NULL | 关联的AI模型ID（关联 ai_model 表） |
| require_vision | TINYINT | 是 | 0 | 是否需要视觉能力：0-否，1-是 |
| description | VARCHAR(255) | 否 | NULL | 模块描述/角色设定 |
| prompt_template | TEXT | 否 | NULL | 提示词模板，支持{poetName}等变量 |
| max_response_length | INT | 是 | 200 | 最大回答长度(字数) |
| response_style | VARCHAR(50) | 是 | concise | 回答风格：concise-简洁，detailed-详细，balanced-均衡 |
| first_response_length | INT | 是 | 100 | 首次回答最大长度(字数) |
| enable_markdown | TINYINT | 是 | 0 | 是否允许Markdown格式：0-否，1-是 |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_module_code (module_code)
- KEY idx_model_id (model_id)

**初始数据**：chat（AI诗词问答）、poet_chat（诗人对话）、poetry_chat（诗词AI助手）、write_poem（看图写诗）、analyze（诗词鉴赏分析）、couplet（AI对对联），共 6 条

---

### 23. poem_content_cache（诗词内容缓存表）

缓存诗词外部获取的译文、赏析、创作背景等内容，避免重复调用 AI 接口。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | 自增 | 主键 |
| poem_title | VARCHAR(100) | 是 | - | 诗词标题 |
| poet_name | VARCHAR(50) | 是 | '' | 诗人姓名 |
| content_type | VARCHAR(50) | 是 | - | 内容类型：annotation/appreciation/background/author_info/knowledge_dynastyPoetry/knowledge_literarySchool/knowledge_historicalContext |
| content | TEXT | 是 | - | 缓存内容 |
| source | VARCHAR(20) | 是 | ai | 来源：ai/manual |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_poem_content_cache (poem_title, poet_name, content_type)
- KEY idx_poem_content_cache_title (poem_title)

---

### 24. poet_suggestion（诗人内容建议表）

记录用户对诗人内容的修改建议及审核状态。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | 自增 | 主键 |
| poet_id | BIGINT | 是 | - | 诗人ID |
| user_id | BIGINT | 是 | - | 建议用户ID |
| section | VARCHAR(50) | 是 | - | 修改板块 |
| content | TEXT | 是 | - | 建议内容 |
| category | VARCHAR(50) | 否 | other | 意见分类：biography/life_story/influence/evaluation/anecdotes/other |
| status | VARCHAR(20) | 是 | pending | 状态 |
| review_comment | VARCHAR(500) | 否 | NULL | 审核备注 |
| reviewer_id | BIGINT | 否 | NULL | 审核者ID |
| ip | VARCHAR(50) | 否 | NULL | 提交者IP |
| review_time | DATETIME | 否 | NULL | 审核时间 |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

**索引**：
- PRIMARY KEY (id)
- KEY idx_poet_suggestion_poet_id (poet_id)
- KEY idx_poet_suggestion_user_id (user_id)
- KEY idx_poet_suggestion_status (status)

---

### 25. poet_draft（诗人内容草稿表）

记录管理员编辑诗人内容的草稿及审核流程。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | 自增 | 主键 |
| poet_id | BIGINT | 是 | - | 诗人ID |
| section | VARCHAR(50) | 是 | - | 编辑板块：biography/life_story/influence/evaluation/anecdotes |
| content | TEXT | 是 | - | 草稿内容 |
| original_content | TEXT | 否 | NULL | 修改前的原始内容 |
| editor_id | BIGINT | 是 | - | 编辑者ID（管理员） |
| status | TINYINT | 是 | 0 | 状态：0-待审核，1-已通过（已发布），2-已拒绝 |
| reviewer_id | BIGINT | 否 | NULL | 审核者ID |
| review_comment | VARCHAR(500) | 否 | NULL | 审核备注 |
| review_time | DATETIME | 否 | NULL | 审核时间 |
| ip | VARCHAR(50) | 否 | NULL | 编辑者IP |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

**索引**：
- PRIMARY KEY (id)
- KEY idx_poet_draft_poet_id (poet_id)
- KEY idx_poet_draft_editor_id (editor_id)
- KEY idx_poet_draft_status (status)
- KEY idx_poet_draft_create_time (create_time)

---

### 26. static_page（静态页面内容表）

存储网站静态页面（使用条款、隐私政策、联系我们）内容，content 为 JSON 格式的章节数组。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | 自增 | 主键 |
| page_key | VARCHAR(50) | 是 | - | 页面标识（如：terms、privacy、contact，唯一） |
| title | VARCHAR(100) | 是 | - | 页面标题 |
| content | TEXT | 是 | - | 页面内容 |
| status | TINYINT | 是 | 1 | 状态：0-禁用，1-启用 |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_static_page_key (page_key)

**初始数据**：terms（使用条款）、privacy（隐私政策）、contact（联系我们），共 3 条

---

### 27. ai_generated_content（AI生成内容审核表）

记录 AI 生成的诗词/诗人内容及人工审核状态。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | 自增 | 主键 |
| target_type | VARCHAR(20) | 是 | - | 目标类型：poem-诗词，poet-诗人 |
| target_id | BIGINT | 是 | - | 目标ID（诗词ID或诗人ID） |
| target_name | VARCHAR(200) | 否 | NULL | 目标名称（诗词标题或诗人姓名） |
| field_name | VARCHAR(50) | 是 | - | 字段名：translation/appreciation/background/biography/life_story/influence/evaluation/anecdotes |
| content | TEXT | 是 | - | AI生成的内容 |
| ai_model | VARCHAR(50) | 否 | NULL | 使用的AI模型 |
| status | TINYINT | 是 | 0 | 审核状态：0-待审核，1-已通过，2-已拒绝 |
| reviewer_id | BIGINT | 否 | NULL | 审核者ID |
| review_comment | VARCHAR(500) | 否 | NULL | 审核备注 |
| review_time | DATETIME | 否 | NULL | 审核时间 |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |

**索引**：
- PRIMARY KEY (id)
- KEY idx_agc_target (target_type, target_id)
- KEY idx_agc_status (status)
- KEY idx_agc_create_time (create_time)

---

### 28. repair_order（报修工单表）

记录用户报修工单，支持处理进度跟踪与满意度评价。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | 自增 | 主键 |
| order_no | VARCHAR(32) | 是 | - | 工单编号（唯一） |
| title | VARCHAR(200) | 是 | - | 问题标题 |
| description | TEXT | 是 | - | 问题描述 |
| category | VARCHAR(50) | 是 | - | 报修类别：system-系统故障,function-功能异常,ui-界面问题,data-数据问题,other-其他 |
| priority | TINYINT | 是 | 2 | 优先级：1-低,2-中,3-高,4-紧急 |
| status | TINYINT | 是 | 0 | 状态：0-待处理,1-处理中,2-已解决,3-已关闭,4-已驳回 |
| images | TEXT | 否 | NULL | 问题截图URL（JSON数组） |
| user_id | BIGINT | 是 | - | 提交用户ID |
| assignee_id | BIGINT | 否 | NULL | 处理人ID |
| resolve_content | TEXT | 否 | NULL | 解决方案 |
| resolve_time | DATETIME | 否 | NULL | 解决时间 |
| close_time | DATETIME | 否 | NULL | 关闭时间 |
| satisfaction | TINYINT | 否 | NULL | 满意度评分：1-5 |
| satisfaction_comment | VARCHAR(500) | 否 | NULL | 满意度评价 |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | 是 | CURRENT_TIMESTAMP | 更新时间 |
| deleted | TINYINT | 是 | 0 | 逻辑删除：0-未删除，1-已删除 |

**索引**：
- PRIMARY KEY (id)
- UNIQUE KEY uk_order_no (order_no)
- KEY idx_repair_user_id (user_id)
- KEY idx_repair_status (status)
- KEY idx_repair_category (category)
- KEY idx_repair_priority (priority)
- KEY idx_repair_assignee_id (assignee_id)
- KEY idx_repair_create_time (create_time)

---

### 29. repair_comment（报修反馈表）

记录报修工单的沟通记录与反馈，支持内部备注。

| 字段名 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| id | BIGINT | 是 | 自增 | 主键 |
| repair_order_id | BIGINT | 是 | - | 工单ID（关联 repair_order 表） |
| user_id | BIGINT | 是 | - | 评论用户ID |
| content | TEXT | 是 | - | 评论内容 |
| images | TEXT | 否 | NULL | 附图URL（JSON数组） |
| is_internal | TINYINT | 是 | 0 | 是否内部备注：0-用户可见,1-仅管理员可见 |
| create_time | DATETIME | 是 | CURRENT_TIMESTAMP | 创建时间 |

**索引**：
- PRIMARY KEY (id)
- KEY idx_rc_repair_order_id (repair_order_id)
- KEY idx_rc_user_id (user_id)
- KEY idx_rc_create_time (create_time)

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
  │            │
  │            └── poem_rating（poem_id FK，用户评分/AI评分）
  │
  │
  └── poet_featured（poet_id FK，精选诗人展示）

user ──┬── forum_post（user_id FK）
       │      │
       │      │ target_id (FK，target_type=2)
       │      ▼
       ├── comment（user_id FK, 多态 target_id + target_type, 支持多级回复：parent_id 自关联）
       │
       ├── user_favorite（多态：target_type → poem/post/poet）
       │
       ├── user_like（多态：target_type → poem/post/comment）
       │
       ├── user_history（多态：target_type → poem/post）
       │
       ├── poet_profile（user_id FK，诗人认证资料，一对一）
       │
       └── operation_log（user_id FK，可为NULL）

ai_model（独立配置表，无外键关联）
  │
  └── ai_module_config（model_id FK，AI模块关联模型）

home_navigation（独立配置表，link_id 关联 poem/category/dynasty）

vision_article（独立文章表，无外键关联）

visit_log（独立日志表，user_id FK 可为NULL）

rhyme（独立韵脚表，无外键关联，用于韵律查询）

file_metadata（独立文件元数据表，related_id 多态关联 user/poem/forum/vision/article）

ai_image_record（独立AI图片记录表，user_id FK 可为NULL）

poem_content_cache（独立缓存表，按诗词标题/诗人/内容类型去重）

poet ──┬── poet_suggestion（poet_id FK，用户建议）
       └── poet_draft（poet_id FK，管理员草稿）

ai_generated_content（独立AI内容审核表，target_type+target_id 多态关联 poem/poet）

user ──┬── repair_order（user_id FK，报修工单）
       └── repair_comment（repair_order_id FK → repair_order，user_id FK）

static_page（独立静态页面表，无外键关联）
```

### 外键约束说明

| 外键约束名 | 源表.字段 | 引用表.字段 | 说明 |
|------------|-----------|-------------|------|
| fk_poem_poet | poem.poet_id | poet.id | 诗词→诗人 |
| fk_poem_dynasty | poem.dynasty_id | dynasty.id | 诗词→朝代 |
| fk_poem_category | poem.category_id | category.id | 诗词→分类 |
| fk_forum_post_user | forum_post.user_id | user.id | 帖子→用户 |
| fk_comment_user | comment.user_id | user.id | 评论→用户 |
| fk_user_favorite_user | user_favorite.user_id | user.id | 收藏→用户 |
| fk_user_like_user | user_like.user_id | user.id | 点赞→用户 |
| fk_ai_image_record_user | ai_image_record.user_id | user.id | AI图片记录→用户（ON DELETE SET NULL） |

> **注意**：外键约束在 `database-standards.md` 的建表SQL中定义。`init.sql` 中大部分建表语句为简化版，未包含外键约束，但 `ai_image_record` 表包含 `fk_ai_image_record_user` 外键约束。`comment` 表已从 `post_id` 重构为 `target_id + target_type` 多态模式，不再直接外键关联 `forum_post`。

---

**文档版本**：v2.3
**最后更新**：2026-08-29（表清单补全至 29 张表，新增 ai_module_config、poem_content_cache、poet_suggestion、poet_draft、static_page、ai_generated_content、repair_order、repair_comment 结构说明，同步 init.sql v3.0 全量合并版）
**维护人员**：墨渊开发团队