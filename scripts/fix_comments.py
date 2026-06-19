import pymysql

COMMENT_MAP = {
    'dynasty': {'id': '主键', 'name': '朝代名称', 'start_year': '开始年份', 'end_year': '结束年份', 'description': '朝代简介', 'sort_order': '排序顺序', 'create_time': '创建时间', 'update_time': '更新时间'},
    'poet': {'id': '主键', 'name': '诗人姓名', 'courtesy_name': '字', 'pseudonym': '号', 'dynasty_id': '朝代ID', 'birth_year': '出生年份', 'death_year': '去世年份', 'birthplace': '出生地', 'biography': '生平简介', 'life_story': '人物生平', 'influence': '主要影响', 'evaluation': '历史评价', 'anecdotes': '轶事典故', 'avatar': '头像URL', 'poet_type': '诗人类型', 'status': '状态', 'create_time': '创建时间', 'update_time': '更新时间', 'deleted': '逻辑删除'},
    'category': {'id': '主键', 'name': '分类名称', 'parent_id': '父分类ID', 'description': '分类描述', 'icon': '分类图标', 'sort_order': '排序顺序', 'status': '状态', 'create_time': '创建时间', 'update_time': '更新时间'},
    'poem': {'id': '主键', 'title': '诗词标题', 'content': '诗词内容', 'poet_id': '诗人ID', 'dynasty_id': '朝代ID', 'category_id': '分类ID', 'translation': '译文', 'appreciation': '赏析', 'background': '创作背景', 'source': '来源', 'view_count': '浏览次数', 'like_count': '点赞次数', 'favorite_count': '收藏次数', 'status': '状态', 'is_featured': '是否精选', 'is_original': '是否原创', 'poem_type': '诗词类型', 'audit_status': '审核状态', 'audit_time': '审核时间', 'audit_reason': '审核备注', 'avg_score': '平均评分', 'rating_count': '评分数量', 'create_time': '创建时间', 'update_time': '更新时间', 'deleted': '逻辑删除'},
    'user': {'id': '主键', 'username': '用户名', 'password': '密码', 'email': '邮箱', 'phone': '手机号', 'nickname': '昵称', 'avatar': '头像URL', 'gender': '性别', 'birthday': '生日', 'bio': '个人简介', 'role': '角色', 'poet_verified': '诗人认证状态', 'poet_profile_id': '诗人资料ID', 'status': '状态', 'last_login_time': '最后登录时间', 'last_login_ip': '最后登录IP', 'create_time': '创建时间', 'update_time': '更新时间', 'deleted': '逻辑删除'},
    'forum_post': {'id': '主键', 'title': '帖子标题', 'content': '帖子内容', 'user_id': '发帖用户ID', 'category': '帖子分类', 'view_count': '浏览次数', 'like_count': '点赞次数', 'comment_count': '评论数量', 'is_top': '是否置顶', 'is_featured': '是否精选', 'status': '状态', 'last_comment_time': '最后评论时间', 'create_time': '创建时间', 'update_time': '更新时间', 'deleted': '逻辑删除'},
    'comment': {'id': '主键', 'content': '评论内容', 'user_id': '评论用户ID', 'target_id': '目标ID', 'target_type': '目标类型', 'parent_id': '父评论ID', 'like_count': '点赞次数', 'status': '状态', 'create_time': '创建时间', 'update_time': '更新时间', 'deleted': '逻辑删除'},
    'user_favorite': {'id': '主键', 'user_id': '用户ID', 'target_id': '收藏目标ID', 'target_type': '收藏类型', 'create_time': '创建时间'},
    'user_like': {'id': '主键', 'user_id': '用户ID', 'target_id': '点赞目标ID', 'target_type': '点赞类型', 'create_time': '创建时间'},
    'operation_log': {'id': '主键', 'user_id': '操作用户ID', 'username': '操作用户名', 'operation': '操作类型', 'method': '请求方法', 'uri': '请求URI', 'params': '请求参数', 'result': '返回结果', 'ip': '请求IP', 'duration': '请求时长', 'status': '操作状态', 'error_msg': '错误信息', 'create_time': '创建时间'},
    'user_history': {'id': '主键', 'user_id': '用户ID', 'target_id': '浏览目标ID', 'target_type': '浏览类型', 'create_time': '创建时间'},
    'poet_featured': {'id': '主键', 'poet_id': '关联诗人ID', 'name': '诗人姓名', 'dynasty': '朝代', 'description': '简介', 'biography': '详细生平', 'image_url': '意境图URL', 'sort_order': '排序顺序', 'status': '状态', 'create_time': '创建时间', 'update_time': '更新时间', 'deleted': '逻辑删除'},
    'home_navigation': {'id': '主键', 'type': '类型', 'title': '标题', 'subtitle': '副标题', 'image_url': '图片URL', 'link_id': '关联ID', 'link_type': '链接类型', 'sort_order': '排序', 'status': '状态', 'create_time': '创建时间', 'update_time': '更新时间', 'deleted': '逻辑删除'},
    'ai_model': {'id': '主键', 'name': '模型标识', 'display_name': '显示名称', 'provider': '提供商', 'model_type': '模型类型', 'api_url': 'API地址', 'api_key': 'API密钥', 'model_id': '模型ID', 'vision_model_id': '视觉模型ID', 'max_tokens': '最大token数', 'extra_config': '额外配置', 'is_enabled': '是否启用', 'is_default': '是否默认', 'sort_order': '排序顺序', 'create_time': '创建时间', 'update_time': '更新时间'},
    'ai_module_config': {'id': '主键', 'module_code': '模块编码', 'module_name': '模块名称', 'model_id': '关联的AI模型ID', 'require_vision': '是否需要视觉能力', 'description': '模块描述', 'prompt_template': '提示词模板', 'max_response_length': '最大回答长度', 'response_style': '回答风格', 'first_response_length': '首次回答最大长度', 'enable_markdown': '是否允许Markdown', 'create_time': '创建时间', 'update_time': '更新时间'},
    'vision_article': {'id': '主键', 'title': '文章标题', 'summary': '文章摘要', 'content': '文章正文', 'cover_image': '封面图片URL', 'author': '作者', 'category': '文章分类', 'view_count': '浏览次数', 'like_count': '点赞次数', 'status': '状态', 'is_featured': '是否推荐', 'sort_order': '排序顺序', 'create_time': '创建时间', 'update_time': '更新时间', 'deleted': '逻辑删除'},
    'visit_log': {'id': '主键', 'user_id': '用户ID', 'ip': '访问IP', 'user_agent': '浏览器UA', 'path': '访问路径', 'visit_date': '访问日期', 'create_time': '创建时间'},
    'file_metadata': {'id': '主键', 'file_key': '存储路径', 'original_name': '原始文件名', 'file_type': '文件类型', 'mime_type': 'MIME类型', 'size': '文件大小', 'md5': '文件MD5', 'width': '图片宽度', 'height': '图片高度', 'related_id': '关联ID', 'related_type': '关联类型', 'storage_type': '存储类型', 'status': '状态', 'create_time': '创建时间', 'update_time': '更新时间'},
    'ai_image_record': {'id': '主键', 'session_id': '会话ID', 'user_id': '用户ID', 'model_name': '模型名称', 'prompt': '生成提示词', 'negative_prompt': '负面提示词', 'image_url': '图片URL', 'image_key': '图片存储路径', 'width': '图片宽度', 'height': '图片高度', 'seed': '随机种子', 'steps': '生成步数', 'cfg_scale': 'CFG缩放', 'generation_time': '生成耗时', 'status': '状态', 'create_time': '创建时间'},
    'poet_profile': {'id': '主键', 'user_id': '关联用户ID', 'pen_name': '笔名', 'real_name': '真实姓名', 'specialty': '擅长体裁', 'introduction': '诗人简介', 'literary_concept': '创作理念', 'achievements': '主要成就', 'contact_info': '联系方式', 'verified_status': '认证状态', 'verified_time': '认证时间', 'verified_reason': '认证审核备注', 'work_count': '作品数量', 'like_count': '获赞总数', 'favorite_count': '被收藏总数', 'follower_count': '粉丝数', 'create_time': '创建时间', 'update_time': '更新时间', 'deleted': '逻辑删除'},
    'poem_rating': {'id': '主键', 'poem_id': '诗词ID', 'user_id': '评分用户ID', 'score': '评分', 'rating_type': '评分类型', 'dimension': '评分维度', 'comment': '评分说明', 'ai_model': 'AI模型名称', 'ai_analysis': 'AI分析内容', 'create_time': '创建时间', 'update_time': '更新时间'},
    'poem_content_cache': {'id': '主键', 'poem_title': '诗词标题', 'poet_name': '诗人姓名', 'content_type': '内容类型', 'content': '缓存内容', 'source': '来源', 'create_time': '创建时间', 'update_time': '更新时间'},
    'poet_suggestion': {'id': '主键', 'poet_id': '诗人ID', 'user_id': '建议用户ID', 'section': '修改板块', 'content': '建议内容', 'status': '状态', 'review_comment': '审核备注', 'reviewer_id': '审核者ID', 'review_time': '审核时间', 'create_time': '创建时间', 'update_time': '更新时间'},
}

