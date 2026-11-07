-- ============================================================
-- 新增当代诗人「九刀」：账号 + 认证 + 现代诗人库条目 + 唯一存作《如风的微笑》
-- 关联：user(jiudao) → poet_profile → poet(id 新建, poet_type='modern')
-- 位置：现代诗人库（dynasty_id=13 / poet_type=modern / 分类=青年原创现代诗 47）
-- 登录密码：123456（BCrypt）。脚本幂等，可重复执行。
-- ============================================================

SET NAMES utf8mb4;

-- ------------------------------------------------------------
-- 1. 账号
-- ------------------------------------------------------------
INSERT IGNORE INTO `user`
(`username`, `password`, `email`, `nickname`, `avatar`, `gender`, `bio`, `interests`,
 `role`, `poet_verified`, `status`, `create_time`)
VALUES
('jiudao', '$2a$10$WJj6mQAQMY9OfJdoYbefquZtWh6B0r0Dm7Wg2EY2EP4XPBLD9.8Zy',
 'jiudao@moyuan.com', '九刀',
 '/img/poet_avatars/avatar_cn_modern_male_03.jpg', 1,
 '只写过一首诗，写完就没再写。「许多情事只是时间撒下的谎言。」',
 '现代诗,短句,旧事',
 'user', 1, 1, '2026-04-18 21:06:00');

UPDATE `user` SET
  `password` = '$2a$10$WJj6mQAQMY9OfJdoYbefquZtWh6B0r0Dm7Wg2EY2EP4XPBLD9.8Zy',
  `nickname` = '九刀', `email` = 'jiudao@moyuan.com',
  `avatar` = '/img/poet_avatars/avatar_cn_modern_male_03.jpg', `gender` = 1,
  `bio` = '只写过一首诗，写完就没再写。「许多情事只是时间撒下的谎言。」',
  `interests` = '现代诗,短句,旧事',
  `poet_verified` = 1, `status` = 1
WHERE `username` = 'jiudao';

-- 另两个账号：清掉早前编造的手机号与生日，保持「不虚构真人身份」的口径
UPDATE `user` SET `phone` = NULL, `birthday` = NULL WHERE `username` IN ('senyao', 'biwang');

SET @jiudao_id := (SELECT `id` FROM `user` WHERE `username` = 'jiudao');

-- ------------------------------------------------------------
-- 2. 现代诗人库条目（poet）
-- ------------------------------------------------------------
INSERT INTO `poet`
(`name`, `dynasty_id`, `avatar`, `biography`, `life_story`, `influence`, `evaluation`, `anecdotes`,
 `poet_type`, `style`, `status`)
