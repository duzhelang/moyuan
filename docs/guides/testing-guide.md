# 后端测试指南

## 概述

本文档定义"古今诗话——墨渊"项目后端（`sc-moyuan-backend/`）的测试体系，包括测试技术栈、目录分层、测试环境配置、用例清单与常见问题，供开发与维护参考。

> 测试代码位于 `sc-moyuan-backend/src/test/`，通过 `mvn test` 运行。

## 测试技术栈

| 技术 | 说明 |
|------|------|
| JUnit 5（Jupiter） | 测试框架，含 `@Test`、`@ExtendWith`、`@TestMethodOrder` 等 |
| Mockito | 单元测试 Mock 框架 |
| spring-boot-starter-test | Spring Boot 测试脚手架（含 JUnit5、Mockito、MockMvc、AssertJ 等） |
| spring-security-test | Spring Security 测试支持（`@WithMockUser` 等） |
| H2 | 测试用内存数据库（`jdbc:h2:mem`，MySQL 兼容模式） |

> 测试依赖由父工程 `spring-boot-starter-parent`（3.2.5）统一管理，无需单独指定 JUnit/Mockito 版本。

## 测试分层与目录结构

测试按职责划分为三层：单元测试（Service）、切片测试（Controller）、集成测试（Database）。

```
src/test/
├── java/com/moyuan/
│   ├── controller/          # @WebMvcTest 切片测试（Controller 层）
│   │   ├── AuthControllerTest.java
│   │   ├── CategoryControllerTest.java
│   │   ├── DynastyControllerTest.java
│   │   ├── PoemControllerTest.java
│   │   └── UserControllerTest.java
│   ├── service/             # Mockito 单元测试（Service 层）
│   │   ├── CategoryServiceImplTest.java
│   │   ├── DynastyServiceImplTest.java
│   │   ├── PoemServiceImplTest.java
│   │   └── UserServiceImplTest.java
│   └── integration/         # @SpringBootTest 集成测试（真实 H2 数据源）
│       └── CategoryIntegrationTest.java
└── resources/
    ├── application-test.yml # 测试环境配置（H2、JWT 固定值）
    └── db/
        ├── schema-h2.sql    # 测试库建表脚本
        └── data-h2.sql      # 测试初始化数据
```

### 1. 单元测试（Service）

- 使用 `@ExtendWith(MockitoExtension.class)` + `@Mock` / `@InjectMocks`，不加载 Spring 上下文，运行最快。
- Mapper、工具类等外部依赖全部 Mock。
- 注意：MyBatis-Plus `ServiceImpl` 的基类字段 `baseMapper` 不会被 Mockito 自动注入，需在 `setUp()` 中用 `ReflectionTestUtils.setField(service, "baseMapper", mapper)` 手动注入。

### 2. 切片测试（Controller）

- 使用 `@WebMvcTest(XxxController.class)`，仅加载目标 Controller 及安全过滤器。
- 所有 Service 依赖通过 `@MockBean` 提供，并 `@MockBean` 掉 `JwtUtil` 与 `UserService`（`JwtAuthenticationFilter` 依赖）。
- 统一添加 `@AutoConfigureMockMvc(addFilters = false)` 关闭安全过滤器，避免默认 Spring Security 拦截（未认证 401 / CSRF 403）。
- 通过 `properties = "app.mapper-scan-enabled=false"` 关闭 Mapper 扫描，避免无 `sqlSessionFactory` 时创建 Mapper Bean 失败。
- 涉及 `SecurityUtil.getCurrentUserId()` 的用例，需在 `@BeforeEach` 中向 `SecurityContextHolder` 注入 `LoginUser` 类型的认证对象。

### 3. 集成测试（Database）

- 使用 `@SpringBootTest` + `@ActiveProfiles("test")`，加载完整上下文与 H2 内存库。
- 数据由 `db/schema-h2.sql` 与 `db/data-h2.sql` 在启动时初始化。
- 启动时会触发 AI 模型缓存初始化（查询 `ai_model` 表），若测试 schema 不含相关表，用 `@MockBean AiModelRegistry` 跳过副作用。
- 测试数据通过 `@TestMethodOrder` + `@Order` 保证执行顺序，删除类用例使用临时插入的数据，避免污染 seed 数据。

