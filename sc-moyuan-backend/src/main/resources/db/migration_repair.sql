-- 报修工单表
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

-- 报修反馈表
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
