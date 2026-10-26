package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moyuan.dto.response.RecommendItemDTO;
import com.moyuan.dto.response.RecommendModelDTO;
import com.moyuan.dto.response.RecommendModelsResponseDTO;
import com.moyuan.dto.response.RecommendResponseDTO;
import com.moyuan.dto.response.RecommendStatsDTO;
import com.moyuan.entity.*;
import com.moyuan.mapper.*;
import com.moyuan.enums.TargetType;
import com.moyuan.service.MlRecommendationService;
import com.moyuan.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模型策略推荐实现：只做查表 + 过滤 + 回填，不跑模型、不用 Spring 缓存
 * （避免已交互过滤结果被缓存住）。线上池口径与首页一致：古典、上架、未交互。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MlRecommendationServiceImpl implements MlRecommendationService {

    private static final Set<String> MODEL_STRATEGIES = Set.of("era", "genre", "fusion", "global");
    private static final int POEM_TARGET_TYPE = TargetType.POEM.getCode();
    private static final int CACHE_TOP_N = 20;
    private static final int HOT_FILL_LIMIT = 50;

    private static final Map<String, String> STRATEGY_LABELS = Map.of(
            "era", "古今",
            "genre", "体裁",
            "fusion", "融合",
            "global", "全局"
    );

    private final RecommendationModelVersionMapper modelVersionMapper;
    private final RecommendationCacheMapper recommendationCacheMapper;
    private final RecommendationEventMapper recommendationEventMapper;
    private final RecommendationService recommendationService;
    private final PoemMapper poemMapper;
    private final PoetMapper poetMapper;
    private final DynastyMapper dynastyMapper;
    private final UserLikeMapper userLikeMapper;
    private final UserFavoriteMapper userFavoriteMapper;
    private final UserHistoryMapper userHistoryMapper;
    private final ObjectMapper objectMapper;

    @Override
    public RecommendResponseDTO recommend(Long userId, String strategy, int topK) {
        int limit = Math.max(1, Math.min(20, topK));
        if (strategy == null || !MODEL_STRATEGIES.contains(strategy)) {
            return buildFallback(strategy, null, "cache-miss", userId, limit);
        }
        try {
            return doRecommend(userId, strategy, limit);
        } catch (Exception e) {
            // 缺表/数据异常等：记录告警并直接回落规则，不让新环境初始化缺失阻塞首页
            log.warn("模型策略推荐异常，回落规则: userId={}, strategy={}, error={}", userId, strategy, e.getMessage());
            return buildFallback(strategy, null, "model-offline", userId, limit);
        }
    }

    private RecommendResponseDTO doRecommend(Long userId, String strategy, int limit) {

        RecommendationModelVersion active = selectActiveVersion(strategy);
        if (active == null) {
            return buildFallback(strategy, null, "model-offline", userId, limit);
        }

        List<RecommendationCache> cacheRows = recommendationCacheMapper.selectList(
                new LambdaQueryWrapper<RecommendationCache>()
                        .eq(RecommendationCache::getUserId, userId)
                        .eq(RecommendationCache::getStrategy, strategy)
                        .eq(RecommendationCache::getModelVersion, active.getVersion())
                        .orderByAsc(RecommendationCache::getRankNo)
                        .last("LIMIT " + CACHE_TOP_N));
        if (cacheRows.isEmpty()) {
            return buildFallback(strategy, active, "cache-miss", userId, limit);
        }

        // 只保留古典、上架作品，并过滤当前时点已交互（点赞/收藏/浏览）的作品，
        // 按 rank_no 顺序取满 topK——缓存行本身就是"剩余 Top20 回填"的来源
        Set<Long> interactedIds = loadInteractedPoemIds(userId);
        LinkedHashMap<Long, RecommendationCache> usable = new LinkedHashMap<>();
        for (RecommendationCache row : cacheRows) {
            if (usable.size() >= limit) break;
            if (interactedIds.contains(row.getPoemId())) continue;
            usable.put(row.getPoemId(), row);
        }

        List<RecommendItemDTO> items = buildItems(usable.values());
        if (items.size() < limit) {
            items.addAll(buildHotFillItems(userId, interactedIds, usable.keySet(), limit - items.size()));
        }

        if (items.isEmpty()) {
            return buildFallback(strategy, active, "empty", userId, limit);
        }

        RecommendResponseDTO response = new RecommendResponseDTO();
        response.setRequestedStrategy(strategy);
        response.setStrategy(strategy);
        response.setFallback(false);
        response.setModelVersion(active.getVersion());
        response.setMetrics(parseMetrics(active.getMetricsJson()));
        response.setAvailableStrategies(availableStrategiesFor(userId));
        response.setItems(items);
        return response;
    }

    @Override
    public RecommendModelsResponseDTO getModels(Long userId) {
        try {
            return doGetModels(userId);
        } catch (Exception e) {
            // 缺表等异常：降级为空模型 + 只显示规则，不让新环境初始化缺失打挂首页
            log.warn("查询推荐模型信息异常，降级为仅规则: userId={}, error={}", userId, e.getMessage());
            RecommendModelsResponseDTO response = new RecommendModelsResponseDTO();
            response.setModels(Collections.emptyList());
            response.setAvailableStrategies(List.of("rule"));
            return response;
        }
    }

    private RecommendModelsResponseDTO doGetModels(Long userId) {
        List<RecommendationModelVersion> actives = modelVersionMapper.selectList(
                new LambdaQueryWrapper<RecommendationModelVersion>()
                        .eq(RecommendationModelVersion::getStatus, 1)
                        .orderByAsc(RecommendationModelVersion::getStrategy));

        List<RecommendModelDTO> models = actives.stream().map(v -> {
            RecommendModelDTO dto = new RecommendModelDTO();
            dto.setStrategy(v.getStrategy());
            dto.setVersion(v.getVersion());
            dto.setDataFingerprint(v.getDataFingerprint());
            dto.setMetricsPool(v.getMetricsPool());
            dto.setMetrics(parseMetrics(v.getMetricsJson()));
            dto.setStatus(v.getStatus());
            dto.setCreateTime(v.getCreateTime());
            return dto;
        }).collect(Collectors.toList());

        RecommendModelsResponseDTO response = new RecommendModelsResponseDTO();
        response.setModels(models);
        response.setAvailableStrategies(availableStrategiesFor(userId));
        return response;
    }

    @Override
    public void recordEvent(Long userId, String page, String fromStrategy, String toStrategy) {
        RecommendationEvent event = new RecommendationEvent();
        event.setUserId(userId);
        event.setPage(page == null || page.isBlank() ? "home" : page);
        event.setFromStrategy(fromStrategy);
        event.setToStrategy(toStrategy);
        recommendationEventMapper.insert(event);
    }

    @Override
    public RecommendStatsDTO getLiveStats() {
        RecommendStatsDTO stats = new RecommendStatsDTO();
        stats.setCacheRows(recommendationCacheMapper.selectCount(null));
        stats.setCacheUsers((long) recommendationCacheMapper.selectObjs(
                new QueryWrapper<RecommendationCache>().select("DISTINCT user_id")).size());
        stats.setCachePoems((long) recommendationCacheMapper.selectObjs(
                new QueryWrapper<RecommendationCache>().select("DISTINCT poem_id")).size());
        stats.setCacheStrategies((long) recommendationCacheMapper.selectObjs(
                new QueryWrapper<RecommendationCache>().select("DISTINCT strategy")).size());
        stats.setPoolSize(poemMapper.selectCount(
                new LambdaQueryWrapper<Poem>()
                        .eq(Poem::getStatus, 1)
                        .eq(Poem::getPoemType, "classical")));
        stats.setEventTotal(recommendationEventMapper.selectCount(null));
        stats.setEventToday(recommendationEventMapper.selectCount(
                new LambdaQueryWrapper<RecommendationEvent>()
                        .ge(RecommendationEvent::getCreateTime, LocalDate.now().atStartOfDay())));

        RecommendationEvent last = recommendationEventMapper.selectOne(
                new LambdaQueryWrapper<RecommendationEvent>()
                        .select(RecommendationEvent::getCreateTime)
                        .orderByDesc(RecommendationEvent::getId)
                        .last("LIMIT 1"));
        stats.setLastEventTime(last == null ? null : last.getCreateTime());

        // 上线模型 + 缓存状态
        List<RecommendationModelVersion> actives = modelVersionMapper.selectList(
                new LambdaQueryWrapper<RecommendationModelVersion>()
                        .eq(RecommendationModelVersion::getStatus, 1)
                        .orderByAsc(RecommendationModelVersion::getStrategy));
        stats.setModels(actives.stream().map(v -> {
            RecommendStatsDTO.ModelStat model = new RecommendStatsDTO.ModelStat();
            model.setStrategy(v.getStrategy());
            model.setVersion(v.getVersion());
            model.setStatus(v.getStatus());
            model.setMetricsPool(v.getMetricsPool());
            model.setMetrics(parseMetrics(v.getMetricsJson()));
            model.setCreateTime(v.getCreateTime());
            model.setCacheRows(recommendationCacheMapper.selectCount(
                    new LambdaQueryWrapper<RecommendationCache>()
                            .eq(RecommendationCache::getStrategy, v.getStrategy())
                            .eq(RecommendationCache::getModelVersion, v.getVersion())));
            model.setCacheUsers((long) recommendationCacheMapper.selectObjs(
                    new QueryWrapper<RecommendationCache>()
                            .select("DISTINCT user_id")
                            .eq("strategy", v.getStrategy())
                            .eq("model_version", v.getVersion())).size());
            return model;
        }).collect(Collectors.toList()));

        // 近 7 天切换事件：趋势、去向、来源页面
        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        Map<String, Long> byDate = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            byDate.put(today.minusDays(i).format(fmt), 0L);
        }
        Map<String, Long> byStrategy = new LinkedHashMap<>();
        Map<String, Long> byPage = new LinkedHashMap<>();
        List<RecommendationEvent> weekEvents = recommendationEventMapper.selectList(
                new LambdaQueryWrapper<RecommendationEvent>()
                        .ge(RecommendationEvent::getCreateTime, today.minusDays(6).atStartOfDay()));
        for (RecommendationEvent event : weekEvents) {
            String date = event.getCreateTime().toLocalDate().format(fmt);
            byDate.merge(date, 1L, Long::sum);
            byStrategy.merge(event.getToStrategy(), 1L, Long::sum);
            byPage.merge(event.getPage(), 1L, Long::sum);
        }
        stats.setEventTrend(byDate.entrySet().stream().map(e -> {
            RecommendStatsDTO.DayCount item = new RecommendStatsDTO.DayCount();
            item.setDate(e.getKey());
            item.setCount(e.getValue());
            return item;
        }).collect(Collectors.toList()));
        stats.setEventByStrategy(toNameCounts(byStrategy));
        stats.setEventByPage(toNameCounts(byPage));

        // 最近 10 条切换事件
        stats.setRecentEvents(recommendationEventMapper.selectList(
                        new LambdaQueryWrapper<RecommendationEvent>()
                                .orderByDesc(RecommendationEvent::getId)
                                .last("LIMIT 10"))
                .stream().map(event -> {
                    RecommendStatsDTO.EventItem item = new RecommendStatsDTO.EventItem();
                    item.setCreateTime(event.getCreateTime());
                    item.setUserId(event.getUserId());
                    item.setPage(event.getPage());
                    item.setFromStrategy(event.getFromStrategy());
                    item.setToStrategy(event.getToStrategy());
                    return item;
                }).collect(Collectors.toList()));
        return stats;
    }

    /** 名称计数转列表并按次数降序，空键归入"未知"。 */
    private List<RecommendStatsDTO.NameCount> toNameCounts(Map<String, Long> counts) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> {
                    RecommendStatsDTO.NameCount item = new RecommendStatsDTO.NameCount();
                    item.setName(e.getKey() == null || e.getKey().isBlank() ? "未知" : e.getKey());
                    item.setCount(e.getValue());
                    return item;
                }).collect(Collectors.toList());
    }

    // ==================== 内部方法 ====================

    private RecommendationModelVersion selectActiveVersion(String strategy) {
        return modelVersionMapper.selectOne(
                new LambdaQueryWrapper<RecommendationModelVersion>()
                        .eq(RecommendationModelVersion::getStrategy, strategy)
                        .eq(RecommendationModelVersion::getStatus, 1)
                        .last("LIMIT 1"));
    }

    /**
     * 当前用户可用策略：rule 恒可用；模型策略需存在上线版本且该用户有缓存记录。
     * 无缓存账号只显示规则，避免出现点了会失败的假标签。
     */
    private List<String> availableStrategiesFor(Long userId) {
        List<String> available = new ArrayList<>();
        available.add("rule");
        if (userId == null) {
            return available;
        }
        Set<String> activeStrategies = modelVersionMapper.selectList(
                        new LambdaQueryWrapper<RecommendationModelVersion>()
                                .eq(RecommendationModelVersion::getStatus, 1)
                                .select(RecommendationModelVersion::getStrategy))
                .stream()
                .map(RecommendationModelVersion::getStrategy)
                .collect(Collectors.toSet());
        Set<String> cachedStrategies = recommendationCacheMapper.selectList(
                        new LambdaQueryWrapper<RecommendationCache>()
                                .eq(RecommendationCache::getUserId, userId)
                                .select(RecommendationCache::getStrategy))
                .stream()
                .map(RecommendationCache::getStrategy)
                .collect(Collectors.toSet());
        for (String strategy : List.of("era", "genre", "fusion", "global")) {
            if (activeStrategies.contains(strategy) && cachedStrategies.contains(strategy)) {
                available.add(strategy);
            }
        }
        return available;
    }

    /** 当前时点已交互的诗词 id：点赞 + 收藏 + 浏览（target_type=1）。 */
    private Set<Long> loadInteractedPoemIds(Long userId) {
        Set<Long> ids = new HashSet<>();
        userLikeMapper.selectList(new LambdaQueryWrapper<UserLike>()
                        .eq(UserLike::getUserId, userId)
                        .eq(UserLike::getTargetType, POEM_TARGET_TYPE)
                        .select(UserLike::getTargetId))
                .forEach(like -> ids.add(like.getTargetId()));
        userFavoriteMapper.selectList(new LambdaQueryWrapper<UserFavorite>()
                        .eq(UserFavorite::getUserId, userId)
                        .eq(UserFavorite::getTargetType, POEM_TARGET_TYPE)
                        .select(UserFavorite::getTargetId))
                .forEach(fav -> ids.add(fav.getTargetId()));
        userHistoryMapper.selectList(new LambdaQueryWrapper<UserHistory>()
                        .eq(UserHistory::getUserId, userId)
                        .eq(UserHistory::getTargetType, POEM_TARGET_TYPE)
                        .select(UserHistory::getTargetId))
                .forEach(history -> ids.add(history.getTargetId()));
        return ids;
    }

    private List<RecommendItemDTO> buildItems(Collection<RecommendationCache> rows) {
        List<RecommendItemDTO> items = new ArrayList<>();
        if (rows.isEmpty()) {
            return items;
        }
        List<Long> poemIds = rows.stream().map(RecommendationCache::getPoemId).collect(Collectors.toList());
        Map<Long, Poem> poemMap = poemMapper.selectBatchIds(poemIds).stream()
                .collect(Collectors.toMap(Poem::getId, p -> p));
        for (RecommendationCache row : rows) {
            Poem poem = poemMap.get(row.getPoemId());
            if (poem == null || !Integer.valueOf(1).equals(poem.getStatus())
                    || !"classical".equals(poem.getPoemType())) {
                continue;
            }
            items.add(toItem(poem, row.getScore(), STRATEGY_LABELS.getOrDefault(row.getStrategy(), row.getStrategy()) + "模型推荐"));
        }
        return items;
    }

    /** 过滤后仍不足 topK 时用热门古典补齐（同样排除已交互与已选作品）。 */
    private List<RecommendItemDTO> buildHotFillItems(Long userId, Set<Long> interactedIds,
                                                     Set<Long> selectedIds, int need) {
        if (need <= 0) {
            return Collections.emptyList();
        }
        List<Poem> hotPoems = poemMapper.selectList(
                new LambdaQueryWrapper<Poem>()
                        .eq(Poem::getStatus, 1)
                        .eq(Poem::getPoemType, "classical")
                        .orderByDesc(Poem::getViewCount)
                        .orderByDesc(Poem::getLikeCount)
                        .last("LIMIT " + HOT_FILL_LIMIT));
        List<RecommendItemDTO> items = new ArrayList<>();
        for (Poem poem : hotPoems) {
            if (items.size() >= need) break;
            if (interactedIds.contains(poem.getId()) || selectedIds.contains(poem.getId())) {
                continue;
            }
            items.add(toItem(poem, 0.0, "热门补齐"));
        }
        return items;
    }

    private RecommendItemDTO toItem(Poem poem, double score, String reason) {
        RecommendItemDTO item = new RecommendItemDTO();
        item.setId(poem.getId());
        item.setTitle(poem.getTitle());
        item.setContent(poem.getContent() == null ? "" : poem.getContent());
        item.setSource("local");
        item.setRecommendScore(score);
        item.setRecommendReason(reason);
        if (poem.getPoetId() != null) {
            Poet poet = poetMapper.selectById(poem.getPoetId());
            if (poet != null) {
                item.setAuthor(poet.getName());
            }
        }
        if (item.getAuthor() == null && poem.getAuthor() != null) {
            item.setAuthor(poem.getAuthor());
        }
        if (poem.getDynastyId() != null) {
            Dynasty dynasty = dynastyMapper.selectById(poem.getDynastyId());
            if (dynasty != null) {
                item.setDynasty(dynasty.getName());
            }
        }
        return item;
    }

    /** 回退响应：HTTP 200 + fallback=true + strategy=rule + 规则推荐结果。 */
    private RecommendResponseDTO buildFallback(String requestedStrategy, RecommendationModelVersion active,
                                               String reason, Long userId, int topK) {
        RecommendResponseDTO response = new RecommendResponseDTO();
        response.setRequestedStrategy(requestedStrategy);
        response.setStrategy("rule");
        response.setFallback(true);
        response.setFallbackReason(reason);
        if (active != null) {
            response.setModelVersion(active.getVersion());
            response.setMetrics(parseMetrics(active.getMetricsJson()));
        }
        response.setAvailableStrategies(availableStrategiesFor(userId));
        try {
            response.setItems(toItemsFromMap(recommendationService.getRecommendedPoems(userId, topK)));
        } catch (Exception e) {
            log.warn("回退规则推荐失败: userId={}, strategy={}, error={}", userId, requestedStrategy, e.getMessage());
            response.setItems(Collections.emptyList());
        }
        return response;
    }

    /** 规则推荐返回 List<Map>，字段名与 RecommendItemDTO 保持一致。 */
    private List<RecommendItemDTO> toItemsFromMap(List<Map<String, Object>> maps) {
        List<RecommendItemDTO> items = new ArrayList<>();
        for (Map<String, Object> map : maps) {
            RecommendItemDTO item = new RecommendItemDTO();
            Object id = map.get("id");
            if (id instanceof Number number) {
                item.setId(number.longValue());
            } else {
                continue;
            }
            item.setTitle((String) map.get("title"));
            String content = (String) map.get("content");
            item.setContent(content == null ? "" : content);
            item.setAuthor((String) map.get("author"));
            item.setDynasty((String) map.get("dynasty"));
            item.setSource("local");
            Object score = map.get("recommendScore");
            if (score instanceof Number scoreNumber) {
                item.setRecommendScore(scoreNumber.doubleValue());
            }
            item.setRecommendReason((String) map.get("recommendReason"));
            items.add(item);
        }
        return items;
    }

    private Map<String, Double> parseMetrics(String metricsJson) {
        Map<String, Double> metrics = new LinkedHashMap<>();
        if (metricsJson == null || metricsJson.isBlank()) {
            return metrics;
        }
        try {
            Map<String, Object> raw = objectMapper.readValue(metricsJson, new TypeReference<Map<String, Object>>() {});
            for (String key : List.of("recall10", "ndcg10", "precision10", "coverage")) {
                Object value = raw.get(key);
                if (value instanceof Number number) {
                    metrics.put(key, number.doubleValue());
                }
            }
        } catch (Exception e) {
            log.warn("解析模型指标失败: {}", e.getMessage());
        }
        return metrics;
    }
}
