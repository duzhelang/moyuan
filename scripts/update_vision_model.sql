-- 更新智谱AI视觉模型配置
UPDATE `ai_model` SET 
  `api_key` = '1a0d9529e8b64c88bffb00cdfba77b44.3ZkEwG4Kvsanf6Qb',
  `model_id` = 'glm-4',
  `vision_model_id` = 'glm-4.6v-flash',
  `model_type` = 'both',
  `is_enabled` = 1,
  `is_default` = 1
WHERE `name` = 'zhipu';

UPDATE `ai_model` SET 
  `api_key` = '1a0d9529e8b64c88bffb00cdfba77b44.3ZkEwG4Kvsanf6Qb',
  `is_enabled` = 1
WHERE `name` = 'zhipu-flash';

-- 验证更新结果
SELECT `name`, `display_name`, `model_type`, `model_id`, `vision_model_id`, `is_enabled`, `is_default` 
FROM `ai_model` 
WHERE `name` IN ('zhipu', 'zhipu-flash');
