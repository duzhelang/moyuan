package com.moyuan.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 最近异常事件：把 AI 失败、安全事件、接口 5xx 三类记录合并成一条时间线，
 * 用于监测页底部的事件流。三张表都可能不存在（未执行迁移），任一缺失只跳过该来源。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecentEventService {

    private final JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> recent(int limit) {
        int size = limit <= 0 ? 12 : Math.min(limit, 50);
        try {
            return jdbcTemplate.queryForList(
                    "SELECT * FROM ("
                            + " SELECT create_time AS time, 'ai' AS source, "
                            + "   CONCAT('AI 调用失败 · ', endpoint) AS title, "
                            + "   LEFT(COALESCE(error_msg, ''), 80) AS detail "
                            + "   FROM ai_call_log WHERE status <> 'success' "
                            + " UNION ALL "
                            + " SELECT create_time, 'security', "
                            + "   CASE type WHEN 'login_fail' THEN '登录失败' "
                            + "             WHEN 'forbidden' THEN '越权访问' "
                            + "             WHEN 'rate_limit' THEN '触发限流' "
                            + "             ELSE type END, "
                            + "   CONCAT(COALESCE(detail, ''), ' ', COALESCE(ip, '')) "
                            + "   FROM security_event "
                            + " UNION ALL "
                            + " SELECT create_time, 'api', "
                            + "   CONCAT('接口 5xx · ', path), "
                            + "   CONCAT('HTTP ', status_code) "
                            + "   FROM api_request_metric WHERE status_code >= 500 "
                            + ") e ORDER BY time DESC LIMIT ?",
                    size);
        } catch (DataAccessException e) {
            log.warn("最近异常事件查询失败（表未迁移或数据库异常）: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
