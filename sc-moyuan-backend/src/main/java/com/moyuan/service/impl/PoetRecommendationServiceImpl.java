package com.moyuan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.moyuan.entity.*;
import com.moyuan.mapper.*;
import com.moyuan.service.DynastyService;
import com.moyuan.service.PoetRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PoetRecommendationServiceImpl implements PoetRecommendationService {

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
    private final UserMapper userMapper;
    private final DynastyService dynastyService;

    @Override
    @Cacheable(value = "poetRecommendation", key = "'user:' + #userId + ':' + #limit")
    public List<Poet> getRecommendedPoets(Long userId, int limit) {
        if (userId == null) {
            return getPopularPoets(limit);
        }

        Map<Long, Map<Long, Double>> allUserPoetScores = buildUserPoetMatrix();

        // 基于协同过滤的推荐
        Map<Long, Double> collaborativeScores = new HashMap<>();
        if (allUserPoetScores.containsKey(userId) && !allUserPoetScores.get(userId).isEmpty()) {
            Map<Long, Double> targetUserScores = allUserPoetScores.get(userId);
            Map<Long, Double> userSimilarities = new HashMap<>();

            for (Map.Entry<Long, Map<Long, Double>> entry : allUserPoetScores.entrySet()) {
                Long otherUserId = entry.getKey();
                if (otherUserId.equals(userId)) continue;
                double similarity = cosineSimilarity(targetUserScores, entry.getValue());
                if (similarity > 0) {
                    userSimilarities.put(otherUserId, similarity);
                }
            }

            List<Long> similarUsers = userSimilarities.entrySet().stream()
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .limit(MAX_SIMILAR_USERS)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            for (Long similarUserId : similarUsers) {
                double similarity = userSimilarities.get(similarUserId);
                Map<Long, Double> similarUserScores = allUserPoetScores.get(similarUserId);
                for (Map.Entry<Long, Double> entry : similarUserScores.entrySet()) {
                    Long poetId = entry.getKey();
                    if (targetUserScores.containsKey(poetId)) continue;
                    collaborativeScores.merge(poetId, similarity * entry.getValue(), Double::sum);
                }
            }
        }

        // 基于用户兴趣的推荐
        Map<Long, Double> interestScores = getInterestBasedScores(userId);

        // 合并两种推荐分数
        Map<Long, Double> combinedScores = new HashMap<>(collaborativeScores);
        for (Map.Entry<Long, Double> entry : interestScores.entrySet()) {
            combinedScores.merge(entry.getKey(), entry.getValue(), Double::sum);
        }

        List<Long> recommendedPoetIds = combinedScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (recommendedPoetIds.isEmpty()) {
            return getPopularPoets(limit);
        }

        List<Poet> poets = poetMapper.selectBatchIds(recommendedPoetIds);
        Map<Long, Poet> poetMap = poets.stream().collect(Collectors.toMap(Poet::getId, p -> p));
        return recommendedPoetIds.stream()
                .map(poetMap::get)
                .filter(Objects::nonNull)
                .filter(p -> p.getStatus() != null && p.getStatus() == 1)
                .collect(Collectors.toList());
    }

    private Map<Long, Double> getInterestBasedScores(Long userId) {
        Map<Long, Double> scores = new HashMap<>();
        
        User user = userMapper.selectById(userId);
        if (user == null || user.getInterests() == null || user.getInterests().isEmpty()) {
            return scores;
        }

        String[] interests = user.getInterests().split(",");
        Set<String> interestSet = new HashSet<>(Arrays.asList(interests));

        // 根据兴趣推荐相关朝代的诗人
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
            // 外国诗歌兴趣，可以推荐一些有国际影响力的诗人
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

    @Override
    @Cacheable(value = "poetRecommendation", key = "'popular:' + #limit")
    public List<Poet> getPopularPoets(int limit) {
        Map<Long, Double> poetPopularity = calculatePoetPopularity();

        List<Long> topPoetIds = poetPopularity.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (topPoetIds.isEmpty()) {
            LambdaQueryWrapper<Poet> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Poet::getStatus, 1)
                    .orderByDesc(Poet::getCreateTime)
                    .last("LIMIT " + limit);
            return poetMapper.selectList(wrapper);
        }

        List<Poet> poets = poetMapper.selectBatchIds(topPoetIds);
        Map<Long, Poet> poetMap = poets.stream().collect(Collectors.toMap(Poet::getId, p -> p));
        return topPoetIds.stream()
                .map(poetMap::get)
                .filter(Objects::nonNull)
                .filter(p -> p.getStatus() != null && p.getStatus() == 1)
                .collect(Collectors.toList());
    }

    private Map<Long, Map<Long, Double>> buildUserPoetMatrix() {
        Map<Long, Map<Long, Double>> matrix = new HashMap<>();

        LambdaQueryWrapper<UserFavorite> favWrapper = new LambdaQueryWrapper<>();
        favWrapper.eq(UserFavorite::getTargetType, POET_FAVORITE_TYPE);
        List<UserFavorite> poetFavorites = userFavoriteMapper.selectList(favWrapper);
        for (UserFavorite fav : poetFavorites) {
            matrix.computeIfAbsent(fav.getUserId(), k -> new HashMap<>())
                    .merge(fav.getTargetId(), (double) FAVORITE_WEIGHT, Double::sum);
        }

        Map<Long, Long> poemPoetMap = getPoemPoetMap();

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
}