-- =============================================================
-- 审核流水线：审核员 / 分派规则 / 工单
--   幂等脚本，可重复执行。
--   审核员账号密码统一 123456（与森垚/逼王/九刀一致）。
-- =============================================================

SET NAMES utf8mb4;

-- ---------- 1. 审核员 ----------
CREATE TABLE IF NOT EXISTS `audit_reviewer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '关联 user.id',
  `display_name` varchar(50) NOT NULL COMMENT '工位名，如 诗部·唐',
  `specialties` varchar(200) DEFAULT NULL COMMENT '擅长类型，逗号分隔',
  `max_concurrent` int NOT NULL DEFAULT 30 COMMENT '同时在手工单上限',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1 在岗 0 停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核员';

-- ---------- 2. 分派规则 ----------
CREATE TABLE IF NOT EXISTS `audit_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `scope_type` varchar(20) NOT NULL COMMENT 'author / dynasty / content_type / global',
  `scope_value` varchar(100) DEFAULT NULL COMMENT '作者 id、朝代 id 或内容类型',
  `reviewer_id` bigint NOT NULL COMMENT '命中后分派给谁',
  `priority` int NOT NULL DEFAULT 100 COMMENT '越小越先匹配；建议 author 10 / dynasty 20 / content_type 30 / global 90',
  `enabled` tinyint NOT NULL DEFAULT 1,
  `remark` varchar(200) DEFAULT NULL COMMENT '说明，看板与配置页直接展示',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_scope` (`scope_type`, `scope_value`, `priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核分派规则';

-- ---------- 3. 工单 ----------
CREATE TABLE IF NOT EXISTS `audit_ticket` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `biz_type` varchar(30) NOT NULL COMMENT 'poem/comment/ai_content/poet_profile/poet_draft/poet_suggestion',
  `biz_id` bigint NOT NULL COMMENT '业务主键',
  `title` varchar(200) DEFAULT NULL COMMENT '冗余标题，看板直出',
  `author_name` varchar(100) DEFAULT NULL COMMENT '冗余作者/提交人',
  `author_ref` varchar(50) DEFAULT NULL COMMENT '作者 id，供 author 规则匹配',
  `dynasty_name` varchar(50) DEFAULT NULL,
  `content_type` varchar(30) DEFAULT NULL COMMENT 'poem_ancient/poem_modern/comment/ai_content/...',
  `priority` tinyint NOT NULL DEFAULT 0 COMMENT '0 普通 1 高 2 紧急',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/assigned/reviewing/approved/rejected/void',
  `reviewer_id` bigint DEFAULT NULL,
  `result` varchar(500) DEFAULT NULL COMMENT '审核结论或理由',
  `assign_note` varchar(200) DEFAULT NULL COMMENT '命中的规则说明，便于追溯为什么派给他',
  `assigned_at` datetime DEFAULT NULL,
  `claimed_at` datetime DEFAULT NULL,
  `finished_at` datetime DEFAULT NULL,
  `deadline_at` datetime DEFAULT NULL COMMENT '按优先级算出的 SLA 截止',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz` (`biz_type`, `biz_id`),
  KEY `idx_status_deadline` (`status`, `deadline_at`),
  KEY `idx_reviewer` (`reviewer_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核工单';

-- ---------- 4. 四个部门审核员账号 ----------
INSERT INTO `user`
(`username`, `password`, `email`, `phone`, `nickname`, `avatar`, `gender`, `bio`, `role`, `status`,
 `poet_verified`, `create_time`, `update_time`)
SELECT 'reviewer_tang', '$2a$10$WJj6mQAQMY9OfJdoYbefquZtWh6B0r0Dm7Wg2EY2EP4XPBLD9.8Zy',
       'reviewer_tang@moyuan.com', NULL, '古诗一组',
       '/img/poet_avatars/avatar_cn_modern_male_01.jpg', 1,
       '负责古体诗的审核，重点看格律、用典与作者归属。',
       'reviewer', 1, 0, '2026-11-01 09:00:00', '2026-11-01 09:00:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'reviewer_tang');

INSERT INTO `user`
(`username`, `password`, `email`, `phone`, `nickname`, `avatar`, `gender`, `bio`, `role`, `status`,
 `poet_verified`, `create_time`, `update_time`)
SELECT 'reviewer_song', '$2a$10$WJj6mQAQMY9OfJdoYbefquZtWh6B0r0Dm7Wg2EY2EP4XPBLD9.8Zy',
       'reviewer_song@moyuan.com', NULL, '古诗二组',
       '/img/poet_avatars/avatar_cn_modern_male_01.jpg', 1,
       '负责古体诗与词的审核，重点看词牌、断句与异文比对。',
       'reviewer', 1, 0, '2026-11-01 09:05:00', '2026-11-01 09:05:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'reviewer_song');

