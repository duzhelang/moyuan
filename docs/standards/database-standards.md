# 数据库设计规范文档

## 概述

本文档定义了"古今诗话——墨渊"项目数据库设计的规范，包括命名规则、表结构设计、索引设计、SQL编写规范等。

## 数据库选型

### 主数据库

| 数据库 | 版本 | 说明 |
|--------|------|------|
| MySQL | 8.0+ | 关系型数据库，存储核心业务数据 |

### 缓存数据库

| 数据库 | 版本 | 说明 |
|--------|------|------|
| Redis | 7.x | 缓存热点数据、会话信息 |

## 命名规范

### 数据库命名
- 数据库名：`moyuan`（小写，下划线分隔）
- 测试数据库：`moyuan_test`

### 表命名
- 全部小写，下划线分隔：`user`、`poem`、`forum_post`
- 业务表前缀：无（核心业务表）
- 关联表：使用两个表名组合：`user_favorite`、`poem_category`
- 临时表：`tmp_` 前缀
- 备份表：`bak_` 前缀 + 日期

### 字段命名
- 全部小写，下划线分隔：`user_id`、`create_time`
- 主键：`id`
- 外键：`关联表名_id`（如 `user_id`、`poem_id`）
- 布尔值：`is_` 前缀（如 `is_deleted`、`is_active`）
- 时间字段：`_time` 后缀（如 `create_time`、`update_time`）
- 状态字段：`status` 或 `state`
- 备注字段：`remark` 或 `description`

### 索引命名
- 主键索引：`pk_表名`（如 `pk_user`）
- 唯一索引：`uk_表名_字段名`（如 `uk_user_username`）
- 普通索引：`idx_表名_字段名`（如 `idx_poem_dynasty_id`）
- 全文索引：`ft_表名_字段名`（如 `ft_poem_title`）

## 数据库设计

### 数据库字符集

```sql
CREATE DATABASE moyuan
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

### 表结构设计

#### 1. 用户表 (user)

```sql
CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `bio` VARCHAR(500) DEFAULT NULL COMMENT '个人简介',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  UNIQUE KEY `uk_user_email` (`email`),
  KEY `idx_user_status` (`status`),
  KEY `idx_user_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
```

#### 2. 诗人表 (poet)

```sql
CREATE TABLE `poet` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '诗人ID',
  `name` VARCHAR(50) NOT NULL COMMENT '诗人姓名',
  `courtesy_name` VARCHAR(50) DEFAULT NULL COMMENT '字',
  `pseudonym` VARCHAR(50) DEFAULT NULL COMMENT '号',
  `dynasty_id` BIGINT NOT NULL COMMENT '朝代ID',
  `birth_year` INT DEFAULT NULL COMMENT '出生年份',
  `death_year` INT DEFAULT NULL COMMENT '去世年份',
  `birthplace` VARCHAR(100) DEFAULT NULL COMMENT '出生地',
  `biography` TEXT COMMENT '生平简介',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_poet_dynasty_id` (`dynasty_id`),
  KEY `idx_poet_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='诗人表';
```

#### 3. 朝代表 (dynasty)

```sql
CREATE TABLE `dynasty` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '朝代ID',
  `name` VARCHAR(50) NOT NULL COMMENT '朝代名称',
  `start_year` INT DEFAULT NULL COMMENT '开始年份',
  `end_year` INT DEFAULT NULL COMMENT '结束年份',
  `description` TEXT COMMENT '朝代简介',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dynasty_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='朝代表';
```

#### 4. 诗词分类表 (category)

```sql
CREATE TABLE `category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父分类ID，0表示顶级分类',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '分类描述',
  `icon` VARCHAR(255) DEFAULT NULL COMMENT '分类图标',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='诗词分类表';
```

#### 5. 诗词表 (poem)

```sql
CREATE TABLE `poem` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '诗词ID',
  `title` VARCHAR(100) NOT NULL COMMENT '诗词标题',
  `content` TEXT NOT NULL COMMENT '诗词内容',
  `poet_id` BIGINT DEFAULT NULL COMMENT '诗人ID',
  `dynasty_id` BIGINT DEFAULT NULL COMMENT '朝代ID',
  `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
  `translation` TEXT COMMENT '译文',
  `appreciation` TEXT COMMENT '赏析',
  `background` TEXT COMMENT '创作背景',
  `source` VARCHAR(255) DEFAULT NULL COMMENT '来源',
  `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞次数',
  `favorite_count` INT NOT NULL DEFAULT 0 COMMENT '收藏次数',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-草稿，1-已发布，2-待审核',
  `is_featured` TINYINT NOT NULL DEFAULT 0 COMMENT '是否精选：0-否，1-是',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_poem_poet_id` (`poet_id`),
  KEY `idx_poem_dynasty_id` (`dynasty_id`),
  KEY `idx_poem_category_id` (`category_id`),
  KEY `idx_poem_status` (`status`),
  KEY `idx_poem_create_time` (`create_time`),
  FULLTEXT KEY `ft_poem_title_content` (`title`, `content`),
  CONSTRAINT `fk_poem_poet` FOREIGN KEY (`poet_id`) REFERENCES `poet` (`id`),
  CONSTRAINT `fk_poem_dynasty` FOREIGN KEY (`dynasty_id`) REFERENCES `dynasty` (`id`),
  CONSTRAINT `fk_poem_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='诗词表';
