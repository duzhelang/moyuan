package com.moyuan.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyuan.common.R;
import com.moyuan.dto.response.AiMetricsDTO;
import com.moyuan.dto.response.SearchMetricsDTO;
import com.moyuan.dto.response.SecurityMetricsDTO;
import com.moyuan.dto.response.SystemHealthDTO;
import com.moyuan.dto.response.SystemMetricsDTO;
import com.moyuan.service.AiMetricService;
import com.moyuan.service.ApiMetricService;
import com.moyuan.service.SearchMetricService;
import com.moyuan.service.SecurityMetricService;
import com.moyuan.service.RecentEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 系统健康检查：管理端「系统链路」与「运维资源」的数据源。
 *
 * 设计约定：
 * 1. 每个节点独立探测并各自超时，任一节点失败不影响其他节点返回；
 * 2. 结果缓存 cache-ms（默认 10s），避免面板 30s 轮询时反复打下游；
 * 3. 探测失败只标 down + 原因，不抛异常，页面不会因此空白。
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/system")
@RequiredArgsConstructor
@Tag(name = "系统健康检查")
public class SystemHealthController {

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;
    private final ObjectMapper objectMapper;
    private final ApiMetricService apiMetricService;
    private final SearchMetricService searchMetricService;
    private final AiMetricService aiMetricService;
    private final SecurityMetricService securityMetricService;
    private final RecentEventService recentEventService;

    @Value("${moyuan.health.rag-url:http://127.0.0.1:8010/rag/health}")
    private String ragHealthUrl;

    @Value("${moyuan.health.timeout-ms:800}")
    private long timeoutMs;

    /**
     * RAG 单独放宽超时：/rag/health 里还会探测 Ollama，
     * 与 MySQL/Redis 的毫秒级探活不是一个量级，共用 800ms 容易误判断连。
     */
    @Value("${moyuan.health.rag-timeout-ms:3000}")
    private long ragTimeoutMs;

    @Value("${moyuan.health.cache-ms:10000}")
    private long cacheMs;

