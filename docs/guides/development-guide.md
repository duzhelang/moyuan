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
# 进入完整版后端目录（包含合并版 init.sql）
cd sc-moyuan-backend

# 执行初始化脚本（自动创建数据库、建表、导入初始数据，共29张表）
mysql -u root -p < src/main/resources/db/init.sql
```

> **说明**：`init.sql` 是全量合并版脚本（v3.0），包含数据库创建、全部29张表建表语句、初始数据（13个朝代、74个分类、6个诗人、15首诗词、3个测试用户、27个AI模型、6位精选诗人、24条首页导航）。详细说明见 `sc-moyuan-backend/src/main/resources/db/数据库初始化说明文档.md`。

#### 6. 配置敏感信息

项目使用分层配置管理敏感信息：

> **兜底说明**：`application.yml` 中 `MYSQL_PASSWORD`、`JWT_SECRET`、`APIHZ_ID`、`APIHZ_KEY` 等环境变量占位符均带有默认值兜底，未配置环境变量时应用也能正常启动（本地开发）。但默认值仅用于本地开发兜底，**生产环境必须通过环境变量或 `secrets/application-secrets.yml` 覆盖**，尤其是 `JWT_SECRET`（默认密钥为公开的开发用值，仅作本地启动使用）。

**配置文件结构**：
```
src/main/resources/
├── application.yml           # 主配置（通用配置 + 环境变量引用）
├── application-dev.yml       # 开发环境（Redis连接池）
├── application-prod.yml      # 生产环境（数据库、Redis使用环境变量）
└── secrets/                  # 敏感配置目录（.gitignore 排除）
    └── application-secrets.yml  # 真实密钥配置
```

**配置 secrets/application-secrets.yml**：

复制示例文件并填入真实密钥：
```bash
cp src/main/resources/secrets/application-secrets.yml.example src/main/resources/secrets/application-secrets.yml
```

配置内容：
```yaml
# 数据库密码
MYSQL_PASSWORD: your_mysql_password

# JWT 密钥
JWT_SECRET: your_jwt_secret_key_at_least_256_bits

# 第三方 API
APIHZ_ID: your_apihz_id
APIHZ_KEY: your_apihz_key

# AI 模型 API Key
ZHIPU_API_KEY: your_zhipu_api_key
DEEPSEEK_API_KEY: your_deepseek_api_key
KIMI_API_KEY: your_kimi_api_key
MIMO_API_KEY: your_mimo_api_key
NVIDIA_API_KEY: your_nvidia_api_key
QWEN_API_KEY: your_qwen_api_key
OPENROUTER_API_KEY: your_openrouter_api_key
```

> **注意**：`secrets/` 目录已在 `.gitignore` 中排除，不会提交到版本控制。

> **⚠️ YAML 数值陷阱**：以 `0` 开头的纯数字字符串（如密码 `010125`）在 YAML 中会被 SnakeYAML 按**八进制**解析（`010125` → `4181`），导致数据库连接报 `Access denied`。密码、账号等数值型字符串**必须加引号**强制按字符串处理：
> ```yaml
> # 错误：010125 被解析为八进制整数 4181
> spring:
>   datasource:
>     password: 010125
> 
> # 正确：加引号按字符串处理
> spring:
>   datasource:
>     password: "010125"
> ```
> 排查技巧：若 JDBC 直连可通但应用报 `Access denied`，可用启动参数 `--spring.datasource.password=xxx` 临时覆盖验证是否为 YAML 解析问题。

#### 7. 启动后端服务
```bash
# 启动后端（sc-moyuan-backend 为当前唯一后端，包含安全认证、JWT、Druid、Knife4j 等完整功能）
cd sc-moyuan-backend
mvn spring-boot:run
```

> **后端模块说明**：
> - ⚠️ **backend/**：基础版后端目录已合并入 `sc-moyuan-backend/`，当前项目根目录下不再存在该模块，相关启动方式（`cd backend`）已失效
> - **sc-moyuan-backend/**：当前唯一后端，包含AuthController、ForumController、AiController、AiModelConfigController、AdminController、SearchController、FileController、HistoryController等，已实现全部核心功能

访问 http://localhost:8085/doc.html 查看API文档（Knife4j，仅完整版后端）

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

项目当前仅包含一个后端模块：

> ⚠️ **backend/（基础版后端）**：已合并入 `sc-moyuan-backend/`，该目录在项目根目录下已不存在。原结构（PoemController/CategoryMapper/DynastyMapper 等基础CRUD）已全部并入完整版后端。

#### sc-moyuan-backend/（完整版后端，当前唯一后端）

```
sc-moyuan-backend/
├── src/main/java/com/moyuan/
│   ├── config/                 # 配置类（CorsConfig, MyBatisPlusConfig, RedisConfig, SecurityConfig, SwaggerConfig）
│   ├── controller/             # 控制器层（AuthController, ForumController, AiController, AiModelConfigController, AdminController, SearchController, FileController, HistoryController 等）
│   ├── common/                 # 公共类（R.java, ResultCode.java, PageRequest.java）
│   ├── aspect/                 # 切面（OperationLogAspect）
│   ├── service/                # 服务层（AiModelConfigService 等）
│   ├── mapper/                 # Mapper层（AiModelMapper 等）
│   ├── entity/                 # 实体类（AiModel 等）
│   ├── dto/                    # 数据传输对象
│   ├── exception/              # 异常处理
│   ├── security/               # 安全相关（JWT等）
│   └── util/                   # 工具类
├── src/main/resources/
│   ├── application.yml         # 主配置文件（含数据库、JWT、跨域等）
│   ├── application-dev.yml     # 开发环境配置（Redis）
│   ├── application-prod.yml    # 生产环境配置
│   ├── secrets/                # 敏感配置目录（已在.gitignore中排除）
│   │   └── application-secrets.yml # AI模型API Key配置
│   ├── db/
│   │   ├── init.sql            # 全量初始化脚本（含全部29张表+初始数据）
│   │   └── 数据库初始化说明文档.md # 数据库初始化说明
│   └── mapper/
│       └── StatsMapper.xml     # 统计相关MyBatis映射
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
   - 访问 http://localhost:8085/doc.html
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