SELECT '九刀', 13, '/img/poet_avatars/avatar_cn_modern_male_03.jpg',
 '九刀，当代诗歌作者，本站「现代创作」栏目存作一首《如风的微笑》。全诗不分节、不用标点，只在两处落下省略号；以「视线」为线索，把一次目光的偏移写成一场告别。写作数量极少，仅此一首。',
 '《如风的微笑》共十二行，从头到尾没有标点，唯一的停顿是两处省略号：「倘若……」与结尾的「……」。\n\n开头两句就把关系写定了。「凉风卷起你的衣袖却留下你的微笑」，风是动的，衣袖是被人带走的，留下的是笑；紧接着「你的身影从我痴情的视线中出了轨」——「出轨」本是现代词汇，落到这里成了目光的偏移。它比「离开」更狠：不是消失，是滑走。\n\n第三句到第六句是「追」。微笑「调皮地拨动我有限的视线」，于是「沿着爱上你那一刻的视线努力摸索前进」，结果是「时光里并没有你的身影」；等回头，才「发现只剩下如风的微笑」。追了一路，追到的只是当初那个笑。\n\n第七、八句转入夜与模仿：「多少个魂牵梦绕的凉风夜／只能一次又一次故意模仿彷徨在那一刻」。这里的「故意」是关键——连难过都是自己演的，因为那个人已经不在场。\n\n最后三句把整首诗从私人经验里提出来：「只恨这时间匆匆地走却留不住你的脚步／倘若……／许多情事只是时间撒下的谎言／……」。一个「倘若」没有下文，结尾用一个省略号收住，等于把话交还给读者。\n\n全诗最见功夫的是「视线」的复用：开头它是痴情的、有限的，中段它是路径，最后它什么也没找到。',
 '单篇作者在诗库里是个特殊的存在：他没有序列，只有一首，读者只能整体地读它，而不能比较它的前后。这首诗在栏目里的意义也在这里——它示范了一种写法：把一个意思写到尽头，然后停。',
 '可取处集中在两句之间：「视线中出了轨」的现代感，与「许多情事只是时间撒下的谎言」的抽离。前者让抒情有了尖锐度，后者让一首情诗没有停在情诗上。\n\n不足也明显：句式偏长、偏散，「只能一次又一次故意模仿彷徨在那一刻」这类句子节奏拖沓；「倘若……」之后没有接住，是留白，也是断裂。',
 '· 全诗十二行，只有两处标点，都是省略号：「倘若……」与结尾的「……」。停顿的位置比字更重要。\n· 「视线」一词在诗中出现了四次，从「痴情的视线」到「有限的视线」，最后什么也没找到——它的变化就是全诗的结构。\n· 「出了轨」是全诗唯一的现代词汇，与「衣袖」「凉风」这类旧意象并置，反差正是这首诗的底色。',
 'modern', '自由体抒情', 1
WHERE NOT EXISTS (SELECT 1 FROM `poet` WHERE `name` = '九刀' AND `deleted` = 0);

SET @jiudao_poet := (SELECT `id` FROM `poet` WHERE `name` = '九刀' AND `deleted` = 0 ORDER BY `id` LIMIT 1);

-- ------------------------------------------------------------
-- 3. 唯一存作《如风的微笑》
-- ------------------------------------------------------------
INSERT INTO `poem`
(`title`, `content`, `author`, `poet_id`, `dynasty_id`, `category_id`, `appreciation`,
 `source`, `view_count`, `like_count`, `favorite_count`, `status`, `is_original`, `poem_type`,
 `audit_status`, `audit_time`, `is_featured`, `create_time`)
SELECT '如风的微笑',
 '凉风卷起你的衣袖却留下你的微笑\n你的身影从我痴情的视线中出了轨\n微笑却总是调皮地拨动我有限的视线\n我沿着爱上你那一刻的视线努力摸索前进\n却发现时光里并没有你的身影\n当我反回的时候才发现只剩下如风的微笑\n多少个魂牵梦绕的凉风夜\n只能一次又一次故意模仿彷徨在那一刻\n只恨这时间匆匆地走却留不住你的脚步\n倘若……\n许多情事只是时间撒下的谎言\n……',
 '九刀', @jiudao_poet, 13, 47,
 '全诗以「视线」为线索：先是「痴情的视线」，再是「有限的视线」，最后「沿着视线摸索前进」，追到头只剩一个笑。作者不写离别，只写目光追不上人；不写遗憾，只用两个省略号把话咽回去。「许多情事只是时间撒下的谎言」一句把私人的失落抬到时间层面，是全诗的转轴——从这一句起，诗不再只关于一个人。',
 '九刀', 412, 26, 7, 1, 1, 'modern', 1, '2026-04-18 22:40:00', 0, '2026-04-18 22:40:00'
WHERE NOT EXISTS (SELECT 1 FROM `poem` WHERE `title` = '如风的微笑' AND `deleted` = 0);

SET @jiudao_poem := (SELECT `id` FROM `poem` WHERE `title` = '如风的微笑' AND `deleted` = 0 AND `poet_id` = @jiudao_poet ORDER BY `id` LIMIT 1);

