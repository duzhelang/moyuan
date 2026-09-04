# 代码审查详细报告

> **项目名称：** 古今诗话——墨渊（SC_MoYuan2）
> **审查日期：** 2026-06-06
> **审查范围：** 诗人认证与创作功能增强
> **审查代理：** code-reviewer

---

## 审查范围

| 层级 | 文件数量 | 审查内容 |
|------|----------|----------|
| 数据库 | 1 | init.sql |
| 后端实体 | 4 | PoetProfile.java, PoemRating.java, User.java, Poem.java |
| 后端Mapper | 2 | PoetProfileMapper.java, PoemRatingMapper.java |
| 后端Service | 4 | PoetProfileService.java, PoetProfileServiceImpl.java, PoemRatingService.java, PoemRatingServiceImpl.java |
| 后端Controller | 4 | PoetProfileController.java, PoemRatingController.java, AdminController.java, UserController.java |
| 前端页面 | 5 | homepage.vue, list.vue, detail.vue, create.vue, PoemCard.vue |
| 前端API | 2 | user.ts, poem.ts |
| 前端类型 | 2 | model.d.ts, api.d.ts |
| 前端路由 | 1 | router/index.ts |

---

## 一、严重问题（必须修复）

### 1.1 分层架构违规 - AdminController 直接注入 Mapper

**文件：** `sc-moyuan-backend/src/main/java/com/moyuan/controller/AdminController.java` (L36-L49)

**问题描述：** Controller 直接注入多个 Mapper（PoemMapper, PoetMapper, CategoryMapper, DynastyMapper 等），绕过 Service 层直接操作数据库。

**违反规范：** Controller → Service（接口+impl）→ Mapper，不允许跨层调用

**影响：** 
- 业务逻辑分散在 Controller 层，难以维护
- 无法利用 Service 层的事务管理
- 代码复用性差

**修复建议：**
```java
// 错误示例
@Autowired
private PoemMapper poemMapper;

// 正确示例
@Autowired
private PoemService poemService;

// 将 poemMapper.selectCount(null) 改为 poemService.count()
// 将 poemMapper.selectPage(...) 改为 poemService.page(...)
```

---

### 1.2 分层架构违规 - Service 层直接注入 Mapper

**文件：** 
- `sc-moyuan-backend/src/main/java/com/moyuan/service/impl/PoetProfileServiceImpl.java` (L20-L21)
- `sc-moyuan-backend/src/main/java/com/moyuan/service/impl/PoemRatingServiceImpl.java` (L27)

**问题描述：** Service 实现类直接注入 UserMapper 和 PoemMapper，违反分层架构。

**修复建议：**
```java
// 错误示例
private final UserMapper userMapper;

// 正确示例
private final UserService userService;

// 将 userMapper.selectById() 改为 userService.getById()
// 将 userMapper.updateById() 改为 userService.updateById()
```

---

### 1.3 Controller 异常处理缺失

**文件：** 
- `sc-moyuan-backend/src/main/java/com/moyuan/controller/PoetProfileController.java`
- `sc-moyuan-backend/src/main/java/com/moyuan/controller/PoemRatingController.java`

**问题描述：** Controller 缺少 try-catch 异常处理，所有方法直接返回结果，未捕获任何异常。

**违反规范：** Controller 必须使用 try-catch 并返回 `R.error()`

**修复建议：**
```java
@GetMapping("/me")
@Operation(summary = "获取当前用户诗人资料")
public R<PoetProfile> getMyProfile(@AuthenticationPrincipal UserDetailsImpl userDetails) {
    try {
        PoetProfile profile = poetProfileService.getByUserId(userDetails.getUser().getId());
        return R.ok(profile);
    } catch (Exception e) {
        return R.error(e.getMessage());
    }
}
```

---

### 1.4 安全漏洞 - 评分接口无需认证

**文件：** `sc-moyuan-backend/src/main/java/com/moyuan/config/SecurityConfig.java` (L54-L55)

**问题描述：** `/api/poems/**` 被设为 `permitAll()`，导致评分接口 `/api/poems/{poemId}/ratings` 的 POST 写操作无需认证即可访问。

