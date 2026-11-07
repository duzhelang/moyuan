-- ============================================================
-- 当代诗人「森垚」「常平逼王」账号 + 诗人认证 + 社区痕迹
--
-- 关联的诗人主档（poet 表既有记录，不新建）：
--   森垚     → poet.id = 6
--   常平逼王 → poet.id = 1（早期以「初芒」之名发表过《清平乐·情(道姑)》，
--              两者为同一作者，曾用笔名记录在 poet_profile.contact_info）
--
-- 分工：
--   本脚本    —— 账号、认证资料骨架、作品统计、社区内容
--   merge_chumang_and_regen_profiles.sql —— 初芒并入常平逼王、两位诗人主档与资料正文
--   建议顺序：init.sql → 本脚本 → merge 脚本
--
-- 登录密码：123456（BCrypt）。脚本幂等，可重复执行。
-- 头像：森垚 avatar_cn_modern_male_02.jpg / 常平逼王 avatar_cn_modern_male_04.jpg（各一张专属水墨肖像）
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1. 账号
-- ------------------------------------------------------------
INSERT IGNORE INTO `user`
(`username`, `password`, `email`, `phone`, `nickname`, `avatar`, `gender`, `birthday`, `bio`, `interests`,
 `role`, `poet_verified`, `status`, `last_login_time`, `create_time`)
VALUES
('senyao', '$2a$10$WJj6mQAQMY9OfJdoYbefquZtWh6B0r0Dm7Wg2EY2EP4XPBLD9.8Zy',
 'senyao@moyuan.com', NULL, '森垚',
 '/img/poet_avatars/avatar_cn_modern_male_02.jpg', 1, NULL,
 '在旧词牌里写今人的心事。夜雨、旧巷、远人，都收进韵脚。',
 '词,古体诗,现代诗,书法,茶',
 'user', 1, 1, '2026-11-06 23:12:00', '2026-03-12 20:15:00'),
('biwang', '$2a$10$WJj6mQAQMY9OfJdoYbefquZtWh6B0r0Dm7Wg2EY2EP4XPBLD9.8Zy',
 'biwang@moyuan.com', NULL, '逼王',
 '/img/poet_avatars/avatar_cn_modern_male_04.jpg', 1, NULL,
 '诗不端着。格律里也能装江湖气，落笔要响。（曾用笔名：初芒）',
 '词,古体诗,现代诗,酒,江湖',
 'user', 1, 1, '2026-11-07 00:41:00', '2026-02-26 22:41:00');

UPDATE `user` SET `password` = '$2a$10$WJj6mQAQMY9OfJdoYbefquZtWh6B0r0Dm7Wg2EY2EP4XPBLD9.8Zy'
WHERE `username` IN ('senyao', 'biwang');

UPDATE `user` SET
  `nickname` = '森垚', `email` = 'senyao@moyuan.com', `phone` = NULL,
  `avatar` = '/img/poet_avatars/avatar_cn_modern_male_02.jpg', `gender` = 1, `birthday` = NULL,
  `bio` = '在旧词牌里写今人的心事。夜雨、旧巷、远人，都收进韵脚。',
  `interests` = '词,古体诗,现代诗,书法,茶',
  `poet_verified` = 1, `status` = 1
WHERE `username` = 'senyao';

UPDATE `user` SET
  `nickname` = '逼王', `email` = 'biwang@moyuan.com', `phone` = NULL,
  `avatar` = '/img/poet_avatars/avatar_cn_modern_male_04.jpg', `gender` = 1, `birthday` = NULL,
  `bio` = '诗不端着。格律里也能装江湖气，落笔要响。（曾用笔名：初芒）',
  `interests` = '词,古体诗,现代诗,酒,江湖',
  `poet_verified` = 1, `status` = 1
WHERE `username` = 'biwang';

SET @senyao_id := (SELECT `id` FROM `user` WHERE `username` = 'senyao');
SET @biwang_id := (SELECT `id` FROM `user` WHERE `username` = 'biwang');

-- ------------------------------------------------------------
-- 2. 作品数据盘子（确定式，重跑结果一致）
-- ------------------------------------------------------------
UPDATE `poem` SET
  `view_count` = 168 + (`id` * 37) % 760,
  `like_count` = 9 + (`id` * 13) % 41,
  `favorite_count` = 4 + (`id` * 7) % 21
WHERE `poet_id` = 1 AND `deleted` = 0;

