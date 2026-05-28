-- 创建数据库
CREATE DATABASE IF NOT EXISTS moyuan DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE moyuan;

-- 创建朝代表
CREATE TABLE IF NOT EXISTS dynasty (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '朝代名称',
    start_year INT COMMENT '开始年份',
    end_year INT COMMENT '结束年份',
    description TEXT COMMENT '朝代简介',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='朝代表';

-- 创建诗人表
CREATE TABLE IF NOT EXISTS poet (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '诗人姓名',
    courtesy_name VARCHAR(50) COMMENT '字',
    pseudonym VARCHAR(50) COMMENT '号',
    dynasty_id BIGINT NOT NULL COMMENT '朝代ID',
    birth_year INT COMMENT '出生年份',
    death_year INT COMMENT '去世年份',
    birthplace VARCHAR(100) COMMENT '出生地',
    biography TEXT COMMENT '生平简介',
    avatar VARCHAR(255) COMMENT '头像URL',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_poet_dynasty_id (dynasty_id),
    INDEX idx_poet_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='诗人表';

-- 创建诗词分类表
CREATE TABLE IF NOT EXISTS category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID',
    description VARCHAR(255) COMMENT '分类描述',
    icon VARCHAR(255) COMMENT '分类图标',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_category_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='诗词分类表';

-- 创建诗词表
CREATE TABLE IF NOT EXISTS poem (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL COMMENT '诗词标题',
    content TEXT NOT NULL COMMENT '诗词内容',
    poet_id BIGINT COMMENT '诗人ID',
    dynasty_id BIGINT COMMENT '朝代ID',
    category_id BIGINT COMMENT '分类ID',
    translation TEXT COMMENT '译文',
    appreciation TEXT COMMENT '赏析',
    background TEXT COMMENT '创作背景',
    source VARCHAR(255) COMMENT '来源',
    view_count INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
    like_count INT NOT NULL DEFAULT 0 COMMENT '点赞次数',
    favorite_count INT NOT NULL DEFAULT 0 COMMENT '收藏次数',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-草稿，1-已发布，2-待审核',
    is_featured TINYINT NOT NULL DEFAULT 0 COMMENT '是否精选：0-否，1-是',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_poem_poet_id (poet_id),
    INDEX idx_poem_dynasty_id (dynasty_id),
    INDEX idx_poem_category_id (category_id),
    INDEX idx_poem_status (status),
    INDEX idx_poem_create_time (create_time),
    FULLTEXT KEY ft_poem_title_content (title, content)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='诗词表';

-- 插入朝代数据
INSERT INTO dynasty (name, start_year, end_year, description, sort_order) VALUES
('先秦', -2100, -221, '先秦时期', 1),
('秦朝', -221, -206, '秦朝时期', 2),
('汉朝', -206, 220, '汉朝时期', 3),
('魏晋南北朝', 220, 589, '魏晋南北朝时期', 4),
('隋朝', 581, 618, '隋朝时期', 5),
('唐朝', 618, 907, '唐朝时期', 6),
('五代十国', 907, 979, '五代十国时期', 7),
('宋朝', 960, 1279, '宋朝时期', 8),
('元朝',1271, 1368, '元朝时期', 9),
('明朝', 1368, 1644, '明朝时期', 10),
('清朝', 1644, 1912, '清朝时期', 11),
('民国', 1912, 1949, '民国时期', 12),
('现代', 1949, NULL, '现代时期', 13);

-- 插入现代诗人数据
INSERT INTO poet (name, dynasty_id, biography, status) VALUES
('常平逼王', 13, '现代诗人，作品多表现个人情感与生活感悟', 1);

-- 插入诗词分类数据
INSERT INTO category (name, parent_id, description, sort_order) VALUES
('古体诗词', 0, '古代诗词', 1),
('现代诗歌', 0, '现代诗歌', 2),
('外国诗歌', 0, '外国诗歌', 3),
('当代青年', 0, '当代青年作品', 4),
('人生派', 2, '现代诗歌流派', 1),
('七月派', 2, '现代诗歌流派', 2),
('朦胧派', 2, '现代诗歌流派', 3),
('现实主义诗派', 2, '现代诗歌流派', 4),
('新时代派', 2, '现代诗歌流派', 5),
('流萤诗派', 2, '现代诗歌流派', 6);

-- 插入现代诗词数据
INSERT INTO poem (title, content, poet_id, dynasty_id, category_id, source, status, is_featured) VALUES
('愿如花已落千行', '愿如花已落千行，未闻花语浅别殇。\n幽僻心境漫过少，唯有诗语解锁缰。\n金甲未脱抬眼望，怒剑难收向疆场。\n笑祝功成人与事，再鼓一旬又何妨。', 1, 13, 2, '常平逼王', 1, 1),
('西江月.未语', '华灯初照白路，夜来树影凄楚，俳徊只与月相伴，茫然一片迷雾。\n离人兀自心扬，未语难避哀伤，只若通明过后，真心但化霓裳。', 1, 13, 2, '常平逼王', 1, 1),
('忆江南.哀', '秋风寒，\n落红自凋残。\n未识伊人愁几许，\n伊人兀自笑颜开。\n吾心不胜哀！', 1, 13, 2, '常平逼王', 1, 1),
('东风吹起白蝴蝶', '东风吹起白蝴蝶，散入云幕尽青烟。\n但觉离魂归来少，春日暖暖艳阳天。\n松柏长青久可立，香印再燃意更坚。\n思情尽付潇湘处，绵远流长年复年。', 1, 13, 2, '常平逼王', 1, 1),
('清平乐.梦怀', '雨落窗外，淅声渐消潺，一帘凝愁入梦来，若雾浸湿枕畔。\n经年离愁之初，再会此情更苦，最是万千惆怅，与白共登天目!', 1, 13, 2, '常平逼王', 1, 1),
('乌夜啼.叹城府', '淡雾兀遮明眸，更添愁，独倚玉阑不觉泪空流。\n人情恶，谁易错，自长酌，一心明月向沟成污浊。', 1, 13, 2, '常平逼王', 1, 1),
('新桃映红把福迎', '新桃映红把福迎，银烛高照万家兴。\n火尾连排上清夜，笑语欢声动耳惊。\n奈何遣词功夫浅，此夜此景休论明。\n唯愿持酒歌一曲，与君同乐一身轻', 1, 13, 2, '常平逼王', 1, 1),
('回首一九已成空', '回首一九已成空，展望二零佳意浓。\n世事岂敢轻年少，天意哪可断前程。\n唐猊又着战意显，三尺重出徙侣从。\n烂柯沉木枉悲夫，夫且若怡也可容。', 1, 13, 2, '常平逼王', 1, 1),
('沉影迷叠千层障', '沉影迷叠千层障，乱云归处是它乡。\n酒酣仍识昔日客，心迷难辨眼前芳。\n纵把凡锦比仙缎，不需经年多思量。\n一笑即随羊角去，九风还作万华芳。', 1, 13, 2, '常平逼王', 1, 1),
('一剪梅.无题', '一别三秋未招摇，\n山也迢迢，水也昭昭。\n何人再添新衣袍，\n笑意盈绕，喜上眉梢。\n忆昔花开岁月好，\n蜂字飘摇，蝶字舞蹈。\n而今方知云未晓，\n风又飘飘，雨又萧萧。', 1, 13, 2, '常平逼王', 1, 1),
('清平乐.情(道姑)', '幽情迷朦，三年怀一梦，一遭尘世皆为恒，无怨无悔无憎。\n来日打马南屏，忆昔紫夜流萤，纵剑吴钩霜雪，锦夜孤灯长明。', 1, 13, 4, '初芒', 1, 1);