    private final ExecutorService probePool = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "health-probe");
        t.setDaemon(true);
        return t;
    });

    private volatile SystemHealthDTO cached;
    private volatile long cachedAt;

    @Operation(summary = "系统链路健康检查（MySQL / Redis / RAG / JVM）")
    @GetMapping("/health")
    public R<SystemHealthDTO> health() {
        long now = System.currentTimeMillis();
        SystemHealthDTO hit = cached;
        if (hit != null && now - cachedAt < cacheMs) {
            return R.success(hit);
        }

        List<SystemHealthDTO.Node> nodes = new ArrayList<>(4);
        nodes.add(probe("api", "后端 API", this::probeApi));
        nodes.add(probe("mysql", "MySQL", this::probeMysql));
        nodes.add(probe("redis", "Redis", this::probeRedis));
        nodes.add(probe("rag", "RAG 服务", this::probeRag, ragTimeoutMs));

        SystemHealthDTO dto = new SystemHealthDTO();
        dto.setCheckedAt(java.time.LocalDateTime.now().toString());
        dto.setNodes(nodes);

        cached = dto;
        cachedAt = now;
        return R.success(dto);
    }

    @Operation(summary = "接口调用指标（错误率 / P95 / 慢接口 TOP）")
    @GetMapping("/metrics")
    public R<SystemMetricsDTO> metrics(@RequestParam(defaultValue = "24") int hours) {
        return R.success(apiMetricService.summary(hours));
    }

    @Operation(summary = "搜索指标（无结果率 / 热搜 TOP / 搜不到 TOP / 每日趋势）")
    @GetMapping("/search-metrics")
    public R<SearchMetricsDTO> searchMetrics(@RequestParam(defaultValue = "7") int days) {
        return R.success(searchMetricService.summary(days));
    }

    @Operation(summary = "AI 调用指标（失败率 / P95 / 首字延迟 / 分入口）")
    @GetMapping("/ai-metrics")
    public R<AiMetricsDTO> aiMetrics(@RequestParam(defaultValue = "24") int hours) {
        return R.success(aiMetricService.summary(hours));
    }

    @Operation(summary = "安全事件指标（登录失败 / 越权 403 / 限流 / 高危操作）")
    @GetMapping("/security-metrics")
    public R<SecurityMetricsDTO> securityMetrics(@RequestParam(defaultValue = "24") int hours) {
        return R.success(securityMetricService.summary(hours));
    }

    @Operation(summary = "最近异常事件（AI 失败 / 安全事件 / 接口 5xx 合并时间线）")
    @GetMapping("/recent-events")
    public R<List<Map<String, Object>>> recentEvents(@RequestParam(defaultValue = "12") int limit) {
        return R.success(recentEventService.recent(limit));
    }

    // ==================== 各节点探测 ====================

    private SystemHealthDTO.Node probeApi() {
        SystemHealthDTO.Node node = SystemHealthDTO.Node.of("api", "后端 API");
        node.setLatencyMs(0L);

        Runtime rt = Runtime.getRuntime();
        long usedMb = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        long maxMb = rt.maxMemory() / 1024 / 1024;
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();

        node.metric("堆内存", usedMb + " / " + maxMb + " MB");
        node.metric("活跃线程", String.valueOf(ManagementFactory.getThreadMXBean().getThreadCount()));
        node.metric("运行时长", formatUptime(uptime));
        node.metric("JVM", System.getProperty("java.version", "—"));

        if (maxMb > 0 && usedMb * 100 / maxMb >= 85) {
            node.setStatus("warn");
            node.setMessage("堆内存使用率超过 85%");
        }
        return node;
    }

    private SystemHealthDTO.Node probeMysql() {
        SystemHealthDTO.Node node = SystemHealthDTO.Node.of("mysql", "MySQL");
        long start = System.nanoTime();
        try (Connection conn = dataSource.getConnection()) {
            try (Statement st = conn.createStatement()) {
                st.execute("SELECT 1");
            }
            long ms = (System.nanoTime() - start) / 1_000_000;
            node.setLatencyMs(ms);
            node.metric("数据库", safeText(conn.getCatalog()));
            node.metric("探测耗时", ms + " ms");
            node.metric("连接有效", conn.isValid(1) ? "是" : "否");
            node.metric("数据源", dataSource.getClass().getSimpleName());
            if (ms >= timeoutMs * 0.8) {
                node.setStatus("warn");
                node.setMessage("查询响应偏慢");
            }
            return node;
        } catch (Exception e) {
            log.warn("MySQL 健康探测失败: {}", e.getMessage());
            return down("mysql", "MySQL", e.getMessage());
        }
    }

    @SuppressWarnings("deprecation")
    private SystemHealthDTO.Node probeRedis() {
        SystemHealthDTO.Node node = SystemHealthDTO.Node.of("redis", "Redis");
        long start = System.nanoTime();
        try (RedisConnection conn = redisConnectionFactory.getConnection()) {
            String pong = conn.ping();
            long ms = (System.nanoTime() - start) / 1_000_000;
            node.setLatencyMs(ms);
            node.metric("PING", pong == null ? "—" : pong);

            try {
                Long keys = conn.serverCommands().dbSize();
                node.metric("Key 数量", keys == null ? "—" : String.valueOf(keys));
            } catch (Exception ignore) {
                node.metric("Key 数量", "—");
            }

            try {
                Properties stats = conn.serverCommands().info("stats");
                if (stats != null) {
                    long hits = toLong(stats.getProperty("keyspace_hits"));
                    long misses = toLong(stats.getProperty("keyspace_misses"));
                    if (hits + misses > 0) {
                        node.metric("命中率", String.format("%.1f%%", hits * 100.0 / (hits + misses)));
                    } else {
                        node.metric("命中率", "—");
                    }
                }
            } catch (Exception ignore) {
                node.metric("命中率", "—");
            }

            try {
                Properties memory = conn.serverCommands().info("memory");
                String used = memory == null ? null : memory.getProperty("used_memory_human");
                node.metric("内存占用", used == null ? "—" : used.trim());
            } catch (Exception ignore) {
                node.metric("内存占用", "—");
            }

            if (ms >= timeoutMs * 0.8) {
                node.setStatus("warn");
                node.setMessage("PING 响应偏慢");
            }
            return node;
        } catch (Exception e) {
            log.warn("Redis 健康探测失败: {}", e.getMessage());
            return down("redis", "Redis", e.getMessage());
        }
    }

    private SystemHealthDTO.Node probeRag() {
        SystemHealthDTO.Node node = SystemHealthDTO.Node.of("rag", "RAG 服务");
        int budget = (int) Math.max(300L, ragTimeoutMs);
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(budget);
        factory.setReadTimeout(budget);
        RestTemplate template = new RestTemplate(factory);

        long start = System.nanoTime();
        try {
            ResponseEntity<String> resp = template.getForEntity(ragHealthUrl, String.class);
            long ms = (System.nanoTime() - start) / 1_000_000;
            node.setLatencyMs(ms);
            node.metric("HTTP", String.valueOf(resp.getStatusCode().value()));
            node.metric("响应耗时", ms + " ms");
            node.metric("地址", hostOf(ragHealthUrl));

            JsonNode body = null;
            try {
                body = objectMapper.readTree(resp.getBody() == null ? "{}" : resp.getBody());
            } catch (Exception ignore) {
                // 非 JSON 响应不影响健康判定
            }
            if (body != null) {
                if (body.hasNonNull("status")) {
                    node.metric("服务状态", body.get("status").asText());
                }
                if (body.hasNonNull("model")) {
                    node.metric("模型", body.get("model").asText());
                }
            }
            if (!resp.getStatusCode().is2xxSuccessful()) {
                node.setStatus("warn");
                node.setMessage("HTTP " + resp.getStatusCode().value());
            } else if (ms >= ragTimeoutMs * 0.8) {
                node.setStatus("warn");
                node.setMessage("响应偏慢");
            }
            return node;
        } catch (Exception e) {
            log.warn("RAG 健康探测失败: {}", e.getMessage());
            return down("rag", "RAG 服务", e.getMessage());
        }
    }

    // ==================== 工具方法 ====================

    private SystemHealthDTO.Node probe(String key, String name, Callable<SystemHealthDTO.Node> task) {
        return probe(key, name, task, timeoutMs);
    }

    private SystemHealthDTO.Node probe(
            String key,
            String name,
            Callable<SystemHealthDTO.Node> task,
            long budgetMs
    ) {
        Future<SystemHealthDTO.Node> future = probePool.submit(task);
        try {
            return future.get(budgetMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            return down(key, name, "探测超时（>" + budgetMs + "ms）");
        } catch (Exception e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            return down(key, name, cause.getMessage());
        }
    }

    private SystemHealthDTO.Node down(String key, String name, String message) {
        SystemHealthDTO.Node node = SystemHealthDTO.Node.of(key, name);
        node.setStatus("down");
        node.setMessage(message == null || message.isEmpty() ? "不可用" : message);
        return node;
    }

    private String safeText(String text) {
        return text == null || text.isEmpty() ? "—" : text;
    }

    private long toLong(String text) {
        try {
            return text == null ? 0L : Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private String hostOf(String url) {
        try {
            java.net.URI uri = java.net.URI.create(url);
            int port = uri.getPort();
            return uri.getHost() + (port > 0 ? ":" + port : "");
        } catch (Exception e) {
            return url;
        }
    }

    private String formatUptime(long ms) {
        long totalMinutes = ms / 60000;
        long days = totalMinutes / 1440;
        long hours = (totalMinutes % 1440) / 60;
        long minutes = totalMinutes % 60;
        if (days > 0) {
            return days + " 天 " + hours + " 小时";
        }
        if (hours > 0) {
            return hours + " 小时 " + minutes + " 分";
        }
        return minutes + " 分";
    }
}
