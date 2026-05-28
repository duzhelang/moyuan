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
├── index.html          # 主页面
├── test.html           # 测试页面
├── css/                # 样式文件
├── js/                 # JavaScript文件
├── backend/            # 后端项目
│   ├── src/            # 源代码
│   ├── pom.xml         # Maven配置
│   └── README.md       # 后端说明
└── README.md           # 本文件
```

### 技术栈
- **前端**: HTML5, CSS3, JavaScript
- **后端**: Spring Boot 3.2, MyBatis-Plus 3.5
- **数据库**: MySQL 8.0
- **构建工具**: Maven 3.8

## 更新日志

### v1.0.0 (2026-05-25)
- ✅ 实现现代诗词提取功能
- ✅ 实现诗词CRUD API
- ✅ 实现前后端数据交互
- ✅ 创建测试验证页面

## 许可证

MIT License

## 联系方式

如有问题，请联系项目维护者。
