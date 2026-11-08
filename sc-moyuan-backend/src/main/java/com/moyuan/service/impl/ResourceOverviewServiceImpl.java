package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyuan.entity.AiCallLog;
import com.moyuan.entity.AiModel;
import com.moyuan.entity.AiModuleConfig;
import com.moyuan.entity.AuditTicket;
import com.moyuan.entity.Poem;
import com.moyuan.entity.Poet;
import com.moyuan.entity.User;
import com.moyuan.mapper.AiCallLogMapper;
import com.moyuan.mapper.AiGeneratedContentMapper;
import com.moyuan.mapper.AuditTicketMapper;
import com.moyuan.mapper.CategoryMapper;
import com.moyuan.mapper.CommentMapper;
import com.moyuan.mapper.DynastyMapper;
import com.moyuan.mapper.ForumPostMapper;
import com.moyuan.mapper.GameRecordMapper;
import com.moyuan.mapper.GameSentenceMapper;
import com.moyuan.mapper.PoemMapper;
import com.moyuan.mapper.PoemRatingMapper;
import com.moyuan.mapper.PoetMapper;
import com.moyuan.mapper.RepairOrderMapper;
import com.moyuan.mapper.FileMetadataMapper;
import com.moyuan.mapper.UserFavoriteMapper;
import com.moyuan.mapper.UserHistoryMapper;
import com.moyuan.mapper.UserLikeMapper;
import com.moyuan.mapper.UserMapper;
import com.moyuan.mapper.UserNotificationMapper;
import com.moyuan.mapper.UserReportMapper;
import com.moyuan.service.AiModelConfigService;
import com.moyuan.service.ResourceOverviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * 管理端资源总览：内容 / AI / 存储 / 运行四域聚合。
 *
 * 设计约定：
 * 1. 只采集项目自身数据（表记录、模型配置、RAG 索引、文件与 JVM/Redis/MySQL），
 *    不采集宿主机 CPU、物理内存等操作系统指标；
 * 2. 结果整体缓存，默认 60 秒（前端每 5 分钟自动刷新，缓存只为削峰，不牺牲新鲜度）；
 * 3. 四个域分别做降级：任一域失败只返回该域 error，不影响其他域。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceOverviewServiceImpl implements ResourceOverviewService {

    private final UserMapper userMapper;
    private final PoemMapper poemMapper;
    private final PoetMapper poetMapper;
    private final ForumPostMapper forumPostMapper;
    private final CommentMapper commentMapper;
    private final CategoryMapper categoryMapper;
    private final DynastyMapper dynastyMapper;
    private final UserFavoriteMapper userFavoriteMapper;
    private final UserLikeMapper userLikeMapper;
    private final UserHistoryMapper userHistoryMapper;
    private final PoemRatingMapper poemRatingMapper;
    private final GameRecordMapper gameRecordMapper;
    private final GameSentenceMapper gameSentenceMapper;
    private final AiGeneratedContentMapper aiGeneratedContentMapper;
    private final UserReportMapper userReportMapper;
    private final UserNotificationMapper userNotificationMapper;
    private final RepairOrderMapper repairOrderMapper;
    private final AuditTicketMapper auditTicketMapper;
    private final AiCallLogMapper aiCallLogMapper;
    private final FileMetadataMapper fileMetadataMapper;

    private final AiModelConfigService aiModelConfigService;
    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;
    private final ObjectMapper objectMapper;

    @Value("${moyuan.health.rag-url:http://127.0.0.1:8010/rag/health}")
    private String ragHealthUrl;

    @Value("${moyuan.health.rag-timeout-ms:3000}")
    private long ragTimeoutMs;

    @Value("${moyuan.resource-overview.cache-ms:60000}")
    private long cacheMs;

    private volatile Map<String, Object> cached;
    private volatile long cachedAt;

    @Override
    public Map<String, Object> overview() {
        long now = System.currentTimeMillis();
        Map<String, Object> hit = cached;
        if (hit != null && now - cachedAt < cacheMs) {
            return hit;
        }
        synchronized (this) {
            if (cached != null && System.currentTimeMillis() - cachedAt < cacheMs) {
                return cached;
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("generatedAt", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            // RAG / MySQL / Redis 快照只取一次，供 AI、存储、运行三个域共享
            Map<String, Object> rag = loadRagHealth();
            long[] mysql = mysqlStats();
            long[] redis = redisStats();
            result.put("content", safe(this::buildContent, errorDomain("内容资源统计失败")));
            result.put("ai", safe(() -> buildAi(rag), errorDomain("AI 资源统计失败")));
            result.put("storage", safe(() -> buildStorage(rag, mysql, redis), errorDomain("存储资源统计失败")));
            result.put("runtime", safe(() -> buildRuntime(rag, mysql, redis), errorDomain("运行资源统计失败")));
            cached = result;
            cachedAt = System.currentTimeMillis();
            return result;
        }
    }

    // ==================== 内容资源 ====================

    private Map<String, Object> buildContent() {
        List<Map<String, Object>> items = new ArrayList<>(32);

        add(items, "poem", "诗词", count(poemMapper), "count", "subject");
        add(items, "poet", "诗人", count(poetMapper), "count", "subject");
        add(items, "user", "用户", count(userMapper), "count", "subject");
        add(items, "post", "帖子", count(forumPostMapper), "count", "subject");
        add(items, "comment", "评论", count(commentMapper), "count", "subject");
        add(items, "category", "分类", count(categoryMapper), "count", "subject");
        add(items, "dynasty", "朝代", count(dynastyMapper), "count", "subject");

        add(items, "poem_classical", "古籍诗词",
                count(poemMapper, new LambdaQueryWrapper<Poem>().eq(Poem::getPoemType, "classical")), "count", "structure");
        add(items, "poem_modern", "现代诗词",
                count(poemMapper, new LambdaQueryWrapper<Poem>().eq(Poem::getPoemType, "modern")), "count", "structure");
        add(items, "poem_foreign", "外文诗词",
                count(poemMapper, new LambdaQueryWrapper<Poem>().eq(Poem::getPoemType, "foreign")), "count", "structure");
        add(items, "poet_ancient", "古代诗人",
                count(poetMapper, new LambdaQueryWrapper<Poet>().eq(Poet::getPoetType, "ancient")), "count", "structure");
        add(items, "poet_modern", "现代诗人",
                count(poetMapper, new LambdaQueryWrapper<Poet>().eq(Poet::getPoetType, "modern")), "count", "structure");
        add(items, "poet_foreign", "外国诗人",
                count(poetMapper, new LambdaQueryWrapper<Poet>().eq(Poet::getPoetType, "foreign")), "count", "structure");

        add(items, "user_user", "普通用户",
                count(userMapper, new LambdaQueryWrapper<User>().eq(User::getRole, "user")), "count", "role");
        add(items, "user_poet", "认证诗人",
                count(userMapper, new LambdaQueryWrapper<User>().eq(User::getRole, "poet")), "count", "role");
        add(items, "user_reviewer", "审核员",
                count(userMapper, new LambdaQueryWrapper<User>().eq(User::getRole, "reviewer")), "count", "role");
        add(items, "user_admin", "管理员",
                count(userMapper, new LambdaQueryWrapper<User>().eq(User::getRole, "admin")), "count", "role");

        add(items, "favorite", "收藏", count(userFavoriteMapper), "count", "behavior");
        add(items, "like", "点赞", count(userLikeMapper), "count", "behavior");
        add(items, "history", "浏览", count(userHistoryMapper), "count", "behavior");
        add(items, "rating", "评分", count(poemRatingMapper), "count", "behavior");
        add(items, "game_record", "游戏对局", count(gameRecordMapper), "count", "behavior");
        add(items, "game_sentence", "对局诗句", count(gameSentenceMapper), "count", "behavior");

        long auditBacklog = count(auditTicketMapper, new LambdaQueryWrapper<AuditTicket>()
                .in(AuditTicket::getStatus, "pending", "assigned", "reviewing"));
        add(items, "audit_backlog", "待审工单", auditBacklog, "count", "governance");
        add(items, "ai_generated", "AI 生成内容", count(aiGeneratedContentMapper), "count", "governance");
        add(items, "report", "举报", count(userReportMapper), "count", "governance");
        add(items, "notification", "通知", count(userNotificationMapper), "count", "governance");
        add(items, "repair", "报修工单", count(repairOrderMapper), "count", "governance");

        Map<String, Object> domain = new LinkedHashMap<>();
        domain.put("items", items);
        return domain;
    }

    // ==================== AI 资源 ====================

    private Map<String, Object> buildAi(Map<String, Object> rag) {
        List<AiModel> models = aiModelConfigService.getAllModels();
        List<AiModuleConfig> modules = aiModelConfigService.getAllModuleConfigs();

        long modelTotal = models.size();
        long modelEnabled = models.stream().filter(m -> Integer.valueOf(1).equals(m.getIsEnabled())).count();
        String defaultModel = models.stream()
                .filter(m -> Integer.valueOf(1).equals(m.getIsDefault()))
                .findFirst()
                .map(m -> m.getDisplayName() != null && !m.getDisplayName().isBlank()
                        ? m.getDisplayName() : m.getName())
                .orElse("未设置");

        Map<String, Long> providers = new LinkedHashMap<>();
        for (AiModel model : models) {
            String provider = model.getProvider() == null || model.getProvider().isBlank() ? "未标注" : model.getProvider();
            providers.merge(provider, 1L, Long::sum);
        }
        List<Map<String, Object>> ring = new ArrayList<>();
        for (Map.Entry<String, Long> entry : providers.entrySet()) {
            ring.add(item(entry.getKey(), providerLabel(entry.getKey()), entry.getValue(), "count", "provider"));
        }

        long moduleTotal = modules.size();
        long moduleBound = modules.stream().filter(m -> m.getModelId() != null).count();

        LocalDateTime since = LocalDateTime.now().minusHours(24);
        LambdaQueryWrapper<AiCallLog> last24h = new LambdaQueryWrapper<AiCallLog>().ge(AiCallLog::getCreateTime, since);
        long callTotal = count(aiCallLogMapper, last24h);
        long callFailed = count(aiCallLogMapper, new LambdaQueryWrapper<AiCallLog>()
                .ge(AiCallLog::getCreateTime, since)
                .eq(AiCallLog::getStatus, "error"));
        double failRate = callTotal == 0 ? 0.0 : Math.round(callFailed * 1000.0 / callTotal) / 10.0;

        List<Map<String, Object>> items = new ArrayList<>(16);
        add(items, "model_total", "模型总数", modelTotal, "count", "model");
        add(items, "model_enabled", "已启用模型", modelEnabled, "count", "model");
        add(items, "module_total", "AI 模块", moduleTotal, "count", "module");
        add(items, "module_bound", "已绑定模块", moduleBound, "count", "module");
        add(items, "module_unbound", "未绑定模块", moduleTotal - moduleBound, "count", "module");
        add(items, "rag_chunks", "RAG 文本块", asLong(rag.get("chunks")), "count", "rag");
        add(items, "rag_corpus_files", "语料文件", asLong(rag.get("corpusFiles")), "count", "corpus");
        add(items, "rag_corpus_words", "语料字数", asLong(rag.get("corpusWords")), "count", "corpus");
        add(items, "rag_corpus_bytes", "语料体积", asLong(rag.get("corpusBytes")), "bytes", "corpus");
        add(items, "rag_index_bytes", "索引体积", asLong(rag.get("indexBytes")), "bytes", "rag");
        add(items, "ai_calls_24h", "24h 调用", callTotal, "count", "call");
        add(items, "ai_failed_24h", "24h 失败", callFailed, "count", "call");
        add(items, "ai_fail_rate", "失败率", failRate, "percent", "call");

        Map<String, Object> ai = new LinkedHashMap<>();
        ai.put("ring", ring);
        ai.put("items", items);
        ai.put("rag", rag);
        ai.put("defaultModel", defaultModel);
        ai.put("calls", Map.of("total24h", callTotal, "failed24h", callFailed, "failRate", failRate));
        return ai;
    }

    private Map<String, Object> loadRagHealth() {
        Map<String, Object> rag = new LinkedHashMap<>();
        rag.put("ready", false);
        rag.put("status", "down");
        rag.put("message", "RAG 服务未启动");
        rag.put("chunks", 0L);
        rag.put("corpusFiles", 0L);
        rag.put("corpusBytes", 0L);
        rag.put("indexBytes", 0L);
        rag.put("indexFiles", 0L);
        rag.put("indexDim", 0L);
        rag.put("corpusWords", 0L);
        rag.put("corpusDetails", new ArrayList<>());
        rag.put("indexDetails", new ArrayList<>());
        rag.put("builtAt", "");
        rag.put("embed", "");
        rag.put("embedLabel", "");
        rag.put("generation", "");
        rag.put("ollama", false);
        rag.put("latencyMs", null);

        int budget = (int) Math.max(300L, ragTimeoutMs);
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(budget);
        factory.setReadTimeout(budget);
        RestTemplate template = new RestTemplate(factory);

        long start = System.nanoTime();
        try {
            ResponseEntity<String> resp = template.getForEntity(ragHealthUrl, String.class);
            long ms = (System.nanoTime() - start) / 1_000_000;
            rag.put("latencyMs", ms);
            JsonNode body = objectMapper.readTree(resp.getBody() == null ? "{}" : resp.getBody());
            boolean ready = body.path("index_ready").asBoolean(false);
            rag.put("ready", ready);
            rag.put("status", resp.getStatusCode().is2xxSuccessful() && ready ? "up" : "warn");
            rag.put("message", ready ? "索引可用" : "索引未就绪");
            rag.put("chunks", body.path("chunks").asLong(0));
            rag.put("corpusFiles", body.path("corpus_files").asLong(0));
            rag.put("corpusBytes", body.path("corpus_bytes").asLong(0));
            rag.put("indexBytes", body.path("index_bytes").asLong(0));
            rag.put("indexFiles", body.path("index_files").asLong(0));
            rag.put("indexDim", body.path("index_dim").asLong(0));
            rag.put("corpusWords", body.path("corpus_words").asLong(0));
            JsonNode details = body.path("corpus_details");
            if (details.isArray()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                for (JsonNode node : details) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("name", node.path("name").asText(""));
                    row.put("bytes", node.path("bytes").asLong(0));
                    row.put("chunks", node.path("chunks").asLong(0));
                    rows.add(row);
                }
                rag.put("corpusDetails", rows);
            }
            JsonNode indexDetails = body.path("index_details");
            if (indexDetails.isArray()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                for (JsonNode node : indexDetails) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("name", node.path("name").asText(""));
                    row.put("bytes", node.path("bytes").asLong(0));
                    rows.add(row);
                }
                rag.put("indexDetails", rows);
            }
            rag.put("builtAt", body.path("built_at").asText(""));
            rag.put("embed", body.path("embed").asText(""));
            rag.put("embedLabel", body.path("embed_label").asText(body.path("embed").asText("")));
            rag.put("generation", body.path("generation").asText(""));
            rag.put("ollama", body.path("ollama").asBoolean(false));
        } catch (Exception e) {
            log.warn("资源总览读取 RAG 健康失败: {}", e.getMessage());
        }
        return rag;
    }

    // ==================== 存储资源 ====================

    private Map<String, Object> buildStorage(Map<String, Object> rag, long[] mysql, long[] redis) {
        List<Map<String, Object>> ring = new ArrayList<>();
        long fileCount = 0L;
        long fileSize = 0L;
        List<Map<String, Object>> typeRows = fileMetadataMapper.selectTypeStats();
        for (Map<String, Object> row : typeRows) {
            Object typeValue = valueOf(row, "fileType", "unknown");
            String type = typeValue == null ? "unknown" : String.valueOf(typeValue);
            long count = asLong(valueOf(row, "count", 0));
            long size = asLong(valueOf(row, "size", 0));
            fileCount += count;
            fileSize += size;
            ring.add(item(type, fileTypeLabel(type), count, "count", "fileType"));
        }

        List<Map<String, Object>> items = new ArrayList<>(12);
        add(items, "file_count", "文件总数", fileCount, "count", "file");
        add(items, "file_size", "文件总大小", fileSize, "bytes", "file");
        add(items, "mysql_size", "数据库大小", mysql[1], "bytes", "database");
        add(items, "mysql_tables", "数据库表数", mysql[0], "count", "database");
        add(items, "rag_index_bytes", "RAG 索引体积", asLong(rag.get("indexBytes")), "bytes", "rag");
        add(items, "rag_index_files", "向量索引文件", asLong(rag.get("indexFiles")), "count", "rag");
        add(items, "rag_corpus_files", "RAG 语料文件", asLong(rag.get("corpusFiles")), "count", "rag");
        add(items, "rag_corpus_bytes", "RAG 语料体积", asLong(rag.get("corpusBytes")), "bytes", "rag");
        add(items, "redis_memory", "Redis 内存", redis[1], "bytes", "cache");
        add(items, "redis_keys", "Redis Key", redis[0], "count", "cache");

        Map<String, Object> storage = new LinkedHashMap<>();
        storage.put("ring", ring);
        storage.put("items", items);
        storage.put("rag", rag);
        storage.put("mysqlLatencyMs", mysql[2]);
        storage.put("redisLatencyMs", redis[3]);
        return storage;
    }

    /**
     * 查询当前库的表数量与数据+索引体积。
     * 先用 SELECT 1 判定连通性，再单独查询 information_schema；
     * 后者失败（如权限不足）只让体积为 0，不把 MySQL 误判为不可用。
     */
    private long[] mysqlStats() {
        long[] result = new long[]{0L, 0L, 0L, 0L};
        long start = System.nanoTime();
        try (Connection conn = dataSource.getConnection()) {
            try (Statement st = conn.createStatement()) {
                st.execute("SELECT 1");
            }
            result[2] = Math.max(1L, (System.nanoTime() - start) / 1_000_000);
            result[3] = 1L;

            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT COUNT(*) AS table_count, COALESCE(SUM(data_length + index_length), 0) AS total_bytes "
                                 + "FROM information_schema.tables WHERE table_schema = DATABASE()")) {
                if (rs.next()) {
                    result[0] = rs.getLong("table_count");
                    result[1] = rs.getLong("total_bytes");
                }
            } catch (Exception e) {
                log.warn("资源总览查询数据库体积失败: {}", e.getMessage());
            }
        } catch (Exception e) {
            result[2] = 0L;
            result[3] = 0L;
            log.warn("资源总览查询数据库体积失败: {}", e.getMessage());
        }
        return result;
    }

    @SuppressWarnings("deprecation")
    private long[] redisStats() {
        long[] result = new long[]{0L, 0L, 0L, 0L, 0L};
        long start = System.nanoTime();
        try (RedisConnection conn = redisConnectionFactory.getConnection()) {
            conn.ping();
            result[3] = Math.max(1L, (System.nanoTime() - start) / 1_000_000);
            result[4] = 1L;

            try {
                Long keys = conn.serverCommands().dbSize();
                result[0] = keys == null ? 0L : keys;
            } catch (Exception ignore) {
                result[0] = 0L;
            }
            try {
                Properties memory = conn.serverCommands().info("memory");
                result[1] = memory == null ? 0L : toLong(memory.getProperty("used_memory"));
            } catch (Exception ignore) {
                result[1] = 0L;
            }
            try {
                Properties stats = conn.serverCommands().info("stats");
                if (stats != null) {
                    long hits = toLong(stats.getProperty("keyspace_hits"));
                    long misses = toLong(stats.getProperty("keyspace_misses"));
                    if (hits + misses > 0) {
                        result[2] = hits * 1000 / (hits + misses);
                    }
                }
            } catch (Exception ignore) {
                result[2] = 0L;
            }
        } catch (Exception e) {
            result[3] = 0L;
            result[4] = 0L;
            log.warn("资源总览读取 Redis 失败: {}", e.getMessage());
        }
        return result;
    }

    // ==================== 运行资源 ====================

    private Map<String, Object> buildRuntime(Map<String, Object> rag, long[] mysql, long[] redis) {
        Runtime rt = Runtime.getRuntime();
        long heapUsed = (rt.totalMemory() - rt.freeMemory());
        long heapMax = rt.maxMemory();
        long threads = ManagementFactory.getThreadMXBean().getThreadCount();
        long uptimeSeconds = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;

        List<Map<String, Object>> gauges = new ArrayList<>();
        gauges.add(gauge("heap", "JVM 堆内存", heapUsed, heapMax, "bytes",
                heapMax > 0 && heapUsed * 100 / heapMax >= 85 ? "warn" : "up"));

        List<Map<String, Object>> nodes = new ArrayList<>();
        nodes.add(node("api", "后端 API", "up", 0L, "运行中"));
        nodes.add(node("mysql", "MySQL", mysql[3] == 1 ? "up" : "down", mysql[2],
                mysql[3] == 1 ? "连接正常" : "不可用"));
        nodes.add(node("redis", "Redis", redis[4] == 1 ? "up" : "down", redis[3],
                redis[4] == 1 ? "PING 正常" : "不可用"));
        String ragStatus = String.valueOf(rag.get("status"));
        nodes.add(node("rag", "RAG 服务", ragStatus, nullableLong(rag.get("latencyMs")),
                String.valueOf(rag.get("message"))));

        List<Map<String, Object>> items = new ArrayList<>(10);
        add(items, "jvm_threads", "活跃线程", threads, "count", "jvm");
        add(items, "jvm_uptime", "运行时长", uptimeSeconds, "seconds", "jvm");
        add(items, "mysql_tables", "数据库表数", mysql[0], "count", "mysql");
        add(items, "mysql_size", "数据库大小", mysql[1], "bytes", "mysql");
        add(items, "redis_keys", "Redis Key", redis[0], "count", "redis");
        add(items, "redis_memory", "Redis 内存", redis[1], "bytes", "redis");
        add(items, "redis_hit_rate", "Redis 命中率", redis[2] / 10.0, "percent", "redis");

        Map<String, Object> runtime = new LinkedHashMap<>();
        runtime.put("gauges", gauges);
        runtime.put("nodes", nodes);
        runtime.put("items", items);
        return runtime;
    }

    // ==================== 工具方法 ====================

    private <T> long count(BaseMapper<T> mapper) {
        Long value = mapper.selectCount(null);
        return value == null ? 0L : value;
    }

    private <T> long count(BaseMapper<T> mapper, LambdaQueryWrapper<T> wrapper) {
        Long value = mapper.selectCount(wrapper);
        return value == null ? 0L : value;
    }

    private void add(List<Map<String, Object>> items, String key, String label, Object value, String unit, String group) {
        items.add(item(key, label, value, unit, group));
    }

    private Map<String, Object> item(String key, String label, Object value, String unit, String group) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("key", key);
        item.put("label", label);
        item.put("value", value);
        item.put("unit", unit);
        item.put("group", group);
        return item;
    }

    private Map<String, Object> gauge(String key, String label, long used, long total, String unit, String status) {
        Map<String, Object> gauge = new LinkedHashMap<>();
        gauge.put("key", key);
        gauge.put("label", label);
        gauge.put("used", used);
        gauge.put("total", total);
        gauge.put("unit", unit);
        gauge.put("status", status);
        return gauge;
    }

    private Map<String, Object> node(String key, String label, String status, Long latencyMs, String message) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("key", key);
        node.put("label", label);
        node.put("status", status);
        node.put("latencyMs", latencyMs);
        node.put("message", message);
        return node;
    }

    private Map<String, Object> errorDomain(String message) {
        Map<String, Object> domain = new LinkedHashMap<>();
        domain.put("error", message);
        domain.put("items", new ArrayList<>());
        domain.put("ring", new ArrayList<>());
        return domain;
    }

    private Map<String, Object> safe(Supplier<Map<String, Object>> supplier, Map<String, Object> fallback) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.error("资源总览域统计失败: {}", e.getMessage(), e);
            return fallback;
        }
    }

    private long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    /**
     * 从 MyBatis 聚合结果中按列名取值，兼容不同数据库返回的列名大小写。
     */
    private Object valueOf(Map<String, Object> row, String key, Object fallback) {
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return fallback;
    }

    private Long nullableLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private long toLong(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private String providerLabel(String provider) {
        return switch (provider) {
            case "deepseek" -> "DeepSeek";
            case "openai" -> "OpenAI";
            case "ollama" -> "Ollama";
            case "zhipu" -> "智谱";
            case "kimi" -> "Kimi";
            case "qwen" -> "通义千问";
            case "mimo" -> "MiMo";
            case "nvidia" -> "NVIDIA";
            case "openrouter" -> "OpenRouter";
            case "siliconflow" -> "硅基流动";
            case "doubao", "volcengine" -> "豆包";
            case "custom" -> "自定义";
            default -> provider;
        };
    }

    private String fileTypeLabel(String type) {
        return switch (type) {
            case "avatar" -> "头像";
            case "poem" -> "诗词配图";
            case "forum" -> "论坛附件";
            case "ai_generated" -> "AI 生成图";
            case "vision" -> "诗话视野";
            case "config" -> "站点配置";
            case "export" -> "导出文件";
            case "temp" -> "临时文件";
            case "backup" -> "备份";
            case "audit" -> "审核附件";
            case "watermark" -> "水印";
            case "cache" -> "缓存";
            default -> "其他";
        };
    }
}
