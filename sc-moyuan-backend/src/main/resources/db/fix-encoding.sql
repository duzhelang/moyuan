-- ============================================================
-- 修复乱码数据脚本
-- 说明：清理 vision_article、forum_post 和 comment 表中的乱码数据
--       （??? 或其他编码损坏内容，如 锟斤拷/烫烫烫/屯屯屯）
-- 使用前请先备份数据库！
-- ============================================================

USE `moyuan`;

-- 0. vision_article 乱码数据检查与清理
-- 0.1 查看乱码文章数量
SELECT COUNT(*) AS garbled_vision_count
FROM `vision_article`
WHERE `content` REGEXP '[?]{3,}' OR `title` REGEXP '[?]{3,}'
   OR `content` LIKE '%锟斤拷%' OR `content` LIKE '%烫烫烫%'
   OR `content` LIKE '%屯屯屯%';

-- 0.2 查看乱码文章详情
SELECT `id`, `title` FROM `vision_article`
WHERE `content` REGEXP '[?]{3,}' OR `title` REGEXP '[?]{3,}'
   OR `content` LIKE '%锟斤拷%' OR `content` LIKE '%烫烫烫%'
   OR `content` LIKE '%屯屯屯%';

-- 0.3 删除 vision_article 乱码数据（物理删除）
DELETE FROM `vision_article`
WHERE `content` REGEXP '[?]{3,}' OR `title` REGEXP '[?]{3,}'
   OR `content` LIKE '%锟斤拷%' OR `content` LIKE '%烫烫烫%'
   OR `content` LIKE '%屯屯屯%';

-- 1. 查看乱码帖子数量
SELECT COUNT(*) AS garbled_post_count
FROM `forum_post`
WHERE `content` REGEXP '[?]{3,}' OR `title` REGEXP '[?]{3,}'
   OR `content` LIKE '%锟斤拷%' OR `content` LIKE '%烫烫烫%'
   OR `content` LIKE '%屯屯屯%';

-- 2. 查看乱码评论数量
SELECT COUNT(*) AS garbled_comment_count
FROM `comment`
WHERE `content` REGEXP '[?]{3,}'
   OR `content` LIKE '%锟斤拷%' OR `content` LIKE '%烫烫烫%'
   OR `content` LIKE '%屯屯屯%';

-- 3. 删除乱码帖子（物理删除，因为逻辑删除可能保留垃圾数据）
DELETE FROM `forum_post`
WHERE `content` REGEXP '[?]{3,}' OR `title` REGEXP '[?]{3,}'
   OR `content` LIKE '%锟斤拷%' OR `content` LIKE '%烫烫烫%'
   OR `content` LIKE '%屯屯屯%';

-- 4. 删除乱码评论
DELETE FROM `comment`
WHERE `content` REGEXP '[?]{3,}'
   OR `content` LIKE '%锟斤拷%' OR `content` LIKE '%烫烫烫%'
   OR `content` LIKE '%屯屯屯%';

-- 5. 同时清理关联的点赞和收藏数据（可选）
DELETE FROM `user_like`
WHERE `target_type` = 2 AND `target_id` NOT IN (SELECT `id` FROM `forum_post`);

DELETE FROM `user_like`
WHERE `target_type` = 3 AND `target_id` NOT IN (SELECT `id` FROM `comment`);

DELETE FROM `user_favorite`
WHERE `target_type` = 2 AND `target_id` NOT IN (SELECT `id` FROM `forum_post`);
