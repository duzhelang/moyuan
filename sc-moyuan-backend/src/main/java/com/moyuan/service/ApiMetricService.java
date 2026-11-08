package com.moyuan.service;

import com.moyuan.dto.response.SystemMetricsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 接口指标聚合：错误率 / 平均耗时 / P95 / 慢接口 TOP。
 * 用 JdbcTemplate 直接聚合，避免为一堆统计 SQL 建 Mapper XML。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiMetricService {

    private static final String TABLE = "api_request_metric";

    private final JdbcTemplate jdbcTemplate;

    public SystemMetricsDTO summary(int hours) {
        int window = hours <= 0 ? 24 : Math.min(hours, 24 * 30);
        SystemMetricsDTO dto = new SystemMetricsDTO();
        dto.setWindowHours(window);
        Timestamp from = Timestamp.valueOf(LocalDateTime.now().minusHours(window));

        try {
            Long total = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + TABLE + " WHERE create_time >= ?", Long.class, from);
            long count = total == null ? 0 : total;
            dto.setTotal(count);
            if (count == 0) {
                return dto;
            }

            Long errors = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + TABLE + " WHERE create_time >= ? AND status_code >= 500",
                    Long.class, from);
            long errorCount = errors == null ? 0 : errors;
            dto.setErrorCount(errorCount);
            dto.setErrorRate((double) errorCount / count);

            Double avg = jdbcTemplate.queryForObject(
                    "SELECT AVG(duration_ms) FROM " + TABLE + " WHERE create_time >= ?", Double.class, from);
            dto.setAvgMs(avg == null ? 0 : Math.round(avg));

            // P95：按耗时升序取第 ceil(95%) 条，避免小样本时偏移取低
            int offset = (int) Math.ceil(count * 0.95) - 1;
            if (offset < 0) {
                offset = 0;
            }
            if (offset >= count) {
                offset = (int) (count - 1);
            }
            List<Integer> p95 = jdbcTemplate.queryForList(
                    "SELECT duration_ms FROM " + TABLE + " WHERE create_time >= ? ORDER BY duration_ms ASC LIMIT 1 OFFSET ?",
                    Integer.class, from, offset);
            if (!p95.isEmpty()) {
                dto.setP95Ms(p95.get(0).longValue());
            }

            List<SystemMetricsDTO.SlowApi> slow = jdbcTemplate.query(
                    "SELECT path, COUNT(*) AS c, AVG(duration_ms) AS avg_ms, MAX(duration_ms) AS max_ms "
                            + "FROM " + TABLE + " WHERE create_time >= ? "
                            + "GROUP BY path HAVING COUNT(*) >= 2 ORDER BY avg_ms DESC LIMIT 5",
                    (rs, rowNum) -> {
                        SystemMetricsDTO.SlowApi api = new SystemMetricsDTO.SlowApi();
                        api.setPath(rs.getString("path"));
                        api.setCount(rs.getLong("c"));
                        api.setAvgMs(Math.round(rs.getDouble("avg_ms")));
                        api.setMaxMs(rs.getLong("max_ms"));
                        return api;
                    }, from);
            dto.setSlowTop(slow);

            // 时间桶序列：6 小时内按小时展示，超过 6 小时带日期前缀，避免跨天重复
            String bucketExpr = window <= 6
                    ? "DATE_FORMAT(create_time, '%H:00')"
                    : "DATE_FORMAT(create_time, '%m-%d %H:00')";
            dto.setHourly(jdbcTemplate.query(
                    "SELECT " + bucketExpr + " AS b, COUNT(*) AS c, "
                            + "SUM(CASE WHEN status_code >= 500 THEN 1 ELSE 0 END) AS e, "
                            + "AVG(duration_ms) AS avg_ms "
                            + "FROM " + TABLE + " WHERE create_time >= ? "
                            + "GROUP BY b ORDER BY MIN(create_time)",
                    (rs, rowNum) -> {
                        SystemMetricsDTO.HourlyPoint point = new SystemMetricsDTO.HourlyPoint();
                        point.setLabel(rs.getString("b"));
                        point.setCount(rs.getLong("c"));
                        point.setErrorCount(rs.getLong("e"));
                        point.setAvgMs(Math.round(rs.getDouble("avg_ms")));
                        return point;
                    }, from));

            // 状态码区间分布：2xx 成功 / 3xx 跳转 / 4xx 客户端错误 / 5xx 服务端错误
            long[] buckets = new long[4];
            jdbcTemplate.query(
                    "SELECT status_code, COUNT(*) AS c FROM " + TABLE
                            + " WHERE create_time >= ? GROUP BY status_code",
                    rs -> {
                        int code = rs.getInt("status_code");
                        long c = rs.getLong("c");
                        if (code >= 500) {
                            buckets[3] += c;
                        } else if (code >= 400) {
                            buckets[2] += c;
                        } else if (code >= 300) {
                            buckets[1] += c;
                        } else {
                            buckets[0] += c;
                        }
                    }, from);
            String[] bucketNames = {"2xx 成功", "3xx 跳转", "4xx 客户端错误", "5xx 服务端错误"};
            List<SystemMetricsDTO.StatusBucket> statusBuckets = new java.util.ArrayList<>();
            for (int i = 0; i < bucketNames.length; i++) {
                SystemMetricsDTO.StatusBucket bucket = new SystemMetricsDTO.StatusBucket();
                bucket.setName(bucketNames[i]);
                bucket.setCount(buckets[i]);
                statusBuckets.add(bucket);
            }
            dto.setStatusBuckets(statusBuckets);

            return dto;
        } catch (DataAccessException e) {
            log.warn("接口指标查询失败（表未迁移或数据库异常）: {}", e.getMessage());
            dto.setAvailable(false);
            dto.setMessage("指标表不可用，请执行 db/migration-20261107-api-request-metric.sql");
            return dto;
        }
    }
}
