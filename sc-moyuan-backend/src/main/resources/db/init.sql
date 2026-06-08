-- ============================================================
-- 版本：v3.0（全量合并版）
-- 日期：2026-06-01
-- 说明：全量合并所有表结构和初始数据，删除过渡迁移脚本
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
  `life_story` TEXT DEFAULT NULL COMMENT '人物生平',
  `influence` TEXT DEFAULT NULL COMMENT '主要影响',
  `evaluation` TEXT DEFAULT NULL COMMENT '历史评价',
  `anecdotes` TEXT DEFAULT NULL COMMENT '轶事典故',
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
  `is_original` TINYINT NOT NULL DEFAULT 0 COMMENT '是否原创：0-否，1-是',
  `audit_status` TINYINT NOT NULL DEFAULT 1 COMMENT '审核状态：0-待审核，1-已通过，2-已拒绝',
  `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `audit_reason` VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
  `avg_score` DECIMAL(3,1) DEFAULT NULL COMMENT '平均评分',
  `rating_count` INT NOT NULL DEFAULT 0 COMMENT '评分数量',
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
  `interests` VARCHAR(500) DEFAULT NULL COMMENT '兴趣选项（逗号分隔）',
  `role` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色：user-普通用户，admin-管理员',
  `poet_verified` TINYINT NOT NULL DEFAULT 0 COMMENT '诗人认证状态：0-未认证，1-已认证，2-认证中',
  `poet_profile_id` BIGINT DEFAULT NULL COMMENT '诗人资料ID',
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
-- 12. 精选诗人卡片表
-- ============================================================
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

-- ============================================================
-- 13. 首页导航数据表
-- ============================================================
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
-- 初始数据：诗词分类（优化后的分类体系）
-- ============================================================

-- 一级分类（parent_id = 0）
INSERT IGNORE INTO `category` (`id`, `name`, `parent_id`, `description`, `sort_order`) VALUES
(1, '中国古典诗词', 0, '包含古体、近体、词、曲等传统诗歌形式，跨越先秦至清代', 1),
(2, '中国现当代诗歌', 0, '1919年五四运动至今的中国诗歌作品，包含现代诗与当代诗', 2),
(3, '外国诗歌', 0, '世界各国优秀诗歌作品，涵盖欧美、东方、拉美等地区', 3),
(4, '主题专题诗词', 0, '按场景、主题分类的诗词，适合专题阅读与赏析', 4),
(5, '当代青年原创诗歌', 0, '青年创作的现代诗与旧体诗，包含校园诗歌、网络诗歌等', 5),
(6, '特殊体裁与韵文', 0, '戏曲唱词、辞赋、趣味诗词、童谣民谣等特殊文学形式', 6),
(73, '朝代', 0, '按历史朝代分类，涵盖先秦至清代各朝代的诗词作品', 7),
(74, '流派', 0, '按诗词流派分类，涵盖婉约、豪放、山水田园等经典流派', 8);

-- 二级分类：中国古典诗词（parent_id = 1）- 按体裁
INSERT IGNORE INTO `category` (`id`, `name`, `parent_id`, `description`, `sort_order`) VALUES
(7, '古体诗', 1, '包括四言、五言、七言等，形式自由，不受格律限制，如《古诗十九首》', 1),
(8, '近体诗', 1, '唐代形成的格律诗，包括律诗、绝句，讲究平仄对仗', 2),
(9, '词', 1, '又称长短句、诗余，配合音乐歌唱的诗歌形式，兴于唐盛于宋', 3),
(10, '曲', 1, '元代新兴的文学形式，包括散曲和剧曲，可配乐演唱', 4);

-- 二级分类：中国古典诗词（parent_id = 8）- 近体诗细分
INSERT IGNORE INTO `category` (`id`, `name`, `parent_id`, `description`, `sort_order`) VALUES
(11, '五言律诗', 8, '每句五字，共八句，讲究平仄对仗，如杜甫《春望》', 1),
(12, '七言律诗', 8, '每句七字，共八句，格律严谨，如李商隐《锦瑟》', 2),
(13, '五言绝句', 8, '每句五字，共四句，短小精悍，如王维《鹿柴》', 3),
(14, '七言绝句', 8, '每句七字，共四句，意境深远，如王之涣《登鹳雀楼》', 4);

-- 二级分类：中国古典诗词（parent_id = 1）- 按主题
INSERT IGNORE INTO `category` (`id`, `name`, `parent_id`, `description`, `sort_order`) VALUES
(15, '边塞诗', 1, '以边疆军旅生活为主题，表现边塞风光、戍边将士生活，代表诗人岑参、高适', 5),
(16, '山水田园诗', 1, '描写自然风光、农村景物，表达对自然的热爱，代表诗人王维、孟浩然', 6),
(17, '咏史诗', 1, '以历史为题材，借古讽今，抒发对历史的感慨，代表诗人李商隐、杜牧', 7),
(18, '怀古诗', 1, '借登高望远、咏叹史实来感慨兴衰、寄托哀思，代表诗人辛弃疾、苏轼', 8),
(19, '咏物诗', 1, '以具体事物为描写对象，托物言志，代表诗人王安石、陆游', 9),
(20, '送别诗', 1, '以送别为题材，表达离别之情，代表诗人王维、李白', 10),
(21, '闺怨诗', 1, '描写古代妇女生活和情感，表达闺中愁思，代表诗人王昌龄、温庭筠', 11),
(22, '悼亡诗', 1, '悼念亡故之人，表达哀思怀念，代表诗人元稹、苏轼', 12),
(23, '哲理诗', 1, '蕴含深刻哲理，表达人生感悟，代表诗人苏轼、朱熹', 13);

-- 二级分类：中国现当代诗歌（parent_id = 2）- 按流派
INSERT IGNORE INTO `category` (`id`, `name`, `parent_id`, `description`, `sort_order`) VALUES
(24, '新月派', 2, '主张新诗格律化，代表诗人徐志摩、闻一多、林徽因', 1),
(25, '朦胧诗派', 2, '意象朦胧含蓄，代表诗人北岛、顾城、舒婷', 2),
(26, '象征诗派', 2, '运用象征手法，代表诗人李金发、戴望舒', 3),
(27, '现实主义诗派', 2, '关注社会现实，代表诗人艾青、臧克家', 4),
(28, '人生派', 2, '关注人生问题，代表诗人冰心、宗白华', 5);

-- 二级分类：中国现当代诗歌（parent_id = 2）- 按主题
INSERT IGNORE INTO `category` (`id`, `name`, `parent_id`, `description`, `sort_order`) VALUES
(29, '人生感悟', 2, '表达对人生的思考与感悟', 6),
(30, '社会现实', 2, '关注社会问题，反映时代变迁', 7),
(31, '自然意象', 2, '以自然景物为意象，抒发情感', 8),
(32, '爱情诗歌', 2, '表达爱情主题的现当代诗歌', 9);

-- 二级分类：外国诗歌（parent_id = 3）- 按地域
INSERT IGNORE INTO `category` (`id`, `name`, `parent_id`, `description`, `sort_order`) VALUES
(33, '欧美诗歌', 3, '欧洲与北美诗歌，包括英美法德等国作品', 1),
(34, '东方诗歌', 3, '亚洲诗歌，包括日本俳句、印度诗歌、波斯诗歌等', 2),
(35, '拉美诗歌', 3, '拉丁美洲诗歌，包括聂鲁达、博尔赫斯等诗人作品', 3);

-- 二级分类：外国诗歌（parent_id = 3）- 按体裁/流派
INSERT IGNORE INTO `category` (`id`, `name`, `parent_id`, `description`, `sort_order`) VALUES
(36, '抒情诗', 3, '表达个人情感的诗歌形式', 4),
(37, '叙事诗', 3, '讲述故事的诗歌形式，如荷马史诗', 5),
(38, '十四行诗', 3, '欧洲格律诗，又称商籁体，代表诗人莎士比亚、彼特拉克', 6),
(39, '俳句/短歌', 3, '日本传统诗歌形式，代表诗人松尾芭蕉、与谢芜村', 7),
(40, '象征主义诗歌', 3, '19世纪末法国兴起的诗歌流派，代表诗人波德莱尔、兰波', 8),
(41, '浪漫主义诗歌', 3, '强调情感与想象，代表诗人拜伦、雪莱、普希金', 9);

-- 二级分类：主题专题诗词（parent_id = 4）
INSERT IGNORE INTO `category` (`id`, `name`, `parent_id`, `description`, `sort_order`) VALUES
(42, '节日节气诗词', 4, '与传统节日、二十四节气相关的诗词，如春节、中秋、清明', 1),
(43, '季节时令诗词', 4, '描写春夏秋冬四季景色与情感的诗词', 2),
(44, '山水风光诗词', 4, '描写名山大川、江河湖海等自然景观的诗词', 3),
(45, '家国情怀诗词', 4, '表达爱国情怀、忧国忧民的诗词，代表诗人陆游、辛弃疾', 4),
(46, '友情赠别诗词', 4, '表达友情、送别、赠友主题的诗词', 5);

-- 二级分类：当代青年原创诗歌（parent_id = 5）
INSERT IGNORE INTO `category` (`id`, `name`, `parent_id`, `description`, `sort_order`) VALUES
(47, '青年原创现代诗', 5, '青年人创作的现代自由诗', 1),
(48, '青年原创旧体诗', 5, '青年人用古体、词牌等传统形式创作的诗歌', 2),
(49, '校园诗歌', 5, '校园生活为主题的青年诗歌创作', 3),
(50, '网络诗歌', 5, '在网络平台发表的诗歌作品', 4),
(51, '小众风格原创', 5, '实验性、先锋性的青年诗歌创作', 5);

-- 二级分类：特殊体裁与韵文（parent_id = 6）
INSERT IGNORE INTO `category` (`id`, `name`, `parent_id`, `description`, `sort_order`) VALUES
(52, '戏曲唱词', 6, '戏曲中的唱词部分，如昆曲、京剧等剧种的经典唱段', 1),
(53, '辞赋', 6, '汉代兴起的文体，讲究铺陈排比，如汉赋、骈文', 2),
(54, '趣味诗词', 6, '回文诗、藏头诗、宝塔诗、神智体等趣味形式', 3),
(55, '童谣/儿歌', 6, '儿童传唱的歌谣，语言简练，节奏明快', 4),
(56, '民谣/民歌', 6, '民间传唱的歌谣，反映民间生活与情感', 5);

-- 二级分类：朝代（parent_id = 73）
INSERT IGNORE INTO `category` (`id`, `name`, `parent_id`, `description`, `sort_order`) VALUES
(57, '先秦', 73, '《诗经》《楚辞》及诸子散文中的诗歌，中国诗歌的源头', 1),
(58, '秦汉', 73, '汉乐府民歌与文人诗，如《孔雀东南飞》《古诗十九首》', 2),
(59, '魏晋南北朝', 73, '建安风骨、田园山水诗兴起，代表诗人曹操、陶渊明、谢灵运', 3),
(60, '唐代', 73, '中国古典诗歌的巅峰，代表诗人李白、杜甫、王维、白居易等', 4),
(61, '宋代', 73, '词的黄金时代，代表词人苏轼、李清照、辛弃疾、柳永等', 5),
(62, '元代', 73, '散曲与剧曲兴盛，代表曲家马致远、关汉卿、张养浩等', 6),
(63, '明代', 73, '诗文复古与戏曲繁荣，代表诗人高启、杨慎、于谦等', 7),
(64, '清代', 73, '诗词集大成，代表诗人纳兰性德、龚自珍、黄景仁等', 8);

-- 二级分类：流派（parent_id = 74）
INSERT IGNORE INTO `category` (`id`, `name`, `parent_id`, `description`, `sort_order`) VALUES
(65, '婉约派', 74, '词风婉转含蓄、情感细腻，代表词人柳永、李清照、晏殊、秦观', 1),
(66, '豪放派', 74, '词风豪迈奔放、意境开阔，代表词人苏轼、辛弃疾、岳飞', 2),
(67, '山水田园诗派', 74, '以自然山水和田园生活为题材，代表诗人王维、孟浩然、陶渊明', 3),
(68, '边塞诗派', 74, '以边疆军旅生活为题材，代表诗人高适、岑参、王昌龄、王之涣', 4),
(69, '新乐府运动', 74, '主张"文章合为时而著，歌诗合为事而作"，代表诗人白居易、元稹', 5),
(70, '江西诗派', 74, '宋代影响最大的诗歌流派，主张"无一字无来处"，代表诗人黄庭坚、陈师道', 6),
(71, '花间词派', 74, '晚唐五代词派，词风绮丽柔靡，代表词人温庭筠、韦庄', 7),
(72, '桐城派', 74, '清代散文流派，主张"义法"，代表人物方苞、姚鼐、刘大櫆', 8);

-- ============================================================
-- 初始数据：诗人
-- ============================================================
INSERT IGNORE INTO `poet` (`name`, `dynasty_id`, `biography`, `status`) VALUES
('常平逼王', 13, '现代诗人，作品多表现个人情感与生活感悟', 1),
('陆游', 8, '（1125年—1210年），字务观，号放翁，越州山阴（今浙江绍兴）人。南宋文学家、史学家、爱国诗人。一生笔耕不辍，诗词文俱有很高成就，其诗语言平易晓畅、章法整饬谨严，兼具李白的雄奇奔放与杜甫的沉郁悲凉，尤以饱含爱国热情对后世影响深远。', 1),
('唐婉', 8, '（约1128年—约1156年），字蕙仙，越州山阴（今浙江绍兴）人。南宋才女，陆游的前妻。自幼聪慧，才华横溢，擅长诗词。与陆游的爱情悲剧令人扼腕，其《钗头凤·世情薄》传诵千古。', 1),
('李煜', 7, '（937年—978年），初名从嘉，字重光，号钟隐、莲峰居士，彭城（今江苏徐州）人。南唐末代君主，世称南唐后主。精书法、工绘画、通音律，诗文均有一定造诣，尤以词的成就最高。其词作在晚唐五代词中别树一帜，对后世词坛影响深远。', 1),
('森垚', 13, '现代诗人，作品风格独特，善于运用古典意象表达现代情感。', 1);

-- ============================================================
-- 初始数据：诗词
-- ============================================================
INSERT IGNORE INTO `poem` (`title`, `content`, `poet_id`, `dynasty_id`, `category_id`, `source`, `status`, `is_featured`) VALUES
('愿如花已落千行', '愿如花已落千行，未闻花语浅别殇。\n幽僻心境漫过少，唯有诗语解锁缰。\n金甲未脱抬眼望，怒剑难收向疆场。\n笑祝功成人与事，再鼓一旬又何妨。', 1, 13, 47, '常平逼王', 1, 1),
('西江月.未语', '华灯初照白路，夜来树影凄楚，俳徊只与月相伴，茫然一片迷雾。\n离人兀自心扬，未语难避哀伤，只若通明过后，真心但化霓裳。', 1, 13, 9, '常平逼王', 1, 1),
('忆江南.哀', '秋风寒，\n落红自凋残。\n未识伊人愁几许，\n伊人兀自笑颜开。\n吾心不胜哀！', 1, 13, 9, '常平逼王', 1, 1),
('东风吹起白蝴蝶', '东风吹起白蝴蝶，散入云幕尽青烟。\n但觉离魂归来少，春日暖暖艳阳天。\n松柏长青久可立，香印再燃意更坚。\n思情尽付潇湘处，绵远流长年复年。', 1, 13, 47, '常平逼王', 1, 1),
('清平乐.梦怀', '雨落窗外，淅声渐消潺，一帘凝愁入梦来，若雾浸湿枕畔。\n经年离愁之初，再会此情更苦，最是万千惆怅，与白共登天目!', 1, 13, 9, '常平逼王', 1, 1),
('乌夜啼.叹城府', '淡雾兀遮明眸，更愁，独倚玉阑不觉泪空流。\n人情恶，谁易错，自长酌，一心明月向沟成污浊。', 1, 13, 9, '常平逼王', 1, 1),
('新桃映红把福迎', '新桃映红把福迎，银烛高照万家兴。\n火尾连排上清夜，笑语欢声动耳惊。\n奈何遣词功夫浅，此夜此景休论明。\n唯愿持酒歌一曲，与君同乐一身轻', 1, 13, 47, '常平逼王', 1, 1),
('回首一九已成空', '回首一九已成空，展望二零佳意浓。\n世事岂敢轻年少，天意哪可断前程。\n唐猊又着战意显，三尺重出徙侣从。\n烂柯沉木枉悲夫，夫且若怡也可容。', 1, 13, 47, '常平逼王', 1, 1),
('沉影迷叠千层障', '沉影迷叠千层障，乱云归处是它乡。\n酒酣仍识昔日客，心迷难辨眼前芳。\n纵把凡锦比仙缎，不需经年多思量。\n一笑即随羊角去，九风还作万华芳。', 1, 13, 47, '常平逼王', 1, 1),
('一剪梅.无题', '一别三秋未招摇，\n山也迢迢，水也昭昭。\n何人再添新衣袍，\n笑意盈绕，喜上眉梢。\n忆昔花开岁月好，\n蜂字飘摇，蝶字舞蹈。\n而今方知云未晓，\n风又飘飘，雨又萧萧。', 1, 13, 9, '常平逼王', 1, 1),
('清平乐.情(道姑)', '幽情迷朦，三年怀一梦，一遭尘世皆为恒，无怨无悔无憎。\n来日打马南屏，忆昔紫夜流萤，纵剑吴钩霜雪，锦夜孤灯长明。', 1, 13, 10, '初芒', 1, 1),
('钗头凤·世情薄', '世情薄，人情恶，雨送黄昏花易落。晓风干，泪痕残，欲笺心事，独语斜阑。难，难，难！\n人成各，今非昨，病魂常似秋千索。角声寒，夜阑珊，怕人寻问，咽泪装欢。瞒，瞒，瞒！', 3, 8, 9, '唐婉词', 1, 1),
('钗头凤·红酥手', '红酥手，黄縢酒，满城春色宫墙柳。东风恶，欢情薄。一怀愁绪，几年离索。错、错、错。\n春如旧，人空瘦，泪痕红浥鲛绡透。桃花落，闲池阁。山盟虽在，锦书难托。莫、莫、莫。', 2, 8, 9, '陆游词', 1, 1),
('长相思·一重山', '一重山，两重山，山远天高烟水寒，相思枫叶丹。\n菊花开，菊花残，塞雁高飞人未还，一帘风月闲。', 4, 7, 9, '李煜词', 1, 1),
('白夜月望', '经日积灰新作旧，魂安大腹杯弓痛。\n可怜嫦娥广寒丑，才识沧桑无激流。', 5, 13, 25, '森垚', 1, 1);

-- ============================================================
-- 更新诗词翻译和赏析
-- ============================================================
UPDATE `poem` SET
`translation` = '世事炎凉，人情险恶，黄昏时分风雨摧残花儿凋落。晨风吹干了泪水，泪痕犹存，想要写下心事，独自倚着栏杆自言自语。难啊，难啊，难！\n如今各自分离，今非昔比，多病的灵魂好似秋千般摇摆不定。号角声凄寒，夜色将尽，生怕被人寻问，只能咽下泪水强颜欢笑。瞒啊，瞒啊，瞒！',
`appreciation` = '这首词是唐婉在沈园与陆游重逢后所作。词的上片写自己在人世间的悲惨处境：世情薄、人情恶，黄昏雨落花凋零，晓风干泪痕残，满腹心事无人可诉。下片写与陆游分别后的痛苦：人成各、今非昨，病魂如秋千索，角声寒夜阑珊，怕人询问只能咽泪装欢。三个"难"字和三个"瞒"字，道尽了词人内心的痛苦与无奈。',
`background` = '唐婉与陆游本是恩爱夫妻，因陆母不满被迫离异。后陆游另娶，唐婉改嫁赵士程。多年后二人在沈园偶遇，陆游在墙上题写了《钗头凤·红酥手》，唐婉见后悲痛不已，和了这首《钗头凤·世情薄》。不久后唐婉便郁郁而终。'
WHERE `title` = '钗头凤·世情薄' AND `poet_id` = 3;

UPDATE `poem` SET
`translation` = '红润酥腻的手，捧着黄縢封的美酒，满城荡漾着春天的景色，宫墙边柳树摇曳。东风多么可恶，把欢情吹得那样稀薄。满怀的忧愁情绪，几年来的离别相索。错啊，错啊，错！\n春天依然如旧，人却白白地变得消瘦，泪水湿透了手帕。桃花凋落在寂静的池塘楼阁上。山盟海誓虽然还在，可是锦书再也难以寄托。莫啊，莫啊，莫！',
`appreciation` = '这首词是陆游在沈园偶遇前妻唐婉后题写在园壁上的。词的上片通过"红酥手""黄縢酒""宫墙柳"等意象，回忆了昔日的美好时光，而后笔锋一转，"东风恶"三字写出了被迫分离的无奈。下片写重逢时的情景，"春如旧，人空瘦"对比鲜明，"泪痕红浥鲛绡透"写尽了离别后的相思之苦。三个"错"字和三个"莫"字，饱含了词人对这段感情的无限感慨。',
`background` = '陆游与唐婉本是表兄妹，婚后感情甚笃。但陆母认为陆游沉溺于儿女情长，荒疏了功业，遂强迫二人离异。陆游另娶王氏，唐婉改嫁赵士程。公元1155年，陆游礼部会试失利后到沈园游玩，偶遇唐婉夫妇。唐婉征得赵士程同意后，派人给陆游送去了酒肴。陆游感慨万千，乘醉在园壁上题写了这首《钗头凤》。'
WHERE `title` = '钗头凤·红酥手' AND `poet_id` = 2;

UPDATE `poem` SET
`translation` = '一重又一重的山峦，山峦连绵不断，山远天高，烟雾笼罩着寒水，相思之情如同火红的枫叶。\n菊花盛开，菊花凋零，塞外的大雁高高飞过，远行的人却还没有回来，只有帘外的风月依旧悠闲。',
`appreciation` = '这首词以"一重山，两重山"起笔，用重叠的山峦象征相思的绵延不断。"山远天高烟水寒"描绘了一幅辽远凄清的画面，烘托出相思之人的孤寂心境。"相思枫叶丹"以枫叶的红艳比喻相思的炽热。下片"菊花开，菊花残"暗示时光流逝，相思日久。"塞雁高飞人未还"写远人不归的惆怅，结句"一帘风月闲"以景结情，将无尽的思念融入清风明月之中，余韵悠长。',
`background` = '这首词是李煜前期的作品，描写了闺中思妇对远行之人的深切思念。词中以重山、烟水、枫叶、菊花、塞雁、风月等意象，构建了一幅深秋思妇图，表达了对远方亲人的无限牵挂。'
WHERE `title` = '长相思·一重山' AND `poet_id` = 4;

UPDATE `poem` SET
`translation` = '日子一天天过去，新事物也会变成旧事物，灵魂安放在大腹便便的身躯里，如同杯弓蛇影般痛苦。可怜那嫦娥在广寒宫中孤独寂寞，才明白沧桑变幻中并无激流涌动。',
`appreciation` = '这首诗以独特的视角审视人生与时间的流逝。"经日积灰新作旧"表达了时光荏苒、万物更迭的感慨；"魂安大腹杯弓痛"用"杯弓蛇影"的典故暗喻内心的疑虑与痛苦；"可怜嫦娥广寒丑"反用嫦娥奔月的神话，写出孤独者的凄凉；"才识沧桑无激流"则在历经沧桑后归于平静，表达了对人生无常的深刻体悟。',
`background` = '现代诗人森垚的作品，以古典意象承载现代人的精神困境，在传统与现代之间寻找诗意的平衡点。'
WHERE `title` = '白夜月望' AND `poet_id` = 5;

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

-- 初始数据：AI模型配置
-- ============================================================
-- 独立提供商（直接调用官方API）
-- ============================================================
INSERT IGNORE INTO `ai_model` (`name`, `display_name`, `provider`, `model_type`, `api_url`, `api_key`, `model_id`, `vision_model_id`, `max_tokens`, `is_enabled`, `is_default`, `sort_order`) VALUES
-- 智谱AI
('zhipu', '智谱AI', 'zhipu', 'both', 'https://open.bigmodel.cn/api/paas/v4/chat/completions', 'your-zhipu-api-key', 'glm-4', 'glm-4.6v-flash', 1024, 0, 0, 1),
('zhipu-flash', '智谱GLM-4.7-Flash', 'zhipu', 'text', 'https://open.bigmodel.cn/api/paas/v4/chat/completions', 'your-zhipu-api-key', 'glm-4.7-flash', NULL, 1024, 0, 0, 2),
-- DeepSeek
('deepseek', 'DeepSeek', 'deepseek', 'text', 'https://api.deepseek.com/v1/chat/completions', 'your-deepseek-api-key', 'deepseek-chat', NULL, 1024, 0, 0, 3),
('deepseek-v3', 'DeepSeek-V3', 'deepseek', 'text', 'https://api.deepseek.com/v1/chat/completions', 'your-deepseek-api-key', 'deepseek-v3', NULL, 1024, 0, 0, 4),
-- Kimi
('kimi', 'Kimi', 'kimi', 'text', 'https://api.moonshot.cn/v1/chat/completions', 'your-kimi-api-key', 'moonshot-v1-8k', NULL, 1024, 0, 0, 5),
('kimi-k26', 'Kimi-K2.6', 'kimi', 'text', 'https://api.moonshot.cn/v1/chat/completions', 'your-kimi-api-key', 'kimi-k2.6', NULL, 1024, 0, 0, 6),
-- 千问
('qwen36plus', 'Qwen3.6-Plus', 'qwen', 'text', 'https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions', 'your-qwen-api-key', 'qwen3.6-plus', NULL, 1024, 0, 0, 7),
-- 小米MiMo
('mimo', '小米MiMo', 'mimo', 'text', 'https://api.xiaomimimo.com/v1/chat/completions', 'your-mimo-api-key', 'mimo-v2.5', 'mimo-v2.5-pro', 1024, 0, 0, 8),

-- ============================================================
-- NVIDIA NIM 平台（统一API Key）
-- ============================================================
('nvidia-llama4-scout', 'Llama 4 Scout', 'nvidia', 'text', 'https://integrate.api.nvidia.com/v1/chat/completions', 'your-nvidia-api-key', 'meta/llama-4-scout', NULL, 1024, 0, 0, 9),
('nvidia-llama4-maverick', 'Llama 4 Maverick', 'nvidia', 'text', 'https://integrate.api.nvidia.com/v1/chat/completions', 'your-nvidia-api-key', 'meta/llama-4-maverick', NULL, 1024, 0, 0, 10),
('nvidia-glm5', 'GLM-5', 'nvidia', 'text', 'https://integrate.api.nvidia.com/v1/chat/completions', 'your-nvidia-api-key', 'zai-org/glm-5', NULL, 1024, 0, 0, 11),
('nvidia-glm51', 'GLM-5.1', 'nvidia', 'text', 'https://integrate.api.nvidia.com/v1/chat/completions', 'your-nvidia-api-key', 'zai-org/glm-51', NULL, 1024, 0, 0, 12),
('nvidia-deepseek-v4-flash', 'DeepSeek-V4 Flash', 'nvidia', 'text', 'https://integrate.api.nvidia.com/v1/chat/completions', 'your-nvidia-api-key', 'deepseek-ai/deepseek-v4-flash', NULL, 1024, 0, 0, 13),
('nvidia-deepseek-v4-pro', 'DeepSeek-V4 Pro', 'nvidia', 'text', 'https://integrate.api.nvidia.com/v1/chat/completions', 'your-nvidia-api-key', 'deepseek-ai/deepseek-v4-pro', NULL, 1024, 0, 0, 14),
('nvidia-kimi-k25', 'Kimi-K2.5', 'nvidia', 'text', 'https://integrate.api.nvidia.com/v1/chat/completions', 'your-nvidia-api-key', 'moonshotai/kimi-k2.5', NULL, 1024, 0, 0, 15),
('nvidia-kimi-k26', 'Kimi-K2.6', 'nvidia', 'text', 'https://integrate.api.nvidia.com/v1/chat/completions', 'your-nvidia-api-key', 'moonshotai/kimi-k2.6', NULL, 1024, 0, 0, 16),
('nvidia-minimax-m25', 'MiniMax-M2.5', 'nvidia', 'text', 'https://integrate.api.nvidia.com/v1/chat/completions', 'your-nvidia-api-key', 'minimax-ai/minimax-m25', NULL, 1024, 0, 0, 17),
('nvidia-qwen35', 'Qwen3.5', 'nvidia', 'text', 'https://integrate.api.nvidia.com/v1/chat/completions', 'your-nvidia-api-key', 'qwen/qwen3.5-397b-a17b', NULL, 1024, 0, 0, 18),
('nvidia-qwen3-next', 'Qwen3 Next', 'nvidia', 'text', 'https://integrate.api.nvidia.com/v1/chat/completions', 'your-nvidia-api-key', 'qwen/qwen3-next-80b-a3b-instruct', NULL, 1024, 0, 0, 19),
('nvidia-qwen3-coder', 'Qwen3 Coder', 'nvidia', 'text', 'https://integrate.api.nvidia.com/v1/chat/completions', 'your-nvidia-api-key', 'qwen/qwen3-coder-next', NULL, 1024, 0, 0, 20),

-- ============================================================
-- 免费模型（OpenRouter）
-- ============================================================
('free-deepseek-v4', 'DeepSeek-V4 (免费)', 'openrouter', 'text', 'https://openrouter.ai/api/v1/chat/completions', 'your-openrouter-key', 'deepseek/deepseek-v4-flash:free', NULL, 1024, 0, 0, 21),
('free-kimi-k26', 'Kimi-K2.6 (免费)', 'openrouter', 'text', 'https://openrouter.ai/api/v1/chat/completions', 'your-openrouter-key', 'moonshotai/kimi-k2.6:free', NULL, 1024, 0, 0, 22),
('free-gemma4', 'Gemma-4 (免费)', 'openrouter', 'text', 'https://openrouter.ai/api/v1/chat/completions', 'your-openrouter-key', 'google/gemma-4-26b-a4b-it:free', NULL, 1024, 0, 0, 23),
('free-qwen3-next', 'Qwen3 Next (免费)', 'openrouter', 'text', 'https://openrouter.ai/api/v1/chat/completions', 'your-openrouter-key', 'qwen/qwen3-next-80b-a3b-instruct:free', NULL, 1024, 0, 0, 24),
('free-qwen3-coder', 'Qwen3 Coder (免费)', 'openrouter', 'text', 'https://openrouter.ai/api/v1/chat/completions', 'your-openrouter-key', 'qwen/qwen3-coder:free', NULL, 1024, 0, 0, 25),
('free-minimax-m25', 'MiniMax-M2.5 (免费)', 'openrouter', 'text', 'https://openrouter.ai/api/v1/chat/completions', 'your-openrouter-key', 'minimax/minimax-m2.5:free', NULL, 1024, 0, 0, 26),
('free-llama4-scout', 'Llama 4 Scout (免费)', 'openrouter', 'text', 'https://openrouter.ai/api/v1/chat/completions', 'your-openrouter-key', 'meta-llama/llama-4-scout:free', NULL, 1024, 0, 0, 27);

-- ============================================================
-- AI模块模型配置表
-- ============================================================
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

-- 初始数据：AI模块配置
INSERT IGNORE INTO `ai_module_config` (`module_code`, `module_name`, `require_vision`, `description`, `prompt_template`, `max_response_length`, `response_style`, `first_response_length`, `enable_markdown`) VALUES
('chat', 'AI诗词问答', 0, '资深诗词文化顾问，精通先秦至近现代诗词典籍，擅长诗词鉴赏、创作指导、诗人生平解读、格律讲解及诗词推荐', '你是一个古典诗词文化助手。回答要求：1.语言简洁精炼，避免冗余；2.使用通俗易懂的中文；3.重点突出，条理清晰；4.不要使用markdown格式，直接输出纯文本；5.根据问题复杂度适当展开，但不超过{maxLength}字', 200, 'concise', 100, 0),
('poet_chat', '诗人对话', 0, '诗人介绍助手，专门回答关于诗人的问题', '你是一个古典诗词文化助手，正在为用户介绍诗人{poetName}。回答要求：1.语言简洁精炼，避免冗余；2.使用通俗易懂的中文；3.重点突出，条理清晰；4.不要使用markdown格式，直接输出纯文本；{styleHint}', 150, 'concise', 80, 0),
('write_poem', '看图写诗', 1, '古典诗词创作大师，擅长捕捉画面意境并转化为诗词意象，精通五言七言律诗绝句及各词牌创作', '你是一位古典诗词创作大师，擅长捕捉画面意境并转化为诗词意象。请根据图片内容创作一首诗词，讲究格律严谨、意象优美、情景交融。', 300, 'balanced', 300, 1),
('analyze', '诗词鉴赏分析', 0, '诗词学术研究专家，从意境营造、修辞手法、情感表达、历史背景、艺术特色等多维度深入解读诗词', '你是诗词学术研究专家，请从意境营造、修辞手法、情感表达、历史背景、艺术特色等多维度深入解读诗词，分析鞭辟入里、引经据典，兼顾学术严谨与通俗易懂。', 500, 'detailed', 500, 1),
('couplet', 'AI对对联', 0, '对联艺术大师，深谙平仄声律、词性对仗、意境相承之道', '你是对联艺术大师，深谙平仄声律、词性对仗、意境相承之道。请根据上联创作工整合律的下联，对仗精巧、意境深远。', 100, 'concise', 100, 0);

-- ============================================================
-- 初始数据：测试用户
-- ============================================================
INSERT IGNORE INTO `user` (`username`, `password`, `email`, `nickname`, `role`, `status`) VALUES
('admin', '$2b$12$kMElb/13aTqGl9pfrD4CqOfA8rVJ3ahUcRBPfTHihcQ4D6.5gKWB6', 'admin@moyuan.com', '系统管理员', 'admin', 1),
('test', '$2b$10$7WF/nU54ij0NgOYQzcbvWOIUsfCBQ28BeEq9rc.buaUcl0zaR.9PO', 'test@moyuan.com', '测试用户', 'user', 1),
('text', '$2b$10$7WF/nU54ij0NgOYQzcbvWOIUsfCBQ28BeEq9rc.buaUcl0zaR.9PO', 'text@moyuan.com', '文本用户', 'user', 1);

-- ============================================================
-- 初始数据：论坛示例帖子
-- ============================================================
INSERT IGNORE INTO `forum_post` (`title`, `content`, `user_id`, `view_count`, `like_count`, `comment_count`, `is_top`, `status`) VALUES
('欢迎来到诗汇论坛', '这里是古今诗话的交流园地，无论你是诗词爱好者还是初学者，都欢迎在这里分享你的作品、讨论诗词鉴赏心得、交流创作灵感。\n\n让我们一起在诗词的韵律中感受中华文化的博大精深！', 1, 256, 42, 8, 1, 1),
('浅谈李白诗歌中的浪漫主义精神', '李白的诗歌以其豪放飘逸、想象丰富而著称。"飞流直下三千尺，疑是银河落九天"，这样的诗句展现了诗人超凡的想象力和对自然的热爱。\n\n李白的浪漫主义不仅体现在对自然的描绘上，更体现在他对自由的追求和对权贵的蔑视。"安能摧眉折腰事权贵，使我不得开心颜"，这种精神至今仍然激励着我们。\n\n大家最喜欢李白的哪首诗呢？欢迎分享你的看法。', 2, 189, 35, 12, 0, 1),
('李清照词中的女性意识', '李清照作为宋代婉约派的代表词人，她的作品中展现出了独特的女性视角和细腻的情感表达。\n\n"寻寻觅觅，冷冷清清，凄凄惨惨戚戚"，这十四个叠字将孤独与愁苦表达得淋漓尽致。而"知否，知否，应是绿肥红瘦"则展现了她对生活细节的敏锐观察。\n\n李清照的词不仅是文学的瑰宝，更是研究宋代女性生活和情感世界的重要资料。', 3, 156, 28, 6, 0, 1),
('如何欣赏近体诗的格律之美', '近体诗包括律诗和绝句，讲究平仄、对仗和押韵。对于初学者来说，理解格律是欣赏近体诗的第一步。\n\n以杜甫的《登高》为例："风急天高猿啸哀，渚清沙白鸟飞回。无边落木萧萧下，不尽长江滚滚来。"这首诗不仅意境深远，而且格律严谨，被誉为"古今七律第一"。\n\n建议大家多读多背，慢慢体会格律之美。', 2, 134, 22, 9, 0, 1),
('分享一首我最近写的诗', '秋夜独坐\n\n窗外秋风起，\n庭前落叶黄。\n孤灯照书案，\n思绪入苍茫。\n\n这是前几天秋夜独坐时的即兴之作，虽然笔法稚嫩，但却是真情实感的流露。欢迎各位诗友指正！', 3, 98, 15, 4, 0, 1),
('苏轼的人生智慧与诗词境界', '苏轼一生坎坷，却始终保持着豁达乐观的人生态度。"竹杖芒鞋轻胜马，谁怕？一蓑烟雨任平生"，这种超然物外的境界令人敬佩。\n\n他的诗词不仅有豪放派的大气磅礴，也有细腻入微的生活情趣。"日啖荔枝三百颗，不辞长作岭南人"，即使被贬谪，也能发现生活中的美好。\n\n苏轼的人生态度值得我们学习：在逆境中保持乐观，在顺境中不忘初心。', 1, 210, 38, 15, 0, 1),
('现代诗歌与古典诗词的对话', '在当今时代，现代诗歌和古典诗词各有其独特的魅力。古典诗词讲究格律和意境，现代诗歌则更注重自由表达和个性张扬。\n\n但两者并非对立，而是可以相互借鉴、相互融合。许多现代诗人从古典诗词中汲取营养，创作出既有古典韵味又有现代气息的优秀作品。\n\n大家如何看待现代诗歌与古典诗词的关系？', 2, 87, 12, 3, 0, 1);

-- ============================================================
-- 初始数据：精选诗人
-- ============================================================
INSERT IGNORE INTO `poet_featured` (`poet_id`, `name`, `dynasty`, `description`, `biography`, `image_url`, `sort_order`) VALUES
(1, '李清照', '宋代', '（1084年—1155年），号易安居士，齐州章丘人。宋代婉约派代表词人。', '李清照善于以简洁自然的语言表达真挚的情感，使词作通俗易懂却又意味深长。她的词作情感表达细腻入微，无论是少女的娇羞、对爱情的渴望，还是后期的国仇家恨、孤独愁苦，都能让读者产生强烈的共鸣。', '/img/cd_suolue (10).jpg', 1),
(2, '杜牧', '唐代', '（803年—852年），字牧之，京兆万年（今陕西省西安市）人。唐朝文学家、诗人。', '杜牧为人性情刚直，不拘小节，不屑逢迎。自负经略之才，诗文均有盛名。文以《阿房宫赋》为最著，诗作明丽隽永，绝句诗尤受人称赞，世称小杜。与李商隐齐名，合称"小李杜"。', '/img/cd_suolue (11).jpg', 2),
(3, '苏轼', '宋代', '（1037年—1101年），字子瞻，号东坡居士，世称苏东坡、苏仙、坡仙。', '苏轼的思想融合了儒、释、道三家。儒家思想使他积极入世，关心民生疾苦；道家思想让他在面对困境时能保持内心的超脱和豁达；佛家思想则使他在历经磨难后，获得心灵的慰藉和宁静。', '/img/cd_suolue (12).jpg', 3),
(4, '上官婉儿', '唐代', '（664年—710年）是唐代一位极具影响力的女官和诗人。与蔡文姬、李清照、卓文君并称中国古代四大才女。', '她代朝廷品评天下诗文，大力提倡辞藻华丽、用律严谨的文风，使得当时宫廷内外都流行这种诗歌风格，对唐朝文坛的繁荣和诗歌艺术水平的提高作出了重要贡献。', '/img/cd_suolue (9).jpg', 4),
(5, '辛弃疾', '宋代', '（1140年—1207年），字幼安，号稼轩，历城（今山东济南）人。南宋豪放派词人。', '辛弃疾一生以恢复为志，以功业自许，却命运多舛、壮志难酬。但他始终没有动摇恢复中原的信念，而是把满腔激情和对国家兴亡、民族命运的关切、忧虑，全部寄寓于词作之中。', '/img/cd_suolue (7).jpg', 5),
(6, '李白', '唐代', '（701年—762年），字太白，号青莲居士，唐代伟大的浪漫主义诗人。', '李白的诗雄奇飘逸，艺术成就极高。他讴歌祖国山河与美丽的自然风光，风格雄奇奔放，俊逸清新，富有浪漫主义精神，达到了内容与艺术的完美统一。', '/img/cd_suolue (8).jpg', 6);

-- ============================================================
-- 初始数据：首页导航-作品
-- ============================================================
INSERT IGNORE INTO `home_navigation` (`type`, `title`, `subtitle`, `image_url`, `link_id`, `link_type`, `sort_order`) VALUES
('works', '黄鹤楼', '唐代:崔颢', 'h6.jpg', 42, 'poem', 1),
('works', '春宫怨', '唐代:杜荀鹤', 'h6.2.jpg', 51, 'poem', 2),
('works', '秋日赴阙题潼关驿楼', '唐代:杜荀鹤', 'h6.3.jpg', 62, 'poem', 3),
('works', '次北固山下', '唐代:许浑', 'h6.4.jpg', 73, 'poem', 4),
('works', '静夜思', '唐代:李白', 'h6.jpg', 84, 'poem', 5),
('works', '登鹳雀楼', '唐代:王之涣', 'h6.2.jpg', 95, 'poem', 6),
('works', '春晓', '唐代:孟浩然', 'h6.3.jpg', 106, 'poem', 7),
('works', '悯农', '唐代:李绅', 'h6.4.jpg', 117, 'poem', 8);

-- ============================================================
-- 初始数据：首页导航-流派
-- ============================================================
INSERT IGNORE INTO `home_navigation` (`type`, `title`, `subtitle`, `image_url`, `link_id`, `link_type`, `sort_order`) VALUES
('genres', '边塞·豪放', NULL, 'h6_liupai_4.png', 1, 'category', 1),
('genres', '唐宋八大家', NULL, 'h6_liupai_1.jpg', 2, 'category', 2),
('genres', '竹林七贤', NULL, 'h6_liupai_3.png', 3, 'category', 3),
('genres', '元曲四大家', NULL, 'h6_liupai_2.jpg', 4, 'category', 4),
('genres', '婉约派', NULL, 'h6_liupai_5.jpg', 5, 'category', 5),
('genres', '豪放派', NULL, 'h6_liupai_6.jpg', 6, 'category', 6),
('genres', '田园诗派', NULL, 'h6_liupai_7.jpg', 7, 'category', 7),
('genres', '山水诗派', NULL, 'h6_liupai_8.jpg', 8, 'category', 8);

-- ============================================================
-- 初始数据：首页导航-朝代
-- ============================================================
INSERT IGNORE INTO `home_navigation` (`type`, `title`, `subtitle`, `image_url`, `link_id`, `link_type`, `sort_order`) VALUES
('dynasties', '先秦', '蒹葭苍苍，白露为霜。', 'h6_chaodai_1.jpg', 1, 'dynasty', 1),
('dynasties', '两汉', '青青园中葵，朝露待日晞。', 'h6_chaodai_1.jpg', 2, 'dynasty', 2),
('dynasties', '唐朝', '秋风清，秋月明。', 'h6_chaodai_2.jpg', 3, 'dynasty', 3),
('dynasties', '宋朝', '十年生死两茫茫，不思量，自难忘。', 'h6_chaodai_3.jpg', 4, 'dynasty', 4),
('dynasties', '元朝', '枯藤老树昏鸦，小桥流水人家。', 'cd_suolue (4).jpg', 5, 'dynasty', 5),
('dynasties', '南北朝', '滚滚长江东逝水，浪花淘尽英雄。', 'h6_chaodai_2.jpg', 6, 'dynasty', 6),
('dynasties', '金朝', '人生若只如初见，何事秋风悲画扇。', 'h6_chaodai_3.jpg', 7, 'dynasty', 7),
('dynasties', '明清', '春花秋月何时了？往事知多少。', 'cd_suolue (4).jpg', 8, 'dynasty', 8);

-- ============================================================
-- 诗话视野文章表
-- ============================================================
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

INSERT IGNORE INTO `vision_article` (`title`, `summary`, `content`, `author`, `category`, `view_count`, `like_count`, `is_featured`, `sort_order`) VALUES
('穿越千年的诗意对话：唐诗宋词中的文化传承与创新', '探索唐诗宋词如何在当代社会中继续发挥文化影响力，从古典到现代的文化传承脉络。', '唐诗宋词是中国文学史上最璀璨的两颗明珠。从李白的豪放飘逸到杜甫的沉郁顿挫，从苏轼的豪迈旷达到李清照的婉约细腻，这些诗词不仅是文学的瑰宝，更是中华民族精神文化的根基。\n\n在当代社会，唐诗宋词依然以各种形式影响着我们的生活。从影视作品中的诗词引用，到日常交流中的意境表达，古典诗词的文化基因已经深深融入了中华文明的血脉之中。\n\n本文将从文化传承、创新发展、教育应用三个维度，探讨唐诗宋词在当代社会中的生命力和影响力。', '张文远', '文化传承', 328, 45, 1, 1),
('意象之美：如何通过自然景观来抒发人文情怀', '解读中国古典诗词中自然意象与人文情感的完美交融。', '中国古典诗词讲究"借景抒情"、"托物言志"。从"大漠孤烟直，长河落日圆"的壮阔，到"小荷才露尖尖角，早有蜻蜓立上头"的清新，诗人通过对自然景观的描绘，寄托了深厚的人文情怀。\n\n月亮寄托思乡之情，梅花象征高洁品格，松竹代表坚贞不屈——这些意象已经超越了自然本身，成为中华文化特有的精神符号。\n\n本文将带领读者深入品味这些经典意象背后的文化密码。', '李墨涵', '诗词赏析', 256, 38, 1, 2),
('从"床前明月光"到"明月几时有"：月亮在古诗词中的意象变迁', '梳理古诗词中"月亮"意象从先秦到宋代的演变轨迹与文化意蕴。', '月亮是中国古诗词中出现频率最高的意象之一。从《诗经》中的"月出皎兮"到李白的"床前明月光"，再到苏轼的"明月几时有"，月亮承载了不同时代诗人的情感与思考。\n\n在先秦时期，月亮是自然崇拜的对象；在魏晋南北朝，月亮成为文人寄托忧思的媒介；到唐代，月亮意象达到巅峰，李白、杜甫、王维等诗人赋予月亮丰富多彩的文化内涵；宋代的苏轼更是将月亮意象推向了哲理的高度。\n\n本文将以时间为线索，梳理月亮意象在中国古诗词中的千年变迁。', '王诗词', '诗词赏析', 412, 62, 1, 3),
('诗词中的哲学思考：从杜甫到苏轼的忧国忧民情怀', '探讨杜甫和苏轼诗词中蕴含的深刻哲学思想与人文关怀。', '杜甫被称为"诗圣"，苏轼被誉为"千古第一文人"。他们的诗词不仅在艺术上达到了极高的成就，更蕴含着深刻的哲学思考和忧国忧民的情怀。\n\n杜甫的"安得广厦千万间，大庇天下寒士俱欢颜"体现了儒家的仁爱精神；苏轼的"回首向来萧瑟处，归去，也无风雨也无晴"则展现了道家的超脱智慧。\n\n本文将从哲学角度重新审视两位伟大诗人的作品，探讨其中蕴含的人生智慧和社会关怀。', '陈思齐', '文化传承', 189, 31, 0, 4),
('诗词创作工作坊：提升你的押韵与对仗技巧', '面向诗词爱好者的实用创作指南，详解格律、押韵与对仗的核心技巧。', '中国古典诗词的格律之美，体现在严谨的押韵和精妙的对仗之中。掌握这些技巧，不仅能帮助我们更好地欣赏古典诗词，也能激发创作灵感。\n\n押韵是诗词的音乐性基础。古体诗的押韵相对自由，近体诗则要求严格的韵脚规范。对仗则要求上下句字数相等、词性相对、意义相关。\n\n本文将通过大量实例，循序渐进地讲解押韵和对仗的基本规则与进阶技巧，帮助诗词爱好者迈出创作的第一步。', '赵雅文', '创作技巧', 156, 22, 0, 5),
('诗词与书法：传统艺术的双重魅力', '品鉴诗词文学与书法艺术的完美融合，感受中国传统艺术的独特韵味。', '诗词与书法，是中国传统文化中最具代表性的两门艺术。当诗词遇上书法，文字的意境之美与线条的艺术之美交相辉映，产生了独特的审美体验。\n\n从王羲之书写《兰亭集序》到颜真卿的《祭侄文稿》，从苏轼的《寒食帖》到黄庭坚的《松风阁诗帖》，历代书法家以诗词为载体，创造了无数传世名作。\n\n本文将带领读者走进诗词与书法交融的艺术世界，感受传统艺术的双重魅力。', '林书雅', '艺术鉴赏', 203, 29, 0, 6),
('诗词教育在当代：如何在学校和社会中推广古诗词', '探讨古诗词教育的现状、挑战与创新推广方式。', '近年来，随着《中国诗词大会》等节目的热播，古诗词再次成为社会关注的焦点。如何在当代教育体系中更好地推广古诗词，成为教育工作者面临的重要课题。\n\n当前诗词教育面临的主要挑战包括：教学方式单一、学生兴趣不足、与现实生活脱节等。创新的教学方法，如情境教学、跨学科融合、数字化工具应用等，正在为诗词教育注入新的活力。\n\n本文将分析诗词教育的现状与挑战，并提出切实可行的推广策略。', '刘育华', '教育推广', 134, 18, 0, 7),
('国际视野下的中国诗词：翻译与传播的挑战', '分析中国古典诗词在国际传播中面临的翻译难题与文化隔阂。', '中国古典诗词以其独特的语言艺术和深厚的文化内涵，吸引了越来越多的海外读者。然而，诗词的国际传播面临着语言障碍和文化差异的双重挑战。\n\n诗歌翻译被称为"翻译中的翻译"，因为诗词不仅要传达字面意义，还要保留韵律、意境和文化内涵。从翟理斯到宇文所安，一代又一代的汉学家在诗词翻译领域做出了卓越的贡献。\n\n本文将探讨中国诗词国际传播的历程、翻译策略以及未来发展方向。', '周翰林', '文化传播', 178, 25, 0, 8),
('诗词与现代生活：寻找古典与现代的平衡点', '探索古典诗词在现代生活中的应用场景与精神价值。', '在快节奏的现代生活中，古典诗词似乎离我们越来越远。但实际上，诗词的智慧和美感从未远离我们的生活。\n\n从旅行途中的即兴吟咏，到朋友圈的文案引用；从婚礼上的诗词祝福，到节日里的传统吟诵——古典诗词以其独特的方式融入了现代人的生活场景。\n\n更重要的是，诗词中蕴含的人生智慧和审美情趣，能帮助我们在喧嚣的现代生活中找到内心的宁静。', '孙明远', '文化传承', 221, 34, 0, 9),
('诗词论坛互动专区：分享你的原创诗词，赢取点评与建议', '欢迎来到诗词爱好者的创作交流平台，展示才华、互相切磋。', '诗词论坛是古今诗话平台为诗词爱好者打造的互动交流空间。在这里，你可以发布自己的原创诗词作品，也可以对他人的作品进行点评和讨论。\n\n无论你是初学者还是资深爱好者，这里都是展示才华、交流心得的理想平台。我们的社区鼓励建设性的讨论和真诚的交流，让每一位诗词爱好者都能在这里找到归属感。\n\n快来加入我们，与志同道合的诗友一起分享创作的快乐吧！', '古今诗话编辑部', '社区互动', 145, 20, 0, 10);

-- ============================================================
-- 访问日志表
-- ============================================================
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

-- ============================================================
-- 文件元数据表
-- ============================================================
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

-- ============================================================
-- AI生成图片记录表
-- ============================================================
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

-- ============================================================
-- 19. 认证诗人资料表
-- ============================================================
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

-- ============================================================
-- 20. 诗词评分表
-- ============================================================
CREATE TABLE IF NOT EXISTS `poem_rating` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `poem_id` BIGINT NOT NULL COMMENT '诗词ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '评分用户ID（AI评分时为NULL）',
  `score` DECIMAL(3,1) NOT NULL COMMENT '评分（1.0-5.0）',
  `rating_type` TINYINT NOT NULL DEFAULT 1 COMMENT '评分类型：1-用户评分，2-AI评分',
  `dimension` VARCHAR(50) DEFAULT NULL COMMENT '评分维度（格律,意境,用词,情感,创新）',
  `comment` TEXT DEFAULT NULL COMMENT '评分说明',
  `ai_model` VARCHAR(50) DEFAULT NULL COMMENT 'AI模型名称（AI评分时使用）',
  `ai_analysis` TEXT DEFAULT NULL COMMENT 'AI分析内容',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_poem_rating_poem_id` (`poem_id`),
  KEY `idx_poem_rating_user_id` (`user_id`),
  KEY `idx_poem_rating_type` (`rating_type`),
  UNIQUE KEY `uk_poem_rating_user_poem` (`poem_id`, `user_id`, `rating_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='诗词评分表';

-- ============================================================
-- 21. 韵脚表（平水韵）
-- ============================================================
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

-- 平水韵种子数据 - 上平声（15韵）
INSERT IGNORE INTO `rhyme` (`character`, `rhyme_group`, `tone_type`, `rhyme_category`, `sort_order`) VALUES
-- 一东
('东', '东', '平声', '上平', 1), ('同', '东', '平声', '上平', 2), ('风', '东', '平声', '上平', 3),
('中', '东', '平声', '上平', 4), ('空', '东', '平声', '上平', 5), ('功', '东', '平声', '上平', 6),
('红', '东', '平声', '上平', 7), ('通', '东', '平声', '上平', 8), ('穷', '东', '平声', '上平', 9),
('翁', '东', '平声', '上平', 10), ('宫', '东', '平声', '上平', 11), ('弓', '东', '平声', '上平', 12),
('雄', '东', '平声', '上平', 13), ('融', '东', '平声', '上平', 14), ('丛', '东', '平声', '上平', 15),
('虹', '东', '平声', '上平', 16), ('聪', '东', '平声', '上平', 17), ('笼', '东', '平声', '上平', 18),
('蓬', '东', '平声', '上平', 19), ('桐', '东', '平声', '上平', 20),
-- 二冬
('冬', '冬', '平声', '上平', 1), ('农', '冬', '平声', '上平', 2), ('宗', '冬', '平声', '上平', 3),
('钟', '冬', '平声', '上平', 4), ('龙', '冬', '平声', '上平', 5), ('松', '冬', '平声', '上平', 6),
('峰', '冬', '平声', '上平', 7), ('浓', '冬', '平声', '上平', 8), ('容', '冬', '平声', '上平', 9),
('从', '冬', '平声', '上平', 10), ('踪', '冬', '平声', '上平', 11), ('逢', '冬', '平声', '上平', 12),
('胸', '冬', '平声', '上平', 13), ('庸', '冬', '平声', '上平', 14), ('慵', '冬', '平声', '上平', 15),
-- 三江
('江', '江', '平声', '上平', 1), ('窗', '江', '平声', '上平', 2), ('邦', '江', '平声', '上平', 3),
('降', '江', '平声', '上平', 4), ('双', '江', '平声', '上平', 5), ('庞', '江', '平声', '上平', 6),
('腔', '江', '平声', '上平', 7), ('桩', '江', '平声', '上平', 8),
-- 四支
('支', '支', '平声', '上平', 1), ('时', '支', '平声', '上平', 2), ('诗', '支', '平声', '上平', 3),
('知', '支', '平声', '上平', 4), ('之', '支', '平声', '上平', 5), ('期', '支', '平声', '上平', 6),
('迟', '支', '平声', '上平', 7), ('悲', '支', '平声', '上平', 8), ('丝', '支', '平声', '上平', 9),
('眉', '支', '平声', '上平', 10), ('池', '支', '平声', '上平', 11), ('枝', '支', '平声', '上平', 12),
('移', '支', '平声', '上平', 13), ('随', '支', '平声', '上平', 14), ('谁', '支', '平声', '上平', 15),
('吹', '支', '平声', '上平', 16), ('离', '支', '平声', '上平', 17), ('奇', '支', '平声', '上平', 18),
('宜', '支', '平声', '上平', 19), ('仪', '支', '平声', '上平', 20),
-- 五微
('微', '微', '平声', '上平', 1), ('衣', '微', '平声', '上平', 2), ('归', '微', '平声', '上平', 3),
('飞', '微', '平声', '上平', 4), ('非', '微', '平声', '上平', 5), ('机', '微', '平声', '上平', 6),
('稀', '微', '平声', '上平', 7), ('依', '微', '平声', '上平', 8), ('晖', '微', '平声', '上平', 9),
('辉', '微', '平声', '上平', 10), ('薇', '微', '平声', '上平', 11), ('扉', '微', '平声', '上平', 12),
-- 六鱼
('鱼', '鱼', '平声', '上平', 1), ('书', '鱼', '平声', '上平', 2), ('居', '鱼', '平声', '上平', 3),
('初', '鱼', '平声', '上平', 4), ('如', '鱼', '平声', '上平', 5), ('虚', '鱼', '平声', '上平', 6),
('余', '鱼', '平声', '上平', 7), ('庐', '鱼', '平声', '上平', 8), ('疏', '鱼', '平声', '上平', 9),
('梳', '鱼', '平声', '上平', 10), ('裾', '鱼', '平声', '上平', 11), ('渠', '鱼', '平声', '上平', 12),
-- 七虞
('虞', '虞', '平声', '上平', 1), ('无', '虞', '平声', '上平', 2), ('夫', '虞', '平声', '上平', 3),
('珠', '虞', '平声', '上平', 4), ('湖', '虞', '平声', '上平', 5), ('孤', '虞', '平声', '上平', 6),
('壶', '虞', '平声', '上平', 7), ('途', '虞', '平声', '上平', 8), ('图', '虞', '平声', '上平', 9),
('呼', '虞', '平声', '上平', 10), ('徒', '虞', '平声', '上平', 11), ('炉', '虞', '平声', '上平', 12),
('蒲', '虞', '平声', '上平', 13), ('苏', '虞', '平声', '上平', 14), ('殊', '虞', '平声', '上平', 15),
-- 八齐
('齐', '齐', '平声', '上平', 1), ('西', '齐', '平声', '上平', 2), ('低', '齐', '平声', '上平', 3),
('溪', '齐', '平声', '上平', 4), ('泥', '齐', '平声', '上平', 5), ('迷', '齐', '平声', '上平', 6),
('鸡', '齐', '平声', '上平', 7), ('啼', '齐', '平声', '上平', 8), ('栖', '齐', '平声', '上平', 9),
('题', '齐', '平声', '上平', 10), ('梯', '齐', '平声', '上平', 11), ('闺', '齐', '平声', '上平', 12),
-- 九佳
('佳', '佳', '平声', '上平', 1), ('街', '佳', '平声', '上平', 2), ('鞋', '佳', '平声', '上平', 3),
('牌', '佳', '平声', '上平', 4), ('柴', '佳', '平声', '上平', 5), ('钗', '佳', '平声', '上平', 6),
('崖', '佳', '平声', '上平', 7), ('阶', '佳', '平声', '上平', 8), ('怀', '佳', '平声', '上平', 9),
-- 十灰
('灰', '灰', '平声', '上平', 1), ('开', '灰', '平声', '上平', 2), ('来', '灰', '平声', '上平', 3),
('台', '灰', '平声', '上平', 4), ('才', '灰', '平声', '上平', 5), ('回', '灰', '平声', '上平', 6),
('杯', '灰', '平声', '上平', 7), ('梅', '灰', '平声', '上平', 8), ('哀', '灰', '平声', '上平', 9),
('催', '灰', '平声', '上平', 10), ('裁', '灰', '平声', '上平', 11), ('猜', '灰', '平声', '上平', 12),
-- 十一真
('真', '真', '平声', '上平', 1), ('人', '真', '平声', '上平', 2), ('春', '真', '平声', '上平', 3),
('新', '真', '平声', '上平', 4), ('尘', '真', '平声', '上平', 5), ('身', '真', '平声', '上平', 6),
('神', '真', '平声', '上平', 7), ('亲', '真', '平声', '上平', 8), ('臣', '真', '平声', '上平', 9),
('晨', '真', '平声', '上平', 10), ('频', '真', '平声', '上平', 11), ('邻', '真', '平声', '上平', 12),
('贫', '真', '平声', '上平', 13), ('津', '真', '平声', '上平', 14), ('珍', '真', '平声', '上平', 15),
-- 十二文
('文', '文', '平声', '上平', 1), ('云', '文', '平声', '上平', 2), ('君', '文', '平声', '上平', 3),
('群', '文', '平声', '上平', 4), ('军', '文', '平声', '上平', 5), ('纷', '文', '平声', '上平', 6),
('闻', '文', '平声', '上平', 7), ('分', '文', '平声', '上平', 8), ('氛', '文', '平声', '上平', 9),
('芬', '文', '平声', '上平', 10), ('焚', '文', '平声', '上平', 11), ('坟', '文', '平声', '上平', 12),
-- 十三元
('元', '元', '平声', '上平', 1), ('园', '元', '平声', '上平', 2), ('原', '元', '平声', '上平', 3),
('源', '元', '平声', '上平', 4), ('言', '元', '平声', '上平', 5), ('门', '元', '平声', '上平', 6),
('村', '元', '平声', '上平', 7), ('魂', '元', '平声', '上平', 8), ('存', '元', '平声', '上平', 9),
('根', '元', '平声', '上平', 10), ('昏', '元', '平声', '上平', 11), ('恩', '元', '平声', '上平', 12),
('论', '元', '平声', '上平', 13), ('温', '元', '平声', '上平', 14), ('孙', '元', '平声', '上平', 15),
-- 十四寒
('寒', '寒', '平声', '上平', 1), ('山', '寒', '平声', '上平', 2), ('关', '寒', '平声', '上平', 3),
('难', '寒', '平声', '上平', 4), ('还', '寒', '平声', '上平', 5), ('间', '寒', '平声', '上平', 6),
('看', '寒', '平声', '上平', 7), ('安', '寒', '平声', '上平', 8), ('残', '寒', '平声', '上平', 9),
('闲', '寒', '平声', '上平', 10), ('欢', '寒', '平声', '上平', 11), ('官', '寒', '平声', '上平', 12),
('冠', '寒', '平声', '上平', 13), ('丹', '寒', '平声', '上平', 14), ('兰', '寒', '平声', '上平', 15),
('盘', '寒', '平声', '上平', 16), ('干', '寒', '平声', '上平', 17), ('坛', '寒', '平声', '上平', 18),
-- 十五删
('删', '删', '平声', '上平', 1), ('颜', '删', '平声', '上平', 2), ('攀', '删', '平声', '上平', 3),
('关', '删', '平声', '上平', 4), ('班', '删', '平声', '上平', 5), ('斑', '删', '平声', '上平', 6),
('湾', '删', '平声', '上平', 7), ('环', '删', '平声', '上平', 8), ('闲', '删', '平声', '上平', 9),
('潸', '删', '平声', '上平', 10), ('蛮', '删', '平声', '上平', 11), ('鬟', '删', '平声', '上平', 12);

-- 下平声（15韵）
INSERT IGNORE INTO `rhyme` (`character`, `rhyme_group`, `tone_type`, `rhyme_category`, `sort_order`) VALUES
-- 一先
('先', '先', '平声', '下平', 1), ('天', '先', '平声', '下平', 2), ('年', '先', '平声', '下平', 3),
('前', '先', '平声', '下平', 4), ('然', '先', '平声', '下平', 5), ('边', '先', '平声', '下平', 6),
('仙', '先', '平声', '下平', 7), ('烟', '先', '平声', '下平', 8), ('莲', '先', '平声', '下平', 9),
('田', '先', '平声', '下平', 10), ('川', '先', '平声', '下平', 11), ('弦', '先', '平声', '下平', 12),
('船', '先', '平声', '下平', 13), ('眠', '先', '平声', '下平', 14), ('泉', '先', '平声', '下平', 15),
('篇', '先', '平声', '下平', 16), ('圆', '先', '平声', '下平', 17), ('全', '先', '平声', '下平', 18),
-- 二萧
('萧', '萧', '平声', '下平', 1), ('朝', '萧', '平声', '下平', 2), ('遥', '萧', '平声', '下平', 3),
('桥', '萧', '平声', '下平', 4), ('腰', '萧', '平声', '下平', 5), ('飘', '萧', '平声', '下平', 6),
('苗', '萧', '平声', '下平', 7), ('条', '萧', '平声', '下平', 8), ('昭', '萧', '平声', '下平', 9),
('招', '萧', '平声', '下平', 10), ('消', '萧', '平声', '下平', 11), ('潮', '萧', '平声', '下平', 12),
('箫', '萧', '平声', '下平', 13), ('貂', '萧', '平声', '下平', 14), ('雕', '萧', '平声', '下平', 15),
-- 三肴
('肴', '肴', '平声', '下平', 1), ('交', '肴', '平声', '下平', 2), ('巢', '肴', '平声', '下平', 3),
('梢', '肴', '平声', '下平', 4), ('茅', '肴', '平声', '下平', 5), ('郊', '肴', '平声', '下平', 6),
('包', '肴', '平声', '下平', 7), ('敲', '肴', '平声', '下平', 8), ('抄', '肴', '平声', '下平', 9),
-- 四豪
('豪', '豪', '平声', '下平', 1), ('高', '豪', '平声', '下平', 2), ('刀', '豪', '平声', '下平', 3),
('劳', '豪', '平声', '下平', 4), ('曹', '豪', '平声', '下平', 5), ('毫', '豪', '平声', '下平', 6),
('涛', '豪', '平声', '下平', 7), ('桃', '豪', '平声', '下平', 8), ('袍', '豪', '平声', '下平', 9),
('遭', '豪', '平声', '下平', 10), ('醪', '豪', '平声', '下平', 11), ('号', '豪', '平声', '下平', 12),
-- 五歌
('歌', '歌', '平声', '下平', 1), ('多', '歌', '平声', '下平', 2), ('河', '歌', '平声', '下平', 3),
('何', '歌', '平声', '下平', 4), ('波', '歌', '平声', '下平', 5), ('罗', '歌', '平声', '下平', 6),
('柯', '歌', '平声', '下平', 7), ('蛾', '歌', '平声', '下平', 8), ('梭', '歌', '平声', '下平', 9),
('磨', '歌', '平声', '下平', 10), ('陀', '歌', '平声', '下平', 11), ('跎', '歌', '平声', '下平', 12),
-- 六麻
('麻', '麻', '平声', '下平', 1), ('花', '麻', '平声', '下平', 2), ('家', '麻', '平声', '下平', 3),
('华', '麻', '平声', '下平', 4), ('斜', '麻', '平声', '下平', 5), ('沙', '麻', '平声', '下平', 6),
('茶', '麻', '平声', '下平', 7), ('牙', '麻', '平声', '下平', 8), ('霞', '麻', '平声', '下平', 9),
('涯', '麻', '平声', '下平', 10), ('瓜', '麻', '平声', '下平', 11), ('蛇', '麻', '平声', '下平', 12),
('车', '麻', '平声', '下平', 13), ('纱', '麻', '平声', '下平', 14), ('遮', '麻', '平声', '下平', 15),
-- 七阳
('阳', '阳', '平声', '下平', 1), ('光', '阳', '平声', '下平', 2), ('长', '阳', '平声', '下平', 3),
('方', '阳', '平声', '下平', 4), ('香', '阳', '平声', '下平', 5), ('堂', '阳', '平声', '下平', 6),
('王', '阳', '平声', '下平', 7), ('乡', '阳', '平声', '下平', 8), ('霜', '阳', '平声', '下平', 9),
('凉', '阳', '平声', '下平', 10), ('妆', '阳', '平声', '下平', 11), ('裳', '阳', '平声', '下平', 12),
('忘', '阳', '平声', '下平', 13), ('狂', '阳', '平声', '下平', 14), ('黄', '阳', '平声', '下平', 15),
('苍', '阳', '平声', '下平', 16), ('茫', '阳', '平声', '下平', 17), ('伤', '阳', '平声', '下平', 18),
('肠', '阳', '平声', '下平', 19), ('翔', '阳', '平声', '下平', 20),
-- 八庚
('庚', '庚', '平声', '下平', 1), ('明', '庚', '平声', '下平', 2), ('声', '庚', '平声', '下平', 3),
('清', '庚', '平声', '下平', 4), ('情', '庚', '平声', '下平', 5), ('生', '庚', '平声', '下平', 6),
('行', '庚', '平声', '下平', 7), ('城', '庚', '平声', '下平', 8), ('名', '庚', '平声', '下平', 9),
('平', '庚', '平声', '下平', 10), ('京', '庚', '平声', '下平', 11), ('英', '庚', '平声', '下平', 12),
('兵', '庚', '平声', '下平', 13), ('惊', '庚', '平声', '下平', 14), ('荣', '庚', '平声', '下平', 15),
('营', '庚', '平声', '下平', 16), ('横', '庚', '平声', '下平', 17), ('倾', '庚', '平声', '下平', 18),
-- 九青
('青', '青', '平声', '下平', 1), ('星', '青', '平声', '下平', 2), ('灵', '青', '平声', '下平', 3),
('听', '青', '平声', '下平', 4), ('经', '青', '平声', '下平', 5), ('亭', '青', '平声', '下平', 6),
('形', '青', '平声', '下平', 7), ('瓶', '青', '平声', '下平', 8), ('屏', '青', '平声', '下平', 9),
('铭', '青', '平声', '下平', 10), ('铃', '青', '平声', '下平', 11), ('龄', '青', '平声', '下平', 12),
-- 十蒸
('蒸', '蒸', '平声', '下平', 1), ('冰', '蒸', '平声', '下平', 2), ('升', '蒸', '平声', '下平', 3),
('灯', '蒸', '平声', '下平', 4), ('层', '蒸', '平声', '下平', 5), ('鹰', '蒸', '平声', '下平', 6),
('凭', '蒸', '平声', '下平', 7), ('承', '蒸', '平声', '下平', 8), ('凝', '蒸', '平声', '下平', 9),
('蒸', '蒸', '平声', '下平', 10), ('僧', '蒸', '平声', '下平', 11), ('曾', '蒸', '平声', '下平', 12),
-- 十一尤
('尤', '尤', '平声', '下平', 1), ('秋', '尤', '平声', '下平', 2), ('流', '尤', '平声', '下平', 3),
('楼', '尤', '平声', '下平', 4), ('舟', '尤', '平声', '下平', 5), ('愁', '尤', '平声', '下平', 6),
('游', '尤', '平声', '下平', 7), ('州', '尤', '平声', '下平', 8), ('头', '尤', '平声', '下平', 9),
('休', '尤', '平声', '下平', 10), ('收', '尤', '平声', '下平', 11), ('留', '尤', '平声', '下平', 12),
('求', '尤', '平声', '下平', 13), ('侯', '尤', '平声', '下平', 14), ('钩', '尤', '平声', '下平', 15),
('幽', '尤', '平声', '下平', 16), ('牛', '尤', '平声', '下平', 17), ('丘', '尤', '平声', '下平', 18),
-- 十二侵
('侵', '侵', '平声', '下平', 1), ('心', '侵', '平声', '下平', 2), ('深', '侵', '平声', '下平', 3),
('林', '侵', '平声', '下平', 4), ('金', '侵', '平声', '下平', 5), ('琴', '侵', '平声', '下平', 6),
('音', '侵', '平声', '下平', 7), ('吟', '侵', '平声', '下平', 8), ('阴', '侵', '平声', '下平', 9),
('沉', '侵', '平声', '下平', 10), ('寻', '侵', '平声', '下平', 11), ('禽', '侵', '平声', '下平', 12),
('针', '侵', '平声', '下平', 13), ('临', '侵', '平声', '下平', 14), ('簪', '侵', '平声', '下平', 15),
-- 十三覃
('覃', '覃', '平声', '下平', 1), ('南', '覃', '平声', '下平', 2), ('蓝', '覃', '平声', '下平', 3),
('甘', '覃', '平声', '下平', 4), ('岚', '覃', '平声', '下平', 5), ('蚕', '覃', '平声', '下平', 6),
('贪', '覃', '平声', '下平', 7), ('谈', '覃', '平声', '下平', 8), ('涵', '覃', '平声', '下平', 9),
-- 十四盐
('盐', '盐', '平声', '下平', 1), ('帘', '盐', '平声', '下平', 2), ('添', '盐', '平声', '下平', 3),
('嫌', '盐', '平声', '下平', 4), ('严', '盐', '平声', '下平', 5), ('纤', '盐', '平声', '下平', 6),
('蟾', '盐', '平声', '下平', 7), ('拈', '盐', '平声', '下平', 8), ('签', '盐', '平声', '下平', 9),
-- 十五咸
('咸', '咸', '平声', '下平', 1), ('岩', '咸', '平声', '下平', 2), ('衫', '咸', '平声', '下平', 3),
('帆', '咸', '平声', '下平', 4), ('馋', '咸', '平声', '下平', 5), ('缄', '咸', '平声', '下平', 6),
('杉', '咸', '平声', '下平', 7), ('嵌', '咸', '平声', '下平', 8), ('谗', '咸', '平声', '下平', 9);

-- 常用仄声字（上声、去声、入声各选代表韵部）
INSERT IGNORE INTO `rhyme` (`character`, `rhyme_group`, `tone_type`, `rhyme_category`, `sort_order`) VALUES
-- 上声 一董
('董', '董', '上声', '上声', 1), ('动', '董', '上声', '上声', 2), ('总', '董', '上声', '上声', 3),
('洞', '董', '上声', '上声', 4), ('桶', '董', '上声', '上声', 5), ('拢', '董', '上声', '上声', 6),
-- 上声 四纸
('纸', '纸', '上声', '上声', 1), ('此', '纸', '上声', '上声', 2), ('子', '纸', '上声', '上声', 3),
('己', '纸', '上声', '上声', 4), ('里', '纸', '上声', '上声', 5), ('起', '纸', '上声', '上声', 6),
('死', '纸', '上声', '上声', 7), ('美', '纸', '上声', '上声', 8), ('几', '纸', '上声', '上声', 9),
('指', '纸', '上声', '上声', 10), ('似', '纸', '上声', '上声', 11), ('水', '纸', '上声', '上声', 12),
-- 去声 一送
('送', '送', '去声', '去声', 1), ('梦', '送', '去声', '去声', 2), ('凤', '送', '去声', '去声', 3),
('众', '送', '去声', '去声', 4), ('弄', '送', '去声', '去声', 5), ('冻', '送', '去声', '去声', 6),
-- 去声 四寘
('寘', '寘', '去声', '去声', 1), ('地', '寘', '去声', '去声', 2), ('意', '寘', '去声', '去声', 3),
('至', '寘', '去声', '去声', 4), ('寺', '寘', '去声', '去声', 5), ('泪', '寘', '去声', '去声', 6),
('醉', '寘', '去声', '去声', 7), ('字', '寘', '去声', '去声', 8), ('寄', '寘', '去声', '去声', 9),
('义', '寘', '去声', '去声', 10), ('戏', '寘', '去声', '去声', 11), ('翠', '寘', '去声', '去声', 12),
-- 入声 一屋
('屋', '屋', '入声', '入声', 1), ('木', '屋', '入声', '入声', 2), ('目', '屋', '入声', '入声', 3),
('竹', '屋', '入声', '入声', 4), ('谷', '屋', '入声', '入声', 5), ('足', '屋', '入声', '入声', 6),
('曲', '屋', '入声', '入声', 7), ('玉', '屋', '入声', '入声', 8), ('烛', '屋', '入声', '入声', 9),
('绿', '屋', '入声', '入声', 10), ('独', '屋', '入声', '入声', 11), ('宿', '屋', '入声', '入声', 12),
-- 入声 六月
('月', '月', '入声', '入声', 1), ('骨', '月', '入声', '入声', 2), ('发', '月', '入声', '入声', 3),
('阔', '月', '入声', '入声', 4), ('歇', '月', '入声', '入声', 5), ('没', '月', '入声', '入声', 6),
('节', '月', '入声', '入声', 7), ('雪', '月', '入声', '入声', 8), ('别', '月', '入声', '入声', 9),
('灭', '月', '入声', '入声', 10), ('绝', '月', '入声', '入声', 11), ('说', '月', '入声', '入声', 12);
