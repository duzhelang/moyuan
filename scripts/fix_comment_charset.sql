-- ============================================================
-- 动态修复所有表 COMMENT 乱码（存储过程方式）
-- ============================================================

SET NAMES utf8mb4;
USE `moyuan`;

-- 创建临时表存储 COMMENT 映射
DROP TEMPORARY TABLE IF EXISTS _comment_fix;
CREATE TEMPORARY TABLE _comment_fix (
  tbl VARCHAR(64),
  col VARCHAR(64),
  cmt VARCHAR(500),
  PRIMARY KEY (tbl, col)
);

-- dynasty
INSERT INTO _comment_fix VALUES ('dynasty','id','主键'),('dynasty','name','朝代名称'),('dynasty','start_year','开始年份'),('dynasty','end_year','结束年份'),('dynasty','description','朝代简介'),('dynasty','sort_order','排序顺序'),('dynasty','create_time','创建时间'),('dynasty','update_time','更新时间');
-- poet
INSERT INTO _comment_fix VALUES ('poet','id','主键'),('poet','name','诗人姓名'),('poet','courtesy_name','字'),('poet','pseudonym','号'),('poet','dynasty_id','朝代ID'),('poet','birth_year','出生年份'),('poet','death_year','去世年份'),('poet','birthplace','出生地'),('poet','biography','生平简介'),('poet','life_story','人物生平'),('poet','influence','主要影响'),('poet','evaluation','历史评价'),('poet','anecdotes','轶事典故'),('poet','avatar','头像URL'),('poet','poet_type','诗人类型'),('poet','status','状态'),('poet','create_time','创建时间'),('poet','update_time','更新时间'),('poet','deleted','逻辑删除');
-- category
INSERT INTO _comment_fix VALUES ('category','id','主键'),('category','name','分类名称'),('category','parent_id','父分类ID'),('category','description','分类描述'),('category','icon','分类图标'),('category','sort_order','排序顺序'),('category','status','状态'),('category','create_time','创建时间'),('category','update_time','更新时间');
-- poem
INSERT INTO _comment_fix VALUES ('poem','id','主键'),('poem','title','诗词标题'),('poem','content','诗词内容'),('poem','poet_id','诗人ID'),('poem','dynasty_id','朝代ID'),('poem','category_id','分类ID'),('poem','translation','译文'),('poem','appreciation','赏析'),('poem','background','创作背景'),('poem','source','来源'),('poem','view_count','浏览次数'),('poem','like_count','点赞次数'),('poem','favorite_count','收藏次数'),('poem','status','状态'),('poem','is_featured','是否精选'),('poem','is_original','是否原创'),('poem','poem_type','诗词类型'),('poem','audit_status','审核状态'),('poem','audit_time','审核时间'),('poem','audit_reason','审核备注'),('poem','avg_score','平均评分'),('poem','rating_count','评分数量'),('poem','create_time','创建时间'),('poem','update_time','更新时间'),('poem','deleted','逻辑删除');
-- user
INSERT INTO _comment_fix VALUES ('user','id','主键'),('user','username','用户名'),('user','password','密码'),('user','email','邮箱'),('user','phone','手机号'),('user','nickname','昵称'),('user','avatar','头像URL'),('user','gender','性别'),('user','birthday','生日'),('user','bio','个人简介'),('user','role','角色'),('user','poet_verified','诗人认证状态'),('user','poet_profile_id','诗人资料ID'),('user','status','状态'),('user','last_login_time','最后登录时间'),('user','last_login_ip','最后登录IP'),('user','create_time','创建时间'),('user','update_time','更新时间'),('user','deleted','逻辑删除');
-- forum_post
INSERT INTO _comment_fix VALUES ('forum_post','id','主键'),('forum_post','title','帖子标题'),('forum_post','content','帖子内容'),('forum_post','user_id','发帖用户ID'),('forum_post','category','帖子分类'),('forum_post','view_count','浏览次数'),('forum_post','like_count','点赞次数'),('forum_post','comment_count','评论数量'),('forum_post','is_top','是否置顶'),('forum_post','is_featured','是否精选'),('forum_post','status','状态'),('forum_post','last_comment_time','最后评论时间'),('forum_post','create_time','创建时间'),('forum_post','update_time','更新时间'),('forum_post','deleted','逻辑删除');
-- comment
INSERT INTO _comment_fix VALUES ('comment','id','主键'),('comment','content','评论内容'),('comment','user_id','评论用户ID'),('comment','target_id','目标ID'),('comment','target_type','目标类型'),('comment','parent_id','父评论ID'),('comment','like_count','点赞次数'),('comment','status','状态'),('comment','create_time','创建时间'),('comment','update_time','更新时间'),('comment','deleted','逻辑删除');
-- user_favorite
INSERT INTO _comment_fix VALUES ('user_favorite','id','主键'),('user_favorite','user_id','用户ID'),('user_favorite','target_id','收藏目标ID'),('user_favorite','target_type','收藏类型'),('user_favorite','create_time','创建时间');
-- user_like
INSERT INTO _comment_fix VALUES ('user_like','id','主键'),('user_like','user_id','用户ID'),('user_like','target_id','点赞目标ID'),('user_like','target_type','点赞类型'),('user_like','create_time','创建时间');
-- operation_log
INSERT INTO _comment_fix VALUES ('operation_log','id','主键'),('operation_log','user_id','操作用户ID'),('operation_log','username','操作用户名'),('operation_log','operation','操作类型'),('operation_log','method','请求方法'),('operation_log','uri','请求URI'),('operation_log','params','请求参数'),('operation_log','result','返回结果'),('operation_log','ip','请求IP'),('operation_log','duration','请求时长'),('operation_log','status','操作状态'),('operation_log','error_msg','错误信息'),('operation_log','create_time','创建时间');
-- user_history
INSERT INTO _comment_fix VALUES ('user_history','id','主键'),('user_history','user_id','用户ID'),('user_history','target_id','浏览目标ID'),('user_history','target_type','浏览类型'),('user_history','create_time','创建时间');
-- poet_featured
INSERT INTO _comment_fix VALUES ('poet_featured','id','主键'),('poet_featured','poet_id','关联诗人ID'),('poet_featured','name','诗人姓名'),('poet_featured','dynasty','朝代'),('poet_featured','description','简介'),('poet_featured','biography','详细生平'),('poet_featured','image_url','意境图URL'),('poet_featured','sort_order','排序顺序'),('poet_featured','status','状态'),('poet_featured','create_time','创建时间'),('poet_featured','update_time','更新时间'),('poet_featured','deleted','逻辑删除');
-- home_navigation
INSERT INTO _comment_fix VALUES ('home_navigation','id','主键'),('home_navigation','type','类型'),('home_navigation','title','标题'),('home_navigation','subtitle','副标题'),('home_navigation','image_url','图片URL'),('home_navigation','link_id','关联ID'),('home_navigation','link_type','链接类型'),('home_navigation','sort_order','排序'),('home_navigation','status','状态'),('home_navigation','create_time','创建时间'),('home_navigation','update_time','更新时间'),('home_navigation','deleted','逻辑删除');
-- ai_model
INSERT INTO _comment_fix VALUES ('ai_model','id','主键'),('ai_model','name','模型标识'),('ai_model','display_name','显示名称'),('ai_model','provider','提供商'),('ai_model','model_type','模型类型'),('ai_model','api_url','API地址'),('ai_model','api_key','API密钥'),('ai_model','model_id','模型ID'),('ai_model','vision_model_id','视觉模型ID'),('ai_model','max_tokens','最大token数'),('ai_model','extra_config','额外配置'),('ai_model','is_enabled','是否启用'),('ai_model','is_default','是否默认'),('ai_model','sort_order','排序顺序'),('ai_model','create_time','创建时间'),('ai_model','update_time','更新时间');
-- ai_module_config
INSERT INTO _comment_fix VALUES ('ai_module_config','id','主键'),('ai_module_config','module_code','模块编码'),('ai_module_config','module_name','模块名称'),('ai_module_config','model_id','关联的AI模型ID'),('ai_module_config','require_vision','是否需要视觉能力'),('ai_module_config','description','模块描述'),('ai_module_config','prompt_template','提示词模板'),('ai_module_config','max_response_length','最大回答长度'),('ai_module_config','response_style','回答风格'),('ai_module_config','first_response_length','首次回答最大长度'),('ai_module_config','enable_markdown','是否允许Markdown'),('ai_module_config','create_time','创建时间'),('ai_module_config','update_time','更新时间');
-- vision_article
INSERT INTO _comment_fix VALUES ('vision_article','id','主键'),('vision_article','title','文章标题'),('vision_article','summary','文章摘要'),('vision_article','content','文章正文'),('vision_article','cover_image','封面图片URL'),('vision_article','author','作者'),('vision_article','category','文章分类'),('vision_article','view_count','浏览次数'),('vision_article','like_count','点赞次数'),('vision_article','status','状态'),('vision_article','is_featured','是否推荐'),('vision_article','sort_order','排序顺序'),('vision_article','create_time','创建时间'),('vision_article','update_time','更新时间'),('vision_article','deleted','逻辑删除');
-- visit_log
INSERT INTO _comment_fix VALUES ('visit_log','id','主键'),('visit_log','user_id','用户ID'),('visit_log','ip','访问IP'),('visit_log','user_agent','浏览器UA'),('visit_log','path','访问路径'),('visit_log','visit_date','访问日期'),('visit_log','create_time','创建时间');
-- file_metadata
INSERT INTO _comment_fix VALUES ('file_metadata','id','主键'),('file_metadata','file_key','存储路径'),('file_metadata','original_name','原始文件名'),('file_metadata','file_type','文件类型'),('file_metadata','mime_type','MIME类型'),('file_metadata','size','文件大小'),('file_metadata','md5','文件MD5'),('file_metadata','width','图片宽度'),('file_metadata','height','图片高度'),('file_metadata','related_id','关联ID'),('file_metadata','related_type','关联类型'),('file_metadata','storage_type','存储类型'),('file_metadata','status','状态'),('file_metadata','create_time','创建时间'),('file_metadata','update_time','更新时间');
-- ai_image_record
INSERT INTO _comment_fix VALUES ('ai_image_record','id','主键'),('ai_image_record','session_id','会话ID'),('ai_image_record','user_id','用户ID'),('ai_image_record','model_name','模型名称'),('ai_image_record','prompt','生成提示词'),('ai_image_record','negative_prompt','负面提示词'),('ai_image_record','image_url','图片URL'),('ai_image_record','image_key','图片存储路径'),('ai_image_record','width','图片宽度'),('ai_image_record','height','图片高度'),('ai_image_record','seed','随机种子'),('ai_image_record','steps','生成步数'),('ai_image_record','cfg_scale','CFG缩放'),('ai_image_record','generation_time','生成耗时'),('ai_image_record','status','状态'),('ai_image_record','create_time','创建时间');
-- poet_profile
INSERT INTO _comment_fix VALUES ('poet_profile','id','主键'),('poet_profile','user_id','关联用户ID'),('poet_profile','pen_name','笔名'),('poet_profile','real_name','真实姓名'),('poet_profile','specialty','擅长体裁'),('poet_profile','introduction','诗人简介'),('poet_profile','literary_concept','创作理念'),('poet_profile','achievements','主要成就'),('poet_profile','contact_info','联系方式'),('poet_profile','verified_status','认证状态'),('poet_profile','verified_time','认证时间'),('poet_profile','verified_reason','认证审核备注'),('poet_profile','work_count','作品数量'),('poet_profile','like_count','获赞总数'),('poet_profile','favorite_count','被收藏总数'),('poet_profile','follower_count','粉丝数'),('poet_profile','create_time','创建时间'),('poet_profile','update_time','更新时间'),('poet_profile','deleted','逻辑删除');
-- poem_rating
INSERT INTO _comment_fix VALUES ('poem_rating','id','主键'),('poem_rating','poem_id','诗词ID'),('poem_rating','user_id','评分用户ID'),('poem_rating','score','评分'),('poem_rating','rating_type','评分类型'),('poem_rating','dimension','评分维度'),('poem_rating','comment','评分说明'),('poem_rating','ai_model','AI模型名称'),('poem_rating','ai_analysis','AI分析内容'),('poem_rating','create_time','创建时间'),('poem_rating','update_time','更新时间');
-- poem_content_cache
INSERT INTO _comment_fix VALUES ('poem_content_cache','id','主键'),('poem_content_cache','poem_title','诗词标题'),('poem_content_cache','poet_name','诗人姓名'),('poem_content_cache','content_type','内容类型'),('poem_content_cache','content','缓存内容'),('poem_content_cache','source','来源'),('poem_content_cache','create_time','创建时间'),('poem_content_cache','update_time','更新时间');
-- poet_suggestion
INSERT INTO _comment_fix VALUES ('poet_suggestion','id','主键'),('poet_suggestion','poet_id','诗人ID'),('poet_suggestion','user_id','建议用户ID'),('poet_suggestion','section','修改板块'),('poet_suggestion','content','建议内容'),('poet_suggestion','status','状态'),('poet_suggestion','review_comment','审核备注'),('poet_suggestion','reviewer_id','审核者ID'),('poet_suggestion','review_time','审核时间'),('poet_suggestion','create_time','创建时间'),('poet_suggestion','update_time','更新时间');

