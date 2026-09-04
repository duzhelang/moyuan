# 技术栈限制文档

## 概述

本文档定义了"古今诗话——墨渊"项目的技术栈限制和约束条件，确保团队在开发过程中遵循统一的技术标准。

## 前端技术栈限制

### 必须使用

| 技术 | 版本要求 | 说明 |
|------|----------|------|
| Vue.js | 3.4+ | 必须使用Composition API |
| TypeScript | 5.x | 必须开启严格模式 |
| Vite | 5.x | 官方推荐构建工具 |
| Pinia | 2.x | 官方状态管理库 |
| Vue Router | 4.x | 官方路由库 |
| Element Plus | 2.x | UI组件库 |
| Axios | 1.x | HTTP客户端 |

### 禁止使用

| 技术 | 原因 |
|------|------|
| jQuery | 与Vue理念冲突，操作DOM不推荐 |
| Vuex | 已被Pinia取代 |
| Options API | 统一使用Composition API |
| JavaScript | 必须使用TypeScript |
| CSS预处理器（Sass/Less）以外的 | 统一使用SCSS |

### 可选使用

| 技术 | 使用场景 |
|------|----------|
| dayjs | 日期处理（轻量级替代moment.js） |
| lodash-es | 工具函数（按需引入） |
| ECharts | 数据可视化图表（管理后台Dashboard使用） |
| vue-echarts | Vue ECharts组件封装 |
| highlight.js | 代码高亮 |
| marked | Markdown渲染 |

## 后端技术栈限制

### 必须使用

| 技术 | 版本要求 | 说明 |
|------|----------|------|
| Java | 17+（编译兼容模式，source/target=17） | pom.xml 中 java.version=21，maven-compiler-plugin source/target=17，确保 Java 17+ LTS 可运行 |
| Spring Boot | 3.x | 应用框架 |
| Spring Security | 6.x | 安全框架 |
| MyBatis-Plus | 3.5.x | ORM框架 |
| MySQL | 8.0+ | 主数据库 |
| Redis | 7.x | 缓存数据库 |
| Lombok | 1.18.x | 代码简化 |
| Maven | 3.8+ | 项目构建 |

### 禁止使用

| 技术 | 原因 |
|------|------|
| JPA/Hibernate | 统一使用MyBatis-Plus |
| Spring Boot 2.x | 版本过旧，不支持Java 17 |
| Java 8/11 | 版本过旧，不满足编译要求（source/target=17） |
| Gradle | 统一使用Maven |
| MongoDB | 核心业务使用MySQL，仅在必要时使用 |

### 可选使用

| 技术 | 使用场景 |
|------|----------|
| MapStruct | 对象映射（复杂DTO转换） |
| Knife4j | API文档生成 |
| Elasticsearch | 全文搜索（数据量大时） |
| RabbitMQ | 消息队列（异步任务） |
| MinIO | 文件存储（图片、音频） |

## 字符编码限制

### 全链路编码要求

本项目**强制使用 UTF-8 编码**，确保中文和特殊字符正确处理。

| 环节 | 编码要求 | 配置方式 |
|------|----------|----------|
| HTML页面 | UTF-8 | `<meta charset="UTF-8">` |
| Axios请求 | UTF-8 | `Content-Type: application/json; charset=utf-8` |
| Spring Boot | UTF-8 | `server.servlet.encoding.charset=UTF-8` + `force=true` |
| JDBC连接 | UTF-8 | `jdbc:mysql://localhost:3306/db?useUnicode=true&characterEncoding=UTF-8`（大写，自动映射 utf8mb4） |
| MySQL数据库 | utf8mb4 | `DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci` |
| MySQL连接器 | utf8mb4 | `--default-character-set=utf8mb4` |

### 前端编码规范

```typescript
// Axios 实例配置
const service = axios.create({
  headers: {
    'Content-Type': 'application/json; charset=utf-8'
  }
})
```

### `v-html` 内容渲染编码约束