INSERT INTO `user`
(`username`, `password`, `email`, `phone`, `nickname`, `avatar`, `gender`, `bio`, `role`, `status`,
 `poet_verified`, `create_time`, `update_time`)
SELECT 'reviewer_modern', '$2a$10$WJj6mQAQMY9OfJdoYbefquZtWh6B0r0Dm7Wg2EY2EP4XPBLD9.8Zy',
       'reviewer_modern@moyuan.com', NULL, '现代诗组',
       '/img/poet_avatars/avatar_cn_modern_male_01.jpg', 1,
       '负责现代诗与当代作者创作，重点看原创性与分行。',
       'reviewer', 1, 0, '2026-11-01 09:10:00', '2026-11-01 09:10:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'reviewer_modern');

INSERT INTO `user`
(`username`, `password`, `email`, `phone`, `nickname`, `avatar`, `gender`, `bio`, `role`, `status`,
 `poet_verified`, `create_time`, `update_time`)
SELECT 'reviewer_content', '$2a$10$WJj6mQAQMY9OfJdoYbefquZtWh6B0r0Dm7Wg2EY2EP4XPBLD9.8Zy',
       'reviewer_content@moyuan.com', NULL, '内容认证组',
       '/img/poet_avatars/avatar_cn_modern_male_01.jpg', 1,
       '负责评论、AI 生成内容与诗人认证材料的审核。',
       'reviewer', 1, 0, '2026-11-01 09:15:00', '2026-11-01 09:15:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'reviewer_content');

-- ---------- 5. 登记审核员（管理员是调度角色，不进入自动派单池）----------
-- 管理员负责建单、分派、改派与审核员团队管理，可协审，但不会被自动分派工单。
INSERT INTO `audit_reviewer` (`user_id`, `display_name`, `specialties`, `max_concurrent`, `status`)
SELECT u.`id`, '古诗一组', 'poem_ancient', 40, 1
FROM `user` u
WHERE u.`username` = 'reviewer_tang'
  AND NOT EXISTS (SELECT 1 FROM `audit_reviewer` r WHERE r.`user_id` = u.`id`);

INSERT INTO `audit_reviewer` (`user_id`, `display_name`, `specialties`, `max_concurrent`, `status`)
SELECT u.`id`, '古诗二组', 'poem_ancient', 40, 1
FROM `user` u
WHERE u.`username` = 'reviewer_song'
  AND NOT EXISTS (SELECT 1 FROM `audit_reviewer` r WHERE r.`user_id` = u.`id`);

INSERT INTO `audit_reviewer` (`user_id`, `display_name`, `specialties`, `max_concurrent`, `status`)
SELECT u.`id`, '现代诗组', 'poem_modern', 40, 1
FROM `user` u
WHERE u.`username` = 'reviewer_modern'
  AND NOT EXISTS (SELECT 1 FROM `audit_reviewer` r WHERE r.`user_id` = u.`id`);

INSERT INTO `audit_reviewer` (`user_id`, `display_name`, `specialties`, `max_concurrent`, `status`)
SELECT u.`id`, '内容认证组', 'comment,ai_content,poet_profile,poet_draft,poet_suggestion', 60, 1
FROM `user` u
WHERE u.`username` = 'reviewer_content'
  AND NOT EXISTS (SELECT 1 FROM `audit_reviewer` r WHERE r.`user_id` = u.`id`);

-- ---------- 6. 默认分派规则 ----------
-- 说明：作者维度的优先级最高，保证「同一作者集中审」，尺度一致；
--       朝代次之；内容类型再次；未命中规则的工单按「擅长类型 + 在手最少」自动选人。
INSERT INTO `audit_rule` (`scope_type`, `scope_value`, `reviewer_id`, `priority`, `enabled`, `remark`)
SELECT 'author', '44', r.`id`, 10, 1, '李清照全部作品集中审（11 首待审）'
FROM `audit_reviewer` r WHERE r.`display_name` = '古诗二组'
  AND NOT EXISTS (SELECT 1 FROM `audit_rule` x WHERE x.`scope_type` = 'author' AND x.`scope_value` = '44');

INSERT INTO `audit_rule` (`scope_type`, `scope_value`, `reviewer_id`, `priority`, `enabled`, `remark`)
SELECT 'author', '79', r.`id`, 10, 1, '温庭筠作品集中审（20 首待审）'
FROM `audit_reviewer` r WHERE r.`display_name` = '古诗一组'
  AND NOT EXISTS (SELECT 1 FROM `audit_rule` x WHERE x.`scope_type` = 'author' AND x.`scope_value` = '79');

