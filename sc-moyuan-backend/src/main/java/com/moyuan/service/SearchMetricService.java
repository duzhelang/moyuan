package com.moyuan.service;

import com.moyuan.dto.response.SearchMetricsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 搜索指标聚合：无结果率 / 热搜 TOP / 搜不到 TOP / 每日趋势。
 * 用 JdbcTemplate 直接聚合，避免为一堆统计 SQL 建 Mapper XML。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchMetricService {

    private static final String TABLE = "search_log";

    private final JdbcTemplate jdbcTemplate;

    public SearchMetricsDTO summary(int days) {
        int window = days <= 0 ? 7 : Math.min(days, 90);
        SearchMetricsDTO dto = new SearchMetricsDTO();
        dto.setWindowDays(window);
        Timestamp from = Timestamp.valueOf(LocalDateTime.now().minusDays(window));

        try {
            Long total = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + TABLE + " WHERE create_time >= ?", Long.class, from);
            long count = total == null ? 0 : total;
            dto.setTotal(count);
            if (count == 0) {
                return dto;
            }

            Long noResult = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM " + TABLE + " WHERE create_time >= ? AND result_count = 0",
                    Long.class, from);
            long empty = noResult == null ? 0 : noResult;
            dto.setNoResult(empty);
            dto.setNoResultRate((double) empty / count);

            Long distinct = jdbcTemplate.queryForObject(
                    "SELECT COUNT(DISTINCT keyword) FROM " + TABLE + " WHERE create_time >= ?",
                    Long.class, from);
            dto.setDistinctKeywords(distinct == null ? 0 : distinct);

            dto.setTopKeywords(queryKeywords(
                    "SELECT keyword, COUNT(*) AS c, AVG(result_count) AS avg_result FROM " + TABLE
                            + " WHERE create_time >= ? GROUP BY keyword ORDER BY c DESC, keyword ASC LIMIT 10",
                    from));

            dto.setTopNoResult(queryKeywords(
                    "SELECT keyword, COUNT(*) AS c, AVG(result_count) AS avg_result FROM " + TABLE
                            + " WHERE create_time >= ? AND result_count = 0 "
                            + "GROUP BY keyword ORDER BY c DESC, keyword ASC LIMIT 10",
                    from));

            dto.setTrend(queryTrend(from));
            return dto;
        } catch (DataAccessException e) {
            log.warn("搜索指标查询失败（表未迁移或数据库异常）: {}", e.getMessage());
            dto.setAvailable(false);
            dto.setMessage("搜索埋点表不可用，请执行 db/migration-20261107-search-log.sql");
            return dto;
        }
    }

    private List<SearchMetricsDTO.KeywordRow> queryKeywords(String sql, Timestamp from) {
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            SearchMetricsDTO.KeywordRow row = new SearchMetricsDTO.KeywordRow();
            row.setKeyword(rs.getString("keyword"));
            row.setCount(rs.getLong("c"));
            row.setAvgResult(Math.round(rs.getDouble("avg_result")));
            return row;
        }, from);
    }

    /**
     * 每日趋势：先用一条 GROUP BY 拿到有数据的日子，再补齐空缺日期。
     * 补齐是为了前端折线不会因为某天没人搜而断点。
     */
    private List<SearchMetricsDTO.DailyRow> queryTrend(Timestamp from) {
        List<SearchMetricsDTO.DailyRow> rows = jdbcTemplate.query(
                "SELECT DATE(create_time) AS d, COUNT(*) AS c, "
                        + "SUM(CASE WHEN result_count = 0 THEN 1 ELSE 0 END) AS e "
                        + "FROM " + TABLE + " WHERE create_time >= ? GROUP BY DATE(create_time) ORDER BY d ASC",
                (rs, rowNum) -> {
                    SearchMetricsDTO.DailyRow row = new SearchMetricsDTO.DailyRow();
                    row.setDate(rs.getString("d"));
                    row.setTotal(rs.getLong("c"));
                    row.setNoResult(rs.getLong("e"));
                    return row;
                }, from);

        // 按日期补齐（只补窗口内、且不早于最早一条有数据的日子，避免长尾全 0）
        List<SearchMetricsDTO.DailyRow> filled = new ArrayList<>();
        if (rows.isEmpty()) {
            return filled;
        }
        LocalDate cursor = LocalDate.parse(rows.get(0).getDate());
        LocalDate end = LocalDate.now();
        int index = 0;
        while (!cursor.isAfter(end)) {
            String key = cursor.toString();
            if (index < rows.size() && key.equals(rows.get(index).getDate())) {
                filled.add(rows.get(index));
                index++;
            } else {
                SearchMetricsDTO.DailyRow row = new SearchMetricsDTO.DailyRow();
                row.setDate(key);
                row.setTotal(0);
                row.setNoResult(0);
                filled.add(row);
            }
            cursor = cursor.plusDays(1);
        }
        return filled;
    }
}
