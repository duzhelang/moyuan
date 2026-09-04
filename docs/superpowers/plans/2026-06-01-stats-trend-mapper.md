# 趋势统计 Mapper 聚合查询改造计划

> **给自动化工作者：** 使用 subagent-driven-development 技能来逐任务实现此计划。

**目标：** 将 `getStatsTrend` 接口从 Java 内存聚合改为数据库层面聚合，提升查询性能 10-100 倍。

**架构：** 新增 `StatsMapper` 接口 + XML 映射文件，使用 UNION ALL 将 3 张表的 GROUP BY 查询合并为单次数据库调用，返回扁平化的聚合结果。

**技术栈：** MyBatis-Plus 3.5.x、MySQL 8.0

---

## 任务 1: 创建 StatsTrendDTO

**文件：**
- 创建: `sc-moyuan-backend/src/main/java/com/moyuan/dto/StatsTrendDTO.java`

- [ ] **步骤 1: 创建 DTO 类**

```java
package com.moyuan.dto;

import lombok.Data;

@Data
public class StatsTrendDTO {
    private String date;
    private String type;
    private Long count;
}
```

- [ ] **步骤 2: 验证文件创建成功**

检查文件存在于 `sc-moyuan-backend/src/main/java/com/moyuan/dto/StatsTrendDTO.java`

---

## 任务 2: 创建 StatsMapper 接口

**文件：**
- 创建: `sc-moyuan-backend/src/main/java/com/moyuan/mapper/StatsMapper.java`

- [ ] **步骤 1: 创建 Mapper 接口**

```java
package com.moyuan.mapper;

import com.moyuan.dto.StatsTrendDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StatsMapper {

    List<StatsTrendDTO> selectStatsTrend(@Param("days") int days);
}
```

- [ ] **步骤 2: 验证文件创建成功**

检查文件存在于 `sc-moyuan-backend/src/main/java/com/moyuan/mapper/StatsMapper.java`

---

## 任务 3: 创建 StatsMapper.xml 映射文件

**文件：**
- 创建: `sc-moyuan-backend/src/main/resources/mapper/StatsMapper.xml`

- [ ] **步骤 1: 创建 XML 映射文件**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.moyuan.mapper.StatsMapper">

    <select id="selectStatsTrend" resultType="com.moyuan.dto.StatsTrendDTO">
        SELECT
            DATE_FORMAT(create_time, '%m-%d') AS date,
            'poem' AS type,
            COUNT(*) AS count
        FROM poem
        WHERE create_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY)
        GROUP BY DATE_FORMAT(create_time, '%m-%d')

        UNION ALL

        SELECT
            DATE_FORMAT(create_time, '%m-%d') AS date,
            'post' AS type,
            COUNT(*) AS count
        FROM forum_post
        WHERE create_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY)
        GROUP BY DATE_FORMAT(create_time, '%m-%d')

        UNION ALL

        SELECT
            DATE_FORMAT(create_time, '%m-%d') AS date,
            'user' AS type,
            COUNT(*) AS count
        FROM user
        WHERE create_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY)
        GROUP BY DATE_FORMAT(create_time, '%m-%d')
    </select>

</mapper>
```

- [ ] **步骤 2: 验证文件创建成功**

检查文件存在于 `sc-moyuan-backend/src/main/resources/mapper/StatsMapper.xml`

---

## 任务 4: 修改 AdminController 使用新 Mapper

**文件：**
- 修改: `sc-moyuan-backend/src/main/java/com/moyuan/controller/AdminController.java:65-106`

- [ ] **步骤 1: 添加 StatsMapper 依赖注入**

在 AdminController 类的字段声明区域添加：

```java
private final StatsMapper statsMapper;
```

在构造函数中添加 `StatsMapper statsMapper` 参数。

- [ ] **步骤 2: 替换 getStatsTrend 方法实现**

```java
@Operation(summary = "获取统计趋势")
@GetMapping("/stats/trend")
public R<Map<String, Object>> getStatsTrend() {
    int days = 7;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

    List<String> dates = IntStream.rangeClosed(0, days - 1)
            .mapToObj(i -> LocalDate.now().minusDays(days - 1 - i).format(formatter))
            .toList();

    List<StatsTrendDTO> trendData = statsMapper.selectStatsTrend(days);

    Map<String, Map<String, Long>> grouped = trendData.stream()
            .collect(Collectors.groupingBy(
                    StatsTrendDTO::getType,
                    Collectors.toMap(
                            StatsTrendDTO::getDate,
                            StatsTrendDTO::getCount,
                            Long::sum)));

    List<Long> poemCounts = dates.stream()
            .map(d -> grouped.getOrDefault("poem", Map.of()).getOrDefault(d, 0L))
            .toList();
    List<Long> postCounts = dates.stream()
            .map(d -> grouped.getOrDefault("post", Map.of()).getOrDefault(d, 0L))
            .toList();
    List<Long> userCounts = dates.stream()
            .map(d -> grouped.getOrDefault("user", Map.of()).getOrDefault(d, 0L))
            .toList();

    Map<String, Object> result = new HashMap<>();
    result.put("dates", dates);
    result.put("poems", poemCounts);
    result.put("posts", postCounts);
    result.put("users", userCounts);
    return R.success(result);
}
```

- [ ] **步骤 3: 添加必要的 import 语句**

```java
import com.moyuan.dto.StatsTrendDTO;
import com.moyuan.mapper.StatsMapper;
```

- [ ] **步骤 4: 移除不再需要的 Service 依赖**

从构造函数中移除 `poemService`、`forumPostService`、`userService`（仅当它们在其他方法中未被使用时）。

---

## 任务 5: 验证编译通过

- [ ] **步骤 1: 运行 Maven 编译**

```bash
cd sc-moyuan-backend && mvn compile -q
```

预期：BUILD SUCCESS

- [ ] **步骤 2: 检查无编译错误**

确认没有 import 错误、类型不匹配等问题。

---

## 执行顺序

```
任务 1 (DTO) → 任务 2 (Mapper 接口) → 任务 3 (XML) → 任务 4 (Controller) → 任务 5 (验证)
```

任务 1-3 可并行执行，任务 4 依赖 1-3 完成。

---

## 回滚方案

如需回滚，恢复 `AdminController.java` 的 `getStatsTrend` 方法为原始实现即可。新增的 DTO、Mapper 接口和 XML 文件不影响其他功能。
