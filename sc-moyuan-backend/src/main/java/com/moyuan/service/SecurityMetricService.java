package com.moyuan.service;

import com.moyuan.dto.response.SecurityMetricsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 安全事件指标聚合：登录失败 / 越权 403 / 限流 / 高危操作。
 * 前三类来自 security_event，高危操作复用现有 operation_log。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityMetricService {

    private static final String TABLE = "security_event";

    private final JdbcTemplate jdbcTemplate;

    public SecurityMetricsDTO summary(int hours) {
        int window = hours <= 0 ? 24 : Math.min(hours, 24 * 30);
        SecurityMetricsDTO dto = new SecurityMetricsDTO();
        dto.setWindowHours(window);
        Timestamp from = Timestamp.valueOf(LocalDateTime.now().minusHours(window));

        try {
            List<SecurityMetricsDTO.TypeRow> byType = jdbcTemplate.query(
                    "SELECT type, COUNT(*) AS c FROM " + TABLE
                            + " WHERE create_time >= ? GROUP BY type ORDER BY c DESC",
                    (rs, rowNum) -> {
                        SecurityMetricsDTO.TypeRow row = new SecurityMetricsDTO.TypeRow();
                        row.setType(rs.getString("type"));
                        row.setCount(rs.getLong("c"));
                        return row;
                    }, from);

            long total = 0;
            for (SecurityMetricsDTO.TypeRow row : byType) {
                total += row.getCount();
                switch (row.getType()) {
                    case "login_fail" -> dto.setLoginFail(row.getCount());
                    case "forbidden" -> dto.setForbidden(row.getCount());
                    case "rate_limit" -> dto.setRateLimit(row.getCount());
                    default -> { }
                }
            }
            dto.setTotal(total);
            dto.setByType(byType);

            Long distinctIp = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT ip) FROM " + TABLE
                            + " WHERE create_time >= ? AND type = 'login_fail' AND ip IS NOT NULL",
                    Long.class, from);
            dto.setDistinctIp(distinctIp == null ? 0 : distinctIp);

            dto.setTopIps(jdbcTemplate.query(
                    "SELECT ip, COUNT(*) AS c FROM " + TABLE
                            + " WHERE create_time >= ? AND type = 'login_fail' AND ip IS NOT NULL "
                            + "GROUP BY ip ORDER BY c DESC LIMIT 5",
                    (rs, rowNum) -> {
                        SecurityMetricsDTO.IpRow row = new SecurityMetricsDTO.IpRow();
                        row.setIp(rs.getString("ip"));
                        row.setCount(rs.getLong("c"));
                        return row;
                    }, from));

            dto.setHighRiskOps(queryHighRiskOps(from));

            String bucketExpr = window <= 6
                    ? "DATE_FORMAT(create_time, '%H:00')"
                    : "DATE_FORMAT(create_time, '%m-%d %H:00')";
            dto.setHourly(jdbcTemplate.query(
                    "SELECT " + bucketExpr + " AS b, COUNT(*) AS c, "
                            + "SUM(CASE WHEN type = 'login_fail' THEN 1 ELSE 0 END) AS lf "
                            + "FROM " + TABLE + " WHERE create_time >= ? "
                            + "GROUP BY b ORDER BY MIN(create_time)",
                    (rs, rowNum) -> {
                        SecurityMetricsDTO.HourlyPoint point = new SecurityMetricsDTO.HourlyPoint();
                        point.setLabel(rs.getString("b"));
                        point.setCount(rs.getLong("c"));
                        point.setLoginFail(rs.getLong("lf"));
                        return point;
                    }, from));

            return dto;
        } catch (DataAccessException e) {
            log.warn("安全指标查询失败（表未迁移或数据库异常）: {}", e.getMessage());
            dto.setAvailable(false);
            dto.setMessage("安全事件表不可用，请执行 db/migration-20261107-ai-security-log.sql");
            return dto;
        }
    }

    /**
     * 高危操作数：operation_log 里失败（status=0）的写操作。
     * 表结构和写入口径都属现有功能，读不到时返回 null，前端显示「—」而不是 0。
     */
    private Long queryHighRiskOps(Timestamp from) {
        try {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM operation_log WHERE create_time >= ? AND status = 0",
                    Long.class, from);
            return count == null ? 0 : count;
        } catch (DataAccessException e) {
            log.debug("高危操作统计不可用: {}", e.getMessage());
            return null;
        }
    }
}
