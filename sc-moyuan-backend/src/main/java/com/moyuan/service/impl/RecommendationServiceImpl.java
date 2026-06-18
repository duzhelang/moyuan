package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyuan.entity.*;
import com.moyuan.mapper.*;
import com.moyuan.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private static final int FAVORITE_WEIGHT = 5;
    private static final int LIKE_WEIGHT = 3;
    private static final int VIEW_WEIGHT = 1;
    private static final int INTEREST_WEIGHT = 2;
    private static final int MAX_SIMILAR_USERS = 20;
    private static final int POET_FAVORITE_TYPE = 3;
    private static final int POEM_TYPE = 1;

    private final UserFavoriteMapper userFavoriteMapper;
    private final UserLikeMapper userLikeMapper;
    private final UserHistoryMapper userHistoryMapper;
    private final PoemMapper poemMapper;
    private final PoetMapper poetMapper;
    private final DynastyMapper dynastyMapper;
    private final UserMapper userMapper;
    private final RestTemplate restTemplate;

    @Value("${external.poetry.api.enabled:true}")
    private boolean externalApiEnabled;

    @Value("${apihz.id}")
    private String apihzId;

    @Value("${apihz.key}")
    private String apihzKey;

    private static final String APIHZ_POETRY_URL = "https://cn.apihz.cn/api/zici/poetry.php";

    // ==================== 诗人推荐 ====================

    @Override
    @Cacheable(value = "poetRecommendation", key = "'user:' + #userId + ':' + #limit")
    public List<Poet> getRecommendedPoets(Long userId, int limit) {
        if (userId == null) {
            return getPopularPoets(limit);
        }

        Map<Long, Map<Long, Double>> allUserPoetScores = buildUserPoetMatrix();
        Map<Long, Double> collaborativeScores = calculateCollaborativeScores(userId, allUserPoetScores);
        Map<Long, Double> interestScores = getInterestBasedPoetScores(userId);
        Map<Long, Double> combinedScores = mergeScores(collaborativeScores, interestScores);

        List<Long> recommendedPoetIds = getTopIds(combinedScores, limit);

        if (recommendedPoetIds.isEmpty()) {
            return getPopularPoets(limit);
        }

        return getPoetsByIds(recommendedPoetIds, combinedScores);
    }

    @Override
    @Cacheable(value = "poetRecommendation", key = "'popular:' + #limit")
    public List<Poet> getPopularPoets(int limit) {
        Map<Long, Double> poetPopularity = calculatePoetPopularity();
        List<Long> topPoetIds = getTopIds(poetPopularity, limit);

        if (topPoetIds.isEmpty()) {
            LambdaQueryWrapper<Poet> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Poet::getStatus, 1)
                    .orderByDesc(Poet::getCreateTime)
                    .last("LIMIT " + limit);
            return poetMapper.selectList(wrapper);
        }

        return getPoetsByIds(topPoetIds, null);
    }

    // ==================== 诗词推荐 ====================

    @Override
    @Cacheable(value = "poemRecommendation", key = "'user:' + #userId + ':' + #limit")
    public List<Map<String, Object>> getRecommendedPoems(Long userId, int limit) {
        if (userId == null) {
            return getPopularPoems(limit);
        }

        Map<Long, Map<Long, Double>> allUserPoemScores = buildUserPoemMatrix();
        Map<Long, Double> collaborativeScores = calculateCollaborativeScores(userId, allUserPoemScores);
        Map<Long, Double> interestScores = getInterestBasedPoemScores(userId);
        Map<Long, Double> combinedScores = mergeScores(collaborativeScores, interestScores);

        List<Long> recommendedPoemIds = getTopIds(combinedScores, limit);

        if (recommendedPoemIds.isEmpty()) {
            return getPopularPoems(limit);
        }

        return getPoemsByIds(recommendedPoemIds, combinedScores, collaborativeScores);
    }

    @Override
    @Cacheable(value = "poemRecommendation", key = "'popular:' + #limit")
    public List<Map<String, Object>> getPopularPoems(int limit) {
        LambdaQueryWrapper<Poem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Poem::getStatus, 1)
                .orderByDesc(Poem::getViewCount)
                .orderByDesc(Poem::getLikeCount)
                .last("LIMIT " + limit);

        List<Poem> poems = poemMapper.selectList(wrapper);
        List<Map<String, Object>> results = new ArrayList<>();
        for (Poem poem : poems) {
            Map<String, Object> item = convertPoemToMap(poem);
            item.put("source", "local");
            item.put("recommendReason", "热门诗词");
            results.add(item);
        }
        return results;
    }

    @Override
    public List<Map<String, Object>> searchPoems(String keyword, Long userId, int page, int size) {
        List<Map<String, Object>> results = new ArrayList<>();

        // 搜索本地数据库
        LambdaQueryWrapper<Poem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Poem::getStatus, 1)
                .and(w -> w.like(Poem::getTitle, keyword)
                        .or().like(Poem::getContent, keyword))
                .orderByDesc(Poem::getViewCount)
                .last("LIMIT " + size + " OFFSET " + (page - 1) * size);

        List<Poem> localPoems = poemMapper.selectList(wrapper);
        for (Poem poem : localPoems) {
            Map<String, Object> item = convertPoemToMap(poem);
            item.put("source", "local");
            item.put("recommendScore", calculatePoemRecommendScore(poem, userId));
            results.add(item);
        }

        // 如果本地结果不足，从外部API获取
        if (results.size() < size && externalApiEnabled) {
            List<Map<String, Object>> externalPoems = getExternalPoems(keyword, size - results.size());
            results.addAll(externalPoems);
        }

        // 按推荐分数排序
        results.sort((a, b) -> {
            double scoreA = (double) a.getOrDefault("recommendScore", 0.0);
            double scoreB = (double) b.getOrDefault("recommendScore", 0.0);
            return Double.compare(scoreB, scoreA);
        });

        return results;
    }

    @Override
    public List<Map<String, Object>> getExternalPoems(String keyword, int limit) {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            String url = String.format("%s?id=%s&key=%s&words=%s&page=1",
                    APIHZ_POETRY_URL, apihzId, apihzKey, keyword);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && Integer.valueOf(200).equals(response.get("code"))) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
                if (data != null) {
                    int count = 0;
                    for (Map<String, Object> item : data) {
                        if (count >= limit) break;
                        Map<String, Object> poem = new HashMap<>();
                        poem.put("title", item.get("name"));
                        poem.put("content", stripHtml((String) item.get("content")));
                        poem.put("author", item.get("author"));
                        poem.put("dynasty", item.get("dynasty"));
                        poem.put("source", "external");
                        poem.put("recommendScore", 0.5);
                        poem.put("recommendReason", "古诗词库推荐");
                        results.add(poem);
                        count++;
                    }
                }
            }
        } catch (Exception e) {
            log.error("调用外部诗词API失败: {}", e.getMessage());
        }
        return results;
    }

    private String stripHtml(String html) {
        if (html == null) return null;
        return html.replaceAll("<[^>]+>", "").replaceAll("&nbsp;", " ").trim();
    }

    // ==================== 通用协同过滤算法 ====================

    private Map<Long, Double> calculateCollaborativeScores(Long userId, Map<Long, Map<Long, Double>> allUserScores) {
        Map<Long, Double> collaborativeScores = new HashMap<>();

        if (!allUserScores.containsKey(userId) || allUserScores.get(userId).isEmpty()) {
            return collaborativeScores;
        }

        Map<Long, Double> targetUserScores = allUserScores.get(userId);
        Map<Long, Double> userSimilarities = new HashMap<>();

        // 计算用户相似度
        for (Map.Entry<Long, Map<Long, Double>> entry : allUserScores.entrySet()) {
            Long otherUserId = entry.getKey();
            if (otherUserId.equals(userId)) continue;
            double similarity = cosineSimilarity(targetUserScores, entry.getValue());
            if (similarity > 0) {
                userSimilarities.put(otherUserId, similarity);
            }
        }

        // 获取最相似的用户
        List<Long> similarUsers = userSimilarities.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(MAX_SIMILAR_USERS)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 基于相似用户推荐
        for (Long similarUserId : similarUsers) {
            double similarity = userSimilarities.get(similarUserId);
            Map<Long, Double> similarUserScores = allUserScores.get(similarUserId);
            for (Map.Entry<Long, Double> entry : similarUserScores.entrySet()) {
                Long itemId = entry.getKey();
                if (targetUserScores.containsKey(itemId)) continue;
                collaborativeScores.merge(itemId, similarity * entry.getValue(), Double::sum);
            }
        }

        return collaborativeScores;
    }

    private double cosineSimilarity(Map<Long, Double> vecA, Map<Long, Double> vecB) {
        Set<Long> allKeys = new HashSet<>(vecA.keySet());
        allKeys.retainAll(vecB.keySet());
        if (allKeys.isEmpty()) return 0.0;

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (Long key : allKeys) {
            double a = vecA.get(key);
            double b = vecB.get(key);
            dotProduct += a * b;
        }

        for (double v : vecA.values()) normA += v * v;
        for (double v : vecB.values()) normB += v * v;

        if (normA == 0 || normB == 0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private Map<Long, Double> mergeScores(Map<Long, Double> scores1, Map<Long, Double> scores2) {
        Map<Long, Double> merged = new HashMap<>(scores1);
        for (Map.Entry<Long, Double> entry : scores2.entrySet()) {
            merged.merge(entry.getKey(), entry.getValue(), Double::sum);
        }
        return merged;
    }

    private List<Long> getTopIds(Map<Long, Double> scores, int limit) {
        return scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // ==================== 诗人相关方法 ====================

    private Map<Long, Map<Long, Double>> buildUserPoetMatrix() {
        Map<Long, Map<Long, Double>> matrix = new HashMap<>();

        // 诗人收藏
        LambdaQueryWrapper<UserFavorite> favWrapper = new LambdaQueryWrapper<>();
        favWrapper.eq(UserFavorite::getTargetType, POET_FAVORITE_TYPE);
        List<UserFavorite> poetFavorites = userFavoriteMapper.selectList(favWrapper);
        for (UserFavorite fav : poetFavorites) {
            matrix.computeIfAbsent(fav.getUserId(), k -> new HashMap<>())
                    .merge(fav.getTargetId(), (double) FAVORITE_WEIGHT, Double::sum);
        }

        Map<Long, Long> poemPoetMap = getPoemPoetMap();

        // 诗词收藏 -> 诗人
        LambdaQueryWrapper<UserFavorite> poemFavWrapper = new LambdaQueryWrapper<>();
        poemFavWrapper.eq(UserFavorite::getTargetType, POEM_TYPE);
        List<UserFavorite> poemFavorites = userFavoriteMapper.selectList(poemFavWrapper);
        for (UserFavorite fav : poemFavorites) {
            Long poetId = poemPoetMap.get(fav.getTargetId());
            if (poetId != null) {
                matrix.computeIfAbsent(fav.getUserId(), k -> new HashMap<>())
                        .merge(poetId, (double) FAVORITE_WEIGHT, Double::sum);
            }
        }

        // 诗词点赞 -> 诗人
        LambdaQueryWrapper<UserLike> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.eq(UserLike::getTargetType, POEM_TYPE);
        List<UserLike> poemLikes = userLikeMapper.selectList(likeWrapper);
        for (UserLike like : poemLikes) {
            Long poetId = poemPoetMap.get(like.getTargetId());
            if (poetId != null) {
                matrix.computeIfAbsent(like.getUserId(), k -> new HashMap<>())
                        .merge(poetId, (double) LIKE_WEIGHT, Double::sum);
            }
        }

        // 诗词浏览 -> 诗人
        LambdaQueryWrapper<UserHistory> historyWrapper = new LambdaQueryWrapper<>();
        historyWrapper.eq(UserHistory::getTargetType, POEM_TYPE);
        List<UserHistory> poemHistories = userHistoryMapper.selectList(historyWrapper);
        for (UserHistory history : poemHistories) {
            Long poetId = poemPoetMap.get(history.getTargetId());
            if (poetId != null) {
                matrix.computeIfAbsent(history.getUserId(), k -> new HashMap<>())
                        .merge(poetId, (double) VIEW_WEIGHT, Double::sum);
            }
        }

        return matrix;
    }

    private Map<Long, Double> getInterestBasedPoetScores(Long userId) {
        Map<Long, Double> scores = new HashMap<>();
        User user = userMapper.selectById(userId);
        if (user == null || user.getInterests() == null || user.getInterests().isEmpty()) {
            return scores;
        }

        Set<String> interestSet = new HashSet<>(Arrays.asList(user.getInterests().split(",")));

        if (interestSet.contains("古典")) {
            addDynastyPoetScores(scores, Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L), INTEREST_WEIGHT * 2);
        }
        if (interestSet.contains("现代")) {
            addDynastyPoetScores(scores, Arrays.asList(11L, 12L, 13L), INTEREST_WEIGHT * 2);
        }
        if (interestSet.contains("自由体")) {
            addDynastyPoetScores(scores, Arrays.asList(11L, 12L, 13L), INTEREST_WEIGHT);
        }
        if (interestSet.contains("外国")) {
            addDynastyPoetScores(scores, Arrays.asList(6L, 7L, 8L), INTEREST_WEIGHT);
        }

        return scores;
    }

    private void addDynastyPoetScores(Map<Long, Double> scores, List<Long> dynastyIds, double weight) {
        for (Long dynastyId : dynastyIds) {
            LambdaQueryWrapper<Poet> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Poet::getDynastyId, dynastyId)
                    .eq(Poet::getStatus, 1)
                    .select(Poet::getId);
            List<Poet> poets = poetMapper.selectList(wrapper);
            for (Poet poet : poets) {
                scores.merge(poet.getId(), weight, Double::sum);
            }
        }
    }

    private List<Poet> getPoetsByIds(List<Long> poetIds, Map<Long, Double> scores) {
        List<Poet> poets = poetMapper.selectBatchIds(poetIds);
        Map<Long, Poet> poetMap = poets.stream().collect(Collectors.toMap(Poet::getId, p -> p));
        return poetIds.stream()
                .map(poetMap::get)
                .filter(Objects::nonNull)
                .filter(p -> p.getStatus() != null && p.getStatus() == 1)
                .collect(Collectors.toList());
    }

    private Map<Long, Double> calculatePoetPopularity() {
        Map<Long, Double> popularity = new HashMap<>();

        LambdaQueryWrapper<UserFavorite> favWrapper = new LambdaQueryWrapper<>();
        favWrapper.eq(UserFavorite::getTargetType, POET_FAVORITE_TYPE);
        List<UserFavorite> poetFavorites = userFavoriteMapper.selectList(favWrapper);
        for (UserFavorite fav : poetFavorites) {
            popularity.merge(fav.getTargetId(), (double) FAVORITE_WEIGHT, Double::sum);
        }

        Map<Long, Long> poemPoetMap = getPoemPoetMap();

        LambdaQueryWrapper<UserFavorite> poemFavWrapper = new LambdaQueryWrapper<>();
        poemFavWrapper.eq(UserFavorite::getTargetType, POEM_TYPE);
        List<UserFavorite> poemFavorites = userFavoriteMapper.selectList(poemFavWrapper);
        for (UserFavorite fav : poemFavorites) {
            Long poetId = poemPoetMap.get(fav.getTargetId());
            if (poetId != null) {
                popularity.merge(poetId, (double) FAVORITE_WEIGHT, Double::sum);
            }
        }

        LambdaQueryWrapper<UserLike> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.eq(UserLike::getTargetType, POEM_TYPE);
        List<UserLike> poemLikes = userLikeMapper.selectList(likeWrapper);
        for (UserLike like : poemLikes) {
            Long poetId = poemPoetMap.get(like.getTargetId());
            if (poetId != null) {
                popularity.merge(poetId, (double) LIKE_WEIGHT, Double::sum);
            }
        }

        LambdaQueryWrapper<UserHistory> historyWrapper = new LambdaQueryWrapper<>();
        historyWrapper.eq(UserHistory::getTargetType, POEM_TYPE);
        List<UserHistory> poemHistories = userHistoryMapper.selectList(historyWrapper);
        for (UserHistory history : poemHistories) {
            Long poetId = poemPoetMap.get(history.getTargetId());
            if (poetId != null) {
                popularity.merge(poetId, (double) VIEW_WEIGHT, Double::sum);
            }
        }

        return popularity;
    }

    // ==================== 诗词相关方法 ====================

    private Map<Long, Map<Long, Double>> buildUserPoemMatrix() {
        Map<Long, Map<Long, Double>> matrix = new HashMap<>();

        // 诗词收藏
        LambdaQueryWrapper<UserFavorite> favWrapper = new LambdaQueryWrapper<>();
        favWrapper.eq(UserFavorite::getTargetType, POEM_TYPE);
        List<UserFavorite> favorites = userFavoriteMapper.selectList(favWrapper);
        for (UserFavorite fav : favorites) {
            matrix.computeIfAbsent(fav.getUserId(), k -> new HashMap<>())
                    .merge(fav.getTargetId(), (double) FAVORITE_WEIGHT, Double::sum);
        }

        // 诗词点赞
        LambdaQueryWrapper<UserLike> likeWrapper = new LambdaQueryWrapper<>();
        likeWrapper.eq(UserLike::getTargetType, POEM_TYPE);
        List<UserLike> likes = userLikeMapper.selectList(likeWrapper);
        for (UserLike like : likes) {
            matrix.computeIfAbsent(like.getUserId(), k -> new HashMap<>())
                    .merge(like.getTargetId(), (double) LIKE_WEIGHT, Double::sum);
        }

        // 诗词浏览
        LambdaQueryWrapper<UserHistory> historyWrapper = new LambdaQueryWrapper<>();
        historyWrapper.eq(UserHistory::getTargetType, POEM_TYPE);
        List<UserHistory> histories = userHistoryMapper.selectList(historyWrapper);
        for (UserHistory history : histories) {
            matrix.computeIfAbsent(history.getUserId(), k -> new HashMap<>())
                    .merge(history.getTargetId(), (double) VIEW_WEIGHT, Double::sum);
        }

        return matrix;
    }

    private Map<Long, Double> getInterestBasedPoemScores(Long userId) {
        Map<Long, Double> scores = new HashMap<>();
        User user = userMapper.selectById(userId);
        if (user == null || user.getInterests() == null || user.getInterests().isEmpty()) {
            return scores;
        }

        Set<String> interestSet = new HashSet<>(Arrays.asList(user.getInterests().split(",")));

        if (interestSet.contains("古典")) {
            addDynastyPoemScores(scores, Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L), 2.0);
        }
        if (interestSet.contains("现代")) {
            addDynastyPoemScores(scores, Arrays.asList(11L, 12L, 13L), 2.0);
        }
        if (interestSet.contains("自由体")) {
            addDynastyPoemScores(scores, Arrays.asList(11L, 12L, 13L), 1.0);
        }
        if (interestSet.contains("外国")) {
            addDynastyPoemScores(scores, Arrays.asList(6L, 7L, 8L), 1.0);
        }

        return scores;
    }

    private void addDynastyPoemScores(Map<Long, Double> scores, List<Long> dynastyIds, double weight) {
        for (Long dynastyId : dynastyIds) {
            LambdaQueryWrapper<Poem> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Poem::getDynastyId, dynastyId)
                    .eq(Poem::getStatus, 1)
                    .select(Poem::getId);
            List<Poem> poems = poemMapper.selectList(wrapper);
            for (Poem poem : poems) {
                scores.merge(poem.getId(), weight, Double::sum);
            }
        }
    }

    private List<Map<String, Object>> getPoemsByIds(List<Long> poemIds, Map<Long, Double> combinedScores, Map<Long, Double> collaborativeScores) {
        List<Poem> poems = poemMapper.selectBatchIds(poemIds);
        Map<Long, Poem> poemMap = poems.stream().collect(Collectors.toMap(Poem::getId, p -> p));

        List<Map<String, Object>> results = new ArrayList<>();
        for (Long poemId : poemIds) {
            Poem poem = poemMap.get(poemId);
            if (poem != null && poem.getStatus() != null && poem.getStatus() == 1) {
                Map<String, Object> item = convertPoemToMap(poem);
                item.put("source", "local");
                item.put("recommendScore", combinedScores.get(poemId));
                item.put("recommendReason", collaborativeScores.containsKey(poemId) ? "根据您的浏览偏好推荐" : "根据您的兴趣推荐");
                results.add(item);
            }
        }
        return results;
    }

    private double calculatePoemRecommendScore(Poem poem, Long userId) {
        if (userId == null) {
            return 0.0;
        }

        double score = 0.0;
        User user = userMapper.selectById(userId);
        if (user != null && user.getInterests() != null) {
            for (String interest : user.getInterests().split(",")) {
                if (interest.equals("古典") && poem.getDynastyId() != null && poem.getDynastyId() <= 6) {
                    score += 2.0;
                }
                if (interest.equals("现代") && poem.getDynastyId() != null && poem.getDynastyId() >= 11) {
                    score += 2.0;
                }
            }
        }

        score += Math.log1p(poem.getViewCount() != null ? poem.getViewCount() : 0) * 0.1;
        score += Math.log1p(poem.getLikeCount() != null ? poem.getLikeCount() : 0) * 0.2;

        return score;
    }

    // ==================== 工具方法 ====================

    private Map<Long, Long> getPoemPoetMap() {
        LambdaQueryWrapper<Poem> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(Poem::getPoetId)
                .eq(Poem::getStatus, 1)
                .select(Poem::getId, Poem::getPoetId);
        List<Poem> poems = poemMapper.selectList(wrapper);
        Map<Long, Long> map = new HashMap<>();
        for (Poem poem : poems) {
            if (poem.getPoetId() != null) {
                map.put(poem.getId(), poem.getPoetId());
            }
        }
        return map;
    }

    private Map<String, Object> convertPoemToMap(Poem poem) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", poem.getId());
        map.put("title", poem.getTitle());
        map.put("content", poem.getContent());
        map.put("poetId", poem.getPoetId());
        map.put("dynastyId", poem.getDynastyId());
        map.put("viewCount", poem.getViewCount());
        map.put("likeCount", poem.getLikeCount());
        map.put("favoriteCount", poem.getFavoriteCount());

        if (poem.getPoetId() != null) {
            Poet poet = poetMapper.selectById(poem.getPoetId());
            if (poet != null) {
                map.put("author", poet.getName());
            }
        }

        if (poem.getDynastyId() != null) {
            Dynasty dynasty = dynastyMapper.selectById(poem.getDynastyId());
            if (dynasty != null) {
                map.put("dynasty", dynasty.getName());
            }
        }

        return map;
    }
}