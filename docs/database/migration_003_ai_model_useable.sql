-- ====================================================================
-- 迁移脚本 migration_003_ai_model_useable.sql
-- 目的：按实测结果启用可用 AI 模型，修正失效模型配置
--  1. DeepSeek 改用 deepseek-v4-flash 并启用
--  2. zhipu-flash(glm-4.7-flash) 设为默认模型
--  3. 新增智谱 glm-4-flash（实测可用）
--  4. 新增 OpenRouter MiniMax-M2.7 免费档（实测可用）
-- 说明：模型中 api_key 仅保留占位符，运行时代码会从
--      secrets/application-secrets.yml 的 ai.providers.* 注入真实密钥。
-- 适用：已有运行库；新装库由 init.sql 中相同变更保证。
-- ====================================================================

-- 1) 智谱 glm-4 不再作为默认模型
UPDATE `ai_model` SET `is_default` = 0 WHERE `name` = 'zhipu';

-- 2) zhipu-flash (glm-4.7-flash) 设为默认并启用
UPDATE `ai_model` SET `is_default` = 1, `is_enabled` = 1 WHERE `name` = 'zhipu-flash';

-- 3) DeepSeek 改用 deepseek-v4-flash 并启用
UPDATE `ai_model`
SET `model_id` = 'deepseek-v4-flash', `display_name` = 'DeepSeek-V4-Flash', `is_enabled` = 1, `is_default` = 0
WHERE `name` = 'deepseek';

-- 4) 新增智谱 glm-4-flash（api_key 由 secrets 注入，库内留占位符）
INSERT INTO `ai_model`
(`name`, `display_name`, `provider`, `model_type`, `api_url`, `api_key`, `model_id`, `vision_model_id`, `max_tokens`, `is_enabled`, `is_default`, `sort_order`)
VALUES
('zhipu-glm4-flash', '智谱GLM-4-Flash', 'zhipu', 'text', 'https://open.bigmodel.cn/api/paas/v4/chat/completions', 'your-zhipu-api-key', 'glm-4-flash', NULL, 1024, 1, 0, 28)
ON DUPLICATE KEY UPDATE `model_id` = 'glm-4-flash', `is_enabled` = 1;

-- 5) 新增 OpenRouter MiniMax-M2.7 免费档
INSERT INTO `ai_model`
(`name`, `display_name`, `provider`, `model_type`, `api_url`, `api_key`, `model_id`, `vision_model_id`, `max_tokens`, `is_enabled`, `is_default`, `sort_order`)
VALUES
('free-minimax-m27', 'MiniMax-M2.7 (免费)', 'openrouter', 'text', 'https://openrouter.ai/api/v1/chat/completions', 'your-openrouter-key', 'minimax/minimax-m2.7:free', NULL, 1024, 1, 0, 29)
ON DUPLICATE KEY UPDATE `model_id` = 'minimax/minimax-m2.7:free', `is_enabled` = 1;