-- ============================================================
-- 创建存储过程，动态生成并执行 ALTER TABLE 语句
-- ============================================================

DROP PROCEDURE IF EXISTS _fix_comments;

DELIMITER //

CREATE PROCEDURE _fix_comments()
BEGIN
  DECLARE done INT DEFAULT FALSE;
  DECLARE v_table VARCHAR(64);
  DECLARE v_column VARCHAR(64);
  DECLARE v_column_type VARCHAR(256);
  DECLARE v_is_nullable VARCHAR(3);
  DECLARE v_column_default VARCHAR(512);
  DECLARE v_extra VARCHAR(256);
  DECLARE v_comment VARCHAR(500);
  DECLARE v_sql TEXT;

  DECLARE cur CURSOR FOR
    SELECT c.TABLE_NAME, c.COLUMN_NAME, c.COLUMN_TYPE, c.IS_NULLABLE,
           c.COLUMN_DEFAULT, c.EXTRA, f.cmt
    FROM information_schema.COLUMNS c
    JOIN _comment_fix f ON c.TABLE_SCHEMA = DATABASE()
      AND c.TABLE_NAME = f.tbl
      AND c.COLUMN_NAME = f.col
    ORDER BY c.TABLE_NAME, c.ORDINAL_POSITION;

  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

  OPEN cur;

  read_loop: LOOP
    FETCH cur INTO v_table, v_column, v_column_type, v_is_nullable, v_column_default, v_extra, v_comment;
    IF done THEN
      LEAVE read_loop;
    END IF;

    SET @v_sql = CONCAT(
      'ALTER TABLE `', v_table, '` MODIFY COLUMN `', v_column, '` ',
      v_column_type,
      IF(v_is_nullable = 'YES', '', ' NOT NULL'),
      IF(v_column_default IS NOT NULL, CONCAT(' DEFAULT ', v_column_default), ''),
      IF(v_extra != '', CONCAT(' ', v_extra), ''),
      ' COMMENT ''', REPLACE(v_comment, '''', ''''''), ''''
    );

    PREPARE stmt FROM @v_sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END LOOP;

  CLOSE cur;

  -- 修复表级 COMMENT
  BEGIN
    DECLARE done2 INT DEFAULT FALSE;
    DECLARE v_tbl VARCHAR(64);
    DECLARE cur2 CURSOR FOR
      SELECT DISTINCT f.tbl FROM _comment_fix f
      WHERE f.tbl IN (SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE());
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done2 = TRUE;

    OPEN cur2;
    tbl_loop: LOOP
      FETCH cur2 INTO v_tbl;
      IF done2 THEN LEAVE tbl_loop; END IF;

      -- 每个表用第一条记录的 cmt 来确定表名映射（这里用中文表名）
      -- 由于表名本身就是英文，我们只需要设置 COMMENT
      -- 通过 tbl 列名反推表的中文 COMMENT
      CASE v_tbl
        WHEN 'dynasty' THEN SET @tc = '朝代表';
        WHEN 'poet' THEN SET @tc = '诗人表';
        WHEN 'category' THEN SET @tc = '诗词分类表';
        WHEN 'poem' THEN SET @tc = '诗词表';
        WHEN 'user' THEN SET @tc = '用户表';
        WHEN 'forum_post' THEN SET @tc = '论坛帖子表';
        WHEN 'comment' THEN SET @tc = '评论表';
        WHEN 'user_favorite' THEN SET @tc = '用户收藏表';
        WHEN 'user_like' THEN SET @tc = '用户点赞表';
        WHEN 'operation_log' THEN SET @tc = '操作日志表';
        WHEN 'user_history' THEN SET @tc = '用户浏览历史表';
        WHEN 'poet_featured' THEN SET @tc = '精选诗人卡片表';
        WHEN 'home_navigation' THEN SET @tc = '首页导航数据表';
        WHEN 'ai_model' THEN SET @tc = 'AI模型配置表';
        WHEN 'ai_module_config' THEN SET @tc = 'AI模块模型配置表';
        WHEN 'vision_article' THEN SET @tc = '诗话视野文章表';
        WHEN 'visit_log' THEN SET @tc = '访问日志表';
        WHEN 'file_metadata' THEN SET @tc = '文件元数据表';
        WHEN 'ai_image_record' THEN SET @tc = 'AI生成图片记录表';
        WHEN 'poet_profile' THEN SET @tc = '认证诗人资料表';
        WHEN 'poem_rating' THEN SET @tc = '诗词评分表';
        WHEN 'poem_content_cache' THEN SET @tc = '诗词内容缓存表';
        WHEN 'poet_suggestion' THEN SET @tc = '诗人内容建议表';
        ELSE SET @tc = NULL;
      END CASE;

      IF @tc IS NOT NULL THEN
        SET @v_sql = CONCAT('ALTER TABLE `', v_tbl, '` COMMENT=''', @tc, '''');
        PREPARE stmt FROM @v_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
      END IF;
    END LOOP;
    CLOSE cur2;
  END;

  SELECT 'COMMENT 修复完成' AS result;
END //

DELIMITER ;

-- 执行修复
CALL _fix_comments();

-- 清理
DROP PROCEDURE _fix_comments;
DROP TEMPORARY TABLE _comment_fix;