使用 `v-html` 渲染用户内容时，必须按以下顺序处理：

1. **先转义 HTML 实体**：`&` → `&amp;`、`<` → `&lt;`、`>` → `&gt;`
2. **再替换换行符**：`\r?\n` → `<br/>`（同时兼容 Windows `\r\n` 和 Unix `\n`）
3. **最后处理结构化标签**（如 h1-h6 提取目录）

```typescript
// 正确做法 ✅
let content = rawContent
  .replace(/&/g, '&amp;')      // 第1步：HTML实体转义
  .replace(/</g, '&lt;')
  .replace(/>/g, '&gt;')
  .replace(/\r?\n/g, '<br/>')  // 第2步：换行转<br/>

// 错误做法 ❌ — 直接替换 \n 会导致 < > & 被 v-html 误解析为 HTML 标签
content.replace(/\n/g, '<br/>')
```

### CSS `letter-spacing` 与 `text-align: center` 的视觉偏移约束

当元素同时设置 `letter-spacing` 和 `text-align: center` 时，`letter-spacing` 会在最后一个字符右侧额外添加间距，导致文本视觉上偏左。必须使用 `padding-left` 补偿：

```scss
// 正确做法 ✅ — letter-spacing: 4px 时，padding-left: 4px 补偿
h4 {
  text-align: center;
  letter-spacing: 4px;
  padding-left: 4px; // 补偿 letter-spacing 导致的视觉偏左
}

// 错误做法 ❌ — 不做补偿，文字视觉偏左
h4 {
  text-align: center;
  letter-spacing: 4px;
}
```

### 后端编码规范

```yaml
# application.yml
server:
  servlet:
    encoding:
      charset: UTF-8
      enabled: true
      force: true
```

```yaml
# application-dev.yml / application-prod.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/moyuan?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
```

### SecurityConfig 编码规范

```java
// 错误响应必须显式设置 charset
response.setContentType("application/json;charset=UTF-8");
response.setCharacterEncoding("UTF-8");
```

### 数据库建表规范

```sql
-- 数据库级别
CREATE DATABASE IF NOT EXISTS `moyuan` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 表级别
CREATE TABLE example (
  -- 字段定义
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 终端编码注意事项

在 Windows PowerShell 中执行 MySQL 命令时，需注意终端编码：

```powershell
# 查看当前编码
chcp

# 切换到 UTF-8 编码（重要！）
chcp 65001

