-- 测试数据

-- 插入朝代数据
INSERT INTO dynasty (name, start_year, end_year, description, sort_order) VALUES
('唐朝', 618, 907, '唐朝是中国历史上最重要的朝代之一', 1),
('宋朝', 960, 1279, '宋朝是中国历史上经济文化最繁荣的朝代', 2),
('明朝', 1368, 1644, '明朝是中国历史上最后一个汉族建立的朝代', 3);

-- 插入诗人数据
INSERT INTO poet (name, courtesy_name, pseudonym, dynasty_id, birth_year, death_year, birthplace, biography) VALUES
('李白', '太白', '青莲居士', 1, 701, 762, '碎叶城', '唐代伟大的浪漫主义诗人'),
('杜甫', '子美', '少陵野老', 1, 712, 770, '巩县', '唐代伟大的现实主义诗人'),
('苏轼', '子瞻', '东坡居士', 2, 1037, 1101, '眉山', '北宋中期文坛领袖'),
('辛弃疾', '幼安', '稼轩', 2, 1140, 1207, '历城', '南宋豪放派词人');

-- 插入分类数据
INSERT INTO category (id, name, parent_id, description, sort_order) VALUES
(1, '中国古典诗词', 0, '包含古体、近体、词、曲等传统诗歌形式', 1),
(2, '中国现当代诗歌', 0, '1919年五四运动至今的中国诗歌作品', 2),
(3, '外国诗歌', 0, '世界各国优秀诗歌作品', 3),
(8, '近体诗', 1, '唐代形成的格律诗，包括律诗、绝句', 2),
(9, '词', 1, '又称长短句、诗余，配合音乐歌唱的诗歌形式', 3),
(11, '五言律诗', 8, '每句五字，共八句，讲究平仄对仗', 1),
(12, '七言律诗', 8, '每句七字，共八句，格律严谨', 2),
(13, '五言绝句', 8, '每句五字，共四句，短小精悍', 3);

-- 插入诗词数据
INSERT INTO poem (title, content, dynasty_id, poet_id, category_id, translation, appreciation) VALUES
('静夜思', '床前明月光，疑是地上霜。\n举头望明月，低头思故乡。', 1, 1, 13, '明亮的月光洒在床前的窗户纸上，好像地上泛起了一层霜。我禁不住抬起头来，看那天窗外空中的一轮明月，不由得低头沉思，想起远方的家乡。', '这首诗写的是在寂静的月夜思念家乡的感受。'),
('春望', '国破山河在，城春草木深。\n感时花溅泪，恨别鸟惊心。\n烽火连三月，家书抵万金。\n白头搔更短，浑欲不胜簪。', 1, 2, 11, '国家破碎但山河依旧，春天来到城池里草木繁茂。感伤时局看到花开也不禁流泪，怨恨离别听到鸟叫也令人心惊。', '这首诗反映了诗人热爱祖国、眷恋家人的感情。'),
('水调歌头·明月几时有', '明月几时有？把酒问青天。\n不知天上宫阙，今夕是何年。\n我欲乘风归去，又恐琼楼玉宇，高处不胜寒。\n起舞弄清影，何似在人间。\n\n转朱阁，低绮户，照无眠。\n不应有恨，何事长向别时圆？\n人有悲欢离合，月有阴晴圆缺，此事古难全。\n但愿人长久，千里共婵娟。', 2, 3, 9, '明月从什么时候开始有的呢？我拿着酒杯遥问苍天。', '这首词是公元1076年中秋作者在密州时所作。');

-- 插入用户数据（密码是加密后的123456）
INSERT INTO user (username, password, nickname, email, role) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 'admin@moyuan.com', 'ADMIN'),
('testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '测试用户', 'test@moyuan.com', 'USER');

-- 插入首页导航数据
INSERT INTO home_navigation (title, icon, link, sort_order) VALUES
('诗词欣赏', 'icon-poem', '/poem', 1),
('诗人介绍', 'icon-poet', '/poet', 2),
('诗汇论坛', 'icon-forum', '/forum', 3),
('视界文章', 'icon-vision', '/vision', 4);

-- 插入AI模型配置
INSERT INTO ai_model (name, provider, model_id, description, is_default) VALUES
('GPT-3.5 Turbo', 'OpenAI', 'gpt-3.5-turbo', 'OpenAI GPT-3.5 Turbo模型', 1),
('GPT-4', 'OpenAI', 'gpt-4', 'OpenAI GPT-4模型', 0);