## 测试指南

### 前端测试

#### 测试框架
- **单元测试**：Vitest
- **组件测试**：@vue/test-utils
- **覆盖率**：@vitest/coverage-v8

#### 运行测试
```bash
# 运行所有测试
pnpm test

# 运行测试并监听变化
pnpm test --watch

# 运行测试并生成覆盖率报告
pnpm test:coverage

# 运行测试UI界面
pnpm test:ui
```

#### 测试文件规范
- 测试文件命名：`*.test.ts` 或 `*.spec.ts`
- 测试文件位置：与被测试文件同目录
- 测试目录结构：
  ```
  src/
  ├── utils/
  │   ├── format.ts
  │   └── format.test.ts
  ├── components/
  │   └── common/
  │       ├── LoadingSpinner.vue
  │       └── LoadingSpinner.test.ts
  └── stores/
      ├── app.ts
      └── app.test.ts
  ```

#### 测试示例

**工具函数测试**：
```typescript
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

**组件测试**：
```typescript
import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import LoadingSpinner from './LoadingSpinner.vue'

describe('LoadingSpinner', () => {
  it('loading为true时渲染', () => {
    const wrapper = mount(LoadingSpinner, {
      props: { loading: true }
    })
    expect(wrapper.find('.loading-spinner').exists()).toBe(true)
  })
})
```

**Store测试**：
```typescript
import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAppStore } from './app'

describe('useAppStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('初始状态', () => {
    const store = useAppStore()
    expect(store.cachedViews).toEqual([])
  })
})
```

### 后端测试

#### 测试框架
- **单元测试**：JUnit 5
- **Mock框架**：Mockito
- **集成测试**：Spring Boot Test
- **测试数据库**：H2（内存数据库）

#### 运行测试
```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=CategoryControllerTest

# 运行测试并生成覆盖率报告
mvn test jacoco:report
```

#### 测试文件规范
- 测试类命名：`*Test.java` 或 `*Tests.java`
- 测试目录：`src/test/java/com/moyuan/`
- 测试资源：`src/test/resources/`
- 测试目录结构：
  ```
  src/test/
  ├── java/com/moyuan/
  │   ├── controller/
  │   │   └── CategoryControllerTest.java
  │   ├── service/
  │   │   └── CategoryServiceImplTest.java
  │   └── integration/
  │       └── CategoryIntegrationTest.java
  └── resources/
      ├── application-test.yml
      ├── db/
      │   └── schema-h2.sql
      └── data-h2.sql
  ```

#### 测试示例

**Controller单元测试**：
```java
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

**Service单元测试**：
```java
@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void getAllCategories_返回分类列表() {
        when(categoryMapper.selectList(any())).thenReturn(Arrays.asList(new Category()));

        List<Category> categories = categoryService.getAllCategories();

        assertNotNull(categories);
        assertEquals(1, categories.size());
    }
}
```

**集成测试**：
```java
@DataJpaTest
@ActiveProfiles("test")
class CategoryIntegrationTest {

    @Autowired
    private CategoryMapper categoryMapper;

    @Test
    void 查询所有分类() {
        List<Category> categories = categoryMapper.selectList(null);
        assertNotNull(categories);
        assertEquals(4, categories.size());
    }
}
```

### 测试最佳实践