# 然后执行 MySQL 命令
mysql -u root -p --default-character-set=utf8mb4 -e "SELECT * FROM table"
```

**警告**：在 GBK 编码的终端中直接插入中文数据会导致乱码！

### 编码问题排查清单

- [ ] 检查 HTML meta charset 声明
- [ ] 检查 Axios Content-Type 头
- [ ] 检查 Spring Boot encoding 配置
- [ ] 检查 JDBC 连接串 characterEncoding 参数
- [ ] 检查数据库和表的 charset 设置
- [ ] 检查 SecurityConfig 错误响应的 charset
- [ ] 检查终端编码（Windows 需要 chcp 65001）
- [ ] 检查 `v-html` 内容是否进行了 HTML 实体转义（`&` `<` `>`）
- [ ] 检查 `v-html` 内容是否正确处理了 `\r\n` 双换行符
- [ ] 检查 `text-align: center` + `letter-spacing` 组合是否添加了 `padding-left` 补偿

---

## 数据库限制

### MySQL限制

| 限制项 | 限制值 | 说明 |
|--------|--------|------|
| 最大连接数 | 200 | 生产环境配置 |
| 单表最大行数 | 1000万 | 超过需考虑分表 |
| 单行最大字节数 | 65535 | UTF8mb4编码 |
| 单字段最大长度 | 65535 | TEXT类型 |
| 索引最大数量 | 16 | 单表索引上限 |
| 复合索引最大字段数 | 16 | 单个复合索引 |

### Redis限制

| 限制项 | 限制值 | 说明 |
|--------|--------|------|
| 最大内存 | 2GB | 单实例配置 |
| Key最大长度 | 512MB | 建议不超过1KB |
| Value最大长度 | 512MB | 建议不超过10MB |
| 最大Key数量 | 2^32 | 约42亿 |

## 开发环境限制

### 本地开发

| 工具 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 17+（推荐 17/21 LTS） | OpenJDK或Oracle JDK，pom.xml 编译目标为 Java 17 |
| Node.js | 18+ | LTS版本 |
| npm/pnpm | 最新稳定版 | 推荐pnpm |
| MySQL | 8.0+ | 本地开发数据库 |
| Redis | 7.x | 本地缓存 |
| IDE | IntelliJ IDEA / VS Code | 推荐IntelliJ IDEA |

### 版本控制

| 工具 | 版本要求 | 说明 |
|------|----------|------|
| Git | 2.30+ | 版本控制 |
| GitHub/GitLab | - | 代码托管平台 |

## 代码规范

> **详细规范文档**：
> - 前端代码规范：[前端技术规范](../standards/frontend-standards.md)
> - 后端代码规范：[后端技术规范](../standards/backend-standards.md)

### 前端强制限制

| 限制项 | 要求 | 原因 |
|--------|------|------|
| 必须使用 TypeScript | 禁止纯 JavaScript | 类型安全 |
| 必须使用 Composition API | 禁止 Options API | 统一风格 |
| 必须使用 `<script setup>` | 禁止普通 `<script>` | 简化代码 |
| 必须开启 strict 模式 | tsconfig.json 中 `strict: true` | 类型检查 |
| 必须使用 scoped 样式 | 禁止全局样式污染 | 样式隔离 |

### 后端强制限制

| 限制项 | 要求 | 原因 |
|--------|------|------|
| 必须使用 Lombok | 禁止手写 getter/setter | 代码简洁 |
| 必须使用参数化查询 | 禁止字符串拼接 SQL | 防止 SQL 注入 |
| 必须统一异常处理 | 使用 BusinessException | 错误处理一致性 |
| 必须分层调用 | Controller → Service → Mapper | 架构清晰 |

## 性能限制

### 前端性能

| 指标 | 限制值 | 说明 |
|------|--------|------|
| 首屏加载时间 | < 3秒 | 3G网络环境 |
| 页面切换时间 | < 500ms | 路由切换 |
| 打包体积 | < 500KB | Gzip压缩后 |
| 图片大小 | < 200KB | 单张图片 |
| 接口响应时间 | < 1秒 | 95%的请求 |

### 后端性能

| 指标 | 限制值 | 说明 |
|------|--------|------|
| 接口响应时间 | < 200ms | 普通接口 |
| 复杂查询时间 | < 1秒 | 多表关联查询 |
| 并发用户数 | 1000 | 同时在线用户 |
| QPS | 500 | 每秒请求数 |
| 数据库连接池 | 20 | 最小连接数 |

## 安全限制

### 前端安全

| 限制项 | 要求 | 说明 |
|--------|------|------|
| XSS防护 | 必须 | 输入过滤、输出编码 |
| CSRF防护 | 必须 | Token验证 |
| HTTPS | 必须 | 生产环境强制使用 |
| 敏感数据 | 禁止 | 密码、密钥等不存储在LocalStorage |
| 本地存储前缀 | 必须 | 所有键使用 `moyuan_` 前缀隔离 |
| 缓存过期 | 必须 | API缓存1小时，用户偏好24小时 |
| 第三方CDN | 禁止 | 使用本地资源 |

### 后端安全

| 限制项 | 要求 | 说明 |
|--------|------|------|
| SQL注入防护 | 必须 | 参数化查询 |
| 密码加密 | 必须 | BCrypt加密 |
| JWT有效期 | 24小时 | Token过期时间 |
| 接口限流 | 必须 | 防止恶意请求 |
| 日志脱敏 | 必须 | 敏感信息脱敏 |

## 部署限制

### 生产环境

| 限制项 | 要求 | 说明 |
|--------|------|------|
| 服务器配置 | 2核4G | 最低配置 |
| 操作系统 | Linux | CentOS 7+ / Ubuntu 20+ |
| JDK版本 | 17+ LTS | 与开发环境一致，推荐 Java 17/21 LTS |
| MySQL版本 | 8.0+ | 与开发环境一致 |
| Redis版本 | 7.x | 与开发环境一致 |
| Nginx版本 | 1.20+ | 反向代理 |

### 容器化部署

```yaml
# Docker限制
services:
  backend:
    image: openjdk:17-slim
    memory: 1g
    cpus: 1
    
  frontend:
    image: nginx:alpine
    memory: 256m
    cpus: 0.5
    
  mysql:
    image: mysql:8.0
    memory: 1g
    cpus: 1
    
  redis:
    image: redis:7-alpine
    memory: 512m
    cpus: 0.5
