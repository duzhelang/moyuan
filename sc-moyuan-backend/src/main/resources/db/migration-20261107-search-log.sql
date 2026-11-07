-- ============================================================
-- 搜索埋点表（管理端「搜索无结果率」/「用户分析·搜不到的关键词」数据源）
-- 迁移日期：2026-11-07
-- 说明：由 SearchLogService 异步写入；只记录关键词与命中数，不记录原始查询串之外的内容。
-- ============================================================
CREATE TABLE IF NOT EXISTS `search_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `keyword` VARCHAR(120) NOT NULL COMMENT '搜索关键词（已 trim，超长截断）',
  `result_count` INT NOT NULL DEFAULT 0 COMMENT '命中结果数，0 表示搜不到',
  `route` VARCHAR(32) DEFAULT NULL COMMENT '命中路径：exact/fuzzy/pinyin/pingze/latin/global',
  `source` VARCHAR(20) NOT NULL DEFAULT 'smart' COMMENT '来源：smart 智能搜索 / global 全局搜索',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID（未登录为 NULL）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  PRIMARY KEY (`id`),
  KEY `idx_sl_create_time` (`create_time`),
  KEY `idx_sl_keyword` (`keyword`),
  KEY `idx_sl_result` (`result_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='搜索埋点（无结果率数据源）';