-- ------------------------------------------------------------
-- 3. 认证资料（正文由 merge 脚本统一重写，这里只保证记录存在且已认证）
-- ------------------------------------------------------------
INSERT IGNORE INTO `poet_profile`
(`user_id`, `pen_name`, `real_name`, `specialty`, `contact_info`, `verified_status`, `verified_time`,
 `verified_reason`, `work_count`, `like_count`, `favorite_count`, `follower_count`, `create_time`)
VALUES
(@senyao_id, '森垚', NULL, '词,古体诗,现代诗', 'senyao@moyuan.com', 1, '2026-11-07 10:24:00',
 '作品原创性核验通过，认证为本站认证诗人。', 0, 0, 0, 1286, '2026-03-12 21:02:00'),
(@biwang_id, '常平逼王', NULL, '词,古体诗', 'biwang@moyuan.com（曾用笔名：初芒）', 1, '2026-11-07 10:31:00',
 '作品原创性核验通过，认证为本站认证诗人。', 0, 0, 0, 437, '2026-02-26 23:10:00');

UPDATE `poet_profile` SET
  `pen_name` = '森垚', `specialty` = '词,古体诗,现代诗', `contact_info` = 'senyao@moyuan.com',
  `verified_status` = 1, `verified_time` = '2026-11-07 10:24:00', `follower_count` = 1286
WHERE `user_id` = @senyao_id;

UPDATE `poet_profile` SET
  `pen_name` = '常平逼王', `specialty` = '词,古体诗',
  `contact_info` = 'biwang@moyuan.com（曾用笔名：初芒）',
  `verified_status` = 1, `verified_time` = '2026-11-07 10:31:00', `follower_count` = 437
WHERE `user_id` = @biwang_id;

-- 作品数 / 获赞 / 被收藏 与 poem 实表对齐
UPDATE `poet_profile` p SET
  p.`work_count` = (SELECT COUNT(*) FROM `poem` o WHERE o.`poet_id` = 6 AND o.`deleted` = 0 AND o.`status` = 1),
  p.`like_count` = (SELECT IFNULL(SUM(o.`like_count`), 0) FROM `poem` o WHERE o.`poet_id` = 6 AND o.`deleted` = 0),
  p.`favorite_count` = (SELECT IFNULL(SUM(o.`favorite_count`), 0) FROM `poem` o WHERE o.`poet_id` = 6 AND o.`deleted` = 0)
WHERE p.`user_id` = @senyao_id;

UPDATE `poet_profile` p SET
  p.`work_count` = (SELECT COUNT(*) FROM `poem` o WHERE o.`poet_id` = 1 AND o.`deleted` = 0 AND o.`status` = 1),
  p.`like_count` = (SELECT IFNULL(SUM(o.`like_count`), 0) FROM `poem` o WHERE o.`poet_id` = 1 AND o.`deleted` = 0),
  p.`favorite_count` = (SELECT IFNULL(SUM(o.`favorite_count`), 0) FROM `poem` o WHERE o.`poet_id` = 1 AND o.`deleted` = 0)
WHERE p.`user_id` = @biwang_id;

-- 用户 -> 认证资料 绑定
UPDATE `user` u SET
  u.`poet_verified` = 1,
  u.`poet_profile_id` = (SELECT p.`id` FROM `poet_profile` p WHERE p.`user_id` = @senyao_id)
WHERE u.`id` = @senyao_id;

UPDATE `user` u SET
  u.`poet_verified` = 1,
  u.`poet_profile_id` = (SELECT p.`id` FROM `poet_profile` p WHERE p.`user_id` = @biwang_id)
WHERE u.`id` = @biwang_id;

-- ------------------------------------------------------------
-- 4. 社区痕迹（帖子谈作品本身，不写生活细节）
-- ------------------------------------------------------------
INSERT INTO `forum_post` (`title`, `content`, `user_id`, `category`, `view_count`, `like_count`, `comment_count`, `status`, `audit_time`, `last_comment_time`, `create_time`)
SELECT '关于《八声甘州·愿缘》的第四十七稿', '这首词改了四十七遍，最后一稿只删掉一个字。\n\n删的是「又」字。原句是「又见春深」，删完变成「见春深」。少一个「又」，那种年年重复的钝痛反而出来了——因为读者自己会补上。\n\n写词这些年，越来越觉得减法比加法难。加一句是本事，删一个字是分寸。', @senyao_id, NULL, 326, 42, 1, 1, '2026-10-18 09:20:00', '2026-10-19 22:07:00', '2026-10-18 09:12:00'
WHERE NOT EXISTS (SELECT 1 FROM `forum_post` WHERE `user_id` = @senyao_id AND `title` = '关于《八声甘州·愿缘》的第四十七稿');

