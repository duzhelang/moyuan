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
    private static final int MAX_SIMILAR_USERS = 20;
    private static final int POET_FAVORITE_TYPE = 3;
    private static final int POEM_TYPE = 1;

    private final UserFavoriteMapper userFavoriteMapper;
    private final UserLikeMapper userLikeMapper;
    private final UserHistoryMapper userHistoryMapper;
    private final PoemMapper poemMapper;
    private final PoetMapper poetMapper;
    private final DynastyService dynastyService;

    @Override
    @Cacheable(value = "poetRecommendation", key = "'user:' + #userId + ':' + #limit")
    public List<Poet> getRecommendedPoets(Long userId, int limit) {
        if (userId == null) {
            return getPopularPoets(limit);
        }

        Map<Long, Map<Long, Double>> allUserPoetScores = buildUserPoetMatrix();

        if (!allUserPoetScores.containsKey(userId) || allUserPoetScores.get(userId).isEmpty()) {
            return getPopularPoets(limit);
        }

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

        Map<Long, Double> candidateScores = new HashMap<>();
        for (Long similarUserId : similarUsers) {
            double similarity = userSimilarities.get(similarUserId);
            Map<Long, Double> similarUserScores = allUserPoetScores.get(similarUserId);
            for (Map.Entry<Long, Double> entry : similarUserScores.entrySet()) {
                Long poetId = entry.getKey();
                if (targetUserScores.containsKey(poetId)) continue;
                candidateScores.merge(poetId, similarity * entry.getValue(), Double::sum);
            }
        }

        List<Long> recommendedPoetIds = candidateScores.entrySet().stream()
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