-- ------------------------------------------------------------
-- 4. 认证资料（poet_profile）
-- ------------------------------------------------------------
INSERT IGNORE INTO `poet_profile`
(`user_id`, `pen_name`, `real_name`, `specialty`, `introduction`, `literary_concept`, `achievements`,
 `contact_info`, `verified_status`, `verified_time`, `verified_reason`,
 `work_count`, `like_count`, `favorite_count`, `follower_count`, `create_time`)
VALUES
(@jiudao_id, '九刀', NULL, '现代诗',
 '九刀，当代诗歌作者，代表作《如风的微笑》。写作不分节、少用标点，偏爱长句推进与短句收束；题材集中在「目送」与「追不上」这一类经验上。现存作一首。',
 '一首诗只要能站住一个句子，就不算白写。我不急着写第二首——第一首的意思还没用完。',
 '· 本站现代创作栏目存作《如风的微笑》一首\n· 全诗十二行、仅两处省略号，以「视线」四次复用构成结构，是栏目内少见的「一首即止」式写作\n· 结尾「许多情事只是时间撒下的谎言」被站内读者多次引用',
 'jiudao@moyuan.com', 1, '2026-11-07 11:02:00',
 '作品原创性核验：作者提交的手稿与线上发布内容一致，分行与用词习惯连贯，确认为本人原创，认证为本站认证诗人。',
 1, 26, 7, 37, '2026-04-18 21:12:00');

UPDATE `poet_profile` SET
  `pen_name` = '九刀', `real_name` = NULL, `specialty` = '现代诗',
  `introduction` = '九刀，当代诗歌作者，代表作《如风的微笑》。写作不分节、少用标点，偏爱长句推进与短句收束；题材集中在「目送」与「追不上」这一类经验上。现存作一首。',
  `literary_concept` = '一首诗只要能站住一个句子，就不算白写。我不急着写第二首——第一首的意思还没用完。',
  `achievements` = '· 本站现代创作栏目存作《如风的微笑》一首\n· 全诗十二行、仅两处省略号，以「视线」四次复用构成结构，是栏目内少见的「一首即止」式写作\n· 结尾「许多情事只是时间撒下的谎言」被站内读者多次引用',
  `contact_info` = 'jiudao@moyuan.com',
  `verified_status` = 1, `verified_time` = '2026-11-07 11:02:00',
  `verified_reason` = '作品原创性核验：作者提交的手稿与线上发布内容一致，分行与用词习惯连贯，确认为本人原创，认证为本站认证诗人。',
  `follower_count` = 37
WHERE `user_id` = @jiudao_id;

-- 统计与 poem 实表对齐
UPDATE `poet_profile` p SET
  p.`work_count` = (SELECT COUNT(*) FROM `poem` o WHERE o.`poet_id` = @jiudao_poet AND o.`deleted` = 0 AND o.`status` = 1),
  p.`like_count` = (SELECT IFNULL(SUM(o.`like_count`), 0) FROM `poem` o WHERE o.`poet_id` = @jiudao_poet AND o.`deleted` = 0),
  p.`favorite_count` = (SELECT IFNULL(SUM(o.`favorite_count`), 0) FROM `poem` o WHERE o.`poet_id` = @jiudao_poet AND o.`deleted` = 0)
WHERE p.`user_id` = @jiudao_id;

-- 账号 -> 认证资料 绑定
UPDATE `user` u SET
  u.`poet_verified` = 1,
  u.`poet_profile_id` = (SELECT p.`id` FROM `poet_profile` p WHERE p.`user_id` = @jiudao_id)
WHERE u.`id` = @jiudao_id;