TABLE_COMMENT_MAP = {
    'dynasty': '朝代表', 'poet': '诗人表', 'category': '诗词分类表', 'poem': '诗词表',
    'user': '用户表', 'forum_post': '论坛帖子表', 'comment': '评论表',
    'user_favorite': '用户收藏表', 'user_like': '用户点赞表', 'operation_log': '操作日志表',
    'user_history': '用户浏览历史表', 'poet_featured': '精选诗人卡片表',
    'home_navigation': '首页导航数据表', 'ai_model': 'AI模型配置表',
    'ai_module_config': 'AI模块模型配置表', 'vision_article': '诗话视野文章表',
    'visit_log': '访问日志表', 'file_metadata': '文件元数据表',
    'ai_image_record': 'AI生成图片记录表', 'poet_profile': '认证诗人资料表',
    'poem_rating': '诗词评分表', 'poem_content_cache': '诗词内容缓存表',
    'poet_suggestion': '诗人内容建议表',
}

conn = pymysql.connect(host='localhost', port=3306, user='root', password='010125', database='moyuan', charset='utf8mb4')
cur = conn.cursor()

success_count = 0
fail_count = 0

for table_name, col_map in COMMENT_MAP.items():
    cur.execute("""
        SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, EXTRA
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'moyuan' AND TABLE_NAME = %s
        ORDER BY ORDINAL_POSITION
    """, (table_name,))
    columns = cur.fetchall()

    for col_name, col_type, is_nullable, col_default, extra in columns:
        if col_name not in col_map:
            continue

        comment = col_map[col_name]
        nullable = '' if is_nullable == 'YES' else ' NOT NULL'
        default = ''
        if col_default is not None and col_default != 'NULL':
            if col_default == 'CURRENT_TIMESTAMP' and 'DEFAULT_GENERATED' in (extra or ''):
                default = ' DEFAULT CURRENT_TIMESTAMP'
            else:
                default = f" DEFAULT '{col_default}'"
        extra_str = ''
        if extra and extra != 'DEFAULT_GENERATED' and extra != 'auto_increment':
            extra_str = f' {extra}'
        if extra == 'auto_increment':
            extra_str = ' AUTO_INCREMENT'
        elif extra and 'DEFAULT_GENERATED' in extra and 'on update' in extra:
            extra_str = ' ON UPDATE CURRENT_TIMESTAMP'

        sql = f"ALTER TABLE `{table_name}` MODIFY COLUMN `{col_name}` {col_type}{nullable}{default}{extra_str} COMMENT '{comment}'"

        try:
            cur.execute(sql)
            success_count += 1
        except Exception as e:
            print(f"FAIL: {table_name}.{col_name}: {e}")
            fail_count += 1

    if table_name in TABLE_COMMENT_MAP:
        tbl_comment = TABLE_COMMENT_MAP[table_name]
        try:
            cur.execute(f"ALTER TABLE `{table_name}` COMMENT='{tbl_comment}'")
            success_count += 1
        except Exception as e:
            print(f"FAIL: TABLE {table_name} COMMENT: {e}")
            fail_count += 1

conn.commit()
cur.close()
conn.close()

print(f"\n修复完成: 成功 {success_count} 条, 失败 {fail_count} 条")
