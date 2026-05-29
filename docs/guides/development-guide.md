# 开发指南

## 概述

本文档为"古今诗话——墨渊"项目的开发指南，帮助开发人员快速上手项目开发。

## 环境准备

### 前端开发环境

#### 1. 安装Node.js
- 版本要求：18+
- 下载地址：https://nodejs.org/

#### 2. 安装pnpm（推荐）
```bash
npm install -g pnpm
```

#### 3. 安装依赖
```bash
# 进入前端目录
cd frontend

# 安装依赖
pnpm install
```

#### 4. 启动开发服务器
```bash
pnpm dev
```

访问 http://localhost:5173 查看效果

### 后端开发环境

#### 1. 安装JDK
- 版本要求：17+
- 下载地址：https://adoptium.net/

#### 2. 安装Maven
- 版本要求：3.8+
- 下载地址：https://maven.apache.org/

#### 3. 安装MySQL
- 版本要求：8.0+
- 下载地址：https://dev.mysql.com/downloads/

#### 4. 安装Redis
- 版本要求：7.x
- 下载地址：https://redis.io/download

#### 5. 创建数据库并导入初始化数据
```bash
# 进入基础版后端目录（包含 init.sql）
cd backend

# 执行初始化脚本（自动创建数据库、建表、导入初始数据）
mysql -u root -p < src/main/resources/db/init.sql
```

> **说明**：`init.sql` 包含数据库创建、4张核心表（dynasty、poet、category、poem）建表语句及初始数据（13个朝代、1个诗人、10个分类、11首诗词）。

#### 6. 配置数据库连接
编辑 `src/main/resources/application.yml`，修改数据库连接信息：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/moyuan?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: your_username
    password: your_password
```

#### 7. 启动后端服务
```bash
# 方式一：启动基础版后端（推荐入门使用）
cd backend
mvn spring-boot:run

