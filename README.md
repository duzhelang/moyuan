# 古今诗话——墨渊

> 中国古典诗词文化交流平台 | Vue 3 + Spring Boot 3 + MySQL 8.0

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.oracle.com/java/)
[![Vue](https://img.shields.io/badge/Vue-3.x-brightgreen.svg)](https://vuejs.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)

## 项目简介

**古今诗话——墨渊**是一个集诗词欣赏、诗人介绍、论坛互动、AI辅助创作于一体的综合性中国古典诗词文化平台。项目采用前后端分离架构，前端使用 Vue 3 + TypeScript，后端使用 Spring Boot 3，数据库采用 MySQL 8.0 + Redis 7.x。

### 核心价值

- **文化传承**：提供现代化的诗词学习与交流体验
- **技术先进**：采用最新的前后端技术栈
- **AI赋能**：集成多种AI大模型，支持智能问答和看图写诗
- **社区互动**：论坛系统支持用户交流讨论

## 技术栈

### 前端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue.js | 3.4+ | 前端框架 |
| TypeScript | 5.x | 类型系统 |
| Vite | 5.x | 构建工具 |
| Pinia | 2.x | 状态管理 |
| Vue Router | 4.x | 路由管理 |
| Element Plus | 2.x | UI组件库 |
| Axios | 1.x | HTTP客户端 |
| SCSS | - | CSS预处理器 |
| Vitest | - | 单元测试框架 |

### 后端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.2.5 | 应用框架 |
| Spring Security | 6.x | 安全框架 |
| MyBatis-Plus | 3.5.5 | ORM框架 |
| MySQL | 8.0+ | 关系型数据库 |
| Redis | 7.x | 缓存数据库 |
| JWT (jjwt) | 0.12.5 | 身份认证 |
| Knife4j | 4.3.0 | API文档 |
| Druid | 1.2.20 | 数据库连接池 |
| Lombok | 1.18.30 | 代码简化 |
| Maven | 3.8+ | 项目构建 |

### 开发工具

| 工具 | 用途 |
|------|------|
| Git | 版本控制 |
| Docker | 容器化部署 |
| Nginx | 反向代理 |
| IntelliJ IDEA | 后端IDE |
| VS Code | 前端IDE |

## 系统架构

### 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                      用户层 (User Layer)                      │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐    │
│  │  PC浏览器 │  │ 移动浏览器│  │   小程序  │  │  APP端   │    │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    前端层 (Frontend Layer)                    │
│  ┌─────────────────────────────────────────────────────┐    │
│  │                  Vue 3 + Vite                        │    │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐│    │
│  │  │ 首页模块 │  │ 诗词模块 │  │ 论坛模块 │  │ 用户模块 ││    │
│  │  └─────────┘  └─────────┘  └─────────┘  └─────────┘│    │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐│    │
│  │  │ 管理模块 │  │AI模块   │  │搜索模块 │  │文件模块 ││    │
│  │  └─────────┘  └─────────┘  └─────────┘  └─────────┘│    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    后端层 (Backend Layer)                     │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              Spring Boot 3.x                         │    │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐│    │
│  │  │用户服务 │  │诗词服务 │  │论坛服务 │  │管理服务 ││    │
│  │  └─────────┘  └─────────┘  └─────────┘  └─────────┘│    │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐│    │
│  │  │AI服务   │  │搜索服务 │  │文件服务 │  │历史服务 ││    │
│  │  └─────────┘  └─────────┘  └─────────┘  └─────────┘│    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   数据层 (Data Layer)                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │   MySQL     │  │   Redis     │  │  文件存储   │         │
│  │  主数据库   │  │   缓存层    │  │ (图片/音频) │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
```

### 数据流架构

```
用户请求 → Nginx(反向代理) → Vue前端 → API请求 → Spring Boot后端 → MySQL/Redis → 返回数据 → 前端渲染
```

### 认证流程

```
用户登录 → 后端验证 → 生成JWT → 返回Token → 前端存储 → 后续请求携带Token → 后端验证Token
```

## 功能特性

### 核心功能模块

| 模块 | 功能 | 状态 |
|------|------|------|
| **首页模块** | 轮播图展示、导航菜单、诗人推荐、作品展示、分类精赏 | ✅ 已实现 |
| **诗词模块** | 诗词浏览、按朝代/流派/诗人分类、诗词详情、注释赏析 | ✅ 已实现 |
| **诗人模块** | 诗人列表、诗人详情、诗人推荐（协同过滤算法） | ✅ 已实现 |
| **论坛模块** | 用户发帖、评论互动、精选推荐、投稿上传 | ✅ 已实现 |
| **用户模块** | 用户注册、登录认证、个人中心、收藏管理 | ✅ 已实现 |
| **管理模块** | 系统管理、用户管理、内容管理、数据统计、操作日志 | ✅ 已实现 |
| **AI模块** | AI诗词问答（多模型切换）、看图写诗、智能分析 | ✅ 已实现 |
| **搜索模块** | 全文搜索、诗人搜索、诗词搜索、帖子搜索 | ✅ 已实现 |
| **文件模块** | 文件上传、图片管理 | ✅ 已实现 |
| **浏览历史模块** | 记录用户浏览历史、查看历史记录 | ✅ 已实现 |
| **诗话视野模块** | 诗词文化文章浏览、详情展示、点赞 | ✅ 已实现 |
| **首页导航模块** | 首页导航菜单管理（作品/流派/朝代） | ✅ 已实现 |
| **精选诗人模块** | 精选诗人展示和管理 | ✅ 已实现 |

### AI功能特性

- **多模型支持**：智谱AI、DeepSeek、Kimi、NVIDIA NIM
- **智能问答**：诗词相关问题解答
- **看图写诗**：基于图片生成诗词（视觉模型）
- **智能分析**：诗词内容分析解读
- **模型管理**：管理员可配置和切换AI模型

### 推荐算法

- **基于用户的协同过滤**（User-Based CF）
- 综合收藏、点赞、浏览历史三种行为数据
- 收藏权重 5、点赞权重 3、浏览权重 1
- 使用余弦相似度计算用户相似性
- 新用户降级为热门诗人推荐

## 快速开始

### 环境要求

| 环境 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 17+ | 推荐 Java 17/21 LTS |
| Node.js | 18+ | LTS版本 |
| MySQL | 8.0+ | 主数据库 |
| Redis | 7.x | 缓存数据库 |
| Maven | 3.8+ | 项目构建 |
| pnpm | 最新稳定版 | 推荐pnpm |

### 1. 克隆项目

```bash
git clone https://github.com/your-username/SC_MoYuan2.git
cd SC_MoYuan2
```

### 2. 数据库初始化

```bash
# 进入后端目录
cd sc-moyuan-backend

# 执行初始化脚本（自动创建数据库、建表、导入初始数据）
mysql -u root -p < src/main/resources/db/init.sql
```

> **说明**：`init.sql` 是全量合并版脚本（v3.0），包含数据库创建、全部28张表建表语句、初始数据（13个朝代、8个分类、1个诗人、11首诗词、2个测试用户、4个AI模型、6位精选诗人、24条首页导航）。

### 3. 后端配置

编辑 `sc-moyuan-backend/src/main/resources/application.yml`，修改数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/moyuan?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: your_username
    password: your_password
  
  redis:
    host: localhost
    port: 6379
```

### 4. 启动后端服务

```bash
# 方式一：使用Maven启动
cd sc-moyuan-backend
mvn spring-boot:run

# 方式二：使用启动脚本（Windows）
start.bat
```

后端服务启动后，访问 http://localhost:8081/doc.html 查看API文档（Knife4j）。

### 5. 启动前端服务

```bash
# 进入前端目录
cd frontend

# 安装依赖
pnpm install

# 启动开发服务器
pnpm dev
```

前端服务启动后，访问 http://localhost:5173 查看效果。

### 6. 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 测试用户 | testuser | test123 |

## 项目结构

```
SC_MoYuan2_/
├── frontend/                    # Vue 3 前端项目
│   ├── src/
│   │   ├── api/                 # API接口定义
│   │   ├── assets/              # 静态资源
│   │   ├── components/          # 公共组件
│   │   │   ├── business/        # 业务组件
│   │   │   ├── common/          # 通用组件
│   │   │   └── home/            # 首页子组件（HomeCarousel/ForumPreview等）
│   │   ├── composables/         # 组合式函数
│   │   ├── data/                # 静态数据（JSON）
│   │   ├── layouts/             # 布局组件
│   │   ├── router/              # 路由配置
│   │   ├── stores/              # 状态管理
│   │   ├── types/               # 类型定义
│   │   ├── utils/               # 工具函数
│   │   └── views/               # 页面组件
│   ├── public/                  # 公共资源
│   ├── package.json             # 前端依赖配置
│   ├── tsconfig.json            # TypeScript配置
│   ├── vite.config.ts           # Vite配置
│   └── README.md                # 前端说明文档
├── sc-moyuan-backend/           # Spring Boot 后端项目
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/moyuan/
│   │   │   │   ├── config/      # 配置类
│   │   │   │   ├── controller/  # 控制器层
│   │   │   │   ├── service/     # 服务层
│   │   │   │   ├── mapper/      # Mapper层
│   │   │   │   ├── entity/      # 实体类
│   │   │   │   ├── dto/         # 数据传输对象
│   │   │   │   ├── security/    # 安全相关
│   │   │   │   └── util/        # 工具类
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/init.sql  # 数据库初始化脚本
│   │   └── test/                # 测试代码
│   └── pom.xml                  # Maven配置
├── docs/                        # 项目文档
│   ├── architecture/            # 架构设计
│   ├── standards/               # 技术规范
│   ├── constraints/             # 约束条件
│   ├── database/                # 数据库文档
│   ├── api/                     # API文档
│   ├── business/                # 业务文档
│   └── guides/                  # 开发指南
├── scripts/                     # 脚本文件
├── .gitignore                   # Git忽略文件
├── pom.xml                      # Maven父项目配置
├── start.bat                    # 启动脚本
├── init-db.bat                  # 数据库初始化脚本
└── README.md                    # 本文件
```

## API文档

### API端点概览

| 模块 | 路径前缀 | 说明 |
|------|----------|------|
| 认证模块 | `/api/auth` | 用户注册、登录 |
| 用户模块 | `/api/users` | 用户信息管理 |
| 诗词模块 | `/api/poems` | 诗词CRUD、搜索、点赞、收藏 |
| 诗人模块 | `/api/poets` | 诗人列表、详情、推荐 |
| 朝代模块 | `/api/dynasties` | 朝代列表、详情 |
| 分类模块 | `/api/categories` | 分类列表、详情 |
| 论坛模块 | `/api/forum` | 帖子、评论管理 |
| 精选诗人 | `/api/poet-featured` | 精选诗人展示 |
| 首页导航 | `/api/home-navigation` | 导航菜单管理 |
| 诗话视野 | `/api/vision` | 文章浏览、点赞 |
| 搜索模块 | `/api/search` | 全局搜索 |
| 文件模块 | `/api/files` | 文件上传 |
| 浏览历史 | `/api/history` | 浏览历史管理 |
| AI模块 | `/api/ai` | AI问答、看图写诗、智能分析 |
| 管理员模块 | `/api/admin` | 管理员专属接口 |
| AI模型配置 | `/api/admin/ai-models` | AI模型管理 |

### 统一响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

### 错误码说明

| 错误码 | 说明 |
|--------|------|
| 200 | 操作成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
| 1001 | 用户不存在 |
| 1002 | 用户已存在 |
| 1003 | 密码错误 |
| 2001 | 诗词不存在 |
| 3001 | 帖子不存在 |

### API文档访问

启动后端服务后，访问以下地址查看完整的API文档：

- **Knife4j文档**：http://localhost:8081/doc.html
- **Swagger UI**：http://localhost:8081/swagger-ui.html

## 数据库设计

### 数据库表清单

| 序号 | 表名 | 说明 | 记录数预估 |
|------|------|------|------------|
| 1 | user | 用户表 | 10万+ |
| 2 | poet | 诗人表 | 1000+ |
| 3 | dynasty | 朝代表 | 15 |
| 4 | category | 诗词分类表 | 50+ |
| 5 | poem | 诗词表 | 10万+ |
| 6 | forum_post | 论坛帖子表 | 10万+ |
| 7 | comment | 评论表 | 100万+ |
| 8 | user_favorite | 用户收藏表 | 100万+ |
| 9 | user_like | 用户点赞表 | 100万+ |
| 10 | user_history | 用户浏览历史表 | 100万+ |
| 11 | operation_log | 操作日志表 | 1000万+ |
| 12 | ai_model | AI模型配置表 | <100 |
| 12.1 | ai_module_config | AI模块配置表 | <10 |
| 13 | poet_featured | 精选诗人卡片表 | <100 |
| 14 | home_navigation | 首页导航数据表 | <100 |
| 15 | vision_article | 诗话视野文章表 | 100+ |
| 16 | visit_log | 访问日志表 | 1000万+ |
| 17 | file_metadata | 文件元数据表 | 100万+ |
| 18 | ai_image_record | AI生成图片记录表 | 10万+ |
| 19 | poet_profile | 认证诗人资料表 | 1万+ |
| 20 | poem_rating | 诗词评分表 | 100万+ |
| 21 | poem_content_cache | 诗词内容缓存表 | 10万+ |
| 22 | poet_suggestion | 诗人内容建议表 | 1万+ |
| 23 | ai_generated_content | AI生成内容审核表 | 10万+ |
| 24 | rhyme | 韵部表 | 2000+ |
| 25 | poet_draft | 诗人内容草稿表 | 1万+ |
| 26 | static_page | 静态页面表 | <100 |
| 27 | repair_order | 报修工单表 | 1万+ |
| 28 | repair_comment | 报修评论表 | 10万+ |

### ER关系图

```
dynasty ──────────────────────────────┐
  │                                   │
  │ dynasty_id (FK)                   │
  ▼                                   │
poet ──────────┐                      │
  │            │ poet_id (FK)         │
  │            ▼                      │ dynasty_id (FK)
  │          poem ◄───────────────────┘
  │            │
  │            │ category_id (FK)
  │            ▼
  │         category（支持层级：parent_id → 自关联）
  │
  └── poet_featured（poet_id FK，精选诗人展示）

user ──┬── forum_post（user_id FK）
       │      │
       │      │ target_id (FK，target_type=2)
       │      ▼
       ├── comment（user_id FK, 多态 target_id + target_type, 支持多级回复：parent_id 自关联）
       │
       ├── user_favorite（多态：target_type → poem/post/poet）
       │
       ├── user_like（多态：target_type → poem/post/comment）
       │
       ├── user_history（多态：target_type → poem/post）
       │
       └── operation_log（user_id FK，可为NULL）

ai_model（独立配置表，无外键关联）

home_navigation（独立配置表，link_id 关联 poem/category/dynasty）

vision_article（独立文章表，无外键关联）

visit_log（独立日志表，user_id FK 可为NULL）
```

### 详细表结构

详细的表结构设计请参考：[数据库表结构文档](docs/database/schema.md)

## 开发指南

### 前端开发

#### 安装依赖

```bash
cd frontend
pnpm install
```

#### 启动开发服务器

```bash
pnpm dev
```

访问 http://localhost:5173

#### 代码检查

```bash
# 类型检查
pnpm type-check

# 代码规范检查
pnpm lint

# 自动修复
pnpm lint:fix
```

#### 运行测试

```bash
# 运行所有测试
pnpm test

# 运行测试并生成覆盖率报告
pnpm test:coverage

# 运行测试UI界面
pnpm test:ui
```

### 后端开发

#### 编译检查

```bash
cd sc-moyuan-backend
mvn compile
```

#### 运行测试

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=CategoryControllerTest

# 运行测试并生成覆盖率报告
mvn test jacoco:report
```

#### 启动应用

```bash
mvn spring-boot:run
```

### 开发规范

#### 前端规范

- 使用 Composition API + `<script setup>` 语法
- 使用 TypeScript 进行类型约束
- 遵循 ESLint 代码规范
- 组件使用 scoped 样式
- 样式使用 SCSS 预处理器
- 使用 Pinia 进行状态管理
- 使用 Vue Router 进行路由管理

#### 后端规范

- 使用 Lombok 注解简化代码
- 统一异常处理
- 使用参数化查询防止SQL注入
- 添加接口文档注解（Knife4j）
- 使用事务管理保证数据一致性
- 优化SQL查询提高性能

### 代码组织

#### 前端组件结构

```typescript
// 推荐：组件代码组织顺序
<script setup lang="ts">
// 1. 导入
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

// 2. 组件属性和事件
const props = defineProps<{ id: string }>()
const emit = defineEmits<{ (e: 'update', value: string): void }>()

// 3. 组合式函数（Composables）
const { data, loading } = useApi()

// 4. 响应式状态
const count = ref(0)

// 5. 计算属性
const doubleCount = computed(() => count.value * 2)

// 6. 侦听器
watch(count, (newVal, oldVal) => {
  console.log(`count变化: ${oldVal} -> ${newVal}`)
})

// 7. 方法
function increment() {
  count.value++
  emit('update', String(count.value))
}

// 8. 生命周期
onMounted(() => {
  console.log('组件挂载')
})
</script>
```

#### 后端分层结构

```
Controller层 → Service层 → Mapper层 → 数据库
     ↓              ↓           ↓
   接口定义      业务逻辑     数据访问
```

## 部署指南

### 前端部署

#### 构建生产版本

```bash
cd frontend
pnpm build
```

#### 部署到Nginx

```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    location / {
        root /path/to/dist;
        try_files $uri $uri/ /index.html;
    }
    
    location /api {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 后端部署

#### 打包应用

```bash
cd sc-moyuan-backend
mvn clean package -DskipTests
```

#### 运行应用

```bash
java -jar target/sc-moyuan-backend.jar --spring.profiles.active=prod
```

#### 使用Docker部署

```dockerfile
FROM openjdk:17-slim
COPY target/sc-moyuan-backend.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 生产环境配置

| 配置项 | 建议值 | 说明 |
|--------|--------|------|
| 服务器配置 | 2核4G | 最低配置 |
| 操作系统 | Linux | CentOS 7+ / Ubuntu 20+ |
| JDK版本 | 17+ LTS | 与开发环境一致 |
| MySQL版本 | 8.0+ | 与开发环境一致 |
| Redis版本 | 7.x | 与开发环境一致 |
| Nginx版本 | 1.20+ | 反向代理 |

## 测试指南

### 前端测试

#### 测试框架

- **单元测试**：Vitest
- **组件测试**：@vue/test-utils
- **覆盖率**：@vitest/coverage-v8

#### 测试文件规范

- 测试文件命名：`*.test.ts` 或 `*.spec.ts`
- 测试文件位置：与被测试文件同目录

#### 测试示例

```typescript
// 工具函数测试
import { describe, it, expect } from 'vitest'
import { formatNumber } from './format'

describe('formatNumber', () => {
  it('格式化小于1000的数字', () => {
    expect(formatNumber(500)).toBe('500')
  })

  it('格式化大于等于1000的数字', () => {
    expect(formatNumber(1000)).toBe('1.0k')
  })
})
```

### 后端测试

#### 测试框架

- **单元测试**：JUnit 5
- **Mock框架**：Mockito
- **集成测试**：Spring Boot Test
- **测试数据库**：H2（内存数据库）

#### 测试文件规范

- 测试类命名：`*Test.java` 或 `*Tests.java`
- 测试目录：`src/test/java/com/moyuan/`

#### 测试示例

```java
// Controller单元测试
@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Test
    void getAllCategories_返回分类列表() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(Arrays.asList(new Category()));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
```

### 测试覆盖率目标

- 单元测试覆盖率：≥80%
- 关键业务逻辑：100%覆盖

## 性能优化

### 前端优化

- **代码分割**：路由懒加载
- **图片优化**：懒加载、WebP格式
- **缓存策略**：HTTP缓存、Service Worker
- **打包优化**：Tree Shaking、代码压缩

### 后端优化

- **数据库索引**：合理设计索引
- **缓存策略**：Redis热点数据（@Cacheable）
- **异步处理**：消息队列
- **连接池**：数据库连接池（Druid）
- **SQL优化**：避免N+1查询、使用批量操作

### 性能指标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| 首屏加载时间 | < 3秒 | 3G网络环境 |
| 页面切换时间 | < 500ms | 路由切换 |
| 打包体积 | < 500KB | Gzip压缩后 |
| 接口响应时间 | < 200ms | 普通接口 |
| 复杂查询时间 | < 1秒 | 多表关联查询 |
| 并发用户数 | 1000 | 同时在线用户 |
| QPS | 500 | 每秒请求数 |

## 安全设计

### 前端安全

- **XSS防护**：输入过滤、输出编码
- **CSRF防护**：Token验证
- **HTTPS**：生产环境强制使用
- **敏感数据**：不存储在LocalStorage

### 后端安全

- **SQL注入防护**：参数化查询
- **密码加密**：BCrypt加密
- **JWT认证**：Token有效期24小时
- **接口限流**：防止恶意请求
- **日志脱敏**：敏感信息脱敏
- **CORS配置**：跨域资源共享

## 监控与运维

### 监控指标

- 接口响应时间
- 错误率统计
- 数据库慢查询
- 服务器资源使用
- 用户访问量

### 日志管理

- **应用日志**：Logback
- **访问日志**：Nginx
- **操作日志**：operation_log表
- **访问日志**：visit_log表

## 贡献指南

### 开发流程

1. **Fork项目**：在GitHub上Fork项目到自己的仓库
2. **克隆代码**：`git clone https://github.com/your-username/SC_MoYuan2.git`
3. **创建分支**：`git checkout -b feature/your-feature`
4. **提交代码**：`git commit -m 'feat: 添加新功能'`
5. **推送分支**：git push origin feature/your-feature`
6. **创建Pull Request**：在GitHub上创建PR

### 提交规范

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type类型**：
- `feat`: 新功能
- `fix`: 修复bug
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 代码重构
- `test`: 测试相关
- `chore`: 构建/工具相关

**示例**：
```
feat(user): 添加用户注册功能

- 实现用户注册接口
- 添加注册表单验证
- 集成短信验证码

Closes #123
```

### 代码审查

- 所有PR需要经过代码审查
- 确保代码符合项目规范
- 确保测试覆盖率达标
- 确保文档同步更新

## 常见问题

### Q: 后端启动失败怎么办？

A: 检查以下配置：
1. 数据库连接配置是否正确
2. MySQL服务是否启动
3. Redis服务是否启动
4. JDK版本是否为17+

### Q: 前端无法连接后端？

A: 检查以下配置：
1. 后端服务是否启动
2. CORS配置是否正确
3. API代理配置是否正确
4. 网络连接是否正常

### Q: 数据库表不存在？

A: 执行初始化脚本：
```bash
mysql -u root -p < sc-moyuan-backend/src/main/resources/db/init.sql
```

### Q: 如何切换AI模型？

A: 管理员可以在后台管理页面配置AI模型：
1. 访问管理后台
2. 进入AI模型管理
3. 添加或编辑模型配置
4. 设置默认模型

## 更新日志

### v1.6.0 (2026-06-26)
- ✅ 前端首页重构（3933行 → 819行，减少79%）
- ✅ 提取设计系统（variables.scss 扩展至188行、mixins.scss 扩展至359行）
- ✅ 清理旧版CSS全局覆盖（original.css 3处body覆盖删除）
- ✅ 首页响应式适配（4个断点：1200/992/768/576px）
- ✅ 组件化拆分（HomeCarousel/ForumPreview/AncientPoemSelection/ContemporaryPoems）
- ✅ 数据外置（3个JSON文件替代硬编码数据）
- ✅ 粒子效果性能优化（30fps限制、visibility暂停、prefers-reduced-motion）
- ✅ 引入Swiper替代手写轮播
- ✅ 新增诗人默认头像分配工具类（PoetDefaultAvatar）
- ✅ 新增文件上传AI水印功能
- ✅ 图片文件名规范化（去除空格）
- ✅ 更新数据库初始化脚本

### v1.5.0 (2026-06-09)
- ✅ 新增诗人推荐反馈功能（PoetSuggestion模块）
- ✅ 新增AI模块配置管理接口
- ✅ 优化前端组件样式和交互体验
- ✅ 更新数据库初始化脚本
- ✅ 完善项目文档体系（新增project-docs目录）
- ✅ 修复多个前端组件的类型定义问题
- ✅ 优化后端安全配置和工具类

### v1.4.0 (2026-06-03)
- ✅ 新增交流广场模块（communicate页面）
- ✅ 新增每日诗词组件（DailyPoetry）
- ✅ 新增诗词搜索组件（PoetrySearch）
- ✅ 新增权限控制组合式函数（usePermission）
- ✅ 新增访问日志功能（VisitLogAspect + visit_log表）
- ✅ 新增用户发布诗词功能（poem/create页面）
- ✅ 新增用户收藏列表接口（/api/poems/favorites）
- ✅ 新增诗词点赞/收藏检查接口
- ✅ 优化论坛评论API路径（统一为/comments）
- ✅ 更新数据库表结构文档（新增visit_log表）
- ✅ 更新API端点文档（新增诗词交互接口）

### v1.3.0 (2026-06-02)
- ✅ 新增诗人推荐功能（协同过滤算法，基于收藏/点赞/浏览历史）
- ✅ 新增首页导航管理（后端HomeNavigationController + 前端管理页面）
- ✅ 新增精选诗人管理（后端PoetFeaturedController + 前端管理页面）
- ✅ 新增诗话视野模块（VisionArticleController + 文章列表/详情页）
- ✅ 新增测试框架（Vitest）和单元测试示例
- ✅ 新增AuthHeader、QrCodeLink等通用组件
- ✅ 优化首页布局和交互体验
- ✅ 更新数据库初始化脚本和种子数据

### v1.2.1 (2026-05-30)
- ✅ 项目根目录整理：迁移废弃文件夹至归档目录
- ✅ 迁移旧版静态网站文件（css/, html/, js/, zt/）至 `_archive/old-static-site/`
- ✅ 清理前端构建产物（frontend/dist/）
- ✅ 更新 .gitignore 添加归档目录
- ✅ 更新项目结构文档

### v1.2.0 (2026-05-29)
- ✅ 新增文件上传功能（FileController + ImageUpload组件）
- ✅ 新增浏览历史功能（HistoryController + user_history表）
- ✅ 新增跨模块全局搜索（SearchController：诗词+诗人+帖子）
- ✅ 新增随机诗词和每日诗词接口
- ✅ 新增用户帖子列表接口
- ✅ 新增诗人列表和详情页面
- ✅ 新增论坛发帖页面
- ✅ 新增全局搜索结果页面
- ✅ 新增操作日志管理页面
- ✅ 新增ECharts图表库（管理后台Dashboard）
- ✅ 首页重写（从1100行精简到371行，连接后端API）
- ✅ 后端添加Redis缓存支持（@EnableCaching + @Cacheable）
- ✅ 后端添加定时任务（每日午夜清除诗词缓存）
- ✅ 后端添加静态资源映射（/uploads/**）
- ✅ 更新数据库表结构文档（新增user_history表）

### v1.1.0 (2026-05-29)
- ✅ 新增管理系统后台（控制台、用户管理、诗词管理、分类管理、朝代管理、诗人管理、帖子管理）
- ✅ 新增管理员API接口（AdminApiController）
- ✅ 新增管理员路由和权限守卫
- ✅ 新增默认管理员账号（admin / admin123）
- ✅ 更新安全配置，admin路径要求ADMIN角色

### v1.0.0 (2026-05-25)
- ✅ 实现现代诗词提取功能
- ✅ 实现诗词CRUD API
- ✅ 实现前后端数据交互
- ✅ 创建测试验证页面

## 相关文档

> **说明**：以下文档位于 `docs/` 目录，该目录通过 `.git/info/exclude` 排除在远程仓库之外，仅本地可访问。新开发者需参照项目规则设置本地排除。

- [系统架构文档](docs/architecture/system-architecture.md)
- [前端技术规范](docs/standards/frontend-standards.md)
- [后端技术规范](docs/standards/backend-standards.md)
- [数据库设计规范](docs/standards/database-standards.md)
- [技术栈限制文档](docs/constraints/tech-stack-constraints.md)
- [数据库表结构文档](docs/database/schema.md)
- [API端点清单](docs/api/endpoints.md)
- [认证授权机制](docs/api/auth.md)
- [业务模块说明](docs/business/modules.md)
- [开发指南](docs/guides/development-guide.md)

### 文档访问方式

1. **本地开发**：直接点击上述链接即可访问
2. **新开发者**：克隆仓库后，需先执行以下命令设置本地排除规则：
   ```bash
   # 查看 .git/info/exclude 文件，确认 docs/ 和 .trae/ 已被排除
   cat .git/info/exclude
   ```

### 文档维护

- 文档变更需同步更新相关文档
- 重大功能变更需更新架构文档
- API变更需更新API端点清单
- 数据库变更需更新表结构文档

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 联系方式

- **项目维护者**：墨渊开发团队
- **邮箱**：your-email@example.com
- **GitHub**：https://github.com/your-username/SC_MoYuan2

## 致谢

感谢所有为本项目做出贡献的开发者！

---

**文档版本**：v2.1  
**最后更新**：2026-06-28  
**维护人员**：墨渊开发团队