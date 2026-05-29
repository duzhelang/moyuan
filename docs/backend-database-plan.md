# 古今诗话——墨渊 后端与数据库建设方案

## 一、前端结构分析总结

### 1.1 现有前端技术栈

- **HTML/CSS/JS**：传统前端技术栈
- **主要页面**：
  - `index.html`：首页（轮播图、导航、推荐内容）
  - `html/denglu.html`：登录页面
  - `html/zhuce.html`：注册页面
  - `html/fenyejiagou.html`：分页架构页面（诗人列表等）

### 1.2 前端功能模块

| 模块 | 功能 | 对应后端API |
|------|------|-------------|
| 首页 | 轮播图、导航、推荐诗人/作品 | /api/poem/featured, /api/poet/list |
| 用户 | 登录、注册 | /api/auth/login, /api/auth/register |
| 诗词 | 诗词浏览、搜索 | /api/poem/list, /api/poem/search |
| 诗人 | 诗人列表、详情 | /api/poet/list, /api/poet/{id} |
| 论坛 | 发帖、评论 | /api/forum/* |
| AI | 看图写诗、智能分析 | /api/ai/* |

### 1.3 前端数据需求

- 诗人数据：姓名、朝代、简介、头像
- 诗词数据：标题、内容、作者、朝代、分类
- 用户数据：用户名、密码、邮箱、头像
- 论坛数据：帖子、评论、点赞、收藏

---

## 二、后端项目建设计划

### 2.1 项目结构

```
sc-moyuan-backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── moyuan/
│   │   │           ├── MoyuanApplication.java
│   │   │           ├── config/
│   │   │           │   ├── WebMvcConfig.java
│   │   │           │   ├── SecurityConfig.java
│   │   │           │   ├── RedisConfig.java
│   │   │           │   ├── MybatisPlusConfig.java
│   │   │           │   └── Knife4jConfig.java
│   │   │           ├── controller/
│   │   │           │   ├── AuthController.java
│   │   │           │   ├── UserController.java
│   │   │           │   ├── PoemController.java
│   │   │           │   ├── PoetController.java
│   │   │           │   ├── DynastyController.java
│   │   │           │   ├── CategoryController.java
│   │   │           │   └── ForumController.java
│   │   │           ├── service/
│   │   │           │   ├── AuthService.java
│   │   │           │   ├── UserService.java
│   │   │           │   ├── PoemService.java
│   │   │           │   ├── PoetService.java
│   │   │           │   ├── DynastyService.java
│   │   │           │   ├── CategoryService.java
│   │   │           │   ├── ForumService.java
│   │   │           │   └── impl/
│   │   │           │       ├── AuthServiceImpl.java
│   │   │           │       ├── UserServiceImpl.java
│   │   │           │       └── ...
│   │   │           ├── mapper/
│   │   │           │   ├── UserMapper.java
│   │   │           │   ├── PoemMapper.java
│   │   │           │   ├── PoetMapper.java
│   │   │           │   ├── DynastyMapper.java
│   │   │           │   ├── CategoryMapper.java
│   │   │           │   └── ForumPostMapper.java
│   │   │           ├── entity/
│   │   │           │   ├── User.java
│   │   │           │   ├── Poem.java
│   │   │           │   ├── Poet.java
│   │   │           │   ├── Dynasty.java
│   │   │           │   ├── Category.java
│   │   │           │   ├── ForumPost.java
│   │   │           │   ├── Comment.java
│   │   │           │   ├── UserFavorite.java
│   │   │           │   ├── UserLike.java
│   │   │           │   └── OperationLog.java
│   │   │           ├── dto/
│   │   │           │   ├── request/
│   │   │           │   │   ├── LoginRequest.java
│   │   │           │   │   ├── RegisterRequest.java
│   │   │           │   │   ├── PoemQueryRequest.java
│   │   │           │   │   └── ForumPostRequest.java
│   │   │           │   └── response/
│   │   │           │       ├── LoginResponse.java
│   │   │           │       ├── UserResponse.java
│   │   │           │       ├── PoemResponse.java
│   │   │           │       └── ForumPostResponse.java
│   │   │           ├── vo/
│   │   │           │   ├── Result.java
│   │   │           │   └── PageResult.java
│   │   │           ├── exception/
│   │   │           │   ├── BusinessException.java
│   │   │           │   ├── ErrorCode.java
│   │   │           │   └── GlobalExceptionHandler.java
│   │   │           ├── common/
│   │   │           │   ├── Constants.java
│   │   │           │   ├── ResultCode.java
│   │   │           │   └── PageQuery.java
│   │   │           ├── util/
│   │   │           │   ├── JwtUtil.java
│   │   │           │   ├── RedisUtil.java
│   │   │           │   ├── SecurityUtil.java
│   │   │           │   └── StringUtil.java
│   │   │           ├── security/
│   │   │           │   ├── JwtAuthenticationFilter.java
│   │   │           │   └── UserDetailsServiceImpl.java
│   │   │           ├── aspect/
│   │   │           │   ├── LogAspect.java
│   │   │           │   └── PermissionAspect.java
│   │   │           └── enums/
│   │   │               ├── UserStatus.java
│   │   │               ├── PoemStatus.java
│   │   │               └── ForumPostStatus.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       ├── mapper/
│   │       │   ├── UserMapper.xml
│   │       │   ├── PoemMapper.xml
│   │       │   └── ...
│   │       └── db/
│   │           ├── schema.sql
│   │           └── data.sql
│   └── test/
│       └── java/
│           └── com/
│               └── moyuan/
│                   ├── controller/
│                   ├── service/
│                   └── mapper/
├── pom.xml
└── README.md
```

### 2.2 核心功能模块

#### 2.2.1 认证模块 (Auth)

**功能**：
- 用户注册
- 用户登录（JWT Token）
- 用户登出
- 获取当前用户信息
- 修改密码

**API端点**：
```
POST /api/auth/register    - 用户注册
POST /api/auth/login       - 用户登录
POST /api/auth/logout      - 用户登出
GET  /api/auth/me          - 获取当前用户信息
PUT  /api/auth/password    - 修改密码
```

#### 2.2.2 用户模块 (User)

**功能**：
- 获取用户信息
- 更新个人资料
- 更新头像
- 获取收藏列表
- 获取点赞列表

**API端点**：
```
GET  /api/user/{id}        - 获取用户信息
PUT  /api/user/profile     - 更新个人资料
PUT  /api/user/avatar      - 更新头像
GET  /api/user/favorites   - 获取收藏列表
GET  /api/user/likes       - 获取点赞列表
```

#### 2.2.3 诗词模块 (Poem)

**功能**：
- 获取诗词列表（分页、筛选）
- 获取诗词详情
- 搜索诗词
- 获取精选诗词
- 点赞诗词
- 收藏诗词

**API端点**：
```
GET  /api/poem/list        - 获取诗词列表
GET  /api/poem/{id}        - 获取诗词详情
GET  /api/poem/search      - 搜索诗词
GET  /api/poem/featured    - 获取精选诗词
POST /api/poem/{id}/like   - 点赞诗词
POST /api/poem/{id}/favorite - 收藏诗词
```

#### 2.2.4 诗人模块 (Poet)

**功能**：
- 获取诗人列表
- 获取诗人详情
- 搜索诗人
- 获取诗人作品

**API端点**：
```
GET  /api/poet/list        - 获取诗人列表
GET  /api/poet/{id}        - 获取诗人详情
GET  /api/poet/search      - 搜索诗人
GET  /api/poet/{id}/poems  - 获取诗人作品
```

#### 2.2.5 朝代模块 (Dynasty)

**功能**：
- 获取朝代列表
- 获取朝代详情

**API端点**：
```
GET  /api/dynasty/list     - 获取朝代列表
GET  /api/dynasty/{id}     - 获取朝代详情
```

#### 2.2.6 分类模块 (Category)

**功能**：
- 获取分类列表
- 获取分类树

**API端点**：
```
GET  /api/category/list    - 获取分类列表
GET  /api/category/tree    - 获取分类树
```

#### 2.2.7 论坛模块 (Forum)

**功能**：
- 获取帖子列表
- 获取帖子详情
- 发布帖子
- 编辑帖子
- 删除帖子
- 点赞帖子
- 获取评论列表
- 发表评论
- 点赞评论

**API端点**：
```
GET    /api/forum/post/list           - 获取帖子列表
GET    /api/forum/post/{id}           - 获取帖子详情
POST   /api/forum/post                - 发布帖子
PUT    /api/forum/post/{id}           - 编辑帖子
DELETE /api/forum/post/{id}           - 删除帖子
POST   /api/forum/post/{id}/like      - 点赞帖子
GET    /api/forum/post/{id}/comments  - 获取评论列表
POST   /api/forum/post/{id}/comments  - 发表评论
POST   /api/forum/comment/{id}/like   - 点赞评论
```

### 2.3 技术实现要点

#### 2.3.1 认证与授权

- 使用 JWT Token 进行身份认证
- Spring Security 进行权限控制
- BCrypt 加密密码
- Token 过期时间：24小时

#### 2.3.2 数据库访问

- MyBatis-Plus 作为 ORM 框架
- 支持分页查询
- 逻辑删除
- 自动填充创建时间、更新时间

#### 2.3.3 缓存策略

- Redis 缓存热点数据
- 缓存诗人列表、朝代列表
- 缓存精选诗词
- 缓存过期时间：1小时

#### 2.3.4 接口文档

- Knife4j 生成 API 文档
- 访问地址：http://localhost:8080/doc.html

---

## 三、数据库建设计划

### 3.1 数据库概述

- **数据库类型**：MySQL 8.0
- **字符集**：utf8mb4
- **排序规则**：utf8mb4_unicode_ci
- **存储引擎**：InnoDB

### 3.2 表结构设计

#### 3.2.1 user（用户表）

```sql
CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `bio` VARCHAR(500) DEFAULT NULL COMMENT '个人简介',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  UNIQUE KEY `uk_user_email` (`email`),
  KEY `idx_user_status` (`status`),
  KEY `idx_user_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';
```

#### 3.2.2 poet（诗人表）

```sql
CREATE TABLE `poet` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(50) NOT NULL COMMENT '诗人姓名',
  `courtesy_name` VARCHAR(50) DEFAULT NULL COMMENT '字',
  `pseudonym` VARCHAR(50) DEFAULT NULL COMMENT '号',
  `dynasty_id` BIGINT NOT NULL COMMENT '朝代ID',
  `birth_year` INT DEFAULT NULL COMMENT '出生年份',
  `death_year` INT DEFAULT NULL COMMENT '去世年份',
  `birthplace` VARCHAR(100) DEFAULT NULL COMMENT '出生地',
  `biography` TEXT DEFAULT NULL COMMENT '生平简介',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_poet_dynasty_id` (`dynasty_id`),
  KEY `idx_poet_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='诗人表';
```

#### 3.2.3 dynasty（朝代表）

```sql
CREATE TABLE `dynasty` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(50) NOT NULL COMMENT '朝代名称',
  `start_year` INT DEFAULT NULL COMMENT '开始年份',
  `end_year` INT DEFAULT NULL COMMENT '结束年份',
  `description` TEXT DEFAULT NULL COMMENT '朝代简介',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dynasty_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='朝代表';
```

#### 3.2.4 category（诗词分类表）

```sql
CREATE TABLE `category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父分类ID（0=顶级）',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '分类描述',
  `icon` VARCHAR(255) DEFAULT NULL COMMENT '分类图标',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序顺序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='诗词分类表';
```

#### 3.2.5 poem（诗词表）

```sql
CREATE TABLE `poem` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` VARCHAR(100) NOT NULL COMMENT '诗词标题',
  `content` TEXT NOT NULL COMMENT '诗词内容',
  `poet_id` BIGINT DEFAULT NULL COMMENT '诗人ID',
  `dynasty_id` BIGINT DEFAULT NULL COMMENT '朝代ID',
  `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
  `translation` TEXT DEFAULT NULL COMMENT '译文',
  `appreciation` TEXT DEFAULT NULL COMMENT '赏析',
  `background` TEXT DEFAULT NULL COMMENT '创作背景',
  `source` VARCHAR(255) DEFAULT NULL COMMENT '来源',
  `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞次数',
  `favorite_count` INT NOT NULL DEFAULT 0 COMMENT '收藏次数',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-草稿，1-已发布，2-待审核',
  `is_featured` TINYINT NOT NULL DEFAULT 0 COMMENT '是否精选：0-否，1-是',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_poem_poet_id` (`poet_id`),
  KEY `idx_poem_dynasty_id` (`dynasty_id`),
  KEY `idx_poem_category_id` (`category_id`),
  KEY `idx_poem_status` (`status`),
  KEY `idx_poem_create_time` (`create_time`),
  FULLTEXT KEY `ft_poem_title_content` (`title`, `content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='诗词表';
```

#### 3.2.6 forum_post（论坛帖子表）

```sql
CREATE TABLE `forum_post` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` VARCHAR(200) NOT NULL COMMENT '帖子标题',
  `content` TEXT NOT NULL COMMENT '帖子内容',
  `user_id` BIGINT NOT NULL COMMENT '发帖用户ID',
  `category` VARCHAR(50) DEFAULT NULL COMMENT '帖子分类',
  `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞次数',
  `comment_count` INT NOT NULL DEFAULT 0 COMMENT '评论数量',
  `is_top` TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶',
  `is_featured` TINYINT NOT NULL DEFAULT 0 COMMENT '是否精选',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-草稿，1-已发布，2-已关闭',
  `last_comment_time` DATETIME DEFAULT NULL COMMENT '最后评论时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_forum_post_user_id` (`user_id`),
  KEY `idx_forum_post_category` (`category`),
  KEY `idx_forum_post_status` (`status`),
  KEY `idx_forum_post_create_time` (`create_time`),
  FULLTEXT KEY `ft_forum_post_title_content` (`title`, `content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论坛帖子表';
```

#### 3.2.7 comment（评论表）

```sql
CREATE TABLE `comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `content` TEXT NOT NULL COMMENT '评论内容',
  `user_id` BIGINT NOT NULL COMMENT '评论用户ID',
  `post_id` BIGINT NOT NULL COMMENT '帖子ID',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父评论ID（0=顶级）',
  `reply_to_user_id` BIGINT DEFAULT NULL COMMENT '回复目标用户ID',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞次数',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-隐藏，1-正常',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_comment_user_id` (`user_id`),
  KEY `idx_comment_post_id` (`post_id`),
  KEY `idx_comment_parent_id` (`parent_id`),
  KEY `idx_comment_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';
```

#### 3.2.8 user_favorite（用户收藏表）

```sql
CREATE TABLE `user_favorite` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `target_id` BIGINT NOT NULL COMMENT '收藏目标ID',
  `target_type` TINYINT NOT NULL COMMENT '收藏类型：1-诗词，2-帖子，3-诗人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_favorite` (`user_id`, `target_id`, `target_type`),
  KEY `idx_user_favorite_target` (`target_id`, `target_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收藏表';
```

#### 3.2.9 user_like（用户点赞表）

```sql
CREATE TABLE `user_like` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `target_id` BIGINT NOT NULL COMMENT '点赞目标ID',
  `target_type` TINYINT NOT NULL COMMENT '点赞类型：1-诗词，2-帖子，3-评论',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_like` (`user_id`, `target_id`, `target_type`),
  KEY `idx_user_like_target` (`target_id`, `target_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户点赞表';
```

#### 3.2.10 operation_log（操作日志表）

```sql
CREATE TABLE `operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT DEFAULT NULL COMMENT '操作用户ID',
  `username` VARCHAR(50) DEFAULT NULL COMMENT '操作用户名',
  `operation` VARCHAR(50) NOT NULL COMMENT '操作类型',
  `method` VARCHAR(200) NOT NULL COMMENT '请求方法',
  `params` TEXT DEFAULT NULL COMMENT '请求参数',
  `ip` VARCHAR(50) DEFAULT NULL COMMENT '请求IP',
  `duration` INT DEFAULT NULL COMMENT '请求时长（毫秒）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '操作状态：0-失败，1-成功',
  `error_msg` TEXT DEFAULT NULL COMMENT '错误信息',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_operation_log_user_id` (`user_id`),
  KEY `idx_operation_log_operation` (`operation`),
  KEY `idx_operation_log_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';
```

### 3.3 初始数据

#### 3.3.1 朝代初始数据

```sql
INSERT INTO `dynasty` (`name`, `start_year`, `end_year`, `description`, `sort_order`) VALUES
('先秦', NULL, 221, '先秦时期，包括夏、商、周等朝代', 1),
('秦朝', 221, 207, '秦朝，中国第一个统一的封建王朝', 2),
('汉朝', 206, 220, '汉朝，分为西汉和东汉', 3),
('魏晋南北朝', 220, 589, '魏晋南北朝时期', 4),
('隋朝', 581, 618, '隋朝，结束南北朝分裂局面', 5),
('唐朝', 618, 907, '唐朝，中国诗歌发展的鼎盛时期', 6),
('五代十国', 907, 979, '五代十国时期', 7),
('宋朝', 960, 1279, '宋朝，分为北宋和南宋', 8),
('元朝', 1271, 1368, '元朝，蒙古族建立的王朝', 9),
('明朝', 1368, 1644, '明朝，汉族建立的最后一个封建王朝', 10),
('清朝', 1644, 1912, '清朝，中国最后一个封建王朝', 11),
('民国', 1912, 1949, '中华民国时期', 12);
```

#### 3.3.2 分类初始数据

```sql
INSERT INTO `category` (`name`, `parent_id`, `description`, `sort_order`) VALUES
('古体诗', 0, '古体诗，包括四言、五言、七言等', 1),
('近体诗', 0, '近体诗，包括律诗、绝句等', 2),
('词', 0, '词，包括各种词牌', 3),
('曲', 0, '曲，包括散曲、杂剧等', 4),
('五言律诗', 2, '五言律诗', 1),
('七言律诗', 2, '七言律诗', 2),
('五言绝句', 2, '五言绝句', 3),
('七言绝句', 2, '七言绝句', 4);
```

---

## 四、实施计划

### 4.1 第一阶段：基础框架搭建

1. 创建 Spring Boot 项目
2. 配置 Maven 依赖
3. 配置数据库连接
4. 配置 Redis 连置
5. 配置 MyBatis-Plus
6. 配置 Spring Security
7. 配置 Knife4j

### 4.2 第二阶段：核心功能实现

1. 实现用户认证模块
2. 实现诗词模块
3. 实现诗人模块
4. 实现朝代模块
5. 实现分类模块

### 4.3 第三阶段：论坛功能实现

1. 实现帖子管理
2. 实现评论功能
3. 实现点赞功能
4. 实现收藏功能

### 4.4 第四阶段：优化与完善

1. 添加缓存策略
2. 优化查询性能
3. 完善日志记录
4. 编写单元测试

---

## 五、技术栈总结

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17+ | 编程语言 |
| Spring Boot | 3.x | 应用框架 |
| Spring Security | 6.x | 安全框架 |
| MyBatis-Plus | 3.5.x | ORM框架 |
| MySQL | 8.0 | 关系型数据库 |
| Redis | 7.x | 缓存数据库 |
| JWT | 0.12.x | 身份认证 |
| Knife4j | 4.x | API文档 |
| Lombok | 1.18.x | 代码简化 |
| Maven | 3.8+ | 项目构建 |

---

**文档版本**：v1.0  
**最后更新**：2026-05-25  
**维护人员**：墨渊开发团队