# 方式二：启动完整版后端（包含安全认证、JWT等功能）
cd sc-moyuan-backend
mvn spring-boot:run
```

> **两个后端模块说明**：
> - **backend/**：基础版，提供诗词CRUD功能，API路径前缀 `/api`，无安全框架依赖
> - **sc-moyuan-backend/**：完整版，包含AuthController、ForumController等，尚未完成全部Controller

访问 http://localhost:8080/doc.html 查看API文档（Knife4j，仅完整版后端）

## 项目结构

### 前端项目结构

```
frontend/
├── src/
│   ├── api/                    # API接口
│   ├── assets/                # 静态资源
│   ├── components/            # 公共组件
│   ├── composables/           # 组合式函数
│   ├── layouts/               # 布局组件
│   ├── router/                # 路由配置
│   ├── stores/                # 状态管理
│   ├── types/                 # 类型定义
│   ├── utils/                 # 工具函数
│   ├── views/                 # 页面组件
│   ├── App.vue               # 根组件
│   └── main.ts               # 入口文件
├── index.html
├── package.json
├── tsconfig.json
└── vite.config.ts
```

### 后端项目结构

项目包含两个后端模块：

#### backend/（基础版后端）

```
backend/
├── src/main/java/com/moyuan/
│   ├── config/                 # 配置类（CorsConfig, MybatisPlusConfig）
│   ├── controller/             # 控制器层（PoemController）
│   ├── service/                # 服务层（PoemService, PoemServiceImpl）
│   ├── mapper/                 # Mapper层（PoemMapper, CategoryMapper, DynastyMapper, PoetMapper）
│   ├── entity/                 # 实体类（Poem, Category, Dynasty, Poet）
│   └── common/                 # 公共类（Result.java）
├── src/main/resources/
│   ├── application.yml
│   └── db/init.sql             # 建表 + 初始数据
└── pom.xml
```

#### sc-moyuan-backend/（完整版后端）

```
sc-moyuan-backend/
├── src/main/java/com/moyuan/
│   ├── config/                 # 配置类（CorsConfig, MyBatisPlusConfig, RedisConfig, SecurityConfig, SwaggerConfig）
│   ├── controller/             # 控制器层（AuthController, ForumController 已实现；其余规划中）
│   ├── common/                 # 公共类（R.java, ResultCode.java, PageRequest.java）
│   ├── aspect/                 # 切面（OperationLogAspect）
│   ├── service/                # 服务层（规划中）
│   ├── mapper/                 # Mapper层（规划中）
│   ├── entity/                 # 实体类（规划中）
│   ├── dto/                    # 数据传输对象（规划中）
│   ├── exception/              # 异常处理（规划中）
│   ├── security/               # 安全相关（规划中）
│   └── util/                   # 工具类（规划中）
├── src/main/resources/
│   └── application.yml
└── pom.xml
```

## 开发流程

### 1. 需求分析
- 阅读需求文档，理解功能需求
- 与产品经理沟通确认需求细节
- 评估技术实现难度和工作量

### 2. 技术方案设计
- 查阅 `docs/architecture/system-architecture.md` 了解系统架构
- 查阅 `docs/standards/frontend-standards.md` 或 `docs/standards/backend-standards.md` 了解技术规范
- 设计技术方案，包括：
  - 数据库表设计
  - API接口设计
  - 前端组件设计
  - 状态管理设计

### 3. 编码实现
- 遵循技术规范编写代码
- 使用TypeScript保证类型安全
- 编写清晰的注释和文档
- 及时提交代码到版本控制

### 4. 代码审查
- 自我审查代码质量
- 提交代码审查请求
- 根据审查意见修改代码
- 确保代码符合规范

### 5. 测试验证
- 编写单元测试
- 执行集成测试
- 进行功能测试
- 修复测试发现的问题

### 6. 部署上线
- 合并代码到主分支
- 执行构建脚本
- 部署到测试环境
- 验证功能正常
- 部署到生产环境

## 常见任务示例

### 示例1：创建新的API接口

#### 后端步骤

1. **创建实体类**
```java
@Data
@TableName("new_table")
public class NewEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
```

2. **创建Mapper接口**
```java
@Mapper
public interface NewMapper extends BaseMapper<NewEntity> {
}
```

3. **创建Service接口和实现**
```java
public interface NewService extends IService<NewEntity> {
    R<NewEntity> getById(Long id);
}

@Service
@RequiredArgsConstructor
public class NewServiceImpl extends ServiceImpl<NewMapper, NewEntity> implements NewService {
    @Override
    public R<NewEntity> getById(Long id) {
        NewEntity entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException("数据不存在");
        }
        return R.success(entity);
    }
}
```

4. **创建Controller**
```java
@Tag(name = "新功能管理")
@RestController
@RequestMapping("/api/new")
@RequiredArgsConstructor
public class NewController {
    
    private final NewService newService;
    
    @Operation(summary = "获取详情")
    @GetMapping("/{id}")
    public R<NewEntity> getById(@PathVariable Long id) {
        return newService.getById(id);
    }
}
```

#### 前端步骤

1. **定义TypeScript类型**
```typescript
// types/model.d.ts
export interface NewEntity {
  id: number
  name: string
  createTime: string
  updateTime: string
}
```

2. **创建API接口**
```typescript
// api/modules/new.ts
import request from '@/utils/request'
import type { NewEntity } from '@/types/model'

export function getNewById(id: number) {
  return request.get<any, NewEntity>(`/new/${id}`)
}
```

3. **创建页面组件**
```vue
<!-- views/new/detail.vue -->
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getNewById } from '@/api/modules/new'
import type { NewEntity } from '@/types/model'

const route = useRoute()
const entity = ref<NewEntity | null>(null)
const loading = ref(false)

