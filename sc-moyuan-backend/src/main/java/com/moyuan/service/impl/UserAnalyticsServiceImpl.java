package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyuan.entity.User;
import com.moyuan.mapper.UserMapper;
import com.moyuan.service.UserAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户分析扩展指标实现。
 *
 * 只使用 user 表已有字段（create_time / last_login_time / role），不新增埋点：
 * - 活跃：最近 N 天有登录记录；
 * - 回访留存：注册满 N 天的用户中，最近 N 天有登录的比例；
 * - 角色结构：user / poet / reviewer / admin。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAnalyticsServiceImpl implements UserAnalyticsService {

    private final UserMapper userMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Map<String, Object> structure() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since7 = now.minusDays(7);
        LocalDateTime since30 = now.minusDays(30);

        long total = count(null);
        long newUsers7d = count(new LambdaQueryWrapper<User>().ge(User::getCreateTime, since7));
        long active7d = count(new LambdaQueryWrapper<User>().ge(User::getLastLoginTime, since7));
        long active30d = count(new LambdaQueryWrapper<User>().ge(User::getLastLoginTime, since30));

        long eligible7d = count(new LambdaQueryWrapper<User>().le(User::getCreateTime, since7));
        long eligible30d = count(new LambdaQueryWrapper<User>().le(User::getCreateTime, since30));
        long retained7d = count(new LambdaQueryWrapper<User>()
                .le(User::getCreateTime, since7)
                .ge(User::getLastLoginTime, since7));
        long retained30d = count(new LambdaQueryWrapper<User>()
                .le(User::getCreateTime, since30)
                .ge(User::getLastLoginTime, since30));

        List<Map<String, Object>> roles = new ArrayList<>();
        roles.add(roleItem("user", "普通用户", "user"));
        roles.add(roleItem("poet", "认证诗人", "poet"));
        roles.add(roleItem("reviewer", "审核员", "reviewer"));
        roles.add(roleItem("admin", "管理员", "admin"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalUsers", total);
        result.put("newUsers7d", newUsers7d);
        result.put("roles", roles);
        result.put("active7d", active7d);
        result.put("active30d", active30d);
        result.put("eligible7d", eligible7d);
        result.put("eligible30d", eligible30d);
        result.put("retention7d", rate(retained7d, eligible7d));
        result.put("retention30d", rate(retained30d, eligible30d));
        result.put("silent30d", Math.max(0, total - active30d));
        return result;
    }

    @Override
    public Map<String, Object> activityHeatmap(int days) {
        int window = days <= 0 ? 7 : Math.min(days, 90);
        Timestamp from = Timestamp.valueOf(LocalDateTime.now().minusDays(window));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("days", window);

        long[][] requests = new long[7][24];
        long[][] users = new long[7][24];
        long maxRequests = 0;

        try {
            // 统一成 1=周一 ... 7=周日，避免 MySQL DAYOFWEEK 的周日=1 与前端错位
            jdbcTemplate.query(
                    "SELECT ((DAYOFWEEK(create_time) + 5) % 7) + 1 AS wd, "
                            + "HOUR(create_time) AS hr, COUNT(*) AS c, "
                            + "COUNT(DISTINCT user_id) AS u "
                            + "FROM visit_log WHERE create_time >= ? GROUP BY wd, hr",
                    rs -> {
                        int wd = rs.getInt("wd");
                        int hr = rs.getInt("hr");
                        if (wd >= 1 && wd <= 7 && hr >= 0 && hr <= 23) {
                            requests[wd - 1][hr] = rs.getLong("c");
                            users[wd - 1][hr] = rs.getLong("u");
                        }
                    }, from);

            List<Map<String, Object>> cells = new ArrayList<>(7 * 24);
            for (int wd = 0; wd < 7; wd++) {
                for (int hr = 0; hr < 24; hr++) {
                    Map<String, Object> cell = new LinkedHashMap<>();
                    cell.put("weekday", wd);
                    cell.put("hour", hr);
                    cell.put("requests", requests[wd][hr]);
                    cell.put("users", users[wd][hr]);
                    cells.add(cell);
                    maxRequests = Math.max(maxRequests, requests[wd][hr]);
                }
            }

            result.put("available", true);
            result.put("maxRequests", maxRequests);
            result.put("cells", cells);
            return result;
        } catch (Exception e) {
            log.warn("用户活跃热力图查询失败: {}", e.getMessage());
            result.put("available", false);
            result.put("message", "访问日志表不可用");
            result.put("cells", List.of());
            return result;
        }
    }

    private Map<String, Object> roleItem(String key, String label, String role) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("key", key);
        item.put("label", label);
        item.put("value", count(new LambdaQueryWrapper<User>().eq(User::getRole, role)));
        return item;
    }

    private long count(LambdaQueryWrapper<User> wrapper) {
        Long value = userMapper.selectCount(wrapper);
        return value == null ? 0L : value;
    }

    private double rate(long numerator, long denominator) {
        return denominator == 0 ? 0.0 : Math.round(numerator * 1000.0 / denominator) / 10.0;
    }
}