## 测试环境配置

### application-test.yml（`src/test/resources/`）

| 配置项 | 值 | 说明 |
|--------|-----|------|
| `spring.datasource.url` | `jdbc:h2:mem:moyuan_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;NON_KEYWORDS=USER,COMMENT,VALUE` | H2 内存库，MySQL 兼容模式 |
| `spring.sql.init.mode` | `always` | 每次启动执行初始化脚本 |
| `spring.sql.init.schema-locations` | `classpath:db/schema-h2.sql` | 建表脚本 |
| `spring.sql.init.data-locations` | `classpath:data-h2.sql` | 初始化数据 |
| `jwt.secret` / `jwt.expiration` | 固定测试值 | JWT 测试配置 |
| `apihz.id` / `apihz.key` | `test` | 第三方 API 占位值 |

> H2 中 `USER`、`COMMENT`、`VALUE` 为保留关键字，需在 JDBC URL 中以 `NON_KEYWORDS` 声明，否则建表语句（`CREATE TABLE IF NOT EXISTS user`）会报语法错误。

### Mapper 扫描开关（`app.mapper-scan-enabled`）

主类 `MoyuanApplication` 不再直接标注 `@MapperScan`，改由条件配置类 [MapperScanConfig.java](../../sc-moyuan-backend/src/main/java/com/moyuan/config/MapperScanConfig.java) 控制：

```java
@Configuration
@ConditionalOnProperty(name = "app.mapper-scan-enabled", havingValue = "true", matchIfMissing = true)
@MapperScan("com.moyuan.mapper")
public class MapperScanConfig { }
```

- 生产环境：属性缺省即 `matchIfMissing = true`，正常扫描 Mapper。
- `@WebMvcTest` 切片测试：`@WebMvcTest(properties = "app.mapper-scan-enabled=false")` 关闭扫描，避免无数据源上下文创建 Mapper Bean 失败。

## 测试用例清单

### Controller 切片测试（共 14 例）

| 测试类 | 用例 | 场景 |
|--------|------|------|
| AuthControllerTest | `login_成功返回token` | 登录成功返回 Token |
| | `register_成功注册` | 注册成功返回 Token |
| CategoryControllerTest | `getAllCategories_返回分类列表` | 分类列表 |
| | `getCategoryDetail_返回分类详情` | 分类详情 |
| DynastyControllerTest | `getAllDynasties_返回朝代列表` | 朝代列表 |
| | `getDynastyDetail_返回朝代详情` | 朝代详情 |
| | `getAllDynasties_空列表返回空数组` | 空列表 |
| PoemControllerTest | `getPoemList_返回分页结果` | 诗词分页 |
| | `getPoemList_带筛选条件` | 按朝代/诗人/分类/关键词筛选 |
| | `getPoemDetail_返回诗词详情` | 诗词详情 |
| | `getDailyPoems_返回每日推荐` | 每日推荐 |
| | `getModernPoems_返回现代诗词分页` | 现代诗词分页 |
| UserControllerTest | `getUserInfo_返回用户信息` | 按 ID 获取用户 |
| | `updateUser_成功更新` | 更新当前用户（LoginUser 上下文） |

### Service 单元测试（共 31 例）