```

## 第三方服务限制

### API调用限制

| 服务 | 限制 | 说明 |
|------|------|------|
| AI大模型API | 100次/分钟 | 防止滥用 |
| 短信服务 | 100条/天 | 用户验证 |
| 邮件服务 | 500封/天 | 通知邮件 |
| 文件上传 | 10MB/文件 | 单文件大小 |
| 图片上传 | 5MB/文件 | 图片大小 |

## 兼容性限制

### 浏览器兼容

| 浏览器 | 最低版本 | 说明 |
|--------|----------|------|
| Chrome | 90+ | 主要支持 |
| Firefox | 88+ | 主要支持 |
| Safari | 14+ | 主要支持 |
| Edge | 90+ | 主要支持 |
| IE | 不支持 | 不再兼容 |

### 移动端兼容

| 设备 | 系统版本 | 说明 |
|------|----------|------|
| iOS | 14+ | Safari内核 |
| Android | 10+ | Chrome内核 |

## 扩展性限制

### 水平扩展

| 组件 | 扩展方式 | 限制 |
|------|----------|------|
| 前端 | CDN分发 | 无限制 |
| 后端 | 负载均衡 | 最多10个实例 |
| 数据库 | 主从复制 | 1主2从 |
| 缓存 | Redis集群 | 最多6个节点 |

### 垂直扩展

| 组件 | 扩展方式 | 限制 |
|------|----------|------|
| CPU | 升级配置 | 最多16核 |
| 内存 | 升级配置 | 最多64GB |
| 存储 | 扩容磁盘 | 最多1TB |
| 带宽 | 升级网络 | 最多100Mbps |

## 新代码编写规范

### 依赖引入限制

| 规则 | 说明 | 违规后果 |
|------|------|----------|
| 禁止未经审批引入新依赖 | 新增第三方库需在团队内说明必要性 | 代码审查不通过 |
| 优先使用已有依赖 | 检查项目是否已有类似功能的库 | 避免重复依赖 |
| 版本锁定 | package.json/pom.xml 必须锁定版本号 | 避免版本冲突 |
| 安全审查 | 引入前检查依赖是否有已知漏洞 | 安全风险 |

> **详细规范文档**：
> - 前端代码规范：[前端技术规范](../standards/frontend-standards.md)
> - 后端代码规范：[后端技术规范](../standards/backend-standards.md)

---

**文档版本**：v1.8
**最后更新**：2026-09-04
**维护人员**：墨渊开发团队

## 变更记录

| 版本 | 日期 | 变更内容 |
|------|------|----------|
| v1.8 | 2026-09-04 | 后端 java.version 由 23 调整为 21（编译兼容 source/target=17 不变） |
| v1.7 | 2026-06-08 | 更新前端安全限制：明确本地存储前缀规范和缓存过期策略 |
| v1.6 | 2026-06-08 | 职责分离：删除重复的代码规范内容，改为引用前端/后端技术规范文档 |
| v1.5 | 2026-06-06 | 初始版本 |