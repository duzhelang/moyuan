# 古今诗话——墨渊 现代诗词管理系统

## 系统概述

这是一个将前端页面中的现代诗词存入数据库并实现前后端加载的系统。系统包含：

1. **前端页面** - 展示和管理现代诗词
2. **后端API** - 提供数据存储和查询接口
3. **数据库** - 存储诗词数据
4. **测试页面** - 验证系统功能

## 系统架构

```
前端页面 (index.html)
    ↓ API调用
后端服务 (Spring Boot)
    ↓ 数据库操作
MySQL数据库
```

## 功能特性

### 前端功能
- ✅ 提取页面中的现代诗词
- ✅ 手动添加新诗词
- ✅ 查看已存储的诗词列表
- ✅ 编辑和删除诗词
- ✅ 实时状态反馈

### 后端功能
- ✅ RESTful API接口
- ✅ 诗词CRUD操作
- ✅ 分页查询
- ✅ 搜索功能
- ✅ 统一响应格式
- ✅ 管理员API接口（用户/诗词/分类/朝代/诗人/帖子管理）

### 数据库功能
- ✅ 诗词表 (poem)
- ✅ 诗人表 (poet)
- ✅ 朝代表 (dynasty)
- ✅ 分类表 (category)

## 快速开始

### 1. 环境要求
- Java 17+
- MySQL 8.0+
- Maven 3.8+
- 现代浏览器

### 2. 数据库设置
```sql
-- 执行数据库初始化脚本
source backend/src/main/resources/db/init.sql
```

### 3. 后端启动
```bash
cd backend
mvn spring-boot:run
```

### 4. 前端访问
```
http://localhost:8080/index.html
```

### 5. 测试页面
```
http://localhost:8080/test.html
```

## API接口

### 基础URL
```
http://localhost:8080/api
```

### 主要接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/poems/modern` | GET | 获取现代诗词列表 |
| `/poems/modern/page` | GET | 分页获取现代诗词 |
| `/poems/{id}` | GET | 获取单个诗词 |
| `/poems` | POST | 创建新诗词 |
| `/poems/{id}` | PUT | 更新诗词 |
| `/poems/{id}` | DELETE | 删除诗词 |
| `/poems/search` | GET | 搜索诗词 |

### 响应格式
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

## 使用说明

### 1. 提取页面诗词
- 点击"提取页面诗词"按钮
- 系统自动解析"当代·精选"部分的诗词
- 批量提交到数据库

### 2. 添加新诗词
- 点击"添加新诗词"按钮
- 填写诗词信息（标题、内容、作者等）
- 选择诗词流派
- 点击提交

### 3. 管理诗词
- 查看诗词列表和统计信息
- 编辑或删除现有诗词
- 实时更新显示

## 配置说明

### 后端配置
```yaml
# application.yml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/moyuan
    username: root
    password: root
```

### 前端配置
```javascript
// API基础URL
const API_BASE_URL = 'http://localhost:8080/api';
```

## 测试验证

### 1. 功能测试
访问 `test.html` 页面，测试各项功能：
- API接口测试
- 前端功能测试
- 数据库连接测试

### 2. 数据验证
```sql
-- 查询现代诗词数量
SELECT COUNT(*) FROM poem WHERE dynasty_id = 13;

-- 查询诗人信息
SELECT * FROM poet WHERE dynasty_id = 13;
```

## 常见问题

### Q: 后端启动失败怎么办？
A: 检查数据库连接配置，确保MySQL服务已启动。

### Q: 前端无法连接后端？
A: 确保后端服务已启动，检查CORS配置。

### Q: 数据库表不存在？
A: 执行初始化脚本 `backend/src/main/resources/db/init.sql`

## 开发说明

### 项目结构
```
SC_MoYuan2_/
├── frontend/              # Vue 3 前端项目
│   ├── src/               # 源代码
│   ├── public/            # 静态资源
│   ├── package.json       # 前端依赖配置
│   └── vite.config.ts     # Vite 配置
├── sc-moyuan-backend/     # Spring Boot 后端项目
│   ├── src/               # 源代码
│   └── pom.xml            # Maven 配置
├── img/                   # 图片资源（前端引用）
├── _archive/              # 归档目录（旧版文件）
│   └── old-static-site/   # 旧版静态网站文件
├── index.html             # 旧版主页面（已归档）
├── pom.xml                # Maven 父项目配置
├── start.bat              # 启动脚本
├── init-db.bat            # 数据库初始化脚本
└── README.md              # 本文件
```

### 技术栈
- **前端**: Vue 3, TypeScript, Vite, Element Plus, Pinia
- **后端**: Spring Boot 3.2, MyBatis-Plus 3.5, Spring Security, JWT
- **数据库**: MySQL 8.0, Redis 7.x
- **构建工具**: Maven 3.8, Node.js

## 更新日志

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
  - 迁移旧版静态网站文件（css/, html/, js/, zt/）至 `_archive/old-static-site/`
  - 迁移测试和演示文件（test.html, yinyuebofang.html）
  - 迁移开发笔记文件（备忘录.txt, 备忘录_附1.jpg）
  - 清理前端构建产物（frontend/dist/）
  - 更新 .gitignore 添加归档目录
  - 更新项目结构文档

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

## 许可证

MIT License

## 联系方式

如有问题，请联系项目维护者。
