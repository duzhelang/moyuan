package com.moyuan.service;

import com.moyuan.dto.response.AiMetricsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 调用指标聚合：失败率 / 平均耗时 / P95 / 首字延迟 / 分入口明细。
 * 用 JdbcTemplate 直接聚合，避免建 Mapper XML。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiMetricService {

    private static final String TABLE = "ai_call_log";

    private final JdbcTemplate jdbcTemplate;

    public AiMetricsDTO summary(int hours) {
        int window = hours <= 0 ? 24 : Math.min(hours, 24 * 30);
        AiMetricsDTO dto = new AiMetricsDTO();
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
                    "SELECT COUNT(*) FROM " + TABLE + " WHERE create_time >= ? AND status <> 'success'",
                    Long.class, from);
            long errorCount = errors == null ? 0 : errors;
            dto.setErrorCount(errorCount);
            dto.setErrorRate((double) errorCount / count);

            Double avg = jdbcTemplate.queryForObject(
                    "SELECT AVG(duration_ms) FROM " + TABLE + " WHERE create_time >= ?", Double.class, from);
            dto.setAvgMs(avg == null ? 0 : Math.round(avg));

            // P95：升序取第 ceil(95%) 条
            int offset = (int) Math.ceil(count * 0.95) - 1;
            offset = Math.max(0, Math.min(offset, (int) count - 1));
            List<Integer> p95 = jdbcTemplate.queryForList(
                    "SELECT duration_ms FROM " + TABLE + " WHERE create_time >= ? "
                            + "ORDER BY duration_ms ASC LIMIT 1 OFFSET ?",
                    Integer.class, from, offset);
            if (!p95.isEmpty()) {
                dto.setP95Ms(p95.get(0).longValue());
            }

            // 首字延迟只统计流式接口的成功样本（失败样本的首字不代表可用体验）
            Long streamCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + TABLE + " WHERE create_time >= ? AND endpoint = 'chat_stream'",
                    Long.class, from);
            dto.setStreamCount(streamCount == null ? 0 : streamCount);
            Double ttft = jdbcTemplate.queryForObject(
                    "SELECT AVG(ttft_ms) FROM " + TABLE
                            + " WHERE create_time >= ? AND status = 'success' AND ttft_ms IS NOT NULL",
                    Double.class, from);
            dto.setTtftAvgMs(ttft == null ? null : Math.round(ttft));

            dto.setByEndpoint(jdbcTemplate.query(
                    "SELECT endpoint, COUNT(*) AS c, "
                            + "SUM(CASE WHEN status <> 'success' THEN 1 ELSE 0 END) AS e, "
                            + "AVG(duration_ms) AS avg_ms "
                            + "FROM " + TABLE + " WHERE create_time >= ? "
                            + "GROUP BY endpoint ORDER BY c DESC",
                    (rs, rowNum) -> {
                        AiMetricsDTO.EndpointRow row = new AiMetricsDTO.EndpointRow();
                        row.setEndpoint(rs.getString("endpoint"));
                        row.setCount(rs.getLong("c"));
                        row.setErrorCount(rs.getLong("e"));
                        row.setAvgMs(Math.round(rs.getDouble("avg_ms")));
                        return row;
                    }, from));

            dto.setByModel(jdbcTemplate.query(
                    "SELECT COALESCE(NULLIF(model, ''), 'unknown') AS m, COUNT(*) AS c, "
                            + "SUM(CASE WHEN status <> 'success' THEN 1 ELSE 0 END) AS e "
                            + "FROM " + TABLE + " WHERE create_time >= ? "
                            + "GROUP BY m ORDER BY c DESC LIMIT 8",
                    (rs, rowNum) -> {
                        AiMetricsDTO.ModelRow row = new AiMetricsDTO.ModelRow();
                        row.setModel(rs.getString("m"));
                        row.setCount(rs.getLong("c"));
                        row.setErrorCount(rs.getLong("e"));
                        return row;
                    }, from));

            String bucketExpr = window <= 6
                    ? "DATE_FORMAT(create_time, '%H:00')"
                    : "DATE_FORMAT(create_time, '%m-%d %H:00')";
            dto.setHourly(jdbcTemplate.query(
                    "SELECT " + bucketExpr + " AS b, COUNT(*) AS c, "
                            + "SUM(CASE WHEN status <> 'success' THEN 1 ELSE 0 END) AS e, "
                            + "AVG(duration_ms) AS avg_ms "
                            + "FROM " + TABLE + " WHERE create_time >= ? "
                            + "GROUP BY b ORDER BY MIN(create_time)",
                    (rs, rowNum) -> {
                        AiMetricsDTO.HourlyPoint point = new AiMetricsDTO.HourlyPoint();
                        point.setLabel(rs.getString("b"));
                        point.setCount(rs.getLong("c"));
                        point.setErrorCount(rs.getLong("e"));
                        point.setAvgMs(Math.round(rs.getDouble("avg_ms")));
                        return point;
                    }, from));

            return dto;
        } catch (DataAccessException e) {
            log.warn("AI 调用指标查询失败（表未迁移或数据库异常）: {}", e.getMessage());
            dto.setAvailable(false);
            dto.setMessage("AI 调用日志表不可用，请执行 db/migration-20261107-ai-security-log.sql");
            return dto;
        }
    }
}
