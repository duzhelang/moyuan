# 后端技术规范文档

## 概述

本文档定义了"古今诗话——墨渊"项目后端开发的技术规范，包括项目结构、代码风格、API设计、安全规范等标准。

> **模块说明**：项目包含两个后端模块：
> - **backend/** — 基础版后端，提供诗词基础CRUD功能，无安全框架、无JWT、无Druid连接池
> - **sc-moyuan-backend/** — 完整版后端，包含安全认证、JWT、Druid、Knife4j等完整功能（部分Controller尚在开发中）

## 技术栈

### 核心框架

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 17+ | 编程语言 |
| Spring Boot | 3.2.5 | 应用框架 |
| Spring MVC | 6.x | Web框架 |
| Spring Security | 6.x | 安全框架 |

### 持久层

| 技术 | 版本 | 说明 |
|------|------|------|
| MyBatis-Plus | 3.5.5 | ORM框架 |
| MySQL | 8.0+ | 关系型数据库 |
| Redis | 7.x | 缓存数据库 |
| Druid | 1.2.20 | 数据库连接池 |

### 工具库

| 技术 | 版本 | 说明 |
|------|------|------|
| Lombok | 1.18.34 | 代码简化工具 |
| Knife4j | 4.3.0 | API文档生成 |
| JWT (jjwt) | 0.12.5 | JSON Web Token |

## 项目结构

### backend/（基础版后端）

已实现的类用 ✅ 标注，规划中未实现的用 📋 标注。

```
backend/
├── src/main/java/com/moyuan/
│   ├── config/
│   │   ├── CorsConfig.java                 ✅ 跨域配置
│   │   └── MybatisPlusConfig.java          ✅ MyBatis-Plus配置
│   ├── controller/
│   │   └── PoemController.java             ✅ 诗词控制器
│   ├── service/
│   │   ├── PoemService.java                ✅ 诗词服务接口
│   │   └── impl/PoemServiceImpl.java       ✅ 诗词服务实现
│   ├── mapper/
│   │   ├── PoemMapper.java                 ✅
│   │   ├── CategoryMapper.java             ✅
│   │   ├── DynastyMapper.java              ✅
│   │   └── PoetMapper.java                 ✅
│   ├── entity/
│   │   ├── Poem.java                       ✅
│   │   ├── Category.java                   ✅
│   │   ├── Dynasty.java                    ✅
│   │   └── Poet.java                       ✅
│   └── common/
│       └── Result.java                     ✅ 统一响应（com.moyuan.common.Result）
├── src/main/resources/
│   ├── application.yml
│   └── db/init.sql                         ✅ 建表脚本 + 初始数据（4张表、13朝代、11首诗）
└── pom.xml
```

### sc-moyuan-backend/（完整版后端）

```
sc-moyuan-backend/
├── src/main/java/com/moyuan/
│   ├── config/
│   │   ├── CorsConfig.java                 ✅ 跨域配置
│   │   ├── MyBatisPlusConfig.java          ✅ MyBatis-Plus配置
│   │   ├── RedisConfig.java                ✅ Redis配置（含CacheManager）
│   │   ├── SecurityConfig.java             ✅ Spring Security配置
│   │   ├── SwaggerConfig.java              ✅ Knife4j配置
│   │   └── WebMvcConfig.java               ✅ 静态资源映射（/uploads/**）
│   ├── controller/
│   │   ├── AuthController.java             ✅ 认证控制器
│   │   ├── ForumController.java            ✅ 论坛控制器
│   │   ├── UserController.java             ✅ 用户控制器
│   │   ├── PoemController.java             ✅ 诗词控制器（含随机/每日诗词）
│   │   ├── PoetController.java             ✅ 诗人控制器
│   │   ├── SearchController.java           ✅ 搜索控制器（跨模块搜索）
│   │   ├── FileController.java             ✅ 文件上传控制器
│   │   ├── HistoryController.java          ✅ 浏览历史控制器
│   │   └── AdminController.java            ✅ 管理员控制器
│   ├── service/
│   │   ├── UserService.java                ✅ 用户服务
│   │   ├── PoemService.java                ✅ 诗词服务（含@Cacheable缓存）
│   │   ├── PoetService.java                ✅ 诗人服务（含@Cacheable缓存）
│   │   ├── ForumPostService.java           ✅ 论坛帖子服务
│   │   ├── HistoryService.java             ✅ 浏览历史服务
│   │   └── impl/                           ✅ 服务实现类
│   ├── mapper/                             ✅ Mapper层
│   ├── entity/                             ✅ 实体类
│   ├── dto/                                ✅ 数据传输对象
│   ├── common/
│   │   ├── R.java                          ✅ 统一响应（com.moyuan.common.R）
│   │   ├── ResultCode.java                 ✅ 结果码枚举
│   │   └── PageRequest.java                ✅ 分页请求基类
│   ├── aspect/
│   │   └── OperationLogAspect.java         ✅ 操作日志切面
│   ├── scheduler/
│   │   └── ScheduledTasks.java             ✅ 定时任务（每日清除诗词缓存）
│   ├── exception/                          ✅ 异常处理
│   ├── security/                           ✅ 安全相关
│   ├── util/                               ✅ 工具类
│   └── enums/                              ✅ 枚举类
├── src/main/resources/
│   ├── application.yml
│   └── db/                                 ✅ 数据库脚本
└── pom.xml
```

## 代码规范

### 命名规范

#### 包命名
- 全部小写：`com.moyuan.controller`
- 按模块划分：`com.moyuan.module.service`

#### 类命名
- 大驼峰：`UserController`、`UserService`
- 接口以 `I` 开头或直接命名：`UserService`
- 实现类以 `Impl` 结尾：`UserServiceImpl`
- 抽象类以 `Abstract` 开头：`AbstractBaseService`

#### 方法命名
- 小驼峰：`getUserById`、`createPoem`
- 查询方法：`get`/`find`/`query` + 名词
- 新增方法：`create`/`add`/`save` + 名词
- 修改方法：`update`/`modify` + 名词
- 删除方法：`delete`/`remove` + 名词
- 判断方法：`is`/`has`/`can` + 形容词

#### 变量命名
- 小驼峰：`userId`、`poemTitle`
- 常量：大写下划线：`MAX_PAGE_SIZE`、`DEFAULT_TIMEOUT`
- 布尔值：以 `is`/`has`/`can` 开头

### 实体类规范

```java
package com.moyuan.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("poem")
public class Poem {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String title;
    
    private String content;
    
    private String author;
    
    private Long poetId;
    
    private Long dynastyId;
    
    private Long categoryId;
    
    private String translation;
    
    private String appreciation;
    
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
```

### DTO规范

```java
package com.moyuan.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PoemQueryRequest {
    
    @NotBlank(message = "关键词不能为空")
    private String keyword;
    
    private Long dynastyId;
    
    private Long categoryId;
    
    private Long poetId;
    
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = 1;
    
    @Min(value = 1, message = "每页数量最小为1")
    @Max(value = 100, message = "每页数量最大为100")
    private Integer pageSize = 10;
}
```

### 统一响应封装

> **说明**：两个后端模块使用不同的响应类：
> - `backend/` 使用 `com.moyuan.common.Result<T>`
> - `sc-moyuan-backend/` 使用 `com.moyuan.common.R<T>`

#### backend/ 中的 Result（已实现）

```java
package com.moyuan.common;

import lombok.Data;

@Data
public class Result<T> {
    
    private Integer code;
    
    private String message;
    
    private T data;
    
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }
    
    public static <T> Result<T> success() {
        return success(null);
    }
    
    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
    
    public static <T> Result<T> error(String message) {
        return error(500, message);
    }
}
```

#### sc-moyuan-backend/ 中的 R（已实现）

```java
package com.moyuan.common;

import lombok.Data;

@Data
public class R<T> {
    
    private Integer code;
    
    private String message;
    
    private T data;
    
    public static <T> R<T> success(T data) {
        R<T> result = new R<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }
    
    public static <T> R<T> success() {
        return success(null);
    }
    
    public static <T> R<T> error(Integer code, String message) {
        R<T> result = new R<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
    
    public static <T> R<T> error(String message) {
        return error(500, message);
    }
}
```

### Controller规范

```java
package com.moyuan.controller;

import com.moyuan.common.R;
import com.moyuan.dto.request.PoemQueryRequest;
import com.moyuan.entity.Poem;
import com.moyuan.service.PoemService;
import com.moyuan.vo.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "诗词管理", description = "诗词相关接口")
@RestController
@RequestMapping("/api/poem")
@RequiredArgsConstructor
public class PoemController {
    
    private final PoemService poemService;
    
    @Operation(summary = "获取诗词列表")
    @GetMapping("/list")
    public R<PageResult<Poem>> getPoemList(PoemQueryRequest request) {
        PageResult<Poem> result = poemService.getPoemList(request);
        return R.success(result);
    }
    
    @Operation(summary = "获取诗词详情")
    @GetMapping("/{id}")
    public R<Poem> getPoemById(@PathVariable Long id) {
        Poem poem = poemService.getPoemById(id);
        return R.success(poem);
    }
    
    @Operation(summary = "搜索诗词")
    @GetMapping("/search")
    public R<List<Poem>> searchPoems(@RequestParam String keyword) {
        List<Poem> poems = poemService.searchPoems(keyword);
        return R.success(poems);
    }
}
```

### Service规范

```java
package com.moyuan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.moyuan.dto.request.PoemQueryRequest;
import com.moyuan.entity.Poem;
import com.moyuan.vo.PageResult;

public interface PoemService extends IService<Poem> {
    
    PageResult<Poem> getPoemList(PoemQueryRequest request);
    
    Poem getPoemById(Long id);
    
    List<Poem> searchPoems(String keyword);
}
```

```java
package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moyuan.dto.request.PoemQueryRequest;
import com.moyuan.entity.Poem;
import com.moyuan.exception.BusinessException;
import com.moyuan.mapper.PoemMapper;
import com.moyuan.service.PoemService;
import com.moyuan.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PoemServiceImpl extends ServiceImpl<PoemMapper, Poem> implements PoemService {
    
    @Override
    public PageResult<Poem> getPoemList(PoemQueryRequest request) {
        LambdaQueryWrapper<Poem> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.like(Poem::getTitle, request.getKeyword())
                   .or()
                   .like(Poem::getContent, request.getKeyword());
        }
        
        if (request.getDynastyId() != null) {
            wrapper.eq(Poem::getDynastyId, request.getDynastyId());
        }
        
        if (request.getCategoryId() != null) {
            wrapper.eq(Poem::getCategoryId, request.getCategoryId());
        }
        
        Page<Poem> page = this.page(
            new Page<>(request.getPageNum(), request.getPageSize()),
            wrapper
        );
        
        return new PageResult<>(page.getRecords(), page.getTotal());
    }
    
    @Override
    public Poem getPoemById(Long id) {
        Poem poem = this.getById(id);
        if (poem == null) {
            throw new BusinessException("诗词不存在");
        }
        return poem;
    }
    
    @Override
    public List<Poem> searchPoems(String keyword) {
        LambdaQueryWrapper<Poem> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(Poem::getTitle, keyword)
               .or()
               .like(Poem::getContent, keyword)
               .or()
               .like(Poem::getAuthor, keyword);
        return this.list(wrapper);
    }
}
```

### Mapper规范

```java
package com.moyuan.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moyuan.entity.Poem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PoemMapper extends BaseMapper<Poem> {
    
}
```

## 异常处理规范

### 自定义异常

```java
package com.moyuan.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    
    private final Integer code;
    
    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }
    
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
    
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }
}
```

### 错误码枚举

```java
package com.moyuan.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    
    // 用户相关错误码
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户已存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    USER_DISABLED(1004, "用户已禁用"),
    
    // 诗词相关错误码
    POEM_NOT_FOUND(2001, "诗词不存在"),
    POET_NOT_FOUND(2002, "诗人不存在"),
    
    // 论坛相关错误码
    POST_NOT_FOUND(3001, "帖子不存在"),
    COMMENT_NOT_FOUND(3002, "评论不存在");
    
    private final Integer code;
    
    private final String message;
    
    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
```

### 全局异常处理器

```java
package com.moyuan.exception;

import com.moyuan.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public R<?> handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage());
        return R.error(e.getCode(), e.getMessage());
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return R.error(400, message);
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<?> handleException(Exception e) {
        log.error("系统异常: ", e);
        return R.error("系统繁忙，请稍后重试");
    }
}
```

## 安全规范

### JWT配置

```java
package com.moyuan.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private Long expiration;
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
    
    public String generateToken(Long userId, String username) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }
    
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }
    
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
```

### 安全配置

```java
package com.moyuan.config;

import com.moyuan.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/poem/list", "/api/poem/search").permitAll()
                .requestMatchers("/doc.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

## 日志规范

### 日志配置

```yaml
# application.yml
logging:
  level:
    root: INFO
    com.moyuan: DEBUG
    org.springframework: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/moyuan.log
    max-size: 100MB
    max-history: 30
```

### 日志使用

```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UserServiceImpl {
    
    public User getUserById(Long id) {
        log.debug("查询用户, id: {}", id);
        User user = userMapper.selectById(id);
        if (user == null) {
            log.warn("用户不存在, id: {}", id);
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        log.info("查询用户成功, id: {}, username: {}", id, user.getUsername());
        return user;
    }
}
```

## 配置规范

### 配置文件结构

```yaml
# application.yml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  profiles:
    active: dev
  application:
    name: moyuan-backend

# MyBatis-Plus配置
mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

# JWT配置
jwt:
  secret: your-secret-key-here-must-be-at-least-256-bits
  expiration: 86400000  # 24小时

# Knife4j配置
knife4j:
  enable: true
  setting:
    language: zh_cn
```

### 环境配置

```yaml
# application-dev.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/moyuan?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  redis:
    host: localhost
    port: 6379
    password:

# application-prod.yml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
  redis:
    host: ${REDIS_HOST}
    port: ${REDIS_PORT}
    password: ${REDIS_PASSWORD}
```

## 测试规范

### 单元测试

```java
package com.moyuan.service;

import com.moyuan.entity.Poem;
import com.moyuan.mapper.PoemMapper;
import com.moyuan.service.impl.PoemServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PoemServiceTest {
    
    @Mock
    private PoemMapper poemMapper;
    
    @InjectMocks
    private PoemServiceImpl poemService;
    
    @Test
    void getPoemById_WhenPoemExists_ShouldReturnPoem() {
        // Given
        Long poemId = 1L;
        Poem expectedPoem = new Poem();
        expectedPoem.setId(poemId);
        expectedPoem.setTitle("静夜思");
        
        when(poemMapper.selectById(poemId)).thenReturn(expectedPoem);
        
        // When
        Poem actualPoem = poemService.getPoemById(poemId);
        
        // Then
        assertNotNull(actualPoem);
        assertEquals(expectedPoem.getTitle(), actualPoem.getTitle());
        verify(poemMapper).selectById(poemId);
    }
    
    @Test
    void getPoemById_WhenPoemNotExists_ShouldThrowException() {
        // Given
        Long poemId = 999L;
        when(poemMapper.selectById(poemId)).thenReturn(null);
        
        // When & Then
        assertThrows(BusinessException.class, () -> {
            poemService.getPoemById(poemId);
        });
    }
}
```

---

**文档版本**：v1.2
**最后更新**：2026-05-29
**维护人员**：墨渊开发团队