1. **测试覆盖率目标**
   - 单元测试覆盖率：≥80%
   - 关键业务逻辑：100%覆盖

2. **测试命名规范**
   - 使用描述性名称：`方法名_场景_预期结果`
   - 示例：`getAllCategories_返回分类列表`

3. **测试数据管理**
   - 使用H2内存数据库进行集成测试
   - 测试数据独立，不依赖外部环境
   - 每个测试用例数据隔离

4. **Mock使用原则**
   - 外部依赖使用Mock
   - 数据库操作使用真实数据库（集成测试）
   - 避免过度Mock

5. **测试维护**
   - 测试代码与业务代码同等重要
   - 定期重构测试代码
   - 保持测试简洁明了

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
        proxy_pass http://localhost:8085;
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
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## AI 模型配置

### 模型列表

项目支持以下 AI 模型（共 27 个）：

| 提供商 | 模型 | 类型 | 状态 |
|--------|------|------|------|
| 智谱AI | glm-4, glm-4.7-flash | 文本+视觉 | 默认启用 |
| DeepSeek | deepseek-chat, deepseek-v3 | 文本 | 默认禁用（需配置密钥） |
| Kimi | moonshot-v1-8k, kimi-k2.6 | 文本 | 默认禁用（需配置密钥） |
| 小米MiMo | mimo-v2.5 | 文本 | 默认禁用（需配置密钥） |
| 千问 | qwen3.6-plus | 文本 | 默认禁用（需配置密钥） |
| NVIDIA NIM | 12 个模型 | 文本 | 默认禁用（需配置密钥） |
| OpenRouter | 7 个免费模型 | 文本（免费） | 默认禁用（需配置密钥） |

### NVIDIA NIM 模型

NVIDIA NIM 平台提供以下模型（使用统一 API Key）：

| 模型 | 模型ID |
|------|--------|
| Llama 4 Scout | meta/llama-4-scout |
| Llama 4 Maverick | meta/llama-4-maverick |
| GLM-5 | zai-org/glm-5 |
| GLM-5.1 | zai-org/glm-51 |
| DeepSeek-V4 Flash | deepseek-ai/deepseek-v4-flash |
| DeepSeek-V4 Pro | deepseek-ai/deepseek-v4-pro |
| Kimi-K2.5 | moonshotai/kimi-k2.5 |
| Kimi-K2.6 | moonshotai/kimi-k2.6 |
| MiniMax-M2.5 | minimax-ai/minimax-m25 |
| Qwen3.5 | qwen/qwen3.5-397b-a17b |
| Qwen3 Next | qwen/qwen3-next-80b-a3b-instruct |
| Qwen3 Coder | qwen/qwen3-coder-next |

### OpenRouter 免费模型

OpenRouter 提供以下免费模型（共 7 个，使用统一 API Key）：

| 模型 | 模型ID |
|------|--------|
| DeepSeek-V4 (免费) | deepseek/deepseek-v4-flash:free |
| Kimi-K2.6 (免费) | moonshotai/kimi-k2.6:free |
| Gemma-4 (免费) | google/gemma-4-26b-a4b-it:free |
| Qwen3 Next (免费) | qwen/qwen3-next-80b-a3b-instruct:free |
| Qwen3 Coder (免费) | qwen/qwen3-coder:free |
| MiniMax-M2.5 (免费) | minimax/minimax-m2.5:free |
| Llama 4 Scout (免费) | meta-llama/llama-4-scout:free |

### API 端点

| 提供商 | API 端点 |
|--------|----------|
| 智谱AI | https://open.bigmodel.cn/api/paas/v4/chat/completions |
| DeepSeek | https://api.deepseek.com/v1/chat/completions |
| Kimi | https://api.moonshot.cn/v1/chat/completions |
| 小米MiMo | https://api.xiaomimimo.com/v1/chat/completions |
| 千问 | https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions |
| NVIDIA NIM | https://integrate.api.nvidia.com/v1/chat/completions |
| OpenRouter | https://openrouter.ai/api/v1/chat/completions |

### 测试 API 连通性

运行测试脚本验证 API 配置：
```powershell
powershell -ExecutionPolicy Bypass -File test-apis.ps1
```

## 相关资源

- [Vue 3官方文档](https://vuejs.org/)
- [Vite官方文档](https://vitejs.dev/)
- [Pinia官方文档](https://pinia.vuejs.org/)
- [Element Plus官方文档](https://element-plus.org/)
- [Spring Boot官方文档](https://spring.io/projects/spring-boot)
- [MyBatis-Plus官方文档](https://baomidou.com/)

---

**文档版本**：v1.4
**最后更新**：2026-08-29（更新数据库表数量为29张、初始数据数量、AI模型列表为27个、后端端口为8085）
**维护人员**：墨渊开发团队