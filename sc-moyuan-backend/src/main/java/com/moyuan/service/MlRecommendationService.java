package com.moyuan.service;

import com.moyuan.dto.response.RecommendModelsResponseDTO;
import com.moyuan.dto.response.RecommendResponseDTO;
import com.moyuan.dto.response.RecommendStatsDTO;

/**
 * 模型策略推荐服务（四组实验离线缓存查表）。
 * 规则策略继续走 {@link RecommendationService} + 公开接口 /api/search/poems/recommended，
 * 本服务只覆盖 era/genre/fusion/global 四个模型策略，所有失败场景回退规则结果。
 */
public interface MlRecommendationService {

    /**
     * 按模型策略取推荐。缓存未命中/版本下线/过滤后为空时返回
     * HTTP 200 + fallback=true + strategy=rule + 规则结果，不抛 500。
     */
    RecommendResponseDTO recommend(Long userId, String strategy, int topK);

    /**
     * 上线模型列表与当前用户可用策略。
     * 匿名用户 availableStrategies 固定 ['rule']。
     */
    RecommendModelsResponseDTO getModels(Long userId);

    /** 记录策略切换埋点（userId 由调用方从 JWT 解出）。 */
    void recordEvent(Long userId, String page, String fromStrategy, String toStrategy);

    /** 实时统计：缓存行数/覆盖用户、切换事件总数/今日数/最近时间，供管理端指标屏展示。 */
    RecommendStatsDTO getLiveStats();
}
