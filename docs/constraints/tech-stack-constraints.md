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
| Java | 17+（编译兼容模式，source/target=17） | pom.xml 中 java.version=23，maven-compiler-plugin source/target=17，确保 Java 17+ LTS 可运行 |
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

## 代码规范限制

### Composition API 使用规范

#### 必须使用的特性

| 特性 | 用途 | 示例 |
|------|------|------|
| `<script setup>` | 组件语法糖 | 简化组件代码，自动暴露顶层变量 |
| `ref()` | 响应式基础类型 | `const count = ref(0)` |
| `reactive()` | 响应式对象 | `const state = reactive({})` |
| `computed()` | 计算属性 | `const double = computed(() => count.value * 2)` |
| `watch()` | 侦听器 | `watch(source, callback)` |
| `watchEffect()` | 副作用追踪 | `watchEffect(() => {})` |
| `defineProps()` | 组件属性 | `const props = defineProps<{}>()` |
| `defineEmits()` | 组件事件 | `const emit = defineEmits<{}>()` |
| `defineExpose()` | 暴露方法 | `defineExpose({ method })` |

#### 禁止使用的特性

| 特性 | 原因 | 替代方案 |
|------|------|----------|
| Options API | 统一使用Composition API | 使用 `<script setup>` |
| `this` | Composition API中无this | 使用ref/reactive变量 |
| `data()` | 使用ref/reactive替代 | `const count = ref(0)` |
| `computed:{}` | 使用computed函数 | `const double = computed(...)` |
| `methods:{}` | 直接声明函数 | `function method() {}` |
| `watch:{}` | 使用watch函数 | `watch(source, callback)` |

#### 代码组织规范

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
const state = reactive({
  name: '',
  age: 0
})

// 5. 计算属性
const doubleCount = computed(() => count.value * 2)
const fullName = computed(() => `${state.name} - ${state.age}`)

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

onUnmounted(() => {
  console.log('组件卸载')
})
</script>
```

#### Composables 函数规范

```typescript
// 推荐：使用Composables封装可复用逻辑
// composables/useCounter.ts
import { ref, computed } from 'vue'

export function useCounter(initialValue = 0) {
  const count = ref(initialValue)
  const doubleCount = computed(() => count.value * 2)
  
  function increment() {
    count.value++
  }
  
  function decrement() {
    count.value--
  }
  
  function reset() {
    count.value = initialValue
  }
  
  return {
    count,
    doubleCount,
    increment,
    decrement,
    reset
  }
}

// 使用
const { count, increment } = useCounter(10)
```

### 前端代码规范

```typescript
// 必须：使用TypeScript严格模式
// tsconfig.json
{
  "compilerOptions": {
    "strict": true,
    "noImplicitAny": true,
    "strictNullChecks": true,
    "strictFunctionTypes": true
  }
}

// 必须：使用Composition API
// ✅ 正确
import { ref, computed } from 'vue'

const count = ref(0)
const doubleCount = computed(() => count.value * 2)

// ❌ 错误
export default {
  data() {
    return { count: 0 }
  },
  computed: {
    doubleCount() {
      return this.count * 2
    }
  }
}
```

### 后端代码规范

```java
// 必须：使用Lombok注解
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
}
```java
// 必须：统一异常处理
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public R<?> handleBusinessException(BusinessException e) {
        return R.error(e.getCode(), e.getMessage());
    }
}

// 必须：使用参数化查询
// ✅ 正确
@Select("SELECT * FROM user WHERE id = #{id}")
User selectById(Long id);

// ❌ 错误（SQL注入风险）
@Select("SELECT * FROM user WHERE id = " + id)
User selectById(Long id);
```

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
| 敏感数据 | 禁止 | 不存储在LocalStorage |
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

## 代码审查检查清单

### 前端检查项

- [ ] 是否使用TypeScript严格模式
- [ ] 是否使用Composition API
- [ ] 是否按需引入Element Plus组件
- [ ] 是否处理接口错误
- [ ] 是否添加Loading状态
- [ ] 是否优化图片加载
- [ ] 是否使用路由懒加载

### 后端检查项

- [ ] 是否使用参数化查询
- [ ] 是否添加接口文档注解
- [ ] 是否处理业务异常
- [ ] 是否添加日志记录
- [ ] 是否验证请求参数
- [ ] 是否使用事务管理
- [ ] 是否优化SQL查询

---

**文档版本**：v1.2
**最后更新**：2026-05-29
**维护人员**：墨渊开发团队