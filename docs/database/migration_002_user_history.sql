-- ============================================================
-- 迁移脚本：创建用户浏览历史表
-- 版本：migration_002
-- 日期：2026-05-29
-- 说明：记录用户浏览诗词、诗人、帖子的历史记录
-- ============================================================

-- 创建用户浏览历史表
CREATE TABLE IF NOT EXISTS `user_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `target_id` BIGINT NOT NULL COMMENT '浏览目标ID',
    `target_type` TINYINT NOT NULL COMMENT '浏览类型：1-诗词，2-诗人，3-帖子',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_history_user_id` (`user_id`) COMMENT '用户ID索引',
    KEY `idx_user_history_target` (`target_id`, `target_type`) COMMENT '目标索引',
    KEY `idx_user_history_create_time` (`create_time`) COMMENT '创建时间索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户浏览历史表';

-- ============================================================
-- 回滚语句（如需回滚，执行以下语句）
-- ============================================================
-- DROP TABLE IF EXISTS `user_history`;