INSERT INTO `audit_rule` (`scope_type`, `scope_value`, `reviewer_id`, `priority`, `enabled`, `remark`)
SELECT 'author', '81', r.`id`, 10, 1, '王昌龄作品集中审（15 首待审）'
FROM `audit_reviewer` r WHERE r.`display_name` = '古诗一组'
  AND NOT EXISTS (SELECT 1 FROM `audit_rule` x WHERE x.`scope_type` = 'author' AND x.`scope_value` = '81');

INSERT INTO `audit_rule` (`scope_type`, `scope_value`, `reviewer_id`, `priority`, `enabled`, `remark`)
SELECT 'dynasty', '唐朝', r.`id`, 20, 1, '唐朝作品优先给古诗一组'
FROM `audit_reviewer` r WHERE r.`display_name` = '古诗一组'
  AND NOT EXISTS (SELECT 1 FROM `audit_rule` x WHERE x.`scope_type` = 'dynasty' AND x.`scope_value` = '唐朝');

INSERT INTO `audit_rule` (`scope_type`, `scope_value`, `reviewer_id`, `priority`, `enabled`, `remark`)
SELECT 'dynasty', '宋朝', r.`id`, 20, 1, '宋朝作品优先给古诗二组'
FROM `audit_reviewer` r WHERE r.`display_name` = '古诗二组'
  AND NOT EXISTS (SELECT 1 FROM `audit_rule` x WHERE x.`scope_type` = 'dynasty' AND x.`scope_value` = '宋朝');

INSERT INTO `audit_rule` (`scope_type`, `scope_value`, `reviewer_id`, `priority`, `enabled`, `remark`)
SELECT 'content_type', 'comment', r.`id`, 30, 1, '评论统一由内容认证组处理'
FROM `audit_reviewer` r WHERE r.`display_name` = '内容认证组'
  AND NOT EXISTS (SELECT 1 FROM `audit_rule` x WHERE x.`scope_type` = 'content_type' AND x.`scope_value` = 'comment');

INSERT INTO `audit_rule` (`scope_type`, `scope_value`, `reviewer_id`, `priority`, `enabled`, `remark`)
SELECT 'content_type', 'ai_content', r.`id`, 30, 1, 'AI 生成内容统一由内容认证组处理'
FROM `audit_reviewer` r WHERE r.`display_name` = '内容认证组'
  AND NOT EXISTS (SELECT 1 FROM `audit_rule` x WHERE x.`scope_type` = 'content_type' AND x.`scope_value` = 'ai_content');

INSERT INTO `audit_rule` (`scope_type`, `scope_value`, `reviewer_id`, `priority`, `enabled`, `remark`)
SELECT 'content_type', 'poet_profile', r.`id`, 30, 1, '诗人认证材料由内容认证组处理'
FROM `audit_reviewer` r WHERE r.`display_name` = '内容认证组'
  AND NOT EXISTS (SELECT 1 FROM `audit_rule` x WHERE x.`scope_type` = 'content_type' AND x.`scope_value` = 'poet_profile');

INSERT INTO `audit_rule` (`scope_type`, `scope_value`, `reviewer_id`, `priority`, `enabled`, `remark`)
SELECT 'content_type', 'poet_draft', r.`id`, 30, 1, '正文修订由内容认证组处理'
FROM `audit_reviewer` r WHERE r.`display_name` = '内容认证组'
  AND NOT EXISTS (SELECT 1 FROM `audit_rule` x WHERE x.`scope_type` = 'content_type' AND x.`scope_value` = 'poet_draft');

INSERT INTO `audit_rule` (`scope_type`, `scope_value`, `reviewer_id`, `priority`, `enabled`, `remark`)
SELECT 'content_type', 'poet_suggestion', r.`id`, 30, 1, '用户建议由内容认证组处理'
FROM `audit_reviewer` r WHERE r.`display_name` = '内容认证组'
  AND NOT EXISTS (SELECT 1 FROM `audit_rule` x WHERE x.`scope_type` = 'content_type' AND x.`scope_value` = 'poet_suggestion');

-- 不再配置「全局兜底给管理员」的规则：未命中规则的工单由系统按「擅长类型 + 在手最少」自动选人，
-- 仍分不出去时留在待分派列，由管理员手动指派。

-- ---------- 核验 ----------
SELECT r.`id`, u.`username`, u.`role`, r.`display_name`, r.`specialties`, r.`max_concurrent`
FROM `audit_reviewer` r JOIN `user` u ON u.`id` = r.`user_id` ORDER BY r.`id`;

SELECT x.`id`, x.`scope_type`, x.`scope_value`, r.`display_name` AS reviewer, x.`priority`, x.`remark`
FROM `audit_rule` x JOIN `audit_reviewer` r ON r.`id` = x.`reviewer_id`
ORDER BY x.`priority`, x.`id`;
