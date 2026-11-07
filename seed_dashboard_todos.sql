-- =============================================================
-- 后台仪表盘「运营待办」演示数据
--   1) 2 位申请诗人认证的用户（verified_status = 0，等待审核）
--   2) 3 条待审核评论（status = 0），挂在已有作品 / 帖子下
-- 说明：脚本幂等，重复执行不会产生重复数据。
--       密码统一 123456（与森垚 / 逼王 / 九刀一致）。
-- =============================================================

SET NAMES utf8mb4;

-- ---------- 1. 两个申请认证的账号 ----------
INSERT INTO `user`
(`username`, `password`, `email`, `phone`, `nickname`, `avatar`, `gender`, `birthday`,
 `bio`, `interests`, `role`, `status`, `poet_verified`, `create_time`, `update_time`)
SELECT 'shidengren', '$2a$10$WJj6mQAQMY9OfJdoYbefquZtWh6B0r0Dm7Wg2EY2EP4XPBLD9.8Zy',
       'shidengren@moyuan.com', NULL, '拾灯人',
       '/img/poet_avatars/avatar_cn_modern_male_01.jpg', 1, NULL,
       '读得多，写得少。夜里读书，读到好的句子会抄一遍，抄完就搁着。',
       '古体诗,词,书法,藏书', 'user', 1, 0, '2026-10-30 21:12:00', '2026-10-30 21:12:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'shidengren');

INSERT INTO `user`
(`username`, `password`, `email`, `phone`, `nickname`, `avatar`, `gender`, `birthday`,
 `bio`, `interests`, `role`, `status`, `poet_verified`, `create_time`, `update_time`)
SELECT 'nanmo', '$2a$10$WJj6mQAQMY9OfJdoYbefquZtWh6B0r0Dm7Wg2EY2EP4XPBLD9.8Zy',
       'nanmo@moyuan.com', NULL, '南陌',
       '/img/poet_avatars/avatar_cn_modern_male_01.jpg', 1, NULL,
       '在南方写北方的诗。习惯把句子写到一半就停，剩下的交给天气。',
       '现代诗,翻译,摄影', 'user', 1, 0, '2026-11-01 19:40:00', '2026-11-01 19:40:00'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `username` = 'nanmo');

-- ---------- 2. 两条待审的认证申请 ----------
INSERT INTO `poet_profile`
(`user_id`, `pen_name`, `real_name`, `specialty`, `introduction`, `literary_concept`,
 `achievements`, `contact_info`, `verified_status`, `verified_reason`, `work_count`,
 `like_count`, `favorite_count`, `follower_count`, `create_time`, `update_time`)
SELECT u.`id`, '拾灯人', NULL, '古体诗 / 词',
       '写旧体，五年攒下不到四十首，多半是读别人的时候顺手记下的半句。申请认证是想把压在抽屉里的稿子拿出来见光，也想知道自己离「能立住」还差多远。',
       '旧体诗的门槛不在格律，在有没有一句非说不可的话。先把那句话找到，格律是可以慢慢磨的；找不到，格律再稳也只是空壳。',
       '自印过一册《灯下集》，不对外流通，只送同好十余本。',
       '站内私信', 0, NULL, 0, 0, 0, 0, '2026-11-06 21:18:00', '2026-11-06 21:18:00'
FROM `user` u
WHERE u.`username` = 'shidengren'
  AND NOT EXISTS (SELECT 1 FROM `poet_profile` p WHERE p.`user_id` = u.`id`);

INSERT INTO `poet_profile`
(`user_id`, `pen_name`, `real_name`, `specialty`, `introduction`, `literary_concept`,
 `achievements`, `contact_info`, `verified_status`, `verified_reason`, `work_count`,
 `like_count`, `favorite_count`, `follower_count`, `create_time`, `update_time`)
SELECT u.`id`, '南陌', NULL, '现代诗',
       '写自由体，也译一点外诗。习惯把一首诗写到七分就停手，觉得最后那三分一旦写出来，前面七分就都白写了。想在这里找到愿意读慢句子的读者。',
       '诗不是把话说清楚，是让一句话在某处忽然站住。停顿和留白不是技巧，是态度——愿意等的人才能读到。',
       '译诗散见于几个同人小刊，未结集。',
       '邮箱：nanmo@moyuan.com', 0, NULL, 0, 0, 0, 0, '2026-11-07 22:05:00', '2026-11-07 22:05:00'
