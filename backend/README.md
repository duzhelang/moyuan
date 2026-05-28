# 古今诗话——墨渊 后端服务

## 项目概述

这是古今诗话——墨渊系统的后端服务，基于Spring Boot 3.2构建，提供现代诗词管理的RESTful API。

## 技术栈

- **框架**: Spring Boot 3.2.5
- **ORM**: MyBatis-Plus 3.5.5
- **数据库**: MySQL 8.0
- **缓存**: Redis 7.x
- **安全**: Spring Security 6.x
- **构建工具**: Maven 3.8+

## 项目结构

```
backend/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── moyuan/
│   │   │           ├── MoyuanApplication.java      # 启动类
│   │   │           ├── config/                     # 配置类
│   │   │           │   ├── MybatisPlusConfig.java  # MyBatis-Plus配置
│   │   │           │   └── CorsConfig.java         # CORS配置
│   │   │           ├── common/                     # 公共类
│   │   │           │   └── Result.java             # 统一响应类
│   │   │           ├── entity/                     # 实体类
│   │   │           │   ├── Poem.java               # 诗词实体
│   │   │           │   ├── Poet.java               # 诗人实体
│   │   │           │   ├── Dynasty.java            # 朝代实体
│   │   │           │   └── Category.java           # 分类实体
│   │   │           ├── mapper/                     # Mapper接口
│   │   │           │   ├── PoemMapper.java
│   │   │           │   ├── PoetMapper.java
│   │   │           │   ├── DynastyMapper.java
│   │   │           │   └── CategoryMapper.java
│   │   │           ├── service/                    # 服务层
│   │   │           │   ├── PoemService.java
│   │   │           │   └── impl/
│   │   │           │       └── PoemServiceImpl.java
│   │   │           └── controller/                 # 控制器
│   │   │               └── PoemController.java
│   │   └── resources/
│   │       ├── application.yml                     # 应用配置
│   │       └── db/
│   │           └── init.sql                        # 数据库初始化脚本
│   └── test/                                       # 测试代码
└── pom.xml                                         # Maven配置
```

## 快速开始

### 1. 环境要求
- Java 17+
- MySQL 8.0+
- Maven 3.8+

### 2. 数据库配置
1. 创建数据库：
```sql
CREATE DATABASE moyuan DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 执行初始化脚本：
```bash
mysql -u root -p moyuan < src/main/resources/db/init.sql
```

### 3. 配置文件
修改 `application.yml` 中的数据库连接信息：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/moyuan?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: your_username
    password: your_password
```

### 4. 启动应用
```bash
# 使用Maven启动
mvn spring-boot:run

# 或者打包后启动
mvn clean package
java -jar target/moyuan-backend-1.0.0.jar
```

### 5. 访问应用
- 应用地址: http://localhost:8080
- API文档: http://localhost:8080/api
- 测试页面: http://localhost:8080/test.html

## API接口

### 基础信息
- **Base URL**: `/api`
- **Content-Type**: `application/json`
- **字符编码**: UTF-8

### 诗词接口

#### 获取现代诗词列表
```http
GET /api/poems/modern
```

**响应示例**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "title": "愿如花已落千行",
      "content": "愿如花已落千行，未闻花语浅别殇...",
      "poetId": 1,
      "dynastyId": 13,
      "categoryId": 2,
      "source": "常平逼王",
      "viewCount": 0,
      "likeCount": 0,
      "favoriteCount": 0,
      "status": 1,
      "isFeatured": 1,
      "createTime": "2026-05-25 10:00:00",
      "updateTime": "2026-05-25 10:00:00"
    }
  ]
}
```

#### 分页获取现代诗词
```http
GET /api/poems/modern/page?pageNum=1&pageSize=10
```

#### 获取单个诗词
```http
GET /api/poems/{id}
```

#### 创建新诗词
```http
POST /api/poems
```

**请求体**:
```json
{
  "title": "诗词标题",
  "content": "诗词内容",
  "source": "作者",
  "dynastyId": 13,
  "categoryId": 2,
  "status": 1,
  "isFeatured": 0
}
```

#### 更新诗词
```http
PUT /api/poems/{id}
```

#### 删除诗词
```http
DELETE /api/poems/{id}
```

#### 搜索诗词
```http
GET /api/poems/search?keyword=关键词
```

## 数据库设计

### 诗词表 (poem)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键ID |
| title | VARCHAR(100) | 诗词标题 |
| content | TEXT | 诗词内容 |
| poet_id | BIGINT | 诗人ID |
| dynasty_id | BIGINT | 朝代ID |
| category_id | BIGINT | 分类ID |
| source | VARCHAR(255) | 来源 |
| view_count | INT | 浏览次数 |
| like_count | INT | 点赞次数 |
| favorite_count | INT | 收藏次数 |
| status | TINYINT | 状态 |
| is_featured | TINYINT | 是否精选 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| deleted | TINYINT | 逻辑删除 |

### 朝代表 (dynasty)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键ID |
| name | VARCHAR(50) | 朝代名称 |
| start_year | INT | 开始年份 |
| end_year | INT | 结束年份 |
| description | TEXT | 朝代简介 |
| sort_order | INT | 排序顺序 |

## 配置说明

### 应用配置
```yaml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/moyuan
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.moyuan.entity
  configuration:
    map-underscore-to-camel-case: true
```

### CORS配置
已配置允许所有来源的跨域请求，支持前端开发环境。

## 开发指南

### 添加新实体
1. 在 `entity` 包中创建实体类
2. 在 `mapper` 包中创建Mapper接口
3. 在 `service` 包中创建服务接口和实现
4. 在 `controller` 包中创建控制器

### 添加新接口
1. 在控制器中添加新的请求映射方法
2. 使用 `Result` 类统一响应格式
3. 添加适当的参数验证

### 数据库操作
1. 使用MyBatis-Plus的BaseMapper进行基础CRUD
2. 复杂查询使用LambdaQueryWrapper
3. 分页查询使用Page对象

## 测试

### 单元测试
```bash
mvn test
```

### API测试
使用Postman或curl测试API接口：
```bash
# 获取现代诗词
curl http://localhost:8080/api/poems/modern

# 创建新诗词
curl -X POST http://localhost:8080/api/poems \
  -H "Content-Type: application/json" \
  -d '{"title":"测试","content":"测试内容","source":"测试作者"}'
```

## 部署

### 打包应用
```bash
mvn clean package
```

### 运行应用
```bash
java -jar target/moyuan-backend-1.0.0.jar
```

### Docker部署
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/moyuan-backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 常见问题

### Q: 数据库连接失败
A: 检查 `application.yml` 中的数据库配置，确保MySQL服务已启动。

### Q: 端口被占用
A: 修改 `application.yml` 中的 `server.port` 配置。

### Q: 跨域问题
A: 已配置CORS，如果仍有问题，检查前端请求的URL是否正确。

## 更新日志

### v1.0.0 (2026-05-25)
- ✅ 实现诗词CRUD接口
- ✅ 实现分页查询
- ✅ 实现搜索功能
- ✅ 配置CORS支持
- ✅ 添加数据库初始化脚本

## 许可证

MIT License
