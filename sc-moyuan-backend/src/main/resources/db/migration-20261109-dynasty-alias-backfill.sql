-- =============================================================================
-- 朝代名称归一化 + 外部诗词存量修复
-- 迁移日期：2026-11-09
--
-- 背景：外部收录链路用「朝代名全等匹配」，上游返回「唐代 / 宋代」而 dynasty 表存
--       「唐朝 / 宋朝」，导致外部诗 dynasty_id 全为 NULL；同时 resolvePoet 按名字
--       匹配失败时会新建诗人，产生了 15 个与正牌诗人同名的「空壳诗人」。
--
-- 口径：跨代诗人按「前一个朝代」归属
--       韦庄 / 贯休 / 齐己（唐末五代）→ 唐朝
--       丘葵 / 黄公绍（宋末元初）→ 宋朝
--       刘基 / 杨维桢（元末明初）→ 元朝
--
-- 特性：幂等，可重复执行。
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. 朝代别名表：上游写法 → dynasty.id，供 DynastyNameResolver 读取
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `dynasty_alias` (
  `id`         BIGINT       NOT NULL AUTO_INCREMENT,
  `alias`      VARCHAR(32)  NOT NULL COMMENT '上游/人工常见写法',
  `dynasty_id` BIGINT       NOT NULL COMMENT '归一到的朝代 id',
  `remark`     VARCHAR(120) DEFAULT NULL,
  `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_alias` (`alias`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='朝代名称别名（上游写法归一）';

INSERT INTO `dynasty_alias` (`alias`, `dynasty_id`, `remark`) VALUES
  ('先秦', 1, NULL), ('先秦时期', 1, NULL), ('上古', 1, NULL),
  ('春秋', 1, NULL), ('战国', 1, NULL), ('春秋战国', 1, '并入先秦'),
  ('周', 1, '并入先秦'), ('西周', 1, NULL), ('东周', 1, NULL),
  ('秦', 2, NULL), ('秦代', 2, NULL),
  ('汉', 3, NULL), ('汉代', 3, NULL), ('西汉', 3, NULL), ('东汉', 3, NULL), ('两汉', 3, NULL),
  ('三国', 4, NULL), ('晋', 4, NULL), ('西晋', 4, NULL), ('东晋', 4, NULL),
  ('魏晋', 4, NULL), ('南北朝', 4, NULL), ('魏晋南北朝', 4, NULL), ('魏晋南北朝时期', 4, NULL),
  ('隋', 5, NULL), ('隋代', 5, NULL),
  ('唐', 6, NULL), ('唐代', 6, '上游今日诗词写法'), ('初唐', 6, NULL), ('盛唐', 6, NULL),
  ('中唐', 6, NULL), ('晚唐', 6, NULL), ('唐末', 6, '跨代取前朝'),
  ('五代', 7, NULL), ('五代十国', 7, NULL), ('五代时期', 7, NULL), ('南唐', 7, NULL),
  ('宋', 8, NULL), ('宋代', 8, '上游今日诗词写法'), ('北宋', 8, NULL), ('南宋', 8, NULL), ('两宋', 8, NULL),
  ('元', 9, NULL), ('元代', 9, NULL), ('元末', 9, '跨代取前朝'),
  ('明', 10, NULL), ('明代', 10, NULL),
  ('清', 11, NULL), ('清代', 11, NULL),
  ('民国', 12, NULL), ('民国时期', 12, NULL), ('近代', 12, NULL), ('近现代', 12, '跨代取前朝'),
  ('现代', 13, NULL), ('当代', 13, NULL), ('现当代', 13, NULL)
ON DUPLICATE KEY UPDATE `dynasty_id` = VALUES(`dynasty_id`), `remark` = VALUES(`remark`);

-- 「外国」在历史库为 id 120，而 init.sql 新建库自增为 14；按名称对齐，避免硬编码
INSERT IGNORE INTO `dynasty_alias` (`alias`, `dynasty_id`)
SELECT t.`alias`, d.`id`
FROM (SELECT '外国' AS `alias` UNION ALL SELECT '海外' UNION ALL SELECT '国外') t
JOIN `dynasty` d ON d.`name` = '外国';
UPDATE `dynasty_alias` a JOIN `dynasty` d ON d.`name` = '外国'
SET a.`dynasty_id` = d.`id`
WHERE a.`alias` IN ('外国', '海外', '国外');

-- -----------------------------------------------------------------------------
-- 2. poem 增加收录备注列（记录未识别的原始朝代，便于后续补别名）
-- -----------------------------------------------------------------------------
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'poem' AND column_name = 'import_note'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `poem` ADD COLUMN `import_note` VARCHAR(255) DEFAULT NULL COMMENT ''收录备注：朝代未识别等'' AFTER `audit_reason`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- -----------------------------------------------------------------------------
-- 3. 合并同名「空壳诗人」到正牌诗人
--    条件：同名记录里恰好只有 1 条带朝代（正牌），其余朝代为空（收录时新建的空壳）。
--    佚名 / 无名氏 属匿名占位，不做合并。
-- -----------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS `tmp_poet_merge`;
CREATE TEMPORARY TABLE `tmp_poet_merge` AS
SELECT s.`id` AS shell_id, c.`id` AS canonical_id, s.`name`
FROM `poet` s
JOIN `poet` c
  ON c.`name` = s.`name` AND c.`deleted` = 0 AND c.`dynasty_id` IS NOT NULL AND c.`id` <> s.`id`
WHERE s.`deleted` = 0
  AND s.`dynasty_id` IS NULL
  AND s.`name` NOT IN ('佚名', '无名氏')
  AND (SELECT COUNT(*) FROM `poet` x
       WHERE x.`name` = s.`name` AND x.`deleted` = 0 AND x.`dynasty_id` IS NOT NULL) = 1;

SELECT * FROM `tmp_poet_merge` ORDER BY `name`;

-- 作品改挂到正牌诗人名下（poem 的 (title,poet_id) 非唯一索引，不会冲突）
UPDATE `poem` o
JOIN `tmp_poet_merge` m ON o.`poet_id` = m.`shell_id`
SET o.`poet_id` = m.`canonical_id`;

-- 其余引用表（当前均为 0 条，写全是为了以后重跑安全）
UPDATE `poet_featured` f   JOIN `tmp_poet_merge` m ON f.`poet_id`  = m.`shell_id` SET f.`poet_id`  = m.`canonical_id`;
UPDATE `poet_draft` d      JOIN `tmp_poet_merge` m ON d.`poet_id`  = m.`shell_id` SET d.`poet_id`  = m.`canonical_id`;
UPDATE `poet_suggestion` s JOIN `tmp_poet_merge` m ON s.`poet_id`  = m.`shell_id` SET s.`poet_id`  = m.`canonical_id`;
UPDATE `home_navigation` n JOIN `tmp_poet_merge` m ON n.`link_id`  = m.`shell_id` SET n.`link_id`  = m.`canonical_id`;

-- 空壳软删除（保留记录便于追溯，不再出现在任何列表）
UPDATE `poet` p
JOIN `tmp_poet_merge` m ON p.`id` = m.`shell_id`
SET p.`deleted` = 1, p.`status` = 0;

-- -----------------------------------------------------------------------------
-- 4. 剩余无朝代诗人按名字回填（此时每个名字只剩一条有效记录，不会撞唯一键）
-- -----------------------------------------------------------------------------
UPDATE `poet`
SET `dynasty_id` = CASE `name`
    WHEN '杜甫' THEN 6 WHEN '杜牧' THEN 6 WHEN '卢象' THEN 6 WHEN '司空图' THEN 6
    WHEN '杨巨源' THEN 6 WHEN '沈佺期' THEN 6 WHEN '温庭筠' THEN 6 WHEN '王昌龄' THEN 6
    WHEN '白居易' THEN 6 WHEN '韦庄' THEN 6 WHEN '贯休' THEN 6 WHEN '齐己' THEN 6
    WHEN '刘禹锡' THEN 6 WHEN '裴度' THEN 6
    WHEN '丘葵' THEN 8 WHEN '冯时行' THEN 8 WHEN '宋庠' THEN 8 WHEN '张镃' THEN 8
    WHEN '戴复古' THEN 8 WHEN '朱敦儒' THEN 8 WHEN '杨亿' THEN 8 WHEN '王安石' THEN 8
    WHEN '白玉蟾' THEN 8 WHEN '葛立方' THEN 8 WHEN '许开' THEN 8 WHEN '赵长卿' THEN 8
    WHEN '释祖先' THEN 8 WHEN '陆游' THEN 8 WHEN '陈藻' THEN 8 WHEN '高荷' THEN 8
    WHEN '高观国' THEN 8 WHEN '黄公绍' THEN 8 WHEN '黄庶' THEN 8 WHEN '苏辙' THEN 8
    WHEN '苏轼' THEN 8 WHEN '李清照' THEN 8 WHEN '辛弃疾' THEN 8
    WHEN '刘基' THEN 9 WHEN '杨维桢' THEN 9 WHEN '楚石梵琦' THEN 9
    WHEN '李东阳' THEN 10 WHEN '唐寅' THEN 10
    WHEN '乾隆' THEN 11 WHEN '李鸿章' THEN 11 WHEN '赵翼' THEN 11
    WHEN '柳亚子' THEN 12
    WHEN '陆机' THEN 4
    WHEN '耿湋' THEN 6 WHEN '畅当' THEN 6 WHEN '吴融' THEN 6
    WHEN '陈普' THEN 8 WHEN '刘克庄' THEN 8 WHEN '宋太宗' THEN 8 WHEN '范仲淹' THEN 8
    WHEN '陈深' THEN 8 WHEN '释智圆' THEN 8 WHEN '释道宁' THEN 8 WHEN '王禹偁' THEN 8
    WHEN '洪咨夔' THEN 8 WHEN '真宗' THEN 8 WHEN '黄庭坚' THEN 8 WHEN '宋祁' THEN 8
    WHEN '释正觉' THEN 8 WHEN '文天祥' THEN 8 WHEN '陆游妾' THEN 8 WHEN '陆游妾某' THEN 8
    ELSE NULL END
WHERE `dynasty_id` IS NULL
  AND `deleted` = 0
  AND `name` IN (
    '杜甫','杜牧','卢象','司空图','杨巨源','沈佺期','温庭筠','王昌龄','白居易','韦庄','贯休','齐己',
    '刘禹锡','裴度','丘葵','冯时行','宋庠','张镃','戴复古','朱敦儒','杨亿','王安石','白玉蟾','葛立方',
    '许开','赵长卿','释祖先','陆游','陈藻','高荷','高观国','黄公绍','黄庶','苏辙','苏轼','李清照',
    '辛弃疾','刘基','杨维桢','李东阳','楚石梵琦','唐寅','乾隆','李鸿章','赵翼','柳亚子','陆机',
    '耿湋','畅当','吴融','陈普','刘克庄','宋太宗','范仲淹','陈深','释智圆','释道宁','王禹偁',
    '洪咨夔','真宗','黄庭坚','宋祁','释正觉','文天祥','陆游妾','陆游妾某'
  );

-- -----------------------------------------------------------------------------
-- 5. 跨代诗人统一到「前一个朝代」（正牌记录此前被标成五代十国的，按口径改为唐朝）
--    如需恢复，改回 7（五代十国）即可。
-- -----------------------------------------------------------------------------
UPDATE `poet` SET `dynasty_id` = 6
WHERE `deleted` = 0 AND `name` IN ('韦庄', '贯休', '齐己') AND `dynasty_id` = 7;

-- 5b. 个别诗人主档已失效（佚名占位被软删）的诗，只能按作品本身判定：
--     《郊庙歌辞》属《全唐诗》乐府，直接归唐朝
UPDATE `poem` SET `dynasty_id` = 6
WHERE `deleted` = 0 AND `dynasty_id` IS NULL AND LOCATE('郊庙歌辞', `title`) > 0;

-- 5c. 指向已软删诗人的作品，改挂到同名有效诗人（按朝代对齐，避免挂到另一个同名占位）
UPDATE `poem` o
JOIN `poet` dead  ON dead.`id` = o.`poet_id` AND dead.`deleted` = 1
JOIN `poet` alive ON alive.`name` = dead.`name` AND alive.`deleted` = 0
                 AND (o.`dynasty_id` IS NULL OR alive.`dynasty_id` = o.`dynasty_id`)
SET o.`poet_id` = alive.`id`
WHERE o.`deleted` = 0;

-- -----------------------------------------------------------------------------
-- 6. 诗朝代从诗人主档继承
-- -----------------------------------------------------------------------------
UPDATE `poem` o
JOIN `poet` p ON p.`id` = o.`poet_id`
SET o.`dynasty_id` = p.`dynasty_id`
WHERE o.`dynasty_id` IS NULL
  AND p.`dynasty_id` IS NOT NULL;

-- -----------------------------------------------------------------------------
-- 7. 作者名从诗人主档回填（buildPoem 从不写 author，导致列表页作者为空）
-- -----------------------------------------------------------------------------
UPDATE `poem` o
JOIN `poet` p ON p.`id` = o.`poet_id`
SET o.`author` = p.`name`
WHERE (o.`author` IS NULL OR o.`author` = '')
  AND p.`name` IS NOT NULL AND p.`name` <> '';

-- -----------------------------------------------------------------------------
-- 8. 外部古体诗补默认分类（标题含「·」判为词 9，其余为古体诗 7）
-- -----------------------------------------------------------------------------
UPDATE `poem`
SET `category_id` = CASE WHEN LOCATE('·', `title`) > 0 THEN 9 ELSE 7 END
WHERE `source` = 'external'
  AND (`category_id` IS NULL OR `category_id` = 0);

-- -----------------------------------------------------------------------------
-- 9. 回填结果核验
-- -----------------------------------------------------------------------------
SELECT '外部诗完整性' AS item, COUNT(*) AS total,
       SUM(`dynasty_id` IS NULL) AS dyn_null,
       SUM(`author` IS NULL OR `author` = '') AS author_empty,
       SUM(`category_id` IS NULL OR `category_id` = 0) AS cat_empty
FROM `poem` WHERE `source` = 'external' AND `deleted` = 0;

SELECT '待审诗完整性' AS item, COUNT(*) AS total, SUM(`dynasty_id` IS NULL) AS dyn_null
FROM `poem` WHERE `audit_status` = 0 AND `deleted` = 0;

SELECT '剩余同名字人（未合并的匿名占位）' AS item, COUNT(*) AS c
FROM (SELECT `name` FROM `poet` WHERE `deleted` = 0 GROUP BY `name` HAVING COUNT(*) > 1) t;

SELECT '别名条数' AS item, COUNT(*) AS c FROM `dynasty_alias`;
