-- ============================================================
-- 接口调用指标表（管理端「今日体检」错误率 / P95、监测页「可用性」数据源）
-- 迁移日期：2026-11-07
-- 说明：由 ApiMetricInterceptor 异步写入，单条 INSERT，表结构保持精简。
-- ============================================================
CREATE TABLE IF NOT EXISTS `api_request_metric` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `method` VARCHAR(10) DEFAULT NULL COMMENT 'HTTP 方法',
  `path` VARCHAR(200) NOT NULL COMMENT '接口路径（不含查询串）',
  `status_code` INT NOT NULL COMMENT 'HTTP 状态码',
  `duration_ms` INT NOT NULL COMMENT '耗时（毫秒）',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID（未登录为 NULL）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  PRIMARY KEY (`id`),
  KEY `idx_arm_create_time` (`create_time`),
  KEY `idx_arm_path` (`path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='接口调用指标（错误率/P95 数据源）';
