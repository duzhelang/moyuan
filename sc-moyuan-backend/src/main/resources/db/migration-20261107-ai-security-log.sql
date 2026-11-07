-- ============================================================
-- AI 调用日志表（管理端「AI 失败率」/ 监测页「AI 链路」数据源）
-- 迁移日期：2026-11-07
-- 说明：由 AiCallLogService 异步写入。业务异常返回 HTTP 200，
--       无法用 HTTP 状态码判断失败，因此必须显式记录状态。
-- ============================================================
CREATE TABLE IF NOT EXISTS `ai_call_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `endpoint` VARCHAR(32) NOT NULL COMMENT '入口：chat/chat_stream/analyze/write_poem/ocr/couplet',
  `model` VARCHAR(64) DEFAULT NULL COMMENT '请求的模型标识或模块编码',
  `module_code` VARCHAR(64) DEFAULT NULL COMMENT 'AI 模块编码（可为空）',
  `status` VARCHAR(16) NOT NULL COMMENT 'success / error',
  `error_msg` VARCHAR(255) DEFAULT NULL COMMENT '失败原因（截断）',
  `duration_ms` INT NOT NULL DEFAULT 0 COMMENT '总耗时（毫秒）',
  `ttft_ms` INT DEFAULT NULL COMMENT '首字延迟（毫秒），仅流式接口有值',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID（未登录为 NULL）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  PRIMARY KEY (`id`),
  KEY `idx_acl_create_time` (`create_time`),
  KEY `idx_acl_endpoint` (`endpoint`),
  KEY `idx_acl_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 调用日志（失败率/首字延迟数据源）';

-- ============================================================
-- 安全事件表（管理端「登录失败」/ 监测页「安全审计」数据源）
-- 迁移日期：2026-11-07
-- 说明：Spring Security 过滤器拦截的 401/403 不经过 MVC，需在 handler 里显式记录。
-- ============================================================
CREATE TABLE IF NOT EXISTS `security_event` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type` VARCHAR(24) NOT NULL COMMENT 'login_fail / forbidden / rate_limit',
  `detail` VARCHAR(255) DEFAULT NULL COMMENT '事件详情（用户名/路径/原因）',
  `ip` VARCHAR(64) DEFAULT NULL COMMENT '来源 IP',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID（可为空）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间',
  PRIMARY KEY (`id`),
  KEY `idx_se_create_time` (`create_time`),
  KEY `idx_se_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='安全事件（登录失败/越权/限流）';
