-- 古今诗话——墨渊 数据库表结构脚本
-- 数据库：moyuan
-- 字符集：utf8mb4
-- 排序规则：utf8mb4_unicode_ci
-- 存储引擎：InnoDB

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `moyuan` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `moyuan`;

-- 1. 用户表
CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
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

-- 2. 诗人表
CREATE TABLE `poet` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(50) NOT NULL COMMENT '诗人姓名',
  `courtesy_name` VARCHAR(50) DEFAULT NULL COMMENT '字',
  `pseudonym` VARCHAR(50) DEFAULT NULL COMMENT '号',
  `dynasty_id` BIGINT NOT NULL COMMENT '朝代ID',
  `birth_year` INT DEFAULT NULL COMMENT '出生年份',
  `death_year` INT DEFAULT NULL COMMENT '去世年份',
  `birthplace` VARCHAR(100) DEFAULT NULL COMMENT '出生地',
  `biography` TEXT DEFAULT NULL COMMENT '生平简介',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_poet_dynasty_id` (`dynasty_id`),
  KEY `idx_poet_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='诗人表';

-- 3. 朝代表
CREATE TABLE `dynasty` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(50) NOT NULL COMMENT '朝代名称',
  `start_year` INT DEFAULT NULL COMMENT '开始年份',
  `end_year` INT DEFAULT NULL COMMENT '结束年份',
  `description` TEXT DEFAULT NULL COMMENT '朝代简介',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dynasty_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='朝代表';

-- 4. 诗词分类表
CREATE TABLE `category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父分类ID（0=顶级）',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '分类描述',
  `icon` VARCHAR(255) DEFAULT NULL COMMENT '分类图标',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='诗词分类表';

-- 5. 诗词表
CREATE TABLE `poem` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` VARCHAR(100) NOT NULL COMMENT '诗词标题',
  `content` TEXT NOT NULL COMMENT '诗词内容',
  `poet_id` BIGINT DEFAULT NULL COMMENT '诗人ID',
  `dynasty_id` BIGINT DEFAULT NULL COMMENT '朝代ID',
  `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
  `translation` TEXT DEFAULT NULL COMMENT '译文',
  `appreciation` TEXT DEFAULT NULL COMMENT '赏析',
  `background` TEXT DEFAULT NULL COMMENT '创作背景',
  `source` VARCHAR(255) DEFAULT NULL COMMENT '来源',
  `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞次数',
  `favorite_count` INT NOT NULL DEFAULT 0 COMMENT '收藏次数',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-草稿，1-已发布，2-待审核',
  `is_featured` TINYINT NOT NULL DEFAULT 0 COMMENT '是否精选：0-否，1-是',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_poem_poet_id` (`poet_id`),
  KEY `idx_poem_dynasty_id` (`dynasty_id`),
  KEY `idx_poem_category_id` (`category_id`),
  KEY `idx_poem_status` (`status`),
  KEY `idx_poem_create_time` (`create_time`),
  FULLTEXT KEY `ft_poem_title_content` (`title`, `content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='诗词表';

-- 6. 论坛帖子表
CREATE TABLE `forum_post` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` VARCHAR(200) NOT NULL COMMENT '帖子标题',
  `content` TEXT NOT NULL COMMENT '帖子内容',
  `user_id` BIGINT NOT NULL COMMENT '发帖用户ID',
  `category` VARCHAR(50) DEFAULT NULL COMMENT '帖子分类',
  `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞次数',
  `comment_count` INT NOT NULL DEFAULT 0 COMMENT '评论数量',
  `is_top` TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶',
  `is_featured` TINYINT NOT NULL DEFAULT 0 COMMENT '是否精选',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-草稿，1-已发布，2-已关闭',
  `last_comment_time` DATETIME DEFAULT NULL COMMENT '最后评论时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_forum_post_user_id` (`user_id`),
  KEY `idx_forum_post_category` (`category`),
  KEY `idx_forum_post_status` (`status`),
  KEY `idx_forum_post_create_time` (`create_time`),
  FULLTEXT KEY `ft_forum_post_title_content` (`title`, `content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论坛帖子表';

-- 7. 评论表
CREATE TABLE `comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `content` TEXT NOT NULL COMMENT '评论内容',
  `user_id` BIGINT NOT NULL COMMENT '评论用户ID',
  `post_id` BIGINT NOT NULL COMMENT '帖子ID',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父评论ID（0=顶级）',
  `reply_to_user_id` BIGINT DEFAULT NULL COMMENT '回复目标用户ID',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞次数',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-隐藏，1-正常',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_comment_user_id` (`user_id`),
  KEY `idx_comment_post_id` (`post_id`),
  KEY `idx_comment_parent_id` (`parent_id`),
  KEY `idx_comment_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- 8. 用户收藏表
CREATE TABLE `user_favorite` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `target_id` BIGINT NOT NULL COMMENT '收藏目标ID',
  `target_type` TINYINT NOT NULL COMMENT '收藏类型：1-诗词，2-帖子，3-诗人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_favorite` (`user_id`, `target_id`, `target_type`),
  KEY `idx_user_favorite_target` (`target_id`, `target_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收藏表';

-- 9. 用户点赞表
CREATE TABLE `user_like` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `target_id` BIGINT NOT NULL COMMENT '点赞目标ID',
  `target_type` TINYINT NOT NULL COMMENT '点赞类型：1-诗词，2-帖子，3-评论',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_like` (`user_id`, `target_id`, `target_type`),
  KEY `idx_user_like_target` (`target_id`, `target_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户点赞表';

-- 10. 操作日志表
CREATE TABLE `operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT DEFAULT NULL COMMENT '操作用户ID',
  `username` VARCHAR(50) DEFAULT NULL COMMENT '操作用户名',
  `operation` VARCHAR(50) NOT NULL COMMENT '操作类型',
  `method` VARCHAR(200) NOT NULL COMMENT '请求方法',
  `params` TEXT DEFAULT NULL COMMENT '请求参数',
  `ip` VARCHAR(50) DEFAULT NULL COMMENT '请求IP',
  `duration` INT DEFAULT NULL COMMENT '请求时长（毫秒）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '操作状态：0-失败，1-成功',
  `error_msg` TEXT DEFAULT NULL COMMENT '错误信息',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_operation_log_user_id` (`user_id`),
  KEY `idx_operation_log_operation` (`operation`),
  KEY `idx_operation_log_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';