INSERT INTO `forum_post` (`title`, `content`, `user_id`, `category`, `view_count`, `like_count`, `comment_count`, `status`, `audit_time`, `last_comment_time`, `create_time`)
SELECT '词里的冷，是删出来的', '常有人问：写这么冷的词，是不是心里很苦。\n\n其实不是苦，是删。初稿往往写得很满，满到自己都不好意思，于是开始删——删掉解释、删掉感叹、删掉那句「我很难过」。删到最后剩下的，读者读着就冷。\n\n冷不是姿态，是减法减过头之后剩下的骨头。', @senyao_id, NULL, 214, 33, 1, 1, '2026-10-29 21:35:00', '2026-10-30 08:41:00', '2026-10-29 21:28:00'
WHERE NOT EXISTS (SELECT 1 FROM `forum_post` WHERE `user_id` = @senyao_id AND `title` = '词里的冷，是删出来的');

INSERT INTO `forum_post` (`title`, `content`, `user_id`, `category`, `view_count`, `like_count`, `comment_count`, `status`, `audit_time`, `last_comment_time`, `create_time`)
SELECT '《忆江南·哀》为什么三字一顿', '《忆江南》只有二十七字，塞不进一个完整的故事，我索性三字一顿：秋风寒／落红自凋残。\n\n顿开之后，读者得自己把每一顿接起来。最后一句「吾心不胜哀」几乎是喊出来的——我知道不像词，但那会儿不喊，前面二十几个字就白忍了。\n\n短调的好处是藏不住，每个字都得顶用。', @biwang_id, NULL, 481, 67, 1, 1, '2026-10-22 11:05:00', '2026-10-23 19:30:00', '2026-10-22 10:58:00'
WHERE NOT EXISTS (SELECT 1 FROM `forum_post` WHERE `user_id` = @biwang_id AND `title` = '《忆江南·哀》为什么三字一顿');

INSERT INTO `forum_post` (`title`, `content`, `user_id`, `category`, `view_count`, `like_count`, `comment_count`, `status`, `audit_time`, `last_comment_time`, `create_time`)
SELECT '两首《清平乐》：一个人间，一个江湖', '同一个词牌，我写过两首。《梦怀》是雨夜：「雨落窗外，淅声渐消潺」，写的是枕畔醒着的人；《情（道姑）》是江湖：「纵剑吴钩霜雪，锦夜孤灯长明」，写的是打马南屏的人。\n\n有人问哪首更像词。我觉得都像。词牌本来就是个容器，装雨夜和装刀剑，容量是一样的。\n\n只是写完《道姑》那首，我停了很久没再填《清平乐》——一个调子被两件事占满，就没地方了。', @biwang_id, NULL, 392, 58, 1, 1, '2026-11-02 20:16:00', '2026-11-03 12:44:00', '2026-11-02 20:09:00'
WHERE NOT EXISTS (SELECT 1 FROM `forum_post` WHERE `user_id` = @biwang_id AND `title` = '两首《清平乐》：一个人间，一个江湖');

SET @p1 := (SELECT `id` FROM `forum_post` WHERE `user_id` = @senyao_id AND `title` = '关于《八声甘州·愿缘》的第四十七稿' LIMIT 1);
SET @p3 := (SELECT `id` FROM `forum_post` WHERE `user_id` = @biwang_id AND `title` = '《忆江南·哀》为什么三字一顿' LIMIT 1);

SET @poem_senyao := (SELECT `id` FROM `poem` WHERE `poet_id` = 6 AND `deleted` = 0 ORDER BY `id` LIMIT 1);
SET @poem_biwang := (SELECT `id` FROM `poem` WHERE `poet_id` = 1 AND `deleted` = 0 ORDER BY `id` LIMIT 1);

INSERT INTO `comment` (`content`, `user_id`, `target_id`, `target_type`, `parent_id`, `like_count`, `status`, `create_time`)
SELECT '删「又」这个判断是对的。重复感交给读者去补，比自己喊出来狠得多。', @biwang_id, @p1, 2, 0, 5, 1, '2026-10-19 22:07:00'
WHERE @p1 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `comment` WHERE `user_id` = @biwang_id AND `target_id` = @p1 AND `target_type` = 2);