-- ------------------------------------------------------------
-- 5. 社区痕迹（1 帖 + 1 评论 + 2 赞 1 收藏）
-- ------------------------------------------------------------
INSERT INTO `forum_post` (`title`, `content`, `user_id`, `category`, `view_count`, `like_count`, `comment_count`, `status`, `audit_time`, `last_comment_time`, `create_time`)
SELECT '为什么只写这一首', '有人问我为什么只有一首。\n\n不是没得写，是这首诗的意思还没用完。它写了十二行，从「凉风卷起你的衣袖」一直到「许多情事只是时间撒下的谎言」，该说的都说完了；再写第二首，无非把同一个意思换个说法。\n\n我宁可让它只有一个版本。', @jiudao_id, NULL, 186, 24, 1, 1, '2026-04-19 10:12:00', '2026-04-20 09:30:00', '2026-04-19 10:05:00'
WHERE NOT EXISTS (SELECT 1 FROM `forum_post` WHERE `user_id` = @jiudao_id AND `title` = '为什么只写这一首');

SET @jiudao_post := (SELECT `id` FROM `forum_post` WHERE `user_id` = @jiudao_id AND `title` = '为什么只写这一首' LIMIT 1);
SET @senyao_id := (SELECT `id` FROM `user` WHERE `username` = 'senyao');
SET @biwang_id := (SELECT `id` FROM `user` WHERE `username` = 'biwang');

INSERT INTO `comment` (`content`, `user_id`, `target_id`, `target_type`, `parent_id`, `like_count`, `status`, `create_time`)
SELECT '十二行里只留两处省略号，这很奢侈——多数人会把「倘若」后面填满。你留住了那口气。', @senyao_id, @jiudao_poem, 1, 0, 8, 1, '2026-04-21 23:40:00'
WHERE @jiudao_poem IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `comment` WHERE `user_id` = @senyao_id AND `target_id` = @jiudao_poem AND `target_type` = 1);

INSERT INTO `user_like` (`user_id`, `target_id`, `target_type`, `create_time`)
SELECT @senyao_id, @jiudao_poem, 1, '2026-04-21 23:38:00'
WHERE @jiudao_poem IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `user_like` WHERE `user_id` = @senyao_id AND `target_id` = @jiudao_poem AND `target_type` = 1);

INSERT INTO `user_like` (`user_id`, `target_id`, `target_type`, `create_time`)
SELECT @biwang_id, @jiudao_post, 2, '2026-04-20 09:30:00'
WHERE @jiudao_post IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `user_like` WHERE `user_id` = @biwang_id AND `target_id` = @jiudao_post AND `target_type` = 2);

INSERT INTO `user_favorite` (`user_id`, `target_id`, `target_type`, `create_time`)
SELECT @senyao_id, @jiudao_poem, 1, '2026-04-21 23:39:00'
WHERE @jiudao_poem IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `user_favorite` WHERE `user_id` = @senyao_id AND `target_id` = @jiudao_poem AND `target_type` = 1);

-- ------------------------------------------------------------
-- 6. 结果
-- ------------------------------------------------------------
SELECT '账号' AS kind, u.`id` AS user_id, u.`username`, u.`nickname`, u.`poet_verified`,
       pr.`pen_name`, pr.`verified_status`, pr.`work_count`, pr.`like_count`, pr.`follower_count`
FROM `user` u LEFT JOIN `poet_profile` pr ON pr.`id` = u.`poet_profile_id`
WHERE u.`username` = 'jiudao';

SELECT '诗人' AS kind, p.`id`, p.`name`, p.`poet_type`, p.`style`, p.`deleted`,
       (SELECT COUNT(*) FROM `poem` o WHERE o.`poet_id` = p.`id` AND o.`deleted` = 0) AS works
FROM `poet` p WHERE p.`name` = '九刀';

SELECT '作品' AS kind, o.`id`, o.`title`, o.`author`, o.`poet_id`, o.`category_id`,
       o.`poem_type`, o.`is_original`, o.`status`, CHAR_LENGTH(o.`content`) AS chars,
       (CHAR_LENGTH(o.`content`) - CHAR_LENGTH(REPLACE(o.`content`, CHAR(10), '')) + 1) AS line_cnt
FROM `poem` o WHERE o.`title` = '如风的微笑' AND o.`deleted` = 0;
