-- ============================================================
-- 「诗人内容审核」演示数据（草稿审核 + 用户建议）
-- 关联诗人：森垚 id=6 / 常平逼王 id=1 / 九刀 id=502
-- 关联账号：senyao 260 / biwang 261 / jiudao 266 / admin 1
-- 幂等：以（poet_id, section, 提交人）为判重键，可重复执行
-- ============================================================

SET NAMES utf8mb4;

SET @senyao_id  := (SELECT `id` FROM `user` WHERE `username` = 'senyao');
SET @biwang_id  := (SELECT `id` FROM `user` WHERE `username` = 'biwang');
SET @jiudao_id  := (SELECT `id` FROM `user` WHERE `username` = 'jiudao');

-- ------------------------------------------------------------
-- 1. 草稿审核（poet_draft）：status 0-待审核 / 1-已通过 / 2-已拒绝
-- ------------------------------------------------------------

-- 1.1 森垚 修订「常平逼王」的主要影响（待审核）
INSERT INTO `poet_draft` (`poet_id`, `section`, `content`, `original_content`, `editor_id`, `status`, `ip`, `create_time`)
SELECT 1, 'influence',
 '常平逼王的写作把「说」看得比「写」重。《忆江南·哀》五句三顿，《乌夜啼·叹城府》一路直下，都不肯给情绪留缓冲。这种直给在当代旧体写作里少见——多数人把情绪藏在典故后面，他反过来把典故撤成背景，让情绪站到最前面。十一首作品累计获赞三百余次，说明直白也有它的读者。',
 '常平逼王在站内的位置有些特别：他的词不追典雅，句子常常短到近乎断裂，却在断裂处把情绪压得很实。这类写法在当代旧体写作里不多见——多数作者更愿意把情绪藏在典故背后。他的选择相反：典故退到背景，情绪站到前面。',
 @senyao_id, 0, '127.0.0.1', '2026-11-05 21:18:00'
WHERE NOT EXISTS (SELECT 1 FROM `poet_draft` WHERE `poet_id` = 1 AND `section` = 'influence' AND `editor_id` = @senyao_id);

-- 1.2 逼王 修订「九刀」的人物生平（已通过）
INSERT INTO `poet_draft` (`poet_id`, `section`, `content`, `original_content`, `editor_id`, `status`, `reviewer_id`, `review_comment`, `review_time`, `ip`, `create_time`)
SELECT 502, 'life_story',
 '《如风的微笑》十二行不分节，标点只剩两处省略号。前六行写「追」：视线从「痴情」到「有限」，一路摸索到「时光里并没有你的身影」，回头只剩一个笑。中段转入夜与模仿，「故意」二字点破——连难过都是自己演的，因为那个人已经不在场。末三行以「倘若……」断开，收在「许多情事只是时间撒下的谎言」，把私人的事交给时间，诗就到此为止。',
 '《如风的微笑》共十二行，从头到尾没有标点，唯一的停顿是两处省略号：「倘若……」与结尾的「……」。开头两句就把关系写定了。',
 @biwang_id, 1, 1, '补写与原文一致，且把「故意」一处点出来了，已并入正文。', '2026-11-04 10:22:00', '127.0.0.1', '2026-11-04 09:41:00'
WHERE NOT EXISTS (SELECT 1 FROM `poet_draft` WHERE `poet_id` = 502 AND `section` = 'life_story' AND `editor_id` = @biwang_id);

-- 1.3 九刀 修订「森垚」的轶事典故（已拒绝）
INSERT INTO `poet_draft` (`poet_id`, `section`, `content`, `original_content`, `editor_id`, `status`, `reviewer_id`, `review_comment`, `review_time`, `ip`, `create_time`)
SELECT 6, 'anecdotes',
 '· 《八声甘州》四首里，「愿缘」最见铺陈，「生」最见收束。\n· 他改词常在凌晨三点，故有「三更」之说。',
 '· 《八声甘州》他写了四首：愿缘、泪、乱绪、生。同一个词牌反复用，等于把同一副骨架拆开重搭四遍。',
 @jiudao_id, 2, 1, '「三更」一说没有出处；前半条与本页已有内容重复，建议合并进第一条后再提。', '2026-11-03 15:06:00', '127.0.0.1', '2026-11-03 14:12:00'
WHERE NOT EXISTS (SELECT 1 FROM `poet_draft` WHERE `poet_id` = 6 AND `section` = 'anecdotes' AND `editor_id` = @jiudao_id);

-- ------------------------------------------------------------
-- 2. 用户建议（poet_suggestion）：status pending / approved / rejected
-- ------------------------------------------------------------

-- 2.1 森垚 给「九刀」的简介提建议（待审核）
INSERT INTO `poet_suggestion` (`poet_id`, `user_id`, `section`, `content`, `category`, `status`, `ip`, `create_time`)
SELECT 502, @senyao_id, 'introduction',
 '建议在简介里点明「全诗不分节、只用两处省略号」这个形式特征，比泛说「语言克制」更准——这首的特点首先是分行与停顿，其次才是用词。',
 'biography', 'pending', '127.0.0.1', '2026-11-05 22:40:00'
WHERE NOT EXISTS (SELECT 1 FROM `poet_suggestion` WHERE `poet_id` = 502 AND `section` = 'introduction' AND `user_id` = @senyao_id);

-- 2.2 逼王 给「森垚」的历史评价提建议（已通过）
INSERT INTO `poet_suggestion` (`poet_id`, `user_id`, `section`, `content`, `category`, `status`, `reviewer_id`, `review_comment`, `review_time`, `ip`, `create_time`)
SELECT 6, @biwang_id, 'evaluation',
 '「有时再往前半步，情绪本可以更亮」这句评得准，但建议补一个反例说明「往前半步」是哪一首——《我希望》几乎是白话，正好与《苏幕遮·逝》的严守句读形成对照。',
 'evaluation', 'approved', 1, '采纳，已作为对照例子并入评价段落。', '2026-11-02 11:30:00', '127.0.0.1', '2026-11-02 10:55:00'
WHERE NOT EXISTS (SELECT 1 FROM `poet_suggestion` WHERE `poet_id` = 6 AND `section` = 'evaluation' AND `user_id` = @biwang_id);

-- 2.3 九刀 给「常平逼王」的轶事典故提建议（已拒绝）
INSERT INTO `poet_suggestion` (`poet_id`, `user_id`, `section`, `content`, `category`, `status`, `reviewer_id`, `review_comment`, `review_time`, `ip`, `create_time`)
SELECT 1, @jiudao_id, 'anecdotes',
 '轶事里可以加一条：两首《清平乐》同一词牌，一写雨夜、一写江湖，是他自己说的「一个调子装两件事」。',
 'anecdotes', 'rejected', 1, '与已有「一个人间一个江湖」一条内容重复，暂不重复收录。', '2026-11-01 09:20:00', '127.0.0.1', '2026-11-01 08:47:00'
WHERE NOT EXISTS (SELECT 1 FROM `poet_suggestion` WHERE `poet_id` = 1 AND `section` = 'anecdotes' AND `user_id` = @jiudao_id);

-- ------------------------------------------------------------
-- 3. 结果
-- ------------------------------------------------------------
SELECT '草稿' AS kind, `id`, `poet_id`, `section`, `status`, `editor_id`,
       LEFT(`content`, 30) AS summary, `create_time`
FROM `poet_draft` ORDER BY `id`;

SELECT '建议' AS kind, `id`, `poet_id`, `section`, `status`, `user_id`,
       LEFT(`content`, 30) AS summary, `create_time`
FROM `poet_suggestion` ORDER BY `id`;