**影响：** 任何人都可以对诗词进行评分，存在恶意刷分风险

**修复建议：**
```java
// 将 /api/poems/** 从 permitAll 中移除
// 改为更精确的匹配规则
.requestMatchers(HttpMethod.GET, "/api/poems", "/api/poems/{id}").permitAll()
.requestMatchers("/api/poems/**").authenticated()
```

---

### 1.5 响应类 code 类型不符

**文件：** `sc-moyuan-backend/src/main/java/com/moyuan/common/R.java` (L7)

**问题描述：** 统一响应类 `R<T>` 的 `code` 字段为 `int` 类型，不符合规范要求的 `String` 类型。

**修复建议：**
```java
// 错误示例
private int code;

// 正确示例
private String code;

// 同步修改所有 setCode(200) 为 setCode("200")
```

**注意：** 此为全局基础类，修改影响范围大，需全面评估

---

### 1.6 TypeScript/JavaScript 规范冲突

**文件：** 
- `frontend/src/views/user/homepage.vue`
- `frontend/src/views/poem/list.vue`
- `frontend/src/views/poem/detail.vue`
- `frontend/src/views/poem/create.vue`
- `frontend/src/components/business/PoemCard.vue`

**问题描述：** 前端所有 Vue 组件和 TS 文件使用 TypeScript 语法，但项目规范明确要求使用纯 JavaScript。

**修复建议：**
- 如项目确实使用 TypeScript → 更新规范文档
- 如项目应使用纯 JS → 移除所有 `lang="ts"` 声明，将 `.ts` 文件改为 `.js` 文件

---

### 1.7 类型定义不完整

**文件：** `frontend/src/types/model.d.ts`

**问题描述：** User 接口缺少新增的 `poetVerified` 和 `poetProfileId` 字段。

**修复建议：**
```typescript
export interface User {
  // ... 现有字段
  poetVerified?: number
  poetProfileId?: number
}
```

---

## 二、一般问题（建议修复）

### 2.1 评分范围校验缺失

**文件：** `sc-moyuan-backend/src/main/java/com/moyuan/controller/PoemRatingController.java` (L33-L34)

**问题描述：** `ratePoem` 方法的 `score` 参数未做范围校验，数据库定义评分范围为 1.0-5.0，但接口未校验此范围。

**修复建议：**
```java
@PostMapping
public R<Void> ratePoem(@PathVariable Long poemId,
                        @AuthenticationPrincipal UserDetailsImpl userDetails,
                        @RequestParam BigDecimal score,
                        @RequestParam(required = false) String comment) {
    if (score.compareTo(BigDecimal.ONE) < 0 || score.compareTo(new BigDecimal("5")) > 0) {
        return R.error("评分范围应在 1.0-5.0 之间");
    }
    poemRatingService.ratePoem(poemId, userDetails.getUser().getId(), score, comment);
    return R.ok();
}
```

---

### 2.2 AI 评分解析逻辑脆弱

**文件：** `sc-moyuan-backend/src/main/java/com/moyuan/service/impl/PoemRatingServiceImpl.java` (L135-L148)

**问题描述：** `parseAiScore` 方法使用硬编码的字符串截取位置（+3, 取3个字符），非常脆弱，容易因 AI 返回格式变化而解析失败。

**修复建议：**
```java
private BigDecimal parseAiScore(String analysis) {
    // 使用正则表达式匹配评分数字
    Pattern pattern = Pattern.compile("评分[：:]\\s*(\\d+(\\.\\d+)?)");
    Matcher matcher = pattern.matcher(analysis);
    if (matcher.find()) {
        return new BigDecimal(matcher.group(1));
    }
    return new BigDecimal("3.5"); // 默认分
}
```

---

### 2.3 硬编码 SQL 片段

**文件：** `sc-moyuan-backend/src/main/java/com/moyuan/service/impl/PoemRatingServiceImpl.java` (L58-L62)

**问题描述：** `getAiRating` 方法使用 `.last("LIMIT 1")` 硬编码 SQL 片段，不具数据库可移植性。

