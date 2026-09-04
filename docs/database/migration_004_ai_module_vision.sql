-- ====================================================================
-- 迁移脚本 migration_004_ai_module_vision.sql
-- 目的：打好「看图写诗」视觉链路 + 模块挂到新默认模型
--  1. 智谱 zhipu 的视觉模型改为 glm-4v-flash（实测 200 可用）
--  2. write_poem（看图写诗）关联 zhipu（both，具备视觉模型）
--  3. 文本类模块挂到新默认 zhipu-flash（glm-4.7-flash）
-- 适用：已有运行库；新装库由 init.sql 中相同变更保证。
-- ====================================================================

-- 1) 智谱主模型的视觉模型改用 glm-4v-flash
UPDATE `ai_model` SET `vision_model_id` = 'glm-4v-flash' WHERE `name` = 'zhipu';

-- 2) 看图写诗模块关联具备视觉能力的 zhipu
UPDATE `ai_module_config`
SET `model_id` = (SELECT `id` FROM `ai_model` WHERE `name` = 'zhipu')
WHERE `module_code` = 'write_poem';

-- 3) 文本类模块关联新默认 zhipu-flash
UPDATE `ai_module_config`
SET `model_id` = (SELECT `id` FROM `ai_model` WHERE `name` = 'zhipu-flash')
WHERE `module_code` IN ('chat', 'poet_chat', 'poetry_chat', 'analyze', 'couplet');