| 测试类 | 用例 | 场景 |
|--------|------|------|
| CategoryServiceImplTest | `getAllCategories_返回排序后的分类列表` | 分类按 sortOrder 排序 |
| | `getCategoryDetail_存在时返回分类` | 分类详情 |
| | `getCategoryDetail_不存在时抛出异常` | 分类不存在 |
| DynastyServiceImplTest | `getAllDynasties_返回排序后的朝代列表` | 朝代列表排序 |
| | `getAllDynasties_空列表返回空` | 空列表 |
| | `getDynastyDetail_存在时返回朝代` | 朝代详情 |
| | `getDynastyDetail_不存在时抛出异常` | 朝代不存在 |
| PoemServiceImplTest | `getPoemList_返回分页结果` / `_带筛选条件` | 诗词分页与筛选 |
| | `getPoemDetail_存在/不存在/未审核` | 详情与异常分支 |
| | `toggleLike_未点赞时添加点赞` / `_已点赞时取消点赞` | 点赞/取消（LambdaUpdateWrapper） |
| | `isLike_已点赞返回true` / `_未点赞返回false` | 点赞状态 |
| | `getFavorites_有收藏/无收藏` | 收藏分页 |
| | `createPoemWithAudit_设置待审核状态` | 新建诗进入待审核 |
| | `auditPoem_审核通过/拒绝/不存在` | 审核逻辑 |
| UserServiceImplTest | `login_成功返回token` / `_用户名不存在` / `_密码错误` | 登录与异常分支 |
| | `register_成功注册` / `_用户名已存在` | 注册与异常分支 |
| | `getUserById_存在时返回用户` / `getUserInfo_不存在时抛出异常` | 用户信息 |
| | `updateUser_成功更新` / `_用户不存在` | 更新用户 |

### 集成测试（共 5 例）

| 测试类 | 用例 | 场景 |
|--------|------|------|
| CategoryIntegrationTest | `查询所有分类` | H2 真实查询分类列表 |
| | `根据ID查询分类` | 按主键查询 |
| | `插入新分类` | 新增并回查 |
| | `更新分类` | 修改描述 |
| | `删除分类` | 临时数据删除 |

## 运行方式

```bash
# 在 sc-moyuan-backend/ 目录执行全量测试
mvn test

# 运行单个测试类
mvn test -Dtest=PoemServiceImplTest

# 运行单个用例
mvn test -Dtest=PoemServiceImplTest#toggleLike_未点赞时添加点赞

# 跳过测试打包
mvn package -DskipTests
```

测试报告输出至 `sc-moyuan-backend/target/surefire-reports/`。

## 常见问题与处理

| 问题 | 原因 | 处理 |
|------|------|------|
| `@WebMvcTest` 报 `Property 'sqlSessionFactory' or 'sqlSessionTemplate' are required` | 主类 `@MapperScan` 在无数据源切片上下文中注册 Mapper Bean | 使用 `MapperScanConfig` 条件配置，测试通过 `app.mapper-scan-enabled=false` 关闭扫描 |
| `@WebMvcTest` 报 `No qualifying bean of type 'UserService'` | `JwtAuthenticationFilter`（@Component Filter）依赖 `UserService` | 在测试中 `@MockBean UserService` |
| Controller 测试返回 401/403 | 默认 Spring Security 拦截所有请求（未认证 401，POST/PUT 触发 CSRF 403） | 添加 `@AutoConfigureMockMvc(addFilters = false)` |
| `SecurityUtil.getCurrentUserId()` 抛 `UNAUTHORIZED` | 安全上下文 principal 非 `LoginUser` 类型 | `@BeforeEach` 中手动向 `SecurityContextHolder` 注入 `LoginUser` 认证对象 |
| H2 报 `Syntax error ... expected "identifier"`（user 表） | H2 中 `USER` 为保留关键字 | JDBC URL 添加 `NON_KEYWORDS=USER,COMMENT,VALUE` |
| `verify(poemMapper).update(any(), any(LambdaQueryWrapper.class))` 失败 | `PoemServiceImpl.toggleLike` 实际使用 `LambdaUpdateWrapper` | verify 断言改用 `LambdaUpdateWrapper` |
| `getBaseMapper()` 返回 null | Mockito 未注入 MyBatis-Plus `ServiceImpl` 基类字段 | `setUp()` 用 `ReflectionTestUtils.setField(service, "baseMapper", mapper)` 注入 |

---

**最后更新**：2026-09-04
**版本号**：1.0.0