INSERT INTO `comment` (`content`, `user_id`, `target_id`, `target_type`, `parent_id`, `like_count`, `status`, `create_time`)
SELECT '三字一顿这个选择是对的。你砍掉的是解释，留下的是节奏——读者在顿号里自己补情绪，比你替他们说出来狠。', @senyao_id, @p3, 2, 0, 4, 1, '2026-10-23 19:30:00'
WHERE @p3 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `comment` WHERE `user_id` = @senyao_id AND `target_id` = @p3 AND `target_type` = 2);

INSERT INTO `comment` (`content`, `user_id`, `target_id`, `target_type`, `parent_id`, `like_count`, `status`, `create_time`)
SELECT '这首的冷是真好，但「灯」出现得太勤了，四处都在亮，反而把最该亮的那一盏淹了。删掉一处，会疼得更准。', @biwang_id, @poem_senyao, 1, 0, 7, 1, '2026-10-26 23:18:00'
WHERE @poem_senyao IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `comment` WHERE `user_id` = @biwang_id AND `target_id` = @poem_senyao AND `target_type` = 1);

INSERT INTO `comment` (`content`, `user_id`, `target_id`, `target_type`, `parent_id`, `like_count`, `status`, `create_time`)
SELECT '俗字用得准，格律也守得住，不容易。建议第三句再压一压，别急着把情绪交出来——你这首最好的地方，恰恰是没说完的那半句。', @senyao_id, @poem_biwang, 1, 0, 6, 1, '2026-10-28 00:52:00'
WHERE @poem_biwang IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `comment` WHERE `user_id` = @senyao_id AND `target_id` = @poem_biwang AND `target_type` = 1);

INSERT INTO `user_like` (`user_id`, `target_id`, `target_type`, `create_time`)
SELECT @senyao_id, @poem_biwang, 1, '2026-10-28 00:50:00'
WHERE @poem_biwang IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `user_like` WHERE `user_id` = @senyao_id AND `target_id` = @poem_biwang AND `target_type` = 1);

INSERT INTO `user_like` (`user_id`, `target_id`, `target_type`, `create_time`)
SELECT @biwang_id, @poem_senyao, 1, '2026-10-26 23:15:00'
WHERE @poem_senyao IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `user_like` WHERE `user_id` = @biwang_id AND `target_id` = @poem_senyao AND `target_type` = 1);

INSERT INTO `user_like` (`user_id`, `target_id`, `target_type`, `create_time`)
SELECT @senyao_id, @p3, 2, '2026-10-23 19:31:00'
WHERE @p3 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `user_like` WHERE `user_id` = @senyao_id AND `target_id` = @p3 AND `target_type` = 2);

INSERT INTO `user_like` (`user_id`, `target_id`, `target_type`, `create_time`)
SELECT @biwang_id, @p1, 2, '2026-10-19 22:08:00'
WHERE @p1 IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `user_like` WHERE `user_id` = @biwang_id AND `target_id` = @p1 AND `target_type` = 2);

INSERT INTO `user_favorite` (`user_id`, `target_id`, `target_type`, `create_time`)
SELECT @senyao_id, @poem_biwang, 1, '2026-10-28 00:51:00'
WHERE @poem_biwang IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `user_favorite` WHERE `user_id` = @senyao_id AND `target_id` = @poem_biwang AND `target_type` = 1);

INSERT INTO `user_favorite` (`user_id`, `target_id`, `target_type`, `create_time`)
SELECT @biwang_id, @poem_senyao, 1, '2026-10-26 23:16:00'
WHERE @poem_senyao IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `user_favorite` WHERE `user_id` = @biwang_id AND `target_id` = @poem_senyao AND `target_type` = 1);

-- ------------------------------------------------------------
-- 5. 结果
-- ------------------------------------------------------------
SELECT '账号' AS kind, u.`id` AS user_id, u.`username`, u.`nickname`, u.`poet_verified`,
       pr.`pen_name`, pr.`verified_status`, pr.`work_count`, pr.`like_count`, pr.`follower_count`
FROM `user` u LEFT JOIN `poet_profile` pr ON pr.`id` = u.`poet_profile_id`
WHERE u.`username` IN ('senyao', 'biwang');
