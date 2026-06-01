package com.moyuan.service;

import com.moyuan.entity.Poet;
import java.util.List;

public interface PoetRecommendationService {
    List<Poet> getRecommendedPoets(Long userId, int limit);
    List<Poet> getPopularPoets(int limit);
}