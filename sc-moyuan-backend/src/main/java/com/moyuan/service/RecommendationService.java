package com.moyuan.service;

import com.moyuan.entity.Poet;
import java.util.List;
import java.util.Map;

public interface RecommendationService {
    // 诗人推荐
    List<Poet> getRecommendedPoets(Long userId, int limit);
    List<Poet> getPopularPoets(int limit);

    // 诗词推荐
    List<Map<String, Object>> getRecommendedPoems(Long userId, int limit);
    List<Map<String, Object>> getPopularPoems(int limit);
    List<Map<String, Object>> searchPoems(String keyword, Long userId, int page, int size);
    List<Map<String, Object>> getExternalPoems(String keyword, int limit);
    Map<String, Object> getExternalPoemDetail(String keyword);
}