```

#### 6. 论坛帖子表 (forum_post)

```sql
CREATE TABLE `forum_post` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '帖子ID',
  `title` VARCHAR(200) NOT NULL COMMENT '帖子标题',
  `content` TEXT NOT NULL COMMENT '帖子内容',
  `user_id` BIGINT NOT NULL COMMENT '发帖用户ID',
  `category` VARCHAR(50) DEFAULT NULL COMMENT '帖子分类',
  `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞次数',
  `comment_count` INT NOT NULL DEFAULT 0 COMMENT '评论数量',
  `is_top` TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶：0-否，1-是',
  `is_featured` TINYINT NOT NULL DEFAULT 0 COMMENT '是否精选：0-否，1-是',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-草稿，1-已发布，2-已关闭，3-已删除',
  `last_comment_time` DATETIME DEFAULT NULL COMMENT '最后评论时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_forum_post_user_id` (`user_id`),
  KEY `idx_forum_post_category` (`category`),
  KEY `idx_forum_post_status` (`status`),
  KEY `idx_forum_post_create_time` (`create_time`),
  FULLTEXT KEY `ft_forum_post_title_content` (`title`, `content`),
  CONSTRAINT `fk_forum_post_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论坛帖子表';
```

#### 7. 评论表 (comment)

```sql
CREATE TABLE `comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `content` TEXT NOT NULL COMMENT '评论内容',
  `user_id` BIGINT NOT NULL COMMENT '评论用户ID',
  `target_id` BIGINT NOT NULL COMMENT '目标ID',
  `target_type` TINYINT NOT NULL COMMENT '目标类型：1-诗词，2-帖子，3-评论',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父评论ID，0表示顶级评论',
  `reply_to_user_id` BIGINT DEFAULT NULL COMMENT '回复目标用户ID',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞次数',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-隐藏，1-正常',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_comment_target` (`target_id`, `target_type`),
  KEY `idx_comment_user_id` (`user_id`),
  KEY `idx_comment_parent_id` (`parent_id`),
  KEY `idx_comment_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';
```

#### 8. 用户收藏表 (user_favorite)

```sql
CREATE TABLE `user_favorite` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `target_id` BIGINT NOT NULL COMMENT '收藏目标ID',
  `target_type` TINYINT NOT NULL COMMENT '收藏类型：1-诗词，2-帖子，3-诗人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_favorite` (`user_id`, `target_id`, `target_type`),
  KEY `idx_user_favorite_target` (`target_id`, `target_type`),
  CONSTRAINT `fk_user_favorite_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收藏表';
```

#### 9. 用户点赞表 (user_like)

```sql
CREATE TABLE `user_like` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `target_id` BIGINT NOT NULL COMMENT '点赞目标ID',
  `target_type` TINYINT NOT NULL COMMENT '点赞类型：1-诗词，2-帖子，3-评论',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_like` (`user_id`, `target_id`, `target_type`),
  KEY `idx_user_like_target` (`target_id`, `target_type`),
  CONSTRAINT `fk_user_like_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户点赞表';
```

#### 10. 用户浏览历史表 (user_history)

```sql
CREATE TABLE `user_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `target_id` BIGINT NOT NULL COMMENT '浏览目标ID',
  `target_type` TINYINT NOT NULL COMMENT '浏览类型：1-诗词，2-帖子',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_history_user_id` (`user_id`),
  KEY `idx_user_history_target` (`target_id`, `target_type`),
  KEY `idx_user_history_create_time` (`create_time`),
  CONSTRAINT `fk_user_history_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户浏览历史表';
```

#### 11. 操作日志表 (operation_log)

```sql
CREATE TABLE `operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '操作用户ID',
  `username` VARCHAR(50) DEFAULT NULL COMMENT '操作用户名',
  `operation` VARCHAR(50) NOT NULL COMMENT '操作类型',
  `method` VARCHAR(200) NOT NULL COMMENT '请求方法',
  `uri` VARCHAR(500) DEFAULT NULL COMMENT '请求URI',
  `params` TEXT COMMENT '请求参数',
  `result` TEXT COMMENT '返回结果',
  `ip` VARCHAR(50) DEFAULT NULL COMMENT '请求IP',
  `duration` INT DEFAULT NULL COMMENT '请求时长（毫秒）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '操作状态：0-失败，1-成功',
  `error_msg` TEXT COMMENT '错误信息',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_operation_log_user_id` (`user_id`),
  KEY `idx_operation_log_operation` (`operation`),
  KEY `idx_operation_log_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';
```

