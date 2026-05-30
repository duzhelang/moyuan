-- ============================================================
-- 古今诗话——墨渊 数据库初始化脚本
-- 版本：v2.0（合并版）
-- 日期：2026-05-29
-- 说明：合并 init.sql + schema.sql + data.sql + migration_002
--       所有建表使用 IF NOT EXISTS，数据使用 INSERT IGNORE
-- ============================================================

CREATE DATABASE IF NOT EXISTS `moyuan` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `moyuan`;

-- ============================================================
-- 1. 朝代表
-- ============================================================
CREATE TABLE IF NOT EXISTS `dynasty` (
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

-- ============================================================
-- 2. 诗人表
-- ============================================================
CREATE TABLE IF NOT EXISTS `poet` (
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
  KEY `idx_poet_name` (`name`),
  UNIQUE KEY `uk_poet_dynasty_name` (`dynasty_id`, `name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='诗人表';

-- ============================================================
-- 3. 诗词分类表
-- ============================================================
CREATE TABLE IF NOT EXISTS `category` (
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
  UNIQUE KEY `uk_category_name` (`name`),
  KEY `idx_category_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='诗词分类表';

-- ============================================================
-- 4. 诗词表
-- ============================================================
CREATE TABLE IF NOT EXISTS `poem` (
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
  UNIQUE KEY `uk_poem_title_poet` (`title`, `poet_id`),
  KEY `idx_poem_poet_id` (`poet_id`),
  KEY `idx_poem_dynasty_id` (`dynasty_id`),
  KEY `idx_poem_category_id` (`category_id`),
  KEY `idx_poem_status` (`status`),
  KEY `idx_poem_create_time` (`create_time`),
  FULLTEXT KEY `ft_poem_title_content` (`title`, `content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='诗词表';

-- ============================================================
-- 5. 用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `gender` TINYINT DEFAULT 0 COMMENT '性别：0-未知，1-男，2-女',
  `birthday` DATE DEFAULT NULL COMMENT '生日',
  `bio` VARCHAR(500) DEFAULT NULL COMMENT '个人简介',
  `role` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色：user-普通用户，admin-管理员',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  UNIQUE KEY `uk_user_email` (`email`),
  KEY `idx_user_status` (`status`),
  KEY `idx_user_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 6. 论坛帖子表
-- ============================================================
CREATE TABLE IF NOT EXISTS `forum_post` (
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

-- ============================================================
-- 7. 评论表
-- ============================================================
CREATE TABLE IF NOT EXISTS `comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `content` TEXT NOT NULL COMMENT '评论内容',
  `user_id` BIGINT NOT NULL COMMENT '评论用户ID',
  `target_id` BIGINT NOT NULL COMMENT '目标ID',
  `target_type` TINYINT NOT NULL COMMENT '目标类型：1-诗词，2-帖子，3-评论',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父评论ID（0=顶级）',
  `reply_to_user_id` BIGINT DEFAULT NULL COMMENT '回复目标用户ID',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞次数',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-隐藏，1-正常',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_comment_target` (`target_id`, `target_type`),
  KEY `idx_comment_user_id` (`user_id`),
  KEY `idx_comment_parent_id` (`parent_id`),
  KEY `idx_comment_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- ============================================================
-- 8. 用户收藏表
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_favorite` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `target_id` BIGINT NOT NULL COMMENT '收藏目标ID',
  `target_type` TINYINT NOT NULL COMMENT '收藏类型：1-诗词，2-帖子，3-诗人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_favorite` (`user_id`, `target_id`, `target_type`),
  KEY `idx_user_favorite_target` (`target_id`, `target_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收藏表';

-- ============================================================
-- 9. 用户点赞表
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_like` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `target_id` BIGINT NOT NULL COMMENT '点赞目标ID',
  `target_type` TINYINT NOT NULL COMMENT '点赞类型：1-诗词，2-帖子，3-评论',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_like` (`user_id`, `target_id`, `target_type`),
  KEY `idx_user_like_target` (`target_id`, `target_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户点赞表';

-- ============================================================
-- 10. 操作日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS `operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT DEFAULT NULL COMMENT '操作用户ID',
  `username` VARCHAR(50) DEFAULT NULL COMMENT '操作用户名',
  `operation` VARCHAR(50) NOT NULL COMMENT '操作类型',
  `method` VARCHAR(200) NOT NULL COMMENT '请求方法',
  `uri` VARCHAR(500) DEFAULT NULL COMMENT '请求URI',
  `params` TEXT DEFAULT NULL COMMENT '请求参数',
  `result` TEXT DEFAULT NULL COMMENT '返回结果',
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

-- ============================================================
-- 11. 用户浏览历史表
-- ============================================================
CREATE TABLE IF NOT EXISTS `user_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `target_id` BIGINT NOT NULL COMMENT '浏览目标ID',
  `target_type` TINYINT NOT NULL COMMENT '浏览类型：1-诗词，2-帖子',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_history_user_id` (`user_id`),
  KEY `idx_user_history_target` (`target_id`, `target_type`),
  KEY `idx_user_history_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户浏览历史表';

-- ============================================================
-- 初始数据：朝代
-- ============================================================
INSERT IGNORE INTO `dynasty` (`name`, `start_year`, `end_year`, `description`, `sort_order`) VALUES
('先秦', -2100, -221, '先秦时期，包括夏、商、周等朝代', 1),
('秦朝', -221, -206, '秦朝，中国第一个统一的封建王朝', 2),
('汉朝', -206, 220, '汉朝，分为西汉和东汉', 3),
('魏晋南北朝', 220, 589, '魏晋南北朝时期', 4),
('隋朝', 581, 618, '隋朝，结束南北朝分裂局面', 5),
('唐朝', 618, 907, '唐朝，中国诗歌发展的鼎盛时期', 6),
('五代十国', 907, 979, '五代十国时期', 7),
('宋朝', 960, 1279, '宋朝，分为北宋和南宋', 8),
('元朝', 1271, 1368, '元朝，蒙古族建立的王朝', 9),
('明朝', 1368, 1644, '明朝，汉族建立的最后一个封建王朝', 10),
('清朝', 1644, 1912, '清朝，中国最后一个封建王朝', 11),
('民国', 1912, 1949, '中华民国时期', 12),
('现代', 1949, NULL, '现代时期', 13);

-- ============================================================
-- 初始数据：诗词分类
-- ============================================================
INSERT IGNORE INTO `category` (`name`, `parent_id`, `description`, `sort_order`) VALUES
('古体诗', 0, '古体诗，包括四言、五言、七言等', 1),
('近体诗', 0, '近体诗，包括律诗、绝句等', 2),
('词', 0, '词，包括各种词牌', 3),
('曲', 0, '曲，包括散曲、杂剧等', 4),
('五言律诗', 2, '五言律诗', 1),
('七言律诗', 2, '七言律诗', 2),
('五言绝句', 2, '五言绝句', 3),
('七言绝句', 2, '七言绝句', 4);

-- ============================================================
-- 初始数据：诗人
-- ============================================================
INSERT IGNORE INTO `poet` (`name`, `dynasty_id`, `biography`, `status`) VALUES
('常平逼王', 13, '现代诗人，作品多表现个人情感与生活感悟', 1);

-- ============================================================
-- 初始数据：诗词
-- ============================================================
INSERT IGNORE INTO `poem` (`title`, `content`, `poet_id`, `dynasty_id`, `category_id`, `source`, `status`, `is_featured`) VALUES
('愿如花已落千行', '愿如花已落千行，未闻花语浅别殇。\n幽僻心境漫过少，唯有诗语解锁缰。\n金甲未脱抬眼望，怒剑难收向疆场。\n笑祝功成人与事，再鼓一旬又何妨。', 1, 13, 2, '常平逼王', 1, 1),
('西江月.未语', '华灯初照白路，夜来树影凄楚，俳徊只与月相伴，茫然一片迷雾。\n离人兀自心扬，未语难避哀伤，只若通明过后，真心但化霓裳。', 1, 13, 2, '常平逼王', 1, 1),
('忆江南.哀', '秋风寒，\n落红自凋残。\n未识伊人愁几许，\n伊人兀自笑颜开。\n吾心不胜哀！', 1, 13, 2, '常平逼王', 1, 1),
('东风吹起白蝴蝶', '东风吹起白蝴蝶，散入云幕尽青烟。\n但觉离魂归来少，春日暖暖艳阳天。\n松柏长青久可立，香印再燃意更坚。\n思情尽付潇湘处，绵远流长年复年。', 1, 13, 2, '常平逼王', 1, 1),
('清平乐.梦怀', '雨落窗外，淅声渐消潺，一帘凝愁入梦来，若雾浸湿枕畔。\n经年离愁之初，再会此情更苦，最是万千惆怅，与白共登天目!', 1, 13, 2, '常平逼王', 1, 1),
('乌夜啼.叹城府', '淡雾兀遮明眸，更愁，独倚玉阑不觉泪空流。\n人情恶，谁易错，自长酌，一心明月向沟成污浊。', 1, 13, 2, '常平逼王', 1, 1),
('新桃映红把福迎', '新桃映红把福迎，银烛高照万家兴。\n火尾连排上清夜，笑语欢声动耳惊。\n奈何遣词功夫浅，此夜此景休论明。\n唯愿持酒歌一曲，与君同乐一身轻', 1, 13, 2, '常平逼王', 1, 1),
('回首一九已成空', '回首一九已成空，展望二零佳意浓。\n世事岂敢轻年少，天意哪可断前程。\n唐猊又着战意显，三尺重出徙侣从。\n烂柯沉木枉悲夫，夫且若怡也可容。', 1, 13, 2, '常平逼王', 1, 1),
('沉影迷叠千层障', '沉影迷叠千层障，乱云归处是它乡。\n酒酣仍识昔日客，心迷难辨眼前芳。\n纵把凡锦比仙缎，不需经年多思量。\n一笑即随羊角去，九风还作万华芳。', 1, 13, 2, '常平逼王', 1, 1),
('一剪梅.无题', '一别三秋未招摇，\n山也迢迢，水也昭昭。\n何人再添新衣袍，\n笑意盈绕，喜上眉梢。\n忆昔花开岁月好，\n蜂字飘摇，蝶字舞蹈。\n而今方知云未晓，\n风又飘飘，雨又萧萧。', 1, 13, 2, '常平逼王', 1, 1),
('清平乐.情(道姑)', '幽情迷朦，三年怀一梦，一遭尘世皆为恒，无怨无悔无憎。\n来日打马南屏，忆昔紫夜流萤，纵剑吴钩霜雪，锦夜孤灯长明。', 1, 13, 4, '初芒', 1, 1);

-- ============================================================
-- AI 模型配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_model` (
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

-- 初始数据：国内可连接的AI模型
INSERT IGNORE INTO `ai_model` (`name`, `display_name`, `provider`, `model_type`, `api_url`, `api_key`, `model_id`, `vision_model_id`, `max_tokens`, `is_enabled`, `is_default`, `sort_order`) VALUES
('zhipu', '智谱AI', 'zhipu', 'both', 'https://open.bigmodel.cn/api/paas/v4/chat/completions', 'your-zhipu-api-key', 'glm-4', 'glm-4.6v-flash', 1024, 0, 0, 1),
('deepseek', 'DeepSeek', 'deepseek', 'text', 'https://api.deepseek.com/v1/chat/completions', 'your-deepseek-api-key', 'deepseek-chat', NULL, 1024, 0, 0, 2),
('kimi', 'Kimi', 'kimi', 'text', 'https://api.moonshot.cn/v1/chat/completions', 'your-kimi-api-key', 'moonshot-v1-8k', NULL, 1024, 0, 0, 3),
('nvidia', 'NVIDIA NIM', 'nvidia', 'text', 'https://integrate.api.nvidia.com/v1/chat/completions', 'your-nvidia-api-key', 'meta/llama-4-scout', NULL, 1024, 0, 0, 4);