onMounted(async () => {
  const id = Number(route.params.id)
  loading.value = true
  try {
    entity.value = await getNewById(id)
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div v-loading="loading">
    <h1>{{ entity?.name }}</h1>
  </div>
</template>
```

4. **配置路由**
```typescript
// router/modules/new.ts
const newRoutes = [
  {
    path: '/new/:id',
    name: 'NewDetail',
    component: () => import('@/views/new/detail.vue'),
    meta: { title: '详情' }
  }
]
```

### 示例2：添加新的状态管理

```typescript
// stores/new.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { NewEntity } from '@/types/model'
import { getNewById, getNewList } from '@/api/modules/new'

export const useNewStore = defineStore('new', () => {
  const currentEntity = ref<NewEntity | null>(null)
  const entityList = ref<NewEntity[]>([])
  const loading = ref(false)
  
  const entityCount = computed(() => entityList.value.length)
  
  async function fetchEntity(id: number) {
    loading.value = true
    try {
      currentEntity.value = await getNewById(id)
    } finally {
      loading.value = false
    }
  }
  
  async function fetchEntityList() {
    loading.value = true
    try {
      entityList.value = await getNewList()
    } finally {
      loading.value = false
    }
  }
  
  return {
    currentEntity,
    entityList,
    loading,
    entityCount,
    fetchEntity,
    fetchEntityList
  }
})
```

## 调试技巧

### 前端调试

1. **使用Vue Devtools**
   - 安装浏览器扩展
   - 查看组件树和状态
   - 调试Pinia状态

2. **使用浏览器DevTools**
   - 查看网络请求
   - 调试JavaScript代码
   - 检查元素样式

3. **日志输出**
```typescript
console.log('调试信息:', data)
console.error('错误信息:', error)
console.table(arrayData)
```

### 后端调试

1. **使用IDE调试器**
   - 设置断点
   - 单步执行
   - 查看变量值

2. **日志输出**
```java
log.debug("调试信息: {}", data);
log.error("错误信息: {}", error);
```

3. **API文档测试**
   - 访问 http://localhost:8080/doc.html
   - 使用Knife4j测试API接口

## 常见问题解决

### 1. 依赖安装失败
```bash
# 清除缓存
pnpm store prune

# 重新安装
pnpm install
```

### 2. 端口被占用
```bash
# 查找占用端口的进程
netstat -ano | findstr :5173

# 终止进程
taskkill /PID <进程ID> /F
```

### 3. 数据库连接失败
- 检查MySQL服务是否启动
- 检查数据库配置是否正确
- 检查用户权限是否足够

### 4. 编译错误
- 检查TypeScript类型定义
- 检查导入路径是否正确
- 检查语法错误

## 代码规范检查

### 前端代码检查
```bash
# 类型检查
pnpm type-check

# 代码规范检查
pnpm lint

# 自动修复
pnpm lint:fix
```

### 后端代码检查
```bash
# 编译检查
mvn compile

# 运行测试
mvn test
```

## 版本控制

### Git工作流

1. **创建功能分支**
```bash
git checkout -b feature/功能名称
```

2. **提交代码**
```bash
git add .
git commit -m "feat: 添加新功能"
```

3. **推送分支**
```bash
git push origin feature/功能名称
```

4. **创建Pull Request**
- 在GitHub/GitLab上创建PR
- 填写PR描述
- 请求代码审查

5. **合并代码**
- 审查通过后合并到主分支
- 删除功能分支

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

## 部署指南

### 前端部署

1. **构建生产版本**
```bash
pnpm build
```

2. **部署到Nginx**
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
    }
}
```

### 后端部署

1. **打包应用**
```bash
mvn clean package -DskipTests
```

2. **运行应用**
```bash
java -jar target/moyuan-backend.jar --spring.profiles.active=prod
```

3. **使用Docker部署**
```dockerfile
FROM openjdk:17-slim
COPY target/sc-moyuan-backend.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## 相关资源

- [Vue 3官方文档](https://vuejs.org/)
- [Vite官方文档](https://vitejs.dev/)
- [Pinia官方文档](https://pinia.vuejs.org/)
- [Element Plus官方文档](https://element-plus.org/)
- [Spring Boot官方文档](https://spring.io/projects/spring-boot)
- [MyBatis-Plus官方文档](https://baomidou.com/)

---

**文档版本**：v1.1
**最后更新**：2026-05-28
**维护人员**：墨渊开发团队