FROM `user` u
WHERE u.`username` = 'nanmo'
  AND NOT EXISTS (SELECT 1 FROM `poet_profile` p WHERE p.`user_id` = u.`id`);

-- ---------- 3. 三条待审核评论（target_type：1=诗词，2=帖子）----------

-- 3.1 拾灯人 → 九刀《如风的微笑》(poem 9873)
INSERT INTO `comment`
(`content`, `user_id`, `target_id`, `target_type`, `parent_id`, `like_count`, `status`,
 `create_time`, `update_time`, `deleted`)
SELECT '「视线」出现四次，每次前面的修饰都不一样：痴情的、有限的、沿着摸索的、最后什么都没找到的。到第四次的时候，这个词已经空了——你把整首诗的重量全压在这一个词的耗尽上，很狠。',
       u.`id`, 9873, 1, 0, 0, 0, '2026-11-07 23:12:00', '2026-11-07 23:12:00', 0
FROM `user` u
WHERE u.`username` = 'shidengren'
  AND NOT EXISTS (
    SELECT 1 FROM `comment` c
    WHERE c.`user_id` = u.`id` AND c.`target_type` = 1 AND c.`target_id` = 9873
  );

-- 3.2 南陌 → 森垚《白夜月望》(poem 15)
INSERT INTO `comment`
(`content`, `user_id`, `target_id`, `target_type`, `parent_id`, `like_count`, `status`,
 `create_time`, `update_time`, `deleted`)
SELECT '「经日积灰新作旧」七个字里塞了时间的两种走法：东西在旧，人也在旧，而新作反倒先旧了。后面接「杯弓痛」有点急，弓影之痛还没落地，就先替读者喊了出来，这句可以再压一压。',
       u.`id`, 15, 1, 0, 0, 0, '2026-11-07 23:26:00', '2026-11-07 23:26:00', 0
FROM `user` u
WHERE u.`username` = 'nanmo'
  AND NOT EXISTS (
    SELECT 1 FROM `comment` c
    WHERE c.`user_id` = u.`id` AND c.`target_type` = 1 AND c.`target_id` = 15
  );

-- 3.3 拾灯人 → 常平逼王《忆江南·哀》为什么三字一顿 (post 123)
INSERT INTO `comment`
(`content`, `user_id`, `target_id`, `target_type`, `parent_id`, `like_count`, `status`,
 `create_time`, `update_time`, `deleted`)
SELECT '三字一顿的代价是每顿都要自己扛住。二十七字里如果有一处顿得虚，整首就散了，所以这不是把句子切短，是把每一处停顿都变成一次呼吸。你这个选择很险，但守住了。',
       u.`id`, 123, 2, 0, 0, 0, '2026-11-07 23:41:00', '2026-11-07 23:41:00', 0
FROM `user` u
WHERE u.`username` = 'shidengren'
  AND NOT EXISTS (
    SELECT 1 FROM `comment` c
    WHERE c.`user_id` = u.`id` AND c.`target_type` = 2 AND c.`target_id` = 123
  );

-- ---------- 核验 ----------
SELECT '待办统计' AS item,
       (SELECT COUNT(*) FROM poem   WHERE audit_status = 0 AND deleted = 0) AS 诗词待审核,
       (SELECT COUNT(*) FROM comment WHERE status = 0 AND deleted = 0)      AS 评论待审核,
       (SELECT COUNT(*) FROM poet_profile WHERE verified_status = 0)        AS 诗人认证,
       (SELECT COUNT(*) FROM ai_generated_content WHERE status = 0)         AS AI内容待审,
       (SELECT COUNT(*) FROM poet_draft WHERE status = 0)                   AS 草稿待审,
       (SELECT COUNT(*) FROM poet_suggestion WHERE status = 'pending')      AS 建议待审;

SELECT u.`id`, u.`username`, u.`nickname`, p.`pen_name`, p.`verified_status`, p.`create_time`
FROM `user` u
LEFT JOIN `poet_profile` p ON p.`user_id` = u.`id`
WHERE u.`username` IN ('shidengren', 'nanmo');