**修复建议：** 使用 MyBatis-Plus 的 `Page(1, 1)` 方式替代

---

### 2.4 AI 模型名称硬编码

**文件：** `sc-moyuan-backend/src/main/java/com/moyuan/service/impl/PoemRatingServiceImpl.java` (L58-L85)

**问题描述：** `requestAiRating` 方法硬编码 AI 模型名称为 `"zhipu"`，缺乏灵活性。

**修复建议：** 将 AI 模型名称作为参数传入，或从配置中读取默认模型

---

### 2.5 前后端字段命名不一致

**文件：** `frontend/src/types/api.d.ts` (L2)

**问题描述：** 前端 `PoemRatingsData` 中的 `totalCount` 字段与后端返回的 `ratingCount` key 不一致。

**修复建议：** 统一前后端字段命名

---

### 2.6 并发竞态条件

**文件：** `sc-moyuan-backend/src/main/java/com/moyuan/service/impl/PoetProfileServiceImpl.java` (L58-L63)

**问题描述：** `incrementWorkCount` 和 `incrementLikeCount` 方法存在并发竞态条件，先读后写操作在高并发场景下可能导致数据不准确。

**修复建议：**
```java
// 使用 SQL 级别的原子更新
@Override
public void incrementWorkCount(Long userId) {
    baseMapper.incrementWorkCount(userId);
}

// 在 Mapper 中定义
@Update("UPDATE poet_profile SET work_count = work_count + 1 WHERE user_id = #{userId}")
void incrementWorkCount(@Param("userId") Long userId);
```

---

### 2.7 AI 评分 user_id 设计问题

**文件：** `sc-moyuan-backend/src/main/resources/db/init.sql` (L657)

**问题描述：** `poem_rating` 表中 AI 评分使用 `user_id = 0` 表示，而 0 不是有效的外键引用（若后续添加外键约束会失败）。

**修复建议：** 使用 `NULL` 表示 AI 评分的 user_id，或使用一个特殊的系统用户 ID

---

## 三、优化建议

### 3.1 敏感信息暴露

**文件：** `sc-moyuan-backend/src/main/java/com/moyuan/controller/PoetProfileController.java` (L37-L42)

**问题描述：** `getProfile` 接口通过 userId 查询诗人资料，任何已认证用户都可查看其他用户的诗人资料（包含联系方式 contactInfo 等敏感信息）。

**建议：** 增加数据脱敏或权限控制

---

### 3.2 性能问题 - 内存计算

**文件：** `sc-moyuan-backend/src/main/java/com/moyuan/service/impl/PoemRatingServiceImpl.java` (L88-L105)

**问题描述：** `getPoemRatings` 方法查询所有评分记录到内存中再计算，当评分数据量大时会有性能问题。

**建议：** 使用数据库聚合函数直接计算

---

### 3.3 性能问题 - 趋势统计

**文件：** `sc-moyuan-backend/src/main/java/com/moyuan/controller/AdminController.java` (L75-L114)

**问题描述：** `getStatsTrend` 方法一次性查询最近7天的所有诗词、帖子、用户数据到内存中做分组统计。

**建议：** 使用数据库 GROUP BY 聚合查询

---

### 3.4 AI 评分等待机制

**文件：** `frontend/src/views/poem/detail.vue` (L138-L139)

**问题描述：** AI 评分请求后使用 `setTimeout(() => fetchRatings(), 2000)` 硬等待 2 秒再刷新，不保证 AI 评分已完成。

**建议：** 改为轮询机制或使用 WebSocket 通知

---

### 3.5 前后端评分范围不一致

**文件：** `frontend/src/views/poem/detail.vue` (L299-L304)

**问题描述：** `el-rate` 组件 `:max="10"` 但后端数据库定义评分为 1.0-5.0 的 DECIMAL(3,1)。

**建议：** 统一评分范围，建议前端 el-rate 的 max 设为 5

---

### 3.6 空值检查缺失

**文件：** `frontend/src/views/user/homepage.vue` (L96)

**问题描述：** `poem.content.substring(0, 100)` 未检查 content 是否为空，当 content 为 null/undefined 时会报错。