#### 12. AI模型配置表 (ai_model)

```sql
CREATE TABLE `ai_model` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(50) NOT NULL COMMENT '模型标识',
  `display_name` VARCHAR(100) NOT NULL COMMENT '显示名称',
  `provider` VARCHAR(50) NOT NULL COMMENT '提供商',
  `model_type` ENUM('text', 'vision', 'both') NOT NULL DEFAULT 'text' COMMENT '模型类型',
  `api_url` VARCHAR(255) NOT NULL COMMENT 'API地址',
  `api_key` VARCHAR(255) NOT NULL COMMENT 'API密钥',
  `model_id` VARCHAR(100) NOT NULL COMMENT '模型ID',
  `vision_model_id` VARCHAR(100) DEFAULT NULL COMMENT '视觉模型ID',
  `max_tokens` INT DEFAULT 1024 COMMENT '最大token数',
  `extra_config` JSON DEFAULT NULL COMMENT '额外配置',
  `is_enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
  `is_default` TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认：0-否，1-是',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_model_name` (`name`),
  KEY `idx_ai_model_provider` (`provider`),
  KEY `idx_ai_model_enabled` (`is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI模型配置表';
```

#### 13. poet_featured（精选诗人卡片表）

```sql
CREATE TABLE IF NOT EXISTS `poet_featured` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `poet_id` BIGINT DEFAULT NULL COMMENT '关联诗人ID',
  `name` VARCHAR(50) NOT NULL COMMENT '诗人姓名',
  `dynasty` VARCHAR(50) DEFAULT NULL COMMENT '朝代',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '简介',
  `biography` TEXT DEFAULT NULL COMMENT '详细生平',
  `image_url` VARCHAR(255) DEFAULT NULL COMMENT '意境图URL',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_poet_featured_status` (`status`),
  KEY `idx_poet_featured_sort` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='精选诗人卡片表';
```

---


#### 14. home_navigation（首页导航数据表）

```sql
CREATE TABLE IF NOT EXISTS `home_navigation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type` VARCHAR(20) NOT NULL COMMENT '类型：works-作品，genres-流派，dynasties-朝代',
  `title` VARCHAR(100) NOT NULL COMMENT '标题',
  `subtitle` VARCHAR(200) DEFAULT NULL COMMENT '副标题/描述',
  `image_url` VARCHAR(500) DEFAULT NULL COMMENT '图片URL',
  `link_id` BIGINT DEFAULT NULL COMMENT '关联ID（诗词ID、分类ID、朝代ID等）',
  `link_type` VARCHAR(20) DEFAULT NULL COMMENT '链接类型：poem-诗词，category-分类，dynasty-朝代',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_home_navigation_type` (`type`),
  KEY `idx_home_navigation_sort_order` (`sort_order`),
  KEY `idx_home_navigation_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='首页导航数据表';
```

---


#### 15. ai_module_config（AI模块模型配置表）

```sql
CREATE TABLE IF NOT EXISTS `ai_module_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `module_code` VARCHAR(50) NOT NULL COMMENT '模块编码',
  `module_name` VARCHAR(100) NOT NULL COMMENT '模块名称',
  `model_id` BIGINT DEFAULT NULL COMMENT '关联的AI模型ID',
  `require_vision` TINYINT NOT NULL DEFAULT 0 COMMENT '是否需要视觉能力：0-否，1-是',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '模块描述/角色设定',
  `prompt_template` TEXT DEFAULT NULL COMMENT '提示词模板，支持{poetName}等变量',
  `max_response_length` INT NOT NULL DEFAULT 200 COMMENT '最大回答长度(字数)',
  `response_style` VARCHAR(50) DEFAULT 'concise' COMMENT '回答风格：concise-简洁，detailed-详细，balanced-均衡',
  `first_response_length` INT NOT NULL DEFAULT 100 COMMENT '首次回答最大长度(字数)',
  `enable_markdown` TINYINT NOT NULL DEFAULT 0 COMMENT '是否允许Markdown格式：0-否，1-是',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_module_code` (`module_code`),
  KEY `idx_model_id` (`model_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI模块模型配置表';
```

---


#### 16. vision_article（诗话视野文章表）

```sql
CREATE TABLE IF NOT EXISTS `vision_article` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` VARCHAR(200) NOT NULL COMMENT '文章标题',
  `summary` VARCHAR(500) DEFAULT NULL COMMENT '文章摘要',
  `content` TEXT NOT NULL COMMENT '文章正文',
  `cover_image` VARCHAR(500) DEFAULT NULL COMMENT '封面图片URL',
  `author` VARCHAR(50) DEFAULT NULL COMMENT '作者',
  `category` VARCHAR(50) DEFAULT NULL COMMENT '文章分类',
  `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞次数',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-草稿，1-已发布',
  `is_featured` TINYINT NOT NULL DEFAULT 0 COMMENT '是否推荐：0-否，1-是',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_vision_article_status` (`status`),
  KEY `idx_vision_article_category` (`category`),
  KEY `idx_vision_article_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='诗话视野文章表';
```

---


#### 17. visit_log（访问日志表）

```sql
CREATE TABLE IF NOT EXISTS `visit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID（未登录为NULL）',
  `ip` VARCHAR(50) NOT NULL COMMENT '访问IP',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '浏览器UA',
  `path` VARCHAR(200) DEFAULT NULL COMMENT '访问路径',
  `visit_date` DATE DEFAULT NULL COMMENT '访问日期',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_visit_log_create_time` (`create_time`),
  KEY `idx_visit_log_visit_date` (`visit_date`),
  KEY `idx_visit_log_ip` (`ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='访问日志表';
```

---


#### 18. file_metadata（文件元数据表）

```sql
CREATE TABLE IF NOT EXISTS `file_metadata` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `file_key` VARCHAR(255) NOT NULL COMMENT '存储路径（相对uploads）',
  `original_name` VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
  `file_type` VARCHAR(50) NOT NULL COMMENT '文件类型：avatar/poem/forum/ai_generated/vision/config/export/temp/backup/audit/watermark/cache',
  `mime_type` VARCHAR(100) DEFAULT NULL COMMENT 'MIME类型',
  `size` BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
  `md5` VARCHAR(32) DEFAULT NULL COMMENT '文件MD5',
  `width` INT DEFAULT NULL COMMENT '图片宽度',
  `height` INT DEFAULT NULL COMMENT '图片高度',
  `related_id` BIGINT DEFAULT NULL COMMENT '关联的用户ID/诗词ID/帖子ID',
  `related_type` VARCHAR(50) DEFAULT NULL COMMENT '关联类型：user/poem/forum/vision/article',
  `storage_type` VARCHAR(20) NOT NULL DEFAULT 'local' COMMENT '存储类型：local/oss',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_key` (`file_key`),
  KEY `idx_file_type` (`file_type`),
  KEY `idx_related` (`related_type`, `related_id`),
  KEY `idx_md5` (`md5`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件元数据表';
```

---


#### 19. ai_image_record（AI生成图片记录表）

```sql
CREATE TABLE IF NOT EXISTS `ai_image_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_id` VARCHAR(100) DEFAULT NULL COMMENT '会话ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
  `model_name` VARCHAR(50) NOT NULL COMMENT '模型名称',
  `prompt` TEXT NOT NULL COMMENT '生成提示词',
  `negative_prompt` TEXT DEFAULT NULL COMMENT '负面提示词',
  `image_url` VARCHAR(500) NOT NULL COMMENT '图片URL',
  `image_key` VARCHAR(255) DEFAULT NULL COMMENT '图片存储路径',
  `width` INT DEFAULT NULL COMMENT '图片宽度',
  `height` INT DEFAULT NULL COMMENT '图片高度',
  `seed` BIGINT DEFAULT NULL COMMENT '随机种子',
  `steps` INT DEFAULT NULL COMMENT '生成步数',
  `cfg_scale` DECIMAL(5,2) DEFAULT NULL COMMENT 'CFG缩放',
  `generation_time` INT DEFAULT NULL COMMENT '生成耗时（毫秒）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-删除，1-正常',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_model_name` (`model_name`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_ai_image_record_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI生成图片记录表';
```

---


#### 20. poet_profile（认证诗人资料表）

```sql
CREATE TABLE IF NOT EXISTS `poet_profile` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '关联用户ID',
  `pen_name` VARCHAR(50) DEFAULT NULL COMMENT '笔名',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `specialty` VARCHAR(200) DEFAULT NULL COMMENT '擅长体裁（逗号分隔：古体诗,近体诗,词,曲,现代诗）',
  `introduction` TEXT DEFAULT NULL COMMENT '诗人简介',
  `literary_concept` TEXT DEFAULT NULL COMMENT '创作理念',
  `achievements` TEXT DEFAULT NULL COMMENT '主要成就',
  `contact_info` VARCHAR(200) DEFAULT NULL COMMENT '联系方式（邮箱/微信）',
  `verified_status` TINYINT NOT NULL DEFAULT 0 COMMENT '认证状态：0-未认证，1-已认证，2-认证中，3-认证失败',
  `verified_time` DATETIME DEFAULT NULL COMMENT '认证时间',
  `verified_reason` VARCHAR(500) DEFAULT NULL COMMENT '认证审核备注',
  `work_count` INT NOT NULL DEFAULT 0 COMMENT '作品数量',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '获赞总数',
  `favorite_count` INT NOT NULL DEFAULT 0 COMMENT '被收藏总数',
  `follower_count` INT NOT NULL DEFAULT 0 COMMENT '粉丝数',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_poet_profile_user_id` (`user_id`),
  KEY `idx_poet_profile_verified_status` (`verified_status`),
  KEY `idx_poet_profile_work_count` (`work_count`),
  KEY `idx_poet_profile_like_count` (`like_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认证诗人资料表';
```

---


#### 21. poem_rating（诗词评分表）

```sql
CREATE TABLE IF NOT EXISTS `poem_rating` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `poem_id` BIGINT NOT NULL COMMENT '诗词ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '评分用户ID（AI评分时为NULL）',
  `score` DECIMAL(3,1) NOT NULL COMMENT '评分（1.0-5.0）',
  `rating_type` TINYINT NOT NULL DEFAULT 1 COMMENT '评分类型：1-用户评分，2-AI评分',
  `dimension` VARCHAR(50) DEFAULT NULL COMMENT '评分维度（格律,意境,用词,情感,创新）',
  `comment` TEXT DEFAULT NULL COMMENT '评分说明',
  `ai_model` VARCHAR(50) DEFAULT NULL COMMENT 'AI模型名称（AI评分时使用）',
  `ai_summary` TEXT DEFAULT NULL COMMENT 'AI评价概要',
  `ai_analysis` TEXT DEFAULT NULL COMMENT 'AI分析详情',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_poem_rating_poem_id` (`poem_id`),
  KEY `idx_poem_rating_user_id` (`user_id`),
  KEY `idx_poem_rating_type` (`rating_type`),
  UNIQUE KEY `uk_poem_rating_user_poem` (`poem_id`, `user_id`, `rating_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='诗词评分表';
```

---


#### 22. poem_content_cache（诗词内容缓存表）

```sql
CREATE TABLE IF NOT EXISTS `poem_content_cache` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `poem_title` VARCHAR(100) NOT NULL COMMENT '诗词标题',
  `poet_name` VARCHAR(50) NOT NULL DEFAULT '' COMMENT '诗人姓名',
  `content_type` VARCHAR(50) NOT NULL COMMENT '内容类型：annotation/appreciation/background/author_info/knowledge_dynastyPoetry/knowledge_literarySchool/knowledge_historicalContext',
  `content` TEXT NOT NULL COMMENT '缓存内容',
  `source` VARCHAR(20) NOT NULL DEFAULT 'ai' COMMENT '来源：ai/manual',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_poem_content_cache` (`poem_title`, `poet_name`, `content_type`),
  KEY `idx_poem_content_cache_title` (`poem_title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='诗词内容缓存表';
```

---


#### 23. rhyme（韵脚表（平水韵））

```sql
CREATE TABLE IF NOT EXISTS `rhyme` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `character` VARCHAR(4) NOT NULL COMMENT '汉字',
  `rhyme_group` VARCHAR(20) NOT NULL COMMENT '韵部名称（如：东、冬、江、支）',
  `tone_type` VARCHAR(4) NOT NULL COMMENT '声调类型：平声、上声、去声、入声',
  `rhyme_category` VARCHAR(10) NOT NULL COMMENT '韵类：平声（上平/下平）、仄声（上声/去声/入声）',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_rhyme_character` (`character`),
  KEY `idx_rhyme_group` (`rhyme_group`),
  KEY `idx_rhyme_tone` (`tone_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='韵脚表（平水韵）';
```

---


#### 24. poet_suggestion（诗人内容建议表）

```sql
CREATE TABLE IF NOT EXISTS `poet_suggestion` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `poet_id` BIGINT NOT NULL COMMENT '诗人ID',
  `user_id` BIGINT NOT NULL COMMENT '建议用户ID',
  `section` VARCHAR(50) NOT NULL COMMENT '修改板块',
  `content` TEXT NOT NULL COMMENT '建议内容',
  `category` VARCHAR(50) DEFAULT 'other' COMMENT '意见分类：biography/life_story/influence/evaluation/anecdotes/other',
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态',
  `review_comment` VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
  `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核者ID',
  `ip` VARCHAR(50) DEFAULT NULL COMMENT '提交者IP',
  `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_poet_suggestion_poet_id` (`poet_id`),
  KEY `idx_poet_suggestion_user_id` (`user_id`),
  KEY `idx_poet_suggestion_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='诗人内容建议表';
```

---


#### 25. poet_draft（诗人内容草稿表）

```sql
CREATE TABLE IF NOT EXISTS `poet_draft` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `poet_id` BIGINT NOT NULL COMMENT '诗人ID',
  `section` VARCHAR(50) NOT NULL COMMENT '编辑板块：biography/life_story/influence/evaluation/anecdotes',
  `content` TEXT NOT NULL COMMENT '草稿内容',
  `original_content` TEXT DEFAULT NULL COMMENT '修改前的原始内容',
  `editor_id` BIGINT NOT NULL COMMENT '编辑者ID（管理员）',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待审核，1-已通过（已发布），2-已拒绝',
  `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核者ID',
  `review_comment` VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
  `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `ip` VARCHAR(50) DEFAULT NULL COMMENT '编辑者IP',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_poet_draft_poet_id` (`poet_id`),
  KEY `idx_poet_draft_editor_id` (`editor_id`),
  KEY `idx_poet_draft_status` (`status`),
  KEY `idx_poet_draft_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='诗人内容草稿表';
```

---


#### 26. static_page（静态页面内容表）

```sql
CREATE TABLE IF NOT EXISTS `static_page` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `page_key` VARCHAR(50) NOT NULL COMMENT '页面标识（如：terms、privacy、contact）',
  `title` VARCHAR(100) NOT NULL COMMENT '页面标题',
  `content` TEXT NOT NULL COMMENT '页面内容',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_static_page_key` (`page_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='静态页面内容表';
```

---


#### 27. ai_generated_content（AI生成内容审核表）

```sql
CREATE TABLE IF NOT EXISTS `ai_generated_content` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `target_type` VARCHAR(20) NOT NULL COMMENT '目标类型：poem-诗词，poet-诗人',
  `target_id` BIGINT NOT NULL COMMENT '目标ID（诗词ID或诗人ID）',
  `target_name` VARCHAR(200) DEFAULT NULL COMMENT '目标名称（诗词标题或诗人姓名）',
  `field_name` VARCHAR(50) NOT NULL COMMENT '字段名：translation/appreciation/background/biography/life_story/influence/evaluation/anecdotes',
  `content` TEXT NOT NULL COMMENT 'AI生成的内容',
  `ai_model` VARCHAR(50) DEFAULT NULL COMMENT '使用的AI模型',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态：0-待审核，1-已通过，2-已拒绝',
  `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核者ID',
  `review_comment` VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
  `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_agc_target` (`target_type`, `target_id`),
  KEY `idx_agc_status` (`status`),
  KEY `idx_agc_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI生成内容审核表';
```

---


#### 28. repair_order（报修工单表）

```sql
CREATE TABLE IF NOT EXISTS `repair_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` VARCHAR(32) NOT NULL COMMENT '工单编号',
  `title` VARCHAR(200) NOT NULL COMMENT '问题标题',
  `description` TEXT NOT NULL COMMENT '问题描述',
  `category` VARCHAR(50) NOT NULL COMMENT '报修类别：system-系统故障,function-功能异常,ui-界面问题,data-数据问题,other-其他',
  `priority` TINYINT NOT NULL DEFAULT 2 COMMENT '优先级：1-低,2-中,3-高,4-紧急',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待处理,1-处理中,2-已解决,3-已关闭,4-已驳回',
  `images` TEXT DEFAULT NULL COMMENT '问题截图URL（JSON数组）',
  `user_id` BIGINT NOT NULL COMMENT '提交用户ID',
  `assignee_id` BIGINT DEFAULT NULL COMMENT '处理人ID',
  `resolve_content` TEXT DEFAULT NULL COMMENT '解决方案',
  `resolve_time` DATETIME DEFAULT NULL COMMENT '解决时间',
  `close_time` DATETIME DEFAULT NULL COMMENT '关闭时间',
  `satisfaction` TINYINT DEFAULT NULL COMMENT '满意度评分：1-5',
  `satisfaction_comment` VARCHAR(500) DEFAULT NULL COMMENT '满意度评价',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_repair_user_id` (`user_id`),
  KEY `idx_repair_status` (`status`),
  KEY `idx_repair_category` (`category`),
  KEY `idx_repair_priority` (`priority`),
  KEY `idx_repair_assignee_id` (`assignee_id`),
  KEY `idx_repair_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报修工单表';
```

---


#### 29. repair_comment（报修反馈表）

```sql
CREATE TABLE IF NOT EXISTS `repair_comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `repair_order_id` BIGINT NOT NULL COMMENT '工单ID',
  `user_id` BIGINT NOT NULL COMMENT '评论用户ID',
  `content` TEXT NOT NULL COMMENT '评论内容',
  `images` TEXT DEFAULT NULL COMMENT '附图URL（JSON数组）',
  `is_internal` TINYINT NOT NULL DEFAULT 0 COMMENT '是否内部备注：0-用户可见,1-仅管理员可见',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_rc_repair_order_id` (`repair_order_id`),
  KEY `idx_rc_user_id` (`user_id`),
  KEY `idx_rc_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报修反馈表';
```

---

## 索引设计规范

### 索引设计原则

1. **选择性原则**：选择区分度高的字段建索引
2. **最左前缀原则**：复合索引遵循最左前缀
3. **覆盖索引**：尽量使用覆盖索引减少回表
4. **避免过度索引**：单表索引数量不超过5个

### 索引类型选择

| 场景 | 索引类型 | 示例 |
|------|----------|------|
| 主键 | PRIMARY KEY | `id` |
| 唯一约束 | UNIQUE INDEX | `username`、`email` |
| 外键关联 | INDEX | `user_id`、`poem_id` |
| 查询条件 | INDEX | `status`、`create_time` |
| 全文搜索 | FULLTEXT INDEX | `title`、`content` |

### 索引示例

```sql
-- 单列索引
CREATE INDEX idx_poem_poet_id ON poem(poet_id);

-- 复合索引
CREATE INDEX idx_poem_dynasty_category ON poem(dynasty_id, category_id);

-- 覆盖索引
CREATE INDEX idx_poem_cover ON poem(dynasty_id, category_id, status, create_time);
```

## SQL编写规范

### 基本规范

1. **关键字大写**：`SELECT`、`FROM`、`WHERE`、`JOIN` 等
2. **表名别名**：使用有意义的别名
3. **字段列表**：明确指定字段，避免 `SELECT *`
4. **注释说明**：复杂SQL添加注释

### 查询示例

```sql
-- 好的写法
SELECT 
    p.id,
    p.title,
    p.content,
    poet.name AS poet_name,
    d.name AS dynasty_name
FROM 
    poem p
    LEFT JOIN poet ON p.poet_id = poet.id
    LEFT JOIN dynasty d ON p.dynasty_id = d.id
WHERE 
    p.status = 1
    AND p.deleted = 0
ORDER BY 
    p.create_time DESC
LIMIT 10 OFFSET 0;

-- 避免的写法
SELECT * FROM poem WHERE status = 1;
```

### 分页查询

```sql
-- 使用LIMIT分页
SELECT 
    id, title, content
FROM 
    poem
WHERE 
    status = 1
    AND deleted = 0
ORDER BY 
    id DESC
LIMIT 10 OFFSET 20;

-- 使用游标分页（性能更好）
SELECT 
    id, title, content
FROM 
    poem
WHERE 
    id < 1000  -- 上一页最后一条记录的ID
    AND status = 1
    AND deleted = 0
ORDER BY 
    id DESC
LIMIT 10;
```

### 批量操作

```sql
-- 批量插入
INSERT INTO poem (title, content, poet_id, status)
VALUES 
    ('静夜思', '床前明月光...', 1, 1),
    ('春晓', '春眠不觉晓...', 2, 1),
    ('登鹳雀楼', '白日依山尽...', 3, 1);

-- 批量更新
UPDATE poem 
SET status = 1, update_time = NOW()
WHERE id IN (1, 2, 3, 4, 5);

-- 批量删除（逻辑删除）
UPDATE poem 
SET deleted = 1, update_time = NOW()
WHERE id IN (1, 2, 3, 4, 5);
```

## 数据安全规范

### 敏感数据处理

1. **密码加密**：使用BCrypt加密存储
2. **手机号脱敏**：显示为 `138****8888`
3. **邮箱脱敏**：显示为 `t***@example.com`
4. **身份证脱敏**：显示为 `110***********1234`

```sql
-- 手机号脱敏查询
SELECT 
    id,
    username,
    CONCAT(LEFT(phone, 3), '****', RIGHT(phone, 4)) AS phone_masked
FROM 
    user;
```

### SQL注入防护

1. **使用参数化查询**：禁止字符串拼接SQL
2. **输入验证**：对用户输入进行验证和过滤
3. **最小权限**：数据库账号使用最小权限

### 数据备份

```bash
# 完全备份
mysqldump -u root -p moyuan > moyuan_backup_$(date +%Y%m%d).sql

# 增量备份
mysqlbinlog --start-datetime="2026-05-25 00:00:00" mysql-bin.000001 > increment.sql
```

## 性能优化

### 查询优化

1. **避免全表扫描**：确保查询条件使用索引
2. **减少JOIN数量**：单表查询不超过3个表关联
3. **避免子查询**：使用JOIN替代子查询
4. **合理使用缓存**：热点数据使用Redis缓存

### 表设计优化

1. **字段类型选择**：使用最小满足需求的类型
2. **NULL值处理**：尽量使用NOT NULL DEFAULT
3. **大字段分离**：TEXT/BLOB字段单独存储
4. **垂直拆分**：按业务模块拆分表

### 慢查询排查

```sql
-- 开启慢查询日志
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 2;

-- 查看慢查询
SHOW VARIABLES LIKE 'slow_query%';
SHOW VARIABLES LIKE 'long_query_time';

-- 分析慢查询
EXPLAIN SELECT * FROM poem WHERE title LIKE '%静夜思%';
```

## 初始化数据

### 基础数据

```sql
-- 朝代数据（13个，与 init.sql 一致）
INSERT INTO dynasty (name, start_year, end_year, sort_order) VALUES
('先秦', -2100, -221, 1),
('秦朝', -221, -206, 2),
('汉朝', -206, 220, 3),
('魏晋南北朝', 220, 589, 4),
('隋朝', 581, 618, 5),
('唐朝', 618, 907, 6),
('五代十国', 907, 979, 7),
('宋朝', 960, 1279, 8),
('元朝', 1271, 1368, 9),
('明朝', 1368, 1644, 10),
('清朝', 1644, 1912, 11),
('民国', 1912, 1949, 12),
('现代', 1949, NULL, 13);
```

> **分类初始数据说明**：分类数据已重构（2026-06-08），当前生效版本为：
> - **一级分类（6个）**：中国古典诗词、中国现当代诗歌、外国诗歌、主题专题诗词、当代青年原创诗歌、特殊体裁与韵文
> - **二级分类（50个）**：按体裁、流派、主题等维度细分
> - **总计**：56个分类

```sql
-- 诗词分类数据（重构后版本，与 init.sql 一致）
-- 一级分类（parent_id = 0）
INSERT INTO category (id, name, parent_id, description, sort_order) VALUES
(1, '中国古典诗词', 0, '包含古体、近体、词、曲等传统诗歌形式，跨越先秦至清代', 1),
(2, '中国现当代诗歌', 0, '1919年五四运动至今的中国诗歌作品，包含现代诗与当代诗', 2),
(3, '外国诗歌', 0, '世界各国优秀诗歌作品，涵盖欧美、东方、拉美等地区', 3),
(4, '主题专题诗词', 0, '按场景、主题分类的诗词，适合专题阅读与赏析', 4),
(5, '当代青年原创诗歌', 0, '青年创作的现代诗与旧体诗，包含校园诗歌、网络诗歌等', 5),
(6, '特殊体裁与韵文', 0, '戏曲唱词、辞赋、趣味诗词、童谣民谣等特殊文学形式', 6);

-- 二级分类示例（详见 init.sql 完整数据）
-- 中国古典诗词子分类：古体诗(7)、近体诗(8)、词(9)、曲(10) + 近体诗细分(11-14) + 主题分类(15-23)
-- 中国现当代诗歌子分类：新月派(24)、朦胧诗派(25)、象征诗派(26)、现实主义诗派(27)、人生派(28) + 主题(29-32)
-- 外国诗歌子分类：欧美诗歌(33)、东方诗歌(34)、拉美诗歌(35) + 体裁(36-41)
-- 主题专题诗词子分类：节日节气(42)、季节时令(43)、山水风光(44)、家国情怀(45)、友情赠别(46)
-- 当代青年原创诗歌子分类：青年原创现代诗(47)、青年原创旧体诗(48)、校园诗歌(49)、网络诗歌(50)、小众风格原创(51)
-- 特殊体裁与韵文子分类：戏曲唱词(52)、辞赋(53)、趣味诗词(54)、童谣/儿歌(55)、民谣/民歌(56)
```

## 字符编码规范

### 编码要求

| 层级 | 编码设置 | 说明 |
|------|----------|------|
| 数据库 | `utf8mb4` | 支持完整 Unicode（包括 emoji） |
| 表 | `utf8mb4_unicode_ci` | 所有表统一使用 |
| 连接 | `characterEncoding=utf8` | JDBC 连接串参数 |
| 连接器 | `--default-character-set=utf8mb4` | 命令行工具 |

### 编码问题排查

#### 1. 检查数据库编码

```sql
-- 查看数据库编码
SHOW CREATE DATABASE moyuan;

-- 查看表编码
SHOW CREATE TABLE forum_post;

-- 查看连接编码
SHOW VARIABLES LIKE 'character_set%';
```

#### 2. 检查数据是否乱码

```sql
-- 查看可能的乱码数据（连续问号）
SELECT id, title FROM forum_post 
WHERE title REGEXP '[?]{3,}' OR content REGEXP '[?]{3,}';

-- 查看数据的 HEX 编码
SELECT id, HEX(title) FROM forum_post WHERE id = 1;
```

#### 3. 修复乱码数据

```sql
-- 删除乱码数据
DELETE FROM forum_post WHERE title REGEXP '[?]{3,}';

-- 或标记为已删除
UPDATE forum_post SET deleted = 1 WHERE title REGEXP '[?]{3,}';
```

### 终端编码注意事项

**Windows PowerShell 用户必须注意**：

```powershell
# 1. 检查当前编码
chcp

# 2. 切换到 UTF-8（必须！）
chcp 65001

# 3. 然后执行 MySQL 命令
mysql -u root -p --default-character-set=utf8mb4
```

**警告**：在 GBK 编码（chcp 936）的终端中插入中文数据会导致乱码！

---

## 数据库维护

### 定期维护任务

1. **表优化**：定期执行 `OPTIMIZE TABLE`
2. **索引重建**：定期重建碎片化索引
3. **统计信息更新**：定期更新表统计信息
4. **日志清理**：定期清理过期日志数据

```sql
-- 表优化
OPTIMIZE TABLE poem, forum_post, comment;

-- 索引重建
ALTER TABLE poem DROP INDEX idx_poem_title, ADD INDEX idx_poem_title(title);

-- 统计信息更新
ANALYZE TABLE poem, poet, dynasty;
```

---

**文档版本**：v2.0
**最后更新**：2026-09-05（建表 DDL 由第 12 张补齐至全部 29 张，新增 13-29 张表结构，与 init.sql / schema.md 对齐）
**维护人员**：墨渊开发团队