**建议：** 使用可选链 `poem.content?.substring(0, 100) || ''`

---

### 3.7 API Key 安全

**文件：** `sc-moyuan-backend/src/main/resources/db/init.sql` (L436-L440)

**问题描述：** AI 模型配置表中的 API Key 以明文硬编码在 SQL 初始化脚本中。

**建议：** 生产环境应使用环境变量或密钥管理服务注入 API Key

---

### 3.8 错误处理不友好

**文件：** 
- `frontend/src/views/poem/list.vue` (L89)
- `frontend/src/views/poem/detail.vue` (L57)

**问题描述：** 错误处理使用 `console.error` 而非 `ElMessage.error` 给用户反馈。

**建议：** 在 catch 块中增加 `ElMessage.error('获取诗词列表失败')` 给用户友好的错误提示

---

## 四、问题统计

| 严重程度 | 数量 | 占比 |
|----------|------|------|
| 严重 | 7 | 35% |
| 一般 | 7 | 35% |
| 建议 | 8 | 30% |
| **总计** | **22** | **100%** |

---

## 五、修复优先级

| 优先级 | 问题 | 影响范围 |
|--------|------|----------|
| P0 | 分层架构违规（1.1, 1.2） | 全局架构 |
| P0 | 安全漏洞（1.4） | 安全性 |
| P1 | Controller 异常处理缺失（1.3） | 稳定性 |
| P1 | 响应类 code 类型（1.5） | 接口规范 |
| P2 | 评分范围校验（2.1） | 数据完整性 |
| P2 | 并发竞态条件（2.6） | 数据准确性 |
| P3 | TypeScript 规范冲突（1.6） | 代码规范 |
| P3 | 性能优化（3.2, 3.3） | 性能 |

---

## 六、修复建议清单

| 序号 | 问题 | 严重程度 | 修复状态 |
|------|------|----------|----------|
| 1.1 | AdminController 直接注入 Mapper | 严重 | ✅ 已修复 |
| 1.2 | Service 层直接注入 Mapper | 严重 | ✅ 已修复 |
| 1.3 | Controller 异常处理缺失 | 严重 | ✅ 已修复 |
| 1.4 | 评分接口安全漏洞 | 严重 | ✅ 已修复 |
| 1.5 | 响应类 code 类型不符 | 严重 | ✅ 已修复 |
| 1.6 | TypeScript 规范冲突 | 严重 | ℹ️ 规范文档已更新（项目使用TS） |
| 1.7 | 类型定义不完整 | 严重 | ✅ 已修复 |
| 2.1 | 评分范围校验缺失 | 一般 | ✅ 已修复 |
| 2.2 | AI 评分解析逻辑脆弱 | 一般 | ✅ 已修复 |
| 2.3 | 硬编码 SQL 片段 | 一般 | ✅ 已修复 |
| 2.4 | AI 模型名称硬编码 | 一般 | ✅ 已修复 |
| 2.5 | 前后端字段命名不一致 | 一般 | ✅ 已修复 |
| 2.6 | 并发竞态条件 | 一般 | ✅ 已修复 |
| 2.7 | AI 评分 user_id 设计问题 | 一般 | ✅ 已修复 |
| 3.1 | 敏感信息暴露 | 建议 | ✅ 已修复 |
| 3.2 | 性能问题 - 内存计算 | 建议 | ⏭️ 后续优化 |
| 3.3 | 性能问题 - 趋势统计 | 建议 | ⏭️ 后续优化 |
| 3.4 | AI 评分等待机制 | 建议 | ✅ 已修复 |
| 3.5 | 前后端评分范围不一致 | 建议 | ✅ 已修复 |
| 3.6 | 空值检查缺失 | 建议 | ✅ 已修复 |
| 3.7 | API Key 安全 | 建议 | ℹ️ 使用占位符，生产环境由运维注入 |
| 3.8 | 错误处理不友好 | 建议 | ✅ 已修复 |

---

**报告生成时间：** 2026-06-06 14:30 (Asia/Shanghai)
**维护人员：** 墨渊开发团队
